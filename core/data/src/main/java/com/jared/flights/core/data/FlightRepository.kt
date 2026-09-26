package com.jared.flights.core.data

import com.jared.flights.core.database.dao.BookingDao
import com.jared.flights.core.database.dao.FlightDao
import com.jared.flights.core.database.dao.SyncStateDao
import com.jared.flights.core.database.dao.TripSplitDao
import com.jared.flights.core.database.entity.BookingEntity
import com.jared.flights.core.database.entity.TripSplitEntity
import com.jared.flights.core.database.entity.toModel
import com.jared.flights.core.model.Flight
import com.jared.flights.core.model.FlightNumber
import com.jared.flights.core.model.Trip
import com.jared.flights.core.model.groupIntoTrips
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.io.IOException
import java.time.Instant
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

/**
 * What the screens use to get flights. Everything here is read from the
 * phone's database, so it works offline (DECISIONS #7). [FlightSync] keeps
 * the database up to date.
 */
interface FlightRepository {
    /**
     * Tracked flights: active ones first (earliest departure first),
     * then finished ones (most recent first).
     */
    fun observeTrackedFlights(): Flow<List<Flight>>

    /** One tracked flight by id, or null if it isn't tracked. */
    fun observeFlight(id: String): Flow<Flight?>

    /**
     * Tracked flights grouped into trips (connecting legs together, DECISIONS #31).
     * Same order as [observeTrackedFlights], by each trip's first flight.
     */
    fun observeTrips(): Flow<List<Trip>>

    /** The trip that contains this flight, or null. */
    fun observeTripFor(flightId: String): Flow<Trip?>

    /** The user says [flightId] is not a connection: it starts a new trip. Saved on the phone. */
    suspend fun splitTripBefore(flightId: String)

    /** Online/offline, and when the flights were last refreshed. */
    fun observeSyncStatus(): Flow<SyncStatus>

    /** Pull-to-refresh: fetch the latest now. False if offline (saved data is kept). */
    suspend fun refresh(): Boolean

    /** Find a flight by number and local departure date. Throws [java.io.IOException] if offline. */
    suspend fun search(number: FlightNumber, date: LocalDate): List<Flight>

    /**
     * Start tracking flights by id (one, or every leg of a trip when undoing a removal).
     * [bookingReference] (optional, single flight) is saved on the phone only.
     * False if offline.
     */
    suspend fun track(flightIds: List<String>, bookingReference: String? = null): Boolean

    /** Stop tracking these flights (one, or every leg of a trip). False if offline. */
    suspend fun untrack(flightIds: List<String>): Boolean

    /** The user's booking reference for a flight, if they entered one. */
    fun observeBookingReference(flightId: String): Flow<String?>

    /** Save, change or (with null/blank) clear a booking reference. */
    suspend fun setBookingReference(flightId: String, reference: String?)
}

@Singleton
class DefaultFlightRepository @Inject constructor(
    private val flightDao: FlightDao,
    private val tripSplitDao: TripSplitDao,
    private val syncStateDao: SyncStateDao,
    private val bookingDao: BookingDao,
    private val dataSource: FlightDataSource,
    private val flightSync: FlightSync,
) : FlightRepository {

    private val flights: Flow<List<Flight>> = flightDao.observeAll().map { rows -> rows.map { it.toModel() } }

    override fun observeTrackedFlights(): Flow<List<Flight>> =
        flights.map { flights ->
            val (finished, active) = flights.partition { it.isFinished }
            active.sortedBy { it.departure.best } +
                finished.sortedByDescending { it.departure.best }
        }

    override fun observeFlight(id: String): Flow<Flight?> =
        flightDao.observeById(id).map { it?.toModel() }

    override fun observeTrips(): Flow<List<Trip>> =
        combine(flights, tripSplitDao.observeSplitFlightIds()) { flights, splitBefore ->
            val (finished, active) = groupIntoTrips(flights, splitBefore.toSet()).partition { it.isFinished }
            active.sortedBy { it.legs.first().departure.best } +
                finished.sortedByDescending { it.legs.first().departure.best }
        }

    override fun observeTripFor(flightId: String): Flow<Trip?> =
        observeTrips().map { trips -> trips.find { trip -> trip.legs.any { it.id == flightId } } }

    override suspend fun splitTripBefore(flightId: String) {
        tripSplitDao.insert(TripSplitEntity(flightId))
    }

    override fun observeSyncStatus(): Flow<SyncStatus> =
        combine(dataSource.observeOnline(), syncStateDao.observeLastSuccessAt()) { online, lastMillis ->
            SyncStatus(isOnline = online, lastSuccessAt = lastMillis?.let(Instant::ofEpochMilli))
        }

    override suspend fun refresh(): Boolean = flightSync.refresh()

    override suspend fun search(number: FlightNumber, date: LocalDate): List<Flight> =
        dataSource.searchFlights(number, date)

    override suspend fun track(flightIds: List<String>, bookingReference: String?): Boolean = try {
        flightIds.forEach { dataSource.trackFlight(it) }
        if (bookingReference != null) flightIds.singleOrNull()?.let { setBookingReference(it, bookingReference) }
        true
    } catch (e: IOException) {
        false
    }

    override suspend fun untrack(flightIds: List<String>): Boolean = try {
        flightIds.forEach { dataSource.untrackFlight(it) }
        true
    } catch (e: IOException) {
        false
    }

    override fun observeBookingReference(flightId: String): Flow<String?> =
        bookingDao.observeReference(flightId)

    override suspend fun setBookingReference(flightId: String, reference: String?) {
        val tidy = reference?.trim()?.uppercase()
        if (tidy.isNullOrEmpty()) bookingDao.delete(flightId) else bookingDao.upsert(BookingEntity(flightId, tidy))
    }
}
