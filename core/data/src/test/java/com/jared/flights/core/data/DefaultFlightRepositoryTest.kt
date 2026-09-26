package com.jared.flights.core.data

import com.jared.flights.core.data.mock.buildMockFlights
import com.jared.flights.core.model.Flight
import com.jared.flights.core.model.FlightNumber
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
import java.time.LocalDate
import java.time.ZoneId

class DefaultFlightRepositoryTest {

    private val clock = Clock.fixed(Instant.parse("2026-09-25T13:07:00Z"), ZoneId.of("Europe/London"))
    private val mockFlights = buildMockFlights(clock)

    private val flightDao = FakeFlightDao()
    private val splitDao = FakeTripSplitDao()
    private val syncDao = FakeSyncStateDao()
    private val bookingDao = FakeBookingDao()

    private fun repository(
        online: Boolean = true,
        sourceFlights: List<Flight> = mockFlights,
        source: FakeFlightDataSource = FakeFlightDataSource(sourceFlights, online),
    ): DefaultFlightRepository {
        val sync = FlightSync(source, flightDao, syncDao, clock, CoroutineScope(Dispatchers.Unconfined))
        return DefaultFlightRepository(flightDao, splitDao, syncDao, bookingDao, source, sync)
    }

    private val sync = FlightSync(
        FakeFlightDataSource(mockFlights), flightDao, syncDao, clock, CoroutineScope(Dispatchers.Unconfined),
    )

    /** Put the mock flights into the (fake) database, like a sync would. */
    private fun saved(online: Boolean = true): FlightRepository {
        runBlocking { sync.save(mockFlights) }
        return repository(online)
    }

    // ---- Search, add, remove, booking references (Phase 1.7) ----

    private val fm123 = mockFlights.first().copy(id = "FM123_2026-09-26", airlineIata = "FM", flightNumber = "123")

    @Test fun `search finds a flight by number`() = runBlocking {
        val repo = repository(source = FakeFlightDataSource(searchable = listOf(fm123)))
        assertEquals(listOf("FM123_2026-09-26"), repo.search(FlightNumber("FM", "123"), LocalDate.of(2026, 9, 26)).map { it.id })
    }

    @Test fun `adding a flight tracks it and saves the booking reference tidied up`() = runBlocking {
        val source = FakeFlightDataSource(searchable = listOf(fm123))
        val repo = repository(source = source)
        assertTrue(repo.track(listOf(fm123.id), bookingReference = " x7k2qp "))
        assertEquals(listOf("FM123_2026-09-26"), source.tracked.value.map { it.id })
        assertEquals("X7K2QP", repo.observeBookingReference(fm123.id).first())
    }

    @Test fun `adding without a booking reference saves none`() = runBlocking {
        val repo = repository(source = FakeFlightDataSource(searchable = listOf(fm123)))
        repo.track(listOf(fm123.id), bookingReference = "  ")
        assertEquals(null, repo.observeBookingReference(fm123.id).first())
    }

    @Test fun `adding while offline fails`() = runBlocking {
        val repo = repository(source = FakeFlightDataSource(online = false, searchable = listOf(fm123)))
        assertFalse(repo.track(listOf(fm123.id)))
    }

    @Test fun `removing flights stops tracking them, and adding back undoes it`() = runBlocking {
        val source = FakeFlightDataSource(flights = mockFlights)
        val repo = repository(source = source)
        val trip = listOf("EK2-mock", "EK432-mock")
        assertTrue(repo.untrack(trip))
        assertTrue(source.tracked.value.none { it.id in trip })
        assertTrue(repo.track(trip))
        assertEquals(mockFlights.size, source.tracked.value.size)
    }

    @Test fun `a booking reference can be changed and cleared`() = runBlocking {
        val repo = repository()
        repo.setBookingReference("VS3-mock", "abc123")
        assertEquals("ABC123", repo.observeBookingReference("VS3-mock").first())
        repo.setBookingReference("VS3-mock", null)
        assertEquals(null, repo.observeBookingReference("VS3-mock").first())
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
