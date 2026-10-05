package com.rds.questlog.data.mapper

import com.fleeksoft.ksoup.Ksoup
import com.rds.questlog.data.local.entity.ContentNodeEntity
import com.rds.questlog.data.scraper.inlineText
import com.rds.questlog.domain.model.ContentNode
import com.rds.questlog.domain.model.NodeType
import com.rds.questlog.domain.model.TableCell
import org.json.JSONException
import org.json.JSONObject

/**
 * Entity -> model domain. Metadata JSON diurai di sini (data layer) supaya domain tetap bebas org.json:
 * img {"path","alt"} dan table {"html"} (baris/sel dibaca dari HTML tabel).
 */
internal fun ContentNodeEntity.toDomain(): ContentNode {
    val type = NodeType.entries.firstOrNull { it.dbValue == nodeType } ?: NodeType.P
    val meta = metadataJson?.let { json -> runCatching { JSONObject(json) }.getOrNull() }
    return ContentNode(
        id = id,
        sourcePageId = sourcePageId,
        type = type,
        displayOrder = displayOrder,
        text = textContent.orEmpty(),
        imagePath = if (type == NodeType.IMG) meta?.optStringOrNull("path") else null,
        tableRows = if (type == NodeType.TABLE) {
            meta?.optStringOrNull(
                "html",
            )?.let(::parseTableRows).orEmpty()
        } else {
            emptyList()
        },
    )
}

/**
 * Baris tabel: sel th/td (teks saja) dengan colspan dan penanda header. Hanya baris milik tabel ini; baris dari
 * tabel bersarang tidak ikut sehingga tidak ganda. Baris tanpa sel dibuang.
 */
internal fun parseTableRows(html: String): List<List<TableCell>> {
    val table = Ksoup.parse(html).selectFirst("table") ?: return emptyList()
    return table.select("tr")
        .filter { row -> row.parents().firstOrNull { it.tagName().equals("table", ignoreCase = true) } == table }
        .map { row ->
            row.children()
                .filter { it.tagName().equals("th", ignoreCase = true) || it.tagName().equals("td", ignoreCase = true) }
                .map { cell ->
                    TableCell(
                        text = cell.inlineText(),
                        colSpan = cell.attr("colspan").toIntOrNull()?.coerceIn(1, MAX_COLSPAN) ?: 1,
                        isHeader = cell.tagName().equals("th", ignoreCase = true),
                    )
                }
        }
        .filter { it.isNotEmpty() }
}

private const val MAX_COLSPAN = 50

private fun JSONObject.optStringOrNull(key: String): String? = try {
    getString(key).takeIf { it.isNotBlank() }
} catch (_: JSONException) {
    null
}
