package com.jared.flights.core.data

import com.jared.flights.core.data.mock.buildMockFlights
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneId

class LivePositionTrackerTest {

    private val clock = Clock.fixed(Instant.parse("2026-09-25T13:07:00Z"), ZoneId.of("Europe/London"))
    private val flights = buildMockFlights(clock)

    @Test fun `offline still gives an estimated position for a flight in the air`() = runBlocking {
        val flightDao = FakeFlightDao()
        val syncDao = FakeSyncStateDao()
        val source = FakeFlightDataSource(flights, online = false)
        val sync = FlightSync(source, flightDao, syncDao, clock, CoroutineScope(Dispatchers.Unconfined))
        sync.save(flights) // saved earlier, while online
        val repo = DefaultFlightRepository(flightDao, FakeTripSplitDao(), syncDao, FakeBookingDao(), source, sync)

        // LH 946 is in the air in the mock data.
        val position = LivePositionTracker(repo, clock).observe("LH946-mock").first()
        assertNotNull(position)
        assertTrue("altitude ${position!!.altitudeFeet}", position.altitudeFeet > 0)
    }
}
