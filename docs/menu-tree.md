# QuestLog — Menu Tree / Peta Navigasi

Pendamping dari `design.md`. Dokumen ini memetakan struktur navigasi aplikasi: layar utama, seluruh popup/sheet/dialog, serta titik masuk (entry point) dan aksi yang menghubungkannya.

---

## 1. Struktur Pohon Navigasi

```
QuestLog (App Shell)
│
├─ S1 · Article List  [HOME / default route]
│   ├─ [tap badge tier]                    → Unlock Sheet
│   ├─ [input pencarian]                    (in-place, tanpa navigasi)
│   ├─ [tap chip filter]                    → Game Filter Sheet
│   ├─ [tap baris artikel — status READY]   → S3 · Reader (resume: posisi terakhir)
│   ├─ [tap baris artikel — status SCRAPING]→ snackbar "masih diproses" (tetap di S1)
│   ├─ [tap baris artikel — status ERROR]   → Scrape Error Dialog
│   ├─ [tombol "Lanjutkan Baca" di baris]   → S3 · Reader (resume: checkpoint)
│   ├─ [titik-tiga / tekan-tahan baris]     → Article Actions Sheet
│   ├─ [tap FAB "+"]                        → Scrape Mode Sheet
│   └─ [notifikasi scraping muncul otomatis]→ Scrape Notification (overlay, non-blocking)
│
├─ S2 · Add Article  [Simpan Walkthrough]
│   │  entry: dari Scrape Mode Sheet (mode=new/append) ATAU dari Article Actions Sheet (mode=append, target terisi)
│   ├─ Tab: Buat artikel baru
│   │   ├─ Input Game (autocomplete)
│   │   │   └─ [ketik nama baru]            → opsi "Buat Game baru" (dibatasi limit tier gratis → link Upgrade → Unlock Sheet)
│   │   ├─ Input Judul artikel
│   │   └─ Daftar URL halaman (tambah/hapus/urutkan)
│   ├─ Tab: Lengkapi artikel
│   │   ├─ Pilih artikel target (radio list, hanya status READY)
│   │   └─ Daftar URL halaman tambahan (append di akhir)
│   ├─ [tombol kembali]                     → S1 · Article List
│   ├─ [link "Upgrade" saat limit tercapai] → Unlock Sheet
│   └─ [submit "Simpan & Mulai Unduh" / "Tambahkan Halaman"]
│                                            → S1 · Article List (artikel baru berstatus SCRAPING,
│                                               proses berjalan di background + Scrape Notification)
│
├─ S3 · Reader  [Baca Artikel]
│   │  entry: dari S1 (tap artikel READY / tombol Lanjutkan Baca / tap notifikasi selesai)
│   │  state: success · loading · not_found · empty · db_error
│   ├─ [tombol kembali]                     → S1 · Article List (posisi baca tersimpan otomatis)
│   ├─ [tombol checkpoint/bookmark]          set checkpoint di posisi baca saat ini (in-place)
│   ├─ [tombol pengaturan tampilan]          → Display Settings Sheet
│   ├─ [toggle Seamless / Per Halaman]       (in-place, ganti mode tampil)
│   ├─ [banner partial → "Coba Lagi"]        retry halaman gagal (in-place, loading singkat)
│   ├─ [state error → tombol aksi]           → S1 · Article List (not_found/empty) atau retry-load (db_error)
│   └─ [navigasi Sebelumnya/Berikutnya]      (khusus mode Per Halaman, in-place)
│
├─ Scrape Mode Sheet  [Popup — dari FAB S1]
│   ├─ [pilih "Buat artikel baru"]          → S2 · Add Article (mode=new)
│   ├─ [pilih "Lengkapi artikel yang ada"]  → S2 · Add Article (mode=append) — nonaktif jika belum ada artikel READY
│   └─ [Batal / tap area luar]              tutup popup, kembali ke S1
│
├─ Article Actions Sheet  [Popup — dari baris artikel S1]
│   ├─ [Lanjutkan dari checkpoint]          → S3 · Reader (resume: checkpoint) — tampil jika ada checkpoint
│   ├─ [Buka artikel]                       → S3 · Reader (resume: terakhir) — tampil jika status READY
│   ├─ [Tambah halaman]                     → S2 · Add Article (mode=append, target=artikel ini)
│   ├─ [Hapus artikel]                      → Delete Article Dialog
│   └─ [tutup / tap area luar]              tutup popup, kembali ke S1
│
├─ Delete Article Dialog  [Popup — dari Article Actions Sheet]
│   ├─ [Hapus]                              hapus artikel → kembali ke S1 (snackbar "Artikel dihapus")
│   └─ [Batal]                              tutup popup, kembali ke S1
│
├─ Scrape Error Dialog  [Popup — dari artikel status ERROR di S1]
│   ├─ [Coba Lagi]                          retry semua halaman gagal → status kembali SCRAPING, tutup popup
│   ├─ [Hapus]                              hapus artikel
│   └─ [Tutup]                              tutup popup, kembali ke S1
│
├─ Scrape Notification  [Overlay — muncul otomatis, tidak menutupi navigasi]
│   │  kind: progress · done · partial · error
│   └─ [tap notifikasi]
│       ├─ jika artikel READY                → S3 · Reader
│       └─ jika artikel ERROR                → S1 · Article List + Scrape Error Dialog terbuka
│
├─ Unlock Sheet  [Popup — QuestLog Unlimited]
│   │  entry: badge tier di S1, atau link "Upgrade" di S2 saat limit tercapai
│   ├─ [Unlock Unlimited]                   proses pembelian (Google Play) → state sukses in-place
│   ├─ [Restore Purchase]                   proses pemulihan pembelian → state sukses in-place (jika ada)
│   └─ [Lanjutkan (setelah sukses) / tutup] tutup popup, kembali ke layar asal
│
├─ Game Filter Sheet  [Popup — dari chip filter S1]
│   ├─ [pilih "Semua Game" / nama game]     terapkan filter → tutup popup, kembali ke S1 (daftar terfilter)
│   └─ [tap area luar]                      tutup popup tanpa ubah filter
│
└─ Display Settings Sheet  [Popup — dari S3 · Reader]
    ├─ [+ / − ukuran teks]                  ubah ukuran teks in-place (12–24sp), berlaku ke semua artikel
    ├─ [toggle Mode Gelap]                  ubah tema Reader in-place
    └─ [tap area luar]                      tutup popup, kembali ke S3
```

