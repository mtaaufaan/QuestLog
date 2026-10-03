# PRD - Game Guide Offline Reader (Working Title)

2026-09-19 · Disusun bersama @Taufan

## Problem

Gamer yang memainkan game RPG story-heavy atau game klasik/retro (dimainkan via emulator) sangat bergantung pada walkthrough dan panduan berbasis web untuk menyelesaikan game secara maksimal (quest, item, collectible, boss). Namun mereka menghadapi tiga masalah utama:

1. **Ketergantungan koneksi internet** — walkthrough hanya bisa diakses online, padahal sesi bermain (terutama di perangkat portabel/emulator) sering terjadi di tempat dengan koneksi terbatas atau tanpa internet sama sekali.
2. **Pengalaman baca yang buruk** — situs wiki/walkthrough besar umumnya sangat berat iklan dan elemen pengganggu, membuat pengalaman baca di mobile tidak nyaman.
3. **Kehilangan jejak progres** — tidak ada cara mudah untuk menandai "sudah sampai mana" dalam sebuah walkthrough panjang, sehingga pemain harus mencari ulang posisi terakhir setiap kali kembali membaca, terutama untuk walkthrough yang sangat panjang atau terpecah jadi banyak halaman (umum terjadi pada guide game klasik di GameFAQs).

## Target User

### Persona utama — Completionist RPG Klasik/Retro (via emulator)

Pemain RPG klasik/lawas (mis. Dragon Quest VII, Breath of Fire) yang dimainkan ulang lewat emulator di perangkat handheld atau Android. Ingin menyelesaikan game 100% (semua item, sidequest, ending) dengan bantuan walkthrough, tapi sumber panduannya tersebar di situs lama (GameFAQs, fan wiki) dengan format teks panjang dan sering terpecah jadi banyak halaman.

### Persona sekunder — Gamer dengan Koneksi Terbatas

Pemain yang sering bermain dalam perjalanan (transportasi umum, area tanpa sinyal) dan butuh akses panduan tanpa bergantung pada internet stabil saat itu juga.

*Catatan: persona "Trophy Hunter" pada draft awal disesuaikan menjadi "Completionist" karena game klasik/retro umumnya tidak memiliki sistem trophy/achievement bawaan.*

## Core Feature (MVP vs Later)

| Fitur | MVP | Later (v2+) |
| --- | --- | --- |
| Scraper | Ekstraksi teks/gambar/format penting, hilangkan iklan; input multi-URL saat pembuatan file; aksi "tambah halaman" ke file yang sudah ada (append-only) | Auto-detect pagination (deteksi otomatis halaman berikutnya + konfirmasi user) |
| Mode offline | Semua konten tersimpan lokal, dapat diakses penuh tanpa koneksi | Cloud backup & sync dua arah via Google Drive pribadi pengguna (folder biasa, terlihat & dapat dibuka manual di Drive user); saat konflik data antar device terdeteksi, sistem menanyakan ke user versi mana yang dipakai (bukan otomatis menimpa) |
| Checkpoint | Manual, 1 per artikel, anchor ke elemen konten (bukan pixel/scroll); auto-resume ke posisi terakhir dibaca saat kembali ke artikel | Checkpoint 1 per game (lintas artikel) |
| Game | Entitas terstruktur (id + judul), dipilih via autocomplete atau buat baru | Game Binder penuh (cover/ikon, sub-kategori: quest, item location, tips & trik) |
| Organisasi | Daftar flat + search + filter sederhana per game | Tampilan binder per-game, multi-kategori |
| Note | — (ditunda) | General note, inline note (terikat teks), daftar centang |
| Import/export & share | — (ditunda) | Export/import file (satu format file portable dipakai untuk backup ke Drive maupun share antar pengguna via kanal apa pun — tanpa infrastruktur server terpisah) |
| Bookmark antar-file | — (ditunda) | Bookmark yang saling terhubung lintas artikel |
| Model bisnis | Freemium — maks. 2 game dan maks. 5 artikel per game untuk tier gratis + one-time in-app purchase untuk unlock unlimited, 100% on-device (Google Play Billing) | Seluruh fitur (termasuk sync via Google Drive pribadi dan share via export/import file) tidak menimbulkan biaya server, sehingga tetap tercakup dalam one-time purchase; tier langganan berpotensi relevan di masa depan hanya jika ditambahkan fitur berbasis AI/API berbayar (saat ini di luar cakupan) |
| Bookmark antar-file | — | Bookmark antar-artikel dalam game yang sama, memanfaatkan struktur Game entity & filter per game yang sudah ada sejak MVP |
| Reading View | Mode seamless (scroll berkelanjutan) & mode per halaman (navigasi next/prev, indikator posisi), dapat di-toggle; memakai metadata batas halaman sumber | — |

