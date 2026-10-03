# QuestLog — Prompt Library untuk Coding (per Sprint × Tahap)

2026-09-26 · Disusun bersama @Taufan

Kumpulan prompt siap-pakai untuk mengerjakan tiap sprint. Satu prompt = satu Tahap (Frontend atau Wiring) dari satu sprint — lihat `docs/backlog.md` §0 untuk peta lengkapnya. Tinggal salin-tempel; acceptance criteria sudah ditempel langsung di tiap prompt supaya tidak perlu bolak-balik buka `backlog.md`.

**Cara pakai:** jalankan Tahap 1 dulu, review hasil visualnya (jalankan di emulator/device, cocokkan ke `design.md`), baru jalankan Tahap 2 pada sesi/percakapan baru — supaya konteksnya tidak tercampur antara "membangun tampilan" dan "menyambung logika".

**Template dasar tiap prompt:**
1. Dokumen yang wajib dibaca dulu
2. Tugas (scope layar/popup, batas Tahap)
3. Acceptance criteria yang harus dipenuhi
4. Batasan teknis/arsitektur
5. Yang TIDAK boleh disentuh di prompt ini

---

## Sprint 0 — Fondasi Proyek

> Sebelum menjalankan prompt ini, pastikan Prasyarat Lingkungan di `backlog.md` §0 sudah terpenuhi (Android Studio, SDK, emulator/device, Git). Akun Google Play Console dan signing keystore boleh ditunda — Sprint 0 memakai `FakeBillingService` sebagai default.

### Sprint 0 · Tahap 1 (Frontend — scaffolding, tema, navigasi)

```
Baca dulu sebelum mulai:
- docs/tech-stack.md (§1 Bahasa & Runtime, §2 UI Framework, §3 Arsitektur, §11 Build Variants,
  §12 Ringkasan Library)
- docs/design.md (§3 Design System)
- docs/screen_flow.md (§2 Route Definitions, §4 Arsitektur Popup/Overlay)
- docs/coding_convention.md

Tugas: bangun scaffolding project Android dari nol, design system dasar di Compose, dan
skeleton navigasi — belum ada fitur, tapi app harus bisa di-run dan kelihatan bentuknya.

Ini Sprint 0 - Tahap 1 (Frontend). Artinya:
- Buat struktur modul presentation/domain/data sesuai tech-stack.md §3.
- build.gradle.kts dengan seluruh dependency & versi persis dari tech-stack.md §12 (Compose
  BOM, Navigation Compose, Hilt, Room, OkHttp, Ksoup, Coil 3, WorkManager, Coroutines,
  DataStore Preferences, Google Play Billing).
- Build variant debug/full/release sesuai tech-stack.md §11 — build debug pakai
  FakeBillingService (toggle premium manual via SharedPreferences), jangan integrasikan
  Google Play Billing asli dulu di prompt ini.
- Compose Theme: warna, tipografi (Cormorant Garamond/Lora/JetBrains Mono via Google Fonts)
  sesuai design.md §3.1-3.2, plus varian dark mode.
- Komponen dasar reusable: tombol outline/ghost/bahaya, input teks, wrapper BottomSheet,
  wrapper Dialog, Snackbar, Badge — sesuai design.md §3.4 (radius, warna, animasi
  qlFade/qlSheet/qlPop).
- NavHost dengan 3 route (ArticleList, AddArticle, Reader) sesuai screen_flow.md §2 — isi
  layarnya boleh placeholder ("Layar S1" dst.), yang penting bisa berpindah dan bertema
  benar.
- Buat sealed interface Popup (ScrapeMode, ArticleActions, DeleteArticle, ScrapeError,
  Unlock, GameFilter, DisplaySettings) sesuai screen_flow.md §4 — belum diisi UI popup
  nyata, cukup skeleton state activePopup di level NavHost/shared state.

Definisi selesai: project bisa di-build & di-run, tampil bertema (parchment/serif, warna
aksen emas #b68235/#7d5411) dan bisa berpindah antar 3 layar placeholder kosong.

Jangan menyentuh: Room schema, Hilt module isi (provideDatabase dll), ktlint/detekt config
— itu di prompt Tahap 2.
```

### Sprint 0 · Tahap 2 (Wiring — skema DB, DI, lint, data dummy bersama)

