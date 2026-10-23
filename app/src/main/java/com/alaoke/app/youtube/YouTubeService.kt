package com.alaoke.app.youtube

import com.alaoke.app.BuildConfig
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class YouTubeService {

    private val client = OkHttpClient.Builder()
        .addInterceptor(
            HttpLoggingInterceptor().apply {
                level = if (BuildConfig.DEBUG) {
                    HttpLoggingInterceptor.Level.BASIC
                } else {
                    HttpLoggingInterceptor.Level.NONE
                }
            }
        )
        .build()

    private val api: YouTubeApiService = Retrofit.Builder()
        .baseUrl("https://www.googleapis.com/youtube/v3/")
        .client(client)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(YouTubeApiService::class.java)

    suspend fun search(query: String): Result<List<YouTubeSearchResult>> {
        if (BuildConfig.YOUTUBE_API_KEY.isBlank() || BuildConfig.YOUTUBE_API_KEY == "PUT_YOUR_YOUTUBE_DATA_API_KEY_HERE") {
            return Result.failure(
                IllegalStateException(
                    "No YouTube Data API key configured. Add YOUTUBE_API_KEY to local.properties " +
                        "or app/build.gradle.kts before searching."
                )
            )
        }
        return try {
            val response = api.search(
                query = "$query karaoke",
                apiKey = BuildConfig.YOUTUBE_API_KEY
            )
            val results = response.items.mapNotNull { item ->
                val videoId = item.id?.videoId ?: return@mapNotNull null
                YouTubeSearchResult(
                    videoId = videoId,
                    title = item.snippet?.title.orEmpty(),
                    channelTitle = item.snippet?.channelTitle.orEmpty(),
                    thumbnailUrl = item.snippet?.thumbnails?.high?.url
                        ?: item.snippet?.thumbnails?.medium?.url
                )
            }
            Result.success(results)
        } catch (t: Throwable) {
            Result.failure(t)
        }
    }
}
