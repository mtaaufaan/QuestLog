package com.rds.questlog.domain.usecase.article

private val HTTPS_CANDIDATE = Regex("https://[^\\s<>\"\u00A0]+", RegexOption.IGNORE_CASE)
private val VALID_URL = Regex("^https://\\S+\\.\\S+$", RegexOption.IGNORE_CASE)
private const val SCHEME_LENGTH = 8 // panjang "https://"
private const val TRAILING_PUNCTUATION = ".,;:!?'\"”’"

/**
 * Mengambil URL dari teks yang dibagikan lewat menu "Bagikan" (QL-21). Teks dari browser bisa memuat judul atau kata
 * lain di samping URL, jadi URL dicari di dalam teks: yang diambil adalah URL `https://` valid PERTAMA (host harus
 * bertitik, sama dengan validasi input manual). Tanda baca di ujung ("…lihat https://a.com/x.") dan kurung penutup
 * yang tidak berpasangan dibuang; kurung yang berpasangan (mis. `/wiki/Foo_(bar)`) dipertahankan, dan skema
 * selalu menjadi huruf kecil. Murni Kotlin tanpa Android dan tanpa DI, sehingga bisa dibuat langsung
 * (`ParseSharedUrlUseCase()`).
 */
class ParseSharedUrlUseCase {

    /** URL valid pertama dari [text], atau null bila tidak ada (teks kosong, hanya `http://`, atau bukan URL). */
    operator fun invoke(text: String?): String? =
        HTTPS_CANDIDATE.findAll(text.orEmpty()).map { clean(it.value) }.firstOrNull { VALID_URL.matches(it) }
            ?.let { "https://" + it.drop(SCHEME_LENGTH) }

    private fun clean(candidate: String): String {
        var url = candidate
        while (url.isNotEmpty()) {
            val last = url.last()
            val trim = last in TRAILING_PUNCTUATION || (
                last in CLOSERS && url.count {
                    it == last
                } > url.count { it == OPENER[last] }
                )
            if (!trim) break
            url = url.dropLast(1)
        }
        return url
    }

    private companion object {
        val OPENER = mapOf(')' to '(', ']' to '[', '}' to '{')
        val CLOSERS = OPENER.keys
    }
}
