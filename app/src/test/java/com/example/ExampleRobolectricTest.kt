package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.StreakInfo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Вогник", appName)
    }

    @Test
    fun `test fire tier calculations`() {
        val zeroStreak = StreakInfo(currentStreak = 0)
        assertEquals(0, zeroStreak.fireLevelNumber)

        val threeDayStreak = StreakInfo(currentStreak = 3)
        assertEquals(1, threeDayStreak.fireLevelNumber)
        assertEquals("Маленька іскра", threeDayStreak.fireTierTitle)

        val sevenDayStreak = StreakInfo(currentStreak = 7)
        assertEquals(2, sevenDayStreak.fireLevelNumber)
        assertEquals("Впевнене полум'я", sevenDayStreak.fireTierTitle)

        val tenDayStreak = StreakInfo(currentStreak = 10)
        assertEquals(3, tenDayStreak.fireLevelNumber)
        assertEquals("Гаряче багаття", tenDayStreak.fireTierTitle)
    }
}
