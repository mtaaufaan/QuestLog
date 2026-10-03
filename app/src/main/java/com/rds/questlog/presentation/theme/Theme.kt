package com.rds.questlog.presentation.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LocalQlColors = staticCompositionLocalOf { LightQlColors }

object QuestLogTheme {
    val colors: QlColors
        @Composable @ReadOnlyComposable
        get() = LocalQlColors.current
}

@Composable
fun QuestLogTheme(darkTheme: Boolean = false, content: @Composable () -> Unit) {
    SystemBarIcons(darkTheme)
    CompositionLocalProvider(LocalQlColors provides if (darkTheme) DarkQlColors else LightQlColors) {
        MaterialTheme(
            colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
            typography = QlTypography,
            content = content,
        )
    }
}

/**
 * enableEdgeToEdge() memilih warna ikon status/navigation bar dari tema sistem, bukan tema app.
 * Di sini ikon mengikuti [darkTheme]; nilai sebelumnya dipulihkan saat tema ini keluar dari komposisi
 * (mis. Reader gelap ditutup, kembali ke layar terang).
 */
@Composable
private fun SystemBarIcons(darkTheme: Boolean) {
    val view = LocalView.current
    if (view.isInEditMode) return
    DisposableEffect(view, darkTheme) {
        val window = (view.context as Activity).window
        val controller = WindowCompat.getInsetsController(window, view)
        val prevStatus = controller.isAppearanceLightStatusBars
        val prevNav = controller.isAppearanceLightNavigationBars
        controller.isAppearanceLightStatusBars = !darkTheme
        controller.isAppearanceLightNavigationBars = !darkTheme
        onDispose {
            controller.isAppearanceLightStatusBars = prevStatus
            controller.isAppearanceLightNavigationBars = prevNav
        }
    }
}
