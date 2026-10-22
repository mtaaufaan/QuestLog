package com.rds.questlog.presentation.theme

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import com.rds.questlog.domain.model.ReaderTheme

/** Lama transisi warna saat Mode Baca diganti. */
private const val THEME_TRANSITION_MS = 250
private const val ACTIVE_TINT_MIX = 0.25f
private const val PRESSED_MIX = 0.06f
private const val SCRIM_ALPHA = 0.45f

/**
 * Token warna Mode Baca (design.md §3.2): dipakai Reader (S3), Pratinjau Bookmark (P20), dan Display Settings Sheet.
 * [bg] latar baca, [shell] pelat/bar, [bar] header/footer P20, [sheet] latar sheet dan popup, [fg]/[muted]/[faint]
 * teks primer/sekunder/tersier, [rule] garis pembatas, [accent] teks beraksen, [accentStroke] garis/ikon beraksen,
 * [tint] latar tersorot, [surface] blok kode/gambar, [snackBg]/[snackFg]/[snackAction] snackbar, [track] jalur
 * progres, [handle] pegangan sheet.
 */
@Immutable
data class ReaderColors(
    val bg: Color,
    val shell: Color,
    val bar: Color,
    val sheet: Color,
    val fg: Color,
    val muted: Color,
    val faint: Color,
    val rule: Color,
    val accent: Color,
    val accentStroke: Color,
    val tint: Color,
    val surface: Color,
    val danger: Color,
    val dangerTint: Color,
    val snackBg: Color,
    val snackFg: Color,
    val snackAction: Color,
    val track: Color,
    val handle: Color,
)

val LightReaderColors = ReaderColors(
    bg = Color(0xFFF3F2F2),
    shell = Color(0xFFEAE9E9),
    bar = Color(0xFFEAE9E9),
    sheet = Color(0xFFF3F2F2),
    fg = Color(0xFF201F1D),
    muted = Color(0xFF605D5D),
    faint = Color(0xFF7D7979),
    rule = Color(0xFF201F1D).copy(alpha = 0.16f),
    accent = Color(0xFF7D5411),
    accentStroke = Color(0xFFB68235),
    tint = Color(0xFFFFF3E4),
    surface = Color(0xFFEAE9E9),
    danger = Color(0xFF9E3B2C),
    dangerTint = Color(0xFFF8ECEA),
    snackBg = Color(0xFF2D2B2B),
    snackFg = Color(0xFFF3F2F2),
    snackAction = Color(0xFFE1AD66),
    track = Color(0xFFD7D3D3),
    handle = Color(0xFFBAB6B6),
)

val DarkReaderColors = ReaderColors(
    bg = Color(0xFF1D1C1A),
    shell = Color(0xFF262422),
    bar = Color(0xFF262422),
    sheet = Color(0xFF22211F),
    fg = Color(0xFFE8E3DC),
    muted = Color(0xFFA8A29A),
    faint = Color(0xFF837D76),
    rule = Color.White.copy(alpha = 0.13f),
    accent = Color(0xFFE1AD66),
    accentStroke = Color(0xFFE1AD66),
    tint = Color(0xFFE1AD66).copy(alpha = 0.14f),
    surface = Color(0xFF282624),
    danger = Color(0xFFE38A78),
    dangerTint = Color(0xFFE38A78).copy(alpha = 0.08f),
    snackBg = Color(0xFFE8E3DC),
    snackFg = Color(0xFF1D1C1A),
    snackAction = Color(0xFF7D5411),
    track = Color.White.copy(alpha = 0.14f),
    handle = Color.White.copy(alpha = 0.25f),
)

val SepiaReaderColors = ReaderColors(
    bg = Color(0xFFFBF0D9),
    shell = Color(0xFFF2E5CC),
    bar = Color(0xFFF2E5CC),
    sheet = Color(0xFFFBF0D9),
    fg = Color(0xFF2C261E),
    muted = Color(0xFF736553),
    faint = Color(0xFF9C8C76),
    rule = Color(0xFF2C261E).copy(alpha = 0.15f),
    accent = Color(0xFF8C5D28),
    accentStroke = Color(0xFF8C5D28),
    tint = Color(0xFFF3E2C6),
    surface = Color(0xFFF4E8D1),
    danger = Color(0xFF9A3A26),
    dangerTint = Color(0xFFF6E2CF),
    snackBg = Color(0xFF2C261E),
    snackFg = Color(0xFFFBF0D9),
    snackAction = Color(0xFFE1AD66),
    track = Color(0xFFE2D2B4),
    handle = Color(0xFFC9B89B),
)

