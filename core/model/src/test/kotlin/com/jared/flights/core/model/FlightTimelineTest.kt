package com.jared.flights.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Duration
import java.time.Instant
import java.time.ZoneId

class FlightTimelineTest {

    private val t0: Instant = Instant.parse("2026-09-25T10:00:00Z")
    private fun min(m: Long): Instant = t0.plus(Duration.ofMinutes(m))

    private val lhr = Airport("LHR", "EGLL", "Heathrow", "London", ZoneId.of("Europe/London"))
    private val edi = Airport("EDI", "EGPH", "Edinburgh", "Edinburgh", ZoneId.of("Europe/London"))
    private val gla = Airport("GLA", "EGPF", "Glasgow", "Glasgow", ZoneId.of("Europe/London"))

    /** In the air: left the gate and took off, not yet landed. */
    private val inAir = Flight(
        id = "test", airlineIata = "BA", airlineName = "British Airways", flightNumber = "1438",
        origin = lhr, destination = edi,
        departure = FlightTimes(min(0), actual = min(5)),
        takeoff = FlightTimes(min(15), actual = min(20)),
        landing = FlightTimes(min(75), estimated = min(80)),
        arrival = FlightTimes(min(85), estimated = min(90)),
        status = FlightStatus.EN_ROUTE,
    )

    @Test fun `five milestones in order`() {
        assertEquals(
            listOf(
                MilestoneType.BOARDING, MilestoneType.GATE_OUT, MilestoneType.TAKEOFF,
                MilestoneType.LANDING, MilestoneType.GATE_IN,
            ),
            inAir.timeline.map { it.type },
        )
    }

    @Test fun `milestones with an actual time are done`() {
        assertEquals(listOf(true, true, true, false, false), inAir.timeline.map { it.done })
    }

    @Test fun `without runway times only boarding and the gate milestones remain`() {
        val gateOnly = inAir.copy(takeoff = null, landing = null)
        assertEquals(
            listOf(MilestoneType.BOARDING, MilestoneType.GATE_OUT, MilestoneType.GATE_IN),
            gateOnly.timeline.map { it.type },
        )
    }

    @Test fun `boarding is estimated 30 minutes before departure when not given`() {
        val scheduled = inAir.copy(status = FlightStatus.SCHEDULED, departure = FlightTimes(min(0), estimated = min(20)))
        val boarding = scheduled.timeline.first()
        assertEquals(min(-30), boarding.times.scheduled)
        assertEquals(min(-10), boarding.times.best) // follows the 20 min departure delay
        assertFalse(boarding.done)
    }

    @Test fun `boarding is done once the status says boarding`() {
        val boarding = inAir.copy(status = FlightStatus.BOARDING, departure = FlightTimes(min(0)))
        assertTrue(boarding.timeline.first().done)
    }

    @Test fun `a given boarding time is used as is`() {
        val given = inAir.copy(boarding = FlightTimes(min(-40), actual = min(-38)))
        assertEquals(min(-38), given.timeline.first().times.best)
    }

    @Test fun `taxi and air times use the best times`() {
        val (_, gateOut, takeoff, landing) = inAir.timeline
        assertEquals(Duration.ofMinutes(15), durationBetween(gateOut, takeoff)) // 5 → 20
        assertEquals(Duration.ofMinutes(60), durationBetween(takeoff, landing)) // 20 → 80
    }

    @Test fun `diverted flights land at the diversion airport`() {
        val diverted = inAir.copy(status = FlightStatus.DIVERTED, divertedTo = gla)
        assertEquals(gla, diverted.timeline.last().airport)
        assertTrue(diverted.timeline[1].airport == lhr)
        assertFalse(diverted.timeline.last().done)
    }
}
