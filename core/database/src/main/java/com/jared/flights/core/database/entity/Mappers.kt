package com.jared.flights.core.database.entity

import com.jared.flights.core.model.Airport
import com.jared.flights.core.model.Flight
import com.jared.flights.core.model.FlightStatus
import com.jared.flights.core.model.FlightTimes
import com.jared.flights.core.model.LatLon
import java.time.Instant
import java.time.ZoneId

// Converting between the app's model and database rows.

fun Flight.toEntity(updatedAt: Instant): FlightEntity = FlightEntity(
    id = id,
    airlineIata = airlineIata,
    airlineName = airlineName,
    flightNumber = flightNumber,
    origin = origin.toColumns(),
    destination = destination.toColumns(),
    divertedTo = divertedTo?.toColumns(),
    boarding = boarding?.toColumns(),
    departure = departure.toColumns(),
    takeoff = takeoff?.toColumns(),
    landing = landing?.toColumns(),
    arrival = arrival.toColumns(),
    status = status.name,
    departureTerminal = departureTerminal,
    departureGate = departureGate,
    arrivalTerminal = arrivalTerminal,
    arrivalGate = arrivalGate,
    baggageClaim = baggageClaim,
    aircraftType = aircraftType,
    updatedAt = updatedAt.toEpochMilli(),
)

fun FlightEntity.toModel(): Flight = Flight(
    id = id,
    airlineIata = airlineIata,
    airlineName = airlineName,
    flightNumber = flightNumber,
    origin = origin.toModel(),
    destination = destination.toModel(),
    divertedTo = divertedTo?.toModel(),
    boarding = boarding?.toModel(),
    departure = departure.toModel(),
    takeoff = takeoff?.toModel(),
    landing = landing?.toModel(),
    arrival = arrival.toModel(),
    // An unknown status (e.g. from a newer app version) falls back to SCHEDULED.
    status = FlightStatus.entries.find { it.name == status } ?: FlightStatus.SCHEDULED,
    departureTerminal = departureTerminal,
    departureGate = departureGate,
    arrivalTerminal = arrivalTerminal,
    arrivalGate = arrivalGate,
    baggageClaim = baggageClaim,
    aircraftType = aircraftType,
)

private fun Airport.toColumns() =
    AirportColumns(iata, icao, name, city, timeZone.id, location?.latitude, location?.longitude)
private fun AirportColumns.toModel() = Airport(
    iata, icao, name, city, ZoneId.of(timeZone),
    location = if (latitude != null && longitude != null) LatLon(latitude, longitude) else null,
)

private fun FlightTimes.toColumns() = TimesColumns(
    scheduled = scheduled.toEpochMilli(),
    estimated = estimated?.toEpochMilli(),
    actual = actual?.toEpochMilli(),
)

private fun TimesColumns.toModel() = FlightTimes(
    scheduled = Instant.ofEpochMilli(scheduled),
    estimated = estimated?.let(Instant::ofEpochMilli),
    actual = actual?.let(Instant::ofEpochMilli),
)
