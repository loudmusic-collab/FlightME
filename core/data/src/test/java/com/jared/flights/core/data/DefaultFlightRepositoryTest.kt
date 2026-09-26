package com.jared.flights.core.data

import com.jared.flights.core.data.mock.buildMockFlights
import com.jared.flights.core.model.Flight
import com.jared.flights.core.model.FlightStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneId

class DefaultFlightRepositoryTest {

    private val clock = Clock.fixed(Instant.parse("2026-09-25T13:07:00Z"), ZoneId.of("Europe/London"))
    private val mockFlights = buildMockFlights(clock)

    private val flightDao = FakeFlightDao()
    private val splitDao = FakeTripSplitDao()
    private val syncDao = FakeSyncStateDao()

    private fun repository(online: Boolean = true, sourceFlights: List<Flight> = mockFlights): DefaultFlightRepository {
        val source = FakeFlightDataSource(sourceFlights, online)
        val sync = FlightSync(source, flightDao, syncDao, clock, CoroutineScope(Dispatchers.Unconfined))
        return DefaultFlightRepository(flightDao, splitDao, syncDao, source, sync)
    }

    private val sync = FlightSync(
        FakeFlightDataSource(mockFlights), flightDao, syncDao, clock, CoroutineScope(Dispatchers.Unconfined),
    )

    /** Put the mock flights into the (fake) database, like a sync would. */
    private fun saved(online: Boolean = true): FlightRepository {
        runBlocking { sync.save(mockFlights) }
        return repository(online)
    }

    @Test fun `screens read what the sync saved`() = runBlocking {
        assertEquals(mockFlights.size, saved().observeTrackedFlights().first().size)
    }

    @Test fun `nothing saved yet means an empty list`() = runBlocking {
        assertTrue(repository().observeTrackedFlights().first().isEmpty())
    }

    @Test fun `saved flights are still there when offline`() = runBlocking {
        val repo = saved(online = false)
        assertEquals(mockFlights.size, repo.observeTrackedFlights().first().size)
        val status = repo.observeSyncStatus().first()
        assertFalse(status.isOnline)
        assertEquals(clock.instant(), status.lastSuccessAt)
    }

    @Test fun `a sync removes flights that are no longer tracked`() = runBlocking {
        val repo = saved()
        sync.save(mockFlights.drop(1))
        assertEquals(mockFlights.size - 1, repo.observeTrackedFlights().first().size)
    }

    @Test fun `finished flights come last`() = runBlocking {
        val flights = saved().observeTrackedFlights().first()
        val firstFinished = flights.indexOfFirst { it.isFinished }
        assertTrue(flights.drop(firstFinished).all { it.isFinished })
        assertTrue(flights.any { it.status == FlightStatus.CANCELLED })
    }

    @Test fun `active flights are in departure order`() = runBlocking {
        val active = saved().observeTrackedFlights().first().filterNot { it.isFinished }
        assertEquals(active.sortedBy { it.departure.best }, active)
    }

    @Test fun `trips put connecting legs together`() = runBlocking {
        val trip = saved().observeTripFor("EK432-mock").first()
        assertEquals(listOf("EK2", "EK432"), trip?.legs?.map { it.ident })
    }

    @Test fun `splitting a trip is saved and makes two trips`() = runBlocking {
        val repo = saved()
        val before = repo.observeTrips().first().size
        repo.splitTripBefore("EK432-mock")
        // A brand-new repository (like after an app restart) still sees the split.
        val afterRestart = repository()
        assertEquals(before + 1, afterRestart.observeTrips().first().size)
        assertEquals(listOf("EK432"), afterRestart.observeTripFor("EK432-mock").first()?.legs?.map { it.ident })
    }

    @Test fun `a single flight can be found by id`() = runBlocking {
        val repo = saved()
        assertEquals("VS3", repo.observeFlight("VS3-mock").first()?.ident)
        assertEquals(null, repo.observeFlight("nope").first())
    }

    @Test fun `pull to refresh saves the latest flights`() = runBlocking {
        val repo = repository(sourceFlights = mockFlights.take(3))
        assertTrue(repo.refresh())
        assertEquals(3, repo.observeTrackedFlights().first().size)
        assertEquals(clock.instant(), repo.observeSyncStatus().first().lastSuccessAt)
    }

    @Test fun `pull to refresh while offline fails and keeps the saved flights`() = runBlocking {
        saved()
        val offline = repository(online = false, sourceFlights = emptyList())
        assertFalse(offline.refresh())
        assertEquals(mockFlights.size, offline.observeTrackedFlights().first().size)
    }

    @Test fun `finished flights are most recent first`() = runBlocking {
        val finished = saved().observeTrackedFlights().first().filter { it.isFinished }
        assertTrue(finished.size >= 2)
        assertEquals(finished.sortedByDescending { it.departure.best }, finished)
    }
}
