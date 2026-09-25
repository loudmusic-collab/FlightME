package com.jared.flights.feature.myflights

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.jared.flights.R
import com.jared.flights.core.designsystem.component.EmptyState
import com.jared.flights.core.designsystem.icon.FlightMeIcons

@Composable
fun MyFlightsScreen(modifier: Modifier = Modifier) {
    EmptyState(
        icon = FlightMeIcons.Flight,
        title = stringResource(R.string.my_flights_empty_title),
        message = stringResource(R.string.my_flights_empty_message),
        modifier = modifier,
    )
}
