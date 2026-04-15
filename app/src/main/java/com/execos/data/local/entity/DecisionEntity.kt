package com.execos.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "decisions",
    indices = [Index(value = ["userId", "date"])],
)
data class DecisionEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val title: String,
    val context: String,
    val options: String,
    val finalDecision: String,
    val confidence: Int,
    val date: String,
    val aiAnalysis: String?,
)
