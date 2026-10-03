# Database Schema — QuestLog

> Dokumen ini adalah referensi schema Room Database untuk QuestLog.
> Mencerminkan semua keputusan desain dari gap analysis (September 2026).

---

## Diagram Relasi

```
games
  └── articles (game_id → games.id)
        ├── source_pages (article_id → articles.id)
        │     └── content_nodes (source_page_id → source_pages.id)
        ├── content_nodes (article_id → articles.id)
        ├── checkpoints (article_id → articles.id)  [1:1]
        └── images (article_id → articles.id)

app_config  [standalone, no FK]
```

---

## DDL Lengkap

### 1. games

```sql
CREATE TABLE games (
    id          INTEGER PRIMARY KEY AUTOINCREMENT,
    title       TEXT    NOT NULL,
    created_at  INTEGER NOT NULL  -- Unix epoch milliseconds
);
```

### 2. articles

```sql
CREATE TABLE articles (
    id               INTEGER PRIMARY KEY AUTOINCREMENT,
    game_id          INTEGER NOT NULL REFERENCES games(id) ON DELETE CASCADE,
    title            TEXT    NOT NULL,
    is_scraping_done INTEGER NOT NULL DEFAULT 0,
    -- 0 = WorkManager masih berjalan atau ada page PENDING/IN_PROGRESS
    -- 1 = semua source_pages berstatus COMPLETED atau FAILED
    created_at       INTEGER NOT NULL
);
```

### 3. source_pages

```sql
CREATE TABLE source_pages (
    id          INTEGER PRIMARY KEY AUTOINCREMENT,
    article_id  INTEGER NOT NULL REFERENCES articles(id) ON DELETE CASCADE,
    source_url  TEXT    NOT NULL,
    page_order  INTEGER NOT NULL,
    status      TEXT    NOT NULL DEFAULT 'PENDING',
    -- PENDING | IN_PROGRESS | COMPLETED | FAILED
    order_start INTEGER,  -- reserved range start (mis. 1, 1001, 2001, ...)
    order_end   INTEGER   -- reserved range end   (mis. 1000, 2000, 3000, ...)
);
```

**Catatan order_start / order_end:**
Setiap source_page mendapat slot 1000 `display_order` yang dipesan di muka.
Jika page 3 awalnya gagal kemudian di-retry, content_nodes-nya dapat disisipkan
di antara page 2 (slot 2001–3000) dan page 4 (slot 3001–4000) tanpa menggeser
node yang sudah ada.

### 4. content_nodes

```sql
CREATE TABLE content_nodes (
    id              INTEGER PRIMARY KEY AUTOINCREMENT,
    article_id      INTEGER NOT NULL REFERENCES articles(id)      ON DELETE CASCADE,
    source_page_id  INTEGER NOT NULL REFERENCES source_pages(id),
    node_type       TEXT    NOT NULL,
    -- h1, h2, h3, p, p_cont, img, pre, table, li
    display_order   INTEGER NOT NULL,
    text_content    TEXT,
    metadata_json   TEXT
    -- img  → { "path": "/filesDir/images/abc123.webp", "alt": "..." }
    -- table→ { "html": "<table>...</table>" }
    -- p_cont tidak berbeda secara visual; hanya untuk granularitas checkpoint
);

CREATE INDEX idx_content_nodes_article_order
    ON content_nodes (article_id, display_order);

CREATE INDEX idx_content_nodes_source_page
    ON content_nodes (source_page_id);
```

**Tipe node (`node_type`):**

| Nilai | Deskripsi |
|---|---|
| `h1`, `h2`, `h3` | Heading |
| `p` | Paragraf pendek (≤400 karakter atau ≤3 kalimat) |
| `p_cont` | Kelanjutan paragraf panjang (hasil split otomatis) |
| `img` | Gambar — path WebP lokal di `metadata_json` |
| `pre` | Blok preformatted / ASCII art |
| `table` | Tabel — HTML mentah di `metadata_json` |
| `li` | Item list |

### 5. checkpoints

```sql
CREATE TABLE checkpoints (
    article_id           INTEGER PRIMARY KEY REFERENCES articles(id) ON DELETE CASCADE,
    anchor_node_id       INTEGER NOT NULL REFERENCES content_nodes(id),
    -- checkpoint MANUAL yang disengaja user
    fallback_order       INTEGER NOT NULL,
    -- display_order dari anchor_node; dipakai jika anchor_node terhapus
    last_visited_node_id INTEGER REFERENCES content_nodes(id),
    -- posisi terakhir AUTO-SAVED saat user meninggalkan layar baca (MVP)
    last_read_at         INTEGER,
    -- timestamp kunjungan terakhir (schema tersedia, UI v2)
    read_progress        REAL,
    -- 0.0–1.0, persentase scroll artikel (schema tersedia, UI v2)
    updated_at           INTEGER NOT NULL
);
```

