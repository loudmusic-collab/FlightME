package com.jared.flights.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.jared.flights.R
import com.jared.flights.core.data.timemachine.TimeMachineState
import com.jared.flights.core.data.timemachine.TimeMachineStep

/** What the time-machine buttons do. */
class TimeMachineActions(
    val start: () -> Unit,
    val next: () -> Unit,
    val addDelay: () -> Unit,
    val changeGate: () -> Unit,
    val stop: () -> Unit,
)

/**
 * Debug-only controls for the FM 100 test flight (DECISIONS #38):
 * "Now: Boarding" then [Next step ▶] [+15 min delay] [Change gate] [Remove].
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TimeMachineControls(
    state: TimeMachineState,
    actions: TimeMachineActions,
    modifier: Modifier = Modifier,
) {
    Column(modifier) {
        if (!state.active) {
            Text(
                text = stringResource(R.string.tm_intro),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Button(onClick = actions.start) { Text(stringResource(R.string.tm_start)) }
            return@Column
        }

        val isLast = state.step == TimeMachineStep.entries.last()
        Text(
            text = stringResource(R.string.tm_now, stepLabel(state.step)) +
                if (state.delayMinutes > 0) " · " + stringResource(R.string.tm_delayed_by, state.delayMinutes) else "",
            style = MaterialTheme.typography.bodyLarge,
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = actions.next, enabled = !isLast) { Text(stringResource(R.string.tm_next)) }
            OutlinedButton(onClick = actions.addDelay, enabled = !isLast) { Text(stringResource(R.string.tm_delay)) }
            OutlinedButton(onClick = actions.changeGate, enabled = !isLast) { Text(stringResource(R.string.tm_gate)) }
            OutlinedButton(onClick = actions.start) { Text(stringResource(R.string.tm_restart)) }
            OutlinedButton(onClick = actions.stop) { Text(stringResource(R.string.tm_remove)) }
        }
    }
}

@Composable
private fun stepLabel(step: TimeMachineStep): String = stringResource(
    when (step) {
        TimeMachineStep.SCHEDULED -> R.string.tm_step_scheduled
        TimeMachineStep.BOARDING -> R.string.tm_step_boarding
        TimeMachineStep.LEFT_GATE -> R.string.tm_step_left_gate
        TimeMachineStep.TOOK_OFF -> R.string.tm_step_took_off
        TimeMachineStep.HALFWAY -> R.string.tm_step_halfway
        TimeMachineStep.LANDED -> R.string.tm_step_landed
        TimeMachineStep.AT_GATE -> R.string.tm_step_at_gate
    },
)
