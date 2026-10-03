# QuestLog — Design Documentation

**Sumber:** Prototype Claude Design — `QuestLogApp.dc.html` (dan 10 file layar/popup turunannya)
**Platform:** Mobile app (frame 390×844 — setara iPhone standar), gaya kartu/dokumen (serif, warna parchment)
**Disusun oleh:** Dianalisis dari kode desain interaktif (Design Compiler / `.dc.html`) untuk QuestLog — aplikasi penyimpan & pembaca walkthrough game secara offline.

---

## 1. Ringkasan Produk

QuestLog adalah aplikasi mobile yang memungkinkan pengguna **menyimpan walkthrough/FAQ game dari web (scraping URL) agar bisa dibaca offline**, lengkap dengan checkpoint bacaan, pengaturan tampilan baca, dan model bisnis freemium (one-time purchase, bukan langganan).

Alur inti: pengguna menambahkan satu atau beberapa URL sumber → sistem melakukan scraping di latar belakang → hasil disimpan sebagai "artikel" per game → pengguna membaca artikel secara offline dengan progress & checkpoint tersimpan.

### Fitur utama
- Menyimpan walkthrough baru (multi-URL, urutan halaman bisa diatur) atau melengkapi artikel yang sudah ada (append-only)
- Pemrosesan scraping berjalan di latar belakang dengan notifikasi progres/selesai/gagal
- Pembaca (Reader) dengan mode Seamless (scroll menerus) dan Per Halaman, checkpoint, pengaturan ukuran teks & mode gelap
- Pencarian & filter artikel per game
- Penanganan halaman gagal di-scrape (retry per artikel)
- Monetisasi: batas gratis (2 game, 5 artikel/game) → "QuestLog Unlimited" (pembelian sekali bayar via Google Play)

---

## 2. Prinsip & Mood Desain

- **Nuansa "buku/jurnal digital"** — bukan gaya UI aplikasi teknis pada umumnya. Tipografi serif, palet warna kertas hangat (parchment/cream), aksen emas tua (antique gold), menekankan kenyamanan membaca dalam waktu lama.
- **Minim-chrome, konten dulu** — header tipis, banyak whitespace, garis pembatas tipis alih-alih kartu/shadow berat.
- **Feedback halus, bukan mengganggu** — status proses (scraping, error) disampaikan lewat notifikasi non-modal (banner/snackbar) sebisa mungkin, popup modal hanya untuk aksi yang butuh konfirmasi/keputusan.
- **Offline-first messaging** — copy secara konsisten menegaskan "tersimpan offline", "diunduh di latar belakang", untuk membangun kepercayaan bahwa konten aman dibaca tanpa koneksi.

---

## 3. Design System

### 3.1 Tipografi

| Peran | Font | Contoh pemakaian |
|---|---|---|
| Judul besar / display | **Cormorant Garamond** (500, 600) | Nama app "QuestLog", judul artikel (H1 Reader), judul dialog/sheet |
| Heading di konten artikel | Cormorant Garamond 600 | H2/H3 pada isi walkthrough |
| Body & UI umum | **Lora** (400, 600, italic) | Paragraf, label, tombol, deskripsi |
| Data teknis / URL / halaman | **JetBrains Mono** | URL sumber, label halaman, ukuran font |
| Status bar / notifikasi sistem | System UI (-apple-system dst.) | Jam status bar, notifikasi push |

Skala ukuran teks baca (Reader) dapat diubah pengguna: **12–24sp**, step 2, default 16sp — memengaruhi H1/H2/H3/paragraf/preformatted/tabel secara proporsional.

### 3.2 Palet Warna

**Mode Terang (default, dipakai di semua layar)**

