package com.jared.flights.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jared.flights.core.data.DevSettings
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val devSettings: DevSettings,
) : ViewModel() {

    val simulateOffline: StateFlow<Boolean> = devSettings.simulateOffline
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    fun setSimulateOffline(on: Boolean) {
        viewModelScope.launch { devSettings.setSimulateOffline(on) }
    }
}
