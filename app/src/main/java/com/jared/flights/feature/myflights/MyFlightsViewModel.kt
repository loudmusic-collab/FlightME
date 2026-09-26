package com.jared.flights.feature.myflights

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jared.flights.core.data.FlightRepository
import com.jared.flights.core.data.SyncStatus
import com.jared.flights.core.model.Trip
import com.jared.flights.ui.clockTicker
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.Instant
import javax.inject.Inject

sealed interface MyFlightsUiState {
    data object Loading : MyFlightsUiState

    /**
     * Tracked flights as trips (a non-stop flight is a one-leg trip).
     * [now] moves the planes along their routes and labels dates "Today" / "Tomorrow".
     */
    data class Success(val trips: List<Trip>, val syncStatus: SyncStatus, val now: Instant) : MyFlightsUiState
}

/** One-off messages for the bar at the bottom of the screen. */
sealed interface MyFlightsMessage {
    /** "BA 283 removed" with an Undo button. */
    data class Removed(val label: String, val flightIds: List<String>) : MyFlightsMessage
    data object RefreshFailedOffline : MyFlightsMessage
    data object ChangeFailedOffline : MyFlightsMessage
}

@HiltViewModel
class MyFlightsViewModel @Inject constructor(
    private val repository: FlightRepository,
    clock: Clock,
) : ViewModel() {

    // Flights being removed are hidden straight away, before the server confirms.
    private val hiddenIds = MutableStateFlow<Set<String>>(emptySet())

    val uiState: StateFlow<MyFlightsUiState> =
        combine(
            repository.observeTrips(),
            repository.observeSyncStatus(),
            clockTicker(clock),
            hiddenIds,
        ) { trips, sync, now, hidden ->
            MyFlightsUiState.Success(trips.filter { t -> t.legs.none { it.id in hidden } }, sync, now)
        }.stateIn(
            scope = viewModelScope,
            // Stops ticking 5 s after the screen goes away (e.g. the app is in the background).
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = MyFlightsUiState.Loading,
        )

    private val _isRefreshing = MutableStateFlow(false)

    /** True while a pull-to-refresh is in progress (shows the spinner). */
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _justUpdated = MutableStateFlow(false)

    /** True for a few seconds after a successful pull-to-refresh ("Updated just now"). */
    val justUpdated: StateFlow<Boolean> = _justUpdated.asStateFlow()

    private val _messages = Channel<MyFlightsMessage>(Channel.BUFFERED)
    val messages: Flow<MyFlightsMessage> = _messages.receiveAsFlow()

    fun refresh() {
        if (_isRefreshing.value) return
        viewModelScope.launch {
            _isRefreshing.value = true
            val ok = repository.refresh()
            _isRefreshing.value = false
            if (ok) {
                _justUpdated.value = true
                delay(JUST_UPDATED_MILLIS)
                _justUpdated.value = false
            } else {
                _messages.send(MyFlightsMessage.RefreshFailedOffline)
            }
        }
    }

    /** Stop tracking [flightIds] (a flight, or every leg of a trip). [label] is e.g. "BA 283". */
    fun remove(flightIds: List<String>, label: String) {
        hiddenIds.update { it + flightIds }
        viewModelScope.launch {
            if (repository.untrack(flightIds)) {
                _messages.send(MyFlightsMessage.Removed(label, flightIds))
                // Once they're gone from the saved list, stop hiding them (so re-adding works).
                repository.observeTrackedFlights().first { list -> list.none { it.id in flightIds } }
                hiddenIds.update { it - flightIds.toSet() }
            } else {
                hiddenIds.update { it - flightIds.toSet() }
                _messages.send(MyFlightsMessage.ChangeFailedOffline)
            }
        }
    }

    fun undoRemove(flightIds: List<String>) {
        viewModelScope.launch {
            hiddenIds.update { it - flightIds.toSet() }
            if (!repository.track(flightIds)) _messages.send(MyFlightsMessage.ChangeFailedOffline)
        }
    }

    private companion object {
        const val JUST_UPDATED_MILLIS = 3_000L
    }
}
