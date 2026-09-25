package com.jared.flights.navigation

import androidx.annotation.StringRes
import androidx.compose.ui.graphics.vector.ImageVector
import com.jared.flights.R
import com.jared.flights.core.designsystem.icon.FlightMeIcons
import kotlinx.serialization.Serializable
import kotlin.reflect.KClass

// Type-safe routes: each screen is identified by a class, not a text string.
@Serializable data object MyFlightsRoute
@Serializable data object PassportRoute
@Serializable data object SettingsRoute

/** The bottom-bar tabs, in display order. */
enum class TopLevelDestination(
    val route: Any,
    val routeClass: KClass<*>,
    val icon: ImageVector,
    @StringRes val label: Int,
) {
    MY_FLIGHTS(MyFlightsRoute, MyFlightsRoute::class, FlightMeIcons.Flight, R.string.tab_my_flights),
    PASSPORT(PassportRoute, PassportRoute::class, FlightMeIcons.Globe, R.string.tab_passport),
    SETTINGS(SettingsRoute, SettingsRoute::class, FlightMeIcons.Sliders, R.string.tab_settings),
}
