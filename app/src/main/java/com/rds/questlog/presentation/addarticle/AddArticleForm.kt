package com.rds.questlog.presentation.addarticle

import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import com.rds.questlog.presentation.model.ScrapeMode

const val MAX_URLS = 10
const val MAX_GAME_NAME = 100

private const val NO_ID = -1L
private val URL_SEPARATOR = Regex("[\\s,]+")
private val URL_SCHEME = Regex("https?://")

/** Isi form S2 beserta operasinya; semuanya murni (tanpa Compose) agar mudah diuji. */
data class AddArticleForm(
    val mode: ScrapeMode = ScrapeMode.NEW,
    val gameInput: String = "",
    /** Game yang dipilih dari daftar; null bila belum dipilih atau [newGame]. */
    val gameId: Long? = null,
    val newGame: Boolean = false,
    val title: String = "",
    val urls: List<String> = listOf(""),
    val targetId: Long? = null,
    /** Error yang baru tampil setelah user mencoba menyimpan (kosong, game, judul, target). */
    val showErrors: Boolean = false,
    val saving: Boolean = false,
) {
    fun withMode(newMode: ScrapeMode) = copy(mode = newMode, showErrors = false)

    /** Mengetik di field game membatalkan pilihan game sebelumnya. */
    fun withGameInput(text: String) = copy(gameInput = text, gameId = null, newGame = false)

    fun withPickedGame(id: Long, name: String) = copy(gameId = id, gameInput = name, newGame = false)

    fun withNewGame() = copy(newGame = true, gameInput = normalizeGameName(gameInput))

    fun withTitle(text: String) = copy(title = text)

    fun withTarget(id: Long) = copy(targetId = id)

    /**
     * Mengubah baris URL ke-[index]. Teks yang berisi beberapa URL (dipisah spasi, baris, atau koma) dari paste
     * dipecah menjadi beberapa baris, maksimal [MAX_URLS] baris.
     */
    fun withUrlChanged(index: Int, text: String): AddArticleForm {
        val parts = text.split(URL_SEPARATOR).filter { it.isNotEmpty() }
        // Hanya lonjakan teks (paste); mengetik koma/spasi satu per satu tidak memecah baris.
        val pasted = text.length - urls[index].length > 1
        if (pasted && parts.size > 1 && URL_SCHEME.containsMatchIn(text)) {
            return copy(urls = (urls.take(index) + parts + urls.drop(index + 1)).take(MAX_URLS))
        }
        return copy(urls = urls.mapIndexed { i, url -> if (i == index) text else url })
    }

    fun withUrlAdded() = if (urls.size >= MAX_URLS) this else copy(urls = urls + "")

    fun withUrlRemoved(index: Int) = copy(
        urls = if (urls.size > 1) urls.filterIndexed { i, _ -> i != index } else listOf(""),
    )

    /** Menukar baris [index] dengan tetangganya ([delta] = -1 naik, +1 turun); di luar batas diabaikan. */
    fun withUrlMoved(index: Int, delta: Int): AddArticleForm {
        val target = index + delta
        if (index !in urls.indices || target !in urls.indices) return this
        val moved = urls.toMutableList()
        moved[index] = urls[target]
        moved[target] = urls[index]
        return copy(urls = moved)
    }

    val filledUrlCount: Int get() = urls.count { it.isNotBlank() }
}

/** Menyimpan form saat rotasi layar/proses dimatikan; status `saving` sengaja tidak dipulihkan. */
val AddArticleFormSaver: Saver<AddArticleForm, Any> = listSaver(
    save = {
        listOf(
            it.mode.name,
            it.gameInput,
            it.gameId ?: NO_ID,
            it.newGame,
            it.title,
            ArrayList(it.urls),
            it.targetId ?: NO_ID,
            it.showErrors,
        )
    },
    restore = {
        @Suppress("UNCHECKED_CAST")
        AddArticleForm(
            mode = ScrapeMode.valueOf(it[0] as String),
            gameInput = it[1] as String,
            gameId = (it[2] as Long).takeIf { id -> id != NO_ID },
            newGame = it[3] as Boolean,
            title = it[4] as String,
            urls = it[5] as List<String>,
            targetId = (it[6] as Long).takeIf { id -> id != NO_ID },
            showErrors = it[7] as Boolean,
        )
    },
)
