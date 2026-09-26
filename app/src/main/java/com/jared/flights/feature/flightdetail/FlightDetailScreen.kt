package com.jared.flights.feature.flightdetail

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jared.flights.R
import com.jared.flights.core.designsystem.component.EmptyState
import com.jared.flights.core.designsystem.icon.FlightMeIcons
import com.jared.flights.core.designsystem.theme.FlightMeTheme
import com.jared.flights.core.model.Flight
import com.jared.flights.core.model.FlightStatus
import com.jared.flights.core.model.Milestone
import com.jared.flights.core.model.MilestoneType
import com.jared.flights.core.model.durationBetween
import com.jared.flights.core.model.progressAt
import com.jared.flights.core.model.timeline
import com.jared.flights.ui.FlightStatusLabel
import com.jared.flights.ui.RouteLine
import com.jared.flights.ui.arrivalDayOffset
import com.jared.flights.ui.dateLabel
import com.jared.flights.ui.formatDuration
import com.jared.flights.ui.localTime
import com.jared.flights.ui.timeColor
import java.time.Duration
import java.time.Instant

@Composable
fun FlightDetailScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: FlightDetailViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    Column(modifier.fillMaxSize()) {
        IconButton(onClick = onBack, modifier = Modifier.padding(start = 4.dp, top = 4.dp)) {
            Icon(FlightMeIcons.ArrowBack, contentDescription = stringResource(R.string.action_back))
        }
        when (val state = uiState) {
            FlightDetailUiState.Loading -> Unit
            FlightDetailUiState.NotFound -> EmptyState(
                icon = FlightMeIcons.Flight,
                title = stringResource(R.string.detail_not_found_title),
                message = stringResource(R.string.detail_not_found_message),
            )
            is FlightDetailUiState.Success -> FlightDetailContent(state.flight, state.now)
        }
    }
}

@Composable
private fun FlightDetailContent(flight: Flight, now: Instant) {
    val cancelled = flight.status == FlightStatus.CANCELLED
    Column(
        Modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(bottom = 32.dp),
    ) {
        // Header
        Text("${flight.airlineIata} ${flight.flightNumber}", style = MaterialTheme.typography.headlineMedium)
        Text(
            text = listOfNotNull(flight.airlineName, flight.aircraftType).joinToString(" · "),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(8.dp))
        FlightStatusLabel(flight)

        when {
            cancelled -> Banner(stringResource(R.string.detail_cancelled_banner))
            flight.status == FlightStatus.DIVERTED && flight.divertedTo != null -> Banner(
                stringResource(R.string.detail_diverted_banner, flight.divertedTo!!.name, flight.divertedTo!!.iata),
            )
        }

        // Route
        Spacer(Modifier.height(24.dp))
        RouteLine(
            origin = flight.origin,
            destination = flight.destination,
            progress = flight.progressAt(now),
            backgroundColor = MaterialTheme.colorScheme.background,
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            SmallText(
                listOf(flight.origin.city, dateLabel(flight.departure.best, flight.origin.timeZone, now))
                    .joinToString(" · "),
            )
            SmallText(
                listOfNotNull(flight.destination.city, arrivalDayOffset(flight)).joinToString(" · "),
                textAlign = TextAlign.End,
            )
        }
        Spacer(Modifier.height(4.dp))
        SmallText(journeyFacts(flight))

        // Timeline
        SectionDivider()
        Text(stringResource(R.string.detail_timeline), style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(16.dp))
        Timeline(flight.timeline, cancelled)

        // Terminals, gates, baggage
        SectionDivider()
        Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
            AirportInfo(
                title = stringResource(R.string.detail_departure, flight.origin.iata),
                rows = listOf(
                    stringResource(R.string.detail_terminal) to flight.departureTerminal,
                    stringResource(R.string.detail_gate) to flight.departureGate,
                ),
                modifier = Modifier.weight(1f),
            )
            AirportInfo(
                title = stringResource(R.string.detail_arrival, (flight.divertedTo ?: flight.destination).iata),
                rows = listOf(
                    stringResource(R.string.detail_terminal) to flight.arrivalTerminal,
                    stringResource(R.string.detail_gate) to flight.arrivalGate,
                    stringResource(R.string.detail_baggage) to flight.baggageClaim,
                ),
                modifier = Modifier.weight(1f),
            )
        }
    }
}

/** "8h flight · New York is 5h behind" */
@Composable
private fun journeyFacts(flight: Flight): String {
    val duration = formatDuration(Duration.between(flight.departure.scheduled, flight.arrival.scheduled))
    val at = flight.departure.scheduled
    val offsetSeconds = flight.destination.timeZone.rules.getOffset(at).totalSeconds -
        flight.origin.timeZone.rules.getOffset(at).totalSeconds
    val offset = formatDuration(Duration.ofSeconds(offsetSeconds.toLong()))
    val zoneText = when {
        offsetSeconds == 0 -> stringResource(R.string.detail_same_time_zone)
        offsetSeconds < 0 -> stringResource(R.string.detail_time_behind, flight.destination.city, offset)
        else -> stringResource(R.string.detail_time_ahead, flight.destination.city, offset)
    }
    return stringResource(R.string.detail_flight_length, duration) + " · " + zoneText
}

