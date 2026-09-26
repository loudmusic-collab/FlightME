package com.jared.flights

import android.app.Application
import com.jared.flights.core.data.FlightSync
import com.jared.flights.notifications.Notifications
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

/** App entry point. `@HiltAndroidApp` switches on dependency injection. */
@HiltAndroidApp
class FlightMeApplication : Application() {

    @Inject lateinit var flightSync: FlightSync
    @Inject lateinit var notifications: Notifications

    override fun onCreate() {
        super.onCreate()
        // Alerts listen for changes first, so none are missed.
        notifications.start()
        // Keep the on-phone database up to date while the app runs.
        flightSync.start()
    }
}
