package com.rds.questlog.presentation.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rds.questlog.presentation.theme.QuestLogTheme

/**
 * Input teks (tinggi [height]): border abu 22%, fokus → aksen, error → merah (design.md §3.4, radius 4dp).
 * [errorText] (bila ada) tampil di bawah field; [isError] hanya mewarnai border (pesan ditampilkan pemanggil).
 */
@Composable
fun QlTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    errorText: String? = null,
    isError: Boolean = errorText != null,
    enabled: Boolean = true,
    singleLine: Boolean = true,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    height: Dp = 48.dp,
    textStyle: TextStyle = MaterialTheme.typography.bodyLarge,
) {
    val c = QuestLogTheme.colors
    var focused by remember { mutableStateOf(false) }
    val borderColor = when {
        isError -> c.danger
        focused -> c.accentLine
        else -> MaterialTheme.colorScheme.onBackground.copy(alpha = 0.22f)
    }
    Column(modifier) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            enabled = enabled,
            singleLine = singleLine,
            keyboardOptions = keyboardOptions,
            textStyle = textStyle.copy(color = MaterialTheme.colorScheme.onBackground),
            cursorBrush = SolidColor(c.accentLine),
            modifier = Modifier.fillMaxWidth().heightIn(min = height).onFocusChanged { focused = it.isFocused },
            decorationBox = { inner ->
                Box(
                    Modifier
                        .fillMaxWidth()
                        .heightIn(min = height)
                        .border(1.dp, borderColor, RoundedCornerShape(4.dp))
                        .padding(horizontal = 12.dp),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    if (value.isEmpty() && placeholder != null) {
                        Text(placeholder, color = c.iconMuted, style = textStyle)
                    }
                    inner()
                }
            },
        )
        if (errorText != null) {
            Text(
                errorText,
                color = c.danger,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.5.sp),
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}
