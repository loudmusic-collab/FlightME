package com.jared.flights.feature.myflights

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.jared.flights.core.designsystem.theme.FlightMeTheme
import com.jared.flights.core.model.Airport
import com.jared.flights.core.model.Flight
import com.jared.flights.core.model.FlightStatus
import com.jared.flights.core.model.FlightTimes
import com.jared.flights.core.model.progressAt
import com.jared.flights.ui.FlightStatusLabel
import com.jared.flights.ui.RouteLine
import com.jared.flights.ui.arrivalDayOffset
import com.jared.flights.ui.dateLabel
import com.jared.flights.ui.localTime
import com.jared.flights.ui.timeColor
import java.time.Instant
import java.time.ZoneId

/**
 * One tracked flight in the My Flights list:
 *
 *   BA 283                                  ● On time
 *   British Airways · Airbus A350-1000
 *   LHR  ─────────── ✈ ───────────  LAX
 *   14:05                              17:25
 *   London · Today                  Los Angeles
 */
@Composable
fun FlightCard(
    flight: Flight,
    now: Instant,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val cardColor = MaterialTheme.colorScheme.surface

    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = cardColor),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "${flight.airlineIata} ${flight.flightNumber}",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                )
                FlightStatusLabel(flight)
            }
            Text(
                text = listOfNotNull(flight.airlineName, flight.aircraftType).joinToString(" · "),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(12.dp))
            RouteLine(flight.origin, flight.destination, flight.progressAt(now), cardColor)
            Spacer(Modifier.height(4.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                TimeColumn(
                    times = flight.departure,
                    airport = flight.origin,
                    dateLabel = dateLabel(flight.departure.best, flight.origin.timeZone, now),
                    cancelled = flight.status == FlightStatus.CANCELLED,
                    alignment = Alignment.Start,
                )
                TimeColumn(
                    times = flight.arrival,
                    airport = flight.destination,
                    dateLabel = arrivalDayOffset(flight),
                    cancelled = flight.status == FlightStatus.CANCELLED,
                    alignment = Alignment.End,
                )
            }
        }
    }
}

/**
 * The time to show (actual/estimated if known, in the airport's local time).
 * If it differs from the timetable, the timetable time is shown struck through.
 */
@Composable
private fun TimeColumn(
    times: FlightTimes,
    airport: Airport,
    dateLabel: String?,
    cancelled: Boolean,
    alignment: Alignment.Horizontal,
) {
    val changed = !cancelled && times.best != times.scheduled
    val textAlign = if (alignment == Alignment.End) TextAlign.End else TextAlign.Start

    Column(horizontalAlignment = alignment) {
        Row(verticalAlignment = Alignment.Bottom) {
            if (changed && alignment == Alignment.End) ScheduledStruck(times, airport)
            Text(
                text = times.best.localTime(airport.timeZone),
                style = MaterialTheme.typography.titleLarge,
                color = timeColor(times, cancelled),
                textDecoration = if (cancelled) TextDecoration.LineThrough else null,
            )
            if (changed && alignment == Alignment.Start) ScheduledStruck(times, airport)
        }
        Text(
            text = listOfNotNull(airport.city, dateLabel).joinToString(" · "),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = textAlign,
        )
    }
}

@Composable
private fun ScheduledStruck(times: FlightTimes, airport: Airport) {
    Text(
        text = times.scheduled.localTime(airport.timeZone),
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textDecoration = TextDecoration.LineThrough,
        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
    )
}

@Preview(showBackground = true)
@Composable
private fun FlightCardPreview() {
    val lhr = Airport("LHR", "EGLL", "Heathrow", "London", ZoneId.of("Europe/London"))
    val jfk = Airport("JFK", "KJFK", "John F. Kennedy", "New York", ZoneId.of("America/New_York"))
    val now = Instant.parse("2026-09-25T12:00:00Z")
    FlightMeTheme {
        FlightCard(
            flight = Flight(
                id = "preview", airlineIata = "VS", airlineName = "Virgin Atlantic",
                flightNumber = "3", origin = lhr, destination = jfk,
                departure = FlightTimes(now, estimated = now.plusSeconds(75 * 60)),
                arrival = FlightTimes(now.plusSeconds(480 * 60), estimated = now.plusSeconds(545 * 60)),
                status = FlightStatus.SCHEDULED,
                aircraftType = "Airbus A350-1000",
            ),
            now = now,
            onClick = {},
            modifier = Modifier.padding(16.dp),
        )
    }
}
