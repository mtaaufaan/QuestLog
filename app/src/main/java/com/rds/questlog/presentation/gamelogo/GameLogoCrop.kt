package com.rds.questlog.presentation.gamelogo

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import com.rds.questlog.domain.model.LOGO_FRAME
import com.rds.questlog.domain.model.LogoCrop
import com.rds.questlog.presentation.theme.QuestLogTheme
import kotlin.math.max

/** Sisi muat gambar di memori; cukup tajam untuk zoom 4x tanpa memuat foto penuh. */
private const val IMAGE_REQUEST_PX = 1600
private const val GRID_ALPHA = 0.45f
private const val SCRIM_ALPHA = 0.78f
private val FRAME_RADIUS = 30.dp
private val FRAME_STROKE = 2.dp

/**
 * Gambar yang ditempatkan sesuai [crop] di dalam kotak [frame] x [frame] (skala [frame] / 264 dari bingkai crop):
 * dipakai oleh area crop (tanpa [clip], gambar di luar bingkai tetap tampak) dan pratinjau 52/64dp (dengan [clip]).
 * [aspect] (lebar / tinggi) belum diketahui = gambar dimuat dulu lalu dilaporkan lewat [onAspect].
 */
@Composable
internal fun CropImage(
    model: Any,
    crop: LogoCrop,
    aspect: Float?,
    frame: Dp,
    clip: Boolean,
    modifier: Modifier = Modifier,
    onAspect: (Float) -> Unit = {},
) {
    val context = LocalContext.current
    val density = LocalDensity.current.density
    val request = remember(model) { ImageRequest.Builder(context).data(model).size(IMAGE_REQUEST_PX).build() }
    val scale = frame.value / LOGO_FRAME
    Box(
        modifier.size(frame).then(if (clip) Modifier.clipToBounds() else Modifier),
        contentAlignment = Alignment.Center,
    ) {
        if (aspect == null) {
            AsyncImage(
                model = request,
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier.size(frame).alpha(0f),
                onSuccess = { state ->
                    val size = state.painter.intrinsicSize
                    if (size.height > 0f && size.width > 0f) onAspect(size.width / size.height)
                },
            )
        } else {
            AsyncImage(
                model = request,
                contentDescription = null,
                contentScale = ContentScale.FillBounds,
                modifier = Modifier
                    .requiredSize(frame * max(1f, aspect), frame * max(1f, 1f / aspect))
                    .graphicsLayer {
                        scaleX = crop.zoom
                        scaleY = crop.zoom
                        translationX = crop.x * scale * density
                        translationY = crop.y * scale * density
                    },
            )
        }
    }
}

/** Lapisan krem di luar bingkai (lubang bersudut bulat), bingkai emas 2dp, dan grid sepertiga. */
@Composable
internal fun CropOverlay(modifier: Modifier = Modifier) {
    val c = QuestLogTheme.colors
    val scrim = androidx.compose.material3.MaterialTheme.colorScheme.background.copy(alpha = SCRIM_ALPHA)
    Canvas(modifier.fillMaxSize()) {
        val frame = LOGO_FRAME.dp.toPx()
        val left = (size.width - frame) / 2f
        val top = (size.height - frame) / 2f
        val radius = CornerRadius(FRAME_RADIUS.toPx())
        val hole = RoundRect(left, top, left + frame, top + frame, radius)
        val path = Path().apply {
            fillType = PathFillType.EvenOdd
            addRect(Rect(Offset.Zero, size))
            addRoundRect(hole)
        }
        drawPath(path, scrim)
        val grid = c.accentLine.copy(alpha = GRID_ALPHA)
        for (i in 1..2) {
            val offset = frame * i / 3f
            drawLine(grid, Offset(left + offset, top), Offset(left + offset, top + frame), 1.dp.toPx())
            drawLine(grid, Offset(left, top + offset), Offset(left + frame, top + offset), 1.dp.toPx())
        }
        drawRoundRect(
            c.accentLine,
            Offset(left, top),
            Size(frame, frame),
            radius,
            style = Stroke(FRAME_STROKE.toPx()),
        )
    }
}

/** Bingkai putus-putus untuk keadaan belum ada gambar. */
@Composable
internal fun DashedFrame(modifier: Modifier = Modifier) {
    val c = QuestLogTheme.colors
    Canvas(modifier) {
        val dash = PathEffect.dashPathEffect(floatArrayOf(10.dp.toPx(), 8.dp.toPx()))
        drawRoundRect(
            c.accentLine,
            cornerRadius = CornerRadius(FRAME_RADIUS.toPx()),
            style = Stroke(FRAME_STROKE.toPx(), pathEffect = dash),
        )
    }
}
