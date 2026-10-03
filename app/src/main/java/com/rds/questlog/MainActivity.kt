package com.rds.questlog

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.rds.questlog.presentation.navigation.QuestLogNavHost
import com.rds.questlog.presentation.theme.QuestLogTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            // Mode gelap hanya untuk Reader (design.md §3.2); sisa app selalu terang.
            QuestLogTheme(darkTheme = false) {
                QuestLogNavHost()
            }
        }
    }
}