```
Baca dulu sebelum mulai:
- docs/database_schema.md
- docs/tech-stack.md (§4 Database, §10 Dependency Injection, §11 Build Variants)
- docs/coding_convention.md
- Kode scaffolding hasil Tahap 1 (JANGAN ubah tema/navigasi, hanya tambah)

Tugas: buat skema database penuh, skeleton Hilt, konfigurasi lint, dan modul data dummy
bersama yang dipakai ulang semua sprint fitur berikutnya.

Ini Sprint 0 - Tahap 2 (Wiring). Artinya:
- Buat SELURUH tabel Room sekaligus sesuai database_schema.md: games, articles,
  source_pages, content_nodes, checkpoints, images, app_config — termasuk checkpoints dan
  app_config yang baru dipakai di Sprint 3-4, supaya tidak perlu migration Room bertahap.
- Buat index idx_content_nodes_article_order dan idx_content_nodes_source_page.
- Hilt AppModule dasar: provideDatabase, provideOkHttpClient, provideBillingService (default
  FakeBillingService untuk build debug) sesuai tech-stack.md §10.
- Konfigurasi ktlint/detekt sesuai coding_convention.md.
- Buat modul PreviewData: objek data dummy bersama (2 game, 5 artikel dengan variasi status
  READY/SCRAPING/ERROR, satu partial dengan halaman FAILED) — persis contoh di
  QuestLogApp.dc.html — supaya semua prompt Tahap 1 Sprint 1-4 tinggal reuse dari sini,
  bukan bikin data dummy sendiri-sendiri.

Definisi selesai: `./gradlew build` dan lint check lulus tanpa error, database bisa
diinstansiasi (test kosong/insert dummy berhasil), modul PreviewData bisa diimpor dari
modul presentation.

Batasan arsitektur: domain layer pure Kotlin — tidak boleh import android.*/room.*/
okhttp3.* di domain.
```

---

## Sprint 1 — S1 Article List + Article Actions Sheet + Delete Article Dialog + Scrape Error Dialog + Game Filter Sheet

### Sprint 1 · Tahap 1 (Frontend)

```
Baca dulu sebelum mulai:
- docs/design.md (§3 Design System, §6 S1)
- docs/component-contract.md (§1 S1, §4 Article Actions Sheet, §5 Delete Article Dialog,
  §6 Scrape Error Dialog, §10 Game Filter Sheet, §0 Model Data Global)
- docs/menu-tree.md (bagian S1 dan popup yang menempel)
- docs/tech-stack.md (§2 UI Framework, §3 Arsitektur)
- docs/coding_convention.md

Tugas: bangun Compose UI untuk ArticleListScreen beserta 4 popup yang menempel padanya
(Article Actions Sheet, Delete Article Dialog, Scrape Error Dialog, Game Filter Sheet).

Ini Sprint 1 - Tahap 1 (Frontend penuh). Artinya:
- SEMUA state visual harus tampil dan bisa dipencet: empty state, status READY/SCRAPING/ERROR,
  badge partial, search bar, filter chip, FAB, snackbar, badge tier "X/2 game".
- Gunakan data dummy persis seperti contoh state di QuestLogApp.dc.html (2 game, 5 artikel
  dengan variasi status READY/SCRAPING/ERROR, salah satu partial). Taruh di objek preview
  lokal, JANGAN buat Room/DAO di prompt ini.
- Semua props & callback harus PERSIS sesuai nama di component-contract.md §1/§4/§5/§6/§10
  (onQuery, onOpenFilter, onArticleTap, onResume, onOpenActions, onAdd, onOpenUnlock, dst)
  supaya gampang disambung ke ViewModel asli di Tahap 2.
- Ikuti design tokens dari design.md §3 (warna, font Cormorant Garamond/Lora/JetBrains Mono,
  radius bottom sheet 12px / dialog 8px, animasi qlFade/qlSheet/qlPop) — jangan pakai
  Material default tanpa override.
- Article Actions Sheet: item "Lanjutkan dari checkpoint" hanya tampil kalau ada
  checkpointNodeId; "Buka artikel" hanya kalau status READY (lihat component-contract.md §4).
- Game Filter Sheet adalah BOTTOM SHEET dengan radio group (bukan dropdown) — lihat
  component-contract.md §10.

Acceptance criteria yang harus terpenuhi (dari backlog.md):

QL-10 — Cari artikel berdasarkan judul
- Tersedia search bar di daftar artikel
- Pencarian mencocokkan judul artikel (case-insensitive, partial match)
- Hasil pencarian update secara real-time saat mengetik (terhadap data dummy dulu)

QL-11 — Filter artikel berdasarkan Game
- Tersedia filter (Game Filter Sheet) berisi daftar Game yang sudah dipakai
- Memilih satu Game menampilkan hanya artikel dengan game_id tersebut
- Filter dapat dikombinasikan dengan search (QL-10)
- Ada opsi "Tampilkan semua" untuk reset filter

Jangan menyentuh: domain layer, Room, WorkManager, scraper — itu di prompt Tahap 2 terpisah.
Output: file Composable baru di presentation/articlelist/, tanpa ViewModel nyata dulu
(boleh pakai fake/preview ViewModel atau state lokal).
```

