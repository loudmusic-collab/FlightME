package com.jared.flights.feature.myflights

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.jared.flights.R
import com.jared.flights.core.designsystem.icon.FlightMeIcons
import com.jared.flights.core.designsystem.theme.FlightMeTheme
import com.jared.flights.core.model.Airport
import com.jared.flights.core.model.DelayRules
import com.jared.flights.core.model.DelaySeverity
import com.jared.flights.core.model.Flight
import com.jared.flights.core.model.FlightStatus
import com.jared.flights.core.model.FlightTimes
import com.jared.flights.core.model.progressAt
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

private val TimeFormat = DateTimeFormatter.ofPattern("HH:mm")
private val DateFormat = DateTimeFormatter.ofPattern("EEE d MMM")

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
fun FlightCard(flight: Flight, now: Instant, modifier: Modifier = Modifier) {
    val statusColor = flightStatusColor(flight)
    val cardColor = MaterialTheme.colorScheme.surface

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = cardColor),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    ) {
        Column(Modifier.padding(16.dp)) {
            HeaderRow(flight, statusColor)
            Text(
                text = listOfNotNull(flight.airlineName, flight.aircraftType).joinToString(" · "),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(12.dp))
            RouteRow(flight.origin, flight.destination, flight.progressAt(now), cardColor)
            Spacer(Modifier.height(4.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                TimeColumn(
                    times = flight.departure,
                    airport = flight.origin,
                    dateLabel = dateLabel(flight.departure.best, flight.origin.timeZone, now),
                    flight = flight,
                    alignment = Alignment.Start,
                )
                TimeColumn(
                    times = flight.arrival,
                    airport = flight.destination,
                    dateLabel = arrivalDayOffset(flight),
                    flight = flight,
                    alignment = Alignment.End,
                )
            }
        }
    }
}

@Composable
private fun HeaderRow(flight: Flight, statusColor: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = "${flight.airlineIata} ${flight.flightNumber}",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.weight(1f),
        )
        Box(
            Modifier
                .size(8.dp)
                .background(statusColor, CircleShape),
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = flightStatusText(flight),
            style = MaterialTheme.typography.labelLarge,
            color = statusColor,
        )
    }
}

/**
 * FROM ──✈──── TO. The plane sits along the line at [progress]
 * (0 = departure gate, 1 = arrival gate), and the part already flown is drawn darker.
 */
@Composable
private fun RouteRow(origin: Airport, destination: Airport, progress: Float, cardColor: Color) {
    // Slide smoothly to the new position on each refresh, instead of jumping.
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = 1_000),
        label = "planeProgress",
    )
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(origin.iata, style = MaterialTheme.typography.headlineMedium)
        BoxWithConstraints(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 12.dp),
            contentAlignment = Alignment.CenterStart,
        ) {
            HorizontalDivider(color = MaterialTheme.colorScheme.outline)
            HorizontalDivider(
                modifier = Modifier.fillMaxWidth(animatedProgress),
                thickness = 1.5.dp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            val planeWidth = PlaneSize + PlanePadding * 2
            Icon(
                imageVector = FlightMeIcons.Flight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .offset(x = (maxWidth - planeWidth) * animatedProgress)
                    .background(cardColor)
                    .padding(horizontal = PlanePadding)
                    .size(PlaneSize)
                    .rotate(90f),
            )
        }
        Text(destination.iata, style = MaterialTheme.typography.headlineMedium)
    }
}

private val PlaneSize = 18.dp
private val PlanePadding = 6.dp

/**
 * The time to show (actual/estimated if known, in the airport's local time).
 * If it differs from the timetable, the timetable time is shown struck through.
 */
@Composable
private fun TimeColumn(
    times: FlightTimes,
    airport: Airport,
    dateLabel: String?,
    flight: Flight,
    alignment: Alignment.Horizontal,
) {
    val cancelled = flight.status == FlightStatus.CANCELLED
    val changed = !cancelled && times.best != times.scheduled
    // Each time is coloured by its own delay, not the flight's overall status.
    // Green only when we have a live time (estimate/actual); timetable-only stays neutral.
    val hasLiveTime = times.estimated != null || times.actual != null
    val timeColor = when {
        cancelled -> MaterialTheme.colorScheme.onSurfaceVariant
        !hasLiveTime -> MaterialTheme.colorScheme.onSurface
        else -> when (DelayRules.severityOf(times.delay)) {
            DelaySeverity.ON_TIME -> FlightMeTheme.statusColors.onTime
            DelaySeverity.MINOR -> FlightMeTheme.statusColors.minorDelay
            DelaySeverity.MAJOR -> FlightMeTheme.statusColors.majorDelay
        }
    }
    val textAlign = if (alignment == Alignment.End) TextAlign.End else TextAlign.Start

    Column(horizontalAlignment = alignment) {
        Row(verticalAlignment = Alignment.Bottom) {
            if (changed && alignment == Alignment.End) ScheduledStruck(times, airport)
            Text(
                text = times.best.atZone(airport.timeZone).format(TimeFormat),
                style = MaterialTheme.typography.titleLarge,
                color = timeColor,
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
        text = times.scheduled.atZone(airport.timeZone).format(TimeFormat),
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textDecoration = TextDecoration.LineThrough,
        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
    )
}

/** "Today", "Tomorrow", "Yesterday" or e.g. "Sat 27 Sep", in the airport's own time zone. */
@Composable
private fun dateLabel(time: Instant, zone: ZoneId, now: Instant): String {
    val date = time.atZone(zone).toLocalDate()
    val today = now.atZone(zone).toLocalDate()
    return when (ChronoUnit.DAYS.between(today, date)) {
        0L -> stringResource(R.string.date_today)
        1L -> stringResource(R.string.date_tomorrow)
        -1L -> stringResource(R.string.date_yesterday)
        else -> date.format(DateFormat)
    }
}

/** "+1" when the flight lands on a later local date than it left, e.g. overnight to New York. */
@Composable
private fun arrivalDayOffset(flight: Flight): String? {
    val departDate = flight.departure.best.atZone(flight.origin.timeZone).toLocalDate()
    val arriveDate = flight.arrival.best.atZone(flight.destination.timeZone).toLocalDate()
    val days = ChronoUnit.DAYS.between(departDate, arriveDate)
    return if (days != 0L) stringResource(R.string.date_day_offset, days) else null
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
            modifier = Modifier.padding(16.dp),
        )
    }
}
