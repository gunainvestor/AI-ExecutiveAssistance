package com.execos.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "strava_activities",
    indices = [
        Index(value = ["userId", "startDate"]),
    ],
)
data class StravaActivityEntity(
    @PrimaryKey val id: Long,
    val userId: String,
    val name: String,
    val type: String,
    val startDate: String,
    val elapsedTimeSec: Long,
    val movingTimeSec: Long,
    val distanceMeters: Double,
    val elevationGainMeters: Double,
    val kilojoules: Double?,
    val calories: Double?,
    val avgHeartRate: Double?,
    val maxHeartRate: Double?,
)

