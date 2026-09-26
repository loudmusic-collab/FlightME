package com.jared.flights.core.data

import java.time.Instant

/** Whether we're connected, and when flights were last refreshed (null = never). */
data class SyncStatus(
    val isOnline: Boolean,
    val lastSuccessAt: Instant?,
)
