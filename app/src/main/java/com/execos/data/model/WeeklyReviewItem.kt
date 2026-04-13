package com.execos.data.model

data class WeeklyReviewItem(
    val id: String = "",
    val weekStart: String = "",
    val wins: String = "",
    val mistakes: String = "",
    val learnings: String = "",
    val aiSummary: String? = null,
)
