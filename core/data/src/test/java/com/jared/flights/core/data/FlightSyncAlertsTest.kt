package com.jared.flights.core.data

import com.jared.flights.core.data.mock.buildMockFlights
import com.jared.flights.core.model.AlertType
import com.jared.flights.core.model.FlightTimes
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneId

class FlightSyncAlertsTest {

    private val clock = Clock.fixed(Instant.parse("2026-09-25T13:07:00Z"), ZoneId.of("Europe/London"))
    private val flights = buildMockFlights(clock)
    private val sync = FlightSync(
        FakeFlightDataSource(flights), FakeFlightDao(), FakeSyncStateDao(), FakeTripSplitDao(), clock,
        CoroutineScope(Dispatchers.Unconfined),
    )

    private fun changed(id: String, change: (com.jared.flights.core.model.Flight) -> com.jared.flights.core.model.Flight) =
        flights.map { if (it.id == id) change(it) else it }

    @Test fun `the first update after starting raises no alerts`() = runBlocking {
        assertEquals(emptyList<AlertType>(), sync.save(flights).map { it.type })
    }

    @Test fun `a gate change on a later update raises an alert`() = runBlocking {
        sync.save(flights)
        val alerts = sync.save(changed("BA283-mock") { it.copy(departureGate = "B40") })
        assertEquals(listOf(AlertType.GATE_CHANGE), alerts.map { it.type })
        assertEquals("BA283", alerts.single().flight.ident)
    }

    @Test fun `time machine re-timing is not a schedule change`() = runBlocking {
        val tm = flights.first().copy(id = com.jared.flights.core.data.timemachine.TIME_MACHINE_FLIGHT_ID)
        sync.save(flights + tm)
        val moved = tm.copy(departure = FlightTimes(tm.departure.scheduled.plus(Duration.ofMinutes(30))))
        assertEquals(emptyList<AlertType>(), sync.save(flights + moved).map { it.type }.filter { it == AlertType.SCHEDULE_CHANGE })
    }

    @Test fun `the same data again raises nothing`() = runBlocking {
        sync.save(flights)
        assertEquals(emptyList<AlertType>(), sync.save(flights).map { it.type })
    }

    @Test fun `delaying a first leg can put a connection at risk`() = runBlocking {
        sync.save(flights)
        // A delay moves both leaving and landing later by the same amount.
        fun ek2Late(minutes: Long) = changed("EK2-mock") { f ->
            fun FlightTimes.late() = FlightTimes(scheduled, estimated = scheduled.plus(Duration.ofMinutes(minutes)))
            f.copy(departure = f.departure.late(), arrival = f.arrival.late())
        }
        // 60 min late: the 2h 10m Dubai connection drops to 1h 10m (still comfortable).
        assertEquals(listOf(AlertType.DELAYED), sync.save(ek2Late(60)).map { it.type })
        // 100 min late: only 30 min left to change planes.
        assertEquals(
            listOf(AlertType.DELAYED, AlertType.CONNECTION_AT_RISK),
            sync.save(ek2Late(100)).map { it.type },
        )
    }
}
