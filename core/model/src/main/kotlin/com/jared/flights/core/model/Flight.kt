package com.jared.flights.core.model

import java.time.Duration

/** One flight on one day, e.g. BA 283 from LHR to LAX on 25 September. */
data class Flight(
    val id: String,
    /** Two-character airline code, e.g. "BA". */
    val airlineIata: String,
    val airlineName: String,
    /** The number part only, e.g. "283". */
    val flightNumber: String,
    val origin: Airport,
    val destination: Airport,
    /** Leaving the departure gate. */
    val departure: FlightTimes,
    /** Arriving at the destination gate. */
    val arrival: FlightTimes,
    val status: FlightStatus,
    /** Boarding starts. Null if the data source doesn't give it (see [boardingOrTypical]). */
    val boarding: FlightTimes? = null,
    /** Wheels off the runway. Null if the data source doesn't give it. */
    val takeoff: FlightTimes? = null,
    /** Wheels on the runway at the destination. Null if the data source doesn't give it. */
    val landing: FlightTimes? = null,
    val departureTerminal: String? = null,
    val departureGate: String? = null,
    val arrivalTerminal: String? = null,
    val arrivalGate: String? = null,
    val baggageClaim: String? = null,
    val aircraftType: String? = null,
    /** Where the flight actually went, if it was diverted. */
    val divertedTo: Airport? = null,
) {
    /** Flight number as passengers write it, e.g. "BA283". */
    val ident: String
        get() = airlineIata + flightNumber

    /**
     * The delay that matters right now: the departure delay until the plane
     * leaves the gate, then the arrival delay.
     */
    val relevantDelay: Duration
        get() = if (status.hasDeparted) arrival.delay else departure.delay

    val delaySeverity: DelaySeverity
        get() = DelayRules.severityOf(relevantDelay)

    /** Finished flights (arrived or cancelled) go to the bottom of the list. */
    val isFinished: Boolean
        get() = status == FlightStatus.ARRIVED || status == FlightStatus.CANCELLED
}
