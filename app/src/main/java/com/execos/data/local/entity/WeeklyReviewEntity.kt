package com.execos.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "weekly_reviews",
    indices = [Index(value = ["userId", "weekStart"], unique = true)],
)
data class WeeklyReviewEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val weekStart: String,
    val wins: String,
    val mistakes: String,
    val learnings: String,
    val aiSummary: String?,
)
