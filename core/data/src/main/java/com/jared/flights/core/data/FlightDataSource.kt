package com.jared.flights.core.data

import com.jared.flights.core.model.Flight
import kotlinx.coroutines.flow.Flow

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
}
