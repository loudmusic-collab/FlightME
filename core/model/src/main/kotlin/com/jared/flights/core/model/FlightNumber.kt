package com.jared.flights.core.model

/**
 * A flight number as typed by a person, tidied up: "ba 0283" → BA 283.
 * [airline] is the 2-character IATA code, [number] has no leading zeros.
 */
data class FlightNumber(val airline: String, val number: String) {

    /** "BA283": the form used in flight ids. */
    val ident: String get() = airline + number

    /** "BA 283": the form shown to people. */
    override fun toString(): String = "$airline $number"

    companion object {
        // Two characters, letters or digits but not both digits (e.g. BA, U2, 9W), then 1–4 digits,
        // then an optional letter suffix (some airlines use e.g. "BA 283A").
        private val IATA = Regex("^([A-Z][A-Z0-9]|[0-9][A-Z])0*([0-9]{1,4})[A-Z]?$")

        // Three letters (the code air traffic control uses, e.g. BAW), then digits.
        private val ICAO = Regex("^([A-Z]{3})0*([0-9]{1,4})[A-Z]?$")

        /** 3-letter airline codes we can translate to the 2-letter ones people know. */
        private val ICAO_TO_IATA = mapOf(
            "BAW" to "BA", "VIR" to "VS", "EZY" to "U2", "RYR" to "FR", "DLH" to "LH",
            "AFR" to "AF", "KLM" to "KL", "UAE" to "EK", "DAL" to "DL", "AAL" to "AA",
            "SIA" to "SQ", "QFA" to "QF", "UAL" to "UA",
        )

        /** Returns null if [text] doesn't look like a flight number. */
        fun parse(text: String): FlightNumber? {
            val cleaned = text.uppercase().filter { it.isLetterOrDigit() }
            IATA.matchEntire(cleaned)?.let { m ->
                return FlightNumber(m.groupValues[1], m.groupValues[2].trimStart('0').ifEmpty { "0" })
            }
            ICAO.matchEntire(cleaned)?.let { m ->
                val iata = ICAO_TO_IATA[m.groupValues[1]] ?: return null
                return FlightNumber(iata, m.groupValues[2].trimStart('0').ifEmpty { "0" })
            }
            return null
        }
    }
}
