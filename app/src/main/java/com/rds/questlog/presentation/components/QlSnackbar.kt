package com.rds.questlog.presentation.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.rds.questlog.presentation.theme.QuestLogTheme
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Pil gelap melayang di bawah layar (design.md §3.4). Animasi masuk mengikuti SnackbarHost bawaan. */
@Composable
fun QlSnackbarHost(state: SnackbarHostState, modifier: Modifier = Modifier) {
    val c = QuestLogTheme.colors
    SnackbarHost(state, modifier) { data ->
        Box(Modifier.padding(16.dp)) {
            Snackbar(
                shape = RoundedCornerShape(50),
                containerColor = c.snackbar,
                contentColor = c.snackbarText,
            ) {
                Text(data.visuals.message, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

/** Tampilkan snackbar lalu auto-hide ±2,5 detik (design.md §3.4). */
suspend fun SnackbarHostState.showQl(message: String) = coroutineScope {
    val hider = launch {
        delay(2_500)
        currentSnackbarData?.dismiss()
    }
    showSnackbar(message, duration = SnackbarDuration.Indefinite)
    hider.cancel()
}
