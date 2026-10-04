package com.rds.questlog.data.mapper

import com.fleeksoft.ksoup.Ksoup
import com.rds.questlog.data.local.entity.ContentNodeEntity
import com.rds.questlog.domain.model.ContentNode
import com.rds.questlog.domain.model.NodeType
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

/** Baris tabel (sel th/td per tr, teks saja); baris tanpa sel dibuang. */
internal fun parseTableRows(html: String): List<List<String>> = Ksoup.parse(html).select("tr")
    .map { row -> row.select("th, td").map { it.text().trim() } }
    .filter { it.isNotEmpty() }

private fun JSONObject.optStringOrNull(key: String): String? = try {
    getString(key).takeIf { it.isNotBlank() }
} catch (_: JSONException) {
    null
}
