package com.jared.flights.feature.passport

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.jared.flights.R
import com.jared.flights.core.designsystem.component.EmptyState
import com.jared.flights.core.designsystem.icon.FlightMeIcons

@Composable
fun PassportScreen(modifier: Modifier = Modifier) {
    EmptyState(
        icon = FlightMeIcons.Globe,
        title = stringResource(R.string.passport_empty_title),
        message = stringResource(R.string.passport_empty_message),
        modifier = modifier,
    )
}
