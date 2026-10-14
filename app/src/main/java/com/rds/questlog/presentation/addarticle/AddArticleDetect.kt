package com.rds.questlog.presentation.addarticle

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rds.questlog.R
import com.rds.questlog.presentation.components.QlGhostButton
import com.rds.questlog.presentation.components.QlIcons
import com.rds.questlog.presentation.components.QlOutlineButton
import com.rds.questlog.presentation.theme.Lora
import com.rds.questlog.presentation.theme.QuestLogTheme

/**
 * Bagian "Halaman lanjutan" di S2 (QL-34): tombol "Cari halaman lanjutan" (premium; bergembok → Unlock Sheet saat
 * gratis; nonaktif bila belum ada URL valid atau slot habis), lalu loading, daftar bercentang "Hal. n" dengan tombol
 * konfirmasi, atau pesan tidak ada hasil / gagal. Pemeriksaan hanya saat tombol diketuk, tidak pernah otomatis.
 */
@Composable
fun DetectSection(
    premium: Boolean,
    state: DetectState,
    base: String?,
    slots: Int,
    filled: Int,
    actions: DetectActions,
    modifier: Modifier = Modifier,
) {
    val c = QuestLogTheme.colors
    val shape = RoundedCornerShape(4.dp)
    Column(
        modifier = modifier.fillMaxWidth().border(1.dp, c.divider, shape).padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = stringResource(R.string.detect_title),
                    color = MaterialTheme.colorScheme.onBackground,
                    style = TextStyle(fontFamily = Lora, fontWeight = FontWeight.SemiBold, fontSize = 14.sp),
                )
                Text(
                    text = detectHint(premium, base, slots),
                    color = c.textSecondary,
                    style = TextStyle(fontFamily = Lora, fontSize = 12.5.sp, lineHeight = 17.sp),
                )
            }
            DetectButton(premium, state == DetectState.Loading, canDetect(premium, base, slots, state)) {
                if (premium) actions.onDetect() else actions.onOpenUnlock()
            }
        }
        when (state) {
            is DetectState.Found -> FoundList(state, slots, filled, actions)
            DetectState.NoResults -> Note(stringResource(R.string.detect_none), c.textTertiary)
            DetectState.Failed -> Note(stringResource(R.string.detect_failed), c.danger)
            is DetectState.Added -> Note(stringResource(R.string.detect_added, state.count), c.accentText)
            DetectState.Idle, DetectState.Loading -> Unit
        }
    }
}

@Composable
private fun detectHint(premium: Boolean, base: String?, slots: Int): String = when {
    !premium -> stringResource(R.string.detect_hint_free)
    base == null -> stringResource(R.string.detect_hint_no_url)
    slots <= 0 -> stringResource(R.string.detect_hint_full)
    else -> stringResource(
        R.string.detect_hint_source,
        base.removePrefix("https://").removePrefix("www.").substringBefore('/'),
    )
}

@Composable
private fun DetectButton(premium: Boolean, loading: Boolean, enabled: Boolean, onClick: () -> Unit) {
    val c = QuestLogTheme.colors
    val shape = RoundedCornerShape(4.dp)
    Row(
        modifier = Modifier
            .heightIn(min = 38.dp)
            .alpha(if (enabled) 1f else 0.45f)
            .clip(shape)
            .border(1.dp, c.accentLine, shape)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (loading) CircularProgressIndicator(Modifier.size(12.dp), color = c.accentLine, strokeWidth = 1.5.dp)
        Text(
            text = stringResource(if (loading) R.string.detect_searching else R.string.detect_action),
            color = c.accentText,
            style = TextStyle(fontFamily = Lora, fontWeight = FontWeight.SemiBold, fontSize = 12.5.sp),
        )
        if (!premium) {
            Icon(
                QlIcons.Lock,
                contentDescription = null,
                tint = c.accentText,
                modifier = Modifier.size(12.dp),
            )
        }
    }
}

@Composable
private fun Note(text: String, color: Color) {
    Text(
        text = text,
        color = color,
        style = TextStyle(fontFamily = Lora, fontStyle = FontStyle.Italic, fontSize = 12.5.sp),
    )
}

@Composable
private fun FoundList(state: DetectState.Found, slots: Int, filled: Int, actions: DetectActions) {
    val c = QuestLogTheme.colors
    Column {
        HorizontalDivider(color = c.divider)
        state.urls.forEachIndexed { index, url ->
            val on = url in state.selected
            val locked = !on && state.selected.size >= slots
            FoundRow(
                label = "Hal. ${filled + index + 1}",
                url = url,
                on = on,
                enabled = !locked,
            ) { actions.onToggle(url) }
        }
        if (state.urls.size > slots) {
            Text(
                text = stringResource(R.string.detect_limit, slots),
                color = c.textTertiary,
                modifier = Modifier.padding(top = 8.dp),
                style = TextStyle(fontFamily = Lora, fontStyle = FontStyle.Italic, fontSize = 12.sp),
            )
        }
        Row(
            Modifier.fillMaxWidth().padding(top = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.End),
        ) {
            QlGhostButton(stringResource(R.string.detect_dismiss), actions.onDismiss, Modifier.heightIn(min = 38.dp))
            QlOutlineButton(
                text = stringResource(R.string.detect_apply, state.selected.size),
                onClick = actions.onApply,
                modifier = Modifier.heightIn(min = 38.dp),
                enabled = state.selected.isNotEmpty(),
            )
        }
    }
}

@Composable
private fun FoundRow(label: String, url: String, on: Boolean, enabled: Boolean, onClick: () -> Unit) {
    val c = QuestLogTheme.colors
    val box = RoundedCornerShape(3.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 44.dp)
            .alpha(if (enabled) 1f else 0.45f)
            .clickable(enabled = enabled, role = Role.Checkbox, onClick = onClick),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(18.dp)
                .background(if (on) c.accentLine else Color.Transparent, box)
                .border(1.dp, if (on) c.accentLine else c.iconMuted, box),
            contentAlignment = Alignment.Center,
        ) {
            if (on) {
                Icon(
                    QlIcons.Check,
                    null,
                    tint = MaterialTheme.colorScheme.background,
                    modifier = Modifier.size(12.dp),
                )
            }
        }
        Text(
            text = label,
            color = c.textSecondary,
            modifier = Modifier.width(44.dp),
            style = TextStyle(
                fontFamily = Lora,
                fontWeight = FontWeight.SemiBold,
                fontSize = 12.sp,
                fontFeatureSettings = "tnum",
            ),
        )
        Text(
            text = url.removePrefix("https://"),
            color = c.textSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 11.5.sp),
        )
    }
}