| Token | Hex | Kegunaan |
|---|---|---|
| Background utama | `#f3f2f2` | Latar layar & sheet |
| Background shell/app | `#e6e3de` | Latar canvas prototipe |
| Teks primer | `#201f1d` | Judul, teks utama |
| Teks sekunder | `#605d5d` | Subjudul, meta info |
| Teks tersier | `#7d7979` | Placeholder, caption |
| Aksen (teks) | `#7d5411` | Link, teks tombol utama, label kicker |
| Aksen (garis/ikon) | `#b68235` / `#a06f24` | Border tombol outline, ikon aktif |
| Aksen muda | `#e1ad66` | Ikon di notifikasi gelap |
| Aksen tint (bg) | `#fff3e4` | Hover/active state elemen beraksen |
| Aksen tint active | `#ffe3bf` | Pressed state |
| Bahaya/error | `#9e3b2c` | Teks & border error, tombol hapus |
| Bahaya tint (bg) | `#f6e6e2` / `#f8ecea` | Background peringatan/error |
| Garis pembatas | `rgba(32,31,29,.12–.22)` | Divider, border input |
| Scrim overlay | `rgba(20,19,18,.45)` | Latar belakang modal/sheet |
| Snackbar | bg `#2d2b2b`, teks `#f3f2f2` | Toast konfirmasi |

**Mode Gelap** (khusus Reader & Display Settings — satu-satunya bagian yang mendukung dark mode)

| Token | Hex |
|---|---|
| Background | `#1d1c1a` (sheet: `#22211f`) |
| Teks primer | `#e8e3dc` |
| Teks sekunder | `#a8a29a` |
| Aksen | `#e1ad66` |
| Bahaya | `#e38a78` |
| Surface (blok kode/gambar) | `#282624` |

### 3.3 Ikonografi
Ikon garis (line icon) custom inline SVG, stroke 1.6–1.8px, `stroke-linecap: round`, tanpa fill (kecuali beberapa aksen kecil) — gaya mirip Lucide/Feather icons. Digunakan konsisten untuk: kembali, cari, filter, tambah (+), retry, hapus, checkpoint/bookmark, pengaturan tampilan, sukses (centang), lock/unlock premium.

### 3.4 Komponen UI

| Komponen | Karakteristik |
|---|---|
| **Tombol outline (primer)** | Border 1px `#b68235`, teks `#7d5411`, background transparan → hover `#fff3e4`, active `#ffe3bf`, disabled opacity .4–.45 |
| **Tombol teks/ghost** | Tanpa border, teks abu (`#605d5d`), hover background abu muda |
| **Tombol bahaya** | Border/teks `#9e3b2c`, hover `#f6e6e2` |
| **Input teks** | Tinggi 44–48px, border 1px abu, focus → border aksen; error → border merah + pesan di bawah |
| **Bottom sheet** | Radius atas 12px, drag handle 36×4px, animasi slide-up 0.28s, scrim fade |
| **Dialog/modal** | Radius 8px, max-width 320–340px, animasi fade+scale-pop 0.22s, `role="alertdialog"` |
| **Snackbar/toast** | Pil gelap melayang di bawah layar, auto-hide ~2.4–2.6 detik |
| **Notifikasi in-app** | Banner gelap melayang di atas layar (mirip push notification), progress bar tipis untuk status "sedang mengunduh" |
| **FAB** | Lingkaran 60px, kanan-bawah, outline aksen, ikon "+" |
| **List row (artikel)** | Tap = buka; tekan-tahan (long-press ~520ms) atau tombol titik tiga = buka Article Actions Sheet |
| **Radio select row** | Untuk pilih game (filter) / pilih artikel target (append) |
| **Combobox/autocomplete** | Input game dengan dropdown pencarian + opsi "buat game baru" |
| **Progress bar tipis** | 2–3px, dipakai di header Reader dan baris artikel yang sedang di-scrape |
| **Badge status** | Pil kecil bertepi (mis. "1 halaman gagal") |

### 3.5 Motion
- `qlFade` 0.2s — overlay scrim muncul
- `qlPop` 0.22s — dialog modal muncul (scale + fade)
- `qlSheet` 0.28s cubic-bezier — bottom sheet slide-up
- `qlDrop` 0.35s — notifikasi banner turun dari atas
- `qlUp` 0.22s — snackbar naik dari bawah
- `qlSpin` 0.8s linear infinite — loading spinner
- `qlMark` 0.3s — penanda checkpoint muncul di Reader