### Sprint 1 · Tahap 2 (Wiring)

```
Baca dulu sebelum mulai:
- docs/database_schema.md
- docs/domain_contract.md
- docs/tech-stack.md (§3 Arsitektur — Clean Architecture, §4 Database)
- docs/error_state_model.md
- Kode Compose ArticleListScreen + 4 popup hasil Tahap 1 (JANGAN ubah UI-nya, hanya disambung)

Tugas: implementasikan domain + data layer supaya ArticleListScreen dari Tahap 1 menampilkan
data nyata, plus scraper minimal untuk satu URL, plus aksi hapus artikel & retry error.

Ini Sprint 1 - Tahap 2 (Wiring logika). Artinya:
- Bangun skema Room: games, articles, content_nodes sesuai database_schema.md.
- Bangun ArticleRepository, GameRepository sesuai interface di domain_contract.md.
- Bangun ScraperEngine untuk SATU URL: ekstrak teks/gambar/heading/list/tabel, buang
  iklan/navigasi/komentar/pop-up.
- Ganti data dummy di ArticleListViewModel dengan Flow dari Room, tetap expose lewat props
  yang SAMA PERSIS dengan yang dipakai UI Tahap 1 (jangan ubah nama/tipe prop/callback).
- Search (QL-10) dan filter game (QL-11) harus query nyata ke Room (WHERE title LIKE ... AND
  game_id = ?), bukan filter di memori.
- Wiring Delete Article Dialog → hapus artikel + content_nodes + checkpoint terkait.
- Wiring Scrape Error Dialog → retry menimpa pages.status FAILED jadi PENDING, artikel
  kembali SCRAPING.

Acceptance criteria yang harus terpenuhi (dari backlog.md):

QL-1 — Input satu URL untuk disimpan offline
- User dapat memasukkan satu URL melalui field input
- Sistem mengekstrak teks, gambar, dan format penting (heading, list, tabel) dari halaman
- Elemen non-konten (iklan, navigasi, komentar, pop-up) dihilangkan dari hasil
- Konten tersimpan lokal dan dapat dibuka kembali tanpa koneksi internet
- Jika URL gagal diakses, sistem menampilkan pesan error yang jelas (bukan crash/hang)

Batasan arsitektur (wajib, dari tech-stack.md): domain layer pure Kotlin, TIDAK boleh
import android.*/room.*/okhttp3.* di domain; ViewModel hanya observe Flow dari use case,
tidak menyentuh Room/OkHttp langsung.
```

---

## Sprint 2 — S2 Add Article + Scrape Mode Sheet + Scrape Notification

### Sprint 2 · Tahap 1 (Frontend)

