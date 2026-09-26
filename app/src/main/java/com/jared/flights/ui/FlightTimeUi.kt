package com.jared.flights.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import com.jared.flights.R
import com.jared.flights.core.designsystem.theme.FlightMeTheme
import com.jared.flights.core.model.DelayRules
import com.jared.flights.core.model.DelaySeverity
import com.jared.flights.core.model.Flight
import com.jared.flights.core.model.FlightTimes
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

val TimeFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
private val DateFormat = DateTimeFormatter.ofPattern("EEE d MMM")

/** "14:05" in the given airport's local time. */
fun Instant.localTime(zone: ZoneId): String = atZone(zone).format(TimeFormat)

/**
 * Colour for one time, based on its own delay (DECISIONS #29):
 * green/amber/red when we have a live estimate or actual, neutral for
 * timetable-only times, grey when the flight is cancelled.
 */
@Composable
fun timeColor(times: FlightTimes, cancelled: Boolean): Color {
    val hasLiveTime = times.estimated != null || times.actual != null
    return when {
        cancelled -> MaterialTheme.colorScheme.onSurfaceVariant
        !hasLiveTime -> MaterialTheme.colorScheme.onSurface
        else -> when (DelayRules.severityOf(times.delay)) {
            DelaySeverity.ON_TIME -> FlightMeTheme.statusColors.onTime
            DelaySeverity.MINOR -> FlightMeTheme.statusColors.minorDelay
            DelaySeverity.MAJOR -> FlightMeTheme.statusColors.majorDelay
        }
    }
}

/** "Today", "Tomorrow", "Yesterday" or e.g. "Sat 27 Sep", in the airport's own time zone. */
@Composable
fun dateLabel(time: Instant, zone: ZoneId, now: Instant): String {
    val date = time.atZone(zone).toLocalDate()
    val today = now.atZone(zone).toLocalDate()
    return when (ChronoUnit.DAYS.between(today, date)) {
        0L -> stringResource(R.string.date_today)
        1L -> stringResource(R.string.date_tomorrow)
        -1L -> stringResource(R.string.date_yesterday)
        else -> date.format(DateFormat)
    }
}

/** "+1" when the flight lands on a later local date than it left, e.g. overnight to New York. */
@Composable
fun arrivalDayOffset(flight: Flight): String? {
    val departDate = flight.departure.best.atZone(flight.origin.timeZone).toLocalDate()
    val arriveDate = flight.arrival.best.atZone(flight.destination.timeZone).toLocalDate()
    val days = ChronoUnit.DAYS.between(departDate, arriveDate)
    return if (days != 0L) stringResource(R.string.date_day_offset, days) else null
}
