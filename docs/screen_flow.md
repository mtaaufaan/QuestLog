# Screen Flow — QuestLog

**Status**: Draft v2.0 — diperbarui 2026-09-25 agar sinkron dengan `docs/design.md`, `docs/menu-tree.md`, dan kode prototype `docs/desain/*.dc.html` (ground truth).
**Dibuat oleh**: Architect (Claude) · v1.0 · Direvisi bersama @Taufan · v2.0

> **Catatan revisi v2.0**: v1.0 hanya mencakup 3 layar + 1 bottom sheet dan menyebut filter Game sebagai dropdown. Setelah dicek langsung ke kode prototype (`GameFilterSheet.dc.html`), filter Game ternyata adalah **bottom sheet dengan radio group**, dan ada 7 popup/overlay lain yang belum tercatat di v1.0 (Scrape Mode Sheet, Article Actions Sheet, Delete Article Dialog, Scrape Error Dialog, Scrape Notification, Unlock Sheet). Dokumen ini melengkapi semuanya. Rincian prop/callback tiap komponen ada di `docs/component-contract.md` — dokumen ini fokus ke arsitektur navigasi & state, bukan mengulang daftar prop.

**Keputusan navigasi yang dikunci**:

| Gap | Keputusan |
|-----|-----------|
| Gap 1 — Struktur layar | **Opsi A**: Flat Article List + filter Game (3 layar navigasi total). *(Direvisi: filter tampil sebagai bottom sheet radio — Game Filter Sheet — bukan dropdown menu, lihat Gap 4)* |
| Gap 2 — Scraping progress | **Opsi A**: Background + notifikasi Android + status inline di list |
| Gap 3 — Pengaturan tampilan | **Opsi A**: Bottom Sheet dari Reader (bukan Settings screen terpisah) |
| Gap 4 — Popup & overlay lain *(baru)* | Semua popup/dialog/sheet selain Display Settings (Scrape Mode Sheet, Article Actions Sheet, Delete Article Dialog, Scrape Error Dialog, Scrape Notification, Unlock Sheet, Game Filter Sheet) memakai pola state yang sama: **bukan route NavController**, melainkan state overlay tunggal (mis. `activePopup: PopupType?`) yang di-render di atas layar aktif — persis pola `popup`/`popupId` di `QuestLogApp.dc.html` |

---

## 1. Inventaris Screen & Popup

### 1.1 Layar navigasi (masuk back stack)

| ID | Screen | Route (type-safe) | Composable |
|----|--------|-------------------|------------|
| S1 | Article List | `ArticleList` | `ArticleListScreen` |
| S2 | Add Article | `AddArticle(mode: String, targetArticleId: Long?)` | `AddArticleScreen` |
| S3 | Reader | `Reader(articleId: Long, resumeFrom: String)` | `ReaderScreen` |

**Total layar navigasi: 3.**
**Tidak ada**: Game List screen, Progress screen, Settings screen, Search screen tersendiri (pencarian ada inline di S1 — lihat QL-10, bukan layar terpisah).

### 1.2 Popup / Sheet / Dialog (overlay, di luar back stack)

| ID | Nama | File sumber | Tipe | Dipicu dari |
|----|------|-------------|------|-------------|
| P1 | Scrape Mode Sheet | `ScrapeModeSheet.dc.html` | Bottom sheet | FAB "+" di S1 |
| P2 | Article Actions Sheet | `ArticleActionsSheet.dc.html` | Bottom sheet | Titik-tiga / tekan-tahan baris artikel di S1 |
| P3 | Delete Article Dialog | `DeleteArticleDialog.dc.html` | Modal dialog | "Hapus artikel" di P2 |
| P4 | Scrape Error Dialog | `ScrapeErrorDialog.dc.html` | Modal dialog | Tap artikel status ERROR di S1, atau tap Scrape Notification kind=error |
| P5 | Scrape Notification | `ScrapeNotification.dc.html` | Banner overlay (top, semua layar) | Otomatis saat status scraping berubah |
| P6 | Unlock Sheet | `UnlockSheet.dc.html` | Bottom sheet | Badge tier di header S1, atau link "Upgrade" di S2 saat limit tercapai |
| P7 | Game Filter Sheet | `GameFilterSheet.dc.html` | Bottom sheet (radio group) | Tap chip filter di S1 |
| BS1 | Display Settings Sheet | `DisplaySettingsSheet.dc.html` | Bottom sheet | Ikon pengaturan di S3 |

