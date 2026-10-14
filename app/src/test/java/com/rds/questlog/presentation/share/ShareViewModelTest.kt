package com.rds.questlog.presentation.share

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ShareViewModelTest {

    @Test
    fun `teks berisi URL menjadi event Url`() {
        val vm = ShareViewModel()

        vm.onSharedText("Walkthrough https://a.com/x.")

        assertEquals(ShareEvent.Url("https://a.com/x"), vm.event.value)
    }

    @Test
    fun `teks tanpa URL valid menjadi event NoUrl`() {
        val vm = ShareViewModel()

        listOf(null, "", "tanpa url", "http://tidak-aman.com").forEach { text ->
            vm.onSharedText(text)

            assertEquals(ShareEvent.NoUrl, vm.event.value)
        }
    }

    @Test
    fun `event hilang setelah ditangani`() {
        val vm = ShareViewModel()
        vm.onSharedText("https://a.com/x")

        vm.consume()

        assertNull(vm.event.value)
    }
}
