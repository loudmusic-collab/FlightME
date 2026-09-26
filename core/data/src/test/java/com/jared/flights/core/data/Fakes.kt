package com.jared.flights.core.data

import com.jared.flights.core.database.dao.BookingDao
import com.jared.flights.core.database.dao.FlightDao
import com.jared.flights.core.database.dao.SyncStateDao
import com.jared.flights.core.database.dao.TripSplitDao
import com.jared.flights.core.database.entity.BookingEntity
import com.jared.flights.core.database.entity.FlightEntity
import com.jared.flights.core.database.entity.SyncStateEntity
import com.jared.flights.core.database.entity.TripSplitEntity
import com.jared.flights.core.model.Flight
import com.jared.flights.core.model.FlightNumber
import com.jared.flights.core.model.LivePosition
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import java.io.IOException
import java.time.LocalDate

// In-memory stand-ins for the database and data source, so tests run on the computer.

class FakeFlightDao : FlightDao() {
    val rows = MutableStateFlow<Map<String, FlightEntity>>(emptyMap())

    override fun observeAll(): Flow<List<FlightEntity>> = rows.map { it.values.toList() }
    override fun observeById(id: String): Flow<FlightEntity?> = rows.map { it[id] }
    override suspend fun upsertAll(flights: List<FlightEntity>) = rows.update { it + flights.associateBy { f -> f.id } }
    override suspend fun deleteAllExcept(keepIds: List<String>) = rows.update { it.filterKeys { id -> id in keepIds } }
    override suspend fun deleteAll() = rows.update { emptyMap() }
}

class FakeTripSplitDao : TripSplitDao {
    private val ids = MutableStateFlow<Set<String>>(emptySet())
    override fun observeSplitFlightIds(): Flow<List<String>> = ids.map { it.toList() }
    override suspend fun insert(split: TripSplitEntity) = ids.update { it + split.flightId }
}

class FakeSyncStateDao : SyncStateDao {
    val last = MutableStateFlow<Long?>(null)
    override fun observeLastSuccessAt(): Flow<Long?> = last
    override suspend fun upsert(state: SyncStateEntity) = last.update { state.lastSuccessAt }
}

class FakeBookingDao : BookingDao {
    val rows = MutableStateFlow<Map<String, String>>(emptyMap())
    override fun observeReference(flightId: String): Flow<String?> = rows.map { it[flightId] }
    override suspend fun upsert(booking: BookingEntity) = rows.update { it + (booking.flightId to booking.reference) }
    override suspend fun delete(flightId: String) = rows.update { it - flightId }
}

/** A pretend server: [flights] are tracked; [searchable] can be found and added. */
class FakeFlightDataSource(
    private val flights: List<Flight> = emptyList(),
    private val online: Boolean = true,
    private val searchable: List<Flight> = emptyList(),
) : FlightDataSource {
    val tracked = MutableStateFlow(flights)

    override fun observeTrackedFlights(): Flow<List<Flight>> = if (online) tracked else emptyFlow()
    override fun observeOnline(): Flow<Boolean> = flowOf(online)
    override suspend fun fetchTrackedFlights(): List<Flight> = checkOnline { tracked.value }

    override suspend fun searchFlights(number: FlightNumber, date: LocalDate): List<Flight> = checkOnline {
        (tracked.value + searchable).filter { it.ident == number.ident }.distinctBy { it.id }
    }

    private val known = flights + searchable

    override suspend fun trackFlight(flightId: String) = checkOnline {
        tracked.update { list -> list + listOfNotNull(known.find { it.id == flightId }) }
    }
    override suspend fun untrackFlight(flightId: String) = checkOnline { tracked.update { l -> l.filterNot { it.id == flightId } } }

    override suspend fun getLivePosition(flightId: String): LivePosition? = checkOnline { null }

    private fun <T> checkOnline(block: () -> T): T = if (online) block() else throw IOException("offline")
}