### 3.6 Layout & Platform
- Dioptimalkan untuk satu ukuran layar mobile (390×844), single column, status bar palsu (jam + ikon baterai/sinyal) di setiap layar utama.
- Scrollbar disembunyikan (`scrollbar-width:none`) untuk kesan native app.

---

## 4. Model Data (disimpulkan dari kode)

```
Game
├─ id: number
└─ name: string

Article
├─ id: number
├─ gameId: number
├─ title: string
├─ status: 'SCRAPING' | 'READY' | 'ERROR'
├─ pages: Page[]
├─ checkpointNodeId?: number   // posisi bookmark manual
├─ lastNodeId?: number         // posisi baca terakhir (auto)
├─ lastRead?: string           // label relatif, mis. "dibaca kemarin"
└─ readMode?: 'seamless' | 'paged'

Page
├─ url: string
├─ status: 'PENDING' | 'DONE' | 'FAILED'
└─ reason?: string             // alasan gagal, mis. "HTTP 404"
```

Konten artikel yang sudah di-scrape direpresentasikan sebagai node per halaman: `h1 | h2 | h3 | p | li | pre | img | table | break | missing`.

---

## 5. Aturan Bisnis & Batasan

- **Tier Gratis:** maksimal **2 game**, maksimal **5 artikel per game**.
- **QuestLog Unlimited:** pembelian **sekali bayar** (bukan langganan) via Google Play; status diverifikasi ulang di perangkat setiap aplikasi dibuka; tersedia tombol "Restore Purchase".
- **Validasi URL:** wajib diawali `https://`, tidak boleh duplikat (dalam artikel yang sama maupun dengan URL yang sudah tersimpan), maksimal **10 URL per artikel**. Tempel banyak URL sekaligus (dipisah baris/koma) otomatis terpecah jadi beberapa baris input.
- **Mode "Lengkapi artikel yang ada" (append):** halaman baru **selalu ditambahkan di akhir** (append-only); checkpoint yang sudah ada tetap valid; hanya tersedia jika sudah ada minimal satu artikel berstatus READY.
- **Scraping gagal sebagian:** artikel tetap bisa dibuka (partial), dengan badge "X halaman gagal" dan opsi "Coba Lagi" — hanya halaman yang gagal yang diunduh ulang.
- **Scraping gagal total:** artikel berstatus ERROR, tidak bisa dibuka untuk dibaca, hanya opsi retry atau hapus.
- **Pembatasan konten:** hanya field yang dinyatakan pengguna yang diproses; tidak ada indikasi penyimpanan kredensial/login untuk scraping.

*(Catatan tambahan dari backlog UX yang sudah tercatat sebelumnya: perlu dipastikan validasi duplikat URL dilakukan sebelum scraping dimulai, dan page_order pada mode "Lengkapi" dilanjutkan dari nomor halaman maksimum yang sudah ada — kedua hal ini sudah konsisten dengan perilaku pada purwarupa ini.)*

---

## 6. Rincian per Layar

### S1 — Article List *(Home)*
**File:** `ArticleList.dc.html`
**Tujuan:** Titik masuk utama; menampilkan seluruh walkthrough tersimpan, memungkinkan pencarian, filter per game, dan aksi cepat.

**Struktur:**
- Header: judul "QuestLog" + subtitle jumlah walkthrough, tombol status tier (badge "X/2 game" atau "Unlimited")
- Search bar (cari judul artikel)
- Baris filter: chip "Semua Game"/nama game terpilih + counter jumlah artikel
- Daftar artikel (card list), tiap baris menampilkan: nama game (kicker), judul artikel, meta (jumlah halaman · waktu baca terakhir), status:
  - **READY** → info halaman + badge "partial" bila ada halaman gagal + tombol "Lanjutkan Baca" bila ada checkpoint
  - **SCRAPING** → indikator pulsa + progress bar + label "Memproses… n/total"
  - **ERROR** → indikator merah + teks "Gagal — tap untuk retry"
