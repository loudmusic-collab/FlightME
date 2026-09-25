package com.jared.flights.core.data.mock

import com.jared.flights.core.model.Airport
import java.time.ZoneId

/** Real airports used by the mock flights. */
internal object MockAirports {
    private val london = ZoneId.of("Europe/London")

    val LHR = Airport("LHR", "EGLL", "Heathrow", "London", london)
    val LGW = Airport("LGW", "EGKK", "Gatwick", "London", london)
    val STN = Airport("STN", "EGSS", "Stansted", "London", london)
    val MAN = Airport("MAN", "EGCC", "Manchester", "Manchester", london)
    val EDI = Airport("EDI", "EGPH", "Edinburgh", "Edinburgh", london)
    val DUB = Airport("DUB", "EIDW", "Dublin", "Dublin", ZoneId.of("Europe/Dublin"))
    val AMS = Airport("AMS", "EHAM", "Schiphol", "Amsterdam", ZoneId.of("Europe/Amsterdam"))
    val CDG = Airport("CDG", "LFPG", "Charles de Gaulle", "Paris", ZoneId.of("Europe/Paris"))
    val FRA = Airport("FRA", "EDDF", "Frankfurt", "Frankfurt", ZoneId.of("Europe/Berlin"))
    val GVA = Airport("GVA", "LSGG", "Geneva", "Geneva", ZoneId.of("Europe/Zurich"))
    val DXB = Airport("DXB", "OMDB", "Dubai International", "Dubai", ZoneId.of("Asia/Dubai"))
    val JFK = Airport("JFK", "KJFK", "John F. Kennedy", "New York", ZoneId.of("America/New_York"))
    val LAX = Airport("LAX", "KLAX", "Los Angeles International", "Los Angeles", ZoneId.of("America/Los_Angeles"))
}