/**
 * The four steps as a vertical line of dots:
 *   ●  Left gate        04:00  05:15
 *   │  12m taxi
 *   ○  Takes off        04:12  05:27
 * Filled dot = happened, open dot = still to come.
 */
@Composable
private fun Timeline(milestones: List<Milestone>, cancelled: Boolean) {
    milestones.forEachIndexed { index, milestone ->
        TimelineRow(milestone, next = milestones.getOrNull(index + 1), cancelled = cancelled)
    }
}

@Composable
private fun TimelineRow(milestone: Milestone, next: Milestone?, cancelled: Boolean) {
    val times = milestone.times
    val zone = milestone.airport.timeZone
    val changed = !cancelled && times.best != times.scheduled

    Row(Modifier.height(IntrinsicSize.Min)) {
        // The rail: a dot, then a line down to the next dot.
        Column(
            Modifier
                .width(20.dp)
                .fillMaxHeight(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(5.dp))
            TimelineDot(done = milestone.done)
            if (next != null) {
                Box(
                    Modifier
                        .weight(1f)
                        .width(if (next.done) 1.5.dp else 1.dp)
                        .background(
                            if (next.done) MaterialTheme.colorScheme.onSurfaceVariant
                            else MaterialTheme.colorScheme.outline,
                        ),
                )
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(
            Modifier
                .weight(1f)
                .padding(bottom = if (next != null) 20.dp else 0.dp),
        ) {
            Row {
                Column(Modifier.weight(1f)) {
                    Text(milestoneLabel(milestone), style = MaterialTheme.typography.bodyLarge)
                    SmallText("${milestone.airport.iata} · ${timeKind(milestone, cancelled)}")
                }
                Row(verticalAlignment = Alignment.Bottom) {
                    if (changed) {
                        Text(
                            text = times.scheduled.localTime(zone),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textDecoration = TextDecoration.LineThrough,
                            modifier = Modifier.padding(end = 8.dp, bottom = 1.dp),
                        )
                    }
                    Text(
                        text = times.best.localTime(zone),
                        style = MaterialTheme.typography.titleMedium,
                        color = timeColor(times, cancelled),
                        textDecoration = if (cancelled) TextDecoration.LineThrough else null,
                    )
                }
            }
            if (next != null) {
                Spacer(Modifier.height(6.dp))
                SmallText(segmentLabel(milestone, next))
            }
        }
    }
}

@Composable
private fun TimelineDot(done: Boolean) {
    val modifier = Modifier.size(12.dp)
    if (done) {
        Box(modifier.background(MaterialTheme.colorScheme.onSurface, CircleShape))
    } else {
        Box(modifier.border(1.5.dp, MaterialTheme.colorScheme.onSurfaceVariant, CircleShape))
    }
}

/** "Leaves gate" before it happens, "Left gate" after. */
@Composable
private fun milestoneLabel(m: Milestone): String = stringResource(
    when (m.type) {
        MilestoneType.GATE_OUT -> if (m.done) R.string.milestone_gate_out_done else R.string.milestone_gate_out
        MilestoneType.TAKEOFF -> if (m.done) R.string.milestone_takeoff_done else R.string.milestone_takeoff
        MilestoneType.LANDING -> if (m.done) R.string.milestone_landing_done else R.string.milestone_landing
        MilestoneType.GATE_IN -> if (m.done) R.string.milestone_gate_in_done else R.string.milestone_gate_in
    },
)

/** Where a time comes from: "Actual", "Estimated" or "Scheduled". */
@Composable
private fun timeKind(m: Milestone, cancelled: Boolean): String = stringResource(
    when {
        cancelled -> R.string.time_scheduled
        m.times.actual != null -> R.string.time_actual
        m.times.estimated != null -> R.string.time_estimated
        else -> R.string.time_scheduled
    },
)

/** What happens between two steps: "12m taxi", "7h 36m in the air". */
@Composable
private fun segmentLabel(from: Milestone, to: Milestone): String {
    val duration = formatDuration(durationBetween(from, to))
    return when {
        from.type == MilestoneType.TAKEOFF -> stringResource(R.string.segment_in_air, duration)
        from.type == MilestoneType.GATE_OUT && to.type == MilestoneType.GATE_IN ->
            stringResource(R.string.segment_gate_to_gate, duration)
        else -> stringResource(R.string.segment_taxi, duration)
    }
}

@Composable
private fun AirportInfo(title: String, rows: List<Pair<String, String?>>, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        rows.forEach { (label, value) ->
            Row(Modifier.padding(vertical = 4.dp)) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = value ?: stringResource(R.string.detail_not_announced),
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
        }
    }
}

/** Outlined red notice for cancelled or diverted flights. */
@Composable
private fun Banner(text: String) {
    val color = FlightMeTheme.statusColors.cancelled
    Spacer(Modifier.height(16.dp))
    Text(
        text = text,
        style = MaterialTheme.typography.bodyLarge,
        color = color,
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, color, MaterialTheme.shapes.medium)
            .padding(horizontal = 16.dp, vertical = 12.dp),
    )
}

@Composable
private fun SectionDivider() {
    HorizontalDivider(Modifier.padding(vertical = 24.dp), color = MaterialTheme.colorScheme.outline)
}

@Composable
private fun SmallText(text: String, textAlign: TextAlign? = null) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = textAlign,
    )
}