```
Baca dulu sebelum mulai:
- docs/design.md (§3 Design System, §6 S2, §6 Popup ScrapeModeSheet & ScrapeNotification)
- docs/component-contract.md (§2 S2, §7 Scrape Mode Sheet, §8 Scrape Notification,
  §0 Model Data Global)
- docs/menu-tree.md (bagian S2, Scrape Mode Sheet, Scrape Notification)
- docs/screen_flow.md (§3 S2, §5 Scraping Progress Flow)

Tugas: bangun Compose UI untuk AddArticleScreen (dua tab: Buat artikel baru / Lengkapi
artikel), Scrape Mode Sheet, dan Scrape Notification overlay.

Ini Sprint 2 - Tahap 1 (Frontend penuh). Artinya:
- Tab switcher berfungsi penuh secara visual: mode "Buat baru" (input Game autocomplete +
  judul + daftar URL) dan mode "Lengkapi" (radio list artikel READY + daftar URL tambahan).
- Daftar URL: tambah/hapus/reorder naik-turun, maksimal 10 baris, validasi format https://
  tampil realtime (visual saja dulu, validasi duplikat-terhadap-DB menyusul Tahap 2).
- Paste multi-baris/koma otomatis terpecah jadi beberapa input URL.
- Scrape Mode Sheet: opsi "Lengkapi artikel yang ada" nonaktif kalau hasArticles=false.
- Scrape Notification: 4 varian visual (progress dengan progress bar, done, partial,
  error) sesuai component-contract.md §8 — gunakan data dummy untuk keempatnya.
- Banner limit tier gratis + link "Upgrade" tampil (visual saja, cek limit pakai data dummy
  isPremium/gameCount).
- Semua props/callback persis component-contract.md §2/§7/§8 (mode, targetArticleId, onSave,
  onOpenUnlock, onPick, onTap, dst).

Acceptance criteria yang harus terpenuhi (dari backlog.md, bagian tampilan saja — logika di
Tahap 2):

QL-14 — Dua Mode Scraping: buat baru vs lengkapi artikel
- Saat memulai scraping, user ditampilkan pilihan eksplisit: "Buat artikel baru" atau
  "Lengkapi artikel yang ada"
- Mode "Buat artikel baru": user mengisi judul artikel, memilih game, dan memasukkan daftar
  URL; urutan URL dapat diatur sebelum scraping dimulai
- Mode "Lengkapi artikel yang ada": user memilih artikel dari daftar, lalu memasukkan URL
  tambahan beserta posisinya dalam urutan halaman

Jangan menyentuh: WorkManager, ScraperEngine multi-URL, Room — itu di prompt Tahap 2.
Output: file Composable baru di presentation/addarticle/, presentation/scrapemode/,
presentation/notification/.
```

### Sprint 2 · Tahap 2 (Wiring)

```
Baca dulu sebelum mulai:
- docs/database_schema.md (kolom page_order, source_pages)
- docs/domain_contract.md
- docs/error_state_model.md
- Kode Compose AddArticleScreen + Scrape Mode Sheet + Scrape Notification hasil Tahap 1
  (JANGAN ubah UI-nya, hanya disambung)

Tugas: implementasikan WorkManager scraping multi-URL, validasi nyata, dan enforcement
limit tier gratis.

Ini Sprint 2 - Tahap 2 (Wiring logika). Artinya:
- ScraperEngine menerima list URL, proses satu per satu, tulis content_nodes + page_order
  per segmen, update source_pages.status per URL (PENDING → DONE/FAILED).
- ScrapeArticleWorker (WorkManager) dijalankan setelah submit, artikel berstatus SCRAPING
  sampai semua URL selesai diproses.
- Mode "Lengkapi": page_order dilanjutkan dari nilai maksimum yang sudah ada pada artikel
  target (append-only, tidak boleh disisipkan di tengah).
- Validasi duplikat URL dijalankan SEBELUM scraping dimulai, baik terhadap URL lain dalam
  input yang sama maupun terhadap URL yang sudah tersimpan di artikel manapun.
- Free-tier check: baca gameCount & articleCountForGame via UseCase (bukan query langsung),
  blokir submit + tampilkan pesan kalau limit tercapai (2 game / 5 artikel per game untuk
  user non-premium).
- Wiring Scrape Notification ke status real-time worker (progress current/total, kind
  done/partial/error sesuai hasil akhir).

Acceptance criteria yang harus terpenuhi (dari backlog.md):

QL-2 — Input multi-URL saat menyimpan artikel
- User dapat menambahkan lebih dari satu URL dalam satu proses penyimpanan
- Konten dari semua URL digabung menjadi satu file, sesuai urutan input
- Metadata batas halaman sumber disimpan untuk setiap segmen konten

QL-3 — Tambah halaman ke file yang sudah ada
- Tersedia aksi "Tambah Halaman" pada file yang sudah tersimpan
- Halaman baru selalu ditambahkan di akhir urutan konten (append-only)
- Checkpoint yang sudah ada pada file tersebut tetap valid setelah penambahan halaman

QL-9 — Pilih/buat entitas Game saat simpan artikel
- Saat menyimpan artikel, user diminta memilih Game terkait
- Input menampilkan autocomplete dari daftar Game yang sudah ada
- Jika Game belum ada di daftar, tersedia aksi eksplisit "Buat Game baru: [nama]"
- Nama Game dinormalisasi ringan (trim spasi, title-case) sebelum dicocokkan/disimpan
- Setiap artikel menyimpan referensi ke game_id, bukan teks bebas

QL-14 (lanjutan — validasi)
- Validasi duplikasi URL dijalankan sebelum scraping dimulai di kedua mode

QL-15 (bagian Sprint 2 — notifikasi & retry background)
- Jika sebagian URL berhasil dan sebagian gagal, artikel tetap dapat dibuka dengan konten
  yang tersedia (status tetap READY, bukan ERROR)
- Halaman yang sudah berhasil (DONE) tidak di-scrape ulang saat retry

QL-12 (bagian Sprint 2 — enforcement, bukan Unlock Sheet)
- User free dibatasi maks. 2 game dan maks. 5 artikel per game
- Saat batas tercapai, user melihat pesan jelas + ajakan upgrade (bukan silent fail)
- Artikel/Game yang sudah tersimpan sebelum mencapai batas tetap dapat diakses penuh

Catatan: tombol "Upgrade" cukup buka state activePopup=Unlock (Unlock Sheet-nya sendiri baru
lengkap dengan Billing asli di Sprint 4).

Batasan arsitektur: domain layer pure Kotlin; WorkManager & OkHttp hanya di data layer.
```

