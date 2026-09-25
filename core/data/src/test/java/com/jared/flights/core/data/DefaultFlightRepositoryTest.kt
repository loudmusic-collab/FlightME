package com.jared.flights.core.data

import com.jared.flights.core.data.mock.MockFlightDataSource
import com.jared.flights.core.model.FlightStatus
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneId

class DefaultFlightRepositoryTest {

    private val clock = Clock.fixed(Instant.parse("2026-09-25T13:07:00Z"), ZoneId.of("Europe/London"))
    private val repository = DefaultFlightRepository(MockFlightDataSource(clock))

    @Test fun `finished flights come last`() = runBlocking {
        val flights = repository.observeTrackedFlights().first()
        val firstFinished = flights.indexOfFirst { it.isFinished }
        assertTrue(flights.drop(firstFinished).all { it.isFinished })
        assertTrue(flights.any { it.status == FlightStatus.CANCELLED })
    }

    @Test fun `active flights are in departure order`() = runBlocking {
        val active = repository.observeTrackedFlights().first().filterNot { it.isFinished }
        assertEquals(active.sortedBy { it.departure.best }, active)
    }

    @Test fun `finished flights are most recent first`() = runBlocking {
        val finished = repository.observeTrackedFlights().first().filter { it.isFinished }
        assertTrue(finished.size >= 2)
        assertEquals(finished.sortedByDescending { it.departure.best }, finished)
    }
}