## User Stories (MVP)

1. Sebagai pemain, saya ingin memasukkan URL halaman walkthrough agar kontennya (teks, gambar, format penting) tersimpan secara lokal di HP saya, bebas iklan.
2. Sebagai pemain, saya ingin menambahkan beberapa URL sekaligus saat menyimpan sebuah walkthrough, agar walkthrough yang aslinya terpecah jadi banyak halaman web tetap tersimpan sebagai satu file utuh.
3. Sebagai pemain, saya ingin bisa menambahkan halaman baru ke file walkthrough yang sudah tersimpan, agar saya tidak perlu membuat file terpisah saat menemukan halaman lanjutan.
4. Sebagai pemain, saya ingin membaca seluruh walkthrough yang sudah tersimpan sepenuhnya tanpa koneksi internet.
5. Sebagai pemain, saya ingin menandai (checkpoint) posisi terakhir saya baca dalam sebuah walkthrough, agar saat kembali saya bisa langsung melanjutkan dari titik tersebut.
6. Sebagai pemain, saya ingin memilih atau membuat entri game (judul) saat menyimpan walkthrough, agar setiap artikel selalu terhubung ke game yang benar dan konsisten (tanpa typo/duplikat).
7. Sebagai pemain, saya ingin memfilter daftar walkthrough berdasarkan game tertentu, agar saya mudah menemukan semua panduan untuk game yang sedang saya mainkan.
8. Sebagai pemain, saya ingin mencari walkthrough tersimpan berdasarkan judul, agar saya cepat menemukan artikel yang saya butuhkan tanpa harus scroll daftar panjang.
9. Sebagai pemain, saya ingin memilih antara mode baca seamless (scroll panjang) atau per halaman (mengikuti struktur asli sumber), sesuai preferensi saya.

10. Sebagai pemain, saat kembali ke artikel yang pernah saya baca, saya ingin otomatis dilanjutkan dari posisi terakhir saya, agar tidak perlu scroll dari awal.

## Requirements

### Functional

- Sistem dapat menerima input satu atau lebih URL dan mengekstrak teks, gambar, dan format penting (heading, list, tabel) dari halaman tersebut.
- Sistem menghilangkan elemen non-konten (iklan, navigasi, komentar, pop-up) dari hasil ekstraksi.
- Sistem menyimpan hasil ekstraksi sebagai struktur konten lokal (bukan HTML mentah) yang dapat dirender ulang secara konsisten di berbagai ukuran font/layar.
- Sistem menyediakan aksi "tambah halaman" pada file yang sudah ada; halaman baru selalu ditambahkan di akhir urutan konten (append-only).
- Sistem menyediakan reading mode dengan pengaturan dasar (ukuran font, dark/light mode).
- Sistem menyediakan checkpoint manual per artikel, tersimpan sebagai anchor ke elemen konten (bukan posisi pixel/scroll).
- Sistem menyediakan entitas Game (id + judul) dengan autocomplete saat pemilihan, serta opsi eksplisit untuk membuat entri Game baru.
- Sistem menyediakan daftar seluruh artikel tersimpan, dengan fungsi pencarian (judul) dan filter berdasarkan Game.
- Sistem membatasi tier gratis pada maks. 2 game dan maks. 5 artikel per game; pembelian sekali (one-time in-app purchase, Google Play Billing) membuka batas tersebut sepenuhnya.
- Sistem menyimpan metadata batas halaman sumber untuk setiap konten hasil gabungan multi-URL, dan menyediakan dua mode tampilan yang dapat di-toggle pengguna: seamless (scroll berkelanjutan) dan per halaman (navigasi next/prev dengan indikator posisi).
- Sistem menyimpan posisi baca terakhir per artikel secara otomatis saat pengguna meninggalkan layar baca, dan melanjutkan dari posisi tersebut saat artikel dibuka kembali.
- Saat scraping sebagian halaman berhasil dan sebagian gagal, sistem menampilkan konten yang berhasil dengan indikator bahwa artikel belum lengkap, serta menyediakan aksi retry khusus untuk halaman yang gagal saja.

### Non-Functional

- Seluruh fitur MVP berfungsi penuh tanpa koneksi internet, kecuali proses awal pengambilan/scraping konten dari URL.
- Proses scraping dan penyimpanan dilakukan sepenuhnya on-device, tanpa mengirim data pengguna ke server pihak mana pun.
- Status pembelian (unlock) diverifikasi via `BillingClient.queryPurchasesAsync()` setiap kali aplikasi dibuka, dan di-cache lokal di perangkat (tabel `app_config`).
- Ukuran penyimpanan gambar dikelola (kompresi) agar tidak membebani ruang penyimpanan HP secara berlebihan.
- Aplikasi tetap responsif saat memproses artikel dengan jumlah halaman gabungan yang banyak.

