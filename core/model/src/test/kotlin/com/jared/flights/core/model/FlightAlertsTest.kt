package com.jared.flights.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Duration
import java.time.Instant
import java.time.ZoneId

class FlightAlertsTest {

    private val t0 = Instant.parse("2026-09-26T10:00:00Z")
    private fun min(m: Long) = t0.plus(Duration.ofMinutes(m))
    private fun airport(c: String) = Airport(c, "X$c", c, c, ZoneId.of("UTC"))

    private val base = Flight(
        id = "f", airlineIata = "BA", airlineName = "BA", flightNumber = "1",
        origin = airport("LHR"), destination = airport("EDI"),
        departure = FlightTimes(min(0), estimated = min(0)),
        arrival = FlightTimes(min(90), estimated = min(90)),
        status = FlightStatus.SCHEDULED,
        departureTerminal = "5", departureGate = "A10",
    )

    private fun types(before: Flight, after: Flight) = alertsFor(before, after).map { it.type }

    private fun late(minutes: Long) = base.copy(departure = FlightTimes(min(0), estimated = min(minutes)))

    @Test fun `no change, no alerts`() = assertEquals(emptyList<AlertType>(), types(base, base))

    @Test fun `becoming 15 minutes late is a delay alert`() =
        assertEquals(listOf(AlertType.DELAYED), types(base, late(15)))

    @Test fun `a small wobble is ignored`() = assertEquals(emptyList<AlertType>(), types(base, late(10)))

    @Test fun `getting 15 more minutes late alerts again, 5 more does not`() {
        assertEquals(listOf(AlertType.DELAYED), types(late(20), late(35)))
        assertEquals(emptyList<AlertType>(), types(late(20), late(25)))
    }

    @Test fun `recovering to on time is back on time`() =
        assertEquals(listOf(AlertType.BACK_ON_TIME), types(late(30), late(5)))

    @Test fun `gate change and gate announced`() {
        assertEquals(listOf(AlertType.GATE_CHANGE), types(base, base.copy(departureGate = "B32")))
        assertEquals(listOf(AlertType.GATE_CHANGE), types(base.copy(departureGate = null), base))
    }

    @Test fun `terminal change`() =
        assertEquals(listOf(AlertType.TERMINAL_CHANGE), types(base, base.copy(departureTerminal = "3")))

    @Test fun `timetable moved by the airline`() {
        val retimed = base.copy(departure = FlightTimes(min(30), estimated = min(30)))
        assertTrue(AlertType.SCHEDULE_CHANGE in types(base, retimed))
    }

    @Test fun `journey milestones`() {
        assertEquals(listOf(AlertType.BOARDING), types(base, base.copy(status = FlightStatus.BOARDING)))
        assertEquals(
            listOf(AlertType.DEPARTED),
            types(base.copy(status = FlightStatus.BOARDING), base.copy(status = FlightStatus.EN_ROUTE)),
        )
        assertEquals(
            listOf(AlertType.LANDED, AlertType.ARRIVED, AlertType.BAGGAGE),
            types(base.copy(status = FlightStatus.EN_ROUTE), base.copy(status = FlightStatus.ARRIVED, baggageClaim = "4")),
        )
    }

    @Test fun `cancelled overrides everything else`() {
        val cancelled = base.copy(status = FlightStatus.CANCELLED, departureGate = "Z1")
        assertEquals(listOf(AlertType.CANCELLED), types(base, cancelled))
    }

    @Test fun `diverted`() {
        val diverted = base.copy(status = FlightStatus.DIVERTED, divertedTo = airport("GLA"))
        assertTrue(AlertType.DIVERTED in types(base.copy(status = FlightStatus.EN_ROUTE), diverted))
    }

    @Test fun `a connection going from comfortable to at risk alerts`() {
        val leg1 = base.copy(id = "1", destination = airport("AMS"))
        val leg2 = base.copy(
            id = "2", origin = airport("AMS"), destination = airport("JFK"),
            departure = FlightTimes(min(180), estimated = min(180)),
            arrival = FlightTimes(min(700), estimated = min(700)),
        )
        val leg1Late = leg1.copy(arrival = FlightTimes(min(90), estimated = min(150))) // 30 min to connect
        val alerts = connectionAlertsFor(listOf(Trip(listOf(leg1, leg2))), listOf(Trip(listOf(leg1Late, leg2))))
        assertEquals(listOf(AlertType.CONNECTION_AT_RISK), alerts.map { it.type })
        assertEquals(ConnectionHealth.AT_RISK, alerts.single().connection?.health)
    }

    @Test fun `departed and landed are off by default, the rest on`() {
        assertEquals(setOf(AlertType.DEPARTED, AlertType.LANDED), AlertType.entries.filterNot { it.onByDefault }.toSet())
    }
}
