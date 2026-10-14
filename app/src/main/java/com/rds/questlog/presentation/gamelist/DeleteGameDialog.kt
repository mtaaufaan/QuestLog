package com.rds.questlog.presentation.gamelist

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rds.questlog.R
import com.rds.questlog.presentation.components.QlDangerButton
import com.rds.questlog.presentation.components.QlDialog
import com.rds.questlog.presentation.components.QlGhostButton
import com.rds.questlog.presentation.theme.CormorantGaramond
import com.rds.questlog.presentation.theme.QuestLogTheme

/** Delete Game Dialog (P15): konfirmasi hapus game beserta seluruh artikel dan data turunannya. */
@Composable
fun DeleteGameDialog(gameName: String, articleCount: Int, onConfirm: () -> Unit, onClose: () -> Unit) {
    val c = QuestLogTheme.colors
    QlDialog(onDismiss = onClose, maxWidth = 320.dp) {
        Text(
            text = stringResource(R.string.delete_game_title),
            color = MaterialTheme.colorScheme.onBackground,
            style = MaterialTheme.typography.titleLarge.copy(
                fontFamily = CormorantGaramond,
                fontSize = 24.sp,
                lineHeight = 28.sp,
            ),
            modifier = Modifier.padding(bottom = 10.dp),
        )
        Text(
            text = "“$gameName”",
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.85f),
            style = MaterialTheme.typography.bodyMedium.copy(fontStyle = FontStyle.Italic, lineHeight = 20.sp),
            modifier = Modifier.padding(bottom = 6.dp),
        )
        Text(
            text = stringResource(R.string.delete_game_body, articleCount),
            color = c.textSecondary,
            style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 22.sp, fontWeight = FontWeight.Normal),
        )
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
        ) {
            QlGhostButton(stringResource(R.string.delete_game_cancel), onClose)
            QlDangerButton(stringResource(R.string.delete_game_confirm), onConfirm)
        }
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 640)
@Composable
private fun DeleteGameDialogPreview() {
    QuestLogTheme { DeleteGameDialog("Dragon Quest VII", 3, onConfirm = {}, onClose = {}) }
}
