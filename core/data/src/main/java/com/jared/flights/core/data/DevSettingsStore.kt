package com.jared.flights.core.data

import javax.inject.Qualifier

/** Marks the DataStore file that holds [DevSettings]. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class DevSettingsStore
