package com.jared.flights.core.data

import com.jared.flights.core.model.Flight
import kotlinx.coroutines.flow.Flow

/**
 * Where flight data comes from. Today: [com.jared.flights.core.data.mock.MockFlightDataSource].
 * Phase 2 adds a Firebase version; the `USE_MOCK_DATA` build flag picks one.
 */
interface FlightDataSource {
    /** The flights the user is tracking. Emits again whenever they change. */
    fun observeTrackedFlights(): Flow<List<Flight>>
}
