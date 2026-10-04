package com.rds.questlog.presentation.articlelist

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rds.questlog.R
import com.rds.questlog.presentation.components.QlBottomSheet
import com.rds.questlog.presentation.model.GameFilterOption
import com.rds.questlog.presentation.theme.CormorantGaramond
import com.rds.questlog.presentation.theme.QuestLogTheme

/**
 * Bottom sheet dengan radio group (bukan dropdown). `null` pada [selectedId] / [onSelect] = "Semua Game".
 * Pemanggil menutup sheet setelah memilih.
 */
@Composable
fun GameFilterSheet(games: List<GameFilterOption>, selectedId: Long?, onSelect: (Long?) -> Unit, onClose: () -> Unit) {
    val c = QuestLogTheme.colors
    val total = games.sumOf { it.count }
    QlBottomSheet(onDismiss = onClose) {
        Column(Modifier.padding(start = 12.dp, end = 12.dp, bottom = 22.dp)) {
            Text(
                text = stringResource(R.string.game_filter_title),
                color = MaterialTheme.colorScheme.onBackground,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontFamily = CormorantGaramond,
                    fontSize = 24.sp,
                    lineHeight = 28.sp,
                ),
                modifier = Modifier.padding(start = 12.dp, end = 12.dp, bottom = 12.dp),
            )
            HorizontalDivider(color = c.divider)
            Column(Modifier.padding(top = 6.dp).selectableGroup()) {
                FilterOptionRow(
                    stringResource(R.string.article_list_filter_all),
                    total,
                    selectedId == null,
                ) { onSelect(null) }
                games.forEach { game ->
                    FilterOptionRow(game.name, game.count, selectedId == game.id) { onSelect(game.id) }
                }
            }
        }
    }
}

@Composable
private fun FilterOptionRow(name: String, count: Int, selected: Boolean, onClick: () -> Unit) {
    val c = QuestLogTheme.colors
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(if (pressed) c.hover else Color.Transparent)
            .selectable(
                selected = selected,
                interactionSource = source,
                indication = null,
                role = Role.RadioButton,
                onClick = onClick,
            )
            .padding(horizontal = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier.size(20.dp).border(1.dp, if (selected) c.accentLine else c.iconMuted, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Box(Modifier.size(10.dp).background(if (selected) c.accentLine else Color.Transparent, CircleShape))
        }
        Text(
            text = name,
            color = MaterialTheme.colorScheme.onBackground,
            style = MaterialTheme.typography.bodyLarge.copy(fontSize = 15.sp),
            modifier = Modifier.weight(1f),
        )
        Text(
            text = count.toString(),
            color = c.textTertiary,
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp, fontFeatureSettings = "tnum"),
        )
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 420)
@Composable
private fun GameFilterSheetPreview() {
    QuestLogTheme {
        GameFilterSheet(
            games = listOf(GameFilterOption(1, "Dragon Quest VII", 3), GameFilterOption(2, "Breath Of Fire III", 2)),
            selectedId = 2L,
            onSelect = {},
            onClose = {},
        )
    }
}
