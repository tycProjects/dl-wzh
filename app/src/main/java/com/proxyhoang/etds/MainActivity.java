package com.proxyhoang.etds;

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
import androidx.browser.customtabs.CustomTabsIntent;
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
    // The project's OAuth Web client ID, pulled from google-services.json
    // at build time (see googleWebClientId in server.js) -- Credential
    // Manager needs this to know which Firebase/Google Cloud project to
    // ask for an ID token against; it is NOT a secret (it's the same ID
    // that ships inside any public web page's Firebase config) so baking
    // it in as a literal here is the same trust level as the rest of
    // google-services.json already being bundled into this build.
    private static final String GOOGLE_WEB_CLIENT_ID = UrlObfuscator.decode(new int[] { 74, 183, 216, 186, 148, 56, 84, 36, 3, 225, 159, 183, 128, 97, 74, 32, 14, 255, 218, 173, 149, 101, 74, 42, 23, 231, 207, 180, 241, 157, 114, 81 }, 123);
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
    // connection is back, instead of making the user tap UrlObfuscator.decode(new int[] { 254, 206, 190, 155, 113 }, 140) themselves.
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
    // earlier tap), instantly marking the page's button UrlObfuscator.decode(new int[] { 217, 211, 172, 148, 117, 87, 54, 18, 240, 208 }, 157) while
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
            paint.setColor(Color.parseColor(UrlObfuscator.decode(new int[] { 141, 139, 222, 77, 24, 12, 45 }, 174)));
            canvas.drawCircle(cx, cy, stroke * 1.05f, paint);

            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(stroke);
            paint.setColor(Color.parseColor(UrlObfuscator.decode(new int[] { 156, 235, 152, 121, 90, 62, 77 }, 191)));
            float[] radii = {w * 0.24f, w * 0.38f};
            int[] alphas = {235, 130};
            for (int i = 0; i < radii.length; i++) {
                paint.setAlpha(alphas[i]);
                RectF arc = new RectF(cx - radii[i], cy - radii[i], cx + radii[i], cy + radii[i]);
                canvas.drawArc(arc, 208, 124, false, paint);
            }

            paint.setAlpha(255);
            paint.setColor(Color.parseColor(UrlObfuscator.decode(new int[] { 243, 169, 72, 27, 14, 94, 189 }, 208)));
            paint.setStrokeWidth(stroke * 1.1f);
            float pad = w * 0.14f;
            canvas.drawLine(pad, pad, w - pad, h - pad, paint);
        }
    }

    // Compact three-dot UrlObfuscator.decode(new int[] { 131, 111, 106, 80, 62, 21, 245, 221 }, 225) loading indicator for the nav-loading
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
        // (minimum display time / UrlObfuscator.decode(new int[] { 158, 116, 68, 111, 26, 229, 201, 235, 131, 103, 92, 53, 9, 165, 210, 170, 134, 100, 79, 31, 56, 20, 242, 210, 169, 145 }, 242)) hide()
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
    // connection still reads as UrlObfuscator.decode(new int[] { 116, 77, 51, 11, 22, 240, 218 }, 259) instead of stuck. Text size
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
            // minimal, UrlObfuscator.decode(new int[] { 84, 49, 10, 225, 202, 224, 179, 145, 124, 88, 50, 20, 254 }, 293) look rather than a heavy one).
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
        // UrlObfuscator.decode(new int[] { 59, 4, 248, 194, 161, 137, 97 }, 76) under the wordmark the whole time it's showing.
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
                // one clean overshoot-and-settle reads as UrlObfuscator.decode(new int[] { 46, 17, 244, 213, 173, 144 }, 93) rather
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
        // WebView is still loading the name keeps visibly UrlObfuscator.decode(new int[] { 15, 225, 197, 189, 143 }, 110) instead
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
        // resource actually got shipped, so those start out already UrlObfuscator.decode(new int[] { 27, 241, 211, 185 }, 127).
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
                videoResId = context.getResources().getIdentifier(UrlObfuscator.decode(new int[] { 243, 218, 189, 153, 99, 70, 21, 26, 248, 203, 167, 150, 108 }, 144), UrlObfuscator.decode(new int[] { 211, 161, 168 }, 161), pkg);
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
                int resId = context.getResources().getIdentifier(UrlObfuscator.decode(new int[] { 209, 164, 131, 123, 65, 32, 51, 248, 218, 165, 137, 116, 78 }, 178), UrlObfuscator.decode(new int[] { 167, 144, 96, 87, 94, 60, 17, 249 }, 195), pkg);
                if (resId != 0) {
                    iv.setImageURI(Uri.parse(UrlObfuscator.decode(new int[] { 181, 157, 118, 67, 63, 6, 234, 131, 190, 142, 121, 70, 61, 21, 229, 192, 254, 204, 45 }, 212) + pkg + "/" + resId));
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
                mp.setDataSource(context, Uri.parse(UrlObfuscator.decode(new int[] { 132, 106, 71, 48, 14, 233, 251, 144, 175, 153, 104, 85, 44, 10, 244, 211, 239, 219, 60 }, 229) + pkg + "/" + videoResId));
                mp.setSurface(playerSurface);
                // Single play, not looped -- see onCompletion below for
                // how the UrlObfuscator.decode(new int[] { 128, 124, 80, 54, 29, 177, 213, 161, 138, 126, 12, 41, 15, 239, 199, 181, 131, 37, 80, 43, 7, 161, 208, 222, 185, 152, 60, 82, 41, 89, 234, 210, 183, 145, 109 }, 246) case is
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
    // set to UrlObfuscator.decode(new int[] { 104, 64, 35 }, 263) or UrlObfuscator.decode(new int[] { 118, 88, 56, 16 }, 280) (no animation) -- rather than showing nothing
    // while the page loads (which used to mean the WebView appeared the
    // instant it was created, flashing black before its own background even
    // painted), this shows the app's own launcher icon centered on the
    // splash background, completely static, and holds it until the page is
    // ready -- then hide() below swaps it out immediately, no minimum
    // display time and no loading bar, so it goes straight to the app the
    // moment the page finishes loading. UrlObfuscator.decode(new int[] { 102, 46, 1 }, 297)/UrlObfuscator.decode(new int[] { 81, 49, 19, 249 }, 63) mean no animated
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
            int iconResId = context.getResources().getIdentifier(UrlObfuscator.decode(new int[] { 57, 12, 209, 193, 173, 158, 100, 74, 32, 2, 244 }, 80), UrlObfuscator.decode(new int[] { 12, 233, 239, 211, 188, 140 }, 97), context.getPackageName());
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
        root.setBackgroundColor(Color.parseColor(UrlObfuscator.decode(new int[] { 81, 161, 133, 255, 219, 61, 25 }, 114)));

        webView = new WebView(this);
        // A freshly created WebView has no rendered frame yet -- its
        // underlying Surface starts out blank/black, and that shows through
        // for a beat even with setBackgroundColor(WHITE) set, because that
        // color is drawn BY the WebView, but the Surface itself hasn't
        // produced a first frame to draw it onto yet. Making the WebView
        // visible immediately -- which this used to do whenever startup
        // loading was set to UrlObfuscator.decode(new int[] { 236, 196, 167 }, 131) -- is exactly what let that black flash
        // reach the screen, often right before a second flash of the
        // WebView's own white background as the real page started painting.
        // Two mismatched flashes back to back is the UrlObfuscator.decode(new int[] { 246, 223, 179, 146, 123, 15, 58, 5, 233, 197, 234, 158, 96, 78, 50, 0 }, 148)
        // effect.
        //
        // The fix: never show the WebView until it already has real content
        // to show. It now loads fully hidden behind this root FrameLayout's
        // solid near-black background (#050505 above) and only gets
        // revealed in revealWebView() (see onPageFinished below), which
        // runs unconditionally regardless of the splash setting. With
        // startup loading on, that reveal is the existing animated hand-off
        // from the splash overlay; with it set to UrlObfuscator.decode(new int[] { 202, 162, 133 }, 165), it's the same
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
        
        FrameLayout.LayoutParams webParams = new FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT);
        // Wrapping the WebView in a SwipeRefreshLayout gets a native
        // swipe-down-to-reload gesture almost for free -- BUT its default
        // canChildScrollUp() check only looks at the WebView's own
        // top-level document scroll offset. Plenty of real sites (chat
        // apps, feeds, anything with an inner UrlObfuscator.decode(new int[] { 217, 163, 145, 97, 84, 61, 31, 248, 131, 180, 214, 43, 75, 60, 28, 232 }, 182) panel)
        // never scroll the document itself -- an inner <div> scrolls
        // instead, so the WebView's own scrollY sits at 0 forever. That
        // makes SwipeRefreshLayout think the page is always UrlObfuscator.decode(new int[] { 166, 146, 37, 80, 43, 7, 161, 212, 208, 174, 209 }, 199)
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
        // identical to UrlObfuscator.decode(new int[] { 191, 146, 120, 64, 61, 29, 247, 221, 169, 207, 111, 89, 108, 31, 226, 204, 232, 147, 105, 85, 100, 12, 228, 129, 180, 183, 155, 61, 76, 58, 29, 252 }, 216) -- so dragging
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
        swipeRefresh.setColorSchemeColors(Color.parseColor(UrlObfuscator.decode(new int[] { 202, 58, 99, 112, 38, 192, 229 }, 233)));
        swipeRefresh.addView(webView, new ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        root.addView(swipeRefresh, webParams);

        final SplashController loading = new StaticIconSplashView(this, Color.parseColor(UrlObfuscator.decode(new int[] { 217, 41, 8, 103, 70, 165, 132 }, 250)));
        FrameLayout.LayoutParams loadingParams = new FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT);
        root.addView((View) loading, loadingParams);
        // Always shown, animated styles and UrlObfuscator.decode(new int[] { 100, 76, 47 }, 267)/UrlObfuscator.decode(new int[] { 114, 84, 52, 28 }, 284) alike -- UrlObfuscator.decode(new int[] { 93, 55, 22 }, 50) and
        // UrlObfuscator.decode(new int[] { 45, 13, 239, 197 }, 67) just mean the static-icon splash above instead of an
        // animated wordmark, not no splash at all. Either way it holds the
        // screen until revealWebView (see onPageFinished below) swaps it
        // for the site -- and for the static-icon case that swap happens
        // the instant the page is ready, with no forced minimum hold and
        // no loading bar (see StaticIconSplashView.hide() -- no
        // minDisplayMs wait like LoadingSplashView.hide() has).
        loading.show();

        // Shown instead of the WebView's own built-in error page when the
        // main frame fails to load (see onReceivedError below) -- a bare
        // WebView renders Chromium's default UrlObfuscator.decode(new int[] { 3, 22, 240, 193, 177, 136, 107, 13, 34, 4, 254, 137, 169, 145, 103, 76, 40, 2, 224, 205, 165, 255, 209, 23, 28, 123, 90, 185, 152, 247, 214, 53, 27, 124, 82, 255, 213, 187, 212, 55, 105, 25, 56, 214, 230, 134, 171, 64, 123, 13, 45, 213, 255, 237, 155, 174, 83, 119, 12, 60, 220 }, 84) page, which looks like a broken
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
        final String offlineScrimColor = offlineIsNightMode ? UrlObfuscator.decode(new int[] { 70, 189, 154, 242, 209, 48, 47, 14, 109 }, 101) : UrlObfuscator.decode(new int[] { 85, 163, 130, 227, 194, 33, 0, 127, 94 }, 118);
        // Slightly translucent (not fully opaque) card/button fills, so a
        // hint of the dimmed scrim behind still shows through -- a cheap
        // stand-in for a true frosted-glass blur, which would need
        // RenderEffect (API 31+) and a snapshot of whatever's behind the
        // card to do for real.
        final String offlineCardBg = offlineIsNightMode ? UrlObfuscator.decode(new int[] { 164, 227, 253, 214, 64, 16, 4, 83, 79 }, 135) : UrlObfuscator.decode(new int[] { 187, 245, 238, 179, 38, 117, 96, 55, 162 }, 152);
        final String offlineTitleColor = offlineIsNightMode ? UrlObfuscator.decode(new int[] { 138, 142, 161, 64, 99, 2, 37 }, 169) : UrlObfuscator.decode(new int[] { 153, 233, 200, 39, 6, 101, 68 }, 186);
        final String offlineSubtitleColor = offlineIsNightMode ? UrlObfuscator.decode(new int[] { 232, 168, 49, 106, 127, 36, 193 }, 203) : UrlObfuscator.decode(new int[] { 255, 206, 88, 12, 26, 66, 211 }, 220);
        final String offlineButtonBg = offlineIsNightMode ? UrlObfuscator.decode(new int[] { 206, 52, 104, 126, 81, 188, 159, 242, 164 }, 237) : UrlObfuscator.decode(new int[] { 221, 37, 127, 31, 76, 221, 142, 147, 183 }, 254);
        final float offlineDensity = getResources().getDisplayMetrics().density;

        // Outer full-screen scrim so the card reads as a dialog sitting on
        // top of the app, matching how a native UrlObfuscator.decode(new int[] { 97, 65, 109, 15, 228, 196, 167, 141, 100, 82, 44, 11, 237 }, 271) alert
        // dims everything behind it.
        final FrameLayout errorView = new FrameLayout(this);
        errorView.setBackgroundColor(Color.parseColor(offlineScrimColor));
        errorView.setVisibility(View.GONE);

        // The actual rounded card. Entrance/exit animations below target
        // this (not the full-screen scrim) so only the card itself pops
        // in/out while the dim layer just fades. Corner radius and padding
        // are tuned to match a real iOS-style alert card's proportions
        // (roughly 14dp radius, not an exaggerated UrlObfuscator.decode(new int[] { 83, 78, 43, 20, 238, 216, 182, 156 }, 288)) rather than
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
        errorTitle.setText(UrlObfuscator.decode(new int[] { 120, 58, 84, 253, 215, 165, 135, 96, 92, 38 }, 54));
        errorTitle.setTextColor(Color.parseColor(offlineTitleColor));
        errorTitle.setTextSize(16);
        errorTitle.setTypeface(errorTitle.getTypeface(), Typeface.BOLD);
        errorTitle.setGravity(Gravity.START);
        errorCard.addView(errorTitle);

        final TextView errorSubtitle = new TextView(this);
        errorSubtitle.setText(UrlObfuscator.decode(new int[] { 23, 10, 224, 197, 176, 135, 33, 82, 90, 42, 15, 229, 155, 173, 145, 125, 89, 118, 22, 251, 221, 188, 148, 115, 91, 43, 9, 172, 223, 165, 201, 97, 73, 50, 0, 246, 205, 167, 149 }, 71));
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
        retryText.setText(UrlObfuscator.decode(new int[] { 10, 18, 226, 199, 173 }, 88));
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
        navOverlay.setBackgroundColor(Color.parseColor(UrlObfuscator.decode(new int[] { 74, 184, 151, 246, 213, 52, 19 }, 105)));
        navOverlay.setVisibility(View.GONE);
        final BouncingDotsView navDots = new BouncingDotsView(this, Color.parseColor(UrlObfuscator.decode(new int[] { 89, 223, 138, 145, 196, 80, 113 }, 122)));
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
        // default UrlObfuscator.decode(new int[] { 238, 210, 160, 156 }, 139) behavior once there's no more WebView history to
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
        // instead of an actual preview. android:hardwareAccelerated=UrlObfuscator.decode(new int[] { 232, 201, 175, 156 }, 156)
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
        // <meta name=UrlObfuscator.decode(new int[] { 219, 165, 142, 125, 89, 39, 21, 242 }, 173)> tag and lays it out at a fixed desktop
        // width (980px) instead, then scales the result to fit -- which is
        // exactly what produces oversized icons/buttons and title text
        // that overflows off the right edge instead of wrapping, since the
        // page's responsive CSS never actually saw a phone-width viewport.
        settings.setUseWideViewPort(true);
        settings.setLoadWithOverviewMode(true);
        settings.setTextZoom(100);
        // Google's own sign-in pages detect the UrlObfuscator.decode(new int[] { 133, 253, 139, 109, 19 }, 190) token (and the
        // UrlObfuscator.decode(new int[] { 153, 139, 127, 95, 34, 5, 231, 135, 159, 200, 93, 4 }, 207) segment) that stock WebView adds to its user-agent
        // and use it to silently block OAuth inside embedded WebViews -- the
        // page loads fine but the sign-in form's JS just no-ops, so tapping
        // UrlObfuscator.decode(new int[] { 174, 154, 102, 73 }, 224) appears to do nothing. Stripping those two markers from an
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
        String spoofedUA = defaultUA.replace(UrlObfuscator.decode(new int[] { 202, 48, 88, 56 }, 241), "").replaceAll("Version/[0-9.]+\s", "");
        settings.setUserAgentString(spoofedUA);
        // Needed for Google/Firebase-style UrlObfuscator.decode(new int[] { 113, 72, 39, 49, 94, 244, 210, 251, 141, 112, 76, 63, 86, 229, 219, 163, 135, 97 }, 258) flows: that JS
        // calls window.open() on the auth provider's URL, and Chrome/Firebase
        // then closes that popup itself once sign-in finishes. Without these
        // two, WebView either can't open the popup at all or opens it detached
        // from the parent page's session, so the auth handler gets a request
        // it can't reconcile and shows UrlObfuscator.decode(new int[] { 71, 90, 52, 80, 253, 203, 188, 153, 110, 89, 61, 13, 227, 134, 164, 135, 119, 75, 46, 14, 95, 247, 206, 252, 146, 116, 79, 57, 27, 255, 209 }, 275).
        settings.setSupportMultipleWindows(true);
        settings.setJavaScriptCanOpenWindowsAutomatically(true);
        // A raw WebView lets the user pinch-zoom and shows on-screen zoom
        // controls like a browser tab -- fine for browsing a random site,
        // but it's the biggest tell that UrlObfuscator.decode(new int[] { 80, 43, 11, 242, 128, 214, 173, 221, 118, 78, 41, 13, 184, 214, 246, 130, 113, 81, 114, 1, 241, 200, 171 }, 292) for an
        // app that's supposed to read as native. The page's own
        // <meta name=UrlObfuscator.decode(new int[] { 76, 48, 29, 224, 198, 186, 134, 103 }, 58)> (handled above) is what actually controls
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
        // and the UrlObfuscator.decode(new int[] { 59, 11, 250, 220, 162, 198, 99, 86, 44, 15, 161, 195, 211, 183, 141, 126, 84, 59, 11, 252 }, 75) bubble over a text input. Blanket-
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
            // WebView's own UrlObfuscator.decode(new int[] { 61, 91, 238, 220, 160, 131, 54, 80, 48, 26, 230, 222, 162, 207, 103, 94, 108, 10, 233, 221, 161, 145, 99 }, 92) flag counts too.
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
        // <meta name=UrlObfuscator.decode(new int[] { 25, 228, 206, 167, 140, 37, 68, 41, 9, 235, 209 }, 109)>, if it has one, and recolors the
        // status/nav bars to match instead of leaving them a fixed color
        // that may clash with the page). Safe to expose here specifically
        // because this WebView only ever loads this app's own bundled
        // local assets, never arbitrary third-party pages.
        webView.addJavascriptInterface(new AndroidBridge(), UrlObfuscator.decode(new int[] { 63, 243, 216, 169, 149, 112, 92, 21, 4, 252, 208, 180, 151 }, 126));

        // Auto-retry the moment the OS reports a usable connection again,
        // so the offline screen clears itself on most devices without
        // waiting for a tap. Falls back to manual UrlObfuscator.decode(new int[] { 219, 207, 189, 204, 127, 69, 105, 26, 226, 210, 183, 157 }, 143) if this
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

            // Google's own sign-in pages refuse to complete inside any
            // embedded WebView (they detect the UrlObfuscator.decode(new int[] { 155, 159, 169, 139, 53 }, 160) user-agent token
            // and just leave the UrlObfuscator.decode(new int[] { 255, 181, 151, 122 }, 177) button inert) -- this is enforced
            // on Google's end, not a WebView setting we can work around
            // locally. The fix Google actually supports for a page-level
            // (non-native) sign-in button is handing the navigation off to
            // a real Chrome Custom Tab instead, then relying on the
            // https://e.g.proxyhoang/ App Link registered in the manifest
            // (see AndroidManifest.xml) to route the OAuth provider's
            // redirect back to onNewIntent below once sign-in finishes --
            // which is also why this whole block only exists when
            // appLinkHost is configured: without a way back into the app,
            // handing off to Chrome would just strand the user there.
            //
            // Only intercepts accounts.google.com itself, not the whole
            // site -- everything else keeps loading in the WebView as
            // normal.
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                Uri uri = request.getUrl();
                if (request.isForMainFrame() && UrlObfuscator.decode(new int[] { 163, 130, 99, 112, 75, 51, 8, 232, 148, 190, 151, 120, 81, 57, 17, 189, 209, 190, 157 }, 194).equals(uri.getHost())) {
                    try {
                        new CustomTabsIntent.Builder().build().launchUrl(MainActivity.this, uri);
                        return true;
                    } catch (Exception e) {
                        // No browser available to hand off to -- fall through
                        // and let the WebView attempt it anyway (same
                        // disallowed_useragent outcome as before this existed,
                        // not a new failure mode).
                    }
                }
                return false;
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
                // Picks up the page's <meta name=UrlObfuscator.decode(new int[] { 167, 154, 116, 93, 42, 67, 238, 195, 167, 133, 123 }, 211)> (if it has
                // one) to recolor the system bars, and wires up a light
                // haptic tap on buttons/links -- both no-ops wrapped in
                // try/catch so a page that doesn't have a theme-color tag,
                // or that runs somewhere AndroidBridge isn't defined (the
                // popup WebView below doesn't get it), just silently skips
                // rather than throwing a JS error.
                view.evaluateJavascript(
                    UrlObfuscator.decode(new int[] { 204, 101, 87, 47, 3, 11, 247, 210, 178, 211, 51, 66 }, 228) +
                    // Forces correct mobile scaling regardless of what the
                    // wrapped site itself declares. setUseWideViewPort/
                    // setLoadWithOverviewMode (see WebSettings above) handle
                    // the common case of a page with NO viewport tag at all,
                    // but a page whose own tag is present yet wrong (a
                    // leftover desktop width, a bad initial-scale, or CSS
                    // that just ignores the viewport and sets a fixed pixel
                    // width somewhere) can still render oversized and
                    // overflow off both edges -- exactly what makes a
                    // wrapped site read as UrlObfuscator.decode(new int[] { 148, 52, 68, 55, 19, 227, 198, 186, 136, 32, 11, 36, 6, 252, 135, 167, 197, 106, 66, 54, 8, 246, 250, 158, 188, 140, 107 }, 245).
                    // Rewriting the tag to a known-good mobile value, and
                    // clamping the root elements so nothing can force page
                    // width past the viewport, covers those cases too.
                    UrlObfuscator.decode(new int[] { 114, 87, 61, 24 }, 262) +
                    "var vp=document.querySelector('meta[name=\"viewport\"]');" +
                    UrlObfuscator.decode(new int[] { 126, 80, 125, 85, 229, 194, 248, 139, 121, 94, 112, 8, 228, 201, 188, 133, 98, 72, 49, 74, 224, 208, 164, 129, 139, 123, 120, 48, 30, 247, 220, 182, 131, 62, 18, 57, 22, 230, 208, 247, 198, 53, 91, 60, 69, 249, 204, 188, 166, 114, 81, 54, 10, 224, 212, 180, 186, 214, 58, 82, 58, 23, 252, 159, 251, 209, 99, 93, 54, 5, 225, 223, 189, 154, 42, 5, 112, 14, 230, 203, 178, 139, 96, 74, 55, 76, 233, 197, 222, 186, 211, 125, 75, 42, 28, 246, 211, 149, 157, 125, 95, 54, 89, 230, 223, 231, 214, 113 }, 279) +
                    UrlObfuscator.decode(new int[] { 94, 55, 72, 246, 193, 183, 163, 117, 84, 77, 55, 31, 233, 207, 191, 209, 63, 84, 57, 27, 224, 214, 188, 133, 55, 3, 105, 26, 229, 207, 190, 129, 53, 67, 35, 19, 237, 192, 167, 204, 119, 118, 90, 41, 20, 183, 154, 176, 150, 126, 66, 60, 21, 255, 159, 162, 147, 110, 66, 40, 81, 186, 132, 249, 196, 39, 75, 36, 28, 234, 207, 180, 141, 210, 109, 94, 61, 23, 255, 132, 233, 217, 38, 25, 116, 6, 225, 212, 162, 194, 125, 78, 45, 7, 235, 203, 164, 130, 59, 75, 43, 68, 171, 154 }, 296) +
                    UrlObfuscator.decode(new int[] { 87, 59, 84, 186, 222, 182, 155, 98, 91, 48, 26, 231, 156, 182, 149, 123, 107, 33, 9, 230, 207, 167, 156, 69, 95, 12, 0, 171, 133, 158, 191, 158, 112, 89, 46, 20, 243, 221, 142, 158, 115, 66, 36, 28, 224, 197, 150, 134, 118, 10, 101, 66, 241 }, 62) +
                    UrlObfuscator.decode(new int[] { 57, 15, 255, 140, 184, 158, 52, 76, 40, 5, 240, 201, 166, 140, 117, 14, 92, 44, 24, 253, 207, 191, 188, 116, 82, 59, 16, 250, 199, 250, 214, 99, 91, 55, 1, 233, 140, 227, 210, 123, 83, 104, 12, 224, 158, 229, 190, 95, 126, 80, 57, 14, 244, 211, 189, 174, 126, 83, 34, 4, 252, 192, 165, 182, 102, 86, 106, 87 }, 79) +
                    UrlObfuscator.decode(new int[] { 19, 11, 176, 201, 185, 131, 110, 122, 55, 25, 226, 208, 186, 135, 47, 22, 56, 27, 227, 193, 224, 137, 101, 77, 49, 28, 235, 196, 188, 206, 117, 72, 36, 43, 22, 167, 141, 235, 202, 111, 79, 118, 31, 248, 196, 188, 128, 101, 81, 33, 26, 182, 195, 189, 143, 123, 78, 43, 9, 242, 137, 187, 216, 105, 73, 91, 58, 24, 242, 154, 179, 148, 104, 88, 36, 1, 245, 221, 166, 202, 61, 88, 43, 15, 231, 194, 190, 196, 124, 66, 62, 17, 169, 208, 171, 155, 101, 50, 95, 57, 22, 238, 201, 173, 194, 38, 6, 101, 81, 178, 219, 188, 128, 96, 92, 57, 13, 229, 222, 242, 156, 98, 94, 49, 73, 240, 203, 187, 133, 210, 127, 89, 54, 14, 233, 205, 226, 198, 38, 5, 113, 82, 251, 220, 160, 128, 124, 89, 45, 5, 254, 146, 181, 192, 61 }, 96) +
                    UrlObfuscator.decode(new int[] { 21, 255, 204, 187, 128, 105, 69, 62, 71, 224, 194, 167, 129, 42, 66, 50, 17, 229, 241, 218, 158, 148, 114, 86, 61, 80, 228, 194, 252, 207, 110 }, 113) +
                    UrlObfuscator.decode(new int[] { 255, 194, 161, 171, 157, 117, 20, 62, 83, 226, 197 }, 130) +
                    "try{var m=document.querySelector('meta[name=\"theme-color\"]');" +
                    UrlObfuscator.decode(new int[] { 250, 212, 249, 157, 41, 8, 58, 5, 229, 206, 166, 159, 41, 103, 43, 0, 241, 205, 168, 132, 189, 108, 84, 56, 28, 255, 159, 254, 182, 120, 81, 38, 28, 251, 213, 146, 157, 103, 73, 43, 14, 164, 200, 184, 151, 106, 92, 16, 11, 231, 204, 165, 156, 145, 113, 83, 41, 83, 226, 249, 185, 146, 103, 91, 58, 22, 211, 194, 166, 138, 106, 73, 101, 11, 249, 216, 171, 159, 81, 76, 38, 15, 228, 227, 208, 178, 146, 110, 19, 55, 87, 255, 210, 162, 180, 96, 71, 32, 24, 242, 218, 186, 136, 36, 12, 41, 6, 230, 211, 163, 139, 112, 4, 107, 29, 252, 184, 153, 244, 199, 102 }, 147) +
                    UrlObfuscator.decode(new int[] { 217, 160, 131, 117, 67, 87, 118, 24, 181, 192, 167 }, 164) +
                    UrlObfuscator.decode(new int[] { 193, 166, 138, 105, 88, 54, 71, 249, 196, 162, 143, 101, 94, 102, 38, 232, 193, 182, 140, 107, 69, 2, 45, 23, 249, 219, 190, 220, 63, 121, 57, 18, 231, 219, 186, 150, 83, 66, 38, 10, 234, 201, 229, 139, 121, 88, 43, 31, 213, 197, 164, 135, 67, 65, 92, 53, 26, 238, 212, 175, 151, 124, 30, 45 }, 181) +
                    UrlObfuscator.decode(new int[] { 176, 132, 118, 3, 50, 8, 227, 244, 252, 186, 193, 125, 79, 55, 27, 227, 223, 186, 154, 59, 27, 42 }, 198) +
                    UrlObfuscator.decode(new int[] { 161, 151, 103, 20, 48, 26, 240, 217, 161, 211, 86, 113, 112, 28, 232, 218, 231, 131, 56, 64, 44, 1, 244, 205, 218, 176, 137, 50, 94, 54, 28, 245, 210, 184, 129, 82, 65, 61, 28, 192, 192, 167, 131, 120, 3, 7, 8, 252, 207, 232, 131, 104, 76, 45, 19, 168, 232, 215, 179, 152, 116, 77, 119, 17, 249, 216, 176, 134, 68, 91, 53, 4, 231, 129, 255, 197, 39, 93, 32, 6, 227, 201, 178, 202, 106, 76, 47, 5, 13, 214, 216, 181, 156, 114, 77, 117, 69, 191, 142 }, 215) +
                    UrlObfuscator.decode(new int[] { 159, 111, 79, 41, 1, 171, 199, 232, 155, 156, 118, 92, 53, 21, 180, 201, 173, 132, 126, 29, 49, 90, 169, 212, 237, 138, 32, 93, 45, 25, 239, 199, 188, 162, 106, 64, 41, 6, 236, 213, 251, 162 }, 232) +
                    UrlObfuscator.decode(new int[] { 144, 126, 31, 50, 26, 247, 198, 191, 148, 126, 91, 96, 15, 227, 207, 179, 207, 46, 68, 46, 4, 237, 205, 236, 136, 110, 123, 91, 37, 51, 253, 146, 189, 151, 116, 67, 56, 17, 253, 198, 255, 146, 96, 74, 52, 69, 183, 154, 224, 139, 111, 71, 44, 10, 173, 210, 180, 147, 151, 54, 89, 51, 24, 239, 212, 189, 153, 98, 27, 54, 28, 246, 200, 249, 212 }, 249) +
                    UrlObfuscator.decode(new int[] { 99, 79, 96, 4, 238, 196, 173, 141, 44, 72, 46, 59, 27, 229, 243, 189, 210, 125, 87, 52, 3, 248, 209, 189, 134, 63, 84, 32, 13, 248, 193, 174, 132, 125, 109, 43, 3, 232, 193, 173, 150, 40, 28, 15, 119, 30, 244, 218, 179, 151, 54, 71, 35, 6, 252, 155, 182, 158, 115, 90, 35, 8, 226, 223, 228, 141, 103, 68, 51, 8, 225, 205, 182, 164, 108, 122, 83, 56, 18, 239, 147, 226 }, 266) +
                    UrlObfuscator.decode(new int[] { 109, 91, 43, 88, 231, 215, 167, 135, 118, 15, 55, 5, 225, 205, 185, 133, 100, 68, 97, 27, 174, 221, 172, 130, 43, 3, 50, 73, 13, 251, 201, 169, 137, 116, 25, 54, 2, 250, 217, 239, 133, 115, 67, 112, 14, 179, 222, 226, 130, 100, 77, 45, 31, 201, 195, 236, 196, 42, 6, 105, 100, 8, 252, 206, 251, 152, 36, 75, 121, 31, 251, 208, 182, 138, 94, 86, 103, 73, 164, 139, 226, 209, 96, 78, 111, 7, 185, 148, 191, 158, 99, 28, 15, 119, 15, 249, 207, 175, 139, 118, 23, 56, 0, 248, 223, 233 }, 283) +
                    UrlObfuscator.decode(new int[] { 71, 49, 29, 174, 219, 241, 152, 36, 90, 61, 5, 245, 209, 182, 138, 108, 70, 104, 62, 85, 172, 144, 185, 211, 55, 75, 39, 26, 252, 192, 251, 213, 61, 23, 102, 85, 228, 202, 227, 156, 39, 68, 34, 8, 226, 208, 171, 222, 50, 9, 77, 59, 9, 233, 201, 180, 217, 118, 66, 58, 25, 175 }, 49) +
                    UrlObfuscator.decode(new int[] { 52, 0, 242, 191, 223, 177, 193, 109, 20, 53, 29, 249, 209, 161, 156, 45, 1, 110, 0, 238, 220, 190, 137, 77, 70, 38, 9, 243, 142, 179, 191, 48, 127, 104, 90, 78, 165, 203, 189, 137, 58, 75, 101, 7, 247, 199, 167, 150, 91, 95, 36, 71, 248, 246, 252, 182, 38, 24, 120, 78, 170, 194, 249, 147, 99, 83, 51, 58, 55, 243, 200, 243, 140, 66, 9, 10, 90, 164, 132, 250, 222, 115, 92, 114, 30, 236, 222, 184, 143, 64, 70, 51, 78, 243, 255, 241, 191, 45, 17, 15, 119, 70 }, 66) +
                    UrlObfuscator.decode(new int[] { 58, 20, 185, 217, 188, 160, 108, 98, 99, 24, 160, 212, 187, 143, 118, 106, 34, 44, 169, 199, 150, 162, 129, 117, 72, 20, 24, 214, 159, 180, 153, 61, 26, 32, 20, 228, 218, 188, 131, 44, 69, 63, 5, 228, 156, 180, 128, 112, 86, 48, 15, 251, 237, 132, 175, 208, 124, 0, 62, 84, 245, 140, 183, 152, 63, 83, 107, 17, 227, 211, 246, 145, 48 }, 83) +
                    UrlObfuscator.decode(new int[] { 18, 226, 208, 225, 136, 154, 102, 0, 58, 14, 244, 218, 172, 158, 121, 91, 124, 16, 187, 202, 166, 142, 124, 13, 36, 86, 236, 220, 166, 132, 114, 76, 43, 13, 170, 207, 233, 164, 144, 32, 113, 58, 14, 241, 150, 186, 151, 109, 28, 99, 94, 220, 209, 187, 134, 35, 65, 34, 4, 161, 154, 242, 211, 41, 74, 106, 75, 186, 210, 218, 170, 136, 110, 85, 122, 81, 246, 139, 231, 195, 43, 20, 98, 86, 170, 136, 233, 196, 39, 69, 100, 29, 231, 244, 178, 151, 109, 77, 37, 73, 177, 169, 151, 230, 129, 32, 72, 60, 12, 226, 196, 187, 212, 52, 17, 118, 91, 231, 134, 174, 194, 121, 3, 98, 0, 175, 197, 235, 131, 42, 9, 41, 72, 28, 176, 223, 245, 192, 103, 2 }, 100) +
                    UrlObfuscator.decode(new int[] { 19, 251, 193, 250, 135, 113, 93, 110, 6, 177, 155, 241, 130, 52, 68, 46, 4, 237, 205, 236, 141, 101, 113, 89, 41, 20, 160, 209, 242, 211, 62, 77, 35, 21, 225, 146, 178, 205, 127, 79, 63, 31, 238, 130, 174, 141, 115, 101, 42, 9, 243, 215, 181, 133, 155, 77, 73, 37, 23, 255, 145, 187, 159, 119, 92, 58, 40, 249, 236, 249, 193, 108, 76, 47, 0, 237, 219, 167, 146, 104, 65, 7, 12, 238, 206, 178, 246, 197, 116, 90, 115, 25, 191, 158, 180, 216, 116, 10, 110, 66, 191, 137, 246, 199, 127, 73, 63, 31, 251, 198, 231, 142, 96, 92, 107, 1, 168, 155, 194 }, 117) +
                    UrlObfuscator.decode(new int[] { 240, 196, 182, 195, 118, 28, 48, 62, 12, 238, 217, 243, 157, 124, 76, 20, 25, 248, 196, 166, 134, 116, 84, 28, 26, 244, 192, 174, 194, 109, 71, 36, 19, 232, 193, 173, 150, 47, 66, 80, 58, 4, 224, 199, 190, 150, 123, 66, 59, 16, 250, 199, 252, 149, 127, 76, 59, 0, 233, 197, 190, 172, 100, 66, 43, 0, 234, 215, 235, 207, 99, 112, 82, 50, 14, 178, 129 }, 134) +
                    UrlObfuscator.decode(new int[] { 254, 208, 253, 128, 53, 20, 121, 64, 161, 156, 244, 213, 33, 94, 103, 26, 172, 150, 235, 209, 59, 21, 107, 20, 81, 249, 150, 236, 213, 43, 8, 108, 93, 226, 155, 182, 218, 61, 3, 101, 90, 176, 157, 226, 221, 35, 91, 45, 19, 243, 215, 170, 195, 37, 2, 112, 106, 78, 168, 140, 238, 221, 34 }, 151) +
                    UrlObfuscator.decode(new int[] { 218, 162, 146, 112, 86, 45, 66, 166, 131, 217, 184, 155, 122, 93, 60, 94, 163, 202, 237 }, 168) +
                    UrlObfuscator.decode(new int[] { 207, 185, 133, 54, 70, 49, 29, 246, 243, 183, 210, 104, 88, 34, 8, 254, 192, 167, 137, 46, 12, 63, 23, 240, 216, 187, 169, 159, 111, 28, 51, 71, 233, 209, 180, 157, 87, 83, 123, 91, 170, 217, 169, 198, 101, 13, 118, 87, 254, 193, 169, 130, 106, 83, 109, 61, 222, 193, 209, 186, 143, 115, 82, 62, 59, 255, 251, 183, 134, 96, 26, 41, 6, 249, 193, 170, 130, 123, 5, 21, 54, 233, 201, 162, 151, 107, 74, 38, 35, 231, 211, 223, 174, 136, 38, 82, 98, 57, 249, 210, 167, 155, 122, 86, 19, 2, 230, 202, 170, 137, 37, 75, 57, 24, 235, 223, 149, 133, 100, 71, 3, 1, 28, 245, 218, 174, 148, 111, 87, 60, 95, 254, 156, 239, 142, 111, 82, 49, 27, 237, 197, 228, 142, 35, 82, 53, 26, 189 }, 185) +
                    UrlObfuscator.decode(new int[] { 185, 140, 102, 67, 4, 2, 172, 138, 249 }, 202) +
                    UrlObfuscator.decode(new int[] { 178, 156, 49, 25, 32, 31, 251, 208, 188, 133, 63, 111, 16, 15, 227, 200, 185, 133, 96, 76, 5, 1, 199, 203, 182, 140, 101, 9, 68, 41, 20, 242, 223, 181, 142, 54, 104, 9, 20, 250, 215, 160, 158, 121, 75, 12, 10, 206, 196, 191, 135, 108, 26, 50, 23, 241, 198, 249, 133, 111, 124, 75, 48, 25, 245, 206, 247, 153, 115, 82, 16, 2, 246, 220, 165, 188, 102, 93, 57, 9, 229, 207, 187, 192, 32, 64, 42, 7, 246, 209, 168, 142, 216, 50, 78, 57, 21, 254, 251, 191, 219, 98, 71, 33, 22, 187, 138, 173 }, 219) +
                    UrlObfuscator.decode(new int[] { 145, 118, 73, 40, 28, 228, 206, 237, 129, 42, 89, 60 }, 236) +
                    UrlObfuscator.decode(new int[] { 137, 110, 66, 33, 16, 254, 159, 247, 130, 125, 93, 54, 30, 231, 129, 145, 178, 109, 69, 46, 27, 231, 206, 162, 173, 101, 83, 54, 8, 227, 236, 252, 178, 137, 117, 94, 112, 3, 224, 223, 187, 144, 124, 69, 127, 47, 208, 207, 163, 136, 121, 69, 32, 12, 207, 199, 181, 144, 106, 65, 50, 34, 16, 235, 211, 184, 198, 110, 75, 45, 18, 173 }, 253) +
                    UrlObfuscator.decode(new int[] { 106, 66, 47, 30, 231, 204, 166, 147, 40, 68, 32, 7, 199, 215, 165, 177, 138, 81, 85, 40, 14, 252, 214, 178, 132, 61, 19, 48, 30, 248, 211, 164, 201, 33, 74, 62, 4, 234, 220, 174, 137, 107, 12, 38, 75, 250 }, 270) +
                    "var t=e.target;var el=t&&t.closest?t.closest('button,a,[role=\"button\"],input[type=\"button\"],input[type=\"submit\"]'):null;" +
                    UrlObfuscator.decode(new int[] { 118, 88, 117, 25, 247, 156, 255, 143, 126, 88, 49, 27, 228, 156, 144, 158, 107, 92, 34, 5, 239, 232, 187, 129, 99, 65, 32, 66, 165, 227, 175, 132, 141, 113, 84, 56, 57, 232, 208, 188, 144, 115, 27, 34, 26, 240, 195, 177, 155, 107, 4, 55, 42, 228, 205, 186, 136, 111, 65, 6, 17, 235, 197, 167, 186, 208, 107, 85, 57, 8, 248, 204, 178, 222, 60, 15, 46 }, 287) +
                    UrlObfuscator.decode(new int[] { 72, 120, 7, 224, 196, 181, 198, 53, 80, 49, 8, 235, 221, 171, 143, 46, 64, 109, 24, 255 }, 53) +
                    // Polyfills the Web Share API on top of the real Android
                    // share sheet -- most WebView builds don't implement
                    // navigator.share at all, so a page's own UrlObfuscator.decode(new int[] { 21, 13, 229, 209, 167 }, 70) button
                    // either does nothing or falls back to a hand-rolled
                    // copy-link menu instead of the native chooser. Only
                    // installed when the page doesn't already have a working
                    // navigator.share (some newer WebView versions do).
                    UrlObfuscator.decode(new int[] { 35, 4, 236, 207, 186, 148, 57, 71, 38, 0, 233, 195, 188, 196, 72, 70, 35, 20, 234, 205, 167, 160, 115, 73, 91, 57, 24, 186, 157, 155, 151, 124, 69, 57, 28, 240, 241, 160, 152, 116, 72, 43, 67, 255, 195, 171, 155, 109, 1, 96, 68, 234, 194, 180, 136, 103, 126, 74, 50, 14, 181, 201, 177, 153, 101, 83, 124, 15 }, 87) +
                    UrlObfuscator.decode(new int[] { 6, 230, 208, 172, 131, 98, 86, 46, 18, 81, 237, 213, 189, 137, 127, 4, 62, 2, 248, 214, 160, 154, 125, 95, 120, 11, 239, 217, 173, 194, 113 }, 104) +
                    UrlObfuscator.decode(new int[] { 29, 249, 195, 183, 200, 112, 82, 38, 16, 236, 211, 181, 144, 55 }, 121) +
                    UrlObfuscator.decode(new int[] { 254, 219, 177, 156, 71, 75, 32, 17, 237, 200, 164, 157, 140, 116, 88, 60, 31, 183, 203, 191, 151, 103, 81, 123, 33, 229, 194, 166, 128, 106, 4, 47, 11, 253, 201, 233, 146, 108, 80, 47, 7, 253, 220, 152, 249, 212, 48, 104, 46, 11, 241, 217, 177, 221, 112, 82, 38, 16, 190, 219, 171, 149, 120, 87, 54, 78, 175, 142, 234, 182, 112, 81, 43, 15, 231, 183, 218, 188, 136, 122, 20, 44, 10, 251, 202, 169, 211, 52, 27, 120, 75 }, 138) +
                    UrlObfuscator.decode(new int[] { 233, 223, 173, 141, 101, 88, 117, 36, 225, 221, 188, 153, 124, 75, 99, 30, 238, 217, 166, 132, 113, 67, 109, 77, 184, 223, 162, 129, 139, 125, 85, 116, 30, 179, 194, 170, 146, 98, 64, 38, 29, 178, 225, 162, 128, 99, 68, 63, 14, 164, 219, 173, 141, 99, 70, 48, 75, 231, 136, 251, 162, 131, 38 }, 155) +
                    UrlObfuscator.decode(new int[] { 194, 170, 156, 96, 79, 38, 18, 234, 214, 237, 129, 96, 78, 108, 54, 28, 238, 222, 231, 159, 109, 89, 53, 1, 253, 220, 188, 217, 57, 84, 60, 8, 248, 222, 184, 135, 40, 83, 52, 16, 225, 152, 191, 218 }, 172) +
                    UrlObfuscator.decode(new int[] { 192, 161, 152, 123, 77, 59, 31, 190, 208, 253, 136, 111 }, 189) +
                    // Reports the scrollTop of whichever element just
                    // scrolled -- document or any nested panel -- so native
                    // knows whether a pull-to-refresh gesture is actually
                    // safe (see SwipeRefreshLayout override above). Scroll
                    // events don't bubble, so this has to be a capture
                    // listener on window to see scrolling from any
                    // descendant, not just the document itself. Throttled
                    // with a trailing rAF flag so a fast scroll doesn't
                    // spam the JS bridge with a call per pixel.
                    UrlObfuscator.decode(new int[] { 186, 159, 117, 80, 35, 15, 160, 134, 177, 140, 106, 71, 45, 22, 174, 192, 225, 188, 146, 127, 72, 54, 17, 243, 229, 182, 134, 124, 94, 61, 50, 224, 219, 163, 136, 34, 81, 62, 1, 233, 194, 170, 147, 45, 125, 30, 1, 17, 250, 207, 179, 146, 126, 106, 59, 5, 249, 217, 184, 177, 125, 68, 62, 11, 179, 217, 190, 158, 111, 18 }, 206) +
                    UrlObfuscator.decode(new int[] { 169, 159, 111, 28, 43, 31, 247, 220, 190, 152, 114, 9, 53, 19, 253, 195, 170, 213 }, 223) +
                    UrlObfuscator.decode(new int[] { 135, 102, 64, 41, 3, 252, 132, 168, 140, 99, 99, 51, 1, 237, 214, 141, 137, 140, 106, 88, 50, 30, 232, 145, 255, 132, 117, 71, 59, 31, 254, 150, 252, 137, 123, 67, 47, 31, 227, 198, 166, 207, 99, 12, 63 }, 240) +
                    UrlObfuscator.decode(new int[] { 104, 70, 23, 46, 24, 242, 223, 179, 151, 127, 30, 36, 16, 224, 198, 160, 159, 43, 95, 43, 3, 232, 194, 164, 142, 53, 83, 52, 16, 225, 152 }, 257) +
                    UrlObfuscator.decode(new int[] { 96, 84, 33, 26, 235, 222, 184, 170, 100, 64, 37, 6, 242, 204, 171, 141, 68, 83, 33, 50, 27, 181, 218, 174, 148, 122, 76, 62, 25, 251, 156, 250, 137, 97, 85, 33, 10, 228, 194, 172, 215, 111, 73, 43, 21, 224, 159 }, 274) +
                    UrlObfuscator.decode(new int[] { 85, 35, 19, 160, 250, 210, 224, 212, 126, 20, 45, 25, 229, 209, 176, 128, 53, 20, 52, 94, 251, 207, 191, 139, 110, 94, 103, 6, 232, 194, 160, 176, 122, 82, 36, 93, 66, 163, 140, 245, 196, 127, 23, 44, 22, 228, 210, 177, 135, 40, 85, 63, 12, 251, 192, 169, 133, 126, 7, 59, 4, 244, 202, 168, 143, 107, 79, 39, 26, 18, 248, 209, 190, 148, 109, 3 }, 291) +
                    UrlObfuscator.decode(new int[] { 79, 57, 5, 182, 193, 187, 131, 47, 84, 60, 80, 235, 193, 226, 152, 105, 91, 39, 11, 234, 241, 171, 147, 56, 17, 123 }, 57) +
                    UrlObfuscator.decode(new int[] { 35, 15, 160, 208, 175, 139, 96, 76, 53, 79, 193, 241, 218, 175, 147, 114, 94, 27, 10, 254, 210, 178, 145, 53, 20, 16, 30, 235, 220, 162, 133, 111, 104, 59, 1, 227, 193, 160, 202, 113, 71, 49, 15, 13, 234, 238, 191, 137, 117, 85, 52, 35, 249, 197, 253, 136, 83, 95, 52, 29, 225, 196, 168, 169, 120, 64, 44, 0, 227, 139, 182, 134, 114, 78, 50, 43, 45, 254, 206, 180, 150, 117, 108, 56, 6, 189, 192, 188, 130, 109, 0, 102, 85, 240 }, 74) +
                    UrlObfuscator.decode(new int[] { 38, 83, 162, 197, 251, 130, 103, 65, 54, 91, 170, 205, 178, 141, 108, 88, 40, 2, 161, 205, 238, 157, 120 }, 91) +
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
                    // i.e. the traditional <script src=UrlObfuscator.decode(new int[] { 66, 165, 132, 230, 142, 110, 84, 32, 6, 226, 209, 164, 205, 158, 107, 73, 52, 86, 144, 153, 248, 215, 54, 21, 116, 83, 178, 145, 240, 207, 46, 13, 108, 75, 170, 137, 232, 199, 38, 10, 107, 67, 225, 206, 173, 175, 159, 105, 18, 49, 9 }, 108)> global build. A site that bundles the
                    // modular v9+ SDK (import { getAuth } from
                    // 'firebase/auth') never puts 'firebase' on window at
                    // all, so there's nothing here to find or patch --
                    // sign-in on that kind of site still needs the manual
                    // AndroidBridge.signInWithGoogle() wiring described
                    // where that bridge method is defined.
                    UrlObfuscator.decode(new int[] { 9, 238, 194, 161, 209, 126, 66, 56, 22, 224, 218, 189, 159, 56, 6, 53 }, 125) +
                    UrlObfuscator.decode(new int[] { 231, 203, 228, 202, 125, 64, 38, 3, 233, 210, 234, 162, 108, 69, 50, 48, 23, 249, 254, 169, 147, 125, 95, 50, 10, 233, 149, 146, 156, 117, 66, 32, 7, 233, 238, 185, 131, 109, 79, 34, 72, 246, 205, 164, 140, 72, 78, 104, 55, 9, 244, 252, 181, 150, 127, 91, 51, 92, 230, 214, 166, 132, 98, 65, 117 }, 142) +
                    UrlObfuscator.decode(new int[] { 249, 203, 179, 159, 111, 83, 54, 22, 183, 198, 180, 128, 112, 90, 121, 22, 237, 135, 182 }, 159) +
                    UrlObfuscator.decode(new int[] { 217, 169, 198, 44, 74, 41, 22, 245, 137, 161, 132, 43, 69, 54, 22, 233, 220, 195, 184, 159, 50, 90, 47, 13, 240, 153, 137, 170, 117, 93, 54, 3, 255, 198, 170, 170, 99, 68, 45, 5, 237, 230, 179, 145, 108, 115, 35, 21, 227, 247, 219, 185, 213, 105, 95, 45, 13, 229, 216, 238 }, 176) +
                    UrlObfuscator.decode(new int[] { 183, 129, 141, 62, 122, 29, 43, 167, 223, 186, 217, 119, 64, 32, 27, 188, 246, 191, 128, 105, 65, 41, 42, 255, 221, 160, 183, 116, 74, 50, 10, 230, 196, 178, 228, 151, 123, 20, 122, 61, 216, 232, 254, 132, 112, 64, 38, 0, 255, 139 }, 193) +
                    UrlObfuscator.decode(new int[] { 164, 144, 98, 15, 62, 31, 227, 223, 165, 210, 124, 85, 63, 30, 244, 209, 173, 149, 111, 34, 113, 63, 22, 254, 217, 173, 214, 112, 83, 33, 36, 225, 221, 165, 159, 123, 87, 61, 9, 196, 204, 225, 142, 101, 8, 36, 17, 247, 202, 233, 201, 214, 37, 64, 63, 26, 238, 218, 176, 223, 115, 28, 47, 1, 247, 197, 165, 157, 96, 22, 49 }, 210) +
                    UrlObfuscator.decode(new int[] { 138, 100, 9, 97, 47, 12, 242, 200, 180, 211, 107, 93, 35, 3, 231, 218, 232 }, 227) +
                    UrlObfuscator.decode(new int[] { 175, 52, 65, 56, 23, 225, 231, 163, 187, 98, 94, 33, 56, 232, 214, 176, 148, 36, 14, 102, 19, 22, 249, 211, 149, 149, 77, 80, 44, 31, 196, 208, 176, 154, 96, 84, 51, 27, 169, 240, 226, 141, 101, 91, 13, 6, 229, 205, 236, 133, 119, 79, 35, 43, 23, 242, 210, 243, 151, 48, 67 }, 244) +
                    UrlObfuscator.decode(new int[] { 115, 69, 49, 66, 238, 210, 214, 185, 192, 108, 73, 53, 13, 247, 236, 187, 168, 47, 90, 52, 89, 228, 214, 190, 136, 99, 77, 106, 6, 250, 206, 161, 196, 57, 30, 101, 7, 245, 241, 221, 169, 149, 116, 84, 126, 81, 229, 211, 161, 129, 97, 92, 106 }, 261) +
                    UrlObfuscator.decode(new int[] { 102, 71, 59, 7, 253, 234, 189, 178, 51, 75, 57, 5, 233, 221, 161, 136, 104, 13, 52, 17, 237, 215, 169, 187, 155, 111, 21, 32 }, 278) +
                    UrlObfuscator.decode(new int[] { 81, 39, 23, 164, 202, 177, 166, 111, 112, 89, 49, 25, 166, 202, 171, 151, 97, 95, 49, 17, 225, 148, 247, 216, 127, 92, 34, 26, 226, 206, 172, 154, 41, 86, 55, 11, 245, 203, 165, 133, 141, 87, 89, 97, 70, 167, 158, 191, 152, 121, 82, 56, 22, 188, 210, 191, 130, 41, 81, 48, 67, 205, 232, 152, 193, 32, 85, 54, 12, 244, 200, 164, 186, 140, 61, 85, 53, 9, 237, 217, 185, 149, 112, 91, 53, 82, 214, 241, 159, 199, 36, 23 }, 295) +
                    UrlObfuscator.decode(new int[] { 84, 58, 83, 187, 208, 171, 176, 121, 90, 51, 31, 247, 152, 162, 138, 122, 88, 62, 5, 170, 198, 186, 142, 97, 11, 37, 19, 242, 205, 185, 247, 138, 117, 85, 40, 86, 248, 202, 176, 131, 120, 81, 61, 6, 226, 153, 244 }, 61) +
                    UrlObfuscator.decode(new int[] { 56, 12, 254, 139, 171, 156, 124, 79, 15, 11, 247, 215, 255, 149, 104, 118, 77, 102 }, 78) +
                    UrlObfuscator.decode(new int[] { 45, 27, 233, 201, 169, 148, 57, 86, 50, 1, 181, 228, 161, 157, 124, 89, 60, 11, 165, 202, 190, 132, 106, 92, 46, 9, 235, 140, 177, 135, 114, 79, 83, 40, 24, 176, 201, 191, 147, 125, 84, 34, 92, 239 }, 95) +
                    UrlObfuscator.decode(new int[] { 22, 250, 192, 174, 152, 98, 69, 39, 72, 232, 200, 151, 129, 112, 10, 36, 73, 4 }, 112) +
                    UrlObfuscator.decode(new int[] { 246, 201, 209, 186, 146, 107, 21, 40, 28, 245, 216, 160, 144, 81, 69, 55, 31, 228, 227, 167, 158, 120, 78, 36, 12, 250, 143, 225, 132, 106, 71, 48, 14, 233, 251, 249, 178, 147, 124, 86, 60, 43, 254, 209, 187, 189, 125, 96, 52, 3, 250, 194, 185, 203, 39, 69, 39, 58, 226, 213, 236, 223 }, 129) +
                    UrlObfuscator.decode(new int[] { 228, 208, 162, 207, 106, 16, 41, 69, 238, 204, 188, 134, 111, 73, 56, 31, 249, 220, 251 }, 146) +
                    UrlObfuscator.decode(new int[] { 202, 164, 201, 33, 123, 16, 50, 23, 178, 193, 171, 157, 125, 83, 54, 0, 187, 220, 180, 135, 47, 107, 63, 30, 228, 216, 225, 140, 41, 75, 32, 23, 240, 195, 166, 133, 131, 98, 26, 27, 20, 245, 222, 180, 146, 54, 70, 61, 20, 252, 156, 185, 129, 46, 75, 45, 2, 230, 204, 172, 192, 47, 12, 127, 17, 231, 213, 181, 173, 144, 38, 65 }, 163) +
                    UrlObfuscator.decode(new int[] { 213, 166, 134, 121, 121, 33, 29, 249, 130, 184, 131, 110, 70, 14, 8, 210, 205, 183, 138, 66, 82, 90, 58, 24, 242, 207, 179, 152, 116, 31, 17, 52, 196, 157, 177, 131, 117, 75, 43, 3, 248, 194, 171, 133, 32, 67, 104, 12, 224, 247, 173, 138, 101, 113, 23, 116, 82, 239, 210, 188, 150, 63, 68, 48, 7, 252, 222, 167, 149, 35, 92, 40, 6, 238, 201, 189, 193, 60 }, 180) +
                    "}" +
                    UrlObfuscator.decode(new int[] { 178, 141, 109, 70, 46, 23, 81, 255, 217, 184, 190, 108, 92, 54, 3, 218, 220, 167, 135, 119, 95, 53, 29, 166, 138, 173, 133, 110, 91, 39, 14, 226, 226, 171, 140, 101, 77, 37, 12, 23, 250, 210, 146, 148, 75, 93, 36, 3, 249, 192, 244, 222, 126, 94, 29, 11, 254, 133, 240 }, 197) +
                    UrlObfuscator.decode(new int[] { 151, 155, 112, 65, 61, 24, 244, 237, 188, 132, 104, 76, 47, 71, 251, 206, 161, 139, 77, 77, 21, 8, 244, 247, 249, 178, 147, 124, 86, 60, 80, 190, 141 }, 214) +
                    UrlObfuscator.decode(new int[] { 154, 47, 30 }, 231) +
                    UrlObfuscator.decode(new int[] { 133, 44, 75, 124, 79 }, 248) +
                    UrlObfuscator.decode(new int[] { 111, 74, 105, 7, 240, 208, 171, 204, 94, 127, 94, 48, 25, 238, 212, 179, 157, 95, 88, 57, 18, 248, 214, 147, 132, 100, 71, 30, 12, 248, 200, 162, 140, 108, 26, 50, 23, 241, 198, 249 }, 265) +
                    "}" +
                    UrlObfuscator.decode(new int[] { 115, 95, 112, 0, 255, 219, 176, 156, 101, 31, 54, 6, 252, 200, 174, 138, 121, 76, 97, 28, 246, 196, 176, 128, 106, 9, 55, 54, 16, 249, 211, 172, 212, 127, 81, 37, 19, 247, 213, 160, 151, 56, 11, 50 }, 282) +
                    UrlObfuscator.decode(new int[] { 85, 35, 29, 232, 215, 189, 139, 123, 8, 41, 91, 181, 159, 181, 131, 115, 0, 86, 40, 64, 239, 222, 174, 176, 118, 67, 51, 7, 226, 210, 190, 217, 118, 90, 32, 14, 248, 194, 165, 135, 32, 14, 61, 11, 175, 136, 249 }, 48) +
                    UrlObfuscator.decode(new int[] { 40, 6, 87, 233, 212, 178, 159, 117, 78, 118, 17, 255, 199, 177, 145, 115, 66, 53, 70, 245, 221, 173, 159, 105, 65, 96, 16, 239, 203, 160, 140, 117, 15, 38, 54, 12, 248, 222, 186, 137, 124, 17, 108, 21, 249, 209, 178, 128, 88, 94, 59, 11, 255, 218, 170, 134, 33, 65, 49, 79, 190, 217 }, 65) +
                    UrlObfuscator.decode(new int[] { 55, 29, 227, 202, 238, 132, 106, 3, 36, 87, 186, 151, 239, 158, 103, 79, 39, 0, 242, 214, 208, 169, 153, 105, 76, 56, 20, 191, 223, 163, 221, 40, 79 }, 82) +
                    UrlObfuscator.decode(new int[] { 30, 174, 147, 245, 239, 215, 38, 65 }, 99) +
                    UrlObfuscator.decode(new int[] { 9, 186, 154, 248, 203, 114, 77, 44, 24, 232, 194, 225, 141, 46, 93, 56 }, 116) +
                    UrlObfuscator.decode(new int[] { 248, 141, 235, 203, 58 }, 133),
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
                if (rel.isEmpty()) rel = UrlObfuscator.decode(new int[] { 255, 219, 176, 150, 106, 31, 56, 27, 227, 193 }, 150);

                byte[] data = EmbeddedAssets.get(rel);
                if (data == null) return super.shouldInterceptRequest(view, request);

                return new WebResourceResponse(guessEmbeddedMime(rel), UrlObfuscator.decode(new int[] { 242, 146, 163, 41, 27 }, 167), new ByteArrayInputStream(data));
            }

            private String guessEmbeddedMime(String rel) {
                String lower = rel.toLowerCase();
                if (lower.endsWith(UrlObfuscator.decode(new int[] { 150, 191, 130, 120, 88 }, 184)) || lower.endsWith(UrlObfuscator.decode(new int[] { 231, 128, 115, 75 }, 201))) return UrlObfuscator.decode(new int[] { 174, 156, 96, 67, 121, 29, 224, 222, 190 }, 218);
                if (lower.endsWith(UrlObfuscator.decode(new int[] { 197, 105, 90, 59 }, 235))) return UrlObfuscator.decode(new int[] { 136, 126, 66, 45, 87, 244, 197, 166 }, 252);
                if (lower.endsWith(UrlObfuscator.decode(new int[] { 35, 70, 56 }, 269)) || lower.endsWith(UrlObfuscator.decode(new int[] { 48, 80, 54, 8 }, 286))) return UrlObfuscator.decode(new int[] { 85, 35, 2, 253, 217, 172, 143, 121, 69, 36, 4, 166, 194, 166, 144, 100, 87, 32, 16, 232, 208, 203 }, 52);
                if (lower.endsWith(UrlObfuscator.decode(new int[] { 107, 14, 240, 205, 175 }, 69))) return UrlObfuscator.decode(new int[] { 55, 5, 228, 223, 187, 146, 113, 91, 39, 2, 226, 132, 160, 154, 103, 73 }, 86);
                if (lower.endsWith(UrlObfuscator.decode(new int[] { 73, 245, 211, 163 }, 103))) return UrlObfuscator.decode(new int[] { 17, 250, 215, 178, 145, 60, 65, 39, 23, 164, 214, 160, 128 }, 120);
                if (lower.endsWith(UrlObfuscator.decode(new int[] { 167, 216, 169, 129 }, 137))) return UrlObfuscator.decode(new int[] { 243, 212, 185, 144, 115, 26, 36, 29, 245 }, 154);
                if (lower.endsWith(UrlObfuscator.decode(new int[] { 133, 160, 153, 111 }, 171)) || lower.endsWith(UrlObfuscator.decode(new int[] { 146, 177, 138, 124, 95 }, 188))) return UrlObfuscator.decode(new int[] { 164, 129, 106, 77, 44, 71, 237, 214, 160, 131 }, 205);
                if (lower.endsWith(UrlObfuscator.decode(new int[] { 240, 154, 117, 93 }, 222))) return UrlObfuscator.decode(new int[] { 134, 99, 76, 43, 14, 165, 206, 161, 129 }, 239);
                if (lower.endsWith(UrlObfuscator.decode(new int[] { 46, 104, 91, 63, 12 }, 256))) return UrlObfuscator.decode(new int[] { 120, 93, 46, 9, 232, 131, 188, 143, 107, 88 }, 273);
                if (lower.endsWith(UrlObfuscator.decode(new int[] { 12, 54, 15, 25, 248, 143 }, 290))) return UrlObfuscator.decode(new int[] { 94, 56, 24, 225, 155, 164, 157, 119, 86, 125 }, 56);
                if (lower.endsWith(UrlObfuscator.decode(new int[] { 103, 31, 232, 192, 163 }, 73))) return UrlObfuscator.decode(new int[] { 60, 22, 246, 195, 249, 130, 123, 85, 52 }, 90);
                if (lower.endsWith(UrlObfuscator.decode(new int[] { 69, 254, 221, 174 }, 107))) return UrlObfuscator.decode(new int[] { 26, 244, 212, 173, 215, 99, 66, 51 }, 124);
                if (lower.endsWith(UrlObfuscator.decode(new int[] { 163, 197, 168, 133 }, 141))) return UrlObfuscator.decode(new int[] { 247, 208, 189, 156, 127, 22, 32, 90, 255, 214, 187, 157 }, 158);
                return UrlObfuscator.decode(new int[] { 206, 190, 157, 96, 66, 41, 8, 252, 206, 169, 139, 43, 76, 33, 21, 229, 235, 147, 174, 136, 105, 95, 56, 21 }, 175);
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
            // UrlObfuscator.decode(new int[] { 147, 188, 159, 115, 28, 10, 40 }, 192) feature or a video-chat widget using getUserMedia().
            // Without this override the WebView auto-denies every such
            // request, which is what was showing as UrlObfuscator.decode(new int[] { 146, 145, 98, 75, 63, 13, 171, 218, 172, 154, 106, 79, 54, 23, 234, 205, 175, 234, 223, 62, 29, 124, 91, 186, 153, 248, 215, 54, 21, 116, 92, 189, 145, 180, 138, 96, 68, 41, 15, 170, 198, 186, 199, 115, 75, 37, 21, 227, 200, 172, 190, 156, 113, 89 }, 209) -- the app never even asked Android for
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
            // watchPosition() calls (e.g. a UrlObfuscator.decode(new int[] { 132, 104, 78, 91, 126, 14, 232, 212, 168, 156, 107, 23, 56, 16, 245, 193, 242, 156, 117 }, 226) feature).
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
            // UrlObfuscator.decode(new int[] { 128, 123, 86, 62, 79, 231, 195, 236, 156, 99, 93, 32, 71, 246, 202, 180, 150, 114 }, 243) flows work. A bare WebView has nowhere to put
            // that second window, so without this override the popup silently
            // fails (or opens detached from the parent page) and the auth
            // handler comes back with UrlObfuscator.decode(new int[] { 80, 75, 39, 65, 242, 250, 207, 168, 153, 104, 78, 60, 28, 183, 215, 182, 128, 122, 93, 63, 80, 230, 221, 237, 133, 101, 92, 40, 4, 238, 194 }, 260).
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
                // needs the same UrlObfuscator.decode(new int[] { 46, 20, 36, 4, 184 }, 277)/UrlObfuscator.decode(new int[] { 112, 32, 22, 240, 203, 174, 142, 208, 70, 19, 4, 91 }, 294) stripping or it hits
                // the same silent block.
                String popupDefaultUA = popupSettings.getUserAgentString();
                popupSettings.setUserAgentString(popupDefaultUA.replace(UrlObfuscator.decode(new int[] { 7, 123, 13, 239 }, 60), "").replaceAll("Version/[0-9.]+\s", ""));
                popupWebView.setVerticalScrollBarEnabled(false);
                popupWebView.setHorizontalScrollBarEnabled(false);

                final Dialog popupDialog = new Dialog(MainActivity.this, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
                popupDialog.setContentView(popupWebView);
                popupDialog.setOnDismissListener(d -> popupWebView.destroy());
                popupDialog.show();

                popupWebView.setWebViewClient(new WebViewClient() {
                    // window.open() targets (like the UrlObfuscator.decode(new int[] { 24, 28, 239, 203, 189, 141, 39, 72, 42, 19 }, 77) link) can land
                    // on a page -- e.g. Telegram's t.me web page -- that immediately
                    // tries to hand off to a non-http(s) app scheme (tg://, market://,
                    // mailto:, intent://, etc). A bare WebView can't load those itself
                    // and shows Android's raw UrlObfuscator.decode(new int[] { 9, 24, 254, 203, 187, 158, 125, 23, 56, 26, 224, 147, 179, 135, 113, 70, 34, 12, 238, 199, 175, 201, 39, 45, 102, 69, 164, 131, 226, 193, 32, 63, 30, 125, 92, 187, 154, 249, 216, 55, 22, 117, 84, 179, 157, 254, 208, 74, 124, 31, 51, 222, 228, 130, 166, 72, 113, 11, 59, 214, 240, 141, 191, 172, 93, 117, 25, 54, 223 }, 94) error. Intercept here and hand the URL
                    // to the system instead, so it opens Telegram (or falls back to
                    // the Play Store / browser) the way a real browser tab would.
                    @Override
                    public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                        Uri uri = request.getUrl();
                        String scheme = uri.getScheme();
                        if (scheme != null && !scheme.equals(UrlObfuscator.decode(new int[] { 7, 250, 217, 188 }, 111)) && !scheme.equals(UrlObfuscator.decode(new int[] { 232, 235, 202, 173, 143 }, 128))) {
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
        return EMBED_HOST + UrlObfuscator.decode(new int[] { 248, 222, 171, 139, 117, 2, 35, 30, 228, 196 }, 145);
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
        if (Intent.ACTION_SEND.equals(action) && UrlObfuscator.decode(new int[] { 214, 164, 152, 139, 49, 77, 48, 26, 243, 215 }, 162).equals(intent.getType())) {
            String shared = intent.getStringExtra(Intent.EXTRA_TEXT);
            if (shared != null && !shared.isEmpty()) {
                try {
                    return baseUrl() + UrlObfuscator.decode(new int[] { 140, 161, 153, 113, 93, 43, 9, 211, 223, 175, 145, 124, 26 }, 179) + java.net.URLEncoder.encode(shared, UrlObfuscator.decode(new int[] { 145, 183, 68, 12, 120 }, 196));
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

        String shortcutAction = intent.getStringExtra(UrlObfuscator.decode(new int[] { 166, 156, 124, 64, 37, 19, 250, 218, 146, 141, 104, 94, 32, 7, 233 }, 213));
        if (UrlObfuscator.decode(new int[] { 148, 96, 72, 44, 3, 229 }, 230).equals(shortcutAction)) {
            webView.getSettings().setCacheMode(WebSettings.LOAD_NO_CACHE);
            webView.reload();
            webView.getSettings().setCacheMode(WebSettings.LOAD_CACHE_ELSE_NETWORK);
            return;
        }
        if (UrlObfuscator.decode(new int[] { 159, 121, 88, 49 }, 247).equals(shortcutAction)) {
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
    // system bars to match the page's own <meta name=UrlObfuscator.decode(new int[] { 124, 79, 35, 8, 225, 142, 161, 142, 108, 112, 76 }, 264)>
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
                    sendIntent.setType(UrlObfuscator.decode(new int[] { 109, 93, 47, 2, 186, 196, 191, 147, 120, 94 }, 281));
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
        // the app is open or backgrounded (e.g. on a socket.io UrlObfuscator.decode(new int[] { 65, 43, 26, 172, 198, 175, 154, 123, 70, 33, 0 }, 47)
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
                String safeTitle = (title == null || title.trim().isEmpty()) ? UrlObfuscator.decode(new int[] { 16, 45, 17, 229, 197, 251, 178, 118, 89, 57, 17 }, 64) : title;
                String safeBody = body == null ? "" : body;
                NotificationCompat.Builder notification = new NotificationCompat.Builder(MainActivity.this, UrlObfuscator.decode(new int[] { 53, 21, 233, 207, 184, 128, 127 }, 81))
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
        // which surfaces as a raw UrlObfuscator.decode(new int[] { 53, 228, 194, 207, 191, 154, 121, 27, 52, 22, 236, 151, 183, 131, 117, 90, 62, 16, 242, 195, 171, 205, 35, 11, 15, 59, 218, 248, 133, 170, 74, 109, 7, 34, 212, 214, 241, 147, 163, 73, 127, 10, 61, 195 }, 98)
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
            if (hex == null || !hex.matches(UrlObfuscator.decode(new int[] { 45, 177, 153, 139, 174, 35, 107, 45, 70, 236, 153, 229, 222, 91, 94, 114, 30, 254, 250, 129, 242, 184, 124, 17, 61, 74, 180, 129, 138, 141, 38, 73, 122, 86 }, 115))) return;
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
            if (hex == null || !hex.matches(UrlObfuscator.decode(new int[] { 218, 128, 153, 160, 45, 89, 95, 112, 26, 171, 151, 224, 165, 108, 0, 40, 80 }, 132))) return;
            runOnUiThread(() -> {
                try {
                    int color = Color.parseColor(hex);
                    getWindow().setBackgroundDrawable(new ColorDrawable(color));
                    if (webView != null) webView.setBackgroundColor(color);
                } catch (Exception ignored) {
                }
            });
        }

        // Whether the special UrlObfuscator.decode(new int[] { 212, 216, 191, 210, 119, 89, 35, 11, 254, 140, 170, 137, 106, 77, 52, 21 }, 149) grant (API 30+) is
        // already on. Below API 30 the legacy WRITE_EXTERNAL_STORAGE
        // permission (see storageLegacy in the Options picker) covers
        // everything this would, so this always reads true there --
        // there's no separate toggle to check.
        @JavascriptInterface
        public boolean hasAllFilesAccess() {
            return Build.VERSION.SDK_INT < Build.VERSION_CODES.R || Environment.isExternalStorageManager();
        }

        // Sends the user to the one settings screen that grants UrlObfuscator.decode(new int[] { 231, 169, 136, 9, 2, 97, 64, 95, 190, 157, 252, 219, 53, 22, 120, 17, 255, 217, 177, 128, 50, 80, 51, 12, 235, 222, 191 }, 166) (MANAGE_EXTERNAL_STORAGE) -- unlike the runtime
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
                        Uri.parse(UrlObfuscator.decode(new int[] { 199, 183, 150, 127, 82, 53, 20, 170 }, 183) + getPackageName()));
                    startActivity(intent);
                } catch (ActivityNotFoundException e) {
                    // Some OEM builds don't ship this exact screen -- fall
                    // back to the general UrlObfuscator.decode(new int[] { 137, 139, 106, 5, 34, 10, 238, 196, 179, 255, 159, 126, 95, 62, 9, 234 }, 200) list instead
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
            if (Build.TAGS != null && Build.TAGS.contains(UrlObfuscator.decode(new int[] { 173, 157, 100, 66, 120, 31, 246, 203, 162 }, 217))) return true;
            String[] suPaths = {
                UrlObfuscator.decode(new int[] { 197, 122, 81, 52, 18, 224, 201, 236, 128, 104, 78, 16, 45, 8 }, 234), UrlObfuscator.decode(new int[] { 212, 105, 64, 43, 3, 243, 216, 251, 139, 112, 88, 62, 64, 253, 216 }, 251), UrlObfuscator.decode(new int[] { 35, 88, 40, 0, 230, 136, 181, 144 }, 268),
                UrlObfuscator.decode(new int[] { 50, 79, 34, 9, 237, 221, 186, 217, 116, 68, 35, 93, 194, 197, 191, 139, 127, 89, 56, 15, 251, 134, 166, 150, 110 }, 285), UrlObfuscator.decode(new int[] { 28, 33, 8, 227, 219, 171, 128, 35, 74, 58, 25, 167, 244, 179, 149, 97, 81, 17, 52, 174, 254, 206, 182 }, 51),
                UrlObfuscator.decode(new int[] { 107, 7, 227, 213, 161, 240, 146, 114, 95, 58, 22, 182, 192, 181, 159, 123, 27, 32, 7 }, 68), UrlObfuscator.decode(new int[] { 122, 16, 242, 198, 176, 223, 99, 65, 46, 13, 231, 133, 171, 129, 105, 9, 54, 17 }, 85), UrlObfuscator.decode(new int[] { 73, 225, 197, 183, 131, 46, 76, 80, 61, 28, 240, 148, 169, 140 }, 102),
                UrlObfuscator.decode(new int[] { 88, 229, 192, 251, 145, 123, 95, 127, 28, 251 }, 119), UrlObfuscator.decode(new int[] { 167, 212, 191, 150, 112, 70, 47, 78, 226, 246, 208, 242, 154, 122, 83, 53, 11, 246, 208, 176, 219, 96, 71 }, 136), UrlObfuscator.decode(new int[] { 182, 203, 174, 133, 97, 81, 62, 93, 226, 212, 224, 150, 111, 69, 37, 69, 250, 221 }, 153)
            };
            for (String path : suPaths) {
                if (new File(path).exists()) return true;
            }
            // Magisk hides its su paths on some configs but the manager
            // app itself is still installed under its own package name.
            String[] knownRootPackages = { UrlObfuscator.decode(new int[] { 201, 166, 133, 41, 82, 42, 20, 233, 205, 169, 142, 136, 107, 19, 49, 26, 253, 208, 171, 156 }, 170), UrlObfuscator.decode(new int[] { 222, 175, 215, 123, 95, 55, 28, 250, 213, 187, 131, 117, 1, 61, 24, 252, 206, 184, 154, 125 }, 187), UrlObfuscator.decode(new int[] { 175, 132, 103, 7, 38, 8, 245, 205, 177, 133, 109, 84, 110, 62, 16, 249, 206, 180, 147, 125, 22, 36, 3 }, 204) };
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
                        UrlObfuscator.decode(new int[] { 137, 148, 114, 73, 121, 30, 242, 215, 161, 129, 97, 87, 113, 30, 234, 203, 169, 159, 43, 75, 105, 26, 232, 201, 177, 129, 103, 2, 37, 5, 9, 247, 222, 185, 219, 110, 86, 120, 0, 249, 199, 191, 221 }, 221), Toast.LENGTH_SHORT).show();
                    webView.evaluateJavascript(
                        UrlObfuscator.decode(new int[] { 198, 107, 89, 37, 9, 253, 193, 168, 136, 45, 13, 56, 22, 243, 217, 196, 183, 155, 52, 76, 51, 23, 252, 216, 161, 219, 123, 93, 19, 31, 244, 221, 161, 132, 104, 121, 37, 6, 252, 230, 165, 134, 97, 80, 49, 51, 229, 236, 203, 177, 136, 50, 65 }, 238) +
                        UrlObfuscator.decode(new int[] { 136, 119, 83, 56, 20, 237, 151, 183, 153, 87, 91, 48, 1, 253, 216, 180, 189, 97, 66, 56, 42, 233, 202, 173, 148, 117, 119, 33, 16, 247, 205, 180, 247, 152, 124, 80, 40, 31, 176, 131 }, 255) +
                        UrlObfuscator.decode(new int[] { 109, 82, 45, 12, 248, 200, 162, 193, 109, 14, 61, 24 }, 272) +
                        UrlObfuscator.decode(new int[] { 85, 50, 38, 5, 234, 213, 181, 158, 118, 79, 121, 18, 252, 199, 163, 147, 101, 83, 39, 43, 251, 201, 165, 158, 33, 70, 34, 17, 165, 231, 182, 145, 117, 79, 82, 27, 11, 249, 213, 174, 209, 63, 86, 56, 17, 230, 220, 187, 149, 66, 64, 33, 25, 205, 200, 169, 140, 123, 84, 20, 0, 247, 214, 174, 149, 39, 51 }, 289) +
                        UrlObfuscator.decode(new int[] { 76, 50, 16, 224, 210, 187, 157, 42, 84, 41, 31, 237, 197, 190, 140, 108, 29, 32, 4, 232, 208, 167, 156, 125, 54, 23, 102, 1, 248, 219, 173, 155, 127, 30, 48, 93, 232, 207 }, 55) +
                        UrlObfuscator.decode(new int[] { 53, 78, 174, 140, 255 }, 72), null);
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
                    UrlObfuscator.decode(new int[] { 113, 30, 226, 216, 182, 128, 122, 93, 63, 88, 166, 213, 185, 158, 114, 81, 32, 14, 175, 209, 172, 138, 103, 77, 54, 78, 16, 240, 252, 178, 159, 104, 86, 49, 19, 196, 218, 187, 135, 83, 82, 51, 10, 253, 222, 158, 142, 121, 92, 36, 19, 175, 222 }, 89) +
                    UrlObfuscator.decode(new int[] { 29, 224, 198, 163, 137, 114, 10, 44, 12, 192, 206, 219, 172, 146, 117, 95, 8, 22, 247, 195, 151, 150, 119, 86, 33, 2, 194, 202, 189, 152, 96, 95, 98 }, 106) + result + ");" +
                    UrlObfuscator.decode(new int[] { 6, 231, 218, 185, 131, 117, 93, 124, 22, 187, 202, 173 }, 123) +
                    UrlObfuscator.decode(new int[] { 248, 217, 179, 146, 127, 78, 40, 1, 235, 212, 236, 133, 105, 108, 78, 60, 8, 248, 210, 156, 142, 114, 88, 33, 92, 253, 215, 166, 208, 76, 91, 62, 24, 228, 199, 140, 158, 98, 72, 49, 76, 164, 195, 175, 132, 141, 113, 84, 56, 41, 245, 214, 172, 182, 117, 86, 49, 0, 225, 227, 181, 156, 123, 65, 56, 76, 166 }, 140) +
                    UrlObfuscator.decode(new int[] { 230, 216, 190, 142, 120, 81, 59, 76, 238, 211, 161, 147, 127, 68, 42, 10, 183 }, 157) + result + UrlObfuscator.decode(new int[] { 211, 176, 197, 34, 17, 52, 11, 230, 210, 166, 140, 43, 71, 104, 27, 2 }, 174) +
                    UrlObfuscator.decode(new int[] { 194, 247, 213, 53, 0 }, 191), null));
            }).start();
        }

        // Runs UrlObfuscator.decode(new int[] { 131, 134, 105, 67, 108, 2, 228, 137, 191, 142, 114, 77, 100, 36, 237, 206, 167, 179, 155 }, 208) as a real system flow (Credential
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
    // account chooser, matching the explicit UrlObfuscator.decode(new int[] { 149, 97, 111, 30, 60, 92, 200, 211, 190, 150, 55, 95, 59, 84, 228, 219, 165, 152, 47, 105, 34, 3, 236, 198, 172, 226, 39, 6, 101, 68, 172, 141, 225, 130, 138, 106, 73, 51, 21 }, 225) gesture this is wired to -- GetGoogleIdOption's silent/
    // auto-select behavior is meant for a passive UrlObfuscator.decode(new int[] { 129, 120, 87, 33, 78, 228, 194, 235, 139, 124, 92, 40, 11, 228, 208, 170, 129, 96, 76, 83, 39, 119, 188, 155, 250, 217, 55, 24, 118, 26, 250, 147, 179, 129, 96, 15, 34, 12, 249, 197, 169, 129 }, 242) prompt, which isn't what a button press should do.
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
                        notifyGoogleSignInError(UrlObfuscator.decode(new int[] { 86, 76, 36, 24, 15, 251, 222, 168, 158, 126, 25, 59, 5, 243, 209, 177, 157, 102, 88, 49, 3, 174, 217, 181, 155, 111 }, 259));
                        return;
                    }
                    try {
                        GoogleIdTokenCredential googleIdTokenCredential =
                            GoogleIdTokenCredential.createFrom(((CustomCredential) credential).getData());
                        notifyGoogleSignInSuccess(googleIdTokenCredential.getIdToken());
                    } catch (GoogleIdTokenParsingException e) {
                        notifyGoogleSignInError(UrlObfuscator.decode(new int[] { 87, 92, 39, 29, 244, 143, 160, 130, 120, 11, 58, 8, 250, 212, 163, 197, 67, 76, 45, 6, 236, 250, 158, 148, 184, 59, 78, 54, 19, 242, 216 }, 276));
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
            UrlObfuscator.decode(new int[] { 13, 34, 22, 236, 194, 180, 182, 145, 115, 20, 114, 1, 237, 202, 174, 141, 124, 82, 123, 5, 248, 222, 171, 129, 122, 2, 36, 4, 200, 198, 163, 148, 106, 77, 39, 37, 238, 207, 216, 178, 152, 79, 82, 61, 23, 209, 217, 255, 142 }, 293) +
            UrlObfuscator.decode(new int[] { 76, 51, 23, 252, 216, 161, 219, 123, 93, 19, 31, 244, 221, 161, 132, 104, 108, 37, 6, 239, 203, 163, 182, 109, 68, 44, 40, 238, 183 }, 59) + JSONObject.quote(idToken) + ");" +
            UrlObfuscator.decode(new int[] { 49, 22, 233, 200, 188, 132, 110, 13, 33, 74, 249, 220 }, 76) +
            UrlObfuscator.decode(new int[] { 41, 14, 226, 193, 174, 145, 121, 82, 58, 3, 189, 214, 184, 131, 127, 79, 57, 15, 227, 239, 191, 141, 105, 82, 109, 10, 230, 213, 225, 163, 138, 109, 73, 51, 22, 223, 207, 189, 153, 98, 29, 115, 18, 252, 213, 162, 128, 103, 73, 11, 4, 229, 206, 164, 130, 85, 76, 35, 13, 203, 207, 146, 186, 141, 104, 80, 47, 93, 181 }, 93) +
            UrlObfuscator.decode(new int[] { 21, 233, 201, 191, 139, 96, 68, 125, 29, 234, 207, 249, 150, 115, 85, 90, 114, 20, 248, 239, 181, 146, 125, 89, 108 }, 110) + JSONObject.quote(idToken) + UrlObfuscator.decode(new int[] { 2, 227, 148, 245, 192, 103, 90, 57, 3, 245, 221, 252, 150, 59, 74, 45 }, 127) +
            UrlObfuscator.decode(new int[] { 237, 134, 230, 196, 55 }, 144), null));
    }

    private void notifyGoogleSignInError(String message) {
        runOnUiThread(() -> webView.evaluateJavascript(
            UrlObfuscator.decode(new int[] { 137, 166, 170, 144, 126, 72, 50, 21, 247, 144, 254, 141, 97, 70, 42, 9, 248, 214, 231, 153, 100, 66, 47, 5, 254, 134, 168, 136, 68, 74, 39, 16, 238, 201, 219, 153, 146, 115, 92, 54, 28, 203, 222, 177, 155, 93, 93, 23, 3, 226, 192, 188, 196, 119 }, 161) +
            UrlObfuscator.decode(new int[] { 197, 184, 158, 107, 65, 58, 66, 228, 196, 136, 134, 99, 84, 42, 13, 231, 229, 174, 143, 152, 114, 88, 15, 18, 253, 215, 145, 153, 83, 71, 38, 28, 224, 153 }, 178) + JSONObject.quote(message) + ");" +
            UrlObfuscator.decode(new int[] { 190, 159, 98, 65, 75, 61, 21, 180, 222, 243, 130, 101 }, 195) +
            UrlObfuscator.decode(new int[] { 160, 129, 107, 74, 39, 6, 224, 201, 163, 156, 36, 77, 33, 20, 246, 196, 176, 128, 106, 100, 54, 58, 16, 233, 148, 181, 159, 110, 24, 20, 3, 230, 192, 188, 159, 84, 70, 42, 0, 249, 132, 236, 139, 103, 76, 53, 9, 236, 192, 132, 141, 110, 71, 83, 59, 46, 245, 220, 180, 176, 118, 101, 51, 6, 225, 223, 166, 214, 60 }, 212) +
            UrlObfuscator.decode(new int[] { 158, 96, 70, 54, 0, 233, 243, 132, 166, 147, 112, 0, 63, 25, 251, 197, 176, 216, 126, 87, 34, 3, 238, 201, 168, 214 }, 229) + JSONObject.quote(message) + UrlObfuscator.decode(new int[] { 139, 104, 29, 122, 73, 236, 211, 174, 154, 110, 68, 99, 15, 160, 211, 186 }, 246) +
            UrlObfuscator.decode(new int[] { 122, 15, 109, 77, 184 }, 263), null));
    }


    private boolean isLightColor(int color) {
        double luminance = (0.299 * Color.red(color) + 0.587 * Color.green(color) + 0.114 * Color.blue(color)) / 255;
        return luminance > 0.6;
    }

    // Hands the download off to Android's own DownloadManager instead of
    // fetching it manually. This is what makes the file:
    //  - show a real system notification (progress while downloading, then
    //    UrlObfuscator.decode(new int[] { 92, 88, 33, 27, 248, 220, 179, 149, 48, 76, 33, 0, 252, 199, 175, 157, 109 }, 280)) instead of the app being the only place any
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
    // its own UrlObfuscator.decode(new int[] { 121, 58, 8, 254, 220, 228, 171, 109, 64, 46, 56 }, 297) subfolder in there (DownloadManager
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
            request.addRequestHeader(UrlObfuscator.decode(new int[] { 106, 45, 24, 238, 150, 155, 158, 125, 89, 34 }, 63), userAgent);
            if (cookie != null) request.addRequestHeader(UrlObfuscator.decode(new int[] { 19, 0, 225, 198, 165, 142 }, 80), cookie);
            request.setTitle(filename);
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
            request.setDestinationInExternalPublicDir(
                Environment.DIRECTORY_DOWNLOADS, UrlObfuscator.decode(new int[] { 49, 242, 240, 198, 164, 220, 83, 85, 56, 22, 240, 153 }, 97) + filename);
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
    // instead of leaving the UrlObfuscator.decode(new int[] { 208, 220, 165, 159, 124, 64, 47, 9, 229, 197, 173, 199, 38, 9 }, 148) toast above as the last word
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
            String title = (titleIdx >= 0 && cursor.getString(titleIdx) != null) ? cursor.getString(titleIdx) : UrlObfuscator.decode(new int[] { 227, 173, 143, 103 }, 165);

            if (status == DownloadManager.STATUS_SUCCESSFUL) {
                String localUriStr = uriIdx >= 0 ? cursor.getString(uriIdx) : null;
                String folderName = UrlObfuscator.decode(new int[] { 242, 186, 131, 125, 94, 62, 17, 235, 221 }, 182);
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
                        // the saved path (rather than hardcoding UrlObfuscator.decode(new int[] { 131, 137, 114, 74, 47, 13, 224, 196, 204 }, 199))
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
                Toast.makeText(this, title + UrlObfuscator.decode(new int[] { 248, 147, 121, 66, 58, 31, 253, 208, 180, 138, 106, 13, 8280, 75, 236, 192, 166, 131, 38, 76, 48, 67, 235, 207, 224, 166, 145, 104, 78, 123 }, 216) + folderName + UrlObfuscator.decode(new int[] { 201, 110, 72, 42, 1, 225, 209 }, 233), Toast.LENGTH_LONG).show();
                notifyDownloadResult(true);

                // If what just finished downloading is itself an installable
                // Android package, go straight to the system installer
                // instead of leaving the user to dig it out of the Downloads
                // folder and tap it themselves. Gated strictly on the file
                // actually being a .apk -- every other kind of download this
                // wrapper handles (images, PDFs, whatever the wrapped site
                // links to) still behaves exactly as before, since UrlObfuscator.decode(new int[] { 147, 119, 75, 35, 23, 249, 216 }, 250)
                // wouldn't mean anything for those.
                if (localPath != null && title != null && title.toLowerCase().endsWith(UrlObfuscator.decode(new int[] { 37, 75, 57, 3 }, 267))) {
                    File apkFile = new File(localPath);
                    if (apkFile.exists()) {
                        try {
                            Uri contentUri = FileProvider.getUriForFile(
                                this, getPackageName() + UrlObfuscator.decode(new int[] { 50, 93, 51, 21, 253, 199, 164, 154, 98, 90, 54, 20, 226 }, 284), apkFile);
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
                Toast.makeText(this, UrlObfuscator.decode(new int[] { 118, 62, 7, 225, 194, 162, 141, 111, 10, 47, 9, 238, 202, 160, 128, 57, 2 }, 50) + title + UrlObfuscator.decode(new int[] { 99, 74, 228, 210, 205, 177, 143, 60 }, 67) + reason + ")", Toast.LENGTH_LONG).show();
                notifyDownloadResult(false);
            }
        } finally {
            cursor.close();
        }
    }

    // Tells the page's own download-button UI that Android's DownloadManager
    // has actually finished (or failed) -- see window.__onNativeDownloadComplete
    // in the wrapped page's script. Without this, the button's UrlObfuscator.decode(new int[] { 48, 28, 252, 212 }, 84) state
    // was just a fixed timer guessing how long a download UrlObfuscator.decode(new int[] { 21, 246, 204, 160, 128, 98, 115, 71 }, 101) takes,
    // with no way to know the real file size or connection speed -- so a
    // large APK on a slow connection could show UrlObfuscator.decode(new int[] { 5, 253, 219, 166, 158, 117, 16, 45, 11, 173, 197, 165, 202, 112, 71, 50, 20, 143, 132, 227, 194, 33, 15, 16, 126, 25, 243, 204, 180, 149, 119, 86, 50, 6 }, 118) while DownloadManager was still genuinely working. Fire-
    // and-forget, same as the theme-color/haptics wiring in onPageFinished.
    private void notifyDownloadResult(boolean success) {
        runOnUiThread(() -> {
            try {
                webView.evaluateJavascript(
                    UrlObfuscator.decode(new int[] { 243, 212, 188, 159, 106, 68, 105, 23, 22, 240, 217, 179, 140, 52, 102, 7, 24, 248, 251, 181, 135, 123, 71, 53, 43, 225, 218, 162, 135, 101, 72, 44, 36, 233, 200, 180, 143, 103, 85, 37, 118, 5, 234, 213, 181, 158, 118, 79, 121, 41, 202, 219, 189, 188, 112, 68, 38, 24, 232, 232, 164, 157, 103, 68, 40, 7, 225, 231, 172, 143, 113, 76, 90, 42, 24, 180 }, 135) + success + UrlObfuscator.decode(new int[] { 177, 140, 171, 136, 119, 82, 38, 18, 248, 135, 171, 196, 119, 86 }, 152),
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
                UrlObfuscator.decode(new int[] { 221, 186, 158, 125, 76, 34, 75, 245, 200, 174, 187, 145, 106, 18, 4, 37, 246, 214, 153, 151, 97, 93, 37, 23, 213, 223, 184, 128, 97, 67, 42, 14, 217, 218, 168, 129, 119, 65, 48, 17, 168, 219, 200, 183, 147, 120, 84, 45, 87, 199, 232, 185, 155, 90, 82, 38, 24, 230, 202, 138, 130, 123, 69, 38, 6, 233, 195, 150, 151, 107, 68, 48, 4, 243, 236, 150 }, 169) + done + "," + total + "," + status + UrlObfuscator.decode(new int[] { 147, 226, 133, 106, 85, 52, 0, 240, 218, 249, 149, 38, 85, 48 }, 186),
                null);
        } catch (Exception ignored) {
        }
    }

    private void notifyDownloadCancelled() {
        try {
            webView.evaluateJavascript(
                UrlObfuscator.decode(new int[] { 191, 152, 112, 83, 46, 0, 173, 211, 170, 140, 101, 79, 72, 112, 34, 195, 212, 180, 183, 121, 67, 63, 3, 241, 247, 189, 134, 126, 67, 33, 12, 232, 232, 165, 132, 120, 75, 35, 17, 225, 138, 185, 150, 105, 113, 90, 50, 11, 181, 229, 134, 151, 121, 120, 52, 0, 250, 196, 180, 180, 96, 89, 35, 0, 228, 203, 173, 171, 104, 75, 53, 8, 230, 214, 164, 200, 153, 127, 81, 47, 30, 182, 158, 187, 150, 120, 86, 49, 31, 254, 212, 180, 200, 39, 22, 49, 22, 233, 200, 188, 132, 110, 13, 33, 74, 249, 220 }, 203),
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
    // it (rather than a generic UrlObfuscator.decode(new int[] { 187, 148, 58, 77, 55, 87, 197, 208, 160, 135, 123, 95, 55, 28 }, 220) toast) and parks the
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
            // The settings screen has no defined UrlObfuscator.decode(new int[] { 238, 222, 169, 140, 116, 67 }, 156) for this action --
            // resultCode isn't reliable here, so just re-check the real
            // permission state directly instead of trusting it.
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O || getPackageManager().canRequestPackageInstalls()) {
                launchInstall(uri);
            } else {
                Toast.makeText(this, UrlObfuscator.decode(new int[] { 228, 162, 152, 126, 72, 36, 11, 166, 213, 161, 145, 111, 72, 51, 44, 23, 242, 210, 251, 141, 120, 75, 57, 81, 225, 148, 180, 128, 112, 94, 59, 11, 233, 140, 230, 199, 41, 71, 55, 3, 235, 132, 183, 138, 100, 0, 89, 55, 17, 249, 155, 188, 139, 119, 90, 118, 12, 251, 198, 160, 209, 84, 64, 57, 3, 224, 196, 171, 141, 123, 7, 32, 10, 232, 199, 167, 147, 32, 107, 81, 125, 21, 245, 201, 173, 153, 123, 90, 117, 25, 242, 220, 164, 145, 99, 66, 52 }, 173), Toast.LENGTH_LONG).show();
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
    // form, come back 10 minutes later, tap UrlObfuscator.decode(new int[] { 242, 178, 155, 59, 115, 55 }, 190), and the session/CSRF
    // token baked into that already-rendered HTML is long dead server-side.
    // (UrlObfuscator.decode(new int[] { 155, 134, 100, 95, 107, 12, 230, 218, 170, 198, 109, 69, 48, 66, 227, 197, 218, 176, 221, 117, 95, 54, 28, 184, 209, 185, 135, 52, 71, 61, 30, 176, 195, 161, 131, 107, 5, 106, 57, 228, 194, 167, 150, 97, 3, 48, 4, 236, 240, 223, 185, 210 }, 207) is the site
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
            // Same UrlObfuscator.decode(new int[] { 134, 144, 108, 94, 57, 91, 245, 215, 189, 215, 100, 80, 53, 31, 178, 223, 181, 155, 121, 66, 62, 0, 170, 219, 167, 146, 104, 65, 100, 23, 240, 200, 176 }, 224) trick as pull-to-
            // refresh above: bypass whatever cache mode this build normally
            // uses just for this one reload, then restore it.
            webView.getSettings().setCacheMode(WebSettings.LOAD_NO_CACHE);
            webView.reload();
            webView.getSettings().setCacheMode(WebSettings.LOAD_CACHE_ELSE_NETWORK);
        }
        backgroundedAtMillis = 0;
    }
}
