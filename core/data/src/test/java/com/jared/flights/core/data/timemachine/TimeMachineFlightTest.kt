package com.jared.flights.core.data.timemachine

import com.jared.flights.core.model.FlightStatus
import com.jared.flights.core.model.MilestoneType
import com.jared.flights.core.model.progressAt
import com.jared.flights.core.model.timeline
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Duration
import java.time.Instant

class TimeMachineFlightTest {

    private val gateOut = Instant.parse("2026-09-26T14:00:00Z")

    private fun state(step: TimeMachineStep, delay: Long = 0, gates: Int = 0) = TimeMachineState(
        active = true, step = step, delayMinutes = delay, gateChanges = gates,
        scheduledGateOutMillis = gateOut.toEpochMilli(),
    )

    /** Which milestones count as done at each step. */
    private fun doneAt(step: TimeMachineStep) = buildTimeMachineFlight(state(step)).timeline.map { it.done }

    @Test fun `status follows the step`() {
        assertEquals(
            listOf(
                FlightStatus.SCHEDULED, FlightStatus.BOARDING, FlightStatus.DEPARTED, FlightStatus.EN_ROUTE,
                FlightStatus.EN_ROUTE, FlightStatus.LANDED, FlightStatus.ARRIVED,
            ),
            TimeMachineStep.entries.map { buildTimeMachineFlight(state(it)).status },
        )
    }

    @Test fun `milestones fill in as the flight progresses`() {
        assertEquals(listOf(false, false, false, false, false), doneAt(TimeMachineStep.SCHEDULED))
        assertEquals(listOf(true, false, false, false, false), doneAt(TimeMachineStep.BOARDING))
        assertEquals(listOf(true, true, false, false, false), doneAt(TimeMachineStep.LEFT_GATE))
        assertEquals(listOf(true, true, true, false, false), doneAt(TimeMachineStep.TOOK_OFF))
        assertEquals(listOf(true, true, true, false, false), doneAt(TimeMachineStep.HALFWAY))
        assertEquals(listOf(true, true, true, true, false), doneAt(TimeMachineStep.LANDED))
        assertEquals(listOf(true, true, true, true, true), doneAt(TimeMachineStep.AT_GATE))
    }

    @Test fun `halfway puts the plane near the middle of the route`() {
        val flight = buildTimeMachineFlight(state(TimeMachineStep.HALFWAY))
        val now = gateOut.plus(Duration.ofMinutes(TimeMachineStep.HALFWAY.minutesAfterGateOut))
        val progress = flight.progressAt(now)
        assertTrue("progress was $progress", progress in 0.4f..0.6f)
    }

    @Test fun `a delay pushes every time later but keeps the timetable`() {
        val onTime = buildTimeMachineFlight(state(TimeMachineStep.SCHEDULED))
        val late = buildTimeMachineFlight(state(TimeMachineStep.SCHEDULED, delay = 30))
        assertEquals(onTime.departure.scheduled, late.departure.scheduled)
        assertEquals(Duration.ofMinutes(30), late.departure.delay)
        assertEquals(Duration.ofMinutes(30), late.timeline.first { it.type == MilestoneType.GATE_IN }.times.delay)
    }

    @Test fun `changing the gate changes the departure gate`() {
        val before = buildTimeMachineFlight(state(TimeMachineStep.BOARDING)).departureGate
        val after = buildTimeMachineFlight(state(TimeMachineStep.BOARDING, gates = 1)).departureGate
        assertNotEquals(before, after)
    }

    @Test fun `baggage belt appears on arrival`() {
        assertEquals(null, buildTimeMachineFlight(state(TimeMachineStep.LANDED)).baggageClaim)
        assertEquals("3", buildTimeMachineFlight(state(TimeMachineStep.AT_GATE)).baggageClaim)
    }
}
