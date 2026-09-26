package com.jared.flights.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * The user's own booking reference (PNR) for a flight, e.g. "X7K2QP".
 * Kept on the phone only, for the user to see and copy; never sent anywhere
 * or used to look anything up (DECISIONS #40).
 */
@Entity(tableName = "bookings")
data class BookingEntity(
    @PrimaryKey val flightId: String,
    val reference: String,
)
