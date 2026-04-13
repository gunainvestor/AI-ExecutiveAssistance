package com.execos.data.model

data class DecisionItem(
    val id: String = "",
    val title: String = "",
    val context: String = "",
    val options: String = "",
    val finalDecision: String = "",
    val confidence: Int = 3,
    val date: String = "",
    val aiAnalysis: String? = null,
)
