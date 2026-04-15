package com.execos.data.strava

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

data class StravaTokens(
    val accessToken: String,
    val refreshToken: String,
    val expiresAtEpochSeconds: Long,
    val athleteId: Long?,
)

@Singleton
class StravaTokenStore @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val prefs = EncryptedSharedPreferences.create(
        context,
        "strava_tokens_v1",
        MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build(),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
    )

    private fun key(uid: String, suffix: String) = "u:$uid:$suffix"

    fun get(uid: String): StravaTokens? {
        val a = prefs.getString(key(uid, "access"), null) ?: return null
        val r = prefs.getString(key(uid, "refresh"), null) ?: return null
        val e = prefs.getLong(key(uid, "expires"), 0L)
        val athlete = if (prefs.contains(key(uid, "athlete"))) prefs.getLong(key(uid, "athlete"), 0L) else null
        return StravaTokens(accessToken = a, refreshToken = r, expiresAtEpochSeconds = e, athleteId = athlete)
    }

    fun put(uid: String, tokens: StravaTokens) {
        prefs.edit()
            .putString(key(uid, "access"), tokens.accessToken)
            .putString(key(uid, "refresh"), tokens.refreshToken)
            .putLong(key(uid, "expires"), tokens.expiresAtEpochSeconds)
            .apply {
                if (tokens.athleteId != null) putLong(key(uid, "athlete"), tokens.athleteId) else remove(key(uid, "athlete"))
            }
            .apply()
    }

    fun clear(uid: String) {
        prefs.edit()
            .remove(key(uid, "access"))
            .remove(key(uid, "refresh"))
            .remove(key(uid, "expires"))
            .remove(key(uid, "athlete"))
            .apply()
    }
}

