package com.rds.questlog.data.local

import org.junit.Assert.assertEquals
import org.junit.Test

class LikeEscapeTest {

    @Test
    fun `karakter khusus LIKE di-escape`() {
        assertEquals("100\\% \\_ a\\\\b", escapeLike("100% _ a\\b"))
    }

    @Test
    fun `teks biasa tidak berubah`() {
        assertEquals("Walkthrough Utama", escapeLike("Walkthrough Utama"))
        assertEquals("", escapeLike(""))
    }
}
