package com.generated.alexahub;

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
    // text below -- all AES-encrypted and compiled straight into
    // libscreennative.so (see native/screennative.c, written by
    // writeNativeScreenModule in server.js), so none of it sits as plain
    // text -- or even as Java bytecode -- in the APK at all; only this
    // native call crosses into classes.dex. Decrypted once, on first use,
    // and cached -- nativeDecryptScreen() does real work (AES) each call,
    // not something to repeat on every WebView error.
    static { System.loadLibrary(UrlObfuscator.decode(new int[] { 51, 60, 12, 248, 217, 181, 148, 120, 76, 62, 0, 240 }, 64)); }
    private static native String nativeDecryptScreen(int id);
    private static final int ERROR_PAGE_HTML_ID = 0;
    private static final int NO_INTERNET_PAGE_HTML_ID = 1;
    private static final int EXIT_DIALOG_TITLE_ID = 2;
    private static final int EXIT_DIALOG_MESSAGE_ID = 3;
    private static final int EXIT_DIALOG_EXIT_BTN_ID = 4;
    private static final int EXIT_DIALOG_STAY_BTN_ID = 5;
    private String errorPageHtmlCache;
    private String noInternetPageHtmlCache;
    private String getErrorPageHtml() {
        if (errorPageHtmlCache == null) errorPageHtmlCache = nativeDecryptScreen(ERROR_PAGE_HTML_ID);
        return errorPageHtmlCache;
    }
    private String getNoInternetPageHtml() {
        if (noInternetPageHtmlCache == null) noInternetPageHtmlCache = nativeDecryptScreen(NO_INTERNET_PAGE_HTML_ID);
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
        return d.contains(UrlObfuscator.decode(new int[] { 24, 62, 219, 235, 159, 162, 78, 126, 22, 44, 206, 245, 134, 171, 77, 108, 4, 35, 43, 219, 249 }, 81)) || d.contains(UrlObfuscator.decode(new int[] { 44, 192, 237, 250, 129, 179, 83, 111, 5, 43, 221, 228, 153, 185, 66, 118, 22 }, 98))
            || d.contains(UrlObfuscator.decode(new int[] { 61, 215, 229, 135, 160, 92, 102, 19, 40, 194, 232, 134, 160, 67, 97 }, 115)) || d.contains(UrlObfuscator.decode(new int[] { 199, 236, 140, 175, 69, 92, 106, 20, 51, 213, 229, 141, 177, 90, 115, 17, 43, 220, 231, 133 }, 132))
            || d.contains(UrlObfuscator.decode(new int[] { 212, 240, 151, 160, 84, 99, 28, 49, 216, 226, 153, 175, 72, 107, 15, 39, 199, 232, 134 }, 149)) || d.contains(UrlObfuscator.decode(new int[] { 246, 151, 171, 91, 123, 30, 35, 48, 208, 243, 153, 184, 78, 112, 23, 57, 201, 243, 149, 186, 94, 116, 20 }, 166));
    }

    // Friendly {title, message} for one failure ("HTTP 404", "ERR_...", ...).
    private String[] errorCopy(String code) {
        String c = code == null ? "" : code;
        if (c.startsWith(UrlObfuscator.decode(new int[] { 255, 130, 161, 68, 19 }, 183))) {
            int status = 0;
            try { status = Integer.parseInt(c.substring(5).trim()); } catch (Exception ignored) {}
            if (status == 404 || status == 410) {
                return new String[]{UrlObfuscator.decode(new int[] { 159, 130, 38, 70, 37, 13, 165, 213, 224, 185, 151, 115, 88, 123, 14, 241, 217, 163 }, 200),
                    UrlObfuscator.decode(new int[] { 142, 144, 118, 66, 117, 13, 252, 199, 246, 130, 106, 14, 33, 3, 228, 193, 160, 134, 96, 6, 35, 11, 241, 130, 168, 147, 145, 57, 73, 124, 19, 255, 203, 189, 217, 54, 124, 32, 83, 255, 208, 169, 207, 102, 76, 58, 14, 170, 203, 173, 130, 104, 5, 41, 12, 244, 196, 164, 255, 145, 111, 28, 41, 31, 244, 215, 161, 147, 113, 26 }, 217)};
            }
            if (status >= 500 || status == 408 || status == 429) {
                return new String[]{UrlObfuscator.decode(new int[] { 185, 102, 69, 34, 18, 237, 205, 173, 133, 33, 66, 77, 49, 22, 249 }, 234),
                    UrlObfuscator.decode(new int[] { 175, 114, 88, 44, 87, 249, 219, 177, 212, 97, 17, 63, 1, 174, 216, 191, 199, 42, 71, 39, 19, 166, 220, 171, 150, 44, 1, 25, 48, 11, 189, 223, 186, 148, 57, 72, 56, 29, 240, 148, 186, 134, 49, 81, 40, 15, 228, 194, 231, 202, 107, 93, 51, 70, 235, 203, 227, 146, 115, 79, 82, 55, 14, 249, 200, 244 }, 251)};
            }
            return new String[]{UrlObfuscator.decode(new int[] { 77, 72, 41, 12, 251, 212, 230, 129, 97, 77, 43, 4, 228 }, 268),
                UrlObfuscator.decode(new int[] { 68, 83, 46, 90, 253, 215, 185, 209, 97, 20, 59, 19, 231, 213, 239, 143, 110, 79, 46, 25, 250, 136, 179, 137, 37, 80, 43, 11, 242, 128, 205, 183, 154, 116, 79, 122, 23, 247, 192, 248 }, 285)};
        }
        return new String[]{UrlObfuscator.decode(new int[] { 96, 61, 28, 245, 219, 166, 132, 98, 76, 106, 30, 237, 201, 178, 197, 119, 74, 38, 4, 247, 254, 199, 174 }, 51),
            UrlObfuscator.decode(new int[] { 19, 6, 162, 194, 175, 170, 146, 121, 82, 124, 14, 185, 212, 184, 151, 113, 20, 39, 26, 248, 195, 239, 156, 100, 75, 35, 30, 169, 198, 168, 145, 43, 4, 26, 13, 244, 128, 220, 191, 147, 60, 75, 53, 18, 253, 151, 191, 129, 52, 82, 53, 16, 249, 193, 226, 205, 110, 94, 62, 73, 230, 200, 230, 149, 118, 76, 47, 8, 243, 250, 205, 243 }, 68)};
    }

    // Tells RESOURCE_ERROR_JS that a sub-resource failed, so a failed iframe can
    // swap in a small notice (iframes get no reliable error event of their own).
    private void reportFrameFailure(WebView view, String failedUrl) {
        try {
            view.evaluateJavascript(
                UrlObfuscator.decode(new int[] { 34, 29, 253, 214, 190, 135, 33, 113, 18, 24, 242, 236, 187, 137, 106, 67, 3, 5, 234, 206, 164, 132, 217, 56, 74, 53, 21, 254, 214, 175, 217, 73, 106, 32, 10, 212, 195, 177, 130, 107, 107, 45, 2, 230, 204, 172, 207 }, 85) + org.json.JSONObject.quote(failedUrl) + ")", null);
        } catch (Exception ignored) {}
    }

    // Picks and fills in the right error page for one specific failure.
    private String renderErrorPage(String description, String failingUrl) {
        if (isOfflineError(description)) {
            String appName = UrlObfuscator.decode(new int[] { 18, 237, 205, 176, 194, 96, 80, 79 }, 102);
            try {
                appName = getApplicationInfo().loadLabel(getPackageManager()).toString();
            } catch (Exception ignored) {}
            return getNoInternetPageHtml().replace(UrlObfuscator.decode(new int[] { 40, 201, 244, 132, 163, 77, 127, 17, 34, 203, 242, 147 }, 119), android.text.TextUtils.htmlEncode(appName));
        }
        String code = (description == null || description.trim().isEmpty() || UrlObfuscator.decode(new int[] { 230, 210, 170, 137 }, 136).equals(description))
            ? UrlObfuscator.decode(new int[] { 220, 234, 133, 169, 64, 122, 24, 60, 222, 231, 129 }, 153) : description.replace(UrlObfuscator.decode(new int[] { 196, 172, 156, 61, 28 }, 170), "").trim();
        // Raw codes (HTTP 404, net::ERR_...) and host names would give away that
        // this is a wrapped site, so the screen only shows an opaque reference
        // code. The real detail still goes to logcat for debugging.
        android.util.Log.w("AppError", code + " @ " + failingUrl);
        String shownCode;
        if (code.startsWith(UrlObfuscator.decode(new int[] { 243, 142, 173, 72, 23 }, 187))) {
            shownCode = "E" + code.substring(5).trim();
        } else {
            shownCode = String.format(java.util.Locale.US, UrlObfuscator.decode(new int[] { 137, 206, 58, 29, 16 }, 204), code.hashCode() & 0xFFFF);
        }
        String[] copy = errorCopy(code);
        return getErrorPageHtml()
            .replace(UrlObfuscator.decode(new int[] { 130, 163, 94, 104, 11, 39, 212, 249, 145, 177, 76, 109 }, 221), android.text.TextUtils.htmlEncode(shownCode))
            .replace(UrlObfuscator.decode(new int[] { 177, 82, 105, 25, 56, 214, 252, 142, 178, 73, 97, 28, 61 }, 238), android.text.TextUtils.htmlEncode(copy[0]))
            .replace(UrlObfuscator.decode(new int[] { 160, 65, 120, 14, 41, 197, 244, 139, 176, 73, 106 }, 255), android.text.TextUtils.htmlEncode(copy[1]))
            .replace(UrlObfuscator.decode(new int[] { 79, 112, 11, 63, 222, 244, 154, 187, 65, 106, 7, 55, 221, 252, 157 }, 272), UrlObfuscator.decode(new int[] { 113, 47, 52, 27, 189, 213, 175, 218, 120, 95, 54, 31, 251 }, 289))
            .replace(UrlObfuscator.decode(new int[] { 104, 9, 61, 219, 254, 151, 174, 69, 125, 2, 50, 211 }, 55), "");
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
        UrlObfuscator.decode(new int[] { 96, 1, 243, 203, 167, 151, 107, 78, 46, 119, 87, 230 }, 72) +
        UrlObfuscator.decode(new int[] { 45, 10, 238, 205, 188, 146, 59, 69, 56, 30, 235, 193, 186, 194, 127, 69, 57, 73, 186, 155, 178, 141, 109, 70, 46, 23, 86, 236, 216, 168, 142, 104, 87, 99, 10, 245, 212, 160, 144, 122, 25, 53, 70, 245, 223, 169, 159, 127, 91, 38, 92, 251 }, 89) +
        UrlObfuscator.decode(new int[] { 3, 239, 128, 176, 143, 107, 64, 44, 21, 175, 255, 224, 164, 146, 115, 86, 22, 22, 251, 220, 148, 154, 97, 93, 54, 88, 226, 202, 186, 152, 126, 69, 113, 30, 225, 201, 162, 138, 115, 13, 29, 62, 250, 240, 209, 176, 176, 116, 89, 50, 58, 248, 195, 187, 144, 46, 70, 35, 5, 234, 149 }, 106) +
        UrlObfuscator.decode(new int[] { 13, 251, 203, 248, 161, 70, 8, 115, 4, 251, 213, 164, 135, 51, 73, 41, 29, 227, 202, 173, 202, 113, 76, 32, 23, 234, 141, 224, 182, 144, 116, 72, 50, 27, 245, 149, 164, 149, 116, 88, 54, 79, 160, 158, 255, 194, 45, 65, 42, 18, 224, 197, 178, 139, 40, 87, 32, 3, 237, 197, 130, 239, 211, 44, 23, 122, 20, 241, 217, 191, 152, 97, 94, 127, 2, 243, 206, 162, 136, 49, 26, 100, 89, 164, 135, 179, 150, 97, 81, 111, 18, 227, 254, 210, 188, 158, 119, 95, 100, 22, 248, 145, 238 }, 123) +
        UrlObfuscator.decode(new int[] { 234, 222, 164, 138, 124, 78, 41, 11, 164, 207, 173, 130, 107, 73, 78, 117, 85, 224, 206, 171, 129, 108, 64, 52, 6, 179, 218, 236, 148, 96, 77, 56, 1, 238, 196, 189, 198, 111, 67, 36, 0, 255, 222, 165, 143, 156, 107, 80, 57, 21, 238, 151, 188, 152, 117, 64, 57, 22, 252, 197, 149, 131, 107, 64, 41, 5, 254, 146, 161, 129, 46, 4, 44, 74, 240, 196, 180, 170, 140, 115, 7 }, 140) +
        UrlObfuscator.decode(new int[] { 235, 221, 169, 218, 117, 5, 51, 25, 246, 193, 190, 151, 127, 68, 97, 31, 248, 201, 185, 147, 90, 77, 43, 3, 230, 208, 172, 144, 64, 76, 83, 118, 90, 241, 222, 174, 152, 67, 89, 55, 24, 241, 142, 164, 152, 117, 88, 62, 2, 254, 223, 151, 206, 33, 28 }, 157) +
        UrlObfuscator.decode(new int[] { 199, 171, 196, 42, 70, 103, 4, 226, 200, 162, 144, 107, 11, 58, 22, 30, 236, 157, 177, 198, 126, 86, 59, 2, 251, 208, 186, 135, 60, 82, 34, 10, 239, 217, 169, 174, 102, 76, 37, 2, 232, 209, 236, 196, 111, 68, 52, 62, 89, 180, 135, 182, 212, 106, 93, 35, 55, 225, 192, 161, 155, 115, 69, 59, 11, 165, 139, 165, 139, 100, 77, 96, 74, 162, 210, 170, 135, 118, 80, 80, 44, 9, 187, 146, 225, 148, 54, 68, 51, 1, 213, 199, 166, 131, 121, 77, 59, 25, 233, 131, 237, 138, 103, 73, 50, 0, 234, 215, 229, 205, 86, 79, 23, 102, 20, 181, 219, 169, 136, 114, 88, 49, 55, 251, 219, 189, 148, 39, 67, 100, 87, 249, 207, 189, 157, 117, 72, 126, 25 }, 174) +
        UrlObfuscator.decode(new int[] { 217, 177, 143, 52, 77, 59, 11, 184, 222, 235, 197, 47, 90, 110, 29, 190, 195, 171, 131, 107, 95, 34, 82, 225, 140, 237, 204, 127, 74, 36, 73, 236, 196, 215, 128, 210, 124, 95, 45, 57, 227, 194, 167, 157, 113, 71, 37, 21, 167, 137, 174, 131, 101, 94, 44, 6, 243, 129, 236, 197, 62, 31, 23, 48, 86, 242, 230, 181, 166, 52, 74, 61, 3, 215, 193, 160, 129, 123, 83, 37, 27, 235, 133, 235, 136, 101, 71, 60, 2, 232, 209, 227, 207, 84, 113, 105, 100, 3 }, 191) +
        UrlObfuscator.decode(new int[] { 173, 140, 111, 89, 47, 3, 162, 204, 225, 156, 123, 88 }, 208) +
        UrlObfuscator.decode(new int[] { 135, 117, 113, 93, 41, 21, 244, 212, 249, 148, 120, 85, 62, 55, 224, 193, 249, 217, 116, 90, 63, 21, 240, 195, 175, 192, 99, 73, 38, 17, 238, 199, 175, 148, 209, 121, 88, 40, 62, 246, 220, 181, 146, 120, 65, 22, 10, 219, 213, 248, 200, 81, 114, 45, 5, 238, 219, 167, 142, 98, 127, 43, 12, 239, 237, 175, 188, 149, 58, 21, 114, 8, 252, 204, 162, 132, 123, 15 }, 225) +
        UrlObfuscator.decode(new int[] { 132, 112, 66, 111, 6, 176, 200, 164, 137, 124, 69, 34, 8, 241, 138, 171, 135, 96, 68, 67, 34, 25, 243, 216, 175, 148, 125, 89, 34, 91, 240, 220, 177, 132, 125, 74, 32, 25, 201, 199, 175, 132, 109, 73, 50, 94, 237, 197, 234, 192, 104, 54, 76, 56, 8, 238, 200, 183, 195 }, 242) +
        UrlObfuscator.decode(new int[] { 117, 67, 51, 64, 12, 234, 128, 184, 148, 121, 76, 53, 18, 248, 193, 250, 144, 96, 84, 49, 27, 235, 232, 160, 142, 103, 76, 38, 19, 174, 130, 183, 151, 123, 77, 37, 120, 87, 166, 207, 175, 212, 112, 92, 106, 81, 202, 235, 178, 156, 117, 66, 32, 7, 233, 246, 164, 133, 100, 100, 40, 5, 238, 131, 248 }, 259) +
        UrlObfuscator.decode(new int[] { 103, 71, 124, 5, 245, 215, 186, 174, 99, 69, 62, 12, 230, 211, 251, 194, 62, 84, 42, 4, 242, 250, 150, 181, 136, 118, 86, 117, 26, 248, 210, 172, 221, 104, 70, 62, 5, 236, 198, 224, 141, 104, 94, 32, 7, 233, 156, 168, 133, 109, 75, 49, 21, 19, 255, 201, 181, 148, 116, 68, 3, 19, 247, 193, 181, 222, 115, 93, 60, 0, 249, 128, 182, 132, 101, 68, 21, 75, 168, 196, 168, 143, 109, 86, 109, 37, 17, 242, 209, 160, 142, 118, 77, 52, 30, 184, 213, 176, 134, 120, 95, 33, 84, 227, 195, 165, 143, 116, 64, 51, 11, 233, 136, 161, 141, 101, 89, 68, 115, 10, 249, 217, 177, 144, 108, 26, 34, 16, 236, 199, 255, 130, 121, 85, 43, 64, 237, 207, 160, 156, 123, 83, 124, 84, 180, 147, 231, 192, 105, 114, 78, 50, 14, 239, 219, 183, 140, 44, 66, 48, 12, 231, 159, 162, 153, 117, 75, 96, 13, 239, 192, 188, 155, 115, 28, 116, 84, 179, 135, 224, 137, 146, 110, 82, 46, 15, 251, 215, 172, 204, 107, 18, 111 }, 276) +
        UrlObfuscator.decode(new int[] { 77, 106, 2, 242, 209, 165, 177, 154, 94, 84, 50, 22, 253, 144, 164, 130, 60, 15, 46, 17, 240, 196, 172, 134, 37, 73, 98, 17, 244, 213 }, 293) +
        UrlObfuscator.decode(new int[] { 93, 47, 23, 251, 195, 191, 154, 122, 19, 59, 31, 249, 219, 230, 196, 119, 71, 37, 10, 227, 241, 182, 205, 45, 24, 46, 14, 227, 244, 253, 174, 143, 51, 19, 98 }, 59) +
        UrlObfuscator.decode(new int[] { 56, 25, 243, 210, 161, 129, 46, 65, 43, 0, 247, 204, 165, 177, 138, 51, 84, 62, 27, 253, 145, 172, 152, 112, 67, 115, 63, 228, 196, 174, 154, 100, 67, 37, 37, 235, 219, 162, 148, 115, 65, 49, 74, 231, 213, 209, 189, 137, 117, 84, 52, 81, 177, 204, 186, 154, 119, 88, 4, 1, 184, 134, 245, 129, 99, 72, 33, 42, 251, 212, 238, 204, 63, 94, 107 }, 76) +
        UrlObfuscator.decode(new int[] { 115, 19, 249, 201, 188, 138, 97, 83, 125, 16, 252, 209, 164, 157, 106, 64, 57, 66, 227, 207, 168, 140, 43, 93, 38, 12, 234, 206, 165, 172, 150, 109, 73, 102, 15, 232, 204, 189, 219, 101, 64, 54, 7, 224, 212, 181, 213, 122, 95, 57, 14, 166, 200, 188, 147, 116, 76, 38, 22, 246, 196, 179, 229, 138, 111, 73, 62, 86, 248, 204, 163, 132, 124, 86, 38, 6, 244, 246, 166, 130, 121, 73, 57, 80, 210, 143, 164, 137, 107, 80, 38, 12, 245, 135, 147, 249, 147, 125, 86, 63, 94, 197, 202, 255, 206, 105, 78, 49, 16, 228, 204, 166, 197, 105, 2, 49, 20, 245 }, 93) +
        UrlObfuscator.decode(new int[] { 26, 255, 213, 176, 131, 111, 0, 35, 9, 230, 209, 174, 135, 111, 84, 17, 54, 24, 253, 223, 243, 130, 113, 89, 63, 1, 188, 154, 233, 140, 117, 67, 61, 8, 247 }, 110) +
        UrlObfuscator.decode(new int[] { 9, 255, 207, 252, 159, 117, 87, 61, 74, 240, 212, 184, 128, 119, 10, 38, 14, 252, 141, 171, 132, 55, 79, 61, 9, 229, 209, 173, 140, 108, 9, 105, 36, 23, 251, 148, 191, 149, 119, 93, 126, 4, 240, 192, 166, 128, 127, 11, 43, 1, 227, 201, 246, 158, 123, 93, 34, 93, 236, 202, 170, 150, 41, 9, 4, 35, 70 }, 127) +
        UrlObfuscator.decode(new int[] { 249, 201, 230, 137, 99, 72, 63, 4, 237, 201, 178, 203, 96, 76, 33, 20, 237, 250, 208, 169, 185, 119, 95, 52, 29, 249, 194, 252, 143, 125, 87, 38, 80, 194, 219, 185, 141, 127, 67, 38, 6, 200, 196, 182, 129, 113, 84, 36, 18, 87, 248, 200, 178, 152, 110, 80, 55, 25, 190, 216, 248, 156, 59, 74, 57, 9, 166, 201, 163, 136, 127, 68, 45, 9, 242, 139, 172, 134, 99, 69, 105, 36, 17, 179, 216, 178, 137, 122, 87, 57, 24, 240, 215, 167, 218, 56, 11, 40, 1, 165, 133, 240, 151, 116, 1 }, 144) +
        UrlObfuscator.decode(new int[] { 143, 175, 189, 141, 120, 78, 45, 31, 177, 220, 184, 149, 96, 89, 54, 28, 229, 158, 171, 129, 110, 89, 38, 15, 231, 220, 130, 138, 96, 73, 38, 12, 245, 140, 196, 189, 149, 117, 87, 62, 53, 241, 196, 162, 207, 96, 65, 39, 20, 237, 134, 245, 144 }, 161) +
        UrlObfuscator.decode(new int[] { 214, 190, 147, 122, 67, 40, 2, 255, 132, 168, 140, 99, 99, 51, 1, 237, 214, 141, 137, 140, 106, 88, 50, 30, 232, 145, 255, 179, 89, 120, 23, 28, 252, 197, 181, 129, 122, 97, 35, 10, 238, 204, 172, 192, 42, 66, 43, 74, 185, 220, 189, 188, 159, 105, 95, 51, 82, 252, 145, 172, 139 }, 178) +
        UrlObfuscator.decode(new int[] { 190, 203, 41, 9, 4 }, 195);
    // Removes the grey tap flash, the long-press callout and accidental text
    // selection so the page behaves like a native screen. Inputs, textareas
    // and contenteditable stay fully selectable/typeable; a site can opt any
    // element back in with data-allow-select. Zero-specificity (:where), so
    // a site's own user-select rules still win. Idempotent.
    private static final String NATIVE_FEEL_JS =
        UrlObfuscator.decode(new int[] { 252, 149, 103, 95, 51, 27, 231, 194, 162, 195, 35, 82 }, 212) +
        UrlObfuscator.decode(new int[] { 140, 98, 11, 53, 8, 238, 251, 209, 170, 210, 68, 101, 55, 25, 227, 223, 163, 145, 85, 87, 52, 28, 205, 193, 184, 130, 111, 3, 59, 13, 243, 211, 183, 138, 56, 85, 40, 14, 27, 241, 202, 242, 164, 69, 87, 57, 3, 255, 195, 177, 181, 119, 84, 60, 45, 225, 216, 162, 143, 55, 93, 58, 18, 227, 158 }, 229) +
        UrlObfuscator.decode(new int[] { 144, 96, 90, 48, 6, 248, 223, 161, 206, 106, 67, 99, 67, 242, 220, 181, 159, 126, 77, 37, 74, 229, 207, 220, 171, 144, 121, 85, 46, 87, 255, 210, 162, 176, 120, 86, 63, 20, 254, 219, 140, 148, 69, 79, 98, 78, 215, 248, 168, 132, 112, 74, 52, 4, 198, 250, 219, 177, 191, 104, 73, 126, 81, 190, 196, 176, 128, 102, 64, 63, 75 }, 246) +
        UrlObfuscator.decode(new int[] { 113, 71, 55, 68, 235, 159, 165, 143, 156, 107, 80, 57, 21, 238, 151, 176, 146, 119, 81, 40, 15, 246, 222, 179, 154, 99, 72, 34, 31, 164, 205, 167, 132, 115, 72, 33, 13, 246, 228, 172, 186, 147, 120, 82, 47, 65, 240, 222, 255, 215, 125, 29, 33, 23, 229, 197, 189, 128, 54 }, 263) +
        UrlObfuscator.decode(new int[] { 110, 86, 36, 85, 231, 199, 239, 149, 127, 76, 59, 0, 233, 197, 190, 199, 107, 85, 35, 4, 240, 198, 135, 141, 101, 114, 91, 51, 8, 179, 157, 170, 140, 110, 90, 48, 83, 186, 137, 162, 132, 33, 71, 41, 81, 172, 245, 150, 134, 102, 82, 44, 18, 230, 228, 164, 133, 147, 93, 78, 47, 92, 161 }, 280) +
        UrlObfuscator.decode(new int[] { 90, 60, 73, 242, 192, 188, 151, 65, 78, 46, 43, 27, 243, 200, 230, 221, 51, 67, 122, 1, 240, 214, 184, 155, 101, 29, 59, 15, 253, 129, 163, 131, 110, 64, 43, 15, 226, 204, 183, 207, 98, 79, 83, 49, 15, 166, 207, 168, 152, 118, 68, 38, 20, 230, 214, 188, 133, 109, 71, 58, 0, 224, 135, 168, 134, 108, 94, 61, 72, 243, 198, 160, 138, 105, 107, 19, 41, 19, 238, 217, 177, 213, 116, 87, 57, 24, 252, 199, 165, 202, 97, 65, 35, 9, 246, 144, 190, 128, 98, 84, 32, 76, 225, 205, 165, 153, 223, 52, 7, 50, 20, 238, 145, 177, 153, 102, 64, 32, 95, 230, 212, 168, 155, 111, 95, 41, 10, 166, 218, 173, 139, 99, 70, 48, 79, 217, 194, 175, 177, 138, 120, 82, 47, 31, 253, 209, 163, 151, 119, 88, 54, 47, 189, 235, 172, 129, 99, 88, 46, 4, 253, 205, 163, 143, 113, 69, 33, 14, 228, 253, 159, 244, 212, 53, 64, 119, 14, 253, 213, 189, 156, 96, 30, 39, 2, 245, 221, 227, 158, 105, 71, 47, 10, 252, 157, 168, 138, 106, 70, 121, 20, 243, 250, 204, 240, 143, 126, 86, 60, 27, 227, 140, 187, 155, 125, 87, 44, 43, 235, 207, 185, 141, 38, 75, 37, 4, 232, 209, 232, 151, 102, 78, 36, 3, 11, 195, 145, 135, 159, 123, 77, 57, 90, 247, 217, 184, 156, 101, 28, 35, 10, 226, 200, 175, 159, 87, 9, 98, 28, 171, 210, 161, 129, 105, 72, 52, 114, 11, 238, 217, 169, 215, 106, 93, 59, 19, 246, 192, 233, 134, 116, 72, 59, 85, 248, 223, 174, 152, 36, 91, 34, 10, 224, 199, 183, 216, 117, 69, 71, 42, 0, 187, 128 }, 297) +
        UrlObfuscator.decode(new int[] { 87, 112, 28, 236, 203, 191, 151, 124, 116, 62, 28, 248, 215, 250, 130, 100, 6, 117, 16, 239, 202, 190, 138, 96, 15, 35, 76, 255, 222, 191 }, 63) +
        UrlObfuscator.decode(new int[] { 55, 0, 166, 132, 247 }, 80) +
        UrlObfuscator.decode(new int[] { 21, 242, 230, 197, 180, 154, 51, 27, 61, 23, 244, 195, 184, 145, 125, 70, 127, 23, 234, 218, 136, 128, 110, 71, 44, 6, 243, 228, 188, 173, 103, 10, 102, 63, 32, 240, 220, 168, 146, 108, 92, 30, 18, 243, 217, 151, 128, 97, 22, 121, 70, 245, 201, 163, 136, 127, 68, 45, 9, 242, 139, 165, 135, 102, 100, 54, 58, 16, 233, 240, 178, 137, 109, 93, 57, 19, 231, 156, 244, 182, 94, 125, 12, 1, 227, 216, 174, 132, 125, 100, 40, 7, 225, 193, 167, 197, 45, 71, 80, 119, 70, 225, 198, 185, 152, 108, 84, 62, 93, 241, 154, 169, 140 }, 97) +
        UrlObfuscator.decode(new int[] { 15, 184, 152, 230, 213 }, 114);
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
        UrlObfuscator.decode(new int[] { 171, 196, 180, 142, 156, 106, 84, 51, 21, 178, 144, 163, 131, 100, 76, 47 }, 131) +
        UrlObfuscator.decode(new int[] { 242, 198, 188, 146, 100, 70, 33, 3, 172, 196, 161, 193, 107, 14, 61, 23, 225, 215, 183, 147, 110, 63, 93, 123, 90, 248, 155, 228, 197, 48, 66, 39, 21, 253, 193, 161, 145, 125, 75, 35, 24, 172, 140, 239, 139, 41, 79, 43, 0, 230, 218, 142, 134, 215, 57, 79, 59, 25, 251, 145, 232, 219, 54, 5, 120, 83, 162, 157, 240, 223, 39, 10, 101, 74, 183, 148, 248, 220, 123 }, 148) +
        UrlObfuscator.decode(new int[] { 195, 177, 141, 97, 85, 41, 48, 16, 189, 207, 180, 150, 112, 92, 127, 21, 188, 207, 161, 151, 101, 69, 61, 0, 173, 207, 229, 131, 103, 76, 34, 30, 202, 194, 235, 197, 115, 71, 93, 63, 85, 187, 146, 231, 196, 37, 7, 105, 5, 245, 193, 161, 148, 86, 67, 33, 12, 248, 131, 169, 199, 123, 87, 42, 12, 240, 139, 229, 205, 39, 54, 101, 110, 33, 178, 132, 233, 214, 33, 12, 33, 6, 230, 215, 234, 141 }, 165) +
        UrlObfuscator.decode(new int[] { 192, 180, 134, 51, 80, 54, 77, 225, 219, 161, 128, 39, 79, 37, 85, 227, 201, 166, 145, 110, 71, 47, 20, 81, 251, 209, 185, 150, 127, 87, 44, 49, 228, 218, 185, 163, 125, 88, 62, 27, 166, 196, 162, 133, 111, 91, 31, 14, 226, 209, 172, 204, 48, 13, 41, 49, 16, 248, 206, 147, 159, 112, 95, 63, 2, 186, 134, 250, 201 }, 182) +
        UrlObfuscator.decode(new int[] { 176, 142, 108, 72, 38, 74, 228, 204, 153, 248, 220, 126, 92, 115, 2, 238, 214, 164, 213, 119, 14, 53, 20, 228, 236, 161, 128, 124, 94, 62, 12, 236, 244, 178, 156, 104, 70, 106, 4, 236, 182, 144, 191, 157, 120, 81, 62, 10, 248, 195, 187, 144, 80, 93, 61, 31, 253, 149, 164, 138, 35, 69, 34, 64, 228, 143, 227, 194, 112, 77, 45, 9, 27, 182, 222, 245, 210, 120, 94, 101, 20, 173, 208, 184, 206, 119, 93, 126, 31, 239, 223, 169, 133, 126, 108, 36, 2, 235, 192, 170, 151, 57, 92 }, 199) +
        UrlObfuscator.decode(new int[] { 177, 145, 62, 20, 54, 20, 187, 202, 166, 142, 124, 13, 46, 86, 237, 204, 188, 164, 105, 72, 52, 22, 246, 196, 164, 140, 138, 100, 80, 62, 82, 253, 215, 180, 131, 120, 81, 61, 6, 191, 212, 160, 141, 120, 65, 46, 4, 253, 237, 171, 131, 104, 65, 45, 22, 168, 142, 221, 191, 158, 119, 92, 40, 22, 237, 217, 178, 182, 123, 95, 61, 3, 171, 198, 168, 197, 99, 64, 98, 11, 161, 142, 164, 130, 57, 65, 121, 28 }, 216) +
        UrlObfuscator.decode(new int[] { 159, 105, 85, 102, 8, 185, 199, 173, 130, 117, 114, 91, 51, 8, 181, 203, 172, 157, 101, 79, 6, 17, 255, 215, 178, 132, 96, 92, 101, 75, 230, 207, 189, 137, 92, 72, 36, 9, 230, 159, 181, 136, 154, 115, 88, 113, 24, 245, 213, 183, 133, 75, 18, 125, 72, 228, 208, 162, 207, 122, 69, 113, 6, 181, 196, 230, 128, 99, 81, 5, 23, 246, 211, 169, 189, 139, 105, 89, 115, 93, 250, 215, 185, 130, 112, 90, 39, 85, 184, 138, 161, 155, 97, 64, 112 }, 233) +
        UrlObfuscator.decode(new int[] { 140, 120, 74, 119, 20, 225, 218, 238, 156, 100, 92, 35, 66, 255, 145, 230, 219, 37, 74, 52, 91, 225, 203, 160, 151, 108, 69, 81, 42, 83, 237, 206, 191, 139, 97, 100, 51, 25, 241, 208, 166, 158, 98, 110, 34, 1, 164, 140, 168, 156, 124, 83, 41, 11, 168, 194, 238, 186, 114, 112, 82, 56, 65, 249, 207, 173, 140, 120, 88, 8, 88, 250, 220, 161, 133, 123, 117, 57, 21, 251, 207, 244, 155, 114, 68, 40, 13, 247, 255, 230, 201, 196 }, 250) +
        UrlObfuscator.decode(new int[] { 109, 69, 59, 64, 241, 199, 183, 196, 106, 31, 113, 91, 22, 162, 223, 175, 213, 118, 92, 54, 16, 226, 221, 242, 213, 123, 13, 104, 95, 181, 196, 231, 192, 35, 82, 62, 6, 244, 133, 161, 222, 96, 82, 27, 54, 35, 177, 205, 230, 159, 55, 95, 50, 2, 215, 219, 166, 156, 117, 89, 33, 9, 206, 192, 162, 143, 103, 92, 21, 3, 230, 208, 235, 203, 58 }, 267) +
        UrlObfuscator.decode(new int[] { 117, 93, 114, 8, 182, 192, 191, 145, 96, 91, 110, 68, 166, 211, 178, 156, 34, 67, 47, 0, 239, 207, 178, 217, 54, 27, 62, 29, 241, 177, 220, 178, 136, 111, 85, 52, 68, 167, 202, 169, 133, 61, 70, 62, 0, 177, 199, 163, 130, 110, 88, 1, 13, 238, 193, 173, 144, 42, 65, 46, 14, 11, 247, 211, 169, 158, 33 }, 284) +
        UrlObfuscator.decode(new int[] { 68, 48, 2, 175, 221, 240, 139, 110, 94, 10, 7, 234, 214, 176, 144, 102, 70, 18, 20, 6, 242, 216, 244, 158, 51, 21, 59, 69, 171, 198, 250, 145, 115, 82, 59, 8, 252, 194, 185, 133, 110, 106, 39, 11, 233, 215, 255 }, 50) +
        UrlObfuscator.decode(new int[] { 42, 4, 169, 207, 212, 246, 158, 46, 18, 124, 95, 235, 216, 186, 156, 112, 27, 49, 67, 185, 137, 232, 142, 62, 10, 119, 84, 234, 192, 239, 158, 102, 87, 44, 92, 227, 173, 133, 175, 193, 107, 91, 43, 11, 242, 240, 185, 155, 114, 70, 121, 3, 161, 204, 162, 158, 111, 79, 59, 58, 230, 194, 172, 145, 112, 11, 61, 28, 79, 165, 223, 174, 158, 123, 82, 99, 10, 235 }, 67) +
        UrlObfuscator.decode(new int[] { 38, 22, 230, 196, 162, 129, 46, 103, 31, 36, 196, 135, 187, 147, 116, 76, 42, 4, 235, 199, 185, 247, 133, 127, 91, 97, 24, 254, 148, 163, 158, 112, 89, 54, 72, 229, 216, 227, 140, 121, 66, 113, 8, 253, 198, 235, 132, 113, 74, 17, 88, 243, 221, 150, 229 }, 84) +
        UrlObfuscator.decode(new int[] { 24, 231, 194, 182, 130, 104, 55, 91, 116, 7, 233, 223, 173, 141, 101, 88, 117, 83, 232, 207, 246, 203, 114, 83, 100, 68, 162, 145 }, 101);
    // Universal video loading indicator. Any <video> on any page that is
    // still fetching/buffering gets a spinner overlay (fades in, removed once
    // it can play), so the user never stares at a blank/grey box. Pure JS,
    // no per-site config; picks up videos added later too. Overlay is a
    // separate fixed element, so the page's own DOM/layout is untouched.
    // Idempotent (__vidLoaderBound).
    // Records the name a page gives an <a download="..."> link (also for
    // anchors clicked from script) so blob:/data: downloads keep it.
    private static final String DOWNLOAD_NAME_JS =
        UrlObfuscator.decode(new int[] { 94, 243, 193, 189, 145, 101, 89, 32, 0, 165, 133, 176, 158, 123, 81, 60, 15, 227, 140, 180, 139, 111, 68, 80, 41, 83, 195, 228, 190, 149, 86, 86, 59, 16, 214, 220, 167, 159, 116, 6, 60, 8, 248, 222, 184, 135, 51, 80, 47, 11, 224, 204, 181, 207, 95, 64, 90, 49, 50, 250, 215, 188, 186, 120, 67, 59, 16, 174, 198, 163, 133, 106, 21 }, 118) +
        UrlObfuscator.decode(new int[] { 225, 211, 171, 135, 119, 75, 46, 14, 95, 236, 216, 191, 211, 123, 16, 35, 3, 228, 204, 175, 154, 116, 25, 49, 73, 168, 204, 226, 131, 107, 90, 9, 19, 242, 215, 173, 129, 119, 85, 37, 121, 88, 252, 146, 179, 155, 106, 121, 35, 2, 231, 221, 177, 135, 101, 85, 103, 73, 233, 195, 188, 132, 101, 71, 38, 2, 162, 141, 229, 196, 96, 14, 87, 44, 24, 250, 157, 252, 142, 113, 89, 50, 26, 227, 157, 147, 159, 116, 93, 33, 4, 232, 233, 184, 128, 108, 64, 35, 67, 162, 226, 172, 133, 114, 112, 87, 57, 62, 233, 211, 189, 159, 114, 24, 59, 27, 231, 215, 149, 159, 120, 64, 33, 3, 234, 206, 135, 137, 106, 67, 108, 31, 194, 204, 165, 146, 144, 119, 89, 30, 9, 243, 221, 191, 146, 56, 91, 59, 7, 247, 245, 191, 152, 96, 65, 35, 10, 238, 231, 169, 138, 99, 13, 37, 77, 234, 211, 165, 185, 210, 124, 18, 60, 31, 237, 249, 163, 130, 103, 93, 49, 7, 229, 213, 231, 201, 105, 67, 60, 4, 229, 199, 166, 130, 34, 13, 63, 30, 166, 135, 150, 229, 128, 97, 88, 59, 13, 251, 223, 254, 144, 61, 72, 47, 12 }, 135) +
        UrlObfuscator.decode(new int[] { 252, 216, 181, 128, 121, 86, 60, 5, 190, 206, 170, 137, 73, 93, 47, 7, 252, 235, 175, 150, 112, 70, 44, 4, 242, 183, 153, 190, 144, 114, 89, 50, 95, 187, 208, 160, 154, 112, 70, 56, 31, 225, 134, 168, 197, 112, 92, 40, 26, 167, 199, 248, 129, 45, 86, 32, 18, 24, 251, 201, 250, 221, 127, 23, 44, 22, 228, 210, 177, 135, 60, 82, 60, 0, 253, 200, 191, 159, 53, 76, 102, 19, 231, 215, 163, 134, 118, 15, 35, 51, 17, 238, 217, 168, 142, 49, 31, 54, 45, 241, 219, 164, 156, 125, 95, 46, 10, 208, 139, 226, 208, 103, 93, 43, 10, 190, 214, 166, 129, 41, 65, 22, 101, 0, 176, 207, 168, 140, 125, 30, 109 }, 152) +
        UrlObfuscator.decode(new int[] { 223, 169, 149, 38, 74, 39, 94, 202, 245, 141, 147, 191, 115, 95, 51, 21, 235, 253, 187, 147, 120, 81, 61, 6, 191, 192, 189, 129, 121, 67, 63, 19, 249, 205, 233, 133, 105, 77, 32, 9, 186 }, 169) +
        UrlObfuscator.decode(new int[] { 242, 141, 181, 91, 119, 59, 23, 251, 221, 163, 181, 99, 75, 32, 9, 229, 222, 231, 152, 117, 73, 49, 11, 247, 219, 177, 133, 209, 125, 81, 53, 24, 241, 132, 190, 130, 120, 86, 32, 26, 253, 223, 248, 198, 117, 95, 41, 8, 162, 221, 160, 142, 117, 12, 127, 17, 231, 213, 181, 173, 144, 61, 83, 56, 84, 248, 200, 167, 154, 108, 28, 39, 26, 248, 195, 227, 143, 127, 75, 62, 7, 236, 198, 179, 149, 44, 31, 62, 89 }, 186) +
        UrlObfuscator.decode(new int[] { 182, 137, 104, 92, 36, 14, 173, 193, 234, 153, 124, 93, 22, 118, 84, 167 }, 203);
    private static final String VIDEO_LOADER_JS =
        UrlObfuscator.decode(new int[] { 244, 157, 111, 87, 59, 3, 255, 218, 186, 219, 59, 74 }, 220) +
        UrlObfuscator.decode(new int[] { 132, 106, 3, 61, 0, 230, 195, 169, 146, 42, 124, 29, 23, 233, 251, 242, 178, 157, 127, 95, 43, 58, 248, 195, 187, 144, 58, 64, 52, 4, 250, 220, 163, 215, 124, 67, 39, 12, 232, 209, 235, 187, 92, 84, 40, 4, 51, 241, 220, 184, 158, 104, 123, 55, 2, 248, 209, 233, 135, 96, 68, 53, 84 }, 237) +
        UrlObfuscator.decode(new int[] { 136, 124, 78, 123, 8, 248, 222, 234, 198, 46 }, 254) +
        UrlObfuscator.decode(new int[] { 105, 91, 35, 15, 255, 195, 166, 134, 39, 69, 54, 23, 171, 139, 186, 148, 141, 103, 70, 53, 29, 178, 221, 183, 148, 99, 88, 49, 29, 230, 159, 183, 138, 122, 104, 32, 14, 231, 204, 166, 147, 68, 92, 13, 7, 170, 134, 159, 128, 136, 116, 88, 23, 21, 248, 220, 178, 132, 86, 71, 32, 85, 184, 153, 189, 139, 121, 89, 57, 4, 178, 222, 166, 148, 37, 76, 126, 6, 238, 195, 202, 179, 152, 114, 79, 116, 17, 253, 214, 178, 137, 104, 87, 61, 18, 229, 194, 171, 131, 120, 5, 46, 6, 235, 210, 171, 128, 106, 87, 7, 13, 229, 242, 219, 179, 136, 32, 83, 63, 80, 182, 222, 252, 134, 118, 70, 36, 2, 225, 149, 187, 141, 121, 10, 58, 28, 186, 194, 170, 135, 118, 79, 36, 14, 11, 176, 222, 174, 158, 123, 77, 61, 50, 250, 208, 185, 150, 124, 69, 120, 72, 253, 217, 181, 135, 111, 14, 97, 92, 245, 209, 234, 138, 102, 28, 103, 0, 33, 235, 213, 191, 182, 118, 89, 51, 19, 231, 247, 160, 129, 54, 11 }, 271) +
        "st.textContent='@keyframes __vlspin{to{transform:rotate(360deg)}}.__vl{position:fixed;z-index:2147483000;pointer-events:none;display:none;align-items:center;justify-content:center;background:rgba(8,12,28,.6);transition:opacity .25s ease;opacity:0;box-sizing:border-box}.__vl.on{opacity:1}.__vl i{width:38px;height:38px;border-radius:50%;border:3px solid rgba(255,255,255,.25);border-top-color:#fff;animation:__vlspin .8s linear infinite;box-sizing:border-box}';" +
        UrlObfuscator.decode(new int[] { 72, 17, 63, 13, 236, 222, 180, 157, 91, 95, 63, 25, 240, 155, 161, 133, 57, 20, 51, 14, 237, 223, 169, 129, 32, 66, 111, 30, 249, 222 }, 288) +
        UrlObfuscator.decode(new int[] { 80, 32, 26, 240, 198, 184, 159, 97, 14, 35, 9, 238, 206, 186, 192, 113, 15, 62, 16, 241, 219, 186 }, 54) +
        UrlObfuscator.decode(new int[] { 46, 0, 173, 210, 237, 135, 115, 82, 80, 44, 1, 224, 205, 244, 139, 125, 86, 50, 12, 199, 199, 179, 133, 117, 17, 115, 94, 240, 215, 188, 199, 102, 66, 50, 18, 235, 209, 169, 178, 116, 126, 74, 56, 65, 166, 135, 234, 209, 101, 83, 33, 1, 225, 220, 241, 150, 110, 66, 62, 9, 176 }, 71) +
        UrlObfuscator.decode(new int[] { 49, 17, 190, 148, 162, 221, 113, 68, 34, 29, 235, 195, 184, 184, 120, 74, 110, 65, 167, 211, 234, 132, 103, 85, 1, 43, 10, 239, 213, 185, 143, 109, 93, 127, 81, 230, 198, 176, 213, 56, 22, 105, 79, 251, 130, 186, 159, 108, 90, 62, 53, 224, 200, 166, 129, 117, 79, 77, 118, 90, 239, 212, 175, 139, 123, 82, 113, 92, 189, 193, 183, 133, 101, 93, 32, 77, 234, 202, 166, 154, 109, 28 }, 88) +
        UrlObfuscator.decode(new int[] { 27, 237, 211, 179, 151, 106, 3, 52, 79, 238, 250, 202, 170, 147, 105, 81, 10, 12, 246, 194, 176, 201, 46, 15, 99, 12, 243, 216, 227, 159, 110, 79, 34, 1, 233, 193, 185, 152, 34, 84, 111, 16, 30, 235, 206, 185, 159, 102, 69, 46, 89, 247, 192, 160, 156, 98, 93, 49, 22, 242, 209, 237, 202, 124, 7, 23, 56, 240, 201, 147, 130, 107, 85, 123 }, 105) +
        UrlObfuscator.decode(new int[] { 7, 250, 217, 163, 149, 125, 28, 54, 91, 234, 194, 170, 154, 120, 94, 37, 74, 239, 201, 171, 149, 96, 31, 62, 31 }, 122) +
        UrlObfuscator.decode(new int[] { 237, 223, 167, 139, 115, 79, 42, 10, 163, 202, 168, 132, 154, 54, 82, 117, 0, 245, 151, 187, 155, 119, 70, 39, 63, 251, 194, 164, 193, 124, 72, 33, 4, 252, 204, 224, 192, 105, 75, 99, 74, 185, 210, 165, 171, 170, 116, 81, 62, 21, 236, 204, 255, 144, 96, 90, 48, 6, 248, 223, 161, 198, 36, 87, 34, 12, 161, 137, 168, 200, 102, 72, 34, 17, 242, 236, 214, 173, 137, 50, 88, 53, 23, 236, 214, 191, 155, 103, 27, 117, 30, 254, 136, 231, 196, 99, 5, 57, 29, 241, 203, 163, 203, 96, 74, 49, 17, 236, 254, 199, 224, 219, 117, 85, 55, 29, 176, 141, 168, 216, 33, 4, 97, 89, 180, 211 }, 139) +
        UrlObfuscator.decode(new int[] { 250, 206, 180, 154, 108, 94, 57, 27, 180, 199, 187, 146, 123, 7, 103, 22, 239, 216, 185, 193, 33, 28, 48, 4, 246, 131, 180, 146, 61, 123, 81, 62, 9, 246, 223, 183, 140, 57, 81, 48, 0, 214, 222, 180, 157, 106, 64, 57, 31, 201, 211, 157, 137, 96, 104, 36, 9, 230, 138, 230, 150, 150, 122, 88, 51, 92, 179, 130, 174, 150, 100, 21, 53, 16, 230, 216, 166, 138, 51, 29, 119 }, 156) +
        UrlObfuscator.decode(new int[] { 203, 163, 153, 34, 95, 41, 21, 166, 204, 249, 211, 57, 72, 124, 41, 13, 179, 208, 190, 148, 126, 76, 63, 77, 252, 159, 248, 219, 106, 70, 46, 28, 173, 218, 246, 156, 122, 115, 46, 59, 190, 210, 162, 144, 33, 79, 2, 40, 83, 195, 228, 172, 149, 35, 65, 55, 7, 180, 193, 239, 135, 62, 72, 43, 25, 206, 196, 191, 135, 108, 78, 40, 2, 199, 207, 171, 132, 110, 107, 108, 56, 31, 239, 146, 240, 195, 97, 87, 39, 84, 240, 193, 236, 151, 106, 90, 14, 3, 230, 218, 188, 156, 98, 66, 22, 16, 250, 206, 164, 200, 137, 55, 6 }, 173) +
        UrlObfuscator.decode(new int[] { 200, 188, 142, 59, 73, 49, 23, 224, 139, 187, 145, 118, 86, 34, 88, 249, 135, 235, 202, 121, 4, 62, 1, 227, 210, 173, 218, 62, 20, 113, 70, 89, 236, 147, 180, 158, 115, 94, 48, 3, 168, 136, 226, 195, 52, 23, 34, 65, 236, 194, 184, 159, 101, 68, 118, 87, 160, 131, 182, 205, 118, 78, 48, 99, 23, 243, 210, 190, 136, 81, 93, 62, 17, 253, 192, 245, 212, 99, 30, 61, 7, 234, 196, 191, 212, 57, 14, 97, 20, 171, 200, 166, 132, 117, 28, 86, 48, 19, 249, 201, 141, 144, 124, 67, 62, 83, 178, 208, 161, 223, 102, 70, 61, 4, 238, 194, 166, 128, 124, 94, 103, 88, 185, 132, 170, 136, 100, 123, 91, 51, 91, 189, 156, 186, 139, 57, 82, 60, 7, 227, 222, 176, 137, 46, 19, 112, 75, 229, 197, 167, 141, 32, 29 }, 190) +
        UrlObfuscator.decode(new int[] { 166, 136, 37, 95, 35, 5, 254, 129, 188 }, 207) +
        UrlObfuscator.decode(new int[] { 137, 153, 54, 28, 51, 82, 225, 214, 229, 147, 121, 86, 33, 30, 247, 223, 164, 193, 109, 95, 41, 10, 254, 204, 141, 139, 99, 72, 33, 13, 246, 137, 231, 187, 151, 107, 27, 114, 65, 246, 150, 180, 154, 116, 71, 32, 60, 240, 221, 170, 211, 42, 115, 20, 28, 229, 143, 252, 137, 43, 69, 51, 18, 228, 206, 219, 157, 149, 117, 87, 62, 81, 252, 216, 181, 128, 121, 86, 60, 5, 190, 204, 188, 136, 109, 95, 47, 44, 228, 194, 171, 128, 106, 87, 106, 70, 233, 184, 151, 244, 199, 51, 94, 54, 27, 226, 219, 176, 154, 103, 28, 51, 31, 235, 215, 177, 144, 111, 69, 42, 29, 234, 195, 171, 144, 45, 70, 46, 3, 10, 243, 216, 178, 143, 95, 85, 61, 26, 243, 219, 160, 218, 60, 80, 32, 31, 235, 195, 168, 168, 98, 64, 36, 3, 174, 202, 237, 216, 116, 15, 31, 0, 8, 241, 129, 180, 193, 100 }, 224) +
        UrlObfuscator.decode(new int[] { 135, 113, 93, 110, 26, 237, 216, 247, 134, 38, 84, 50, 28, 232, 198, 236, 133, 105, 108, 78, 49, 29, 226, 135, 228, 197, 48, 80, 57, 17, 235, 149, 234 }, 241) +
        UrlObfuscator.decode(new int[] { 109, 15, 51, 43, 7, 241, 217, 245, 158, 112, 75, 39, 26, 244, 205, 238, 213, 119, 92, 42, 22, 170, 151, 164, 196, 122, 92, 62, 10, 224, 138, 175, 135, 103, 84, 2, 44, 83, 240, 222, 188, 141, 51, 16, 38, 13, 179, 136, 189, 223, 99, 91, 55, 1, 233, 133, 190, 134, 120, 26, 52, 75, 240, 204, 178, 202, 39, 111, 70, 122, 71, 244, 148, 170, 140, 110, 90, 48, 90, 228, 219, 181, 132, 103, 19, 63, 66, 252, 195, 173, 156, 111, 13, 98, 20, 251, 133, 250, 143, 209, 109, 73, 37, 23, 255, 151, 176, 146, 127, 82, 60, 7, 175, 195, 254, 135, 107, 68, 43, 3, 254, 130, 239, 151, 126, 2, 127, 12, 172, 210, 180, 166, 146, 120, 18, 57, 21, 235, 220, 178, 132, 71, 85, 55, 27, 228, 195, 242, 141, 126, 2, 41, 5, 251, 204, 162, 148, 87, 69, 39, 11, 244, 211, 132 }, 258) +
        UrlObfuscator.decode(new int[] { 122, 84, 121, 81, 248, 207, 190, 197, 112, 2, 47, 29, 233, 197, 177, 141, 108, 76, 105, 24, 86, 229, 207, 185, 138, 111, 92, 43, 3, 215, 219, 189, 158, 115, 69, 57, 0, 224, 235, 190, 138, 103, 76, 96, 1, 243, 203, 167, 151, 107, 78, 46, 119, 87, 230, 196, 245, 153, 117, 89, 36, 5, 217, 221, 160, 134, 63, 81, 43, 10, 165, 139, 164, 132, 46, 1, 124, 27, 172, 159, 190, 203, 41, 79, 22, 101, 0, 249, 215, 169, 156, 99, 88, 120, 22, 248, 210, 161, 130, 92, 70, 61, 25, 162, 202, 174, 141, 32, 0, 41, 11, 163, 138, 249, 156 }, 275) +
        UrlObfuscator.decode(new int[] { 69, 32, 22, 232, 214, 218, 245, 214, 39 }, 292) +
        UrlObfuscator.decode(new int[] { 71, 60, 20, 228, 211, 245, 157, 117, 26, 62, 86, 169, 193, 227, 159, 127, 83, 37, 13, 169, 194, 172, 151, 115, 78, 32, 25, 94, 163, 128, 251, 149, 117, 87, 61, 80, 176, 147, 187, 221, 113, 93, 49, 28, 253, 225, 165, 152, 126, 7, 43, 8, 232, 209, 165, 138, 108, 82, 104, 120, 17, 243, 155, 242, 211, 98, 80, 62, 18, 240, 156, 188, 219, 42, 77 }, 58) +
        "}" +
        UrlObfuscator.decode(new int[] { 57, 15, 253, 221, 181, 136, 37, 69, 32, 22, 232, 214, 218, 229, 128 }, 75) +
        UrlObfuscator.decode(new int[] { 58, 14, 244, 218, 172, 158, 121, 91, 116, 21, 224, 208, 189, 138, 38, 4, 55, 25, 235, 207, 245, 215, 61, 81, 54, 26, 249, 200, 166, 247, 138, 116, 95, 48, 82, 176, 134, 231, 223, 103, 85, 53, 79, 227, 213, 190, 155, 104, 95, 63, 43, 231, 193, 170, 135, 113, 77, 44, 12, 199, 210, 222, 179, 152, 52, 93, 40, 24, 245, 210, 255, 206, 105, 80, 51, 5, 243, 199, 230, 136, 37, 80, 55, 20 }, 92) +
        UrlObfuscator.decode(new int[] { 11, 249, 197, 169, 157, 97, 72, 40, 69, 243, 194, 171, 149, 40, 122, 23, 38, 21, 253, 146, 188, 214, 99, 87, 39, 19, 246, 198, 247, 214, 106, 0, 57, 13, 249, 205, 172, 156, 41, 82, 36, 3, 205, 195, 172, 133, 194, 35, 0, 123, 45, 211, 253, 157, 184, 49, 28, 49, 93, 230, 208, 162, 136, 107, 89, 98, 52, 213, 223, 164, 176, 103, 76, 48, 94, 246, 211, 181, 186, 197, 96 }, 109) +
        UrlObfuscator.decode(new int[] { 24, 232, 210, 184, 142, 112, 87, 57, 86, 250, 223, 251, 151, 56, 75, 38, 8, 165, 201, 229, 158, 104, 90, 32, 3, 241, 130, 229, 135, 47, 84, 94, 44, 26, 249, 207, 244, 141, 121, 80, 24, 20, 249, 214, 239, 204, 45, 8, 24, 36, 200, 238, 133, 206, 33, 66, 104, 17, 229, 209, 165, 132, 116, 49, 97, 2, 10, 247, 237, 184, 145, 99, 11, 51, 21, 255, 193, 180, 203, 114 }, 126) +
        UrlObfuscator.decode(new int[] { 212, 137, 186, 141, 98, 94, 32, 6, 224, 129, 233, 195, 112, 86, 32, 12, 19, 251, 217, 251, 215, 61, 74, 61, 18, 253, 220, 186, 148, 53, 108, 126, 9, 225, 223, 137, 138, 105, 65, 96, 1, 243, 203, 167, 151, 107, 78, 46, 119, 10, 180, 199, 191, 149, 122, 77, 58, 19, 251, 192, 253, 147, 117, 84, 10, 24, 232, 194, 191, 166, 96, 91, 51, 3, 235, 193, 177, 202, 117, 12, 72, 63, 20, 232, 151, 174, 139, 109, 82, 127, 78, 233, 154, 233 }, 143) +
        UrlObfuscator.decode(new int[] { 251, 152, 174, 145, 125, 66, 51, 23, 255, 144, 250, 210, 119, 82, 60, 1, 252, 206, 183, 202, 32, 12, 41, 8, 230, 215, 170, 132, 125, 87, 42, 19, 239, 234, 217, 181, 219, 55, 29, 53, 23, 246, 210, 176, 144, 119, 83, 37, 17, 168, 130, 234, 137, 121, 88, 38, 26, 160, 138, 226, 129, 110, 82, 53, 9, 26, 250, 154, 240, 220, 123, 91, 55, 5, 226, 146, 137, 221, 116, 94, 34, 42, 239, 206, 164, 195, 108, 92, 38, 4, 242, 204, 171, 141, 42, 85, 105, 36, 26, 242, 223, 174, 151, 124, 86, 35, 88, 244, 208, 183, 183, 103, 85, 33, 26, 193, 197, 184, 158, 108, 70, 34, 20, 173, 208, 239, 141, 106, 12, 75, 44, 8, 249, 146, 225, 132, 49, 12 }, 160) +
        UrlObfuscator.decode(new int[] { 194, 181, 155, 71, 67, 56, 14, 248, 223, 169, 139, 46, 67, 49, 13, 225, 213, 169, 176, 144, 53, 21, 32, 19, 255, 144, 246, 132, 116, 82, 122, 20, 227, 209, 162, 139, 37, 5, 112, 23, 165, 154, 242, 214, 44, 31 }, 177) +
        UrlObfuscator.decode(new int[] { 191, 200, 40, 54, 5 }, 194);
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
        UrlObfuscator.decode(new int[] { 251, 148, 100, 94, 44, 26, 228, 195, 165, 194, 32, 83 }, 211) +
        UrlObfuscator.decode(new int[] { 141, 101, 10, 54, 9, 17, 250, 210, 171, 213, 105, 81, 55, 0, 217, 197, 177, 157, 84, 88, 60, 10, 222, 196, 175, 128, 111, 91, 97, 21, 227, 209, 177, 145, 108, 26 }, 228) +
        UrlObfuscator.decode(new int[] { 130, 125, 93, 54, 30, 231, 129, 189, 133, 99, 92, 5, 25, 237, 201, 128, 140, 104, 70, 18, 8, 227, 244, 219, 175, 193, 125, 79, 55, 27, 227, 223, 186, 154, 59, 93, 33, 4, 252, 135, 182 }, 245) +
        UrlObfuscator.decode(new int[] { 105, 85, 48, 16, 191, 206, 176, 171, 141, 97, 64, 32, 7, 162 }, 262) +
        UrlObfuscator.decode(new int[] { 101, 83, 33, 1, 225, 220, 241, 158, 106, 89, 109, 60, 249, 197, 164, 129, 116, 67, 109, 2, 246, 204, 162, 148, 150, 113, 83, 116, 9, 255, 202, 183, 155, 96, 80, 120, 1, 247, 219, 181, 140, 122, 4, 55 }, 279) +
        UrlObfuscator.decode(new int[] { 94, 38, 20, 165, 205, 173, 146, 116, 84, 2, 58, 18, 255, 206, 183, 156, 118, 67, 120, 22, 230, 214, 179, 133, 117, 106, 34, 8, 225, 206, 164, 157, 32, 0, 47, 11, 244, 214, 182, 198, 41, 36 }, 296) +
        UrlObfuscator.decode(new int[] { 87, 51, 12, 238, 206, 247, 140, 110, 70, 48, 73, 180, 212, 184, 156, 106, 9, 118 }, 62) +
        UrlObfuscator.decode(new int[] { 38, 8, 165, 195, 187, 158, 122, 6, 42, 19, 233, 208, 170, 146, 109, 69, 22, 55, 19, 236, 206, 174, 215, 117, 66, 58, 1, 253, 195, 190, 148, 45, 91, 60, 24, 233, 144 }, 79) +
        UrlObfuscator.decode(new int[] { 20, 13, 231, 198, 170, 154, 104, 25, 61, 15, 226, 198, 233, 168, 79, 10, 120, 0, 254, 217, 191, 197, 126, 80, 56, 2, 245, 217, 184, 184, 95, 8, 110, 57, 17, 239, 249, 186, 153, 113, 16, 49, 3, 251, 215, 167, 155, 126, 94, 103, 26, 164, 215 }, 96) +
        UrlObfuscator.decode(new int[] { 7, 241, 221, 238, 140, 49, 95, 108, 79, 252, 137, 167, 134, 103, 70, 50, 21, 187, 246, 216, 245, 221, 122, 19, 43, 29, 227, 195, 167, 154, 40 }, 113) +
        UrlObfuscator.decode(new int[] { 205, 195, 170, 186, 157, 105, 18, 48, 31, 224, 203, 255, 151, 60, 26, 53, 29, 227, 245, 174, 141, 101, 4, 45, 31, 231, 203, 179, 143, 106, 74, 107, 15, 232, 205, 218, 247, 134, 121, 67, 46, 10, 182, 199, 163, 134, 124, 27, 63, 24, 253, 202, 231, 214 }, 130) +
        UrlObfuscator.decode(new int[] { 187, 211, 138, 157, 102, 67, 40, 49, 247, 214, 146, 181, 46, 8, 35, 11, 241, 231, 160, 131, 151, 54, 91, 41, 21, 249, 205, 177, 152, 120, 29, 49, 11, 230, 152, 171, 138, 118, 89, 63, 69, 250, 220, 187, 143, 46, 64, 60, 23, 171, 154, 189, 246, 197, 96, 21, 96, 7, 176, 131 }, 147) +
        UrlObfuscator.decode(new int[] { 205, 165, 202, 100, 88, 75, 45, 83, 240, 222, 180, 158, 108, 95, 127, 28, 250, 195, 167, 133, 62, 78, 45, 14, 233, 219, 190, 212, 109, 95, 50, 22, 170, 201, 173, 136, 110, 55, 25, 113, 91, 178, 129, 164, 155, 118, 66, 54, 28, 187, 215, 248, 139, 114 }, 164) +
        UrlObfuscator.decode(new int[] { 220, 186, 131, 103, 69, 126, 28, 250, 212, 160, 142, 36, 77, 33, 20, 246, 201, 165, 154, 63, 6, 46, 48, 16, 248, 155, 224 }, 181) +
        UrlObfuscator.decode(new int[] { 162, 138, 103, 86, 47, 4, 238, 235, 144, 191, 147, 127, 67, 119, 25, 231, 198, 176, 154, 119, 113, 57, 25, 227, 202, 229, 133, 101, 90, 60, 28, 174, 157 }, 198) +
        UrlObfuscator.decode(new int[] { 190, 152, 101, 65, 39, 92, 240, 212, 171, 171, 123, 73, 37, 30, 197, 193, 180, 146, 96, 74, 38, 16, 169, 135, 220, 182, 156, 114, 92, 63, 94, 180, 209, 163, 155, 119, 71, 59, 30, 254, 135, 231, 150 }, 215) +
        UrlObfuscator.decode(new int[] { 156, 117, 95, 62, 0, 236, 193, 180, 141, 154, 112, 73, 114, 25, 245, 221, 161, 217, 100, 80, 57, 28, 228, 212, 147, 135, 103, 65, 40, 67, 227, 199, 184, 146, 114, 12, 127, 30, 225, 192, 180, 188, 150, 53, 89, 114, 1, 228 }, 232) +
        UrlObfuscator.decode(new int[] { 144, 126, 31, 119, 28, 250, 195, 167, 133, 62, 73, 39, 1, 233, 216, 182, 149, 41, 78, 40, 21, 241, 215, 236, 135, 105, 115, 91, 46, 82, 247, 223, 183, 159, 99, 94, 124, 15 }, 249) +
        UrlObfuscator.decode(new int[] { 124, 72, 58, 71, 227, 215, 182, 216, 118, 83, 57, 36, 27, 239, 206, 230, 148, 124, 79, 119, 50, 218, 249, 150, 138, 114, 85, 63, 26, 228, 195, 165, 194, 46, 124, 47, 3, 165, 209, 176, 135, 115, 0, 94, 60, 18, 238, 207, 191, 157, 56, 86, 118, 7, 241, 194, 167, 148, 99, 91, 96, 74, 160, 140, 139, 139, 103, 85, 50, 32, 246, 209, 173, 147, 39, 54, 5, 32 }, 266) +
        UrlObfuscator.decode(new int[] { 120, 91, 45, 27, 255, 158, 176, 221, 104, 87, 35, 2, 178, 192, 168, 155, 43, 111, 59, 26, 232, 212, 237, 195, 66, 64, 46, 18, 11, 219, 207, 174, 148, 104, 30, 113, 76, 235 }, 283) +
        UrlObfuscator.decode(new int[] { 67, 53, 5, 235, 206, 184, 195, 111, 91, 58, 78, 189, 215, 161, 151, 119, 83, 46, 100, 3 }, 49) +
        UrlObfuscator.decode(new int[] { 52, 0, 242, 191, 214, 188, 146, 127, 86, 60, 11, 170, 247, 167, 134, 114, 75, 127, 0, 253, 193, 185, 131, 127, 83, 57, 13, 169, 203, 164, 148, 45, 65, 32, 12, 19, 182, 212, 178, 139, 111, 77, 118, 17, 255, 217, 177, 128, 62, 87, 37, 1, 237, 217, 165, 132, 100, 1, 46, 14, 234, 192, 237, 152 }, 66) +
        UrlObfuscator.decode(new int[] { 33, 23, 229, 197, 189, 128, 118, 71, 34, 4, 237, 146, 224, 128, 108, 72, 38, 69, 173, 206, 222, 179, 152, 38, 93, 51, 21, 253, 153, 184, 148, 121, 86, 126, 22, 245, 219, 136, 132, 96, 78, 112, 15, 253, 201, 165, 145, 109, 76, 44, 73, 169, 228, 204, 184, 136, 110, 72, 55, 88, 199, 196, 186, 153, 122, 65, 52, 94, 253, 203, 190, 131, 103, 92, 44, 64, 225, 207, 169, 129, 42, 25, 60, 29, 68, 227, 148, 231 }, 83) +
        UrlObfuscator.decode(new int[] { 22, 230, 209, 174, 140, 137, 123, 21, 51, 11, 238, 202, 246, 154, 99, 89, 32, 26, 226, 221, 181, 208, 102, 76, 34, 15, 230, 204, 187, 221, 93, 77, 37, 13, 230, 205, 165, 172, 165, 45, 97, 6, 83, 162, 197, 254, 205 }, 100) +
        UrlObfuscator.decode(new int[] { 28, 250, 195, 167, 133, 62, 76, 34, 4, 239, 192, 226, 192, 51, 90, 111, 94, 249, 152 }, 117) +
        UrlObfuscator.decode(new int[] { 251, 141, 237, 202 }, 134);
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
            line.setColor(Color.parseColor(UrlObfuscator.decode(new int[] { 180, 134, 151, 197, 36, 6, 97 }, 151)));
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
            int fur = Color.parseColor(UrlObfuscator.decode(new int[] { 139, 129, 210, 67, 17, 5, 32 }, 168));
            int pink = Color.parseColor(UrlObfuscator.decode(new int[] { 154, 158, 195, 87, 6, 23, 67 }, 185));
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
            line.setColor(Color.parseColor(UrlObfuscator.decode(new int[] { 233, 168, 77, 101, 126, 32, 180 }, 202)));
            c.drawLine(22, 47, 9, 44, line);
            c.drawLine(22, 52, 9, 53, line);
            c.drawLine(78, 47, 91, 44, line);
            c.drawLine(78, 52, 91, 53, line);
            line.setColor(Color.parseColor(UrlObfuscator.decode(new int[] { 248, 202, 91, 9, 96, 66, 165 }, 219)));

            fill.setColor(Color.parseColor(UrlObfuscator.decode(new int[] { 207, 58, 30, 123, 92, 178, 229 }, 236)));
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
            shape(c, tear, Color.parseColor(UrlObfuscator.decode(new int[] { 222, 36, 126, 30, 65, 222, 143 }, 253)));

            android.graphics.Path nose = new android.graphics.Path();
            nose.moveTo(47.5f, 48f); nose.lineTo(52.5f, 48f); nose.lineTo(50f, 51f); nose.close();
            fill.setColor(Color.parseColor(UrlObfuscator.decode(new int[] { 45, 104, 116, 92, 191, 144, 141 }, 270))); c.drawPath(nose, fill);

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
            paint.setColor(Color.parseColor(UrlObfuscator.decode(new int[] { 60, 120, 111, 58, 169, 255, 156 }, 287)));
            canvas.drawCircle(cx, cy, stroke * 1.05f, paint);

            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(stroke);
            paint.setColor(Color.parseColor(UrlObfuscator.decode(new int[] { 22, 97, 22, 247, 208, 180, 219 }, 53)));
            float[] radii = {w * 0.24f, w * 0.38f};
            int[] alphas = {235, 130};
            for (int i = 0; i < radii.length; i++) {
                paint.setAlpha(alphas[i]);
                RectF arc = new RectF(cx - radii[i], cy - radii[i], cx + radii[i], cy + radii[i]);
                canvas.drawArc(arc, 208, 124, false, paint);
            }

            paint.setAlpha(255);
            paint.setColor(Color.parseColor(UrlObfuscator.decode(new int[] { 101, 35, 194, 149, 128, 212, 55 }, 70)));
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
            String name = (appName == null || appName.trim().isEmpty()) ? UrlObfuscator.decode(new int[] { 22, 6, 229 }, 87) : appName.trim().toUpperCase();

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
            letter.setTypeface(Typeface.create(UrlObfuscator.decode(new int[] { 27, 230, 200, 182, 201, 112, 71, 51, 9, 25, 179, 208, 185, 159, 115, 76, 53 }, 104), Typeface.NORMAL));
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
                videoResId = context.getResources().getIdentifier(UrlObfuscator.decode(new int[] { 26, 237, 196, 162, 154, 121, 108, 33, 1, 252, 206, 189, 133 }, 121), UrlObfuscator.decode(new int[] { 248, 200, 191 }, 138), pkg);
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
                int resId = context.getResources().getIdentifier(UrlObfuscator.decode(new int[] { 248, 207, 170, 140, 120, 91, 10, 7, 227, 222, 176, 131, 103 }, 155), UrlObfuscator.decode(new int[] { 200, 185, 139, 126, 73, 37, 10, 224 }, 172), pkg);
                if (resId != 0) {
                    iv.setImageURI(Uri.parse(UrlObfuscator.decode(new int[] { 220, 178, 159, 104, 86, 49, 19, 184, 199, 177, 128, 125, 68, 34, 12, 235, 151, 227, 196 }, 189) + pkg + "/" + resId));
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
                mp.setDataSource(context, Uri.parse(UrlObfuscator.decode(new int[] { 175, 131, 104, 89, 37, 0, 236, 137, 180, 128, 119, 76, 55, 19, 227, 250, 132, 242, 211 }, 206) + pkg + "/" + videoResId));
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
            int iconResId = context.getResources().getIdentifier(UrlObfuscator.decode(new int[] { 182, 157, 66, 80, 58, 15, 247, 219, 191, 147, 103 }, 223), UrlObfuscator.decode(new int[] { 157, 102, 94, 32, 13, 251 }, 240), context.getPackageName());
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
        root.setBackgroundColor(Color.parseColor(UrlObfuscator.decode(new int[] { 34, 16, 10, 110, 72, 172, 142 }, 257)));

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
        swipeRefresh.setColorSchemeColors(Color.parseColor(UrlObfuscator.decode(new int[] { 49, 3, 20, 89, 205, 233, 138 }, 274)));
        swipeRefresh.addView(webView, new ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        root.addView(swipeRefresh, webParams);

        // Status-bar and navigation-bar strips. Added before the splash /
        // error / navigation overlays so those still cover the whole screen.
        topScrim = new View(this);
        topScrim.setBackgroundColor(Color.parseColor(UrlObfuscator.decode(new int[] { 0, 114, 84, 176, 170, 142, 232 }, 291)));
        root.addView(topScrim, new FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT, 0, Gravity.TOP));
        bottomScrim = new View(this);
        bottomScrim.setBackgroundColor(Color.parseColor(UrlObfuscator.decode(new int[] { 26, 104, 66, 166, 128, 228, 198 }, 57)));
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

        final SplashController loading = new LoadingSplashView(this, Color.parseColor(UrlObfuscator.decode(new int[] { 105, 89, 184, 151, 246, 213, 52 }, 74)), UrlObfuscator.decode(new int[] { 26, 22, 252, 192, 182, 214, 93, 65, 49 }, 91), 0);
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
        final String offlineScrimColor = offlineIsNightMode ? UrlObfuscator.decode(new int[] { 79, 178, 147, 249, 216, 55, 22, 117, 84 }, 108) : UrlObfuscator.decode(new int[] { 94, 170, 141, 234, 201, 40, 7, 102, 69 }, 125);
        // Slightly translucent (not fully opaque) card/button fills, so a
        // hint of the dimmed scrim behind still shows through -- a cheap
        // stand-in for a true frosted-glass blur, which would need
        // RenderEffect (API 31+) and a snapshot of whatever's behind the
        // card to do for real.
        final String offlineCardBg = offlineIsNightMode ? UrlObfuscator.decode(new int[] { 173, 232, 244, 217, 73, 27, 13, 84, 182 }, 142) : UrlObfuscator.decode(new int[] { 188, 252, 229, 186, 41, 124, 107, 62, 165 }, 159);
        final String offlineTitleColor = offlineIsNightMode ? UrlObfuscator.decode(new int[] { 147, 137, 168, 75, 106, 13, 44 }, 176) : UrlObfuscator.decode(new int[] { 226, 208, 207, 46, 13, 108, 75 }, 193);
        final String offlineSubtitleColor = offlineIsNightMode ? UrlObfuscator.decode(new int[] { 241, 179, 40, 109, 118, 47, 200 }, 210) : UrlObfuscator.decode(new int[] { 192, 55, 99, 117, 29, 75, 216 }, 227);
        final String offlineButtonBg = offlineIsNightMode ? UrlObfuscator.decode(new int[] { 215, 43, 113, 101, 72, 187, 150, 249, 173 }, 244) : UrlObfuscator.decode(new int[] { 38, 28, 0, 38, 183, 228, 137, 154, 188 }, 261);
        final float offlineDensity = getResources().getDisplayMetrics().density;

        // Outer full-screen scrim so the card reads as a dialog sitting on
        // top of the app, matching how a native "no connection" alert
        // dims everything behind it.
        final FrameLayout errorView = new FrameLayout(this);
        errorView.setBackgroundColor(Color.parseColor(UrlObfuscator.decode(new int[] { 53, 5, 21, 67, 211, 129, 146 }, 278)));
        errorView.setVisibility(View.GONE);

        // The error screen is the branded HTML page in ERROR_PAGE_HTML,
        // rendered in its own small WebView that fills errorView. Its
        // Retry / Back buttons call back into native code through the
        // AndroidError interface (registered below, once doRetry exists).
        // Navigation inside it is blocked so it can only ever show that page.
        final WebView errorWeb = new WebView(this);
        errorWeb.setBackgroundColor(Color.parseColor(UrlObfuscator.decode(new int[] { 4, 118, 36, 180, 226, 242, 163 }, 295)));
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
        navOverlay.setBackgroundColor(Color.parseColor(UrlObfuscator.decode(new int[] { 30, 108, 75, 170, 137, 232, 199 }, 61)));
        navOverlay.setVisibility(View.GONE);
        final BouncingDotsView navDots = new BouncingDotsView(this, Color.parseColor(UrlObfuscator.decode(new int[] { 109, 43, 190, 237, 248, 172, 77 }, 78)));
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
        }, UrlObfuscator.decode(new int[] { 30, 16, 249, 206, 180, 147, 125, 125, 37, 4, 250, 198 }, 95));

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
        String spoofedUA = defaultUA.replace(UrlObfuscator.decode(new int[] { 75, 175, 217, 187 }, 112), "").replaceAll("Version/[0-9.]+\s", "");
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
        webView.addJavascriptInterface(new AndroidBridge(), UrlObfuscator.decode(new int[] { 192, 206, 219, 172, 146, 117, 95, 24, 11, 241, 211, 177, 144 }, 129));

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
                    UrlObfuscator.decode(new int[] { 186, 215, 165, 129, 109, 89, 37, 4, 228, 129, 225, 156 }, 146) +
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
                    UrlObfuscator.decode(new int[] { 215, 176, 152, 123 }, 163) +
                    "var vp=document.querySelector('meta[name=\"viewport\"]');" +
                    UrlObfuscator.decode(new int[] { 221, 181, 218, 48, 70, 63, 71, 246, 218, 187, 215, 109, 71, 36, 19, 232, 193, 173, 150, 47, 67, 77, 59, 28, 232, 222, 159, 149, 125, 90, 51, 27, 224, 155, 245, 156, 117, 91, 47, 74, 165, 144, 188, 153, 38, 84, 35, 17, 197, 215, 182, 147, 105, 125, 75, 41, 25, 179, 157, 183, 153, 122, 83, 114, 88, 180, 196, 184, 149, 120, 94, 34, 30, 255, 141, 224, 211, 99, 73, 38, 17, 238, 199, 175, 148, 209, 118, 88, 61, 31, 180, 216, 168, 135, 115, 91, 48, 48, 250, 216, 188, 139, 38, 91, 60, 66, 177, 212 }, 180) +
                    UrlObfuscator.decode(new int[] { 179, 148, 45, 81, 36, 20, 62, 234, 201, 174, 146, 120, 76, 44, 18, 190, 146, 183, 156, 124, 69, 53, 1, 250, 138, 224, 204, 125, 64, 44, 19, 238, 152, 160, 134, 116, 72, 35, 58, 83, 234, 213, 191, 142, 113, 20, 119, 31, 251, 221, 167, 155, 112, 92, 98, 29, 238, 205, 167, 143, 52, 25, 105, 86, 169, 132, 174, 131, 121, 73, 82, 43, 16, 177, 200, 185, 152, 116, 82, 107, 68, 186, 131, 254, 209, 125, 70, 32, 4, 225, 222, 167, 196, 123, 68, 39, 9, 225, 158, 243, 207, 48, 51, 30, 40, 15, 254, 200, 244, 139, 116, 87, 57, 21, 241, 222, 180, 205, 97, 65, 106, 69, 176 }, 197) +
                    UrlObfuscator.decode(new int[] { 191, 147, 60, 18, 54, 30, 243, 218, 163, 136, 98, 95, 100, 14, 237, 211, 131, 137, 97, 78, 39, 15, 244, 221, 199, 148, 152, 51, 29, 6, 39, 246, 216, 177, 134, 124, 91, 53, 38, 230, 203, 186, 156, 100, 88, 61, 46, 238, 222, 226, 205, 42, 89 }, 214) +
                    UrlObfuscator.decode(new int[] { 145, 103, 87, 100, 16, 246, 156, 164, 176, 157, 104, 81, 62, 20, 237, 150, 180, 132, 112, 85, 39, 23, 212, 220, 170, 131, 104, 66, 63, 66, 174, 219, 179, 159, 105, 65, 100, 75, 186, 211, 203, 240, 148, 120, 6, 125, 38, 199, 214, 184, 145, 102, 92, 59, 21, 198, 198, 171, 154, 124, 68, 56, 29, 206, 206, 190, 194, 63 }, 231) +
                    UrlObfuscator.decode(new int[] { 139, 99, 24, 33, 17, 235, 198, 146, 159, 97, 90, 40, 2, 255, 151, 238, 128, 115, 75, 41, 72, 225, 205, 165, 153, 132, 115, 92, 36, 86, 237, 208, 188, 131, 126, 15, 101, 67, 162, 199, 167, 206, 103, 64, 60, 4, 248, 221, 169, 137, 114, 30, 43, 21, 231, 211, 166, 179, 145, 106, 17, 35, 64, 241, 209, 179, 146, 112, 90, 114, 27, 252, 192, 160, 156, 121, 77, 37, 30, 178, 199, 177, 131, 119, 66, 47, 13, 246, 141, 199, 228, 158, 112, 82, 42, 88, 241, 218, 166, 154, 102, 71, 51, 31, 228, 148, 227, 154, 105, 73, 33, 0, 252, 138, 178, 128, 124, 87, 111, 18, 233, 229, 219, 240, 157, 127, 80, 44, 11, 227, 140, 228, 196, 35, 23, 112, 25, 226, 222, 162, 158, 127, 75, 39, 28, 188, 210, 160, 156, 119, 15, 50, 9, 5, 251, 144, 189, 159, 112, 76, 43, 3, 172, 132, 228, 195, 55, 16, 57, 2, 254, 194, 190, 159, 107, 71, 60, 92, 251, 130, 255 }, 248) +
                    UrlObfuscator.decode(new int[] { 109, 71, 36, 19, 232, 193, 173, 150, 47, 72, 90, 63, 25, 178, 218, 170, 137, 125, 89, 50, 54, 252, 218, 190, 149, 56, 92, 58, 68, 183, 214 }, 265) +
                    UrlObfuscator.decode(new int[] { 103, 90, 57, 3, 245, 221, 252, 150, 59, 74, 45 }, 282) +
                    "try{var m=document.querySelector('meta[name=\"theme-color\"]');" +
                    UrlObfuscator.decode(new int[] { 89, 41, 70, 224, 138, 237, 157, 96, 70, 35, 9, 242, 138, 130, 140, 101, 82, 80, 55, 25, 222, 201, 179, 157, 127, 82, 112, 83, 213, 221, 182, 131, 127, 70, 42, 47, 254, 194, 174, 142, 109, 9, 39, 21, 244, 207, 187, 181, 104, 122, 83, 56, 63, 244, 214, 182, 138, 62, 77, 20, 26, 247, 192, 190, 153, 107, 108, 63, 5, 239, 205, 172, 198, 102, 86, 53, 8, 250, 246, 169, 133, 146, 123, 126, 51, 23, 245, 203, 240, 154, 56, 82, 49, 7, 211, 197, 164, 157, 103, 79, 57, 31, 239, 129, 239, 132, 105, 75, 48, 6, 236, 213, 231, 246, 130, 97, 27, 124, 83, 162, 197 }, 48) +
                    UrlObfuscator.decode(new int[] { 60, 3, 30, 234, 222, 180, 211, 127, 16, 35, 10 }, 65) +
                    UrlObfuscator.decode(new int[] { 38, 3, 233, 212, 167, 139, 36, 92, 35, 7, 236, 200, 177, 203, 69, 77, 38, 19, 239, 246, 218, 159, 142, 114, 94, 62, 29, 177, 144, 148, 154, 119, 64, 62, 25, 235, 236, 191, 133, 111, 77, 44, 70, 230, 214, 181, 136, 122, 114, 32, 7, 26, 220, 220, 191, 144, 125, 75, 55, 2, 248, 209, 253, 136 }, 82) +
                    UrlObfuscator.decode(new int[] { 21, 227, 211, 224, 175, 151, 126, 87, 25, 29, 164, 222, 162, 152, 118, 64, 58, 29, 255, 152, 230, 149 }, 99) +
                    UrlObfuscator.decode(new int[] { 2, 242, 192, 241, 147, 103, 79, 36, 2, 182, 241, 148, 211, 113, 71, 55, 68, 230, 159, 165, 143, 156, 107, 80, 57, 21, 238, 151, 189, 155, 115, 88, 49, 29, 230, 247, 162, 128, 99, 125, 35, 2, 228, 221, 224, 170, 103, 81, 44, 77, 228, 205, 175, 176, 140, 53, 75, 50, 20, 253, 215, 160, 216, 124, 90, 61, 23, 227, 231, 166, 138, 121, 68, 100, 88, 160, 132, 176, 143, 107, 64, 44, 21, 175, 201, 209, 176, 152, 110, 115, 63, 16, 255, 223, 162, 216, 38, 26, 105 }, 116) +
                    UrlObfuscator.decode(new int[] { 242, 204, 170, 142, 100, 8, 90, 119, 6, 255, 211, 187, 144, 118, 25, 38, 0, 231, 219, 250, 148, 57, 20, 43, 80, 233, 133, 186, 136, 122, 66, 40, 17, 193, 207, 167, 140, 101, 113, 74, 102, 1 }, 133) +
                    UrlObfuscator.decode(new int[] { 255, 211, 252, 151, 125, 82, 37, 2, 235, 195, 184, 197, 104, 70, 44, 30, 160, 131, 167, 139, 99, 72, 46, 113, 23, 243, 216, 190, 130, 86, 94, 127, 18, 250, 215, 166, 159, 116, 94, 59, 64, 239, 195, 175, 147, 32, 20, 119, 79, 230, 204, 162, 139, 111, 14, 79, 43, 14, 244, 147, 190, 150, 123, 66, 59, 16, 250, 199, 252, 147, 127, 75, 55, 68, 183 }, 150) +
                    UrlObfuscator.decode(new int[] { 206, 160, 205, 103, 75, 35, 8, 238, 177, 215, 179, 152, 126, 66, 22, 30, 191, 210, 186, 151, 102, 95, 52, 30, 251, 128, 169, 131, 104, 95, 36, 13, 233, 210, 128, 136, 102, 79, 36, 14, 11, 183, 129, 236, 210, 121, 81, 57, 30, 248, 155, 164, 134, 97, 89, 120, 11, 225, 206, 185, 134, 111, 71, 60, 73, 226, 202, 167, 150, 111, 68, 46, 43, 59, 241, 217, 182, 159, 119, 76, 126, 77 }, 167) +
                    UrlObfuscator.decode(new int[] { 206, 182, 132, 53, 68, 50, 0, 226, 213, 242, 136, 120, 66, 40, 30, 224, 199, 169, 206, 118, 13, 56, 11, 231, 136, 158, 173, 212, 110, 94, 46, 12, 234, 217, 246, 155, 97, 95, 62, 74, 230, 206, 188, 205, 109, 22, 57, 71, 225, 201, 162, 128, 124, 108, 36, 73, 167, 183, 153, 244, 199, 109, 91, 43, 88, 245, 139, 166, 218, 122, 92, 53, 21, 247, 225, 171, 196, 44, 3, 110, 65, 188, 207, 163, 204, 98, 30, 113, 28, 3, 252, 129, 236, 210, 104, 92, 44, 2, 228, 219, 244, 157, 103, 93, 60, 84 }, 184) +
                    UrlObfuscator.decode(new int[] { 191, 137, 117, 6, 51, 89, 240, 140, 178, 149, 157, 109, 73, 46, 18, 244, 222, 240, 150, 61, 4, 120, 17, 187, 159, 163, 159, 98, 68, 56, 67, 173, 133, 239, 206, 61, 76, 34, 75, 244, 143, 172, 186, 144, 122, 72, 51, 70, 170, 145, 165, 147, 97, 65, 33, 28, 177, 222, 186, 130, 97, 23 }, 201) +
                    UrlObfuscator.decode(new int[] { 172, 152, 106, 23, 55, 25, 169, 197, 252, 157, 117, 65, 41, 25, 228, 149, 249, 214, 120, 70, 52, 22, 225, 229, 174, 142, 97, 107, 22, 43, 39, 168, 231, 240, 194, 38, 13, 35, 21, 225, 146, 163, 205, 127, 79, 63, 31, 238, 227, 167, 156, 47, 80, 30, 84, 222, 142, 240, 208, 214, 50, 90, 97, 11, 251, 203, 171, 146, 95, 91, 32, 91, 228, 234, 225, 178, 34, 28, 124, 66, 166, 203, 164, 218, 118, 68, 54, 16, 231, 232, 174, 171, 214, 107, 103, 105, 39, 181, 137, 231, 223, 46 }, 218) +
                    UrlObfuscator.decode(new int[] { 130, 108, 1, 33, 20, 200, 196, 138, 203, 112, 8, 60, 35, 23, 238, 242, 186, 180, 49, 95, 126, 10, 233, 221, 160, 188, 112, 126, 103, 12, 225, 133, 226, 152, 108, 92, 50, 20, 235, 132, 173, 151, 109, 76, 4, 44, 24, 232, 206, 168, 151, 99, 69, 108, 7, 184, 212, 232, 150, 60, 77, 116, 15, 224, 135, 171, 211, 105, 75, 59, 94, 249, 152 }, 235) +
                    UrlObfuscator.decode(new int[] { 138, 122, 72, 121, 16, 242, 206, 232, 146, 102, 92, 50, 4, 230, 193, 163, 196, 104, 3, 50, 30, 230, 212, 229, 140, 62, 68, 52, 14, 28, 234, 212, 179, 149, 50, 87, 113, 12, 248, 136, 153, 146, 102, 89, 126, 2, 239, 213, 228, 219, 38, 100, 41, 19, 238, 139, 169, 138, 108, 9, 114, 106, 75, 177, 210, 242, 211, 34, 74, 50, 2, 224, 198, 189, 210, 57, 94, 115, 95, 187, 147, 236, 218, 46, 18, 96, 65, 172, 143, 173, 204, 117, 79, 108, 42, 15, 245, 213, 189, 209, 41, 1, 127, 78, 233, 136, 160, 148, 100, 90, 60, 3, 172, 140, 233, 206, 35, 79, 110, 6, 170, 209, 235, 202, 104, 55, 93, 115, 27, 178, 145, 177, 208, 116, 24, 55, 93, 168, 207, 234 }, 252) +
                    UrlObfuscator.decode(new int[] { 107, 67, 57, 66, 255, 201, 181, 198, 110, 25, 115, 89, 234, 156, 220, 182, 156, 117, 85, 116, 21, 253, 217, 177, 129, 124, 8, 57, 90, 187, 134, 181, 155, 109, 89, 106, 10, 181, 215, 167, 151, 119, 70, 106, 6, 229, 235, 253, 178, 145, 107, 79, 45, 29, 243, 229, 161, 141, 127, 87, 121, 19, 231, 207, 164, 130, 80, 65, 20, 65, 169, 196, 164, 135, 104, 69, 51, 15, 10, 240, 217, 159, 148, 118, 86, 42, 94, 173, 220, 178, 219, 113, 23, 118, 12, 160, 204, 242, 214, 58, 7, 113, 94, 175, 215, 161, 151, 119, 83, 46, 127, 22, 248, 196, 243, 153, 48, 3, 42 }, 269) +
                    UrlObfuscator.decode(new int[] { 104, 92, 46, 91, 238, 132, 168, 150, 100, 70, 49, 91, 245, 212, 164, 172, 97, 64, 60, 30, 254, 204, 172, 180, 114, 92, 40, 6, 170, 197, 175, 188, 139, 112, 89, 53, 14, 183, 218, 184, 146, 108, 72, 47, 22, 254, 211, 186, 131, 104, 66, 63, 68, 237, 199, 164, 147, 104, 65, 45, 22, 196, 204, 218, 179, 152, 114, 79, 115, 87, 251, 216, 186, 154, 102, 26, 105 }, 286) +
                    UrlObfuscator.decode(new int[] { 93, 53, 90, 229, 150, 233, 198, 61, 2, 121, 83, 176, 130, 179, 200, 119, 15, 115, 76, 180, 152, 136, 244, 137, 50, 92, 113, 73, 182, 134, 231, 193, 62, 71, 124, 19, 185, 128, 252, 216, 57, 21, 122, 71, 190, 142, 180, 128, 112, 86, 48, 15, 160, 184, 157, 237, 201, 43, 15, 105, 77, 176, 141 }, 52) +
                    UrlObfuscator.decode(new int[] { 55, 1, 247, 215, 179, 142, 223, 57, 30, 58, 29, 252, 223, 190, 145, 49, 14, 41, 72 }, 69) +
                    UrlObfuscator.decode(new int[] { 32, 20, 230, 147, 161, 148, 126, 75, 12, 10, 177, 205, 191, 135, 107, 83, 47, 10, 234, 139, 235, 154, 116, 109, 71, 38, 10, 250, 200, 249, 144, 42, 70, 60, 23, 248, 240, 182, 216, 38, 21, 36, 10, 163, 194, 232, 213, 58, 81, 44, 10, 231, 205, 182, 206, 160, 65, 92, 50, 31, 232, 214, 177, 147, 84, 82, 24, 18, 225, 197, 249, 148, 121, 68, 34, 15, 229, 222, 230, 184, 89, 68, 42, 7, 240, 206, 169, 187, 188, 122, 112, 58, 9, 237, 133, 191, 205, 84, 90, 55, 0, 254, 217, 171, 172, 127, 69, 47, 13, 236, 134, 166, 150, 117, 72, 58, 50, 224, 199, 218, 156, 156, 127, 80, 61, 11, 247, 194, 184, 145, 60, 91, 123, 74, 237, 210, 173, 140, 120, 72, 34, 65, 237, 142, 189, 152, 121, 24 }, 86) +
                    UrlObfuscator.decode(new int[] { 20, 227, 203, 160, 161, 101, 9, 105, 100 }, 103) +
                    UrlObfuscator.decode(new int[] { 17, 241, 158, 244, 131, 122, 92, 53, 31, 248, 128, 146, 179, 106, 68, 45, 26, 232, 207, 161, 166, 100, 96, 46, 21, 17, 250, 148, 167, 140, 115, 87, 60, 24, 225, 155, 139, 172, 115, 95, 52, 29, 225, 196, 168, 169, 109, 107, 39, 18, 232, 193, 249, 151, 112, 84, 37, 100, 26, 242, 223, 174, 151, 124, 86, 35, 88, 244, 208, 183, 183, 103, 85, 33, 26, 193, 197, 184, 158, 108, 70, 34, 20, 173, 131, 165, 141, 98, 85, 76, 55, 19, 187, 151, 169, 156, 118, 83, 20, 18, 184, 199, 160, 132, 117, 6, 117 }, 120) +
                    UrlObfuscator.decode(new int[] { 255, 201, 181, 198, 105, 69, 48, 22, 213, 211, 130, 238, 198, 120, 84, 57, 12, 245, 210, 184, 129, 58, 82, 54, 21, 213, 217, 171, 131, 120, 103, 35, 26, 252, 194, 168, 128, 118, 11, 101, 21, 239, 234, 221, 181, 143, 111, 91, 43, 12, 176, 154, 179, 129, 125, 81, 37, 25, 224, 192, 229, 197, 112, 92, 40, 26, 167, 200, 248, 160, 98, 86, 36, 78, 17, 241, 202, 244, 210, 33, 80, 62, 95, 248, 152, 184, 146, 97, 69, 4, 28, 178, 149, 252, 219, 35, 91, 45, 19, 243, 215, 170, 216, 110, 64, 51, 43, 42, 238, 129, 181, 193, 106, 93, 57, 18, 215, 211, 251, 219, 42, 77, 99, 21, 238, 205, 187, 158, 124, 90, 34, 92, 241, 214, 182, 135, 45, 80, 94, 45, 14, 245, 205, 191, 195, 108, 69, 35, 16, 233, 154, 233, 134, 121, 65, 42, 2, 251, 133, 171, 141, 108, 98, 48, 0, 234, 215, 142, 136, 115, 107, 91, 51, 25, 233, 146, 254, 138, 114, 69, 60, 14, 246, 149, 253, 131, 106, 64, 41, 46, 236, 131, 242 }, 137) +
                    UrlObfuscator.decode(new int[] { 238, 203, 161, 140, 96, 84, 38, 83, 240, 214, 132, 210, 96, 88, 32, 7, 177, 223, 169, 149, 38, 71, 35, 48, 225, 201, 165, 187, 195, 123, 73, 53, 25, 237, 209, 184, 152, 61, 29, 40, 27, 247, 152, 173, 137, 89, 5, 57, 15, 253, 221, 181, 136, 62, 70, 36, 54, 188, 211, 218, 170, 169, 117, 86, 63, 22, 237, 195, 254, 147, 97, 93, 49, 5, 249, 192, 160, 197, 37, 80, 40, 14, 220, 154, 168, 144, 104, 79, 121, 18, 229, 241, 218, 159, 155, 51, 19, 98, 5, 187, 135, 224, 196, 58, 9, 44, 75 }, 154) +
                    UrlObfuscator.decode(new int[] { 197, 175, 158, 40, 106, 51, 17, 229, 215, 171, 142, 110, 80, 92, 46, 25, 233, 204, 188, 138, 63, 84, 50, 39, 240, 218, 180, 148, 38, 0, 34, 14, 248, 207, 187, 158, 98, 14, 33, 11, 224, 215, 172, 133, 145, 106, 19, 56, 20, 249, 204, 181, 146, 120, 65, 17, 31, 247, 220, 181, 129, 122, 1, 55, 10, 254, 221, 186, 142, 100, 80, 48, 6, 241, 155, 180, 173, 139, 120, 16, 58, 14, 237, 202, 190, 148, 96, 64, 54, 52, 248, 220, 187, 139, 127, 22, 16, 77, 234, 196, 166, 149, 118, 3, 111, 69, 242, 212, 198, 178, 152, 59, 23, 125, 29, 249, 195, 183, 216, 96, 91, 55, 28, 245, 136, 147, 144, 37, 16 }, 171) +
                    UrlObfuscator.decode(new int[] { 213, 189, 210, 125, 87, 52, 3, 248, 209, 189, 134, 63, 82, 32, 10, 244, 133, 165, 143, 126, 8, 10, 19, 241, 197, 183, 139, 110, 78, 112, 60, 14, 249, 201, 172, 156, 106, 31, 52, 18, 199, 208, 186, 148, 116, 6, 96, 2, 238, 216, 175, 155, 126, 66, 110, 1, 235, 192, 183, 140, 101, 113, 74, 115, 30, 244, 222, 160, 212, 108, 87, 33, 0, 225, 219, 179, 133, 123, 75, 62, 86, 255, 216, 188, 141, 43, 71, 49, 16, 241, 203, 163, 149, 139, 123, 123, 53, 23, 238, 220, 170, 205, 77, 18, 55, 31, 243, 194, 163, 200, 34, 10, 63, 31, 243, 197, 173, 192, 91, 88, 109, 88, 255, 194, 161, 171, 157, 117, 20, 62, 83, 226, 197, 170 }, 188) +
                    UrlObfuscator.decode(new int[] { 176, 145, 104, 75, 61, 11, 239, 142, 160, 205, 120, 95 }, 205) +
                    UrlObfuscator.decode(new int[] { 170, 143, 101, 64, 51, 31, 176, 150, 161, 156, 122, 87, 61, 6, 190, 240, 145, 140, 98, 79, 56, 6, 225, 195, 142, 132, 116, 87, 43, 2, 243, 221, 209, 168, 146, 127, 19, 34, 15, 254, 216, 177, 155, 100, 28, 14, 47, 238, 192, 169, 158, 100, 67, 45, 32, 230, 214, 177, 141, 96, 81, 3, 15, 10, 240, 217, 225, 143, 104, 76, 61, 76 }, 222) +
                    UrlObfuscator.decode(new int[] { 139, 97, 78, 57, 6, 239, 199, 188, 201, 103, 65, 32, 38, 244, 196, 174, 171, 178, 116, 79, 47, 31, 247, 221, 165, 222, 50, 87, 63, 27, 242, 219, 232, 194, 107, 89, 37, 9, 253, 193, 168, 136, 45, 65, 106, 25 }, 239) +
                    "var t=e.target;var el=t&&t.closest?t.closest('button,a,[role=\"button\"],input[type=\"button\"],input[type=\"submit\"]'):null;" +
                    UrlObfuscator.decode(new int[] { 105, 121, 22, 56, 16, 189, 156, 174, 145, 121, 82, 58, 3, 189, 243, 191, 148, 125, 65, 36, 8, 201, 216, 160, 140, 96, 67, 99, 66, 194, 204, 165, 146, 144, 119, 89, 30, 9, 243, 221, 191, 146, 56, 67, 61, 17, 224, 208, 164, 138, 39, 86, 13, 5, 238, 219, 167, 142, 98, 103, 54, 10, 230, 198, 165, 241, 136, 116, 94, 41, 27, 237, 221, 255, 223, 46, 73 }, 256) +
                    UrlObfuscator.decode(new int[] { 108, 28, 59, 28, 248, 201, 226, 209, 116, 85, 36, 7, 241, 199, 171, 202, 100, 9, 68, 35 }, 273) +
                    // Polyfills the Web Share API on top of the real Android
                    // share sheet -- most WebView builds don't implement
                    // navigator.share at all, so a page's own "Share" button
                    // either does nothing or falls back to a hand-rolled
                    // copy-link menu instead of the native chooser. Only
                    // installed when the page doesn't already have a working
                    // navigator.share (some newer WebView versions do).
                    UrlObfuscator.decode(new int[] { 86, 51, 25, 4, 247, 219, 244, 140, 115, 87, 60, 24, 225, 155, 149, 157, 118, 67, 63, 6, 234, 239, 190, 130, 110, 78, 45, 65, 160, 228, 170, 135, 112, 78, 41, 59, 60, 239, 213, 191, 157, 124, 22, 36, 30, 244, 198, 182, 212, 55, 17, 33, 15, 251, 197, 172, 139, 125, 71, 53, 72, 246, 204, 162, 144, 100, 9, 68 }, 290) +
                    UrlObfuscator.decode(new int[] { 86, 54, 0, 252, 211, 178, 134, 126, 66, 97, 29, 229, 205, 185, 143, 52, 78, 50, 8, 230, 208, 170, 141, 111, 8, 91, 63, 9, 253, 146, 161 }, 56) +
                    UrlObfuscator.decode(new int[] { 45, 9, 243, 199, 248, 128, 98, 86, 32, 28, 3, 229, 192, 231 }, 73) +
                    UrlObfuscator.decode(new int[] { 46, 11, 225, 204, 151, 155, 112, 65, 61, 24, 244, 237, 188, 132, 104, 76, 47, 71, 251, 207, 167, 151, 97, 11, 17, 21, 242, 246, 208, 186, 212, 127, 91, 45, 25, 185, 194, 188, 128, 127, 87, 45, 12, 168, 137, 228, 192, 88, 94, 59, 1, 233, 193, 237, 128, 98, 86, 32, 78, 11, 251, 197, 168, 135, 102, 30, 127, 94, 186, 230, 160, 129, 123, 95, 55, 71, 234, 204, 184, 138, 36, 92, 58, 11, 250, 217, 227, 196, 43, 8, 123 }, 90) +
                    UrlObfuscator.decode(new int[] { 25, 239, 221, 189, 149, 104, 5, 20, 17, 237, 204, 169, 172, 155, 51, 78, 62, 9, 246, 212, 161, 147, 61, 29, 104, 15, 242, 209, 187, 141, 101, 4, 46, 67, 242, 218, 162, 146, 112, 86, 45, 66, 209, 210, 208, 179, 148, 111, 94, 116, 11, 253, 221, 179, 150, 96, 27, 55, 88, 171, 210, 179, 214 }, 107) +
                    UrlObfuscator.decode(new int[] { 18, 250, 204, 176, 159, 118, 66, 58, 6, 189, 209, 176, 158, 92, 70, 44, 30, 238, 151, 175, 157, 105, 69, 49, 13, 236, 204, 233, 201, 132, 108, 88, 40, 14, 232, 215, 248, 131, 100, 64, 49, 72, 239, 138 }, 124) +
                    UrlObfuscator.decode(new int[] { 240, 209, 168, 139, 125, 75, 47, 78, 224, 141, 184, 159 }, 141) +
                    // Reports the scrollTop of whichever element just
                    // scrolled -- document or any nested panel -- so native
                    // knows whether a pull-to-refresh gesture is actually
                    // safe (see SwipeRefreshLayout override above). Scroll
                    // events don't bubble, so this has to be a capture
                    // listener on window to see scrolling from any
                    // descendant, not just the document itself. Throttled
                    // with a trailing rAF flag so a fast scroll doesn't
                    // spam the JS bridge with a call per pixel.
                    UrlObfuscator.decode(new int[] { 234, 207, 165, 128, 115, 95, 112, 86, 225, 220, 186, 151, 125, 70, 126, 48, 209, 204, 162, 143, 120, 70, 33, 3, 213, 198, 182, 140, 110, 77, 2, 48, 11, 243, 216, 242, 129, 110, 81, 57, 18, 250, 195, 253, 173, 78, 81, 33, 10, 255, 195, 162, 142, 90, 75, 53, 9, 233, 200, 129, 141, 116, 78, 91, 99, 9, 238, 206, 191, 194 }, 158) +
                    UrlObfuscator.decode(new int[] { 217, 175, 159, 44, 91, 47, 7, 236, 206, 168, 130, 57, 69, 35, 13, 243, 250, 146, 177, 157, 104, 78, 9, 23, 228, 139, 179, 149, 127, 65, 52, 75 }, 175) +
                    UrlObfuscator.decode(new int[] { 183, 182, 144, 121, 83, 44, 84, 248, 220, 179, 179, 99, 81, 61, 6, 221, 217, 188, 154, 104, 66, 46, 24, 161, 143, 180, 133, 119, 75, 47, 14, 166, 140, 217, 171, 147, 127, 79, 51, 22, 246, 159, 179, 220, 111 }, 192) +
                    UrlObfuscator.decode(new int[] { 184, 150, 39, 94, 40, 2, 239, 195, 167, 143, 46, 84, 32, 16, 246, 208, 175, 219, 143, 123, 83, 56, 18, 244, 222, 229, 131, 100, 64, 49, 72 }, 209) +
                    UrlObfuscator.decode(new int[] { 144, 100, 81, 74, 59, 14, 232, 250, 180, 144, 117, 86, 34, 28, 251, 221, 148, 131, 113, 66, 43, 69, 234, 222, 164, 138, 124, 78, 41, 11, 172, 138, 185, 145, 101, 113, 90, 52, 18, 252, 135, 191, 153, 123, 69, 48, 79 }, 226) +
                    UrlObfuscator.decode(new int[] { 133, 115, 67, 112, 10, 226, 144, 228, 142, 36, 93, 41, 21, 225, 192, 176, 197, 36, 68, 110, 43, 31, 239, 219, 190, 142, 55, 86, 56, 18, 240, 224, 170, 130, 116, 13, 114, 83, 188, 133, 244, 143, 39, 92, 38, 20, 226, 193, 183, 216, 101, 79, 92, 43, 16, 249, 213, 174, 215, 107, 84, 36, 26, 248, 223, 187, 159, 119, 106, 34, 8, 225, 206, 164, 157, 51 }, 243) +
                    UrlObfuscator.decode(new int[] { 114, 66, 48, 65, 244, 240, 206, 224, 153, 119, 5, 60, 20, 185, 197, 182, 134, 124, 94, 61, 36, 224, 222, 247, 220, 48 }, 260) +
                    UrlObfuscator.decode(new int[] { 99, 85, 33, 82, 225, 223, 188, 211, 121, 67, 59, 84, 185, 147, 174, 128, 45, 84, 44, 17, 160, 157, 130, 178, 156, 111, 79, 10, 22, 235, 158, 173, 153, 117, 64, 38, 33, 255, 220, 243, 157, 99, 88, 113, 0, 238, 143, 177, 140, 106, 71, 45, 22, 174, 222, 208, 185, 142, 116, 83, 61, 58, 229, 223, 177, 147, 118, 20, 119, 49, 225, 202, 191, 131, 98, 78, 11, 26, 238, 194, 162, 129, 45, 80, 36, 16, 16, 236, 201, 143, 152, 104, 86, 52, 27, 194, 218, 164, 218, 105, 112, 62, 11, 252, 194, 165, 143, 72, 91, 33, 3, 225, 192, 234, 145, 103, 81, 47, 45, 10, 206, 223, 169, 149, 117, 84, 3, 25, 229, 156, 167, 157, 97, 76, 127, 71, 182, 209, 182 }, 277) +
                    UrlObfuscator.decode(new int[] { 91, 108, 95, 254, 142, 181, 146, 138, 123, 20, 103, 6, 231, 218, 185, 131, 117, 93, 124, 22, 187, 202, 173 }, 294) +
                    UrlObfuscator.decode(new int[] { 65, 114, 82, 176, 131 }, 60),
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
                if (!UrlObfuscator.decode(new int[] { 10, 41, 223 }, 77).equalsIgnoreCase(request.getMethod())) {
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
                    showOffline(UrlObfuscator.decode(new int[] { 22, 41, 200, 235, 250 }, 94) + status, request.getUrl().toString());
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
                int errBg = Color.parseColor(isOfflineError(description) ? UrlObfuscator.decode(new int[] { 76, 200, 235, 138, 173, 76, 111 }, 111) : UrlObfuscator.decode(new int[] { 163, 175, 255, 237, 189, 43, 120 }, 128));
                errorView.setBackgroundColor(errBg);
                errorWeb.setBackgroundColor(errBg);
                errorWeb.loadDataWithBaseURL(UrlObfuscator.decode(new int[] { 249, 196, 187, 158, 126, 22, 100, 69, 236, 218, 181, 137, 119, 10, 47, 13, 226, 193, 211, 241 }, 145),
                    renderErrorPage(description, failingUrl), UrlObfuscator.decode(new int[] { 214, 164, 152, 139, 49, 85, 40, 22, 246 }, 162), UrlObfuscator.decode(new int[] { 230, 134, 183, 61, 23 }, 179), null);
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
                popupSettings.setUserAgentString(popupDefaultUA.replace(UrlObfuscator.decode(new int[] { 255, 195, 117, 87 }, 196), "").replaceAll("Version/[0-9.]+\s", ""));
                popupWebView.setVerticalScrollBarEnabled(false);
                popupWebView.setHorizontalScrollBarEnabled(false);

                final Dialog popupDialog = new Dialog(MainActivity.this, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
                // Loading overlay: same bouncing dots + colors as the main
                // navigation overlay, shown over the popup until its page has
                // painted, so there is never an empty black/white screen while
                // a link (sign-in, Telegram, any website) is still loading.
                final FrameLayout popupRoot = new FrameLayout(MainActivity.this);
                popupRoot.setBackgroundColor(Color.parseColor(UrlObfuscator.decode(new int[] { 246, 196, 35, 2, 97, 64, 191 }, 213)));
                popupRoot.addView(popupWebView, new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
                final FrameLayout popupLoader = new FrameLayout(MainActivity.this);
                popupLoader.setBackgroundColor(Color.parseColor(UrlObfuscator.decode(new int[] { 197, 53, 20, 115, 82, 177, 144 }, 230)));
                final BouncingDotsView popupDots = new BouncingDotsView(MainActivity.this, Color.parseColor(UrlObfuscator.decode(new int[] { 212, 80, 7, 18, 65, 215, 244 }, 247)));
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
                                UrlObfuscator.decode(new int[] { 124, 66, 62, 17, 171, 203, 182, 140, 108 }, 264), UrlObfuscator.decode(new int[] { 76, 108, 17, 91, 173 }, 281), failed);
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
                                renderErrorPage(UrlObfuscator.decode(new int[] { 103, 26, 57, 220, 139 }, 47) + status, failed),
                                UrlObfuscator.decode(new int[] { 52, 58, 6, 233, 147, 179, 142, 116, 84 }, 64), UrlObfuscator.decode(new int[] { 4, 36, 201, 131, 245 }, 81), failed);
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
                        if (scheme != null && !scheme.equals(UrlObfuscator.decode(new int[] { 10, 245, 212, 207 }, 98)) && !scheme.equals(UrlObfuscator.decode(new int[] { 27, 230, 197, 160, 156 }, 115))) {
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
        if (Intent.ACTION_SEND.equals(action) && UrlObfuscator.decode(new int[] { 240, 198, 186, 149, 47, 111, 82, 60, 21, 245 }, 132).equals(intent.getType())) {
            String shared = intent.getStringExtra(Intent.EXTRA_TEXT);
            if (shared != null && !shared.isEmpty()) {
                try {
                    return baseUrl() + UrlObfuscator.decode(new int[] { 170, 199, 187, 147, 99, 85, 43, 49, 249, 201, 179, 158, 52 }, 149) + java.net.URLEncoder.encode(shared, UrlObfuscator.decode(new int[] { 243, 145, 162, 46, 26 }, 166));
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

        String shortcutAction = intent.getStringExtra(UrlObfuscator.decode(new int[] { 196, 190, 154, 102, 71, 49, 4, 228, 240, 175, 142, 120, 66, 37, 7 }, 183));
        if (UrlObfuscator.decode(new int[] { 186, 130, 106, 74, 37, 7 }, 200).equals(shortcutAction)) {
            webView.getSettings().setCacheMode(WebSettings.LOAD_NO_CACHE);
            webView.reload();
            webView.getSettings().setCacheMode(WebSettings.LOAD_CACHE_ELSE_NETWORK);
            return;
        }
        if (UrlObfuscator.decode(new int[] { 177, 151, 122, 83 }, 217).equals(shortcutAction)) {
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
                blobTmp = File.createTempFile(UrlObfuscator.decode(new int[] { 136, 101, 71, 37 }, 234), UrlObfuscator.decode(new int[] { 213, 110, 84, 40 }, 251), getCacheDir());
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
                Toast.makeText(MainActivity.this, UrlObfuscator.decode(new int[] { 72, 68, 61, 7, 228, 200, 167, 129, 36, 69, 35, 8, 236, 250, 218, 231, 220, 120, 85, 44, 20, 243, 150, 187, 155, 103, 18, 35, 21, 238, 202, 237, 152, 99, 67, 58, 72, 225, 207, 169, 129 }, 268), Toast.LENGTH_LONG).show();
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
                    sendIntent.setType(UrlObfuscator.decode(new int[] { 105, 89, 35, 14, 182, 200, 187, 151, 124, 90 }, 285));
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
                String safeTitle = (title == null || title.trim().isEmpty()) ? UrlObfuscator.decode(new int[] { 114, 62, 20, 232, 206, 238, 165, 121, 73 }, 51) : title;
                String safeBody = body == null ? "" : body;
                NotificationCompat.Builder notification = new NotificationCompat.Builder(MainActivity.this, UrlObfuscator.decode(new int[] { 32, 6, 228, 192, 181, 179, 138 }, 68))
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
            if (hex == null || !hex.matches(UrlObfuscator.decode(new int[] { 11, 87, 187, 233, 144, 221, 73, 79, 96, 10, 187, 135, 240, 181, 124, 16, 56, 24, 216, 227, 236, 166, 158, 51, 91, 108, 86, 163, 228, 163, 196, 107, 28, 112 }, 85))) return;
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
            if (hex == null || !hex.matches(UrlObfuscator.decode(new int[] { 56, 166, 255, 130, 207, 71, 65, 18, 56, 77, 177, 130, 135, 130, 46, 74, 114 }, 102))) return;
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
                        Uri.parse(UrlObfuscator.decode(new int[] { 7, 247, 214, 191, 146, 117, 84, 106 }, 119) + getPackageName()));
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
            UrlObfuscator.decode(new int[] { 160, 193, 179, 139, 103, 87, 43, 14, 238, 183, 151, 166, 136, 105, 67, 34 }, 136) +
            UrlObfuscator.decode(new int[] { 239, 217, 165, 214, 101, 9, 55, 29, 242, 197, 162, 139, 99, 88, 101, 13, 236, 220, 130, 138, 96, 73, 38, 12, 245, 226, 198, 151, 153, 52, 28, 46, 24, 250, 231, 183, 155, 113, 95, 29, 1, 228, 198, 161, 131, 127, 12, 99, 82 }, 153) +
            UrlObfuscator.decode(new int[] { 195, 175, 192, 38, 86, 57, 24, 243, 140, 169, 137, 155, 122, 88, 50, 7, 230, 152, 168, 217, 117, 89, 53, 0, 225, 253, 185, 156, 122, 3, 47, 4, 228, 221, 169, 142, 104, 86, 108, 68, 235, 210, 237, 190, 157, 105, 85, 45, 31, 190, 145, 254, 132, 112, 64, 38, 0, 255, 139 }, 170) +
            UrlObfuscator.decode(new int[] { 205, 187, 139, 56, 86, 107, 17, 251, 208, 167, 156, 117, 65, 58, 67, 237, 200, 190, 128, 126, 66, 3, 9, 225, 206, 167, 143, 116, 36, 87, 59, 84, 186, 219, 165, 132, 118, 11, 104, 73, 247, 221, 178, 133, 98, 75, 35, 24, 165, 200, 166, 140, 126, 90, 57, 69, 243, 140, 162, 143, 145, 106, 92, 53, 21, 233, 145, 185, 222, 63, 71, 49, 7, 231, 195, 190, 212 }, 187) +
            UrlObfuscator.decode(new int[] { 186, 138, 120, 9, 60, 90, 174, 196, 234, 151, 99, 70, 14, 62, 19, 248, 192, 167, 221, 62, 17, 121, 2, 250, 248, 188, 133, 116, 66, 12, 15, 254, 201, 227, 195, 50 }, 204) +
            UrlObfuscator.decode(new int[] { 180, 154, 51, 78, 120, 69, 170, 145, 188, 154, 99, 71, 37, 87, 169, 136, 185, 205, 54, 23, 110, 28, 226, 222, 177, 133, 113, 71, 32, 71, 89, 184, 201, 253, 198, 39, 30, 43, 18, 250, 208, 183, 135, 53, 23, 118, 78, 239, 131, 165, 152, 73, 70, 38, 19, 227, 203, 176, 166, 102, 72, 52, 62, 28, 241, 217, 242, 136, 124, 76, 34, 4, 251, 143 }, 221) +
            UrlObfuscator.decode(new int[] { 152, 108, 94, 107, 24, 180, 201, 233, 129, 96, 80, 1, 13, 244, 206, 219, 183, 147, 123, 120, 54, 16, 253, 217, 162, 167, 113, 80, 38, 89, 185, 148 }, 238) +
            UrlObfuscator.decode(new int[] { 137, 127, 79, 124, 13, 236, 132, 175, 158, 120, 81, 59, 4, 188, 199, 185, 156, 123, 76, 32, 61, 227, 204, 191, 151, 105, 87, 48, 88, 244, 192, 178, 255, 136, 117, 1, 45, 12, 166, 206, 161, 216, 125, 81, 58, 21, 249, 196, 245, 153, 100, 66, 47, 5, 254, 134, 174, 136, 107, 65, 49, 42, 228, 201, 216, 182, 137, 39 }, 255) +
            UrlObfuscator.decode(new int[] { 102, 78, 60, 77, 252, 202, 174, 212, 57, 17, 125, 19, 229, 209, 226, 133, 101, 115, 74, 60, 65, 171, 129 }, 272) +
            UrlObfuscator.decode(new int[] { 72, 38, 119, 12, 179, 222, 180, 142, 109, 87, 58, 72, 227, 220, 254, 130, 112, 84, 102, 10, 232, 192, 191, 139, 52, 101, 38, 18, 237, 138, 174, 139, 111, 8, 77, 112, 31, 243, 207, 174, 150, 117, 26, 126, 3, 252, 158, 162, 144, 116, 6, 98, 32, 237, 223, 162, 199, 101, 70, 62, 77, 182, 151, 238, 151, 104, 53, 14, 115, 77, 163, 147, 240, 195 }, 289) +
            UrlObfuscator.decode(new int[] { 82, 58, 6, 241, 147, 187, 151, 56, 93, 96, 25, 227, 219, 246, 153, 105, 67, 111, 1, 225, 207, 182, 128, 61, 50, 115, 60, 8, 243, 148, 180, 145, 121, 30, 37, 21, 247, 159, 163, 222, 123, 65, 61, 64, 198, 203, 189, 128, 41, 75, 36, 28, 171, 147, 247, 204, 137, 118, 23, 108, 85, 171, 137, 241, 222, 45 }, 55) +
            UrlObfuscator.decode(new int[] { 33, 1, 174, 193, 161, 143, 118, 64, 105, 36, 8, 252, 206, 251, 137, 122, 5, 54, 88, 246, 216, 188, 129, 116, 67, 59, 70, 170, 130, 168, 133, 103, 92, 34, 8, 241, 136, 237, 150, 96, 66, 111, 63, 19, 249, 215, 246, 215, 119, 71, 34, 28, 251, 221, 161, 220, 115, 64, 32, 25, 233, 197, 190, 197, 83, 67, 39, 17, 229, 142, 177, 130, 114, 112, 82, 49, 81, 248, 213, 183, 140, 118, 95, 59, 17, 225, 239, 246, 217, 52 }, 72) +
            UrlObfuscator.decode(new int[] { 48, 30, 191, 197, 182, 210, 53, 65, 50, 94, 252, 205, 191, 131, 103, 70, 1, 13, 238, 193, 173, 144, 61, 81, 34, 78, 28, 242, 212, 185, 149, 110, 113, 61, 30, 241, 221, 160, 218, 105, 66, 51, 65, 253, 206, 190, 132, 102, 69, 10, 30, 174, 222, 176, 140, 114, 27, 36, 58, 18, 233, 221, 247, 152, 124, 80, 54, 0, 252, 219, 161, 200, 54, 81, 58, 26, 226, 139, 182, 195, 50, 85 }, 89) +
            UrlObfuscator.decode(new int[] { 15, 229, 219, 162, 157, 114, 77, 45, 6, 238, 215, 145, 173, 158, 110, 84, 54, 21, 218, 206, 254, 142, 96, 92, 34, 75, 244, 202, 162, 153, 109, 7, 40, 12, 224, 198, 176, 140, 107, 81, 120, 70, 225, 234, 202, 178, 219, 102, 19, 98, 5, 234 }, 106) +
            UrlObfuscator.decode(new int[] { 6, 249, 216, 172, 148, 126, 29, 49, 90, 233, 204, 173, 198, 38, 4, 119 }, 123), null);
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
            UrlObfuscator.decode(new int[] { 164, 205, 191, 135, 107, 83, 47, 10, 234, 139, 235, 154, 116, 109, 71, 38, 10, 250, 200, 249, 153, 42, 82, 58, 23, 230, 223, 180, 158, 123, 0, 44, 15, 255, 195, 191, 141, 66, 74, 32, 9, 230, 204, 181, 219, 150, 120, 21, 125, 26, 230, 197, 185, 202, 43, 8, 48, 28, 241, 196, 189, 138, 96, 89, 98, 9, 229, 205, 177, 206, 116, 64, 48, 22, 240, 207, 224, 242, 207, 38 }, 140) +
            UrlObfuscator.decode(new int[] { 235, 221, 169, 218, 109, 5, 127, 23, 187, 192, 178, 149, 95, 81, 34, 11, 241, 208, 236, 205, 32, 6, 51, 9, 201, 203, 180, 135, 115, 99, 94, 45, 24, 180, 146, 225 }, 157) +
            UrlObfuscator.decode(new int[] { 199, 171, 196, 127, 11, 116, 85, 160, 207, 171, 148, 118, 86, 102, 70, 89, 234, 156, 225, 198, 61, 77, 61, 15, 226, 212, 166, 150, 115, 22, 118, 73, 250, 140, 241, 214, 45, 90, 45, 11, 227, 198, 176, 196, 36, 7, 97, 62, 80, 244, 207, 152, 149, 119, 76, 50, 24, 225, 241, 183, 155, 101, 81, 45, 2, 232, 133, 185, 143, 125, 93, 53, 8, 165, 137, 242, 217 }, 174) +
            UrlObfuscator.decode(new int[] { 201, 191, 143, 60, 73, 103, 24, 182, 208, 179, 129, 86, 92, 39, 31, 244, 198, 160, 138, 79, 71, 35, 12, 230, 211, 148, 128, 103, 87, 106, 72, 187, 237, 219, 169, 137, 105, 84, 121, 53, 246, 194, 189, 218, 97, 93, 36, 30, 235, 134, 191, 194, 105, 69, 61, 28, 232, 203, 239, 213, 51, 18, 104, 79, 78, 174, 141, 231, 134, 121, 88, 44, 20, 254, 157, 177, 218, 105, 67, 53, 27, 251, 223, 162, 203, 39, 24, 115, 26, 251, 140, 236, 202, 57 }, 191),
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
        if (t.isEmpty() || UrlObfuscator.decode(new int[] { 190, 154, 98, 65 }, 208).equals(t)) return null;
        try {
            if (t.startsWith("#")) return Color.parseColor(t) | 0xFF000000;
            if (!t.startsWith(UrlObfuscator.decode(new int[] { 147, 103, 125 }, 225))) return null;
            String inner = t.substring(t.indexOf('(') + 1);
            String[] parts = inner.split(UrlObfuscator.decode(new int[] { 169, 79, 0, 98, 87, 163, 241, 224 }, 242));
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
        int scrim = Color.parseColor(UrlObfuscator.decode(new int[] { 32, 19, 117, 82, 75, 171, 254 }, 259));
        try {
            if (topScrim != null && topScrim.getBackground() instanceof ColorDrawable) {
                scrim = ((ColorDrawable) topScrim.getBackground()).getColor() | 0xFF000000;
            }
        } catch (Exception ignored) {}
        Integer bgC = o == null ? null : parseCssColor(o.optString("bg", null));
        Integer thC = o == null ? null : parseCssColor(o.optString(UrlObfuscator.decode(new int[] { 96, 91, 55, 28, 245 }, 276), null));
        Integer btC = o == null ? null : parseCssColor(o.optString(UrlObfuscator.decode(new int[] { 71, 48, 13 }, 293), null));
        double btnR = o == null ? -1 : o.optDouble(UrlObfuscator.decode(new int[] { 89, 46, 23, 202 }, 59), -1);

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
                accent = dark ? Color.parseColor(UrlObfuscator.decode(new int[] { 111, 45, 184, 239, 250, 161, 49 }, 76)) : Color.parseColor(UrlObfuscator.decode(new int[] { 126, 77, 172, 139, 238, 201, 84 }, 93));
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
        p.title = cardDark ? Color.WHITE : Color.parseColor(UrlObfuscator.decode(new int[] { 77, 188, 152, 250, 222, 56, 105 }, 110));
        p.sub = mixColor(p.title, p.cardTop, 0.35f);
        p.border = mixColor(accent, p.cardTop, 0.45f);
        p.accentTop = mixColor(accent, Color.WHITE, 0.12f);
        p.accentBottom = mixColor(accent, Color.BLACK, 0.12f);
        p.accentText = lumOf(accent) < 0.55 ? Color.WHITE : Color.parseColor(UrlObfuscator.decode(new int[] { 92, 175, 137, 237, 207, 43, 120 }, 127));
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
        title.setText(nativeDecryptScreen(EXIT_DIALOG_TITLE_ID));
        title.setTextColor(pal.title);
        title.setTextSize(18f);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setGravity(Gravity.CENTER);
        card.addView(title, new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        TextView msg = new TextView(this);
        msg.setText(nativeDecryptScreen(EXIT_DIALOG_MESSAGE_ID));
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
        exit.setText(nativeDecryptScreen(EXIT_DIALOG_EXIT_BTN_ID));
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
        stay.setText(nativeDecryptScreen(EXIT_DIALOG_STAY_BTN_ID));
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
                cancel.setText(UrlObfuscator.decode(new int[] { 211, 238, 128, 174, 73, 103 }, 144));
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
            boolean chat = h.equals(UrlObfuscator.decode(new int[] { 213, 238, 178, 155 }, 161)) || h.endsWith(UrlObfuscator.decode(new int[] { 156, 165, 222, 98, 75 }, 178))
                || h.equals(UrlObfuscator.decode(new int[] { 183, 135, 109, 69, 88, 44, 28, 241, 149, 183, 156 }, 195)) || h.equals(UrlObfuscator.decode(new int[] { 160, 150, 126, 84, 55, 29, 239, 192, 226, 143, 101, 78 }, 212))
                || h.equals(UrlObfuscator.decode(new int[] { 146, 101, 13, 47, 4 }, 229)) || h.equals(UrlObfuscator.decode(new int[] { 151, 101, 93, 125, 5, 249, 209, 187, 157, 108, 92, 59, 68, 234, 199, 170 }, 246));
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
        if (scheme.equals(UrlObfuscator.decode(new int[] { 111, 82, 49, 20 }, 263)) || scheme.equals(UrlObfuscator.decode(new int[] { 112, 67, 34, 5, 231 }, 280)) || scheme.equals(UrlObfuscator.decode(new int[] { 79, 33, 11, 227 }, 297))
                || scheme.equals(UrlObfuscator.decode(new int[] { 94, 60, 18, 233, 207 }, 63)) || scheme.equals(UrlObfuscator.decode(new int[] { 52, 14, 250, 204 }, 80)) || scheme.equals(UrlObfuscator.decode(new int[] { 3, 236, 240, 220 }, 97))
                || scheme.equals(UrlObfuscator.decode(new int[] { 24, 240, 198, 174, 157, 110, 94, 34, 26, 253 }, 114)) || scheme.equals(UrlObfuscator.decode(new int[] { 224, 205, 175, 148, 154, 112, 73 }, 131))) {
            return false;
        }
        try {
            Intent intent;
            if (scheme.equals(UrlObfuscator.decode(new int[] { 253, 221, 166, 148, 126, 91 }, 148))) {
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
                if (scheme.equals(UrlObfuscator.decode(new int[] { 204, 170, 151, 103, 79, 52 }, 165))) {
                    Intent parsed = Intent.parseUri(uri.toString(), Intent.URI_INTENT_SCHEME);
                    String fallback = parsed.getStringExtra(UrlObfuscator.decode(new int[] { 212, 167, 155, 100, 65, 52, 2, 208, 200, 172, 128, 103, 72, 40, 11, 236, 249, 176, 150, 111 }, 182));
                    if (fallback != null && (fallback.startsWith(UrlObfuscator.decode(new int[] { 175, 146, 113, 84, 48, 88, 174, 143 }, 199)) || fallback.startsWith(UrlObfuscator.decode(new int[] { 176, 131, 98, 69, 110, 92, 189 }, 216)))) {
                        view.loadUrl(fallback);
                        return true;
                    }
                }
            } catch (Exception ignored) {}
            Toast.makeText(this, UrlObfuscator.decode(new int[] { 167, 103, 7, 39, 21, 244, 131, 164, 142, 117, 113, 90, 125, 8, 244, 154, 182, 136, 114, 88, 117, 0, 251, 219, 162, 208, 99, 71, 35, 7 }, 233), Toast.LENGTH_SHORT).show();
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
        String name = raw == null ? "" : raw.replaceAll("[" + (char) 92 + (char) 92 + UrlObfuscator.decode(new int[] { 213, 35, 18, 104 }, 250) + (char) 34 + UrlObfuscator.decode(new int[] { 55, 20, 53, 53 }, 267), "_").trim();
        if (name.length() > 120) name = name.substring(name.length() - 120);
        if (name.isEmpty() || name.equals(".") || name.equals("..")) name = UrlObfuscator.decode(new int[] { 120, 84, 45, 23, 244, 216, 183, 145 }, 284);
        if (name.lastIndexOf('.') <= 0 && mime != null && !mime.isEmpty()) {
            String ext = android.webkit.MimeTypeMap.getSingleton().getExtensionFromMimeType(mime);
            if (ext != null) name = name + "." + ext;
        }
        return name;
    }

    private void handleInlineDownload(String url, String contentDisposition, String mimeType) {
        try {
            String named = downloadNames.get(downloadKey(url));
            if (url.startsWith(UrlObfuscator.decode(new int[] { 86, 48, 4, 238, 148 }, 50))) {
                int comma = url.indexOf(',');
                if (comma < 0) throw new IllegalArgumentException(UrlObfuscator.decode(new int[] { 33, 3, 229, 128, 219, 191, 137, 125, 27, 15, 43, 212 }, 67));
                String meta = url.substring(5, comma);
                String payload = url.substring(comma + 1);
                String mime = meta.split(";")[0];
                if (mime.isEmpty()) mime = (mimeType != null && !mimeType.isEmpty()) ? mimeType : UrlObfuscator.decode(new int[] { 32, 22, 234, 197, 255, 159, 98, 76, 37, 5 }, 84);
                byte[] bytes;
                if (meta.contains(UrlObfuscator.decode(new int[] { 94, 230, 194, 177, 132, 54, 43 }, 101))) {
                    if (payload.indexOf('%') >= 0) payload = Uri.decode(payload);
                    bytes = android.util.Base64.decode(payload, android.util.Base64.DEFAULT);
                } else {
                    bytes = Uri.decode(payload).getBytes(UrlObfuscator.decode(new int[] { 35, 193, 242, 254, 202 }, 118));
                }
                String name = cleanDownloadName(named != null ? named : URLUtil.guessFileName(url, contentDisposition, mime), mime);
                File tmp = File.createTempFile("dl", UrlObfuscator.decode(new int[] { 169, 210, 168, 148 }, 135), getCacheDir());
                java.io.FileOutputStream fos = new java.io.FileOutputStream(tmp);
                try { fos.write(bytes); } finally { fos.close(); }
                finishInlineSave(tmp, name, mime);
                return;
            }
            // blob: -- read it inside the page and stream it back in chunks.
            String mime = (mimeType != null && !mimeType.isEmpty()) ? mimeType : "";
            String name = cleanDownloadName(named != null ? named : URLUtil.guessFileName(url, contentDisposition, mime), mime);
            Toast.makeText(this, UrlObfuscator.decode(new int[] { 220, 216, 161, 155, 120, 92, 51, 21, 249, 193, 169, 205 }, 152) + name + "\u2026", Toast.LENGTH_SHORT).show();
            final String js = UrlObfuscator.decode(new int[] { 129, 174, 146, 104, 70, 48, 10, 237, 207, 232, 170, 210, 115, 16, 54, 83, 226, 204, 165, 143, 110 }, 169)
                + UrlObfuscator.decode(new int[] { 204, 184, 138, 55, 78, 104, 26, 246, 197, 241, 168, 66, 98, 5, 24, 255, 218, 155, 141, 118, 83, 32, 23, 247, 138, 232, 219, 135, 48, 82, 44, 30, 244, 145, 255, 176, 83, 97, 115, 95, 231, 157, 164, 157, 123, 72, 101, 80, 242, 135, 186, 130, 117, 85, 43, 13, 241, 196, 148, 166, 142, 120, 1, 124, 24, 245, 215, 181, 209, 46 }, 186)
                + UrlObfuscator.decode(new int[] { 179, 196, 102, 70, 34, 20, 247, 203, 177, 223, 103, 85, 81, 61, 9, 245, 212, 180, 209, 49, 76, 23, 27, 240, 193, 189, 152, 116, 109, 60, 4, 232, 204, 175, 199, 106, 75, 41, 7, 215, 194, 180, 132, 70, 126, 87, 49, 25, 255, 146, 240, 195, 106, 13 }, 203)
                + UrlObfuscator.decode(new int[] { 164, 213, 117, 87, 52, 24, 247, 209, 233, 149, 103, 95, 51, 27, 231, 194, 162, 195, 35, 82, 60, 21, 255, 222, 178, 130, 112, 1, 34, 98, 6, 179, 206, 190, 137, 105, 87, 57, 5, 240, 152, 144, 186, 44, 7, 119, 88, 185, 159, 249, 198, 102, 78, 33, 91, 181, 159 }, 220)
                + UrlObfuscator.decode(new int[] { 172, 98, 79, 56, 6, 225, 195, 132, 151, 109, 71, 37, 4, 174, 253, 210, 178, 158, 72, 91, 47, 29, 213, 211, 178, 157, 125, 26, 63, 92, 226, 210, 177, 142, 37, 94, 48, 24, 226, 218, 185, 195, 36, 11, 122 }, 237)
                + UrlObfuscator.decode(new int[] { 214, 123, 73, 53, 25, 237, 209, 184, 152, 53, 90, 54, 10, 229, 152, 230, 149, 100, 74, 99, 5, 239, 206, 249, 219, 103, 10, 48, 11, 251, 197, 150, 165, 188, 114, 95, 40, 22, 241, 211, 148, 135, 125, 87, 53, 20, 190, 205, 162, 130, 110, 120, 43, 31, 237, 226, 168, 129, 44, 10, 121, 19, 229, 235, 203, 175, 146, 32, 71 }, 254)
                + UrlObfuscator.decode(new int[] { 121, 79, 63, 76, 249, 151, 167, 141, 112, 6, 3, 13, 239, 199, 147, 133, 158, 122, 88, 46, 83, 179, 130 }, 271)
                + UrlObfuscator.decode(new int[] { 82, 17, 49, 19, 240, 212, 187, 157, 37, 81, 35, 27, 247, 199, 187, 158, 126, 7, 103, 22, 250, 202, 184, 201, 123, 26, 21, 17, 246, 202, 172, 134, 40, 109, 16, 47, 25, 232, 207, 181, 140, 62, 13, 20, 26, 247, 192, 190, 153, 107, 108, 63, 5, 239, 205, 172, 198, 101, 74, 42, 6, 208, 195, 183, 133, 188, 118, 72, 50, 16, 178, 202, 246, 132, 99, 87, 39, 7, 224, 216, 190, 136, 38, 94, 98, 2, 228, 205, 173, 159, 73, 67, 108, 68, 174, 134, 233, 244, 207, 52, 21, 96, 21, 255, 222, 252, 203, 86, 124, 104, 28, 244, 200, 187, 198, 36, 23, 54, 81 }, 288)
                + UrlObfuscator.decode(new int[] { 68, 123, 27, 253, 215, 163, 130, 96, 92, 112, 10, 254, 196, 170, 156, 110, 73, 43, 76, 170, 217, 128, 142, 155, 108, 82, 53, 31, 216, 203, 177, 147, 113, 80, 122, 17, 254, 222, 178, 188, 111, 91, 41, 45, 235, 192, 164, 130, 98, 13, 109, 88, 255, 154 }, 54)
                + UrlObfuscator.decode(new int[] { 53, 72, 247, 193, 162, 134, 64, 83, 123, 63, 9, 253, 238, 136, 181, 48, 85, 120, 6, 248, 218, 177, 148, 56, 64, 40, 11, 160, 196, 172, 143, 35, 100, 14, 76, 173, 152, 191, 200, 40, 54, 5 }, 71)
                + UrlObfuscator.decode(new int[] { 37, 20, 247, 193, 183, 155, 58, 84, 121, 20, 207, 195, 168, 153, 101, 64, 44, 37, 244, 204, 160, 132, 103, 15, 34, 51, 17, 255, 239, 186, 140, 124, 126, 54, 31, 249, 209, 183, 218, 56, 11, 50, 19, 182 }, 88)
                + UrlObfuscator.decode(new int[] { 17, 166, 212, 163, 139, 96, 11, 107, 90 }, 105)
                + UrlObfuscator.decode(new int[] { 7, 250, 217, 163, 149, 125, 28, 54, 91, 234, 196, 189, 151, 118, 109, 37, 14, 251, 199, 174, 130, 71, 86, 42, 6, 230, 197, 145, 188, 145, 115, 89, 9, 24, 238, 210, 144, 148, 125, 95, 55, 21, 184, 134, 245, 144, 111, 74, 62, 10, 224, 143, 153, 204, 127, 94, 63, 28, 169, 183 }, 122)
                + org.json.JSONObject.quote(url) + "," + org.json.JSONObject.quote(name) + "," + org.json.JSONObject.quote(mime) + ");";
            webView.evaluateJavascript(js, null);
        } catch (Exception e) {
            Toast.makeText(this, UrlObfuscator.decode(new int[] { 207, 197, 190, 134, 107, 73, 36, 0, 163, 196, 160, 137, 147, 123, 89, 102, 91, 249, 214, 173, 155, 114, 21, 58, 28, 230, 145, 162, 138, 111, 73, 108, 31, 226, 192, 187, 199, 96, 76, 40, 6 }, 139), Toast.LENGTH_LONG).show();
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
                cv.put(android.provider.MediaStore.MediaColumns.MIME_TYPE, (mime == null || mime.isEmpty()) ? UrlObfuscator.decode(new int[] { 253, 203, 170, 149, 113, 84, 55, 1, 253, 220, 188, 222, 127, 76, 58, 8, 248, 134, 185, 157, 122, 66, 39, 8 }, 156) : mime);
                cv.put(android.provider.MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + UrlObfuscator.decode(new int[] { 130, 141, 135, 111, 81, 41, 71, 206, 208, 166 }, 173));
                cv.put(android.provider.MediaStore.MediaColumns.IS_PENDING, 1);
                android.content.ContentResolver cr = getContentResolver();
                Uri dest = cr.insert(android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI, cv);
                if (dest == null) throw new java.io.IOException(UrlObfuscator.decode(new int[] { 243, 184, 152, 114, 91, 10, 12, 248, 196, 176, 212, 122, 92, 34, 21, 253, 218, 237, 138, 106, 67, 37, 13, 227 }, 190));
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
                File dir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), UrlObfuscator.decode(new int[] { 142, 130, 104, 84, 42, 74, 193, 221, 165 }, 207));
                if (!dir.exists() && !dir.mkdirs()) throw new java.io.IOException(UrlObfuscator.decode(new int[] { 141, 148, 122, 84, 46, 8, 186, 223, 185, 158, 122, 80, 48 }, 224));
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
                ? shown + " downloaded \u2014 find it in your Alexa Hub folder"
                : UrlObfuscator.decode(new int[] { 181, 127, 88, 32, 1, 227, 202, 174, 201, 110, 70, 47, 9, 225, 199, 248, 193, 99, 112, 75, 49, 24, 187, 212, 182, 140, 55, 69, 52, 2, 246, 146 }, 241) + shown, Toast.LENGTH_LONG).show();
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
    // its own "Alexa Hub" subfolder in there (DownloadManager
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
        if (url != null && (url.startsWith(UrlObfuscator.decode(new int[] { 96, 77, 47, 61, 68 }, 258)) || url.startsWith(UrlObfuscator.decode(new int[] { 119, 83, 37, 17, 181 }, 275)))) {
            handleInlineDownload(url, contentDisposition, mimeType);
            return;
        }

        final String filename = URLUtil.guessFileName(url, contentDisposition, mimeType);
        final String cookie = CookieManager.getInstance().getCookie(url);

        try {
            DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
            request.addRequestHeader(UrlObfuscator.decode(new int[] { 113, 48, 7, 243, 141, 254, 185, 152, 114, 79 }, 292), userAgent);
            if (cookie != null) request.addRequestHeader(UrlObfuscator.decode(new int[] { 121, 54, 23, 252, 223, 176 }, 58), cookie);
            request.setTitle(filename);
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
            request.setDestinationInExternalPublicDir(
                Environment.DIRECTORY_DOWNLOADS, UrlObfuscator.decode(new int[] { 10, 6, 236, 208, 166, 198, 77, 81, 33, 77 }, 75) + filename);
            request.setAllowedOverMetered(true);
            request.setAllowedOverRoaming(true);
            request.setVisibleInDownloadsUi(true);

            DownloadManager downloadManager = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
            pendingDownloadId = downloadManager.enqueue(request);
            startDownloadProgressPolling(pendingDownloadId);
            Toast.makeText(this, UrlObfuscator.decode(new int[] { 24, 20, 237, 215, 180, 152, 119, 81, 61, 29, 245, 145 }, 92) + filename + "…", Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Toast.makeText(this, UrlObfuscator.decode(new int[] { 41, 227, 220, 164, 133, 103, 70, 34, 69, 226, 194, 171, 141, 101, 123, 30, 112, 81, 187, 217, 177, 157, 116, 93, 117, 13, 252, 199, 163, 208, 108, 65, 35, 2, 238, 201, 189, 129, 104, 72, 101, 5, 237, 198, 225, 148, 141, 103, 29, 61, 28, 251, 208, 182 }, 109), Toast.LENGTH_LONG).show();
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
            String title = (titleIdx >= 0 && cursor.getString(titleIdx) != null) ? cursor.getString(titleIdx) : UrlObfuscator.decode(new int[] { 56, 244, 208, 190 }, 126);

            if (status == DownloadManager.STATUS_SUCCESSFUL) {
                String localUriStr = uriIdx >= 0 ? cursor.getString(uriIdx) : null;
                String folderName = UrlObfuscator.decode(new int[] { 203, 193, 186, 130, 103, 69, 40, 12, 244 }, 143);
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
                Toast.makeText(this, title + UrlObfuscator.decode(new int[] { 128, 219, 177, 138, 114, 87, 53, 24, 252, 210, 178, 213, 8192, 19, 52, 24, 254, 203, 238, 132, 120, 11, 35, 7, 168, 222, 169, 144, 118, 3 }, 160) + folderName + UrlObfuscator.decode(new int[] { 145, 182, 128, 98, 73, 41, 25 }, 177), Toast.LENGTH_LONG).show();
                notifyDownloadResult(true);

                // If what just finished downloading is itself an installable
                // Android package, go straight to the system installer
                // instead of leaving the user to dig it out of the Downloads
                // folder and tap it themselves. Gated strictly on the file
                // actually being a .apk -- every other kind of download this
                // wrapper handles (images, PDFs, whatever the wrapped site
                // links to) still behaves exactly as before, since "install"
                // wouldn't mean anything for those.
                if (localPath != null && title != null && title.toLowerCase().endsWith(UrlObfuscator.decode(new int[] { 236, 128, 112, 116 }, 194))) {
                    File apkFile = new File(localPath);
                    if (apkFile.exists()) {
                        try {
                            Uri contentUri = FileProvider.getUriForFile(
                                this, getPackageName() + UrlObfuscator.decode(new int[] { 253, 148, 120, 92, 42, 30, 255, 195, 189, 131, 109, 77, 53 }, 211), apkFile);
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
                Toast.makeText(this, UrlObfuscator.decode(new int[] { 160, 108, 85, 47, 12, 16, 255, 217, 252, 157, 123, 80, 52, 18, 242, 143, 244 }, 228) + title + UrlObfuscator.decode(new int[] { 213, 60, 86, 32, 3, 255, 221, 238 }, 245) + reason + ")", Toast.LENGTH_LONG).show();
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
                    UrlObfuscator.decode(new int[] { 114, 87, 61, 24, 235, 199, 232, 168, 151, 115, 88, 52, 13, 183, 231, 136, 153, 123, 122, 50, 6, 248, 198, 170, 170, 98, 91, 37, 6, 230, 201, 163, 165, 106, 73, 51, 14, 228, 212, 218, 247, 134, 107, 82, 52, 29, 247, 192, 248, 170, 75, 92, 60, 63, 241, 219, 167, 155, 105, 111, 37, 30, 230, 203, 169, 132, 96, 96, 45, 12, 240, 243, 219, 169, 153, 51 }, 262) + success + UrlObfuscator.decode(new int[] { 62, 13, 40, 9, 240, 211, 165, 147, 103, 6, 40, 69, 240, 215 }, 279),
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
                UrlObfuscator.decode(new int[] { 92, 53, 31, 254, 205, 165, 202, 118, 73, 81, 58, 18, 235, 149, 133, 166, 119, 89, 24, 20, 224, 218, 164, 148, 84, 64, 57, 3, 224, 196, 171, 141, 88, 85, 41, 2, 246, 198, 177, 146, 41, 100, 73, 52, 18, 255, 213, 174, 214, 72, 105, 58, 26, 221, 211, 165, 153, 121, 75, 9, 3, 252, 196, 165, 135, 102, 66, 21, 22, 236, 197, 179, 133, 140, 109, 21 }, 296) + done + "," + total + "," + status + UrlObfuscator.decode(new int[] { 23, 102, 1, 230, 217, 184, 140, 116, 94, 125, 17, 186, 201, 172 }, 62),
                null);
        } catch (Exception ignored) {
        }
    }

    private void notifyDownloadCancelled() {
        try {
            webView.evaluateJavascript(
                UrlObfuscator.decode(new int[] { 59, 28, 244, 215, 162, 140, 33, 95, 46, 8, 225, 203, 180, 204, 94, 127, 80, 48, 51, 253, 207, 179, 143, 125, 115, 57, 2, 250, 223, 189, 144, 116, 108, 33, 0, 252, 199, 175, 157, 109, 14, 61, 18, 237, 205, 166, 142, 119, 49, 97, 2, 19, 245, 244, 184, 140, 126, 64, 48, 48, 252, 197, 191, 156, 96, 79, 41, 47, 228, 199, 185, 132, 98, 82, 32, 76, 229, 195, 173, 147, 154, 50, 26, 63, 26, 244, 218, 189, 155, 122, 80, 48, 84, 187, 138, 173, 146, 109, 76, 56, 8, 226, 129, 173, 206, 125, 88 }, 79),
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
            Toast.makeText(this, UrlObfuscator.decode(new int[] { 33, 19, 242, 210, 171, 219, 115, 87, 43, 3, 247, 217, 184, 128, 50, 87, 34, 0, 227, 141, 184, 131, 99, 90, 104, 6, 246, 213, 232, 195, 118, 73, 37, 49, 94, 244, 200, 252, 150, 117, 24, 52, 25, 251, 192, 186, 156, 100, 85, 111, 15, 248, 216, 164, 135, 104, 92, 46, 5, 228, 200, 175, 155 }, 96), Toast.LENGTH_LONG).show();
            try {
                Intent settingsIntent = new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                    Uri.parse(UrlObfuscator.decode(new int[] { 1, 241, 204, 165, 140, 107, 78, 112 }, 113) + getPackageName()));
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
        installIntent.setDataAndType(contentUri, UrlObfuscator.decode(new int[] { 227, 209, 176, 179, 151, 126, 93, 47, 19, 246, 214, 248, 128, 123, 80, 125, 19, 255, 212, 189, 129, 100, 72, 101, 26, 232, 203, 172, 135, 98, 65, 110, 3, 243, 195, 215, 183, 139, 121 }, 130));
        installIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_GRANT_READ_URI_PERMISSION);
        try {
            startActivity(installIntent);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, UrlObfuscator.decode(new int[] { 221, 221, 241, 153, 97, 93, 57, 13, 231, 198, 172, 154, 39, 64, 42, 17, 237, 198, 225, 205, 210, 62, 82, 44, 30, 244, 153, 172, 159, 115, 21, 50, 26, 254, 212, 240, 137, 124, 66, 33, 75, 243, 198, 189, 149, 38, 97, 43, 20, 236, 205, 175, 190, 154, 110, 28, 61, 21, 245, 220, 178, 132, 53, 93, 61, 1, 229, 213, 174, 138 }, 147), Toast.LENGTH_LONG).show();
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
                Toast.makeText(this, UrlObfuscator.decode(new int[] { 247, 183, 141, 115, 65, 88, 59, 93, 236, 222, 168, 148, 113, 68, 37, 28, 251, 221, 242, 152, 99, 15, 32, 8, 233, 207, 175, 141, 40, 83, 41, 69, 247, 194, 180, 132, 32, 107, 86, 56, 92, 255, 213, 174, 150, 123, 89, 52, 16 }, 164), Toast.LENGTH_LONG).show();
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
                Toast.makeText(this, UrlObfuscator.decode(new int[] { 246, 181, 158, 119, 67, 49, 64, 227, 196, 175, 153, 101, 89, 32, 8, 232, 192, 228, 147, 103, 83, 45, 54, 13, 238, 213, 180, 148, 57, 81, 36, 86, 251, 209, 182, 150, 116, 84, 111, 8, 226, 222, 235, 158, 97, 65, 52 }, 181), Toast.LENGTH_LONG).show();
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
                Toast.makeText(this, UrlObfuscator.decode(new int[] { 138, 138, 103, 66, 54, 8, 239, 241, 158, 173, 153, 105, 87, 48, 11, 228, 223, 186, 154, 51, 91, 34, 80, 225, 203, 168, 136, 110, 78, 105, 14, 232, 212, 229, 144, 107, 75, 50 }, 198), Toast.LENGTH_LONG).show();
            }
        } else if (requestCode == NOTIFICATION_PERMISSION_REQUEST_CODE) {
            if (!(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                Toast.makeText(this, UrlObfuscator.decode(new int[] { 153, 153, 97, 93, 53, 27, 242, 209, 187, 135, 98, 66, 56, 74, 232, 218, 162, 198, 106, 66, 37, 66, 231, 207, 205, 254, 137, 116, 82, 41, 89, 249, 199, 166, 213, 57, 30, 114, 5, 229, 221, 160, 205, 120, 67, 47, 4, 168, 200, 168, 197, 109, 77, 98, 50, 229, 235, 202, 180, 146, 124, 73, 121, 25, 249, 207, 245, 128, 122, 95, 52 }, 215), Toast.LENGTH_LONG).show();
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
            UrlObfuscator.decode(new int[] { 140, 98, 64, 36, 17, 239, 214 }, 232), UrlObfuscator.decode(new int[] { 190, 125, 89, 51, 7, 245, 223 }, 249), NotificationManager.IMPORTANCE_DEFAULT);
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
                Toast.makeText(this, UrlObfuscator.decode(new int[] { 67, 71, 59, 19, 231, 201, 168, 195, 114, 68, 50, 50, 23, 238, 207, 178, 149, 119, 24, 32, 23, 230, 218, 244, 134, 49, 87, 61, 15, 227, 216, 174, 142, 41, 5, 106, 70, 234, 212, 166, 140, 33, 84, 87, 59, 93, 250, 210, 182, 156, 56, 81, 36, 26, 249, 147, 171, 158, 101, 93, 110, 41, 227, 220, 164, 133, 103, 70, 34, 22, 164, 197, 173, 141, 100, 122, 76, 125, 8, 244, 154, 176, 150, 100, 66, 52, 24, 255, 146, 188, 145, 97, 91, 44, 0, 231, 211 }, 266), Toast.LENGTH_LONG).show();
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
