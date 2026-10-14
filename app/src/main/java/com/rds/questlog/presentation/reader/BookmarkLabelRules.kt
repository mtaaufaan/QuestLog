package com.rds.questlog.presentation.reader

/** Panjang maksimum label bookmark (SRS-FR-19). */
const val MAX_BOOKMARK_LABEL = 60

private const val MAX_RECENT = 3
private const val MAX_SUGGESTIONS = 7
private val BASE_SUGGESTIONS = listOf("Peti", "Boss", "Mini Medal", "Sidequest", "Item")

/** Satu chip saran di Bookmark Label Dialog; [isRecent] = label terbaru (diawali ↺), [selected] = sama dengan isian. */
data class LabelSuggestion(val text: String, val isRecent: Boolean, val selected: Boolean)

/** Chip saran: maksimal 3 label terbaru lebih dulu, lalu saran baku yang belum ada; total maksimal 7. */
fun labelSuggestions(recent: List<String>, current: String): List<LabelSuggestion> {
    val rec = recent.filter { it.isNotBlank() }.take(MAX_RECENT)
    val all = (rec + BASE_SUGGESTIONS.filter { base -> rec.none { it.equals(base, ignoreCase = true) } })
        .take(MAX_SUGGESTIONS)
    return all.mapIndexed { index, text ->
        LabelSuggestion(text, isRecent = index < rec.size, selected = current.trim().equals(text, ignoreCase = true))
    }
}

/**
 * Isian baru setelah mengetuk [suggestion]: menggantikan isian bila kosong, sudah berupa saran/label terbaru, atau
 * saran itu sendiri; selain itu ditambahkan ("Isian — Saran"). Hasil dipotong ke [MAX_BOOKMARK_LABEL].
 */
fun applySuggestion(suggestion: LabelSuggestion, current: String, recent: List<String>): String {
    val typed = current.trim()
    val known = BASE_SUGGESTIONS + recent.take(MAX_RECENT)
    val replace = suggestion.selected || typed.isEmpty() || typed in known
    return (if (replace) suggestion.text else "$typed — ${suggestion.text}").take(MAX_BOOKMARK_LABEL)
}
