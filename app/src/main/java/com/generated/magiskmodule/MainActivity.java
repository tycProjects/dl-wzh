package com.generated.magiskmodule;

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
    // Branded "page didn't load"/"no internet" screens, and the exit-dialog
    // text below -- all AES-encrypted (see ScreenCrypto, ScreenCrypto.java)
    // so none of it sits as plain text in classes.dex. Decrypted once, on
    // first use, and cached -- decryptText() does real work (Base64 decode
    // + AES), not something to repeat on every WebView error.
    private static final String ERROR_PAGE_HTML_ENC = "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";
    private static final String NO_INTERNET_PAGE_HTML_ENC = "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";
    private static final String EXIT_DIALOG_TITLE_ENC = UrlObfuscator.decode(new int[] { 26, 12, 28, 168, 203, 190, 188, 95, 113, 98, 6, 218, 228, 188, 171, 70, 5, 37, 65, 185, 205, 164, 166, 98, 114, 44, 62, 213, 229, 172, 128, 115, 79, 108, 107, 9, 207, 140, 234, 191, 51, 90, 4, 55, 218, 214, 138, 151, 115, 106, 13, 42, 254, 232, 179, 153, 126, 18, 53, 39, 180, 193, 167, 141 }, 64);
    private static final String EXIT_DIALOG_MESSAGE_ENC = UrlObfuscator.decode(new int[] { 99, 5, 215, 212, 161, 180, 65, 120, 60, 12, 209, 224, 145, 210, 117, 18, 22, 48, 5, 201, 254, 180, 188, 115, 113, 14, 32, 224, 207, 151, 158, 115, 120, 19, 55, 193, 158, 245, 196, 91, 25, 9, 6, 208, 210, 171, 136, 49, 99, 47, 107, 36, 205, 147, 151, 176, 75, 124, 58, 2, 224, 223, 177, 195 }, 81);
    private static final String EXIT_DIALOG_EXIT_BTN_ENC = UrlObfuscator.decode(new int[] { 36, 181, 238, 141, 188, 187, 118, 99, 106, 27, 244, 244, 143, 176, 78, 114, 11, 29, 217, 224, 132, 161, 59, 92, 6, 47, 220, 213, 145, 209, 75, 96, 21, 47, 233, 243, 242, 177, 147, 46, 66, 26, 27, 170 }, 98);
    private static final String EXIT_DIALOG_STAY_BTN_ENC = UrlObfuscator.decode(new int[] { 25, 230, 247, 182, 162, 72, 95, 99, 46, 239, 198, 249, 162, 72, 96, 7, 17, 230, 192, 165, 179, 181, 119, 73, 24, 0, 243, 234, 179, 221, 115, 5, 101, 36, 217, 137, 130, 139, 76, 100, 39, 7, 226, 149 }, 115);
    private String errorPageHtmlCache;
    private String noInternetPageHtmlCache;
    private String getErrorPageHtml() {
        if (errorPageHtmlCache == null) errorPageHtmlCache = ScreenCrypto.decryptText(ERROR_PAGE_HTML_ENC);
        return errorPageHtmlCache;
    }
    private String getNoInternetPageHtml() {
        if (noInternetPageHtmlCache == null) noInternetPageHtmlCache = ScreenCrypto.decryptText(NO_INTERNET_PAGE_HTML_ENC);
        return noInternetPageHtmlCache;
    }

    // Page-level script: placeholders for failed images / iframes, reload bar for
    // failed scripts and stylesheets (see RESOURCE_ERROR_JS in server.js).
    private static final String RESOURCE_ERROR_JS = "(function () {\n  if (window.__tyResErr) return;\n  window.__tyResErr = true;\n  var SANS = '-apple-system, BlinkMacSystemFont, \"SF Pro Text\", \"Helvetica Neue\", system-ui, Roboto, sans-serif';\n  var PH = 'data:image/svg+xml;utf8,' + encodeURIComponent(\n    '<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"64\" height=\"64\" viewBox=\"0 0 64 64\">' +\n    '<rect width=\"64\" height=\"64\" rx=\"10\" fill=\"#8d8d94\" fill-opacity=\".14\"/>' +\n    '<g fill=\"none\" stroke=\"#8d8d94\" stroke-width=\"3\" stroke-linecap=\"round\" stroke-linejoin=\"round\">' +\n    '<rect x=\"14\" y=\"17\" width=\"36\" height=\"30\" rx=\"5\"/><path d=\"M14 40l10-9 8 7 6-5 12 9\"/>' +\n    '<path d=\"M10 54L54 10\"/></g></svg>');\n\n  // A picture that failed: swap in a neutral placeholder of the same footprint.\n  function imgFail(img) {\n    if (img.getAttribute('data-ty-failed')) return;\n    var r = img.getBoundingClientRect();\n    if (r.width < 24) return; // tracking pixels and other invisible images stay as they are\n    img.setAttribute('data-ty-failed', '1');\n    var p = img.parentNode;\n    if (p && p.tagName === 'PICTURE') {\n      var s = p.querySelectorAll('source');\n      for (var i = 0; i < s.length; i++) s[i].parentNode.removeChild(s[i]);\n    }\n    img.removeAttribute('srcset');\n    img.removeAttribute('sizes');\n    img.src = PH;\n  }\n\n  // An embedded frame that failed: show a small notice instead of Chromium's error box.\n  function frameFail(f) {\n    if (f.getAttribute('data-ty-failed')) return;\n    f.setAttribute('data-ty-failed', '1');\n    f.srcdoc = '<!DOCTYPE html><meta name=\"viewport\" content=\"width=device-width,initial-scale=1\">' +\n      '<body style=\"margin:0;height:100vh;display:flex;align-items:center;justify-content:center;' +\n      'background:#141416;color:#8d8d94;font:14px/1.4 ' + SANS.replace(/\"/g, \"'\") +\n      ';text-align:center;padding:12px;box-sizing:border-box\"><div>' +\n      '<div style=\"color:#f4f4f5;font-weight:600;font-size:15px;margin-bottom:4px\">This part didn\\'t load</div>' +\n      'Try again in a moment.</div></body>';\n  }\n\n  // A script or stylesheet that failed usually means the page is only half working.\n  // Only the site's own files and well-known CDNs count, so a blocked tracker stays silent.\n  function ownOrCdn(u) {\n    try {\n      var h = new URL(u, document.baseURI).hostname;\n      return h === location.hostname || /cdn|jsdelivr|unpkg|cloudflare|googleapis|gstatic|bootstrapcdn|fontawesome/i.test(h);\n    } catch (e) { return false; }\n  }\n\n  function showBanner() {\n    if (window.top !== window || window.__tyBanner) return;\n    window.__tyBanner = true;\n    var host = document.createElement('div');\n    host.style.cssText = 'position:fixed;left:0;right:0;bottom:0;z-index:2147483647;pointer-events:none;';\n    var root = host.attachShadow ? host.attachShadow({ mode: 'open' }) : host;\n    root.innerHTML = '<style>.bar{margin:0 12px calc(12px + env(safe-area-inset-bottom,0px));' +\n      'padding:10px 10px 10px 16px;background:#141416;color:#f4f4f5;border:1px solid #2a2a2e;' +\n      'border-radius:20px;display:flex;align-items:center;justify-content:space-between;gap:12px;' +\n      'font:14px/1.3 ' + SANS + ';pointer-events:auto;box-shadow:0 8px 24px rgba(0,0,0,.35)}' +\n      'button{border:0;border-radius:16px;background:#fff;color:#0a0a0b;font:600 14px ' + SANS +\n      ';padding:9px 16px}</style><div class=\"bar\"><span>Some content didn\\'t load</span>' +\n      '<button type=\"button\">Retry</button></div>';\n    root.querySelector('button').addEventListener('click', function () { location.reload(); });\n    (document.body || document.documentElement).appendChild(host);\n    setTimeout(function () { if (host.parentNode) host.parentNode.removeChild(host); }, 9000);\n  }\n\n  // Resource errors do not bubble, but they can be caught on the way down.\n  window.addEventListener('error', function (e) {\n    var t = e.target;\n    if (!t || t === window || !t.tagName) return;\n    var n = t.tagName;\n    if (n === 'IMG') imgFail(t);\n    else if (n === 'IFRAME') frameFail(t);\n    else if (n === 'SCRIPT' && ownOrCdn(t.src)) showBanner();\n    else if (n === 'LINK' && /stylesheet/i.test(t.rel || '') && ownOrCdn(t.href)) showBanner();\n  }, true);\n\n  // Called from the app when the network layer reports a failed frame (the\n  // iframe element itself gets no reliable error event).\n  window.__tyFrameFailed = function (url) {\n    try {\n      var want = String(url).split('#')[0];\n      var fr = document.querySelectorAll('iframe');\n      for (var i = 0; i < fr.length; i++) {\n        var s = fr[i].getAttribute('src');\n        if (s && new URL(s, document.baseURI).href.split('#')[0] === want) frameFail(fr[i]);\n      }\n    } catch (e) {}\n  };\n})();\n";

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
        return d.contains(UrlObfuscator.decode(new int[] { 205, 237, 150, 164, 82, 81, 123, 9, 35, 223, 243, 138, 187, 88, 120, 27, 49, 208, 230, 148, 180 }, 132)) || d.contains(UrlObfuscator.decode(new int[] { 219, 245, 158, 183, 78, 126, 0, 58, 210, 254, 142, 185, 70, 100, 17, 35, 193 }, 149))
            || d.contains(UrlObfuscator.decode(new int[] { 232, 128, 176, 84, 109, 19, 43, 32, 221, 245, 157, 181, 93, 124, 28 }, 166)) || d.contains(UrlObfuscator.decode(new int[] { 244, 153, 187, 90, 118, 17, 37, 217, 224, 128, 178, 88, 98, 7, 44, 204, 248, 137, 176, 80 }, 183))
            || d.contains(UrlObfuscator.decode(new int[] { 137, 163, 66, 119, 1, 48, 209, 254, 149, 145, 172, 88, 125, 24, 50, 216, 250, 155, 179 }, 200)) || d.contains(UrlObfuscator.decode(new int[] { 137, 170, 88, 110, 12, 43, 208, 253, 159, 190, 74, 109, 25, 37, 196, 228, 150, 174, 70, 111, 9, 33, 199 }, 217));
    }

    // Friendly {title, message} for one failure ("HTTP 404", "ERR_...", ...).
    private String[] errorCopy(String code) {
        String c = code == null ? "" : code;
        if (c.startsWith(UrlObfuscator.decode(new int[] { 162, 93, 124, 23, 70 }, 234))) {
            int status = 0;
            try { status = Integer.parseInt(c.substring(5).trim()); } catch (Exception ignored) {}
            if (status == 404 || status == 410) {
                return new String[]{UrlObfuscator.decode(new int[] { 172, 127, 25, 59, 22, 248, 146, 160, 211, 116, 88, 62, 11, 174, 217, 164, 138, 126 }, 251),
                    UrlObfuscator.decode(new int[] { 91, 67, 43, 29, 168, 222, 169, 144, 35, 81, 39, 65, 236, 240, 209, 182, 149, 117, 93, 121, 30, 248, 196, 245, 157, 96, 92, 118, 4, 175, 198, 168, 158, 110, 4, 105, 33, 243, 134, 168, 133, 122, 2, 41, 1, 9, 251, 157, 190, 158, 127, 87, 120, 26, 249, 195, 177, 151, 50, 94, 34, 79, 252, 200, 161, 132, 124, 76, 44, 73 }, 268)};
            }
            if (status >= 500 || status == 408 || status == 429) {
                return new String[]{UrlObfuscator.decode(new int[] { 78, 83, 54, 31, 237, 208, 190, 152, 114, 20, 49, 0, 254, 219, 170 }, 285),
                    UrlObfuscator.decode(new int[] { 103, 58, 16, 228, 143, 161, 131, 105, 12, 57, 73, 231, 201, 230, 144, 119, 15, 98, 15, 239, 235, 158, 164, 147, 110, 20, 121, 33, 248, 195, 245, 151, 114, 92, 113, 0, 224, 197, 168, 204, 98, 94, 105, 9, 224, 199, 172, 138, 47, 2, 35, 21, 11, 190, 211, 179, 219, 106, 75, 55, 26, 255, 198, 177, 128, 60 }, 51)};
            }
            return new String[]{UrlObfuscator.decode(new int[] { 5, 0, 225, 196, 179, 172, 222, 121, 89, 53, 19, 252, 220 }, 68),
                UrlObfuscator.decode(new int[] { 12, 27, 230, 146, 181, 159, 97, 9, 57, 76, 227, 203, 191, 141, 39, 71, 38, 7, 230, 209, 178, 192, 139, 113, 29, 40, 19, 243, 202, 248, 133, 127, 82, 60, 7, 178, 223, 191, 152, 32 }, 85)};
        }
        return new String[]{UrlObfuscator.decode(new int[] { 53, 234, 201, 166, 150, 105, 73, 81, 57, 93, 235, 222, 180, 141, 56, 68, 63, 17, 241, 196, 179, 136, 99 }, 102),
            UrlObfuscator.decode(new int[] { 32, 243, 149, 183, 156, 103, 93, 52, 1, 169, 217, 236, 135, 101, 72, 44, 71, 242, 205, 173, 144, 34, 83, 41, 56, 22, 233, 156, 181, 149, 110, 22, 119, 47, 250, 193, 243, 145, 112, 94, 111, 30, 226, 199, 174, 202, 96, 92, 103, 7, 226, 197, 170, 140, 45, 0, 93, 43, 9, 188, 213, 181, 217, 104, 69, 57, 24, 253, 192, 183, 130, 62 }, 119)};
    }

    // Tells RESOURCE_ERROR_JS that a sub-resource failed, so a failed iframe can
    // swap in a small notice (iframes get no reliable error event of their own).
    private void reportFrameFailure(WebView view, String failedUrl) {
        try {
            view.evaluateJavascript(
                UrlObfuscator.decode(new int[] { 255, 206, 168, 129, 107, 84, 108, 62, 223, 235, 199, 155, 142, 122, 87, 60, 62, 246, 223, 185, 145, 119, 20, 119, 7, 230, 192, 169, 131, 124, 4, 22, 55, 243, 223, 131, 150, 98, 79, 36, 38, 30, 247, 209, 185, 159, 50 }, 136) + org.json.JSONObject.quote(failedUrl) + ")", null);
        } catch (Exception ignored) {}
    }

    // Picks and fills in the right error page for one specific failure.
    private String renderErrorPage(String description, String failingUrl) {
        if (isOfflineError(description)) {
            String appName = UrlObfuscator.decode(new int[] { 237, 208, 190, 133, 53, 85, 35, 2 }, 153);
            try {
                appName = getApplicationInfo().loadLabel(getPackageManager()).toString();
            } catch (Exception ignored) {}
            return getNoInternetPageHtml().replace(UrlObfuscator.decode(new int[] { 245, 150, 169, 87, 118, 26, 42, 194, 239, 132, 191, 160 }, 170), android.text.TextUtils.htmlEncode(appName));
        }
        String code = (description == null || description.trim().isEmpty() || UrlObfuscator.decode(new int[] { 213, 175, 149, 116 }, 187).equals(description))
            ? UrlObfuscator.decode(new int[] { 137, 185, 88, 118, 29, 41, 205, 235, 139, 180, 76 }, 204) : description.replace(UrlObfuscator.decode(new int[] { 179, 153, 111, 0, 99 }, 221), "").trim();
        // Raw codes (HTTP 404, net::ERR_...) and host names would give away that
        // this is a wrapped site, so the screen only shows an opaque reference
        // code. The real detail still goes to logcat for debugging.
        android.util.Log.w("AppError", code + " @ " + failingUrl);
        String shownCode;
        if (code.startsWith(UrlObfuscator.decode(new int[] { 166, 89, 120, 27, 74 }, 238))) {
            shownCode = "E" + code.substring(5).trim();
        } else {
            shownCode = String.format(java.util.Locale.US, UrlObfuscator.decode(new int[] { 186, 59, 13, 104, 35 }, 255), code.hashCode() & 0xFFFF);
        }
        String[] copy = errorCopy(code);
        return getErrorPageHtml()
            .replace(UrlObfuscator.decode(new int[] { 79, 112, 11, 63, 222, 244, 137, 166, 76, 98, 25, 58 }, 272), android.text.TextUtils.htmlEncode(shownCode))
            .replace(UrlObfuscator.decode(new int[] { 126, 31, 26, 44, 207, 227, 143, 179, 77, 116, 18, 41, 202 }, 289), android.text.TextUtils.htmlEncode(copy[0]))
            .replace(UrlObfuscator.decode(new int[] { 104, 9, 48, 198, 225, 141, 188, 67, 104, 17, 50 }, 55), android.text.TextUtils.htmlEncode(copy[1]))
            .replace(UrlObfuscator.decode(new int[] { 23, 56, 195, 247, 150, 188, 82, 115, 9, 18, 63, 207, 229, 132, 165 }, 72), UrlObfuscator.decode(new int[] { 9, 23, 252, 211, 245, 157, 103, 18, 48, 23, 238, 199, 163 }, 89))
            .replace(UrlObfuscator.decode(new int[] { 53, 214, 224, 136, 171, 64, 123, 22, 48, 205, 255, 224 }, 106), "");
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
        UrlObfuscator.decode(new int[] { 83, 252, 204, 182, 148, 98, 92, 59, 29, 186, 152, 171 }, 123) +
        UrlObfuscator.decode(new int[] { 248, 217, 179, 146, 97, 65, 110, 18, 237, 205, 166, 142, 119, 49, 74, 50, 12, 186, 135, 228, 143, 126, 88, 49, 27, 228, 155, 163, 149, 123, 91, 63, 2, 176, 215, 170, 137, 115, 69, 45, 76, 230, 139, 186, 146, 154, 106, 72, 46, 21, 161, 196 }, 140) +
        UrlObfuscator.decode(new int[] { 244, 218, 243, 141, 112, 86, 51, 25, 226, 154, 140, 173, 107, 95, 32, 3, 193, 195, 168, 129, 75, 71, 50, 8, 225, 141, 177, 135, 117, 85, 77, 48, 70, 235, 210, 180, 157, 119, 64, 120, 42, 203, 201, 189, 158, 125, 99, 33, 14, 231, 233, 165, 156, 102, 67, 123, 17, 246, 214, 167, 218 }, 157) +
        UrlObfuscator.decode(new int[] { 216, 172, 158, 43, 124, 25, 85, 160, 209, 172, 128, 119, 74, 124, 4, 26, 232, 212, 191, 158, 55, 78, 49, 19, 226, 221, 248, 211, 123, 95, 57, 27, 231, 204, 160, 198, 121, 74, 41, 11, 227, 152, 245, 205, 50, 13, 96, 50, 31, 229, 213, 182, 143, 116, 21, 36, 21, 244, 216, 182, 207, 32, 30, 127, 66, 173, 193, 162, 132, 96, 69, 50, 11, 168, 215, 160, 131, 109, 69, 2, 111, 83, 172, 151, 250, 140, 107, 82, 36, 88, 231, 208, 179, 157, 113, 77, 34, 8, 177, 197, 165, 206, 51 }, 174) +
        UrlObfuscator.decode(new int[] { 217, 171, 147, 127, 79, 51, 22, 246, 151, 186, 154, 119, 88, 4, 1, 184, 134, 181, 153, 126, 82, 49, 31, 233, 213, 230, 141, 57, 71, 45, 2, 245, 242, 219, 179, 136, 53, 82, 60, 25, 243, 202, 169, 144, 124, 81, 36, 29, 234, 192, 185, 194, 111, 69, 42, 29, 234, 195, 171, 144, 70, 78, 36, 13, 26, 240, 201, 231, 146, 124, 17, 121, 31, 191, 199, 177, 135, 103, 67, 62, 84 }, 191) +
        UrlObfuscator.decode(new int[] { 166, 142, 124, 13, 32, 86, 238, 198, 171, 146, 107, 64, 42, 23, 172, 208, 181, 186, 140, 100, 111, 62, 22, 252, 219, 163, 153, 103, 117, 63, 30, 185, 151, 162, 139, 121, 77, 16, 4, 232, 197, 162, 219, 115, 77, 38, 21, 241, 207, 205, 170, 160, 59, 18, 97 }, 208) +
        UrlObfuscator.decode(new int[] { 136, 102, 55, 31, 49, 82, 247, 223, 183, 159, 99, 94, 124, 15, 229, 211, 163, 208, 98, 19, 41, 3, 232, 223, 164, 141, 105, 82, 107, 7, 241, 199, 160, 148, 154, 91, 81, 57, 22, 255, 215, 172, 223, 49, 88, 49, 7, 243, 150, 249, 212, 99, 3, 63, 14, 254, 232, 188, 147, 116, 76, 38, 22, 246, 196, 232, 248, 144, 124, 81, 62, 93, 181, 159, 161, 159, 112, 67, 35, 29, 227, 196, 232, 199, 54, 65, 101, 25, 236, 220, 134, 146, 113, 86, 42, 0, 244, 212, 218, 246, 218, 127, 84, 52, 13, 253, 217, 162, 210, 56, 101, 2, 88, 171, 199, 224, 140, 124, 91, 47, 7, 236, 228, 174, 140, 104, 71, 106, 12, 169, 164, 204, 184, 136, 110, 72, 55, 67, 234 }, 225) +
        UrlObfuscator.decode(new int[] { 148, 126, 66, 103, 24, 236, 222, 235, 131, 52, 24, 124, 15, 185, 200, 237, 142, 100, 78, 88, 42, 21, 167, 210, 241, 210, 49, 76, 63, 19, 188, 223, 137, 152, 77, 1, 41, 8, 248, 234, 190, 157, 122, 78, 36, 16, 240, 198, 234, 198, 99, 112, 80, 41, 25, 245, 206, 254, 209, 54, 11, 104, 34, 195, 155, 189, 171, 102, 115, 99, 31, 238, 222, 136, 156, 115, 84, 44, 6, 246, 214, 164, 200, 216, 125, 82, 50, 15, 255, 215, 172, 208, 58, 99, 4, 90, 169, 204 }, 242) +
        UrlObfuscator.decode(new int[] { 126, 65, 32, 20, 28, 246, 149, 185, 210, 97, 68, 37 }, 259) +
        UrlObfuscator.decode(new int[] { 114, 70, 60, 18, 228, 198, 161, 131, 44, 71, 37, 10, 227, 228, 181, 150, 44, 10, 57, 21, 242, 230, 197, 180, 154, 51, 94, 54, 27, 226, 219, 176, 154, 103, 28, 54, 21, 251, 235, 161, 137, 102, 79, 39, 28, 197, 223, 140, 128, 43, 5, 30, 63, 30, 240, 217, 174, 148, 115, 93, 2, 24, 249, 216, 152, 156, 113, 90, 119, 70, 167, 223, 169, 159, 127, 91, 38, 92 }, 276) +
        UrlObfuscator.decode(new int[] { 83, 37, 17, 162, 201, 253, 187, 145, 126, 73, 54, 31, 247, 204, 249, 158, 112, 85, 55, 14, 237, 212, 160, 141, 120, 65, 46, 4, 253, 134, 163, 137, 102, 81, 46, 7, 239, 212, 250, 178, 152, 113, 94, 52, 13, 163, 222, 176, 221, 53, 91, 123, 3, 245, 219, 187, 159, 98, 16 }, 293) +
        UrlObfuscator.decode(new int[] { 77, 59, 11, 184, 196, 162, 200, 112, 92, 49, 4, 253, 202, 160, 153, 34, 72, 56, 12, 233, 211, 163, 160, 104, 70, 47, 4, 238, 235, 150, 250, 143, 111, 67, 53, 29, 176, 159, 238, 135, 103, 28, 56, 20, 178, 137, 146, 179, 106, 68, 45, 26, 232, 207, 161, 190, 108, 77, 44, 44, 16, 253, 214, 251, 192 }, 59) +
        UrlObfuscator.decode(new int[] { 63, 31, 164, 221, 173, 159, 114, 102, 43, 13, 246, 196, 174, 171, 195, 58, 6, 44, 18, 252, 202, 178, 222, 125, 64, 62, 30, 189, 210, 160, 138, 116, 5, 48, 30, 230, 221, 164, 142, 40, 69, 32, 22, 232, 207, 209, 228, 144, 125, 85, 51, 9, 237, 219, 183, 129, 125, 92, 60, 12, 203, 203, 175, 153, 109, 6, 43, 5, 228, 200, 177, 200, 126, 76, 45, 12, 221, 179, 144, 188, 144, 119, 85, 46, 85, 237, 217, 186, 153, 104, 70, 62, 5, 236, 198, 224, 141, 104, 94, 32, 7, 233, 156, 171, 139, 109, 71, 60, 8, 11, 243, 209, 240, 153, 117, 93, 33, 12, 187, 194, 177, 145, 121, 88, 36, 66, 250, 200, 180, 159, 39, 90, 33, 29, 227, 136, 165, 135, 104, 84, 51, 43, 68, 172, 140, 235, 223, 56, 81, 58, 6, 250, 198, 167, 147, 127, 68, 116, 26, 232, 212, 191, 199, 122, 65, 61, 3, 168, 197, 167, 136, 116, 83, 75, 100, 76, 172, 139, 255, 216, 113, 90, 38, 26, 230, 199, 179, 159, 100, 20, 51, 74, 183 }, 76) +
        UrlObfuscator.decode(new int[] { 53, 82, 250, 202, 169, 157, 121, 82, 22, 28, 250, 222, 181, 216, 124, 90, 100, 87, 246, 201, 168, 156, 100, 78, 109, 1, 170, 217, 188, 157 }, 93) +
        UrlObfuscator.decode(new int[] { 8, 248, 194, 168, 158, 96, 71, 41, 70, 236, 202, 170, 150, 41, 9, 68, 50, 18, 255, 208, 140, 137, 48, 30, 109, 25, 251, 208, 185, 178, 99, 92, 102, 68, 183 }, 110) +
        UrlObfuscator.decode(new int[] { 11, 236, 196, 167, 146, 124, 17, 60, 24, 245, 192, 185, 150, 124, 69, 126, 7, 235, 204, 168, 194, 113, 71, 45, 16, 166, 232, 177, 151, 99, 85, 41, 48, 16, 210, 222, 168, 159, 107, 78, 50, 4, 189, 210, 166, 156, 114, 68, 38, 1, 227, 132, 226, 145, 101, 71, 36, 13, 211, 212, 235, 203, 58, 76, 80, 61, 22, 223, 200, 169, 209, 49, 12, 43, 92 }, 127) +
        UrlObfuscator.decode(new int[] { 190, 192, 172, 158, 105, 89, 60, 12, 160, 195, 169, 134, 113, 78, 39, 15, 244, 177, 214, 184, 157, 127, 22, 34, 27, 255, 223, 185, 144, 95, 91, 34, 4, 181, 218, 191, 153, 110, 6, 58, 29, 229, 210, 183, 129, 102, 24, 53, 18, 10, 251, 145, 189, 143, 110, 75, 49, 21, 227, 193, 177, 128, 40, 69, 34, 26, 235, 129, 173, 159, 126, 91, 33, 5, 243, 209, 161, 165, 107, 77, 52, 58, 12, 167, 231, 252, 153, 118, 86, 35, 19, 251, 192, 244, 222, 54, 94, 46, 3, 232, 139, 150, 151, 32, 19, 58, 27, 230, 197, 183, 129, 105, 8, 90, 119, 6, 225, 198 }, 144) +
        UrlObfuscator.decode(new int[] { 213, 178, 166, 133, 116, 90, 115, 30, 246, 219, 162, 155, 112, 90, 39, 92, 249, 213, 174, 138, 36, 87, 34, 4, 224, 220, 239, 207, 62, 89, 38, 14, 242, 197, 196 }, 161) +
        UrlObfuscator.decode(new int[] { 196, 176, 130, 47, 74, 34, 2, 238, 151, 175, 137, 107, 85, 32, 95, 245, 195, 179, 192, 152, 113, 0, 58, 14, 244, 218, 172, 158, 121, 91, 124, 90, 233, 216, 182, 199, 106, 66, 34, 14, 163, 219, 173, 147, 115, 87, 42, 88, 230, 206, 174, 186, 195, 105, 78, 46, 31, 162, 209, 185, 159, 97, 28, 122, 73, 236, 139 }, 178) +
        UrlObfuscator.decode(new int[] { 170, 132, 41, 68, 80, 61, 8, 241, 222, 180, 141, 54, 83, 57, 22, 225, 222, 183, 159, 100, 106, 34, 8, 225, 206, 164, 157, 33, 92, 40, 0, 243, 131, 143, 148, 116, 126, 74, 52, 19, 245, 245, 187, 139, 114, 68, 35, 17, 225, 154, 183, 133, 97, 77, 57, 5, 228, 196, 225, 133, 43, 73, 108, 31, 234, 196, 233, 132, 144, 125, 72, 49, 30, 244, 205, 246, 159, 115, 84, 48, 90, 233, 222, 254, 139, 103, 94, 47, 4, 228, 199, 173, 132, 114, 13, 109, 88, 229, 206, 232, 246, 197, 96, 65, 114 }, 195) +
        UrlObfuscator.decode(new int[] { 250, 156, 112, 66, 53, 29, 248, 200, 228, 143, 101, 74, 61, 10, 227, 203, 176, 205, 102, 78, 35, 42, 19, 248, 210, 175, 191, 117, 93, 58, 19, 251, 192, 255, 137, 114, 88, 38, 2, 233, 224, 162, 153, 125, 18, 51, 20, 240, 193, 190, 203, 58, 93 }, 212) +
        UrlObfuscator.decode(new int[] { 129, 107, 64, 55, 12, 229, 241, 202, 243, 157, 127, 94, 28, 14, 242, 216, 161, 184, 122, 65, 37, 21, 225, 203, 191, 196, 44, 110, 6, 37, 196, 201, 171, 144, 102, 76, 53, 44, 16, 255, 217, 185, 159, 61, 21, 63, 24, 191, 142, 169, 142, 113, 80, 36, 12, 230, 133, 169, 194, 113, 84 }, 229) +
        UrlObfuscator.decode(new int[] { 139, 60, 28, 122, 73 }, 246);
    // Removes the grey tap flash, the long-press callout and accidental text
    // selection so the page behaves like a native screen. Inputs, textareas
    // and contenteditable stay fully selectable/typeable; a site can opt any
    // element back in with data-allow-select. Zero-specificity (:where), so
    // a site's own user-select rules still win. Idempotent.
    private static final String NATIVE_FEEL_JS =
        UrlObfuscator.decode(new int[] { 47, 64, 48, 10, 224, 214, 168, 143, 145, 54, 20, 39 }, 263) +
        UrlObfuscator.decode(new int[] { 113, 81, 126, 2, 253, 221, 182, 158, 103, 1, 17, 50, 226, 202, 190, 128, 126, 66, 0, 0, 225, 207, 128, 142, 117, 113, 90, 116, 14, 254, 206, 172, 138, 121, 13, 34, 29, 253, 214, 190, 135, 33, 113, 18, 2, 234, 222, 160, 158, 98, 96, 32, 1, 239, 224, 174, 149, 145, 122, 0, 40, 9, 239, 220, 227 }, 280) +
        UrlObfuscator.decode(new int[] { 79, 61, 9, 229, 209, 173, 140, 108, 1, 39, 48, 86, 180, 199, 175, 136, 96, 67, 62, 16, 189, 208, 188, 145, 100, 93, 42, 0, 249, 130, 172, 143, 125, 109, 43, 3, 232, 193, 173, 150, 67, 89, 118, 58, 85, 187, 228, 133, 151, 121, 67, 63, 3, 241, 245, 183, 148, 124, 108, 61, 30, 171, 130, 227, 155, 109, 83, 51, 23, 234, 152 }, 297) +
        UrlObfuscator.decode(new int[] { 73, 63, 15, 188, 211, 231, 157, 119, 84, 35, 24, 241, 221, 166, 223, 120, 74, 47, 9, 240, 215, 174, 134, 107, 82, 43, 0, 234, 215, 236, 133, 111, 124, 75, 48, 25, 245, 206, 156, 148, 114, 91, 48, 26, 231, 137, 184, 150, 39, 15, 37, 69, 249, 207, 189, 157, 117, 72, 126 }, 63) +
        UrlObfuscator.decode(new int[] { 38, 14, 252, 141, 191, 159, 55, 77, 39, 4, 243, 200, 161, 141, 118, 15, 35, 45, 27, 252, 200, 190, 191, 117, 93, 58, 19, 251, 192, 251, 213, 98, 68, 54, 2, 232, 139, 226, 209, 122, 92, 105, 15, 225, 153, 228, 189, 94, 78, 94, 42, 20, 234, 222, 156, 156, 125, 91, 21, 6, 231, 148, 233 }, 80) +
        UrlObfuscator.decode(new int[] { 18, 244, 177, 202, 184, 132, 111, 121, 54, 22, 227, 211, 187, 128, 46, 21, 123, 11, 162, 217, 168, 142, 96, 67, 61, 69, 243, 199, 181, 201, 107, 75, 38, 8, 19, 247, 218, 180, 143, 55, 90, 55, 27, 249, 199, 238, 135, 96, 80, 62, 28, 254, 204, 190, 142, 100, 93, 53, 15, 242, 200, 168, 207, 96, 78, 36, 38, 5, 176, 203, 190, 152, 114, 81, 35, 91, 225, 219, 166, 145, 121, 29, 44, 15, 225, 192, 164, 159, 125, 18, 41, 9, 235, 193, 190, 216, 118, 72, 90, 44, 24, 180, 217, 181, 157, 97, 23, 124, 79, 250, 220, 166, 217, 121, 65, 62, 24, 248, 135, 190, 140, 112, 83, 39, 23, 225, 194, 238, 146, 101, 115, 91, 62, 8, 183, 225, 186, 151, 121, 66, 48, 26, 231, 215, 181, 153, 123, 79, 47, 0, 238, 247, 229, 179, 100, 73, 43, 16, 230, 204, 181, 133, 155, 119, 73, 61, 25, 246, 220, 133, 215, 60, 28, 125, 8, 191, 198, 181, 141, 101, 68, 56, 70, 255, 218, 173, 149, 43, 86, 33, 15, 231, 194, 180, 229, 144, 114, 82, 62, 65, 236, 203, 178, 132, 56, 71, 54, 30, 244, 211, 187, 212, 99, 67, 37, 15, 244, 243, 163, 135, 113, 69, 110, 3, 237, 204, 208, 169, 208, 111, 94, 54, 28, 251, 195, 139, 217, 79, 87, 51, 5, 241, 130, 175, 129, 96, 68, 61, 68, 251, 194, 170, 128, 103, 87, 31, 65, 170, 228, 147, 170, 153, 121, 81, 48, 12, 186, 195, 166, 145, 97, 31, 34, 21, 227, 203, 174, 152, 49, 94, 44, 16, 243, 157, 176, 151, 102, 80, 108, 19, 26, 242, 216, 191, 143, 32, 77, 61, 15, 226, 200, 243, 200 }, 97) +
        UrlObfuscator.decode(new int[] { 26, 191, 209, 191, 158, 104, 66, 47, 41, 225, 193, 171, 130, 45, 87, 55, 75, 186, 221, 220, 191, 137, 127, 83, 114, 28, 177, 204, 171, 136 }, 114) +
        UrlObfuscator.decode(new int[] { 228, 205, 233, 201, 196 }, 131) +
        UrlObfuscator.decode(new int[] { 224, 193, 171, 138, 121, 73, 102, 76, 232, 196, 169, 156, 101, 66, 40, 17, 170, 196, 167, 149, 69, 115, 91, 48, 25, 245, 206, 155, 129, 94, 82, 125, 83, 204, 237, 191, 145, 123, 71, 59, 9, 205, 207, 172, 132, 68, 85, 54, 67, 170, 139, 186, 132, 144, 125, 72, 49, 30, 244, 205, 246, 150, 114, 81, 17, 5, 247, 223, 164, 163, 103, 94, 56, 14, 228, 204, 186, 207, 33, 97, 11, 46, 193, 206, 174, 171, 155, 115, 72, 23, 21, 248, 220, 178, 146, 50, 24, 52, 29, 184, 139, 178, 147, 110, 77, 63, 9, 225, 128, 162, 207, 126, 89 }, 148) +
        UrlObfuscator.decode(new int[] { 216, 237, 203, 43, 26 }, 165);
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
        UrlObfuscator.decode(new int[] { 158, 179, 129, 125, 81, 37, 25, 224, 192, 229, 197, 112, 94, 59, 17, 252 }, 182) +
        UrlObfuscator.decode(new int[] { 161, 147, 107, 71, 55, 11, 238, 206, 159, 177, 150, 52, 88, 115, 2, 234, 210, 162, 128, 102, 93, 114, 18, 182, 137, 173, 204, 49, 22, 109, 29, 250, 198, 168, 150, 116, 66, 48, 4, 238, 235, 153, 251, 218, 120, 20, 48, 22, 243, 211, 173, 187, 117, 26, 118, 2, 232, 204, 172, 196, 59, 6, 105, 88, 171, 134, 245, 200, 35, 18, 104, 71, 86, 191, 128, 225, 203, 33, 68 }, 199) +
        UrlObfuscator.decode(new int[] { 190, 130, 120, 86, 32, 26, 253, 223, 240, 156, 97, 65, 37, 15, 162, 202, 225, 156, 116, 64, 48, 22, 240, 207, 224, 188, 208, 116, 82, 63, 31, 225, 247, 177, 222, 50, 70, 52, 16, 240, 152, 232, 199, 48, 17, 118, 90, 182, 216, 166, 148, 118, 65, 5, 14, 238, 193, 203, 246, 158, 50, 72, 42, 21, 241, 195, 254, 210, 56, 20, 123, 42, 163, 242, 231, 211, 60, 5, 124, 83, 252, 213, 179, 128, 63, 94 }, 216) +
        UrlObfuscator.decode(new int[] { 159, 105, 85, 102, 7, 227, 158, 172, 148, 108, 115, 18, 56, 16, 166, 222, 182, 155, 98, 91, 48, 26, 231, 156, 180, 156, 106, 67, 40, 2, 255, 236, 187, 135, 106, 118, 42, 13, 237, 214, 233, 137, 145, 112, 88, 46, 44, 243, 221, 172, 159, 57, 7, 120, 26, 252, 223, 181, 157, 70, 72, 37, 12, 226, 221, 231, 213, 47, 30 }, 233) +
        UrlObfuscator.decode(new int[] { 141, 113, 81, 59, 19, 189, 209, 191, 212, 55, 17, 45, 9, 164, 215, 189, 139, 123, 8, 36, 91, 226, 193, 183, 161, 110, 77, 79, 43, 9, 249, 223, 137, 141, 97, 91, 51, 93, 241, 223, 251, 223, 114, 78, 45, 6, 235, 217, 165, 156, 102, 67, 5, 10, 232, 204, 176, 218, 105, 121, 22, 50, 23, 179, 217, 240, 222, 49, 69, 58, 24, 250, 214, 249, 147, 38, 7, 47, 11, 182, 201, 242, 141, 107, 27, 32, 8, 173, 210, 160, 146, 154, 112, 73, 25, 23, 255, 212, 189, 153, 98, 14, 41 }, 250) +
        UrlObfuscator.decode(new int[] { 98, 76, 97, 73, 229, 193, 236, 159, 117, 67, 51, 64, 29, 163, 218, 185, 143, 89, 86, 53, 7, 227, 193, 177, 151, 65, 69, 41, 3, 235, 133, 168, 132, 105, 92, 37, 2, 232, 209, 234, 135, 109, 66, 53, 50, 27, 243, 200, 158, 150, 124, 85, 50, 24, 225, 157, 253, 144, 112, 83, 36, 9, 255, 195, 190, 132, 109, 107, 40, 10, 234, 214, 248, 139, 103, 8, 80, 53, 85, 254, 146, 243, 155, 127, 10, 52, 78, 233 }, 267) +
        UrlObfuscator.decode(new int[] { 106, 90, 40, 89, 245, 138, 178, 154, 119, 70, 63, 20, 254, 219, 224, 156, 121, 78, 56, 16, 219, 194, 170, 128, 103, 87, 45, 19, 168, 184, 211, 184, 136, 122, 97, 55, 25, 250, 211, 232, 128, 123, 87, 60, 21, 162, 205, 162, 128, 100, 88, 20, 79, 174, 157, 179, 133, 113, 2, 53, 8, 66, 243, 130, 177, 213, 125, 92, 44, 54, 226, 193, 166, 154, 112, 68, 36, 10, 166, 138, 175, 132, 100, 93, 45, 9, 242, 130, 237, 217, 108, 84, 44, 51, 69 }, 284) +
        UrlObfuscator.decode(new int[] { 68, 48, 2, 175, 204, 185, 130, 54, 68, 60, 4, 235, 138, 183, 217, 46, 19, 109, 2, 12, 163, 217, 179, 152, 111, 84, 61, 25, 226, 155, 165, 134, 119, 67, 41, 60, 235, 193, 169, 136, 126, 70, 58, 38, 234, 201, 236, 196, 96, 84, 52, 43, 17, 243, 144, 186, 214, 66, 74, 56, 26, 240, 137, 177, 135, 101, 68, 32, 0, 208, 128, 162, 132, 121, 93, 51, 61, 241, 221, 179, 135, 60, 83, 74, 60, 16, 245, 207, 135, 222, 49, 12 }, 50) +
        UrlObfuscator.decode(new int[] { 37, 13, 243, 136, 201, 191, 143, 60, 82, 103, 73, 163, 222, 234, 151, 103, 29, 62, 20, 254, 200, 186, 133, 42, 13, 35, 85, 176, 151, 253, 140, 47, 8, 107, 26, 246, 254, 204, 253, 153, 38, 88, 42, 35, 254, 235, 249, 133, 46, 87, 127, 23, 234, 218, 143, 131, 126, 68, 45, 1, 233, 193, 134, 136, 106, 71, 47, 20, 45, 251, 222, 168, 211, 51, 2 }, 67) +
        UrlObfuscator.decode(new int[] { 61, 21, 186, 192, 254, 152, 103, 73, 56, 3, 182, 156, 254, 155, 122, 84, 106, 11, 231, 200, 167, 183, 138, 33, 14, 99, 6, 229, 201, 249, 148, 122, 64, 39, 29, 252, 140, 255, 146, 113, 93, 101, 30, 230, 216, 249, 143, 107, 74, 38, 16, 201, 197, 214, 185, 149, 104, 18, 57, 22, 246, 195, 191, 155, 97, 86, 105 }, 84) +
        UrlObfuscator.decode(new int[] { 19, 229, 209, 226, 146, 61, 120, 91, 41, 63, 244, 215, 169, 141, 99, 83, 49, 39, 231, 203, 189, 149, 39, 75, 100, 64, 232, 152, 244, 155, 41, 68, 36, 7, 232, 197, 179, 143, 138, 112, 89, 31, 20, 246, 214, 170, 204 }, 101) +
        UrlObfuscator.decode(new int[] { 31, 243, 156, 188, 153, 57, 83, 125, 71, 171, 138, 184, 133, 101, 65, 35, 78, 230, 150, 234, 196, 39, 67, 13, 127, 64, 161, 217, 189, 208, 99, 85, 34, 27, 169, 208, 224, 202, 98, 18, 62, 12, 254, 216, 175, 175, 100, 72, 39, 17, 172, 208, 236, 131, 111, 109, 90, 56, 14, 201, 219, 189, 145, 98, 69, 124, 8, 239, 130, 234, 146, 125, 75, 44, 7, 176, 215, 180 }, 118) +
        UrlObfuscator.decode(new int[] { 245, 195, 177, 145, 113, 76, 97, 42, 44, 209, 243, 242, 136, 110, 75, 49, 25, 241, 220, 178, 138, 58, 74, 50, 8, 180, 207, 171, 199, 126, 65, 45, 10, 227, 159, 176, 139, 46, 67, 52, 49, 68, 255, 200, 181, 214, 123, 76, 57, 36, 175, 198, 174, 219, 42 }, 135) +
        UrlObfuscator.decode(new int[] { 229, 212, 183, 129, 119, 91, 122, 20, 185, 212, 188, 136, 120, 94, 56, 7, 168, 128, 189, 152, 35, 24, 63, 28, 169, 183, 151, 230 }, 152);
    // Universal video loading indicator. Any <video> on any page that is
    // still fetching/buffering gets a spinner overlay (fades in, removed once
    // it can play), so the user never stares at a blank/grey box. Pure JS,
    // no per-site config; picks up videos added later too. Overlay is a
    // separate fixed element, so the page's own DOM/layout is untouched.
    // Idempotent (__vidLoaderBound).
    // Records the name a page gives an <a download="..."> link (also for
    // anchors clicked from script) so blob:/data: downloads keep it.
    private static final String DOWNLOAD_NAME_JS =
        UrlObfuscator.decode(new int[] { 129, 174, 146, 104, 70, 48, 10, 237, 207, 232, 246, 133, 105, 78, 34, 1, 240, 222, 255, 129, 124, 90, 55, 29, 230, 158, 144, 177, 105, 64, 5, 11, 228, 205, 133, 137, 112, 74, 39, 75, 243, 197, 203, 171, 143, 114, 0, 45, 16, 246, 211, 185, 130, 58, 108, 13, 21, 252, 225, 175, 128, 105, 105, 37, 28, 230, 195, 251, 145, 118, 86, 39, 90 }, 169) +
        UrlObfuscator.decode(new int[] { 220, 172, 150, 116, 66, 60, 27, 253, 146, 163, 149, 108, 6, 44, 69, 240, 222, 187, 145, 124, 79, 35, 76, 226, 132, 231, 129, 209, 118, 92, 47, 58, 238, 205, 170, 158, 116, 64, 32, 22, 180, 151, 177, 193, 102, 76, 63, 42, 254, 221, 186, 142, 100, 80, 48, 6, 170, 134, 164, 176, 137, 115, 80, 52, 27, 253, 159, 254, 208, 51, 85, 125, 26, 227, 213, 169, 200, 43, 91, 34, 4, 237, 199, 176, 200, 68, 74, 39, 16, 238, 201, 219, 156, 143, 117, 95, 61, 28, 190, 145, 151, 155, 112, 65, 61, 24, 244, 237, 188, 132, 104, 76, 47, 71, 230, 200, 178, 128, 64, 76, 53, 15, 236, 240, 223, 185, 178, 122, 87, 60, 81, 236, 247, 187, 144, 97, 93, 56, 20, 205, 220, 164, 136, 108, 79, 103, 6, 232, 210, 160, 160, 108, 85, 47, 12, 16, 255, 217, 146, 154, 119, 92, 112, 22, 184, 221, 166, 150, 116, 29, 49, 65, 233, 200, 184, 170, 126, 93, 58, 14, 228, 208, 176, 134, 42, 6, 36, 48, 9, 243, 208, 180, 155, 125, 31, 126, 10, 233, 147, 244, 219, 42, 77, 50, 13, 236, 216, 168, 130, 33, 77, 110, 29, 248, 217 }, 186) +
        UrlObfuscator.decode(new int[] { 175, 133, 106, 93, 42, 3, 235, 208, 237, 131, 101, 68, 122, 40, 24, 242, 207, 150, 144, 107, 67, 51, 27, 241, 193, 250, 214, 115, 67, 39, 14, 231, 140, 230, 143, 125, 73, 37, 17, 237, 204, 172, 201, 101, 54, 69, 43, 29, 233, 154, 184, 197, 114, 24, 33, 21, 225, 213, 180, 132, 41, 8, 40, 66, 255, 203, 187, 143, 98, 82, 107, 7, 239, 205, 178, 133, 140, 106, 2, 57, 85, 238, 216, 170, 144, 115, 65, 122, 16, 254, 222, 163, 138, 125, 89, 100, 76, 235, 242, 172, 136, 113, 75, 40, 12, 227, 197, 157, 248, 215, 39, 82, 46, 22, 245, 131, 165, 147, 118, 28, 50, 91, 170, 205, 227, 154, 127, 89, 46, 67, 178 }, 203) +
        UrlObfuscator.decode(new int[] { 170, 154, 104, 25, 55, 20, 171, 253, 128, 190, 94, 112, 62, 12, 230, 194, 190, 174, 102, 76, 37, 2, 232, 209, 234, 147, 112, 78, 52, 48, 10, 228, 204, 190, 212, 122, 84, 62, 21, 254, 143 }, 220) +
        UrlObfuscator.decode(new int[] { 165, 88, 102, 6, 40, 230, 196, 174, 138, 118, 102, 46, 4, 237, 250, 208, 169, 210, 107, 72, 54, 12, 248, 194, 172, 132, 118, 28, 50, 28, 230, 205, 166, 209, 109, 95, 39, 11, 243, 207, 170, 138, 43, 11, 58, 18, 26, 253, 149, 168, 147, 115, 74, 113, 76, 228, 208, 160, 134, 96, 95, 112, 0, 237, 131, 173, 155, 122, 69, 49, 79, 242, 205, 173, 144, 46, 64, 50, 56, 11, 240, 217, 181, 142, 106, 17, 108, 11, 174 }, 237) +
        UrlObfuscator.decode(new int[] { 131, 126, 93, 47, 25, 241, 144, 178, 223, 110, 73, 46, 91, 185, 153, 244 }, 254);
    private static final String VIDEO_LOADER_JS =
        UrlObfuscator.decode(new int[] { 39, 72, 56, 2, 232, 222, 160, 135, 105, 14, 108, 31 }, 271) +
        UrlObfuscator.decode(new int[] { 73, 89, 118, 10, 245, 213, 190, 150, 111, 25, 9, 42, 226, 218, 182, 189, 127, 78, 42, 8, 254, 233, 165, 156, 102, 67, 111, 23, 225, 215, 183, 147, 110, 36, 73, 52, 18, 255, 213, 174, 214, 72, 105, 35, 29, 247, 254, 190, 145, 107, 75, 63, 46, 228, 223, 167, 140, 58, 82, 55, 17, 230, 153 }, 288) +
        UrlObfuscator.decode(new int[] { 64, 52, 6, 179, 192, 176, 150, 50, 30, 118 }, 54) +
        UrlObfuscator.decode(new int[] { 33, 19, 235, 199, 183, 139, 110, 78, 31, 61, 14, 239, 147, 243, 130, 108, 69, 47, 14, 253, 213, 250, 149, 127, 76, 59, 0, 233, 197, 190, 199, 111, 66, 50, 32, 232, 198, 175, 132, 110, 107, 124, 36, 53, 255, 146, 254, 167, 72, 64, 60, 16, 223, 221, 176, 148, 106, 92, 14, 31, 248, 141, 224, 193, 117, 67, 49, 17, 241, 204, 250, 150, 158, 108, 29, 52, 70, 254, 214, 187, 130, 123, 80, 58, 7, 188, 217, 181, 142, 106, 81, 48, 15, 229, 202, 189, 138, 99, 75, 48, 77, 230, 206, 163, 170, 147, 120, 82, 47, 63, 245, 221, 186, 147, 123, 64, 104, 27, 247, 152, 238, 134, 36, 94, 46, 30, 252, 218, 169, 221, 115, 69, 49, 66, 242, 212, 130, 186, 146, 127, 78, 55, 28, 246, 195, 248, 150, 102, 86, 51, 5, 245, 234, 162, 136, 97, 78, 36, 29, 160, 128, 181, 145, 125, 79, 39, 70, 169, 164, 205, 169, 210, 114, 94, 100, 95, 200, 233, 163, 157, 119, 126, 62, 17, 235, 203, 191, 175, 120, 89, 110, 83 }, 71) +
        "st.textContent='@keyframes __vlspin{to{transform:rotate(360deg)}}.__vl{position:fixed;z-index:2147483000;pointer-events:none;display:none;align-items:center;justify-content:center;background:rgba(8,12,28,.6);transition:opacity .25s ease;opacity:0;box-sizing:border-box}.__vl.on{opacity:1}.__vl i{width:38px;height:38px;border-radius:50%;border:3px solid rgba(255,255,255,.25);border-top-color:#fff;animation:__vlspin .8s linear infinite;box-sizing:border-box}';" +
        UrlObfuscator.decode(new int[] { 48, 89, 247, 197, 164, 150, 124, 85, 19, 7, 231, 193, 168, 195, 121, 93, 97, 92, 251, 198, 165, 151, 97, 73, 104, 58, 87, 230, 193, 166 }, 88) +
        UrlObfuscator.decode(new int[] { 15, 253, 201, 165, 145, 109, 76, 44, 65, 238, 250, 219, 185, 143, 51, 76, 112, 3, 227, 196, 172, 143 }, 105) +
        UrlObfuscator.decode(new int[] { 19, 255, 144, 161, 216, 112, 70, 33, 29, 227, 204, 179, 152, 35, 94, 46, 11, 237, 209, 148, 146, 100, 80, 38, 92, 188, 147, 195, 162, 139, 50, 85, 63, 13, 239, 216, 164, 158, 71, 71, 51, 5, 245, 146, 243, 208, 63, 2, 56, 12, 252, 210, 180, 139, 36, 69, 35, 13, 243, 250, 133 }, 122) +
        UrlObfuscator.decode(new int[] { 226, 204, 225, 201, 113, 8, 38, 17, 241, 208, 164, 142, 139, 77, 79, 63, 93, 188, 152, 174, 217, 113, 80, 32, 50, 230, 197, 162, 134, 108, 88, 56, 14, 162, 142, 187, 149, 101, 2, 109, 69, 164, 128, 182, 241, 143, 104, 89, 41, 3, 202, 221, 187, 147, 118, 64, 60, 0, 185, 151, 188, 129, 120, 94, 40, 15, 174, 129, 238, 148, 96, 80, 54, 16, 239, 128, 217, 191, 145, 111, 94, 97 }, 139) +
        UrlObfuscator.decode(new int[] { 238, 222, 174, 140, 106, 89, 118, 3, 186, 221, 183, 133, 103, 64, 60, 6, 223, 223, 171, 157, 109, 26, 123, 88, 182, 223, 190, 151, 46, 108, 91, 56, 23, 242, 212, 190, 132, 107, 23, 35, 90, 227, 211, 164, 131, 106, 74, 49, 16, 253, 132, 168, 157, 115, 73, 53, 8, 226, 219, 189, 156, 222, 63, 75, 114, 36, 197, 207, 180, 160, 119, 92, 32, 72 }, 156) +
        UrlObfuscator.decode(new int[] { 208, 175, 138, 126, 74, 32, 79, 227, 140, 191, 145, 103, 85, 53, 45, 16, 189, 218, 186, 150, 106, 93, 108, 11, 232 }, 173) +
        UrlObfuscator.decode(new int[] { 216, 168, 146, 120, 78, 48, 23, 249, 150, 189, 157, 119, 87, 121, 31, 166, 213, 162, 194, 104, 70, 40, 27, 244, 234, 172, 151, 119, 12, 51, 5, 18, 241, 203, 185, 211, 61, 86, 54, 80, 191, 142, 167, 150, 102, 101, 57, 2, 235, 194, 185, 159, 34, 79, 61, 9, 229, 209, 173, 140, 108, 9, 105, 36, 23, 251, 148, 250, 149, 55, 91, 59, 23, 230, 199, 159, 155, 98, 68, 97, 13, 226, 194, 191, 139, 96, 70, 52, 78, 162, 203, 173, 197, 40, 9, 80, 112, 14, 232, 194, 182, 156, 54, 83, 63, 6, 228, 223, 179, 136, 45, 8, 32, 2, 226, 206, 237, 210, 117, 11, 116, 83, 180, 138, 249, 156 }, 190) +
        UrlObfuscator.decode(new int[] { 169, 155, 99, 79, 63, 3, 230, 198, 231, 146, 108, 71, 40, 74, 168, 219, 220, 173, 142, 52, 18, 97, 15, 249, 197, 246, 131, 103, 14, 54, 30, 243, 218, 163, 136, 98, 95, 100, 14, 237, 211, 131, 137, 97, 78, 39, 15, 244, 236, 252, 164, 168, 122, 93, 23, 25, 250, 211, 253, 211, 101, 91, 53, 21, 224, 137, 228, 215, 125, 75, 59, 72, 230, 197, 177, 141, 117, 71, 124, 80, 68 }, 207) +
        UrlObfuscator.decode(new int[] { 134, 144, 108, 21, 42, 26, 232, 153, 177, 202, 38, 14, 61, 79, 228, 194, 254, 131, 107, 67, 43, 31, 226, 146, 161, 204, 45, 12, 63, 21, 227, 211, 224, 169, 195, 107, 79, 0, 19, 196, 131, 161, 151, 103, 20, 60, 79, 231, 158, 144, 177, 123, 64, 112, 28, 232, 218, 231, 148, 56, 82, 109, 5, 228, 212, 253, 177, 136, 114, 95, 51, 23, 255, 244, 186, 156, 113, 93, 38, 35, 245, 204, 186, 197, 37, 16, 60, 8, 250, 135, 165, 150, 57, 68, 39, 21, 195, 240, 211, 173, 137, 111, 95, 61, 43, 227, 207, 185, 145, 59, 68, 120, 75 }, 224) +
        UrlObfuscator.decode(new int[] { 135, 113, 93, 110, 30, 228, 196, 189, 212, 102, 66, 35, 1, 247, 139, 180, 200, 38, 57, 76, 115, 11, 242, 222, 173, 144, 41, 11, 99, 68, 181, 148, 163, 222, 103, 75, 36, 11, 227, 222, 247, 213, 49, 22, 99, 66, 241, 140, 163, 143, 139, 106, 82, 49, 69, 170, 159, 254, 133, 56, 65, 59, 3, 174, 216, 190, 129, 107, 95, 4, 14, 227, 206, 160, 147, 32, 3, 54, 77, 240, 200, 167, 183, 138, 35, 12, 125, 92, 235, 150, 187, 147, 115, 64, 111, 27, 255, 222, 170, 156, 90, 69, 47, 30, 225, 142, 225, 133, 118, 10, 53, 11, 242, 201, 221, 183, 145, 117, 79, 35, 88, 165, 138, 241, 157, 125, 87, 54, 20, 254, 136, 232, 203, 111, 88, 100, 13, 225, 212, 182, 137, 101, 90, 99, 92, 189, 184, 208, 178, 146, 126, 29, 98 }, 241) +
        UrlObfuscator.decode(new int[] { 107, 71, 104, 44, 22, 242, 203, 242, 129 }, 258) +
        UrlObfuscator.decode(new int[] { 122, 84, 121, 81, 224, 135, 182, 131, 54, 78, 38, 11, 242, 203, 160, 138, 119, 12, 34, 18, 26, 255, 201, 185, 190, 118, 92, 53, 18, 248, 193, 252, 212, 118, 88, 38, 72, 167, 150, 163, 197, 105, 69, 41, 20, 245, 235, 165, 142, 103, 28, 103, 0, 33, 235, 208, 252, 193, 118, 22, 54, 6, 229, 209, 189, 150, 82, 88, 38, 2, 233, 132, 175, 133, 106, 93, 42, 3, 235, 208, 237, 129, 115, 69, 94, 42, 24, 217, 215, 191, 148, 125, 89, 34, 93, 179, 218, 245, 216, 57, 20, 102, 9, 227, 200, 191, 132, 109, 73, 50, 75, 230, 204, 166, 152, 124, 99, 90, 50, 31, 238, 215, 188, 150, 99, 24, 49, 27, 240, 199, 188, 149, 97, 90, 8, 0, 238, 199, 172, 134, 115, 15, 107, 5, 243, 210, 164, 142, 155, 93, 85, 53, 23, 254, 145, 183, 222, 45, 67, 122, 44, 205, 199, 188, 210, 97, 22, 49 }, 275) +
        UrlObfuscator.decode(new int[] { 82, 34, 16, 161, 215, 222, 173, 192, 115, 21, 41, 13, 225, 219, 179, 219, 112, 90, 33, 1, 252, 206, 183, 208, 49, 22, 109, 15, 228, 194, 190, 194, 63 }, 292) +
        UrlObfuscator.decode(new int[] { 85, 119, 11, 227, 207, 185, 145, 61, 86, 56, 3, 255, 194, 172, 149, 54, 13, 47, 4, 226, 222, 226, 223, 108, 12, 50, 20, 6, 242, 216, 242, 151, 127, 95, 44, 74, 228, 155, 184, 150, 116, 69, 123, 72, 254, 213, 235, 208, 101, 7, 59, 19, 255, 201, 161, 205, 118, 78, 48, 98, 12, 179, 200, 180, 138, 50, 31, 39, 14, 178, 143, 188, 220, 98, 68, 54, 2, 232, 130, 188, 131, 109, 92, 47, 91, 247, 138, 180, 139, 101, 84, 87, 117, 90, 236, 195, 253, 194, 119, 25, 37, 1, 237, 223, 183, 223, 120, 74, 39, 10, 228, 223, 247, 155, 38, 79, 35, 12, 227, 203, 182, 202, 39, 111, 70, 122, 71, 244, 148, 170, 140, 110, 90, 48, 90, 241, 221, 163, 148, 106, 92, 31, 13, 239, 195, 188, 155, 58, 69, 54, 74, 225, 205, 179, 132, 154, 108, 111, 61, 31, 243, 204, 171, 204 }, 58) +
        UrlObfuscator.decode(new int[] { 34, 12, 161, 137, 176, 135, 118, 13, 56, 74, 231, 213, 209, 189, 137, 117, 84, 52, 81, 224, 158, 173, 135, 113, 66, 39, 20, 227, 219, 143, 131, 101, 70, 43, 29, 225, 200, 168, 163, 118, 66, 47, 4, 168, 249, 203, 179, 159, 111, 83, 54, 22, 191, 159, 174, 140, 61, 81, 61, 17, 252, 221, 129, 133, 120, 94, 103, 9, 227, 194, 237, 195, 108, 76, 102, 73, 68, 227, 148, 231, 134, 51, 17, 55, 94, 173, 200, 177, 159, 97, 84, 43, 0, 160, 206, 160, 138, 121, 90, 4, 14, 245, 209, 234, 130, 102, 69, 104, 120, 17, 243, 155, 242, 193, 100 }, 75) +
        UrlObfuscator.decode(new int[] { 61, 24, 238, 208, 174, 146, 61, 30, 111 }, 92) +
        UrlObfuscator.decode(new int[] { 16, 233, 199, 185, 140, 40, 78, 32, 77, 235, 133, 228, 142, 46, 108, 74, 36, 16, 254, 148, 189, 145, 100, 70, 57, 21, 234, 147, 236, 205, 40, 64, 34, 2, 238, 141, 239, 206, 104, 8, 38, 8, 226, 209, 178, 172, 150, 109, 73, 114, 24, 245, 215, 172, 150, 127, 91, 39, 91, 181, 222, 190, 200, 39, 4, 55, 3, 227, 205, 173, 207, 105, 12, 127, 30 }, 109) +
        "}" +
        UrlObfuscator.decode(new int[] { 12, 248, 200, 174, 136, 119, 24, 54, 21, 225, 221, 165, 151, 42, 77 }, 126) +
        UrlObfuscator.decode(new int[] { 233, 219, 163, 143, 127, 67, 38, 6, 167, 192, 183, 133, 110, 71, 105, 73, 4, 236, 220, 186, 198, 42, 2, 44, 5, 239, 206, 189, 149, 58, 69, 57, 12, 229, 133, 229, 213, 58, 0, 58, 6, 224, 152, 182, 134, 115, 84, 37, 44, 10, 220, 210, 178, 151, 120, 76, 62, 25, 251, 242, 161, 147, 124, 85, 103, 8, 255, 205, 166, 143, 32, 19, 58, 5, 228, 208, 160, 138, 41, 69, 22, 37, 0, 225 }, 143) +
        UrlObfuscator.decode(new int[] { 198, 202, 176, 158, 104, 82, 53, 23, 184, 192, 183, 156, 96, 27, 55, 88, 235, 198, 168, 197, 105, 5, 62, 8, 250, 192, 163, 145, 34, 5, 39, 79, 244, 254, 204, 186, 153, 111, 20, 45, 25, 240, 248, 180, 153, 118, 15, 108, 77, 168, 248, 132, 168, 78, 101, 110, 65, 226, 136, 177, 133, 113, 69, 36, 20, 81, 193, 226, 170, 151, 77, 88, 49, 3, 171, 193, 166, 134, 119, 10, 45 }, 160) +
        UrlObfuscator.decode(new int[] { 215, 165, 129, 109, 89, 37, 4, 228, 137, 167, 140, 46, 64, 109, 24, 235, 199, 232, 186, 208, 105, 93, 41, 29, 252, 204, 241, 208, 112, 26, 39, 19, 227, 215, 170, 154, 35, 88, 42, 13, 199, 201, 170, 131, 56, 25, 126, 69, 215, 233, 251, 155, 178, 59, 18, 63, 87, 236, 214, 164, 146, 113, 71, 124, 46, 207, 217, 162, 186, 109, 66, 62, 84, 238, 198, 170, 150, 97, 24, 63 }, 177) +
        UrlObfuscator.decode(new int[] { 153, 198, 119, 126, 87, 41, 21, 245, 221, 254, 212, 48, 69, 33, 21, 255, 222, 180, 148, 40, 2, 106, 31, 238, 207, 162, 129, 105, 65, 98, 57, 173, 196, 174, 146, 186, 127, 94, 52, 83, 252, 204, 182, 148, 98, 92, 59, 29, 186, 197, 249, 148, 106, 66, 47, 30, 231, 204, 166, 147, 40, 68, 32, 7, 199, 215, 165, 177, 138, 81, 85, 40, 14, 252, 214, 178, 132, 61, 64, 127, 5, 240, 217, 187, 194, 121, 94, 62, 15, 160, 147, 186, 207, 62 }, 194) +
        UrlObfuscator.decode(new int[] { 136, 213, 97, 92, 46, 23, 228, 194, 172, 205, 37, 15, 36, 7, 235, 212, 175, 131, 120, 7, 19, 121, 30, 253, 213, 170, 149, 121, 78, 34, 29, 230, 220, 167, 150, 120, 8, 98, 74, 224, 196, 171, 141, 109, 67, 34, 4, 240, 194, 229, 205, 39, 122, 76, 47, 19, 233, 157, 245, 223, 114, 91, 37, 0, 250, 215, 181, 215, 35, 9, 44, 14, 228, 216, 189, 207, 90, 8, 35, 11, 241, 231, 160, 131, 151, 54, 91, 41, 21, 249, 205, 177, 152, 120, 29, 32, 90, 233, 213, 191, 140, 123, 64, 41, 5, 254, 135, 169, 131, 98, 96, 50, 6, 236, 213, 140, 182, 141, 105, 89, 53, 31, 235, 144, 163, 218, 122, 95, 127, 6, 227, 197, 170, 199, 54, 81, 98, 81 }, 211) +
        UrlObfuscator.decode(new int[] { 151, 102, 86, 8, 14, 11, 251, 207, 170, 154, 118, 17, 62, 2, 248, 214, 160, 154, 125, 95, 120, 70, 245, 196, 170, 195, 43, 91, 41, 1, 175, 195, 182, 130, 111, 68, 104, 118, 69, 224, 144, 233, 207, 41, 17, 108 }, 228) +
        UrlObfuscator.decode(new int[] { 136, 61, 27, 123, 74 }, 245);
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
        UrlObfuscator.decode(new int[] { 46, 67, 49, 13, 225, 213, 169, 176, 144, 53, 21, 32 }, 262) +
        UrlObfuscator.decode(new int[] { 126, 80, 125, 3, 250, 220, 181, 159, 120, 0, 62, 4, 228, 221, 134, 152, 98, 72, 3, 13, 239, 199, 145, 137, 156, 117, 88, 46, 82, 232, 220, 172, 130, 100, 91, 111 }, 279) +
        UrlObfuscator.decode(new int[] { 95, 46, 8, 225, 203, 180, 204, 114, 72, 80, 41, 50, 236, 222, 180, 191, 113, 91, 51, 37, 253, 208, 185, 148, 98, 18, 40, 24, 226, 200, 190, 128, 103, 73, 110, 10, 244, 215, 177, 200, 123 }, 296) +
        UrlObfuscator.decode(new int[] { 81, 45, 8, 232, 135, 182, 136, 99, 69, 41, 8, 232, 207, 234 }, 62) +
        UrlObfuscator.decode(new int[] { 61, 11, 249, 217, 185, 132, 41, 70, 34, 17, 165, 244, 177, 141, 108, 73, 76, 59, 85, 250, 206, 180, 154, 108, 94, 57, 27, 188, 193, 183, 130, 127, 67, 56, 8, 160, 217, 175, 131, 109, 68, 50, 76, 255 }, 79) +
        UrlObfuscator.decode(new int[] { 22, 30, 236, 157, 181, 149, 106, 76, 44, 74, 242, 218, 183, 134, 127, 84, 62, 27, 160, 206, 190, 142, 107, 93, 45, 34, 234, 192, 169, 134, 108, 85, 104, 120, 23, 243, 204, 174, 142, 62, 17, 108 }, 96) +
        UrlObfuscator.decode(new int[] { 24, 254, 223, 187, 153, 34, 95, 51, 25, 237, 154, 225, 131, 109, 79, 39, 70, 187 }, 113) +
        UrlObfuscator.decode(new int[] { 235, 199, 232, 176, 142, 105, 79, 117, 23, 236, 212, 163, 159, 101, 88, 54, 91, 248, 222, 191, 155, 121, 2, 38, 31, 229, 220, 174, 150, 105, 65, 126, 22, 243, 213, 218, 229 }, 130) +
        UrlObfuscator.decode(new int[] { 231, 192, 168, 139, 121, 79, 63, 76, 238, 210, 189, 155, 58, 125, 24, 95, 171, 205, 177, 148, 140, 48, 73, 37, 11, 255, 202, 164, 139, 77, 104, 125, 93, 244, 222, 162, 170, 111, 78, 36, 67, 236, 220, 166, 132, 114, 76, 43, 13, 170, 213, 233, 164 }, 147) +
        UrlObfuscator.decode(new int[] { 210, 162, 144, 33, 65, 2, 42, 91, 186, 207, 244, 152, 123, 84, 51, 5, 224, 136, 187, 151, 56, 14, 47, 68, 254, 206, 190, 156, 122, 73, 125 }, 164) +
        UrlObfuscator.decode(new int[] { 250, 182, 153, 119, 82, 36, 65, 229, 200, 181, 152, 34, 72, 97, 73, 224, 202, 182, 166, 99, 66, 40, 119, 24, 232, 210, 184, 142, 112, 87, 57, 94, 248, 221, 190, 151, 56, 75, 42, 22, 249, 223, 229, 154, 124, 91, 47, 78, 232, 205, 174, 135, 40, 27 }, 181) +
        UrlObfuscator.decode(new int[] { 238, 132, 95, 78, 43, 12, 229, 194, 194, 161, 167, 70, 19, 119, 30, 248, 196, 144, 149, 112, 90, 121, 22, 250, 192, 174, 152, 98, 69, 39, 64, 226, 222, 177, 205, 120, 71, 57, 20, 12, 176, 205, 169, 136, 114, 17, 61, 15, 226, 156, 239, 142, 59, 10, 45, 70, 181, 208, 229, 208 }, 198) +
        UrlObfuscator.decode(new int[] { 190, 144, 61, 81, 43, 6, 226, 158, 163, 139, 99, 75, 63, 2, 160, 193, 169, 150, 112, 80, 109, 3, 226, 195, 218, 174, 137, 33, 94, 34, 13, 235, 153, 188, 154, 125, 93, 122, 86, 188, 136, 231, 214, 113, 72, 43, 29, 235, 207, 238, 128, 45, 88, 63 }, 215) +
        UrlObfuscator.decode(new int[] { 129, 105, 86, 48, 16, 173, 209, 181, 153, 147, 123, 19, 56, 18, 233, 201, 180, 150, 111, 8, 115, 29, 253, 223, 181, 200, 53 }, 232) +
        UrlObfuscator.decode(new int[] { 157, 119, 84, 35, 24, 241, 221, 166, 223, 114, 64, 42, 20, 162, 202, 186, 153, 109, 73, 34, 38, 236, 202, 174, 133, 40, 118, 80, 45, 9, 239, 147, 226 }, 249) +
        UrlObfuscator.decode(new int[] { 99, 71, 56, 18, 242, 139, 165, 135, 102, 100, 54, 58, 16, 233, 240, 178, 137, 109, 93, 57, 19, 231, 156, 244, 145, 121, 81, 33, 9, 232, 139, 231, 140, 124, 70, 36, 18, 236, 203, 173, 202, 40, 91 }, 266) +
        UrlObfuscator.decode(new int[] { 111, 72, 32, 3, 243, 217, 182, 129, 126, 87, 63, 4, 161, 204, 162, 136, 114, 4, 59, 13, 234, 201, 179, 129, 64, 74, 40, 12, 27, 182, 212, 178, 139, 111, 77, 113, 76, 235, 214, 181, 135, 113, 89, 120, 10, 167, 214, 177 }, 283) +
        UrlObfuscator.decode(new int[] { 88, 54, 71, 175, 196, 162, 155, 127, 93, 102, 1, 239, 201, 161, 144, 126, 93, 97, 54, 16, 237, 201, 175, 212, 127, 81, 59, 19, 230, 154, 191, 151, 127, 87, 59, 6, 164, 215 }, 49) +
        UrlObfuscator.decode(new int[] { 52, 0, 242, 191, 219, 175, 142, 32, 78, 43, 1, 236, 211, 167, 134, 46, 92, 52, 7, 175, 234, 130, 161, 78, 82, 42, 13, 247, 210, 172, 139, 109, 10, 102, 52, 23, 251, 157, 169, 136, 127, 75, 120, 22, 244, 218, 166, 135, 119, 85, 112, 14, 174, 223, 169, 154, 127, 76, 59, 19, 168, 130, 232, 196, 67, 67, 47, 45, 10, 216, 206, 169, 149, 107, 31, 126, 77, 232 }, 66) +
        UrlObfuscator.decode(new int[] { 48, 19, 229, 211, 167, 198, 104, 5, 48, 15, 251, 218, 250, 136, 96, 83, 99, 39, 243, 210, 208, 172, 213, 59, 122, 56, 22, 234, 195, 147, 135, 102, 92, 32, 86, 185, 148, 179 }, 83) +
        UrlObfuscator.decode(new int[] { 22, 230, 200, 164, 131, 139, 54, 88, 46, 9, 179, 130, 170, 146, 98, 64, 38, 29, 169, 204 }, 100) +
        UrlObfuscator.decode(new int[] { 3, 245, 193, 242, 153, 113, 65, 42, 1, 233, 216, 247, 168, 122, 85, 39, 28, 170, 211, 176, 142, 116, 112, 74, 36, 12, 254, 148, 180, 153, 103, 24, 54, 21, 255, 222, 249, 153, 97, 94, 56, 24, 165, 204, 160, 132, 98, 85, 105, 2, 246, 204, 162, 148, 150, 113, 83, 116, 29, 243, 213, 189, 222, 109 }, 117) +
        UrlObfuscator.decode(new int[] { 244, 192, 176, 150, 112, 79, 59, 52, 23, 243, 216, 225, 221, 127, 81, 59, 19, 178, 152, 189, 147, 124, 85, 117, 8, 228, 192, 174, 196, 103, 73, 42, 3, 169, 195, 166, 150, 71, 73, 83, 59, 71, 250, 206, 180, 154, 108, 94, 57, 27, 188, 154, 169, 131, 117, 91, 59, 31, 226, 139, 154, 155, 103, 74, 47, 22, 225, 141, 176, 132, 115, 112, 82, 43, 25, 179, 220, 176, 148, 114, 31, 110, 9, 238, 137, 172, 217, 52 }, 134) +
        UrlObfuscator.decode(new int[] { 229, 211, 166, 155, 127, 68, 52, 88, 224, 222, 185, 159, 37, 71, 60, 4, 243, 207, 181, 136, 102, 29, 41, 1, 17, 250, 209, 185, 136, 32, 98, 48, 22, 248, 209, 184, 150, 97, 106, 96, 50, 211, 132, 247, 150, 35, 18 }, 151) +
        UrlObfuscator.decode(new int[] { 193, 169, 150, 112, 80, 109, 1, 237, 201, 220, 181, 213, 53, 0, 39, 80, 163, 202, 237 }, 168) +
        UrlObfuscator.decode(new int[] { 196, 240, 222, 63 }, 185);
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
            line.setColor(Color.parseColor(UrlObfuscator.decode(new int[] { 233, 217, 74, 22, 113, 81, 180 }, 202)));
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
            int fur = Color.parseColor(UrlObfuscator.decode(new int[] { 248, 188, 45, 126, 98, 48, 215 }, 219));
            int pink = Color.parseColor(UrlObfuscator.decode(new int[] { 207, 77, 30, 8, 91, 196, 150 }, 236));
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
            line.setColor(Color.parseColor(UrlObfuscator.decode(new int[] { 222, 93, 126, 24, 65, 221, 135 }, 253)));
            c.drawLine(22, 47, 9, 44, line);
            c.drawLine(22, 52, 9, 53, line);
            c.drawLine(78, 47, 91, 44, line);
            c.drawLine(78, 52, 91, 53, line);
            line.setColor(Color.parseColor(UrlObfuscator.decode(new int[] { 45, 29, 14, 90, 189, 157, 248 }, 270)));

            fill.setColor(Color.parseColor(UrlObfuscator.decode(new int[] { 60, 15, 105, 78, 175, 143, 154 }, 287)));
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
            shape(c, tear, Color.parseColor(UrlObfuscator.decode(new int[] { 22, 108, 54, 214, 137, 150, 215 }, 53)));

            android.graphics.Path nose = new android.graphics.Path();
            nose.moveTo(47.5f, 48f); nose.lineTo(52.5f, 48f); nose.lineTo(50f, 51f); nose.close();
            fill.setColor(Color.parseColor(UrlObfuscator.decode(new int[] { 101, 32, 188, 148, 247, 216, 69 }, 70))); c.drawPath(nose, fill);

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
            paint.setColor(Color.parseColor(UrlObfuscator.decode(new int[] { 116, 48, 167, 242, 225, 183, 84 }, 87)));
            canvas.drawCircle(cx, cy, stroke * 1.05f, paint);

            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(stroke);
            paint.setColor(Color.parseColor(UrlObfuscator.decode(new int[] { 75, 178, 195, 160, 133, 103, 22 }, 104)));
            float[] radii = {w * 0.24f, w * 0.38f};
            int[] alphas = {235, 130};
            for (int i = 0; i < radii.length; i++) {
                paint.setAlpha(alphas[i]);
                RectF arc = new RectF(cx - radii[i], cy - radii[i], cx + radii[i], cy + radii[i]);
                canvas.drawArc(arc, 208, 124, false, paint);
            }

            paint.setAlpha(255);
            paint.setColor(Color.parseColor(UrlObfuscator.decode(new int[] { 90, 222, 241, 224, 183, 33, 4 }, 121)));
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
        static final float SPEED_MULT = 0.6f;

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
            String name = (appName == null || appName.trim().isEmpty()) ? UrlObfuscator.decode(new int[] { 203, 217, 184 }, 138) : appName.trim().toUpperCase();

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
            letter.setTypeface(Typeface.create(UrlObfuscator.decode(new int[] { 232, 219, 183, 139, 58, 69, 48, 6, 250, 212, 252, 157, 106, 74, 36, 25, 230 }, 155), Typeface.NORMAL));
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
                videoResId = context.getResources().getIdentifier(UrlObfuscator.decode(new int[] { 207, 190, 153, 125, 71, 42, 57, 246, 212, 175, 131, 114, 72 }, 172), UrlObfuscator.decode(new int[] { 207, 189, 140 }, 189), pkg);
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
                int resId = context.getResources().getIdentifier(UrlObfuscator.decode(new int[] { 173, 152, 127, 95, 37, 4, 215, 212, 182, 137, 101, 80, 42 }, 206), UrlObfuscator.decode(new int[] { 187, 140, 124, 75, 58, 24, 245, 221 }, 223), pkg);
                if (resId != 0) {
                    iv.setImageURI(Uri.parse(UrlObfuscator.decode(new int[] { 145, 97, 74, 63, 3, 226, 206, 231, 154, 98, 85, 42, 17, 241, 193, 164, 218, 208, 49 }, 240) + pkg + "/" + resId));
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
                mp.setDataSource(context, Uri.parse(UrlObfuscator.decode(new int[] { 96, 78, 91, 44, 18, 245, 223, 244, 139, 125, 68, 57, 0, 230, 208, 183, 203, 63, 0 }, 257) + pkg + "/" + videoResId));
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
            int iconResId = context.getResources().getIdentifier(UrlObfuscator.decode(new int[] { 123, 82, 15, 3, 239, 216, 162, 136, 98, 76, 58 }, 274), UrlObfuscator.decode(new int[] { 78, 43, 17, 237, 254, 206 }, 291), context.getPackageName());
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
        root.setBackgroundColor(Color.parseColor(UrlObfuscator.decode(new int[] { 26, 104, 66, 166, 128, 228, 198 }, 57)));

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
        // Starts slightly shrunk/faded/dropped so the reveal in
        // onPageFinished below has something to animate from -- otherwise
        // it'd just pop in at full size the instant it's set VISIBLE.
        webView.setAlpha(0f);
        webView.setScaleX(0.94f);
        webView.setScaleY(0.94f);
        webView.setTranslationY(14f);
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
        swipeRefresh.setColorSchemeColors(Color.parseColor(UrlObfuscator.decode(new int[] { 105, 91, 204, 145, 133, 161, 66 }, 74)));
        swipeRefresh.addView(webView, new ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        root.addView(swipeRefresh, webParams);

        // Status-bar and navigation-bar strips. Added before the splash /
        // error / navigation overlays so those still cover the whole screen.
        topScrim = new View(this);
        topScrim.setBackgroundColor(Color.parseColor(UrlObfuscator.decode(new int[] { 120, 74, 172, 136, 226, 198, 32 }, 91)));
        root.addView(topScrim, new FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT, 0, Gravity.TOP));
        bottomScrim = new View(this);
        bottomScrim.setBackgroundColor(Color.parseColor(UrlObfuscator.decode(new int[] { 79, 187, 159, 249, 221, 55, 19 }, 108)));
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

        final SplashController loading = new CustomSplashView(this, Color.parseColor(UrlObfuscator.decode(new int[] { 94, 172, 139, 234, 201, 40, 7 }, 125)), true);
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
        final String offlineScrimColor = offlineIsNightMode ? UrlObfuscator.decode(new int[] { 173, 148, 245, 219, 58, 25, 120, 87, 182 }, 142) : UrlObfuscator.decode(new int[] { 188, 136, 235, 204, 43, 10, 105, 72, 167 }, 159);
        // Slightly translucent (not fully opaque) card/button fills, so a
        // hint of the dimmed scrim behind still shows through -- a cheap
        // stand-in for a true frosted-glass blur, which would need
        // RenderEffect (API 31+) and a snapshot of whatever's behind the
        // card to do for real.
        final String offlineCardBg = offlineIsNightMode ? UrlObfuscator.decode(new int[] { 147, 138, 214, 63, 111, 121, 47, 186, 152 }, 176) : UrlObfuscator.decode(new int[] { 226, 162, 199, 88, 15, 26, 73, 220, 139 }, 193);
        final String offlineTitleColor = offlineIsNightMode ? UrlObfuscator.decode(new int[] { 241, 183, 86, 105, 8, 43, 202 }, 210) : UrlObfuscator.decode(new int[] { 192, 50, 17, 112, 111, 78, 173 }, 227);
        final String offlineSubtitleColor = offlineIsNightMode ? UrlObfuscator.decode(new int[] { 215, 81, 10, 19, 72, 205, 234 }, 244) : UrlObfuscator.decode(new int[] { 38, 17, 1, 87, 195, 149, 250 }, 261);
        final String offlineButtonBg = offlineIsNightMode ? UrlObfuscator.decode(new int[] { 53, 13, 23, 71, 170, 133, 232, 219, 79 }, 278) : UrlObfuscator.decode(new int[] { 4, 126, 38, 192, 149, 134, 215, 68, 94 }, 295);
        final float offlineDensity = getResources().getDisplayMetrics().density;

        // Outer full-screen scrim so the card reads as a dialog sitting on
        // top of the app, matching how a native "no connection" alert
        // dims everything behind it.
        final FrameLayout errorView = new FrameLayout(this);
        errorView.setBackgroundColor(Color.parseColor(UrlObfuscator.decode(new int[] { 30, 108, 58, 170, 248, 232, 181 }, 61)));
        errorView.setVisibility(View.GONE);

        // The error screen is the branded HTML page in ERROR_PAGE_HTML,
        // rendered in its own small WebView that fills errorView. Its
        // Retry / Back buttons call back into native code through the
        // AndroidError interface (registered below, once doRetry exists).
        // Navigation inside it is blocked so it can only ever show that page.
        final WebView errorWeb = new WebView(this);
        errorWeb.setBackgroundColor(Color.parseColor(UrlObfuscator.decode(new int[] { 109, 93, 205, 155, 139, 217, 74 }, 78)));
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
        navOverlay.setBackgroundColor(Color.parseColor(UrlObfuscator.decode(new int[] { 124, 78, 173, 140, 235, 202, 41 }, 95)));
        navOverlay.setVisibility(View.GONE);
        final BouncingDotsView navDots = new BouncingDotsView(this, Color.parseColor(UrlObfuscator.decode(new int[] { 83, 201, 156, 139, 222, 78, 111 }, 112)));
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
        }, UrlObfuscator.decode(new int[] { 192, 206, 219, 172, 146, 117, 95, 31, 11, 234, 216, 164 }, 129));

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
        webView.setLayerType(View.LAYER_TYPE_HARDWARE, null);
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
        String spoofedUA = defaultUA.replace(UrlObfuscator.decode(new int[] { 169, 145, 167, 153 }, 146), "").replaceAll("Version/[0-9.]+\s", "");
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
        webView.addJavascriptInterface(new AndroidBridge(), UrlObfuscator.decode(new int[] { 226, 172, 133, 114, 112, 87, 57, 62, 233, 211, 189, 159, 114 }, 163));

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
            webView.animate().cancel();
            webView.animate()
                .alpha(1f).scaleX(1f).scaleY(1f).translationY(0f)
                .setDuration(460)
                .setInterpolator(new OvershootInterpolator(0.9f))
                .start();
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
                    UrlObfuscator.decode(new int[] { 156, 181, 135, 127, 83, 59, 7, 226, 194, 227, 195, 114 }, 180) +
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
                    UrlObfuscator.decode(new int[] { 177, 150, 122, 89 }, 197) +
                    "var vp=document.querySelector('meta[name=\"viewport\"]');" +
                    UrlObfuscator.decode(new int[] { 191, 147, 60, 18, 36, 1, 185, 212, 184, 157, 49, 79, 37, 10, 253, 202, 163, 139, 112, 13, 33, 19, 229, 254, 202, 184, 185, 119, 95, 52, 29, 249, 194, 253, 211, 126, 87, 37, 17, 168, 135, 246, 154, 123, 4, 58, 13, 243, 231, 177, 144, 113, 75, 35, 21, 11, 251, 149, 251, 149, 123, 84, 61, 80, 186, 146, 162, 154, 119, 70, 32, 0, 252, 217, 235, 194, 49, 77, 39, 4, 243, 200, 161, 141, 118, 15, 40, 58, 31, 249, 146, 186, 138, 105, 93, 57, 18, 214, 220, 186, 158, 117, 24, 57, 30, 164, 151, 182 }, 214) +
                    UrlObfuscator.decode(new int[] { 145, 118, 11, 55, 6, 246, 224, 180, 171, 140, 116, 94, 46, 14, 252, 144, 240, 149, 122, 90, 39, 23, 255, 196, 232, 194, 42, 91, 34, 14, 253, 192, 250, 130, 96, 82, 42, 1, 228, 141, 200, 183, 153, 104, 83, 118, 89, 241, 217, 191, 129, 125, 82, 62, 92, 227, 204, 175, 129, 105, 22, 123, 71, 184, 139, 230, 136, 101, 91, 43, 12, 245, 242, 147, 174, 159, 122, 86, 60, 69, 166, 152, 229, 216, 51, 95, 56, 30, 230, 195, 184, 129, 38, 89, 42, 9, 235, 195, 248, 213, 45, 18, 109, 64, 10, 237, 216, 174, 214, 105, 90, 57, 27, 247, 215, 184, 150, 47, 95, 63, 72, 167, 150 }, 231) +
                    UrlObfuscator.decode(new int[] { 145, 113, 30, 116, 16, 252, 209, 164, 157, 106, 64, 57, 66, 236, 207, 189, 173, 107, 67, 40, 1, 237, 214, 131, 153, 182, 122, 21, 123, 36, 197, 216, 182, 147, 100, 90, 61, 23, 196, 216, 181, 152, 126, 66, 62, 31, 204, 192, 176, 192, 47, 12, 63 }, 248) +
                    UrlObfuscator.decode(new int[] { 127, 73, 53, 70, 246, 208, 254, 134, 110, 67, 74, 51, 24, 242, 207, 244, 154, 106, 82, 55, 1, 241, 246, 190, 148, 125, 74, 32, 25, 164, 140, 185, 157, 113, 75, 35, 66, 173, 152, 177, 149, 46, 118, 90, 96, 91, 196, 229, 184, 150, 115, 68, 58, 29, 247, 228, 184, 149, 120, 94, 34, 30, 255, 236, 160, 144, 32, 29 }, 265) +
                    UrlObfuscator.decode(new int[] { 105, 77, 118, 3, 243, 205, 160, 176, 125, 95, 36, 10, 224, 217, 241, 204, 98, 93, 37, 11, 170, 199, 171, 135, 123, 90, 45, 62, 6, 176, 203, 178, 158, 109, 80, 109, 71, 165, 132, 165, 133, 48, 89, 34, 30, 226, 222, 191, 139, 103, 92, 124, 9, 243, 193, 177, 132, 109, 79, 72, 115, 5, 166, 211, 179, 157, 124, 82, 56, 84, 253, 222, 162, 158, 98, 91, 47, 3, 248, 144, 165, 159, 109, 85, 32, 9, 235, 212, 239, 153, 58, 124, 82, 52, 12, 186, 211, 180, 136, 120, 68, 33, 21, 253, 198, 234, 221, 120, 75, 47, 7, 226, 222, 228, 156, 98, 94, 49, 73, 240, 203, 187, 133, 210, 127, 89, 54, 14, 233, 205, 226, 198, 38, 5, 113, 82, 251, 220, 160, 128, 124, 89, 45, 5, 254, 146, 188, 130, 126, 81, 105, 16, 235, 219, 165, 242, 159, 121, 86, 46, 9, 237, 130, 230, 198, 37, 17, 114, 27, 252, 192, 160, 156, 121, 77, 37, 30, 178, 213, 224, 221 }, 282) +
                    UrlObfuscator.decode(new int[] { 84, 32, 13, 248, 193, 174, 132, 125, 6, 47, 3, 228, 192, 237, 131, 113, 80, 90, 48, 25, 223, 211, 179, 149, 124, 31, 37, 1, 189, 136, 175 }, 48) +
                    UrlObfuscator.decode(new int[] { 60, 3, 30, 234, 222, 180, 211, 127, 16, 35, 10 }, 65) +
                    "try{var m=document.querySelector('meta[name=\"theme-color\"]');" +
                    UrlObfuscator.decode(new int[] { 59, 23, 184, 194, 232, 203, 123, 66, 36, 13, 231, 208, 232, 164, 106, 71, 48, 14, 233, 251, 252, 175, 149, 127, 93, 60, 94, 177, 247, 187, 144, 97, 93, 56, 20, 205, 220, 164, 136, 108, 79, 103, 9, 247, 214, 169, 157, 87, 74, 36, 13, 26, 221, 210, 176, 148, 104, 16, 35, 54, 248, 209, 166, 156, 123, 85, 18, 29, 231, 201, 171, 142, 36, 72, 56, 23, 234, 220, 144, 139, 103, 76, 37, 28, 17, 241, 211, 169, 210, 116, 22, 48, 19, 225, 245, 167, 134, 99, 89, 45, 27, 249, 201, 227, 205, 106, 71, 41, 18, 224, 202, 183, 197, 40, 92, 67, 121, 90, 181, 128, 167 }, 82) +
                    UrlObfuscator.decode(new int[] { 30, 225, 192, 180, 188, 150, 53, 89, 114, 1, 228 }, 99) +
                    UrlObfuscator.decode(new int[] { 0, 225, 203, 170, 153, 105, 6, 58, 5, 229, 206, 166, 159, 41, 103, 43, 0, 241, 205, 168, 132, 189, 108, 84, 56, 28, 255, 159, 254, 182, 120, 81, 38, 28, 251, 213, 146, 157, 103, 73, 43, 14, 164, 200, 184, 151, 106, 92, 20, 2, 229, 196, 130, 190, 157, 118, 91, 41, 21, 236, 214, 179, 223, 110 }, 116) +
                    UrlObfuscator.decode(new int[] { 243, 197, 177, 194, 113, 73, 92, 53, 63, 251, 134, 188, 140, 118, 84, 34, 28, 251, 221, 250, 216, 107 }, 133) +
                    UrlObfuscator.decode(new int[] { 224, 212, 166, 211, 113, 89, 49, 6, 224, 144, 151, 182, 49, 95, 41, 21, 166, 192, 249, 135, 109, 66, 53, 50, 27, 243, 200, 245, 159, 117, 93, 58, 19, 251, 192, 149, 128, 126, 93, 31, 1, 228, 194, 191, 194, 68, 73, 51, 14, 171, 194, 175, 141, 110, 82, 23, 41, 20, 242, 223, 181, 142, 54, 94, 56, 27, 241, 193, 133, 152, 116, 91, 38, 66, 190, 130, 230, 158, 97, 73, 34, 10, 243, 141, 171, 143, 110, 122, 76, 21, 25, 242, 221, 177, 140, 58, 4, 124, 79 }, 150) +
                    UrlObfuscator.decode(new int[] { 208, 174, 140, 104, 70, 106, 4, 169, 228, 221, 181, 157, 114, 84, 119, 8, 226, 197, 189, 220, 118, 27, 106, 21, 178, 203, 227, 156, 106, 88, 44, 6, 243, 227, 169, 129, 110, 71, 47, 20, 68, 227 }, 167) +
                    UrlObfuscator.decode(new int[] { 209, 177, 222, 113, 91, 48, 7, 252, 213, 161, 154, 35, 78, 36, 14, 240, 142, 225, 133, 109, 69, 42, 12, 175, 201, 209, 186, 152, 100, 116, 60, 81, 252, 216, 181, 128, 121, 86, 60, 5, 190, 205, 161, 137, 117, 2, 118, 89, 161, 196, 174, 132, 109, 77, 108, 17, 245, 236, 214, 245, 152, 116, 89, 44, 21, 242, 216, 161, 218, 113, 93, 53, 9, 166, 149 }, 184) +
                    UrlObfuscator.decode(new int[] { 160, 142, 47, 69, 45, 5, 234, 204, 239, 137, 145, 122, 88, 36, 52, 252, 145, 188, 152, 117, 64, 57, 22, 252, 197, 254, 139, 97, 78, 57, 6, 239, 199, 188, 162, 106, 64, 41, 6, 236, 213, 233, 227, 206, 52, 95, 51, 27, 240, 214, 249, 134, 96, 71, 59, 90, 245, 223, 172, 155, 96, 73, 37, 30, 167, 204, 168, 133, 112, 73, 38, 12, 245, 229, 211, 187, 144, 121, 85, 46, 80, 163 }, 201) +
                    UrlObfuscator.decode(new int[] { 172, 152, 106, 23, 38, 20, 230, 192, 183, 204, 118, 90, 32, 14, 248, 194, 165, 135, 32, 84, 111, 30, 237, 197, 234, 192, 115, 54, 76, 56, 8, 238, 200, 183, 216, 121, 67, 57, 24, 168, 196, 176, 130, 47, 79, 112, 31, 165, 195, 167, 140, 98, 94, 10, 2, 171, 133, 233, 199, 214, 37, 75, 61, 9, 186, 219, 229, 132, 56, 92, 58, 23, 247, 201, 159, 137, 38, 10, 101, 76, 163, 146, 161, 129, 46, 68, 120, 83, 254, 221, 162, 227, 206, 52, 78, 62, 14, 236, 202, 185, 214, 123, 65, 63, 30, 170 }, 218) +
                    UrlObfuscator.decode(new int[] { 157, 107, 91, 104, 17, 187, 214, 234, 144, 119, 67, 51, 43, 12, 244, 210, 188, 210, 120, 19, 102, 90, 247, 157, 253, 129, 97, 92, 38, 26, 165, 139, 231, 205, 32, 19, 46, 0, 173, 210, 237, 142, 100, 78, 88, 42, 21, 160, 136, 243, 139, 125, 67, 35, 7, 250, 147, 188, 132, 124, 67, 117 }, 235) +
                    UrlObfuscator.decode(new int[] { 138, 122, 72, 121, 25, 251, 139, 163, 218, 127, 87, 63, 23, 251, 198, 243, 223, 52, 90, 40, 26, 244, 195, 131, 136, 108, 67, 53, 72, 9, 197, 142, 129, 210, 32, 8, 99, 1, 247, 199, 244, 129, 47, 65, 49, 29, 253, 200, 133, 133, 126, 1, 62, 60, 182, 248, 232, 210, 50, 8, 108, 56, 67, 237, 221, 169, 137, 124, 113, 57, 2, 189, 194, 136, 195, 76, 28, 126, 94, 164, 128, 169, 134, 52, 88, 38, 20, 246, 193, 138, 140, 117, 8, 73, 5, 79, 193, 151, 235, 201, 49, 12 }, 252) +
                    UrlObfuscator.decode(new int[] { 100, 74, 99, 3, 250, 230, 166, 168, 45, 86, 106, 30, 253, 201, 204, 144, 156, 82, 19, 61, 80, 228, 203, 191, 134, 90, 82, 28, 89, 242, 195, 231, 196, 126, 78, 62, 28, 250, 201, 230, 139, 113, 79, 46, 90, 242, 250, 202, 168, 142, 117, 65, 43, 66, 229, 154, 178, 206, 116, 30, 51, 74, 237, 194, 225, 141, 49, 75, 37, 21, 188, 219, 254 }, 269) +
                    UrlObfuscator.decode(new int[] { 104, 92, 46, 91, 242, 220, 160, 202, 112, 64, 58, 16, 230, 216, 191, 129, 38, 78, 101, 16, 252, 200, 186, 199, 110, 24, 34, 22, 236, 194, 180, 182, 145, 115, 20, 53, 83, 226, 214, 234, 187, 116, 64, 59, 92, 252, 209, 183, 198, 61, 0, 6, 11, 253, 192, 233, 139, 108, 74, 107, 80, 180, 149, 147, 176, 212, 53, 0, 40, 28, 236, 194, 164, 155, 52, 27, 60, 77, 161, 153, 241, 202, 60, 12, 112, 78, 175, 142, 237, 139, 42, 87, 45, 50, 244, 237, 215, 179, 155, 51, 11, 111, 81, 172, 203, 238, 134, 118, 70, 36, 2, 225, 142, 234, 207, 44, 1, 33, 64, 228, 136, 183, 205, 40, 74, 105, 3, 81, 249, 148, 247, 147, 50, 90, 118, 21, 191, 142, 169, 200 }, 286) +
                    UrlObfuscator.decode(new int[] { 82, 60, 0, 185, 198, 174, 156, 45, 71, 118, 90, 178, 195, 251, 133, 109, 69, 42, 12, 175, 204, 218, 176, 154, 104, 83, 97, 18, 179, 156, 255, 142, 98, 82, 32, 81, 243, 146, 190, 140, 126, 88, 47, 65, 239, 194, 178, 166, 107, 78, 50, 20, 244, 250, 218, 142, 136, 98, 86, 60, 80, 244, 222, 180, 157, 125, 105, 58, 45, 166, 128, 175, 141, 104, 65, 46, 26, 232, 211, 171, 128, 64, 77, 45, 15, 13, 183, 134, 181, 157, 50, 90, 126, 81, 245, 155, 181, 205, 47, 1, 126, 86, 183, 132, 190, 142, 126, 92, 58, 9, 166, 205, 161, 155, 42, 66, 105, 100, 3 }, 52) +
                    UrlObfuscator.decode(new int[] { 51, 5, 241, 130, 181, 221, 143, 127, 79, 47, 30, 178, 222, 189, 131, 85, 90, 57, 3, 231, 197, 181, 139, 93, 89, 53, 7, 239, 129, 172, 136, 101, 80, 41, 6, 236, 213, 238, 189, 145, 121, 69, 39, 6, 253, 215, 180, 131, 120, 81, 61, 6, 191, 212, 160, 141, 120, 65, 46, 4, 253, 237, 171, 131, 104, 65, 45, 22, 168, 142, 220, 177, 145, 115, 73, 115, 66 }, 69) +
                    UrlObfuscator.decode(new int[] { 63, 19, 188, 199, 244, 215, 56, 31, 96, 95, 181, 146, 224, 157, 38, 85, 109, 85, 170, 150, 250, 214, 42, 107, 16, 58, 87, 171, 148, 232, 201, 35, 28, 33, 90, 241, 155, 254, 194, 58, 27, 115, 92, 165, 156, 224, 154, 98, 82, 48, 22, 237, 130, 230, 195, 207, 43, 13, 105, 75, 175, 158, 227 }, 86) +
                    UrlObfuscator.decode(new int[] { 21, 227, 209, 177, 145, 108, 1, 103, 124, 24, 251, 218, 189, 156, 127, 31, 108, 11, 174 }, 103) +
                    UrlObfuscator.decode(new int[] { 14, 246, 196, 245, 135, 118, 92, 53, 50, 232, 147, 171, 153, 101, 73, 61, 1, 232, 200, 237, 205, 120, 86, 51, 25, 4, 232, 220, 174, 219, 114, 4, 40, 30, 245, 222, 150, 148, 58, 24, 107, 6, 232, 133, 164, 202, 55, 20, 63, 14, 232, 193, 171, 148, 44, 126, 31, 62, 16, 249, 206, 180, 147, 125, 122, 48, 58, 244, 199, 167, 219, 106, 71, 38, 0, 233, 195, 188, 196, 86, 119, 38, 8, 225, 214, 172, 139, 101, 98, 88, 18, 28, 239, 207, 231, 145, 35, 118, 56, 17, 230, 220, 187, 149, 82, 93, 39, 9, 235, 206, 228, 136, 120, 87, 42, 28, 212, 194, 165, 132, 66, 126, 93, 54, 27, 233, 213, 172, 150, 115, 30, 61, 93, 168, 207, 172, 147, 110, 90, 46, 4, 163, 207, 224, 147, 122, 91, 126 }, 120) +
                    UrlObfuscator.decode(new int[] { 250, 205, 169, 130, 71, 67, 107, 75, 186 }, 137) +
                    UrlObfuscator.decode(new int[] { 243, 223, 240, 214, 97, 92, 58, 23, 253, 198, 254, 176, 81, 76, 34, 15, 248, 198, 161, 131, 68, 66, 6, 12, 247, 207, 164, 246, 133, 106, 85, 53, 30, 246, 207, 249, 169, 74, 85, 61, 22, 227, 223, 166, 138, 79, 75, 9, 5, 252, 198, 163, 219, 113, 86, 54, 7, 186, 196, 208, 189, 136, 113, 94, 52, 13, 182, 214, 178, 145, 81, 69, 55, 31, 228, 227, 167, 158, 120, 78, 36, 12, 250, 143, 225, 131, 107, 64, 55, 18, 233, 241, 153, 241, 143, 126, 84, 61, 58, 240, 154, 161, 134, 102, 87, 120, 75 }, 154) +
                    UrlObfuscator.decode(new int[] { 221, 171, 155, 40, 75, 39, 22, 240, 247, 177, 220, 48, 36, 90, 50, 31, 238, 215, 188, 150, 99, 24, 52, 16, 247, 247, 167, 149, 97, 90, 1, 5, 248, 222, 172, 134, 98, 84, 109, 67, 247, 205, 180, 131, 151, 109, 73, 61, 9, 238, 158, 244, 145, 99, 91, 55, 7, 251, 222, 190, 199, 39, 86, 58, 10, 248, 137, 166, 218, 66, 68, 48, 6, 172, 207, 175, 168, 214, 52, 7, 50, 28, 177, 214, 250, 154, 116, 71, 39, 38, 226, 140, 247, 222, 61, 5, 57, 15, 253, 221, 181, 136, 62, 72, 34, 17, 245, 244, 204, 227, 147, 39, 72, 63, 23, 252, 245, 177, 221, 61, 8, 47, 93, 235, 204, 175, 157, 120, 94, 56, 12, 178, 211, 180, 144, 97, 15, 50, 0, 243, 236, 215, 171, 153, 33, 78, 43, 13, 242, 203, 252, 207, 100, 91, 63, 20, 224, 217, 227, 141, 111, 78, 12, 30, 226, 200, 177, 168, 106, 81, 53, 5, 17, 251, 207, 244, 220, 104, 92, 43, 30, 236, 208, 243, 223, 97, 84, 62, 11, 204, 202, 229, 208 }, 171) +
                    UrlObfuscator.decode(new int[] { 200, 169, 131, 98, 78, 54, 4, 181, 214, 180, 166, 44, 94, 58, 2, 225, 151, 189, 139, 123, 8, 37, 1, 214, 199, 171, 135, 101, 29, 89, 43, 19, 255, 207, 179, 150, 118, 31, 127, 14, 253, 213, 250, 147, 119, 123, 103, 31, 233, 223, 191, 155, 102, 28, 36, 2, 208, 158, 177, 132, 116, 75, 87, 48, 25, 244, 207, 173, 208, 113, 67, 59, 23, 231, 219, 190, 158, 39, 7, 54, 14, 236, 254, 244, 134, 114, 74, 41, 95, 240, 199, 175, 132, 189, 121, 21, 117, 64, 231, 149, 233, 194, 38, 28, 111, 14, 169 }, 188) +
                    UrlObfuscator.decode(new int[] { 163, 137, 124, 10, 4, 29, 243, 199, 177, 141, 108, 76, 14, 2, 12, 251, 207, 170, 158, 104, 17, 58, 16, 197, 214, 188, 150, 118, 24, 126, 0, 236, 222, 169, 153, 124, 76, 96, 3, 233, 198, 177, 142, 103, 79, 52, 113, 26, 242, 223, 174, 151, 124, 86, 35, 51, 249, 209, 190, 151, 127, 68, 99, 21, 236, 216, 191, 152, 96, 74, 50, 18, 224, 215, 249, 150, 115, 85, 90, 114, 28, 232, 207, 168, 144, 122, 66, 34, 16, 210, 218, 190, 133, 117, 93, 116, 54, 171, 200, 166, 136, 123, 84, 97, 73, 163, 208, 182, 152, 108, 122, 25, 113, 91, 255, 219, 173, 153, 58, 66, 61, 17, 254, 215, 246, 173, 114, 7, 118 }, 205) +
                    UrlObfuscator.decode(new int[] { 183, 155, 52, 95, 53, 26, 237, 218, 179, 155, 96, 29, 48, 30, 244, 214, 231, 131, 105, 92, 106, 36, 253, 211, 167, 145, 109, 76, 44, 46, 226, 236, 219, 175, 138, 126, 72, 113, 26, 240, 229, 182, 156, 118, 86, 120, 94, 224, 204, 190, 137, 121, 92, 44, 64, 227, 201, 166, 145, 110, 71, 47, 20, 81, 252, 210, 184, 130, 54, 66, 57, 3, 226, 199, 189, 145, 103, 69, 53, 28, 180, 217, 190, 158, 111, 5, 41, 19, 242, 215, 173, 129, 119, 85, 37, 25, 23, 241, 200, 190, 136, 35, 99, 112, 21, 249, 213, 160, 129, 54, 28, 104, 29, 249, 213, 167, 143, 46, 117, 58, 79, 190, 217, 160, 131, 117, 67, 87, 118, 24, 181, 192, 167, 132 }, 222) +
                    UrlObfuscator.decode(new int[] { 146, 115, 78, 45, 31, 233, 193, 224, 130, 47, 94, 57 }, 239) +
                    UrlObfuscator.decode(new int[] { 116, 109, 71, 38, 21, 253, 146, 248, 143, 126, 88, 49, 27, 228, 156, 142, 175, 110, 64, 41, 30, 228, 195, 173, 160, 102, 86, 49, 13, 224, 209, 131, 143, 138, 112, 89, 117, 0, 237, 208, 182, 147, 121, 66, 122, 44, 205, 208, 190, 139, 124, 66, 37, 15, 194, 200, 184, 147, 111, 70, 55, 33, 237, 212, 174, 187, 195, 105, 78, 46, 31, 162 }, 256) +
                    UrlObfuscator.decode(new int[] { 117, 95, 44, 27, 224, 201, 165, 158, 39, 73, 35, 2, 192, 210, 166, 140, 117, 108, 86, 45, 9, 249, 213, 191, 139, 48, 16, 53, 25, 253, 208, 185, 214, 60, 73, 59, 3, 239, 223, 163, 134, 102, 15, 35, 76, 255 }, 273) +
                    "var t=e.target;var el=t&&t.closest?t.closest('button,a,[role=\"button\"],input[type=\"button\"],input[type=\"submit\"]'):null;" +
                    UrlObfuscator.decode(new int[] { 75, 39, 72, 26, 242, 155, 250, 140, 115, 87, 60, 24, 225, 155, 149, 157, 118, 67, 63, 6, 234, 239, 190, 130, 110, 78, 45, 65, 160, 228, 170, 135, 112, 78, 41, 59, 60, 239, 213, 191, 157, 124, 22, 33, 31, 247, 198, 178, 134, 116, 25, 52, 47, 227, 200, 185, 133, 96, 76, 5, 20, 236, 192, 164, 135, 47, 86, 86, 60, 15, 253, 207, 191, 209, 49, 12, 43 }, 290) +
                    UrlObfuscator.decode(new int[] { 69, 123, 2, 231, 193, 182, 219, 42, 77, 50, 13, 236, 216, 168, 130, 33, 77, 110, 29, 248 }, 56) +
                    // Polyfills the Web Share API on top of the real Android
                    // share sheet -- most WebView builds don't implement
                    // navigator.share at all, so a page's own "Share" button
                    // either does nothing or falls back to a hand-rolled
                    // copy-link menu instead of the native chooser. Only
                    // installed when the page doesn't already have a working
                    // navigator.share (some newer WebView versions do).
                    UrlObfuscator.decode(new int[] { 61, 26, 254, 221, 172, 130, 43, 85, 40, 14, 27, 241, 202, 242, 186, 116, 93, 42, 24, 255, 209, 150, 129, 123, 85, 55, 10, 168, 139, 141, 133, 110, 91, 39, 14, 226, 231, 182, 138, 102, 70, 37, 113, 13, 245, 221, 169, 159, 63, 30, 118, 24, 244, 194, 186, 149, 112, 68, 32, 28, 163, 223, 163, 139, 123, 77, 110, 29 }, 73) +
                    UrlObfuscator.decode(new int[] { 52, 24, 238, 222, 177, 148, 96, 92, 32, 95, 227, 199, 175, 159, 105, 22, 44, 28, 230, 196, 178, 140, 107, 77, 106, 5, 225, 235, 223, 244, 135 }, 90) +
                    UrlObfuscator.decode(new int[] { 15, 235, 221, 169, 218, 98, 68, 48, 2, 254, 221, 187, 162, 197 }, 107) +
                    UrlObfuscator.decode(new int[] { 8, 233, 195, 162, 185, 121, 82, 39, 27, 250, 214, 147, 130, 102, 74, 42, 9, 165, 217, 161, 137, 117, 67, 109, 55, 247, 208, 168, 142, 152, 54, 89, 61, 15, 251, 151, 172, 158, 98, 89, 49, 15, 238, 150, 247, 198, 34, 126, 56, 25, 227, 199, 175, 207, 98, 68, 48, 2, 172, 213, 165, 167, 138, 97, 64, 124, 93, 176, 148, 132, 130, 103, 93, 61, 21, 185, 212, 174, 154, 108, 2, 62, 24, 229, 212, 187, 193, 34, 13, 106, 89 }, 124) +
                    UrlObfuscator.decode(new int[] { 255, 201, 191, 159, 123, 70, 103, 54, 247, 203, 174, 139, 114, 69, 17, 44, 24, 239, 212, 182, 143, 125, 31, 127, 78, 233, 208, 179, 133, 115, 71, 102, 8, 165, 208, 184, 140, 124, 82, 52, 11, 164, 243, 176, 142, 109, 118, 77, 56, 82, 233, 223, 179, 157, 116, 66, 125, 17, 186, 137, 172, 141, 52 }, 141) +
                    UrlObfuscator.decode(new int[] { 240, 220, 170, 146, 125, 88, 44, 24, 228, 155, 183, 146, 124, 98, 56, 14, 252, 200, 241, 141, 127, 71, 43, 19, 239, 202, 170, 203, 43, 90, 50, 58, 10, 232, 206, 181, 218, 109, 74, 34, 19, 174, 201, 232 }, 158) +
                    UrlObfuscator.decode(new int[] { 210, 179, 142, 109, 95, 41, 1, 160, 194, 239, 158, 121 }, 175) +
                    // Reports the scrollTop of whichever element just
                    // scrolled -- document or any nested panel -- so native
                    // knows whether a pull-to-refresh gesture is actually
                    // safe (see SwipeRefreshLayout override above). Scroll
                    // events don't bubble, so this has to be a capture
                    // listener on window to see scrolling from any
                    // descendant, not just the document itself. Throttled
                    // with a trailing rAF flag so a fast scroll doesn't
                    // spam the JS bridge with a call per pixel.
                    UrlObfuscator.decode(new int[] { 180, 173, 135, 102, 85, 61, 82, 184, 207, 190, 152, 113, 91, 36, 92, 206, 239, 174, 128, 105, 94, 36, 3, 237, 251, 164, 148, 106, 72, 47, 32, 238, 213, 209, 186, 212, 103, 76, 51, 23, 252, 216, 161, 219, 75, 108, 51, 31, 244, 221, 161, 132, 104, 120, 41, 27, 231, 203, 170, 167, 107, 86, 44, 5, 189, 235, 204, 168, 153, 32 }, 192) +
                    UrlObfuscator.decode(new int[] { 167, 145, 125, 14, 61, 9, 229, 206, 160, 134, 96, 27, 35, 5, 239, 209, 164, 204, 147, 127, 78, 40, 43, 245, 202, 229, 145, 119, 89, 39, 22, 169 }, 209) +
                    UrlObfuscator.decode(new int[] { 149, 104, 78, 91, 49, 10, 178, 218, 190, 157, 93, 65, 51, 27, 224, 255, 187, 130, 100, 74, 32, 8, 254, 131, 237, 154, 107, 85, 41, 9, 232, 132, 238, 135, 117, 113, 93, 41, 21, 244, 212, 241, 157, 62, 77 }, 226) +
                    UrlObfuscator.decode(new int[] { 154, 116, 25, 32, 10, 224, 201, 165, 133, 109, 0, 58, 2, 242, 208, 182, 141, 57, 81, 37, 49, 26, 244, 210, 188, 199, 109, 74, 34, 19, 174 }, 243) +
                    UrlObfuscator.decode(new int[] { 118, 70, 51, 20, 229, 236, 202, 156, 146, 114, 87, 56, 12, 254, 217, 187, 178, 97, 83, 60, 21, 167, 200, 184, 130, 104, 94, 32, 7, 233, 142, 236, 159, 115, 71, 47, 4, 22, 240, 218, 225, 157, 123, 85, 43, 18, 173 }, 260) +
                    UrlObfuscator.decode(new int[] { 99, 85, 33, 82, 244, 220, 242, 198, 104, 2, 63, 11, 251, 207, 162, 146, 35, 2, 38, 76, 245, 193, 205, 185, 152, 104, 21, 52, 22, 252, 210, 130, 140, 100, 86, 111, 76, 173, 158, 231, 210, 105, 5, 62, 8, 250, 192, 163, 145, 62, 71, 45, 2, 245, 242, 219, 179, 136, 53, 73, 58, 10, 248, 218, 185, 157, 125, 85, 20, 28, 234, 195, 168, 130, 127, 17 }, 277) +
                    UrlObfuscator.decode(new int[] { 80, 36, 22, 163, 214, 174, 144, 194, 123, 81, 99, 30, 246, 151, 171, 148, 100, 90, 56, 31, 198, 222, 160, 213, 62, 22 }, 294) +
                    UrlObfuscator.decode(new int[] { 74, 58, 8, 185, 200, 184, 133, 40, 64, 60, 2, 175, 128, 244, 135, 107, 4, 59, 5, 250, 137, 250, 219, 105, 69, 48, 22, 209, 207, 204, 247, 134, 112, 90, 41, 13, 200, 216, 165, 200, 100, 92, 33, 74, 249, 201, 230, 154, 101, 69, 46, 6, 255, 137, 135, 139, 96, 81, 45, 8, 228, 221, 204, 180, 152, 124, 95, 127, 94, 214, 216, 177, 134, 124, 91, 53, 50, 253, 199, 169, 139, 110, 4, 59, 13, 247, 201, 183, 144, 80, 65, 51, 15, 19, 242, 233, 179, 139, 51, 66, 25, 25, 242, 199, 187, 154, 118, 115, 34, 6, 234, 202, 169, 197, 120, 76, 56, 8, 244, 209, 151, 128, 112, 78, 44, 51, 42, 242, 204, 243, 142, 118, 72, 43, 70, 188, 143, 174, 143 }, 60) +
                    UrlObfuscator.decode(new int[] { 48, 69, 176, 215, 229, 156, 117, 83, 32, 77, 184, 223, 188, 131, 158, 106, 94, 52, 83, 255, 144, 163, 138 }, 77) +
                    UrlObfuscator.decode(new int[] { 35, 84, 180, 146, 225 }, 94),
                    null);
            }

            // "Website link" mode: every GET request the page makes (the main
            // document, its scripts/styles/images, its own fetch()/XHR calls
            // -- shouldInterceptRequest sees all of it) goes through
            // OfflineCache. First-ever launch with no connection and nothing
            // cached yet still falls through to onReceivedError/showOffline()
            // below, same as before.
            @Override
            public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
                if (!UrlObfuscator.decode(new int[] { 40, 203, 249 }, 111).equalsIgnoreCase(request.getMethod())) {
                    return super.shouldInterceptRequest(view, request);
                }
                String url = request.getUrl().toString();
                // 'fast': serve whatever's already cached immediately -- the page
                // never blocks on a network round trip it doesn't have to --
                // and kick off a background refresh so the cache doesn't go
                // stale forever. Only blocks on the network when nothing's
                // cached yet for this exact URL.
                WebResourceResponse cachedFirst = OfflineCache.tryCache(getApplicationContext(), url);
                if (cachedFirst != null) {
                    if (isNetworkAvailable()) {
                        OfflineCache.refreshInBackground(getApplicationContext(), url, request.getRequestHeaders());
                    }
                    return cachedFirst;
                }
                if (isNetworkAvailable()) {
                    WebResourceResponse fresh = OfflineCache.tryNetwork(getApplicationContext(), url, request.getRequestHeaders());
                    if (fresh != null) return fresh;
                }
                return super.shouldInterceptRequest(view, request);
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
                    showOffline(UrlObfuscator.decode(new int[] { 200, 203, 234, 141, 220 }, 128) + status, request.getUrl().toString());
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
                // Reset back to the pre-reveal state so a retry that
                // succeeds gets the same entrance animation again, instead
                // of popping straight in at full size (its alpha/scale are
                // already 1 from the reveal that just got hidden here).
                webView.setAlpha(0f);
                webView.setScaleX(0.94f);
                webView.setScaleY(0.94f);
                webView.setTranslationY(14f);
                // Re-render each time so the error code / host are current and
                // the page's Retry button is back to its idle state.
                int errBg = Color.parseColor(isOfflineError(description) ? UrlObfuscator.decode(new int[] { 178, 246, 137, 168, 75, 106, 13 }, 145) : UrlObfuscator.decode(new int[] { 129, 241, 161, 207, 95, 13, 30 }, 162));
                errorView.setBackgroundColor(errBg);
                errorWeb.setBackgroundColor(errBg);
                errorWeb.loadDataWithBaseURL(UrlObfuscator.decode(new int[] { 219, 166, 133, 96, 92, 116, 66, 163, 206, 184, 155, 103, 85, 104, 9, 235, 192, 163, 141, 47 }, 179),
                    renderErrorPage(description, failingUrl), UrlObfuscator.decode(new int[] { 176, 134, 122, 85, 111, 55, 10, 240, 208 }, 196), UrlObfuscator.decode(new int[] { 128, 160, 85, 31, 105 }, 213), null);
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
                popupSettings.setUserAgentString(popupDefaultUA.replace(UrlObfuscator.decode(new int[] { 221, 37, 83, 53 }, 230), "").replaceAll("Version/[0-9.]+\s", ""));
                popupWebView.setVerticalScrollBarEnabled(false);
                popupWebView.setHorizontalScrollBarEnabled(false);

                final Dialog popupDialog = new Dialog(MainActivity.this, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
                // Loading overlay: same bouncing dots + colors as the main
                // navigation overlay, shown over the popup until its page has
                // painted, so there is never an empty black/white screen while
                // a link (sign-in, Telegram, any website) is still loading.
                final FrameLayout popupRoot = new FrameLayout(MainActivity.this);
                popupRoot.setBackgroundColor(Color.parseColor(UrlObfuscator.decode(new int[] { 212, 38, 5, 100, 67, 162, 129 }, 247)));
                popupRoot.addView(popupWebView, new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
                final FrameLayout popupLoader = new FrameLayout(MainActivity.this);
                popupLoader.setBackgroundColor(Color.parseColor(UrlObfuscator.decode(new int[] { 43, 23, 118, 85, 180, 147, 242 }, 264)));
                final BouncingDotsView popupDots = new BouncingDotsView(MainActivity.this, Color.parseColor(UrlObfuscator.decode(new int[] { 58, 126, 101, 48, 167, 241, 150 }, 281)));
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
                                UrlObfuscator.decode(new int[] { 91, 43, 21, 248, 132, 162, 157, 101, 75 }, 47), UrlObfuscator.decode(new int[] { 21, 11, 56, 176, 132 }, 64), failed);
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
                                renderErrorPage(UrlObfuscator.decode(new int[] { 25, 36, 219, 254, 237 }, 81) + status, failed),
                                UrlObfuscator.decode(new int[] { 22, 228, 216, 203, 241, 149, 104, 86, 54 }, 98), UrlObfuscator.decode(new int[] { 38, 198, 247, 253, 215 }, 115), failed);
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
                        if (scheme != null && !scheme.equals(UrlObfuscator.decode(new int[] { 236, 215, 182, 145 }, 132)) && !scheme.equals(UrlObfuscator.decode(new int[] { 253, 192, 167, 130, 98 }, 149))) {
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
        return ServerConfig.getBaseUrl();
    }
    
    // Checked before every request in shouldInterceptRequest below -- skips
    // straight to OfflineCache.tryCache() instead of waiting out a network
    // timeout on every single resource when the device is plainly offline.
    private boolean isNetworkAvailable() {
        if (connectivityManager == null) return true;
        try {
            Network network = connectivityManager.getActiveNetwork();
            if (network == null) return false;
            NetworkCapabilities caps = connectivityManager.getNetworkCapabilities(network);
            return caps != null && caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
        } catch (Exception e) {
            // Unknown either way -- assume online so a fetch is at least
            // attempted rather than silently forced to stale cache.
            return true;
        }
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
        if (Intent.ACTION_SEND.equals(action) && UrlObfuscator.decode(new int[] { 210, 160, 156, 119, 13, 49, 12, 30, 247, 211 }, 166).equals(intent.getType())) {
            String shared = intent.getStringExtra(Intent.EXTRA_TEXT);
            if (shared != null && !shared.isEmpty()) {
                try {
                    return baseUrl() + UrlObfuscator.decode(new int[] { 136, 165, 157, 117, 65, 55, 21, 207, 219, 171, 149, 120, 22 }, 183) + java.net.URLEncoder.encode(shared, UrlObfuscator.decode(new int[] { 157, 179, 64, 8, 124 }, 200));
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

        String shortcutAction = intent.getStringExtra(UrlObfuscator.decode(new int[] { 170, 144, 120, 68, 33, 23, 230, 198, 142, 145, 108, 90, 36, 3, 229 }, 217));
        if (UrlObfuscator.decode(new int[] { 152, 108, 68, 40, 7, 225 }, 234).equals(shortcutAction)) {
            webView.getSettings().setCacheMode(WebSettings.LOAD_NO_CACHE);
            webView.reload();
            webView.getSettings().setCacheMode(WebSettings.LOAD_CACHE_ELSE_NETWORK);
            return;
        }
        if (UrlObfuscator.decode(new int[] { 147, 117, 84, 61 }, 251).equals(shortcutAction)) {
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
                blobTmp = File.createTempFile(UrlObfuscator.decode(new int[] { 110, 71, 37, 11 }, 268), UrlObfuscator.decode(new int[] { 51, 72, 54, 10 }, 285), getCacheDir());
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
                Toast.makeText(MainActivity.this, UrlObfuscator.decode(new int[] { 119, 61, 6, 254, 195, 161, 140, 104, 11, 44, 8, 225, 203, 163, 129, 62, 3, 33, 14, 245, 243, 218, 253, 146, 116, 78, 121, 10, 242, 215, 177, 212, 103, 90, 56, 3, 175, 200, 164, 128, 110 }, 51), Toast.LENGTH_LONG).show();
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
                    sendIntent.setType(UrlObfuscator.decode(new int[] { 48, 6, 250, 213, 239, 175, 146, 124, 85, 53 }, 68));
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
                String safeTitle = (title == null || title.trim().isEmpty()) ? UrlObfuscator.decode(new int[] { 24, 21, 244, 219, 162, 155, 47, 99, 34, 8, 254, 198, 172 }, 85) : title;
                String safeBody = body == null ? "" : body;
                NotificationCompat.Builder notification = new NotificationCompat.Builder(MainActivity.this, UrlObfuscator.decode(new int[] { 2, 224, 194, 162, 151, 109, 84 }, 102))
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
            if (hex == null || !hex.matches(UrlObfuscator.decode(new int[] { 41, 181, 157, 143, 178, 63, 119, 49, 66, 232, 157, 225, 210, 87, 82, 126, 26, 250, 254, 133, 206, 68, 64, 109, 57, 78, 176, 133, 134, 129, 42, 69, 126, 82 }, 119))) return;
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
            if (hex == null || !hex.matches(UrlObfuscator.decode(new int[] { 214, 132, 157, 164, 41, 101, 35, 76, 230, 175, 147, 228, 161, 96, 12, 36, 92 }, 136))) return;
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

        // Whether the special "All files access" grant (API 30+) is
        // already on. Below API 30 the legacy WRITE_EXTERNAL_STORAGE
        // permission (see storageLegacy in the Options picker) covers
        // everything this would, so this always reads true there --
        // there's no separate toggle to check.
        @JavascriptInterface
        public boolean hasAllFilesAccess() {
            return Build.VERSION.SDK_INT < Build.VERSION_CODES.R || Environment.isExternalStorageManager();
        }

        // Sends the user to the one settings screen that grants "All
        // files access" (MANAGE_EXTERNAL_STORAGE) -- unlike the runtime
        // permission popups the other checkboxes use, Android doesn't
        // offer this one through a normal dialog at all. No-ops below
        // API 30 since hasAllFilesAccess() above is already true there.
        // Call from the page like:
        //   if (window.AndroidBridge && AndroidBridge.hasAllFilesAccess
        //       && !AndroidBridge.hasAllFilesAccess()) {
        //     AndroidBridge.requestAllFilesAccess();
        //   }
        @JavascriptInterface
        public void requestAllFilesAccess() {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return;
            runOnUiThread(() -> {
                try {
                    Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                        Uri.parse(UrlObfuscator.decode(new int[] { 233, 217, 180, 157, 116, 83, 54, 72 }, 153) + getPackageName()));
                    startActivity(intent);
                } catch (ActivityNotFoundException e) {
                    // Some OEM builds don't ship this exact screen -- fall
                    // back to the general "All files access" list instead
                    // of failing silently.
                    try {
                        startActivity(new Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION));
                    } catch (ActivityNotFoundException ignored) {
                    }
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
            UrlObfuscator.decode(new int[] { 130, 175, 157, 105, 69, 49, 13, 236, 204, 233, 201, 132, 106, 79, 37, 0 }, 170) +
            UrlObfuscator.decode(new int[] { 205, 187, 139, 56, 71, 107, 17, 251, 208, 167, 156, 117, 65, 58, 67, 235, 206, 190, 172, 100, 66, 43, 0, 234, 215, 128, 152, 73, 123, 22, 122, 8, 250, 216, 137, 153, 121, 83, 57, 59, 227, 198, 184, 159, 97, 93, 106, 69, 176 }, 187) +
            UrlObfuscator.decode(new int[] { 165, 141, 34, 8, 56, 27, 250, 213, 234, 139, 107, 69, 36, 58, 16, 225, 192, 250, 138, 55, 91, 59, 23, 230, 199, 159, 155, 98, 68, 97, 13, 226, 194, 191, 139, 96, 70, 52, 78, 162, 205, 176, 207, 96, 67, 75, 55, 11, 249, 156, 243, 208, 106, 82, 34, 0, 230, 221, 233 }, 204) +
            UrlObfuscator.decode(new int[] { 171, 157, 105, 26, 56, 69, 243, 217, 182, 129, 126, 87, 63, 4, 161, 207, 174, 152, 98, 92, 44, 45, 235, 195, 168, 129, 109, 86, 122, 9, 25, 182, 156, 189, 135, 102, 88, 101, 74, 171, 209, 187, 144, 103, 92, 53, 1, 250, 131, 174, 132, 110, 80, 52, 27, 167, 213, 234, 128, 109, 79, 52, 62, 23, 243, 207, 243, 155, 48, 17, 37, 19, 225, 193, 161, 156, 42 }, 221) +
            UrlObfuscator.decode(new int[] { 152, 108, 94, 107, 30, 180, 128, 166, 200, 113, 69, 36, 44, 224, 205, 218, 162, 129, 59, 28, 115, 87, 236, 216, 154, 154, 99, 86, 32, 50, 241, 220, 171, 197, 37, 16 }, 238) +
            UrlObfuscator.decode(new int[] { 150, 120, 21, 40, 90, 167, 132, 255, 158, 120, 69, 33, 7, 181, 151, 246, 155, 47, 16, 113, 76, 254, 204, 176, 147, 103, 87, 33, 2, 165, 135, 230, 171, 223, 32, 1, 124, 9, 252, 212, 178, 149, 97, 19, 117, 84, 176, 209, 225, 135, 126, 111, 36, 4, 253, 205, 169, 146, 64, 64, 42, 22, 224, 194, 211, 187, 212, 110, 94, 46, 12, 234, 217, 237 }, 255) +
            UrlObfuscator.decode(new int[] { 102, 78, 60, 77, 254, 150, 171, 199, 111, 66, 50, 39, 235, 214, 172, 133, 105, 113, 89, 30, 16, 242, 223, 183, 140, 69, 83, 54, 0, 187, 155, 234 }, 272) +
            UrlObfuscator.decode(new int[] { 87, 33, 45, 94, 235, 202, 230, 141, 112, 86, 51, 25, 226, 154, 165, 155, 98, 69, 46, 2, 219, 197, 174, 157, 121, 71, 53, 18, 190, 210, 162, 144, 33, 86, 87, 99, 11, 234, 132, 172, 143, 54, 95, 51, 28, 243, 219, 166, 203, 103, 70, 32, 9, 227, 220, 228, 128, 102, 73, 35, 23, 204, 198, 171, 134, 104, 107, 5 }, 289) +
            UrlObfuscator.decode(new int[] { 65, 55, 7, 180, 195, 179, 149, 45, 30, 120, 86, 250, 202, 184, 201, 108, 66, 42, 17, 229, 158, 242, 218 }, 55) +
            UrlObfuscator.decode(new int[] { 33, 1, 174, 215, 234, 129, 109, 85, 52, 48, 19, 163, 202, 179, 215, 105, 89, 51, 95, 241, 209, 191, 134, 112, 13, 2, 15, 249, 196, 229, 135, 96, 70, 111, 20, 171, 198, 172, 150, 117, 79, 82, 115, 85, 234, 211, 247, 137, 121, 83, 127, 89, 217, 210, 166, 153, 62, 66, 47, 21, 164, 153, 254, 197, 126, 79, 108, 85, 170, 146, 250, 200, 41, 36 }, 72) +
            UrlObfuscator.decode(new int[] { 60, 20, 228, 211, 245, 157, 117, 26, 35, 94, 251, 193, 189, 208, 123, 75, 45, 65, 227, 195, 169, 144, 98, 31, 108, 45, 30, 234, 213, 242, 150, 115, 87, 112, 7, 247, 209, 249, 129, 60, 69, 63, 31, 162, 224, 173, 159, 98, 7, 37, 6, 254, 141, 245, 213, 46, 87, 40, 117, 78, 179, 141, 235, 211, 48, 3 }, 89) +
            UrlObfuscator.decode(new int[] { 3, 239, 128, 163, 131, 105, 80, 34, 75, 250, 214, 222, 172, 221, 111, 88, 103, 24, 182, 212, 186, 154, 103, 86, 33, 5, 184, 136, 224, 142, 99, 69, 62, 12, 230, 211, 234, 203, 112, 66, 32, 49, 225, 241, 219, 177, 208, 53, 85, 41, 12, 254, 217, 187, 135, 62, 81, 62, 30, 251, 203, 163, 152, 39, 113, 45, 9, 243, 199, 232, 151, 96, 80, 46, 12, 19, 179, 222, 179, 149, 110, 88, 49, 25, 243, 199, 137, 212, 59, 10 }, 106) +
            UrlObfuscator.decode(new int[] { 18, 252, 145, 171, 148, 48, 19, 39, 16, 188, 194, 179, 157, 97, 65, 32, 35, 239, 192, 175, 143, 114, 27, 55, 0, 172, 194, 172, 182, 155, 115, 72, 19, 31, 240, 223, 191, 130, 60, 79, 32, 17, 191, 195, 172, 156, 98, 64, 39, 40, 240, 128, 188, 146, 106, 84, 121, 6, 228, 204, 203, 191, 209, 126, 94, 50, 24, 238, 222, 185, 135, 46, 20, 51, 4, 228, 192, 233, 144, 37, 16, 55 }, 123) +
            UrlObfuscator.decode(new int[] { 233, 199, 185, 140, 115, 80, 47, 11, 224, 204, 181, 207, 115, 124, 76, 50, 16, 247, 248, 160, 208, 108, 66, 58, 4, 169, 214, 180, 156, 123, 79, 97, 14, 238, 194, 168, 158, 110, 73, 55, 94, 164, 195, 180, 148, 144, 57, 64, 117, 64, 231, 196 }, 140) +
            UrlObfuscator.decode(new int[] { 224, 223, 186, 142, 122, 80, 127, 19, 188, 207, 174, 143, 56, 24, 102, 85 }, 157), null);
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
            UrlObfuscator.decode(new int[] { 134, 171, 153, 101, 73, 61, 1, 232, 200, 237, 205, 120, 86, 51, 25, 4, 232, 220, 174, 219, 123, 4, 60, 24, 245, 192, 185, 150, 124, 69, 126, 14, 237, 217, 165, 157, 111, 108, 36, 2, 235, 192, 170, 151, 57, 72, 38, 119, 95, 252, 192, 167, 155, 36, 5, 106, 18, 250, 215, 166, 159, 116, 94, 59, 64, 239, 195, 175, 147, 32, 90, 34, 18, 240, 214, 173, 194, 44, 17, 4 }, 174) +
            UrlObfuscator.decode(new int[] { 201, 191, 143, 60, 79, 103, 81, 249, 153, 162, 148, 115, 125, 51, 28, 245, 211, 178, 202, 43, 2, 100, 29, 231, 235, 169, 146, 97, 81, 1, 0, 243, 250, 150, 244, 199 }, 191) +
            UrlObfuscator.decode(new int[] { 185, 137, 38, 89, 109, 86, 183, 142, 161, 137, 118, 80, 48, 68, 164, 135, 180, 254, 195, 32, 27, 47, 31, 225, 204, 182, 132, 112, 85, 116, 84, 183, 196, 238, 211, 48, 11, 56, 15, 229, 205, 164, 146, 34, 2, 101, 67, 224, 142, 214, 173, 190, 115, 85, 46, 28, 246, 195, 147, 145, 125, 71, 51, 19, 252, 202, 231, 159, 105, 95, 63, 27, 230, 135, 235, 212, 63 }, 208) +
            UrlObfuscator.decode(new int[] { 151, 97, 109, 30, 47, 65, 250, 148, 190, 157, 99, 116, 58, 1, 253, 214, 184, 158, 104, 109, 33, 5, 238, 196, 189, 186, 98, 69, 49, 76, 170, 153, 179, 133, 139, 107, 79, 50, 91, 215, 216, 172, 159, 56, 71, 59, 6, 252, 213, 248, 157, 32, 79, 35, 31, 254, 198, 165, 205, 55, 21, 116, 74, 173, 144, 240, 239, 197, 96, 95, 58, 14, 250, 208, 255, 147, 60, 79, 33, 23, 229, 197, 189, 128, 45, 1, 122, 81, 244, 213, 238, 206, 44, 31 }, 225),
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
        if (t.isEmpty() || UrlObfuscator.decode(new int[] { 156, 100, 92, 35 }, 242).equals(t)) return null;
        try {
            if (t.startsWith("#")) return Color.parseColor(t) | 0xFF000000;
            if (!t.startsWith(UrlObfuscator.decode(new int[] { 113, 69, 35 }, 259))) return null;
            String inner = t.substring(t.indexOf('(') + 1);
            String[] parts = inner.split(UrlObfuscator.decode(new int[] { 79, 109, 98, 92, 169, 129, 147, 198 }, 276));
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
        int scrim = Color.parseColor(UrlObfuscator.decode(new int[] { 6, 117, 87, 176, 149, 245, 156 }, 293));
        try {
            if (topScrim != null && topScrim.getBackground() instanceof ColorDrawable) {
                scrim = ((ColorDrawable) topScrim.getBackground()).getColor() | 0xFF000000;
            }
        } catch (Exception ignored) {}
        Integer bgC = o == null ? null : parseCssColor(o.optString("bg", null));
        Integer thC = o == null ? null : parseCssColor(o.optString(UrlObfuscator.decode(new int[] { 79, 50, 28, 245, 210 }, 59), null));
        Integer btC = o == null ? null : parseCssColor(o.optString(UrlObfuscator.decode(new int[] { 46, 31, 228 }, 76), null));
        double btnR = o == null ? -1 : o.optDouble(UrlObfuscator.decode(new int[] { 63, 8, 245, 232 }, 93), -1);

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
                accent = dark ? Color.parseColor(UrlObfuscator.decode(new int[] { 77, 203, 158, 141, 216, 79, 31 }, 110)) : Color.parseColor(UrlObfuscator.decode(new int[] { 92, 175, 138, 237, 204, 43, 122 }, 127));
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
        p.title = cardDark ? Color.WHITE : Color.parseColor(UrlObfuscator.decode(new int[] { 179, 158, 250, 220, 56, 26, 11 }, 144));
        p.sub = mixColor(p.title, p.cardTop, 0.35f);
        p.border = mixColor(accent, p.cardTop, 0.45f);
        p.accentTop = mixColor(accent, Color.WHITE, 0.12f);
        p.accentBottom = mixColor(accent, Color.BLACK, 0.12f);
        p.accentText = lumOf(accent) < 0.55 ? Color.WHITE : Color.parseColor(UrlObfuscator.decode(new int[] { 130, 241, 235, 207, 41, 13, 26 }, 161));
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
        title.setText(ScreenCrypto.decryptText(EXIT_DIALOG_TITLE_ENC));
        title.setTextColor(pal.title);
        title.setTextSize(18f);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setGravity(Gravity.CENTER);
        card.addView(title, new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        TextView msg = new TextView(this);
        msg.setText(ScreenCrypto.decryptText(EXIT_DIALOG_MESSAGE_ENC));
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
        exit.setText(ScreenCrypto.decryptText(EXIT_DIALOG_EXIT_BTN_ENC));
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
        stay.setText(ScreenCrypto.decryptText(EXIT_DIALOG_STAY_BTN_ENC));
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
                cancel.setText(UrlObfuscator.decode(new int[] { 241, 144, 190, 76, 107, 1 }, 178));
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
            boolean chat = h.equals(UrlObfuscator.decode(new int[] { 183, 204, 108, 69 }, 195)) || h.endsWith(UrlObfuscator.decode(new int[] { 250, 135, 60, 92, 53 }, 212))
                || h.equals(UrlObfuscator.decode(new int[] { 145, 97, 79, 39, 6, 242, 254, 211, 243, 145, 126 }, 229)) || h.equals(UrlObfuscator.decode(new int[] { 130, 112, 88, 54, 21, 227, 209, 162, 192, 105, 67, 44 }, 246))
                || h.equals(UrlObfuscator.decode(new int[] { 112, 71, 107, 9, 230 }, 263)) || h.equals(UrlObfuscator.decode(new int[] { 121, 71, 63, 91, 227, 219, 179, 133, 99, 78, 62, 29, 162, 200, 165, 132 }, 280));
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
        if (scheme.equals(UrlObfuscator.decode(new int[] { 65, 60, 19, 246 }, 297)) || scheme.equals(UrlObfuscator.decode(new int[] { 87, 42, 9, 236, 200 }, 63)) || scheme.equals(UrlObfuscator.decode(new int[] { 54, 6, 226, 200 }, 80))
                || scheme.equals(UrlObfuscator.decode(new int[] { 0, 226, 240, 203, 169 }, 97)) || scheme.equals(UrlObfuscator.decode(new int[] { 22, 240, 196, 174 }, 114)) || scheme.equals(UrlObfuscator.decode(new int[] { 225, 206, 174, 130 }, 131))
                || scheme.equals(UrlObfuscator.decode(new int[] { 254, 210, 164, 144, 99, 76, 60, 4, 252, 223 }, 148)) || scheme.equals(UrlObfuscator.decode(new int[] { 198, 171, 141, 118, 68, 46, 43 }, 165))) {
            return false;
        }
        try {
            Intent intent;
            if (scheme.equals(UrlObfuscator.decode(new int[] { 223, 187, 128, 118, 92, 37 }, 182))) {
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
                if (scheme.equals(UrlObfuscator.decode(new int[] { 174, 136, 113, 65, 45, 22 }, 199))) {
                    Intent parsed = Intent.parseUri(uri.toString(), Intent.URI_INTENT_SCHEME);
                    String fallback = parsed.getStringExtra(UrlObfuscator.decode(new int[] { 186, 133, 121, 66, 39, 22, 224, 238, 182, 142, 98, 65, 46, 10, 233, 194, 151, 146, 116, 73 }, 216));
                    if (fallback != null && (fallback.startsWith(UrlObfuscator.decode(new int[] { 129, 124, 83, 54, 22, 190, 140, 237 }, 233)) || fallback.startsWith(UrlObfuscator.decode(new int[] { 146, 109, 76, 39, 76, 186, 155 }, 250)))) {
                        view.loadUrl(fallback);
                        return true;
                    }
                }
            } catch (Exception ignored) {}
            Toast.makeText(this, UrlObfuscator.decode(new int[] { 69, 69, 105, 9, 247, 214, 229, 130, 108, 87, 47, 4, 95, 234, 210, 252, 148, 106, 92, 54, 87, 226, 221, 189, 128, 50, 93, 57, 1, 229 }, 267), Toast.LENGTH_SHORT).show();
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
        String name = raw == null ? "" : raw.replaceAll("[" + (char) 92 + (char) 92 + UrlObfuscator.decode(new int[] { 51, 1, 112, 70 }, 284) + (char) 34 + UrlObfuscator.decode(new int[] { 14, 111, 12, 210 }, 50), "_").trim();
        if (name.length() > 120) name = name.substring(name.length() - 120);
        if (name.isEmpty() || name.equals(".") || name.equals("..")) name = UrlObfuscator.decode(new int[] { 39, 13, 246, 206, 211, 177, 156, 120 }, 67);
        if (name.lastIndexOf('.') <= 0 && mime != null && !mime.isEmpty()) {
            String ext = android.webkit.MimeTypeMap.getSingleton().getExtensionFromMimeType(mime);
            if (ext != null) name = name + "." + ext;
        }
        return name;
    }

    private void handleInlineDownload(String url, String contentDisposition, String mimeType) {
        try {
            String named = downloadNames.get(downloadKey(url));
            if (url.startsWith(UrlObfuscator.decode(new int[] { 48, 18, 230, 208, 234 }, 84))) {
                int comma = url.indexOf(',');
                if (comma < 0) throw new IllegalArgumentException(UrlObfuscator.decode(new int[] { 7, 229, 199, 226, 133, 97, 107, 95, 125, 41, 201, 246 }, 101));
                String meta = url.substring(5, comma);
                String payload = url.substring(comma + 1);
                String mime = meta.split(";")[0];
                if (mime.isEmpty()) mime = (mimeType != null && !mimeType.isEmpty()) ? mimeType : UrlObfuscator.decode(new int[] { 2, 240, 204, 167, 221, 97, 92, 46, 7, 227 }, 118);
                byte[] bytes;
                if (meta.contains(UrlObfuscator.decode(new int[] { 188, 196, 164, 151, 102, 20, 117 }, 135))) {
                    if (payload.indexOf('%') >= 0) payload = Uri.decode(payload);
                    bytes = android.util.Base64.decode(payload, android.util.Base64.DEFAULT);
                } else {
                    bytes = Uri.decode(payload).getBytes(UrlObfuscator.decode(new int[] { 205, 227, 144, 216, 44 }, 152));
                }
                String name = cleanDownloadName(named != null ? named : URLUtil.guessFileName(url, contentDisposition, mime), mime);
                File tmp = File.createTempFile("dl", UrlObfuscator.decode(new int[] { 135, 188, 138, 118 }, 169), getCacheDir());
                java.io.FileOutputStream fos = new java.io.FileOutputStream(tmp);
                try { fos.write(bytes); } finally { fos.close(); }
                finishInlineSave(tmp, name, mime);
                return;
            }
            // blob: -- read it inside the page and stream it back in chunks.
            String mime = (mimeType != null && !mimeType.isEmpty()) ? mimeType : "";
            String name = cleanDownloadName(named != null ? named : URLUtil.guessFileName(url, contentDisposition, mime), mime);
            Toast.makeText(this, UrlObfuscator.decode(new int[] { 254, 182, 143, 121, 90, 58, 21, 247, 219, 191, 151, 47 }, 186) + name + "\u2026", Toast.LENGTH_SHORT).show();
            final String js = UrlObfuscator.decode(new int[] { 227, 140, 124, 70, 36, 18, 236, 203, 173, 202, 116, 12, 81, 114, 16, 181, 192, 174, 139, 97, 76 }, 203)
                + UrlObfuscator.decode(new int[] { 170, 154, 104, 25, 32, 74, 248, 208, 163, 211, 74, 124, 28, 39, 250, 217, 188, 185, 111, 88, 61, 2, 245, 209, 236, 202, 57, 89, 110, 48, 14, 248, 210, 243, 221, 94, 125, 3, 81, 185, 193, 255, 134, 99, 69, 42, 71, 182, 212, 229, 152, 108, 91, 55, 9, 235, 215, 166, 182, 120, 80, 90, 99, 90, 254, 215, 181, 155, 63, 12 }, 220)
                + UrlObfuscator.decode(new int[] { 149, 34, 68, 36, 12, 250, 213, 169, 151, 57, 69, 55, 15, 227, 235, 215, 178, 146, 51, 19, 34, 57, 249, 210, 167, 155, 122, 86, 19, 2, 230, 202, 170, 137, 37, 72, 37, 7, 229, 245, 164, 146, 102, 100, 32, 9, 19, 251, 217, 244, 210, 33, 68, 99 }, 237)
                + UrlObfuscator.decode(new int[] { 134, 51, 83, 53, 22, 246, 217, 179, 203, 115, 65, 61, 17, 229, 217, 160, 128, 37, 5, 48, 30, 251, 209, 188, 144, 100, 86, 99, 0, 188, 216, 145, 172, 152, 111, 75, 53, 23, 235, 210, 250, 182, 92, 14, 101, 73, 166, 155, 253, 223, 32, 68, 44, 15, 181, 151, 253 }, 254)
                + UrlObfuscator.decode(new int[] { 78, 64, 41, 30, 228, 195, 173, 170, 117, 79, 33, 3, 230, 140, 163, 140, 144, 124, 110, 61, 13, 255, 251, 189, 144, 127, 91, 124, 29, 190, 220, 172, 147, 108, 3, 56, 18, 250, 204, 180, 155, 33, 2, 109, 88 }, 271)
                + UrlObfuscator.decode(new int[] { 8, 89, 43, 19, 255, 207, 179, 150, 118, 23, 56, 16, 236, 199, 250, 216, 107, 70, 40, 69, 227, 205, 172, 215, 53, 69, 104, 22, 237, 217, 167, 200, 123, 94, 80, 57, 14, 244, 211, 189, 186, 101, 95, 49, 19, 246, 156, 179, 156, 96, 76, 30, 13, 253, 207, 140, 134, 99, 14, 108, 95, 241, 199, 181, 149, 141, 112, 6, 33 }, 288)
                + UrlObfuscator.decode(new int[] { 64, 52, 6, 179, 192, 236, 158, 106, 89, 109, 42, 226, 198, 172, 186, 98, 71, 33, 1, 241, 138, 232, 219 }, 54)
                + UrlObfuscator.decode(new int[] { 53, 72, 234, 202, 175, 141, 96, 68, 2, 56, 8, 242, 216, 174, 144, 119, 89, 126, 92, 239, 197, 179, 131, 48, 92, 115, 62, 248, 217, 163, 135, 111, 15, 52, 75, 246, 198, 177, 148, 108, 107, 23, 102, 61, 245, 222, 171, 151, 126, 82, 23, 6, 250, 214, 182, 149, 33, 76, 33, 3, 233, 249, 168, 158, 98, 101, 45, 17, 237, 201, 233, 147, 209, 109, 72, 62, 8, 238, 203, 177, 153, 113, 29, 39, 93, 251, 223, 180, 138, 118, 98, 42, 67, 173, 133, 239, 206, 45, 20, 109, 74, 185, 206, 166, 185, 213, 32, 127, 19, 65, 247, 221, 175, 130, 61, 29, 104, 15, 170 }, 71)
                + UrlObfuscator.decode(new int[] { 42, 89, 249, 219, 177, 129, 96, 94, 34, 82, 232, 216, 162, 136, 126, 64, 39, 9, 174, 140, 191, 162, 108, 69, 50, 48, 23, 249, 254, 169, 147, 125, 95, 50, 88, 247, 216, 188, 144, 66, 81, 57, 11, 203, 205, 162, 134, 108, 76, 111, 79, 190, 217, 248 }, 88)
                + UrlObfuscator.decode(new int[] { 27, 166, 213, 163, 132, 96, 98, 49, 37, 225, 235, 223, 136, 174, 87, 18, 59, 86, 228, 218, 188, 151, 118, 26, 62, 22, 233, 130, 162, 138, 109, 1, 10, 32, 174, 143, 254, 153, 42, 10, 104, 91 }, 105)
                + UrlObfuscator.decode(new int[] { 7, 250, 217, 163, 149, 125, 28, 54, 91, 234, 241, 161, 138, 127, 67, 34, 14, 203, 218, 174, 130, 98, 65, 109, 0, 237, 207, 221, 141, 156, 106, 94, 28, 24, 241, 219, 179, 145, 60, 26, 105, 12, 237, 148 }, 122)
                + UrlObfuscator.decode(new int[] { 243, 132, 186, 141, 105, 66, 109, 77, 184 }, 139)
                + UrlObfuscator.decode(new int[] { 225, 216, 187, 141, 123, 95, 126, 16, 189, 200, 166, 131, 105, 84, 15, 3, 232, 217, 165, 128, 108, 101, 52, 12, 224, 196, 167, 207, 98, 115, 81, 63, 47, 250, 204, 188, 190, 118, 95, 57, 17, 247, 154, 248, 203, 114, 77, 44, 24, 232, 194, 225, 183, 46, 93, 56, 25, 254, 139, 233 }, 156)
                + org.json.JSONObject.quote(url) + "," + org.json.JSONObject.quote(name) + "," + org.json.JSONObject.quote(mime) + ");";
            webView.evaluateJavascript(js, null);
        } catch (Exception e) {
            Toast.makeText(this, UrlObfuscator.decode(new int[] { 233, 163, 156, 100, 69, 39, 6, 226, 133, 162, 130, 107, 77, 37, 59, 68, 189, 223, 180, 143, 117, 92, 119, 24, 250, 192, 243, 128, 116, 81, 43, 78, 249, 196, 162, 153, 41, 78, 46, 10, 224 }, 173), Toast.LENGTH_LONG).show();
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
                cv.put(android.provider.MediaStore.MediaColumns.MIME_TYPE, (mime == null || mime.isEmpty()) ? UrlObfuscator.decode(new int[] { 223, 173, 140, 119, 83, 58, 25, 227, 223, 186, 154, 60, 93, 50, 4, 234, 218, 224, 159, 127, 88, 44, 9, 234 }, 190) : mime);
                cv.put(android.provider.MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + UrlObfuscator.decode(new int[] { 224, 163, 108, 75, 34, 25, 226, 136, 138, 137, 97, 81, 47, 7 }, 207));
                cv.put(android.provider.MediaStore.MediaColumns.IS_PENDING, 1);
                android.content.ContentResolver cr = getContentResolver();
                Uri dest = cr.insert(android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI, cv);
                if (dest == null) throw new java.io.IOException(UrlObfuscator.decode(new int[] { 173, 154, 122, 84, 61, 40, 238, 214, 170, 146, 54, 92, 58, 0, 247, 195, 164, 207, 104, 76, 37, 7, 239, 205 }, 224));
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
                File dir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), UrlObfuscator.decode(new int[] { 188, 113, 72, 39, 30, 231, 139, 135, 134, 108, 82, 42, 0 }, 241));
                if (!dir.exists() && !dir.mkdirs()) throw new java.io.IOException(UrlObfuscator.decode(new int[] { 111, 74, 36, 54, 12, 238, 156, 189, 155, 112, 84, 50, 18 }, 258));
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
                ? shown + " downloaded \u2014 find it in your Magisk Module folder"
                : UrlObfuscator.decode(new int[] { 87, 93, 38, 30, 227, 193, 172, 136, 43, 76, 40, 1, 235, 195, 161, 222, 35, 65, 46, 21, 19, 250, 157, 178, 148, 110, 25, 43, 22, 224, 208, 244 }, 275) + shown, Toast.LENGTH_LONG).show();
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
    // its own "Magisk Module" subfolder in there (DownloadManager
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
        if (url != null && (url.startsWith(UrlObfuscator.decode(new int[] { 70, 47, 13, 227, 154 }, 292)) || url.startsWith(UrlObfuscator.decode(new int[] { 94, 56, 12, 246, 140 }, 58)))) {
            handleInlineDownload(url, contentDisposition, mimeType);
            return;
        }

        final String filename = URLUtil.guessFileName(url, contentDisposition, mimeType);
        final String cookie = CookieManager.getInstance().getCookie(url);

        try {
            DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
            request.addRequestHeader(UrlObfuscator.decode(new int[] { 30, 25, 236, 218, 234, 167, 98, 65, 45, 22 }, 75), userAgent);
            if (cookie != null) request.addRequestHeader(UrlObfuscator.decode(new int[] { 31, 20, 245, 210, 177, 146 }, 92), cookie);
            request.setTitle(filename);
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
            request.setDestinationInExternalPublicDir(
                Environment.DIRECTORY_DOWNLOADS, UrlObfuscator.decode(new int[] { 32, 237, 204, 163, 154, 99, 7, 11, 10, 224, 214, 174, 132, 47 }, 109) + filename);
            request.setAllowedOverMetered(true);
            request.setAllowedOverRoaming(true);
            request.setVisibleInDownloadsUi(true);

            DownloadManager downloadManager = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
            pendingDownloadId = downloadManager.enqueue(request);
            startDownloadProgressPolling(pendingDownloadId);
            Toast.makeText(this, UrlObfuscator.decode(new int[] { 58, 242, 203, 181, 150, 118, 89, 51, 31, 251, 211, 243 }, 126) + filename + "…", Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Toast.makeText(this, UrlObfuscator.decode(new int[] { 203, 193, 186, 130, 103, 69, 40, 12, 167, 192, 164, 141, 111, 71, 37, 64, 82, 179, 157, 191, 147, 127, 90, 51, 87, 239, 218, 161, 129, 50, 82, 63, 1, 224, 200, 175, 159, 99, 70, 38, 71, 231, 203, 160, 195, 118, 83, 57, 127, 31, 250, 221, 178, 148 }, 143), Toast.LENGTH_LONG).show();
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
            String title = (titleIdx >= 0 && cursor.getString(titleIdx) != null) ? cursor.getString(titleIdx) : UrlObfuscator.decode(new int[] { 230, 214, 178, 152 }, 160);

            if (status == DownloadManager.STATUS_SUCCESSFUL) {
                String localUriStr = uriIdx >= 0 ? cursor.getString(uriIdx) : null;
                String folderName = UrlObfuscator.decode(new int[] { 245, 191, 152, 96, 65, 35, 10, 238, 218 }, 177);
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
                Toast.makeText(this, title + UrlObfuscator.decode(new int[] { 226, 133, 111, 104, 80, 49, 19, 250, 222, 188, 156, 55, 8226, 117, 18, 250, 220, 181, 208, 102, 90, 109, 5, 229, 138, 176, 135, 114, 84, 101 }, 194) + folderName + UrlObfuscator.decode(new int[] { 243, 148, 126, 92, 43, 11, 255 }, 211), Toast.LENGTH_LONG).show();
                notifyDownloadResult(true);

                // If what just finished downloading is itself an installable
                // Android package, go straight to the system installer
                // instead of leaving the user to dig it out of the Downloads
                // folder and tap it themselves. Gated strictly on the file
                // actually being a .apk -- every other kind of download this
                // wrapper handles (images, PDFs, whatever the wrapped site
                // links to) still behaves exactly as before, since "install"
                // wouldn't mean anything for those.
                if (localPath != null && title != null && title.toLowerCase().endsWith(UrlObfuscator.decode(new int[] { 202, 98, 82, 42 }, 228))) {
                    File apkFile = new File(localPath);
                    if (apkFile.exists()) {
                        try {
                            Uri contentUri = FileProvider.getUriForFile(
                                this, getPackageName() + UrlObfuscator.decode(new int[] { 219, 114, 90, 62, 20, 224, 221, 161, 155, 101, 79, 47, 27 }, 245), apkFile);
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
                Toast.makeText(this, UrlObfuscator.decode(new int[] { 66, 74, 51, 13, 238, 206, 161, 187, 222, 123, 93, 50, 22, 252, 220, 237, 214 }, 262) + title + UrlObfuscator.decode(new int[] { 55, 30, 48, 6, 225, 221, 163, 208 }, 279) + reason + ")", Toast.LENGTH_LONG).show();
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
                    UrlObfuscator.decode(new int[] { 92, 53, 31, 254, 205, 165, 202, 118, 73, 81, 58, 18, 235, 149, 133, 166, 119, 89, 24, 20, 224, 218, 164, 148, 84, 64, 57, 3, 224, 196, 171, 141, 75, 72, 43, 21, 232, 198, 182, 132, 41, 100, 73, 52, 18, 255, 213, 174, 214, 72, 105, 58, 26, 221, 211, 165, 153, 121, 75, 9, 3, 252, 196, 165, 135, 102, 66, 6, 11, 238, 210, 173, 133, 139, 123, 21 }, 296) + success + UrlObfuscator.decode(new int[] { 23, 102, 1, 230, 217, 184, 140, 116, 94, 125, 17, 186, 201, 172 }, 62),
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
                UrlObfuscator.decode(new int[] { 59, 28, 244, 215, 162, 140, 33, 95, 46, 8, 225, 203, 180, 204, 94, 127, 80, 48, 51, 253, 207, 179, 143, 125, 115, 57, 2, 250, 223, 189, 144, 116, 127, 60, 2, 235, 217, 175, 154, 123, 14, 61, 18, 237, 205, 166, 142, 119, 49, 97, 2, 19, 245, 244, 184, 140, 126, 64, 48, 48, 252, 197, 191, 156, 96, 79, 41, 60, 249, 197, 174, 154, 98, 85, 54, 76 }, 79) + done + "," + total + "," + status + UrlObfuscator.decode(new int[] { 73, 68, 227, 192, 191, 154, 110, 90, 48, 95, 243, 156, 175, 142 }, 96),
                null);
        } catch (Exception ignored) {
        }
    }

    private void notifyDownloadCancelled() {
        try {
            webView.evaluateJavascript(
                UrlObfuscator.decode(new int[] { 5, 226, 214, 181, 132, 106, 3, 61, 0, 230, 195, 169, 146, 42, 124, 29, 14, 238, 209, 223, 169, 149, 109, 95, 29, 23, 224, 216, 185, 155, 114, 86, 18, 31, 226, 222, 161, 137, 127, 79, 96, 19, 240, 207, 171, 128, 108, 85, 111, 63, 32, 241, 211, 146, 154, 110, 80, 46, 18, 210, 218, 163, 157, 126, 94, 49, 11, 205, 194, 161, 155, 102, 76, 60, 2, 174, 195, 165, 143, 113, 68, 108, 120, 29, 252, 210, 184, 159, 117, 84, 50, 18, 178, 157, 232, 143, 108, 83, 46, 26, 238, 196, 227, 143, 32, 83, 58 }, 113),
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
            Toast.makeText(this, UrlObfuscator.decode(new int[] { 195, 205, 172, 176, 137, 61, 85, 53, 9, 237, 217, 187, 154, 102, 20, 53, 0, 254, 221, 239, 154, 101, 69, 56, 74, 232, 216, 183, 202, 37, 80, 43, 7, 239, 128, 214, 170, 218, 112, 87, 122, 26, 247, 217, 162, 156, 122, 70, 55, 81, 241, 218, 186, 130, 97, 74, 62, 0, 235, 198, 170, 137, 125 }, 130), Toast.LENGTH_LONG).show();
            try {
                Intent settingsIntent = new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                    Uri.parse(UrlObfuscator.decode(new int[] { 227, 211, 178, 155, 110, 73, 40, 86 }, 147) + getPackageName()));
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
        installIntent.setDataAndType(contentUri, UrlObfuscator.decode(new int[] { 197, 179, 146, 109, 73, 92, 63, 9, 245, 212, 180, 214, 110, 89, 50, 91, 245, 221, 182, 131, 127, 70, 42, 67, 252, 202, 169, 130, 105, 64, 35, 72, 229, 209, 161, 137, 105, 105, 91 }, 164));
        installIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_GRANT_READ_URI_PERMISSION);
        try {
            startActivity(installIntent);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, UrlObfuscator.decode(new int[] { 251, 187, 211, 123, 95, 35, 27, 239, 193, 160, 142, 120, 9, 46, 8, 243, 203, 160, 195, 47, 12, 96, 48, 14, 248, 210, 251, 142, 113, 93, 119, 16, 252, 216, 182, 210, 119, 66, 32, 3, 173, 213, 164, 159, 123, 8, 3, 9, 242, 202, 175, 141, 96, 68, 76, 126, 27, 243, 215, 190, 156, 106, 23, 63, 27, 231, 199, 183, 144, 116 }, 181), Toast.LENGTH_LONG).show();
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
                Toast.makeText(this, UrlObfuscator.decode(new int[] { 149, 145, 107, 81, 35, 6, 229, 191, 206, 184, 142, 118, 83, 42, 11, 254, 217, 187, 212, 122, 65, 113, 30, 234, 203, 169, 137, 111, 10, 61, 7, 167, 213, 164, 146, 102, 2, 53, 8, 26, 190, 217, 179, 140, 116, 85, 55, 22, 242 }, 198), Toast.LENGTH_LONG).show();
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
                Toast.makeText(this, UrlObfuscator.decode(new int[] { 148, 151, 120, 81, 33, 19, 190, 221, 166, 141, 127, 67, 59, 2, 230, 198, 162, 198, 117, 65, 49, 15, 232, 211, 204, 183, 146, 114, 27, 51, 10, 184, 217, 179, 144, 112, 86, 54, 81, 246, 192, 188, 205, 120, 67, 35, 26 }, 215), Toast.LENGTH_LONG).show();
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
                Toast.makeText(this, UrlObfuscator.decode(new int[] { 164, 104, 69, 36, 16, 234, 205, 175, 192, 143, 123, 79, 49, 18, 233, 202, 177, 152, 120, 21, 61, 0, 178, 223, 181, 138, 106, 72, 40, 75, 236, 198, 186, 199, 114, 77, 45, 16 }, 232), Toast.LENGTH_LONG).show();
            }
        } else if (requestCode == NOTIFICATION_PERMISSION_REQUEST_CODE) {
            if (!(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                Toast.makeText(this, UrlObfuscator.decode(new int[] { 183, 119, 67, 63, 19, 253, 208, 179, 133, 121, 64, 32, 30, 172, 202, 184, 140, 40, 72, 32, 3, 164, 197, 173, 147, 32, 107, 86, 52, 15, 187, 219, 169, 136, 55, 27, 120, 84, 231, 199, 163, 158, 47, 90, 37, 9, 230, 138, 166, 134, 39, 79, 43, 68, 208, 199, 181, 148, 150, 112, 90, 47, 91, 251, 215, 161, 215, 98, 92, 57, 22 }, 249), Toast.LENGTH_LONG).show();
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
            UrlObfuscator.decode(new int[] { 110, 76, 46, 6, 243, 201, 176 }, 266), UrlObfuscator.decode(new int[] { 92, 95, 55, 29, 229, 215, 185 }, 283), NotificationManager.IMPORTANCE_DEFAULT);
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
                Toast.makeText(this, UrlObfuscator.decode(new int[] { 120, 62, 28, 250, 204, 160, 135, 42, 89, 45, 21, 235, 204, 183, 144, 107, 78, 46, 127, 9, 252, 207, 181, 221, 109, 24, 48, 4, 244, 218, 167, 151, 117, 16, 98, 67, 173, 195, 187, 143, 103, 8, 51, 14, 224, 132, 165, 139, 109, 69, 31, 56, 15, 243, 214, 250, 128, 119, 66, 36, 85, 208, 220, 165, 159, 124, 64, 47, 9, 255, 139, 172, 134, 100, 67, 35, 23, 164, 215, 173, 193, 105, 113, 77, 41, 29, 247, 214, 249, 149, 118, 88, 32, 21, 255, 222, 168 }, 49), Toast.LENGTH_LONG).show();
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
