package com.jared.flights.core.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/** Colours for flight status. Read them with `FlightMeTheme.statusColors`. */
@Immutable
data class StatusColors(
    val onTime: Color,
    val minorDelay: Color,
    val majorDelay: Color,
    val cancelled: Color,
    val unknown: Color,
)

internal val LightStatusColors = StatusColors(
    onTime = OnTimeLight,
    minorDelay = MinorDelayLight,
    majorDelay = MajorDelayLight,
    cancelled = CancelledLight,
    unknown = UnknownLight,
)

internal val DarkStatusColors = StatusColors(
    onTime = OnTimeDark,
    minorDelay = MinorDelayDark,
    majorDelay = MajorDelayDark,
    cancelled = CancelledDark,
    unknown = UnknownDark,
)

internal val LocalStatusColors = staticCompositionLocalOf { LightStatusColors }
