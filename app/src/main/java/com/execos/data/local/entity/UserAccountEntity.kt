package com.execos.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "user_accounts",
    indices = [
        Index(value = ["email"], unique = true),
    ],
)
data class UserAccountEntity(
    @PrimaryKey val id: String,
    val email: String,
    val passwordHash: String,
    val createdAt: String,
)

