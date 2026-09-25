package com.jared.flights.core.model

import java.time.Duration
import java.time.Instant

/**
 * The three versions of one event time (e.g. leaving the gate):
 * what the timetable says, the latest estimate, and what actually happened.
 */
data class FlightTimes(
    val scheduled: Instant,
    val estimated: Instant? = null,
    val actual: Instant? = null,
) {
    /** The most reliable time we have: actual, else estimated, else scheduled. */
    val best: Instant
        get() = actual ?: estimated ?: scheduled

    /** How late (positive) or early (negative) compared with the timetable. */
    val delay: Duration
        get() = Duration.between(scheduled, best)
}
