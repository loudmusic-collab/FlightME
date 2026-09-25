package com.jared.flights.feature.myflights

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jared.flights.core.data.FlightRepository
import com.jared.flights.core.model.Flight
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import java.time.Clock
import java.time.Instant
import javax.inject.Inject

sealed interface MyFlightsUiState {
    data object Loading : MyFlightsUiState

    /** [now] moves the planes along their routes and labels dates "Today" / "Tomorrow". */
    data class Success(val flights: List<Flight>, val now: Instant) : MyFlightsUiState
}

@HiltViewModel
class MyFlightsViewModel @Inject constructor(
    repository: FlightRepository,
    clock: Clock,
) : ViewModel() {

    val uiState: StateFlow<MyFlightsUiState> =
        combine(repository.observeTrackedFlights(), ticker(clock)) { flights, now ->
            MyFlightsUiState.Success(flights, now)
        }.stateIn(
            scope = viewModelScope,
            // Stops ticking 5 s after the screen goes away (e.g. the app is in the background).
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = MyFlightsUiState.Loading,
        )

    private fun ticker(clock: Clock): Flow<Instant> = flow {
        while (true) {
            emit(clock.instant())
            delay(TICK_MILLIS)
        }
    }

    private companion object {
        const val TICK_MILLIS = 30_000L
    }
}