**Total popup/overlay: 8.** Semua dikelola sebagai state UI, bukan `NavHost` route — lihat §4.

---

## 2. Route Definitions (Type-Safe Navigation Compose)

```kotlin
// navigation/Routes.kt

@Serializable
object ArticleList

@Serializable
data class AddArticle(val mode: String = "new", val targetArticleId: Long? = null)

@Serializable
data class Reader(val articleId: Long, val resumeFrom: String = "last")
```

> Perubahan dari v1.0: `AddArticle` dan `Reader` sekarang membawa parameter sesuai kontrak komponen (`mode`/`targetArticleId` untuk AddArticle; `resumeFrom` untuk Reader, nilai `"last"` atau `"checkpoint"` — lihat `component-contract.md` §2 dan §3), bukan objek kosong.

### NavHost

```kotlin
NavHost(
    navController = navController,
    startDestination = ArticleList
) {
    composable<ArticleList> {
        ArticleListScreen(navController = navController)
    }
    composable<AddArticle> { backStackEntry ->
        val route = backStackEntry.toRoute<AddArticle>()
        AddArticleScreen(
            mode = route.mode,
            targetArticleId = route.targetArticleId,
            navController = navController
        )
    }
    composable<Reader> { backStackEntry ->
        val route = backStackEntry.toRoute<Reader>()
        ReaderScreen(
            articleId = route.articleId,
            resumeFrom = route.resumeFrom,
            navController = navController
        )
    }
}
```

---

## 3. Navigation Flow per Screen

### S1 — Article List Screen

**Entry point**: App launch (start destination, tidak bisa di-pop)
**Back behavior**: Keluar dari app (sistem Android handle)

#### Navigasi keluar dari S1

| User action | Kondisi | Destination | Call |
|-------------|---------|--------------|------|
| Tap ＋ | — | P1 Scrape Mode Sheet | `activePopup = ScrapeMode` |
| Tap artikel | `status = READY` | S3 Reader (`resumeFrom="last"`) | `navController.navigate(Reader(articleId, "last"))` |
| Tap artikel | `status = SCRAPING` | — | Snackbar: "Artikel masih diproses, harap tunggu" |
| Tap artikel | `status = ERROR` | P4 Scrape Error Dialog | `activePopup = ScrapeError(articleId)` |
| Tap "Lanjutkan Baca" pada baris | ada `checkpointNodeId` | S3 Reader (`resumeFrom="checkpoint"`) | `navController.navigate(Reader(articleId, "checkpoint"))` |
| Titik-tiga / tekan-tahan baris | — | P2 Article Actions Sheet | `activePopup = ArticleActions(articleId)` |
| Tap chip filter | — | P7 Game Filter Sheet | `activePopup = GameFilter` |
| Tap badge tier di header | — | P6 Unlock Sheet | `activePopup = Unlock` |

#### State lokal S1

```kotlin
// ArticleListViewModel.kt
data class ArticleListUiState(
    val articles: List<ArticleUiModel> = emptyList(),
    val games: List<GameUiModel> = emptyList(),          // untuk Game Filter Sheet (dengan count per game)
    val selectedGameFilter: Long? = null,                 // null = "Semua Game"
    val query: String = "",
    val isPremium: Boolean = false,
    val isLoading: Boolean = false
)
```

#### Filter Game (revisi — bottom sheet, bukan dropdown)

- Tap chip filter → buka **Game Filter Sheet** (P7), bukan dropdown inline.
- Sheet menampilkan radio list: "Semua Game" (dengan total artikel) + tiap game (dengan count artikel masing-masing) — lihat `component-contract.md` §10 untuk bentuk data `{id, name, count}[]`.
- Pilihan diterapkan lewat `onSelect(id: Long?)`, sheet tertutup otomatis setelah memilih.
- Filter dikombinasikan dengan search (`query`) di level query Room: `WHERE (game_id = ? OR ? IS NULL) AND title LIKE '%' || ? || '%'`.

#### Indikator scraping inline

