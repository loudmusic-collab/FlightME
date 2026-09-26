package com.jared.flights.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jared.flights.core.data.AppPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Small bits of app-wide state for the shell (e.g. the one-time notification prompt). */
@HiltViewModel
class AppShellViewModel @Inject constructor(
    private val prefs: AppPreferences,
) : ViewModel() {

    /** True if we haven't yet asked about notifications (we ask only once, DECISIONS #42). */
    suspend fun shouldAskForNotifications(): Boolean = !prefs.notificationPromptShown()

    fun markNotificationPromptShown() {
        viewModelScope.launch { prefs.setNotificationPromptShown() }
    }
}
