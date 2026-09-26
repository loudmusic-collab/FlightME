package com.jared.flights.notifications

import android.content.Context
import com.jared.flights.core.data.FlightSync
import com.jared.flights.core.data.di.ApplicationScope
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/** Starts everything notification-related when the app starts. */
@Singleton
class Notifications @Inject constructor(
    @ApplicationContext private val context: Context,
    private val flightSync: FlightSync,
    private val alertNotifier: AlertNotifier,
    private val liveUpdates: LiveUpdateController,
    @ApplicationScope private val scope: CoroutineScope,
) {
    fun start() {
        NotificationChannels.create(context)
        scope.launch { flightSync.alerts.collect { alertNotifier.show(it) } }
        liveUpdates.start()
    }
}
