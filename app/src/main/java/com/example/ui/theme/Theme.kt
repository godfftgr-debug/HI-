package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val TpmnDarkColorScheme = darkColorScheme(
    primary = GoldPrimary,
    onPrimary = NavyObsidian,
    primaryContainer = GoldContainer,
    onPrimaryContainer = GoldLight,
    secondary = EmeraldGlow,
    onSecondary = NavyObsidian,
    secondaryContainer = EmeraldDeep,
    onSecondaryContainer = TextPrimary,
    tertiary = GoldLight,
    onTertiary = NavyObsidian,
    background = NavyObsidian,
    onBackground = TextPrimary,
    surface = NavySurface,
    onSurface = TextPrimary,
    surfaceVariant = NavyElevated,
    onSurfaceVariant = TextSecondary,
    outline = NavyBorder,
    error = DangerCrimson,
    onError = TextPrimary,
    errorContainer = DangerContainer,
    onErrorContainer = TextPrimary
)

val TpmnShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp)
)

@Composable
fun TpmnBankTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = TpmnDarkColorScheme,
        typography = TpmnTypography,
        shapes = TpmnShapes,
        content = content
    )
}
