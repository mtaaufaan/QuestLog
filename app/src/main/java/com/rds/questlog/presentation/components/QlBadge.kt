package com.rds.questlog.presentation.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rds.questlog.presentation.theme.QuestLogTheme

/** Pil kecil bertepi 20dp, mis. "1 halaman gagal". */
@Composable
fun QlBadge(text: String, modifier: Modifier = Modifier, color: Color = QuestLogTheme.colors.accentText) {
    Box(
        modifier = modifier
            .height(20.dp)
            .border(1.dp, color.copy(alpha = 0.6f), RoundedCornerShape(10.dp))
            .padding(horizontal = 7.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            color = color,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.5.sp, fontWeight = FontWeight.SemiBold),
        )
    }
}
