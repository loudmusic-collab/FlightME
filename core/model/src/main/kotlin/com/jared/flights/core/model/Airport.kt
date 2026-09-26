package com.jared.flights.core.model

import java.time.ZoneId

data class Airport(
    /** Three-letter code passengers know, e.g. "LHR". */
    val iata: String,
    /** Four-letter code used by air traffic control, e.g. "EGLL". */
    val icao: String,
    val name: String,
    val city: String,
    /** Local time zone. Flight times are shown in the airport's local time. */
    val timeZone: ZoneId,
    /** Where it is on the map. Null if unknown (the map is then not shown). */
    val location: LatLon? = null,
)
