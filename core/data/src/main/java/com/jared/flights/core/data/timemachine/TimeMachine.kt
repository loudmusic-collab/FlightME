package com.jared.flights.core.data.timemachine

import com.jared.flights.core.data.DevSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import java.time.Clock
import java.time.Duration
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Debug tool: moves the FM 100 test flight through its life one step at a time,
 * so screens (and later notifications) can be tested in minutes, not hours.
 * State is saved on the phone, so it survives an app restart.
 */
@Singleton
class TimeMachine @Inject constructor(
    private val devSettings: DevSettings,
    private val clock: Clock,
) {
    val state: Flow<TimeMachineState> = devSettings.timeMachine

    /** Add FM 100, due to leave the gate in 45 minutes. */
    suspend fun start() {
        devSettings.setTimeMachine(
            TimeMachineState(active = true).placedNow(),
        )
    }

    /** Move to the next stage. The flight's times are shifted so that stage is happening now. */
    suspend fun next() {
        val current = state.first()
        if (!current.active) return
        val nextStep = TimeMachineStep.entries.getOrNull(current.step.ordinal + 1) ?: return
        devSettings.setTimeMachine(current.copy(step = nextStep).placedNow())
    }

    /** Push every time not yet reached back by 15 minutes (a delay). */
    suspend fun addDelay() {
        val current = state.first()
        if (current.active) devSettings.setTimeMachine(current.copy(delayMinutes = current.delayMinutes + 15))
    }

    /** Move the departure to a different gate. */
    suspend fun changeGate() {
        val current = state.first()
        if (current.active) devSettings.setTimeMachine(current.copy(gateChanges = current.gateChanges + 1))
    }

    /** Take FM 100 off the list. */
    suspend fun stop() {
        devSettings.setTimeMachine(TimeMachineState(active = false))
    }

    /** Pick the timetable so that this state's step is happening right now. */
    private fun TimeMachineState.placedNow(): TimeMachineState {
        val gateOut = clock.instant()
            .minus(Duration.ofMinutes(step.minutesAfterGateOut))
            .minus(Duration.ofMinutes(delayMinutes))
        return copy(scheduledGateOutMillis = gateOut.toEpochMilli())
    }
}
