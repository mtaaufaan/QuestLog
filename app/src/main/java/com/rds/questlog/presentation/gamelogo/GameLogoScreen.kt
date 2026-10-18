package com.rds.questlog.presentation.gamelogo

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.rds.questlog.R
import com.rds.questlog.domain.model.LOGO_FRAME
import com.rds.questlog.domain.model.LOGO_MAX_ZOOM
import com.rds.questlog.domain.model.LOGO_MIN_ZOOM
import com.rds.questlog.domain.model.LogoCrop
import com.rds.questlog.domain.model.clamped
import com.rds.questlog.presentation.components.LogoInitial
import com.rds.questlog.presentation.components.QlDangerButton
import com.rds.questlog.presentation.components.QlOutlineButton
import com.rds.questlog.presentation.theme.CormorantGaramond
import com.rds.questlog.presentation.theme.Lora
import com.rds.questlog.presentation.theme.QuestLogTheme
import java.io.File

private const val ZOOM_STEP = 0.25f
private val CropSaver: Saver<LogoCrop, Any> = listSaver(
    save = { listOf(it.zoom, it.x, it.y) },
    restore = { LogoCrop(it[0], it[1], it[2]) },
)

/**
 * P19 Logo game (premium): memilih gambar dari Photo Picker, menggeser/mencubit/zoom di bawah bingkai 264dp, lalu
 * Simpan. Logo yang sudah ada dimuat dengan posisi tersimpan sehingga bisa diatur ulang tanpa unggah ulang.
 */
@Composable
fun GameLogoScreen(
    navController: NavController,
    viewModel: GameLogoEditViewModel = hiltViewModel(),
    logoViewModel: GameLogoViewModel = rememberGameLogoViewModel(),
) {
    val game by viewModel.game.collectAsStateWithLifecycle()
    val logoState by logoViewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var pickedUri by rememberSaveable { mutableStateOf<String?>(null) }
    var crop by rememberSaveable(stateSaver = CropSaver) { mutableStateOf(LogoCrop()) }
    var loaded by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(game) {
        val current = game
        if (!loaded && current != null) {
            current.logoCrop?.let { crop = it }
            loaded = true
        }
    }
    LaunchedEffect(Unit) { logoViewModel.done.collect { navController.popBackStack() } }

    val stored = game?.let { (it.logoSourcePath ?: it.logoPath)?.let { path -> File(context.filesDir, path) } }
    val model: Any? = pickedUri ?: stored
    var aspect by remember(model) { mutableStateOf<Float?>(null) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            pickedUri = uri.toString()
            crop = LogoCrop()
        }
    }
    val pick = { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }
    val applied = crop.clamped(aspect ?: 1f)
    val name = game?.name.orEmpty()

    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
            TopBar(
                name = name,
                saving = logoState.saving,
                canSave = model != null && aspect != null,
                onCancel = { navController.popBackStack() },
                onSave = { logoViewModel.save(viewModel.gameId, pickedUri, applied) },
            )
            CropArea(
                name = name,
                model = model,
                crop = applied,
                aspect = aspect,
                modifier = Modifier.weight(1f),
                onCrop = { crop = it },
                onAspect = { aspect = it },
                onPick = pick,
            )
            if (model != null) ZoomControls(applied, aspect ?: 1f) { crop = it }
            PreviewRow(name, model, applied, aspect, caption(model, pickedUri))
            Footer(
                hasLogo = game?.logoPath != null,
                hasImage = model != null,
                onPick = pick,
                onRemove = { logoViewModel.remove(viewModel.gameId) },
            )
        }
        LogoSnackbar(
            logoState,
            logoViewModel,
            Modifier.align(Alignment.BottomStart).navigationBarsPadding().padding(bottom = 72.dp),
        )
    }
}

@Composable
private fun caption(model: Any?, pickedUri: String?): String = stringResource(
    when {
        model == null -> R.string.logo_edit_caption_none
        pickedUri != null -> R.string.logo_edit_caption_new
        else -> R.string.logo_edit_caption_current
    },
)

@Composable
private fun TopBar(name: String, saving: Boolean, canSave: Boolean, onCancel: () -> Unit, onSave: () -> Unit) {
    val c = QuestLogTheme.colors
    Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        TopAction(stringResource(R.string.logo_edit_cancel), c.textSecondary, enabled = true, onClick = onCancel)
        Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                stringResource(R.string.logo_edit_title),
                color = MaterialTheme.colorScheme.onBackground,
                style = TextStyle(fontFamily = CormorantGaramond, fontWeight = FontWeight.Medium, fontSize = 22.sp),
            )
            Text(name, color = c.textSecondary, maxLines = 1, style = MaterialTheme.typography.bodySmall)
        }
        if (saving) {
            Box(Modifier.size(width = 72.dp, height = 48.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp, color = c.accentLine)
            }
        } else {
            TopAction(stringResource(R.string.logo_edit_save), c.accentText, enabled = canSave, onClick = onSave)
        }
    }
}

@Composable
private fun TopAction(text: String, color: androidx.compose.ui.graphics.Color, enabled: Boolean, onClick: () -> Unit) {
    Box(
        Modifier
            .size(width = 72.dp, height = 48.dp)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text,
            color = if (enabled) color else color.copy(alpha = 0.4f),
            style = MaterialTheme.typography.labelLarge.copy(fontSize = 15.sp, fontWeight = FontWeight.SemiBold),
        )
    }
}

