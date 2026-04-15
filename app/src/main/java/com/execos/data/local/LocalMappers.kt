package com.execos.data.local

import com.execos.data.local.entity.DecisionEntity
import com.execos.data.local.entity.EnergyEntity
import com.execos.data.local.entity.ReflectionEntity
import com.execos.data.local.entity.TaskEntity
import com.execos.data.local.entity.WeeklyReviewEntity
import com.execos.data.model.DecisionItem
import com.execos.data.model.EnergyEntry
import com.execos.data.model.ReflectionItem
import com.execos.data.model.TaskItem
import com.execos.data.model.WeeklyReviewItem

fun TaskEntity.toItem() = TaskItem(
    id = id,
    title = title,
    impactScore = impactScore,
    notes = notes,
    completed = completed,
    date = date,
)

fun TaskItem.toEntity(userId: String, id: String = this.id) = TaskEntity(
    id = id,
    userId = userId,
    title = title,
    impactScore = impactScore.coerceIn(1, 5),
    notes = notes,
    completed = completed,
    date = date,
)

fun DecisionEntity.toItem() = DecisionItem(
    id = id,
    title = title,
    context = context,
    options = options,
    finalDecision = finalDecision,
    confidence = confidence,
    date = date,
    aiAnalysis = aiAnalysis,
)

fun DecisionItem.toEntity(userId: String, id: String = this.id) = DecisionEntity(
    id = id,
    userId = userId,
    title = title,
    context = context,
    options = options,
    finalDecision = finalDecision,
    confidence = confidence.coerceIn(1, 5),
    date = date,
    aiAnalysis = aiAnalysis,
)

fun ReflectionEntity.toItem() = ReflectionItem(
    id = id,
    textInput = textInput,
    aiOutput = aiOutput,
    date = date,
)

fun ReflectionItem.toEntity(userId: String, id: String = this.id) = ReflectionEntity(
    id = id,
    userId = userId,
    textInput = textInput,
    aiOutput = aiOutput,
    date = date,
)

fun EnergyEntity.toEntry() = EnergyEntry(
    id = id,
    morningScore = morningScore,
    eveningScore = eveningScore,
    date = date,
)

fun EnergyEntry.toEntity(userId: String, id: String) = EnergyEntity(
    id = id,
    userId = userId,
    morningScore = morningScore.coerceIn(1, 5),
    eveningScore = eveningScore.coerceIn(1, 5),
    date = date,
)

fun WeeklyReviewEntity.toItem() = WeeklyReviewItem(
    id = id,
    weekStart = weekStart,
    wins = wins,
    mistakes = mistakes,
    learnings = learnings,
    aiSummary = aiSummary,
)

fun WeeklyReviewItem.toEntity(userId: String, id: String) = WeeklyReviewEntity(
    id = id,
    userId = userId,
    weekStart = weekStart,
    wins = wins,
    mistakes = mistakes,
    learnings = learnings,
    aiSummary = aiSummary,
)
