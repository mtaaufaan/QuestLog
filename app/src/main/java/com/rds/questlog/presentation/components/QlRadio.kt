package com.rds.questlog.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.rds.questlog.presentation.theme.QuestLogTheme

/** Lingkaran radio 20dp (design.md §3.4); dipasang di dalam baris yang `selectable`. */
@Composable
fun QlRadio(selected: Boolean, modifier: Modifier = Modifier) {
    val c = QuestLogTheme.colors
    Box(
        modifier = modifier.size(20.dp).border(1.dp, if (selected) c.accentLine else c.iconMuted, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Box(Modifier.size(10.dp).background(if (selected) c.accentLine else Color.Transparent, CircleShape))
    }
}
