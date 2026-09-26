package com.jared.flights.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.jared.flights.core.database.entity.SyncStateEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SyncStateDao {

    /** Epoch millis of the last successful refresh, or null if there never was one. */
    @Query("SELECT lastSuccessAt FROM sync_state WHERE id = 0")
    fun observeLastSuccessAt(): Flow<Long?>

    @Upsert
    suspend fun upsert(state: SyncStateEntity)
}
