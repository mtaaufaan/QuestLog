package com.rds.questlog.presentation.articlelist

import org.junit.Assert.assertEquals
import org.junit.Test

class RelativeTimeTest {

    private val now = 1_000_000_000_000L
    private val minute = 60_000L
    private val hour = 60 * minute
    private val day = 24 * hour

    private fun age(ago: Long) = relativeAge(now - ago, now)

    @Test
    fun `kurang dari semenit adalah baru saja, termasuk waktu di masa depan`() {
        assertEquals(RelativeAge(AgeUnit.NOW), age(30_000))
        assertEquals(RelativeAge(AgeUnit.NOW), relativeAge(now + hour, now))
    }

    @Test
    fun `menit dan jam dibulatkan ke bawah`() {
        assertEquals(RelativeAge(AgeUnit.MINUTES, 1), age(minute))
        assertEquals(RelativeAge(AgeUnit.MINUTES, 59), age(59 * minute + 59_000))
        assertEquals(RelativeAge(AgeUnit.HOURS, 2), age(2 * hour + 40 * minute))
        assertEquals(RelativeAge(AgeUnit.HOURS, 23), age(23 * hour + 59 * minute))
    }

    @Test
    fun `satu hari penuh adalah kemarin, lalu hari, minggu, dan bulan`() {
        assertEquals(RelativeAge(AgeUnit.YESTERDAY), age(day + hour))
        assertEquals(RelativeAge(AgeUnit.DAYS, 3), age(3 * day))
        assertEquals(RelativeAge(AgeUnit.DAYS, 6), age(6 * day + 23 * hour))
        assertEquals(RelativeAge(AgeUnit.WEEKS, 1), age(7 * day))
        assertEquals(RelativeAge(AgeUnit.WEEKS, 4), age(29 * day))
        assertEquals(RelativeAge(AgeUnit.MONTHS, 1), age(30 * day))
        assertEquals(RelativeAge(AgeUnit.MONTHS, 12), age(365 * day))
    }
}
