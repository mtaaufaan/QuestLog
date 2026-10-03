# QuestLog - Backlog (MVP)

2026-09-19 · Disusun bersama @Taufan
2026-09-25 · Ditambahkan pemetaan Sprint & Tahap pengerjaan (lihat §0)
2026-09-26 · Ditambahkan Prasyarat Lingkungan dan Sprint 0 — Fondasi Proyek

Backlog ini menerjemahkan User Stories di PRD QuestLog MVP menjadi item kerja dengan acceptance criteria. Setiap item mereferensikan user story terkait di PRD.

## 0. Prasyarat Lingkungan (sebelum Sprint 0 dimulai)

Dilakukan sekali per komputer developer, bukan bagian dari sprint manapun — Gradle yang menangani semua dependency library di `tech-stack.md` §12 secara otomatis, jadi tidak perlu instalasi manual untuk Compose/Room/Hilt/OkHttp/Ksoup/Coil/WorkManager/DataStore.

**Wajib sebelum Sprint 0:**
- [ ] Android Studio terpasang (sudah termasuk JDK bawaan)
- [ ] Android SDK Platform API 26 & API 35 + build tools (via SDK Manager di Android Studio)
- [ ] Emulator atau device fisik Android 8.0+ untuk testing
- [ ] Git

**Ditunda tanpa batas waktu (keputusan @Taufan, 2026-09-26):**
- [ ] Akun Google Play Console — untuk setup produk in-app "Unlock Unlimited" (QL-13) dan distribusi resmi lewat Play Store
- [ ] Signing keystore khusus untuk distribusi `release` ke Play Store

Build variant `debug` (pakai `FakeBillingService`, signing pakai debug keystore bawaan Android Studio) dan `full` (pakai `FullVersionBillingService`, signing-nya di `tech-stack.md` §11 sengaja memakai debug keystore yang sama, bukan keystore terpisah) **tidak butuh keduanya sama sekali** — Sprint 0-4 bisa dikerjakan dan didemokan penuh tanpanya.

Yang tertunda kalau keduanya tidak dikerjakan: hanya jalur `release` ke Play Store yang sesungguhnya — yaitu verifikasi QL-13 dengan transaksi Billing asli (`GooglePlayBillingService`, hanya bisa diuji lewat instalasi via Play Store/internal testing track) dan publish resmi ke publik. Ini tidak menghambat pengerjaan kode di Sprint 4 (yang dites pakai `debug`/`full`), hanya menahan kapan aplikasi bisa benar-benar dirilis ke pengguna nyata via Play Store — keputusan kapan itu diproses ada di tangan @Taufan, tidak terikat sprint manapun.

## 1. Peta Sprint & Tahap

Tim: 2 Android dev + 1 QA · Sprint: 2 minggu, part-time · Urutan sprint mengikuti file desain (lihat `docs/menu-tree.md` & `docs/component-contract.md`), bukan urutan epic.

Setiap sprint dikerjakan dua tahap:
- **Tahap 1 — Frontend penuh**: UI layar/popup terkait dibangun menyeluruh sesuai `docs/design.md` + `docs/component-contract.md`, dijalankan dengan data dummy (boleh reuse data contoh di `QuestLogApp.dc.html`) supaya seluruh state visual langsung bisa didemokan.
- **Tahap 2 — Wiring logika**: data dummy diganti koneksi nyata (Room, scraper, WorkManager, Billing) tanpa mengubah UI dari Tahap 1.

| Sprint | Fokus | Item backlog |
|---|---|---|
| 0 | Fondasi proyek (scaffolding, design system, navigasi, skema DB, DI) | F1-F6 |
| 1 | S1 · Article List + Article Actions Sheet + Delete Article Dialog + Scrape Error Dialog + Game Filter Sheet | QL-1, QL-10, QL-11 |
| 2 | S2 · Add Article + Scrape Mode Sheet + Scrape Notification | QL-2, QL-3, QL-9, QL-12 (sebagian), QL-14, QL-15 (sebagian) |
| 3 | S3 · Reader + Display Settings Sheet | QL-4, QL-5, QL-6, QL-7, QL-8, QL-15 (sebagian), QL-16 |
| 4 | Unlock Sheet + hardening | QL-12 (selesai), QL-13, QL-17 |

