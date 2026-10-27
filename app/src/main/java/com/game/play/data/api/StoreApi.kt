package com.game.play.data.api

import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import java.io.File

class ApiResponse(val code: Int, val body: String) {
    val ok: Boolean get() = code in 200..299
}

/** Klien HTTP sederhana (blocking) — panggil dari Dispatchers.IO. */
class StoreApi(private val http: OkHttpClient = ApiClient.http) {

    fun get(path: String, params: Map<String, String?> = emptyMap()): ApiResponse {
        val builder = (ApiClient.BASE_URL + path).toHttpUrl().newBuilder()
        for ((key, value) in params) {
            if (!value.isNullOrBlank()) builder.addQueryParameter(key, value)
        }
        val request = Request.Builder().url(builder.build()).header("Accept", "application/json").build()
        return execute(request)
    }

    fun post(path: String, json: String): ApiResponse {
        val body = json.toRequestBody("application/json; charset=utf-8".toMediaType())
        val request = Request.Builder()
            .url(ApiClient.BASE_URL + path)
            .header("Accept", "application/json")
            .post(body)
            .build()
        return execute(request)
    }

    fun put(url: String, file: File, contentType: String, headers: Map<String, String> = emptyMap()): ApiResponse {
        val builder = Request.Builder().url(url).put(file.asRequestBody(contentType.toMediaType()))
        for ((key, value) in headers) builder.header(key, value)
        return execute(builder.build())
    }


    fun postFile(path: String, file: File, filename: String, contentType: String): ApiResponse {
        val url = (ApiClient.BASE_URL + path).toHttpUrl().newBuilder()
            .addQueryParameter("filename", filename)
            .addQueryParameter("contentType", contentType)
            .build()
        val request = Request.Builder()
            .url(url)
            .header("Accept", "application/json")
            .header("Content-Type", contentType)
            .post(file.asRequestBody(contentType.toMediaType()))
            .build()
        return execute(request)
    }

    private fun execute(request: Request): ApiResponse =
        http.newCall(request).execute().use { response ->
            ApiResponse(response.code, response.body?.string().orEmpty())
        }
}
