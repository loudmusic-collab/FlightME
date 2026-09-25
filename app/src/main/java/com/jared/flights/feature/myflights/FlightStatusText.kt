package com.jared.flights.feature.myflights

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import com.jared.flights.R
import com.jared.flights.core.designsystem.theme.FlightMeTheme
import com.jared.flights.core.model.DelaySeverity
import com.jared.flights.core.model.Flight
import com.jared.flights.core.model.FlightStatus
import java.time.Duration

/** Short status line for a card, e.g. "On time", "Delayed 25m", "In the air · 50m late". */
@Composable
fun flightStatusText(flight: Flight): String {
    val late = flight.delaySeverity != DelaySeverity.ON_TIME
    val delay = formatDelay(flight.relevantDelay)
    return when (flight.status) {
        FlightStatus.SCHEDULED ->
            if (late) stringResource(R.string.status_delayed, delay) else stringResource(R.string.status_on_time)
        FlightStatus.BOARDING -> withDelay(stringResource(R.string.status_boarding), late, delay)
        FlightStatus.DEPARTED -> withDelay(stringResource(R.string.status_departed), late, delay)
        FlightStatus.EN_ROUTE -> withDelay(stringResource(R.string.status_in_the_air), late, delay)
        FlightStatus.LANDED -> withDelay(stringResource(R.string.status_landed), late, delay)
        FlightStatus.ARRIVED -> withDelay(stringResource(R.string.status_arrived), late, delay)
        FlightStatus.CANCELLED -> stringResource(R.string.status_cancelled)
        FlightStatus.DIVERTED -> flight.divertedTo
            ?.let { stringResource(R.string.status_diverted_to, it.iata) }
            ?: stringResource(R.string.status_diverted)
    }
}

@Composable
private fun withDelay(phase: String, late: Boolean, delay: String): String =
    if (late) stringResource(R.string.status_phase_late, phase, delay) else phase

/** Colour for a flight's status: green on time, amber minor delay, red major/cancelled/diverted. */
@Composable
fun flightStatusColor(flight: Flight): Color {
    val colors = FlightMeTheme.statusColors
    return when (flight.status) {
        FlightStatus.CANCELLED, FlightStatus.DIVERTED -> colors.cancelled
        else -> when (flight.delaySeverity) {
            DelaySeverity.ON_TIME -> colors.onTime
            DelaySeverity.MINOR -> colors.minorDelay
            DelaySeverity.MAJOR -> colors.majorDelay
        }
    }
}

/** "25m" or "1h 10m". */
@Composable
fun formatDelay(delay: Duration): String {
    val minutes = delay.toMinutes().coerceAtLeast(0)
    return if (minutes < 60) {
        stringResource(R.string.duration_minutes, minutes)
    } else {
        stringResource(R.string.duration_hours_minutes, minutes / 60, minutes % 60)
    }
}
