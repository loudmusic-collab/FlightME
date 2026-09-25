package com.jared.flights.core.designsystem.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.FontWeight

// Material 3 defaults, with lighter headings for a minimal look.
private val Base = Typography()

internal val FlightMeTypography = Typography(
    displayLarge = Base.displayLarge.copy(fontWeight = FontWeight.Light),
    displayMedium = Base.displayMedium.copy(fontWeight = FontWeight.Light),
    displaySmall = Base.displaySmall.copy(fontWeight = FontWeight.Light),
    headlineLarge = Base.headlineLarge.copy(fontWeight = FontWeight.Normal),
    headlineMedium = Base.headlineMedium.copy(fontWeight = FontWeight.Normal),
    headlineSmall = Base.headlineSmall.copy(fontWeight = FontWeight.Normal),
    titleLarge = Base.titleLarge.copy(fontWeight = FontWeight.Medium),
)
