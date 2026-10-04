package com.rds.questlog.presentation.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.rds.questlog.presentation.addarticle.AddArticleScreen
import com.rds.questlog.presentation.articlelist.ArticleListScreen
import com.rds.questlog.presentation.components.QlSnackbarHost
import com.rds.questlog.presentation.model.ScrapeNotificationKind
import com.rds.questlog.presentation.model.ScrapeNotificationUi
import com.rds.questlog.presentation.notification.ScrapeNotificationHost
import com.rds.questlog.presentation.notification.ScrapeNotificationViewModel
import com.rds.questlog.presentation.popup.Popup
import com.rds.questlog.presentation.popup.PopupHost
import com.rds.questlog.presentation.reader.ReaderScreen

@Composable
fun QuestLogNavHost() {
    val navController = rememberNavController()
    var activePopup by remember { mutableStateOf<Popup?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    val notificationViewModel: ScrapeNotificationViewModel = hiltViewModel()
    val notification by notificationViewModel.notification.collectAsStateWithLifecycle()

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
                    onSaved = notificationViewModel::track,
                )
            }
            composable<Reader> { entry ->
                val route = entry.toRoute<Reader>()
                ReaderScreen(
                    articleId = route.articleId,
                    resumeFrom = route.resumeFrom,
                    navController = navController,
                    activePopup = activePopup,
                    onShowPopup = { activePopup = it },
                    onDismissPopup = { activePopup = null },
                )
            }
        }
        PopupHost(popup = activePopup, onDismiss = { activePopup = null })
        ScrapeNotificationHost(
            notification = notification,
            onTap = {
                notificationViewModel.dismiss()
                navController.onNotificationTap(it) { popup -> activePopup = popup }
            },
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
