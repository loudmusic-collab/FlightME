package com.jared.flights.navigation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.jared.flights.feature.flightdetail.FlightDetailScreen
import com.jared.flights.feature.myflights.MyFlightsScreen
import com.jared.flights.feature.passport.PassportScreen
import com.jared.flights.feature.settings.SettingsScreen

/** App shell: the bottom tab bar plus the area where the current screen is shown. */
@Composable
fun FlightMeApp(navController: NavHostController = rememberNavController()) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination
    // Tabs show on the three main screens only; detail screens are full screen.
    val showTabs = currentDestination == null ||
        TopLevelDestination.entries.any { currentDestination.isOn(it) }

    Scaffold(
        bottomBar = {
            if (showTabs) {
                Column {
                    HorizontalDivider()
                    NavigationBar {
                        TopLevelDestination.entries.forEach { destination ->
                            NavigationBarItem(
                                selected = currentDestination.isOn(destination),
                                onClick = { navController.navigateToTab(destination) },
                                icon = { Icon(destination.icon, contentDescription = null) },
                                label = { Text(stringResource(destination.label)) },
                            )
                        }
                    }
                }
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = MyFlightsRoute,
            modifier = Modifier.padding(innerPadding),
        ) {
            composable<MyFlightsRoute> {
                MyFlightsScreen(onFlightClick = { id -> navController.navigate(FlightDetailRoute(id)) })
            }
            composable<PassportRoute> { PassportScreen() }
            composable<SettingsRoute> { SettingsScreen() }
            composable<FlightDetailRoute> {
                FlightDetailScreen(onBack = { navController.popBackStack() })
            }
        }
    }
}

private fun NavDestination?.isOn(tab: TopLevelDestination): Boolean =
    this?.hierarchy?.any { it.hasRoute(tab.routeClass) } == true

/** Switch tabs the standard way: one copy of each tab, and each tab keeps its own state. */
private fun NavHostController.navigateToTab(tab: TopLevelDestination) {
    navigate(tab.route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