```
status = READY    → item normal, chevron ›, bisa di-tap; badge "partial" bila ada halaman FAILED; tombol "Lanjutkan Baca" bila ada checkpointNodeId
status = SCRAPING → dot animasi + progress bar tipis + teks "Memproses… X/Y"
status = ERROR    → dot merah + teks "Gagal — tap untuk retry"
```

---

### S2 — Add Article Screen

**Entry**: Dari P1 Scrape Mode Sheet (`mode` terpilih user), atau langsung dari P2 Article Actions Sheet aksi "Tambah halaman" (`mode="append"`, `targetArticleId` terisi otomatis)
**Back behavior**: Pop ke S1 (back gesture / button)

#### Form fields

| Field | Komponen | Validasi |
|-------|----------|----------|
| Tab | `TabRow` (2 tab) | "Buat artikel baru" vs "Lengkapi artikel" |
| Game (mode baru) | `ExposedDropdownMenuBox` + autocomplete | Wajib; pilih yang ada atau buat baru (dibatasi limit tier gratis — QL-12) |
| Judul artikel (mode baru) | `OutlinedTextField` | Wajib |
| Artikel target (mode lengkapi) | Radio list (hanya status READY) | Wajib pilih satu |
| URL halaman | Daftar `OutlinedTextField`, reorder naik/turun, hapus per baris | Min 1, maks 10 URL; wajib `https://`; tidak boleh duplikat (dalam artikel yang sama maupun dengan URL tersimpan); paste multi-baris otomatis terpecah |

#### Navigasi keluar dari S2

| Aksi | Kondisi | Destination | Call |
|------|---------|--------------|------|
| Tap "Simpan & Mulai Unduh" / "Tambahkan Halaman" | Form valid | Pop ke S1 | `navController.popBackStack()` |
| Tap submit | Limit free tier tercapai | — | Button disabled; banner pesan + link "Upgrade" → P6 Unlock Sheet |
| Back / Cancel | — | Pop ke S1 | `navController.popBackStack()` |

#### Side effect setelah Simpan

```
navController.popBackStack()                // kembali ke S1
WorkManager.enqueue(ScrapeArticleWorker)     // background job mulai (per-URL, lihat §5)
S1 otomatis recompose via Flow dari Room     // artikel baru muncul status SCRAPING
P5 Scrape Notification muncul otomatis       // overlay di atas S1
```

#### Free tier enforcement di S2

```kotlin
val gameCount by viewModel.gameCount.collectAsState()
val articleCount by viewModel.articleCountForGame.collectAsState()
val isFreeUser by viewModel.isFreeUser.collectAsState()

val isLimitReached = isFreeUser && (gameCount >= 2 || articleCount >= 5)

Button(
    enabled = formIsValid && !isLimitReached,
    onClick = { viewModel.save() }
) { Text("Simpan") }

if (isLimitReached) {
    Row {
        Text("Limit gratis tercapai (maks. 2 game, 5 artikel/game).")
        TextButton(onClick = { activePopup = Unlock }) { Text("Upgrade") }
    }
}
```

---

### S3 — Reader Screen

**Entry**: Dari S1 (tap artikel `status=READY`, tombol "Lanjutkan Baca", atau tap P5 Scrape Notification kind=done)
**Back behavior**: Pop ke S1, mengirim `nodeId` posisi baca saat ini (untuk auto-resume — lihat `onBack`/`onLeave` di `component-contract.md` §3)
**Argument**: `articleId: Long`, `resumeFrom: 'last' | 'checkpoint'`

#### Navigasi keluar dari S3

| Aksi | Destination | Call |
|------|-------------|------|
| Back / gesture | Pop ke S1 | `navController.popBackStack()` (kirim nodeId via `onLeave`) |
| Tap ⚙ | BS1 Display Settings Sheet | `activePopup = DisplaySettings` |
| Banner partial → "Coba Lagi" | — (in-place) | `viewModel.retryFailedPages(articleId)` |

#### 5 state layar (`viewState`)

Wajib diimplementasikan semua, bukan hanya `success`:

```
success   → konten + toggle mode + checkpoint + banner partial (bila ada)
loading   → spinner tengah
not_found → ikon + judul + deskripsi + tombol "Kembali" → pop ke S1
empty     → ikon + judul + deskripsi + tombol "Kembali" → pop ke S1
db_error  → ikon + judul + deskripsi + tombol "Coba Lagi" → `onRetryLoad` (reload, bukan navigasi)
```

