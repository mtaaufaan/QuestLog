package com.rds.questlog.presentation.addarticle

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavController
import com.rds.questlog.R
import com.rds.questlog.presentation.components.PlaceholderScreen
import com.rds.questlog.presentation.components.QlOutlineButton

// Parameter sesuai kontrak route (screen_flow.md §2); dipakai saat layar nyata dibangun.
@Suppress("UnusedParameter")
@Composable
fun AddArticleScreen(mode: String, targetArticleId: Long?, navController: NavController) {
    PlaceholderScreen(stringResource(R.string.placeholder_s2), stringResource(R.string.placeholder_s2_subtitle)) {
        QlOutlineButton(stringResource(R.string.placeholder_back), { navController.popBackStack() })
    }
}
