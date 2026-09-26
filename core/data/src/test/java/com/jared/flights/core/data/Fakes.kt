package com.jared.flights.core.data

import com.jared.flights.core.database.dao.FlightDao
import com.jared.flights.core.database.dao.SyncStateDao
import com.jared.flights.core.database.dao.TripSplitDao
import com.jared.flights.core.database.entity.FlightEntity
import com.jared.flights.core.database.entity.SyncStateEntity
import com.jared.flights.core.database.entity.TripSplitEntity
import com.jared.flights.core.model.Flight
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import java.io.IOException

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

class FakeFlightDataSource(
    private val flights: List<Flight> = emptyList(),
    private val online: Boolean = true,
) : FlightDataSource {
    override fun observeTrackedFlights(): Flow<List<Flight>> = if (online) flowOf(flights) else emptyFlow()
    override fun observeOnline(): Flow<Boolean> = flowOf(online)
    override suspend fun fetchTrackedFlights(): List<Flight> =
        if (online) flights else throw IOException("offline")
}
