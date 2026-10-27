package com.game.play.data.api

import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

object ApiClient {
    const val BASE_URL = "https://por-gamecp.vercel.app/"

    val http: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(120, TimeUnit.SECONDS)
            .build()
    }
}
