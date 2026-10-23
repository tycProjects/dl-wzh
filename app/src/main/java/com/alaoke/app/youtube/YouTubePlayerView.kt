package com.alaoke.app.youtube

import android.annotation.SuppressLint
import android.view.ViewGroup
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView

/**
 * Handle used by callers to control an embedded YouTube IFrame player and to
 * observe simple playback state (used to auto-advance the Party Queue).
 */
class YouTubePlayerController {
    internal var webView: WebView? = null
    var onVideoEnded: (() -> Unit)? = null
    var onPlayerReady: (() -> Unit)? = null

    fun loadVideo(videoId: String) {
        webView?.evaluateJavascript("loadAndPlay('$videoId')", null)
    }

    fun play() { webView?.evaluateJavascript("player && player.playVideo();", null) }
    fun pause() { webView?.evaluateJavascript("player && player.pauseVideo();", null) }
    fun setVolume(percent: Int) { webView?.evaluateJavascript("player && player.setVolume($percent);", null) }
}

private const val PLAYER_HTML = """
<!DOCTYPE html>
<html>
<head>
  <meta name="viewport" content="width=device-width, initial-scale=1, viewport-fit=cover">
  <style>
    html, body { margin:0; padding:0; background:#000; height:100%; width:100%; overflow:hidden; }
    #player { position:absolute; top:0; left:0; width:100%; height:100%; }
  </style>
</head>
<body>
  <div id="player"></div>
  <script src="https://www.youtube.com/iframe_api"></script>
  <script>
    var player;
    var pendingVideoId = null;
    function onYouTubeIframeAPIReady() {
      player = new YT.Player('player', {
        height: '100%',
        width: '100%',
        playerVars: { 'playsinline': 1, 'controls': 0, 'rel': 0, 'modestbranding': 1 },
        events: {
          'onReady': function(e) {
            AndroidBridge.onReady();
            if (pendingVideoId) { player.loadVideoById(pendingVideoId); pendingVideoId = null; }
          },
          'onStateChange': function(e) {
            if (e.data === YT.PlayerState.ENDED) { AndroidBridge.onEnded(); }
          }
        }
      });
    }
    function loadAndPlay(videoId) {
      if (player && player.loadVideoById) { player.loadVideoById(videoId); }
      else { pendingVideoId = videoId; }
    }
  </script>
</body>
</html>
"""

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun YouTubePlayerView(
    controller: YouTubePlayerController,
    initialVideoId: String?,
    modifier: Modifier = Modifier
) {
    AndroidView(
        modifier = modifier.fillMaxSize(),
        factory = { context ->
            WebView(context).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                settings.javaScriptEnabled = true
                settings.mediaPlaybackRequiresUserGesture = false
                settings.cacheMode = WebSettings.LOAD_DEFAULT
                webChromeClient = WebChromeClient()
                addJavascriptInterface(
                    object {
                        @JavascriptInterface
                        fun onEnded() { controller.onVideoEnded?.invoke() }

                        @JavascriptInterface
                        fun onReady() { controller.onPlayerReady?.invoke() }
                    },
                    "AndroidBridge"
                )
                loadDataWithBaseURL(
                    "https://www.youtube.com",
                    PLAYER_HTML,
                    "text/html",
                    "utf-8",
                    null
                )
                controller.webView = this
            }
        }
    )

    LaunchedEffect(initialVideoId) {
        if (initialVideoId != null) {
            controller.loadVideo(initialVideoId)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            controller.webView?.destroy()
            controller.webView = null
        }
    }
}
