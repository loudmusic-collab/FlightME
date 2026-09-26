package com.jared.flights.core.designsystem.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Sky,
    onPrimary = Color.White,
    primaryContainer = Sky,
    onPrimaryContainer = Color.White,
    secondary = InkSoft,
    onSecondary = Color.White,
    background = Paper,
    onBackground = Ink,
    surface = Paper,
    onSurface = Ink,
    surfaceVariant = PaperRaised,
    onSurfaceVariant = InkSoft,
    surfaceContainer = PaperRaised,
    secondaryContainer = Line,
    onSecondaryContainer = Ink,
    outline = Line,
    outlineVariant = Line,
)

private val DarkColors = darkColorScheme(
    primary = SkyLight,
    onPrimary = Night,
    primaryContainer = SkyLight,
    onPrimaryContainer = Night,
    secondary = NightTextSoft,
    onSecondary = Night,
    background = Night,
    onBackground = NightText,
    surface = Night,
    onSurface = NightText,
    surfaceVariant = NightRaised,
    onSurfaceVariant = NightTextSoft,
    surfaceContainer = NightRaised,
    secondaryContainer = NightLine,
    onSecondaryContainer = NightText,
    outline = NightLine,
    outlineVariant = NightLine,
)

/**
 * FlightME theme. Follows the system light/dark setting by default.
 * Android "dynamic colour" (wallpaper colours) is deliberately off so the
 * app keeps its own look (DECISIONS #27).
 */
@Composable
fun FlightMeTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(
        LocalStatusColors provides if (darkTheme) DarkStatusColors else LightStatusColors,
    ) {
        MaterialTheme(
            colorScheme = if (darkTheme) DarkColors else LightColors,
            typography = FlightMeTypography,
            content = content,
        )
    }
}

/** Access FlightME-specific theme values, e.g. `FlightMeTheme.statusColors.onTime`. */
object FlightMeTheme {
    val statusColors: StatusColors
        @Composable
        @ReadOnlyComposable
        get() = LocalStatusColors.current
}
