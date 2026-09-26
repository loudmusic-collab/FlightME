package com.jared.flights.feature.addflight

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jared.flights.core.data.FlightRepository
import com.jared.flights.core.model.Flight
import com.jared.flights.core.model.FlightNumber
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.IOException
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject

/** What the search part of the screen is showing. */
sealed interface SearchState {
    data object Idle : SearchState
    data object Searching : SearchState
    /** What was typed isn't a flight number. */
    data object InvalidNumber : SearchState
    data object Offline : SearchState
    data class Results(val number: FlightNumber, val date: LocalDate, val flights: List<Flight>) : SearchState
}

sealed interface AddFlightEvent {
    /** Added: go back to My Flights and say "FM 123 added". */
    data class Added(val flightLabel: String) : AddFlightEvent
    data object AddFailedOffline : AddFlightEvent
}

@HiltViewModel
class AddFlightViewModel @Inject constructor(
    private val repository: FlightRepository,
    clock: Clock,
) : ViewModel() {

    val today: LocalDate = LocalDate.now(clock)

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val _date = MutableStateFlow(today)
    val date: StateFlow<LocalDate> = _date.asStateFlow()

    private val _search = MutableStateFlow<SearchState>(SearchState.Idle)
    val search: StateFlow<SearchState> = _search.asStateFlow()

    /** Ids already being tracked, so results can show "Added" instead of "Add". */
    val trackedIds: StateFlow<Set<String>> = repository.observeTrackedFlights()
        .map { flights -> flights.map { it.id }.toSet() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())

    private val _adding = MutableStateFlow(false)
    /** True while an add is in progress (disables the buttons). */
    val adding: StateFlow<Boolean> = _adding.asStateFlow()

    private val events = Channel<AddFlightEvent>(Channel.BUFFERED)
    val eventFlow: Flow<AddFlightEvent> = events.receiveAsFlow()

    fun onQueryChange(text: String) {
        _query.value = text
        if (_search.value is SearchState.InvalidNumber) _search.value = SearchState.Idle
    }

    fun onDateChange(date: LocalDate) {
        _date.value = date
        // A new date means the old results no longer apply.
        if (_search.value is SearchState.Results) search()
    }

    fun search() {
        val number = FlightNumber.parse(_query.value)
        if (number == null) {
            _search.value = SearchState.InvalidNumber
            return
        }
        val date = _date.value
        _search.value = SearchState.Searching
        viewModelScope.launch {
            _search.value = try {
                SearchState.Results(number, date, repository.search(number, date))
            } catch (e: IOException) {
                SearchState.Offline
            }
        }
    }

    fun add(flight: Flight, bookingReference: String?) {
        if (_adding.value) return
        viewModelScope.launch {
            _adding.update { true }
            val ok = repository.track(listOf(flight.id), bookingReference)
            _adding.update { false }
            events.send(
                if (ok) AddFlightEvent.Added("${flight.airlineIata} ${flight.flightNumber}")
                else AddFlightEvent.AddFailedOffline,
            )
        }
    }
}