/** Area crop: gambar di bawah bingkai (geser, cubit), atau bingkai putus-putus bila belum ada gambar. */
@Composable
private fun CropArea(
    name: String,
    model: Any?,
    crop: LogoCrop,
    aspect: Float?,
    onCrop: (LogoCrop) -> Unit,
    onAspect: (Float) -> Unit,
    onPick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current.density
    val latestCrop by rememberUpdatedState(crop)
    val latestAspect by rememberUpdatedState(aspect)
    Box(
        modifier
            .fillMaxWidth()
            .heightIn(min = 300.dp)
            .background(QuestLogTheme.colors.pressed)
            .clipToBounds()
            .pointerInput(model) {
                if (model != null) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        val ratio = latestAspect ?: return@detectTransformGestures
                        val now = latestCrop
                        onCrop(
                            LogoCrop(now.zoom * zoom, now.x + pan.x / density, now.y + pan.y / density).clamped(ratio),
                        )
                    }
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        if (model != null) {
            CropImage(model, crop, aspect, LOGO_FRAME.dp, clip = false, onAspect = onAspect)
            CropOverlay()
        } else {
            EmptyFrame(name, onPick)
        }
    }
}

@Composable
private fun EmptyFrame(name: String, onPick: () -> Unit) {
    Box(Modifier.size(LOGO_FRAME.dp), contentAlignment = Alignment.Center) {
        DashedFrame(Modifier.size(LOGO_FRAME.dp))
        Column(
            Modifier.padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            LogoInitial(name, 96.dp)
            Text(
                stringResource(R.string.logo_edit_empty),
                color = QuestLogTheme.colors.textSecondary,
                textAlign = TextAlign.Center,
                style = TextStyle(fontFamily = Lora, fontSize = 12.5.sp, lineHeight = 17.sp),
            )
            QlOutlineButton(stringResource(R.string.logo_edit_upload), onPick, Modifier.heightIn(min = 48.dp))
        }
    }
}

@Composable
private fun ZoomControls(crop: LogoCrop, aspect: Float, onCrop: (LogoCrop) -> Unit) {
    val c = QuestLogTheme.colors
    val zoom = { value: Float -> onCrop(crop.copy(zoom = value).clamped(aspect)) }
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        StepButton("−", stringResource(R.string.logo_edit_zoom_out)) { zoom(crop.zoom - ZOOM_STEP) }
        Slider(
            value = crop.zoom,
            onValueChange = zoom,
            valueRange = LOGO_MIN_ZOOM..LOGO_MAX_ZOOM,
            modifier = Modifier.weight(1f),
            colors = SliderDefaults.colors(
                thumbColor = c.accentLine,
                activeTrackColor = c.accentLine,
                inactiveTrackColor = c.track,
            ),
        )
        StepButton("+", stringResource(R.string.logo_edit_zoom_in)) { zoom(crop.zoom + ZOOM_STEP) }
        Box(
            Modifier
                .heightIn(min = 48.dp)
                .clickable(role = Role.Button) { onCrop(LogoCrop()) }
                .padding(horizontal = 10.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                stringResource(R.string.logo_edit_reset),
                color = c.accentText,
                style = MaterialTheme.typography.labelLarge.copy(fontSize = 13.sp),
            )
        }
    }
}

@Composable
private fun StepButton(symbol: String, description: String, onClick: () -> Unit) {
    Box(
        Modifier
            .size(48.dp)
            .semantics { contentDescription = description }
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(symbol, color = QuestLogTheme.colors.textSecondary, style = TextStyle(fontSize = 22.sp))
    }
}

/** Pratinjau live 52dp dan 64dp mengikuti posisi dan zoom; tanpa gambar menampilkan inisial. */
@Composable
private fun PreviewRow(name: String, model: Any?, crop: LogoCrop, aspect: Float?, caption: String) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PreviewTile(52.dp, 6.dp, name, model, crop, aspect)
        PreviewTile(64.dp, 8.dp, name, model, crop, aspect)
        Text(
            caption,
            color = QuestLogTheme.colors.textSecondary,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.5.sp),
        )
    }
}

@Composable
private fun PreviewTile(size: Dp, radius: Dp, name: String, model: Any?, crop: LogoCrop, aspect: Float?) {
    val c = QuestLogTheme.colors
    val shape = RoundedCornerShape(radius)
    Box(
        Modifier.size(size).clip(shape).background(c.hover).border(1.dp, c.divider, shape),
        contentAlignment = Alignment.Center,
    ) {
        if (model != null) CropImage(model, crop, aspect, size, clip = true) else LogoInitial(name, size)
    }
}

@Composable
private fun Footer(hasLogo: Boolean, hasImage: Boolean, onPick: () -> Unit, onRemove: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, bottom = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.End),
    ) {
        if (hasLogo) QlDangerButton(stringResource(R.string.game_logo_remove), onRemove, Modifier.heightIn(min = 48.dp))
        QlOutlineButton(
            stringResource(if (hasImage) R.string.logo_edit_upload_new else R.string.logo_edit_upload),
            onPick,
            Modifier.heightIn(min = 48.dp),
        )
    }
}
