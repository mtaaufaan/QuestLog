package com.rds.questlog.presentation.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rds.questlog.R
import com.rds.questlog.presentation.theme.QuestLogTheme

/** Kolom pencarian (S0 dan S1): ikon, placeholder, dan tombol hapus saat terisi. */
@Composable
fun QlSearchField(query: String, placeholder: String, onQuery: (String) -> Unit, modifier: Modifier = Modifier) {
    val c = QuestLogTheme.colors
    val focusManager = LocalFocusManager.current
    BasicTextField(
        value = query,
        onValueChange = onQuery,
        singleLine = true,
        textStyle = MaterialTheme.typography.bodyLarge.copy(
            fontSize = 15.sp,
            color = MaterialTheme.colorScheme.onBackground,
        ),
        cursorBrush = SolidColor(c.accentLine),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
        modifier = modifier.fillMaxWidth(),
        decorationBox = { inner ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .border(1.dp, MaterialTheme.colorScheme.onBackground.copy(alpha = 0.22f), RoundedCornerShape(4.dp))
                    .padding(start = 12.dp, end = if (query.isEmpty()) 12.dp else 8.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(QlIcons.Search, contentDescription = null, tint = c.textTertiary, modifier = Modifier.size(18.dp))
                Box(Modifier.weight(1f)) {
                    if (query.isEmpty()) {
                        Text(
                            text = placeholder,
                            color = c.iconMuted,
                            style = MaterialTheme.typography.bodyLarge.copy(fontSize = 15.sp),
                        )
                    }
                    inner()
                }
                if (query.isNotEmpty()) {
                    ClearButton { onQuery("") }
                }
            }
        },
    )
}

@Composable
private fun ClearButton(onClick: () -> Unit) {
    val c = QuestLogTheme.colors
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    Box(
        modifier = Modifier
            .size(28.dp)
            .clip(CircleShape)
            .background(if (pressed) c.hover else Color.Transparent)
            .clickable(interactionSource = source, indication = null, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            QlIcons.Close,
            contentDescription = stringResource(R.string.article_list_search_clear),
            tint = c.textSecondary,
            modifier = Modifier.size(16.dp),
        )
    }
}

/** FAB "+" di kanan bawah (S0 dan S1). */
@Composable
fun QlAddFab(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val c = QuestLogTheme.colors
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    Box(
        modifier = modifier
            .size(60.dp)
            .shadow(4.dp, CircleShape, ambientColor = c.snackbar, spotColor = c.snackbar)
            .clip(CircleShape)
            .background(if (pressed) c.accentTintActive else MaterialTheme.colorScheme.background)
            .border(1.dp, c.accentLine, CircleShape)
            .clickable(interactionSource = source, indication = null, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            QlIcons.Plus,
            contentDescription = stringResource(R.string.article_list_add),
            tint = Color(0xFFA06F24),
            modifier = Modifier.size(26.dp),
        )
    }
}

/** Toast: kiri 16dp, kanan 92dp (menyisakan ruang FAB), naik 12dp + fade 0,22 dtk (qlUp). */
@Composable
fun QlToast(message: String?, modifier: Modifier = Modifier) {
    val c = QuestLogTheme.colors
    // Simpan teks terakhir agar tetap terbaca selama animasi keluar.
    var lastMessage by remember { mutableStateOf("") }
    LaunchedEffect(message) { if (message != null) lastMessage = message }
    val offsetPx = with(LocalDensity.current) { 12.dp.roundToPx() }
    AnimatedVisibility(
        visible = message != null,
        modifier = modifier,
        enter = fadeIn(tween(220)) + slideInVertically(tween(220)) { offsetPx },
        exit = fadeOut(tween(150)),
    ) {
        Box(
            Modifier
                .padding(start = 16.dp, end = 92.dp, bottom = 32.dp)
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .shadow(4.dp, RoundedCornerShape(4.dp))
                .background(c.snackbar, RoundedCornerShape(4.dp))
                .padding(horizontal = 16.dp, vertical = 12.dp),
            contentAlignment = Alignment.CenterStart,
        ) {
            Text(
                text = message ?: lastMessage,
                color = c.snackbarText,
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.5.sp, lineHeight = 19.sp),
            )
        }
    }
}
