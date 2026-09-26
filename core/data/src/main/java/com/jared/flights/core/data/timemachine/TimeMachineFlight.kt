package com.jared.flights.core.data.timemachine

import com.jared.flights.core.data.mock.MockAirports
import com.jared.flights.core.model.Flight
import com.jared.flights.core.model.FlightStatus
import com.jared.flights.core.model.FlightTimes
import java.time.Duration
import java.time.Instant

/**
 * The stages the time-machine flight moves through, in order.
 * [minutesAfterGateOut] is where "now" sits for that stage, measured from
 * when the plane (actually or expectedly) leaves the gate.
 */
enum class TimeMachineStep(val status: FlightStatus, val minutesAfterGateOut: Long) {
    SCHEDULED(FlightStatus.SCHEDULED, -45),
    BOARDING(FlightStatus.BOARDING, -20),
    LEFT_GATE(FlightStatus.DEPARTED, 2),
    TOOK_OFF(FlightStatus.EN_ROUTE, TAKEOFF_MIN + 2),
    HALFWAY(FlightStatus.EN_ROUTE, (TAKEOFF_MIN + LANDING_MIN) / 2),
    LANDED(FlightStatus.LANDED, LANDING_MIN + 2),
    AT_GATE(FlightStatus.ARRIVED, GATE_IN_MIN + 2),
}

// Timetable for the test flight, in minutes after it's due to leave the gate.
private const val BOARDING_MIN = -30L
private const val TAKEOFF_MIN = 15L
private const val LANDING_MIN = 100L
private const val GATE_IN_MIN = 110L

/**
 * Saved state of the time machine (DECISIONS #38).
 * [scheduledGateOutMillis]: when the flight is timetabled to leave the gate.
 */
data class TimeMachineState(
    val active: Boolean = false,
    val step: TimeMachineStep = TimeMachineStep.SCHEDULED,
    val delayMinutes: Long = 0,
    val gateChanges: Int = 0,
    val scheduledGateOutMillis: Long = 0,
)

const val TIME_MACHINE_FLIGHT_ID = "FM100-timemachine"

private val GATES = listOf("A10", "A14", "B32", "B40")

/**
 * FM 100 London → Edinburgh, as it looks at the current [TimeMachineState.step].
 * Steps that have been reached get actual times; later ones get estimates.
 */
fun buildTimeMachineFlight(state: TimeMachineState): Flight {
    val scheduledGateOut = Instant.ofEpochMilli(state.scheduledGateOutMillis)
    val delay = Duration.ofMinutes(state.delayMinutes)
    // The moment in the flight's life that this step represents.
    val stepMoment = scheduledGateOut.plus(delay).plus(Duration.ofMinutes(state.step.minutesAfterGateOut))

    fun times(minutesAfterGateOut: Long): FlightTimes {
        val scheduled = scheduledGateOut.plus(Duration.ofMinutes(minutesAfterGateOut))
        val expected = scheduled.plus(delay)
        return if (!expected.isAfter(stepMoment)) {
            FlightTimes(scheduled, actual = expected)
        } else {
            FlightTimes(scheduled, estimated = expected)
        }
    }

    val arrived = state.step == TimeMachineStep.AT_GATE
    return Flight(
        id = TIME_MACHINE_FLIGHT_ID,
        airlineIata = "FM",
        airlineName = "FlightME Test",
        flightNumber = "100",
        origin = MockAirports.LHR,
        destination = MockAirports.EDI,
        boarding = times(BOARDING_MIN),
        departure = times(0),
        takeoff = times(TAKEOFF_MIN),
        landing = times(LANDING_MIN),
        arrival = times(GATE_IN_MIN),
        status = state.step.status,
        departureTerminal = "5",
        departureGate = GATES[state.gateChanges % GATES.size],
        arrivalTerminal = "1",
        arrivalGate = if (state.step >= TimeMachineStep.LANDED) "7" else null,
        baggageClaim = if (arrived) "3" else null,
        aircraftType = "Airbus A320",
    )
}
