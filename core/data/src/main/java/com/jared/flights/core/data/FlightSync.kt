package com.jared.flights.core.data

import com.jared.flights.core.data.di.ApplicationScope
import com.jared.flights.core.data.timemachine.TIME_MACHINE_FLIGHT_ID
import com.jared.flights.core.database.dao.FlightDao
import com.jared.flights.core.database.dao.SyncStateDao
import com.jared.flights.core.database.dao.TripSplitDao
import com.jared.flights.core.database.entity.SyncStateEntity
import com.jared.flights.core.database.entity.toEntity
import com.jared.flights.core.database.entity.toModel
import com.jared.flights.core.model.AlertType
import com.jared.flights.core.model.Flight
import com.jared.flights.core.model.FlightAlert
import com.jared.flights.core.model.alertsFor
import com.jared.flights.core.model.connectionAlertsFor
import com.jared.flights.core.model.groupIntoTrips
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.io.IOException
import java.time.Clock
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Copies fresh data from the [FlightDataSource] into the phone's database
 * for as long as the app is running. Started once from the Application.
 *
 * Until the server exists (Phase 2), it also spots changes worth an alert
 * (gate change, delay, boarding…) by comparing new data with what was saved
 * (DECISIONS #42). From Phase 2 the server does this and sends push messages.
 */
@Singleton
class FlightSync @Inject constructor(
    private val dataSource: FlightDataSource,
    private val flightDao: FlightDao,
    private val syncStateDao: SyncStateDao,
    private val tripSplitDao: TripSplitDao,
    private val clock: Clock,
    @ApplicationScope private val scope: CoroutineScope,
) {
    private var job: Job? = null

    // The first update after the app starts is only a starting point: comparing it with
    // data saved in an earlier run would raise stale alerts for things that changed meanwhile.
    private var haveBaseline = false

    private val _alerts = MutableSharedFlow<List<FlightAlert>>(extraBufferCapacity = 16)

    /** Changes worth telling the user about, as they're spotted. */
    val alerts: SharedFlow<List<FlightAlert>> = _alerts.asSharedFlow()

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

    /** Saves [flights] and returns (and announces) the alerts the changes deserve. */
    internal suspend fun save(flights: List<Flight>): List<FlightAlert> {
        val before = flightDao.getAll().map { it.toModel() }
        val now = clock.instant()
        flightDao.replaceAll(flights.map { it.toEntity(updatedAt = now) })
        syncStateDao.upsert(SyncStateEntity(lastSuccessAt = now.toEpochMilli()))

        if (!haveBaseline) {
            haveBaseline = true
            return emptyList()
        }
        val alerts = findAlerts(before, flights)
        if (alerts.isNotEmpty()) _alerts.emit(alerts)
        return alerts
    }

    private suspend fun findAlerts(before: List<Flight>, after: List<Flight>): List<FlightAlert> {
        val beforeById = before.associateBy { it.id }
        // Only flights that were already tracked: a newly added flight isn't "news".
        val flightAlerts = after.flatMap { flight -> beforeById[flight.id]?.let { alertsFor(it, flight) }.orEmpty() }
        val splits = tripSplitDao.observeSplitFlightIds().first().toSet()
        val connectionAlerts = connectionAlertsFor(groupIntoTrips(before, splits), groupIntoTrips(after, splits))
        return (flightAlerts + connectionAlerts)
            // The time machine moves FM 100's timetable at every step (DECISIONS #38),
            // which isn't a real schedule change.
            .filterNot { it.flight.id == TIME_MACHINE_FLIGHT_ID && it.type == AlertType.SCHEDULE_CHANGE }
    }
}
