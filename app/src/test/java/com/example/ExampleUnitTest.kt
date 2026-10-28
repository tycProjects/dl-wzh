package com.example

import com.example.model.AppConfig
import com.example.model.Category
import com.example.model.Sound
import org.junit.Assert.*
import org.junit.Test

class ExampleUnitTest {
  @Test
  fun testAppConfigDefaults() {
    val config = AppConfig()
    assertFalse(config.maintenance_mode)
    assertEquals("1.0", config.min_version)
    assertTrue(config.telegram_popup_enabled)
    assertTrue(config.user_uploads_enabled)
    assertEquals(10.0, config.upload_size_limit_mb, 0.01)
    assertTrue(config.ad_banner_enabled)
    assertTrue(config.maintenance_message.contains("এখানে বর্তমানে কাজ চলছে"))
  }

  @Test
  fun testSoundModel() {
    val sound = Sound(
      id = "s_test",
      title = "Sniper Shot",
      category_id = "cat_fps",
      audio_url = "synth://headshot",
      duration_seconds = 1.5
    )
    assertEquals("s_test", sound.id)
    assertEquals("Sniper Shot", sound.title)
    assertTrue(sound.is_published)
  }

  @Test
  fun testVersionOutdatedCalculation() {
    val installed = "1.0"
    val minRequiredNew = "2.0"
    val isOutdated = (installed.toDoubleOrNull() ?: 1.0) < (minRequiredNew.toDoubleOrNull() ?: 1.0)
    assertTrue(isOutdated)

    val minRequiredOld = "0.9"
    val isOutdatedOld = (installed.toDoubleOrNull() ?: 1.0) < (minRequiredOld.toDoubleOrNull() ?: 1.0)
    assertFalse(isOutdatedOld)
  }
}