---

## Sprint 3 — S3 Reader + Display Settings Sheet

### Sprint 3 · Tahap 1 (Frontend)

```
Baca dulu sebelum mulai:
- docs/design.md (§3 Design System, §6 S3, §6 Popup DisplaySettingsSheet)
- docs/component-contract.md (§3 S3, §11 Display Settings Sheet, §0 Model Data Global)
- docs/menu-tree.md (bagian S3 dan Display Settings Sheet)
- docs/screen_flow.md (§3 S3, §3 BS1)

Tugas: bangun Compose UI untuk ReaderScreen (5 viewState) dan Display Settings Sheet.

Ini Sprint 3 - Tahap 1 (Frontend penuh). Artinya:
- Implementasikan SEMUA 5 viewState: success, loading, not_found, empty, db_error — jangan
  hanya success.
- State success: header (kembali, judul+game, tombol checkpoint, tombol pengaturan),
  progress bar tipis, toggle Seamless/Per Halaman, banner partial + tombol "Coba Lagi",
  render tiap tipe content node (h1/h2/h3/p/li/pre/img/table/break/missing), footer navigasi
  khusus mode Per Halaman, animasi qlMark saat set checkpoint.
- Gunakan artikel dummy dengan campuran tipe content node dan satu halaman FAILED (untuk
  menguji banner partial secara visual).
- Display Settings Sheet: kontrol ukuran teks 12-24sp step 2, toggle dark mode — perubahan
  langsung terlihat di preview (state lokal dulu, DataStore menyusul Tahap 2).
- Props/callback persis component-contract.md §3/§11 (viewState, fontSize, darkMode,
  resumeNodeId, resumeFrom, onBack, onLeave, onSetCheckpoint, onModeChange, onRetryFailed,
  onRetryLoad, onFontSizeChange, onDarkModeToggle).

Acceptance criteria yang harus terpenuhi (tampilan saja — logika di Tahap 2):

QL-4 — Reading mode dasar
- User dapat mengatur ukuran font (minimal 3 tingkat: kecil/sedang/besar)
- User dapat mengaktifkan dark mode / light mode
- Gambar ditampilkan proporsional, tidak overflow dari lebar layar

QL-5 — Mode baca seamless
- Seluruh konten gabungan ditampilkan sebagai satu scroll berkelanjutan
- Ini adalah mode default saat artikel pertama kali dibuka

QL-6 — Mode baca per halaman (tampilan)
- User dapat toggle ke mode per halaman dari mode seamless (dan sebaliknya)
- Tersedia navigasi next/prev antar halaman, indikator posisi (mis. "Halaman 2 dari 5")

Jangan menyentuh: anchor checkpoint nyata, DataStore, auto-resume nyata — itu di Tahap 2.
Output: file Composable baru di presentation/reader/, presentation/displaysettings/.
```

### Sprint 3 · Tahap 2 (Wiring)

