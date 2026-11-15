package com.zewmod.engine.bridge

import android.util.Log
import com.zewmod.engine.model.AppConfig
import com.zewmod.engine.model.ServerInfo
import com.zewmod.engine.model.ServerStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

/**
 * RelayBridge: kết nối đến server relay để xác thực và lấy config.
 */
object RelayBridge {

    private const val TAG = "RelayBridge"
    private const val VERIFY_URL = "http://127.0.0.1:8787/verify"
    private const val TIMEOUT_S = 5L

    private val client by lazy {
        OkHttpClient.Builder()
            .connectTimeout(TIMEOUT_S, TimeUnit.SECONDS)
            .readTimeout(TIMEOUT_S, TimeUnit.SECONDS)
            .build()
    }

    /** Kiểm tra token hợp lệ với server */
    suspend fun verify(token: String): Boolean = withContext(Dispatchers.IO) {
        if (token == "local") return@withContext true
        runCatching {
            val request = Request.Builder()
                .url(VERIFY_URL)
                .addHeader("Authorization", "Bearer $token")
                .build()
            client.newCall(request).execute().use { resp ->
                resp.isSuccessful
            }
        }.getOrElse {
            Log.w(TAG, "Verify failed: ${it.message}")
            false
        }
    }

    /** Ping một server và trả về ms, -1 nếu offline */
    suspend fun pingServer(server: ServerInfo): Int = withContext(Dispatchers.IO) {
        runCatching {
            val start = System.currentTimeMillis()
            val process = Runtime.getRuntime().exec("ping -c 1 -W 2 ${server.ip}")
            process.waitFor()
            val elapsed = (System.currentTimeMillis() - start).toInt()
            if (process.exitValue() == 0) elapsed else -1
        }.getOrElse { -1 }
    }

    /** Ping tất cả server và cập nhật trạng thái */
    suspend fun pingAll(servers: List<ServerInfo>): List<ServerInfo> =
        withContext(Dispatchers.IO) {
            servers.map { server ->
                val ms = pingServer(server)
                server.copy(
                    ping = ms,
                    status = if (ms >= 0) ServerStatus.ONLINE else ServerStatus.OFFLINE
                )
            }
        }

    /** Lấy config từ server */
    suspend fun fetchConfig(server: ServerInfo, token: String): AppConfig? =
        withContext(Dispatchers.IO) {
            if (server.ip == "127.0.0.1") return@withContext null
            runCatching {
                val url = "http://${server.ip}:${server.port}/config"
                val request = Request.Builder()
                    .url(url)
                    .addHeader("Authorization", "Bearer $token")
                    .addHeader("X-ZewMod-Key", server.key)
                    .build()
                client.newCall(request).execute().use { resp ->
                    if (resp.isSuccessful) {
                        // parse config — in production, decode JSON
                        null
                    } else null
                }
            }.getOrElse {
                Log.w(TAG, "fetchConfig failed: ${it.message}")
                null
            }
        }

    /** Danh sách server mặc định (local) */
    fun defaultServers(): List<ServerInfo> = listOf(
        ServerInfo(
            name = "LOCAL",
            ip = "127.0.0.1",
            port = 38571,
            key = "local",
            features = "track,head,legit,freeze,scope",
            status = ServerStatus.ONLINE,
            ping = 1,
            type = 0
        )
    )
}
