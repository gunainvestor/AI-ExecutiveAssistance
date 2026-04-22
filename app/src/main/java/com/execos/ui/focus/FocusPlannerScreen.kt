package com.execos.ui.focus

import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
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
import com.execos.data.model.TaskItem
import com.execos.ui.components.ExecGradientBackground
import com.execos.ui.components.ExecOutlinedTextField
import com.execos.ui.components.PlannedGoalsCard
import com.execos.ui.components.PlannedGoalsHorizon
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FocusPlannerScreen(
    viewModel: FocusPlannerViewModel = hiltViewModel(),
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
                title = {
                    Text(
                        "Today’s priorities",
                        fontWeight = FontWeight.SemiBold,
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                ),
            )
        },
        floatingActionButton = {
            if (!state.loading && state.error == null) {
                FloatingActionButton(
                    onClick = {
                        if (state.tasks.size < 3 && !state.busy) viewModel.addTask()
                    },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add priority")
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        ExecGradientBackground {
            when {
                state.loading -> {
                    Column(
                        Modifier
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
                        Modifier
                            .fillMaxSize()
                            .padding(padding)
                            .padding(24.dp),
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Text(state.error ?: "", style = MaterialTheme.typography.bodyLarge)
                    }
                }
                else -> {
                    Column(
                        Modifier
                            .fillMaxSize()
                            .padding(padding)
                            .padding(horizontal = 16.dp)
                            .verticalScroll(rememberScrollState()),
                    ) {
                        val total = state.tasks.size.coerceAtLeast(1)
                        val done = state.tasks.count { it.completed }
                        val rate = if (state.tasks.isEmpty()) 0f else done.toFloat() / total
                        Text(
                            "Top 3 for ${state.date}",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Spacer(Modifier.height(12.dp))
                        PlannedGoalsCard(
                            horizons = listOf(
                                PlannedGoalsHorizon("Yearly", state.yearGoals.map { it.title }),
                                PlannedGoalsHorizon("Quarter", state.quarterGoals.map { it.title }),
                                PlannedGoalsHorizon("Month", state.monthGoals.map { it.title }),
                                PlannedGoalsHorizon("Week", state.weekGoals.map { it.title }),
                            ),
                        )
                        Spacer(Modifier.height(12.dp))
                        Text(
                            "$done of ${state.tasks.size} done · ${(rate * 100).toInt()}%",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.height(12.dp))
                        LinearProgressIndicator(
                            progress = { rate },
                            modifier = Modifier.fillMaxWidth(),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant,
                        )
                        Spacer(Modifier.height(16.dp))
                        state.tasks.forEach { task ->
                            TaskEditorCard(
                                task = task,
                                onChange = viewModel::updateTask,
                                onToggle = { viewModel.toggleComplete(task) },
                                onDelete = { viewModel.deleteTask(task) },
                            )
                            Spacer(Modifier.height(12.dp))
                        }
                        if (state.tasks.isEmpty()) {
                            Text(
                                "Tap + to add up to three high-impact tasks. Edit title, impact, and notes — everything saves as you type.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Spacer(Modifier.height(80.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun TaskEditorCard(
    task: TaskItem,
    onChange: (TaskItem) -> Unit,
    onToggle: () -> Unit,
    onDelete: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    var titleDraft by remember(task.id) { mutableStateOf(task.title) }
    var notesDraft by remember(task.id) { mutableStateOf(task.notes) }
    var pendingSave by remember(task.id) { mutableStateOf<Job?>(null) }

    val titleInteractions = remember(task.id) { MutableInteractionSource() }
    val notesInteractions = remember(task.id) { MutableInteractionSource() }
    val titleFocused by titleInteractions.collectIsFocusedAsState()
    val notesFocused by notesInteractions.collectIsFocusedAsState()

    // Keep local drafts aligned when task changes from outside this editor.
    LaunchedEffect(task.title) {
        if (!titleFocused && task.title != titleDraft) titleDraft = task.title
    }
    LaunchedEffect(task.notes) {
        if (!notesFocused && task.notes != notesDraft) notesDraft = task.notes
    }

    fun scheduleSave(updated: TaskItem) {
        pendingSave?.cancel()
        pendingSave = scope.launch {
            delay(220)
            onChange(updated)
        }
    }

    DisposableEffect(task.id) {
        onDispose {
            pendingSave?.cancel()
            // Flush latest drafts when leaving composition.
            if (titleDraft != task.title || notesDraft != task.notes) {
                onChange(task.copy(title = titleDraft, notes = notesDraft))
            }
        }
    }

    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = task.completed,
                    onCheckedChange = { onToggle() },
                )
                Text(
                    "Done",
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Remove")
                }
            }
            Spacer(Modifier.height(8.dp))
            ExecOutlinedTextField(
                value = titleDraft,
                onValueChange = {
                    titleDraft = it
                    scheduleSave(task.copy(title = it, notes = notesDraft))
                },
                label = { Text("Priority title") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                interactionSource = titleInteractions,
            )
            Spacer(Modifier.height(12.dp))
            Text(
                "Impact · ${task.impactScore}",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Slider(
                value = task.impactScore.toFloat(),
                onValueChange = { onChange(task.copy(impactScore = it.toInt().coerceIn(1, 5))) },
                valueRange = 1f..5f,
                steps = 3,
                colors = SliderDefaults.colors(
                    thumbColor = MaterialTheme.colorScheme.primary,
                    activeTrackColor = MaterialTheme.colorScheme.primary,
                ),
            )
            ExecOutlinedTextField(
                value = notesDraft,
                onValueChange = {
                    notesDraft = it
                    scheduleSave(task.copy(title = titleDraft, notes = it))
                },
                label = { Text("Notes (optional)") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                maxLines = 8,
                interactionSource = notesInteractions,
            )
        }
    }
}
