package com.jared.flights.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import com.jared.flights.R

/**
 * Android groups notifications into "channels" that people can switch on/off
 * in the phone's settings. FlightME has two.
 */
object NotificationChannels {
    /** Gate changes, delays, boarding… Pops up with sound. */
    const val ALERTS = "flight_alerts"

    /** The ongoing progress notification during a journey (Live Update on Android 16). */
    const val LIVE = "live_progress"

    fun create(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannels(
            listOf(
                NotificationChannel(ALERTS, context.getString(R.string.channel_alerts), NotificationManager.IMPORTANCE_HIGH)
                    .apply { description = context.getString(R.string.channel_alerts_description) },
                // Live Updates need at least DEFAULT importance, and shouldn't make a sound every update.
                NotificationChannel(LIVE, context.getString(R.string.channel_live), NotificationManager.IMPORTANCE_DEFAULT)
                    .apply {
                        description = context.getString(R.string.channel_live_description)
                        setSound(null, null)
                        enableVibration(false)
                    },
            ),
        )
    }
}
