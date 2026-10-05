package com.rds.questlog.domain.model

/**
 * Penanda gaya inline (tebal/miring) di dalam teks blok konten. Memakai karakter area pribadi Unicode yang tidak
 * pernah muncul di teks halaman, jadi tidak bentrok dengan isi (mis. tanda bintang pada seni ASCII). Teks tanpa
 * penanda tetap teks biasa, sehingga artikel lama tetap terbaca.
 */
object InlineMarkup {
    const val BOLD_ON = ''
    const val BOLD_OFF = ''
    const val ITALIC_ON = ''
    const val ITALIC_OFF = ''

    private val MARKERS = Regex("[-]")

    /** Teks polos tanpa penanda gaya. */
    fun strip(text: String): String = MARKERS.replace(text, "")

    /**
     * Menyeimbangkan penanda di potongan-potongan hasil pemecahan satu teks: gaya yang masih terbuka di akhir sebuah
     * potongan ditutup, lalu dibuka lagi di awal potongan berikutnya, sehingga tebal/miring tidak putus di tengah.
     */
    fun rebalance(chunks: List<String>): List<String> {
        var bold = false
        var italic = false
        return chunks.map { chunk ->
            val prefix = (if (bold) "$BOLD_ON" else "") + (if (italic) "$ITALIC_ON" else "")
            chunk.forEach { char ->
                when (char) {
                    BOLD_ON -> bold = true
                    BOLD_OFF -> bold = false
                    ITALIC_ON -> italic = true
                    ITALIC_OFF -> italic = false
                }
            }
            prefix + chunk + (if (bold) "$BOLD_OFF" else "") + (if (italic) "$ITALIC_OFF" else "")
        }
    }
}
