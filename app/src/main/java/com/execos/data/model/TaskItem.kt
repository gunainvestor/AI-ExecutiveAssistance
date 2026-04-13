package com.execos.data.model

data class TaskItem(
    val id: String = "",
    val title: String = "",
    val impactScore: Int = 3,
    val notes: String = "",
    val completed: Boolean = false,
    val date: String = "",
)
