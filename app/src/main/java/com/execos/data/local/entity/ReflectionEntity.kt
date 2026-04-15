package com.execos.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "reflections",
    indices = [Index(value = ["userId", "date"])],
)
data class ReflectionEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val textInput: String,
    val aiOutput: String,
    val date: String,
)
