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
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
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

@HiltViewModel
class MyFlightsViewModel @Inject constructor(
    private val repository: FlightRepository,
    clock: Clock,
) : ViewModel() {

    val uiState: StateFlow<MyFlightsUiState> =
        combine(repository.observeTrips(), repository.observeSyncStatus(), clockTicker(clock)) { trips, sync, now ->
            MyFlightsUiState.Success(trips, sync, now)
        }.stateIn(
            scope = viewModelScope,
            // Stops ticking 5 s after the screen goes away (e.g. the app is in the background).
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = MyFlightsUiState.Loading,
        )

    private val _isRefreshing = MutableStateFlow(false)

    /** True while a pull-to-refresh is in progress (shows the spinner). */
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val refreshFailures = Channel<Unit>(Channel.CONFLATED)

    /** Fires once each time a pull-to-refresh fails (to show "Couldn't update"). */
    val refreshFailed: Flow<Unit> = refreshFailures.receiveAsFlow()

    private val _justUpdated = MutableStateFlow(false)

    /** True for a few seconds after a successful pull-to-refresh ("Updated just now"). */
    val justUpdated: StateFlow<Boolean> = _justUpdated.asStateFlow()

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
                refreshFailures.send(Unit)
            }
        }
    }

    private companion object {
        const val JUST_UPDATED_MILLIS = 3_000L
    }
}
