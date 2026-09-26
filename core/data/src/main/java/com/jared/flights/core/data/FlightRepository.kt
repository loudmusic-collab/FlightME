package com.jared.flights.core.data

import com.jared.flights.core.database.dao.FlightDao
import com.jared.flights.core.database.dao.SyncStateDao
import com.jared.flights.core.database.dao.TripSplitDao
import com.jared.flights.core.database.entity.TripSplitEntity
import com.jared.flights.core.database.entity.toModel
import com.jared.flights.core.model.Flight
import com.jared.flights.core.model.Trip
import com.jared.flights.core.model.groupIntoTrips
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.time.Instant
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
}

@Singleton
class DefaultFlightRepository @Inject constructor(
    private val flightDao: FlightDao,
    private val tripSplitDao: TripSplitDao,
    private val syncStateDao: SyncStateDao,
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
}
