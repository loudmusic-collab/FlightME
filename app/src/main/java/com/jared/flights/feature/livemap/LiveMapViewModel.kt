package com.jared.flights.feature.livemap

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.jared.flights.core.data.FlightRepository
import com.jared.flights.core.data.LivePositionTracker
import com.jared.flights.core.model.Flight
import com.jared.flights.core.model.LivePosition
import com.jared.flights.navigation.LiveMapRoute
import com.jared.flights.ui.clockTicker
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.Clock
import java.time.Instant
import javax.inject.Inject

data class LiveMapUiState(val flight: Flight?, val position: LivePosition?, val now: Instant)

@HiltViewModel
class LiveMapViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    repository: FlightRepository,
    positions: LivePositionTracker,
    clock: Clock,
) : ViewModel() {

    private val flightId = savedStateHandle.toRoute<LiveMapRoute>().flightId

    val uiState: StateFlow<LiveMapUiState?> =
        combine(
            repository.observeFlight(flightId),
            // Asked for once a minute, only while this screen is visible.
            positions.observe(flightId),
            clockTicker(clock),
        ) { flight, position, now -> LiveMapUiState(flight, position, now) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
}
