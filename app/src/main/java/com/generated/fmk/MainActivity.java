package com.generated.fmk;

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
    private ValueCallback<Uri[]> filePathCallback;
    // See the WebViewCompat.addDocumentStartJavaScript call in onCreate and
    // the onPageStarted fallback below for why this exists in two places.
    private boolean documentStartScriptSupported = false;
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
        UrlObfuscator.decode(new int[] { 104, 57, 11, 243, 223, 175, 147, 118, 86, 127, 95, 238 }, 64) +
        UrlObfuscator.decode(new int[] { 56, 22, 167, 217, 164, 130, 111, 69, 62, 70, 244, 206, 170, 147, 76, 82, 36, 14, 57, 247, 209, 185, 171, 115, 90, 51, 18, 228, 156, 166, 150, 102, 68, 34, 1, 181 }, 81) +
        UrlObfuscator.decode(new int[] { 21, 232, 206, 219, 177, 138, 50, 72, 50, 22, 239, 248, 166, 144, 122, 117, 59, 29, 245, 255, 167, 142, 103, 78, 56, 84, 238, 210, 168, 134, 112, 74, 45, 15, 168, 240, 206, 169, 143, 50, 65 }, 98) +
        UrlObfuscator.decode(new int[] { 28, 226, 197, 163, 210, 97, 93, 56, 24, 246, 213, 179, 154, 61 }, 115) +
        UrlObfuscator.decode(new int[] { 246, 198, 182, 148, 114, 113, 30, 51, 25, 236, 154, 137, 138, 120, 91, 60, 7, 246, 154, 183, 133, 97, 77, 57, 5, 228, 196, 225, 154, 98, 85, 42, 8, 245, 199, 237, 146, 154, 116, 88, 63, 15, 179, 194 }, 132) +
        UrlObfuscator.decode(new int[] { 227, 213, 161, 210, 120, 94, 63, 27, 249, 145, 175, 133, 106, 93, 42, 3, 235, 208, 237, 129, 115, 69, 94, 42, 24, 217, 215, 191, 148, 125, 89, 34, 93, 179, 218, 188, 129, 101, 91, 105, 68, 183 }, 149) +
        UrlObfuscator.decode(new int[] { 207, 171, 148, 118, 86, 111, 20, 6, 238, 216, 225, 220, 124, 80, 52, 18, 177, 142 }, 166) +
        UrlObfuscator.decode(new int[] { 222, 176, 221, 123, 67, 38, 2, 190, 194, 187, 129, 120, 66, 58, 5, 237, 142, 175, 139, 116, 86, 54, 79, 237, 234, 210, 169, 149, 107, 86, 60, 69, 227, 196, 160, 145, 40 }, 183) +
        UrlObfuscator.decode(new int[] { 188, 149, 127, 94, 50, 2, 240, 129, 165, 167, 138, 110, 1, 0, 39, 162, 144, 184, 134, 97, 71, 125, 6, 232, 192, 170, 157, 113, 80, 16, 55, 160, 134, 161, 137, 119, 97, 34, 1, 233, 136, 217, 171, 147, 127, 79, 51, 22, 246, 159, 162, 220, 111 }, 200) +
        UrlObfuscator.decode(new int[] { 175, 153, 101, 22, 52, 73, 231, 148, 247, 132, 33, 79, 46, 15, 238, 218, 189, 211, 110, 64, 109, 69, 226, 139, 179, 133, 139, 107, 79, 50, 64 }, 217) +
        UrlObfuscator.decode(new int[] { 165, 107, 66, 34, 5, 241, 138, 168, 135, 120, 83, 23, 63, 84, 178, 221, 181, 139, 93, 86, 53, 29, 188, 213, 167, 159, 115, 91, 39, 2, 226, 131, 167, 128, 101, 66, 111, 30, 225, 219, 182, 146, 46, 111, 75, 46, 20, 179, 215, 176, 149, 114, 31, 110 }, 234) +
        UrlObfuscator.decode(new int[] { 211, 123, 98, 53, 30, 251, 208, 137, 143, 110, 106, 13, 70, 160, 203, 163, 153, 79, 72, 43, 15, 174, 195, 177, 141, 97, 85, 41, 48, 16, 181, 217, 163, 142, 48, 67, 50, 14, 225, 199, 253, 130, 100, 67, 39, 70, 232, 212, 191, 195, 50, 85, 110, 93, 248, 141, 248, 159, 40, 27 }, 251) +
        UrlObfuscator.decode(new int[] { 101, 77, 98, 12, 240, 211, 181, 203, 104, 70, 44, 6, 244, 247, 151, 180, 146, 107, 79, 45, 86, 246, 213, 182, 145, 99, 70, 108, 21, 247, 218, 190, 194, 97, 69, 32, 6, 175, 129, 233, 195, 42, 25, 60, 3, 30, 234, 222, 180, 211, 127, 16, 35, 10 }, 268) +
        UrlObfuscator.decode(new int[] { 116, 82, 43, 15, 237, 150, 164, 130, 108, 88, 54, 92, 245, 217, 188, 158, 97, 77, 50, 87, 174, 198, 168, 136, 96, 3, 120 }, 285) +
        UrlObfuscator.decode(new int[] { 87, 61, 18, 229, 194, 171, 131, 120, 5, 40, 6, 236, 222, 232, 132, 116, 83, 39, 15, 228, 220, 214, 180, 144, 127, 18, 48, 22, 231, 195, 161, 221, 40 }, 51) +
        UrlObfuscator.decode(new int[] { 45, 13, 242, 212, 180, 241, 159, 121, 88, 30, 12, 252, 214, 163, 186, 124, 71, 39, 23, 255, 213, 189, 198, 42, 79, 35, 11, 231, 207, 162, 193, 41, 66, 54, 12, 226, 212, 214, 177, 147, 52, 18, 33 }, 68) +
        UrlObfuscator.decode(new int[] { 33, 6, 234, 201, 181, 159, 108, 91, 32, 9, 229, 222, 231, 138, 104, 66, 60, 74, 241, 199, 172, 143, 137, 123, 126, 52, 18, 246, 221, 240, 158, 120, 69, 33, 7, 187, 138, 173, 140, 111, 89, 47, 3, 162, 204, 225, 156, 123 }, 85) +
        UrlObfuscator.decode(new int[] { 15, 227, 140, 226, 139, 111, 80, 74, 42, 83, 250, 210, 182, 156, 107, 75, 42, 84, 253, 221, 162, 132, 100, 1, 40, 4, 224, 206, 185, 199, 100, 66, 40, 2, 240, 203, 235, 154 }, 102) +
        UrlObfuscator.decode(new int[] { 1, 247, 199, 244, 150, 96, 67, 107, 27, 252, 212, 183, 142, 120, 91, 117, 9, 227, 210, 228, 167, 77, 108, 5, 39, 29, 248, 204, 175, 147, 118, 86, 127, 81, 193, 220, 182, 210, 100, 67, 42, 28, 173, 205, 169, 133, 123, 92, 34, 2, 165, 197, 227, 144, 100, 81, 74, 59, 14, 232, 149, 253, 213, 63, 118, 52, 26, 230, 199, 151, 131, 98, 64, 60, 74, 165, 144, 183 }, 119) +
        UrlObfuscator.decode(new int[] { 235, 198, 178, 134, 108, 11, 39, 72, 251, 250, 204, 175, 193, 117, 95, 46, 88, 210, 196, 167, 155, 97, 26, 118, 49, 237, 193, 191, 152, 78, 88, 59, 7, 245, 129, 236, 223, 126 }, 136) +
        UrlObfuscator.decode(new int[] { 235, 221, 189, 147, 118, 64, 123, 23, 227, 194, 230, 213, 127, 73, 63, 31, 251, 198, 252, 155 }, 153) +
        UrlObfuscator.decode(new int[] { 220, 168, 154, 39, 78, 36, 10, 231, 206, 164, 147, 194, 95, 79, 46, 26, 227, 151, 168, 133, 121, 65, 59, 7, 235, 193, 181, 193, 99, 76, 60, 69, 233, 200, 164, 139, 46, 76, 42, 19, 247, 213, 238, 185, 151, 113, 89, 40, 86, 255, 205, 185, 149, 97, 93, 60, 28, 185, 214, 166, 130, 104, 5, 48 }, 170) +
        UrlObfuscator.decode(new int[] { 201, 191, 141, 109, 69, 56, 14, 255, 218, 188, 149, 42, 8, 40, 4, 224, 206, 237, 197, 102, 70, 43, 0, 190, 197, 171, 141, 101, 49, 80, 60, 17, 254, 150, 190, 157, 99, 112, 60, 24, 246, 136, 183, 133, 97, 77, 57, 5, 228, 196, 225, 193, 124, 84, 32, 16, 246, 208, 175, 192, 175, 108, 82, 49, 18, 233, 220, 246, 133, 115, 70, 59, 31, 228, 212, 248, 137, 103, 65, 41, 66, 177, 212, 181, 220, 123, 12, 127 }, 187) +
        UrlObfuscator.decode(new int[] { 190, 142, 121, 70, 36, 17, 227, 141, 171, 147, 118, 82, 110, 50, 11, 241, 200, 178, 138, 117, 93, 104, 30, 244, 218, 183, 158, 116, 67, 117, 53, 229, 205, 165, 142, 101, 77, 52, 61, 181, 249, 158, 203, 58, 93, 22, 101 }, 204) +
        UrlObfuscator.decode(new int[] { 180, 146, 107, 79, 45, 86, 244, 218, 188, 151, 120, 26, 120, 75, 242, 135, 246, 145, 48 }, 221) +
        UrlObfuscator.decode(new int[] { 147, 37, 5, 98 }, 238);
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
            paint.setColor(Color.parseColor(UrlObfuscator.decode(new int[] { 220, 88, 15, 26, 73, 223, 252 }, 255)));
            canvas.drawCircle(cx, cy, stroke * 1.05f, paint);

            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(stroke);
            paint.setColor(Color.parseColor(UrlObfuscator.decode(new int[] { 51, 26, 43, 8, 237, 207, 254 }, 272)));
            float[] radii = {w * 0.24f, w * 0.38f};
            int[] alphas = {235, 130};
            for (int i = 0; i < radii.length; i++) {
                paint.setAlpha(alphas[i]);
                RectF arc = new RectF(cx - radii[i], cy - radii[i], cx + radii[i], cy + radii[i]);
                canvas.drawArc(arc, 208, 124, false, paint);
            }

            paint.setAlpha(255);
            paint.setColor(Color.parseColor(UrlObfuscator.decode(new int[] { 2, 6, 25, 72, 223, 137, 236 }, 289)));
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
            String name = (appName == null || appName.trim().isEmpty()) ? UrlObfuscator.decode(new int[] { 118, 38, 5 }, 55) : appName.trim().toUpperCase();

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
            letter.setTypeface(Typeface.create(UrlObfuscator.decode(new int[] { 59, 6, 232, 214, 233, 144, 103, 83, 41, 57, 83, 240, 217, 191, 147, 108, 85 }, 72), Typeface.NORMAL));
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
                videoResId = context.getResources().getIdentifier(UrlObfuscator.decode(new int[] { 58, 13, 228, 194, 186, 153, 76, 65, 33, 28, 238, 221, 165 }, 89), UrlObfuscator.decode(new int[] { 24, 232, 223 }, 106), pkg);
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
                int resId = context.getResources().getIdentifier(UrlObfuscator.decode(new int[] { 24, 239, 202, 172, 152, 123, 106, 39, 3, 254, 208, 163, 135 }, 123), UrlObfuscator.decode(new int[] { 232, 217, 171, 158, 105, 69, 42, 0 }, 140), pkg);
                if (resId != 0) {
                    iv.setImageURI(Uri.parse(UrlObfuscator.decode(new int[] { 252, 210, 191, 136, 118, 81, 51, 88, 231, 209, 160, 157, 100, 66, 44, 11, 183, 131, 228 }, 157) + pkg + "/" + resId));
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
                mp.setDataSource(context, Uri.parse(UrlObfuscator.decode(new int[] { 207, 163, 136, 121, 69, 32, 12, 169, 212, 160, 151, 108, 87, 51, 3, 26, 164, 146, 243 }, 174) + pkg + "/" + videoResId));
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
            int iconResId = context.getResources().getIdentifier(UrlObfuscator.decode(new int[] { 214, 189, 162, 112, 90, 47, 23, 251, 223, 179, 135 }, 191), UrlObfuscator.decode(new int[] { 189, 134, 126, 64, 45, 27 }, 208), context.getPackageName());
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
        root.setBackgroundColor(Color.parseColor(UrlObfuscator.decode(new int[] { 194, 48, 42, 14, 104, 76, 174 }, 225)));

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
        swipeRefresh.setColorSchemeColors(Color.parseColor(UrlObfuscator.decode(new int[] { 209, 35, 116, 121, 45, 201, 234 }, 242)));
        swipeRefresh.addView(webView, new ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        root.addView(swipeRefresh, webParams);

        // Status-bar and navigation-bar strips. Added before the splash /
        // error / navigation overlays so those still cover the whole screen.
        topScrim = new View(this);
        topScrim.setBackgroundColor(Color.parseColor(UrlObfuscator.decode(new int[] { 32, 18, 116, 80, 74, 174, 136 }, 259)));
        root.addView(topScrim, new FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT, 0, Gravity.TOP));
        bottomScrim = new View(this);
        bottomScrim.setBackgroundColor(Color.parseColor(UrlObfuscator.decode(new int[] { 55, 3, 103, 65, 165, 159, 251 }, 276)));
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

        final SplashController loading = new StaticIconSplashView(this, Color.parseColor(UrlObfuscator.decode(new int[] { 6, 116, 83, 178, 145, 240, 239 }, 293)));
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
        final String offlineScrimColor = offlineIsNightMode ? UrlObfuscator.decode(new int[] { 24, 99, 64, 168, 135, 230, 197, 36, 3 }, 59) : UrlObfuscator.decode(new int[] { 111, 93, 188, 153, 248, 215, 54, 21, 116 }, 76);
        // Slightly translucent (not fully opaque) card/button fills, so a
        // hint of the dimmed scrim behind still shows through -- a cheap
        // stand-in for a true frosted-glass blur, which would need
        // RenderEffect (API 31+) and a snapshot of whatever's behind the
        // card to do for real.
        final String offlineCardBg = offlineIsNightMode ? UrlObfuscator.decode(new int[] { 126, 57, 163, 136, 154, 202, 82, 5, 101 }, 93) : UrlObfuscator.decode(new int[] { 77, 207, 148, 141, 216, 79, 26, 1, 84 }, 110);
        final String offlineTitleColor = offlineIsNightMode ? UrlObfuscator.decode(new int[] { 92, 216, 251, 154, 189, 92, 127 }, 127) : UrlObfuscator.decode(new int[] { 179, 159, 254, 221, 60, 27, 122 }, 144);
        final String offlineSubtitleColor = offlineIsNightMode ? UrlObfuscator.decode(new int[] { 130, 130, 231, 188, 37, 126, 31 }, 161) : UrlObfuscator.decode(new int[] { 145, 228, 178, 58, 108, 120, 41 }, 178);
        final String offlineButtonBg = offlineIsNightMode ? UrlObfuscator.decode(new int[] { 224, 218, 66, 20, 7, 106, 69, 168, 250 }, 195) : UrlObfuscator.decode(new int[] { 247, 203, 81, 117, 102, 43, 184, 233, 141 }, 212);
        final float offlineDensity = getResources().getDisplayMetrics().density;

        // Outer full-screen scrim so the card reads as a dialog sitting on
        // top of the app, matching how a native "no connection" alert
        // dims everything behind it.
        final FrameLayout errorView = new FrameLayout(this);
        errorView.setBackgroundColor(Color.parseColor(offlineScrimColor));
        errorView.setVisibility(View.GONE);

        // The actual rounded card. Entrance/exit animations below target
        // this (not the full-screen scrim) so only the card itself pops
        // in/out while the dim layer just fades. Corner radius and padding
        // are tuned to match a real iOS-style alert card's proportions
        // (roughly 14dp radius, not an exaggerated "squircle") rather than
        // the more rounded, more padded first pass.
        final LinearLayout errorCard = new LinearLayout(this);
        errorCard.setOrientation(LinearLayout.VERTICAL);
        int cardPadH = (int) (18 * offlineDensity);
        int cardPadTop = (int) (14 * offlineDensity);
        int cardPadBottom = (int) (12 * offlineDensity);
        errorCard.setPadding(cardPadH, cardPadTop, cardPadH, cardPadBottom);
        GradientDrawable cardBg = new GradientDrawable();
        cardBg.setColor(Color.parseColor(offlineCardBg));
        cardBg.setCornerRadius(16 * offlineDensity);
        errorCard.setBackground(cardBg);
        errorCard.setElevation(6f * offlineDensity);
        FrameLayout.LayoutParams cardParams = new FrameLayout.LayoutParams(
            (int) (270 * offlineDensity), ViewGroup.LayoutParams.WRAP_CONTENT);
        cardParams.gravity = Gravity.CENTER;
        errorView.addView(errorCard, cardParams);

        final TextView errorTitle = new TextView(this);
        errorTitle.setText(UrlObfuscator.decode(new int[] { 171, 107, 3, 44, 4, 244, 232, 209, 175, 151 }, 229));
        errorTitle.setTextColor(Color.parseColor(offlineTitleColor));
        errorTitle.setTextSize(16);
        errorTitle.setTypeface(errorTitle.getTypeface(), Typeface.BOLD);
        errorTitle.setGravity(Gravity.START);
        errorCard.addView(errorTitle);

        final TextView errorSubtitle = new TextView(this);
        errorSubtitle.setText(UrlObfuscator.decode(new int[] { 166, 121, 81, 50, 1, 244, 144, 189, 139, 121, 94, 50, 74, 254, 192, 162, 136, 37, 71, 44, 12, 239, 197, 220, 170, 152, 120, 27, 46, 22, 184, 222, 184, 129, 113, 65, 60, 20, 228 }, 246));
        errorSubtitle.setTextColor(Color.parseColor(offlineSubtitleColor));
        errorSubtitle.setTextSize(13);
        errorSubtitle.setGravity(Gravity.START);
        LinearLayout.LayoutParams subtitleParams = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        subtitleParams.topMargin = (int) (4 * offlineDensity);
        subtitleParams.bottomMargin = (int) (13 * offlineDensity);
        errorCard.addView(errorSubtitle, subtitleParams);

        // Full-width pill button, the one actionable element on the card --
        // setOnTouchListener below scales + dims it slightly on press since
        // a bare TextView (unlike a Button) has no tap feedback of its own.
        // Label is plain (not accent-tinted) to match the reference, which
        // uses the same neutral title/button color throughout.
        final TextView retryText = new TextView(this);
        retryText.setText(UrlObfuscator.decode(new int[] { 85, 67, 49, 22, 250 }, 263));
        retryText.setTextColor(Color.parseColor(offlineTitleColor));
        retryText.setTextSize(14);
        retryText.setTypeface(retryText.getTypeface(), Typeface.BOLD);
        retryText.setGravity(Gravity.CENTER);
        int retryPadV = (int) (11 * offlineDensity);
        retryText.setPadding(0, retryPadV, 0, retryPadV);
        GradientDrawable retryPill = new GradientDrawable();
        retryPill.setColor(Color.parseColor(offlineButtonBg));
        retryPill.setCornerRadius(999f);
        retryText.setBackground(retryPill);
        LinearLayout.LayoutParams retryParams = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        errorCard.addView(retryText, retryParams);

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
        navOverlay.setBackgroundColor(Color.parseColor(UrlObfuscator.decode(new int[] { 59, 7, 102, 69, 164, 131, 226 }, 280)));
        navOverlay.setVisibility(View.GONE);
        final BouncingDotsView navDots = new BouncingDotsView(this, Color.parseColor(UrlObfuscator.decode(new int[] { 10, 14, 85, 192, 151, 129, 166 }, 297)));
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
                if (webView.canGoBack()) {
                    webView.goBack();
                } else {
                    setEnabled(false);
                    getOnBackPressedDispatcher().onBackPressed();
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
            errorCard.animate().scaleX(0.94f).scaleY(0.94f).setDuration(180).start();
            errorView.animate()
                .alpha(0f)
                .setDuration(180)
                .withEndAction(() -> {
                    errorView.setVisibility(View.GONE);
                    errorView.setAlpha(1f);
                    errorCard.setScaleX(1f);
                    errorCard.setScaleY(1f);
                }).start();
        };

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
        String spoofedUA = defaultUA.replace(UrlObfuscator.decode(new int[] { 4, 126, 10, 234 }, 63), "").replaceAll("Version/[0-9.]+\s", "");
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
        // dead giveaway it's a WebView -- native views don't do that.
        webView.setOverScrollMode(View.OVER_SCROLL_NEVER);
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
        webView.addJavascriptInterface(new AndroidBridge(), UrlObfuscator.decode(new int[] { 17, 1, 234, 223, 163, 130, 110, 107, 58, 14, 226, 194, 161 }, 80));

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

        errorView.setOnClickListener(v -> doRetry[0].run());
        errorView.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    retryText.animate().cancel();
                    retryText.animate().scaleX(0.94f).scaleY(0.94f).alpha(0.75f)
                        .setDuration(90).start();
                    break;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    retryText.animate().cancel();
                    retryText.animate().scaleX(1f).scaleY(1f).alpha(1f)
                        .setDuration(160).setInterpolator(new OvershootInterpolator(2.2f)).start();
                    break;
            }
            return false;
        });

        // Brings the WebView in with a little life instead of just
        // flipping it to VISIBLE at full size the instant the splash
        // clears -- a quick scale/fade/settle so the site's first frame
        // feels like it's arriving, not just appearing.
        final Runnable revealWebView = () -> {
            loading.hide();
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


            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                swipeRefresh.setRefreshing(false);
                navOverlay.animate().cancel();
                navOverlay.animate().alpha(0f).setDuration(180)
                    .withEndAction(() -> navOverlay.setVisibility(View.GONE)).start();
                if (hasError) return;
                revealWebView.run();
                hasLoadedOnce[0] = true;
                // Picks up the page's <meta name="theme-color"> (if it has
                // one) to recolor the system bars, and wires up a light
                // haptic tap on buttons/links -- both no-ops wrapped in
                // try/catch so a page that doesn't have a theme-color tag,
                // or that runs somewhere AndroidBridge isn't defined (the
                // popup WebView below doesn't get it), just silently skips
                // rather than throwing a JS error.
                view.evaluateJavascript(
                    UrlObfuscator.decode(new int[] { 73, 230, 234, 208, 190, 136, 114, 85, 55, 80, 190, 205 }, 97) +
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
                    UrlObfuscator.decode(new int[] { 6, 227, 201, 180 }, 114) +
                    "var vp=document.querySelector('meta[name=\"viewport\"]');" +
                    UrlObfuscator.decode(new int[] { 234, 196, 233, 193, 137, 110, 20, 39, 13, 234, 132, 188, 152, 117, 64, 57, 22, 252, 197, 254, 140, 124, 72, 45, 31, 239, 236, 164, 130, 107, 64, 42, 23, 170, 134, 173, 186, 138, 124, 27, 114, 65, 239, 200, 249, 133, 112, 64, 18, 6, 229, 194, 166, 140, 120, 88, 46, 66, 174, 198, 166, 139, 96, 3, 111, 69, 247, 201, 218, 169, 141, 115, 73, 46, 94, 177, 140, 178, 154, 119, 70, 63, 20, 254, 219, 224, 133, 105, 74, 46, 71, 233, 215, 182, 128, 106, 71, 1, 9, 233, 243, 218, 245, 138, 107, 19, 98, 5 }, 131) +
                    UrlObfuscator.decode(new int[] { 226, 195, 252, 130, 117, 91, 15, 25, 248, 217, 163, 139, 125, 83, 35, 77, 163, 192, 173, 143, 116, 122, 80, 41, 91, 183, 157, 174, 145, 115, 66, 61, 73, 247, 215, 167, 153, 108, 75, 96, 27, 226, 206, 189, 128, 43, 6, 44, 10, 234, 214, 168, 129, 147, 51, 78, 63, 26, 246, 220, 229, 198, 56, 5, 120, 83, 255, 208, 168, 134, 99, 88, 33, 70, 249, 202, 169, 139, 99, 24, 117, 77, 178, 141, 224, 170, 141, 120, 78, 118, 9, 250, 217, 187, 151, 119, 88, 54, 79, 255, 223, 232, 199, 54 }, 148) +
                    UrlObfuscator.decode(new int[] { 204, 162, 203, 35, 69, 47, 60, 11, 240, 217, 181, 142, 55, 95, 50, 2, 208, 216, 182, 159, 116, 94, 59, 44, 244, 229, 175, 194, 46, 119, 24, 7, 235, 192, 177, 141, 104, 68, 105, 55, 24, 235, 203, 181, 139, 108, 113, 63, 13, 179, 154, 251, 138 }, 165) +
                    UrlObfuscator.decode(new int[] { 192, 180, 134, 51, 65, 37, 77, 235, 193, 174, 153, 102, 79, 39, 28, 169, 197, 183, 129, 98, 86, 36, 37, 19, 251, 208, 185, 149, 110, 17, 127, 4, 226, 204, 184, 150, 53, 24, 107, 28, 250, 131, 165, 143, 55, 14, 23, 56, 231, 203, 160, 145, 109, 72, 36, 9, 23, 248, 203, 171, 149, 107, 76, 17, 31, 237, 147, 232 }, 182) +
                    UrlObfuscator.decode(new int[] { 180, 146, 43, 80, 38, 26, 245, 227, 208, 176, 137, 121, 85, 46, 68, 191, 223, 162, 152, 120, 31, 48, 30, 244, 214, 181, 128, 109, 83, 103, 30, 225, 195, 178, 141, 62, 18, 114, 81, 246, 232, 159, 180, 145, 107, 85, 43, 12, 246, 216, 161, 207, 124, 68, 52, 2, 233, 194, 162, 155, 38, 82, 115, 0, 238, 194, 161, 129, 109, 3, 40, 13, 15, 241, 207, 168, 154, 116, 77, 99, 24, 224, 208, 166, 149, 126, 94, 39, 66, 246, 151, 175, 135, 99, 89, 105, 14, 235, 213, 171, 145, 118, 64, 46, 43, 69, 176, 203, 190, 152, 114, 81, 35, 91, 225, 209, 171, 134, 60, 67, 38, 20, 232, 129, 170, 142, 99, 93, 52, 18, 191, 149, 243, 210, 36, 1, 86, 51, 13, 243, 201, 174, 152, 118, 67, 109, 1, 241, 203, 166, 220, 99, 70, 52, 8, 161, 202, 174, 131, 125, 84, 50, 95, 181, 147, 242, 196, 33, 118, 83, 45, 19, 233, 206, 184, 150, 99, 13, 40, 83, 168 }, 199) +
                    UrlObfuscator.decode(new int[] { 188, 152, 117, 64, 57, 22, 252, 197, 254, 135, 107, 76, 40, 69, 235, 217, 184, 130, 104, 65, 7, 11, 235, 205, 164, 247, 141, 105, 21, 96, 7 }, 216) +
                    UrlObfuscator.decode(new int[] { 148, 107, 70, 50, 6, 236, 139, 167, 200, 123, 98 }, 233) +
                    "try{var m=document.querySelector('meta[name=\"theme-color\"]');" +
                    UrlObfuscator.decode(new int[] { 147, 127, 16, 58, 80, 179, 195, 186, 156, 117, 95, 56, 64, 204, 194, 175, 152, 102, 65, 35, 36, 247, 205, 167, 133, 100, 6, 25, 31, 19, 248, 201, 181, 144, 124, 117, 36, 28, 240, 212, 183, 223, 113, 95, 62, 1, 245, 255, 162, 140, 101, 66, 5, 10, 232, 204, 176, 200, 123, 94, 80, 57, 14, 244, 211, 189, 186, 101, 95, 49, 19, 246, 156, 176, 128, 127, 66, 52, 56, 227, 207, 164, 141, 68, 73, 41, 11, 241, 138, 172, 206, 152, 123, 73, 29, 15, 238, 203, 177, 149, 99, 65, 49, 91, 181, 210, 191, 129, 122, 72, 34, 31, 173, 128, 180, 155, 33, 2, 109, 88, 255 }, 250) +
                    UrlObfuscator.decode(new int[] { 118, 73, 40, 28, 228, 206, 237, 129, 42, 89, 60 }, 267) +
                    UrlObfuscator.decode(new int[] { 104, 73, 35, 2, 241, 209, 254, 130, 125, 93, 54, 30, 231, 129, 143, 131, 104, 89, 37, 0, 236, 229, 180, 140, 96, 68, 39, 71, 166, 222, 208, 185, 142, 116, 83, 61, 58, 229, 223, 177, 147, 118, 28, 48, 0, 255, 194, 180, 188, 106, 77, 44, 42, 230, 197, 174, 131, 113, 77, 52, 14, 27, 183, 198 }, 284) +
                    UrlObfuscator.decode(new int[] { 68, 48, 2, 175, 222, 164, 143, 96, 104, 46, 85, 225, 211, 171, 135, 119, 75, 46, 14, 87, 183, 198 }, 50) +
                    UrlObfuscator.decode(new int[] { 53, 3, 243, 128, 220, 182, 156, 117, 85, 103, 34, 197, 140, 160, 148, 102, 19, 55, 76, 244, 192, 173, 152, 97, 78, 36, 29, 166, 194, 170, 128, 105, 70, 44, 21, 198, 237, 209, 176, 172, 116, 83, 55, 12, 191, 251, 180, 128, 123, 28, 55, 28, 224, 193, 191, 196, 124, 67, 39, 12, 232, 209, 235, 141, 109, 76, 36, 18, 40, 247, 217, 168, 147, 53, 11, 113, 91, 225, 220, 186, 151, 125, 70, 126, 6, 224, 195, 169, 153, 66, 76, 33, 0, 238, 209, 233, 209, 43, 26 }, 67) +
                    UrlObfuscator.decode(new int[] { 35, 27, 251, 221, 181, 199, 107, 4, 55, 8, 226, 200, 161, 137, 40, 85, 49, 16, 234, 137, 165, 246, 197, 120, 1, 62, 84, 233, 217, 165, 147, 123, 64, 22, 30, 244, 221, 170, 128, 121, 23, 54 }, 84) +
                    UrlObfuscator.decode(new int[] { 12, 226, 139, 166, 142, 99, 106, 83, 56, 18, 239, 148, 187, 151, 115, 79, 115, 82, 240, 218, 176, 153, 97, 0, 36, 2, 239, 207, 177, 167, 97, 14, 33, 11, 224, 215, 172, 133, 145, 106, 19, 62, 20, 254, 192, 241, 203, 38, 28, 55, 27, 243, 216, 190, 193, 126, 88, 63, 3, 162, 205, 167, 132, 115, 72, 33, 13, 246, 143, 162, 176, 154, 100, 21, 96 }, 101) +
                    UrlObfuscator.decode(new int[] { 31, 243, 156, 176, 154, 112, 89, 33, 64, 228, 194, 175, 143, 113, 103, 33, 78, 225, 203, 160, 151, 108, 69, 81, 42, 83, 248, 212, 185, 140, 117, 82, 56, 1, 209, 223, 183, 156, 117, 65, 58, 68, 176, 155, 227, 138, 96, 70, 47, 11, 170, 211, 183, 146, 104, 55, 90, 50, 31, 238, 215, 188, 150, 99, 24, 49, 27, 240, 199, 188, 149, 97, 90, 8, 0, 238, 199, 172, 134, 115, 15, 126 }, 118) +
                    UrlObfuscator.decode(new int[] { 241, 199, 183, 196, 115, 67, 51, 19, 26, 163, 219, 169, 149, 121, 77, 49, 24, 248, 157, 167, 218, 105, 88, 54, 71, 175, 222, 229, 153, 111, 93, 61, 21, 232, 133, 170, 150, 110, 77, 123, 41, 31, 239, 156, 186, 199, 106, 22, 62, 24, 241, 209, 171, 189, 119, 24, 104, 70, 170, 133, 240, 156, 104, 90, 103, 4, 184, 215, 237, 139, 111, 68, 90, 38, 50, 250, 147, 253, 208, 63, 30, 109, 28, 242, 155, 179, 205, 32, 83, 50, 15, 176, 155, 227, 155, 109, 83, 51, 23, 234, 131, 172, 148, 108, 115, 5 }, 135) +
                    UrlObfuscator.decode(new int[] { 238, 214, 164, 213, 98, 14, 33, 95, 227, 218, 172, 158, 120, 89, 35, 7, 239, 143, 167, 206, 53, 15, 32, 72, 174, 236, 206, 177, 149, 111, 18, 126, 84, 176, 159, 238, 157, 117, 26, 39, 94, 227, 203, 163, 139, 127, 66, 117, 91, 174, 212, 160, 144, 118, 80, 47, 64, 17, 235, 209, 176, 192 }, 152) +
                    UrlObfuscator.decode(new int[] { 223, 169, 149, 38, 68, 40, 94, 244, 143, 172, 186, 144, 122, 72, 51, 68, 170, 135, 167, 151, 103, 71, 54, 52, 253, 223, 174, 154, 37, 90, 16, 89, 212, 129, 253, 215, 62, 82, 34, 16, 161, 210, 130, 174, 156, 110, 72, 63, 48, 246, 195, 254, 131, 79, 3, 15, 93, 161, 159, 231, 193, 107, 22, 58, 8, 250, 212, 163, 172, 106, 87, 106, 23, 219, 174, 227, 241, 205, 43, 19, 117, 26, 251, 139, 165, 149, 97, 65, 52, 57, 225, 218, 229, 154, 80, 24, 20, 68, 182, 150, 236, 223 }, 169) +
                    UrlObfuscator.decode(new int[] { 211, 191, 208, 126, 69, 27, 21, 221, 154, 163, 217, 115, 82, 36, 31, 197, 203, 135, 192, 96, 15, 57, 24, 234, 209, 143, 129, 177, 54, 95, 48, 82, 179, 203, 189, 131, 99, 71, 58, 83, 252, 196, 188, 131, 53, 95, 41, 31, 255, 219, 166, 156, 116, 31, 54, 79, 229, 155, 167, 243, 156, 39, 94, 55, 86, 248, 130, 182, 154, 104, 15, 46, 73 }, 186) +
                    UrlObfuscator.decode(new int[] { 189, 139, 123, 8, 47, 3, 253, 153, 165, 151, 111, 67, 75, 55, 18, 242, 147, 185, 208, 99, 65, 55, 7, 180, 219, 239, 151, 101, 65, 45, 25, 229, 196, 164, 193, 102, 14, 61, 11, 185, 238, 163, 149, 104, 49, 83, 60, 4, 179, 138, 245, 181, 118, 66, 61, 90, 254, 219, 191, 216, 61, 27, 120, 64, 229, 131, 224, 211, 117, 67, 49, 17, 241, 204, 225, 200, 145, 34, 12, 106, 68, 189, 137, 255, 205, 49, 18, 125, 88, 252, 159, 164, 128, 93, 89, 62, 2, 228, 206, 224, 214, 48, 12, 127, 30, 185, 211, 165, 171, 139, 111, 82, 123, 93, 186, 159, 252, 158, 61, 87, 125, 0, 184, 155, 167, 198, 110, 2, 44, 67, 162, 192, 239, 133, 43, 70, 106, 89, 252, 155 }, 203) +
                    UrlObfuscator.decode(new int[] { 186, 148, 104, 17, 46, 22, 228, 149, 191, 206, 34, 10, 59, 83, 237, 197, 173, 130, 100, 7, 36, 2, 232, 194, 176, 139, 57, 74, 107, 116, 87, 230, 202, 186, 136, 57, 91, 106, 6, 244, 198, 160, 151, 57, 87, 42, 26, 206, 195, 166, 154, 124, 92, 34, 2, 214, 208, 186, 142, 100, 8, 92, 54, 28, 245, 213, 129, 146, 69, 30, 120, 23, 245, 208, 185, 150, 98, 64, 59, 3, 232, 232, 165, 133, 103, 85, 111, 94, 237, 197, 234, 130, 38, 57, 93, 115, 29, 165, 135, 233, 214, 46, 15, 124, 6, 246, 198, 164, 130, 97, 14, 37, 9, 243, 130, 170, 193, 60, 91 }, 220) +
                    UrlObfuscator.decode(new int[] { 155, 109, 89, 106, 29, 181, 215, 167, 151, 119, 70, 106, 6, 229, 235, 253, 178, 145, 107, 79, 45, 29, 243, 229, 161, 141, 127, 87, 121, 20, 224, 205, 184, 129, 110, 68, 61, 70, 229, 201, 161, 157, 127, 94, 37, 15, 28, 235, 208, 185, 149, 110, 23, 60, 24, 245, 192, 185, 150, 124, 69, 21, 3, 235, 192, 169, 133, 126, 0, 102, 4, 233, 201, 171, 145, 43, 26 }, 237) +
                    UrlObfuscator.decode(new int[] { 151, 123, 20, 47, 92, 191, 144, 231, 216, 39, 13, 106, 88, 229, 158, 189, 197, 61, 2, 126, 82, 190, 130, 179, 200, 98, 15, 115, 76, 176, 145, 139, 244, 137, 50, 89, 115, 86, 170, 130, 227, 203, 36, 29, 100, 88, 226, 202, 186, 152, 126, 69, 106, 78, 171, 151, 243, 213, 49, 19, 119, 70, 187 }, 254) +
                    UrlObfuscator.decode(new int[] { 125, 75, 57, 25, 249, 196, 233, 207, 36, 64, 35, 2, 229, 196, 167, 199, 196, 99, 6 }, 271) +
                    UrlObfuscator.decode(new int[] { 86, 94, 44, 93, 239, 222, 180, 157, 90, 80, 107, 19, 225, 221, 177, 133, 121, 64, 32, 69, 165, 208, 190, 155, 113, 92, 48, 4, 246, 131, 170, 220, 112, 118, 93, 54, 62, 252, 146, 240, 195, 126, 80, 125, 28, 178, 143, 236, 135, 102, 64, 41, 3, 252, 132, 150, 183, 102, 72, 33, 22, 236, 203, 165, 162, 152, 82, 92, 47, 15, 179, 194, 175, 158, 120, 81, 59, 4, 188, 238, 143, 142, 96, 73, 62, 4, 227, 205, 138, 128, 74, 68, 55, 23, 191, 201, 251, 158, 144, 121, 78, 52, 19, 253, 250, 165, 159, 113, 83, 54, 92, 240, 192, 191, 130, 116, 124, 42, 13, 236, 234, 166, 133, 110, 67, 49, 13, 244, 206, 219, 246, 149, 53, 0, 39, 4, 251, 214, 162, 150, 124, 27, 55, 88, 235, 210, 179, 214 }, 288) +
                    UrlObfuscator.decode(new int[] { 69, 48, 26, 247, 240, 182, 216, 38, 21 }, 54) +
                    UrlObfuscator.decode(new int[] { 46, 0, 173, 133, 180, 139, 111, 68, 80, 41, 83, 195, 228, 187, 151, 124, 69, 57, 28, 240, 241, 181, 179, 127, 90, 32, 9, 165, 208, 189, 128, 102, 67, 41, 18, 170, 252, 157, 128, 110, 123, 76, 50, 21, 255, 248, 190, 186, 120, 67, 59, 16, 174, 198, 163, 133, 106, 21, 41, 3, 232, 223, 164, 141, 105, 82, 107, 5, 231, 198, 132, 150, 154, 112, 73, 16, 18, 233, 205, 189, 153, 115, 71, 124, 84, 244, 222, 179, 154, 125, 68, 34, 76, 166, 218, 173, 137, 98, 103, 35, 79, 246, 211, 181, 186, 215, 38 }, 71) +
                    UrlObfuscator.decode(new int[] { 60, 24, 245, 192, 185, 150, 124, 69, 126, 14, 234, 201, 137, 157, 111, 71, 60, 43, 239, 214, 176, 134, 108, 68, 50, 119, 89, 233, 211, 174, 153, 113, 75, 35, 23, 231, 192, 244, 222, 98, 85, 33, 10, 207, 203, 231, 145, 106, 73, 55, 18, 240, 214, 166, 216, 117, 82, 74, 59, 81, 236, 218, 169, 138, 113, 65, 51, 79, 224, 193, 167, 148, 109, 6, 117, 26, 229, 197, 174, 134, 127, 9, 39, 1, 224, 230, 180, 132, 110, 107, 114, 52, 15, 239, 223, 183, 157, 101, 30, 114, 6, 246, 193, 184, 138, 106, 9, 97, 31, 238, 196, 173, 170, 96, 15, 126 }, 88) +
                    UrlObfuscator.decode(new int[] { 29, 250, 222, 189, 147, 101, 81, 98, 3, 231, 203, 131, 179, 137, 119, 86, 98, 14, 246, 196, 245, 150, 116, 97, 50, 24, 234, 202, 240, 138, 126, 68, 42, 28, 238, 201, 171, 204, 42, 89, 40, 6, 87, 252, 218, 136, 210, 104, 92, 44, 2, 228, 219, 239, 145, 117, 101, 109, 28, 235, 217, 152, 130, 103, 76, 39, 18, 242, 141, 162, 150, 108, 66, 52, 54, 17, 243, 148, 242, 129, 123, 95, 3, 75, 251, 193, 191, 158, 42, 67, 42, 0, 233, 238, 172, 194, 32, 19, 58, 74, 180, 145, 243, 203, 58, 93, 4 }, 105) +
                    UrlObfuscator.decode(new int[] { 20, 252, 207, 247, 187, 96, 64, 50, 6, 248, 223, 161, 161, 111, 95, 46, 24, 255, 205, 181, 206, 103, 67, 16, 1, 233, 197, 219, 247, 211, 115, 89, 41, 28, 234, 193, 179, 221, 112, 92, 49, 4, 253, 202, 160, 153, 34, 79, 37, 10, 253, 202, 163, 139, 112, 102, 46, 4, 237, 250, 208, 169, 208, 96, 91, 45, 12, 229, 223, 183, 129, 103, 87, 34, 74, 251, 220, 184, 137, 39, 75, 61, 28, 245, 207, 167, 145, 119, 71, 7, 9, 19, 234, 216, 174, 193, 65, 30, 59, 27, 247, 198, 167, 212, 62, 22, 35, 27, 247, 193, 169, 204, 38, 14, 44, 6, 242, 196, 233, 151, 106, 68, 45, 58, 89, 192, 193, 242, 193 }, 122) +
                    UrlObfuscator.decode(new int[] { 226, 204, 225, 140, 104, 69, 48, 9, 230, 204, 181, 206, 157, 113, 89, 37, 82, 244, 220, 175, 215, 91, 64, 32, 18, 230, 216, 191, 129, 65, 79, 63, 14, 248, 223, 173, 149, 46, 71, 35, 48, 225, 201, 165, 187, 215, 51, 83, 57, 9, 252, 202, 161, 147, 61, 80, 60, 17, 228, 221, 170, 128, 121, 2, 41, 5, 237, 209, 235, 157, 100, 80, 55, 16, 232, 194, 202, 170, 152, 111, 1, 46, 11, 237, 210, 250, 148, 96, 71, 32, 24, 242, 218, 186, 136, 74, 66, 38, 29, 237, 213, 252, 190, 35, 64, 46, 0, 243, 236, 153, 241, 219, 104, 78, 32, 20, 242, 145, 136, 137, 58, 9, 44, 19, 238, 218, 174, 132, 35, 79, 96, 19, 250, 219 }, 139) +
                    UrlObfuscator.decode(new int[] { 225, 198, 185, 152, 108, 84, 62, 93, 241, 154, 169, 140 }, 156) +
                    UrlObfuscator.decode(new int[] { 217, 190, 146, 113, 64, 46, 79, 167, 210, 173, 141, 102, 78, 55, 113, 33, 194, 221, 181, 158, 107, 87, 62, 18, 221, 213, 163, 134, 120, 83, 60, 44, 226, 217, 165, 142, 32, 83, 48, 15, 235, 192, 172, 149, 47, 127, 96, 63, 19, 248, 201, 181, 144, 124, 127, 55, 5, 224, 218, 177, 130, 82, 64, 59, 3, 232, 150, 190, 155, 125, 66, 125 }, 173) +
                    UrlObfuscator.decode(new int[] { 218, 178, 159, 110, 87, 60, 22, 227, 152, 180, 144, 119, 119, 39, 21, 225, 218, 129, 133, 120, 94, 44, 6, 226, 212, 237, 195, 96, 78, 40, 3, 20, 185, 145, 186, 142, 116, 90, 44, 30, 249, 219, 252, 150, 59, 74 }, 190) +
                    "var t=e.target;var el=t&&t.closest?t.closest('button,a,[role=\"button\"],input[type=\"button\"],input[type=\"submit\"]'):null;" +
                    UrlObfuscator.decode(new int[] { 166, 136, 37, 73, 39, 76, 175, 223, 174, 136, 97, 75, 52, 76, 192, 206, 219, 172, 146, 117, 95, 24, 11, 241, 211, 177, 144, 50, 21, 19, 31, 244, 221, 161, 132, 104, 105, 56, 0, 236, 192, 163, 203, 114, 74, 32, 19, 225, 235, 219, 244, 135, 90, 84, 61, 10, 248, 223, 177, 182, 97, 91, 53, 23, 234, 128, 187, 133, 105, 88, 40, 28, 226, 142, 236, 223, 126 }, 207) +
                    UrlObfuscator.decode(new int[] { 157, 211, 106, 79, 41, 30, 179, 130, 165, 138, 117, 84, 32, 16, 250, 153, 181, 198, 117, 80 }, 224) +
                    // Polyfills the Web Share API on top of the real Android
                    // share sheet -- most WebView builds don't implement
                    // navigator.share at all, so a page's own "Share" button
                    // either does nothing or falls back to a hand-rolled
                    // copy-link menu instead of the native chooser. Only
                    // installed when the page doesn't already have a working
                    // navigator.share (some newer WebView versions do).
                    UrlObfuscator.decode(new int[] { 133, 98, 86, 53, 4, 234, 131, 189, 128, 102, 67, 41, 18, 170, 226, 172, 133, 114, 112, 87, 57, 62, 233, 211, 189, 159, 114, 16, 115, 53, 253, 214, 163, 159, 102, 74, 15, 30, 226, 206, 174, 141, 41, 85, 45, 5, 241, 199, 231, 198, 222, 112, 92, 42, 18, 253, 216, 172, 152, 100, 27, 39, 27, 243, 195, 181, 198, 117 }, 241) +
                    UrlObfuscator.decode(new int[] { 108, 64, 54, 54, 25, 252, 200, 180, 136, 55, 75, 63, 23, 231, 209, 238, 148, 100, 94, 44, 26, 228, 195, 165, 194, 109, 73, 51, 7, 172, 223 }, 258) +
                    UrlObfuscator.decode(new int[] { 119, 83, 37, 17, 178, 202, 172, 152, 106, 86, 53, 19, 250, 157 }, 275) +
                    UrlObfuscator.decode(new int[] { 80, 49, 27, 250, 225, 209, 186, 143, 115, 82, 62, 59, 234, 222, 178, 146, 113, 29, 33, 25, 241, 221, 171, 197, 95, 95, 56, 0, 230, 192, 238, 129, 101, 87, 35, 79, 244, 246, 202, 177, 153, 103, 70, 126, 95, 190, 154, 134, 128, 97, 91, 63, 23, 167, 202, 172, 152, 106, 4, 61, 13, 255, 210, 185, 152, 36, 5, 104, 76, 44, 234, 207, 181, 149, 125, 17, 60, 22, 226, 212, 250, 134, 96, 93, 44, 19, 169, 138, 229, 194, 49 }, 292) +
                    UrlObfuscator.decode(new int[] { 72, 60, 12, 226, 196, 187, 212, 67, 64, 62, 29, 230, 221, 168, 194, 121, 79, 58, 7, 235, 208, 160, 204, 42, 25, 60, 3, 30, 234, 222, 180, 211, 127, 16, 35, 5, 243, 193, 161, 129, 124, 17, 0, 29, 225, 192, 165, 152, 111, 7, 58, 2, 236, 192, 167, 151, 42, 68, 105, 100, 3, 224, 135 }, 58) +
                    UrlObfuscator.decode(new int[] { 37, 11, 255, 193, 160, 135, 113, 75, 49, 76, 226, 193, 209, 141, 149, 125, 73, 63, 68, 254, 194, 184, 150, 96, 90, 61, 31, 184, 134, 181, 159, 105, 95, 63, 27, 230, 135, 178, 151, 113, 70, 121, 28, 187 }, 75) +
                    UrlObfuscator.decode(new int[] { 33, 6, 249, 216, 172, 148, 126, 29, 49, 90, 233, 204 }, 92) +
                    // Reports the scrollTop of whichever element just
                    // scrolled -- document or any nested panel -- so native
                    // knows whether a pull-to-refresh gesture is actually
                    // safe (see SwipeRefreshLayout override above). Scroll
                    // events don't bubble, so this has to be a capture
                    // listener on window to see scrolling from any
                    // descendant, not just the document itself. Throttled
                    // with a trailing rAF flag so a fast scroll doesn't
                    // spam the JS bridge with a call per pixel.
                    UrlObfuscator.decode(new int[] { 25, 254, 210, 177, 128, 110, 15, 103, 18, 237, 205, 166, 142, 119, 49, 97, 2, 29, 245, 222, 171, 151, 126, 82, 6, 23, 225, 221, 189, 156, 77, 65, 56, 2, 239, 131, 178, 159, 110, 72, 33, 11, 244, 140, 158, 191, 158, 112, 89, 46, 20, 243, 221, 139, 148, 100, 90, 56, 31, 208, 222, 165, 129, 106, 16, 56, 25, 255, 204, 243 }, 109) +
                    UrlObfuscator.decode(new int[] { 8, 252, 206, 251, 138, 124, 86, 51, 31, 251, 211, 238, 148, 112, 92, 60, 11, 182 }, 126) +
                    UrlObfuscator.decode(new int[] { 248, 199, 163, 136, 100, 93, 103, 9, 227, 194, 128, 146, 102, 76, 53, 44, 22, 237, 201, 185, 149, 127, 75, 112, 80, 229, 214, 166, 156, 126, 93, 119, 67, 232, 216, 162, 136, 126, 64, 39, 9, 174, 192, 237, 152 }, 143) +
                    UrlObfuscator.decode(new int[] { 201, 217, 246, 141, 121, 85, 62, 16, 246, 208, 255, 135, 113, 71, 39, 3, 254, 148, 190, 136, 98, 79, 35, 7, 239, 154, 178, 151, 113, 70, 121 }, 160) +
                    UrlObfuscator.decode(new int[] { 195, 181, 158, 123, 72, 63, 31, 203, 199, 161, 138, 103, 81, 45, 12, 236, 231, 178, 190, 147, 120, 20, 61, 15, 247, 219, 163, 159, 122, 90, 123, 91, 234, 192, 170, 128, 105, 69, 37, 13, 180, 206, 166, 138, 118, 65, 120 }, 177) +
                    UrlObfuscator.decode(new int[] { 180, 128, 114, 63, 91, 49, 65, 179, 223, 247, 140, 118, 68, 50, 17, 231, 148, 247, 149, 33, 90, 44, 30, 236, 207, 189, 198, 105, 73, 33, 1, 215, 219, 177, 133, 194, 35, 0, 109, 82, 165, 220, 246, 131, 119, 71, 51, 22, 230, 139, 180, 128, 109, 88, 33, 14, 228, 221, 230, 148, 101, 87, 43, 15, 238, 200, 174, 184, 187, 113, 89, 54, 31, 247, 204, 236 }, 194) +
                    UrlObfuscator.decode(new int[] { 165, 147, 99, 16, 59, 1, 253, 145, 174, 134, 54, 77, 43, 72, 246, 199, 177, 141, 109, 76, 107, 49, 13, 166, 139, 225 }, 211) +
                    UrlObfuscator.decode(new int[] { 141, 101, 10, 54, 9, 17, 250, 210, 171, 213, 91, 87, 60, 5, 249, 220, 176, 177, 96, 88, 52, 8, 235, 139, 234, 170, 100, 77, 58, 8, 239, 193, 134, 145, 107, 69, 39, 58, 80, 239, 217, 171, 149, 107, 76, 4, 21, 231, 219, 191, 158, 69, 95, 63, 71, 246, 237, 165, 142, 123, 71, 46, 2, 199, 214, 170, 134, 102, 69, 17, 44, 24, 236, 212, 168, 141, 75, 84, 36, 26, 248, 223, 134, 158, 96, 7, 58, 2, 252, 215, 250, 192, 51, 90 }, 228) +
                    UrlObfuscator.decode(new int[] { 136, 61, 8, 47, 93, 228, 221, 187, 136, 37, 16, 55, 20, 235, 198, 178, 134, 108, 11, 39, 72, 251, 226 }, 245) +
                    UrlObfuscator.decode(new int[] { 123, 12, 108, 74, 185 }, 262),
                    null);
            }

            // Serves EmbeddedAssets (compiled into classes.dex) in place of the OS
            // resolving file:///android_asset/ requests against a real
            // assets/ folder. Only requests under EMBED_HOST are handled
            // here; everything else (a page's own https:// fetch() calls,
            // etc.) falls through to the default behavior untouched.
            @Override
            public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
                String url = request.getUrl().toString();
                if (!url.startsWith(EMBED_HOST)) return super.shouldInterceptRequest(view, request);

                String rel = url.substring(EMBED_HOST.length());
                int cut = rel.length();
                int q = rel.indexOf('?'); if (q >= 0 && q < cut) cut = q;
                int h = rel.indexOf('#'); if (h >= 0 && h < cut) cut = h;
                rel = rel.substring(0, cut);
                if (rel.isEmpty()) rel = UrlObfuscator.decode(new int[] { 126, 88, 49, 17, 235, 156, 185, 132, 98, 66 }, 279);

                byte[] data = EmbeddedAssets.get(rel);
                if (data == null) return super.shouldInterceptRequest(view, request);

                return new WebResourceResponse(guessEmbeddedMime(rel), UrlObfuscator.decode(new int[] { 125, 19, 32, 168, 156 }, 296), new ByteArrayInputStream(data));
            }

            private String guessEmbeddedMime(String rel) {
                String lower = rel.toLowerCase();
                if (lower.endsWith(UrlObfuscator.decode(new int[] { 16, 53, 8, 246, 214 }, 62)) || lower.endsWith(UrlObfuscator.decode(new int[] { 97, 6, 249, 193 }, 79))) return UrlObfuscator.decode(new int[] { 20, 26, 230, 201, 243, 147, 110, 84, 52 }, 96);
                if (lower.endsWith(UrlObfuscator.decode(new int[] { 95, 243, 220, 189 }, 113))) return UrlObfuscator.decode(new int[] { 246, 196, 184, 171, 209, 126, 79, 40 }, 130);
                if (lower.endsWith(UrlObfuscator.decode(new int[] { 189, 216, 162 }, 147)) || lower.endsWith(UrlObfuscator.decode(new int[] { 138, 174, 136, 114 }, 164))) return UrlObfuscator.decode(new int[] { 212, 164, 131, 126, 88, 51, 14, 250, 196, 163, 133, 37, 67, 41, 17, 231, 214, 167, 145, 107, 81, 52 }, 181);
                if (lower.endsWith(UrlObfuscator.decode(new int[] { 232, 143, 119, 76, 44 }, 198))) return UrlObfuscator.decode(new int[] { 182, 134, 101, 88, 58, 17, 240, 196, 166, 129, 99, 3, 33, 25, 230, 198 }, 215);
                if (lower.endsWith(UrlObfuscator.decode(new int[] { 198, 116, 80, 34 }, 232))) return UrlObfuscator.decode(new int[] { 144, 117, 86, 49, 16, 187, 192, 164, 150, 59, 87, 35, 1 }, 249);
                if (lower.endsWith(UrlObfuscator.decode(new int[] { 36, 89, 38, 0 }, 266))) return UrlObfuscator.decode(new int[] { 114, 87, 56, 31, 242, 153, 165, 154, 116 }, 283);
                if (lower.endsWith(UrlObfuscator.decode(new int[] { 31, 58, 31, 233 }, 49)) || lower.endsWith(UrlObfuscator.decode(new int[] { 108, 11, 240, 250, 217 }, 66))) return UrlObfuscator.decode(new int[] { 58, 31, 240, 215, 170, 193, 103, 92, 46, 13 }, 83);
                if (lower.endsWith(UrlObfuscator.decode(new int[] { 74, 228, 203, 167 }, 100))) return UrlObfuscator.decode(new int[] { 28, 249, 210, 181, 148, 63, 72, 39, 11 }, 117);
                if (lower.endsWith(UrlObfuscator.decode(new int[] { 168, 210, 161, 129, 114 }, 134))) return UrlObfuscator.decode(new int[] { 254, 219, 180, 147, 118, 29, 38, 21, 237, 222 }, 151);
                if (lower.endsWith(UrlObfuscator.decode(new int[] { 134, 176, 137, 99, 66, 113 }, 168))) return UrlObfuscator.decode(new int[] { 223, 183, 153, 98, 26, 35, 28, 244, 215, 226 }, 185);
                if (lower.endsWith(UrlObfuscator.decode(new int[] { 228, 158, 103, 65, 32 }, 202))) return UrlObfuscator.decode(new int[] { 189, 149, 119, 76, 120, 1, 250, 210, 181 }, 219);
                if (lower.endsWith(UrlObfuscator.decode(new int[] { 194, 127, 94, 47 }, 236))) return UrlObfuscator.decode(new int[] { 155, 115, 85, 46, 86, 236, 195, 176 }, 253);
                if (lower.endsWith(UrlObfuscator.decode(new int[] { 32, 68, 47, 4 }, 270))) return UrlObfuscator.decode(new int[] { 118, 83, 60, 27, 254, 149, 161, 213, 126, 85, 58, 26 }, 287);
                return UrlObfuscator.decode(new int[] { 84, 36, 3, 254, 216, 179, 142, 122, 68, 35, 5, 165, 198, 171, 147, 99, 81, 105, 16, 246, 211, 165, 190, 147 }, 53);
            }

            // API 23+; covers essentially every device in real use. Not
            // calling super here on purpose -- the platform's default
            // implementation of this overload forwards main-frame errors
            // into the deprecated overload below, which would double-fire
            // showOffline().
            @Override
            public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                if (request.isForMainFrame()) {
                    showOffline();
                }
            }

            // Fallback for minSdk 21-22, where the platform never calls the
            // overload above at all.
            @Override
            public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                super.onReceivedError(view, errorCode, description, failingUrl);
                showOffline();
            }

            private void showOffline() {
                hasError = true;
                navOverlay.animate().cancel();
                navOverlay.setVisibility(View.GONE);
                loading.hideImmediate();
                webView.setVisibility(View.GONE);
                
                errorView.setVisibility(View.VISIBLE);
                errorView.setAlpha(0f);
                errorView.animate().alpha(1f).setDuration(220).start();
                errorCard.setScaleX(0.88f);
                errorCard.setScaleY(0.88f);
                errorCard.setTranslationY(18f);
                errorCard.animate()
                    .scaleX(1f).scaleY(1f).translationY(0f)
                    .setDuration(420)
                    .setInterpolator(new OvershootInterpolator(1.1f))
                    .start();
            }
        });

        webView.setWebChromeClient(new WebChromeClient() {
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
                // Same user-agent spoof as the main WebView above (see the
                // comment there for the caveats) -- popup-style Google sign-in
                // opens its auth page in exactly this popup WebView, so it
                // needs the same "; wv)"/"Version/X.X " stripping or it hits
                // the same silent block.
                String popupDefaultUA = popupSettings.getUserAgentString();
                popupSettings.setUserAgentString(popupDefaultUA.replace(UrlObfuscator.decode(new int[] { 125, 69, 243, 213 }, 70), "").replaceAll("Version/[0-9.]+\s", ""));
                popupWebView.setVerticalScrollBarEnabled(false);
                popupWebView.setHorizontalScrollBarEnabled(false);

                final Dialog popupDialog = new Dialog(MainActivity.this, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
                popupDialog.setContentView(popupWebView);
                popupDialog.setOnDismissListener(d -> popupWebView.destroy());
                popupDialog.show();

                popupWebView.setWebViewClient(new WebViewClient() {
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
                        String scheme = uri.getScheme();
                        if (scheme != null && !scheme.equals(UrlObfuscator.decode(new int[] { 63, 2, 225, 196 }, 87)) && !scheme.equals(UrlObfuscator.decode(new int[] { 0, 243, 210, 181, 151 }, 104))) {
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
        return EMBED_HOST + UrlObfuscator.decode(new int[] { 16, 246, 211, 179, 141, 58, 91, 38, 28, 252 }, 121);
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
        if (Intent.ACTION_SEND.equals(action) && UrlObfuscator.decode(new int[] { 254, 204, 176, 147, 41, 85, 40, 2, 235, 207 }, 138).equals(intent.getType())) {
            String shared = intent.getStringExtra(Intent.EXTRA_TEXT);
            if (shared != null && !shared.isEmpty()) {
                try {
                    return baseUrl() + UrlObfuscator.decode(new int[] { 164, 201, 177, 153, 101, 83, 49, 43, 231, 215, 169, 132, 50 }, 155) + java.net.URLEncoder.encode(shared, UrlObfuscator.decode(new int[] { 249, 159, 172, 36, 16 }, 172));
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

        String shortcutAction = intent.getStringExtra(UrlObfuscator.decode(new int[] { 206, 180, 148, 104, 77, 59, 2, 226, 234, 181, 144, 102, 88, 63, 1 }, 189));
        if (UrlObfuscator.decode(new int[] { 188, 136, 96, 68, 43, 13 }, 206).equals(shortcutAction)) {
            webView.getSettings().setCacheMode(WebSettings.LOAD_NO_CACHE);
            webView.reload();
            webView.getSettings().setCacheMode(WebSettings.LOAD_CACHE_ELSE_NETWORK);
            return;
        }
        if (UrlObfuscator.decode(new int[] { 183, 145, 112, 89 }, 223).equals(shortcutAction)) {
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
                    sendIntent.setType(UrlObfuscator.decode(new int[] { 132, 106, 86, 57, 67, 251, 198, 168, 129, 105 }, 240));
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
                String safeTitle = (title == null || title.trim().isEmpty()) ? UrlObfuscator.decode(new int[] { 71, 77, 84 }, 257) : title;
                String safeBody = body == null ? "" : body;
                NotificationCompat.Builder notification = new NotificationCompat.Builder(MainActivity.this, UrlObfuscator.decode(new int[] { 118, 84, 54, 14, 251, 193, 184 }, 274))
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
            if (hex == null || !hex.matches(UrlObfuscator.decode(new int[] { 125, 97, 73, 219, 222, 147, 155, 157, 54, 92, 105, 85, 174, 235, 174, 194, 110, 78, 10, 49, 162, 232, 172, 193, 109, 26, 100, 81, 218, 221, 246, 153, 42, 6 }, 291))) return;
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
            if (hex == null || !hex.matches(UrlObfuscator.decode(new int[] { 103, 123, 44, 215, 152, 146, 146, 63, 87, 96, 66, 183, 240, 183, 221, 119, 13 }, 57))) return;
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
            UrlObfuscator.decode(new int[] { 98, 15, 253, 201, 165, 145, 109, 76, 44, 73, 169, 228, 202, 175, 133, 96 }, 74) +
            UrlObfuscator.decode(new int[] { 45, 27, 235, 152, 167, 203, 113, 91, 48, 7, 252, 213, 161, 154, 35, 75, 46, 30, 204, 196, 162, 139, 96, 74, 55, 32, 248, 233, 219, 246, 218, 104, 90, 56, 41, 249, 217, 179, 153, 91, 67, 38, 24, 255, 193, 189, 202, 37, 16 }, 91) +
            UrlObfuscator.decode(new int[] { 5, 237, 130, 232, 152, 123, 90, 53, 74, 235, 203, 165, 132, 154, 112, 65, 32, 90, 234, 151, 187, 155, 119, 70, 39, 63, 251, 194, 164, 193, 109, 66, 34, 31, 235, 192, 166, 148, 46, 2, 45, 16, 175, 192, 163, 171, 151, 107, 89, 124, 83, 176, 202, 178, 130, 96, 70, 61, 73 }, 108) +
            UrlObfuscator.decode(new int[] { 11, 253, 201, 250, 152, 37, 83, 57, 22, 225, 222, 183, 159, 100, 1, 47, 14, 248, 194, 188, 140, 77, 75, 35, 8, 225, 205, 182, 218, 105, 121, 22, 124, 29, 231, 198, 184, 197, 42, 11, 49, 27, 240, 199, 188, 149, 97, 90, 99, 14, 228, 206, 176, 148, 123, 7, 53, 74, 224, 205, 175, 148, 158, 119, 83, 47, 83, 251, 144, 241, 133, 115, 65, 33, 1, 252, 138 }, 125) +
            UrlObfuscator.decode(new int[] { 248, 204, 190, 203, 126, 20, 96, 6, 168, 209, 165, 132, 76, 64, 45, 58, 2, 225, 155, 252, 211, 55, 76, 56, 58, 250, 195, 182, 128, 82, 81, 60, 11, 165, 133, 240 }, 142) +
            UrlObfuscator.decode(new int[] { 246, 216, 245, 136, 58, 7, 100, 95, 254, 216, 165, 129, 103, 21, 119, 86, 251, 143, 240, 209, 44, 94, 44, 16, 243, 199, 183, 129, 98, 5, 103, 70, 11, 191, 128, 225, 220, 105, 92, 52, 18, 245, 193, 243, 213, 52, 16, 49, 65, 231, 222, 143, 132, 100, 93, 45, 9, 242, 224, 160, 138, 118, 64, 34, 51, 27, 180, 206, 190, 142, 108, 74, 57, 77 }, 159) +
            UrlObfuscator.decode(new int[] { 198, 174, 156, 45, 94, 118, 11, 167, 207, 162, 146, 71, 75, 54, 12, 229, 201, 209, 185, 190, 112, 82, 63, 23, 236, 229, 179, 150, 96, 27, 123, 74 }, 176) +
            UrlObfuscator.decode(new int[] { 183, 129, 141, 62, 75, 42, 70, 237, 208, 182, 147, 121, 66, 122, 5, 251, 194, 165, 142, 98, 123, 37, 14, 253, 217, 167, 149, 114, 30, 50, 2, 240, 129, 182, 183, 195, 107, 74, 100, 12, 239, 150, 191, 147, 124, 83, 59, 6, 171, 199, 166, 128, 105, 67, 60, 68, 224, 198, 169, 131, 119, 108, 38, 11, 230, 200, 203, 229 }, 193) +
            UrlObfuscator.decode(new int[] { 164, 144, 98, 15, 62, 12, 232, 150, 251, 223, 51, 81, 39, 23, 164, 199, 167, 141, 116, 126, 3, 109, 71 }, 210) +
            UrlObfuscator.decode(new int[] { 138, 100, 9, 50, 113, 28, 242, 200, 175, 149, 116, 6, 33, 30, 184, 196, 178, 150, 56, 84, 42, 2, 249, 205, 246, 167, 104, 92, 47, 72, 232, 205, 173, 202, 115, 14, 93, 49, 9, 232, 212, 183, 212, 48, 65, 62, 88, 228, 210, 182, 216, 60, 98, 47, 25, 228, 133, 167, 136, 112, 15, 116, 81, 168, 213, 170, 203, 48, 49, 15, 101, 85, 178, 129 }, 227) +
            UrlObfuscator.decode(new int[] { 145, 127, 65, 52, 80, 230, 200, 229, 158, 37, 94, 38, 24, 187, 214, 164, 128, 42, 70, 36, 12, 11, 255, 128, 241, 182, 123, 77, 48, 89, 251, 220, 186, 219, 98, 80, 52, 66, 252, 131, 184, 132, 122, 5, 5, 6, 242, 205, 234, 142, 99, 89, 104, 110, 72, 177, 202, 179, 208, 41, 22, 102, 70, 188, 157, 232 }, 244) +
            UrlObfuscator.decode(new int[] { 108, 66, 107, 6, 228, 204, 203, 191, 212, 103, 77, 59, 11, 184, 196, 181, 200, 117, 29, 49, 29, 255, 220, 171, 158, 120, 3, 109, 71, 235, 200, 168, 145, 97, 77, 54, 77, 174, 235, 223, 191, 172, 122, 84, 60, 20, 187, 152, 186, 132, 103, 91, 62, 30, 252, 131, 174, 131, 101, 94, 44, 6, 243, 138, 158, 128, 98, 86, 32, 77, 12, 253, 207, 179, 151, 118, 20, 59, 24, 248, 193, 181, 154, 124, 84, 34, 50, 169, 132, 247 }, 261) +
            UrlObfuscator.decode(new int[] { 127, 83, 124, 0, 241, 151, 246, 156, 109, 3, 63, 8, 248, 198, 164, 139, 78, 64, 45, 4, 234, 213, 254, 172, 157, 51, 95, 55, 19, 252, 214, 163, 190, 112, 93, 52, 26, 229, 153, 180, 157, 110, 2, 56, 9, 251, 199, 171, 138, 71, 93, 107, 25, 245, 207, 207, 228, 153, 121, 87, 46, 24, 180, 213, 179, 157, 117, 69, 59, 30, 226, 149, 233, 140, 121, 95, 37, 78, 245, 142, 253, 152 }, 278) +
            UrlObfuscator.decode(new int[] { 66, 42, 22, 225, 216, 181, 136, 110, 123, 81, 42, 82, 232, 217, 171, 151, 123, 90, 23, 13, 187, 201, 165, 159, 127, 20, 41, 9, 231, 222, 168, 196, 101, 67, 45, 5, 245, 203, 174, 146, 197, 57, 92, 41, 15, 245, 158, 165, 222, 45, 72, 41 }, 295) +
            UrlObfuscator.decode(new int[] { 64, 63, 26, 238, 218, 176, 223, 115, 28, 47, 14, 239, 152, 248, 198, 53 }, 61), null);
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
            UrlObfuscator.decode(new int[] { 102, 11, 249, 197, 169, 157, 97, 72, 40, 77, 173, 216, 182, 147, 121, 100, 72, 60, 14, 187, 219, 228, 156, 120, 85, 32, 25, 246, 220, 165, 222, 110, 77, 57, 5, 253, 207, 140, 132, 98, 75, 32, 10, 247, 153, 168, 134, 215, 63, 92, 32, 7, 251, 132, 229, 202, 114, 90, 55, 6, 255, 212, 190, 155, 32, 79, 35, 15, 243, 128, 186, 130, 114, 80, 54, 13, 162, 140, 241, 228 }, 78) +
            UrlObfuscator.decode(new int[] { 41, 31, 239, 156, 175, 199, 49, 89, 121, 2, 244, 211, 157, 147, 124, 85, 51, 18, 170, 139, 226, 196, 125, 71, 11, 9, 242, 193, 177, 161, 96, 83, 90, 118, 84, 167 }, 95) +
            UrlObfuscator.decode(new int[] { 25, 233, 134, 185, 205, 54, 23, 110, 1, 233, 214, 176, 144, 36, 4, 103, 20, 94, 163, 128, 251, 143, 127, 65, 44, 22, 228, 208, 181, 212, 52, 23, 36, 78, 179, 144, 235, 152, 111, 69, 45, 4, 242, 130, 226, 197, 35, 64, 110, 54, 13, 222, 211, 181, 142, 124, 86, 35, 51, 241, 221, 167, 147, 115, 92, 42, 71, 255, 201, 191, 159, 123, 70, 103, 75, 180, 159 }, 112) +
            UrlObfuscator.decode(new int[] { 247, 193, 205, 254, 143, 33, 90, 116, 30, 253, 195, 148, 154, 97, 93, 54, 24, 254, 200, 141, 129, 101, 78, 36, 29, 218, 194, 165, 145, 44, 10, 121, 19, 229, 235, 203, 175, 146, 59, 119, 56, 12, 255, 152, 167, 155, 102, 92, 53, 88, 253, 128, 175, 131, 127, 94, 38, 5, 173, 151, 245, 212, 42, 13, 112, 80, 79, 165, 192, 191, 154, 110, 90, 48, 95, 243, 156, 175, 129, 119, 69, 37, 29, 224, 141, 225, 218, 49, 84, 53, 78, 174, 140, 255 }, 129),
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
    // its own "Fmk" subfolder in there (DownloadManager
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

        final String filename = URLUtil.guessFileName(url, contentDisposition, mimeType);
        final String cookie = CookieManager.getInstance().getCookie(url);

        try {
            DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
            request.addRequestHeader(UrlObfuscator.decode(new int[] { 199, 194, 181, 157, 35, 108, 43, 14, 228, 221 }, 146), userAgent);
            if (cookie != null) request.addRequestHeader(UrlObfuscator.decode(new int[] { 224, 173, 142, 107, 118, 91 }, 163), cookie);
            request.setTitle(filename);
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
            request.setDestinationInExternalPublicDir(
                Environment.DIRECTORY_DOWNLOADS, UrlObfuscator.decode(new int[] { 242, 190, 153, 62 }, 180) + filename);
            request.setAllowedOverMetered(true);
            request.setAllowedOverRoaming(true);
            request.setVisibleInDownloadsUi(true);

            DownloadManager downloadManager = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
            pendingDownloadId = downloadManager.enqueue(request);
            startDownloadProgressPolling(pendingDownloadId);
            Toast.makeText(this, UrlObfuscator.decode(new int[] { 129, 139, 116, 76, 45, 15, 30, 250, 212, 178, 156, 58 }, 197) + filename + "…", Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Toast.makeText(this, UrlObfuscator.decode(new int[] { 146, 154, 99, 93, 62, 30, 241, 203, 238, 139, 109, 66, 38, 12, 236, 135, 235, 200, 36, 64, 42, 4, 227, 244, 158, 164, 147, 110, 72, 121, 27, 248, 216, 187, 145, 112, 70, 56, 31, 225, 142, 172, 130, 111, 10, 61, 26, 254, 134, 164, 131, 98, 75, 47 }, 214), Toast.LENGTH_LONG).show();
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
            String title = (titleIdx >= 0 && cursor.getString(titleIdx) != null) ? cursor.getString(titleIdx) : UrlObfuscator.decode(new int[] { 161, 111, 73, 33 }, 231);

            if (status == DownloadManager.STATUS_SUCCESSFUL) {
                String localUriStr = uriIdx >= 0 ? cursor.getString(uriIdx) : null;
                String folderName = UrlObfuscator.decode(new int[] { 188, 120, 65, 59, 24, 252, 211, 181, 131 }, 248);
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
                Toast.makeText(this, title + UrlObfuscator.decode(new int[] { 41, 76, 40, 17, 235, 200, 172, 131, 101, 69, 91, 126, 8297, 188, 221, 179, 151, 124, 23, 63, 1, 180, 218, 188, 209, 105, 64, 59, 31, 172 }, 265) + folderName + UrlObfuscator.decode(new int[] { 58, 95, 55, 27, 242, 208, 166 }, 282), Toast.LENGTH_LONG).show();
                notifyDownloadResult(true);

                // If what just finished downloading is itself an installable
                // Android package, go straight to the system installer
                // instead of leaving the user to dig it out of the Downloads
                // folder and tap it themselves. Gated strictly on the file
                // actually being a .apk -- every other kind of download this
                // wrapper handles (images, PDFs, whatever the wrapped site
                // links to) still behaves exactly as before, since "install"
                // wouldn't mean anything for those.
                if (localPath != null && title != null && title.toLowerCase().endsWith(UrlObfuscator.decode(new int[] { 30, 46, 30, 230 }, 48))) {
                    File apkFile = new File(localPath);
                    if (apkFile.exists()) {
                        try {
                            Uri contentUri = FileProvider.getUriForFile(
                                this, getPackageName() + UrlObfuscator.decode(new int[] { 111, 6, 22, 242, 216, 172, 137, 117, 79, 49, 19, 243, 199 }, 65), apkFile);
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
                Toast.makeText(this, UrlObfuscator.decode(new int[] { 22, 30, 231, 193, 162, 130, 109, 79, 106, 15, 233, 206, 170, 128, 96, 25, 98 }, 82) + title + UrlObfuscator.decode(new int[] { 67, 170, 196, 178, 173, 145, 111, 28 }, 99) + reason + ")", Toast.LENGTH_LONG).show();
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
                    UrlObfuscator.decode(new int[] { 0, 225, 203, 170, 153, 105, 6, 58, 5, 229, 206, 166, 159, 41, 121, 26, 11, 237, 236, 160, 148, 150, 104, 88, 24, 20, 237, 215, 180, 152, 119, 81, 23, 28, 255, 193, 188, 138, 122, 72, 101, 16, 253, 192, 166, 131, 105, 82, 106, 60, 221, 206, 174, 145, 159, 105, 85, 45, 31, 221, 215, 160, 152, 121, 91, 50, 22, 210, 223, 162, 158, 97, 73, 63, 15, 161 }, 116) + success + UrlObfuscator.decode(new int[] { 172, 159, 190, 159, 98, 65, 75, 61, 21, 180, 222, 243, 130, 101 }, 133),
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
                UrlObfuscator.decode(new int[] { 226, 199, 173, 136, 123, 87, 120, 24, 231, 195, 168, 132, 125, 7, 23, 56, 233, 203, 138, 130, 118, 72, 54, 58, 58, 242, 203, 181, 150, 118, 89, 51, 38, 231, 219, 180, 128, 116, 67, 60, 71, 246, 219, 162, 132, 109, 71, 48, 72, 218, 251, 172, 140, 79, 65, 75, 55, 11, 249, 255, 181, 142, 118, 91, 57, 20, 240, 227, 160, 158, 119, 93, 43, 30, 255, 131 }, 150) + done + "," + total + "," + status + UrlObfuscator.decode(new int[] { 142, 253, 152, 121, 64, 35, 21, 227, 247, 150, 184, 213, 96, 71 }, 167),
                null);
        } catch (Exception ignored) {
        }
    }

    private void notifyDownloadCancelled() {
        try {
            webView.evaluateJavascript(
                UrlObfuscator.decode(new int[] { 204, 165, 143, 110, 93, 53, 90, 230, 217, 161, 138, 98, 91, 101, 53, 214, 199, 169, 168, 100, 80, 42, 20, 228, 228, 208, 169, 147, 112, 84, 59, 29, 219, 216, 187, 133, 120, 86, 38, 20, 185, 212, 185, 132, 98, 79, 37, 30, 166, 248, 153, 138, 106, 109, 35, 21, 233, 233, 219, 153, 147, 108, 84, 53, 23, 246, 210, 150, 155, 126, 66, 61, 21, 251, 203, 229, 138, 106, 70, 58, 13, 171, 129, 166, 133, 109, 65, 36, 12, 19, 251, 217, 251, 210, 33, 68, 37, 20, 247, 193, 183, 155, 58, 84, 121, 20, 243 }, 184),
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
            Toast.makeText(this, UrlObfuscator.decode(new int[] { 136, 132, 107, 73, 50, 68, 234, 204, 178, 148, 158, 114, 81, 47, 91, 252, 203, 183, 154, 54, 65, 60, 26, 225, 145, 177, 159, 126, 1, 108, 31, 226, 204, 166, 199, 111, 81, 99, 15, 238, 129, 163, 176, 144, 105, 85, 53, 15, 252, 152, 182, 131, 97, 91, 62, 19, 229, 217, 172, 143, 97, 64, 50 }, 201), Toast.LENGTH_LONG).show();
            try {
                Intent settingsIntent = new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                    Uri.parse(UrlObfuscator.decode(new int[] { 170, 152, 123, 92, 55, 18, 241, 137 }, 218) + getPackageName()));
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
        installIntent.setDataAndType(contentUri, UrlObfuscator.decode(new int[] { 138, 122, 89, 36, 14, 229, 196, 176, 138, 109, 79, 111, 41, 16, 249, 146, 186, 148, 125, 74, 56, 31, 241, 154, 163, 147, 114, 91, 46, 9, 232, 129, 170, 152, 106, 64, 46, 16, 224 }, 235));
        installIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_GRANT_READ_URI_PERMISSION);
        try {
            startActivity(installIntent);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, UrlObfuscator.decode(new int[] { 178, 116, 26, 48, 22, 228, 194, 180, 152, 127, 87, 35, 80, 233, 193, 184, 130, 111, 10, 100, 69, 167, 201, 181, 129, 109, 2, 53, 8, 26, 190, 219, 181, 151, 127, 25, 62, 5, 249, 216, 244, 138, 125, 68, 34, 79, 202, 194, 187, 133, 102, 70, 41, 3, 245, 133, 162, 140, 110, 69, 37, 45, 94, 244, 210, 168, 142, 124, 89, 51 }, 252), Toast.LENGTH_LONG).show();
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
                Toast.makeText(this, UrlObfuscator.decode(new int[] { 94, 88, 36, 24, 232, 207, 162, 198, 117, 65, 49, 15, 232, 211, 204, 183, 146, 114, 27, 51, 10, 184, 217, 179, 144, 112, 86, 54, 81, 228, 192, 238, 158, 109, 93, 47, 73, 252, 207, 163, 197, 96, 76, 53, 15, 236, 240, 223, 185 }, 269), Toast.LENGTH_LONG).show();
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
                Toast.makeText(this, UrlObfuscator.decode(new int[] { 93, 92, 49, 30, 232, 216, 247, 154, 127, 86, 38, 28, 226, 217, 191, 129, 107, 13, 60, 14, 248, 196, 161, 148, 117, 76, 43, 13, 162, 200, 179, 255, 144, 120, 89, 63, 31, 253, 152, 177, 153, 103, 20, 39, 26, 248, 195 }, 286), Toast.LENGTH_LONG).show();
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
                Toast.makeText(this, UrlObfuscator.decode(new int[] { 120, 60, 17, 240, 196, 166, 129, 99, 12, 59, 15, 251, 197, 174, 149, 118, 77, 44, 12, 161, 201, 204, 254, 147, 121, 94, 62, 28, 252, 151, 176, 154, 102, 19, 38, 25, 249, 220 }, 52), Toast.LENGTH_LONG).show();
            }
        } else if (requestCode == NOTIFICATION_PERMISSION_REQUEST_CODE) {
            if (!(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                Toast.makeText(this, UrlObfuscator.decode(new int[] { 11, 11, 247, 203, 167, 137, 156, 127, 73, 53, 20, 244, 202, 248, 150, 100, 80, 116, 28, 244, 215, 240, 137, 97, 95, 108, 31, 226, 192, 187, 199, 103, 85, 52, 67, 175, 140, 224, 171, 139, 111, 82, 123, 14, 241, 221, 186, 214, 122, 90, 115, 27, 255, 144, 156, 139, 121, 88, 34, 4, 238, 219, 231, 135, 107, 93, 99, 22, 232, 205, 218 }, 69), Toast.LENGTH_LONG).show();
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
            UrlObfuscator.decode(new int[] { 50, 16, 242, 210, 167, 157, 100 }, 86), UrlObfuscator.decode(new int[] { 32, 227, 203, 161, 145, 99, 77 }, 103), NotificationManager.IMPORTANCE_DEFAULT);
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
                Toast.makeText(this, UrlObfuscator.decode(new int[] { 49, 249, 197, 161, 149, 127, 94, 113, 0, 234, 220, 160, 133, 120, 89, 32, 7, 233, 134, 178, 133, 112, 76, 102, 20, 95, 249, 207, 189, 149, 110, 92, 60, 87, 187, 152, 244, 156, 98, 84, 62, 79, 250, 197, 169, 203, 108, 64, 36, 2, 166, 195, 182, 140, 111, 1, 57, 48, 11, 239, 156, 159, 149, 110, 86, 59, 25, 244, 208, 160, 210, 119, 95, 35, 10, 232, 222, 235, 158, 102, 8, 46, 8, 246, 208, 162, 142, 109, 0, 82, 63, 19, 233, 218, 182, 149, 97 }, 120), Toast.LENGTH_LONG).show();
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
