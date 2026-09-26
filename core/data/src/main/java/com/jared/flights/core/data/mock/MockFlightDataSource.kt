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
import com.jared.flights.core.data.mock.MockAirports.LPL
import com.jared.flights.core.data.mock.MockAirports.MAN
import com.jared.flights.core.data.mock.MockAirports.SIN
import com.jared.flights.core.data.mock.MockAirports.STN
import com.jared.flights.core.model.Flight
import com.jared.flights.core.model.FlightStatus
import com.jared.flights.core.model.FlightTimes
import com.jared.flights.core.data.DevSettings
import com.jared.flights.core.data.timemachine.buildTimeMachineFlight
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import java.io.IOException
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
    private val devSettings: DevSettings,
) : FlightDataSource {

    // Built once per app run, so every screen sees exactly the same times.
    private val flights: List<Flight> by lazy { buildMockFlights(clock) }

    /** Online unless "Simulate offline" is switched on in the developer options. */
    override fun observeOnline(): Flow<Boolean> =
        devSettings.simulateOffline.map { !it }.distinctUntilChanged()

    /**
     * The scripted flights, plus the time-machine flight (FM 100) while it's
     * switched on. Sends again every time the time machine moves.
     */
    private val flightsWithTimeMachine: Flow<List<Flight>> =
        devSettings.timeMachine.map { tm ->
            if (tm.active) flights + buildTimeMachineFlight(tm) else flights
        }

    /** Sends the flights whenever we're (back) online or they change; nothing while offline. */
    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observeTrackedFlights(): Flow<List<Flight>> =
        observeOnline().flatMapLatest { online -> if (online) flightsWithTimeMachine else emptyFlow() }

    /** Pretends to call a server: short wait, then the flights (or fails if "offline"). */
    override suspend fun fetchTrackedFlights(): List<Flight> {
        delay(FAKE_NETWORK_DELAY_MILLIS)
        if (devSettings.simulateOffline.first()) throw IOException("Simulated offline")
        return flightsWithTimeMachine.first()
    }

    private companion object {
        const val FAKE_NETWORK_DELAY_MILLIS = 800L
    }
}

/**
 * The scripted flights, with times placed around the [clock]'s "now".
 */
internal fun buildMockFlights(clock: Clock): List<Flight> {
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
            // Boarding started just now, 20 min later than planned.
            boarding = act(-20, 0),
            departure = est(10, 35), takeoff = est(22, 47),
            landing = est(98, 118), arrival = est(105, 125),
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
            id = "LH946-mock", airlineIata = "LH", airlineName = "Lufthansa",
            flightNumber = "946", origin = FRA, destination = MAN,
            departure = act(-55, -50), takeoff = act(-43, -38),
            landing = est(38, 43), arrival = est(45, 50),
            status = FlightStatus.EN_ROUTE,
            departureTerminal = "1", departureGate = "A26", arrivalTerminal = "1",
            aircraftType = "Airbus A321",
        ),
        // In the air, running early.
        Flight(
            id = "EK17-mock", airlineIata = "EK", airlineName = "Emirates",
            flightNumber = "17", origin = DXB, destination = MAN,
            departure = act(-400, -405), takeoff = act(-385, -388),
            landing = est(50, 30), arrival = est(60, 40),
            status = FlightStatus.EN_ROUTE,
            departureTerminal = "3", departureGate = "A12", arrivalTerminal = "2",
            aircraftType = "Airbus A380",
        ),
        // Diverted to Liverpool.
        Flight(
            id = "KL1071-mock", airlineIata = "KL", airlineName = "KLM",
            flightNumber = "1071", origin = AMS, destination = MAN,
            departure = act(-60, -45), takeoff = act(-50, -35),
            landing = est(8, 23), arrival = est(15, 30),
            status = FlightStatus.DIVERTED,
            divertedTo = LPL, departureGate = "D6", aircraftType = "Embraer 190",
        ),

        // ---- Trip: London → Singapore via Dubai, comfortable 2h 10m connection ----
        Flight(
            id = "EK2-mock", airlineIata = "EK", airlineName = "Emirates",
            flightNumber = "2", origin = LHR, destination = DXB,
            departure = est(240, 240), takeoff = est(255, 255),
            landing = est(645, 645), arrival = est(655, 655),
            status = FlightStatus.SCHEDULED,
            departureTerminal = "3", arrivalTerminal = "3", aircraftType = "Airbus A380",
        ),
        Flight(
            id = "EK432-mock", airlineIata = "EK", airlineName = "Emirates",
            flightNumber = "432", origin = DXB, destination = SIN,
            departure = est(785, 785), takeoff = est(800, 800),
            landing = est(1220, 1220), arrival = est(1230, 1230),
            status = FlightStatus.SCHEDULED,
            departureTerminal = "3", arrivalTerminal = "1", aircraftType = "Boeing 777-300ER",
        ),

        // ---- Trip: London → New York via Amsterdam, first leg 40 min late: connection at risk ----
        // Planned 1h 25m to change planes; the delay cuts it to 45 min.
        Flight(
            id = "KL1008-mock", airlineIata = "KL", airlineName = "KLM",
            flightNumber = "1008", origin = LHR, destination = AMS,
            departure = est(20, 60), takeoff = est(32, 72),
            landing = est(78, 118), arrival = est(85, 125),
            status = FlightStatus.SCHEDULED,
            departureTerminal = "4", aircraftType = "Embraer 190",
        ),
        Flight(
            id = "KL643-mock", airlineIata = "KL", airlineName = "KLM",
            flightNumber = "643", origin = AMS, destination = JFK,
            departure = est(170, 170), takeoff = est(185, 185),
            landing = est(670, 670), arrival = est(680, 680),
            status = FlightStatus.SCHEDULED,
            arrivalTerminal = "4", aircraftType = "Boeing 777-200",
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
