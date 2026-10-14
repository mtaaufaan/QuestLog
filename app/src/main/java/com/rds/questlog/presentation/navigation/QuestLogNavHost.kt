package com.rds.questlog.presentation.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.rds.questlog.R
import com.rds.questlog.presentation.addarticle.AddArticleScreen
import com.rds.questlog.presentation.articlelist.ArticleListScreen
import com.rds.questlog.presentation.binder.BinderScreen
import com.rds.questlog.presentation.components.QlSnackbarHost
import com.rds.questlog.presentation.gamelist.GameListScreen
import com.rds.questlog.presentation.model.ScrapeNotificationUi
import com.rds.questlog.presentation.notification.ScrapeNotificationHost
import com.rds.questlog.presentation.notification.ScrapeNotificationViewModel
import com.rds.questlog.presentation.popup.Popup
import com.rds.questlog.presentation.popup.PopupHost
import com.rds.questlog.presentation.popup.PopupSaver
import com.rds.questlog.presentation.reader.ReaderScreen
import com.rds.questlog.presentation.share.ShareEvent
import com.rds.questlog.presentation.share.ShareViewModel
import kotlinx.coroutines.launch

@Composable
fun QuestLogNavHost() {
    val navController = rememberNavController()
    var activePopup by rememberSaveable(stateSaver = PopupSaver) { mutableStateOf<Popup?>(null) }
    // URL dari "Bagikan": menunggu pilihan mode di Scrape Mode Sheet, atau dikirim ke S2 yang sedang terbuka.
    var pendingSharedUrl by rememberSaveable { mutableStateOf<String?>(null) }
    var incomingSharedUrl by rememberSaveable { mutableStateOf<String?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    val notificationViewModel: ScrapeNotificationViewModel = hiltViewModel()
    val notification by notificationViewModel.notification.collectAsStateWithLifecycle()
    val shareViewModel: ShareViewModel = hiltViewModel()
    val shareEvent by shareViewModel.event.collectAsStateWithLifecycle()
    val onAddArticle = navController.currentBackStackEntryAsState().value?.destination?.hasRoute<AddArticle>() == true
    val noUrlMessage = stringResource(R.string.share_no_url)
    val scope = rememberCoroutineScope()

    LaunchedEffect(shareEvent) {
        val event = shareEvent ?: return@LaunchedEffect
        shareViewModel.consume()
        when (event) {
            // Scope tersendiri: consume() mengganti kunci efek ini dan akan membatalkan snackbar yang sedang tampil.
            ShareEvent.NoUrl -> scope.launch { snackbarHostState.showSnackbar(noUrlMessage) }
            is ShareEvent.Url -> if (onAddArticle) {
                incomingSharedUrl = event.url
            } else {
                navController.popBackStack<GameList>(inclusive = false)
                pendingSharedUrl = event.url
                activePopup = Popup.ScrapeMode
            }
        }
    }
    // URL yang menunggu pilihan mode dibuang bila Scrape Mode Sheet ditutup tanpa memilih.
    LaunchedEffect(activePopup) { if (activePopup != Popup.ScrapeMode) pendingSharedUrl = null }

    Box {
        NavHost(navController = navController, startDestination = GameList) {
            composable<GameList> {
                GameListScreen(
                    navController = navController,
                    activePopup = activePopup,
                    onShowPopup = { activePopup = it },
                    onDismissPopup = { activePopup = null },
                    pendingSharedUrl = pendingSharedUrl,
                )
            }
            composable<ArticleList> {
                ArticleListScreen(
                    navController = navController,
                    activePopup = activePopup,
                    onShowPopup = { activePopup = it },
                    onDismissPopup = { activePopup = null },
                    onScrapeStarted = notificationViewModel::track,
                    pendingSharedUrl = pendingSharedUrl,
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
                    sharedUrl = route.sharedUrl,
                    gameId = route.gameId,
                    incomingSharedUrl = incomingSharedUrl,
                    onIncomingHandled = { incomingSharedUrl = null },
                )
            }
            composable<Binder> {
                BinderScreen(
                    navController = navController,
                    activePopup = activePopup,
                    onShowPopup = { activePopup = it },
                    onDismissPopup = { activePopup = null },
                )
            }
            composable<Reader> { entry ->
                val route = entry.toRoute<Reader>()
                ReaderScreen(
                    articleId = route.articleId,
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
    when (val target = notificationTarget(n)) {
        is NotificationTarget.OpenReader -> navigate(Reader(articleId = target.articleId, resumeFrom = "last"))
        is NotificationTarget.OpenGameWithError -> {
            showGamePage(target.gameId)
            onShowPopup(Popup.ScrapeError(target.articleId))
        }
        NotificationTarget.None -> Unit
    }
}
