# QuestLog — Kontrak Komponen

2026-09-25 · Disusun bersama @Taufan
Diekstrak langsung dari kode prototype (`docs/desain/*.dc.html`) — bukan dari ringkasan `design.md` — sehingga setiap prop, tipe, dan callback di bawah ini persis sama dengan yang ada di file sumber. Dokumen ini adalah acuan saat menerjemahkan tiap layar/popup ke Composable + ViewModel, supaya tidak perlu membuka file `.dc.html` mentah lagi.

Cara pakai: tiap baris "Props" jadi parameter `@Composable fun XScreen(...)`, tiap baris "Callback" jadi lambda yang di-invoke ViewModel/NavController. Nama persis dipertahankan (mis. `onOpenUnlock`) supaya mudah ditelusuri balik ke prototype kalau ada keraguan perilaku.

---

## 0. Model Data Global

Diambil dari state di `QuestLogApp.dc.html` (shell/orchestrator) — ini bentuk data yang mengalir ke semua layar:

```ts
type Game = {
  id: number
  name: string
}

type Page = {
  url: string
  status: 'PENDING' | 'DONE' | 'FAILED'
  reason?: string          // alasan gagal, mis. "HTTP 404 — halaman tidak ditemukan"
}

type Article = {
  id: number
  gameId: number
  title: string
  status: 'SCRAPING' | 'READY' | 'ERROR'
  pages: Page[]
  checkpointNodeId?: number   // penanda manual — 1 per artikel
  lastNodeId?: number         // posisi baca terakhir (auto-resume)
  lastRead?: string           // label relatif, mis. "dibaca 2 jam lalu"
  readMode?: 'seamless' | 'paged'
}

type Notif = {
  kind: 'progress' | 'done' | 'partial' | 'error'
  title: string
  current: number
  total: number
  failed: number
  id: number
}
```

Catatan penting dari logika shell (bukan sekadar tampilan, ini aturan yang harus direplikasi di domain layer):
- **`checkpointNodeId` dan `lastNodeId` adalah dua field terpisah** — jangan digabung. Reader menerima parameter `resumeFrom: 'last' | 'checkpoint'` untuk tahu mau scroll ke posisi mana; dikirim tergantung user tap baris biasa (→ `'last'`) atau tombol "Lanjutkan Baca" (→ `'checkpoint'`).
- **Badge tier pakai `gamesUsed` = jumlah Game**, bukan jumlah artikel. Limitnya di enforce di 2 game / 5 artikel per game, tapi badge di header S1 dan prop `UnlockSheet.gamesUsed` sama-sama menghitung Game.
- **Retry di ScrapeErrorDialog mengulang SEMUA halaman FAILED milik artikel itu** (`status: FAILED → PENDING`, artikel kembali ke `SCRAPING`). Retry di Reader (banner partial → "Coba Lagi") juga sama secara logika — mengulang semua halaman FAILED artikel yang sedang dibuka. Halaman berstatus `DONE` tidak pernah di-scrape ulang.
- **Durasi notifikasi berbeda per kind**: toast biasa (snackbar) ±2.6 detik; notifikasi `progress` ±2.6 detik lalu auto-hide (update `current` berjalan tanpa reset timer selama masih progress artikel yang sama); notifikasi `done`/`partial`/`error` bertahan lebih lama, ±3.8 detik, karena butuh dibaca sebelum hilang.
- **Aksi di Article Actions Sheet dikirim sebagai satu enum**: `'resume' | 'open' | 'append' | 'delete'` — cocokkan langsung ke `sealed interface ArticleAction` di Kotlin, jangan pecah jadi 4 callback terpisah.
- Saat user membuat Game baru lewat Add Article, `gameId` baru di-generate di sisi klien lalu artikel disimpan dengan referensi itu — konsisten dengan backlog QL-9 (`game_id`, bukan teks bebas).

---

## 1. S1 — Article List
**File sumber:** `ArticleList.dc.html`

| Props | Tipe | Keterangan |
|---|---|---|
| `games` | `Game[]` | Seluruh game tersimpan |
| `articles` | `Article[]` | Seluruh artikel (sudah difilter/di-search di level shell atau di ViewModel) |
| `isPremium` | `boolean` (default `false`) | Menentukan tampilan badge tier & apakah limit berlaku |
| `query` | `string` | Nilai search bar saat ini (controlled) |
| `filterGameId` | `number \| null` | Game yang sedang difilter; `null` = "Semua Game" |
| `snackbar` | `string \| null` | Pesan toast yang sedang tampil (dikontrol dari luar, bukan state lokal layar) |

