package com.alaoke.app.youtube

import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Thin wrapper around the officially supported YouTube Data API v3 `search.list` endpoint.
 * We only ever fetch metadata (title, channel, thumbnail, video id) to show search results
 * and to hand a video id to the YouTube IFrame Player for playback. No video/audio content
 * is ever downloaded or extracted by this app.
 */
interface YouTubeApiService {
    @GET("search")
    suspend fun search(
        @Query("part") part: String = "snippet",
        @Query("q") query: String,
        @Query("type") type: String = "video",
        @Query("videoEmbeddable") videoEmbeddable: String = "true",
        @Query("maxResults") maxResults: Int = 25,
        @Query("key") apiKey: String
    ): YouTubeSearchResponse
}

data class YouTubeSearchResponse(
    val items: List<YouTubeSearchItem> = emptyList()
)

data class YouTubeSearchItem(
    val id: YouTubeVideoId?,
    val snippet: YouTubeSnippet?
)

data class YouTubeVideoId(
    val videoId: String?
)

data class YouTubeSnippet(
    val title: String?,
    val channelTitle: String?,
    val thumbnails: YouTubeThumbnails?
)

data class YouTubeThumbnails(
    val medium: YouTubeThumbnail?,
    val high: YouTubeThumbnail?
)

data class YouTubeThumbnail(
    val url: String?
)

/** Simplified, UI-friendly representation of a search hit. */
data class YouTubeSearchResult(
    val videoId: String,
    val title: String,
    val channelTitle: String,
    val thumbnailUrl: String?
)
