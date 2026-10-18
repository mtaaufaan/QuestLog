package com.rds.questlog.presentation.gamelogo

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.activity.ComponentActivity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rds.questlog.R
import com.rds.questlog.presentation.theme.QuestLogTheme

/** [GameLogoViewModel] milik Activity: sama untuk S0, S1, dan P19 sehingga pesan hasil ikut ke layar asal. */
@Composable
fun rememberGameLogoViewModel(): GameLogoViewModel {
    val context = LocalContext.current
    val owner = remember(context) { context.findComponentActivity() }
    return hiltViewModel(viewModelStoreOwner = owner)
}

private tailrec fun Context.findComponentActivity(): ComponentActivity = when (this) {
    is ComponentActivity -> this
    is ContextWrapper -> baseContext.findComponentActivity()
    else -> error("Tidak ada Activity di ${(this as? Activity)?.javaClass}")
}

/**
 * Membungkus layar yang memuat logo game (S0, S1): menampilkan snackbar hasil (Logo diperbarui / dihapus + Urungkan)
 * dan memberi [content] aksi pesan hanya-baca untuk status premium hilang.
 */
@Composable
fun GameLogoHost(content: @Composable (showReadOnly: () -> Unit) -> Unit) {
    val viewModel = rememberGameLogoViewModel()
    val state by viewModel.state.collectAsStateWithLifecycle()
    Box(Modifier.fillMaxSize()) {
        content(viewModel::showReadOnly)
        LogoSnackbar(state, viewModel, Modifier.align(Alignment.BottomStart).navigationBarsPadding())
    }
}

/** Snackbar bergaya [com.rds.questlog.presentation.components.QlToast] dengan satu aksi teks (Urungkan). */
@Composable
fun LogoSnackbar(state: GameLogoUiState, viewModel: GameLogoViewModel, modifier: Modifier = Modifier) {
    val c = QuestLogTheme.colors
    val message = state.message
    val text = message?.let { stringResource(it.res) }
    val action = message?.undoGameId?.let { stringResource(R.string.game_logo_undo) }
    // Simpan isi terakhir agar tetap terbaca selama animasi keluar.
    var last by remember { mutableStateOf("" to (null as String?)) }
    if (text != null) last = text to action
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
                        .clickable(role = Role.Button) { message?.undoGameId?.let(viewModel::undo) }
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
