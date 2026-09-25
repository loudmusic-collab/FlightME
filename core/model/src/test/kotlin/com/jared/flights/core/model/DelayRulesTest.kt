package com.jared.flights.core.model

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Duration

class DelayRulesTest {

    private fun severity(minutes: Long, seconds: Long = 0) =
        DelayRules.severityOf(Duration.ofMinutes(minutes).plusSeconds(seconds))

    @Test fun `no delay is on time`() = assertEquals(DelaySeverity.ON_TIME, severity(0))

    @Test fun `early is on time`() = assertEquals(DelaySeverity.ON_TIME, severity(-20))

    @Test fun `just under 15 minutes is on time`() =
        assertEquals(DelaySeverity.ON_TIME, severity(14, 59))

    @Test fun `15 minutes is a minor delay`() = assertEquals(DelaySeverity.MINOR, severity(15))

    @Test fun `44 minutes is still minor`() = assertEquals(DelaySeverity.MINOR, severity(44))

    @Test fun `45 minutes is a major delay`() = assertEquals(DelaySeverity.MAJOR, severity(45))

    @Test fun `several hours is major`() = assertEquals(DelaySeverity.MAJOR, severity(300))
}
