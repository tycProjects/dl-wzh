package com.fakecall.app

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

enum class Screen { Incoming, Active, Ended }

/** حالة التطبيق كلها. الواجهة بتقرأ منها، والأزرار بتنادي دوالها. */
class AppState(private val ctx: Context, val audio: CallAudio) {

    var screen by mutableStateOf(Screen.Incoming)
    var ringing by mutableStateOf(false)
    var seconds by mutableStateOf(0)
    var finalSeconds by mutableStateOf(0)
    var callId by mutableStateOf(0)

    var profiles by mutableStateOf(Store.profiles(ctx))
    var activeId by mutableStateOf(Store.activeId(ctx))
    var photoVersion by mutableStateOf(0)

    var settingsOpen by mutableStateOf(false)
    var editorOpen by mutableStateOf(false)
    var ringName by mutableStateOf(Store.ringName(ctx))
    var scheduledAt by mutableStateOf(Store.scheduledAt(ctx))

    var toast by mutableStateOf<Pair<Int, String>?>(null)
    private var toastCounter = 0

    // محرر البروفايل
    var edId by mutableStateOf("")
    var edName by mutableStateOf("")
    var edNumber by mutableStateOf("")
    var edVoiceName by mutableStateOf("")
    var edIsNew by mutableStateOf(false)

    val active: Profile
        get() = profiles.firstOrNull { it.id == activeId } ?: profiles.first()

    fun showToast(msg: String) {
        toastCounter++
        toast = Pair(toastCounter, msg)
    }

    fun refresh() {
        profiles = Store.profiles(ctx)
        activeId = Store.activeId(ctx)
        ringName = Store.ringName(ctx)
        scheduledAt = Store.scheduledAt(ctx)
        photoVersion++
    }

    // ───────── المكالمة ─────────

    fun startRing() {
        if (ringing) return
        ringing = true
        if (!audio.startRing(Store.vibrate(ctx))) showToast("تعذّر تشغيل النغمة")
    }

    fun stopRing() {
        ringing = false
        audio.stopRing()
    }

    /** بتتنادى لما المنبّه المجدول يفتح التطبيق (ringNow = true). */
    fun showIncoming(ringNow: Boolean) {
        if (screen == Screen.Active) audio.end()
        settingsOpen = false
        editorOpen = false
        refresh()
        seconds = 0
        screen = Screen.Incoming
        ringing = false
        if (ringNow) startRing()
    }

    fun accept() {
        ringing = false
        callId++
        seconds = 0
        audio.onAnswered(Store.voiceFile(ctx, activeId))
        screen = Screen.Active
    }

    fun end() {
        finalSeconds = seconds
        audio.end()
        screen = Screen.Ended
    }

    fun decline() {
        stopRing()
        finalSeconds = 0
        audio.end()
        screen = Screen.Ended
    }

    fun restart() {
        seconds = 0
        screen = Screen.Incoming
        startRing()
    }

    // ───────── الإعدادات والبروفايلات ─────────

    fun openSettings() {
        if (settingsOpen) return
        audio.tick()
        refresh()
        settingsOpen = true
    }

    fun closeSettings() {
        settingsOpen = false
    }

    fun select(id: String) {
        Store.setActive(ctx, id)
        activeId = id
        settingsOpen = false
    }

    fun openEditor(id: String?) {
        val p = profiles.firstOrNull { it.id == id }
        edIsNew = p == null
        edId = p?.id ?: Store.newId()
        edName = p?.name ?: ""
        edNumber = p?.number ?: ""
        edVoiceName = p?.voiceName ?: ""
        editorOpen = true
    }

    fun saveEditor() {
        if (edName.isBlank()) {
            showToast("اكتب اسم المتصل")
            return
        }
        val hasVoice = Store.voiceFile(ctx, edId).exists()
        val np = Profile(edId, edName.trim(), edNumber.trim(), if (hasVoice) edVoiceName else "")
        val list = if (edIsNew) profiles + np else profiles.map { if (it.id == edId) np else it }
        Store.saveProfiles(ctx, list)
        editorOpen = false
        refresh()
    }

    fun cancelEditor() {
        if (edIsNew) Store.deleteFiles(ctx, edId)
        editorOpen = false
    }

    fun deleteEditor() {
        if (profiles.size <= 1) return
        val list = profiles.filter { it.id != edId }
        Store.deleteFiles(ctx, edId)
        Store.saveProfiles(ctx, list)
        if (activeId == edId) Store.setActive(ctx, list.first().id)
        editorOpen = false
        refresh()
    }

    fun clearPhoto() {
        Store.photoFile(ctx, edId).delete()
        Store.thumbFile(ctx, edId).delete()
        photoVersion++
    }

    fun clearVoice() {
        Store.voiceFile(ctx, edId).delete()
        edVoiceName = ""
    }

    // ───────── النغمة العامة والجدولة ─────────

    fun resetRing() {
        Store.resetRing(ctx)
        ringName = ""
    }

    fun setVibrate(v: Boolean) = Store.setVibrate(ctx, v)

    fun schedule(seconds: Int) {
        val s = seconds.coerceAtLeast(3)
        Store.setDelay(ctx, s)
        if (Scheduler.schedule(ctx, s)) {
            scheduledAt = Store.scheduledAt(ctx)
            showToast("تمت الجدولة. تقدر تقفل التطبيق")
        } else {
            showToast("تعذّرت الجدولة")
        }
    }

    fun cancelSchedule() {
        Scheduler.cancel(ctx)
        scheduledAt = 0L
    }

    // ───────── زرار الرجوع ─────────

    fun onBack(finish: () -> Unit) {
        when {
            editorOpen -> cancelEditor()
            settingsOpen -> closeSettings()
            screen == Screen.Incoming -> decline()
            screen == Screen.Ended -> finish()
            else -> Unit // وأنت في مكالمة: الرجوع متعطّل، لازم تقفل بزرار الإنهاء
        }
    }
}
