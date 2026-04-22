package com.execos.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.execos.ui.theme.BottomBarBackground
import com.execos.ui.theme.BottomBarInactive
import com.execos.ui.theme.EarningsTextStyle
import com.execos.ui.theme.HighlightGradientEnd
import com.execos.ui.theme.HighlightGradientStart
import com.execos.ui.theme.PremiumElevatedSurface
import com.execos.ui.theme.PremiumEarningsGreen
import com.execos.ui.theme.PremiumRewardGold
import com.execos.ui.theme.PrimaryGradientEnd
import com.execos.ui.theme.PrimaryGradientStart

object ExecSpacing {
    val xs = 8.dp
    val md = 16.dp
    val lg = 24.dp
}

object ExecutiveFocusBrushes {
    val primaryCta: Brush
        @Composable get() = Brush.linearGradient(listOf(PrimaryGradientStart, PrimaryGradientEnd))

    val highlight: Brush
        @Composable get() = Brush.linearGradient(listOf(HighlightGradientStart, HighlightGradientEnd))
}

@Composable
fun PremiumGradientButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1f,
        animationSpec = tween(durationMillis = 180, easing = FastOutSlowInEasing),
        label = "premiumButtonScale",
    )

    Button(
        onClick = onClick,
        enabled = enabled,
        interactionSource = interactionSource,
        shape = MaterialTheme.shapes.medium,
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.Transparent,
            contentColor = Color.White,
            disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
        contentPadding = PaddingValues(
            horizontal = ExecSpacing.lg,
            vertical = ExecSpacing.md,
        ),
        modifier = modifier
            .scale(scale)
            .clip(MaterialTheme.shapes.medium)
            .background(ExecutiveFocusBrushes.primaryCta),
    ) {
        Text(text = text, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
fun PremiumSecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = MaterialTheme.colorScheme.onSurface,
            containerColor = Color.Transparent,
        ),
        contentPadding = PaddingValues(horizontal = ExecSpacing.lg, vertical = ExecSpacing.md),
    ) {
        Text(text = text, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
fun PremiumCard(
    modifier: Modifier = Modifier,
    shape: Shape = MaterialTheme.shapes.medium,
    content: @Composable BoxScope.() -> Unit,
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, PremiumElevatedSurface),
    ) {
        Box(modifier = Modifier.padding(ExecSpacing.md), content = content)
    }
}

@Composable
fun AnimatedPremiumCard(
    visible: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(180)) + slideInVertically(initialOffsetY = { it / 5 }, animationSpec = tween(180)),
    ) {
        PremiumCard(modifier = modifier, content = content)
    }
}

@Composable
fun PremiumGradientProgressBar(
    progress: Float,
    modifier: Modifier = Modifier,
    height: Dp = 8.dp,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(progress.coerceIn(0f, 1f))
                .height(height)
                .clip(CircleShape)
                .background(ExecutiveFocusBrushes.primaryCta),
        )
    }
}

@Composable
fun RewardBadge(
    text: String,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(PremiumRewardGold)
            .padding(horizontal = ExecSpacing.md, vertical = ExecSpacing.xs),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = Color(0xFF1F2937),
        )
    }
}

@Composable
fun PremiumGradientFab(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    FloatingActionButton(
        onClick = onClick,
        modifier = modifier
            .shadow(8.dp, CircleShape, clip = false)
            .clip(CircleShape)
            .background(ExecutiveFocusBrushes.primaryCta),
        containerColor = Color.Transparent,
        elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 8.dp),
        content = content,
    )
}

@Composable
fun ShimmerLoadingBar(
    modifier: Modifier = Modifier,
    shape: Shape = MaterialTheme.shapes.medium,
) {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val shift by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "shimmerShift",
    )
    val shimmerBrush = Brush.linearGradient(
        colors = listOf(
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f),
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        ),
        start = Offset(shift - 250f, 0f),
        end = Offset(shift, 0f),
    )
    Box(
        modifier = modifier
            .clip(shape)
            .background(shimmerBrush),
    )
}

@Composable
fun EarningsCountUpText(
    targetValue: Int,
    modifier: Modifier = Modifier,
    prefix: String = "$",
    style: TextStyle = EarningsTextStyle,
) {
    val animatedValue by animateIntAsState(
        targetValue = targetValue,
        animationSpec = spring(dampingRatio = 0.9f, stiffness = 280f),
        label = "earningsCountUp",
    )
    Text(
        text = "$prefix$animatedValue",
        modifier = modifier,
        style = style,
        color = PremiumEarningsGreen,
    )
}

object ExecutiveNavigationDefaults {
    val background = BottomBarBackground
    val inactive = BottomBarInactive
    val activeBrush: Brush
        @Composable get() = ExecutiveFocusBrushes.primaryCta
}
