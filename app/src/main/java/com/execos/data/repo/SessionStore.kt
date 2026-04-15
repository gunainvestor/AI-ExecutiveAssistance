package com.execos.data.repo

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.sessionDataStore by preferencesDataStore("session")

@Singleton
class SessionStore @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val KEY_UID = stringPreferencesKey("uid")

    val uidFlow: Flow<String?> = context.sessionDataStore.data.map { prefs: Preferences ->
        prefs[KEY_UID]
    }

    suspend fun setUid(uid: String?) {
        context.sessionDataStore.edit { prefs ->
            if (uid == null) prefs.remove(KEY_UID) else prefs[KEY_UID] = uid
        }
    }
}

