package com.jared.flights.core.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Developer-only switches (shown in debug builds only). Saved on the phone,
 * so they survive an app restart.
 */
@Singleton
class DevSettings @Inject constructor(
    @DevSettingsStore private val store: DataStore<Preferences>,
) {
    /** When on, the mock data source acts as if there's no signal. */
    val simulateOffline: Flow<Boolean> = store.data.map { it[SIMULATE_OFFLINE] ?: false }

    suspend fun setSimulateOffline(on: Boolean) {
        store.edit { it[SIMULATE_OFFLINE] = on }
    }

    private companion object {
        val SIMULATE_OFFLINE = booleanPreferencesKey("simulate_offline")
    }
}
