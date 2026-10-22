package com.aura.app.core.designsystem

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = SageBrand,
    onPrimary = SoftIvory,
    primaryContainer = SoftIvory,
    onPrimaryContainer = DeepBotanical,
    secondary = StoneNeutral,
    onSecondary = CharcoalText,
    tertiary = MutedAmber,
    onTertiary = SoftIvory,
    background = Porcelain,
    onBackground = CharcoalText,
    surface = SoftIvory,
    onSurface = CharcoalText,
    surfaceVariant = Porcelain,
    onSurfaceVariant = SecondaryText,
    outline = BorderDivider
)

private val DarkColorScheme = darkColorScheme(
    primary = DarkPrimary,
    onPrimary = DarkBackground,
    primaryContainer = DarkSurface,
    onPrimaryContainer = DarkText,
    secondary = StoneNeutral,
    onSecondary = DarkText,
    tertiary = MutedAmber,
    onTertiary = DarkBackground,
    background = DarkBackground,
    onBackground = DarkText,
    surface = DarkSurface,
    onSurface = DarkText,
    surfaceVariant = DarkBackground,
    onSurfaceVariant = DarkSecondaryText,
    outline = DarkBorder
)

data class AuraExtendedColors(
    val deepBotanical: Color,
    val mutedAmber: Color,
    val borderDivider: Color,
    val glowCyan: Color,
    val glowPurple: Color
)

val LocalAuraExtendedColors = staticCompositionLocalOf {
    AuraExtendedColors(
        deepBotanical = DeepBotanical,
        mutedAmber = MutedAmber,
        borderDivider = BorderDivider,
        glowCyan = GlowCyan,
        glowPurple = GlowPurple
    )
}

@Composable
fun AuraTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val extendedColors = AuraExtendedColors(
        deepBotanical = if (darkTheme) DarkPrimary else DeepBotanical,
        mutedAmber = MutedAmber,
        borderDivider = if (darkTheme) DarkBorder else BorderDivider,
        glowCyan = GlowCyan,
        glowPurple = GlowPurple
    )

    CompositionLocalProvider(LocalAuraExtendedColors provides extendedColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = AuraTypography,
            shapes = AuraShapes,
            content = content
        )
    }
}
