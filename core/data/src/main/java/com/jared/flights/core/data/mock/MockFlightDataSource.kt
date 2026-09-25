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
import kotlinx.coroutines.flow.flow
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.temporal.ChronoUnit
import javax.inject.Inject

/**
 * Scripted test flights (no network, no cost). Times are set relative to
 * "now" when the list is loaded, so the list always looks current.
 * Real airline codes and airports, made-up situations (DECISIONS #30).
 */
class MockFlightDataSource @Inject constructor(
    private val clock: Clock,
) : FlightDataSource {

    override fun observeTrackedFlights(): Flow<List<Flight>> = flow { emit(buildFlights()) }

    internal fun buildFlights(): List<Flight> {
        // Round "now" down to 5 minutes so times look like real timetable times.
        val nowMinute = clock.instant().truncatedTo(ChronoUnit.MINUTES)
        val base = nowMinute.minus(Duration.ofMinutes(nowMinute.atZone(clock.zone).minute % 5L))
        fun at(minutes: Long): Instant = base.plus(Duration.ofMinutes(minutes))

        val tomorrowMorning = LocalDate.now(clock.withZone(LHR.timeZone)).plusDays(1)
            .atTime(LocalTime.of(8, 15)).atZone(LHR.timeZone).toInstant()

        return listOf(
            // Upcoming, on time, long haul.
            Flight(
                id = "BA283-mock", airlineIata = "BA", airlineName = "British Airways",
                flightNumber = "283", origin = LHR, destination = LAX,
                departure = FlightTimes(at(180), estimated = at(180)),
                arrival = FlightTimes(at(860), estimated = at(860)),
                status = FlightStatus.SCHEDULED,
                departureTerminal = "5", aircraftType = "Airbus A350-1000",
            ),
            // Boarding, minor delay (25 min).
            Flight(
                id = "U28461-mock", airlineIata = "U2", airlineName = "easyJet",
                flightNumber = "8461", origin = LGW, destination = GVA,
                departure = FlightTimes(at(30), estimated = at(55)),
                arrival = FlightTimes(at(125), estimated = at(145)),
                status = FlightStatus.BOARDING,
                departureTerminal = "N", departureGate = "55", aircraftType = "Airbus A320neo",
            ),
            // Upcoming, major delay (75 min).
            Flight(
                id = "VS3-mock", airlineIata = "VS", airlineName = "Virgin Atlantic",
                flightNumber = "3", origin = LHR, destination = JFK,
                departure = FlightTimes(at(300), estimated = at(375)),
                arrival = FlightTimes(at(780), estimated = at(845)),
                status = FlightStatus.SCHEDULED,
                departureTerminal = "3", aircraftType = "Airbus A350-1000",
            ),
            // Cancelled.
            Flight(
                id = "FR202-mock", airlineIata = "FR", airlineName = "Ryanair",
                flightNumber = "202", origin = STN, destination = DUB,
                departure = FlightTimes(at(140)),
                arrival = FlightTimes(at(215)),
                status = FlightStatus.CANCELLED,
                aircraftType = "Boeing 737-800",
            ),
            // In the air, on time (5 min late on arrival).
            Flight(
                id = "LH921-mock", airlineIata = "LH", airlineName = "Lufthansa",
                flightNumber = "921", origin = FRA, destination = LHR,
                departure = FlightTimes(at(-55), actual = at(-50)),
                arrival = FlightTimes(at(45), estimated = at(50)),
                status = FlightStatus.EN_ROUTE,
                departureTerminal = "1", arrivalTerminal = "2", aircraftType = "Airbus A321",
            ),
            // In the air, running early.
            Flight(
                id = "EK1-mock", airlineIata = "EK", airlineName = "Emirates",
                flightNumber = "1", origin = DXB, destination = LHR,
                departure = FlightTimes(at(-400), actual = at(-405)),
                arrival = FlightTimes(at(60), estimated = at(40)),
                status = FlightStatus.EN_ROUTE,
                departureTerminal = "3", arrivalTerminal = "3", aircraftType = "Airbus A380",
            ),
            // Diverted to Stansted.
            Flight(
                id = "KL1007-mock", airlineIata = "KL", airlineName = "KLM",
                flightNumber = "1007", origin = AMS, destination = LHR,
                departure = FlightTimes(at(-60), actual = at(-45)),
                arrival = FlightTimes(at(15), estimated = at(30)),
                status = FlightStatus.DIVERTED,
                divertedTo = STN, aircraftType = "Embraer 190",
            ),
            // Arrived, 50 min late, bags on belt 4.
            Flight(
                id = "AF1680-mock", airlineIata = "AF", airlineName = "Air France",
                flightNumber = "1680", origin = CDG, destination = MAN,
                departure = FlightTimes(at(-160), actual = at(-110)),
                arrival = FlightTimes(at(-75), actual = at(-25)),
                status = FlightStatus.ARRIVED,
                departureTerminal = "2E", arrivalTerminal = "2", baggageClaim = "4",
                aircraftType = "Airbus A220-300",
            ),
            // Tomorrow morning.
            Flight(
                id = "BA1438-mock", airlineIata = "BA", airlineName = "British Airways",
                flightNumber = "1438", origin = LHR, destination = EDI,
                departure = FlightTimes(tomorrowMorning),
                arrival = FlightTimes(tomorrowMorning.plus(Duration.ofMinutes(85))),
                status = FlightStatus.SCHEDULED,
                departureTerminal = "5", aircraftType = "Airbus A320",
            ),
        )
    }
}
