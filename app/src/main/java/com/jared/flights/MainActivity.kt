package com.jared.flights

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.jared.flights.core.designsystem.theme.FlightMeTheme
import com.jared.flights.navigation.FlightMeApp
import dagger.hilt.android.AndroidEntryPoint

/** The app's only activity. Every screen is a Compose destination inside it. */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            FlightMeTheme {
                FlightMeApp()
            }
        }
    }
}
