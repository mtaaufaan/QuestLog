package com.rds.questlog.presentation.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.dp

/** Ikon garis custom (design.md §3.3); path disalin dari kode desain. Beri warna lewat `Icon(tint = ...)`. */
object QlIcons {
    val Search = lineIcon("Search", 1.7f, circle(11f, 11f, 8f), "m21 21-4.3-4.3")
    val Close = lineIcon("Close", 1.8f, "M18 6 6 18", "m6 6 12 12")
    val ChevronDown = lineIcon("ChevronDown", 1.8f, "m6 9 6 6 6-6")
    val ChevronRight = lineIcon("ChevronRight", 1.6f, "m9 18 6-6-6-6")
    val Plus = lineIcon("Plus", 1.6f, "M5 12h14", "M12 5v14")
    val PlusSmall = lineIcon("PlusSmall", 2f, "M5 12h14", "M12 5v14")
    val ArrowLeft = lineIcon("ArrowLeft", 1.7f, "m12 19-7-7 7-7", "M19 12H5")
    val Check = lineIcon("Check", 1.8f, "M20 6 9 17l-5-5")
    val ChevronUp = lineIcon("ChevronUp", 2f, "m18 15-6-6-6 6")
    val ChevronDownSmall = lineIcon("ChevronDownSmall", 2f, "m6 9 6 6 6-6")
    val FilePlus = lineIcon(
        "FilePlus",
        1.6f,
        "M15 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V7Z",
        "M14 2v4a2 2 0 0 0 2 2h4",
        "M9 15h6",
        "M12 18v-6",
    )
    val Lock = lineIcon(
        "Lock",
        2f,
        "M5,11h14a2,2 0 0 1 2,2v7a2,2 0 0 1 -2,2H5a2,2 0 0 1 -2,-2v-7a2,2 0 0 1 2,-2z",
        "M7 11V7a5 5 0 0 1 10 0v4",
    )
    val Bookmark = lineIcon("Bookmark", 1.7f, BOOKMARK_PATH)
    val BookOpen = lineIcon(
        "BookOpen",
        1.7f,
        "M2 3h6a4 4 0 0 1 4 4v14a3 3 0 0 0-3-3H2z",
        "M22 3h-6a4 4 0 0 0-4 4v14a3 3 0 0 1 3-3h7z",
    )
    val BookPlus = lineIcon(
        "BookPlus",
        1.7f,
        "M4 19.5v-15A2.5 2.5 0 0 1 6.5 2H20v20H6.5a2.5 2.5 0 0 1 0-5H20",
        "M12 7v6",
        "M9 10h6",
    )
    val Trash = lineIcon(
        "Trash",
        1.7f,
        "M3 6h18",
        "M19 6v14c0 1-1 2-2 2H7c-1 0-2-1-2-2V6",
        "M8 6V4c0-1 1-2 2-2h4c1 0 2 1 2 2v2",
    )
    val Refresh = lineIcon(
        "Refresh",
        1.8f,
        "M3 12a9 9 0 0 1 9-9 9.75 9.75 0 0 1 6.74 2.74L21 8",
        "M21 3v5h-5",
        "M21 12a9 9 0 0 1-9 9 9.75 9.75 0 0 1-6.74-2.74L3 16",
        "M8 16H3v5",
    )

    /** Tiga titik vertikal (terisi). */
    val MoreVert = ImageVector.Builder("MoreVert", 24.dp, 24.dp, 24f, 24f).apply {
        listOf(5f, 12f, 19f).forEach { cy ->
            addPath(PathParser().parsePathString(circle(12f, cy, 1.6f)).toNodes(), fill = SolidColor(Color.Black))
        }
    }.build()

    /** Bookmark berwarna tetap (emas) untuk tombol "Lanjutkan Baca"; tampilkan dengan `Image`, bukan `Icon`. */
    val BookmarkFilled = ImageVector.Builder("BookmarkFilled", 24.dp, 24.dp, 24f, 24f).apply {
        addPath(
            pathData = PathParser().parsePathString(BOOKMARK_PATH).toNodes(),
            fill = SolidColor(Color(0xFFE1AD66)),
            stroke = SolidColor(Color(0xFFA06F24)),
            strokeLineWidth = 1.6f,
            strokeLineJoin = StrokeJoin.Round,
        )
    }.build()
}

private const val BOOKMARK_PATH = "m19 21-7-4-7 4V5a2 2 0 0 1 2-2h10a2 2 0 0 1 2 2v16z"

private fun circle(cx: Float, cy: Float, r: Float) = "M${cx - r},${cy}a$r,$r 0 1,0 ${2 * r},0a$r,$r 0 1,0 ${-2 * r},0z"

private fun lineIcon(name: String, strokeWidth: Float, vararg paths: String): ImageVector =
    ImageVector.Builder(name, 24.dp, 24.dp, 24f, 24f).apply {
        paths.forEach { d ->
            addPath(
                pathData = PathParser().parsePathString(d).toNodes(),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = strokeWidth,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            )
        }
    }.build()
