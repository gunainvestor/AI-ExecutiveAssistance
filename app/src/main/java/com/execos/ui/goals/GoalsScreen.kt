package com.execos.ui.goals

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.execos.data.model.GoalItem
import com.execos.data.model.GoalPeriod
import com.execos.ui.components.ExecGradientBackground
import com.execos.ui.components.ExecOutlinedTextField
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoalsScreen(
    viewModel: GoalsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(state.message) {
        val m = state.message ?: return@LaunchedEffect
        snackbar.showSnackbar(m)
        viewModel.consumeMessage()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Goal planning", fontWeight = FontWeight.SemiBold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        ExecGradientBackground {
            when {
                state.loading -> {
                    Column(
                        Modifier.fillMaxSize().padding(padding),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) { CircularProgressIndicator(color = MaterialTheme.colorScheme.primary) }
                }
                state.error != null -> {
                    Column(Modifier.fillMaxSize().padding(padding).padding(24.dp)) {
                        Text(state.error ?: "")
                    }
                }
                else -> {
                    Column(
                        Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState())
                            .padding(horizontal = 16.dp).padding(bottom = 24.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Text(
                            "Set top 3 goals for each horizon. Weekly and Daily screens will reference Month/Quarter/Week goals.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        GoalsSection("Year (${state.yearKey})", GoalPeriod.YEAR, state.yearGoals, viewModel)
                        GoalsSection("Quarter (${state.quarterKey})", GoalPeriod.QUARTER, state.quarterGoals, viewModel)
                        GoalsSection("Month (${state.monthKey})", GoalPeriod.MONTH, state.monthGoals, viewModel)
                        GoalsSection("Week (${state.weekKey})", GoalPeriod.WEEK, state.weekGoals, viewModel)
                        Spacer(Modifier.height(72.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun GoalsSection(
    title: String,
    periodType: String,
    goals: List<GoalItem>,
    viewModel: GoalsViewModel,
) {
    Card(modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.large) {
        Column(Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                IconButton(onClick = { viewModel.addGoal(periodType) }) {
                    Icon(Icons.Default.Add, contentDescription = "Add goal")
                }
            }
            Spacer(Modifier.height(8.dp))
            if (goals.isEmpty()) {
                Text("No goals yet.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            goals.sortedBy { it.rank }.forEach { goal ->
                GoalEditorRow(
                    goal = goal,
                    onUpdate = { updatedTitle -> viewModel.updateGoal(goal, updatedTitle) },
                    onDelete = { viewModel.deleteGoal(goal) },
                )
                Spacer(Modifier.height(6.dp))
            }
        }
    }
}

@Composable
private fun GoalEditorRow(
    goal: GoalItem,
    onUpdate: (String) -> Unit,
    onDelete: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    var draft by remember(goal.id) { mutableStateOf(goal.title) }
    var pendingSave by remember(goal.id) { mutableStateOf<Job?>(null) }
    val interactions = remember(goal.id) { MutableInteractionSource() }
    val focused by interactions.collectIsFocusedAsState()

    // If the underlying goal changes externally, keep draft in sync (without fighting the user mid-typing).
    LaunchedEffect(goal.title) {
        if (!focused && goal.title != draft) draft = goal.title
    }

    fun scheduleSave(next: String) {
        pendingSave?.cancel()
        pendingSave = scope.launch {
            delay(220)
            onUpdate(next)
        }
    }

    DisposableEffect(goal.id) {
        onDispose {
            pendingSave?.cancel()
            if (draft != goal.title) onUpdate(draft)
        }
    }

    Row(verticalAlignment = Alignment.CenterVertically) {
        Text("${goal.rank}.", modifier = Modifier.padding(end = 6.dp))
        ExecOutlinedTextField(
            value = draft,
            onValueChange = { t ->
                draft = t
                scheduleSave(t)
            },
            label = { Text("Goal") },
            modifier = Modifier.weight(1f),
            singleLine = true,
            interactionSource = interactions,
        )
        IconButton(onClick = onDelete) {
            Icon(Icons.Default.Delete, contentDescription = "Delete goal")
        }
    }
}

