package com.jared.flights.core.data

import com.jared.flights.core.model.Flight
import com.jared.flights.core.model.Trip
import com.jared.flights.core.model.groupIntoTrips
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

/** What the screens use to get flights. Hides where the data comes from. */
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

    /** The user says [flightId] is not a connection: it starts a new trip. */
    fun splitTripBefore(flightId: String)
}

@Singleton
class DefaultFlightRepository @Inject constructor(
    private val dataSource: FlightDataSource,
) : FlightRepository {

    // Kept in memory for now; saved on the phone from step 1.5 (Room).
    private val splits = MutableStateFlow<Set<String>>(emptySet())

    override fun observeTrackedFlights(): Flow<List<Flight>> =
        dataSource.observeTrackedFlights().map { flights ->
            val (finished, active) = flights.partition { it.isFinished }
            active.sortedBy { it.departure.best } +
                finished.sortedByDescending { it.departure.best }
        }

    override fun observeFlight(id: String): Flow<Flight?> =
        dataSource.observeTrackedFlights().map { flights -> flights.find { it.id == id } }

    override fun observeTrips(): Flow<List<Trip>> =
        combine(dataSource.observeTrackedFlights(), splits) { flights, splitBefore ->
            val (finished, active) = groupIntoTrips(flights, splitBefore).partition { it.isFinished }
            active.sortedBy { it.legs.first().departure.best } +
                finished.sortedByDescending { it.legs.first().departure.best }
        }

    override fun observeTripFor(flightId: String): Flow<Trip?> =
        observeTrips().map { trips -> trips.find { trip -> trip.legs.any { it.id == flightId } } }

    override fun splitTripBefore(flightId: String) {
        splits.update { it + flightId }
    }
}
