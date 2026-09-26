package com.jared.flights.core.database.entity

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey

/** One saved flight. Mirrors [com.jared.flights.core.model.Flight] as database columns. */
@Entity(tableName = "flights")
data class FlightEntity(
    @PrimaryKey val id: String,
    val airlineIata: String,
    val airlineName: String,
    val flightNumber: String,
    @Embedded(prefix = "origin_") val origin: AirportColumns,
    @Embedded(prefix = "dest_") val destination: AirportColumns,
    @Embedded(prefix = "diverted_") val divertedTo: AirportColumns?,
    @Embedded(prefix = "boarding_") val boarding: TimesColumns?,
    @Embedded(prefix = "gate_out_") val departure: TimesColumns,
    @Embedded(prefix = "takeoff_") val takeoff: TimesColumns?,
    @Embedded(prefix = "landing_") val landing: TimesColumns?,
    @Embedded(prefix = "gate_in_") val arrival: TimesColumns,
    /** [com.jared.flights.core.model.FlightStatus] name, e.g. "EN_ROUTE". */
    val status: String,
    val departureTerminal: String?,
    val departureGate: String?,
    val arrivalTerminal: String?,
    val arrivalGate: String?,
    val baggageClaim: String?,
    val aircraftType: String?,
    /** When this row was last refreshed from the data source (epoch millis). */
    val updatedAt: Long,
)

data class AirportColumns(
    val iata: String,
    val icao: String,
    val name: String,
    val city: String,
    /** Time zone id, e.g. "Europe/London". */
    val timeZone: String,
)

/** Times as epoch milliseconds (UTC). */
data class TimesColumns(
    val scheduled: Long,
    val estimated: Long?,
    val actual: Long?,
)
