package com.jared.flights.core.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

private val Context.appPrefsStore: DataStore<Preferences> by preferencesDataStore(name = "app_prefs")

/** Small everyday settings saved on the phone. Settings (1.11) will add more here. */
@Singleton
class AppPreferences @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    /** Have we already asked the user to allow notifications? We only ask once. */
    suspend fun notificationPromptShown(): Boolean = context.appPrefsStore.data.first()[PROMPT_SHOWN] ?: false

    suspend fun setNotificationPromptShown() {
        context.appPrefsStore.edit { it[PROMPT_SHOWN] = true }
    }

    private companion object {
        val PROMPT_SHOWN = booleanPreferencesKey("notification_prompt_shown")
    }
}
