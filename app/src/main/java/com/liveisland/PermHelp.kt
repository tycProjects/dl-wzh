package com.liveisland

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import com.google.android.material.dialog.MaterialAlertDialogBuilder

/**
 * Объяснение разрешения перед тем, как открыть настройки:
 * что это, почему безопасно, пошагово, и что делать, если система запрещает.
 */
object PermHelp {
  fun overlay(a: Activity) = show(a, R.string.step1_title, R.string.perm_overlay_why,
    R.string.perm_overlay_steps, R.string.perm_denied_overlay) {
    Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${a.packageName}"))
  }

  fun listener(a: Activity) = show(a, R.string.step2_title, R.string.perm_listener_why,
    R.string.perm_listener_steps, R.string.perm_denied_listener) {
    Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
  }

  fun install(a: Activity) = show(a, R.string.perm_install_title, R.string.perm_install_why,
    R.string.perm_install_steps, R.string.perm_denied_install) {
    Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${a.packageName}"))
  }

  fun battery(a: Activity) = show(a, R.string.perm_battery_title, R.string.perm_battery_why,
    R.string.perm_battery_steps, R.string.perm_denied_battery) {
    Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
  }

  private fun show(a: Activity, titleRes: Int, whyRes: Int, stepsRes: Int, deniedRes: Int, intent: () -> Intent) {
    val msg = a.getString(whyRes) + "\n\n" + a.getString(stepsRes) + "\n\n" + a.getString(R.string.perm_safe_text)
    MaterialAlertDialogBuilder(a)
      .setTitle(titleRes)
      .setMessage(msg)
      .setPositiveButton(R.string.perm_open) { _, _ ->
        try { a.startActivity(intent()) } catch (_: Exception) {}
      }
      .setNegativeButton(R.string.update_later, null)
      .setNeutralButton(R.string.perm_denied_btn) { _, _ ->
        MaterialAlertDialogBuilder(a)
          .setTitle(R.string.perm_denied_title)
          .setMessage(a.getString(deniedRes))
          .setPositiveButton(R.string.ok, null)
          .show()
      }
      .show()
  }
}
