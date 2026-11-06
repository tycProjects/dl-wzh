// app/src/main/java/com/hyperboost/engine/ui/FfViewModel.kt
package com.hyperboost.engine.ui

import android.app.Application
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.hyperboost.engine.core.ColorSettings
import com.hyperboost.engine.core.FfOptions
import com.hyperboost.engine.core.FreeFire
import com.hyperboost.engine.core.FreeFireBoost
import com.hyperboost.engine.services.FreeFireWatcherService
import com.hyperboost.engine.services.GameSidebarService
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/** State and actions of the Free Fire Game Turbo screen. */
class FfViewModel(app: Application) : AndroidViewModel(app) {

    private val ctx = app.applicationContext
    private val boost = FreeFireBoost(ctx)

    private val _opts = MutableStateFlow(boost.loadOptions())
    val opts: StateFlow<FfOptions> = _opts.asStateFlow()

    private val _color = MutableStateFlow(boost.loadColor())
    val color: StateFlow<ColorSettings> = _color.asStateFlow()

    private val _games = MutableStateFlow<List<FreeFire.Variant>>(emptyList())
    val games: StateFlow<List<FreeFire.Variant>> = _games.asStateFlow()

    private val _selected = MutableStateFlow<String?>(null)
    val selected: StateFlow<String?> = _selected.asStateFlow()

    private val _active = MutableStateFlow(boost.isActive())
    val active: StateFlow<Boolean> = _active.asStateFlow()

    private val _busy = MutableStateFlow(false)
    val busy: StateFlow<Boolean> = _busy.asStateFlow()

    private val _message = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val message: SharedFlow<String> = _message.asSharedFlow()

    init { refresh() }

    fun refresh() {
        val list = FreeFire.installed(ctx.packageManager)
        _games.value = list
        if (_selected.value == null || list.none { it.pkg == _selected.value }) _selected.value = list.firstOrNull()?.pkg
        _active.value = boost.isActive()
    }

    fun select(pkg: String) { _selected.value = pkg }

    fun update(change: (FfOptions) -> FfOptions) {
        val old = _opts.value
        val new = change(old)
        _opts.value = new
        boost.saveOptions(new)
        if (new.autoStart != old.autoStart) {
            if (new.autoStart) FreeFireWatcherService.start(ctx)
            else if (!_active.value) FreeFireWatcherService.stop(ctx)
        }
    }

    private fun say(text: String) { _message.tryEmit(text) }

    private fun variant() = _games.value.firstOrNull { it.pkg == _selected.value }

    /** Boosts the phone, opens Free Fire and shows the in-game sidebar. */
    fun play() {
        val v = variant() ?: return say("Chưa cài Free Fire trên máy này")
        viewModelScope.launch {
            _busy.value = true
            try {
                val o = _opts.value
                val fails = boost.apply(v.pkg, o)
                _active.value = true
                FreeFireWatcherService.start(ctx)
                ctx.packageManager.getLaunchIntentForPackage(v.pkg)?.let {
                    it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    ctx.startActivity(it)
                }
                if (o.sidebar) GameSidebarService.start(ctx, v.pkg, v.name)
                say(if (fails.isEmpty()) "Đã bật Game Turbo cho ${v.name}" else "Đã bật, ${fails.size} mục máy không hỗ trợ: ${fails.take(3).joinToString()}")
            } finally { _busy.value = false }
        }
    }

    fun stopBoost() {
        viewModelScope.launch {
            _busy.value = true
            try {
                val fails = boost.restore()
                _active.value = false
                GameSidebarService.stop(ctx)
                if (!_opts.value.autoStart) FreeFireWatcherService.stop(ctx)
                say(if (fails.isEmpty()) "Đã khôi phục cài đặt máy" else "Khôi phục xong, lỗi: ${fails.joinToString()}")
            } finally { _busy.value = false }
        }
    }

    private fun task(label: String, block: suspend () -> Result<String>) {
        viewModelScope.launch {
            _busy.value = true
            try { block().fold({ say(it) }, { say("$label: ${it.message}") }) } finally { _busy.value = false }
        }
    }

    fun compileShaders() { val v = variant() ?: return say("Chưa cài Free Fire"); task("Shader") { boost.compileShaders(v.pkg) } }
    fun clearCache() { val v = variant() ?: return say("Chưa cài Free Fire"); task("Cache") { boost.clearCache(v.pkg) } }
    fun cleanRam() = task("Dọn RAM") { boost.cleanRam() }
    fun screenshot() = task("Chụp màn hình") { boost.screenshot() }

    // ---- colour / resolution / brightness -------------------------------------------------------

    /** [apply] false while a slider is being dragged; true when the value is final. */
    fun setColor(c: ColorSettings, apply: Boolean) {
        _color.value = c
        if (!apply) { boost.saveColor(c); return }
        viewModelScope.launch { boost.applyColor(c).onFailure { say("Chỉnh màu: ${it.message}") } }
    }

    fun resetColor() {
        _color.value = ColorSettings()
        viewModelScope.launch { boost.resetColor().fold({ say(it) }, { say("Màu: ${it.message}") }) }
    }

    fun applyResolutionNow() {
        val v = variant() ?: return say("Chưa cài Free Fire")
        task("Độ phân giải") { boost.applyResolution(v.pkg, _opts.value.resLevel) }
    }

    fun brightness(): Int = boost.currentBrightness()

    fun setBrightness(v: Int) {
        viewModelScope.launch { if (!boost.setBrightness(v)) say("Máy không cho chỉnh độ sáng") }
    }
}
