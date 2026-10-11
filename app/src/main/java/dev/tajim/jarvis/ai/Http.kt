package dev.tajim.jarvis.ai

import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URL
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AiException(val kind: Kind, message: String) : Exception(message) {
    enum class Kind { NOT_CONFIGURED, AUTH, RATE_LIMIT, NETWORK, TIMEOUT, SERVER, BAD_RESPONSE }
}

/** Minimal HTTPS client on HttpURLConnection (no extra dependency). Returns (status code, body). */
object Http {
    suspend fun request(
        url: String,
        method: String = "GET",
        headers: Map<String, String> = emptyMap(),
        body: String? = null,
        readTimeoutMs: Int = 45_000,
    ): Pair<Int, String> = withContext(Dispatchers.IO) {
        val conn = URL(url).openConnection() as HttpURLConnection
        try {
            conn.requestMethod = method
            conn.connectTimeout = 15_000
            conn.readTimeout = readTimeoutMs
            headers.forEach { (k, v) -> conn.setRequestProperty(k, v) }
            if (body != null) {
                conn.doOutput = true
                conn.outputStream.use { it.write(body.toByteArray()) }
            }
            val code = conn.responseCode
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
            code to (stream?.bufferedReader()?.use { it.readText() }.orEmpty())
        } catch (e: SocketTimeoutException) {
            throw AiException(AiException.Kind.TIMEOUT, "The request timed out.")
        } catch (e: CancellationException) {
            throw e
        } catch (e: java.io.IOException) {
            throw AiException(AiException.Kind.NETWORK, "Network error. Check your internet connection.")
        } finally {
            conn.disconnect()
        }
    }
}

/** Runs [block] and wraps failures in Result, but never swallows coroutine cancellation. */
suspend fun <T> safely(block: suspend () -> T): Result<T> =
    try { Result.success(block()) } catch (e: CancellationException) { throw e } catch (e: Throwable) { Result.failure(e) }
