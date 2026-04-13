package com.execos.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "tasks",
    indices = [Index(value = ["date"])],
)
data class TaskEntity(
    @PrimaryKey val id: String,
    val title: String,
    val impactScore: Int,
    val notes: String,
    val completed: Boolean,
    val date: String,
)
