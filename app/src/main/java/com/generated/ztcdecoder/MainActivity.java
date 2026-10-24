package com.generated.ztcdecoder;

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
    private View leftScrim;
    private View rightScrim;
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
            int iconResId = context.getResources().getIdentifier(UrlObfuscator.decode(new int[] { 214, 186, 183, 161, 110, 76, 55, 27, 234, 208, 136, 154, 122, 83, 60 }, 161), UrlObfuscator.decode(new int[] { 214, 163, 145, 120, 79, 47, 0, 238 }, 178), context.getPackageName());
            boolean roundedAsset = iconResId != 0;
            if (iconResId == 0) iconResId = context.getResources().getIdentifier(UrlObfuscator.decode(new int[] { 170, 129, 94, 76, 94, 43, 19, 255, 211, 191, 139 }, 195), UrlObfuscator.decode(new int[] { 185, 154, 98, 92, 49, 31 }, 212), context.getPackageName());
            if (iconResId != 0) {
                icon.setImageResource(iconResId);
            }
            if (!roundedAsset && android.os.Build.VERSION.SDK_INT >= 21) {
                final int roundRadiusPx = Math.round(iconSize * 0.2237f);
                icon.setOutlineProvider(new android.view.ViewOutlineProvider() {
                    @Override
                    public void getOutline(View view, android.graphics.Outline outline) {
                        outline.setRoundRect(0, 0, view.getWidth(), view.getHeight(), roundRadiusPx);
                    }
                });
                icon.setClipToOutline(true);
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
            int iconResId = context.getResources().getIdentifier(UrlObfuscator.decode(new int[] { 146, 126, 75, 29, 18, 240, 243, 223, 174, 148, 68, 86, 54, 31, 248 }, 229), UrlObfuscator.decode(new int[] { 146, 103, 85, 36, 19, 243, 220, 170 }, 246), context.getPackageName());
            boolean roundedAsset = iconResId != 0;
            if (iconResId == 0) iconResId = context.getResources().getIdentifier(UrlObfuscator.decode(new int[] { 110, 69, 26, 8, 226, 215, 175, 131, 151, 123, 79 }, 263), UrlObfuscator.decode(new int[] { 117, 94, 38, 24, 245, 195 }, 280), context.getPackageName());
            if (iconResId != 0) {
                iv.setImageResource(iconResId);
            }
            if (!roundedAsset && android.os.Build.VERSION.SDK_INT >= 21) {
                final int roundRadiusPx = Math.round(iconSize * 0.2237f);
                iv.setOutlineProvider(new android.view.ViewOutlineProvider() {
                    @Override
                    public void getOutline(View view, android.graphics.Outline outline) {
                        outline.setRoundRect(0, 0, view.getWidth(), view.getHeight(), roundRadiusPx);
                    }
                });
                iv.setClipToOutline(true);
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
        // Landscape (locked or auto-rotated): hide the status/navigation
        // bars and draw into the camera-cutout area; a swipe from the edge
        // brings the bars back briefly. Portrait leaves them alone.
        applyImmersiveMode();
        if (Build.VERSION.SDK_INT >= 30) {
            getWindow().getAttributes().layoutInDisplayCutoutMode =
                android.view.WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS;
        } else if (Build.VERSION.SDK_INT >= 28) {
            getWindow().getAttributes().layoutInDisplayCutoutMode =
                android.view.WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
        }

        FrameLayout root = new FrameLayout(this);
        rootLayout = root;
        root.setBackgroundColor(Color.parseColor(UrlObfuscator.decode(new int[] { 10, 120, 82, 182, 144, 244, 214 }, 297)));

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
        // The default Material pull-to-refresh circle (white disc, shadow, colored
        // arc) is hidden on purpose: the gesture still reloads the page, but no
        // spinner is drawn for it. Child 0 is SwipeRefreshLayout's own circle
        // view (it is added in the constructor, before the WebView).
        if (swipeRefresh.getChildCount() > 0) {
            swipeRefresh.getChildAt(0).setAlpha(0f);
        }
        swipeRefresh.setColorSchemeColors(Color.parseColor(UrlObfuscator.decode(new int[] { 28, 108, 57, 170, 248, 158, 191 }, 63)));
        swipeRefresh.addView(webView, new ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        root.addView(swipeRefresh, webParams);

        // Status-bar and navigation-bar strips. Added before the splash /
        // error / navigation overlays so those still cover the whole screen.
        // Side strips (added first so the top and bottom strips draw over
        // them -- the status bar color then runs across the corners too).
        // Side strips: the camera cutout / nav bar area when the phone is
        // sideways. Painted with the page's own color at that edge so the
        // app looks full screen while content stays inside the safe zone.
        leftScrim = new View(this);
        leftScrim.setBackgroundColor(Color.parseColor(UrlObfuscator.decode(new int[] { 115, 95, 187, 157, 249, 219, 63 }, 80)));
        root.addView(leftScrim, new FrameLayout.LayoutParams(
            0, FrameLayout.LayoutParams.MATCH_PARENT, Gravity.LEFT));
        rightScrim = new View(this);
        rightScrim.setBackgroundColor(Color.parseColor(UrlObfuscator.decode(new int[] { 66, 176, 170, 142, 232, 204, 46 }, 97)));
        root.addView(rightScrim, new FrameLayout.LayoutParams(
            0, FrameLayout.LayoutParams.MATCH_PARENT, Gravity.RIGHT));
        topScrim = new View(this);
        topScrim.setBackgroundColor(Color.parseColor(UrlObfuscator.decode(new int[] { 81, 161, 133, 255, 219, 61, 25 }, 114)));
        root.addView(topScrim, new FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT, 0, Gravity.TOP));
        bottomScrim = new View(this);
        bottomScrim.setBackgroundColor(Color.parseColor(UrlObfuscator.decode(new int[] { 160, 146, 244, 208, 202, 46, 8 }, 131)));
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
            boolean edgeToEdge = fsView == null && shouldBeImmersive();
            applySafeArea(edgeToEdge ? 0 : bars.left, topInset, edgeToEdge ? 0 : bars.right, bars.bottom, ime.bottom);
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

        final SplashController loading = new CustomSplashView(this, Color.parseColor(UrlObfuscator.decode(new int[] { 183, 131, 226, 193, 32, 31, 126 }, 148)), false);
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
        final String offlineScrimColor = offlineIsNightMode ? UrlObfuscator.decode(new int[] { 134, 253, 218, 50, 17, 112, 111, 78, 173 }, 165) : UrlObfuscator.decode(new int[] { 149, 227, 194, 35, 2, 97, 64, 191, 158 }, 182);
        // Slightly translucent (not fully opaque) card/button fills, so a
        // hint of the dimmed scrim behind still shows through -- a cheap
        // stand-in for a true frosted-glass blur, which would need
        // RenderEffect (API 31+) and a snapshot of whatever's behind the
        // card to do for real.
        final String offlineCardBg = offlineIsNightMode ? UrlObfuscator.decode(new int[] { 228, 163, 61, 22, 0, 80, 196, 147, 143 }, 199) : UrlObfuscator.decode(new int[] { 251, 181, 46, 115, 102, 53, 160, 247, 226 }, 216);
        final String offlineTitleColor = offlineIsNightMode ? UrlObfuscator.decode(new int[] { 202, 78, 97, 0, 35, 194, 229 }, 233) : UrlObfuscator.decode(new int[] { 217, 41, 8, 103, 70, 165, 132 }, 250);
        final String offlineSubtitleColor = offlineIsNightMode ? UrlObfuscator.decode(new int[] { 40, 104, 113, 42, 191, 228, 129 }, 267) : UrlObfuscator.decode(new int[] { 63, 14, 24, 76, 218, 130, 147 }, 284);
        final String offlineButtonBg = offlineIsNightMode ? UrlObfuscator.decode(new int[] { 17, 105, 51, 187, 150, 249, 212, 63, 107 }, 50) : UrlObfuscator.decode(new int[] { 96, 90, 194, 228, 137, 154, 203, 88, 122 }, 67);
        final float offlineDensity = getResources().getDisplayMetrics().density;

        // Outer full-screen scrim so the card reads as a dialog sitting on
        // top of the app, matching how a native "no connection" alert
        // dims everything behind it.
        final FrameLayout errorView = new FrameLayout(this);
        errorView.setBackgroundColor(Color.parseColor(UrlObfuscator.decode(new int[] { 119, 67, 211, 129, 145, 223, 76 }, 84)));
        errorView.setVisibility(View.GONE);

        // The error screen is the branded HTML page in ERROR_PAGE_HTML,
        // rendered in its own small WebView that fills errorView. Its
        // Retry / Back buttons call back into native code through the
        // AndroidError interface (registered below, once doRetry exists).
        // Navigation inside it is blocked so it can only ever show that page.
        final WebView errorWeb = new WebView(this);
        errorWeb.setBackgroundColor(Color.parseColor(UrlObfuscator.decode(new int[] { 70, 180, 226, 242, 160, 48, 93 }, 101)));
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
        navOverlay.setBackgroundColor(Color.parseColor(UrlObfuscator.decode(new int[] { 85, 165, 132, 227, 194, 33, 0 }, 118)));
        navOverlay.setVisibility(View.GONE);
        final SkeletonView navSkeleton = new SkeletonView(this, Color.parseColor(UrlObfuscator.decode(new int[] { 164, 150, 245, 212, 51, 18, 113 }, 135)));
        navSkeleton.setTarget(webView);
        navSkeletonRef = navSkeleton;
        navOverlay.addView(navSkeleton, new FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
        final BouncingDotsView navDots = new BouncingDotsView(this, Color.parseColor(UrlObfuscator.decode(new int[] { 187, 241, 228, 179, 38, 118, 23 }, 152)));
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
                UrlObfuscator.decode(new int[] { 129, 174, 146, 104, 70, 48, 10, 237, 207, 232, 246, 133, 105, 78, 34, 1, 239, 217, 165, 214, 119, 9, 55, 29, 242, 197, 162, 139, 99, 88, 101, 8, 230, 204, 190, 221, 108, 66, 107, 67, 227, 137, 205, 187, 137, 105, 73, 52, 89, 236, 197, 163, 144, 47 }, 169)
                + UrlObfuscator.decode(new int[] { 211, 191, 208, 63, 84, 123, 29, 253, 220, 180, 130, 91, 75, 53, 24, 247, 214, 238, 207, 46, 8, 49, 22, 234, 207, 233, 201, 209, 114, 88, 50, 28, 238, 209, 230, 199, 63, 71, 49, 7, 231, 195, 190, 207, 104, 76, 32, 24, 239, 146 }, 186)
                + UrlObfuscator.decode(new int[] { 185, 143, 125, 93, 53, 8, 165, 198, 237, 147, 116, 69, 77, 39, 46, 249, 215, 191, 154, 108, 88, 36, 52, 248, 223, 250, 214, 121, 66, 41, 65, 250, 194, 174, 140, 103, 11, 37, 4, 234, 213, 163, 146, 44, 108, 72, 58, 80, 242, 220, 171, 153, 122, 83, 121, 29, 253, 194, 164, 132, 35, 76, 56, 24, 255, 197, 167, 207, 46, 8, 41, 1, 237, 197, 181, 136, 194, 35, 0, 108, 64, 231 }, 203)
                + UrlObfuscator.decode(new int[] { 191, 154, 110, 90, 48, 95, 243, 156, 175, 129, 119, 69, 37, 29, 224, 141, 170, 138, 102, 90, 45, 92, 251, 216, 237, 203, 43 }, 220),
                value -> {
                    if (UrlObfuscator.decode(new int[] { 153, 126, 94, 47 }, 237).equals(value)) revealNavOverlay.run();
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
        }, UrlObfuscator.decode(new int[] { 191, 115, 88, 41, 21, 240, 220, 146, 132, 103, 91, 33 }, 254));

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
        String spoofedUA = defaultUA.replace(UrlObfuscator.decode(new int[] { 52, 14, 58, 26 }, 271), "").replaceAll("Version/[0-9.]+\s", "");
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
        webView.addJavascriptInterface(new AndroidBridge(), UrlObfuscator.decode(new int[] { 97, 81, 58, 15, 243, 210, 190, 187, 106, 94, 50, 18, 241 }, 288));

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

        // First load only: the launch screen (icon on the site colour, or
        // the splash) stays up until the page has real content on screen,
        // instead of revealing a blank page with loading dots. Gives up
        // after 2.5 s and reveals anyway, so a page that stays empty on
        // purpose can never be held back.
        final boolean[] revealWaiting = { false };
        final long[] revealDeadline = { 0L };
        final Runnable[] revealPoll = new Runnable[1];
        revealPoll[0] = () -> {
            webView.evaluateJavascript(
                UrlObfuscator.decode(new int[] { 30, 51, 1, 253, 209, 165, 153, 96, 64, 101, 69, 240, 222, 187, 145, 124, 80, 36, 22, 163, 192, 252, 132, 144, 125, 72, 49, 30, 244, 205, 246, 149, 121, 81, 45, 72, 251, 215, 248, 206, 108, 4, 62, 14, 254, 220, 186, 137, 38, 21, 127 }, 54) +
                UrlObfuscator.decode(new int[] { 46, 0, 173, 140, 161, 204, 104, 78, 81, 59, 15, 200, 222, 162, 141, 100, 75, 113, 82, 189, 157, 166, 131, 121, 66, 102, 68, 162, 199, 175, 135, 111, 83, 46, 91, 180, 138, 176, 132, 116, 106, 76, 51, 92, 170, 129 }, 71) +
                UrlObfuscator.decode(new int[] { 46, 22, 228, 149, 165, 206, 112, 31, 33, 26, 235, 223, 181, 184, 111, 69, 45, 4, 242, 202, 182, 162, 110, 77, 104, 120, 23, 240, 219, 247, 140, 112, 92, 50, 25, 185, 215, 178, 156, 103, 81, 60, 66, 254, 218, 172, 198, 96, 78, 53, 7, 232, 193, 239, 139, 111, 80, 74, 42, 81, 254, 206, 174, 141, 119, 89, 113, 92, 175 }, 88) +
                UrlObfuscator.decode(new int[] { 15, 231, 213, 238, 147, 101, 81, 98, 8, 189, 175, 133, 180, 192, 106, 20, 53, 29, 249, 209, 161, 156, 53, 20, 56, 76, 187, 158, 246, 133, 32, 1, 96, 19, 241, 199, 183, 196, 113, 31, 48, 59, 22, 195, 147, 187, 158, 110, 123, 55, 2, 248, 209, 189, 157, 117, 114, 60, 6, 235, 195, 184, 185, 111, 74, 60, 79, 175, 158 }, 105) +
                UrlObfuscator.decode(new int[] { 19, 255, 144, 165, 216, 98, 93, 55, 6, 249, 142, 242, 214, 43, 10, 57, 68, 225, 205, 174, 129, 109, 80, 125, 95, 185, 137, 205, 187, 137, 105, 73, 52, 89, 169, 140, 171, 135, 113, 71, 39, 3, 254, 143, 254, 214, 113, 72, 43, 29, 235, 207, 238, 128, 45, 88, 48, 4, 244, 234, 204, 179, 220, 42, 1, 36, 5, 190, 158, 252 }, 122),
                value -> {
                    if ("1".equals(value) || System.currentTimeMillis() >= revealDeadline[0]) {
                        revealWaiting[0] = false;
                        revealWebView.run();
                    } else {
                        webView.postDelayed(revealPoll[0], 100);
                    }
                });
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
                if (hasLoadedOnce[0]) {
                    revealWebView.run();
                } else if (!revealWaiting[0]) {
                    revealWaiting[0] = true;
                    revealDeadline[0] = System.currentTimeMillis() + 2500L;
                    revealPoll[0].run();
                }
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
                        UrlObfuscator.decode(new int[] { 255, 216, 176, 147, 110, 64, 109, 19, 234, 204, 165, 143, 136, 48, 124, 50, 31, 232, 214, 177, 147, 84, 71, 61, 23, 245, 212, 246, 201, 79, 67, 40, 25, 229, 192, 172, 165, 116, 76, 32, 4, 231, 143, 179, 190, 136, 120, 111, 48, 31, 245, 221, 163, 153, 123, 18, 117, 83, 230, 217, 161, 138, 98, 91, 101, 53, 214, 219, 172, 131, 105, 102, 44, 23, 239, 196, 150, 165, 138, 117, 85, 62, 22, 239, 153, 137, 170, 103, 88, 55, 29, 210, 192, 187, 131, 104, 22, 62, 27, 253, 194, 253 }, 139) +
                        UrlObfuscator.decode(new int[] { 234, 218, 168, 217, 123, 86, 38, 72, 242, 198, 188, 146, 100, 70, 33, 3, 164, 130, 177, 157, 122, 94, 61 }, 156) +
                        UrlObfuscator.decode(new int[] { 196, 170, 195, 125, 64, 38, 3, 233, 210, 234, 144, 97, 83, 47, 51, 18, 196, 130, 237, 202, 48, 74, 50, 2, 224, 198, 189, 201 }, 173) +
                        UrlObfuscator.decode(new int[] { 200, 188, 142, 59, 109, 100, 15, 254, 216, 177, 155, 100, 28, 56, 30, 225, 203, 191, 187, 98, 78, 61, 0, 171, 238, 248, 147, 106, 76, 37, 15, 8, 176, 212, 178, 149, 127, 75, 16, 18, 255, 210, 188, 135, 41, 88, 54, 71, 175, 250, 176, 151, 43, 97, 97, 21, 227, 209, 177, 145, 108, 26 }, 190) +
                        UrlObfuscator.decode(new int[] { 185, 143, 127, 12, 36, 31, 253, 149, 156, 187, 62 }, 207) +
                        UrlObfuscator.decode(new int[] { 150, 158, 108, 29, 44, 14, 233, 209, 229, 145, 99, 91, 55, 7, 251, 222, 190, 199, 98, 1, 56, 71, 253, 133, 160, 203, 116, 68, 32, 74, 249, 206, 181, 171, 208, 109, 73, 40, 18, 177, 227, 252, 222, 121, 27, 4, 91, 191, 196, 160, 168, 100, 84, 46, 14, 161, 156, 238, 202, 46, 12, 55, 77, 201, 137, 145, 170, 146, 90, 82, 34, 28, 252, 159, 226, 220, 56, 24, 122, 6, 191, 248, 231, 195, 120, 68, 12, 0, 240, 194, 162, 205, 48, 10, 110, 74, 168, 247, 145, 149, 213, 53, 78, 54, 62, 254, 206, 176, 144, 59, 6, 120, 92, 164, 134, 191, 141, 111, 5, 30, 65, 169, 210, 170, 162, 106, 90, 36, 4, 87, 170, 148, 129, 210, 33, 68, 99 }, 224) +
                        UrlObfuscator.decode(new int[] { 135, 113, 93, 110, 5, 237, 216, 158, 140, 112, 83, 123, 3, 241, 205, 161, 149, 105, 112, 80, 117, 25, 247, 147, 162, 158, 120, 68, 125, 2, 242, 192, 241, 153, 50, 30, 118, 5, 183, 207, 165, 198, 100, 78, 44, 8, 231, 236, 174, 132, 154, 109, 19, 48, 30, 244, 222, 172, 159, 45, 92, 127, 88, 187, 202, 166, 142, 124, 13, 34, 86, 239, 197, 230, 132, 110, 76, 40, 7, 204, 206, 164, 186, 141, 70, 85, 6, 65, 240, 222, 255, 152, 59, 90, 60, 22, 244, 228, 182, 158, 104, 17, 118, 87, 186, 142, 225, 136, 43, 74, 44, 6, 228, 246, 222, 178, 136, 121, 21, 46, 11, 241, 218, 254, 220, 58, 95, 55, 31, 247, 219, 166, 211, 60, 2, 56, 12, 252, 210, 180, 139, 36, 87, 48, 20, 229, 164, 195, 175, 153, 111, 79, 43, 22, 183, 208, 180, 152, 96, 87, 106, 13, 180 }, 241) +
                        UrlObfuscator.decode(new int[] { 116, 64, 50, 127, 16, 242, 216, 190, 137, 36, 92, 56, 21, 224, 217, 182, 156, 101, 30, 62, 27, 232, 222, 178, 185, 108, 68, 34, 5, 241, 203, 177, 163, 109, 76, 23, 121, 20, 241, 220, 246, 143, 113, 83, 51, 26, 184, 208, 179, 159, 102, 78, 61, 65, 255, 221, 173, 197, 106, 82, 50, 17, 235, 205, 238, 136, 110, 111, 75, 41, 80, 232, 223, 181, 157, 116, 66, 121, 0, 246, 202, 165, 145, 125, 75, 44, 64, 208, 216, 166, 132, 98, 27, 39, 17, 247, 214, 174, 142, 162, 50, 85, 109, 87, 242, 139, 244, 159, 37, 25, 60, 71, 190, 193, 252, 131, 103, 1, 45, 71, 249, 217, 169, 137, 42, 73, 37, 1, 231, 205, 231, 246, 197 }, 258) +
                        UrlObfuscator.decode(new int[] { 117, 93, 35, 88, 249, 207, 191, 204, 98, 23, 121, 83, 238, 154, 171, 139, 103, 71, 50, 78, 19, 251, 211, 187, 143, 114, 31, 126, 24, 227, 193, 250, 159, 119, 95, 55, 27, 230, 145, 253, 217, 58, 18, 33, 76, 173, 140, 191 }, 275) +
                        UrlObfuscator.decode(new int[] { 82, 34, 16, 161, 197, 211, 227, 147, 115, 95, 63, 10, 195, 222, 139, 206, 98, 82, 32, 81, 226, 146, 171, 129, 34, 76, 47, 29, 202, 200, 179, 139, 96, 74, 44, 6, 195, 243, 215, 184, 146, 111, 104, 60, 27, 227, 158, 252, 207 }, 292) +
                        UrlObfuscator.decode(new int[] { 83, 63, 80, 229, 152, 162, 157, 119, 70, 57, 76, 189, 158, 177, 144, 121, 4, 33, 13, 238, 193, 173, 144, 63, 26, 61, 28, 13, 176, 223, 179, 143, 110, 86, 53, 75, 171, 133, 168, 143, 96, 31, 36, 0, 254, 147, 241, 163, 118, 85, 58, 73, 244, 204, 163, 139, 118, 29, 125, 111, 2, 225, 206, 245, 150, 124, 94, 35, 72, 168, 227, 250, 145, 126, 94, 59, 7, 227, 217, 174, 209 }, 58) +
                        UrlObfuscator.decode(new int[] { 34, 12, 161, 218, 233, 145, 108, 64, 55, 10, 171, 210, 145, 182, 152, 117, 92, 50, 13, 166, 135, 248, 192, 62, 100, 120, 57, 185, 204, 161, 131, 120, 66, 36, 28, 237, 156 }, 75) +
                        UrlObfuscator.decode(new int[] { 42, 26, 232, 153, 187, 132, 43, 82, 49, 7, 209, 222, 189, 159, 123, 89, 41, 15, 217, 221, 177, 139, 99, 13, 33, 15, 171, 154 }, 92) +
                        UrlObfuscator.decode(new int[] { 4, 234, 131, 169, 154, 38, 67, 47, 22, 244, 207, 163, 152, 61, 34, 3, 122, 18, 244, 212, 188, 223, 107, 74, 54, 7, 189, 196, 184, 131, 102, 76, 36, 0, 226, 222, 176, 213, 58, 27, 98, 12, 234, 198, 165, 133, 145, 57, 65, 32, 11, 251, 203, 171, 146, 80, 89, 59, 18, 230, 153, 179, 156, 32, 66, 60, 10, 233, 192, 188, 158, 122, 89, 99, 82, 165, 136, 252, 239, 208, 45, 9, 114, 25, 246, 214, 163, 159, 123, 65, 54, 73 }, 109) +
                        UrlObfuscator.decode(new int[] { 8, 252, 206, 251, 142, 120, 95, 106, 19, 249, 154, 167, 147, 118, 126, 46, 3, 232, 130, 191, 133, 69, 71, 48, 3, 247, 231, 162, 145, 100, 8, 22, 101 }, 126) +
                        UrlObfuscator.decode(new int[] { 249, 207, 191, 204, 98, 89, 29, 13, 255, 210, 248, 204, 119, 67, 38, 93, 66, 163, 154, 180, 202, 61, 69, 36, 3, 247, 210, 233, 206, 47, 22, 56, 93, 169, 209, 176, 159, 107, 78, 117, 90, 187, 130, 172, 208, 37, 93, 60, 43, 31, 250, 129, 230, 199, 62, 80, 99, 81, 233, 200, 167, 147, 118, 13, 114, 83, 170, 220, 236, 150, 117, 92, 38, 1, 184, 153, 254, 197, 109, 73, 24, 34, 1, 232, 218, 189, 196, 37, 10, 113, 20, 179, 207, 174, 133, 113, 72, 115, 80, 177, 140, 185, 153, 105, 73, 97, 25, 248, 215, 163, 134, 61, 34, 3, 122, 16, 250, 216, 188, 148, 48, 31, 110 }, 143) +
                        UrlObfuscator.decode(new int[] { 201, 217, 246, 148, 111, 111, 63, 1, 236, 158, 173 }, 160) +
                        UrlObfuscator.decode(new int[] { 216, 182, 199, 47, 69, 45, 24, 222, 204, 176, 147, 46, 64, 40, 74, 171, 194, 175, 177, 138, 116, 82, 46, 31, 162 }, 177) +
                        UrlObfuscator.decode(new int[] { 180, 128, 114, 63, 88, 46, 65, 235, 219, 171, 139, 114, 112, 57, 27, 242, 198, 249, 147, 124, 0, 43, 3, 229, 222, 154, 129, 125, 67, 108, 24, 255, 147, 247, 219, 137, 127, 79, 124, 23, 242, 132, 168, 150, 100, 70, 49, 53, 254, 222, 177, 155, 38, 78, 63, 69, 230, 192, 166, 130, 78, 64, 45, 4, 234, 213, 233, 228, 151, 123, 20, 122, 22, 241, 196, 171, 154, 125, 8, 53, 1, 184, 220, 167, 211, 107, 95, 97, 91, 167, 155, 252 }, 194) +
                        UrlObfuscator.decode(new int[] { 165, 147, 99, 16, 35, 7, 227, 201, 184, 215, 68, 73, 51, 14, 171, 201, 162, 154, 41, 17, 19, 19, 28, 232, 211, 244, 148, 113, 89, 126, 70, 184, 254, 179, 133, 120, 1, 60, 2, 249, 197, 174, 193, 122, 9, 46, 0, 237, 196, 170, 149, 47, 115, 86, 116, 85, 178, 129 }, 211) +
                        UrlObfuscator.decode(new int[] { 146, 98, 80, 97, 2, 23, 163, 240, 189, 143, 114, 23, 53, 22, 238, 157, 236, 223, 116, 66, 122, 95, 160, 149, 249, 194, 49, 95, 41, 21, 166, 201, 161, 133, 118, 28, 50, 113, 18, 248, 218, 175, 193, 111, 89, 37, 86, 226, 132, 238, 128, 63, 71, 38, 10, 249, 196, 240 }, 228) +
                        UrlObfuscator.decode(new int[] { 156, 114, 27, 62, 24, 254, 202, 189, 208, 49, 22, 123, 64, 243, 211, 180, 156, 127, 85, 35, 19, 160, 237, 217, 224, 152, 116, 89, 44, 21, 242, 216, 161, 218, 112, 64, 52, 17, 251, 203, 159, 141, 101, 77, 44, 64, 174, 157, 183, 131, 45, 81, 36, 12, 26, 253, 201, 146, 148, 126, 92, 27, 24, 248, 193, 177, 157, 102, 66, 120, 10, 226, 132, 247, 157, 107, 91, 104, 21, 244, 152, 182, 132, 44, 70, 37, 43, 60, 242, 201, 181, 158, 112, 86, 48, 53, 249, 221, 182, 156, 101, 98, 42, 13, 249, 132, 226, 209, 96, 78, 111, 20, 247, 138, 180, 139, 101, 84, 87, 96, 69, 181, 192, 182, 156, 126, 67, 107, 7, 230, 157, 190, 148, 118, 91, 117, 26, 188, 150, 135, 136, 124, 79, 104, 8, 237, 205, 234, 147, 46, 104, 87, 57, 8, 243, 150, 171, 138, 57, 65, 60, 16, 231, 218, 248, 203, 114, 83, 46, 13, 255, 201, 161, 192, 98, 15, 62, 25, 254 }, 245) +
                        UrlObfuscator.decode(new int[] { 96, 74, 54, 75, 244, 192, 178, 255, 148, 32, 12, 96, 16, 165, 212, 190, 152, 112, 71, 104, 24, 186, 155, 230, 149, 125, 89, 56, 2, 161, 196, 162, 128, 113, 8, 49, 76, 245, 207, 207, 245, 151, 54, 87, 50, 82, 176, 219, 190, 216, 118, 91, 123, 94, 162, 131, 230, 129, 101, 69, 47, 26, 182, 150, 224, 195, 110, 30, 127, 92, 236, 246, 208, 184, 143, 54, 11, 112, 71, 224, 134, 255, 196, 61, 4, 99, 74, 248, 158, 225, 142, 99, 6, 125, 65, 188, 219 }, 262) +
                        UrlObfuscator.decode(new int[] { 106, 83, 57, 7, 246, 201 }, 279) +
                        UrlObfuscator.decode(new int[] { 94, 38, 20, 165, 198, 177, 223, 113, 65, 77, 45, 24, 218, 215, 181, 152, 108, 31, 53, 6, 186, 209, 189, 131, 116, 74, 60, 57, 227, 219, 134, 140, 110, 83, 20, 4, 224, 202, 183, 146, 41, 99, 66, 109, 71 }, 296) +
                        UrlObfuscator.decode(new int[] { 87, 59, 84, 200, 206, 171, 145, 121, 81, 125, 23, 224, 156, 179, 159, 125, 74, 40, 30, 223, 197, 185, 164, 98, 64, 49, 54, 226, 198, 168, 149, 140, 55, 19, 53, 21, 254, 220, 160, 184, 112, 29, 115, 86, 181, 152, 238, 210, 62, 4, 46, 25, 183, 203, 186, 200, 55, 21, 116, 73, 207, 192, 180, 183, 208, 112, 85, 53, 82, 235, 150, 160, 159, 113, 64, 59, 94, 227, 158, 167, 139, 100, 75, 35, 30, 160, 147 }, 62) +
                        UrlObfuscator.decode(new int[] { 63, 27, 254, 196, 227, 152, 39, 68, 34, 0, 241, 136, 177, 204, 117, 79, 79, 114, 15, 178, 204, 179, 157, 108, 95, 122, 7, 186, 219, 183, 152, 119, 71, 58, 65, 238, 217, 227, 210 }, 79) +
                        "}}" +
                        UrlObfuscator.decode(new int[] { 9, 25, 182, 210, 169, 143, 52, 85, 61, 25, 241, 193, 188, 207, 38, 24, 34, 10, 250, 216, 190, 133, 49 }, 96) +
                        UrlObfuscator.decode(new int[] { 30, 229, 219, 224, 158, 99, 89, 62, 65, 238, 210, 168, 134, 112, 74, 45, 15, 168, 254, 146, 191, 213, 96, 72, 60, 12, 226, 196, 187, 212, 114, 105, 96, 45, 162, 204, 150, 221, 86, 17, 52, 65, 188 }, 113) +
                        UrlObfuscator.decode(new int[] { 235, 199, 232, 176, 139, 105, 18, 55, 31, 247, 223, 163, 158, 43, 12, 99, 91, 254, 197, 187, 192, 97, 73, 37, 13, 253, 192, 250, 222, 53, 31 }, 130) +
                        UrlObfuscator.decode(new int[] { 229, 211, 163, 208, 100, 19, 33, 3, 232, 203, 189, 129, 104, 72, 107, 11, 241, 203, 166, 137, 145, 53, 81, 51, 24, 251, 205, 177, 152, 120, 27, 36, 18, 230, 217, 190, 142, 99, 72, 119 }, 147) +
                        UrlObfuscator.decode(new int[] { 211, 171, 139, 109, 69, 23, 53, 83, 240, 222, 180, 158, 108, 95, 104, 77, 178, 149, 185, 223, 115, 71, 47, 31, 205, 223, 226, 130, 38, 75, 35, 11, 227, 215, 170, 204, 49, 54, 3, 96, 65, 188, 149, 254, 209, 124, 11, 62, 90, 224, 199, 179, 131, 123, 92, 36, 2, 236, 130, 249, 196, 108, 8, 41, 1, 237, 197, 181, 136, 210, 47, 20, 103 }, 164) +
                        UrlObfuscator.decode(new int[] { 244, 186, 151, 96, 94, 57, 11, 204, 223, 165, 143, 109, 76, 102, 20, 231, 211, 161, 176, 105, 68, 44, 58, 10, 242, 210, 243, 145, 53, 114, 4, 57, 219, 154, 160, 134, 99, 89, 33, 9, 228, 202, 178, 194, 102, 93, 51, 79, 172, 159 }, 181) +
                        UrlObfuscator.decode(new int[] { 187, 134, 101, 87, 33, 9, 168, 250, 151, 166, 129, 102, 1 }, 198) +
                        UrlObfuscator.decode(new int[] { 164, 147, 97, 96, 58, 31, 244, 223, 186, 154, 37, 79, 42, 26, 165, 145, 247, 214, 44, 31, 48, 7, 245, 244, 214, 179, 152, 115, 78, 46, 81, 251, 214, 166, 217, 38, 11, 98, 65, 185, 148 }, 215) +
                        UrlObfuscator.decode(new int[] { 149, 122, 69, 36, 16, 224, 202, 233, 133, 214, 101, 64 }, 232), null);
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
                    UrlObfuscator.decode(new int[] { 209, 126, 66, 56, 22, 224, 218, 189, 159, 56, 6, 53 }, 249) +
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
                    UrlObfuscator.decode(new int[] { 126, 91, 49, 28 }, 266) +
                    "var vp=document.querySelector('meta[name=\"viewport\"]');" +
                    UrlObfuscator.decode(new int[] { 114, 92, 113, 89, 225, 198, 252, 143, 101, 66, 108, 20, 224, 205, 184, 129, 110, 68, 61, 70, 228, 212, 160, 133, 119, 71, 4, 12, 26, 243, 216, 178, 143, 50, 30, 53, 18, 226, 212, 243, 218, 41, 71, 32, 65, 253, 200, 184, 170, 126, 93, 58, 14, 228, 208, 176, 134, 42, 6, 46, 62, 19, 248, 155, 247, 221, 111, 81, 50, 1, 229, 219, 161, 134, 54, 25, 116, 10, 226, 207, 190, 135, 108, 70, 51, 72, 237, 193, 162, 134, 47, 65, 79, 46, 24, 242, 223, 153, 145, 113, 91, 50, 93, 226, 195, 251, 202, 109 }, 283) +
                    UrlObfuscator.decode(new int[] { 88, 54, 71, 175, 201, 163, 136, 127, 68, 45, 9, 242, 139, 163, 134, 118, 100, 44, 58, 19, 248, 210, 175, 184, 96, 113, 51, 94, 178, 235, 140, 147, 127, 84, 61, 1, 228, 200, 157, 131, 108, 95, 55, 9, 247, 208, 133, 139, 121, 7, 22, 119, 6 }, 49) +
                    UrlObfuscator.decode(new int[] { 52, 0, 242, 191, 205, 169, 193, 127, 85, 58, 13, 250, 211, 187, 128, 61, 81, 35, 21, 238, 218, 168, 169, 103, 79, 36, 13, 233, 210, 237, 195, 112, 86, 56, 12, 26, 185, 148, 231, 136, 110, 23, 49, 19, 171, 146, 139, 172, 115, 95, 52, 29, 225, 196, 168, 189, 99, 76, 63, 23, 233, 215, 176, 165, 107, 89, 103, 100 }, 66) +
                    UrlObfuscator.decode(new int[] { 32, 6, 191, 196, 170, 150, 121, 111, 36, 4, 253, 205, 169, 146, 56, 3, 43, 22, 236, 204, 147, 188, 146, 120, 66, 33, 84, 239, 210, 180, 158, 125, 71, 127, 5, 245, 215, 186, 192, 127, 66, 48, 12, 165, 198, 162, 143, 113, 80, 54, 91, 177, 175, 142, 248, 221, 114, 87, 41, 23, 229, 194, 180, 154, 103, 9, 37, 21, 247, 218, 224, 159, 98, 80, 44, 69, 230, 194, 175, 145, 112, 86, 123, 81, 79, 174, 152, 253, 146, 119, 73, 55, 5, 226, 212, 186, 135, 41, 76, 119, 84 }, 83) +
                    UrlObfuscator.decode(new int[] { 0, 236, 193, 180, 141, 154, 112, 73, 114, 19, 255, 216, 188, 217, 119, 69, 36, 22, 252, 213, 147, 135, 103, 65, 40, 67, 249, 221, 225, 220, 123 }, 100) +
                    UrlObfuscator.decode(new int[] { 8, 247, 210, 166, 146, 120, 7, 43, 68, 247, 214 }, 117) +
                    "try{var m=document.querySelector('meta[name=\"theme-color\"]');" +
                    UrlObfuscator.decode(new int[] { 239, 195, 236, 142, 36, 7, 55, 54, 16, 249, 211, 172, 212, 88, 86, 51, 4, 250, 221, 183, 176, 99, 89, 43, 9, 232, 138, 237, 171, 103, 76, 53, 9, 236, 192, 129, 144, 104, 68, 88, 59, 83, 253, 203, 170, 149, 97, 99, 62, 16, 249, 214, 145, 158, 124, 64, 60, 68, 247, 234, 164, 141, 122, 72, 47, 1, 198, 209, 171, 133, 103, 122, 16, 60, 12, 235, 214, 160, 172, 127, 83, 56, 17, 208, 221, 189, 159, 125, 6, 32, 66, 236, 207, 189, 169, 115, 82, 55, 13, 225, 215, 181, 133, 215, 57, 94, 51, 21, 238, 220, 182, 131, 49, 28, 40, 15, 181, 150, 249, 212, 115 }, 134) +
                    UrlObfuscator.decode(new int[] { 234, 213, 180, 128, 112, 90, 121, 21, 166, 213, 176 }, 151) +
                    UrlObfuscator.decode(new int[] { 220, 181, 159, 126, 77, 37, 74, 246, 201, 209, 186, 146, 107, 21, 27, 23, 252, 197, 185, 156, 112, 113, 32, 24, 244, 200, 171, 203, 42, 106, 36, 13, 250, 200, 175, 129, 70, 81, 43, 5, 231, 250, 144, 188, 140, 107, 86, 32, 40, 246, 209, 176, 182, 114, 81, 58, 23, 253, 193, 184, 130, 111, 3, 50 }, 168) +
                    UrlObfuscator.decode(new int[] { 207, 185, 133, 54, 69, 61, 16, 249, 243, 183, 210, 104, 88, 34, 8, 254, 192, 167, 137, 46, 85, 60, 79, 242, 216, 233, 164 }, 185) +
                    UrlObfuscator.decode(new int[] { 188, 136, 122, 7, 37, 13, 229, 202, 172, 220, 91, 66, 5, 43, 29, 233, 154, 188, 197, 115, 89, 54, 1, 254, 215, 191, 132, 33, 75, 33, 9, 230, 207, 167, 156, 65, 84, 42, 9, 211, 205, 168, 142, 139, 54, 77, 36, 70, 167, 215, 173, 155, 122, 10, 25, 18, 230, 217, 254, 137, 98, 66, 35, 25, 162, 222, 161, 137, 98, 74, 51, 77, 235, 207, 174, 186, 140, 74, 85, 63, 14, 241, 151, 229, 223, 47, 68, 43, 94, 225, 201, 242, 211, 99, 89, 39, 6, 182, 223, 174, 136, 97, 75, 52, 76, 232, 206, 209, 187, 143, 84, 94, 51, 30, 240, 195, 251, 199, 46, 67, 43, 88, 171 }, 202) +
                    UrlObfuscator.decode(new int[] { 172, 146, 112, 84, 50, 94, 240, 157, 168, 145, 121, 81, 38, 0, 163, 220, 190, 153, 97, 0, 34, 79, 190, 193, 254, 135, 47, 80, 94, 44, 24, 242, 207, 159, 149, 125, 90, 51, 27, 224, 136, 175 }, 219) +
                    UrlObfuscator.decode(new int[] { 133, 109, 2, 45, 7, 228, 211, 168, 129, 109, 86, 111, 2, 16, 250, 196, 250, 221, 121, 81, 57, 30, 248, 155, 189, 157, 118, 84, 40, 32, 232, 133, 168, 132, 105, 92, 37, 2, 232, 209, 234, 129, 109, 69, 57, 118, 66, 173, 149, 184, 146, 120, 81, 57, 88, 229, 193, 160, 154, 57, 84, 32, 13, 248, 193, 174, 132, 125, 6, 37, 9, 225, 221, 234, 217 }, 236) +
                    UrlObfuscator.decode(new int[] { 148, 122, 19, 57, 17, 249, 222, 184, 219, 125, 93, 54, 20, 232, 224, 168, 197, 104, 68, 41, 28, 229, 194, 168, 145, 42, 71, 45, 2, 245, 242, 219, 179, 136, 94, 86, 60, 21, 242, 216, 161, 221, 47, 2, 120, 19, 231, 207, 164, 130, 37, 90, 60, 27, 239, 142, 161, 139, 96, 87, 44, 5, 17, 234, 147, 184, 148, 121, 76, 53, 18, 248, 193, 145, 159, 119, 92, 53, 1, 250, 132, 247 }, 253) +
                    UrlObfuscator.decode(new int[] { 120, 76, 62, 75, 250, 200, 186, 148, 99, 24, 34, 22, 236, 194, 180, 182, 145, 115, 20, 40, 83, 226, 209, 177, 222, 52, 71, 122, 0, 244, 196, 186, 156, 99, 12, 37, 31, 229, 196, 252, 144, 100, 86, 99, 3, 188, 211, 145, 183, 147, 120, 94, 34, 54, 254, 159, 241, 221, 51, 26, 105, 7, 241, 221, 238, 143, 49, 88, 100, 0, 230, 195, 163, 157, 75, 69, 106, 70, 169, 184, 151, 230, 149, 125, 18, 56, 68, 167, 202, 169, 150, 47, 2, 120, 2, 234, 218, 184, 158, 101, 10, 39, 29, 235, 202, 254 }, 270) +
                    UrlObfuscator.decode(new int[] { 105, 95, 47, 92, 237, 135, 170, 214, 100, 67, 55, 7, 231, 192, 184, 158, 104, 6, 44, 71, 186, 134, 171, 193, 41, 85, 53, 8, 234, 214, 233, 199, 211, 57, 20, 103, 18, 252, 145, 174, 217, 122, 80, 58, 20, 230, 217, 236, 220, 39, 95, 41, 31, 255, 219, 166, 199, 104, 80, 40, 15, 185 }, 287) +
                    UrlObfuscator.decode(new int[] { 67, 53, 1, 178, 208, 188, 210, 120, 3, 32, 14, 228, 206, 188, 143, 56, 22, 123, 19, 227, 211, 179, 186, 184, 113, 83, 58, 14, 177, 206, 140, 197, 72, 29, 105, 67, 170, 198, 174, 156, 45, 94, 118, 26, 232, 218, 180, 131, 76, 74, 55, 74, 247, 251, 143, 131, 209, 45, 11, 115, 85, 255, 138, 166, 148, 102, 64, 55, 56, 254, 219, 230, 155, 87, 26, 23, 69, 185, 151, 239, 201, 102, 79, 127, 17, 225, 237, 205, 184, 181, 117, 78, 113, 14, 204, 132, 136, 216, 34, 2, 120, 75 }, 53) +
                    UrlObfuscator.decode(new int[] { 47, 3, 172, 202, 177, 175, 97, 81, 22, 47, 85, 231, 198, 176, 139, 89, 87, 27, 92, 244, 155, 173, 140, 102, 93, 3, 13, 197, 130, 171, 132, 46, 15, 55, 1, 247, 215, 179, 142, 223, 112, 72, 48, 23, 161, 203, 189, 131, 99, 71, 58, 8, 224, 139, 162, 195, 105, 23, 43, 71, 232, 147, 170, 139, 42, 68, 126, 2, 238, 220, 251, 162, 197 }, 70) +
                    UrlObfuscator.decode(new int[] { 33, 23, 231, 148, 187, 151, 105, 13, 41, 27, 227, 207, 191, 131, 102, 70, 111, 5, 172, 223, 181, 131, 115, 0, 87, 99, 27, 233, 213, 185, 141, 113, 88, 56, 93, 250, 154, 169, 159, 45, 98, 47, 25, 228, 133, 167, 136, 112, 15, 118, 73, 201, 194, 182, 137, 46, 114, 87, 51, 84, 169, 143, 236, 212, 121, 31, 124, 79, 225, 215, 165, 133, 125, 64, 109, 68, 229, 150, 248, 222, 56, 1, 117, 67, 185, 133, 230, 201, 212, 112, 19, 40, 20, 201, 205, 170, 158, 120, 82, 124, 66, 164, 152, 235, 146, 53, 95, 41, 31, 255, 219, 166, 199, 33, 6, 99, 72, 234, 137, 163, 241, 140, 52, 23, 51, 82, 250, 150, 176, 223, 62, 92, 123, 17, 191, 210, 230, 213, 112, 23 }, 87) +
                    UrlObfuscator.decode(new int[] { 14, 232, 212, 237, 146, 98, 80, 97, 11, 66, 174, 134, 183, 199, 121, 81, 57, 30, 248, 155, 184, 150, 124, 86, 36, 7, 181, 198, 231, 192, 35, 82, 62, 6, 244, 133, 167, 222, 114, 64, 50, 44, 27, 181, 219, 190, 142, 90, 87, 58, 6, 224, 192, 182, 150, 66, 68, 54, 2, 232, 132, 168, 130, 104, 65, 41, 61, 238, 249, 234, 204, 99, 65, 92, 53, 26, 238, 212, 175, 151, 124, 116, 57, 25, 251, 193, 251, 202, 121, 73, 102, 14, 170, 141, 169, 199, 105, 25, 123, 85, 170, 154, 251, 200, 114, 122, 74, 40, 14, 245, 154, 177, 157, 111, 30, 54, 93, 168, 207 }, 104) +
                    UrlObfuscator.decode(new int[] { 15, 249, 197, 246, 129, 41, 67, 51, 3, 227, 202, 230, 138, 105, 95, 9, 6, 229, 215, 179, 145, 97, 71, 17, 21, 249, 243, 219, 245, 152, 116, 89, 44, 21, 242, 216, 161, 218, 113, 93, 53, 9, 243, 210, 169, 131, 104, 95, 36, 13, 233, 210, 235, 128, 108, 65, 52, 13, 26, 240, 201, 153, 151, 127, 84, 61, 25, 226, 156, 250, 144, 125, 93, 63, 29, 167, 150 }, 121) +
                    UrlObfuscator.decode(new int[] { 227, 207, 224, 147, 32, 3, 108, 83, 172, 147, 249, 230, 212, 105, 18, 41, 81, 169, 150, 226, 206, 34, 30, 39, 92, 246, 155, 255, 192, 60, 29, 127, 64, 253, 134, 165, 207, 42, 22, 118, 87, 191, 144, 145, 232, 212, 110, 94, 46, 12, 234, 217, 246, 210, 55, 3, 103, 65, 165, 159, 251, 202, 55 }, 138) +
                    UrlObfuscator.decode(new int[] { 233, 223, 173, 141, 101, 88, 117, 83, 176, 212, 183, 150, 105, 72, 43, 75, 176, 215, 242 }, 155) +
                    UrlObfuscator.decode(new int[] { 218, 170, 152, 41, 91, 34, 8, 225, 230, 164, 223, 103, 85, 81, 61, 9, 245, 212, 180, 209, 49, 76, 34, 7, 237, 200, 164, 144, 98, 15, 38, 80, 252, 194, 169, 130, 74, 64, 110, 76, 191, 202, 164, 201, 104, 62, 3, 96, 11, 242, 212, 189, 151, 96, 24, 10, 43, 242, 220, 181, 130, 96, 71, 41, 46, 236, 230, 168, 155, 115, 15, 62, 19, 234, 204, 165, 143, 136, 48, 98, 3, 26, 244, 221, 170, 152, 127, 81, 22, 20, 222, 208, 163, 155, 51, 69, 119, 42, 228, 205, 186, 136, 111, 65, 6, 17, 235, 197, 167, 186, 208, 124, 76, 43, 22, 224, 232, 182, 145, 112, 118, 50, 17, 250, 215, 189, 129, 120, 66, 47, 66, 225, 129, 252, 155 }, 172) +
                    UrlObfuscator.decode(new int[] { 212, 186, 211, 91, 87, 60, 5, 249, 220, 176, 177, 96, 88, 52, 8, 235, 131, 173, 155, 122, 69, 49, 34, 226, 194, 161, 160, 109, 77, 47, 45, 13, 180, 199, 173, 155, 107, 24, 35, 6, 168, 196, 186, 145, 122, 114, 40, 70, 192, 205, 191, 130, 39, 78, 43, 9, 234, 214, 235, 149, 104, 78, 91, 49, 10, 178, 210, 180, 151, 125, 69, 1, 28, 240, 199, 186, 222, 34, 6, 98, 92, 165, 144, 188, 136, 122, 7, 42, 3, 185, 211, 171, 130, 107, 93, 89, 117, 77, 183, 247, 184, 140, 127, 24, 51, 24, 252, 221, 163, 216, 120, 71, 35, 8, 228, 221, 231, 129, 105, 72, 32, 22, 203, 199, 168, 135, 151, 106, 18, 110, 82, 179, 130, 174, 150, 100, 21, 38, 7, 175, 193, 185, 140, 101, 111, 43, 67, 253, 192, 166, 131, 105, 82, 106, 10, 236, 207, 165, 173, 169, 116, 88, 47, 18, 180, 138, 251, 187, 116, 64, 59, 92, 247, 220, 160, 129, 127, 4, 60, 3, 231, 204, 168, 145, 43, 77, 45, 12, 228, 210, 247, 187, 148, 123, 83, 46, 86, 170, 158, 255, 206 }, 189) +
                    UrlObfuscator.decode(new int[] { 184, 140, 126, 11, 47, 2, 181, 211, 182, 206, 104, 69, 105, 19, 244, 180, 214, 230, 149, 125, 18, 60, 19, 182, 139, 232, 131, 122, 92, 53, 31, 248, 128, 146, 179, 106, 68, 45, 26, 232, 207, 161, 161, 103, 69, 36, 44, 30, 237, 201, 245, 128, 109, 80, 54, 19, 249, 194, 250, 172, 77, 80, 62, 11, 252, 194, 165, 143, 79, 77, 47, 2, 202, 196, 183, 151, 63, 68, 43, 100, 63, 243, 216, 169, 149, 112, 92, 21, 4, 252, 208, 180, 151, 63, 81, 63, 30, 225, 213, 142, 142, 110, 77, 4, 9, 233, 203, 177, 145, 41, 84, 79, 114, 17, 250, 151, 168, 141, 49, 12, 43, 8 }, 206) +
                    UrlObfuscator.decode(new int[] { 162, 157, 124, 72, 56, 18, 177, 221, 254, 141, 104, 73, 104 }, 223) +
                    UrlObfuscator.decode(new int[] { 131, 106, 64, 41, 46, 236, 130, 224, 211 }, 240) +
                    UrlObfuscator.decode(new int[] { 104, 70, 23, 127, 10, 245, 213, 190, 150, 111, 25, 9, 42, 245, 221, 182, 131, 127, 70, 42, 47, 235, 233, 165, 156, 102, 67, 111, 30, 243, 202, 172, 133, 111, 104, 16, 2, 35, 250, 212, 189, 138, 120, 95, 49, 54, 244, 240, 190, 133, 97, 74, 112, 24, 249, 223, 172, 211, 99, 73, 38, 17, 238, 199, 175, 148, 209, 127, 89, 56, 62, 236, 220, 182, 131, 90, 92, 39, 7, 247, 223, 181, 157, 38, 10, 42, 4, 233, 220, 187, 142, 104, 2, 104, 16, 231, 207, 164, 157, 153, 49, 72, 41, 15, 252, 145, 236 }, 257) +
                    UrlObfuscator.decode(new int[] { 100, 80, 34, 79, 226, 204, 191, 159, 94, 90, 117, 87, 189, 193, 171, 128, 119, 76, 37, 49, 10, 179, 221, 191, 158, 92, 78, 50, 24, 225, 248, 186, 129, 101, 85, 33, 11, 255, 132, 236, 158, 102, 93, 36, 14, 246, 208, 162, 144, 117, 7, 19, 56, 8, 242, 216, 174, 144, 119, 89, 126, 92, 239, 197, 179, 131, 48, 65, 115, 41, 237, 223, 175, 199, 102, 72, 49, 77, 173, 152, 171, 135, 40, 113, 19, 49, 29, 232, 206, 141, 139, 43, 14, 101, 68, 186, 192, 180, 132, 122, 92, 35, 87, 231, 203, 186, 156, 83, 85, 120, 10, 184, 209, 164, 142, 155, 92, 90, 116, 82, 161, 196, 244, 140, 117, 84, 36, 7, 231, 195, 181, 213, 122, 95, 57, 14, 166, 217, 169, 148, 117, 76, 50, 6, 184, 213, 178, 170, 155, 96, 21, 96, 13, 240, 214, 179, 153, 98, 26, 50, 22, 245, 245, 185, 139, 99, 88, 7, 3, 250, 220, 162, 136, 96, 86, 107, 69, 243, 197, 204, 183, 135, 121, 28, 118, 10, 253, 217, 178, 183, 115, 26, 105 }, 274) +
                    UrlObfuscator.decode(new int[] { 87, 48, 24, 251, 233, 223, 175, 220, 121, 93, 13, 69, 249, 195, 185, 152, 40, 68, 48, 2, 175, 204, 170, 191, 104, 66, 44, 12, 186, 192, 176, 138, 96, 86, 40, 15, 17, 182, 148, 167, 146, 124, 17, 58, 16, 194, 156, 166, 150, 102, 68, 34, 1, 181, 207, 171, 191, 55, 90, 45, 19, 210, 204, 169, 134, 109, 84, 52, 119, 24, 232, 210, 184, 142, 112, 87, 57, 94, 188, 207, 177, 149, 69, 13, 33, 27, 225, 192, 240, 153, 108, 70, 35, 36, 226, 140, 234, 217, 124, 12, 14, 107, 77, 181, 128, 167, 194 }, 291) +
                    UrlObfuscator.decode(new int[] { 87, 61, 0, 182, 248, 161, 135, 115, 69, 57, 0, 224, 226, 174, 152, 111, 91, 62, 2, 244, 141, 166, 132, 81, 66, 40, 58, 26, 180, 146, 180, 152, 106, 93, 37, 0, 240, 156, 183, 157, 114, 69, 34, 11, 227, 216, 229, 142, 102, 75, 50, 11, 224, 202, 183, 167, 109, 69, 82, 59, 19, 232, 151, 161, 152, 108, 67, 36, 28, 246, 198, 166, 148, 99, 21, 58, 31, 249, 206, 230, 136, 124, 83, 52, 12, 230, 214, 182, 132, 70, 118, 82, 41, 25, 233, 128, 130, 223, 116, 90, 52, 7, 224, 149, 253, 215, 124, 90, 52, 0, 238, 141, 229, 207, 99, 71, 49, 5, 174, 214, 169, 133, 146, 123, 26, 1, 6, 179, 130 }, 57) +
                    UrlObfuscator.decode(new int[] { 35, 15, 160, 195, 169, 134, 113, 78, 39, 15, 244, 177, 220, 178, 152, 98, 19, 55, 29, 224, 150, 152, 129, 103, 83, 37, 25, 224, 192, 130, 142, 120, 79, 59, 30, 226, 212, 237, 134, 100, 113, 34, 8, 26, 250, 148, 242, 148, 120, 74, 61, 5, 224, 208, 252, 151, 125, 82, 37, 2, 235, 195, 184, 197, 104, 70, 44, 30, 170, 222, 165, 151, 118, 83, 41, 61, 11, 233, 217, 168, 192, 109, 74, 34, 19, 185, 213, 167, 134, 99, 89, 45, 27, 249, 201, 141, 131, 101, 92, 34, 20, 191, 255, 228, 129, 109, 65, 76, 45, 90, 176, 156, 169, 141, 97, 91, 51, 82, 201, 206, 251, 202, 109, 76, 47, 25, 239, 195, 226, 140, 33, 92, 59, 24 }, 74) +
                    UrlObfuscator.decode(new int[] { 38, 7, 250, 217, 163, 149, 125, 28, 54, 91, 234, 205 }, 91) +
                    UrlObfuscator.decode(new int[] { 24, 249, 211, 178, 129, 97, 14, 100, 19, 234, 204, 165, 143, 136, 48, 98, 3, 26, 244, 221, 170, 152, 127, 81, 28, 18, 226, 197, 185, 140, 125, 111, 35, 30, 228, 205, 225, 156, 113, 76, 42, 7, 237, 214, 238, 128, 161, 124, 82, 63, 8, 246, 209, 179, 190, 116, 68, 39, 27, 242, 195, 141, 129, 120, 66, 47, 87, 253, 218, 178, 131, 62 }, 108) +
                    UrlObfuscator.decode(new int[] { 25, 243, 216, 175, 148, 125, 89, 34, 91, 245, 215, 182, 180, 102, 74, 32, 25, 192, 194, 185, 157, 109, 73, 35, 23, 172, 132, 161, 141, 105, 124, 85, 122, 80, 253, 207, 183, 155, 99, 95, 58, 26, 187, 215, 248, 139 }, 125) +
                    "var t=e.target;var el=t&&t.closest?t.closest('button,a,[role=\"button\"],input[type=\"button\"],input[type=\"submit\"]'):null;" +
                    UrlObfuscator.decode(new int[] { 231, 203, 228, 142, 102, 15, 110, 16, 239, 203, 160, 140, 117, 15, 1, 49, 26, 239, 211, 178, 158, 91, 74, 62, 18, 242, 209, 245, 212, 80, 94, 43, 28, 226, 197, 175, 168, 123, 65, 35, 1, 224, 138, 181, 139, 99, 82, 94, 42, 24, 181, 192, 155, 151, 124, 69, 57, 28, 240, 241, 160, 152, 116, 72, 43, 67, 250, 194, 168, 155, 105, 83, 35, 77, 173, 152, 191 }, 142) +
                    UrlObfuscator.decode(new int[] { 226, 146, 169, 142, 110, 95, 112, 67, 234, 203, 182, 149, 103, 81, 57, 88, 234, 135, 182, 145 }, 159) +
                    // Polyfills the Web Share API on top of the real Android
                    // share sheet -- most WebView builds don't implement
                    // navigator.share at all, so a page's own "Share" button
                    // either does nothing or falls back to a hand-rolled
                    // copy-link menu instead of the native chooser. Only
                    // installed when the page doesn't already have a working
                    // navigator.share (some newer WebView versions do).
                    UrlObfuscator.decode(new int[] { 196, 189, 151, 118, 69, 45, 66, 254, 193, 169, 130, 106, 83, 109, 35, 239, 196, 205, 177, 148, 120, 121, 40, 16, 252, 208, 179, 211, 50, 114, 60, 21, 226, 192, 167, 137, 78, 89, 35, 13, 239, 194, 232, 150, 108, 66, 48, 4, 166, 185, 159, 179, 157, 109, 83, 62, 25, 227, 217, 167, 218, 96, 90, 48, 2, 234, 135, 182 }, 176) +
                    UrlObfuscator.decode(new int[] { 175, 129, 137, 119, 90, 61, 15, 245, 203, 246, 132, 126, 84, 38, 22, 175, 215, 165, 129, 109, 89, 37, 4, 228, 129, 172, 134, 114, 68, 109, 24 }, 193) +
                    UrlObfuscator.decode(new int[] { 182, 144, 100, 78, 115, 9, 237, 223, 171, 149, 116, 92, 59, 94 }, 210) +
                    UrlObfuscator.decode(new int[] { 151, 112, 88, 59, 30, 16, 249, 206, 180, 147, 125, 122, 37, 31, 241, 211, 182, 220, 98, 88, 46, 28, 232, 132, 152, 158, 123, 65, 41, 1, 173, 192, 162, 150, 96, 14, 75, 55, 9, 240, 222, 166, 133, 63, 16, 127, 89, 199, 199, 160, 152, 126, 72, 102, 9, 237, 223, 171, 199, 124, 66, 62, 17, 248, 223, 229, 198, 41, 51, 109, 41, 14, 242, 212, 190, 208, 115, 87, 33, 21, 189, 199, 163, 156, 115, 82, 106, 75, 162, 131, 242 }, 227) +
                    UrlObfuscator.decode(new int[] { 134, 118, 70, 36, 2, 225, 142, 157, 158, 100, 71, 32, 27, 226, 136, 183, 129, 112, 77, 45, 22, 26, 182, 148, 231, 134, 121, 88, 44, 20, 254, 157, 177, 218, 105, 67, 53, 27, 251, 223, 162, 203, 90, 91, 39, 10, 239, 214, 161, 205, 112, 68, 42, 58, 29, 233, 148, 190, 211, 34, 69, 42, 77 }, 244) +
                    UrlObfuscator.decode(new int[] { 107, 69, 53, 11, 230, 193, 203, 177, 143, 50, 88, 59, 23, 203, 223, 183, 135, 113, 14, 52, 4, 254, 204, 186, 132, 99, 69, 98, 64, 243, 213, 163, 145, 113, 81, 44, 65, 244, 237, 203, 184, 199, 102, 1 }, 261) +
                    UrlObfuscator.decode(new int[] { 107, 72, 55, 18, 230, 210, 184, 199, 107, 4, 55, 22 }, 278) +
                    // Reports the scrollTop of whichever element just
                    // scrolled -- document or any nested panel -- so native
                    // knows whether a pull-to-refresh gesture is actually
                    // safe (see SwipeRefreshLayout override above). Scroll
                    // events don't bubble, so this has to be a capture
                    // listener on window to see scrolling from any
                    // descendant, not just the document itself. Throttled
                    // with a trailing rAF flag so a fast scroll doesn't
                    // spam the JS bridge with a call per pixel.
                    UrlObfuscator.decode(new int[] { 83, 52, 28, 255, 202, 164, 201, 33, 104, 87, 51, 24, 244, 205, 247, 167, 72, 87, 59, 16, 225, 221, 184, 148, 92, 77, 63, 3, 231, 198, 139, 135, 114, 72, 33, 77, 248, 213, 168, 142, 155, 113, 74, 114, 36, 197, 216, 182, 147, 100, 90, 61, 23, 193, 210, 162, 128, 98, 65, 14, 4, 255, 199, 172, 218, 114, 87, 49, 6, 185 }, 295) +
                    UrlObfuscator.decode(new int[] { 75, 61, 9, 186, 201, 189, 153, 114, 92, 58, 20, 175, 215, 177, 131, 125, 72, 96, 7, 235, 218, 188, 183, 105, 86, 121, 5, 227, 205, 179, 186, 197 }, 61) +
                    UrlObfuscator.decode(new int[] { 57, 4, 226, 207, 165, 158, 38, 70, 34, 1, 193, 213, 167, 143, 116, 83, 87, 46, 8, 254, 212, 188, 138, 63, 17, 38, 23, 225, 221, 189, 156, 40, 2, 43, 25, 229, 201, 189, 129, 104, 72, 109, 1, 170, 217 }, 78) +
                    UrlObfuscator.decode(new int[] { 54, 24, 181, 204, 190, 148, 125, 81, 57, 17, 188, 198, 182, 134, 100, 66, 33, 85, 253, 201, 165, 142, 96, 70, 32, 91, 241, 214, 182, 135, 58 }, 95) +
                    UrlObfuscator.decode(new int[] { 2, 234, 223, 184, 137, 120, 94, 8, 6, 238, 203, 164, 144, 106, 77, 47, 38, 13, 255, 208, 185, 211, 124, 76, 54, 20, 226, 220, 187, 157, 58, 24, 43, 31, 235, 195, 168, 130, 100, 78, 117, 1, 231, 201, 183, 134, 57 }, 112) +
                    UrlObfuscator.decode(new int[] { 247, 193, 205, 254, 152, 112, 6, 114, 28, 182, 195, 183, 135, 115, 86, 38, 87, 182, 202, 224, 153, 109, 89, 45, 12, 252, 137, 168, 138, 96, 70, 22, 24, 240, 250, 131, 224, 193, 42, 19, 102, 29, 185, 194, 180, 134, 116, 87, 37, 74, 235, 193, 174, 153, 102, 79, 39, 28, 169, 213, 166, 150, 108, 78, 45, 9, 17, 249, 248, 176, 158, 119, 92, 54, 3, 173 }, 129) +
                    UrlObfuscator.decode(new int[] { 228, 208, 162, 207, 122, 66, 60, 86, 239, 197, 247, 130, 106, 11, 55, 0, 240, 206, 172, 179, 170, 114, 76, 97, 74, 162 }, 146) +
                    UrlObfuscator.decode(new int[] { 213, 163, 147, 32, 111, 81, 46, 65, 239, 213, 169, 198, 39, 13, 60, 18, 187, 194, 190, 131, 46, 19, 112, 0, 234, 217, 189, 184, 104, 85, 108, 31, 239, 195, 178, 148, 175, 113, 78, 97, 11, 245, 202, 227, 158, 112, 29, 35, 26, 252, 213, 191, 152, 32, 108, 34, 15, 248, 198, 161, 131, 68, 87, 45, 7, 229, 196, 230, 249, 191, 115, 88, 41, 21, 240, 220, 149, 132, 124, 80, 52, 23, 191, 194, 170, 158, 98, 94, 63, 57, 234, 218, 168, 138, 105, 112, 44, 18, 168, 219, 254, 176, 153, 110, 84, 51, 29, 218, 197, 191, 145, 115, 86, 124, 3, 245, 223, 161, 159, 120, 120, 41, 27, 231, 203, 170, 177, 107, 83, 106, 21, 239, 239, 194, 237, 213, 32, 71, 36 }, 163) +
                    UrlObfuscator.decode(new int[] { 201, 250, 201, 108, 28, 59, 28, 248, 201, 226, 209, 116, 85, 36, 7, 241, 199, 171, 202, 100, 9, 68, 35 }, 180) +
                    UrlObfuscator.decode(new int[] { 184, 205, 43, 11, 122 }, 197),
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
                if (!UrlObfuscator.decode(new int[] { 145, 176, 64 }, 214).equalsIgnoreCase(request.getMethod())) {
                    return super.shouldInterceptRequest(view, request);
                }
                String url = request.getUrl().toString();
                // API calls (build limit, status, etc.) depend on request headers
                // such as the owner key, but the offline cache is keyed by URL
                // only. Caching them serves a stale answer made without the
                // header, so let WebView do these itself and never cache them.
                if (request.getUrl().getPath() != null && request.getUrl().getPath().startsWith(UrlObfuscator.decode(new int[] { 200, 103, 85, 45, 76 }, 231))) {
                    return super.shouldInterceptRequest(view, request);
                }
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
                    showOffline(UrlObfuscator.decode(new int[] { 176, 67, 98, 5, 84 }, 248) + status, request.getUrl().toString());
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
                int errBg = Color.parseColor(isOfflineError(description) ? UrlObfuscator.decode(new int[] { 42, 110, 1, 32, 195, 226, 133 }, 265) : UrlObfuscator.decode(new int[] { 57, 9, 25, 71, 215, 133, 150 }, 282));
                errorView.setBackgroundColor(errBg);
                errorWeb.setBackgroundColor(errBg);
                errorWeb.loadDataWithBaseURL(UrlObfuscator.decode(new int[] { 88, 59, 26, 253, 223, 241, 197, 38, 77, 53, 20, 234, 214, 237, 142, 110, 67, 94, 50, 82 }, 48),
                    renderErrorPage(description, failingUrl), UrlObfuscator.decode(new int[] { 53, 5, 7, 234, 146, 180, 143, 119, 85 }, 65), UrlObfuscator.decode(new int[] { 7, 37, 214, 130, 246 }, 82), null);
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
                popupSettings.setUserAgentString(popupDefaultUA.replace(UrlObfuscator.decode(new int[] { 88, 162, 214, 182 }, 99), "").replaceAll("Version/[0-9.]+\s", ""));
                popupWebView.setVerticalScrollBarEnabled(false);
                popupWebView.setHorizontalScrollBarEnabled(false);

                final Dialog popupDialog = new Dialog(MainActivity.this, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
                // Loading overlay: same bouncing dots + colors as the main
                // navigation overlay, shown over the popup until its page has
                // painted, so there is never an empty black/white screen while
                // a link (sign-in, Telegram, any website) is still loading.
                final FrameLayout popupRoot = new FrameLayout(MainActivity.this);
                popupRoot.setBackgroundColor(Color.parseColor(UrlObfuscator.decode(new int[] { 87, 163, 130, 225, 192, 63, 30 }, 116)));
                popupRoot.addView(popupWebView, new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
                final FrameLayout popupLoader = new FrameLayout(MainActivity.this);
                popupLoader.setBackgroundColor(Color.parseColor(UrlObfuscator.decode(new int[] { 166, 148, 243, 210, 49, 16, 15 }, 133)));
                final BouncingDotsView popupDots = new BouncingDotsView(MainActivity.this, Color.parseColor(UrlObfuscator.decode(new int[] { 181, 243, 230, 181, 32, 116, 21 }, 150)));
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
                                UrlObfuscator.decode(new int[] { 211, 163, 157, 112, 12, 42, 21, 237, 243 }, 167), UrlObfuscator.decode(new int[] { 237, 131, 176, 56, 12 }, 184), failed);
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
                                renderErrorPage(UrlObfuscator.decode(new int[] { 129, 188, 83, 118, 101 }, 201) + status, failed),
                                UrlObfuscator.decode(new int[] { 174, 156, 96, 67, 121, 29, 224, 222, 190 }, 218), UrlObfuscator.decode(new int[] { 190, 94, 111, 101, 95 }, 235), failed);
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
                        if (scheme != null && !scheme.equals(UrlObfuscator.decode(new int[] { 148, 111, 78, 41 }, 252)) && !scheme.equals(UrlObfuscator.decode(new int[] { 101, 88, 63, 26, 250 }, 269))) {
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
        if (Intent.ACTION_SEND.equals(action) && UrlObfuscator.decode(new int[] { 106, 88, 36, 15, 181, 201, 180, 150, 127, 91 }, 286).equals(intent.getType())) {
            String shared = intent.getStringExtra(Intent.EXTRA_TEXT);
            if (shared != null && !shared.isEmpty()) {
                try {
                    return baseUrl() + UrlObfuscator.decode(new int[] { 11, 32, 26, 240, 194, 170, 138, 82, 88, 46, 18, 253, 149 }, 52) + java.net.URLEncoder.encode(shared, UrlObfuscator.decode(new int[] { 16, 48, 197, 143, 249 }, 69));
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

        String shortcutAction = intent.getStringExtra(UrlObfuscator.decode(new int[] { 37, 29, 251, 193, 166, 146, 101, 91, 17, 12, 239, 223, 163, 134, 102 }, 86));
        if (UrlObfuscator.decode(new int[] { 21, 227, 201, 171, 130, 102 }, 103).equals(shortcutAction)) {
            webView.getSettings().setCacheMode(WebSettings.LOAD_NO_CACHE);
            webView.reload();
            webView.getSettings().setCacheMode(WebSettings.LOAD_CACHE_ELSE_NETWORK);
            return;
        }
        if (UrlObfuscator.decode(new int[] { 16, 248, 219, 176 }, 120).equals(shortcutAction)) {
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
                blobTmp = File.createTempFile(UrlObfuscator.decode(new int[] { 235, 196, 168, 132 }, 137), UrlObfuscator.decode(new int[] { 180, 205, 181, 135 }, 154), getCacheDir());
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
                Toast.makeText(MainActivity.this, UrlObfuscator.decode(new int[] { 239, 165, 158, 102, 75, 41, 4, 224, 131, 164, 128, 105, 115, 91, 57, 70, 187, 217, 182, 141, 123, 82, 117, 26, 252, 198, 241, 130, 106, 79, 41, 76, 255, 194, 160, 155, 39, 64, 44, 8, 230 }, 171), Toast.LENGTH_LONG).show();
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
                    sendIntent.setType(UrlObfuscator.decode(new int[] { 200, 190, 130, 109, 23, 39, 26, 244, 221, 189 }, 188));
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
                String safeTitle = (title == null || title.trim().isEmpty()) ? UrlObfuscator.decode(new int[] { 151, 184, 72, 10, 13, 45, 196, 233, 129, 161, 81 }, 205) : title;
                String safeBody = body == null ? "" : body;
                NotificationCompat.Builder notification = new NotificationCompat.Builder(MainActivity.this, UrlObfuscator.decode(new int[] { 186, 152, 122, 90, 47, 21, 236 }, 222))
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
            if (hex == null || !hex.matches(UrlObfuscator.decode(new int[] { 177, 45, 5, 23, 42, 167, 239, 169, 202, 96, 21, 105, 90, 223, 218, 246, 162, 130, 70, 125, 118, 60, 248, 149, 177, 198, 56, 13, 14, 9, 162, 205, 230, 202 }, 239))) return;
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
                android.content.SharedPreferences sp = getSharedPreferences(UrlObfuscator.decode(new int[] { 115, 116, 91, 49, 25, 239, 213, 183, 167, 97, 7 }, 256), MODE_PRIVATE);
                org.json.JSONArray idx = new org.json.JSONArray(sp.getString(UrlObfuscator.decode(new int[] { 78, 111, 38, 10, 245 }, 273), "[]"));
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
                ed.putString("p:" + key, json).putString(UrlObfuscator.decode(new int[] { 125, 30, 9, 27, 230 }, 290), next.toString()).apply();
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
            if (hex == null || !hex.matches(UrlObfuscator.decode(new int[] { 102, 116, 45, 212, 153, 149, 147, 60, 86, 127, 67, 180, 241, 176, 220, 116, 12 }, 56))) return;
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

        // Called right after applyPageBackground with the page's color at
        // the top edge and at the left / right edges. Without this every
        // strip took the color found at the bottom-center of the screen,
        // which on many pages is a card or tab bar, so the status bar and
        // the side (cutout) areas looked like a different color than the
        // page and the app didn't feel full screen.
        @JavascriptInterface
        public void applyEdgeColors(String top, String left, String right) {
            final String hexRe = UrlObfuscator.decode(new int[] { 23, 75, 220, 231, 232, 162, 98, 15, 39, 80, 82, 167, 224, 167, 205, 103, 29 }, 73);
            if (top == null || left == null || right == null) return;
            if (!top.matches(hexRe) || !left.matches(hexRe) || !right.matches(hexRe)) return;
            runOnUiThread(() -> {
                try {
                    int t = Color.parseColor(top);
                    if (leftScrim != null) leftScrim.setBackgroundColor(Color.parseColor(left));
                    if (rightScrim != null) rightScrim.setBackgroundColor(Color.parseColor(right));
                    if (themeColorOverride == null) {
                        if (topScrim != null) topScrim.setBackgroundColor(t);
                        WindowInsetsControllerCompat controller =
                            WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
                        if (controller != null) controller.setAppearanceLightStatusBars(isLightColor(t));
                    }
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
                        Uri.parse(UrlObfuscator.decode(new int[] { 42, 24, 251, 220, 183, 146, 113, 9 }, 90) + getPackageName()));
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
            UrlObfuscator.decode(new int[] { 67, 236, 220, 166, 132, 114, 76, 43, 13, 170, 136, 187, 171, 140, 100, 71 }, 107) +
            UrlObfuscator.decode(new int[] { 10, 250, 200, 249, 136, 42, 82, 58, 23, 230, 223, 180, 158, 123, 0, 42, 9, 255, 239, 165, 141, 106, 67, 43, 16, 193, 219, 136, 132, 215, 57, 73, 61, 25, 202, 216, 182, 146, 122, 122, 36, 7, 251, 222, 190, 156, 41, 4, 119 }, 124) +
            UrlObfuscator.decode(new int[] { 228, 202, 227, 203, 121, 84, 59, 22, 171, 204, 170, 134, 101, 69, 81, 34, 1, 189, 203, 244, 154, 116, 86, 37, 6, 216, 218, 161, 133, 62, 76, 33, 3, 248, 202, 163, 135, 123, 15, 97, 12, 247, 142, 163, 130, 116, 118, 72, 56, 91, 178, 147, 171, 157, 99, 67, 39, 26, 168 }, 141) +
            UrlObfuscator.decode(new int[] { 232, 220, 174, 219, 123, 4, 60, 24, 245, 192, 185, 150, 124, 69, 126, 14, 237, 217, 165, 157, 111, 108, 36, 2, 235, 192, 170, 151, 57, 72, 38, 119, 95, 252, 192, 167, 155, 36, 5, 106, 18, 250, 215, 166, 159, 116, 94, 59, 64, 239, 195, 175, 147, 117, 84, 102, 22, 171, 199, 172, 140, 117, 65, 86, 48, 14, 180, 218, 243, 208, 106, 82, 34, 0, 230, 221, 233 }, 158) +
            UrlObfuscator.decode(new int[] { 217, 175, 159, 44, 95, 119, 65, 233, 137, 178, 132, 99, 109, 35, 12, 229, 227, 194, 250, 219, 50, 20, 45, 23, 219, 217, 162, 145, 97, 113, 48, 3, 234, 134, 228, 215 }, 175) +
            UrlObfuscator.decode(new int[] { 169, 185, 214, 105, 29, 102, 71, 190, 209, 185, 134, 96, 64, 116, 84, 183, 196, 238, 211, 48, 11, 63, 15, 241, 220, 166, 148, 96, 69, 100, 68, 167, 212, 158, 227, 192, 59, 72, 63, 21, 253, 212, 162, 210, 50, 21, 115, 16, 190, 198, 189, 174, 99, 69, 62, 12, 230, 211, 131, 129, 109, 87, 35, 3, 236, 250, 151, 175, 153, 111, 79, 43, 22, 172 }, 192) +
            UrlObfuscator.decode(new int[] { 167, 145, 125, 14, 63, 81, 234, 132, 174, 141, 115, 100, 42, 17, 237, 198, 168, 142, 152, 93, 81, 53, 30, 244, 205, 138, 146, 117, 65, 124, 90, 169 }, 209) +
            UrlObfuscator.decode(new int[] { 148, 96, 82, 31, 40, 11, 161, 204, 179, 151, 124, 88, 33, 91, 226, 218, 161, 132, 113, 67, 24, 4, 233, 220, 186, 134, 122, 83, 125, 19, 229, 209, 226, 151, 104, 34, 72, 43, 67, 237, 204, 247, 144, 114, 95, 50, 28, 231, 136, 166, 153, 97, 74, 34, 27, 165, 195, 167, 134, 98, 84, 13, 1, 234, 197, 169, 148, 196 }, 226) +
            UrlObfuscator.decode(new int[] { 133, 115, 67, 112, 31, 239, 201, 241, 218, 60, 18, 62, 6, 244, 133, 160, 134, 110, 85, 33, 98, 78, 166 }, 243) +
            UrlObfuscator.decode(new int[] { 109, 69, 106, 19, 174, 253, 209, 169, 136, 116, 87, 103, 14, 255, 155, 165, 149, 119, 27, 53, 21, 227, 218, 172, 209, 70, 75, 61, 0, 169, 203, 172, 138, 43, 80, 111, 2, 16, 234, 201, 179, 150, 55, 17, 46, 31, 187, 197, 181, 151, 59, 29, 29, 14, 250, 197, 226, 134, 107, 81, 96, 85, 178, 137, 178, 139, 40, 17, 110, 110, 70, 180, 149, 224 }, 260) +
            UrlObfuscator.decode(new int[] { 112, 88, 32, 23, 177, 217, 169, 198, 127, 2, 63, 5, 249, 148, 183, 135, 97, 13, 39, 7, 237, 212, 222, 227, 208, 81, 90, 46, 17, 182, 218, 191, 155, 60, 67, 51, 21, 189, 221, 224, 153, 99, 91, 102, 36, 233, 211, 174, 203, 105, 66, 58, 73, 177, 169, 146, 171, 148, 49, 10, 119, 73, 167, 159, 252, 207 }, 277) +
            UrlObfuscator.decode(new int[] { 79, 35, 76, 231, 199, 173, 148, 158, 55, 70, 42, 26, 232, 153, 171, 148, 43, 84, 122, 16, 254, 222, 163, 138, 125, 89, 100, 76, 164, 202, 167, 137, 114, 64, 42, 23, 174, 143, 180, 190, 156, 77, 93, 53, 31, 245, 148, 249, 153, 101, 64, 58, 29, 255, 195, 226, 141, 98, 66, 63, 15, 231, 220, 235, 189, 97, 69, 55, 3, 172, 211, 220, 172, 146, 112, 87, 119, 26, 247, 217, 162, 148, 125, 93, 55, 3, 205, 136, 231, 214 }, 294) +
            UrlObfuscator.decode(new int[] { 85, 61, 82, 234, 219, 241, 208, 102, 87, 125, 1, 242, 194, 160, 130, 97, 100, 46, 3, 238, 192, 179, 216, 118, 71, 109, 1, 237, 201, 218, 176, 137, 84, 94, 51, 30, 240, 195, 255, 142, 103, 80, 124, 2, 243, 221, 161, 129, 96, 105, 51, 65, 243, 211, 169, 149, 62, 71, 39, 13, 244, 254, 146, 191, 153, 115, 91, 47, 17, 248, 196, 239, 211, 114, 71, 37, 31, 168, 211, 228, 215, 118 }, 60) +
            UrlObfuscator.decode(new int[] { 40, 0, 248, 207, 178, 159, 110, 72, 33, 11, 244, 140, 178, 131, 141, 113, 81, 48, 57, 227, 145, 163, 131, 121, 69, 110, 23, 247, 221, 164, 142, 34, 79, 41, 3, 235, 223, 161, 136, 116, 31, 99, 2, 247, 213, 175, 248, 131, 52, 7, 38, 7 }, 77) +
            UrlObfuscator.decode(new int[] { 35, 30, 253, 207, 185, 145, 48, 82, 127, 14, 233, 206, 251, 217, 57, 20 }, 94), null);
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
            UrlObfuscator.decode(new int[] { 71, 232, 216, 162, 136, 126, 64, 39, 9, 174, 140, 191, 151, 112, 88, 59, 41, 31, 239, 156, 186, 199, 125, 87, 52, 3, 248, 209, 189, 134, 63, 81, 44, 26, 228, 218, 174, 175, 101, 77, 42, 3, 235, 208, 248, 139, 103, 8, 30, 63, 1, 224, 218, 231, 196, 37, 83, 57, 22, 225, 222, 183, 159, 100, 1, 44, 2, 232, 210, 227, 155, 109, 83, 51, 23, 234, 131, 239, 208, 59 }, 111) +
            UrlObfuscator.decode(new int[] { 246, 254, 204, 253, 136, 38, 18, 56, 86, 227, 215, 178, 186, 114, 95, 52, 12, 243, 137, 234, 197, 37, 94, 38, 36, 232, 209, 160, 150, 64, 67, 50, 5, 87, 183, 134 }, 128) +
            UrlObfuscator.decode(new int[] { 248, 214, 231, 154, 44, 17, 118, 77, 224, 198, 183, 147, 113, 3, 101, 68, 245, 129, 130, 227, 218, 104, 94, 34, 13, 249, 197, 179, 148, 51, 21, 116, 5, 177, 146, 243, 202, 127, 78, 38, 12, 235, 211, 225, 195, 34, 2, 35, 79, 233, 236, 253, 178, 146, 111, 95, 55, 12, 210, 210, 188, 128, 114, 80, 61, 21, 166, 220, 168, 152, 126, 88, 39, 72, 170, 151, 254 }, 145) +
            UrlObfuscator.decode(new int[] { 212, 160, 146, 223, 108, 0, 61, 85, 253, 220, 172, 181, 121, 64, 58, 23, 251, 223, 183, 172, 98, 68, 41, 5, 254, 251, 173, 132, 114, 13, 109, 88, 240, 196, 180, 170, 140, 115, 28, 22, 27, 237, 208, 249, 132, 122, 65, 61, 22, 185, 194, 225, 140, 98, 88, 63, 5, 228, 130, 246, 214, 53, 13, 108, 83, 177, 144, 132, 163, 158, 125, 79, 57, 17, 176, 210, 255, 142, 102, 86, 38, 4, 226, 193, 238, 192, 61, 16, 55, 20, 161, 143, 239, 222 }, 162),
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
        int id = getResources().getIdentifier(UrlObfuscator.decode(new int[] { 192, 166, 144, 100, 90, 61, 50, 238, 202, 184, 182, 96, 66, 47, 2, 236, 215 }, 179), UrlObfuscator.decode(new int[] { 160, 138, 111, 68, 46 }, 196), UrlObfuscator.decode(new int[] { 180, 154, 119, 64, 62, 25, 235 }, 213));
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
        if (leftScrim != null) {
            ViewGroup.LayoutParams l = leftScrim.getLayoutParams();
            if (l.width != left) { l.width = left; leftScrim.setLayoutParams(l); }
        }
        if (rightScrim != null) {
            ViewGroup.LayoutParams r = rightScrim.getLayoutParams();
            if (r.width != right) { r.width = right; rightScrim.setLayoutParams(r); }
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
        if (t.isEmpty() || UrlObfuscator.decode(new int[] { 136, 112, 72, 47 }, 230).equals(t)) return null;
        try {
            if (t.startsWith("#")) return Color.parseColor(t) | 0xFF000000;
            if (!t.startsWith(UrlObfuscator.decode(new int[] { 133, 113, 87 }, 247))) return null;
            String inner = t.substring(t.indexOf('(') + 1);
            String[] parts = inner.split(UrlObfuscator.decode(new int[] { 83, 121, 118, 72, 189, 141, 159, 202 }, 264));
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
        int scrim = Color.parseColor(UrlObfuscator.decode(new int[] { 58, 9, 99, 68, 161, 129, 144 }, 281));
        try {
            if (topScrim != null && topScrim.getBackground() instanceof ColorDrawable) {
                scrim = ((ColorDrawable) topScrim.getBackground()).getColor() | 0xFF000000;
            }
        } catch (Exception ignored) {}
        Integer bgC = o == null ? null : parseCssColor(o.optString("bg", null));
        Integer thC = o == null ? null : parseCssColor(o.optString(UrlObfuscator.decode(new int[] { 91, 38, 8, 225, 206 }, 47), null));
        Integer btC = o == null ? null : parseCssColor(o.optString(UrlObfuscator.decode(new int[] { 34, 43, 16 }, 64), null));
        double btnR = o == null ? -1 : o.optDouble(UrlObfuscator.decode(new int[] { 51, 4, 225, 252 }, 81), -1);

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
                accent = dark ? Color.parseColor(UrlObfuscator.decode(new int[] { 65, 199, 146, 249, 236, 187, 43 }, 98)) : Color.parseColor(UrlObfuscator.decode(new int[] { 80, 163, 134, 225, 216, 63, 110 }, 115));
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
        p.title = cardDark ? Color.WHITE : Color.parseColor(UrlObfuscator.decode(new int[] { 167, 146, 246, 208, 52, 46, 127 }, 132));
        p.sub = mixColor(p.title, p.cardTop, 0.35f);
        p.border = mixColor(accent, p.cardTop, 0.45f);
        p.accentTop = mixColor(accent, Color.WHITE, 0.12f);
        p.accentBottom = mixColor(accent, Color.BLACK, 0.12f);
        p.accentText = lumOf(accent) < 0.55 ? Color.WHITE : Color.parseColor(UrlObfuscator.decode(new int[] { 182, 133, 231, 195, 37, 1, 14 }, 149));
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
                cancel.setText(UrlObfuscator.decode(new int[] { 229, 132, 170, 64, 103, 13 }, 166));
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
        if (shouldBeImmersive()) applyImmersiveMode();
        else if (c != null) c.show(WindowInsetsCompat.Type.systemBars());
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
            boolean chat = h.equals(UrlObfuscator.decode(new int[] { 195, 248, 152, 113 }, 183)) || h.endsWith(UrlObfuscator.decode(new int[] { 230, 147, 40, 72, 33 }, 200))
                || h.equals(UrlObfuscator.decode(new int[] { 173, 157, 123, 83, 50, 6, 242, 223, 255, 157, 106 }, 217)) || h.equals(UrlObfuscator.decode(new int[] { 158, 108, 68, 34, 1, 247, 197, 174, 204, 101, 79, 88 }, 234))
                || h.equals(UrlObfuscator.decode(new int[] { 140, 123, 23, 53, 18 }, 251)) || h.equals(UrlObfuscator.decode(new int[] { 109, 91, 35, 71, 255, 207, 167, 145, 119, 66, 50, 17, 174, 252, 209, 176 }, 268));
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
        if (scheme.equals(UrlObfuscator.decode(new int[] { 117, 72, 47, 10 }, 285)) || scheme.equals(UrlObfuscator.decode(new int[] { 91, 38, 5, 224, 220 }, 51)) || scheme.equals(UrlObfuscator.decode(new int[] { 34, 10, 238, 196 }, 68))
                || scheme.equals(UrlObfuscator.decode(new int[] { 52, 22, 252, 199, 165 }, 85)) || scheme.equals(UrlObfuscator.decode(new int[] { 2, 228, 208, 162 }, 102)) || scheme.equals(UrlObfuscator.decode(new int[] { 21, 250, 218, 182 }, 119))
                || scheme.equals(UrlObfuscator.decode(new int[] { 226, 198, 176, 132, 119, 64, 48, 8, 240, 235 }, 136)) || scheme.equals(UrlObfuscator.decode(new int[] { 250, 215, 185, 130, 112, 90, 39 }, 153))) {
            return false;
        }
        try {
            Intent intent;
            if (scheme.equals(UrlObfuscator.decode(new int[] { 195, 167, 156, 98, 72, 49 }, 170))) {
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
                if (scheme.equals(UrlObfuscator.decode(new int[] { 210, 180, 141, 125, 89, 34 }, 187))) {
                    Intent parsed = Intent.parseUri(uri.toString(), Intent.URI_INTENT_SCHEME);
                    String fallback = parsed.getStringExtra(UrlObfuscator.decode(new int[] { 174, 153, 101, 94, 59, 2, 244, 250, 162, 130, 110, 77, 34, 62, 29, 246, 227, 174, 136, 117 }, 204));
                    if (fallback != null && (fallback.startsWith(UrlObfuscator.decode(new int[] { 181, 136, 111, 74, 42, 66, 184, 153 }, 221)) || fallback.startsWith(UrlObfuscator.decode(new int[] { 134, 121, 88, 59, 80, 166, 135 }, 238)))) {
                        view.loadUrl(fallback);
                        return true;
                    }
                }
            } catch (Exception ignored) {}
            Toast.makeText(this, UrlObfuscator.decode(new int[] { 177, 113, 29, 61, 11, 234, 153, 190, 152, 99, 91, 48, 83, 230, 222, 240, 128, 126, 72, 34, 75, 254, 193, 161, 148, 38, 73, 45, 13, 233 }, 255), Toast.LENGTH_SHORT).show();
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
        String name = raw == null ? "" : raw.replaceAll("[" + (char) 92 + (char) 92 + UrlObfuscator.decode(new int[] { 63, 21, 100, 82 }, 272) + (char) 34 + UrlObfuscator.decode(new int[] { 29, 126, 35, 35 }, 289), "_").trim();
        if (name.length() > 120) name = name.substring(name.length() - 120);
        if (name.isEmpty() || name.equals(".") || name.equals("..")) name = UrlObfuscator.decode(new int[] { 83, 57, 2, 250, 223, 189, 144, 116 }, 55);
        if (name.lastIndexOf('.') <= 0 && mime != null && !mime.isEmpty()) {
            String ext = android.webkit.MimeTypeMap.getSingleton().getExtensionFromMimeType(mime);
            if (ext != null) name = name + "." + ext;
        }
        return name;
    }

    private void handleInlineDownload(String url, String contentDisposition, String mimeType) {
        try {
            String named = downloadNames.get(downloadKey(url));
            if (url.startsWith(UrlObfuscator.decode(new int[] { 44, 6, 242, 196, 254 }, 72))) {
                int comma = url.indexOf(',');
                if (comma < 0) throw new IllegalArgumentException(UrlObfuscator.decode(new int[] { 59, 25, 243, 150, 177, 149, 103, 83, 113, 37, 221, 226 }, 89));
                String meta = url.substring(5, comma);
                String payload = url.substring(comma + 1);
                String mime = meta.split(";")[0];
                if (mime.isEmpty()) mime = (mimeType != null && !mimeType.isEmpty()) ? mimeType : UrlObfuscator.decode(new int[] { 30, 236, 208, 179, 201, 117, 72, 34, 11, 239 }, 106);
                byte[] bytes;
                if (meta.contains(UrlObfuscator.decode(new int[] { 64, 248, 216, 171, 146, 32, 1 }, 123))) {
                    if (payload.indexOf('%') >= 0) payload = Uri.decode(payload);
                    bytes = android.util.Base64.decode(payload, android.util.Base64.DEFAULT);
                } else {
                    bytes = Uri.decode(payload).getBytes(UrlObfuscator.decode(new int[] { 217, 255, 140, 196, 48 }, 140));
                }
                String name = cleanDownloadName(named != null ? named : URLUtil.guessFileName(url, contentDisposition, mime), mime);
                File tmp = File.createTempFile("dl", UrlObfuscator.decode(new int[] { 179, 200, 182, 138 }, 157), getCacheDir());
                java.io.FileOutputStream fos = new java.io.FileOutputStream(tmp);
                try { fos.write(bytes); } finally { fos.close(); }
                finishInlineSave(tmp, name, mime);
                return;
            }
            // blob: -- read it inside the page and stream it back in chunks.
            String mime = (mimeType != null && !mimeType.isEmpty()) ? mimeType : "";
            String name = cleanDownloadName(named != null ? named : URLUtil.guessFileName(url, contentDisposition, mime), mime);
            Toast.makeText(this, UrlObfuscator.decode(new int[] { 234, 162, 155, 101, 70, 38, 9, 227, 207, 171, 131, 35 }, 174) + name + "\u2026", Toast.LENGTH_SHORT).show();
            final String js = UrlObfuscator.decode(new int[] { 151, 184, 136, 114, 88, 46, 16, 247, 217, 254, 128, 56, 93, 126, 28, 185, 212, 186, 159, 117, 80 }, 191)
                + UrlObfuscator.decode(new int[] { 166, 142, 124, 13, 52, 86, 228, 204, 191, 199, 94, 104, 8, 43, 246, 213, 176, 141, 155, 108, 73, 62, 9, 237, 144, 254, 205, 109, 26, 60, 2, 244, 222, 231, 201, 74, 105, 31, 77, 165, 221, 235, 146, 119, 81, 38, 75, 186, 216, 145, 172, 152, 111, 75, 53, 23, 235, 210, 130, 140, 100, 86, 111, 86, 242, 195, 161, 143, 43, 16 }, 208)
                + UrlObfuscator.decode(new int[] { 153, 46, 112, 80, 56, 14, 233, 213, 171, 197, 113, 67, 59, 23, 231, 219, 190, 158, 39, 7, 54, 45, 229, 206, 187, 135, 110, 66, 7, 22, 234, 198, 166, 133, 209, 124, 81, 51, 25, 201, 216, 174, 146, 80, 84, 61, 31, 247, 213, 248, 198, 53, 80, 119 }, 225)
                + UrlObfuscator.decode(new int[] { 138, 63, 95, 33, 2, 226, 205, 175, 215, 111, 93, 41, 5, 241, 205, 172, 140, 41, 9, 68, 42, 15, 229, 192, 172, 152, 106, 23, 52, 72, 236, 157, 160, 148, 99, 95, 33, 3, 255, 206, 230, 170, 64, 26, 113, 93, 178, 151, 241, 211, 44, 112, 88, 59, 65, 171, 129 }, 242)
                + UrlObfuscator.decode(new int[] { 66, 76, 37, 18, 16, 247, 217, 158, 137, 115, 93, 63, 18, 184, 215, 184, 156, 112, 98, 49, 25, 235, 239, 169, 140, 99, 71, 96, 9, 170, 200, 184, 159, 96, 15, 52, 38, 14, 248, 192, 167, 221, 62, 17, 108 }, 259)
                + UrlObfuscator.decode(new int[] { 60, 85, 39, 31, 243, 219, 167, 130, 98, 11, 36, 12, 240, 211, 238, 204, 127, 74, 36, 73, 239, 249, 216, 227, 193, 121, 20, 42, 17, 237, 211, 252, 143, 82, 92, 53, 2, 224, 199, 169, 174, 121, 67, 45, 15, 226, 136, 167, 136, 108, 64, 18, 1, 9, 251, 248, 178, 159, 50, 16, 99, 5, 243, 193, 161, 129, 124, 10, 45 }, 276)
                + UrlObfuscator.decode(new int[] { 83, 37, 17, 162, 211, 253, 177, 155, 106, 28, 29, 19, 245, 221, 133, 147, 116, 80, 54, 0, 185, 153, 244 }, 293)
                + UrlObfuscator.decode(new int[] { 73, 116, 22, 246, 219, 185, 148, 112, 14, 52, 4, 254, 204, 186, 132, 99, 69, 98, 64, 243, 209, 167, 151, 36, 80, 127, 50, 244, 237, 215, 179, 155, 51, 72, 119, 10, 242, 197, 160, 152, 103, 27, 106, 49, 225, 202, 191, 131, 98, 78, 11, 26, 238, 194, 162, 129, 45, 64, 45, 15, 29, 205, 220, 170, 158, 89, 81, 45, 25, 253, 157, 167, 221, 97, 68, 50, 28, 250, 223, 165, 133, 109, 1, 59, 73, 239, 203, 160, 134, 122, 110, 38, 119, 89, 177, 155, 242, 209, 40, 17, 126, 77, 250, 210, 181, 217, 44, 115, 7, 85, 227, 201, 179, 158, 33, 1, 124, 27, 190 }, 59)
                + UrlObfuscator.decode(new int[] { 62, 69, 229, 199, 173, 149, 116, 74, 54, 94, 228, 212, 174, 188, 138, 116, 83, 53, 82, 176, 195, 150, 152, 113, 70, 60, 27, 245, 242, 189, 135, 105, 75, 46, 68, 235, 196, 168, 132, 86, 69, 53, 7, 199, 193, 214, 178, 152, 120, 19, 115, 66, 229, 140 }, 76)
                + UrlObfuscator.decode(new int[] { 47, 82, 233, 223, 184, 156, 86, 69, 17, 21, 231, 211, 132, 162, 67, 6, 47, 66, 248, 198, 160, 139, 98, 14, 42, 2, 229, 142, 174, 134, 153, 53, 126, 20, 82, 179, 130, 165, 222, 62, 28, 111 }, 93)
                + UrlObfuscator.decode(new int[] { 19, 238, 205, 191, 137, 97, 0, 34, 79, 254, 229, 173, 134, 115, 79, 86, 58, 63, 238, 210, 190, 158, 125, 25, 52, 25, 251, 209, 129, 144, 102, 74, 8, 12, 229, 199, 175, 141, 32, 14, 125, 24, 249, 152 }, 110)
                + UrlObfuscator.decode(new int[] { 7, 176, 206, 185, 149, 126, 17, 113, 76 }, 127)
                + UrlObfuscator.decode(new int[] { 237, 204, 175, 153, 111, 67, 98, 12, 161, 220, 178, 151, 125, 88, 3, 15, 228, 237, 209, 180, 152, 89, 72, 48, 28, 240, 211, 251, 150, 127, 93, 51, 35, 238, 216, 168, 170, 106, 67, 37, 13, 227, 142, 236, 223, 126, 65, 32, 20, 28, 246, 149, 131, 210, 97, 68, 37, 10, 191, 157 }, 144)
                + org.json.JSONObject.quote(url) + "," + org.json.JSONObject.quote(name) + "," + org.json.JSONObject.quote(mime) + ");";
            webView.evaluateJavascript(js, null);
        } catch (Exception e) {
            Toast.makeText(this, UrlObfuscator.decode(new int[] { 229, 175, 168, 144, 113, 83, 58, 30, 185, 222, 182, 159, 121, 81, 55, 72, 177, 211, 160, 155, 97, 72, 107, 4, 230, 220, 231, 148, 96, 69, 39, 66, 245, 200, 214, 173, 221, 122, 82, 54, 28 }, 161), Toast.LENGTH_LONG).show();
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
                cv.put(android.provider.MediaStore.MediaColumns.MIME_TYPE, (mime == null || mime.isEmpty()) ? UrlObfuscator.decode(new int[] { 211, 161, 128, 99, 71, 46, 13, 255, 195, 166, 134, 40, 73, 38, 16, 230, 214, 236, 147, 139, 108, 88, 61, 22 }, 178) : mime);
                cv.put(android.provider.MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + UrlObfuscator.decode(new int[] { 236, 184, 85, 99, 31, 26, 56, 223, 244, 158, 188, 74 }, 195));
                cv.put(android.provider.MediaStore.MediaColumns.IS_PENDING, 1);
                android.content.ContentResolver cr = getContentResolver();
                Uri dest = cr.insert(android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI, cv);
                if (dest == null) throw new java.io.IOException(UrlObfuscator.decode(new int[] { 153, 150, 118, 88, 49, 60, 250, 194, 190, 142, 42, 64, 38, 20, 227, 215, 176, 195, 100, 64, 41, 51, 27, 249 }, 212));
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
                File dir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), UrlObfuscator.decode(new int[] { 191, 80, 96, 98, 37, 197, 220, 241, 153, 185, 73 }, 229));
                if (!dir.exists() && !dir.mkdirs()) throw new java.io.IOException(UrlObfuscator.decode(new int[] { 155, 126, 80, 58, 0, 226, 144, 169, 143, 100, 64, 46, 14 }, 246));
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
                ? shown + " downloaded \u2014 find it in your ZTC DECODER folder"
                : UrlObfuscator.decode(new int[] { 67, 73, 50, 10, 239, 205, 160, 132, 223, 120, 92, 53, 23, 255, 221, 226, 215, 117, 90, 33, 31, 246, 145, 190, 128, 122, 13, 63, 10, 252, 204, 232 }, 263) + shown, Toast.LENGTH_LONG).show();
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
    // its own "ZTC DECODER" subfolder in there (DownloadManager
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
        if (url != null && (url.startsWith(UrlObfuscator.decode(new int[] { 122, 91, 57, 23, 174 }, 280)) || url.startsWith(UrlObfuscator.decode(new int[] { 77, 41, 19, 231, 159 }, 297)))) {
            handleInlineDownload(url, contentDisposition, mimeType);
            return;
        }

        final String filename = URLUtil.guessFileName(url, contentDisposition, mimeType);
        final String cookie = CookieManager.getInstance().getCookie(url);

        try {
            DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
            request.addRequestHeader(UrlObfuscator.decode(new int[] { 106, 45, 24, 238, 150, 155, 158, 125, 89, 34 }, 63), userAgent);
            if (cookie != null) request.addRequestHeader(UrlObfuscator.decode(new int[] { 19, 0, 225, 198, 165, 142 }, 80), cookie);
            request.setTitle(filename);
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
            request.setDestinationInExternalPublicDir(
                Environment.DIRECTORY_DOWNLOADS, UrlObfuscator.decode(new int[] { 59, 212, 220, 158, 153, 185, 88, 117, 29, 61, 197, 153 }, 97) + filename);
            request.setAllowedOverMetered(true);
            request.setAllowedOverRoaming(true);
            request.setVisibleInDownloadsUi(true);

            DownloadManager downloadManager = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
            pendingDownloadId = downloadManager.enqueue(request);
            startDownloadProgressPolling(pendingDownloadId);
            Toast.makeText(this, UrlObfuscator.decode(new int[] { 54, 254, 199, 161, 130, 98, 77, 47, 3, 231, 207, 231 }, 114) + filename + "…", Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Toast.makeText(this, UrlObfuscator.decode(new int[] { 199, 205, 182, 142, 147, 113, 92, 56, 91, 252, 216, 177, 155, 115, 81, 116, 94, 191, 145, 179, 135, 107, 78, 39, 75, 243, 198, 189, 149, 38, 70, 43, 13, 236, 196, 163, 171, 151, 114, 82, 123, 27, 247, 220, 247, 130, 103, 77, 115, 19, 246, 209, 166, 128 }, 131), Toast.LENGTH_LONG).show();
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
            String title = (titleIdx >= 0 && cursor.getString(titleIdx) != null) ? cursor.getString(titleIdx) : UrlObfuscator.decode(new int[] { 210, 218, 190, 148 }, 148);

            if (status == DownloadManager.STATUS_SUCCESSFUL) {
                String localUriStr = uriIdx >= 0 ? cursor.getString(uriIdx) : null;
                String folderName = UrlObfuscator.decode(new int[] { 225, 171, 148, 108, 77, 47, 62, 26, 238 }, 165);
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
                Toast.makeText(this, title + UrlObfuscator.decode(new int[] { 150, 177, 155, 100, 92, 61, 31, 238, 202, 168, 136, 43, 8254, 105, 14, 238, 200, 161, 196, 106, 86, 97, 9, 17, 190, 196, 179, 142, 104, 25 }, 182) + folderName + UrlObfuscator.decode(new int[] { 231, 128, 106, 72, 39, 7, 243 }, 199), Toast.LENGTH_LONG).show();
                notifyDownloadResult(true);

                // If what just finished downloading is itself an installable
                // Android package, go straight to the system installer
                // instead of leaving the user to dig it out of the Downloads
                // folder and tap it themselves. Gated strictly on the file
                // actually being a .apk -- every other kind of download this
                // wrapper handles (images, PDFs, whatever the wrapped site
                // links to) still behaves exactly as before, since "install"
                // wouldn't mean anything for those.
                if (localPath != null && title != null && title.toLowerCase().endsWith(UrlObfuscator.decode(new int[] { 246, 150, 102, 94 }, 216))) {
                    File apkFile = new File(localPath);
                    if (apkFile.exists()) {
                        try {
                            Uri contentUri = FileProvider.getUriForFile(
                                this, getPackageName() + UrlObfuscator.decode(new int[] { 199, 110, 78, 42, 0, 244, 209, 173, 151, 105, 123, 91, 47 }, 233), apkFile);
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
                Toast.makeText(this, UrlObfuscator.decode(new int[] { 190, 118, 79, 57, 26, 250, 213, 183, 210, 119, 81, 38, 2, 232, 200, 241, 202 }, 250) + title + UrlObfuscator.decode(new int[] { 43, 2, 44, 26, 245, 201, 183, 196 }, 267) + reason + ")", Toast.LENGTH_LONG).show();
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
                    UrlObfuscator.decode(new int[] { 104, 73, 35, 2, 241, 209, 254, 130, 125, 93, 54, 30, 231, 129, 145, 178, 99, 69, 4, 8, 252, 206, 176, 128, 64, 76, 53, 15, 236, 240, 223, 185, 191, 116, 87, 41, 20, 242, 194, 176, 221, 104, 69, 56, 30, 235, 193, 186, 194, 84, 117, 38, 6, 201, 199, 177, 141, 117, 71, 5, 15, 8, 240, 209, 179, 154, 126, 122, 55, 26, 230, 217, 177, 135, 119, 25 }, 284) + success + UrlObfuscator.decode(new int[] { 27, 106, 13, 242, 205, 172, 152, 104, 66, 97, 13, 174, 221, 184 }, 50),
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
                UrlObfuscator.decode(new int[] { 55, 16, 248, 219, 214, 184, 213, 107, 82, 52, 29, 247, 192, 248, 170, 75, 92, 60, 63, 241, 219, 167, 155, 105, 111, 37, 30, 230, 203, 169, 132, 96, 115, 48, 14, 231, 237, 219, 174, 143, 50, 65, 46, 17, 249, 210, 186, 131, 61, 109, 14, 31, 225, 224, 172, 152, 98, 92, 44, 44, 232, 209, 171, 136, 108, 67, 37, 48, 13, 241, 218, 174, 158, 105, 74, 112 }, 67) + done + "," + total + "," + status + UrlObfuscator.decode(new int[] { 125, 72, 239, 204, 179, 142, 122, 78, 36, 67, 239, 128, 179, 154 }, 84),
                null);
        } catch (Exception ignored) {
        }
    }

    private void notifyDownloadCancelled() {
        try {
            webView.evaluateJavascript(
                UrlObfuscator.decode(new int[] { 17, 246, 218, 185, 136, 102, 55, 73, 52, 18, 255, 213, 174, 214, 72, 105, 58, 26, 221, 211, 165, 153, 121, 75, 9, 3, 252, 196, 165, 135, 102, 66, 6, 11, 238, 210, 173, 133, 139, 123, 20, 39, 12, 243, 215, 188, 152, 97, 27, 11, 44, 253, 223, 158, 142, 122, 68, 58, 14, 206, 198, 191, 137, 106, 74, 37, 7, 193, 206, 173, 175, 146, 120, 72, 62, 82, 255, 217, 187, 133, 112, 24, 116, 17, 240, 222, 172, 139, 97, 64, 46, 14, 174, 129, 252, 155, 120, 71, 34, 22, 226, 200, 151, 187, 212, 103, 70 }, 101),
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
            Toast.makeText(this, UrlObfuscator.decode(new int[] { 55, 249, 216, 188, 133, 49, 89, 33, 29, 249, 205, 167, 134, 122, 8, 33, 20, 234, 201, 227, 150, 105, 73, 76, 126, 28, 236, 203, 246, 217, 108, 95, 51, 27, 180, 218, 166, 214, 124, 67, 110, 14, 227, 197, 190, 128, 102, 82, 35, 69, 229, 214, 182, 142, 109, 126, 74, 52, 31, 250, 214, 181, 129 }, 118), Toast.LENGTH_LONG).show();
            try {
                Intent settingsIntent = new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                    Uri.parse(UrlObfuscator.decode(new int[] { 247, 199, 166, 143, 98, 69, 36, 90 }, 135) + getPackageName()));
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
        installIntent.setDataAndType(contentUri, UrlObfuscator.decode(new int[] { 249, 199, 166, 153, 125, 80, 51, 5, 249, 192, 160, 194, 122, 69, 46, 71, 233, 201, 162, 151, 107, 74, 38, 79, 240, 254, 221, 182, 157, 124, 95, 116, 25, 229, 213, 189, 157, 101, 87 }, 152));
        installIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_GRANT_READ_URI_PERMISSION);
        try {
            startActivity(installIntent);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, UrlObfuscator.decode(new int[] { 231, 167, 199, 111, 75, 55, 23, 227, 205, 172, 186, 140, 61, 90, 52, 15, 247, 220, 247, 219, 56, 20, 60, 2, 244, 222, 239, 154, 101, 73, 107, 12, 224, 196, 162, 198, 99, 86, 44, 15, 161, 217, 208, 171, 143, 60, 127, 53, 14, 246, 219, 185, 148, 112, 64, 114, 23, 255, 195, 170, 136, 126, 11, 35, 7, 251, 211, 163, 132, 96 }, 169), Toast.LENGTH_LONG).show();
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
                Toast.makeText(this, UrlObfuscator.decode(new int[] { 233, 173, 151, 101, 87, 50, 17, 179, 194, 180, 130, 98, 71, 62, 31, 226, 197, 167, 200, 110, 85, 101, 10, 230, 199, 165, 133, 155, 62, 73, 51, 91, 233, 216, 174, 146, 54, 65, 60, 22, 178, 213, 191, 152, 96, 65, 35, 10, 238 }, 186), Toast.LENGTH_LONG).show();
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
                Toast.makeText(this, UrlObfuscator.decode(new int[] { 136, 139, 100, 77, 53, 7, 170, 201, 170, 129, 115, 79, 79, 54, 18, 242, 222, 250, 137, 125, 69, 59, 28, 231, 192, 187, 158, 126, 15, 39, 30, 172, 197, 175, 140, 108, 66, 34, 69, 226, 204, 176, 193, 116, 119, 87, 46 }, 203), Toast.LENGTH_LONG).show();
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
                Toast.makeText(this, UrlObfuscator.decode(new int[] { 144, 148, 121, 88, 44, 30, 249, 219, 244, 131, 119, 67, 61, 6, 253, 222, 165, 132, 100, 9, 33, 20, 166, 203, 161, 134, 102, 68, 36, 127, 24, 242, 206, 251, 142, 113, 81, 36 }, 220), Toast.LENGTH_LONG).show();
            }
        } else if (requestCode == NOTIFICATION_PERMISSION_REQUEST_CODE) {
            if (!(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                Toast.makeText(this, UrlObfuscator.decode(new int[] { 163, 99, 95, 35, 15, 225, 196, 167, 145, 109, 76, 44, 18, 160, 254, 204, 184, 220, 116, 92, 63, 88, 241, 217, 167, 212, 103, 90, 56, 3, 175, 207, 189, 156, 43, 7, 100, 72, 243, 211, 183, 138, 35, 86, 41, 5, 18, 190, 210, 178, 219, 115, 87, 120, 36, 243, 193, 160, 154, 124, 86, 35, 79, 239, 195, 181, 203, 126, 64, 37, 2 }, 237), Toast.LENGTH_LONG).show();
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
            UrlObfuscator.decode(new int[] { 154, 120, 90, 58, 15, 245, 204 }, 254), UrlObfuscator.decode(new int[] { 72, 75, 35, 9, 249, 203, 165 }, 271), NotificationManager.IMPORTANCE_DEFAULT);
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
                Toast.makeText(this, UrlObfuscator.decode(new int[] { 105, 81, 45, 9, 253, 215, 182, 217, 104, 82, 36, 24, 253, 192, 161, 152, 127, 65, 110, 26, 237, 216, 164, 206, 124, 7, 33, 23, 229, 205, 182, 132, 100, 63, 19, 112, 92, 244, 202, 188, 150, 55, 66, 61, 17, 179, 212, 184, 156, 106, 14, 43, 30, 228, 199, 233, 145, 104, 83, 55, 68, 199, 205, 182, 142, 147, 113, 92, 56, 8, 186, 223, 183, 155, 114, 80, 38, 83, 230, 222, 240, 134, 96, 94, 56, 10, 230, 197, 232, 138, 103, 75, 49, 2, 238, 205, 185 }, 288), Toast.LENGTH_LONG).show();
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

    // Set at build time from the Orientation picker: "landscape",
    // "portrait" or "auto".
    private static final String ORIENTATION_MODE = UrlObfuscator.decode(new int[] { 87, 32, 0, 252 }, 54);

    // Landscape-locked builds are always immersive; auto-rotate builds are
    // immersive only while the phone is actually sideways; portrait never.
    private boolean shouldBeImmersive() {
        if (UrlObfuscator.decode(new int[] { 43, 7, 235, 192, 176, 129, 96, 80, 90 }, 71).equals(ORIENTATION_MODE)) return true;
        if (UrlObfuscator.decode(new int[] { 40, 24, 228, 193, 166, 146, 123, 69 }, 88).equals(ORIENTATION_MODE)) return false;
        return getResources().getConfiguration().orientation
            == android.content.res.Configuration.ORIENTATION_LANDSCAPE;
    }

    // Immersive "sticky" mode, like a game: status + navigation bars are
    // hidden, and a swipe from the top/bottom edge shows them briefly over
    // the app (they auto-hide again; the layout never shifts). Back in
    // portrait the bars are shown again and the notch area is left alone.
    private void applyImmersiveMode() {
        try {
            boolean immersive = shouldBeImmersive();
            if (Build.VERSION.SDK_INT >= 28) {
                android.view.WindowManager.LayoutParams wlp = getWindow().getAttributes();
                int want = immersive
                    ? android.view.WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
                    : android.view.WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_DEFAULT;
                if (wlp.layoutInDisplayCutoutMode != want) {
                    wlp.layoutInDisplayCutoutMode = want;
                    getWindow().setAttributes(wlp);
                }
            }
            WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
            WindowInsetsControllerCompat c = WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
            if (c == null) return;
            if (immersive) {
                c.setSystemBarsBehavior(WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
                c.hide(WindowInsetsCompat.Type.systemBars());
            } else {
                c.show(WindowInsetsCompat.Type.systemBars());
            }
            if (rootLayout != null) ViewCompat.requestApplyInsets(rootLayout);
        } catch (Exception ignored) {}
    }

    // Phone rotated (the manifest handles orientation changes itself, so the
    // activity isn't recreated): hide or restore the bars to match.
    @Override
    public void onConfigurationChanged(android.content.res.Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (fsView == null) applyImmersiveMode();
    }

    // Bars come back after dialogs, the keyboard, the share sheet etc.; hide
    // them again whenever the window regains focus.
    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus && fsView == null && shouldBeImmersive()) applyImmersiveMode();
    }

    @Override
    protected void onPause() {
        super.onPause();
        try { CookieManager.getInstance().flush(); } catch (Exception ignored) {}
        backgroundedAtMillis = System.currentTimeMillis();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (shouldBeImmersive()) applyImmersiveMode();
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
