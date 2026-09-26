package com.jared.flights.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jared.flights.core.data.DevSettings
import com.jared.flights.core.data.timemachine.TimeMachine
import com.jared.flights.core.data.timemachine.TimeMachineState
import com.jared.flights.ui.TimeMachineActions
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val devSettings: DevSettings,
    private val timeMachine: TimeMachine,
) : ViewModel() {

    val simulateOffline: StateFlow<Boolean> = devSettings.simulateOffline
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    val timeMachineState: StateFlow<TimeMachineState> = timeMachine.state
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TimeMachineState())

    val timeMachineActions = TimeMachineActions(
        start = { viewModelScope.launch { timeMachine.start() } },
        next = { viewModelScope.launch { timeMachine.next() } },
        addDelay = { viewModelScope.launch { timeMachine.addDelay() } },
        changeGate = { viewModelScope.launch { timeMachine.changeGate() } },
        stop = { viewModelScope.launch { timeMachine.stop() } },
    )

    fun setSimulateOffline(on: Boolean) {
        viewModelScope.launch { devSettings.setSimulateOffline(on) }
    }
}
