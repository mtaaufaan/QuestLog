package com.rds.questlog.presentation.gamelogo

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rds.questlog.R
import com.rds.questlog.presentation.articlelist.ActionRow
import com.rds.questlog.presentation.components.QlBottomSheet
import com.rds.questlog.presentation.components.QlIcons
import com.rds.questlog.presentation.theme.QuestLogTheme

/**
 * Membungkus layar yang memuat logo game: Photo Picker (tanpa izin storage), sheet Ganti/Hapus logo, dan snackbar
 * hasil (termasuk Urungkan). Logika simpan ada di [GameLogoViewModel]; perubahan tampil lewat data game yang diamati.
 */
@Composable
fun GameLogoHost(viewModel: GameLogoViewModel = hiltViewModel(), content: @Composable (GameLogoControls) -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var sheetFor by rememberSaveable { mutableStateOf<Long?>(null) }
    var pickFor by rememberSaveable { mutableStateOf<Long?>(null) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        val gameId = pickFor
        pickFor = null
        if (gameId != null && uri != null) viewModel.set(gameId, uri.toString())
    }
    val pick = { gameId: Long ->
        pickFor = gameId
        launcher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }
    val controls = remember(state.busyGameId) {
        GameLogoControls(state.busyGameId) { gameId, hasLogo -> if (hasLogo) sheetFor = gameId else pick(gameId) }
    }

    Box(Modifier.fillMaxSize()) {
        content(controls)
        LogoSnackbar(
            message = state.message?.let { stringResource(it.res) },
            actionLabel = state.message?.undoGameId?.let { stringResource(R.string.game_logo_undo) },
            onAction = { state.message?.undoGameId?.let(viewModel::undo) },
            modifier = Modifier.align(Alignment.BottomStart).navigationBarsPadding(),
        )
    }

    sheetFor?.let { gameId ->
        val c = QuestLogTheme.colors
        QlBottomSheet(onDismiss = { sheetFor = null }) {
            Column(Modifier.padding(start = 12.dp, end = 12.dp, bottom = 22.dp)) {
                ActionRow(QlIcons.Pencil, stringResource(R.string.game_logo_change), c.textSecondary) {
                    sheetFor = null
                    pick(gameId)
                }
                ActionRow(
                    icon = QlIcons.Trash,
                    label = stringResource(R.string.game_logo_remove),
                    iconColor = c.danger,
                    textColor = c.danger,
                    pressedColor = c.dangerTint,
                ) {
                    sheetFor = null
                    viewModel.remove(gameId)
                }
            }
        }
    }
}

/** Snackbar bergaya [com.rds.questlog.presentation.components.QlToast] dengan satu aksi teks (Urungkan). */
@Composable
private fun LogoSnackbar(message: String?, actionLabel: String?, onAction: () -> Unit, modifier: Modifier) {
    val c = QuestLogTheme.colors
    // Simpan isi terakhir agar tetap terbaca selama animasi keluar.
    var last by remember { mutableStateOf("" to (null as String?)) }
    if (message != null) last = message to actionLabel
    AnimatedVisibility(
        visible = message != null,
        modifier = modifier,
        enter = fadeIn() + slideInVertically { it / 2 },
        exit = fadeOut(),
    ) {
        Row(
            Modifier
                .padding(start = 16.dp, end = 92.dp, bottom = 32.dp)
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .shadow(4.dp, RoundedCornerShape(4.dp))
                .background(c.snackbar, RoundedCornerShape(4.dp))
                .padding(start = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                last.first,
                color = c.snackbarText,
                modifier = Modifier.weight(1f).padding(vertical = 12.dp),
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.5.sp, lineHeight = 19.sp),
            )
            last.second?.let { label ->
                Box(
                    Modifier
                        .heightIn(min = 48.dp)
                        .clickable(role = Role.Button, onClick = onAction)
                        .padding(horizontal = 16.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        label,
                        color = c.accentSoft,
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.SemiBold,
                        ),
                    )
                }
            }
        }
    }
}
