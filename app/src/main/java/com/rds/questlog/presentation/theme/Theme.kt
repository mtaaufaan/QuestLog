package com.rds.questlog.presentation.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf

private val LocalQlColors = staticCompositionLocalOf { LightQlColors }

object QuestLogTheme {
    val colors: QlColors
        @Composable @ReadOnlyComposable get() = LocalQlColors.current
}

@Composable
fun QuestLogTheme(darkTheme: Boolean = false, content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalQlColors provides if (darkTheme) DarkQlColors else LightQlColors) {
        MaterialTheme(
            colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
            typography = QlTypography,
            content = content,
        )
    }
}
