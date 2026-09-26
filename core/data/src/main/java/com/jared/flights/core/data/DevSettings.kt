package com.jared.flights.core.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import com.jared.flights.core.data.timemachine.TimeMachineState
import com.jared.flights.core.data.timemachine.TimeMachineStep
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
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
    val simulateOffline: Flow<Boolean> = store.data.map { it[SIMULATE_OFFLINE] ?: false }.distinctUntilChanged()

    suspend fun setSimulateOffline(on: Boolean) {
        store.edit { it[SIMULATE_OFFLINE] = on }
    }

    /** The time machine's current state (see [com.jared.flights.core.data.timemachine.TimeMachine]). */
    val timeMachine: Flow<TimeMachineState> = store.data.map { prefs ->
        TimeMachineState(
            active = prefs[TM_ACTIVE] ?: false,
            step = TimeMachineStep.entries.find { it.name == prefs[TM_STEP] } ?: TimeMachineStep.SCHEDULED,
            delayMinutes = prefs[TM_DELAY] ?: 0,
            gateChanges = prefs[TM_GATE_CHANGES] ?: 0,
            scheduledGateOutMillis = prefs[TM_GATE_OUT] ?: 0,
        )
    }.distinctUntilChanged()

    suspend fun setTimeMachine(state: TimeMachineState) {
        store.edit {
            it[TM_ACTIVE] = state.active
            it[TM_STEP] = state.step.name
            it[TM_DELAY] = state.delayMinutes
            it[TM_GATE_CHANGES] = state.gateChanges
            it[TM_GATE_OUT] = state.scheduledGateOutMillis
        }
    }

    /** Turn the time-machine flight on/off without losing where it was (for remove + undo). */
    suspend fun setTimeMachineActive(active: Boolean) {
        store.edit { it[TM_ACTIVE] = active }
    }

    // ---- Mock "server" tracking state (stands in for the user's tracks until Phase 2) ----

    /** Ids of flights added through search (e.g. "FM123_2026-09-27"). */
    val mockAddedIds: Flow<Set<String>> = store.data.map { it[MOCK_ADDED] ?: emptySet() }.distinctUntilChanged()

    /** Ids of built-in mock flights the user removed. */
    val mockRemovedIds: Flow<Set<String>> = store.data.map { it[MOCK_REMOVED] ?: emptySet() }.distinctUntilChanged()

    suspend fun updateMockAddedIds(change: (Set<String>) -> Set<String>) {
        store.edit { it[MOCK_ADDED] = change(it[MOCK_ADDED] ?: emptySet()) }
    }

    suspend fun updateMockRemovedIds(change: (Set<String>) -> Set<String>) {
        store.edit { it[MOCK_REMOVED] = change(it[MOCK_REMOVED] ?: emptySet()) }
    }

    /** Back to the starting set of test flights: removed ones return, added ones go. */
    suspend fun resetMockTracking() {
        store.edit {
            it.remove(MOCK_ADDED)
            it.remove(MOCK_REMOVED)
        }
    }

    private companion object {
        val MOCK_ADDED = stringSetPreferencesKey("mock_added_ids")
        val MOCK_REMOVED = stringSetPreferencesKey("mock_removed_ids")
        val SIMULATE_OFFLINE = booleanPreferencesKey("simulate_offline")
        val TM_ACTIVE = booleanPreferencesKey("tm_active")
        val TM_STEP = stringPreferencesKey("tm_step")
        val TM_DELAY = longPreferencesKey("tm_delay_minutes")
        val TM_GATE_CHANGES = intPreferencesKey("tm_gate_changes")
        val TM_GATE_OUT = longPreferencesKey("tm_scheduled_gate_out")
    }
}