| Callback | Signature | Trigger |
|---|---|---|
| `onQuery` | `(v: string) => void` | Tiap ketikan di search bar (real-time, sesuai QL-10) |
| `onOpenFilter` | `() => void` | Tap chip filter → buka Game Filter Sheet |
| `onArticleTap` | `(a: Article) => void` | Tap baris artikel — perilaku beda per status (lihat §0 shell logic: READY→Reader, SCRAPING→snackbar, ERROR→Scrape Error Dialog) |
| `onResume` | `(a: Article) => void` | Tap tombol "Lanjutkan Baca" pada baris → Reader dengan `resumeFrom='checkpoint'` |
| `onOpenActions` | `(a: Article) => void` | Titik-tiga / tekan-tahan baris → Article Actions Sheet |
| `onAdd` | `() => void` | Tap FAB "+" → Scrape Mode Sheet |
| `onOpenUnlock` | `() => void` | Tap badge tier di header → Unlock Sheet |

---

## 2. S2 — Add Article
**File sumber:** `AddArticle.dc.html`

| Props | Tipe | Keterangan |
|---|---|---|
| `mode` | `'new' \| 'append'` (default `'new'`) | Menentukan tab aktif saat layar dibuka |
| `isPremium` | `boolean` | Untuk cek limit tier gratis |
| `targetArticleId` | `number \| null` | Terisi otomatis kalau entry dari Article Actions Sheet ("Tambah halaman") |
| `games` | `Game[]` | Untuk autocomplete input Game |
| `articles` | `Article[]` | Untuk daftar radio pilihan artikel di mode "Lengkapi" (filter hanya status READY) |

| Callback | Signature | Trigger |
|---|---|---|
| `onBack` | `() => void` | Tombol kembali / cancel → pop ke S1 |
| `onSave` | `(payload) => void` | Submit form. `payload` berisi mode, gameId/newGameName, title (mode new), targetArticleId (mode append), dan daftar URL — field persis harus dicek ulang di logika `onSave` shell saat implementasi ViewModel |
| `onOpenUnlock` | `() => void` | Tap link "Upgrade" saat limit tier gratis tercapai |

Aturan validasi dari `design.md` §5 yang tidak ada di tipe prop tapi wajib direplikasi di form: URL wajib `https://`, tidak boleh duplikat (dalam artikel yang sama maupun dengan URL tersimpan lain), maksimal 10 URL per artikel, paste multi-URL otomatis terpecah per baris.

---

## 3. S3 — Reader
**File sumber:** `Reader.dc.html`

| Props | Tipe | Keterangan |
|---|---|---|
| `viewState` | `'success' \| 'loading' \| 'not_found' \| 'empty' \| 'db_error'` (default `'success'`) | 5 state layar — implementasikan semua, bukan cuma `success` |
| `fontSize` | `number` (range 12–24, step 2, default 16) | Preferensi app-wide dari Display Settings |
| `darkMode` | `boolean` | Preferensi app-wide, khusus berlaku di Reader & Display Settings |
| `article` | `Article` | Artikel yang sedang dibuka |
| `resumeNodeId` | `number` | Node tujuan auto-scroll saat layar dibuka |
| `resumeFrom` | `'last' \| 'checkpoint'` | Sumber `resumeNodeId` — pengaruh ke label snackbar ("Melanjutkan dari posisi terakhir" vs checkpoint) |
| `snackbar` | `string \| null` | mis. "Checkpoint disimpan" |

| Callback | Signature | Trigger |
|---|---|---|
| `onBack` | `(nodeId) => void` | Kembali ke S1 — **mengirim nodeId posisi baca saat ini**, ini yang jadi `lastNodeId` baru (auto-resume) |
| `onLeave` | `(nodeId) => void` | Terpicu saat user keluar Reader dengan cara lain (mis. lifecycle `onDispose`), payload sama seperti `onBack` — pastikan keduanya menulis ke `lastNodeId`, jangan hanya salah satu |
| `onOpenSettings` | `() => void` | Tap ikon pengaturan → Display Settings Sheet |
| `onSetCheckpoint` | `(nodeId) => void` | Tap tombol bookmark — menimpa `checkpointNodeId` lama (hanya 1 aktif per artikel) |
| `onModeChange` | `(m) => void` | Toggle Seamless ⇄ Per Halaman — tersimpan sebagai `article.readMode` |
| `onRetryFailed` | `() => void` | Tap "Coba Lagi" di banner partial — retry hanya halaman FAILED artikel ini |
| `onRetryLoad` | `() => void` | Tombol aksi di state `db_error` — reload data, bukan navigasi |

---

## 4. Popup — Article Actions Sheet
**File sumber:** `ArticleActionsSheet.dc.html`

| Props | Tipe |
|---|---|
| `article` | `Article` |

| Callback | Signature | Catatan |
|---|---|---|
| `onAction` | `(k: 'resume' \| 'open' \| 'append' \| 'delete') => void` | Satu callback, dibedakan lewat parameter — lihat §0 |
| `onClose` | `() => void` | Tap area luar / tutup |

