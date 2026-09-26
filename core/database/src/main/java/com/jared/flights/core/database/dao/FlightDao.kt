package com.jared.flights.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.jared.flights.core.database.entity.FlightEntity
import kotlinx.coroutines.flow.Flow

@Dao
abstract class FlightDao {

    @Query("SELECT * FROM flights")
    abstract fun observeAll(): Flow<List<FlightEntity>>

    @Query("SELECT * FROM flights WHERE id = :id")
    abstract fun observeById(id: String): Flow<FlightEntity?>

    @Upsert
    protected abstract suspend fun upsertAll(flights: List<FlightEntity>)

    @Query("DELETE FROM flights WHERE id NOT IN (:keepIds)")
    protected abstract suspend fun deleteAllExcept(keepIds: List<String>)

    @Query("DELETE FROM flights")
    protected abstract suspend fun deleteAll()

    /**
     * Make the saved flights exactly match [flights]: add new ones, update
     * changed ones and remove any that are no longer tracked. All in one go,
     * so the screens never see a half-updated list.
     */
    @Transaction
    open suspend fun replaceAll(flights: List<FlightEntity>) {
        if (flights.isEmpty()) {
            deleteAll()
        } else {
            upsertAll(flights)
            deleteAllExcept(flights.map { it.id })
        }
    }
}
