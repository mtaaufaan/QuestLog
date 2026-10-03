# QuestLog - Glossary

2026-09-19 · Disusun bersama @Taufan

Daftar istilah yang dipakai secara konsisten di seluruh dokumen QuestLog (PRD, Backlog, dan dokumen turunan lainnya), supaya semua pihak (dev, desainer, QA) memakai definisi yang sama.

| Istilah | Definisi |
| --- | --- |
| **Anchor (Checkpoint Anchor)** | Referensi checkpoint ke elemen konten tertentu (mis. ID paragraf/heading), bukan ke posisi pixel/scroll — supaya tetap akurat meski ukuran font atau orientasi layar berubah |
| **Append-only** | Aturan bahwa halaman baru yang ditambahkan ke file yang sudah ada selalu diletakkan di akhir urutan konten, tidak boleh disisipkan di tengah, agar anchor checkpoint yang sudah ada tidak bergeser |
| **Batas Halaman Sumber** | Metadata yang menandai elemen konten mana berasal dari URL/halaman sumber yang mana, dipakai untuk merender mode baca per halaman |
| **Checkpoint** | Penanda posisi baca terakhir pada sebuah artikel, dibuat manual oleh user, 1 checkpoint aktif per artikel |
| **Cloud Backup & Sync** | Fitur v2: menyimpan salinan data ke Google Drive pribadi milik pengguna (folder biasa, terlihat) untuk pemulihan data dan sinkronisasi dua arah antar device milik user yang sama |
| **Completionist** | Persona utama QuestLog: pemain RPG klasik/retro (via emulator) yang ingin menyelesaikan game 100% (semua item, sidequest, ending) dengan bantuan walkthrough |
| **Game Entity** | Objek data terstruktur (`id` + judul) yang merepresentasikan sebuah game; dipilih via autocomplete atau dibuat baru saat menyimpan artikel — bukan teks bebas |
| **Game Tag (MVP)** | Implementasi minimal dari Game Entity di MVP: metadata game per artikel tanpa tampilan/UI kategori khusus, dipakai untuk search dan filter sederhana |
| **Game Binder (v2)** | Fitur lanjutan yang menampilkan artikel terkelompok per game dengan sub-kategori (quest, item location, tips & trik) dan cover/ikon game |
| **Gamer dengan Koneksi Terbatas** | Persona sekunder QuestLog: pemain yang sering bermain dalam perjalanan/area tanpa sinyal, butuh akses panduan tanpa bergantung internet stabil |
| **Google Play Billing** | API resmi Google untuk memproses pembelian in-app (termasuk one-time purchase) di Android tanpa memerlukan backend/server milik developer |
| **In-App Purchase (IAP)** | Pembelian yang dilakukan di dalam aplikasi untuk membuka fitur/batasan tertentu; di QuestLog dipakai untuk unlock unlimited secara one-time (bukan langganan) |
| **Mode Per Halaman** | Mode tampilan baca yang memecah konten sesuai batas halaman sumber asli, dengan navigasi next/prev dan indikator posisi |
| **Mode Seamless** | Mode tampilan baca yang menampilkan seluruh konten gabungan sebagai satu scroll berkelanjutan tanpa jeda |
| **On-device** | Prinsip arsitektur bahwa pemrosesan (scraping) dan penyimpanan data dilakukan sepenuhnya di perangkat pengguna, tanpa mengirim data ke server milik developer |
| **One-time Purchase** | Model bisnis QuestLog: pengguna membayar sekali untuk membuka fitur/batas penuh, tanpa biaya berlangganan berkelanjutan |
| **Out of Scope** | Daftar fitur yang secara eksplisit tidak dikerjakan pada suatu fase (mis. MVP), untuk mencegah scope creep |
| **Smart Scraping (Gaming Mode)** | Konsep awal scraper yang secara otomatis membersihkan halaman web dari iklan/elemen pengganggu dan menyesuaikan ekstraksi untuk konten bertema game |
| **User Story** | Deskripsi kebutuhan dari sudut pandang pengguna dengan format "Sebagai \[peran\], saya ingin \[tujuan\], agar \[manfaat\]", dipakai di PRD sebagai representasi ringkas requirement |
| **v2 / Later** | Penanda bahwa sebuah fitur ditunda untuk dikerjakan setelah MVP dirilis, tercantum di PRD sebagai roadmap fitur lanjutan |
