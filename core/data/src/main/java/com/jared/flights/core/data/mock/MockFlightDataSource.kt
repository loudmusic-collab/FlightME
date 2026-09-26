package com.jared.flights.core.data.mock

import com.jared.flights.core.data.FlightDataSource
import com.jared.flights.core.data.mock.MockAirports.AMS
import com.jared.flights.core.data.mock.MockAirports.CDG
import com.jared.flights.core.data.mock.MockAirports.DUB
import com.jared.flights.core.data.mock.MockAirports.DXB
import com.jared.flights.core.data.mock.MockAirports.EDI
import com.jared.flights.core.data.mock.MockAirports.FRA
import com.jared.flights.core.data.mock.MockAirports.GVA
import com.jared.flights.core.data.mock.MockAirports.JFK
import com.jared.flights.core.data.mock.MockAirports.LAX
import com.jared.flights.core.data.mock.MockAirports.LGW
import com.jared.flights.core.data.mock.MockAirports.LHR
import com.jared.flights.core.data.mock.MockAirports.MAN
import com.jared.flights.core.data.mock.MockAirports.STN
import com.jared.flights.core.model.Flight
import com.jared.flights.core.model.FlightStatus
import com.jared.flights.core.model.FlightTimes
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.temporal.ChronoUnit
import javax.inject.Inject

/**
 * Scripted test flights (no network, no cost). Times are set relative to
 * "now" when the app starts, so the list always looks current.
 * Real airline codes and airports, made-up situations (DECISIONS #30).
 */
class MockFlightDataSource @Inject constructor(
    private val clock: Clock,
) : FlightDataSource {

    // Built once, so every screen sees exactly the same times.
    private val flights: List<Flight> by lazy { buildFlights() }

    override fun observeTrackedFlights(): Flow<List<Flight>> = flowOf(flights)

    internal fun buildFlights(): List<Flight> {
        // Round "now" down to 5 minutes so times look like real timetable times.
        val nowMinute = clock.instant().truncatedTo(ChronoUnit.MINUTES)
        val base = nowMinute.minus(Duration.ofMinutes(nowMinute.atZone(clock.zone).minute % 5L))
        fun at(minutes: Long): Instant = base.plus(Duration.ofMinutes(minutes))

        // Timetable time only (nothing known yet).
        fun sched(m: Long) = FlightTimes(at(m))
        // Timetable time + latest estimate.
        fun est(scheduled: Long, estimated: Long) = FlightTimes(at(scheduled), estimated = at(estimated))
        // Timetable time + what actually happened.
        fun act(scheduled: Long, actual: Long) = FlightTimes(at(scheduled), actual = at(actual))

        val tomorrowMorning = LocalDate.now(clock.withZone(LHR.timeZone)).plusDays(1)
            .atTime(LocalTime.of(8, 15)).atZone(LHR.timeZone).toInstant()
        fun tomorrow(m: Long) = FlightTimes(tomorrowMorning.plus(Duration.ofMinutes(m)))

        return listOf(
            // Upcoming, on time, long haul.
            Flight(
                id = "BA283-mock", airlineIata = "BA", airlineName = "British Airways",
                flightNumber = "283", origin = LHR, destination = LAX,
                departure = est(180, 180), takeoff = est(195, 195),
                landing = est(848, 848), arrival = est(860, 860),
                status = FlightStatus.SCHEDULED,
                departureTerminal = "5", arrivalTerminal = "B", aircraftType = "Airbus A350-1000",
            ),
            // Boarding, minor delay (25 min).
            Flight(
                id = "U28461-mock", airlineIata = "U2", airlineName = "easyJet",
                flightNumber = "8461", origin = LGW, destination = GVA,
                departure = est(30, 55), takeoff = est(42, 67),
                landing = est(118, 138), arrival = est(125, 145),
                status = FlightStatus.BOARDING,
                departureTerminal = "N", departureGate = "55", arrivalTerminal = "1",
                aircraftType = "Airbus A320neo",
            ),
            // Upcoming, major delay (75 min).
            Flight(
                id = "VS3-mock", airlineIata = "VS", airlineName = "Virgin Atlantic",
                flightNumber = "3", origin = LHR, destination = JFK,
                departure = est(300, 375), takeoff = est(312, 387),
                landing = est(768, 833), arrival = est(780, 845),
                status = FlightStatus.SCHEDULED,
                departureTerminal = "3", arrivalTerminal = "4", aircraftType = "Airbus A350-1000",
            ),
            // Cancelled.
            Flight(
                id = "FR202-mock", airlineIata = "FR", airlineName = "Ryanair",
                flightNumber = "202", origin = STN, destination = DUB,
                departure = sched(140), takeoff = sched(152),
                landing = sched(208), arrival = sched(215),
                status = FlightStatus.CANCELLED,
                aircraftType = "Boeing 737-800",
            ),
            // In the air, on time (5 min late on arrival).
            Flight(
                id = "LH921-mock", airlineIata = "LH", airlineName = "Lufthansa",
                flightNumber = "921", origin = FRA, destination = LHR,
                departure = act(-55, -50), takeoff = act(-43, -38),
                landing = est(38, 43), arrival = est(45, 50),
                status = FlightStatus.EN_ROUTE,
                departureTerminal = "1", departureGate = "A26", arrivalTerminal = "2",
                aircraftType = "Airbus A321",
            ),
            // In the air, running early.
            Flight(
                id = "EK1-mock", airlineIata = "EK", airlineName = "Emirates",
                flightNumber = "1", origin = DXB, destination = LHR,
                departure = act(-400, -405), takeoff = act(-385, -388),
                landing = est(50, 30), arrival = est(60, 40),
                status = FlightStatus.EN_ROUTE,
                departureTerminal = "3", departureGate = "A12", arrivalTerminal = "3",
                aircraftType = "Airbus A380",
            ),
            // Diverted to Stansted.
            Flight(
                id = "KL1007-mock", airlineIata = "KL", airlineName = "KLM",
                flightNumber = "1007", origin = AMS, destination = LHR,
                departure = act(-60, -45), takeoff = act(-50, -35),
                landing = est(8, 23), arrival = est(15, 30),
                status = FlightStatus.DIVERTED,
                divertedTo = STN, departureGate = "D6", aircraftType = "Embraer 190",
            ),
            // Arrived, 50 min late, bags on belt 4.
            Flight(
                id = "AF1680-mock", airlineIata = "AF", airlineName = "Air France",
                flightNumber = "1680", origin = CDG, destination = MAN,
                departure = act(-160, -110), takeoff = act(-148, -96),
                landing = act(-82, -33), arrival = act(-75, -25),
                status = FlightStatus.ARRIVED,
                departureTerminal = "2E", departureGate = "L41",
                arrivalTerminal = "2", arrivalGate = "212", baggageClaim = "4",
                aircraftType = "Airbus A220-300",
            ),
            // Tomorrow morning, nothing announced yet.
            Flight(
                id = "BA1438-mock", airlineIata = "BA", airlineName = "British Airways",
                flightNumber = "1438", origin = LHR, destination = EDI,
                departure = tomorrow(0), takeoff = tomorrow(15),
                landing = tomorrow(78), arrival = tomorrow(85),
                status = FlightStatus.SCHEDULED,
                departureTerminal = "5", aircraftType = "Airbus A320",
            ),
        )
    }
}
