package com.jared.flights.core.model

import java.time.Duration
import java.time.Instant

/**
 * How far through the journey a flight is, from 0.0 (at the departure gate)
 * to 1.0 (at the arrival gate). Used to place the plane on the route line.
 *
 * - Not yet departed (or cancelled): 0.0
 * - Landed or arrived: 1.0
 * - Otherwise: time elapsed since departure ÷ expected total time,
 *   using actual/estimated times when we have them.
 */
fun Flight.progressAt(now: Instant): Float = when {
    status == FlightStatus.CANCELLED || !status.hasDeparted -> 0f
    status == FlightStatus.LANDED || status == FlightStatus.ARRIVED -> 1f
    else -> {
        val total = Duration.between(departure.best, arrival.best).toMillis()
        val elapsed = Duration.between(departure.best, now).toMillis()
        if (total <= 0) 1f else (elapsed.toFloat() / total).coerceIn(0f, 1f)
    }
}