```
Baca dulu sebelum mulai:
- docs/database_schema.md (content_nodes, checkpoints)
- docs/domain_contract.md
- docs/error_state_model.md
- Kode Compose ReaderScreen + Display Settings Sheet hasil Tahap 1 (JANGAN ubah UI-nya)

Tugas: implementasikan rendering konten nyata, sistem checkpoint & auto-resume, retry
halaman gagal dari dalam Reader, dan persistensi preferensi tampilan.

Ini Sprint 3 - Tahap 2 (Wiring logika). Artinya:
- Render content_nodes dari Room sesuai urutan page_order, split per halaman untuk mode
  Per Halaman sesuai metadata dari Sprint 2.
- Checkpoint manual: simpan sebagai anchor ke content_node.id (BUKAN posisi pixel/scroll).
  Hanya 1 checkpoint aktif per artikel — set baru menimpa yang lama.
- Fallback checkpoint: kalau anchor sudah tidak ada (mis. karena append halaman baru), cari
  elemen terdekat yang masih ada — jangan error/crash.
- Auto-resume: simpan lastNodeId otomatis saat user keluar dari Reader (DisposableEffect di
  onLeave/onBack), TERPISAH dari checkpointNodeId manual.
- fontSize & darkMode disimpan di DataStore<Preferences>, berlaku app-wide (bukan per
  artikel); readMode (seamless/paged) disimpan per-artikel.
- onRetryFailed: ulang hanya pages.status=FAILED milik artikel yang sedang dibuka, halaman
  DONE tidak disentuh.

Acceptance criteria yang harus terpenuhi (dari backlog.md):

QL-4 (lanjutan)
- Pengaturan tampilan tersimpan sebagai preferensi (tidak reset tiap buka artikel)

QL-6 (lanjutan)
- Konten dipecah sesuai metadata batas halaman sumber (QL-2)
- Preferensi mode tersimpan per artikel (tidak perlu toggle ulang tiap buka)

QL-7 — Buat/perbarui checkpoint manual
- User dapat menandai posisi baca saat ini sebagai checkpoint (1 tombol/aksi)
- Checkpoint disimpan sebagai anchor ke elemen konten (bukan posisi pixel/scroll)
- Hanya ada 1 checkpoint aktif per artikel
- Checkpoint tetap akurat meski ukuran font atau orientasi layar berubah

QL-8 — Lanjutkan dari checkpoint
- Artikel dengan checkpoint tersimpan menampilkan tombol "Lanjutkan Baca" di daftar artikel
- Menekan "Lanjutkan Baca" membuka artikel dan scroll otomatis ke posisi checkpoint
- Jika elemen anchor checkpoint sudah tidak ada, sistem fallback ke elemen terdekat

QL-15 (bagian Sprint 3 — banner partial di dalam Reader)
- Artikel parsial menampilkan indikator jelas bahwa konten belum lengkap
- Tersedia tombol "Coba Lagi" yang hanya mengulang halaman yang gagal

QL-16 — Auto-resume posisi baca saat kembali ke artikel
- Sistem menyimpan posisi baca terakhir secara otomatis saat pengguna meninggalkan layar baca
- Saat artikel dibuka kembali, scroll otomatis ke posisi terakhir dibaca
- Bekerja terpisah dari checkpoint manual
- Bekerja untuk perpindahan antar artikel dalam game yang sama maupun antar game

Batasan arsitektur: domain layer pure Kotlin; DataStore hanya diakses lewat repository/
use case, bukan langsung dari Composable.
```

---

## Sprint 4 — Unlock Sheet + Billing + Hardening

### Sprint 4 · Tahap 1 (Frontend)

```
Baca dulu sebelum mulai:
- docs/design.md (§3 Design System, §6 Popup UnlockSheet, §5 Aturan Bisnis & Batasan)
- docs/component-contract.md (§9 Unlock Sheet, §0 Model Data Global)
- docs/menu-tree.md (bagian Unlock Sheet)

Tugas: bangun Compose UI untuk Unlock Sheet, dan wiring tampilan badge tier di S1 header /
link "Upgrade" di S2 supaya benar-benar membuka sheet ini (sebelumnya masih placeholder).

Ini Sprint 4 - Tahap 1 (Frontend penuh). Artinya:
- Tabel perbandingan Gratis vs Unlimited, tombol "Unlock Unlimited" & "Restore Purchase",
  state sukses setelah pembelian — semua tampil dengan hasil pembelian DISIMULASIKAN
  (misal tombol langsung set isPremium=true di state lokal), belum panggil Billing asli.
- gamesUsed dari props (0-2), tampilkan progress/counter sesuai component-contract.md §9.
- Props/callback persis component-contract.md §9 (gamesUsed, isPremium, onPurchase,
  onRestore, onClose).

Acceptance criteria yang harus terpenuhi (tampilan saja — Billing asli di Tahap 2):

QL-13 (bagian tampilan)
- Tersedia tombol "Unlock Unlimited"
- Tombol "Restore Purchase" tersedia

Jangan menyentuh: BillingClient, app_config table — itu di prompt Tahap 2.
Output: file Composable baru di presentation/unlock/.
```

