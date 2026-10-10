package com.tourarena.app;

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
    private static final int ZOOM_LOCK_JS_ID = 6;
    private static final int NATIVE_FEEL_JS_ID = 7;
    private static final int ELASTIC_PULL_JS_ID = 8;
    private static final int EXIT_THEME_JS_ID = 9;
    private static final int DOWNLOAD_NAME_JS_ID = 10;
    private static final int VIDEO_LOADER_JS_ID = 11;
    private static final int SHOW_OPEN_FILE_PICKER_POLYFILL_ID = 12;
    private static final int RESOURCE_ERROR_JS_ID = 13;
    private static final int CLICK_LOADER_JS_ID = 14;
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
    // failed scripts and stylesheets (see RESOURCE_ERROR_JS in server.js) --
    // native-only now, like the fields above (see native/screennative.c).
    private static final String RESOURCE_ERROR_JS = nativeDecryptScreen(RESOURCE_ERROR_JS_ID);

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
    private static final String ZOOM_LOCK_JS = nativeDecryptScreen(ZOOM_LOCK_JS_ID);
    // Removes the grey tap flash, the long-press callout and accidental text
    // selection so the page behaves like a native screen. Inputs, textareas
    // and contenteditable stay fully selectable/typeable; a site can opt any
    // element back in with data-allow-select. Zero-specificity (:where), so
    // a site's own user-select rules still win. Idempotent.
    private static final String NATIVE_FEEL_JS = nativeDecryptScreen(NATIVE_FEEL_JS_ID);
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
    private static final String ELASTIC_PULL_JS = nativeDecryptScreen(ELASTIC_PULL_JS_ID);
    // Reads the live page (background behind the screen centre, theme-color,
    // the first real button's color and corner radius) so the exit dialog can
    // be dressed to match whatever site the app wraps. Read-only; returns '{}'
    // on any error and the dialog falls back to the app's bar color.
    private static final String EXIT_THEME_JS = nativeDecryptScreen(EXIT_THEME_JS_ID);
    // Universal video loading indicator. Any <video> on any page that is
    // still fetching/buffering gets a spinner overlay (fades in, removed once
    // it can play), so the user never stares at a blank/grey box. Pure JS,
    // no per-site config; picks up videos added later too. Overlay is a
    // separate fixed element, so the page's own DOM/layout is untouched.
    // Idempotent (__vidLoaderBound).
    // Records the name a page gives an <a download="..."> link (also for
    // anchors clicked from script) so blob:/data: downloads keep it.
    private static final String DOWNLOAD_NAME_JS = nativeDecryptScreen(DOWNLOAD_NAME_JS_ID);
    private static final String VIDEO_LOADER_JS = nativeDecryptScreen(VIDEO_LOADER_JS_ID);
    private static final String CLICK_LOADER_JS = nativeDecryptScreen(CLICK_LOADER_JS_ID);
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
    private static final String SHOW_OPEN_FILE_PICKER_POLYFILL = nativeDecryptScreen(SHOW_OPEN_FILE_PICKER_POLYFILL_ID);
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
    private SkeletonView navSkeletonRef;
    private BouncingDotsView navDotsRef;

    // Finds the layout recorded for a page the last time it finished loading,
    // keyed by origin + path (no query or fragment, no trailing slash) so the
    // same screen matches however it was reached. Null when never recorded.
    private String skeletonFor(String url) {
        if (url == null) return null;
        String k = url;
        int q = k.indexOf('?');
        if (q >= 0) k = k.substring(0, q);
        int hh = k.indexOf('#');
        if (hh >= 0) k = k.substring(0, hh);
        while (k.length() > 8 && k.endsWith("/")) k = k.substring(0, k.length() - 1);
        try {
            return getSharedPreferences(UrlObfuscator.decode(new int[] { 59, 12, 227, 201, 161, 151, 109, 79, 31, 41, 79 }, 72), MODE_PRIVATE).getString("p:" + k, null);
        } catch (Exception e) {
            return null;
        }
    }
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
            line.setColor(Color.parseColor(UrlObfuscator.decode(new int[] { 122, 72, 213, 135, 226, 192, 35 }, 89)));
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
            int fur = Color.parseColor(UrlObfuscator.decode(new int[] { 73, 207, 156, 129, 211, 67, 102 }, 106));
            int pink = Color.parseColor(UrlObfuscator.decode(new int[] { 88, 220, 141, 153, 196, 85, 5 }, 123));
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
            line.setColor(Color.parseColor(UrlObfuscator.decode(new int[] { 175, 234, 143, 171, 48, 98, 118 }, 140)));
            c.drawLine(22, 47, 9, 44, line);
            c.drawLine(22, 52, 9, 53, line);
            c.drawLine(78, 47, 91, 44, line);
            c.drawLine(78, 52, 91, 53, line);
            line.setColor(Color.parseColor(UrlObfuscator.decode(new int[] { 190, 140, 153, 203, 46, 12, 103 }, 157)));

            fill.setColor(Color.parseColor(UrlObfuscator.decode(new int[] { 141, 252, 216, 57, 30, 124, 43 }, 174)));
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
            shape(c, tear, Color.parseColor(UrlObfuscator.decode(new int[] { 156, 230, 184, 88, 3, 28, 65 }, 191)));

            android.graphics.Path nose = new android.graphics.Path();
            nose.moveTo(47.5f, 48f); nose.lineTo(52.5f, 48f); nose.lineTo(50f, 51f); nose.close();
            fill.setColor(Color.parseColor(UrlObfuscator.decode(new int[] { 243, 170, 54, 26, 121, 82, 207 }, 208))); c.drawPath(nose, fill);

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
            paint.setColor(Color.parseColor(UrlObfuscator.decode(new int[] { 194, 70, 45, 120, 111, 57, 222 }, 225)));
            canvas.drawCircle(cx, cy, stroke * 1.05f, paint);

            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(stroke);
            paint.setColor(Color.parseColor(UrlObfuscator.decode(new int[] { 209, 36, 85, 42, 15, 233, 152 }, 242)));
            float[] radii = {w * 0.24f, w * 0.38f};
            int[] alphas = {235, 130};
            for (int i = 0; i < radii.length; i++) {
                paint.setAlpha(alphas[i]);
                RectF arc = new RectF(cx - radii[i], cy - radii[i], cx + radii[i], cy + radii[i]);
                canvas.drawArc(arc, 208, 124, false, paint);
            }

            paint.setAlpha(255);
            paint.setColor(Color.parseColor(UrlObfuscator.decode(new int[] { 32, 100, 7, 86, 61, 171, 138 }, 259)));
            paint.setStrokeWidth(stroke * 1.1f);
            float pad = w * 0.14f;
            canvas.drawLine(pad, pad, w - pad, h - pad, paint);
        }
    }

    // Native-style indeterminate spinner for the nav-loading overlay and the
    // popup loader: a thin round-capped arc that grows and shrinks while it
    // rotates, the same motion as Material's circular progress indicator.
    // (Class keeps its old name so every call site picks it up unchanged.)
    private static class BouncingDotsView extends View {
        private final Paint trackPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint arcPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final RectF arcBox = new RectF();
        private int color;
        private float rotation = 0f;
        private ValueAnimator anim;

        BouncingDotsView(Context context, int color) {
            super(context);
            this.color = color;
            trackPaint.setStyle(Paint.Style.STROKE);
            arcPaint.setStyle(Paint.Style.STROKE);
            arcPaint.setStrokeCap(Paint.Cap.ROUND);
            setWillNotDraw(false);
            applyColor();
        }

        void setColor(int color) {
            this.color = color;
            applyColor();
            invalidate();
        }

        private void applyColor() {
            trackPaint.setColor(color);
            trackPaint.setAlpha(40);
            arcPaint.setColor(color);
            arcPaint.setShader(null);
        }

        @Override
        protected void onSizeChanged(int w, int h, int oldw, int oldh) {
            super.onSizeChanged(w, h, oldw, oldh);
            applyColor();
            int clear = Color.argb(0, Color.red(color), Color.green(color), Color.blue(color));
            arcPaint.setShader(new android.graphics.SweepGradient(
                w / 2f, h / 2f, new int[] { clear, color }, new float[] { 0f, 0.75f }));
        }

        // Only animates while actually on screen.
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
            anim = ValueAnimator.ofFloat(0f, 360f);
            anim.setDuration(900);
            anim.setInterpolator(new android.view.animation.LinearInterpolator());
            anim.setRepeatCount(ValueAnimator.INFINITE);
            anim.addUpdateListener(animator -> {
                rotation = (float) animator.getAnimatedValue();
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
            float size = Math.min(getWidth(), getHeight());
            if (size <= 0) return;
            float stroke = size * 0.07f;
            trackPaint.setStrokeWidth(stroke);
            arcPaint.setStrokeWidth(stroke);
            float d = size * 0.72f;
            float cx = getWidth() / 2f;
            float cy = getHeight() / 2f;
            arcBox.set(cx - d / 2f, cy - d / 2f, cx + d / 2f, cy + d / 2f);
            canvas.drawOval(arcBox, trackPaint);
            canvas.save();
            canvas.rotate(rotation, cx, cy);
            canvas.drawArc(arcBox, 0f, 270f, false, arcPaint);
            canvas.restore();
        }
    }

    // Skeleton placeholder for the nav-loading overlay. It never invents a
    // layout: it only draws the block structure this device recorded the last
    // time that same page finished loading (see saveSkeleton in AndroidBridge).
    // A page the app hasn't seen yet has no recorded layout, so the overlay
    // shows just the page-colored screen with the spinner on top instead.
    // Colors come from the page's real background (setBase, called from
    // applyPageBackground): blocks are that color nudged toward white (dark
    // pages) or black (light pages), with one soft highlight band sweeping
    // across all of them together.
    private static class SkeletonView extends View {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final RectF box = new RectF();
        private final android.graphics.Matrix shaderMatrix = new android.graphics.Matrix();
        private android.graphics.LinearGradient shader;
        private float shaderBandW = -1f;
        private final int[] locA = new int[2];
        private final int[] locB = new int[2];
        private View target;
        private float[][] rects;
        private int baseColor, blockColor, glowColor;
        private float sweep = 0f;
        private ValueAnimator anim;

        SkeletonView(Context context, int base) {
            super(context);
            setWillNotDraw(false);
            setBase(base);
        }

        void setTarget(View v) {
            target = v;
        }

        private static int mix(int a, int b, float t) {
            int r = Math.round(Color.red(a) + (Color.red(b) - Color.red(a)) * t);
            int g = Math.round(Color.green(a) + (Color.green(b) - Color.green(a)) * t);
            int bl = Math.round(Color.blue(a) + (Color.blue(b) - Color.blue(a)) * t);
            return Color.rgb(r, g, bl);
        }

        void setBase(int base) {
            baseColor = base;
            float lum = 0.299f * Color.red(base) + 0.587f * Color.green(base) + 0.114f * Color.blue(base);
            boolean dark = lum < 140f;
            blockColor = mix(base, dark ? Color.WHITE : Color.BLACK, dark ? 0.08f : 0.07f);
            glowColor = mix(blockColor, Color.WHITE, dark ? 0.07f : 0.55f);
            shader = null;
            invalidate();
        }

        // Loads a recorded layout: a JSON array of [x, y, w, h, radius]
        // entries, all as fractions of the page viewport width/height.
        // Returns true when a usable layout was set, false (and clears any
        // old one) otherwise.
        boolean setLayoutJson(String json) {
            rects = null;
            if (json == null) return false;
            try {
                org.json.JSONArray arr = new org.json.JSONArray(json);
                int n = Math.min(arr.length(), 80);
                float[][] out = new float[n][];
                int k = 0;
                for (int i = 0; i < n; i++) {
                    org.json.JSONArray e = arr.optJSONArray(i);
                    if (e == null || e.length() < 5) continue;
                    out[k++] = new float[] {
                        (float) e.optDouble(0), (float) e.optDouble(1), (float) e.optDouble(2),
                        (float) e.optDouble(3), (float) e.optDouble(4)
                    };
                }
                if (k < 4) return false;
                rects = java.util.Arrays.copyOf(out, k);
                return true;
            } catch (Exception e) {
                return false;
            }
        }

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
            if (anim != null || rects == null) return;
            anim = ValueAnimator.ofFloat(0f, 1f);
            anim.setDuration(1300);
            anim.setInterpolator(new android.view.animation.LinearInterpolator());
            anim.setRepeatCount(ValueAnimator.INFINITE);
            anim.addUpdateListener(animator -> {
                sweep = (float) animator.getAnimatedValue();
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
            canvas.drawColor(baseColor);
            if (rects == null) return;

            float ox = 0f, oy = 0f, tw = w, th = h;
            if (target != null && target.getWidth() > 0 && target.getHeight() > 0) {
                target.getLocationInWindow(locA);
                getLocationInWindow(locB);
                ox = locA[0] - locB[0];
                oy = locA[1] - locB[1];
                tw = target.getWidth();
                th = target.getHeight();
            }

            float bandW = tw * 0.7f;
            if (shader == null || shaderBandW != bandW) {
                shader = new android.graphics.LinearGradient(0f, 0f, bandW, 0f,
                    new int[] { blockColor, glowColor, blockColor },
                    new float[] { 0f, 0.5f, 1f },
                    android.graphics.Shader.TileMode.CLAMP);
                shaderBandW = bandW;
            }
            shaderMatrix.setTranslate(ox - bandW + sweep * (tw + bandW), 0f);
            shader.setLocalMatrix(shaderMatrix);
            paint.setShader(shader);

            for (float[] r : rects) {
                float l = ox + r[0] * tw;
                float t = oy + r[1] * th;
                float rw = r[2] * tw;
                float rh = r[3] * th;
                if (rw < 2f || rh < 2f) continue;
                float rad = Math.min(r[4] * tw, Math.min(rw, rh) / 2f);
                box.set(l, t, l + rw, t + rh);
                canvas.drawRoundRect(box, rad, rad, paint);
            }
            paint.setShader(null);
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
            String name = (appName == null || appName.trim().isEmpty()) ? UrlObfuscator.decode(new int[] { 85, 67, 34 }, 276) : appName.trim().toUpperCase();

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
            letter.setTypeface(Typeface.create(UrlObfuscator.decode(new int[] { 86, 37, 13, 241, 140, 179, 186, 140, 116, 90, 118, 23, 252, 220, 190, 131, 120 }, 293), Typeface.NORMAL));
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
                videoResId = context.getResources().getIdentifier(UrlObfuscator.decode(new int[] { 88, 47, 10, 236, 216, 187, 170, 103, 67, 62, 16, 227, 199 }, 59), UrlObfuscator.decode(new int[] { 62, 10, 253 }, 76), pkg);
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
                int resId = context.getResources().getIdentifier(UrlObfuscator.decode(new int[] { 62, 9, 232, 206, 182, 149, 72, 69, 37, 24, 242, 193, 185 }, 93), UrlObfuscator.decode(new int[] { 10, 255, 205, 188, 139, 107, 68, 34 }, 110), pkg);
                if (resId != 0) {
                    iv.setImageURI(Uri.parse(UrlObfuscator.decode(new int[] { 30, 240, 217, 174, 148, 115, 93, 118, 5, 243, 198, 187, 134, 96, 82, 53, 85, 161, 130 }, 127) + pkg + "/" + resId));
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
                mp.setDataSource(context, Uri.parse(UrlObfuscator.decode(new int[] { 241, 193, 170, 159, 99, 66, 46, 71, 250, 194, 181, 138, 113, 81, 33, 4, 186, 176, 145 }, 144) + pkg + "/" + videoResId));
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

    // Default startup screen ("native" style). Behaves like a normal app
    // launch: the app's own icon sits centered on the site's theme color at
    // the exact size/spot of the OS launch screen (so the handoff from the OS
    // is invisible), holds for a short beat so it never flashes, then fades
    // away to reveal the page that is already drawn underneath. Nothing
    // animates while the load is quick. Only if the load is still running
    // after a moment does a small spinner fade in near the bottom.
    private static class NativeLaunchView extends FrameLayout implements SplashController {
        private static final long MIN_HOLD_MS = 350;
        private static final long SLOW_AFTER_MS = 1600;
        private final ImageView icon;
        private final BouncingDotsView spinner;
        private final Handler handler = new Handler(Looper.getMainLooper());
        private long shownAt = 0;
        private boolean shownOnce = false;

        private final Runnable showSpinner = new Runnable() {
            @Override
            public void run() {
                if (getVisibility() != View.VISIBLE) return;
                spinner.setAlpha(0f);
                spinner.setVisibility(View.VISIBLE);
                spinner.animate().alpha(1f).setDuration(250).start();
            }
        };

        private final Runnable fadeOut = new Runnable() {
            @Override
            public void run() {
                handler.removeCallbacks(showSpinner);
                spinner.animate().cancel();
                spinner.animate().alpha(0f).setDuration(120).start();
                icon.animate().cancel();
                animate().cancel();
                if (variant == 1) {
                    // iOS-style: the whole launch screen zooms toward the
                    // viewer and fades, the page settles in underneath.
                    setPivotX(getWidth() / 2f);
                    setPivotY(getHeight() / 2f);
                    animate().scaleX(1.12f).scaleY(1.12f).alpha(0f).setDuration(380)
                        .setInterpolator(new DecelerateInterpolator())
                        .withEndAction(() -> finishHidden()).start();
                } else if (variant == 2) {
                    // The launch screen lifts up and off the top, uncovering the page.
                    animate().translationY(-getHeight()).setDuration(420)
                        .setInterpolator(new android.view.animation.AccelerateDecelerateInterpolator())
                        .withEndAction(() -> finishHidden()).start();
                } else if (variant == 3) {
                    // The icon swells a touch, then collapses to nothing while
                    // the background fades out behind it.
                    icon.animate().scaleX(1.14f).scaleY(1.14f).setDuration(110)
                        .withEndAction(() -> icon.animate().scaleX(0f).scaleY(0f).alpha(0f).setDuration(220)
                            .setInterpolator(new android.view.animation.AccelerateInterpolator()).start())
                        .start();
                    animate().alpha(0f).setStartDelay(150).setDuration(260)
                        .setInterpolator(new DecelerateInterpolator())
                        .withEndAction(() -> finishHidden()).start();
                } else {
                    icon.animate().scaleX(1.06f).scaleY(1.06f).setDuration(280)
                        .setInterpolator(new DecelerateInterpolator()).start();
                    animate().alpha(0f).setDuration(280)
                        .setInterpolator(new DecelerateInterpolator())
                        .withEndAction(() -> finishHidden()).start();
                }
            }
        };

        private final int variant;

        NativeLaunchView(Context context, int bgColor, int spinnerColor, int variant) {
            super(context);
            this.variant = variant;
            setBackgroundColor(bgColor);
            setVisibility(View.INVISIBLE);
            setAlpha(0f);

            float density = context.getResources().getDisplayMetrics().density;
            int iconSize = Math.round(136 * density);
            icon = new ImageView(context);
            icon.setScaleType(ImageView.ScaleType.FIT_CENTER);
            int iconResId = context.getResources().getIdentifier(UrlObfuscator.decode(new int[] { 200, 163, 128, 146, 124, 73, 53, 25, 241, 221, 165 }, 161), UrlObfuscator.decode(new int[] { 223, 184, 128, 98, 79, 61 }, 178), context.getPackageName());
            if (iconResId != 0) {
                icon.setImageResource(iconResId);
            }
            FrameLayout.LayoutParams iconParams = new FrameLayout.LayoutParams(iconSize, iconSize);
            iconParams.gravity = Gravity.CENTER;
            addView(icon, iconParams);

            spinner = new BouncingDotsView(context, spinnerColor);
            spinner.setVisibility(View.GONE);
            int spinSize = Math.round(26 * density);
            FrameLayout.LayoutParams spinParams = new FrameLayout.LayoutParams(spinSize, spinSize);
            spinParams.gravity = Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL;
            spinParams.bottomMargin = Math.round(88 * density);
            addView(spinner, spinParams);
        }

        private void resetLook() {
            animate().cancel();
            animate().setStartDelay(0);
            setScaleX(1f);
            setScaleY(1f);
            setTranslationY(0f);
            icon.setScaleX(1f);
            icon.setScaleY(1f);
            icon.setAlpha(1f);
            spinner.setAlpha(1f);
        }

        private void finishHidden() {
            setVisibility(View.GONE);
            resetLook();
            spinner.setVisibility(View.GONE);
        }

        public void show() {
            handler.removeCallbacks(fadeOut);
            handler.removeCallbacks(showSpinner);
            icon.animate().cancel();
            spinner.animate().cancel();
            resetLook();
            spinner.setVisibility(View.GONE);
            setVisibility(View.VISIBLE);
            shownAt = System.currentTimeMillis();
            if (!shownOnce) {
                // App launch: the OS launch screen underneath already shows
                // this icon in this spot, so appear instantly.
                shownOnce = true;
                setAlpha(1f);
            } else {
                setAlpha(0f);
                animate().alpha(1f).setDuration(200).start();
            }
            handler.postDelayed(showSpinner, SLOW_AFTER_MS);
        }

        public void hide() {
            handler.removeCallbacks(fadeOut);
            long wait = Math.max(0L, MIN_HOLD_MS - (System.currentTimeMillis() - shownAt));
            handler.postDelayed(fadeOut, wait);
        }

        @Override
        public void hideImmediate() {
            handler.removeCallbacks(fadeOut);
            handler.removeCallbacks(showSpinner);
            animate().cancel();
            icon.animate().cancel();
            setAlpha(0f);
            finishHidden();
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
            int iconSize = Math.round(136 * density);
            ImageView iv = new ImageView(context);
            iv.setScaleType(ImageView.ScaleType.FIT_CENTER);
            // Looked up by name (same pattern as CustomSplashView's
            // custom_splash asset above) rather than referenced as
            // R.mipmap.ic_launcher, since this generated source doesn't
            // otherwise depend on the built R class. ic_launcher is always
            // present -- either the uploaded logo or the generated default
            // -- so this should never come back 0.
            int iconResId = context.getResources().getIdentifier(UrlObfuscator.decode(new int[] { 170, 129, 94, 76, 94, 43, 19, 255, 211, 191, 139 }, 195), UrlObfuscator.decode(new int[] { 185, 154, 98, 92, 49, 31 }, 212), context.getPackageName());
            if (iconResId != 0) {
                iv.setImageResource(iconResId);
            }
            FrameLayout.LayoutParams iconParams = new FrameLayout.LayoutParams(iconSize, iconSize);
            iconParams.gravity = Gravity.CENTER;
            addView(iv, iconParams);
        }

        private boolean shownOnce = false;

        public void show() {
            animate().cancel();
            setVisibility(View.VISIBLE);
            if (!shownOnce) {
                // First show is app launch: the OS launch screen underneath
                // already has this exact icon in this exact spot, so appear
                // instantly instead of fading in (a fade here would dip the
                // icon's brightness while the system screen fades out).
                shownOnce = true;
                setAlpha(1f);
                return;
            }
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
        // Android 12+: by default the system launch screen disappears with a
        // hard cut the moment the first frame is drawn. Fade it out instead
        // (must be registered here, before the first frame).
        if (Build.VERSION.SDK_INT >= 31) {
            getSplashScreen().setOnExitAnimationListener(splashScreenView -> {
                splashScreenView.animate().alpha(0f).setDuration(250)
                    .withEndAction(splashScreenView::remove).start();
            });
        }
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
        root.setBackgroundColor(Color.parseColor(UrlObfuscator.decode(new int[] { 198, 52, 19, 114, 81, 176, 175 }, 229)));

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
        // Startup loading is off: no splash overlay, but the site still
        // eases in (soft fade plus a tiny settle) once it is ready,
        // instead of snapping on.
        webView.setAlpha(0f);
        webView.setScaleX(0.97f);
        webView.setScaleY(0.97f);
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
        // The default Material pull-to-refresh circle (white disc, shadow, colored
        // arc) is hidden on purpose: the gesture still reloads the page, but no
        // spinner is drawn for it. Child 0 is SwipeRefreshLayout's own circle
        // view (it is added in the constructor, before the WebView).
        if (swipeRefresh.getChildCount() > 0) {
            swipeRefresh.getChildAt(0).setAlpha(0f);
        }
        swipeRefresh.setColorSchemeColors(Color.parseColor(UrlObfuscator.decode(new int[] { 213, 39, 112, 101, 49, 213, 246 }, 246)));
        swipeRefresh.addView(webView, new ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        root.addView(swipeRefresh, webParams);

        // Status-bar and navigation-bar strips. Added before the splash /
        // error / navigation overlays so those still cover the whole screen.
        topScrim = new View(this);
        topScrim.setBackgroundColor(Color.parseColor(UrlObfuscator.decode(new int[] { 36, 22, 112, 84, 182, 146, 244 }, 263)));
        root.addView(topScrim, new FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT, 0, Gravity.TOP));
        bottomScrim = new View(this);
        bottomScrim.setBackgroundColor(Color.parseColor(UrlObfuscator.decode(new int[] { 59, 7, 99, 69, 161, 131, 231 }, 280)));
        root.addView(bottomScrim, new FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT, 0, Gravity.BOTTOM));
        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            androidx.core.graphics.Insets bars = insets.getInsets(
                WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout());
            androidx.core.graphics.Insets ime = insets.getInsets(WindowInsetsCompat.Type.ime());
            // Some devices report a zero top inset while the status bar is
            // plainly on screen, which let the page slide under the clock and
            // battery. If the system says the status bar is visible but the
            // combined inset is 0, fall back to the status bar's own height
            // (its inset ignoring visibility, then the framework dimen).
            int topInset = bars.top;
            if (topInset == 0 && fsView == null
                    && insets.isVisible(WindowInsetsCompat.Type.statusBars())) {
                topInset = Math.max(
                    insets.getInsetsIgnoringVisibility(WindowInsetsCompat.Type.statusBars()).top,
                    statusBarHeightFallback());
            }
            applySafeArea(bars.left, topInset, bars.right, bars.bottom, ime.bottom);
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
        // Ask for a fresh inset pass now that the listener exists, and
        // double-check shortly after launch: if no inset has been applied
        // yet (WebView still flush with the top of the screen) while the
        // status bar is showing, inset by the status bar's height so the
        // page never starts underneath the clock and battery. Any later real
        // inset callback overrides this with the exact values.
        ViewCompat.requestApplyInsets(root);
        root.postDelayed(() -> {
            try {
                if (swipeRefresh == null || fsView != null) return;
                FrameLayout.LayoutParams wlp = (FrameLayout.LayoutParams) swipeRefresh.getLayoutParams();
                if (wlp.topMargin != 0) return;
                WindowInsetsCompat ri = ViewCompat.getRootWindowInsets(root);
                if (ri == null || !ri.isVisible(WindowInsetsCompat.Type.statusBars())) return;
                int h = Math.max(
                    ri.getInsetsIgnoringVisibility(WindowInsetsCompat.Type.statusBars()).top,
                    statusBarHeightFallback());
                if (h > 0) applySafeArea(wlp.leftMargin, h, wlp.rightMargin, wlp.bottomMargin, 0);
            } catch (Exception ignored) {
            }
        }, 600);

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
        final String offlineScrimColor = offlineIsNightMode ? UrlObfuscator.decode(new int[] { 10, 113, 94, 182, 149, 244, 211, 50, 17 }, 297) : UrlObfuscator.decode(new int[] { 28, 104, 75, 172, 139, 234, 201, 40, 7 }, 63);
        // Slightly translucent (not fully opaque) card/button fills, so a
        // hint of the dimmed scrim behind still shows through -- a cheap
        // stand-in for a true frosted-glass blur, which would need
        // RenderEffect (API 31+) and a snapshot of whatever's behind the
        // card to do for real.
        final String offlineCardBg = offlineIsNightMode ? UrlObfuscator.decode(new int[] { 115, 42, 182, 159, 143, 217, 79, 26, 120 }, 80) : UrlObfuscator.decode(new int[] { 66, 194, 167, 248, 239, 186, 41, 124, 107 }, 97);
        final String offlineTitleColor = offlineIsNightMode ? UrlObfuscator.decode(new int[] { 81, 215, 246, 137, 168, 75, 106 }, 114) : UrlObfuscator.decode(new int[] { 160, 146, 241, 208, 207, 46, 13 }, 131);
        final String offlineSubtitleColor = offlineIsNightMode ? UrlObfuscator.decode(new int[] { 183, 241, 234, 179, 40, 109, 10 }, 148) : UrlObfuscator.decode(new int[] { 134, 241, 161, 55, 99, 117, 26 }, 165);
        final String offlineButtonBg = offlineIsNightMode ? UrlObfuscator.decode(new int[] { 149, 237, 183, 39, 10, 101, 72, 187, 239 }, 182) : UrlObfuscator.decode(new int[] { 228, 222, 70, 96, 117, 38, 183, 228, 254 }, 199);
        final float offlineDensity = getResources().getDisplayMetrics().density;

        // Outer full-screen scrim so the card reads as a dialog sitting on
        // top of the app, matching how a native "no connection" alert
        // dims everything behind it.
        final FrameLayout errorView = new FrameLayout(this);
        errorView.setBackgroundColor(Color.parseColor(UrlObfuscator.decode(new int[] { 251, 199, 87, 5, 21, 67, 208 }, 216)));
        errorView.setVisibility(View.GONE);

        // The error screen is the branded HTML page in ERROR_PAGE_HTML,
        // rendered in its own small WebView that fills errorView. Its
        // Retry / Back buttons call back into native code through the
        // AndroidError interface (registered below, once doRetry exists).
        // Navigation inside it is blocked so it can only ever show that page.
        final WebView errorWeb = new WebView(this);
        errorWeb.setBackgroundColor(Color.parseColor(UrlObfuscator.decode(new int[] { 202, 56, 102, 118, 36, 180, 225 }, 233)));
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
        navOverlay.setBackgroundColor(Color.parseColor(UrlObfuscator.decode(new int[] { 217, 41, 8, 103, 70, 165, 132 }, 250)));
        navOverlay.setVisibility(View.GONE);
        final SkeletonView navSkeleton = new SkeletonView(this, Color.parseColor(UrlObfuscator.decode(new int[] { 40, 26, 121, 88, 183, 150, 245 }, 267)));
        navSkeleton.setTarget(webView);
        navSkeletonRef = navSkeleton;
        navOverlay.addView(navSkeleton, new FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
        final BouncingDotsView navDots = new BouncingDotsView(this, Color.parseColor(UrlObfuscator.decode(new int[] { 63, 125, 104, 63, 170, 242, 147 }, 284)));
        navDotsRef = navDots;
        int navDotsSize = (int) (44 * getResources().getDisplayMetrics().density);
        FrameLayout.LayoutParams navDotsParams = new FrameLayout.LayoutParams(navDotsSize, navDotsSize);
        navDotsParams.gravity = Gravity.CENTER;
        navOverlay.addView(navDots, navDotsParams);
        FrameLayout.LayoutParams navOverlayParams = new FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT);
        root.addView(navOverlay, navOverlayParams);
        // Flips true right after the first successful page load -- used to
        // skip navOverlay on that first load only (the splash already has
        // it covered) and show it on every navigation after that.
        final boolean[] hasLoadedOnce = { false };
        // The cover only appears if a navigation is still running after a
        // short beat, so quick page changes don't flash a screen at all, and
        // it fades in instead of popping on.
        final Runnable revealNavOverlay = () -> {
            navOverlay.animate().cancel();
            navOverlay.setAlpha(0f);
            navOverlay.setVisibility(View.VISIBLE);
            navOverlay.animate().alpha(1f).setDuration(120).start();
        };
        // Only reveal the cover when the page really has nothing to show (empty
        // body: no text, no images/video/canvas/svg/iframe). A page that already
        // has content never gets a spinner on top of it.
        final Runnable showNavOverlay = () -> {
            webView.evaluateJavascript(
                UrlObfuscator.decode(new int[] { 26, 55, 5, 225, 205, 185, 133, 100, 68, 97, 65, 252, 210, 183, 157, 120, 84, 32, 18, 95, 252, 128, 184, 148, 121, 76, 53, 18, 248, 193, 250, 145, 125, 85, 41, 84, 231, 203, 228, 202, 104, 0, 58, 2, 242, 208, 182, 141, 34, 85, 50, 42, 27, 166 }, 50)
                + UrlObfuscator.decode(new int[] { 42, 4, 169, 136, 221, 240, 148, 114, 85, 63, 11, 204, 210, 174, 129, 104, 79, 117, 86, 185, 129, 186, 159, 101, 70, 98, 64, 166, 203, 163, 139, 99, 87, 42, 95, 176, 182, 204, 184, 136, 110, 72, 55, 88, 241, 215, 185, 135, 118, 9 }, 67)
                + UrlObfuscator.decode(new int[] { 38, 22, 230, 196, 162, 129, 46, 79, 98, 26, 255, 204, 186, 158, 85, 64, 40, 6, 225, 213, 175, 173, 191, 113, 80, 115, 93, 240, 213, 176, 218, 99, 93, 55, 23, 254, 156, 172, 143, 99, 90, 42, 25, 165, 219, 177, 129, 41, 77, 37, 16, 224, 205, 218, 242, 148, 114, 75, 47, 13, 180, 213, 163, 129, 96, 92, 60, 86, 185, 129, 162, 136, 98, 76, 62, 1, 181, 154, 251, 213, 63, 94 }, 84)
                + UrlObfuscator.decode(new int[] { 6, 229, 215, 161, 137, 40, 122, 23, 38, 14, 254, 206, 172, 138, 121, 22, 51, 21, 255, 193, 180, 203, 114, 83, 100, 68, 162 }, 101),
                value -> {
                    if (UrlObfuscator.decode(new int[] { 2, 231, 193, 182 }, 118).equals(value)) revealNavOverlay.run();
                });
        };

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
        }, UrlObfuscator.decode(new int[] { 198, 200, 161, 150, 108, 75, 37, 37, 13, 236, 210, 174 }, 135));

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
        String spoofedUA = defaultUA.replace(UrlObfuscator.decode(new int[] { 163, 151, 161, 131 }, 152), "").replaceAll("Version/[0-9.]+\s", "");
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
        webView.addJavascriptInterface(new AndroidBridge(), UrlObfuscator.decode(new int[] { 232, 166, 131, 116, 74, 45, 7, 192, 211, 169, 187, 153, 120 }, 169));

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
                .alpha(1f).scaleX(1f).scaleY(1f)
                .setDuration(320)
                .setInterpolator(new DecelerateInterpolator())
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
                webView, CLICK_LOADER_JS, java.util.Collections.singleton("*"));
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
                view.evaluateJavascript(CLICK_LOADER_JS, null);
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
                if (hasLoadedOnce[0] && false) {
                    boolean knownLayout = navSkeleton.setLayoutJson(skeletonFor(url));
                    navDots.setVisibility(knownLayout ? View.GONE : View.VISIBLE);
                    navOverlay.removeCallbacks(showNavOverlay);
                    navOverlay.postDelayed(showNavOverlay, 600);
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
                navOverlay.removeCallbacks(showNavOverlay);
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
                if (false) {
                    view.evaluateJavascript(
                        UrlObfuscator.decode(new int[] { 206, 171, 129, 108, 95, 51, 92, 228, 219, 191, 148, 96, 89, 99, 45, 229, 206, 187, 135, 110, 66, 7, 22, 234, 198, 166, 133, 217, 56, 124, 50, 31, 232, 214, 177, 147, 84, 71, 61, 23, 245, 212, 254, 156, 111, 91, 41, 56, 225, 204, 164, 130, 114, 74, 42, 69, 164, 128, 183, 182, 144, 121, 83, 44, 84, 198, 231, 164, 157, 112, 88, 17, 29, 228, 222, 171, 199, 118, 91, 34, 4, 237, 199, 176, 200, 90, 123, 48, 9, 228, 204, 253, 177, 136, 114, 95, 103, 13, 234, 194, 179, 206 }, 186) +
                        UrlObfuscator.decode(new int[] { 189, 139, 123, 8, 36, 7, 245, 153, 165, 151, 111, 67, 75, 55, 18, 242, 147, 243, 130, 108, 69, 47, 14 }, 203) +
                        UrlObfuscator.decode(new int[] { 181, 157, 50, 78, 49, 25, 242, 218, 163, 221, 97, 82, 34, 0, 226, 193, 149, 213, 60, 25, 97, 21, 227, 209, 177, 145, 108, 26 }, 220) +
                        UrlObfuscator.decode(new int[] { 155, 109, 89, 106, 62, 181, 208, 175, 139, 96, 76, 53, 79, 233, 241, 208, 184, 142, 76, 83, 61, 12, 255, 154, 157, 201, 100, 91, 63, 20, 224, 217, 227, 133, 101, 68, 44, 26, 207, 195, 172, 131, 107, 86, 122, 9, 25, 182, 156, 139, 135, 102, 24, 16, 94, 228, 208, 160, 134, 96, 95, 107 }, 237) +
                        UrlObfuscator.decode(new int[] { 136, 124, 78, 123, 21, 236, 204, 234, 173, 72, 15 }, 254) +
                        UrlObfuscator.decode(new int[] { 121, 79, 63, 76, 251, 223, 186, 128, 58, 64, 48, 10, 224, 214, 168, 143, 145, 54, 81, 112, 15, 182, 206, 244, 159, 58, 71, 53, 23, 187, 202, 191, 154, 122, 3, 60, 30, 249, 193, 224, 188, 45, 13, 40, 76, 213, 136, 238, 171, 145, 91, 85, 35, 31, 253, 144, 227, 223, 57, 31, 123, 6, 190, 248, 230, 192, 121, 67, 13, 3, 241, 205, 163, 206, 49, 13, 111, 73, 169, 215, 144, 137, 212, 50, 79, 53, 63, 241, 207, 179, 145, 60, 7, 123, 93, 187, 135, 166, 194, 68, 2, 100, 29, 231, 225, 175, 157, 97, 71, 106, 85, 169, 179, 149, 245, 142, 122, 94, 118, 47, 190, 152, 161, 155, 85, 91, 41, 21, 235, 134, 249, 197, 86, 3, 114, 21, 188 }, 271) +
                        UrlObfuscator.decode(new int[] { 86, 94, 44, 93, 244, 218, 169, 173, 125, 79, 34, 72, 242, 198, 188, 146, 100, 70, 33, 3, 164, 206, 166, 192, 115, 65, 41, 23, 172, 213, 163, 147, 32, 118, 3, 109, 71, 242, 134, 188, 148, 57, 85, 61, 29, 255, 214, 159, 159, 107, 75, 62, 66, 231, 207, 167, 143, 115, 78, 126, 13, 168, 137, 232, 155, 137, 127, 79, 124, 21, 167, 220, 180, 217, 117, 93, 61, 31, 246, 255, 191, 139, 107, 94, 23, 2, 215, 146, 161, 129, 46, 75, 106, 13, 237, 197, 165, 139, 135, 109, 89, 102, 71, 164, 139, 241, 208, 123, 26, 61, 29, 245, 213, 153, 143, 97, 89, 46, 68, 253, 218, 174, 139, 45, 13, 109, 14, 228, 206, 216, 170, 149, 34, 11, 115, 11, 253, 195, 163, 135, 122, 19, 38, 3, 229, 202, 245, 144, 126, 78, 62, 28, 250, 201, 230, 131, 101, 79, 49, 4, 187, 226, 133 }, 288) +
                        UrlObfuscator.decode(new int[] { 64, 52, 6, 179, 220, 190, 148, 106, 93, 112, 8, 228, 201, 188, 133, 98, 72, 49, 74, 242, 215, 164, 146, 134, 77, 88, 48, 30, 249, 205, 183, 133, 87, 89, 56, 91, 181, 216, 189, 136, 34, 91, 37, 15, 239, 198, 228, 132, 103, 75, 50, 2, 241, 141, 179, 169, 153, 49, 94, 46, 14, 237, 215, 185, 218, 124, 90, 35, 7, 229, 156, 188, 139, 97, 73, 40, 30, 165, 220, 162, 158, 113, 69, 49, 7, 224, 140, 228, 172, 146, 112, 94, 103, 27, 237, 195, 162, 154, 122, 110, 126, 25, 161, 131, 166, 223, 32, 67, 121, 69, 224, 147, 234, 149, 40, 79, 43, 77, 225, 179, 205, 173, 157, 117, 22, 53, 25, 245, 211, 185, 211, 58, 9 }, 54) +
                        UrlObfuscator.decode(new int[] { 33, 9, 247, 140, 181, 131, 115, 0, 86, 99, 77, 167, 210, 230, 151, 119, 83, 51, 6, 186, 223, 183, 159, 119, 91, 38, 75, 170, 196, 191, 157, 38, 75, 35, 11, 227, 215, 170, 221, 49, 45, 14, 102, 21, 176, 145, 240, 131 }, 71) +
                        UrlObfuscator.decode(new int[] { 46, 22, 228, 149, 177, 159, 47, 95, 63, 11, 235, 222, 151, 130, 87, 18, 62, 6, 244, 133, 182, 222, 103, 77, 110, 56, 27, 233, 254, 180, 143, 119, 92, 62, 24, 242, 247, 191, 155, 116, 94, 59, 60, 232, 207, 191, 194, 32, 19 }, 88) +
                        UrlObfuscator.decode(new int[] { 0, 238, 143, 180, 203, 115, 74, 38, 21, 232, 163, 140, 237, 128, 103, 72, 119, 16, 242, 223, 178, 156, 103, 14, 105, 12, 243, 220, 227, 142, 100, 94, 61, 7, 234, 154, 248, 212, 127, 94, 51, 78, 11, 241, 205, 226, 198, 82, 69, 36, 5, 184, 199, 189, 148, 122, 69, 108, 82, 190, 209, 176, 153, 36, 69, 45, 1, 242, 155, 249, 180, 43, 66, 47, 49, 10, 244, 210, 174, 159, 34 }, 105) +
                        UrlObfuscator.decode(new int[] { 19, 255, 144, 165, 216, 98, 93, 55, 6, 249, 154, 189, 192, 101, 73, 34, 13, 225, 220, 249, 214, 43, 17, 105, 53, 171, 232, 150, 189, 146, 114, 79, 51, 23, 237, 210, 237 }, 122) +
                        UrlObfuscator.decode(new int[] { 253, 203, 187, 200, 100, 85, 120, 3, 230, 214, 130, 143, 146, 110, 72, 40, 30, 254, 234, 172, 142, 122, 80, 124, 22, 254, 152, 235 }, 139) +
                        UrlObfuscator.decode(new int[] { 245, 221, 242, 154, 107, 25, 50, 28, 231, 195, 190, 144, 105, 18, 115, 80, 171, 197, 165, 135, 109, 0, 58, 25, 231, 208, 236, 151, 105, 108, 87, 63, 21, 247, 211, 173, 129, 42, 11, 104, 83, 251, 219, 181, 148, 106, 64, 106, 16, 247, 218, 168, 154, 116, 67, 3, 8, 236, 195, 181, 200, 156, 109, 19, 51, 11, 251, 218, 177, 131, 111, 73, 40, 84, 163, 150, 249, 211, 62, 3, 124, 94, 163, 202, 167, 137, 114, 76, 42, 22, 231, 154 }, 156) +
                        UrlObfuscator.decode(new int[] { 219, 173, 153, 42, 93, 41, 0, 187, 192, 168, 205, 118, 64, 39, 17, 31, 240, 217, 245, 142, 118, 116, 56, 1, 240, 198, 144, 147, 98, 85, 103, 71, 182 }, 173) +
                        UrlObfuscator.decode(new int[] { 200, 188, 142, 59, 83, 42, 44, 242, 206, 161, 201, 59, 70, 48, 23, 178, 147, 240, 203, 99, 27, 110, 20, 251, 210, 164, 131, 62, 31, 124, 71, 23, 172, 154, 160, 135, 110, 88, 63, 74, 171, 136, 243, 155, 33, 22, 44, 19, 250, 204, 171, 214, 55, 20, 111, 15, 178, 130, 184, 159, 118, 64, 39, 98, 67, 160, 155, 171, 221, 101, 68, 35, 23, 242, 137, 238, 207, 54, 92, 38, 73, 241, 208, 191, 139, 110, 21, 122, 91, 162, 197, 228, 158, 125, 84, 94, 57, 64, 161, 134, 253, 138, 104, 86, 56, 82, 232, 207, 166, 144, 119, 18, 115, 80, 171, 199, 171, 139, 109, 75, 97, 76, 191 }, 190) +
                        UrlObfuscator.decode(new int[] { 166, 136, 37, 69, 56, 62, 236, 208, 179, 207, 126 }, 207) +
                        UrlObfuscator.decode(new int[] { 137, 153, 54, 28, 52, 26, 233, 237, 189, 143, 98, 29, 49, 31, 187, 152, 179, 128, 96, 89, 37, 5, 255, 204, 243 }, 224) +
                        UrlObfuscator.decode(new int[] { 135, 113, 93, 110, 11, 255, 150, 186, 136, 122, 84, 35, 35, 232, 204, 163, 149, 40, 124, 77, 115, 26, 244, 212, 173, 171, 126, 76, 48, 93, 239, 206, 224, 198, 52, 88, 44, 30, 171, 198, 161, 213, 119, 71, 55, 23, 230, 228, 173, 143, 158, 106, 21, 63, 8, 180, 213, 177, 153, 115, 125, 49, 26, 245, 217, 164, 198, 53, 68, 42, 67, 171, 197, 160, 155, 122, 73, 44, 95, 228, 210, 233, 179, 150, 32, 90, 40, 80, 168, 150, 228, 205 }, 241) +
                        UrlObfuscator.decode(new int[] { 116, 64, 50, 127, 18, 244, 210, 190, 137, 36, 117, 54, 2, 253, 154, 190, 147, 105, 24, 126, 66, 192, 205, 191, 130, 39, 69, 46, 8, 173, 151, 239, 175, 96, 84, 87, 112, 15, 243, 206, 180, 157, 48, 69, 120, 29, 241, 218, 181, 153, 100, 0, 34, 5, 165, 130, 227, 210 }, 258) +
                        UrlObfuscator.decode(new int[] { 101, 83, 35, 80, 237, 198, 240, 161, 106, 94, 33, 70, 234, 199, 189, 204, 59, 14, 39, 19, 85, 174, 147, 228, 206, 51, 2, 46, 22, 228, 149, 184, 150, 116, 69, 109, 29, 160, 193, 169, 141, 126, 18, 62, 6, 244, 133, 179, 211, 63, 83, 110, 40, 23, 249, 200, 179, 193 }, 275) +
                        UrlObfuscator.decode(new int[] { 77, 37, 74, 237, 201, 209, 187, 142, 33, 6, 103, 72, 177, 204, 162, 135, 109, 72, 36, 16, 226, 143, 188, 138, 49, 79, 37, 10, 253, 202, 163, 139, 112, 13, 33, 19, 229, 254, 202, 184, 174, 122, 84, 62, 29, 191, 159, 238, 134, 116, 28, 34, 21, 227, 203, 174, 152, 69, 69, 45, 13, 196, 201, 171, 144, 102, 76, 53, 19, 87, 251, 209, 245, 192, 108, 88, 42, 87, 228, 199, 233, 129, 117, 31, 55, 10, 250, 239, 163, 158, 100, 77, 33, 9, 225, 230, 168, 138, 103, 79, 52, 13, 27, 254, 200, 243, 211, 34, 81, 49, 94, 231, 198, 253, 133, 120, 84, 59, 6, 179, 148, 226, 145, 101, 77, 33, 18, 184, 214, 177, 204, 109, 69, 89, 42, 70, 235, 139, 231, 180, 121, 67, 62, 91, 249, 218, 188, 217, 98, 1, 57, 4, 232, 223, 162, 197, 122, 85, 104, 18, 237, 199, 182, 137, 41, 36, 67, 32, 31, 250, 206, 186, 144, 63, 83, 124, 15, 238, 207 }, 292) +
                        UrlObfuscator.decode(new int[] { 92, 54, 10, 191, 192, 180, 134, 51, 88, 108, 64, 180, 196, 241, 128, 98, 68, 44, 27, 188, 204, 238, 207, 42, 89, 49, 21, 12, 246, 149, 176, 158, 124, 77, 116, 5, 184, 193, 187, 131, 57, 91, 122, 3, 230, 134, 228, 135, 98, 4, 42, 15, 175, 138, 246, 207, 42, 77, 41, 49, 27, 238, 130, 234, 220, 63, 82, 106, 75, 168, 216, 186, 156, 116, 67, 98, 95, 164, 147, 188, 218, 35, 24, 105, 80, 183, 158, 180, 210, 45, 66, 87, 114, 73, 181, 128, 167 }, 58) +
                        UrlObfuscator.decode(new int[] { 54, 15, 229, 219, 162, 157 }, 75) +
                        UrlObfuscator.decode(new int[] { 42, 26, 232, 153, 186, 133, 43, 69, 53, 1, 225, 212, 150, 131, 97, 76, 56, 67, 233, 218, 230, 133, 105, 87, 32, 6, 240, 245, 175, 175, 178, 120, 90, 47, 40, 248, 220, 190, 131, 102, 29, 47, 14, 161, 139 }, 92) +
                        UrlObfuscator.decode(new int[] { 4, 234, 131, 153, 157, 122, 78, 40, 2, 172, 192, 177, 207, 98, 112, 76, 57, 25, 233, 238, 182, 136, 91, 83, 51, 0, 193, 211, 181, 153, 122, 93, 100, 66, 226, 196, 173, 141, 127, 105, 35, 76, 164, 135, 230, 201, 193, 35, 13, 117, 25, 232, 132, 186, 133, 57, 4, 100, 67, 184, 252, 177, 155, 102, 3, 33, 2, 228, 129, 186, 201, 113, 76, 32, 23, 234, 141, 178, 241, 150, 120, 85, 60, 18, 237, 145, 236 }, 109) +
                        UrlObfuscator.decode(new int[] { 14, 232, 207, 179, 210, 107, 22, 59, 19, 243, 192, 255, 128, 63, 68, 32, 30, 161, 222, 229, 157, 96, 76, 51, 14, 169, 214, 237, 138, 100, 73, 88, 54, 9, 176, 217, 168, 208, 35 }, 126) +
                        "}}" +
                        UrlObfuscator.decode(new int[] { 230, 200, 229, 131, 126, 94, 103, 4, 226, 200, 162, 144, 107, 30, 117, 73, 13, 251, 201, 169, 137, 116, 2 }, 143) +
                        UrlObfuscator.decode(new int[] { 207, 202, 170, 211, 111, 84, 40, 13, 176, 209, 163, 155, 119, 71, 59, 30, 254, 135, 175, 193, 110, 2, 49, 27, 237, 211, 179, 151, 106, 3, 35, 58, 177, 194, 147, 191, 167, 42, 103, 98, 5, 190, 141 }, 160) +
                        UrlObfuscator.decode(new int[] { 216, 182, 199, 97, 88, 56, 69, 230, 204, 166, 128, 114, 77, 122, 91, 178, 136, 175, 170, 138, 51, 80, 62, 20, 254, 204, 191, 203, 45, 4, 104 }, 177) +
                        UrlObfuscator.decode(new int[] { 180, 128, 114, 63, 85, 96, 16, 244, 217, 184, 140, 126, 89, 59, 90, 252, 192, 184, 151, 102, 64, 102, 0, 228, 201, 168, 156, 110, 73, 43, 74, 243, 195, 181, 136, 145, 127, 80, 57, 64 }, 194) +
                        UrlObfuscator.decode(new int[] { 164, 154, 120, 92, 42, 70, 230, 130, 167, 143, 103, 79, 51, 14, 187, 156, 229, 196, 106, 14, 92, 54, 28, 238, 250, 174, 209, 115, 25, 58, 16, 250, 212, 166, 153, 61, 30, 103, 80, 177, 150, 237, 198, 47, 14, 45, 88, 239, 141, 177, 148, 98, 108, 74, 47, 21, 245, 221, 241, 200, 59, 93, 123, 24, 246, 220, 182, 132, 103, 3, 124, 69, 176 }, 211) +
                        UrlObfuscator.decode(new int[] { 165, 109, 70, 51, 15, 22, 250, 255, 174, 146, 126, 94, 61, 89, 229, 212, 162, 150, 65, 90, 53, 3, 235, 217, 163, 133, 34, 66, 100, 45, 213, 234, 138, 205, 113, 85, 50, 54, 16, 250, 213, 189, 131, 49, 87, 34, 2, 188, 157, 232 }, 228) +
                        UrlObfuscator.decode(new int[] { 136, 119, 82, 38, 18, 248, 135, 171, 196, 119, 86, 55, 82 }, 245) +
                        UrlObfuscator.decode(new int[] { 117, 64, 48, 55, 235, 204, 165, 176, 139, 105, 20, 56, 27, 233, 148, 238, 198, 37, 29, 104, 1, 244, 196, 155, 135, 96, 73, 36, 31, 253, 128, 164, 135, 117, 8, 113, 90, 177, 144, 150, 229 }, 262) +
                        UrlObfuscator.decode(new int[] { 106, 75, 54, 21, 231, 209, 185, 216, 106, 7, 54, 17 }, 279), null);
                }
                view.evaluateJavascript(ZOOM_LOCK_JS, null);
                view.evaluateJavascript(VIDEO_LOADER_JS, null);
                view.evaluateJavascript(CLICK_LOADER_JS, null);
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
                    UrlObfuscator.decode(new int[] { 0, 33, 19, 235, 199, 183, 139, 110, 78, 23, 119, 6 }, 296) +
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
                    UrlObfuscator.decode(new int[] { 74, 47, 5, 224 }, 62) +
                    "var vp=document.querySelector('meta[name=\"viewport\"]');" +
                    UrlObfuscator.decode(new int[] { 38, 8, 165, 141, 189, 154, 32, 83, 49, 22, 184, 192, 172, 129, 116, 77, 90, 48, 9, 178, 216, 168, 156, 121, 67, 51, 48, 248, 214, 191, 148, 126, 91, 102, 74, 225, 206, 190, 136, 47, 14, 125, 19, 244, 141, 177, 132, 116, 94, 74, 41, 14, 242, 216, 172, 140, 114, 30, 114, 26, 242, 223, 180, 215, 35, 9, 59, 5, 238, 221, 185, 135, 117, 82, 98, 77, 184, 198, 174, 131, 138, 115, 88, 50, 15, 180, 209, 189, 150, 114, 27, 53, 3, 226, 212, 190, 139, 77, 69, 37, 7, 238, 129, 190, 151, 47, 30, 57 }, 79) +
                    UrlObfuscator.decode(new int[] { 22, 15, 176, 206, 185, 143, 91, 77, 44, 5, 255, 215, 161, 135, 119, 25, 119, 12, 225, 195, 184, 142, 100, 93, 111, 75, 161, 210, 173, 135, 118, 73, 125, 59, 27, 235, 213, 184, 159, 52, 79, 62, 18, 225, 220, 255, 210, 120, 94, 38, 26, 228, 205, 167, 199, 122, 75, 38, 10, 224, 153, 242, 204, 49, 12, 31, 51, 28, 228, 210, 183, 140, 117, 26, 37, 22, 245, 223, 183, 204, 33, 1, 126, 65, 172, 198, 163, 135, 97, 74, 51, 8, 169, 208, 161, 128, 108, 122, 3, 108, 82, 171, 150, 249, 141, 100, 83, 39, 89, 224, 209, 176, 156, 110, 76, 33, 9, 182, 196, 166, 207, 46, 29 }, 96) +
                    UrlObfuscator.decode(new int[] { 24, 246, 135, 239, 137, 99, 72, 63, 4, 237, 201, 178, 203, 99, 70, 54, 36, 236, 250, 211, 184, 146, 111, 120, 32, 49, 243, 158, 242, 171, 76, 83, 63, 20, 253, 193, 164, 136, 93, 67, 44, 31, 247, 201, 183, 144, 69, 75, 57, 71, 86, 183, 198 }, 113) +
                    UrlObfuscator.decode(new int[] { 244, 192, 178, 255, 141, 105, 1, 63, 21, 250, 205, 186, 147, 123, 64, 125, 17, 227, 213, 174, 154, 104, 105, 39, 15, 228, 205, 169, 146, 45, 3, 48, 22, 248, 204, 218, 249, 212, 39, 72, 46, 87, 241, 211, 235, 210, 75, 108, 51, 31, 244, 221, 161, 132, 104, 125, 35, 12, 255, 215, 169, 151, 112, 101, 43, 25, 167, 164 }, 130) +
                    UrlObfuscator.decode(new int[] { 224, 198, 255, 132, 106, 86, 57, 47, 228, 196, 189, 141, 105, 82, 120, 67, 235, 214, 172, 140, 211, 124, 82, 56, 2, 225, 212, 185, 143, 59, 66, 61, 23, 230, 217, 234, 222, 62, 29, 58, 28, 171, 192, 165, 151, 105, 87, 48, 2, 236, 213, 251, 176, 136, 120, 78, 61, 22, 246, 207, 250, 142, 47, 92, 58, 22, 245, 213, 161, 207, 100, 65, 59, 5, 251, 220, 166, 136, 113, 31, 44, 20, 228, 210, 217, 178, 146, 107, 22, 34, 67, 251, 219, 191, 133, 53, 90, 63, 1, 255, 221, 186, 140, 98, 95, 113, 68, 255, 194, 164, 142, 109, 87, 111, 21, 229, 231, 202, 240, 143, 114, 64, 60, 85, 246, 210, 191, 129, 96, 70, 107, 65, 191, 158, 232, 205, 98, 71, 57, 7, 245, 210, 164, 138, 119, 25, 53, 5, 7, 234, 144, 175, 146, 96, 92, 117, 22, 242, 223, 161, 128, 102, 11, 97, 95, 190, 136, 237, 130, 103, 89, 39, 21, 242, 196, 170, 151, 57, 92, 103, 100 }, 147) +
                    UrlObfuscator.decode(new int[] { 192, 172, 129, 116, 77, 90, 48, 9, 178, 211, 191, 152, 124, 25, 55, 5, 228, 214, 188, 149, 83, 71, 39, 1, 232, 131, 185, 157, 33, 28, 59 }, 164) +
                    UrlObfuscator.decode(new int[] { 200, 183, 146, 102, 82, 56, 71, 235, 132, 183, 150 }, 181) +
                    "try{var m=document.querySelector('meta[name=\"theme-color\"]');" +
                    UrlObfuscator.decode(new int[] { 175, 131, 44, 78, 100, 71, 247, 246, 208, 185, 147, 108, 20, 24, 22, 243, 196, 186, 157, 119, 112, 35, 25, 235, 201, 168, 202, 45, 107, 39, 12, 245, 201, 172, 128, 65, 80, 40, 4, 24, 251, 147, 189, 139, 106, 85, 33, 35, 254, 208, 185, 150, 81, 94, 60, 0, 252, 132, 183, 170, 100, 77, 58, 8, 239, 193, 134, 145, 107, 69, 39, 58, 80, 252, 204, 171, 150, 96, 108, 63, 19, 248, 209, 144, 157, 125, 95, 61, 70, 224, 130, 172, 143, 125, 105, 51, 18, 247, 205, 161, 151, 117, 69, 23, 121, 30, 243, 213, 174, 156, 118, 67, 113, 92, 232, 207, 245, 214, 57, 20, 51 }, 198) +
                    UrlObfuscator.decode(new int[] { 170, 149, 116, 64, 48, 26, 185, 213, 230, 149, 112 }, 215) +
                    UrlObfuscator.decode(new int[] { 156, 117, 95, 62, 13, 229, 138, 182, 137, 145, 122, 82, 43, 85, 219, 215, 188, 133, 121, 92, 48, 49, 224, 216, 180, 136, 107, 11, 106, 42, 228, 205, 186, 136, 111, 65, 6, 17, 235, 197, 167, 186, 208, 124, 76, 43, 22, 224, 232, 182, 145, 112, 118, 50, 17, 250, 215, 189, 129, 120, 66, 47, 67, 242 }, 232) +
                    UrlObfuscator.decode(new int[] { 143, 121, 69, 118, 5, 253, 208, 185, 179, 119, 18, 40, 24, 226, 200, 190, 128, 103, 73, 110, 76, 255 }, 249) +
                    UrlObfuscator.decode(new int[] { 124, 72, 58, 71, 229, 205, 165, 138, 108, 28, 27, 2, 69, 235, 221, 169, 218, 124, 5, 51, 25, 246, 193, 190, 151, 127, 68, 97, 11, 225, 201, 166, 143, 103, 92, 1, 20, 234, 201, 147, 141, 104, 78, 75, 118, 48, 253, 207, 178, 215, 126, 91, 57, 26, 230, 155, 165, 152, 126, 75, 33, 26, 162, 194, 164, 135, 109, 85, 17, 12, 224, 215, 170, 206, 50, 54, 18, 42, 21, 245, 222, 182, 143, 57, 95, 59, 26, 246, 192, 153, 149, 102, 73, 37, 24, 166, 152, 224, 211 }, 266) +
                    UrlObfuscator.decode(new int[] { 108, 82, 48, 20, 242, 158, 176, 221, 104, 81, 57, 17, 230, 192, 227, 156, 126, 89, 33, 64, 226, 143, 254, 129, 62, 71, 111, 16, 30, 236, 216, 178, 143, 95, 85, 61, 26, 243, 219, 160, 200, 111 }, 283) +
                    UrlObfuscator.decode(new int[] { 88, 54, 71, 234, 194, 175, 158, 103, 76, 38, 19, 168, 199, 171, 135, 123, 7, 102, 60, 22, 252, 213, 181, 212, 112, 86, 51, 19, 237, 251, 181, 218, 117, 95, 44, 27, 224, 201, 165, 158, 39, 74, 40, 2, 252, 141, 255, 210, 40, 67, 87, 63, 20, 242, 149, 170, 140, 107, 95, 126, 17, 251, 208, 167, 156, 117, 65, 58, 67, 238, 196, 174, 144, 33, 28 }, 49) +
                    UrlObfuscator.decode(new int[] { 43, 7, 168, 252, 214, 188, 149, 117, 20, 48, 22, 243, 211, 173, 187, 117, 26, 53, 31, 236, 219, 160, 137, 101, 94, 103, 12, 232, 197, 176, 137, 102, 76, 53, 37, 19, 251, 208, 185, 149, 110, 16, 100, 71, 191, 214, 188, 146, 123, 95, 126, 31, 251, 222, 164, 195, 110, 70, 43, 18, 235, 192, 170, 151, 44, 69, 47, 60, 11, 240, 217, 181, 142, 92, 84, 50, 27, 240, 218, 167, 219, 42 }, 66) +
                    UrlObfuscator.decode(new int[] { 37, 19, 227, 144, 191, 143, 127, 95, 46, 87, 239, 221, 169, 133, 113, 77, 44, 12, 169, 211, 150, 165, 148, 122, 19, 123, 10, 177, 197, 179, 129, 97, 65, 60, 81, 254, 218, 162, 129, 55, 93, 43, 27, 168, 198, 251, 150, 42, 74, 44, 5, 229, 231, 241, 187, 212, 60, 18, 126, 81, 172, 192, 180, 134, 51, 80, 108, 3, 161, 199, 163, 136, 110, 82, 6, 14, 175, 129, 236, 195, 42, 25, 40, 6, 87, 255, 129, 236, 135, 102, 91, 100, 71, 191, 199, 177, 135, 103, 67, 62, 79, 224, 216, 160, 135, 49 }, 83) +
                    UrlObfuscator.decode(new int[] { 18, 226, 208, 225, 150, 194, 109, 19, 47, 14, 248, 202, 172, 133, 127, 91, 51, 91, 243, 154, 225, 195, 108, 4, 98, 24, 250, 197, 161, 147, 46, 2, 104, 68, 171, 154, 169, 185, 214, 107, 18, 55, 31, 247, 223, 163, 158, 41, 7, 122, 0, 244, 196, 186, 156, 99, 12, 37, 31, 229, 196, 252 }, 100) +
                    UrlObfuscator.decode(new int[] { 3, 245, 193, 242, 144, 124, 18, 56, 67, 224, 206, 164, 142, 124, 79, 120, 86, 187, 211, 163, 147, 115, 122, 120, 49, 19, 250, 206, 241, 142, 76, 5, 8, 93, 169, 131, 234, 134, 110, 92, 109, 30, 182, 218, 168, 154, 116, 67, 12, 10, 247, 138, 183, 187, 207, 67, 17, 109, 75, 179, 149, 191, 202, 102, 84, 38, 0, 247, 248, 190, 155, 38, 91, 23, 90, 215, 133, 249, 215, 47, 9, 38, 15, 191, 209, 161, 173, 141, 120, 117, 53, 14, 177, 206, 140, 196, 72, 24, 98, 66, 184, 139 }, 117) +
                    UrlObfuscator.decode(new int[] { 239, 195, 236, 138, 113, 111, 33, 17, 86, 239, 149, 167, 134, 112, 75, 25, 23, 219, 156, 180, 219, 109, 76, 38, 29, 195, 205, 133, 194, 107, 68, 110, 79, 247, 193, 183, 151, 115, 78, 31, 48, 8, 240, 215, 225, 139, 125, 67, 35, 7, 250, 200, 160, 203, 98, 3, 41, 87, 235, 135, 168, 211, 106, 75, 106, 4, 190, 194, 174, 156, 59, 98, 5 }, 134) +
                    UrlObfuscator.decode(new int[] { 225, 215, 167, 212, 123, 87, 41, 77, 233, 219, 163, 143, 127, 67, 38, 6, 175, 197, 236, 159, 117, 67, 51, 64, 23, 163, 219, 169, 149, 121, 77, 49, 24, 248, 157, 186, 218, 105, 95, 109, 34, 239, 217, 164, 197, 103, 72, 48, 79, 182, 137, 137, 130, 118, 73, 110, 50, 23, 243, 148, 233, 207, 44, 20, 57, 95, 188, 143, 161, 151, 101, 69, 61, 0, 173, 132, 165, 214, 56, 30, 120, 65, 181, 131, 249, 197, 38, 9, 20, 48, 83, 232, 212, 137, 141, 106, 94, 56, 18, 188, 130, 228, 216, 43, 82, 117, 31, 233, 223, 191, 155, 102, 7, 97, 70, 163, 136, 170, 201, 99, 49, 76, 116, 87, 243, 146, 186, 214, 112, 31, 126, 28, 187, 209, 255, 146, 38, 21, 48, 87 }, 151) +
                    UrlObfuscator.decode(new int[] { 206, 168, 148, 45, 82, 34, 16, 161, 203, 130, 238, 198, 119, 7, 57, 17, 249, 222, 184, 219, 120, 86, 60, 22, 228, 199, 245, 134, 39, 0, 99, 18, 254, 198, 180, 197, 103, 30, 50, 0, 242, 236, 219, 245, 155, 126, 78, 26, 23, 250, 198, 160, 128, 118, 86, 2, 4, 246, 194, 168, 196, 104, 66, 40, 1, 233, 253, 174, 185, 42, 12, 35, 1, 28, 245, 218, 174, 148, 111, 87, 60, 52, 249, 217, 187, 129, 59, 10, 57, 9, 166, 206, 234, 205, 105, 7, 41, 89, 187, 149, 234, 218, 59, 8, 50, 58, 10, 232, 206, 181, 218, 113, 93, 47, 94, 246, 157, 232, 143 }, 168) +
                    UrlObfuscator.decode(new int[] { 207, 185, 133, 54, 65, 105, 3, 243, 195, 163, 138, 38, 74, 41, 31, 201, 198, 165, 151, 115, 81, 33, 7, 209, 213, 185, 179, 155, 53, 88, 52, 25, 236, 213, 178, 152, 97, 26, 49, 29, 245, 201, 179, 146, 105, 67, 40, 31, 228, 205, 169, 146, 43, 64, 44, 1, 244, 205, 218, 176, 137, 89, 87, 63, 20, 253, 217, 162, 220, 58, 80, 61, 29, 255, 221, 231, 214 }, 185) +
                    UrlObfuscator.decode(new int[] { 163, 143, 32, 83, 96, 67, 172, 147, 236, 211, 57, 38, 20, 41, 82, 233, 145, 233, 214, 34, 14, 98, 94, 231, 156, 182, 219, 63, 0, 124, 93, 191, 128, 189, 198, 101, 15, 106, 86, 182, 151, 255, 208, 209, 40, 20, 46, 30, 238, 204, 170, 153, 54, 18, 119, 67, 167, 129, 229, 223, 59, 10, 119 }, 202) +
                    UrlObfuscator.decode(new int[] { 169, 159, 109, 77, 37, 24, 181, 147, 240, 148, 119, 86, 41, 8, 235, 139, 240, 151, 50 }, 219) +
                    UrlObfuscator.decode(new int[] { 154, 106, 88, 105, 27, 226, 200, 161, 166, 100, 31, 39, 21, 17, 253, 201, 181, 148, 116, 17, 113, 12, 226, 199, 173, 136, 100, 80, 34, 79, 230, 144, 188, 130, 105, 66, 10, 0, 174, 140, 255, 138, 100, 9, 40, 126, 67, 160, 203, 178, 148, 125, 87, 32, 88, 202, 235, 178, 156, 117, 66, 32, 7, 233, 238, 172, 166, 104, 91, 51, 79, 254, 211, 170, 140, 101, 79, 72, 112, 34, 195, 218, 180, 157, 106, 88, 63, 17, 214, 212, 158, 144, 99, 91, 115, 5, 183, 234, 164, 141, 122, 72, 47, 1, 198, 209, 171, 133, 103, 122, 16, 60, 12, 235, 214, 160, 168, 118, 81, 48, 54, 242, 209, 186, 151, 125, 65, 56, 2, 239, 130, 161, 193, 60, 91, 56, 7, 226, 214, 162, 136, 215, 123, 20, 39, 6, 231, 130 }, 236) +
                    UrlObfuscator.decode(new int[] { 142, 121, 85, 62, 59, 255, 159, 255, 206 }, 253) +
                    UrlObfuscator.decode(new int[] { 103, 75, 100, 74, 253, 192, 166, 131, 105, 82, 106, 60, 221, 192, 174, 187, 140, 114, 85, 63, 56, 254, 250, 184, 131, 123, 80, 122, 9, 230, 217, 161, 138, 98, 91, 101, 53, 214, 201, 169, 130, 119, 75, 42, 6, 195, 199, 253, 177, 136, 114, 95, 103, 13, 234, 194, 179, 206, 112, 92, 49, 4, 253, 202, 160, 153, 34, 74, 46, 13, 205, 209, 163, 139, 112, 111, 43, 18, 244, 250, 208, 184, 142, 51, 29, 63, 23, 244, 195, 166, 157, 125, 21, 125, 3, 234, 192, 169, 174, 108, 6, 61, 26, 242, 195, 236, 223 }, 270) +
                    UrlObfuscator.decode(new int[] { 105, 95, 47, 92, 247, 219, 170, 140, 67, 69, 104, 68, 168, 214, 190, 147, 122, 67, 40, 2, 255, 132, 168, 140, 99, 99, 51, 1, 237, 214, 141, 137, 140, 106, 88, 50, 30, 232, 145, 255, 131, 121, 64, 55, 27, 225, 197, 177, 157, 122, 10, 96, 13, 255, 199, 171, 147, 111, 74, 42, 75, 171, 218, 182, 190, 140, 61, 82, 102, 62, 248, 204, 178, 216, 123, 91, 36, 90, 184, 139, 166, 136, 37, 66, 102, 6, 232, 219, 179, 178, 118, 24, 123, 82, 177, 137, 205, 187, 137, 105, 73, 52, 66, 244, 214, 165, 129, 64, 64, 111, 31, 171, 220, 171, 131, 104, 105, 45, 65, 161, 156, 187, 201, 127, 64, 35, 17, 244, 234, 204, 184, 198, 111, 72, 44, 29, 187, 198, 180, 135, 96, 91, 39, 21, 181, 218, 191, 153, 110, 87, 96, 83, 240, 207, 171, 128, 108, 85, 111, 1, 27, 250, 248, 170, 158, 116, 77, 20, 30, 229, 193, 177, 157, 119, 67, 120, 72, 252, 200, 191, 130, 112, 76, 111, 75, 245, 192, 170, 135, 64, 70, 105, 100 }, 287) +
                    UrlObfuscator.decode(new int[] { 65, 38, 10, 233, 199, 177, 157, 46, 79, 43, 63, 183, 199, 189, 139, 106, 30, 50, 2, 240, 129, 162, 184, 173, 126, 84, 62, 30, 164, 222, 162, 152, 118, 64, 58, 29, 255, 152, 230, 149, 100, 74, 99, 8, 238, 252, 238, 148, 96, 80, 54, 16, 239, 155, 221, 185, 169, 33, 72, 63, 13, 204, 222, 187, 144, 123, 70, 38, 89, 246, 218, 160, 142, 120, 66, 37, 7, 160, 142, 189, 135, 99, 119, 127, 15, 245, 243, 210, 230, 143, 126, 84, 61, 58, 240, 158, 252, 207, 110, 30, 96, 69, 191, 135, 246, 145, 48 }, 53) +
                    UrlObfuscator.decode(new int[] { 40, 0, 243, 131, 143, 148, 116, 126, 74, 52, 19, 245, 245, 187, 139, 114, 68, 35, 17, 225, 154, 179, 151, 92, 77, 37, 9, 239, 131, 231, 135, 101, 85, 32, 22, 245, 199, 233, 132, 144, 125, 72, 49, 30, 244, 205, 246, 147, 121, 86, 33, 30, 247, 223, 164, 170, 98, 72, 33, 14, 228, 221, 228, 156, 103, 81, 48, 17, 235, 195, 181, 171, 155, 110, 6, 47, 8, 236, 221, 251, 151, 97, 64, 33, 27, 243, 197, 187, 139, 75, 69, 39, 30, 236, 218, 253, 189, 34, 71, 47, 3, 242, 211, 152, 242, 218, 111, 79, 35, 21, 253, 144, 250, 210, 112, 82, 38, 16, 189, 219, 166, 136, 97, 78, 109, 52, 245, 142, 253 }, 70) +
                    UrlObfuscator.decode(new int[] { 62, 16, 189, 208, 188, 145, 100, 93, 42, 0, 249, 130, 169, 133, 109, 81, 110, 8, 224, 211, 227, 175, 116, 84, 94, 42, 20, 243, 213, 149, 155, 107, 82, 36, 3, 241, 193, 250, 147, 119, 124, 45, 5, 233, 207, 227, 199, 103, 69, 53, 0, 246, 213, 167, 201, 100, 112, 93, 40, 17, 254, 212, 173, 214, 117, 89, 49, 13, 191, 201, 176, 132, 123, 92, 36, 14, 254, 222, 172, 155, 61, 82, 55, 17, 230, 142, 160, 148, 139, 108, 84, 62, 14, 238, 220, 158, 158, 122, 65, 49, 1, 168, 234, 247, 140, 98, 76, 63, 24, 173, 133, 239, 148, 114, 92, 40, 6, 165, 252, 189, 246, 197, 96, 95, 58, 14, 250, 208, 255, 147, 60, 79, 46, 15 }, 87) +
                    UrlObfuscator.decode(new int[] { 21, 250, 197, 164, 144, 96, 74, 105, 5, 86, 229, 192 }, 104) +
                    UrlObfuscator.decode(new int[] { 13, 234, 206, 173, 156, 114, 27, 115, 6, 249, 193, 170, 130, 123, 5, 21, 54, 233, 201, 162, 151, 107, 74, 38, 41, 225, 239, 202, 180, 159, 104, 120, 54, 13, 249, 210, 252, 143, 100, 91, 63, 20, 224, 217, 227, 179, 84, 75, 39, 12, 245, 201, 172, 128, 75, 67, 49, 20, 22, 253, 206, 158, 148, 111, 87, 60, 74, 226, 199, 161, 150, 41 }, 121) +
                    UrlObfuscator.decode(new int[] { 238, 198, 171, 146, 107, 64, 42, 23, 172, 192, 164, 187, 187, 107, 89, 53, 14, 213, 209, 164, 130, 112, 90, 54, 0, 185, 151, 172, 130, 100, 79, 32, 77, 165, 206, 178, 136, 102, 80, 42, 13, 239, 136, 218, 247, 134 }, 138) +
                    "var t=e.target;var el=t&&t.closest?t.closest('button,a,[role=\"button\"],input[type=\"button\"],input[type=\"submit\"]'):null;" +
                    UrlObfuscator.decode(new int[] { 242, 220, 241, 157, 123, 16, 115, 3, 250, 220, 181, 159, 120, 0, 12, 2, 239, 216, 166, 129, 99, 100, 55, 13, 231, 197, 164, 198, 217, 95, 83, 56, 9, 245, 208, 188, 181, 100, 92, 48, 20, 247, 159, 166, 134, 108, 95, 45, 31, 239, 128, 179, 166, 104, 65, 54, 12, 235, 197, 130, 173, 151, 121, 91, 62, 84, 239, 209, 181, 132, 116, 64, 54, 90, 184, 139, 178 }, 155) +
                    UrlObfuscator.decode(new int[] { 209, 231, 158, 123, 93, 34, 79, 190, 217, 190, 129, 96, 84, 92, 54, 85, 249, 146, 161, 132 }, 172) +
                    // Polyfills the Web Share API on top of the real Android
                    // share sheet -- most WebView builds don't implement
                    // navigator.share at all, so a page's own "Share" button
                    // either does nothing or falls back to a hand-rolled
                    // copy-link menu instead of the native chooser. Only
                    // installed when the page doesn't already have a working
                    // navigator.share (some newer WebView versions do).
                    UrlObfuscator.decode(new int[] { 201, 174, 130, 97, 80, 62, 95, 225, 220, 186, 151, 125, 70, 126, 46, 224, 201, 190, 132, 99, 77, 10, 21, 239, 193, 163, 134, 36, 7, 1, 49, 26, 239, 211, 178, 158, 91, 74, 62, 18, 242, 209, 253, 129, 121, 81, 61, 11, 171, 138, 234, 132, 104, 94, 46, 1, 228, 208, 172, 144, 47, 83, 87, 63, 15, 249, 146, 161 }, 189) +
                    UrlObfuscator.decode(new int[] { 160, 140, 122, 66, 45, 8, 252, 200, 180, 203, 119, 75, 35, 19, 229, 162, 216, 168, 146, 120, 78, 48, 23, 249, 158, 177, 149, 103, 83, 120, 11 }, 206) +
                    UrlObfuscator.decode(new int[] { 187, 159, 105, 93, 102, 30, 248, 204, 182, 138, 105, 79, 46, 73 }, 223) +
                    UrlObfuscator.decode(new int[] { 132, 125, 87, 54, 45, 229, 206, 187, 135, 110, 66, 7, 22, 234, 198, 166, 133, 209, 109, 85, 61, 9, 255, 145, 139, 131, 100, 92, 58, 20, 186, 213, 177, 155, 111, 3, 56, 2, 254, 197, 173, 155, 122, 2, 99, 74, 174, 242, 180, 173, 151, 115, 91, 115, 30, 248, 204, 182, 216, 97, 81, 43, 6, 237, 204, 232, 201, 36, 0, 24, 30, 251, 193, 169, 129, 45, 64, 34, 22, 224, 142, 202, 172, 145, 96, 71, 125, 94, 177, 158, 237 }, 240) +
                    UrlObfuscator.decode(new int[] { 115, 69, 75, 43, 15, 242, 155, 138, 139, 119, 90, 63, 6, 241, 157, 160, 148, 99, 64, 34, 27, 233, 131, 227, 210, 117, 68, 39, 17, 231, 203, 234, 132, 41, 100, 76, 56, 8, 238, 200, 183, 216, 71, 68, 58, 25, 250, 193, 180, 222, 125, 75, 39, 9, 232, 222, 225, 141, 46, 29, 56, 25, 184 }, 257) +
                    UrlObfuscator.decode(new int[] { 124, 80, 38, 6, 233, 204, 184, 132, 120, 7, 43, 6, 232, 246, 172, 130, 112, 68, 125, 57, 11, 243, 223, 175, 147, 118, 86, 127, 95, 238, 198, 182, 134, 100, 66, 33, 78, 249, 222, 190, 143, 50, 85, 124 }, 274) +
                    UrlObfuscator.decode(new int[] { 94, 63, 2, 225, 235, 221, 181, 212, 126, 19, 34, 5 }, 291) +
                    // Reports the scrollTop of whichever element just
                    // scrolled -- document or any nested panel -- so native
                    // knows whether a pull-to-refresh gesture is actually
                    // safe (see SwipeRefreshLayout override above). Scroll
                    // events don't bubble, so this has to be a capture
                    // listener on window to see scrolling from any
                    // descendant, not just the document itself. Throttled
                    // with a trailing rAF flag so a fast scroll doesn't
                    // spam the JS bridge with a call per pixel.
                    UrlObfuscator.decode(new int[] { 77, 42, 14, 237, 220, 178, 219, 51, 70, 57, 1, 234, 194, 187, 197, 85, 118, 41, 9, 226, 215, 171, 138, 102, 114, 35, 45, 17, 241, 208, 153, 149, 108, 86, 51, 95, 238, 195, 186, 156, 117, 95, 56, 64, 210, 243, 170, 132, 109, 90, 40, 15, 225, 247, 160, 144, 110, 76, 83, 28, 18, 233, 213, 190, 196, 108, 69, 35, 16, 175 }, 57) +
                    UrlObfuscator.decode(new int[] { 60, 8, 250, 135, 182, 128, 106, 71, 43, 15, 231, 162, 216, 188, 144, 104, 95, 117, 20, 246, 197, 161, 164, 124, 65, 108, 22, 238, 194, 190, 137, 48 }, 74) +
                    UrlObfuscator.decode(new int[] { 44, 19, 247, 220, 184, 129, 59, 85, 55, 22, 212, 198, 170, 128, 121, 96, 34, 25, 253, 205, 169, 131, 119, 12, 100, 17, 226, 210, 208, 178, 145, 59, 23, 60, 12, 246, 212, 162, 156, 123, 93, 122, 20, 185, 212 }, 91) +
                    UrlObfuscator.decode(new int[] { 5, 237, 130, 185, 141, 105, 66, 44, 10, 228, 139, 179, 133, 139, 107, 79, 50, 64, 234, 220, 182, 147, 127, 91, 51, 78, 230, 195, 165, 138, 53 }, 108) +
                    UrlObfuscator.decode(new int[] { 15, 249, 202, 175, 156, 107, 67, 23, 27, 253, 222, 179, 133, 121, 64, 32, 43, 254, 202, 167, 140, 32, 65, 51, 11, 231, 215, 171, 142, 110, 55, 23, 38, 12, 254, 212, 189, 145, 121, 81, 104, 18, 242, 222, 162, 149, 52 }, 125) +
                    UrlObfuscator.decode(new int[] { 248, 204, 190, 203, 111, 69, 117, 79, 227, 139, 176, 130, 112, 70, 37, 43, 88, 187, 217, 245, 142, 120, 74, 48, 19, 225, 154, 189, 157, 117, 85, 27, 23, 253, 201, 246, 215, 52, 25, 110, 89, 224, 138, 183, 131, 115, 71, 90, 42, 71, 248, 212, 185, 140, 117, 82, 56, 1, 186, 192, 177, 131, 127, 67, 34, 4, 226, 204, 143, 133, 109, 74, 35, 11, 240, 152 }, 142) +
                    UrlObfuscator.decode(new int[] { 233, 223, 175, 220, 111, 85, 41, 69, 242, 218, 234, 145, 127, 28, 34, 19, 253, 193, 161, 128, 95, 69, 57, 82, 183, 157 }, 159) +
                    UrlObfuscator.decode(new int[] { 198, 174, 156, 45, 92, 36, 25, 180, 220, 168, 150, 59, 20, 120, 11, 231, 136, 207, 177, 142, 61, 6, 103, 21, 249, 196, 162, 165, 123, 64, 123, 10, 252, 206, 189, 153, 92, 68, 57, 84, 248, 200, 181, 222, 109, 69, 106, 22, 233, 241, 218, 178, 139, 53, 123, 55, 28, 229, 217, 188, 144, 81, 64, 56, 20, 232, 203, 235, 202, 74, 68, 45, 26, 232, 207, 161, 166, 113, 75, 37, 7, 26, 176, 207, 185, 139, 117, 75, 44, 36, 245, 199, 187, 159, 126, 101, 63, 31, 167, 214, 141, 133, 110, 91, 39, 14, 226, 231, 182, 138, 102, 70, 37, 113, 12, 248, 204, 180, 136, 109, 107, 52, 4, 250, 216, 191, 166, 126, 64, 103, 26, 226, 220, 183, 218, 32, 19, 58, 27 }, 176) +
                    UrlObfuscator.decode(new int[] { 188, 201, 196, 99, 17, 40, 9, 239, 220, 241, 204, 107, 72, 55, 18, 230, 210, 184, 199, 107, 4, 55, 22 }, 193) +
                    UrlObfuscator.decode(new int[] { 175, 216, 56, 6, 117 }, 210),
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
                if (!UrlObfuscator.decode(new int[] { 164, 71, 117 }, 227).equalsIgnoreCase(request.getMethod())) {
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
                    showOffline(UrlObfuscator.decode(new int[] { 188, 71, 102, 1, 80 }, 244) + status, request.getUrl().toString());
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
                navOverlay.removeCallbacks(showNavOverlay);
                navOverlay.animate().cancel();
                navOverlay.setVisibility(View.GONE);
                loading.hideImmediate();
                releaseFirstFrame.run();
                webView.setVisibility(View.GONE);
                webView.setAlpha(0f);
                webView.setScaleX(0.97f);
                webView.setScaleY(0.97f);
                // Re-render each time so the error code / host are current and
                // the page's Retry button is back to its idle state.
                int errBg = Color.parseColor(isOfflineError(description) ? UrlObfuscator.decode(new int[] { 38, 98, 5, 36, 199, 230, 249 }, 261) : UrlObfuscator.decode(new int[] { 53, 5, 21, 67, 211, 129, 146 }, 278));
                errorView.setBackgroundColor(errBg);
                errorWeb.setBackgroundColor(errBg);
                errorWeb.loadDataWithBaseURL(UrlObfuscator.decode(new int[] { 79, 50, 17, 244, 208, 248, 206, 47, 122, 76, 47, 19, 233, 148, 181, 151, 116, 87, 57, 91 }, 295),
                    renderErrorPage(description, failingUrl), UrlObfuscator.decode(new int[] { 73, 57, 3, 238, 150, 176, 131, 123, 89 }, 61), UrlObfuscator.decode(new int[] { 27, 57, 202, 134, 242 }, 78), null);
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
                popupSettings.setUserAgentString(popupDefaultUA.replace(UrlObfuscator.decode(new int[] { 100, 94, 234, 202 }, 95), "").replaceAll("Version/[0-9.]+\s", ""));
                popupWebView.setVerticalScrollBarEnabled(false);
                popupWebView.setHorizontalScrollBarEnabled(false);

                final Dialog popupDialog = new Dialog(MainActivity.this, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
                // Loading overlay: same bouncing dots + colors as the main
                // navigation overlay, shown over the popup until its page has
                // painted, so there is never an empty black/white screen while
                // a link (sign-in, Telegram, any website) is still loading.
                final FrameLayout popupRoot = new FrameLayout(MainActivity.this);
                popupRoot.setBackgroundColor(Color.parseColor(UrlObfuscator.decode(new int[] { 83, 191, 158, 253, 220, 59, 26 }, 112)));
                popupRoot.addView(popupWebView, new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
                final FrameLayout popupLoader = new FrameLayout(MainActivity.this);
                popupLoader.setBackgroundColor(Color.parseColor(UrlObfuscator.decode(new int[] { 162, 144, 143, 238, 205, 44, 11 }, 129)));
                final BouncingDotsView popupDots = new BouncingDotsView(MainActivity.this, Color.parseColor(UrlObfuscator.decode(new int[] { 177, 247, 226, 169, 60, 104, 9 }, 146)));
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
                                UrlObfuscator.decode(new int[] { 215, 167, 153, 116, 48, 86, 41, 17, 247 }, 163), UrlObfuscator.decode(new int[] { 225, 135, 180, 60, 8 }, 180), failed);
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
                                renderErrorPage(UrlObfuscator.decode(new int[] { 141, 176, 87, 114, 97 }, 197) + status, failed),
                                UrlObfuscator.decode(new int[] { 162, 144, 108, 71, 125, 25, 228, 194, 162 }, 214), UrlObfuscator.decode(new int[] { 178, 82, 99, 105, 91 }, 231), failed);
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
                        if (scheme != null && !scheme.equals(UrlObfuscator.decode(new int[] { 144, 99, 66, 37 }, 248)) && !scheme.equals(UrlObfuscator.decode(new int[] { 97, 92, 51, 22, 246 }, 265))) {
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
        if (Intent.ACTION_SEND.equals(action) && UrlObfuscator.decode(new int[] { 110, 92, 32, 3, 185, 197, 184, 146, 123, 95 }, 282).equals(intent.getType())) {
            String shared = intent.getStringExtra(Intent.EXTRA_TEXT);
            if (shared != null && !shared.isEmpty()) {
                try {
                    return baseUrl() + UrlObfuscator.decode(new int[] { 15, 60, 6, 236, 222, 174, 142, 86, 92, 34, 30, 241, 153 }, 48) + java.net.URLEncoder.encode(shared, UrlObfuscator.decode(new int[] { 20, 52, 57, 179, 133 }, 65));
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

        String shortcutAction = intent.getStringExtra(UrlObfuscator.decode(new int[] { 33, 25, 255, 221, 186, 142, 121, 95, 21, 8, 235, 211, 175, 138, 106 }, 82));
        if (UrlObfuscator.decode(new int[] { 17, 231, 205, 175, 190, 154 }, 99).equals(shortcutAction)) {
            webView.getSettings().setCacheMode(WebSettings.LOAD_NO_CACHE);
            webView.reload();
            webView.getSettings().setCacheMode(WebSettings.LOAD_CACHE_ELSE_NETWORK);
            return;
        }
        if (UrlObfuscator.decode(new int[] { 28, 252, 223, 180 }, 116).equals(shortcutAction)) {
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
                blobTmp = File.createTempFile(UrlObfuscator.decode(new int[] { 231, 200, 172, 128 }, 133), UrlObfuscator.decode(new int[] { 184, 193, 185, 131 }, 150), getCacheDir());
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
                Toast.makeText(MainActivity.this, UrlObfuscator.decode(new int[] { 227, 169, 146, 106, 79, 45, 0, 228, 191, 216, 188, 149, 119, 95, 61, 66, 183, 213, 186, 129, 127, 86, 113, 30, 224, 218, 237, 158, 110, 75, 45, 72, 243, 206, 172, 151, 35, 68, 40, 12, 26 }, 167), Toast.LENGTH_LONG).show();
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
                    sendIntent.setType(UrlObfuscator.decode(new int[] { 204, 178, 142, 97, 27, 35, 30, 240, 217, 161 }, 184));
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
                String safeTitle = (title == null || title.trim().isEmpty()) ? UrlObfuscator.decode(new int[] { 157, 135, 114, 84, 36, 22, 230, 204, 160 }, 201) : title;
                String safeBody = body == null ? "" : body;
                NotificationCompat.Builder notification = new NotificationCompat.Builder(MainActivity.this, UrlObfuscator.decode(new int[] { 190, 156, 126, 86, 35, 25, 224 }, 218))
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
            if (hex == null || !hex.matches(UrlObfuscator.decode(new int[] { 181, 41, 1, 19, 38, 171, 227, 165, 206, 100, 17, 109, 102, 35, 230, 138, 166, 134, 66, 121, 122, 48, 244, 153, 181, 194, 60, 9, 18, 21, 190, 209, 226, 206 }, 235))) return;
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

        // Stores the block layout the page-finished script measured (shapes
        // only -- no text or page content), so the next navigation to the
        // same page can show a skeleton of what it really looks like. Keeps
        // the 30 most recently seen pages.
        @JavascriptInterface
        public void saveSkeleton(String key, String json) {
            if (key == null || json == null || key.length() > 400 || json.length() > 16000) return;
            try {
                org.json.JSONArray test = new org.json.JSONArray(json);
                if (test.length() < 4) return;
                android.content.SharedPreferences sp = getSharedPreferences(UrlObfuscator.decode(new int[] { 143, 112, 95, 53, 29, 227, 217, 187, 171, 101, 3 }, 252), MODE_PRIVATE);
                org.json.JSONArray idx = new org.json.JSONArray(sp.getString(UrlObfuscator.decode(new int[] { 82, 115, 34, 14, 241 }, 269), "[]"));
                org.json.JSONArray next = new org.json.JSONArray();
                for (int i = 0; i < idx.length(); i++) {
                    String k = idx.optString(i);
                    if (!k.equals(key)) next.put(k);
                }
                next.put(key);
                android.content.SharedPreferences.Editor ed = sp.edit();
                while (next.length() > 30) {
                    ed.remove("p:" + next.optString(0));
                    next.remove(0);
                }
                ed.putString("p:" + key, json).putString(UrlObfuscator.decode(new int[] { 65, 98, 53, 31, 226 }, 286), next.toString()).apply();
            } catch (Exception ignored) {
            }
        }

        // Called from the page-finished script (and again on every focusin, i.e.
        // just before the keyboard opens) with the page's actual background
        // color. Keeps the window and WebView backgrounds identical to it, so
        // the area exposed while the keyboard animates in or out is the same
        // color as the page instead of a white or black flash.
        @JavascriptInterface
        public void applyPageBackground(String hex) {
            if (hex == null || !hex.matches(UrlObfuscator.decode(new int[] { 106, 112, 41, 208, 157, 137, 143, 32, 74, 123, 71, 176, 245, 188, 208, 120, 0 }, 52))) return;
            runOnUiThread(() -> {
                try {
                    int color = Color.parseColor(hex);
                    getWindow().setBackgroundDrawable(new ColorDrawable(color));
                    if (rootLayout != null) rootLayout.setBackgroundColor(color);
                    if (webView != null) webView.setBackgroundColor(color);
                    if (navSkeletonRef != null) navSkeletonRef.setBase(color);
                    if (navDotsRef != null) {
                        float pl = 0.299f * Color.red(color) + 0.587f * Color.green(color) + 0.114f * Color.blue(color);
                        navDotsRef.setColor(pl < 140f ? 0xFFF2F2EE : 0xFF1A1A1A);
                    }
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
            UrlObfuscator.decode(new int[] { 109, 2, 246, 204, 162, 148, 150, 113, 83, 116, 82, 225, 205, 170, 142, 109 }, 69) +
            UrlObfuscator.decode(new int[] { 32, 20, 230, 147, 162, 204, 116, 64, 45, 24, 225, 206, 164, 157, 38, 64, 35, 17, 193, 207, 167, 140, 101, 113, 74, 31, 5, 210, 222, 241, 223, 99, 87, 55, 36, 242, 220, 180, 156, 64, 94, 57, 5, 228, 196, 186, 207, 46, 29 }, 86) +
            UrlObfuscator.decode(new int[] { 14, 224, 141, 229, 147, 126, 93, 48, 113, 22, 244, 216, 191, 159, 119, 68, 43, 87, 229, 154, 176, 158, 112, 67, 60, 34, 228, 223, 191, 196, 106, 71, 41, 18, 228, 205, 173, 145, 41, 7, 86, 45, 80, 253, 216, 174, 144, 110, 82, 113, 92, 189, 193, 183, 133, 101, 93, 32, 86 }, 103) +
            UrlObfuscator.decode(new int[] { 14, 246, 196, 245, 149, 46, 86, 62, 19, 250, 195, 168, 130, 127, 4, 40, 11, 243, 207, 179, 129, 70, 78, 36, 13, 26, 240, 201, 231, 146, 124, 17, 121, 22, 234, 201, 181, 206, 47, 12, 52, 0, 237, 216, 161, 142, 100, 93, 102, 5, 233, 193, 189, 159, 126, 0, 48, 113, 29, 242, 210, 175, 155, 112, 86, 36, 94, 244, 157, 250, 128, 116, 68, 58, 28, 227, 151 }, 120) +
            UrlObfuscator.decode(new int[] { 255, 201, 181, 198, 113, 25, 107, 3, 175, 212, 222, 185, 179, 125, 86, 63, 5, 228, 144, 241, 220, 58, 71, 61, 61, 255, 216, 171, 159, 79, 74, 57, 12, 160, 142, 253 }, 137) +
            UrlObfuscator.decode(new int[] { 243, 223, 240, 131, 55, 8, 105, 84, 251, 223, 160, 154, 122, 10, 106, 77, 254, 136, 245, 218, 33, 81, 33, 27, 246, 192, 178, 186, 159, 58, 26, 125, 14, 184, 133, 234, 209, 102, 81, 63, 23, 242, 196, 232, 200, 43, 13, 42, 68, 224, 219, 132, 137, 107, 80, 38, 12, 245, 229, 219, 183, 137, 125, 89, 54, 28, 177, 197, 179, 129, 97, 65, 60, 74 }, 154) +
            UrlObfuscator.decode(new int[] { 221, 171, 155, 40, 85, 123, 4, 170, 196, 167, 149, 66, 112, 75, 51, 24, 242, 212, 190, 187, 123, 95, 48, 26, 231, 224, 180, 147, 123, 6, 100, 87 }, 171) +
            UrlObfuscator.decode(new int[] { 202, 186, 136, 57, 78, 33, 75, 226, 221, 189, 150, 126, 71, 97, 24, 228, 223, 190, 139, 101, 126, 46, 3, 242, 212, 172, 144, 117, 27, 73, 63, 15, 188, 205, 178, 196, 110, 65, 105, 3, 226, 157, 186, 148, 121, 72, 38, 25, 182, 220, 163, 135, 108, 72, 49, 75, 237, 205, 172, 132, 114, 87, 91, 52, 27, 243, 206, 226 }, 188) +
            UrlObfuscator.decode(new int[] { 187, 141, 121, 10, 57, 9, 227, 155, 244, 210, 56, 84, 32, 18, 95, 250, 216, 176, 143, 123, 4, 104, 76 }, 205) +
            UrlObfuscator.decode(new int[] { 183, 155, 52, 73, 116, 27, 247, 195, 162, 154, 121, 13, 36, 25, 189, 223, 175, 137, 37, 79, 47, 5, 252, 198, 251, 168, 101, 87, 42, 79, 237, 246, 208, 245, 142, 53, 88, 54, 12, 227, 217, 184, 217, 59, 68, 57, 93, 255, 207, 169, 197, 39, 103, 40, 28, 239, 136, 168, 133, 123, 10, 115, 84, 83, 232, 213, 246, 203, 52, 8, 96, 94, 191, 142 }, 222) +
            UrlObfuscator.decode(new int[] { 138, 98, 94, 41, 75, 227, 207, 224, 149, 40, 81, 43, 19, 190, 209, 161, 187, 215, 121, 89, 55, 14, 248, 133, 250, 187, 116, 64, 59, 92, 252, 217, 161, 198, 125, 77, 47, 71, 251, 134, 179, 137, 117, 8, 14, 3, 245, 200, 145, 179, 156, 100, 19, 107, 79, 180, 193, 190, 223, 36, 29, 99, 65, 185, 134, 245 }, 239) +
            UrlObfuscator.decode(new int[] { 105, 121, 22, 57, 25, 247, 206, 184, 209, 108, 64, 52, 6, 179, 193, 178, 205, 110, 0, 46, 0, 228, 217, 172, 155, 115, 14, 98, 74, 224, 205, 175, 148, 154, 112, 73, 112, 85, 238, 216, 186, 167, 119, 91, 49, 31, 190, 159, 191, 159, 122, 68, 35, 5, 249, 132, 171, 136, 104, 81, 33, 13, 246, 141, 155, 187, 159, 105, 93, 118, 9, 250, 202, 184, 154, 121, 25, 48, 29, 255, 196, 174, 135, 99, 73, 57, 55, 174, 129, 252 }, 256) +
            UrlObfuscator.decode(new int[] { 120, 86, 103, 29, 238, 138, 237, 153, 106, 6, 52, 5, 247, 203, 175, 142, 73, 69, 86, 57, 21, 232, 133, 169, 154, 54, 84, 58, 28, 241, 221, 166, 185, 117, 70, 41, 5, 248, 130, 177, 154, 107, 9, 53, 6, 246, 204, 174, 141, 66, 102, 22, 38, 8, 244, 202, 227, 156, 114, 90, 33, 21, 191, 208, 180, 152, 110, 88, 36, 3, 249, 144, 238, 137, 114, 82, 42, 67, 254, 139, 250, 157 }, 273) +
            UrlObfuscator.decode(new int[] { 71, 45, 19, 26, 229, 202, 181, 149, 126, 86, 47, 89, 229, 214, 166, 156, 126, 93, 18, 22, 166, 214, 184, 132, 122, 19, 44, 2, 234, 209, 165, 207, 96, 68, 40, 62, 8, 244, 211, 169, 192, 62, 89, 34, 2, 250, 147, 174, 219, 42, 77, 50 }, 290) +
            UrlObfuscator.decode(new int[] { 69, 52, 23, 225, 215, 187, 218, 116, 25, 52, 19, 240, 133, 227, 195, 50 }, 56), null);
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
            UrlObfuscator.decode(new int[] { 97, 14, 242, 200, 166, 144, 106, 77, 47, 72, 86, 229, 201, 174, 130, 97, 79, 57, 5, 182, 212, 233, 151, 125, 82, 37, 2, 235, 195, 184, 197, 107, 74, 60, 14, 240, 192, 129, 143, 103, 76, 37, 49, 10, 166, 213, 189, 210, 56, 89, 43, 10, 244, 137, 238, 207, 117, 95, 44, 27, 224, 201, 165, 158, 39, 74, 40, 2, 252, 141, 177, 135, 117, 85, 77, 48, 93, 177, 138, 225 }, 73) +
            UrlObfuscator.decode(new int[] { 44, 24, 234, 151, 162, 200, 60, 82, 124, 5, 241, 200, 128, 140, 97, 78, 54, 21, 175, 128, 239, 203, 112, 76, 14, 14, 247, 250, 204, 158, 157, 104, 95, 113, 81, 172 }, 90) +
            UrlObfuscator.decode(new int[] { 2, 236, 129, 188, 198, 59, 24, 99, 10, 236, 209, 181, 171, 217, 59, 26, 47, 91, 164, 133, 240, 130, 112, 76, 39, 19, 227, 213, 174, 201, 43, 10, 63, 75, 180, 149, 224, 149, 96, 72, 38, 1, 245, 135, 153, 248, 220, 125, 21, 51, 10, 219, 216, 184, 129, 113, 93, 38, 52, 244, 198, 186, 140, 110, 71, 47, 64, 250, 194, 178, 144, 118, 77, 98, 76, 177, 164 }, 107) +
            UrlObfuscator.decode(new int[] { 10, 250, 200, 249, 138, 42, 87, 123, 19, 246, 198, 147, 159, 122, 64, 41, 5, 229, 205, 138, 132, 110, 67, 43, 16, 209, 199, 162, 148, 215, 55, 6, 46, 30, 238, 204, 170, 153, 54, 120, 53, 7, 250, 159, 162, 128, 123, 67, 40, 67, 248, 135, 170, 136, 114, 81, 43, 14, 168, 144, 240, 239, 215, 50, 13, 107, 74, 162, 197, 180, 151, 97, 87, 59, 90, 244, 153, 180, 156, 104, 88, 62, 24, 231, 136, 234, 215, 62, 89, 62, 75, 169, 137, 132 }, 124),
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
    private int statusBarHeightFallback() {
        int id = getResources().getIdentifier(UrlObfuscator.decode(new int[] { 254, 216, 170, 158, 124, 91, 24, 4, 228, 214, 156, 138, 100, 73, 88, 54, 9 }, 141), UrlObfuscator.decode(new int[] { 250, 212, 177, 158, 116 }, 158), UrlObfuscator.decode(new int[] { 206, 160, 137, 126, 68, 35, 13 }, 175));
        return id > 0 ? getResources().getDimensionPixelSize(id) : 0;
    }

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
        if (t.isEmpty() || UrlObfuscator.decode(new int[] { 174, 170, 146, 113 }, 192).equals(t)) return null;
        try {
            if (t.startsWith("#")) return Color.parseColor(t) | 0xFF000000;
            if (!t.startsWith(UrlObfuscator.decode(new int[] { 163, 151, 109 }, 209))) return null;
            String inner = t.substring(t.indexOf('(') + 1);
            String[] parts = inner.split(UrlObfuscator.decode(new int[] { 185, 95, 16, 18, 103, 83, 193, 144 }, 226));
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
        int scrim = Color.parseColor(UrlObfuscator.decode(new int[] { 208, 35, 5, 98, 91, 187, 238 }, 243));
        try {
            if (topScrim != null && topScrim.getBackground() instanceof ColorDrawable) {
                scrim = ((ColorDrawable) topScrim.getBackground()).getColor() | 0xFF000000;
            }
        } catch (Exception ignored) {}
        Integer bgC = o == null ? null : parseCssColor(o.optString("bg", null));
        Integer thC = o == null ? null : parseCssColor(o.optString(UrlObfuscator.decode(new int[] { 112, 75, 39, 12, 229 }, 260), null));
        Integer btC = o == null ? null : parseCssColor(o.optString(UrlObfuscator.decode(new int[] { 119, 64, 61 }, 277), null));
        double btnR = o == null ? -1 : o.optDouble(UrlObfuscator.decode(new int[] { 68, 49, 10, 209 }, 294), -1);

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
                accent = dark ? Color.parseColor(UrlObfuscator.decode(new int[] { 31, 29, 72, 223, 138, 145, 193 }, 60)) : Color.parseColor(UrlObfuscator.decode(new int[] { 110, 93, 188, 155, 254, 217, 68 }, 77));
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
        p.title = cardDark ? Color.WHITE : Color.parseColor(UrlObfuscator.decode(new int[] { 125, 76, 168, 138, 238, 200, 89 }, 94));
        p.sub = mixColor(p.title, p.cardTop, 0.35f);
        p.border = mixColor(accent, p.cardTop, 0.45f);
        p.accentTop = mixColor(accent, Color.WHITE, 0.12f);
        p.accentBottom = mixColor(accent, Color.BLACK, 0.12f);
        p.accentText = lumOf(accent) < 0.55 ? Color.WHITE : Color.parseColor(UrlObfuscator.decode(new int[] { 76, 191, 153, 253, 223, 59, 104 }, 111));
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
                cancel.setText(UrlObfuscator.decode(new int[] { 195, 222, 240, 158, 185, 87 }, 128));
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
            boolean chat = h.equals(UrlObfuscator.decode(new int[] { 229, 158, 162, 139 }, 145)) || h.endsWith(UrlObfuscator.decode(new int[] { 140, 181, 206, 146, 123 }, 162))
                || h.equals(UrlObfuscator.decode(new int[] { 199, 183, 157, 117, 72, 60, 12, 225, 133, 167, 140 }, 179)) || h.equals(UrlObfuscator.decode(new int[] { 176, 134, 110, 68, 39, 45, 31, 240, 146, 191, 149, 126 }, 196))
                || h.equals(UrlObfuscator.decode(new int[] { 162, 149, 61, 95, 52 }, 213)) || h.equals(UrlObfuscator.decode(new int[] { 135, 117, 77, 109, 21, 233, 193, 203, 173, 156, 108, 75, 116, 26, 247, 218 }, 230));
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
        if (scheme.equals(UrlObfuscator.decode(new int[] { 159, 98, 65, 36 }, 247)) || scheme.equals(UrlObfuscator.decode(new int[] { 96, 83, 50, 21, 247 }, 264)) || scheme.equals(UrlObfuscator.decode(new int[] { 127, 81, 59, 19 }, 281))
                || scheme.equals(UrlObfuscator.decode(new int[] { 78, 44, 2, 249, 223 }, 47)) || scheme.equals(UrlObfuscator.decode(new int[] { 36, 62, 10, 252 }, 64)) || scheme.equals(UrlObfuscator.decode(new int[] { 51, 28, 224, 204 }, 81))
                || scheme.equals(UrlObfuscator.decode(new int[] { 8, 224, 214, 222, 173, 158, 110, 82, 42, 13 }, 98)) || scheme.equals(UrlObfuscator.decode(new int[] { 16, 253, 223, 164, 138, 96, 89 }, 115))) {
            return false;
        }
        try {
            Intent intent;
            if (scheme.equals(UrlObfuscator.decode(new int[] { 237, 205, 182, 132, 110, 107 }, 132))) {
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
                if (scheme.equals(UrlObfuscator.decode(new int[] { 252, 218, 167, 151, 127, 68 }, 149))) {
                    Intent parsed = Intent.parseUri(uri.toString(), Intent.URI_INTENT_SCHEME);
                    String fallback = parsed.getStringExtra(UrlObfuscator.decode(new int[] { 196, 183, 139, 116, 81, 36, 18, 32, 248, 220, 176, 151, 120, 88, 59, 28, 201, 192, 166, 159 }, 166));
                    if (fallback != null && (fallback.startsWith(UrlObfuscator.decode(new int[] { 223, 162, 129, 100, 64, 104, 94, 191 }, 183)) || fallback.startsWith(UrlObfuscator.decode(new int[] { 160, 147, 114, 85, 126, 76, 173 }, 200)))) {
                        view.loadUrl(fallback);
                        return true;
                    }
                }
            } catch (Exception ignored) {}
            Toast.makeText(this, UrlObfuscator.decode(new int[] { 151, 151, 55, 87, 37, 4, 179, 212, 190, 133, 97, 74, 109, 24, 228, 138, 166, 152, 98, 72, 101, 16, 235, 203, 178, 192, 147, 119, 83, 55 }, 217), Toast.LENGTH_SHORT).show();
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
        String name = raw == null ? "" : raw.replaceAll("[" + (char) 92 + (char) 92 + UrlObfuscator.decode(new int[] { 197, 51, 2, 120 }, 234) + (char) 34 + UrlObfuscator.decode(new int[] { 199, 36, 69, 5 }, 251), "_").trim();
        if (name.length() > 120) name = name.substring(name.length() - 120);
        if (name.isEmpty() || name.equals(".") || name.equals("..")) name = UrlObfuscator.decode(new int[] { 104, 68, 61, 7, 228, 200, 167, 129 }, 268);
        if (name.lastIndexOf('.') <= 0 && mime != null && !mime.isEmpty()) {
            String ext = android.webkit.MimeTypeMap.getSingleton().getExtensionFromMimeType(mime);
            if (ext != null) name = name + "." + ext;
        }
        return name;
    }

    private void handleInlineDownload(String url, String contentDisposition, String mimeType) {
        try {
            String named = downloadNames.get(downloadKey(url));
            if (url.startsWith(UrlObfuscator.decode(new int[] { 121, 93, 47, 27, 163 }, 285))) {
                int comma = url.indexOf(',');
                if (comma < 0) throw new IllegalArgumentException(UrlObfuscator.decode(new int[] { 81, 51, 21, 176, 203, 175, 153, 109, 11, 31, 59, 196 }, 51));
                String meta = url.substring(5, comma);
                String payload = url.substring(comma + 1);
                String mime = meta.split(";")[0];
                if (mime.isEmpty()) mime = (mimeType != null && !mimeType.isEmpty()) ? mimeType : UrlObfuscator.decode(new int[] { 48, 6, 250, 213, 239, 175, 146, 124, 85, 53 }, 68);
                byte[] bytes;
                if (meta.contains(UrlObfuscator.decode(new int[] { 110, 22, 242, 193, 180, 198, 59 }, 85))) {
                    if (payload.indexOf('%') >= 0) payload = Uri.decode(payload);
                    bytes = android.util.Base64.decode(payload, android.util.Base64.DEFAULT);
                } else {
                    bytes = Uri.decode(payload).getBytes(UrlObfuscator.decode(new int[] { 51, 209, 226, 238, 218 }, 102));
                }
                String name = cleanDownloadName(named != null ? named : URLUtil.guessFileName(url, contentDisposition, mime), mime);
                File tmp = File.createTempFile("dl", UrlObfuscator.decode(new int[] { 89, 226, 216, 164 }, 119), getCacheDir());
                java.io.FileOutputStream fos = new java.io.FileOutputStream(tmp);
                try { fos.write(bytes); } finally { fos.close(); }
                finishInlineSave(tmp, name, mime);
                return;
            }
            // blob: -- read it inside the page and stream it back in chunks.
            String mime = (mimeType != null && !mimeType.isEmpty()) ? mimeType : "";
            String name = cleanDownloadName(named != null ? named : URLUtil.guessFileName(url, contentDisposition, mime), mime);
            Toast.makeText(this, UrlObfuscator.decode(new int[] { 204, 200, 177, 139, 104, 76, 35, 5, 233, 241, 217, 253 }, 136) + name + "\u2026", Toast.LENGTH_SHORT).show();
            final String js = UrlObfuscator.decode(new int[] { 177, 222, 162, 152, 118, 64, 58, 29, 255, 152, 186, 194, 99, 0, 38, 67, 242, 220, 181, 159, 126 }, 153)
                + UrlObfuscator.decode(new int[] { 220, 168, 154, 39, 94, 120, 10, 230, 213, 225, 184, 178, 82, 117, 40, 15, 234, 235, 189, 134, 99, 80, 39, 7, 186, 152, 235, 151, 32, 66, 60, 14, 228, 129, 239, 160, 67, 113, 99, 79, 247, 141, 180, 173, 139, 120, 21, 96, 2, 183, 202, 178, 133, 101, 91, 61, 1, 244, 228, 182, 158, 104, 17, 108, 8, 229, 199, 165, 193, 62 }, 170)
                + UrlObfuscator.decode(new int[] { 195, 244, 150, 118, 82, 36, 7, 251, 193, 239, 151, 101, 65, 45, 25, 229, 196, 164, 193, 33, 92, 7, 11, 224, 209, 173, 136, 100, 93, 76, 52, 24, 252, 223, 247, 154, 123, 89, 55, 39, 242, 196, 180, 182, 110, 71, 33, 9, 239, 130, 224, 211, 122, 29 }, 187)
                + UrlObfuscator.decode(new int[] { 180, 197, 101, 71, 36, 8, 231, 193, 249, 133, 119, 79, 35, 43, 23, 242, 210, 243, 211, 98, 76, 37, 15, 238, 194, 178, 128, 49, 82, 114, 22, 163, 222, 174, 153, 121, 71, 41, 21, 224, 136, 128, 170, 60, 23, 7, 104, 73, 175, 137, 246, 150, 126, 81, 107, 69, 175 }, 204)
                + UrlObfuscator.decode(new int[] { 156, 146, 127, 72, 54, 17, 243, 244, 167, 157, 119, 85, 52, 94, 237, 194, 162, 142, 88, 75, 63, 13, 197, 195, 162, 141, 109, 10, 47, 76, 18, 226, 193, 190, 213, 110, 64, 40, 18, 234, 201, 243, 212, 59, 10 }, 221)
                + UrlObfuscator.decode(new int[] { 198, 107, 89, 37, 9, 253, 193, 168, 136, 37, 74, 38, 26, 245, 136, 150, 165, 148, 122, 19, 53, 31, 254, 137, 235, 151, 58, 64, 59, 11, 245, 134, 181, 172, 98, 79, 56, 6, 225, 195, 132, 151, 109, 71, 37, 4, 174, 253, 210, 178, 158, 72, 91, 47, 29, 210, 216, 177, 220, 58, 9, 35, 21, 251, 219, 191, 130, 48, 87 }, 238)
                + UrlObfuscator.decode(new int[] { 137, 127, 79, 124, 9, 167, 215, 189, 128, 54, 115, 61, 31, 247, 227, 181, 142, 106, 72, 62, 67, 163, 146 }, 255)
                + UrlObfuscator.decode(new int[] { 98, 1, 33, 3, 224, 196, 171, 141, 53, 65, 51, 11, 231, 215, 171, 142, 110, 55, 23, 38, 10, 250, 200, 249, 139, 42, 101, 33, 6, 250, 220, 182, 216, 125, 0, 63, 9, 248, 223, 165, 156, 46, 29, 4, 10, 231, 208, 174, 137, 155, 92, 79, 53, 31, 253, 220, 246, 149, 122, 90, 54, 32, 243, 199, 181, 172, 102, 88, 34, 0, 162, 218, 230, 148, 115, 71, 55, 23, 240, 200, 174, 184, 214, 110, 18, 50, 20, 253, 221, 175, 185, 115, 28, 116, 94, 182, 153, 228, 223, 36, 5, 112, 5, 239, 206, 236, 219, 70, 108, 120, 12, 228, 216, 203, 246, 212, 39, 70, 97 }, 272)
                + UrlObfuscator.decode(new int[] { 83, 110, 48, 16, 248, 206, 169, 149, 107, 5, 49, 3, 251, 215, 167, 155, 126, 94, 103, 71, 246, 237, 165, 142, 123, 71, 46, 2, 199, 214, 170, 134, 102, 69, 17, 60, 17, 243, 217, 137, 152, 110, 82, 16, 20, 253, 223, 183, 149, 56, 6, 117, 16, 183 }, 289)
                + UrlObfuscator.decode(new int[] { 69, 120, 7, 241, 210, 182, 176, 99, 107, 47, 25, 237, 254, 152, 165, 32, 69, 104, 22, 232, 202, 161, 132, 40, 112, 88, 59, 80, 244, 220, 191, 211, 84, 126, 124, 93, 168, 207, 248, 216, 38, 21 }, 55)
                + UrlObfuscator.decode(new int[] { 53, 4, 231, 209, 167, 139, 42, 68, 105, 36, 63, 243, 216, 169, 149, 112, 92, 21, 4, 252, 208, 180, 151, 63, 82, 35, 1, 239, 255, 170, 156, 108, 110, 38, 15, 233, 193, 167, 202, 40, 27, 66, 35, 70 }, 72)
                + UrlObfuscator.decode(new int[] { 33, 86, 228, 211, 187, 144, 59, 27, 106 }, 89)
                + UrlObfuscator.decode(new int[] { 23, 234, 201, 179, 133, 109, 12, 38, 75, 250, 212, 205, 167, 134, 93, 85, 62, 11, 247, 222, 178, 183, 102, 90, 54, 22, 245, 129, 172, 129, 99, 73, 25, 8, 254, 194, 128, 132, 109, 79, 39, 5, 168, 182, 133, 160, 159, 122, 78, 58, 16, 191, 233, 252, 143, 110, 79, 44, 89, 167 }, 106)
                + org.json.JSONObject.quote(url) + "," + org.json.JSONObject.quote(name) + "," + org.json.JSONObject.quote(mime) + ");";
            webView.evaluateJavascript(js, null);
        } catch (Exception e) {
            Toast.makeText(this, UrlObfuscator.decode(new int[] { 63, 245, 206, 182, 155, 121, 84, 48, 83, 244, 208, 185, 131, 107, 73, 118, 75, 233, 198, 189, 139, 98, 5, 42, 12, 246, 129, 178, 186, 159, 121, 28, 47, 18, 240, 203, 247, 144, 124, 88, 54 }, 123), Toast.LENGTH_LONG).show();
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
                cv.put(android.provider.MediaStore.MediaColumns.MIME_TYPE, (mime == null || mime.isEmpty()) ? UrlObfuscator.decode(new int[] { 237, 219, 186, 133, 97, 68, 39, 17, 237, 204, 172, 206, 111, 124, 74, 56, 8, 182, 201, 173, 138, 114, 87, 56 }, 140) : mime);
                cv.put(android.provider.MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + UrlObfuscator.decode(new int[] { 178, 232, 180, 143, 107, 89, 37, 19, 251, 213 }, 157));
                cv.put(android.provider.MediaStore.MediaColumns.IS_PENDING, 1);
                android.content.ContentResolver cr = getContentResolver();
                Uri dest = cr.insert(android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI, cv);
                if (dest == null) throw new java.io.IOException(UrlObfuscator.decode(new int[] { 227, 168, 136, 98, 75, 26, 28, 232, 212, 160, 196, 106, 76, 50, 5, 13, 234, 157, 186, 154, 115, 85, 61, 19 }, 174));
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
                File dir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), UrlObfuscator.decode(new int[] { 235, 177, 136, 110, 90, 40, 28, 246, 214 }, 191));
                if (!dir.exists() && !dir.mkdirs()) throw new java.io.IOException(UrlObfuscator.decode(new int[] { 189, 132, 106, 68, 62, 24, 170, 207, 169, 142, 106, 64, 32 }, 208));
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
                ? shown + " downloaded \u2014 find it in your Tourarena folder"
                : UrlObfuscator.decode(new int[] { 165, 111, 104, 80, 49, 19, 250, 222, 249, 158, 118, 95, 57, 17, 247, 136, 241, 147, 96, 91, 33, 8, 171, 196, 166, 156, 39, 85, 36, 18, 230, 130 }, 225) + shown, Toast.LENGTH_LONG).show();
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
    // its own "Tourarena" subfolder in there (DownloadManager
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
        if (url != null && (url.startsWith(UrlObfuscator.decode(new int[] { 144, 125, 95, 45, 84 }, 242)) || url.startsWith(UrlObfuscator.decode(new int[] { 103, 67, 53, 1, 69 }, 259)))) {
            handleInlineDownload(url, contentDisposition, mimeType);
            return;
        }

        final String filename = URLUtil.guessFileName(url, contentDisposition, mimeType);
        final String cookie = CookieManager.getInstance().getCookie(url);

        try {
            DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
            request.addRequestHeader(UrlObfuscator.decode(new int[] { 65, 64, 55, 3, 189, 238, 169, 136, 98, 95 }, 276), userAgent);
            if (cookie != null) request.addRequestHeader(UrlObfuscator.decode(new int[] { 102, 43, 12, 233, 200, 165 }, 293), cookie);
            request.setTitle(filename);
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
            request.setDestinationInExternalPublicDir(
                Environment.DIRECTORY_DOWNLOADS, UrlObfuscator.decode(new int[] { 111, 53, 12, 234, 214, 164, 144, 122, 82, 125 }, 59) + filename);
            request.setAllowedOverMetered(true);
            request.setAllowedOverRoaming(true);
            request.setVisibleInDownloadsUi(true);

            DownloadManager downloadManager = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
            pendingDownloadId = downloadManager.enqueue(request);
            startDownloadProgressPolling(pendingDownloadId);
            Toast.makeText(this, UrlObfuscator.decode(new int[] { 8, 4, 253, 199, 164, 136, 103, 65, 45, 13, 229, 129 }, 76) + filename + "…", Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Toast.makeText(this, UrlObfuscator.decode(new int[] { 25, 19, 236, 212, 181, 151, 118, 82, 117, 18, 242, 219, 189, 149, 107, 14, 96, 65, 171, 201, 161, 141, 100, 77, 101, 29, 236, 215, 179, 192, 156, 113, 83, 50, 30, 249, 205, 177, 152, 120, 21, 53, 29, 246, 145, 164, 157, 119, 13, 45, 12, 235, 192, 166 }, 93), Toast.LENGTH_LONG).show();
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
            String title = (titleIdx >= 0 && cursor.getString(titleIdx) != null) ? cursor.getString(titleIdx) : UrlObfuscator.decode(new int[] { 40, 228, 192, 174 }, 110);

            if (status == DownloadManager.STATUS_SUCCESSFUL) {
                String localUriStr = uriIdx >= 0 ? cursor.getString(uriIdx) : null;
                String folderName = UrlObfuscator.decode(new int[] { 59, 241, 202, 178, 151, 117, 88, 60, 4 }, 127);
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
                Toast.makeText(this, title + UrlObfuscator.decode(new int[] { 176, 203, 161, 154, 98, 71, 37, 8, 236, 194, 162, 197, 8208, 3, 36, 8, 238, 251, 158, 180, 136, 59, 83, 55, 88, 238, 217, 160, 134, 51 }, 144) + folderName + UrlObfuscator.decode(new int[] { 129, 166, 176, 146, 121, 89, 41 }, 161), Toast.LENGTH_LONG).show();
                notifyDownloadResult(true);

                // If what just finished downloading is itself an installable
                // Android package, go straight to the system installer
                // instead of leaving the user to dig it out of the Downloads
                // folder and tap it themselves. Gated strictly on the file
                // actually being a .apk -- every other kind of download this
                // wrapper handles (images, PDFs, whatever the wrapped site
                // links to) still behaves exactly as before, since "install"
                // wouldn't mean anything for those.
                if (localPath != null && title != null && title.toLowerCase().endsWith(UrlObfuscator.decode(new int[] { 156, 176, 128, 100 }, 178))) {
                    File apkFile = new File(localPath);
                    if (apkFile.exists()) {
                        try {
                            Uri contentUri = FileProvider.getUriForFile(
                                this, getPackageName() + UrlObfuscator.decode(new int[] { 237, 132, 104, 76, 90, 46, 15, 243, 205, 179, 157, 125, 69 }, 195), apkFile);
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
                Toast.makeText(this, UrlObfuscator.decode(new int[] { 144, 156, 101, 95, 60, 0, 239, 201, 236, 141, 107, 64, 36, 2, 226, 159, 228 }, 212) + title + UrlObfuscator.decode(new int[] { 197, 44, 70, 48, 19, 239, 237, 158 }, 229) + reason + ")", Toast.LENGTH_LONG).show();
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
                    UrlObfuscator.decode(new int[] { 130, 103, 77, 40, 27, 247, 152, 184, 135, 99, 72, 36, 29, 167, 247, 152, 137, 107, 106, 34, 22, 232, 214, 218, 154, 146, 107, 85, 54, 22, 249, 211, 149, 154, 121, 67, 62, 20, 228, 202, 231, 150, 123, 66, 36, 13, 231, 208, 232, 186, 91, 76, 44, 47, 225, 235, 215, 171, 153, 95, 85, 46, 22, 251, 217, 180, 144, 80, 93, 60, 0, 227, 203, 185, 137, 35 }, 246) + success + UrlObfuscator.decode(new int[] { 46, 29, 56, 25, 224, 195, 181, 131, 151, 54, 88, 117, 0, 231 }, 263),
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
                UrlObfuscator.decode(new int[] { 108, 69, 47, 14, 253, 213, 250, 134, 121, 65, 42, 2, 251, 133, 149, 182, 103, 73, 8, 4, 240, 202, 180, 132, 68, 112, 73, 51, 16, 244, 219, 189, 168, 101, 89, 50, 6, 246, 193, 162, 217, 116, 89, 36, 2, 239, 197, 190, 198, 88, 121, 42, 10, 205, 195, 181, 137, 137, 123, 121, 51, 12, 244, 213, 183, 150, 114, 101, 38, 28, 245, 195, 181, 156, 125, 5 }, 280) + done + "," + total + "," + status + UrlObfuscator.decode(new int[] { 0, 115, 26, 251, 198, 165, 151, 97, 73, 104, 58, 87, 230, 193 }, 297),
                null);
        } catch (Exception ignored) {
        }
    }

    private void notifyDownloadCancelled() {
        try {
            webView.evaluateJavascript(
                UrlObfuscator.decode(new int[] { 75, 44, 4, 231, 210, 188, 209, 111, 94, 56, 17, 251, 196, 252, 174, 79, 64, 32, 35, 237, 223, 163, 159, 109, 99, 41, 18, 234, 207, 173, 128, 100, 92, 81, 48, 12, 247, 223, 173, 157, 62, 77, 34, 29, 253, 214, 190, 135, 33, 113, 18, 3, 229, 228, 168, 156, 110, 80, 32, 32, 236, 213, 175, 140, 144, 127, 89, 31, 20, 247, 201, 180, 146, 98, 80, 124, 21, 243, 221, 163, 138, 34, 10, 47, 10, 228, 202, 173, 139, 106, 64, 32, 68, 171, 154, 189, 162, 157, 124, 72, 56, 18, 177, 221, 254, 141, 104 }, 63),
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
            Toast.makeText(this, UrlObfuscator.decode(new int[] { 17, 3, 226, 194, 187, 203, 99, 71, 59, 19, 231, 201, 168, 144, 34, 71, 50, 48, 19, 189, 200, 179, 147, 106, 24, 54, 6, 229, 152, 243, 134, 121, 85, 33, 78, 228, 216, 236, 134, 101, 8, 36, 9, 235, 208, 170, 140, 116, 69, 31, 63, 8, 232, 212, 183, 152, 108, 94, 53, 20, 248, 223, 171 }, 80), Toast.LENGTH_LONG).show();
            try {
                Intent settingsIntent = new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                    Uri.parse(UrlObfuscator.decode(new int[] { 17, 225, 252, 213, 188, 155, 126, 0 }, 97) + getPackageName()));
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
        installIntent.setDataAndType(contentUri, UrlObfuscator.decode(new int[] { 19, 225, 192, 163, 135, 110, 77, 63, 3, 230, 198, 232, 144, 107, 64, 109, 3, 239, 196, 205, 177, 148, 120, 21, 42, 24, 251, 220, 183, 146, 113, 30, 51, 3, 243, 199, 167, 155, 105 }, 114));
        installIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_GRANT_READ_URI_PERMISSION);
        try {
            startActivity(installIntent);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, UrlObfuscator.decode(new int[] { 205, 205, 225, 137, 145, 109, 73, 61, 23, 246, 220, 170, 215, 112, 90, 33, 29, 246, 145, 253, 194, 46, 66, 60, 14, 228, 137, 188, 143, 99, 5, 34, 10, 238, 196, 224, 185, 140, 114, 81, 123, 3, 246, 205, 165, 214, 81, 91, 36, 28, 253, 223, 174, 138, 126, 12, 45, 5, 229, 204, 162, 148, 37, 77, 45, 17, 245, 197, 222, 186 }, 131), Toast.LENGTH_LONG).show();
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
                Toast.makeText(this, UrlObfuscator.decode(new int[] { 199, 199, 189, 131, 113, 72, 43, 77, 252, 206, 184, 132, 97, 84, 53, 12, 235, 205, 226, 136, 115, 63, 80, 56, 25, 255, 223, 189, 216, 99, 89, 117, 7, 242, 196, 180, 208, 123, 70, 40, 76, 239, 197, 190, 134, 107, 73, 36, 0 }, 148), Toast.LENGTH_LONG).show();
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
                Toast.makeText(this, UrlObfuscator.decode(new int[] { 230, 165, 142, 103, 83, 33, 112, 19, 244, 223, 169, 149, 105, 80, 56, 24, 240, 148, 163, 151, 99, 93, 38, 29, 254, 197, 164, 132, 41, 65, 52, 70, 235, 193, 166, 134, 100, 68, 31, 56, 18, 238, 155, 174, 145, 113, 68 }, 165), Toast.LENGTH_LONG).show();
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
                Toast.makeText(this, UrlObfuscator.decode(new int[] { 250, 186, 151, 114, 70, 56, 31, 225, 142, 189, 137, 121, 71, 32, 27, 244, 207, 170, 138, 35, 75, 50, 64, 17, 251, 216, 184, 158, 126, 25, 62, 24, 228, 149, 160, 155, 123, 66 }, 182), Toast.LENGTH_LONG).show();
            }
        } else if (requestCode == NOTIFICATION_PERMISSION_REQUEST_CODE) {
            if (!(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                Toast.makeText(this, UrlObfuscator.decode(new int[] { 137, 137, 113, 77, 37, 11, 226, 193, 203, 183, 146, 114, 72, 122, 24, 234, 210, 246, 154, 114, 85, 114, 23, 255, 221, 238, 153, 100, 66, 57, 73, 233, 215, 182, 197, 41, 14, 98, 21, 245, 237, 208, 253, 136, 115, 95, 52, 88, 248, 216, 245, 157, 125, 18, 2, 21, 251, 218, 164, 130, 108, 89, 105, 9, 233, 223, 229, 144, 106, 79, 36 }, 199), Toast.LENGTH_LONG).show();
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
            UrlObfuscator.decode(new int[] { 188, 146, 112, 84, 33, 31, 230 }, 216), UrlObfuscator.decode(new int[] { 174, 109, 73, 35, 23, 229, 207 }, 233), NotificationManager.IMPORTANCE_DEFAULT);
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
                Toast.makeText(this, UrlObfuscator.decode(new int[] { 179, 119, 75, 35, 23, 249, 216, 243, 130, 116, 66, 34, 7, 254, 223, 162, 133, 103, 8, 48, 7, 246, 202, 228, 150, 33, 71, 77, 63, 19, 232, 222, 190, 217, 53, 26, 118, 26, 228, 214, 188, 209, 100, 71, 43, 77, 234, 194, 166, 140, 40, 65, 52, 10, 233, 131, 187, 142, 117, 109, 30, 25, 19, 236, 212, 181, 151, 118, 82, 38, 84, 245, 221, 189, 148, 106, 92, 109, 24, 228, 138, 160, 134, 116, 82, 36, 8, 239, 130, 172, 129, 145, 107, 92, 48, 23, 227 }, 250), Toast.LENGTH_LONG).show();
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
        try { CookieManager.getInstance().flush(); } catch (Exception ignored) {}
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
