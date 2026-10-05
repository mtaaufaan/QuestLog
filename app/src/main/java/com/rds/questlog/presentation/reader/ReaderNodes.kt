package com.rds.questlog.presentation.reader

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.rds.questlog.R
import com.rds.questlog.presentation.components.QlIcons
import com.rds.questlog.presentation.model.ContentNodeType
import com.rds.questlog.presentation.model.ContentNodeUi
import com.rds.questlog.presentation.theme.CormorantGaramond
import com.rds.questlog.presentation.theme.JetBrainsMono
import com.rds.questlog.presentation.theme.Lora
import com.rds.questlog.presentation.theme.QuestLogTheme
import java.io.File
import kotlin.math.roundToInt

private const val MARK_MS = 300
private const val MARK_SHIFT = -6f
private const val STRIPE_PERIOD_DP = 22.6f
private const val STRIPE_WIDTH_DP = 11.3f

/** Satu baris Reader sesuai jenisnya. [fontSize] dalam sp; [isCheckpoint] menampilkan penanda qlMark di atas blok. */
@Composable
fun ReaderRowItem(row: ReaderRow, fontSize: Int, articleTitle: String, isCheckpoint: Boolean) {
    when (row) {
        is ReaderRow.Node -> Column {
            if (isCheckpoint) CheckpointMarker()
            NodeBlock(row.node, fontSize, articleTitle)
        }
        is ReaderRow.Break -> PageBreak(stringResource(R.string.reader_page_break, row.page, row.host))
        is ReaderRow.Missing -> MissingPage(stringResource(R.string.reader_page_missing_label, row.page))
    }
}

@Composable
private fun NodeBlock(node: ContentNodeUi, fs: Int, articleTitle: String) {
    when (node.type) {
        ContentNodeType.H1 -> Heading(
            node.text.ifEmpty {
                articleTitle
            },
            CormorantGaramond,
            FontWeight.Medium,
            fs * H1_SCALE,
            1.08f,
            4.dp,
            16.dp,
        )
        ContentNodeType.H2 -> Heading(
            node.text,
            CormorantGaramond,
            FontWeight.SemiBold,
            fs * H2_SCALE,
            1.15f,
            26.dp,
            10.dp,
        )
        ContentNodeType.H3 -> Heading(node.text, Lora, FontWeight.SemiBold, fs * H3_SCALE, 1.3f, 18.dp, 6.dp)
        ContentNodeType.P -> Text(
            text = rememberStyled(node.text),
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Justify,
            modifier = Modifier.fillMaxWidth().padding(bottom = 14.dp),
            style = TextStyle(fontFamily = Lora, fontSize = fs.sp, lineHeight = (fs * 1.68f).sp),
        )
        ContentNodeType.LI -> ListItem(node.text, fs)
        ContentNodeType.PRE -> Preformatted(node.text, fs)
        ContentNodeType.IMG -> ImageBlock(node.text, node.imagePath)
        ContentNodeType.TABLE -> TableBlock(node, fs)
    }
}

private const val H1_SCALE = 1.95f
private const val H2_SCALE = 1.45f
private const val H3_SCALE = 1.08f
private const val PRE_SCALE = 0.72f

@Composable
private fun Heading(
    text: String,
    family: FontFamily,
    weight: FontWeight,
    sizeSp: Float,
    lineHeight: Float,
    top: Dp,
    bottom: Dp,
) {
    Text(
        text = text,
        color = MaterialTheme.colorScheme.onBackground,
        modifier = Modifier.fillMaxWidth().padding(top = top, bottom = bottom),
        style = TextStyle(
            fontFamily = family,
            fontWeight = weight,
            fontSize = sizeSp.roundToInt().sp,
            lineHeight = (sizeSp.roundToInt() * lineHeight).sp,
        ),
    )
}

@Composable
private fun ListItem(text: String, fs: Int) {
    Row(Modifier.fillMaxWidth().padding(bottom = 8.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("—", color = QuestLogTheme.colors.accentLine, style = TextStyle(fontFamily = Lora, fontSize = fs.sp))
        Text(
            text = rememberStyled(text),
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.weight(1f),
            style = TextStyle(fontFamily = Lora, fontSize = fs.sp, lineHeight = (fs * 1.6f).sp),
        )
    }
}

@Composable
private fun Preformatted(text: String, fs: Int) {
    val c = QuestLogTheme.colors
    val size = (fs * PRE_SCALE).roundToInt().coerceAtLeast(10)
    val shape = RoundedCornerShape(4.dp)
    Box(
        Modifier
            .fillMaxWidth()
            .padding(top = 6.dp, bottom = 18.dp)
            .clip(shape)
            .background(c.surface)
            .border(1.dp, c.divider, shape)
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 12.dp, vertical = 14.dp),
    ) {
        Text(
            text = text,
            color = MaterialTheme.colorScheme.onBackground,
            softWrap = false,
            style = TextStyle(fontFamily = JetBrainsMono, fontSize = size.sp, lineHeight = (size * 1.45f).sp),
        )
    }
}

