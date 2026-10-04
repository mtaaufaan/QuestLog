package com.rds.questlog.presentation.displaysettings

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rds.questlog.R
import com.rds.questlog.presentation.components.QlBottomSheet
import com.rds.questlog.presentation.theme.CormorantGaramond
import com.rds.questlog.presentation.theme.QuestLogColors
import com.rds.questlog.presentation.theme.QuestLogTheme

const val MIN_FONT_SIZE = 12
const val MAX_FONT_SIZE = 24
const val FONT_SIZE_STEP = 2
const val DEFAULT_FONT_SIZE = 16

/**
 * Display Settings Sheet (component-contract.md §11): ukuran teks 12–24sp (step 2) dan mode gelap. Berlaku app-wide;
 * sheet ikut tampil gelap bila [darkMode] aktif. Perubahan langsung dikirim lewat callback.
 */
@Composable
fun DisplaySettingsSheet(
    fontSize: Int,
    darkMode: Boolean,
    onFontSizeChange: (Int) -> Unit,
    onDarkModeToggle: (Boolean) -> Unit,
    onClose: () -> Unit,
) {
    // Sheet berada di window sendiri, jadi tema gelap Reader harus diteruskan secara eksplisit.
    QuestLogColors(darkTheme = darkMode) {
        QlBottomSheet(onDismiss = onClose) {
            SheetBody(fontSize, darkMode, onFontSizeChange, onDarkModeToggle)
        }
    }
}

@Composable
private fun SheetBody(
    fontSize: Int,
    darkMode: Boolean,
    onFontSizeChange: (Int) -> Unit,
    onDarkModeToggle: (Boolean) -> Unit,
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
        DarkModeRow(darkMode, onDarkModeToggle)
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

@Composable
private fun StepButton(label: String, description: String, enabled: Boolean, onClick: () -> Unit) {
    val c = QuestLogTheme.colors
    Box(
        modifier = Modifier
            .size(44.dp)
            .alpha(if (enabled) 1f else 0.4f)
            .clip(RoundedCornerShape(4.dp))
            .border(1.dp, c.accentText, RoundedCornerShape(4.dp))
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        Text(label, color = c.accentText, style = MaterialTheme.typography.labelLarge.copy(fontSize = 20.sp))
    }
}

@Composable
private fun DarkModeRow(darkMode: Boolean, onToggle: (Boolean) -> Unit) {
    val c = QuestLogTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(role = Role.Switch) { onToggle(!darkMode) }
            .padding(top = 18.dp, bottom = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                stringResource(R.string.display_settings_dark_mode),
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.titleMedium.copy(fontSize = 15.sp),
            )
            Text(
                stringResource(if (darkMode) R.string.display_settings_dark_on else R.string.display_settings_dark_off),
                color = c.textSecondary,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp),
            )
        }
        Switch(darkMode)
    }
}

/** Saklar 48×28 gaya desain (bukan Material Switch): track beraksen saat aktif, knob 20dp bergeser 0,2 dtk. */
@Composable
private fun Switch(on: Boolean) {
    val c = QuestLogTheme.colors
    val track by animateColorAsState(if (on) c.accentLine else Color.Transparent, tween(SWITCH_MS), label = "track")
    val knob by animateColorAsState(
        if (on) MaterialTheme.colorScheme.surface else c.iconMuted,
        tween(SWITCH_MS),
        label = "knob",
    )
    val offset by animateDpAsState(if (on) 23.dp else 3.dp, tween(SWITCH_MS), label = "offset")
    Box(
        Modifier
            .size(width = 48.dp, height = 28.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(track)
            .border(1.dp, if (on) c.accentLine else c.iconMuted, RoundedCornerShape(14.dp))
            .semantics { role = Role.Switch },
    ) {
        Box(Modifier.offset(x = offset, y = 3.dp).size(20.dp).clip(CircleShape).background(knob))
    }
}

private const val SWITCH_MS = 200

@Preview(showBackground = true, widthDp = 390, heightDp = 560)
@Composable
private fun DisplaySettingsLightPreview() {
    QuestLogColors(darkTheme = false) { SheetPreviewBody(16, false) }
}

@Preview(showBackground = true, backgroundColor = 0xFF22211F, widthDp = 390, heightDp = 560)
@Composable
private fun DisplaySettingsDarkPreview() {
    QuestLogColors(darkTheme = true) { SheetPreviewBody(20, true) }
}

@Composable
private fun SheetPreviewBody(fontSize: Int, dark: Boolean) {
    Box(Modifier.background(MaterialTheme.colorScheme.surface)) { SheetBody(fontSize, dark, {}, {}) }
}
