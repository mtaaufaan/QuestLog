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

    /** Bookmark (pita): fitur premium, TERPISAH dari checkpoint (jangkar). Beri warna lewat `Icon(tint = ...)`. */
    val Bookmark = lineIcon("Bookmark", 1.9f, "m19 21-7-4-7 4V5a2 2 0 0 1 2-2h10a2 2 0 0 1 2 2v16z")

    /** Ke checkpoint: jangkar dengan panah kecil (menggulir ke checkpoint). */
    val AnchorJump = lineIcon(
        "AnchorJump",
        1.7f,
        "M10 21V9",
        "M4 13H2a8 8 0 0 0 16 0h-2",
        circle(10f, 5.5f, 2.5f),
        "M17 3l4 3.5-4 3.5",
        "M21 6.5h-6",
    )

    /** Artikel utama game (bintang). */
    val Star = lineIcon(
        "Star",
        1.7f,
        "M11.525 2.295a.53.53 0 0 1 .95 0l2.31 4.679a2.123 2.123 0 0 0 1.595 1.16l5.166.756a.53.53 0 0 1 " +
            ".294.904l-3.736 3.638a2.123 2.123 0 0 0-.611 1.878l.882 5.14a.53.53 0 0 1-.771.56l-4.618-2.428a2.122 " +
            "2.122 0 0 0-1.973 0L6.396 21.01a.53.53 0 0 1-.77-.56l.881-5.139a2.122 2.122 0 0 0-.611-1.879L2.16 " +
            "9.795a.53.53 0 0 1 .294-.906l5.165-.755a2.122 2.122 0 0 0 1.597-1.16z",
    )

    /** Catatan (berkas bergaris). */
    val Note = lineIcon(
        "Note",
        1.7f,
        "M15 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V7Z",
        "M14 2v4a2 2 0 0 0 2 2h4",
        "M8 13h8",
        "M8 17h5",
    )

    /** Papan klip (tombol Tempel). */
    val Clipboard = lineIcon(
        "Clipboard",
        1.9f,
        "M9 2h6a1 1 0 0 1 1 1v2a1 1 0 0 1-1 1H9a1 1 0 0 1-1-1V3a1 1 0 0 1 1-1z",
        "M16 4h2a2 2 0 0 1 2 2v14a2 2 0 0 1-2 2H6a2 2 0 0 1-2-2V6a2 2 0 0 1 2-2h2",
    )

    /** Bagikan: tiga titik terhubung (penanda URL dari browser). */
    val Share = lineIcon(
        "Share",
        1.9f,
        circle(18f, 5f, 3f),
        circle(6f, 12f, 3f),
        circle(18f, 19f, 3f),
        "m8.6 13.5 6.8 4",
        "m15.4 6.5-6.8 4",
    )

    /** Checkpoint (jangkar): tiang, lengkung, dan cincin; garis saja, beri warna lewat `Icon(tint = ...)`. */
    val Anchor = anchorIcon("Anchor", 1.7f)

    /** Jangkar bergaris tebal untuk ukuran kecil (penanda checkpoint inline 12dp). */
    val AnchorBold = anchorIcon("AnchorBold", 2f)
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

    val ChevronLeft = lineIcon("ChevronLeft", 1.8f, "m15 18-6-6 6-6")
    val Pencil = lineIcon("Pencil", 1.7f, "M12 20h9", "M16.5 3.5a2.12 2.12 0 0 1 3 3L7 19l-4 1 1-4Z")
    val ListBullets = lineIcon(
        "ListBullets",
        1.7f,
        "M8 6h13",
        "M8 12h13",
        "M8 18h13",
        "M3 6h.01",
        "M3 12h.01",
        "M3 18h.01",
    )
    val Sliders = lineIcon(
        "Sliders",
        1.7f,
        "M21 4h-7",
        "M10 4H3",
        "M21 12h-9",
        "M8 12H3",
        "M21 20h-5",
        "M12 20H3",
        "M14 2v4",
        "M8 10v4",
        "M16 18v4",
    )
    val FileX = lineIcon(
        "FileX",
        1.5f,
        "M15 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V7Z",
        "M14 2v4a2 2 0 0 0 2 2h4",
        "m14.5 12.5-5 5",
        "m9.5 12.5 5 5",
    )
    val Inbox = lineIcon(
        "Inbox",
        1.5f,
        "M22 12h-6l-2 3h-4l-2-3H2",
        "M5.45 5.11 2 12v6a2 2 0 0 0 2 2h16a2 2 0 0 0 2-2v-6l-3.45-6.89A2 2 0 0 0 16.76 4H7.24a2 2 0 0 0-1.79 1.11z",
    )
    val AlertTriangle = lineIcon(
        "AlertTriangle",
        1.5f,
        "m21.73 18-8-14a2 2 0 0 0-3.48 0l-8 14A2 2 0 0 0 4 21h16a2 2 0 0 0 1.73-3",
        "M12 9v4",
        "M12 17h.01",
    )

    /** Bookmark berwarna tetap (emas) untuk baris di Bookmark Panel; tampilkan dengan `Image`, bukan `Icon`. */
    val BookmarkGold = ImageVector.Builder("BookmarkGold", 24.dp, 24.dp, 24f, 24f).apply {
        addPath(
            pathData = PathParser().parsePathString("m19 21-7-4-7 4V5a2 2 0 0 1 2-2h10a2 2 0 0 1 2 2v16z").toNodes(),
            fill = SolidColor(Color(0xFFE1AD66)),
            stroke = SolidColor(Color(0xFFA06F24)),
            strokeLineWidth = 1.6f,
            strokeLineJoin = StrokeJoin.Round,
        )
    }.build()

    /** Tiga titik vertikal (terisi). */
    val MoreVert = ImageVector.Builder("MoreVert", 24.dp, 24.dp, 24f, 24f).apply {
        listOf(5f, 12f, 19f).forEach { cy ->
            addPath(PathParser().parsePathString(circle(12f, cy, 1.6f)).toNodes(), fill = SolidColor(Color.Black))
        }
    }.build()
}

private fun anchorIcon(name: String, strokeWidth: Float) =
    lineIcon(name, strokeWidth, "M12 22V8", "M5 12H2a10 10 0 0 0 20 0h-3", circle(12f, 5f, 3f))

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
