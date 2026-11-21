package com.android.island.ai

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class InputPickerTest {
    @Test fun messageBoxAtBottomIsChosenOverSearchBar() {
        val fields = listOf(
            InputPicker.Field(top = 120, hint = "Cari", focused = true),
            InputPicker.Field(top = 1300, hint = "Ketik pesan", focused = false)
        )
        assertEquals(1, InputPicker.pick(fields))
    }

    @Test fun onlySearchFieldMeansNoTarget() {
        assertNull(InputPicker.pick(listOf(InputPicker.Field(top = 120, hint = "Search", focused = true))))
    }

    @Test fun passwordFieldsAreNeverChosen() {
        assertNull(InputPicker.pick(listOf(InputPicker.Field(top = 900, hint = "Kata sandi", focused = true, password = true))))
    }
}
