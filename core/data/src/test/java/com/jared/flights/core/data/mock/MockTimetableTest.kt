package com.jared.flights.core.data.mock

import com.jared.flights.core.model.FlightNumber
import com.jared.flights.core.model.FlightStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Instant
import java.time.LocalDate

class MockTimetableTest {

    private val date = LocalDate.of(2026, 9, 27)
    private val fm123 = FlightNumber("FM", "123") // LHR → CDG, leaves 09:30 London time

    private fun at(isoInstant: String) = Instant.parse(isoInstant)

    @Test fun `FM 123 can be found`() {
        val flight = MockTimetable.find(fm123, date, at("2026-09-26T12:00:00Z"))
        assertNotNull(flight)
        assertEquals("LHR", flight!!.origin.iata)
        assertEquals("CDG", flight.destination.iata)
        assertEquals("FM123_2026-09-27", flight.id)
        // 09:30 in London (summer time, UTC+1) is 08:30 UTC.
        assertEquals(at("2026-09-27T08:30:00Z"), flight.departure.scheduled)
    }

    @Test fun `an unknown flight number finds nothing`() {
        assertNull(MockTimetable.find(FlightNumber("ZZ", "9"), date, at("2026-09-26T12:00:00Z")))
    }

    @Test fun `status is worked out from the time`() {
        fun statusAt(utc: String) = MockTimetable.find(fm123, date, at(utc))!!.status
        assertEquals(FlightStatus.SCHEDULED, statusAt("2026-09-27T07:00:00Z"))
        assertEquals(FlightStatus.BOARDING, statusAt("2026-09-27T08:10:00Z"))
        assertEquals(FlightStatus.EN_ROUTE, statusAt("2026-09-27T09:00:00Z"))
        assertEquals(FlightStatus.ARRIVED, statusAt("2026-09-27T12:00:00Z"))
    }

    @Test fun `a flight can be rebuilt from its id`() {
        val now = at("2026-09-26T12:00:00Z")
        val original = MockTimetable.find(fm123, date, now)
        assertEquals(original, MockTimetable.findById("FM123_2026-09-27", now))
        assertNull(MockTimetable.findById("nonsense", now))
    }
}
