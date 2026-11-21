package com.tyce.enhancer

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID

/**
 * Talks to a GPU server the user rents themselves (RunPod, Vast.ai, Lambda, a VPS with a
 * card, or their own box) and runs the companion script in /server. No SDK, no vendor
 * lock-in: this is one multipart POST and a response with the upscaled image bytes.
 *
 * Request:  POST {url}            multipart/form-data
 *             image  = the photo, JPEG
 *             scale  = "2" or "4"
 *           header X-API-Key: <key>   (only if a key is set)
 * Response: 200, body = the upscaled image (jpeg or png), OR
 *           non-200, body = plain-text error message to show the user.
 */
object CloudEnhancer {

    class CloudError(message: String) : IOException(message)

    /** Uploads [src] and returns the upscaled bitmap. Runs network I/O; call from Dispatchers.IO. */
    fun enhance(
        url: String,
        apiKey: String,
        scale: Int,
        src: Bitmap,
        connectTimeoutMs: Int = 15_000,
        readTimeoutMs: Int = 180_000,
    ): Bitmap {
        val boundary = "TyceEnhancer-${UUID.randomUUID()}"
        val jpeg = ByteArrayOutputStream().also {
            src.compress(Bitmap.CompressFormat.JPEG, 95, it)
        }.toByteArray()

        val conn = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            doOutput = true
            connectTimeout = connectTimeoutMs
            readTimeout = readTimeoutMs
            setRequestProperty("Content-Type", "multipart/form-data; boundary=$boundary")
            if (apiKey.isNotBlank()) setRequestProperty("X-API-Key", apiKey)
        }

        try {
            conn.outputStream.use { out ->
                writeFormField(out, boundary, "scale", scale.toString())
                writeFormFile(out, boundary, "image", "photo.jpg", "image/jpeg", jpeg)
                writeFinalBoundary(out, boundary)
            }

            val code = conn.responseCode
            if (code !in 200..299) {
                val msg = try {
                    conn.errorStream?.bufferedReader()?.use { it.readText() }
                } catch (_: Throwable) { null }
                throw CloudError(
                    if (!msg.isNullOrBlank()) msg.take(300)
                    else "Server returned HTTP $code"
                )
            }

            val bytes = conn.inputStream.use { it.readBytes() }
            return BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                ?: throw CloudError("Server response was not a valid image")
        } catch (t: java.net.SocketTimeoutException) {
            throw CloudError("Timed out waiting for the server. It may still be loading the model — try again in a bit.")
        } catch (t: java.net.ConnectException) {
            throw CloudError("Could not reach the server. Check the URL and that it's running.")
        } finally {
            conn.disconnect()
        }
    }

    /** Small no-image ping so the Model sheet can show "Connected" / "Failed". */
    fun test(url: String, apiKey: String, timeoutMs: Int = 10_000): String? {
        return try {
            val probe = Bitmap.createBitmap(8, 8, Bitmap.Config.ARGB_8888)
            enhance(url, apiKey, scale = 2, src = probe, connectTimeoutMs = timeoutMs, readTimeoutMs = timeoutMs)
            null
        } catch (t: CloudError) {
            t.message ?: "Connection failed"
        } catch (t: Throwable) {
            t.message ?: "Connection failed"
        }
    }

    private fun writeFormField(out: OutputStream, boundary: String, name: String, value: String) {
        out.write("--$boundary\r\n".toByteArray())
        out.write("Content-Disposition: form-data; name=\"$name\"\r\n\r\n".toByteArray())
        out.write(value.toByteArray())
        out.write("\r\n".toByteArray())
    }

    private fun writeFormFile(
        out: OutputStream, boundary: String, name: String, filename: String, contentType: String, data: ByteArray
    ) {
        out.write("--$boundary\r\n".toByteArray())
        out.write(
            "Content-Disposition: form-data; name=\"$name\"; filename=\"$filename\"\r\n".toByteArray()
        )
        out.write("Content-Type: $contentType\r\n\r\n".toByteArray())
        out.write(data)
        out.write("\r\n".toByteArray())
    }

    private fun writeFinalBoundary(out: OutputStream, boundary: String) {
        out.write("--$boundary--\r\n".toByteArray())
    }

    // ------- settings, stashed in the same "enhancer" prefs file -------

    fun isEnabled(context: Context) =
        context.getSharedPreferences("enhancer", Context.MODE_PRIVATE).getBoolean("cloud", false)

    fun url(context: Context): String =
        context.getSharedPreferences("enhancer", Context.MODE_PRIVATE).getString("cloud_url", "") ?: ""

    fun apiKey(context: Context): String =
        context.getSharedPreferences("enhancer", Context.MODE_PRIVATE).getString("cloud_key", "") ?: ""

    fun isConfigured(context: Context) = isEnabled(context) && url(context).isNotBlank()
}
