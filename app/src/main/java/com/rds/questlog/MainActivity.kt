package com.rds.questlog

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.rds.questlog.presentation.AppViewModel
import com.rds.questlog.presentation.navigation.QuestLogNavHost
import com.rds.questlog.presentation.share.ShareViewModel
import com.rds.questlog.presentation.theme.QuestLogTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val appViewModel: AppViewModel by viewModels()
    private val shareViewModel: ShareViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        appViewModel.verifyPremium()
        // Intent yang sama tidak diproses ulang setelah rotasi (savedInstanceState != null).
        if (savedInstanceState == null) handleShare(intent)
        setContent {
            // Mode gelap hanya untuk Reader (design.md §3.2); sisa app selalu terang.
            QuestLogTheme(darkTheme = false) {
                QuestLogNavHost()
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleShare(intent)
    }

    /** "Bagikan" teks dari browser (ACTION_SEND text/plain): URL diekstrak dan diteruskan ke navigasi (QL-21). */
    private fun handleShare(intent: Intent?) {
        if (intent?.action != Intent.ACTION_SEND || intent.type?.startsWith("text/") != true) return
        shareViewModel.onSharedText(intent.getCharSequenceExtra(Intent.EXTRA_TEXT)?.toString())
    }
}
