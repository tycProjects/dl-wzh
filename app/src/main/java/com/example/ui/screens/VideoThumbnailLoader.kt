package com.example.ui.screens

import android.content.ContentResolver
import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.os.CancellationSignal
import android.os.OperationCanceledException
import android.provider.MediaStore
import android.util.LruCache
import android.util.Size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import com.example.data.model.VideoItem
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.job
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext

/**
 * Lightweight, memory-safe video thumbnail loader for local MediaStore and content/file URIs.
 *
 * Avoids loading full video files into memory by using:
 * 1. An in-memory [LruCache] of downscaled thumbnail bitmaps,
 * 2. [ContentResolver.loadThumbnail] on Android 10+ (API 29+) with [CancellationSignal],
 * 3. [MediaStore.Video.Thumbnails] and [MediaMetadataRetriever.getScaledFrameAtTime] as fallbacks,
 * 4. Bounded background concurrency via [Semaphore] so fast scrolling cancels off-screen work cleanly.
 */
internal object VideoThumbnailLoader {
    private const val DEFAULT_THUMB_WIDTH_PX = 240
    private const val DEFAULT_THUMB_HEIGHT_PX = 180

    // Limit concurrent MediaMetadataRetriever / codec extractions to 2 so scrolling "More Videos"
    // never starves ExoPlayer's active hardware video decoder on budget chipsets (e.g. Snapdragon 680).
    private val extractionSemaphore = Semaphore(permits = 2)
    private const val MAX_FAILED_CACHE_ENTRIES = 256
    private val failedKeys = LinkedHashSet<String>()

    private val memoryCache: LruCache<String, Bitmap> = run {
        val maxHeapKb = (Runtime.getRuntime().maxMemory() / 1024L).toInt().coerceAtLeast(16 * 1024)
        val cacheSizeKb = (maxHeapKb / 16).coerceIn(4 * 1024, 16 * 1024)
        object : LruCache<String, Bitmap>(cacheSizeKb) {
            override fun sizeOf(key: String, value: Bitmap): Int {
                return (value.allocationByteCount / 1024).coerceAtLeast(1)
            }
        }
    }

    fun cacheKey(videoId: Long, contentUri: Uri): String = "${videoId}|${contentUri}"

    private fun isKnownFailure(key: String): Boolean = synchronized(failedKeys) {
        failedKeys.contains(key)
    }

    private fun markFailure(key: String) {
        synchronized(failedKeys) {
            if (failedKeys.size >= MAX_FAILED_CACHE_ENTRIES) {
                val first = failedKeys.iterator()
                if (first.hasNext()) {
                    first.next()
                    first.remove()
                }
            }
            failedKeys.add(key)
        }
    }

    fun getCached(videoId: Long, contentUri: Uri): Bitmap? {
        val cached = synchronized(memoryCache) {
            memoryCache.get(cacheKey(videoId, contentUri))
        }
        return cached?.takeIf { !it.isRecycled && it.width > 0 && it.height > 0 }
    }

    internal fun putCachedForTest(videoId: Long, contentUri: Uri, bitmap: Bitmap) {
        if (!bitmap.isRecycled && bitmap.width > 0 && bitmap.height > 0) {
            val key = cacheKey(videoId, contentUri)
            synchronized(failedKeys) {
                failedKeys.remove(key)
            }
            synchronized(memoryCache) {
                memoryCache.put(key, bitmap)
            }
        }
    }

    internal fun clearCache() {
        synchronized(failedKeys) {
            failedKeys.clear()
        }
        synchronized(memoryCache) {
            memoryCache.evictAll()
        }
    }

