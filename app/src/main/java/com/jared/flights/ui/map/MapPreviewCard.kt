package com.jared.flights.ui.map

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp

/**
 * A small, still map of the route and plane at the top of a flight's detail screen.
 * Tap it to open the full live map.
 */
@Composable
fun MapPreviewCard(data: FlightMapData, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val shape = MaterialTheme.shapes.large
    Box(
        modifier
            .fillMaxWidth()
            .height(180.dp)
            .clip(shape)
            .border(1.dp, MaterialTheme.colorScheme.outline, shape),
    ) {
        FlightMap(data = data, interactive = false, modifier = Modifier.fillMaxSize(), fitPaddingDp = 24)
        // Invisible layer on top that catches the tap (the map itself ignores touches here).
        Box(
            Modifier
                .fillMaxSize()
                .clickable(onClick = onClick),
        )
    }
}
