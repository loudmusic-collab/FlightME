package com.jared.flights.feature.addflight

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jared.flights.R
import com.jared.flights.core.designsystem.icon.FlightMeIcons
import com.jared.flights.core.model.Flight
import com.jared.flights.feature.myflights.FlightSummary
import com.jared.flights.ui.BookingReferenceDialog
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

private val ChipDateFormat = DateTimeFormatter.ofPattern("EEE d MMM")

/**
 * Add a flight: type a flight number, pick a date, search, then Add.
 * [onAdded] is called with e.g. "FM 123" once the flight is being tracked.
 */
@Composable
fun AddFlightScreen(
    onBack: () -> Unit,
    onAdded: (flightLabel: String) -> Unit,
    onOfflineMessage: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AddFlightViewModel = hiltViewModel(),
) {
    val query by viewModel.query.collectAsStateWithLifecycle()
    val date by viewModel.date.collectAsStateWithLifecycle()
    val search by viewModel.search.collectAsStateWithLifecycle()
    val trackedIds by viewModel.trackedIds.collectAsStateWithLifecycle()
    val adding by viewModel.adding.collectAsStateWithLifecycle()
    val keyboard = LocalSoftwareKeyboardController.current

    var confirming by remember { mutableStateOf<Flight?>(null) }
    var pickingDate by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.eventFlow.collect { event ->
            when (event) {
                is AddFlightEvent.Added -> onAdded(event.flightLabel)
                AddFlightEvent.AddFailedOffline -> onOfflineMessage()
            }
        }
    }

    Column(modifier.fillMaxSize()) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 4.dp, top = 4.dp)) {
            IconButton(onClick = onBack) {
                Icon(FlightMeIcons.ArrowBack, contentDescription = stringResource(R.string.action_back))
            }
        }
        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
        ) {
            Text(stringResource(R.string.add_flight_title), style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(20.dp))

            val runSearch = {
                keyboard?.hide()
                viewModel.search()
            }
            OutlinedTextField(
                value = query,
                onValueChange = viewModel::onQueryChange,
                label = { Text(stringResource(R.string.add_flight_number)) },
                placeholder = { Text(stringResource(R.string.add_flight_number_example)) },
                isError = search is SearchState.InvalidNumber,
                supportingText = if (search is SearchState.InvalidNumber) {
                    { Text(stringResource(R.string.add_flight_invalid)) }
                } else {
                    null
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Characters,
                    imeAction = ImeAction.Search,
                ),
                keyboardActions = KeyboardActions(onSearch = { runSearch() }),
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(12.dp))
            DateChips(
                today = viewModel.today,
                selected = date,
                onSelect = viewModel::onDateChange,
                onPickDate = { pickingDate = true },
            )

            Spacer(Modifier.height(12.dp))
            Button(
                onClick = runSearch,
                enabled = query.isNotBlank() && search !is SearchState.Searching,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.add_flight_search))
            }

            Spacer(Modifier.height(24.dp))
            when (val s = search) {
                SearchState.Idle, SearchState.InvalidNumber -> Unit
                SearchState.Searching -> Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                    CircularProgressIndicator(Modifier.size(32.dp), strokeWidth = 2.dp)
                }
                SearchState.Offline -> Message(stringResource(R.string.add_flight_offline))
                is SearchState.Results ->
                    if (s.flights.isEmpty()) {
                        Message(stringResource(R.string.add_flight_none, s.number.toString(), s.date.format(ChipDateFormat)))
                    } else {
                        s.flights.forEach { flight ->
                            ResultCard(
                                flight = flight,
                                alreadyAdded = flight.id in trackedIds,
                                enabled = !adding,
                                onAdd = { confirming = flight },
                            )
                            Spacer(Modifier.height(12.dp))
                        }
                    }
            }
        }
    }

    confirming?.let { flight ->
        BookingReferenceDialog(
            title = stringResource(R.string.add_flight_confirm, "${flight.airlineIata} ${flight.flightNumber}"),
            confirmLabel = stringResource(R.string.add_flight_add),
            initial = null,
            onConfirm = { reference ->
                confirming = null
                viewModel.add(flight, reference)
            },
            onDismiss = { confirming = null },
        )
    }

    if (pickingDate) {
        DatePickerSheet(
            initial = date,
            earliest = viewModel.today.minusDays(1),
            onPicked = {
                pickingDate = false
                viewModel.onDateChange(it)
            },
            onDismiss = { pickingDate = false },
        )
    }
}

/** [Today] [Tomorrow] [📅 Sat 27 Sep] */
@Composable
private fun DateChips(
    today: LocalDate,
    selected: LocalDate,
    onSelect: (LocalDate) -> Unit,
    onPickDate: () -> Unit,
) {
    val tomorrow = today.plusDays(1)
    val other = selected != today && selected != tomorrow
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FilterChip(
            selected = selected == today,
            onClick = { onSelect(today) },
            label = { Text(stringResource(R.string.date_today)) },
        )
        FilterChip(
            selected = selected == tomorrow,
            onClick = { onSelect(tomorrow) },
            label = { Text(stringResource(R.string.date_tomorrow)) },
        )
        FilterChip(
            selected = other,
            onClick = onPickDate,
            label = {
                Text(if (other) selected.format(ChipDateFormat) else stringResource(R.string.add_flight_other_date))
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DatePickerSheet(
    initial: LocalDate,
    earliest: LocalDate,
    onPicked: (LocalDate) -> Unit,
    onDismiss: () -> Unit,
) {
    // The date picker works in UTC midnight milliseconds.
    fun LocalDate.millis() = atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
    val earliestMillis = earliest.millis()
    val state = rememberDatePickerState(
        initialSelectedDateMillis = initial.millis(),
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long) = utcTimeMillis >= earliestMillis
        },
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    state.selectedDateMillis?.let {
                        onPicked(Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate())
                    } ?: onDismiss()
                },
            ) { Text(stringResource(R.string.action_ok)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
    ) {
        DatePicker(state)
    }
}

/** A search result: the usual flight summary, with [Add] or "✓ Added". */
@Composable
private fun ResultCard(flight: Flight, alreadyAdded: Boolean, enabled: Boolean, onAdd: () -> Unit) {
    val cardColor = MaterialTheme.colorScheme.surface
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = cardColor),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    ) {
        Column(Modifier.padding(16.dp)) {
            FlightSummary(flight, Instant.now(), cardColor)
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                if (alreadyAdded) {
                    Text(
                        text = stringResource(R.string.add_flight_already_added),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 10.dp),
                    )
                } else {
                    Button(onClick = onAdd, enabled = enabled) {
                        Icon(FlightMeIcons.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.add_flight_add))
                    }
                }
            }
        }
    }
}

@Composable
private fun Message(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}
