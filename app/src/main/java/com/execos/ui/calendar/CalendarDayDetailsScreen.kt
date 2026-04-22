package com.execos.ui.calendar

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarDayDetailsScreen(
    onBack: () -> Unit,
    viewModel: CalendarDayDetailsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Day details", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface),
            )
        },
    ) { padding ->
        when {
            state.loading -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            }

            state.error != null -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(24.dp),
                ) {
                    Text(state.error ?: "")
                }
            }

            else -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(state.dateIso, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                    SummaryRow(
                        tasks = state.tasksCount,
                        decisions = state.decisionsCount,
                        reflections = state.reflectionsCount,
                    )
                    DayGoalsCard(
                        plannedGoals = state.plannedGoals,
                        achievedGoals = state.achievedGoals,
                    )
                }
            }
        }
    }
}

@Composable
private fun SummaryRow(
    tasks: Int,
    decisions: Int,
    reflections: Int,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        StatChip("Tasks", tasks)
        StatChip("Decisions", decisions)
        StatChip("Reflections", reflections)
    }
}

@Composable
private fun StatChip(
    label: String,
    value: Int,
) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .padding(horizontal = 12.dp, vertical = 10.dp),
    ) {
        Text(value.toString(), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun DayGoalsCard(
    plannedGoals: List<String>,
    achievedGoals: List<String>,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Goals", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f))
            GoalsSection(
                title = "Planned",
                goals = plannedGoals,
                emptyText = "No planned goals.",
                useFlipCards = true,
            )
            GoalsSection(title = "Achieved", goals = achievedGoals, emptyText = "No achieved goals yet.")
        }
    }
}

@Composable
private fun GoalsSection(
    title: String,
    goals: List<String>,
    emptyText: String,
    useFlipCards: Boolean = false,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            title,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.SemiBold,
        )
        if (goals.isEmpty()) {
            Text(
                emptyText,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                goals.forEachIndexed { idx, goal ->
                    if (useFlipCards) {
                        PlannedGoalFlipCard(rank = idx + 1, goal = goal)
                    } else {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                        ) {
                            Text(
                                "${idx + 1}. $goal",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PlannedGoalFlipCard(
    rank: Int,
    goal: String,
) {
    var flipped by rememberSaveable(goal) { mutableStateOf(false) }
    val rotationY by animateFloatAsState(
        targetValue = if (flipped) 180f else 0f,
        animationSpec = tween(durationMillis = 420),
        label = "plannedGoalFlip",
    )
    val shape = RoundedCornerShape(14.dp)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                this.rotationY = rotationY
                cameraDistance = 12f * density
            }
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
            .clickable { flipped = !flipped }
            .padding(horizontal = 12.dp, vertical = 12.dp),
    ) {
        if (rotationY <= 90f) {
            Text(
                text = "$rank. $goal",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
        } else {
            Text(
                text = "Planned goal #$rank\nTap again to flip back",
                modifier = Modifier.graphicsLayer { this.rotationY = 180f },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
