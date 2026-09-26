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

    @Test fun `trips put connecting legs together`() = runBlocking {
        val trip = repository.observeTripFor("EK432-mock").first()
        assertEquals(listOf("EK2", "EK432"), trip?.legs?.map { it.ident })
    }

    @Test fun `splitting a trip makes two trips`() = runBlocking {
        val before = repository.observeTrips().first().size
        repository.splitTripBefore("EK432-mock")
        val after = repository.observeTrips().first()
        assertEquals(before + 1, after.size)
        assertEquals(listOf("EK432"), repository.observeTripFor("EK432-mock").first()?.legs?.map { it.ident })
    }

    @Test fun `a single flight can be found by id`() = runBlocking {
        assertEquals("VS3", repository.observeFlight("VS3-mock").first()?.ident)
        assertEquals(null, repository.observeFlight("nope").first())
    }

    @Test fun `finished flights are most recent first`() = runBlocking {
        val finished = repository.observeTrackedFlights().first().filter { it.isFinished }
        assertTrue(finished.size >= 2)
        assertEquals(finished.sortedByDescending { it.departure.best }, finished)
    }
}
