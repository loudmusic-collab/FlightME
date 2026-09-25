package com.jared.flights.di

import com.jared.flights.BuildConfig
import com.jared.flights.core.data.FlightDataSource
import com.jared.flights.core.data.mock.MockFlightDataSource
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import java.time.Clock
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    /** The current time. Injected so tests and the time machine (1.5) can control it. */
    @Provides
    @Singleton
    fun providesClock(): Clock = Clock.systemDefaultZone()

    /** Chooses where flight data comes from, using the USE_MOCK_DATA build flag. */
    @Provides
    @Singleton
    fun providesFlightDataSource(mock: MockFlightDataSource): FlightDataSource {
        check(BuildConfig.USE_MOCK_DATA) { "The Firebase data source arrives in Phase 2.3" }
        return mock
    }
}
