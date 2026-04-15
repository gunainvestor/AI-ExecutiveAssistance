package com.execos.data.strava

import com.google.gson.annotations.SerializedName

data class StravaTokenResponse(
    @SerializedName("access_token") val accessToken: String,
    @SerializedName("refresh_token") val refreshToken: String,
    @SerializedName("expires_at") val expiresAt: Long,
    @SerializedName("athlete") val athlete: StravaAthleteDto?,
)

data class StravaAthleteDto(
    @SerializedName("id") val id: Long,
)

data class StravaActivityDto(
    @SerializedName("id") val id: Long,
    @SerializedName("name") val name: String?,
    @SerializedName("type") val type: String?,
    @SerializedName("start_date") val startDate: String?, // ISO-8601 Z
    @SerializedName("elapsed_time") val elapsedTime: Long?,
    @SerializedName("moving_time") val movingTime: Long?,
    @SerializedName("distance") val distanceMeters: Double?,
    @SerializedName("total_elevation_gain") val totalElevationGain: Double?,
    @SerializedName("kilojoules") val kilojoules: Double?,
    @SerializedName("calories") val calories: Double?,
    @SerializedName("average_heartrate") val avgHeartRate: Double?,
    @SerializedName("max_heartrate") val maxHeartRate: Double?,
)

