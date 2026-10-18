package com.rds.questlog.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.rds.questlog.R
import com.rds.questlog.presentation.theme.CormorantGaramond
import com.rds.questlog.presentation.theme.QuestLogTheme
import java.io.File

private const val INITIAL_SCALE = 0.5f
private val MIN_TOUCH = 48.dp
private val LARGE_LOGO = 60.dp

/** Badge di pojok kotak logo kosong: ＋ (premium), gembok (gratis), atau tanpa badge (premium hilang). */
enum class LogoBadge { PLUS, LOCK, NONE }

/**
 * Logo game (S0 [size] 52dp, S1 64dp): gambar kustom bila [logoPath] (relatif ke `filesDir`) terisi, selain itu huruf
 * awal [name] dengan [badge] sesuai status premium. Klik hanya untuk logo ini (tidak diteruskan ke baris game).
 */
@Composable
fun GameLogo(
    name: String,
    logoPath: String?,
    size: Dp,
    cornerRadius: Dp,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    badge: LogoBadge = LogoBadge.PLUS,
) {
    val c = QuestLogTheme.colors
    val shape = RoundedCornerShape(cornerRadius)
    val description = stringResource(R.string.game_logo_description, name)
    Box(
        modifier = modifier
            .widthIn(min = MIN_TOUCH)
            .heightIn(min = MIN_TOUCH)
            .semantics {
                contentDescription = description
                role = Role.Button
            }
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Box(Modifier.size(size)) {
            Box(
                Modifier.size(size).clip(shape).background(c.hover).border(1.dp, c.divider, shape),
                contentAlignment = Alignment.Center,
            ) {
                if (logoPath != null) {
                    val file = File(LocalContext.current.filesDir, logoPath)
                    AsyncImage(
                        model = file,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.size(size),
                    )
                } else {
                    LogoInitial(name, size)
                }
            }
            if (logoPath == null && badge != LogoBadge.NONE) {
                val diameter = if (size >= LARGE_LOGO) 18.dp else 16.dp
                Box(
                    Modifier
                        .align(Alignment.BottomEnd)
                        .offset(x = 2.dp, y = 2.dp)
                        .size(diameter)
                        .background(c.surface, CircleShape)
                        .border(1.dp, c.divider, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    val icon = if (badge == LogoBadge.LOCK) QlIcons.Lock else QlIcons.PlusSmall
                    Icon(icon, null, tint = c.textSecondary, modifier = Modifier.size(diameter - 6.dp))
                }
            }
        }
    }
}

/** Huruf awal nama game (kapital) untuk logo kosong; ukuran huruf setengah sisi kotak [size]. */
@Composable
fun LogoInitial(name: String, size: Dp) {
    Text(
        text = name.trim().take(1).uppercase().ifEmpty { "?" },
        color = QuestLogTheme.colors.accentText,
        style = TextStyle(
            fontFamily = CormorantGaramond,
            fontWeight = FontWeight.Medium,
            fontSize = (size.value * INITIAL_SCALE).sp,
        ),
    )
}

@Preview(showBackground = true)
@Composable
private fun GameLogoEmptyPreview() = QuestLogTheme {
    GameLogo("Dragon Quest VII", null, 52.dp, 6.dp, onClick = {})
}

@Preview(showBackground = true)
@Composable
private fun GameLogoLockedPreview() = QuestLogTheme {
    GameLogo("Chrono Cross", null, 64.dp, 8.dp, onClick = {}, badge = LogoBadge.LOCK)
}
