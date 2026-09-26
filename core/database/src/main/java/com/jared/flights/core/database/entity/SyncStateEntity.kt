package com.jared.flights.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/** A single row: when flights were last successfully refreshed from the data source. */
@Entity(tableName = "sync_state")
data class SyncStateEntity(
    @PrimaryKey val id: Int = SINGLE_ROW,
    val lastSuccessAt: Long,
) {
    companion object {
        const val SINGLE_ROW = 0
    }
}
