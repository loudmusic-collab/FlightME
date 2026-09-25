package com.jared.flights.core.model

import java.time.Duration

enum class DelaySeverity { ON_TIME, MINOR, MAJOR }

/**
 * How late counts as "delayed" (DECISIONS #29).
 * Under 15 min is on time (the usual industry definition),
 * 15–44 min is a minor delay (amber), 45 min or more is major (red).
 * Early flights count as on time.
 */
object DelayRules {
    val MINOR_FROM: Duration = Duration.ofMinutes(15)
    val MAJOR_FROM: Duration = Duration.ofMinutes(45)

    fun severityOf(delay: Duration): DelaySeverity = when {
        delay >= MAJOR_FROM -> DelaySeverity.MAJOR
        delay >= MINOR_FROM -> DelaySeverity.MINOR
        else -> DelaySeverity.ON_TIME
    }
}
