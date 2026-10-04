package com.rds.questlog.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.rds.questlog.presentation.theme.QuestLogTheme

/**
 * Bottom sheet: radius atas 12dp, drag handle 36×4dp, scrim 45%.
 * Animasi slide-up (qlSheet 0.28s) memakai animasi bawaan ModalBottomSheet.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QlBottomSheet(onDismiss: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    val c = QuestLogTheme.colors
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        // Langsung terbuka penuh; tanpa posisi setengah layar yang menyembunyikan item.
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        tonalElevation = 0.dp,
        scrimColor = c.scrim,
        dragHandle = {
            Box(
                Modifier
                    .padding(top = 10.dp, bottom = 6.dp)
                    .size(width = 36.dp, height = 4.dp)
                    .background(c.textTertiary.copy(alpha = 0.5f), RoundedCornerShape(2.dp)),
            )
        },
        content = content,
    )
}
