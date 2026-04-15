package com.execos.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "energy",
    indices = [Index(value = ["userId", "date"], unique = true)],
)
data class EnergyEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val morningScore: Int,
    val eveningScore: Int,
    val date: String,
)
