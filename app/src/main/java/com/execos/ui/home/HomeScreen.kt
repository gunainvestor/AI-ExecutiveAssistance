package com.execos.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Today
import androidx.compose.material.icons.filled.TrackChanges
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.execos.ui.components.ExecGradientBackground
import com.execos.ui.navigation.Routes
import com.execos.ui.widget.DailyGoalsWidgetReceiver
import androidx.compose.ui.platform.LocalContext

private data class FeatureTile(
    val route: String,
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigate: (String) -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
    dailyFeedbackViewModel: DailyFeedbackViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val feedback by dailyFeedbackViewModel.popup.collectAsStateWithLifecycle()
    val done = state.todayTasks.count { it.completed }
    val total = state.todayTasks.size.coerceAtLeast(1)
    val completionRate = if (state.todayTasks.isEmpty()) 0f else done.toFloat() / total
    val executionScore = if (state.todayTasks.isEmpty()) 38 else ((completionRate * 70f) + 30f).toInt().coerceIn(0, 100)
    val insight = when {
        state.todayTasks.isEmpty() -> "No priorities locked. Busy mode is likely."
        done == 0 -> "You focused on low-impact tasks today."
        done < state.todayTasks.size -> "You planned ${state.todayTasks.size} tasks but completed $done."
        else -> "You executed your bets. Keep this standard tomorrow."
    }
    val appBarScrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    LaunchedEffect(state.todayTasks, state.weekGoals) {
        DailyGoalsWidgetReceiver.refreshAll(context)
    }
    val tiles = listOf(
        FeatureTile(Routes.Home, "Top 3 Planner", "Define what actually matters", Icons.Default.Today),
        FeatureTile(Routes.Decisions, "Decisions", "What decision is blocking execution?", Icons.Default.Psychology),
        FeatureTile(Routes.AiCoach, "AI Coach", "Accountability alerts and momentum moves", Icons.Default.CheckCircle),
        FeatureTile(Routes.Weekly, "Performance Review", "How did you perform this week?", Icons.Default.TrackChanges),
        FeatureTile(Routes.Calendar, "Calendar", "See your month at a glance", Icons.Default.DateRange),
        FeatureTile(Routes.Goals, "Goals", "Year/quarter/month/week top 3", Icons.Default.TrackChanges),
        FeatureTile(Routes.Usage, "Usage", "See time-wasters (daily/weekly)", Icons.Default.TrackChanges),
        FeatureTile(Routes.Energy, "Energy", "Morning & evening", Icons.Default.Bolt),
    )

    Scaffold(
        modifier = Modifier.nestedScroll(appBarScrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = {
                    Column {
                        Text(
                            "ExecOS",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            "Daily operator report card",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { onNavigate(Routes.Account) }) {
                        Icon(Icons.Default.Person, contentDescription = "Account")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                ),
                scrollBehavior = appBarScrollBehavior,
            )
        },
    ) { padding ->
        ExecGradientBackground {
            if (feedback != null) {
                val f = feedback!!
                AlertDialog(
                    onDismissRequest = { dailyFeedbackViewModel.markShown(f) },
                    title = { Text("AI Coach Brief") },
                    text = { Text(f.text) },
                    confirmButton = {
                        Button(
                            onClick = {
                                dailyFeedbackViewModel.markShown(f)
                                onNavigate(Routes.Reflection)
                            },
                        ) {
                            Text("Evaluate my day")
                        }
                    },
                    dismissButton = {
                        OutlinedButton(onClick = { dailyFeedbackViewModel.markShown(f) }) {
                            Text("Later")
                        }
                    },
                )
            }
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
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Text("Could not start", style = MaterialTheme.typography.titleLarge)
                        Spacer(Modifier.height(8.dp))
                        Text(state.error ?: "", style = MaterialTheme.typography.bodyMedium)
                    }
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        item {
                            ElevatedCard(
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                                ),
                                shape = MaterialTheme.shapes.large,
                            ) {
                                Column(Modifier.padding(20.dp)) {
                                    Text("AI Execution Score", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onPrimaryContainer)
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        "$executionScore / 100",
                                        style = MaterialTheme.typography.headlineSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    )
                                    Spacer(Modifier.height(6.dp))
                                    Text(insight, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
                                    Spacer(Modifier.height(10.dp))
                                    LinearProgressIndicator(
                                        progress = { executionScore / 100f },
                                        modifier = Modifier.fillMaxWidth(),
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        trackColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.22f),
                                    )
                                }
                            }
                        }
                        item {
                            PrioritiesCard(state.todayTasks)
                        }
                        item {
                            MentionedGoalsCard(
                                goals = state.weekGoals.map { it.title },
                                onOpenGoals = { onNavigate(Routes.Goals) },
                            )
                        }
                        item {
                            Text(
                                "Execution Workspace",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                        items(tiles.chunked(2)) { rowTiles ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                rowTiles.forEach { tile ->
                                    Box(modifier = Modifier.weight(1f)) {
                                        FeatureCard(tile) { onNavigate(tile.route) }
                                    }
                                }
                                if (rowTiles.size == 1) {
                                    Spacer(Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PrioritiesCard(tasks: List<com.execos.data.model.TaskItem>) {
    val total = tasks.size.coerceAtLeast(1)
    val done = tasks.count { it.completed }
    val rate = if (tasks.isEmpty()) 0f else done.toFloat() / total

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        ),
        shape = MaterialTheme.shapes.large,
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                "Today’s focus",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                if (tasks.isEmpty()) {
                    "What are the 3 things that will make today successful?"
                } else {
                    "$done of ${tasks.size} completed (${(rate * 100).toInt()}%) · Define what actually matters."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(12.dp))
            LinearProgressIndicator(
                progress = { rate },
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surface,
            )
        }
    }
}

@Composable
private fun MentionedGoalsCard(
    goals: List<String>,
    onOpenGoals: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.65f),
        ),
        shape = MaterialTheme.shapes.large,
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                "Daily Mentioned Goals",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "Keep these visible all day so your execution does not drift.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
            )
            Spacer(Modifier.height(10.dp))
            if (goals.isEmpty()) {
                Text(
                    "No weekly goals added yet. Add your top 3 and pin your direction.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                )
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                ) {
                    goals.take(5).forEach { goal ->
                        AssistChip(
                            onClick = onOpenGoals,
                            label = { Text(goal) },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = MaterialTheme.colorScheme.surface,
                                labelColor = MaterialTheme.colorScheme.onSurface,
                            ),
                        )
                        Spacer(Modifier.width(8.dp))
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            OutlinedButton(onClick = onOpenGoals) {
                Text("Manage goals")
            }
        }
    }
}

@Composable
private fun FeatureCard(tile: FeatureTile, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(132.dp),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(
            Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Icon(
                tile.icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp),
            )
            Text(
                tile.title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                tile.subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
            )
        }
    }
}
