package com.qrstudio.app.ui.create

import android.app.Application
import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.qrstudio.app.data.SettingsRepository
import com.qrstudio.app.qr.FaviconFetcher
import com.qrstudio.app.qr.QrContentBuilders
import com.qrstudio.app.qr.QrRenderer
import com.qrstudio.app.qr.QrStyleConfig
import com.qrstudio.app.qr.QrThemePreset
import com.qrstudio.app.qr.fromPreset
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CreateViewModel(
    private val settings: SettingsRepository,
) : ViewModel() {

    private val _contentType = MutableStateFlow(ContentType.URL)
    val contentType: StateFlow<ContentType> = _contentType.asStateFlow()

    private val _fields = MutableStateFlow<Map<String, String>>(emptyMap())
    val fields: StateFlow<Map<String, String>> = _fields.asStateFlow()

    private val _style = MutableStateFlow(
        QrStyleConfig(
            errorCorrection = settings.errorCorrection,
            exportSize = settings.defaultSize,
        )
    )
    val style: StateFlow<QrStyleConfig> = _style.asStateFlow()

    private val _payload = MutableStateFlow("")
    val payload: StateFlow<String> = _payload.asStateFlow()

    private val _bitmap = MutableStateFlow<Bitmap?>(null)
    val bitmap: StateFlow<Bitmap?> = _bitmap.asStateFlow()

    private val _logo = MutableStateFlow<Bitmap?>(null)
    val logo: StateFlow<Bitmap?> = _logo.asStateFlow()

    private val _faviconLoading = MutableStateFlow(false)
    val faviconLoading: StateFlow<Boolean> = _faviconLoading.asStateFlow()

    private val _events = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val events = _events.asSharedFlow()

    private var renderJob: Job? = null

    init {
        selectType(ContentType.URL)
    }

    fun notify(message: String) {
        _events.tryEmit(message)
    }

    fun selectType(type: ContentType) {
        _contentType.value = type
        _fields.value = ContentForms.fieldsFor(type).associate { it.key to it.defaultValue }
        recompile()
    }

    fun updateField(key: String, value: String) {
        _fields.value = _fields.value + (key to value)
        recompile()
    }

    fun updateStyle(transform: (QrStyleConfig) -> QrStyleConfig) {
        _style.value = transform(_style.value)
        render()
    }

    fun applyTheme(preset: QrThemePreset) {
        _style.value = _style.value.fromPreset(preset)
        render()
    }

    fun resetStyle() {
        _logo.value = null
        _style.value = QrStyleConfig(
            errorCorrection = settings.errorCorrection,
            exportSize = settings.defaultSize,
        )
        render()
    }

    fun applyWhiteTheme() {
        _style.value = _style.value.copy(
            primaryColor = 0xFF000000,
            secondaryColor = 0xFF000000,
            gradient = false,
            backgroundColor = 0xFFFFFFFF,
            transparentBg = false,
            captionColor = 0xFF000000,
        )
        render()
    }

    fun applyBlackTheme() {
        _style.value = _style.value.copy(
            primaryColor = 0xFFFFFFFF,
            secondaryColor = 0xFFFFFFFF,
            gradient = false,
            backgroundColor = 0xFF0b0f14,
            transparentBg = false,
            captionColor = 0xFFFFFFFF,
        )
        render()
    }

    fun setLogo(bitmap: Bitmap?) {
        _logo.value = bitmap
        if (bitmap != null) {
            _style.value = _style.value.copy(errorCorrection = "H")
            _events.tryEmit("تمت إضافة الشعار ورفع تصحيح الخطأ إلى 30% تلقائياً")
        }
        render()
    }

    fun fetchFavicon() {
        val url = _fields.value["url"].orEmpty()
        if (url.isBlank()) {
            _events.tryEmit("أدخل الرابط أولاً لجلب شعار الموقع")
            return
        }
        viewModelScope.launch {
            _faviconLoading.value = true
            val bmp = withContext(Dispatchers.IO) { FaviconFetcher.fetch(url) }
            _faviconLoading.value = false
            if (bmp != null) {
                setLogo(bmp)
            } else {
                _events.tryEmit("تعذر جلب الشعار — هذه الميزة تحتاج اتصالاً بالإنترنت")
            }
        }
    }

    fun loadFromText(text: String) {
        selectType(ContentType.TEXT)
        updateField("text", text)
    }

    private fun recompile() {
        _payload.value = QrContentBuilders.build(_contentType.value.name, _fields.value)
        render()
    }

    private fun render() {
        val content = _payload.value
        val styleSnapshot = _style.value
        val logoSnapshot = _logo.value
        renderJob?.cancel()
        renderJob = viewModelScope.launch(Dispatchers.Default) {
            _bitmap.value = if (content.isBlank()) {
                null
            } else {
                QrRenderer.render(content, styleSnapshot, logoSnapshot)
            }
        }
    }

    override fun onCleared() {
        renderJob?.cancel()
        super.onCleared()
    }
}
