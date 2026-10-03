package com.rds.questlog.presentation.articlelist

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavController
import com.rds.questlog.R
import com.rds.questlog.presentation.components.PlaceholderScreen
import com.rds.questlog.presentation.components.QlOutlineButton
import com.rds.questlog.presentation.navigation.AddArticle
import com.rds.questlog.presentation.navigation.Reader
import com.rds.questlog.presentation.popup.Popup

@Composable
fun ArticleListScreen(navController: NavController, onShowPopup: (Popup) -> Unit) {
    PlaceholderScreen(stringResource(R.string.placeholder_s1), stringResource(R.string.placeholder_s1_subtitle)) {
        QlOutlineButton(stringResource(R.string.placeholder_go_add_article), { navController.navigate(AddArticle()) })
        QlOutlineButton(stringResource(R.string.placeholder_go_reader), { navController.navigate(Reader(articleId = 1)) })
        QlOutlineButton(stringResource(R.string.placeholder_open_sheet), { onShowPopup(Popup.GameFilter) })
        QlOutlineButton(stringResource(R.string.placeholder_open_dialog), { onShowPopup(Popup.DeleteArticle(1)) })
    }
}
