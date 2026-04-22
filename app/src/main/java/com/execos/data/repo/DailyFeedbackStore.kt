package com.execos.data.repo

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.execos.util.Dates
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dailyFeedbackDataStore by preferencesDataStore("daily_feedback")

data class DailyFeedback(
    val date: String,
    val text: String,
)

@Singleton
class DailyFeedbackStore @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val KEY_DATE = stringPreferencesKey("date")
    private val KEY_TEXT = stringPreferencesKey("text")
    private val KEY_SHOWN = booleanPreferencesKey("shown")

    val todayUnseenFeedback: Flow<DailyFeedback?> =
        context.dailyFeedbackDataStore.data.map { prefs: Preferences ->
            val today = Dates.todayIso()
            val date = prefs[KEY_DATE] ?: return@map null
            val text = prefs[KEY_TEXT].orEmpty()
            val shown = prefs[KEY_SHOWN] ?: false
            if (date != today) return@map null
            if (text.isBlank()) return@map null
            if (shown) return@map null
            DailyFeedback(date = date, text = text)
        }

    suspend fun setFeedback(date: String, text: String) {
        context.dailyFeedbackDataStore.edit { prefs ->
            prefs[KEY_DATE] = date
            prefs[KEY_TEXT] = text
            prefs[KEY_SHOWN] = false
        }
    }

    suspend fun markShown(date: String) {
        context.dailyFeedbackDataStore.edit { prefs ->
            if (prefs[KEY_DATE] == date) {
                prefs[KEY_SHOWN] = true
            }
        }
    }
}

