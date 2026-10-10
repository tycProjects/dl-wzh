package com.generated.konyi;

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
        // Native launch: the page waits fully hidden under the launch
        // screen, then is switched on at full size and the launch screen
        // fades away over it. No scale, no bounce.
        webView.setAlpha(0f);
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

        final SplashController loading = new NativeLaunchView(this, Color.parseColor(UrlObfuscator.decode(new int[] { 10, 120, 87, 182, 149, 244, 211 }, 297)), Color.parseColor(UrlObfuscator.decode(new int[] { 28, 24, 79, 218, 137, 159, 188 }, 63)), 1);
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
        final String offlineScrimColor = offlineIsNightMode ? UrlObfuscator.decode(new int[] { 115, 86, 183, 157, 252, 219, 58, 25, 120 }, 80) : UrlObfuscator.decode(new int[] { 66, 182, 169, 142, 237, 204, 43, 10, 105 }, 97);
        // Slightly translucent (not fully opaque) card/button fills, so a
        // hint of the dimmed scrim behind still shows through -- a cheap
        // stand-in for a true frosted-glass blur, which would need
        // RenderEffect (API 31+) and a snapshot of whatever's behind the
        // card to do for real.
        final String offlineCardBg = offlineIsNightMode ? UrlObfuscator.decode(new int[] { 81, 212, 136, 253, 173, 63, 105, 120, 90 }, 114) : UrlObfuscator.decode(new int[] { 160, 224, 249, 166, 205, 88, 15, 26, 73 }, 131);
        final String offlineTitleColor = offlineIsNightMode ? UrlObfuscator.decode(new int[] { 183, 245, 148, 183, 86, 105, 8 }, 148) : UrlObfuscator.decode(new int[] { 134, 244, 211, 50, 17, 112, 111 }, 165);
        final String offlineSubtitleColor = offlineIsNightMode ? UrlObfuscator.decode(new int[] { 149, 151, 204, 81, 10, 19, 52 }, 182) : UrlObfuscator.decode(new int[] { 228, 211, 71, 17, 1, 87, 196 }, 199);
        final String offlineButtonBg = offlineIsNightMode ? UrlObfuscator.decode(new int[] { 251, 207, 85, 1, 108, 71, 170, 133, 145 }, 216) : UrlObfuscator.decode(new int[] { 202, 48, 100, 2, 83, 192, 149, 134, 160 }, 233);
        final float offlineDensity = getResources().getDisplayMetrics().density;

        // Outer full-screen scrim so the card reads as a dialog sitting on
        // top of the app, matching how a native "no connection" alert
        // dims everything behind it.
        final FrameLayout errorView = new FrameLayout(this);
        errorView.setBackgroundColor(Color.parseColor(UrlObfuscator.decode(new int[] { 217, 41, 121, 103, 55, 165, 246 }, 250)));
        errorView.setVisibility(View.GONE);

        // The error screen is the branded HTML page in ERROR_PAGE_HTML,
        // rendered in its own small WebView that fills errorView. Its
        // Retry / Back buttons call back into native code through the
        // AndroidError interface (registered below, once doRetry exists).
        // Navigation inside it is blocked so it can only ever show that page.
        final WebView errorWeb = new WebView(this);
        errorWeb.setBackgroundColor(Color.parseColor(UrlObfuscator.decode(new int[] { 40, 26, 8, 88, 198, 150, 135 }, 267)));
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
        navOverlay.setBackgroundColor(Color.parseColor(UrlObfuscator.decode(new int[] { 63, 11, 106, 73, 168, 135, 230 }, 284)));
        navOverlay.setVisibility(View.GONE);
        final SkeletonView navSkeleton = new SkeletonView(this, Color.parseColor(UrlObfuscator.decode(new int[] { 17, 97, 64, 191, 158, 253, 220 }, 50)));
        navSkeleton.setTarget(webView);
        navSkeletonRef = navSkeleton;
        navOverlay.addView(navSkeleton, new FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
        final BouncingDotsView navDots = new BouncingDotsView(this, Color.parseColor(UrlObfuscator.decode(new int[] { 96, 36, 179, 230, 141, 155, 184 }, 67)));
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
                UrlObfuscator.decode(new int[] { 124, 21, 231, 223, 179, 155, 103, 66, 34, 67, 163, 210, 188, 149, 127, 94, 50, 2, 240, 129, 162, 226, 154, 114, 95, 46, 23, 252, 214, 163, 216, 119, 91, 55, 11, 170, 217, 169, 198, 44, 78, 98, 24, 236, 220, 178, 148, 107, 4, 55, 16, 244, 197, 132 }, 84)
                + UrlObfuscator.decode(new int[] { 12, 226, 139, 234, 131, 46, 118, 80, 51, 25, 233, 238, 188, 128, 99, 74, 41, 83, 180, 155, 255, 132, 125, 71, 32, 68, 162, 132, 165, 141, 105, 65, 49, 12, 189, 146, 232, 146, 154, 106, 72, 46, 21, 186, 223, 185, 155, 101, 80, 111 }, 101)
                + UrlObfuscator.decode(new int[] { 4, 240, 192, 166, 128, 127, 16, 45, 64, 252, 217, 174, 152, 112, 123, 34, 10, 224, 199, 183, 141, 115, 97, 83, 50, 85, 187, 210, 183, 158, 52, 65, 63, 17, 241, 220, 254, 146, 113, 65, 56, 12, 255, 135, 185, 159, 111, 11, 47, 3, 246, 194, 175, 132, 44, 118, 80, 45, 9, 239, 150, 187, 141, 99, 66, 58, 26, 180, 155, 255, 156, 106, 64, 42, 24, 227, 151, 244, 213, 55, 29, 56 }, 118)
                + UrlObfuscator.decode(new int[] { 228, 199, 177, 135, 107, 10, 36, 73, 4, 236, 216, 168, 142, 104, 87, 120, 17, 247, 217, 167, 150, 41, 76, 45, 70, 166, 132 }, 135),
                value -> {
                    if (UrlObfuscator.decode(new int[] { 236, 197, 163, 144 }, 152).equals(value)) revealNavOverlay.run();
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
        }, UrlObfuscator.decode(new int[] { 232, 166, 131, 116, 74, 45, 7, 199, 211, 178, 176, 140 }, 169));

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
        String spoofedUA = defaultUA.replace(UrlObfuscator.decode(new int[] { 129, 249, 143, 97 }, 186), "").replaceAll("Version/[0-9.]+\s", "");
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
        webView.addJavascriptInterface(new AndroidBridge(), UrlObfuscator.decode(new int[] { 138, 132, 109, 90, 40, 15, 225, 230, 177, 139, 101, 71, 90 }, 203));

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
            webView.setAlpha(1f);
            webView.setTranslationY(0f);
            // iOS-style: the page settles forward from just behind
            // the launch screen while that screen zooms away.
            webView.setScaleX(0.97f);
            webView.setScaleY(0.97f);
            webView.animate().scaleX(1f).scaleY(1f).setDuration(380)
                .setInterpolator(new DecelerateInterpolator()).start();
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
                if (hasLoadedOnce[0] && true) {
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
                if (true) {
                    view.evaluateJavascript(
                        UrlObfuscator.decode(new int[] { 168, 137, 99, 66, 49, 17, 190, 194, 189, 157, 118, 94, 39, 65, 207, 195, 168, 153, 101, 64, 44, 37, 244, 204, 160, 132, 103, 7, 102, 30, 16, 249, 206, 180, 147, 125, 122, 37, 31, 241, 211, 182, 220, 98, 81, 57, 11, 222, 199, 174, 134, 108, 92, 40, 8, 163, 130, 226, 149, 104, 78, 91, 49, 10, 178, 228, 133, 138, 115, 82, 58, 55, 251, 198, 188, 149, 57, 84, 57, 4, 226, 207, 165, 158, 38, 120, 25, 22, 239, 198, 174, 163, 111, 106, 80, 57, 65, 239, 200, 172, 157, 44 }, 220) +
                        UrlObfuscator.decode(new int[] { 155, 109, 89, 106, 10, 233, 215, 251, 131, 113, 77, 33, 21, 233, 240, 208, 245, 213, 96, 78, 43, 1, 236 }, 237) +
                        UrlObfuscator.decode(new int[] { 151, 123, 20, 44, 19, 247, 220, 184, 129, 59, 71, 48, 0, 254, 220, 163, 183, 51, 26, 123, 67, 251, 205, 179, 147, 119, 74, 120 }, 254) +
                        UrlObfuscator.decode(new int[] { 121, 79, 63, 76, 220, 151, 190, 129, 105, 66, 42, 19, 173, 203, 175, 142, 154, 108, 106, 53, 31, 238, 209, 244, 191, 43, 66, 61, 29, 246, 222, 167, 193, 103, 67, 34, 14, 248, 225, 173, 142, 97, 77, 48, 88, 235, 199, 232, 254, 169, 97, 64, 122, 50, 176, 202, 178, 130, 96, 70, 61, 73 }, 271) +
                        UrlObfuscator.decode(new int[] { 86, 94, 44, 93, 243, 206, 174, 196, 67, 106, 109 }, 288) +
                        UrlObfuscator.decode(new int[] { 64, 52, 6, 179, 194, 164, 131, 103, 19, 43, 25, 229, 201, 189, 129, 104, 72, 109, 8, 175, 214, 237, 151, 211, 118, 17, 46, 26, 254, 144, 163, 152, 99, 65, 122, 3, 231, 194, 184, 199, 85, 6, 100, 7, 165, 254, 225, 201, 114, 74, 2, 10, 250, 196, 164, 247, 202, 52, 16, 112, 82, 237, 151, 159, 223, 59, 64, 60, 52, 248, 200, 170, 138, 37, 24, 98, 70, 162, 128, 176, 201, 82, 13, 109, 22, 238, 230, 214, 166, 152, 120, 19, 110, 80, 180, 156, 254, 157, 59, 123, 123, 95, 228, 192, 136, 132, 116, 78, 46, 65, 188, 142, 234, 206, 44, 81, 35, 5, 175, 200, 151, 243, 136, 116, 124, 48, 0, 242, 210, 253, 192, 58, 111, 120, 75, 242, 149 }, 54) +
                        UrlObfuscator.decode(new int[] { 49, 7, 247, 132, 171, 131, 114, 116, 90, 38, 9, 161, 221, 175, 151, 123, 67, 63, 26, 250, 155, 183, 157, 57, 84, 40, 2, 254, 131, 188, 136, 122, 7, 47, 88, 180, 152, 171, 221, 101, 115, 16, 62, 20, 242, 214, 189, 182, 120, 82, 48, 7, 189, 222, 180, 158, 104, 90, 37, 87, 226, 129, 226, 193, 124, 80, 36, 22, 163, 204, 252, 133, 147, 48, 94, 52, 18, 246, 221, 150, 152, 114, 80, 39, 40, 251, 236, 235, 134, 104, 5, 34, 69, 228, 198, 172, 130, 82, 92, 52, 6, 191, 156, 253, 236, 216, 59, 82, 117, 20, 246, 220, 178, 160, 116, 88, 38, 23, 191, 196, 189, 135, 96, 4, 98, 68, 229, 205, 169, 129, 113, 76, 125, 82, 168, 210, 218, 170, 136, 110, 85, 122, 13, 234, 194, 179, 206, 105, 65, 55, 5, 229, 221, 160, 205, 106, 74, 38, 26, 237, 156, 187, 222 }, 71) +
                        UrlObfuscator.decode(new int[] { 46, 22, 228, 149, 186, 156, 118, 84, 35, 82, 234, 194, 175, 158, 103, 76, 38, 19, 168, 212, 177, 134, 112, 88, 19, 58, 18, 248, 223, 175, 149, 107, 121, 59, 26, 189, 147, 186, 159, 118, 28, 57, 7, 233, 201, 164, 198, 106, 73, 41, 16, 228, 215, 239, 145, 119, 71, 19, 60, 8, 232, 207, 181, 151, 52, 94, 56, 5, 225, 199, 254, 130, 117, 67, 43, 14, 248, 135, 190, 140, 112, 83, 39, 23, 225, 194, 238, 186, 114, 112, 82, 56, 65, 249, 207, 173, 140, 120, 88, 8, 88, 251, 131, 253, 152, 61, 2, 37, 95, 167, 194, 253, 196, 119, 10, 41, 13, 175, 195, 237, 147, 143, 127, 83, 112, 23, 251, 219, 189, 155, 49, 28, 111 }, 88) +
                        UrlObfuscator.decode(new int[] { 15, 231, 213, 238, 147, 101, 81, 98, 8, 189, 175, 133, 180, 192, 117, 85, 61, 29, 228, 152, 185, 145, 125, 85, 37, 24, 169, 136, 162, 153, 127, 4, 37, 13, 233, 193, 177, 140, 63, 19, 115, 80, 68, 247, 150, 247, 210, 97 }, 105) +
                        UrlObfuscator.decode(new int[] { 12, 248, 202, 247, 147, 121, 9, 61, 29, 245, 213, 188, 181, 100, 113, 112, 28, 232, 218, 231, 148, 56, 65, 47, 76, 230, 197, 203, 156, 146, 105, 85, 62, 16, 246, 208, 149, 153, 125, 86, 60, 5, 194, 202, 173, 153, 36, 2, 113 }, 122) +
                        UrlObfuscator.decode(new int[] { 226, 204, 225, 154, 41, 81, 44, 0, 247, 202, 253, 210, 207, 98, 65, 46, 85, 242, 220, 177, 144, 126, 65, 104, 75, 238, 205, 162, 193, 108, 66, 56, 31, 229, 196, 244, 218, 54, 89, 56, 17, 172, 213, 175, 175, 192, 32, 116, 39, 6, 235, 150, 165, 159, 114, 92, 39, 78, 172, 128, 179, 146, 127, 2, 39, 15, 239, 220, 249, 219, 82, 13, 32, 13, 239, 212, 214, 176, 136, 121, 0 }, 139) +
                        UrlObfuscator.decode(new int[] { 245, 221, 242, 139, 54, 64, 63, 17, 224, 219, 248, 131, 62, 71, 43, 4, 235, 195, 190, 215, 56, 9, 115, 79, 211, 137, 138, 200, 99, 112, 80, 41, 21, 245, 207, 188, 195 }, 156) +
                        UrlObfuscator.decode(new int[] { 219, 173, 153, 42, 74, 59, 90, 225, 192, 176, 160, 109, 76, 48, 42, 10, 248, 216, 136, 142, 96, 84, 50, 94, 240, 216, 250, 201 }, 173) +
                        UrlObfuscator.decode(new int[] { 215, 187, 212, 120, 73, 119, 28, 254, 197, 165, 152, 114, 75, 108, 77, 178, 137, 163, 131, 101, 79, 110, 20, 251, 197, 182, 202, 117, 75, 50, 9, 29, 247, 209, 181, 143, 99, 4, 101, 74, 177, 221, 189, 151, 118, 84, 62, 72, 242, 209, 188, 138, 120, 90, 45, 33, 234, 202, 165, 151, 42, 66, 51, 113, 17, 237, 221, 184, 147, 109, 65, 43, 10, 178, 133, 244, 219, 45, 0, 97, 94, 184, 133, 168, 133, 103, 92, 46, 8, 240, 193, 248 }, 190) +
                        UrlObfuscator.decode(new int[] { 185, 143, 127, 12, 63, 11, 238, 149, 162, 138, 43, 80, 34, 5, 207, 193, 210, 187, 211, 104, 84, 22, 22, 239, 210, 164, 182, 117, 64, 55, 89, 185, 148 }, 207) +
                        UrlObfuscator.decode(new int[] { 150, 158, 108, 29, 53, 8, 206, 220, 160, 131, 43, 29, 32, 18, 245, 140, 237, 210, 41, 69, 125, 76, 246, 213, 188, 134, 97, 24, 121, 94, 165, 201, 242, 248, 130, 97, 72, 58, 29, 164, 133, 234, 209, 125, 7, 116, 14, 237, 196, 174, 137, 48, 17, 118, 77, 225, 156, 224, 154, 121, 80, 34, 5, 188, 157, 130, 249, 141, 59, 71, 38, 13, 249, 208, 235, 200, 41, 20, 62, 24, 183, 211, 178, 153, 109, 76, 119, 84, 181, 128, 167, 194, 120, 95, 54, 0, 231, 162, 131, 224, 219, 104, 74, 56, 22, 176, 202, 169, 128, 114, 85, 108, 77, 178, 137, 161, 141, 105, 79, 37, 79, 174, 157 }, 224) +
                        UrlObfuscator.decode(new int[] { 152, 118, 7, 39, 30, 216, 206, 178, 157, 33, 92 }, 241) +
                        UrlObfuscator.decode(new int[] { 107, 71, 104, 126, 22, 252, 207, 143, 159, 97, 76, 127, 19, 249, 157, 250, 145, 126, 94, 59, 7, 227, 217, 174, 209 }, 258) +
                        UrlObfuscator.decode(new int[] { 101, 83, 35, 80, 233, 221, 240, 156, 106, 88, 58, 13, 193, 202, 170, 133, 119, 10, 34, 19, 81, 248, 210, 178, 143, 73, 80, 34, 18, 191, 201, 168, 194, 36, 10, 38, 14, 252, 141, 160, 131, 55, 89, 41, 21, 245, 192, 130, 143, 109, 64, 52, 119, 29, 238, 146, 183, 147, 119, 93, 31, 19, 252, 211, 187, 134, 56, 11, 38, 8, 165, 141, 167, 130, 117, 84, 43, 14, 185, 194, 176, 203, 109, 72, 2, 56, 14, 182, 138, 244, 202, 35 }, 275) +
                        UrlObfuscator.decode(new int[] { 82, 34, 16, 161, 204, 214, 176, 152, 111, 6, 23, 24, 236, 223, 248, 152, 117, 75, 122, 64, 188, 226, 175, 153, 100, 5, 39, 0, 230, 143, 245, 201, 73, 66, 54, 9, 174, 237, 209, 168, 146, 127, 18, 43, 86, 255, 211, 188, 147, 123, 70, 126, 28, 231, 135, 228, 197, 48 }, 292) +
                        UrlObfuscator.decode(new int[] { 76, 56, 10, 183, 212, 189, 201, 94, 83, 37, 24, 161, 195, 172, 148, 35, 18, 101, 14, 244, 140, 245, 202, 59, 23, 104, 91, 9, 255, 207, 252, 151, 127, 95, 44, 74, 228, 155, 184, 150, 116, 69, 107, 25, 239, 223, 236, 156, 58, 20, 58, 73, 241, 204, 160, 151, 106, 26 }, 58) +
                        UrlObfuscator.decode(new int[] { 34, 12, 161, 196, 174, 136, 96, 87, 126, 95, 188, 145, 150, 165, 137, 110, 66, 33, 15, 249, 197, 246, 135, 115, 14, 54, 30, 243, 218, 163, 136, 98, 95, 100, 10, 250, 194, 167, 145, 97, 113, 35, 15, 231, 250, 150, 244, 199, 105, 93, 119, 11, 242, 218, 176, 151, 103, 124, 62, 20, 234, 237, 162, 130, 127, 79, 39, 28, 244, 142, 160, 136, 42, 25, 55, 1, 13, 190, 207, 174, 198, 104, 94, 118, 16, 243, 193, 150, 156, 103, 95, 52, 6, 224, 202, 143, 135, 99, 76, 38, 19, 212, 192, 167, 151, 42, 8, 123, 54, 24, 181, 206, 169, 212, 110, 81, 51, 2, 253, 138, 235, 219, 106, 92, 42, 8, 249, 145, 185, 152, 39, 68, 34, 0, 241, 159, 180, 210, 60, 109, 94, 42, 21, 178, 214, 179, 151, 48, 69, 120, 2, 253, 215, 166, 153, 60, 93, 60, 67, 251, 194, 174, 157, 96, 14, 125, 24, 249, 192, 163, 149, 99, 119, 22, 56, 85, 224, 199, 164 }, 75) +
                        UrlObfuscator.decode(new int[] { 58, 20, 232, 145, 174, 150, 100, 21, 62, 78, 162, 138, 186, 211, 98, 68, 34, 14, 249, 146, 162, 204, 45, 12, 63, 19, 247, 210, 168, 247, 146, 120, 90, 47, 86, 235, 150, 163, 153, 101, 31, 57, 88, 253, 216, 228, 198, 97, 68, 102, 8, 225, 129, 232, 212, 41, 12, 47, 11, 239, 197, 204, 224, 204, 58, 29, 48, 68, 165, 138, 186, 156, 122, 86, 33, 92, 161, 134, 241, 154, 60, 1, 122, 71, 190, 149, 252, 146, 52, 15, 32, 9, 172, 171, 151, 230, 129 }, 92) +
                        UrlObfuscator.decode(new int[] { 16, 233, 199, 185, 140, 115 }, 109) +
                        UrlObfuscator.decode(new int[] { 8, 252, 206, 251, 152, 107, 5, 39, 23, 231, 199, 182, 180, 125, 95, 46, 26, 165, 207, 184, 196, 107, 71, 53, 2, 224, 214, 151, 141, 113, 108, 90, 56, 9, 206, 218, 190, 144, 109, 68, 127, 9, 232, 131, 233 }, 126) +
                        UrlObfuscator.decode(new int[] { 230, 200, 229, 191, 127, 88, 32, 6, 224, 142, 166, 151, 45, 64, 46, 18, 27, 251, 207, 136, 148, 106, 117, 61, 17, 226, 231, 181, 151, 123, 68, 35, 70, 160, 196, 162, 143, 111, 81, 7, 1, 174, 130, 225, 196, 43, 31, 125, 111, 87, 255, 206, 230, 152, 107, 23, 102, 70, 165, 158, 158, 147, 101, 88, 97, 3, 228, 194, 227, 152, 39, 95, 46, 2, 241, 204, 239, 144, 47, 72, 90, 55, 26, 244, 207, 243, 194 }, 143) +
                        UrlObfuscator.decode(new int[] { 208, 202, 173, 149, 52, 73, 116, 21, 253, 209, 162, 217, 102, 29, 38, 30, 224, 131, 188, 195, 123, 66, 46, 29, 224, 139, 180, 203, 108, 70, 43, 6, 232, 235, 146, 191, 142, 50, 1 }, 160) +
                        "}}" +
                        UrlObfuscator.decode(new int[] { 216, 182, 199, 97, 88, 56, 69, 230, 204, 166, 128, 114, 77, 120, 87, 171, 211, 165, 171, 139, 111, 82, 96 }, 177) +
                        UrlObfuscator.decode(new int[] { 173, 148, 116, 49, 77, 50, 14, 239, 146, 191, 141, 121, 85, 33, 29, 252, 220, 249, 145, 35, 76, 100, 23, 249, 207, 189, 157, 117, 72, 101, 5, 216, 147, 156, 205, 157, 69, 12, 1, 64, 231, 144, 227 }, 194) +
                        UrlObfuscator.decode(new int[] { 186, 148, 57, 95, 58, 26, 163, 192, 174, 132, 110, 92, 47, 88, 189, 148, 234, 141, 116, 84, 17, 50, 24, 242, 220, 174, 145, 37, 15, 102, 78 }, 211) +
                        UrlObfuscator.decode(new int[] { 146, 98, 80, 97, 11, 66, 242, 210, 191, 154, 110, 80, 55, 25, 184, 218, 166, 154, 117, 88, 62, 68, 226, 194, 175, 138, 126, 64, 39, 9, 168, 213, 165, 151, 106, 79, 33, 50, 27, 166 }, 228) +
                        UrlObfuscator.decode(new int[] { 130, 124, 90, 62, 20, 184, 196, 224, 129, 105, 69, 45, 29, 224, 153, 254, 195, 34, 72, 108, 2, 232, 254, 204, 156, 136, 51, 81, 119, 20, 242, 216, 178, 128, 123, 31, 96, 89, 178, 147, 240, 203, 36, 13, 96, 3, 186, 205, 235, 151, 118, 64, 50, 20, 13, 247, 211, 187, 211, 42, 21, 51, 89, 250, 208, 186, 148, 102, 89, 125, 94, 167, 150 }, 245) +
                        UrlObfuscator.decode(new int[] { 71, 75, 32, 17, 237, 200, 164, 157, 140, 116, 88, 60, 31, 183, 203, 182, 128, 112, 103, 56, 23, 253, 213, 187, 129, 99, 4, 32, 70, 195, 251, 136, 168, 43, 87, 55, 16, 232, 206, 216, 183, 155, 101, 19, 53, 12, 236, 158, 255, 206 }, 262) +
                        UrlObfuscator.decode(new int[] { 106, 85, 52, 0, 240, 218, 249, 149, 38, 85, 48, 17, 176 }, 279) +
                        UrlObfuscator.decode(new int[] { 91, 34, 18, 209, 205, 174, 135, 110, 85, 75, 118, 30, 253, 203, 246, 192, 40, 7, 127, 78, 231, 214, 166, 165, 121, 66, 43, 2, 249, 223, 226, 138, 105, 87, 106, 87, 188, 147, 242, 200, 59 }, 296) +
                        UrlObfuscator.decode(new int[] { 67, 32, 31, 250, 206, 186, 144, 63, 83, 124, 15, 238 }, 62), null);
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
                    UrlObfuscator.decode(new int[] { 103, 8, 248, 194, 168, 158, 96, 71, 41, 78, 172, 223 }, 79) +
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
                    UrlObfuscator.decode(new int[] { 20, 13, 231, 198 }, 96) +
                    "var vp=document.querySelector('meta[name=\"viewport\"]');" +
                    UrlObfuscator.decode(new int[] { 24, 246, 135, 239, 155, 124, 2, 49, 31, 248, 154, 162, 138, 103, 86, 47, 4, 238, 235, 144, 190, 142, 126, 91, 45, 29, 210, 218, 176, 153, 118, 92, 37, 88, 168, 195, 168, 152, 106, 13, 96, 83, 241, 214, 235, 151, 102, 86, 0, 20, 11, 236, 212, 190, 142, 110, 92, 112, 80, 248, 212, 185, 150, 53, 29, 119, 25, 231, 200, 187, 155, 101, 91, 60, 64, 175, 158, 160, 140, 97, 84, 45, 58, 16, 233, 146, 179, 159, 120, 92, 121, 23, 229, 196, 182, 156, 117, 115, 39, 7, 225, 200, 227, 156, 121, 1, 124, 27 }, 113) +
                    UrlObfuscator.decode(new int[] { 244, 209, 238, 172, 155, 105, 125, 47, 14, 235, 209, 181, 131, 97, 81, 123, 85, 242, 223, 161, 154, 104, 66, 63, 77, 165, 143, 176, 143, 97, 80, 43, 95, 229, 197, 201, 183, 158, 121, 22, 45, 16, 252, 195, 190, 217, 52, 90, 60, 24, 228, 198, 175, 129, 33, 88, 41, 8, 228, 194, 251, 212, 42, 19, 110, 65, 237, 254, 198, 180, 145, 110, 87, 116, 11, 244, 215, 185, 145, 46, 3, 127, 64, 163, 142, 160, 133, 101, 67, 36, 29, 234, 139, 182, 135, 98, 78, 36, 93, 78, 176, 141, 240, 219, 111, 74, 61, 5, 187, 198, 183, 146, 126, 80, 50, 3, 235, 144, 162, 132, 45, 0, 115 }, 130) +
                    UrlObfuscator.decode(new int[] { 250, 212, 249, 209, 107, 65, 46, 25, 230, 207, 167, 156, 41, 65, 32, 16, 198, 206, 164, 141, 154, 112, 73, 30, 2, 211, 221, 240, 208, 73, 106, 53, 29, 246, 195, 191, 134, 106, 123, 37, 14, 253, 217, 167, 149, 114, 99, 45, 27, 165, 136, 233, 164 }, 147) +
                    UrlObfuscator.decode(new int[] { 210, 162, 144, 33, 83, 75, 99, 25, 243, 216, 175, 148, 125, 89, 34, 91, 247, 193, 183, 144, 100, 74, 11, 1, 233, 198, 175, 135, 124, 15, 97, 22, 240, 218, 174, 132, 39, 54, 5, 46, 8, 181, 211, 189, 197, 48, 105, 10, 21, 253, 214, 163, 159, 102, 74, 27, 5, 238, 221, 185, 135, 117, 82, 3, 13, 251, 133, 250 }, 164) +
                    UrlObfuscator.decode(new int[] { 198, 160, 221, 102, 84, 40, 27, 205, 194, 162, 159, 111, 71, 60, 90, 161, 205, 176, 142, 110, 13, 34, 48, 26, 228, 199, 182, 155, 97, 21, 32, 31, 241, 192, 187, 200, 32, 0, 127, 24, 250, 141, 162, 135, 121, 71, 53, 18, 228, 202, 183, 217, 110, 86, 90, 44, 27, 240, 212, 173, 212, 96, 13, 62, 28, 240, 215, 183, 159, 49, 70, 35, 29, 227, 217, 190, 136, 102, 83, 125, 10, 242, 198, 176, 135, 108, 112, 73, 112, 4, 161, 217, 181, 145, 103, 23, 60, 25, 227, 221, 163, 132, 110, 64, 57, 87, 166, 221, 172, 138, 108, 79, 49, 73, 247, 199, 185, 148, 210, 109, 84, 38, 30, 183, 216, 188, 157, 99, 70, 32, 73, 163, 129, 224, 202, 47, 68, 33, 27, 229, 219, 188, 134, 104, 81, 127, 23, 231, 217, 180, 242, 141, 116, 70, 62, 87, 248, 220, 189, 131, 102, 64, 105, 67, 161, 128, 234, 207, 100, 65, 59, 5, 251, 220, 166, 136, 113, 31, 62, 69, 186 }, 181) +
                    UrlObfuscator.decode(new int[] { 162, 138, 103, 86, 47, 4, 238, 235, 144, 181, 153, 122, 94, 119, 25, 231, 198, 176, 154, 119, 113, 57, 25, 227, 202, 229, 159, 127, 3, 114, 21 }, 198) +
                    UrlObfuscator.decode(new int[] { 170, 149, 116, 64, 48, 26, 185, 213, 230, 149, 112 }, 215) +
                    "try{var m=document.querySelector('meta[name=\"theme-color\"]');" +
                    UrlObfuscator.decode(new int[] { 129, 97, 14, 40, 66, 165, 213, 168, 142, 155, 113, 74, 114, 58, 244, 221, 170, 152, 127, 81, 22, 1, 251, 213, 183, 138, 40, 11, 13, 5, 238, 219, 167, 142, 98, 103, 54, 10, 230, 198, 165, 241, 159, 109, 76, 55, 3, 205, 208, 178, 155, 112, 119, 60, 30, 254, 194, 230, 149, 76, 66, 47, 24, 230, 193, 163, 164, 119, 77, 39, 5, 228, 142, 222, 174, 141, 112, 66, 14, 17, 253, 218, 179, 182, 123, 95, 61, 3, 184, 194, 224, 138, 105, 95, 11, 29, 252, 213, 175, 135, 113, 87, 39, 73, 167, 252, 209, 179, 136, 126, 84, 45, 95, 190, 202, 169, 211, 52, 27, 106, 13 }, 232) +
                    UrlObfuscator.decode(new int[] { 132, 123, 86, 34, 22, 252, 155, 183, 216, 107, 82 }, 249) +
                    UrlObfuscator.decode(new int[] { 126, 91, 49, 28, 239, 195, 236, 148, 107, 79, 36, 48, 9, 179, 253, 181, 158, 107, 87, 62, 18, 215, 198, 186, 150, 118, 85, 105, 72, 204, 194, 175, 152, 102, 65, 35, 36, 247, 205, 167, 133, 100, 14, 94, 46, 13, 240, 194, 138, 152, 127, 82, 20, 20, 247, 216, 181, 131, 127, 90, 32, 9, 165, 208 }, 266) +
                    UrlObfuscator.decode(new int[] { 109, 91, 43, 88, 231, 223, 182, 159, 81, 85, 108, 22, 250, 192, 174, 152, 98, 69, 39, 64, 174, 221 }, 283) +
                    UrlObfuscator.decode(new int[] { 71, 49, 29, 174, 206, 164, 138, 99, 71, 117, 60, 219, 158, 178, 130, 112, 1, 37, 98, 26, 242, 223, 174, 151, 124, 86, 35, 88, 240, 216, 182, 159, 116, 94, 59, 40, 255, 195, 166, 186, 102, 65, 41, 18, 173, 233, 162, 150, 105, 14, 89, 50, 18, 243, 201, 242, 142, 113, 89, 50, 26, 227, 157, 187, 159, 126, 74, 60, 58, 229, 207, 190, 129, 39, 21, 111, 73, 243, 202, 172, 133, 111, 104, 16, 52, 18, 245, 223, 171, 176, 114, 95, 50, 28, 231, 159, 227, 217, 52 }, 49) +
                    UrlObfuscator.decode(new int[] { 53, 9, 233, 243, 219, 245, 153, 50, 65, 58, 16, 246, 223, 187, 218, 99, 71, 34, 24, 167, 203, 228, 215, 110, 23, 44, 70, 247, 199, 183, 129, 109, 86, 4, 12, 26, 243, 216, 178, 143, 33, 68 }, 66) +
                    UrlObfuscator.decode(new int[] { 58, 20, 185, 212, 160, 141, 120, 65, 46, 4, 253, 134, 165, 137, 97, 93, 101, 68, 226, 200, 222, 183, 147, 50, 82, 52, 29, 253, 207, 153, 147, 60, 87, 61, 18, 229, 194, 171, 131, 120, 5, 40, 6, 236, 222, 239, 217, 52, 10, 33, 9, 225, 246, 208, 243, 140, 110, 73, 49, 80, 243, 217, 182, 129, 126, 87, 63, 4, 161, 204, 162, 136, 114, 3, 114 }, 83) +
                    UrlObfuscator.decode(new int[] { 13, 229, 138, 162, 136, 158, 119, 83, 114, 18, 244, 221, 189, 143, 89, 83, 124, 23, 253, 210, 165, 130, 107, 67, 56, 69, 238, 198, 171, 146, 107, 64, 42, 23, 199, 205, 165, 178, 155, 115, 72, 114, 70, 169, 145, 180, 158, 116, 93, 61, 92, 225, 197, 188, 134, 37, 72, 36, 9, 252, 197, 162, 136, 113, 10, 39, 13, 226, 213, 210, 187, 147, 104, 126, 54, 28, 245, 210, 184, 129, 61, 8 }, 100) +
                    UrlObfuscator.decode(new int[] { 3, 245, 193, 242, 129, 113, 93, 61, 8, 177, 205, 191, 135, 107, 83, 47, 10, 234, 139, 177, 200, 123, 118, 88, 117, 93, 232, 147, 171, 157, 99, 67, 39, 26, 179, 220, 164, 156, 99, 21, 59, 13, 249, 138, 168, 213, 116, 8, 44, 10, 231, 199, 185, 175, 153, 54, 26, 116, 92, 179, 130, 174, 150, 100, 21, 54, 78, 225, 159, 185, 129, 106, 72, 52, 36, 236, 129, 239, 206, 33, 12, 127, 10, 228, 137, 161, 227, 206, 97, 64, 57, 70, 169, 145, 165, 147, 97, 65, 33, 28, 177, 222, 186, 130, 97, 23 }, 117) +
                    UrlObfuscator.decode(new int[] { 240, 196, 182, 195, 116, 28, 51, 113, 13, 232, 222, 168, 142, 107, 81, 57, 17, 189, 213, 248, 195, 61, 82, 102, 64, 254, 220, 167, 131, 125, 0, 96, 74, 162, 141, 248, 139, 103, 8, 73, 112, 17, 249, 213, 189, 141, 112, 11, 101, 92, 230, 214, 166, 132, 98, 65, 110, 3, 249, 199, 166, 210 }, 134) +
                    UrlObfuscator.decode(new int[] { 225, 215, 167, 212, 114, 94, 108, 6, 161, 194, 168, 130, 108, 94, 33, 86, 180, 153, 181, 133, 113, 81, 36, 38, 19, 241, 220, 168, 211, 108, 98, 107, 42, 191, 143, 229, 200, 100, 80, 34, 79, 252, 144, 188, 138, 120, 90, 45, 46, 232, 209, 236, 149, 89, 17, 29, 115, 79, 173, 149, 247, 157, 36, 72, 54, 4, 230, 209, 154, 156, 101, 24, 57, 53, 188, 241, 231, 219, 57, 1, 107, 4, 233, 153, 179, 131, 115, 83, 90, 23, 19, 232, 147, 172, 162, 42, 106, 122, 68, 164, 154, 233 }, 151) +
                    UrlObfuscator.decode(new int[] { 193, 161, 206, 108, 87, 13, 3, 207, 136, 205, 247, 129, 96, 82, 41, 55, 249, 249, 254, 146, 61, 79, 46, 24, 227, 225, 175, 163, 36, 73, 38, 64, 161, 213, 163, 145, 113, 81, 44, 65, 238, 234, 210, 177, 199, 105, 95, 45, 13, 229, 216, 174, 134, 41, 64, 125, 23, 181, 201, 225, 142, 49, 72, 37, 68, 230, 156, 164, 136, 126, 25, 60, 91 }, 168) +
                    UrlObfuscator.decode(new int[] { 207, 185, 133, 54, 93, 49, 11, 175, 215, 165, 129, 109, 89, 37, 4, 228, 129, 171, 206, 125, 83, 37, 17, 162, 201, 253, 185, 139, 115, 95, 47, 19, 246, 214, 255, 152, 60, 79, 61, 79, 220, 209, 187, 134, 35, 65, 42, 18, 161, 152, 235, 171, 100, 80, 43, 76, 236, 201, 209, 246, 207, 41, 14, 118, 23, 177, 158, 237, 135, 113, 71, 39, 3, 254, 143, 230, 131, 48, 26, 124, 86, 175, 151, 225, 223, 35, 4, 107, 74, 238, 177, 202, 178, 175, 111, 72, 48, 22, 240, 158, 228, 194, 58, 9, 44, 75, 253, 203, 185, 153, 121, 68, 105, 79, 164, 129, 238, 140, 43, 65, 111, 18, 86, 181, 213, 244, 152, 52, 94, 113, 92, 254, 157, 183, 221, 112, 24, 107, 18, 181 }, 185) +
                    UrlObfuscator.decode(new int[] { 172, 134, 122, 15, 48, 4, 246, 131, 169, 220, 48, 36, 85, 97, 31, 243, 219, 176, 150, 57, 90, 48, 26, 244, 198, 185, 203, 100, 5, 102, 69, 240, 220, 168, 154, 39, 69, 120, 20, 226, 208, 178, 133, 215, 121, 88, 40, 56, 245, 212, 168, 130, 98, 80, 48, 32, 230, 200, 188, 138, 38, 78, 36, 10, 227, 199, 147, 140, 91, 12, 106, 1, 227, 194, 171, 184, 140, 114, 73, 53, 30, 218, 215, 187, 153, 103, 29, 104, 27, 247, 152, 172, 200, 43, 79, 101, 11, 183, 149, 247, 200, 60, 29, 106, 16, 228, 212, 202, 172, 147, 60, 83, 63, 1, 176, 212, 255, 206, 105 }, 202) +
                    UrlObfuscator.decode(new int[] { 173, 155, 107, 24, 35, 75, 229, 213, 161, 129, 116, 24, 40, 11, 249, 239, 164, 135, 121, 93, 51, 3, 225, 247, 183, 155, 109, 69, 23, 58, 18, 255, 206, 183, 156, 118, 67, 120, 23, 251, 215, 171, 141, 108, 75, 33, 14, 249, 198, 175, 135, 124, 9, 34, 10, 231, 214, 175, 132, 110, 107, 123, 49, 25, 246, 223, 183, 140, 62, 24, 54, 27, 255, 221, 163, 217, 52 }, 219) +
                    UrlObfuscator.decode(new int[] { 133, 109, 2, 61, 78, 161, 142, 245, 202, 49, 27, 120, 74, 11, 176, 207, 247, 203, 52, 12, 96, 64, 188, 193, 250, 148, 57, 1, 126, 94, 191, 153, 230, 159, 36, 75, 97, 72, 180, 144, 241, 221, 50, 15, 118, 118, 12, 248, 200, 174, 136, 119, 24, 112, 85, 165, 129, 227, 199, 33, 5, 104, 85 }, 236) +
                    UrlObfuscator.decode(new int[] { 143, 121, 79, 47, 11, 246, 151, 241, 214, 114, 85, 52, 23, 246, 201, 233, 214, 113, 16 }, 253) +
                    UrlObfuscator.decode(new int[] { 120, 76, 62, 75, 249, 204, 166, 131, 68, 66, 121, 5, 247, 207, 163, 171, 151, 114, 82, 115, 83, 226, 204, 165, 143, 110, 66, 50, 0, 177, 216, 242, 158, 100, 79, 32, 40, 238, 128, 238, 221, 108, 66, 107, 10, 160, 157, 130, 169, 148, 114, 95, 53, 14, 182, 232, 137, 148, 122, 87, 32, 30, 249, 203, 140, 138, 64, 74, 57, 29, 161, 220, 177, 140, 106, 71, 45, 22, 174, 192, 225, 188, 146, 127, 72, 54, 17, 243, 244, 178, 184, 114, 65, 37, 77, 231, 149, 140, 130, 111, 88, 38, 1, 227, 228, 183, 141, 103, 69, 36, 78, 30, 238, 205, 176, 130, 74, 88, 63, 18, 212, 212, 183, 152, 117, 67, 63, 26, 224, 201, 228, 131, 35, 18, 53, 26, 229, 196, 176, 128, 106, 9, 37, 118, 5, 224, 193, 224 }, 270) +
                    UrlObfuscator.decode(new int[] { 108, 91, 51, 24, 217, 221, 241, 209, 44 }, 287) +
                    UrlObfuscator.decode(new int[] { 92, 50, 91, 179, 198, 185, 129, 106, 66, 59, 69, 213, 246, 169, 137, 98, 87, 43, 10, 230, 227, 167, 157, 145, 104, 82, 63, 83, 226, 207, 190, 152, 113, 91, 36, 92, 206, 239, 174, 128, 105, 94, 36, 3, 237, 234, 160, 164, 106, 81, 45, 6, 188, 212, 205, 171, 152, 39, 95, 53, 26, 237, 218, 179, 155, 96, 29, 51, 21, 244, 234, 184, 136, 98, 95, 6, 0, 251, 211, 163, 139, 97, 81, 106, 70, 230, 240, 221, 168, 143, 114, 84, 126, 84, 228, 211, 187, 144, 81, 85, 125, 4, 253, 219, 168, 197, 48 }, 53) +
                    UrlObfuscator.decode(new int[] { 48, 4, 246, 131, 174, 128, 115, 107, 106, 46, 65, 171, 129, 189, 151, 116, 67, 56, 17, 253, 198, 255, 145, 107, 74, 8, 26, 238, 196, 189, 164, 110, 85, 49, 1, 237, 199, 179, 200, 216, 106, 82, 41, 24, 242, 202, 172, 150, 100, 65, 115, 95, 244, 196, 190, 140, 122, 68, 35, 5, 162, 128, 179, 145, 103, 87, 100, 13, 191, 229, 161, 171, 155, 51, 82, 52, 13, 177, 145, 236, 159, 115, 28, 61, 95, 253, 209, 188, 154, 89, 95, 119, 82, 185, 152, 238, 148, 96, 80, 54, 16, 239, 155, 211, 191, 142, 104, 111, 41, 68, 246, 140, 165, 144, 122, 87, 16, 22, 184, 134, 245, 144, 32, 80, 41, 8, 248, 211, 179, 151, 97, 25, 54, 19, 245, 250, 146, 173, 157, 104, 73, 48, 14, 242, 140, 161, 134, 102, 87, 44, 89, 180, 217, 164, 130, 111, 69, 62, 70, 230, 194, 161, 161, 117, 71, 47, 20, 51, 247, 206, 168, 158, 116, 92, 42, 95, 177, 199, 177, 128, 123, 75, 53, 72, 162, 222, 169, 133, 110, 107, 47, 78, 189 }, 70) +
                    UrlObfuscator.decode(new int[] { 35, 4, 236, 207, 165, 147, 99, 16, 45, 9, 217, 145, 165, 159, 101, 68, 124, 16, 228, 214, 227, 128, 102, 115, 92, 54, 24, 248, 134, 188, 140, 118, 84, 34, 28, 251, 221, 250, 216, 107, 70, 40, 69, 238, 204, 158, 192, 122, 66, 50, 16, 246, 205, 249, 131, 103, 75, 3, 46, 25, 239, 238, 176, 149, 114, 89, 32, 0, 187, 212, 164, 158, 108, 90, 36, 3, 229, 130, 224, 147, 101, 65, 17, 89, 237, 215, 173, 140, 196, 109, 88, 50, 31, 216, 222, 240, 222, 45, 72, 120, 66, 167, 129, 249, 212, 115, 22 }, 87) +
                    UrlObfuscator.decode(new int[] { 6, 226, 209, 229, 169, 118, 86, 32, 20, 22, 241, 211, 147, 153, 105, 92, 42, 1, 243, 199, 252, 145, 117, 98, 51, 7, 235, 201, 229, 197, 101, 75, 59, 2, 244, 211, 161, 203, 102, 78, 35, 42, 19, 248, 210, 175, 212, 125, 87, 52, 3, 248, 209, 189, 134, 84, 92, 42, 3, 232, 194, 191, 198, 114, 73, 51, 18, 247, 205, 161, 151, 117, 69, 76, 100, 9, 238, 206, 191, 213, 121, 67, 34, 7, 253, 209, 167, 133, 117, 105, 39, 1, 248, 206, 184, 211, 83, 0, 37, 9, 229, 208, 177, 198, 44, 56, 77, 41, 5, 247, 223, 254, 212, 48, 82, 52, 0, 242, 159, 165, 152, 106, 67, 40, 75, 214, 215, 224, 211 }, 104) +
                    UrlObfuscator.decode(new int[] { 16, 254, 159, 178, 154, 119, 70, 63, 20, 254, 219, 224, 143, 99, 79, 51, 64, 230, 194, 177, 197, 73, 86, 54, 0, 244, 246, 209, 179, 179, 121, 73, 60, 10, 225, 211, 167, 220, 113, 85, 2, 19, 231, 203, 169, 197, 37, 69, 43, 27, 226, 212, 179, 129, 43, 70, 46, 3, 10, 243, 216, 178, 143, 52, 91, 55, 19, 239, 153, 175, 146, 102, 69, 34, 6, 236, 216, 184, 142, 121, 19, 60, 21, 243, 192, 232, 130, 118, 85, 50, 54, 28, 232, 200, 190, 188, 112, 84, 35, 19, 231, 142, 136, 213, 114, 92, 46, 29, 254, 139, 231, 205, 122, 92, 62, 10, 224, 131, 158, 159, 40, 27, 66, 61, 28, 232, 216, 178, 209, 125, 30, 45, 8, 233 }, 121) +
                    UrlObfuscator.decode(new int[] { 247, 212, 171, 134, 114, 70, 44, 75, 231, 136, 187, 162 }, 138) +
                    UrlObfuscator.decode(new int[] { 239, 200, 160, 131, 126, 80, 125, 85, 228, 219, 191, 148, 96, 89, 99, 51, 212, 203, 167, 140, 117, 73, 44, 0, 203, 195, 177, 148, 150, 125, 78, 30, 20, 239, 215, 188, 222, 109, 66, 61, 29, 246, 222, 167, 193, 81, 114, 45, 5, 238, 219, 167, 142, 98, 109, 37, 19, 246, 200, 163, 172, 188, 114, 73, 53, 30, 164, 204, 165, 131, 112, 15 }, 155) +
                    UrlObfuscator.decode(new int[] { 200, 164, 137, 124, 69, 34, 8, 241, 138, 162, 134, 101, 101, 73, 59, 19, 232, 247, 179, 138, 108, 82, 56, 16, 230, 155, 245, 146, 124, 70, 45, 6, 171, 135, 172, 156, 102, 68, 50, 12, 235, 205, 234, 132, 41, 100 }, 172) +
                    "var t=e.target;var el=t&&t.closest?t.closest('button,a,[role=\"button\"],input[type=\"button\"],input[type=\"submit\"]'):null;" +
                    UrlObfuscator.decode(new int[] { 212, 186, 211, 127, 85, 126, 81, 225, 220, 186, 151, 125, 70, 126, 46, 224, 201, 190, 132, 99, 77, 10, 21, 239, 193, 163, 134, 36, 7, 1, 49, 26, 239, 211, 178, 158, 91, 74, 62, 18, 242, 209, 253, 132, 120, 82, 61, 15, 249, 201, 226, 145, 72, 70, 35, 20, 234, 205, 167, 160, 115, 73, 91, 57, 24, 178, 205, 179, 155, 106, 86, 34, 16, 188, 154, 233, 140 }, 189) +
                    UrlObfuscator.decode(new int[] { 179, 193, 120, 89, 63, 12, 161, 156, 187, 152, 103, 66, 54, 2, 232, 183, 219, 244, 135, 102 }, 206) +
                    // Polyfills the Web Share API on top of the real Android
                    // share sheet -- most WebView builds don't implement
                    // navigator.share at all, so a page's own "Share" button
                    // either does nothing or falls back to a hand-rolled
                    // copy-link menu instead of the native chooser. Only
                    // installed when the page doesn't already have a working
                    // navigator.share (some newer WebView versions do).
                    UrlObfuscator.decode(new int[] { 171, 140, 100, 71, 50, 28, 177, 207, 190, 152, 113, 91, 36, 92, 208, 222, 171, 156, 98, 69, 47, 40, 251, 193, 163, 129, 96, 2, 101, 35, 239, 196, 205, 177, 148, 120, 121, 40, 16, 252, 208, 179, 219, 103, 91, 51, 3, 245, 137, 232, 204, 98, 74, 60, 0, 239, 198, 178, 138, 118, 13, 49, 9, 225, 237, 219, 244, 135 }, 223) +
                    UrlObfuscator.decode(new int[] { 158, 110, 88, 36, 11, 234, 222, 166, 154, 41, 85, 45, 5, 241, 199, 252, 134, 138, 112, 94, 40, 18, 245, 215, 240, 147, 119, 65, 53, 90, 233 }, 240) +
                    UrlObfuscator.decode(new int[] { 101, 65, 75, 63, 64, 248, 218, 174, 152, 100, 75, 45, 8, 175 }, 257) +
                    UrlObfuscator.decode(new int[] { 102, 67, 41, 20, 207, 195, 168, 153, 101, 64, 44, 37, 244, 204, 160, 132, 103, 15, 51, 55, 31, 239, 217, 243, 169, 109, 74, 62, 24, 242, 156, 183, 147, 101, 81, 97, 26, 228, 216, 167, 143, 117, 84, 96, 65, 172, 136, 144, 150, 115, 73, 81, 57, 85, 248, 218, 174, 152, 54, 67, 51, 13, 224, 207, 174, 214, 55, 6, 98, 62, 248, 217, 163, 135, 111, 15, 34, 4, 240, 194, 236, 148, 114, 115, 66, 33, 91, 188, 147, 240, 195 }, 274) +
                    UrlObfuscator.decode(new int[] { 81, 39, 21, 245, 237, 208, 253, 172, 105, 85, 52, 17, 228, 211, 251, 134, 118, 65, 62, 28, 249, 203, 229, 197, 48, 87, 42, 9, 243, 197, 173, 204, 102, 11, 58, 18, 26, 234, 200, 174, 149, 58, 105, 42, 24, 251, 220, 167, 150, 60, 67, 53, 5, 235, 206, 184, 195, 111, 0, 115, 26, 251, 158 }, 291) +
                    UrlObfuscator.decode(new int[] { 87, 57, 1, 255, 210, 181, 135, 125, 67, 126, 12, 239, 195, 159, 131, 107, 91, 45, 90, 224, 208, 170, 128, 118, 72, 47, 49, 86, 180, 199, 169, 159, 109, 77, 37, 24, 181, 192, 161, 135, 116, 11, 50, 85 }, 57) +
                    UrlObfuscator.decode(new int[] { 55, 20, 235, 198, 178, 134, 108, 11, 39, 72, 251, 226 }, 74) +
                    // Reports the scrollTop of whichever element just
                    // scrolled -- document or any nested panel -- so native
                    // knows whether a pull-to-refresh gesture is actually
                    // safe (see SwipeRefreshLayout override above). Scroll
                    // events don't bubble, so this has to be a capture
                    // listener on window to see scrolling from any
                    // descendant, not just the document itself. Throttled
                    // with a trailing rAF flag so a fast scroll doesn't
                    // spam the JS bridge with a call per pixel.
                    UrlObfuscator.decode(new int[] { 47, 8, 224, 195, 190, 144, 61, 21, 36, 27, 255, 212, 160, 153, 35, 115, 20, 11, 231, 204, 181, 137, 108, 64, 16, 1, 243, 207, 211, 178, 191, 115, 78, 52, 29, 177, 204, 161, 156, 122, 87, 61, 6, 190, 240, 145, 140, 98, 79, 56, 6, 225, 195, 149, 134, 118, 76, 46, 13, 194, 240, 203, 179, 152, 38, 78, 43, 13, 242, 141 }, 91) +
                    UrlObfuscator.decode(new int[] { 26, 234, 216, 233, 152, 98, 72, 33, 13, 237, 197, 252, 134, 158, 114, 78, 57, 87, 246, 216, 171, 131, 70, 90, 39, 78, 244, 208, 188, 156, 107, 22 }, 108) +
                    UrlObfuscator.decode(new int[] { 10, 245, 213, 190, 150, 111, 25, 55, 17, 240, 246, 164, 148, 126, 91, 2, 4, 255, 223, 175, 135, 109, 85, 110, 66, 247, 192, 176, 142, 108, 115, 25, 113, 26, 238, 212, 186, 140, 126, 89, 59, 92, 246, 155, 170 }, 125) +
                    UrlObfuscator.decode(new int[] { 231, 203, 228, 155, 111, 71, 44, 14, 232, 194, 237, 145, 103, 85, 53, 45, 16, 166, 204, 190, 148, 125, 81, 57, 17, 168, 192, 161, 135, 116, 11 }, 142) +
                    UrlObfuscator.decode(new int[] { 237, 219, 172, 137, 126, 73, 45, 57, 249, 223, 184, 149, 103, 91, 62, 30, 201, 220, 172, 129, 110, 2, 47, 29, 233, 197, 177, 141, 108, 76, 105, 73, 4, 238, 216, 178, 159, 115, 87, 63, 74, 240, 212, 184, 128, 119, 10 }, 159) +
                    UrlObfuscator.decode(new int[] { 198, 174, 156, 45, 73, 39, 87, 161, 205, 233, 146, 100, 86, 36, 7, 245, 134, 153, 187, 211, 104, 90, 40, 30, 253, 195, 248, 155, 123, 87, 55, 37, 233, 223, 171, 208, 49, 22, 123, 64, 183, 194, 232, 145, 101, 81, 37, 4, 244, 165, 218, 178, 159, 110, 87, 60, 22, 227, 152, 166, 151, 97, 93, 61, 28, 230, 192, 170, 169, 103, 79, 36, 13, 233, 210, 254 }, 176) +
                    UrlObfuscator.decode(new int[] { 183, 129, 141, 62, 73, 51, 11, 167, 220, 180, 200, 115, 89, 122, 0, 241, 195, 191, 131, 98, 121, 35, 27, 176, 153, 243 }, 193) +
                    UrlObfuscator.decode(new int[] { 164, 144, 98, 15, 62, 2, 255, 150, 190, 134, 120, 25, 118, 94, 237, 197, 234, 145, 111, 108, 31, 96, 65, 247, 219, 170, 140, 71, 89, 38, 93, 232, 222, 176, 131, 123, 126, 34, 31, 182, 218, 166, 155, 60, 79, 35, 76, 244, 203, 175, 132, 144, 105, 19, 29, 21, 254, 203, 183, 158, 114, 119, 38, 26, 246, 214, 181, 201, 40, 108, 34, 15, 248, 198, 161, 131, 68, 87, 45, 7, 229, 196, 238, 173, 155, 109, 83, 41, 14, 202, 219, 165, 153, 121, 88, 7, 29, 225, 153, 180, 175, 99, 72, 57, 5, 224, 204, 133, 148, 108, 64, 36, 7, 175, 210, 218, 174, 146, 110, 79, 9, 26, 234, 216, 186, 153, 64, 92, 34, 89, 228, 192, 190, 145, 60, 2, 113, 20, 245 }, 210) +
                    UrlObfuscator.decode(new int[] { 158, 43, 26, 61, 115, 10, 239, 201, 190, 211, 34, 69, 42, 21, 244, 192, 176, 154, 57, 85, 102, 21, 240 }, 227) +
                    UrlObfuscator.decode(new int[] { 137, 58, 26, 120, 75 }, 244),
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
                if (rel.isEmpty()) rel = UrlObfuscator.decode(new int[] { 108, 74, 39, 7, 249, 142, 215, 170, 144, 112 }, 261);

                byte[] data = embedCache.get(rel);
                if (data == null) {
                    data = EmbeddedAssets.get(rel);
                    if (data != null) embedCache.put(rel, data);
                }
                if (data == null) return super.shouldInterceptRequest(view, request);

                return new WebResourceResponse(guessEmbeddedMime(rel), UrlObfuscator.decode(new int[] { 67, 97, 18, 94, 170 }, 278), new ByteArrayInputStream(data));
            }

            private String guessEmbeddedMime(String rel) {
                String lower = rel.toLowerCase();
                if (lower.endsWith(UrlObfuscator.decode(new int[] { 9, 46, 17, 233, 207 }, 295)) || lower.endsWith(UrlObfuscator.decode(new int[] { 19, 52, 15, 247 }, 61))) return UrlObfuscator.decode(new int[] { 58, 8, 244, 223, 229, 129, 124, 74, 42 }, 78);
                if (lower.endsWith(UrlObfuscator.decode(new int[] { 113, 29, 238, 207 }, 95))) return UrlObfuscator.decode(new int[] { 4, 234, 214, 185, 195, 104, 89, 58 }, 112);
                if (lower.endsWith(UrlObfuscator.decode(new int[] { 175, 202, 204 }, 129)) || lower.endsWith(UrlObfuscator.decode(new int[] { 188, 220, 186, 156 }, 146))) return UrlObfuscator.decode(new int[] { 194, 178, 145, 108, 118, 93, 60, 8, 242, 213, 183, 215, 125, 87, 35, 21, 224, 209, 163, 153, 127, 90 }, 163);
                if (lower.endsWith(UrlObfuscator.decode(new int[] { 154, 185, 129, 126, 94 }, 180))) return UrlObfuscator.decode(new int[] { 164, 148, 115, 78, 40, 3, 30, 234, 212, 179, 149, 53, 83, 43, 24, 248 }, 197);
                if (lower.endsWith(UrlObfuscator.decode(new int[] { 248, 134, 98, 84 }, 214))) return UrlObfuscator.decode(new int[] { 142, 107, 68, 35, 6, 173, 210, 182, 184, 213, 101, 81, 55 }, 231);
                if (lower.endsWith(UrlObfuscator.decode(new int[] { 214, 103, 88, 50 }, 248))) return UrlObfuscator.decode(new int[] { 96, 69, 38, 1, 224, 139, 179, 140, 102 }, 265);
                if (lower.endsWith(UrlObfuscator.decode(new int[] { 52, 83, 40, 16 }, 282)) || lower.endsWith(UrlObfuscator.decode(new int[] { 30, 37, 30, 232, 203 }, 48))) return UrlObfuscator.decode(new int[] { 40, 13, 30, 249, 216, 243, 145, 106, 92, 63 }, 65);
                if (lower.endsWith(UrlObfuscator.decode(new int[] { 124, 22, 249, 201 }, 82))) return UrlObfuscator.decode(new int[] { 10, 239, 192, 167, 186, 209, 122, 85, 61 }, 99);
                if (lower.endsWith(UrlObfuscator.decode(new int[] { 90, 228, 215, 179, 128 }, 116))) return UrlObfuscator.decode(new int[] { 236, 201, 162, 133, 100, 15, 72, 59, 31, 236 }, 133);
                if (lower.endsWith(UrlObfuscator.decode(new int[] { 184, 194, 187, 149, 116, 3 }, 150))) return UrlObfuscator.decode(new int[] { 193, 169, 139, 112, 12, 53, 14, 230, 249, 140 }, 167);
                if (lower.endsWith(UrlObfuscator.decode(new int[] { 150, 160, 153, 115, 82 }, 184))) return UrlObfuscator.decode(new int[] { 175, 135, 105, 82, 106, 19, 236, 196, 167 }, 201);
                if (lower.endsWith(UrlObfuscator.decode(new int[] { 244, 141, 108, 81 }, 218))) return UrlObfuscator.decode(new int[] { 141, 101, 71, 60, 72, 242, 209, 162 }, 235);
                if (lower.endsWith(UrlObfuscator.decode(new int[] { 210, 114, 89, 54 }, 252))) return UrlObfuscator.decode(new int[] { 100, 65, 42, 13, 236, 135, 191, 203, 108, 71, 44, 12 }, 269);
                return UrlObfuscator.decode(new int[] { 127, 77, 44, 23, 243, 218, 185, 131, 127, 90, 58, 92, 253, 210, 164, 138, 122, 0, 63, 31, 248, 204, 169, 138 }, 286);
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
                    showOffline(UrlObfuscator.decode(new int[] { 124, 7, 38, 193, 144 }, 52) + status, request.getUrl().toString());
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
                int errBg = Color.parseColor(isOfflineError(description) ? UrlObfuscator.decode(new int[] { 102, 34, 197, 228, 135, 166, 185 }, 69) : UrlObfuscator.decode(new int[] { 117, 69, 213, 131, 147, 193, 82 }, 86));
                errorView.setBackgroundColor(errBg);
                errorWeb.setBackgroundColor(errBg);
                errorWeb.loadDataWithBaseURL(UrlObfuscator.decode(new int[] { 15, 242, 209, 180, 144, 56, 14, 111, 58, 12, 239, 211, 169, 212, 117, 87, 52, 23, 249, 155 }, 103),
                    renderErrorPage(description, failingUrl), UrlObfuscator.decode(new int[] { 12, 242, 206, 161, 219, 123, 70, 60, 28 }, 120), UrlObfuscator.decode(new int[] { 220, 252, 129, 203, 61 }, 137), null);
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
                popupSettings.setUserAgentString(popupDefaultUA.replace(UrlObfuscator.decode(new int[] { 161, 153, 175, 129 }, 154), "").replaceAll("Version/[0-9.]+\s", ""));
                popupWebView.setVerticalScrollBarEnabled(false);
                popupWebView.setHorizontalScrollBarEnabled(false);

                final Dialog popupDialog = new Dialog(MainActivity.this, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
                // Loading overlay: same bouncing dots + colors as the main
                // navigation overlay, shown over the popup until its page has
                // painted, so there is never an empty black/white screen while
                // a link (sign-in, Telegram, any website) is still loading.
                final FrameLayout popupRoot = new FrameLayout(MainActivity.this);
                popupRoot.setBackgroundColor(Color.parseColor(UrlObfuscator.decode(new int[] { 136, 250, 217, 56, 23, 118, 85 }, 171)));
                popupRoot.addView(popupWebView, new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
                final FrameLayout popupLoader = new FrameLayout(MainActivity.this);
                popupLoader.setBackgroundColor(Color.parseColor(UrlObfuscator.decode(new int[] { 159, 235, 202, 41, 8, 103, 70 }, 188)));
                final BouncingDotsView popupDots = new BouncingDotsView(MainActivity.this, Color.parseColor(UrlObfuscator.decode(new int[] { 238, 170, 57, 108, 123, 45, 194 }, 205)));
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
                                UrlObfuscator.decode(new int[] { 170, 152, 100, 79, 117, 17, 236, 218, 186 }, 222), UrlObfuscator.decode(new int[] { 186, 90, 107, 97, 83 }, 239), failed);
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
                                renderErrorPage(UrlObfuscator.decode(new int[] { 72, 75, 106, 13, 92 }, 256) + status, failed),
                                UrlObfuscator.decode(new int[] { 101, 85, 55, 26, 162, 196, 191, 135, 101 }, 273), UrlObfuscator.decode(new int[] { 119, 21, 38, 82, 166 }, 290), failed);
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
                        if (scheme != null && !scheme.equals(UrlObfuscator.decode(new int[] { 80, 35, 2, 229 }, 56)) && !scheme.equals(UrlObfuscator.decode(new int[] { 33, 28, 243, 214, 182 }, 73))) {
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
        return EMBED_HOST + UrlObfuscator.decode(new int[] { 51, 23, 252, 210, 174, 219, 124, 71, 63, 29 }, 90);
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
        if (Intent.ACTION_SEND.equals(action) && UrlObfuscator.decode(new int[] { 31, 239, 209, 188, 200, 118, 73, 37, 10, 236 }, 107).equals(intent.getType())) {
            String shared = intent.getStringExtra(Intent.EXTRA_TEXT);
            if (shared != null && !shared.isEmpty()) {
                try {
                    return baseUrl() + UrlObfuscator.decode(new int[] { 67, 232, 210, 184, 138, 114, 82, 10, 0, 246, 202, 165, 205 }, 124) + java.net.URLEncoder.encode(shared, UrlObfuscator.decode(new int[] { 216, 248, 141, 199, 49 }, 141));
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

        String shortcutAction = intent.getStringExtra(UrlObfuscator.decode(new int[] { 237, 213, 179, 137, 110, 90, 45, 3, 201, 212, 183, 135, 123, 94, 62 }, 158));
        if (UrlObfuscator.decode(new int[] { 221, 171, 129, 99, 74, 46 }, 175).equals(shortcutAction)) {
            webView.getSettings().setCacheMode(WebSettings.LOAD_NO_CACHE);
            webView.reload();
            webView.getSettings().setCacheMode(WebSettings.LOAD_CACHE_ELSE_NETWORK);
            return;
        }
        if (UrlObfuscator.decode(new int[] { 168, 176, 147, 120 }, 192).equals(shortcutAction)) {
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
                blobTmp = File.createTempFile(UrlObfuscator.decode(new int[] { 179, 156, 96, 76 }, 209), UrlObfuscator.decode(new int[] { 204, 117, 77, 79 }, 226), getCacheDir());
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
                Toast.makeText(MainActivity.this, UrlObfuscator.decode(new int[] { 183, 125, 70, 62, 3, 225, 204, 168, 203, 108, 72, 33, 11, 227, 193, 254, 195, 97, 78, 53, 51, 26, 189, 210, 180, 142, 57, 74, 50, 23, 241, 148, 167, 154, 120, 67, 111, 8, 228, 192, 174 }, 243), Toast.LENGTH_LONG).show();
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
                    sendIntent.setType(UrlObfuscator.decode(new int[] { 112, 70, 58, 21, 175, 239, 210, 188, 149, 117 }, 260));
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
                String safeTitle = (title == null || title.trim().isEmpty()) ? UrlObfuscator.decode(new int[] { 126, 91, 61, 11, 248 }, 277) : title;
                String safeBody = body == null ? "" : body;
                NotificationCompat.Builder notification = new NotificationCompat.Builder(MainActivity.this, UrlObfuscator.decode(new int[] { 66, 32, 2, 226, 215, 173, 148 }, 294))
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
            if (hex == null || !hex.matches(UrlObfuscator.decode(new int[] { 98, 120, 82, 194, 249, 250, 176, 116, 25, 53, 66, 188, 137, 146, 149, 59, 81, 55, 49, 200, 133, 129, 135, 40, 66, 115, 79, 184, 253, 196, 237, 128, 53, 31 }, 60))) return;
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
                android.content.SharedPreferences sp = getSharedPreferences(UrlObfuscator.decode(new int[] { 62, 7, 238, 198, 172, 156, 104, 72, 26, 18, 178 }, 77), MODE_PRIVATE);
                org.json.JSONArray idx = new org.json.JSONArray(sp.getString(UrlObfuscator.decode(new int[] { 1, 34, 245, 223, 162 }, 94), "[]"));
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
                ed.putString("p:" + key, json).putString(UrlObfuscator.decode(new int[] { 48, 209, 196, 168, 147 }, 111), next.toString()).apply();
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
            if (hex == null || !hex.matches(UrlObfuscator.decode(new int[] { 222, 188, 229, 156, 209, 93, 91, 116, 30, 167, 155, 236, 169, 104, 4, 44, 84 }, 128))) return;
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
                        Uri.parse(UrlObfuscator.decode(new int[] { 225, 209, 172, 133, 108, 75, 46, 80 }, 145) + getPackageName()));
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

        // Best-effort root check -- the same handful of signals most root-
        // detection libraries use (no single one is conclusive on its own,
        // which is why it checks several). This only ever reports whether
        // root *appears* to be present; it never grants anything by
        // itself. Call from the page like:
        //   if (window.AndroidBridge && AndroidBridge.isDeviceRooted
        //       && AndroidBridge.isDeviceRooted()) {
        //     AndroidBridge.requestRootAccess();
        //   }
        @JavascriptInterface
        public boolean isDeviceRooted() {
            if (Build.TAGS != null && Build.TAGS.contains(UrlObfuscator.decode(new int[] { 214, 164, 147, 139, 51, 86, 57, 2, 233 }, 162))) return true;
            String[] suPaths = {
                UrlObfuscator.decode(new int[] { 156, 161, 136, 99, 91, 43, 0, 163, 201, 163, 135, 39, 84, 51 }, 179), UrlObfuscator.decode(new int[] { 235, 144, 123, 82, 52, 58, 19, 178, 196, 185, 147, 119, 23, 36, 3 }, 196), UrlObfuscator.decode(new int[] { 250, 135, 113, 91, 63, 95, 252, 219 }, 213),
                UrlObfuscator.decode(new int[] { 201, 118, 93, 48, 22, 228, 205, 144, 191, 141, 108, 20, 9, 12, 232, 210, 164, 128, 103, 86, 32, 95, 241, 223, 165 }, 230), UrlObfuscator.decode(new int[] { 216, 101, 76, 39, 7, 247, 220, 255, 142, 126, 93, 99, 56, 255, 217, 173, 149, 85, 112, 106, 2, 242, 202 }, 247),
                UrlObfuscator.decode(new int[] { 39, 67, 39, 17, 229, 140, 174, 142, 99, 126, 82, 114, 4, 249, 211, 183, 215, 100, 67 }, 264), UrlObfuscator.decode(new int[] { 54, 92, 54, 2, 244, 155, 191, 157, 114, 81, 35, 65, 239, 197, 165, 197, 122, 93 }, 281), UrlObfuscator.decode(new int[] { 0, 42, 12, 248, 202, 229, 133, 103, 68, 39, 9, 171, 208, 183 }, 47),
                UrlObfuscator.decode(new int[] { 111, 44, 11, 178, 222, 178, 148, 54, 75, 34 }, 64), UrlObfuscator.decode(new int[] { 126, 3, 246, 221, 185, 137, 102, 5, 43, 1, 233, 137, 163, 133, 106, 78, 50, 1, 25, 251, 146, 175, 142 }, 81), UrlObfuscator.decode(new int[] { 77, 242, 217, 204, 170, 152, 113, 20, 41, 29, 183, 207, 180, 156, 122, 28, 33, 4 }, 98)
            };
            for (String path : suPaths) {
                if (new File(path).exists()) return true;
            }
            // Magisk hides its su paths on some configs but the manager
            // app itself is still installed under its own package name.
            String[] knownRootPackages = { UrlObfuscator.decode(new int[] { 16, 253, 220, 254, 155, 97, 93, 38, 4, 226, 199, 191, 146, 40, 72, 37, 4, 235, 210, 171 }, 115), UrlObfuscator.decode(new int[] { 225, 214, 236, 130, 104, 126, 87, 51, 26, 242, 200, 188, 214, 100, 67, 37, 17, 225, 193, 164 }, 132), UrlObfuscator.decode(new int[] { 246, 219, 190, 220, 127, 95, 60, 6, 248, 202, 164, 159, 39, 73, 41, 2, 247, 203, 170, 134, 47, 83, 74 }, 149) };
            PackageManager pm = getPackageManager();
            for (String pkg : knownRootPackages) {
                try {
                    pm.getPackageInfo(pkg, 0);
                    return true;
                } catch (PackageManager.NameNotFoundException ignored) {
                }
            }
            return false;
        }

        // Actually asks for su -- but only bothers if isDeviceRooted()
        // above already found root; on a non-rooted device there's no su
        // to call, so this shows a plain toast explaining why instead of
        // silently doing nothing or failing with no explanation. When
        // root IS present, access is still decided entirely by whatever
        // root manager owns su on this device (Magisk/SuperSU) -- it
        // shows its own grant/deny prompt (or auto-grants, if the device
        // owner configured that app allowlist themselves), and this code
        // has no way to see or skip that step. Runs off the UI thread
        // since Process.waitFor() blocks. Reports the result via
        // window.onAndroidRootAccessResult(granted) and an
        // 'androidRootAccessResult' CustomEvent, same pattern as Google
        // sign-in below.
        @JavascriptInterface
        public void requestRootAccess() {
            if (!isDeviceRooted()) {
                runOnUiThread(() -> {
                    Toast.makeText(MainActivity.this,
                        UrlObfuscator.decode(new int[] { 242, 173, 141, 112, 2, 39, 5, 30, 234, 200, 174, 158, 58, 87, 61, 18, 242, 198, 244, 146, 50, 67, 63, 0, 250, 200, 168, 203, 110, 76, 62, 14, 229, 192, 228, 151, 109, 1, 55, 48, 12, 246, 146 }, 166), Toast.LENGTH_SHORT).show();
                    webView.evaluateJavascript(
                        UrlObfuscator.decode(new int[] { 159, 176, 128, 122, 80, 38, 24, 255, 193, 230, 196, 119, 95, 56, 16, 243, 206, 160, 205, 115, 74, 44, 5, 239, 232, 144, 178, 146, 90, 84, 61, 10, 248, 223, 177, 166, 124, 93, 37, 49, 236, 205, 168, 159, 120, 120, 44, 27, 242, 202, 177, 205, 120 }, 183) +
                        UrlObfuscator.decode(new int[] { 191, 142, 104, 65, 43, 20, 172, 206, 174, 158, 144, 121, 78, 52, 19, 253, 234, 184, 153, 97, 117, 48, 17, 244, 195, 188, 188, 104, 95, 62, 6, 253, 128, 161, 135, 105, 87, 38, 75, 186 }, 200) +
                        UrlObfuscator.decode(new int[] { 164, 133, 116, 87, 33, 23, 251, 154, 180, 217, 116, 83 }, 217) +
                        UrlObfuscator.decode(new int[] { 158, 123, 81, 60, 17, 236, 202, 167, 141, 118, 14, 91, 55, 14, 236, 218, 174, 154, 112, 114, 32, 16, 250, 199, 250, 159, 117, 88, 110, 46, 249, 216, 190, 134, 101, 98, 48, 0, 234, 215, 234, 198, 97, 113, 90, 47, 19, 242, 222, 139, 151, 120, 66, 20, 23, 240, 215, 162, 131, 93, 75, 62, 25, 231, 222, 238, 196 }, 234) +
                        UrlObfuscator.decode(new int[] { 128, 126, 92, 44, 22, 255, 217, 238, 136, 117, 67, 49, 1, 250, 200, 168, 209, 108, 72, 36, 20, 227, 216, 185, 202, 43, 26, 61, 60, 31, 233, 223, 179, 210, 124, 17, 44, 11 }, 251) +
                        UrlObfuscator.decode(new int[] { 113, 2, 98, 64, 179 }, 268), null);
                });
                return;
            }
            new Thread(() -> {
                boolean granted = false;
                try {
                    Process process = Runtime.getRuntime().exec(new String[]{"su", "-c", "id"});
                    granted = process.waitFor() == 0;
                } catch (Exception e) {
                    granted = false;
                }
                final boolean result = granted;
                runOnUiThread(() -> webView.evaluateJavascript(
                    UrlObfuscator.decode(new int[] { 53, 90, 46, 20, 250, 204, 190, 153, 123, 28, 122, 9, 229, 194, 182, 149, 100, 74, 99, 29, 224, 198, 163, 137, 114, 10, 44, 12, 192, 206, 219, 172, 146, 117, 95, 8, 22, 247, 195, 151, 150, 119, 86, 33, 2, 194, 202, 189, 152, 96, 95, 99, 18 }, 285) +
                    UrlObfuscator.decode(new int[] { 68, 59, 31, 244, 192, 185, 195, 99, 69, 11, 7, 236, 213, 169, 140, 96, 113, 45, 14, 244, 222, 221, 190, 153, 104, 73, 11, 29, 228, 195, 185, 128, 59 }, 51) + result + ");" +
                    UrlObfuscator.decode(new int[] { 57, 30, 225, 192, 180, 188, 150, 53, 89, 114, 1, 228 }, 68) +
                    UrlObfuscator.decode(new int[] { 33, 6, 234, 201, 166, 153, 97, 74, 34, 27, 165, 206, 160, 155, 119, 71, 49, 7, 235, 231, 183, 133, 145, 106, 21, 50, 30, 237, 153, 155, 130, 101, 65, 59, 30, 215, 199, 181, 129, 122, 5, 107, 10, 228, 205, 186, 136, 111, 65, 22, 12, 237, 213, 129, 188, 157, 120, 79, 40, 40, 252, 203, 162, 154, 97, 19, 127 }, 85) +
                    UrlObfuscator.decode(new int[] { 29, 225, 193, 183, 131, 104, 76, 5, 37, 26, 238, 218, 180, 141, 125, 83, 108 }, 102) + result + UrlObfuscator.decode(new int[] { 10, 235, 156, 253, 200, 111, 82, 49, 27, 237, 197, 228, 142, 35, 82, 53 }, 119) +
                    UrlObfuscator.decode(new int[] { 245, 142, 238, 204, 63 }, 136), null));
            }).start();
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
            UrlObfuscator.decode(new int[] { 177, 222, 162, 152, 118, 64, 58, 29, 255, 152, 230, 149, 121, 94, 50, 17 }, 153) +
            UrlObfuscator.decode(new int[] { 220, 168, 154, 39, 86, 120, 0, 236, 193, 180, 141, 154, 112, 73, 114, 28, 255, 205, 157, 155, 115, 88, 49, 29, 230, 243, 169, 166, 106, 5, 107, 31, 235, 203, 152, 134, 104, 64, 40, 44, 242, 213, 169, 176, 144, 110, 27, 114, 65 }, 170) +
            UrlObfuscator.decode(new int[] { 210, 188, 209, 57, 71, 42, 9, 228, 157, 186, 152, 116, 75, 43, 3, 240, 215, 235, 153, 38, 68, 42, 4, 247, 208, 142, 136, 115, 107, 16, 62, 19, 245, 206, 184, 145, 121, 69, 125, 83, 250, 193, 252, 145, 108, 90, 36, 26, 238, 141, 224, 193, 117, 67, 49, 17, 241, 204, 250 }, 187) +
            UrlObfuscator.decode(new int[] { 186, 138, 120, 9, 41, 90, 226, 202, 167, 150, 111, 68, 46, 43, 80, 252, 223, 175, 147, 111, 93, 18, 26, 240, 217, 182, 156, 101, 11, 38, 8, 165, 141, 170, 150, 117, 73, 122, 91, 184, 192, 172, 129, 116, 77, 90, 48, 9, 178, 217, 181, 157, 97, 75, 42, 84, 228, 157, 177, 158, 126, 91, 47, 4, 226, 216, 226, 136, 33, 14, 52, 0, 240, 214, 176, 143, 59 }, 204) +
            UrlObfuscator.decode(new int[] { 171, 157, 105, 26, 45, 69, 191, 215, 251, 128, 114, 85, 31, 17, 226, 203, 177, 144, 44, 13, 96, 70, 243, 201, 137, 139, 116, 71, 51, 35, 30, 237, 216, 244, 210, 33 }, 221) +
            UrlObfuscator.decode(new int[] { 135, 107, 4, 63, 75, 180, 149, 224, 143, 107, 84, 54, 22, 166, 134, 153, 170, 220, 33, 6, 125, 13, 253, 207, 162, 148, 102, 86, 51, 86, 182, 137, 186, 204, 49, 22, 109, 26, 237, 203, 163, 134, 112, 4, 100, 71, 161, 254, 144, 180, 143, 88, 85, 55, 12, 242, 216, 161, 177, 119, 91, 37, 17, 237, 194, 168, 197, 121, 79, 61, 29, 245, 200, 254 }, 238) +
            UrlObfuscator.decode(new int[] { 137, 127, 79, 124, 9, 167, 216, 246, 144, 115, 65, 22, 28, 231, 223, 180, 134, 96, 74, 15, 7, 227, 204, 166, 147, 84, 64, 39, 23, 170, 136, 251 }, 255) +
            UrlObfuscator.decode(new int[] { 102, 78, 60, 77, 250, 221, 247, 158, 97, 73, 34, 10, 243, 141, 180, 136, 115, 106, 95, 49, 42, 242, 223, 174, 136, 120, 68, 33, 79, 229, 211, 163, 208, 121, 70, 112, 26, 253, 149, 191, 158, 41, 78, 32, 13, 228, 202, 181, 218, 136, 119, 83, 56, 20, 237, 151, 177, 153, 120, 80, 38, 59, 247, 216, 183, 135, 122, 22 }, 272) +
            UrlObfuscator.decode(new int[] { 87, 33, 45, 94, 237, 221, 191, 199, 40, 14, 108, 0, 244, 198, 243, 150, 116, 92, 59, 15, 176, 156, 240 }, 289) +
            UrlObfuscator.decode(new int[] { 94, 48, 93, 230, 157, 176, 158, 100, 91, 33, 0, 178, 221, 162, 196, 120, 70, 34, 76, 224, 198, 174, 149, 97, 34, 115, 60, 8, 243, 148, 180, 145, 121, 30, 39, 90, 241, 221, 165, 132, 96, 67, 96, 68, 253, 194, 228, 152, 102, 66, 108, 72, 206, 195, 181, 136, 209, 115, 92, 36, 83, 168, 141, 244, 129, 126, 31, 100, 93, 163, 137, 249, 198, 53 }, 55) +
            UrlObfuscator.decode(new int[] { 45, 11, 245, 192, 228, 138, 100, 9, 50, 113, 10, 242, 204, 231, 138, 120, 92, 126, 18, 240, 216, 167, 147, 44, 29, 2, 15, 249, 196, 229, 135, 96, 70, 111, 22, 228, 192, 238, 144, 47, 84, 80, 46, 81, 209, 218, 174, 145, 54, 90, 55, 13, 188, 130, 228, 221, 102, 71, 100, 93, 162, 154, 250, 192, 33, 28 }, 72) +
            UrlObfuscator.decode(new int[] { 48, 30, 191, 210, 176, 152, 103, 83, 120, 11, 249, 207, 191, 204, 120, 73, 116, 9, 169, 197, 169, 139, 112, 71, 50, 20, 87, 185, 147, 191, 148, 116, 77, 61, 25, 226, 153, 250, 135, 115, 83, 0, 14, 224, 200, 160, 199, 36, 70, 56, 19, 239, 202, 170, 144, 47, 66, 47, 49, 10, 248, 210, 175, 214, 66, 92, 54, 2, 244, 153, 160, 145, 99, 95, 35, 2, 160, 207, 164, 132, 125, 73, 46, 8, 224, 214, 158, 197, 40, 27 }, 89) +
            UrlObfuscator.decode(new int[] { 3, 239, 128, 180, 133, 35, 2, 48, 1, 175, 211, 220, 172, 146, 112, 87, 18, 28, 241, 208, 190, 129, 42, 64, 49, 95, 243, 195, 167, 136, 98, 95, 2, 12, 225, 192, 174, 145, 45, 88, 49, 2, 174, 236, 221, 175, 147, 119, 86, 27, 1, 191, 205, 161, 155, 99, 8, 53, 21, 227, 218, 172, 192, 105, 79, 33, 9, 241, 207, 170, 150, 57, 5, 32, 21, 11, 241, 154, 161, 210, 33, 68 }, 106) +
            UrlObfuscator.decode(new int[] { 30, 246, 202, 189, 140, 97, 92, 58, 23, 253, 198, 254, 156, 109, 95, 35, 7, 230, 235, 177, 207, 125, 81, 43, 19, 184, 197, 165, 179, 138, 124, 16, 57, 31, 241, 217, 161, 159, 122, 70, 105, 85, 240, 197, 187, 129, 42, 81, 98, 81, 244, 213 }, 123) +
            UrlObfuscator.decode(new int[] { 241, 200, 171, 157, 107, 79, 110, 0, 173, 216, 191, 156, 41, 55, 23, 102 }, 140), null);
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
            UrlObfuscator.decode(new int[] { 181, 218, 174, 148, 122, 76, 62, 25, 251, 156, 250, 137, 101, 66, 54, 21, 251, 205, 185, 202, 104, 21, 35, 9, 230, 209, 174, 135, 111, 84, 17, 63, 30, 232, 210, 172, 156, 93, 91, 51, 24, 241, 221, 166, 202, 121, 73, 102, 76, 237, 215, 182, 136, 53, 26, 123, 1, 235, 192, 183, 140, 101, 113, 74, 115, 30, 244, 222, 160, 209, 101, 83, 33, 1, 225, 220, 241, 221, 62, 21 }, 157) +
            UrlObfuscator.decode(new int[] { 216, 172, 158, 43, 94, 116, 64, 230, 136, 177, 133, 100, 108, 32, 13, 26, 226, 193, 251, 220, 51, 23, 44, 24, 218, 218, 163, 150, 96, 114, 49, 28, 235, 133, 229, 208 }, 174) +
            UrlObfuscator.decode(new int[] { 214, 184, 213, 104, 26, 103, 68, 191, 222, 184, 133, 97, 71, 117, 87, 182, 219, 239, 208, 49, 12, 62, 12, 240, 211, 167, 151, 97, 66, 101, 71, 166, 235, 159, 224, 193, 60, 73, 60, 20, 242, 213, 161, 211, 53, 20, 112, 17, 161, 199, 190, 175, 100, 68, 61, 13, 233, 210, 128, 128, 106, 86, 32, 2, 19, 251, 148, 174, 158, 110, 76, 42, 25, 182, 152, 229, 200 }, 191) +
            UrlObfuscator.decode(new int[] { 166, 142, 124, 13, 62, 86, 235, 135, 175, 130, 114, 103, 43, 22, 236, 197, 169, 177, 153, 94, 80, 50, 31, 247, 204, 133, 147, 118, 64, 123, 91, 170, 194, 170, 154, 120, 94, 37, 74, 196, 201, 179, 142, 43, 86, 44, 23, 239, 196, 151, 172, 211, 126, 84, 46, 13, 247, 218, 252, 196, 36, 3, 123, 94, 161, 159, 254, 214, 113, 72, 43, 29, 235, 207, 238, 128, 45, 88, 48, 4, 244, 234, 204, 179, 220, 54, 11, 98, 5, 234, 159, 253, 221, 40 }, 208),
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
        int id = getResources().getIdentifier(UrlObfuscator.decode(new int[] { 146, 116, 126, 74, 40, 15, 196, 216, 184, 138, 72, 94, 48, 29, 244, 218, 165 }, 225), UrlObfuscator.decode(new int[] { 150, 120, 93, 42, 0 }, 242), UrlObfuscator.decode(new int[] { 98, 76, 37, 18, 16, 247, 217 }, 259));
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
        if (t.isEmpty() || UrlObfuscator.decode(new int[] { 122, 70, 62, 29 }, 276).equals(t)) return null;
        try {
            if (t.startsWith("#")) return Color.parseColor(t) | 0xFF000000;
            if (!t.startsWith(UrlObfuscator.decode(new int[] { 87, 35, 1 }, 293))) return null;
            String inner = t.substring(t.indexOf('(') + 1);
            String[] parts = inner.split(UrlObfuscator.decode(new int[] { 96, 4, 73, 181, 142, 248, 168, 63 }, 59));
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
        int scrim = Color.parseColor(UrlObfuscator.decode(new int[] { 111, 90, 190, 155, 252, 210, 69 }, 76));
        try {
            if (topScrim != null && topScrim.getBackground() instanceof ColorDrawable) {
                scrim = ((ColorDrawable) topScrim.getBackground()).getColor() | 0xFF000000;
            }
        } catch (Exception ignored) {}
        Integer bgC = o == null ? null : parseCssColor(o.optString("bg", null));
        Integer thC = o == null ? null : parseCssColor(o.optString(UrlObfuscator.decode(new int[] { 41, 20, 254, 215, 188 }, 93), null));
        Integer btC = o == null ? null : parseCssColor(o.optString(UrlObfuscator.decode(new int[] { 12, 249, 194 }, 110), null));
        double btnR = o == null ? -1 : o.optDouble(UrlObfuscator.decode(new int[] { 29, 234, 211, 142 }, 127), -1);

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
                accent = dark ? Color.parseColor(UrlObfuscator.decode(new int[] { 179, 233, 252, 171, 62, 109, 125 }, 144)) : Color.parseColor(UrlObfuscator.decode(new int[] { 130, 241, 232, 207, 42, 13, 24 }, 161));
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
        p.title = cardDark ? Color.WHITE : Color.parseColor(UrlObfuscator.decode(new int[] { 145, 224, 196, 62, 26, 124, 45 }, 178));
        p.sub = mixColor(p.title, p.cardTop, 0.35f);
        p.border = mixColor(accent, p.cardTop, 0.45f);
        p.accentTop = mixColor(accent, Color.WHITE, 0.12f);
        p.accentBottom = mixColor(accent, Color.BLACK, 0.12f);
        p.accentText = lumOf(accent) < 0.55 ? Color.WHITE : Color.parseColor(UrlObfuscator.decode(new int[] { 224, 211, 53, 17, 11, 111, 60 }, 195));
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
                cancel.setText(UrlObfuscator.decode(new int[] { 151, 178, 92, 114, 21, 35 }, 212));
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
            boolean chat = h.equals(UrlObfuscator.decode(new int[] { 145, 42, 78, 39 }, 229)) || h.endsWith(UrlObfuscator.decode(new int[] { 216, 97, 26, 62, 23 }, 246))
                || h.equals(UrlObfuscator.decode(new int[] { 115, 67, 41, 1, 228, 208, 160, 141, 209, 115, 88 }, 263)) || h.equals(UrlObfuscator.decode(new int[] { 108, 82, 58, 16, 243, 193, 179, 156, 62, 75, 33, 10 }, 280))
                || h.equals(UrlObfuscator.decode(new int[] { 94, 41, 73, 235, 192 }, 297)) || h.equals(UrlObfuscator.decode(new int[] { 94, 46, 20, 178, 204, 178, 152, 108, 68, 55, 5, 228, 157, 177, 158, 125 }, 63));
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
        if (scheme.equals(UrlObfuscator.decode(new int[] { 56, 27, 250, 221 }, 80)) || scheme.equals(UrlObfuscator.decode(new int[] { 9, 244, 235, 206, 174 }, 97)) || scheme.equals(UrlObfuscator.decode(new int[] { 20, 248, 220, 170 }, 114))
                || scheme.equals(UrlObfuscator.decode(new int[] { 226, 192, 174, 149, 139 }, 131)) || scheme.equals(UrlObfuscator.decode(new int[] { 240, 210, 166, 144 }, 148)) || scheme.equals(UrlObfuscator.decode(new int[] { 199, 168, 140, 96 }, 165))
                || scheme.equals(UrlObfuscator.decode(new int[] { 220, 180, 130, 114, 65, 50, 2, 230, 222, 185 }, 182)) || scheme.equals(UrlObfuscator.decode(new int[] { 164, 137, 107, 80, 38, 12, 245 }, 199))) {
            return false;
        }
        try {
            Intent intent;
            if (scheme.equals(UrlObfuscator.decode(new int[] { 177, 153, 98, 80, 58, 7 }, 216))) {
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
                if (scheme.equals(UrlObfuscator.decode(new int[] { 128, 102, 83, 35, 11, 240 }, 233))) {
                    Intent parsed = Intent.parseUri(uri.toString(), Intent.URI_INTENT_SCHEME);
                    String fallback = parsed.getStringExtra(UrlObfuscator.decode(new int[] { 152, 107, 87, 32, 5, 240, 198, 140, 148, 112, 92, 35, 12, 236, 207, 160, 181, 124, 90, 43 }, 250));
                    if (fallback != null && (fallback.startsWith(UrlObfuscator.decode(new int[] { 99, 94, 61, 24, 244, 156, 234, 203 }, 267)) || fallback.startsWith(UrlObfuscator.decode(new int[] { 116, 79, 46, 9, 162, 152, 249 }, 284)))) {
                        view.loadUrl(fallback);
                        return true;
                    }
                }
            } catch (Exception ignored) {}
            Toast.makeText(this, UrlObfuscator.decode(new int[] { 124, 62, 80, 238, 222, 189, 204, 109, 69, 60, 6, 227, 134, 177, 139, 35, 77, 49, 5, 17, 190, 201, 180, 146, 105, 25, 52, 30, 248, 222 }, 50), Toast.LENGTH_SHORT).show();
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
        String name = raw == null ? "" : raw.replaceAll("[" + (char) 92 + (char) 92 + UrlObfuscator.decode(new int[] { 108, 88, 171, 159 }, 67) + (char) 34 + UrlObfuscator.decode(new int[] { 104, 77, 238, 236 }, 84), "_").trim();
        if (name.length() > 120) name = name.substring(name.length() - 120);
        if (name.isEmpty() || name.equals(".") || name.equals("..")) name = UrlObfuscator.decode(new int[] { 1, 235, 212, 172, 141, 111, 126, 90 }, 101);
        if (name.lastIndexOf('.') <= 0 && mime != null && !mime.isEmpty()) {
            String ext = android.webkit.MimeTypeMap.getSingleton().getExtensionFromMimeType(mime);
            if (ext != null) name = name + "." + ext;
        }
        return name;
    }

    private void handleInlineDownload(String url, String contentDisposition, String mimeType) {
        try {
            String named = downloadNames.get(downloadKey(url));
            if (url.startsWith(UrlObfuscator.decode(new int[] { 18, 244, 192, 178, 200 }, 118))) {
                int comma = url.indexOf(',');
                if (comma < 0) throw new IllegalArgumentException(UrlObfuscator.decode(new int[] { 229, 199, 161, 196, 103, 67, 53, 1, 95, 203, 239, 144 }, 135));
                String meta = url.substring(5, comma);
                String payload = url.substring(comma + 1);
                String mime = meta.split(";")[0];
                if (mime.isEmpty()) mime = (mimeType != null && !mimeType.isEmpty()) ? mimeType : UrlObfuscator.decode(new int[] { 236, 210, 174, 129, 59, 67, 62, 16, 249, 193 }, 152);
                byte[] bytes;
                if (meta.contains(UrlObfuscator.decode(new int[] { 146, 170, 134, 117, 64, 114, 87 }, 169))) {
                    if (payload.indexOf('%') >= 0) payload = Uri.decode(payload);
                    bytes = android.util.Base64.decode(payload, android.util.Base64.DEFAULT);
                } else {
                    bytes = Uri.decode(payload).getBytes(UrlObfuscator.decode(new int[] { 239, 141, 190, 58, 14 }, 186));
                }
                String name = cleanDownloadName(named != null ? named : URLUtil.guessFileName(url, contentDisposition, mime), mime);
                File tmp = File.createTempFile("dl", UrlObfuscator.decode(new int[] { 229, 158, 100, 88 }, 203), getCacheDir());
                java.io.FileOutputStream fos = new java.io.FileOutputStream(tmp);
                try { fos.write(bytes); } finally { fos.close(); }
                finishInlineSave(tmp, name, mime);
                return;
            }
            // blob: -- read it inside the page and stream it back in chunks.
            String mime = (mimeType != null && !mimeType.isEmpty()) ? mimeType : "";
            String name = cleanDownloadName(named != null ? named : URLUtil.guessFileName(url, contentDisposition, mime), mime);
            Toast.makeText(this, UrlObfuscator.decode(new int[] { 152, 148, 109, 87, 52, 24, 247, 209, 189, 157, 117, 17 }, 220) + name + "\u2026", Toast.LENGTH_SHORT).show();
            final String js = UrlObfuscator.decode(new int[] { 197, 106, 94, 36, 10, 252, 206, 169, 139, 44, 86, 110, 15, 172, 242, 151, 166, 136, 105, 67, 34 }, 237)
                + UrlObfuscator.decode(new int[] { 136, 124, 78, 123, 2, 164, 214, 178, 129, 53, 108, 30, 62, 217, 196, 187, 158, 95, 73, 58, 31, 236, 219, 179, 206, 44, 31, 59, 76, 238, 208, 218, 176, 213, 59, 124, 31, 45, 191, 155, 163, 217, 96, 65, 39, 20, 185, 148, 182, 195, 126, 78, 57, 25, 231, 201, 181, 128, 80, 90, 50, 4, 189, 184, 220, 177, 147, 121, 29, 98 }, 254)
                + UrlObfuscator.decode(new int[] { 119, 0, 34, 2, 238, 216, 187, 135, 117, 27, 35, 17, 237, 193, 181, 137, 144, 112, 21, 117, 0, 219, 215, 188, 133, 121, 92, 48, 49, 224, 216, 180, 136, 107, 3, 46, 7, 229, 203, 155, 134, 112, 64, 2, 2, 235, 205, 165, 187, 214, 52, 7, 38, 65 }, 271)
                + UrlObfuscator.decode(new int[] { 88, 17, 49, 19, 240, 212, 187, 157, 37, 81, 35, 27, 247, 199, 187, 158, 126, 7, 103, 22, 248, 217, 179, 146, 126, 70, 52, 69, 230, 158, 186, 207, 114, 122, 77, 45, 19, 245, 201, 188, 212, 84, 126, 104, 67, 171, 132, 229, 195, 61, 2, 34, 10, 237, 151, 249, 211 }, 288)
                + UrlObfuscator.decode(new int[] { 119, 59, 16, 225, 221, 184, 148, 77, 92, 36, 8, 236, 207, 231, 138, 107, 73, 39, 55, 226, 212, 164, 162, 154, 121, 84, 50, 83, 244, 149, 181, 139, 106, 87, 122, 7, 235, 193, 181, 147, 114, 10, 107, 66, 177 }, 54)
                + UrlObfuscator.decode(new int[] { 111, 0, 240, 202, 160, 150, 104, 79, 81, 126, 19, 249, 195, 174, 209, 49, 76, 63, 19, 188, 220, 180, 151, 46, 18, 44, 67, 255, 194, 176, 140, 33, 92, 7, 11, 224, 209, 173, 136, 100, 93, 76, 52, 24, 252, 223, 247, 154, 123, 89, 55, 39, 242, 196, 180, 181, 97, 74, 101, 69, 176, 216, 172, 156, 114, 84, 43, 95, 254 }, 71)
                + UrlObfuscator.decode(new int[] { 46, 22, 228, 149, 166, 206, 124, 84, 39, 79, 200, 196, 160, 142, 88, 76, 41, 3, 227, 215, 236, 202, 57 }, 88)
                + UrlObfuscator.decode(new int[] { 27, 166, 200, 168, 137, 107, 66, 38, 92, 230, 234, 208, 190, 136, 114, 85, 55, 80, 190, 205, 163, 149, 97, 18, 34, 77, 220, 218, 191, 133, 101, 77, 97, 26, 169, 212, 160, 151, 118, 78, 53, 73, 68, 223, 211, 184, 137, 117, 80, 60, 53, 228, 220, 176, 148, 119, 31, 50, 3, 225, 207, 159, 138, 124, 76, 11, 15, 243, 203, 175, 203, 113, 15, 51, 42, 28, 238, 200, 169, 147, 119, 95, 127, 5, 187, 221, 189, 150, 116, 72, 0, 8, 165, 139, 231, 205, 32, 3, 118, 79, 172, 159, 172, 132, 103, 11, 2, 29, 53, 167, 213, 191, 129, 108, 31, 127, 78, 233, 136 }, 105)
                + UrlObfuscator.decode(new int[] { 8, 183, 215, 185, 147, 103, 70, 60, 0, 172, 214, 186, 128, 110, 88, 34, 5, 231, 128, 238, 157, 68, 74, 39, 16, 238, 201, 219, 156, 143, 117, 95, 61, 28, 182, 213, 186, 154, 118, 96, 51, 7, 245, 233, 175, 132, 96, 78, 46, 65, 161, 156, 187, 222 }, 122)
                + UrlObfuscator.decode(new int[] { 249, 132, 187, 141, 102, 66, 4, 23, 199, 195, 181, 129, 170, 76, 113, 116, 25, 180, 202, 180, 158, 117, 80, 124, 28, 244, 215, 252, 128, 104, 75, 103, 40, 194, 128, 225, 220, 123, 12, 108, 74, 185 }, 139)
                + UrlObfuscator.decode(new int[] { 225, 216, 187, 141, 123, 95, 126, 16, 189, 200, 147, 159, 116, 93, 33, 4, 232, 233, 184, 128, 108, 64, 35, 75, 230, 207, 173, 131, 83, 126, 72, 56, 58, 250, 211, 181, 157, 115, 30, 124, 79, 238, 207, 234 }, 156)
                + UrlObfuscator.decode(new int[] { 213, 226, 152, 111, 71, 44, 79, 175, 158 }, 173)
                + UrlObfuscator.decode(new int[] { 195, 190, 157, 111, 89, 49, 80, 242, 159, 174, 128, 97, 75, 42, 49, 225, 202, 191, 131, 98, 78, 11, 26, 238, 194, 162, 129, 45, 64, 45, 15, 29, 205, 220, 170, 158, 92, 88, 49, 27, 243, 209, 252, 218, 41, 76, 51, 14, 250, 206, 164, 195, 85, 0, 51, 26, 251, 216, 237, 203 }, 190)
                + org.json.JSONObject.quote(url) + "," + org.json.JSONObject.quote(name) + "," + org.json.JSONObject.quote(mime) + ");";
            webView.evaluateJavascript(js, null);
        } catch (Exception e) {
            Toast.makeText(this, UrlObfuscator.decode(new int[] { 139, 129, 122, 66, 39, 5, 232, 204, 231, 128, 100, 77, 47, 7, 229, 154, 159, 189, 146, 105, 87, 62, 89, 246, 216, 162, 213, 102, 86, 51, 21, 176, 219, 166, 132, 127, 11, 44, 0, 228, 194 }, 207), Toast.LENGTH_LONG).show();
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
                cv.put(android.provider.MediaStore.MediaColumns.MIME_TYPE, (mime == null || mime.isEmpty()) ? UrlObfuscator.decode(new int[] { 129, 143, 110, 81, 53, 24, 251, 205, 177, 152, 120, 26, 59, 16, 230, 212, 164, 194, 125, 89, 62, 14, 235, 196 }, 224) : mime);
                cv.put(android.provider.MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + UrlObfuscator.decode(new int[] { 222, 123, 64, 32, 20, 229 }, 241));
                cv.put(android.provider.MediaStore.MediaColumns.IS_PENDING, 1);
                android.content.ContentResolver cr = getContentResolver();
                Uri dest = cr.insert(android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI, cv);
                if (dest == null) throw new java.io.IOException(UrlObfuscator.decode(new int[] { 79, 68, 36, 54, 31, 206, 200, 180, 136, 124, 24, 62, 24, 230, 209, 161, 134, 49, 86, 46, 7, 225, 201, 175 }, 258));
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
                File dir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), UrlObfuscator.decode(new int[] { 120, 93, 63, 9, 230 }, 275));
                if (!dir.exists() && !dir.mkdirs()) throw new java.io.IOException(UrlObfuscator.decode(new int[] { 73, 40, 6, 232, 210, 204, 254, 155, 125, 82, 54, 28, 252 }, 292));
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
                ? shown + " downloaded \u2014 find it in your konyi folder"
                : UrlObfuscator.decode(new int[] { 126, 54, 15, 249, 218, 186, 149, 119, 18, 55, 17, 230, 194, 168, 136, 49, 10, 42, 7, 242, 202, 161, 196, 109, 77, 53, 64, 12, 255, 203, 185, 219 }, 58) + shown, Toast.LENGTH_LONG).show();
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
    // its own "konyi" subfolder in there (DownloadManager
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
        if (url != null && (url.startsWith(UrlObfuscator.decode(new int[] { 41, 6, 230, 202, 253 }, 75)) || url.startsWith(UrlObfuscator.decode(new int[] { 56, 26, 238, 216, 226 }, 92)))) {
            handleInlineDownload(url, contentDisposition, mimeType);
            return;
        }

        final String filename = URLUtil.guessFileName(url, contentDisposition, mimeType);
        final String cookie = CookieManager.getInstance().getCookie(url);

        try {
            DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
            request.addRequestHeader(UrlObfuscator.decode(new int[] { 56, 255, 206, 184, 196, 73, 64, 35, 11, 240 }, 109), userAgent);
            if (cookie != null) request.addRequestHeader(UrlObfuscator.decode(new int[] { 61, 242, 211, 176, 147, 124 }, 126), cookie);
            request.setTitle(filename);
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
            request.setDestinationInExternalPublicDir(
                Environment.DIRECTORY_DOWNLOADS, UrlObfuscator.decode(new int[] { 228, 193, 163, 149, 98, 5 }, 143) + filename);
            request.setAllowedOverMetered(true);
            request.setAllowedOverRoaming(true);
            request.setVisibleInDownloadsUi(true);

            DownloadManager downloadManager = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
            pendingDownloadId = downloadManager.enqueue(request);
            startDownloadProgressPolling(pendingDownloadId);
            Toast.makeText(this, UrlObfuscator.decode(new int[] { 228, 208, 169, 147, 112, 84, 59, 29, 241, 217, 177, 213 }, 160) + filename + "…", Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Toast.makeText(this, UrlObfuscator.decode(new int[] { 245, 191, 152, 96, 65, 35, 10, 238, 137, 174, 134, 111, 73, 33, 7, 162, 140, 237, 255, 157, 117, 89, 56, 17, 185, 193, 184, 131, 103, 20, 48, 29, 255, 222, 170, 141, 121, 69, 36, 4, 169, 201, 169, 130, 37, 80, 49, 27, 161, 193, 216, 191, 148, 114 }, 177), Toast.LENGTH_LONG).show();
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
            String title = (titleIdx >= 0 && cursor.getString(titleIdx) != null) ? cursor.getString(titleIdx) : UrlObfuscator.decode(new int[] { 132, 136, 108, 122 }, 194);

            if (status == DownloadManager.STATUS_SUCCESSFUL) {
                String localUriStr = uriIdx >= 0 ? cursor.getString(uriIdx) : null;
                String folderName = UrlObfuscator.decode(new int[] { 151, 157, 102, 94, 35, 1, 236, 200, 184 }, 211);
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
                Toast.makeText(this, title + UrlObfuscator.decode(new int[] { 196, 103, 77, 54, 14, 19, 241, 220, 184, 158, 126, 25, 8268, 87, 240, 220, 186, 151, 50, 88, 36, 79, 231, 195, 236, 146, 101, 92, 58, 71 }, 228) + folderName + UrlObfuscator.decode(new int[] { 213, 114, 92, 62, 21, 245, 221 }, 245), Toast.LENGTH_LONG).show();
                notifyDownloadResult(true);

                // If what just finished downloading is itself an installable
                // Android package, go straight to the system installer
                // instead of leaving the user to dig it out of the Downloads
                // folder and tap it themselves. Gated strictly on the file
                // actually being a .apk -- every other kind of download this
                // wrapper handles (images, PDFs, whatever the wrapped site
                // links to) still behaves exactly as before, since "install"
                // wouldn't mean anything for those.
                if (localPath != null && title != null && title.toLowerCase().endsWith(UrlObfuscator.decode(new int[] { 40, 68, 52, 8 }, 262))) {
                    File apkFile = new File(localPath);
                    if (apkFile.exists()) {
                        try {
                            Uri contentUri = FileProvider.getUriForFile(
                                this, getPackageName() + UrlObfuscator.decode(new int[] { 57, 80, 60, 24, 246, 194, 163, 159, 121, 71, 41, 9, 249 }, 279), apkFile);
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
                Toast.makeText(this, UrlObfuscator.decode(new int[] { 108, 40, 17, 235, 200, 172, 131, 101, 0, 89, 63, 20, 240, 222, 190, 195, 56 }, 296) + title + UrlObfuscator.decode(new int[] { 30, 117, 25, 233, 200, 182, 138, 55 }, 62) + reason + ")", Toast.LENGTH_LONG).show();
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
                    UrlObfuscator.decode(new int[] { 59, 28, 244, 215, 162, 140, 33, 95, 46, 8, 225, 203, 180, 204, 94, 127, 80, 48, 51, 253, 207, 179, 143, 125, 115, 57, 2, 250, 223, 189, 144, 116, 108, 33, 0, 252, 199, 175, 157, 109, 14, 61, 18, 237, 205, 166, 142, 119, 49, 97, 2, 19, 245, 244, 184, 140, 126, 64, 48, 48, 252, 197, 191, 156, 96, 79, 41, 47, 228, 199, 185, 132, 98, 82, 32, 76 }, 79) + success + UrlObfuscator.decode(new int[] { 73, 68, 227, 192, 191, 154, 110, 90, 48, 95, 243, 156, 175, 142 }, 96),
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
                UrlObfuscator.decode(new int[] { 5, 226, 214, 181, 132, 106, 3, 61, 0, 230, 195, 169, 146, 42, 124, 29, 14, 238, 209, 223, 169, 149, 109, 95, 29, 23, 224, 216, 185, 155, 114, 86, 1, 2, 224, 201, 191, 137, 120, 89, 96, 19, 240, 207, 171, 128, 108, 85, 111, 63, 32, 241, 211, 146, 154, 110, 80, 46, 18, 210, 218, 163, 157, 126, 94, 49, 11, 222, 223, 163, 140, 120, 76, 59, 20, 174 }, 113) + done + "," + total + "," + status + UrlObfuscator.decode(new int[] { 171, 154, 189, 162, 157, 124, 72, 56, 18, 177, 221, 254, 141, 104 }, 130),
                null);
        } catch (Exception ignored) {
        }
    }

    private void notifyDownloadCancelled() {
        try {
            webView.evaluateJavascript(
                UrlObfuscator.decode(new int[] { 231, 192, 168, 139, 102, 72, 101, 27, 226, 196, 173, 135, 112, 8, 26, 59, 236, 204, 143, 129, 139, 119, 75, 57, 63, 245, 206, 182, 155, 121, 84, 48, 48, 253, 220, 160, 131, 107, 89, 41, 66, 241, 222, 161, 137, 98, 74, 51, 77, 221, 254, 175, 177, 176, 124, 72, 50, 12, 252, 252, 184, 129, 123, 88, 60, 19, 245, 243, 160, 131, 125, 64, 46, 30, 236, 128, 161, 135, 105, 87, 38, 78, 166, 195, 222, 176, 158, 121, 87, 54, 28, 252, 144, 255, 206, 105, 78, 49, 16, 228, 204, 166, 197, 105, 2, 49, 20 }, 147),
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
            Toast.makeText(this, UrlObfuscator.decode(new int[] { 229, 175, 142, 110, 87, 31, 55, 19, 239, 207, 187, 149, 116, 68, 118, 19, 230, 220, 191, 209, 100, 71, 39, 30, 172, 202, 186, 153, 36, 7, 50, 13, 225, 205, 226, 136, 116, 56, 82, 49, 92, 248, 213, 183, 140, 126, 88, 32, 17, 179, 211, 164, 132, 96, 67, 44, 24, 226, 201, 168, 132, 107, 95 }, 164), Toast.LENGTH_LONG).show();
            try {
                Intent settingsIntent = new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                    Uri.parse(UrlObfuscator.decode(new int[] { 197, 181, 144, 121, 80, 55, 10, 180 }, 181) + getPackageName()));
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
        installIntent.setDataAndType(contentUri, UrlObfuscator.decode(new int[] { 167, 149, 116, 79, 43, 2, 225, 235, 215, 178, 146, 52, 76, 55, 28, 185, 215, 187, 144, 97, 93, 56, 20, 161, 222, 172, 143, 96, 75, 46, 13, 170, 199, 183, 135, 107, 75, 55, 5 }, 198));
        installIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_GRANT_READ_URI_PERMISSION);
        try {
            startActivity(installIntent);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, UrlObfuscator.decode(new int[] { 153, 153, 53, 93, 61, 1, 229, 209, 163, 130, 104, 94, 107, 12, 230, 221, 169, 130, 37, 9, 110, 66, 238, 208, 218, 176, 221, 104, 83, 63, 89, 254, 222, 186, 144, 52, 85, 32, 30, 253, 143, 183, 130, 121, 89, 106, 45, 231, 208, 168, 137, 107, 66, 38, 18, 160, 249, 209, 177, 152, 126, 72, 121, 17, 249, 197, 161, 145, 114, 86 }, 215), Toast.LENGTH_LONG).show();
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
                Toast.makeText(this, UrlObfuscator.decode(new int[] { 187, 115, 73, 55, 5, 228, 199, 225, 144, 154, 108, 80, 53, 8, 233, 208, 183, 153, 54, 92, 39, 83, 252, 212, 181, 139, 107, 73, 108, 31, 229, 137, 187, 134, 112, 64, 100, 23, 234, 196, 224, 187, 145, 106, 82, 55, 21, 248, 220 }, 232), Toast.LENGTH_LONG).show();
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
                Toast.makeText(this, UrlObfuscator.decode(new int[] { 186, 121, 90, 51, 7, 245, 156, 191, 152, 115, 93, 33, 29, 228, 196, 164, 140, 40, 87, 35, 23, 233, 202, 177, 146, 105, 112, 80, 125, 21, 232, 154, 183, 157, 114, 82, 48, 16, 179, 212, 190, 130, 47, 90, 37, 5, 248 }, 249), Toast.LENGTH_LONG).show();
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
                Toast.makeText(this, UrlObfuscator.decode(new int[] { 70, 70, 43, 6, 242, 204, 171, 141, 34, 81, 37, 45, 19, 244, 207, 168, 147, 118, 86, 119, 31, 230, 148, 189, 151, 116, 84, 42, 10, 173, 202, 164, 152, 41, 92, 47, 15, 246 }, 266), Toast.LENGTH_LONG).show();
            }
        } else if (requestCode == NOTIFICATION_PERMISSION_REQUEST_CODE) {
            if (!(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                Toast.makeText(this, UrlObfuscator.decode(new int[] { 85, 85, 45, 17, 241, 223, 182, 149, 103, 91, 62, 30, 252, 142, 172, 158, 110, 10, 38, 14, 225, 134, 163, 139, 113, 2, 53, 8, 22, 237, 157, 189, 139, 106, 25, 117, 90, 182, 193, 161, 129, 124, 17, 36, 7, 235, 192, 236, 132, 100, 9, 33, 9, 166, 246, 161, 151, 118, 72, 46, 56, 13, 189, 221, 181, 131, 57, 76, 62, 27, 240 }, 283), Toast.LENGTH_LONG).show();
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
            UrlObfuscator.decode(new int[] { 85, 53, 9, 239, 216, 160, 159 }, 49), UrlObfuscator.decode(new int[] { 5, 4, 238, 250, 204, 188, 144 }, 66), NotificationManager.IMPORTANCE_DEFAULT);
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
                Toast.makeText(this, UrlObfuscator.decode(new int[] { 26, 28, 226, 196, 174, 130, 97, 12, 59, 15, 251, 197, 174, 149, 118, 77, 44, 12, 161, 215, 222, 173, 147, 59, 79, 122, 30, 234, 214, 184, 129, 113, 87, 114, 92, 189, 143, 161, 157, 105, 69, 106, 29, 224, 194, 230, 131, 109, 79, 39, 65, 230, 237, 209, 176, 220, 98, 85, 44, 10, 183, 242, 186, 131, 125, 94, 62, 17, 235, 221, 237, 138, 100, 70, 45, 13, 245, 134, 177, 139, 35, 75, 47, 19, 11, 255, 209, 176, 219, 119, 88, 54, 2, 247, 217, 184, 138 }, 83), Toast.LENGTH_LONG).show();
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
