package com.rds.questlog.data.repository

import com.rds.questlog.data.local.entity.ContentNodeEntity
import kotlin.math.abs

/**
 * Node pengganti untuk [old] setelah halamannya diunduh ulang: node baru dengan teks yang sama (yang posisinya
 * terdekat bila ada beberapa); bila teks sudah berubah atau kosong (gambar/tabel), node dengan display_order
 * terdekat di halaman yang sama. Null hanya bila halaman baru tidak punya node.
 */
internal fun pickReplacement(old: ContentNodeEntity, fresh: List<ContentNodeEntity>): ContentNodeEntity? {
    val sameText = old.textContent?.takeIf { it.isNotBlank() }?.let { text -> fresh.filter { it.textContent == text } }
    return (sameText.orEmpty().ifEmpty { fresh }).minByOrNull { abs(it.displayOrder - old.displayOrder) }
}
