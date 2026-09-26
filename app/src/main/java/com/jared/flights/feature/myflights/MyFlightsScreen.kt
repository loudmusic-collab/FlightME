package com.jared.flights.feature.myflights

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jared.flights.R
import com.jared.flights.core.designsystem.component.EmptyState
import com.jared.flights.core.designsystem.icon.FlightMeIcons
import com.jared.flights.core.data.SyncStatus
import com.jared.flights.core.model.Trip
import com.jared.flights.ui.OfflineNote
import com.jared.flights.ui.UpdatedNote
import java.time.Instant

@Composable
fun MyFlightsScreen(
    onFlightClick: (flightId: String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MyFlightsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()
    val justUpdated by viewModel.justUpdated.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val offlineMessage = stringResource(R.string.refresh_failed_offline)

    LaunchedEffect(Unit) {
        viewModel.refreshFailed.collect { snackbarHostState.showSnackbar(offlineMessage) }
    }

    Box(modifier.fillMaxSize()) {
        // Pull down to refresh, on top of the automatic updates.
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = viewModel::refresh,
            modifier = Modifier.fillMaxSize(),
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
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
            }
        }
        SnackbarHost(snackbarHostState, Modifier.align(Alignment.BottomCenter))
    }
}

@Composable
private fun TripList(
    trips: List<Trip>,
    syncStatus: SyncStatus,
    justUpdated: Boolean,
    now: Instant,
    onFlightClick: (flightId: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
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
            if (trip.isConnecting) {
                TripCard(trip, now, onFlightClick)
            } else {
                val flight = trip.legs.single()
                FlightCard(flight, now, onClick = { onFlightClick(flight.id) })
            }
        }
    }
}
