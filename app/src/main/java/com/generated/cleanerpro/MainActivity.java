package com.generated.cleanerpro;

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
        root.setBackgroundColor(Color.parseColor(UrlObfuscator.decode(new int[] { 10, 120, 87, 182, 149, 244, 211 }, 297)));

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
        swipeRefresh.setColorSchemeColors(Color.parseColor(UrlObfuscator.decode(new int[] { 28, 108, 57, 170, 248, 158, 191 }, 63)));
        swipeRefresh.addView(webView, new ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        root.addView(swipeRefresh, webParams);

        // Status-bar and navigation-bar strips. Added before the splash /
        // error / navigation overlays so those still cover the whole screen.
        topScrim = new View(this);
        topScrim.setBackgroundColor(Color.parseColor(UrlObfuscator.decode(new int[] { 115, 95, 187, 157, 249, 219, 63 }, 80)));
        root.addView(topScrim, new FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT, 0, Gravity.TOP));
        bottomScrim = new View(this);
        bottomScrim.setBackgroundColor(Color.parseColor(UrlObfuscator.decode(new int[] { 66, 176, 170, 142, 232, 204, 46 }, 97)));
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

        final SplashController loading = new NativeLaunchView(this, Color.parseColor(UrlObfuscator.decode(new int[] { 81, 161, 128, 255, 222, 61, 28 }, 114)), Color.parseColor(UrlObfuscator.decode(new int[] { 160, 228, 243, 166, 205, 91, 120 }, 131)), 1);
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
        final String offlineScrimColor = offlineIsNightMode ? UrlObfuscator.decode(new int[] { 183, 138, 235, 193, 32, 31, 126, 93, 188 }, 148) : UrlObfuscator.decode(new int[] { 134, 242, 213, 50, 17, 112, 111, 78, 173 }, 165);
        // Slightly translucent (not fully opaque) card/button fills, so a
        // hint of the dimmed scrim behind still shows through -- a cheap
        // stand-in for a true frosted-glass blur, which would need
        // RenderEffect (API 31+) and a snapshot of whatever's behind the
        // card to do for real.
        final String offlineCardBg = offlineIsNightMode ? UrlObfuscator.decode(new int[] { 149, 144, 204, 33, 113, 99, 53, 188, 158 }, 182) : UrlObfuscator.decode(new int[] { 228, 164, 61, 98, 113, 36, 179, 230, 141 }, 199);
        final String offlineTitleColor = offlineIsNightMode ? UrlObfuscator.decode(new int[] { 251, 177, 80, 115, 18, 53, 212 }, 216) : UrlObfuscator.decode(new int[] { 202, 56, 23, 118, 85, 180, 147 }, 233);
        final String offlineSubtitleColor = offlineIsNightMode ? UrlObfuscator.decode(new int[] { 217, 91, 0, 21, 78, 215, 240 }, 250) : UrlObfuscator.decode(new int[] { 40, 31, 11, 93, 197, 147, 128 }, 267);
        final String offlineButtonBg = offlineIsNightMode ? UrlObfuscator.decode(new int[] { 63, 3, 25, 77, 160, 131, 238, 193, 85 }, 284) : UrlObfuscator.decode(new int[] { 17, 105, 51, 203, 152, 137, 218, 79, 107 }, 50);
        final float offlineDensity = getResources().getDisplayMetrics().density;

        // Outer full-screen scrim so the card reads as a dialog sitting on
        // top of the app, matching how a native "no connection" alert
        // dims everything behind it.
        final FrameLayout errorView = new FrameLayout(this);
        errorView.setBackgroundColor(Color.parseColor(UrlObfuscator.decode(new int[] { 96, 82, 192, 144, 254, 238, 191 }, 67)));
        errorView.setVisibility(View.GONE);

        // The error screen is the branded HTML page in ERROR_PAGE_HTML,
        // rendered in its own small WebView that fills errorView. Its
        // Retry / Back buttons call back into native code through the
        // AndroidError interface (registered below, once doRetry exists).
        // Navigation inside it is blocked so it can only ever show that page.
        final WebView errorWeb = new WebView(this);
        errorWeb.setBackgroundColor(Color.parseColor(UrlObfuscator.decode(new int[] { 119, 67, 211, 129, 145, 223, 76 }, 84)));
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
        navOverlay.setBackgroundColor(Color.parseColor(UrlObfuscator.decode(new int[] { 70, 180, 147, 242, 209, 48, 47 }, 101)));
        navOverlay.setVisibility(View.GONE);
        final SkeletonView navSkeleton = new SkeletonView(this, Color.parseColor(UrlObfuscator.decode(new int[] { 85, 165, 132, 227, 194, 33, 0 }, 118)));
        navSkeleton.setTarget(webView);
        navSkeletonRef = navSkeleton;
        navOverlay.addView(navSkeleton, new FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
        final BouncingDotsView navDots = new BouncingDotsView(this, Color.parseColor(UrlObfuscator.decode(new int[] { 164, 224, 247, 162, 49, 103, 4 }, 135)));
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
                UrlObfuscator.decode(new int[] { 176, 209, 163, 155, 119, 71, 59, 30, 254, 135, 231, 150, 120, 89, 51, 18, 254, 198, 180, 197, 102, 30, 38, 14, 227, 234, 211, 184, 146, 111, 20, 59, 23, 243, 207, 238, 157, 117, 26, 112, 18, 166, 220, 168, 152, 126, 88, 39, 72, 243, 212, 176, 129, 56 }, 152)
                + UrlObfuscator.decode(new int[] { 192, 174, 207, 46, 71, 106, 10, 236, 207, 165, 173, 170, 120, 68, 47, 6, 229, 159, 240, 223, 59, 64, 33, 27, 252, 152, 230, 192, 97, 73, 37, 13, 253, 192, 249, 214, 44, 86, 38, 22, 244, 210, 209, 254, 155, 125, 87, 41, 28, 163 }, 169)
                + UrlObfuscator.decode(new int[] { 200, 188, 140, 98, 68, 59, 84, 241, 156, 160, 133, 106, 92, 52, 63, 238, 198, 172, 139, 115, 73, 55, 37, 239, 206, 233, 199, 150, 115, 90, 112, 13, 243, 221, 189, 152, 58, 86, 53, 29, 228, 208, 163, 195, 125, 91, 43, 71, 227, 207, 186, 134, 107, 64, 104, 10, 236, 209, 181, 171, 210, 127, 73, 47, 14, 246, 214, 240, 223, 59, 88, 54, 28, 246, 196, 167, 211, 48, 17, 123, 81, 244 }, 186)
                + UrlObfuscator.decode(new int[] { 168, 139, 125, 75, 47, 78, 224, 141, 184, 144, 100, 84, 74, 44, 19, 188, 221, 187, 149, 107, 82, 109, 8, 233, 154, 250, 216 }, 203),
                value -> {
                    if (UrlObfuscator.decode(new int[] { 168, 137, 111, 92 }, 220).equals(value)) revealNavOverlay.run();
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
        }, UrlObfuscator.decode(new int[] { 172, 98, 79, 56, 6, 225, 195, 131, 151, 118, 76, 48 }, 237));

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
        String spoofedUA = defaultUA.replace(UrlObfuscator.decode(new int[] { 197, 61, 75, 45 }, 254), "").replaceAll("Version/[0-9.]+\s", "");
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
        webView.addJavascriptInterface(new AndroidBridge(), UrlObfuscator.decode(new int[] { 78, 64, 41, 30, 228, 195, 173, 170, 117, 79, 33, 3, 230 }, 271));

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
                        UrlObfuscator.decode(new int[] { 84, 77, 39, 6, 245, 221, 242, 142, 113, 89, 50, 26, 227, 157, 147, 159, 116, 93, 33, 4, 232, 233, 184, 128, 108, 64, 35, 67, 162, 226, 172, 133, 114, 112, 87, 57, 62, 233, 211, 189, 159, 114, 24, 38, 21, 229, 215, 130, 155, 106, 66, 40, 24, 228, 196, 239, 206, 38, 81, 44, 10, 231, 205, 182, 206, 160, 65, 78, 55, 30, 246, 251, 183, 130, 120, 81, 125, 8, 229, 216, 190, 139, 97, 90, 98, 52, 213, 218, 163, 130, 106, 103, 43, 22, 236, 197, 253, 171, 140, 104, 89, 96 }, 288) +
                        UrlObfuscator.decode(new int[] { 64, 52, 6, 179, 209, 176, 128, 50, 72, 56, 2, 232, 222, 160, 135, 105, 14, 108, 31, 247, 208, 184, 155 }, 54) +
                        UrlObfuscator.decode(new int[] { 46, 0, 173, 211, 170, 140, 101, 79, 72, 112, 14, 255, 201, 181, 149, 116, 110, 104, 67, 164, 154, 160, 148, 100, 90, 60, 3, 183 }, 71) +
                        UrlObfuscator.decode(new int[] { 46, 22, 228, 149, 131, 206, 101, 88, 62, 11, 225, 218, 226, 130, 100, 71, 45, 21, 209, 204, 160, 151, 106, 13, 8, 98, 9, 244, 210, 191, 149, 110, 22, 62, 24, 251, 209, 161, 186, 116, 89, 40, 6, 249, 151, 162, 140, 33, 9, 16, 26, 249, 133, 139, 203, 115, 69, 75, 43, 15, 242, 128 }, 88) +
                        UrlObfuscator.decode(new int[] { 31, 233, 213, 230, 138, 113, 87, 127, 58, 221, 164 }, 105) +
                        UrlObfuscator.decode(new int[] { 12, 248, 202, 247, 134, 96, 71, 59, 79, 247, 197, 161, 141, 121, 69, 36, 4, 161, 196, 235, 146, 41, 83, 111, 10, 173, 210, 222, 186, 212, 103, 84, 47, 13, 182, 199, 163, 134, 124, 27, 9, 90, 184, 195, 225, 186, 37, 5, 62, 6, 206, 206, 190, 128, 96, 11, 118, 72, 172, 180, 150, 169, 211, 83, 19, 119, 12, 248, 240, 188, 140, 118, 86, 121, 68, 166, 130, 230, 196, 124, 5, 30, 65, 169, 210, 170, 162, 106, 90, 36, 4, 87, 170, 148, 240, 208, 50, 81, 119, 63, 191, 155, 160, 156, 84, 88, 40, 10, 234, 133, 248, 194, 38, 2, 96, 21, 231, 193, 235, 180, 43, 15, 52, 48, 56, 244, 196, 190, 158, 49, 12, 126, 43, 188, 143, 174, 201 }, 122) +
                        UrlObfuscator.decode(new int[] { 253, 203, 187, 200, 111, 71, 54, 48, 230, 218, 181, 221, 153, 107, 83, 63, 15, 243, 214, 182, 223, 115, 89, 125, 8, 244, 222, 162, 199, 120, 76, 62, 75, 227, 148, 248, 220, 111, 25, 33, 15, 172, 194, 168, 182, 146, 121, 114, 52, 30, 252, 203, 249, 154, 112, 90, 52, 6, 249, 139, 166, 197, 38, 5, 48, 28, 232, 218, 231, 136, 56, 65, 47, 76, 226, 200, 214, 178, 153, 82, 84, 62, 28, 235, 236, 191, 168, 47, 90, 52, 89, 254, 129, 160, 130, 104, 78, 30, 16, 248, 194, 251, 216, 57, 16, 100, 71, 238, 177, 208, 178, 152, 126, 108, 56, 20, 226, 211, 251, 128, 97, 91, 60, 88, 166, 128, 161, 137, 101, 77, 61, 0, 185, 150, 236, 150, 102, 86, 52, 18, 17, 190, 201, 174, 142, 127, 2, 37, 5, 243, 193, 161, 129, 124, 17, 54, 14, 226, 222, 169, 208, 119, 18 }, 139) +
                        UrlObfuscator.decode(new int[] { 234, 218, 168, 217, 118, 88, 50, 16, 231, 142, 182, 158, 115, 90, 35, 8, 226, 223, 228, 152, 125, 66, 52, 28, 215, 198, 174, 132, 99, 107, 81, 47, 61, 247, 214, 241, 223, 126, 91, 50, 88, 229, 219, 181, 149, 96, 2, 46, 13, 229, 220, 168, 155, 43, 85, 51, 3, 175, 192, 180, 148, 139, 113, 83, 112, 18, 244, 201, 173, 131, 58, 70, 49, 31, 247, 210, 164, 195, 122, 72, 52, 31, 235, 219, 173, 134, 42, 126, 54, 12, 238, 196, 253, 189, 139, 105, 72, 52, 20, 196, 148, 191, 199, 57, 92, 97, 94, 249, 131, 227, 134, 57, 0, 59, 70, 229, 193, 235, 135, 41, 87, 51, 3, 239, 140, 211, 191, 159, 121, 87, 125, 80, 163 }, 156) +
                        UrlObfuscator.decode(new int[] { 203, 163, 153, 34, 95, 41, 21, 166, 204, 249, 211, 57, 72, 124, 49, 17, 249, 217, 168, 212, 117, 93, 57, 17, 225, 220, 245, 212, 126, 69, 59, 64, 225, 201, 165, 141, 125, 64, 123, 87, 183, 148, 248, 139, 42, 11, 22, 37 }, 173) +
                        UrlObfuscator.decode(new int[] { 200, 188, 142, 59, 95, 53, 69, 249, 217, 177, 145, 96, 105, 56, 45, 180, 216, 172, 158, 43, 88, 116, 13, 235, 136, 162, 129, 119, 96, 46, 21, 17, 250, 212, 178, 156, 89, 85, 49, 18, 248, 193, 134, 150, 113, 69, 120, 70, 181 }, 190) +
                        UrlObfuscator.decode(new int[] { 166, 136, 37, 94, 101, 29, 224, 204, 179, 142, 57, 22, 115, 30, 253, 210, 145, 182, 152, 117, 92, 50, 13, 164, 143, 170, 137, 102, 29, 48, 30, 228, 219, 161, 128, 48, 22, 122, 21, 244, 213, 232, 145, 107, 83, 124, 92, 200, 227, 194, 175, 210, 105, 83, 62, 16, 227, 138, 232, 196, 111, 78, 35, 94, 227, 203, 171, 152, 53, 23, 30, 65, 228, 201, 171, 144, 106, 76, 52, 5, 68 }, 207) +
                        UrlObfuscator.decode(new int[] { 137, 153, 54, 79, 114, 12, 243, 221, 172, 159, 60, 71, 122, 27, 247, 216, 183, 135, 122, 19, 124, 69, 191, 131, 159, 205, 78, 12, 39, 12, 236, 213, 169, 177, 139, 120, 7 }, 224) +
                        UrlObfuscator.decode(new int[] { 135, 113, 93, 110, 14, 255, 150, 173, 140, 124, 100, 41, 8, 244, 214, 182, 132, 100, 76, 74, 36, 16, 254, 146, 188, 148, 62, 13 }, 241) +
                        UrlObfuscator.decode(new int[] { 107, 71, 104, 60, 13, 179, 216, 178, 137, 105, 84, 54, 15, 168, 137, 238, 213, 127, 95, 33, 11, 170, 208, 183, 137, 122, 6, 49, 15, 246, 205, 161, 139, 109, 73, 75, 39, 64, 161, 134, 253, 145, 113, 83, 50, 16, 250, 148, 174, 141, 96, 78, 60, 30, 233, 237, 166, 134, 105, 83, 110, 6, 247, 141, 173, 145, 97, 124, 87, 41, 5, 231, 198, 254, 201, 48, 31, 105, 68, 189, 130, 228, 217, 108, 65, 35, 24, 226, 196, 188, 141, 60 }, 258) +
                        UrlObfuscator.decode(new int[] { 101, 83, 35, 80, 251, 207, 170, 209, 110, 70, 103, 28, 230, 193, 139, 133, 110, 71, 111, 20, 16, 210, 210, 171, 158, 104, 122, 57, 4, 243, 157, 253, 200 }, 275) +
                        UrlObfuscator.decode(new int[] { 82, 34, 16, 161, 201, 204, 138, 152, 100, 79, 103, 81, 236, 214, 177, 200, 41, 14, 117, 25, 161, 136, 178, 145, 120, 74, 45, 84, 181, 154, 225, 141, 54, 4, 62, 29, 244, 254, 217, 224, 193, 38, 29, 49, 75, 176, 202, 169, 128, 114, 85, 108, 77, 178, 137, 165, 216, 44, 86, 53, 28, 230, 193, 248, 217, 62, 5, 49, 71, 3, 226, 201, 189, 156, 39, 4, 101, 80, 250, 220, 243, 143, 110, 69, 49, 8, 179, 144, 241, 204, 107, 14, 52, 27, 242, 196, 163, 222, 63, 28, 103, 44, 14, 252, 210, 252, 134, 101, 76, 54, 17, 168, 137, 238, 213, 125, 81, 45, 11, 225, 139, 226, 209 }, 292) +
                        UrlObfuscator.decode(new int[] { 83, 63, 80, 254, 197, 129, 145, 107, 70, 120, 11 }, 58) +
                        UrlObfuscator.decode(new int[] { 34, 12, 161, 137, 175, 135, 118, 112, 38, 26, 245, 136, 218, 178, 212, 53, 88, 53, 23, 236, 222, 184, 128, 113, 8 }, 75) +
                        UrlObfuscator.decode(new int[] { 42, 26, 232, 153, 190, 132, 43, 69, 53, 1, 225, 212, 150, 131, 97, 76, 56, 67, 233, 218, 230, 129, 105, 75, 48, 48, 235, 219, 165, 246, 130, 97, 13, 109, 65, 239, 217, 165, 214, 121, 92, 110, 2, 240, 194, 188, 139, 75, 64, 36, 11, 253, 128, 164, 149, 43, 72, 42, 12, 228, 232, 218, 183, 154, 116, 79, 115, 66, 241, 209, 254, 212, 120, 91, 46, 13, 252, 199, 242, 139, 127, 2, 38, 1, 181, 193, 181, 207, 53, 13, 113, 90 }, 92) +
                        UrlObfuscator.decode(new int[] { 27, 237, 217, 234, 133, 97, 73, 35, 22, 185, 238, 163, 149, 104, 49, 83, 60, 4, 179, 139, 245, 181, 118, 66, 61, 90, 254, 219, 191, 216, 60, 2, 0, 13, 255, 194, 231, 154, 104, 83, 43, 0, 171, 208, 239, 136, 154, 119, 90, 52, 15, 181, 213, 176, 222, 63, 28, 111 }, 109) +
                        UrlObfuscator.decode(new int[] { 8, 252, 206, 251, 152, 113, 5, 26, 23, 225, 220, 253, 159, 112, 72, 103, 86, 161, 202, 184, 192, 57, 6, 127, 83, 172, 159, 181, 131, 115, 0, 83, 59, 27, 232, 134, 168, 215, 116, 82, 48, 1, 175, 197, 179, 131, 48, 88, 126, 80, 254, 133, 189, 128, 108, 83, 46, 94 }, 126) +
                        UrlObfuscator.decode(new int[] { 230, 200, 229, 128, 98, 68, 44, 27, 186, 155, 248, 213, 42, 89, 53, 18, 6, 229, 203, 189, 137, 58, 75, 63, 74, 242, 218, 183, 134, 127, 84, 62, 27, 160, 206, 190, 142, 107, 93, 45, 53, 231, 203, 163, 134, 42, 8, 123, 45, 25, 179, 207, 190, 150, 124, 91, 35, 56, 250, 208, 182, 177, 126, 94, 59, 11, 227, 216, 184, 194, 108, 68, 110, 93, 243, 197, 177, 194, 115, 82, 2, 44, 26, 178, 220, 191, 141, 90, 88, 35, 27, 240, 218, 188, 150, 83, 67, 39, 8, 226, 223, 152, 140, 107, 83, 110, 76, 191, 202, 164, 201, 114, 109, 16, 42, 21, 255, 206, 177, 198, 47, 31, 46, 24, 246, 212, 165, 205, 125, 92, 99, 0, 238, 204, 189, 211, 112, 22, 120, 41, 226, 214, 169, 206, 146, 119, 83, 116, 9, 180, 206, 177, 147, 98, 93, 120, 1, 224, 159, 167, 134, 106, 89, 36, 66, 177, 212, 181, 132, 103, 81, 39, 11, 170, 196, 233, 164, 131, 96 }, 143) +
                        UrlObfuscator.decode(new int[] { 198, 208, 172, 213, 106, 90, 40, 89, 242, 138, 230, 206, 126, 15, 62, 24, 254, 202, 189, 214, 102, 0, 97, 64, 243, 215, 179, 150, 108, 11, 46, 4, 230, 235, 146, 175, 210, 111, 85, 41, 83, 253, 156, 185, 156, 56, 26, 61, 24, 162, 204, 165, 197, 36, 24, 101, 64, 235, 207, 171, 129, 112, 28, 112, 70, 89, 244, 128, 225, 198, 118, 80, 54, 18, 229, 152, 229, 218, 45, 70, 96, 69, 190, 131, 250, 217, 48, 94, 120, 75, 228, 205, 232, 215, 43, 26, 61 }, 160) +
                        UrlObfuscator.decode(new int[] { 204, 181, 131, 125, 72, 55 }, 177) +
                        UrlObfuscator.decode(new int[] { 180, 128, 114, 63, 92, 47, 65, 235, 219, 171, 139, 114, 112, 57, 27, 242, 198, 249, 147, 124, 0, 47, 3, 249, 206, 172, 154, 83, 73, 53, 40, 230, 196, 181, 178, 158, 122, 84, 41, 8, 179, 197, 164, 199, 45 }, 194) +
                        UrlObfuscator.decode(new int[] { 186, 148, 57, 99, 59, 28, 228, 194, 172, 194, 106, 91, 105, 4, 234, 214, 167, 135, 115, 116, 80, 46, 49, 249, 221, 174, 171, 121, 83, 63, 0, 231, 154, 252, 152, 126, 75, 43, 21, 195, 205, 226, 206, 45, 0, 111, 91, 185, 147, 235, 131, 114, 34, 92, 47, 83, 170, 138, 233, 210, 90, 87, 33, 28, 189, 223, 184, 158, 39, 92, 99, 27, 226, 206, 189, 128, 43, 84, 107, 12, 230, 203, 166, 136, 139, 55, 6 }, 211) +
                        UrlObfuscator.decode(new int[] { 148, 118, 81, 41, 72, 13, 176, 209, 185, 157, 110, 21, 42, 89, 226, 218, 164, 223, 96, 31, 39, 6, 234, 217, 164, 199, 120, 7, 32, 2, 239, 194, 172, 151, 46, 67, 50, 118, 69 }, 228) +
                        "}}" +
                        UrlObfuscator.decode(new int[] { 156, 114, 27, 61, 4, 228, 129, 162, 136, 98, 76, 62, 1, 180, 147, 239, 151, 97, 87, 55, 19, 238, 164 }, 245) +
                        UrlObfuscator.decode(new int[] { 105, 80, 48, 77, 241, 206, 178, 171, 214, 123, 73, 53, 25, 237, 209, 184, 152, 61, 85, 127, 16, 184, 203, 189, 139, 121, 89, 57, 4, 169, 201, 156, 215, 88, 9, 33, 57, 176, 253, 132, 163, 212, 39 }, 262) +
                        UrlObfuscator.decode(new int[] { 126, 80, 125, 27, 230, 198, 255, 156, 106, 64, 42, 24, 227, 148, 241, 216, 46, 73, 48, 16, 173, 206, 164, 142, 152, 106, 85, 97, 67, 170, 130 }, 279) +
                        UrlObfuscator.decode(new int[] { 94, 38, 20, 165, 207, 254, 142, 110, 67, 94, 42, 20, 243, 213, 244, 150, 106, 94, 49, 28, 250, 152, 190, 158, 115, 78, 58, 4, 227, 197, 228, 153, 105, 83, 46, 11, 229, 206, 167, 218 }, 296) +
                        UrlObfuscator.decode(new int[] { 73, 53, 21, 247, 223, 241, 147, 57, 90, 48, 26, 244, 198, 185, 206, 55, 8, 107, 7, 165, 201, 161, 137, 117, 103, 49, 76, 232, 140, 173, 133, 145, 121, 73, 52, 86, 171, 144, 229, 202, 43, 18, 123, 84, 187, 218, 237, 132, 32, 94, 57, 9, 249, 221, 186, 142, 104, 66, 108, 83, 174, 202, 238, 179, 155, 115, 91, 47, 18, 180, 137, 254, 205 }, 62) +
                        UrlObfuscator.decode(new int[] { 14, 0, 233, 222, 164, 131, 109, 106, 53, 15, 225, 195, 166, 204, 114, 65, 73, 59, 46, 247, 222, 182, 156, 108, 88, 56, 93, 255, 159, 152, 162, 95, 97, 96, 30, 248, 217, 163, 135, 111, 78, 32, 28, 172, 204, 183, 149, 41, 54, 5 }, 79) +
                        UrlObfuscator.decode(new int[] { 29, 28, 255, 201, 191, 147, 50, 92, 113, 12, 235, 200, 239 }, 96) +
                        UrlObfuscator.decode(new int[] { 2, 245, 219, 154, 132, 97, 78, 37, 28, 252, 143, 165, 132, 116, 15, 123, 81, 176, 182, 133, 174, 153, 111, 110, 48, 21, 242, 217, 160, 128, 59, 81, 48, 0, 163, 156, 245, 220, 59, 3, 114 }, 113) +
                        UrlObfuscator.decode(new int[] { 255, 220, 163, 190, 138, 126, 84, 115, 31, 176, 195, 170 }, 130), null);
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
                    UrlObfuscator.decode(new int[] { 187, 212, 164, 158, 108, 90, 36, 3, 229, 130, 224, 147 }, 147) +
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
                    UrlObfuscator.decode(new int[] { 208, 177, 155, 122 }, 164) +
                    "var vp=document.querySelector('meta[name=\"viewport\"]');" +
                    UrlObfuscator.decode(new int[] { 220, 178, 219, 51, 71, 32, 70, 245, 219, 188, 214, 110, 70, 43, 18, 235, 192, 170, 151, 44, 66, 50, 58, 31, 233, 217, 158, 150, 124, 85, 50, 24, 225, 156, 244, 159, 116, 68, 46, 73, 164, 151, 189, 154, 39, 91, 34, 18, 196, 208, 183, 144, 104, 66, 74, 42, 24, 180, 156, 180, 152, 117, 82, 113, 89, 179, 197, 187, 148, 103, 95, 33, 31, 248, 140, 227, 210, 108, 72, 37, 16, 233, 198, 172, 149, 46, 119, 91, 60, 24, 181, 219, 169, 136, 114, 88, 49, 55, 251, 219, 189, 148, 39, 88, 61, 69, 176, 215 }, 181) +
                    UrlObfuscator.decode(new int[] { 176, 149, 42, 80, 39, 21, 193, 235, 202, 175, 149, 121, 79, 45, 29, 191, 145, 182, 155, 125, 70, 52, 30, 251, 137, 225, 203, 124, 67, 45, 28, 239, 155, 161, 129, 117, 75, 34, 5, 82, 233, 212, 184, 143, 114, 21, 120, 30, 248, 220, 160, 154, 115, 93, 125, 28, 237, 204, 160, 142, 55, 24, 102, 87, 170, 133, 169, 130, 122, 72, 45, 42, 19, 176, 207, 184, 155, 117, 93, 106, 71, 187, 132, 255, 210, 124, 89, 33, 7, 224, 217, 166, 199, 122, 75, 38, 10, 224, 153, 242, 204, 49, 12, 31, 43, 14, 249, 201, 247, 138, 123, 86, 58, 20, 246, 223, 183, 204, 126, 64, 105, 68, 183 }, 198) +
                    UrlObfuscator.decode(new int[] { 190, 144, 61, 21, 55, 29, 242, 197, 162, 139, 99, 88, 101, 13, 236, 220, 130, 138, 96, 73, 38, 12, 245, 226, 198, 151, 153, 52, 28, 5, 38, 249, 217, 178, 135, 123, 90, 54, 39, 249, 202, 185, 157, 99, 89, 62, 47, 225, 223, 225, 204, 45, 88 }, 215) +
                    UrlObfuscator.decode(new int[] { 158, 102, 84, 101, 23, 247, 159, 165, 143, 156, 107, 80, 57, 21, 238, 151, 187, 133, 115, 84, 32, 22, 215, 221, 181, 130, 107, 67, 56, 67, 173, 218, 188, 158, 106, 64, 99, 74, 185, 210, 180, 241, 151, 121, 1, 124, 37, 198, 217, 185, 146, 103, 91, 58, 22, 199, 217, 170, 153, 125, 67, 57, 30, 207, 193, 191, 193, 62 }, 232) +
                    UrlObfuscator.decode(new int[] { 138, 108, 25, 34, 16, 236, 199, 145, 158, 126, 91, 43, 3, 248, 150, 237, 129, 124, 74, 42, 73, 230, 204, 166, 152, 123, 114, 95, 37, 81, 236, 211, 189, 140, 127, 12, 100, 68, 163, 196, 166, 209, 102, 67, 61, 3, 249, 222, 168, 134, 115, 29, 42, 18, 230, 208, 167, 140, 144, 105, 16, 36, 65, 242, 208, 188, 147, 115, 91, 117, 26, 255, 193, 191, 157, 122, 76, 34, 31, 177, 198, 190, 130, 116, 67, 40, 12, 245, 140, 184, 229, 157, 113, 85, 43, 91, 240, 213, 167, 153, 103, 64, 50, 28, 229, 139, 226, 153, 104, 78, 32, 3, 253, 133, 179, 131, 125, 80, 110, 17, 232, 218, 218, 243, 156, 120, 81, 47, 10, 236, 141, 231, 197, 36, 22, 115, 24, 253, 223, 161, 159, 120, 74, 36, 29, 179, 211, 163, 157, 112, 14, 49, 8, 250, 250, 147, 188, 152, 113, 79, 42, 12, 173, 135, 229, 196, 54, 19, 56, 29, 255, 193, 191, 152, 106, 68, 61, 83, 250, 129, 254 }, 249) +
                    UrlObfuscator.decode(new int[] { 110, 70, 43, 18, 235, 192, 170, 151, 44, 73, 37, 62, 26, 179, 221, 171, 138, 124, 86, 51, 53, 253, 221, 191, 150, 57, 67, 59, 71, 182, 209 }, 266) +
                    UrlObfuscator.decode(new int[] { 102, 89, 56, 12, 244, 222, 253, 145, 58, 73, 44 }, 283) +
                    "try{var m=document.querySelector('meta[name=\"theme-color\"]');" +
                    UrlObfuscator.decode(new int[] { 88, 54, 71, 227, 139, 234, 156, 99, 71, 44, 8, 241, 139, 133, 141, 102, 83, 47, 54, 26, 223, 206, 178, 158, 126, 93, 113, 80, 212, 218, 183, 128, 126, 89, 43, 44, 255, 197, 175, 141, 108, 6, 38, 22, 245, 200, 186, 182, 105, 69, 82, 59, 62, 243, 215, 181, 139, 49, 76, 23, 27, 240, 193, 189, 152, 116, 109, 60, 4, 232, 204, 175, 199, 105, 87, 54, 9, 253, 247, 170, 132, 109, 122, 125, 50, 16, 244, 200, 241, 149, 57, 81, 48, 0, 210, 198, 165, 130, 102, 76, 56, 24, 238, 130, 238, 139, 104, 72, 49, 1, 237, 214, 230, 201, 131, 98, 26, 123, 82, 161, 196 }, 49) +
                    UrlObfuscator.decode(new int[] { 63, 2, 225, 235, 221, 181, 212, 126, 19, 34, 5 }, 66) +
                    UrlObfuscator.decode(new int[] { 39, 0, 232, 203, 166, 136, 37, 91, 34, 4, 237, 199, 176, 200, 68, 74, 39, 16, 238, 201, 219, 156, 143, 117, 95, 61, 28, 190, 145, 151, 155, 112, 65, 61, 24, 244, 237, 188, 132, 104, 76, 47, 71, 233, 215, 182, 137, 125, 115, 35, 6, 229, 221, 223, 190, 151, 124, 72, 54, 13, 249, 210, 252, 143 }, 83) +
                    UrlObfuscator.decode(new int[] { 18, 226, 208, 225, 144, 150, 125, 86, 30, 28, 167, 223, 173, 153, 117, 65, 61, 28, 252, 153, 249, 148 }, 100) +
                    UrlObfuscator.decode(new int[] { 3, 245, 193, 242, 146, 120, 78, 39, 3, 177, 240, 151, 210, 126, 70, 52, 69, 225, 158, 166, 142, 99, 106, 83, 56, 18, 239, 148, 188, 148, 114, 91, 48, 26, 231, 244, 163, 159, 98, 126, 34, 5, 229, 222, 225, 165, 102, 82, 45, 74, 229, 206, 174, 143, 141, 54, 74, 53, 21, 254, 214, 175, 217, 127, 91, 58, 22, 224, 230, 185, 139, 122, 69, 99, 89, 163, 133, 191, 142, 104, 65, 43, 20, 172, 200, 174, 177, 155, 111, 116, 62, 19, 254, 208, 163, 219, 39, 29, 104 }, 117) +
                    UrlObfuscator.decode(new int[] { 241, 205, 173, 143, 103, 9, 37, 118, 5, 254, 212, 186, 147, 119, 22, 39, 3, 230, 220, 251, 151, 56, 11, 42, 83, 232, 130, 187, 139, 123, 77, 41, 18, 192, 200, 166, 143, 100, 78, 75, 101, 0 }, 134) +
                    UrlObfuscator.decode(new int[] { 254, 208, 253, 144, 124, 81, 36, 29, 234, 192, 185, 194, 105, 69, 45, 17, 161, 128, 166, 140, 98, 75, 47, 78, 22, 240, 217, 185, 131, 85, 95, 112, 19, 249, 214, 161, 158, 119, 95, 36, 65, 236, 194, 168, 146, 35, 21, 120, 78, 229, 205, 165, 138, 108, 15, 48, 42, 13, 245, 148, 191, 149, 122, 77, 58, 19, 251, 192, 253, 144, 126, 84, 54, 71, 182 }, 151) +
                    UrlObfuscator.decode(new int[] { 193, 161, 206, 102, 76, 34, 11, 239, 142, 214, 176, 153, 121, 67, 21, 31, 176, 211, 185, 150, 97, 94, 55, 31, 228, 129, 170, 130, 111, 94, 39, 12, 230, 211, 131, 137, 97, 78, 39, 15, 244, 182, 130, 237, 213, 120, 82, 56, 17, 249, 152, 165, 129, 96, 90, 121, 20, 224, 205, 184, 129, 110, 68, 61, 70, 227, 201, 166, 145, 110, 71, 47, 20, 58, 242, 216, 177, 158, 116, 77, 113, 76 }, 168) +
                    UrlObfuscator.decode(new int[] { 207, 185, 133, 54, 69, 53, 1, 225, 212, 237, 137, 123, 67, 47, 31, 227, 198, 166, 207, 117, 12, 63, 10, 228, 137, 225, 172, 215, 111, 89, 47, 15, 235, 214, 247, 152, 96, 88, 63, 73, 231, 209, 189, 206, 108, 17, 56, 68, 224, 198, 163, 131, 125, 107, 37, 74, 166, 136, 152, 247, 198, 106, 90, 40, 89, 250, 138, 165, 219, 125, 93, 54, 20, 232, 224, 168, 197, 43, 2, 109, 64, 179, 206, 160, 205, 101, 31, 114, 29, 252, 253, 130, 237, 213, 105, 95, 45, 13, 229, 216, 245, 154, 102, 94, 61, 75 }, 185) +
                    UrlObfuscator.decode(new int[] { 188, 136, 122, 7, 48, 88, 247, 141, 177, 148, 98, 108, 74, 47, 21, 245, 221, 241, 153, 60, 7, 121, 22, 186, 156, 162, 128, 99, 71, 57, 68, 172, 134, 238, 193, 60, 79, 35, 76, 245, 140, 173, 133, 145, 121, 73, 52, 71, 169, 144, 170, 146, 98, 64, 38, 29, 178, 223, 165, 131, 98, 22 }, 202) +
                    UrlObfuscator.decode(new int[] { 173, 155, 107, 24, 54, 26, 168, 194, 253, 158, 116, 94, 40, 26, 229, 146, 248, 213, 121, 73, 53, 21, 224, 226, 175, 141, 96, 84, 23, 40, 38, 175, 230, 243, 195, 41, 12, 32, 20, 230, 147, 160, 204, 96, 78, 60, 30, 233, 226, 164, 157, 32, 81, 29, 85, 217, 143, 243, 209, 41, 51, 89, 96, 12, 250, 200, 170, 157, 94, 88, 33, 92, 229, 233, 224, 173, 35, 31, 125, 69, 167, 200, 165, 213, 119, 71, 55, 23, 230, 235, 175, 148, 215, 104, 102, 110, 38, 182, 136, 232, 222, 45 }, 219) +
                    UrlObfuscator.decode(new int[] { 133, 109, 2, 32, 27, 201, 199, 139, 204, 113, 11, 61, 28, 22, 237, 243, 189, 181, 50, 94, 113, 11, 234, 220, 167, 189, 115, 127, 120, 13, 226, 132, 229, 153, 111, 93, 61, 21, 232, 133, 170, 150, 110, 77, 123, 45, 27, 233, 201, 169, 148, 98, 74, 109, 4, 185, 211, 233, 149, 61, 82, 117, 12, 225, 128, 170, 208, 104, 68, 58, 93, 248, 159 }, 236) +
                    UrlObfuscator.decode(new int[] { 139, 125, 73, 122, 17, 253, 207, 235, 147, 97, 93, 49, 5, 249, 192, 160, 197, 111, 2, 49, 31, 233, 213, 230, 141, 57, 69, 55, 15, 227, 235, 215, 178, 146, 51, 84, 112, 3, 249, 139, 152, 149, 103, 90, 127, 29, 238, 214, 229, 220, 39, 103, 40, 28, 239, 136, 168, 141, 109, 10, 115, 85, 74, 178, 211, 245, 210, 33, 75, 61, 3, 227, 199, 186, 211, 58, 95, 108, 94, 184, 146, 235, 219, 45, 19, 111, 64, 175, 142, 170, 205, 118, 78, 19, 43, 12, 244, 210, 188, 210, 40, 14, 126, 77, 232, 143, 161, 151, 101, 69, 61, 0, 173, 139, 232, 205, 34, 64, 111, 5, 171, 214, 234, 201, 105, 8, 92, 112, 26, 181, 144, 178, 209, 123, 25, 52, 92, 175, 206, 233 }, 253) +
                    UrlObfuscator.decode(new int[] { 104, 66, 62, 67, 252, 200, 186, 199, 109, 24, 116, 88, 233, 157, 163, 183, 159, 116, 82, 117, 22, 252, 214, 176, 130, 125, 15, 56, 89, 186, 153, 180, 152, 108, 94, 107, 9, 180, 216, 166, 148, 118, 65, 107, 5, 228, 212, 252, 177, 144, 108, 78, 46, 28, 252, 228, 162, 140, 120, 86, 122, 18, 248, 206, 167, 131, 87, 64, 23, 64, 166, 197, 167, 134, 111, 68, 48, 14, 245, 241, 218, 158, 147, 119, 85, 43, 81, 172, 223, 179, 220, 112, 20, 119, 19, 161, 207, 243, 209, 59, 4, 112, 81, 174, 212, 160, 144, 118, 80, 47, 64, 23, 251, 197, 244, 152, 51, 2, 37 }, 270) +
                    UrlObfuscator.decode(new int[] { 105, 95, 47, 92, 239, 135, 169, 153, 101, 69, 48, 92, 244, 215, 165, 179, 96, 67, 61, 25, 255, 207, 173, 187, 115, 95, 41, 1, 171, 198, 174, 131, 138, 115, 88, 50, 15, 180, 219, 183, 147, 111, 73, 40, 23, 253, 210, 165, 130, 107, 67, 56, 69, 238, 198, 171, 146, 107, 64, 42, 23, 199, 205, 165, 178, 155, 115, 72, 114, 84, 250, 215, 187, 153, 103, 29, 104 }, 287) +
                    UrlObfuscator.decode(new int[] { 92, 50, 91, 230, 151, 246, 199, 62, 3, 126, 82, 179, 131, 188, 201, 116, 14, 116, 77, 183, 153, 247, 245, 138, 51, 91, 112, 74, 183, 137, 230, 194, 63, 64, 125, 16, 184, 159, 253, 219, 56, 18, 123, 68, 191, 129, 181, 131, 113, 81, 49, 12, 161, 135, 156, 238, 200, 44, 14, 106, 76, 191, 140 }, 53) +
                    UrlObfuscator.decode(new int[] { 52, 0, 240, 214, 176, 143, 32, 56, 29, 59, 26, 253, 220, 191, 158, 48, 13, 40, 79 }, 70) +
                    UrlObfuscator.decode(new int[] { 33, 23, 231, 148, 160, 151, 127, 84, 13, 9, 176, 202, 190, 132, 106, 92, 46, 9, 235, 140, 234, 153, 117, 82, 70, 37, 11, 253, 201, 250, 145, 37, 71, 63, 22, 255, 241, 181, 217, 57, 20, 39, 11, 164, 195, 235, 212, 53, 80, 47, 11, 224, 204, 181, 207, 95, 64, 95, 51, 24, 233, 213, 176, 156, 85, 81, 25, 21, 224, 198, 248, 139, 120, 71, 35, 8, 228, 221, 231, 183, 88, 71, 43, 0, 241, 205, 168, 132, 189, 121, 113, 61, 8, 238, 132, 176, 204, 87, 91, 48, 1, 253, 216, 180, 173, 124, 68, 40, 12, 239, 135, 169, 151, 118, 73, 61, 51, 227, 198, 165, 157, 159, 126, 87, 60, 8, 246, 205, 185, 146, 61, 92, 122, 73, 236, 205, 172, 143, 121, 79, 35, 66, 236, 129, 188, 155, 120, 31 }, 87) +
                    UrlObfuscator.decode(new int[] { 27, 226, 200, 161, 166, 100, 10, 104, 91 }, 104) +
                    UrlObfuscator.decode(new int[] { 16, 254, 159, 247, 130, 125, 93, 54, 30, 231, 129, 145, 178, 109, 69, 46, 27, 231, 206, 162, 167, 99, 97, 45, 20, 238, 251, 151, 166, 139, 114, 84, 61, 23, 224, 152, 138, 171, 114, 92, 53, 2, 224, 199, 169, 174, 108, 104, 38, 29, 233, 194, 248, 144, 113, 87, 36, 91, 27, 241, 222, 169, 150, 127, 87, 44, 89, 247, 209, 176, 182, 100, 84, 62, 27, 194, 196, 191, 159, 111, 71, 45, 21, 174, 130, 162, 140, 97, 84, 51, 54, 16, 186, 144, 168, 159, 119, 92, 21, 17, 185, 192, 161, 135, 116, 25, 116 }, 121) +
                    UrlObfuscator.decode(new int[] { 252, 200, 186, 199, 106, 68, 55, 23, 214, 210, 253, 239, 197, 121, 83, 56, 15, 244, 221, 185, 130, 59, 85, 55, 22, 212, 198, 170, 128, 121, 96, 34, 25, 253, 205, 169, 131, 119, 12, 100, 22, 238, 213, 220, 182, 142, 104, 90, 40, 13, 191, 155, 176, 128, 122, 80, 38, 24, 255, 193, 230, 196, 119, 93, 43, 27, 168, 201, 251, 161, 101, 87, 39, 79, 238, 240, 201, 245, 213, 32, 83, 63, 80, 249, 155, 185, 149, 96, 70, 5, 3, 179, 150, 253, 220, 34, 88, 44, 28, 242, 212, 171, 223, 111, 67, 50, 20, 43, 237, 128, 178, 192, 105, 92, 54, 19, 212, 210, 252, 218, 41, 76, 124, 20, 237, 204, 188, 159, 127, 91, 45, 93, 242, 215, 177, 134, 46, 81, 33, 44, 13, 244, 202, 190, 192, 109, 74, 34, 19, 232, 157, 232, 133, 120, 94, 43, 1, 250, 130, 170, 142, 109, 109, 49, 3, 235, 208, 143, 139, 114, 84, 90, 48, 24, 238, 147, 253, 139, 125, 68, 63, 15, 241, 148, 254, 130, 117, 65, 42, 47, 235, 130, 241 }, 138) +
                    UrlObfuscator.decode(new int[] { 239, 200, 160, 131, 97, 87, 39, 84, 241, 213, 133, 205, 97, 91, 33, 0, 176, 220, 168, 154, 39, 68, 34, 55, 224, 202, 164, 132, 194, 120, 72, 50, 24, 238, 208, 183, 153, 62, 28, 47, 26, 244, 153, 178, 136, 90, 4, 62, 14, 254, 220, 186, 137, 61, 71, 35, 55, 191, 210, 165, 171, 170, 116, 81, 62, 21, 236, 204, 255, 144, 96, 90, 48, 6, 248, 223, 161, 198, 36, 87, 41, 13, 221, 149, 169, 147, 105, 72, 120, 17, 228, 206, 219, 156, 154, 52, 18, 97, 4, 180, 134, 227, 197, 61, 8, 47, 74 }, 155) +
                    UrlObfuscator.decode(new int[] { 194, 174, 157, 41, 101, 50, 18, 228, 208, 170, 141, 111, 111, 93, 45, 24, 238, 205, 191, 139, 48, 85, 49, 38, 247, 219, 183, 149, 57, 1, 33, 15, 255, 206, 184, 159, 109, 15, 34, 10, 231, 214, 175, 132, 110, 107, 16, 57, 19, 248, 207, 180, 157, 121, 66, 16, 24, 246, 223, 180, 158, 123, 2, 54, 13, 255, 222, 187, 129, 101, 83, 49, 1, 240, 152, 181, 146, 138, 123, 17, 61, 15, 238, 203, 177, 149, 99, 65, 49, 53, 251, 221, 164, 138, 124, 23, 23, 76, 233, 197, 169, 148, 117, 2, 104, 68, 241, 213, 185, 179, 155, 58, 16, 124, 30, 248, 204, 182, 219, 97, 92, 54, 31, 244, 151, 146, 147, 36, 23 }, 172) +
                    UrlObfuscator.decode(new int[] { 212, 186, 211, 126, 86, 59, 2, 251, 208, 186, 135, 60, 83, 63, 11, 247, 132, 162, 142, 125, 9, 5, 18, 242, 196, 176, 138, 109, 79, 15, 61, 13, 248, 206, 173, 159, 107, 16, 53, 17, 198, 215, 187, 151, 117, 25, 97, 1, 239, 223, 174, 152, 127, 77, 111, 2, 234, 199, 182, 143, 100, 78, 75, 112, 31, 243, 223, 163, 213, 99, 86, 34, 1, 230, 218, 176, 132, 100, 74, 61, 87, 248, 217, 191, 140, 36, 70, 50, 17, 246, 202, 160, 148, 116, 122, 120, 52, 16, 239, 223, 171, 194, 76, 17, 54, 24, 242, 193, 162, 215, 35, 9, 62, 24, 242, 198, 172, 207, 90, 91, 108, 95, 254, 193, 160, 148, 156, 118, 21, 57, 82, 225, 196, 165 }, 189) +
                    UrlObfuscator.decode(new int[] { 179, 144, 111, 74, 62, 10, 224, 143, 163, 204, 127, 94 }, 206) +
                    UrlObfuscator.decode(new int[] { 171, 140, 100, 71, 50, 28, 177, 153, 160, 159, 123, 80, 60, 5, 191, 239, 144, 143, 99, 72, 57, 5, 224, 204, 143, 135, 117, 80, 42, 1, 242, 226, 208, 171, 147, 120, 18, 33, 14, 241, 217, 178, 154, 99, 29, 13, 46, 241, 193, 170, 159, 99, 66, 46, 33, 233, 215, 178, 140, 103, 80, 0, 14, 245, 241, 218, 224, 136, 105, 79, 60, 67 }, 223) +
                    UrlObfuscator.decode(new int[] { 148, 96, 77, 56, 1, 238, 196, 189, 198, 102, 66, 33, 33, 245, 199, 175, 148, 179, 119, 78, 40, 30, 244, 220, 170, 223, 49, 86, 56, 26, 241, 218, 247, 195, 104, 88, 34, 8, 254, 192, 167, 137, 46, 64, 109, 24 }, 240) +
                    "var t=e.target;var el=t&&t.closest?t.closest('button,a,[role=\"button\"],input[type=\"button\"],input[type=\"submit\"]'):null;" +
                    UrlObfuscator.decode(new int[] { 104, 70, 23, 59, 17, 186, 157, 173, 144, 118, 83, 57, 2, 186, 242, 188, 149, 98, 64, 39, 9, 206, 217, 163, 141, 111, 66, 96, 67, 197, 205, 166, 147, 111, 118, 90, 31, 14, 242, 222, 190, 157, 57, 64, 60, 22, 225, 211, 165, 149, 38, 85, 12, 2, 239, 216, 166, 129, 99, 100, 55, 13, 231, 197, 164, 206, 137, 119, 95, 46, 26, 238, 220, 240, 222, 45, 72 }, 257) +
                    UrlObfuscator.decode(new int[] { 111, 29, 36, 29, 251, 200, 229, 208, 119, 84, 43, 6, 242, 198, 172, 203, 103, 8, 59, 34 }, 274) +
                    // Polyfills the Web Share API on top of the real Android
                    // share sheet -- most WebView builds don't implement
                    // navigator.share at all, so a page's own "Share" button
                    // either does nothing or falls back to a hand-rolled
                    // copy-link menu instead of the native chooser. Only
                    // installed when the page doesn't already have a working
                    // navigator.share (some newer WebView versions do).
                    UrlObfuscator.decode(new int[] { 87, 48, 24, 251, 246, 216, 245, 139, 114, 84, 61, 23, 224, 152, 148, 154, 119, 64, 62, 25, 235, 236, 191, 133, 111, 77, 44, 78, 161, 231, 171, 128, 113, 77, 40, 4, 61, 236, 212, 184, 156, 127, 23, 43, 31, 247, 199, 177, 213, 52, 16, 62, 14, 248, 196, 171, 138, 126, 70, 58, 73, 245, 205, 165, 145, 103, 8, 59 }, 291) +
                    UrlObfuscator.decode(new int[] { 87, 57, 1, 255, 210, 181, 135, 125, 67, 126, 28, 230, 204, 190, 142, 55, 79, 61, 9, 229, 209, 173, 140, 108, 9, 36, 62, 10, 252, 149, 160 }, 57) +
                    UrlObfuscator.decode(new int[] { 46, 8, 252, 198, 251, 129, 101, 87, 35, 29, 252, 228, 195, 230 }, 74) +
                    UrlObfuscator.decode(new int[] { 47, 8, 224, 195, 150, 152, 113, 70, 60, 27, 245, 242, 189, 135, 105, 75, 46, 68, 250, 192, 166, 148, 96, 12, 16, 22, 243, 201, 209, 185, 213, 120, 90, 46, 24, 182, 195, 191, 129, 120, 86, 46, 13, 183, 136, 231, 193, 95, 95, 56, 0, 230, 192, 238, 129, 101, 87, 35, 79, 244, 250, 198, 169, 128, 103, 29, 126, 81, 187, 229, 161, 134, 122, 92, 54, 88, 235, 207, 185, 141, 37, 95, 59, 4, 251, 218, 226, 195, 42, 11, 122 }, 91) +
                    UrlObfuscator.decode(new int[] { 30, 238, 222, 188, 154, 105, 6, 21, 22, 236, 207, 168, 147, 154, 48, 79, 57, 8, 245, 213, 174, 146, 62, 28, 111, 14, 241, 208, 164, 140, 102, 5, 41, 66, 241, 219, 173, 147, 115, 87, 42, 67, 210, 211, 175, 178, 151, 110, 89, 117, 8, 252, 210, 178, 149, 97, 28, 54, 91, 170, 205, 178, 213 }, 108) +
                    UrlObfuscator.decode(new int[] { 19, 253, 205, 179, 158, 121, 67, 57, 7, 186, 208, 179, 159, 67, 71, 47, 31, 233, 150, 172, 156, 102, 68, 50, 12, 235, 205, 234, 200, 123, 109, 91, 41, 9, 233, 212, 249, 140, 101, 67, 48, 79, 238, 137 }, 125) +
                    UrlObfuscator.decode(new int[] { 243, 208, 175, 138, 126, 74, 32, 79, 227, 140, 191, 158 }, 142) +
                    // Reports the scrollTop of whichever element just
                    // scrolled -- document or any nested panel -- so native
                    // knows whether a pull-to-refresh gesture is actually
                    // safe (see SwipeRefreshLayout override above). Scroll
                    // events don't bubble, so this has to be a capture
                    // listener on window to see scrolling from any
                    // descendant, not just the document itself. Throttled
                    // with a trailing rAF flag so a fast scroll doesn't
                    // spam the JS bridge with a call per pixel.
                    UrlObfuscator.decode(new int[] { 235, 204, 164, 135, 114, 92, 113, 89, 224, 223, 187, 144, 124, 69, 127, 47, 208, 207, 163, 136, 121, 69, 32, 12, 212, 197, 183, 139, 111, 78, 3, 15, 10, 240, 217, 245, 128, 109, 80, 54, 19, 249, 194, 250, 172, 77, 80, 62, 11, 252, 194, 165, 143, 89, 74, 58, 8, 234, 201, 134, 140, 119, 79, 36, 98, 10, 239, 201, 190, 193 }, 159) +
                    UrlObfuscator.decode(new int[] { 198, 174, 156, 45, 92, 46, 4, 237, 193, 169, 129, 56, 66, 34, 14, 242, 197, 147, 178, 156, 111, 79, 10, 22, 235, 138, 176, 148, 120, 64, 55, 74 }, 176) +
                    UrlObfuscator.decode(new int[] { 182, 137, 145, 122, 82, 43, 85, 251, 221, 188, 178, 96, 80, 58, 7, 222, 216, 163, 155, 107, 67, 41, 25, 162, 142, 187, 132, 116, 74, 40, 15, 165, 141, 166, 170, 144, 126, 72, 50, 21, 247, 144, 178, 223, 110 }, 193) +
                    UrlObfuscator.decode(new int[] { 187, 151, 56, 95, 43, 3, 232, 194, 164, 142, 33, 85, 35, 17, 241, 209, 172, 218, 112, 122, 80, 57, 21, 245, 221, 228, 140, 101, 67, 48, 79 }, 210) +
                    UrlObfuscator.decode(new int[] { 145, 103, 80, 53, 58, 13, 233, 253, 181, 147, 116, 89, 35, 31, 250, 218, 149, 128, 112, 93, 42, 70, 235, 217, 165, 137, 125, 65, 40, 8, 173, 141, 184, 146, 100, 78, 91, 55, 19, 251, 134, 188, 152, 116, 68, 51, 78 }, 227) +
                    UrlObfuscator.decode(new int[] { 130, 114, 64, 113, 21, 227, 147, 229, 137, 37, 94, 40, 26, 224, 195, 177, 194, 37, 71, 111, 20, 30, 236, 218, 185, 143, 52, 87, 55, 19, 243, 225, 173, 131, 119, 12, 109, 82, 191, 132, 243, 142, 36, 93, 41, 21, 225, 192, 176, 217, 102, 78, 35, 42, 19, 248, 210, 175, 212, 106, 91, 37, 25, 249, 216, 186, 156, 118, 117, 35, 11, 224, 201, 165, 158, 50 }, 244) +
                    UrlObfuscator.decode(new int[] { 115, 69, 49, 66, 245, 207, 207, 227, 152, 112, 4, 63, 21, 182, 196, 181, 135, 123, 95, 62, 37, 255, 223, 244, 221, 55 }, 261) +
                    UrlObfuscator.decode(new int[] { 96, 84, 38, 83, 226, 222, 163, 210, 122, 66, 60, 85, 186, 146, 161, 129, 46, 85, 43, 16, 163, 156, 253, 179, 159, 110, 72, 11, 21, 234, 145, 172, 154, 116, 71, 39, 34, 254, 195, 242, 158, 98, 95, 112, 3, 239, 128, 176, 143, 107, 64, 44, 21, 175, 225, 209, 186, 143, 115, 82, 62, 59, 234, 222, 178, 146, 113, 21, 116, 48, 254, 203, 188, 130, 101, 79, 8, 27, 225, 195, 161, 128, 42, 81, 39, 17, 239, 237, 202, 142, 159, 105, 85, 53, 20, 195, 217, 165, 221, 104, 115, 63, 20, 253, 193, 164, 136, 73, 88, 32, 12, 224, 195, 235, 150, 102, 82, 46, 18, 11, 205, 222, 174, 148, 118, 85, 12, 24, 230, 157, 160, 156, 98, 77, 96, 70, 181, 208, 177 }, 278) +
                    UrlObfuscator.decode(new int[] { 90, 111, 94, 249, 143, 182, 147, 117, 122, 23, 102, 1, 230, 217, 184, 140, 116, 94, 125, 17, 186, 201, 172 }, 295) +
                    UrlObfuscator.decode(new int[] { 64, 117, 83, 179, 130 }, 61),
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
                if (rel.isEmpty()) rel = UrlObfuscator.decode(new int[] { 39, 3, 232, 206, 178, 199, 96, 83, 43, 9 }, 78);

                byte[] data = embedCache.get(rel);
                if (data == null) {
                    data = EmbeddedAssets.get(rel);
                    if (data != null) embedCache.put(rel, data);
                }
                if (data == null) return super.shouldInterceptRequest(view, request);

                return new WebResourceResponse(guessEmbeddedMime(rel), UrlObfuscator.decode(new int[] { 10, 42, 219, 145, 227 }, 95), new ByteArrayInputStream(data));
            }

            private String guessEmbeddedMime(String rel) {
                String lower = rel.toLowerCase();
                if (lower.endsWith(UrlObfuscator.decode(new int[] { 94, 231, 218, 160, 128 }, 112)) || lower.endsWith(UrlObfuscator.decode(new int[] { 175, 200, 203, 179 }, 129))) return UrlObfuscator.decode(new int[] { 230, 212, 168, 155, 33, 69, 56, 6, 230 }, 146);
                if (lower.endsWith(UrlObfuscator.decode(new int[] { 141, 161, 146, 115 }, 163))) return UrlObfuscator.decode(new int[] { 192, 182, 138, 101, 31, 44, 29, 254 }, 180);
                if (lower.endsWith(UrlObfuscator.decode(new int[] { 235, 142, 112 }, 197)) || lower.endsWith(UrlObfuscator.decode(new int[] { 248, 152, 126, 64 }, 214))) return UrlObfuscator.decode(new int[] { 134, 118, 85, 40, 10, 225, 192, 180, 182, 145, 115, 19, 49, 27, 239, 217, 164, 149, 103, 93, 35, 6 }, 231);
                if (lower.endsWith(UrlObfuscator.decode(new int[] { 214, 125, 69, 58, 26 }, 248))) return UrlObfuscator.decode(new int[] { 104, 88, 55, 10, 236, 199, 162, 150, 104, 79, 81, 113, 23, 239, 212, 180 }, 265);
                if (lower.endsWith(UrlObfuscator.decode(new int[] { 52, 74, 46, 16 }, 282))) return UrlObfuscator.decode(new int[] { 89, 34, 15, 234, 201, 228, 153, 127, 79, 108, 30, 232, 200 }, 48);
                if (lower.endsWith(UrlObfuscator.decode(new int[] { 111, 16, 17, 249 }, 65))) return UrlObfuscator.decode(new int[] { 59, 28, 241, 200, 171, 194, 124, 69, 45 }, 82);
                if (lower.endsWith(UrlObfuscator.decode(new int[] { 77, 232, 209, 167 }, 99)) || lower.endsWith(UrlObfuscator.decode(new int[] { 90, 249, 194, 180, 151 }, 116))) return UrlObfuscator.decode(new int[] { 236, 201, 162, 133, 100, 15, 85, 46, 24, 251 }, 133);
                if (lower.endsWith(UrlObfuscator.decode(new int[] { 184, 210, 189, 149 }, 150))) return UrlObfuscator.decode(new int[] { 206, 171, 132, 99, 70, 109, 6, 233, 249 }, 167);
                if (lower.endsWith(UrlObfuscator.decode(new int[] { 150, 160, 147, 119, 68 }, 184))) return UrlObfuscator.decode(new int[] { 160, 133, 102, 65, 32, 75, 244, 199, 163, 144 }, 201);
                if (lower.endsWith(UrlObfuscator.decode(new int[] { 244, 142, 119, 81, 48, 71 }, 218))) return UrlObfuscator.decode(new int[] { 141, 101, 71, 60, 72, 241, 202, 162, 133, 48 }, 235);
                if (lower.endsWith(UrlObfuscator.decode(new int[] { 210, 108, 85, 63, 30 }, 252))) return UrlObfuscator.decode(new int[] { 107, 67, 37, 30, 166, 223, 168, 128, 99 }, 269);
                if (lower.endsWith(UrlObfuscator.decode(new int[] { 48, 73, 40, 29 }, 286))) return UrlObfuscator.decode(new int[] { 82, 60, 28, 229, 159, 187, 154, 107 }, 52);
                if (lower.endsWith(UrlObfuscator.decode(new int[] { 107, 13, 224, 205 }, 69))) return UrlObfuscator.decode(new int[] { 63, 24, 245, 212, 183, 222, 104, 2, 39, 14, 227, 197 }, 86);
                return UrlObfuscator.decode(new int[] { 6, 246, 213, 168, 138, 97, 64, 52, 54, 17, 243, 147, 180, 153, 109, 93, 35, 91, 230, 192, 161, 151, 112, 93 }, 103);
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
                    showOffline(UrlObfuscator.decode(new int[] { 48, 195, 226, 133, 212 }, 120) + status, request.getUrl().toString());
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
                int errBg = Color.parseColor(isOfflineError(description) ? UrlObfuscator.decode(new int[] { 170, 238, 129, 160, 67, 98, 5 }, 137) : UrlObfuscator.decode(new int[] { 185, 137, 153, 199, 87, 5, 22 }, 154));
                errorView.setBackgroundColor(errBg);
                errorWeb.setBackgroundColor(errBg);
                errorWeb.loadDataWithBaseURL(UrlObfuscator.decode(new int[] { 195, 190, 157, 120, 84, 124, 74, 171, 198, 176, 147, 111, 109, 16, 49, 19, 248, 219, 181, 215 }, 171),
                    renderErrorPage(description, failingUrl), UrlObfuscator.decode(new int[] { 200, 190, 130, 109, 23, 63, 2, 248, 216 }, 188), UrlObfuscator.decode(new int[] { 152, 184, 77, 7, 113 }, 205), null);
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
                popupSettings.setUserAgentString(popupDefaultUA.replace(UrlObfuscator.decode(new int[] { 229, 221, 107, 77 }, 222), "").replaceAll("Version/[0-9.]+\s", ""));
                popupWebView.setVerticalScrollBarEnabled(false);
                popupWebView.setHorizontalScrollBarEnabled(false);

                final Dialog popupDialog = new Dialog(MainActivity.this, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
                // Loading overlay: same bouncing dots + colors as the main
                // navigation overlay, shown over the popup until its page has
                // painted, so there is never an empty black/white screen while
                // a link (sign-in, Telegram, any website) is still loading.
                final FrameLayout popupRoot = new FrameLayout(MainActivity.this);
                popupRoot.setBackgroundColor(Color.parseColor(UrlObfuscator.decode(new int[] { 204, 62, 29, 124, 91, 186, 153 }, 239)));
                popupRoot.addView(popupWebView, new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
                final FrameLayout popupLoader = new FrameLayout(MainActivity.this);
                popupLoader.setBackgroundColor(Color.parseColor(UrlObfuscator.decode(new int[] { 35, 47, 14, 109, 76, 171, 138 }, 256)));
                final BouncingDotsView popupDots = new BouncingDotsView(MainActivity.this, Color.parseColor(UrlObfuscator.decode(new int[] { 50, 118, 125, 40, 191, 233, 142 }, 273)));
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
                                UrlObfuscator.decode(new int[] { 86, 36, 24, 11, 177, 213, 168, 150, 118 }, 290), UrlObfuscator.decode(new int[] { 109, 3, 48, 184, 140 }, 56), failed);
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
                                renderErrorPage(UrlObfuscator.decode(new int[] { 1, 60, 211, 246, 229 }, 73) + status, failed),
                                UrlObfuscator.decode(new int[] { 46, 28, 224, 195, 249, 157, 96, 94, 62 }, 90), UrlObfuscator.decode(new int[] { 62, 222, 239, 229, 223 }, 107), failed);
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
                        if (scheme != null && !scheme.equals(UrlObfuscator.decode(new int[] { 20, 239, 206, 169 }, 124)) && !scheme.equals(UrlObfuscator.decode(new int[] { 229, 216, 191, 154, 122 }, 141))) {
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
        return EMBED_HOST + UrlObfuscator.decode(new int[] { 247, 211, 184, 158, 98, 23, 48, 3, 251, 217 }, 158);
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
        if (Intent.ACTION_SEND.equals(action) && UrlObfuscator.decode(new int[] { 219, 171, 149, 120, 4, 58, 5, 233, 206, 168 }, 175).equals(intent.getType())) {
            String shared = intent.getStringExtra(Intent.EXTRA_TEXT);
            if (shared != null && !shared.isEmpty()) {
                try {
                    return baseUrl() + UrlObfuscator.decode(new int[] { 255, 172, 150, 124, 78, 62, 30, 198, 204, 178, 142, 97, 9 }, 192) + java.net.URLEncoder.encode(shared, UrlObfuscator.decode(new int[] { 132, 164, 73, 3, 117 }, 209));
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

        String shortcutAction = intent.getStringExtra(UrlObfuscator.decode(new int[] { 145, 105, 79, 77, 42, 30, 233, 207, 133, 152, 123, 67, 63, 26, 250 }, 226));
        if (UrlObfuscator.decode(new int[] { 129, 119, 93, 63, 14, 234 }, 243).equals(shortcutAction)) {
            webView.getSettings().setCacheMode(WebSettings.LOAD_NO_CACHE);
            webView.reload();
            webView.getSettings().setCacheMode(WebSettings.LOAD_CACHE_ELSE_NETWORK);
            return;
        }
        if (UrlObfuscator.decode(new int[] { 108, 76, 47, 4 }, 260).equals(shortcutAction)) {
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
                blobTmp = File.createTempFile(UrlObfuscator.decode(new int[] { 119, 88, 60, 16 }, 277), UrlObfuscator.decode(new int[] { 8, 49, 9, 243 }, 294), getCacheDir());
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
                Toast.makeText(MainActivity.this, UrlObfuscator.decode(new int[] { 120, 52, 13, 247, 212, 184, 151, 113, 20, 53, 19, 248, 220, 170, 138, 55, 12, 40, 5, 252, 196, 163, 198, 107, 75, 55, 66, 243, 197, 222, 186, 221, 104, 83, 51, 10, 184, 209, 191, 153, 113 }, 60), Toast.LENGTH_LONG).show();
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
                    sendIntent.setType(UrlObfuscator.decode(new int[] { 57, 9, 243, 222, 230, 152, 107, 71, 44, 10 }, 77));
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
                String safeTitle = (title == null || title.trim().isEmpty()) ? UrlObfuscator.decode(new int[] { 29, 17, 249, 218, 180, 156, 106, 103, 36, 26 }, 94) : title;
                String safeBody = body == null ? "" : body;
                NotificationCompat.Builder notification = new NotificationCompat.Builder(MainActivity.this, UrlObfuscator.decode(new int[] { 11, 235, 203, 173, 158, 102, 93 }, 111))
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
            if (hex == null || !hex.matches(UrlObfuscator.decode(new int[] { 222, 188, 150, 134, 189, 54, 124, 56, 85, 241, 134, 248, 205, 78, 73, 103, 13, 243, 245, 140, 193, 77, 75, 100, 14, 183, 139, 252, 185, 120, 17, 60, 73, 91 }, 128))) return;
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
                android.content.SharedPreferences sp = getSharedPreferences(UrlObfuscator.decode(new int[] { 226, 219, 170, 130, 104, 88, 36, 4, 214, 222, 246 }, 145), MODE_PRIVATE);
                org.json.JSONArray idx = new org.json.JSONArray(sp.getString(UrlObfuscator.decode(new int[] { 253, 158, 137, 155, 102 }, 162), "[]"));
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
                ed.putString("p:" + key, json).putString(UrlObfuscator.decode(new int[] { 236, 141, 152, 116, 87 }, 179), next.toString()).apply();
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
            if (hex == null || !hex.matches(UrlObfuscator.decode(new int[] { 154, 192, 89, 96, 109, 25, 31, 176, 218, 235, 215, 32, 101, 44, 64, 232, 144 }, 196))) return;
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
                        Uri.parse(UrlObfuscator.decode(new int[] { 165, 149, 112, 89, 48, 23, 234, 148 }, 213) + getPackageName()));
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
            UrlObfuscator.decode(new int[] { 206, 99, 81, 45, 1, 245, 201, 208, 176, 213, 53, 64, 46, 11, 225, 204 }, 230) +
            UrlObfuscator.decode(new int[] { 129, 119, 71, 116, 3, 175, 213, 191, 140, 123, 64, 41, 5, 254, 135, 175, 130, 114, 96, 40, 6, 239, 196, 174, 171, 188, 100, 117, 63, 82, 190, 204, 182, 148, 69, 85, 61, 23, 253, 255, 191, 154, 100, 67, 37, 25, 174, 129, 252 }, 247) +
            UrlObfuscator.decode(new int[] { 97, 65, 110, 68, 244, 223, 190, 145, 46, 119, 87, 57, 24, 254, 212, 165, 132, 54, 70, 123, 23, 255, 211, 162, 131, 67, 71, 62, 24, 165, 201, 166, 134, 115, 71, 44, 10, 240, 138, 230, 137, 140, 51, 92, 63, 15, 243, 207, 189, 208, 63, 28, 38, 22, 230, 196, 162, 129, 53 }, 264) +
            UrlObfuscator.decode(new int[] { 111, 89, 37, 86, 244, 137, 183, 157, 114, 69, 34, 11, 227, 216, 229, 139, 106, 92, 46, 16, 224, 225, 175, 135, 108, 69, 81, 42, 70, 245, 221, 242, 216, 121, 75, 42, 20, 169, 142, 239, 149, 127, 76, 59, 0, 233, 197, 190, 199, 106, 72, 34, 28, 248, 223, 227, 145, 46, 124, 81, 51, 8, 250, 211, 183, 139, 63, 87, 124, 93, 225, 215, 165, 133, 125, 64, 118 }, 281) +
            UrlObfuscator.decode(new int[] { 89, 47, 31, 172, 223, 247, 193, 105, 9, 50, 4, 227, 237, 163, 140, 101, 99, 66, 122, 91, 178, 148, 173, 151, 91, 89, 34, 17, 225, 241, 176, 131, 106, 6, 100, 87 }, 47) +
            UrlObfuscator.decode(new int[] { 41, 57, 86, 233, 157, 230, 199, 62, 81, 57, 6, 224, 192, 244, 212, 55, 68, 110, 83, 176, 139, 191, 143, 113, 92, 38, 20, 224, 197, 228, 196, 39, 84, 30, 99, 64, 187, 200, 191, 149, 125, 84, 34, 82, 178, 149, 243, 144, 62, 70, 61, 46, 227, 197, 190, 140, 102, 83, 3, 1, 237, 215, 163, 131, 108, 122, 23, 47, 25, 239, 207, 171, 150, 44 }, 64) +
            UrlObfuscator.decode(new int[] { 39, 17, 253, 142, 191, 209, 106, 4, 46, 13, 243, 228, 170, 145, 109, 70, 40, 14, 24, 221, 209, 181, 158, 116, 77, 10, 18, 245, 193, 252, 218, 41 }, 81) +
            UrlObfuscator.decode(new int[] { 20, 224, 210, 159, 168, 139, 33, 76, 51, 23, 252, 216, 161, 219, 98, 90, 33, 4, 241, 195, 152, 132, 105, 92, 58, 6, 250, 211, 253, 147, 101, 81, 98, 23, 232, 162, 200, 171, 195, 109, 76, 119, 16, 242, 223, 178, 156, 103, 8, 38, 25, 225, 202, 162, 155, 37, 67, 39, 6, 226, 212, 141, 129, 106, 69, 41, 20, 68 }, 98) +
            UrlObfuscator.decode(new int[] { 5, 243, 195, 240, 159, 111, 73, 113, 90, 188, 146, 190, 134, 116, 5, 32, 6, 238, 213, 161, 226, 206, 38 }, 115) +
            UrlObfuscator.decode(new int[] { 237, 197, 234, 147, 46, 125, 81, 41, 8, 244, 215, 231, 142, 127, 27, 37, 21, 247, 155, 181, 149, 99, 90, 44, 81, 198, 203, 189, 128, 41, 75, 44, 10, 171, 208, 239, 130, 144, 106, 73, 51, 22, 183, 145, 174, 159, 59, 69, 53, 23, 187, 157, 157, 142, 122, 69, 98, 6, 235, 209, 224, 213, 50, 9, 50, 11, 168, 145, 238, 238, 198, 52, 21, 96 }, 132) +
            UrlObfuscator.decode(new int[] { 240, 216, 160, 151, 49, 89, 41, 70, 255, 130, 191, 133, 121, 20, 55, 7, 225, 141, 167, 135, 109, 84, 94, 99, 80, 209, 218, 174, 145, 54, 90, 63, 27, 188, 195, 179, 149, 61, 93, 96, 25, 227, 219, 230, 164, 105, 83, 46, 75, 233, 194, 186, 201, 49, 41, 18, 43, 20, 177, 138, 247, 201, 39, 31, 124, 79 }, 149) +
            UrlObfuscator.decode(new int[] { 207, 163, 204, 103, 71, 45, 20, 30, 183, 198, 170, 154, 104, 25, 43, 20, 171, 212, 250, 144, 126, 94, 35, 10, 253, 217, 228, 204, 36, 74, 39, 9, 242, 192, 170, 151, 46, 15, 52, 62, 28, 205, 221, 181, 159, 117, 20, 121, 25, 229, 192, 186, 157, 127, 67, 98, 13, 226, 194, 191, 143, 103, 92, 107, 61, 225, 197, 183, 131, 44, 83, 92, 44, 18, 240, 215, 247, 154, 119, 89, 34, 20, 253, 221, 183, 131, 77, 8, 103, 86 }, 166) +
            UrlObfuscator.decode(new int[] { 222, 176, 221, 103, 80, 116, 87, 227, 204, 224, 158, 111, 89, 37, 5, 228, 239, 163, 140, 99, 75, 54, 95, 243, 252, 144, 190, 144, 114, 95, 55, 12, 223, 211, 188, 147, 123, 70, 120, 11, 252, 205, 227, 159, 104, 88, 38, 4, 235, 228, 188, 204, 120, 86, 46, 16, 69, 250, 216, 176, 143, 123, 21, 58, 18, 254, 212, 162, 154, 125, 67, 106, 72, 239, 216, 184, 132, 45, 84, 97, 92, 251 }, 183) +
            UrlObfuscator.decode(new int[] { 173, 139, 117, 64, 63, 20, 235, 207, 164, 176, 137, 51, 79, 56, 8, 246, 212, 187, 180, 108, 28, 40, 6, 254, 192, 245, 138, 104, 64, 63, 11, 165, 202, 162, 142, 100, 82, 42, 13, 243, 154, 152, 191, 136, 104, 84, 125, 4, 177, 140, 171, 136 }, 200) +
            UrlObfuscator.decode(new int[] { 164, 155, 118, 66, 54, 28, 187, 215, 248, 139, 114, 83, 100, 68, 162, 145 }, 217), null);
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
            UrlObfuscator.decode(new int[] { 194, 111, 93, 41, 5, 241, 205, 172, 140, 41, 9, 68, 42, 15, 229, 192, 172, 152, 106, 23, 55, 72, 240, 220, 177, 132, 125, 74, 32, 25, 162, 202, 169, 157, 97, 81, 35, 32, 232, 198, 175, 132, 110, 107, 5, 52, 26, 179, 155, 184, 132, 107, 87, 104, 73, 174, 214, 190, 147, 122, 67, 40, 2, 255, 132, 171, 135, 99, 95, 108, 22, 230, 214, 180, 146, 145, 62, 16, 109, 64 }, 234) +
            UrlObfuscator.decode(new int[] { 141, 123, 75, 120, 3, 171, 157, 181, 221, 102, 80, 55, 33, 239, 192, 169, 151, 118, 14, 111, 78, 168, 209, 171, 175, 109, 86, 37, 45, 61, 252, 207, 190, 210, 48, 3 }, 251) +
            UrlObfuscator.decode(new int[] { 101, 77, 98, 29, 169, 154, 251, 194, 109, 77, 50, 20, 244, 184, 152, 251, 136, 58, 7, 100, 95, 227, 211, 173, 128, 114, 64, 52, 17, 168, 136, 235, 152, 42, 23, 116, 79, 244, 195, 169, 129, 96, 86, 102, 70, 89, 191, 220, 242, 146, 105, 122, 55, 25, 226, 208, 186, 135, 87, 85, 57, 27, 239, 207, 160, 142, 35, 91, 45, 19, 243, 215, 170, 195, 47, 16, 123 }, 268) +
            UrlObfuscator.decode(new int[] { 107, 93, 41, 90, 235, 133, 182, 216, 114, 81, 39, 48, 254, 197, 161, 138, 100, 66, 44, 41, 229, 193, 162, 136, 113, 118, 38, 1, 245, 136, 150, 229, 143, 121, 79, 47, 11, 246, 151, 155, 148, 96, 91, 124, 3, 255, 218, 160, 137, 36, 89, 100, 11, 231, 211, 178, 138, 105, 9, 115, 81, 176, 182, 145, 236, 204, 43, 1, 36, 27, 246, 194, 182, 156, 59, 87, 120, 11, 253, 203, 185, 153, 121, 68, 105, 69, 182, 157, 184, 153, 42, 10, 104, 91 }, 285),
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
        int id = getResources().getIdentifier(UrlObfuscator.decode(new int[] { 64, 38, 16, 228, 218, 189, 178, 110, 74, 56, 54, 224, 194, 175, 130, 108, 87 }, 51), UrlObfuscator.decode(new int[] { 32, 10, 239, 196, 174 }, 68), UrlObfuscator.decode(new int[] { 52, 26, 247, 192, 190, 153, 107 }, 85));
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
        if (t.isEmpty() || UrlObfuscator.decode(new int[] { 8, 240, 200, 175 }, 102).equals(t)) return null;
        try {
            if (t.startsWith("#")) return Color.parseColor(t) | 0xFF000000;
            if (!t.startsWith(UrlObfuscator.decode(new int[] { 5, 241, 215 }, 119))) return null;
            String inner = t.substring(t.indexOf('(') + 1);
            String[] parts = inner.split(UrlObfuscator.decode(new int[] { 211, 249, 246, 200, 61, 13, 31, 74 }, 136));
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
        int scrim = Color.parseColor(UrlObfuscator.decode(new int[] { 186, 137, 227, 196, 33, 1, 16 }, 153));
        try {
            if (topScrim != null && topScrim.getBackground() instanceof ColorDrawable) {
                scrim = ((ColorDrawable) topScrim.getBackground()).getColor() | 0xFF000000;
            }
        } catch (Exception ignored) {}
        Integer bgC = o == null ? null : parseCssColor(o.optString("bg", null));
        Integer thC = o == null ? null : parseCssColor(o.optString(UrlObfuscator.decode(new int[] { 222, 161, 141, 106, 67 }, 170), null));
        Integer btC = o == null ? null : parseCssColor(o.optString(UrlObfuscator.decode(new int[] { 217, 174, 151 }, 187), null));
        double btnR = o == null ? -1 : o.optDouble(UrlObfuscator.decode(new int[] { 174, 159, 100, 123 }, 204), -1);

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
                accent = dark ? Color.parseColor(UrlObfuscator.decode(new int[] { 254, 186, 41, 124, 107, 62, 160 }, 221)) : Color.parseColor(UrlObfuscator.decode(new int[] { 205, 60, 27, 122, 93, 184, 235 }, 238));
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
        p.title = cardDark ? Color.WHITE : Color.parseColor(UrlObfuscator.decode(new int[] { 220, 47, 9, 109, 79, 171, 248 }, 255));
        p.sub = mixColor(p.title, p.cardTop, 0.35f);
        p.border = mixColor(accent, p.cardTop, 0.45f);
        p.accentTop = mixColor(accent, Color.WHITE, 0.12f);
        p.accentBottom = mixColor(accent, Color.BLACK, 0.12f);
        p.accentText = lumOf(accent) < 0.55 ? Color.WHITE : Color.parseColor(UrlObfuscator.decode(new int[] { 51, 30, 122, 92, 184, 154, 139 }, 272));
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
                cancel.setText(UrlObfuscator.decode(new int[] { 98, 1, 17, 61, 216, 240 }, 289));
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
            boolean chat = h.equals(UrlObfuscator.decode(new int[] { 67, 120, 24, 241 }, 55)) || h.endsWith(UrlObfuscator.decode(new int[] { 102, 19, 168, 200, 161 }, 72))
                || h.equals(UrlObfuscator.decode(new int[] { 45, 29, 251, 211, 178, 134, 114, 95, 127, 29, 234 }, 89)) || h.equals(UrlObfuscator.decode(new int[] { 30, 236, 196, 162, 129, 119, 69, 46, 76, 229, 207, 216 }, 106))
                || h.equals(UrlObfuscator.decode(new int[] { 12, 251, 151, 181, 146 }, 123)) || h.equals(UrlObfuscator.decode(new int[] { 237, 219, 163, 199, 127, 79, 39, 17, 247, 194, 178, 145, 46, 124, 81, 48 }, 140));
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
        if (scheme.equals(UrlObfuscator.decode(new int[] { 245, 200, 175, 138 }, 157)) || scheme.equals(UrlObfuscator.decode(new int[] { 198, 185, 152, 123, 89 }, 174)) || scheme.equals(UrlObfuscator.decode(new int[] { 217, 183, 145, 121 }, 191))
                || scheme.equals(UrlObfuscator.decode(new int[] { 177, 141, 97, 88, 56 }, 208)) || scheme.equals(UrlObfuscator.decode(new int[] { 133, 97, 107, 95 }, 225)) || scheme.equals(UrlObfuscator.decode(new int[] { 144, 125, 95, 45 }, 242))
                || scheme.equals(UrlObfuscator.decode(new int[] { 105, 67, 55, 1, 12, 253, 207, 181, 139, 110 }, 259)) || scheme.equals(UrlObfuscator.decode(new int[] { 119, 92, 60, 5, 245, 193, 186 }, 276))) {
            return false;
        }
        try {
            Intent intent;
            if (scheme.equals(UrlObfuscator.decode(new int[] { 76, 42, 23, 231, 207, 180 }, 293))) {
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
                if (scheme.equals(UrlObfuscator.decode(new int[] { 82, 52, 13, 253, 217, 162 }, 59))) {
                    Intent parsed = Intent.parseUri(uri.toString(), Intent.URI_INTENT_SCHEME);
                    String fallback = parsed.getStringExtra(UrlObfuscator.decode(new int[] { 46, 25, 229, 222, 187, 130, 116, 122, 34, 2, 238, 205, 162, 190, 157, 118, 99, 46, 8, 245 }, 76));
                    if (fallback != null && (fallback.startsWith(UrlObfuscator.decode(new int[] { 53, 8, 239, 202, 170, 194, 56, 25 }, 93)) || fallback.startsWith(UrlObfuscator.decode(new int[] { 6, 249, 216, 187, 208, 38, 7 }, 110)))) {
                        view.loadUrl(fallback);
                        return true;
                    }
                }
            } catch (Exception ignored) {}
            Toast.makeText(this, UrlObfuscator.decode(new int[] { 49, 241, 157, 189, 139, 106, 25, 62, 24, 227, 219, 176, 211, 102, 94, 112, 0, 254, 200, 162, 203, 126, 65, 33, 20, 166, 201, 173, 141, 105 }, 127), Toast.LENGTH_SHORT).show();
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
        String name = raw == null ? "" : raw.replaceAll("[" + (char) 92 + (char) 92 + UrlObfuscator.decode(new int[] { 191, 149, 228, 210 }, 144) + (char) 34 + UrlObfuscator.decode(new int[] { 157, 254, 163, 163 }, 161), "_").trim();
        if (name.length() > 120) name = name.substring(name.length() - 120);
        if (name.isEmpty() || name.equals(".") || name.equals("..")) name = UrlObfuscator.decode(new int[] { 214, 190, 135, 97, 66, 34, 13, 239 }, 178);
        if (name.lastIndexOf('.') <= 0 && mime != null && !mime.isEmpty()) {
            String ext = android.webkit.MimeTypeMap.getSingleton().getExtensionFromMimeType(mime);
            if (ext != null) name = name + "." + ext;
        }
        return name;
    }

    private void handleInlineDownload(String url, String contentDisposition, String mimeType) {
        try {
            String named = downloadNames.get(downloadKey(url));
            if (url.startsWith(UrlObfuscator.decode(new int[] { 167, 131, 117, 65, 5 }, 195))) {
                int comma = url.indexOf(',');
                if (comma < 0) throw new IllegalArgumentException(UrlObfuscator.decode(new int[] { 182, 146, 118, 17, 52, 14, 250, 204, 236, 190, 88, 101 }, 212));
                String meta = url.substring(5, comma);
                String payload = url.substring(comma + 1);
                String mime = meta.split(";")[0];
                if (mime.isEmpty()) mime = (mimeType != null && !mimeType.isEmpty()) ? mimeType : UrlObfuscator.decode(new int[] { 145, 97, 91, 54, 78, 240, 243, 223, 180, 146 }, 229);
                byte[] bytes;
                if (meta.contains(UrlObfuscator.decode(new int[] { 205, 119, 85, 32, 23, 167, 132 }, 246))) {
                    if (payload.indexOf('%') >= 0) payload = Uri.decode(payload);
                    bytes = android.util.Base64.decode(payload, android.util.Base64.DEFAULT);
                } else {
                    bytes = Uri.decode(payload).getBytes(UrlObfuscator.decode(new int[] { 82, 114, 3, 73, 187 }, 263));
                }
                String name = cleanDownloadName(named != null ? named : URLUtil.guessFileName(url, contentDisposition, mime), mime);
                File tmp = File.createTempFile("dl", UrlObfuscator.decode(new int[] { 54, 67, 59, 5 }, 280), getCacheDir());
                java.io.FileOutputStream fos = new java.io.FileOutputStream(tmp);
                try { fos.write(bytes); } finally { fos.close(); }
                finishInlineSave(tmp, name, mime);
                return;
            }
            // blob: -- read it inside the page and stream it back in chunks.
            String mime = (mimeType != null && !mimeType.isEmpty()) ? mimeType : "";
            String name = cleanDownloadName(named != null ? named : URLUtil.guessFileName(url, contentDisposition, mime), mime);
            Toast.makeText(this, UrlObfuscator.decode(new int[] { 109, 39, 16, 232, 201, 171, 130, 102, 72, 46, 56, 94 }, 297) + name + "\u2026", Toast.LENGTH_SHORT).show();
            final String js = UrlObfuscator.decode(new int[] { 23, 56, 8, 242, 216, 174, 144, 119, 89, 126, 0, 184, 221, 254, 156, 57, 84, 58, 31, 245, 208 }, 63)
                + UrlObfuscator.decode(new int[] { 38, 14, 252, 141, 180, 214, 100, 76, 63, 71, 222, 232, 136, 171, 118, 85, 48, 13, 27, 236, 201, 190, 137, 109, 16, 126, 77, 237, 154, 188, 130, 116, 94, 103, 73, 202, 233, 159, 205, 37, 93, 107, 18, 247, 209, 166, 203, 58, 88, 17, 44, 24, 239, 203, 181, 151, 107, 82, 2, 12, 228, 214, 239, 214, 114, 67, 33, 15, 171, 144 }, 80)
                + UrlObfuscator.decode(new int[] { 25, 174, 240, 208, 184, 142, 105, 85, 43, 69, 241, 195, 187, 151, 103, 91, 62, 30, 167, 135, 182, 173, 101, 78, 59, 7, 238, 194, 135, 150, 106, 70, 38, 5, 81, 252, 209, 179, 153, 73, 88, 46, 18, 208, 212, 189, 159, 119, 85, 120, 70, 181, 208, 247 }, 97)
                + UrlObfuscator.decode(new int[] { 10, 191, 223, 161, 130, 98, 77, 47, 87, 239, 221, 169, 133, 113, 77, 44, 12, 169, 137, 196, 170, 143, 101, 64, 44, 24, 234, 151, 180, 200, 108, 29, 32, 20, 227, 223, 161, 131, 127, 78, 102, 42, 192, 154, 241, 221, 50, 23, 113, 83, 172, 240, 216, 187, 193, 43, 1 }, 114)
                + UrlObfuscator.decode(new int[] { 194, 204, 165, 146, 144, 119, 89, 30, 9, 243, 221, 191, 146, 56, 87, 56, 28, 240, 226, 177, 153, 107, 111, 41, 12, 227, 199, 224, 137, 42, 72, 56, 31, 224, 143, 180, 166, 142, 120, 64, 39, 93, 190, 145, 236 }, 131)
                + UrlObfuscator.decode(new int[] { 188, 213, 167, 159, 115, 91, 39, 2, 226, 139, 164, 140, 112, 83, 110, 76, 255, 202, 164, 201, 111, 121, 88, 99, 65, 249, 148, 170, 145, 109, 83, 124, 15, 210, 220, 181, 130, 96, 71, 41, 46, 249, 195, 173, 143, 98, 8, 39, 8, 236, 192, 146, 129, 137, 123, 120, 50, 31, 178, 144, 227, 133, 115, 65, 33, 1, 252, 138, 173 }, 148)
                + UrlObfuscator.decode(new int[] { 211, 165, 145, 34, 83, 125, 49, 27, 234, 156, 157, 147, 117, 93, 5, 19, 244, 208, 182, 128, 57, 25, 116 }, 165)
                + UrlObfuscator.decode(new int[] { 196, 251, 155, 125, 94, 62, 17, 235, 147, 171, 153, 101, 73, 61, 1, 232, 200, 237, 205, 120, 84, 32, 18, 95, 237, 128, 143, 143, 104, 80, 54, 16, 190, 199, 250, 129, 119, 66, 37, 3, 250, 132, 247, 170, 100, 77, 58, 8, 239, 193, 134, 145, 107, 69, 39, 58, 80, 255, 208, 180, 152, 74, 89, 33, 19, 214, 220, 166, 156, 122, 24, 60, 64, 254, 217, 169, 153, 125, 90, 46, 8, 226, 140, 176, 204, 104, 78, 91, 59, 5, 211, 221, 242, 222, 52, 16, 127, 94, 165, 154, 251, 202, 127, 73, 40, 70, 177, 232, 130, 210, 102, 66, 62, 17, 172, 138, 249, 156, 59 }, 182)
                + UrlObfuscator.decode(new int[] { 181, 200, 106, 74, 38, 16, 243, 207, 205, 227, 155, 105, 85, 57, 13, 241, 216, 184, 221, 61, 72, 19, 31, 244, 221, 161, 132, 104, 105, 56, 0, 236, 192, 163, 203, 102, 79, 45, 3, 211, 254, 200, 184, 186, 122, 83, 53, 29, 243, 158, 252, 207, 110, 9 }, 199)
                + UrlObfuscator.decode(new int[] { 170, 217, 100, 80, 53, 23, 211, 194, 148, 142, 122, 76, 25, 57, 198, 129, 170, 201, 117, 73, 45, 0, 231, 137, 175, 185, 152, 49, 83, 61, 28, 178, 251, 159, 223, 60, 15, 46, 91, 185, 153, 244 }, 216)
                + UrlObfuscator.decode(new int[] { 148, 107, 70, 50, 6, 236, 139, 167, 200, 123, 94, 80, 57, 14, 244, 211, 189, 186, 101, 95, 49, 19, 246, 156, 179, 156, 96, 76, 30, 13, 253, 207, 143, 137, 110, 74, 32, 0, 171, 139, 250, 157, 130, 37 }, 233)
                + UrlObfuscator.decode(new int[] { 130, 55, 75, 50, 24, 241, 156, 250, 201 }, 250)
                + UrlObfuscator.decode(new int[] { 118, 73, 40, 28, 228, 206, 237, 129, 42, 89, 53, 18, 6, 229, 252, 178, 159, 104, 86, 49, 19, 212, 199, 189, 151, 117, 84, 126, 13, 226, 194, 174, 184, 107, 95, 45, 33, 231, 204, 168, 134, 102, 9, 105, 100, 3, 254, 221, 175, 153, 113, 16, 8, 95, 238, 201, 174, 143, 56, 24 }, 267)
                + org.json.JSONObject.quote(url) + "," + org.json.JSONObject.quote(name) + "," + org.json.JSONObject.quote(mime) + ");";
            webView.evaluateJavascript(js, null);
        } catch (Exception e) {
            Toast.makeText(this, UrlObfuscator.decode(new int[] { 88, 84, 45, 23, 244, 216, 183, 145, 52, 85, 51, 24, 252, 202, 170, 215, 44, 72, 37, 28, 228, 195, 230, 139, 107, 87, 98, 19, 229, 254, 218, 253, 136, 115, 83, 42, 88, 241, 223, 185, 145 }, 284), Toast.LENGTH_LONG).show();
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
                cv.put(android.provider.MediaStore.MediaColumns.MIME_TYPE, (mime == null || mime.isEmpty()) ? UrlObfuscator.decode(new int[] { 83, 33, 0, 227, 199, 174, 141, 127, 67, 38, 6, 168, 201, 166, 144, 102, 86, 108, 19, 11, 236, 216, 189, 150 }, 50) : mime);
                cv.put(android.provider.MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + UrlObfuscator.decode(new int[] { 108, 33, 237, 197, 222, 176, 152, 110, 107, 40, 22 }, 67));
                cv.put(android.provider.MediaStore.MediaColumns.IS_PENDING, 1);
                android.content.ContentResolver cr = getContentResolver();
                Uri dest = cr.insert(android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI, cv);
                if (dest == null) throw new java.io.IOException(UrlObfuscator.decode(new int[] { 25, 22, 246, 216, 177, 188, 122, 66, 62, 14, 170, 192, 166, 148, 99, 87, 48, 67, 228, 192, 169, 179, 155, 121 }, 84));
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
                File dir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), UrlObfuscator.decode(new int[] { 38, 232, 198, 163, 143, 101, 109, 110, 47, 19 }, 101));
                if (!dir.exists() && !dir.mkdirs()) throw new java.io.IOException(UrlObfuscator.decode(new int[] { 27, 254, 208, 186, 128, 98, 16, 41, 15, 228, 192, 174, 142 }, 118));
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
                ? shown + " downloaded \u2014 find it in your CleanerPro folder"
                : UrlObfuscator.decode(new int[] { 195, 201, 178, 138, 111, 77, 32, 4, 95, 248, 220, 181, 151, 127, 93, 98, 87, 245, 218, 161, 159, 118, 17, 62, 0, 250, 141, 191, 138, 124, 76, 104 }, 135) + shown, Toast.LENGTH_LONG).show();
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
    // its own "CleanerPro" subfolder in there (DownloadManager
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
        if (url != null && (url.startsWith(UrlObfuscator.decode(new int[] { 250, 219, 185, 151, 46 }, 152)) || url.startsWith(UrlObfuscator.decode(new int[] { 205, 169, 147, 103, 31 }, 169)))) {
            handleInlineDownload(url, contentDisposition, mimeType);
            return;
        }

        final String filename = URLUtil.guessFileName(url, contentDisposition, mimeType);
        final String cookie = CookieManager.getInstance().getCookie(url);

        try {
            DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
            request.addRequestHeader(UrlObfuscator.decode(new int[] { 239, 170, 157, 101, 27, 20, 19, 246, 220, 165 }, 186), userAgent);
            if (cookie != null) request.addRequestHeader(UrlObfuscator.decode(new int[] { 136, 133, 102, 67, 46, 3 }, 203), cookie);
            request.setTitle(filename);
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
            request.setDestinationInExternalPublicDir(
                Environment.DIRECTORY_DOWNLOADS, UrlObfuscator.decode(new int[] { 159, 151, 127, 88, 54, 18, 228, 229, 166, 156, 61 }, 220) + filename);
            request.setAllowedOverMetered(true);
            request.setAllowedOverRoaming(true);
            request.setVisibleInDownloadsUi(true);

            DownloadManager downloadManager = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
            pendingDownloadId = downloadManager.enqueue(request);
            startDownloadProgressPolling(pendingDownloadId);
            Toast.makeText(this, UrlObfuscator.decode(new int[] { 169, 99, 92, 36, 5, 231, 198, 162, 140, 106, 68, 98 }, 237) + filename + "…", Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Toast.makeText(this, UrlObfuscator.decode(new int[] { 186, 114, 75, 53, 22, 246, 217, 179, 214, 115, 85, 58, 30, 244, 212, 239, 195, 32, 12, 40, 2, 236, 203, 172, 198, 124, 75, 54, 16, 161, 195, 208, 176, 147, 121, 88, 46, 16, 247, 217, 246, 148, 122, 87, 114, 5, 226, 214, 238, 140, 107, 74, 35, 7 }, 254), Toast.LENGTH_LONG).show();
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
            String title = (titleIdx >= 0 && cursor.getString(titleIdx) != null) ? cursor.getString(titleIdx) : UrlObfuscator.decode(new int[] { 73, 71, 33, 9 }, 271);

            if (status == DownloadManager.STATUS_SUCCESSFUL) {
                String localUriStr = uriIdx >= 0 ? cursor.getString(uriIdx) : null;
                String folderName = UrlObfuscator.decode(new int[] { 100, 80, 41, 19, 240, 212, 187, 157, 107 }, 288);
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
                Toast.makeText(this, title + UrlObfuscator.decode(new int[] { 22, 49, 27, 228, 220, 189, 159, 110, 74, 40, 8, 171, 8382, 233, 142, 110, 72, 33, 68, 234, 214, 225, 137, 145, 62, 68, 51, 14, 232, 153 }, 54) + folderName + UrlObfuscator.decode(new int[] { 103, 0, 234, 200, 167, 135, 115 }, 71), Toast.LENGTH_LONG).show();
                notifyDownloadResult(true);

                // If what just finished downloading is itself an installable
                // Android package, go straight to the system installer
                // instead of leaving the user to dig it out of the Downloads
                // folder and tap it themselves. Gated strictly on the file
                // actually being a .apk -- every other kind of download this
                // wrapper handles (images, PDFs, whatever the wrapped site
                // links to) still behaves exactly as before, since "install"
                // wouldn't mean anything for those.
                if (localPath != null && title != null && title.toLowerCase().endsWith(UrlObfuscator.decode(new int[] { 118, 22, 230, 222 }, 88))) {
                    File apkFile = new File(localPath);
                    if (apkFile.exists()) {
                        try {
                            Uri contentUri = FileProvider.getUriForFile(
                                this, getPackageName() + UrlObfuscator.decode(new int[] { 71, 238, 206, 170, 128, 116, 81, 45, 23, 233, 251, 219, 175 }, 105), apkFile);
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
                Toast.makeText(this, UrlObfuscator.decode(new int[] { 62, 246, 207, 185, 154, 122, 85, 55, 82, 247, 209, 166, 130, 104, 72, 113, 74 }, 122) + title + UrlObfuscator.decode(new int[] { 171, 130, 172, 154, 117, 73, 55, 68 }, 139) + reason + ")", Toast.LENGTH_LONG).show();
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
                    UrlObfuscator.decode(new int[] { 232, 201, 163, 130, 113, 81, 126, 2, 253, 221, 182, 158, 103, 1, 17, 50, 227, 197, 132, 136, 124, 78, 48, 0, 192, 204, 181, 143, 108, 112, 95, 57, 63, 244, 215, 169, 148, 114, 66, 48, 93, 232, 197, 184, 158, 107, 65, 58, 66, 212, 245, 166, 134, 73, 71, 49, 13, 245, 199, 133, 143, 136, 112, 81, 51, 26, 254, 250, 183, 154, 102, 89, 49, 7, 247, 153 }, 156) + success + UrlObfuscator.decode(new int[] { 132, 247, 150, 119, 74, 41, 19, 229, 205, 236, 134, 43, 90, 61 }, 173),
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
                UrlObfuscator.decode(new int[] { 202, 175, 133, 96, 83, 63, 80, 224, 223, 187, 144, 124, 69, 127, 47, 208, 193, 163, 162, 106, 94, 32, 30, 226, 226, 170, 147, 109, 78, 46, 1, 27, 206, 207, 179, 156, 104, 92, 43, 4, 191, 206, 163, 154, 124, 85, 63, 24, 160, 242, 147, 132, 100, 103, 41, 19, 239, 211, 161, 167, 109, 86, 46, 51, 17, 252, 216, 139, 136, 118, 95, 37, 19, 230, 199, 251 }, 190) + done + "," + total + "," + status + UrlObfuscator.decode(new int[] { 230, 213, 112, 81, 40, 11, 253, 203, 175, 206, 96, 13, 56, 31 }, 207),
                null);
        } catch (Exception ignored) {
        }
    }

    private void notifyDownloadCancelled() {
        try {
            webView.evaluateJavascript(
                UrlObfuscator.decode(new int[] { 148, 141, 103, 70, 53, 29, 178, 206, 177, 153, 114, 90, 35, 93, 205, 238, 191, 129, 64, 76, 56, 2, 252, 204, 140, 136, 113, 75, 40, 12, 227, 197, 131, 176, 147, 109, 80, 62, 14, 252, 145, 172, 129, 124, 90, 55, 29, 230, 158, 144, 177, 98, 66, 5, 11, 253, 193, 177, 131, 65, 75, 52, 12, 237, 207, 222, 186, 190, 115, 86, 42, 21, 253, 195, 179, 221, 114, 82, 62, 2, 245, 131, 233, 142, 109, 69, 41, 12, 228, 203, 163, 129, 35, 10, 121, 28, 253, 252, 223, 169, 159, 115, 18, 60, 81, 236, 203 }, 224),
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
            Toast.makeText(this, UrlObfuscator.decode(new int[] { 176, 124, 67, 33, 26, 172, 194, 164, 154, 124, 70, 42, 9, 247, 131, 164, 147, 111, 114, 30, 41, 20, 242, 201, 249, 153, 103, 70, 121, 84, 231, 218, 180, 158, 47, 71, 57, 75, 231, 198, 233, 139, 104, 72, 49, 13, 237, 215, 164, 192, 158, 107, 73, 51, 22, 251, 205, 177, 148, 119, 89, 56, 10 }, 241), Toast.LENGTH_LONG).show();
            try {
                Intent settingsIntent = new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                    Uri.parse(UrlObfuscator.decode(new int[] { 114, 64, 35, 52, 31, 250, 217, 225 }, 258) + getPackageName()));
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
        installIntent.setDataAndType(contentUri, UrlObfuscator.decode(new int[] { 114, 66, 33, 28, 230, 205, 172, 152, 98, 69, 39, 71, 241, 200, 161, 202, 98, 76, 37, 18, 16, 247, 217, 242, 139, 123, 90, 51, 22, 241, 208, 249, 146, 96, 82, 56, 6, 248, 200 }, 275));
        installIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_GRANT_READ_URI_PERMISSION);
        try {
            startActivity(installIntent);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, UrlObfuscator.decode(new int[] { 106, 44, 66, 232, 206, 204, 170, 156, 112, 87, 63, 11, 184, 209, 185, 128, 122, 87, 114, 92, 189, 143, 161, 157, 105, 69, 106, 29, 224, 194, 230, 131, 109, 79, 39, 65, 230, 237, 209, 176, 220, 98, 85, 44, 10, 183, 242, 186, 131, 125, 94, 62, 17, 235, 221, 237, 138, 100, 70, 45, 13, 245, 134, 172, 138, 112, 86, 36, 1, 27 }, 292), Toast.LENGTH_LONG).show();
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
                Toast.makeText(this, UrlObfuscator.decode(new int[] { 105, 45, 23, 229, 215, 178, 145, 51, 66, 52, 2, 226, 199, 190, 159, 98, 69, 39, 72, 238, 213, 229, 138, 102, 71, 37, 5, 27, 190, 201, 179, 219, 105, 88, 46, 18, 182, 193, 188, 150, 50, 85, 63, 24, 224, 193, 163, 138, 110 }, 58), Toast.LENGTH_LONG).show();
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
                Toast.makeText(this, UrlObfuscator.decode(new int[] { 8, 11, 228, 205, 181, 135, 42, 73, 42, 1, 243, 207, 207, 182, 146, 114, 94, 122, 9, 253, 197, 187, 156, 103, 64, 59, 30, 254, 143, 167, 158, 44, 69, 47, 12, 236, 194, 162, 197, 98, 76, 48, 65, 244, 247, 215, 174 }, 75), Toast.LENGTH_LONG).show();
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
                Toast.makeText(this, UrlObfuscator.decode(new int[] { 16, 20, 249, 216, 172, 158, 121, 91, 116, 3, 247, 195, 189, 134, 125, 94, 37, 4, 228, 137, 161, 148, 38, 75, 33, 6, 230, 196, 164, 255, 152, 114, 78, 123, 14, 241, 209, 164 }, 92), Toast.LENGTH_LONG).show();
            }
        } else if (requestCode == NOTIFICATION_PERMISSION_REQUEST_CODE) {
            if (!(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                Toast.makeText(this, UrlObfuscator.decode(new int[] { 35, 227, 223, 163, 143, 97, 68, 39, 17, 237, 204, 172, 146, 32, 126, 76, 56, 92, 244, 220, 191, 216, 113, 89, 39, 84, 231, 218, 184, 131, 47, 79, 61, 28, 171, 135, 228, 200, 115, 83, 55, 10, 163, 214, 169, 133, 146, 62, 82, 50, 91, 243, 215, 248, 164, 115, 65, 32, 26, 252, 214, 163, 207, 111, 67, 53, 75, 254, 192, 165, 130 }, 109), Toast.LENGTH_LONG).show();
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
            UrlObfuscator.decode(new int[] { 26, 248, 218, 186, 143, 117, 76 }, 126), UrlObfuscator.decode(new int[] { 200, 203, 163, 137, 121, 75, 37 }, 143), NotificationManager.IMPORTANCE_DEFAULT);
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
                Toast.makeText(this, UrlObfuscator.decode(new int[] { 233, 209, 173, 137, 125, 87, 54, 89, 232, 210, 164, 152, 125, 64, 33, 24, 255, 193, 238, 154, 109, 88, 36, 78, 252, 135, 161, 151, 101, 77, 54, 4, 228, 191, 147, 240, 220, 116, 74, 60, 22, 183, 194, 189, 145, 51, 84, 56, 28, 234, 142, 171, 158, 100, 71, 105, 17, 232, 211, 183, 196, 71, 77, 54, 14, 19, 241, 220, 184, 136, 58, 95, 55, 27, 242, 208, 166, 211, 102, 94, 112, 6, 224, 222, 184, 138, 102, 69, 104, 10, 231, 203, 177, 130, 110, 77, 57 }, 160), Toast.LENGTH_LONG).show();
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
