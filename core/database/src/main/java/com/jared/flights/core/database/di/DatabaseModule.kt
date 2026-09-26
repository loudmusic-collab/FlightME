package com.jared.flights.core.database.di

import android.content.Context
import androidx.room.Room
import com.jared.flights.core.database.FlightMeDatabase
import com.jared.flights.core.database.dao.FlightDao
import com.jared.flights.core.database.dao.SyncStateDao
import com.jared.flights.core.database.dao.TripSplitDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object DatabaseModule {

    @Provides
    @Singleton
    fun providesDatabase(@ApplicationContext context: Context): FlightMeDatabase =
        Room.databaseBuilder(context, FlightMeDatabase::class.java, "flightme.db")
            // Before launch only: if the tables change, start with an empty database
            // instead of writing an upgrade step. Flights re-download on the next sync.
            // Must be replaced with real migrations before the first public release (DECISIONS #34).
            .fallbackToDestructiveMigration(dropAllTables = true)
            .build()

    @Provides fun providesFlightDao(db: FlightMeDatabase): FlightDao = db.flightDao()
    @Provides fun providesTripSplitDao(db: FlightMeDatabase): TripSplitDao = db.tripSplitDao()
    @Provides fun providesSyncStateDao(db: FlightMeDatabase): SyncStateDao = db.syncStateDao()
}
