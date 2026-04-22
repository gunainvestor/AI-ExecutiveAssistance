package com.execos.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "goals",
    indices = [
        Index(value = ["userId", "periodType", "periodKey", "rank"], unique = true),
    ],
)
data class GoalEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val periodType: String,
    val periodKey: String,
    val rank: Int,
    val title: String,
)

