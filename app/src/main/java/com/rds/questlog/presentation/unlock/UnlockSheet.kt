package com.rds.questlog.presentation.unlock

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rds.questlog.R
import com.rds.questlog.presentation.components.QlBottomSheet
import com.rds.questlog.presentation.components.QlIcons
import com.rds.questlog.presentation.theme.CormorantGaramond
import com.rds.questlog.presentation.theme.Lora
import com.rds.questlog.presentation.theme.QuestLogTheme

/**
 * Unlock Sheet (component-contract.md §9). Dikendalikan dari luar: [phase] (proses beli/restore) dan [message]
 * (hasil yang tidak berujung aktif) datang dari pemanggil; [isPremium] true menampilkan layar sukses untuk kedua aksi.
 * [gamesUsed] (0-2) = jumlah game, bukan artikel.
 */
@Composable
fun UnlockSheet(
    gamesUsed: Int,
    isPremium: Boolean,
    phase: UnlockPhase,
    message: UnlockMessage?,
    onPurchase: () -> Unit,
    onRestore: () -> Unit,
    onClose: () -> Unit,
) {
    QlBottomSheet(onDismiss = onClose) {
        UnlockSheetContent(
            phase = if (isPremium) UnlockPhase.DONE else phase,
            gamesUsed = gamesUsed,
            message = message,
            onPurchase = onPurchase,
            onRestore = onRestore,
            onClose = onClose,
        )
    }
}

/** Isi sheet tanpa state sendiri, agar tiap [UnlockPhase] bisa dipratinjau. */
@Composable
fun UnlockSheetContent(
    phase: UnlockPhase,
    gamesUsed: Int,
    message: UnlockMessage?,
    onPurchase: () -> Unit,
    onRestore: () -> Unit,
    onClose: () -> Unit,
) {
    Column(Modifier.navigationBarsPadding().padding(start = 24.dp, end = 24.dp, bottom = 26.dp, top = 12.dp)) {
        if (phase == UnlockPhase.DONE) {
            DoneBody(onClose)
        } else {
            Header()
            ComparisonTable(gamesUsed)
            Text(
                text = stringResource(R.string.unlock_note),
                color = QuestLogTheme.colors.textTertiary,
                modifier = Modifier.padding(top = 12.dp, bottom = if (message == null) 18.dp else 8.dp),
                style = TextStyle(fontFamily = Lora, fontSize = 12.5.sp, lineHeight = 18.sp),
            )
            if (message != null) {
                Text(
                    text = stringResource(message.text),
                    color = if (message.isError) QuestLogTheme.colors.danger else QuestLogTheme.colors.accentText,
                    modifier = Modifier.padding(bottom = 14.dp),
                    style = TextStyle(fontFamily = Lora, fontWeight = FontWeight.SemiBold, fontSize = 13.sp),
                )
            }
            val busy = phase != UnlockPhase.IDLE
            PurchaseButton(
                label = stringResource(
                    if (phase == UnlockPhase.PROCESSING) R.string.unlock_processing else R.string.unlock_purchase,
                ),
                loading = phase == UnlockPhase.PROCESSING,
                enabled = !busy,
                onClick = onPurchase,
            )
            RestoreButton(
                label = stringResource(
                    if (phase == UnlockPhase.RESTORING) R.string.unlock_restoring else R.string.unlock_restore,
                ),
                enabled = !busy,
                onClick = onRestore,
            )
        }
    }
}

@Composable
private fun Header() {
    Column(Modifier.padding(bottom = 16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = stringResource(R.string.unlock_kicker).uppercase(),
            color = QuestLogTheme.colors.accentText,
            style = TextStyle(
                fontFamily = Lora,
                fontWeight = FontWeight.SemiBold,
                fontSize = 10.5.sp,
                letterSpacing = 1.26.sp,
            ),
        )
        Text(
            text = stringResource(R.string.unlock_title),
            color = MaterialTheme.colorScheme.onSurface,
            style = TextStyle(
                fontFamily = CormorantGaramond,
                fontWeight = FontWeight.Medium,
                fontSize = 30.sp,
                lineHeight = 33.sp,
            ),
        )
    }
}

@Composable
private fun ComparisonTable(gamesUsed: Int) {
    Column(Modifier.fillMaxWidth()) {
        Box(Modifier.fillMaxWidth().height(1.dp).background(MaterialTheme.colorScheme.onSurface))
        TableRow(
            first = "",
            free = stringResource(R.string.unlock_col_free),
            unlimited = stringResource(R.string.unlock_col_unlimited),
            header = true,
        )
        TableRow(
            first = stringResource(R.string.unlock_row_games),
            free = stringResource(R.string.unlock_games_usage, gamesUsed.coerceIn(0, MAX_GAMES)),
            unlimited = INFINITY,
        )
        TableRow(stringResource(R.string.unlock_row_articles), "5", INFINITY)
        TableRow(stringResource(R.string.unlock_row_offline), CHECK, CHECK)
    }
}

@Composable
private fun TableRow(first: String, free: String, unlimited: String, header: Boolean = false) {
    val c = QuestLogTheme.colors
    val vertical = if (header) 9.dp else 11.dp
    val headerStyle =
        TextStyle(fontFamily = Lora, fontWeight = FontWeight.SemiBold, fontSize = 11.sp, letterSpacing = 0.88.sp)
    val bodyStyle = TextStyle(fontFamily = Lora, fontSize = 14.sp, fontFeatureSettings = "tnum")
    Column {
        Row(Modifier.fillMaxWidth().padding(vertical = vertical), verticalAlignment = Alignment.CenterVertically) {
            Text(first, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f), style = bodyStyle)
            Text(
                text = if (header) free.uppercase() else free,
                color = if (header) c.textSecondary else MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                modifier = Modifier.widthIn(min = 72.dp, max = 72.dp),
                style = if (header) headerStyle else bodyStyle,
            )
            Text(
                text = if (header) unlimited.uppercase() else unlimited,
                color = c.accentText,
                textAlign = TextAlign.Center,
                modifier = Modifier.widthIn(min = 88.dp, max = 88.dp),
                style = if (header) {
                    headerStyle
                } else {
                    bodyStyle.copy(
                        fontWeight = if (unlimited == CHECK) FontWeight.Normal else FontWeight.SemiBold,
                    )
                },
            )
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(c.divider))
    }
}

