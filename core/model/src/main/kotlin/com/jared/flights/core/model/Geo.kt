package com.jared.flights.core.model

import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/** A point on Earth, in degrees. */
data class LatLon(val latitude: Double, val longitude: Double)

/**
 * Great-circle maths: the shortest path between two points on a globe, which is
 * what planes fly (that's why London → Los Angeles curves up over Greenland).
 */
object Geo {
    private const val EARTH_RADIUS_KM = 6371.0

    private fun Double.rad() = Math.toRadians(this)
    private fun Double.deg() = Math.toDegrees(this)

    /** Distance along the Earth's surface, in km. */
    fun distanceKm(a: LatLon, b: LatLon): Double = EARTH_RADIUS_KM * angle(a, b)

    /** The point [fraction] of the way from [a] to [b] along the great circle (0 = a, 1 = b). */
    fun interpolate(a: LatLon, b: LatLon, fraction: Double): LatLon {
        val d = angle(a, b)
        if (d < 1e-9) return a
        val sa = sin((1 - fraction) * d) / sin(d)
        val sb = sin(fraction * d) / sin(d)
        val lat1 = a.latitude.rad(); val lon1 = a.longitude.rad()
        val lat2 = b.latitude.rad(); val lon2 = b.longitude.rad()
        val x = sa * cos(lat1) * cos(lon1) + sb * cos(lat2) * cos(lon2)
        val y = sa * cos(lat1) * sin(lon1) + sb * cos(lat2) * sin(lon2)
        val z = sa * sin(lat1) + sb * sin(lat2)
        return LatLon(atan2(z, sqrt(x * x + y * y)).deg(), atan2(y, x).deg())
    }

    /** Compass direction (0 = north, 90 = east) to head from [a] towards [b]. */
    fun bearing(a: LatLon, b: LatLon): Double {
        val lat1 = a.latitude.rad(); val lat2 = b.latitude.rad()
        val dLon = (b.longitude - a.longitude).rad()
        val y = sin(dLon) * cos(lat2)
        val x = cos(lat1) * sin(lat2) - sin(lat1) * cos(lat2) * cos(dLon)
        return (atan2(y, x).deg() + 360) % 360
    }

    /**
     * [segments] + 1 points along the great circle, for drawing the route.
     * Longitudes are "unwrapped" so a line crossing the date line (±180°) stays continuous.
     */
    fun path(a: LatLon, b: LatLon, segments: Int = 64): List<LatLon> {
        val raw = (0..segments).map { interpolate(a, b, it.toDouble() / segments) }
        val out = ArrayList<LatLon>(raw.size)
        var offset = 0.0
        for ((i, p) in raw.withIndex()) {
            if (i > 0) {
                val jump = p.longitude - raw[i - 1].longitude
                if (jump > 180) offset -= 360 else if (jump < -180) offset += 360
            }
            out += LatLon(p.latitude, p.longitude + offset)
        }
        return out
    }

    /** Angle between two points as seen from the Earth's centre, in radians (haversine). */
    private fun angle(a: LatLon, b: LatLon): Double {
        val dLat = (b.latitude - a.latitude).rad()
        val dLon = (b.longitude - a.longitude).rad()
        val h = sin(dLat / 2).let { it * it } +
            cos(a.latitude.rad()) * cos(b.latitude.rad()) * sin(dLon / 2).let { it * it }
        return 2 * asin(sqrt(h.coerceIn(0.0, 1.0)))
    }
}