    suspend fun loadThumbnail(
        context: Context,
        video: VideoItem,
        widthPx: Int = DEFAULT_THUMB_WIDTH_PX,
        heightPx: Int = DEFAULT_THUMB_HEIGHT_PX
    ): Bitmap? {
        val key = cacheKey(video.id, video.contentUri)
        getCached(video.id, video.contentUri)?.let { return it }
        if (isKnownFailure(key)) return null

        return withContext(Dispatchers.IO) {
            getCached(video.id, video.contentUri)?.let { return@withContext it }
            if (isKnownFailure(key)) return@withContext null

            extractionSemaphore.withPermit {
                getCached(video.id, video.contentUri)?.let { return@withPermit it }
                if (isKnownFailure(key)) return@withPermit null

                val cancellationSignal = CancellationSignal()
                val completionHandle = coroutineContext.job.invokeOnCompletion { cause ->
                    if (cause is CancellationException) {
                        cancellationSignal.cancel()
                    }
                }

                try {
                    val extracted = extractVideoFrameThumbnail(
                        context = context.applicationContext,
                        video = video,
                        widthPx = widthPx,
                        heightPx = heightPx,
                        cancellationSignal = cancellationSignal
                    )
                    if (extracted == null) {
                        markFailure(key)
                        return@withPermit null
                    }

                    val bounded = downscaleIfNeeded(extracted, widthPx, heightPx)
                    if (bounded !== extracted && !extracted.isRecycled) {
                        extracted.recycle()
                    }
                    if (!bounded.isRecycled && bounded.width > 0 && bounded.height > 0) {
                        synchronized(memoryCache) {
                            memoryCache.put(key, bounded)
                        }
                        bounded
                    } else {
                        markFailure(key)
                        null
                    }
                } catch (cancelled: OperationCanceledException) {
                    throw CancellationException("Thumbnail load cancelled", cancelled)
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (_: SecurityException) {
                    markFailure(key)
                    null
                } catch (_: Exception) {
                    markFailure(key)
                    null
                } finally {
                    completionHandle.dispose()
                }
            }
        }
    }

    private fun extractVideoFrameThumbnail(
        context: Context,
        video: VideoItem,
        widthPx: Int,
        heightPx: Int,
        cancellationSignal: CancellationSignal
    ): Bitmap? {
        val uri = video.contentUri
        val resolver = context.contentResolver

        // 1. Android 10+ (API 29+) ContentResolver.loadThumbnail (fast MediaStore cached/indexed thumbnail)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && uri.scheme == ContentResolver.SCHEME_CONTENT) {
            try {
                val thumb = resolver.loadThumbnail(uri, Size(widthPx, heightPx), cancellationSignal)
                if (thumb.width > 0 && thumb.height > 0) {
                    return thumb
                }
            } catch (cancelled: OperationCanceledException) {
                throw cancelled
            } catch (_: Exception) {
                // Fall through to legacy MediaStore / MediaMetadataRetriever extraction
            }
        }

        if (cancellationSignal.isCanceled) {
            throw OperationCanceledException()
        }

        // 2. MediaStore.Video.Thumbnails fallback for content:// MediaStore IDs
        if (video.id > 0L && uri.scheme == ContentResolver.SCHEME_CONTENT) {
            try {
                @Suppress("DEPRECATION")
                val legacyThumb = MediaStore.Video.Thumbnails.getThumbnail(
                    resolver,
                    video.id,
                    MediaStore.Video.Thumbnails.MINI_KIND,
                    null
                )
                if (legacyThumb != null && legacyThumb.width > 0 && legacyThumb.height > 0) {
                    return legacyThumb
                }
            } catch (_: Exception) {
                // Fall through to MediaMetadataRetriever
            }
        }

        if (cancellationSignal.isCanceled) {
            throw OperationCanceledException()
        }

        // 3. Direct single-keyframe extraction via MediaMetadataRetriever (never loads full video into heap)
        val retriever = MediaMetadataRetriever()
        return try {
            when (uri.scheme) {
                ContentResolver.SCHEME_FILE -> {
                    val path = uri.path
                    if (path.isNullOrBlank()) return null
                    retriever.setDataSource(path)
                }
                else -> {
                    retriever.setDataSource(context, uri)
                }
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
                retriever.getScaledFrameAtTime(
                    1_000_000L,
                    MediaMetadataRetriever.OPTION_CLOSEST_SYNC,
                    widthPx,
                    heightPx
                ) ?: retriever.getScaledFrameAtTime(
                    -1L,
                    MediaMetadataRetriever.OPTION_CLOSEST_SYNC,
                    widthPx,
                    heightPx
                )
            } else {
                retriever.getFrameAtTime(1_000_000L, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                    ?: retriever.getFrameAtTime(-1L, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
            }
        } catch (_: Exception) {
            null
        } finally {
            try {
                retriever.release()
            } catch (_: Exception) {
            }
        }
    }

    private fun downscaleIfNeeded(source: Bitmap, maxWidth: Int, maxHeight: Int): Bitmap {
        val w = source.width
        val h = source.height
        if (w <= 0 || h <= 0) return source
        if (w <= maxWidth && h <= maxHeight) return source

        val scale = minOf(
            maxWidth.toFloat() / w.toFloat(),
            maxHeight.toFloat() / h.toFloat()
        ).coerceAtMost(1f)
        val targetW = (w * scale).toInt().coerceAtLeast(1)
        val targetH = (h * scale).toInt().coerceAtLeast(1)
        if (targetW >= w && targetH >= h) return source
        return try {
            Bitmap.createScaledBitmap(source, targetW, targetH, true)
        } catch (_: Exception) {
            source
        }
    }
}

@Composable
internal fun rememberVideoThumbnail(video: VideoItem): State<ImageBitmap?> {
    val context = LocalContext.current
    val initialCached = remember(video.id, video.contentUri) {
        VideoThumbnailLoader.getCached(video.id, video.contentUri)?.asImageBitmap()
    }
    return produceState(initialValue = initialCached, key1 = video.id, key2 = video.contentUri) {
        if (value == null) {
            val loaded = VideoThumbnailLoader.loadThumbnail(context, video)
            if (loaded != null && !loaded.isRecycled) {
                value = loaded.asImageBitmap()
            }
        }
    }
}
