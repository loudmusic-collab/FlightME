package com.jared.flights.core.model

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Duration
import java.time.Instant
import java.time.ZoneId

class FlightTest {

    private val t0: Instant = Instant.parse("2026-09-25T10:00:00Z")
    private fun min(m: Long): Instant = t0.plus(Duration.ofMinutes(m))

    private val lhr = Airport("LHR", "EGLL", "Heathrow", "London", ZoneId.of("Europe/London"))
    private val edi = Airport("EDI", "EGPH", "Edinburgh", "Edinburgh", ZoneId.of("Europe/London"))

    private fun flight(
        status: FlightStatus,
        departure: FlightTimes,
        arrival: FlightTimes,
    ) = Flight(
        id = "test",
        airlineIata = "BA",
        airlineName = "British Airways",
        flightNumber = "1438",
        origin = lhr,
        destination = edi,
        departure = departure,
        arrival = arrival,
        status = status,
    )

    @Test fun `best time prefers actual, then estimated, then scheduled`() {
        assertEquals(min(0), FlightTimes(min(0)).best)
        assertEquals(min(20), FlightTimes(min(0), estimated = min(20)).best)
        assertEquals(min(25), FlightTimes(min(0), estimated = min(20), actual = min(25)).best)
    }

    @Test fun `delay is positive when late and negative when early`() {
        assertEquals(Duration.ofMinutes(20), FlightTimes(min(0), estimated = min(20)).delay)
        assertEquals(Duration.ofMinutes(-5), FlightTimes(min(0), actual = min(-5)).delay)
    }

    @Test fun `before departure the departure delay counts`() {
        val f = flight(
            FlightStatus.SCHEDULED,
            departure = FlightTimes(min(0), estimated = min(30)),
            arrival = FlightTimes(min(85), estimated = min(90)),
        )
        assertEquals(Duration.ofMinutes(30), f.relevantDelay)
        assertEquals(DelaySeverity.MINOR, f.delaySeverity)
    }

    @Test fun `after departure the arrival delay counts`() {
        // Left 30 min late but made up time in the air: now only 5 min late.
        val f = flight(
            FlightStatus.EN_ROUTE,
            departure = FlightTimes(min(0), actual = min(30)),
            arrival = FlightTimes(min(85), estimated = min(90)),
        )
        assertEquals(Duration.ofMinutes(5), f.relevantDelay)
        assertEquals(DelaySeverity.ON_TIME, f.delaySeverity)
    }

    @Test fun `ident joins airline code and number`() {
        val f = flight(FlightStatus.SCHEDULED, FlightTimes(min(0)), FlightTimes(min(85)))
        assertEquals("BA1438", f.ident)
    }
}
