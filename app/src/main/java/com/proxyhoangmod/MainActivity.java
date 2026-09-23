package com.proxyhoangmod;

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
// <input type=UrlObfuscator.decode(new int[] { 73, 39, 1, 233 }, 47)> clicks out of the box -- without this override,
// tapping a file-upload control silently does nothing, which is the most
// common UrlObfuscator.decode(new int[] { 52, 55, 27, 189, 222, 174, 142, 109, 87, 57, 86, 241, 219, 182, 129, 127, 23, 59, 78, 250, 195, 185, 129 }, 64) complaint for wrapped web apps that
// let the user pick a file.
//
// And a DownloadListener: a plain WebView also does NOT know what to do
// with a link to a downloadable file (an APK, a zip, etc) -- without this,
// tapping a UrlObfuscator.decode(new int[] { 21, 31, 248, 192, 161, 131, 106, 78 }, 81) link just fails to navigate anywhere and the app
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
    private static final String EMBED_HOST = UrlObfuscator.decode(new int[] { 10, 245, 212, 207, 173, 199, 51, 20, 63, 20, 250, 210, 178, 145, 113, 87, 124, 29, 255, 204, 175, 129, 35 }, 98);
    private ValueCallback<Uri[]> filePathCallback;
    // See the WebViewCompat.addDocumentStartJavaScript call in onCreate and
    // the onPageStarted fallback below for why this exists in two places.
    private boolean documentStartScriptSupported = false;
    // Best-effort polyfill for the File System Access API
    // (window.showOpenFilePicker), shared by both injection paths below.
    // Plain android.webkit.WebView has never implemented this API (it's
    // Chrome-desktop/modern-mobile-Chrome only -- see caniuse/MDN), so any
    // site written against it -- rather than the older
    // <input type=UrlObfuscator.decode(new int[] { 21, 251, 221, 181 }, 115)> + .click() pattern -- just throws
    // UrlObfuscator.decode(new int[] { 247, 203, 173, 150, 79, 111, 91, 51, 58, 242, 214, 188, 168, 126, 85, 62, 17, 225, 146, 184, 131, 47, 64, 34, 24, 171, 203, 233, 142, 114, 72, 38, 16, 234, 205, 175 }, 132) the moment its own upload
    // button is tapped, which looks to the user exactly like the button
    // silently doing nothing. This reroutes that call through a real
    // hidden <input type=file> instead, which
    // WebChromeClient.onShowFileChooser below already knows how to handle.
    private static final String SHOW_OPEN_FILE_PICKER_POLYFILL =
        UrlObfuscator.decode(new int[] { 189, 210, 166, 156, 114, 68, 38, 1, 227, 132, 226, 145 }, 149) +
        UrlObfuscator.decode(new int[] { 207, 163, 204, 116, 75, 47, 4, 16, 233, 147, 175, 147, 117, 78, 23, 7, 243, 219, 146, 154, 126, 84, 0, 6, 237, 198, 169, 153, 35, 91, 45, 19, 243, 215, 170, 216 }, 166) +
        UrlObfuscator.decode(new int[] { 192, 191, 155, 112, 92, 37, 95, 227, 199, 161, 154, 67, 91, 47, 7, 206, 206, 170, 128, 84, 74, 33, 10, 229, 237, 131, 187, 137, 117, 89, 45, 17, 248, 216, 253, 155, 99, 70, 34, 89, 244 }, 183) +
        UrlObfuscator.decode(new int[] { 167, 151, 114, 86, 121, 12, 242, 213, 179, 163, 130, 102, 65, 96 }, 200) +
        UrlObfuscator.decode(new int[] { 171, 157, 99, 67, 39, 26, 179, 220, 180, 135, 47, 126, 63, 3, 230, 195, 186, 141, 47, 64, 48, 10, 224, 214, 168, 143, 145, 54, 79, 57, 8, 245, 213, 174, 146, 58, 71, 49, 25, 247, 210, 164, 198, 117 }, 217) +
        UrlObfuscator.decode(new int[] { 156, 104, 90, 103, 15, 235, 212, 182, 150, 60, 68, 80, 61, 8, 241, 222, 180, 141, 54, 84, 36, 16, 245, 199, 183, 180, 124, 74, 35, 8, 226, 223, 226, 206, 97, 73, 54, 16, 240, 132, 235, 218 }, 234) +
        UrlObfuscator.decode(new int[] { 146, 116, 73, 45, 3, 184, 193, 173, 131, 119, 12, 119, 9, 231, 193, 169, 204, 49 }, 251) +
        UrlObfuscator.decode(new int[] { 101, 77, 98, 6, 248, 211, 181, 203, 105, 86, 46, 21, 233, 239, 210, 184, 213, 114, 84, 41, 13, 227, 152, 184, 129, 127, 70, 56, 0, 227, 203, 240, 152, 121, 95, 44, 83 }, 268) +
        UrlObfuscator.decode(new int[] { 105, 78, 34, 1, 239, 217, 165, 214, 112, 76, 39, 1, 172, 235, 146, 213, 37, 67, 59, 30, 250, 134, 179, 159, 117, 65, 48, 30, 253, 251, 226, 247, 211, 122, 84, 40, 60, 249, 212, 190, 221, 114, 70, 60, 18, 228, 198, 161, 131, 36, 95, 99, 18 }, 285) +
        UrlObfuscator.decode(new int[] { 69, 51, 3, 176, 206, 243, 153, 42, 13, 62, 71, 233, 196, 165, 128, 116, 87, 121, 8, 230, 183, 159, 188, 213, 105, 95, 45, 13, 229, 216, 238 }, 51) +
        UrlObfuscator.decode(new int[] { 11, 1, 232, 196, 163, 171, 208, 118, 89, 34, 9, 177, 217, 254, 216, 115, 91, 33, 55, 240, 211, 167, 198, 107, 89, 37, 9, 253, 193, 168, 136, 45, 73, 42, 15, 228, 137, 196, 187, 133, 104, 72, 116, 9, 237, 196, 190, 221, 121, 90, 63, 20, 185, 148 }, 68) +
        UrlObfuscator.decode(new int[] { 125, 21, 200, 223, 184, 157, 106, 115, 49, 16, 208, 247, 224, 198, 97, 73, 55, 33, 226, 193, 169, 200, 153, 107, 83, 63, 15, 243, 214, 182, 223, 115, 77, 32, 90, 233, 212, 168, 155, 125, 3, 60, 30, 249, 193, 224, 130, 126, 81, 109, 88, 255, 136, 251, 162, 215, 38, 65, 114, 65 }, 85) +
        UrlObfuscator.decode(new int[] { 15, 227, 140, 166, 154, 117, 83, 17, 50, 24, 242, 220, 174, 145, 49, 94, 56, 5, 225, 199, 252, 144, 115, 76, 43, 29, 248, 150, 175, 145, 124, 84, 104, 15, 235, 202, 172, 201, 39, 51, 25, 116, 71, 230, 217, 184, 140, 116, 94, 125, 17, 186, 201, 172 }, 102) +
        UrlObfuscator.decode(new int[] { 30, 248, 197, 161, 135, 60, 66, 36, 22, 226, 200, 226, 143, 99, 90, 56, 11, 231, 220, 249, 196, 108, 78, 46, 58, 89, 166 }, 119) +
        UrlObfuscator.decode(new int[] { 236, 200, 165, 144, 105, 70, 44, 21, 174, 253, 209, 185, 133, 53, 91, 41, 8, 242, 216, 177, 183, 123, 91, 61, 20, 167, 199, 163, 156, 126, 94, 96, 83 }, 136) +
        UrlObfuscator.decode(new int[] { 240, 214, 167, 131, 97, 26, 50, 22, 245, 245, 185, 139, 99, 88, 7, 3, 250, 220, 162, 136, 96, 86, 107, 69, 226, 200, 222, 176, 154, 121, 28, 118, 31, 237, 217, 181, 129, 125, 92, 60, 89, 185, 212 }, 153) +
        UrlObfuscator.decode(new int[] { 222, 187, 145, 124, 66, 42, 7, 246, 207, 164, 142, 139, 48, 95, 51, 31, 227, 151, 170, 146, 123, 90, 34, 22, 209, 217, 185, 131, 106, 5, 37, 5, 250, 220, 188, 206, 61, 88, 39, 2, 246, 194, 168, 247, 155, 52, 71, 38 }, 170) +
        UrlObfuscator.decode(new int[] { 210, 188, 209, 57, 94, 56, 5, 225, 199, 252, 151, 121, 67, 43, 30, 240, 215, 235, 128, 102, 87, 51, 17, 170, 197, 171, 141, 101, 108, 16, 49, 25, 245, 221, 173, 144, 62, 77 }, 187) +
        UrlObfuscator.decode(new int[] { 186, 138, 120, 9, 45, 21, 244, 158, 176, 145, 123, 90, 37, 45, 12, 160, 210, 190, 141, 57, 124, 24, 59, 208, 204, 176, 151, 97, 68, 38, 1, 227, 132, 236, 190, 97, 77, 103, 19, 246, 193, 177, 194, 96, 66, 80, 44, 9, 249, 223, 250, 152, 56, 69, 51, 4, 225, 214, 161, 133, 62, 8, 98, 74, 205, 201, 165, 155, 124, 98, 52, 23, 235, 209, 229, 200, 59, 98 }, 204) +
        UrlObfuscator.decode(new int[] { 190, 157, 111, 89, 49, 80, 242, 159, 174, 145, 97, 64, 108, 30, 234, 217, 237, 169, 121, 88, 38, 26, 175, 129, 132, 134, 108, 80, 53, 37, 13, 236, 210, 174, 220, 51, 2, 37 }, 221) +
        UrlObfuscator.decode(new int[] { 156, 104, 70, 46, 9, 253, 128, 162, 148, 119, 13, 120, 16, 228, 212, 202, 172, 147, 39, 70 }, 238) +
        UrlObfuscator.decode(new int[] { 137, 127, 79, 124, 19, 251, 215, 188, 155, 115, 70, 105, 50, 224, 195, 177, 150, 32, 93, 62, 4, 254, 198, 188, 158, 118, 64, 106, 14, 227, 209, 238, 188, 159, 113, 80, 115, 19, 247, 200, 162, 130, 59, 82, 58, 30, 244, 195, 227, 136, 120, 66, 40, 30, 224, 199, 169, 206, 99, 77, 47, 7, 168, 219 }, 255) +
        UrlObfuscator.decode(new int[] { 98, 74, 58, 24, 254, 197, 177, 130, 97, 73, 34, 95, 163, 197, 171, 141, 101, 56, 18, 51, 29, 246, 223, 227, 158, 126, 90, 48, 90, 253, 211, 188, 149, 35, 73, 40, 24, 205, 195, 165, 141, 61, 64, 48, 10, 224, 214, 168, 143, 145, 54, 20, 39, 9, 255, 205, 173, 133, 120, 21, 4, 1, 253, 220, 185, 156, 107, 3, 62, 14, 249, 198, 164, 145, 99, 13, 34, 10, 238, 196, 233, 228, 131, 96, 7, 38, 83, 162 }, 272) +
        UrlObfuscator.decode(new int[] { 83, 37, 44, 17, 241, 202, 190, 210, 118, 72, 35, 5, 187, 217, 166, 158, 101, 89, 63, 2, 232, 147, 163, 139, 103, 76, 43, 3, 246, 158, 152, 138, 96, 78, 91, 50, 24, 239, 224, 234, 164, 69, 30, 109, 8, 189, 136 }, 289) +
        UrlObfuscator.decode(new int[] { 94, 56, 5, 225, 199, 252, 146, 124, 70, 45, 6, 164, 130, 241, 148, 33, 28, 59, 94 }, 55) +
        UrlObfuscator.decode(new int[] { 53, 79, 175, 140 }, 72);
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
    // Whether the CURRENT drag started within the top UrlObfuscator.decode(new int[] { 41, 13, 251, 218, 245, 142, 124, 92, 52 }, 89) (see
    // canChildScrollUp() override below). scrollTop alone can't tell a
    // page genuinely at its top apart from a dialog/sheet that just
    // opened and also happens to read scrollTop 0 -- a drag anywhere
    // inside that dialog would otherwise get read as UrlObfuscator.decode(new int[] { 11, 253, 136, 179, 142, 96, 4, 55, 13, 241, 140, 159, 173, 146, 22, 27, 122, 89, 184, 152, 249, 213, 96, 91, 59, 2, 176, 194, 187, 158, 120, 11, 40, 12, 168, 198, 230, 151, 97, 69, 48, 4, 243, 247, 158, 173, 137, 119, 86 }, 106) even though it's nowhere near the
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
    // connection is back, instead of making the user tap UrlObfuscator.decode(new int[] { 9, 255, 205, 170, 142 }, 123) themselves.
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
    // earlier tap), instantly marking the page's button UrlObfuscator.decode(new int[] { 200, 196, 189, 135, 100, 72, 39, 1, 225, 199 }, 140) while
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
            paint.setColor(Color.parseColor(UrlObfuscator.decode(new int[] { 190, 250, 233, 188, 43, 125, 18 }, 157)));
            canvas.drawCircle(cx, cy, stroke * 1.05f, paint);

            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(stroke);
            paint.setColor(Color.parseColor(UrlObfuscator.decode(new int[] { 141, 248, 137, 110, 75, 45, 92 }, 174)));
            float[] radii = {w * 0.24f, w * 0.38f};
            int[] alphas = {235, 130};
            for (int i = 0; i < radii.length; i++) {
                paint.setAlpha(alphas[i]);
                RectF arc = new RectF(cx - radii[i], cy - radii[i], cx + radii[i], cy + radii[i]);
                canvas.drawArc(arc, 208, 124, false, paint);
            }

            paint.setAlpha(255);
            paint.setColor(Color.parseColor(UrlObfuscator.decode(new int[] { 156, 152, 187, 42, 121, 111, 78 }, 191)));
            paint.setStrokeWidth(stroke * 1.1f);
            float pad = w * 0.14f;
            canvas.drawLine(pad, pad, w - pad, h - pad, paint);
        }
    }

    // Compact three-dot UrlObfuscator.decode(new int[] { 178, 128, 123, 67, 47, 2, 228, 206 }, 208) loading indicator for the nav-loading
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
        // (minimum display time / UrlObfuscator.decode(new int[] { 141, 101, 107, 30, 41, 20, 254, 154, 176, 150, 99, 68, 58, 84, 229, 219, 181, 149, 96, 14, 43, 5, 229, 195, 186, 128 }, 225)) hide()
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
    // connection still reads as UrlObfuscator.decode(new int[] { 133, 126, 66, 36, 7, 227, 203 }, 242) instead of stuck. Text size
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
            String name = (appName == null || appName.trim().isEmpty()) ? UrlObfuscator.decode(new int[] { 66, 82, 49 }, 259) : appName.trim().toUpperCase();

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
            // minimal, UrlObfuscator.decode(new int[] { 101, 70, 59, 18, 251, 143, 162, 130, 109, 79, 35, 7, 239 }, 276) look rather than a heavy one).
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
        // UrlObfuscator.decode(new int[] { 76, 53, 11, 243, 222, 184, 146 }, 59) under the wordmark the whole time it's showing.
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
                // one clean overshoot-and-settle reads as UrlObfuscator.decode(new int[] { 63, 6, 229, 198, 188, 143 }, 76) rather
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
        // WebView is still loading the name keeps visibly UrlObfuscator.decode(new int[] { 60, 16, 242, 204, 188 }, 93) instead
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
        // resource actually got shipped, so those start out already UrlObfuscator.decode(new int[] { 10, 226, 194, 174 }, 110).
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
                videoResId = context.getResources().getIdentifier(UrlObfuscator.decode(new int[] { 28, 235, 206, 168, 148, 119, 102, 43, 7, 250, 212, 167, 155 }, 127), UrlObfuscator.decode(new int[] { 226, 206, 185 }, 144), pkg);
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
                int resId = context.getResources().getIdentifier(UrlObfuscator.decode(new int[] { 194, 181, 172, 138, 114, 81, 4, 9, 233, 212, 182, 133, 125 }, 161), UrlObfuscator.decode(new int[] { 214, 163, 145, 120, 79, 47, 0, 238 }, 178), pkg);
                if (resId != 0) {
                    iv.setImageURI(Uri.parse(UrlObfuscator.decode(new int[] { 162, 140, 101, 82, 80, 55, 25, 178, 201, 191, 138, 119, 66, 36, 22, 241, 137, 253, 222 }, 195) + pkg + "/" + resId));
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
                mp.setDataSource(context, Uri.parse(UrlObfuscator.decode(new int[] { 181, 157, 118, 67, 63, 6, 234, 131, 190, 142, 121, 70, 61, 21, 229, 192, 254, 204, 45 }, 212) + pkg + "/" + videoResId));
                mp.setSurface(playerSurface);
                // Single play, not looped -- see onCompletion below for
                // how the UrlObfuscator.decode(new int[] { 147, 109, 71, 39, 14, 160, 250, 208, 185, 143, 59, 88, 60, 30, 248, 196, 176, 212, 103, 90, 52, 80, 255, 207, 170, 137, 43, 67, 58, 72, 245, 195, 164, 128, 122 }, 229) case is
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
    // set to UrlObfuscator.decode(new int[] { 153, 115, 82 }, 246) or UrlObfuscator.decode(new int[] { 105, 73, 43, 1 }, 263) (no animation) -- rather than showing nothing
    // while the page loads (which used to mean the WebView appeared the
    // instant it was created, flashing black before its own background even
    // painted), this shows the app's own launcher icon centered on the
    // splash background, completely static, and holds it until the page is
    // ready -- then hide() below swaps it out immediately, no minimum
    // display time and no loading bar, so it goes straight to the app the
    // moment the page finishes loading. UrlObfuscator.decode(new int[] { 87, 81, 48 }, 280)/UrlObfuscator.decode(new int[] { 71, 39, 9, 227 }, 297) mean no animated
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
            int iconResId = context.getResources().getIdentifier(UrlObfuscator.decode(new int[] { 86, 61, 34, 240, 218, 175, 151, 123, 95, 51, 7 }, 63), UrlObfuscator.decode(new int[] { 61, 6, 254, 192, 173, 155 }, 80), context.getPackageName());
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
        root.setBackgroundColor(Color.parseColor(UrlObfuscator.decode(new int[] { 66, 176, 170, 142, 232, 204, 46 }, 97)));

        webView = new WebView(this);
        // A freshly created WebView has no rendered frame yet -- its
        // underlying Surface starts out blank/black, and that shows through
        // for a beat even with setBackgroundColor(WHITE) set, because that
        // color is drawn BY the WebView, but the Surface itself hasn't
        // produced a first frame to draw it onto yet. Making the WebView
        // visible immediately -- which this used to do whenever startup
        // loading was set to UrlObfuscator.decode(new int[] { 29, 247, 214 }, 114) -- is exactly what let that black flash
        // reach the screen, often right before a second flash of the
        // WebView's own white background as the real page started painting.
        // Two mismatched flashes back to back is the UrlObfuscator.decode(new int[] { 225, 206, 160, 131, 148, 62, 73, 52, 30, 244, 153, 175, 159, 127, 65, 49 }, 131)
        // effect.
        //
        // The fix: never show the WebView until it already has real content
        // to show. It now loads fully hidden behind this root FrameLayout's
        // solid near-black background (#050505 above) and only gets
        // revealed in revealWebView() (see onPageFinished below), which
        // runs unconditionally regardless of the splash setting. With
        // startup loading on, that reveal is the existing animated hand-off
        // from the splash overlay; with it set to UrlObfuscator.decode(new int[] { 251, 213, 180 }, 148), it's the same
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
        // apps, feeds, anything with an inner UrlObfuscator.decode(new int[] { 202, 178, 134, 112, 71, 44, 48, 9, 176, 197, 225, 218, 120, 77, 35, 25 }, 165) panel)
        // never scroll the document itself -- an inner <div> scrolls
        // instead, so the WebView's own scrollY sits at 0 forever. That
        // makes SwipeRefreshLayout think the page is always UrlObfuscator.decode(new int[] { 215, 161, 212, 103, 90, 52, 80, 251, 193, 189, 192 }, 182)
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
        // identical to UrlObfuscator.decode(new int[] { 160, 131, 107, 81, 42, 12, 228, 204, 198, 254, 156, 104, 27, 46, 17, 253, 151, 162, 154, 100, 19, 61, 23, 176, 219, 166, 136, 44, 91, 43, 14, 237 }, 199) -- so dragging
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
        swipeRefresh.setColorSchemeColors(Color.parseColor(UrlObfuscator.decode(new int[] { 251, 197, 82, 3, 23, 55, 212 }, 216)));
        swipeRefresh.addView(webView, new ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        root.addView(swipeRefresh, webParams);

        final SplashController loading = new StaticIconSplashView(this, Color.parseColor(UrlObfuscator.decode(new int[] { 202, 56, 23, 118, 85, 180, 147 }, 233)));
        FrameLayout.LayoutParams loadingParams = new FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT);
        root.addView((View) loading, loadingParams);
        // Always shown, animated styles and UrlObfuscator.decode(new int[] { 149, 127, 94 }, 250)/UrlObfuscator.decode(new int[] { 101, 69, 39, 13 }, 267) alike -- UrlObfuscator.decode(new int[] { 115, 93, 60 }, 284) and
        // UrlObfuscator.decode(new int[] { 92, 62, 30, 234 }, 50) just mean the static-icon splash above instead of an
        // animated wordmark, not no splash at all. Either way it holds the
        // screen until revealWebView (see onPageFinished below) swaps it
        // for the site -- and for the static-icon case that swap happens
        // the instant the page is ready, with no forced minimum hold and
        // no loading bar (see StaticIconSplashView.hide() -- no
        // minDisplayMs wait like LoadingSplashView.hide() has).
        loading.show();

        // Shown instead of the WebView's own built-in error page when the
        // main frame fails to load (see onReceivedError below) -- a bare
        // WebView renders Chromium's default UrlObfuscator.decode(new int[] { 20, 7, 227, 208, 222, 185, 152, 60, 85, 53, 13, 184, 214, 160, 148, 125, 95, 51, 19, 252, 202, 238, 194, 6, 11, 106, 73, 168, 135, 230, 197, 36, 12, 109, 65, 238, 250, 202, 231, 198, 94, 104, 11, 39, 217, 247, 152, 177, 76, 124, 30, 36, 208, 252, 136, 191, 68, 102, 31, 45, 195 }, 67) page, which looks like a broken
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
        final String offlineScrimColor = offlineIsNightMode ? UrlObfuscator.decode(new int[] { 119, 74, 171, 129, 224, 223, 62, 29, 124 }, 84) : UrlObfuscator.decode(new int[] { 70, 178, 149, 242, 209, 48, 47, 14, 109 }, 101);
        // Slightly translucent (not fully opaque) card/button fills, so a
        // hint of the dimmed scrim behind still shows through -- a cheap
        // stand-in for a true frosted-glass blur, which would need
        // RenderEffect (API 31+) and a snapshot of whatever's behind the
        // card to do for real.
        final String offlineCardBg = offlineIsNightMode ? UrlObfuscator.decode(new int[] { 85, 208, 140, 225, 177, 35, 117, 124, 94 }, 118) : UrlObfuscator.decode(new int[] { 164, 228, 253, 162, 49, 100, 115, 38, 77 }, 135);
        final String offlineTitleColor = offlineIsNightMode ? UrlObfuscator.decode(new int[] { 187, 241, 144, 179, 82, 117, 20 }, 152) : UrlObfuscator.decode(new int[] { 138, 248, 215, 54, 21, 116, 83 }, 169);
        final String offlineSubtitleColor = offlineIsNightMode ? UrlObfuscator.decode(new int[] { 153, 155, 192, 85, 14, 23, 48 }, 186) : UrlObfuscator.decode(new int[] { 232, 223, 75, 29, 5, 83, 192 }, 203);
        final String offlineButtonBg = offlineIsNightMode ? UrlObfuscator.decode(new int[] { 255, 195, 89, 13, 96, 67, 174, 129, 149 }, 220) : UrlObfuscator.decode(new int[] { 206, 52, 104, 14, 95, 204, 145, 130, 164 }, 237);
        final float offlineDensity = getResources().getDisplayMetrics().density;

        // Outer full-screen scrim so the card reads as a dialog sitting on
        // top of the app, matching how a native UrlObfuscator.decode(new int[] { 144, 114, 28, 56, 21, 247, 214, 178, 149, 97, 93, 60, 28 }, 254) alert
        // dims everything behind it.
        final FrameLayout errorView = new FrameLayout(this);
        errorView.setBackgroundColor(Color.parseColor(offlineScrimColor));
        errorView.setVisibility(View.GONE);

        // The actual rounded card. Entrance/exit animations below target
        // this (not the full-screen scrim) so only the card itself pops
        // in/out while the dim layer just fades. Corner radius and padding
        // are tuned to match a real iOS-style alert card's proportions
        // (roughly 14dp radius, not an exaggerated UrlObfuscator.decode(new int[] { 124, 95, 56, 5, 249, 201, 165, 141 }, 271)) rather than
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
        errorTitle.setText(UrlObfuscator.decode(new int[] { 110, 80, 126, 19, 249, 207, 173, 150, 106, 92 }, 288));
        errorTitle.setTextColor(Color.parseColor(offlineTitleColor));
        errorTitle.setTextSize(16);
        errorTitle.setTypeface(errorTitle.getTypeface(), Typeface.BOLD);
        errorTitle.setGravity(Gravity.START);
        errorCard.addView(errorTitle);

        final TextView errorSubtitle = new TextView(this);
        errorSubtitle.setText(UrlObfuscator.decode(new int[] { 102, 57, 17, 242, 193, 180, 208, 125, 75, 57, 30, 242, 138, 190, 128, 98, 72, 101, 7, 236, 204, 175, 133, 156, 106, 88, 56, 91, 238, 214, 248, 158, 120, 65, 49, 1, 252, 212, 164 }, 54));
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
        retryText.setText(UrlObfuscator.decode(new int[] { 21, 3, 241, 214, 186 }, 71));
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
        navOverlay.setBackgroundColor(Color.parseColor(UrlObfuscator.decode(new int[] { 123, 71, 166, 133, 228, 195, 34 }, 88)));
        navOverlay.setVisibility(View.GONE);
        final BouncingDotsView navDots = new BouncingDotsView(this, Color.parseColor(UrlObfuscator.decode(new int[] { 74, 206, 149, 128, 215, 65, 102 }, 105)));
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
        // default UrlObfuscator.decode(new int[] { 31, 225, 209, 163 }, 122) behavior once there's no more WebView history to
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
        // instead of an actual preview. android:hardwareAccelerated=UrlObfuscator.decode(new int[] { 255, 216, 188, 141 }, 139)
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
        // <meta name=UrlObfuscator.decode(new int[] { 234, 210, 191, 142, 104, 88, 36, 1 }, 156)> tag and lays it out at a fixed desktop
        // width (980px) instead, then scales the result to fit -- which is
        // exactly what produces oversized icons/buttons and title text
        // that overflows off the right edge instead of wrapping, since the
        // page's responsive CSS never actually saw a phone-width viewport.
        settings.setUseWideViewPort(true);
        settings.setLoadWithOverviewMode(true);
        settings.setTextZoom(100);
        // Google's own sign-in pages detect the UrlObfuscator.decode(new int[] { 150, 236, 156, 124, 0 }, 173) token (and the
        // UrlObfuscator.decode(new int[] { 232, 184, 142, 104, 83, 54, 22, 184, 238, 251, 172, 51 }, 190) segment) that stock WebView adds to its user-agent
        // and use it to silently block OAuth inside embedded WebViews -- the
        // page loads fine but the sign-in form's JS just no-ops, so tapping
        // UrlObfuscator.decode(new int[] { 129, 139, 117, 88 }, 207) appears to do nothing. Stripping those two markers from an
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
        String spoofedUA = defaultUA.replace(UrlObfuscator.decode(new int[] { 219, 223, 105, 75 }, 224), "").replaceAll("Version/[0-9.]+\s", "");
        settings.setUserAgentString(spoofedUA);
        // Needed for Google/Firebase-style UrlObfuscator.decode(new int[] { 130, 121, 72, 32, 77, 229, 197, 234, 158, 97, 83, 46, 69, 244, 204, 178, 148, 112 }, 241) flows: that JS
        // calls window.open() on the auth provider's URL, and Chrome/Firebase
        // then closes that popup itself once sign-in finishes. Without these
        // two, WebView either can't open the popup at all or opens it detached
        // from the parent page's session, so the auth handler gets a request
        // it can't reconcile and shows UrlObfuscator.decode(new int[] { 86, 73, 37, 127, 12, 248, 205, 174, 159, 106, 76, 50, 18, 181, 213, 176, 134, 120, 95, 33, 78, 228, 223, 235, 131, 103, 94, 38, 10, 236, 192 }, 258).
        settings.setSupportMultipleWindows(true);
        settings.setJavaScriptCanOpenWindowsAutomatically(true);
        // A raw WebView lets the user pinch-zoom and shows on-screen zoom
        // controls like a browser tab -- fine for browsing a random site,
        // but it's the biggest tell that UrlObfuscator.decode(new int[] { 103, 90, 56, 3, 175, 199, 190, 204, 97, 95, 58, 28, 167, 199, 229, 147, 102, 64, 97, 16, 30, 249, 216 }, 275) for an
        // app that's supposed to read as native. The page's own
        // <meta name=UrlObfuscator.decode(new int[] { 82, 42, 7, 246, 208, 208, 172, 137 }, 292)> (handled above) is what actually controls
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
        // and the UrlObfuscator.decode(new int[] { 74, 56, 11, 227, 211, 245, 146, 97, 93, 60, 80, 236, 194, 164, 156, 105, 69, 40, 26, 227 }, 58) bubble over a text input. Blanket-
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
            // WebView's own UrlObfuscator.decode(new int[] { 42, 74, 253, 205, 191, 146, 37, 65, 39, 11, 245, 207, 205, 254, 148, 111, 27, 59, 26, 236, 222, 160, 144 }, 75) flag counts too.
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
        // <meta name=UrlObfuscator.decode(new int[] { 40, 19, 255, 212, 189, 218, 117, 90, 56, 28, 224 }, 92)>, if it has one, and recolors the
        // status/nav bars to match instead of leaving them a fixed color
        // that may clash with the page). Safe to expose here specifically
        // because this WebView only ever loads this app's own bundled
        // local assets, never arbitrary third-party pages.
        webView.addJavascriptInterface(new AndroidBridge(), UrlObfuscator.decode(new int[] { 44, 226, 207, 184, 134, 97, 67, 4, 23, 237, 199, 165, 132 }, 109));

        // Auto-retry the moment the OS reports a usable connection again,
        // so the offline screen clears itself on most devices without
        // waiting for a tap. Falls back to manual UrlObfuscator.decode(new int[] { 42, 252, 204, 251, 142, 118, 24, 37, 19, 225, 198, 170 }, 126) if this
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
                // Picks up the page's <meta name=UrlObfuscator.decode(new int[] { 251, 198, 168, 129, 110, 7, 42, 7, 235, 201, 183 }, 143)> (if it has
                // one) to recolor the system bars, and wires up a light
                // haptic tap on buttons/links -- both no-ops wrapped in
                // try/catch so a page that doesn't have a theme-color tag,
                // or that runs somewhere AndroidBridge isn't defined (the
                // popup WebView below doesn't get it), just silently skips
                // rather than throwing a JS error.
                view.evaluateJavascript(
                    UrlObfuscator.decode(new int[] { 136, 217, 171, 147, 127, 79, 51, 22, 246, 159, 255, 142 }, 160) +
                    // Forces correct mobile scaling regardless of what the
                    // wrapped site itself declares. setUseWideViewPort/
                    // setLoadWithOverviewMode (see WebSettings above) handle
                    // the common case of a page with NO viewport tag at all,
                    // but a page whose own tag is present yet wrong (a
                    // leftover desktop width, a bad initial-scale, or CSS
                    // that just ignores the viewport and sets a fixed pixel
                    // width somewhere) can still render oversized and
                    // overflow off both edges -- exactly what makes a
                    // wrapped site read as UrlObfuscator.decode(new int[] { 208, 240, 152, 107, 79, 63, 2, 254, 204, 228, 199, 104, 74, 48, 67, 227, 129, 174, 190, 138, 116, 74, 62, 90, 248, 200, 167 }, 177).
                    // Rewriting the tag to a known-good mobile value, and
                    // clamping the root elements so nothing can force page
                    // width past the viewport, covers those cases too.
                    UrlObfuscator.decode(new int[] { 182, 147, 121, 100 }, 194) +
                    "var vp=document.querySelector('meta[name=\"viewport\"]');" +
                    UrlObfuscator.decode(new int[] { 186, 148, 57, 17, 57, 30, 164, 215, 189, 154, 52, 76, 40, 5, 240, 201, 166, 140, 117, 14, 92, 44, 24, 253, 207, 191, 188, 116, 82, 59, 16, 250, 199, 250, 214, 125, 74, 58, 12, 171, 130, 241, 159, 120, 9, 53, 0, 240, 226, 182, 149, 114, 118, 92, 40, 8, 254, 146, 254, 150, 118, 91, 48, 83, 191, 149, 167, 153, 106, 89, 61, 3, 249, 222, 238, 193, 60, 66, 42, 7, 246, 207, 164, 142, 139, 48, 85, 57, 26, 254, 151, 185, 135, 102, 80, 58, 23, 209, 217, 185, 131, 106, 5, 58, 27, 163, 146, 181 }, 211) +
                    UrlObfuscator.decode(new int[] { 146, 115, 12, 50, 5, 11, 223, 201, 168, 137, 115, 91, 45, 3, 243, 157, 243, 144, 125, 95, 36, 10, 224, 217, 235, 199, 45, 94, 33, 3, 242, 205, 249, 135, 103, 87, 41, 60, 27, 176, 203, 178, 158, 109, 80, 123, 86, 252, 218, 186, 134, 120, 81, 35, 67, 254, 207, 170, 134, 108, 21, 118, 72, 181, 136, 227, 143, 96, 88, 86, 51, 8, 241, 150, 169, 154, 121, 91, 51, 72, 165, 157, 226, 221, 48, 90, 61, 8, 254, 134, 185, 138, 105, 75, 39, 7, 232, 198, 255, 143, 111, 56, 23, 102 }, 228) +
                    UrlObfuscator.decode(new int[] { 156, 114, 27, 115, 21, 255, 204, 187, 128, 105, 69, 62, 71, 239, 194, 178, 160, 104, 70, 47, 4, 238, 235, 252, 164, 181, 127, 18, 126, 39, 200, 215, 187, 144, 97, 93, 56, 20, 217, 199, 168, 155, 123, 69, 59, 28, 193, 207, 189, 195, 42, 11, 58 }, 245) +
                    UrlObfuscator.decode(new int[] { 112, 68, 54, 67, 241, 213, 253, 187, 145, 126, 73, 54, 31, 247, 204, 249, 149, 103, 81, 50, 6, 244, 245, 163, 139, 96, 73, 37, 30, 161, 143, 180, 146, 124, 72, 38, 69, 168, 155, 204, 170, 211, 117, 95, 103, 94, 199, 232, 183, 155, 112, 65, 61, 24, 244, 249, 167, 136, 123, 91, 37, 27, 252, 225, 175, 157, 35, 24 }, 262) +
                    UrlObfuscator.decode(new int[] { 100, 66, 123, 0, 246, 202, 165, 179, 96, 64, 57, 9, 229, 222, 244, 207, 111, 82, 40, 8, 175, 192, 174, 132, 134, 101, 80, 61, 3, 183, 206, 177, 147, 98, 93, 110, 66, 162, 129, 166, 152, 47, 68, 33, 27, 229, 219, 188, 134, 104, 81, 127, 12, 244, 196, 178, 185, 146, 114, 75, 118, 2, 163, 208, 190, 146, 113, 81, 61, 83, 248, 221, 191, 129, 127, 88, 42, 4, 253, 147, 234, 145, 96, 70, 40, 11, 245, 141, 203, 187, 133, 104, 22, 41, 16, 226, 210, 251, 148, 112, 89, 39, 2, 228, 149, 255, 221, 60, 14, 107, 0, 229, 215, 169, 151, 112, 66, 44, 21, 187, 235, 219, 165, 136, 54, 73, 48, 2, 242, 155, 180, 144, 121, 71, 34, 4, 181, 159, 253, 220, 46, 11, 32, 5, 247, 201, 183, 144, 98, 76, 53, 91, 2, 185, 134 }, 279) +
                    UrlObfuscator.decode(new int[] { 76, 40, 5, 240, 201, 166, 140, 117, 14, 87, 59, 28, 248, 149, 187, 137, 104, 82, 56, 17, 215, 219, 187, 157, 116, 7, 61, 25, 165, 144, 183 }, 296) +
                    UrlObfuscator.decode(new int[] { 67, 62, 29, 239, 217, 177, 208, 114, 31, 46, 9 }, 62) +
                    "try{var m=document.querySelector('meta[name=\"theme-color\"]');" +
                    UrlObfuscator.decode(new int[] { 38, 8, 165, 193, 237, 204, 126, 65, 41, 2, 234, 211, 237, 163, 111, 68, 77, 49, 20, 248, 249, 168, 144, 124, 80, 51, 83, 178, 242, 188, 149, 98, 64, 39, 9, 206, 217, 163, 141, 111, 66, 104, 4, 244, 211, 174, 152, 84, 119, 91, 48, 25, 216, 213, 181, 151, 101, 31, 46, 53, 253, 214, 163, 159, 102, 74, 15, 30, 226, 206, 174, 141, 41, 71, 53, 20, 239, 219, 149, 136, 154, 115, 88, 31, 20, 246, 214, 170, 223, 123, 27, 51, 22, 230, 240, 164, 155, 124, 68, 46, 30, 254, 204, 224, 192, 101, 74, 42, 23, 231, 207, 180, 248, 215, 97, 64, 124, 93, 176, 131, 170 }, 79) +
                    UrlObfuscator.decode(new int[] { 29, 28, 255, 201, 191, 147, 50, 92, 113, 12, 235 }, 96) +
                    UrlObfuscator.decode(new int[] { 5, 226, 214, 181, 132, 106, 3, 61, 0, 230, 195, 169, 146, 42, 98, 44, 5, 242, 240, 215, 185, 190, 105, 83, 61, 31, 242, 144, 243, 181, 125, 86, 35, 31, 230, 202, 143, 158, 98, 78, 46, 13, 169, 199, 181, 148, 111, 91, 17, 1, 24, 251, 255, 189, 152, 113, 94, 42, 24, 227, 219, 176, 218, 105 }, 113) +
                    UrlObfuscator.decode(new int[] { 244, 192, 178, 255, 142, 116, 95, 48, 56, 254, 133, 177, 131, 123, 87, 39, 27, 254, 222, 231, 199, 118 }, 130) +
                    UrlObfuscator.decode(new int[] { 229, 211, 163, 208, 108, 70, 44, 5, 229, 151, 146, 181, 60, 80, 36, 22, 163, 199, 252, 132, 144, 125, 72, 49, 30, 244, 205, 246, 146, 122, 80, 57, 22, 252, 197, 150, 157, 97, 64, 28, 4, 227, 199, 188, 207, 75, 68, 48, 11, 172, 199, 172, 176, 145, 111, 20, 44, 19, 247, 220, 184, 129, 59, 93, 61, 28, 244, 194, 152, 135, 105, 88, 35, 69, 187, 129, 235, 145, 108, 74, 39, 13, 246, 142, 214, 176, 147, 121, 73, 18, 28, 241, 208, 190, 129, 57, 1, 123, 74 }, 147) +
                    UrlObfuscator.decode(new int[] { 211, 171, 139, 109, 69, 23, 59, 84, 231, 216, 178, 152, 113, 89, 120, 5, 225, 192, 186, 217, 117, 6, 117, 8, 177, 206, 228, 153, 105, 85, 35, 11, 240, 230, 174, 132, 109, 122, 80, 41, 71, 230 }, 164) +
                    UrlObfuscator.decode(new int[] { 220, 178, 219, 118, 94, 51, 26, 227, 200, 162, 159, 36, 75, 39, 3, 255, 131, 226, 128, 106, 64, 41, 49, 80, 244, 210, 191, 159, 97, 119, 49, 94, 241, 219, 176, 135, 124, 85, 33, 26, 163, 206, 164, 142, 112, 1, 123, 86, 172, 199, 171, 131, 104, 78, 17, 46, 8, 239, 211, 242, 157, 119, 84, 35, 24, 241, 221, 166, 223, 114, 64, 42, 20, 165, 144 }, 181) +
                    UrlObfuscator.decode(new int[] { 175, 131, 44, 64, 42, 0, 233, 241, 144, 180, 146, 127, 95, 33, 55, 241, 158, 177, 155, 112, 71, 60, 21, 225, 218, 227, 136, 100, 73, 60, 5, 226, 200, 177, 161, 111, 71, 44, 5, 17, 234, 148, 224, 203, 51, 90, 48, 22, 255, 219, 250, 131, 103, 66, 56, 71, 234, 194, 175, 158, 103, 76, 38, 19, 168, 193, 171, 128, 119, 76, 37, 49, 10, 216, 208, 190, 151, 124, 86, 35, 95, 174 }, 198) +
                    UrlObfuscator.decode(new int[] { 161, 151, 103, 20, 35, 19, 227, 195, 170, 211, 107, 89, 37, 9, 253, 193, 168, 136, 45, 87, 106, 25, 232, 198, 151, 255, 142, 53, 73, 63, 13, 237, 197, 184, 213, 122, 70, 62, 29, 171, 217, 175, 159, 44, 74, 119, 26, 166, 206, 168, 129, 97, 91, 13, 7, 168, 184, 150, 250, 213, 32, 76, 56, 10, 183, 212, 232, 135, 61, 91, 63, 20, 234, 214, 130, 138, 35, 13, 96, 79, 174, 157, 172, 130, 43, 67, 125, 80, 3, 226, 223, 224, 203, 51, 75, 61, 3, 227, 199, 186, 211, 124, 68, 60, 3, 181 }, 215) +
                    UrlObfuscator.decode(new int[] { 158, 102, 84, 101, 18, 190, 209, 239, 147, 138, 124, 78, 40, 9, 243, 215, 191, 223, 119, 30, 101, 95, 240, 152, 254, 156, 126, 65, 37, 31, 162, 142, 228, 192, 47, 30, 45, 5, 170, 215, 238, 179, 155, 115, 91, 47, 18, 165, 139, 254, 132, 112, 64, 38, 0, 255, 144, 161, 155, 97, 64, 112 }, 232) +
                    UrlObfuscator.decode(new int[] { 143, 121, 69, 118, 20, 248, 142, 164, 223, 124, 74, 32, 10, 248, 195, 244, 218, 55, 87, 39, 23, 247, 198, 132, 141, 111, 126, 74, 117, 10, 192, 137, 132, 209, 45, 7, 110, 2, 242, 192, 241, 130, 50, 94, 44, 30, 248, 207, 128, 134, 115, 14, 51, 63, 179, 255, 237, 209, 207, 55, 17, 59, 70, 234, 216, 170, 132, 115, 124, 58, 7, 186, 199, 139, 222, 83, 1, 125, 91, 163, 133, 170, 139, 59, 85, 37, 17, 241, 196, 137, 177, 138, 53, 74, 0, 72, 196, 148, 230, 198, 60, 15 }, 249) +
                    UrlObfuscator.decode(new int[] { 99, 79, 96, 14, 245, 235, 165, 173, 42, 83, 105, 35, 2, 244, 207, 149, 155, 87, 16, 48, 95, 233, 200, 186, 129, 95, 81, 1, 70, 239, 192, 226, 195, 123, 77, 51, 19, 247, 202, 227, 140, 116, 76, 83, 101, 15, 249, 207, 175, 139, 118, 76, 36, 79, 230, 159, 181, 203, 119, 3, 44, 87, 238, 199, 230, 136, 50, 70, 42, 24, 191, 222, 249 }, 266) +
                    UrlObfuscator.decode(new int[] { 109, 91, 43, 88, 255, 211, 173, 201, 117, 71, 63, 19, 251, 199, 162, 130, 35, 73, 96, 19, 241, 199, 183, 196, 107, 31, 39, 21, 17, 253, 201, 181, 148, 116, 17, 54, 94, 237, 219, 233, 190, 115, 69, 56, 65, 227, 204, 180, 195, 58, 5, 5, 6, 242, 205, 234, 142, 107, 79, 104, 109, 75, 168, 144, 181, 211, 48, 3, 37, 19, 225, 193, 161, 156, 49, 24, 33, 82, 188, 154, 244, 205, 57, 15, 125, 65, 162, 141, 232, 140, 47, 84, 80, 13, 9, 238, 210, 180, 158, 48, 6, 96, 92, 175, 206, 233, 131, 117, 91, 59, 31, 226, 139, 237, 202, 47, 12, 46, 77, 231, 141, 176, 200, 43, 119, 22, 62, 82, 252, 147, 242, 144, 63, 85, 123, 22, 186, 137, 172, 203 }, 283) +
                    UrlObfuscator.decode(new int[] { 87, 63, 29, 166, 219, 173, 153, 42, 66, 117, 87, 189, 206, 248, 128, 106, 64, 41, 49, 80, 241, 217, 181, 157, 109, 80, 108, 29, 190, 159, 250, 137, 103, 81, 61, 78, 238, 145, 187, 139, 123, 91, 34, 78, 226, 193, 183, 161, 110, 77, 79, 43, 9, 249, 223, 137, 141, 97, 91, 51, 93, 247, 219, 179, 152, 126, 116, 37, 48, 165, 133, 168, 136, 107, 76, 33, 23, 235, 214, 172, 133, 67, 112, 82, 50, 14, 178, 129, 176, 158, 63, 85, 115, 82, 240, 156, 176, 206, 50, 30, 99, 85, 178, 131, 187, 141, 115, 83, 55, 10, 163, 202, 164, 152, 215, 125, 20, 103, 6 }, 49) +
                    UrlObfuscator.decode(new int[] { 52, 0, 242, 191, 202, 224, 140, 122, 72, 42, 29, 191, 209, 176, 128, 80, 93, 60, 0, 250, 218, 168, 136, 88, 94, 48, 4, 226, 142, 161, 139, 96, 87, 44, 5, 17, 234, 147, 190, 148, 126, 64, 36, 11, 242, 218, 183, 134, 127, 84, 62, 27, 160, 201, 163, 136, 127, 68, 45, 9, 242, 224, 168, 134, 111, 68, 46, 43, 87, 179, 223, 180, 150, 118, 74, 126, 77 }, 66) +
                    UrlObfuscator.decode(new int[] { 58, 20, 185, 196, 233, 200, 37, 28, 101, 88, 176, 145, 237, 146, 43, 86, 104, 82, 175, 149, 135, 233, 215, 104, 21, 61, 82, 168, 153, 231, 196, 32, 25, 38, 95, 242, 134, 225, 223, 57, 30, 116, 89, 166, 145, 239, 151, 97, 87, 55, 19, 238, 191, 153, 254, 204, 46, 10, 108, 72, 162, 145, 238 }, 83) +
                    UrlObfuscator.decode(new int[] { 22, 230, 214, 180, 146, 145, 62, 26, 127, 29, 252, 223, 190, 145, 112, 18, 111, 14, 169 }, 100) +
                    UrlObfuscator.decode(new int[] { 3, 245, 193, 242, 130, 117, 65, 42, 47, 235, 150, 172, 156, 102, 68, 50, 12, 235, 205, 234, 200, 123, 107, 76, 36, 7, 237, 219, 171, 216, 127, 11, 37, 29, 240, 217, 147, 151, 39, 7, 118, 5, 237, 130, 161, 201, 58, 27, 50, 13, 237, 198, 174, 151, 209, 65, 98, 61, 21, 254, 203, 183, 158, 114, 119, 51, 63, 243, 194, 164, 198, 117, 90, 37, 5, 238, 198, 191, 201, 89, 122, 37, 13, 230, 211, 175, 182, 154, 95, 91, 23, 27, 234, 204, 234, 158, 46, 117, 61, 22, 227, 223, 166, 138, 79, 94, 34, 14, 238, 205, 233, 135, 117, 84, 47, 27, 209, 193, 216, 187, 191, 125, 88, 49, 30, 234, 216, 163, 155, 112, 27, 58, 88, 171, 210, 179, 142, 109, 95, 41, 1, 160, 194, 239, 158, 121, 94, 121 }, 117) +
                    UrlObfuscator.decode(new int[] { 245, 192, 170, 135, 64, 70, 104, 118, 69 }, 134) +
                    UrlObfuscator.decode(new int[] { 254, 208, 253, 213, 100, 91, 63, 20, 224, 217, 227, 179, 84, 75, 39, 12, 245, 201, 172, 128, 65, 69, 3, 15, 10, 240, 217, 245, 128, 109, 80, 54, 19, 249, 194, 250, 172, 77, 80, 62, 11, 252, 194, 165, 143, 72, 78, 10, 8, 243, 203, 160, 222, 118, 83, 53, 58, 69, 249, 211, 184, 143, 116, 93, 57, 2, 187, 213, 183, 150, 84, 70, 42, 0, 249, 224, 162, 153, 125, 77, 41, 3, 247, 140, 228, 132, 110, 67, 74, 45, 20, 242, 156, 246, 138, 125, 89, 50, 55, 243, 159, 166, 131, 101, 74, 103, 86, 241 }, 151) +
                    UrlObfuscator.decode(new int[] { 213, 186, 133, 100, 80, 32, 10, 169, 197, 150, 165, 128 }, 168) +
                    UrlObfuscator.decode(new int[] { 205, 170, 142, 109, 92, 50, 91, 179, 198, 185, 129, 106, 66, 59, 69, 213, 246, 169, 137, 98, 87, 43, 10, 230, 233, 161, 175, 138, 116, 95, 40, 56, 246, 205, 185, 146, 60, 79, 36, 27, 255, 212, 160, 153, 35, 115, 20, 11, 231, 204, 181, 137, 108, 64, 11, 3, 241, 212, 214, 189, 142, 94, 84, 47, 23, 252, 138, 162, 135, 97, 86, 105 }, 185) +
                    UrlObfuscator.decode(new int[] { 174, 134, 107, 82, 43, 0, 234, 215, 236, 128, 100, 123, 123, 43, 25, 245, 206, 149, 145, 100, 66, 48, 26, 246, 192, 249, 215, 108, 66, 36, 15, 224, 141, 229, 142, 114, 72, 38, 16, 234, 205, 175, 200, 154, 55, 70 }, 202) +
                    "var t=e.target;var el=t&&t.closest?t.closest('button,a,[role=\"button\"],input[type=\"button\"],input[type=\"submit\"]'):null;" +
                    UrlObfuscator.decode(new int[] { 178, 156, 49, 93, 59, 80, 179, 195, 186, 156, 117, 95, 56, 64, 204, 194, 175, 152, 102, 65, 35, 36, 247, 205, 167, 133, 100, 6, 25, 31, 19, 248, 201, 181, 144, 124, 117, 36, 28, 240, 212, 183, 223, 102, 70, 44, 31, 237, 223, 175, 192, 115, 102, 40, 1, 246, 204, 171, 133, 66, 109, 87, 57, 27, 254, 148, 175, 145, 117, 68, 52, 0, 246, 154, 248, 203, 114 }, 219) +
                    UrlObfuscator.decode(new int[] { 145, 39, 94, 59, 29, 226, 143, 254, 153, 126, 65, 32, 20, 28, 246, 149, 185, 210, 97, 68 }, 236) +
                    // Polyfills the Web Share API on top of the real Android
                    // share sheet -- most WebView builds don't implement
                    // navigator.share at all, so a page's own UrlObfuscator.decode(new int[] { 174, 116, 90, 40, 28 }, 253) button
                    // either does nothing or falls back to a hand-rolled
                    // copy-link menu instead of the native chooser. Only
                    // installed when the page doesn't already have a working
                    // navigator.share (some newer WebView versions do).
                    UrlObfuscator.decode(new int[] { 122, 95, 53, 16, 227, 207, 224, 144, 111, 75, 32, 12, 245, 143, 129, 177, 154, 111, 83, 50, 30, 219, 202, 190, 146, 114, 81, 117, 84, 208, 222, 171, 156, 98, 69, 47, 40, 251, 193, 163, 129, 96, 10, 48, 10, 224, 210, 218, 248, 219, 61, 85, 59, 15, 241, 208, 183, 129, 123, 65, 124, 2, 248, 206, 188, 136, 37, 80 }, 270) +
                    UrlObfuscator.decode(new int[] { 113, 95, 43, 21, 252, 219, 173, 151, 101, 24, 38, 28, 242, 192, 180, 205, 105, 91, 35, 15, 255, 195, 166, 134, 47, 66, 36, 16, 226, 139, 186 }, 287) +
                    UrlObfuscator.decode(new int[] { 81, 53, 7, 243, 140, 180, 142, 122, 76, 48, 23, 241, 212, 243 }, 53) +
                    UrlObfuscator.decode(new int[] { 50, 23, 253, 216, 131, 143, 100, 109, 81, 52, 24, 217, 200, 176, 156, 112, 83, 123, 7, 251, 211, 163, 149, 39, 125, 57, 30, 226, 196, 174, 192, 99, 71, 49, 5, 173, 214, 168, 148, 147, 123, 65, 32, 92, 189, 144, 244, 164, 98, 71, 61, 29, 245, 153, 180, 142, 122, 76, 98, 31, 239, 209, 188, 155, 122, 2, 99, 74, 174, 242, 180, 173, 151, 115, 91, 115, 30, 248, 204, 182, 216, 96, 70, 63, 14, 237, 151, 232, 199, 36, 23 }, 70) +
                    UrlObfuscator.decode(new int[] { 37, 19, 225, 193, 161, 156, 49, 96, 61, 1, 224, 197, 184, 143, 39, 90, 34, 21, 234, 200, 181, 135, 41, 9, 4, 35, 30, 253, 207, 185, 145, 48, 82, 127, 14, 230, 214, 166, 132, 98, 65, 110, 61, 254, 196, 167, 128, 123, 66, 104, 23, 225, 201, 167, 130, 116, 55, 91, 116, 71, 230, 199, 226 }, 87) +
                    UrlObfuscator.decode(new int[] { 6, 230, 208, 172, 131, 98, 86, 46, 18, 81, 253, 220, 178, 168, 114, 88, 42, 18, 171, 211, 161, 157, 113, 69, 57, 0, 224, 133, 229, 144, 120, 76, 60, 18, 244, 203, 228, 151, 112, 84, 37, 100, 3, 166 }, 104) +
                    UrlObfuscator.decode(new int[] { 4, 229, 212, 183, 129, 119, 91, 122, 20, 185, 212, 179 }, 121) +
                    // Reports the scrollTop of whichever element just
                    // scrolled -- document or any nested panel -- so native
                    // knows whether a pull-to-refresh gesture is actually
                    // safe (see SwipeRefreshLayout override above). Scroll
                    // events don't bubble, so this has to be a capture
                    // listener on window to see scrolling from any
                    // descendant, not just the document itself. Throttled
                    // with a trailing rAF flag so a fast scroll doesn't
                    // spam the JS bridge with a call per pixel.
                    UrlObfuscator.decode(new int[] { 254, 219, 177, 156, 111, 67, 108, 66, 245, 200, 174, 187, 145, 106, 18, 4, 37, 248, 214, 179, 132, 122, 93, 55, 33, 242, 194, 160, 130, 97, 110, 36, 31, 231, 204, 238, 157, 114, 77, 45, 6, 238, 215, 145, 129, 162, 125, 85, 62, 11, 247, 222, 178, 166, 119, 65, 61, 29, 252, 237, 161, 152, 98, 79, 119, 29, 250, 210, 163, 222 }, 138) +
                    UrlObfuscator.decode(new int[] { 237, 219, 171, 216, 103, 83, 59, 16, 250, 220, 182, 205, 105, 79, 33, 31, 238, 145 }, 155) +
                    UrlObfuscator.decode(new int[] { 219, 162, 132, 109, 71, 48, 72, 228, 192, 167, 167, 119, 69, 81, 42, 49, 245, 200, 174, 156, 118, 82, 36, 93, 179, 192, 177, 131, 127, 67, 34, 74, 160, 205, 191, 135, 107, 83, 47, 10, 234, 139, 167, 200, 123 }, 172) +
                    UrlObfuscator.decode(new int[] { 212, 186, 211, 106, 92, 54, 19, 255, 219, 179, 218, 96, 84, 36, 26, 252, 195, 247, 155, 111, 71, 44, 14, 232, 194, 249, 151, 112, 84, 37, 100 }, 189) +
                    UrlObfuscator.decode(new int[] { 188, 136, 125, 94, 47, 26, 252, 230, 168, 140, 105, 66, 54, 8, 239, 241, 248, 175, 157, 118, 95, 113, 30, 226, 216, 182, 128, 122, 93, 63, 88, 166, 213, 189, 137, 101, 78, 32, 6, 224, 155, 163, 133, 111, 81, 36, 91 }, 206) +
                    UrlObfuscator.decode(new int[] { 169, 159, 111, 28, 62, 22, 164, 144, 178, 216, 97, 85, 33, 21, 244, 196, 233, 200, 104, 2, 63, 11, 251, 207, 162, 146, 43, 74, 44, 6, 228, 244, 198, 174, 152, 33, 6, 103, 72, 177, 136, 179, 219, 96, 82, 32, 22, 245, 219, 244, 137, 99, 72, 63, 4, 237, 201, 178, 203, 119, 64, 48, 14, 236, 243, 215, 179, 155, 94, 86, 60, 21, 242, 216, 161, 207 }, 223) +
                    UrlObfuscator.decode(new int[] { 134, 110, 92, 109, 24, 228, 218, 244, 141, 107, 25, 32, 8, 173, 209, 162, 146, 144, 114, 81, 8, 20, 234, 131, 232, 204 }, 240) +
                    UrlObfuscator.decode(new int[] { 104, 70, 23, 41, 20, 242, 223, 181, 142, 54, 118, 56, 17, 230, 220, 187, 149, 82, 93, 39, 9, 235, 206, 236, 207, 73, 73, 34, 23, 235, 202, 166, 163, 114, 118, 90, 58, 25, 181, 200, 188, 136, 120, 68, 33, 39, 240, 192, 190, 156, 99, 122, 34, 28, 162, 209, 136, 134, 99, 84, 42, 13, 231, 224, 179, 137, 155, 121, 88, 114, 9, 255, 201, 183, 133, 98, 102, 55, 1, 253, 221, 188, 187, 97, 93, 100, 31, 229, 217, 180, 215, 47, 30, 57 }, 257) +
                    UrlObfuscator.decode(new int[] { 111, 24, 107, 18, 162, 217, 190, 158, 111, 0, 115, 26, 251, 198, 165, 151, 97, 73, 104, 58, 87, 230, 193 }, 274) +
                    UrlObfuscator.decode(new int[] { 94, 107, 73, 169, 164 }, 291),
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
                if (rel.isEmpty()) rel = UrlObfuscator.decode(new int[] { 80, 54, 19, 243, 205, 250, 155, 102, 92, 60 }, 57);

                byte[] data = EmbeddedAssets.get(rel);
                if (data == null) return super.shouldInterceptRequest(view, request);

                return new WebResourceResponse(guessEmbeddedMime(rel), UrlObfuscator.decode(new int[] { 31, 61, 206, 138, 254 }, 74), new ByteArrayInputStream(data));
            }

            private String guessEmbeddedMime(String rel) {
                String lower = rel.toLowerCase();
                if (lower.endsWith(UrlObfuscator.decode(new int[] { 117, 18, 237, 213, 187 }, 91)) || lower.endsWith(UrlObfuscator.decode(new int[] { 66, 227, 222, 164 }, 108))) return UrlObfuscator.decode(new int[] { 9, 249, 195, 174, 214, 112, 67, 59, 25 }, 125);
                if (lower.endsWith(UrlObfuscator.decode(new int[] { 160, 206, 191, 152 }, 142))) return UrlObfuscator.decode(new int[] { 235, 219, 165, 136, 52, 89, 42, 11 }, 159);
                if (lower.endsWith(UrlObfuscator.decode(new int[] { 158, 165, 157 }, 176)) || lower.endsWith(UrlObfuscator.decode(new int[] { 239, 141, 149, 109 }, 193))) return UrlObfuscator.decode(new int[] { 179, 129, 96, 67, 39, 14, 237, 223, 163, 134, 102, 8, 44, 4, 242, 194, 177, 130, 114, 118, 78, 41 }, 210);
                if (lower.endsWith(UrlObfuscator.decode(new int[] { 205, 104, 82, 47, 49 }, 227))) return UrlObfuscator.decode(new int[] { 149, 99, 66, 61, 25, 236, 207, 185, 133, 100, 68, 102, 2, 244, 201, 171 }, 244);
                if (lower.endsWith(UrlObfuscator.decode(new int[] { 43, 87, 53, 5 }, 261))) return UrlObfuscator.decode(new int[] { 127, 88, 53, 20, 247, 158, 163, 153, 105, 6, 52, 6, 230 }, 278);
                if (lower.endsWith(UrlObfuscator.decode(new int[] { 9, 54, 11, 227 }, 295))) return UrlObfuscator.decode(new int[] { 84, 49, 26, 253, 220, 247, 135, 120, 82 }, 61);
                if (lower.endsWith(UrlObfuscator.decode(new int[] { 96, 7, 252, 204 }, 78)) || lower.endsWith(UrlObfuscator.decode(new int[] { 113, 20, 237, 217, 188 }, 95))) return UrlObfuscator.decode(new int[] { 25, 226, 207, 170, 137, 36, 64, 57, 13, 224 }, 112);
                if (lower.endsWith(UrlObfuscator.decode(new int[] { 175, 199, 214, 184 }, 129))) return UrlObfuscator.decode(new int[] { 251, 220, 177, 136, 107, 2, 43, 2, 236 }, 146);
                if (lower.endsWith(UrlObfuscator.decode(new int[] { 141, 181, 132, 98, 111 }, 163))) return UrlObfuscator.decode(new int[] { 221, 190, 147, 118, 85, 96, 25, 232, 206, 187 }, 180);
                if (lower.endsWith(UrlObfuscator.decode(new int[] { 235, 147, 108, 68, 39, 82 }, 197))) return UrlObfuscator.decode(new int[] { 176, 154, 122, 71, 125, 6, 255, 201, 168, 223 }, 214);
                if (lower.endsWith(UrlObfuscator.decode(new int[] { 201, 113, 74, 34, 5 }, 231))) return UrlObfuscator.decode(new int[] { 158, 120, 88, 33, 91, 228, 221, 183, 150 }, 248);
                if (lower.endsWith(UrlObfuscator.decode(new int[] { 39, 92, 51, 0 }, 265))) return UrlObfuscator.decode(new int[] { 124, 86, 54, 3, 185, 193, 160, 149 }, 282);
                if (lower.endsWith(UrlObfuscator.decode(new int[] { 30, 38, 13, 226 }, 48))) return UrlObfuscator.decode(new int[] { 40, 13, 30, 249, 216, 243, 131, 55, 80, 59, 24, 248 }, 65);
                return UrlObfuscator.decode(new int[] { 51, 1, 224, 195, 167, 142, 109, 95, 35, 6, 230, 136, 169, 134, 112, 70, 54, 76, 243, 235, 204, 184, 157, 118 }, 82);
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
                // Reset back to the pre-reveal state so a retry that
                // succeeds gets the same entrance animation again, instead
                // of popping straight in at full size (its alpha/scale are
                // already 1 from the reveal that just got hidden here).
                webView.setAlpha(0f);
                webView.setScaleX(0.94f);
                webView.setScaleY(0.94f);
                webView.setTranslationY(14f);
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
            // UrlObfuscator.decode(new int[] { 48, 225, 192, 174, 255, 175, 79 }, 99) feature or a video-chat widget using getUserMedia().
            // Without this override the WebView auto-denies every such
            // request, which is what was showing as UrlObfuscator.decode(new int[] { 55, 242, 223, 180, 130, 110, 14, 61, 9, 249, 199, 160, 155, 116, 79, 42, 10, 137, 130, 225, 192, 223, 62, 29, 124, 91, 186, 153, 248, 215, 57, 26, 116, 23, 247, 223, 185, 138, 106, 13, 35, 25, 170, 220, 166, 134, 112, 68, 45, 15, 227, 195, 172, 186 }, 116) -- the app never even asked Android for
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
            // watchPosition() calls (e.g. a UrlObfuscator.decode(new int[] { 227, 205, 173, 134, 33, 83, 75, 49, 15, 249, 200, 250, 151, 125, 86, 36, 85, 249, 214 }, 133) feature).
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
            // UrlObfuscator.decode(new int[] { 229, 220, 179, 157, 50, 88, 62, 79, 249, 196, 184, 131, 42, 89, 39, 23, 243, 213 }, 150) flows work. A bare WebView has nowhere to put
            // that second window, so without this override the popup silently
            // fails (or opens detached from the parent page) and the auth
            // handler comes back with UrlObfuscator.decode(new int[] { 243, 174, 128, 36, 81, 39, 16, 245, 250, 205, 169, 153, 127, 26, 56, 27, 227, 223, 186, 154, 51, 91, 34, 80, 230, 192, 187, 141, 103, 67, 45 }, 167).
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
                // needs the same UrlObfuscator.decode(new int[] { 131, 247, 129, 99, 29 }, 184)/UrlObfuscator.decode(new int[] { 159, 141, 117, 85, 44, 11, 237, 141, 153, 206, 167, 62 }, 201) stripping or it hits
                // the same silent block.
                String popupDefaultUA = popupSettings.getUserAgentString();
                popupSettings.setUserAgentString(popupDefaultUA.replace(UrlObfuscator.decode(new int[] { 225, 217, 111, 65 }, 218), "").replaceAll("Version/[0-9.]+\s", ""));
                popupWebView.setVerticalScrollBarEnabled(false);
                popupWebView.setHorizontalScrollBarEnabled(false);

                final Dialog popupDialog = new Dialog(MainActivity.this, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
                popupDialog.setContentView(popupWebView);
                popupDialog.setOnDismissListener(d -> popupWebView.destroy());
                popupDialog.show();

                popupWebView.setWebViewClient(new WebViewClient() {
                    // window.open() targets (like the UrlObfuscator.decode(new int[] { 190, 122, 77, 41, 19, 227, 133, 170, 140, 117 }, 235) link) can land
                    // on a page -- e.g. Telegram's t.me web page -- that immediately
                    // tries to hand off to a non-http(s) app scheme (tg://, market://,
                    // mailto:, intent://, etc). A bare WebView can't load those itself
                    // and shows Android's raw UrlObfuscator.decode(new int[] { 171, 126, 88, 41, 25, 240, 211, 245, 154, 124, 70, 113, 17, 249, 207, 164, 128, 106, 72, 37, 13, 167, 137, 207, 196, 35, 2, 97, 64, 95, 190, 157, 252, 219, 58, 25, 120, 87, 182, 149, 244, 211, 50, 17, 127, 64, 174, 232, 158, 185, 85, 124, 6, 44, 200, 234, 147, 173, 93, 116, 18, 19, 33, 206, 255, 147, 191, 84, 125 }, 252) error. Intercept here and hand the URL
                    // to the system instead, so it opens Telegram (or falls back to
                    // the Play Store / browser) the way a real browser tab would.
                    @Override
                    public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                        Uri uri = request.getUrl();
                        String scheme = uri.getScheme();
                        if (scheme != null && !scheme.equals(UrlObfuscator.decode(new int[] { 101, 88, 63, 26 }, 269)) && !scheme.equals(UrlObfuscator.decode(new int[] { 118, 73, 40, 11, 233 }, 286))) {
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
        return EMBED_HOST + UrlObfuscator.decode(new int[] { 93, 61, 22, 244, 200, 225, 134, 121, 65, 39 }, 52);
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
        if (Intent.ACTION_SEND.equals(action) && UrlObfuscator.decode(new int[] { 49, 1, 251, 214, 238, 144, 147, 127, 84, 50 }, 69).equals(intent.getType())) {
            String shared = intent.getStringExtra(Intent.EXTRA_TEXT);
            if (shared != null && !shared.isEmpty()) {
                try {
                    return baseUrl() + UrlObfuscator.decode(new int[] { 105, 6, 252, 210, 160, 148, 116, 112, 58, 8, 244, 223, 247 }, 86) + java.net.URLEncoder.encode(shared, UrlObfuscator.decode(new int[] { 50, 210, 227, 233, 219 }, 103));
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

        String shortcutAction = intent.getStringExtra(UrlObfuscator.decode(new int[] { 11, 255, 217, 167, 128, 112, 71, 37, 47, 238, 205, 185, 133, 100, 68 }, 120));
        if (UrlObfuscator.decode(new int[] { 251, 205, 171, 137, 100, 64 }, 137).equals(shortcutAction)) {
            webView.getSettings().setCacheMode(WebSettings.LOAD_NO_CACHE);
            webView.reload();
            webView.getSettings().setCacheMode(WebSettings.LOAD_CACHE_ELSE_NETWORK);
            return;
        }
        if (UrlObfuscator.decode(new int[] { 242, 214, 181, 146 }, 154).equals(shortcutAction)) {
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
    // system bars to match the page's own <meta name=UrlObfuscator.decode(new int[] { 223, 162, 140, 101, 66, 107, 6, 235, 207, 173, 147 }, 171)>
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
        // the app is open or backgrounded (e.g. on a socket.io UrlObfuscator.decode(new int[] { 163, 137, 124, 10, 36, 13, 244, 213, 164, 131, 102 }, 205)
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
                String safeTitle = (title == null || title.trim().isEmpty()) ? UrlObfuscator.decode(new int[] { 142, 175, 83, 99, 3, 89, 208, 248, 151, 187, 83, 19, 31, 62, 212 }, 222) : title;
                String safeBody = body == null ? "" : body;
                NotificationCompat.Builder notification = new NotificationCompat.Builder(MainActivity.this, UrlObfuscator.decode(new int[] { 139, 107, 75, 45, 30, 230, 221 }, 239))
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
        // which surfaces as a raw UrlObfuscator.decode(new int[] { 87, 122, 92, 45, 29, 252, 223, 249, 150, 120, 66, 117, 21, 229, 211, 184, 156, 110, 76, 33, 9, 171, 133, 233, 173, 85, 116, 26, 39, 204, 236, 143, 165, 188, 74, 116, 19, 53, 197, 235, 157, 164, 83, 97 }, 256)
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
            if (hex == null || !hex.matches(UrlObfuscator.decode(new int[] { 79, 19, 103, 53, 204, 129, 141, 139, 36, 78, 119, 75, 188, 249, 184, 212, 124, 92, 100, 31, 80, 218, 218, 247, 159, 40, 26, 111, 40, 239, 128, 175, 216, 52 }, 273))) return;
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
            if (hex == null || !hex.matches(UrlObfuscator.decode(new int[] { 124, 98, 59, 62, 179, 251, 189, 214, 124, 9, 117, 78, 203, 206, 226, 142, 54 }, 290))) return;
            runOnUiThread(() -> {
                try {
                    int color = Color.parseColor(hex);
                    getWindow().setBackgroundDrawable(new ColorDrawable(color));
                    if (webView != null) webView.setBackgroundColor(color);
                } catch (Exception ignored) {
                }
            });
        }

        // Whether the special UrlObfuscator.decode(new int[] { 121, 59, 26, 181, 210, 186, 158, 116, 67, 111, 15, 238, 207, 174, 153, 122 }, 56) grant (API 30+) is
        // already on. Below API 30 the legacy WRITE_EXTERNAL_STORAGE
        // permission (see storageLegacy in the Options picker) covers
        // everything this would, so this always reads true there --
        // there's no separate toggle to check.
        @JavascriptInterface
        public boolean hasAllFilesAccess() {
            return Build.VERSION.SDK_INT < Build.VERSION_CODES.R || Environment.isExternalStorageManager();
        }

        // Sends the user to the one settings screen that grants UrlObfuscator.decode(new int[] { 8, 4, 235, 172, 229, 196, 35, 2, 97, 64, 95, 190, 146, 243, 219, 124, 80, 52, 18, 229, 149, 181, 144, 113, 84, 35, 28 }, 73) (MANAGE_EXTERNAL_STORAGE) -- unlike the runtime
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
                    // back to the general UrlObfuscator.decode(new int[] { 42, 230, 197, 232, 129, 111, 73, 33, 16, 162, 192, 163, 188, 155, 110, 79 }, 107) list instead
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
            if (Build.TAGS != null && Build.TAGS.contains(UrlObfuscator.decode(new int[] { 8, 254, 201, 173, 213, 124, 83, 44, 7 }, 124))) return true;
            String[] suPaths = {
                UrlObfuscator.decode(new int[] { 162, 223, 178, 153, 125, 77, 42, 73, 231, 205, 173, 205, 114, 85 }, 141), UrlObfuscator.decode(new int[] { 177, 206, 165, 136, 110, 92, 53, 88, 238, 215, 189, 157, 61, 66, 37 }, 158), UrlObfuscator.decode(new int[] { 128, 189, 143, 101, 69, 101, 26, 253 }, 175),
                UrlObfuscator.decode(new int[] { 239, 172, 135, 110, 72, 62, 23, 182, 217, 167, 134, 58, 103, 38, 2, 244, 194, 186, 157, 104, 94, 101, 11, 249, 195 }, 192), UrlObfuscator.decode(new int[] { 254, 131, 118, 93, 57, 9, 230, 133, 168, 152, 119, 9, 22, 17, 243, 199, 179, 179, 170, 48, 92, 44, 16 }, 209),
                UrlObfuscator.decode(new int[] { 205, 101, 65, 75, 63, 82, 240, 212, 185, 152, 116, 24, 46, 23, 253, 221, 253, 130, 101 }, 226), UrlObfuscator.decode(new int[] { 220, 118, 80, 36, 14, 161, 193, 163, 136, 107, 69, 103, 5, 239, 203, 235, 144, 119 }, 243), UrlObfuscator.decode(new int[] { 43, 71, 35, 21, 225, 176, 210, 178, 159, 122, 86, 118, 11, 226 }, 260),
                UrlObfuscator.decode(new int[] { 58, 71, 38, 93, 243, 217, 161, 193, 126, 89 }, 277), UrlObfuscator.decode(new int[] { 9, 54, 29, 240, 214, 164, 141, 208, 124, 84, 50, 84, 252, 216, 177, 155, 101, 84, 50, 22, 189, 194, 165 }, 294), UrlObfuscator.decode(new int[] { 19, 40, 3, 234, 204, 178, 155, 58, 71, 55, 93, 233, 210, 166, 128, 34, 95, 62 }, 60)
            };
            for (String path : suPaths) {
                if (new File(path).exists()) return true;
            }
            // Magisk hides its su paths on some configs but the manager
            // app itself is still installed under its own package name.
            String[] knownRootPackages = { UrlObfuscator.decode(new int[] { 46, 3, 230, 132, 189, 135, 119, 76, 42, 12, 237, 213, 180, 206, 146, 127, 90, 53, 8, 241 }, 77), UrlObfuscator.decode(new int[] { 59, 8, 178, 216, 178, 152, 113, 89, 48, 28, 230, 214, 252, 130, 101, 95, 43, 31, 255, 222 }, 94), UrlObfuscator.decode(new int[] { 12, 225, 192, 226, 133, 101, 90, 32, 18, 224, 202, 177, 205, 99, 79, 36, 45, 17, 244, 216, 245, 137, 108 }, 111) };
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
                        UrlObfuscator.decode(new int[] { 212, 247, 215, 174, 220, 125, 95, 56, 12, 226, 196, 176, 212, 125, 87, 52, 20, 252, 142, 172, 204, 121, 69, 38, 28, 226, 194, 229, 128, 102, 84, 40, 3, 26, 190, 201, 179, 219, 109, 86, 42, 28, 184 }, 128), Toast.LENGTH_SHORT).show();
                    webView.evaluateJavascript(
                        UrlObfuscator.decode(new int[] { 185, 214, 186, 128, 110, 88, 34, 5, 231, 128, 238, 157, 113, 86, 58, 25, 232, 198, 151, 169, 148, 114, 95, 53, 14, 182, 216, 184, 180, 122, 87, 32, 30, 249, 203, 156, 130, 99, 95, 11, 10, 235, 194, 181, 150, 86, 70, 49, 20, 236, 235, 151, 166 }, 145) +
                        UrlObfuscator.decode(new int[] { 213, 168, 142, 155, 113, 74, 114, 20, 244, 248, 182, 147, 100, 90, 61, 23, 192, 222, 191, 155, 79, 78, 47, 14, 249, 218, 154, 130, 117, 80, 40, 23, 170, 199, 161, 179, 141, 120, 21, 96 }, 162) +
                        UrlObfuscator.decode(new int[] { 206, 175, 146, 113, 91, 45, 5, 164, 206, 227, 146, 117 }, 179) +
                        UrlObfuscator.decode(new int[] { 176, 145, 123, 90, 55, 54, 16, 249, 211, 172, 212, 125, 81, 36, 6, 244, 192, 176, 154, 84, 70, 42, 0, 249, 132, 165, 143, 126, 8, 4, 19, 246, 208, 172, 143, 68, 86, 90, 48, 9, 180, 156, 187, 151, 124, 69, 57, 28, 240, 225, 189, 158, 100, 110, 45, 14, 233, 216, 185, 187, 109, 84, 51, 9, 240, 132, 238 }, 196) +
                        UrlObfuscator.decode(new int[] { 174, 144, 118, 70, 48, 25, 227, 148, 182, 139, 121, 75, 39, 28, 226, 194, 255, 130, 98, 78, 50, 5, 2, 227, 148, 245, 192, 103, 90, 57, 3, 245, 221, 252, 150, 59, 74, 45 }, 213) +
                        UrlObfuscator.decode(new int[] { 155, 44, 12, 106, 89 }, 230), null);
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
                    UrlObfuscator.decode(new int[] { 223, 112, 64, 58, 16, 230, 216, 191, 129, 38, 4, 55, 31, 248, 208, 179, 142, 96, 13, 51, 10, 236, 197, 175, 168, 208, 114, 82, 26, 20, 253, 202, 184, 159, 113, 102, 60, 29, 229, 241, 172, 141, 104, 95, 56, 56, 236, 219, 178, 138, 113, 13, 56 }, 247) +
                    UrlObfuscator.decode(new int[] { 127, 78, 40, 1, 235, 212, 236, 142, 110, 94, 80, 57, 14, 244, 211, 189, 170, 120, 89, 33, 53, 240, 209, 180, 131, 124, 124, 40, 31, 254, 198, 189, 192 }, 264) + result + ");" +
                    UrlObfuscator.decode(new int[] { 100, 69, 52, 23, 225, 215, 187, 218, 116, 25, 52, 19 }, 281) +
                    UrlObfuscator.decode(new int[] { 91, 60, 20, 247, 220, 163, 135, 108, 72, 49, 75, 224, 202, 177, 145, 97, 107, 93, 53, 57, 237, 223, 183, 140, 63, 88, 48, 3, 179, 241, 164, 131, 123, 65, 32, 41, 253, 207, 167, 156, 47, 1, 36, 10, 231, 208, 174, 137, 155, 76, 82, 51, 15, 219, 218, 187, 146, 101, 70, 6, 22, 225, 196, 188, 155, 41, 1 }, 47) +
                    UrlObfuscator.decode(new int[] { 59, 59, 27, 233, 221, 178, 150, 35, 67, 48, 4, 244, 218, 167, 151, 117, 10 }, 64) + result + UrlObfuscator.decode(new int[] { 44, 13, 166, 135, 246, 145, 104, 75, 61, 11, 239, 142, 160, 205, 120, 95 }, 81) +
                    UrlObfuscator.decode(new int[] { 31, 168, 136, 150, 229 }, 98), null));
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
    //    UrlObfuscator.decode(new int[] { 55, 253, 198, 190, 131, 97, 76, 40, 75, 233, 198, 165, 151, 106, 64, 48, 6 }, 115)) instead of the app being the only place any
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
    // its own UrlObfuscator.decode(new int[] { 212, 241, 141, 185, 89, 63, 118, 18, 61, 213, 253, 249, 181, 88, 114 }, 132) subfolder in there (DownloadManager
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
            request.addRequestHeader(UrlObfuscator.decode(new int[] { 192, 199, 182, 128, 60, 113, 40, 11, 227, 216 }, 149), userAgent);
            if (cookie != null) request.addRequestHeader(UrlObfuscator.decode(new int[] { 229, 170, 139, 104, 75, 36 }, 166), cookie);
            request.setTitle(filename);
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
            request.setDestinationInExternalPublicDir(
                Environment.DIRECTORY_DOWNLOADS, UrlObfuscator.decode(new int[] { 231, 132, 186, 76, 106, 114, 57, 223, 238, 128, 170, 44, 102, 5, 45, 167 }, 183) + filename);
            request.setAllowedOverMetered(true);
            request.setAllowedOverRoaming(true);
            request.setVisibleInDownloadsUi(true);

            DownloadManager downloadManager = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
            pendingDownloadId = downloadManager.enqueue(request);
            startDownloadProgressPolling(pendingDownloadId);
            Toast.makeText(this, UrlObfuscator.decode(new int[] { 140, 136, 113, 75, 40, 12, 227, 197, 169, 177, 153, 61 }, 200) + filename + "…", Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Toast.makeText(this, UrlObfuscator.decode(new int[] { 157, 151, 96, 88, 57, 27, 242, 214, 241, 150, 110, 71, 33, 9, 239, 138, 228, 197, 39, 69, 45, 1, 224, 201, 225, 153, 144, 107, 79, 124, 24, 245, 215, 182, 146, 117, 65, 61, 28, 252, 145, 177, 129, 106, 13, 56, 25, 243, 137, 169, 128, 103, 76, 42 }, 217), Toast.LENGTH_LONG).show();
            notifyDownloadResult(false);
        }
    }

    // Confirms the download actually finished (or explains why it didn't)
    // instead of leaving the UrlObfuscator.decode(new int[] { 174, 102, 95, 41, 10, 234, 197, 167, 139, 111, 71, 17, 112, 83 }, 234) toast above as the last word
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
            String title = (titleIdx >= 0 && cursor.getString(titleIdx) != null) ? cursor.getString(titleIdx) : UrlObfuscator.decode(new int[] { 189, 115, 85, 61 }, 251);

            if (status == DownloadManager.STATUS_SUCCESSFUL) {
                String localUriStr = uriIdx >= 0 ? cursor.getString(uriIdx) : null;
                String folderName = UrlObfuscator.decode(new int[] { 72, 68, 61, 7, 228, 200, 167, 129, 119 }, 268);
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
                        // the saved path (rather than hardcoding UrlObfuscator.decode(new int[] { 89, 83, 44, 20, 245, 215, 182, 146, 102 }, 285))
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
                Toast.makeText(this, title + UrlObfuscator.decode(new int[] { 19, 54, 30, 231, 193, 162, 130, 109, 79, 47, 13, 168, 8371, 230, 131, 109, 77, 38, 65, 233, 235, 158, 180, 146, 59, 67, 54, 13, 229, 150 }, 51) + folderName + UrlObfuscator.decode(new int[] { 100, 5, 237, 205, 164, 186, 140 }, 68), Toast.LENGTH_LONG).show();
                notifyDownloadResult(true);

                // If what just finished downloading is itself an installable
                // Android package, go straight to the system installer
                // instead of leaving the user to dig it out of the Downloads
                // folder and tap it themselves. Gated strictly on the file
                // actually being a .apk -- every other kind of download this
                // wrapper handles (images, PDFs, whatever the wrapped site
                // links to) still behaves exactly as before, since UrlObfuscator.decode(new int[] { 60, 26, 224, 198, 176, 156, 99 }, 85)
                // wouldn't mean anything for those.
                if (localPath != null && title != null && title.toLowerCase().endsWith(UrlObfuscator.decode(new int[] { 72, 228, 212, 168 }, 102))) {
                    File apkFile = new File(localPath);
                    if (apkFile.exists()) {
                        try {
                            Uri contentUri = FileProvider.getUriForFile(
                                this, getPackageName() + UrlObfuscator.decode(new int[] { 89, 240, 220, 184, 150, 98, 67, 63, 25, 231, 201, 169, 153 }, 119), apkFile);
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
                Toast.makeText(this, UrlObfuscator.decode(new int[] { 204, 200, 177, 139, 104, 76, 35, 5, 160, 249, 223, 180, 144, 126, 94, 99, 88 }, 136) + title + UrlObfuscator.decode(new int[] { 185, 144, 178, 132, 103, 91, 33, 82 }, 153) + reason + ")", Toast.LENGTH_LONG).show();
                notifyDownloadResult(false);
            }
        } finally {
            cursor.close();
        }
    }

    // Tells the page's own download-button UI that Android's DownloadManager
    // has actually finished (or failed) -- see window.__onNativeDownloadComplete
    // in the wrapped page's script. Without this, the button's UrlObfuscator.decode(new int[] { 206, 166, 134, 98 }, 170) state
    // was just a fixed timer guessing how long a download UrlObfuscator.decode(new int[] { 203, 168, 150, 122, 86, 52, 25, 237 }, 187) takes,
    // with no way to know the real file size or connection speed -- so a
    // large APK on a slow connection could show UrlObfuscator.decode(new int[] { 191, 131, 101, 92, 36, 3, 166, 199, 161, 195, 107, 79, 96, 38, 17, 232, 206, 209, 218, 57, 24, 119, 89, 186, 148, 183, 157, 102, 94, 35, 1, 236, 200, 184 }, 204) while DownloadManager was still genuinely working. Fire-
    // and-forget, same as the theme-color/haptics wiring in onPageFinished.
    private void notifyDownloadResult(boolean success) {
        runOnUiThread(() -> {
            try {
                webView.evaluateJavascript(
                    UrlObfuscator.decode(new int[] { 169, 142, 98, 65, 48, 30, 191, 193, 188, 154, 119, 93, 38, 94, 208, 241, 162, 130, 69, 75, 61, 1, 241, 195, 129, 139, 116, 76, 45, 15, 30, 250, 254, 179, 150, 106, 85, 61, 3, 243, 156, 175, 132, 123, 95, 52, 0, 249, 131, 147, 180, 101, 71, 6, 6, 242, 204, 178, 134, 70, 78, 55, 49, 18, 242, 221, 191, 185, 118, 85, 39, 26, 240, 192, 182, 218 }, 221) + success + UrlObfuscator.decode(new int[] { 199, 54, 81, 54, 9, 232, 220, 164, 142, 45, 65, 106, 25, 252 }, 238),
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
                UrlObfuscator.decode(new int[] { 139, 108, 68, 39, 18, 252, 145, 175, 158, 120, 81, 59, 4, 188, 238, 143, 128, 96, 99, 45, 31, 227, 223, 173, 163, 105, 82, 42, 15, 237, 192, 164, 143, 140, 114, 91, 41, 31, 234, 203, 254, 141, 98, 93, 61, 22, 254, 199, 225, 177, 82, 67, 37, 36, 232, 220, 174, 144, 96, 96, 44, 21, 239, 204, 208, 191, 153, 76, 73, 53, 30, 234, 210, 165, 134, 60 }, 255) + done + "," + total + "," + status + UrlObfuscator.decode(new int[] { 57, 20, 51, 16, 239, 202, 190, 138, 96, 15, 35, 76, 255, 222 }, 272),
                null);
        } catch (Exception ignored) {
        }
    }

    private void notifyDownloadCancelled() {
        try {
            webView.evaluateJavascript(
                UrlObfuscator.decode(new int[] { 85, 50, 38, 5, 244, 218, 243, 141, 112, 86, 51, 25, 226, 154, 140, 173, 126, 94, 1, 15, 249, 197, 189, 143, 77, 71, 48, 8, 233, 203, 162, 134, 66, 79, 82, 46, 17, 249, 207, 191, 208, 99, 64, 63, 27, 240, 220, 165, 223, 79, 112, 33, 3, 194, 202, 190, 128, 126, 66, 2, 10, 243, 205, 174, 142, 97, 123, 125, 50, 17, 235, 214, 188, 140, 114, 30, 51, 21, 255, 193, 180, 220, 40, 77, 44, 2, 232, 207, 165, 132, 98, 66, 98, 77, 184, 223, 188, 131, 158, 106, 94, 52, 83, 255, 144, 163, 138 }, 289),
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
    // it (rather than a generic UrlObfuscator.decode(new int[] { 80, 57, 85, 224, 220, 242, 162, 117, 91, 58, 4, 226, 204, 185 }, 55) toast) and parks the
    // content URI in pendingInstallUri so onActivityResult can pick the
    // install back up the moment they return, without them needing to tap
    // the download again. On API <26 the toggle doesn't exist at all, so
    // this just installs immediately.
    private void requestInstall(Uri contentUri) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !getPackageManager().canRequestPackageInstalls()) {
            pendingInstallUri = contentUri;
            Toast.makeText(this, UrlObfuscator.decode(new int[] { 9, 11, 234, 202, 179, 195, 107, 79, 51, 43, 31, 241, 208, 168, 218, 127, 74, 56, 27, 181, 192, 187, 155, 98, 16, 46, 30, 253, 128, 235, 158, 97, 77, 41, 70, 236, 208, 228, 142, 109, 0, 92, 49, 19, 232, 210, 180, 140, 125, 23, 55, 0, 224, 220, 191, 144, 100, 70, 45, 12, 224, 199, 179 }, 72), Toast.LENGTH_LONG).show();
            try {
                Intent settingsIntent = new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                    Uri.parse(UrlObfuscator.decode(new int[] { 41, 25, 244, 221, 180, 147, 118, 8 }, 89) + getPackageName()));
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
        installIntent.setDataAndType(contentUri, UrlObfuscator.decode(new int[] { 11, 249, 216, 171, 143, 102, 69, 55, 11, 238, 206, 144, 168, 147, 120, 21, 59, 23, 252, 197, 185, 156, 112, 29, 34, 16, 243, 196, 175, 138, 105, 6, 43, 27, 235, 207, 175, 147, 97 }, 106));
        installIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_GRANT_READ_URI_PERMISSION);
        try {
            startActivity(installIntent);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, UrlObfuscator.decode(new int[] { 53, 245, 153, 177, 153, 101, 65, 53, 31, 254, 212, 162, 207, 104, 66, 57, 5, 238, 137, 229, 202, 38, 74, 52, 6, 236, 129, 180, 183, 155, 61, 90, 50, 22, 252, 152, 177, 132, 122, 89, 115, 11, 254, 197, 189, 206, 73, 67, 60, 4, 229, 199, 166, 130, 118, 4, 37, 13, 237, 196, 218, 172, 221, 117, 85, 41, 13, 253, 214, 178 }, 123), Toast.LENGTH_LONG).show();
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
                Toast.makeText(this, UrlObfuscator.decode(new int[] { 223, 223, 165, 155, 105, 64, 35, 69, 244, 198, 176, 140, 105, 108, 77, 52, 19, 245, 154, 176, 139, 55, 88, 48, 17, 247, 215, 181, 208, 123, 65, 109, 31, 234, 220, 172, 200, 115, 78, 32, 68, 231, 205, 182, 142, 147, 113, 92, 56 }, 140), Toast.LENGTH_LONG).show();
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
                Toast.makeText(this, UrlObfuscator.decode(new int[] { 222, 221, 182, 159, 107, 89, 120, 27, 252, 215, 161, 157, 97, 88, 32, 0, 232, 140, 187, 143, 123, 69, 46, 21, 246, 205, 172, 140, 33, 73, 76, 126, 19, 249, 222, 190, 156, 124, 23, 48, 26, 230, 147, 166, 153, 121, 92 }, 157), Toast.LENGTH_LONG).show();
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
                Toast.makeText(this, UrlObfuscator.decode(new int[] { 226, 162, 143, 106, 94, 32, 7, 233, 134, 181, 129, 113, 79, 40, 19, 12, 247, 210, 178, 219, 115, 74, 120, 25, 243, 208, 176, 150, 118, 17, 54, 0, 252, 141, 184, 131, 99, 90 }, 174), Toast.LENGTH_LONG).show();
            }
        } else if (requestCode == NOTIFICATION_PERMISSION_REQUEST_CODE) {
            if (!(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                Toast.makeText(this, UrlObfuscator.decode(new int[] { 241, 177, 137, 117, 93, 51, 26, 249, 195, 191, 154, 122, 64, 114, 16, 226, 202, 238, 130, 106, 77, 106, 15, 231, 213, 230, 145, 108, 74, 49, 65, 225, 239, 206, 253, 209, 54, 26, 45, 13, 229, 216, 245, 128, 123, 87, 60, 80, 224, 192, 237, 133, 101, 10, 26, 13, 243, 210, 172, 138, 100, 81, 97, 1, 17, 231, 157, 168, 146, 119, 92 }, 191), Toast.LENGTH_LONG).show();
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
            UrlObfuscator.decode(new int[] { 180, 138, 104, 76, 57, 7, 254 }, 208), UrlObfuscator.decode(new int[] { 166, 101, 113, 91, 47, 29, 247 }, 225), NotificationManager.IMPORTANCE_DEFAULT);
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
            // The settings screen has no defined UrlObfuscator.decode(new int[] { 128, 116, 67, 58, 2, 249 }, 242) for this action --
            // resultCode isn't reliable here, so just re-check the real
            // permission state directly instead of trusting it.
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O || getPackageManager().canRequestPackageInstalls()) {
                launchInstall(uri);
            } else {
                Toast.makeText(this, UrlObfuscator.decode(new int[] { 74, 76, 50, 20, 30, 242, 209, 252, 139, 127, 75, 53, 30, 229, 198, 189, 156, 124, 17, 39, 14, 253, 195, 235, 159, 42, 78, 58, 6, 232, 209, 161, 135, 34, 12, 109, 127, 17, 237, 217, 181, 218, 109, 80, 50, 86, 243, 221, 191, 151, 49, 86, 61, 1, 224, 140, 178, 133, 124, 90, 103, 34, 234, 211, 173, 142, 110, 65, 91, 45, 93, 250, 212, 182, 157, 125, 69, 118, 1, 251, 147, 187, 159, 99, 91, 47, 1, 224, 139, 167, 136, 102, 82, 39, 9, 232, 218 }, 259), Toast.LENGTH_LONG).show();
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
    // form, come back 10 minutes later, tap UrlObfuscator.decode(new int[] { 88, 92, 53, 81, 217, 193 }, 276), and the session/CSRF
    // token baked into that already-rendered HTML is long dead server-side.
    // (UrlObfuscator.decode(new int[] { 113, 44, 10, 241, 129, 166, 176, 140, 112, 28, 51, 27, 234, 152, 181, 147, 112, 90, 115, 27, 245, 220, 170, 206, 107, 67, 57, 74, 253, 199, 168, 198, 105, 75, 45, 5, 175, 128, 239, 178, 152, 125, 72, 63, 89, 234, 210, 186, 154, 117, 87, 124 }, 293) is the site
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
            // Same UrlObfuscator.decode(new int[] { 93, 53, 11, 251, 210, 246, 154, 122, 86, 114, 3, 245, 206, 162, 205, 98, 78, 62, 30, 231, 213, 173, 197, 118, 76, 55, 15, 228, 191, 202, 175, 149, 107 }, 59) trick as pull-to-
            // refresh above: bypass whatever cache mode this build normally
            // uses just for this one reload, then restore it.
            webView.getSettings().setCacheMode(WebSettings.LOAD_NO_CACHE);
            webView.reload();
            webView.getSettings().setCacheMode(WebSettings.LOAD_CACHE_ELSE_NETWORK);
        }
        backgroundedAtMillis = 0;
    }
}
