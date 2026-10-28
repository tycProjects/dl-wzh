package com.schedule.app.ui

import android.content.Intent
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext

/**
 * إملاء صوتي عبر خدمة التعرف على الكلام في النظام (غالبا من جوجل؛ تحتاج اتصالا أو حزمة عربية محمّلة).
 * يرجع دالة تُستدعى لبدء الاستماع.
 */
@Composable
fun rememberVoiceInput(onText: (String) -> Unit): () -> Unit {
    val ctx = LocalContext.current
    val cb = rememberUpdatedState(onText)
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { r ->
        r.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()?.let { cb.value(it) }
    }
    return {
        val i = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
            .putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            .putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ar-EG")
            .putExtra(RecognizerIntent.EXTRA_PROMPT, "تكلّم الآن")
        runCatching { launcher.launch(i) }.onFailure { ctx.toast("التعرف على الصوت غير متاح على هذا الجهاز") }
    }
}
