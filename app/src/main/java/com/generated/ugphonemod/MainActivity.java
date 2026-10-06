package com.generated.ugphonemod;

import android.Manifest;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.app.Dialog;
import android.app.DownloadManager;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.media.MediaScannerConnection;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.net.Uri;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.SystemClock;
import android.provider.Settings;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.webkit.CookieManager;
import android.webkit.GeolocationPermissions;
import android.webkit.PermissionRequest;
import android.webkit.URLUtil;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.webkit.WebSettings;
import androidx.webkit.WebViewCompat;
import androidx.webkit.WebViewFeature;
import java.io.ByteArrayInputStream;
import java.io.File;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.media.MediaPlayer;
import android.view.TextureView;
import android.graphics.SurfaceTexture;
import android.view.Surface;
import android.graphics.Matrix;
import android.os.Vibrator;
import android.os.VibrationEffect;
import android.webkit.JavascriptInterface;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import androidx.core.content.FileProvider;
import androidx.core.view.WindowCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import androidx.core.app.NotificationCompat;

// Shows a themed loading screen (matching the app's dark background) the
// moment the app opens, and swaps it out for the WebView content only once
// the page has actually finished loading -- so opening the app never shows
// a blank white flash while the WebView engine spins up.
//
// Also wires up onShowFileChooser: a plain WebView does NOT respond to
// <input type="file"> clicks out of the box -- without this override,
// tapping a file-upload control silently does nothing, which is the most
// common "the button doesn't work" complaint for wrapped web apps that
// let the user pick a file.
//
// And a DownloadListener: a plain WebView also does NOT know what to do
// with a link to a downloadable file (an APK, a zip, etc) -- without this,
// tapping a "Download" link just fails to navigate anywhere and the app
// appears to do nothing / falls back to showing the page underneath it.
// Downloads are handed off to Android's own DownloadManager (see
// startDownload below), which shows a real system notification for
// progress and completion and makes the file findable afterward. If what
// finished downloading is itself a .apk, this also offers the system
// installer straight away (see requestInstall/launchInstall) instead of
// making the user go find the file themselves -- every other kind of
// download still just lands in the Downloads folder, same as before.
public class MainActivity extends AppCompatActivity {

    private static final int FILE_CHOOSER_REQUEST_CODE = 51426;
    private static final int STORAGE_PERMISSION_REQUEST_CODE = 51427;
    // Covers both getUserMedia() resources a page can ask for -- camera and
    // mic -- since a page can (and video-chat widgets often do) ask for
    // both in the same PermissionRequest; see onPermissionRequest below.
    private static final int WEB_MEDIA_PERMISSION_REQUEST_CODE = 51428;
    private static final int NOTIFICATION_PERMISSION_REQUEST_CODE = 51429;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 51430;
    // Handled the same way STORAGE_PERMISSION_REQUEST_CODE/pendingDownload
    // is above: Android 8+ requires the user to explicitly allow this app
    // to install packages before ACTION_VIEW on an APK does anything, so if
    // that's not granted yet this is parked here and resumed from
    // onActivityResult once the user comes back from that settings screen.
    private static final int INSTALL_PERMISSION_REQUEST_CODE = 51431;
    private Uri pendingInstallUri;
    // Synthetic https origin standing in for file:///android_asset/ now that
    // the wrapped site's files are compiled into EmbeddedAssets rather than
    // sitting in a real assets/ folder -- shouldInterceptRequest below
    // resolves anything under this origin by decoding the matching
    // EmbeddedAssets entry instead of the OS resolving it against disk.
    private static final String EMBED_HOST = UrlObfuscator.decode(new int[] { 71, 58, 25, 252, 216, 240, 198, 39, 66, 43, 7, 225, 199, 166, 132, 100, 49, 82, 50, 31, 250, 214, 246 }, 47);
    // Branded "page didn't load" screen -- see ERROR_PAGE_HTML in server.js.
    private static final String ERROR_PAGE_HTML = "<!DOCTYPE html>\n<html lang=\"en\">\n<head>\n<meta charset=\"UTF-8\">\n<meta name=\"viewport\" content=\"width=device-width, initial-scale=1, viewport-fit=cover, user-scalable=no\">\n<meta name=\"color-scheme\" content=\"dark\">\n<meta name=\"theme-color\" content=\"#0a0a0b\">\n<title>Well, that's awkward</title>\n<style>\n  :root {\n    --bg: #0a0a0b;\n    --surface: #141416;\n    --surface-2: #1c1c1f;\n    --line: #2a2a2e;\n    --text: #f4f4f5;\n    --muted: #8d8d94;\n    --accent: #ffffff;\n    box-sizing: border-box;\n  }\n  *, *::before, *::after { box-sizing: inherit; }\n  html, body {\n    height: 100%; margin: 0; background: var(--bg); color: var(--text);\n    overscroll-behavior: none; overflow: hidden;\n  }\n  body {\n    font-family: -apple-system, BlinkMacSystemFont, \"SF Pro Text\", \"Helvetica Neue\", system-ui, Roboto, sans-serif;\n    -webkit-font-smoothing: antialiased;\n    -webkit-tap-highlight-color: transparent;\n    -webkit-user-select: none; user-select: none;\n  }\n\n  .screen {\n    height: 100%;\n    display: flex; flex-direction: column;\n    padding-top: env(safe-area-inset-top, 0px);\n    padding-bottom: env(safe-area-inset-bottom, 0px);\n  }\n\n  /* top app bar */\n  .appbar {\n    height: 56px; flex: none;\n    display: flex; align-items: center; gap: 4px;\n    padding: 0 8px;\n  }\n  .iconbtn {\n    width: 44px; height: 44px; border: 0; border-radius: 22px;\n    background: transparent; color: var(--text);\n    display: grid; place-items: center; cursor: pointer;\n    transition: background .15s;\n  }\n  .iconbtn:active { background: var(--surface-2); }\n  .iconbtn svg { width: 24px; height: 24px; }\n  .appbar .title { font-size: 17px; font-weight: 600; letter-spacing: -.01em; }\n\n  /* body */\n  .content {\n    flex: 1; min-height: 0;\n    display: flex; flex-direction: column; align-items: center; justify-content: center;\n    padding: 0 32px 24px; text-align: center;\n  }\n  .tile {\n    position: relative;\n    width: 112px; height: 112px; border-radius: 32px;\n    background: var(--surface); border: 1px solid var(--line);\n    display: grid; place-items: center;\n    margin-bottom: 28px;\n  }\n  .tile svg { width: 56px; height: 56px; overflow: visible; }\n  .tile .ring {\n    position: absolute; inset: -1px; border-radius: 32px;\n    border: 1px solid var(--accent); opacity: 0;\n    animation: ping 2.4s ease-out 0.4s 2;\n  }\n  @keyframes ping {\n    0% { transform: scale(1); opacity: .5; }\n    100% { transform: scale(1.35); opacity: 0; }\n  }\n  .glyph-line { stroke: var(--text); stroke-width: 2.5; stroke-linecap: round; fill: none; }\n  .glyph-dim { stroke: #4a4a50; }\n  .glyph-dot { fill: var(--accent); }\n\n  h1 { font-family: -apple-system, BlinkMacSystemFont, \"SF Pro Display\", \"Helvetica Neue\", system-ui, Roboto, sans-serif; font-size: 24px; line-height: 1.2; font-weight: 650; letter-spacing: -.02em; margin: 0 0 10px; }\n  .content p { font-size: 15px; line-height: 1.5; color: var(--muted); margin: 0; max-width: 30ch; }\n\n  /* bottom action area */\n  .actions { flex: none; padding: 0 20px 16px; display: grid; gap: 8px; }\n  .btn {\n    height: 52px; border: 0; border-radius: 20px;\n    font: inherit; font-size: 16px; font-weight: 600; cursor: pointer;\n    transition: transform .12s ease, opacity .2s, background .15s;\n  }\n  .btn:active { transform: scale(.97); }\n  .btn:focus-visible, .iconbtn:focus-visible { outline: 2px solid var(--accent); outline-offset: 2px; }\n  .btn.primary { background: var(--accent); color: #0a0a0b; }\n  .btn.ghost { background: transparent; color: var(--muted); height: 44px; font-weight: 500; font-size: 15px; }\n  .btn.ghost:active { color: var(--text); }\n  .btn[disabled] { opacity: .6; pointer-events: none; }\n\n  /* details bottom sheet */\n  .scrim {\n    position: fixed; inset: 0; background: rgba(0,0,0,.6);\n    opacity: 0; pointer-events: none; transition: opacity .25s;\n  }\n  .sheet {\n    position: fixed; left: 0; right: 0; bottom: 0;\n    background: var(--surface); border-top: 1px solid var(--line);\n    border-radius: 24px 24px 0 0;\n    padding: 10px 20px calc(20px + env(safe-area-inset-bottom, 0px));\n    transform: translateY(100%);\n    transition: transform .32s cubic-bezier(.2,.9,.25,1);\n  }\n  .open .scrim { opacity: 1; pointer-events: auto; }\n  .open .sheet { transform: none; }\n  .grab { width: 36px; height: 4px; border-radius: 2px; background: #3a3a40; margin: 0 auto 18px; }\n  .sheet h2 { font-size: 17px; font-weight: 600; margin: 0 0 14px; letter-spacing: -.01em; }\n  .row {\n    display: flex; justify-content: space-between; align-items: baseline; gap: 16px;\n    padding: 13px 14px; background: var(--surface-2); font-size: 14px; color: var(--muted);\n  }\n  .row:first-of-type { border-radius: 12px 12px 0 0; }\n  .row:last-of-type { border-radius: 0 0 12px 12px; }\n  .row + .row { border-top: 1px solid var(--line); }\n  .row b { font: 500 13px \"SF Mono\", ui-monospace, SFMono-Regular, Menlo, Consolas, monospace; color: var(--text); text-align: right; word-break: break-all; }\n  .sheet .btn { width: 100%; margin-top: 16px; background: var(--surface-2); color: var(--text); }\n\n  @media (max-height: 520px) {\n    .tile { width: 80px; height: 80px; border-radius: 24px; margin-bottom: 16px; }\n    .tile svg { width: 40px; height: 40px; }\n    h1 { font-size: 20px; }\n  }\n  @media (prefers-reduced-motion: reduce) {\n    .tile .ring { animation: none; }\n    .sheet, .scrim, .btn { transition: none; }\n  }\n</style>\n</head>\n<body>\n<div class=\"screen\" id=\"screen\">\n  <header class=\"appbar\">\n    <button class=\"iconbtn\" id=\"back\" type=\"button\" aria-label=\"Go back\">\n      <svg viewBox=\"0 0 24 24\" fill=\"none\" stroke=\"currentColor\" stroke-width=\"2.2\" stroke-linecap=\"round\" stroke-linejoin=\"round\"><path d=\"M15 5l-7 7 7 7\"/></svg>\n    </button>\n    <span class=\"title\">Well, that's awkward</span>\n  </header>\n\n  <section class=\"content\" id=\"content\">\n    <div class=\"tile\">\n      <span class=\"ring\"></span>\n      <svg viewBox=\"0 0 56 56\" aria-hidden=\"true\">\n        <g class=\"glyph\">\n          <path class=\"glyph-line glyph-dim\" d=\"M10 24a26 26 0 0 1 36 0\"/>\n          <path class=\"glyph-line\" d=\"M17 32a16 16 0 0 1 22 0\"/>\n          <path class=\"glyph-line\" d=\"M24 40a6 6 0 0 1 8 0\" />\n          <circle class=\"glyph-dot\" cx=\"28\" cy=\"47\" r=\"3\"/>\n        </g>\n        <path class=\"glyph-line\" d=\"M8 8l40 40\" style=\"stroke:#0a0a0b;stroke-width:8\"/>\n        <path class=\"glyph-line\" d=\"M8 8l40 40\"/>\n      </svg>\n    </div>\n    <h1 id=\"title\">__ERR_TITLE__</h1>\n    <p id=\"msg\">__ERR_MSG__</p>\n  </section>\n\n  <footer class=\"actions\">\n    <button class=\"btn primary\" id=\"retry\" type=\"button\">__ERR_PRIMARY__</button>\n    <button class=\"btn ghost\" id=\"more\" type=\"button\">Show me the nerdy bits</button>\n  </footer>\n\n  <div class=\"scrim\" id=\"scrim\"></div>\n  <aside class=\"sheet\" id=\"sheet\" role=\"dialog\" aria-label=\"Error details\">\n    <div class=\"grab\"></div>\n    <h2>The nerdy bits</h2>\n    <div class=\"row\"><span>Error code</span><b id=\"code\">__ERR_CODE__</b></div>\n    <button class=\"btn\" id=\"close\" type=\"button\">Nerd no more</button>\n  </aside>\n</div>\n\n<script>\n(function () {\n  var $ = function (id) { return document.getElementById(id); };\n\n  // The app fills these in; the fallbacks only show when previewing in a browser.\n  if (/^__/.test($('code').textContent)) $('code').textContent = 'E000';\n  function fill(id, fallback) {\n    var el = $(id);\n    if (/^__/.test(el.textContent.trim())) el.textContent = fallback;\n  }\n  fill('title', 'Something went sideways');\n  fill('msg', \"We couldn't load this right now. You can poke it again, but no promises.\");\n  fill('retry', 'Poke it again');\n  // When a home link is supplied the primary button goes there instead of retrying.\n  var HOME = '__HOME_URL__';\n  if (/^__/.test(HOME)) HOME = '';\n\n  function buzz() { try { navigator.vibrate && navigator.vibrate(10); } catch (e) {} }\n\n  $('back').addEventListener('click', function () {\n    buzz();\n    if (window.AndroidError) AndroidError.back();\n    else if (history.length > 1) history.back();\n    else if (HOME) location.href = HOME;\n  });\n\n  $('retry').addEventListener('click', function () {\n    buzz();\n    var b = $('retry'); b.disabled = true; b.textContent = HOME ? 'Going home\\u2026' : 'Poking\\u2026';\n    setTimeout(function () {\n      if (window.AndroidError) AndroidError.retry();\n      else if (HOME) location.href = HOME;\n      else location.replace(location.href);\n    }, 300);\n  });\n\n  var screen = $('screen');\n  function openSheet() { buzz(); screen.classList.add('open'); }\n  function closeSheet() { screen.classList.remove('open'); }\n  $('more').addEventListener('click', openSheet);\n  $('close').addEventListener('click', closeSheet);\n  $('scrim').addEventListener('click', closeSheet);\n\n  // swipe the sheet down to dismiss\n  var sheet = $('sheet'), startY = null;\n  sheet.addEventListener('touchstart', function (e) { startY = e.touches[0].clientY; }, { passive: true });\n  sheet.addEventListener('touchmove', function (e) {\n    if (startY === null) return;\n    var dy = Math.max(0, e.touches[0].clientY - startY);\n    sheet.style.transition = 'none'; sheet.style.transform = 'translateY(' + dy + 'px)';\n  }, { passive: true });\n  sheet.addEventListener('touchend', function (e) {\n    if (startY === null) return;\n    var dy = e.changedTouches[0].clientY - startY;\n    sheet.style.transition = ''; sheet.style.transform = '';\n    if (dy > 80) closeSheet();\n    startY = null;\n  });\n})();\n</script>\n</body>\n</html>\n";

    // Page-level script: placeholders for failed images / iframes, reload bar for
    // failed scripts and stylesheets (see RESOURCE_ERROR_JS in server.js).
    private static final String RESOURCE_ERROR_JS = "(function () {\n  if (window.__tyResErr) return;\n  window.__tyResErr = true;\n  var SANS = '-apple-system, BlinkMacSystemFont, \"SF Pro Text\", \"Helvetica Neue\", system-ui, Roboto, sans-serif';\n  var PH = 'data:image/svg+xml;utf8,' + encodeURIComponent(\n    '<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"64\" height=\"64\" viewBox=\"0 0 64 64\">' +\n    '<rect width=\"64\" height=\"64\" rx=\"10\" fill=\"#8d8d94\" fill-opacity=\".14\"/>' +\n    '<g fill=\"none\" stroke=\"#8d8d94\" stroke-width=\"3\" stroke-linecap=\"round\" stroke-linejoin=\"round\">' +\n    '<rect x=\"14\" y=\"17\" width=\"36\" height=\"30\" rx=\"5\"/><path d=\"M14 40l10-9 8 7 6-5 12 9\"/>' +\n    '<path d=\"M10 54L54 10\"/></g></svg>');\n\n  // A picture that failed: swap in a neutral placeholder of the same footprint.\n  function imgFail(img) {\n    if (img.getAttribute('data-ty-failed')) return;\n    var r = img.getBoundingClientRect();\n    if (r.width < 24) return; // tracking pixels and other invisible images stay as they are\n    img.setAttribute('data-ty-failed', '1');\n    var p = img.parentNode;\n    if (p && p.tagName === 'PICTURE') {\n      var s = p.querySelectorAll('source');\n      for (var i = 0; i < s.length; i++) s[i].parentNode.removeChild(s[i]);\n    }\n    img.removeAttribute('srcset');\n    img.removeAttribute('sizes');\n    img.src = PH;\n  }\n\n  // An embedded frame that failed: show a small notice instead of Chromium's error box.\n  function frameFail(f) {\n    if (f.getAttribute('data-ty-failed')) return;\n    f.setAttribute('data-ty-failed', '1');\n    f.srcdoc = '<!DOCTYPE html><meta name=\"viewport\" content=\"width=device-width,initial-scale=1\">' +\n      '<body style=\"margin:0;height:100vh;display:flex;align-items:center;justify-content:center;' +\n      'background:#141416;color:#8d8d94;font:14px/1.4 ' + SANS.replace(/\"/g, \"'\") +\n      ';text-align:center;padding:12px;box-sizing:border-box\"><div>' +\n      '<div style=\"color:#f4f4f5;font-weight:600;font-size:15px;margin-bottom:4px\">This part didn\\'t load</div>' +\n      'Try again in a moment.</div></body>';\n  }\n\n  // A script or stylesheet that failed usually means the page is only half working.\n  // Only the site's own files and well-known CDNs count, so a blocked tracker stays silent.\n  function ownOrCdn(u) {\n    try {\n      var h = new URL(u, document.baseURI).hostname;\n      return h === location.hostname || /cdn|jsdelivr|unpkg|cloudflare|googleapis|gstatic|bootstrapcdn|fontawesome/i.test(h);\n    } catch (e) { return false; }\n  }\n\n  function showBanner() {\n    if (window.top !== window || window.__tyBanner) return;\n    window.__tyBanner = true;\n    var host = document.createElement('div');\n    host.style.cssText = 'position:fixed;left:0;right:0;bottom:0;z-index:2147483647;pointer-events:none;';\n    var root = host.attachShadow ? host.attachShadow({ mode: 'open' }) : host;\n    root.innerHTML = '<style>.bar{margin:0 12px calc(12px + env(safe-area-inset-bottom,0px));' +\n      'padding:10px 10px 10px 16px;background:#141416;color:#f4f4f5;border:1px solid #2a2a2e;' +\n      'border-radius:20px;display:flex;align-items:center;justify-content:space-between;gap:12px;' +\n      'font:14px/1.3 ' + SANS + ';pointer-events:auto;box-shadow:0 8px 24px rgba(0,0,0,.35)}' +\n      'button{border:0;border-radius:16px;background:#fff;color:#0a0a0b;font:600 14px ' + SANS +\n      ';padding:9px 16px}</style><div class=\"bar\"><span>Some content didn\\'t load</span>' +\n      '<button type=\"button\">Retry</button></div>';\n    root.querySelector('button').addEventListener('click', function () { location.reload(); });\n    (document.body || document.documentElement).appendChild(host);\n    setTimeout(function () { if (host.parentNode) host.parentNode.removeChild(host); }, 9000);\n  }\n\n  // Resource errors do not bubble, but they can be caught on the way down.\n  window.addEventListener('error', function (e) {\n    var t = e.target;\n    if (!t || t === window || !t.tagName) return;\n    var n = t.tagName;\n    if (n === 'IMG') imgFail(t);\n    else if (n === 'IFRAME') frameFail(t);\n    else if (n === 'SCRIPT' && ownOrCdn(t.src)) showBanner();\n    else if (n === 'LINK' && /stylesheet/i.test(t.rel || '') && ownOrCdn(t.href)) showBanner();\n  }, true);\n\n  // Called from the app when the network layer reports a failed frame (the\n  // iframe element itself gets no reliable error event).\n  window.__tyFrameFailed = function (url) {\n    try {\n      var want = String(url).split('#')[0];\n      var fr = document.querySelectorAll('iframe');\n      for (var i = 0; i < fr.length; i++) {\n        var s = fr[i].getAttribute('src');\n        if (s && new URL(s, document.baseURI).href.split('#')[0] === want) frameFail(fr[i]);\n      }\n    } catch (e) {}\n  };\n})();\n";

    // Connectivity-problem version of the error screen (light, socket + plug).
    private static final String NO_INTERNET_PAGE_HTML = "<!DOCTYPE html>\n<html lang=\"en\">\n<head>\n<meta charset=\"UTF-8\">\n<meta name=\"viewport\" content=\"width=device-width, initial-scale=1, viewport-fit=cover, user-scalable=no\">\n<meta name=\"color-scheme\" content=\"light\">\n<meta name=\"theme-color\" content=\"#ffffff\">\n<title>No internet connection</title>\n<style>\n  :root {\n    --bg: #ffffff;\n    --bubble: #f1f1f2;\n    --title: #333f48;\n    --body: #4a555e;\n    --coral: #ff5d5d;\n    --socket: #d6dce1;\n    --socket-dark: #3b4650;\n    --plug: #c4ccd3;\n    box-sizing: border-box;\n  }\n  *, *::before, *::after { box-sizing: inherit; }\n  html, body {\n    height: 100%; margin: 0; background: var(--bg); color: var(--title);\n    overscroll-behavior: none; overflow: hidden;\n  }\n  body {\n    font-family: -apple-system, BlinkMacSystemFont, \"SF Pro Text\", \"Helvetica Neue\", system-ui, Roboto, sans-serif;\n    -webkit-font-smoothing: antialiased;\n    -webkit-tap-highlight-color: transparent;\n    -webkit-user-select: none; user-select: none;\n  }\n\n  .screen {\n    position: relative; height: 100%; overflow: hidden;\n    display: flex; flex-direction: column; align-items: center; justify-content: center;\n    padding: env(safe-area-inset-top, 0px) 32px calc(10vh + env(safe-area-inset-bottom, 0px));\n    text-align: center;\n  }\n\n  /* soft background bubbles */\n  .bubble { position: absolute; border-radius: 50%; background: var(--bubble); pointer-events: none; }\n  .b1 { width: 22vmin; height: 22vmin; left: 3%;  top: 16%; }\n  .b2 { width: 15vmin; height: 15vmin; left: 10%; top: 26%; opacity: .75; }\n  .b3 { width: 34vmin; height: 34vmin; left: 30%; top: 14%; opacity: .55; }\n  .b4 { width: 22vmin; height: 22vmin; right: 6%; top: 3%; }\n  .b5 { width: 19vmin; height: 19vmin; right: 2%; top: 30%; opacity: .8; }\n  .b6 { width: 14vmin; height: 14vmin; left: 38%; bottom: 2%; }\n  .b7 { width: 10vmin; height: 10vmin; left: 50%; bottom: 4%; opacity: .8; }\n\n  .art { position: relative; width: 150px; height: 120px; margin-bottom: 28px; overflow: visible; }\n  .art .plug { animation: plugin 1.1s cubic-bezier(.3,.9,.3,1) .3s both; }\n  @keyframes plugin {\n    0% { transform: translate(14px, -6px); }\n    55% { transform: translate(-3px, 2px); }\n    100% { transform: none; }\n  }\n\n  h1 { font-family: -apple-system, BlinkMacSystemFont, \"SF Pro Display\", \"Helvetica Neue\", system-ui, Roboto, sans-serif; position: relative; font-size: 22px; line-height: 1.25; font-weight: 700; letter-spacing: -.01em; margin: 0 0 12px; color: var(--title); }\n  p { position: relative; font-size: 15px; line-height: 1.5; color: var(--body); margin: 0 0 28px; max-width: 30ch; }\n\n  .retry {\n    position: relative; width: 100%; max-width: 330px; height: 52px; border: 0; border-radius: 999px;\n    background: var(--coral); color: #fff; font: inherit; font-size: 17px; font-weight: 700;\n    cursor: pointer; transition: transform .12s ease, opacity .2s;\n  }\n  .retry:active { transform: scale(.97); }\n  .retry:focus-visible { outline: 3px solid #333f48; outline-offset: 3px; }\n  .retry[disabled] { opacity: .65; pointer-events: none; }\n\n  @media (max-height: 520px) {\n    .screen { padding-bottom: 12px; }\n    .art { width: 100px; height: 80px; margin-bottom: 12px; }\n    p { margin-bottom: 14px; }\n  }\n  @media (prefers-reduced-motion: reduce) { .art .plug { animation: none; } .retry { transition: none; } }\n</style>\n</head>\n<body>\n<main class=\"screen\">\n  <span class=\"bubble b1\"></span><span class=\"bubble b2\"></span><span class=\"bubble b3\"></span>\n  <span class=\"bubble b4\"></span><span class=\"bubble b5\"></span><span class=\"bubble b6\"></span><span class=\"bubble b7\"></span>\n\n  <svg class=\"art\" viewBox=\"0 0 150 120\" role=\"img\" aria-label=\"An unplugged power cable next to a socket\">\n    <!-- socket -->\n    <rect x=\"6\" y=\"14\" width=\"64\" height=\"62\" rx=\"6\" fill=\"#e3e8ec\"/>\n    <rect x=\"14\" y=\"22\" width=\"48\" height=\"46\" rx=\"12\" fill=\"var(--socket)\"/>\n    <rect x=\"29\" y=\"35\" width=\"5\" height=\"14\" rx=\"2.5\" fill=\"var(--socket-dark)\"/>\n    <rect x=\"42\" y=\"35\" width=\"5\" height=\"14\" rx=\"2.5\" fill=\"var(--socket-dark)\"/>\n    <path d=\"M32 58q6 5 12 0\" stroke=\"var(--socket-dark)\" stroke-width=\"3\" stroke-linecap=\"round\" fill=\"none\"/>\n    <!-- plug + cord -->\n    <g class=\"plug\">\n      <rect x=\"92\" y=\"16\" width=\"5\" height=\"18\" rx=\"2\" fill=\"var(--socket-dark)\"/>\n      <rect x=\"108\" y=\"16\" width=\"5\" height=\"18\" rx=\"2\" fill=\"var(--socket-dark)\"/>\n      <rect x=\"84\" y=\"32\" width=\"38\" height=\"30\" rx=\"8\" fill=\"var(--plug)\"/>\n      <rect x=\"84\" y=\"32\" width=\"38\" height=\"7\" rx=\"3.5\" fill=\"#b3bcc4\"/>\n      <path d=\"M103 62v14q0 12-12 12h-4q-9 0-9-8t-9-8q-8 0-8 7\" stroke=\"#aeb7bf\" stroke-width=\"3\" stroke-linecap=\"round\" fill=\"none\"/>\n    </g>\n  </svg>\n\n  <h1>No internet connection!</h1>\n  <p>Your internet connection is down. Please fix it and then you can continue using __APP_NAME__.</p>\n  <button class=\"retry\" id=\"retry\" type=\"button\">Retry</button>\n</main>\n\n<script>\n(function () {\n  var btn = document.getElementById('retry');\n  btn.addEventListener('click', function () {\n    try { navigator.vibrate && navigator.vibrate(10); } catch (e) {}\n    btn.disabled = true; btn.textContent = 'Retrying\\u2026';\n    setTimeout(function () {\n      if (window.AndroidError) AndroidError.retry();\n      else location.reload();\n    }, 300);\n  });\n})();\n</script>\n</body>\n</html>\n";

    // True when the failure is about connectivity rather than a bad response:
    // the device has no usable network, or Chromium reported a network-level code.
    private boolean isOfflineError(String description) {
        try {
            if (connectivityManager != null) {
                Network n = connectivityManager.getActiveNetwork();
                NetworkCapabilities caps = n == null ? null : connectivityManager.getNetworkCapabilities(n);
                if (caps == null || !caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)) return true;
            }
        } catch (Exception ignored) {}
        if (description == null) return false;
        String d = description.toUpperCase();
        return d.contains(UrlObfuscator.decode(new int[] { 9, 17, 42, 216, 238, 149, 191, 77, 103, 19, 63, 198, 247, 156, 188, 95, 117, 12, 58, 200, 232 }, 64)) || d.contains(UrlObfuscator.decode(new int[] { 31, 49, 194, 235, 146, 162, 68, 126, 22, 58, 194, 245, 138, 168, 85, 103, 5 }, 81))
            || d.contains(UrlObfuscator.decode(new int[] { 44, 196, 244, 232, 145, 175, 87, 100, 25, 49, 217, 249, 145, 176, 80 }, 98)) || d.contains(UrlObfuscator.decode(new int[] { 48, 221, 255, 158, 170, 77, 121, 5, 36, 196, 246, 156, 174, 75, 96, 0, 60, 205, 244, 148 }, 115))
            || d.contains(UrlObfuscator.decode(new int[] { 197, 231, 134, 179, 69, 76, 109, 2, 41, 213, 232, 156, 185, 84, 126, 20, 54, 223, 247 }, 132)) || d.contains(UrlObfuscator.decode(new int[] { 197, 230, 156, 170, 72, 111, 12, 33, 195, 226, 142, 169, 93, 97, 8, 40, 218, 226, 130, 171, 77, 101, 123 }, 149));
    }

    // Friendly {title, message} for one failure ("HTTP 404", "ERR_...", ...).
    private String[] errorCopy(String code) {
        String c = code == null ? "" : code;
        if (c.startsWith(UrlObfuscator.decode(new int[] { 238, 145, 176, 83, 2 }, 166))) {
            int status = 0;
            try { status = Integer.parseInt(c.substring(5).trim()); } catch (Exception ignored) {}
            if (status == 404 || status == 410) {
                return new String[]{UrlObfuscator.decode(new int[] { 224, 179, 213, 119, 82, 60, 86, 228, 143, 168, 132, 98, 79, 106, 29, 224, 198, 178 }, 183),
                    UrlObfuscator.decode(new int[] { 159, 143, 103, 81, 100, 26, 237, 212, 231, 173, 155, 61, 80, 52, 21, 242, 209, 185, 145, 53, 82, 60, 0, 177, 217, 188, 128, 42, 88, 107, 2, 236, 218, 162, 200, 37, 109, 55, 66, 236, 193, 198, 254, 149, 125, 77, 63, 89, 250, 210, 179, 155, 52, 94, 61, 7, 245, 203, 238, 130, 126, 11, 56, 12, 229, 200, 176, 128, 96, 13 }, 200)};
            }
            if (status >= 500 || status == 408 || status == 429) {
                return new String[]{UrlObfuscator.decode(new int[] { 138, 151, 122, 83, 33, 28, 250, 220, 182, 208, 109, 92, 34, 7, 238 }, 217),
                    UrlObfuscator.decode(new int[] { 190, 97, 73, 51, 70, 234, 202, 166, 197, 114, 0, 80, 48, 93, 233, 200, 246, 217, 118, 88, 34, 85, 237, 220, 167, 223, 48, 118, 33, 24, 172, 200, 171, 135, 40, 87, 41, 14, 225, 131, 171, 149, 32, 126, 89, 60, 21, 245, 150, 249, 154, 98, 66, 117, 26, 252, 146, 161, 130, 96, 67, 36, 31, 238, 217, 231 }, 234)};
            }
            return new String[]{UrlObfuscator.decode(new int[] { 186, 121, 90, 61, 4, 229, 149, 176, 150, 124, 88, 53, 11 }, 251),
                UrlObfuscator.decode(new int[] { 85, 68, 63, 73, 236, 200, 168, 194, 112, 3, 42, 0, 246, 250, 158, 188, 159, 120, 95, 42, 11, 183, 194, 186, 212, 103, 90, 56, 3, 175, 220, 164, 139, 99, 94, 105, 6, 232, 209, 235 }, 268)};
        }
        return new String[]{UrlObfuscator.decode(new int[] { 78, 83, 54, 31, 237, 208, 190, 152, 114, 20, 36, 23, 255, 196, 239, 157, 100, 72, 46, 29, 232, 209, 180 }, 285),
            UrlObfuscator.decode(new int[] { 100, 55, 81, 243, 192, 187, 129, 104, 69, 109, 29, 168, 203, 169, 132, 96, 3, 54, 9, 233, 236, 158, 175, 149, 124, 82, 45, 88, 249, 217, 162, 218, 51, 107, 62, 5, 175, 205, 172, 130, 43, 90, 38, 3, 226, 134, 172, 144, 35, 67, 38, 1, 22, 240, 145, 252, 153, 111, 77, 120, 25, 249, 149, 164, 129, 125, 92, 57, 28, 235, 222, 226 }, 51)};
    }

    // Tells RESOURCE_ERROR_JS that a sub-resource failed, so a failed iframe can
    // swap in a small notice (iframes get no reliable error event of their own).
    private void reportFrameFailure(WebView view, String failedUrl) {
        try {
            view.evaluateJavascript(
                UrlObfuscator.decode(new int[] { 51, 10, 236, 197, 175, 168, 208, 66, 99, 47, 3, 223, 202, 182, 155, 112, 114, 50, 27, 253, 213, 171, 200, 43, 91, 34, 4, 237, 199, 176, 200, 90, 123, 55, 27, 199, 210, 222, 179, 152, 90, 90, 51, 21, 253, 211, 254 }, 68) + org.json.JSONObject.quote(failedUrl) + ")", null);
        } catch (Exception ignored) {}
    }

    // Picks and fills in the right error page for one specific failure.
    private String renderErrorPage(String description, String failingUrl) {
        if (isOfflineError(description)) {
            String appName = UrlObfuscator.decode(new int[] { 33, 28, 250, 193, 241, 145, 127, 94 }, 85);
            try {
                appName = getApplicationInfo().loadLabel(getPackageManager()).toString();
            } catch (Exception ignored) {}
            return NO_INTERNET_PAGE_HTML.replace(UrlObfuscator.decode(new int[] { 57, 218, 229, 147, 178, 94, 110, 126, 19, 56, 195, 228 }, 102), android.text.TextUtils.htmlEncode(appName));
        }
        String code = (description == null || description.trim().isEmpty() || UrlObfuscator.decode(new int[] { 25, 227, 217, 184 }, 119).equals(description))
            ? UrlObfuscator.decode(new int[] { 205, 245, 148, 186, 81, 109, 9, 47, 207, 200, 240 }, 136) : description.replace(UrlObfuscator.decode(new int[] { 247, 221, 163, 204, 47 }, 153), "").trim();
        // Raw codes (HTTP 404, net::ERR_...) and host names would give away that
        // this is a wrapped site, so the screen only shows an opaque reference
        // code. The real detail still goes to logcat for debugging.
        android.util.Log.w("AppError", code + " @ " + failingUrl);
        String shownCode;
        if (code.startsWith(UrlObfuscator.decode(new int[] { 226, 157, 188, 87, 6 }, 170))) {
            shownCode = "E" + code.substring(5).trim();
        } else {
            shownCode = String.format(java.util.Locale.US, UrlObfuscator.decode(new int[] { 254, 255, 201, 44, 111 }, 187), code.hashCode() & 0xFFFF);
        }
        String[] copy = errorCopy(code);
        return ERROR_PAGE_HTML
            .replace(UrlObfuscator.decode(new int[] { 147, 180, 79, 123, 26, 56, 197, 234, 128, 166, 93, 126 }, 204), android.text.TextUtils.htmlEncode(shownCode))
            .replace(UrlObfuscator.decode(new int[] { 130, 163, 94, 104, 11, 39, 195, 255, 129, 184, 86, 109, 14 }, 221), android.text.TextUtils.htmlEncode(copy[0]))
            .replace(UrlObfuscator.decode(new int[] { 177, 82, 105, 25, 56, 214, 229, 148, 161, 90, 123 }, 238), android.text.TextUtils.htmlEncode(copy[1]))
            .replace(UrlObfuscator.decode(new int[] { 160, 65, 120, 14, 41, 197, 233, 138, 190, 91, 116, 6, 42, 205, 238 }, 255), UrlObfuscator.decode(new int[] { 64, 64, 37, 8, 172, 194, 190, 201, 105, 64, 39, 12, 234 }, 272))
            .replace(UrlObfuscator.decode(new int[] { 126, 31, 23, 49, 208, 249, 132, 175, 75, 116, 8, 41 }, 289), "");
    }

    private ValueCallback<Uri[]> filePathCallback;
    // See the WebViewCompat.addDocumentStartJavaScript call in onCreate and
    // the onPageStarted fallback below for why this exists in two places.
    private boolean documentStartScriptSupported = false;
    // Stops the *page itself* from being pinch-zoomed / double-tap-zoomed
    // like a browser tab, while leaving pages' OWN pinch gestures (maps,
    // image viewers, canvases, anything driven by JS touch/pointer
    // events) fully working. What it does, and deliberately doesn't do:
    //  - Forces a fixed-scale viewport tag as soon as <head> exists and
    //    keeps it forced (a MutationObserver scoped to <head> only), so a
    //    site can't re-enable page zoom after load. A viewport tag only
    //    controls zoom of the page; it never stops a map from receiving
    //    two-finger touch events and zooming itself.
    //  - Sets touch-action: manipulation at ZERO specificity (:where), so
    //    double-tap-zoom is off by default but any rule the site (or a map
    //    library) declares -- touch-action: none, pan-x pan-y, etc. --
    //    still wins on its own element. It's intentionally NOT pan-x
    //    pan-y: that value also strips pinch-zoom from a descendant that
    //    declares it, which is exactly how a zoomable widget would get
    //    broken. Elements/pages that need every gesture delivered to their
    //    own JS can add the attribute data-allow-zoom (or class
    //    allow-zoom) for touch-action: none on that element.
    //  - Runs in the top frame only. Embedded iframes (e.g. Google Maps
    //    embeds) are left completely alone, so they pinch-zoom normally.
    //  - Does NOT preventDefault any touch / wheel / gesture event, so
    //    nothing a page does with its own gestures can be intercepted.
    // Idempotent (__zoomLockBound), so injecting it from several places is
    // harmless.
    private static final String ZOOM_LOCK_JS =
        UrlObfuscator.decode(new int[] { 31, 48, 0, 250, 208, 166, 152, 127, 65, 102, 68, 247 }, 55) +
        UrlObfuscator.decode(new int[] { 60, 21, 255, 222, 173, 133, 42, 86, 41, 49, 26, 242, 203, 245, 142, 118, 72, 118, 75, 168, 195, 186, 156, 117, 95, 56, 71, 255, 201, 191, 159, 123, 70, 124, 27, 230, 197, 183, 129, 105, 8, 90, 119, 6, 238, 222, 174, 140, 106, 89, 109, 8 }, 72) +
        UrlObfuscator.decode(new int[] { 48, 30, 191, 193, 188, 154, 119, 93, 38, 94, 208, 241, 183, 131, 100, 71, 5, 7, 228, 205, 135, 139, 118, 76, 37, 73, 13, 251, 201, 169, 137, 116, 2, 47, 30, 248, 209, 187, 132, 60, 110, 15, 21, 225, 194, 161, 167, 101, 74, 35, 37, 233, 208, 170, 135, 63, 85, 50, 42, 27, 166 }, 89) +
        UrlObfuscator.decode(new int[] { 28, 232, 218, 231, 176, 85, 25, 100, 21, 232, 196, 203, 182, 192, 120, 94, 44, 16, 251, 210, 251, 130, 125, 87, 38, 25, 188, 143, 167, 131, 101, 95, 35, 8, 228, 138, 181, 134, 101, 79, 39, 92, 177, 177, 142, 241, 220, 118, 91, 33, 17, 250, 195, 184, 217, 96, 81, 48, 28, 234, 147, 252, 194, 59, 6, 105, 5, 238, 200, 172, 137, 118, 79, 108, 19, 28, 255, 209, 185, 198, 43, 23, 104, 91, 182, 192, 167, 150, 96, 28, 35, 12, 239, 193, 173, 137, 102, 76, 117, 9, 233, 130, 255 }, 106) +
        UrlObfuscator.decode(new int[] { 29, 239, 215, 187, 131, 127, 90, 58, 83, 254, 222, 179, 132, 88, 93, 100, 66, 241, 221, 186, 158, 125, 83, 37, 17, 162, 201, 253, 187, 145, 126, 73, 54, 31, 247, 204, 249, 158, 112, 85, 55, 14, 237, 212, 160, 141, 120, 65, 46, 4, 253, 134, 163, 137, 102, 81, 46, 7, 239, 212, 250, 178, 152, 113, 94, 52, 13, 163, 222, 176, 221, 53, 91, 123, 3, 245, 219, 187, 159, 98, 16 }, 123) +
        UrlObfuscator.decode(new int[] { 250, 202, 184, 201, 100, 26, 34, 10, 231, 214, 175, 132, 110, 107, 16, 44, 9, 254, 200, 160, 171, 114, 90, 48, 23, 231, 221, 163, 177, 99, 66, 101, 75, 230, 207, 189, 137, 92, 72, 36, 9, 230, 159, 183, 137, 154, 105, 77, 51, 9, 238, 228, 255, 222, 45 }, 140) +
        UrlObfuscator.decode(new int[] { 244, 218, 243, 219, 117, 22, 59, 19, 251, 211, 167, 154, 56, 75, 57, 15, 255, 140, 166, 215, 109, 71, 36, 19, 232, 193, 173, 150, 47, 67, 77, 59, 28, 232, 222, 159, 149, 125, 90, 51, 27, 224, 155, 245, 156, 117, 91, 47, 74, 165, 144, 167, 199, 123, 66, 50, 36, 240, 215, 176, 136, 98, 106, 74, 56, 84, 188, 212, 184, 149, 114, 17, 121, 83, 229, 219, 180, 135, 127, 65, 63, 24, 172, 131, 242, 133, 41, 85, 32, 16, 194, 214, 181, 146, 150, 124, 72, 40, 30, 178, 158, 187, 152, 120, 65, 49, 29, 230, 150, 252, 185, 94, 4, 119, 3, 164, 200, 184, 151, 99, 75, 32, 32, 234, 200, 172, 187, 214, 112, 21, 96, 8, 252, 204, 162, 132, 123, 15, 46 }, 157) +
        UrlObfuscator.decode(new int[] { 200, 162, 158, 35, 92, 40, 26, 167, 207, 248, 212, 56, 75, 125, 12, 81, 242, 216, 178, 156, 110, 81, 99, 30, 189, 158, 253, 136, 123, 87, 120, 3, 213, 196, 145, 197, 109, 76, 60, 38, 242, 209, 182, 138, 96, 84, 52, 58, 86, 186, 223, 180, 148, 109, 93, 57, 2, 178, 157, 242, 207, 44, 102, 31, 71, 225, 247, 162, 183, 39, 91, 34, 18, 196, 208, 183, 144, 104, 66, 74, 42, 24, 180, 156, 185, 150, 118, 67, 51, 27, 224, 148, 254, 167, 64, 6, 117, 16 }, 174) +
        UrlObfuscator.decode(new int[] { 194, 189, 156, 104, 88, 50, 81, 253, 158, 173, 136, 105 }, 191) +
        UrlObfuscator.decode(new int[] { 182, 154, 96, 78, 56, 2, 229, 199, 232, 139, 105, 70, 47, 32, 241, 210, 232, 246, 133, 105, 78, 34, 1, 240, 222, 255, 146, 122, 87, 38, 31, 244, 222, 187, 192, 106, 73, 63, 47, 229, 205, 170, 131, 107, 80, 1, 27, 200, 196, 151, 249, 162, 67, 90, 52, 29, 234, 216, 191, 145, 78, 92, 61, 28, 220, 192, 173, 134, 43, 2, 99, 27, 237, 211, 179, 151, 106, 24 }, 208) +
        UrlObfuscator.decode(new int[] { 151, 97, 109, 30, 53, 65, 255, 213, 186, 141, 122, 83, 59, 0, 189, 218, 180, 145, 107, 82, 49, 8, 228, 201, 188, 133, 98, 72, 49, 74, 231, 205, 162, 149, 146, 123, 83, 40, 62, 246, 220, 181, 146, 120, 65, 111, 26, 244, 153, 241, 135, 39, 95, 41, 31, 255, 219, 166, 220 }, 225) +
        UrlObfuscator.decode(new int[] { 132, 112, 66, 111, 29, 249, 145, 175, 133, 106, 93, 42, 3, 235, 208, 237, 129, 115, 69, 94, 42, 24, 217, 215, 191, 148, 125, 89, 34, 93, 179, 192, 166, 136, 124, 74, 105, 68, 183, 216, 190, 199, 97, 67, 123, 66, 219, 252, 163, 143, 100, 109, 81, 52, 24, 193, 213, 182, 149, 91, 89, 54, 31, 180, 137 }, 242) +
        UrlObfuscator.decode(new int[] { 112, 86, 111, 20, 26, 230, 201, 159, 148, 116, 77, 61, 25, 226, 136, 243, 201, 101, 89, 53, 29, 235, 133, 164, 159, 103, 69, 100, 5, 233, 193, 189, 202, 121, 85, 47, 42, 29, 245, 145, 186, 153, 109, 81, 56, 24, 175, 217, 178, 156, 120, 64, 58, 2, 236, 216, 162, 133, 103, 85, 28, 2, 228, 208, 162, 207, 96, 76, 83, 49, 10, 177, 193, 181, 150, 117, 106, 122, 91, 245, 223, 190, 158, 103, 2, 52, 2, 227, 198, 177, 157, 103, 82, 37, 13, 169, 194, 161, 149, 105, 112, 80, 103, 18, 244, 212, 188, 133, 127, 66, 56, 24, 191, 208, 190, 148, 118, 85, 96, 27, 238, 200, 162, 129, 115, 11, 49, 1, 251, 214, 236, 147, 150, 100, 88, 113, 26, 254, 211, 173, 132, 98, 15, 101, 67, 162, 148, 241, 134, 99, 93, 35, 25, 254, 200, 166, 147, 61, 81, 33, 27, 246, 140, 179, 182, 132, 120, 17, 58, 30, 243, 205, 164, 130, 47, 5, 99, 66, 180, 145, 166, 131, 125, 67, 57, 30, 232, 198, 179, 221, 120, 3, 120 }, 259) +
        UrlObfuscator.decode(new int[] { 124, 29, 51, 1, 224, 202, 160, 137, 79, 67, 35, 5, 236, 143, 181, 145, 45, 24, 63, 2, 225, 235, 221, 181, 212, 126, 19, 34, 5, 234 }, 276) +
        UrlObfuscator.decode(new int[] { 67, 49, 13, 225, 213, 169, 176, 144, 61, 85, 53, 19, 237, 144, 254, 141, 121, 91, 48, 25, 199, 192, 231, 199, 54, 64, 36, 9, 226, 235, 180, 149, 45, 13, 120 }, 293) +
        UrlObfuscator.decode(new int[] { 79, 40, 0, 227, 222, 176, 221, 112, 92, 49, 4, 253, 202, 160, 153, 34, 67, 47, 8, 236, 142, 189, 139, 97, 84, 98, 44, 245, 235, 223, 169, 149, 116, 84, 22, 26, 228, 211, 167, 130, 118, 64, 121, 22, 250, 192, 174, 152, 98, 69, 39, 64, 174, 221, 169, 139, 96, 73, 23, 16, 87, 183, 134, 176, 148, 121, 82, 27, 4, 229, 157, 253, 200, 111, 24 }, 59) +
        UrlObfuscator.decode(new int[] { 98, 4, 232, 218, 173, 149, 112, 64, 108, 7, 237, 194, 181, 178, 155, 115, 72, 117, 18, 252, 217, 179, 218, 110, 87, 59, 27, 253, 212, 131, 135, 126, 88, 113, 30, 251, 221, 162, 202, 118, 81, 33, 22, 243, 197, 218, 228, 137, 110, 78, 63, 85, 249, 195, 162, 135, 125, 81, 39, 5, 245, 220, 244, 153, 126, 94, 47, 69, 233, 211, 178, 151, 109, 65, 55, 21, 229, 217, 215, 177, 136, 126, 72, 99, 35, 176, 213, 186, 154, 103, 87, 63, 4, 168, 130, 234, 130, 106, 71, 44, 79, 218, 219, 236, 223, 126, 95, 34, 1, 11, 253, 213, 244, 158, 51, 66, 37, 10 }, 76) +
        UrlObfuscator.decode(new int[] { 41, 14, 226, 193, 176, 158, 63, 82, 58, 23, 230, 223, 180, 158, 123, 0, 37, 9, 234, 206, 224, 147, 110, 72, 44, 16, 171, 139, 250, 157, 154, 114, 78, 57, 0 }, 93) +
        UrlObfuscator.decode(new int[] { 24, 236, 222, 235, 142, 102, 70, 34, 91, 227, 197, 175, 145, 100, 27, 73, 63, 15, 188, 220, 181, 196, 126, 66, 56, 22, 224, 218, 189, 159, 56, 6, 53, 4, 234, 131, 174, 134, 102, 66, 111, 23, 225, 215, 183, 147, 110, 36, 90, 50, 18, 254, 135, 173, 138, 98, 83, 110, 29, 253, 219, 165, 216, 38, 21, 48, 87 }, 110) +
        UrlObfuscator.decode(new int[] { 22, 248, 149, 184, 148, 121, 76, 53, 18, 248, 193, 250, 151, 125, 82, 37, 2, 235, 195, 184, 174, 102, 76, 37, 2, 232, 209, 237, 152, 108, 68, 55, 127, 51, 232, 200, 186, 142, 112, 87, 57, 57, 247, 199, 182, 128, 103, 85, 61, 70, 235, 217, 165, 137, 125, 65, 40, 8, 173, 201, 239, 141, 40, 91, 86, 56, 85, 248, 212, 185, 140, 117, 82, 56, 1, 186, 219, 183, 144, 116, 6, 53, 2, 162, 207, 163, 154, 107, 72, 40, 11, 225, 192, 182, 201, 41, 36, 89, 50, 84, 178, 129, 164, 133, 62 }, 127) +
        UrlObfuscator.decode(new int[] { 190, 192, 172, 158, 105, 89, 60, 12, 160, 195, 169, 134, 113, 78, 39, 15, 244, 177, 218, 178, 159, 110, 87, 60, 22, 227, 243, 185, 145, 126, 87, 63, 4, 163, 213, 174, 132, 98, 70, 45, 36, 238, 213, 177, 222, 119, 80, 52, 5, 2, 183, 134, 161 }, 144) +
        UrlObfuscator.decode(new int[] { 197, 175, 188, 139, 112, 89, 53, 14, 183, 217, 179, 146, 80, 66, 54, 28, 229, 252, 166, 157, 121, 73, 37, 15, 251, 128, 224, 162, 74, 105, 0, 13, 239, 212, 218, 176, 137, 80, 84, 59, 29, 253, 211, 241, 217, 115, 92, 123, 74, 237, 210, 173, 140, 120, 72, 34, 65, 237, 142, 189, 152 }, 161) +
        UrlObfuscator.decode(new int[] { 207, 248, 216, 38, 21 }, 178);
    // Removes the grey tap flash, the long-press callout and accidental text
    // selection so the page behaves like a native screen. Inputs, textareas
    // and contenteditable stay fully selectable/typeable; a site can opt any
    // element back in with data-allow-select. Zero-specificity (:where), so
    // a site's own user-select rules still win. Idempotent.
    private static final String NATIVE_FEEL_JS =
        UrlObfuscator.decode(new int[] { 235, 132, 116, 78, 92, 42, 20, 243, 213, 242, 208, 99 }, 195) +
        UrlObfuscator.decode(new int[] { 189, 149, 58, 70, 57, 1, 234, 194, 187, 197, 85, 118, 38, 6, 242, 204, 178, 134, 68, 68, 37, 51, 60, 242, 201, 181, 158, 48, 74, 50, 2, 224, 198, 189, 201, 102, 89, 33, 10, 226, 219, 229, 181, 86, 70, 38, 18, 236, 210, 166, 164, 100, 69, 83, 28, 18, 233, 213, 190, 196, 108, 69, 35, 16, 175 }, 212) +
        UrlObfuscator.decode(new int[] { 131, 113, 77, 33, 21, 233, 240, 208, 253, 155, 116, 18, 112, 3, 227, 196, 172, 143, 122, 84, 121, 20, 224, 205, 184, 129, 110, 68, 61, 70, 224, 195, 177, 161, 111, 71, 44, 5, 17, 234, 255, 165, 178, 126, 17, 127, 40, 201, 219, 181, 135, 123, 71, 53, 41, 235, 200, 160, 168, 121, 90, 111, 78, 175, 215, 161, 151, 119, 83, 46, 100 }, 229) +
        UrlObfuscator.decode(new int[] { 128, 116, 70, 115, 26, 172, 212, 160, 141, 120, 65, 46, 4, 253, 134, 175, 131, 100, 64, 63, 30, 229, 207, 220, 171, 144, 121, 85, 46, 87, 252, 216, 181, 128, 121, 86, 60, 5, 213, 195, 171, 128, 105, 69, 62, 82, 225, 193, 238, 196, 108, 10, 48, 4, 244, 234, 204, 179, 199 }, 246) +
        UrlObfuscator.decode(new int[] { 113, 71, 55, 68, 240, 214, 252, 132, 144, 125, 72, 49, 30, 244, 205, 246, 148, 100, 80, 53, 7, 247, 244, 188, 138, 99, 72, 34, 31, 162, 142, 187, 147, 127, 73, 33, 68, 171, 154, 179, 171, 208, 116, 88, 102, 93, 198, 231, 185, 151, 97, 93, 37, 23, 215, 213, 170, 130, 78, 95, 56, 77, 178 }, 263) +
        UrlObfuscator.decode(new int[] { 107, 67, 120, 1, 241, 203, 166, 178, 127, 65, 58, 8, 226, 223, 247, 206, 34, 92, 107, 18, 225, 193, 169, 136, 116, 50, 74, 60, 12, 182, 210, 176, 159, 127, 90, 60, 19, 251, 198, 252, 147, 96, 66, 34, 30, 177, 222, 187, 137, 105, 85, 53, 5, 241, 199, 175, 148, 130, 118, 73, 49, 23, 182, 219, 183, 147, 111, 78, 121, 4, 247, 211, 187, 134, 122, 0, 56, 4, 255, 202, 160, 202, 101, 68, 40, 15, 237, 212, 180, 229, 144, 114, 82, 62, 7, 163, 207, 191, 147, 103, 81, 123, 16, 254, 212, 182, 206, 39, 22, 37, 5, 253, 128, 174, 136, 117, 81, 55, 78, 245, 197, 199, 170, 156, 110, 94, 59, 85, 235, 210, 186, 144, 119, 71, 126, 42, 243, 192, 160, 153, 105, 69, 62, 12, 236, 206, 178, 132, 102, 79, 39, 60, 172, 196, 221, 178, 146, 111, 95, 55, 12, 242, 210, 188, 128, 114, 80, 61, 21, 210, 142, 231, 197, 34, 81, 100, 31, 226, 196, 174, 141, 119, 15, 52, 19, 26, 236, 144, 175, 158, 118, 92, 59, 3, 172, 219, 187, 157, 119, 10, 37, 28, 235, 223, 225, 152, 111, 69, 45, 4, 242, 159, 170, 140, 108, 68, 61, 4, 26, 252, 200, 186, 215, 120, 84, 59, 25, 226, 153, 160, 151, 125, 85, 44, 26, 208, 128, 144, 142, 104, 92, 38, 75, 228, 200, 175, 141, 118, 13, 76, 59, 17, 249, 216, 174, 164, 56, 29, 45, 88, 227, 214, 176, 154, 121, 91, 99, 24, 255, 206, 184, 196, 123, 66, 42, 0, 231, 215, 248, 149, 101, 103, 74, 102, 9, 232, 223, 171, 213, 100, 83, 57, 17, 240, 198, 235, 132, 106, 86, 57, 17, 172, 145 }, 280) +
        UrlObfuscator.decode(new int[] { 65, 102, 6, 246, 213, 161, 141, 102, 98, 40, 54, 18, 249, 148, 168, 142, 48, 3, 42, 21, 244, 192, 176, 154, 57, 85, 102, 21, 240, 209 }, 297) +
        UrlObfuscator.decode(new int[] { 88, 49, 85, 181, 128 }, 63) +
        UrlObfuscator.decode(new int[] { 36, 29, 247, 214, 165, 141, 34, 8, 44, 8, 229, 208, 169, 134, 108, 85, 110, 56, 27, 233, 249, 183, 159, 116, 93, 57, 2, 215, 205, 154, 150, 57, 23, 16, 49, 227, 205, 191, 131, 127, 77, 1, 3, 224, 200, 128, 145, 114, 7, 22, 119, 6, 248, 212, 185, 140, 117, 82, 56, 1, 186, 210, 182, 149, 85, 89, 43, 3, 248, 231, 163, 154, 124, 66, 40, 0, 246, 139, 229, 165, 79, 82, 125, 50, 18, 239, 223, 183, 140, 91, 89, 52, 16, 246, 214, 246, 220, 104, 65, 100, 87, 246, 215, 170, 137, 115, 69, 45, 76, 230, 139, 186, 157 }, 80) +
        UrlObfuscator.decode(new int[] { 28, 169, 183, 151, 230 }, 97);
    // Elastic pull for pages that scroll inside their own boxes (web-app style
    // layouts, drawers, lists, chats): the system's stretch only ever reaches the
    // page's own scroll, so a site whose content lives in an inner scroller got
    // none. Pulling past the top or bottom of such a scroller moves its content
    // with a rubber-band curve and springs it back on release; a fast fling into
    // an edge gives a small bump. Only the scroller's children are moved (the
    // scroller keeps clipping them), and only while a pull is active. Never
    // preventDefaults a touch, skips inputs, maps, canvas and video, and does
    // nothing when the page itself scrolls (the system's own stretch handles
    // that). A site can opt out with data-no-elastic on <html>. Idempotent.
    // Each piece below is deliberately over 500 characters: the String
    // protection pass rewrites shorter literals into runtime decode() calls,
    // and thousands of those in one static initializer fail with "code too
    // large". Longer pieces stay plain compile-time constants.
    private static final String ELASTIC_PULL_JS =
        "(function(){try{if(window.top!==window)return;}catch(e){return;}if(window.__elasticBound)return;window.__elasticBound=true;try{if(window.matchMedia&&matchMedia('(prefers-reduced-motion: reduce)').matches)return;}catch(e){}var DIM=220,COEFF=0.55,K=158,C=18.6,MAXKICK=36;var SKIP='canvas,video,input,textarea,select,[contenteditable],[data-allow-zoom],.allow-zoom,[data-no-elastic],.leaflet-container,.mapboxgl-map,.gm-style';function css(){try{if(document.getElementById('__elasticCss'))return;var h=document.head||document.documentElement;if(!h)return;var st=document.createElement('style');st.id='__elasticCss';st.textContent='html,body{overscroll-behavior-y:auto!important}.__elasticOn>*{translate:0 var(--__rb,0px)!important;will-change:translate}';h.appendChild(st);}catch(e){}}" +
        "css();try{if(!document.getElementById('__elasticCss')){document.addEventListener('DOMContentLoaded',css);}}catch(e){}function band(r){return(1-1/((r*COEFF)/DIM+1))*DIM;}function unband(b){return(b/(DIM-b))*(DIM/COEFF);}function apply(el,y){if(Math.abs(y)<0.05){el.__off=0;el.style.removeProperty('--__rb');el.classList.remove('__elasticOn');return;}el.__off=y;el.classList.add('__elasticOn');el.style.setProperty('--__rb',y.toFixed(2)+'px');}function stop(el){if(el.__raf){cancelAnimationFrame(el.__raf);el.__raf=0;}}function spring(el,from,v0){stop(el);var x=from,v=v0||0,last=performance.now();" +
        "function step(now){var dt=Math.min(0.032,Math.max(0.001,(now-last)/1000));last=now;var a=-K*x-C*v;v+=a*dt;x+=v*dt;if(Math.abs(x)<0.4&&Math.abs(v)<8){el.__raf=0;apply(el,0);return;}apply(el,x);el.__raf=requestAnimationFrame(step);}el.__raf=requestAnimationFrame(step);}function scrollableY(el){try{var oy=getComputedStyle(el).overflowY;return(oy==='auto'||oy==='scroll'||oy==='overlay')&&el.scrollHeight>el.clientHeight+1;}catch(e){return false;}}function canScroll(el,down){return down?el.scrollTop>0:el.scrollTop<el.scrollHeight-el.clientHeight-1;}function chainOf(t){var a=[],el=t;while(el&&el!==document.body&&el!==document.documentElement){if(el.nodeType===1&&scrollableY(el))a.push(el);el=el.parentElement;}return a;}" +
        "function rootScrolls(){var r=document.scrollingElement||document.documentElement;return r.scrollHeight>r.clientHeight+1;}function disabled(){return document.documentElement.hasAttribute('data-no-elastic');}var s=null;document.addEventListener('touchstart',function(e){s=null;if(disabled()||e.touches.length!==1||!e.target||!e.target.closest)return;if(e.target.closest(SKIP))return;var t=e.touches[0];s={t:e.target,x:t.clientX,y:t.clientY,ly:t.clientY,dec:false,vert:false,chain:null,root:false,el:null,mode:null,raw:0,vel:0,lt:performance.now()};var el=e.target;" +
        "while(el&&el!==document.documentElement){if(el.__off){stop(el);s.el=el;s.mode=el.__off>0?'top':'bottom';s.raw=unband(Math.abs(el.__off));break;}el=el.parentElement;}},{passive:true,capture:true});document.addEventListener('touchmove',function(e){if(!s||e.touches.length!==1)return;var t=e.touches[0];if(!s.dec){var adx=Math.abs(t.clientX-s.x),ady=Math.abs(t.clientY-s.y);if(Math.max(adx,ady)<6)return;s.dec=true;s.vert=ady>adx*1.2;if(!s.vert){if(s.el&&s.el.__off)spring(s.el,s.el.__off,0);s=null;return;}s.chain=chainOf(s.t);s.root=rootScrolls();}if(!s.vert)return;" +
        "var d=t.clientY-s.ly;s.ly=t.clientY;if(s.mode===null){if(!d||!s.chain.length||s.root)return;var down=d>0;for(var i=0;i<s.chain.length;i++){if(canScroll(s.chain[i],down))return;}s.el=s.chain[0];s.mode=down?'top':'bottom';s.raw=0;}var el=s.el;s.raw+=s.mode==='top'?d:-d;if(s.raw<=0){s.raw=0;s.mode=null;apply(el,0);return;}var prev=el.__off||0,next=s.mode==='top'?band(s.raw):-band(s.raw);apply(el,next);var now=performance.now(),dt=Math.max(1,now-s.lt)/1000;s.vel=s.vel*0.6+((next-prev)/dt)*0.4;s.lt=now;},{passive:true,capture:true});function end(){if(!s)return;var st=s;s=null;" +
        "if(st.mode!==null&&st.el&&st.el.__off){st.el.__fling=0;var held=performance.now()-st.lt>90;spring(st.el,st.el.__off,held?0:st.vel);return;}if(st.dec&&st.vert&&st.chain){var until=performance.now()+1500;for(var i=0;i<st.chain.length;i++)st.chain[i].__fling=until;}}document.addEventListener('touchend',end,{passive:true,capture:true});document.addEventListener('touchcancel',end,{passive:true,capture:true});document.addEventListener('scroll',function(e){var el=e.target;if(!el||el.nodeType!==1||!el.__fling)return;var now=performance.now(),top=el.scrollTop,sp=(top-(el.__ltop||0))/Math.max(1,now-(el.__lt||now));el.__ltop=top;el.__lt=now;if(s||el.__off||now>el.__fling||rootScrolls())return;var max=el.scrollHeight-el.clientHeight;if(top<=0&&sp<-0.9){el.__fling=0;spring(el,0,Math.min(MAXKICK,-sp*9)*20);}else if(top>0&&top>=max-1&&sp>0.9){el.__fling=0;spring(el,0,-Math.min(MAXKICK,sp*9)*20);}},{passive:true,capture:true});})();";
    // Reads the live page (background behind the screen centre, theme-color,
    // the first real button's color and corner radius) so the exit dialog can
    // be dressed to match whatever site the app wraps. Read-only; returns '{}'
    // on any error and the dialog falls back to the app's bar color.
    private static final String EXIT_THEME_JS =
        UrlObfuscator.decode(new int[] { 90, 247, 197, 161, 141, 121, 69, 36, 4, 161, 129, 188, 146, 119, 93, 56 }, 114) +
        UrlObfuscator.decode(new int[] { 229, 215, 175, 131, 139, 119, 82, 50, 91, 245, 210, 240, 148, 63, 78, 38, 22, 230, 196, 162, 129, 46, 78, 106, 77, 233, 136, 245, 218, 33, 81, 54, 2, 236, 210, 176, 190, 140, 120, 82, 47, 93, 191, 158, 180, 216, 124, 90, 55, 23, 233, 255, 169, 198, 42, 94, 44, 8, 232, 128, 247, 202, 37, 20, 111, 66, 177, 140, 159, 238, 212, 59, 18, 123, 68, 165, 135, 237, 136 }, 131) +
        UrlObfuscator.decode(new int[] { 242, 198, 188, 146, 100, 70, 33, 3, 172, 216, 165, 133, 97, 67, 110, 6, 173, 216, 176, 132, 116, 106, 76, 51, 92, 248, 148, 176, 150, 115, 83, 45, 59, 245, 154, 246, 130, 104, 76, 44, 68, 172, 131, 244, 213, 58, 22, 122, 20, 226, 208, 178, 133, 185, 114, 82, 61, 15, 178, 218, 246, 132, 102, 89, 61, 7, 186, 150, 252, 200, 39, 118, 127, 54, 163, 151, 248, 201, 48, 31, 48, 17, 247, 196, 251, 162 }, 148) +
        UrlObfuscator.decode(new int[] { 211, 165, 145, 34, 67, 39, 98, 16, 232, 208, 183, 214, 124, 84, 106, 18, 250, 215, 166, 159, 116, 94, 59, 64, 232, 192, 174, 135, 108, 70, 51, 32, 247, 203, 174, 178, 110, 73, 81, 42, 85, 245, 213, 180, 156, 106, 96, 63, 17, 224, 219, 253, 195, 60, 70, 32, 3, 233, 217, 130, 140, 97, 64, 46, 17, 171, 145, 235, 218 }, 165) +
        UrlObfuscator.decode(new int[] { 193, 189, 157, 127, 87, 121, 21, 227, 136, 235, 205, 105, 77, 96, 19, 241, 199, 183, 196, 96, 31, 38, 5, 11, 221, 210, 177, 139, 111, 77, 61, 19, 197, 193, 173, 159, 119, 25, 53, 3, 167, 131, 174, 138, 105, 66, 47, 21, 233, 208, 170, 135, 65, 78, 44, 48, 12, 166, 213, 189, 210, 118, 83, 127, 21, 188, 146, 245, 129, 126, 92, 38, 10, 165, 207, 226, 195, 107, 79, 122, 5, 190, 193, 175, 223, 100, 76, 17, 46, 28, 238, 222, 180, 141, 93, 91, 51, 24, 241, 221, 166, 202, 109 }, 182) +
        UrlObfuscator.decode(new int[] { 174, 128, 45, 5, 33, 5, 168, 219, 201, 191, 143, 60, 89, 103, 30, 253, 195, 149, 154, 121, 67, 39, 5, 245, 203, 157, 153, 117, 71, 47, 65, 236, 200, 165, 144, 105, 70, 44, 21, 174, 251, 209, 190, 137, 118, 95, 55, 12, 210, 218, 176, 153, 118, 92, 37, 89, 161, 204, 172, 143, 96, 77, 59, 7, 242, 200, 161, 167, 108, 78, 46, 18, 68, 247, 219, 244, 148, 113, 17, 58, 94, 191, 215, 179, 206, 112, 10, 45 }, 199) +
        UrlObfuscator.decode(new int[] { 174, 150, 100, 21, 57, 78, 246, 222, 179, 154, 99, 72, 34, 31, 164, 216, 189, 130, 116, 92, 23, 6, 238, 196, 163, 171, 145, 111, 20, 124, 23, 252, 204, 182, 173, 123, 85, 62, 23, 172, 196, 167, 139, 96, 73, 102, 9, 230, 196, 168, 148, 88, 3, 106, 89, 247, 193, 205, 254, 137, 116, 6, 55, 70, 245, 153, 177, 144, 96, 114, 38, 5, 226, 198, 172, 152, 120, 78, 98, 78, 235, 200, 168, 145, 97, 77, 54, 70, 169, 165, 208, 168, 144, 119, 1 }, 216) +
        UrlObfuscator.decode(new int[] { 159, 105, 85, 102, 7, 240, 205, 255, 143, 117, 115, 82, 113, 14, 166, 151, 232, 212, 117, 69, 104, 16, 252, 209, 164, 157, 106, 64, 57, 66, 250, 223, 172, 154, 126, 117, 32, 8, 230, 193, 181, 143, 141, 95, 81, 48, 83, 189, 219, 173, 131, 98, 90, 58, 95, 243, 157, 139, 157, 97, 65, 41, 86, 232, 220, 188, 147, 105, 75, 25, 79, 235, 207, 176, 170, 138, 70, 72, 34, 10, 252, 133, 164, 131, 119, 89, 58, 6, 204, 151, 230, 213 }, 233) +
        UrlObfuscator.decode(new int[] { 156, 118, 74, 127, 0, 244, 198, 243, 155, 44, 0, 116, 7, 177, 206, 184, 196, 101, 77, 41, 1, 241, 204, 229, 196, 104, 28, 7, 110, 70, 245, 144, 241, 208, 99, 65, 55, 7, 180, 214, 239, 147, 99, 116, 39, 48, 160, 218, 247, 140, 38, 64, 35, 17, 198, 204, 183, 143, 100, 118, 80, 58, 63, 247, 211, 188, 150, 99, 100, 48, 23, 231, 154, 248, 203 }, 250) +
        UrlObfuscator.decode(new int[] { 98, 76, 97, 25, 169, 209, 172, 128, 119, 74, 125, 85, 73, 226, 193, 173, 213, 114, 92, 49, 16, 254, 193, 232, 193, 42, 77, 44, 30, 160, 207, 163, 159, 126, 70, 37, 91, 182, 217, 184, 146, 44, 85, 47, 47, 64, 244, 210, 181, 159, 107, 112, 50, 31, 242, 220, 167, 219, 114, 95, 33, 26, 228, 194, 190, 143, 50 }, 267) +
        UrlObfuscator.decode(new int[] { 106, 90, 40, 89, 235, 138, 177, 144, 96, 112, 61, 28, 224, 218, 186, 136, 104, 120, 62, 16, 228, 194, 238, 128, 45, 15, 33, 83, 189, 236, 144, 191, 157, 120, 81, 62, 10, 248, 195, 187, 144, 80, 93, 61, 31, 253, 149 }, 284) +
        UrlObfuscator.decode(new int[] { 91, 55, 88, 224, 197, 229, 143, 57, 3, 111, 78, 244, 201, 169, 141, 103, 10, 34, 82, 86, 184, 155, 191, 201, 59, 4, 101, 21, 241, 156, 175, 145, 102, 95, 109, 12, 188, 150, 190, 214, 122, 72, 58, 20, 227, 227, 168, 140, 99, 85, 104, 44, 80, 255, 211, 169, 158, 124, 74, 5, 23, 241, 221, 166, 129, 56, 76, 51, 94, 182, 206, 185, 143, 104, 67, 124, 27, 248 }, 50) +
        UrlObfuscator.decode(new int[] { 49, 7, 245, 213, 205, 176, 221, 86, 104, 21, 55, 182, 196, 162, 135, 125, 93, 53, 24, 246, 214, 230, 150, 110, 76, 112, 11, 239, 139, 178, 141, 97, 78, 39, 91, 244, 247, 146, 191, 136, 117, 0, 59, 12, 249, 154, 183, 128, 125, 96, 107, 2, 242, 135, 246 }, 67) +
        UrlObfuscator.decode(new int[] { 41, 16, 243, 197, 179, 135, 38, 72, 101, 16, 248, 204, 188, 146, 116, 75, 100, 68, 249, 220, 231, 228, 131, 96, 21, 115, 83, 162 }, 84);
    // Universal video loading indicator. Any <video> on any page that is
    // still fetching/buffering gets a spinner overlay (fades in, removed once
    // it can play), so the user never stares at a blank/grey box. Pure JS,
    // no per-site config; picks up videos added later too. Overlay is a
    // separate fixed element, so the page's own DOM/layout is untouched.
    // Idempotent (__vidLoaderBound).
    // Records the name a page gives an <a download="..."> link (also for
    // anchors clicked from script) so blob:/data: downloads keep it.
    private static final String DOWNLOAD_NAME_JS =
        UrlObfuscator.decode(new int[] { 77, 226, 214, 172, 130, 116, 118, 81, 51, 84, 178, 193, 173, 138, 110, 77, 60, 18, 187, 197, 184, 158, 107, 65, 58, 66, 212, 245, 173, 132, 73, 71, 40, 1, 193, 205, 180, 142, 155, 55, 79, 57, 15, 239, 203, 182, 204, 97, 92, 58, 23, 253, 198, 254, 176, 81, 73, 32, 37, 235, 196, 173, 165, 105, 80, 42, 7, 191, 213, 178, 170, 155, 38 }, 101) +
        UrlObfuscator.decode(new int[] { 16, 224, 218, 176, 134, 120, 95, 33, 78, 255, 201, 168, 194, 104, 1, 60, 18, 247, 221, 184, 139, 103, 8, 94, 120, 91, 253, 149, 178, 152, 107, 118, 34, 1, 230, 218, 176, 132, 100, 74, 104, 75, 237, 133, 162, 136, 123, 102, 50, 17, 246, 202, 160, 148, 116, 122, 22, 122, 24, 244, 205, 183, 148, 120, 87, 49, 83, 186, 148, 247, 145, 33, 70, 63, 9, 237, 140, 239, 159, 110, 72, 33, 11, 244, 140, 128, 142, 155, 108, 82, 53, 31, 216, 203, 177, 147, 113, 80, 114, 85, 211, 223, 180, 157, 97, 68, 40, 41, 248, 192, 172, 128, 99, 11, 42, 12, 246, 196, 132, 176, 137, 115, 80, 52, 27, 253, 246, 182, 155, 112, 29, 40, 51, 255, 212, 189, 129, 100, 72, 9, 24, 224, 204, 160, 131, 43, 74, 44, 22, 228, 228, 208, 169, 147, 112, 84, 59, 29, 214, 214, 187, 144, 60, 82, 124, 25, 226, 202, 168, 193, 109, 5, 45, 12, 252, 230, 178, 145, 118, 74, 32, 20, 244, 250, 150, 250, 152, 116, 77, 55, 20, 248, 215, 177, 211, 58, 78, 45, 87, 168, 135, 246, 145, 118, 73, 40, 28, 228, 206, 237, 129, 42, 89, 60, 29 }, 118) +
        UrlObfuscator.decode(new int[] { 227, 201, 166, 145, 110, 71, 47, 20, 81, 255, 217, 184, 190, 108, 92, 54, 3, 218, 220, 167, 135, 119, 95, 53, 29, 166, 138, 175, 135, 99, 74, 35, 64, 170, 195, 177, 141, 97, 85, 41, 48, 16, 181, 217, 242, 129, 111, 89, 37, 86, 244, 137, 182, 220, 101, 81, 61, 9, 232, 216, 237, 204, 108, 6, 51, 7, 247, 195, 166, 150, 47, 67, 83, 49, 14, 249, 200, 174, 198, 125, 25, 34, 20, 230, 212, 183, 133, 62, 76, 34, 2, 255, 206, 185, 157, 32, 0, 39, 62, 224, 204, 181, 143, 108, 112, 95, 57, 33, 188, 147, 227, 150, 98, 90, 57, 79, 225, 215, 178, 216, 110, 7, 118, 17, 167, 222, 187, 157, 98, 15, 126 }, 135) +
        UrlObfuscator.decode(new int[] { 238, 214, 164, 213, 123, 80, 111, 57, 196, 226, 130, 172, 98, 72, 34, 6, 250, 226, 170, 128, 105, 70, 44, 21, 174, 239, 204, 178, 136, 116, 78, 32, 8, 242, 152, 182, 152, 122, 81, 58, 75 }, 152) +
        UrlObfuscator.decode(new int[] { 225, 156, 170, 74, 100, 42, 0, 234, 206, 178, 154, 146, 120, 81, 62, 20, 237, 150, 167, 132, 122, 64, 60, 6, 232, 192, 170, 192, 110, 64, 34, 9, 226, 149, 161, 147, 107, 71, 55, 11, 238, 206, 151, 247, 134, 110, 94, 57, 81, 236, 223, 191, 134, 61, 8, 32, 20, 228, 218, 188, 131, 44, 68, 41, 71, 233, 215, 182, 137, 125, 11, 54, 9, 233, 236, 146, 188, 142, 124, 79, 52, 29, 249, 194, 166, 221, 40, 79, 106 }, 169) +
        UrlObfuscator.decode(new int[] { 199, 186, 153, 99, 85, 61, 92, 246, 155, 170, 141, 114, 7, 101, 69, 176 }, 186);
    private static final String VIDEO_LOADER_JS =
        UrlObfuscator.decode(new int[] { 227, 140, 124, 70, 36, 18, 236, 203, 173, 202, 40, 91 }, 203) +
        UrlObfuscator.decode(new int[] { 181, 157, 50, 78, 49, 25, 242, 218, 163, 221, 77, 110, 38, 6, 234, 225, 163, 138, 110, 76, 58, 37, 233, 208, 170, 135, 43, 83, 37, 43, 11, 239, 210, 224, 141, 112, 86, 51, 25, 226, 154, 140, 173, 103, 89, 43, 34, 226, 205, 175, 143, 123, 106, 40, 19, 235, 192, 254, 150, 115, 85, 90, 101 }, 220) +
        UrlObfuscator.decode(new int[] { 155, 109, 89, 106, 27, 233, 193, 251, 213, 63 }, 237) +
        UrlObfuscator.decode(new int[] { 152, 104, 82, 56, 14, 240, 215, 185, 214, 118, 71, 32, 90, 184, 203, 187, 156, 116, 87, 34, 12, 161, 204, 168, 133, 112, 73, 38, 12, 245, 142, 216, 187, 137, 89, 87, 63, 20, 253, 217, 162, 183, 109, 122, 54, 89, 183, 240, 145, 155, 101, 79, 6, 6, 233, 195, 163, 151, 71, 80, 49, 70, 169, 182, 204, 184, 136, 110, 72, 55, 67, 225, 215, 167, 212, 123, 15, 53, 31, 236, 219, 160, 137, 101, 94, 103, 0, 226, 199, 161, 152, 127, 70, 46, 3, 10, 243, 216, 178, 143, 52, 93, 55, 20, 227, 216, 177, 157, 102, 116, 60, 10, 227, 200, 162, 159, 49, 64, 46, 79, 167, 205, 237, 145, 103, 85, 53, 45, 16, 166, 202, 186, 136, 57, 75, 35, 75, 241, 219, 176, 135, 124, 85, 33, 26, 163, 207, 185, 143, 104, 92, 34, 35, 233, 193, 174, 135, 111, 84, 23, 121, 14, 232, 194, 182, 156, 63, 30, 109, 6, 224, 157, 187, 149, 45, 8, 17, 50, 250, 194, 174, 165, 103, 70, 34, 0, 246, 224, 177, 146, 39, 36 }, 254) +
        "st.textContent='@keyframes __vlspin{to{transform:rotate(360deg)}}.__vl{position:fixed;z-index:2147483000;pointer-events:none;display:none;align-items:center;justify-content:center;background:rgba(8,12,28,.6);transition:opacity .25s ease;opacity:0;box-sizing:border-box}.__vl.on{opacity:1}.__vl i{width:38px;height:38px;border-radius:50%;border:3px solid rgba(255,255,255,.25);border-top-color:#fff;animation:__vlspin .8s linear infinite;box-sizing:border-box}';" +
        UrlObfuscator.decode(new int[] { 103, 0, 44, 28, 251, 207, 167, 140, 68, 78, 44, 8, 231, 138, 178, 148, 214, 37, 64, 63, 26, 238, 218, 176, 223, 115, 28, 47, 14, 239 }, 271) +
        UrlObfuscator.decode(new int[] { 70, 74, 48, 30, 232, 210, 181, 151, 56, 89, 51, 16, 240, 192, 250, 135, 57, 84, 58, 31, 245, 208 }, 288) +
        UrlObfuscator.decode(new int[] { 95, 51, 92, 229, 156, 180, 130, 125, 65, 63, 16, 247, 220, 231, 154, 98, 71, 33, 29, 208, 214, 160, 148, 154, 32, 0, 111, 7, 230, 207, 246, 153, 115, 65, 35, 28, 224, 218, 131, 155, 111, 89, 41, 86, 183, 148, 251, 206, 116, 64, 48, 22, 240, 207, 224, 185, 159, 113, 79, 62, 65 }, 54) +
        UrlObfuscator.decode(new int[] { 46, 0, 173, 133, 181, 204, 98, 85, 77, 44, 24, 242, 207, 137, 139, 123, 17, 112, 84, 226, 157, 181, 148, 100, 110, 58, 25, 254, 194, 168, 156, 124, 66, 110, 66, 247, 209, 161, 198, 41, 57, 24, 124, 10, 181, 203, 172, 157, 101, 79, 6, 17, 255, 215, 178, 132, 96, 92, 101, 75, 248, 197, 188, 154, 100, 67, 98, 77, 170, 208, 164, 148, 138, 108, 83, 124, 29, 251, 213, 171, 146, 45 }, 71) +
        UrlObfuscator.decode(new int[] { 42, 18, 226, 192, 166, 157, 50, 71, 126, 1, 235, 217, 187, 132, 120, 66, 27, 19, 231, 209, 161, 222, 63, 28, 114, 35, 2, 235, 146, 168, 159, 124, 83, 62, 24, 242, 200, 175, 211, 103, 30, 63, 15, 248, 223, 174, 142, 117, 84, 49, 72, 228, 209, 183, 141, 113, 76, 94, 39, 1, 224, 154, 251, 143, 54, 104, 9, 3, 248, 228, 179, 152, 100, 20 }, 88) +
        UrlObfuscator.decode(new int[] { 20, 235, 198, 178, 134, 108, 11, 39, 72, 251, 237, 219, 169, 137, 105, 84, 121, 30, 246, 218, 166, 145, 40, 79, 44 }, 105) +
        UrlObfuscator.decode(new int[] { 28, 236, 214, 180, 130, 124, 91, 61, 82, 249, 217, 171, 139, 37, 67, 98, 17, 230, 134, 164, 138, 100, 87, 48, 46, 232, 211, 203, 240, 143, 121, 86, 53, 15, 253, 159, 241, 154, 122, 20, 123, 74, 227, 202, 186, 185, 101, 70, 47, 6, 253, 211, 238, 131, 113, 77, 33, 21, 233, 240, 208, 245, 213, 96, 83, 63, 80, 182, 217, 251, 151, 127, 83, 34, 3, 195, 199, 190, 152, 37, 73, 38, 6, 243, 199, 172, 138, 112, 10, 102, 15, 17, 185, 148, 245, 148, 52, 74, 44, 14, 250, 208, 250, 151, 123, 66, 32, 3, 239, 212, 241, 204, 100, 70, 38, 2, 161, 158, 185, 207, 48, 23, 112, 118, 69, 224 }, 122) +
        UrlObfuscator.decode(new int[] { 237, 223, 167, 139, 115, 79, 42, 10, 163, 214, 168, 131, 148, 54, 20, 39, 24, 233, 202, 240, 222, 45, 67, 53, 1, 178, 199, 163, 210, 106, 66, 47, 30, 231, 204, 166, 147, 40, 66, 33, 23, 199, 205, 165, 178, 155, 115, 72, 40, 56, 224, 236, 182, 145, 91, 85, 62, 23, 185, 151, 185, 135, 105, 73, 36, 77, 160, 147, 177, 135, 119, 4, 34, 1, 245, 201, 201, 187, 192, 44, 0 }, 139) +
        UrlObfuscator.decode(new int[] { 250, 212, 168, 209, 110, 86, 36, 85, 253, 142, 226, 202, 121, 19, 56, 30, 162, 199, 175, 135, 111, 83, 46, 94, 237, 136, 233, 200, 123, 105, 95, 47, 92, 237, 135, 175, 139, 76, 95, 8, 79, 229, 211, 163, 208, 96, 19, 59, 66, 212, 245, 191, 132, 60, 80, 36, 22, 163, 208, 252, 150, 209, 121, 88, 40, 57, 245, 204, 182, 147, 127, 91, 51, 48, 254, 216, 181, 129, 122, 127, 41, 8, 254, 129, 225, 220, 112, 68, 54, 67, 225, 210, 253, 184, 155, 105, 127, 52, 23, 233, 205, 163, 147, 113, 103, 39, 11, 253, 213, 231, 152, 36, 23 }, 156) +
        UrlObfuscator.decode(new int[] { 219, 173, 153, 42, 90, 32, 8, 241, 152, 170, 134, 103, 69, 51, 119, 8, 180, 154, 253, 136, 55, 79, 62, 18, 225, 220, 237, 207, 39, 0, 105, 72, 255, 130, 163, 143, 96, 79, 47, 18, 187, 153, 245, 210, 39, 6, 77, 112, 31, 243, 207, 174, 150, 117, 9, 102, 83, 178, 193, 252, 133, 127, 95, 114, 4, 226, 197, 175, 155, 64, 66, 47, 2, 236, 215, 228, 199, 114, 49, 76, 52, 27, 243, 206, 231, 200, 49, 16, 39, 90, 255, 215, 183, 132, 51, 71, 35, 2, 238, 216, 158, 129, 99, 82, 45, 66, 165, 193, 178, 206, 137, 119, 78, 53, 25, 243, 213, 177, 131, 111, 20, 105, 78, 181, 217, 185, 139, 106, 72, 34, 76, 172, 143, 171, 148, 40, 65, 45, 16, 242, 205, 161, 166, 223, 32, 1, 124, 20, 246, 214, 178, 209, 46 }, 173) +
        UrlObfuscator.decode(new int[] { 215, 187, 212, 104, 82, 54, 15, 190, 205 }, 190) +
        UrlObfuscator.decode(new int[] { 166, 136, 37, 13, 36, 67, 242, 199, 250, 130, 106, 71, 54, 15, 228, 206, 203, 240, 158, 110, 94, 59, 13, 253, 242, 186, 144, 121, 86, 60, 5, 184, 136, 170, 132, 122, 12, 99, 82, 231, 137, 165, 137, 101, 80, 49, 47, 225, 242, 219, 224, 219, 68, 101, 47, 20, 176, 141, 186, 218, 114, 66, 33, 21, 225, 202, 142, 132, 98, 70, 45, 64, 227, 201, 166, 145, 110, 71, 47, 20, 81, 253, 207, 185, 154, 110, 92, 29, 27, 243, 216, 177, 157, 102, 25, 119, 6, 169, 132, 229, 208, 34, 77, 39, 4, 243, 200, 161, 141, 118, 15, 34, 48, 26, 228, 192, 167, 158, 118, 91, 34, 27, 240, 218, 167, 220, 117, 95, 44, 27, 224, 201, 165, 158, 76, 68, 34, 11, 224, 202, 183, 203, 47, 65, 79, 46, 24, 242, 223, 153, 145, 113, 91, 50, 93, 251, 154, 233, 135, 62, 112, 17, 27, 224, 150, 165, 210, 117 }, 207) +
        UrlObfuscator.decode(new int[] { 150, 158, 108, 29, 43, 26, 233, 132, 183, 217, 101, 65, 45, 31, 247, 159, 180, 134, 125, 93, 32, 10, 243, 148, 245, 218, 33, 67, 40, 6, 250, 134, 251 }, 224) +
        UrlObfuscator.decode(new int[] { 158, 62, 92, 58, 20, 224, 206, 228, 141, 97, 84, 54, 9, 229, 218, 255, 198, 102, 115, 91, 37, 91, 160, 213, 247, 139, 99, 79, 57, 17, 189, 222, 180, 150, 123, 19, 63, 66, 231, 207, 175, 156, 44, 1, 53, 28, 164, 153, 174, 206, 140, 106, 68, 48, 30, 180, 205, 183, 135, 43, 71, 122, 7, 253, 193, 251, 200, 126, 85, 107, 80, 229, 135, 187, 147, 127, 73, 33, 77, 245, 200, 164, 171, 150, 32, 78, 117, 13, 240, 220, 163, 158, 62, 19, 35, 10, 182, 139, 160, 192, 126, 88, 50, 6, 236, 134, 175, 131, 108, 67, 43, 22, 188, 210, 145, 182, 152, 117, 92, 50, 13, 179, 144, 166, 141, 51, 8, 61, 95, 227, 219, 183, 129, 105, 5, 40, 6, 250, 195, 163, 151, 86, 66, 38, 8, 245, 236, 131, 190, 143, 53, 88, 54, 10, 243, 211, 167, 166, 114, 86, 56, 5, 252, 149 }, 241) +
        UrlObfuscator.decode(new int[] { 107, 71, 104, 126, 9, 252, 207, 242, 129, 49, 94, 34, 24, 246, 192, 186, 157, 127, 24, 55, 71, 246, 222, 174, 155, 124, 77, 52, 18, 196, 202, 170, 143, 96, 84, 86, 49, 19, 218, 201, 187, 148, 125, 31, 48, 0, 250, 208, 166, 152, 127, 65, 102, 68, 247, 211, 228, 138, 100, 70, 53, 22, 200, 202, 177, 149, 46, 126, 90, 57, 84, 188, 213, 183, 223, 62, 13, 40, 93, 168, 207, 248, 216, 96, 7, 118, 17, 238, 198, 186, 141, 124, 73, 107, 7, 239, 195, 178, 147, 179, 119, 78, 40, 85, 251, 221, 188, 223, 49, 90, 58, 84, 187, 138, 173 }, 258) +
        UrlObfuscator.decode(new int[] { 114, 81, 37, 25, 249, 203, 230, 199, 48 }, 275) +
        UrlObfuscator.decode(new int[] { 89, 38, 14, 242, 197, 159, 183, 155, 52, 84, 124, 95, 247, 153, 165, 129, 109, 95, 55, 95, 244, 198, 189, 157, 96, 74, 51, 72, 181, 154, 225, 139, 107, 77, 39, 70, 166, 185, 209, 243, 159, 119, 91, 42, 11, 219, 223, 166, 128, 61, 81, 62, 30, 251, 207, 164, 130, 120, 2, 110, 7, 233, 129, 236, 205, 120, 74, 40, 4, 26, 182, 210, 245, 192, 103 }, 292) +
        "}" +
        UrlObfuscator.decode(new int[] { 72, 60, 12, 226, 196, 187, 212, 114, 81, 37, 25, 249, 203, 246, 145 }, 58) +
        UrlObfuscator.decode(new int[] { 45, 31, 231, 203, 179, 143, 106, 74, 99, 4, 243, 193, 210, 187, 213, 53, 64, 40, 24, 254, 138, 230, 206, 96, 65, 43, 10, 249, 201, 230, 153, 101, 72, 33, 65, 161, 153, 246, 204, 118, 66, 36, 92, 242, 250, 207, 168, 153, 104, 78, 24, 22, 254, 219, 180, 128, 122, 93, 63, 54, 253, 207, 160, 137, 35, 76, 59, 9, 234, 195, 236, 223, 126, 65, 32, 20, 28, 246, 149, 185, 210, 97, 68, 37 }, 75) +
        UrlObfuscator.decode(new int[] { 58, 14, 244, 218, 172, 158, 121, 91, 116, 4, 243, 216, 164, 199, 107, 4, 55, 2, 236, 129, 173, 201, 114, 68, 54, 4, 231, 213, 230, 249, 155, 51, 72, 58, 8, 254, 221, 163, 216, 97, 85, 52, 60, 240, 221, 170, 211, 48, 17, 108, 60, 192, 236, 130, 169, 34, 13, 38, 76, 245, 193, 205, 185, 152, 104, 21, 5, 38, 238, 219, 129, 148, 125, 71, 111, 5, 226, 218, 171, 214, 113 }, 92) +
        UrlObfuscator.decode(new int[] { 11, 249, 197, 169, 157, 97, 72, 40, 69, 235, 200, 234, 132, 41, 100, 87, 59, 84, 254, 148, 173, 153, 101, 81, 48, 0, 181, 148, 180, 222, 123, 79, 63, 11, 238, 222, 231, 156, 102, 65, 11, 5, 238, 199, 252, 221, 194, 57, 107, 21, 63, 223, 246, 255, 222, 115, 27, 32, 18, 224, 214, 181, 155, 32, 114, 19, 29, 230, 254, 169, 142, 114, 24, 34, 2, 238, 210, 165, 228, 131 }, 109) +
        UrlObfuscator.decode(new int[] { 37, 186, 203, 186, 147, 109, 81, 57, 17, 178, 152, 244, 129, 101, 81, 35, 2, 232, 200, 236, 198, 46, 91, 34, 3, 238, 205, 173, 133, 38, 125, 17, 56, 18, 238, 254, 187, 154, 112, 31, 48, 0, 250, 208, 166, 152, 127, 65, 102, 25, 165, 208, 174, 134, 107, 82, 43, 0, 234, 215, 236, 128, 100, 123, 123, 43, 25, 245, 206, 149, 145, 100, 66, 48, 26, 246, 192, 249, 132, 35, 89, 44, 5, 255, 134, 189, 154, 114, 67, 108, 95, 254, 139, 250 }, 126) +
        UrlObfuscator.decode(new int[] { 212, 137, 189, 128, 106, 83, 32, 6, 224, 129, 233, 195, 96, 67, 47, 16, 19, 255, 196, 251, 215, 61, 90, 57, 25, 230, 217, 181, 138, 102, 89, 34, 0, 251, 202, 164, 204, 38, 14, 36, 8, 231, 193, 161, 135, 102, 64, 52, 62, 89, 177, 155, 190, 136, 107, 87, 37, 81, 185, 147, 182, 159, 97, 68, 38, 11, 233, 139, 231, 205, 104, 74, 40, 20, 241, 131, 158, 204, 103, 79, 77, 27, 28, 255, 211, 242, 159, 109, 89, 53, 1, 253, 220, 188, 217, 100, 6, 53, 9, 227, 200, 191, 132, 109, 73, 50, 75, 229, 199, 166, 164, 118, 122, 80, 41, 48, 242, 201, 173, 157, 121, 83, 39, 92, 231, 158, 190, 155, 35, 90, 63, 25, 238, 131, 242, 149, 46, 29 }, 143) +
        UrlObfuscator.decode(new int[] { 211, 218, 170, 180, 114, 79, 63, 11, 238, 214, 186, 221, 114, 70, 60, 18, 228, 198, 161, 131, 36, 2, 49, 0, 238, 143, 231, 151, 101, 69, 107, 7, 242, 254, 211, 184, 212, 50, 1, 36, 84, 165, 131, 229, 221, 40 }, 160) +
        UrlObfuscator.decode(new int[] { 204, 249, 199, 39, 22 }, 177);
    // Best-effort polyfill for the File System Access API
    // (window.showOpenFilePicker), shared by both injection paths below.
    // Plain android.webkit.WebView has never implemented this API (it's
    // Chrome-desktop/modern-mobile-Chrome only -- see caniuse/MDN), so any
    // site written against it -- rather than the older
    // <input type="file"> + .click() pattern -- just throws
    // "showOpenFilePicker is not a function" the moment its own upload
    // button is tapped, which looks to the user exactly like the button
    // silently doing nothing. This reroutes that call through a real
    // hidden <input type=file> instead, which
    // WebChromeClient.onShowFileChooser below already knows how to handle.
    private static final String SHOW_OPEN_FILE_PICKER_POLYFILL =
        UrlObfuscator.decode(new int[] { 234, 135, 117, 113, 93, 41, 21, 244, 212, 241, 209, 108 }, 194) +
        UrlObfuscator.decode(new int[] { 186, 148, 57, 71, 38, 0, 233, 195, 188, 196, 122, 64, 40, 17, 202, 212, 166, 140, 71, 73, 83, 59, 45, 245, 216, 177, 156, 106, 30, 36, 16, 224, 198, 160, 159, 43 }, 211) +
        UrlObfuscator.decode(new int[] { 147, 106, 76, 37, 15, 8, 176, 206, 180, 148, 109, 118, 40, 18, 248, 243, 189, 159, 119, 97, 57, 12, 229, 200, 190, 214, 108, 92, 38, 4, 242, 204, 171, 141, 42, 78, 48, 43, 13, 180, 199 }, 228) +
        UrlObfuscator.decode(new int[] { 154, 100, 71, 33, 76, 255, 223, 186, 158, 112, 87, 49, 20, 179 }, 245) +
        UrlObfuscator.decode(new int[] { 116, 64, 48, 22, 240, 207, 224, 177, 155, 106, 28, 11, 8, 246, 213, 190, 133, 112, 28, 53, 7, 255, 211, 187, 135, 98, 66, 99, 24, 236, 219, 168, 138, 115, 65, 111, 16, 228, 202, 218, 189, 137, 53, 64 }, 262) +
        UrlObfuscator.decode(new int[] { 97, 87, 39, 84, 250, 220, 161, 133, 123, 19, 41, 3, 232, 223, 164, 141, 105, 82, 107, 7, 241, 199, 160, 148, 154, 91, 81, 57, 22, 255, 215, 172, 223, 49, 92, 58, 3, 231, 197, 247, 198, 53 }, 279) +
        UrlObfuscator.decode(new int[] { 65, 41, 22, 240, 208, 237, 150, 120, 80, 90, 99, 90, 250, 210, 182, 156, 63, 12 }, 296) +
        UrlObfuscator.decode(new int[] { 87, 59, 84, 244, 202, 173, 139, 57, 91, 32, 24, 231, 219, 161, 156, 106, 7, 36, 2, 251, 223, 189, 198, 106, 83, 41, 16, 234, 210, 173, 133, 194, 106, 79, 41, 30, 161 }, 62) +
        UrlObfuscator.decode(new int[] { 59, 28, 244, 215, 189, 139, 123, 8, 34, 30, 241, 215, 254, 185, 92, 27, 23, 49, 13, 232, 200, 244, 141, 97, 71, 51, 6, 232, 207, 137, 172, 57, 1, 40, 2, 254, 238, 171, 138, 96, 15, 32, 16, 234, 192, 182, 136, 111, 113, 22, 41, 85, 224 }, 79) +
        UrlObfuscator.decode(new int[] { 22, 30, 236, 157, 189, 198, 110, 31, 126, 3, 184, 212, 183, 144, 119, 65, 36, 84, 231, 203, 228, 202, 107, 0, 58, 2, 242, 208, 182, 141, 57 }, 96) +
        UrlObfuscator.decode(new int[] { 62, 242, 197, 171, 142, 120, 5, 33, 12, 241, 212, 238, 132, 45, 13, 36, 14, 242, 218, 223, 190, 148, 51, 92, 44, 22, 244, 194, 188, 155, 125, 26, 60, 25, 226, 203, 228, 151, 110, 82, 61, 27, 169, 214, 176, 151, 107, 10, 44, 9, 18, 251, 148, 231 }, 113) +
        UrlObfuscator.decode(new int[] { 170, 192, 155, 178, 151, 112, 89, 6, 6, 229, 227, 138, 223, 59, 82, 60, 0, 212, 209, 172, 134, 37, 74, 62, 4, 234, 220, 174, 137, 107, 12, 38, 26, 245, 137, 196, 187, 133, 104, 72, 116, 9, 237, 196, 190, 221, 113, 75, 38, 88, 171, 210, 231, 214, 113, 2, 113, 20, 161, 156 }, 130) +
        UrlObfuscator.decode(new int[] { 250, 212, 249, 149, 119, 90, 62, 66, 231, 207, 167, 143, 115, 78, 108, 13, 237, 210, 180, 148, 209, 127, 94, 63, 30, 234, 205, 229, 146, 110, 65, 39, 93, 248, 222, 185, 129, 38, 10, 96, 76, 163, 146, 181, 132, 103, 81, 39, 11, 170, 196, 233, 164, 131 }, 147) +
        UrlObfuscator.decode(new int[] { 205, 173, 146, 116, 84, 17, 45, 9, 229, 215, 191, 215, 124, 94, 37, 5, 248, 210, 171, 204, 55, 65, 33, 3, 233, 140, 241 }, 164) +
        UrlObfuscator.decode(new int[] { 209, 187, 144, 103, 92, 53, 1, 250, 131, 174, 132, 110, 80, 102, 6, 246, 213, 161, 141, 102, 98, 40, 54, 18, 249, 148, 178, 148, 105, 77, 35, 95, 174 }, 181) +
        UrlObfuscator.decode(new int[] { 175, 139, 116, 86, 54, 79, 225, 251, 218, 152, 138, 126, 84, 45, 52, 254, 197, 161, 145, 125, 87, 35, 88, 168, 205, 165, 141, 101, 77, 44, 79, 171, 192, 176, 138, 96, 86, 40, 15, 17, 182, 148, 167 }, 198) +
        UrlObfuscator.decode(new int[] { 163, 132, 108, 79, 55, 29, 242, 197, 162, 139, 99, 88, 101, 8, 230, 204, 190, 200, 119, 65, 46, 13, 247, 197, 252, 182, 148, 112, 95, 114, 16, 246, 199, 163, 129, 61, 8, 47, 18, 241, 219, 173, 133, 36, 78, 99, 18, 245 }, 215) +
        UrlObfuscator.decode(new int[] { 129, 97, 14, 100, 13, 237, 210, 180, 148, 209, 120, 84, 48, 30, 233, 197, 164, 214, 127, 91, 36, 6, 230, 159, 182, 134, 98, 72, 63, 69, 230, 204, 166, 128, 114, 77, 109, 24 }, 232) +
        UrlObfuscator.decode(new int[] { 143, 121, 69, 118, 16, 230, 193, 233, 133, 98, 86, 53, 8, 254, 217, 247, 135, 109, 80, 102, 33, 203, 238, 135, 153, 99, 122, 78, 41, 21, 244, 212, 241, 223, 67, 94, 48, 84, 230, 193, 180, 130, 47, 79, 47, 3, 249, 222, 172, 140, 39, 71, 101, 22, 230, 211, 180, 133, 140, 106, 19, 123, 87, 189, 248, 186, 152, 100, 65, 17, 1, 224, 222, 162, 200, 39, 22, 49 }, 249) +
        UrlObfuscator.decode(new int[] { 105, 72, 60, 4, 238, 141, 161, 202, 121, 68, 50, 45, 67, 243, 217, 172, 218, 92, 74, 37, 25, 231, 156, 244, 179, 115, 95, 61, 26, 200, 222, 185, 133, 123, 15, 110, 93, 248 }, 266) +
        UrlObfuscator.decode(new int[] { 105, 95, 51, 29, 244, 194, 253, 145, 97, 64, 120, 75, 253, 203, 185, 153, 121, 68, 114, 21 }, 283) +
        UrlObfuscator.decode(new int[] { 71, 49, 29, 174, 197, 173, 133, 110, 69, 45, 20, 187, 228, 182, 145, 99, 88, 110, 47, 12, 242, 200, 180, 142, 96, 72, 50, 88, 248, 213, 163, 220, 114, 81, 35, 2, 165, 197, 165, 154, 124, 92, 105, 0, 236, 200, 166, 145, 45, 70, 74, 48, 30, 232, 210, 181, 151, 48, 81, 63, 25, 241, 154, 169 }, 49) +
        UrlObfuscator.decode(new int[] { 48, 4, 244, 234, 204, 179, 135, 112, 83, 55, 28, 173, 145, 179, 157, 127, 87, 118, 92, 225, 207, 160, 137, 49, 76, 32, 4, 226, 136, 171, 133, 110, 71, 109, 7, 26, 234, 251, 181, 151, 127, 3, 62, 2, 248, 214, 160, 154, 125, 95, 120, 70, 245, 223, 169, 159, 127, 91, 38, 71, 214, 215, 171, 142, 107, 82, 37, 113, 12, 248, 207, 180, 150, 111, 93, 127, 16, 252, 216, 182, 219, 42, 77, 50, 85, 240, 133, 240 }, 66) +
        UrlObfuscator.decode(new int[] { 33, 23, 226, 223, 163, 152, 104, 4, 36, 26, 253, 219, 233, 139, 112, 72, 55, 11, 241, 204, 218, 225, 149, 125, 85, 62, 21, 253, 196, 236, 174, 124, 82, 60, 21, 252, 202, 189, 182, 60, 118, 23, 64, 179, 218, 239, 222 }, 83) +
        UrlObfuscator.decode(new int[] { 13, 237, 210, 180, 148, 209, 125, 81, 53, 24, 241, 145, 241, 204, 107, 28, 111, 14, 169 }, 100) +
        UrlObfuscator.decode(new int[] { 8, 188, 154, 251 }, 117);
    private String[] pendingDownload;
    // Field (not a local in onCreate) so onNewIntent -- fired for
    // shortcut taps / deep links / shares while the app's already running,
    // via the singleTask launch mode set in the manifest -- can act on the
    // same WebView instance instead of only being able to touch it from
    // inside onCreate.
    private WebView webView;
    private SwipeRefreshLayout swipeRefresh;
    // Safe-area plumbing (see applySafeArea / paintSystemBarScrims below).
    // The window is edge-to-edge; the WebView is inset by the system bars,
    // display cutout and on-screen keyboard, and these views paint the
    // status/navigation bar strips so they always match the page.
    private FrameLayout rootLayout;
    private View topScrim;
    private View bottomScrim;
    // Keyboard (IME) tracking -- see the insets listener in onCreate and
    // scrollFocusedFieldIntoView() below.
    private boolean imeWasVisible = false;
    private int lastImeBottom = 0;
    private final Runnable scrollFocusedFieldRunnable = new Runnable() {
        @Override public void run() { scrollFocusedFieldIntoView(); }
    };
    // Non-null once the page declares <meta name="theme-color">; then the
    // bar strips use that color instead of the page background.
    private Integer themeColorOverride = null;
    // Updated by AndroidBridge.reportScrollTop() below, driven by a
    // capture-phase JS scroll listener -- see the SwipeRefreshLayout
    // override above for why this exists instead of using the WebView's
    // own getScrollY().
    private volatile int lastKnownScrollTop = 0;
    // Whether the CURRENT drag started within the top "pull zone" (see
    // canChildScrollUp() override below). scrollTop alone can't tell a
    // page genuinely at its top apart from a dialog/sheet that just
    // opened and also happens to read scrollTop 0 -- a drag anywhere
    // inside that dialog would otherwise get read as "at the top, so
    // this must be a refresh pull" even though it's nowhere near the
    // actual top of the screen. Gating on where the gesture *started*
    // fixes that without needing the page to know anything about it.
    private volatile boolean lastTouchInPullZone = true;
    private Vibrator vibrator;

    // The WebView's own in-progress camera/mic request (e.g. a QR scanner
    // or a video-chat widget using getUserMedia) while we go ask Android
    // for the runtime CAMERA/RECORD_AUDIO permission(s) -- resumed in
    // onRequestPermissionsResult once that answer comes back, see
    // onPermissionRequest below.
    private PermissionRequest pendingWebPermissionRequest;
    // Same idea for a page calling navigator.geolocation.getCurrentPosition/
    // watchPosition -- WebView surfaces that as
    // onGeolocationPermissionsShowPrompt rather than onPermissionRequest,
    // with its own callback type, so it needs its own pending pair instead
    // of reusing pendingWebPermissionRequest above.
    private String pendingGeoOrigin;
    private GeolocationPermissions.Callback pendingGeoCallback;

    // Used to auto-dismiss the offline screen the instant the OS reports a
    // connection is back, instead of making the user tap "retry" themselves.
    private ConnectivityManager connectivityManager;
    private ConnectivityManager.NetworkCallback networkCallback;
    // Explicitly re-triggers a media scan on every completed download (see
    // handleDownloadComplete below) -- MIUI/HyperOS (Xiaomi/POCO/Redmi) in
    // particular is known not to index files that a third-party app saved
    // to the shared Downloads folder via DownloadManager: the file is
    // genuinely on disk, it just never appears in their own Downloads/file
    // manager UI until something explicitly asks the OS to scan it.
    private BroadcastReceiver downloadCompleteReceiver;
    // The ID of the download THIS app most recently started, or -1 if none
    // is in flight. DownloadManager.ACTION_DOWNLOAD_COMPLETE is a system-
    // wide broadcast -- it fires for every download completing anywhere on
    // the device through the shared DownloadManager service, not just this
    // app's own. Without checking the completed download's ID against this
    // field, downloadCompleteReceiver would call notifyDownloadResult(true)
    // for ANY finished download (another app's, or a stray leftover from an
    // earlier tap), instantly marking the page's button "Downloaded" while
    // the actual file the user just requested was still genuinely
    // downloading in the system Download Manager -- this is what was fixed.
    private long pendingDownloadId = -1;

    // Tiny custom-drawn glyph for the offline screen: a signal dot with two
    // fading arcs above it, struck through -- avoids needing a drawable
    // resource just for one icon.
    // Flat, clean sad cat that peeks over the top edge of the exit dialog
    // (fits the "neko" theme). Drawn in a 100 x 64 unit box; the box's bottom
    // edge sits on the card's top edge, so the lower part of the head and the
    // paws are what rest on the card.
    private static class SadMascotView extends View {
        private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint line = new Paint(Paint.ANTI_ALIAS_FLAG);
        SadMascotView(Context context) {
            super(context);
            fill.setStyle(Paint.Style.FILL);
            line.setStyle(Paint.Style.STROKE);
            line.setStrokeCap(Paint.Cap.ROUND);
            line.setStrokeJoin(Paint.Join.ROUND);
            line.setColor(Color.parseColor(UrlObfuscator.decode(new int[] { 165, 149, 134, 210, 53, 21, 112 }, 134)));
        }

        private void shape(Canvas c, android.graphics.Path path, int color) {
            fill.setColor(color);
            c.drawPath(path, fill);
            c.drawPath(path, line);
        }

        @Override
        protected void onDraw(Canvas c) {
            float s = getWidth() / 100f;
            if (s <= 0) return;
            c.save();
            c.scale(s, s);
            int fur = Color.parseColor(UrlObfuscator.decode(new int[] { 180, 240, 225, 178, 38, 116, 19 }, 151));
            int pink = Color.parseColor(UrlObfuscator.decode(new int[] { 139, 129, 210, 68, 23, 0, 82 }, 168));
            line.setStrokeWidth(1.8f);

            android.graphics.Path earL = new android.graphics.Path();
            earL.moveTo(23, 36); earL.lineTo(26, 5); earL.lineTo(47, 20); earL.close();
            shape(c, earL, fur);
            android.graphics.Path earR = new android.graphics.Path();
            earR.moveTo(77, 36); earR.lineTo(74, 5); earR.lineTo(53, 20); earR.close();
            shape(c, earR, fur);
            android.graphics.Path inL = new android.graphics.Path();
            inL.moveTo(29, 28); inL.lineTo(30, 13); inL.lineTo(40, 21); inL.close();
            fill.setColor(pink); c.drawPath(inL, fill);
            android.graphics.Path inR = new android.graphics.Path();
            inR.moveTo(71, 28); inR.lineTo(70, 13); inR.lineTo(60, 21); inR.close();
            fill.setColor(pink); c.drawPath(inR, fill);

            android.graphics.Path head = new android.graphics.Path();
            head.addOval(new RectF(20, 18, 80, 72), android.graphics.Path.Direction.CW);
            shape(c, head, fur);

            fill.setColor(pink); c.drawPath(inL, fill);
            fill.setColor(pink); c.drawPath(inR, fill);

            line.setStrokeWidth(1.2f);
            line.setColor(Color.parseColor(UrlObfuscator.decode(new int[] { 154, 153, 178, 84, 13, 17, 67 }, 185)));
            c.drawLine(22, 47, 9, 44, line);
            c.drawLine(22, 52, 9, 53, line);
            c.drawLine(78, 47, 91, 44, line);
            c.drawLine(78, 52, 91, 53, line);
            line.setColor(Color.parseColor(UrlObfuscator.decode(new int[] { 233, 217, 74, 22, 113, 81, 180 }, 202)));

            fill.setColor(Color.parseColor(UrlObfuscator.decode(new int[] { 248, 203, 45, 10, 99, 67, 214 }, 219)));
            c.drawOval(new RectF(33.8f, 36.8f, 42.2f, 47.2f), fill);
            c.drawOval(new RectF(57.8f, 36.8f, 66.2f, 47.2f), fill);
            fill.setColor(Color.WHITE);
            c.drawCircle(36.8f, 40.4f, 1.7f, fill);
            c.drawCircle(60.8f, 40.4f, 1.7f, fill);
            c.drawCircle(39.6f, 44.4f, 0.9f, fill);
            c.drawCircle(63.6f, 44.4f, 0.9f, fill);

            line.setStrokeWidth(2.2f);
            c.drawLine(30.5f, 36.5f, 43.5f, 32.5f, line);
            c.drawLine(56.5f, 32.5f, 69.5f, 36.5f, line);

            line.setStrokeWidth(1.4f);
            android.graphics.Path tear = new android.graphics.Path();
            tear.moveTo(35.5f, 49f);
            tear.quadTo(31.5f, 55f, 35.5f, 58f);
            tear.quadTo(39.5f, 55f, 35.5f, 49f);
            shape(c, tear, Color.parseColor(UrlObfuscator.decode(new int[] { 207, 51, 111, 13, 80, 193, 158 }, 236)));

            android.graphics.Path nose = new android.graphics.Path();
            nose.moveTo(47.5f, 48f); nose.lineTo(52.5f, 48f); nose.lineTo(50f, 51f); nose.close();
            fill.setColor(Color.parseColor(UrlObfuscator.decode(new int[] { 222, 89, 3, 109, 76, 161, 242 }, 253))); c.drawPath(nose, fill);

            line.setStrokeWidth(1.8f);
            android.graphics.Path mouth = new android.graphics.Path();
            mouth.moveTo(50, 51); mouth.lineTo(50, 53.5f);
            mouth.moveTo(43.5f, 59f); mouth.quadTo(47f, 53.5f, 50, 53.5f);
            mouth.quadTo(53f, 53.5f, 56.5f, 59f);
            c.drawPath(mouth, line);

            line.setStrokeWidth(1.8f);
            for (int i = 0; i < 2; i++) {
                float px = i == 0 ? 31f : 69f;
                android.graphics.Path paw = new android.graphics.Path();
                paw.addOval(new RectF(px - 8f, 57f, px + 8f, 69f), android.graphics.Path.Direction.CW);
                shape(c, paw, fur);
                line.setStrokeWidth(1.2f);
                c.drawLine(px - 2.6f, 60f, px - 2.6f, 63.5f, line);
                c.drawLine(px + 2.6f, 60f, px + 2.6f, 63.5f, line);
                line.setStrokeWidth(1.8f);
            }
            c.restore();
        }
    }

    private static class SignalOffIcon extends View {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

        SignalOffIcon(Context context) {
            super(context);
            paint.setStrokeCap(Paint.Cap.ROUND);
            paint.setStrokeJoin(Paint.Join.ROUND);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            float w = getWidth();
            float h = getHeight();
            float cx = w / 2f;
            float cy = h * 0.64f;
            float stroke = Math.max(w * 0.09f, 3f);

            paint.setStyle(Paint.Style.FILL);
            paint.setColor(Color.parseColor(UrlObfuscator.decode(new int[] { 45, 107, 126, 45, 184, 236, 141 }, 270)));
            canvas.drawCircle(cx, cy, stroke * 1.05f, paint);

            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(stroke);
            paint.setColor(Color.parseColor(UrlObfuscator.decode(new int[] { 60, 88, 59, 26, 253, 220, 191 }, 287)));
            float[] radii = {w * 0.24f, w * 0.38f};
            int[] alphas = {235, 130};
            for (int i = 0; i < radii.length; i++) {
                paint.setAlpha(alphas[i]);
                RectF arc = new RectF(cx - radii[i], cy - radii[i], cx + radii[i], cy + radii[i]);
                canvas.drawArc(arc, 208, 124, false, paint);
            }

            paint.setAlpha(255);
            paint.setColor(Color.parseColor(UrlObfuscator.decode(new int[] { 22, 18, 53, 164, 243, 229, 216 }, 53)));
            paint.setStrokeWidth(stroke * 1.1f);
            float pad = w * 0.14f;
            canvas.drawLine(pad, pad, w - pad, h - pad, paint);
        }
    }

    // Compact three-dot "bouncing" loading indicator for the nav-loading
    // overlay (see navOverlay in onCreate) -- each dot bounces up and back
    // down on a loop, staggered so they ripple left-to-right rather than
    // moving in lockstep. Sized to sit centered as a small indicator
    // (rather than filling the screen) over the overlay's background.
    private static class BouncingDotsView extends View {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final float[] dotLift = new float[3];
        private ValueAnimator anim;

        BouncingDotsView(Context context, int dotColor) {
            super(context);
            paint.setColor(dotColor);
            paint.setStyle(Paint.Style.FILL);
            setWillNotDraw(false);
        }

        // Ties the bounce loop to actual on-screen visibility rather than
        // running it for the app's whole lifetime -- navOverlay (this
        // view's parent) sits GONE between navigations, so without this
        // the animator would keep ticking indefinitely in the background
        // for no visible benefit.
        @Override
        protected void onVisibilityChanged(View changedView, int visibility) {
            super.onVisibilityChanged(changedView, visibility);
            if (visibility == View.VISIBLE) {
                startAnim();
            } else {
                stopAnim();
            }
        }

        @Override
        protected void onDetachedFromWindow() {
            super.onDetachedFromWindow();
            stopAnim();
        }

        private void startAnim() {
            if (anim != null) return;
            anim = ValueAnimator.ofFloat(0f, (float) (2 * Math.PI));
            anim.setDuration(1000);
            anim.setRepeatCount(ValueAnimator.INFINITE);
            anim.addUpdateListener(a -> {
                float t = (float) a.getAnimatedValue();
                for (int i = 0; i < 3; i++) {
                    // Each dot's phase is offset from the last so they
                    // bounce in a left-to-right ripple instead of together.
                    // Clamped at 0 so a dot rests on the baseline instead
                    // of dipping below it between bounces.
                    float phase = t - i * 0.55f;
                    dotLift[i] = (float) Math.max(0, Math.sin(phase));
                }
                invalidate();
            });
            anim.start();
        }

        private void stopAnim() {
            if (anim != null) {
                anim.cancel();
                anim = null;
            }
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            float w = getWidth();
            float h = getHeight();
            if (w <= 0 || h <= 0) return;
            float radius = Math.min(w, h) * 0.14f;
            float spacing = radius * 3.2f;
            float baseline = h * 0.68f;
            float bounceHeight = h * 0.38f;
            float startX = w / 2f - spacing;
            for (int i = 0; i < 3; i++) {
                float cy = baseline - dotLift[i] * bounceHeight;
                canvas.drawCircle(startX + i * spacing, cy, radius, paint);
            }
        }
    }

    // Common contract for whatever's sitting in the splash slot -- either
    // the built-in text-animation view below (LoadingSplashView) or a
    // user-uploaded video/image (CustomSplashView, further down). Lets
    // onCreate hold either one behind a single 'loading' variable and just
    // call show()/hide() without caring which kind it actually got.
    private interface SplashController {
        void show();
        void hide();
        // Used only when the load has failed and the offline screen is
        // about to be shown in its place -- skips whatever grace period
        // (minimum display time / "let the intro video finish") hide()
        // normally respects for a successful load, so the splash can't
        // still be fading/playing underneath the offline UI.
        void hideImmediate();
    }

    // Startup loading screen -- five selectable ways the app's name
    // assembles itself on a near-black backdrop while the WebView loads
    // behind it: TUMBLE (letters drop, spin and spring into place), FADE
    // (the whole wordmark rises gently while it fades in), TYPEWRITER
    // (letters type in left-to-right behind a blinking cursor), PULSE
    // (expanding rings ping outward behind the name) and SLIDE (letters
    // glide in from alternating sides). Whichever style is picked, the
    // wordmark keeps a gentle breathing pulse once it's landed so a slow
    // connection still reads as "working" instead of stuck. Text size
    // scales up for short names so they don't look lost in the middle of
    // the screen.
    private static class LoadingSplashView extends FrameLayout implements SplashController {
        static final int STYLE_TUMBLE = 0;
        static final int STYLE_FADE = 1;
        static final int STYLE_TYPEWRITER = 2;
        static final int STYLE_PULSE = 3;
        static final int STYLE_SLIDE = 4;
        static final int STYLE_NONE = 5;

        // Flat speed multiplier baked in from the Options tab's Slow /
        // Normal / Fast choice (1.6 / 1.0 / 0.6) -- every entrance
        // duration and delay below is passed through sd()/sdi() so the
        // whole animation plays slower or faster without changing what
        // it actually does.
        static final float SPEED_MULT = 1f;

        private static long sd(long ms) { return Math.round(ms * SPEED_MULT); }
        private static int sdi(int ms) { return (int) Math.round(ms * SPEED_MULT); }

        private final int style;
        private final View[] letters;
        private final LinearLayout row;
        private final View cursor;
        private final View[] rings;
        private final View barTrack;
        private final View barFill;
        private View barWrap;
        private int barFillWidth;
        private int barTrackWidthPx;
        private ValueAnimator idlePulse;
        private ValueAnimator cursorBlink;
        private ValueAnimator barAnim;
        private final Handler ringHandler = new Handler(Looper.getMainLooper());
        private final Runnable ringLoop = this::runRingLoop;
        private final Handler hideHandler = new Handler(Looper.getMainLooper());
        // Wall-clock time show() was called, and how long the entrance
        // animation needs to fully play out (the per-style value returned by
        // showTumble/showFade/etc, in ms). The page behind this view -- often
        // a local file:///android_asset/ asset -- can finish loading in just
        // a few milliseconds, well before a multi-letter entrance (staggered
        // tumble/typewriter/slide) has visually completed. Without tracking
        // this, hide() would cancel those in-flight per-letter animations
        // immediately, so the name appears to snap or cut off mid-motion
        // instead of finishing. hide() uses these two fields to wait out
        // whatever's left of the entrance before it starts fading out.
        private long showStartTime;
        private long minDisplayMs;

        LoadingSplashView(Context context, int bgColor, String appName, int style) {
            super(context);
            this.style = style;
            // Flat black/gray only -- fixed near-black navy (#10151C, see
            // splashBgColor), no color tint from the site's own accent.
            setBackgroundColor(bgColor);
            setVisibility(View.INVISIBLE);
            setAlpha(0f);

            float density = context.getResources().getDisplayMetrics().density;
            String name = (appName == null || appName.trim().isEmpty()) ? UrlObfuscator.decode(new int[] { 7, 21, 244 }, 70) : appName.trim().toUpperCase();

            // Pulse-ring style gets a few concentric ring outlines behind
            // everything else, pinging outward on a loop -- every other
            // style skips this entirely (empty array, loop never starts).
            if (style == STYLE_PULSE) {
                rings = new View[3];
                for (int i = 0; i < rings.length; i++) {
                    View ring = new View(context);
                    GradientDrawable ringBg = new GradientDrawable();
                    ringBg.setShape(GradientDrawable.OVAL);
                    ringBg.setColor(Color.TRANSPARENT);
                    ringBg.setStroke((int) (1.6f * density), withAlpha(Color.WHITE, 110));
                    ring.setBackground(ringBg);
                    int ringSize = (int) (120 * density);
                    FrameLayout.LayoutParams ringParams = new FrameLayout.LayoutParams(ringSize, ringSize);
                    ringParams.gravity = Gravity.CENTER;
                    ring.setAlpha(0f);
                    addView(ring, ringParams);
                    rings[i] = ring;
                }
            } else {
                rings = new View[0];
            }

            // Everything else stacks vertically -- the wordmark, then a
            // slim loading bar underneath it -- centered as one unit.
            LinearLayout column = new LinearLayout(context);
            column.setOrientation(LinearLayout.VERTICAL);
            column.setGravity(Gravity.CENTER);
            FrameLayout.LayoutParams columnParams = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            columnParams.gravity = Gravity.CENTER;
            addView(column, columnParams);

            // The wordmark row that holds each letter -- see
            // buildLetterView() below for how each one is actually styled
            // (light system weight, wide tracking, soft glow -- a clean,
            // minimal, "quick loading" look rather than a heavy one).
            row = new LinearLayout(context);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER);
            column.addView(row, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));

            float baseSize = letterSizeFor(name.length());
            int glowColor = Color.WHITE;
            letters = new View[name.length()];
            for (int i = 0; i < name.length(); i++) {
                char c = name.charAt(i);
                boolean isFirst = i == 0;
                View letterView = buildLetterView(context, c, isFirst, baseSize, density, glowColor);
                letterView.setAlpha(0f);
                row.addView(letterView);
                letters[i] = letterView;
            }

            // A slim indeterminate loading bar under the wordmark -- a
            // faint track with a brighter segment that slides back and
            // forth the whole time the page is loading, instead of the
            // soft glow the splash used to sit on.
            int barTrackWidth = (int) (108 * density);
            int barHeight = (int) (3 * density);
            barFillWidth = (int) (38 * density);

            barTrack = new View(context);
            GradientDrawable trackBg = new GradientDrawable();
            trackBg.setShape(GradientDrawable.RECTANGLE);
            trackBg.setCornerRadius(barHeight / 2f);
            trackBg.setColor(withAlpha(Color.WHITE, 32));
            barTrack.setBackground(trackBg);

            barFill = new View(context);
            GradientDrawable fillBg = new GradientDrawable();
            fillBg.setShape(GradientDrawable.RECTANGLE);
            fillBg.setCornerRadius(barHeight / 2f);
            fillBg.setColor(Color.WHITE);
            barFill.setBackground(fillBg);

            FrameLayout barWrap = new FrameLayout(context);
            FrameLayout.LayoutParams trackLp = new FrameLayout.LayoutParams(barTrackWidth, barHeight);
            trackLp.gravity = Gravity.CENTER;
            barWrap.addView(barTrack, trackLp);
            FrameLayout.LayoutParams fillLp = new FrameLayout.LayoutParams(barFillWidth, barHeight);
            fillLp.gravity = Gravity.CENTER_VERTICAL | Gravity.START;
            barWrap.addView(barFill, fillLp);
            barWrap.setAlpha(0f);

            LinearLayout.LayoutParams barWrapLp = new LinearLayout.LayoutParams(barTrackWidth, barHeight);
            barWrapLp.topMargin = (int) (24 * density);
            column.addView(barWrap, barWrapLp);
            this.barWrap = barWrap;
            barTrackWidthPx = barTrackWidth;

            // Typewriter style gets a thin blinking cursor bar right after
            // the last letter -- every other style never adds it to the row.
            if (style == STYLE_TYPEWRITER) {
                cursor = new View(context);
                cursor.setBackgroundColor(Color.WHITE);
                int cursorWidth = (int) (3 * density);
                int cursorHeight = (int) (baseSize * density * 0.95f);
                LinearLayout.LayoutParams cursorParams = new LinearLayout.LayoutParams(cursorWidth, cursorHeight);
                cursorParams.leftMargin = (int) (4 * density);
                cursor.setAlpha(0f);
                row.addView(cursor, cursorParams);
            } else {
                cursor = null;
            }
        }

        // Builds one letter as a single, lightweight TextView -- the
        // system's medium Roboto weight rather than a heavy bold, wide
        // letter-spacing, and just a soft white glow (no color tint, no
        // hard outline) -- the clean, minimal, quick-loading wordmark
        // feel of something like Facebook Lite's splash rather than a
        // bold caption-style treatment. Every letter matches the size,
        // weight and brightness the first letter used to have alone, so
        // the whole name reads as one consistently bold wordmark instead
        // of one emphasized letter followed by smaller ones.
        private static View buildLetterView(Context context, char c, boolean isFirst, float baseSize, float density, int glowColor) {
            String txt = c == ' ' ? " " : String.valueOf(c);
            TextView letter = new TextView(context);
            letter.setText(txt);
            letter.setTypeface(Typeface.create(UrlObfuscator.decode(new int[] { 36, 23, 251, 199, 254, 129, 116, 66, 38, 8, 160, 193, 174, 142, 96, 93, 42 }, 87), Typeface.NORMAL));
            letter.setTextSize(baseSize * 1.3f);
            letter.setLetterSpacing(0.14f);
            letter.setTextColor(Color.WHITE);
            letter.setShadowLayer(16f, 0f, 0f, withAlpha(glowColor, 110));
            return letter;
        }

        // Shorter names get noticeably bigger text -- a 3-4 letter name at
        // the same size as a long one would look lost in the middle of the
        // screen, so scale it up as the name gets shorter.
        private static float letterSizeFor(int nameLength) {
            if (nameLength <= 4) return 54f;
            if (nameLength <= 6) return 44f;
            if (nameLength <= 9) return 35f;
            if (nameLength <= 13) return 27f;
            return 22f;
        }

        private static int withAlpha(int color, int alpha) {
            return (color & 0x00FFFFFF) | (alpha << 24);
        }

        // Fades in the backdrop + loading bar, then hands off to whichever
        // entrance the chosen style uses for the letters. Safe to call
        // again after hide() -- resets every child first.
        public void show() {
            stopIdlePulse();
            hideHandler.removeCallbacksAndMessages(null);
            animate().cancel();
            barWrap.animate().cancel();
            setVisibility(View.VISIBLE);
            setAlpha(0f);
            barWrap.setAlpha(0f);

            animate().alpha(1f).setDuration(sd(360)).start();
            barWrap.animate().alpha(1f).setStartDelay(sd(160)).setDuration(sd(500)).start();
            startBarAnim();

            long idleStart;
            switch (style) {
                case STYLE_FADE:
                    idleStart = showFade();
                    break;
                case STYLE_TYPEWRITER:
                    idleStart = showTypewriter();
                    break;
                case STYLE_PULSE:
                    idleStart = showPulse();
                    break;
                case STYLE_SLIDE:
                    idleStart = showSlide();
                    break;
                case STYLE_NONE:
                    idleStart = showNone();
                    break;
                case STYLE_TUMBLE:
                default:
                    idleStart = showTumble();
                    break;
            }

            // Pulsing kicks in once the entrance has landed, and keeps
            // going -- it's only ever stopped by hide(), i.e. it runs for
            // as long as the page is still loading, however long that
            // ends up taking.
            postDelayed(this::startIdlePulse, idleStart);

            showStartTime = SystemClock.uptimeMillis();
            minDisplayMs = idleStart;
        }

        // Slides the bright segment of the loading bar back and forth
        // across the track on an infinite loop -- purely indeterminate
        // (not tied to real page-load percentage), just something visibly
        // "working" under the wordmark the whole time it's showing.
        private void startBarAnim() {
            if (barAnim != null) barAnim.cancel();
            float maxTranslation = barTrackWidthPx - barFillWidth;
            barAnim = ValueAnimator.ofFloat(0f, maxTranslation);
            barAnim.setDuration(sd(950));
            barAnim.setRepeatMode(ValueAnimator.REVERSE);
            barAnim.setRepeatCount(ValueAnimator.INFINITE);
            barAnim.addUpdateListener(a -> barFill.setTranslationX((float) a.getAnimatedValue()));
            barAnim.start();
        }

        private float density() {
            return getResources().getDisplayMetrics().density;
        }

        // TUMBLE: each letter drops, spins slightly off its axis and
        // springs back with a little scale overshoot as it lands -- more
        // like it's physically tumbling into place than just sliding on
        // one axis, staggered left to right so the name reads as being
        // assembled.
        private long showTumble() {
            float density = density();
            int letterStagger = sdi(80);
            for (int i = 0; i < letters.length; i++) {
                View letter = letters[i];
                letter.animate().cancel();
                letter.setAlpha(0f);
                letter.setTranslationX(0f);
                letter.setTranslationY(-56 * density);
                letter.setScaleX(0.3f);
                letter.setScaleY(0.3f);
                // Alternating tilt direction per letter, growing slightly
                // toward the middle letters, so the row doesn't read as a
                // mechanically identical repeat of the same motion.
                float tilt = (i % 2 == 0 ? -1f : 1f) * (16f + (i * 5f) % 14f);
                letter.setRotation(tilt);

                long delay = sd(200) + (long) i * letterStagger;

                ObjectAnimator fall = ObjectAnimator.ofFloat(letter, View.TRANSLATION_Y, -56 * density, 0f);
                fall.setDuration(sd(560));
                // Smoother single-settle landing instead of the multi-bounce
                // BounceInterpolator used to give -- still a snappy pop, but
                // one clean overshoot-and-settle reads as "smooth" rather
                // than jittery, closer to a slick caption-style entrance.
                fall.setInterpolator(new OvershootInterpolator(1.8f));

                ObjectAnimator spin = ObjectAnimator.ofFloat(letter, View.ROTATION, tilt, 0f);
                spin.setDuration(sd(520));
                spin.setInterpolator(new OvershootInterpolator(2.2f));

                ObjectAnimator growX = ObjectAnimator.ofFloat(letter, View.SCALE_X, 0.3f, 1f);
                ObjectAnimator growY = ObjectAnimator.ofFloat(letter, View.SCALE_Y, 0.3f, 1f);
                growX.setDuration(sd(480));
                growY.setDuration(sd(480));
                growX.setInterpolator(new OvershootInterpolator(3.4f));
                growY.setInterpolator(new OvershootInterpolator(3.4f));

                ObjectAnimator fadeIn = ObjectAnimator.ofFloat(letter, View.ALPHA, 0f, 1f);
                fadeIn.setDuration(sd(220));

                AnimatorSet letterIn = new AnimatorSet();
                letterIn.playTogether(fall, spin, growX, growY, fadeIn);
                letterIn.setStartDelay(delay);
                letterIn.start();
            }
            return sd(200) + (long) letters.length * letterStagger + sd(700);
        }

        // FADE & RISE: no per-letter stagger at all -- the whole wordmark
        // rises gently out of the backdrop as one block while it fades in,
        // the calmest of the five.
        private long showFade() {
            float density = density();
            for (View letter : letters) {
                letter.animate().cancel();
                letter.setAlpha(1f);
                letter.setScaleX(1f);
                letter.setScaleY(1f);
                letter.setTranslationX(0f);
                letter.setTranslationY(0f);
                letter.setRotation(0f);
            }
            row.animate().cancel();
            row.setAlpha(0f);
            row.setTranslationY(28 * density);
            row.animate()
                .alpha(1f)
                .translationY(0f)
                .setStartDelay(sd(160))
                .setDuration(sd(700))
                .start();
            return sd(160) + sd(700);
        }

        // NONE: no per-letter animation at all. The name is placed in its
        // final resting state immediately -- the only motion on screen is
        // the plain backdrop fade-in that show() already does for every
        // style. Used when the user just wants a static loading screen.
        private long showNone() {
            for (View letter : letters) {
                letter.animate().cancel();
                letter.setAlpha(1f);
                letter.setScaleX(1f);
                letter.setScaleY(1f);
                letter.setTranslationX(0f);
                letter.setTranslationY(0f);
                letter.setRotation(0f);
            }
            row.animate().cancel();
            row.setAlpha(1f);
            row.setTranslationY(0f);
            if (cursor != null) {
                cursor.setAlpha(0f);
            }
            // Still matches the container's own 360ms fade-in so the splash
            // can't be hidden before it's even fully visible.
            return sd(360);
        }

        // TYPEWRITER: letters appear left to right with a quick fade + tiny
        // grow, no bounce or spin, behind a cursor bar that only appears
        // and starts blinking once typing finishes -- reads as the name
        // being typed out.
        private long showTypewriter() {
            if (cursor != null) {
                cursor.animate().cancel();
                cursor.setAlpha(0f);
            }
            int letterStagger = sdi(90);
            for (int i = 0; i < letters.length; i++) {
                View letter = letters[i];
                letter.animate().cancel();
                letter.setAlpha(0f);
                letter.setTranslationX(0f);
                letter.setTranslationY(0f);
                letter.setRotation(0f);
                letter.setScaleX(0.9f);
                letter.setScaleY(0.9f);

                long delay = sd(250) + (long) i * letterStagger;
                ObjectAnimator fadeIn = ObjectAnimator.ofFloat(letter, View.ALPHA, 0f, 1f);
                ObjectAnimator growX = ObjectAnimator.ofFloat(letter, View.SCALE_X, 0.9f, 1f);
                ObjectAnimator growY = ObjectAnimator.ofFloat(letter, View.SCALE_Y, 0.9f, 1f);
                fadeIn.setDuration(sd(160));
                growX.setDuration(sd(160));
                growY.setDuration(sd(160));
                AnimatorSet letterIn = new AnimatorSet();
                letterIn.playTogether(fadeIn, growX, growY);
                letterIn.setStartDelay(delay);
                letterIn.start();
            }
            long typedDone = sd(250) + (long) letters.length * letterStagger + sd(160);
            if (cursor != null) {
                postDelayed(() -> {
                    cursor.setAlpha(1f);
                    startCursorBlink();
                }, typedDone);
            }
            return typedDone + sd(300);
        }

        // PULSE RINGS: concentric ring outlines ping outward from center on
        // a repeating loop, starting immediately, while the wordmark fades
        // in as one block on top of them a beat later.
        private long showPulse() {
            for (View letter : letters) {
                letter.animate().cancel();
                letter.setAlpha(1f);
                letter.setScaleX(1f);
                letter.setScaleY(1f);
                letter.setTranslationX(0f);
                letter.setTranslationY(0f);
                letter.setRotation(0f);
            }
            row.animate().cancel();
            row.setAlpha(0f);
            row.animate().alpha(1f).setStartDelay(sd(360)).setDuration(sd(500)).start();

            for (View ring : rings) {
                ring.animate().cancel();
                ring.setScaleX(0.4f);
                ring.setScaleY(0.4f);
                ring.setAlpha(0f);
            }
            ringHandler.removeCallbacks(ringLoop);
            ringHandler.post(ringLoop);
            return sd(360) + sd(500);
        }

        // SLIDE IN: letters glide in horizontally from alternating sides
        // (odd from the left, even from the right) and settle with a small
        // overshoot -- a sideways counterpart to the vertical tumble, no
        // rotation or bounce.
        private long showSlide() {
            float density = density();
            int letterStagger = sdi(70);
            for (int i = 0; i < letters.length; i++) {
                View letter = letters[i];
                letter.animate().cancel();
                letter.setAlpha(0f);
                letter.setTranslationY(0f);
                letter.setRotation(0f);
                letter.setScaleX(1f);
                letter.setScaleY(1f);
                float startX = (i % 2 == 0 ? -1f : 1f) * 90 * density;
                letter.setTranslationX(startX);

                long delay = sd(180) + (long) i * letterStagger;
                ObjectAnimator slide = ObjectAnimator.ofFloat(letter, View.TRANSLATION_X, startX, 0f);
                slide.setDuration(sd(520));
                slide.setInterpolator(new OvershootInterpolator(1.6f));
                ObjectAnimator fadeIn = ObjectAnimator.ofFloat(letter, View.ALPHA, 0f, 1f);
                fadeIn.setDuration(sd(320));
                AnimatorSet letterIn = new AnimatorSet();
                letterIn.playTogether(slide, fadeIn);
                letterIn.setStartDelay(delay);
                letterIn.start();
            }
            return sd(180) + (long) letters.length * letterStagger + sd(520);
        }

        // One ping outward per ring, staggered, then reposts itself so the
        // sonar effect keeps going for as long as the splash is showing.
        private void runRingLoop() {
            for (int i = 0; i < rings.length; i++) {
                final View ring = rings[i];
                ring.animate().cancel();
                ring.setScaleX(0.4f);
                ring.setScaleY(0.4f);
                ring.setAlpha(0.8f);
                ring.animate()
                    .scaleX(1.6f).scaleY(1.6f).alpha(0f)
                    .setStartDelay((long) i * sd(260))
                    .setDuration(sd(1400))
                    .setInterpolator(new DecelerateInterpolator())
                    .start();
            }
            ringHandler.postDelayed(ringLoop, sd(1600));
        }

        // Fades the view out and sets it GONE so the WebView underneath
        // takes over. Callers (onPageFinished, error handling, etc.) can
        // call this the instant the page is ready, which for a local
        // file:///android_asset/ page is often only a handful of
        // milliseconds after show() -- long before a staggered entrance
        // (tumble/typewriter/slide) has actually finished playing. Rather
        // than cutting that animation off mid-flight, wait out whatever's
        // left of minDisplayMs before starting the actual fade-out.
        public void hide() {
            hideHandler.removeCallbacksAndMessages(null);
            long elapsed = SystemClock.uptimeMillis() - showStartTime;
            long remaining = minDisplayMs - elapsed;
            if (remaining > 0) {
                hideHandler.postDelayed(this::doHide, remaining);
            } else {
                doHide();
            }
        }

        @Override
        public void hideImmediate() {
            hideHandler.removeCallbacksAndMessages(null);
            doHide();
        }

        private void doHide() {
            stopIdlePulse();
            ringHandler.removeCallbacks(ringLoop);
            if (barAnim != null) {
                barAnim.cancel();
                barAnim = null;
            }
            animate().cancel();
            animate()
                .alpha(0f)
                .setDuration(sd(320))
                .withEndAction(() -> setVisibility(View.GONE))
                .start();
        }

        // Gentle breathing on the wordmark -- a soft alpha + scale pulse --
        // runs continuously until hide() is called, so as long as the
        // WebView is still loading the name keeps visibly "alive" instead
        // of sitting static. Skipped for typewriter, which already has its
        // own blinking cursor doing that job.
        private void startIdlePulse() {
            if (idlePulse != null || style == STYLE_TYPEWRITER || style == STYLE_NONE) return;
            idlePulse = ValueAnimator.ofFloat(0f, 1f);
            idlePulse.setDuration(sd(1300));
            idlePulse.setRepeatMode(ValueAnimator.REVERSE);
            idlePulse.setRepeatCount(ValueAnimator.INFINITE);
            idlePulse.addUpdateListener(a -> {
                float t = (float) a.getAnimatedValue();
                float pulseAlpha = 0.6f + 0.4f * t;
                float pulseScale = 1f + 0.035f * t;
                for (View letter : letters) {
                    letter.setAlpha(pulseAlpha);
                    letter.setScaleX(pulseScale);
                    letter.setScaleY(pulseScale);
                }
            });
            idlePulse.start();
        }

        // Blink loop for the typewriter cursor -- a plain alpha square-wave
        // rather than a smooth pulse, so it reads as a real text cursor.
        private void startCursorBlink() {
            if (cursorBlink != null || cursor == null) return;
            cursorBlink = ValueAnimator.ofFloat(0f, 1f);
            cursorBlink.setDuration(sd(530));
            cursorBlink.setRepeatMode(ValueAnimator.RESTART);
            cursorBlink.setRepeatCount(ValueAnimator.INFINITE);
            cursorBlink.addUpdateListener(a -> {
                float t = (float) a.getAnimatedValue();
                cursor.setAlpha(t < 0.5f ? 1f : 0f);
            });
            cursorBlink.start();
        }

        private void stopIdlePulse() {
            if (idlePulse != null) {
                idlePulse.cancel();
                idlePulse = null;
            }
            if (cursorBlink != null) {
                cursorBlink.cancel();
                cursorBlink = null;
            }
        }
    }

    // A full-bleed, center-cropped video surface for the splash screen.
    //
    // This deliberately does NOT use android.widget.VideoView. VideoView
    // (backed by a SurfaceView) relies on
    // MediaPlayer.setVideoScalingMode(VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING)
    // to preserve aspect ratio while filling the screen -- and on a lot of
    // real devices that mode is silently ignored, so the video just gets
    // stretched to exactly fill the view (non-uniform scale), distorting
    // anything that isn't already the same aspect ratio as the screen.
    // That's a widely-reported VideoView/MediaPlayer quirk, not something
    // fixable by picking a different scaling-mode constant.
    //
    // TextureView sidesteps it: by default it *also* stretches its content
    // to exactly fill the view, but since we render into it manually we can
    // apply our own Matrix that compensates for that stretch and restores a
    // true uniform-scale-and-crop (the same visual result as ImageView's
    // CENTER_CROP), independent of device/OEM MediaPlayer behavior.
    private static class CropTextureView extends TextureView implements TextureView.SurfaceTextureListener {
        private int videoWidth = 0;
        private int videoHeight = 0;

        CropTextureView(Context context) {
            super(context);
            setSurfaceTextureListener(this);
        }

        void setVideoSize(int width, int height) {
            videoWidth = width;
            videoHeight = height;
            applyCropTransform();
        }

        private void applyCropTransform() {
            int viewWidth = getWidth();
            int viewHeight = getHeight();
            if (viewWidth == 0 || viewHeight == 0 || videoWidth == 0 || videoHeight == 0) return;

            // TextureView's default transform already stretches the buffer
            // non-uniformly to exactly fill (viewWidth x viewHeight). To turn
            // that into a center-crop, scale further around the center by
            // however much the video's aspect ratio differs from the view's.
            float viewRatio = viewWidth / (float) viewHeight;
            float videoRatio = videoWidth / (float) videoHeight;
            float scaleX = 1f, scaleY = 1f;
            if (videoRatio > viewRatio) {
                scaleX = videoRatio / viewRatio;
            } else {
                scaleY = viewRatio / videoRatio;
            }
            Matrix matrix = new Matrix();
            matrix.setScale(scaleX, scaleY, viewWidth / 2f, viewHeight / 2f);
            setTransform(matrix);
        }

        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
            applyCropTransform();
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
            applyCropTransform();
        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
            return true;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surface) {}
    }

    // Startup screen built from a user-uploaded file instead of one of the
    // built-in text animations: either a single-play video (custom_splash
    // in res/raw) or a still image (custom_splash in res/drawable).
    // Looked up by resource name at runtime via getIdentifier rather than a
    // generated R reference, since whether that resource even exists
    // depends entirely on whether this specific build actually shipped one.
    private static class CustomSplashView extends FrameLayout implements SplashController {
        private final boolean isVideo;
        private final CropTextureView textureView;
        private MediaPlayer mediaPlayer;
        private Surface playerSurface;
        // False only while an uploaded video still has playback left --
        // there's nothing to wait on for a still image, or when no video
        // resource actually got shipped, so those start out already "done".
        private boolean videoDone = true;
        private final boolean hasVideoSource;
        private final int videoResId;
        // hide() can be asked to close before the video's finished (the
        // WebView is done loading first, which is the common case) --
        // remember that it was asked, and actually close once
        // onCompletion/onError fires instead of cutting the clip off.
        private boolean pendingHide = false;
        // show() can be called before the SurfaceTexture is ready yet
        // (first launch, cold start) -- remember that playback was
        // requested and start it as soon as the surface actually shows up.
        private boolean pendingPlay = false;
        // Three-dot pulse shown ONLY while the clip has finished playing
        // but the page hasn't -- i.e. exactly the frozen-frame wait. Never
        // shown during normal playback, so it doesn't compete visually
        // with the video itself.
        private final LinearLayout waitDots;
        private final View[] waitDotViews = new View[3];
        private ValueAnimator waitDotsAnim;

        CustomSplashView(Context context, int bgColor, boolean isVideo) {
            super(context);
            this.isVideo = isVideo;
            setBackgroundColor(bgColor);
            setVisibility(View.INVISIBLE);
            setAlpha(0f);

            String pkg = context.getPackageName();
            FrameLayout.LayoutParams fill = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT);

            if (isVideo) {
                videoResId = context.getResources().getIdentifier(UrlObfuscator.decode(new int[] { 11, 242, 213, 177, 139, 110, 125, 50, 16, 19, 255, 206, 180 }, 104), UrlObfuscator.decode(new int[] { 11, 249, 192 }, 121), pkg);
                hasVideoSource = videoResId != 0;
                CropTextureView tv = new CropTextureView(context);
                if (hasVideoSource) {
                    tv.setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {
                        @Override
                        public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
                            tv.onSurfaceTextureAvailable(surface, width, height);
                            playerSurface = new Surface(surface);
                            preparePlayer(context);
                        }
                        @Override
                        public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
                            tv.onSurfaceTextureSizeChanged(surface, width, height);
                        }
                        @Override
                        public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
                            if (mediaPlayer != null) {
                                mediaPlayer.release();
                                mediaPlayer = null;
                            }
                            if (playerSurface != null) {
                                playerSurface.release();
                                playerSurface = null;
                            }
                            return tv.onSurfaceTextureDestroyed(surface);
                        }
                        @Override
                        public void onSurfaceTextureUpdated(SurfaceTexture surface) {
                            tv.onSurfaceTextureUpdated(surface);
                        }
                    });
                }
                textureView = tv;
                addView(textureView, fill);
            } else {
                textureView = null;
                hasVideoSource = false;
                videoResId = 0;
                ImageView iv = new ImageView(context);
                iv.setScaleType(ImageView.ScaleType.CENTER_CROP);
                int resId = context.getResources().getIdentifier(UrlObfuscator.decode(new int[] { 233, 220, 187, 147, 105, 72, 27, 16, 242, 205, 161, 172, 150 }, 138), UrlObfuscator.decode(new int[] { 255, 200, 184, 143, 118, 84, 57, 17 }, 155), pkg);
                if (resId != 0) {
                    iv.setImageURI(Uri.parse(UrlObfuscator.decode(new int[] { 205, 165, 142, 123, 71, 46, 2, 171, 214, 166, 145, 110, 85, 77, 61, 24, 166, 148, 245 }, 172) + pkg + "/" + resId));
                }
                addView(iv, fill);
            }

            // Built once regardless of image/video -- only ever made
            // visible from the video branch's frozen-frame wait (see
            // onCompletion below), but harmless (and unused) for images.
            float density = context.getResources().getDisplayMetrics().density;
            int dotSize = Math.round(8 * density);
            int dotGap = Math.round(10 * density);
            waitDots = new LinearLayout(context);
            waitDots.setOrientation(LinearLayout.HORIZONTAL);
            waitDots.setAlpha(0f);
            waitDots.setVisibility(View.INVISIBLE);
            for (int i = 0; i < waitDotViews.length; i++) {
                View dot = new View(context);
                GradientDrawable dotBg = new GradientDrawable();
                dotBg.setShape(GradientDrawable.OVAL);
                dotBg.setColor(Color.WHITE);
                dot.setBackground(dotBg);
                LinearLayout.LayoutParams dotParams = new LinearLayout.LayoutParams(dotSize, dotSize);
                if (i > 0) dotParams.leftMargin = dotGap;
                waitDots.addView(dot, dotParams);
                waitDotViews[i] = dot;
            }
            FrameLayout.LayoutParams dotsParams = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT);
            dotsParams.gravity = Gravity.CENTER_HORIZONTAL | Gravity.BOTTOM;
            dotsParams.bottomMargin = Math.round(64 * density);
            addView(waitDots, dotsParams);
        }

        // Starts (or is a no-op if already running) the staggered
        // pulse -- each dot fades/scales up and back down in its own
        // phase, looping until stopWaitDots() is called.
        private void startWaitDots() {
            if (waitDotsAnim != null) return;
            waitDots.setVisibility(View.VISIBLE);
            waitDots.animate().alpha(1f).setDuration(220).start();
            waitDotsAnim = ValueAnimator.ofFloat(0f, 1f);
            waitDotsAnim.setDuration(1000);
            waitDotsAnim.setRepeatCount(ValueAnimator.INFINITE);
            waitDotsAnim.addUpdateListener(a -> {
                float t = (float) a.getAnimatedValue();
                for (int i = 0; i < waitDotViews.length; i++) {
                    // Each dot's own phase is offset by a third of the
                    // cycle so the pulse visibly travels left-to-right
                    // rather than all three dots moving in lockstep.
                    float phase = (t + (i / (float) waitDotViews.length)) % 1f;
                    float bump = (float) Math.sin(phase * Math.PI);
                    float scale = 0.7f + 0.3f * bump;
                    waitDotViews[i].setScaleX(scale);
                    waitDotViews[i].setScaleY(scale);
                    waitDotViews[i].setAlpha(0.4f + 0.6f * bump);
                }
            });
            waitDotsAnim.start();
        }

        private void stopWaitDots() {
            if (waitDotsAnim != null) {
                waitDotsAnim.cancel();
                waitDotsAnim = null;
            }
            waitDots.animate().cancel();
            waitDots.animate().alpha(0f).setDuration(160)
                .withEndAction(() -> waitDots.setVisibility(View.INVISIBLE))
                .start();
        }

        // Sets up the MediaPlayer once the TextureView's SurfaceTexture is
        // actually available -- can't render into it any earlier than that.
        private void preparePlayer(Context context) {
            if (mediaPlayer != null || playerSurface == null) return;
            String pkg = context.getPackageName();
            MediaPlayer mp = new MediaPlayer();
            mediaPlayer = mp;
            try {
                mp.setDataSource(context, Uri.parse(UrlObfuscator.decode(new int[] { 220, 178, 159, 104, 86, 49, 19, 184, 199, 177, 128, 125, 68, 34, 12, 235, 151, 227, 196 }, 189) + pkg + "/" + videoResId));
                mp.setSurface(playerSurface);
                // Single play, not looped -- see onCompletion below for
                // how the "video ends before the page is ready" case is
                // actually handled (freeze on the last frame, not repeat).
                mp.setLooping(false);
                mp.setOnPreparedListener(p -> {
                    textureView.setVideoSize(p.getVideoWidth(), p.getVideoHeight());
                    if (pendingPlay) {
                        pendingPlay = false;
                        videoDone = false;
                        p.seekTo(0);
                        p.start();
                    }
                });
                mp.setOnCompletionListener(p -> {
                    videoDone = true;
                    // If the page isn't ready yet, freeze on the clip's
                    // last frame instead of looping it (annoying to watch
                    // repeat) or leaving it black. Some devices clear the
                    // TextureView's buffer once MediaPlayer hits its
                    // PlaybackCompleted state -- re-seeking to just before
                    // the end forces a redraw so that last frame actually
                    // stays visible while we wait. The pulsing dots make
                    // it visually clear the app hasn't stalled.
                    if (pendingHide) {
                        doHide();
                    } else {
                        p.seekTo(Math.max(0, p.getDuration() - 33));
                        startWaitDots();
                    }
                });
                // A clip that can't actually play (bad codec, corrupt
                // upload, whatever) shouldn't leave the app stuck behind a
                // black screen forever -- treat a playback error the same
                // as having finished.
                mp.setOnErrorListener((p, what, extra) -> {
                    videoDone = true;
                    if (pendingHide) doHide();
                    return true;
                });
                mp.prepareAsync();
            } catch (Exception e) {
                videoDone = true;
                if (pendingHide) doHide();
            }
        }

        public void show() {
            animate().cancel();
            setVisibility(View.VISIBLE);
            setAlpha(0f);
            animate().alpha(1f).setDuration(280).start();
            pendingHide = false;
            stopWaitDots();
            if (hasVideoSource) {
                if (mediaPlayer != null) {
                    videoDone = false;
                    mediaPlayer.seekTo(0);
                    mediaPlayer.start();
                } else {
                    // Surface (and therefore the player) isn't ready yet --
                    // preparePlayer()'s onPrepared will start it instead.
                    pendingPlay = true;
                }
            }
        }

        public void hide() {
            if (hasVideoSource && !videoDone) {
                // The page is done loading, but the intro clip isn't --
                // let it play out before the WebView actually appears
                // instead of cutting it off mid-frame.
                pendingHide = true;
                return;
            }
            doHide();
        }

        @Override
        public void hideImmediate() {
            pendingHide = false;
            doHide();
        }

        private void doHide() {
            pendingHide = false;
            stopWaitDots();
            animate().cancel();
            animate()
                .alpha(0f)
                .setDuration(280)
                .withEndAction(() -> {
                    setVisibility(View.GONE);
                    if (isVideo && mediaPlayer != null) mediaPlayer.pause();
                })
                .start();
        }
    }

    // Startup loading screen for when the Options tab's animation picker is
    // set to "off" or "none" (no animation) -- rather than showing nothing
    // while the page loads (which used to mean the WebView appeared the
    // instant it was created, flashing black before its own background even
    // painted), this shows the app's own launcher icon centered on the
    // splash background, completely static, and holds it until the page is
    // ready -- then hide() below swaps it out immediately, no minimum
    // display time and no loading bar, so it goes straight to the app the
    // moment the page finishes loading. "Off"/"none" mean no animated
    // entrance, not no splash.
    private static class StaticIconSplashView extends FrameLayout implements SplashController {
        StaticIconSplashView(Context context, int bgColor) {
            super(context);
            setBackgroundColor(bgColor);
            setVisibility(View.INVISIBLE);
            setAlpha(0f);

            float density = context.getResources().getDisplayMetrics().density;
            int iconSize = Math.round(96 * density);
            ImageView iv = new ImageView(context);
            iv.setScaleType(ImageView.ScaleType.FIT_CENTER);
            // Looked up by name (same pattern as CustomSplashView's
            // custom_splash asset above) rather than referenced as
            // R.mipmap.ic_launcher, since this generated source doesn't
            // otherwise depend on the built R class. ic_launcher is always
            // present -- either the uploaded logo or the generated default
            // -- so this should never come back 0.
            int iconResId = context.getResources().getIdentifier(UrlObfuscator.decode(new int[] { 167, 142, 83, 71, 43, 28, 230, 196, 174, 128, 118 }, 206), UrlObfuscator.decode(new int[] { 178, 151, 109, 81, 58, 10 }, 223), context.getPackageName());
            if (iconResId != 0) {
                iv.setImageResource(iconResId);
            }
            FrameLayout.LayoutParams iconParams = new FrameLayout.LayoutParams(iconSize, iconSize);
            iconParams.gravity = Gravity.CENTER;
            addView(iv, iconParams);
        }

        public void show() {
            animate().cancel();
            setVisibility(View.VISIBLE);
            setAlpha(0f);
            animate().alpha(1f).setDuration(220).start();
        }

        public void hide() {
            animate().cancel();
            animate().alpha(0f).setDuration(220)
                .withEndAction(() -> setVisibility(View.GONE))
                .start();
        }

        @Override
        public void hideImmediate() {
            animate().cancel();
            setAlpha(0f);
            setVisibility(View.GONE);
        }
    }

    // Used when the Options tab's startup loading is set to "off": no overlay
    // of any kind (no icon, no color, no animation). It exists only so the
    // 'loading' variable in onCreate still has something to call show()/hide()
    // on. The app's first frame is held back instead (see firstFrameHold in
    // onCreate), so the OS launch screen stays up until the page is ready.
    private static class NoSplashView extends View implements SplashController {
        NoSplashView(Context context) {
            super(context);
            setVisibility(View.GONE);
        }
        public void show() { }
        public void hide() { }
        @Override
        public void hideImmediate() { }
    }

    // Small helper the pulse loop below calls on each ring in turn: scales
    // a translucent circle up while fading it out, sonar-style. Kept as a
    // plain method (not a local lambda) since it doesn't need to capture
    // anything from onCreate.
    private void pulseOnce(View ring) {
        ring.animate().cancel();
        ring.setScaleX(0.5f);
        ring.setScaleY(0.5f);
        ring.setAlpha(0.5f);
        ring.animate()
            .scaleX(1.7f).scaleY(1.7f).alpha(0f)
            .setDuration(1400)
            .setInterpolator(new DecelerateInterpolator())
            .start();
    }

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        UpdateChecker.check(this);
        // Channel + runtime permission are needed for ANY notification --
        // local (AndroidBridge.showNotification, always available) as well
        // as Firebase push (only if google-services.json was provided) --
        // so both now run unconditionally rather than only when FCM is on.
        createNotificationChannel();
        requestNotificationPermissionIfNeeded();

        // Edge-to-edge + safe area. The app draws behind the system bars and
        // handles the insets itself (status bar, gesture/nav bar, camera
        // cutout, keyboard) -- see the insets listener after the WebView is
        // added below. Doing the keyboard through insets, instead of letting
        // the system resize the whole window, is what removes the black /
        // white flash: the strip that opens above the keyboard is now the
        // root layout, which is always painted the page's own color.
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        getWindow().setStatusBarColor(Color.TRANSPARENT);
        getWindow().setNavigationBarColor(Color.TRANSPARENT);
        if (Build.VERSION.SDK_INT >= 29) {
            getWindow().setNavigationBarContrastEnforced(false);
        }
        if (Build.VERSION.SDK_INT >= 30) {
            getWindow().getAttributes().layoutInDisplayCutoutMode =
                android.view.WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS;
        } else if (Build.VERSION.SDK_INT >= 28) {
            getWindow().getAttributes().layoutInDisplayCutoutMode =
                android.view.WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
        }

        FrameLayout root = new FrameLayout(this);
        rootLayout = root;
        root.setBackgroundColor(Color.parseColor(UrlObfuscator.decode(new int[] { 211, 63, 27, 125, 89, 187, 159 }, 240)));

        webView = new WebView(this);
        // A freshly created WebView has no rendered frame yet -- its
        // underlying Surface starts out blank/black, and that shows through
        // for a beat even with setBackgroundColor(WHITE) set, because that
        // color is drawn BY the WebView, but the Surface itself hasn't
        // produced a first frame to draw it onto yet. Making the WebView
        // visible immediately -- which this used to do whenever startup
        // loading was set to "off" -- is exactly what let that black flash
        // reach the screen, often right before a second flash of the
        // WebView's own white background as the real page started painting.
        // Two mismatched flashes back to back is the "black then white"
        // effect.
        //
        // The fix: never show the WebView until it already has real content
        // to show. It now loads fully hidden behind this root FrameLayout's
        // solid near-black background (#050505 above) and only gets
        // revealed in revealWebView() (see onPageFinished below), which
        // runs unconditionally regardless of the splash setting. With
        // startup loading on, that reveal is the existing animated hand-off
        // from the splash overlay; with it set to "off", it's the same
        // reveal minus the overlay -- a plain near-black hold, then the
        // site, with nothing flashing in between.
        // White is only the default for pages that draw no background of their
        // own; once a page finishes loading, AndroidBridge.applyPageBackground
        // replaces it (and the window background) with the page's real color,
        // so resizing for the keyboard never exposes a mismatched color.
        webView.setBackgroundColor(Color.WHITE);
        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);

        webView.setVisibility(View.GONE);
        
        FrameLayout.LayoutParams webParams = new FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT);
        // Wrapping the WebView in a SwipeRefreshLayout gets a native
        // swipe-down-to-reload gesture almost for free -- BUT its default
        // canChildScrollUp() check only looks at the WebView's own
        // top-level document scroll offset. Plenty of real sites (chat
        // apps, feeds, anything with an inner "overflow-y: auto" panel)
        // never scroll the document itself -- an inner <div> scrolls
        // instead, so the WebView's own scrollY sits at 0 forever. That
        // makes SwipeRefreshLayout think the page is always "at the top,"
        // so it hijacks every downward drag anywhere on screen -- including
        // ones meant to scroll that inner panel -- as a refresh gesture,
        // which blocks the real scroll entirely. The capture-phase JS
        // listener registered in onPageFinished below reports the
        // scrollTop of whatever element actually just scrolled (scroll
        // events don't bubble, but they ARE observable via a capture
        // listener on window), and reportScrollTop() uses that -- not
        // webView.getScrollY() -- to decide whether a pull should refresh.
        //
        // That alone still isn't enough for dialogs/bottom sheets: one
        // that just opened (or was never scrolled) reads scrollTop 0,
        // identical to "genuinely at the top of the page" -- so dragging
        // anywhere inside it still reads as a refresh pull. Fixed below
        // by also gating on where the drag physically started: only a
        // drag beginning within a small band under the status bar (where
        // the real page content actually starts) is allowed to become a
        // refresh at all. A dialog sitting lower on screen -- which is
        // how virtually every bottom sheet / centered modal is laid out
        // -- never has its drags reach that band in the first place, so
        // it's excluded regardless of its own internal scroll state.
        swipeRefresh = new SwipeRefreshLayout(this) {
            @Override
            public boolean canChildScrollUp() {
                return lastKnownScrollTop > 0 || !lastTouchInPullZone;
            }
        };
        final int pullZonePx = (int) (72 * getResources().getDisplayMetrics().density);
        swipeRefresh.setOnTouchListener((v, event) -> {
            if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
                lastTouchInPullZone = event.getY() <= pullZonePx;
            } else if (event.getActionMasked() == MotionEvent.ACTION_POINTER_DOWN) {
                // A second finger means a pinch (e.g. zooming a map), never
                // a refresh pull -- don't let it get read as one.
                lastTouchInPullZone = false;
            }
            return false; // never consume -- this only samples the gesture's start point
        });
        swipeRefresh.setOnRefreshListener(() -> {
            // A pull-to-refresh is the user explicitly asking for the latest
            // version of the page -- a plain webView.reload() could just
            // serve back a cached copy (with 'fast' caching on) and make the
            // refresh gesture feel like it did nothing. Force one real
            // network round trip here, then restore whichever cache mode
            // this build was configured with for normal navigation.
            webView.getSettings().setCacheMode(WebSettings.LOAD_NO_CACHE);
            webView.reload();
            webView.getSettings().setCacheMode(WebSettings.LOAD_CACHE_ELSE_NETWORK);
        });
        // Off by default -- the page's own JS can still turn it back on
        // at any point via AndroidBridge.setPullToRefreshEnabled(true).
        swipeRefresh.setEnabled(false);
        swipeRefresh.setColorSchemeColors(Color.parseColor(UrlObfuscator.decode(new int[] { 34, 70, 89, 56, 27, 250, 221 }, 257)));
        swipeRefresh.addView(webView, new ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        root.addView(swipeRefresh, webParams);

        // Status-bar and navigation-bar strips. Added before the splash /
        // error / navigation overlays so those still cover the whole screen.
        topScrim = new View(this);
        topScrim.setBackgroundColor(Color.parseColor(UrlObfuscator.decode(new int[] { 49, 1, 101, 95, 187, 157, 249 }, 274)));
        root.addView(topScrim, new FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT, 0, Gravity.TOP));
        bottomScrim = new View(this);
        bottomScrim.setBackgroundColor(Color.parseColor(UrlObfuscator.decode(new int[] { 0, 114, 84, 176, 170, 142, 232 }, 291)));
        root.addView(bottomScrim, new FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT, 0, Gravity.BOTTOM));
        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            androidx.core.graphics.Insets bars = insets.getInsets(
                WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout());
            androidx.core.graphics.Insets ime = insets.getInsets(WindowInsetsCompat.Type.ime());
            applySafeArea(bars.left, bars.top, bars.right, bars.bottom, ime.bottom);
            // Keyboard just opened, or its height changed (suggestion strip,
            // emoji panel, split/floating keyboard on tablets and foldables):
            // once the WebView has finished resizing, nudge the focused field
            // into view. Two passes -- the second catches keyboards that
            // report their final height a beat late.
            boolean imeNow = ime.bottom > 0;
            if (imeNow && (!imeWasVisible || ime.bottom != lastImeBottom)) {
                v.removeCallbacks(scrollFocusedFieldRunnable);
                v.postDelayed(scrollFocusedFieldRunnable, 120);
                v.postDelayed(scrollFocusedFieldRunnable, 420);
            }
            if (imeNow) {
                final int imeB = ime.bottom, barsB = bars.bottom;
                v.postDelayed(() -> panFocusedFieldAboveKeyboard(imeB, barsB), 150);
            } else if (imeWasVisible) {
                panFocusedFieldAboveKeyboard(0, bars.bottom);
            }
            imeWasVisible = imeNow;
            lastImeBottom = ime.bottom;
            return WindowInsetsCompat.CONSUMED;
        });

        final SplashController loading = new NoSplashView(this);
        FrameLayout.LayoutParams loadingParams = new FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT);
        root.addView((View) loading, loadingParams);
        // Always shown, animated styles and "off"/"none" alike -- "off" and
        // "none" just mean the static-icon splash above instead of an
        // animated wordmark, not no splash at all. Either way it holds the
        // screen until revealWebView (see onPageFinished below) swaps it
        // for the site -- and for the static-icon case that swap happens
        // the instant the page is ready, with no forced minimum hold and
        // no loading bar (see StaticIconSplashView.hide() -- no
        // minDisplayMs wait like LoadingSplashView.hide() has).
        loading.show();

        // Shown instead of the WebView's own built-in error page when the
        // main frame fails to load (see onReceivedError below) -- a bare
        // WebView renders Chromium's default "Webpage not available /
        // net::ERR_NAME_NOT_RESOLVED" page, which looks like a broken
        // browser rather than part of this app. Tapping it retries the load.
        // Also auto-retries the moment the OS reports connectivity back
        // (see the ConnectivityManager callback near the bottom of
        // onCreate), so on most devices the user never has to tap anything.
        //
        // Styled as a native system-alert card (title / message / full-width
        // action) rather than a full-bleed branded screen. Colors are pulled
        // at runtime from the phone's own light/dark setting -- not baked
        // into the build -- so this always matches whatever mode the rest of
        // the OS is in. The one exception is the action label, which is
        // tinted with this app's own accent color (see offlineAccentColor)
        // the same way iOS/Android tint a dialog's default action with the
        // app's brand color, so the alert still reads as part of THIS app.
        final boolean offlineIsNightMode =
            (getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK)
                == Configuration.UI_MODE_NIGHT_YES;
        final String offlineScrimColor = offlineIsNightMode ? UrlObfuscator.decode(new int[] { 26, 97, 78, 166, 133, 228, 195, 34, 1 }, 57) : UrlObfuscator.decode(new int[] { 105, 95, 190, 151, 246, 213, 52, 19, 114 }, 74);
        // Slightly translucent (not fully opaque) card/button fills, so a
        // hint of the dimmed scrim behind still shows through -- a cheap
        // stand-in for a true frosted-glass blur, which would need
        // RenderEffect (API 31+) and a snapshot of whatever's behind the
        // card to do for real.
        final String offlineCardBg = offlineIsNightMode ? UrlObfuscator.decode(new int[] { 120, 63, 161, 138, 148, 196, 80, 7, 99 }, 91) : UrlObfuscator.decode(new int[] { 79, 201, 146, 143, 218, 65, 20, 3, 86 }, 108);
        final String offlineTitleColor = offlineIsNightMode ? UrlObfuscator.decode(new int[] { 94, 218, 253, 156, 191, 94, 113 }, 125) : UrlObfuscator.decode(new int[] { 173, 157, 252, 219, 58, 25, 120 }, 142);
        final String offlineSubtitleColor = offlineIsNightMode ? UrlObfuscator.decode(new int[] { 188, 252, 229, 190, 35, 120, 29 }, 159) : UrlObfuscator.decode(new int[] { 147, 250, 172, 56, 110, 126, 47 }, 176);
        final String offlineButtonBg = offlineIsNightMode ? UrlObfuscator.decode(new int[] { 226, 216, 188, 42, 5, 104, 67, 174, 248 }, 193) : UrlObfuscator.decode(new int[] { 241, 201, 83, 107, 120, 41, 186, 239, 139 }, 210);
        final float offlineDensity = getResources().getDisplayMetrics().density;

        // Outer full-screen scrim so the card reads as a dialog sitting on
        // top of the app, matching how a native "no connection" alert
        // dims everything behind it.
        final FrameLayout errorView = new FrameLayout(this);
        errorView.setBackgroundColor(Color.parseColor(UrlObfuscator.decode(new int[] { 192, 50, 96, 112, 30, 78, 223 }, 227)));
        errorView.setVisibility(View.GONE);

        // The error screen is the branded HTML page in ERROR_PAGE_HTML,
        // rendered in its own small WebView that fills errorView. Its
        // Retry / Back buttons call back into native code through the
        // AndroidError interface (registered below, once doRetry exists).
        // Navigation inside it is blocked so it can only ever show that page.
        final WebView errorWeb = new WebView(this);
        errorWeb.setBackgroundColor(Color.parseColor(UrlObfuscator.decode(new int[] { 215, 35, 115, 97, 49, 191, 236 }, 244)));
        errorWeb.setOverScrollMode(View.OVER_SCROLL_NEVER);
        errorWeb.setVerticalScrollBarEnabled(false);
        errorWeb.getSettings().setJavaScriptEnabled(true);
        errorWeb.getSettings().setAllowFileAccess(false);
        errorWeb.getSettings().setAllowContentAccess(false);
        errorWeb.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView v, WebResourceRequest r) {
                return true;
            }
        });
        errorView.addView(errorWeb, new FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));

        FrameLayout.LayoutParams errorParams = new FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT);
        root.addView(errorView, errorParams);

        // Lightweight cover for every navigation AFTER the first one --
        // e.g. tapping a link/button that loads a brand-new page. Without
        // this, the WebView briefly shows its own blank white/black frame
        // between the old page unloading and the new one's first paint.
        // The very first load doesn't need this: the full splash above is
        // already covering that gap.
        final FrameLayout navOverlay = new FrameLayout(this);
        navOverlay.setBackgroundColor(Color.parseColor(UrlObfuscator.decode(new int[] { 38, 66, 37, 4, 231, 198, 217 }, 261)));
        navOverlay.setVisibility(View.GONE);
        final BouncingDotsView navDots = new BouncingDotsView(this, Color.parseColor(UrlObfuscator.decode(new int[] { 53, 4, 21, 66, 211, 128, 145 }, 278)));
        int navDotsWidth = (int) (84 * getResources().getDisplayMetrics().density);
        int navDotsHeight = (int) (36 * getResources().getDisplayMetrics().density);
        FrameLayout.LayoutParams navDotsParams = new FrameLayout.LayoutParams(navDotsWidth, navDotsHeight);
        navDotsParams.gravity = Gravity.CENTER;
        navOverlay.addView(navDots, navDotsParams);
        FrameLayout.LayoutParams navOverlayParams = new FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT);
        root.addView(navOverlay, navOverlayParams);
        // Flips true right after the first successful page load -- used to
        // skip navOverlay on that first load only (the splash already has
        // it covered) and show it on every navigation after that.
        final boolean[] hasLoadedOnce = { false };

        setContentView(root);

        // Holds this window's first frame until the page is ready (or 10s
        // pass, or the offline screen needs to show), so whatever the OS shows
        // while an app opens (the system launch screen on Android 12+) just
        // stays up instead of an in-app black/white/icon screen appearing
        // first. Released by revealWebView / showOffline / the timeout below.
        final boolean[] firstFrameReleased = { false };
        final android.view.ViewTreeObserver.OnPreDrawListener firstFrameHold =
            new android.view.ViewTreeObserver.OnPreDrawListener() {
                @Override
                public boolean onPreDraw() {
                    return firstFrameReleased[0];
                }
            };
        final Runnable releaseFirstFrame = () -> {
            if (firstFrameReleased[0]) return;
            firstFrameReleased[0] = true;
            try {
                root.getViewTreeObserver().removeOnPreDrawListener(firstFrameHold);
            } catch (Exception ignored) {
            }
            root.invalidate();
        };
        root.getViewTreeObserver().addOnPreDrawListener(firstFrameHold);
        new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(releaseFirstFrame, 10000);

        // Makes the system back button/gesture navigate the WebView's own
        // history first (like a real browser back) instead of immediately
        // closing the Activity/exiting the app. Only falls through to the
        // default "exit" behavior once there's no more WebView history to
        // go back to. Implemented via OnBackPressedCallback (not the older
        // onBackPressed() override) so it also plays nicely with Android
        // 13+'s predictive-back swipe gesture, not just a hardware/nav-bar
        // back button press.
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (fsView != null) {
                    exitVideoFullscreen();
                    return;
                }
                if (webView.canGoBack()) {
                    webView.goBack();
                } else {
                    // Nothing left to go back to: this back press would close
                    // the app, so ask first. Never shown while navigating
                    // back through pages or leaving fullscreen video.
                    if (exitDialogPending || (exitDialog != null && exitDialog.isShowing())) return;
                    if (!canShowDialog()) {
                        setEnabled(false);
                        getOnBackPressedDispatcher().onBackPressed();
                        return;
                    }
                    showThemedExitDialog(() -> {
                        setEnabled(false);
                        getOnBackPressedDispatcher().onBackPressed();
                    });
                }
            }
        });

        // Shared retry path for the tap target AND the auto-retry-on-
        // reconnect callback below -- plays a quick shrink-and-fade exit
        // before actually reloading, instead of just vanishing. Only the
        // card shrinks; the scrim behind it just fades out with it.
        final Runnable[] doRetry = new Runnable[1];
        doRetry[0] = () -> {
            loading.show();
            // A retry (whether tapped manually or fired automatically when
            // connectivity comes back) means the previous attempt failed --
            // there's no good cached success response to speed this up with,
            // so this should always be a genuine network attempt rather than
            // risking a cache hit on a stale/incomplete response.
            webView.getSettings().setCacheMode(WebSettings.LOAD_NO_CACHE);
            webView.reload();
            webView.getSettings().setCacheMode(WebSettings.LOAD_CACHE_ELSE_NETWORK);
            errorView.animate()
                .alpha(0f)
                .setDuration(180)
                .withEndAction(() -> {
                    errorView.setVisibility(View.GONE);
                    errorView.setAlpha(1f);
                }).start();
        };

        // Buttons on the branded error page (ERROR_PAGE_HTML) call these.
        errorWeb.addJavascriptInterface(new Object() {
            @JavascriptInterface
            public void retry() {
                runOnUiThread(() -> doRetry[0].run());
            }

            @JavascriptInterface
            public void back() {
                runOnUiThread(() -> getOnBackPressedDispatcher().onBackPressed());
            }
        }, UrlObfuscator.decode(new int[] { 102, 40, 1, 246, 204, 171, 133, 69, 109, 76, 50, 14 }, 295));

        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setAllowFileAccess(true);
        settings.setAllowContentAccess(true);
        // Off by default in WebView -- without this, a page calling
        // navigator.geolocation never even reaches
        // onGeolocationPermissionsShowPrompt below, it just fails silently.
        settings.setGeolocationEnabled(true);
        // Chromium needs a hardware-composited layer to decode and paint a
        // video's first frame -- without this, <video> elements still play
        // fine on tap (audio/duration both work), but the thumbnail/poster
        // frame never renders and falls back to a generic placeholder icon
        // instead of an actual preview. android:hardwareAccelerated="true"
        // on <application> (see AndroidManifest.xml) covers the window as a
        // whole, but WebView's own layer type can still default to
        // software on some OEM builds, so it's set explicitly here too.
        // No <video> found in this site: leave the WebView on its default layer type (smoother scrolling/animation; forcing a hardware layer is only needed for video poster frames).
        // Matches how a normal mobile browser tab handles inline video
        // (autoplay muted, no tap-to-start-decoding required) -- without
        // this, WebView can leave a video's decoder totally uninitialized
        // until playback is explicitly requested, which is the other half
        // of why the poster frame never showed up.
        settings.setMediaPlaybackRequiresUserGesture(false);
        // Without these two, WebView ignores the page's own
        // <meta name="viewport"> tag and lays it out at a fixed desktop
        // width (980px) instead, then scales the result to fit -- which is
        // exactly what produces oversized icons/buttons and title text
        // that overflows off the right edge instead of wrapping, since the
        // page's responsive CSS never actually saw a phone-width viewport.
        settings.setUseWideViewPort(true);
        settings.setLoadWithOverviewMode(true);
        settings.setTextZoom(100);
        // Google's own sign-in pages detect the "; wv)" token (and the
        // "Version/X.X " segment) that stock WebView adds to its user-agent
        // and use it to silently block OAuth inside embedded WebViews -- the
        // page loads fine but the sign-in form's JS just no-ops, so tapping
        // "Next" appears to do nothing. Stripping those two markers from an
        // otherwise-real device UA (rather than hardcoding a fake one) is
        // the standard workaround: it still looks like a legitimate mobile
        // Chrome UA, just without the tell.
        //
        // Heads up: this is not a real fix, it's evading a detection Google
        // runs specifically to prevent embedded WebViews from harvesting
        // Google credentials. It can stop working with no warning on any
        // Chrome/WebView update, and if Google's backend flags the traffic
        // anyway, the consequence isn't just this failing again -- it's the
        // app's OAuth client getting throttled or suspended. The only path
        // Google actually guarantees is native sign-in via Credential
        // Manager (see googleSignInEnabled above), which needs nothing more
        // than a free Web Client ID.
        String defaultUA = settings.getUserAgentString();
        String spoofedUA = defaultUA.replace(UrlObfuscator.decode(new int[] { 6, 124, 12, 236 }, 61), "").replaceAll("Version/[0-9.]+\s", "");
        settings.setUserAgentString(spoofedUA);
        // Needed for Google/Firebase-style "sign in with popup" flows: that JS
        // calls window.open() on the auth provider's URL, and Chrome/Firebase
        // then closes that popup itself once sign-in finishes. Without these
        // two, WebView either can't open the popup at all or opens it detached
        // from the parent page's session, so the auth handler gets a request
        // it can't reconcile and shows "The requested action is invalid".
        settings.setSupportMultipleWindows(true);
        settings.setJavaScriptCanOpenWindowsAutomatically(true);
        // A raw WebView lets the user pinch-zoom and shows on-screen zoom
        // controls like a browser tab -- fine for browsing a random site,
        // but it's the biggest tell that "this is just a web page" for an
        // app that's supposed to read as native. The page's own
        // <meta name="viewport"> (handled above) is what actually controls
        // layout sizing; this only turns off the *manual* pinch/zoom-button
        // affordance on top of that.
        settings.setSupportZoom(false);
        settings.setBuiltInZoomControls(false);
        settings.setDisplayZoomControls(false);
        // A long-press on selectable page text opens Android's native
        // text-selection action mode (the Translate / Copy / Share / more
        // popup) -- another dead giveaway it's a WebView, and it can get
        // stuck sitting on top of the page since nothing in a normal wrapped
        // app ever dismisses it for the user. setLongClickable(false) alone
        // isn't enough on its own to stop Chromium from starting selection
        // (it still owns long-press internally), so this also swallows the
        // long-click event at the View level and returns true to mark it
        // consumed -- taps, scrolling, links, and buttons on the page are
        // untouched since those aren't long-clicks.
        //
        // EDIT_TEXT_TYPE is the one exception: that's the hit-test result
        // for a focused/editable field, and the exact same ActionMode this
        // is built to suppress is also what draws the text-insertion handle
        // and the "paste from clipboard" bubble over a text input. Blanket-
        // consuming every long-click was taking that down too, so a form
        // field's own clipboard tray/suggestion would highlight on tap but
        // never actually insert anything -- returning false here for editable
        // hits lets Chromium handle those long-clicks normally while every
        // other long-click on the page (the actual giveaway) stays swallowed.
        webView.setLongClickable(false);
        webView.setOnLongClickListener(v -> {
            // A focused text field (keyboard up) must never have its long-press
            // swallowed, or the Paste bubble can't appear. The hit-test type
            // alone isn't reliable for that on every WebView version, so the
            // WebView's own "a text editor is active" flag counts too.
            if (webView.onCheckIsTextEditor()) return false;
            WebView.HitTestResult result = webView.getHitTestResult();
            return result == null || result.getType() != WebView.HitTestResult.EDIT_TEXT_TYPE;
        });
        webView.setHapticFeedbackEnabled(false);
        // Chrome's blue overscroll glow at the top/bottom edges is another
        // dead giveaway it's a WebView -- native views don't do that. On
        // Android 12+ (API 31) though, overscroll isn't a glow any more: it's
        // the system's native stretch animation, the exact same elastic
        // pull every native scroll view gets at the top/bottom. So there it's
        // switched ON; older versions would only get the blue glow, so they
        // keep it OFF.
        webView.setOverScrollMode(Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
            ? View.OVER_SCROLL_ALWAYS
            : View.OVER_SCROLL_NEVER);
        // Same idea for the thin scrollbar indicator that flashes on the
        // right edge while scrolling -- that's Android's default browser
        // chrome, not something a native screen shows. Content still
        // scrolls completely normally; this only hides the indicator
        // itself, not the scrolling behavior.
        webView.setVerticalScrollBarEnabled(false);
        webView.setHorizontalScrollBarEnabled(false);
        // Local assets don't need HTTP cache tuning (they load straight off
        // disk already), but any fetch()/XHR calls the page makes to a real
        // backend do. This build uses 'fast': serves a cached response instantly, skipping the network check -- quicker, but can go stale until evicted or refreshed
        // (see the SwipeRefreshLayout listener above, which forces a real
        // network reload either way). setDatabaseEnabled covers older
        // WebSQL-based storage some libraries still fall back to.
        settings.setCacheMode(WebSettings.LOAD_CACHE_ELSE_NETWORK);
        settings.setDatabaseEnabled(true);
        CookieManager.getInstance().setAcceptCookie(true);
        CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true);

        // Exposes vibrate() (used for a light haptic tap on buttons/links,
        // see the injected click-listener script in onPageFinished below)
        // and applyThemeColor() (reads the page's own
        // <meta name="theme-color">, if it has one, and recolors the
        // status/nav bars to match instead of leaving them a fixed color
        // that may clash with the page). Safe to expose here specifically
        // because this WebView only ever loads this app's own bundled
        // local assets, never arbitrary third-party pages.
        webView.addJavascriptInterface(new AndroidBridge(), UrlObfuscator.decode(new int[] { 15, 3, 232, 217, 165, 128, 108, 101, 52, 12, 224, 196, 167 }, 78));

        // Auto-retry the moment the OS reports a usable connection again,
        // so the offline screen clears itself on most devices without
        // waiting for a tap. Falls back to manual "Tap to retry" if this
        // can't register (missing ACCESS_NETWORK_STATE on some OEM ROMs).
        connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager != null) {
            NetworkRequest networkRequest = new NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build();
            networkCallback = new ConnectivityManager.NetworkCallback() {
                @Override
                public void onAvailable(Network network) {
                    super.onAvailable(network);
                    runOnUiThread(() -> {
                        if (errorView.getVisibility() == View.VISIBLE) {
                            doRetry[0].run();
                        }
                    });
                }
            };
            try {
                connectivityManager.registerNetworkCallback(networkRequest, networkCallback);
            } catch (SecurityException ignored) {
            }
        }


        // Brings the WebView in with a little life instead of just
        // flipping it to VISIBLE at full size the instant the splash
        // clears -- a quick scale/fade/settle so the site's first frame
        // feels like it's arriving, not just appearing.
        final Runnable revealWebView = () -> {
            loading.hide();
            releaseFirstFrame.run();
            webView.setVisibility(View.VISIBLE);
            
        };

        // Guaranteed-first-script injection where the installed WebView
        // supports it (Chromium ~M94+, i.e. the large majority of real
        // devices) -- runs before ANY of the page's own scripts, on every
        // navigation, automatically. Falls back to the best-effort
        // onPageStarted injection below on older WebView versions that
        // don't support this feature at all.
        if (WebViewFeature.isFeatureSupported(WebViewFeature.DOCUMENT_START_SCRIPT)) {
            documentStartScriptSupported = true;
            WebViewCompat.addDocumentStartJavaScript(
                webView, SHOW_OPEN_FILE_PICKER_POLYFILL, java.util.Collections.singleton("*"));
            // Zoom lock at document start, so the very first layout already
            // uses the locked mobile viewport instead of the site's own.
            WebViewCompat.addDocumentStartJavaScript(
                webView, ZOOM_LOCK_JS, java.util.Collections.singleton("*"));
            WebViewCompat.addDocumentStartJavaScript(
                webView, VIDEO_LOADER_JS, java.util.Collections.singleton("*"));
            WebViewCompat.addDocumentStartJavaScript(
                webView, ELASTIC_PULL_JS, java.util.Collections.singleton("*"));
            WebViewCompat.addDocumentStartJavaScript(
                webView, NATIVE_FEEL_JS, java.util.Collections.singleton("*"));
            WebViewCompat.addDocumentStartJavaScript(
                webView, RESOURCE_ERROR_JS, java.util.Collections.singleton("*"));
        }

        webView.setWebViewClient(new WebViewClient() {
            // True once the current navigation has failed, so onPageFinished
            // (which WebView still calls after an error) knows not to reveal
            // the WebView underneath the error screen.
            private boolean hasError = false;

            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                super.onPageStarted(view, url, favicon);
                hasError = false;
                view.evaluateJavascript(ZOOM_LOCK_JS, null);
                view.evaluateJavascript(VIDEO_LOADER_JS, null);
                view.evaluateJavascript(DOWNLOAD_NAME_JS, null);
                view.evaluateJavascript(NATIVE_FEEL_JS, null);
                view.evaluateJavascript(ELASTIC_PULL_JS, null);
                view.evaluateJavascript(RESOURCE_ERROR_JS, null);
                // Only needed as a fallback here: if
                // WebViewCompat.addDocumentStartJavaScript (see onCreate)
                // was supported on this device, the polyfill is already
                // guaranteed to run before the page's own scripts and
                // doesn't need re-injecting on every navigation. Where it
                // isn't supported (older WebView versions), this
                // evaluateJavascript call is best-effort -- it usually wins
                // the race against the page's own inline scripts, but
                // isn't a hard guarantee the way the document-start path
                // is.
                if (!documentStartScriptSupported) {
                    view.evaluateJavascript(SHOW_OPEN_FILE_PICKER_POLYFILL, null);
                }
                if (hasLoadedOnce[0]) {
                    navOverlay.animate().cancel();
                    navOverlay.setAlpha(1f);
                    navOverlay.setVisibility(View.VISIBLE);
                }
            }
            // Hands non-web links (mailto:, tel:, sms:, geo:, market:, tg:,
            // intent://, ...) to the system instead of letting the WebView
            // show ERR_UNKNOWN_URL_SCHEME. Plain web/file/data/blob links stay
            // in the WebView. A subframe without a user gesture is never
            // allowed to launch another app (stops ad iframes doing it).
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                Uri uri = request.getUrl();
                if (!request.isForMainFrame() && !request.hasGesture()) return false;
                return handleExternalScheme(uri, view);
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                swipeRefresh.setRefreshing(false);
                refreshDialogTheme();
                navOverlay.animate().cancel();
                navOverlay.animate().alpha(0f).setDuration(180)
                    .withEndAction(() -> navOverlay.setVisibility(View.GONE)).start();
                if (hasError) return;
                revealWebView.run();
                // Short slide-in in the direction of travel (forward from the
                // right, back from the left) on every later navigation.
                // Translation only, on the container; the first load keeps
                // its own reveal animation, and reloads/redirects (same
                // history index) don't animate.
                try {
                    int navIdx = view.copyBackForwardList().getCurrentIndex();
                    if (hasLoadedOnce[0] && lastNavIndex >= 0 && navIdx != lastNavIndex) {
                        float slide = getResources().getDisplayMetrics().density * 28f;
                        float from = navIdx > lastNavIndex ? slide : -slide;
                        android.animation.ObjectAnimator slideIn =
                            android.animation.ObjectAnimator.ofFloat(swipeRefresh, View.TRANSLATION_X, from, 0f);
                        slideIn.setDuration(220);
                        slideIn.setInterpolator(new android.view.animation.DecelerateInterpolator());
                        slideIn.start();
                    }
                    lastNavIndex = navIdx;
                } catch (Exception ignored) {}
                hasLoadedOnce[0] = true;
                view.evaluateJavascript(ZOOM_LOCK_JS, null);
                view.evaluateJavascript(VIDEO_LOADER_JS, null);
                view.evaluateJavascript(DOWNLOAD_NAME_JS, null);
                view.evaluateJavascript(NATIVE_FEEL_JS, null);
                view.evaluateJavascript(ELASTIC_PULL_JS, null);
                // Picks up the page's <meta name="theme-color"> (if it has
                // one) to recolor the system bars, and wires up a light
                // haptic tap on buttons/links -- both no-ops wrapped in
                // try/catch so a page that doesn't have a theme-color tag,
                // or that runs somewhere AndroidBridge isn't defined (the
                // popup WebView below doesn't get it), just silently skips
                // rather than throwing a JS error.
                view.evaluateJavascript(
                    UrlObfuscator.decode(new int[] { 119, 24, 232, 210, 184, 142, 112, 87, 57, 94, 188, 207 }, 95) +
                    // Forces correct mobile scaling regardless of what the
                    // wrapped site itself declares. setUseWideViewPort/
                    // setLoadWithOverviewMode (see WebSettings above) handle
                    // the common case of a page with NO viewport tag at all,
                    // but a page whose own tag is present yet wrong (a
                    // leftover desktop width, a bad initial-scale, or CSS
                    // that just ignores the viewport and sets a fixed pixel
                    // width somewhere) can still render oversized and
                    // overflow off both edges -- exactly what makes a
                    // wrapped site read as "a website, not a native app".
                    // Rewriting the tag to a known-good mobile value, and
                    // clamping the root elements so nothing can force page
                    // width past the viewport, covers those cases too.
                    UrlObfuscator.decode(new int[] { 4, 253, 215, 182 }, 112) +
                    "var vp=document.querySelector('meta[name=\"viewport\"]');" +
                    UrlObfuscator.decode(new int[] { 232, 198, 151, 255, 139, 108, 18, 33, 15, 232, 138, 178, 154, 119, 70, 63, 20, 254, 219, 224, 142, 126, 78, 43, 29, 237, 226, 170, 128, 105, 70, 44, 21, 168, 184, 211, 184, 136, 122, 29, 112, 67, 225, 198, 251, 135, 118, 70, 16, 4, 251, 220, 164, 142, 126, 94, 44, 64, 160, 200, 164, 137, 102, 5, 109, 71, 9, 247, 216, 171, 139, 117, 75, 44, 80, 191, 142, 176, 156, 113, 68, 61, 10, 224, 217, 226, 131, 111, 72, 44, 73, 231, 213, 180, 134, 108, 69, 3, 55, 23, 241, 216, 243, 140, 105, 17, 108, 11 }, 129) +
                    UrlObfuscator.decode(new int[] { 228, 193, 254, 156, 107, 89, 13, 31, 254, 219, 161, 133, 115, 81, 33, 75, 165, 194, 175, 177, 138, 120, 82, 47, 93, 181, 159, 160, 159, 113, 64, 59, 79, 245, 213, 185, 135, 110, 73, 102, 29, 224, 204, 179, 142, 41, 4, 42, 12, 232, 212, 214, 191, 145, 49, 72, 57, 24, 244, 210, 235, 196, 58, 3, 126, 81, 253, 206, 182, 132, 97, 94, 39, 68, 251, 196, 167, 137, 97, 30, 115, 79, 176, 179, 158, 176, 149, 117, 83, 52, 13, 250, 155, 166, 151, 114, 94, 52, 77, 190, 128, 253, 192, 43, 95, 58, 13, 245, 139, 182, 135, 98, 78, 32, 2, 19, 251, 128, 178, 148, 61, 16, 99 }, 146) +
                    UrlObfuscator.decode(new int[] { 202, 164, 201, 33, 123, 81, 62, 9, 246, 223, 183, 140, 57, 81, 48, 0, 214, 222, 180, 157, 106, 64, 57, 46, 242, 227, 173, 192, 32, 121, 26, 5, 237, 198, 179, 143, 150, 122, 107, 53, 30, 237, 201, 183, 133, 98, 115, 61, 11, 181, 152, 249, 148 }, 163) +
                    UrlObfuscator.decode(new int[] { 194, 178, 128, 49, 67, 59, 83, 233, 195, 168, 159, 100, 77, 41, 18, 171, 199, 177, 135, 96, 84, 90, 27, 17, 249, 214, 191, 151, 108, 31, 113, 6, 224, 202, 190, 148, 55, 6, 117, 30, 248, 133, 163, 141, 53, 0, 25, 58, 229, 205, 166, 147, 111, 118, 90, 11, 21, 254, 205, 169, 151, 101, 66, 19, 29, 235, 149, 234 }, 180) +
                    UrlObfuscator.decode(new int[] { 182, 144, 45, 86, 36, 24, 11, 221, 210, 178, 143, 127, 87, 44, 74, 177, 221, 160, 158, 126, 29, 50, 0, 234, 212, 183, 134, 107, 81, 101, 16, 239, 193, 176, 139, 56, 16, 112, 111, 8, 234, 157, 178, 151, 105, 87, 37, 2, 244, 218, 167, 201, 126, 70, 42, 28, 235, 192, 164, 157, 36, 80, 125, 14, 236, 192, 167, 135, 111, 1, 86, 51, 13, 243, 201, 174, 152, 118, 67, 109, 26, 226, 214, 160, 151, 124, 64, 57, 64, 244, 145, 169, 133, 97, 87, 103, 12, 233, 211, 173, 147, 116, 126, 80, 41, 71, 182, 205, 188, 154, 124, 95, 33, 89, 231, 215, 169, 132, 34, 93, 36, 22, 238, 135, 168, 140, 109, 83, 54, 16, 185, 147, 241, 208, 218, 63, 84, 49, 11, 245, 203, 172, 150, 120, 65, 111, 7, 247, 201, 164, 194, 125, 68, 54, 14, 167, 200, 172, 141, 115, 86, 48, 89, 179, 145, 240, 250, 223, 116, 81, 43, 21, 235, 204, 182, 152, 97, 15, 46, 85, 170 }, 197) +
                    UrlObfuscator.decode(new int[] { 178, 154, 119, 70, 63, 20, 254, 219, 224, 133, 105, 74, 46, 71, 233, 215, 182, 128, 106, 71, 1, 9, 233, 243, 218, 245, 143, 111, 19, 98, 5 }, 214) +
                    UrlObfuscator.decode(new int[] { 154, 101, 68, 48, 0, 234, 137, 165, 246, 133, 96 }, 231) +
                    "try{var m=document.querySelector('meta[name=\"theme-color\"]');" +
                    UrlObfuscator.decode(new int[] { 145, 113, 30, 56, 82, 181, 197, 184, 158, 107, 65, 58, 66, 202, 196, 173, 154, 104, 79, 33, 38, 241, 203, 165, 135, 154, 56, 27, 29, 21, 254, 203, 183, 158, 114, 119, 38, 26, 246, 214, 181, 193, 111, 93, 60, 7, 243, 253, 160, 130, 107, 64, 7, 12, 238, 206, 178, 246, 133, 92, 82, 63, 8, 246, 209, 179, 180, 103, 93, 55, 21, 244, 158, 174, 158, 125, 64, 50, 62, 225, 205, 170, 131, 70, 75, 47, 13, 243, 136, 210, 240, 154, 121, 79, 27, 13, 236, 197, 191, 151, 97, 71, 55, 89, 183, 204, 161, 131, 120, 78, 36, 29, 175, 142, 186, 153, 35, 4, 107, 90, 253 }, 248) +
                    UrlObfuscator.decode(new int[] { 116, 75, 38, 18, 230, 204, 235, 135, 40, 91, 66 }, 265) +
                    UrlObfuscator.decode(new int[] { 110, 75, 33, 12, 255, 211, 252, 132, 123, 95, 52, 0, 249, 131, 141, 133, 110, 91, 39, 14, 226, 231, 182, 138, 102, 70, 37, 121, 88, 220, 210, 191, 136, 118, 81, 51, 52, 231, 221, 183, 149, 116, 30, 46, 30, 253, 192, 178, 186, 104, 79, 34, 36, 228, 199, 168, 133, 115, 79, 74, 48, 25, 181, 192 }, 282) +
                    UrlObfuscator.decode(new int[] { 70, 46, 28, 173, 220, 162, 137, 98, 106, 32, 91, 227, 209, 173, 129, 117, 73, 80, 48, 85, 181, 192 }, 48) +
                    UrlObfuscator.decode(new int[] { 55, 1, 13, 190, 222, 180, 154, 115, 87, 101, 44, 203, 142, 162, 146, 96, 17, 53, 82, 234, 194, 175, 158, 103, 76, 38, 19, 168, 192, 168, 134, 111, 68, 46, 43, 56, 239, 211, 182, 170, 118, 81, 57, 2, 189, 249, 178, 134, 121, 30, 41, 2, 226, 195, 185, 194, 126, 65, 41, 2, 234, 211, 237, 139, 111, 78, 90, 44, 42, 245, 223, 174, 145, 55, 5, 127, 89, 227, 218, 188, 149, 127, 88, 96, 4, 226, 197, 175, 155, 64, 66, 47, 2, 236, 215, 239, 211, 41, 36 }, 65) +
                    UrlObfuscator.decode(new int[] { 37, 25, 249, 195, 171, 197, 105, 2, 49, 10, 224, 198, 175, 139, 42, 83, 55, 18, 232, 183, 219, 244, 199, 126, 7, 60, 86, 231, 215, 167, 145, 125, 70, 20, 28, 234, 195, 168, 130, 127, 17, 52 }, 82) +
                    UrlObfuscator.decode(new int[] { 10, 228, 137, 164, 176, 157, 104, 81, 62, 20, 237, 150, 181, 153, 113, 77, 117, 84, 242, 216, 174, 135, 99, 2, 34, 4, 237, 205, 191, 169, 99, 12, 39, 13, 226, 213, 210, 187, 147, 104, 21, 56, 22, 252, 206, 255, 201, 36, 26, 49, 25, 241, 198, 160, 195, 124, 94, 57, 1, 160, 195, 169, 134, 113, 78, 39, 15, 244, 177, 220, 178, 152, 98, 19, 98 }, 99) +
                    UrlObfuscator.decode(new int[] { 29, 245, 154, 178, 152, 110, 71, 35, 66, 226, 196, 173, 141, 127, 105, 35, 76, 231, 205, 162, 149, 146, 123, 83, 40, 85, 254, 214, 187, 130, 123, 80, 58, 7, 215, 221, 181, 130, 107, 67, 56, 66, 182, 153, 225, 132, 110, 68, 45, 13, 172, 209, 181, 172, 150, 53, 88, 52, 25, 236, 213, 178, 152, 97, 26, 55, 29, 242, 197, 162, 139, 99, 88, 14, 6, 236, 197, 162, 136, 113, 13, 120 }, 116) +
                    UrlObfuscator.decode(new int[] { 243, 197, 177, 194, 113, 65, 77, 45, 24, 161, 221, 175, 151, 123, 67, 63, 26, 250, 155, 161, 216, 107, 70, 40, 69, 173, 216, 227, 155, 109, 83, 51, 23, 234, 131, 172, 148, 108, 115, 5, 43, 29, 233, 154, 184, 197, 100, 24, 60, 26, 247, 215, 169, 191, 105, 6, 106, 68, 172, 131, 242, 158, 102, 84, 101, 6, 190, 209, 239, 137, 145, 122, 88, 36, 52, 252, 145, 255, 222, 49, 28, 111, 26, 244, 153, 177, 211, 62, 81, 48, 9, 182, 153, 225, 149, 99, 81, 49, 17, 236, 129, 174, 170, 146, 113, 7 }, 133) +
                    UrlObfuscator.decode(new int[] { 224, 212, 166, 211, 100, 12, 35, 65, 253, 216, 174, 152, 126, 91, 33, 9, 225, 141, 165, 200, 51, 13, 34, 118, 80, 238, 204, 183, 147, 109, 16, 112, 90, 178, 157, 232, 155, 119, 24, 57, 64, 225, 201, 165, 141, 125, 64, 123, 85, 172, 214, 166, 150, 116, 82, 81, 126, 19, 233, 215, 182, 194 }, 150) +
                    UrlObfuscator.decode(new int[] { 209, 167, 151, 36, 66, 46, 92, 246, 177, 210, 184, 146, 124, 78, 49, 70, 164, 137, 165, 149, 97, 65, 52, 54, 227, 193, 172, 152, 35, 92, 18, 91, 218, 143, 255, 213, 56, 84, 32, 18, 95, 236, 128, 172, 154, 104, 74, 61, 62, 248, 193, 252, 133, 73, 1, 13, 67, 191, 157, 229, 199, 109, 20, 56, 6, 244, 214, 161, 170, 108, 85, 104, 41, 37, 172, 225, 247, 203, 41, 17, 123, 20, 249, 137, 163, 147, 99, 67, 42, 39, 227, 216, 227, 156, 82, 26, 26, 74, 180, 148, 234, 217 }, 167) +
                    UrlObfuscator.decode(new int[] { 209, 177, 222, 124, 71, 29, 19, 223, 152, 189, 199, 113, 80, 34, 25, 199, 201, 137, 206, 98, 13, 63, 30, 232, 211, 241, 191, 179, 52, 89, 54, 80, 177, 197, 179, 129, 97, 65, 60, 81, 254, 218, 162, 129, 55, 89, 47, 29, 253, 213, 168, 158, 118, 25, 48, 77, 231, 165, 217, 241, 158, 33, 88, 53, 84, 246, 140, 180, 152, 110, 9, 44, 75 }, 184) +
                    UrlObfuscator.decode(new int[] { 191, 137, 117, 6, 45, 1, 251, 159, 167, 149, 145, 125, 73, 53, 20, 244, 145, 187, 222, 109, 67, 53, 1, 178, 217, 237, 137, 123, 67, 47, 31, 227, 198, 166, 207, 104, 12, 63, 13, 191, 236, 161, 171, 150, 51, 81, 58, 2, 177, 136, 251, 187, 116, 64, 59, 92, 252, 217, 161, 198, 63, 25, 126, 70, 231, 129, 238, 221, 119, 65, 55, 23, 243, 206, 159, 246, 147, 32, 10, 108, 70, 191, 135, 241, 207, 51, 20, 123, 90, 254, 129, 186, 130, 95, 95, 56, 0, 230, 192, 238, 212, 50, 10, 121, 28, 187, 237, 219, 169, 137, 105, 84, 121, 95, 180, 145, 254, 156, 59, 81, 127, 2, 166, 133, 165, 196, 104, 4, 46, 65, 172, 206, 237, 135, 45, 64, 104, 91, 2, 165 }, 201) +
                    UrlObfuscator.decode(new int[] { 188, 150, 106, 31, 32, 20, 230, 147, 185, 204, 32, 20, 37, 81, 239, 195, 171, 128, 102, 9, 42, 0, 234, 196, 182, 137, 59, 116, 21, 118, 85, 224, 204, 184, 138, 55, 85, 104, 4, 242, 192, 162, 149, 39, 73, 40, 24, 200, 197, 164, 152, 114, 82, 32, 0, 208, 214, 184, 140, 154, 54, 94, 52, 26, 243, 215, 131, 156, 75, 28, 122, 17, 243, 210, 187, 136, 124, 66, 57, 5, 238, 234, 167, 139, 105, 87, 109, 88, 235, 199, 232, 188, 216, 59, 95, 117, 27, 167, 133, 231, 216, 44, 13, 122, 0, 244, 196, 186, 156, 99, 12, 35, 15, 241, 128, 164, 207, 62, 89 }, 218) +
                    UrlObfuscator.decode(new int[] { 157, 107, 91, 104, 19, 187, 213, 165, 145, 113, 68, 104, 56, 27, 233, 255, 180, 151, 105, 77, 35, 19, 241, 231, 167, 139, 125, 85, 103, 10, 226, 207, 190, 135, 108, 70, 51, 72, 231, 203, 167, 155, 125, 92, 91, 49, 30, 233, 214, 191, 151, 108, 25, 50, 26, 247, 198, 191, 148, 126, 91, 11, 1, 233, 198, 175, 135, 124, 14, 104, 6, 235, 207, 173, 147, 41, 36 }, 235) +
                    UrlObfuscator.decode(new int[] { 149, 125, 18, 45, 94, 177, 158, 229, 218, 33, 11, 104, 90, 251, 128, 191, 199, 59, 4, 124, 80, 176, 140, 177, 202, 100, 9, 113, 78, 78, 175, 137, 246, 143, 52, 91, 113, 88, 164, 128, 225, 205, 34, 31, 102, 70, 252, 200, 184, 158, 120, 71, 104, 64, 165, 149, 241, 211, 55, 17, 117, 120, 69 }, 252) +
                    UrlObfuscator.decode(new int[] { 127, 73, 63, 31, 251, 198, 231, 193, 38, 66, 37, 4, 231, 198, 217, 249, 198, 97, 0 }, 269) +
                    UrlObfuscator.decode(new int[] { 104, 92, 46, 91, 233, 220, 182, 147, 84, 82, 105, 21, 231, 223, 179, 155, 103, 66, 34, 67, 163, 210, 188, 149, 127, 94, 50, 2, 240, 129, 168, 226, 142, 116, 95, 48, 56, 254, 144, 254, 205, 124, 82, 123, 26, 176, 141, 242, 153, 100, 66, 47, 5, 254, 134, 152, 185, 100, 74, 39, 16, 238, 201, 219, 156, 154, 80, 90, 41, 13, 177, 204, 161, 156, 122, 87, 61, 6, 190, 240, 145, 140, 98, 79, 56, 6, 225, 195, 132, 130, 72, 66, 49, 21, 189, 247, 133, 156, 146, 127, 72, 54, 17, 243, 244, 167, 157, 119, 85, 52, 94, 238, 222, 189, 128, 114, 122, 40, 15, 226, 228, 164, 135, 104, 69, 51, 15, 10, 240, 217, 244, 147, 51, 2, 37, 10, 245, 212, 160, 144, 122, 25, 53, 70, 245, 208, 177, 208 }, 286) +
                    UrlObfuscator.decode(new int[] { 71, 54, 28, 245, 242, 168, 198, 36, 23 }, 52) +
                    UrlObfuscator.decode(new int[] { 44, 2, 171, 131, 182, 137, 145, 122, 82, 43, 85, 197, 230, 185, 153, 114, 71, 59, 26, 246, 243, 183, 173, 97, 88, 34, 15, 163, 210, 191, 142, 104, 65, 43, 20, 172, 254, 159, 190, 144, 121, 78, 52, 19, 253, 250, 176, 180, 122, 65, 61, 22, 172, 196, 189, 155, 104, 23, 47, 5, 234, 221, 170, 131, 107, 80, 109, 3, 229, 196, 250, 168, 152, 114, 79, 22, 16, 235, 195, 179, 155, 113, 65, 122, 86, 246, 192, 173, 152, 127, 66, 36, 78, 164, 212, 163, 139, 96, 97, 37, 77, 244, 237, 203, 184, 213, 32 }, 69) +
                    UrlObfuscator.decode(new int[] { 32, 20, 230, 147, 190, 144, 99, 91, 26, 30, 177, 155, 241, 141, 103, 68, 51, 8, 225, 205, 182, 207, 97, 123, 90, 24, 10, 254, 212, 173, 180, 126, 69, 33, 17, 253, 215, 163, 216, 40, 90, 34, 25, 232, 194, 186, 156, 102, 84, 49, 67, 175, 196, 180, 142, 156, 106, 84, 51, 21, 178, 144, 163, 129, 119, 71, 116, 29, 175, 245, 177, 155, 107, 3, 34, 4, 253, 129, 225, 220, 111, 67, 108, 13, 175, 205, 161, 172, 138, 73, 79, 103, 66, 169, 136, 254, 132, 112, 64, 38, 0, 255, 139, 163, 143, 126, 88, 31, 25, 180, 198, 252, 149, 96, 74, 39, 32, 230, 136, 150, 229, 128, 48, 64, 57, 24, 232, 195, 163, 135, 113, 9, 38, 3, 229, 202, 226, 157, 109, 88, 57, 0, 254, 194, 252, 145, 118, 86, 39, 28, 169, 164, 201, 180, 146, 127, 85, 46, 86, 246, 210, 177, 177, 101, 87, 63, 4, 195, 199, 190, 152, 110, 68, 44, 26, 175, 129, 183, 129, 112, 75, 59, 5, 88, 178, 206, 185, 149, 126, 123, 63, 94, 173 }, 86) +
                    UrlObfuscator.decode(new int[] { 19, 244, 220, 191, 149, 99, 83, 96, 61, 25, 201, 129, 181, 143, 117, 84, 108, 0, 244, 198, 243, 144, 118, 99, 44, 6, 232, 200, 246, 140, 124, 70, 36, 18, 236, 203, 173, 202, 40, 91, 86, 56, 85, 254, 220, 142, 208, 106, 82, 34, 0, 230, 221, 233, 147, 119, 123, 115, 30, 233, 223, 158, 128, 101, 66, 41, 16, 240, 139, 164, 148, 110, 124, 74, 52, 19, 245, 146, 240, 131, 117, 81, 1, 73, 253, 199, 189, 156, 52, 93, 40, 2, 239, 232, 174, 192, 46, 29, 56, 72, 178, 151, 241, 201, 196, 99, 6 }, 103) +
                    UrlObfuscator.decode(new int[] { 22, 242, 193, 245, 185, 102, 70, 48, 4, 230, 193, 163, 163, 105, 89, 44, 26, 241, 195, 183, 204, 97, 69, 18, 3, 23, 251, 217, 245, 213, 117, 91, 43, 18, 228, 195, 177, 219, 118, 94, 51, 26, 227, 200, 162, 159, 36, 77, 39, 4, 243, 200, 161, 141, 118, 100, 44, 58, 19, 248, 210, 175, 214, 98, 89, 35, 2, 231, 221, 177, 135, 101, 85, 60, 84, 249, 222, 190, 143, 37, 73, 51, 18, 247, 205, 161, 151, 117, 69, 121, 55, 17, 232, 222, 168, 195, 67, 16, 53, 25, 245, 192, 161, 214, 60, 8, 61, 25, 245, 199, 175, 206, 36, 0, 34, 4, 240, 194, 239, 149, 104, 122, 83, 56, 91, 198, 199, 240, 195 }, 120) +
                    UrlObfuscator.decode(new int[] { 224, 206, 239, 130, 106, 71, 54, 15, 228, 206, 203, 240, 159, 115, 95, 35, 80, 246, 210, 161, 213, 89, 70, 38, 16, 228, 198, 161, 131, 67, 73, 57, 12, 250, 209, 163, 151, 44, 65, 37, 50, 227, 247, 219, 185, 213, 53, 85, 59, 11, 242, 196, 163, 145, 59, 86, 62, 19, 250, 195, 168, 130, 127, 4, 43, 7, 227, 223, 233, 159, 98, 86, 53, 18, 22, 252, 200, 168, 158, 105, 3, 44, 5, 227, 208, 248, 146, 102, 69, 34, 6, 236, 216, 184, 142, 76, 64, 36, 19, 227, 215, 254, 184, 37, 66, 44, 62, 13, 238, 155, 247, 221, 106, 76, 46, 26, 240, 147, 142, 143, 56, 11, 50, 13, 236, 216, 168, 130, 33, 77, 110, 29, 248, 217 }, 137) +
                    UrlObfuscator.decode(new int[] { 231, 196, 187, 150, 98, 86, 60, 91, 247, 152, 171, 146 }, 154) +
                    UrlObfuscator.decode(new int[] { 223, 184, 144, 115, 78, 32, 77, 165, 212, 171, 143, 100, 112, 73, 115, 35, 196, 219, 183, 156, 101, 89, 60, 16, 219, 211, 161, 132, 102, 77, 62, 46, 228, 223, 167, 140, 46, 93, 50, 13, 237, 198, 174, 151, 209, 65, 98, 61, 21, 254, 203, 183, 158, 114, 125, 53, 3, 230, 216, 179, 156, 76, 66, 57, 5, 238, 148, 188, 149, 115, 64, 127 }, 171) +
                    UrlObfuscator.decode(new int[] { 216, 180, 153, 108, 85, 50, 24, 225, 154, 178, 150, 117, 117, 57, 11, 227, 216, 135, 131, 122, 92, 34, 8, 224, 214, 235, 197, 98, 76, 86, 61, 22, 187, 151, 188, 140, 118, 84, 34, 28, 251, 221, 250, 148, 57, 84 }, 188) +
                    "var t=e.target;var el=t&&t.closest?t.closest('button,a,[role=\"button\"],input[type=\"button\"],input[type=\"submit\"]'):null;" +
                    UrlObfuscator.decode(new int[] { 164, 138, 35, 79, 37, 78, 161, 209, 172, 138, 103, 77, 54, 78, 62, 240, 217, 174, 148, 115, 93, 26, 5, 255, 209, 179, 150, 52, 23, 17, 1, 234, 223, 163, 130, 110, 107, 58, 14, 226, 194, 161, 205, 116, 72, 34, 45, 31, 233, 217, 242, 129, 88, 86, 51, 4, 250, 221, 183, 176, 99, 89, 43, 9, 232, 130, 189, 131, 107, 90, 38, 18, 224, 140, 234, 217, 124 }, 205) +
                    UrlObfuscator.decode(new int[] { 163, 209, 104, 73, 47, 28, 177, 140, 171, 136, 119, 82, 38, 18, 248, 135, 171, 196, 119, 86 }, 222) +
                    // Polyfills the Web Share API on top of the real Android
                    // share sheet -- most WebView builds don't implement
                    // navigator.share at all, so a page's own "Share" button
                    // either does nothing or falls back to a hand-rolled
                    // copy-link menu instead of the native chooser. Only
                    // installed when the page doesn't already have a working
                    // navigator.share (some newer WebView versions do).
                    UrlObfuscator.decode(new int[] { 155, 124, 84, 55, 2, 236, 129, 191, 142, 104, 65, 43, 20, 172, 224, 174, 187, 140, 114, 85, 63, 56, 235, 209, 179, 145, 112, 18, 117, 51, 255, 212, 189, 129, 100, 72, 9, 24, 224, 204, 160, 131, 43, 87, 43, 3, 243, 197, 153, 248, 220, 114, 90, 44, 16, 255, 214, 162, 154, 102, 29, 33, 25, 241, 221, 171, 196, 119 }, 239) +
                    UrlObfuscator.decode(new int[] { 110, 126, 72, 52, 27, 250, 206, 182, 138, 57, 69, 61, 21, 225, 215, 236, 150, 122, 64, 46, 24, 226, 197, 167, 192, 99, 71, 49, 5, 170, 217 }, 256) +
                    UrlObfuscator.decode(new int[] { 117, 81, 59, 15, 176, 200, 170, 158, 104, 84, 59, 29, 248, 159 }, 273) +
                    UrlObfuscator.decode(new int[] { 86, 51, 25, 4, 223, 211, 184, 137, 117, 80, 60, 53, 228, 220, 176, 148, 119, 31, 35, 7, 239, 223, 169, 195, 89, 93, 58, 14, 232, 194, 236, 135, 99, 85, 33, 113, 10, 244, 200, 183, 159, 101, 68, 112, 81, 188, 152, 128, 134, 99, 89, 33, 9, 165, 200, 170, 158, 104, 6, 51, 3, 253, 208, 191, 158, 38, 7, 22, 114, 46, 232, 201, 179, 151, 127, 31, 50, 20, 224, 210, 252, 132, 98, 67, 50, 17, 171, 140, 227, 192, 51 }, 290) +
                    UrlObfuscator.decode(new int[] { 74, 50, 2, 224, 198, 189, 210, 65, 66, 32, 3, 228, 223, 174, 196, 123, 77, 52, 9, 233, 210, 166, 202, 40, 27, 66, 61, 28, 232, 216, 178, 209, 125, 30, 45, 7, 241, 199, 167, 131, 126, 15, 30, 31, 227, 198, 163, 154, 109, 9, 52, 0, 238, 198, 161, 149, 40, 122, 23, 102, 1, 230, 129 }, 56) +
                    UrlObfuscator.decode(new int[] { 39, 9, 241, 207, 162, 133, 119, 77, 51, 78, 28, 255, 211, 143, 147, 123, 75, 61, 74, 240, 192, 186, 144, 102, 88, 63, 1, 166, 132, 183, 153, 111, 93, 61, 21, 232, 133, 176, 145, 119, 68, 123, 34, 69 }, 73) +
                    UrlObfuscator.decode(new int[] { 39, 4, 251, 214, 162, 150, 124, 27, 55, 88, 235, 210 }, 90) +
                    // Reports the scrollTop of whichever element just
                    // scrolled -- document or any nested panel -- so native
                    // knows whether a pull-to-refresh gesture is actually
                    // safe (see SwipeRefreshLayout override above). Scroll
                    // events don't bubble, so this has to be a capture
                    // listener on window to see scrolling from any
                    // descendant, not just the document itself. Throttled
                    // with a trailing rAF flag so a fast scroll doesn't
                    // spam the JS bridge with a call per pixel.
                    UrlObfuscator.decode(new int[] { 31, 248, 208, 179, 142, 96, 13, 101, 20, 235, 207, 164, 176, 137, 51, 99, 4, 27, 247, 220, 165, 153, 124, 80, 0, 17, 227, 223, 163, 130, 79, 67, 62, 4, 237, 129, 188, 145, 108, 74, 39, 13, 246, 142, 224, 129, 156, 114, 95, 40, 22, 241, 211, 133, 150, 102, 92, 62, 29, 210, 192, 187, 131, 104, 22, 62, 27, 253, 194, 253 }, 107) +
                    UrlObfuscator.decode(new int[] { 10, 250, 200, 249, 136, 114, 88, 49, 29, 253, 213, 236, 150, 110, 66, 62, 9, 167, 198, 168, 155, 115, 118, 42, 23, 190, 196, 160, 140, 140, 123, 6 }, 124) +
                    UrlObfuscator.decode(new int[] { 250, 197, 165, 142, 102, 95, 105, 7, 225, 192, 134, 148, 100, 78, 75, 18, 20, 239, 207, 191, 151, 125, 69, 126, 82, 231, 208, 160, 158, 124, 67, 105, 65, 234, 222, 164, 138, 124, 78, 41, 11, 172, 198, 235, 154 }, 141) +
                    UrlObfuscator.decode(new int[] { 247, 219, 244, 139, 127, 87, 60, 30, 248, 210, 253, 129, 119, 69, 37, 29, 224, 150, 188, 142, 100, 77, 33, 9, 225, 152, 176, 145, 119, 68, 123 }, 158) +
                    UrlObfuscator.decode(new int[] { 221, 171, 156, 121, 78, 57, 29, 201, 201, 175, 136, 101, 87, 43, 14, 238, 217, 204, 188, 145, 126, 18, 63, 13, 249, 213, 161, 157, 124, 92, 121, 89, 244, 222, 168, 130, 111, 67, 39, 15, 186, 192, 164, 136, 112, 71, 122 }, 175) +
                    UrlObfuscator.decode(new int[] { 182, 190, 140, 61, 89, 55, 71, 177, 221, 249, 130, 116, 70, 52, 23, 229, 150, 233, 139, 35, 88, 42, 24, 238, 205, 179, 200, 107, 75, 39, 7, 213, 217, 207, 187, 192, 33, 6, 107, 80, 167, 210, 248, 129, 117, 65, 53, 20, 228, 149, 170, 130, 111, 94, 39, 12, 230, 211, 232, 150, 103, 81, 45, 13, 236, 246, 208, 186, 185, 119, 95, 52, 29, 249, 194, 238 }, 192) +
                    UrlObfuscator.decode(new int[] { 167, 145, 125, 14, 57, 3, 251, 151, 172, 132, 56, 67, 41, 74, 240, 193, 179, 143, 147, 114, 105, 51, 11, 160, 137, 227 }, 209) +
                    UrlObfuscator.decode(new int[] { 148, 96, 82, 31, 46, 18, 239, 134, 174, 150, 104, 9, 102, 78, 253, 213, 250, 129, 127, 92, 111, 80, 177, 199, 171, 154, 124, 119, 41, 22, 173, 216, 174, 128, 115, 107, 110, 50, 15, 166, 202, 182, 139, 44, 95, 51, 92, 228, 219, 191, 148, 96, 89, 99, 45, 229, 206, 187, 135, 110, 66, 7, 22, 234, 198, 166, 133, 217, 56, 124, 50, 31, 232, 214, 177, 147, 84, 71, 61, 23, 245, 212, 254, 157, 107, 93, 35, 25, 254, 250, 171, 149, 105, 73, 40, 55, 237, 209, 233, 164, 191, 115, 88, 41, 21, 240, 220, 149, 132, 124, 80, 52, 23, 191, 194, 170, 158, 98, 94, 63, 57, 234, 218, 168, 138, 105, 112, 44, 18, 169, 212, 208, 174, 129, 44, 18, 97, 4, 229 }, 226) +
                    UrlObfuscator.decode(new int[] { 142, 59, 10, 45, 67, 250, 223, 185, 142, 35, 18, 53, 26, 229, 196, 176, 128, 106, 9, 37, 118, 5, 224 }, 243) +
                    UrlObfuscator.decode(new int[] { 121, 10, 106, 72, 187 }, 260),
                    null);
            }

            // Serves EmbeddedAssets (compiled into classes.dex) in place of the OS
            // resolving file:///android_asset/ requests against a real
            // assets/ folder. Only requests under EMBED_HOST are handled
            // here; everything else (a page's own https:// fetch() calls,
            // etc.) falls through to the default behavior untouched.
            // Decoded (and, if enabled, decrypted) assets are kept in memory so
            // each file is decoded once instead of on every request. Capped at
            // min(32 MB, 1/8 of the app's heap); least-recently-used files drop out.
            private final android.util.LruCache<String, byte[]> embedCache =
                new android.util.LruCache<String, byte[]>(
                    (int) Math.min(32L * 1024 * 1024, Runtime.getRuntime().maxMemory() / 8)) {
                    @Override protected int sizeOf(String key, byte[] value) { return value.length; }
                };

            @Override
            public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
                String url = request.getUrl().toString();
                if (!url.startsWith(EMBED_HOST)) return super.shouldInterceptRequest(view, request);

                String rel = url.substring(EMBED_HOST.length());
                int cut = rel.length();
                int q = rel.indexOf('?'); if (q >= 0 && q < cut) cut = q;
                int h = rel.indexOf('#'); if (h >= 0 && h < cut) cut = h;
                rel = rel.substring(0, cut);
                if (rel.isEmpty()) rel = UrlObfuscator.decode(new int[] { 124, 90, 55, 23, 233, 158, 167, 154, 96, 64 }, 277);

                byte[] data = embedCache.get(rel);
                if (data == null) {
                    data = EmbeddedAssets.get(rel);
                    if (data != null) embedCache.put(rel, data);
                }
                if (data == null) return super.shouldInterceptRequest(view, request);

                return new WebResourceResponse(guessEmbeddedMime(rel), UrlObfuscator.decode(new int[] { 115, 17, 34, 174, 154 }, 294), new ByteArrayInputStream(data));
            }

            private String guessEmbeddedMime(String rel) {
                String lower = rel.toLowerCase();
                if (lower.endsWith(UrlObfuscator.decode(new int[] { 18, 51, 14, 244, 212 }, 60)) || lower.endsWith(UrlObfuscator.decode(new int[] { 99, 4, 255, 199 }, 77))) return UrlObfuscator.decode(new int[] { 42, 24, 228, 207, 245, 145, 108, 90, 58 }, 94);
                if (lower.endsWith(UrlObfuscator.decode(new int[] { 65, 237, 222, 191 }, 111))) return UrlObfuscator.decode(new int[] { 244, 250, 198, 169, 211, 120, 73, 42 }, 128);
                if (lower.endsWith(UrlObfuscator.decode(new int[] { 191, 218, 188 }, 145)) || lower.endsWith(UrlObfuscator.decode(new int[] { 140, 172, 138, 140 }, 162))) return UrlObfuscator.decode(new int[] { 210, 162, 129, 124, 70, 45, 12, 248, 194, 165, 135, 39, 77, 39, 19, 229, 208, 161, 147, 105, 111, 74 }, 179);
                if (lower.endsWith(UrlObfuscator.decode(new int[] { 234, 137, 113, 78, 46 }, 196))) return UrlObfuscator.decode(new int[] { 180, 132, 99, 94, 56, 19, 238, 218, 164, 131, 101, 5, 35, 27, 232, 200 }, 213);
                if (lower.endsWith(UrlObfuscator.decode(new int[] { 200, 118, 82, 36 }, 230))) return UrlObfuscator.decode(new int[] { 158, 123, 84, 51, 22, 189, 194, 166, 136, 37, 85, 33, 7 }, 247);
                if (lower.endsWith(UrlObfuscator.decode(new int[] { 38, 87, 40, 2 }, 264))) return UrlObfuscator.decode(new int[] { 112, 85, 54, 17, 240, 155, 163, 156, 118 }, 281);
                if (lower.endsWith(UrlObfuscator.decode(new int[] { 1, 36, 29, 235 }, 47)) || lower.endsWith(UrlObfuscator.decode(new int[] { 110, 53, 14, 248, 219 }, 64))) return UrlObfuscator.decode(new int[] { 56, 29, 238, 201, 168, 195, 97, 90, 44, 15 }, 81);
                if (lower.endsWith(UrlObfuscator.decode(new int[] { 76, 230, 201, 217 }, 98))) return UrlObfuscator.decode(new int[] { 26, 255, 208, 183, 138, 33, 74, 37, 13 }, 115);
                if (lower.endsWith(UrlObfuscator.decode(new int[] { 170, 212, 167, 131, 112 }, 132))) return UrlObfuscator.decode(new int[] { 252, 217, 178, 149, 116, 31, 56, 11, 239, 220 }, 149);
                if (lower.endsWith(UrlObfuscator.decode(new int[] { 136, 178, 139, 101, 68, 115 }, 166))) return UrlObfuscator.decode(new int[] { 209, 185, 155, 96, 28, 37, 30, 246, 201, 252 }, 183);
                if (lower.endsWith(UrlObfuscator.decode(new int[] { 230, 144, 105, 67, 34 }, 200))) return UrlObfuscator.decode(new int[] { 191, 151, 121, 66, 122, 3, 252, 212, 183 }, 217);
                if (lower.endsWith(UrlObfuscator.decode(new int[] { 196, 125, 92, 33 }, 234))) return UrlObfuscator.decode(new int[] { 157, 117, 87, 44, 88, 226, 193, 178 }, 251);
                if (lower.endsWith(UrlObfuscator.decode(new int[] { 34, 66, 41, 6 }, 268))) return UrlObfuscator.decode(new int[] { 116, 81, 58, 29, 252, 151, 175, 219, 124, 87, 60, 28 }, 285);
                return UrlObfuscator.decode(new int[] { 82, 34, 1, 252, 198, 173, 140, 120, 66, 37, 7, 167, 200, 165, 145, 97, 87, 111, 18, 244, 237, 219, 188, 145 }, 51);
            }

            // API 23+; covers essentially every device in real use. Not
            // calling super here on purpose -- the platform's default
            // implementation of this overload forwards main-frame errors
            // into the deprecated overload below, which would double-fire
            // showOffline().
            @Override
            public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                if (request.isForMainFrame()) {
                    showOffline(String.valueOf(error.getDescription()), request.getUrl().toString());
                } else {
                    reportFrameFailure(view, request.getUrl().toString());
                }
            }

            // The server answered, but with an error status (404, 500, ...).
            // 401 / 407 are login challenges, not failures, so they pass through.
            @Override
            public void onReceivedHttpError(WebView view, WebResourceRequest request, WebResourceResponse errorResponse) {
                super.onReceivedHttpError(view, request, errorResponse);
                int status = errorResponse.getStatusCode();
                if (status < 400 || status == 401 || status == 407) return;
                if (request.isForMainFrame()) {
                    showOffline(UrlObfuscator.decode(new int[] { 12, 55, 214, 241, 224 }, 68) + status, request.getUrl().toString());
                } else {
                    reportFrameFailure(view, request.getUrl().toString());
                }
            }

            // Fallback for minSdk 21-22, where the platform never calls the
            // overload above at all.
            @Override
            public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                super.onReceivedError(view, errorCode, description, failingUrl);
                showOffline(description, failingUrl);
            }

            private void showOffline(String description, String failingUrl) {
                hasError = true;
                navOverlay.animate().cancel();
                navOverlay.setVisibility(View.GONE);
                loading.hideImmediate();
                releaseFirstFrame.run();
                webView.setVisibility(View.GONE);
                
                // Re-render each time so the error code / host are current and
                // the page's Retry button is back to its idle state.
                int errBg = Color.parseColor(isOfflineError(description) ? UrlObfuscator.decode(new int[] { 118, 50, 213, 244, 151, 182, 73 }, 85) : UrlObfuscator.decode(new int[] { 69, 181, 229, 243, 163, 49, 98 }, 102));
                errorView.setBackgroundColor(errBg);
                errorWeb.setBackgroundColor(errBg);
                errorWeb.loadDataWithBaseURL(UrlObfuscator.decode(new int[] { 31, 226, 193, 164, 128, 40, 30, 127, 10, 252, 223, 163, 153, 36, 69, 39, 4, 231, 201, 235 }, 119),
                    renderErrorPage(description, failingUrl), UrlObfuscator.decode(new int[] { 252, 194, 190, 145, 43, 75, 54, 12, 236 }, 136), UrlObfuscator.decode(new int[] { 204, 236, 145, 219, 45 }, 153), null);
                errorView.setVisibility(View.VISIBLE);
                errorView.setAlpha(0f);
                errorView.animate().alpha(1f).setDuration(220).start();
            }
        });

        webView.setWebChromeClient(new WebChromeClient() {
            // WebView paints a grey rounded box with a black play icon for any
            // <video> that has no poster while it loads. Returning a fully
            // transparent 1x1 bitmap removes it, so the loading spinner
            // (VIDEO_LOADER_JS) shows over the page's own background instead.
            @Override
            public boolean onJsAlert(WebView view, String url, String message, android.webkit.JsResult result) {
                return showJsAlert(url, message, result);
            }

            @Override
            public boolean onJsConfirm(WebView view, String url, String message, android.webkit.JsResult result) {
                return showJsConfirm(url, message, result);
            }

            @Override
            public boolean onJsPrompt(WebView view, String url, String message, String defaultValue, android.webkit.JsPromptResult result) {
                return showJsPrompt(url, message, defaultValue, result);
            }

            @Override
            public void onShowCustomView(View view, WebChromeClient.CustomViewCallback callback) {
                enterVideoFullscreen(view, callback);
            }

            @Override
            public void onHideCustomView() {
                exitVideoFullscreen();
            }

            @Override
            public Bitmap getDefaultVideoPoster() {
                Bitmap b = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888);
                b.eraseColor(Color.TRANSPARENT);
                return b;
            }

            @Override
            public boolean onShowFileChooser(WebView view, ValueCallback<Uri[]> callback, FileChooserParams params) {
                if (filePathCallback != null) {
                    filePathCallback.onReceiveValue(null);
                }
                filePathCallback = callback;

                Intent intent = params.createIntent();
                try {
                    startActivityForResult(intent, FILE_CHOOSER_REQUEST_CODE);
                } catch (ActivityNotFoundException e) {
                    filePathCallback = null;
                    return false;
                }
                return true;
            }

            // Handles the site itself asking for camera/mic access -- a
            // "Scan QR" feature or a video-chat widget using getUserMedia().
            // Without this override the WebView auto-denies every such
            // request, which is what was showing as "Camera permission
            // denied or unavailable" -- the app never even asked Android for
            // the underlying runtime permission, regardless of whether the
            // person would have said yes. Camera and mic are requested
            // independently of each other: a page that only asked for one
            // only gets asked (and only ends up granted) for that one, even
            // if it later asks for the other too.
            @Override
            public void onPermissionRequest(PermissionRequest request) {
                java.util.List<String> requested = java.util.Arrays.asList(request.getResources());
                java.util.List<String> neededAndroidPerms = new java.util.ArrayList<>();
                if (requested.contains(PermissionRequest.RESOURCE_VIDEO_CAPTURE)) {
                    neededAndroidPerms.add(Manifest.permission.CAMERA);
                }
                if (requested.contains(PermissionRequest.RESOURCE_AUDIO_CAPTURE)) {
                    neededAndroidPerms.add(Manifest.permission.RECORD_AUDIO);
                }
                if (neededAndroidPerms.isEmpty()) {
                    // Nothing else in a PermissionRequest is backed by a
                    // declared runtime permission here -- deny rather than
                    // silently hang.
                    request.deny();
                    return;
                }

                java.util.List<String> stillMissing = new java.util.ArrayList<>();
                for (String perm : neededAndroidPerms) {
                    if (checkSelfPermission(perm) != PackageManager.PERMISSION_GRANTED) stillMissing.add(perm);
                }
                if (stillMissing.isEmpty()) {
                    request.grant(request.getResources());
                    return;
                }

                // Ask Android for whichever runtime permission(s) are still
                // missing and hold onto the WebView's request until that
                // answer comes back.
                pendingWebPermissionRequest = request;
                requestPermissions(stillMissing.toArray(new String[0]), WEB_MEDIA_PERMISSION_REQUEST_CODE);
            }

            // Handles navigator.geolocation.getCurrentPosition()/
            // watchPosition() calls (e.g. a "find stores near me" feature).
            // WebView routes these through this separate callback rather
            // than onPermissionRequest above, and always auto-denies them
            // without this override -- same failure mode as camera/mic, just
            // a different WebChromeClient method.
            @Override
            public void onGeolocationPermissionsShowPrompt(String origin, GeolocationPermissions.Callback callback) {
                boolean fineGranted = checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
                boolean coarseGranted = checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
                if (fineGranted || coarseGranted) {
                    // retain=true: don't ask again for this origin every
                    // single page load, same as a real browser remembering
                    // the choice per-site.
                    callback.invoke(origin, true, true);
                    return;
                }
                pendingGeoOrigin = origin;
                pendingGeoCallback = callback;
                requestPermissions(
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
            }

            // Handles window.open() calls, which is how Google/Firebase-style
            // "sign in with popup" flows work. A bare WebView has nowhere to put
            // that second window, so without this override the popup silently
            // fails (or opens detached from the parent page) and the auth
            // handler comes back with "The requested action is invalid".
            //
            // We give it a real WebView hosted in a full-screen Dialog, and rely
            // on the provider's own page calling window.close() when the flow
            // finishes (which Firebase's auth handler does) to dismiss it.
            @Override
            public boolean onCreateWindow(WebView view, boolean isDialog, boolean isUserGesture, Message resultMsg) {
                WebView popupWebView = new WebView(MainActivity.this);
                WebSettings popupSettings = popupWebView.getSettings();
                popupSettings.setJavaScriptEnabled(true);
                popupSettings.setDomStorageEnabled(true);
                // Same no-zoom behavior as the main WebView (the popup had
                // none of these set, so sign-in popups were pinch-zoomable).
                popupSettings.setSupportZoom(false);
                popupSettings.setBuiltInZoomControls(false);
                popupSettings.setDisplayZoomControls(false);
                popupSettings.setTextZoom(100);
                // Same user-agent spoof as the main WebView above (see the
                // comment there for the caveats) -- popup-style Google sign-in
                // opens its auth page in exactly this popup WebView, so it
                // needs the same "; wv)"/"Version/X.X " stripping or it hits
                // the same silent block.
                String popupDefaultUA = popupSettings.getUserAgentString();
                popupSettings.setUserAgentString(popupDefaultUA.replace(UrlObfuscator.decode(new int[] { 145, 233, 159, 113 }, 170), "").replaceAll("Version/[0-9.]+\s", ""));
                popupWebView.setVerticalScrollBarEnabled(false);
                popupWebView.setHorizontalScrollBarEnabled(false);

                final Dialog popupDialog = new Dialog(MainActivity.this, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
                // Loading overlay: same bouncing dots + colors as the main
                // navigation overlay, shown over the popup until its page has
                // painted, so there is never an empty black/white screen while
                // a link (sign-in, Telegram, any website) is still loading.
                final FrameLayout popupRoot = new FrameLayout(MainActivity.this);
                popupRoot.setBackgroundColor(Color.parseColor(UrlObfuscator.decode(new int[] { 152, 188, 159, 126, 81, 48, 19 }, 187)));
                popupRoot.addView(popupWebView, new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
                final FrameLayout popupLoader = new FrameLayout(MainActivity.this);
                popupLoader.setBackgroundColor(Color.parseColor(UrlObfuscator.decode(new int[] { 239, 141, 108, 79, 46, 1, 224 }, 204)));
                final BouncingDotsView popupDots = new BouncingDotsView(MainActivity.this, Color.parseColor(UrlObfuscator.decode(new int[] { 254, 205, 90, 11, 24, 73, 214 }, 221)));
                FrameLayout.LayoutParams popupDotsParams = new FrameLayout.LayoutParams(
                    (int) (84 * getResources().getDisplayMetrics().density),
                    (int) (36 * getResources().getDisplayMetrics().density));
                popupDotsParams.gravity = Gravity.CENTER;
                popupLoader.addView(popupDots, popupDotsParams);
                popupRoot.addView(popupLoader, new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
                final Runnable hidePopupLoader = () -> {
                    if (popupLoader.getVisibility() != View.VISIBLE) return;
                    popupLoader.animate().alpha(0f).setDuration(180)
                        .withEndAction(() -> popupLoader.setVisibility(View.GONE)).start();
                };
                // Safety net: never leave the loader up forever if a page
                // stalls; after 12s the page (or its own error) shows instead.
                popupLoader.postDelayed(hidePopupLoader, 12000);
                popupDialog.setContentView(popupRoot);
                popupDialog.setOnDismissListener(d -> popupWebView.destroy());
                popupDialog.show();

                popupWebView.setWebViewClient(new WebViewClient() {
                    @Override
                    public void onPageStarted(WebView v, String u, Bitmap f) {
                        super.onPageStarted(v, u, f);
                        if (u != null && openChatLinkExternally(Uri.parse(u))) {
                            v.stopLoading();
                            popupDialog.dismiss();
                            return;
                        }
                        v.evaluateJavascript(ZOOM_LOCK_JS, null);
                        v.evaluateJavascript(VIDEO_LOADER_JS, null);
                        v.evaluateJavascript(NATIVE_FEEL_JS, null);
                        v.evaluateJavascript(RESOURCE_ERROR_JS, null);
                    }

                    @Override
                    public void onPageFinished(WebView v, String u) {
                        super.onPageFinished(v, u);
                        v.evaluateJavascript(ZOOM_LOCK_JS, null);
                        hidePopupLoader.run();
                    }

                    @Override
                    public void onReceivedError(WebView v, WebResourceRequest request, WebResourceError error) {
                        super.onReceivedError(v, request, error);
                        if (request.isForMainFrame()) {
                            hidePopupLoader.run();
                            // Replace Chromium's built-in "Webpage not available"
                            // screen with the branded one. Base/history URL is the
                            // failed address, so the page's Retry just re-navigates there.
                            String failed = request.getUrl().toString();
                            v.loadDataWithBaseURL(failed,
                                renderErrorPage(String.valueOf(error.getDescription()), failed),
                                UrlObfuscator.decode(new int[] { 154, 104, 84, 63, 69, 225, 220, 170, 138 }, 238), UrlObfuscator.decode(new int[] { 170, 74, 123, 113, 67 }, 255), failed);
                        }
                    }

                    @Override
                    public void onReceivedHttpError(WebView v, WebResourceRequest request, WebResourceResponse errorResponse) {
                        super.onReceivedHttpError(v, request, errorResponse);
                        int status = errorResponse.getStatusCode();
                        if (status < 400 || status == 401 || status == 407) return;
                        String failed = request.getUrl().toString();
                        if (request.isForMainFrame()) {
                            hidePopupLoader.run();
                            v.loadDataWithBaseURL(failed,
                                renderErrorPage(UrlObfuscator.decode(new int[] { 88, 123, 26, 61, 172 }, 272) + status, failed),
                                UrlObfuscator.decode(new int[] { 85, 37, 39, 10, 178, 212, 175, 151, 117 }, 289), UrlObfuscator.decode(new int[] { 98, 2, 51, 185, 139 }, 55), failed);
                        } else {
                            reportFrameFailure(v, failed);
                        }
                    }

                    // window.open() targets (like the "Update now" link) can land
                    // on a page -- e.g. Telegram's t.me web page -- that immediately
                    // tries to hand off to a non-http(s) app scheme (tg://, market://,
                    // mailto:, intent://, etc). A bare WebView can't load those itself
                    // and shows Android's raw "Webpage not available /
                    // ERR_UNKNOWN_URL_SCHEME" error. Intercept here and hand the URL
                    // to the system instead, so it opens Telegram (or falls back to
                    // the Play Store / browser) the way a real browser tab would.
                    @Override
                    public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                        Uri uri = request.getUrl();
                        if (openChatLinkExternally(uri)) {
                            popupDialog.dismiss();
                            return true;
                        }
                        String scheme = uri.getScheme();
                        if (scheme != null && !scheme.equals(UrlObfuscator.decode(new int[] { 32, 19, 242, 213 }, 72)) && !scheme.equals(UrlObfuscator.decode(new int[] { 49, 12, 227, 198, 166 }, 89))) {
                            try {
                                startActivity(new Intent(Intent.ACTION_VIEW, uri));
                            } catch (ActivityNotFoundException e) {
                                // No app installed to handle it (e.g. Telegram not
                                // installed) -- nothing sensible to fall back to for
                                // a non-http(s) scheme, so just drop it.
                            }
                            popupDialog.dismiss();
                            return true;
                        }
                        return false;
                    }
                });
                popupWebView.setWebChromeClient(new WebChromeClient() {
                    @Override
                    public void onProgressChanged(WebView window, int progress) {
                        if (progress >= 85) hidePopupLoader.run();
                    }

                    @Override
                    public void onCloseWindow(WebView window) {
                        popupDialog.dismiss();
                    }
                });

                WebView.WebViewTransport transport = (WebView.WebViewTransport) resultMsg.obj;
                transport.setWebView(popupWebView);
                resultMsg.sendToTarget();
                return true;
            }
        });

        webView.setDownloadListener((url, userAgent, contentDisposition, mimeType, contentLength) ->
            startDownload(url, userAgent, contentDisposition, mimeType));

        // Registered once here rather than per-download -- handleDownloadComplete
        // looks the finished (or failed) download up by the ID Android hands
        // back in the broadcast. Filtered against pendingDownloadId (set in
        // startDownload) so a broadcast for some OTHER completed download --
        // another app's, or a stray one already sitting in the system queue
        // -- is just ignored instead of being mistaken for the download the
        // user actually just tapped.
        downloadCompleteReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                long id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1);
                if (id != -1 && id == pendingDownloadId) {
                    pendingDownloadId = -1;
                    handleDownloadComplete(id);
                }
            }
        };
        IntentFilter downloadFilter = new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE);
        if (Build.VERSION.SDK_INT >= 33) {
            // RECEIVER_EXPORTED, not RECEIVER_NOT_EXPORTED -- this broadcast
            // comes from the system's own download provider (a different
            // process from this app), not from anything this app sends
            // itself. NOT_EXPORTED would only accept broadcasts from within
            // this same app, so on Android 13+ it silently never fired here.
            registerReceiver(downloadCompleteReceiver, downloadFilter, Context.RECEIVER_EXPORTED);
        } else {
            registerReceiver(downloadCompleteReceiver, downloadFilter);
        }

        webView.loadUrl(resolveStartUrl(getIntent()));
    }

    // Where the WebView loads on a normal launch (tapping the icon, no
    // special intent data attached) -- also the fallback baseline that
    // resolveStartUrl()/handleIncomingIntent() build on top of.
    private String baseUrl() {
        return EMBED_HOST + UrlObfuscator.decode(new int[] { 3, 231, 204, 162, 158, 43, 76, 55, 15, 237 }, 106);
    }
    

    // Turns an incoming Intent -- a custom-scheme deep link or a shared
    // text/link from another app's share sheet -- into the URL that should
    // actually be loaded on a cold start. A plain launch (nothing special
    // attached) just returns baseUrl(). Shortcut taps (see
    // res/xml/shortcuts.xml) don't need special handling here since a cold
    // start already lands on a fresh baseUrl() either way -- they only
    // matter in handleIncomingIntent, for when the app's already running.
    private String resolveStartUrl(Intent intent) {
        if (intent == null) return baseUrl();
        String action = intent.getAction();

        // Deep link: <scheme>://open/some/path?x=y#frag -- forwards
        // everything after the scheme onto the bundled page's own URL so
        // the web app's own router, if it has one, can see it.
        if (Intent.ACTION_VIEW.equals(action) && intent.getData() != null) {
            Uri data = intent.getData();
            StringBuilder sb = new StringBuilder(baseUrl());
            String path = data.getPath();
            String query = data.getQuery();
            String fragment = data.getFragment();
            if (query != null && !query.isEmpty()) sb.append('?').append(query);
            if (fragment != null && !fragment.isEmpty()) {
                sb.append('#').append(fragment);
            } else if (path != null && !path.isEmpty() && !"/".equals(path)) {
                sb.append('#').append(path);
            }
            return sb.toString();
        }

        // Shared into this app from another app's share sheet -- the web
        // app can read this back out via location.search if it wants to
        // act on it (e.g. pre-fill a message box with the shared text).
        if (Intent.ACTION_SEND.equals(action) && UrlObfuscator.decode(new int[] { 15, 255, 193, 172, 216, 102, 89, 53, 26, 252 }, 123).equals(intent.getType())) {
            String shared = intent.getStringExtra(Intent.EXTRA_TEXT);
            if (shared != null && !shared.isEmpty()) {
                try {
                    return baseUrl() + UrlObfuscator.decode(new int[] { 179, 216, 162, 136, 122, 66, 34, 58, 240, 198, 186, 149, 61 }, 140) + java.net.URLEncoder.encode(shared, UrlObfuscator.decode(new int[] { 200, 232, 157, 215, 33 }, 157));
                } catch (java.io.UnsupportedEncodingException e) {
                    return baseUrl();
                }
            }
        }

        return baseUrl();
    }

    // Same intent handling as resolveStartUrl, but for when the app is
    // already running -- the singleTask launch mode set in the manifest
    // routes a second launch (a shortcut tap, a deep link, a share) here
    // instead of spawning a duplicate Activity, so this acts directly on
    // the existing WebView rather than returning a URL for onCreate's
    // initial loadUrl() call.
    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        if (webView == null || intent == null) return;

        String shortcutAction = intent.getStringExtra(UrlObfuscator.decode(new int[] { 221, 165, 131, 121, 94, 42, 29, 243, 249, 164, 135, 119, 75, 46, 14 }, 174));
        if (UrlObfuscator.decode(new int[] { 205, 187, 145, 115, 90, 62 }, 191).equals(shortcutAction)) {
            webView.getSettings().setCacheMode(WebSettings.LOAD_NO_CACHE);
            webView.reload();
            webView.getSettings().setCacheMode(WebSettings.LOAD_CACHE_ELSE_NETWORK);
            return;
        }
        if (UrlObfuscator.decode(new int[] { 184, 128, 99, 72 }, 208).equals(shortcutAction)) {
            webView.loadUrl(baseUrl());
            webView.clearHistory();
            return;
        }

        String url = resolveStartUrl(intent);
        if (!url.equals(baseUrl())) {
            webView.loadUrl(url);
        }
    }

    // Backs the AndroidBridge JS interface (see addJavascriptInterface
    // above): a short haptic tap on buttons/links, and recoloring the
    // system bars to match the page's own <meta name="theme-color">
    // instead of leaving them a fixed color that may clash with it.
    private class AndroidBridge {
        // Remembers the file name a page asked for with <a download=...>, so
        // blob:/data: downloads keep their real name (see handleInlineDownload).
        @JavascriptInterface
        public void noteDownloadName(String href, String name) {
            try {
                if (href == null || name == null || name.isEmpty()) return;
                if (downloadNames.size() > 40) downloadNames.clear();
                downloadNames.put(downloadKey(href), name);
            } catch (Exception ignored) {}
        }

        @JavascriptInterface
        public void blobSaveBegin(String name, String mime) {
            try {
                if (blobOut != null) { try { blobOut.close(); } catch (Exception ignored) {} }
                if (blobTmp != null) blobTmp.delete();
                blobTmp = File.createTempFile(UrlObfuscator.decode(new int[] { 131, 108, 112, 92 }, 225), UrlObfuscator.decode(new int[] { 220, 101, 93, 63 }, 242), getCacheDir());
                blobOut = new java.io.FileOutputStream(blobTmp);
                blobName = name;
                blobMime = mime;
            } catch (Exception e) {
                blobSaveFailed();
            }
        }

        @JavascriptInterface
        public void blobSaveChunk(String base64) {
            try {
                if (blobOut == null) return;
                blobOut.write(android.util.Base64.decode(base64, android.util.Base64.DEFAULT));
            } catch (Exception e) {
                blobSaveFailed();
            }
        }

        @JavascriptInterface
        public void blobSaveEnd() {
            try {
                if (blobOut == null || blobTmp == null) return;
                blobOut.close();
                File tmp = blobTmp;
                String name = blobName;
                String mime = blobMime;
                blobOut = null;
                blobTmp = null;
                finishInlineSave(tmp, name, mime);
            } catch (Exception e) {
                blobSaveFailed();
            }
        }

        @JavascriptInterface
        public void blobSaveFailed() {
            try { if (blobOut != null) blobOut.close(); } catch (Exception ignored) {}
            if (blobTmp != null) blobTmp.delete();
            blobOut = null;
            blobTmp = null;
            runOnUiThread(() -> {
                Toast.makeText(MainActivity.this, UrlObfuscator.decode(new int[] { 71, 77, 54, 14, 19, 241, 220, 184, 219, 124, 88, 49, 27, 243, 209, 238, 211, 113, 94, 37, 3, 234, 141, 162, 132, 126, 9, 58, 2, 231, 193, 228, 151, 106, 72, 51, 127, 24, 244, 208, 190 }, 259), Toast.LENGTH_LONG).show();
                notifyDownloadResult(false);
            });
        }

        @JavascriptInterface
        public void vibrate() {
            runOnUiThread(() -> {
                if (vibrator == null || !vibrator.hasVibrator()) return;
                try {
                    if (Build.VERSION.SDK_INT >= 26) {
                        vibrator.vibrate(VibrationEffect.createOneShot(12, VibrationEffect.DEFAULT_AMPLITUDE));
                    } else {
                        vibrator.vibrate(12);
                    }
                } catch (SecurityException e) {
                    // VIBRATE permission not declared/granted for this build -- skip haptic silently
                    // instead of crashing every tap (see comment on vibrate() above).
                }
            });
        }

        // Lets the page know this APK can report real DownloadManager
        // progress (see startDownloadProgressPolling). An APK built before
        // this existed has no such method, so the page keeps its old
        // behavior there instead of waiting for progress that never comes.
        @JavascriptInterface
        public boolean supportsDownloadProgress() {
            return true;
        }

        @JavascriptInterface
        public void reportScrollTop(int top) {
            lastKnownScrollTop = top;
        }



        // Manual override for pages that want to be explicit about it --
        // e.g. call AndroidBridge.setPullToRefreshEnabled(false) right when
        // opening a dialog that covers the whole screen (so even a drag
        // starting at y=0 can't trigger a refresh under it), and re-enable
        // on close. The top-zone gating above handles most dialogs/sheets
        // automatically without needing this, but a truly edge-to-edge
        // modal starts exactly where the real page would too, so nothing
        // purely native can tell those two apart -- only the page itself
        // knows when that's happening.
        @JavascriptInterface
        public void setPullToRefreshEnabled(boolean enabled) {
            runOnUiThread(() -> swipeRefresh.setEnabled(enabled));
        }

        // Backs the navigator.share() polyfill above -- opens the real
        // Android share sheet (the same chooser a native app gets) instead
        // of a page having to fake one out of a copy-link button. Best
        // effort: fire-and-forget on the UI thread, matching vibrate()
        // above, since the JS side already treats the call as fire-and-
        // forget (it resolves its Promise immediately rather than waiting
        // to hear whether the user actually picked a target app).
        @JavascriptInterface
        public void share(String title, String text, String url) {
            runOnUiThread(() -> {
                try {
                    String body = text == null ? "" : text;
                    if (url != null && !url.isEmpty()) {
                        body = body.isEmpty() ? url : body + "\n" + url;
                    }
                    Intent sendIntent = new Intent(Intent.ACTION_SEND);
                    sendIntent.setType(UrlObfuscator.decode(new int[] { 96, 86, 42, 5, 191, 223, 162, 140, 101, 69 }, 276));
                    if (title != null && !title.isEmpty()) sendIntent.putExtra(Intent.EXTRA_SUBJECT, title);
                    sendIntent.putExtra(Intent.EXTRA_TEXT, body);
                    startActivity(Intent.createChooser(sendIntent, null));
                } catch (Exception ignored) {
                    // No app installed that can handle a share -- nothing
                    // sensible to fall back to, so just drop it.
                }
            });
        }

        // Lets the page's own JS trigger a real system notification while
        // the app is open or backgrounded (e.g. on a socket.io "new message"
        // event), with no Firebase/google-services.json needed -- unlike
        // FCM push, this can't wake the app up once it's fully killed, but
        // it needs zero external setup. Call from the web app like:
        //   if (window.AndroidBridge) AndroidBridge.showNotification(title, body);
        @JavascriptInterface
        public void showNotification(String title, String body) {
            runOnUiThread(() -> {
                NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                if (manager == null) return;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                    && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                    return; // user hasn't granted the permission -- nothing we can show
                }
                String safeTitle = (title == null || title.trim().isEmpty()) ? UrlObfuscator.decode(new int[] { 112, 3, 51, 202, 238, 142, 154, 222, 80, 115, 31 }, 293) : title;
                String safeBody = body == null ? "" : body;
                NotificationCompat.Builder notification = new NotificationCompat.Builder(MainActivity.this, UrlObfuscator.decode(new int[] { 95, 63, 31, 249, 194, 186, 129 }, 59))
                    .setSmallIcon(android.R.drawable.ic_dialog_info)
                    .setContentTitle(safeTitle)
                    .setContentText(safeBody)
                    .setStyle(new NotificationCompat.BigTextStyle().bigText(safeBody))
                    .setAutoCancel(true)
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT);
                manager.notify((int) System.currentTimeMillis(), notification.build());
            });
        }

        // Hands a URL straight to the system (real browser / Telegram app /
        // whatever's registered for it) instead of letting the page's own
        // window.open() land it in the onCreateWindow popup WebView. Needed
        // for links like the lockout screen's t.me channel: Telegram's edge
        // resets/refuses connections from an embedded WebView (same class of
        // problem as accounts.google.com rejecting WebView sign-in above),
        // which surfaces as a raw "Webpage not available / ERR_CONNECTION_RESET"
        // inside the popup even though the same URL opens fine from a real
        // browser or the Telegram app itself. Call from the page like:
        //   if (window.AndroidBridge && AndroidBridge.openExternal) {
        //     AndroidBridge.openExternal(url);
        //   } else {
        //     window.open(url, '_blank'); // fallback outside the app (e.g. a browser tab)
        //   }
        @JavascriptInterface
        public void openExternal(String url) {
            if (url == null || url.isEmpty()) return;
            runOnUiThread(() -> {
                try {
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
                } catch (Exception ignored) {
                    // No app installed that can handle it -- nothing sensible
                    // to fall back to from here, so just drop it.
                }
            });
        }

        @JavascriptInterface
        public void applyThemeColor(String hex) {
            if (hex == null || !hex.matches(UrlObfuscator.decode(new int[] { 18, 72, 162, 242, 137, 202, 64, 68, 105, 5, 178, 140, 249, 130, 133, 43, 65, 39, 33, 216, 149, 145, 151, 56, 82, 99, 95, 168, 237, 180, 221, 112, 5, 111 }, 76))) return;
            final String full = hex.length() == 4
                ? "#" + hex.charAt(1) + hex.charAt(1) + hex.charAt(2) + hex.charAt(2) + hex.charAt(3) + hex.charAt(3)
                : hex;
            runOnUiThread(() -> {
                try {
                    int color = Color.parseColor(full);
                    themeColorOverride = color;
                    paintSystemBarScrims(color);
                } catch (Exception ignored) {
                }
            });
        }

        // Called from the page-finished script (and again on every focusin, i.e.
        // just before the keyboard opens) with the page's actual background
        // color. Keeps the window and WebView backgrounds identical to it, so
        // the area exposed while the keyboard animates in or out is the same
        // color as the page instead of a white or black flash.
        @JavascriptInterface
        public void applyPageBackground(String hex) {
            if (hex == null || !hex.matches(UrlObfuscator.decode(new int[] { 3, 95, 192, 251, 244, 190, 118, 27, 51, 68, 190, 139, 140, 139, 57, 83, 105 }, 93))) return;
            runOnUiThread(() -> {
                try {
                    int color = Color.parseColor(hex);
                    getWindow().setBackgroundDrawable(new ColorDrawable(color));
                    if (rootLayout != null) rootLayout.setBackgroundColor(color);
                    if (webView != null) webView.setBackgroundColor(color);
                    if (themeColorOverride == null) paintSystemBarScrims(color);
                } catch (Exception ignored) {
                }
            });
        }
    }


    // Native keyboard correction is intentionally limited to the Options
    // panel. Chrome already handles ordinary fields well; forcing every
    // focused field to the center in a WebView can create a large jump and
    // fight the page's own scroll containers. Only make a small correction
    // when an Options field is actually outside the visible viewport.
    private void scrollFocusedFieldIntoView() {
        if (webView == null) return;
        webView.evaluateJavascript(
            UrlObfuscator.decode(new int[] { 70, 235, 217, 165, 137, 125, 65, 40, 8, 173, 141, 184, 150, 115, 89, 68 }, 110) +
            UrlObfuscator.decode(new int[] { 9, 255, 207, 252, 139, 39, 93, 55, 20, 227, 216, 177, 157, 102, 31, 55, 10, 250, 232, 160, 142, 103, 76, 38, 19, 196, 220, 141, 135, 42, 6, 52, 62, 28, 205, 221, 181, 159, 117, 119, 39, 2, 252, 219, 189, 129, 54, 25, 116 }, 127) +
            UrlObfuscator.decode(new int[] { 249, 201, 230, 204, 124, 87, 54, 25, 166, 207, 175, 129, 96, 70, 44, 29, 252, 190, 206, 243, 159, 119, 91, 42, 11, 219, 223, 166, 128, 61, 81, 62, 30, 251, 207, 164, 130, 120, 2, 110, 1, 244, 139, 164, 135, 119, 75, 55, 5, 88, 183, 148, 174, 158, 110, 76, 42, 25, 173 }, 144) +
            UrlObfuscator.decode(new int[] { 215, 161, 173, 222, 124, 1, 63, 21, 250, 205, 186, 147, 123, 64, 125, 19, 242, 196, 166, 152, 104, 105, 39, 15, 228, 205, 169, 146, 62, 77, 37, 74, 160, 193, 195, 162, 156, 33, 6, 103, 29, 247, 212, 163, 152, 113, 93, 38, 95, 242, 192, 170, 148, 112, 87, 107, 25, 166, 196, 169, 139, 112, 66, 43, 15, 243, 183, 223, 244, 213, 105, 95, 45, 13, 229, 216, 238 }, 161) +
            UrlObfuscator.decode(new int[] { 196, 176, 130, 47, 90, 112, 68, 234, 132, 189, 137, 96, 104, 36, 9, 230, 222, 189, 199, 216, 55, 19, 40, 20, 214, 214, 175, 146, 100, 118, 53, 0, 247, 153, 249, 212 }, 178) +
            UrlObfuscator.decode(new int[] { 170, 132, 41, 84, 30, 99, 64, 187, 210, 180, 137, 109, 67, 113, 83, 178, 199, 243, 204, 45, 8, 58, 8, 244, 223, 171, 155, 109, 70, 97, 67, 162, 215, 227, 220, 61, 56, 77, 56, 16, 254, 217, 173, 223, 49, 16, 116, 21, 189, 219, 162, 179, 96, 64, 57, 9, 229, 222, 140, 140, 110, 82, 36, 6, 239, 199, 232, 146, 154, 106, 72, 46, 21, 161 }, 195) +
            UrlObfuscator.decode(new int[] { 162, 146, 96, 17, 34, 82, 239, 131, 171, 142, 126, 107, 39, 18, 232, 193, 173, 141, 101, 98, 44, 54, 27, 243, 200, 137, 159, 122, 76, 127, 95, 174 }, 212) +
            UrlObfuscator.decode(new int[] { 147, 101, 81, 98, 23, 246, 162, 201, 180, 146, 127, 85, 46, 86, 225, 223, 166, 129, 114, 94, 7, 25, 234, 217, 189, 131, 121, 94, 114, 30, 230, 212, 229, 146, 107, 31, 55, 22, 64, 232, 203, 242, 147, 127, 80, 63, 31, 226, 143, 163, 154, 124, 85, 63, 24, 160, 196, 162, 133, 111, 91, 0, 2, 239, 194, 172, 151, 57 }, 229) +
            UrlObfuscator.decode(new int[] { 128, 116, 70, 115, 2, 240, 212, 242, 223, 59, 23, 61, 11, 251, 136, 163, 131, 105, 80, 34, 95, 177, 155 }, 246) +
            UrlObfuscator.decode(new int[] { 110, 64, 109, 22, 173, 192, 174, 148, 139, 113, 80, 98, 13, 242, 148, 168, 150, 114, 28, 48, 22, 254, 197, 177, 210, 67, 76, 56, 3, 164, 196, 161, 137, 46, 87, 106, 1, 237, 213, 180, 176, 147, 48, 20, 45, 18, 180, 200, 182, 146, 60, 24, 30, 19, 229, 216, 225, 131, 108, 84, 99, 88, 189, 132, 177, 142, 47, 20, 109, 83, 185, 137, 150, 229 }, 263) +
            UrlObfuscator.decode(new int[] { 125, 91, 37, 16, 180, 218, 180, 217, 98, 1, 58, 2, 252, 151, 186, 136, 108, 14, 34, 0, 232, 215, 163, 220, 45, 82, 95, 41, 20, 181, 215, 176, 150, 63, 70, 52, 16, 190, 192, 255, 132, 96, 94, 97, 33, 234, 222, 161, 198, 106, 71, 61, 76, 178, 148, 237, 150, 151, 52, 13, 114, 74, 170, 144, 241, 204 }, 280) +
            UrlObfuscator.decode(new int[] { 64, 46, 79, 226, 192, 168, 151, 99, 8, 59, 41, 31, 239, 156, 168, 153, 36, 89, 121, 21, 249, 219, 160, 151, 98, 68, 103, 73, 163, 207, 164, 132, 125, 77, 41, 18, 169, 138, 183, 131, 99, 112, 94, 48, 24, 240, 151, 244, 150, 104, 67, 63, 26, 250, 192, 255, 146, 127, 65, 58, 8, 226, 223, 230, 178, 108, 70, 50, 4, 169, 208, 161, 147, 111, 115, 82, 112, 31, 244, 212, 173, 153, 126, 88, 48, 6, 206, 149, 248, 203 }, 297) +
            UrlObfuscator.decode(new int[] { 86, 56, 85, 239, 216, 252, 223, 107, 84, 120, 6, 247, 193, 189, 157, 124, 103, 43, 4, 235, 195, 190, 215, 123, 68, 104, 6, 232, 202, 167, 143, 116, 87, 91, 52, 27, 243, 206, 240, 131, 100, 85, 123, 7, 240, 192, 190, 156, 99, 108, 52, 68, 240, 222, 166, 152, 61, 66, 32, 8, 247, 195, 237, 130, 154, 118, 92, 42, 18, 245, 203, 226, 208, 119, 64, 32, 28, 181, 204, 249, 212, 115 }, 63) +
            UrlObfuscator.decode(new int[] { 53, 3, 253, 200, 183, 156, 99, 71, 44, 8, 241, 139, 183, 128, 112, 78, 44, 51, 60, 228, 148, 160, 142, 118, 72, 109, 18, 240, 216, 167, 147, 61, 82, 42, 6, 236, 218, 162, 133, 123, 18, 96, 7, 240, 208, 172, 197, 124, 9, 4, 35, 0 }, 80) +
            UrlObfuscator.decode(new int[] { 28, 227, 254, 202, 190, 148, 51, 95, 112, 3, 234, 203, 252, 220, 58, 9 }, 97), null);
    }

    // Lifts the WebView just enough that the focused text field sits above
    // the keyboard (like a browser scrolling the field into view), and drops
    // it back when the keyboard closes. Does nothing if the field is already
    // visible, so a sheet that fits above the keyboard is not moved at all.
    private void panFocusedFieldAboveKeyboard(final int imeBottom, final int bottomBars) {
        if (webView == null || swipeRefresh == null) return;
        if (imeBottom <= 0) {
            swipeRefresh.animate().translationY(0f).setDuration(120).start();
            return;
        }
        final int kb = Math.max(0, imeBottom - bottomBars);
        webView.evaluateJavascript(
            UrlObfuscator.decode(new int[] { 90, 247, 197, 161, 141, 121, 69, 36, 4, 161, 129, 188, 146, 119, 93, 56, 20, 224, 210, 159, 191, 192, 120, 84, 57, 12, 245, 210, 184, 129, 58, 82, 49, 5, 249, 217, 171, 168, 96, 78, 39, 12, 230, 211, 253, 140, 98, 11, 99, 0, 252, 227, 223, 224, 193, 38, 94, 54, 27, 226, 219, 176, 154, 103, 28, 51, 31, 235, 215, 228, 158, 110, 94, 60, 26, 233, 134, 232, 213, 56 }, 114) +
            UrlObfuscator.decode(new int[] { 245, 195, 179, 192, 139, 35, 21, 61, 85, 238, 216, 191, 185, 119, 88, 49, 15, 238, 150, 247, 198, 32, 89, 35, 39, 229, 222, 173, 149, 69, 68, 55, 6, 170, 136, 251 }, 131) +
            UrlObfuscator.decode(new int[] { 253, 213, 250, 133, 49, 18, 115, 74, 229, 197, 186, 156, 124, 0, 96, 67, 240, 130, 255, 220, 39, 107, 91, 37, 8, 250, 200, 188, 153, 48, 16, 115, 0, 178, 143, 236, 215, 124, 75, 33, 9, 232, 222, 238, 206, 33, 7, 36, 74, 234, 209, 130, 143, 145, 106, 88, 50, 15, 223, 221, 177, 131, 119, 87, 56, 22, 187, 195, 181, 155, 123, 95, 34, 75, 167, 152, 243 }, 148) +
            UrlObfuscator.decode(new int[] { 211, 165, 145, 34, 83, 125, 62, 80, 250, 217, 175, 184, 118, 77, 57, 18, 252, 218, 180, 177, 125, 89, 42, 0, 249, 254, 174, 137, 125, 0, 110, 93, 247, 193, 183, 151, 115, 78, 31, 19, 28, 232, 211, 244, 139, 119, 66, 56, 17, 188, 193, 252, 147, 127, 91, 58, 2, 225, 129, 251, 217, 56, 14, 105, 84, 180, 147, 249, 156, 99, 126, 74, 62, 20, 179, 223, 240, 131, 101, 83, 33, 1, 225, 220, 241, 221, 62, 21, 48, 17, 162, 130, 224, 211 }, 165),
            value -> {
                try {
                    double bottomCss = Double.parseDouble(value == null ? "-1" : value.trim());
                    if (bottomCss < 0) return;
                    float density = getResources().getDisplayMetrics().density;
                    int visibleBottomPx = swipeRefresh.getHeight() - kb;
                    int padPx = (int) (24 * density);
                    int overlapPx = (int) (bottomCss * density) + padPx - visibleBottomPx;
                    float ty = overlapPx > 0 ? -Math.min(overlapPx, kb) : 0f;
                    swipeRefresh.animate().translationY(ty).setDuration(120).start();
                } catch (Exception ignored) {}
            });
    }

    // Insets the WebView by the system bars / display cutout / keyboard so
    // page content always sits inside the safe area, and sizes the
    // status-bar and navigation-bar strips. Bottom is whichever is taller of
    // the navigation bar and the keyboard.
    private void applySafeArea(int left, int top, int right, int bottomBars, int imeBottom) {
        if (swipeRefresh == null) return;
        // The keyboard no longer shrinks the WebView. Resizing it made the
        // page's layout viewport (100vh/100dvh) collapse to the strip above
        // the keyboard, so bottom sheets/dialogs stretched to fill the whole
        // screen -- much taller than in a mobile browser, where the keyboard
        // simply overlays the page. Instead the keyboard overlays the page
        // and panFocusedFieldAboveKeyboard() lifts the view only as far as
        // needed to keep the focused field visible.
        int bottom = bottomBars;
        FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) swipeRefresh.getLayoutParams();
        if (lp.leftMargin != left || lp.topMargin != top || lp.rightMargin != right || lp.bottomMargin != bottom) {
            lp.setMargins(left, top, right, bottom);
            swipeRefresh.setLayoutParams(lp);
        }
        if (topScrim != null) {
            ViewGroup.LayoutParams t = topScrim.getLayoutParams();
            if (t.height != top) { t.height = top; topScrim.setLayoutParams(t); }
        }
        if (bottomScrim != null) {
            ViewGroup.LayoutParams b = bottomScrim.getLayoutParams();
            if (b.height != bottomBars) { b.height = bottomBars; bottomScrim.setLayoutParams(b); }
        }
    }

    // Last WebView history index a page finished loading at; lets the
    // slide transition know whether the user went forward or back.
    private int lastNavIndex = -1;
    private android.app.Dialog exitDialog = null;

    // Dialog title is the app's own name, never the site's address.
    private String dialogTitleFor(String url) {
        try {
            CharSequence label = getApplicationInfo().loadLabel(getPackageManager());
            if (label != null && label.length() > 0) return label.toString();
        } catch (Exception ignored) {}
        return null;
    }

    private int dpPx(float v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }

    private static class ExitPalette {
        int cardTop, cardBottom, border, title, sub;
        int accentTop, accentBottom, accentText, exitStroke, exitText;
        float cardRadiusDp = 22f, btnRadiusDp = 22f;
    }

    private static int mixColor(int a, int b, float t) {
        float it = 1f - t;
        return Color.rgb(
            Math.round(Color.red(a) * it + Color.red(b) * t),
            Math.round(Color.green(a) * it + Color.green(b) * t),
            Math.round(Color.blue(a) * it + Color.blue(b) * t));
    }

    private static double lumOf(int c) {
        return (0.299 * Color.red(c) + 0.587 * Color.green(c) + 0.114 * Color.blue(c)) / 255.0;
    }

    // Accepts hex (#RRGGBB) and the rgb()/rgba() strings getComputedStyle
    // returns. Anything else (named, color(), oklch...) gives null.
    private static Integer parseCssColor(String v) {
        if (v == null) return null;
        String t = v.trim();
        if (t.isEmpty() || UrlObfuscator.decode(new int[] { 216, 160, 152, 127 }, 182).equals(t)) return null;
        try {
            if (t.startsWith("#")) return Color.parseColor(t) | 0xFF000000;
            if (!t.startsWith(UrlObfuscator.decode(new int[] { 181, 129, 103 }, 199))) return null;
            String inner = t.substring(t.indexOf('(') + 1);
            String[] parts = inner.split(UrlObfuscator.decode(new int[] { 131, 169, 38, 24, 109, 93, 207, 154 }, 216));
            java.util.ArrayList<Double> nums = new java.util.ArrayList<>();
            for (String part : parts) {
                if (!part.isEmpty() && !".".equals(part)) nums.add(Double.parseDouble(part));
            }
            if (nums.size() < 3) return null;
            return Color.rgb(
                (int) Math.max(0, Math.min(255, Math.round(nums.get(0)))),
                (int) Math.max(0, Math.min(255, Math.round(nums.get(1)))),
                (int) Math.max(0, Math.min(255, Math.round(nums.get(2)))));
        } catch (Exception e) {
            return null;
        }
    }

    private ExitPalette deriveExitPalette(org.json.JSONObject o) {
        int scrim = Color.parseColor(UrlObfuscator.decode(new int[] { 202, 57, 19, 116, 81, 177, 224 }, 233));
        try {
            if (topScrim != null && topScrim.getBackground() instanceof ColorDrawable) {
                scrim = ((ColorDrawable) topScrim.getBackground()).getColor() | 0xFF000000;
            }
        } catch (Exception ignored) {}
        Integer bgC = o == null ? null : parseCssColor(o.optString("bg", null));
        Integer thC = o == null ? null : parseCssColor(o.optString(UrlObfuscator.decode(new int[] { 142, 113, 93, 58, 19 }, 250), null));
        Integer btC = o == null ? null : parseCssColor(o.optString(UrlObfuscator.decode(new int[] { 105, 94, 39 }, 267), null));
        double btnR = o == null ? -1 : o.optDouble(UrlObfuscator.decode(new int[] { 126, 79, 52, 43 }, 284), -1);

        int base = bgC != null ? bgC : (thC != null ? thC : scrim);
        boolean dark = lumOf(base) < 0.5;

        int accent;
        if (btC != null && Math.abs(lumOf(btC) - lumOf(base)) >= 0.15) {
            accent = btC;
        } else if (thC != null && Math.abs(lumOf(thC) - lumOf(base)) >= 0.15) {
            accent = thC;
        } else {
            float[] hsv = new float[3];
            Color.colorToHSV(base, hsv);
            if (hsv[1] < 0.12f) {
                accent = dark ? Color.parseColor(UrlObfuscator.decode(new int[] { 17, 23, 66, 201, 156, 139, 219 }, 50)) : Color.parseColor(UrlObfuscator.decode(new int[] { 96, 83, 182, 145, 136, 239, 190 }, 67));
            } else {
                hsv[1] = Math.max(hsv[1], 0.55f);
                hsv[2] = dark ? 0.95f : 0.6f;
                accent = Color.HSVToColor(hsv);
            }
        }

        ExitPalette p = new ExitPalette();
        p.cardTop = dark ? mixColor(base, Color.WHITE, 0.10f) : base;
        p.cardBottom = dark ? mixColor(base, Color.BLACK, 0.12f) : mixColor(base, Color.BLACK, 0.05f);
        boolean cardDark = lumOf(p.cardTop) < 0.5;
        p.title = cardDark ? Color.WHITE : Color.parseColor(UrlObfuscator.decode(new int[] { 119, 66, 166, 128, 228, 222, 79 }, 84));
        p.sub = mixColor(p.title, p.cardTop, 0.35f);
        p.border = mixColor(accent, p.cardTop, 0.45f);
        p.accentTop = mixColor(accent, Color.WHITE, 0.12f);
        p.accentBottom = mixColor(accent, Color.BLACK, 0.12f);
        p.accentText = lumOf(accent) < 0.55 ? Color.WHITE : Color.parseColor(UrlObfuscator.decode(new int[] { 70, 181, 151, 243, 213, 49, 94 }, 101));
        p.exitStroke = mixColor(p.title, p.cardTop, 0.55f);
        p.exitText = p.sub;
        if (btnR >= 0 && btnR < 18) {
            float r = (float) Math.max(4, Math.min(14, btnR));
            p.btnRadiusDp = r;
            p.cardRadiusDp = r + 8f;
        }
        return p;
    }

    private boolean exitDialogPending = false;

    // Samples the page, then shows the exit dialog dressed to match it.
    // Falls back to the app's bar color if the page can't be read within a
    // moment, so the prompt always appears.
    private void showThemedExitDialog(final Runnable onExit) {
        exitDialogPending = true;
        final boolean[] done = { false };
        final Runnable fallback = () -> {
            if (done[0]) return;
            done[0] = true;
            presentExitDialog(null, onExit);
        };
        try {
            webView.evaluateJavascript(EXIT_THEME_JS, value -> {
                if (done[0]) return;
                done[0] = true;
                org.json.JSONObject o = null;
                try {
                    Object parsed = new org.json.JSONTokener(value).nextValue();
                    if (parsed instanceof String) o = new org.json.JSONObject((String) parsed);
                } catch (Exception ignored) {}
                presentExitDialog(o, onExit);
            });
            webView.postDelayed(fallback, 350);
        } catch (Exception e) {
            fallback.run();
        }
    }

    private void presentExitDialog(org.json.JSONObject o, Runnable onExit) {
        exitDialogPending = false;
        if (!canShowDialog()) {
            onExit.run();
            return;
        }
        try {
            exitDialog = buildExitDialog(onExit, deriveExitPalette(o));
            exitDialog.show();
        } catch (Exception e) {
            onExit.run();
        }
    }

    // Exit confirmation: a card with a sad cat peeking over the top and two
    // pill/rounded buttons, colored from the palette read off the live page.
    // STAY, tapping outside or back keep the app open; only EXIT leaves.
    private android.app.Dialog buildExitDialog(final Runnable onExit, ExitPalette pal) {
        final android.app.Dialog dlg = new android.app.Dialog(this);
        dlg.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE);

        int screenW = getResources().getDisplayMetrics().widthPixels;
        int cardW = Math.min(screenW - dpPx(48), dpPx(320));

        LinearLayout column = new LinearLayout(this);
        column.setOrientation(LinearLayout.VERTICAL);
        column.setGravity(Gravity.CENTER_HORIZONTAL);

        SadMascotView mascot = new SadMascotView(this);
        column.addView(mascot, new LinearLayout.LayoutParams(dpPx(112), dpPx(72)));

        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setGravity(Gravity.CENTER_HORIZONTAL);
        card.setPadding(dpPx(22), dpPx(22), dpPx(22), dpPx(22));
        GradientDrawable bg = new GradientDrawable(
            GradientDrawable.Orientation.TOP_BOTTOM, new int[] { pal.cardTop, pal.cardBottom });
        bg.setCornerRadius(dpPx(pal.cardRadiusDp));
        bg.setStroke(dpPx(1), pal.border);
        card.setBackground(bg);

        TextView title = new TextView(this);
        title.setText(UrlObfuscator.decode(new int[] { 58, 240, 213, 165, 155, 127, 87, 111, 15, 225, 222, 174, 139, 109, 81, 120 }, 118));
        title.setTextColor(pal.title);
        title.setTextSize(18f);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setGravity(Gravity.CENTER);
        card.addView(title, new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        TextView msg = new TextView(this);
        msg.setText(UrlObfuscator.decode(new int[] { 198, 212, 160, 196, 122, 77, 52, 64, 12, 235, 207, 185, 219, 99, 86, 45, 87, 225, 212, 186, 135, 50, 69, 63, 79, 235, 213, 165, 159, 53 }, 135));
        msg.setTextColor(pal.sub);
        msg.setTextSize(14f);
        msg.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams msgLp = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        msgLp.topMargin = dpPx(6);
        card.addView(msg, msgLp);

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams rowLp = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        rowLp.topMargin = dpPx(22);
        card.addView(row, rowLp);

        TextView exit = new TextView(this);
        exit.setText(UrlObfuscator.decode(new int[] { 221, 239, 159, 161 }, 152));
        exit.setTextColor(pal.exitText);
        exit.setTextSize(13f);
        exit.setTypeface(Typeface.DEFAULT_BOLD);
        exit.setLetterSpacing(0.12f);
        exit.setGravity(Gravity.CENTER);
        GradientDrawable exitBg = new GradientDrawable();
        exitBg.setColor(Color.TRANSPARENT);
        exitBg.setCornerRadius(dpPx(pal.btnRadiusDp));
        exitBg.setStroke(dpPx(1), pal.exitStroke);
        exit.setBackground(exitBg);

        TextView stay = new TextView(this);
        stay.setText(UrlObfuscator.decode(new int[] { 250, 156, 166, 95 }, 169));
        stay.setTextColor(pal.accentText);
        stay.setTextSize(13f);
        stay.setTypeface(Typeface.DEFAULT_BOLD);
        stay.setLetterSpacing(0.12f);
        stay.setGravity(Gravity.CENTER);
        GradientDrawable stayBg = new GradientDrawable(
            GradientDrawable.Orientation.TOP_BOTTOM, new int[] { pal.accentTop, pal.accentBottom });
        stayBg.setCornerRadius(dpPx(pal.btnRadiusDp));
        stay.setBackground(stayBg);

        LinearLayout.LayoutParams eLp = new LinearLayout.LayoutParams(0, dpPx(44), 1f);
        eLp.rightMargin = dpPx(6);
        LinearLayout.LayoutParams sLp = new LinearLayout.LayoutParams(0, dpPx(44), 1f);
        sLp.leftMargin = dpPx(6);
        row.addView(exit, eLp);
        row.addView(stay, sLp);

        exit.setOnClickListener(v -> {
            dlg.dismiss();
            onExit.run();
        });
        stay.setOnClickListener(v -> dlg.dismiss());

        column.addView(card, new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        dlg.setContentView(column, new ViewGroup.LayoutParams(cardW, ViewGroup.LayoutParams.WRAP_CONTENT));
        dlg.setCancelable(true);
        dlg.setCanceledOnTouchOutside(true);
        android.view.Window w = dlg.getWindow();
        if (w != null) {
            w.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            w.setLayout(cardW, ViewGroup.LayoutParams.WRAP_CONTENT);
            w.setDimAmount(0.6f);
        }
        return dlg;
    }

    private boolean canShowDialog() {
        return !isFinishing() && !isDestroyed();
    }

    // Last page theme read off the live page (same sampler the exit dialog
    // uses). JS alert()/confirm()/prompt() block the page's JS thread while
    // they're open, so evaluateJavascript can't run at that moment -- the
    // theme has to be sampled ahead of time and cached. Refreshed after each
    // page load and after each dialog closes, so a site that switches
    // light/dark still gets a matching dialog on the next one.
    private org.json.JSONObject cachedDialogTheme = null;

    private void refreshDialogTheme() {
        try {
            webView.postDelayed(() -> {
                try {
                    webView.evaluateJavascript(EXIT_THEME_JS, value -> {
                        try {
                            Object parsed = new org.json.JSONTokener(value).nextValue();
                            if (parsed instanceof String) cachedDialogTheme = new org.json.JSONObject((String) parsed);
                        } catch (Exception ignored) {}
                    });
                } catch (Exception ignored) {}
            }, 400);
        } catch (Exception ignored) {}
    }

    // JS alert()/confirm()/prompt(). Without these WebView shows nothing and
    // confirm() silently answers "cancel". Every path resolves the result
    // (confirm, cancel, or on dismiss) so the page's JS can never hang.
    private boolean showJsAlert(String url, String message, final android.webkit.JsResult result) {
        return showThemedJsDialog(0, message, null, result);
    }

    private boolean showJsConfirm(String url, String message, final android.webkit.JsResult result) {
        return showThemedJsDialog(1, message, null, result);
    }

    private boolean showJsPrompt(String url, String message, String defaultValue, final android.webkit.JsPromptResult result) {
        return showThemedJsDialog(2, message, defaultValue, result);
    }

    // kind: 0 = alert (one button), 1 = confirm, 2 = prompt (text field).
    // Same card, colors and pill buttons as the exit dialog, taken from the
    // page. The JsResult is resolved exactly once: OK, Cancel, back button,
    // tap outside, or any failure while building the dialog.
    private boolean showThemedJsDialog(final int kind, final String message, final String defaultValue,
            final android.webkit.JsResult result) {
        if (!canShowDialog()) { result.cancel(); return true; }
        try {
            final ExitPalette pal = deriveExitPalette(cachedDialogTheme);
            final boolean[] done = { false };
            final android.widget.EditText[] inputRef = { null };

            final android.app.Dialog dlg = new android.app.Dialog(this);
            dlg.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE);

            android.util.DisplayMetrics dm = getResources().getDisplayMetrics();
            int cardW = Math.min(dm.widthPixels - dpPx(48), dpPx(340));

            LinearLayout card = new LinearLayout(this);
            card.setOrientation(LinearLayout.VERTICAL);
            card.setPadding(dpPx(22), dpPx(22), dpPx(22), dpPx(20));
            GradientDrawable bg = new GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM, new int[] { pal.cardTop, pal.cardBottom });
            bg.setCornerRadius(dpPx(pal.cardRadiusDp));
            bg.setStroke(dpPx(1), pal.border);
            card.setBackground(bg);

            String t = dialogTitleFor(null);
            if (t != null) {
                TextView title = new TextView(this);
                title.setText(t);
                title.setTextColor(pal.title);
                title.setTextSize(18f);
                title.setTypeface(Typeface.DEFAULT_BOLD);
                title.setSingleLine(true);
                title.setEllipsize(android.text.TextUtils.TruncateAt.END);
                card.addView(title, new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
            }

            TextView msg = new TextView(this);
            msg.setText(message == null ? "" : message);
            msg.setTextColor(pal.sub);
            msg.setTextSize(15f);
            msg.setLineSpacing(0f, 1.15f);
            android.widget.ScrollView scroll = new android.widget.ScrollView(this);
            scroll.addView(msg);
            LinearLayout.LayoutParams scrollLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            scrollLp.topMargin = dpPx(t != null ? 8 : 0);
            card.addView(scroll, scrollLp);

            if (kind == 2) {
                final android.widget.EditText input = new android.widget.EditText(this);
                inputRef[0] = input;
                input.setSingleLine(true);
                input.setText(defaultValue == null ? "" : defaultValue);
                input.setSelectAllOnFocus(true);
                input.setTextColor(pal.title);
                input.setHintTextColor(pal.sub);
                input.setTextSize(15f);
                input.setHighlightColor(mixColor(pal.accentTop, pal.cardTop, 0.55f));
                input.setPadding(dpPx(14), 0, dpPx(14), 0);
                GradientDrawable inBg = new GradientDrawable();
                inBg.setColor(mixColor(pal.cardTop, pal.title, 0.07f));
                inBg.setCornerRadius(dpPx(pal.btnRadiusDp));
                inBg.setStroke(dpPx(1), pal.border);
                input.setBackground(inBg);
                LinearLayout.LayoutParams inLp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, dpPx(46));
                inLp.topMargin = dpPx(14);
                card.addView(input, inLp);
            }

            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER);
            LinearLayout.LayoutParams rowLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            rowLp.topMargin = dpPx(20);
            card.addView(row, rowLp);

            TextView ok = new TextView(this);
            ok.setText("OK");
            ok.setTextColor(pal.accentText);
            ok.setTextSize(13f);
            ok.setTypeface(Typeface.DEFAULT_BOLD);
            ok.setLetterSpacing(0.12f);
            ok.setGravity(Gravity.CENTER);
            GradientDrawable okBg = new GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM, new int[] { pal.accentTop, pal.accentBottom });
            okBg.setCornerRadius(dpPx(pal.btnRadiusDp));
            ok.setBackground(okBg);

            ok.setOnClickListener(v -> {
                if (done[0]) return;
                done[0] = true;
                try {
                    if (kind == 2 && result instanceof android.webkit.JsPromptResult) {
                        ((android.webkit.JsPromptResult) result).confirm(inputRef[0].getText().toString());
                    } else {
                        result.confirm();
                    }
                } catch (Exception ignored) {}
                dlg.dismiss();
            });

            if (kind != 0) {
                TextView cancel = new TextView(this);
                cancel.setText(UrlObfuscator.decode(new int[] { 249, 152, 182, 84, 115, 25 }, 186));
                cancel.setTextColor(pal.exitText);
                cancel.setTextSize(13f);
                cancel.setTypeface(Typeface.DEFAULT_BOLD);
                cancel.setLetterSpacing(0.12f);
                cancel.setGravity(Gravity.CENTER);
                GradientDrawable cBg = new GradientDrawable();
                cBg.setColor(Color.TRANSPARENT);
                cBg.setCornerRadius(dpPx(pal.btnRadiusDp));
                cBg.setStroke(dpPx(1), pal.exitStroke);
                cancel.setBackground(cBg);
                cancel.setOnClickListener(v -> dlg.cancel());
                LinearLayout.LayoutParams cLp = new LinearLayout.LayoutParams(0, dpPx(44), 1f);
                cLp.rightMargin = dpPx(6);
                row.addView(cancel, cLp);
                LinearLayout.LayoutParams oLp = new LinearLayout.LayoutParams(0, dpPx(44), 1f);
                oLp.leftMargin = dpPx(6);
                row.addView(ok, oLp);
            } else {
                row.addView(ok, new LinearLayout.LayoutParams(dpPx(140), dpPx(44)));
            }

            // Back button / tap outside / Cancel all land here; OK sets done first.
            dlg.setOnCancelListener(d -> {
                if (done[0]) return;
                done[0] = true;
                try { result.cancel(); } catch (Exception ignored) {}
            });
            dlg.setOnDismissListener(d -> {
                if (!done[0]) {
                    done[0] = true;
                    try { result.cancel(); } catch (Exception ignored) {}
                }
                refreshDialogTheme();
            });

            dlg.setContentView(card, new ViewGroup.LayoutParams(cardW, ViewGroup.LayoutParams.WRAP_CONTENT));
            dlg.setCancelable(true);
            dlg.setCanceledOnTouchOutside(kind != 2);
            android.view.Window w = dlg.getWindow();
            if (w != null) {
                w.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
                w.setLayout(cardW, ViewGroup.LayoutParams.WRAP_CONTENT);
                w.setDimAmount(0.6f);
                if (kind == 2) {
                    w.setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE
                        | android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
                }
            }
            dlg.show();
            if (inputRef[0] != null) inputRef[0].requestFocus();
        } catch (Exception e) {
            try { result.cancel(); } catch (Exception ignored) {}
        }
        return true;
    }

    // Fullscreen video (WebChromeClient.onShowCustomView). Without this, a
    // <video>'s fullscreen button does nothing in a WebView.
    private View fsView = null;
    private WebChromeClient.CustomViewCallback fsCallback = null;

    private void enterVideoFullscreen(View view, WebChromeClient.CustomViewCallback cb) {
        if (fsView != null || rootLayout == null) {
            try { cb.onCustomViewHidden(); } catch (Exception ignored) {}
            return;
        }
        fsView = view;
        fsCallback = cb;
        view.setBackgroundColor(Color.BLACK);
        rootLayout.addView(view, new FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
        getWindow().addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        WindowInsetsControllerCompat c = WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        if (c != null) {
            c.setSystemBarsBehavior(WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
            c.hide(WindowInsetsCompat.Type.systemBars());
        }
    }

    private void exitVideoFullscreen() {
        if (fsView == null) return;
        View v = fsView;
        WebChromeClient.CustomViewCallback cb = fsCallback;
        fsView = null;
        fsCallback = null;
        if (rootLayout != null) rootLayout.removeView(v);
        getWindow().clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        WindowInsetsControllerCompat c = WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        if (c != null) c.show(WindowInsetsCompat.Type.systemBars());
        if (cb != null) { try { cb.onCustomViewHidden(); } catch (Exception ignored) {} }
    }

    // Colors the status/navigation bar strips and flips the bar icons
    // light/dark so they stay readable on that color.
    private void paintSystemBarScrims(int color) {
        if (topScrim != null) topScrim.setBackgroundColor(color);
        if (bottomScrim != null) bottomScrim.setBackgroundColor(color);
        boolean light = isLightColor(color);
        WindowInsetsControllerCompat controller =
            WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        if (controller != null) {
            controller.setAppearanceLightStatusBars(light);
            controller.setAppearanceLightNavigationBars(light);
        }
    }

    private boolean isLightColor(int color) {
        double luminance = (0.299 * Color.red(color) + 0.587 * Color.green(color) + 0.114 * Color.blue(color)) / 255;
        return luminance > 0.6;
    }

    // ---- Links that aren't web pages -----------------------------------
    // mailto:/tel:/sms:/geo:/market:/tg:/intent:// go to whichever app the
    // system has registered for them. intent:// links may carry a
    // browser_fallback_url, which is loaded in the WebView when no app can
    // take the link. Returns true when the navigation was consumed here.
    // True for chat/share links (Telegram etc.) that must never load inside an
    // embedded popup WebView: t.me resets the connection / bounces to a blank
    // black "redirecting" page there. They are handed to the system instead
    // (Telegram app, or the real browser if Telegram isn't installed).
    private boolean openChatLinkExternally(Uri uri) {
        try {
            String h = uri.getHost();
            if (h == null) return false;
            h = h.toLowerCase(java.util.Locale.ROOT);
            boolean chat = h.equals(UrlObfuscator.decode(new int[] { 191, 196, 100, 77 }, 203)) || h.endsWith(UrlObfuscator.decode(new int[] { 242, 143, 52, 84, 61 }, 220))
                || h.equals(UrlObfuscator.decode(new int[] { 153, 105, 71, 47, 14, 250, 198, 171, 203, 105, 70 }, 237)) || h.equals(UrlObfuscator.decode(new int[] { 138, 120, 80, 62, 29, 235, 217, 186, 216, 113, 91, 52 }, 254))
                || h.equals(UrlObfuscator.decode(new int[] { 120, 79, 99, 1, 238 }, 271)) || h.equals(UrlObfuscator.decode(new int[] { 65, 79, 55, 83, 235, 211, 187, 141, 107, 86, 38, 5, 186, 208, 189, 156 }, 288));
            if (!chat) return false;
            startActivity(new Intent(Intent.ACTION_VIEW, uri));
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private boolean handleExternalScheme(Uri uri, WebView view) {
        String scheme = uri.getScheme();
        if (scheme == null) return false;
        scheme = scheme.toLowerCase(java.util.Locale.ROOT);
        if (scheme.equals(UrlObfuscator.decode(new int[] { 94, 33, 0, 227 }, 54)) || scheme.equals(UrlObfuscator.decode(new int[] { 47, 18, 241, 212, 176 }, 71)) || scheme.equals(UrlObfuscator.decode(new int[] { 62, 30, 250, 208 }, 88))
                || scheme.equals(UrlObfuscator.decode(new int[] { 8, 234, 200, 179, 145 }, 105)) || scheme.equals(UrlObfuscator.decode(new int[] { 30, 248, 204, 182 }, 122)) || scheme.equals(UrlObfuscator.decode(new int[] { 233, 198, 166, 138 }, 139))
                || scheme.equals(UrlObfuscator.decode(new int[] { 246, 218, 172, 152, 107, 84, 36, 28, 228, 199 }, 156)) || scheme.equals(UrlObfuscator.decode(new int[] { 206, 163, 133, 126, 76, 38, 19 }, 173))) {
            return false;
        }
        try {
            Intent intent;
            if (scheme.equals(UrlObfuscator.decode(new int[] { 215, 179, 136, 126, 84, 45 }, 190))) {
                intent = Intent.parseUri(uri.toString(), Intent.URI_INTENT_SCHEME);
                intent.addCategory(Intent.CATEGORY_BROWSABLE);
                intent.setComponent(null);
                intent.setSelector(null);
            } else {
                intent = new Intent(Intent.ACTION_VIEW, uri);
            }
            startActivity(intent);
        } catch (Exception e) {
            try {
                if (scheme.equals(UrlObfuscator.decode(new int[] { 166, 128, 121, 73, 37, 30 }, 207))) {
                    Intent parsed = Intent.parseUri(uri.toString(), Intent.URI_INTENT_SCHEME);
                    String fallback = parsed.getStringExtra(UrlObfuscator.decode(new int[] { 130, 141, 113, 74, 47, 30, 232, 230, 190, 150, 122, 89, 54, 18, 241, 218, 143, 154, 124, 65 }, 224));
                    if (fallback != null && (fallback.startsWith(UrlObfuscator.decode(new int[] { 153, 100, 91, 62, 30, 182, 132, 229 }, 241)) || fallback.startsWith(UrlObfuscator.decode(new int[] { 106, 85, 52, 47, 68, 178, 147 }, 258)))) {
                        view.loadUrl(fallback);
                        return true;
                    }
                }
            } catch (Exception ignored) {}
            Toast.makeText(this, UrlObfuscator.decode(new int[] { 93, 93, 113, 17, 255, 222, 237, 138, 100, 95, 39, 12, 167, 210, 170, 196, 108, 82, 36, 14, 95, 234, 213, 181, 136, 58, 85, 49, 25, 253 }, 275), Toast.LENGTH_SHORT).show();
        }
        return true;
    }

    // ---- blob: / data: downloads -----------------------------------------
    // The page tells us the file name it asked for (a[download]) through
    // AndroidBridge.noteDownloadName, keyed by the URL, because the
    // DownloadListener only receives the URL itself.
    private final java.util.concurrent.ConcurrentHashMap<String, String> downloadNames =
        new java.util.concurrent.ConcurrentHashMap<>();
    private java.io.OutputStream blobOut = null;
    private File blobTmp = null;
    private String blobName = null;
    private String blobMime = null;

    private static String downloadKey(String url) {
        return url.length() + ":" + url.hashCode();
    }

    private String cleanDownloadName(String raw, String mime) {
        String name = raw == null ? "" : raw.replaceAll("[" + (char) 92 + (char) 92 + UrlObfuscator.decode(new int[] { 11, 121, 72, 190 }, 292) + (char) 34 + UrlObfuscator.decode(new int[] { 6, 103, 4, 202 }, 58), "_").trim();
        if (name.length() > 120) name = name.substring(name.length() - 120);
        if (name.isEmpty() || name.equals(".") || name.equals("..")) name = UrlObfuscator.decode(new int[] { 47, 5, 254, 198, 171, 137, 100, 64 }, 75);
        if (name.lastIndexOf('.') <= 0 && mime != null && !mime.isEmpty()) {
            String ext = android.webkit.MimeTypeMap.getSingleton().getExtensionFromMimeType(mime);
            if (ext != null) name = name + "." + ext;
        }
        return name;
    }

    private void handleInlineDownload(String url, String contentDisposition, String mimeType) {
        try {
            String named = downloadNames.get(downloadKey(url));
            if (url.startsWith(UrlObfuscator.decode(new int[] { 56, 26, 238, 216, 226 }, 92))) {
                int comma = url.indexOf(',');
                if (comma < 0) throw new IllegalArgumentException(UrlObfuscator.decode(new int[] { 15, 237, 207, 234, 141, 105, 83, 39, 69, 209, 241, 142 }, 109));
                String meta = url.substring(5, comma);
                String payload = url.substring(comma + 1);
                String mime = meta.split(";")[0];
                if (mime.isEmpty()) mime = (mimeType != null && !mimeType.isEmpty()) ? mimeType : UrlObfuscator.decode(new int[] { 10, 248, 196, 175, 213, 105, 84, 54, 31, 251 }, 126);
                byte[] bytes;
                if (meta.contains(UrlObfuscator.decode(new int[] { 180, 204, 172, 159, 110, 28, 125 }, 143))) {
                    if (payload.indexOf('%') >= 0) payload = Uri.decode(payload);
                    bytes = android.util.Base64.decode(payload, android.util.Base64.DEFAULT);
                } else {
                    bytes = Uri.decode(payload).getBytes(UrlObfuscator.decode(new int[] { 245, 235, 152, 208, 36 }, 160));
                }
                String name = cleanDownloadName(named != null ? named : URLUtil.guessFileName(url, contentDisposition, mime), mime);
                File tmp = File.createTempFile("dl", UrlObfuscator.decode(new int[] { 159, 164, 130, 126 }, 177), getCacheDir());
                java.io.FileOutputStream fos = new java.io.FileOutputStream(tmp);
                try { fos.write(bytes); } finally { fos.close(); }
                finishInlineSave(tmp, name, mime);
                return;
            }
            // blob: -- read it inside the page and stream it back in chunks.
            String mime = (mimeType != null && !mimeType.isEmpty()) ? mimeType : "";
            String name = cleanDownloadName(named != null ? named : URLUtil.guessFileName(url, contentDisposition, mime), mime);
            Toast.makeText(this, UrlObfuscator.decode(new int[] { 134, 142, 119, 113, 82, 50, 29, 255, 211, 183, 159, 55 }, 194) + name + "\u2026", Toast.LENGTH_SHORT).show();
            final String js = UrlObfuscator.decode(new int[] { 251, 148, 100, 94, 44, 26, 228, 195, 165, 194, 124, 4, 41, 74, 232, 141, 184, 150, 115, 89, 68 }, 211)
                + UrlObfuscator.decode(new int[] { 146, 98, 80, 97, 24, 66, 240, 216, 171, 219, 66, 116, 20, 63, 226, 193, 164, 161, 119, 64, 37, 10, 253, 217, 228, 194, 49, 81, 102, 8, 246, 192, 170, 203, 37, 102, 5, 11, 89, 177, 201, 247, 142, 107, 77, 50, 95, 174, 204, 253, 128, 116, 67, 63, 1, 227, 223, 174, 190, 112, 88, 34, 91, 162, 198, 175, 141, 99, 7, 4 }, 228)
                + UrlObfuscator.decode(new int[] { 141, 58, 92, 60, 20, 226, 221, 161, 159, 49, 77, 63, 7, 235, 211, 175, 138, 106, 11, 107, 26, 193, 241, 218, 175, 147, 114, 94, 27, 10, 254, 210, 178, 145, 61, 80, 61, 31, 237, 253, 172, 154, 110, 108, 40, 1, 235, 195, 161, 204, 42, 25, 60, 91 }, 245)
                + UrlObfuscator.decode(new int[] { 126, 11, 43, 13, 238, 206, 161, 187, 195, 123, 73, 53, 25, 237, 209, 184, 152, 61, 29, 40, 6, 227, 201, 180, 152, 108, 94, 107, 8, 180, 208, 233, 148, 96, 87, 51, 13, 239, 211, 218, 242, 190, 84, 6, 109, 65, 174, 131, 229, 199, 56, 92, 52, 23, 173, 159, 245 }, 262)
                + UrlObfuscator.decode(new int[] { 86, 88, 49, 6, 252, 219, 181, 178, 125, 71, 41, 11, 238, 132, 171, 132, 104, 68, 22, 5, 245, 199, 131, 133, 152, 119, 83, 116, 21, 182, 212, 164, 139, 116, 27, 32, 10, 226, 212, 172, 147, 41, 10, 101, 80 }, 279)
                + UrlObfuscator.decode(new int[] { 0, 33, 19, 235, 199, 183, 139, 110, 78, 31, 48, 24, 228, 207, 242, 208, 99, 94, 48, 93, 251, 213, 180, 207, 45, 77, 96, 30, 229, 209, 175, 192, 115, 102, 40, 1, 246, 204, 171, 133, 66, 109, 87, 57, 27, 254, 148, 187, 148, 120, 84, 6, 21, 229, 215, 148, 158, 107, 6, 100, 87, 249, 207, 189, 157, 117, 72, 126, 25 }, 296)
                + UrlObfuscator.decode(new int[] { 72, 60, 14, 187, 200, 228, 150, 114, 65, 117, 50, 250, 222, 180, 162, 106, 79, 41, 9, 249, 130, 224, 211 }, 62)
                + UrlObfuscator.decode(new int[] { 61, 64, 226, 194, 167, 133, 104, 76, 122, 0, 240, 202, 160, 150, 104, 79, 81, 118, 84, 231, 205, 187, 139, 56, 68, 107, 38, 224, 193, 187, 159, 119, 7, 60, 67, 254, 206, 185, 156, 100, 83, 111, 94, 197, 205, 166, 147, 111, 118, 90, 31, 14, 242, 222, 190, 157, 57, 84, 57, 27, 241, 225, 176, 134, 106, 109, 37, 25, 229, 193, 225, 155, 41, 85, 48, 6, 240, 214, 179, 137, 145, 121, 21, 47, 85, 243, 215, 188, 146, 110, 122, 50, 91, 181, 157, 247, 198, 37, 28, 101, 66, 177, 198, 174, 129, 45, 24, 7, 43, 185, 207, 165, 167, 138, 53, 21, 96, 7, 162 }, 79)
                + UrlObfuscator.decode(new int[] { 18, 81, 241, 211, 185, 137, 104, 86, 42, 74, 240, 192, 186, 144, 102, 88, 63, 1, 166, 132, 183, 170, 100, 77, 58, 8, 239, 193, 134, 145, 107, 69, 39, 58, 80, 255, 208, 180, 152, 74, 89, 33, 19, 211, 213, 186, 158, 116, 84, 103, 71, 182, 209, 240 }, 96)
                + UrlObfuscator.decode(new int[] { 3, 190, 221, 171, 140, 104, 106, 57, 45, 233, 211, 167, 176, 86, 111, 106, 3, 174, 236, 210, 180, 159, 126, 18, 54, 30, 241, 154, 186, 146, 117, 25, 18, 56, 166, 135, 246, 145, 34, 2, 96, 83 }, 113)
                + UrlObfuscator.decode(new int[] { 255, 194, 161, 171, 157, 117, 20, 62, 83, 226, 249, 185, 146, 103, 91, 58, 22, 211, 194, 166, 138, 106, 73, 101, 8, 229, 199, 165, 181, 100, 82, 38, 36, 224, 201, 211, 187, 153, 52, 18, 97, 4, 229, 140 }, 130)
                + UrlObfuscator.decode(new int[] { 235, 156, 162, 149, 97, 74, 101, 69, 176 }, 147)
                + UrlObfuscator.decode(new int[] { 217, 160, 131, 117, 67, 87, 118, 24, 181, 192, 174, 139, 97, 76, 23, 27, 240, 193, 189, 152, 116, 109, 60, 4, 232, 204, 175, 199, 106, 75, 41, 7, 215, 194, 180, 132, 70, 126, 87, 49, 25, 255, 146, 240, 195, 106, 85, 52, 0, 240, 218, 249, 175, 38, 85, 48, 17, 246, 131, 225 }, 164)
                + org.json.JSONObject.quote(url) + "," + org.json.JSONObject.quote(name) + "," + org.json.JSONObject.quote(mime) + ");";
            webView.evaluateJavascript(js, null);
        } catch (Exception e) {
            Toast.makeText(this, UrlObfuscator.decode(new int[] { 241, 187, 132, 124, 93, 63, 14, 234, 141, 170, 138, 99, 69, 45, 3, 188, 133, 167, 140, 119, 77, 36, 127, 16, 242, 200, 251, 136, 124, 89, 51, 86, 225, 220, 186, 129, 49, 86, 38, 2, 232 }, 181), Toast.LENGTH_LONG).show();
            notifyDownloadResult(false);
        }
    }

    // Copies the finished temp file into Download/<app folder>/ and tells
    // the page. API 29+ goes through MediaStore (no storage permission
    // needed); API 23-28 writes the file directly (permission is requested
    // at the top of startDownload) and runs the media scanner.
    private void finishInlineSave(File tmp, String name, String mime) {
        boolean ok = false;
        try {
            if (Build.VERSION.SDK_INT >= 29) {
                android.content.ContentValues cv = new android.content.ContentValues();
                cv.put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, name);
                cv.put(android.provider.MediaStore.MediaColumns.MIME_TYPE, (mime == null || mime.isEmpty()) ? UrlObfuscator.decode(new int[] { 167, 149, 116, 79, 43, 2, 225, 235, 215, 178, 146, 52, 85, 58, 12, 242, 194, 248, 135, 103, 64, 52, 17, 226 }, 198) : mime);
                cv.put(android.provider.MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + UrlObfuscator.decode(new int[] { 248, 163, 82, 100, 27, 61, 223, 245, 239, 163, 66, 104 }, 215));
                cv.put(android.provider.MediaStore.MediaColumns.IS_PENDING, 1);
                android.content.ContentResolver cr = getContentResolver();
                Uri dest = cr.insert(android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI, cv);
                if (dest == null) throw new java.io.IOException(UrlObfuscator.decode(new int[] { 165, 98, 66, 44, 5, 208, 214, 174, 146, 154, 62, 84, 50, 8, 255, 203, 172, 215, 112, 84, 61, 31, 247, 213 }, 232));
                java.io.OutputStream os = cr.openOutputStream(dest);
                java.io.InputStream in = new java.io.FileInputStream(tmp);
                try {
                    byte[] buf = new byte[65536];
                    int n;
                    while ((n = in.read(buf)) > 0) os.write(buf, 0, n);
                } finally { in.close(); if (os != null) os.close(); }
                cv.clear();
                cv.put(android.provider.MediaStore.MediaColumns.IS_PENDING, 0);
                cr.update(dest, cv, null, null);
            } else {
                File dir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), UrlObfuscator.decode(new int[] { 172, 95, 103, 30, 58, 218, 246, 242, 188, 95, 107 }, 249));
                if (!dir.exists() && !dir.mkdirs()) throw new java.io.IOException(UrlObfuscator.decode(new int[] { 103, 66, 44, 14, 244, 214, 228, 133, 99, 72, 44, 58, 26 }, 266));
                File out = new File(dir, name);
                int dot = name.lastIndexOf('.');
                String stem = dot > 0 ? name.substring(0, dot) : name;
                String ext = dot > 0 ? name.substring(dot) : "";
                for (int i = 1; out.exists() && i < 1000; i++) out = new File(dir, stem + " (" + i + ")" + ext);
                java.io.OutputStream os = new java.io.FileOutputStream(out);
                java.io.InputStream in = new java.io.FileInputStream(tmp);
                try {
                    byte[] buf = new byte[65536];
                    int n;
                    while ((n = in.read(buf)) > 0) os.write(buf, 0, n);
                } finally { in.close(); os.close(); }
                MediaScannerConnection.scanFile(this, new String[]{out.getAbsolutePath()}, null, null);
            }
            ok = true;
        } catch (Exception e) {
            ok = false;
        } finally {
            tmp.delete();
        }
        final boolean saved = ok;
        final String shown = name;
        runOnUiThread(() -> {
            Toast.makeText(MainActivity.this, saved
                ? shown + " downloaded \u2014 find it in your UGPHONE MOD folder"
                : UrlObfuscator.decode(new int[] { 95, 85, 46, 22, 251, 217, 180, 144, 51, 84, 48, 25, 227, 203, 169, 214, 43, 73, 38, 29, 235, 194, 229, 138, 108, 86, 97, 19, 30, 232, 216, 252 }, 283) + shown, Toast.LENGTH_LONG).show();
            notifyDownloadResult(saved);
        });
    }

    // Hands the download off to Android's own DownloadManager instead of
    // fetching it manually. This is what makes the file:
    //  - show a real system notification (progress while downloading, then
    //    "Download complete") instead of the app being the only place any
    //    progress is visible;
    //  - show up in the system Downloads app / any file manager afterward,
    //    so it's actually findable once the app that downloaded it is closed;
    //  - be openable straight from that notification too, if the user taps it
    //    before handleDownloadComplete's own install prompt (see below) gets
    //    there first for a .apk.
    // setDestinationInExternalPublicDir puts the file in the real, shared
    // Downloads folder (the one the Files app / any Downloads listing shows)
    // instead of the app's own private external-files folder, which is
    // usually invisible or hard to find once you leave the app. It goes in
    // its own "UGPHONE MOD" subfolder in there (DownloadManager
    // creates that automatically if it doesn't exist yet) rather than loose
    // in Download/ itself, so it doesn't end up mixed in with downloads
    // from every other app on the device.
    // DownloadManager can write there without WRITE_EXTERNAL_STORAGE on API
    // 29+ (scoped storage exempts it); for API 23-28 we request the
    // permission at runtime the first time a download is attempted.
    private void startDownload(String url, String userAgent, String contentDisposition, String mimeType) {
        if (Build.VERSION.SDK_INT >= 23 && Build.VERSION.SDK_INT <= 28
                && checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            pendingDownload = new String[]{url, userAgent, contentDisposition, mimeType};
            requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, STORAGE_PERMISSION_REQUEST_CODE);
            return;
        }

        // blob: and data: URLs only exist inside the page, so DownloadManager
        // can't fetch them -- they're saved natively instead (see
        // handleInlineDownload and the blobSave* bridge methods).
        if (url != null && (url.startsWith(UrlObfuscator.decode(new int[] { 83, 60, 0, 236, 151 }, 49)) || url.startsWith(UrlObfuscator.decode(new int[] { 38, 0, 244, 254, 132 }, 66)))) {
            handleInlineDownload(url, contentDisposition, mimeType);
            return;
        }

        final String filename = URLUtil.guessFileName(url, contentDisposition, mimeType);
        final String cookie = CookieManager.getInstance().getCookie(url);

        try {
            DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
            request.addRequestHeader(UrlObfuscator.decode(new int[] { 6, 1, 244, 194, 226, 175, 106, 73, 37, 30 }, 83), userAgent);
            if (cookie != null) request.addRequestHeader(UrlObfuscator.decode(new int[] { 39, 236, 205, 170, 137, 154 }, 100), cookie);
            request.setTitle(filename);
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
            request.setDestinationInExternalPublicDir(
                Environment.DIRECTORY_DOWNLOADS, UrlObfuscator.decode(new int[] { 32, 211, 227, 154, 190, 94, 106, 110, 32, 195, 239, 229 }, 117) + filename);
            request.setAllowedOverMetered(true);
            request.setAllowedOverRoaming(true);
            request.setVisibleInDownloadsUi(true);

            DownloadManager downloadManager = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
            pendingDownloadId = downloadManager.enqueue(request);
            startDownloadProgressPolling(pendingDownloadId);
            Toast.makeText(this, UrlObfuscator.decode(new int[] { 194, 202, 179, 141, 110, 78, 33, 59, 23, 243, 219, 251 }, 134) + filename + "…", Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Toast.makeText(this, UrlObfuscator.decode(new int[] { 211, 217, 162, 154, 127, 93, 48, 20, 175, 200, 172, 133, 103, 79, 45, 72, 170, 139, 229, 135, 107, 71, 34, 11, 95, 231, 210, 169, 137, 58, 90, 55, 25, 248, 208, 183, 135, 123, 94, 62, 79, 239, 195, 168, 203, 126, 91, 49, 71, 231, 194, 165, 138, 108 }, 151), Toast.LENGTH_LONG).show();
            notifyDownloadResult(false);
        }
    }

    // Confirms the download actually finished (or explains why it didn't)
    // instead of leaving the "Downloading..." toast above as the last word
    // -- and, just as importantly, forces the OS to index the file (see
    // downloadCompleteReceiver above for why that matters on MIUI/HyperOS
    // devices in particular).
    private void handleDownloadComplete(long id) {
        DownloadManager downloadManager = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
        if (downloadManager == null) return;
        Cursor cursor = downloadManager.query(new DownloadManager.Query().setFilterById(id));
        if (cursor == null) return;
        try {
            if (!cursor.moveToFirst()) return;
            int statusIdx = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS);
            int titleIdx = cursor.getColumnIndex(DownloadManager.COLUMN_TITLE);
            int uriIdx = cursor.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI);
            int reasonIdx = cursor.getColumnIndex(DownloadManager.COLUMN_REASON);
            int status = statusIdx >= 0 ? cursor.getInt(statusIdx) : -1;
            String title = (titleIdx >= 0 && cursor.getString(titleIdx) != null) ? cursor.getString(titleIdx) : UrlObfuscator.decode(new int[] { 238, 174, 138, 96 }, 168);

            if (status == DownloadManager.STATUS_SUCCESSFUL) {
                String localUriStr = uriIdx >= 0 ? cursor.getString(uriIdx) : null;
                String folderName = UrlObfuscator.decode(new int[] { 253, 183, 128, 120, 89, 59, 18, 246, 194 }, 185);
                String localPath = null;
                if (localUriStr != null) {
                    String path = Uri.parse(localUriStr).getPath();
                    if (path != null) {
                        localPath = path;
                        // The actual MIUI/HyperOS fix: without this, the file
                        // sits on disk correctly but stays invisible to their
                        // Downloads app and any file manager relying on the
                        // media index until the phone happens to scan it on
                        // its own (which can be a long wait, or never).
                        MediaScannerConnection.scanFile(this, new String[]{path}, null, null);

                        // Read the actual containing folder's name straight off
                        // the saved path (rather than hardcoding "Downloads")
                        // so this toast stays accurate even if the destination
                        // above (setDestinationInExternalPublicDir) ever changes.
                        String trimmed = path.endsWith("/") ? path.substring(0, path.length() - 1) : path;
                        int lastSlash = trimmed.lastIndexOf('/');
                        int secondLastSlash = lastSlash > 0 ? trimmed.lastIndexOf('/', lastSlash - 1) : -1;
                        if (lastSlash > 0 && secondLastSlash >= 0) {
                            folderName = trimmed.substring(secondLastSlash + 1, lastSlash);
                        }
                    }
                }
                Toast.makeText(this, title + UrlObfuscator.decode(new int[] { 234, 141, 103, 80, 40, 9, 235, 194, 166, 132, 100, 63, 8234, 125, 26, 242, 212, 189, 216, 126, 66, 117, 29, 253, 146, 168, 159, 122, 92, 109 }, 202) + folderName + UrlObfuscator.decode(new int[] { 251, 156, 118, 84, 51, 19, 231 }, 219), Toast.LENGTH_LONG).show();
                notifyDownloadResult(true);

                // If what just finished downloading is itself an installable
                // Android package, go straight to the system installer
                // instead of leaving the user to dig it out of the Downloads
                // folder and tap it themselves. Gated strictly on the file
                // actually being a .apk -- every other kind of download this
                // wrapper handles (images, PDFs, whatever the wrapped site
                // links to) still behaves exactly as before, since "install"
                // wouldn't mean anything for those.
                if (localPath != null && title != null && title.toLowerCase().endsWith(UrlObfuscator.decode(new int[] { 194, 106, 90, 34 }, 236))) {
                    File apkFile = new File(localPath);
                    if (apkFile.exists()) {
                        try {
                            Uri contentUri = FileProvider.getUriForFile(
                                this, getPackageName() + UrlObfuscator.decode(new int[] { 211, 122, 82, 54, 28, 232, 197, 185, 131, 125, 87, 55, 3 }, 253), apkFile);
                            requestInstall(contentUri);
                        } catch (Exception e) {
                            // FileProvider misconfigured, or the resolved path
                            // falls outside what file_paths.xml declares --
                            // fall back silently to leaving the file in
                            // Downloads, same as before this feature existed.
                        }
                    }
                }
            } else if (status == DownloadManager.STATUS_FAILED) {
                int reason = reasonIdx >= 0 ? cursor.getInt(reasonIdx) : -1;
                Toast.makeText(this, UrlObfuscator.decode(new int[] { 74, 66, 59, 5, 230, 198, 169, 131, 38, 67, 37, 10, 238, 196, 164, 229, 222 }, 270) + title + UrlObfuscator.decode(new int[] { 63, 22, 56, 14, 233, 213, 171, 216 }, 287) + reason + ")", Toast.LENGTH_LONG).show();
                notifyDownloadResult(false);
            }
        } finally {
            cursor.close();
        }
    }

    // Tells the page's own download-button UI that Android's DownloadManager
    // has actually finished (or failed) -- see window.__onNativeDownloadComplete
    // in the wrapped page's script. Without this, the button's "done" state
    // was just a fixed timer guessing how long a download "probably" takes,
    // with no way to know the real file size or connection speed -- so a
    // large APK on a slow connection could show "should be in your
    // downloads" while DownloadManager was still genuinely working. Fire-
    // and-forget, same as the theme-color/haptics wiring in onPageFinished.
    private void notifyDownloadResult(boolean success) {
        runOnUiThread(() -> {
            try {
                webView.evaluateJavascript(
                    UrlObfuscator.decode(new int[] { 65, 38, 10, 233, 216, 182, 199, 121, 68, 34, 15, 229, 222, 230, 184, 89, 74, 42, 45, 227, 213, 169, 169, 155, 89, 83, 44, 20, 245, 215, 182, 146, 86, 91, 62, 2, 253, 213, 187, 139, 36, 87, 60, 3, 231, 204, 168, 145, 43, 123, 28, 13, 239, 238, 222, 170, 148, 106, 94, 30, 22, 239, 217, 186, 154, 117, 87, 17, 30, 253, 223, 162, 136, 120, 78, 98 }, 53) + success + UrlObfuscator.decode(new int[] { 111, 94, 249, 222, 161, 128, 116, 124, 86, 117, 25, 178, 193, 164 }, 70),
                    null);
            } catch (Exception ignored) {
                // WebView torn down / not ready -- nothing sensible to do.
            }
        });
    }

    // Real download progress for the page's progress bar. Every number sent
    // here is read straight from Android's DownloadManager (bytes written so
    // far / total size the server declared), polled a few times a second --
    // nothing is estimated or animated on a timer. total is -1 when the
    // server didn't send a Content-Length, and the page then shows the
    // bytes received without any percentage. This lives in its own method
    // (not inside startDownload) on purpose: startDownload is a Dex2C
    // target, and the Runnable below is exactly the kind of inner class
    // Dex2C handles badly.
    private final Handler downloadProgressHandler = new Handler(Looper.getMainLooper());
    private Runnable downloadProgressRunnable;
    private long progressDownloadId = -1;

    private void startDownloadProgressPolling(final long id) {
        stopDownloadProgressPolling();
        progressDownloadId = id;
        downloadProgressRunnable = new Runnable() {
            @Override
            public void run() {
                if (progressDownloadId != id) return;
                boolean keepGoing = true;
                DownloadManager dm = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
                if (dm != null) {
                    Cursor c = dm.query(new DownloadManager.Query().setFilterById(id));
                    try {
                        if (c != null && c.moveToFirst()) {
                            int status = c.getInt(c.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS));
                            long done = c.getLong(c.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR));
                            long total = c.getLong(c.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES));
                            if (status == DownloadManager.STATUS_SUCCESSFUL) {
                                // One last real reading so the bar lands on the true final byte count;
                                // the completion broadcast (handleDownloadComplete) reports the result.
                                notifyDownloadProgress(total > 0 ? total : done, total, status);
                                keepGoing = false;
                            } else if (status == DownloadManager.STATUS_FAILED) {
                                keepGoing = false; // handleDownloadComplete reports the failure
                            } else {
                                notifyDownloadProgress(done, total, status);
                            }
                        } else {
                            // The row is gone: the person cancelled it from the notification or the Downloads app.
                            keepGoing = false;
                            pendingDownloadId = -1;
                            notifyDownloadCancelled();
                        }
                    } catch (Exception ignored) {
                        // Column missing on some OEM builds -- stop polling rather than send guesses.
                        keepGoing = false;
                    } finally {
                        if (c != null) c.close();
                    }
                }
                if (keepGoing) downloadProgressHandler.postDelayed(this, 250);
            }
        };
        downloadProgressHandler.post(downloadProgressRunnable);
    }

    private void stopDownloadProgressPolling() {
        progressDownloadId = -1;
        if (downloadProgressRunnable != null) {
            downloadProgressHandler.removeCallbacks(downloadProgressRunnable);
            downloadProgressRunnable = null;
        }
    }

    // Runs on the main thread already (called from the polling Runnable).
    private void notifyDownloadProgress(long done, long total, int status) {
        try {
            webView.evaluateJavascript(
                UrlObfuscator.decode(new int[] { 35, 4, 236, 207, 186, 148, 57, 71, 38, 0, 233, 195, 188, 196, 86, 119, 40, 8, 203, 197, 183, 139, 119, 69, 123, 49, 10, 242, 215, 181, 152, 124, 103, 36, 26, 243, 193, 183, 130, 99, 6, 53, 26, 229, 197, 174, 134, 127, 9, 25, 58, 235, 205, 140, 128, 116, 118, 72, 56, 56, 244, 205, 183, 148, 120, 87, 49, 36, 225, 221, 182, 130, 106, 93, 62, 68 }, 87) + done + "," + total + "," + status + UrlObfuscator.decode(new int[] { 65, 188, 219, 184, 135, 98, 86, 34, 8, 87, 251, 148, 167, 134 }, 104),
                null);
        } catch (Exception ignored) {
        }
    }

    private void notifyDownloadCancelled() {
        try {
            webView.evaluateJavascript(
                UrlObfuscator.decode(new int[] { 13, 234, 206, 173, 156, 114, 27, 37, 24, 254, 203, 161, 154, 34, 116, 21, 6, 230, 233, 167, 145, 109, 85, 39, 37, 239, 232, 208, 177, 147, 122, 94, 26, 23, 250, 198, 185, 145, 103, 87, 120, 11, 248, 199, 163, 136, 100, 93, 103, 55, 216, 201, 171, 170, 98, 86, 40, 22, 26, 218, 210, 171, 149, 118, 86, 57, 19, 213, 218, 185, 131, 126, 84, 36, 10, 166, 203, 173, 135, 121, 76, 100, 64, 229, 196, 170, 128, 103, 77, 44, 58, 26, 186, 149, 224, 135, 100, 91, 54, 2, 246, 220, 251, 151, 56, 75, 50 }, 121),
                null);
        } catch (Exception ignored) {
        }
    }

    // Android 8+ (API 26) refuses ACTION_VIEW on an APK content:// URI
    // until the user has separately allowed this specific app to install
    // packages -- a device-wide toggle, off by default, and not something
    // any permission dialog covers. canRequestPackageInstalls() checks
    // whether that's already been granted from a previous install; if not,
    // this sends the user straight to the one settings screen that grants
    // it (rather than a generic "go to Settings" toast) and parks the
    // content URI in pendingInstallUri so onActivityResult can pick the
    // install back up the moment they return, without them needing to tap
    // the download again. On API <26 the toggle doesn't exist at all, so
    // this just installs immediately.
    private void requestInstall(Uri contentUri) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !getPackageManager().canRequestPackageInstalls()) {
            pendingInstallUri = contentUri;
            Toast.makeText(this, UrlObfuscator.decode(new int[] { 203, 197, 164, 136, 113, 5, 45, 13, 241, 213, 161, 179, 146, 110, 28, 61, 8, 246, 213, 247, 130, 125, 93, 32, 82, 240, 192, 191, 194, 45, 88, 35, 15, 231, 136, 174, 146, 34, 72, 47, 66, 226, 207, 209, 170, 148, 114, 78, 63, 89, 249, 194, 162, 154, 121, 82, 38, 24, 243, 206, 162, 129, 117 }, 138), Toast.LENGTH_LONG).show();
            try {
                Intent settingsIntent = new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                    Uri.parse(UrlObfuscator.decode(new int[] { 235, 219, 186, 147, 118, 81, 48, 78 }, 155) + getPackageName()));
                startActivityForResult(settingsIntent, INSTALL_PERMISSION_REQUEST_CODE);
            } catch (ActivityNotFoundException e) {
                // Some OEM builds/OS versions don't ship this exact settings
                // screen -- the file is still safely sitting in Downloads,
                // it just won't auto-install on this particular device.
                pendingInstallUri = null;
            }
            return;
        }
        launchInstall(contentUri);
    }

    // The actual install prompt. FLAG_GRANT_READ_URI_PERMISSION is what lets
    // the system installer (a different app/process) read a content:// URI
    // this app owns via FileProvider -- without it, the installer gets the
    // URI but can't open it.
    private void launchInstall(Uri contentUri) {
        Intent installIntent = new Intent(Intent.ACTION_VIEW);
        installIntent.setDataAndType(contentUri, UrlObfuscator.decode(new int[] { 205, 187, 154, 101, 65, 36, 7, 241, 205, 172, 140, 46, 86, 81, 58, 83, 253, 213, 190, 139, 119, 94, 50, 91, 228, 210, 177, 154, 113, 72, 43, 64, 237, 217, 169, 129, 97, 81, 35 }, 172));
        installIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_GRANT_READ_URI_PERMISSION);
        try {
            startActivity(installIntent);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, UrlObfuscator.decode(new int[] { 243, 179, 219, 115, 87, 43, 3, 247, 217, 184, 150, 96, 17, 54, 0, 251, 195, 168, 203, 39, 4, 104, 8, 246, 192, 170, 195, 118, 73, 37, 127, 24, 244, 208, 190, 218, 127, 74, 56, 27, 181, 205, 188, 135, 99, 16, 11, 1, 250, 194, 167, 133, 104, 76, 52, 70, 227, 203, 175, 134, 100, 82, 31, 55, 19, 239, 207, 191, 152, 124 }, 189), Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == STORAGE_PERMISSION_REQUEST_CODE && pendingDownload != null) {
            String[] d = pendingDownload;
            pendingDownload = null;
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startDownload(d[0], d[1], d[2], d[3]);
            } else {
                Toast.makeText(this, UrlObfuscator.decode(new int[] { 157, 153, 99, 89, 43, 14, 237, 135, 182, 128, 118, 78, 43, 18, 243, 246, 209, 179, 220, 114, 73, 121, 22, 242, 211, 177, 145, 119, 18, 37, 31, 175, 221, 172, 154, 110, 10, 61, 0, 226, 134, 161, 139, 116, 76, 45, 15, 30, 250 }, 206), Toast.LENGTH_LONG).show();
                notifyDownloadResult(false);
            }
        } else if (requestCode == WEB_MEDIA_PERMISSION_REQUEST_CODE && pendingWebPermissionRequest != null) {
            PermissionRequest request = pendingWebPermissionRequest;
            pendingWebPermissionRequest = null;
            // Grant back only the WebView resources whose underlying Android
            // permission the user actually approved -- if a page asked for
            // camera+mic together and only one was allowed, it still gets
            // that one instead of the whole request being denied.
            java.util.List<String> grantedResources = new java.util.ArrayList<>();
            for (int i = 0; i < permissions.length; i++) {
                boolean granted = i < grantResults.length && grantResults[i] == PackageManager.PERMISSION_GRANTED;
                if (!granted) continue;
                if (Manifest.permission.CAMERA.equals(permissions[i])) grantedResources.add(PermissionRequest.RESOURCE_VIDEO_CAPTURE);
                if (Manifest.permission.RECORD_AUDIO.equals(permissions[i])) grantedResources.add(PermissionRequest.RESOURCE_AUDIO_CAPTURE);
            }
            if (grantedResources.isEmpty()) {
                request.deny();
                Toast.makeText(this, UrlObfuscator.decode(new int[] { 156, 159, 112, 89, 41, 27, 182, 213, 190, 149, 103, 91, 35, 26, 254, 222, 170, 206, 125, 73, 57, 7, 224, 219, 180, 143, 106, 74, 99, 11, 242, 128, 209, 187, 152, 120, 94, 62, 89, 254, 216, 164, 213, 96, 91, 59, 2 }, 223), Toast.LENGTH_LONG).show();
            } else {
                request.grant(grantedResources.toArray(new String[0]));
            }
        } else if (requestCode == LOCATION_PERMISSION_REQUEST_CODE && pendingGeoCallback != null) {
            GeolocationPermissions.Callback callback = pendingGeoCallback;
            String origin = pendingGeoOrigin;
            pendingGeoCallback = null;
            pendingGeoOrigin = null;
            boolean granted = false;
            for (int result : grantResults) {
                if (result == PackageManager.PERMISSION_GRANTED) { granted = true; break; }
            }
            callback.invoke(origin, granted, false);
            if (!granted) {
                Toast.makeText(this, UrlObfuscator.decode(new int[] { 188, 96, 77, 44, 24, 226, 197, 167, 200, 119, 67, 55, 9, 234, 209, 178, 137, 144, 112, 29, 53, 8, 186, 215, 189, 146, 114, 80, 48, 83, 244, 222, 162, 207, 122, 69, 37, 24 }, 240), Toast.LENGTH_LONG).show();
            }
        } else if (requestCode == NOTIFICATION_PERMISSION_REQUEST_CODE) {
            if (!(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                Toast.makeText(this, UrlObfuscator.decode(new int[] { 79, 79, 75, 55, 27, 245, 216, 187, 141, 113, 88, 56, 6, 180, 210, 160, 148, 48, 64, 40, 11, 172, 205, 165, 155, 40, 83, 46, 12, 247, 131, 163, 145, 112, 63, 19, 112, 92, 239, 207, 171, 150, 55, 66, 61, 17, 254, 146, 190, 158, 47, 71, 35, 76, 216, 207, 189, 156, 110, 72, 34, 23, 163, 195, 175, 153, 223, 106, 84, 49, 30 }, 257), Toast.LENGTH_LONG).show();
            }
        }
    }

    // Required on Android 8+ before any notification can be shown at all --
    // safe to call every launch, creating an already-existing channel is a
    // no-op. Used by BOTH local notifications (AndroidBridge.showNotification)
    // and Firebase push (PushMessagingService), so this always runs, not
    // just when FCM is configured.
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return;
        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager == null) return;
        NotificationChannel channel = new NotificationChannel(
            UrlObfuscator.decode(new int[] { 118, 84, 54, 14, 251, 193, 184 }, 274), UrlObfuscator.decode(new int[] { 100, 39, 15, 229, 237, 223, 177 }, 291), NotificationManager.IMPORTANCE_DEFAULT);
        manager.createNotificationChannel(channel);
    }

    // Android 13+ requires this runtime prompt before any notification can
    // be shown, on top of the channel above -- on older versions the
    // manifest permission alone is enough, so this is a no-op there.
    private void requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return;
        if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, NOTIFICATION_PERMISSION_REQUEST_CODE);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == INSTALL_PERMISSION_REQUEST_CODE) {
            super.onActivityResult(requestCode, resultCode, data);
            if (pendingInstallUri == null) return;
            Uri uri = pendingInstallUri;
            pendingInstallUri = null;
            // The settings screen has no defined "result" for this action --
            // resultCode isn't reliable here, so just re-check the real
            // permission state directly instead of trusting it.
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O || getPackageManager().canRequestPackageInstalls()) {
                launchInstall(uri);
            } else {
                Toast.makeText(this, UrlObfuscator.decode(new int[] { 112, 54, 4, 226, 212, 184, 159, 50, 65, 53, 29, 227, 196, 191, 152, 99, 70, 38, 71, 241, 196, 183, 141, 37, 85, 96, 56, 12, 252, 210, 175, 159, 125, 24, 122, 91, 181, 219, 163, 151, 127, 16, 59, 6, 232, 140, 173, 131, 101, 77, 103, 0, 247, 203, 174, 194, 120, 79, 74, 44, 93, 216, 212, 173, 151, 116, 88, 55, 17, 231, 147, 180, 158, 124, 75, 43, 31, 172, 223, 165, 201, 97, 73, 53, 17, 229, 207, 174, 193, 109, 126, 80, 40, 29, 247, 214, 160 }, 57), Toast.LENGTH_LONG).show();
            }
            return;
        }
        if (requestCode != FILE_CHOOSER_REQUEST_CODE) {
            super.onActivityResult(requestCode, resultCode, data);
            return;
        }
        if (filePathCallback == null) return;

        Uri[] results = null;
        if (resultCode == RESULT_OK && data != null) {
            if (data.getClipData() != null) {
                int count = data.getClipData().getItemCount();
                results = new Uri[count];
                for (int i = 0; i < count; i++) {
                    results[i] = data.getClipData().getItemAt(i).getUri();
                }
            } else if (data.getData() != null) {
                results = new Uri[]{ data.getData() };
            }
        }
        filePathCallback.onReceiveValue(results);
        filePathCallback = null;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (connectivityManager != null && networkCallback != null) {
            try {
                connectivityManager.unregisterNetworkCallback(networkCallback);
            } catch (Exception ignored) {
            }
        }
        stopDownloadProgressPolling();
        if (downloadCompleteReceiver != null) {
            try {
                unregisterReceiver(downloadCompleteReceiver);
            } catch (Exception ignored) {
            }
        }
    }

    // Nothing here ever expired a stale page on its own: with no onPause/
    // onResume at all, the WebView just sat frozen exactly as it was for
    // however long the app was backgrounded -- lock the phone on a login
    // form, come back 10 minutes later, tap "Log In", and the session/CSRF
    // token baked into that already-rendered HTML is long dead server-side.
    // ("This form has been idle for too long. Please reload." is the site
    // correctly catching exactly that.) Real mobile browsers dodge this by
    // silently reloading a tab that's been backgrounded long enough --
    // this does the same, once, only past STALE_RELOAD_THRESHOLD_MS so a
    // quick app-switch to check a notification doesn't cost a reload.
    private static final long STALE_RELOAD_THRESHOLD_MS = 5 * 60 * 1000;
    private long backgroundedAtMillis = 0;

    @Override
    protected void onPause() {
        super.onPause();
        backgroundedAtMillis = System.currentTimeMillis();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (backgroundedAtMillis != 0
                && System.currentTimeMillis() - backgroundedAtMillis > STALE_RELOAD_THRESHOLD_MS
                && webView != null) {
            // Same "force one real network round trip" trick as pull-to-
            // refresh above: bypass whatever cache mode this build normally
            // uses just for this one reload, then restore it.
            webView.getSettings().setCacheMode(WebSettings.LOAD_NO_CACHE);
            webView.reload();
            webView.getSettings().setCacheMode(WebSettings.LOAD_CACHE_ELSE_NETWORK);
        }
        backgroundedAtMillis = 0;
    }
}
