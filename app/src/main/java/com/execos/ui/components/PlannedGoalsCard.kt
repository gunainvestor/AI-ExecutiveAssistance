package com.execos.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.execos.ui.theme.PrimaryGradientEnd
import com.execos.ui.theme.PrimaryGradientStart

@Immutable
data class PlannedGoalsHorizon(
    val label: String,
    val items: List<String>,
)

@Composable
fun PlannedGoalsCard(
    horizons: List<PlannedGoalsHorizon>,
    modifier: Modifier = Modifier,
) {
    val normalized = remember(horizons) { normalizeAndOrderHorizons(horizons) }
    if (normalized.isEmpty()) return

    val flipStates = remember { mutableStateMapOf<String, Boolean>() }
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(
            Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                "Planned goals",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                "Tap any card to flip details",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                normalized.forEach { horizon ->
                    val isFlipped = flipStates[horizon.label] == true
                    FlippableGoalCard(
                        horizon = horizon,
                        isFlipped = isFlipped,
                        onToggleFlip = { flipStates[horizon.label] = !isFlipped },
                    )
                }
            }
        }
    }
}

@Composable
private fun FlippableGoalCard(
    horizon: PlannedGoalsHorizon,
    isFlipped: Boolean,
    onToggleFlip: () -> Unit,
) {
    val rotationY by animateFloatAsState(
        targetValue = if (isFlipped) 180f else 0f,
        animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing),
        label = "goalCardFlip",
    )
    val frontAlpha by animateFloatAsState(
        targetValue = if (rotationY <= 90f) 1f else 0f,
        animationSpec = tween(durationMillis = 120),
        label = "goalCardFrontAlpha",
    )
    val backAlpha by animateFloatAsState(
        targetValue = if (rotationY > 90f) 1f else 0f,
        animationSpec = tween(durationMillis = 120),
        label = "goalCardBackAlpha",
    )
    val gradient = Brush.linearGradient(listOf(PrimaryGradientStart, PrimaryGradientEnd))

    Card(
        onClick = onToggleFlip,
        modifier = Modifier
            .fillMaxWidth(0.85f)
            .height(180.dp)
            .graphicsLayer {
                this.rotationY = rotationY
                cameraDistance = 12 * density
            },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Box(Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .alpha(frontAlpha)
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        normalizeLabel(horizon.label),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        if (horizon.items.isEmpty()) "0/3" else "${horizon.items.size}/3",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Spacer(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .background(gradient, RoundedCornerShape(999.dp)),
                )
                Text(
                    if (horizon.items.isEmpty()) "No goals set yet." else horizon.items.first(),
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    if (horizon.items.isEmpty()) "Create in Goals screen" else "Tap to view all goals",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer { this.rotationY = 180f }
                    .alpha(backAlpha)
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    "${normalizeLabel(horizon.label)} Goals",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                )
                if (horizon.items.isEmpty()) {
                    Text(
                        "No goals set yet. Add up to 3 priorities.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    horizon.items.take(3).forEachIndexed { index, goal ->
                        Text(
                            "${index + 1}. $goal",
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                Text(
                    "Tap to flip back",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

private fun normalizeAndOrderHorizons(horizons: List<PlannedGoalsHorizon>): List<PlannedGoalsHorizon> {
    val orderedLabels = listOf("Quarter", "Yearly", "Month", "Week")
    val byLabel = horizons.associateBy { normalizeLabel(it.label) }
    val ordered = orderedLabels.mapNotNull { byLabel[it] }
    val extras = horizons.filter { normalizeLabel(it.label) !in orderedLabels }
    return ordered + extras
}

private fun normalizeLabel(input: String): String {
    val key = input.trim().lowercase()
    return when (key) {
        "quarter", "quater" -> "Quarter"
        "year", "yearly", "yesrl" -> "Yearly"
        "month", "monthly" -> "Month"
        "week", "weekly" -> "Week"
        else -> input.trim().replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
    }
}


