package com.jared.flights.notifications

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.jared.flights.R
import com.jared.flights.core.model.AlertType
import com.jared.flights.core.model.FlightAlert
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

/** Turns alerts into phone notifications, in plain language. */
@Singleton
class AlertNotifier @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    fun show(alerts: List<FlightAlert>) {
        if (!canNotify()) return
        val manager = NotificationManagerCompat.from(context)
        alerts
            // Per-alert switches arrive with Settings (1.11); for now use the defaults.
            .filter { it.type.onByDefault }
            .forEach { alert ->
                val (title, text) = words(alert)
                val notification = NotificationCompat.Builder(context, NotificationChannels.ALERTS)
                    .setSmallIcon(R.drawable.ic_stat_flight)
                    .setContentTitle(title)
                    .setContentText(text)
                    .setStyle(NotificationCompat.BigTextStyle().bigText(text))
                    .setContentIntent(OpenFlightIntent.pendingIntent(context, alert.flight.id))
                    .setAutoCancel(true)
                    .setCategory(NotificationCompat.CATEGORY_STATUS)
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .build()
                manager.notify(alert.flight.id.hashCode() * 31 + alert.type.ordinal, notification)
            }
    }

    private fun canNotify(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED

    /** Title and message for an alert, e.g. "BA 283 gate change" / "Now gate B32 (was A10)." */
    private fun words(alert: FlightAlert): Pair<String, String> {
        val f = alert.flight
        val old = alert.before
        val name = "${f.airlineIata} ${f.flightNumber}"
        fun s(id: Int, vararg args: Any) = context.getString(id, *args)
        val arrivalAirport = f.divertedTo ?: f.destination

        return when (alert.type) {
            AlertType.DELAYED ->
                if (f.status.hasDeparted) {
                    s(R.string.alert_delayed_title, name) to s(
                        R.string.alert_delayed_arrival, time(f.arrival.best, arrivalAirport.timeZone),
                        minutes(f.relevantDelay), arrivalAirport.city,
                    )
                } else {
                    s(R.string.alert_delayed_title, name) to s(
                        R.string.alert_delayed_departure, time(f.departure.best, f.origin.timeZone),
                        minutes(f.relevantDelay), f.origin.city,
                    )
                }
            AlertType.BACK_ON_TIME -> s(R.string.alert_on_time_title, name) to s(
                R.string.alert_on_time_text, time(f.departure.best, f.origin.timeZone),
            )
            AlertType.SCHEDULE_CHANGE -> s(R.string.alert_schedule_title, name) to s(
                R.string.alert_schedule_text,
                time(old.departure.scheduled, f.origin.timeZone), time(f.departure.scheduled, f.origin.timeZone),
            )
            AlertType.GATE_CHANGE ->
                if (old.departureGate == null) {
                    s(R.string.alert_gate_announced_title, name) to s(R.string.alert_gate_announced_text, f.departureGate.orEmpty())
                } else {
                    s(R.string.alert_gate_change_title, name) to
                        s(R.string.alert_gate_change_text, f.departureGate.orEmpty(), old.departureGate.orEmpty())
                }
            AlertType.TERMINAL_CHANGE -> s(R.string.alert_terminal_title, name) to s(
                R.string.alert_terminal_text, f.departureTerminal.orEmpty(), old.departureTerminal.orEmpty(),
            )
            AlertType.BOARDING -> s(R.string.alert_boarding_title, name) to (
                f.departureGate?.let { s(R.string.alert_boarding_gate, it, time(f.departure.best, f.origin.timeZone)) }
                    ?: s(R.string.alert_boarding_no_gate, time(f.departure.best, f.origin.timeZone))
                )
            AlertType.DEPARTED -> s(R.string.alert_departed_title, name) to s(
                R.string.alert_departed_text, time(f.arrival.best, arrivalAirport.timeZone), arrivalAirport.city,
            )
            AlertType.LANDED -> s(R.string.alert_landed_title, name) to s(R.string.alert_landed_text, arrivalAirport.city)
            AlertType.ARRIVED -> s(R.string.alert_arrived_title, name) to listOfNotNull(
                s(R.string.alert_arrived_text, arrivalAirport.city, time(f.arrival.best, arrivalAirport.timeZone)),
                f.baggageClaim?.let { s(R.string.alert_baggage_text, it) },
            ).joinToString(" ")
            AlertType.BAGGAGE -> s(R.string.alert_baggage_title, name) to s(R.string.alert_baggage_text, f.baggageClaim.orEmpty())
            AlertType.CANCELLED -> s(R.string.alert_cancelled_title, name) to s(
                R.string.alert_cancelled_text, f.origin.city, f.destination.city,
            )
            AlertType.DIVERTED -> s(R.string.alert_diverted_title, name) to s(
                R.string.alert_diverted_text, arrivalAirport.city, arrivalAirport.iata, f.destination.city,
            )
            AlertType.CONNECTION_AT_RISK -> {
                val c = alert.connection!!
                val next = "${c.outbound.airlineIata} ${c.outbound.flightNumber}"
                s(R.string.alert_connection_title, c.airport.city) to s(
                    R.string.alert_connection_text, name, minutes(c.layover), next,
                )
            }
        }
    }

    private fun time(instant: Instant, zone: ZoneId) = instant.atZone(zone).format(TIME)

    /** "25m" or "1h 10m". */
    private fun minutes(d: Duration): String {
        val m = d.toMinutes().coerceAtLeast(0)
        return if (m < 60) "${m}m" else if (m % 60 == 0L) "${m / 60}h" else "${m / 60}h ${m % 60}m"
    }

    private companion object {
        val TIME: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
    }
}

