package com.anjo.adb.util

import android.R

/**
 * Cada card pede um ícone por nome (ex "ic_target"). Pra não precisar
 * desenhar 50 vetores na mão, mapeamos pra ícones do próprio Android —
 * fica funcional de cara. Se quiser trocar por ícone bonito depois, é só
 * substituir o valor aqui por um @drawable seu.
 */
object IconProvider {
    fun resourceFor(nome: String): Int = when (nome) {
        "ic_target" -> R.drawable.ic_menu_mylocation
        "ic_refresh" -> R.drawable.ic_popup_sync
        "ic_spark" -> R.drawable.btn_star_big_on
        "ic_bolt" -> R.drawable.ic_menu_manage
        "ic_rocket" -> R.drawable.ic_menu_send
        "ic_sliders" -> R.drawable.ic_menu_preferences
        "ic_hz" -> R.drawable.ic_menu_recent_history
        "ic_broom" -> R.drawable.ic_menu_delete
        "ic_snow" -> R.drawable.ic_menu_close_clear_cancel
        "ic_sync_off" -> R.drawable.ic_menu_close_clear_cancel
        "ic_minus" -> R.drawable.ic_delete
        "ic_sun" -> R.drawable.ic_menu_day
        "ic_battery" -> R.drawable.ic_lock_idle_charging
        "ic_thermo" -> R.drawable.ic_dialog_alert
        "ic_gps_off" -> R.drawable.ic_menu_mapmode
        "ic_mic_off" -> R.drawable.ic_btn_speak_now
        "ic_palette" -> R.drawable.ic_menu_gallery
        "ic_restrict" -> R.drawable.ic_menu_close_clear_cancel
        "ic_sensor" -> R.drawable.ic_menu_compass
        "ic_eye" -> R.drawable.ic_menu_view
        "ic_cloud_off" -> R.drawable.ic_menu_upload
        "ic_alarm_off" -> R.drawable.ic_lock_idle_alarm
        "ic_pause" -> R.drawable.ic_media_pause
        "ic_store" -> R.drawable.ic_menu_agenda
        "ic_moon" -> R.drawable.ic_lock_idle_charging
        "ic_trash" -> R.drawable.ic_menu_delete
        "ic_feather" -> R.drawable.ic_menu_slideshow
        "ic_footstep" -> R.drawable.ic_menu_directions
        "ic_speaker" -> R.drawable.ic_lock_silent_mode_off
        "ic_wave" -> R.drawable.ic_menu_slideshow
        "ic_voice" -> R.drawable.ic_btn_speak_now
        "ic_vol_up" -> R.drawable.ic_lock_silent_mode_off
        "ic_limiter" -> R.drawable.ic_menu_preferences
        "ic_compress" -> R.drawable.ic_menu_manage
        "ic_headphones" -> R.drawable.ic_lock_silent_mode_off
        "ic_vol_auto" -> R.drawable.ic_lock_silent_mode_off
        "ic_mute" -> R.drawable.ic_lock_silent_mode
        "ic_wifi" -> R.drawable.ic_menu_mapmode
        "ic_wifi_lock" -> R.drawable.ic_menu_mapmode
        "ic_swap" -> R.drawable.ic_menu_rotate
        "ic_bluetooth_off" -> R.drawable.ic_menu_close_clear_cancel
        "ic_touch" -> R.drawable.ic_menu_edit
        "ic_vibrate" -> R.drawable.ic_lock_silent_mode_off
        else -> R.drawable.ic_menu_help
    }
}
