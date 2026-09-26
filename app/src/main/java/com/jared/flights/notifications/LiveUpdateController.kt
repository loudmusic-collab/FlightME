package com.jared.flights.notifications

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.IconCompat
import com.jared.flights.BuildConfig
import com.jared.flights.R
import com.jared.flights.core.data.FlightRepository
import com.jared.flights.core.data.timemachine.TIME_MACHINE_FLIGHT_ID
import com.jared.flights.core.data.di.ApplicationScope
import com.jared.flights.core.model.DelaySeverity
import com.jared.flights.core.model.Flight
import com.jared.flights.core.model.FlightStatus
import com.jared.flights.core.model.MilestoneType
import com.jared.flights.core.model.timeline
import com.jared.flights.ui.clockTicker
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

/**
 * One ongoing notification for the journey you're on, from 3 h before departure
 * to 30 min after arrival (DECISIONS #42). On Android 16 it becomes a "Live Update"
 * (progress bar with a plane, status-bar chip); on older phones it's a normal
 * ongoing notification with a progress bar and countdown.
 */
@Singleton
class LiveUpdateController @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: FlightRepository,
    private val clock: Clock,
    @ApplicationScope private val scope: CoroutineScope,
) {
    private var started = false
    private var shownFlightId: String? = null

    fun start() {
        if (started) return
        started = true
        scope.launch {
            // Re-check when flights change, and every minute so the progress keeps moving.
            combine(repository.observeTrackedFlights(), clockTicker(clock, 60_000)) { flights, now ->
                pick(flights, now) to now
            }.collect { (flight, now) -> show(flight, now) }
        }
    }

    /** The flight to show: the earliest one inside its "live" window, or none. */
    internal fun pick(flights: List<Flight>, now: Instant): Flight? {
        val live = flights
            .filter { it.status != FlightStatus.CANCELLED }
            .filter { f ->
                !now.isBefore(f.departure.best.minus(BEFORE_DEPARTURE)) && now.isBefore(f.arrival.best.plus(AFTER_ARRIVAL))
            }
        // Test builds: the time-machine flight (FM 100) goes first, so stepping it shows here.
        if (BuildConfig.DEBUG) live.find { it.id == TIME_MACHINE_FLIGHT_ID }?.let { return it }
        return live.minByOrNull { it.departure.best }
    }

    private fun show(flight: Flight?, now: Instant) {
        val manager = NotificationManagerCompat.from(context)
        if (flight == null || !canNotify()) {
            if (shownFlightId != null) manager.cancel(NOTIFICATION_ID)
            shownFlightId = null
            return
        }
        manager.notify(NOTIFICATION_ID, build(flight, now))
        shownFlightId = flight.id
    }

    private fun build(f: Flight, now: Instant): android.app.Notification {
        val name = "${f.airlineIata} ${f.flightNumber}"
        val arrivalAirport = f.divertedTo ?: f.destination
        val timeline = f.timeline
        fun time(i: Instant, zone: java.time.ZoneId) = i.atZone(zone).format(TIME)
        val depTime = time(f.departure.best, f.origin.timeZone)
        val arrTime = time(f.arrival.best, arrivalAirport.timeZone)
        val late = if (f.delaySeverity != DelaySeverity.ON_TIME) {
            " · " + context.getString(R.string.live_late, minutes(f.relevantDelay))
        } else {
            ""
        }
        val gate = f.departureGate?.let { context.getString(R.string.live_gate, it) }

        // What's happening now, in a few words, plus a short "chip" for the status bar.
        val (text, chip, countdownTo) = when (f.status) {
            FlightStatus.SCHEDULED -> Triple(
                listOfNotNull(context.getString(R.string.live_leaves, depTime), gate).joinToString(" · ") + late,
                gate ?: depTime,
                f.departure.best,
            )
            FlightStatus.BOARDING -> Triple(
                listOfNotNull(context.getString(R.string.live_boarding), gate, context.getString(R.string.live_leaves, depTime))
                    .joinToString(" · ") + late,
                gate ?: context.getString(R.string.live_boarding),
                f.departure.best,
            )
            FlightStatus.DEPARTED -> Triple(context.getString(R.string.live_taxiing) + late, context.getString(R.string.live_taxiing), null)
            FlightStatus.EN_ROUTE -> Triple(
                context.getString(R.string.live_in_air, arrTime, arrivalAirport.city) + late,
                minutes(Duration.between(now, (f.landing ?: f.arrival).best)),
                (f.landing ?: f.arrival).best,
            )
            FlightStatus.DIVERTED -> Triple(
                context.getString(R.string.live_diverted, arrivalAirport.city),
                context.getString(R.string.live_diverted_chip),
                null,
            )
            FlightStatus.LANDED -> Triple(context.getString(R.string.live_landed), context.getString(R.string.live_landed_chip), null)
            FlightStatus.ARRIVED -> Triple(
                listOfNotNull(
                    f.arrivalGate?.let { context.getString(R.string.live_at_gate, it) } ?: context.getString(R.string.live_arrived),
                    f.baggageClaim?.let { context.getString(R.string.live_baggage, it) },
                ).joinToString(" · "),
                context.getString(R.string.live_arrived_chip),
                null,
            )
            FlightStatus.CANCELLED -> Triple("", "", null) // never shown (filtered out in pick)
        }

        // Progress bar: one segment per stage (boarding, taxi, flight, taxi), sized by
        // how long each takes; the plane sits at "now". Grey on the ground, blue in the air.
        val stops = timeline.map { it.times.best }
        val start = stops.first()
        fun minutesFromStart(i: Instant) = Duration.between(start, i).toMinutes().toInt().coerceAtLeast(0)
        val ground = ContextCompat.getColor(context, R.color.live_ground)
        val air = ContextCompat.getColor(context, R.color.live_air)
        val segments = timeline.zipWithNext().map { (a, b) ->
            NotificationCompat.ProgressStyle.Segment(
                (minutesFromStart(b.times.best) - minutesFromStart(a.times.best)).coerceAtLeast(1),
            ).setColor(if (a.type == MilestoneType.TAKEOFF) air else ground)
        }
        val total = segments.sumOf { it.length }
        val progress = minutesFromStart(now).coerceIn(0, total)
        val points = stops.drop(1).dropLast(1).map { NotificationCompat.ProgressStyle.Point(minutesFromStart(it).coerceIn(0, total)) }
        val style = NotificationCompat.ProgressStyle()
            .setProgressSegments(segments)
            .setProgressPoints(points)
            .setProgress(progress)
            .setStyledByProgress(true)
            .setProgressTrackerIcon(IconCompat.createWithResource(context, R.drawable.ic_progress_plane))

        val builder = NotificationCompat.Builder(context, NotificationChannels.LIVE)
            .setSmallIcon(R.drawable.ic_stat_flight)
            .setContentTitle(context.getString(R.string.live_title, name, f.origin.iata, arrivalAirport.iata))
            .setContentText(text)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            // Its own group, so Android doesn't bundle it in with the alerts.
            .setGroup(GROUP)
            .setContentIntent(OpenFlightIntent.pendingIntent(context, f.id))
            .setStyle(style)
            .setShortCriticalText(chip)
            // Android 16: ask for this to be shown as a Live Update (status-bar chip, lock screen).
            .setRequestPromotedOngoing(true)

        // A ticking countdown (to departure, or to landing while in the air).
        if (countdownTo != null && countdownTo.isAfter(now)) {
            builder.setWhen(countdownTo.toEpochMilli())
                .setShowWhen(true)
                .setUsesChronometer(true)
                .setChronometerCountDown(true)
        } else {
            builder.setShowWhen(false)
        }
        // Older Android versions don't draw the segmented bar, so give them a plain one too.
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.BAKLAVA) builder.setProgress(total, progress, false)

        return builder.build()
    }

    private fun canNotify(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED

    private fun minutes(d: Duration): String {
        val m = d.toMinutes().coerceAtLeast(0)
        return if (m < 60) "${m}m" else if (m % 60 == 0L) "${m / 60}h" else "${m / 60}h ${m % 60}m"
    }

    private companion object {
        const val NOTIFICATION_ID = 1_000
        const val GROUP = "live_progress"
        val BEFORE_DEPARTURE: Duration = Duration.ofHours(3)
        val AFTER_ARRIVAL: Duration = Duration.ofMinutes(30)
        val TIME: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
    }
}
