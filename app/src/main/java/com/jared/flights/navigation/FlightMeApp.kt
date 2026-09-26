package com.jared.flights.navigation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
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
import com.jared.flights.R
import com.jared.flights.core.designsystem.icon.FlightMeIcons
import com.jared.flights.feature.addflight.AddFlightScreen
import com.jared.flights.feature.flightdetail.FlightDetailScreen
import com.jared.flights.feature.livemap.LiveMapScreen
import com.jared.flights.feature.myflights.MyFlightsScreen
import com.jared.flights.feature.myflights.RemoveRequest
import com.jared.flights.feature.passport.PassportScreen
import com.jared.flights.feature.settings.SettingsScreen
import kotlinx.coroutines.launch

/** App shell: the bottom tab bar, the "+" button, messages, and the area where the current screen is shown. */
@Composable
fun FlightMeApp(navController: NavHostController = rememberNavController()) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination
    // Tabs show on the three main screens only; other screens are full screen.
    val showTabs = currentDestination == null ||
        TopLevelDestination.entries.any { currentDestination.isOn(it) }
    val onMyFlights = currentDestination == null || currentDestination.isOn(TopLevelDestination.MY_FLIGHTS)

    // One message bar for the whole app, so messages survive moving between screens.
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val addedTemplate = stringResource(R.string.added_message)
    val changeFailed = stringResource(R.string.change_failed_offline)

    // "Remove flight" on a detail screen is handed to My Flights, which offers Undo.
    var pendingRemove by remember { mutableStateOf<RemoveRequest?>(null) }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            // Rounded-rectangle "+" above the Settings tab, on My Flights only.
            if (onMyFlights) {
                FloatingActionButton(
                    onClick = { navController.navigate(AddFlightRoute) },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ) {
                    Icon(FlightMeIcons.Add, contentDescription = stringResource(R.string.add_flight_title))
                }
            }
        },
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
                MyFlightsScreen(
                    onFlightClick = { id -> navController.navigate(FlightDetailRoute(id)) },
                    snackbarHostState = snackbarHostState,
                    removeRequest = pendingRemove,
                    onRemoveRequestHandled = { pendingRemove = null },
                )
            }
            composable<PassportRoute> { PassportScreen() }
            composable<SettingsRoute> { SettingsScreen() }
            composable<FlightDetailRoute> {
                FlightDetailScreen(
                    onBack = { navController.popBackStack() },
                    onFlightClick = { id -> navController.navigate(FlightDetailRoute(id)) },
                    onOpenMap = { id -> navController.navigate(LiveMapRoute(id)) },
                    onRemove = { ids, label ->
                        pendingRemove = RemoveRequest(ids, label)
                        navController.popBackStack<MyFlightsRoute>(inclusive = false)
                    },
                )
            }
            composable<LiveMapRoute> {
                LiveMapScreen(onBack = { navController.popBackStack() })
            }
            composable<AddFlightRoute> {
                AddFlightScreen(
                    onBack = { navController.popBackStack() },
                    onAdded = { label ->
                        navController.popBackStack()
                        scope.launch { snackbarHostState.showSnackbar(addedTemplate.format(label)) }
                    },
                    onOfflineMessage = { scope.launch { snackbarHostState.showSnackbar(changeFailed) } },
                )
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
