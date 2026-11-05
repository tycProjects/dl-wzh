package com.khalifatopup;

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
import com.google.firebase.messaging.FirebaseMessaging;
import android.os.CancellationSignal;
import androidx.core.content.ContextCompat;
import androidx.credentials.Credential;
import androidx.credentials.CredentialManager;
import androidx.credentials.CustomCredential;
import androidx.credentials.GetCredentialRequest;
import androidx.credentials.GetCredentialResponse;
import androidx.credentials.exceptions.GetCredentialException;
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption;
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential;
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException;
import org.json.JSONObject;

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
    // The project's OAuth Web client ID, pulled from google-services.json
    // at build time (see googleWebClientId in server.js) -- Credential
    // Manager needs this to know which Firebase/Google Cloud project to
    // ask for an ID token against; it is NOT a secret (it's the same ID
    // that ships inside any public web page's Firebase config) so baking
    // it in as a literal here is the same trust level as the rest of
    // google-services.json already being bundled into this build.
    private static final String GOOGLE_WEB_CLIENT_ID = UrlObfuscator.decode(new int[] { 97, 74, 160, 131, 228, 193, 42, 11, 96, 68, 190, 158, 224, 212, 98, 79, 45, 81, 234, 145, 161, 140, 108, 75, 32, 81, 13, 253, 137, 174, 151, 42, 81, 48, 68, 240, 216, 183, 148, 126, 94, 50, 14, 184, 154, 226, 138, 122, 89, 59, 73, 225, 202, 171, 132, 110, 68, 53, 44, 27, 239, 223, 180, 148, 109, 93, 57, 2, 187, 215, 188, 159 }, 89);
    private CredentialManager credentialManager;

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
            line.setColor(Color.parseColor(UrlObfuscator.decode(new int[] { 73, 185, 234, 246, 209, 49, 20 }, 106)));
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
            int fur = Color.parseColor(UrlObfuscator.decode(new int[] { 88, 220, 141, 158, 194, 80, 119 }, 123));
            int pink = Color.parseColor(UrlObfuscator.decode(new int[] { 175, 237, 254, 168, 59, 100, 118 }, 140));
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
            line.setColor(Color.parseColor(UrlObfuscator.decode(new int[] { 190, 253, 158, 184, 33, 125, 103 }, 157)));
            c.drawLine(22, 47, 9, 44, line);
            c.drawLine(22, 52, 9, 53, line);
            c.drawLine(78, 47, 91, 44, line);
            c.drawLine(78, 52, 91, 53, line);
            line.setColor(Color.parseColor(UrlObfuscator.decode(new int[] { 141, 253, 174, 58, 29, 125, 88 }, 174)));

            fill.setColor(Color.parseColor(UrlObfuscator.decode(new int[] { 156, 239, 201, 46, 15, 111, 58 }, 191)));
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
            shape(c, tear, Color.parseColor(UrlObfuscator.decode(new int[] { 243, 215, 75, 105, 116, 45, 178 }, 208)));

            android.graphics.Path nose = new android.graphics.Path();
            nose.moveTo(47.5f, 48f); nose.lineTo(52.5f, 48f); nose.lineTo(50f, 51f); nose.close();
            fill.setColor(Color.parseColor(UrlObfuscator.decode(new int[] { 194, 69, 39, 9, 104, 69, 222 }, 225))); c.drawPath(nose, fill);

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
            paint.setColor(Color.parseColor(UrlObfuscator.decode(new int[] { 209, 87, 2, 9, 92, 200, 233 }, 242)));
            canvas.drawCircle(cx, cy, stroke * 1.05f, paint);

            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(stroke);
            paint.setColor(Color.parseColor(UrlObfuscator.decode(new int[] { 32, 18, 4, 33, 74, 219, 132 }, 259)));
            float[] radii = {w * 0.24f, w * 0.38f};
            int[] alphas = {235, 130};
            for (int i = 0; i < radii.length; i++) {
                paint.setAlpha(alphas[i]);
                RectF arc = new RectF(cx - radii[i], cy - radii[i], cx + radii[i], cy + radii[i]);
                canvas.drawArc(arc, 208, 124, false, paint);
            }

            paint.setAlpha(255);
            paint.setColor(Color.parseColor(UrlObfuscator.decode(new int[] { 55, 117, 20, 71, 210, 154, 249 }, 276)));
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
            String name = (appName == null || appName.trim().isEmpty()) ? UrlObfuscator.decode(new int[] { 100, 52, 19 }, 293) : appName.trim().toUpperCase();

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
            letter.setTypeface(Typeface.create(UrlObfuscator.decode(new int[] { 72, 59, 23, 235, 154, 165, 144, 102, 90, 52, 92, 253, 202, 170, 132, 121, 70 }, 59), Typeface.NORMAL));
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
                videoResId = context.getResources().getIdentifier(UrlObfuscator.decode(new int[] { 47, 30, 249, 221, 167, 138, 89, 86, 52, 15, 227, 210, 168 }, 76), UrlObfuscator.decode(new int[] { 47, 29, 236 }, 93), pkg);
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
                int resId = context.getResources().getIdentifier(UrlObfuscator.decode(new int[] { 13, 248, 223, 191, 133, 100, 119, 52, 22, 233, 197, 176, 138 }, 110), UrlObfuscator.decode(new int[] { 27, 236, 220, 171, 154, 120, 85, 61 }, 127), pkg);
                if (resId != 0) {
                    iv.setImageURI(Uri.parse(UrlObfuscator.decode(new int[] { 241, 193, 170, 159, 99, 66, 46, 71, 250, 194, 181, 138, 113, 81, 33, 4, 186, 176, 145 }, 144) + pkg + "/" + resId));
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
                mp.setDataSource(context, Uri.parse(UrlObfuscator.decode(new int[] { 192, 174, 187, 140, 114, 85, 63, 84, 235, 221, 164, 153, 96, 70, 48, 23, 171, 159, 224 }, 161) + pkg + "/" + videoResId));
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
            int iconResId = context.getResources().getIdentifier(UrlObfuscator.decode(new int[] { 197, 171, 152, 80, 93, 61, 0, 234, 217, 161, 183, 107, 73, 34, 11 }, 178), UrlObfuscator.decode(new int[] { 167, 144, 96, 87, 94, 60, 17, 249 }, 195), context.getPackageName());
            boolean roundedAsset = iconResId != 0;
            if (iconResId == 0) iconResId = context.getResources().getIdentifier(UrlObfuscator.decode(new int[] { 189, 144, 77, 93, 49, 26, 224, 206, 164, 142, 120 }, 212), UrlObfuscator.decode(new int[] { 136, 109, 83, 47, 0, 240 }, 229), context.getPackageName());
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
            int iconResId = context.getResources().getIdentifier(UrlObfuscator.decode(new int[] { 129, 111, 92, 12, 1, 225, 220, 174, 157, 101, 115, 39, 5, 238, 199 }, 246), UrlObfuscator.decode(new int[] { 99, 84, 36, 19, 226, 192, 173, 133 }, 263), context.getPackageName());
            boolean roundedAsset = iconResId != 0;
            if (iconResId == 0) iconResId = context.getResources().getIdentifier(UrlObfuscator.decode(new int[] { 113, 84, 9, 25, 245, 198, 188, 146, 120, 74, 60 }, 280), UrlObfuscator.decode(new int[] { 68, 33, 23, 235, 196, 180 }, 297), context.getPackageName());
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
        // Logged (see PushMessagingService.onNewToken too) so it's visible
        // via logcat -- a real backend needs this per-install token to be
        // able to target a push at this specific device.
        FirebaseMessaging.getInstance().getToken().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                android.util.Log.d(UrlObfuscator.decode(new int[] { 114, 63, 20, 242, 250, 185, 141, 113, 65, 63, 1, 237 }, 63), UrlObfuscator.decode(new int[] { 22, 44, 195, 141, 184, 132, 97, 76, 38, 93, 166 }, 80) + task.getResult());
            }
        });

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
        root.setBackgroundColor(Color.parseColor(UrlObfuscator.decode(new int[] { 66, 176, 218, 255, 232, 185, 34 }, 97)));

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
        credentialManager = CredentialManager.create(this);

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
        swipeRefresh.setColorSchemeColors(Color.parseColor(UrlObfuscator.decode(new int[] { 81, 161, 245, 142, 219, 72, 21 }, 114)));
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
        leftScrim.setBackgroundColor(Color.parseColor(UrlObfuscator.decode(new int[] { 160, 146, 244, 208, 202, 46, 8 }, 131)));
        root.addView(leftScrim, new FrameLayout.LayoutParams(
            0, FrameLayout.LayoutParams.MATCH_PARENT, Gravity.LEFT));
        rightScrim = new View(this);
        rightScrim.setBackgroundColor(Color.parseColor(UrlObfuscator.decode(new int[] { 183, 131, 231, 193, 37, 31, 123 }, 148)));
        root.addView(rightScrim, new FrameLayout.LayoutParams(
            0, FrameLayout.LayoutParams.MATCH_PARENT, Gravity.RIGHT));
        topScrim = new View(this);
        topScrim.setBackgroundColor(Color.parseColor(UrlObfuscator.decode(new int[] { 134, 244, 214, 50, 20, 112, 106 }, 165)));
        root.addView(topScrim, new FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT, 0, Gravity.TOP));
        bottomScrim = new View(this);
        bottomScrim.setBackgroundColor(Color.parseColor(UrlObfuscator.decode(new int[] { 149, 229, 193, 35, 7, 97, 69 }, 182)));
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
        final String offlineScrimColor = offlineIsNightMode ? UrlObfuscator.decode(new int[] { 228, 223, 60, 20, 115, 82, 177, 144, 143 }, 199) : UrlObfuscator.decode(new int[] { 251, 193, 32, 5, 100, 67, 162, 129, 224 }, 216);
        // Slightly translucent (not fully opaque) card/button fills, so a
        // hint of the dimmed scrim behind still shows through -- a cheap
        // stand-in for a true frosted-glass blur, which would need
        // RenderEffect (API 31+) and a snapshot of whatever's behind the
        // card to do for real.
        final String offlineCardBg = offlineIsNightMode ? UrlObfuscator.decode(new int[] { 202, 77, 31, 116, 38, 182, 230, 241, 209 }, 233) : UrlObfuscator.decode(new int[] { 217, 91, 0, 17, 68, 211, 134, 149, 192 }, 250);
        final String offlineTitleColor = offlineIsNightMode ? UrlObfuscator.decode(new int[] { 40, 108, 15, 46, 193, 224, 131 }, 267) : UrlObfuscator.decode(new int[] { 63, 11, 106, 73, 168, 135, 230 }, 284);
        final String offlineSubtitleColor = offlineIsNightMode ? UrlObfuscator.decode(new int[] { 17, 19, 72, 205, 150, 143, 168 }, 50) : UrlObfuscator.decode(new int[] { 96, 87, 195, 149, 253, 235, 184 }, 67);
        final String offlineButtonBg = offlineIsNightMode ? UrlObfuscator.decode(new int[] { 119, 75, 209, 133, 232, 219, 54, 25, 13 }, 84) : UrlObfuscator.decode(new int[] { 70, 188, 224, 134, 215, 68, 41, 122, 28 }, 101);
        final float offlineDensity = getResources().getDisplayMetrics().density;

        // Outer full-screen scrim so the card reads as a dialog sitting on
        // top of the app, matching how a native "no connection" alert
        // dims everything behind it.
        final FrameLayout errorView = new FrameLayout(this);
        errorView.setBackgroundColor(Color.parseColor(UrlObfuscator.decode(new int[] { 85, 165, 245, 227, 179, 33, 114 }, 118)));
        errorView.setVisibility(View.GONE);

        // The error screen is the branded HTML page in ERROR_PAGE_HTML,
        // rendered in its own small WebView that fills errorView. Its
        // Retry / Back buttons call back into native code through the
        // AndroidError interface (registered below, once doRetry exists).
        // Navigation inside it is blocked so it can only ever show that page.
        final WebView errorWeb = new WebView(this);
        errorWeb.setBackgroundColor(Color.parseColor(UrlObfuscator.decode(new int[] { 164, 150, 132, 212, 66, 18, 3 }, 135)));
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
        navOverlay.setBackgroundColor(Color.parseColor(UrlObfuscator.decode(new int[] { 187, 135, 147, 180, 33, 118, 107 }, 152)));
        navOverlay.setVisibility(View.GONE);
        final SkeletonView navSkeleton = new SkeletonView(this, Color.parseColor(UrlObfuscator.decode(new int[] { 138, 248, 162, 71, 16, 1, 90 }, 169)));
        navSkeleton.setTarget(webView);
        navSkeletonRef = navSkeleton;
        navOverlay.addView(navSkeleton, new FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
        final BouncingDotsView navDots = new BouncingDotsView(this, Color.parseColor(UrlObfuscator.decode(new int[] { 153, 159, 202, 81, 4, 16, 49 }, 186)));
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
                UrlObfuscator.decode(new int[] { 227, 140, 124, 70, 36, 18, 236, 203, 173, 202, 40, 91, 75, 44, 4, 231, 205, 187, 139, 56, 85, 107, 17, 251, 208, 167, 156, 117, 65, 58, 67, 238, 196, 174, 144, 51, 78, 32, 77, 165, 193, 235, 147, 101, 107, 75, 47, 18, 187, 206, 171, 141, 114, 13 }, 203)
                + UrlObfuscator.decode(new int[] { 181, 157, 50, 17, 58, 89, 255, 219, 186, 150, 96, 101, 53, 23, 250, 209, 176, 204, 45, 0, 102, 19, 244, 204, 169, 203, 43, 15, 44, 58, 16, 250, 200, 179, 196, 41, 17, 37, 19, 225, 193, 161, 156, 49, 86, 46, 2, 254, 201, 240 }, 220)
                + UrlObfuscator.decode(new int[] { 159, 105, 95, 63, 27, 230, 135, 164, 203, 117, 86, 39, 19, 249, 204, 219, 177, 153, 120, 78, 54, 10, 214, 218, 185, 220, 52, 91, 60, 23, 163, 216, 164, 136, 110, 69, 101, 11, 230, 200, 179, 133, 112, 14, 50, 22, 24, 178, 212, 186, 137, 123, 84, 61, 91, 255, 219, 164, 134, 102, 29, 50, 26, 250, 217, 163, 133, 45, 0, 102, 11, 227, 203, 163, 151, 106, 28, 125, 98, 78, 166, 193 }, 237)
                + UrlObfuscator.decode(new int[] { 157, 124, 72, 56, 18, 177, 221, 254, 141, 103, 81, 39, 7, 227, 222, 239, 136, 108, 64, 56, 15, 178, 213, 186, 207, 45, 13 }, 254),
                value -> {
                    if (UrlObfuscator.decode(new int[] { 123, 92, 56, 9 }, 271).equals(value)) revealNavOverlay.run();
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
        }, UrlObfuscator.decode(new int[] { 97, 81, 58, 15, 243, 210, 190, 188, 106, 69, 57, 7 }, 288));

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
        String spoofedUA = defaultUA.replace(UrlObfuscator.decode(new int[] { 13, 117, 3, 229 }, 54), "").replaceAll("Version/[0-9.]+\s", "");
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
        webView.addJavascriptInterface(new AndroidBridge(), UrlObfuscator.decode(new int[] { 6, 8, 225, 214, 172, 139, 101, 98, 77, 55, 25, 251, 222 }, 71));

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
                UrlObfuscator.decode(new int[] { 112, 17, 227, 219, 183, 135, 123, 94, 62, 71, 167, 214, 184, 153, 115, 82, 62, 6, 244, 133, 166, 222, 102, 78, 35, 42, 19, 248, 210, 175, 212, 123, 87, 51, 15, 174, 221, 181, 218, 48, 82, 102, 28, 232, 216, 190, 152, 103, 8, 119, 93 }, 88) +
                UrlObfuscator.decode(new int[] { 0, 238, 143, 238, 135, 42, 74, 44, 15, 229, 237, 234, 184, 132, 111, 70, 37, 95, 176, 159, 251, 128, 97, 91, 60, 88, 166, 128, 161, 137, 101, 77, 61, 0, 185, 150, 236, 150, 102, 86, 52, 18, 17, 190, 140, 231 }, 105) +
                UrlObfuscator.decode(new int[] { 12, 248, 202, 247, 135, 40, 86, 125, 3, 228, 213, 189, 151, 94, 73, 39, 15, 234, 220, 168, 148, 68, 72, 47, 74, 166, 201, 210, 185, 209, 106, 82, 62, 28, 247, 155, 181, 148, 122, 69, 51, 2, 188, 220, 184, 138, 32, 66, 44, 27, 233, 202, 163, 201, 109, 77, 50, 20, 244, 179, 220, 168, 136, 111, 85, 55, 95, 190, 141 }, 122) +
                UrlObfuscator.decode(new int[] { 237, 197, 187, 192, 113, 71, 55, 68, 234, 159, 241, 219, 150, 34, 76, 114, 23, 255, 215, 191, 131, 126, 19, 114, 26, 174, 133, 224, 212, 103, 6, 103, 66, 241, 223, 169, 149, 38, 87, 121, 18, 217, 200, 157, 241, 153, 120, 72, 25, 21, 236, 214, 179, 159, 123, 83, 16, 30, 248, 213, 161, 154, 95, 73, 40, 30, 161, 129, 252 }, 139) +
                UrlObfuscator.decode(new int[] { 245, 221, 242, 139, 54, 64, 63, 17, 224, 219, 236, 204, 40, 9, 104, 31, 162, 195, 175, 128, 111, 79, 50, 91, 185, 155, 235, 147, 101, 107, 75, 47, 18, 187, 139, 226, 133, 101, 83, 33, 1, 225, 220, 241, 192, 52, 83, 46, 13, 255, 201, 161, 192, 98, 15, 62, 22, 230, 214, 180, 146, 145, 62, 12, 103, 6, 231, 144, 240, 222 }, 156),
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
                if (false) {
                    view.evaluateJavascript(
                        UrlObfuscator.decode(new int[] { 217, 190, 146, 113, 64, 46, 79, 241, 204, 170, 135, 109, 86, 110, 30, 16, 249, 206, 180, 147, 125, 122, 37, 31, 241, 211, 182, 212, 55, 113, 33, 10, 255, 195, 162, 142, 75, 90, 46, 2, 226, 193, 237, 145, 96, 86, 90, 13, 22, 249, 215, 191, 141, 119, 89, 112, 83, 181, 196, 187, 159, 116, 64, 57, 67, 211, 244, 185, 130, 109, 75, 4, 10, 241, 205, 166, 200, 123, 104, 87, 51, 24, 244, 205, 247, 167, 72, 69, 62, 17, 255, 240, 190, 133, 97, 74, 112, 24, 249, 223, 172, 211 }, 173) +
                        UrlObfuscator.decode(new int[] { 200, 188, 142, 59, 89, 56, 8, 170, 208, 160, 154, 112, 70, 56, 31, 225, 134, 228, 151, 127, 88, 48, 19 }, 190) +
                        UrlObfuscator.decode(new int[] { 166, 136, 37, 91, 34, 4, 237, 199, 176, 200, 118, 71, 49, 13, 237, 204, 230, 224, 203, 44, 18, 40, 28, 236, 194, 164, 155, 47 }, 207) +
                        UrlObfuscator.decode(new int[] { 150, 158, 108, 29, 11, 70, 237, 208, 182, 147, 121, 66, 122, 26, 252, 223, 181, 157, 89, 68, 40, 31, 226, 133, 128, 218, 113, 76, 42, 7, 237, 214, 238, 182, 144, 115, 89, 41, 50, 252, 209, 176, 158, 97, 15, 58, 20, 185, 145, 152, 146, 113, 13, 3, 67, 251, 205, 179, 147, 119, 74, 120 }, 224) +
                        UrlObfuscator.decode(new int[] { 135, 113, 93, 110, 2, 249, 223, 247, 178, 85, 28 }, 241) +
                        UrlObfuscator.decode(new int[] { 116, 64, 50, 127, 14, 232, 207, 179, 199, 127, 77, 57, 21, 225, 221, 188, 156, 57, 92, 99, 26, 161, 219, 231, 130, 37, 90, 38, 2, 172, 223, 172, 151, 117, 14, 79, 43, 14, 244, 147, 129, 210, 48, 91, 121, 34, 189, 157, 166, 158, 86, 70, 54, 8, 232, 131, 254, 192, 36, 12, 110, 17, 171, 235, 235, 207, 116, 112, 120, 52, 4, 254, 222, 241, 204, 62, 26, 126, 92, 228, 157, 134, 217, 33, 90, 34, 42, 226, 210, 172, 140, 47, 18, 108, 72, 168, 138, 169, 207, 183, 55, 19, 40, 20, 220, 208, 160, 146, 114, 29, 96, 90, 190, 154, 248, 157, 111, 73, 99, 60, 163, 135, 188, 136, 64, 76, 60, 6, 230, 137, 244, 246, 163, 52, 7, 38, 65 }, 258) +
                        UrlObfuscator.decode(new int[] { 101, 83, 35, 80, 231, 207, 190, 184, 110, 82, 61, 85, 225, 211, 171, 135, 119, 75, 46, 14, 87, 251, 209, 245, 128, 124, 86, 42, 95, 224, 212, 166, 211, 123, 12, 96, 84, 231, 145, 169, 135, 36, 74, 32, 14, 234, 193, 138, 140, 102, 68, 51, 113, 18, 248, 210, 188, 142, 113, 3, 62, 93, 190, 157, 168, 132, 112, 66, 111, 0, 176, 201, 167, 196, 106, 64, 46, 10, 225, 234, 172, 134, 100, 83, 100, 55, 32, 167, 210, 188, 209, 118, 25, 56, 26, 240, 214, 134, 136, 96, 74, 115, 80, 177, 152, 236, 207, 102, 9, 40, 10, 224, 198, 148, 128, 108, 106, 91, 115, 8, 233, 211, 180, 208, 62, 24, 57, 17, 253, 213, 165, 152, 49, 30, 100, 30, 238, 222, 188, 154, 105, 6, 49, 22, 246, 199, 250, 157, 141, 123, 73, 41, 9, 244, 153, 190, 150, 122, 70, 49, 72, 239, 138 }, 275) +
                        UrlObfuscator.decode(new int[] { 82, 34, 16, 161, 206, 208, 186, 152, 111, 6, 62, 22, 251, 194, 187, 144, 122, 71, 124, 0, 229, 202, 188, 148, 95, 78, 38, 12, 235, 211, 169, 151, 69, 79, 46, 73, 167, 246, 211, 186, 208, 109, 83, 61, 29, 248, 154, 182, 149, 125, 68, 48, 3, 163, 221, 187, 139, 39, 72, 60, 28, 243, 201, 171, 200, 106, 76, 49, 21, 11, 178, 206, 185, 151, 127, 90, 44, 91, 226, 208, 172, 135, 115, 67, 53, 14, 162, 246, 190, 132, 102, 76, 117, 5, 243, 209, 176, 140, 108, 124, 108, 55, 79, 177, 212, 233, 214, 113, 11, 123, 30, 161, 152, 163, 222, 125, 89, 99, 15, 161, 223, 187, 139, 103, 4, 43, 7, 231, 193, 175, 197, 40, 27 }, 292) +
                        UrlObfuscator.decode(new int[] { 92, 54, 10, 191, 192, 180, 134, 51, 91, 108, 64, 180, 199, 241, 130, 100, 78, 44, 27, 169, 202, 160, 138, 100, 86, 41, 70, 89, 241, 200, 168, 213, 118, 92, 54, 16, 226, 221, 232, 194, 32, 1, 107, 6, 165, 134, 229, 144 }, 58) +
                        UrlObfuscator.decode(new int[] { 61, 11, 251, 136, 162, 138, 56, 74, 44, 6, 228, 211, 228, 183, 160, 39, 77, 59, 11, 184, 197, 235, 144, 120, 29, 53, 20, 228, 237, 161, 152, 98, 79, 35, 7, 239, 228, 170, 140, 97, 77, 54, 51, 229, 252, 202, 245, 213, 32 }, 75) +
                        UrlObfuscator.decode(new int[] { 53, 29, 178, 203, 246, 128, 127, 81, 32, 27, 174, 131, 224, 147, 114, 95, 98, 3, 239, 192, 175, 143, 114, 25, 124, 31, 254, 211, 238, 189, 145, 105, 72, 52, 23, 165, 133, 231, 138, 105, 70, 125, 6, 254, 192, 241, 211, 69, 80, 55, 24, 167, 218, 174, 129, 109, 80, 127, 95, 177, 220, 195, 172, 211, 112, 94, 60, 13, 166, 138, 129, 220, 119, 92, 60, 5, 249, 193, 187, 136, 55 }, 92) +
                        UrlObfuscator.decode(new int[] { 4, 234, 131, 184, 199, 127, 78, 34, 17, 236, 137, 176, 207, 104, 122, 87, 58, 20, 239, 132, 233, 214, 34, 28, 2, 94, 219, 155, 178, 159, 97, 90, 36, 2, 254, 207, 242 }, 109) +
                        UrlObfuscator.decode(new int[] { 8, 252, 206, 251, 153, 106, 5, 48, 19, 225, 247, 188, 159, 97, 69, 59, 11, 233, 255, 191, 147, 101, 77, 111, 3, 233, 141, 248 }, 126) +
                        UrlObfuscator.decode(new int[] { 230, 200, 229, 143, 120, 4, 45, 1, 244, 214, 169, 133, 122, 31, 124, 93, 88, 240, 210, 178, 158, 61, 69, 36, 20, 229, 155, 162, 154, 97, 88, 50, 6, 226, 196, 184, 146, 55, 20, 117, 64, 238, 204, 160, 135, 103, 79, 103, 35, 2, 237, 221, 169, 137, 124, 126, 59, 25, 244, 192, 251, 145, 98, 30, 32, 30, 236, 207, 162, 158, 112, 84, 59, 65, 180, 131, 234, 222, 49, 14, 15, 107, 84, 255, 212, 180, 141, 113, 89, 35, 16, 175 }, 143) +
                        UrlObfuscator.decode(new int[] { 214, 222, 172, 221, 104, 90, 61, 68, 253, 219, 248, 129, 117, 84, 28, 16, 253, 202, 224, 153, 99, 103, 37, 30, 237, 213, 133, 132, 119, 70, 106, 72, 187 }, 160) +
                        UrlObfuscator.decode(new int[] { 199, 177, 157, 46, 68, 63, 63, 239, 209, 188, 218, 46, 81, 37, 4, 191, 156, 253, 248, 150, 44, 27, 39, 6, 237, 217, 176, 203, 40, 9, 116, 26, 163, 151, 179, 146, 121, 77, 44, 87, 180, 149, 224, 142, 54, 3, 63, 30, 245, 193, 216, 227, 192, 33, 28, 50, 77, 191, 203, 170, 129, 117, 84, 111, 76, 173, 136, 190, 202, 112, 87, 62, 8, 239, 154, 251, 216, 35, 79, 43, 70, 252, 227, 202, 188, 155, 38, 7, 100, 95, 246, 145, 169, 136, 103, 83, 54, 77, 178, 147, 234, 159, 123, 75, 39, 79, 251, 218, 177, 133, 100, 31, 124, 93, 88, 242, 220, 190, 158, 118, 30, 113, 76 }, 177) +
                        UrlObfuscator.decode(new int[] { 171, 135, 40, 118, 77, 9, 25, 227, 206, 240, 131 }, 194) +
                        UrlObfuscator.decode(new int[] { 186, 148, 57, 17, 39, 15, 254, 248, 174, 146, 125, 0, 34, 10, 172, 141, 160, 141, 111, 84, 86, 48, 8, 249, 128 }, 211) +
                        UrlObfuscator.decode(new int[] { 146, 98, 80, 97, 6, 12, 163, 205, 189, 137, 105, 92, 30, 27, 249, 212, 160, 219, 113, 66, 126, 9, 225, 195, 184, 184, 99, 83, 45, 78, 250, 217, 245, 213, 57, 87, 33, 45, 94, 241, 212, 230, 138, 120, 74, 36, 19, 211, 216, 188, 147, 101, 24, 44, 29, 163, 192, 162, 132, 108, 96, 34, 15, 226, 204, 183, 203, 58, 73, 89, 118, 92, 240, 211, 166, 133, 116, 95, 106, 19, 231, 154, 190, 153, 45, 73, 61, 71, 189, 133, 249, 210 }, 228) +
                        UrlObfuscator.decode(new int[] { 131, 117, 65, 114, 29, 249, 193, 171, 158, 49, 102, 43, 29, 224, 137, 171, 132, 124, 11, 115, 77, 205, 254, 202, 181, 210, 118, 83, 55, 80, 164, 154, 152, 149, 103, 90, 127, 2, 224, 219, 163, 136, 35, 88, 103, 0, 226, 207, 162, 140, 119, 13, 45, 8, 86, 183, 148, 231 }, 245) +
                        UrlObfuscator.decode(new int[] { 112, 68, 54, 67, 224, 201, 253, 146, 159, 105, 84, 117, 23, 248, 192, 255, 206, 57, 82, 32, 88, 161, 158, 247, 219, 36, 23, 61, 11, 251, 136, 171, 131, 99, 80, 126, 16, 175, 204, 218, 184, 137, 39, 77, 59, 11, 184, 192, 230, 200, 102, 29, 37, 24, 244, 219, 166, 214 }, 262) +
                        UrlObfuscator.decode(new int[] { 126, 80, 125, 24, 250, 220, 180, 131, 50, 19, 112, 93, 162, 209, 189, 154, 126, 93, 51, 5, 241, 130, 179, 135, 194, 122, 82, 63, 14, 247, 220, 182, 131, 56, 86, 38, 22, 243, 197, 181, 189, 111, 67, 43, 14, 162, 128, 243, 149, 97, 11, 55, 6, 238, 196, 163, 171, 176, 114, 88, 62, 57, 246, 214, 163, 147, 123, 64, 32, 90, 244, 220, 230, 213, 123, 77, 57, 74, 251, 218, 250, 148, 98, 10, 36, 7, 245, 226, 208, 171, 147, 120, 82, 52, 30, 219, 219, 191, 144, 122, 71, 0, 20, 243, 219, 230, 196, 55, 66, 44, 65, 250, 213, 232, 146, 109, 71, 54, 9, 190, 167, 151, 166, 144, 126, 92, 45, 69, 229, 196, 251, 152, 118, 84, 37, 75, 248, 158, 240, 161, 106, 94, 33, 70, 234, 207, 171, 204, 113, 12, 54, 9, 27, 234, 213, 240, 137, 104, 23, 47, 30, 242, 193, 188, 218, 41, 76, 45, 12, 239, 217, 175, 131, 34, 76, 97, 28, 251, 216 }, 279) +
                        UrlObfuscator.decode(new int[] { 78, 40, 20, 173, 210, 162, 144, 33, 74, 2, 110, 70, 246, 135, 182, 144, 118, 82, 37, 78, 254, 152, 249, 216, 107, 95, 59, 30, 228, 131, 166, 140, 110, 83, 106, 23, 170, 215, 173, 145, 43, 117, 20, 49, 20, 176, 146, 181, 144, 58, 84, 61, 93, 188, 128, 253, 216, 99, 71, 35, 9, 248, 148, 248, 206, 33, 76, 120, 89, 190, 206, 168, 142, 154, 109, 16, 109, 82, 165, 206, 232, 221, 38, 27, 98, 65, 168, 198, 224, 195, 108, 69, 96, 95, 163, 146, 181 }, 296) +
                        UrlObfuscator.decode(new int[] { 67, 56, 16, 232, 223, 162 }, 62) +
                        UrlObfuscator.decode(new int[] { 57, 15, 255, 140, 169, 152, 52, 88, 38, 20, 246, 193, 133, 142, 110, 65, 75, 118, 30, 239, 149, 184, 150, 106, 83, 51, 7, 192, 220, 162, 189, 117, 73, 58, 63, 237, 207, 163, 156, 123, 14, 58, 25, 180, 152 }, 79) +
                        UrlObfuscator.decode(new int[] { 9, 25, 182, 238, 168, 137, 115, 87, 63, 95, 245, 198, 250, 145, 125, 67, 52, 10, 252, 249, 163, 155, 70, 76, 46, 19, 212, 196, 160, 138, 119, 82, 105, 113, 23, 243, 216, 190, 130, 86, 94, 127, 81, 176, 147, 250, 204, 44, 0, 102, 12, 255, 145, 169, 152, 38, 25, 119, 86, 175, 233, 162, 150, 105, 14, 82, 55, 19, 180, 201, 244, 142, 113, 83, 34, 29, 184, 193, 252, 153, 117, 70, 41, 5, 248, 130, 241 }, 96) +
                        UrlObfuscator.decode(new int[] { 1, 229, 220, 166, 197, 126, 5, 38, 12, 238, 211, 234, 151, 42, 87, 45, 17, 172, 237, 144, 170, 149, 127, 78, 49, 84, 229, 152, 189, 145, 122, 85, 57, 4, 163, 204, 191, 197, 48 }, 113) +
                        "}}" +
                        UrlObfuscator.decode(new int[] { 235, 199, 232, 176, 139, 105, 18, 55, 31, 247, 223, 163, 158, 41, 0, 122, 0, 244, 196, 186, 156, 99, 23 }, 130) +
                        UrlObfuscator.decode(new int[] { 252, 199, 165, 222, 124, 65, 63, 24, 163, 204, 188, 134, 100, 82, 44, 11, 237, 138, 160, 204, 157, 55, 70, 46, 30, 238, 204, 170, 153, 54, 84, 15, 66, 207, 156, 178, 180, 63, 112, 119, 22, 163, 146 }, 147) +
                        UrlObfuscator.decode(new int[] { 205, 165, 202, 110, 85, 75, 112, 17, 249, 213, 189, 141, 112, 9, 110, 69, 189, 220, 167, 133, 62, 67, 43, 3, 235, 223, 162, 212, 48, 23, 125 }, 164) +
                        UrlObfuscator.decode(new int[] { 195, 181, 129, 50, 90, 109, 3, 225, 206, 173, 159, 99, 70, 38, 73, 233, 215, 173, 132, 107, 79, 107, 51, 17, 254, 221, 175, 147, 118, 86, 121, 6, 244, 192, 187, 156, 112, 93, 42, 85 }, 181) +
                        UrlObfuscator.decode(new int[] { 177, 141, 109, 79, 39, 73, 235, 177, 210, 184, 146, 124, 78, 49, 70, 175, 144, 243, 159, 61, 81, 57, 17, 253, 239, 185, 196, 96, 4, 37, 13, 233, 193, 177, 140, 46, 19, 104, 93, 66, 163, 154, 243, 220, 51, 82, 101, 28, 184, 198, 161, 145, 97, 69, 34, 6, 224, 202, 228, 219, 38, 66, 102, 11, 227, 203, 163, 151, 106, 12, 113, 118, 69 }, 198) +
                        UrlObfuscator.decode(new int[] { 150, 152, 113, 70, 60, 27, 245, 242, 189, 135, 105, 75, 46, 68, 250, 201, 177, 131, 86, 79, 38, 14, 228, 212, 208, 176, 213, 119, 23, 16, 42, 215, 249, 248, 134, 96, 65, 59, 31, 247, 198, 168, 148, 36, 68, 63, 29, 161, 142, 253 }, 215) +
                        UrlObfuscator.decode(new int[] { 149, 100, 71, 49, 7, 235, 138, 164, 201, 132, 99, 64, 103 }, 232) +
                        UrlObfuscator.decode(new int[] { 138, 125, 67, 2, 28, 249, 214, 189, 132, 100, 7, 45, 12, 252, 135, 243, 217, 56, 14, 125, 22, 225, 215, 150, 136, 109, 122, 81, 40, 8, 179, 217, 184, 136, 59, 4, 109, 68, 163, 155, 234 }, 249) +
                        UrlObfuscator.decode(new int[] { 119, 84, 43, 6, 242, 198, 172, 203, 103, 8, 59, 34 }, 266), null);
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
                    UrlObfuscator.decode(new int[] { 51, 92, 44, 22, 244, 194, 188, 155, 125, 26, 120, 11 }, 283) +
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
                    UrlObfuscator.decode(new int[] { 69, 34, 22, 245 }, 49) +
                    "var vp=document.querySelector('meta[name=\"viewport\"]');" +
                    UrlObfuscator.decode(new int[] { 43, 7, 168, 190, 200, 173, 213, 96, 76, 41, 69, 243, 217, 182, 129, 126, 87, 63, 4, 161, 205, 191, 137, 106, 94, 44, 45, 235, 195, 168, 129, 109, 86, 105, 71, 18, 251, 201, 189, 220, 51, 2, 46, 7, 184, 198, 177, 135, 83, 69, 36, 29, 231, 207, 185, 159, 111, 1, 111, 9, 231, 200, 161, 196, 46, 6, 54, 54, 27, 234, 204, 180, 136, 109, 31, 126, 77, 241, 219, 176, 135, 124, 85, 33, 26, 163, 196, 174, 139, 109, 6, 38, 22, 245, 193, 173, 134, 66, 72, 86, 50, 25, 180, 205, 170, 208, 35, 74 }, 66) +
                    UrlObfuscator.decode(new int[] { 58, 20, 185, 145, 171, 129, 110, 89, 38, 15, 231, 220, 233, 129, 96, 80, 6, 14, 228, 205, 218, 176, 137, 94, 66, 19, 29, 176, 144, 137, 170, 117, 93, 54, 3, 255, 198, 170, 187, 101, 78, 61, 25, 231, 213, 178, 163, 109, 91, 101, 72, 169, 228 }, 83) +
                    UrlObfuscator.decode(new int[] { 18, 226, 208, 225, 147, 139, 35, 89, 51, 24, 239, 212, 189, 153, 98, 27, 55, 1, 247, 208, 164, 138, 75, 65, 41, 6, 239, 199, 188, 207, 33, 86, 48, 26, 238, 196, 231, 246, 197, 110, 72, 117, 19, 253, 133, 240, 169, 74, 85, 61, 22, 227, 223, 166, 138, 91, 69, 46, 29, 249, 199, 181, 146, 67, 77, 59, 69, 186 }, 100) +
                    UrlObfuscator.decode(new int[] { 6, 224, 157, 166, 148, 104, 91, 13, 2, 226, 223, 175, 135, 124, 26, 97, 13, 240, 206, 174, 205, 98, 112, 90, 36, 7, 182, 205, 188, 154, 124, 95, 33, 89, 231, 215, 169, 132, 34, 93, 36, 22, 238, 135, 168, 140, 109, 83, 54, 16, 185, 147, 241, 208, 218, 63, 84, 49, 11, 245, 203, 172, 150, 120, 65, 111, 7, 247, 201, 164, 194, 125, 68, 54, 14, 167, 200, 172, 141, 115, 86, 48, 89, 179, 145, 240, 250, 223, 116, 81, 43, 21, 235, 204, 182, 152, 97, 15, 46, 85, 170 }, 117) +
                    UrlObfuscator.decode(new int[] { 226, 202, 167, 150, 111, 68, 46, 43, 80, 245, 217, 186, 158, 55, 89, 39, 6, 240, 218, 183, 177, 121, 89, 35, 10, 165, 223, 191, 195, 50, 85 }, 134) +
                    UrlObfuscator.decode(new int[] { 234, 213, 180, 128, 112, 90, 121, 21, 166, 213, 176 }, 151) +
                    "try{var m=document.querySelector('meta[name=\"theme-color\"]');" +
                    UrlObfuscator.decode(new int[] { 193, 161, 206, 104, 2, 101, 21, 232, 206, 219, 177, 138, 50, 122, 52, 29, 234, 216, 191, 145, 86, 65, 59, 21, 247, 202, 232, 203, 77, 69, 46, 27, 231, 206, 162, 167, 118, 74, 38, 6, 229, 177, 223, 173, 140, 119, 67, 13, 16, 242, 219, 176, 183, 124, 94, 62, 2, 166, 213, 140, 130, 111, 88, 38, 1, 227, 228, 183, 141, 103, 69, 36, 78, 30, 238, 205, 176, 130, 78, 81, 61, 26, 243, 246, 187, 159, 125, 67, 120, 2, 160, 202, 169, 159, 75, 93, 60, 21, 239, 199, 177, 151, 103, 9, 103, 60, 17, 243, 200, 190, 148, 109, 31, 126, 10, 233, 147, 244, 219, 42, 77 }, 168) +
                    UrlObfuscator.decode(new int[] { 196, 187, 150, 98, 86, 60, 91, 247, 152, 171, 146 }, 185) +
                    UrlObfuscator.decode(new int[] { 190, 155, 113, 92, 47, 3, 172, 212, 171, 143, 100, 112, 73, 115, 61, 245, 222, 171, 151, 126, 82, 23, 6, 250, 214, 182, 149, 41, 8, 12, 2, 239, 216, 166, 129, 99, 100, 55, 13, 231, 197, 164, 206, 158, 110, 77, 48, 2, 202, 216, 191, 146, 84, 84, 55, 24, 245, 195, 191, 154, 96, 73, 101, 16 }, 202) +
                    UrlObfuscator.decode(new int[] { 173, 155, 107, 24, 39, 31, 246, 223, 145, 149, 44, 86, 58, 0, 238, 216, 162, 133, 103, 0, 55, 30, 169, 212, 186, 203, 122 }, 219) +
                    UrlObfuscator.decode(new int[] { 154, 106, 88, 105, 11, 239, 199, 172, 138, 62, 121, 28, 91, 9, 255, 207, 252, 158, 39, 93, 55, 20, 227, 216, 177, 157, 102, 31, 53, 3, 235, 192, 169, 133, 126, 111, 58, 8, 235, 245, 171, 138, 108, 85, 104, 47, 6, 160, 129, 181, 143, 117, 84, 104, 59, 244, 192, 187, 220, 119, 92, 32, 1, 255, 132, 188, 131, 103, 76, 40, 17, 171, 205, 173, 140, 100, 82, 104, 55, 25, 232, 211, 245, 203, 49, 13, 38, 13, 184, 195, 171, 204, 45, 65, 59, 1, 224, 148, 189, 128, 102, 67, 41, 18, 170, 202, 172, 143, 101, 109, 118, 56, 21, 252, 210, 173, 213, 37, 12, 37, 13, 186, 137 }, 236) +
                    UrlObfuscator.decode(new int[] { 138, 116, 82, 54, 28, 176, 210, 255, 142, 119, 91, 51, 24, 254, 129, 190, 152, 127, 67, 98, 12, 161, 156, 163, 216, 97, 13, 50, 0, 242, 250, 208, 169, 185, 119, 95, 52, 29, 249, 194, 238, 137 }, 253) +
                    UrlObfuscator.decode(new int[] { 103, 75, 100, 15, 229, 202, 189, 138, 99, 75, 48, 77, 224, 206, 164, 166, 216, 59, 95, 51, 27, 240, 214, 249, 159, 123, 80, 54, 10, 222, 214, 231, 138, 98, 79, 62, 7, 236, 198, 179, 200, 103, 75, 39, 27, 168, 156, 143, 247, 158, 116, 90, 51, 23, 182, 199, 163, 134, 124, 27, 54, 30, 243, 218, 163, 136, 98, 95, 100, 11, 231, 195, 191, 204, 63 }, 270) +
                    UrlObfuscator.decode(new int[] { 118, 88, 117, 31, 243, 219, 176, 150, 57, 95, 59, 16, 246, 202, 158, 150, 39, 74, 34, 15, 254, 199, 172, 134, 115, 8, 33, 11, 224, 215, 172, 133, 145, 106, 120, 48, 30, 247, 220, 182, 131, 63, 9, 100, 90, 241, 217, 177, 134, 96, 3, 60, 30, 249, 193, 224, 131, 105, 70, 49, 14, 231, 207, 180, 241, 154, 114, 95, 46, 23, 252, 214, 163, 179, 121, 81, 62, 23, 255, 196, 230, 213 }, 287) +
                    UrlObfuscator.decode(new int[] { 67, 53, 1, 178, 193, 177, 157, 125, 72, 113, 13, 255, 199, 171, 147, 111, 74, 42, 75, 241, 136, 187, 182, 152, 53, 29, 40, 83, 235, 221, 163, 131, 103, 90, 115, 28, 228, 220, 163, 213, 123, 77, 57, 74, 232, 149, 180, 200, 108, 74, 39, 7, 249, 239, 217, 246, 218, 52, 28, 115, 66, 238, 214, 164, 213, 118, 14, 33, 95, 249, 193, 170, 136, 116, 100, 44, 65, 175, 142, 225, 204, 63, 74, 36, 73, 225, 163, 142, 161, 128, 121, 6, 105, 81, 229, 211, 161, 129, 97, 92, 113, 30, 250, 194, 161, 215 }, 53) +
                    UrlObfuscator.decode(new int[] { 48, 4, 246, 131, 180, 220, 115, 49, 77, 40, 30, 232, 206, 171, 145, 121, 81, 125, 21, 184, 131, 253, 146, 38, 0, 62, 28, 231, 195, 189, 192, 32, 10, 98, 77, 184, 203, 167, 200, 137, 48, 81, 57, 21, 253, 205, 176, 203, 37, 28, 38, 22, 230, 196, 162, 129, 46, 67, 57, 7, 230, 146 }, 70) +
                    UrlObfuscator.decode(new int[] { 33, 23, 231, 148, 178, 158, 44, 70, 97, 2, 232, 194, 172, 158, 97, 22, 116, 89, 245, 197, 177, 145, 100, 102, 83, 49, 28, 232, 147, 172, 162, 43, 106, 127, 79, 165, 136, 164, 144, 98, 15, 60, 80, 252, 202, 184, 154, 109, 110, 40, 17, 172, 213, 153, 209, 93, 51, 15, 109, 85, 183, 221, 228, 136, 118, 68, 38, 17, 218, 220, 165, 216, 121, 117, 124, 49, 167, 155, 249, 193, 43, 68, 41, 89, 243, 195, 179, 147, 154, 87, 83, 40, 83, 236, 226, 234, 170, 58, 4, 100, 90, 169 }, 87) +
                    UrlObfuscator.decode(new int[] { 1, 225, 142, 172, 151, 77, 67, 15, 72, 13, 183, 193, 160, 146, 105, 119, 57, 57, 190, 210, 253, 143, 110, 88, 35, 33, 239, 227, 228, 137, 102, 0, 97, 21, 227, 209, 177, 145, 108, 1, 46, 42, 18, 241, 135, 169, 159, 109, 77, 37, 24, 238, 198, 233, 128, 61, 87, 117, 9, 161, 206, 241, 136, 101, 4, 38, 92, 228, 200, 190, 217, 124, 27 }, 104) +
                    UrlObfuscator.decode(new int[] { 15, 249, 197, 246, 157, 113, 75, 111, 23, 229, 193, 173, 153, 101, 68, 36, 65, 235, 142, 189, 147, 101, 81, 98, 9, 189, 249, 203, 179, 159, 111, 83, 54, 22, 191, 216, 252, 143, 125, 15, 28, 17, 251, 198, 227, 129, 106, 82, 97, 88, 171, 235, 164, 144, 107, 12, 44, 9, 17, 182, 143, 233, 206, 54, 87, 113, 94, 173, 199, 177, 135, 103, 67, 62, 79, 166, 195, 240, 218, 60, 22, 111, 87, 161, 159, 227, 196, 43, 10, 46, 113, 10, 242, 239, 175, 136, 112, 86, 48, 94, 164, 130, 250, 201, 108, 11, 61, 11, 249, 217, 185, 132, 41, 15, 100, 65, 174, 204, 235, 129, 47, 82, 22, 117, 21, 180, 216, 244, 158, 49, 28, 62, 93, 247, 157, 176, 216, 43, 82, 117 }, 121) +
                    UrlObfuscator.decode(new int[] { 236, 198, 186, 207, 112, 68, 54, 67, 233, 156, 240, 228, 149, 33, 95, 51, 27, 240, 214, 249, 154, 112, 90, 52, 6, 249, 139, 164, 197, 38, 5, 48, 28, 232, 218, 231, 133, 56, 84, 34, 16, 242, 197, 151, 185, 152, 104, 120, 53, 20, 232, 194, 162, 144, 112, 96, 38, 8, 252, 202, 230, 142, 100, 74, 35, 7, 211, 204, 155, 204, 42, 65, 35, 2, 235, 248, 204, 178, 137, 117, 94, 26, 23, 251, 217, 167, 221, 40, 91, 55, 88, 236, 136, 235, 143, 37, 75, 119, 85, 183, 136, 252, 221, 42, 80, 36, 20, 10, 236, 211, 252, 147, 127, 65, 112, 20, 191, 142, 169 }, 138) +
                    UrlObfuscator.decode(new int[] { 237, 219, 171, 216, 99, 11, 37, 21, 225, 193, 180, 216, 104, 75, 57, 47, 228, 199, 185, 157, 115, 67, 33, 55, 247, 219, 173, 133, 215, 122, 82, 63, 14, 247, 220, 182, 131, 56, 87, 59, 23, 235, 205, 172, 139, 97, 78, 57, 6, 239, 199, 188, 201, 98, 74, 39, 22, 239, 196, 174, 171, 187, 113, 89, 54, 31, 247, 204, 254, 216, 118, 91, 63, 29, 227, 153, 244 }, 155) +
                    UrlObfuscator.decode(new int[] { 197, 173, 194, 125, 14, 97, 78, 181, 138, 241, 219, 56, 10, 75, 112, 15, 183, 139, 244, 204, 32, 0, 124, 1, 186, 212, 249, 193, 62, 30, 127, 89, 166, 223, 228, 139, 33, 8, 116, 80, 177, 157, 242, 207, 54, 54, 76, 56, 8, 238, 200, 183, 216, 48, 21, 101, 65, 163, 135, 225, 197, 40, 21 }, 172) +
                    UrlObfuscator.decode(new int[] { 207, 185, 143, 111, 75, 54, 87, 177, 150, 178, 149, 116, 87, 54, 9, 169, 150, 177, 208 }, 189) +
                    UrlObfuscator.decode(new int[] { 184, 140, 126, 11, 57, 12, 230, 195, 132, 130, 57, 69, 55, 15, 227, 235, 215, 178, 146, 51, 19, 34, 12, 229, 207, 174, 130, 114, 64, 113, 24, 178, 222, 164, 143, 96, 104, 46, 64, 174, 157, 172, 130, 43, 74, 96, 93, 66, 233, 212, 178, 159, 117, 78, 118, 40, 201, 212, 186, 151, 96, 94, 57, 11, 204, 202, 128, 138, 121, 93, 97, 28, 241, 204, 170, 135, 109, 86, 110, 0, 33, 252, 210, 191, 136, 118, 81, 51, 52, 242, 248, 178, 129, 101, 13, 39, 85, 204, 194, 175, 152, 102, 65, 35, 36, 247, 205, 167, 133, 100, 14, 94, 46, 13, 240, 194, 138, 152, 127, 82, 20, 20, 247, 216, 181, 131, 127, 90, 32, 9, 164, 195, 227, 210, 117 }, 206) +
                    UrlObfuscator.decode(new int[] { 182, 152, 53, 125, 53, 30, 235, 215, 190, 146, 87, 70, 58, 22, 246, 213, 225, 143, 125, 92, 39, 19, 204, 204, 160, 131, 70, 75, 47, 13, 243, 211, 150, 165, 139, 125, 73, 122, 13, 232, 138, 166, 156, 119, 88, 16, 22, 184, 226, 175, 153, 100, 5, 44, 5, 231, 200, 180, 205, 115, 74, 44, 5, 239, 232, 144, 180, 146, 117, 95, 43, 47, 254, 210, 161, 156, 60, 0, 120, 92, 190, 135, 246, 154, 106, 88, 105, 4, 225, 155, 181, 141, 96, 73, 3, 7, 87, 175, 145, 145, 154, 110, 81, 118, 17, 250, 218, 187, 129, 58, 70, 57, 1, 234, 194, 187, 197, 99, 71, 38, 2, 244, 237, 161, 138, 101, 73, 52, 112, 76, 180, 149, 224, 140, 120, 74, 119, 4, 225, 137, 163, 155, 114, 91, 13, 9, 165, 219, 162, 132, 109, 71, 48, 72, 236, 202, 173, 135, 115, 119, 86, 58, 9, 244, 150, 232, 213, 85, 86, 34, 29, 186, 213, 190, 158, 127, 93, 102, 26, 229, 197, 174, 134, 127, 9, 47, 11, 234, 198, 176, 169, 101, 118, 89, 53, 8, 180, 136, 240, 209, 44 }, 223) +
                    UrlObfuscator.decode(new int[] { 134, 110, 92, 109, 9, 224, 151, 189, 152, 44, 74, 35, 79, 241, 214, 234, 136, 196, 119, 91, 116, 30, 241, 152, 229, 202, 97, 92, 58, 23, 253, 198, 254, 176, 81, 76, 34, 15, 248, 198, 161, 131, 67, 65, 35, 6, 206, 192, 179, 171, 215, 102, 75, 50, 20, 253, 215, 160, 216, 74, 107, 50, 28, 245, 194, 160, 135, 105, 105, 47, 13, 236, 228, 166, 149, 113, 25, 38, 9, 186, 225, 209, 186, 143, 115, 82, 62, 59, 234, 222, 178, 146, 113, 29, 51, 1, 224, 195, 183, 168, 104, 76, 47, 42, 231, 203, 169, 151, 119, 11, 54, 17, 172, 243, 216, 241, 142, 111, 19, 98, 5, 234 }, 240) +
                    UrlObfuscator.decode(new int[] { 124, 67, 94, 42, 30, 244, 147, 191, 208, 99, 74, 43, 78 }, 257) +
                    UrlObfuscator.decode(new int[] { 97, 84, 62, 11, 204, 202, 228, 194, 49 }, 274) +
                    UrlObfuscator.decode(new int[] { 74, 36, 73, 161, 232, 215, 179, 152, 116, 77, 119, 39, 200, 215, 187, 144, 97, 93, 56, 20, 205, 201, 143, 131, 126, 68, 45, 65, 252, 209, 172, 138, 103, 77, 54, 78, 32, 193, 220, 178, 159, 104, 86, 49, 19, 212, 210, 150, 156, 103, 95, 52, 82, 250, 223, 185, 142, 49, 77, 39, 4, 243, 200, 161, 141, 118, 15, 33, 59, 26, 216, 202, 190, 148, 109, 116, 62, 5, 225, 209, 189, 151, 99, 24, 104, 8, 226, 207, 190, 153, 96, 70, 96, 74, 246, 193, 173, 134, 67, 71, 19, 42, 15, 233, 222, 243, 194 }, 291) +
                    UrlObfuscator.decode(new int[] { 79, 57, 5, 182, 217, 181, 128, 102, 101, 35, 82, 190, 150, 168, 132, 105, 92, 37, 2, 232, 209, 234, 130, 102, 69, 5, 41, 27, 243, 200, 151, 147, 106, 76, 50, 24, 240, 198, 251, 213, 101, 95, 58, 13, 229, 223, 191, 139, 123, 92, 96, 74, 227, 209, 173, 129, 117, 73, 80, 48, 85, 181, 192, 172, 152, 106, 23, 56, 72, 208, 210, 166, 148, 62, 65, 33, 26, 164, 130, 241, 128, 110, 15, 40, 72, 232, 194, 177, 149, 84, 108, 2, 101, 76, 171, 147, 171, 157, 99, 67, 39, 26, 168, 222, 176, 131, 123, 122, 62, 81, 229, 145, 186, 141, 105, 66, 7, 3, 171, 139, 250, 157, 211, 101, 94, 61, 11, 238, 204, 170, 146, 44, 65, 38, 6, 247, 157, 160, 142, 125, 94, 37, 29, 239, 147, 188, 149, 115, 64, 57, 74, 185, 214, 169, 177, 154, 114, 75, 117, 27, 253, 220, 146, 128, 112, 90, 39, 62, 248, 195, 187, 139, 99, 73, 57, 66, 174, 218, 162, 149, 108, 94, 38, 69, 173, 211, 218, 176, 153, 94, 92, 115, 66 }, 57) +
                    UrlObfuscator.decode(new int[] { 62, 27, 241, 220, 176, 132, 118, 3, 32, 6, 212, 162, 208, 168, 144, 119, 1, 47, 25, 229, 150, 183, 147, 64, 81, 57, 21, 235, 147, 171, 153, 101, 73, 61, 1, 232, 200, 237, 205, 120, 75, 39, 72, 29, 249, 233, 245, 137, 127, 77, 45, 5, 248, 142, 182, 148, 70, 12, 35, 10, 250, 249, 165, 134, 111, 70, 61, 19, 174, 195, 177, 141, 97, 85, 41, 48, 16, 181, 149, 160, 152, 126, 108, 106, 24, 224, 216, 191, 201, 98, 85, 33, 10, 207, 203, 227, 195, 50, 85, 107, 87, 176, 148, 234, 217, 124, 27 }, 74) +
                    UrlObfuscator.decode(new int[] { 53, 31, 238, 152, 154, 131, 97, 85, 39, 27, 254, 222, 128, 140, 126, 73, 57, 28, 236, 218, 239, 132, 98, 119, 32, 10, 228, 196, 150, 240, 146, 126, 72, 63, 11, 238, 210, 254, 145, 123, 80, 39, 28, 245, 193, 186, 195, 104, 68, 41, 28, 229, 194, 168, 145, 65, 79, 39, 12, 229, 241, 202, 241, 135, 122, 78, 45, 10, 254, 212, 160, 128, 118, 65, 107, 4, 253, 219, 168, 192, 106, 94, 61, 26, 238, 196, 176, 144, 102, 100, 40, 12, 11, 251, 207, 230, 160, 61, 90, 52, 22, 229, 198, 243, 223, 53, 66, 36, 22, 226, 200, 235, 199, 45, 77, 41, 19, 231, 136, 176, 139, 103, 76, 37, 120, 35, 224, 149, 224 }, 91) +
                    UrlObfuscator.decode(new int[] { 5, 237, 130, 173, 135, 100, 83, 40, 1, 237, 214, 239, 130, 144, 122, 68, 117, 21, 255, 206, 248, 186, 99, 65, 53, 7, 251, 222, 190, 160, 108, 94, 41, 25, 252, 204, 186, 207, 100, 66, 23, 0, 234, 196, 164, 246, 208, 114, 94, 40, 31, 235, 206, 178, 222, 113, 91, 48, 7, 252, 213, 161, 154, 35, 78, 36, 14, 240, 132, 188, 135, 113, 80, 49, 11, 227, 213, 203, 187, 142, 38, 79, 40, 12, 253, 155, 183, 129, 96, 65, 59, 19, 229, 219, 171, 171, 101, 71, 62, 12, 250, 157, 157, 194, 103, 79, 35, 18, 243, 184, 146, 250, 143, 111, 67, 53, 29, 176, 235, 168, 221, 40, 79, 50, 17, 251, 205, 165, 196, 110, 3, 50, 21, 250 }, 108) +
                    UrlObfuscator.decode(new int[] { 0, 225, 216, 187, 141, 123, 95, 126, 16, 189, 200, 175 }, 125) +
                    UrlObfuscator.decode(new int[] { 250, 223, 181, 144, 99, 79, 96, 70, 241, 204, 170, 135, 109, 86, 110, 0, 33, 252, 210, 191, 136, 118, 81, 51, 62, 244, 196, 167, 155, 114, 67, 13, 1, 248, 194, 175, 195, 114, 95, 46, 8, 225, 203, 180, 204, 94, 127, 94, 48, 25, 238, 212, 179, 157, 80, 86, 38, 1, 253, 208, 161, 179, 127, 90, 32, 9, 177, 223, 184, 156, 109, 28 }, 142) +
                    UrlObfuscator.decode(new int[] { 251, 209, 190, 137, 118, 95, 55, 12, 185, 215, 177, 144, 86, 68, 52, 30, 251, 226, 164, 159, 127, 79, 39, 13, 245, 142, 226, 135, 111, 75, 34, 11, 88, 178, 219, 169, 149, 121, 77, 49, 24, 248, 157, 177, 218, 105 }, 159) +
                    "var t=e.target;var el=t&&t.closest?t.closest('button,a,[role=\"button\"],input[type=\"button\"],input[type=\"submit\"]'):null;" +
                    UrlObfuscator.decode(new int[] { 217, 169, 198, 104, 64, 109, 76, 254, 193, 169, 130, 106, 83, 109, 35, 239, 196, 205, 177, 148, 120, 121, 40, 16, 252, 208, 179, 211, 50, 114, 60, 21, 226, 192, 167, 137, 78, 89, 35, 13, 239, 194, 232, 147, 109, 65, 48, 0, 244, 250, 151, 166, 189, 117, 94, 43, 23, 254, 210, 151, 134, 122, 86, 54, 21, 161, 216, 164, 142, 121, 75, 61, 13, 175, 143, 254, 153 }, 176) +
                    UrlObfuscator.decode(new int[] { 188, 204, 139, 108, 72, 57, 82, 161, 196, 165, 148, 119, 65, 55, 27, 186, 212, 249, 148, 115 }, 193) +
                    // Polyfills the Web Share API on top of the real Android
                    // share sheet -- most WebView builds don't implement
                    // navigator.share at all, so a page's own "Share" button
                    // either does nothing or falls back to a hand-rolled
                    // copy-link menu instead of the native chooser. Only
                    // installed when the page doesn't already have a working
                    // navigator.share (some newer WebView versions do).
                    UrlObfuscator.decode(new int[] { 166, 131, 105, 84, 39, 11, 164, 220, 163, 135, 108, 72, 49, 75, 197, 205, 166, 147, 111, 118, 90, 31, 14, 242, 222, 190, 157, 49, 16, 20, 26, 247, 192, 190, 153, 107, 108, 63, 5, 239, 205, 172, 198, 116, 78, 36, 22, 230, 132, 231, 193, 145, 127, 75, 53, 28, 251, 205, 183, 133, 56, 70, 60, 18, 224, 212, 249, 148 }, 210) +
                    UrlObfuscator.decode(new int[] { 141, 99, 87, 41, 56, 31, 233, 211, 169, 212, 106, 80, 54, 4, 240, 137, 181, 135, 127, 83, 59, 7, 226, 194, 227, 142, 104, 92, 38, 79, 254 }, 227) +
                    UrlObfuscator.decode(new int[] { 144, 114, 70, 48, 77, 235, 207, 185, 141, 119, 86, 50, 21, 188 }, 244) +
                    UrlObfuscator.decode(new int[] { 113, 86, 58, 25, 192, 206, 219, 172, 146, 117, 95, 24, 11, 241, 211, 177, 144, 58, 64, 58, 16, 226, 202, 230, 190, 120, 89, 35, 7, 239, 143, 162, 132, 112, 66, 108, 21, 233, 235, 210, 184, 128, 103, 29, 126, 81, 187, 229, 161, 134, 122, 92, 54, 88, 235, 207, 185, 141, 37, 94, 44, 16, 243, 218, 185, 195, 36, 11, 109, 51, 11, 236, 212, 178, 156, 50, 93, 57, 3, 247, 155, 161, 129, 126, 77, 44, 72, 169, 132, 229, 208 }, 261) +
                    UrlObfuscator.decode(new int[] { 100, 80, 32, 6, 224, 223, 240, 191, 124, 66, 33, 2, 249, 204, 230, 149, 99, 86, 43, 15, 244, 196, 232, 246, 197, 96, 95, 58, 14, 250, 208, 255, 147, 60, 79, 33, 23, 229, 197, 189, 128, 45, 124, 57, 5, 228, 193, 180, 131, 43, 86, 38, 8, 228, 195, 203, 246, 152, 53, 0, 39, 4, 163 }, 278) +
                    UrlObfuscator.decode(new int[] { 73, 39, 19, 237, 196, 163, 149, 111, 109, 16, 62, 29, 245, 233, 177, 153, 101, 83, 104, 18, 230, 220, 178, 132, 102, 65, 35, 68, 162, 209, 187, 141, 115, 83, 55, 10, 163, 214, 179, 149, 154, 37, 64, 103 }, 295) +
                    UrlObfuscator.decode(new int[] { 64, 33, 24, 251, 205, 187, 159, 62, 80, 125, 8, 239 }, 61) +
                    // Reports the scrollTop of whichever element just
                    // scrolled -- document or any nested panel -- so native
                    // knows whether a pull-to-refresh gesture is actually
                    // safe (see SwipeRefreshLayout override above). Scroll
                    // events don't bubble, so this has to be a capture
                    // listener on window to see scrolling from any
                    // descendant, not just the document itself. Throttled
                    // with a trailing rAF flag so a fast scroll doesn't
                    // spam the JS bridge with a call per pixel.
                    UrlObfuscator.decode(new int[] { 58, 31, 245, 208, 163, 143, 32, 6, 49, 12, 234, 199, 173, 150, 46, 64, 97, 60, 18, 255, 200, 182, 145, 115, 101, 54, 6, 252, 222, 189, 178, 96, 91, 35, 8, 162, 209, 190, 129, 105, 66, 42, 19, 173, 253, 158, 129, 145, 122, 79, 51, 18, 254, 234, 187, 133, 121, 89, 56, 49, 253, 196, 190, 139, 51, 89, 62, 30, 239, 146 }, 78) +
                    UrlObfuscator.decode(new int[] { 41, 31, 239, 156, 171, 159, 119, 92, 62, 24, 242, 137, 181, 147, 125, 67, 42, 66, 225, 205, 184, 158, 89, 71, 52, 91, 227, 197, 175, 145, 100, 27 }, 95) +
                    UrlObfuscator.decode(new int[] { 7, 230, 192, 169, 131, 124, 4, 40, 12, 227, 227, 179, 129, 109, 86, 13, 9, 12, 234, 216, 178, 158, 104, 17, 127, 4, 245, 199, 187, 159, 126, 22, 124, 9, 251, 195, 175, 159, 99, 70, 38, 79, 227, 140, 191 }, 112) +
                    UrlObfuscator.decode(new int[] { 232, 198, 151, 174, 152, 114, 95, 51, 23, 255, 158, 164, 144, 96, 70, 32, 31, 171, 223, 171, 131, 104, 66, 36, 14, 181, 211, 180, 144, 97, 24 }, 129) +
                    UrlObfuscator.decode(new int[] { 224, 212, 161, 154, 107, 94, 56, 42, 228, 192, 165, 134, 114, 76, 43, 13, 196, 211, 161, 178, 155, 53, 90, 46, 20, 250, 204, 190, 153, 123, 28, 122, 9, 225, 213, 161, 138, 100, 66, 44, 87, 239, 201, 171, 149, 96, 31 }, 146) +
                    UrlObfuscator.decode(new int[] { 213, 163, 147, 32, 122, 82, 96, 84, 254, 148, 173, 153, 101, 81, 48, 0, 181, 148, 180, 222, 123, 79, 63, 11, 238, 222, 231, 134, 104, 66, 32, 48, 250, 210, 164, 221, 194, 35, 12, 117, 68, 255, 151, 172, 150, 100, 82, 49, 7, 168, 213, 191, 140, 123, 64, 41, 5, 254, 135, 187, 132, 116, 74, 40, 15, 235, 207, 167, 154, 146, 120, 81, 62, 20, 237, 131 }, 163) +
                    UrlObfuscator.decode(new int[] { 194, 178, 128, 49, 68, 32, 30, 176, 201, 167, 213, 108, 68, 105, 21, 230, 214, 172, 142, 109, 116, 80, 46, 71, 172, 128 }, 180) +
                    UrlObfuscator.decode(new int[] { 179, 133, 113, 2, 49, 15, 12, 163, 201, 179, 139, 36, 9, 99, 30, 240, 157, 164, 156, 97, 16, 109, 82, 226, 204, 191, 159, 90, 70, 59, 78, 253, 201, 165, 144, 118, 113, 47, 44, 67, 237, 211, 168, 193, 112, 94, 127, 1, 252, 218, 183, 157, 102, 30, 14, 0, 233, 222, 164, 131, 109, 106, 53, 15, 225, 195, 166, 196, 39, 97, 81, 58, 15, 243, 210, 190, 187, 106, 94, 50, 18, 241, 157, 160, 148, 96, 64, 60, 25, 223, 200, 184, 134, 100, 75, 18, 10, 244, 138, 185, 160, 110, 123, 76, 50, 21, 255, 248, 171, 145, 115, 81, 48, 90, 225, 215, 161, 159, 125, 90, 30, 15, 249, 197, 165, 132, 83, 73, 53, 76, 247, 205, 177, 156, 207, 55, 6, 33, 6 }, 197) +
                    UrlObfuscator.decode(new int[] { 171, 220, 47, 78, 126, 5, 226, 218, 171, 196, 55, 86, 55, 10, 233, 211, 165, 141, 44, 70, 107, 26, 253 }, 214) +
                    // Zero-touch native Google sign-in: patches the page's own
                    // Firebase Auth instance (if it finds one) so
                    // signInWithPopup/signInWithRedirect with a Google
                    // provider transparently run through Credential
                    // Manager instead of the popup Google always rejects
                    // from inside a WebView -- no change needed in the
                    // wrapped site's own code. Only patches once
                    // (__androidGoogleAuthPatched guards re-running this
                    // on every onPageFinished, e.g. after an SPA reload).
                    // IMPORTANT LIMITATION: this only sees window.firebase,
                    // i.e. the traditional <script src=".../firebase-auth-
                    // compat.js"> global build. A site that bundles the
                    // modular v9+ SDK (import { getAuth } from
                    // 'firebase/auth') never puts 'firebase' on window at
                    // all, so there's nothing here to find or patch --
                    // sign-in on that kind of site still needs the manual
                    // AndroidBridge.signInWithGoogle() wiring described
                    // where that bridge method is defined.
                    UrlObfuscator.decode(new int[] { 147, 116, 92, 63, 75, 228, 212, 174, 188, 138, 116, 83, 53, 82, 176, 195 }, 231) +
                    UrlObfuscator.decode(new int[] { 145, 113, 30, 116, 3, 250, 220, 181, 159, 120, 0, 12, 2, 239, 216, 166, 129, 99, 100, 55, 13, 231, 197, 164, 156, 131, 63, 124, 50, 31, 232, 214, 177, 147, 84, 71, 61, 23, 245, 212, 254, 156, 103, 74, 34, 34, 228, 254, 161, 147, 110, 98, 43, 12, 229, 205, 165, 246, 140, 120, 72, 46, 8, 247, 131 }, 248) +
                    UrlObfuscator.decode(new int[] { 111, 93, 41, 5, 241, 205, 172, 140, 33, 80, 94, 42, 30, 244, 147, 188, 155, 49, 76 }, 265) +
                    UrlObfuscator.decode(new int[] { 115, 95, 112, 86, 240, 215, 168, 143, 51, 87, 50, 65, 239, 216, 184, 131, 118, 85, 46, 5, 168, 196, 177, 151, 106, 15, 31, 0, 31, 243, 216, 169, 149, 112, 92, 16, 25, 250, 211, 191, 151, 80, 69, 59, 6, 221, 205, 191, 137, 97, 77, 35, 79, 247, 193, 183, 151, 115, 78, 4 }, 282) +
                    UrlObfuscator.decode(new int[] { 70, 46, 28, 173, 235, 138, 186, 52, 78, 37, 72, 228, 209, 183, 138, 47, 103, 80, 49, 26, 240, 222, 155, 140, 108, 95, 6, 7, 251, 197, 187, 149, 117, 93, 117, 4, 234, 131, 235, 174, 73, 119, 111, 23, 225, 215, 183, 147, 110, 36 }, 48) +
                    UrlObfuscator.decode(new int[] { 55, 1, 13, 190, 205, 174, 148, 110, 86, 99, 3, 228, 204, 175, 131, 96, 94, 36, 0, 179, 226, 174, 129, 111, 74, 60, 73, 225, 192, 176, 179, 112, 78, 52, 48, 10, 228, 204, 190, 181, 127, 16, 49, 20, 187, 213, 166, 134, 121, 24, 102, 71, 182, 209, 168, 139, 125, 75, 47, 78, 224, 141, 184, 144, 100, 84, 74, 44, 19, 167, 198 }, 65) +
                    UrlObfuscator.decode(new int[] { 59, 23, 184, 142, 190, 159, 99, 95, 37, 64, 250, 194, 178, 144, 118, 77, 121 }, 82) +
                    UrlObfuscator.decode(new int[] { 56, 165, 210, 169, 184, 144, 84, 82, 12, 19, 237, 208, 135, 153, 101, 65, 35, 85, 189, 151, 188, 135, 106, 66, 2, 4, 222, 193, 179, 142, 87, 65, 39, 11, 243, 197, 220, 170, 218, 65, 21, 60, 22, 234, 242, 183, 150, 124, 27, 52, 4, 254, 204, 186, 132, 99, 69, 98, 4, 161, 220 }, 99) +
                    UrlObfuscator.decode(new int[] { 2, 242, 192, 241, 159, 125, 71, 42, 81, 251, 216, 166, 156, 104, 125, 40, 57, 184, 203, 167, 200, 139, 103, 77, 57, 20, 252, 153, 183, 133, 127, 82, 117, 78, 175, 150, 182, 154, 96, 78, 56, 2, 229, 199, 239, 206, 116, 64, 48, 22, 240, 207, 251 }, 116) +
                    UrlObfuscator.decode(new int[] { 245, 214, 172, 150, 110, 123, 82, 3, 64, 250, 206, 180, 154, 108, 94, 57, 27, 188, 195, 160, 158, 102, 70, 42, 8, 254, 130, 177 }, 133) +
                    UrlObfuscator.decode(new int[] { 224, 212, 166, 211, 123, 66, 23, 0, 225, 202, 160, 142, 55, 89, 58, 8, 240, 204, 160, 134, 112, 7, 102, 119, 14, 239, 211, 173, 147, 125, 93, 37, 88, 229, 198, 188, 132, 120, 84, 42, 28, 196, 200, 246, 215, 52, 15, 32, 9, 234, 195, 175, 135, 47, 67, 80, 51, 90, 224, 199, 242, 190, 89, 103, 112, 83, 228, 193, 189, 135, 121, 75, 43, 31, 172, 194, 164, 154, 124, 70, 40, 6, 225, 204, 164, 193, 71, 94, 110, 116, 85, 160 }, 150) +
                    UrlObfuscator.decode(new int[] { 206, 160, 205, 37, 74, 49, 38, 239, 240, 217, 177, 153, 50, 72, 60, 12, 226, 196, 187, 212, 124, 64, 56, 23, 161, 207, 189, 156, 103, 83, 97, 28, 239, 207, 182, 200, 98, 80, 38, 21, 18, 251, 211, 168, 136, 51, 2 }, 167) +
                    UrlObfuscator.decode(new int[] { 206, 182, 132, 53, 85, 38, 6, 249, 249, 161, 157, 121, 17, 63, 2, 224, 219, 252 }, 184) +
                    UrlObfuscator.decode(new int[] { 187, 141, 115, 83, 55, 10, 163, 204, 164, 151, 223, 78, 79, 51, 22, 243, 202, 189, 223, 112, 64, 58, 16, 230, 216, 191, 129, 38, 95, 41, 24, 229, 197, 190, 130, 42, 87, 33, 9, 231, 194, 180, 246, 133 }, 201) +
                    UrlObfuscator.decode(new int[] { 188, 140, 118, 84, 34, 28, 251, 221, 242, 158, 126, 125, 43, 30, 164, 206, 227, 146 }, 218) +
                    UrlObfuscator.decode(new int[] { 156, 99, 71, 44, 8, 241, 139, 182, 134, 111, 78, 54, 58, 59, 235, 217, 181, 142, 85, 81, 36, 2, 240, 218, 182, 128, 57, 23, 46, 0, 233, 222, 164, 131, 109, 111, 40, 9, 226, 200, 166, 177, 104, 71, 81, 23, 19, 206, 222, 169, 140, 116, 67, 113, 89, 251, 221, 128, 148, 99, 6, 117 }, 235) +
                    UrlObfuscator.decode(new int[] { 138, 122, 72, 121, 28, 170, 211, 251, 144, 118, 70, 48, 25, 227, 210, 177, 151, 118, 17 }, 252) +
                    UrlObfuscator.decode(new int[] { 100, 74, 99, 75, 237, 134, 168, 141, 44, 95, 49, 7, 235, 197, 220, 170, 213, 114, 94, 45, 89, 221, 197, 164, 154, 102, 27, 54, 95, 253, 202, 189, 158, 109, 76, 47, 21, 244, 128, 129, 138, 107, 68, 46, 4, 160, 236, 215, 186, 146, 54, 83, 55, 88, 241, 215, 188, 152, 118, 86, 118, 89, 166, 149, 191, 137, 127, 95, 59, 6, 188, 219 }, 269) +
                    UrlObfuscator.decode(new int[] { 127, 72, 40, 19, 211, 215, 171, 131, 56, 70, 61, 20, 252, 248, 190, 184, 103, 89, 36, 40, 248, 204, 172, 130, 104, 81, 45, 2, 238, 137, 135, 158, 174, 51, 95, 41, 31, 253, 221, 185, 130, 124, 85, 63, 90, 245, 158, 166, 138, 89, 67, 32, 15, 231, 129, 238, 200, 113, 76, 38, 12, 169, 210, 218, 173, 146, 112, 77, 63, 85, 234, 210, 188, 144, 119, 71, 123, 74 }, 286) +
                    "}" +
                    UrlObfuscator.decode(new int[] { 67, 58, 28, 245, 223, 184, 192, 108, 72, 47, 47, 255, 205, 169, 146, 73, 77, 48, 22, 228, 206, 218, 172, 213, 59, 90, 52, 29, 234, 216, 191, 145, 83, 92, 61, 22, 252, 202, 157, 132, 107, 69, 3, 7, 218, 194, 181, 144, 104, 87, 101, 77, 239, 241, 236, 184, 143, 50, 1 }, 52) +
                    UrlObfuscator.decode(new int[] { 4, 10, 231, 208, 174, 137, 155, 92, 79, 53, 31, 253, 220, 246, 132, 127, 82, 58, 58, 252, 230, 185, 155, 102, 106, 35, 4, 237, 197, 173, 207, 47, 30 }, 69) +
                    UrlObfuscator.decode(new int[] { 43, 92, 175 }, 86) +
                    UrlObfuscator.decode(new int[] { 26, 189, 216, 237, 216 }, 103) +
                    UrlObfuscator.decode(new int[] { 30, 245, 152, 180, 129, 103, 90, 127, 47, 208, 207, 163, 136, 121, 69, 32, 12, 192, 201, 170, 131, 111, 71, 0, 21, 11, 246, 237, 189, 143, 121, 81, 61, 19, 171, 193, 166, 134, 119, 10 }, 120) +
                    "}" +
                    UrlObfuscator.decode(new int[] { 224, 206, 239, 145, 108, 74, 39, 13, 246, 142, 217, 183, 143, 121, 89, 59, 10, 253, 158, 173, 133, 117, 71, 49, 25, 184, 216, 167, 131, 104, 68, 61, 71, 238, 206, 180, 128, 102, 66, 49, 4, 169, 164, 195 }, 137) +
                    UrlObfuscator.decode(new int[] { 255, 213, 171, 146, 109, 67, 53, 1, 178, 223, 237, 223, 53, 91, 45, 25, 170, 192, 190, 218, 117, 64, 48, 42, 236, 213, 165, 173, 136, 124, 80, 115, 28, 236, 214, 180, 130, 124, 91, 61, 90, 184, 203, 161, 197, 38, 23 }, 154) +
                    UrlObfuscator.decode(new int[] { 194, 172, 193, 127, 78, 40, 1, 235, 212, 236, 135, 105, 109, 91, 63, 29, 232, 223, 240, 131, 103, 87, 33, 23, 251, 154, 166, 153, 97, 74, 34, 27, 165, 204, 160, 154, 98, 68, 36, 23, 230, 139, 250, 131, 147, 123, 92, 46, 50, 244, 205, 189, 133, 96, 84, 56, 91, 251, 199, 249, 212, 115 }, 171) +
                    UrlObfuscator.decode(new int[] { 217, 183, 137, 124, 24, 62, 16, 189, 218, 237, 192, 33, 25, 52, 13, 225, 201, 170, 152, 64, 70, 51, 3, 247, 210, 162, 142, 41, 73, 73, 119, 70, 225 }, 188) +
                    UrlObfuscator.decode(new int[] { 176, 192, 57, 31, 121, 65, 188, 219 }, 205) +
                    UrlObfuscator.decode(new int[] { 163, 212, 52, 18, 97, 4, 251, 214, 162, 150, 124, 27, 55, 88, 235, 210 }, 222) +
                    UrlObfuscator.decode(new int[] { 146, 39, 5, 101, 80 }, 239),
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
                if (rel.isEmpty()) rel = UrlObfuscator.decode(new int[] { 105, 113, 90, 56, 4, 181, 210, 173, 149, 123 }, 256);

                byte[] data = embedCache.get(rel);
                if (data == null) {
                    data = EmbeddedAssets.get(rel);
                    if (data != null) embedCache.put(rel, data);
                }
                if (data == null) return super.shouldInterceptRequest(view, request);

                return new WebResourceResponse(guessEmbeddedMime(rel), UrlObfuscator.decode(new int[] { 68, 100, 9, 67, 181 }, 273), new ByteArrayInputStream(data));
            }

            private String guessEmbeddedMime(String rel) {
                String lower = rel.toLowerCase();
                if (lower.endsWith(UrlObfuscator.decode(new int[] { 12, 41, 20, 18, 242 }, 290)) || lower.endsWith(UrlObfuscator.decode(new int[] { 22, 63, 2, 248 }, 56))) return UrlObfuscator.decode(new int[] { 61, 13, 255, 210, 234, 140, 119, 79, 45 }, 73);
                if (lower.endsWith(UrlObfuscator.decode(new int[] { 116, 26, 235, 196 }, 90))) return UrlObfuscator.decode(new int[] { 31, 239, 209, 188, 200, 101, 86, 55 }, 107);
                if (lower.endsWith(UrlObfuscator.decode(new int[] { 82, 241, 201 }, 124)) || lower.endsWith(UrlObfuscator.decode(new int[] { 163, 193, 161, 153 }, 141))) return UrlObfuscator.decode(new int[] { 255, 205, 172, 151, 115, 90, 57, 3, 255, 218, 186, 220, 120, 80, 38, 14, 253, 206, 190, 130, 122, 93 }, 158);
                if (lower.endsWith(UrlObfuscator.decode(new int[] { 129, 164, 158, 99, 69 }, 175))) return UrlObfuscator.decode(new int[] { 161, 175, 142, 113, 85, 56, 27, 237, 209, 184, 152, 58, 94, 32, 29, 255 }, 192);
                if (lower.endsWith(UrlObfuscator.decode(new int[] { 255, 131, 121, 73 }, 209))) return UrlObfuscator.decode(new int[] { 139, 108, 65, 88, 59, 82, 239, 205, 189, 210, 96, 90, 58 }, 226);
                if (lower.endsWith(UrlObfuscator.decode(new int[] { 221, 98, 95, 55 }, 243))) return UrlObfuscator.decode(new int[] { 109, 78, 35, 6, 229, 176, 206, 179, 155 }, 260);
                if (lower.endsWith(UrlObfuscator.decode(new int[] { 59, 94, 35, 21 }, 277)) || lower.endsWith(UrlObfuscator.decode(new int[] { 8, 47, 20, 230, 197 }, 294))) return UrlObfuscator.decode(new int[] { 85, 54, 27, 254, 221, 248, 156, 101, 81, 52 }, 60);
                if (lower.endsWith(UrlObfuscator.decode(new int[] { 99, 11, 226, 204 }, 77))) return UrlObfuscator.decode(new int[] { 55, 16, 253, 220, 191, 214, 127, 94, 48 }, 94);
                if (lower.endsWith(UrlObfuscator.decode(new int[] { 65, 249, 200, 174, 155 }, 111))) return UrlObfuscator.decode(new int[] { 233, 242, 223, 186, 153, 52, 77, 60, 26, 231 }, 128);
                if (lower.endsWith(UrlObfuscator.decode(new int[] { 191, 199, 160, 136, 107, 30 }, 145))) return UrlObfuscator.decode(new int[] { 196, 174, 142, 139, 49, 74, 51, 29, 252, 139 }, 162);
                if (lower.endsWith(UrlObfuscator.decode(new int[] { 157, 165, 158, 118, 73 }, 179))) return UrlObfuscator.decode(new int[] { 162, 140, 108, 85, 111, 40, 17, 251, 218 }, 196);
                if (lower.endsWith(UrlObfuscator.decode(new int[] { 251, 128, 103, 84 }, 213))) return UrlObfuscator.decode(new int[] { 128, 106, 74, 55, 77, 245, 212, 217 }, 230);
                if (lower.endsWith(UrlObfuscator.decode(new int[] { 217, 127, 86, 59 }, 247))) return UrlObfuscator.decode(new int[] { 97, 74, 39, 2, 225, 140, 186, 204, 105, 124, 81, 51 }, 264);
                return UrlObfuscator.decode(new int[] { 120, 72, 39, 26, 252, 215, 178, 134, 120, 95, 33, 65, 226, 207, 191, 143, 125, 5, 52, 18, 247, 193, 162, 143 }, 281);
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
                // A form submit (login, signup, checkout...) that the site answers
                // with 4xx/5xx is the site's own result page -- "wrong password",
                // "try again later" -- not a broken load. Taking it over with the
                // full-screen error replaced the site's page and made every failed
                // login look like the app crashed. Only the very first load, or a
                // plain link/GET navigation, still gets the branded error screen.
                if (request.isForMainFrame() && hasLoadedOnce[0]
                        && !UrlObfuscator.decode(new int[] { 104, 11, 57 }, 47).equalsIgnoreCase(request.getMethod())) {
                    return;
                }
                if (request.isForMainFrame()) {
                    showOffline(UrlObfuscator.decode(new int[] { 8, 11, 42, 205, 156 }, 64) + status, request.getUrl().toString());
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
                int errBg = Color.parseColor(isOfflineError(description) ? UrlObfuscator.decode(new int[] { 114, 54, 201, 232, 139, 170, 77 }, 81) : UrlObfuscator.decode(new int[] { 65, 177, 225, 143, 159, 205, 94 }, 98));
                errorView.setBackgroundColor(errBg);
                errorWeb.setBackgroundColor(errBg);
                errorWeb.loadDataWithBaseURL(UrlObfuscator.decode(new int[] { 27, 230, 197, 160, 156, 52, 2, 99, 14, 248, 219, 167, 149, 40, 73, 43, 0, 227, 205, 239 }, 115),
                    renderErrorPage(description, failingUrl), UrlObfuscator.decode(new int[] { 240, 198, 186, 149, 47, 119, 74, 48, 16 }, 132), UrlObfuscator.decode(new int[] { 192, 224, 149, 223, 41 }, 149), null);
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
                popupSettings.setUserAgentString(popupDefaultUA.replace(UrlObfuscator.decode(new int[] { 157, 229, 147, 117 }, 166), "").replaceAll("Version/[0-9.]+\s", ""));
                popupWebView.setVerticalScrollBarEnabled(false);
                popupWebView.setHorizontalScrollBarEnabled(false);

                final Dialog popupDialog = new Dialog(MainActivity.this, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
                // Loading overlay: same bouncing dots + colors as the main
                // navigation overlay, shown over the popup until its page has
                // painted, so there is never an empty black/white screen while
                // a link (sign-in, Telegram, any website) is still loading.
                final FrameLayout popupRoot = new FrameLayout(MainActivity.this);
                popupRoot.setBackgroundColor(Color.parseColor(UrlObfuscator.decode(new int[] { 148, 230, 176, 85, 6, 23, 72 }, 183)));
                popupRoot.addView(popupWebView, new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
                final FrameLayout popupLoader = new FrameLayout(MainActivity.this);
                popupLoader.setBackgroundColor(Color.parseColor(UrlObfuscator.decode(new int[] { 235, 215, 67, 100, 113, 38, 187 }, 200)));
                final BouncingDotsView popupDots = new BouncingDotsView(MainActivity.this, Color.parseColor(UrlObfuscator.decode(new int[] { 250, 190, 37, 112, 103, 49, 214 }, 217)));
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
                                UrlObfuscator.decode(new int[] { 158, 108, 80, 51, 73, 237, 208, 174, 142 }, 234), UrlObfuscator.decode(new int[] { 174, 78, 127, 117, 79 }, 251), failed);
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
                                renderErrorPage(UrlObfuscator.decode(new int[] { 68, 127, 30, 57, 168 }, 268) + status, failed),
                                UrlObfuscator.decode(new int[] { 105, 89, 35, 14, 182, 208, 163, 155, 121 }, 285), UrlObfuscator.decode(new int[] { 102, 6, 55, 189, 151 }, 51), failed);
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
                        if (scheme != null && !scheme.equals(UrlObfuscator.decode(new int[] { 44, 23, 246, 209 }, 68)) && !scheme.equals(UrlObfuscator.decode(new int[] { 61, 0, 231, 194, 162 }, 85))) {
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
        return EMBED_HOST + UrlObfuscator.decode(new int[] { 15, 235, 192, 166, 154, 47, 72, 75, 51, 17 }, 102);
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
        if (Intent.ACTION_SEND.equals(action) && UrlObfuscator.decode(new int[] { 3, 243, 205, 160, 220, 98, 93, 49, 6, 224 }, 119).equals(intent.getType())) {
            String shared = intent.getStringExtra(Intent.EXTRA_TEXT);
            if (shared != null && !shared.isEmpty()) {
                try {
                    return baseUrl() + UrlObfuscator.decode(new int[] { 183, 212, 174, 132, 118, 70, 38, 62, 244, 250, 198, 169, 193 }, 136) + java.net.URLEncoder.encode(shared, UrlObfuscator.decode(new int[] { 204, 236, 145, 219, 45 }, 153));
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

        String shortcutAction = intent.getStringExtra(UrlObfuscator.decode(new int[] { 217, 161, 135, 117, 82, 38, 17, 247, 253, 160, 131, 139, 119, 82, 50 }, 170));
        if (UrlObfuscator.decode(new int[] { 201, 191, 149, 119, 86, 50 }, 187).equals(shortcutAction)) {
            webView.getSettings().setCacheMode(WebSettings.LOAD_NO_CACHE);
            webView.reload();
            webView.getSettings().setCacheMode(WebSettings.LOAD_CACHE_ELSE_NETWORK);
            return;
        }
        if (UrlObfuscator.decode(new int[] { 164, 132, 103, 76 }, 204).equals(shortcutAction)) {
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
                blobTmp = File.createTempFile(UrlObfuscator.decode(new int[] { 191, 144, 116, 88 }, 221), UrlObfuscator.decode(new int[] { 192, 121, 65, 59 }, 238), getCacheDir());
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
                Toast.makeText(MainActivity.this, UrlObfuscator.decode(new int[] { 187, 113, 74, 50, 23, 245, 216, 188, 215, 112, 84, 61, 31, 247, 213, 234, 207, 109, 66, 57, 7, 238, 137, 166, 136, 114, 5, 54, 6, 227, 197, 224, 171, 150, 116, 79, 123, 28, 240, 212, 178 }, 255), Toast.LENGTH_LONG).show();
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
                    sendIntent.setType(UrlObfuscator.decode(new int[] { 100, 74, 54, 25, 163, 219, 166, 136, 97, 73 }, 272));
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
                String safeTitle = (title == null || title.trim().isEmpty()) ? UrlObfuscator.decode(new int[] { 106, 40, 62, 18, 244, 218, 186, 174, 118, 72, 2, 6 }, 289) : title;
                String safeBody = body == null ? "" : body;
                NotificationCompat.Builder notification = new NotificationCompat.Builder(MainActivity.this, UrlObfuscator.decode(new int[] { 83, 51, 19, 245, 198, 190, 133 }, 55))
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
            if (hex == null || !hex.matches(UrlObfuscator.decode(new int[] { 22, 68, 174, 254, 133, 206, 68, 64, 109, 57, 78, 176, 133, 134, 129, 47, 69, 43, 45, 212, 153, 149, 147, 60, 86, 127, 67, 180, 241, 176, 217, 116, 1, 99 }, 72))) return;
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
                android.content.SharedPreferences sp = getSharedPreferences(UrlObfuscator.decode(new int[] { 42, 19, 242, 218, 176, 128, 124, 92, 14, 6, 190 }, 89), MODE_PRIVATE);
                org.json.JSONArray idx = new org.json.JSONArray(sp.getString(UrlObfuscator.decode(new int[] { 53, 214, 193, 163, 158 }, 106), "[]"));
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
                ed.putString("p:" + key, json).putString(UrlObfuscator.decode(new int[] { 36, 197, 208, 188, 143 }, 123), next.toString()).apply();
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
            if (hex == null || !hex.matches(UrlObfuscator.decode(new int[] { 210, 136, 145, 168, 37, 97, 39, 72, 226, 147, 239, 216, 93, 100, 8, 32, 88 }, 140))) return;
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
            final String hexRe = UrlObfuscator.decode(new int[] { 195, 159, 128, 187, 52, 126, 54, 91, 243, 132, 254, 203, 76, 75, 121, 19, 169 }, 157);
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

        // Runs "Sign in with Google" as a real system flow (Credential
        // Manager's own bottom sheet, drawn outside the WebView entirely)
        // instead of a popup inside the page -- Google's OAuth endpoint
        // rejects sign-in from any embedded WebView on sight, so this is
        // the only path that actually completes. Call from the wrapped
        // site like:
        //   if (window.AndroidBridge && AndroidBridge.signInWithGoogle) {
        //     AndroidBridge.signInWithGoogle();
        //   } else {
        //     // fall back to the site's normal web sign-in for browser tabs
        //   }
        // and define window.onAndroidGoogleSignIn(idToken) /
        // window.onAndroidGoogleSignInError(message) on the page to
        // receive the result -- see startGoogleSignIn() below for exactly
        // what's sent. A Firebase Auth page typically finishes the flow
        // with:
        //   firebase.auth().signInWithCredential(
        //     firebase.auth.GoogleAuthProvider.credential(idToken));
        @JavascriptInterface
        public void signInWithGoogle() {
            runOnUiThread(MainActivity.this::startGoogleSignIn);
        }
    }

    // Backs AndroidBridge.signInWithGoogle() above. GetSignInWithGoogleOption
    // (rather than the older GetGoogleIdOption) always shows Google's
    // account chooser, matching the explicit "tap a Sign in with Google
    // button" gesture this is wired to -- GetGoogleIdOption's silent/
    // auto-select behavior is meant for a passive "sign in automatically
    // on app launch" prompt, which isn't what a button press should do.
    private void startGoogleSignIn() {
        GetSignInWithGoogleOption option = new GetSignInWithGoogleOption
            .Builder(GOOGLE_WEB_CLIENT_ID)
            .build();
        GetCredentialRequest request = new GetCredentialRequest.Builder()
            .addCredentialOption(option)
            .build();
        credentialManager.getCredentialAsync(
            this,
            request,
            new CancellationSignal(),
            ContextCompat.getMainExecutor(this),
            new androidx.credentials.CredentialManagerCallback<GetCredentialResponse, GetCredentialException>() {
                @Override
                public void onResult(GetCredentialResponse result) {
                    Credential credential = result.getCredential();
                    if (!(credential instanceof CustomCredential) ||
                        !GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL.equals(credential.getType())) {
                        notifyGoogleSignInError(UrlObfuscator.decode(new int[] { 251, 163, 137, 115, 90, 44, 11, 243, 195, 161, 196, 96, 80, 36, 4, 26, 240, 201, 181, 154, 118, 25, 44, 14, 230, 208 }, 174));
                        return;
                    }
                    try {
                        GoogleIdTokenCredential googleIdTokenCredential =
                            GoogleIdTokenCredential.createFrom(((CustomCredential) credential).getData());
                        notifyGoogleSignInSuccess(googleIdTokenCredential.getIdToken());
// AUTO-REPAIR (dead code, provably unreachable):                     } catch (GoogleIdTokenParsingException e) {
                        notifyGoogleSignInError(UrlObfuscator.decode(new int[] { 252, 177, 136, 112, 95, 122, 23, 247, 195, 246, 133, 117, 65, 33, 20, 176, 232, 161, 130, 107, 71, 47, 73, 193, 227, 230, 145, 107, 72, 39, 15 }, 191));
                    }
                }

                @Override
                public void onError(GetCredentialException e) {
                    // User dismissed the chooser (NoCredentialException / the
                    // user tapped outside it) is the overwhelmingly common
                    // case here, not a real failure -- surfaced the same way
                    // as any other error so the page decides how to handle
                    // it, rather than this silently swallowing it.
                    notifyGoogleSignInError(e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName());
                }
            }
        );
    }

    // JSONObject.quote() produces a properly escaped JSON string literal
    // (quotes, backslashes, control chars all handled), which is also
    // valid JS string syntax -- the same trick used for the injected
    // theme-color/haptics script in onPageFinished above, just via the
    // library instead of hand-rolled replace() calls since a Google ID
    // token is long enough that a missed edge case would be easy to miss.
    // Fires a CustomEvent in addition to the optional named callbacks --
    // the named callbacks are for a site that was manually wired up to
    // AndroidBridge.signInWithGoogle(); the event is what the zero-touch
    // Firebase auto-patch injected in onPageFinished listens for instead,
    // since it can't assume the page defined anything.
    private void notifyGoogleSignInSuccess(String idToken) {
        runOnUiThread(() -> webView.evaluateJavascript(
            UrlObfuscator.decode(new int[] { 248, 137, 123, 67, 47, 31, 227, 198, 166, 207, 47, 94, 48, 17, 251, 218, 169, 185, 214, 106, 85, 53, 30, 246, 207, 249, 153, 123, 117, 61, 22, 227, 223, 166, 138, 74, 67, 36, 13, 229, 205, 148, 143, 98, 74, 10, 12, 168, 219 }, 208) +
            UrlObfuscator.decode(new int[] { 150, 105, 113, 90, 50, 11, 181, 213, 183, 185, 121, 82, 39, 27, 250, 214, 150, 159, 96, 73, 33, 9, 216, 195, 174, 134, 78, 72, 109 }, 225) + JSONObject.quote(idToken) + ");" +
            UrlObfuscator.decode(new int[] { 143, 108, 83, 46, 26, 238, 196, 227, 143, 32, 83, 58 }, 242) +
            UrlObfuscator.decode(new int[] { 119, 80, 56, 27, 8, 247, 211, 184, 148, 109, 23, 60, 30, 229, 197, 181, 135, 113, 89, 21, 25, 235, 195, 184, 195, 100, 76, 63, 71, 197, 208, 183, 151, 109, 76, 5, 41, 27, 243, 200, 243, 221, 120, 86, 51, 4, 250, 221, 183, 181, 126, 95, 40, 2, 232, 255, 162, 141, 103, 97, 41, 52, 224, 215, 182, 142, 117, 7, 19 }, 259) +
            UrlObfuscator.decode(new int[] { 111, 87, 55, 5, 241, 198, 162, 215, 119, 68, 33, 83, 252, 213, 179, 128, 40, 74, 38, 53, 239, 244, 219, 179, 198 }, 276) + JSONObject.quote(idToken) + UrlObfuscator.decode(new int[] { 88, 57, 74, 171, 154, 189, 188, 159, 105, 95, 51, 82, 252, 145, 172, 139 }, 293) +
            UrlObfuscator.decode(new int[] { 70, 115, 81, 177, 140 }, 59), null));
    }

    private void notifyGoogleSignInError(String message) {
        runOnUiThread(() -> webView.evaluateJavascript(
            UrlObfuscator.decode(new int[] { 100, 13, 255, 199, 171, 147, 111, 74, 42, 75, 171, 218, 180, 173, 135, 102, 85, 61, 82, 238, 209, 185, 146, 122, 67, 125, 29, 255, 241, 161, 138, 127, 67, 34, 14, 206, 199, 168, 129, 105, 65, 16, 11, 230, 206, 246, 176, 184, 110, 73, 53, 11, 177, 204 }, 76) +
            UrlObfuscator.decode(new int[] { 42, 21, 245, 222, 182, 143, 57, 89, 59, 53, 253, 214, 163, 159, 102, 74, 10, 3, 228, 205, 165, 141, 84, 79, 34, 10, 202, 204, 132, 146, 141, 113, 79, 116 }, 93) + JSONObject.quote(message) + ");" +
            UrlObfuscator.decode(new int[] { 19, 240, 207, 170, 158, 106, 64, 111, 3, 172, 223, 190 }, 110) +
            UrlObfuscator.decode(new int[] { 11, 236, 196, 167, 140, 115, 87, 60, 24, 225, 155, 176, 154, 97, 65, 49, 27, 237, 197, 137, 157, 111, 71, 60, 79, 232, 192, 179, 195, 65, 84, 51, 43, 17, 240, 249, 173, 159, 119, 76, 127, 81, 244, 218, 183, 128, 126, 89, 43, 41, 226, 195, 172, 134, 108, 123, 46, 1, 235, 237, 173, 176, 100, 83, 74, 50, 9, 187, 151 }, 127) +
            UrlObfuscator.decode(new int[] { 235, 203, 171, 153, 109, 66, 38, 83, 243, 200, 173, 223, 98, 66, 46, 18, 229, 179, 211, 184, 143, 104, 91, 62, 29, 173 }, 144) + JSONObject.quote(message) + UrlObfuscator.decode(new int[] { 220, 189, 246, 215, 38, 65, 56, 27, 237, 219, 191, 222, 112, 29, 40, 15 }, 161) +
            UrlObfuscator.decode(new int[] { 207, 248, 216, 38, 21 }, 178), null));
    }


    // Native keyboard correction is intentionally limited to the Options
    // panel. Chrome already handles ordinary fields well; forcing every
    // focused field to the center in a WebView can create a large jump and
    // fight the page's own scroll containers. Only make a small correction
    // when an Options field is actually outside the visible viewport.
    private void scrollFocusedFieldIntoView() {
        if (webView == null) return;
        webView.evaluateJavascript(
            UrlObfuscator.decode(new int[] { 235, 132, 116, 78, 92, 42, 20, 243, 213, 242, 208, 99, 67, 36, 12, 239 }, 195) +
            UrlObfuscator.decode(new int[] { 162, 146, 96, 17, 32, 82, 234, 194, 175, 158, 103, 76, 38, 19, 168, 194, 161, 151, 71, 77, 37, 50, 27, 243, 200, 153, 131, 80, 92, 127, 81, 225, 213, 177, 162, 112, 94, 42, 2, 194, 220, 191, 131, 102, 70, 52, 65, 172, 159 }, 212) +
            UrlObfuscator.decode(new int[] { 140, 98, 11, 99, 17, 252, 227, 206, 243, 148, 114, 94, 61, 29, 249, 202, 169, 213, 99, 28, 50, 28, 238, 221, 190, 160, 98, 89, 61, 70, 228, 201, 171, 144, 98, 75, 47, 19, 87, 185, 212, 175, 214, 123, 90, 44, 30, 224, 208, 243, 218, 59, 67, 53, 27, 251, 223, 162, 208 }, 229) +
            UrlObfuscator.decode(new int[] { 128, 116, 70, 115, 19, 172, 212, 160, 141, 120, 65, 46, 4, 253, 134, 166, 133, 113, 77, 53, 7, 196, 204, 218, 179, 152, 114, 79, 97, 16, 254, 159, 247, 148, 104, 79, 51, 76, 173, 146, 170, 130, 111, 94, 39, 12, 230, 211, 232, 135, 107, 71, 59, 29, 252, 190, 206, 243, 159, 116, 84, 45, 25, 254, 216, 166, 220, 114, 27, 120, 2, 234, 218, 184, 158, 101, 17 }, 246) +
            UrlObfuscator.decode(new int[] { 113, 71, 55, 68, 247, 159, 233, 129, 209, 106, 92, 59, 53, 251, 212, 189, 139, 106, 18, 115, 90, 188, 197, 191, 163, 97, 90, 41, 25, 201, 200, 187, 130, 46, 12, 127 }, 263) +
            UrlObfuscator.decode(new int[] { 113, 81, 126, 1, 181, 142, 239, 214, 121, 65, 62, 24, 248, 140, 236, 207, 124, 6, 123, 88, 163, 215, 167, 153, 116, 126, 76, 56, 29, 188, 156, 255, 140, 54, 11, 104, 83, 224, 215, 189, 149, 108, 90, 106, 74, 173, 139, 168, 198, 110, 85, 6, 11, 237, 214, 164, 142, 139, 91, 89, 53, 15, 251, 219, 180, 146, 63, 71, 49, 7, 231, 195, 190, 212 }, 280) +
            UrlObfuscator.decode(new int[] { 95, 41, 21, 166, 215, 249, 130, 44, 70, 37, 43, 60, 242, 201, 181, 158, 112, 86, 48, 53, 249, 221, 182, 156, 101, 98, 42, 13, 249, 132, 226, 209 }, 297) +
            UrlObfuscator.decode(new int[] { 73, 63, 15, 188, 205, 172, 196, 111, 94, 56, 17, 251, 196, 252, 135, 121, 92, 59, 12, 224, 253, 163, 140, 127, 87, 41, 23, 240, 152, 180, 128, 114, 63, 72, 53, 65, 237, 204, 230, 142, 97, 24, 61, 17, 250, 213, 185, 132, 53, 89, 36, 2, 239, 197, 190, 198, 110, 72, 43, 1, 241, 234, 164, 137, 152, 118, 73, 103 }, 63) +
            UrlObfuscator.decode(new int[] { 38, 14, 252, 141, 188, 138, 110, 20, 121, 81, 189, 211, 165, 145, 34, 69, 37, 51, 10, 252, 129, 235, 193 }, 80) +
            UrlObfuscator.decode(new int[] { 8, 230, 183, 204, 243, 158, 116, 78, 45, 23, 250, 136, 163, 156, 62, 66, 48, 20, 166, 202, 168, 128, 127, 75, 116, 37, 230, 210, 173, 202, 110, 75, 47, 72, 13, 176, 223, 179, 143, 110, 86, 53, 90, 190, 195, 188, 222, 98, 80, 52, 70, 162, 224, 173, 159, 98, 7, 37, 6, 254, 141, 246, 215, 46, 87, 40, 117, 78, 179, 141, 227, 211, 48, 3 }, 97) +
            UrlObfuscator.decode(new int[] { 23, 253, 195, 170, 206, 100, 74, 99, 24, 167, 220, 168, 150, 57, 84, 34, 6, 168, 196, 218, 178, 137, 125, 6, 119, 52, 249, 195, 190, 219, 121, 90, 60, 89, 224, 206, 170, 192, 126, 5, 62, 6, 248, 139, 139, 132, 112, 75, 108, 12, 225, 231, 150, 236, 202, 55, 76, 49, 82, 167, 152, 228, 196, 58, 27, 106 }, 114) +
            UrlObfuscator.decode(new int[] { 234, 196, 233, 132, 154, 114, 73, 61, 82, 225, 207, 185, 133, 54, 70, 55, 78, 243, 159, 179, 131, 97, 94, 41, 24, 254, 129, 239, 201, 101, 74, 42, 23, 231, 207, 180, 243, 208, 105, 93, 57, 42, 248, 214, 178, 154, 57, 26, 60, 2, 229, 217, 160, 128, 126, 1, 40, 5, 231, 220, 162, 136, 113, 8, 24, 6, 224, 212, 222, 243, 142, 127, 73, 53, 21, 244, 154, 181, 154, 122, 71, 51, 24, 254, 202, 188, 176, 43, 2, 113 }, 131) +
            UrlObfuscator.decode(new int[] { 253, 213, 250, 130, 115, 9, 104, 30, 239, 133, 185, 138, 122, 72, 42, 9, 204, 198, 171, 134, 104, 107, 0, 46, 31, 181, 217, 181, 145, 114, 88, 33, 60, 246, 219, 182, 152, 123, 7, 54, 31, 232, 132, 186, 139, 117, 73, 41, 8, 193, 219, 233, 155, 139, 113, 77, 102, 31, 255, 213, 172, 150, 58, 87, 49, 27, 243, 199, 185, 128, 124, 23, 107, 10, 255, 221, 167, 192, 123, 12, 127, 30 }, 148) +
            UrlObfuscator.decode(new int[] { 192, 168, 144, 103, 90, 55, 54, 16, 249, 211, 172, 212, 106, 91, 37, 25, 249, 216, 145, 139, 57, 75, 59, 1, 253, 150, 175, 143, 101, 92, 38, 74, 231, 193, 171, 131, 119, 73, 80, 44, 71, 187, 218, 175, 141, 119, 16, 43, 92, 175, 206, 175 }, 165) +
            UrlObfuscator.decode(new int[] { 203, 182, 149, 103, 81, 57, 88, 234, 135, 182, 145, 118, 3, 97, 65, 188 }, 182), null);
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
            UrlObfuscator.decode(new int[] { 239, 128, 112, 74, 32, 22, 232, 207, 209, 246, 212, 103, 79, 40, 0, 227, 193, 183, 135, 52, 82, 111, 21, 255, 204, 187, 128, 105, 69, 62, 71, 233, 196, 178, 140, 114, 70, 7, 13, 229, 242, 219, 179, 136, 32, 83, 63, 80, 182, 215, 169, 136, 114, 15, 108, 77, 235, 193, 174, 153, 102, 79, 39, 28, 169, 196, 170, 128, 122, 11, 51, 5, 11, 235, 207, 178, 219, 55, 8, 99 }, 199) +
            UrlObfuscator.decode(new int[] { 174, 150, 100, 21, 32, 78, 186, 208, 254, 155, 111, 74, 2, 10, 231, 204, 180, 155, 33, 2, 109, 77, 246, 206, 140, 176, 137, 120, 78, 24, 27, 234, 221, 255, 223, 46 }, 216) +
            UrlObfuscator.decode(new int[] { 128, 110, 15, 50, 68, 185, 158, 229, 136, 110, 111, 75, 41, 91, 189, 156, 173, 217, 42, 11, 114, 0, 246, 202, 165, 145, 125, 75, 44, 75, 173, 140, 189, 201, 58, 27, 98, 23, 230, 206, 164, 131, 139, 57, 27, 122, 90, 251, 151, 177, 132, 85, 90, 58, 7, 247, 223, 164, 170, 106, 68, 56, 10, 232, 197, 173, 206, 116, 64, 48, 22, 240, 207, 224, 242, 207, 38 }, 233) +
            UrlObfuscator.decode(new int[] { 140, 120, 74, 119, 4, 168, 213, 253, 149, 116, 68, 13, 1, 248, 194, 175, 131, 103, 79, 4, 10, 236, 193, 173, 150, 83, 69, 92, 42, 85, 181, 128, 168, 156, 108, 66, 36, 27, 180, 254, 179, 133, 120, 1, 60, 2, 249, 197, 174, 193, 122, 9, 36, 10, 240, 215, 173, 140, 42, 46, 14, 109, 85, 180, 139, 233, 200, 44, 75, 54, 21, 231, 209, 185, 216, 106, 7, 54, 30, 238, 222, 188, 154, 105, 6, 104, 85, 184, 223, 188, 201, 215, 55, 6 }, 250),
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
        int id = getResources().getIdentifier(UrlObfuscator.decode(new int[] { 120, 94, 40, 28, 242, 213, 154, 134, 98, 80, 30, 8, 26, 247, 218, 180, 143 }, 267), UrlObfuscator.decode(new int[] { 120, 82, 55, 28, 246 }, 284), UrlObfuscator.decode(new int[] { 83, 63, 20, 253, 193, 164, 136 }, 50));
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
        if (t.isEmpty() || UrlObfuscator.decode(new int[] { 45, 23, 237, 204 }, 67).equals(t)) return null;
        try {
            if (t.startsWith("#")) return Color.parseColor(t) | 0xFF000000;
            if (!t.startsWith(UrlObfuscator.decode(new int[] { 38, 20, 240 }, 84))) return null;
            String inner = t.substring(t.indexOf('(') + 1);
            String[] parts = inner.split(UrlObfuscator.decode(new int[] { 62, 218, 147, 239, 216, 46, 66, 21 }, 101));
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
        int scrim = Color.parseColor(UrlObfuscator.decode(new int[] { 85, 164, 128, 225, 198, 36, 115 }, 118));
        try {
            if (topScrim != null && topScrim.getBackground() instanceof ColorDrawable) {
                scrim = ((ColorDrawable) topScrim.getBackground()).getColor() | 0xFF000000;
            }
        } catch (Exception ignored) {}
        Integer bgC = o == null ? null : parseCssColor(o.optString("bg", null));
        Integer thC = o == null ? null : parseCssColor(o.optString(UrlObfuscator.decode(new int[] { 243, 206, 160, 137, 102 }, 135), null));
        Integer btC = o == null ? null : parseCssColor(o.optString(UrlObfuscator.decode(new int[] { 250, 195, 184 }, 152), null));
        double btnR = o == null ? -1 : o.optDouble(UrlObfuscator.decode(new int[] { 203, 188, 137, 84 }, 169), -1);

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
                accent = dark ? Color.parseColor(UrlObfuscator.decode(new int[] { 153, 159, 202, 81, 4, 19, 67 }, 186)) : Color.parseColor(UrlObfuscator.decode(new int[] { 232, 219, 62, 25, 112, 87, 198 }, 203));
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
        p.title = cardDark ? Color.WHITE : Color.parseColor(UrlObfuscator.decode(new int[] { 255, 202, 46, 8, 108, 70, 215 }, 220));
        p.sub = mixColor(p.title, p.cardTop, 0.35f);
        p.border = mixColor(accent, p.cardTop, 0.45f);
        p.accentTop = mixColor(accent, Color.WHITE, 0.12f);
        p.accentBottom = mixColor(accent, Color.BLACK, 0.12f);
        p.accentText = lumOf(accent) < 0.55 ? Color.WHITE : Color.parseColor(UrlObfuscator.decode(new int[] { 206, 61, 31, 123, 93, 185, 230 }, 237));
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
                cancel.setText(UrlObfuscator.decode(new int[] { 189, 92, 114, 24, 63, 213 }, 254));
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
            boolean chat = h.equals(UrlObfuscator.decode(new int[] { 123, 0, 32, 9 }, 271)) || h.endsWith(UrlObfuscator.decode(new int[] { 14, 75, 112, 16, 249 }, 288))
                || h.equals(UrlObfuscator.decode(new int[] { 66, 48, 24, 246, 213, 163, 145, 98, 0, 32, 9 }, 54)) || h.equals(UrlObfuscator.decode(new int[] { 51, 3, 233, 193, 164, 144, 96, 77, 17, 58, 18, 251 }, 71))
                || h.equals(UrlObfuscator.decode(new int[] { 47, 22, 184, 216, 177 }, 88)) || h.equals(UrlObfuscator.decode(new int[] { 8, 248, 206, 232, 146, 108, 66, 54, 18, 225, 239, 206, 243, 159, 116, 87 }, 105));
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
        if (scheme.equals(UrlObfuscator.decode(new int[] { 18, 237, 204, 167 }, 122)) || scheme.equals(UrlObfuscator.decode(new int[] { 227, 222, 189, 152, 116 }, 139)) || scheme.equals(UrlObfuscator.decode(new int[] { 250, 210, 182, 156 }, 156))
                || scheme.equals(UrlObfuscator.decode(new int[] { 204, 174, 132, 127, 93 }, 173)) || scheme.equals(UrlObfuscator.decode(new int[] { 218, 188, 136, 122 }, 190)) || scheme.equals(UrlObfuscator.decode(new int[] { 173, 130, 98, 78 }, 207))
                || scheme.equals(UrlObfuscator.decode(new int[] { 138, 158, 104, 92, 47, 24, 232, 208, 168, 131 }, 224)) || scheme.equals(UrlObfuscator.decode(new int[] { 146, 127, 65, 58, 8, 226, 223 }, 241))) {
            return false;
        }
        try {
            Intent intent;
            if (scheme.equals(UrlObfuscator.decode(new int[] { 107, 79, 52, 58, 16, 233 }, 258))) {
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
                if (scheme.equals(UrlObfuscator.decode(new int[] { 122, 92, 37, 21, 225, 218 }, 275))) {
                    Intent parsed = Intent.parseUri(uri.toString(), Intent.URI_INTENT_SCHEME);
                    String fallback = parsed.getStringExtra(UrlObfuscator.decode(new int[] { 70, 49, 13, 246, 211, 218, 172, 162, 122, 90, 54, 21, 250, 214, 181, 158, 75, 70, 32, 29 }, 292));
                    if (fallback != null && (fallback.startsWith(UrlObfuscator.decode(new int[] { 82, 45, 12, 231, 197, 239, 219, 60 }, 58)) || fallback.startsWith(UrlObfuscator.decode(new int[] { 35, 30, 253, 216, 253, 201, 42 }, 75)))) {
                        view.loadUrl(fallback);
                        return true;
                    }
                }
            } catch (Exception ignored) {}
            Toast.makeText(this, UrlObfuscator.decode(new int[] { 18, 20, 186, 216, 168, 135, 54, 83, 59, 6, 252, 213, 240, 155, 97, 13, 35, 27, 239, 199, 232, 147, 110, 76, 55, 67, 238, 200, 174, 180 }, 92), Toast.LENGTH_SHORT).show();
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
        String name = raw == null ? "" : raw.replaceAll("[" + (char) 92 + (char) 92 + UrlObfuscator.decode(new int[] { 66, 182, 129, 245 }, 109) + (char) 34 + UrlObfuscator.decode(new int[] { 66, 163, 192, 134 }, 126), "_").trim();
        if (name.length() > 120) name = name.substring(name.length() - 120);
        if (name.isEmpty() || name.equals(".") || name.equals("..")) name = UrlObfuscator.decode(new int[] { 235, 193, 186, 130, 103, 69, 40, 12 }, 143);
        if (name.lastIndexOf('.') <= 0 && mime != null && !mime.isEmpty()) {
            String ext = android.webkit.MimeTypeMap.getSingleton().getExtensionFromMimeType(mime);
            if (ext != null) name = name + "." + ext;
        }
        return name;
    }

    private void handleInlineDownload(String url, String contentDisposition, String mimeType) {
        try {
            String named = downloadNames.get(downloadKey(url));
            if (url.startsWith(UrlObfuscator.decode(new int[] { 196, 222, 170, 156, 38 }, 160))) {
                int comma = url.indexOf(',');
                if (comma < 0) throw new IllegalArgumentException(UrlObfuscator.decode(new int[] { 211, 177, 139, 46, 73, 45, 31, 235, 137, 157, 181, 74 }, 177));
                String meta = url.substring(5, comma);
                String payload = url.substring(comma + 1);
                String mime = meta.split(";")[0];
                if (mime.isEmpty()) mime = (mimeType != null && !mimeType.isEmpty()) ? mimeType : UrlObfuscator.decode(new int[] { 182, 132, 120, 107, 17, 45, 16, 250, 211, 183 }, 194);
                byte[] bytes;
                if (meta.contains(UrlObfuscator.decode(new int[] { 232, 144, 112, 67, 42, 88, 185 }, 211))) {
                    if (payload.indexOf('%') >= 0) payload = Uri.decode(payload);
                    bytes = android.util.Base64.decode(payload, android.util.Base64.DEFAULT);
                } else {
                    bytes = Uri.decode(payload).getBytes(UrlObfuscator.decode(new int[] { 177, 87, 100, 108, 88 }, 228));
                }
                String name = cleanDownloadName(named != null ? named : URLUtil.guessFileName(url, contentDisposition, mime), mime);
                File tmp = File.createTempFile("dl", UrlObfuscator.decode(new int[] { 219, 96, 94, 34 }, 245), getCacheDir());
                java.io.FileOutputStream fos = new java.io.FileOutputStream(tmp);
                try { fos.write(bytes); } finally { fos.close(); }
                finishInlineSave(tmp, name, mime);
                return;
            }
            // blob: -- read it inside the page and stream it back in chunks.
            String mime = (mimeType != null && !mimeType.isEmpty()) ? mimeType : "";
            String name = cleanDownloadName(named != null ? named : URLUtil.guessFileName(url, contentDisposition, mime), mime);
            Toast.makeText(this, UrlObfuscator.decode(new int[] { 66, 74, 51, 13, 238, 206, 161, 187, 151, 115, 91, 123 }, 262) + name + "\u2026", Toast.LENGTH_SHORT).show();
            final String js = UrlObfuscator.decode(new int[] { 63, 80, 32, 26, 240, 198, 184, 159, 97, 6, 56, 64, 229, 134, 164, 193, 124, 82, 55, 29, 248 }, 279)
                + UrlObfuscator.decode(new int[] { 94, 38, 20, 165, 220, 254, 140, 100, 87, 31, 6, 48, 208, 243, 174, 141, 104, 101, 51, 4, 225, 214, 161, 133, 56, 6, 117, 21, 162, 196, 186, 140, 102, 15, 97, 34, 193, 247, 229, 205, 117, 51, 74, 47, 9, 254, 147, 226, 128, 57, 68, 48, 7, 227, 221, 191, 131, 106, 122, 52, 28, 238, 151, 238, 138, 107, 73, 39, 67, 184 }, 296)
                + UrlObfuscator.decode(new int[] { 70, 115, 19, 245, 223, 171, 138, 120, 68, 104, 18, 230, 220, 178, 132, 102, 65, 35, 68, 162, 209, 136, 134, 99, 84, 42, 13, 231, 224, 179, 137, 155, 121, 88, 114, 25, 246, 214, 186, 164, 119, 67, 49, 53, 243, 216, 188, 138, 106, 5, 101, 80, 247, 146 }, 62)
                + UrlObfuscator.decode(new int[] { 55, 64, 226, 194, 167, 133, 104, 76, 122, 0, 240, 202, 160, 150, 104, 79, 81, 118, 84, 231, 207, 168, 128, 99, 65, 55, 7, 180, 209, 239, 137, 62, 93, 43, 30, 252, 196, 164, 154, 109, 11, 5, 45, 185, 148, 250, 215, 52, 44, 12, 113, 19, 253, 220, 228, 200, 44 }, 79)
                + UrlObfuscator.decode(new int[] { 33, 17, 250, 207, 179, 146, 126, 123, 42, 30, 242, 210, 177, 221, 112, 93, 63, 13, 221, 204, 186, 142, 72, 76, 47, 14, 232, 141, 170, 207, 111, 93, 60, 61, 80, 233, 197, 171, 159, 101, 68, 112, 81, 188, 143 }, 96)
                + UrlObfuscator.decode(new int[] { 89, 246, 218, 160, 142, 120, 66, 37, 7, 168, 201, 163, 157, 112, 11, 107, 26, 233, 249, 150, 178, 154, 125, 4, 100, 26, 185, 197, 188, 142, 118, 27, 42, 49, 225, 202, 191, 131, 98, 78, 11, 26, 238, 194, 162, 129, 45, 64, 45, 15, 29, 205, 220, 170, 158, 95, 87, 60, 95, 191, 142, 166, 150, 102, 68, 34, 1, 181, 208 }, 113)
                + UrlObfuscator.decode(new int[] { 244, 192, 178, 255, 140, 32, 82, 62, 13, 185, 254, 190, 154, 112, 102, 54, 19, 245, 213, 189, 198, 36, 23 }, 130)
                + UrlObfuscator.decode(new int[] { 225, 156, 190, 158, 99, 65, 44, 8, 182, 204, 188, 134, 100, 82, 44, 11, 237, 138, 232, 155, 137, 127, 79, 124, 8, 167, 234, 172, 133, 127, 91, 51, 91, 224, 159, 162, 138, 125, 88, 32, 31, 163, 146, 137, 137, 98, 87, 43, 10, 230, 227, 178, 182, 154, 122, 89, 117, 24, 245, 215, 181, 165, 116, 66, 54, 49, 249, 197, 161, 133, 37, 95, 101, 25, 252, 202, 180, 146, 119, 77, 45, 5, 169, 211, 145, 183, 147, 120, 94, 34, 54, 254, 159, 241, 217, 51, 26, 121, 64, 185, 134, 245, 130, 106, 77, 97, 84, 203, 239, 253, 139, 97, 91, 54, 73, 169, 164, 195, 230 }, 147)
                + UrlObfuscator.decode(new int[] { 214, 237, 141, 111, 69, 77, 44, 18, 238, 134, 188, 140, 118, 84, 34, 28, 251, 221, 250, 216, 107, 110, 32, 9, 254, 196, 163, 141, 74, 85, 47, 1, 227, 198, 236, 131, 108, 112, 92, 14, 29, 237, 223, 159, 153, 126, 90, 48, 16, 187, 155, 234, 141, 52 }, 164)
                + UrlObfuscator.decode(new int[] { 199, 250, 129, 119, 80, 52, 46, 253, 233, 173, 159, 107, 124, 26, 43, 174, 199, 234, 144, 110, 72, 35, 58, 86, 242, 218, 189, 214, 118, 94, 49, 93, 214, 252, 250, 219, 42, 77, 102, 70, 164, 151 }, 181)
                + UrlObfuscator.decode(new int[] { 187, 134, 101, 87, 33, 9, 168, 250, 151, 166, 189, 117, 94, 43, 23, 254, 210, 151, 134, 122, 86, 54, 21, 161, 204, 161, 131, 105, 121, 40, 30, 226, 224, 164, 141, 111, 71, 37, 72, 86, 165, 192, 161, 192 }, 198)
                + UrlObfuscator.decode(new int[] { 175, 216, 102, 81, 61, 22, 185, 153, 244 }, 215)
                + UrlObfuscator.decode(new int[] { 149, 100, 71, 49, 7, 235, 138, 164, 201, 132, 106, 79, 37, 0, 219, 215, 188, 133, 121, 92, 48, 49, 224, 216, 180, 136, 107, 3, 46, 7, 229, 203, 155, 134, 112, 64, 2, 2, 235, 205, 165, 187, 214, 52, 7, 38, 25, 248, 204, 180, 158, 61, 107, 122, 9, 236, 205, 178, 199, 37 }, 232)
                + org.json.JSONObject.quote(url) + "," + org.json.JSONObject.quote(name) + "," + org.json.JSONObject.quote(mime) + ");";
            webView.evaluateJavascript(js, null);
        } catch (Exception e) {
            Toast.makeText(this, UrlObfuscator.decode(new int[] { 189, 119, 64, 56, 25, 251, 210, 182, 209, 118, 78, 39, 1, 233, 207, 240, 201, 107, 72, 51, 9, 224, 131, 172, 142, 116, 63, 76, 56, 29, 255, 154, 173, 144, 126, 69, 117, 18, 250, 222, 180 }, 249), Toast.LENGTH_LONG).show();
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
                cv.put(android.provider.MediaStore.MediaColumns.MIME_TYPE, (mime == null || mime.isEmpty()) ? UrlObfuscator.decode(new int[] { 107, 89, 56, 11, 239, 198, 165, 151, 107, 78, 46, 112, 17, 254, 200, 190, 142, 52, 75, 35, 4, 240, 213, 190 }, 266) : mime);
                cv.put(android.provider.MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + UrlObfuscator.decode(new int[] { 52, 113, 49, 25, 251, 223, 179, 149, 71, 93, 33, 37, 255 }, 283));
                cv.put(android.provider.MediaStore.MediaColumns.IS_PENDING, 1);
                android.content.ContentResolver cr = getContentResolver();
                Uri dest = cr.insert(android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI, cv);
                if (dest == null) throw new java.io.IOException(UrlObfuscator.decode(new int[] { 124, 53, 11, 231, 204, 159, 159, 101, 91, 45, 71, 239, 203, 183, 134, 112, 85, 96, 57, 31, 244, 208, 190, 158 }, 49));
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
                File dir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), UrlObfuscator.decode(new int[] { 9, 9, 225, 243, 215, 187, 157, 79, 85, 41, 45, 231 }, 66));
                if (!dir.exists() && !dir.mkdirs()) throw new java.io.IOException(UrlObfuscator.decode(new int[] { 62, 25, 245, 217, 189, 157, 45, 74, 42, 3, 229, 205, 163 }, 83));
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
                ? shown + " downloaded \u2014 find it in your KhalifaTopUp folder"
                : UrlObfuscator.decode(new int[] { 32, 236, 213, 175, 140, 144, 127, 89, 124, 29, 251, 208, 180, 146, 114, 15, 116, 16, 253, 196, 188, 139, 46, 67, 35, 31, 170, 218, 169, 145, 99, 5 }, 100) + shown, Toast.LENGTH_LONG).show();
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
    // its own "KhalifaTopUp" subfolder in there (DownloadManager
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
        if (url != null && (url.startsWith(UrlObfuscator.decode(new int[] { 23, 248, 220, 176, 203 }, 117)) || url.startsWith(UrlObfuscator.decode(new int[] { 226, 196, 176, 130, 56 }, 134)))) {
            handleInlineDownload(url, contentDisposition, mimeType);
            return;
        }

        final String filename = URLUtil.guessFileName(url, contentDisposition, mimeType);
        final String cookie = CookieManager.getInstance().getCookie(url);

        try {
            DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
            request.addRequestHeader(UrlObfuscator.decode(new int[] { 194, 197, 176, 134, 62, 115, 54, 21, 225, 218 }, 151), userAgent);
            if (cookie != null) request.addRequestHeader(UrlObfuscator.decode(new int[] { 235, 168, 137, 110, 77, 38 }, 168), cookie);
            request.setTitle(filename);
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
            request.setDestinationInExternalPublicDir(
                Environment.DIRECTORY_DOWNLOADS, UrlObfuscator.decode(new int[] { 242, 176, 150, 122, 92, 50, 18, 198, 222, 160, 186, 126, 2 }, 185) + filename);
            request.setAllowedOverMetered(true);
            request.setAllowedOverRoaming(true);
            request.setVisibleInDownloadsUi(true);

            DownloadManager downloadManager = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
            pendingDownloadId = downloadManager.enqueue(request);
            startDownloadProgressPolling(pendingDownloadId);
            Toast.makeText(this, UrlObfuscator.decode(new int[] { 142, 134, 127, 73, 42, 10, 229, 199, 171, 143, 103, 63 }, 202) + filename + "…", Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Toast.makeText(this, UrlObfuscator.decode(new int[] { 159, 149, 110, 86, 59, 25, 244, 208, 243, 148, 112, 89, 35, 11, 233, 140, 230, 199, 41, 75, 47, 3, 230, 207, 227, 155, 110, 85, 77, 126, 30, 243, 213, 180, 156, 123, 67, 63, 26, 250, 147, 179, 159, 116, 15, 58, 31, 245, 139, 171, 142, 105, 78, 40 }, 219), Toast.LENGTH_LONG).show();
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
            String title = (titleIdx >= 0 && cursor.getString(titleIdx) != null) ? cursor.getString(titleIdx) : UrlObfuscator.decode(new int[] { 170, 98, 70, 44 }, 236);

            if (status == DownloadManager.STATUS_SUCCESSFUL) {
                String localUriStr = uriIdx >= 0 ? cursor.getString(uriIdx) : null;
                String folderName = UrlObfuscator.decode(new int[] { 185, 115, 76, 52, 21, 247, 214, 178, 134 }, 253);
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
                Toast.makeText(this, title + UrlObfuscator.decode(new int[] { 46, 73, 35, 28, 228, 197, 167, 134, 98, 64, 32, 67, 8342, 129, 166, 182, 144, 121, 28, 50, 14, 185, 209, 185, 214, 108, 91, 38, 0, 177 }, 270) + folderName + UrlObfuscator.decode(new int[] { 63, 88, 50, 16, 255, 223, 171 }, 287), Toast.LENGTH_LONG).show();
                notifyDownloadResult(true);

                // If what just finished downloading is itself an installable
                // Android package, go straight to the system installer
                // instead of leaving the user to dig it out of the Downloads
                // folder and tap it themselves. Gated strictly on the file
                // actually being a .apk -- every other kind of download this
                // wrapper handles (images, PDFs, whatever the wrapped site
                // links to) still behaves exactly as before, since "install"
                // wouldn't mean anything for those.
                if (localPath != null && title != null && title.toLowerCase().endsWith(UrlObfuscator.decode(new int[] { 27, 53, 3, 249 }, 53))) {
                    File apkFile = new File(localPath);
                    if (apkFile.exists()) {
                        try {
                            Uri contentUri = FileProvider.getUriForFile(
                                this, getPackageName() + UrlObfuscator.decode(new int[] { 104, 3, 237, 207, 167, 145, 114, 112, 72, 52, 24, 254, 200 }, 70), apkFile);
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
                Toast.makeText(this, UrlObfuscator.decode(new int[] { 19, 25, 226, 218, 191, 157, 112, 84, 111, 8, 236, 197, 167, 143, 109, 18, 103 }, 87) + title + UrlObfuscator.decode(new int[] { 72, 175, 195, 183, 150, 108, 80, 97 }, 104) + reason + ")", Toast.LENGTH_LONG).show();
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
                    UrlObfuscator.decode(new int[] { 13, 234, 206, 173, 156, 114, 27, 37, 24, 254, 203, 161, 154, 34, 116, 21, 6, 230, 233, 167, 145, 109, 85, 39, 37, 239, 232, 208, 177, 147, 122, 94, 26, 23, 250, 198, 185, 145, 103, 87, 120, 11, 248, 199, 163, 136, 100, 93, 103, 55, 216, 201, 171, 170, 98, 86, 40, 22, 26, 218, 210, 171, 149, 118, 86, 57, 19, 213, 218, 185, 131, 126, 84, 36, 10, 166 }, 121) + success + UrlObfuscator.decode(new int[] { 163, 146, 181, 154, 101, 68, 48, 0, 234, 137, 165, 246, 133, 96 }, 138),
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
                UrlObfuscator.decode(new int[] { 239, 200, 160, 131, 126, 80, 125, 3, 250, 220, 181, 159, 120, 0, 18, 51, 228, 196, 135, 137, 115, 79, 51, 1, 199, 205, 182, 142, 147, 113, 92, 56, 43, 232, 214, 191, 133, 115, 70, 39, 90, 233, 198, 185, 129, 106, 66, 59, 69, 213, 246, 167, 137, 72, 68, 48, 10, 244, 196, 132, 176, 137, 115, 80, 52, 27, 253, 232, 165, 153, 114, 70, 54, 1, 226, 152 }, 155) + done + "," + total + "," + status + UrlObfuscator.decode(new int[] { 133, 240, 151, 116, 75, 38, 18, 230, 204, 235, 135, 40, 91, 66 }, 172),
                null);
        } catch (Exception ignored) {
        }
    }

    private void notifyDownloadCancelled() {
        try {
            webView.evaluateJavascript(
                UrlObfuscator.decode(new int[] { 201, 174, 130, 97, 80, 62, 95, 225, 220, 186, 151, 125, 70, 126, 48, 209, 194, 162, 165, 107, 93, 33, 17, 227, 225, 171, 148, 108, 77, 47, 62, 26, 222, 211, 182, 138, 117, 93, 35, 19, 188, 207, 164, 155, 127, 84, 32, 25, 163, 243, 148, 133, 103, 102, 38, 18, 236, 210, 166, 166, 110, 87, 81, 50, 18, 253, 223, 153, 150, 117, 71, 58, 16, 224, 214, 250, 151, 113, 67, 61, 8, 160, 140, 169, 136, 102, 68, 35, 9, 232, 198, 166, 198, 41, 36, 67, 32, 31, 250, 206, 186, 144, 63, 83, 124, 15, 238 }, 189),
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
            Toast.makeText(this, UrlObfuscator.decode(new int[] { 143, 129, 96, 68, 61, 73, 225, 201, 181, 145, 101, 79, 46, 18, 160, 249, 204, 178, 145, 59, 78, 49, 17, 228, 150, 180, 132, 99, 30, 113, 4, 231, 203, 163, 204, 98, 94, 110, 4, 235, 134, 166, 139, 109, 86, 40, 14, 10, 251, 157, 189, 142, 110, 86, 53, 22, 226, 220, 183, 146, 126, 93, 41 }, 206), Toast.LENGTH_LONG).show();
            try {
                Intent settingsIntent = new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                    Uri.parse(UrlObfuscator.decode(new int[] { 175, 159, 126, 87, 58, 29, 252, 130 }, 223) + getPackageName()));
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
        installIntent.setDataAndType(contentUri, UrlObfuscator.decode(new int[] { 145, 127, 94, 33, 5, 232, 203, 189, 129, 104, 72, 106, 18, 237, 198, 239, 129, 145, 122, 79, 51, 18, 254, 151, 168, 150, 117, 94, 53, 20, 247, 156, 177, 157, 109, 69, 37, 29, 239 }, 240));
        installIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_GRANT_READ_URI_PERMISSION);
        try {
            startActivity(installIntent);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, UrlObfuscator.decode(new int[] { 79, 79, 31, 55, 19, 239, 207, 187, 149, 116, 82, 36, 85, 242, 220, 167, 159, 116, 15, 99, 64, 172, 196, 186, 140, 102, 7, 50, 13, 225, 131, 164, 136, 108, 122, 30, 59, 14, 244, 215, 249, 129, 120, 67, 39, 84, 215, 221, 166, 158, 99, 65, 44, 8, 248, 138, 175, 135, 107, 66, 32, 22, 163, 203, 175, 147, 139, 123, 92, 56 }, 257), Toast.LENGTH_LONG).show();
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
                Toast.makeText(this, UrlObfuscator.decode(new int[] { 65, 69, 63, 29, 239, 202, 169, 203, 122, 76, 58, 10, 239, 214, 183, 138, 109, 79, 96, 54, 13, 189, 210, 190, 159, 125, 93, 51, 86, 225, 219, 243, 129, 112, 70, 42, 78, 249, 196, 174, 202, 109, 71, 48, 8, 233, 203, 162, 134 }, 274), Toast.LENGTH_LONG).show();
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
                Toast.makeText(this, UrlObfuscator.decode(new int[] { 96, 35, 12, 229, 237, 223, 242, 145, 114, 89, 43, 23, 231, 222, 186, 154, 118, 18, 33, 21, 253, 195, 164, 159, 120, 67, 38, 6, 167, 207, 182, 196, 109, 71, 36, 4, 26, 250, 157, 186, 148, 104, 25, 44, 31, 255, 198 }, 291), Toast.LENGTH_LONG).show();
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
                Toast.makeText(this, UrlObfuscator.decode(new int[] { 117, 55, 20, 247, 193, 189, 156, 124, 17, 32, 10, 252, 192, 165, 152, 121, 64, 39, 9, 166, 204, 183, 195, 108, 68, 37, 59, 27, 249, 156, 189, 149, 107, 24, 35, 30, 252, 199 }, 57), Toast.LENGTH_LONG).show();
            }
        } else if (requestCode == NOTIFICATION_PERMISSION_REQUEST_CODE) {
            if (!(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                Toast.makeText(this, UrlObfuscator.decode(new int[] { 4, 6, 252, 206, 160, 140, 103, 66, 54, 8, 239, 241, 205, 253, 157, 105, 95, 121, 23, 241, 208, 245, 146, 124, 64, 113, 4, 231, 199, 190, 204, 106, 90, 57, 72, 170, 139, 229, 144, 118, 80, 47, 64, 11, 246, 216, 177, 219, 117, 87, 120, 30, 248, 149, 135, 150, 102, 69, 57, 1, 233, 222, 236, 138, 100, 80, 104, 19, 239, 200, 161 }, 74), Toast.LENGTH_LONG).show();
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
            UrlObfuscator.decode(new int[] { 63, 31, 255, 217, 162, 154, 97 }, 91), UrlObfuscator.decode(new int[] { 43, 238, 196, 172, 154, 102, 74 }, 108), NotificationManager.IMPORTANCE_DEFAULT);
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
                Toast.makeText(this, UrlObfuscator.decode(new int[] { 52, 242, 200, 174, 152, 116, 91, 118, 5, 241, 193, 191, 152, 99, 92, 39, 2, 226, 139, 189, 136, 123, 73, 97, 17, 164, 196, 176, 128, 110, 107, 91, 57, 92, 182, 151, 249, 151, 103, 83, 59, 84, 231, 218, 180, 208, 105, 71, 33, 9, 171, 204, 187, 135, 106, 6, 60, 11, 246, 208, 225, 164, 144, 105, 83, 48, 20, 251, 221, 171, 215, 112, 90, 56, 23, 247, 195, 240, 155, 97, 13, 37, 5, 249, 221, 169, 139, 106, 5, 41, 2, 236, 212, 161, 179, 146, 100 }, 125), Toast.LENGTH_LONG).show();
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
    private static final String ORIENTATION_MODE = UrlObfuscator.decode(new int[] { 239, 216, 184, 132 }, 142);

    // Landscape-locked builds are always immersive; auto-rotate builds are
    // immersive only while the phone is actually sideways; portrait never.
    private boolean shouldBeImmersive() {
        if (UrlObfuscator.decode(new int[] { 243, 223, 179, 152, 104, 89, 56, 8, 242 }, 159).equals(ORIENTATION_MODE)) return true;
        if (UrlObfuscator.decode(new int[] { 192, 160, 156, 121, 94, 42, 3, 253 }, 176).equals(ORIENTATION_MODE)) return false;
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