**Perbedaan `anchor_node_id` vs `last_visited_node_id`:**

| Kolom | Siapa yang mengisi | Kapan dipakai |
|---|---|---|
| `anchor_node_id` | User (tap tombol checkpoint) | "Lanjutkan Baca" dari daftar artikel |
| `last_visited_node_id` | Sistem otomatis (`DisposableEffect.onDispose`) | Auto-scroll saat artikel dibuka kembali |

**Pola implementasi auto-resume:**
```kotlin
// Simpan saat keluar layar
DisposableEffect(Unit) {
    onDispose {
        val nodeId = contentNodes.getOrNull(listState.firstVisibleItemIndex)?.id
        if (nodeId != null) viewModel.saveLastPosition(articleId, nodeId)
    }
}

// Resume saat buka artikel
LaunchedEffect(lastVisitedNodeId) {
    lastVisitedNodeId?.let { nodeId ->
        val index = contentNodes.indexOfFirst { it.id == nodeId }
        if (index >= 0) listState.scrollToItem(index)
    }
}
```

**Fallback checkpoint manual:**
```sql
-- Dipakai jika anchor_node_id sudah tidak ada setelah re-scrape
SELECT id FROM content_nodes
WHERE article_id = :articleId
  AND display_order >= :fallbackOrder
ORDER BY display_order ASC
LIMIT 1;
```

### 6. images

```sql
CREATE TABLE images (
    id          INTEGER PRIMARY KEY AUTOINCREMENT,
    article_id  INTEGER NOT NULL REFERENCES articles(id) ON DELETE CASCADE,
    filename    TEXT    NOT NULL,  -- MD5(source_url) + ".webp"
    file_path   TEXT    NOT NULL,  -- path absolut di filesDir
    file_size   INTEGER,           -- bytes, diisi setelah kompresi
    source_url  TEXT    NOT NULL,  -- URL asli (untuk deduplication)
    created_at  INTEGER NOT NULL
);
```

**Catatan deduplication:**
`filename` = MD5(source_url) + ".webp" — URL yang sama menghasilkan filename yang
sama. Sebelum menghapus file fisik, cek dulu jumlah referensi:
```sql
SELECT COUNT(*) FROM images WHERE filename = :filename;
```
File fisik hanya dihapus jika `COUNT = 0`.

**Image cleanup** dijalankan via WorkManager dengan `ExistingWorkPolicy.KEEP`
setelah artikel dihapus.

### 7. app_config

```sql
CREATE TABLE app_config (
    key   TEXT PRIMARY KEY,
    value TEXT NOT NULL
);
-- Rows yang digunakan:
-- ('is_premium',           '0' | '1')   -- cache dari queryPurchasesAsync
-- ('purchase_token',       '<token>')    -- token dari Google Play
-- ('purchase_verified_at', '<epoch>')   -- timestamp verifikasi terakhir
```

**Alur verifikasi premium:**
1. App launch → `BillingClient.queryPurchasesAsync()`
2. Jika ada purchase aktif → update `is_premium = '1'` dan simpan token
3. UseCase layer membaca `is_premium` sebelum membuat game/artikel baru
4. Jika `is_premium = '0'` dan limit tercapai → tampilkan paywall

---

## Ringkasan Index

| Index | Tabel | Kolom | Tujuan |
|---|---|---|---|
| `idx_content_nodes_article_order` | `content_nodes` | `(article_id, display_order)` | Query rendering — ambil semua node artikel berurutan |
| `idx_content_nodes_source_page` | `content_nodes` | `(source_page_id)` | Hapus node per page saat retry parsial |
| PK implisit | Semua tabel | `id` | Lookup by ID |
| FK cascade | `articles`, `source_pages`, `content_nodes`, `checkpoints`, `images` | `*_id` | Delete propagation |

---

## Batasan Free Tier

Enforcement dilakukan di **UseCase layer** (bukan DB constraint):

| Entitas | Limit Free | Cara cek |
|---|---|---|
| Game | Maks. 2 | `SELECT COUNT(*) FROM games` |
| Artikel per game | Maks. 5 | `SELECT COUNT(*) FROM articles WHERE game_id = ?` |

Jika limit tercapai dan `is_premium = '0'` → UseCase mengembalikan error `FreeTierLimitReached`, bukan `Exception`. Artikel/game yang sudah tersimpan sebelum mencapai batas tetap dapat diakses penuh.

---

## Kolom yang Disimpan untuk v2 (Schema Tersedia, UI Ditunda)

| Tabel | Kolom | Rencana v2 |
|---|---|---|
| `checkpoints` | `last_read_at` | Tampilkan "Terakhir dibaca: X hari lalu" |
| `checkpoints` | `read_progress` | Progress bar per artikel di daftar |

---

*Dokumen dibuat: September 2026*
*Dibuat bersama: Taufan (Product Owner) × Claude (Software Architect)*
