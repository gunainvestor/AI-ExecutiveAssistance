package com.execos.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.lerp
import androidx.compose.material3.MaterialTheme

@Composable
fun ExecGradientBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val dark = isSystemInDarkTheme()

    val base = scheme.background

    // Premium pastel: lean on container tones + blend toward the base for a soft “misty” look.
    val pastel1 = lerp(if (dark) scheme.primary else scheme.primaryContainer, base, if (dark) 0.70f else 0.40f)
        .copy(alpha = if (dark) 0.22f else 0.70f)
    val pastel2 = lerp(if (dark) scheme.secondary else scheme.secondaryContainer, base, if (dark) 0.74f else 0.45f)
        .copy(alpha = if (dark) 0.18f else 0.62f)
    val pastel3 = lerp(if (dark) scheme.tertiary else scheme.tertiaryContainer, base, if (dark) 0.78f else 0.50f)
        .copy(alpha = if (dark) 0.14f else 0.55f)

    val wash = Brush.linearGradient(
        colors = listOf(
            pastel1,
            pastel2,
            pastel3,
        ),
        tileMode = TileMode.Clamp,
    )

    val bloom = Brush.radialGradient(
        colors = listOf(
            pastel1.copy(alpha = pastel1.alpha * 0.9f),
            pastel2.copy(alpha = pastel2.alpha * 0.7f),
            Color.Transparent,
        ),
        center = androidx.compose.ui.geometry.Offset.Unspecified,
        radius = if (dark) 1200f else 1500f,
        tileMode = TileMode.Clamp,
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(base)
            .background(wash)
            .background(bloom),
        content = content,
    )
}

