package com.execos.ui.usage

import android.content.Intent
import android.provider.Settings
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.execos.util.Dates
import kotlinx.coroutines.isActive
import com.execos.data.usage.AppUsageItem
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UsageScreen(
    viewModel: UsageViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val launcher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
    ) {
        viewModel.refresh()
    }

    LaunchedEffect(Unit) {
        viewModel.refresh()
    }

    LaunchedEffect(Unit) {
        while (isActive) {
            delay(Dates.millisUntilNextLocalMidnight())
            viewModel.refresh()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Device usage", fontWeight = FontWeight.SemiBold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface),
            )
        },
    ) { padding ->
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
                ) {
                    Text(state.error ?: "", style = MaterialTheme.typography.bodyLarge)
                    Spacer(Modifier.height(12.dp))
                    Button(onClick = { viewModel.refresh() }) {
                        Text("Retry")
                    }
                }
            }
            !state.hasAccess -> {
                Column(
                    Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        "To show WhatsApp/social app usage (daily + weekly), Android requires you to grant Usage Access.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Button(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            launcher.launch(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
                        },
                    ) {
                        Text("Grant Usage Access")
                    }
                    Text(
                        "After granting, come back here — we’ll automatically refresh.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    item {
                        Spacer(Modifier.height(8.dp))
                        val totalToday = state.items.sumOf { it.minutesToday }
                        val totalWeek = state.items.sumOf { it.minutesThisWeek }
                        UsageHeroCard(
                            minutesToday = totalToday,
                            minutesThisWeek = totalWeek,
                            aiEmojiLine = extractEmojiLine(state.aiText),
                        )
                        Spacer(Modifier.height(10.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                            Button(
                                modifier = Modifier.weight(1f),
                                onClick = { viewModel.refresh() },
                            ) { Text("Refresh") }
                            Button(
                                modifier = Modifier.weight(1f),
                                onClick = { scope.launch { viewModel.generateAiCoach() } },
                                enabled = !state.aiBusy,
                            ) { Text(if (state.aiBusy) "Thinking…" else "AI coach") }
                        }
                        Spacer(Modifier.height(4.dp))

                        AnimatedVisibility(
                            visible = state.aiPlan != null || state.aiText.isNotBlank(),
                            enter = fadeIn(animationSpec = tween(250)),
                            exit = fadeOut(animationSpec = tween(150)),
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(18.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
                                    .padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                val plan = state.aiPlan
                                if (plan != null) {
                                    AiPlanGraphic(plan)
                                } else {
                                    // Fallback if JSON parse fails.
                                    Text(
                                        state.aiText.trim(),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface,
                                    )
                                }
                            }
                        }
                    }
                    items(state.items, key = { it.packageName }) { item ->
                        UsageRow(item)
                    }
                    item { Spacer(Modifier.height(72.dp)) }
                }
            }
        }
    }
}

@Composable
private fun UsageHeroCard(
    minutesToday: Int,
    minutesThisWeek: Int,
    aiEmojiLine: String?,
) {
    val target = minutesToday.coerceAtLeast(0)
    val animatedMinutes = remember(target) { Animatable(0f) }
    LaunchedEffect(target) {
        animatedMinutes.snapTo(0f)
        animatedMinutes.animateTo(
            targetValue = target.toFloat(),
            animationSpec = tween(durationMillis = 650),
        )
    }

    val emoji = when {
        target <= 10 -> "🙂"
        target <= 45 -> "😕"
        target <= 90 -> "😬"
        target <= 150 -> "😱"
        else -> "🫠"
    }

    val pulse = rememberInfiniteTransition(label = "pulse")
        .animateFloat(
            initialValue = 1f,
            targetValue = 1.06f,
            animationSpec = infiniteRepeatable(
                animation = tween(900, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse,
            ),
            label = "pulseScale",
        ).value

    val todayRatio by animateFloatAsState(
        targetValue = (minutesToday / 180f).coerceIn(0f, 1f), // 3h cap for the ring
        animationSpec = tween(650, easing = FastOutSlowInEasing),
        label = "todayRatio",
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(MaterialTheme.colorScheme.primaryContainer)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.10f)),
                contentAlignment = Alignment.Center,
            ) {
                Text(emoji, modifier = Modifier.scale(pulse), style = MaterialTheme.typography.headlineMedium)
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Time check",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
                Text(
                    "${animatedMinutes.value.toInt()} min today · $minutesThisWeek min this week",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(54.dp)) {
                CircularProgressIndicator(
                    progress = { todayRatio },
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    trackColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.18f),
                    strokeWidth = 6.dp,
                )
            }
        }

        Text(
            "Social + browsing time (from Usage Access). No shame — just signal.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f),
        )

        if (!aiEmojiLine.isNullOrBlank()) {
            Text(
                aiEmojiLine.trim(),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        }
    }
}

@Composable
private fun UsageRow(item: AppUsageItem) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
    ) {
        Text(item.appLabel, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(2.dp))
        Text(
            "${item.minutesToday} min today · ${item.minutesThisWeek} min this week",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private fun extractEmojiLine(aiText: String): String? {
    if (aiText.isBlank()) return null
    val lines = aiText.lines().map { it.trim() }.filter { it.isNotBlank() }
    // Expecting "4) ..." but be resilient.
    return lines.firstOrNull { it.startsWith("4)") }?.removePrefix("4)")?.trim()
        ?: lines.lastOrNull { it.any { ch -> ch.code > 0x1F000 } }
}

@Composable
private fun AiPlanGraphic(plan: UsageAiPlan) {
    Text(
        plan.emojiHeadline.ifBlank { "✨" },
        style = MaterialTheme.typography.titleLarge,
    )
    if (plan.realityCheck.isNotBlank()) {
        Text(
            plan.realityCheck,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 2,
        )
    }
    if (plan.focusGoal.isNotBlank()) {
        Spacer(Modifier.height(6.dp))
        Text(
            "🎯 ${plan.focusHorizon.ifBlank { "Goal" }} · ${plan.focusGoal}",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
        )
    }
    Spacer(Modifier.height(10.dp))

    Text("Instead of scrolling", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
    Spacer(Modifier.height(6.dp))
    SuggestionBars(plan.timeCouldHaveBeen)
    Spacer(Modifier.height(10.dp))
    HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f))
    Spacer(Modifier.height(10.dp))

    Text("Next 2 hours", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
    Spacer(Modifier.height(6.dp))
    SuggestionBars(plan.next2Hours)
}

@Composable
private fun SuggestionBars(items: List<UsageAiItem>) {
    val safe = items.filter { it.title.isNotBlank() }.take(3)
    val total = safe.sumOf { it.minutes.coerceAtLeast(0) }.coerceAtLeast(1)

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        safe.forEach { item ->
            val ratio = (item.minutes.coerceAtLeast(0) / total.toFloat()).coerceIn(0f, 1f)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(item.emoji.ifBlank { "✅" })
                }
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(item.title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                        Text("${item.minutes}m", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Spacer(Modifier.height(6.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(10.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f)),
                    ) {
                        Box(
                            modifier = Modifier
                                .width((ratio * 1_000).dp) // capped by parent width
                                .fillMaxWidth(ratio)
                                .height(10.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(MaterialTheme.colorScheme.primary),
                        )
                    }
                }
            }
        }
    }
}