Item yang muncul di lebih dari satu sprint (QL-12, QL-15) memang berlapis: bagian UI/pesan selesai lebih awal, bagian logika penuh menyusul di sprint yang mengerjakan layar terkait — detail per item ada di bawah.

---

## Epic: Fondasi Proyek (Sprint 0)

Bukan user story — ini prasyarat teknis yang dipakai bersama oleh semua sprint fitur (1-4). Tidak bernomor QL karena bukan acceptance criteria yang dilihat pengguna akhir, tapi tetap punya definisi "selesai" yang jelas per tahap.

### F1 — Scaffolding proyek
**Sprint 0 · Tahap 1**
- [ ] Struktur modul `presentation/domain/data` sesuai `tech-stack.md` §3
- [ ] `build.gradle.kts` dengan seluruh dependency & versi di `tech-stack.md` §12
- [ ] Build variant `debug`/`full`/`release` sesuai `tech-stack.md` §11 (memakai `FakeBillingService` di `debug`)
- [ ] Project berhasil di-build & di-run (boleh masih layar kosong)

### F2 — Design system dasar
**Sprint 0 · Tahap 1**
- [ ] Compose Theme: warna, tipografi (Cormorant Garamond/Lora/JetBrains Mono via Google Fonts), sesuai `design.md` §3
- [ ] Komponen dasar dapat dipakai ulang: tombol outline/ghost/bahaya, input teks, wrapper bottom sheet, wrapper dialog, snackbar, badge — sesuai `design.md` §3.4
- [ ] Mode gelap tersedia sebagai varian tema (dipakai nanti khusus di Reader & Display Settings)

### F3 — Skeleton navigasi
**Sprint 0 · Tahap 1**
- [ ] `NavHost` dengan 3 route (`ArticleList`, `AddArticle`, `Reader`) sesuai `screen_flow.md` §2 — boleh masih halaman placeholder kosong
- [ ] Pola state `activePopup` (sealed interface `Popup`) sesuai `screen_flow.md` §4, belum diisi popup nyata
- [ ] App bisa di-run dan berpindah antar 3 layar kosong bertema (hasil gabungan F1+F2+F3 sudah kelihatan bentuknya, bukan cuma kode)

### F4 — Skema database penuh
**Sprint 0 · Tahap 2**
- [ ] Seluruh tabel Room dibuat sekaligus sesuai `database_schema.md`/`tech-stack.md` §4: `games`, `articles`, `source_pages`, `content_nodes`, `checkpoints`, `images`, `app_config` — termasuk yang baru dipakai sprint belakangan (checkpoints, app_config), supaya tidak perlu migration Room bertahap per sprint
- [ ] Index sesuai schema (`idx_content_nodes_article_order`, `idx_content_nodes_source_page`)

### F5 — DI skeleton
**Sprint 0 · Tahap 2**
- [ ] Hilt module dasar (`AppModule`): `provideDatabase`, `provideOkHttpClient`, `provideBillingService` (default `FakeBillingService` di build `debug`) sesuai `tech-stack.md` §10

### F6 — Lint & data dummy bersama
**Sprint 0 · Tahap 2**
- [ ] Konfigurasi ktlint/detekt sesuai `coding_convention.md`
- [ ] Modul `PreviewData` berisi data dummy bersama (persis contoh di `QuestLogApp.dc.html` — 2 game, 5 artikel dengan variasi status) untuk dipakai ulang oleh semua prompt Tahap 1 di Sprint 1-4, supaya tidak ada versi dummy data yang berbeda-beda antar sprint

---

