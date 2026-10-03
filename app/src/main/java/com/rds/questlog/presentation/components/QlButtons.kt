package com.rds.questlog.presentation.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.rds.questlog.presentation.theme.QuestLogTheme

// Radius 4dp sesuai kode desain (tombol di semua dialog/sheet).
private val ButtonShape = RoundedCornerShape(4.dp)

@Composable
private fun QlButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier,
    enabled: Boolean,
    contentColor: Color,
    pressedColor: Color,
    border: BorderStroke?,
    icon: ImageVector? = null,
) {
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    Row(
        modifier = modifier
            .heightIn(min = 44.dp)
            .alpha(if (enabled) 1f else 0.4f)
            .clip(ButtonShape)
            .background(if (pressed) pressedColor else Color.Transparent)
            .then(if (border != null) Modifier.border(border, ButtonShape) else Modifier)
            .clickable(
                interactionSource = source,
                indication = null,
                enabled = enabled,
                role = Role.Button,
                onClick = onClick,
            )
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = contentColor, modifier = Modifier.size(16.dp))
        }
        Text(text, color = contentColor, style = MaterialTheme.typography.labelLarge)
    }
}

/** Tombol primer: border aksen, latar transparan (design.md §3.4). [icon] opsional di kiri teks. */
@Composable
fun QlOutlineButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null,
) {
    val c = QuestLogTheme.colors
    QlButton(text, onClick, modifier, enabled, c.accentText, c.accentTintActive, BorderStroke(1.dp, c.accentLine), icon)
}

/** Tombol teks tanpa border. */
@Composable
fun QlGhostButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    val c = QuestLogTheme.colors
    QlButton(text, onClick, modifier, enabled, c.textSecondary, c.hover, null)
}

/** Tombol aksi destruktif (hapus) dengan border. */
@Composable
fun QlDangerButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    val c = QuestLogTheme.colors
    QlButton(text, onClick, modifier, enabled, c.danger, c.dangerTint, BorderStroke(1.dp, c.danger))
}

/** Tombol destruktif tanpa border (mis. "Hapus" di Scrape Error Dialog). */
@Composable
fun QlDangerGhostButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    val c = QuestLogTheme.colors
    QlButton(text, onClick, modifier, enabled, c.danger, c.dangerTint, null)
}
