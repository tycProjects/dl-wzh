package com.generated.bankchoanhdi;

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
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final RectF arcBox = new RectF();
        private float arcStart = -90f, arcSweep = 20f;
        private int cycle = 0;
        private ValueAnimator anim;

        BouncingDotsView(Context context, int color) {
            super(context);
            paint.setColor(color);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeCap(Paint.Cap.ROUND);
            setWillNotDraw(false);
        }

        void setColor(int color) {
            paint.setColor(color);
            invalidate();
        }

        // Only animates while actually on screen (the overlay sits GONE
        // between navigations, so nothing ticks in the background).
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

        private static float ease(float u) {
            return 0.5f - 0.5f * (float) Math.cos(Math.PI * u);
        }

        private void startAnim() {
            if (anim != null) return;
            cycle = 0;
            anim = ValueAnimator.ofFloat(0f, 1f);
            anim.setDuration(1400);
            anim.setInterpolator(new android.view.animation.LinearInterpolator());
            anim.setRepeatCount(ValueAnimator.INFINITE);
            anim.addListener(new android.animation.AnimatorListenerAdapter() {
                @Override
                public void onAnimationRepeat(android.animation.Animator animator) {
                    cycle++;
                }
            });
            anim.addUpdateListener(animator -> {
                float t = (float) animator.getAnimatedValue();
                float tail;
                float len;
                if (t < 0.5f) {
                    // Head races ahead, tail stays put: the arc grows.
                    tail = 0f;
                    len = 20f + 250f * ease(t * 2f);
                } else {
                    // Tail catches up, head holds: the arc shrinks.
                    float e = ease((t - 0.5f) * 2f);
                    tail = 250f * e;
                    len = 270f - 250f * e;
                }
                // cycle * 250 carries the tail's travel into the next loop so
                // the arc never jumps when the animation repeats.
                arcStart = -90f + cycle * 250f + t * 360f + tail;
                arcSweep = len;
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
            paint.setStrokeWidth(size * 0.09f);
            float d = size * 0.72f;
            float cx = getWidth() / 2f;
            float cy = getHeight() / 2f;
            arcBox.set(cx - d / 2f, cy - d / 2f, cx + d / 2f, cy + d / 2f);
            canvas.drawArc(arcBox, arcStart, arcSweep, false, paint);
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
            int iconResId = context.getResources().getIdentifier(UrlObfuscator.decode(new int[] { 200, 163, 128, 146, 124, 73, 53, 25, 241, 221, 165 }, 161), UrlObfuscator.decode(new int[] { 223, 184, 128, 98, 79, 61 }, 178), context.getPackageName());
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
        root.setBackgroundColor(Color.parseColor(UrlObfuscator.decode(new int[] { 224, 210, 52, 16, 10, 110, 72 }, 195)));

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
        swipeRefresh.setColorSchemeColors(Color.parseColor(UrlObfuscator.decode(new int[] { 247, 193, 86, 7, 19, 43, 200 }, 212)));
        swipeRefresh.addView(webView, new ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        root.addView(swipeRefresh, webParams);

        // Status-bar and navigation-bar strips. Added before the splash /
        // error / navigation overlays so those still cover the whole screen.
        topScrim = new View(this);
        topScrim.setBackgroundColor(Color.parseColor(UrlObfuscator.decode(new int[] { 198, 52, 22, 114, 84, 176, 170 }, 229)));
        root.addView(topScrim, new FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT, 0, Gravity.TOP));
        bottomScrim = new View(this);
        bottomScrim.setBackgroundColor(Color.parseColor(UrlObfuscator.decode(new int[] { 213, 37, 1, 99, 71, 161, 133 }, 246)));
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

        final SplashController loading = new LoadingSplashView(this, Color.parseColor(UrlObfuscator.decode(new int[] { 36, 22, 117, 84, 179, 146, 241 }, 263)), UrlObfuscator.decode(new int[] { 90, 86, 56, 30, 215, 219, 189, 176, 126, 71, 10, 4 }, 280), 3);
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
        final Runnable showNavOverlay = () -> {
            navOverlay.animate().cancel();
            navOverlay.setAlpha(0f);
            navOverlay.setVisibility(View.VISIBLE);
            navOverlay.animate().alpha(1f).setDuration(120).start();
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
        }, UrlObfuscator.decode(new int[] { 115, 63, 20, 253, 193, 164, 136, 78, 88, 59, 7, 245 }, 50));

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
        String spoofedUA = defaultUA.replace(UrlObfuscator.decode(new int[] { 120, 66, 246, 214 }, 67), "").replaceAll("Version/[0-9.]+\s", "");
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
        webView.addJavascriptInterface(new AndroidBridge(), UrlObfuscator.decode(new int[] { 21, 29, 246, 195, 191, 134, 106, 111, 62, 2, 238, 206, 173 }, 84));

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
                    navOverlay.postDelayed(showNavOverlay, 200);
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
                        UrlObfuscator.decode(new int[] { 17, 246, 218, 185, 136, 102, 55, 73, 52, 18, 255, 213, 174, 214, 86, 88, 49, 6, 252, 219, 181, 178, 125, 71, 41, 11, 238, 140, 239, 169, 105, 66, 55, 11, 234, 198, 131, 146, 150, 122, 90, 57, 85, 233, 216, 174, 146, 69, 94, 49, 31, 247, 197, 191, 129, 40, 11, 109, 28, 227, 199, 172, 136, 113, 11, 27, 60, 241, 202, 165, 179, 188, 114, 73, 53, 30, 176, 195, 160, 159, 123, 80, 60, 5, 191, 239, 144, 157, 102, 73, 39, 40, 230, 221, 169, 130, 56, 80, 49, 23, 228, 155 }, 101) +
                        UrlObfuscator.decode(new int[] { 0, 244, 198, 243, 145, 112, 64, 114, 8, 248, 194, 168, 158, 96, 71, 41, 78, 172, 223, 183, 144, 120, 91 }, 118) +
                        UrlObfuscator.decode(new int[] { 238, 192, 237, 147, 106, 76, 37, 15, 8, 176, 206, 191, 137, 117, 85, 52, 46, 168, 131, 228, 218, 96, 84, 36, 26, 252, 195, 247 }, 135) +
                        UrlObfuscator.decode(new int[] { 238, 214, 164, 213, 67, 14, 37, 24, 254, 203, 161, 154, 34, 66, 36, 7, 237, 213, 145, 140, 96, 87, 42, 77, 200, 162, 201, 180, 146, 127, 85, 46, 86, 254, 216, 187, 145, 97, 122, 52, 25, 232, 198, 185, 215, 98, 76, 97, 73, 208, 218, 185, 197, 75, 11, 51, 5, 11, 235, 207, 178, 192 }, 152) +
                        UrlObfuscator.decode(new int[] { 223, 169, 149, 38, 74, 49, 23, 191, 250, 157, 228 }, 169) +
                        UrlObfuscator.decode(new int[] { 204, 184, 138, 55, 70, 32, 7, 251, 143, 183, 133, 97, 77, 57, 5, 228, 196, 225, 132, 43, 82, 105, 19, 175, 202, 237, 146, 158, 122, 20, 39, 20, 239, 205, 246, 135, 99, 70, 60, 91, 201, 154, 248, 131, 33, 122, 101, 69, 254, 198, 142, 142, 126, 64, 32, 75, 182, 136, 236, 244, 214, 105, 19, 19, 83, 183, 204, 184, 176, 124, 76, 54, 22, 185, 132, 230, 194, 38, 4, 60, 69, 222, 129, 233, 146, 106, 98, 42, 26, 228, 196, 151, 234, 212, 48, 16, 114, 17, 183, 255, 255, 219, 96, 92, 20, 24, 232, 202, 170, 197, 56, 2, 102, 66, 160, 213, 167, 129, 43, 116, 107, 79, 244, 240, 248, 180, 132, 126, 94, 113, 76, 190, 235, 252, 207, 110, 9 }, 186) +
                        UrlObfuscator.decode(new int[] { 189, 139, 123, 8, 47, 7, 246, 240, 166, 154, 117, 29, 89, 43, 19, 255, 207, 179, 150, 118, 31, 51, 25, 189, 200, 180, 158, 98, 7, 56, 12, 254, 139, 163, 212, 56, 28, 47, 89, 225, 207, 236, 130, 104, 118, 82, 57, 50, 244, 222, 188, 139, 57, 90, 48, 26, 244, 198, 185, 203, 102, 5, 102, 69, 240, 220, 168, 154, 39, 72, 120, 1, 239, 140, 162, 136, 150, 114, 89, 18, 20, 254, 220, 171, 172, 127, 104, 111, 26, 244, 153, 190, 193, 96, 66, 40, 14, 222, 208, 184, 130, 59, 24, 121, 80, 164, 135, 174, 241, 144, 114, 88, 62, 44, 248, 212, 162, 147, 59, 64, 33, 27, 252, 152, 230, 192, 97, 73, 37, 13, 253, 192, 249, 214, 44, 86, 38, 22, 244, 210, 209, 254, 137, 110, 78, 63, 66, 229, 197, 179, 129, 97, 65, 60, 81, 246, 206, 162, 158, 105, 16, 55, 82 }, 203) +
                        UrlObfuscator.decode(new int[] { 170, 154, 104, 25, 54, 24, 242, 208, 167, 206, 118, 94, 51, 26, 227, 200, 162, 159, 36, 88, 61, 2, 244, 220, 151, 134, 110, 68, 35, 43, 17, 239, 253, 183, 150, 49, 31, 62, 27, 242, 152, 165, 155, 117, 85, 32, 66, 238, 205, 165, 156, 104, 91, 107, 21, 243, 195, 239, 128, 116, 84, 75, 49, 19, 176, 210, 180, 137, 109, 67, 122, 6, 241, 223, 183, 146, 100, 3, 58, 8, 244, 223, 171, 155, 109, 70, 106, 62, 246, 204, 174, 132, 61, 125, 75, 41, 8, 244, 212, 132, 212, 127, 7, 121, 28, 161, 158, 185, 195, 35, 70, 121, 64, 251, 134, 165, 129, 43, 71, 105, 23, 243, 195, 175, 204, 147, 127, 95, 57, 23, 189, 144, 227 }, 220) +
                        UrlObfuscator.decode(new int[] { 139, 99, 89, 98, 31, 233, 213, 230, 140, 57, 19, 121, 8, 188, 241, 209, 185, 153, 104, 20, 53, 29, 249, 209, 161, 156, 53, 20, 62, 5, 251, 128, 161, 137, 101, 77, 61, 0, 187, 151, 247, 212, 56, 75, 106, 75, 86, 229 }, 237) +
                        UrlObfuscator.decode(new int[] { 136, 124, 78, 123, 31, 245, 133, 185, 153, 113, 81, 32, 41, 248, 237, 244, 152, 108, 94, 107, 24, 180, 205, 171, 200, 98, 65, 55, 32, 238, 213, 209, 186, 148, 114, 92, 25, 21, 241, 210, 184, 129, 70, 86, 49, 5, 184, 134, 245 }, 254) +
                        UrlObfuscator.decode(new int[] { 102, 72, 101, 30, 165, 221, 160, 140, 115, 78, 121, 86, 179, 222, 189, 146, 209, 118, 88, 53, 28, 242, 205, 228, 207, 106, 73, 38, 93, 240, 222, 164, 155, 97, 64, 112, 86, 186, 213, 180, 149, 40, 81, 43, 19, 188, 156, 136, 163, 130, 111, 18, 41, 19, 254, 208, 163, 202, 40, 4, 47, 14, 227, 158, 163, 139, 107, 88, 117, 87, 222, 129, 164, 137, 107, 80, 42, 12, 244, 197, 132 }, 271) +
                        UrlObfuscator.decode(new int[] { 73, 89, 118, 15, 178, 204, 179, 157, 108, 95, 124, 7, 186, 219, 183, 152, 119, 71, 58, 83, 188, 133, 255, 195, 95, 13, 14, 76, 231, 204, 172, 149, 105, 113, 75, 56, 71 }, 288) +
                        UrlObfuscator.decode(new int[] { 64, 52, 6, 179, 209, 162, 205, 104, 75, 57, 47, 228, 199, 185, 157, 115, 67, 33, 55, 247, 219, 173, 133, 215, 123, 81, 117, 64 }, 54) +
                        UrlObfuscator.decode(new int[] { 46, 0, 173, 199, 176, 204, 101, 73, 76, 46, 17, 253, 194, 231, 196, 37, 16, 56, 26, 250, 214, 245, 141, 108, 76, 61, 67, 250, 194, 185, 128, 106, 78, 42, 12, 240, 218, 255, 220, 61, 56, 86, 52, 24, 255, 223, 183, 223, 107, 74, 37, 21, 225, 193, 180, 182, 99, 65, 44, 24, 163, 201, 186, 198, 104, 86, 36, 7, 234, 214, 184, 156, 131, 57, 12, 123, 82, 166, 137, 246, 199, 35, 28, 55, 28, 252, 197, 185, 129, 123, 72, 119 }, 71) +
                        UrlObfuscator.decode(new int[] { 46, 22, 228, 149, 160, 146, 117, 12, 53, 3, 160, 217, 173, 140, 68, 72, 37, 2, 168, 209, 171, 175, 109, 86, 37, 45, 61, 252, 207, 190, 210, 48, 3 }, 88) +
                        UrlObfuscator.decode(new int[] { 31, 233, 213, 230, 140, 119, 119, 39, 25, 244, 162, 150, 169, 157, 124, 7, 100, 69, 176, 222, 228, 211, 111, 78, 37, 17, 232, 147, 240, 209, 44, 66, 123, 79, 251, 218, 177, 133, 100, 31, 124, 93, 88, 246, 142, 251, 135, 102, 77, 57, 16, 171, 136, 233, 212, 122, 5, 119, 19, 242, 217, 173, 140, 55, 20, 117, 64, 246, 130, 184, 159, 118, 64, 39, 98, 67, 160, 155, 183, 147, 62, 68, 43, 2, 244, 211, 238, 207, 44, 23, 46, 73, 241, 208, 191, 139, 110, 21, 122, 91, 162, 215, 179, 131, 111, 7, 67, 34, 9, 253, 220, 231, 196, 37, 16, 58, 20, 246, 214, 190, 214, 57, 20 }, 105) +
                        UrlObfuscator.decode(new int[] { 19, 255, 144, 190, 133, 65, 81, 43, 6, 184, 203 }, 122) +
                        UrlObfuscator.decode(new int[] { 226, 204, 225, 201, 111, 71, 54, 48, 230, 218, 181, 200, 154, 114, 20, 117, 24, 245, 215, 172, 158, 120, 64, 49, 72 }, 139) +
                        UrlObfuscator.decode(new int[] { 234, 218, 168, 217, 126, 68, 107, 5, 245, 193, 161, 148, 86, 67, 33, 12, 248, 131, 169, 154, 38, 65, 41, 11, 240, 240, 171, 155, 101, 54, 66, 33, 77, 173, 129, 175, 153, 101, 22, 57, 28, 174, 194, 176, 130, 124, 75, 11, 0, 228, 203, 189, 192, 100, 85, 107, 8, 234, 204, 164, 168, 154, 119, 90, 52, 15, 179, 130, 177, 145, 62, 20, 56, 27, 238, 205, 188, 135, 50, 75, 63, 66, 230, 193, 245, 129, 117, 15, 117, 77, 177, 154 }, 156) +
                        UrlObfuscator.decode(new int[] { 219, 173, 153, 42, 69, 33, 9, 227, 214, 249, 174, 99, 85, 40, 113, 19, 252, 196, 243, 203, 53, 117, 54, 2, 253, 154, 190, 155, 127, 24, 124, 66, 192, 205, 191, 130, 39, 90, 40, 19, 235, 192, 235, 144, 47, 72, 90, 55, 26, 244, 207, 245, 149, 112, 30, 127, 92, 175 }, 173) +
                        UrlObfuscator.decode(new int[] { 200, 188, 142, 59, 88, 49, 69, 218, 215, 161, 156, 61, 95, 48, 8, 167, 150, 225, 138, 120, 0, 121, 70, 191, 147, 236, 223, 117, 67, 51, 64, 19, 251, 219, 168, 198, 104, 23, 52, 18, 240, 193, 239, 133, 115, 67, 112, 24, 190, 144, 190, 197, 125, 64, 44, 19, 238, 158 }, 190) +
                        UrlObfuscator.decode(new int[] { 166, 136, 37, 64, 34, 4, 236, 219, 250, 219, 56, 21, 106, 25, 245, 210, 198, 165, 139, 125, 73, 122, 11, 255, 138, 178, 154, 119, 70, 63, 20, 254, 219, 224, 142, 126, 78, 43, 29, 237, 245, 167, 139, 99, 70, 106, 72, 187, 237, 217, 243, 143, 126, 86, 60, 27, 227, 248, 186, 144, 118, 113, 62, 30, 251, 203, 163, 152, 120, 2, 44, 4, 174, 157, 179, 133, 113, 2, 51, 18, 66, 236, 218, 242, 156, 127, 77, 26, 24, 227, 219, 176, 154, 124, 86, 19, 3, 231, 200, 162, 159, 88, 76, 43, 19, 174, 140, 255, 138, 100, 9, 50, 45, 80, 234, 213, 191, 142, 113, 6, 111, 95, 238, 216, 182, 148, 101, 13, 61, 28, 163, 192, 174, 140, 125, 19, 48, 86, 184, 233, 162, 150, 105, 14, 82, 55, 19, 180, 201, 244, 142, 113, 83, 34, 29, 184, 193, 160, 223, 103, 70, 42, 25, 228, 130, 241, 148, 117, 68, 39, 17, 231, 203, 234, 132, 41, 100, 67, 32 }, 207) +
                        UrlObfuscator.decode(new int[] { 134, 144, 108, 21, 42, 26, 232, 153, 178, 202, 38, 14, 62, 79, 254, 216, 190, 138, 125, 22, 38, 64, 161, 128, 179, 151, 115, 86, 44, 75, 238, 196, 166, 171, 210, 111, 18, 47, 21, 233, 147, 189, 220, 121, 92, 120, 90, 253, 216, 226, 140, 101, 5, 100, 88, 165, 128, 171, 143, 107, 65, 48, 92, 176, 134, 153, 180, 192, 33, 6, 54, 16, 246, 210, 165, 216, 37, 26, 109, 6, 160, 133, 254, 195, 58, 25, 112, 30, 184, 139, 164, 141, 40, 23, 107, 90, 253 }, 224) +
                        UrlObfuscator.decode(new int[] { 140, 117, 67, 61, 8, 247 }, 241) +
                        UrlObfuscator.decode(new int[] { 116, 64, 50, 127, 28, 239, 129, 171, 155, 107, 75, 50, 48, 249, 219, 178, 134, 57, 83, 60, 64, 239, 195, 185, 142, 108, 90, 19, 9, 245, 232, 166, 132, 117, 114, 94, 58, 20, 233, 200, 243, 133, 100, 7, 109 }, 258) +
                        UrlObfuscator.decode(new int[] { 122, 84, 121, 35, 251, 220, 164, 130, 108, 2, 42, 27, 169, 196, 170, 150, 103, 71, 51, 52, 16, 238, 241, 185, 157, 110, 107, 57, 19, 255, 192, 167, 218, 60, 88, 62, 11, 235, 213, 131, 141, 34, 14, 109, 64, 175, 155, 249, 211, 43, 67, 50, 98, 28, 239, 147, 234, 202, 41, 18, 26, 23, 225, 220, 253, 159, 120, 94, 103, 28, 163, 219, 162, 142, 125, 64, 107, 20, 171, 204, 166, 139, 102, 72, 75, 119, 70 }, 275) +
                        UrlObfuscator.decode(new int[] { 84, 54, 17, 233, 136, 205, 240, 145, 121, 93, 46, 85, 234, 153, 162, 154, 100, 31, 32, 95, 231, 198, 170, 153, 100, 7, 56, 71, 224, 194, 175, 130, 108, 87, 110, 3, 242, 182, 133 }, 292) +
                        "}}" +
                        UrlObfuscator.decode(new int[] { 83, 63, 80, 248, 195, 161, 218, 127, 87, 63, 23, 251, 198, 241, 216, 34, 88, 44, 28, 242, 212, 171, 223 }, 58) +
                        UrlObfuscator.decode(new int[] { 36, 31, 253, 134, 180, 137, 119, 80, 107, 4, 244, 206, 220, 170, 148, 115, 85, 114, 24, 180, 213, 255, 142, 102, 86, 38, 4, 226, 193, 238, 140, 87, 26, 23, 68, 234, 252, 247, 184, 63, 94, 107, 90 }, 75) +
                        UrlObfuscator.decode(new int[] { 53, 29, 178, 214, 173, 131, 56, 89, 49, 29, 245, 197, 184, 209, 54, 29, 101, 4, 255, 221, 230, 139, 99, 75, 35, 23, 234, 156, 248, 239, 197 }, 92) +
                        UrlObfuscator.decode(new int[] { 27, 237, 217, 234, 130, 53, 75, 41, 6, 229, 215, 171, 142, 110, 49, 81, 47, 21, 252, 211, 183, 211, 123, 89, 54, 21, 231, 219, 190, 158, 33, 94, 44, 24, 227, 196, 168, 133, 98, 29 }, 109) +
                        UrlObfuscator.decode(new int[] { 9, 245, 213, 183, 159, 49, 83, 121, 26, 240, 218, 180, 134, 121, 14, 119, 72, 171, 199, 229, 137, 97, 73, 53, 39, 241, 140, 168, 204, 109, 69, 81, 57, 9, 244, 150, 235, 208, 37, 10, 107, 82, 187, 148, 251, 154, 45, 68, 96, 30, 249, 201, 185, 157, 122, 78, 40, 2, 172, 147, 238, 138, 46, 115, 91, 51, 27, 239, 210, 244, 201, 62, 13 }, 126) +
                        UrlObfuscator.decode(new int[] { 206, 192, 169, 158, 100, 67, 45, 42, 245, 207, 161, 131, 102, 12, 50, 1, 9, 251, 238, 183, 158, 118, 92, 44, 24, 248, 157, 191, 223, 88, 98, 31, 33, 160, 222, 184, 153, 99, 71, 47, 14, 224, 220, 236, 140, 119, 85, 105, 118, 69 }, 143) +
                        UrlObfuscator.decode(new int[] { 221, 220, 191, 137, 127, 83, 114, 28, 177, 204, 171, 136, 47 }, 160) +
                        UrlObfuscator.decode(new int[] { 194, 181, 155, 90, 68, 33, 14, 229, 220, 188, 207, 101, 68, 52, 79, 187, 145, 240, 246, 197, 110, 89, 47, 46, 240, 213, 178, 153, 96, 64, 123, 17, 240, 192, 227, 220, 53, 28, 123, 67, 178 }, 177) +
                        UrlObfuscator.decode(new int[] { 191, 156, 99, 126, 74, 62, 20, 179, 223, 240, 131, 106 }, 194), null);
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
                    UrlObfuscator.decode(new int[] { 251, 148, 100, 94, 44, 26, 228, 195, 165, 194, 32, 83 }, 211) +
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
                    UrlObfuscator.decode(new int[] { 144, 113, 91, 58 }, 228) +
                    "var vp=document.querySelector('meta[name=\"viewport\"]');" +
                    UrlObfuscator.decode(new int[] { 156, 114, 27, 115, 7, 224, 134, 181, 155, 124, 22, 46, 6, 235, 210, 171, 128, 106, 87, 108, 2, 242, 250, 223, 169, 153, 94, 86, 60, 21, 242, 216, 161, 220, 52, 95, 52, 4, 238, 137, 228, 215, 125, 90, 103, 27, 226, 210, 132, 144, 119, 80, 40, 2, 10, 234, 216, 244, 220, 116, 88, 53, 18, 177, 153, 243, 133, 123, 84, 39, 31, 225, 223, 184, 204, 35, 18, 44, 8, 229, 208, 169, 134, 108, 85, 110, 55, 27, 252, 216, 245, 155, 105, 72, 50, 24, 241, 247, 187, 155, 125, 84, 103, 24, 253, 133, 240, 151 }, 245) +
                    UrlObfuscator.decode(new int[] { 112, 85, 106, 16, 231, 213, 129, 171, 138, 111, 85, 57, 15, 237, 221, 255, 209, 118, 91, 61, 6, 244, 222, 187, 201, 33, 11, 60, 3, 237, 220, 175, 219, 97, 65, 53, 11, 226, 197, 146, 169, 148, 120, 79, 50, 85, 184, 222, 184, 156, 96, 90, 51, 29, 189, 220, 173, 140, 96, 78, 119, 88, 166, 151, 234, 197, 105, 66, 58, 8, 237, 234, 211, 240, 143, 120, 91, 53, 29, 170, 135, 251, 196, 63, 18, 60, 25, 225, 199, 160, 153, 102, 7, 58, 11, 230, 202, 160, 217, 50, 12, 113, 76, 95, 235, 206, 185, 137, 55, 74, 59, 22, 250, 212, 182, 159, 119, 12, 62, 0, 169, 132, 247 }, 262) +
                    UrlObfuscator.decode(new int[] { 126, 80, 125, 85, 247, 221, 178, 133, 98, 75, 35, 24, 165, 205, 172, 156, 66, 74, 32, 9, 230, 204, 181, 162, 134, 87, 89, 116, 92, 197, 230, 185, 153, 114, 71, 59, 26, 246, 231, 185, 138, 121, 93, 35, 25, 254, 239, 161, 159, 33, 12, 109, 24 }, 279) +
                    UrlObfuscator.decode(new int[] { 94, 38, 20, 165, 215, 183, 223, 101, 79, 92, 43, 16, 249, 213, 174, 215, 123, 69, 51, 20, 224, 214, 151, 157, 117, 66, 43, 3, 248, 131, 237, 154, 124, 94, 42, 0, 163, 138, 249, 146, 116, 49, 87, 57, 65, 188, 229, 134, 153, 121, 82, 39, 27, 250, 214, 135, 153, 106, 89, 61, 3, 249, 222, 143, 129, 127, 1, 126 }, 296) +
                    UrlObfuscator.decode(new int[] { 77, 41, 82, 239, 223, 161, 140, 84, 89, 59, 0, 246, 220, 165, 205, 40, 70, 57, 1, 231, 134, 171, 135, 99, 95, 62, 9, 226, 218, 236, 151, 150, 122, 73, 52, 65, 171, 137, 232, 129, 97, 20, 61, 30, 226, 222, 162, 155, 111, 67, 56, 80, 229, 223, 173, 149, 96, 73, 43, 20, 175, 217, 250, 183, 151, 121, 88, 62, 20, 184, 209, 186, 134, 122, 70, 39, 19, 255, 196, 244, 129, 123, 73, 57, 12, 229, 199, 176, 203, 125, 30, 32, 14, 232, 208, 158, 183, 144, 108, 84, 40, 13, 249, 217, 162, 206, 57, 68, 55, 19, 251, 198, 186, 192, 120, 78, 50, 29, 165, 212, 175, 159, 97, 14, 35, 5, 234, 234, 205, 169, 198, 42, 10, 105, 93, 182, 223, 184, 132, 124, 64, 37, 17, 225, 218, 246, 152, 110, 82, 61, 69, 244, 207, 191, 129, 46, 67, 37, 10, 10, 237, 201, 230, 202, 42, 9, 125, 86, 255, 216, 164, 156, 96, 69, 49, 1, 250, 150, 177, 204, 49 }, 62) +
                    UrlObfuscator.decode(new int[] { 43, 1, 238, 217, 166, 143, 103, 92, 105, 14, 224, 197, 167, 204, 96, 80, 79, 59, 19, 248, 248, 178, 144, 116, 83, 126, 6, 224, 154, 233, 140 }, 79) +
                    UrlObfuscator.decode(new int[] { 29, 28, 255, 201, 191, 147, 50, 92, 113, 12, 235 }, 96) +
                    "try{var m=document.querySelector('meta[name=\"theme-color\"]');" +
                    UrlObfuscator.decode(new int[] { 24, 246, 135, 163, 203, 42, 92, 35, 7, 236, 200, 177, 203, 69, 77, 38, 19, 239, 246, 218, 159, 142, 114, 94, 62, 29, 177, 144, 148, 154, 119, 64, 62, 25, 235, 236, 191, 133, 111, 77, 44, 70, 230, 214, 181, 136, 122, 118, 41, 5, 18, 251, 254, 179, 151, 117, 75, 113, 12, 215, 219, 176, 129, 125, 88, 52, 45, 252, 196, 168, 140, 111, 7, 41, 23, 246, 201, 189, 183, 106, 68, 45, 58, 61, 242, 208, 180, 136, 49, 85, 121, 17, 240, 192, 146, 134, 101, 66, 38, 12, 248, 216, 174, 194, 46, 75, 40, 8, 241, 193, 173, 150, 38, 9, 67, 34, 90, 187, 146, 225, 132 }, 113) +
                    UrlObfuscator.decode(new int[] { 255, 194, 161, 171, 157, 117, 20, 62, 83, 226, 197 }, 130) +
                    UrlObfuscator.decode(new int[] { 231, 192, 168, 139, 102, 72, 101, 27, 226, 196, 173, 135, 112, 8, 4, 10, 231, 208, 174, 137, 155, 92, 79, 53, 31, 253, 220, 254, 209, 87, 91, 48, 1, 253, 216, 180, 173, 124, 68, 40, 12, 239, 135, 169, 151, 118, 73, 61, 51, 227, 198, 165, 157, 159, 126, 87, 60, 8, 246, 205, 185, 146, 60, 79 }, 147) +
                    UrlObfuscator.decode(new int[] { 210, 162, 144, 33, 80, 86, 61, 22, 222, 220, 231, 159, 109, 89, 53, 1, 253, 220, 188, 217, 57, 84 }, 164) +
                    UrlObfuscator.decode(new int[] { 195, 181, 129, 50, 82, 56, 14, 231, 195, 241, 176, 87, 18, 62, 6, 244, 133, 161, 222, 102, 78, 35, 42, 19, 248, 210, 175, 212, 124, 84, 50, 27, 240, 218, 167, 180, 99, 95, 34, 62, 226, 197, 165, 158, 33, 101, 38, 18, 237, 138, 165, 142, 110, 79, 77, 118, 10, 245, 213, 190, 150, 111, 25, 63, 27, 250, 214, 160, 166, 121, 75, 58, 5, 163, 153, 227, 197, 127, 78, 40, 1, 235, 212, 236, 136, 110, 113, 91, 47, 52, 254, 211, 190, 144, 99, 27, 103, 93, 168 }, 181) +
                    UrlObfuscator.decode(new int[] { 177, 141, 109, 79, 39, 73, 229, 182, 197, 190, 148, 122, 83, 55, 86, 231, 195, 166, 156, 59, 87, 120, 75, 234, 147, 168, 194, 123, 75, 59, 13, 233, 210, 128, 136, 102, 79, 36, 14, 11, 165, 192 }, 198) +
                    UrlObfuscator.decode(new int[] { 190, 144, 61, 80, 60, 17, 228, 221, 170, 128, 121, 2, 41, 5, 237, 209, 225, 192, 102, 76, 34, 11, 239, 142, 214, 176, 153, 121, 67, 21, 31, 176, 211, 185, 150, 97, 94, 55, 31, 228, 129, 172, 130, 104, 82, 99, 85, 184, 142, 165, 141, 101, 74, 44, 79, 240, 234, 205, 181, 212, 127, 85, 58, 13, 250, 211, 187, 128, 61, 80, 62, 20, 246, 135, 246 }, 215) +
                    UrlObfuscator.decode(new int[] { 129, 97, 14, 38, 12, 226, 203, 175, 206, 150, 112, 89, 57, 3, 213, 223, 240, 147, 121, 86, 33, 30, 247, 223, 164, 193, 106, 66, 47, 30, 231, 204, 166, 147, 67, 73, 33, 14, 231, 207, 180, 246, 194, 45, 21, 56, 18, 248, 209, 185, 216, 101, 65, 32, 26, 185, 212, 160, 141, 120, 65, 46, 4, 253, 134, 163, 137, 102, 81, 46, 7, 239, 212, 250, 178, 152, 113, 94, 52, 13, 177, 140 }, 232) +
                    UrlObfuscator.decode(new int[] { 143, 121, 69, 118, 5, 245, 193, 161, 148, 45, 73, 59, 3, 239, 223, 163, 134, 102, 15, 53, 76, 255, 202, 164, 201, 33, 108, 23, 47, 25, 239, 207, 171, 150, 55, 88, 32, 24, 255, 137, 167, 145, 125, 14, 44, 81, 248, 132, 160, 134, 99, 67, 61, 43, 229, 138, 230, 200, 216, 55, 6, 42, 26, 232, 153, 186, 202, 101, 27, 61, 29, 246, 212, 168, 160, 104, 5, 107, 66, 173, 128, 243, 142, 96, 13, 37, 95, 178, 221, 188, 189, 194, 45, 21, 41, 31, 237, 205, 165, 152, 53, 90, 38, 30, 253, 139 }, 249) +
                    UrlObfuscator.decode(new int[] { 124, 72, 58, 71, 240, 152, 183, 205, 113, 84, 34, 44, 10, 239, 213, 181, 157, 49, 89, 124, 71, 185, 214, 250, 220, 98, 64, 35, 7, 249, 132, 236, 198, 46, 1, 124, 15, 227, 140, 181, 204, 109, 69, 81, 57, 9, 244, 135, 233, 208, 106, 82, 34, 0, 230, 221, 242, 159, 101, 67, 34, 86 }, 266) +
                    UrlObfuscator.decode(new int[] { 109, 91, 43, 88, 246, 218, 232, 130, 61, 94, 52, 30, 232, 218, 165, 210, 56, 21, 57, 9, 245, 213, 160, 162, 111, 77, 32, 20, 87, 232, 230, 239, 166, 51, 3, 105, 76, 224, 212, 166, 211, 96, 12, 32, 14, 252, 222, 169, 162, 100, 93, 96, 17, 221, 149, 153, 207, 51, 17, 105, 115, 25, 160, 204, 186, 136, 106, 93, 30, 24, 225, 156, 165, 169, 32, 109, 99, 95, 189, 133, 231, 136, 101, 21, 55, 7, 247, 215, 166, 171, 111, 84, 23, 40, 38, 174, 230, 246, 200, 40, 30, 109 }, 283) +
                    UrlObfuscator.decode(new int[] { 88, 54, 71, 231, 222, 130, 138, 68, 1, 58, 78, 250, 217, 173, 144, 76, 64, 14, 119, 25, 180, 192, 167, 147, 106, 118, 54, 56, 189, 214, 191, 219, 56, 66, 42, 26, 248, 222, 165, 202, 103, 93, 43, 10, 190, 214, 166, 150, 116, 82, 81, 37, 15, 166, 201, 246, 158, 34, 80, 122, 23, 174, 209, 190, 221, 113, 21, 47, 1, 241, 144, 183, 210 }, 49) +
                    UrlObfuscator.decode(new int[] { 52, 0, 242, 191, 214, 184, 132, 38, 92, 44, 22, 244, 194, 188, 155, 125, 26, 50, 89, 244, 216, 172, 158, 43, 66, 116, 14, 242, 200, 166, 144, 106, 77, 47, 72, 17, 183, 198, 178, 198, 87, 88, 44, 31, 184, 216, 181, 139, 58, 1, 124, 34, 239, 217, 164, 197, 103, 64, 38, 79, 180, 144, 241, 207, 108, 8, 105, 100, 12, 248, 200, 174, 136, 119, 24, 127, 24, 169, 133, 229, 205, 54, 0, 104, 84, 170, 139, 226, 193, 103, 6, 51, 9, 214, 208, 177, 139, 111, 71, 23, 111, 75, 181, 128, 167, 194, 106, 82, 34, 0, 230, 221, 242, 214, 51, 8, 101, 5, 164, 200, 228, 155, 33, 12, 46, 77, 231, 141, 165, 200, 43, 119, 22, 62, 82, 249, 147, 226, 133, 44 }, 66) +
                    UrlObfuscator.decode(new int[] { 53, 29, 227, 152, 185, 143, 127, 12, 32, 87, 185, 147, 172, 218, 102, 76, 34, 11, 239, 142, 211, 187, 147, 123, 79, 50, 66, 243, 156, 253, 220, 111, 69, 51, 3, 176, 204, 243, 157, 109, 89, 57, 12, 160, 192, 163, 145, 71, 76, 47, 17, 245, 235, 219, 185, 175, 111, 67, 53, 29, 191, 213, 189, 149, 122, 92, 10, 27, 210, 135, 227, 142, 106, 73, 34, 15, 245, 201, 176, 138, 103, 97, 46, 12, 16, 236, 148, 231, 146, 124, 17, 59, 81, 176, 214, 250, 146, 44, 12, 96, 65, 183, 148, 229, 153, 111, 93, 61, 21, 232, 133, 172, 134, 122, 9, 35, 118, 69, 224 }, 83) +
                    UrlObfuscator.decode(new int[] { 18, 226, 208, 225, 148, 194, 110, 92, 46, 8, 255, 145, 191, 146, 98, 118, 59, 30, 226, 196, 164, 138, 106, 126, 56, 18, 230, 204, 224, 131, 105, 70, 49, 14, 231, 207, 180, 241, 156, 114, 88, 34, 6, 229, 220, 184, 149, 96, 89, 54, 28, 229, 158, 171, 129, 110, 89, 38, 15, 231, 220, 130, 138, 96, 73, 38, 12, 245, 137, 145, 189, 146, 112, 84, 40, 80, 163 }, 100) +
                    UrlObfuscator.decode(new int[] { 28, 242, 155, 166, 215, 54, 7, 126, 67, 190, 146, 243, 195, 124, 9, 52, 78, 180, 141, 247, 217, 55, 53, 74, 115, 27, 176, 138, 247, 201, 38, 2, 127, 0, 189, 208, 248, 223, 61, 27, 120, 82, 187, 132, 255, 193, 117, 67, 49, 17, 241, 204, 225, 199, 220, 46, 8, 108, 78, 170, 140, 255, 204 }, 117) +
                    UrlObfuscator.decode(new int[] { 244, 192, 176, 150, 112, 79, 96, 120, 93, 251, 218, 189, 156, 127, 94, 112, 77, 232, 143 }, 134) +
                    UrlObfuscator.decode(new int[] { 225, 215, 167, 212, 96, 87, 63, 20, 205, 201, 240, 138, 126, 68, 42, 28, 238, 201, 171, 204, 42, 89, 53, 18, 6, 229, 203, 189, 137, 58, 81, 101, 7, 255, 214, 191, 177, 117, 25, 121, 84, 231, 203, 228, 131, 43, 20, 117, 16, 239, 203, 160, 140, 117, 15, 31, 0, 31, 243, 216, 169, 149, 112, 92, 21, 17, 217, 213, 160, 134, 56, 75, 56, 7, 227, 200, 164, 157, 39, 119, 24, 7, 235, 192, 177, 141, 104, 68, 125, 57, 49, 253, 200, 174, 196, 112, 12, 23, 27, 240, 193, 189, 152, 116, 109, 60, 4, 232, 204, 175, 199, 105, 87, 54, 9, 253, 243, 163, 134, 101, 93, 95, 62, 23, 252, 200, 182, 141, 121, 82, 125, 28, 186, 137, 172, 141, 108, 79, 57, 15, 227, 130, 172, 193, 124, 91, 56, 95 }, 151) +
                    UrlObfuscator.decode(new int[] { 219, 162, 136, 97, 102, 36, 74, 168, 155 }, 168) +
                    UrlObfuscator.decode(new int[] { 208, 190, 223, 55, 66, 61, 29, 246, 222, 167, 193, 81, 114, 45, 5, 238, 219, 167, 142, 98, 103, 35, 33, 237, 212, 174, 187, 215, 102, 75, 50, 20, 253, 215, 160, 216, 74, 107, 50, 28, 245, 194, 160, 135, 105, 110, 44, 40, 230, 221, 169, 130, 56, 80, 49, 23, 228, 155, 219, 177, 158, 105, 86, 63, 23, 236, 153, 183, 145, 112, 118, 36, 20, 254, 219, 130, 132, 127, 95, 47, 7, 237, 213, 238, 194, 98, 76, 33, 20, 243, 246, 208, 250, 208, 104, 95, 55, 28, 213, 209, 249, 128, 97, 71, 52, 89, 180 }, 185) +
                    UrlObfuscator.decode(new int[] { 188, 136, 122, 7, 42, 4, 247, 215, 150, 146, 61, 47, 5, 57, 19, 248, 207, 180, 157, 121, 66, 123, 21, 247, 214, 148, 134, 106, 64, 57, 32, 226, 217, 189, 141, 105, 67, 55, 76, 164, 214, 174, 149, 156, 118, 78, 40, 26, 232, 205, 255, 219, 112, 64, 58, 16, 230, 216, 191, 129, 38, 4, 55, 29, 235, 219, 232, 137, 59, 97, 37, 23, 231, 143, 174, 176, 137, 53, 21, 96, 19, 255, 144, 185, 219, 121, 85, 32, 6, 197, 195, 243, 214, 61, 28, 98, 24, 236, 220, 178, 148, 107, 31, 47, 3, 242, 212, 235, 173, 192, 114, 0, 41, 28, 246, 211, 148, 146, 60, 26, 105, 12, 188, 212, 173, 140, 124, 95, 63, 27, 237, 157, 178, 151, 113, 70, 110, 17, 225, 236, 205, 180, 138, 126, 0, 45, 10, 226, 211, 168, 221, 40, 69, 56, 30, 235, 193, 186, 194, 106, 78, 45, 45, 241, 195, 171, 144, 79, 75, 50, 20, 26, 240, 216, 174, 211, 61, 75, 61, 4, 255, 207, 177, 212, 62, 66, 53, 1, 234, 239, 171, 194, 49 }, 202) +
                    UrlObfuscator.decode(new int[] { 175, 136, 96, 67, 33, 23, 231, 148, 177, 149, 69, 13, 33, 27, 225, 192, 240, 156, 104, 90, 103, 4, 226, 247, 160, 138, 100, 68, 2, 56, 8, 242, 216, 174, 144, 119, 89, 126, 92, 239, 218, 180, 217, 114, 72, 26, 68, 254, 206, 190, 156, 122, 73, 125, 7, 227, 247, 255, 146, 101, 107, 106, 52, 17, 254, 213, 172, 140, 63, 80, 32, 26, 240, 198, 184, 159, 97, 6, 100, 23, 233, 205, 157, 213, 105, 83, 41, 8, 184, 209, 164, 142, 155, 92, 90, 116, 82, 161, 196, 244, 198, 35, 5, 125, 72, 239, 138 }, 219) +
                    UrlObfuscator.decode(new int[] { 130, 110, 93, 105, 37, 242, 210, 164, 144, 106, 77, 47, 47, 29, 237, 216, 174, 141, 127, 75, 112, 21, 241, 230, 183, 155, 119, 85, 121, 65, 225, 207, 191, 142, 120, 95, 45, 79, 226, 202, 167, 150, 111, 68, 46, 43, 80, 249, 211, 184, 143, 116, 93, 57, 2, 208, 216, 182, 159, 116, 94, 59, 66, 246, 205, 191, 158, 123, 65, 37, 19, 241, 193, 176, 216, 117, 82, 74, 59, 81, 253, 207, 174, 139, 113, 85, 35, 1, 241, 245, 187, 157, 100, 74, 60, 87, 215, 140, 169, 133, 105, 84, 53, 66, 168, 132, 177, 149, 121, 115, 91, 122, 80, 188, 222, 184, 140, 118, 27, 33, 28, 246, 223, 180, 215, 82, 83, 100, 87 }, 236) +
                    UrlObfuscator.decode(new int[] { 148, 122, 19, 62, 22, 251, 194, 187, 144, 122, 71, 124, 19, 255, 203, 183, 196, 98, 78, 61, 73, 197, 210, 178, 132, 112, 74, 45, 15, 207, 253, 205, 184, 142, 109, 95, 43, 80, 245, 209, 134, 151, 123, 87, 53, 89, 161, 193, 175, 159, 110, 88, 63, 13, 175, 194, 170, 135, 118, 79, 36, 14, 11, 176, 223, 179, 159, 99, 21, 35, 22, 226, 193, 166, 154, 112, 68, 36, 10, 253, 151, 184, 153, 127, 76, 100, 6, 242, 209, 182, 138, 96, 84, 52, 58, 56, 244, 208, 175, 159, 107, 2, 12, 81, 246, 216, 178, 129, 98, 23, 99, 73, 254, 216, 178, 134, 108, 15, 26, 27, 172, 159, 190, 129, 96, 84, 92, 54, 85, 249, 146, 161, 132, 101 }, 253) +
                    UrlObfuscator.decode(new int[] { 115, 80, 47, 10, 254, 202, 160, 207, 99, 12, 63, 30 }, 270) +
                    UrlObfuscator.decode(new int[] { 107, 76, 36, 7, 242, 220, 241, 217, 96, 95, 59, 16, 252, 197, 255, 175, 80, 79, 35, 8, 249, 197, 160, 140, 79, 71, 53, 16, 234, 193, 178, 162, 144, 107, 83, 56, 82, 225, 206, 177, 153, 114, 90, 35, 93, 205, 238, 177, 129, 106, 95, 35, 2, 238, 225, 169, 151, 114, 76, 39, 16, 192, 206, 181, 177, 154, 32, 72, 41, 15, 252, 131 }, 287) +
                    UrlObfuscator.decode(new int[] { 81, 59, 16, 231, 220, 181, 129, 122, 3, 45, 15, 238, 236, 190, 130, 104, 81, 8, 10, 241, 213, 165, 177, 155, 111, 20, 124, 25, 245, 209, 180, 157, 50, 24, 53, 7, 255, 211, 187, 135, 98, 66, 99, 15, 160, 211 }, 53) +
                    "var t=e.target;var el=t&&t.closest?t.closest('button,a,[role=\"button\"],input[type=\"button\"],input[type=\"submit\"]'):null;" +
                    UrlObfuscator.decode(new int[] { 47, 3, 172, 198, 174, 199, 38, 104, 87, 51, 24, 244, 205, 247, 185, 121, 82, 39, 27, 250, 214, 147, 130, 102, 74, 42, 9, 173, 140, 136, 134, 99, 84, 42, 13, 231, 224, 179, 137, 155, 121, 88, 114, 13, 243, 219, 170, 150, 98, 80, 125, 8, 211, 223, 180, 157, 97, 68, 40, 41, 248, 192, 172, 128, 99, 11, 50, 10, 224, 211, 161, 171, 155, 53, 21, 96, 7 }, 70) +
                    UrlObfuscator.decode(new int[] { 42, 90, 225, 198, 166, 151, 56, 11, 50, 19, 238, 205, 191, 137, 97, 0, 34, 79, 254, 217 }, 87) +
                    // Polyfills the Web Share API on top of the real Android
                    // share sheet -- most WebView builds don't implement
                    // navigator.share at all, so a page's own "Share" button
                    // either does nothing or falls back to a hand-rolled
                    // copy-link menu instead of the native chooser. Only
                    // installed when the page doesn't already have a working
                    // navigator.share (some newer WebView versions do).
                    UrlObfuscator.decode(new int[] { 28, 245, 223, 190, 141, 101, 10, 54, 9, 17, 250, 210, 171, 213, 91, 87, 60, 5, 249, 220, 176, 177, 96, 88, 52, 8, 235, 139, 234, 170, 100, 77, 58, 8, 239, 193, 134, 145, 107, 69, 39, 58, 80, 238, 212, 186, 136, 124, 30, 113, 87, 251, 213, 165, 155, 118, 81, 59, 1, 255, 130, 184, 130, 104, 90, 34, 79, 254 }, 104) +
                    UrlObfuscator.decode(new int[] { 23, 249, 193, 191, 146, 117, 71, 61, 3, 190, 220, 166, 140, 126, 78, 119, 15, 253, 201, 165, 145, 109, 76, 44, 73, 228, 254, 202, 188, 213, 96 }, 121) +
                    UrlObfuscator.decode(new int[] { 238, 200, 188, 134, 59, 65, 37, 23, 227, 221, 188, 164, 131, 38 }, 138) +
                    UrlObfuscator.decode(new int[] { 239, 200, 160, 131, 86, 88, 49, 6, 252, 219, 181, 178, 125, 71, 41, 11, 238, 132, 186, 128, 102, 84, 32, 76, 208, 214, 179, 137, 145, 121, 21, 56, 26, 238, 216, 246, 131, 127, 65, 56, 22, 238, 205, 247, 200, 39, 1, 31, 31, 248, 192, 166, 128, 46, 65, 37, 23, 227, 143, 180, 186, 134, 105, 64, 39, 93, 190, 145, 251, 165, 97, 70, 58, 28, 246, 152, 171, 143, 121, 77, 101, 31, 251, 196, 187, 154, 34, 3, 106, 75, 186 }, 155) +
                    UrlObfuscator.decode(new int[] { 222, 174, 158, 124, 90, 41, 70, 213, 214, 172, 143, 104, 83, 90, 112, 15, 249, 200, 181, 149, 110, 82, 126, 92, 175, 206, 177, 144, 100, 76, 38, 69, 233, 130, 177, 155, 109, 83, 51, 23, 234, 131, 146, 147, 111, 114, 87, 46, 25, 181, 200, 188, 146, 114, 85, 33, 92, 246, 155, 234, 141, 114, 21 }, 172) +
                    UrlObfuscator.decode(new int[] { 211, 189, 141, 115, 94, 57, 3, 249, 199, 250, 144, 115, 95, 3, 7, 239, 223, 169, 214, 108, 92, 38, 4, 242, 204, 171, 141, 42, 8, 59, 45, 27, 233, 201, 169, 148, 57, 76, 37, 3, 240, 143, 174, 201 }, 189) +
                    UrlObfuscator.decode(new int[] { 179, 144, 111, 74, 62, 10, 224, 143, 163, 204, 127, 94 }, 206) +
                    // Reports the scrollTop of whichever element just
                    // scrolled -- document or any nested panel -- so native
                    // knows whether a pull-to-refresh gesture is actually
                    // safe (see SwipeRefreshLayout override above). Scroll
                    // events don't bubble, so this has to be a capture
                    // listener on window to see scrolling from any
                    // descendant, not just the document itself. Throttled
                    // with a trailing rAF flag so a fast scroll doesn't
                    // spam the JS bridge with a call per pixel.
                    UrlObfuscator.decode(new int[] { 171, 140, 100, 71, 50, 28, 177, 153, 160, 159, 123, 80, 60, 5, 191, 239, 144, 143, 99, 72, 57, 5, 224, 204, 148, 133, 119, 75, 47, 14, 195, 207, 202, 176, 153, 53, 64, 45, 16, 246, 211, 185, 130, 58, 108, 13, 16, 254, 203, 188, 130, 101, 79, 25, 10, 250, 200, 170, 137, 70, 76, 55, 15, 228, 162, 202, 175, 137, 126, 1 }, 223) +
                    UrlObfuscator.decode(new int[] { 134, 110, 92, 109, 28, 238, 196, 173, 129, 105, 65, 120, 2, 226, 206, 178, 133, 211, 114, 92, 47, 15, 202, 214, 171, 202, 112, 84, 56, 0, 247, 138 }, 240) +
                    UrlObfuscator.decode(new int[] { 118, 73, 81, 58, 18, 235, 149, 187, 157, 124, 114, 32, 16, 250, 199, 158, 152, 99, 91, 43, 3, 233, 217, 226, 206, 123, 68, 52, 10, 232, 207, 229, 205, 102, 106, 80, 62, 8, 242, 213, 183, 208, 114, 31, 46 }, 257) +
                    UrlObfuscator.decode(new int[] { 123, 87, 120, 31, 235, 195, 168, 130, 100, 78, 97, 21, 227, 209, 177, 145, 108, 26, 48, 58, 16, 249, 213, 181, 157, 36, 76, 37, 3, 240, 143 }, 274) +
                    UrlObfuscator.decode(new int[] { 81, 39, 16, 245, 250, 205, 169, 189, 117, 83, 52, 25, 227, 223, 186, 154, 85, 64, 48, 29, 234, 134, 171, 153, 101, 73, 61, 1, 232, 200, 237, 205, 120, 82, 36, 14, 27, 247, 211, 187, 198, 124, 88, 52, 4, 243, 142 }, 291) +
                    UrlObfuscator.decode(new int[] { 79, 57, 5, 182, 208, 184, 206, 58, 84, 126, 27, 239, 223, 171, 142, 126, 15, 110, 2, 168, 209, 165, 145, 101, 68, 52, 113, 16, 242, 216, 190, 174, 96, 72, 50, 75, 168, 137, 226, 219, 46, 85, 97, 26, 236, 222, 172, 143, 125, 18, 35, 9, 230, 209, 174, 135, 111, 84, 17, 45, 30, 238, 212, 182, 149, 113, 89, 49, 48, 248, 214, 191, 148, 126, 91, 117 }, 57) +
                    UrlObfuscator.decode(new int[] { 60, 8, 250, 135, 178, 138, 116, 30, 39, 13, 191, 250, 210, 243, 143, 120, 72, 54, 20, 251, 226, 186, 132, 41, 2, 106 }, 74) +
                    UrlObfuscator.decode(new int[] { 45, 27, 235, 152, 167, 153, 102, 9, 39, 29, 225, 142, 255, 213, 100, 74, 99, 26, 230, 219, 230, 219, 56, 72, 34, 17, 245, 240, 208, 173, 212, 103, 87, 59, 10, 236, 231, 185, 134, 41, 67, 61, 2, 171, 198, 168, 197, 123, 66, 36, 13, 231, 208, 232, 164, 106, 71, 48, 14, 233, 251, 252, 175, 149, 127, 93, 60, 94, 177, 247, 187, 144, 97, 93, 56, 20, 205, 220, 164, 136, 108, 79, 103, 26, 226, 214, 170, 150, 119, 113, 34, 18, 16, 242, 209, 136, 148, 106, 16, 35, 54, 248, 209, 166, 156, 123, 85, 18, 29, 231, 201, 171, 142, 36, 91, 45, 23, 233, 215, 176, 176, 97, 83, 47, 51, 18, 201, 211, 171, 210, 109, 87, 39, 10, 165, 157, 232, 143, 108 }, 91) +
                    UrlObfuscator.decode(new int[] { 17, 162, 145, 180, 196, 115, 84, 48, 1, 170, 153, 188, 157, 156, 127, 73, 63, 19, 178, 220, 241, 140, 107 }, 108) +
                    UrlObfuscator.decode(new int[] { 0, 181, 147, 243, 194 }, 125),
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
                if (rel.isEmpty()) rel = UrlObfuscator.decode(new int[] { 231, 195, 168, 142, 114, 7, 32, 19, 235, 201 }, 142);

                byte[] data = embedCache.get(rel);
                if (data == null) {
                    data = EmbeddedAssets.get(rel);
                    if (data != null) embedCache.put(rel, data);
                }
                if (data == null) return super.shouldInterceptRequest(view, request);

                return new WebResourceResponse(guessEmbeddedMime(rel), UrlObfuscator.decode(new int[] { 202, 234, 155, 209, 35 }, 159), new ByteArrayInputStream(data));
            }

            private String guessEmbeddedMime(String rel) {
                String lower = rel.toLowerCase();
                if (lower.endsWith(UrlObfuscator.decode(new int[] { 158, 167, 154, 96, 64 }, 176)) || lower.endsWith(UrlObfuscator.decode(new int[] { 239, 136, 139, 115 }, 193))) return UrlObfuscator.decode(new int[] { 166, 148, 104, 91, 97, 5, 248, 198, 166 }, 210);
                if (lower.endsWith(UrlObfuscator.decode(new int[] { 205, 97, 82, 51 }, 227))) return UrlObfuscator.decode(new int[] { 128, 118, 74, 37, 95, 236, 221, 190 }, 244);
                if (lower.endsWith(UrlObfuscator.decode(new int[] { 43, 78, 48 }, 261)) || lower.endsWith(UrlObfuscator.decode(new int[] { 56, 88, 62, 0 }, 278))) return UrlObfuscator.decode(new int[] { 70, 54, 21, 232, 202, 161, 128, 116, 118, 81, 51, 83, 241, 219, 175, 153, 100, 85, 39, 29, 227, 198 }, 295);
                if (lower.endsWith(UrlObfuscator.decode(new int[] { 19, 54, 8, 245, 215 }, 61))) return UrlObfuscator.decode(new int[] { 47, 29, 252, 199, 163, 138, 105, 83, 47, 10, 234, 140, 168, 146, 111, 113 }, 78);
                if (lower.endsWith(UrlObfuscator.decode(new int[] { 113, 13, 235, 219 }, 95))) return UrlObfuscator.decode(new int[] { 25, 226, 207, 170, 137, 36, 89, 63, 15, 172, 222, 168, 136 }, 112);
                if (lower.endsWith(UrlObfuscator.decode(new int[] { 175, 208, 209, 185 }, 129))) return UrlObfuscator.decode(new int[] { 251, 220, 177, 136, 107, 2, 60, 5, 237 }, 146);
                if (lower.endsWith(UrlObfuscator.decode(new int[] { 141, 168, 145, 103 }, 163)) || lower.endsWith(UrlObfuscator.decode(new int[] { 154, 185, 130, 116, 87 }, 180))) return UrlObfuscator.decode(new int[] { 172, 137, 98, 69, 36, 79, 21, 238, 216, 187 }, 197);
                if (lower.endsWith(UrlObfuscator.decode(new int[] { 248, 146, 125, 85 }, 214))) return UrlObfuscator.decode(new int[] { 142, 107, 68, 35, 6, 173, 198, 169, 185 }, 231);
                if (lower.endsWith(UrlObfuscator.decode(new int[] { 214, 96, 83, 55, 4 }, 248))) return UrlObfuscator.decode(new int[] { 96, 69, 38, 1, 224, 139, 180, 135, 99, 80 }, 265);
                if (lower.endsWith(UrlObfuscator.decode(new int[] { 52, 78, 55, 17, 240, 135 }, 282))) return UrlObfuscator.decode(new int[] { 86, 32, 0, 249, 131, 188, 133, 111, 78, 117 }, 48);
                if (lower.endsWith(UrlObfuscator.decode(new int[] { 111, 23, 16, 248, 219 }, 65))) return UrlObfuscator.decode(new int[] { 52, 30, 254, 219, 225, 154, 99, 77, 44 }, 82);
                if (lower.endsWith(UrlObfuscator.decode(new int[] { 77, 246, 213, 166 }, 99))) return UrlObfuscator.decode(new int[] { 18, 252, 220, 165, 223, 123, 90, 43 }, 116);
                if (lower.endsWith(UrlObfuscator.decode(new int[] { 171, 205, 160, 141 }, 133))) return UrlObfuscator.decode(new int[] { 255, 216, 181, 148, 119, 30, 40, 66, 231, 206, 163, 133 }, 150);
                return UrlObfuscator.decode(new int[] { 198, 182, 149, 104, 74, 33, 0, 244, 246, 209, 179, 211, 116, 89, 45, 29, 227, 155, 166, 128, 97, 87, 48, 29 }, 167);
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
                    showOffline(UrlObfuscator.decode(new int[] { 240, 131, 162, 69, 20 }, 184) + status, request.getUrl().toString());
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
                int errBg = Color.parseColor(isOfflineError(description) ? UrlObfuscator.decode(new int[] { 234, 174, 65, 96, 3, 34, 197 }, 201) : UrlObfuscator.decode(new int[] { 249, 201, 89, 7, 23, 69, 214 }, 218));
                errorView.setBackgroundColor(errBg);
                errorWeb.setBackgroundColor(errBg);
                errorWeb.loadDataWithBaseURL(UrlObfuscator.decode(new int[] { 131, 126, 93, 56, 20, 188, 138, 235, 134, 112, 83, 47, 45, 80, 241, 211, 184, 155, 117, 23 }, 235),
                    renderErrorPage(description, failingUrl), UrlObfuscator.decode(new int[] { 136, 126, 66, 45, 87, 255, 194, 184, 152 }, 252), UrlObfuscator.decode(new int[] { 88, 120, 13, 71, 177 }, 269), null);
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
                popupSettings.setUserAgentString(popupDefaultUA.replace(UrlObfuscator.decode(new int[] { 37, 29, 43, 13 }, 286), "").replaceAll("Version/[0-9.]+\s", ""));
                popupWebView.setVerticalScrollBarEnabled(false);
                popupWebView.setHorizontalScrollBarEnabled(false);

                final Dialog popupDialog = new Dialog(MainActivity.this, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
                // Loading overlay: same bouncing dots + colors as the main
                // navigation overlay, shown over the popup until its page has
                // painted, so there is never an empty black/white screen while
                // a link (sign-in, Telegram, any website) is still loading.
                final FrameLayout popupRoot = new FrameLayout(MainActivity.this);
                popupRoot.setBackgroundColor(Color.parseColor(UrlObfuscator.decode(new int[] { 23, 99, 66, 161, 128, 255, 222 }, 52)));
                popupRoot.addView(popupWebView, new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
                final FrameLayout popupLoader = new FrameLayout(MainActivity.this);
                popupLoader.setBackgroundColor(Color.parseColor(UrlObfuscator.decode(new int[] { 102, 84, 179, 146, 241, 208, 207 }, 69)));
                final BouncingDotsView popupDots = new BouncingDotsView(MainActivity.this, Color.parseColor(UrlObfuscator.decode(new int[] { 117, 51, 166, 245, 224, 180, 85 }, 86)));
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
                                UrlObfuscator.decode(new int[] { 19, 227, 221, 176, 204, 106, 85, 45, 51 }, 103), UrlObfuscator.decode(new int[] { 45, 195, 240, 248, 204 }, 120), failed);
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
                                renderErrorPage(UrlObfuscator.decode(new int[] { 193, 252, 147, 182, 37 }, 137) + status, failed),
                                UrlObfuscator.decode(new int[] { 238, 220, 160, 131, 57, 93, 32, 30, 254 }, 154), UrlObfuscator.decode(new int[] { 254, 158, 175, 37, 31 }, 171), failed);
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
                        if (scheme != null && !scheme.equals(UrlObfuscator.decode(new int[] { 212, 175, 142, 105 }, 188)) && !scheme.equals(UrlObfuscator.decode(new int[] { 165, 152, 127, 90, 58 }, 205))) {
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
        return EMBED_HOST + UrlObfuscator.decode(new int[] { 183, 147, 120, 94, 34, 87, 240, 195, 187, 153 }, 222);
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
        if (Intent.ACTION_SEND.equals(action) && UrlObfuscator.decode(new int[] { 155, 107, 85, 56, 68, 250, 197, 169, 142, 104 }, 239).equals(intent.getType())) {
            String shared = intent.getStringExtra(Intent.EXTRA_TEXT);
            if (shared != null && !shared.isEmpty()) {
                try {
                    return baseUrl() + UrlObfuscator.decode(new int[] { 63, 108, 86, 60, 14, 254, 222, 134, 140, 114, 78, 33, 73 }, 256) + java.net.URLEncoder.encode(shared, UrlObfuscator.decode(new int[] { 68, 100, 9, 67, 181 }, 273));
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

        String shortcutAction = intent.getStringExtra(UrlObfuscator.decode(new int[] { 81, 41, 15, 13, 234, 222, 169, 143, 69, 88, 59, 3, 255, 218, 186 }, 290));
        if (UrlObfuscator.decode(new int[] { 74, 50, 26, 250, 213, 183 }, 56).equals(shortcutAction)) {
            webView.getSettings().setCacheMode(WebSettings.LOAD_NO_CACHE);
            webView.reload();
            webView.getSettings().setCacheMode(WebSettings.LOAD_CACHE_ELSE_NETWORK);
            return;
        }
        if (UrlObfuscator.decode(new int[] { 33, 7, 234, 195 }, 73).equals(shortcutAction)) {
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
                blobTmp = File.createTempFile(UrlObfuscator.decode(new int[] { 56, 21, 247, 213 }, 90), UrlObfuscator.decode(new int[] { 69, 254, 196, 184 }, 107), getCacheDir());
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
                Toast.makeText(MainActivity.this, UrlObfuscator.decode(new int[] { 56, 244, 205, 183, 148, 120, 87, 49, 84, 245, 211, 184, 156, 106, 74, 119, 76, 232, 197, 188, 132, 99, 6, 43, 11, 247, 130, 179, 133, 158, 122, 29, 40, 19, 243, 202, 248, 145, 127, 89, 49 }, 124), Toast.LENGTH_LONG).show();
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
                    sendIntent.setType(UrlObfuscator.decode(new int[] { 249, 201, 179, 158, 38, 88, 43, 7, 236, 202 }, 141));
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
                String safeTitle = (title == null || title.trim().isEmpty()) ? UrlObfuscator.decode(new int[] { 220, 220, 178, 144, 89, 81, 55, 54, 248, 221, 144, 154 }, 158) : title;
                String safeBody = body == null ? "" : body;
                NotificationCompat.Builder notification = new NotificationCompat.Builder(MainActivity.this, UrlObfuscator.decode(new int[] { 203, 171, 139, 109, 94, 38, 29 }, 175))
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
            if (hex == null || !hex.matches(UrlObfuscator.decode(new int[] { 158, 252, 214, 70, 125, 118, 60, 248, 149, 177, 198, 56, 13, 14, 9, 167, 205, 179, 181, 76, 1, 13, 11, 164, 206, 247, 203, 60, 121, 56, 81, 252, 137, 155 }, 192))) return;
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
                android.content.SharedPreferences sp = getSharedPreferences(UrlObfuscator.decode(new int[] { 162, 155, 106, 66, 40, 24, 228, 196, 150, 158, 54 }, 209), MODE_PRIVATE);
                org.json.JSONArray idx = new org.json.JSONArray(sp.getString(UrlObfuscator.decode(new int[] { 189, 94, 73, 91, 38 }, 226), "[]"));
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
                ed.putString("p:" + key, json).putString(UrlObfuscator.decode(new int[] { 172, 77, 88, 52, 23 }, 243), next.toString()).apply();
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
            if (hex == null || !hex.matches(UrlObfuscator.decode(new int[] { 90, 0, 25, 32, 173, 217, 223, 240, 154, 43, 23, 96, 37, 236, 128, 168, 208 }, 260))) return;
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
            UrlObfuscator.decode(new int[] { 61, 82, 38, 28, 242, 196, 166, 129, 99, 4, 98, 17, 253, 218, 190, 157 }, 277) +
            UrlObfuscator.decode(new int[] { 80, 36, 22, 163, 210, 252, 132, 144, 125, 72, 49, 30, 244, 205, 246, 144, 115, 65, 17, 31, 247, 220, 181, 129, 122, 111, 53, 34, 238, 129, 239, 147, 103, 71, 20, 2, 236, 196, 172, 144, 142, 105, 85, 52, 20, 234, 159, 254, 205 }, 294) +
            UrlObfuscator.decode(new int[] { 85, 61, 82, 184, 200, 171, 138, 101, 26, 59, 27, 245, 212, 170, 128, 113, 80, 106, 26, 167, 203, 171, 135, 118, 87, 15, 11, 242, 212, 145, 189, 146, 114, 79, 59, 16, 246, 196, 254, 210, 125, 64, 127, 16, 243, 219, 167, 155, 105, 12, 99, 64, 250, 194, 178, 144, 118, 77, 121 }, 60) +
            UrlObfuscator.decode(new int[] { 59, 13, 249, 138, 168, 213, 99, 73, 38, 17, 238, 199, 175, 148, 209, 127, 94, 40, 18, 236, 220, 157, 155, 115, 88, 49, 29, 230, 138, 185, 137, 38, 12, 45, 23, 246, 200, 245, 218, 59, 65, 43, 0, 247, 204, 165, 177, 138, 51, 94, 52, 30, 224, 196, 171, 215, 101, 26, 48, 29, 255, 196, 174, 135, 99, 95, 99, 11, 160, 129, 181, 131, 113, 81, 49, 12, 186 }, 77) +
            UrlObfuscator.decode(new int[] { 40, 28, 238, 155, 174, 196, 48, 86, 120, 1, 245, 212, 156, 144, 125, 74, 50, 17, 171, 140, 227, 199, 124, 72, 10, 10, 243, 198, 176, 162, 97, 108, 91, 117, 85, 160 }, 94) +
            UrlObfuscator.decode(new int[] { 6, 232, 133, 184, 202, 55, 20, 111, 14, 232, 213, 177, 151, 37, 7, 102, 43, 95, 160, 129, 252, 142, 124, 64, 35, 23, 231, 209, 178, 213, 55, 22, 59, 79, 176, 145, 236, 153, 108, 68, 34, 5, 241, 131, 229, 196, 32, 65, 17, 55, 14, 223, 212, 180, 141, 125, 89, 34, 48, 240, 218, 166, 144, 114, 67, 43, 68, 254, 206, 190, 156, 122, 73, 125 }, 111) +
            UrlObfuscator.decode(new int[] { 246, 254, 204, 253, 142, 38, 91, 119, 31, 242, 194, 151, 155, 102, 92, 53, 25, 225, 201, 142, 128, 98, 79, 39, 28, 213, 195, 166, 144, 43, 11, 122 }, 128) +
            UrlObfuscator.decode(new int[] { 231, 209, 189, 206, 123, 90, 118, 29, 224, 198, 163, 137, 114, 10, 53, 11, 242, 213, 222, 178, 171, 117, 94, 45, 9, 247, 197, 162, 206, 98, 82, 32, 81, 230, 199, 243, 155, 122, 20, 60, 31, 166, 207, 163, 140, 99, 75, 54, 91, 247, 246, 208, 185, 147, 108, 20, 48, 22, 249, 211, 167, 188, 118, 91, 54, 24, 251, 149 }, 145) +
            UrlObfuscator.decode(new int[] { 212, 160, 146, 223, 110, 92, 56, 70, 171, 143, 227, 129, 119, 71, 116, 23, 247, 221, 164, 142, 51, 29, 119 }, 162) +
            UrlObfuscator.decode(new int[] { 218, 180, 217, 98, 1, 44, 2, 248, 223, 165, 132, 54, 81, 46, 72, 244, 194, 166, 200, 100, 122, 82, 41, 29, 166, 247, 184, 140, 127, 24, 56, 29, 253, 154, 163, 222, 109, 65, 57, 24, 228, 199, 228, 192, 113, 78, 104, 20, 226, 198, 232, 204, 178, 127, 73, 52, 85, 247, 216, 160, 223, 36, 1, 120, 5, 250, 155, 224, 193, 63, 21, 101, 66, 177 }, 179) +
            UrlObfuscator.decode(new int[] { 161, 143, 113, 68, 96, 54, 24, 181, 206, 245, 142, 118, 72, 107, 6, 244, 208, 250, 150, 116, 92, 59, 15, 176, 129, 134, 139, 125, 64, 105, 11, 236, 202, 235, 146, 96, 68, 18, 44, 83, 232, 212, 170, 213, 85, 86, 34, 29, 186, 222, 179, 137, 56, 30, 120, 65, 250, 195, 224, 217, 38, 22, 118, 76, 173, 152 }, 196) +
            UrlObfuscator.decode(new int[] { 188, 146, 59, 86, 52, 28, 251, 207, 228, 151, 125, 75, 59, 72, 244, 197, 248, 133, 45, 65, 45, 15, 12, 251, 206, 168, 211, 61, 23, 59, 24, 248, 193, 177, 157, 102, 29, 126, 27, 239, 207, 156, 138, 100, 76, 36, 75, 168, 202, 180, 151, 107, 78, 46, 44, 83, 254, 211, 181, 142, 124, 86, 35, 90, 206, 208, 178, 134, 112, 29, 60, 13, 255, 195, 167, 134, 36, 75, 40, 8, 241, 197, 170, 140, 100, 82, 98, 121, 84, 167 }, 213) +
            UrlObfuscator.decode(new int[] { 143, 99, 12, 48, 1, 167, 134, 204, 189, 211, 111, 88, 40, 22, 244, 219, 158, 144, 125, 84, 58, 5, 174, 220, 173, 195, 111, 71, 35, 12, 230, 211, 142, 128, 109, 68, 42, 21, 169, 228, 205, 190, 210, 104, 89, 43, 23, 251, 218, 151, 141, 59, 73, 37, 31, 255, 148, 169, 137, 103, 94, 40, 68, 229, 195, 173, 133, 117, 75, 46, 18, 69, 185, 220, 169, 143, 117, 30, 37, 94, 173, 200 }, 230) +
            UrlObfuscator.decode(new int[] { 146, 122, 70, 49, 8, 229, 216, 190, 139, 97, 90, 98, 24, 233, 219, 167, 139, 106, 103, 61, 75, 249, 213, 175, 175, 196, 121, 89, 55, 14, 248, 148, 181, 147, 125, 85, 37, 27, 254, 194, 245, 201, 108, 89, 63, 5, 174, 213, 238, 221, 120, 89 }, 247) +
            UrlObfuscator.decode(new int[] { 117, 68, 39, 17, 231, 203, 234, 132, 41, 100, 67, 32, 85, 179, 147, 226 }, 264), null);
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
            UrlObfuscator.decode(new int[] { 49, 94, 34, 24, 246, 192, 186, 157, 127, 24, 102, 21, 249, 222, 178, 145, 127, 73, 53, 70, 228, 153, 167, 141, 98, 85, 82, 59, 19, 232, 149, 187, 154, 108, 94, 32, 16, 209, 223, 183, 156, 117, 65, 58, 86, 229, 205, 226, 200, 105, 91, 58, 4, 185, 158, 255, 133, 111, 124, 75, 48, 25, 245, 206, 247, 154, 120, 82, 44, 93, 225, 215, 165, 133, 125, 64, 109, 65, 186, 145 }, 281) +
            UrlObfuscator.decode(new int[] { 89, 47, 31, 172, 223, 247, 193, 105, 9, 50, 4, 227, 237, 163, 140, 101, 99, 66, 122, 91, 178, 148, 173, 151, 91, 89, 34, 17, 225, 241, 176, 131, 106, 6, 100, 87 }, 47) +
            UrlObfuscator.decode(new int[] { 41, 57, 86, 233, 157, 230, 199, 62, 81, 57, 6, 224, 192, 244, 212, 55, 68, 110, 83, 176, 139, 191, 143, 113, 92, 38, 20, 224, 197, 228, 196, 39, 84, 30, 99, 64, 187, 200, 191, 149, 125, 84, 34, 82, 178, 149, 243, 144, 62, 70, 61, 46, 227, 197, 190, 140, 102, 83, 3, 1, 237, 215, 163, 131, 108, 122, 23, 47, 25, 239, 207, 171, 150, 55, 27, 100, 79 }, 64) +
            UrlObfuscator.decode(new int[] { 39, 17, 253, 142, 191, 209, 106, 4, 46, 13, 243, 228, 170, 145, 109, 70, 40, 14, 24, 221, 209, 181, 158, 116, 77, 10, 18, 245, 193, 252, 218, 41, 67, 53, 27, 251, 223, 162, 203, 71, 72, 60, 15, 168, 215, 171, 150, 108, 69, 104, 45, 80, 255, 211, 175, 142, 118, 85, 125, 71, 165, 132, 250, 221, 32, 0, 127, 85, 240, 207, 170, 158, 106, 64, 111, 3, 172, 223, 177, 135, 117, 85, 77, 48, 93, 177, 138, 225, 132, 101, 30, 126, 92, 175 }, 81),
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
        if (t.isEmpty() || UrlObfuscator.decode(new int[] { 12, 244, 204, 211 }, 98).equals(t)) return null;
        try {
            if (t.startsWith("#")) return Color.parseColor(t) | 0xFF000000;
            if (!t.startsWith(UrlObfuscator.decode(new int[] { 1, 245, 211 }, 115))) return null;
            String inner = t.substring(t.indexOf('(') + 1);
            String[] parts = inner.split(UrlObfuscator.decode(new int[] { 223, 253, 242, 204, 57, 49, 99, 118 }, 132));
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
        int scrim = Color.parseColor(UrlObfuscator.decode(new int[] { 182, 133, 231, 192, 37, 5, 12 }, 149));
        try {
            if (topScrim != null && topScrim.getBackground() instanceof ColorDrawable) {
                scrim = ((ColorDrawable) topScrim.getBackground()).getColor() | 0xFF000000;
            }
        } catch (Exception ignored) {}
        Integer bgC = o == null ? null : parseCssColor(o.optString("bg", null));
        Integer thC = o == null ? null : parseCssColor(o.optString(UrlObfuscator.decode(new int[] { 210, 173, 129, 110, 71 }, 166), null));
        Integer btC = o == null ? null : parseCssColor(o.optString(UrlObfuscator.decode(new int[] { 213, 162, 155 }, 183), null));
        double btnR = o == null ? -1 : o.optDouble(UrlObfuscator.decode(new int[] { 170, 147, 104, 119 }, 200), -1);

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
                accent = dark ? Color.parseColor(UrlObfuscator.decode(new int[] { 250, 190, 37, 112, 103, 50, 164 }, 217)) : Color.parseColor(UrlObfuscator.decode(new int[] { 201, 56, 31, 118, 81, 180, 231 }, 234));
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
        p.title = cardDark ? Color.WHITE : Color.parseColor(UrlObfuscator.decode(new int[] { 216, 43, 13, 105, 67, 167, 244 }, 251));
        p.sub = mixColor(p.title, p.cardTop, 0.35f);
        p.border = mixColor(accent, p.cardTop, 0.45f);
        p.accentTop = mixColor(accent, Color.WHITE, 0.12f);
        p.accentBottom = mixColor(accent, Color.BLACK, 0.12f);
        p.accentText = lumOf(accent) < 0.55 ? Color.WHITE : Color.parseColor(UrlObfuscator.decode(new int[] { 47, 26, 126, 88, 188, 150, 135 }, 268));
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
                cancel.setText(UrlObfuscator.decode(new int[] { 94, 125, 21, 57, 220, 244 }, 285));
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
            boolean chat = h.equals(UrlObfuscator.decode(new int[] { 71, 124, 28, 245 }, 51)) || h.endsWith(UrlObfuscator.decode(new int[] { 106, 23, 172, 204, 165 }, 68))
                || h.equals(UrlObfuscator.decode(new int[] { 33, 17, 255, 215, 182, 130, 110, 67, 99, 1, 238 }, 85)) || h.equals(UrlObfuscator.decode(new int[] { 18, 224, 200, 166, 133, 115, 65, 82, 112, 25, 243, 220 }, 102))
                || h.equals(UrlObfuscator.decode(new int[] { 0, 247, 155, 185, 150 }, 119)) || h.equals(UrlObfuscator.decode(new int[] { 233, 215, 175, 203, 115, 75, 35, 21, 243, 254, 206, 173, 210, 120, 85, 52 }, 136));
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
        if (scheme.equals(UrlObfuscator.decode(new int[] { 241, 204, 163, 134 }, 153)) || scheme.equals(UrlObfuscator.decode(new int[] { 194, 189, 156, 119, 85 }, 170)) || scheme.equals(UrlObfuscator.decode(new int[] { 221, 179, 149, 125 }, 187))
                || scheme.equals(UrlObfuscator.decode(new int[] { 173, 137, 101, 92, 60 }, 204)) || scheme.equals(UrlObfuscator.decode(new int[] { 185, 157, 111, 91 }, 221)) || scheme.equals(UrlObfuscator.decode(new int[] { 140, 97, 67, 41 }, 238))
                || scheme.equals(UrlObfuscator.decode(new int[] { 149, 127, 75, 61, 8, 249, 203, 177, 135, 98 }, 255)) || scheme.equals(UrlObfuscator.decode(new int[] { 115, 64, 32, 25, 233, 197, 190 }, 272))) {
            return false;
        }
        try {
            Intent intent;
            if (scheme.equals(UrlObfuscator.decode(new int[] { 72, 46, 43, 27, 243, 200 }, 289))) {
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
                if (scheme.equals(UrlObfuscator.decode(new int[] { 94, 56, 1, 241, 221, 166 }, 55))) {
                    Intent parsed = Intent.parseUri(uri.toString(), Intent.URI_INTENT_SCHEME);
                    String fallback = parsed.getStringExtra(UrlObfuscator.decode(new int[] { 42, 21, 233, 210, 183, 134, 112, 126, 38, 62, 18, 241, 222, 186, 153, 114, 103, 34, 4, 249 }, 72));
                    if (fallback != null && (fallback.startsWith(UrlObfuscator.decode(new int[] { 49, 12, 227, 198, 166, 206, 60, 29 }, 89)) || fallback.startsWith(UrlObfuscator.decode(new int[] { 2, 253, 220, 183, 220, 42, 11 }, 106)))) {
                        view.loadUrl(fallback);
                        return true;
                    }
                }
            } catch (Exception ignored) {}
            Toast.makeText(this, UrlObfuscator.decode(new int[] { 53, 245, 153, 185, 135, 102, 21, 50, 28, 231, 223, 180, 207, 122, 66, 108, 4, 250, 204, 166, 199, 114, 77, 45, 16, 162, 205, 169, 177, 149 }, 123), Toast.LENGTH_SHORT).show();
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
        String name = raw == null ? "" : raw.replaceAll("[" + (char) 92 + (char) 92 + UrlObfuscator.decode(new int[] { 163, 145, 224, 214 }, 140) + (char) 34 + UrlObfuscator.decode(new int[] { 161, 130, 167, 167 }, 157), "_").trim();
        if (name.length() > 120) name = name.substring(name.length() - 120);
        if (name.isEmpty() || name.equals(".") || name.equals("..")) name = UrlObfuscator.decode(new int[] { 202, 162, 155, 101, 70, 38, 9, 227 }, 174);
        if (name.lastIndexOf('.') <= 0 && mime != null && !mime.isEmpty()) {
            String ext = android.webkit.MimeTypeMap.getSingleton().getExtensionFromMimeType(mime);
            if (ext != null) name = name + "." + ext;
        }
        return name;
    }

    private void handleInlineDownload(String url, String contentDisposition, String mimeType) {
        try {
            String named = downloadNames.get(downloadKey(url));
            if (url.startsWith(UrlObfuscator.decode(new int[] { 219, 191, 137, 125, 1 }, 191))) {
                int comma = url.indexOf(',');
                if (comma < 0) throw new IllegalArgumentException(UrlObfuscator.decode(new int[] { 178, 142, 106, 13, 40, 10, 254, 200, 232, 178, 84, 105 }, 208));
                String meta = url.substring(5, comma);
                String payload = url.substring(comma + 1);
                String mime = meta.split(";")[0];
                if (mime.isEmpty()) mime = (mimeType != null && !mimeType.isEmpty()) ? mimeType : UrlObfuscator.decode(new int[] { 149, 101, 103, 74, 114, 12, 247, 219, 176, 150 }, 225);
                byte[] bytes;
                if (meta.contains(UrlObfuscator.decode(new int[] { 201, 115, 81, 60, 11, 187, 152 }, 242))) {
                    if (payload.indexOf('%') >= 0) payload = Uri.decode(payload);
                    bytes = android.util.Base64.decode(payload, android.util.Base64.DEFAULT);
                } else {
                    bytes = Uri.decode(payload).getBytes(UrlObfuscator.decode(new int[] { 86, 118, 7, 77, 71 }, 259));
                }
                String name = cleanDownloadName(named != null ? named : URLUtil.guessFileName(url, contentDisposition, mime), mime);
                File tmp = File.createTempFile("dl", UrlObfuscator.decode(new int[] { 58, 71, 63, 1 }, 276), getCacheDir());
                java.io.FileOutputStream fos = new java.io.FileOutputStream(tmp);
                try { fos.write(bytes); } finally { fos.close(); }
                finishInlineSave(tmp, name, mime);
                return;
            }
            // blob: -- read it inside the page and stream it back in chunks.
            String mime = (mimeType != null && !mimeType.isEmpty()) ? mimeType : "";
            String name = cleanDownloadName(named != null ? named : URLUtil.guessFileName(url, contentDisposition, mime), mime);
            Toast.makeText(this, UrlObfuscator.decode(new int[] { 97, 43, 20, 236, 205, 175, 190, 154, 116, 82, 60, 90 }, 293) + name + "\u2026", Toast.LENGTH_SHORT).show();
            final String js = UrlObfuscator.decode(new int[] { 19, 60, 12, 246, 212, 162, 156, 123, 93, 122, 4, 188, 193, 226, 128, 37, 80, 62, 27, 241, 220 }, 59)
                + UrlObfuscator.decode(new int[] { 58, 10, 248, 137, 176, 218, 104, 64, 51, 67, 218, 236, 140, 151, 138, 105, 76, 9, 31, 232, 205, 178, 133, 97, 28, 122, 73, 233, 158, 160, 158, 104, 66, 99, 77, 206, 237, 147, 193, 41, 81, 111, 22, 243, 213, 218, 247, 198, 100, 21, 40, 28, 235, 199, 185, 155, 103, 86, 6, 8, 224, 202, 243, 202, 110, 71, 37, 11, 175, 156 }, 76)
                + UrlObfuscator.decode(new int[] { 37, 82, 244, 212, 188, 138, 101, 89, 39, 73, 245, 199, 191, 147, 123, 71, 34, 2, 163, 131, 178, 169, 105, 66, 55, 11, 234, 198, 131, 146, 150, 122, 90, 57, 85, 248, 213, 183, 149, 69, 84, 34, 22, 212, 208, 185, 131, 107, 73, 100, 66, 177, 212, 243 }, 93)
                + UrlObfuscator.decode(new int[] { 22, 163, 195, 165, 134, 102, 73, 35, 91, 227, 209, 173, 129, 117, 73, 80, 48, 85, 181, 192, 174, 139, 97, 76, 32, 20, 230, 147, 176, 204, 104, 1, 60, 8, 255, 219, 165, 135, 123, 66, 106, 38, 204, 158, 245, 217, 54, 43, 13, 111, 80, 244, 220, 191, 197, 39, 13 }, 110)
                + UrlObfuscator.decode(new int[] { 62, 240, 217, 174, 148, 115, 93, 26, 5, 255, 209, 179, 150, 60, 83, 60, 0, 236, 254, 173, 157, 111, 107, 45, 0, 239, 203, 236, 141, 46, 76, 60, 35, 28, 179, 200, 162, 138, 124, 68, 43, 81, 178, 157, 232 }, 127)
                + UrlObfuscator.decode(new int[] { 184, 201, 187, 131, 111, 95, 35, 6, 230, 135, 168, 128, 124, 87, 106, 72, 251, 246, 216, 245, 147, 125, 92, 103, 69, 245, 152, 166, 157, 105, 87, 120, 11, 206, 192, 169, 158, 100, 67, 45, 42, 245, 207, 161, 131, 102, 12, 35, 12, 16, 252, 238, 189, 141, 127, 124, 54, 19, 190, 156, 239, 129, 119, 69, 37, 29, 224, 150, 177 }, 144)
                + UrlObfuscator.decode(new int[] { 215, 161, 173, 222, 111, 1, 53, 31, 238, 152, 145, 159, 121, 81, 1, 23, 240, 212, 170, 156, 37, 5, 112 }, 161)
                + UrlObfuscator.decode(new int[] { 192, 255, 159, 97, 66, 34, 13, 239, 151, 175, 157, 105, 69, 49, 13, 236, 204, 233, 201, 132, 104, 92, 46, 91, 233, 132, 139, 131, 100, 92, 58, 20, 186, 195, 254, 157, 107, 94, 57, 7, 254, 128, 243, 166, 104, 65, 54, 12, 235, 197, 130, 173, 151, 121, 91, 62, 84, 251, 212, 184, 148, 70, 85, 37, 23, 210, 216, 186, 128, 102, 4, 56, 68, 250, 221, 165, 149, 113, 86, 42, 12, 230, 136, 204, 240, 148, 114, 95, 63, 1, 215, 209, 254, 210, 56, 20, 123, 90, 161, 134, 231, 214, 99, 77, 44, 66, 181, 228, 142, 222, 106, 70, 58, 21, 168, 182, 133, 160, 199 }, 178)
                + UrlObfuscator.decode(new int[] { 177, 204, 110, 78, 90, 44, 15, 243, 201, 231, 159, 109, 89, 53, 1, 253, 220, 188, 217, 57, 84, 15, 3, 232, 217, 165, 128, 108, 101, 52, 12, 224, 196, 167, 207, 98, 115, 81, 63, 47, 250, 204, 188, 190, 118, 95, 57, 17, 247, 154, 248, 203, 114, 21 }, 195)
                + UrlObfuscator.decode(new int[] { 166, 221, 96, 84, 49, 11, 207, 222, 136, 138, 126, 72, 29, 53, 202, 141, 166, 205, 113, 77, 41, 60, 27, 181, 211, 189, 156, 53, 87, 49, 16, 190, 247, 155, 219, 56, 11, 50, 71, 165, 133, 240 }, 212)
                + UrlObfuscator.decode(new int[] { 152, 103, 66, 54, 2, 232, 183, 219, 244, 135, 90, 84, 61, 10, 248, 223, 177, 182, 97, 91, 53, 23, 234, 128, 175, 128, 100, 72, 26, 9, 241, 195, 131, 133, 106, 78, 36, 4, 87, 183, 134, 161, 134, 33 }, 229)
                + UrlObfuscator.decode(new int[] { 142, 59, 71, 54, 28, 245, 152, 230, 213 }, 246)
                + UrlObfuscator.decode(new int[] { 122, 69, 36, 16, 224, 202, 233, 133, 214, 101, 73, 46, 2, 225, 248, 182, 147, 100, 90, 61, 23, 208, 195, 185, 139, 105, 72, 98, 9, 230, 198, 170, 180, 103, 83, 33, 37, 227, 200, 172, 186, 154, 53, 21, 96, 7, 250, 217, 163, 149, 125, 28, 12, 91, 234, 205, 178, 147, 36, 4 }, 263)
                + org.json.JSONObject.quote(url) + "," + org.json.JSONObject.quote(name) + "," + org.json.JSONObject.quote(mime) + ");";
            webView.evaluateJavascript(js, null);
        } catch (Exception e) {
            Toast.makeText(this, UrlObfuscator.decode(new int[] { 92, 88, 33, 27, 248, 220, 179, 149, 48, 73, 47, 4, 224, 206, 174, 211, 40, 68, 41, 16, 232, 199, 226, 143, 111, 107, 30, 47, 25, 250, 222, 249, 140, 127, 95, 38, 84, 245, 219, 189, 149 }, 280), Toast.LENGTH_LONG).show();
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
                cv.put(android.provider.MediaStore.MediaColumns.MIME_TYPE, (mime == null || mime.isEmpty()) ? UrlObfuscator.decode(new int[] { 72, 56, 23, 234, 204, 167, 130, 118, 72, 47, 49, 81, 242, 223, 175, 159, 109, 21, 36, 2, 231, 209, 178, 159 }, 297) : mime);
                cv.put(android.provider.MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + UrlObfuscator.decode(new int[] { 16, 28, 28, 242, 208, 153, 145, 119, 118, 56, 29, 208, 218 }, 63));
                cv.put(android.provider.MediaStore.MediaColumns.IS_PENDING, 1);
                android.content.ContentResolver cr = getContentResolver();
                Uri dest = cr.insert(android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI, cv);
                if (dest == null) throw new java.io.IOException(UrlObfuscator.decode(new int[] { 29, 10, 234, 196, 173, 184, 126, 70, 58, 2, 166, 204, 170, 144, 103, 83, 52, 127, 24, 252, 213, 183, 159, 125 }, 80));
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
                File dir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), UrlObfuscator.decode(new int[] { 35, 225, 241, 213, 158, 148, 116, 123, 55, 16, 211, 223 }, 97));
                if (!dir.exists() && !dir.mkdirs()) throw new java.io.IOException(UrlObfuscator.decode(new int[] { 31, 250, 212, 166, 156, 126, 12, 45, 11, 224, 196, 162, 130 }, 114));
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
                ? shown + " downloaded \u2014 find it in your BankChoAnhDi folder"
                : UrlObfuscator.decode(new int[] { 199, 205, 182, 142, 147, 113, 92, 56, 91, 252, 216, 177, 155, 115, 81, 110, 83, 241, 222, 165, 131, 106, 13, 34, 4, 254, 137, 187, 134, 112, 64, 100 }, 131) + shown, Toast.LENGTH_LONG).show();
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
    // its own "BankChoAnhDi" subfolder in there (DownloadManager
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
        if (url != null && (url.startsWith(UrlObfuscator.decode(new int[] { 246, 223, 189, 147, 42 }, 148)) || url.startsWith(UrlObfuscator.decode(new int[] { 193, 165, 151, 99, 27 }, 165)))) {
            handleInlineDownload(url, contentDisposition, mimeType);
            return;
        }

        final String filename = URLUtil.guessFileName(url, contentDisposition, mimeType);
        final String cookie = CookieManager.getInstance().getCookie(url);

        try {
            DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
            request.addRequestHeader(UrlObfuscator.decode(new int[] { 227, 166, 145, 97, 31, 16, 23, 234, 192, 185 }, 182), userAgent);
            if (cookie != null) request.addRequestHeader(UrlObfuscator.decode(new int[] { 132, 137, 106, 79, 42, 7 }, 199), cookie);
            request.setTitle(filename);
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
            request.setDestinationInExternalPublicDir(
                Environment.DIRECTORY_DOWNLOADS, UrlObfuscator.decode(new int[] { 154, 150, 120, 94, 23, 27, 253, 240, 190, 135, 74, 68, 99 }, 216) + filename);
            request.setAllowedOverMetered(true);
            request.setAllowedOverRoaming(true);
            request.setVisibleInDownloadsUi(true);

            DownloadManager downloadManager = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
            pendingDownloadId = downloadManager.enqueue(request);
            startDownloadProgressPolling(pendingDownloadId);
            Toast.makeText(this, UrlObfuscator.decode(new int[] { 173, 103, 80, 40, 9, 235, 194, 166, 136, 110, 120, 30 }, 233) + filename + "…", Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Toast.makeText(this, UrlObfuscator.decode(new int[] { 190, 118, 79, 57, 26, 250, 213, 183, 210, 119, 81, 38, 2, 232, 200, 235, 199, 36, 8, 36, 14, 224, 199, 168, 194, 120, 79, 74, 44, 93, 255, 212, 180, 151, 125, 84, 34, 28, 251, 221, 242, 144, 126, 75, 110, 25, 254, 210, 234, 136, 111, 70, 47, 11 }, 250), Toast.LENGTH_LONG).show();
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
            String title = (titleIdx >= 0 && cursor.getString(titleIdx) != null) ? cursor.getString(titleIdx) : UrlObfuscator.decode(new int[] { 77, 67, 37, 13 }, 267);

            if (status == DownloadManager.STATUS_SUCCESSFUL) {
                String localUriStr = uriIdx >= 0 ? cursor.getString(uriIdx) : null;
                String folderName = UrlObfuscator.decode(new int[] { 88, 84, 45, 23, 244, 216, 183, 145, 103 }, 284);
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
                Toast.makeText(this, title + UrlObfuscator.decode(new int[] { 18, 53, 31, 248, 192, 161, 131, 106, 78, 44, 12, 167, 8370, 229, 130, 106, 76, 37, 64, 22, 234, 157, 181, 149, 58, 64, 55, 2, 228, 149 }, 50) + folderName + UrlObfuscator.decode(new int[] { 99, 4, 238, 204, 219, 187, 143 }, 67), Toast.LENGTH_LONG).show();
                notifyDownloadResult(true);

                // If what just finished downloading is itself an installable
                // Android package, go straight to the system installer
                // instead of leaving the user to dig it out of the Downloads
                // folder and tap it themselves. Gated strictly on the file
                // actually being a .apk -- every other kind of download this
                // wrapper handles (images, PDFs, whatever the wrapped site
                // links to) still behaves exactly as before, since "install"
                // wouldn't mean anything for those.
                if (localPath != null && title != null && title.toLowerCase().endsWith(UrlObfuscator.decode(new int[] { 122, 18, 226, 218 }, 84))) {
                    File apkFile = new File(localPath);
                    if (apkFile.exists()) {
                        try {
                            Uri contentUri = FileProvider.getUriForFile(
                                this, getPackageName() + UrlObfuscator.decode(new int[] { 75, 226, 202, 174, 132, 112, 109, 81, 43, 21, 255, 223, 171 }, 101), apkFile);
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
                Toast.makeText(this, UrlObfuscator.decode(new int[] { 50, 250, 195, 189, 158, 126, 81, 43, 78, 235, 205, 162, 134, 108, 76, 125, 70 }, 118) + title + UrlObfuscator.decode(new int[] { 167, 142, 160, 150, 113, 77, 51, 64 }, 135) + reason + ")", Toast.LENGTH_LONG).show();
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
                    UrlObfuscator.decode(new int[] { 236, 197, 175, 142, 125, 85, 122, 6, 249, 193, 170, 130, 123, 5, 21, 54, 231, 201, 136, 132, 112, 74, 52, 4, 196, 240, 201, 179, 144, 116, 91, 61, 59, 248, 219, 165, 152, 118, 70, 52, 89, 244, 217, 164, 130, 111, 69, 62, 70, 216, 249, 170, 138, 77, 67, 53, 9, 9, 251, 249, 179, 140, 116, 85, 55, 22, 242, 246, 187, 158, 98, 93, 53, 27, 235, 133 }, 152) + success + UrlObfuscator.decode(new int[] { 128, 243, 154, 123, 70, 37, 23, 225, 201, 232, 186, 215, 102, 65 }, 169),
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
                UrlObfuscator.decode(new int[] { 206, 171, 129, 108, 95, 51, 92, 228, 219, 191, 148, 96, 89, 99, 51, 212, 197, 167, 166, 102, 82, 44, 18, 230, 230, 174, 151, 145, 114, 82, 61, 31, 202, 203, 183, 144, 100, 80, 39, 0, 187, 202, 167, 134, 96, 73, 35, 28, 164, 246, 151, 136, 104, 107, 37, 23, 235, 215, 165, 155, 145, 106, 82, 55, 21, 248, 220, 135, 132, 122, 83, 33, 23, 226, 195, 231 }, 186) + done + "," + total + "," + status + UrlObfuscator.decode(new int[] { 226, 209, 116, 85, 36, 7, 241, 199, 171, 202, 100, 9, 68, 35 }, 203),
                null);
        } catch (Exception ignored) {
        }
    }

    private void notifyDownloadCancelled() {
        try {
            webView.evaluateJavascript(
                UrlObfuscator.decode(new int[] { 168, 137, 99, 66, 49, 17, 190, 194, 189, 157, 118, 94, 39, 65, 209, 242, 163, 133, 68, 72, 60, 14, 240, 192, 128, 140, 117, 79, 44, 48, 31, 249, 255, 180, 151, 105, 84, 50, 2, 240, 157, 168, 133, 120, 94, 43, 1, 250, 130, 148, 181, 102, 70, 9, 7, 241, 205, 181, 135, 69, 79, 72, 48, 17, 243, 218, 190, 186, 119, 90, 38, 25, 241, 199, 183, 217, 118, 78, 34, 30, 233, 135, 237, 138, 105, 73, 37, 0, 232, 207, 167, 133, 39, 54, 5, 32, 1, 248, 219, 173, 155, 127, 30, 48, 93, 232, 207 }, 220),
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
            Toast.makeText(this, UrlObfuscator.decode(new int[] { 172, 96, 71, 37, 30, 168, 206, 168, 150, 112, 66, 46, 13, 243, 191, 216, 175, 147, 118, 26, 45, 16, 254, 197, 245, 149, 99, 66, 125, 80, 251, 198, 168, 130, 43, 67, 61, 79, 235, 202, 229, 135, 108, 76, 53, 9, 17, 235, 216, 252, 154, 111, 77, 55, 26, 247, 193, 189, 144, 115, 93, 60, 22 }, 237), Toast.LENGTH_LONG).show();
            try {
                Intent settingsIntent = new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                    Uri.parse(UrlObfuscator.decode(new int[] { 142, 124, 95, 48, 27, 254, 221, 237 }, 254) + getPackageName()));
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
        installIntent.setDataAndType(contentUri, UrlObfuscator.decode(new int[] { 110, 94, 61, 0, 226, 201, 168, 156, 110, 73, 43, 75, 245, 204, 165, 206, 158, 112, 89, 46, 20, 243, 221, 246, 135, 119, 86, 63, 18, 245, 212, 253, 142, 124, 78, 36, 2, 252, 204 }, 271));
        installIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_GRANT_READ_URI_PERMISSION);
        try {
            startActivity(installIntent);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, UrlObfuscator.decode(new int[] { 110, 80, 126, 20, 242, 200, 174, 152, 116, 91, 51, 7, 180, 213, 189, 132, 126, 75, 110, 64, 161, 139, 165, 153, 109, 73, 102, 17, 236, 198, 226, 135, 105, 115, 91, 125, 26, 233, 213, 180, 216, 110, 89, 32, 6, 179, 246, 190, 135, 97, 66, 34, 13, 239, 217, 233, 142, 104, 74, 33, 1, 241, 130, 168, 142, 140, 106, 88, 61, 31 }, 288), Toast.LENGTH_LONG).show();
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
                Toast.makeText(this, UrlObfuscator.decode(new int[] { 101, 33, 27, 225, 211, 182, 149, 47, 94, 40, 30, 230, 195, 186, 155, 110, 73, 43, 68, 234, 209, 225, 142, 154, 123, 89, 57, 31, 186, 205, 183, 215, 101, 84, 34, 22, 178, 197, 184, 138, 46, 73, 35, 28, 228, 197, 167, 134, 98 }, 54), Toast.LENGTH_LONG).show();
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
                Toast.makeText(this, UrlObfuscator.decode(new int[] { 4, 7, 232, 193, 177, 131, 46, 77, 86, 61, 15, 243, 203, 178, 150, 118, 82, 118, 5, 241, 193, 191, 152, 99, 92, 39, 2, 226, 139, 163, 154, 40, 73, 35, 0, 224, 198, 166, 193, 102, 112, 76, 125, 8, 243, 211, 170 }, 71), Toast.LENGTH_LONG).show();
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
                Toast.makeText(this, UrlObfuscator.decode(new int[] { 20, 24, 245, 212, 160, 154, 125, 95, 112, 31, 235, 223, 161, 130, 121, 90, 33, 8, 232, 133, 173, 144, 34, 79, 37, 58, 26, 248, 216, 251, 156, 118, 74, 119, 2, 253, 221, 160 }, 88), Toast.LENGTH_LONG).show();
            }
        } else if (requestCode == NOTIFICATION_PERMISSION_REQUEST_CODE) {
            if (!(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                Toast.makeText(this, UrlObfuscator.decode(new int[] { 39, 231, 211, 175, 131, 109, 64, 35, 21, 233, 240, 208, 174, 220, 122, 72, 60, 88, 248, 208, 179, 212, 117, 93, 35, 80, 251, 198, 164, 159, 43, 75, 57, 24, 167, 139, 232, 196, 119, 87, 51, 14, 95, 234, 213, 185, 150, 58, 86, 54, 87, 255, 219, 244, 160, 119, 69, 36, 6, 224, 202, 191, 203, 107, 71, 49, 71, 242, 204, 169, 134 }, 105), Toast.LENGTH_LONG).show();
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
            UrlObfuscator.decode(new int[] { 30, 252, 222, 182, 131, 121, 64 }, 122), UrlObfuscator.decode(new int[] { 204, 207, 167, 141, 117, 71, 41 }, 139), NotificationManager.IMPORTANCE_DEFAULT);
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
                Toast.makeText(this, UrlObfuscator.decode(new int[] { 213, 213, 169, 141, 121, 91, 58, 85, 228, 214, 160, 156, 121, 92, 61, 4, 227, 197, 234, 158, 105, 84, 40, 66, 240, 131, 165, 147, 97, 113, 74, 56, 24, 187, 151, 244, 216, 120, 70, 48, 26, 179, 198, 185, 149, 47, 72, 36, 0, 238, 138, 175, 154, 104, 75, 101, 29, 236, 215, 179, 192, 187, 113, 74, 50, 23, 245, 216, 188, 132, 54, 83, 59, 31, 246, 212, 162, 207, 122, 66, 108, 2, 228, 218, 188, 134, 106, 73, 100, 14, 227, 207, 181, 190, 146, 113, 69 }, 156), Toast.LENGTH_LONG).show();
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
