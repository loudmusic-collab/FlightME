package com.jared.flights.feature.flightdetail

import android.content.ClipData
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jared.flights.BuildConfig
import com.jared.flights.R
import com.jared.flights.core.designsystem.component.EmptyState
import com.jared.flights.core.designsystem.icon.FlightMeIcons
import com.jared.flights.core.designsystem.theme.FlightMeTheme
import com.jared.flights.core.data.SyncStatus
import com.jared.flights.core.data.timemachine.TIME_MACHINE_FLIGHT_ID
import com.jared.flights.core.model.Airport
import com.jared.flights.core.model.Connection
import com.jared.flights.core.model.Flight
import com.jared.flights.core.model.FlightStatus
import com.jared.flights.core.model.Milestone
import com.jared.flights.core.model.MilestoneType
import com.jared.flights.core.model.durationBetween
import com.jared.flights.core.model.progressAt
import com.jared.flights.core.model.timeline
import com.jared.flights.ui.AirportMapButton
import com.jared.flights.ui.BookingReferenceDialog
import com.jared.flights.ui.ConnectionRow
import com.jared.flights.ui.FlightStatusLabel
import com.jared.flights.ui.OfflineNote
import com.jared.flights.ui.RouteLine
import com.jared.flights.ui.TimeMachineControls
import com.jared.flights.ui.arrivalDayOffset
import com.jared.flights.ui.dateLabel
import com.jared.flights.ui.formatDuration
import com.jared.flights.ui.localTime
import com.jared.flights.ui.timeColor
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.Instant

