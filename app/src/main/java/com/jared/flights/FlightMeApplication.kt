package com.jared.flights

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/** App entry point. `@HiltAndroidApp` switches on dependency injection. */
@HiltAndroidApp
class FlightMeApplication : Application()