- Tombol titik-tiga per baris / tekan-tahan baris → buka **Article Actions Sheet**
- Empty state (belum ada artikel / hasil pencarian kosong)
- FAB "+" di kanan bawah → buka **Scrape Mode Sheet**
- Snackbar untuk feedback aksi

**Interaksi tap pada baris:** READY → buka Reader; SCRAPING → snackbar "masih diproses"; ERROR → buka Scrape Error Dialog.

---

### S2 — Add Article *(Simpan Walkthrough)*
**File:** `AddArticle.dc.html`
**Tujuan:** Form untuk menyimpan artikel baru atau menambah halaman ke artikel lama.
**Entry point:** dari Scrape Mode Sheet (pilih mode), atau langsung dari Article Actions Sheet (mode "append" dengan target sudah terisi).

**Struktur:**
- Header dengan tombol kembali + judul "Simpan Walkthrough"
- Tab switcher: **Buat artikel baru** vs **Lengkapi artikel**
- **Mode Baru:**
  - Input Game dengan autocomplete (cari/pilih game ada, atau buat game baru — dibatasi limit tier gratis)
  - Input Judul artikel
- **Mode Lengkapi:**
  - Daftar radio pilihan artikel (READY saja) yang bisa dilengkapi, menampilkan game, judul, jumlah halaman
- Bagian URL halaman (sama untuk kedua mode):
  - Daftar input URL, tiap baris bisa dipindah urutan (naik/turun), dihapus; validasi realtime per baris
  - Tombol "Tambah URL" (maks. 10)
  - Catatan: tempel banyak URL sekaligus otomatis terpecah per baris
- Banner peringatan bila limit tier gratis tercapai + link Upgrade
- Tombol submit ("Simpan & Mulai Unduh" / "Tambahkan Halaman"), status loading saat submit
- Footer info: "Diunduh di latar belakang · notifikasi saat siap dibaca"

---

### S3 — Reader *(Pembaca Artikel)*
**File:** `Reader.dc.html`
**Tujuan:** Membaca konten walkthrough yang sudah tersimpan offline.
**State layar:** `success` (default) · `loading` · `not_found` · `empty` · `db_error`

**Struktur (state success):**
- Header: kembali, nama game (kicker) + judul artikel, tombol checkpoint (bookmark), tombol pengaturan tampilan → **Display Settings Sheet**
- Progress bar tipis (persentase posisi baca)
- Toggle mode baca: **Seamless** (scroll menerus, halaman disambung dengan pemisah) vs **Per Halaman** (satu halaman per layar, navigasi Sebelumnya/Berikutnya)
- Banner peringatan bila artikel partial (ada halaman gagal) + tombol "Coba Lagi" (retry hanya halaman gagal)
- Konten artikel: mendukung heading (H1/H2/H3), paragraf, list, blok kode (pre), gambar (placeholder dengan caption), tabel, pemisah antar-halaman, dan penanda checkpoint inline
- Placeholder "Halaman gagal dimuat" untuk halaman yang belum berhasil disimpan
- Footer navigasi (khusus mode Per Halaman)
- Snackbar (mis. "Checkpoint disimpan", "Melanjutkan dari posisi terakhir")

**State lain:** loading (spinner tengah), not_found / empty / db_error (ikon + judul + deskripsi + tombol aksi "Kembali"/"Coba Lagi").

**Catatan UX:** posisi baca terakhir (`lastNodeId`) otomatis tersimpan saat pengguna keluar dari Reader; checkpoint (`checkpointNodeId`) adalah penanda manual terpisah yang bisa diset lewat tombol bookmark.

---

### Popup / Sheet / Dialog