@Composable
fun FlightDetailScreen(
    onBack: () -> Unit,
    onFlightClick: (flightId: String) -> Unit,
    /** Remove this flight: [flightIds] and a label like "BA 283" for the Undo message. */
    onRemove: (flightIds: List<String>, label: String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: FlightDetailViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val bookingReference by viewModel.bookingReference.collectAsStateWithLifecycle()
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
            is FlightDetailUiState.Success -> {
                Box(Modifier.weight(1f)) {
                    FlightDetailContent(
                        flight = state.flight,
                        previous = state.previous,
                        next = state.next,
                        syncStatus = state.syncStatus,
                        now = state.now,
                        onFlightClick = onFlightClick,
                        onSplit = viewModel::splitFromNext,
                        bookingReference = bookingReference,
                        onSetBookingReference = viewModel::setBookingReference,
                        onRemove = { onRemove(listOf(state.flight.id), "${state.flight.airlineIata} ${state.flight.flightNumber}") },
                    )
                }
                // Debug builds: step the FM 100 test flight from its own screen.
                if (BuildConfig.DEBUG && state.flight.id == TIME_MACHINE_FLIGHT_ID) {
                    val tmState by viewModel.timeMachineState.collectAsStateWithLifecycle()
                    Surface(color = MaterialTheme.colorScheme.surfaceVariant) {
                        Column(
                            Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 12.dp),
                        ) {
                            Text(stringResource(R.string.tm_title), style = MaterialTheme.typography.labelLarge)
                            TimeMachineControls(tmState, viewModel.timeMachineActions)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FlightDetailContent(
    flight: Flight,
    previous: Connection?,
    next: Connection?,
    syncStatus: SyncStatus,
    now: Instant,
    onFlightClick: (flightId: String) -> Unit,
    onSplit: (nextFlightId: String) -> Unit,
    bookingReference: String?,
    onSetBookingReference: (String?) -> Unit,
    onRemove: () -> Unit,
) {
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
        OfflineNote(syncStatus, now, Modifier.padding(top = 6.dp))

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

        // The user's own booking reference (saved on the phone only).
        Spacer(Modifier.height(16.dp))
        BookingReferenceRow(bookingReference, onSetBookingReference)

        // Connections either side (only for flights that are part of a trip)
        if (previous != null || next != null) {
            SectionDivider()
            Text(stringResource(R.string.detail_connection), style = MaterialTheme.typography.titleMedium)
            previous?.let { connection ->
                LinkedFlight(
                    label = stringResource(R.string.detail_previous_flight),
                    text = stringResource(
                        R.string.detail_flight_from,
                        "${connection.inbound.airlineIata} ${connection.inbound.flightNumber}",
                        connection.inbound.origin.city,
                    ),
                    onClick = { onFlightClick(connection.inbound.id) },
                )
                ConnectionRow(connection, Modifier.padding(vertical = 8.dp))
                ConnectionMapButton(connection)
            }
            next?.let { connection ->
                ConnectionRow(connection, Modifier.padding(vertical = 8.dp))
                ConnectionMapButton(connection)
                LinkedFlight(
                    label = stringResource(R.string.detail_next_flight),
                    text = stringResource(
                        R.string.detail_flight_to,
                        "${connection.outbound.airlineIata} ${connection.outbound.flightNumber}",
                        connection.outbound.destination.city,
                    ),
                    onClick = { onFlightClick(connection.outbound.id) },
                )
                TextButton(
                    onClick = { onSplit(connection.outbound.id) },
                    contentPadding = PaddingValues(horizontal = 0.dp),
                ) {
                    Text(stringResource(R.string.detail_split_trip))
                }
            }
        }

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
                airport = flight.origin,
                terminal = flight.departureTerminal,
                modifier = Modifier.weight(1f),
            )
            AirportInfo(
                title = stringResource(R.string.detail_arrival, (flight.divertedTo ?: flight.destination).iata),
                rows = listOf(
                    stringResource(R.string.detail_terminal) to flight.arrivalTerminal,
                    stringResource(R.string.detail_gate) to flight.arrivalGate,
                    stringResource(R.string.detail_baggage) to flight.baggageClaim,
                ),
                airport = flight.divertedTo ?: flight.destination,
                terminal = flight.arrivalTerminal,
                modifier = Modifier.weight(1f),
            )
        }

        // Stop tracking this flight (Undo is offered on My Flights).
        SectionDivider()
        TextButton(onClick = onRemove, contentPadding = PaddingValues(horizontal = 0.dp)) {
            val red = FlightMeTheme.statusColors.cancelled
            Icon(FlightMeIcons.Remove, contentDescription = null, tint = red, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.detail_remove_flight), color = red)
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
 * The steps as a vertical line of dots:
 *   ●  Boarding started 03:30  04:45
 *   │  30m boarding
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
        MilestoneType.BOARDING -> if (m.done) R.string.milestone_boarding_done else R.string.milestone_boarding
        MilestoneType.GATE_OUT -> if (m.done) R.string.milestone_gate_out_done else R.string.milestone_gate_out
        MilestoneType.TAKEOFF -> if (m.done) R.string.milestone_takeoff_done else R.string.milestone_takeoff
        MilestoneType.LANDING -> if (m.done) R.string.milestone_landing_done else R.string.milestone_landing
        MilestoneType.GATE_IN -> if (m.done) R.string.milestone_gate_in_done else R.string.milestone_gate_in
    },
)

/** Where a time comes from: "Actual", "Estimated", "Scheduled", or "Typical" (our own estimate). */
@Composable
private fun timeKind(m: Milestone, cancelled: Boolean): String = stringResource(
    when {
        cancelled -> R.string.time_scheduled
        m.isTypicalEstimate -> R.string.time_typical
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
        from.type == MilestoneType.BOARDING -> stringResource(R.string.segment_boarding, duration)
        from.type == MilestoneType.TAKEOFF -> stringResource(R.string.segment_in_air, duration)
        from.type == MilestoneType.GATE_OUT && to.type == MilestoneType.GATE_IN ->
            stringResource(R.string.segment_gate_to_gate, duration)
        else -> stringResource(R.string.segment_taxi, duration)
    }
}

@Composable
private fun AirportInfo(
    title: String,
    rows: List<Pair<String, String?>>,
    airport: Airport,
    terminal: String?,
    modifier: Modifier = Modifier,
) {
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
        AirportMapButton(airport, terminal)
    }
}

/**
 * "Map of Amsterdam airport": where you change planes. Uses the terminal you
 * arrive at, since that's where you start walking from.
 */
@Composable
private fun ConnectionMapButton(connection: Connection) {
    AirportMapButton(
        airport = connection.airport,
        terminal = connection.inbound.arrivalTerminal,
        label = stringResource(R.string.airport_map_of, connection.airport.city),
    )
}

/**
 * "Booking reference  X7K2QP  [copy] [edit]", or "+ Add booking reference".
 * Stored on the phone only (DECISIONS #40).
 */
@Composable
private fun BookingReferenceRow(reference: String?, onSet: (String?) -> Unit) {
    var editing by rememberSaveable { mutableStateOf(false) }
    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()

    if (reference == null) {
        TextButton(onClick = { editing = true }, contentPadding = PaddingValues(horizontal = 0.dp)) {
            Icon(FlightMeIcons.Add, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.booking_reference_add))
        }
    } else {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                SmallText(stringResource(R.string.booking_reference))
                Text(reference, style = MaterialTheme.typography.titleMedium)
            }
            IconButton(
                onClick = {
                    scope.launch {
                        clipboard.setClipEntry(ClipEntry(ClipData.newPlainText("Booking reference", reference)))
                    }
                },
            ) {
                Icon(FlightMeIcons.Copy, contentDescription = stringResource(R.string.action_copy), modifier = Modifier.size(20.dp))
            }
            IconButton(onClick = { editing = true }) {
                Icon(FlightMeIcons.Edit, contentDescription = stringResource(R.string.action_edit), modifier = Modifier.size(20.dp))
            }
        }
    }

    if (editing) {
        BookingReferenceDialog(
            title = stringResource(R.string.booking_reference),
            confirmLabel = stringResource(R.string.action_save),
            initial = reference,
            onConfirm = {
                onSet(it)
                editing = false
            },
            onDismiss = { editing = false },
        )
    }
}

/** "Next flight · KL 643 to New York  →", tappable. */
@Composable
private fun LinkedFlight(label: String, text: String, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            SmallText(label)
            Text(text, style = MaterialTheme.typography.bodyLarge)
        }
        Icon(
            imageVector = FlightMeIcons.ArrowBack,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .size(20.dp)
                .rotate(180f),
        )
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