## Epic: Scraper & Content Import

### QL-1 — Input satu URL untuk disimpan offline

*Ref: User Story #1*
**Sprint 1 · Tahap 2** — bagian fondasi (schema + scraper minimal) yang menyediakan data nyata untuk S1; UI daftar artikelnya sendiri ada di Tahap 1 Sprint 1.

- [ ] User dapat memasukkan satu URL melalui field input
- [ ] Sistem mengekstrak teks, gambar, dan format penting (heading, list, tabel) dari halaman
- [ ] Elemen non-konten (iklan, navigasi, komentar, pop-up) dihilangkan dari hasil
- [ ] Konten tersimpan lokal dan dapat dibuka kembali tanpa koneksi internet
- [ ] Jika URL gagal diakses, sistem menampilkan pesan error yang jelas (bukan crash/hang)

### QL-2 — Input multi-URL saat menyimpan artikel

*Ref: User Story #2*
**Sprint 2 · Tahap 2** — UI daftar URL-nya sendiri (tambah/hapus/urutkan) ada di Tahap 1 Sprint 2 (bagian dari layar S2 Add Article).

- [ ] User dapat menambahkan lebih dari satu URL dalam satu proses penyimpanan
- [ ] Konten dari semua URL digabung menjadi satu file, sesuai urutan input
- [ ] Metadata batas halaman sumber disimpan untuk setiap segmen konten

### QL-3 — Tambah halaman ke file yang sudah ada

*Ref: User Story #3*
**Sprint 2 · Tahap 2** — tab "Lengkapi artikel" di S2 (Tahap 1) menyediakan UI-nya.

- [ ] Tersedia aksi "Tambah Halaman" pada file yang sudah tersimpan
- [ ] Halaman baru selalu ditambahkan di akhir urutan konten (append-only), tidak bisa disisipkan di tengah
- [ ] Checkpoint yang sudah ada pada file tersebut tetap valid setelah penambahan halaman

## Epic: Reading Experience

### QL-4 — Reading mode dasar

*Ref: User Story #4*
**Sprint 3 · Tahap 2** — UI Display Settings Sheet & Reader ada di Tahap 1 Sprint 3.

- [ ] User dapat mengatur ukuran font (minimal 3 tingkat: kecil/sedang/besar)
- [ ] User dapat mengaktifkan dark mode / light mode
- [ ] Gambar ditampilkan proporsional, tidak overflow dari lebar layar
- [ ] Pengaturan tampilan tersimpan sebagai preferensi (tidak reset tiap buka artikel)

### QL-5 — Mode baca seamless

*Ref: User Story #9*
**Sprint 3 · Tahap 1 & 2** — mode default, sudah harus tampil benar sejak Tahap 1 (dengan konten dummy).

- [ ] Seluruh konten gabungan ditampilkan sebagai satu scroll berkelanjutan
- [ ] Ini adalah mode default saat artikel pertama kali dibuka

### QL-6 — Mode baca per halaman

*Ref: User Story #9*
**Sprint 3 · Tahap 2** — bergantung pada metadata page_order dari QL-2 (Sprint 2).

- [ ] User dapat toggle ke mode per halaman dari mode seamless (dan sebaliknya)
- [ ] Konten dipecah sesuai metadata batas halaman sumber (QL-2)
- [ ] Tersedia navigasi next/prev antar halaman
- [ ] Indikator posisi ditampilkan (mis. "Halaman 2 dari 5")
- [ ] Preferensi mode tersimpan per artikel (tidak perlu toggle ulang tiap buka)

## Epic: Checkpoint

### QL-7 — Buat/perbarui checkpoint manual

*Ref: User Story #5*
**Sprint 3 · Tahap 2**

- [ ] User dapat menandai posisi baca saat ini sebagai checkpoint (1 tombol/aksi)
- [ ] Checkpoint disimpan sebagai anchor ke elemen konten (bukan posisi pixel/scroll)
- [ ] Hanya ada 1 checkpoint aktif per artikel — menandai checkpoint baru menimpa yang lama
- [ ] Checkpoint tetap akurat meski ukuran font atau orientasi layar berubah

