package com.rds.questlog.presentation.displaysettings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rds.questlog.R
import com.rds.questlog.domain.model.ReaderTheme
import com.rds.questlog.presentation.components.QlBottomSheet
import com.rds.questlog.presentation.theme.CormorantGaramond
import com.rds.questlog.presentation.theme.Lora
import com.rds.questlog.presentation.theme.QuestLogTheme
import com.rds.questlog.presentation.theme.ReaderColorsScope
import com.rds.questlog.presentation.theme.readerColorsFor

const val MIN_FONT_SIZE = 12
const val MAX_FONT_SIZE = 24
const val FONT_SIZE_STEP = 2
const val DEFAULT_FONT_SIZE = 16

private val SWATCH_HEIGHT = 56.dp
private val SWATCH_RADIUS = 6.dp

/**
 * Display Settings Sheet (component-contract.md §11): ukuran teks 12–24sp (step 2) dan Mode Baca (Light · Dark ·
 * Sepia · Muted). Berlaku app-wide; sheet langsung berganti warna mengikuti [theme] yang dipilih.
 */
@Composable
fun DisplaySettingsSheet(
    fontSize: Int,
    theme: ReaderTheme,
    onFontSizeChange: (Int) -> Unit,
    onThemeChange: (ReaderTheme) -> Unit,
    onClose: () -> Unit,
) {
    // Sheet berada di window sendiri, jadi Mode Baca Reader harus diteruskan secara eksplisit.
    ReaderColorsScope(theme) {
        QlBottomSheet(onDismiss = onClose) {
            SheetBody(fontSize, theme, onFontSizeChange, onThemeChange)
        }
    }
}

@Composable
private fun SheetBody(
    fontSize: Int,
    theme: ReaderTheme,
    onFontSizeChange: (Int) -> Unit,
    onThemeChange: (ReaderTheme) -> Unit,
) {
    val c = QuestLogTheme.colors
    Column(Modifier.navigationBarsPadding().padding(start = 24.dp, end = 24.dp, bottom = 30.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 18.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom,
        ) {
            Text(
                text = stringResource(R.string.display_settings_title),
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.titleLarge.copy(fontFamily = CormorantGaramond, fontSize = 26.sp),
            )
            Text(
                text = stringResource(R.string.display_settings_scope).uppercase(),
                color = c.textSecondary,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 1.26.sp,
                ),
            )
        }
        HorizontalDivider(color = c.divider)
        FontSizeSection(fontSize, onFontSizeChange)
        HorizontalDivider(color = c.divider)
        ThemeSection(theme, onThemeChange)
    }
}

@Composable
private fun FontSizeSection(fontSize: Int, onFontSizeChange: (Int) -> Unit) {
    val c = QuestLogTheme.colors
    Column(Modifier.padding(vertical = 18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    stringResource(R.string.display_settings_font_size),
                    color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.titleMedium.copy(fontSize = 15.sp),
                )
                Text(
                    stringResource(R.string.display_settings_font_size_value, fontSize),
                    color = c.textSecondary,
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp, fontFeatureSettings = "tnum"),
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StepButton("−", stringResource(R.string.display_settings_font_smaller), fontSize > MIN_FONT_SIZE) {
                    onFontSizeChange((fontSize - FONT_SIZE_STEP).coerceAtLeast(MIN_FONT_SIZE))
                }
                StepButton("＋", stringResource(R.string.display_settings_font_larger), fontSize < MAX_FONT_SIZE) {
                    onFontSizeChange((fontSize + FONT_SIZE_STEP).coerceAtMost(MAX_FONT_SIZE))
                }
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            for (step in MIN_FONT_SIZE..MAX_FONT_SIZE step FONT_SIZE_STEP) {
                Box(
                    Modifier
                        .weight(1f)
                        .height(3.dp)
                        .clip(RoundedCornerShape(1.dp))
                        .background(if (step <= fontSize) c.accentLine else c.track),
                )
            }
        }
        Text(
            text = stringResource(R.string.display_settings_preview),
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Justify,
            modifier = Modifier.padding(top = 4.dp),
            style = MaterialTheme.typography.bodyLarge.copy(fontSize = fontSize.sp, lineHeight = (fontSize * 1.6f).sp),
        )
    }
}

