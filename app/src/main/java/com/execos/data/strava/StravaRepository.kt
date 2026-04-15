package com.execos.data.strava

import com.execos.BuildConfig
import com.execos.data.local.ExecOsDatabase
import com.execos.data.local.entity.StravaActivityEntity
import com.execos.data.repo.AuthRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StravaRepository @Inject constructor(
    private val api: StravaApi,
    private val tokenStore: StravaTokenStore,
    private val authRepository: AuthRepository,
    private val db: ExecOsDatabase,
) {
    private val dao get() = db.stravaActivityDao()

    suspend fun isConnected(): Boolean = tokenStore.get(authRepository.currentUserId()) != null

    suspend fun disconnect() {
        val uid = authRepository.currentUserId()
        tokenStore.clear(uid)
    }

    suspend fun exchangeCodeAndStore(code: String) = withContext(Dispatchers.IO) {
        val uid = authRepository.currentUserId()
        val res = api.tokenExchange(
            clientId = BuildConfig.STRAVA_CLIENT_ID,
            clientSecret = BuildConfig.STRAVA_CLIENT_SECRET,
            code = code,
        )
        tokenStore.put(
            uid,
            StravaTokens(
                accessToken = res.accessToken,
                refreshToken = res.refreshToken,
                expiresAtEpochSeconds = res.expiresAt,
                athleteId = res.athlete?.id,
            ),
        )
    }

    private suspend fun ensureAccessToken(uid: String): String = withContext(Dispatchers.IO) {
        val tokens = tokenStore.get(uid) ?: throw IllegalStateException("Strava not connected")
        val now = System.currentTimeMillis() / 1000L
        // Refresh if expiring in the next 2 minutes.
        if (tokens.expiresAtEpochSeconds > now + 120) return@withContext tokens.accessToken

        val refreshed = api.refreshToken(
            clientId = BuildConfig.STRAVA_CLIENT_ID,
            clientSecret = BuildConfig.STRAVA_CLIENT_SECRET,
            refreshToken = tokens.refreshToken,
        )
        val next = StravaTokens(
            accessToken = refreshed.accessToken,
            refreshToken = refreshed.refreshToken,
            expiresAtEpochSeconds = refreshed.expiresAt,
            athleteId = refreshed.athlete?.id ?: tokens.athleteId,
        )
        tokenStore.put(uid, next)
        next.accessToken
    }

    suspend fun syncRecentActivities(daysBack: Int = 30): Int = withContext(Dispatchers.IO) {
        val uid = authRepository.currentUserId()
        val token = ensureAccessToken(uid)
        val after = (System.currentTimeMillis() / 1000L) - (daysBack.toLong() * 24 * 60 * 60)

        val page1 = api.listActivities(
            bearer = "Bearer $token",
            afterEpochSeconds = after,
            page = 1,
            perPage = 50,
        )

        val rows = page1.mapNotNull { a ->
            val start = a.startDate ?: return@mapNotNull null
            StravaActivityEntity(
                id = a.id,
                userId = uid,
                name = a.name ?: "",
                type = a.type ?: "",
                startDate = start,
                elapsedTimeSec = a.elapsedTime ?: 0L,
                movingTimeSec = a.movingTime ?: 0L,
                distanceMeters = a.distanceMeters ?: 0.0,
                elevationGainMeters = a.totalElevationGain ?: 0.0,
                kilojoules = a.kilojoules,
                calories = a.calories,
                avgHeartRate = a.avgHeartRate,
                maxHeartRate = a.maxHeartRate,
            )
        }
        if (rows.isNotEmpty()) dao.upsertAll(rows)
        rows.size
    }

    suspend fun recentActivitiesSnapshot(limit: Int = 20): List<StravaActivityEntity> =
        dao.getRecentSnapshot(authRepository.currentUserId(), limit)
}

