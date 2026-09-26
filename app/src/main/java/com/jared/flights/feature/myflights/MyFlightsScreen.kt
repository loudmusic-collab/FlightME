package com.jared.flights.feature.myflights

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jared.flights.R
import com.jared.flights.core.data.SyncStatus
import com.jared.flights.core.designsystem.component.EmptyState
import com.jared.flights.core.designsystem.icon.FlightMeIcons
import com.jared.flights.core.designsystem.theme.FlightMeTheme
import com.jared.flights.core.model.Trip
import com.jared.flights.ui.OfflineNote
import com.jared.flights.ui.UpdatedNote
import java.time.Instant

/** A request (from the detail screen) to remove flights, e.g. after "Remove flight". */
data class RemoveRequest(val flightIds: List<String>, val label: String)

@Composable
fun MyFlightsScreen(
    onFlightClick: (flightId: String) -> Unit,
    snackbarHostState: SnackbarHostState,
    removeRequest: RemoveRequest?,
    onRemoveRequestHandled: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MyFlightsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()
    val justUpdated by viewModel.justUpdated.collectAsStateWithLifecycle()

    val refreshFailed = stringResource(R.string.refresh_failed_offline)
    val changeFailed = stringResource(R.string.change_failed_offline)
    val removedTemplate = stringResource(R.string.removed_message)
    val undo = stringResource(R.string.action_undo)

    LaunchedEffect(Unit) {
        viewModel.messages.collect { message ->
            when (message) {
                is MyFlightsMessage.Removed -> {
                    val result = snackbarHostState.showSnackbar(
                        message = removedTemplate.format(message.label),
                        actionLabel = undo,
                        duration = SnackbarDuration.Long,
                    )
                    if (result == SnackbarResult.ActionPerformed) viewModel.undoRemove(message.flightIds)
                }
                MyFlightsMessage.RefreshFailedOffline -> snackbarHostState.showSnackbar(refreshFailed)
                MyFlightsMessage.ChangeFailedOffline -> snackbarHostState.showSnackbar(changeFailed)
            }
        }
    }

    // "Remove flight" pressed on a detail screen: do it here, where Undo lives.
    LaunchedEffect(removeRequest) {
        removeRequest?.let {
            viewModel.remove(it.flightIds, it.label)
            onRemoveRequestHandled()
        }
    }

    // Pull down to refresh, on top of the automatic updates.
    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = viewModel::refresh,
        modifier = modifier.fillMaxSize(),
    ) {
        when (val state = uiState) {
            MyFlightsUiState.Loading -> Unit
            is MyFlightsUiState.Success ->
                if (state.trips.isEmpty()) {
                    // Scrollable so the pull gesture works here too.
                    Column(
                        Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState()),
                    ) {
                        EmptyState(
                            icon = FlightMeIcons.Flight,
                            title = stringResource(R.string.my_flights_empty_title),
                            message = stringResource(R.string.my_flights_empty_message),
                        )
                    }
                } else {
                    TripList(
                        trips = state.trips,
                        syncStatus = state.syncStatus,
                        justUpdated = justUpdated,
                        now = state.now,
                        onFlightClick = onFlightClick,
                        onRemove = viewModel::remove,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
        }
    }
}

@Composable
private fun TripList(
    trips: List<Trip>,
    syncStatus: SyncStatus,
    justUpdated: Boolean,
    now: Instant,
    onFlightClick: (flightId: String) -> Unit,
    onRemove: (flightIds: List<String>, label: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier,
        // Extra space at the bottom so the "+" button never covers the last card.
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Column(Modifier.padding(start = 4.dp, top = 8.dp, bottom = 4.dp)) {
                Text(
                    text = stringResource(R.string.tab_my_flights),
                    style = MaterialTheme.typography.headlineMedium,
                )
                OfflineNote(syncStatus, now, Modifier.padding(top = 4.dp))
                // Brief confirmation after a successful pull-to-refresh; fades in and out.
                AnimatedVisibility(
                    visible = justUpdated && syncStatus.isOnline,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically(),
                ) {
                    UpdatedNote(Modifier.padding(top = 4.dp))
                }
            }
        }
        items(trips, key = { it.id }) { trip ->
            val label = if (trip.isConnecting) {
                stringResource(R.string.trip_title, trip.origin.city, trip.destination.city)
            } else {
                trip.legs.single().let { "${it.airlineIata} ${it.flightNumber}" }
            }
            SwipeToRemove(onRemove = { onRemove(trip.legs.map { it.id }, label) }) {
                if (trip.isConnecting) {
                    TripCard(trip, now, onFlightClick)
                } else {
                    val flight = trip.legs.single()
                    FlightCard(flight, now, onClick = { onFlightClick(flight.id) })
                }
            }
        }
    }
}

/** Swipe a card to the left to remove it (with Undo afterwards). */
@Composable
private fun SwipeToRemove(onRemove: () -> Unit, content: @Composable () -> Unit) {
    val state = rememberSwipeToDismissBoxState()
    // The list remembers each card's swipe position. A card brought back by Undo would
    // otherwise come back still swiped off-screen (invisible), so put it back in place.
    LaunchedEffect(Unit) {
        if (state.currentValue != SwipeToDismissBoxValue.Settled) state.snapTo(SwipeToDismissBoxValue.Settled)
    }
    SwipeToDismissBox(
        state = state,
        enableDismissFromStartToEnd = false,
        onDismiss = { value -> if (value == SwipeToDismissBoxValue.EndToStart) onRemove() },
        backgroundContent = {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(FlightMeTheme.statusColors.cancelled, MaterialTheme.shapes.large)
                    .padding(end = 24.dp),
                contentAlignment = Alignment.CenterEnd,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(FlightMeIcons.Remove, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.action_remove), color = Color.White, style = MaterialTheme.typography.labelLarge)
                }
            }
        },
    ) {
        content()
    }
}
