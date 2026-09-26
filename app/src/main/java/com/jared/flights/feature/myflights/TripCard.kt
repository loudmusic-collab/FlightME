package com.jared.flights.feature.myflights

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.jared.flights.R
import com.jared.flights.core.model.Trip
import com.jared.flights.ui.ConnectionRow
import java.time.Instant

/**
 * A journey with connections, as one card:
 *
 *   London → Singapore
 *   via Dubai · 2 flights
 *   ┌ EK 2   LHR ──✈── DXB ... ┐   (tap a leg to open it)
 *   ⏱ 2h 10m to change planes in Dubai · Comfortable
 *   └ EK 432 DXB ───── SIN ... ┘
 */
@Composable
fun TripCard(
    trip: Trip,
    now: Instant,
    onFlightClick: (flightId: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val cardColor = MaterialTheme.colorScheme.surface

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = cardColor),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    ) {
        Column(Modifier.padding(vertical = 8.dp)) {
            // The title opens the first flight of the trip.
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp)
                    .clip(MaterialTheme.shapes.medium)
                    .clickable { onFlightClick(trip.legs.first().id) }
                    .padding(horizontal = 8.dp, vertical = 8.dp),
            ) {
                Text(
                    text = stringResource(R.string.trip_title, trip.origin.city, trip.destination.city),
                    style = MaterialTheme.typography.titleLarge,
                )
                Text(
                    text = stringResource(R.string.trip_via, trip.stops.joinToString(", ") { it.city }) + " · " +
                        pluralStringResource(R.plurals.trip_flight_count, trip.legs.size, trip.legs.size),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            trip.legs.forEachIndexed { index, leg ->
                FlightSummary(
                    flight = leg,
                    now = now,
                    backgroundColor = cardColor,
                    modifier = Modifier
                        .padding(horizontal = 8.dp)
                        .clip(MaterialTheme.shapes.medium)
                        .clickable { onFlightClick(leg.id) }
                        .padding(8.dp),
                )
                trip.connections.getOrNull(index)?.let { connection ->
                    HorizontalDivider(
                        Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                        color = MaterialTheme.colorScheme.outline,
                    )
                    ConnectionRow(connection, Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
                    HorizontalDivider(
                        Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                        color = MaterialTheme.colorScheme.outline,
                    )
                }
            }
            Spacer(Modifier.height(4.dp))
        }
    }
}
