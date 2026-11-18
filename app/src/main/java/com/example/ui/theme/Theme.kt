package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val SmartMoneyDarkColorScheme = darkColorScheme(
    primary = EmeraldPrimary,
    onPrimary = NavyDeep,
    primaryContainer = EmeraldContainer,
    onPrimaryContainer = EmeraldBright,
    secondary = InfoCyan,
    onSecondary = NavyDeep,
    secondaryContainer = NavySurfaceElevated,
    onSecondaryContainer = CrispWhite,
    tertiary = PendingAmber,
    onTertiary = NavyDeep,
    tertiaryContainer = PendingContainer,
    onTertiaryContainer = PendingAmber,
    error = ExpenseCoral,
    onError = CrispWhite,
    errorContainer = ExpenseContainer,
    onErrorContainer = ExpenseCoral,
    background = NavyDeep,
    onBackground = CrispWhite,
    surface = NavyPrimary,
    onSurface = CrispWhite,
    surfaceVariant = NavySurface,
    onSurfaceVariant = MutedSlate,
    surfaceContainerLowest = NavyDeep,
    surfaceContainerLow = NavyPrimary,
    surfaceContainer = NavySurface,
    surfaceContainerHigh = NavySurfaceElevated,
    outline = NavyBorder,
    outlineVariant = NavySurfaceElevated
)

val SmartMoneyShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun SmartMoneyTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = SmartMoneyDarkColorScheme,
        typography = SmartMoneyTypography,
        shapes = SmartMoneyShapes,
        content = content
    )
}
