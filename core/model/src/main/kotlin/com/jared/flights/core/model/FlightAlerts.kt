package com.jared.flights.core.model

import java.time.Duration

/**
 * Every kind of alert, in one list (PLAN §5 "alert catalogue as data").
 * [onByDefault]: whether it's switched on before the user changes anything (DECISIONS #42).
 */
enum class AlertType(val onByDefault: Boolean) {
    DELAYED(true),
    BACK_ON_TIME(true),
    SCHEDULE_CHANGE(true),
    GATE_CHANGE(true),
    TERMINAL_CHANGE(true),
    BOARDING(true),
    DEPARTED(false), // "arrived at gate" covers the important part; can feel chatty
    LANDED(false),
    ARRIVED(true),
    BAGGAGE(true),
    CANCELLED(true),
    DIVERTED(true),
    CONNECTION_AT_RISK(true),
}

/** Something worth telling the user about one flight. The app turns it into words. */
data class FlightAlert(
    val type: AlertType,
    val flight: Flight,
    /** The flight as it was before, for "A10 → A14" style messages. */
    val before: Flight,
    /** For [AlertType.CONNECTION_AT_RISK]: the connection that got worse. */
    val connection: Connection? = null,
)

private val SMALL_CHANGE: Duration = Duration.ofMinutes(15)
private val RETIMED: Duration = Duration.ofMinutes(5)

/**
 * The rules engine: compares a flight before and after an update and returns the
 * alerts that change deserves. Small wobbles (under 15 min of extra delay) are ignored.
 */
fun alertsFor(before: Flight, after: Flight): List<FlightAlert> {
    val alerts = mutableListOf<AlertType>()
    val b = before.status
    val a = after.status

    if (a == FlightStatus.CANCELLED && b != FlightStatus.CANCELLED) {
        alerts += AlertType.CANCELLED
        return alerts.map { FlightAlert(it, after, before) } // nothing else matters now
    }
    if (a == FlightStatus.DIVERTED && b != FlightStatus.DIVERTED) alerts += AlertType.DIVERTED

    // Delays: compare the delay that matters now (departure until take-off, then arrival).
    val oldDelay = before.relevantDelay
    val newDelay = after.relevantDelay
    val nowLate = after.delaySeverity != DelaySeverity.ON_TIME
    val wasLate = before.delaySeverity != DelaySeverity.ON_TIME
    when {
        nowLate && (!wasLate || newDelay.minus(oldDelay) >= SMALL_CHANGE) -> alerts += AlertType.DELAYED
        wasLate && !nowLate -> alerts += AlertType.BACK_ON_TIME
    }

    // The airline moved the timetable itself.
    if (Duration.between(before.departure.scheduled, after.departure.scheduled).abs() >= RETIMED) {
        alerts += AlertType.SCHEDULE_CHANGE
    }

    if (after.departureGate != null && after.departureGate != before.departureGate) alerts += AlertType.GATE_CHANGE
    if (before.departureTerminal != null && after.departureTerminal != null &&
        after.departureTerminal != before.departureTerminal
    ) {
        alerts += AlertType.TERMINAL_CHANGE
    }

    // Journey milestones (each only once, as the status moves forward).
    if (a == FlightStatus.BOARDING && !b.hasStartedBoarding) alerts += AlertType.BOARDING
    if ((a == FlightStatus.DEPARTED || a == FlightStatus.EN_ROUTE) && !b.hasDeparted) alerts += AlertType.DEPARTED
    val landedStates = setOf(FlightStatus.LANDED, FlightStatus.ARRIVED)
    if (a in landedStates && b !in landedStates) alerts += AlertType.LANDED
    if (a == FlightStatus.ARRIVED && b != FlightStatus.ARRIVED) alerts += AlertType.ARRIVED
    if (after.baggageClaim != null && after.baggageClaim != before.baggageClaim) alerts += AlertType.BAGGAGE

    return alerts.map { FlightAlert(it, after, before) }
}

/**
 * Connection alerts: a connection that got worse (e.g. comfortable → at risk)
 * because of a delay. Compares trips before and after an update.
 */
fun connectionAlertsFor(before: List<Trip>, after: List<Trip>): List<FlightAlert> {
    val oldHealth = before.flatMap { it.connections }.associate { (it.inbound.id to it.outbound.id) to it }
    return after.flatMap { it.connections }.mapNotNull { now ->
        val then = oldHealth[now.inbound.id to now.outbound.id] ?: return@mapNotNull null
        val worse = now.health.ordinal > then.health.ordinal
        val worrying = now.health == ConnectionHealth.AT_RISK || now.health == ConnectionHealth.MISSED ||
            now.health == ConnectionHealth.TIGHT
        if (worse && worrying) FlightAlert(AlertType.CONNECTION_AT_RISK, now.inbound, then.inbound, now) else null
    }
}