### QL-8 — Lanjutkan dari checkpoint

*Ref: User Story #5*
**Sprint 3 · Tahap 2** — tombol "Lanjutkan Baca" di S1 sudah ada sejak Tahap 1 Sprint 1 (memanggil `onResume`), logikanya menyusul di sini.

- [ ] Artikel dengan checkpoint tersimpan menampilkan indikator/tombol "Lanjutkan Baca" di daftar artikel
- [ ] Menekan "Lanjutkan Baca" membuka artikel dan scroll otomatis ke posisi checkpoint
- [ ] Jika elemen anchor checkpoint sudah tidak ada (mis. akibat perubahan konten), sistem fallback ke elemen terdekat yang masih ada, bukan error

## Epic: Game Entity & Organisasi

### QL-9 — Pilih/buat entitas Game saat simpan artikel

*Ref: User Story #6*
**Sprint 2 · Tahap 2** — input autocomplete Game ada di UI S2 sejak Tahap 1 Sprint 2.

- [ ] Saat menyimpan artikel, user diminta memilih Game terkait
- [ ] Input menampilkan autocomplete dari daftar Game yang sudah ada
- [ ] Jika Game belum ada di daftar, tersedia aksi eksplisit "Buat Game baru: \[nama\]"
- [ ] Nama Game dinormalisasi ringan (trim spasi, title-case) sebelum dicocokkan/disimpan
- [ ] Setiap artikel menyimpan referensi ke `game_id`, bukan teks bebas

### QL-10 — Cari artikel berdasarkan judul

*Ref: User Story #8*
**Sprint 1 · Tahap 1 & 2** — search bar built-in di layar S1; hasil pencarian terhadap data nyata baru berlaku setelah Tahap 2 (skema/DB) Sprint 1 selesai.

- [ ] Tersedia search bar di daftar artikel
- [ ] Pencarian mencocokkan judul artikel (case-insensitive, partial match)
- [ ] Hasil pencarian update secara real-time saat mengetik

### QL-11 — Filter artikel berdasarkan Game

*Ref: User Story #7*
**Sprint 1 · Tahap 1 & 2** — Game Filter Sheet (bottom sheet radio, bukan dropdown — lihat `component-contract.md` §10) dibangun bareng S1.

- [ ] Tersedia filter/dropdown berisi daftar Game yang sudah dipakai
- [ ] Memilih satu Game menampilkan hanya artikel dengan `game_id` tersebut
- [ ] Filter dapat dikombinasikan dengan search (QL-10)
- [ ] Ada opsi "Tampilkan semua" untuk reset filter

## Epic: Monetisasi

### QL-12 — Batasan pemakaian gratis

*Ref: Model Bisnis, PRD*
**Sprint 2 · Tahap 2 (enforcement) → Sprint 4 · Tahap 2 (selesai)** — pesan batas & blocking submit selesai di Sprint 2 begitu S2 dikerjakan; tombol "Upgrade" baru benar-benar membuka Unlock Sheet yang berfungsi penuh setelah Sprint 4 (butuh QL-17 & QL-13).

- [ ] User free dibatasi maks. 2 game dan maks. 5 artikel per game
- [ ] Saat batas tercapai, user melihat pesan jelas + ajakan upgrade (bukan silent fail)
- [ ] Artikel/Game yang sudah tersimpan sebelum mencapai batas tetap dapat diakses penuh

### QL-13 — Unlock unlimited via Google Play Billing

*Ref: Model Bisnis, PRD*
**Sprint 4 · Tahap 2** — UI Unlock Sheet (tabel banding, tombol beli/restore) sendiri ada di Tahap 1 Sprint 4.

