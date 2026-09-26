package com.jared.flights.core.data.mock

import com.jared.flights.core.model.DelaySeverity
import com.jared.flights.core.model.ConnectionHealth
import com.jared.flights.core.model.FlightStatus
import com.jared.flights.core.model.groupIntoTrips
import com.jared.flights.core.model.timeline
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneId

class MockFlightDataSourceTest {

    private val clock = Clock.fixed(Instant.parse("2026-09-25T13:07:00Z"), ZoneId.of("Europe/London"))
    private val flights = buildMockFlights(clock)

    @Test fun `has about a dozen flights with unique ids`() {
        assertTrue(flights.size in 8..14)
        assertEquals(flights.size, flights.map { it.id }.toSet().size)
    }

    @Test fun `groups into exactly two connecting trips, one comfortable and one at risk`() {
        val connecting = groupIntoTrips(flights).filter { it.isConnecting }
        assertEquals(listOf("EK2", "KL1008"), connecting.map { it.legs.first().ident }.sorted())
        val health = connecting.associate { it.legs.first().ident to it.connections.single().health }
        assertEquals(ConnectionHealth.COMFORTABLE, health["EK2"])
        assertEquals(ConnectionHealth.AT_RISK, health["KL1008"])
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

    @Test fun `milestones are in time order`() {
        flights.forEach { flight ->
            val scheduled = flight.timeline.map { it.times.scheduled }
            assertEquals(flight.ident, scheduled.sorted(), scheduled)
            val best = flight.timeline.map { it.times.best }
            assertEquals(flight.ident, best.sorted(), best)
        }
    }

    @Test fun `done milestones match the status`() {
        flights.forEach { flight ->
            val done = flight.timeline.map { it.done }
            val expected = when (flight.status) {
                FlightStatus.SCHEDULED, FlightStatus.BOARDING, FlightStatus.CANCELLED -> listOf(false, false, false, false)
                FlightStatus.EN_ROUTE, FlightStatus.DIVERTED -> listOf(true, true, false, false)
                FlightStatus.ARRIVED -> listOf(true, true, true, true)
                else -> done
            }
            assertEquals(flight.ident, expected, done)
        }
    }
}
