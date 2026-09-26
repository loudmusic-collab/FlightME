package com.jared.flights.ui

import android.content.ActivityNotFoundException
import android.net.Uri
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.jared.flights.R
import com.jared.flights.core.designsystem.icon.FlightMeIcons
import com.jared.flights.core.model.Airport

/**
 * "🗺 Airport map": opens Google Maps (app, or the website if the app isn't
 * installed) searching for the airport and terminal. Many big airports have
 * indoor maps there showing gates. Free, no API key (DECISIONS #37).
 */
@Composable
fun AirportMapButton(
    airport: Airport,
    terminal: String?,
    modifier: Modifier = Modifier,
    label: String = stringResource(R.string.airport_map),
) {
    val uriHandler = LocalUriHandler.current
    TextButton(
        onClick = {
            try {
                uriHandler.openUri(airportMapUrl(airport, terminal))
            } catch (e: ActivityNotFoundException) {
                // No maps app or browser on the phone: nothing sensible to do.
            }
        },
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 0.dp),
    ) {
        Icon(FlightMeIcons.Map, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text(label)
    }
}

/** Google Maps search link, e.g. for "LHR Heathrow Airport Terminal 5". */
fun airportMapUrl(airport: Airport, terminal: String?): String {
    val query = buildString {
        append("${airport.iata} ${airport.name} Airport")
        if (!terminal.isNullOrBlank()) append(" Terminal $terminal")
    }
    return "https://www.google.com/maps/search/?api=1&query=" + Uri.encode(query)
}