---

## 2. Tabel Ringkas — Tipe & Level Navigasi

| # | Nama | Tipe | Level | Dipicu dari |
|---|---|---|---|---|
| 1 | Article List (S1) | Layar penuh | Root / Home | — (default route) |
| 2 | Add Article (S2) | Layar penuh | Level-2 | Scrape Mode Sheet, Article Actions Sheet |
| 3 | Reader (S3) | Layar penuh | Level-2 | S1 (tap artikel / lanjutkan baca), Scrape Notification |
| 4 | Scrape Mode Sheet | Bottom sheet | Overlay atas S1 | FAB "+" di S1 |
| 5 | Article Actions Sheet | Bottom sheet | Overlay atas S1 | Titik-tiga / tekan-tahan baris artikel |
| 6 | Delete Article Dialog | Modal dialog | Overlay atas S1 (dari Actions Sheet) | "Hapus artikel" |
| 7 | Scrape Error Dialog | Modal dialog | Overlay atas S1 | Tap artikel status ERROR, tap notifikasi error |
| 8 | Scrape Notification | Banner overlay | Overlay atas semua layar (top) | Otomatis saat status scraping berubah |
| 9 | Unlock Sheet | Bottom sheet | Overlay atas S1/S2 | Badge tier, link Upgrade |
| 10 | Game Filter Sheet | Bottom sheet | Overlay atas S1 | Chip filter di S1 |
| 11 | Display Settings Sheet | Bottom sheet | Overlay atas S3 | Ikon pengaturan di Reader |

---

## 3. Catatan Alur Penting

- **Tidak ada bottom navigation bar / tab utama** — aplikasi berbasis single-stack navigation (Article List sebagai home, Add Article & Reader sebagai layar dorong/push, seluruh popup sebagai overlay non-route).
- **Notifikasi scraping** adalah satu-satunya elemen yang bisa muncul di atas layar mana pun tanpa mengganggu alur — dirancang agar pengguna tetap bisa menavigasi aplikasi selagi proses unduh berjalan di latar belakang.
- **Unlock Sheet** dapat dipicu dari lebih dari satu titik (badge tier di S1, limit di S2) — bersifat "global popup", bukan bagian dari satu alur linear.
- **Kembali dari Reader ke S1** selalu menyimpan posisi baca terakhir secara otomatis (terpisah dari checkpoint manual).
- **Mode "Lengkapi artikel"** di S2 dapat diakses lewat dua jalur: dari Scrape Mode Sheet (pengguna memilih sendiri artikelnya) atau langsung dari Article Actions Sheet suatu artikel (target sudah otomatis terisi).