@Composable
private fun PurchaseButton(label: String, loading: Boolean, enabled: Boolean, onClick: () -> Unit) {
    val c = QuestLogTheme.colors
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val shape = RoundedCornerShape(4.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
            .alpha(if (enabled || loading) 1f else 0.7f)
            .clip(shape)
            .background(if (pressed && enabled) c.accentTintActive else Color.Transparent)
            .border(1.dp, c.accentLine, shape)
            .clickable(
                interactionSource = source,
                indication = null,
                enabled = enabled,
                role = Role.Button,
                onClick = onClick,
            ),
        horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (loading) CircularProgressIndicator(Modifier.size(14.dp), color = c.accentLine, strokeWidth = 1.5.dp)
        Text(
            label,
            color = c.accentText,
            style = TextStyle(fontFamily = Lora, fontWeight = FontWeight.SemiBold, fontSize = 15.sp),
        )
    }
}

@Composable
private fun RestoreButton(label: String, enabled: Boolean, onClick: () -> Unit) {
    val c = QuestLogTheme.colors
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    Box(
        modifier = Modifier
            .padding(top = 6.dp)
            .fillMaxWidth()
            .height(46.dp)
            .alpha(if (enabled) 1f else 0.7f)
            .clip(RoundedCornerShape(4.dp))
            .background(if (pressed && enabled) c.hover else Color.Transparent)
            .clickable(
                interactionSource = source,
                indication = null,
                enabled = enabled,
                role = Role.Button,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            color = c.textSecondary,
            style = TextStyle(fontFamily = Lora, fontWeight = FontWeight.SemiBold, fontSize = 14.sp),
        )
    }
}

@Composable
private fun DoneBody(onClose: () -> Unit) {
    val c = QuestLogTheme.colors
    Column(
        modifier = Modifier.fillMaxWidth().padding(top = 18.dp, bottom = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(Modifier.size(56.dp).border(1.dp, c.accentLine, CircleShape), contentAlignment = Alignment.Center) {
            Icon(QlIcons.Check, contentDescription = null, tint = c.accentText, modifier = Modifier.size(26.dp))
        }
        Text(
            text = stringResource(R.string.unlock_done_title),
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(top = 6.dp),
            style = TextStyle(
                fontFamily = CormorantGaramond,
                fontWeight = FontWeight.SemiBold,
                fontSize = 28.sp,
                lineHeight = 32.sp,
            ),
        )
        Text(
            text = stringResource(R.string.unlock_done_body),
            color = c.textSecondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(max = 280.dp),
            style = TextStyle(fontFamily = Lora, fontSize = 14.sp, lineHeight = 21.sp),
        )
        val shape = RoundedCornerShape(4.dp)
        Box(
            modifier = Modifier
                .padding(top = 14.dp)
                .fillMaxWidth()
                .height(48.dp)
                .clip(shape)
                .border(1.dp, c.accentLine, shape)
                .clickable(role = Role.Button, onClick = onClose),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                stringResource(R.string.unlock_done_continue),
                color = c.accentText,
                style = TextStyle(fontFamily = Lora, fontWeight = FontWeight.SemiBold, fontSize = 15.sp),
            )
        }
    }
}

private const val MAX_GAMES = 2
private const val INFINITY = "∞"
private const val CHECK = "✓"

@Preview(showBackground = true, widthDp = 390)
@Composable
private fun UnlockIdlePreview() = PreviewSheet(UnlockPhase.IDLE, gamesUsed = 2)

@Preview(showBackground = true, widthDp = 390)
@Composable
private fun UnlockFewGamesPreview() = PreviewSheet(UnlockPhase.IDLE, gamesUsed = 0)

@Preview(showBackground = true, widthDp = 390)
@Composable
private fun UnlockPurchasingPreview() = PreviewSheet(UnlockPhase.PROCESSING, gamesUsed = 2)

@Preview(showBackground = true, widthDp = 390)
@Composable
private fun UnlockRestoringPreview() = PreviewSheet(UnlockPhase.RESTORING, gamesUsed = 1)

@Preview(showBackground = true, widthDp = 390)
@Composable
private fun UnlockNotFoundPreview() = PreviewSheet(UnlockPhase.IDLE, gamesUsed = 2, message = UnlockMessage.NOT_FOUND)

@Preview(showBackground = true, widthDp = 390)
@Composable
private fun UnlockUnavailablePreview() = PreviewSheet(
    UnlockPhase.IDLE,
    gamesUsed = 2,
    message = UnlockMessage.UNAVAILABLE,
)

@Preview(showBackground = true, widthDp = 390)
@Composable
private fun UnlockDonePreview() = PreviewSheet(UnlockPhase.DONE, gamesUsed = 2)

@Composable
private fun PreviewSheet(phase: UnlockPhase, gamesUsed: Int, message: UnlockMessage? = null) {
    QuestLogTheme {
        Box(Modifier.background(MaterialTheme.colorScheme.surface)) {
            UnlockSheetContent(phase, gamesUsed, message, onPurchase = {}, onRestore = {}, onClose = {})
        }
    }
}
