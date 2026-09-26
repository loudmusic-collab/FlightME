package com.jared.flights.feature.flightdetail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.jared.flights.core.data.FlightRepository
import com.jared.flights.core.data.SyncStatus
import com.jared.flights.core.data.timemachine.TimeMachine
import com.jared.flights.core.data.timemachine.TimeMachineState
import com.jared.flights.ui.TimeMachineActions
import com.jared.flights.core.model.Connection
import com.jared.flights.core.model.Flight
import com.jared.flights.navigation.FlightDetailRoute
import com.jared.flights.ui.clockTicker
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
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
        val syncStatus: SyncStatus,
        val now: Instant,
    ) : FlightDetailUiState
}

@HiltViewModel
class FlightDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: FlightRepository,
    private val timeMachine: TimeMachine,
    clock: Clock,
) : ViewModel() {

    private val flightId = savedStateHandle.toRoute<FlightDetailRoute>().flightId

    val uiState: StateFlow<FlightDetailUiState> =
        combine(
            repository.observeFlight(flightId),
            repository.observeTripFor(flightId),
            repository.observeSyncStatus(),
            clockTicker(clock),
        ) { flight, trip, sync, now ->
            if (flight == null) {
                FlightDetailUiState.NotFound
            } else {
                FlightDetailUiState.Success(
                    flight = flight,
                    previous = trip?.connectionBefore(flightId),
                    next = trip?.connectionAfter(flightId),
                    syncStatus = sync,
                    now = now,
                )
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = FlightDetailUiState.Loading,
        )

    /** "These aren't connecting": the next flight becomes its own trip. */
    /** The user's booking reference for this flight (saved on the phone only), or null. */
    val bookingReference: StateFlow<String?> = repository.observeBookingReference(flightId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun setBookingReference(reference: String?) {
        viewModelScope.launch { repository.setBookingReference(flightId, reference) }
    }

    /** Debug builds: the time machine, for when this is the FM 100 test flight. */
    val timeMachineState: StateFlow<TimeMachineState> = timeMachine.state
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TimeMachineState())

    val timeMachineActions = TimeMachineActions(
        start = { viewModelScope.launch { timeMachine.start() } },
        next = { viewModelScope.launch { timeMachine.next() } },
        addDelay = { viewModelScope.launch { timeMachine.addDelay() } },
        changeGate = { viewModelScope.launch { timeMachine.changeGate() } },
        stop = { viewModelScope.launch { timeMachine.stop() } },
    )

    fun splitFromNext(nextFlightId: String) {
        viewModelScope.launch { repository.splitTripBefore(nextFlightId) }
    }
}
