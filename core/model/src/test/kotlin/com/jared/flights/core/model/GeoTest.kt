package com.jared.flights.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import kotlin.math.abs

class GeoTest {

    private val lhr = LatLon(51.4700, -0.4543)
    private val jfk = LatLon(40.6413, -73.7781)
    private val cdg = LatLon(49.0097, 2.5479)

    @Test fun `London to New York is about 5540 km`() {
        assertEquals(5540.0, Geo.distanceKm(lhr, jfk), 30.0)
    }

    @Test fun `London to Paris is about 350 km`() {
        assertEquals(348.0, Geo.distanceKm(lhr, cdg), 10.0)
    }

    @Test fun `halfway to New York is up north near Ireland's latitude, not straight across`() {
        val mid = Geo.interpolate(lhr, jfk, 0.5)
        assertTrue("latitude ${mid.latitude}", mid.latitude > 51.5) // curves north of both ends
    }

    @Test fun `interpolating the ends gives the ends`() {
        val start = Geo.interpolate(lhr, jfk, 0.0)
        val end = Geo.interpolate(lhr, jfk, 1.0)
        assertEquals(lhr.latitude, start.latitude, 1e-6)
        assertEquals(jfk.longitude, end.longitude, 1e-6)
    }

    @Test fun `heading from London to New York starts out north-west`() {
        val bearing = Geo.bearing(lhr, jfk)
        assertTrue("bearing $bearing", bearing in 280.0..300.0)
    }

    @Test fun `a route across the date line stays continuous`() {
        val tokyo = LatLon(35.55, 139.78)
        val sanFrancisco = LatLon(37.62, -122.38)
        val path = Geo.path(tokyo, sanFrancisco)
        path.zipWithNext().forEach { (a, b) -> assertTrue(abs(b.longitude - a.longitude) < 180) }
    }

    // ---- Estimated live position ----

    private val t0 = Instant.parse("2026-09-26T10:00:00Z")
    private fun min(m: Long) = t0.plus(Duration.ofMinutes(m))
    private val london = Airport("LHR", "EGLL", "Heathrow", "London", ZoneId.of("Europe/London"), lhr)
    private val newYork = Airport("JFK", "KJFK", "JFK", "New York", ZoneId.of("America/New_York"), jfk)

    private fun transatlantic(status: FlightStatus) = Flight(
        id = "t", airlineIata = "BA", airlineName = "BA", flightNumber = "117",
        origin = london, destination = newYork,
        departure = FlightTimes(min(0), actual = min(0)),
        takeoff = FlightTimes(min(15), actual = min(15)),
        landing = FlightTimes(min(475), estimated = min(475)),
        arrival = FlightTimes(min(485), estimated = min(485)),
        status = status,
    )

    @Test fun `halfway through the air time the plane is cruising near the middle`() {
        val p = transatlantic(FlightStatus.EN_ROUTE).estimatedPositionAt(min(245))!!
        assertEquals(37_000, p.altitudeFeet)
        assertEquals(Geo.distanceKm(lhr, jfk) / 2, Geo.distanceKm(lhr, p.location), 100.0)
        assertTrue("speed ${p.groundSpeedKmh}", p.groundSpeedKmh in 650..1000)
    }

    @Test fun `still climbing shortly after take-off`() {
        val p = transatlantic(FlightStatus.EN_ROUTE).estimatedPositionAt(min(25))!!
        assertTrue("altitude ${p.altitudeFeet}", p.altitudeFeet in 10_000..30_000)
    }

    @Test fun `before take-off it's on the ground at the origin`() {
        val p = transatlantic(FlightStatus.SCHEDULED).estimatedPositionAt(min(-60))!!
        assertTrue(p.isOnGround)
        assertEquals(lhr, p.location)
    }

    @Test fun `after arrival it's on the ground at the destination`() {
        val p = transatlantic(FlightStatus.ARRIVED).estimatedPositionAt(min(500))!!
        assertTrue(p.isOnGround)
        assertEquals(jfk, p.location)
    }

    @Test fun `no position without airport locations`() {
        val noMap = transatlantic(FlightStatus.EN_ROUTE).copy(origin = london.copy(location = null))
        assertEquals(null, noMap.estimatedPositionAt(min(100)))
    }
}
