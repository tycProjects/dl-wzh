package ai.casper.app.ui

import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap

// Декодирует картинку из data-URL (data:image/...;base64,...) для показа в чате.
fun decodeImage(dataUrl: String): ImageBitmap? = runCatching {
    val bytes = Base64.decode(dataUrl.substringAfter("base64,", ""), Base64.DEFAULT)
    BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
}.getOrNull()
