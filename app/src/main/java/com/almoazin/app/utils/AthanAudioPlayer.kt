package com.almoazin.app.utils

import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.almoazin.app.data.AzanReciter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class AthanAudioPlayer(private val context: Context) {

    private var exoPlayer: ExoPlayer? = null
    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying

    private val _currentTrackName = MutableStateFlow("")
    val currentTrackName: StateFlow<String> = _currentTrackName

    companion object {
        val RECITERS = listOf(
            AzanReciter(
                id = "makkah_mulla",
                nameAr = "الشيخ علي ملا (أذان الحرم المكي)",
                locationAr = "المسجد الحرام، مكة المكرمة",
                audioUrls = listOf(
                    "https://ia800201.us.archive.org/21/items/AdhanMakkah/Adhan_Makkah.mp3",
                    "https://download.quranicaudio.com/athan/makkah.mp3"
                ),
                durationSeconds = 215
            ),
            AzanReciter(
                id = "madinah_surayhi",
                nameAr = "الشيخ عبد المجيد السريحي (أذان الحرم النبوي)",
                locationAr = "المسجد النبوي الشريف",
                audioUrls = listOf(
                    "https://ia800302.us.archive.org/30/items/AdhanAlMadinah/Adhan_Madinah.mp3",
                    "https://download.quranicaudio.com/athan/madinah.mp3"
                ),
                durationSeconds = 230
            ),
            AzanReciter(
                id = "mishary_alafasy",
                nameAr = "الشيخ مشاري راشد العفاسي",
                locationAr = "الكويت والعالم الإسلامي",
                audioUrls = listOf(
                    "https://ia800504.us.archive.org/33/items/AthanMisharyRashidAlafasy/AthanMishary.mp3"
                ),
                durationSeconds = 220
            ),
            AzanReciter(
                id = "abdulbasit",
                nameAr = "الشيخ عبد الباسط عبد الصمد",
                locationAr = "مصر - تسجيل تاريخي نقي",
                audioUrls = listOf(
                    "https://ia800305.us.archive.org/15/items/AdhanAbdulBasit/Adhan_AbdulBasit.mp3"
                ),
                durationSeconds = 210
            ),
            AzanReciter(
                id = "alaqsa",
                nameAr = "أذان المسجد الأقصى المبارك",
                locationAr = "القدس الشريف، فلسطين",
                audioUrls = listOf(
                    "https://ia800301.us.archive.org/20/items/AdhanAl-aqsa/Adhan_Al-Aqsa.mp3"
                ),
                durationSeconds = 205
            )
        )
    }

    init {
        initPlayer()
    }

    private fun initPlayer() {
        exoPlayer = ExoPlayer.Builder(context).build().apply {
            addListener(object : Player.Listener {
                override fun onIsPlayingChanged(playing: Boolean) {
                    _isPlaying.value = playing
                }

                override fun onPlaybackStateChanged(playbackState: Int) {
                    if (playbackState == Player.STATE_ENDED) {
                        _isPlaying.value = false
                    }
                }

                override fun onPlayerError(error: PlaybackException) {
                    _isPlaying.value = false
                }
            })
        }
    }

    fun playAthan(reciter: AzanReciter) {
        val url = reciter.audioUrls.firstOrNull() ?: return
        _currentTrackName.value = reciter.nameAr
        exoPlayer?.apply {
            stop()
            clearMediaItems()
            setMediaItem(MediaItem.fromUri(url))
            prepare()
            play()
        }
    }

    fun playAudioUrl(url: String, title: String) {
        _currentTrackName.value = title
        exoPlayer?.apply {
            stop()
            clearMediaItems()
            setMediaItem(MediaItem.fromUri(url))
            prepare()
            play()
        }
    }

    fun stop() {
        exoPlayer?.stop()
        _isPlaying.value = false
    }

    fun release() {
        exoPlayer?.release()
        exoPlayer = null
    }
}
