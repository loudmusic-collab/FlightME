package com.jared.flights.notifications

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.jared.flights.MainActivity

/** Tapping a notification opens the app on that flight's detail screen. */
object OpenFlightIntent {
    const val EXTRA_FLIGHT_ID = "com.jared.flights.FLIGHT_ID"

    fun pendingIntent(context: Context, flightId: String): PendingIntent {
        val intent = Intent(context, MainActivity::class.java)
            .putExtra(EXTRA_FLIGHT_ID, flightId)
            .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        return PendingIntent.getActivity(
            context,
            flightId.hashCode(),
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
    }
}