/** Tombol − / ＋ ukuran teks: border garis aksen, teks aksen. */
@Composable
private fun StepButton(label: String, description: String, enabled: Boolean, onClick: () -> Unit) {
    val c = QuestLogTheme.colors
    Box(
        modifier = Modifier
            .size(44.dp)
            .alpha(if (enabled) 1f else 0.4f)
            .clip(RoundedCornerShape(4.dp))
            .border(1.dp, c.accentLine, RoundedCornerShape(4.dp))
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        Text(label, color = c.accentText, style = MaterialTheme.typography.labelLarge.copy(fontSize = 20.sp))
    }
}

/** Baris "Mode Baca" dengan nama mode aktif, dan grid 4 swatch (radio group). */
@Composable
private fun ThemeSection(selected: ReaderTheme, onSelect: (ReaderTheme) -> Unit) {
    val c = QuestLogTheme.colors
    Column(Modifier.padding(top = 18.dp, bottom = 4.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                stringResource(R.string.display_settings_reader_mode),
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.titleMedium.copy(fontSize = 15.sp),
            )
            Text(
                themeName(selected),
                color = c.textSecondary,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp),
            )
        }
        Row(Modifier.fillMaxWidth().selectableGroup(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ReaderTheme.entries.forEach { theme ->
                ThemeSwatch(theme, theme == selected, Modifier.weight(1f)) { onSelect(theme) }
            }
        }
    }
}

/** Swatch "Aa" berlatar dan berteks warna mode [theme]; terpilih = border 2dp garis aksen, selain itu 1dp pembatas. */
@Composable
private fun ThemeSwatch(theme: ReaderTheme, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val c = QuestLogTheme.colors
    val preview = readerColorsFor(theme)
    val shape = RoundedCornerShape(SWATCH_RADIUS)
    Column(
        modifier = modifier.selectable(selected = selected, role = Role.RadioButton, onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(SWATCH_HEIGHT)
                .clip(shape)
                .background(preview.sheet)
                .border(if (selected) 2.dp else 1.dp, if (selected) c.accentLine else c.divider, shape),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                "Aa",
                color = preview.fg,
                style = TextStyle(fontFamily = CormorantGaramond, fontWeight = FontWeight.SemiBold, fontSize = 20.sp),
            )
        }
        Text(
            themeName(theme),
            color = if (selected) MaterialTheme.colorScheme.onSurface else c.textSecondary,
            style = TextStyle(
                fontFamily = Lora,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                fontSize = 12.5.sp,
            ),
        )
    }
}

@Composable
private fun themeName(theme: ReaderTheme): String = stringResource(
    when (theme) {
        ReaderTheme.LIGHT -> R.string.display_settings_theme_light
        ReaderTheme.DARK -> R.string.display_settings_theme_dark
        ReaderTheme.SEPIA -> R.string.display_settings_theme_sepia
        ReaderTheme.MUTED -> R.string.display_settings_theme_muted
    },
)

@Preview(showBackground = true, widthDp = 390, heightDp = 560)
@Composable
private fun DisplaySettingsLightPreview() = SheetPreview(ReaderTheme.LIGHT, 16)

@Preview(showBackground = true, backgroundColor = 0xFF22211F, widthDp = 390, heightDp = 560)
@Composable
private fun DisplaySettingsDarkPreview() = SheetPreview(ReaderTheme.DARK, 20)

@Preview(showBackground = true, backgroundColor = 0xFFFBF0D9, widthDp = 390, heightDp = 560)
@Composable
private fun DisplaySettingsSepiaPreview() = SheetPreview(ReaderTheme.SEPIA, 16)

@Preview(showBackground = true, backgroundColor = 0xFFE4E4E6, widthDp = 390, heightDp = 560)
@Composable
private fun DisplaySettingsMutedPreview() = SheetPreview(ReaderTheme.MUTED, 18)

@Composable
private fun SheetPreview(theme: ReaderTheme, fontSize: Int) {
    ReaderColorsScope(theme) {
        Box(Modifier.background(MaterialTheme.colorScheme.surface)) { SheetBody(fontSize, theme, {}, {}) }
    }
}
