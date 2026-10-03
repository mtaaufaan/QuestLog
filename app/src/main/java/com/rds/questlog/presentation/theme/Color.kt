package com.rds.questlog.presentation.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/** Token di luar slot Material 3 (design.md §3.2). Diakses lewat [QuestLogTheme.colors]. */
@Immutable
data class QlColors(
    val accentText: Color,
    val accentLine: Color,
    val accentSoft: Color,
    val accentTint: Color,
    val accentTintActive: Color,
    val danger: Color,
    val dangerTint: Color,
    val divider: Color,
    val scrim: Color,
    val surface: Color,
    val textSecondary: Color,
    val textTertiary: Color,
    val snackbar: Color,
    val snackbarText: Color,
)

val LightQlColors = QlColors(
    accentText = Color(0xFF7D5411),
    accentLine = Color(0xFFB68235),
    accentSoft = Color(0xFFE1AD66),
    accentTint = Color(0xFFFFF3E4),
    accentTintActive = Color(0xFFFFE3BF),
    danger = Color(0xFF9E3B2C),
    dangerTint = Color(0xFFF6E6E2),
    divider = Color(0xFF201F1D).copy(alpha = 0.16f),
    scrim = Color(0xFF141312).copy(alpha = 0.45f),
    surface = Color(0xFFF3F2F2),
    textSecondary = Color(0xFF605D5D),
    textTertiary = Color(0xFF7D7979),
    snackbar = Color(0xFF2D2B2B),
    snackbarText = Color(0xFFF3F2F2),
)

// ponytail: tint/active/tertiary di dark mode adalah estimasi (design.md hanya mendefinisikan 6 token gelap).
val DarkQlColors = QlColors(
    accentText = Color(0xFFE1AD66),
    accentLine = Color(0xFFB68235),
    accentSoft = Color(0xFFE1AD66),
    accentTint = Color(0xFF3A2F1E),
    accentTintActive = Color(0xFF4A3A22),
    danger = Color(0xFFE38A78),
    dangerTint = Color(0xFF3A2420),
    divider = Color(0xFFE8E3DC).copy(alpha = 0.16f),
    scrim = Color(0xFF141312).copy(alpha = 0.45f),
    surface = Color(0xFF282624),
    textSecondary = Color(0xFFA8A29A),
    textTertiary = Color(0xFF7D7770),
    snackbar = Color(0xFF2D2B2B),
    snackbarText = Color(0xFFF3F2F2),
)

val LightColorScheme: ColorScheme = lightColorScheme(
    primary = Color(0xFF7D5411),
    onPrimary = Color(0xFFF3F2F2),
    background = Color(0xFFF3F2F2),
    onBackground = Color(0xFF201F1D),
    surface = Color(0xFFF3F2F2),
    onSurface = Color(0xFF201F1D),
    onSurfaceVariant = Color(0xFF605D5D),
    outline = Color(0xFFB68235),
    error = Color(0xFF9E3B2C),
)

val DarkColorScheme: ColorScheme = darkColorScheme(
    primary = Color(0xFFE1AD66),
    onPrimary = Color(0xFF1D1C1A),
    background = Color(0xFF1D1C1A),
    onBackground = Color(0xFFE8E3DC),
    surface = Color(0xFF22211F),
    onSurface = Color(0xFFE8E3DC),
    onSurfaceVariant = Color(0xFFA8A29A),
    outline = Color(0xFFB68235),
    error = Color(0xFFE38A78),
)
