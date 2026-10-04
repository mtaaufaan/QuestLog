package com.rds.questlog.data.scraper

private const val MAX_PARAGRAPH_CHARS = 400
private const val MAX_SHORT_SENTENCES = 3
private const val MAX_PRE_CHARS = 4000
private const val PRE_BREAK_MIN_CHARS = 1500

private val SENTENCE_BOUNDARY = Regex("(?<=[.!?])\\s+")

/**
 * Paragraf > 400 karakter DAN > 3 kalimat dipecah pada batas kalimat agar checkpoint lebih granular
 * (tech-stack.md §4). Elemen pertama menjadi node `p`, sisanya `p_cont`.
 */
fun splitParagraph(text: String): List<String> {
    val sentences = text.split(SENTENCE_BOUNDARY)
    if (text.length <= MAX_PARAGRAPH_CHARS || sentences.size <= MAX_SHORT_SENTENCES) return listOf(text)

    val chunks = mutableListOf<String>()
    var current = StringBuilder()
    for (sentence in sentences) {
        if (current.length + sentence.length > MAX_PARAGRAPH_CHARS && current.isNotEmpty()) {
            chunks.add(current.toString().trim())
            current = StringBuilder()
        }
        current.append(sentence).append(' ')
    }
    if (current.isNotEmpty()) chunks.add(current.toString().trim())
    return chunks
}

/**
 * Memecah blok `<pre>` raksasa (mis. teks GameFAQs) menjadi beberapa node ≤ [maxChars], dipotong di batas baris
 * (lebih disukai di baris kosong). Indentasi dipertahankan; potongan kosong dibuang.
 */
fun chunkPreformatted(text: String, maxChars: Int = MAX_PRE_CHARS): List<String> {
    val chunks = mutableListOf<String>()
    val current = StringBuilder()
    fun flush() {
        val chunk = current.toString().trim('\n', '\r').trimEnd()
        if (chunk.isNotBlank()) chunks.add(chunk)
        current.clear()
    }
    for (line in text.lines()) {
        if (current.isNotEmpty() && current.length + line.length + 1 > maxChars) flush()
        if (line.isBlank() && current.length >= PRE_BREAK_MIN_CHARS) {
            flush()
        } else {
            current.append(line).append('\n')
        }
    }
    flush()
    return chunks
}
