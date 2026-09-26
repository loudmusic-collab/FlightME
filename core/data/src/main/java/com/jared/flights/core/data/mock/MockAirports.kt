package com.jared.flights.core.data.mock

import com.jared.flights.core.model.Airport
import com.jared.flights.core.model.LatLon
import java.time.ZoneId

/** Real airports used by the mock flights. */
internal object MockAirports {
    private val london = ZoneId.of("Europe/London")

    val LHR = Airport("LHR", "EGLL", "Heathrow", "London", london, LatLon(51.4700, -0.4543))
    val LGW = Airport("LGW", "EGKK", "Gatwick", "London", london, LatLon(51.1537, -0.1821))
    val STN = Airport("STN", "EGSS", "Stansted", "London", london, LatLon(51.8860, 0.2389))
    val MAN = Airport("MAN", "EGCC", "Manchester", "Manchester", london, LatLon(53.3650, -2.2728))
    val EDI = Airport("EDI", "EGPH", "Edinburgh", "Edinburgh", london, LatLon(55.9500, -3.3725))
    val LPL = Airport("LPL", "EGGP", "Liverpool John Lennon", "Liverpool", london, LatLon(53.3336, -2.8497))
    val DUB = Airport("DUB", "EIDW", "Dublin", "Dublin", ZoneId.of("Europe/Dublin"), LatLon(53.4264, -6.2499))
    val AMS = Airport("AMS", "EHAM", "Schiphol", "Amsterdam", ZoneId.of("Europe/Amsterdam"), LatLon(52.3105, 4.7683))
    val CDG = Airport("CDG", "LFPG", "Charles de Gaulle", "Paris", ZoneId.of("Europe/Paris"), LatLon(49.0097, 2.5479))
    val FRA = Airport("FRA", "EDDF", "Frankfurt", "Frankfurt", ZoneId.of("Europe/Berlin"), LatLon(50.0379, 8.5622))
    val BCN = Airport("BCN", "LEBL", "Barcelona–El Prat", "Barcelona", ZoneId.of("Europe/Madrid"), LatLon(41.2974, 2.0833))
    val GVA = Airport("GVA", "LSGG", "Geneva", "Geneva", ZoneId.of("Europe/Zurich"), LatLon(46.2381, 6.1090))
    val DXB = Airport("DXB", "OMDB", "Dubai International", "Dubai", ZoneId.of("Asia/Dubai"), LatLon(25.2532, 55.3657))
    val SIN = Airport("SIN", "WSSS", "Changi", "Singapore", ZoneId.of("Asia/Singapore"), LatLon(1.3644, 103.9915))
    val JFK = Airport("JFK", "KJFK", "John F. Kennedy", "New York", ZoneId.of("America/New_York"), LatLon(40.6413, -73.7781))
    val LAX = Airport("LAX", "KLAX", "Los Angeles International", "Los Angeles", ZoneId.of("America/Los_Angeles"), LatLon(33.9416, -118.4085))
}