| Nama | File | Trigger | Fungsi |
|---|---|---|---|
| **Scrape Mode Sheet** | `ScrapeModeSheet.dc.html` | Tap FAB "+" di S1 | Pilih "Buat artikel baru" atau "Lengkapi artikel yang ada" (opsi kedua disabled jika belum ada artikel) → lanjut ke S2 |
| **Article Actions Sheet** | `ArticleActionsSheet.dc.html` | Tekan-tahan / titik-tiga baris artikel di S1 | Aksi kontekstual: Lanjutkan dari checkpoint, Buka artikel, Tambah halaman (→S2 mode append), Hapus artikel (→Delete Dialog) |
| **Delete Article Dialog** | `DeleteArticleDialog.dc.html` | Pilih "Hapus artikel" | Konfirmasi hapus artikel beserta seluruh konten & checkpoint-nya |
| **Scrape Error Dialog** | `ScrapeErrorDialog.dc.html` | Tap artikel berstatus ERROR | Menampilkan daftar halaman yang gagal + alasan; aksi: Coba Lagi (retry semua halaman gagal), Hapus, Tutup |
| **Scrape Notification** | `ScrapeNotification.dc.html` | Otomatis muncul saat proses scraping berjalan/selesai | Banner non-modal di atas layar: progress (dengan bar), selesai, partial, atau error; tap → buka artikel terkait / dialog error |
| **Unlock Sheet** | `UnlockSheet.dc.html` | Tap badge tier di header S1, atau limit tercapai (link "Upgrade") | Tabel perbandingan Gratis vs Unlimited, tombol Beli (sekali bayar) & Restore Purchase; state sukses setelah pembelian |
| **Game Filter Sheet** | `GameFilterSheet.dc.html` | Tap chip filter di S1 | Daftar radio: "Semua Game" + tiap game (dengan jumlah artikel) |
| **Display Settings Sheet** | `DisplaySettingsSheet.dc.html` | Tap ikon pengaturan di Reader | Atur ukuran teks (12–24sp, step 2) dan Mode Gelap; berlaku untuk semua artikel |

---

## 7. Aksesibilitas & Detail Teknis Tambahan
- Penggunaan atribut ARIA: `role="alertdialog"` pada dialog konfirmasi/error, `role="tablist"`/`role="tab"` pada toggle mode, `role="radiogroup"`/`role="radio"` pada seleksi game/artikel, `role="switch"` pada toggle mode gelap, `aria-label` pada tombol ikon.
- Semua tombol interaktif memiliki target sentuh minimal ±44px tinggi, sesuai pedoman mobile touch target.
- Kontras teks pada mode terang & gelap dijaga cukup tinggi (abu tua di atas latar terang; krem terang di atas latar gelap).
- Bahasa antarmuka: **Bahasa Indonesia** di seluruh microcopy.

---

## 8. Berkas Sumber

| Berkas | Peran |
|---|---|
| `QuestLogApp.dc.html` | Shell/orchestrator — routing antar layar & popup, state global (games, articles, tier, dsb.) |
| `ArticleList.dc.html` | Layar S1 |
| `AddArticle.dc.html` | Layar S2 |
| `Reader.dc.html` | Layar S3 |
| `ScrapeModeSheet.dc.html` | Popup — pilih mode simpan |
| `ArticleActionsSheet.dc.html` | Popup — aksi artikel |
| `DeleteArticleDialog.dc.html` | Popup — konfirmasi hapus |
| `ScrapeErrorDialog.dc.html` | Popup — error scraping |
| `ScrapeNotification.dc.html` | Overlay — notifikasi in-app |
| `UnlockSheet.dc.html` | Popup — upsell premium |
| `GameFilterSheet.dc.html` | Popup — filter game |
| `DisplaySettingsSheet.dc.html` | Popup — pengaturan tampilan baca |
| `support.js` | Runtime pendukung Design Compiler (tidak berisi logika produk) |

Lihat juga **`menu-tree.md`** untuk peta navigasi lengkap antar layar dan popup.
