package com.jared.flights.core.data.mock

import com.jared.flights.core.model.DelaySeverity
import com.jared.flights.core.model.FlightStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneId

class MockFlightDataSourceTest {

    private val clock = Clock.fixed(Instant.parse("2026-09-25T13:07:00Z"), ZoneId.of("Europe/London"))
    private val flights = MockFlightDataSource(clock).buildFlights()

    @Test fun `has about eight flights with unique ids`() {
        assertTrue(flights.size in 8..10)
        assertEquals(flights.size, flights.map { it.id }.toSet().size)
    }

    @Test fun `covers on time, minor and major delays`() {
        val active = flights.filter { it.status != FlightStatus.CANCELLED && it.status != FlightStatus.DIVERTED }
        val severities = active.map { it.delaySeverity }.toSet()
        assertEquals(DelaySeverity.entries.toSet(), severities)
    }

    @Test fun `covers cancelled, diverted, in the air and arrived`() {
        val statuses = flights.map { it.status }.toSet()
        assertTrue(
            statuses.containsAll(
                listOf(FlightStatus.CANCELLED, FlightStatus.DIVERTED, FlightStatus.EN_ROUTE, FlightStatus.ARRIVED),
            ),
        )
    }

    @Test fun `times are rounded to five minutes`() {
        flights.forEach { assertEquals(0, it.departure.scheduled.epochSecond % 300) }
    }

    @Test fun `every flight arrives after it departs`() {
        flights.forEach { assertTrue(it.ident, it.arrival.scheduled > it.departure.scheduled) }
    }
}
