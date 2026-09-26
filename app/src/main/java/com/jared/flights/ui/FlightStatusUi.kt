package com.jared.flights.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.jared.flights.R
import com.jared.flights.core.designsystem.theme.FlightMeTheme
import com.jared.flights.core.model.DelaySeverity
import com.jared.flights.core.model.Flight
import com.jared.flights.core.model.FlightStatus
import java.time.Duration

/** Coloured dot + status text, e.g. "● Delayed 25m". */
@Composable
fun FlightStatusLabel(flight: Flight, modifier: Modifier = Modifier) {
    val color = flightStatusColor(flight)
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .size(8.dp)
                .background(color, CircleShape),
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = flightStatusText(flight),
            style = MaterialTheme.typography.labelLarge,
            color = color,
        )
    }
}

/** Short status line, e.g. "On time", "Delayed 25m", "In the air · 50m late". */
@Composable
fun flightStatusText(flight: Flight): String {
    val late = flight.delaySeverity != DelaySeverity.ON_TIME
    val delay = formatDuration(flight.relevantDelay)
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

/** "25m", "5h" or "1h 10m". Negative durations are shown as their size (e.g. -5 min → "5m"). */
@Composable
fun formatDuration(duration: Duration): String {
    val minutes = duration.abs().toMinutes()
    return when {
        minutes < 60 -> stringResource(R.string.duration_minutes, minutes)
        minutes % 60 == 0L -> stringResource(R.string.duration_hours, minutes / 60)
        else -> stringResource(R.string.duration_hours_minutes, minutes / 60, minutes % 60)
    }
}
