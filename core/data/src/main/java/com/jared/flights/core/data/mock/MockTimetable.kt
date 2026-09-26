package com.jared.flights.core.data.mock

import com.jared.flights.core.data.mock.MockAirports.AMS
import com.jared.flights.core.data.mock.MockAirports.BCN
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
import com.jared.flights.core.data.mock.MockAirports.SIN
import com.jared.flights.core.data.mock.MockAirports.STN
import com.jared.flights.core.model.Airport
import com.jared.flights.core.model.Flight
import com.jared.flights.core.model.FlightNumber
import com.jared.flights.core.model.FlightStatus
import com.jared.flights.core.model.FlightTimes
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime

/**
 * A pretend daily timetable that mock search looks things up in (no network, no cost).
 * Real airline codes and airports; times are made up (DECISIONS #30).
 */
internal object MockTimetable {

    private class Entry(
        val airline: String,
        val airlineName: String,
        val number: String,
        val origin: Airport,
        val destination: Airport,
        /** Leaves the gate at this local time at [origin]. */
        val departs: LocalTime,
        /** Gate to gate. */
        val blockMinutes: Long,
        val aircraft: String,
        val departureTerminal: String? = null,
        val arrivalTerminal: String? = null,
    )

    private val entries = listOf(
        Entry("FM", "FlightME Test", "123", LHR, CDG, LocalTime.of(9, 30), 80, "Airbus A320", "5", "2E"),
        Entry("BA", "British Airways", "117", LHR, JFK, LocalTime.of(8, 25), 480, "Airbus A380", "5", "8"),
        Entry("BA", "British Airways", "283", LHR, LAX, LocalTime.of(15, 20), 680, "Airbus A350-1000", "5", "B"),
        Entry("BA", "British Airways", "1438", LHR, EDI, LocalTime.of(8, 15), 85, "Airbus A320", "5"),
        Entry("BA", "British Airways", "2762", LGW, GVA, LocalTime.of(7, 5), 95, "Airbus A320", "S", "1"),
        Entry("VS", "Virgin Atlantic", "3", LHR, JFK, LocalTime.of(11, 0), 480, "Airbus A350-1000", "3", "4"),
        Entry("U2", "easyJet", "8461", LGW, GVA, LocalTime.of(10, 40), 95, "Airbus A320neo", "N", "1"),
        Entry("U2", "easyJet", "8715", LGW, BCN, LocalTime.of(6, 30), 125, "Airbus A320neo", "N", "2"),
        Entry("FR", "Ryanair", "202", STN, DUB, LocalTime.of(12, 20), 75, "Boeing 737-800"),
        Entry("LH", "Lufthansa", "901", FRA, LHR, LocalTime.of(7, 30), 100, "Airbus A321", "1", "2"),
        Entry("LH", "Lufthansa", "946", FRA, MAN, LocalTime.of(9, 50), 100, "Airbus A321", "1", "1"),
        Entry("AF", "Air France", "1680", CDG, MAN, LocalTime.of(13, 45), 85, "Airbus A220-300", "2E", "2"),
        Entry("KL", "KLM", "1008", LHR, AMS, LocalTime.of(10, 5), 65, "Embraer 190", "4"),
        Entry("KL", "KLM", "643", AMS, JFK, LocalTime.of(13, 30), 510, "Boeing 777-200", null, "4"),
        Entry("EK", "Emirates", "2", LHR, DXB, LocalTime.of(14, 15), 415, "Airbus A380", "3", "3"),
        Entry("EK", "Emirates", "17", DXB, MAN, LocalTime.of(3, 5), 460, "Airbus A380", "3", "2"),
        Entry("EK", "Emirates", "432", DXB, SIN, LocalTime.of(3, 20), 445, "Boeing 777-300ER", "3", "1"),
        Entry("SQ", "Singapore Airlines", "317", SIN, LHR, LocalTime.of(9, 5), 830, "Airbus A350-900", "3", "2"),
        Entry("DL", "Delta", "1", JFK, LHR, LocalTime.of(22, 0), 420, "Airbus A330-900", "4", "3"),
        Entry("AA", "American Airlines", "100", JFK, LHR, LocalTime.of(18, 30), 420, "Boeing 777-300ER", "8", "3"),
    )

    /**
     * FM 123 etc. on [date] (local date at the departure airport), or null if
     * there's no such flight. Its status is worked out from [now]: scheduled,
     * boarding, in the air or arrived.
     */
    fun find(number: FlightNumber, date: LocalDate, now: Instant): Flight? {
        val entry = entries.find { it.airline == number.airline && it.number == number.number } ?: return null
        return build(entry, date, now)
    }

    /** Rebuilds a flight from its id (e.g. "FM123_2026-09-27"), or null if it isn't one of ours. */
    fun findById(id: String, now: Instant): Flight? {
        val (ident, dateText) = id.split('_').takeIf { it.size == 2 } ?: return null
        val number = FlightNumber.parse(ident) ?: return null
        val date = runCatching { LocalDate.parse(dateText) }.getOrNull() ?: return null
        return find(number, date, now)
    }

    fun idFor(number: FlightNumber, date: LocalDate) = "${number.ident}_$date"

    private fun build(e: Entry, date: LocalDate, now: Instant): Flight {
        val gateOut = date.atTime(e.departs).atZone(e.origin.timeZone).toInstant()
        fun at(minutes: Long) = gateOut.plus(Duration.ofMinutes(minutes))

        val takeoffMin = 15L
        val landingMin = e.blockMinutes - 10
        val gateIn = at(e.blockMinutes)
        // "Live" estimates only within 2 days of departure, like real data feeds.
        val live = Duration.between(now, gateOut) < Duration.ofDays(2)

        fun times(minutes: Long): FlightTimes {
            val t = at(minutes)
            return when {
                !t.isAfter(now) -> FlightTimes(t, actual = t)
                live -> FlightTimes(t, estimated = t)
                else -> FlightTimes(t)
            }
        }

        val status = when {
            !gateIn.isAfter(now) -> FlightStatus.ARRIVED
            !at(landingMin).isAfter(now) -> FlightStatus.LANDED
            !at(takeoffMin).isAfter(now) -> FlightStatus.EN_ROUTE
            !gateOut.isAfter(now) -> FlightStatus.DEPARTED
            !at(-30).isAfter(now) -> FlightStatus.BOARDING
            else -> FlightStatus.SCHEDULED
        }

        return Flight(
            id = idFor(FlightNumber(e.airline, e.number), date),
            airlineIata = e.airline,
            airlineName = e.airlineName,
            flightNumber = e.number,
            origin = e.origin,
            destination = e.destination,
            departure = times(0),
            takeoff = times(takeoffMin),
            landing = times(landingMin),
            arrival = times(e.blockMinutes),
            status = status,
            departureTerminal = e.departureTerminal,
            arrivalTerminal = e.arrivalTerminal,
            aircraftType = e.aircraft,
        )
    }
}
