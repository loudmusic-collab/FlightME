package com.jared.flights.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.jared.flights.R
import com.jared.flights.core.designsystem.theme.FlightMeTheme
import com.jared.flights.core.data.SyncStatus
import java.time.Duration
import java.time.Instant

/**
 * "○ Offline · last updated 12m ago". Shown only while offline;
 * shows nothing when connected.
 */
@Composable
fun OfflineNote(status: SyncStatus, now: Instant, modifier: Modifier = Modifier) {
    if (status.isOnline) return
    val text = when (val last = status.lastSuccessAt) {
        null -> stringResource(R.string.offline_never_updated)
        else -> {
            val age = Duration.between(last, now)
            if (age < Duration.ofMinutes(1)) {
                stringResource(R.string.offline_updated_just_now)
            } else {
                stringResource(R.string.offline_updated_ago, formatDuration(age))
            }
        }
    }
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        // Open circle = not connected.
        Box(
            Modifier
                .size(8.dp)
                .border(1.5.dp, MaterialTheme.colorScheme.onSurfaceVariant, CircleShape),
        )
        Spacer(Modifier.width(6.dp))
        NoteText(text)
    }
}

/** "● Updated just now", shown briefly after a successful pull-to-refresh. */
@Composable
fun UpdatedNote(modifier: Modifier = Modifier) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        // Filled green dot = connected and fresh.
        Box(
            Modifier
                .size(8.dp)
                .background(FlightMeTheme.statusColors.onTime, CircleShape),
        )
        Spacer(Modifier.width(6.dp))
        NoteText(stringResource(R.string.updated_just_now))
    }
}

@Composable
private fun NoteText(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}
