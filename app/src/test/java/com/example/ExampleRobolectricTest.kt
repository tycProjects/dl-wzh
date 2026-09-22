package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.InitialDictionaryData
import com.example.data.model.LanguageDirection
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Bisaya Subanen", appName)
  }

  @Test
  fun `verify sample dictionary data is marked as needs verification`() {
    val sampleEntries = InitialDictionaryData.SAMPLE_TEST_ENTRIES
    assertTrue(sampleEntries.isNotEmpty())
    sampleEntries.forEach { entry ->
      assertEquals("Needs verification", entry.verificationStatus)
      assertTrue(entry.notes.contains("[TEST PLACEHOLDER"))
      assertNotNull(entry.getSourceWord(LanguageDirection.BISAYA_TO_SUBANEN))
      assertNotNull(entry.getTranslationWord(LanguageDirection.BISAYA_TO_SUBANEN))
    }
  }
}