#### Auto-resume (last position) & checkpoint manual

```kotlin
// ReaderScreen.kt
val listState = rememberLazyListState()

// Scroll ke posisi sesuai resumeFrom: 'last' → lastNodeId, 'checkpoint' → checkpointNodeId
LaunchedEffect(resumeNodeId) {
    resumeNodeId?.let { nodeId ->
        val index = contentNodes.indexOfFirst { it.id == nodeId }
        listState.scrollToItem(if (index >= 0) index else fallbackNearestIndex(nodeId, contentNodes))
    }
}

// Simpan posisi baca terakhir saat layar ditutup (auto-resume, terpisah dari checkpoint manual)
DisposableEffect(Unit) {
    onDispose {
        val nodeId = contentNodes.getOrNull(listState.firstVisibleItemIndex)?.id
        if (nodeId != null) viewModel.saveLastPosition(articleId, nodeId)
    }
}

// Set checkpoint manual (menimpa checkpointNodeId lama — hanya 1 aktif per artikel)
fun onSetCheckpoint() {
    val nodeId = contentNodes.getOrNull(listState.firstVisibleItemIndex)?.id
    nodeId?.let { viewModel.setCheckpoint(articleId, it) }
}
```

#### State lokal S3

```kotlin
data class ReaderUiState(
    val article: ArticleUiModel? = null,
    val contentNodes: List<ContentNodeUiModel> = emptyList(),
    val viewState: String = "loading",          // success | loading | not_found | empty | db_error
    val readMode: String = "seamless",          // seamless | paged, persisted per-artikel
    val fontSize: Int = 16,                     // app-wide, dari DataStore
    val darkMode: Boolean = false,              // app-wide, dari DataStore
    val resumeNodeId: Long? = null,
    val hasFailedPages: Boolean = false
)
```

---

### BS1 — Display Settings Sheet

**Bukan layar navigasi.** Dikelola via state overlay yang sama dengan popup lain (§4), scope pengaturan **app-wide** (berlaku ke semua artikel), bukan per-artikel.

**Entry**: Tap ⚙ di topbar ReaderScreen
**Dismiss**: Swipe down atau tap di luar sheet

```kotlin
// Di dalam ReaderScreen.kt, memakai activePopup dari shared UI state (bukan sheetState lokal)
if (activePopup == Popup.DisplaySettings) {
    ModalBottomSheet(onDismissRequest = { activePopup = null }) {
        DisplaySettingsSheet(
            fontSize = readerState.fontSize,
            darkMode = readerState.darkMode,
            onFontSizeChange = viewModel::setFontSize,
            onDarkModeToggle = viewModel::toggleDarkMode,
            onClose = { activePopup = null }
        )
    }
}
```

| Control | Range / State | Storage |
|---------|--------------|---------|
| Ukuran Teks (− / ＋) | 12–24 sp, step 2 | `DataStore<Preferences>` key: `FONT_SIZE_SP` |
| Mode Gelap toggle | on/off | `DataStore<Preferences>` key: `DARK_MODE_OVERRIDE` |

---

## 4. Arsitektur Popup/Overlay (Gap 4 — baru di v2.0)

Ketujuh popup selain Display Settings **tidak masuk `NavHost`**, mengikuti pola yang sama persis dengan orkestrasi di `QuestLogApp.dc.html` (state tunggal `popup` + `popupId`). Direkomendasikan satu shared state, bukan boolean terpisah per popup:

```kotlin
sealed interface Popup {
    data object ScrapeMode : Popup
    data class ArticleActions(val articleId: Long) : Popup
    data class DeleteArticle(val articleId: Long) : Popup
    data class ScrapeError(val articleId: Long) : Popup
    data object Unlock : Popup
    data object GameFilter : Popup
    data object DisplaySettings : Popup
}

// Di level NavHost atau shared ViewModel:
var activePopup by remember { mutableStateOf<Popup?>(null) }
```

Alasan satu state bersama (bukan per-popup): mencerminkan aturan navigasi asli — hanya **satu popup aktif dalam satu waktu**, tap area luar/scrim selalu menutup popup yang sedang terbuka, dan Scrape Notification (P5) adalah satu-satunya elemen yang **tidak** memakai state ini karena dia overlay independen yang boleh tampil bersamaan dengan layar apa pun (lihat §5).

