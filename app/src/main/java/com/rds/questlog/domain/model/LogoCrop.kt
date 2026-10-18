package com.rds.questlog.domain.model

/** Sisi bingkai crop logo dalam "satuan bingkai" (dp di layar Logo game); zoom dan posisi disimpan dalam satuan ini. */
const val LOGO_FRAME = 264f

/** Batas zoom crop logo. */
const val LOGO_MIN_ZOOM = 1f
const val LOGO_MAX_ZOOM = 4f

/**
 * Posisi crop logo: [zoom] 1..4 dari ukuran "cover" (sisi terpendek gambar = bingkai), [x]/[y] = geseran pusat gambar
 * dari pusat bingkai dalam satuan bingkai (positif = gambar bergeser ke kanan/bawah).
 */
data class LogoCrop(val zoom: Float = 1f, val x: Float = 0f, val y: Float = 0f)

/** Area persegi di gambar sumber (piksel) yang berada di dalam bingkai. */
data class LogoSourceRect(val left: Float, val top: Float, val size: Float)

/**
 * Crop yang sudah dibatasi agar bingkai selalu tertutup gambar berperbandingan [aspect] (lebar / tinggi): zoom ke
 * 1..4 dan geseran tidak melewati tepi gambar.
 */
fun LogoCrop.clamped(aspect: Float): LogoCrop {
    val ratio = if (aspect.isFinite() && aspect > 0f) aspect else 1f
    val z = zoom.coerceIn(LOGO_MIN_ZOOM, LOGO_MAX_ZOOM)
    val width = LOGO_FRAME * z * maxOf(1f, ratio)
    val height = LOGO_FRAME * z * maxOf(1f, 1f / ratio)
    val maxX = ((width - LOGO_FRAME) / 2f).coerceAtLeast(0f)
    val maxY = ((height - LOGO_FRAME) / 2f).coerceAtLeast(0f)
    return LogoCrop(z, x.coerceIn(-maxX, maxX), y.coerceIn(-maxY, maxY))
}

/** Area gambar sumber [width] x [height] piksel yang tampak di bingkai untuk crop ini (setelah [clamped]). */
fun LogoCrop.sourceRect(width: Int, height: Int): LogoSourceRect {
    val c = clamped(width.toFloat() / height)
    // Satuan bingkai per piksel sumber; sisi bingkai setara LOGO_FRAME / scale piksel.
    val scale = LOGO_FRAME / minOf(width, height) * c.zoom
    val size = LOGO_FRAME / scale
    return LogoSourceRect(
        left = width / 2f - c.x / scale - size / 2f,
        top = height / 2f - c.y / scale - size / 2f,
        size = size,
    )
}
