package com.jared.flights.feature.myflights

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jared.flights.core.data.FlightRepository
import com.jared.flights.core.model.Trip
import com.jared.flights.ui.clockTicker
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.Clock
import java.time.Instant
import javax.inject.Inject

sealed interface MyFlightsUiState {
    data object Loading : MyFlightsUiState

    /**
     * Tracked flights as trips (a non-stop flight is a one-leg trip).
     * [now] moves the planes along their routes and labels dates "Today" / "Tomorrow".
     */
    data class Success(val trips: List<Trip>, val now: Instant) : MyFlightsUiState
}

@HiltViewModel
class MyFlightsViewModel @Inject constructor(
    repository: FlightRepository,
    clock: Clock,
) : ViewModel() {

    val uiState: StateFlow<MyFlightsUiState> =
        combine(repository.observeTrips(), clockTicker(clock)) { trips, now ->
            MyFlightsUiState.Success(trips, now)
        }.stateIn(
            scope = viewModelScope,
            // Stops ticking 5 s after the screen goes away (e.g. the app is in the background).
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = MyFlightsUiState.Loading,
        )
}
