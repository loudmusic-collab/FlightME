package com.jared.flights.feature.livemap

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jared.flights.R
import com.jared.flights.core.designsystem.icon.FlightMeIcons
import com.jared.flights.core.model.Flight
import com.jared.flights.core.model.LivePosition
import com.jared.flights.ui.FlightStatusLabel
import com.jared.flights.ui.formatDuration
import com.jared.flights.ui.map.FlightMap
import com.jared.flights.ui.map.FlightMapData
import com.jared.flights.ui.map.altitudeText
import com.jared.flights.ui.map.speedText
import java.time.Duration
import java.time.Instant

/** Full-screen live map for one flight, with a readout panel at the bottom. */
@Composable
fun LiveMapScreen(
    onBack: () -> Unit,
    viewModel: LiveMapViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val flight = state?.flight
    val from = flight?.origin?.location
    val to = (flight?.divertedTo ?: flight?.destination)?.location

    Box(Modifier.fillMaxSize()) {
        if (flight != null && from != null && to != null) {
            FlightMap(
                data = FlightMapData(from, to, state?.position),
                interactive = true,
                modifier = Modifier.fillMaxSize(),
                fitPaddingDp = 64,
            )
            Readout(
                flight = flight,
                position = state?.position,
                now = state?.now ?: Instant.now(),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp),
            )
        }
        // Round back button floating over the map.
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 2.dp,
            modifier = Modifier.padding(12.dp),
        ) {
            IconButton(onClick = onBack) {
                Icon(FlightMeIcons.ArrowBack, contentDescription = stringResource(R.string.action_back))
            }
        }
    }
}

/**
 *  BA 283 · LHR → LAX                ● In the air
 *  Altitude     Speed        Lands in
 *  36,000 ft    890 km/h     3h 12m
 */
@Composable
private fun Readout(flight: Flight, position: LivePosition?, now: Instant, modifier: Modifier = Modifier) {
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        shadowElevation = 2.dp,
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp), // keep clear of the map credit
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "${flight.airlineIata} ${flight.flightNumber} · ${flight.origin.iata} → ${(flight.divertedTo ?: flight.destination).iata}",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                )
                FlightStatusLabel(flight)
            }
            Spacer(Modifier.height(12.dp))
            if (position == null || position.isOnGround) {
                Text(
                    text = stringResource(
                        R.string.map_on_ground,
                        if (flight.status.hasDeparted) (flight.divertedTo ?: flight.destination).city else flight.origin.city,
                    ),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                val landing = (flight.landing ?: flight.arrival).best
                val untilLanding = Duration.between(now, landing).coerceAtLeast(Duration.ZERO)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Stat(stringResource(R.string.map_altitude), altitudeText(position.altitudeFeet))
                    Stat(stringResource(R.string.map_speed), speedText(position.groundSpeedKmh))
                    Stat(stringResource(R.string.map_lands_in), formatDuration(untilLanding))
                }
            }
        }
    }
}

@Composable
private fun Stat(label: String, value: String) {
    Column {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.titleLarge)
    }
}
