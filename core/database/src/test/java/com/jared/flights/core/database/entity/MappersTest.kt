package com.jared.flights.core.database.entity

import com.jared.flights.core.model.Airport
import com.jared.flights.core.model.Flight
import com.jared.flights.core.model.FlightStatus
import com.jared.flights.core.model.FlightTimes
import com.jared.flights.core.model.LatLon
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant
import java.time.ZoneId

class MappersTest {

    private val t0 = Instant.parse("2026-09-26T10:00:00Z")
    private val ams = Airport("AMS", "EHAM", "Schiphol", "Amsterdam", ZoneId.of("Europe/Amsterdam"), LatLon(52.3105, 4.7683))
    private val man = Airport("MAN", "EGCC", "Manchester", "Manchester", ZoneId.of("Europe/London"))
    private val lpl = Airport("LPL", "EGGP", "Liverpool John Lennon", "Liverpool", ZoneId.of("Europe/London"))

    private val everythingSet = Flight(
        id = "KL1071-test", airlineIata = "KL", airlineName = "KLM", flightNumber = "1071",
        origin = ams, destination = man, divertedTo = lpl,
        boarding = FlightTimes(t0.minusSeconds(1800), actual = t0.minusSeconds(1500)),
        departure = FlightTimes(t0, actual = t0.plusSeconds(900)),
        takeoff = FlightTimes(t0.plusSeconds(600), estimated = t0.plusSeconds(1500)),
        landing = FlightTimes(t0.plusSeconds(4000)),
        arrival = FlightTimes(t0.plusSeconds(4500), estimated = t0.plusSeconds(5400), actual = null),
        status = FlightStatus.DIVERTED,
        departureTerminal = "1", departureGate = "D6", arrivalTerminal = "2", arrivalGate = "12",
        baggageClaim = "4", aircraftType = "Embraer 190",
    )

    @Test fun `a flight survives the round trip to the database unchanged`() {
        assertEquals(everythingSet, everythingSet.toEntity(t0).toModel())
    }

    @Test fun `optional fields can be empty`() {
        val minimal = everythingSet.copy(
            divertedTo = null, boarding = null, takeoff = null, landing = null,
            departureTerminal = null, departureGate = null, arrivalTerminal = null,
            arrivalGate = null, baggageClaim = null, aircraftType = null,
        )
        assertEquals(minimal, minimal.toEntity(t0).toModel())
    }

    @Test fun `an unknown status falls back to scheduled`() {
        val row = everythingSet.toEntity(t0).copy(status = "SOMETHING_NEW")
        assertEquals(FlightStatus.SCHEDULED, row.toModel().status)
    }
}
