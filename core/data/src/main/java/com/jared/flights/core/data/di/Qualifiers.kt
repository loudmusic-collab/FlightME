package com.jared.flights.core.data.di

import javax.inject.Qualifier

/** A coroutine scope that lives as long as the app, for background work like syncing. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ApplicationScope
