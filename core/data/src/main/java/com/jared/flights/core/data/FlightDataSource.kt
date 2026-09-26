package com.jared.flights.core.data

import com.jared.flights.core.model.Flight
import com.jared.flights.core.model.FlightNumber
import com.jared.flights.core.model.LivePosition
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

/**
 * Where fresh flight data comes from. Today: [com.jared.flights.core.data.mock.MockFlightDataSource].
 * Phase 2 adds a Firebase version; the `USE_MOCK_DATA` build flag picks one.
 *
 * The screens never read this directly: [FlightSync] copies what it sends into
 * the phone's database, and the screens read the database (DECISIONS #7).
 */
interface FlightDataSource {
    /** The flights the user is tracking. Emits whenever fresh data arrives; nothing while offline. */
    fun observeTrackedFlights(): Flow<List<Flight>>

    /** True while the source can reach its server. */
    fun observeOnline(): Flow<Boolean>

    /**
     * Ask for the latest flights right now (pull-to-refresh).
     * Reads our own server's copy; it never forces a paid provider call (DECISIONS #36).
     * Throws [java.io.IOException] if there's no connection.
     */
    suspend fun fetchTrackedFlights(): List<Flight>

    /**
     * Find flight [number] leaving on [date] (local date at the departure airport).
     * Usually one result, none if it doesn't fly that day. Phase 2: the `searchFlights`
     * server function, which caches results (PLAN §6). Throws IOException if offline.
     */
    suspend fun searchFlights(number: FlightNumber, date: LocalDate): List<Flight>

    /** Start tracking a flight by id (a search result, or one just removed). Throws IOException if offline. */
    suspend fun trackFlight(flightId: String)

    /** Stop tracking a flight. Throws IOException if offline. */
    suspend fun untrackFlight(flightId: String)

    /**
     * Where the plane is now, for the live map. Only asked for while a map is on
     * screen, at most once a minute. Phase 2+: the `getLivePosition` server function,
     * which shares one provider call per flight per minute across all users (PLAN §6).
     * Null if unknown. Throws IOException if offline.
     */
    suspend fun getLivePosition(flightId: String): LivePosition?
}
