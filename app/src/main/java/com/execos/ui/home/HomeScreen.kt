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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.execos.ui.components.ExecGradientBackground
import com.execos.ui.navigation.Routes

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
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val feedback by dailyFeedbackViewModel.popup.collectAsStateWithLifecycle()
    val tiles = listOf(
        FeatureTile(Routes.Focus, "Top 3", "Plan today’s impact", Icons.Default.Today),
        FeatureTile(Routes.Decisions, "Decisions", "Context & confidence", Icons.Default.Psychology),
        FeatureTile(Routes.Reflection, "Reflection", "AI review of your day", Icons.Default.CheckCircle),
        FeatureTile(Routes.Weekly, "Weekly review", "Patterns & growth", Icons.Default.TrackChanges),
        FeatureTile(Routes.Calendar, "Calendar", "See your month at a glance", Icons.Default.DateRange),
        FeatureTile(Routes.Goals, "Goals", "Year/quarter/month/week top 3", Icons.Default.TrackChanges),
        FeatureTile(Routes.Usage, "Usage", "See time-wasters (daily/weekly)", Icons.Default.TrackChanges),
        FeatureTile(Routes.Energy, "Energy", "Morning & evening", Icons.Default.Bolt),
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "ExecOS",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            "Executive operating system",
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
            )
        },
    ) { padding ->
        ExecGradientBackground {
            if (feedback != null) {
                val f = feedback!!
                AlertDialog(
                    onDismissRequest = { dailyFeedbackViewModel.markShown(f) },
                    title = { Text("Daily feedback") },
                    text = { Text(f.text) },
                    confirmButton = {
                        Button(
                            onClick = {
                                dailyFeedbackViewModel.markShown(f)
                                onNavigate(Routes.Reflection)
                            },
                        ) {
                            Text("Open reflection")
                        }
                    },
                    dismissButton = {
                        OutlinedButton(onClick = { dailyFeedbackViewModel.markShown(f) }) {
                            Text("Dismiss")
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
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                                ),
                                shape = MaterialTheme.shapes.large,
                            ) {
                                Column(Modifier.padding(20.dp)) {
                                    Text(
                                        "Today",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    )
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        "Start with priorities, then log decisions and a quick reflection.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    )
                                }
                            }
                        }
                        item {
                            PrioritiesCard(state.todayTasks)
                        }
                        item {
                            Text(
                                "Workspace",
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
                    "No priorities yet — open Today to add up to three."
                } else {
                    "$done of ${tasks.size} completed (${(rate * 100).toInt()}%)"
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
