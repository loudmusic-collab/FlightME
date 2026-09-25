package com.jared.flights.core.model

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Duration
import java.time.Instant
import java.time.ZoneId

class FlightProgressTest {

    private val t0: Instant = Instant.parse("2026-09-25T10:00:00Z")
    private fun min(m: Long): Instant = t0.plus(Duration.ofMinutes(m))

    private val lhr = Airport("LHR", "EGLL", "Heathrow", "London", ZoneId.of("Europe/London"))
    private val edi = Airport("EDI", "EGPH", "Edinburgh", "Edinburgh", ZoneId.of("Europe/London"))

    /** A 100-minute flight: leaves at t0, due at t0 + 100 min. */
    private fun flight(status: FlightStatus) = Flight(
        id = "test", airlineIata = "BA", airlineName = "British Airways", flightNumber = "1438",
        origin = lhr, destination = edi,
        departure = FlightTimes(min(0), actual = min(0)),
        arrival = FlightTimes(min(100), estimated = min(100)),
        status = status,
    )

    @Test fun `not departed sits at the start`() =
        assertEquals(0f, flight(FlightStatus.SCHEDULED).progressAt(min(50)), 0.001f)

    @Test fun `cancelled sits at the start`() =
        assertEquals(0f, flight(FlightStatus.CANCELLED).progressAt(min(50)), 0.001f)

    @Test fun `halfway through the flight is 0,5`() =
        assertEquals(0.5f, flight(FlightStatus.EN_ROUTE).progressAt(min(50)), 0.001f)

    @Test fun `a quarter of the way is 0,25`() =
        assertEquals(0.25f, flight(FlightStatus.EN_ROUTE).progressAt(min(25)), 0.001f)

    @Test fun `running past the estimate stays at the end`() =
        assertEquals(1f, flight(FlightStatus.EN_ROUTE).progressAt(min(130)), 0.001f)

    @Test fun `landed and arrived are at the end`() {
        assertEquals(1f, flight(FlightStatus.LANDED).progressAt(min(90)), 0.001f)
        assertEquals(1f, flight(FlightStatus.ARRIVED).progressAt(min(90)), 0.001f)
    }
}
