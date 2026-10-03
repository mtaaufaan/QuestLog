package com.rds.questlog.presentation.reader

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavController
import com.rds.questlog.R
import com.rds.questlog.presentation.components.PlaceholderScreen
import com.rds.questlog.presentation.components.QlOutlineButton
import com.rds.questlog.presentation.popup.Popup

@Composable
fun ReaderScreen(articleId: Long, resumeFrom: String, navController: NavController, onShowPopup: (Popup) -> Unit) {
    PlaceholderScreen(stringResource(R.string.placeholder_s3), stringResource(R.string.placeholder_s3_subtitle)) {
        QlOutlineButton(stringResource(R.string.placeholder_open_sheet), { onShowPopup(Popup.DisplaySettings) })
        QlOutlineButton(stringResource(R.string.placeholder_back), { navController.popBackStack() })
    }
}
