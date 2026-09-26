package com.jared.flights.core.data

import com.jared.flights.core.model.Flight
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/** What the screens use to get flights. Hides where the data comes from. */
interface FlightRepository {
    /**
     * Tracked flights: active ones first (earliest departure first),
     * then finished ones (most recent first).
     */
    fun observeTrackedFlights(): Flow<List<Flight>>

    /** One tracked flight by id, or null if it isn't tracked. */
    fun observeFlight(id: String): Flow<Flight?>
}

class DefaultFlightRepository @Inject constructor(
    private val dataSource: FlightDataSource,
) : FlightRepository {

    override fun observeTrackedFlights(): Flow<List<Flight>> =
        dataSource.observeTrackedFlights().map { flights ->
            val (finished, active) = flights.partition { it.isFinished }
            active.sortedBy { it.departure.best } +
                finished.sortedByDescending { it.departure.best }
        }

    override fun observeFlight(id: String): Flow<Flight?> =
        dataSource.observeTrackedFlights().map { flights -> flights.find { it.id == id } }
}
