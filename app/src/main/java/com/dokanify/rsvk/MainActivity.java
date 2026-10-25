package com.dokanify.rsvk;

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
            paint.setColor(Color.parseColor(UrlObfuscator.decode(new int[] { 209, 119, 86, 41, 8, 235, 202 }, 242)));
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
        root.setBackgroundColor(Color.parseColor(UrlObfuscator.decode(new int[] { 10, 46, 1, 224, 195, 162, 133 }, 297)));

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
        swipeRefresh.setColorSchemeColors(Color.parseColor(UrlObfuscator.decode(new int[] { 28, 56, 27, 250, 221, 188, 159 }, 63)));
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

        final SplashController loading = new NativeLaunchView(this, Color.parseColor(UrlObfuscator.decode(new int[] { 183, 213, 180, 151, 118, 73, 40 }, 148)), Color.parseColor(UrlObfuscator.decode(new int[] { 134, 245, 162, 51, 96, 113, 30 }, 165)), 1);
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
        final String offlineScrimColor = offlineIsNightMode ? UrlObfuscator.decode(new int[] { 149, 236, 205, 35, 2, 97, 64, 191, 158 }, 182) : UrlObfuscator.decode(new int[] { 228, 208, 51, 20, 115, 82, 177, 144, 143 }, 199);
        // Slightly translucent (not fully opaque) card/button fills, so a
        // hint of the dimmed scrim behind still shows through -- a cheap
        // stand-in for a true frosted-glass blur, which would need
        // RenderEffect (API 31+) and a snapshot of whatever's behind the
        // card to do for real.
        final String offlineCardBg = offlineIsNightMode ? UrlObfuscator.decode(new int[] { 251, 178, 46, 7, 23, 65, 215, 130, 224 }, 216) : UrlObfuscator.decode(new int[] { 202, 74, 31, 0, 87, 194, 145, 132, 211 }, 233);
        final String offlineTitleColor = offlineIsNightMode ? UrlObfuscator.decode(new int[] { 217, 95, 126, 17, 48, 211, 242 }, 250) : UrlObfuscator.decode(new int[] { 40, 26, 121, 88, 183, 150, 245 }, 267);
        final String offlineSubtitleColor = offlineIsNightMode ? UrlObfuscator.decode(new int[] { 63, 121, 98, 59, 160, 245, 146 }, 284) : UrlObfuscator.decode(new int[] { 17, 100, 50, 186, 236, 248, 169 }, 50);
        final String offlineButtonBg = offlineIsNightMode ? UrlObfuscator.decode(new int[] { 96, 90, 194, 148, 135, 234, 197, 40, 122 }, 67) : UrlObfuscator.decode(new int[] { 119, 75, 209, 245, 230, 171, 56, 105, 13 }, 84);
        final float offlineDensity = getResources().getDisplayMetrics().density;

        // Outer full-screen scrim so the card reads as a dialog sitting on
        // top of the app, matching how a native "no connection" alert
        // dims everything behind it.
        final FrameLayout errorView = new FrameLayout(this);
        errorView.setBackgroundColor(Color.parseColor(UrlObfuscator.decode(new int[] { 70, 180, 226, 242, 160, 48, 93 }, 101)));
        errorView.setVisibility(View.GONE);

        // The error screen is the branded HTML page in ERROR_PAGE_HTML,
        // rendered in its own small WebView that fills errorView. Its
        // Retry / Back buttons call back into native code through the
        // AndroidError interface (registered below, once doRetry exists).
        // Navigation inside it is blocked so it can only ever show that page.
        final WebView errorWeb = new WebView(this);
        errorWeb.setBackgroundColor(Color.parseColor(UrlObfuscator.decode(new int[] { 85, 165, 245, 227, 179, 33, 114 }, 118)));
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
        navOverlay.setBackgroundColor(Color.parseColor(UrlObfuscator.decode(new int[] { 164, 192, 163, 130, 101, 68, 39 }, 135)));
        navOverlay.setVisibility(View.GONE);
        final SkeletonView navSkeleton = new SkeletonView(this, Color.parseColor(UrlObfuscator.decode(new int[] { 187, 209, 176, 147, 114, 85, 52 }, 152)));
        navSkeleton.setTarget(webView);
        navSkeletonRef = navSkeleton;
        navOverlay.addView(navSkeleton, new FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
        final BouncingDotsView navDots = new BouncingDotsView(this, Color.parseColor(UrlObfuscator.decode(new int[] { 138, 249, 166, 55, 100, 117, 34 }, 169)));
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
                UrlObfuscator.decode(new int[] { 146, 191, 141, 121, 85, 33, 29, 252, 220, 249, 217, 116, 90, 63, 21, 240, 220, 168, 154, 39, 68, 120, 0, 236, 193, 180, 141, 154, 112, 73, 114, 25, 245, 221, 161, 204, 127, 83, 124, 82, 240, 152, 162, 138, 122, 88, 62, 5, 170, 221, 186, 146, 99, 30 }, 186)
                + UrlObfuscator.decode(new int[] { 162, 140, 33, 0, 37, 72, 236, 202, 173, 135, 115, 116, 90, 38, 9, 224, 199, 253, 222, 49, 25, 34, 7, 253, 222, 250, 216, 62, 67, 43, 3, 235, 223, 162, 215, 56, 14, 52, 0, 240, 214, 176, 143, 32, 121, 95, 49, 15, 254, 129 }, 203)
                + UrlObfuscator.decode(new int[] { 174, 158, 110, 76, 42, 25, 182, 215, 250, 130, 103, 84, 34, 22, 221, 200, 160, 142, 105, 93, 39, 21, 199, 201, 168, 203, 37, 72, 45, 56, 82, 235, 213, 191, 159, 118, 20, 52, 23, 251, 194, 178, 129, 61, 67, 57, 9, 161, 197, 173, 152, 104, 69, 34, 74, 236, 202, 179, 151, 117, 12, 93, 43, 9, 232, 212, 180, 222, 49, 25, 58, 16, 250, 212, 166, 153, 45, 18, 115, 93, 183, 214 }, 220)
                + UrlObfuscator.decode(new int[] { 142, 109, 95, 41, 1, 160, 194, 239, 158, 118, 70, 54, 20, 242, 241, 158, 187, 157, 119, 73, 60, 67, 234, 203, 252, 220, 58 }, 237),
                value -> {
                    if (UrlObfuscator.decode(new int[] { 138, 111, 73, 62 }, 254).equals(value)) revealNavOverlay.run();
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
        }, UrlObfuscator.decode(new int[] { 78, 64, 41, 30, 228, 195, 173, 173, 117, 84, 42, 22 }, 271));

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
        String spoofedUA = defaultUA.replace(UrlObfuscator.decode(new int[] { 27, 31, 41, 11 }, 288), "").replaceAll("Version/[0-9.]+\s", "");
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
        webView.addJavascriptInterface(new AndroidBridge(), UrlObfuscator.decode(new int[] { 119, 59, 16, 225, 221, 184, 148, 77, 92, 36, 8, 236, 207 }, 54));

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
                UrlObfuscator.decode(new int[] { 111, 0, 240, 202, 160, 150, 104, 79, 81, 118, 84, 231, 207, 168, 128, 99, 65, 55, 7, 180, 209, 239, 149, 127, 76, 59, 0, 233, 197, 190, 199, 106, 72, 34, 28, 191, 202, 164, 201, 33, 125, 23, 47, 25, 239, 207, 171, 150, 55, 6, 110 }, 71) +
                UrlObfuscator.decode(new int[] { 49, 17, 190, 157, 182, 221, 123, 95, 62, 10, 252, 249, 169, 147, 126, 85, 52, 64, 161, 140, 234, 151, 112, 72, 45, 119, 87, 179, 208, 190, 148, 126, 76, 63, 72, 165, 157, 161, 151, 101, 69, 61, 0, 173, 157, 240 }, 88) +
                UrlObfuscator.decode(new int[] { 31, 233, 213, 230, 148, 57, 65, 108, 16, 245, 250, 204, 164, 175, 126, 86, 60, 27, 227, 217, 167, 181, 127, 94, 121, 87, 230, 195, 170, 192, 125, 67, 45, 13, 232, 138, 166, 133, 109, 84, 32, 19, 83, 237, 203, 187, 215, 115, 95, 42, 22, 251, 208, 248, 154, 124, 65, 37, 27, 162, 207, 185, 159, 126, 70, 38, 64, 175, 158 }, 105) +
                UrlObfuscator.decode(new int[] { 28, 246, 202, 255, 128, 116, 70, 115, 27, 172, 128, 244, 135, 49, 93, 101, 6, 236, 198, 160, 146, 109, 2, 101, 11, 189, 148, 143, 229, 148, 55, 16, 115, 2, 238, 214, 164, 213, 102, 14, 35, 42, 249, 242, 224, 138, 105, 95, 8, 6, 253, 201, 162, 140, 106, 68, 1, 13, 233, 250, 208, 169, 174, 126, 89, 45, 80, 190, 141 }, 122) +
                UrlObfuscator.decode(new int[] { 226, 204, 225, 154, 41, 81, 44, 0, 247, 202, 255, 221, 199, 56, 27, 46, 85, 242, 220, 177, 144, 126, 65, 106, 78, 170, 152, 162, 138, 122, 88, 62, 5, 170, 152, 243, 154, 116, 64, 48, 22, 240, 207, 224, 239, 197, 96, 95, 58, 14, 250, 208, 255, 147, 60, 79, 33, 23, 229, 197, 189, 128, 45, 29, 112, 23, 244, 129, 239, 207 }, 139),
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
                        UrlObfuscator.decode(new int[] { 232, 201, 163, 130, 113, 81, 126, 2, 253, 221, 182, 158, 103, 1, 15, 3, 232, 217, 165, 128, 108, 101, 52, 12, 224, 196, 167, 199, 38, 94, 80, 57, 14, 244, 211, 189, 186, 101, 95, 49, 19, 246, 156, 162, 145, 121, 75, 30, 7, 238, 198, 172, 156, 104, 72, 99, 66, 162, 213, 168, 142, 155, 113, 74, 114, 36, 197, 202, 179, 146, 122, 119, 59, 6, 252, 213, 249, 148, 121, 68, 34, 15, 229, 222, 230, 184, 89, 86, 47, 6, 238, 227, 175, 170, 144, 121, 1, 47, 8, 236, 221, 236 }, 156) +
                        UrlObfuscator.decode(new int[] { 219, 173, 153, 42, 74, 41, 23, 187, 195, 177, 141, 97, 85, 41, 48, 16, 181, 149, 160, 142, 107, 65, 44 }, 173) +
                        UrlObfuscator.decode(new int[] { 215, 187, 212, 108, 83, 55, 28, 248, 193, 251, 135, 112, 64, 62, 28, 227, 247, 243, 218, 59, 3, 59, 13, 243, 211, 183, 138, 56 }, 190) +
                        UrlObfuscator.decode(new int[] { 185, 143, 127, 12, 28, 87, 254, 193, 169, 130, 106, 83, 109, 11, 239, 206, 218, 172, 170, 117, 95, 46, 17, 180, 255, 235, 130, 125, 93, 54, 30, 231, 129, 167, 131, 98, 78, 56, 33, 237, 206, 161, 141, 112, 24, 43, 7, 168, 190, 233, 161, 128, 58, 114, 112, 10, 242, 194, 160, 134, 125, 9 }, 207) +
                        UrlObfuscator.decode(new int[] { 150, 158, 108, 29, 51, 14, 238, 132, 131, 170, 45 }, 224) +
                        UrlObfuscator.decode(new int[] { 135, 113, 93, 110, 29, 249, 216, 162, 212, 110, 82, 40, 6, 240, 202, 173, 143, 40, 115, 18, 41, 80, 236, 150, 177, 212, 101, 87, 49, 93, 232, 221, 164, 132, 33, 94, 56, 31, 227, 130, 146, 195, 47, 74, 106, 51, 170, 140, 181, 143, 185, 119, 69, 57, 31, 178, 141, 241, 219, 61, 29, 32, 92, 218, 152, 254, 155, 97, 107, 37, 19, 239, 205, 224, 211, 47, 9, 111, 75, 245, 142, 151, 246, 208, 105, 83, 29, 19, 225, 221, 179, 222, 33, 29, 127, 89, 185, 216, 224, 166, 36, 2, 63, 5, 207, 193, 191, 131, 97, 12, 119, 75, 173, 139, 151, 172, 156, 120, 20, 13, 80, 182, 195, 185, 179, 125, 75, 55, 21, 184, 155, 231, 176, 37, 16, 55, 82 }, 241) +
                        UrlObfuscator.decode(new int[] { 116, 64, 50, 127, 22, 252, 207, 143, 159, 97, 76, 106, 16, 224, 218, 176, 134, 120, 95, 33, 70, 232, 192, 226, 145, 111, 71, 53, 78, 243, 197, 177, 194, 104, 29, 15, 101, 20, 160, 222, 182, 215, 123, 95, 63, 25, 240, 253, 189, 149, 117, 92, 96, 1, 233, 197, 173, 157, 96, 28, 47, 78, 175, 138, 185, 151, 97, 109, 30, 51, 65, 254, 214, 247, 155, 127, 95, 57, 16, 221, 221, 181, 149, 124, 117, 36, 49, 176, 195, 175, 192, 105, 8, 43, 11, 231, 199, 149, 153, 143, 123, 0, 97, 70, 169, 159, 254, 153, 56, 91, 59, 23, 247, 231, 177, 131, 123, 72, 98, 31, 248, 192, 165, 207, 47, 11, 40, 6, 236, 198, 180, 183, 192, 45, 21, 41, 31, 237, 205, 165, 152, 53, 64, 33, 7, 244, 139, 178, 156, 104, 88, 62, 24, 231, 136, 161, 135, 105, 87, 38, 89, 252, 155 }, 258) +
                        UrlObfuscator.decode(new int[] { 101, 83, 35, 80, 225, 193, 169, 137, 120, 23, 45, 7, 228, 211, 168, 129, 109, 86, 111, 17, 10, 251, 207, 165, 168, 127, 85, 61, 20, 226, 218, 166, 178, 126, 93, 120, 72, 231, 192, 171, 199, 124, 64, 44, 2, 233, 137, 167, 130, 108, 87, 33, 44, 82, 238, 202, 188, 214, 123, 77, 35, 2, 250, 218, 255, 155, 127, 64, 58, 26, 161, 223, 174, 134, 108, 75, 51, 74, 241, 193, 187, 150, 96, 82, 90, 63, 81, 199, 201, 181, 149, 125, 10, 52, 0, 224, 199, 189, 159, 77, 3, 38, 92, 160, 195, 248, 197, 96, 20, 106, 13, 176, 143, 178, 205, 108, 118, 18, 60, 80, 232, 202, 184, 150, 59, 90, 52, 22, 246, 222, 246, 217, 52 }, 275) +
                        UrlObfuscator.decode(new int[] { 66, 44, 16, 169, 214, 222, 172, 221, 117, 6, 106, 66, 241, 139, 184, 154, 112, 86, 33, 95, 252, 202, 160, 138, 120, 67, 108, 79, 231, 210, 178, 203, 104, 70, 44, 6, 244, 247, 130, 236, 206, 43, 1, 48, 83, 188, 159, 174 }, 292) +
                        UrlObfuscator.decode(new int[] { 76, 56, 10, 183, 211, 185, 201, 125, 93, 53, 21, 252, 245, 164, 177, 48, 92, 40, 26, 167, 212, 248, 129, 111, 12, 38, 5, 11, 220, 210, 169, 149, 126, 80, 54, 16, 213, 217, 189, 150, 124, 69, 2, 10, 237, 217, 228, 194, 49 }, 58) +
                        UrlObfuscator.decode(new int[] { 34, 12, 161, 218, 233, 145, 108, 64, 55, 10, 189, 146, 143, 162, 129, 110, 21, 50, 28, 241, 208, 190, 129, 40, 11, 46, 13, 226, 129, 172, 130, 120, 95, 37, 4, 180, 154, 246, 153, 120, 81, 108, 21, 239, 239, 128, 224, 180, 103, 70, 43, 86, 229, 223, 178, 156, 103, 14, 108, 64, 243, 210, 191, 194, 103, 79, 47, 28, 185, 155, 146, 205, 96, 77, 47, 20, 22, 240, 200, 185, 192 }, 75) +
                        UrlObfuscator.decode(new int[] { 53, 29, 178, 203, 246, 128, 127, 81, 32, 27, 184, 195, 254, 135, 107, 68, 43, 3, 254, 151, 248, 201, 51, 15, 19, 73, 202, 136, 163, 176, 144, 105, 85, 53, 15, 252, 131 }, 92) +
                        UrlObfuscator.decode(new int[] { 27, 237, 217, 234, 138, 123, 26, 33, 0, 240, 224, 173, 140, 112, 106, 74, 56, 24, 200, 206, 160, 148, 114, 30, 48, 24, 186, 137 }, 109) +
                        UrlObfuscator.decode(new int[] { 23, 251, 148, 184, 137, 55, 92, 62, 5, 229, 216, 178, 139, 44, 13, 114, 73, 227, 195, 165, 143, 46, 84, 59, 5, 246, 138, 181, 139, 114, 73, 93, 55, 17, 245, 207, 163, 196, 37, 10, 113, 29, 253, 215, 182, 148, 126, 8, 50, 17, 252, 202, 184, 154, 109, 97, 42, 10, 229, 215, 234, 130, 115, 49, 81, 45, 29, 248, 211, 173, 129, 107, 74, 114, 69, 180, 155, 237, 192, 33, 30, 120, 69, 232, 197, 167, 156, 110, 72, 48, 1, 184 }, 126) +
                        UrlObfuscator.decode(new int[] { 249, 207, 191, 204, 127, 75, 46, 85, 226, 202, 235, 144, 98, 69, 15, 1, 18, 251, 147, 168, 148, 86, 86, 47, 18, 228, 246, 181, 128, 119, 25, 121, 84 }, 143) +
                        UrlObfuscator.decode(new int[] { 214, 222, 172, 221, 117, 72, 14, 28, 224, 195, 235, 221, 96, 82, 53, 76, 173, 146, 233, 133, 61, 12, 54, 21, 252, 198, 161, 216, 57, 30, 101, 9, 178, 184, 194, 161, 136, 122, 93, 100, 69, 170, 145, 189, 199, 52, 78, 45, 4, 238, 201, 240, 209, 54, 13, 33, 92, 160, 218, 185, 144, 98, 69, 124, 93, 66, 185, 205, 251, 135, 102, 77, 57, 16, 171, 136, 233, 212, 126, 88, 119, 19, 242, 217, 173, 140, 55, 20, 117, 64, 231, 130, 184, 159, 118, 64, 39, 98, 67, 160, 155, 168, 138, 120, 86, 112, 10, 233, 192, 178, 149, 44, 13, 114, 73, 225, 205, 169, 143, 101, 15, 110, 93 }, 160) +
                        UrlObfuscator.decode(new int[] { 216, 182, 199, 103, 94, 24, 14, 242, 221, 225, 156 }, 177) +
                        UrlObfuscator.decode(new int[] { 171, 135, 40, 62, 86, 60, 15, 207, 223, 161, 140, 63, 83, 57, 93, 186, 209, 190, 158, 123, 71, 35, 25, 238, 145 }, 194) +
                        UrlObfuscator.decode(new int[] { 165, 147, 99, 16, 41, 29, 176, 220, 170, 152, 122, 77, 1, 10, 234, 197, 183, 202, 98, 83, 17, 56, 18, 242, 207, 137, 144, 98, 82, 127, 9, 232, 130, 228, 202, 102, 78, 60, 77, 224, 195, 247, 153, 105, 85, 53, 0, 194, 207, 173, 128, 116, 55, 93, 46, 82, 247, 211, 183, 157, 95, 83, 60, 19, 251, 198, 248, 203, 102, 72, 101, 77, 231, 194, 181, 148, 107, 78, 121, 2, 240, 139, 173, 136, 194, 120, 78, 118, 74, 180, 138, 227 }, 211) +
                        UrlObfuscator.decode(new int[] { 146, 98, 80, 97, 12, 22, 240, 216, 175, 198, 87, 88, 44, 31, 184, 216, 181, 139, 58, 0, 124, 34, 239, 217, 164, 197, 103, 64, 38, 79, 181, 137, 137, 130, 118, 73, 110, 45, 17, 232, 210, 191, 210, 107, 22, 63, 19, 252, 211, 187, 134, 62, 92, 39, 71, 164, 133, 240 }, 228) +
                        UrlObfuscator.decode(new int[] { 131, 117, 65, 114, 19, 248, 146, 131, 140, 120, 67, 100, 4, 233, 223, 238, 221, 40, 69, 49, 75, 176, 177, 134, 232, 213, 32, 76, 56, 10, 183, 218, 176, 146, 103, 15, 35, 94, 227, 203, 171, 152, 48, 92, 40, 26, 167, 209, 245, 217, 113, 12, 54, 9, 27, 234, 213, 231 }, 245) +
                        UrlObfuscator.decode(new int[] { 111, 67, 108, 15, 235, 207, 165, 172, 195, 32, 1, 106, 83, 226, 204, 165, 143, 110, 66, 50, 0, 177, 194, 168, 211, 105, 67, 40, 31, 228, 205, 169, 146, 43, 71, 49, 7, 224, 212, 218, 140, 156, 114, 92, 63, 81, 177, 140, 164, 146, 58, 64, 55, 29, 245, 204, 186, 163, 99, 79, 47, 42, 231, 201, 178, 128, 106, 87, 49, 73, 229, 243, 151, 230, 138, 122, 72, 121, 10, 229, 139, 167, 147, 61, 85, 52, 4, 205, 193, 184, 130, 111, 67, 39, 15, 196, 202, 172, 129, 109, 86, 19, 5, 28, 234, 149, 245, 192, 115, 95, 112, 5, 228, 155, 163, 154, 118, 69, 56, 81, 182, 132, 183, 135, 111, 79, 60, 90, 244, 215, 234, 143, 103, 71, 52, 100, 9, 173, 129, 150, 155, 109, 80, 121, 27, 252, 218, 251, 128, 63, 71, 38, 10, 249, 196, 231, 152, 123, 6, 48, 15, 225, 208, 171, 203, 58, 93, 66, 61, 28, 232, 216, 178, 209, 125, 30, 45, 8, 233 }, 262) +
                        UrlObfuscator.decode(new int[] { 113, 89, 39, 92, 229, 211, 163, 208, 101, 19, 125, 87, 225, 150, 165, 129, 105, 67, 54, 95, 233, 137, 234, 201, 132, 110, 72, 47, 19, 178, 213, 189, 145, 98, 25, 38, 93, 230, 222, 160, 196, 100, 7, 32, 3, 161, 129, 164, 143, 43, 71, 44, 74, 173, 147, 236, 247, 146, 116, 82, 62, 9, 167, 137, 241, 208, 127, 9, 110, 79, 253, 217, 161, 139, 126, 1, 122, 67, 182, 223, 247, 204, 53, 10, 117, 80, 187, 215, 143, 242, 159, 116, 23, 110, 80, 163, 202 }, 279) +
                        UrlObfuscator.decode(new int[] { 85, 34, 10, 246, 193, 184 }, 296) +
                        UrlObfuscator.decode(new int[] { 72, 60, 14, 187, 216, 171, 197, 103, 87, 39, 7, 246, 244, 189, 159, 110, 90, 101, 15, 248, 132, 171, 135, 117, 66, 32, 22, 215, 205, 177, 172, 154, 120, 73, 14, 26, 254, 208, 173, 132, 63, 73, 40, 67, 169 }, 62) +
                        UrlObfuscator.decode(new int[] { 38, 8, 165, 255, 191, 152, 96, 70, 32, 78, 230, 215, 237, 128, 110, 82, 91, 59, 15, 200, 212, 170, 181, 125, 81, 34, 39, 245, 215, 187, 132, 99, 6, 96, 4, 226, 207, 175, 145, 71, 65, 110, 66, 161, 132, 235, 223, 61, 47, 23, 63, 14, 166, 216, 171, 215, 38, 6, 101, 94, 222, 211, 165, 152, 33, 67, 36, 2, 163, 216, 231, 159, 110, 66, 49, 12, 175, 208, 239, 136, 154, 119, 90, 52, 15, 179, 130 }, 79) +
                        UrlObfuscator.decode(new int[] { 16, 10, 237, 213, 244, 137, 52, 85, 61, 17, 226, 153, 166, 221, 102, 94, 32, 67, 252, 131, 187, 130, 110, 93, 32, 75, 244, 139, 172, 134, 107, 70, 40, 43, 82, 255, 206, 242, 193 }, 96) +
                        "}}" +
                        UrlObfuscator.decode(new int[] { 24, 246, 135, 161, 152, 120, 5, 38, 12, 230, 192, 178, 141, 56, 23, 107, 19, 229, 235, 203, 175, 146, 32 }, 113) +
                        UrlObfuscator.decode(new int[] { 237, 212, 180, 241, 141, 114, 78, 47, 82, 255, 205, 185, 149, 97, 93, 60, 28, 185, 209, 227, 140, 36, 87, 57, 15, 253, 221, 181, 136, 37, 69, 24, 83, 220, 141, 221, 133, 204, 65, 0, 39, 80, 163 }, 130) +
                        UrlObfuscator.decode(new int[] { 250, 212, 249, 159, 122, 90, 99, 0, 238, 196, 174, 156, 111, 24, 125, 84, 170, 205, 180, 148, 209, 114, 88, 50, 28, 238, 209, 229, 207, 38, 14 }, 147) +
                        UrlObfuscator.decode(new int[] { 210, 162, 144, 33, 75, 2, 50, 18, 255, 218, 174, 144, 119, 89, 120, 26, 230, 218, 181, 152, 126, 4, 34, 2, 239, 202, 190, 128, 103, 73, 104, 21, 229, 215, 170, 143, 97, 114, 91, 102 }, 164) +
                        UrlObfuscator.decode(new int[] { 194, 188, 154, 126, 84, 120, 4, 160, 193, 169, 133, 109, 93, 32, 89, 190, 131, 226, 136, 44, 66, 40, 62, 12, 220, 200, 243, 145, 55, 84, 50, 24, 242, 192, 187, 223, 32, 25, 114, 83, 176, 139, 228, 205, 32, 67, 122, 13, 171, 215, 182, 128, 114, 84, 77, 55, 19, 251, 147, 234, 213, 115, 25, 58, 16, 250, 212, 166, 153, 61, 30, 103, 86 }, 181) +
                        UrlObfuscator.decode(new int[] { 135, 139, 96, 81, 45, 8, 228, 221, 204, 180, 152, 124, 95, 119, 11, 246, 192, 176, 167, 120, 87, 61, 21, 251, 193, 163, 196, 96, 6, 3, 59, 200, 232, 235, 151, 119, 80, 40, 14, 24, 247, 219, 165, 211, 117, 76, 44, 94, 191, 142 }, 198) +
                        UrlObfuscator.decode(new int[] { 170, 149, 116, 64, 48, 26, 185, 213, 230, 149, 112, 81, 112 }, 215) +
                        UrlObfuscator.decode(new int[] { 155, 98, 82, 17, 13, 238, 199, 174, 149, 139, 54, 94, 61, 11, 182, 128, 232, 199, 63, 14, 39, 22, 230, 229, 185, 130, 107, 66, 57, 31, 162, 202, 169, 151, 42, 23, 124, 83, 178, 136, 251 }, 232) +
                        UrlObfuscator.decode(new int[] { 132, 101, 84, 55, 1, 247, 219, 250, 148, 57, 84, 51 }, 249), null);
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
                    UrlObfuscator.decode(new int[] { 34, 79, 61, 9, 229, 209, 173, 140, 108, 9, 105, 36 }, 266) +
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
                    UrlObfuscator.decode(new int[] { 111, 72, 32, 3 }, 283) +
                    "var vp=document.querySelector('meta[name=\"viewport\"]');" +
                    UrlObfuscator.decode(new int[] { 88, 54, 71, 175, 219, 188, 194, 113, 95, 56, 90, 226, 202, 167, 150, 111, 68, 46, 43, 80, 254, 206, 190, 155, 109, 93, 18, 26, 240, 217, 182, 156, 101, 24, 104, 3, 232, 216, 170, 205, 32, 19, 49, 22, 171, 215, 166, 150, 64, 84, 75, 44, 20, 254, 206, 174, 156, 48, 16, 56, 20, 249, 214, 245, 221, 55, 89, 39, 8, 251, 219, 165, 155, 124, 0, 111, 94, 224, 204, 161, 148, 109, 122, 80, 41, 82, 243, 223, 184, 156, 57, 87, 37, 4, 246, 220, 181, 179, 103, 71, 33, 8, 163, 220, 185, 193, 60, 91 }, 49) +
                    UrlObfuscator.decode(new int[] { 43, 7, 168, 190, 218, 178, 159, 110, 87, 60, 22, 227, 152, 178, 145, 103, 119, 61, 21, 226, 203, 163, 152, 73, 83, 0, 12, 175, 129, 154, 187, 98, 76, 37, 18, 16, 247, 217, 138, 146, 127, 78, 40, 24, 228, 193, 146, 154, 106, 22, 121, 70, 245 }, 66) +
                    UrlObfuscator.decode(new int[] { 37, 19, 227, 144, 188, 154, 48, 72, 36, 9, 252, 197, 162, 136, 113, 10, 32, 16, 228, 193, 203, 187, 184, 112, 94, 55, 28, 246, 195, 254, 210, 103, 71, 43, 29, 245, 136, 231, 214, 127, 95, 100, 0, 236, 154, 225, 186, 91, 66, 44, 5, 242, 240, 215, 185, 170, 114, 95, 46, 8, 248, 196, 161, 178, 122, 74, 118, 75 }, 83) +
                    UrlObfuscator.decode(new int[] { 23, 247, 140, 181, 133, 135, 106, 126, 51, 21, 238, 220, 182, 131, 43, 18, 60, 7, 255, 221, 252, 141, 97, 73, 53, 16, 167, 222, 173, 133, 109, 76, 48, 78, 246, 196, 184, 171, 211, 110, 85, 33, 31, 180, 217, 179, 156, 96, 71, 39, 72, 160, 128, 255, 203, 44, 69, 38, 26, 230, 218, 179, 135, 107, 80, 120, 22, 228, 216, 203, 243, 142, 117, 65, 63, 84, 249, 211, 188, 128, 103, 71, 104, 64, 160, 159, 235, 204, 101, 70, 58, 6, 250, 211, 167, 139, 112, 24, 63, 70, 187 }, 100) +
                    UrlObfuscator.decode(new int[] { 17, 251, 208, 167, 156, 117, 65, 58, 67, 228, 206, 171, 141, 38, 70, 54, 21, 225, 205, 166, 162, 104, 118, 82, 57, 84, 232, 206, 240, 195, 106 }, 117) +
                    UrlObfuscator.decode(new int[] { 251, 198, 165, 151, 97, 73, 104, 58, 87, 230, 193 }, 134) +
                    "try{var m=document.querySelector('meta[name=\"theme-color\"]');" +
                    UrlObfuscator.decode(new int[] { 254, 208, 253, 153, 53, 20, 38, 25, 225, 202, 162, 155, 37, 107, 39, 12, 245, 201, 172, 128, 65, 80, 40, 4, 24, 251, 155, 250, 186, 116, 93, 42, 24, 255, 209, 150, 129, 123, 85, 55, 10, 160, 204, 188, 155, 102, 80, 28, 15, 227, 200, 161, 160, 109, 77, 47, 45, 87, 230, 253, 181, 158, 107, 87, 62, 18, 215, 198, 186, 150, 118, 85, 97, 15, 253, 220, 167, 147, 93, 64, 34, 11, 224, 231, 172, 142, 110, 82, 23, 51, 83, 251, 222, 174, 184, 108, 67, 36, 28, 246, 198, 166, 148, 56, 8, 45, 2, 226, 223, 175, 135, 124, 0, 111, 25, 248, 132, 229, 200, 59, 98 }, 151) +
                    UrlObfuscator.decode(new int[] { 213, 164, 135, 113, 71, 43, 74, 228, 137, 196, 163 }, 168) +
                    UrlObfuscator.decode(new int[] { 205, 170, 142, 109, 92, 50, 91, 229, 216, 190, 139, 97, 90, 98, 42, 228, 205, 186, 136, 111, 65, 6, 17, 235, 197, 167, 186, 216, 59, 125, 53, 30, 235, 215, 190, 146, 87, 70, 58, 22, 246, 213, 225, 143, 125, 92, 39, 19, 217, 201, 160, 131, 71, 69, 32, 9, 230, 210, 208, 171, 147, 120, 18, 33 }, 185) +
                    UrlObfuscator.decode(new int[] { 188, 136, 122, 7, 54, 12, 231, 200, 128, 134, 61, 121, 75, 51, 31, 239, 211, 182, 150, 63, 70, 45, 88, 227, 203, 248, 139 }, 202) +
                    UrlObfuscator.decode(new int[] { 173, 155, 107, 24, 52, 30, 244, 221, 189, 207, 74, 109, 116, 24, 236, 222, 235, 143, 52, 76, 40, 5, 240, 201, 166, 140, 117, 14, 90, 50, 24, 241, 222, 180, 141, 94, 69, 57, 24, 196, 220, 187, 159, 100, 7, 62, 21, 177, 150, 164, 156, 100, 75, 121, 40, 229, 215, 170, 207, 102, 115, 81, 50, 14, 179, 205, 176, 150, 115, 89, 34, 90, 250, 220, 191, 149, 125, 121, 36, 8, 255, 194, 230, 218, 46, 28, 53, 28, 175, 210, 184, 221, 194, 112, 72, 48, 23, 165, 206, 177, 153, 114, 90, 35, 93, 251, 223, 190, 138, 124, 101, 41, 2, 237, 193, 188, 202, 52, 31, 52, 26, 171, 154 }, 219) +
                    UrlObfuscator.decode(new int[] { 155, 99, 67, 37, 13, 175, 195, 236, 159, 96, 74, 32, 9, 17, 176, 205, 169, 136, 114, 17, 61, 94, 173, 208, 233, 150, 60, 65, 49, 29, 235, 195, 184, 174, 102, 76, 37, 2, 232, 209, 255, 158 }, 236) +
                    UrlObfuscator.decode(new int[] { 148, 122, 19, 62, 22, 251, 194, 187, 144, 122, 71, 124, 19, 255, 203, 183, 203, 42, 72, 34, 8, 225, 201, 232, 140, 106, 71, 39, 25, 207, 249, 150, 185, 147, 120, 79, 52, 29, 249, 194, 251, 150, 124, 86, 40, 89, 179, 158, 228, 143, 99, 75, 32, 6, 169, 214, 176, 151, 107, 10, 37, 15, 28, 235, 208, 185, 149, 110, 23, 58, 24, 242, 204, 253, 200 }, 253) +
                    UrlObfuscator.decode(new int[] { 103, 75, 100, 8, 226, 200, 161, 137, 40, 76, 42, 7, 231, 217, 143, 185, 214, 121, 83, 56, 15, 244, 221, 185, 130, 59, 80, 60, 17, 228, 221, 170, 128, 121, 105, 39, 15, 228, 205, 169, 146, 44, 24, 115, 75, 226, 200, 222, 183, 147, 50, 75, 47, 10, 240, 159, 178, 154, 119, 70, 63, 20, 254, 219, 224, 137, 99, 72, 63, 4, 237, 201, 178, 160, 104, 70, 47, 4, 238, 235, 151, 230 }, 270) +
                    UrlObfuscator.decode(new int[] { 105, 95, 47, 92, 235, 219, 171, 139, 114, 11, 51, 1, 253, 209, 165, 153, 96, 64, 101, 31, 162, 209, 160, 142, 47, 7, 54, 77, 241, 199, 181, 149, 141, 112, 29, 50, 14, 246, 213, 227, 129, 119, 71, 116, 18, 175, 194, 254, 134, 96, 73, 41, 19, 197, 207, 224, 192, 46, 2, 109, 88, 244, 192, 178, 255, 156, 32, 79, 117, 19, 247, 220, 178, 142, 90, 82, 123, 85, 184, 151, 230, 213, 100, 74, 99, 11, 181, 152, 187, 154, 103, 24, 115, 75, 243, 197, 203, 171, 143, 114, 27, 52, 12, 244, 219, 237 }, 287) +
                    UrlObfuscator.decode(new int[] { 67, 53, 1, 178, 199, 237, 156, 32, 94, 57, 9, 249, 221, 186, 142, 104, 66, 108, 2, 169, 144, 236, 189, 215, 51, 79, 43, 22, 240, 204, 255, 209, 57, 19, 122, 73, 248, 214, 231, 152, 35, 64, 46, 4, 238, 220, 175, 218, 54, 13, 49, 7, 245, 213, 205, 176, 221, 114, 78, 54, 21, 163 }, 53) +
                    UrlObfuscator.decode(new int[] { 48, 4, 246, 131, 163, 141, 61, 105, 16, 49, 25, 245, 221, 173, 144, 41, 5, 106, 4, 242, 192, 162, 149, 73, 66, 34, 13, 255, 130, 191, 179, 52, 123, 108, 94, 178, 153, 183, 129, 141, 62, 79, 97, 11, 251, 203, 171, 146, 95, 91, 32, 91, 228, 234, 224, 178, 34, 28, 124, 66, 166, 206, 245, 151, 103, 87, 55, 6, 203, 207, 180, 247, 136, 70, 13, 6, 86, 168, 136, 254, 218, 119, 88, 110, 2, 240, 194, 188, 139, 68, 66, 63, 66, 255, 243, 245, 187, 41, 21, 115, 75, 186 }, 70) +
                    UrlObfuscator.decode(new int[] { 62, 16, 189, 221, 160, 188, 112, 126, 103, 28, 164, 208, 183, 131, 122, 102, 38, 40, 173, 195, 234, 158, 125, 73, 76, 16, 28, 210, 147, 184, 149, 49, 30, 36, 16, 224, 198, 160, 159, 48, 65, 59, 1, 224, 144, 184, 140, 124, 82, 52, 11, 255, 209, 248, 147, 44, 120, 4, 58, 80, 249, 128, 187, 148, 59, 87, 111, 21, 255, 207, 234, 141, 52 }, 87) +
                    UrlObfuscator.decode(new int[] { 30, 230, 212, 229, 140, 102, 90, 124, 6, 10, 240, 222, 168, 146, 117, 87, 112, 20, 191, 206, 162, 146, 96, 17, 56, 82, 232, 216, 162, 136, 126, 64, 39, 9, 174, 203, 237, 152, 108, 28, 13, 62, 10, 245, 146, 182, 155, 97, 16, 103, 90, 216, 213, 167, 154, 63, 93, 38, 0, 165, 158, 254, 223, 37, 70, 110, 79, 190, 214, 166, 150, 116, 82, 81, 126, 85, 242, 135, 235, 207, 39, 16, 102, 82, 174, 148, 245, 216, 59, 65, 96, 25, 227, 248, 190, 155, 97, 73, 33, 77, 181, 149, 235, 218, 125, 36, 76, 56, 8, 238, 200, 183, 216, 48, 21, 114, 95, 251, 154, 178, 222, 125, 7, 102, 4, 163, 201, 231, 143, 46, 13, 45, 76, 224, 140, 163, 201, 196, 99, 6 }, 104) +
                    UrlObfuscator.decode(new int[] { 31, 247, 197, 254, 131, 117, 65, 114, 26, 173, 159, 245, 134, 48, 72, 34, 8, 225, 201, 232, 137, 97, 77, 37, 21, 232, 164, 213, 246, 215, 50, 65, 47, 25, 229, 150, 182, 201, 99, 83, 35, 3, 234, 134, 170, 137, 127, 105, 38, 5, 247, 211, 177, 129, 103, 113, 53, 25, 19, 251, 149, 191, 147, 123, 80, 54, 44, 253, 232, 253, 221, 112, 80, 51, 4, 233, 223, 163, 158, 100, 77, 11, 8, 234, 202, 182, 202, 57, 72, 38, 119, 29, 187, 154, 184, 212, 120, 6, 106, 70, 187, 141, 234, 219, 99, 85, 59, 27, 255, 194, 235, 130, 108, 80, 111, 5, 172, 159, 190 }, 121) +
                    UrlObfuscator.decode(new int[] { 252, 200, 186, 199, 114, 24, 52, 2, 240, 210, 165, 247, 153, 120, 72, 24, 21, 244, 200, 162, 130, 112, 80, 0, 6, 232, 220, 170, 198, 105, 67, 40, 31, 228, 205, 169, 146, 43, 70, 44, 6, 248, 220, 195, 186, 146, 127, 78, 55, 28, 246, 195, 248, 145, 123, 80, 39, 28, 245, 193, 186, 168, 96, 78, 39, 12, 230, 211, 239, 203, 103, 76, 46, 14, 242, 182, 133 }, 138) +
                    UrlObfuscator.decode(new int[] { 242, 220, 241, 140, 49, 16, 125, 68, 189, 128, 232, 201, 37, 90, 99, 30, 160, 154, 231, 221, 63, 17, 111, 16, 173, 197, 234, 208, 209, 47, 12, 104, 81, 238, 151, 186, 222, 57, 7, 97, 70, 172, 129, 254, 217, 39, 95, 41, 31, 255, 219, 166, 199, 33, 6, 116, 86, 178, 148, 240, 234, 217, 38 }, 155) +
                    UrlObfuscator.decode(new int[] { 222, 174, 158, 124, 90, 41, 70, 162, 135, 165, 132, 103, 70, 89, 56, 90, 167, 198, 225 }, 172) +
                    UrlObfuscator.decode(new int[] { 203, 189, 137, 58, 74, 61, 25, 242, 247, 179, 206, 116, 68, 62, 12, 250, 196, 163, 133, 34, 0, 51, 19, 244, 220, 191, 149, 99, 83, 96, 55, 67, 237, 213, 184, 145, 91, 95, 127, 95, 174, 221, 181, 218, 121, 17, 114, 83, 250, 197, 165, 142, 102, 95, 105, 57, 218, 197, 173, 134, 115, 79, 86, 58, 63, 251, 247, 187, 138, 108, 30, 45, 2, 253, 221, 182, 158, 103, 1, 17, 50, 237, 197, 174, 155, 103, 78, 34, 39, 227, 239, 163, 146, 116, 34, 86, 102, 61, 245, 222, 171, 151, 126, 82, 23, 6, 250, 214, 182, 149, 33, 79, 61, 28, 231, 211, 153, 137, 96, 67, 7, 5, 224, 201, 166, 146, 144, 107, 83, 56, 83, 242, 144, 227, 138 }, 189) +
                    UrlObfuscator.decode(new int[] { 167, 139, 36, 106, 36, 13, 250, 200, 175, 129, 70, 81, 43, 5, 231, 250, 144, 188, 140, 107, 86, 32, 61, 243, 209, 176, 183, 124, 94, 62, 2, 252, 135, 182, 154, 106, 88, 105, 28, 247, 155, 181, 141, 96, 73, 3, 7, 87, 211, 220, 168, 147, 52, 95, 52, 24, 249, 199, 252, 132, 123, 95, 52, 0, 249, 131, 165, 133, 100, 76, 58, 48, 239, 193, 176, 139, 45, 19, 105, 115, 79, 180, 135, 173, 155, 107, 24, 59, 16, 168, 196, 186, 145, 122, 114, 40, 70, 188, 128, 134, 139, 125, 64, 105, 0, 233, 203, 172, 144, 41, 87, 86, 48, 25, 243, 204, 244, 144, 118, 89, 51, 7, 220, 214, 187, 150, 120, 91, 97, 95, 165, 130, 241, 159, 105, 85, 102, 23, 240, 158, 178, 136, 99, 116, 124, 58, 84, 236, 211, 183, 156, 120, 65, 123, 29, 253, 220, 180, 130, 88, 71, 41, 24, 227, 135, 251, 196, 74, 71, 49, 12, 173, 196, 173, 143, 144, 108, 21, 43, 18, 244, 221, 183, 128, 56, 92, 58, 29, 247, 195, 152, 138, 103, 74, 36, 31, 165, 155, 225, 206, 61 }, 206) +
                    UrlObfuscator.decode(new int[] { 169, 159, 111, 28, 62, 17, 164, 204, 167, 221, 121, 82, 120, 0, 229, 155, 167, 213, 100, 74, 99, 15, 226, 137, 250, 219, 114, 77, 45, 6, 238, 215, 145, 129, 162, 125, 85, 62, 11, 247, 222, 178, 176, 112, 84, 55, 61, 241, 220, 186, 196, 119, 92, 35, 7, 236, 200, 177, 203, 91, 124, 35, 15, 228, 237, 209, 180, 152, 94, 94, 62, 29, 219, 215, 166, 128, 46, 87, 58, 75, 206, 192, 169, 158, 100, 67, 45, 42, 245, 207, 161, 131, 102, 12, 32, 16, 15, 242, 196, 153, 159, 125, 92, 27, 24, 250, 218, 166, 128, 58, 69, 32, 67, 226, 203, 224, 153, 126, 0, 115, 26, 251 }, 223) +
                    UrlObfuscator.decode(new int[] { 141, 108, 79, 57, 15, 227, 130, 172, 193, 124, 91, 56, 95 }, 240) +
                    UrlObfuscator.decode(new int[] { 114, 69, 81, 58, 63, 251, 147, 243, 194 }, 257) +
                    UrlObfuscator.decode(new int[] { 123, 87, 120, 78, 249, 196, 162, 143, 101, 94, 102, 56, 217, 196, 170, 135, 112, 78, 41, 59, 60, 250, 254, 180, 143, 119, 92, 126, 13, 226, 221, 189, 150, 126, 71, 97, 49, 210, 205, 165, 142, 123, 71, 46, 2, 199, 195, 129, 141, 116, 78, 91, 99, 9, 238, 206, 191, 194, 124, 88, 53, 0, 249, 214, 188, 133, 62, 78, 42, 9, 201, 221, 175, 135, 124, 107, 47, 22, 240, 198, 172, 132, 114, 55, 25, 59, 19, 248, 207, 170, 145, 121, 17, 121, 7, 246, 220, 181, 178, 104, 2, 57, 30, 254, 207, 224, 211 }, 274) +
                    UrlObfuscator.decode(new int[] { 85, 35, 19, 160, 243, 223, 174, 136, 79, 73, 100, 72, 172, 210, 186, 151, 102, 95, 52, 30, 251, 128, 172, 136, 111, 111, 63, 13, 233, 210, 137, 141, 112, 86, 36, 14, 26, 236, 149, 251, 143, 117, 76, 59, 31, 229, 193, 181, 129, 102, 22, 124, 9, 251, 195, 175, 159, 99, 70, 38, 79, 175, 222, 178, 130, 112, 1, 46, 98, 58, 252, 200, 190, 212, 119, 87, 32, 94, 188, 143, 186, 148, 57, 94, 98, 2, 236, 223, 191, 190, 122, 20, 127, 86, 181, 141, 177, 135, 117, 85, 77, 48, 70, 240, 218, 169, 141, 76, 68, 107, 27, 175, 192, 183, 159, 116, 109, 41, 69, 165, 144, 183, 197, 115, 68, 39, 21, 240, 214, 176, 132, 58, 107, 76, 40, 25, 183, 202, 184, 139, 100, 95, 35, 17, 169, 198, 163, 133, 106, 83, 100, 87, 252, 195, 167, 140, 104, 81, 107, 5, 231, 198, 132, 150, 154, 112, 73, 16, 18, 233, 205, 189, 153, 115, 71, 124, 84, 224, 212, 163, 134, 116, 72, 107, 71, 249, 204, 166, 131, 68, 66, 109, 88 }, 291) +
                    UrlObfuscator.decode(new int[] { 77, 42, 14, 237, 195, 181, 129, 50, 83, 55, 59, 179, 195, 185, 135, 102, 18, 62, 6, 244, 133, 166, 132, 81, 66, 40, 58, 26, 160, 218, 174, 148, 122, 76, 62, 25, 251, 156, 250, 137, 120, 86, 103, 12, 234, 248, 226, 152, 108, 92, 50, 20, 235, 159, 161, 133, 85, 29, 76, 59, 9, 200, 210, 183, 156, 119, 66, 34, 93, 242, 198, 188, 146, 100, 70, 33, 3, 164, 130, 177, 139, 111, 115, 123, 11, 241, 207, 174, 218, 115, 122, 80, 57, 62, 252, 146, 240, 195, 106, 26, 100, 65, 163, 155, 234, 141, 52 }, 57) +
                    UrlObfuscator.decode(new int[] { 36, 12, 255, 135, 139, 144, 112, 66, 54, 8, 239, 241, 241, 191, 143, 126, 72, 47, 29, 229, 158, 183, 147, 64, 81, 57, 21, 235, 135, 227, 131, 105, 89, 44, 26, 241, 195, 237, 128, 108, 65, 52, 13, 26, 240, 201, 242, 159, 117, 90, 45, 26, 243, 219, 160, 182, 126, 84, 61, 10, 224, 217, 224, 144, 107, 93, 60, 21, 239, 199, 177, 151, 103, 82, 122, 43, 12, 232, 217, 247, 155, 109, 76, 37, 31, 247, 193, 167, 151, 87, 89, 35, 26, 232, 222, 241, 177, 46, 75, 43, 7, 246, 215, 228, 206, 38, 83, 75, 39, 17, 249, 156, 246, 222, 124, 86, 34, 20, 185, 199, 186, 148, 125, 74, 105, 48, 241, 130, 241 }, 74) +
                    UrlObfuscator.decode(new int[] { 50, 28, 177, 220, 184, 149, 96, 89, 54, 28, 229, 158, 173, 129, 105, 85, 98, 4, 236, 223, 231, 171, 112, 80, 34, 22, 232, 207, 209, 145, 159, 111, 94, 40, 15, 253, 197, 254, 151, 115, 96, 49, 25, 245, 203, 231, 195, 99, 73, 57, 12, 250, 209, 163, 205, 96, 76, 33, 20, 237, 250, 208, 169, 210, 121, 85, 61, 1, 187, 205, 180, 128, 103, 64, 56, 18, 250, 218, 168, 159, 49, 94, 59, 29, 226, 138, 164, 144, 119, 80, 40, 2, 10, 234, 216, 154, 146, 118, 77, 61, 5, 172, 238, 243, 144, 126, 80, 35, 28, 169, 129, 235, 152, 126, 80, 36, 2, 161, 248, 185, 202, 57, 92, 35, 62, 10, 254, 212, 243, 159, 48, 67, 42, 11 }, 91) +
                    UrlObfuscator.decode(new int[] { 17, 246, 201, 168, 156, 100, 78, 109, 1, 170, 217, 188 }, 108) +
                    UrlObfuscator.decode(new int[] { 9, 238, 194, 161, 144, 126, 31, 119, 2, 253, 221, 182, 158, 103, 1, 17, 50, 237, 197, 174, 155, 103, 78, 34, 45, 229, 211, 182, 136, 99, 108, 124, 50, 9, 245, 222, 240, 131, 96, 95, 59, 16, 252, 197, 255, 175, 80, 79, 35, 8, 249, 197, 160, 140, 79, 71, 53, 16, 234, 193, 178, 162, 144, 107, 83, 56, 70, 238, 203, 173, 146, 45 }, 125) +
                    UrlObfuscator.decode(new int[] { 234, 194, 175, 158, 103, 76, 38, 19, 168, 196, 160, 135, 71, 87, 37, 49, 10, 209, 213, 168, 142, 124, 86, 50, 4, 189, 147, 176, 158, 120, 83, 36, 73, 161, 202, 190, 132, 106, 92, 46, 9, 235, 140, 166, 203, 122 }, 142) +
                    "var t=e.target;var el=t&&t.closest?t.closest('button,a,[role=\"button\"],input[type=\"button\"],input[type=\"submit\"]'):null;" +
                    UrlObfuscator.decode(new int[] { 246, 216, 245, 153, 119, 28, 127, 15, 254, 216, 177, 155, 100, 28, 16, 30, 235, 220, 162, 133, 111, 104, 59, 1, 227, 193, 160, 194, 37, 99, 47, 4, 13, 241, 212, 184, 185, 104, 80, 60, 16, 243, 155, 162, 154, 112, 67, 49, 27, 235, 132, 183, 170, 100, 77, 58, 8, 239, 193, 134, 145, 107, 69, 39, 58, 80, 235, 213, 185, 136, 120, 76, 50, 94, 188, 143, 174 }, 159) +
                    UrlObfuscator.decode(new int[] { 205, 227, 154, 127, 89, 46, 67, 178, 213, 186, 133, 100, 80, 32, 10, 169, 197, 150, 165, 128 }, 176) +
                    // Polyfills the Web Share API on top of the real Android
                    // share sheet -- most WebView builds don't implement
                    // navigator.share at all, so a page's own "Share" button
                    // either does nothing or falls back to a hand-rolled
                    // copy-link menu instead of the native chooser. Only
                    // installed when the page doesn't already have a working
                    // navigator.share (some newer WebView versions do).
                    UrlObfuscator.decode(new int[] { 181, 146, 134, 101, 84, 58, 83, 237, 208, 182, 147, 121, 66, 122, 50, 252, 213, 162, 128, 103, 73, 14, 25, 227, 205, 175, 130, 32, 3, 5, 13, 230, 211, 175, 182, 154, 95, 78, 50, 30, 254, 221, 249, 133, 125, 85, 33, 23, 183, 150, 238, 128, 108, 90, 34, 13, 232, 220, 168, 148, 43, 87, 43, 3, 243, 197, 150, 165 }, 193) +
                    UrlObfuscator.decode(new int[] { 188, 144, 102, 70, 41, 12, 248, 196, 184, 199, 123, 79, 39, 23, 225, 158, 164, 148, 110, 124, 74, 52, 19, 245, 146, 189, 153, 99, 87, 124, 15 }, 210) +
                    UrlObfuscator.decode(new int[] { 135, 99, 85, 33, 98, 26, 252, 200, 186, 134, 101, 67, 42, 77 }, 227) +
                    UrlObfuscator.decode(new int[] { 128, 97, 75, 42, 49, 225, 202, 191, 131, 98, 78, 11, 26, 238, 194, 162, 129, 45, 81, 41, 1, 13, 251, 149, 143, 143, 104, 80, 54, 16, 190, 209, 181, 135, 115, 31, 36, 6, 250, 193, 169, 151, 118, 14, 111, 78, 170, 246, 176, 145, 107, 79, 39, 119, 26, 252, 200, 186, 212, 109, 93, 47, 2, 233, 200, 244, 213, 56, 28, 28, 26, 255, 197, 165, 141, 33, 76, 38, 18, 228, 138, 182, 144, 109, 92, 67, 121, 90, 181, 146, 225 }, 244) +
                    UrlObfuscator.decode(new int[] { 119, 65, 55, 23, 243, 206, 159, 142, 143, 115, 86, 51, 10, 253, 153, 164, 144, 103, 92, 62, 7, 245, 135, 231, 214, 113, 72, 43, 29, 235, 207, 238, 128, 45, 88, 48, 4, 244, 234, 204, 179, 220, 75, 72, 54, 21, 254, 197, 176, 218, 97, 87, 59, 21, 236, 218, 229, 137, 34, 17, 52, 21, 188 }, 261) +
                    UrlObfuscator.decode(new int[] { 120, 84, 34, 26, 245, 208, 164, 128, 124, 3, 47, 10, 228, 250, 160, 134, 116, 64, 121, 5, 247, 207, 163, 171, 151, 114, 82, 115, 83, 226, 202, 178, 130, 96, 70, 61, 82, 229, 194, 186, 139, 54, 81, 112 }, 278) +
                    UrlObfuscator.decode(new int[] { 90, 59, 6, 229, 215, 161, 137, 40, 122, 23, 38, 1 }, 295) +
                    // Reports the scrollTop of whichever element just
                    // scrolled -- document or any nested panel -- so native
                    // knows whether a pull-to-refresh gesture is actually
                    // safe (see SwipeRefreshLayout override above). Scroll
                    // events don't bubble, so this has to be a capture
                    // listener on window to see scrolling from any
                    // descendant, not just the document itself. Throttled
                    // with a trailing rAF flag so a fast scroll doesn't
                    // spam the JS bridge with a call per pixel.
                    UrlObfuscator.decode(new int[] { 73, 46, 2, 225, 208, 190, 223, 55, 66, 61, 29, 246, 222, 167, 193, 81, 114, 45, 5, 238, 219, 167, 142, 98, 118, 39, 17, 237, 205, 172, 157, 145, 104, 82, 63, 83, 226, 207, 190, 152, 113, 91, 36, 92, 206, 239, 174, 128, 105, 94, 36, 3, 237, 251, 164, 148, 106, 72, 47, 32, 238, 213, 209, 186, 192, 104, 73, 47, 28, 163 }, 61) +
                    UrlObfuscator.decode(new int[] { 56, 12, 254, 139, 186, 140, 102, 67, 47, 11, 227, 158, 164, 128, 108, 108, 91, 113, 16, 250, 201, 173, 168, 120, 69, 104, 18, 242, 222, 162, 149, 52 }, 78) +
                    UrlObfuscator.decode(new int[] { 40, 23, 243, 216, 180, 141, 55, 89, 51, 18, 208, 194, 182, 156, 101, 124, 38, 29, 249, 201, 165, 143, 123, 0, 96, 21, 230, 214, 172, 142, 109, 7, 19, 56, 8, 242, 216, 174, 144, 119, 89, 126, 16, 189, 200 }, 95) +
                    UrlObfuscator.decode(new int[] { 25, 233, 134, 189, 137, 101, 78, 32, 6, 224, 143, 183, 129, 119, 87, 51, 14, 68, 238, 216, 178, 159, 115, 87, 63, 74, 226, 199, 161, 150, 41 }, 112) +
                    UrlObfuscator.decode(new int[] { 243, 197, 206, 171, 152, 111, 79, 27, 23, 241, 218, 183, 129, 125, 92, 60, 55, 226, 206, 163, 136, 36, 77, 63, 7, 235, 211, 175, 138, 106, 11, 107, 26, 240, 250, 208, 185, 149, 117, 93, 100, 30, 246, 218, 166, 145, 40 }, 129) +
                    UrlObfuscator.decode(new int[] { 228, 208, 162, 207, 107, 65, 113, 67, 239, 135, 188, 134, 116, 66, 33, 23, 164, 135, 165, 241, 138, 124, 78, 60, 31, 237, 150, 185, 153, 113, 81, 7, 11, 225, 213, 242, 211, 48, 29, 98, 85, 236, 134, 179, 135, 119, 67, 38, 22, 187, 196, 208, 189, 136, 113, 94, 52, 13, 182, 196, 181, 135, 123, 95, 62, 24, 254, 200, 139, 129, 105, 70, 47, 7, 252, 156 }, 146) +
                    UrlObfuscator.decode(new int[] { 213, 163, 147, 32, 107, 81, 45, 65, 254, 214, 230, 157, 123, 24, 38, 23, 225, 221, 189, 156, 91, 65, 61, 86, 187, 145 }, 163) +
                    UrlObfuscator.decode(new int[] { 194, 178, 128, 49, 64, 32, 29, 176, 216, 164, 154, 55, 24, 124, 15, 227, 140, 179, 141, 114, 1, 2, 99, 17, 253, 200, 174, 169, 119, 68, 127, 14, 248, 210, 161, 133, 64, 64, 61, 80, 252, 196, 185, 210, 97, 65, 110, 18, 237, 205, 166, 142, 119, 49, 127, 51, 24, 233, 213, 176, 156, 85, 68, 60, 16, 244, 215, 247, 214, 78, 64, 41, 30, 228, 195, 173, 170, 117, 79, 33, 3, 230, 140, 179, 133, 143, 113, 79, 40, 40, 249, 203, 183, 155, 122, 97, 59, 3, 187, 202, 145, 129, 106, 95, 35, 2, 238, 235, 186, 142, 98, 66, 33, 77, 240, 196, 176, 176, 140, 105, 111, 56, 8, 246, 212, 187, 162, 122, 68, 123, 6, 254, 192, 179, 222, 36, 23, 54, 23 }, 180) +
                    UrlObfuscator.decode(new int[] { 184, 205, 56, 95, 109, 20, 13, 235, 216, 245, 192, 103, 68, 59, 22, 226, 214, 188, 219, 119, 24, 43, 18 }, 197) +
                    UrlObfuscator.decode(new int[] { 171, 220, 60, 26, 105 }, 214),
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
                if (rel.isEmpty()) rel = UrlObfuscator.decode(new int[] { 142, 104, 65, 33, 27, 172, 201, 180, 178, 146 }, 231);

                byte[] data = embedCache.get(rel);
                if (data == null) {
                    data = EmbeddedAssets.get(rel);
                    if (data != null) embedCache.put(rel, data);
                }
                if (data == null) return super.shouldInterceptRequest(view, request);

                return new WebResourceResponse(guessEmbeddedMime(rel), UrlObfuscator.decode(new int[] { 173, 67, 112, 120, 76 }, 248), new ByteArrayInputStream(data));
            }

            private String guessEmbeddedMime(String rel) {
                String lower = rel.toLowerCase();
                if (lower.endsWith(UrlObfuscator.decode(new int[] { 39, 64, 51, 11, 233 }, 265)) || lower.endsWith(UrlObfuscator.decode(new int[] { 52, 81, 44, 26 }, 282))) return UrlObfuscator.decode(new int[] { 68, 42, 22, 249, 131, 163, 158, 100, 68 }, 48);
                if (lower.endsWith(UrlObfuscator.decode(new int[] { 111, 3, 12, 237 }, 65))) return UrlObfuscator.decode(new int[] { 38, 20, 232, 219, 225, 142, 127, 88 }, 82);
                if (lower.endsWith(UrlObfuscator.decode(new int[] { 77, 232, 210 }, 99)) || lower.endsWith(UrlObfuscator.decode(new int[] { 90, 254, 216, 162 }, 116))) return UrlObfuscator.decode(new int[] { 228, 212, 179, 142, 104, 67, 94, 42, 20, 243, 213, 245, 147, 121, 65, 55, 6, 247, 193, 187, 129, 100 }, 133);
                if (lower.endsWith(UrlObfuscator.decode(new int[] { 184, 223, 167, 156, 124 }, 150))) return UrlObfuscator.decode(new int[] { 198, 182, 149, 104, 74, 33, 0, 244, 246, 209, 179, 211, 113, 73, 54, 22 }, 167);
                if (lower.endsWith(UrlObfuscator.decode(new int[] { 150, 164, 128, 114 }, 184))) return UrlObfuscator.decode(new int[] { 160, 133, 102, 65, 32, 75, 240, 212, 166, 203, 135, 115, 81 }, 201);
                if (lower.endsWith(UrlObfuscator.decode(new int[] { 244, 137, 118, 80 }, 218))) return UrlObfuscator.decode(new int[] { 130, 103, 72, 47, 2, 169, 213, 170, 132 }, 235);
                if (lower.endsWith(UrlObfuscator.decode(new int[] { 210, 113, 74, 62 }, 252)) || lower.endsWith(UrlObfuscator.decode(new int[] { 35, 70, 59, 15, 238 }, 269))) return UrlObfuscator.decode(new int[] { 119, 80, 61, 28, 255, 150, 178, 135, 115, 82 }, 286);
                if (lower.endsWith(UrlObfuscator.decode(new int[] { 26, 52, 27, 247 }, 52))) return UrlObfuscator.decode(new int[] { 44, 9, 226, 197, 164, 207, 152, 119, 91 }, 69);
                if (lower.endsWith(UrlObfuscator.decode(new int[] { 120, 2, 241, 209, 162 }, 86))) return UrlObfuscator.decode(new int[] { 14, 235, 196, 163, 134, 45, 86, 37, 61, 14 }, 103);
                if (lower.endsWith(UrlObfuscator.decode(new int[] { 86, 224, 217, 179, 146, 33 }, 120))) return UrlObfuscator.decode(new int[] { 239, 199, 169, 146, 42, 83, 44, 4, 231, 146 }, 137);
                if (lower.endsWith(UrlObfuscator.decode(new int[] { 180, 206, 183, 145, 112 }, 154))) return UrlObfuscator.decode(new int[] { 205, 165, 135, 124, 8, 49, 10, 226, 197 }, 171);
                if (lower.endsWith(UrlObfuscator.decode(new int[] { 146, 175, 142, 127 }, 188))) return UrlObfuscator.decode(new int[] { 171, 131, 101, 94, 102, 28, 243, 192 }, 205);
                if (lower.endsWith(UrlObfuscator.decode(new int[] { 240, 148, 127, 84 }, 222))) return UrlObfuscator.decode(new int[] { 134, 99, 76, 43, 14, 165, 209, 229, 142, 101, 74, 42 }, 239);
                return UrlObfuscator.decode(new int[] { 97, 111, 78, 49, 21, 248, 219, 173, 145, 120, 88, 122, 27, 240, 198, 180, 132, 34, 93, 57, 30, 238, 203, 164 }, 256);
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
                    showOffline(UrlObfuscator.decode(new int[] { 89, 100, 27, 62, 173 }, 273) + status, request.getUrl().toString());
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
                int errBg = Color.parseColor(isOfflineError(description) ? UrlObfuscator.decode(new int[] { 1, 7, 38, 57, 216, 251, 154 }, 290) : UrlObfuscator.decode(new int[] { 27, 103, 55, 165, 245, 227, 176 }, 56));
                errorView.setBackgroundColor(errBg);
                errorWeb.setBackgroundColor(errBg);
                errorWeb.loadDataWithBaseURL(UrlObfuscator.decode(new int[] { 33, 28, 243, 214, 182, 222, 44, 13, 36, 18, 13, 241, 207, 242, 151, 117, 90, 57, 27, 185 }, 73),
                    renderErrorPage(description, failingUrl), UrlObfuscator.decode(new int[] { 46, 28, 224, 195, 249, 157, 96, 94, 62 }, 90), UrlObfuscator.decode(new int[] { 62, 222, 239, 229, 223 }, 107), null);
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
                popupSettings.setUserAgentString(popupDefaultUA.replace(UrlObfuscator.decode(new int[] { 71, 187, 205, 175 }, 124), "").replaceAll("Version/[0-9.]+\s", ""));
                popupWebView.setVerticalScrollBarEnabled(false);
                popupWebView.setHorizontalScrollBarEnabled(false);

                final Dialog popupDialog = new Dialog(MainActivity.this, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
                // Loading overlay: same bouncing dots + colors as the main
                // navigation overlay, shown over the popup until its page has
                // painted, so there is never an empty black/white screen while
                // a link (sign-in, Telegram, any website) is still loading.
                final FrameLayout popupRoot = new FrameLayout(MainActivity.this);
                popupRoot.setBackgroundColor(Color.parseColor(UrlObfuscator.decode(new int[] { 174, 202, 173, 140, 111, 78, 33 }, 141)));
                popupRoot.addView(popupWebView, new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
                final FrameLayout popupLoader = new FrameLayout(MainActivity.this);
                popupLoader.setBackgroundColor(Color.parseColor(UrlObfuscator.decode(new int[] { 189, 219, 186, 157, 124, 95, 62 }, 158)));
                final BouncingDotsView popupDots = new BouncingDotsView(MainActivity.this, Color.parseColor(UrlObfuscator.decode(new int[] { 140, 255, 172, 61, 106, 123, 40 }, 175)));
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
                                UrlObfuscator.decode(new int[] { 180, 186, 134, 105, 19, 51, 14, 244, 212 }, 192), UrlObfuscator.decode(new int[] { 132, 164, 73, 3, 117 }, 209), failed);
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
                                renderErrorPage(UrlObfuscator.decode(new int[] { 170, 85, 116, 111, 126 }, 226) + status, failed),
                                UrlObfuscator.decode(new int[] { 135, 119, 73, 36, 64, 230, 217, 161, 135 }, 243), UrlObfuscator.decode(new int[] { 81, 119, 4, 76, 184 }, 260), failed);
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
                        if (scheme != null && !scheme.equals(UrlObfuscator.decode(new int[] { 125, 64, 39, 2 }, 277)) && !scheme.equals(UrlObfuscator.decode(new int[] { 78, 49, 16, 243, 209 }, 294))) {
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
        return EMBED_HOST + UrlObfuscator.decode(new int[] { 85, 53, 30, 252, 192, 249, 158, 97, 89, 63 }, 60);
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
        if (Intent.ACTION_SEND.equals(action) && UrlObfuscator.decode(new int[] { 57, 9, 243, 222, 230, 152, 107, 71, 44, 10 }, 77).equals(intent.getType())) {
            String shared = intent.getStringExtra(Intent.EXTRA_TEXT);
            if (shared != null && !shared.isEmpty()) {
                try {
                    return baseUrl() + UrlObfuscator.decode(new int[] { 97, 14, 244, 218, 168, 156, 124, 104, 34, 16, 236, 199, 239 }, 94) + java.net.URLEncoder.encode(shared, UrlObfuscator.decode(new int[] { 58, 218, 235, 225, 211 }, 111));
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

        String shortcutAction = intent.getStringExtra(UrlObfuscator.decode(new int[] { 243, 247, 209, 175, 136, 120, 79, 45, 39, 246, 213, 161, 157, 124, 92 }, 128));
        if (UrlObfuscator.decode(new int[] { 227, 213, 163, 129, 108, 72 }, 145).equals(shortcutAction)) {
            webView.getSettings().setCacheMode(WebSettings.LOAD_NO_CACHE);
            webView.reload();
            webView.getSettings().setCacheMode(WebSettings.LOAD_CACHE_ELSE_NETWORK);
            return;
        }
        if (UrlObfuscator.decode(new int[] { 202, 174, 141, 154 }, 162).equals(shortcutAction)) {
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
                blobTmp = File.createTempFile(UrlObfuscator.decode(new int[] { 209, 190, 158, 114 }, 179), UrlObfuscator.decode(new int[] { 234, 151, 111, 81 }, 196), getCacheDir());
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
                Toast.makeText(MainActivity.this, UrlObfuscator.decode(new int[] { 145, 155, 100, 92, 61, 31, 238, 202, 237, 138, 106, 67, 37, 13, 227, 156, 229, 135, 108, 87, 45, 4, 95, 240, 210, 168, 219, 104, 92, 57, 19, 182, 193, 188, 154, 97, 17, 54, 6, 226, 200 }, 213), Toast.LENGTH_LONG).show();
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
                    sendIntent.setType(UrlObfuscator.decode(new int[] { 146, 96, 92, 55, 77, 241, 204, 222, 183, 147 }, 230));
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
                String safeTitle = (title == null || title.trim().isEmpty()) ? UrlObfuscator.decode(new int[] { 179, 121, 94, 53, 29, 251, 215, 169 }, 247) : title;
                String safeBody = body == null ? "" : body;
                NotificationCompat.Builder notification = new NotificationCompat.Builder(MainActivity.this, UrlObfuscator.decode(new int[] { 108, 66, 32, 4, 241, 207, 182 }, 264))
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
            if (hex == null || !hex.matches(UrlObfuscator.decode(new int[] { 71, 27, 127, 45, 212, 153, 149, 147, 60, 86, 127, 67, 180, 241, 176, 220, 116, 84, 28, 39, 168, 226, 162, 207, 103, 16, 18, 103, 32, 231, 136, 167, 208, 60 }, 281))) return;
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
                android.content.SharedPreferences sp = getSharedPreferences(UrlObfuscator.decode(new int[] { 92, 37, 8, 224, 206, 190, 134, 102, 120, 48, 84 }, 47), MODE_PRIVATE);
                org.json.JSONArray idx = new org.json.JSONArray(sp.getString(UrlObfuscator.decode(new int[] { 31, 0, 23, 249, 196 }, 64), "[]"));
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
                ed.putString("p:" + key, json).putString(UrlObfuscator.decode(new int[] { 14, 47, 230, 202, 181 }, 81), next.toString()).apply();
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
            if (hex == null || !hex.matches(UrlObfuscator.decode(new int[] { 60, 162, 251, 254, 243, 187, 125, 22, 60, 73, 181, 142, 139, 142, 34, 78, 118 }, 98))) return;
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
            final String hexRe = UrlObfuscator.decode(new int[] { 45, 177, 234, 145, 194, 72, 76, 97, 13, 186, 132, 241, 186, 125, 19, 57, 71 }, 115);
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
                        Uri.parse(UrlObfuscator.decode(new int[] { 244, 194, 161, 138, 97, 120, 91, 103 }, 132) + getPackageName()));
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
            UrlObfuscator.decode(new int[] { 189, 210, 166, 156, 114, 68, 38, 1, 227, 132, 226, 145, 125, 90, 62, 29 }, 149) +
            UrlObfuscator.decode(new int[] { 208, 164, 150, 35, 82, 124, 4, 16, 253, 200, 177, 158, 116, 77, 118, 16, 243, 193, 145, 159, 119, 92, 53, 1, 250, 239, 181, 162, 110, 1, 111, 19, 231, 199, 148, 130, 108, 68, 44, 16, 14, 233, 213, 180, 148, 106, 31, 126, 77 }, 166) +
            UrlObfuscator.decode(new int[] { 222, 176, 221, 53, 67, 46, 13, 224, 129, 166, 132, 104, 79, 47, 7, 244, 219, 231, 149, 42, 64, 46, 0, 243, 236, 242, 180, 143, 111, 20, 58, 23, 249, 194, 180, 157, 125, 65, 121, 87, 230, 221, 224, 141, 104, 94, 32, 30, 226, 129, 236, 205, 113, 71, 53, 21, 13, 240, 134 }, 183) +
            UrlObfuscator.decode(new int[] { 190, 134, 116, 5, 37, 94, 230, 206, 163, 170, 147, 120, 82, 47, 84, 248, 219, 163, 159, 99, 81, 22, 30, 244, 221, 170, 128, 121, 23, 34, 12, 161, 137, 166, 154, 121, 69, 126, 95, 188, 196, 208, 189, 136, 113, 94, 52, 13, 182, 213, 185, 145, 109, 79, 46, 80, 224, 129, 173, 130, 98, 95, 43, 0, 230, 212, 238, 132, 45, 10, 48, 4, 244, 234, 204, 179, 199 }, 200) +
            UrlObfuscator.decode(new int[] { 175, 153, 101, 22, 33, 73, 187, 211, 255, 132, 110, 73, 3, 13, 230, 207, 181, 148, 32, 1, 108, 74, 247, 205, 141, 143, 136, 123, 79, 31, 26, 233, 220, 240, 222, 45 }, 217) +
            UrlObfuscator.decode(new int[] { 131, 111, 0, 51, 71, 184, 153, 228, 139, 111, 80, 74, 42, 90, 186, 157, 174, 216, 37, 10, 113, 1, 241, 203, 166, 144, 98, 74, 47, 74, 170, 141, 190, 200, 53, 26, 97, 22, 225, 207, 167, 130, 116, 56, 24, 123, 93, 250, 148, 176, 139, 84, 89, 59, 0, 246, 220, 165, 181, 107, 71, 57, 13, 233, 198, 172, 193, 117, 67, 49, 17, 241, 204, 250 }, 234) +
            UrlObfuscator.decode(new int[] { 141, 123, 75, 120, 5, 171, 212, 250, 148, 119, 69, 18, 0, 251, 195, 168, 130, 100, 78, 11, 11, 239, 192, 170, 151, 80, 68, 35, 43, 86, 180, 135 }, 251) +
            UrlObfuscator.decode(new int[] { 122, 74, 56, 73, 254, 209, 251, 146, 109, 77, 38, 14, 247, 177, 200, 180, 143, 110, 91, 53, 46, 254, 211, 162, 132, 124, 64, 37, 75, 249, 207, 191, 204, 125, 66, 116, 30, 241, 153, 179, 146, 45, 74, 36, 9, 24, 246, 201, 230, 140, 115, 87, 60, 24, 225, 155, 189, 157, 124, 84, 34, 39, 235, 196, 171, 131, 126, 18 }, 268) +
            UrlObfuscator.decode(new int[] { 107, 93, 41, 90, 233, 217, 179, 203, 36, 2, 104, 4, 240, 194, 239, 138, 104, 64, 63, 11, 180, 152, 252 }, 285) +
            UrlObfuscator.decode(new int[] { 90, 52, 89, 226, 129, 172, 130, 120, 95, 37, 4, 182, 209, 174, 200, 116, 66, 38, 72, 228, 250, 210, 169, 157, 38, 119, 56, 12, 255, 152, 184, 157, 125, 26, 35, 94, 237, 193, 185, 152, 100, 71, 100, 64, 241, 206, 232, 148, 98, 70, 104, 76, 50, 255, 201, 180, 213, 119, 88, 32, 95, 164, 129, 248, 133, 122, 27, 96, 65, 191, 149, 229, 194, 49 }, 51) +
            UrlObfuscator.decode(new int[] { 33, 15, 241, 196, 224, 182, 152, 53, 78, 117, 14, 246, 200, 235, 134, 116, 80, 122, 22, 244, 220, 187, 143, 48, 1, 6, 11, 253, 192, 233, 139, 108, 74, 107, 18, 224, 196, 146, 172, 211, 104, 84, 42, 85, 213, 214, 162, 157, 58, 94, 51, 9, 184, 158, 248, 193, 122, 67, 96, 89, 166, 150, 246, 204, 45, 24 }, 68) +
            UrlObfuscator.decode(new int[] { 60, 18, 187, 214, 180, 156, 123, 79, 100, 23, 253, 203, 187, 200, 116, 69, 120, 5, 173, 193, 173, 143, 140, 123, 78, 40, 83, 189, 151, 187, 152, 120, 65, 49, 29, 230, 157, 254, 155, 111, 79, 28, 10, 228, 204, 164, 203, 40, 74, 52, 23, 235, 206, 174, 172, 211, 126, 83, 53, 14, 252, 214, 163, 218, 78, 80, 50, 6, 240, 157, 188, 141, 127, 67, 39, 6, 164, 203, 168, 136, 113, 69, 42, 12, 228, 210, 226, 249, 212, 39 }, 85) +
            UrlObfuscator.decode(new int[] { 15, 227, 140, 176, 129, 39, 6, 76, 61, 83, 239, 216, 168, 150, 116, 91, 30, 16, 253, 212, 186, 133, 46, 92, 45, 67, 239, 199, 163, 140, 102, 83, 14, 0, 237, 196, 170, 149, 41, 100, 77, 62, 82, 232, 217, 171, 151, 123, 90, 23, 13, 187, 201, 165, 159, 127, 20, 41, 9, 231, 222, 168, 196, 101, 67, 45, 5, 245, 203, 174, 146, 197, 57, 92, 41, 15, 245, 158, 165, 222, 45, 72 }, 102) +
            UrlObfuscator.decode(new int[] { 18, 250, 198, 177, 136, 101, 88, 62, 11, 225, 218, 226, 152, 105, 91, 39, 11, 234, 231, 189, 203, 121, 85, 47, 47, 68, 249, 217, 183, 142, 120, 20, 53, 19, 253, 213, 165, 155, 126, 66, 117, 73, 236, 217, 191, 133, 46, 85, 110, 93, 248, 217 }, 119) +
            UrlObfuscator.decode(new int[] { 245, 196, 167, 145, 103, 75, 106, 4, 169, 228, 195, 160, 213, 51, 19, 98 }, 136), null);
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
            UrlObfuscator.decode(new int[] { 177, 222, 162, 152, 118, 64, 58, 29, 255, 152, 230, 149, 121, 94, 50, 17, 255, 201, 181, 198, 100, 25, 39, 13, 226, 213, 210, 187, 147, 104, 21, 59, 26, 236, 222, 160, 144, 81, 95, 55, 28, 245, 193, 186, 214, 101, 77, 98, 72, 233, 219, 186, 132, 57, 30, 127, 5, 239, 252, 203, 176, 153, 117, 78, 119, 26, 248, 210, 172, 221, 97, 87, 37, 5, 253, 192, 237, 193, 58, 17 }, 153) +
            UrlObfuscator.decode(new int[] { 220, 168, 154, 39, 82, 120, 76, 226, 140, 181, 129, 152, 80, 92, 49, 30, 230, 197, 255, 208, 63, 27, 32, 28, 222, 222, 167, 138, 124, 110, 45, 24, 239, 129, 225, 220 }, 170) +
            UrlObfuscator.decode(new int[] { 210, 188, 209, 108, 22, 107, 72, 179, 218, 188, 129, 101, 91, 105, 75, 170, 223, 235, 212, 53, 0, 50, 0, 252, 215, 163, 147, 101, 126, 25, 123, 90, 239, 155, 228, 197, 48, 69, 48, 24, 246, 209, 165, 215, 41, 8, 108, 13, 165, 195, 186, 171, 104, 72, 49, 1, 237, 214, 132, 132, 150, 106, 92, 62, 23, 255, 144, 170, 146, 98, 64, 38, 29, 178, 156, 225, 212 }, 187) +
            UrlObfuscator.decode(new int[] { 186, 138, 120, 9, 58, 90, 231, 139, 163, 134, 118, 99, 47, 42, 16, 249, 213, 181, 157, 90, 84, 62, 19, 251, 192, 129, 151, 114, 68, 103, 71, 182, 222, 174, 158, 124, 90, 41, 70, 200, 197, 183, 138, 47, 82, 80, 43, 19, 248, 147, 168, 215, 122, 88, 34, 1, 251, 222, 248, 192, 32, 31, 103, 66, 189, 155, 250, 210, 117, 68, 39, 17, 231, 203, 234, 132, 41, 100, 76, 56, 8, 238, 200, 183, 216, 58, 7, 110, 9, 238, 155, 249, 217, 52 }, 204),
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
        int id = getResources().getIdentifier(UrlObfuscator.decode(new int[] { 174, 136, 122, 78, 44, 11, 200, 212, 180, 134, 76, 90, 52, 25, 232, 198, 185 }, 221), UrlObfuscator.decode(new int[] { 138, 100, 65, 46, 4 }, 238), UrlObfuscator.decode(new int[] { 158, 112, 89, 46, 20, 243, 221 }, 255));
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
        if (t.isEmpty() || UrlObfuscator.decode(new int[] { 126, 90, 34, 1 }, 272).equals(t)) return null;
        try {
            if (t.startsWith("#")) return Color.parseColor(t) | 0xFF000000;
            if (!t.startsWith(UrlObfuscator.decode(new int[] { 83, 39, 61 }, 289))) return null;
            String inner = t.substring(t.indexOf('(') + 1);
            String[] parts = inner.split(UrlObfuscator.decode(new int[] { 108, 8, 69, 185, 138, 252, 172, 59 }, 55));
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
        int scrim = Color.parseColor(UrlObfuscator.decode(new int[] { 107, 86, 178, 151, 240, 214, 65 }, 72));
        try {
            if (topScrim != null && topScrim.getBackground() instanceof ColorDrawable) {
                scrim = ((ColorDrawable) topScrim.getBackground()).getColor() | 0xFF000000;
            }
        } catch (Exception ignored) {}
        Integer bgC = o == null ? null : parseCssColor(o.optString("bg", null));
        Integer thC = o == null ? null : parseCssColor(o.optString(UrlObfuscator.decode(new int[] { 45, 16, 242, 219, 176 }, 89), null));
        Integer btC = o == null ? null : parseCssColor(o.optString(UrlObfuscator.decode(new int[] { 8, 253, 198 }, 106), null));
        double btnR = o == null ? -1 : o.optDouble(UrlObfuscator.decode(new int[] { 25, 238, 215, 138 }, 123), -1);

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
                accent = dark ? Color.parseColor(UrlObfuscator.decode(new int[] { 175, 237, 248, 175, 58, 97, 113 }, 140)) : Color.parseColor(UrlObfuscator.decode(new int[] { 190, 141, 236, 203, 46, 9, 20 }, 157));
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
        p.title = cardDark ? Color.WHITE : Color.parseColor(UrlObfuscator.decode(new int[] { 141, 252, 216, 58, 30, 120, 41 }, 174));
        p.sub = mixColor(p.title, p.cardTop, 0.35f);
        p.border = mixColor(accent, p.cardTop, 0.45f);
        p.accentTop = mixColor(accent, Color.WHITE, 0.12f);
        p.accentBottom = mixColor(accent, Color.BLACK, 0.12f);
        p.accentText = lumOf(accent) < 0.55 ? Color.WHITE : Color.parseColor(UrlObfuscator.decode(new int[] { 156, 239, 201, 45, 15, 107, 56 }, 191));
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
                cancel.setText(UrlObfuscator.decode(new int[] { 147, 174, 64, 110, 9, 39 }, 208));
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
            boolean chat = h.equals(UrlObfuscator.decode(new int[] { 149, 46, 114, 91 }, 225)) || h.endsWith(UrlObfuscator.decode(new int[] { 220, 101, 30, 34, 11 }, 242))
                || h.equals(UrlObfuscator.decode(new int[] { 119, 71, 45, 5, 24, 236, 220, 177, 213, 119, 92 }, 259)) || h.equals(UrlObfuscator.decode(new int[] { 96, 86, 62, 20, 247, 221, 175, 128, 34, 79, 37, 14 }, 276))
                || h.equals(UrlObfuscator.decode(new int[] { 82, 37, 77, 239, 196 }, 293)) || h.equals(UrlObfuscator.decode(new int[] { 90, 42, 16, 182, 192, 190, 148, 96, 64, 51, 1, 224, 129, 173, 130, 97 }, 59));
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
        if (scheme.equals(UrlObfuscator.decode(new int[] { 36, 31, 254, 217 }, 76)) || scheme.equals(UrlObfuscator.decode(new int[] { 53, 8, 239, 202, 170 }, 93)) || scheme.equals(UrlObfuscator.decode(new int[] { 8, 228, 192, 174 }, 110))
                || scheme.equals(UrlObfuscator.decode(new int[] { 30, 252, 210, 169, 143 }, 127)) || scheme.equals(UrlObfuscator.decode(new int[] { 244, 206, 186, 140 }, 144)) || scheme.equals(UrlObfuscator.decode(new int[] { 195, 172, 176, 156 }, 161))
                || scheme.equals(UrlObfuscator.decode(new int[] { 216, 176, 134, 110, 93, 46, 30, 226, 218, 189 }, 178)) || scheme.equals(UrlObfuscator.decode(new int[] { 160, 141, 111, 84, 90, 48, 9 }, 195))) {
            return false;
        }
        try {
            Intent intent;
            if (scheme.equals(UrlObfuscator.decode(new int[] { 189, 157, 102, 84, 62, 27 }, 212))) {
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
                if (scheme.equals(UrlObfuscator.decode(new int[] { 140, 106, 87, 39, 15, 244 }, 229))) {
                    Intent parsed = Intent.parseUri(uri.toString(), Intent.URI_INTENT_SCHEME);
                    String fallback = parsed.getStringExtra(UrlObfuscator.decode(new int[] { 148, 103, 91, 36, 1, 244, 194, 144, 136, 108, 64, 39, 8, 232, 203, 172, 185, 112, 86, 47 }, 246));
                    if (fallback != null && (fallback.startsWith(UrlObfuscator.decode(new int[] { 111, 82, 49, 20, 240, 152, 238, 207 }, 263)) || fallback.startsWith(UrlObfuscator.decode(new int[] { 112, 67, 34, 5, 174, 156, 253 }, 280)))) {
                        view.loadUrl(fallback);
                        return true;
                    }
                }
            } catch (Exception ignored) {}
            Toast.makeText(this, UrlObfuscator.decode(new int[] { 103, 39, 71, 231, 213, 180, 195, 100, 78, 53, 49, 26, 189, 200, 180, 218, 118, 72, 50, 24, 181, 192, 187, 155, 98, 16, 35, 7, 227, 199 }, 297), Toast.LENGTH_SHORT).show();
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
        String name = raw == null ? "" : raw.replaceAll("[" + (char) 92 + (char) 92 + UrlObfuscator.decode(new int[] { 16, 100, 87, 163 }, 63) + (char) 34 + UrlObfuscator.decode(new int[] { 108, 81, 242, 240 }, 80), "_").trim();
        if (name.length() > 120) name = name.substring(name.length() - 120);
        if (name.isEmpty() || name.equals(".") || name.equals("..")) name = UrlObfuscator.decode(new int[] { 5, 239, 232, 208, 177, 147, 122, 94 }, 97);
        if (name.lastIndexOf('.') <= 0 && mime != null && !mime.isEmpty()) {
            String ext = android.webkit.MimeTypeMap.getSingleton().getExtensionFromMimeType(mime);
            if (ext != null) name = name + "." + ext;
        }
        return name;
    }

    private void handleInlineDownload(String url, String contentDisposition, String mimeType) {
        try {
            String named = downloadNames.get(downloadKey(url));
            if (url.startsWith(UrlObfuscator.decode(new int[] { 22, 240, 196, 174, 212 }, 114))) {
                int comma = url.indexOf(',');
                if (comma < 0) throw new IllegalArgumentException(UrlObfuscator.decode(new int[] { 225, 195, 165, 192, 155, 127, 73, 61, 91, 207, 235, 148 }, 131));
                String meta = url.substring(5, comma);
                String payload = url.substring(comma + 1);
                String mime = meta.split(";")[0];
                if (mime.isEmpty()) mime = (mimeType != null && !mimeType.isEmpty()) ? mimeType : UrlObfuscator.decode(new int[] { 224, 214, 170, 133, 63, 95, 34, 12, 229, 197 }, 148);
                byte[] bytes;
                if (meta.contains(UrlObfuscator.decode(new int[] { 158, 166, 130, 113, 68, 118, 107 }, 165))) {
                    if (payload.indexOf('%') >= 0) payload = Uri.decode(payload);
                    bytes = android.util.Base64.decode(payload, android.util.Base64.DEFAULT);
                } else {
                    bytes = Uri.decode(payload).getBytes(UrlObfuscator.decode(new int[] { 227, 129, 178, 62, 10 }, 182));
                }
                String name = cleanDownloadName(named != null ? named : URLUtil.guessFileName(url, contentDisposition, mime), mime);
                File tmp = File.createTempFile("dl", UrlObfuscator.decode(new int[] { 233, 146, 104, 84 }, 199), getCacheDir());
                java.io.FileOutputStream fos = new java.io.FileOutputStream(tmp);
                try { fos.write(bytes); } finally { fos.close(); }
                finishInlineSave(tmp, name, mime);
                return;
            }
            // blob: -- read it inside the page and stream it back in chunks.
            String mime = (mimeType != null && !mimeType.isEmpty()) ? mimeType : "";
            String name = cleanDownloadName(named != null ? named : URLUtil.guessFileName(url, contentDisposition, mime), mime);
            Toast.makeText(this, UrlObfuscator.decode(new int[] { 156, 152, 97, 91, 56, 28, 243, 213, 185, 129, 105, 13 }, 216) + name + "\u2026", Toast.LENGTH_SHORT).show();
            final String js = UrlObfuscator.decode(new int[] { 193, 110, 82, 40, 6, 240, 202, 173, 143, 40, 106, 18, 51, 80, 246, 147, 162, 140, 101, 79, 46 }, 233)
                + UrlObfuscator.decode(new int[] { 140, 120, 74, 119, 14, 168, 218, 182, 133, 49, 104, 2, 34, 197, 216, 191, 154, 91, 77, 54, 19, 224, 215, 183, 202, 40, 27, 71, 112, 18, 236, 222, 180, 209, 63, 112, 19, 33, 179, 159, 167, 221, 100, 93, 59, 8, 165, 144, 178, 199, 122, 66, 53, 21, 235, 205, 177, 132, 84, 102, 78, 56, 65, 188, 216, 181, 151, 117, 17, 110 }, 250)
                + UrlObfuscator.decode(new int[] { 115, 4, 38, 6, 226, 212, 183, 139, 113, 31, 39, 21, 17, 253, 201, 181, 148, 116, 17, 113, 12, 215, 219, 176, 129, 125, 88, 52, 45, 252, 196, 168, 140, 111, 7, 42, 11, 233, 199, 151, 130, 116, 68, 6, 62, 23, 241, 217, 191, 210, 48, 3, 42, 77 }, 267)
                + UrlObfuscator.decode(new int[] { 100, 21, 53, 23, 244, 216, 183, 145, 41, 85, 39, 31, 243, 219, 167, 130, 98, 3, 99, 18, 252, 213, 191, 158, 114, 66, 48, 65, 226, 162, 198, 243, 142, 126, 73, 41, 23, 249, 197, 176, 216, 80, 122, 108, 71, 183, 152, 249, 223, 57, 6, 38, 14, 225, 155, 245, 223 }, 284)
                + UrlObfuscator.decode(new int[] { 115, 63, 20, 253, 193, 164, 136, 73, 88, 32, 12, 224, 195, 235, 134, 111, 77, 35, 51, 30, 232, 216, 158, 158, 125, 80, 54, 95, 248, 153, 185, 143, 110, 83, 126, 27, 247, 221, 169, 151, 118, 14, 111, 78, 189 }, 50)
                + UrlObfuscator.decode(new int[] { 107, 4, 244, 206, 220, 170, 148, 115, 85, 122, 23, 253, 207, 162, 221, 61, 72, 59, 23, 184, 192, 168, 139, 50, 22, 40, 71, 251, 206, 188, 128, 45, 88, 3, 15, 228, 237, 209, 180, 152, 89, 72, 48, 28, 240, 211, 251, 150, 127, 93, 51, 35, 238, 216, 168, 169, 101, 78, 97, 65, 188, 212, 160, 144, 118, 80, 47, 91, 2 }, 67)
                + UrlObfuscator.decode(new int[] { 34, 18, 224, 145, 162, 210, 96, 72, 59, 75, 204, 192, 164, 130, 84, 64, 37, 7, 231, 211, 232, 246, 197 }, 84)
                + UrlObfuscator.decode(new int[] { 23, 170, 204, 172, 141, 111, 126, 90, 96, 26, 238, 212, 186, 140, 126, 89, 59, 92, 186, 201, 167, 145, 125, 14, 62, 81, 216, 222, 187, 129, 105, 65, 109, 22, 173, 208, 164, 147, 138, 114, 73, 117, 64, 219, 215, 188, 133, 121, 92, 48, 49, 224, 216, 180, 136, 107, 3, 46, 7, 229, 203, 155, 134, 112, 64, 7, 11, 247, 207, 171, 247, 141, 51, 79, 46, 24, 234, 204, 165, 159, 123, 83, 123, 1, 191, 217, 161, 138, 104, 84, 4, 12, 161, 143, 235, 193, 44, 15, 114, 75, 168, 155, 208, 184, 155, 55, 6, 25, 49, 163, 217, 179, 141, 96, 27, 123, 74, 237, 148 }, 101)
                + UrlObfuscator.decode(new int[] { 4, 187, 219, 189, 151, 99, 66, 32, 28, 176, 202, 190, 132, 106, 92, 46, 9, 235, 140, 234, 153, 64, 78, 91, 44, 18, 245, 223, 152, 139, 113, 83, 49, 16, 186, 209, 190, 158, 114, 124, 47, 27, 233, 237, 171, 128, 100, 66, 34, 77, 173, 152, 191, 218 }, 118)
                + UrlObfuscator.decode(new int[] { 245, 136, 183, 129, 98, 70, 0, 19, 59, 255, 201, 189, 174, 72, 117, 112, 21, 184, 198, 184, 154, 113, 84, 120, 0, 232, 203, 224, 132, 108, 79, 99, 36, 206, 140, 237, 216, 127, 8, 104, 118, 69 }, 135)
                + UrlObfuscator.decode(new int[] { 229, 212, 183, 129, 119, 91, 122, 20, 185, 212, 143, 131, 104, 89, 37, 0, 236, 229, 180, 140, 96, 68, 39, 79, 226, 243, 209, 191, 175, 122, 76, 60, 62, 246, 223, 185, 145, 119, 26, 120, 75, 242, 211, 246 }, 152)
                + UrlObfuscator.decode(new int[] { 209, 230, 148, 99, 75, 32, 75, 171, 154 }, 169)
                + UrlObfuscator.decode(new int[] { 199, 186, 153, 99, 85, 61, 92, 246, 155, 170, 132, 125, 87, 54, 45, 229, 206, 187, 135, 110, 66, 7, 22, 234, 198, 166, 133, 209, 124, 81, 51, 25, 201, 216, 174, 146, 80, 84, 61, 31, 247, 213, 248, 198, 53, 80, 47, 10, 254, 202, 160, 207, 89, 12, 63, 30, 255, 220, 233, 247 }, 186)
                + org.json.JSONObject.quote(url) + "," + org.json.JSONObject.quote(name) + "," + org.json.JSONObject.quote(mime) + ");";
            webView.evaluateJavascript(js, null);
        } catch (Exception e) {
            Toast.makeText(this, UrlObfuscator.decode(new int[] { 143, 133, 126, 70, 43, 9, 228, 192, 227, 132, 96, 73, 83, 59, 25, 166, 155, 185, 150, 109, 91, 50, 85, 250, 220, 166, 209, 98, 74, 47, 9, 172, 223, 162, 128, 123, 7, 32, 12, 232, 198 }, 203), Toast.LENGTH_LONG).show();
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
                cv.put(android.provider.MediaStore.MediaColumns.MIME_TYPE, (mime == null || mime.isEmpty()) ? UrlObfuscator.decode(new int[] { 189, 139, 106, 85, 49, 20, 247, 193, 189, 156, 124, 30, 63, 12, 250, 200, 184, 198, 121, 93, 58, 2, 231, 200 }, 220) : mime);
                cv.put(android.provider.MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + UrlObfuscator.decode(new int[] { 194, 72, 68, 33, 8, 230, 206, 160, 156 }, 237));
                cv.put(android.provider.MediaStore.MediaColumns.IS_PENDING, 1);
                android.content.ContentResolver cr = getContentResolver();
                Uri dest = cr.insert(android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI, cv);
                if (dest == null) throw new java.io.IOException(UrlObfuscator.decode(new int[] { 179, 120, 88, 50, 27, 202, 204, 184, 132, 112, 20, 58, 28, 226, 213, 189, 154, 45, 74, 42, 3, 229, 205, 163 }, 254));
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
                File dir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), UrlObfuscator.decode(new int[] { 75, 65, 38, 13, 229, 195, 175, 145 }, 271));
                if (!dir.exists() && !dir.mkdirs()) throw new java.io.IOException(UrlObfuscator.decode(new int[] { 77, 84, 58, 20, 238, 200, 250, 159, 121, 94, 58, 16, 240 }, 288));
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
                ? shown + " downloaded \u2014 find it in your Dokanify folder"
                : UrlObfuscator.decode(new int[] { 114, 58, 3, 253, 222, 190, 145, 107, 14, 43, 13, 226, 198, 172, 140, 61, 6, 38, 11, 246, 206, 165, 192, 145, 113, 73, 124, 8, 251, 207, 189, 215 }, 54) + shown, Toast.LENGTH_LONG).show();
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
    // its own "Dokanify" subfolder in there (DownloadManager
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
        if (url != null && (url.startsWith(UrlObfuscator.decode(new int[] { 37, 10, 234, 198, 249 }, 71)) || url.startsWith(UrlObfuscator.decode(new int[] { 60, 22, 226, 212, 238 }, 88)))) {
            handleInlineDownload(url, contentDisposition, mimeType);
            return;
        }

        final String filename = URLUtil.guessFileName(url, contentDisposition, mimeType);
        final String cookie = CookieManager.getInstance().getCookie(url);

        try {
            DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
            request.addRequestHeader(UrlObfuscator.decode(new int[] { 60, 251, 194, 180, 200, 69, 68, 39, 15, 244 }, 105), userAgent);
            if (cookie != null) request.addRequestHeader(UrlObfuscator.decode(new int[] { 57, 246, 215, 188, 159, 112 }, 122), cookie);
            request.setTitle(filename);
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
            request.setDestinationInExternalPublicDir(
                Environment.DIRECTORY_DOWNLOADS, UrlObfuscator.decode(new int[] { 207, 197, 162, 137, 105, 79, 35, 29, 172 }, 139) + filename);
            request.setAllowedOverMetered(true);
            request.setAllowedOverRoaming(true);
            request.setVisibleInDownloadsUi(true);

            DownloadManager downloadManager = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
            pendingDownloadId = downloadManager.enqueue(request);
            startDownloadProgressPolling(pendingDownloadId);
            Toast.makeText(this, UrlObfuscator.decode(new int[] { 216, 212, 173, 151, 116, 88, 55, 17, 253, 221, 181, 209 }, 156) + filename + "…", Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Toast.makeText(this, UrlObfuscator.decode(new int[] { 233, 163, 156, 100, 69, 39, 6, 226, 133, 162, 130, 107, 77, 37, 59, 94, 176, 145, 251, 153, 113, 93, 52, 29, 181, 205, 188, 135, 99, 16, 44, 1, 227, 194, 174, 137, 125, 65, 40, 8, 165, 197, 173, 134, 33, 84, 77, 39, 93, 253, 220, 187, 144, 118 }, 173), Toast.LENGTH_LONG).show();
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
            String title = (titleIdx >= 0 && cursor.getString(titleIdx) != null) ? cursor.getString(titleIdx) : UrlObfuscator.decode(new int[] { 248, 180, 144, 126 }, 190);

            if (status == DownloadManager.STATUS_SUCCESSFUL) {
                String localUriStr = uriIdx >= 0 ? cursor.getString(uriIdx) : null;
                String folderName = UrlObfuscator.decode(new int[] { 139, 129, 122, 66, 39, 5, 232, 204, 180 }, 207);
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
                Toast.makeText(this, title + UrlObfuscator.decode(new int[] { 192, 155, 113, 74, 50, 23, 245, 216, 188, 146, 114, 21, 8256, 83, 244, 216, 190, 139, 46, 68, 56, 75, 227, 199, 232, 158, 105, 80, 54, 67 }, 224) + folderName + UrlObfuscator.decode(new int[] { 209, 118, 64, 34, 9, 233, 217 }, 241), Toast.LENGTH_LONG).show();
                notifyDownloadResult(true);

                // If what just finished downloading is itself an installable
                // Android package, go straight to the system installer
                // instead of leaving the user to dig it out of the Downloads
                // folder and tap it themselves. Gated strictly on the file
                // actually being a .apk -- every other kind of download this
                // wrapper handles (images, PDFs, whatever the wrapped site
                // links to) still behaves exactly as before, since "install"
                // wouldn't mean anything for those.
                if (localPath != null && title != null && title.toLowerCase().endsWith(UrlObfuscator.decode(new int[] { 44, 64, 48, 52 }, 258))) {
                    File apkFile = new File(localPath);
                    if (apkFile.exists()) {
                        try {
                            Uri contentUri = FileProvider.getUriForFile(
                                this, getPackageName() + UrlObfuscator.decode(new int[] { 61, 84, 56, 28, 234, 222, 191, 131, 125, 67, 45, 13, 245 }, 275), apkFile);
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
                Toast.makeText(this, UrlObfuscator.decode(new int[] { 96, 44, 21, 239, 204, 208, 191, 153, 60, 93, 59, 16, 244, 210, 178, 207, 52 }, 292) + title + UrlObfuscator.decode(new int[] { 26, 113, 29, 229, 196, 186, 134, 51 }, 58) + reason + ")", Toast.LENGTH_LONG).show();
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
                    UrlObfuscator.decode(new int[] { 63, 24, 240, 211, 174, 128, 45, 83, 42, 12, 229, 207, 200, 240, 162, 67, 84, 52, 55, 249, 195, 191, 131, 113, 119, 61, 6, 254, 195, 161, 140, 104, 104, 37, 4, 248, 203, 163, 145, 97, 10, 57, 22, 233, 241, 218, 178, 139, 53, 101, 6, 23, 249, 248, 180, 128, 122, 68, 52, 52, 224, 217, 163, 128, 100, 75, 45, 43, 232, 203, 181, 136, 102, 86, 36, 72 }, 75) + success + UrlObfuscator.decode(new int[] { 117, 64, 231, 196, 187, 150, 98, 86, 60, 91, 247, 152, 171, 146 }, 92),
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
                UrlObfuscator.decode(new int[] { 25, 254, 210, 177, 128, 110, 15, 49, 12, 234, 199, 173, 150, 46, 64, 97, 50, 18, 213, 219, 173, 145, 97, 83, 17, 27, 228, 220, 189, 159, 110, 74, 29, 30, 228, 205, 187, 141, 116, 85, 108, 31, 244, 203, 175, 132, 144, 105, 19, 3, 36, 245, 215, 150, 150, 98, 92, 34, 22, 214, 222, 167, 129, 98, 66, 45, 15, 218, 219, 167, 128, 116, 64, 55, 16, 170 }, 109) + done + "," + total + "," + status + UrlObfuscator.decode(new int[] { 87, 166, 193, 166, 153, 120, 76, 52, 30, 189, 209, 250, 137, 108 }, 126),
                null);
        } catch (Exception ignored) {
        }
    }

    private void notifyDownloadCancelled() {
        try {
            webView.evaluateJavascript(
                UrlObfuscator.decode(new int[] { 251, 220, 180, 151, 98, 76, 97, 31, 238, 200, 161, 139, 116, 12, 30, 63, 16, 240, 243, 189, 143, 115, 79, 61, 51, 249, 194, 186, 159, 125, 80, 52, 44, 225, 192, 188, 135, 111, 93, 45, 78, 253, 210, 173, 141, 102, 78, 55, 113, 33, 194, 211, 181, 180, 120, 76, 62, 0, 240, 240, 188, 133, 127, 92, 32, 15, 233, 239, 164, 135, 121, 68, 34, 18, 224, 140, 165, 131, 109, 83, 90, 114, 90, 255, 218, 180, 154, 125, 91, 58, 16, 240, 148, 251, 202, 109, 82, 45, 12, 248, 200, 162, 193, 109, 14, 61, 24 }, 143),
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
            Toast.makeText(this, UrlObfuscator.decode(new int[] { 225, 211, 178, 146, 107, 27, 51, 23, 235, 195, 183, 153, 120, 64, 114, 23, 226, 192, 163, 205, 120, 67, 35, 26, 168, 198, 182, 149, 40, 3, 54, 9, 229, 241, 158, 180, 136, 60, 86, 53, 88, 244, 217, 187, 128, 122, 92, 36, 21, 175, 207, 184, 152, 100, 71, 40, 28, 238, 197, 164, 136, 111, 91 }, 160), Toast.LENGTH_LONG).show();
            try {
                Intent settingsIntent = new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                    Uri.parse(UrlObfuscator.decode(new int[] { 193, 177, 140, 101, 76, 43, 14, 176 }, 177) + getPackageName()));
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
        installIntent.setDataAndType(contentUri, UrlObfuscator.decode(new int[] { 163, 145, 112, 115, 87, 62, 29, 239, 211, 182, 150, 56, 64, 59, 16, 189, 211, 191, 148, 125, 65, 36, 8, 165, 218, 168, 139, 108, 71, 34, 1, 174, 195, 179, 131, 151, 119, 75, 57 }, 194));
        installIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_GRANT_READ_URI_PERMISSION);
        try {
            startActivity(installIntent);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, UrlObfuscator.decode(new int[] { 157, 157, 49, 89, 33, 29, 249, 205, 167, 134, 108, 90, 103, 0, 234, 209, 173, 134, 33, 13, 18, 126, 18, 236, 222, 180, 217, 108, 95, 51, 85, 242, 218, 190, 148, 48, 73, 60, 2, 225, 139, 179, 134, 125, 85, 102, 33, 235, 212, 172, 141, 111, 126, 90, 46, 92, 253, 213, 181, 156, 114, 68, 117, 29, 253, 193, 165, 149, 110, 74 }, 211), Toast.LENGTH_LONG).show();
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
                Toast.makeText(this, UrlObfuscator.decode(new int[] { 183, 119, 77, 51, 1, 24, 251, 157, 172, 158, 104, 84, 49, 4, 229, 220, 187, 157, 50, 88, 35, 79, 224, 200, 169, 143, 111, 77, 104, 19, 233, 133, 183, 130, 116, 68, 96, 43, 22, 248, 156, 191, 149, 110, 86, 59, 25, 244, 208 }, 228), Toast.LENGTH_LONG).show();
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
                Toast.makeText(this, UrlObfuscator.decode(new int[] { 182, 117, 94, 55, 3, 241, 128, 163, 132, 111, 89, 37, 25, 224, 200, 168, 128, 36, 83, 39, 19, 237, 246, 205, 174, 149, 116, 84, 121, 17, 228, 150, 187, 145, 118, 86, 52, 20, 175, 200, 162, 158, 43, 94, 33, 1, 244 }, 245), Toast.LENGTH_LONG).show();
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
                Toast.makeText(this, UrlObfuscator.decode(new int[] { 74, 74, 39, 2, 246, 200, 175, 177, 222, 109, 89, 41, 23, 240, 203, 164, 159, 122, 90, 115, 27, 226, 144, 161, 139, 104, 72, 46, 14, 169, 206, 168, 148, 37, 80, 43, 11, 242 }, 262), Toast.LENGTH_LONG).show();
            }
        } else if (requestCode == NOTIFICATION_PERMISSION_REQUEST_CODE) {
            if (!(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                Toast.makeText(this, UrlObfuscator.decode(new int[] { 89, 89, 33, 29, 245, 219, 178, 145, 123, 71, 34, 2, 248, 138, 168, 154, 98, 6, 42, 2, 229, 130, 167, 143, 141, 62, 73, 52, 18, 233, 153, 185, 135, 102, 21, 121, 94, 178, 197, 165, 157, 96, 13, 56, 3, 239, 196, 232, 136, 104, 5, 45, 13, 162, 242, 165, 171, 138, 116, 82, 60, 9, 185, 217, 185, 143, 53, 64, 58, 31, 244 }, 279), Toast.LENGTH_LONG).show();
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
            UrlObfuscator.decode(new int[] { 76, 34, 0, 228, 209, 175, 150 }, 296), UrlObfuscator.decode(new int[] { 121, 56, 18, 254, 200, 184, 148 }, 62), NotificationManager.IMPORTANCE_DEFAULT);
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
                Toast.makeText(this, UrlObfuscator.decode(new int[] { 6, 0, 254, 216, 170, 134, 101, 8, 55, 3, 247, 201, 170, 145, 114, 73, 80, 48, 93, 235, 218, 169, 151, 63, 67, 118, 18, 230, 210, 188, 133, 117, 75, 110, 64, 161, 139, 165, 153, 109, 73, 102, 17, 236, 198, 226, 135, 105, 115, 91, 125, 26, 233, 213, 180, 216, 110, 89, 32, 6, 179, 246, 190, 135, 97, 66, 34, 13, 239, 217, 233, 142, 104, 74, 33, 1, 241, 130, 181, 143, 223, 119, 83, 47, 15, 251, 213, 180, 215, 123, 84, 58, 6, 243, 221, 188, 150 }, 79), Toast.LENGTH_LONG).show();
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
    private static final String ORIENTATION_MODE = UrlObfuscator.decode(new int[] { 16, 16, 236, 201, 174, 154, 115, 77 }, 96);

    // Landscape-locked builds are always immersive; auto-rotate builds are
    // immersive only while the phone is actually sideways; portrait never.
    private boolean shouldBeImmersive() {
        if (UrlObfuscator.decode(new int[] { 29, 241, 193, 170, 158, 111, 74, 58, 12 }, 113).equals(ORIENTATION_MODE)) return true;
        if (UrlObfuscator.decode(new int[] { 242, 206, 178, 171, 140, 124, 85, 47 }, 130).equals(ORIENTATION_MODE)) return false;
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
