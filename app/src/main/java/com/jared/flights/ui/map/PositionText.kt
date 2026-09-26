package com.jared.flights.ui.map

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.jared.flights.R
import java.text.NumberFormat

/** "36,000 ft" (altitude stays in feet everywhere; it's what pilots and airlines use). */
@Composable
fun altitudeText(feet: Int): String = stringResource(R.string.map_altitude_feet, NumberFormat.getIntegerInstance().format(feet))

/** "890 km/h" (units become a setting in step 1.11). */
@Composable
fun speedText(kmh: Int): String = stringResource(R.string.map_speed_kmh, NumberFormat.getIntegerInstance().format(kmh))
