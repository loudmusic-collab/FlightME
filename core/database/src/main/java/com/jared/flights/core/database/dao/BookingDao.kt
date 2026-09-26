package com.jared.flights.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.jared.flights.core.database.entity.BookingEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BookingDao {

    @Query("SELECT reference FROM bookings WHERE flightId = :flightId")
    fun observeReference(flightId: String): Flow<String?>

    @Upsert
    suspend fun upsert(booking: BookingEntity)

    @Query("DELETE FROM bookings WHERE flightId = :flightId")
    suspend fun delete(flightId: String)
}