### Sprint 4 · Tahap 2 (Wiring + Hardening)

```
Baca dulu sebelum mulai:
- docs/database_schema.md (app_config)
- docs/domain_contract.md
- docs/tech-stack.md
- Kode Compose Unlock Sheet hasil Tahap 1 (JANGAN ubah UI-nya)
- Seluruh docs/backlog.md untuk regresi akhir

Tugas: implementasikan tabel app_config, integrasi Google Play Billing, selesaikan
enforcement limit tier gratis dengan status premium nyata, dan lakukan regresi penuh
terhadap seluruh 11 file desain + 17 item backlog.

Ini Sprint 4 - Tahap 2 (Wiring logika + Hardening). Artinya:
- Tabel app_config: key (TEXT PK), value (TEXT). Row is_premium, purchase_token,
  purchase_verified_at.
- BillingClient.queryPurchasesAsync() dipanggil setiap kali aplikasi dibuka; hasilnya
  meng-update is_premium di app_config.
- Setelah pembelian sukses, batasan free tier hilang TANPA restart app (observe is_premium
  sebagai Flow, bukan sekali baca).
- Enforcement QL-12 di S2 (dari Sprint 2) dibaca dari is_premium via UseCase layer, ganti
  dari flag dummy.
- Restore Purchase memicu queryPurchasesAsync() ulang dan menampilkan state sukses kalau
  ditemukan purchase valid.
- Setelah semua terpasang: jalankan regresi manual terhadap seluruh 11 file desain
  (ArticleList, AddArticle, Reader, dan 8 popup) mengikuti setiap panah di menu-tree.md,
  dan cocokkan tiap item QL-1 s.d. QL-17 di backlog.md terhadap acceptance criteria-nya.

Acceptance criteria yang harus terpenuhi (dari backlog.md):

QL-13 (lanjutan — logika)
- Tombol "Unlock Unlimited" memicu flow Google Play Billing (one-time product)
- Setelah pembelian sukses, batasan free tier (QL-12) langsung hilang tanpa restart app
- Status "sudah unlock" diverifikasi via BillingClient.queryPurchasesAsync() setiap app
  dibuka, hasilnya di-cache di tabel app_config lokal

QL-17 — Tabel app_config untuk status premium & purchase token
- Tabel app_config tersedia dengan kolom key (TEXT PK) dan value (TEXT)
- Row is_premium diupdate setiap kali queryPurchasesAsync() dipanggil saat app launch
- Row purchase_token menyimpan token dari Google Play
- Row purchase_verified_at menyimpan timestamp verifikasi terakhir
- Enforcement batas (QL-12) membaca is_premium dari app_config via UseCase layer

QL-12 (selesai)
- User free dibatasi maks. 2 game dan maks. 5 artikel per game (dari status premium nyata)
- Artikel/Game yang sudah tersimpan sebelum mencapai batas tetap dapat diakses penuh

Batasan arsitektur: domain layer pure Kotlin; BillingClient hanya di data layer, di-expose
ke domain lewat interface (mis. BillingRepository).
```

---

## Referensi

| Dokumen | Path |
|---|---|
| Peta Sprint & Tahap | `docs/backlog.md` §0 |
| Kontrak komponen (prop/callback) | `docs/component-contract.md` |
| Design system & rincian layar | `docs/design.md` |
| Peta navigasi | `docs/menu-tree.md` |
| Arsitektur navigasi & state popup | `docs/screen_flow.md` |
| Tech stack & arsitektur | `docs/tech-stack.md` |
| Skema database | `docs/database_schema.md` |
| Interface domain layer | `docs/domain_contract.md` |
| Model state error | `docs/error_state_model.md` |
| Konvensi kode | `docs/coding_convention.md` |