Item mana yang tampil bersyarat (dari `design.md`): "Lanjutkan dari checkpoint" hanya kalau `article.checkpointNodeId` ada; "Buka artikel" hanya kalau `status === 'READY'`.

---

## 5. Popup — Delete Article Dialog
**File sumber:** `DeleteArticleDialog.dc.html`

| Props | Tipe |
|---|---|
| `articleTitle` | `string` |

| Callback | Signature |
|---|---|
| `onConfirm` | `() => void` |
| `onClose` | `() => void` |

---

## 6. Popup — Scrape Error Dialog
**File sumber:** `ScrapeErrorDialog.dc.html`

| Props | Tipe |
|---|---|
| `article` | `Article` (dipakai untuk daftar halaman gagal + alasan) |

| Callback | Signature |
|---|---|
| `onRetry` | `() => void` — retry semua halaman FAILED artikel ini |
| `onDelete` | `() => void` — buka konfirmasi hapus (bukan hapus langsung) |
| `onClose` | `() => void` |

---

## 7. Popup — Scrape Mode Sheet
**File sumber:** `ScrapeModeSheet.dc.html`

| Props | Tipe |
|---|---|
| `hasArticles` | `boolean` (default `true`) — kalau `false`, opsi "Lengkapi artikel yang ada" harus disabled |

| Callback | Signature |
|---|---|
| `onPick` | `(mode: 'new' \| 'append') => void` — lanjut ke S2 dengan mode terpilih |
| `onClose` | `() => void` |

---

## 8. Overlay — Scrape Notification
**File sumber:** `ScrapeNotification.dc.html`

| Props | Tipe | Default |
|---|---|---|
| `kind` | `'progress' \| 'done' \| 'partial' \| 'error'` | `'progress'` |
| `articleTitle` | `string` | — |
| `current` | `number` | jumlah halaman yang sudah diproses (sukses+gagal) |
| `total` | `number` | total halaman |
| `failedCount` | `number` | — |

| Callback | Signature |
|---|---|
| `onTap` | `() => void` — kalau artikel jadi READY → buka Reader; kalau ERROR → ke S1 lalu buka Scrape Error Dialog (lihat §0 untuk durasi tampil per kind) |

---

## 9. Popup — Unlock Sheet
**File sumber:** `UnlockSheet.dc.html`

| Props | Tipe |
|---|---|
| `gamesUsed` | `number` (0–2) — dipakai buat progress bar/counter perbandingan, bukan jumlah artikel |
| `isPremium` | `boolean` |

| Callback | Signature |
|---|---|
| `onPurchase` | `() => void` — trigger flow Google Play Billing |
| `onRestore` | `() => void` — trigger `queryPurchasesAsync()`; layar sama menampilkan state sukses untuk kedua aksi |
| `onClose` | `() => void` |

---

## 10. Popup — Game Filter Sheet
**File sumber:** `GameFilterSheet.dc.html`

| Props | Tipe |
|---|---|
| `games` | `{ id: number, name: string, count: number }[]` — `count` = jumlah artikel per game, dihitung di ViewModel sebelum dikirim ke sini |
| `selectedId` | `number \| null` |

| Callback | Signature |
|---|---|
| `onSelect` | `(id: number \| null) => void` — `null` = pilih "Semua Game" |
| `onClose` | `() => void` |

Catatan implementasi: ini **bottom sheet dengan radio group**, bukan dropdown menu — konfirmasi langsung dari kode, bukan interpretasi dari prosa `design.md`.

---

## 11. Popup — Display Settings Sheet
**File sumber:** `DisplaySettingsSheet.dc.html`

| Props | Tipe | Range |
|---|---|---|
| `fontSize` | `number` | 12–24, step 2, default 16 |
| `darkMode` | `boolean` | — |

| Callback | Signature |
|---|---|
| `onFontSizeChange` | `(n: number) => void` |
| `onDarkModeToggle` | `(v: boolean) => void` |
| `onClose` | `() => void` |

Scope: app-wide (`DataStore<Preferences>`), bukan per-artikel — konsisten dengan `screen_flow.md`.

---

## 12. Referensi

| Dokumen | Path |
|---|---|
| Ringkasan visual & design system | `docs/design.md` |
| Peta navigasi | `docs/menu-tree.md` |
| Kode sumber prototype (ground truth) | `docs/desain/*.dc.html` |
| Backlog & acceptance criteria | `docs/backlog.md` |
| PRD | `docs/prd.md` |

**Catatan konsistensi:** `docs/screen_flow.md` (Draft v1.0) tidak mencakup 8 dari 11 file di atas (hanya S1/S2/S3/Display Settings) dan menyebut filter game sebagai dropdown — sudah dikonfirmasi keliru dibanding kode sumber. Jadikan dokumen ini + `menu-tree.md` sebagai acuan, bukan `screen_flow.md`, sampai dokumen itu diperbarui.
