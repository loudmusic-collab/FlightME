package com.jared.flights

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.jared.flights.core.designsystem.theme.FlightMeTheme
import com.jared.flights.navigation.FlightMeApp
import com.jared.flights.notifications.OpenFlightIntent
import dagger.hilt.android.AndroidEntryPoint

/** The app's only activity. Every screen is a Compose destination inside it. */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    /** Set when a notification was tapped: open that flight. */
    private var flightToOpen by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) flightToOpen = intent.getStringExtra(OpenFlightIntent.EXTRA_FLIGHT_ID)
        setContent {
            FlightMeTheme {
                FlightMeApp(
                    flightToOpen = flightToOpen,
                    onFlightOpened = { flightToOpen = null },
                )
            }
        }
    }

    /** A notification was tapped while the app was already open. */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        intent.getStringExtra(OpenFlightIntent.EXTRA_FLIGHT_ID)?.let { flightToOpen = it }
    }
}