## Edge Case

- **Format konten sangat bervariasi** — walkthrough game klasik (gaya GameFAQs: teks polos, ASCII map) vs wiki modern (heading/tabel/infobox terstruktur) memiliki struktur HTML yang sangat berbeda; scraper harus diuji terhadap kedua gaya ini, bukan hanya salah satu.
- **URL tidak dapat diakses/berubah** — halaman sumber sudah dihapus, dipindah, atau butuh login/paywall saat proses scraping dijalankan.
- **Halaman berisi sangat sedikit teks/hanya gambar** (mis. screenshot map collectible) — ekstraksi teks gagal menangkap inti konten karena informasi utamanya ada di gambar.
- **Struktur situs berubah setelah update** — parser yang berhasil untuk satu artikel bisa gagal di artikel lain dari domain yang sama karena perbedaan template halaman.
- **User menambah halaman dengan urutan salah** — misalnya menambah "Part 3" sebelum "Part 2"; karena append-only, urutan akhir mengikuti urutan input, berpotensi salah urut jika user tidak teliti.
- **Checkpoint menjadi tidak valid** — jika anchor konten yang dijadikan rujukan checkpoint terhapus/berubah signifikan setelah penambahan halaman baru (perlu strategi fallback, misalnya checkpoint mengarah ke elemen terdekat yang masih ada).
- **Duplikasi entri Game akibat variasi penulisan** — meski sudah pakai autocomplete, potensi duplikat tetap ada bila user sengaja membuat entri baru yang mirip (mis. "Dragon Quest 7" vs "Dragon Quest VII").
- **Batas penyimpanan HP penuh** — proses scraping/penyimpanan gambar gagal atau terhenti di tengah jalan karena kapasitas penyimpanan device habis.
- **Pembelian in-app gagal terverifikasi** — status unlock tidak konsisten setelah reinstall aplikasi atau pergantian akun Google Play pada device yang sama.
- **Scraping artikel parsial** — sebagian URL berhasil di-scrape, sebagian gagal; konten yang berhasil tetap ditampilkan dengan indikator "belum lengkap" dan tombol retry khusus untuk halaman yang gagal.
- Batas halaman jatuh di tengah kalimat/daftar akibat pemisahan sumber asli — mode per halaman berpotensi terlihat janggal jika transisi halaman terjadi di tengah konten yang seharusnya menyatu.
- Konflik data saat sync dua arah antar device (data diubah di dua device sebelum sempat sinkron) — sistem menampilkan pilihan ke user untuk menentukan versi mana yang dipakai.

## Success Metrics

| Metrik | Target Indikatif | Tujuan |
| --- | --- | --- |
| Tingkat keberhasilan scraping | > 90% URL yang dicoba berhasil diekstrak dengan rapi (teks + gambar utuh) | Validasi kualitas inti fitur scraper |
| Retensi 7 hari & 30 hari | Dipantau sejak rilis, dibandingkan rata-rata app read-it-later sejenis | Validasi apakah checkpoint benar-benar jadi hook retensi |
| Jumlah artikel tersimpan per user aktif | Rata-rata > 5 artikel dalam 2 minggu pertama | Indikasi kebiasaan pakai yang terbentuk |
| Penggunaan fitur checkpoint | > 50% user yang menyimpan 2+ artikel juga memakai checkpoint minimal sekali | Validasi checkpoint sebagai fitur pembeda, bukan sekadar ada |
| Conversion rate ke unlock (IAP) | Ditentukan setelah baseline rilis awal (benchmark industri freemium utility: 2-5%) | Validasi model bisnis freemium + one-time purchase |
| Rating & ulasan Play Store | Rata-rata minimal 4.0, dengan feedback kualitatif dari komunitas niche (r/emulation, r/JRPG) | Validasi product-market fit di niche RPG klasik/retro |

## Out of Scope (MVP)

- Personal note (inline note, general note, daftar centang)
- Import/export file dan share antar pengguna
- Bookmark antar-artikel dalam game yang sama (cross-reference dibatasi ke game yang sama)
- Game Binder penuh (sub-kategori quest, item location, tips & trik; cover/ikon game)
- Checkpoint 1 per game (lintas artikel)
- Auto-detect pagination (deteksi otomatis halaman lanjutan tanpa input manual)
- Cloud sync / backup antar device (direncanakan via Google Drive pribadi pengguna di v2, folder biasa yang terlihat & dapat diakses manual)
- Akun pengguna dan sistem autentikasi
- Fitur berbasis AI (smart extraction berbasis AI, auto-suggest nama game dari judul artikel)
- Text-to-speech, widget/quick-access overlay, spoiler-safe mode
- Model langganan (subscription) — MVP hanya freemium + one-time in-app purchase
