package com.execos.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val LightScheme = lightColorScheme(
    primary = InkPrimary,
    onPrimary = OnInkPrimary,
    primaryContainer = InkPrimaryContainer,
    onPrimaryContainer = OnInkPrimaryContainer,
    secondary = SlateSecondary,
    onSecondary = OnSlateSecondary,
    secondaryContainer = SlateSecondaryContainer,
    onSecondaryContainer = OnSlateSecondaryContainer,
    tertiary = NeutralTertiary,
    onTertiary = OnNeutralTertiary,
    tertiaryContainer = NeutralTertiaryContainer,
    onTertiaryContainer = OnNeutralTertiaryContainer,
    background = LightBackground,
    onBackground = LightOnBackground,
    surface = LightSurface,
    onSurface = LightOnSurface,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightOnSurfaceVariant,
    outline = LightOutline,
    outlineVariant = LightOutlineVariant,
    error = LightError,
    onError = LightOnError,
    errorContainer = LightErrorContainer,
    onErrorContainer = LightOnErrorContainer,
)

private val DarkScheme = darkColorScheme(
    primary = SilverBluePrimary,
    onPrimary = OnSilverBluePrimary,
    primaryContainer = SilverBluePrimaryContainer,
    onPrimaryContainer = OnSilverBluePrimaryContainer,
    secondary = SteelSecondary,
    onSecondary = OnSteelSecondary,
    secondaryContainer = SteelSecondaryContainer,
    onSecondaryContainer = OnSteelSecondaryContainer,
    tertiary = DarkTertiary,
    onTertiary = OnDarkTertiary,
    tertiaryContainer = DarkTertiaryContainer,
    onTertiaryContainer = OnDarkTertiaryContainer,
    background = DarkBackground,
    onBackground = DarkOnBackground,
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkOnSurfaceVariant,
    outline = DarkOutline,
    outlineVariant = DarkOutlineVariant,
    error = DarkError,
    onError = DarkOnError,
    errorContainer = DarkErrorContainer,
    onErrorContainer = DarkOnErrorContainer,
)

@Composable
fun ExecOsTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkScheme
        else -> LightScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = ExecTypography,
        shapes = ExecShapes,
        content = content,
    )
}
