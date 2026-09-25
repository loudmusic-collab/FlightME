package com.jared.flights.core.model

/** Where a flight is in its life, from booking to arrival at the gate. */
enum class FlightStatus {
    SCHEDULED,
    BOARDING,
    /** Left the gate (taxiing or just taken off). */
    DEPARTED,
    /** In the air. */
    EN_ROUTE,
    /** On the runway at the destination, not yet at the gate. */
    LANDED,
    /** At the arrival gate. */
    ARRIVED,
    CANCELLED,
    DIVERTED,
    ;

    /** True once the plane has left the departure gate. */
    val hasDeparted: Boolean
        get() = this in setOf(DEPARTED, EN_ROUTE, LANDED, ARRIVED, DIVERTED)
}
