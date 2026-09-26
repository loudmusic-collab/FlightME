package com.jared.flights.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.jared.flights.core.designsystem.icon.FlightMeIcons
import com.jared.flights.core.model.Airport

/**
 * FROM ──✈──── TO. The plane sits along the line at [progress]
 * (0 = departure gate, 1 = arrival gate), and the part already flown is drawn darker.
 * [backgroundColor] must match what's behind it, so the plane hides the line under it.
 */
@Composable
fun RouteLine(
    origin: Airport,
    destination: Airport,
    progress: Float,
    backgroundColor: Color,
    modifier: Modifier = Modifier,
) {
    // Slide smoothly to the new position on each refresh, instead of jumping.
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = 1_000),
        label = "planeProgress",
    )
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        Text(origin.iata, style = MaterialTheme.typography.headlineMedium)
        BoxWithConstraints(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 12.dp),
            contentAlignment = Alignment.CenterStart,
        ) {
            HorizontalDivider(color = MaterialTheme.colorScheme.outline)
            HorizontalDivider(
                modifier = Modifier.fillMaxWidth(animatedProgress),
                thickness = 1.5.dp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            val planeWidth = PlaneSize + PlanePadding * 2
            Icon(
                imageVector = FlightMeIcons.Flight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .offset(x = (maxWidth - planeWidth) * animatedProgress)
                    .background(backgroundColor)
                    .padding(horizontal = PlanePadding)
                    .size(PlaneSize)
                    .rotate(90f),
            )
        }
        Text(destination.iata, style = MaterialTheme.typography.headlineMedium)
    }
}

private val PlaneSize = 18.dp
private val PlanePadding = 6.dp
