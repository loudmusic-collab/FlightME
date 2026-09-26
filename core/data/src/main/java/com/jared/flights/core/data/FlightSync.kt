package com.jared.flights.core.data

import com.jared.flights.core.data.di.ApplicationScope
import com.jared.flights.core.database.dao.FlightDao
import com.jared.flights.core.database.dao.SyncStateDao
import com.jared.flights.core.database.entity.SyncStateEntity
import com.jared.flights.core.database.entity.toEntity
import com.jared.flights.core.model.Flight
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.io.IOException
import java.time.Clock
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Copies fresh data from the [FlightDataSource] into the phone's database
 * for as long as the app is running. Started once from the Application.
 */
@Singleton
class FlightSync @Inject constructor(
    private val dataSource: FlightDataSource,
    private val flightDao: FlightDao,
    private val syncStateDao: SyncStateDao,
    private val clock: Clock,
    @ApplicationScope private val scope: CoroutineScope,
) {
    private var job: Job? = null

    fun start() {
        if (job != null) return
        job = scope.launch {
            dataSource.observeTrackedFlights().collect { flights -> save(flights) }
        }
    }

    /**
     * Fetch now and save (pull-to-refresh). Returns false if it couldn't
     * connect; the saved flights are left as they were.
     */
    suspend fun refresh(): Boolean = try {
        save(dataSource.fetchTrackedFlights())
        true
    } catch (e: IOException) {
        false
    }

    internal suspend fun save(flights: List<Flight>) {
        val now = clock.instant()
        flightDao.replaceAll(flights.map { it.toEntity(updatedAt = now) })
        syncStateDao.upsert(SyncStateEntity(lastSuccessAt = now.toEpochMilli()))
    }
}
