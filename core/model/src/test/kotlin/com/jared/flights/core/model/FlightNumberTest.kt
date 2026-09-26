package com.jared.flights.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FlightNumberTest {

    private fun parsed(text: String) = FlightNumber.parse(text)?.toString()

    @Test fun `common ways of typing the same flight`() {
        assertEquals("BA 283", parsed("BA283"))
        assertEquals("BA 283", parsed("ba 283"))
        assertEquals("BA 283", parsed("BA0283"))
        assertEquals("BA 283", parsed(" BA-283 "))
    }

    @Test fun `airline codes with a digit`() {
        assertEquals("U2 8461", parsed("U28461"))
        assertEquals("9W 1", parsed("9W1"))
    }

    @Test fun `three-letter airline codes are translated`() {
        assertEquals("BA 283", parsed("BAW283"))
        assertEquals("U2 8461", parsed("EZY8461"))
    }

    @Test fun `an optional letter on the end is ignored`() {
        assertEquals("BA 283", parsed("BA283A"))
    }

    @Test fun `not flight numbers`() {
        assertNull(parsed(""))
        assertNull(parsed("BA"))
        assertNull(parsed("283"))
        assertNull(parsed("12345"))
        assertNull(parsed("BA12345"))
        assertNull(parsed("London"))
        assertNull(parsed("XYZ123")) // unknown 3-letter airline
    }

    @Test fun `ident joins airline and number`() {
        assertEquals("FM123", FlightNumber.parse("fm 123")?.ident)
    }
}
