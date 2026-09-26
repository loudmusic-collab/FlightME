package com.jared.flights.core.model

import java.time.Duration

/**
 * One journey made of one or more flights (legs), e.g. LHR → DXB → SIN.
 * A non-stop flight is a trip with one leg.
 */
data class Trip(val legs: List<Flight>) {
    init {
        require(legs.isNotEmpty()) { "A trip needs at least one flight" }
    }

    /** Stable id: the first leg's id. */
    val id: String get() = legs.first().id

    val origin: Airport get() = legs.first().origin
    val destination: Airport get() = legs.last().destination

    val isConnecting: Boolean get() = legs.size > 1

    /** The airports you change planes at, e.g. [Dubai]. */
    val stops: List<Airport> get() = legs.dropLast(1).map { it.destination }

    /** The layovers between legs, in order. */
    val connections: List<Connection> get() = legs.zipWithNext(::Connection)

    val isFinished: Boolean get() = legs.all { it.isFinished }

    fun connectionBefore(flightId: String): Connection? = connections.find { it.outbound.id == flightId }
    fun connectionAfter(flightId: String): Connection? = connections.find { it.inbound.id == flightId }
}

enum class ConnectionHealth { COMFORTABLE, TIGHT, AT_RISK, MISSED }

/** Changing from [inbound] to [outbound] at [airport]. */
data class Connection(val inbound: Flight, val outbound: Flight) {

    val airport: Airport get() = inbound.destination

    /** Time between landing at the gate and the next flight leaving the gate, using the latest times. */
    val layover: Duration get() = Duration.between(inbound.arrival.best, outbound.departure.best)

    /** The layover the timetable planned. */
    val plannedLayover: Duration get() = Duration.between(inbound.arrival.scheduled, outbound.departure.scheduled)

    val health: ConnectionHealth
        get() = when {
            inbound.status == FlightStatus.CANCELLED ||
                inbound.status == FlightStatus.DIVERTED ||
                outbound.status == FlightStatus.CANCELLED -> ConnectionHealth.AT_RISK
            else -> ConnectionRules.healthOf(layover)
        }
}

/**
 * Connection rules (DECISIONS #31, #32).
 */
object ConnectionRules {
    val COMFORTABLE_FROM: Duration = Duration.ofMinutes(60)
    val AT_RISK_AT_OR_BELOW: Duration = Duration.ofMinutes(45)

    /** Legs further apart than this are separate trips. */
    val MAX_GAP: Duration = Duration.ofHours(24)

    fun healthOf(layover: Duration): ConnectionHealth = when {
        layover <= Duration.ZERO -> ConnectionHealth.MISSED
        layover <= AT_RISK_AT_OR_BELOW -> ConnectionHealth.AT_RISK
        layover < COMFORTABLE_FROM -> ConnectionHealth.TIGHT
        else -> ConnectionHealth.COMFORTABLE
    }

    /**
     * Can [next] follow [trip]? It must leave from where the trip's last leg lands,
     * within [MAX_GAP] of landing (timetable times, so a delay doesn't break the trip up),
     * and must not fly back to where the trip started (that's a return, not a connection).
     */
    fun canContinue(trip: List<Flight>, next: Flight): Boolean {
        val last = trip.last()
        val gap = Duration.between(last.arrival.scheduled, next.departure.scheduled)
        return next.origin.iata == last.destination.iata &&
            gap > Duration.ZERO && gap <= MAX_GAP &&
            next.destination.iata != trip.first().origin.iata
    }
}

/**
 * Groups flights into trips. [splitBefore] holds ids of flights the user said
 * start a new trip (overriding the automatic rule).
 * Returns trips in timetable order of their first flight.
 */
fun groupIntoTrips(flights: List<Flight>, splitBefore: Set<String> = emptySet()): List<Trip> {
    val trips = mutableListOf<MutableList<Flight>>()
    for (flight in flights.sortedBy { it.departure.scheduled }) {
        val joinable = if (flight.id in splitBefore) {
            null
        } else {
            trips
                .filter { ConnectionRules.canContinue(it, flight) }
                // If more than one could fit, join the one that landed most recently.
                .minByOrNull { Duration.between(it.last().arrival.scheduled, flight.departure.scheduled) }
        }
        if (joinable != null) joinable += flight else trips += mutableListOf(flight)
    }
    return trips.map { Trip(it) }
}
