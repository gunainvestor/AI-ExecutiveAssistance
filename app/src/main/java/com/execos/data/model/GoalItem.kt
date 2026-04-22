package com.execos.data.model

data class GoalItem(
    val id: String = "",
    val periodType: String,
    val periodKey: String,
    val rank: Int,
    val title: String,
)

object GoalPeriod {
    const val YEAR = "YEAR"
    const val QUARTER = "QUARTER"
    const val MONTH = "MONTH"
    const val WEEK = "WEEK"
}

