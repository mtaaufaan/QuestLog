package com.rds.questlog.presentation.articlelist

import android.content.Context
import com.rds.questlog.R
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

enum class AgeUnit { NOW, MINUTES, HOURS, YESTERDAY, DAYS, WEEKS, MONTHS }

/** Selisih waktu yang sudah dibulatkan ke satuan terbesar; [count] tidak bermakna untuk NOW dan YESTERDAY. */
data class RelativeAge(val unit: AgeUnit, val count: Int = 0)

private const val MINUTE_MS = 60_000L
private const val HOUR_MS = 60 * MINUTE_MS
private const val DAY_MS = 24 * HOUR_MS
private const val DAYS_PER_WEEK = 7
private const val DAYS_PER_MONTH = 30

/** Umur [then] terhadap [now]: <1 menit "baru saja", menit, jam, kemarin (1 hari), hari (<1 minggu), minggu, bulan. */
fun relativeAge(then: Long, now: Long): RelativeAge {
    val diff = (now - then).coerceAtLeast(0)
    val days = (diff / DAY_MS).toInt()
    return when {
        diff < MINUTE_MS -> RelativeAge(AgeUnit.NOW)
        diff < HOUR_MS -> RelativeAge(AgeUnit.MINUTES, (diff / MINUTE_MS).toInt())
        diff < DAY_MS -> RelativeAge(AgeUnit.HOURS, (diff / HOUR_MS).toInt())
        days == 1 -> RelativeAge(AgeUnit.YESTERDAY)
        days < DAYS_PER_WEEK -> RelativeAge(AgeUnit.DAYS, days)
        days < DAYS_PER_MONTH -> RelativeAge(AgeUnit.WEEKS, days / DAYS_PER_WEEK)
        else -> RelativeAge(AgeUnit.MONTHS, days / DAYS_PER_MONTH)
    }
}

/** Label waktu relatif berbahasa Indonesia, mis. "2 jam lalu", untuk "Terakhir: …" dan "dibaca …". */
class RelativeTimeFormatter @Inject constructor(@ApplicationContext private val context: Context) {

    fun format(then: Long, now: Long = System.currentTimeMillis()): String {
        val age = relativeAge(then, now)
        return when (age.unit) {
            AgeUnit.NOW -> context.getString(R.string.relative_now)
            AgeUnit.MINUTES -> context.getString(R.string.relative_minutes, age.count)
            AgeUnit.HOURS -> context.getString(R.string.relative_hours, age.count)
            AgeUnit.YESTERDAY -> context.getString(R.string.relative_yesterday)
            AgeUnit.DAYS -> context.getString(R.string.relative_days, age.count)
            AgeUnit.WEEKS -> context.getString(R.string.relative_weeks, age.count)
            AgeUnit.MONTHS -> context.getString(R.string.relative_months, age.count)
        }
    }
}
