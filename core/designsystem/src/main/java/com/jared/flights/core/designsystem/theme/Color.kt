package com.jared.flights.core.designsystem.theme

import androidx.compose.ui.graphics.Color

// Provisional FlightME palette (DECISIONS #19): mostly neutral ink on paper,
// one calm accent, and colour reserved for flight status.

// Neutrals
internal val Ink = Color(0xFF14171C)
internal val InkSoft = Color(0xFF5B616B)
internal val Paper = Color(0xFFFAFAF8)
internal val PaperRaised = Color(0xFFF1F1EE)
internal val Line = Color(0xFFD9DAD6)

internal val Night = Color(0xFF0F1115)
internal val NightRaised = Color(0xFF1A1D23)
internal val NightText = Color(0xFFE8E9EB)
internal val NightTextSoft = Color(0xFF9CA2AB)
internal val NightLine = Color(0xFF2E323A)

// Accent
internal val Sky = Color(0xFF2F6BD8)
internal val SkyLight = Color(0xFF8AB4FF)

// Status: light theme
internal val OnTimeLight = Color(0xFF1B8A55)
internal val MinorDelayLight = Color(0xFFB86E00)
internal val MajorDelayLight = Color(0xFFC62828)
internal val CancelledLight = Color(0xFFC62828)
internal val UnknownLight = Color(0xFF7A808A)

// Status: dark theme (lighter so they stay readable on a dark background)
internal val OnTimeDark = Color(0xFF5DD39A)
internal val MinorDelayDark = Color(0xFFF2B35B)
internal val MajorDelayDark = Color(0xFFFF7A70)
internal val CancelledDark = Color(0xFFFF7A70)
internal val UnknownDark = Color(0xFF9CA2AB)