/** Muted memakai dua aksen: [ReaderColors.accentStroke] hanya untuk garis/ikon (kontras 3,3:1), teks memakai accent. */
val MutedReaderColors = ReaderColors(
    bg = Color(0xFFE4E4E6),
    shell = Color(0xFFD1D1D6),
    bar = Color(0xFFD9D9DD),
    sheet = Color(0xFFE4E4E6),
    fg = Color(0xFF3A3A3C),
    muted = Color(0xFF636366),
    faint = Color(0xFF8E8E93),
    rule = Color(0xFF3A3A3C).copy(alpha = 0.14f),
    accent = Color(0xFF7A5A26),
    accentStroke = Color(0xFF9E7B44),
    tint = Color(0xFFD8D8DC),
    surface = Color(0xFFECECEE),
    danger = Color(0xFF97392B),
    dangerTint = Color(0xFFE9DDDB),
    snackBg = Color(0xFF3A3A3C),
    snackFg = Color(0xFFE4E4E6),
    snackAction = Color(0xFFE1AD66),
    track = Color(0xFFC7C7CC),
    handle = Color(0xFFAEAEB2),
)

/** Token warna untuk [theme]. */
fun readerColorsFor(theme: ReaderTheme): ReaderColors = when (theme) {
    ReaderTheme.LIGHT -> LightReaderColors
    ReaderTheme.DARK -> DarkReaderColors
    ReaderTheme.SEPIA -> SepiaReaderColors
    ReaderTheme.MUTED -> MutedReaderColors
}

/** Token Mode Baca yang sedang berlaku (sudah beranimasi saat tema diganti). */
val LocalReaderColors = compositionLocalOf { LightReaderColors }

/** Pemetaan ke token umum yang dibaca komponen bersama, sehingga seluruh komponen Reader ikut Mode Baca. */
private fun ReaderColors.toQlColors() = QlColors(
    accentText = accent,
    accentLine = accentStroke,
    accentSoft = snackAction,
    accentTint = tint,
    accentTintActive = lerp(tint, accentStroke, ACTIVE_TINT_MIX),
    danger = danger,
    dangerTint = dangerTint,
    divider = rule,
    scrim = Color(0xFF141312).copy(alpha = SCRIM_ALPHA),
    surface = sheet,
    textSecondary = muted,
    textTertiary = faint,
    snackbar = snackBg,
    snackbarText = snackFg,
    hover = surface,
    pressed = lerp(surface, fg, PRESSED_MIX),
    track = track,
    iconMuted = faint,
)

private fun ReaderColors.toColorScheme(dark: Boolean): ColorScheme {
    val base = if (dark) darkColorScheme() else lightColorScheme()
    return base.copy(
        primary = accent,
        onPrimary = bg,
        background = bg,
        onBackground = fg,
        surface = sheet,
        onSurface = fg,
        onSurfaceVariant = muted,
        outline = accentStroke,
        error = danger,
    )
}

@Composable
private fun animated(color: Color): Color =
    animateColorAsState(color, tween(THEME_TRANSITION_MS), label = "readerColor").value

@Composable
private fun ReaderColors.animated() = ReaderColors(
    bg = animated(bg),
    shell = animated(shell),
    bar = animated(bar),
    sheet = animated(sheet),
    fg = animated(fg),
    muted = animated(muted),
    faint = animated(faint),
    rule = animated(rule),
    accent = animated(accent),
    accentStroke = animated(accentStroke),
    tint = animated(tint),
    surface = animated(surface),
    danger = animated(danger),
    dangerTint = animated(dangerTint),
    snackBg = animated(snackBg),
    snackFg = animated(snackFg),
    snackAction = animated(snackAction),
    track = animated(track),
    handle = animated(handle),
)

/**
 * Menyediakan token Mode Baca ([LocalReaderColors]), token umum, dan MaterialTheme untuk [theme]; warna beralih
 * 250 ms tanpa menyentuh posisi scroll. Tanpa ikon system bar: dipakai di dalam window sheet/dialog.
 */
@Composable
fun ReaderColorsScope(theme: ReaderTheme, content: @Composable () -> Unit) {
    val colors = readerColorsFor(theme).animated()
    CompositionLocalProvider(LocalReaderColors provides colors, LocalQlColors provides colors.toQlColors()) {
        MaterialTheme(
            colorScheme = colors.toColorScheme(dark = theme == ReaderTheme.DARK),
            typography = QlTypography,
            content = content,
        )
    }
}

/** [ReaderColorsScope] ditambah ikon system bar yang mengikuti [theme] (hanya Dark yang berikon terang). */
@Composable
fun ReaderThemeScope(theme: ReaderTheme, content: @Composable () -> Unit) {
    SystemBarIcons(theme == ReaderTheme.DARK)
    ReaderColorsScope(theme, content)
}
