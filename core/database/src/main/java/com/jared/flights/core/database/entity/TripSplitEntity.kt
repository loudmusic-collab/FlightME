package com.jared.flights.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/** The user said this flight is not a connection: it starts a new trip (DECISIONS #31). */
@Entity(tableName = "trip_splits")
data class TripSplitEntity(
    @PrimaryKey val flightId: String,
)
