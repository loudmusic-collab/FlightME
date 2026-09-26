package com.jared.flights.core.model

import java.time.Duration

/** The five moments every flight goes through, in order. */
enum class MilestoneType { BOARDING, GATE_OUT, TAKEOFF, LANDING, GATE_IN }

data class Milestone(
    val type: MilestoneType,
    val times: FlightTimes,
    /** The airport whose local time this milestone is shown in. */
    val airport: Airport,
    /** True once it has happened. */
    val done: Boolean,
    /** True when the time is our own typical estimate, not from the data source (boarding only). */
    val isTypicalEstimate: Boolean = false,
)

/**
 * When boarding usually starts if the data source doesn't say:
 * this long before the plane is due to leave the gate (DECISIONS #39).
 */
val TYPICAL_BOARDING_BEFORE: Duration = Duration.ofMinutes(30)

/**
 * The flight's boarding time, or a typical estimate (30 min before departure)
 * when the data source doesn't give one.
 */
val Flight.boardingOrTypical: FlightTimes
    get() = boarding ?: FlightTimes(
        scheduled = departure.scheduled.minus(TYPICAL_BOARDING_BEFORE),
        estimated = departure.best.minus(TYPICAL_BOARDING_BEFORE),
    )

/**
 * The flight's milestones in order: boarding, leaves gate, takes off, lands,
 * arrives at gate. Take-off and landing are left out if the data source doesn't have them.
 * A milestone is done once it has an actual time; boarding also counts as done
 * once the status says boarding has begun (providers rarely send a boarding time).
 */
val Flight.timeline: List<Milestone>
    get() {
        val arrivalAirport = divertedTo ?: destination
        return listOfNotNull(
            Milestone(
                MilestoneType.BOARDING, boardingOrTypical, origin,
                done = boardingOrTypical.actual != null || status.hasStartedBoarding,
                isTypicalEstimate = boarding == null,
            ),
            milestone(MilestoneType.GATE_OUT, departure, origin),
            takeoff?.let { milestone(MilestoneType.TAKEOFF, it, origin) },
            landing?.let { milestone(MilestoneType.LANDING, it, arrivalAirport) },
            milestone(MilestoneType.GATE_IN, arrival, arrivalAirport),
        )
    }

private fun milestone(type: MilestoneType, times: FlightTimes, airport: Airport) =
    Milestone(type, times, airport, done = times.actual != null)

/** Time between two milestones (boarding, taxiing, time in the air), using the best times we have. */
fun durationBetween(from: Milestone, to: Milestone): Duration =
    Duration.between(from.times.best, to.times.best)
