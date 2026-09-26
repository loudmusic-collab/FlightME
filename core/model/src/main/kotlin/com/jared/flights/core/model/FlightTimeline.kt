package com.jared.flights.core.model

import java.time.Duration

/** The four moments every flight goes through, in order. */
enum class MilestoneType { GATE_OUT, TAKEOFF, LANDING, GATE_IN }

data class Milestone(
    val type: MilestoneType,
    val times: FlightTimes,
    /** The airport whose local time this milestone is shown in. */
    val airport: Airport,
) {
    /** True once it has actually happened (we have an actual time). */
    val done: Boolean
        get() = times.actual != null
}

/**
 * The flight's milestones in order: leaves gate, takes off, lands, arrives at gate.
 * Take-off and landing are left out if the data source doesn't have them.
 */
val Flight.timeline: List<Milestone>
    get() = listOfNotNull(
        Milestone(MilestoneType.GATE_OUT, departure, origin),
        takeoff?.let { Milestone(MilestoneType.TAKEOFF, it, origin) },
        landing?.let { Milestone(MilestoneType.LANDING, it, divertedTo ?: destination) },
        Milestone(MilestoneType.GATE_IN, arrival, divertedTo ?: destination),
    )

/** Time between two milestones (taxiing, or time in the air), using the best times we have. */
fun durationBetween(from: Milestone, to: Milestone): Duration =
    Duration.between(from.times.best, to.times.best)
