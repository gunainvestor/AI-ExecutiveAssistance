package com.execos.data.usage

data class AppUsageItem(
    val packageName: String,
    val appLabel: String,
    val minutesToday: Int,
    val minutesThisWeek: Int,
)

