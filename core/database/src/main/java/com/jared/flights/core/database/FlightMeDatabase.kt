package com.jared.flights.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.jared.flights.core.database.dao.BookingDao
import com.jared.flights.core.database.dao.FlightDao
import com.jared.flights.core.database.dao.SyncStateDao
import com.jared.flights.core.database.dao.TripSplitDao
import com.jared.flights.core.database.entity.BookingEntity
import com.jared.flights.core.database.entity.FlightEntity
import com.jared.flights.core.database.entity.SyncStateEntity
import com.jared.flights.core.database.entity.TripSplitEntity

/**
 * The on-phone database. What the screens read (DECISIONS #7).
 * Bump [version] whenever a table changes; see DECISIONS #34 for how upgrades are handled.
 */
@Database(
    entities = [FlightEntity::class, TripSplitEntity::class, SyncStateEntity::class, BookingEntity::class],
    version = 3,
    exportSchema = true,
)
abstract class FlightMeDatabase : RoomDatabase() {
    abstract fun flightDao(): FlightDao
    abstract fun tripSplitDao(): TripSplitDao
    abstract fun syncStateDao(): SyncStateDao
    abstract fun bookingDao(): BookingDao
}
