package com.jared.flights.feature.flightdetail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.jared.flights.core.data.FlightRepository
import com.jared.flights.core.model.Connection
import com.jared.flights.core.model.Flight
import com.jared.flights.navigation.FlightDetailRoute
import com.jared.flights.ui.clockTicker
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.Clock
import java.time.Instant
import javax.inject.Inject

sealed interface FlightDetailUiState {
    data object Loading : FlightDetailUiState
    data object NotFound : FlightDetailUiState

    /** [previous] / [next] are the connections either side of this flight, if it's part of a trip. */
    data class Success(
        val flight: Flight,
        val previous: Connection?,
        val next: Connection?,
        val now: Instant,
    ) : FlightDetailUiState
}

@HiltViewModel
class FlightDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: FlightRepository,
    clock: Clock,
) : ViewModel() {

    private val flightId = savedStateHandle.toRoute<FlightDetailRoute>().flightId

    val uiState: StateFlow<FlightDetailUiState> =
        combine(
            repository.observeFlight(flightId),
            repository.observeTripFor(flightId),
            clockTicker(clock),
        ) { flight, trip, now ->
            if (flight == null) {
                FlightDetailUiState.NotFound
            } else {
                FlightDetailUiState.Success(
                    flight = flight,
                    previous = trip?.connectionBefore(flightId),
                    next = trip?.connectionAfter(flightId),
                    now = now,
                )
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = FlightDetailUiState.Loading,
        )

    /** "These aren't connecting": the next flight becomes its own trip. */
    fun splitFromNext(nextFlightId: String) = repository.splitTripBefore(nextFlightId)
}