/**
 * Gambar tersimpan: selebar kolom dengan proporsi aslinya (tidak pernah melebihi lebar layar). Tanpa [imagePath]
 * (mis. data dummy) tampil placeholder bergaris 4:3.
 */
@Composable
private fun ImageBlock(caption: String, imagePath: String?) {
    val c = QuestLogTheme.colors
    Column(Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 18.dp)) {
        if (imagePath != null) {
            AsyncImage(
                model = File(imagePath),
                contentDescription = caption.ifEmpty { null },
                contentScale = ContentScale.FillWidth,
                modifier = Modifier.fillMaxWidth().border(1.dp, c.divider).padding(1.dp),
            )
        } else {
            ImagePlaceholder()
        }
        if (caption.isNotEmpty()) {
            Text(
                text = caption,
                color = c.textSecondary,
                modifier = Modifier.padding(top = 8.dp),
                style = TextStyle(
                    fontFamily = Lora,
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    fontStyle = FontStyle.Italic,
                ),
            )
        }
    }
}

@Composable
private fun ImagePlaceholder() {
    val c = QuestLogTheme.colors
    val bg = MaterialTheme.colorScheme.background
    Box(Modifier.fillMaxWidth().border(1.dp, c.divider).padding(1.dp).border(6.dp, c.surface)) {
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(4f / 3f)
                .padding(6.dp)
                .clipToBounds()
                .drawBehind { drawStripes(c.surface, c.hover) },
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = stringResource(R.string.reader_image_placeholder),
                color = c.textSecondary,
                modifier = Modifier.background(bg).padding(horizontal = 8.dp, vertical = 4.dp),
                style = TextStyle(fontFamily = JetBrainsMono, fontSize = 11.sp),
            )
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawStripes(
    a: androidx.compose.ui.graphics.Color,
    b: androidx.compose.ui.graphics.Color,
) {
    drawRect(a)
    val period = STRIPE_PERIOD_DP.dp.toPx()
    val stroke = STRIPE_WIDTH_DP.dp.toPx()
    var x = -size.height
    while (x < size.width) {
        drawLine(b, Offset(x, size.height), Offset(x + size.height, 0f), strokeWidth = stroke)
        x += period
    }
}

@Composable
private fun PageBreak(label: String) {
    val c = QuestLogTheme.colors
    Row(
        Modifier.fillMaxWidth().padding(top = 30.dp, bottom = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.weight(1f).height(1.dp).background(c.divider))
        Text(
            label.uppercase(),
            color = c.textSecondary,
            style = TextStyle(
                fontFamily = Lora,
                fontWeight = FontWeight.SemiBold,
                fontSize = 10.sp,
                letterSpacing = 1.2.sp,
            ),
        )
        Box(Modifier.weight(1f).height(1.dp).background(c.divider))
    }
}

@Composable
private fun MissingPage(label: String) {
    val c = QuestLogTheme.colors
    val dash = PathEffect.dashPathEffect(floatArrayOf(8f, 6f))
    Column(
        Modifier
            .fillMaxWidth()
            .padding(top = 30.dp, bottom = 12.dp)
            .drawBehind {
                drawRoundRect(
                    c.danger,
                    cornerRadius = CornerRadius(4.dp.toPx()),
                    style = Stroke(1.dp.toPx(), pathEffect = dash),
                )
            }
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            label.uppercase(),
            color = c.danger,
            style = TextStyle(
                fontFamily = Lora,
                fontWeight = FontWeight.SemiBold,
                fontSize = 10.sp,
                letterSpacing = 1.2.sp,
            ),
        )
        Text(
            stringResource(R.string.reader_page_missing_body),
            color = c.textSecondary,
            style = TextStyle(fontFamily = Lora, fontSize = 13.sp, lineHeight = 19.sp),
        )
    }
}

/** Penanda checkpoint inline; muncul dengan qlMark (0,3 dtk: fade + geser 6dp dari kiri). */
@Composable
private fun CheckpointMarker() {
    val c = QuestLogTheme.colors
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) { progress.animateTo(1f, tween(MARK_MS)) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 6.dp, bottom = 8.dp)
            .graphicsLayer {
                alpha = progress.value
                translationX = MARK_SHIFT.dp.toPx() * (1f - progress.value)
            },
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(QlIcons.BookmarkSolid, null, tint = c.accentLine, modifier = Modifier.size(12.dp))
        Text(
            stringResource(R.string.reader_checkpoint_label).uppercase(),
            color = c.accentText,
            style = TextStyle(
                fontFamily = Lora,
                fontWeight = FontWeight.SemiBold,
                fontSize = 10.sp,
                letterSpacing = 1.4.sp,
            ),
        )
        Box(Modifier.weight(1f).height(1.dp).alpha(0.6f).background(c.accentLine))
    }
}
