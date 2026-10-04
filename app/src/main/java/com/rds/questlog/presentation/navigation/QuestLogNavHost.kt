package com.rds.questlog.presentation.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.rds.questlog.presentation.addarticle.AddArticleScreen
import com.rds.questlog.presentation.articlelist.ArticleListScreen
import com.rds.questlog.presentation.components.QlSnackbarHost
import com.rds.questlog.presentation.model.AddArticlePayload
import com.rds.questlog.presentation.model.ScrapeNotificationKind
import com.rds.questlog.presentation.model.ScrapeNotificationUi
import com.rds.questlog.presentation.notification.ScrapeNotificationHost
import com.rds.questlog.presentation.popup.Popup
import com.rds.questlog.presentation.popup.PopupHost
import com.rds.questlog.presentation.reader.ReaderScreen
import kotlinx.coroutines.delay

private const val PROGRESS_STEP_MS = 1_100L
private const val DONE_VISIBLE_MS = 3_800L

@Composable
fun QuestLogNavHost() {
    val navController = rememberNavController()
    var activePopup by remember { mutableStateOf<Popup?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    var notification by remember { mutableStateOf<ScrapeNotificationUi?>(null) }
    var pendingSave by remember { mutableStateOf<AddArticlePayload?>(null) }

    // Tahap 1: urutan dummy progress → selesai; unduhan nyata (WorkManager) menggantikannya di Tahap 2.
    LaunchedEffect(pendingSave) {
        val payload = pendingSave ?: return@LaunchedEffect
        val total = payload.urls.count { it.isNotEmpty() }
        val title = payload.title.ifBlank { payload.newGameName ?: "" }
        for (page in 1..total) {
            notification = ScrapeNotificationUi(ScrapeNotificationKind.PROGRESS, title, page, total)
            delay(PROGRESS_STEP_MS)
        }
        notification = ScrapeNotificationUi(ScrapeNotificationKind.DONE, title, total, total)
        delay(DONE_VISIBLE_MS)
        notification = null
        pendingSave = null
    }

    Box {
        NavHost(navController = navController, startDestination = ArticleList) {
            composable<ArticleList> {
                ArticleListScreen(
                    navController = navController,
                    activePopup = activePopup,
                    onShowPopup = { activePopup = it },
                    onDismissPopup = { activePopup = null },
                )
            }
            composable<AddArticle> { entry ->
                val route = entry.toRoute<AddArticle>()
                AddArticleScreen(
                    mode = route.mode,
                    targetArticleId = route.targetArticleId,
                    navController = navController,
                    onShowPopup = { activePopup = it },
                    onSaved = { pendingSave = it },
                )
            }
            composable<Reader> { entry ->
                val route = entry.toRoute<Reader>()
                ReaderScreen(route.articleId, route.resumeFrom, navController, onShowPopup = { activePopup = it })
            }
        }
        PopupHost(popup = activePopup, onDismiss = { activePopup = null })
        ScrapeNotificationHost(
            notification = notification,
            onTap = { navController.onNotificationTap(it) { popup -> activePopup = popup } },
            modifier = Modifier.align(Alignment.TopCenter),
        )
        QlSnackbarHost(snackbarHostState, Modifier.align(Alignment.BottomCenter).navigationBarsPadding())
    }
}

private fun NavController.onNotificationTap(n: ScrapeNotificationUi, onShowPopup: (Popup) -> Unit) {
    when (n.kind) {
        ScrapeNotificationKind.DONE, ScrapeNotificationKind.PARTIAL ->
            navigate(Reader(articleId = n.articleId, resumeFrom = "last"))
        ScrapeNotificationKind.ERROR -> onShowPopup(Popup.ScrapeError(n.articleId))
        ScrapeNotificationKind.PROGRESS -> Unit
    }
}
