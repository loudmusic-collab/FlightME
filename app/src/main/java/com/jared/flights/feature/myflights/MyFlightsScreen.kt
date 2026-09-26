package com.jared.flights.feature.myflights

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jared.flights.R
import com.jared.flights.core.designsystem.component.EmptyState
import com.jared.flights.core.designsystem.icon.FlightMeIcons
import com.jared.flights.core.model.Trip
import java.time.Instant

@Composable
fun MyFlightsScreen(
    onFlightClick: (flightId: String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MyFlightsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    when (val state = uiState) {
        MyFlightsUiState.Loading -> Unit
        is MyFlightsUiState.Success ->
            if (state.trips.isEmpty()) {
                EmptyState(
                    icon = FlightMeIcons.Flight,
                    title = stringResource(R.string.my_flights_empty_title),
                    message = stringResource(R.string.my_flights_empty_message),
                    modifier = modifier,
                )
            } else {
                TripList(state.trips, state.now, onFlightClick, modifier)
            }
    }
}

@Composable
private fun TripList(
    trips: List<Trip>,
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
            Text(
                text = stringResource(R.string.tab_my_flights),
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(start = 4.dp, top = 8.dp, bottom = 4.dp),
            )
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
