package com.jared.flights.feature.settings

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.jared.flights.R
import com.jared.flights.core.designsystem.component.EmptyState
import com.jared.flights.core.designsystem.icon.FlightMeIcons

@Composable
fun SettingsScreen(modifier: Modifier = Modifier) {
    EmptyState(
        icon = FlightMeIcons.Sliders,
        title = stringResource(R.string.settings_empty_title),
        message = stringResource(R.string.settings_empty_message),
        modifier = modifier,
    )
}