| Popup | Trigger dari | Ditutup oleh |
|---|---|---|
| P1 ScrapeMode | FAB "+" di S1 | Pilih mode (lanjut ke S2) / tap scrim |
| P2 ArticleActions | Titik-tiga/tekan-tahan baris S1 | Pilih aksi / tap scrim |
| P3 DeleteArticle | "Hapus artikel" di P2 | Konfirmasi hapus / batal |
| P4 ScrapeError | Tap artikel ERROR di S1, atau tap P5 kind=error | Retry / hapus / tutup |
| P6 Unlock | Badge tier S1, link Upgrade di S2 | Beli/restore sukses / tutup |
| P7 GameFilter | Chip filter S1 | Pilih game / tap scrim |
| BS1 DisplaySettings | Ikon ⚙ di S3 | Tap scrim |

---

## 5. Scraping Progress — Flow Detail (Opsi A)

```
[S2] User tap "Simpan"
  │
  ├─ navController.popBackStack() → kembali ke S1
  │
  └─ WorkManager.enqueue(ScrapeArticleWorker)
       │
       ├─ articles.scraping_status = 'SCRAPING'  ← Room update → S1 recompose
       │
       ├─ [Loop per source_page URL, hanya yang berstatus PENDING — lihat QL-15]
       │    ├─ Fetch HTML → parse → insert content_nodes
       │    ├─ source_pages.status = 'DONE' | 'FAILED' (+ reason bila FAILED)
       │    └─ Update P5 Scrape Notification (overlay, independen dari activePopup):
       │         kind='progress' → "QuestLog — Mengunduh… halaman X dari Y"
       │
       └─ Selesai semua URL:
            ├─ articles.is_scraping_done = 1
            ├─ articles.scraping_status = 'READY' (semua sukses) atau tetap 'READY' dengan flag partial (sebagian gagal) atau 'ERROR' (semua gagal)
            └─ P5 notifikasi kind='done' | 'partial' | 'error':
                 - done    → tap → S3 Reader
                 - partial → tap → S3 Reader (dengan banner partial + retry, lihat QL-15)
                 - error   → tap → S1 lalu buka P4 Scrape Error Dialog
```

Retry (baik dari P4 di S1 maupun banner partial di S3) hanya mengulang halaman berstatus `FAILED → PENDING`; halaman `DONE` tidak disentuh.

---

## 6. Layar yang Secara Eksplisit Tidak Dibuat (MVP)

| Layar | Alasan |
|-------|--------|
| Game List Screen | Opsi A: diganti Game Filter Sheet (bottom sheet) di S1 |
| Progress Screen | Opsi A: diganti notifikasi (P5) + status inline di S1 |
| Settings Screen | Opsi A: diganti bottom sheet (BS1) di S3 |
| Onboarding / Splash | Di luar MVP scope |
| Paywall / Purchase Screen tersendiri | Diganti Unlock Sheet (P6) — bottom sheet, bukan layar penuh; batas free tier tampil sebagai pesan inline di S2 |
| Search Screen | Tidak perlu layar terpisah — search bar sudah inline di S1 (QL-10) |

---

## 7. Dependensi Compose Navigation

```kotlin
// build.gradle.kts (app module)
dependencies {
    implementation("androidx.navigation:navigation-compose:2.8.9")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
}

plugins {
    kotlin("plugin.serialization") version "2.0.21"
}
```

---

## 8. Referensi Dokumen Terkait

| Dokumen | Path |
|---------|------|
| PRD | `docs/prd.md` |
| Backlog (dengan pemetaan Sprint & Tahap) | `docs/backlog.md` |
| Ringkasan visual & design system | `docs/design.md` |
| Peta navigasi (naratif) | `docs/menu-tree.md` |
| Kontrak komponen (prop/callback per file) | `docs/component-contract.md` |
| Kode sumber prototype (ground truth) | `docs/desain/*.dc.html` |
| Database Schema | `docs/database_schema.md` |
| Tech Stack | `docs/tech-stack.md` |
| Domain Contract | `docs/domain_contract.md` |
| Error State Model | `docs/error_state_model.md` |
