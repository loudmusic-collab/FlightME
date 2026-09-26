package com.jared.flights.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.jared.flights.R
import com.jared.flights.core.designsystem.icon.FlightMeIcons
import com.jared.flights.core.designsystem.theme.FlightMeTheme
import com.jared.flights.core.model.Connection
import com.jared.flights.core.model.ConnectionHealth
import java.time.Duration

/**
 *  ⏱  Planned 1h 25m                        (only when a delay changed it)
 *      At risk – 45m to change planes in Amsterdam
 */
@Composable
fun ConnectionRow(connection: Connection, modifier: Modifier = Modifier) {
    val color = connectionHealthColor(connection.health)
    val layoverChanged = connection.layover.minus(connection.plannedLayover).abs() >= Duration.ofMinutes(5)

    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = FlightMeIcons.Clock,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(20.dp),
        )
        Spacer(Modifier.width(12.dp))
        Column {
            // Top line (only when a delay changed it): "Planned 1h 25m", normal text.
            if (layoverChanged) {
                Text(
                    text = stringResource(R.string.connection_planned, formatDuration(connection.plannedLayover)),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 2.dp),
                )
            }
            // "At risk – 45m to change planes in Amsterdam": verdict and time coloured, the rest normal.
            val layoverText = formatDuration(connection.layover)
            val rest = if (connection.layover > Duration.ZERO) {
                stringResource(R.string.connection_time, layoverText, connection.airport.city)
            } else {
                stringResource(R.string.connection_no_time, connection.airport.city)
            }
            val colouredEnd = if (connection.layover > Duration.ZERO) layoverText.length else 0
            Text(
                text = buildAnnotatedString {
                    withStyle(SpanStyle(color = color, fontWeight = FontWeight.Medium)) {
                        append(connectionHealthLabel(connection.health))
                        append(" – ")
                        append(rest.substring(0, colouredEnd))
                    }
                    append(rest.substring(colouredEnd))
                },
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Composable
fun connectionHealthLabel(health: ConnectionHealth): String = stringResource(
    when (health) {
        ConnectionHealth.COMFORTABLE -> R.string.connection_comfortable
        ConnectionHealth.TIGHT -> R.string.connection_tight
        ConnectionHealth.AT_RISK -> R.string.connection_at_risk
        ConnectionHealth.MISSED -> R.string.connection_missed
    },
)

/** Green comfortable, amber tight, red at risk or missed (DECISIONS #32). */
@Composable
fun connectionHealthColor(health: ConnectionHealth): Color {
    val colors = FlightMeTheme.statusColors
    return when (health) {
        ConnectionHealth.COMFORTABLE -> colors.onTime
        ConnectionHealth.TIGHT -> colors.minorDelay
        ConnectionHealth.AT_RISK -> colors.majorDelay
        ConnectionHealth.MISSED -> colors.cancelled
    }
}
