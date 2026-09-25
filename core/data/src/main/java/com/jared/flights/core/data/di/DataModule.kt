package com.jared.flights.core.data.di

import com.jared.flights.core.data.DefaultFlightRepository
import com.jared.flights.core.data.FlightRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
internal interface DataModule {
    @Binds
    fun bindsFlightRepository(impl: DefaultFlightRepository): FlightRepository
}
