package com.jared.flights.core.data

import com.jared.flights.core.model.LivePosition
import com.jared.flights.core.model.estimatedPositionAt
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import java.io.IOException
import java.time.Clock
import javax.inject.Inject

/**
 * The plane's position for the live map (DECISIONS #41).
 * Asks the data source once a minute, and only while someone is collecting this
 * (i.e. a map is on screen), to keep costs down once real data arrives (PLAN §6).
 * If that fails (offline, e.g. in the air), falls back to an estimate from the
 * flight's own times, so the map still shows roughly where the plane is.
 */
class LivePositionTracker @Inject constructor(
    private val repository: FlightRepository,
    private val clock: Clock,
) {
    fun observe(flightId: String, refreshMillis: Long = 60_000): Flow<LivePosition?> {
        val fetched = flow {
            while (true) {
                val position = try {
                    repository.fetchLivePosition(flightId)
                } catch (e: IOException) {
                    null
                }
                emit(position)
                delay(refreshMillis)
            }
        }
        return combine(fetched, repository.observeFlight(flightId)) { position, flight ->
            position ?: flight?.estimatedPositionAt(clock.instant())
        }
    }
}
