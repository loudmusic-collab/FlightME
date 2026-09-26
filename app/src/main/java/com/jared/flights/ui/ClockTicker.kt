package com.jared.flights.ui

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.time.Clock
import java.time.Instant

/** Emits the current time now, then every [periodMillis]. Keeps planes and "done" steps moving. */
fun clockTicker(clock: Clock, periodMillis: Long = 30_000L): Flow<Instant> = flow {
    while (true) {
        emit(clock.instant())
        delay(periodMillis)
    }
}
