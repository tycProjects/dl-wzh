package com.generated.ttspro;

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

        FrameLayout root = new FrameLayout(this);
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

        final SplashController loading = new StaticIconSplashView(this, Color.parseColor(UrlObfuscator.decode(new int[] { 32, 18, 113, 80, 79, 174, 141 }, 259)));
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
        final String offlineScrimColor = offlineIsNightMode ? UrlObfuscator.decode(new int[] { 55, 10, 107, 65, 160, 159, 254, 221, 60 }, 276) : UrlObfuscator.decode(new int[] { 6, 114, 85, 178, 145, 240, 239, 206, 45 }, 293);
        // Slightly translucent (not fully opaque) card/button fills, so a
        // hint of the dimmed scrim behind still shows through -- a cheap
        // stand-in for a true frosted-glass blur, which would need
        // RenderEffect (API 31+) and a snapshot of whatever's behind the
        // card to do for real.
        final String offlineCardBg = offlineIsNightMode ? UrlObfuscator.decode(new int[] { 24, 31, 65, 170, 244, 228, 176, 39, 3 }, 59) : UrlObfuscator.decode(new int[] { 111, 41, 178, 239, 250, 161, 52, 99, 118 }, 76);
        final String offlineTitleColor = offlineIsNightMode ? UrlObfuscator.decode(new int[] { 126, 58, 221, 252, 159, 190, 81 }, 93) : UrlObfuscator.decode(new int[] { 77, 189, 156, 251, 218, 57, 24 }, 110);
        final String offlineSubtitleColor = offlineIsNightMode ? UrlObfuscator.decode(new int[] { 92, 220, 133, 158, 195, 88, 125 }, 127) : UrlObfuscator.decode(new int[] { 179, 154, 140, 216, 78, 30, 15 }, 144);
        final String offlineButtonBg = offlineIsNightMode ? UrlObfuscator.decode(new int[] { 130, 248, 156, 202, 37, 8, 99, 78, 216 }, 161) : UrlObfuscator.decode(new int[] { 145, 233, 179, 75, 24, 9, 90, 207, 235 }, 178);
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
        errorTitle.setText(UrlObfuscator.decode(new int[] { 141, 141, 33, 78, 90, 42, 10, 243, 201, 177 }, 195));
        errorTitle.setTextColor(Color.parseColor(offlineTitleColor));
        errorTitle.setTextSize(16);
        errorTitle.setTypeface(errorTitle.getTypeface(), Typeface.BOLD);
        errorTitle.setGravity(Gravity.START);
        errorCard.addView(errorTitle);

        final TextView errorSubtitle = new TextView(this);
        errorSubtitle.setText(UrlObfuscator.decode(new int[] { 132, 159, 119, 80, 35, 10, 174, 223, 169, 159, 120, 80, 104, 16, 238, 192, 170, 195, 97, 78, 46, 49, 27, 254, 200, 190, 158, 57, 76, 56, 86, 252, 218, 167, 151, 99, 94, 42, 26 }, 212));
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
        retryText.setText(UrlObfuscator.decode(new int[] { 183, 97, 87, 48, 24 }, 229));
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
        navOverlay.setBackgroundColor(Color.parseColor(UrlObfuscator.decode(new int[] { 213, 37, 4, 99, 66, 161, 128 }, 246)));
        navOverlay.setVisibility(View.GONE);
        final BouncingDotsView navDots = new BouncingDotsView(this, Color.parseColor(UrlObfuscator.decode(new int[] { 36, 96, 119, 34, 177, 231, 132 }, 263)));
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
        String spoofedUA = defaultUA.replace(UrlObfuscator.decode(new int[] { 35, 23, 33, 3 }, 280), "").replaceAll("Version/[0-9.]+\s", "");
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
        webView.addJavascriptInterface(new AndroidBridge(), UrlObfuscator.decode(new int[] { 104, 38, 3, 244, 202, 173, 135, 64, 83, 41, 59, 25, 248 }, 297));

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
                    UrlObfuscator.decode(new int[] { 23, 56, 8, 242, 216, 174, 144, 119, 89, 126, 92, 239 }, 63) +
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
                    UrlObfuscator.decode(new int[] { 36, 29, 247, 214 }, 80) +
                    "var vp=document.querySelector('meta[name=\"viewport\"]');" +
                    UrlObfuscator.decode(new int[] { 8, 230, 183, 159, 171, 140, 50, 65, 47, 8, 170, 210, 186, 151, 102, 95, 52, 30, 251, 128, 174, 158, 110, 75, 61, 13, 194, 202, 160, 137, 102, 76, 53, 72, 88, 243, 216, 168, 154, 61, 16, 99, 1, 230, 155, 167, 150, 102, 112, 36, 27, 252, 196, 174, 158, 126, 76, 96, 64, 232, 196, 169, 134, 37, 13, 103, 41, 23, 248, 203, 171, 149, 107, 76, 112, 95, 174, 208, 188, 145, 100, 93, 42, 0, 249, 130, 163, 143, 104, 76, 105, 7, 245, 212, 166, 140, 101, 99, 87, 55, 17, 248, 147, 172, 137, 49, 12, 43 }, 97) +
                    UrlObfuscator.decode(new int[] { 4, 225, 158, 188, 139, 121, 109, 63, 30, 251, 193, 165, 147, 113, 65, 107, 69, 226, 207, 209, 170, 152, 114, 79, 125, 85, 191, 192, 191, 145, 96, 91, 111, 21, 245, 217, 167, 142, 105, 6, 61, 0, 236, 211, 174, 201, 36, 74, 44, 8, 244, 246, 223, 177, 209, 104, 89, 56, 20, 242, 139, 228, 218, 35, 30, 113, 29, 238, 214, 164, 129, 126, 71, 100, 27, 228, 199, 169, 129, 62, 19, 111, 80, 83, 190, 200, 175, 158, 104, 20, 43, 20, 247, 217, 181, 145, 126, 84, 109, 1, 225, 138, 229, 208 }, 114) +
                    UrlObfuscator.decode(new int[] { 234, 196, 233, 193, 155, 113, 94, 41, 22, 255, 215, 172, 217, 113, 80, 32, 54, 254, 212, 189, 138, 96, 89, 14, 18, 195, 205, 224, 192, 89, 122, 37, 13, 230, 211, 175, 182, 154, 75, 85, 62, 13, 233, 215, 165, 130, 83, 93, 43, 85, 184, 153, 180 }, 131) +
                    UrlObfuscator.decode(new int[] { 226, 210, 160, 209, 99, 91, 115, 9, 227, 200, 191, 132, 109, 73, 50, 75, 231, 209, 167, 128, 116, 122, 123, 49, 25, 246, 223, 183, 140, 63, 17, 38, 0, 234, 222, 180, 215, 38, 21, 62, 24, 165, 195, 173, 213, 32, 121, 26, 5, 237, 198, 179, 143, 150, 122, 107, 53, 30, 237, 201, 183, 133, 98, 115, 61, 11, 181, 138 }, 148) +
                    UrlObfuscator.decode(new int[] { 214, 176, 205, 118, 68, 56, 43, 61, 242, 210, 175, 159, 119, 76, 106, 81, 253, 192, 190, 158, 61, 82, 32, 10, 244, 215, 166, 139, 113, 5, 48, 15, 225, 208, 171, 216, 48, 16, 15, 40, 10, 189, 210, 183, 137, 119, 69, 34, 20, 250, 199, 233, 158, 102, 74, 60, 11, 224, 196, 189, 196, 112, 29, 46, 12, 224, 199, 167, 143, 33, 118, 83, 45, 19, 233, 206, 184, 150, 99, 13, 120, 3, 246, 208, 186, 153, 123, 3, 57, 9, 243, 222, 228, 155, 110, 92, 32, 73, 226, 198, 171, 149, 140, 106, 7, 109, 75, 170, 156, 249, 158, 123, 69, 59, 1, 230, 208, 190, 155, 53, 89, 41, 19, 254, 132, 187, 142, 124, 64, 105, 2, 230, 203, 181, 172, 138, 39, 13, 107, 74, 188, 153, 190, 155, 101, 91, 33, 6, 240, 222, 187, 213, 112, 11, 112 }, 165) +
                    UrlObfuscator.decode(new int[] { 210, 186, 151, 102, 95, 52, 30, 251, 128, 165, 137, 106, 78, 103, 9, 247, 214, 160, 138, 103, 97, 41, 9, 19, 250, 149, 175, 143, 51, 2, 37 }, 182) +
                    UrlObfuscator.decode(new int[] { 186, 133, 100, 80, 32, 10, 169, 197, 150, 165, 128 }, 199) +
                    "try{var m=document.querySelector('meta[name=\"theme-color\"]');" +
                    UrlObfuscator.decode(new int[] { 177, 145, 62, 88, 114, 85, 229, 216, 190, 139, 97, 90, 98, 42, 228, 205, 186, 136, 111, 65, 6, 17, 235, 197, 167, 186, 216, 59, 125, 53, 30, 235, 215, 190, 146, 87, 70, 58, 22, 246, 213, 225, 143, 125, 92, 39, 19, 221, 192, 162, 139, 96, 103, 44, 14, 238, 210, 150, 165, 188, 114, 95, 40, 22, 241, 211, 148, 135, 125, 87, 53, 20, 190, 206, 190, 157, 96, 82, 30, 1, 237, 202, 163, 166, 107, 79, 45, 19, 168, 242, 144, 186, 153, 111, 123, 45, 12, 229, 223, 183, 129, 103, 87, 121, 87, 236, 193, 163, 152, 110, 68, 61, 79, 174, 218, 185, 195, 36, 11, 122, 29 }, 216) +
                    UrlObfuscator.decode(new int[] { 148, 107, 70, 50, 6, 236, 139, 167, 200, 123, 98 }, 233) +
                    UrlObfuscator.decode(new int[] { 142, 107, 65, 44, 31, 243, 156, 164, 155, 127, 84, 32, 25, 163, 237, 165, 142, 123, 71, 46, 2, 199, 214, 170, 134, 102, 69, 25, 120, 60, 242, 223, 168, 150, 113, 83, 20, 7, 253, 215, 181, 148, 62, 78, 62, 29, 224, 210, 154, 136, 111, 66, 4, 4, 231, 200, 165, 147, 111, 106, 80, 57, 85, 224 }, 250) +
                    UrlObfuscator.decode(new int[] { 125, 75, 59, 72, 247, 207, 166, 143, 65, 69, 124, 6, 10, 240, 222, 168, 146, 117, 87, 112, 94, 237 }, 267) +
                    UrlObfuscator.decode(new int[] { 106, 90, 40, 89, 251, 223, 183, 156, 122, 14, 9, 44, 171, 217, 175, 159, 44, 78, 119, 13, 231, 196, 179, 136, 97, 77, 54, 79, 229, 243, 219, 176, 153, 117, 78, 31, 10, 248, 219, 133, 155, 122, 92, 37, 88, 194, 207, 185, 132, 37, 76, 37, 7, 232, 212, 237, 147, 106, 76, 37, 15, 8, 176, 212, 178, 149, 127, 75, 15, 30, 242, 193, 188, 220, 32, 24, 124, 24, 231, 195, 168, 132, 125, 7, 33, 9, 232, 192, 182, 171, 103, 72, 39, 55, 10, 176, 142, 242, 193 }, 284) +
                    UrlObfuscator.decode(new int[] { 69, 57, 25, 227, 203, 229, 137, 34, 81, 42, 0, 230, 207, 171, 202, 115, 87, 50, 8, 87, 251, 148, 231, 158, 39, 92, 118, 7, 247, 199, 177, 157, 102, 116, 60, 10, 227, 200, 162, 159, 49, 84 }, 50) +
                    UrlObfuscator.decode(new int[] { 42, 4, 169, 196, 208, 189, 136, 113, 94, 52, 13, 182, 213, 185, 145, 109, 21, 116, 18, 248, 206, 167, 131, 34, 66, 36, 13, 237, 223, 137, 131, 44, 71, 45, 2, 245, 242, 219, 179, 136, 53, 88, 54, 28, 238, 159, 233, 196, 58, 81, 57, 17, 230, 192, 227, 156, 126, 89, 33, 64, 227, 201, 166, 145, 110, 71, 47, 20, 81, 252, 210, 184, 130, 51, 2 }, 67) +
                    UrlObfuscator.decode(new int[] { 61, 21, 186, 210, 184, 142, 103, 67, 98, 2, 228, 205, 173, 159, 73, 67, 108, 7, 237, 194, 181, 178, 155, 115, 72, 117, 30, 246, 219, 162, 155, 112, 90, 39, 55, 253, 213, 162, 139, 99, 88, 98, 86, 185, 129, 164, 142, 100, 77, 45, 76, 241, 213, 204, 182, 213, 120, 84, 57, 12, 245, 210, 184, 129, 58, 87, 61, 18, 229, 194, 171, 131, 120, 110, 38, 12, 229, 194, 168, 145, 45, 24 }, 84) +
                    UrlObfuscator.decode(new int[] { 19, 229, 209, 226, 145, 97, 109, 77, 56, 65, 253, 207, 183, 155, 99, 95, 58, 26, 187, 193, 248, 139, 102, 72, 101, 77, 248, 131, 187, 141, 115, 83, 55, 10, 163, 204, 180, 140, 147, 37, 75, 61, 9, 186, 216, 229, 132, 56, 92, 58, 23, 247, 201, 159, 137, 38, 10, 100, 76, 163, 146, 190, 134, 116, 5, 38, 94, 241, 143, 169, 177, 154, 120, 68, 20, 28, 177, 159, 254, 209, 60, 15, 58, 20, 185, 209, 243, 222, 113, 80, 41, 86, 185, 129, 181, 131, 113, 81, 49, 12, 161, 206, 202, 178, 145, 39 }, 101) +
                    UrlObfuscator.decode(new int[] { 0, 244, 198, 243, 132, 44, 67, 97, 29, 248, 206, 184, 158, 123, 65, 41, 1, 173, 197, 232, 211, 45, 66, 22, 112, 14, 236, 215, 179, 141, 48, 16, 122, 82, 189, 136, 187, 151, 56, 89, 96, 1, 233, 197, 173, 157, 96, 27, 117, 76, 246, 198, 182, 148, 114, 113, 30, 51, 9, 247, 214, 226 }, 118) +
                    UrlObfuscator.decode(new int[] { 241, 199, 183, 196, 98, 78, 124, 22, 81, 242, 216, 178, 156, 110, 81, 102, 68, 169, 197, 181, 129, 97, 84, 22, 3, 225, 204, 184, 195, 124, 114, 123, 58, 175, 159, 245, 216, 116, 64, 50, 127, 12, 160, 204, 186, 136, 106, 93, 30, 24, 225, 156, 165, 169, 33, 109, 99, 95, 189, 133, 231, 141, 52, 88, 38, 20, 246, 193, 138, 140, 117, 8, 73, 5, 76, 193, 151, 235, 201, 49, 27, 52, 25, 169, 195, 179, 131, 99, 74, 7, 3, 248, 131, 188, 178, 58, 122, 106, 84, 180, 138, 249 }, 135) +
                    UrlObfuscator.decode(new int[] { 241, 209, 254, 156, 103, 125, 51, 63, 184, 221, 231, 145, 112, 66, 57, 39, 233, 233, 238, 130, 45, 95, 62, 8, 243, 209, 223, 147, 212, 121, 86, 112, 81, 229, 211, 161, 129, 97, 92, 113, 30, 250, 194, 161, 215, 121, 79, 61, 29, 245, 200, 190, 150, 57, 80, 109, 7, 69, 249, 145, 190, 193, 120, 85, 116, 22, 172, 212, 184, 142, 41, 76, 107 }, 152) +
                    UrlObfuscator.decode(new int[] { 223, 169, 149, 38, 77, 33, 27, 191, 199, 181, 177, 157, 105, 85, 52, 20, 177, 219, 254, 141, 99, 85, 33, 82, 249, 141, 169, 155, 99, 79, 63, 3, 230, 198, 239, 136, 44, 95, 45, 95, 204, 193, 203, 182, 211, 113, 90, 34, 81, 168, 155, 155, 148, 96, 91, 124, 28, 249, 193, 230, 223, 57, 30, 102, 7, 161, 142, 253, 151, 97, 87, 55, 19, 238, 191, 150, 179, 192, 42, 12, 102, 95, 167, 145, 239, 211, 52, 27, 122, 30, 161, 218, 162, 191, 127, 88, 32, 6, 224, 142, 244, 210, 42, 25, 60, 91, 13, 251, 201, 169, 137, 116, 25, 127, 84, 177, 158, 188, 219, 113, 31, 34, 70, 165, 197, 228, 136, 36, 78, 97, 76, 238, 141, 167, 205, 96, 8, 123, 34, 69 }, 169) +
                    UrlObfuscator.decode(new int[] { 220, 182, 138, 63, 64, 52, 6, 179, 217, 236, 192, 52, 69, 113, 15, 227, 203, 160, 134, 41, 74, 32, 10, 228, 214, 169, 219, 148, 53, 22, 117, 0, 236, 216, 170, 215, 117, 8, 36, 18, 224, 194, 181, 199, 105, 72, 56, 40, 229, 196, 184, 146, 114, 64, 32, 48, 246, 216, 172, 186, 214, 126, 84, 58, 19, 247, 227, 188, 171, 60, 26, 49, 19, 242, 219, 168, 156, 98, 89, 37, 14, 202, 199, 171, 137, 119, 13, 120, 11, 231, 136, 220, 248, 219, 127, 21, 59, 71, 165, 135, 248, 204, 45, 26, 32, 20, 228, 218, 188, 131, 44, 67, 47, 17, 160, 196, 239, 222, 121 }, 186) +
                    UrlObfuscator.decode(new int[] { 189, 139, 123, 8, 51, 91, 245, 197, 177, 145, 100, 8, 88, 59, 9, 223, 212, 183, 137, 109, 67, 51, 17, 199, 199, 171, 157, 117, 7, 42, 2, 239, 222, 167, 140, 102, 83, 104, 7, 235, 199, 187, 157, 124, 123, 81, 62, 9, 246, 223, 183, 140, 57, 82, 58, 23, 230, 223, 180, 158, 123, 107, 33, 9, 230, 207, 167, 156, 46, 8, 38, 11, 239, 205, 179, 201, 196 }, 203) +
                    UrlObfuscator.decode(new int[] { 181, 157, 50, 77, 126, 81, 190, 133, 250, 193, 43, 8, 122, 27, 160, 223, 231, 219, 36, 28, 112, 80, 172, 209, 234, 132, 41, 17, 110, 110, 79, 169, 150, 175, 212, 123, 17, 120, 68, 160, 129, 237, 194, 63, 6, 102, 28, 232, 216, 190, 152, 103, 8, 96, 69, 181, 145, 243, 215, 49, 21, 24, 101 }, 220) +
                    UrlObfuscator.decode(new int[] { 159, 105, 95, 63, 27, 230, 135, 225, 198, 98, 69, 36, 7, 230, 249, 153, 230, 129, 32 }, 237) +
                    UrlObfuscator.decode(new int[] { 136, 124, 78, 123, 9, 252, 214, 179, 180, 114, 9, 53, 7, 255, 211, 187, 135, 98, 66, 99, 67, 242, 220, 181, 159, 126, 82, 34, 16, 161, 200, 130, 174, 148, 127, 80, 24, 30, 176, 158, 237, 156, 114, 27, 58, 80, 173, 146, 185, 132, 98, 79, 37, 30, 166, 248, 153, 132, 106, 71, 48, 14, 233, 251, 252, 186, 176, 122, 73, 45, 81, 236, 193, 188, 154, 119, 93, 38, 94, 208, 241, 172, 130, 111, 88, 38, 1, 227, 228, 162, 168, 98, 81, 53, 93, 23, 165, 252, 178, 159, 104, 86, 49, 19, 212, 199, 189, 151, 117, 84, 126, 14, 254, 221, 160, 146, 90, 72, 47, 2, 196, 196, 167, 136, 101, 83, 47, 42, 16, 249, 148, 179, 211, 34, 69, 42, 21, 244, 192, 176, 154, 57, 85, 102, 21, 240, 209, 240 }, 254) +
                    UrlObfuscator.decode(new int[] { 124, 75, 35, 8, 201, 205, 225, 193, 60 }, 271) +
                    UrlObfuscator.decode(new int[] { 73, 89, 118, 92, 235, 210, 180, 157, 119, 64, 120, 42, 203, 210, 188, 149, 98, 64, 39, 9, 206, 204, 136, 134, 125, 73, 34, 76, 255, 212, 171, 143, 100, 112, 73, 115, 35, 196, 219, 183, 156, 101, 89, 60, 16, 209, 213, 147, 159, 122, 64, 41, 81, 255, 216, 188, 141, 60, 66, 42, 7, 246, 207, 164, 142, 139, 48, 92, 56, 31, 223, 207, 189, 153, 98, 121, 61, 0, 230, 212, 190, 138, 124, 5, 107, 13, 229, 202, 189, 148, 111, 75, 99, 79, 241, 196, 174, 187, 188, 122, 16, 47, 8, 236, 221, 254, 205, 104 }, 288) +
                    UrlObfuscator.decode(new int[] { 75, 40, 23, 242, 198, 178, 152, 39, 75, 100, 23, 246 }, 54) +
                    UrlObfuscator.decode(new int[] { 51, 20, 252, 223, 170, 132, 41, 1, 72, 55, 19, 248, 212, 173, 215, 71, 104, 55, 27, 240, 193, 189, 152, 116, 103, 47, 29, 248, 194, 169, 154, 74, 72, 51, 11, 224, 138, 185, 150, 105, 113, 90, 50, 11, 181, 229, 134, 153, 121, 82, 39, 27, 250, 214, 153, 145, 127, 90, 36, 15, 248, 232, 166, 157, 105, 66, 120, 16, 241, 215, 164, 219 }, 71) +
                    UrlObfuscator.decode(new int[] { 60, 24, 245, 192, 185, 150, 124, 69, 126, 14, 234, 201, 137, 157, 111, 71, 60, 43, 239, 214, 176, 134, 108, 68, 50, 119, 89, 254, 208, 178, 153, 114, 31, 123, 16, 224, 218, 176, 134, 120, 95, 33, 70, 232, 133, 176 }, 88) +
                    "var t=e.target;var el=t&&t.closest?t.closest('button,a,[role=\"button\"],input[type=\"button\"],input[type=\"submit\"]'):null;" +
                    UrlObfuscator.decode(new int[] { 0, 238, 143, 163, 137, 34, 5, 53, 8, 238, 251, 209, 170, 210, 90, 84, 61, 10, 248, 223, 177, 182, 97, 91, 53, 23, 234, 136, 235, 173, 101, 78, 59, 7, 238, 194, 135, 150, 106, 70, 38, 5, 81, 232, 212, 190, 137, 123, 77, 61, 94, 237, 244, 186, 151, 96, 94, 57, 11, 204, 223, 165, 143, 109, 76, 102, 17, 239, 199, 182, 130, 118, 68, 104, 118, 69, 224 }, 105) +
                    UrlObfuscator.decode(new int[] { 7, 181, 204, 165, 131, 112, 29, 104, 15, 236, 211, 174, 154, 110, 68, 99, 15, 160, 211, 186 }, 122) +
                    // Polyfills the Web Share API on top of the real Android
                    // share sheet -- most WebView builds don't implement
                    // navigator.share at all, so a page's own "Share" button
                    // either does nothing or falls back to a hand-rolled
                    // copy-link menu instead of the native chooser. Only
                    // installed when the page doesn't already have a working
                    // navigator.share (some newer WebView versions do).
                    UrlObfuscator.decode(new int[] { 255, 216, 176, 147, 110, 64, 109, 19, 234, 204, 165, 143, 136, 48, 124, 50, 31, 232, 214, 177, 147, 84, 71, 61, 23, 245, 212, 246, 201, 79, 67, 40, 25, 229, 192, 172, 165, 116, 76, 32, 4, 231, 143, 179, 183, 159, 111, 89, 125, 92, 184, 214, 182, 128, 124, 83, 50, 6, 254, 194, 225, 157, 101, 77, 57, 15, 160, 211 }, 139) +
                    UrlObfuscator.decode(new int[] { 242, 218, 172, 144, 127, 86, 34, 26, 230, 157, 161, 153, 113, 93, 43, 80, 234, 222, 164, 138, 124, 78, 41, 11, 172, 199, 163, 149, 97, 54, 69 }, 156) +
                    UrlObfuscator.decode(new int[] { 201, 173, 159, 107, 20, 44, 6, 242, 196, 184, 159, 121, 92, 123 }, 173) +
                    UrlObfuscator.decode(new int[] { 202, 175, 133, 96, 123, 55, 28, 229, 217, 188, 144, 81, 64, 56, 20, 232, 203, 227, 159, 99, 75, 59, 13, 175, 245, 177, 150, 106, 76, 38, 72, 27, 255, 201, 189, 213, 110, 80, 44, 27, 243, 201, 168, 212, 53, 24, 124, 60, 250, 223, 165, 133, 109, 1, 44, 6, 242, 196, 234, 151, 103, 89, 52, 35, 2, 186, 155, 242, 214, 74, 76, 37, 31, 251, 211, 251, 150, 112, 68, 46, 64, 248, 222, 167, 150, 117, 15, 96, 79, 172, 159 }, 190) +
                    UrlObfuscator.decode(new int[] { 189, 139, 121, 89, 57, 4, 169, 248, 181, 137, 104, 77, 48, 7, 175, 210, 218, 173, 146, 112, 77, 63, 81, 177, 140, 171, 150, 117, 71, 49, 25, 184, 202, 231, 150, 126, 78, 62, 28, 250, 201, 230, 181, 118, 76, 47, 8, 243, 250, 144, 175, 153, 113, 95, 58, 12, 191, 211, 252, 207, 110, 79, 106 }, 207) +
                    UrlObfuscator.decode(new int[] { 142, 158, 104, 84, 59, 26, 238, 214, 170, 217, 117, 84, 58, 32, 250, 208, 162, 138, 51, 75, 57, 5, 233, 221, 161, 136, 104, 13, 109, 24, 240, 196, 180, 170, 140, 115, 28, 47, 8, 236, 221, 236, 139, 46 }, 224) +
                    UrlObfuscator.decode(new int[] { 140, 109, 76, 47, 25, 239, 195, 226, 140, 33, 92, 59 }, 241) +
                    // Reports the scrollTop of whichever element just
                    // scrolled -- document or any nested panel -- so native
                    // knows whether a pull-to-refresh gesture is actually
                    // safe (see SwipeRefreshLayout override above). Scroll
                    // events don't bubble, so this has to be a capture
                    // listener on window to see scrolling from any
                    // descendant, not just the document itself. Throttled
                    // with a trailing rAF flag so a fast scroll doesn't
                    // spam the JS bridge with a call per pixel.
                    UrlObfuscator.decode(new int[] { 118, 83, 57, 36, 23, 251, 148, 250, 141, 112, 86, 51, 25, 226, 154, 140, 173, 112, 94, 43, 28, 226, 197, 175, 185, 106, 90, 40, 10, 233, 230, 172, 151, 111, 68, 22, 37, 10, 245, 213, 190, 150, 111, 25, 9, 42, 245, 221, 182, 131, 127, 70, 42, 62, 239, 217, 165, 133, 100, 101, 41, 16, 234, 199, 255, 149, 114, 106, 91, 102 }, 258) +
                    UrlObfuscator.decode(new int[] { 101, 83, 35, 80, 255, 203, 163, 136, 98, 68, 46, 85, 225, 199, 169, 151, 102, 25 }, 275) +
                    UrlObfuscator.decode(new int[] { 83, 42, 12, 229, 207, 200, 240, 156, 120, 95, 31, 15, 253, 217, 162, 185, 125, 64, 38, 20, 254, 202, 188, 197, 43, 88, 41, 27, 231, 203, 170, 194, 40, 69, 55, 15, 227, 235, 215, 178, 146, 51, 95, 112, 3 }, 292) +
                    UrlObfuscator.decode(new int[] { 83, 63, 80, 231, 211, 187, 144, 122, 92, 54, 89, 253, 203, 185, 153, 121, 68, 114, 24, 226, 200, 161, 141, 109, 69, 124, 20, 13, 235, 216, 231 }, 58) +
                    UrlObfuscator.decode(new int[] { 57, 15, 248, 221, 162, 149, 113, 101, 45, 11, 236, 193, 203, 183, 146, 114, 125, 40, 24, 245, 210, 254, 147, 97, 93, 49, 5, 249, 192, 160, 197, 37, 80, 58, 12, 230, 195, 175, 139, 99, 30, 36, 0, 236, 236, 219, 230 }, 75) +
                    UrlObfuscator.decode(new int[] { 42, 26, 232, 153, 189, 155, 43, 29, 49, 93, 230, 208, 162, 136, 107, 89, 106, 77, 239, 135, 188, 134, 116, 66, 33, 23, 172, 207, 175, 187, 155, 73, 69, 43, 31, 164, 133, 234, 199, 60, 11, 54, 92, 229, 209, 189, 137, 104, 88, 113, 14, 230, 203, 178, 139, 96, 74, 55, 76, 242, 195, 205, 177, 145, 112, 82, 52, 30, 221, 219, 179, 152, 113, 93, 38, 74 }, 92) +
                    UrlObfuscator.decode(new int[] { 27, 237, 217, 234, 157, 103, 87, 123, 0, 232, 156, 167, 141, 46, 108, 93, 47, 19, 247, 214, 141, 151, 103, 12, 101, 79 }, 109) +
                    UrlObfuscator.decode(new int[] { 23, 251, 148, 172, 147, 119, 92, 56, 1, 187, 245, 189, 150, 99, 95, 38, 10, 207, 222, 162, 142, 110, 77, 97, 64, 196, 202, 167, 144, 110, 73, 91, 28, 15, 245, 223, 189, 156, 54, 69, 51, 5, 251, 193, 166, 162, 115, 93, 33, 1, 224, 255, 165, 153, 33, 92, 7, 11, 224, 209, 173, 136, 100, 93, 76, 52, 24, 252, 223, 247, 138, 114, 70, 58, 6, 231, 225, 178, 130, 96, 66, 33, 56, 228, 218, 225, 156, 104, 86, 57, 84, 170, 153, 188 }, 126) +
                    UrlObfuscator.decode(new int[] { 242, 135, 246, 145, 39, 94, 59, 29, 226, 143, 254, 153, 126, 65, 32, 20, 28, 246, 149, 185, 210, 97, 68 }, 143) +
                    UrlObfuscator.decode(new int[] { 221, 150, 246, 212, 39 }, 160),
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
                if (!UrlObfuscator.decode(new int[] { 246, 149, 187 }, 177).equalsIgnoreCase(request.getMethod())) {
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
                popupSettings.setUserAgentString(popupDefaultUA.replace(UrlObfuscator.decode(new int[] { 249, 193, 119, 105 }, 194), "").replaceAll("Version/[0-9.]+\s", ""));
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
                        if (scheme != null && !scheme.equals(UrlObfuscator.decode(new int[] { 187, 134, 101, 64 }, 211)) && !scheme.equals(UrlObfuscator.decode(new int[] { 140, 119, 86, 49, 19 }, 228))) {
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
        if (Intent.ACTION_SEND.equals(action) && UrlObfuscator.decode(new int[] { 129, 113, 75, 38, 94, 224, 195, 175, 132, 98 }, 245).equals(intent.getType())) {
            String shared = intent.getStringExtra(Intent.EXTRA_TEXT);
            if (shared != null && !shared.isEmpty()) {
                try {
                    return baseUrl() + UrlObfuscator.decode(new int[] { 57, 86, 44, 2, 240, 196, 164, 128, 138, 120, 68, 47, 71 }, 262) + java.net.URLEncoder.encode(shared, UrlObfuscator.decode(new int[] { 66, 98, 19, 89, 171 }, 279));
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

        String shortcutAction = intent.getStringExtra(UrlObfuscator.decode(new int[] { 91, 47, 9, 247, 208, 160, 151, 117, 127, 94, 61, 9, 245, 212, 180 }, 296));
        if (UrlObfuscator.decode(new int[] { 76, 56, 16, 244, 219, 189 }, 62).equals(shortcutAction)) {
            webView.getSettings().setCacheMode(WebSettings.LOAD_NO_CACHE);
            webView.reload();
            webView.getSettings().setCacheMode(WebSettings.LOAD_CACHE_ELSE_NETWORK);
            return;
        }
        if (UrlObfuscator.decode(new int[] { 39, 1, 224, 201 }, 79).equals(shortcutAction)) {
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
                    sendIntent.setType(UrlObfuscator.decode(new int[] { 20, 26, 230, 201, 243, 139, 118, 88, 49, 25 }, 96));
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
                String safeTitle = (title == null || title.trim().isEmpty()) ? UrlObfuscator.decode(new int[] { 37, 228, 220, 238, 189, 94, 100 }, 113) : title;
                String safeBody = body == null ? "" : body;
                NotificationCompat.Builder notification = new NotificationCompat.Builder(MainActivity.this, UrlObfuscator.decode(new int[] { 230, 196, 166, 190, 139, 113, 72 }, 130))
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
            if (hex == null || !hex.matches(UrlObfuscator.decode(new int[] { 205, 145, 249, 171, 78, 3, 11, 13, 166, 204, 249, 197, 62, 123, 62, 82, 254, 222, 154, 161, 210, 88, 92, 113, 29, 170, 148, 225, 170, 109, 6, 41, 90, 182 }, 147))) return;
            runOnUiThread(() -> {
                try {
                    int color = Color.parseColor(hex);
                    getWindow().setStatusBarColor(color);
                    getWindow().setNavigationBarColor(color);
                    boolean light = isLightColor(color);
                    WindowInsetsControllerCompat controller =
                        WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
                    if (controller != null) {
                        controller.setAppearanceLightStatusBars(light);
                        controller.setAppearanceLightNavigationBars(light);
                    }
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
            if (hex == null || !hex.matches(UrlObfuscator.decode(new int[] { 250, 224, 185, 64, 13, 121, 63, 80, 250, 139, 247, 192, 69, 76, 96, 8, 176 }, 164))) return;
            runOnUiThread(() -> {
                try {
                    int color = Color.parseColor(hex);
                    getWindow().setBackgroundDrawable(new ColorDrawable(color));
                    if (webView != null) webView.setBackgroundColor(color);
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
                        Uri.parse(UrlObfuscator.decode(new int[] { 197, 181, 144, 121, 80, 55, 10, 180 }, 181) + getPackageName()));
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
            if (Build.TAGS != null && Build.TAGS.contains(UrlObfuscator.decode(new int[] { 178, 128, 119, 87, 111, 10, 229, 230, 205 }, 198))) return true;
            String[] suPaths = {
                UrlObfuscator.decode(new int[] { 248, 133, 108, 71, 39, 23, 252, 159, 173, 135, 99, 3, 56, 31 }, 215), UrlObfuscator.decode(new int[] { 199, 116, 95, 54, 16, 230, 207, 238, 152, 157, 119, 83, 115, 8, 239 }, 232), UrlObfuscator.decode(new int[] { 214, 107, 85, 63, 27, 187, 192, 167 }, 249),
                UrlObfuscator.decode(new int[] { 37, 90, 49, 20, 242, 192, 169, 204, 99, 81, 48, 112, 45, 232, 204, 190, 136, 108, 75, 50, 4, 187, 213, 163, 153 }, 266), UrlObfuscator.decode(new int[] { 52, 73, 32, 11, 227, 211, 184, 219, 114, 66, 33, 95, 220, 219, 189, 137, 121, 121, 28, 70, 230, 214, 174 }, 283),
                UrlObfuscator.decode(new int[] { 30, 52, 14, 250, 204, 227, 135, 101, 74, 41, 11, 169, 221, 166, 138, 108, 14, 51, 42 }, 49), UrlObfuscator.decode(new int[] { 109, 5, 225, 235, 223, 242, 144, 116, 89, 56, 20, 184, 212, 188, 154, 60, 65, 36 }, 66), UrlObfuscator.decode(new int[] { 124, 22, 240, 196, 174, 193, 97, 67, 40, 11, 229, 135, 180, 147 }, 83),
                UrlObfuscator.decode(new int[] { 75, 240, 215, 238, 130, 150, 112, 18, 47, 14 }, 100), UrlObfuscator.decode(new int[] { 90, 231, 202, 161, 133, 117, 66, 97, 15, 229, 197, 229, 143, 105, 78, 42, 22, 229, 197, 167, 206, 115, 106 }, 117), UrlObfuscator.decode(new int[] { 169, 214, 189, 144, 118, 68, 45, 112, 13, 249, 147, 163, 152, 112, 86, 120, 5, 224 }, 134)
            };
            for (String path : suPaths) {
                if (new File(path).exists()) return true;
            }
            // Magisk hides its su paths on some configs but the manager
            // app itself is still installed under its own package name.
            String[] knownRootPackages = { UrlObfuscator.decode(new int[] { 244, 217, 184, 218, 103, 93, 33, 26, 224, 198, 163, 155, 126, 4, 36, 9, 224, 207, 182, 143 }, 151), UrlObfuscator.decode(new int[] { 205, 178, 200, 102, 76, 34, 11, 239, 198, 214, 172, 152, 50, 72, 47, 9, 253, 197, 165, 128 }, 168), UrlObfuscator.decode(new int[] { 218, 183, 154, 56, 91, 59, 0, 250, 196, 182, 128, 123, 3, 45, 5, 238, 219, 167, 142, 98, 11, 55, 22 }, 185) };
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
                        UrlObfuscator.decode(new int[] { 158, 129, 97, 84, 102, 3, 225, 194, 182, 148, 114, 122, 30, 51, 25, 254, 222, 170, 216, 118, 22, 39, 27, 252, 198, 180, 148, 47, 74, 40, 26, 226, 201, 172, 200, 115, 73, 101, 19, 236, 208, 170, 206 }, 202), Toast.LENGTH_SHORT).show();
                    webView.evaluateJavascript(
                        UrlObfuscator.decode(new int[] { 243, 156, 108, 86, 52, 2, 252, 219, 189, 218, 56, 75, 59, 28, 244, 215, 162, 140, 33, 95, 46, 8, 225, 203, 180, 204, 110, 78, 126, 48, 25, 238, 212, 179, 157, 74, 88, 57, 1, 213, 208, 177, 148, 99, 92, 28, 8, 255, 222, 166, 157, 33, 92 }, 219) +
                        UrlObfuscator.decode(new int[] { 155, 98, 68, 45, 7, 240, 136, 170, 138, 66, 76, 37, 18, 16, 247, 217, 142, 148, 117, 77, 25, 20, 245, 208, 167, 128, 64, 84, 35, 26, 226, 217, 228, 141, 107, 69, 59, 2, 175, 158 }, 236) +
                        UrlObfuscator.decode(new int[] { 128, 97, 88, 59, 13, 251, 223, 254, 144, 61, 72, 47 }, 253) +
                        UrlObfuscator.decode(new int[] { 122, 95, 53, 16, 253, 192, 166, 131, 105, 82, 106, 7, 235, 210, 176, 190, 138, 126, 84, 30, 12, 252, 214, 163, 222, 123, 81, 36, 82, 210, 197, 188, 154, 98, 65, 14, 28, 236, 198, 179, 206, 34, 69, 45, 6, 243, 207, 214, 186, 175, 115, 84, 46, 56, 251, 212, 179, 134, 103, 97, 55, 2, 229, 195, 186, 202, 32 }, 270) +
                        UrlObfuscator.decode(new int[] { 100, 90, 56, 8, 250, 211, 181, 194, 108, 81, 39, 21, 253, 198, 180, 148, 53, 72, 44, 0, 248, 207, 180, 149, 46, 15, 126, 25, 224, 195, 181, 131, 151, 54, 88, 117, 0, 231 }, 287) +
                        UrlObfuscator.decode(new int[] { 72, 125, 91, 187, 138 }, 53), null);
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
                    UrlObfuscator.decode(new int[] { 110, 3, 241, 205, 161, 149, 105, 112, 80, 117, 85, 224, 206, 171, 129, 108, 95, 51, 92, 228, 219, 191, 148, 96, 89, 99, 3, 229, 235, 167, 140, 117, 73, 44, 0, 209, 205, 174, 148, 190, 125, 94, 57, 8, 233, 235, 189, 132, 99, 89, 32, 90, 233 }, 70) +
                    UrlObfuscator.decode(new int[] { 32, 31, 251, 208, 188, 133, 63, 95, 33, 47, 227, 200, 185, 133, 96, 76, 21, 9, 234, 208, 130, 129, 98, 69, 76, 45, 47, 249, 200, 175, 149, 108, 31 }, 87) + result + ");" +
                    UrlObfuscator.decode(new int[] { 21, 250, 197, 164, 144, 96, 74, 105, 5, 86, 229, 192 }, 104) +
                    UrlObfuscator.decode(new int[] { 13, 234, 206, 173, 130, 125, 93, 54, 30, 231, 129, 170, 132, 127, 91, 43, 29, 235, 207, 131, 147, 97, 77, 54, 73, 238, 250, 201, 253, 191, 110, 73, 45, 23, 250, 243, 163, 145, 125, 70, 121, 87, 238, 192, 169, 158, 100, 67, 45, 58, 232, 201, 177, 165, 96, 65, 36, 19, 12, 204, 216, 175, 142, 118, 77, 127, 91 }, 121) +
                    UrlObfuscator.decode(new int[] { 241, 205, 173, 147, 103, 76, 40, 89, 249, 198, 178, 190, 144, 105, 89, 63, 64 }, 138) + result + UrlObfuscator.decode(new int[] { 230, 199, 240, 209, 44, 75, 54, 21, 231, 209, 185, 216, 106, 7, 54, 17 }, 155) +
                    UrlObfuscator.decode(new int[] { 209, 226, 194, 32, 19 }, 172), null));
            }).start();
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
    // its own "Tts PRO" subfolder in there (DownloadManager
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
            request.addRequestHeader(UrlObfuscator.decode(new int[] { 232, 175, 158, 104, 20, 25, 16, 243, 219, 160 }, 189), userAgent);
            if (cookie != null) request.addRequestHeader(UrlObfuscator.decode(new int[] { 141, 130, 99, 64, 35, 12 }, 206), cookie);
            request.setTitle(filename);
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
            request.setDestinationInExternalPublicDir(
                Environment.DIRECTORY_DOWNLOADS, UrlObfuscator.decode(new int[] { 139, 138, 110, 28, 11, 40, 214, 151 }, 223) + filename);
            request.setAllowedOverMetered(true);
            request.setAllowedOverRoaming(true);
            request.setVisibleInDownloadsUi(true);

            DownloadManager downloadManager = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
            pendingDownloadId = downloadManager.enqueue(request);
            startDownloadProgressPolling(pendingDownloadId);
            Toast.makeText(this, UrlObfuscator.decode(new int[] { 180, 96, 89, 35, 0, 228, 203, 173, 129, 105, 65, 101 }, 240) + filename + "…", Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Toast.makeText(this, UrlObfuscator.decode(new int[] { 69, 79, 72, 48, 17, 243, 218, 190, 217, 126, 86, 63, 25, 241, 215, 242, 220, 61, 15, 45, 5, 233, 200, 161, 201, 113, 72, 51, 23, 164, 192, 173, 143, 110, 122, 93, 41, 21, 244, 212, 249, 153, 121, 82, 117, 0, 225, 203, 241, 145, 104, 79, 36, 2 }, 257), Toast.LENGTH_LONG).show();
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
            String title = (titleIdx >= 0 && cursor.getString(titleIdx) != null) ? cursor.getString(titleIdx) : UrlObfuscator.decode(new int[] { 84, 88, 60, 10 }, 274);

            if (status == DownloadManager.STATUS_SUCCESSFUL) {
                String localUriStr = uriIdx >= 0 ? cursor.getString(uriIdx) : null;
                String folderName = UrlObfuscator.decode(new int[] { 103, 45, 22, 238, 243, 209, 188, 152, 104 }, 291);
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
                Toast.makeText(this, title + UrlObfuscator.decode(new int[] { 25, 60, 24, 225, 219, 184, 156, 115, 85, 53, 11, 174, 8377, 236, 141, 99, 71, 44, 71, 239, 209, 228, 138, 108, 1, 57, 48, 11, 239, 156 }, 57) + folderName + UrlObfuscator.decode(new int[] { 106, 15, 231, 203, 162, 128, 118 }, 74), Toast.LENGTH_LONG).show();
                notifyDownloadResult(true);

                // If what just finished downloading is itself an installable
                // Android package, go straight to the system installer
                // instead of leaving the user to dig it out of the Downloads
                // folder and tap it themselves. Gated strictly on the file
                // actually being a .apk -- every other kind of download this
                // wrapper handles (images, PDFs, whatever the wrapped site
                // links to) still behaves exactly as before, since "install"
                // wouldn't mean anything for those.
                if (localPath != null && title != null && title.toLowerCase().endsWith(UrlObfuscator.decode(new int[] { 117, 27, 233, 211 }, 91))) {
                    File apkFile = new File(localPath);
                    if (apkFile.exists()) {
                        try {
                            Uri contentUri = FileProvider.getUriForFile(
                                this, getPackageName() + UrlObfuscator.decode(new int[] { 66, 237, 195, 165, 141, 119, 84, 42, 18, 234, 198, 164, 146 }, 108), apkFile);
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
                Toast.makeText(this, UrlObfuscator.decode(new int[] { 57, 243, 204, 180, 149, 119, 86, 50, 85, 242, 210, 187, 157, 117, 75, 116, 77 }, 125) + title + UrlObfuscator.decode(new int[] { 174, 133, 169, 153, 120, 70, 58, 71 }, 142) + reason + ")", Toast.LENGTH_LONG).show();
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
                    UrlObfuscator.decode(new int[] { 235, 204, 164, 135, 114, 92, 113, 15, 254, 216, 177, 155, 100, 28, 14, 47, 224, 192, 131, 141, 127, 67, 63, 13, 195, 201, 178, 138, 111, 77, 32, 4, 60, 241, 208, 172, 151, 127, 77, 61, 94, 237, 194, 189, 157, 118, 94, 39, 65, 209, 242, 163, 133, 68, 72, 60, 14, 240, 192, 128, 140, 117, 79, 44, 48, 31, 249, 255, 180, 151, 105, 84, 50, 2, 240, 156 }, 159) + success + UrlObfuscator.decode(new int[] { 153, 244, 147, 112, 79, 42, 30, 234, 192, 239, 131, 44, 95, 62 }, 176),
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
                UrlObfuscator.decode(new int[] { 181, 146, 134, 101, 84, 58, 83, 237, 208, 182, 147, 121, 66, 122, 44, 205, 222, 190, 161, 111, 89, 37, 29, 239, 237, 167, 144, 104, 73, 43, 2, 230, 241, 178, 176, 153, 111, 89, 40, 9, 176, 195, 160, 159, 123, 80, 60, 5, 191, 239, 144, 129, 99, 98, 42, 30, 224, 222, 162, 162, 106, 83, 45, 14, 238, 193, 219, 142, 143, 115, 92, 40, 28, 235, 196, 254 }, 193) + done + "," + total + "," + status + UrlObfuscator.decode(new int[] { 251, 202, 109, 82, 45, 12, 248, 200, 162, 193, 109, 14, 61, 24 }, 210),
                null);
        } catch (Exception ignored) {
        }
    }

    private void notifyDownloadCancelled() {
        try {
            webView.evaluateJavascript(
                UrlObfuscator.decode(new int[] { 151, 112, 88, 59, 54, 24, 181, 203, 178, 148, 125, 87, 32, 88, 202, 235, 188, 156, 95, 81, 59, 7, 251, 201, 143, 133, 126, 70, 43, 9, 228, 192, 128, 141, 108, 80, 83, 59, 9, 249, 146, 161, 142, 113, 89, 50, 26, 227, 157, 141, 174, 127, 65, 0, 12, 248, 194, 188, 140, 76, 72, 49, 11, 232, 204, 163, 133, 67, 112, 83, 45, 16, 254, 206, 188, 208, 113, 87, 57, 7, 246, 158, 246, 147, 110, 64, 46, 9, 231, 198, 172, 140, 32, 15, 126, 25, 254, 193, 160, 148, 156, 118, 21, 57, 82, 225, 196 }, 227),
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
            Toast.makeText(this, UrlObfuscator.decode(new int[] { 181, 127, 94, 62, 7, 175, 199, 163, 159, 127, 75, 37, 4, 244, 134, 163, 150, 108, 79, 97, 20, 23, 247, 206, 252, 154, 106, 73, 116, 87, 226, 221, 177, 157, 50, 88, 36, 72, 226, 193, 236, 136, 101, 71, 60, 14, 232, 208, 161, 195, 99, 84, 52, 48, 19, 252, 200, 178, 153, 120, 84, 59, 15 }, 244), Toast.LENGTH_LONG).show();
            try {
                Intent settingsIntent = new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                    Uri.parse(UrlObfuscator.decode(new int[] { 117, 69, 32, 9, 224, 199, 218, 228 }, 261) + getPackageName()));
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
        installIntent.setDataAndType(contentUri, UrlObfuscator.decode(new int[] { 119, 69, 36, 31, 251, 210, 177, 155, 103, 66, 34, 68, 252, 199, 172, 201, 103, 75, 32, 17, 237, 200, 164, 241, 142, 124, 95, 48, 27, 254, 221, 250, 151, 103, 87, 59, 27, 231, 213 }, 278));
        installIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_GRANT_READ_URI_PERMISSION);
        try {
            startActivity(installIntent);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, UrlObfuscator.decode(new int[] { 105, 41, 69, 237, 205, 177, 149, 97, 115, 82, 56, 14, 187, 220, 182, 141, 121, 82, 117, 89, 190, 146, 190, 128, 106, 64, 109, 24, 227, 207, 233, 142, 110, 74, 32, 68, 229, 208, 174, 141, 223, 103, 82, 41, 9, 186, 253, 183, 128, 120, 89, 59, 18, 246, 194, 240, 137, 97, 65, 40, 14, 248, 137, 161, 137, 117, 81, 33, 2, 230 }, 295), Toast.LENGTH_LONG).show();
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
                Toast.makeText(this, UrlObfuscator.decode(new int[] { 110, 40, 20, 232, 216, 191, 146, 54, 69, 49, 1, 255, 216, 163, 156, 103, 66, 34, 75, 227, 218, 232, 137, 99, 64, 32, 6, 230, 129, 180, 176, 222, 110, 93, 45, 31, 185, 204, 191, 147, 53, 80, 60, 5, 255, 220, 160, 143, 105 }, 61), Toast.LENGTH_LONG).show();
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
                Toast.makeText(this, UrlObfuscator.decode(new int[] { 13, 12, 225, 206, 184, 136, 39, 74, 47, 6, 246, 204, 178, 137, 111, 113, 91, 125, 12, 254, 200, 180, 145, 100, 69, 60, 27, 253, 146, 184, 131, 47, 64, 40, 9, 239, 207, 173, 200, 97, 73, 55, 68, 247, 202, 168, 147 }, 78), Toast.LENGTH_LONG).show();
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
                Toast.makeText(this, UrlObfuscator.decode(new int[] { 19, 17, 254, 221, 175, 147, 118, 86, 119, 6, 240, 198, 190, 155, 98, 67, 38, 1, 227, 140, 162, 153, 41, 70, 34, 3, 225, 193, 167, 194, 103, 79, 77, 126, 9, 244, 210, 169 }, 95), Toast.LENGTH_LONG).show();
            }
        } else if (requestCode == NOTIFICATION_PERMISSION_REQUEST_CODE) {
            if (!(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                Toast.makeText(this, UrlObfuscator.decode(new int[] { 62, 224, 218, 164, 138, 98, 73, 40, 28, 238, 201, 171, 151, 35, 67, 51, 5, 95, 241, 219, 186, 219, 124, 86, 42, 87, 226, 221, 189, 128, 50, 80, 32, 31, 174, 128, 225, 203, 126, 92, 58, 9, 166, 209, 172, 134, 111, 1, 47, 49, 94, 244, 210, 251, 169, 124, 76, 35, 31, 251, 211, 160, 210, 112, 94, 54, 78, 249, 197, 166, 143 }, 112), Toast.LENGTH_LONG).show();
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
            UrlObfuscator.decode(new int[] { 229, 197, 217, 191, 136, 112, 79 }, 129), UrlObfuscator.decode(new int[] { 213, 212, 190, 138, 124, 76, 32 }, 146), NotificationManager.IMPORTANCE_DEFAULT);
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
                Toast.makeText(this, UrlObfuscator.decode(new int[] { 234, 172, 146, 116, 126, 82, 49, 92, 235, 223, 171, 149, 126, 69, 38, 29, 252, 220, 241, 135, 110, 93, 35, 75, 255, 138, 174, 154, 102, 72, 49, 1, 231, 130, 236, 205, 223, 113, 77, 57, 21, 186, 205, 176, 146, 54, 83, 61, 31, 247, 145, 182, 157, 97, 64, 108, 18, 229, 220, 186, 199, 66, 74, 51, 13, 238, 206, 161, 187, 141, 61, 90, 52, 22, 253, 221, 165, 214, 97, 91, 115, 27, 255, 195, 187, 143, 97, 64, 107, 7, 232, 198, 178, 135, 105, 72, 58 }, 163), Toast.LENGTH_LONG).show();
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