- [ ] Tersedia tombol "Unlock Unlimited" yang memicu flow Google Play Billing (one-time product)
- [ ] Setelah pembelian sukses, batasan free tier (QL-12) langsung hilang tanpa restart app
- [ ] Status "sudah unlock" diverifikasi via `BillingClient.queryPurchasesAsync()` setiap kali aplikasi dibuka, hasilnya di-cache di tabel `app_config` lokal
- [ ] Tombol "Restore Purchase" tersedia untuk kasus reinstall app di device yang sama

### QL-17 — Tabel app_config untuk status premium & purchase token

*Ref: QL-12, QL-13 — implementasi storage*
**Sprint 4 · Tahap 2** — prasyarat teknis untuk QL-13, dikerjakan di awal sprint yang sama sebelum wiring billing.

- [ ] Tabel `app_config` tersedia di database dengan kolom `key` (TEXT PK) dan `value` (TEXT)
- [ ] Row `is_premium` diupdate setiap kali `queryPurchasesAsync()` dipanggil saat app launch
- [ ] Row `purchase_token` menyimpan token dari Google Play; digunakan saat query ke Play Billing
- [ ] Row `purchase_verified_at` menyimpan timestamp verifikasi terakhir
- [ ] Enforcement batas (QL-12) membaca `is_premium` dari `app_config` via UseCase layer, bukan langsung dari DB

## Epic: Scraping Resilience

### QL-14 — Dua Mode Scraping: buat baru vs lengkapi artikel

*Ref: User Story #1, #2, #3 — UX Flow baru*
**Sprint 2 · Tahap 1 & 2** — tab switcher & Scrape Mode Sheet adalah bagian utama UI S2.

- [ ] Saat memulai scraping, user ditampilkan pilihan eksplisit: "Buat artikel baru" atau "Lengkapi artikel yang ada"
- [ ] Mode "Buat artikel baru": user mengisi judul artikel, memilih game, dan memasukkan daftar URL; urutan URL dapat diatur sebelum scraping dimulai
- [ ] Mode "Lengkapi artikel yang ada": user memilih artikel dari daftar, lalu memasukkan URL tambahan beserta posisinya dalam urutan halaman
- [ ] Validasi duplikasi URL dijalankan sebelum scraping dimulai di kedua mode

### QL-15 — Tampilan artikel parsial dan retry halaman gagal

*Ref: Functional Requirement baru — Scraping Resilience*
**Berlapis 3 sprint** — Scrape Error Dialog (artikel gagal total) di **Sprint 1**; Scrape Notification banner + logika retry background di **Sprint 2**; banner partial + tombol "Coba Lagi" di dalam Reader di **Sprint 3**.

- [ ] Jika sebagian URL berhasil dan sebagian gagal, artikel tetap dapat dibuka dengan konten yang tersedia
- [ ] Artikel parsial menampilkan indikator jelas bahwa konten belum lengkap (mis. banner/chip "X halaman gagal dimuat")
- [ ] Tersedia tombol "Coba Lagi" yang hanya mengulang scraping untuk halaman yang gagal, bukan seluruh artikel
- [ ] Halaman yang sudah berhasil tidak di-scrape ulang saat retry

## Epic: Reading Experience

### QL-16 — Auto-resume posisi baca saat kembali ke artikel

*Ref: User Story #10*
**Sprint 3 · Tahap 2** — dikerjakan bersamaan dengan checkpoint (QL-7/QL-8) karena berbagi mekanisme anchor yang sama.

- [ ] Sistem menyimpan posisi baca terakhir secara otomatis saat pengguna meninggalkan layar baca (tanpa aksi manual)
- [ ] Saat artikel dibuka kembali, scroll otomatis ke posisi terakhir dibaca
- [ ] Fitur ini bekerja terpisah dari checkpoint manual — checkpoint adalah penanda yang disengaja, auto-resume adalah posisi terakhir
- [ ] Auto-resume bekerja untuk perpindahan antar artikel dalam game yang sama maupun antar game
