package com.jared.flights.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Duration
import java.time.Instant
import java.time.ZoneId

class TripTest {

    private val t0: Instant = Instant.parse("2026-09-26T08:00:00Z")
    private fun min(m: Long): Instant = t0.plus(Duration.ofMinutes(m))

    private fun airport(code: String) = Airport(code, "X$code", code, code, ZoneId.of("UTC"))
    private val lhr = airport("LHR")
    private val dxb = airport("DXB")
    private val sin = airport("SIN")
    private val dub = airport("DUB")

    private fun flight(
        id: String,
        from: Airport,
        to: Airport,
        dep: Long,
        arr: Long,
        arrEstimate: Long? = null,
        status: FlightStatus = FlightStatus.SCHEDULED,
    ) = Flight(
        id = id, airlineIata = "XX", airlineName = "Test", flightNumber = id,
        origin = from, destination = to,
        departure = FlightTimes(min(dep)),
        arrival = FlightTimes(min(arr), estimated = arrEstimate?.let(::min)),
        status = status,
    )

    // LHR → DXB lands at 420, DXB → SIN leaves at 550 (2h 10m planned layover).
    private val leg1 = flight("1", lhr, dxb, 0, 420)
    private val leg2 = flight("2", dxb, sin, 550, 1000)

    // ---- Grouping ----

    @Test fun `connecting legs become one trip`() {
        val trips = groupIntoTrips(listOf(leg2, leg1))
        assertEquals(1, trips.size)
        assertEquals(listOf("1", "2"), trips.single().legs.map { it.id })
        assertEquals(listOf(dxb), trips.single().stops)
    }

    @Test fun `a flight from a different airport is a separate trip`() {
        val other = flight("3", dub, sin, 550, 1000)
        assertEquals(2, groupIntoTrips(listOf(leg1, other)).size)
    }

    @Test fun `more than 24 hours apart is a separate trip`() {
        val later = flight("4", dxb, sin, 420 + 24 * 60 + 1, 2000)
        assertEquals(2, groupIntoTrips(listOf(leg1, later)).size)
    }

    @Test fun `flying back to the start is a return, not a connection`() {
        val back = flight("5", dxb, lhr, 600, 1020)
        assertEquals(2, groupIntoTrips(listOf(leg1, back)).size)
    }

    @Test fun `a delay does not break a trip apart`() {
        // First leg now lands after the second leaves: still one trip (a missed connection).
        val late = flight("1", lhr, dxb, 0, 420, arrEstimate = 600)
        assertEquals(1, groupIntoTrips(listOf(late, leg2)).size)
    }

    @Test fun `the user can split a trip`() {
        assertEquals(2, groupIntoTrips(listOf(leg1, leg2), splitBefore = setOf("2")).size)
    }

    // ---- Connection health (DECISIONS #32) ----

    private fun healthFor(layoverMinutes: Long) = ConnectionRules.healthOf(Duration.ofMinutes(layoverMinutes))

    @Test fun `60 minutes or more is comfortable`() {
        assertEquals(ConnectionHealth.COMFORTABLE, healthFor(60))
        assertEquals(ConnectionHealth.COMFORTABLE, healthFor(130))
    }

    @Test fun `46 to 59 minutes is tight`() {
        assertEquals(ConnectionHealth.TIGHT, healthFor(46))
        assertEquals(ConnectionHealth.TIGHT, healthFor(59))
    }

    @Test fun `45 minutes or less is at risk`() {
        assertEquals(ConnectionHealth.AT_RISK, healthFor(45))
        assertEquals(ConnectionHealth.AT_RISK, healthFor(1))
    }

    @Test fun `no time left is missed`() {
        assertEquals(ConnectionHealth.MISSED, healthFor(0))
        assertEquals(ConnectionHealth.MISSED, healthFor(-20))
    }

    @Test fun `layover uses the latest arrival estimate`() {
        val late = flight("1", lhr, dxb, 0, 420, arrEstimate = 520) // 100 min late
        val connection = Connection(late, leg2)
        assertEquals(Duration.ofMinutes(130), connection.plannedLayover)
        assertEquals(Duration.ofMinutes(30), connection.layover)
        assertEquals(ConnectionHealth.AT_RISK, connection.health)
    }

    @Test fun `a diverted inbound flight puts the connection at risk`() {
        val diverted = leg1.copy(status = FlightStatus.DIVERTED)
        assertEquals(ConnectionHealth.AT_RISK, Connection(diverted, leg2).health)
    }

    @Test fun `trip finds the connections either side of a leg`() {
        val trip = Trip(listOf(leg1, leg2))
        assertEquals("2", trip.connectionAfter("1")?.outbound?.id)
        assertEquals("1", trip.connectionBefore("2")?.inbound?.id)
        assertEquals(null, trip.connectionBefore("1"))
        assertTrue(trip.isConnecting)
        assertFalse(Trip(listOf(leg1)).isConnecting)
    }
}
