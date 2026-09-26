package com.jared.flights.core.model

import java.time.Duration
import java.time.Instant

/** Where a plane is right now, for the live map. */
data class LivePosition(
    val location: LatLon,
    /** Compass direction the nose is pointing (0 = north). */
    val headingDegrees: Double,
    val altitudeFeet: Int,
    val groundSpeedKmh: Int,
    /** When this position was measured. */
    val at: Instant,
) {
    val isOnGround: Boolean get() = altitudeFeet == 0
}

/**
 * A made-up but realistic position for a mock flight at [now], worked out from
 * its timetable: along the great-circle route, climbing for ~20 min, cruising,
 * then descending for the last ~35 min. Null if an airport has no location.
 * Real positions come from the provider from Phase 3.
 */
fun Flight.estimatedPositionAt(now: Instant): LivePosition? {
    val from = origin.location ?: return null
    val to = (divertedTo ?: destination).location ?: return null
    val heading = Geo.bearing(from, to)
    val takeoffTime = (takeoff ?: departure).best
    val landingTime = (landing ?: arrival).best

    fun onGround(at: LatLon, headingDegrees: Double) = LivePosition(at, headingDegrees, 0, 0, now)
    if (!status.hasDeparted || status == FlightStatus.DEPARTED || !now.isAfter(takeoffTime)) {
        return onGround(from, heading)
    }
    if (status == FlightStatus.LANDED || status == FlightStatus.ARRIVED || !now.isBefore(landingTime)) {
        return onGround(to, Geo.bearing(Geo.interpolate(from, to, 0.99), to))
    }

    val airMinutes = Duration.between(takeoffTime, landingTime).toMinutes().coerceAtLeast(1).toDouble()
    val elapsed = Duration.between(takeoffTime, now).toSeconds() / 60.0
    val fraction = (elapsed / airMinutes).coerceIn(0.0, 1.0)
    val here = Geo.interpolate(from, to, fraction)
    val ahead = Geo.interpolate(from, to, (fraction + 0.01).coerceAtMost(1.0))

    // Short hops don't reach full cruising height.
    val distanceKm = Geo.distanceKm(from, to)
    val cruiseFeet = when {
        distanceKm < 500 -> 24_000
        distanceKm < 1500 -> 34_000
        else -> 37_000
    }
    val climbMinutes = minOf(20.0, airMinutes / 3)
    val descentMinutes = minOf(35.0, airMinutes / 3)
    val remaining = airMinutes - elapsed
    val altitude = when {
        elapsed < climbMinutes -> cruiseFeet * elapsed / climbMinutes
        remaining < descentMinutes -> cruiseFeet * remaining / descentMinutes
        else -> cruiseFeet.toDouble()
    }
    // Average speed for the trip, slower while climbing and descending.
    val averageKmh = distanceKm / (airMinutes / 60)
    val speedFactor = (0.55 + 0.45 * (altitude / cruiseFeet)).coerceIn(0.55, 1.0)

    return LivePosition(
        location = here,
        headingDegrees = Geo.bearing(here, ahead),
        altitudeFeet = (altitude / 100).toInt() * 100,
        groundSpeedKmh = (averageKmh * speedFactor * 1.08).toInt(),
        at = now,
    )
}
