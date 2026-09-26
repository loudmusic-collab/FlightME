package com.jared.flights.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.jared.flights.core.database.entity.TripSplitEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TripSplitDao {

    @Query("SELECT flightId FROM trip_splits")
    fun observeSplitFlightIds(): Flow<List<String>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(split: TripSplitEntity)
}
