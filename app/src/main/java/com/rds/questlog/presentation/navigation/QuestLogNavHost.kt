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
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.rds.questlog.presentation.addarticle.AddArticleScreen
import com.rds.questlog.presentation.articlelist.ArticleListScreen
import com.rds.questlog.presentation.components.QlSnackbarHost
import com.rds.questlog.presentation.popup.Popup
import com.rds.questlog.presentation.popup.PopupHost
import com.rds.questlog.presentation.reader.ReaderScreen

@Composable
fun QuestLogNavHost() {
    val navController = rememberNavController()
    var activePopup by remember { mutableStateOf<Popup?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }

    Box {
        NavHost(navController = navController, startDestination = ArticleList) {
            composable<ArticleList> {
                ArticleListScreen(navController, onShowPopup = { activePopup = it })
            }
            composable<AddArticle> { entry ->
                val route = entry.toRoute<AddArticle>()
                AddArticleScreen(route.mode, route.targetArticleId, navController)
            }
            composable<Reader> { entry ->
                val route = entry.toRoute<Reader>()
                ReaderScreen(route.articleId, route.resumeFrom, navController, onShowPopup = { activePopup = it })
            }
        }
        PopupHost(popup = activePopup, onDismiss = { activePopup = null })
        QlSnackbarHost(snackbarHostState, Modifier.align(Alignment.BottomCenter).navigationBarsPadding())
    }
}
