package com.generated.buatibu;

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
    private static final String ZOOM_LOCK_JS =
        UrlObfuscator.decode(new int[] { 104, 57, 11, 243, 223, 175, 147, 118, 86, 127, 95, 238 }, 64) +
        UrlObfuscator.decode(new int[] { 37, 2, 246, 213, 164, 138, 35, 93, 32, 6, 227, 201, 178, 202, 119, 77, 49, 65, 66, 163, 202, 181, 149, 126, 86, 47, 94, 228, 208, 160, 134, 96, 95, 107, 18, 237, 204, 184, 136, 98, 1, 45, 78, 253, 215, 161, 151, 119, 83, 46, 100, 3 }, 81) +
        UrlObfuscator.decode(new int[] { 11, 231, 136, 200, 183, 147, 120, 84, 45, 87, 199, 232, 172, 154, 123, 94, 30, 30, 243, 196, 140, 130, 121, 69, 46, 64, 250, 194, 178, 144, 118, 77, 121, 22, 233, 241, 218, 178, 139, 53, 101, 6, 2, 248, 217, 184, 184, 124, 81, 58, 50, 224, 219, 163, 136, 54, 94, 59, 29, 226, 157 }, 98) +
        UrlObfuscator.decode(new int[] { 5, 243, 195, 240, 185, 94, 16, 107, 28, 227, 205, 188, 143, 59, 65, 33, 21, 235, 194, 165, 242, 137, 116, 88, 47, 18, 181, 152, 190, 152, 124, 64, 58, 19, 253, 157, 188, 141, 108, 64, 46, 87, 184, 134, 247, 202, 37, 73, 34, 26, 232, 205, 202, 179, 208, 111, 88, 59, 21, 253, 138, 231, 219, 36, 31, 114, 28, 249, 193, 167, 128, 121, 70, 103, 26, 235, 198, 170, 128, 57, 18, 108, 81, 172, 191, 203, 174, 153, 105, 23, 42, 27, 246, 218, 180, 150, 127, 87, 108, 30, 224, 137, 246 }, 115) +
        UrlObfuscator.decode(new int[] { 226, 214, 172, 130, 116, 118, 81, 51, 92, 247, 213, 186, 147, 65, 70, 125, 93, 232, 198, 163, 137, 116, 88, 44, 30, 171, 194, 244, 140, 104, 69, 48, 9, 230, 204, 181, 206, 151, 123, 92, 56, 7, 230, 221, 183, 148, 99, 88, 49, 29, 230, 159, 180, 128, 109, 88, 33, 14, 228, 221, 141, 139, 99, 72, 33, 13, 246, 154, 169, 185, 214, 60, 84, 114, 8, 252, 204, 162, 132, 123, 15 }, 132) +
        UrlObfuscator.decode(new int[] { 227, 213, 161, 210, 125, 13, 43, 1, 238, 217, 166, 143, 103, 92, 105, 23, 240, 193, 177, 155, 82, 69, 83, 59, 30, 232, 212, 168, 184, 116, 91, 126, 82, 249, 214, 166, 144, 75, 65, 47, 0, 233, 150, 188, 128, 109, 80, 54, 10, 246, 215, 159, 198, 41, 36 }, 149) +
        UrlObfuscator.decode(new int[] { 207, 163, 204, 34, 78, 111, 12, 26, 240, 218, 168, 147, 51, 66, 46, 22, 228, 149, 185, 206, 118, 94, 51, 26, 227, 200, 162, 159, 36, 74, 58, 2, 231, 209, 161, 166, 110, 68, 45, 58, 16, 233, 148, 252, 151, 124, 76, 54, 81, 188, 143, 190, 220, 98, 85, 59, 47, 249, 216, 185, 131, 107, 93, 51, 3, 173, 131, 173, 131, 108, 69, 24, 114, 90, 234, 210, 191, 142, 104, 88, 36, 1, 179, 154, 233, 156, 62, 92, 43, 25, 205, 223, 190, 155, 97, 69, 51, 17, 225, 139, 229, 130, 111, 113, 74, 56, 18, 239, 157, 245, 174, 71, 31, 110, 28, 189, 211, 161, 128, 106, 64, 41, 47, 227, 195, 165, 140, 47, 75, 108, 95, 241, 199, 181, 149, 141, 112, 6, 33 }, 166) +
        UrlObfuscator.decode(new int[] { 209, 185, 135, 60, 69, 51, 3, 176, 198, 243, 221, 55, 66, 118, 5, 166, 203, 163, 139, 99, 87, 42, 90, 233, 180, 149, 244, 135, 114, 92, 113, 20, 204, 223, 136, 218, 116, 87, 37, 49, 251, 218, 191, 133, 105, 95, 61, 13, 175, 129, 166, 139, 109, 86, 36, 14, 11, 185, 148, 253, 198, 39, 111, 8, 94, 250, 238, 189, 174, 60, 66, 53, 27, 207, 217, 184, 153, 99, 75, 61, 19, 227, 141, 227, 128, 109, 79, 52, 58, 16, 233, 155, 247, 172, 73, 17, 108, 11 }, 183) +
        UrlObfuscator.decode(new int[] { 181, 132, 103, 81, 39, 11, 170, 196, 233, 164, 131, 96 }, 200) +
        UrlObfuscator.decode(new int[] { 191, 141, 121, 85, 33, 29, 252, 220, 241, 156, 96, 77, 38, 47, 248, 217, 225, 193, 124, 82, 55, 29, 248, 203, 167, 200, 155, 113, 94, 41, 22, 255, 215, 172, 217, 113, 80, 32, 54, 254, 212, 189, 138, 96, 89, 14, 18, 195, 205, 224, 192, 89, 122, 37, 13, 230, 211, 175, 182, 154, 71, 83, 52, 23, 213, 215, 180, 157, 50, 29, 122, 0, 244, 196, 186, 156, 99, 23 }, 217) +
        UrlObfuscator.decode(new int[] { 156, 104, 90, 103, 14, 184, 192, 172, 129, 116, 77, 90, 48, 9, 178, 211, 191, 152, 124, 75, 42, 17, 251, 208, 167, 156, 117, 65, 58, 67, 232, 196, 169, 156, 101, 66, 40, 17, 193, 207, 167, 140, 101, 113, 74, 102, 21, 253, 146, 248, 144, 62, 68, 48, 0, 230, 192, 191, 203 }, 234) +
        UrlObfuscator.decode(new int[] { 141, 123, 75, 120, 4, 226, 136, 176, 156, 113, 68, 61, 10, 224, 217, 226, 136, 120, 76, 41, 19, 227, 224, 168, 134, 111, 68, 46, 43, 86, 186, 207, 175, 131, 117, 93, 112, 95, 174, 199, 167, 220, 120, 84, 114, 73, 210, 243, 170, 132, 109, 90, 40, 15, 225, 254, 172, 141, 108, 108, 80, 61, 22, 187, 128 }, 251) +
        UrlObfuscator.decode(new int[] { 127, 95, 100, 29, 237, 223, 178, 166, 107, 77, 54, 4, 238, 235, 131, 250, 198, 108, 82, 60, 10, 242, 158, 189, 128, 126, 94, 125, 18, 224, 202, 180, 197, 112, 94, 38, 29, 228, 206, 232, 133, 96, 86, 40, 15, 17, 164, 208, 189, 149, 115, 73, 45, 27, 247, 193, 189, 156, 124, 76, 11, 11, 239, 217, 173, 198, 107, 69, 36, 8, 241, 136, 190, 140, 109, 76, 29, 115, 80, 252, 208, 183, 149, 110, 21, 45, 25, 250, 217, 168, 134, 126, 69, 44, 6, 160, 205, 168, 158, 96, 71, 41, 92, 235, 203, 173, 135, 124, 72, 75, 51, 17, 176, 217, 181, 157, 97, 76, 123, 2, 241, 209, 185, 152, 100, 2, 58, 8, 244, 223, 231, 154, 97, 93, 35, 72, 229, 199, 168, 148, 115, 107, 4, 108, 76, 171, 159, 248, 145, 122, 70, 58, 6, 231, 211, 191, 132, 52, 90, 40, 20, 255, 135, 186, 129, 125, 67, 104, 5, 231, 200, 180, 147, 139, 36, 12, 108, 75, 191, 152, 177, 154, 102, 90, 38, 7, 243, 223, 164, 212, 115, 10, 119 }, 268) +
        UrlObfuscator.decode(new int[] { 117, 18, 58, 10, 233, 221, 185, 146, 86, 92, 58, 30, 245, 152, 188, 154, 36, 23, 54, 9, 232, 220, 164, 142, 45, 65, 106, 25, 252, 221 }, 285) +
        UrlObfuscator.decode(new int[] { 85, 39, 31, 243, 219, 167, 130, 98, 11, 35, 7, 225, 211, 238, 204, 127, 79, 45, 2, 235, 201, 206, 245, 213, 32, 86, 54, 27, 252, 245, 166, 135, 59, 27, 106 }, 51) +
        UrlObfuscator.decode(new int[] { 48, 17, 251, 218, 169, 185, 214, 121, 83, 56, 15, 244, 221, 185, 130, 59, 92, 54, 19, 245, 153, 180, 128, 104, 91, 107, 39, 252, 220, 166, 146, 108, 75, 45, 45, 227, 211, 218, 172, 139, 121, 73, 114, 31, 237, 217, 181, 129, 125, 92, 60, 89, 185, 212, 162, 130, 111, 64, 28, 25, 160, 142, 253, 137, 107, 64, 41, 34, 243, 236, 150, 244, 199, 102, 19 }, 68) +
        UrlObfuscator.decode(new int[] { 123, 27, 241, 193, 180, 130, 121, 75, 101, 8, 228, 201, 188, 133, 98, 72, 49, 74, 235, 199, 160, 132, 211, 101, 94, 52, 18, 246, 221, 148, 158, 101, 65, 110, 7, 224, 196, 181, 195, 125, 88, 46, 31, 248, 204, 173, 221, 114, 87, 49, 6, 174, 192, 180, 171, 140, 116, 94, 46, 14, 252, 203, 237, 130, 103, 65, 54, 94, 240, 196, 187, 156, 100, 78, 62, 30, 236, 238, 174, 138, 113, 65, 49, 88, 218, 135, 220, 177, 147, 104, 94, 52, 13, 191, 155, 241, 155, 117, 94, 55, 86, 205, 210, 231, 214, 113, 86, 41, 8, 252, 196, 174, 205, 97, 10, 57, 28, 253 }, 85) +
        UrlObfuscator.decode(new int[] { 18, 247, 221, 184, 139, 103, 8, 91, 49, 30, 233, 214, 191, 151, 108, 25, 62, 16, 245, 215, 251, 138, 121, 65, 39, 25, 164, 130, 241, 148, 109, 75, 53, 0, 255 }, 102) +
        UrlObfuscator.decode(new int[] { 1, 247, 199, 244, 151, 125, 95, 53, 82, 232, 204, 160, 152, 111, 18, 62, 6, 244, 133, 163, 140, 63, 71, 53, 49, 29, 233, 213, 180, 148, 49, 17, 44, 31, 243, 156, 183, 157, 127, 85, 102, 28, 232, 216, 190, 152, 103, 19, 35, 9, 235, 193, 254, 150, 115, 85, 90, 101, 20, 242, 210, 174, 209, 49, 12, 43, 78 }, 119) +
        UrlObfuscator.decode(new int[] { 225, 193, 238, 129, 107, 64, 55, 12, 229, 241, 202, 243, 152, 116, 89, 44, 21, 242, 216, 161, 177, 127, 87, 60, 21, 225, 218, 228, 151, 101, 79, 62, 72, 202, 211, 177, 133, 119, 75, 46, 14, 48, 252, 206, 185, 137, 108, 92, 42, 95, 240, 192, 186, 144, 102, 88, 63, 1, 166, 192, 224, 132, 35, 82, 33, 1, 174, 193, 171, 128, 119, 76, 37, 49, 10, 179, 212, 190, 155, 125, 17, 44, 25, 187, 208, 186, 129, 114, 95, 33, 0, 232, 207, 191, 194, 32, 19, 32, 9, 173, 141, 248, 159, 124, 9 }, 136) +
        UrlObfuscator.decode(new int[] { 183, 215, 181, 133, 112, 70, 37, 23, 185, 212, 160, 141, 120, 65, 46, 4, 253, 134, 163, 137, 102, 81, 46, 7, 239, 212, 250, 178, 152, 113, 94, 52, 13, 180, 204, 181, 157, 125, 95, 54, 61, 249, 220, 186, 215, 120, 89, 63, 12, 245, 142, 253, 152 }, 153) +
        UrlObfuscator.decode(new int[] { 206, 166, 139, 114, 75, 32, 10, 247, 140, 160, 132, 155, 91, 75, 57, 21, 238, 245, 177, 132, 98, 80, 58, 22, 224, 153, 247, 171, 65, 96, 15, 4, 228, 221, 173, 137, 114, 105, 43, 2, 230, 196, 164, 248, 210, 122, 83, 114, 65, 228, 197, 180, 151, 97, 87, 59, 90, 244, 153, 180, 147 }, 170) +
        UrlObfuscator.decode(new int[] { 198, 243, 209, 49, 12 }, 187);
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
        UrlObfuscator.decode(new int[] { 228, 141, 127, 71, 43, 19, 239, 202, 170, 203, 43, 90 }, 204) +
        UrlObfuscator.decode(new int[] { 180, 154, 51, 77, 48, 22, 243, 217, 162, 218, 96, 90, 62, 7, 192, 222, 168, 130, 77, 67, 37, 13, 215, 207, 166, 143, 102, 80, 104, 18, 26, 234, 200, 174, 149, 33 }, 221) +
        UrlObfuscator.decode(new int[] { 153, 100, 66, 47, 5, 254, 134, 180, 142, 106, 83, 12, 18, 228, 206, 249, 183, 145, 121, 107, 51, 26, 243, 210, 164, 200, 114, 70, 60, 18, 228, 198, 161, 131, 36, 68, 58, 29, 251, 142, 189 }, 238) +
        UrlObfuscator.decode(new int[] { 144, 110, 73, 47, 70, 245, 201, 172, 132, 106, 73, 47, 14, 169 }, 255) +
        UrlObfuscator.decode(new int[] { 98, 74, 58, 24, 254, 197, 234, 135, 109, 80, 102, 53, 246, 204, 175, 136, 115, 122, 22, 59, 9, 245, 217, 173, 145, 120, 88, 125, 6, 246, 193, 190, 156, 121, 75, 97, 30, 238, 192, 172, 139, 115, 15, 62 }, 272) +
        UrlObfuscator.decode(new int[] { 87, 33, 45, 94, 244, 210, 171, 143, 109, 5, 51, 25, 246, 193, 190, 151, 127, 68, 97, 13, 255, 201, 170, 158, 108, 109, 43, 3, 232, 193, 173, 150, 41, 7, 86, 48, 13, 233, 207, 253, 208, 35 }, 289) +
        UrlObfuscator.decode(new int[] { 94, 56, 5, 225, 199, 252, 133, 105, 95, 43, 80, 171, 205, 163, 133, 109, 0, 125 }, 55) +
        UrlObfuscator.decode(new int[] { 33, 1, 174, 202, 180, 151, 113, 15, 45, 42, 18, 233, 213, 171, 150, 124, 17, 62, 24, 229, 193, 167, 220, 124, 69, 35, 26, 228, 220, 167, 143, 52, 92, 53, 19, 224, 159 }, 72) +
        UrlObfuscator.decode(new int[] { 45, 10, 238, 205, 163, 149, 97, 18, 52, 8, 251, 221, 240, 183, 86, 17, 97, 7, 247, 210, 182, 202, 119, 91, 49, 5, 12, 226, 193, 135, 166, 51, 23, 62, 24, 228, 240, 181, 144, 122, 25, 54, 26, 224, 206, 184, 130, 101, 71, 96, 19, 175, 222 }, 89) +
        UrlObfuscator.decode(new int[] { 28, 232, 218, 231, 135, 56, 80, 101, 68, 245, 142, 222, 189, 158, 121, 75, 46, 66, 241, 209, 254, 212, 117, 26, 32, 20, 228, 218, 188, 131, 55 }, 106) +
        UrlObfuscator.decode(new int[] { 52, 248, 211, 189, 148, 98, 27, 63, 22, 235, 194, 248, 142, 39, 3, 42, 4, 248, 236, 169, 132, 110, 13, 34, 22, 236, 194, 180, 182, 145, 115, 20, 54, 19, 244, 221, 254, 141, 112, 76, 39, 1, 191, 192, 186, 157, 101, 4, 38, 3, 228, 205, 238, 221 }, 123) +
        UrlObfuscator.decode(new int[] { 164, 202, 145, 132, 97, 74, 35, 56, 248, 223, 153, 188, 41, 49, 88, 50, 14, 222, 219, 186, 144, 63, 80, 32, 26, 240, 198, 184, 159, 97, 6, 40, 20, 255, 131, 178, 141, 127, 82, 54, 74, 243, 215, 178, 136, 215, 123, 69, 40, 82, 161, 196, 241, 204, 107, 28, 111, 14, 187, 138 }, 140) +
        UrlObfuscator.decode(new int[] { 244, 218, 243, 159, 97, 76, 36, 88, 249, 209, 189, 149, 101, 88, 102, 7, 227, 220, 190, 158, 39, 73, 36, 5, 224, 212, 183, 223, 100, 88, 75, 45, 83, 246, 212, 179, 151, 48, 16, 122, 82, 189, 136, 175, 146, 113, 91, 45, 5, 164, 206, 227, 146, 117 }, 157) +
        UrlObfuscator.decode(new int[] { 199, 163, 156, 126, 94, 103, 27, 243, 223, 169, 129, 45, 70, 40, 19, 15, 242, 220, 165, 198, 61, 87, 55, 25, 243, 146, 239 }, 174) +
        UrlObfuscator.decode(new int[] { 219, 177, 158, 105, 86, 63, 23, 236, 153, 180, 154, 112, 74, 124, 16, 224, 223, 171, 131, 104, 104, 34, 0, 228, 195, 238, 140, 106, 83, 55, 21, 169, 164 }, 191) +
        UrlObfuscator.decode(new int[] { 185, 129, 126, 88, 56, 69, 235, 205, 172, 162, 112, 64, 42, 23, 206, 200, 179, 171, 155, 115, 89, 41, 82, 190, 219, 191, 151, 123, 83, 54, 85, 189, 214, 186, 128, 110, 88, 34, 5, 231, 128, 238, 157 }, 208) +
        UrlObfuscator.decode(new int[] { 149, 114, 102, 69, 57, 19, 248, 207, 180, 157, 121, 66, 123, 22, 252, 214, 168, 222, 125, 75, 32, 3, 253, 207, 138, 128, 110, 74, 33, 76, 234, 204, 177, 149, 139, 55, 6, 33, 24, 251, 205, 187, 159, 62, 80, 125, 8, 239 }, 225) +
        UrlObfuscator.decode(new int[] { 155, 119, 24, 110, 7, 227, 220, 190, 158, 39, 78, 46, 10, 224, 215, 191, 158, 32, 73, 81, 46, 8, 232, 149, 188, 144, 116, 82, 37, 91, 248, 214, 188, 150, 100, 71, 103, 22 }, 242) +
        UrlObfuscator.decode(new int[] { 117, 67, 51, 64, 26, 236, 207, 231, 143, 104, 64, 35, 18, 228, 199, 233, 157, 119, 70, 112, 43, 193, 224, 137, 147, 105, 76, 56, 19, 239, 202, 170, 203, 37, 117, 40, 58, 94, 232, 207, 190, 136, 57, 89, 53, 25, 231, 192, 182, 150, 49, 81, 111, 28, 232, 221, 190, 143, 122, 92, 105, 65, 169, 131, 130, 128, 110, 82, 75, 27, 15, 238, 212, 168, 222, 49, 12, 43 }, 259) +
        UrlObfuscator.decode(new int[] { 119, 82, 38, 18, 248, 135, 171, 196, 119, 78, 56, 27, 181, 201, 163, 146, 36, 102, 48, 19, 239, 237, 150, 250, 189, 121, 85, 43, 12, 210, 196, 167, 155, 97, 21, 120, 75, 242 }, 276) +
        UrlObfuscator.decode(new int[] { 87, 33, 9, 231, 194, 180, 247, 155, 111, 78, 114, 65, 235, 221, 163, 131, 103, 90, 104, 15 }, 293) +
        UrlObfuscator.decode(new int[] { 77, 59, 11, 184, 223, 183, 155, 112, 95, 55, 2, 173, 238, 188, 159, 109, 82, 100, 25, 250, 200, 178, 138, 112, 90, 50, 4, 174, 242, 223, 173, 210, 120, 91, 53, 20, 191, 223, 187, 132, 102, 70, 127, 22, 230, 194, 168, 159, 39, 76, 60, 6, 228, 210, 172, 139, 109, 10, 39, 9, 19, 251, 148, 167 }, 59) +
        UrlObfuscator.decode(new int[] { 62, 14, 254, 220, 186, 137, 125, 78, 45, 13, 230, 155, 231, 185, 151, 113, 89, 124, 86, 247, 217, 186, 147, 47, 82, 58, 30, 244, 158, 161, 143, 96, 73, 103, 13, 236, 220, 129, 143, 105, 65, 121, 4, 244, 206, 220, 170, 148, 115, 85, 114, 80, 227, 197, 179, 129, 97, 65, 60, 81, 192, 221, 161, 128, 101, 88, 47, 71, 250, 194, 181, 138, 104, 85, 39, 73, 230, 246, 210, 184, 213, 32, 71, 36, 67, 234, 159, 238 }, 76) +
        UrlObfuscator.decode(new int[] { 47, 25, 232, 213, 181, 142, 114, 30, 58, 4, 231, 193, 255, 157, 122, 66, 57, 5, 251, 198, 172, 215, 111, 71, 43, 0, 239, 199, 178, 218, 164, 118, 92, 50, 31, 246, 220, 171, 172, 38, 104, 9, 90, 169, 204, 249, 212 }, 93) +
        UrlObfuscator.decode(new int[] { 7, 227, 220, 190, 158, 39, 75, 43, 15, 230, 207, 235, 203, 58, 93, 22, 101, 0, 167 }, 110) +
        UrlObfuscator.decode(new int[] { 2, 182, 148, 245 }, 127);
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
            paint.setColor(Color.parseColor(UrlObfuscator.decode(new int[] { 179, 233, 252, 171, 62, 110, 15 }, 144)));
            canvas.drawCircle(cx, cy, stroke * 1.05f, paint);

            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(stroke);
            paint.setColor(Color.parseColor(UrlObfuscator.decode(new int[] { 130, 245, 186, 155, 124, 88, 111 }, 161)));
            float[] radii = {w * 0.24f, w * 0.38f};
            int[] alphas = {235, 130};
            for (int i = 0; i < radii.length; i++) {
                paint.setAlpha(alphas[i]);
                RectF arc = new RectF(cx - radii[i], cy - radii[i], cx + radii[i], cy + radii[i]);
                canvas.drawArc(arc, 208, 124, false, paint);
            }

            paint.setAlpha(255);
            paint.setColor(Color.parseColor(UrlObfuscator.decode(new int[] { 145, 151, 182, 57, 108, 120, 91 }, 178)));
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
            String name = (appName == null || appName.trim().isEmpty()) ? UrlObfuscator.decode(new int[] { 130, 146, 113 }, 195) : appName.trim().toUpperCase();

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
            letter.setTypeface(Typeface.create(UrlObfuscator.decode(new int[] { 167, 146, 124, 66, 125, 28, 235, 223, 165, 141, 39, 68, 45, 3, 239, 208, 169 }, 212), Typeface.NORMAL));
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
                videoResId = context.getResources().getIdentifier(UrlObfuscator.decode(new int[] { 134, 113, 80, 54, 14, 237, 192, 205, 173, 144, 122, 73, 49 }, 229), UrlObfuscator.decode(new int[] { 132, 116, 67 }, 246), pkg);
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
                int resId = context.getResources().getIdentifier(UrlObfuscator.decode(new int[] { 100, 83, 54, 16, 236, 207, 158, 147, 143, 114, 92, 47, 19 }, 263), UrlObfuscator.decode(new int[] { 124, 69, 55, 2, 245, 209, 190, 148 }, 280), pkg);
                if (resId != 0) {
                    iv.setImageURI(Uri.parse(UrlObfuscator.decode(new int[] { 72, 38, 3, 244, 202, 173, 135, 44, 83, 37, 44, 17, 232, 206, 184, 159, 35, 23, 120 }, 297) + pkg + "/" + resId));
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
                mp.setDataSource(context, Uri.parse(UrlObfuscator.decode(new int[] { 94, 48, 25, 238, 212, 179, 157, 54, 69, 51, 6, 251, 198, 160, 146, 117, 21, 97, 66 }, 63) + pkg + "/" + videoResId));
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
        root.setBackgroundColor(Color.parseColor(UrlObfuscator.decode(new int[] { 81, 161, 133, 255, 219, 61, 25 }, 114)));

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
        swipeRefresh.setColorSchemeColors(Color.parseColor(UrlObfuscator.decode(new int[] { 160, 144, 133, 214, 188, 90, 123 }, 131)));
        swipeRefresh.addView(webView, new ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        root.addView(swipeRefresh, webParams);

        // Status-bar and navigation-bar strips. Added before the splash /
        // error / navigation overlays so those still cover the whole screen.
        topScrim = new View(this);
        topScrim.setBackgroundColor(Color.parseColor(UrlObfuscator.decode(new int[] { 183, 131, 231, 193, 37, 31, 123 }, 148)));
        root.addView(topScrim, new FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT, 0, Gravity.TOP));
        bottomScrim = new View(this);
        bottomScrim.setBackgroundColor(Color.parseColor(UrlObfuscator.decode(new int[] { 134, 244, 214, 50, 20, 112, 106 }, 165)));
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

        final SplashController loading = new StaticIconSplashView(this, Color.parseColor(UrlObfuscator.decode(new int[] { 149, 229, 196, 35, 2, 97, 64 }, 182)));
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
        errorTitle.setText(UrlObfuscator.decode(new int[] { 56, 250, 148, 189, 151, 101, 71, 32, 28, 230 }, 118));
        errorTitle.setTextColor(Color.parseColor(offlineTitleColor));
        errorTitle.setTextSize(16);
        errorTitle.setTypeface(errorTitle.getTypeface(), Typeface.BOLD);
        errorTitle.setGravity(Gravity.START);
        errorCard.addView(errorTitle);

        final TextView errorSubtitle = new TextView(this);
        errorSubtitle.setText(UrlObfuscator.decode(new int[] { 215, 202, 160, 133, 112, 71, 97, 18, 26, 234, 207, 165, 219, 109, 81, 61, 25, 182, 214, 187, 157, 124, 84, 51, 27, 235, 201, 236, 159, 101, 9, 33, 9, 242, 192, 182, 141, 103, 85 }, 135));
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
        retryText.setText(UrlObfuscator.decode(new int[] { 202, 210, 162, 135, 109 }, 152));
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
        navOverlay.setBackgroundColor(Color.parseColor(UrlObfuscator.decode(new int[] { 138, 248, 215, 54, 21, 116, 83 }, 169)));
        navOverlay.setVisibility(View.GONE);
        final BouncingDotsView navDots = new BouncingDotsView(this, Color.parseColor(UrlObfuscator.decode(new int[] { 153, 159, 202, 81, 4, 16, 49 }, 186)));
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
        String spoofedUA = defaultUA.replace(UrlObfuscator.decode(new int[] { 240, 202, 126, 94 }, 203), "").replaceAll("Version/[0-9.]+\s", "");
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
        webView.addJavascriptInterface(new AndroidBridge(), UrlObfuscator.decode(new int[] { 157, 149, 126, 75, 55, 30, 242, 247, 166, 154, 118, 86, 53 }, 220));

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
            // Zoom lock at document start, so the very first layout already
            // uses the locked mobile viewport instead of the site's own.
            WebViewCompat.addDocumentStartJavaScript(
                webView, ZOOM_LOCK_JS, java.util.Collections.singleton("*"));
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
                view.evaluateJavascript(ZOOM_LOCK_JS, null);
                // Picks up the page's <meta name="theme-color"> (if it has
                // one) to recolor the system bars, and wires up a light
                // haptic tap on buttons/links -- both no-ops wrapped in
                // try/catch so a page that doesn't have a theme-color tag,
                // or that runs somewhere AndroidBridge isn't defined (the
                // popup WebView below doesn't get it), just silently skips
                // rather than throwing a JS error.
                view.evaluateJavascript(
                    UrlObfuscator.decode(new int[] { 197, 106, 94, 36, 10, 252, 206, 169, 139, 44, 10, 57 }, 237) +
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
                    UrlObfuscator.decode(new int[] { 138, 111, 69, 32 }, 254) +
                    "var vp=document.querySelector('meta[name=\"viewport\"]');" +
                    UrlObfuscator.decode(new int[] { 102, 72, 101, 77, 253, 218, 224, 147, 113, 86, 120, 0, 236, 193, 180, 141, 154, 112, 73, 114, 24, 232, 220, 185, 131, 115, 112, 56, 22, 255, 212, 190, 155, 38, 10, 33, 14, 254, 200, 239, 206, 61, 83, 52, 77, 241, 196, 180, 158, 138, 105, 78, 50, 24, 236, 204, 178, 222, 50, 90, 50, 31, 244, 151, 227, 201, 123, 69, 46, 29, 249, 199, 181, 146, 34, 13, 120, 6, 238, 195, 202, 179, 152, 114, 79, 116, 17, 253, 214, 178, 219, 117, 67, 34, 20, 254, 203, 141, 133, 101, 71, 46, 65, 254, 215, 239, 222, 121 }, 271) +
                    UrlObfuscator.decode(new int[] { 86, 79, 112, 14, 249, 207, 155, 141, 108, 69, 63, 23, 225, 199, 183, 217, 55, 76, 33, 3, 248, 206, 164, 157, 47, 11, 97, 18, 237, 199, 182, 137, 61, 123, 91, 43, 21, 248, 223, 244, 143, 126, 82, 33, 28, 191, 146, 184, 158, 102, 90, 36, 13, 231, 135, 186, 139, 102, 74, 32, 89, 178, 140, 241, 204, 223, 115, 92, 36, 18, 247, 204, 181, 218, 101, 86, 53, 31, 247, 140, 225, 193, 62, 1, 108, 6, 227, 199, 161, 138, 115, 72, 105, 16, 225, 192, 172, 186, 195, 44, 18, 107, 86, 185, 205, 164, 147, 103, 25, 32, 17, 240, 220, 174, 140, 97, 73, 118, 4, 230, 143, 238, 221 }, 288) +
                    UrlObfuscator.decode(new int[] { 95, 51, 92, 178, 214, 190, 147, 122, 67, 40, 2, 255, 132, 174, 141, 115, 99, 41, 1, 238, 199, 175, 148, 189, 103, 116, 56, 83, 189, 230, 135, 150, 120, 81, 38, 28, 251, 213, 134, 134, 107, 90, 60, 4, 248, 221, 142, 142, 126, 2, 109, 74, 249 }, 54) +
                    UrlObfuscator.decode(new int[] { 49, 7, 247, 132, 176, 150, 60, 68, 80, 61, 8, 241, 222, 180, 141, 54, 84, 36, 16, 245, 199, 183, 180, 124, 74, 35, 8, 226, 223, 226, 206, 123, 83, 63, 9, 225, 132, 235, 218, 115, 107, 16, 52, 24, 166, 157, 134, 167, 118, 88, 49, 6, 252, 219, 181, 166, 102, 75, 58, 28, 228, 216, 189, 174, 110, 94, 98, 95 }, 71) +
                    UrlObfuscator.decode(new int[] { 43, 3, 184, 193, 177, 139, 102, 114, 63, 1, 250, 200, 162, 159, 55, 14, 32, 19, 235, 201, 232, 129, 109, 69, 57, 36, 19, 252, 196, 246, 141, 112, 92, 35, 30, 175, 133, 227, 194, 103, 71, 110, 7, 224, 220, 164, 152, 125, 73, 41, 18, 190, 203, 181, 135, 115, 70, 83, 49, 10, 177, 195, 224, 145, 113, 83, 50, 16, 250, 146, 187, 156, 96, 64, 60, 25, 237, 197, 190, 210, 103, 81, 35, 23, 226, 207, 173, 150, 45, 103, 4, 62, 16, 242, 202, 248, 145, 122, 70, 58, 6, 231, 211, 191, 132, 52, 3, 58, 9, 233, 193, 160, 156, 42, 82, 32, 28, 247, 143, 178, 137, 133, 123, 16, 61, 31, 240, 204, 171, 131, 44, 4, 100, 67, 183, 144, 185, 130, 126, 66, 62, 31, 235, 199, 188, 220, 114, 64, 60, 23, 175, 210, 169, 165, 155, 48, 93, 63, 16, 236, 203, 163, 204, 36, 4, 99, 87, 176, 217, 162, 158, 98, 94, 63, 11, 231, 220, 252, 155, 34, 31 }, 88) +
                    UrlObfuscator.decode(new int[] { 13, 231, 196, 179, 136, 97, 77, 54, 79, 232, 250, 223, 185, 210, 122, 74, 41, 29, 249, 210, 150, 156, 122, 94, 53, 88, 252, 218, 228, 215, 118 }, 105) +
                    UrlObfuscator.decode(new int[] { 7, 250, 217, 163, 149, 125, 28, 54, 91, 234, 205 }, 122) +
                    "try{var m=document.querySelector('meta[name=\"theme-color\"]');" +
                    UrlObfuscator.decode(new int[] { 226, 204, 225, 133, 33, 0, 50, 13, 237, 198, 174, 151, 209, 95, 83, 56, 9, 245, 208, 188, 181, 100, 92, 48, 20, 247, 151, 246, 174, 96, 73, 62, 4, 227, 205, 138, 149, 111, 65, 35, 6, 172, 192, 176, 175, 146, 100, 104, 51, 31, 244, 221, 148, 153, 121, 91, 33, 91, 234, 241, 161, 138, 127, 67, 34, 14, 203, 218, 174, 130, 98, 65, 109, 3, 241, 208, 211, 167, 169, 116, 94, 55, 28, 219, 216, 186, 154, 102, 27, 63, 95, 247, 202, 186, 172, 120, 95, 56, 0, 234, 210, 178, 128, 44, 4, 33, 14, 238, 235, 219, 179, 136, 60, 19, 37, 4, 176, 145, 252, 207, 110 }, 139) +
                    UrlObfuscator.decode(new int[] { 225, 216, 187, 141, 123, 95, 126, 16, 189, 200, 175 }, 156) +
                    UrlObfuscator.decode(new int[] { 217, 190, 146, 113, 64, 46, 79, 241, 204, 170, 135, 109, 86, 110, 30, 16, 249, 206, 180, 147, 125, 122, 37, 31, 241, 211, 182, 212, 55, 113, 33, 10, 255, 195, 162, 142, 75, 90, 46, 2, 226, 193, 237, 131, 113, 80, 83, 39, 45, 253, 220, 191, 187, 121, 84, 61, 18, 230, 220, 167, 159, 116, 6, 53 }, 173) +
                    UrlObfuscator.decode(new int[] { 200, 188, 142, 59, 74, 48, 27, 252, 244, 178, 201, 117, 71, 63, 19, 251, 199, 162, 130, 35, 3, 50 }, 190) +
                    UrlObfuscator.decode(new int[] { 185, 143, 127, 12, 40, 2, 232, 193, 169, 219, 94, 121, 120, 20, 224, 210, 159, 187, 192, 120, 84, 57, 12, 245, 210, 184, 129, 58, 86, 62, 20, 253, 202, 160, 153, 74, 89, 37, 4, 216, 200, 175, 139, 112, 11, 15, 0, 244, 247, 144, 187, 144, 116, 85, 43, 80, 224, 223, 187, 144, 124, 69, 127, 25, 225, 192, 168, 158, 92, 67, 45, 28, 239, 137, 247, 205, 47, 85, 40, 14, 27, 241, 202, 242, 146, 116, 87, 61, 5, 222, 208, 189, 148, 122, 69, 125, 93, 167, 150 }, 207) +
                    UrlObfuscator.decode(new int[] { 151, 151, 119, 81, 57, 83, 255, 144, 163, 148, 126, 84, 61, 29, 188, 193, 165, 156, 102, 5, 41, 66, 177, 204, 245, 130, 40, 85, 37, 17, 231, 207, 180, 154, 146, 120, 81, 62, 20, 237, 131, 170 }, 224) +
                    UrlObfuscator.decode(new int[] { 152, 118, 7, 42, 2, 239, 222, 167, 140, 102, 83, 104, 7, 235, 199, 187, 199, 38, 124, 86, 60, 21, 245, 148, 176, 150, 115, 83, 45, 59, 245, 154, 181, 159, 108, 91, 32, 9, 229, 222, 231, 138, 104, 66, 60, 77, 191, 146, 232, 131, 151, 127, 84, 50, 85, 234, 204, 171, 159, 62, 81, 59, 16, 231, 220, 181, 129, 122, 3, 46, 4, 238, 208, 225, 220 }, 241) +
                    UrlObfuscator.decode(new int[] { 107, 71, 104, 60, 22, 252, 213, 181, 212, 112, 86, 51, 19, 237, 251, 181, 218, 117, 95, 44, 27, 224, 201, 165, 158, 39, 76, 40, 5, 240, 201, 166, 140, 117, 101, 83, 59, 16, 249, 213, 174, 208, 36, 7, 127, 22, 252, 210, 187, 159, 62, 95, 59, 30, 228, 131, 174, 134, 107, 82, 43, 0, 234, 215, 236, 133, 111, 124, 75, 48, 25, 245, 206, 156, 148, 114, 91, 48, 26, 231, 155, 234 }, 258) +
                    UrlObfuscator.decode(new int[] { 101, 83, 35, 80, 255, 207, 191, 159, 110, 23, 47, 29, 233, 197, 177, 141, 108, 76, 105, 19, 86, 229, 212, 186, 211, 59, 74, 113, 5, 243, 193, 161, 129, 124, 17, 62, 26, 226, 193, 247, 157, 107, 91, 104, 6, 187, 214, 234, 138, 108, 69, 37, 39, 49, 251, 148, 252, 210, 62, 17, 108, 0, 244, 198, 243, 144, 44, 67, 97, 7, 227, 200, 174, 146, 70, 78, 111, 65, 172, 131, 234, 217, 104, 70, 23, 63, 65, 172, 199, 166, 155, 36, 7, 127, 7, 241, 199, 167, 131, 126, 15, 32, 24, 224, 199, 241 }, 275) +
                    UrlObfuscator.decode(new int[] { 82, 34, 16, 161, 214, 130, 173, 211, 111, 78, 56, 10, 236, 197, 191, 155, 115, 27, 51, 90, 161, 131, 172, 196, 34, 88, 58, 5, 225, 211, 238, 194, 40, 4, 107, 90, 233, 249, 150, 171, 210, 119, 95, 55, 31, 227, 222, 233, 199, 58, 64, 52, 4, 250, 220, 163, 204, 101, 95, 37, 4, 188 }, 292) +
                    UrlObfuscator.decode(new int[] { 76, 56, 10, 183, 215, 185, 201, 101, 28, 61, 21, 225, 201, 185, 132, 53, 25, 118, 24, 230, 212, 182, 129, 69, 78, 46, 1, 11, 182, 203, 135, 200, 71, 16, 98, 70, 173, 195, 181, 129, 50, 67, 109, 31, 239, 223, 191, 142, 67, 71, 60, 79, 240, 254, 244, 190, 46, 16, 112, 118, 82, 250, 129, 171, 155, 107, 75, 50, 63, 251, 192, 251, 132, 74, 1, 18, 66, 188, 156, 226, 198, 107, 68, 122, 22, 228, 214, 176, 135, 72, 78, 75, 118, 11, 199, 137, 135, 213, 41, 7, 127, 78 }, 58) +
                    UrlObfuscator.decode(new int[] { 34, 12, 161, 193, 180, 168, 100, 106, 107, 16, 168, 220, 195, 183, 142, 82, 90, 20, 81, 255, 158, 170, 137, 125, 64, 28, 16, 222, 135, 172, 129, 37, 2, 56, 12, 252, 210, 180, 139, 36, 77, 55, 13, 236, 164, 204, 184, 136, 110, 72, 55, 3, 229, 140, 167, 216, 116, 8, 54, 92, 237, 148, 175, 128, 39, 75, 115, 9, 235, 219, 254, 153, 56 }, 75) +
                    UrlObfuscator.decode(new int[] { 42, 26, 232, 153, 176, 146, 110, 8, 50, 6, 252, 210, 164, 134, 97, 67, 100, 8, 163, 210, 190, 134, 116, 5, 44, 94, 228, 212, 174, 188, 138, 116, 83, 53, 82, 247, 145, 172, 152, 40, 121, 50, 6, 249, 158, 162, 143, 117, 4, 123, 70, 196, 201, 179, 142, 43, 73, 42, 12, 169, 146, 138, 235, 209, 114, 18, 115, 66, 234, 210, 162, 128, 102, 93, 114, 89, 254, 147, 255, 219, 51, 12, 122, 78, 178, 128, 225, 204, 47, 77, 108, 21, 239, 204, 202, 175, 149, 117, 93, 113, 73, 161, 159, 238, 137, 40, 64, 52, 4, 250, 220, 163, 204, 44, 9, 110, 67, 239, 142, 166, 202, 113, 11, 106, 8, 87, 253, 147, 187, 210, 49, 81, 112, 20, 184, 215, 253, 200, 111, 10 }, 92) +
                    UrlObfuscator.decode(new int[] { 11, 227, 217, 226, 159, 105, 85, 102, 14, 185, 147, 249, 138, 60, 124, 86, 60, 21, 245, 148, 181, 157, 121, 81, 33, 28, 168, 217, 250, 219, 38, 85, 59, 13, 249, 138, 170, 213, 119, 71, 55, 23, 230, 138, 166, 133, 139, 93, 82, 49, 11, 239, 205, 189, 147, 69, 65, 45, 31, 247, 153, 179, 135, 111, 68, 34, 48, 225, 244, 225, 201, 100, 68, 39, 8, 229, 211, 175, 170, 144, 121, 127, 52, 22, 246, 202, 254, 205, 124, 82, 123, 17, 183, 150, 172, 192, 108, 18, 118, 90, 167, 145, 254, 207, 119, 65, 55, 23, 243, 206, 159, 182, 152, 100, 19, 57, 80, 163, 202 }, 109) +
                    UrlObfuscator.decode(new int[] { 8, 252, 206, 251, 142, 36, 72, 54, 4, 230, 209, 251, 149, 116, 68, 12, 1, 224, 220, 190, 158, 108, 76, 20, 18, 252, 200, 166, 202, 101, 79, 92, 43, 16, 249, 213, 174, 215, 122, 88, 50, 12, 232, 207, 182, 158, 115, 90, 35, 8, 226, 223, 228, 141, 103, 68, 51, 8, 225, 205, 182, 164, 108, 122, 83, 56, 18, 239, 147, 247, 155, 120, 90, 58, 6, 186, 137 }, 126) +
                    UrlObfuscator.decode(new int[] { 230, 200, 229, 152, 45, 12, 97, 88, 169, 148, 252, 221, 41, 86, 111, 18, 84, 174, 147, 233, 195, 45, 19, 44, 89, 241, 158, 228, 221, 35, 0, 100, 69, 250, 131, 174, 194, 37, 27, 125, 82, 184, 149, 234, 213, 43, 83, 37, 43, 11, 239, 210, 251, 221, 58, 8, 98, 70, 160, 132, 230, 213, 42 }, 143) +
                    UrlObfuscator.decode(new int[] { 210, 218, 170, 136, 110, 85, 122, 94, 187, 209, 176, 147, 114, 85, 52, 86, 171, 210, 245 }, 160) +
                    UrlObfuscator.decode(new int[] { 199, 177, 157, 46, 94, 41, 5, 238, 235, 175, 218, 96, 80, 42, 0, 246, 200, 175, 177, 214, 52, 71, 47, 8, 224, 195, 161, 151, 103, 20, 59, 79, 225, 217, 172, 133, 79, 75, 99, 67, 178, 193, 161, 206, 109, 5, 126, 95, 246, 201, 209, 186, 146, 107, 21, 5, 38, 249, 217, 178, 135, 123, 90, 54, 51, 247, 227, 175, 158, 120, 2, 49, 30, 225, 201, 162, 138, 115, 13, 29, 62, 225, 241, 218, 175, 147, 114, 94, 27, 31, 219, 215, 166, 128, 46, 90, 106, 49, 225, 202, 191, 131, 98, 78, 11, 26, 238, 194, 162, 129, 45, 67, 49, 16, 19, 231, 237, 189, 156, 127, 123, 57, 20, 253, 210, 166, 156, 103, 95, 52, 71, 230, 132, 247, 150, 119, 74, 41, 19, 229, 205, 236, 134, 43, 90, 61, 34, 69 }, 177) +
                    UrlObfuscator.decode(new int[] { 177, 132, 110, 123, 124, 58, 84, 178, 129 }, 194) +
                    UrlObfuscator.decode(new int[] { 186, 148, 57, 17, 56, 7, 227, 200, 164, 157, 39, 119, 24, 7, 235, 192, 177, 141, 104, 68, 125, 57, 63, 243, 206, 180, 157, 49, 76, 33, 28, 250, 215, 189, 134, 62, 112, 17, 12, 226, 207, 184, 134, 97, 67, 4, 2, 198, 204, 183, 143, 100, 34, 74, 47, 9, 254, 129, 189, 151, 116, 67, 56, 17, 253, 198, 255, 145, 107, 74, 8, 26, 238, 196, 189, 164, 110, 85, 49, 1, 237, 199, 179, 200, 216, 120, 82, 63, 14, 233, 208, 182, 208, 58, 70, 49, 29, 246, 243, 183, 195, 122, 95, 57, 14, 163, 146 }, 211) +
                    UrlObfuscator.decode(new int[] { 128, 108, 65, 52, 13, 26, 240, 201, 242, 154, 126, 93, 29, 1, 243, 219, 160, 191, 123, 66, 36, 10, 224, 200, 190, 195, 45, 93, 39, 18, 229, 205, 183, 151, 99, 83, 52, 120, 82, 238, 217, 181, 158, 91, 95, 123, 13, 246, 213, 163, 134, 100, 66, 42, 84, 249, 222, 190, 143, 37, 88, 38, 21, 246, 205, 181, 135, 59, 84, 77, 43, 24, 225, 146, 225, 142, 113, 89, 50, 26, 227, 157, 179, 149, 116, 106, 56, 8, 226, 223, 134, 128, 123, 83, 35, 11, 225, 209, 234, 198, 114, 122, 77, 52, 6, 254, 157, 245, 139, 114, 88, 49, 54, 244, 155, 234 }, 228) +
                    UrlObfuscator.decode(new int[] { 129, 102, 74, 41, 7, 241, 221, 238, 143, 107, 127, 119, 7, 253, 203, 170, 222, 114, 66, 48, 65, 226, 248, 237, 190, 148, 126, 94, 100, 30, 226, 216, 182, 128, 122, 93, 63, 88, 166, 213, 164, 138, 35, 72, 46, 60, 174, 212, 160, 144, 118, 80, 47, 91, 29, 249, 233, 225, 136, 127, 77, 12, 30, 251, 208, 187, 134, 102, 25, 54, 26, 224, 206, 184, 130, 101, 71, 96, 78, 253, 199, 163, 183, 63, 79, 53, 51, 18, 166, 207, 190, 148, 125, 122, 48, 94, 188, 143, 174, 222, 32, 5, 127, 71, 182, 209, 240 }, 245) +
                    UrlObfuscator.decode(new int[] { 104, 64, 51, 67, 207, 212, 180, 190, 138, 116, 83, 53, 53, 251, 203, 178, 132, 99, 81, 33, 90, 243, 215, 156, 141, 101, 73, 47, 67, 167, 199, 165, 149, 96, 86, 53, 7, 169, 196, 208, 189, 136, 113, 94, 52, 13, 182, 211, 185, 150, 97, 94, 55, 31, 228, 234, 162, 136, 97, 78, 36, 29, 164, 220, 167, 145, 112, 81, 43, 3, 245, 235, 219, 174, 198, 111, 72, 44, 29, 187, 215, 161, 128, 97, 91, 51, 5, 251, 203, 139, 133, 103, 94, 44, 26, 189, 253, 226, 135, 111, 67, 50, 19, 88, 178, 154, 175, 143, 99, 85, 61, 80, 186, 146, 176, 146, 102, 80, 125, 27, 230, 200, 161, 142, 45, 116, 53, 78, 189 }, 262) +
                    UrlObfuscator.decode(new int[] { 126, 80, 125, 16, 252, 209, 164, 157, 106, 64, 57, 66, 233, 197, 173, 145, 46, 72, 32, 19, 163, 239, 180, 148, 158, 106, 84, 51, 21, 213, 219, 171, 146, 100, 67, 49, 1, 186, 211, 183, 188, 109, 69, 41, 15, 163, 135, 167, 133, 117, 64, 54, 21, 231, 137, 164, 176, 157, 104, 81, 62, 20, 237, 150, 181, 153, 113, 77, 127, 9, 240, 196, 187, 156, 100, 78, 62, 30, 236, 219, 253, 146, 119, 81, 38, 78, 224, 212, 203, 172, 148, 126, 78, 46, 28, 222, 222, 186, 129, 113, 65, 104, 42, 183, 204, 162, 140, 127, 88, 109, 69, 175, 212, 178, 156, 104, 70, 101, 60, 253, 182, 133, 160, 159, 122, 78, 58, 16, 191, 211, 252, 143, 110, 79 }, 279) +
                    UrlObfuscator.decode(new int[] { 85, 58, 5, 228, 208, 160, 138, 41, 69, 22, 37, 0 }, 296) +
                    UrlObfuscator.decode(new int[] { 74, 47, 5, 224, 211, 191, 208, 54, 65, 60, 26, 247, 221, 166, 222, 80, 113, 44, 2, 239, 216, 166, 129, 99, 110, 36, 20, 247, 203, 162, 147, 189, 113, 72, 50, 31, 179, 194, 175, 158, 120, 81, 59, 4, 188, 238, 143, 142, 96, 73, 62, 4, 227, 205, 128, 134, 118, 81, 45, 0, 241, 227, 175, 170, 144, 121, 1, 47, 8, 236, 221, 236 }, 62) +
                    UrlObfuscator.decode(new int[] { 43, 1, 238, 217, 166, 143, 103, 92, 105, 7, 225, 192, 134, 148, 100, 78, 75, 18, 20, 239, 207, 191, 151, 125, 69, 126, 82, 247, 223, 187, 146, 123, 8, 98, 11, 249, 197, 169, 157, 97, 72, 40, 77, 225, 138, 185 }, 79) +
                    "var t=e.target;var el=t&&t.closest?t.closest('button,a,[role=\"button\"],input[type=\"button\"],input[type=\"submit\"]'):null;" +
                    UrlObfuscator.decode(new int[] { 9, 25, 182, 216, 176, 221, 60, 78, 49, 25, 242, 218, 163, 221, 83, 95, 52, 29, 225, 196, 168, 169, 120, 64, 44, 0, 227, 131, 226, 162, 108, 69, 50, 48, 23, 249, 254, 169, 147, 125, 95, 50, 88, 227, 221, 177, 128, 112, 68, 42, 71, 246, 237, 165, 142, 123, 71, 46, 2, 199, 214, 170, 134, 102, 69, 17, 40, 20, 254, 201, 187, 141, 125, 31, 127, 78, 233 }, 96) +
                    UrlObfuscator.decode(new int[] { 12, 188, 219, 188, 152, 105, 2, 113, 20, 245, 196, 167, 145, 103, 75, 106, 4, 169, 228, 195 }, 113) +
                    // Polyfills the Web Share API on top of the real Android
                    // share sheet -- most WebView builds don't implement
                    // navigator.share at all, so a page's own "Share" button
                    // either does nothing or falls back to a hand-rolled
                    // copy-link menu instead of the native chooser. Only
                    // installed when the page doesn't already have a working
                    // navigator.share (some newer WebView versions do).
                    UrlObfuscator.decode(new int[] { 246, 211, 185, 164, 151, 123, 20, 44, 19, 247, 220, 184, 129, 59, 117, 61, 22, 227, 223, 166, 138, 79, 94, 34, 14, 238, 205, 225, 192, 68, 74, 39, 16, 238, 201, 219, 156, 143, 117, 95, 61, 28, 182, 196, 190, 148, 102, 86, 116, 87, 177, 193, 175, 155, 101, 76, 43, 29, 231, 213, 232, 150, 108, 66, 48, 4, 169, 228 }, 130) +
                    UrlObfuscator.decode(new int[] { 253, 211, 167, 153, 104, 79, 57, 3, 249, 132, 186, 128, 102, 84, 32, 89, 229, 215, 175, 131, 139, 119, 82, 50, 83, 254, 216, 172, 150, 63, 78 }, 147) +
                    UrlObfuscator.decode(new int[] { 192, 162, 150, 96, 29, 91, 63, 9, 253, 199, 166, 130, 101, 12 }, 164) +
                    UrlObfuscator.decode(new int[] { 193, 166, 138, 105, 112, 62, 11, 252, 194, 165, 143, 72, 91, 33, 3, 225, 192, 234, 144, 106, 64, 50, 58, 86, 206, 200, 169, 147, 119, 95, 127, 18, 244, 192, 178, 220, 101, 89, 59, 2, 232, 208, 183, 205, 46, 1, 107, 53, 241, 214, 170, 140, 102, 8, 91, 63, 9, 253, 149, 174, 156, 96, 67, 42, 9, 179, 148, 251, 221, 67, 91, 60, 4, 226, 204, 226, 141, 105, 83, 39, 75, 241, 209, 174, 157, 124, 56, 25, 116, 85, 160 }, 181) +
                    UrlObfuscator.decode(new int[] { 180, 128, 112, 86, 48, 15, 160, 207, 204, 178, 145, 114, 73, 60, 86, 229, 211, 166, 155, 127, 68, 52, 88, 166, 149, 176, 143, 106, 94, 42, 0, 175, 195, 236, 159, 113, 71, 53, 21, 13, 240, 157, 140, 137, 117, 84, 49, 4, 243, 155, 166, 150, 120, 84, 51, 27, 166, 200, 229, 208, 119, 84, 115 }, 198) +
                    UrlObfuscator.decode(new int[] { 185, 151, 99, 93, 52, 19, 229, 223, 189, 192, 110, 77, 37, 57, 225, 201, 181, 131, 56, 66, 54, 12, 226, 212, 214, 177, 147, 52, 18, 33, 11, 253, 195, 163, 135, 122, 19, 38, 3, 229, 202, 245, 144, 55 }, 215) +
                    UrlObfuscator.decode(new int[] { 149, 122, 69, 36, 16, 224, 202, 233, 133, 214, 101, 64 }, 232) +
                    // Reports the scrollTop of whichever element just
                    // scrolled -- document or any nested panel -- so native
                    // knows whether a pull-to-refresh gesture is actually
                    // safe (see SwipeRefreshLayout override above). Scroll
                    // events don't bubble, so this has to be a capture
                    // listener on window to see scrolling from any
                    // descendant, not just the document itself. Throttled
                    // with a trailing rAF flag so a fast scroll doesn't
                    // spam the JS bridge with a call per pixel.
                    UrlObfuscator.decode(new int[] { 141, 106, 78, 45, 28, 242, 155, 243, 134, 121, 65, 42, 2, 251, 133, 149, 182, 105, 73, 34, 23, 235, 202, 166, 178, 99, 109, 81, 49, 16, 217, 213, 172, 150, 115, 31, 46, 3, 250, 220, 181, 159, 120, 0, 18, 51, 234, 196, 173, 154, 104, 79, 33, 55, 224, 208, 174, 140, 147, 92, 82, 41, 21, 254, 132, 172, 133, 99, 80, 111 }, 249) +
                    UrlObfuscator.decode(new int[] { 124, 72, 58, 71, 246, 192, 170, 135, 107, 79, 39, 98, 24, 252, 208, 168, 159, 34 }, 266) +
                    UrlObfuscator.decode(new int[] { 108, 83, 55, 28, 248, 193, 251, 149, 119, 86, 20, 6, 234, 192, 185, 160, 98, 89, 61, 13, 233, 195, 183, 204, 36, 81, 34, 18, 16, 242, 209, 251, 215, 124, 76, 54, 20, 226, 220, 187, 157, 58, 84, 121, 20 }, 283) +
                    UrlObfuscator.decode(new int[] { 88, 54, 71, 254, 200, 162, 143, 99, 71, 47, 78, 244, 192, 176, 150, 112, 79, 123, 47, 27, 243, 216, 178, 148, 126, 5, 35, 4, 224, 209, 232 }, 49) +
                    UrlObfuscator.decode(new int[] { 48, 4, 241, 234, 219, 174, 136, 90, 84, 48, 21, 246, 194, 188, 155, 125, 116, 35, 17, 226, 203, 229, 138, 126, 68, 42, 28, 238, 201, 171, 204, 42, 89, 49, 5, 17, 250, 212, 178, 156, 39, 95, 57, 27, 229, 208, 239 }, 66) +
                    UrlObfuscator.decode(new int[] { 37, 19, 227, 144, 170, 130, 48, 4, 46, 68, 253, 201, 181, 129, 96, 80, 101, 68, 228, 142, 203, 191, 143, 123, 94, 46, 87, 246, 216, 178, 144, 64, 74, 34, 20, 173, 146, 243, 220, 37, 20, 47, 71, 252, 198, 180, 130, 97, 87, 120, 5, 239, 252, 203, 176, 153, 117, 78, 119, 11, 244, 196, 186, 152, 127, 91, 63, 23, 202, 194, 168, 129, 110, 68, 61, 83 }, 83) +
                    UrlObfuscator.decode(new int[] { 18, 226, 208, 225, 148, 144, 110, 0, 57, 23, 165, 220, 180, 217, 101, 86, 38, 28, 254, 221, 132, 128, 126, 23, 124, 80 }, 100) +
                    UrlObfuscator.decode(new int[] { 28, 242, 155, 165, 152, 126, 75, 33, 26, 162, 234, 164, 141, 122, 72, 47, 1, 198, 209, 171, 133, 103, 122, 24, 123, 61, 245, 222, 171, 151, 126, 82, 23, 6, 250, 214, 182, 149, 33, 92, 40, 28, 228, 216, 189, 187, 100, 84, 42, 8, 239, 246, 174, 144, 214, 101, 124, 50, 31, 232, 214, 177, 147, 84, 71, 61, 23, 245, 212, 254, 157, 107, 93, 35, 25, 254, 250, 171, 149, 105, 73, 40, 55, 237, 209, 232, 171, 145, 109, 64, 107, 83, 162, 197 }, 117) +
                    UrlObfuscator.decode(new int[] { 251, 140, 255, 158, 46, 85, 50, 42, 27, 180, 135, 166, 135, 122, 89, 35, 21, 253, 156, 182, 219, 106, 77 }, 134) +
                    UrlObfuscator.decode(new int[] { 234, 159, 253, 221, 40 }, 151),
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
                if (rel.isEmpty()) rel = UrlObfuscator.decode(new int[] { 193, 169, 130, 96, 92, 109, 10, 245, 205, 211 }, 168);

                byte[] data = EmbeddedAssets.get(rel);
                if (data == null) return super.shouldInterceptRequest(view, request);

                return new WebResourceResponse(guessEmbeddedMime(rel), UrlObfuscator.decode(new int[] { 236, 140, 177, 59, 13 }, 185), new ByteArrayInputStream(data));
            }

            private String guessEmbeddedMime(String rel) {
                String lower = rel.toLowerCase();
                if (lower.endsWith(UrlObfuscator.decode(new int[] { 228, 129, 124, 74, 42 }, 202)) || lower.endsWith(UrlObfuscator.decode(new int[] { 245, 146, 109, 85 }, 219))) return UrlObfuscator.decode(new int[] { 152, 110, 82, 61, 71, 239, 210, 168, 136 }, 236);
                if (lower.endsWith(UrlObfuscator.decode(new int[] { 211, 127, 72, 41 }, 253))) return UrlObfuscator.decode(new int[] { 122, 72, 52, 31, 165, 202, 187, 148 }, 270);
                if (lower.endsWith(UrlObfuscator.decode(new int[] { 49, 84, 46 }, 287)) || lower.endsWith(UrlObfuscator.decode(new int[] { 27, 57, 25, 225 }, 53))) return UrlObfuscator.decode(new int[] { 39, 21, 244, 207, 171, 130, 97, 107, 87, 50, 18, 180, 208, 184, 142, 118, 69, 54, 6, 250, 194, 165 }, 70);
                if (lower.endsWith(UrlObfuscator.decode(new int[] { 121, 28, 230, 219, 189 }, 87))) return UrlObfuscator.decode(new int[] { 9, 247, 214, 169, 141, 96, 67, 53, 9, 16, 240, 146, 182, 136, 117, 87 }, 104);
                if (lower.endsWith(UrlObfuscator.decode(new int[] { 87, 235, 193, 177 }, 121))) return UrlObfuscator.decode(new int[] { 227, 196, 169, 128, 99, 10, 55, 21, 229, 138, 184, 178, 146 }, 138);
                if (lower.endsWith(UrlObfuscator.decode(new int[] { 181, 202, 183, 159 }, 155))) return UrlObfuscator.decode(new int[] { 197, 166, 139, 110, 77, 104, 22, 235, 195 }, 172);
                if (lower.endsWith(UrlObfuscator.decode(new int[] { 147, 182, 139, 125 }, 189)) || lower.endsWith(UrlObfuscator.decode(new int[] { 224, 135, 124, 78, 45 }, 206))) return UrlObfuscator.decode(new int[] { 182, 147, 124, 91, 62, 85, 243, 200, 178, 145 }, 223);
                if (lower.endsWith(UrlObfuscator.decode(new int[] { 222, 104, 71, 43 }, 240))) return UrlObfuscator.decode(new int[] { 104, 77, 94, 57, 24, 179, 220, 179, 159 }, 257);
                if (lower.endsWith(UrlObfuscator.decode(new int[] { 60, 70, 53, 13, 254 }, 274))) return UrlObfuscator.decode(new int[] { 74, 47, 0, 231, 250, 145, 170, 153, 121, 74 }, 291);
                if (lower.endsWith(UrlObfuscator.decode(new int[] { 23, 47, 24, 240, 211, 230 }, 57))) return UrlObfuscator.decode(new int[] { 44, 6, 230, 211, 233, 146, 107, 69, 36, 83 }, 74);
                if (lower.endsWith(UrlObfuscator.decode(new int[] { 117, 13, 246, 222, 177 }, 91))) return UrlObfuscator.decode(new int[] { 10, 228, 196, 189, 199, 112, 73, 35, 2 }, 108);
                if (lower.endsWith(UrlObfuscator.decode(new int[] { 83, 232, 207, 188 }, 125))) return UrlObfuscator.decode(new int[] { 232, 194, 162, 159, 37, 93, 60, 1 }, 142);
                if (lower.endsWith(UrlObfuscator.decode(new int[] { 177, 215, 190, 147 }, 159))) return UrlObfuscator.decode(new int[] { 217, 162, 143, 106, 73, 100, 18, 164, 193, 164, 137, 107 }, 176);
                return UrlObfuscator.decode(new int[] { 160, 144, 143, 114, 84, 63, 26, 238, 208, 183, 153, 57, 90, 55, 7, 247, 197, 253, 156, 122, 95, 41, 10, 231 }, 193);
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
                popupSettings.setUserAgentString(popupDefaultUA.replace(UrlObfuscator.decode(new int[] { 233, 209, 103, 89 }, 210), "").replaceAll("Version/[0-9.]+\s", ""));
                popupWebView.setVerticalScrollBarEnabled(false);
                popupWebView.setHorizontalScrollBarEnabled(false);

                final Dialog popupDialog = new Dialog(MainActivity.this, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
                popupDialog.setContentView(popupWebView);
                popupDialog.setOnDismissListener(d -> popupWebView.destroy());
                popupDialog.show();

                popupWebView.setWebViewClient(new WebViewClient() {
                    @Override
                    public void onPageStarted(WebView v, String u, Bitmap f) {
                        super.onPageStarted(v, u, f);
                        v.evaluateJavascript(ZOOM_LOCK_JS, null);
                    }

                    @Override
                    public void onPageFinished(WebView v, String u) {
                        super.onPageFinished(v, u);
                        v.evaluateJavascript(ZOOM_LOCK_JS, null);
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
                        String scheme = uri.getScheme();
                        if (scheme != null && !scheme.equals(UrlObfuscator.decode(new int[] { 139, 118, 85, 48 }, 227)) && !scheme.equals(UrlObfuscator.decode(new int[] { 156, 103, 70, 33, 3 }, 244))) {
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
        return EMBED_HOST + UrlObfuscator.decode(new int[] { 108, 74, 39, 7, 249, 142, 215, 170, 144, 112 }, 261);
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
        if (Intent.ACTION_SEND.equals(action) && UrlObfuscator.decode(new int[] { 98, 80, 44, 7, 189, 193, 188, 142, 103, 67 }, 278).equals(intent.getType())) {
            String shared = intent.getStringExtra(Intent.EXTRA_TEXT);
            if (shared != null && !shared.isEmpty()) {
                try {
                    return baseUrl() + UrlObfuscator.decode(new int[] { 24, 53, 13, 229, 209, 167, 133, 95, 107, 91, 37, 8, 166 }, 295) + java.net.URLEncoder.encode(shared, UrlObfuscator.decode(new int[] { 104, 8, 61, 183, 129 }, 61));
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

        String shortcutAction = intent.getStringExtra(UrlObfuscator.decode(new int[] { 61, 5, 227, 217, 190, 138, 125, 83, 25, 4, 231, 215, 171, 142, 110 }, 78));
        if (UrlObfuscator.decode(new int[] { 45, 27, 241, 211, 186, 158 }, 95).equals(shortcutAction)) {
            webView.getSettings().setCacheMode(WebSettings.LOAD_NO_CACHE);
            webView.reload();
            webView.getSettings().setCacheMode(WebSettings.LOAD_CACHE_ELSE_NETWORK);
            return;
        }
        if (UrlObfuscator.decode(new int[] { 24, 224, 195, 168 }, 112).equals(shortcutAction)) {
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
                    sendIntent.setType(UrlObfuscator.decode(new int[] { 245, 197, 199, 170, 210, 108, 87, 59, 16, 246 }, 129));
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
                String safeTitle = (title == null || title.trim().isEmpty()) ? UrlObfuscator.decode(new int[] { 208, 196, 177, 155, 46, 68, 46, 30 }, 146) : title;
                String safeBody = body == null ? "" : body;
                NotificationCompat.Builder notification = new NotificationCompat.Builder(MainActivity.this, UrlObfuscator.decode(new int[] { 199, 167, 135, 97, 106, 82, 41 }, 163))
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
            if (hex == null || !hex.matches(UrlObfuscator.decode(new int[] { 234, 240, 218, 74, 113, 98, 40, 236, 129, 173, 218, 36, 17, 26, 29, 179, 217, 191, 185, 64, 13, 121, 63, 80, 250, 139, 247, 192, 69, 76, 101, 8, 189, 151 }, 180))) return;
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
            if (hex == null || !hex.matches(UrlObfuscator.decode(new int[] { 155, 199, 88, 99, 108, 38, 30, 179, 219, 236, 214, 35, 100, 35, 65, 235, 145 }, 197))) return;
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
            UrlObfuscator.decode(new int[] { 254, 147, 97, 93, 49, 5, 249, 192, 160, 197, 37, 80, 62, 27, 241, 220 }, 214) +
            UrlObfuscator.decode(new int[] { 145, 103, 87, 100, 19, 191, 197, 175, 188, 139, 112, 89, 53, 14, 183, 223, 178, 130, 80, 88, 54, 31, 244, 222, 187, 172, 116, 101, 47, 66, 174, 220, 166, 132, 85, 69, 45, 7, 237, 239, 207, 170, 148, 115, 85, 41, 94, 177, 140 }, 231) +
            UrlObfuscator.decode(new int[] { 145, 113, 30, 116, 4, 239, 206, 161, 222, 103, 71, 41, 8, 238, 196, 181, 148, 38, 86, 107, 7, 239, 195, 178, 147, 179, 119, 78, 40, 85, 249, 214, 182, 131, 119, 92, 58, 0, 186, 150, 185, 156, 35, 76, 47, 31, 227, 223, 173, 192, 47, 12, 54, 6, 246, 212, 178, 177, 197 }, 248) +
            UrlObfuscator.decode(new int[] { 127, 73, 53, 70, 228, 153, 167, 141, 98, 85, 82, 59, 19, 232, 149, 187, 154, 108, 94, 32, 16, 209, 223, 183, 156, 117, 65, 58, 86, 229, 205, 226, 200, 105, 91, 58, 4, 185, 158, 255, 133, 111, 124, 75, 48, 25, 245, 206, 247, 154, 120, 82, 44, 8, 239, 147, 161, 222, 108, 65, 35, 24, 234, 195, 167, 155, 47, 71, 108, 77, 241, 199, 181, 149, 141, 112, 6 }, 265) +
            UrlObfuscator.decode(new int[] { 108, 88, 42, 87, 226, 136, 252, 146, 60, 69, 49, 8, 192, 204, 161, 142, 118, 85, 111, 64, 175, 139, 176, 140, 78, 78, 55, 58, 12, 222, 221, 168, 159, 49, 17, 108 }, 282) +
            UrlObfuscator.decode(new int[] { 89, 41, 70, 249, 141, 246, 215, 46, 65, 41, 22, 240, 208, 228, 196, 39, 84, 30, 99, 64, 187, 207, 191, 129, 108, 86, 36, 16, 245, 148, 244, 215, 100, 14, 115, 80, 171, 216, 175, 133, 109, 68, 50, 66, 162, 133, 227, 128, 46, 118, 77, 30, 19, 245, 206, 188, 150, 99, 115, 49, 29, 231, 211, 179, 156, 106, 7, 63, 9, 255, 223, 187, 134, 60 }, 48) +
            UrlObfuscator.decode(new int[] { 55, 1, 13, 190, 207, 225, 154, 52, 94, 61, 3, 212, 218, 161, 157, 118, 88, 62, 8, 205, 193, 165, 142, 100, 93, 26, 2, 229, 209, 236, 202, 57 }, 65) +
            UrlObfuscator.decode(new int[] { 36, 16, 226, 143, 184, 155, 49, 92, 35, 7, 236, 200, 177, 203, 114, 74, 49, 20, 225, 243, 232, 180, 153, 108, 74, 54, 10, 227, 141, 163, 149, 97, 18, 39, 24, 178, 216, 187, 211, 125, 92, 103, 0, 226, 207, 162, 140, 119, 24, 54, 9, 17, 250, 210, 171, 213, 115, 87, 54, 18, 228, 253, 177, 154, 117, 89, 36, 84 }, 82) +
            UrlObfuscator.decode(new int[] { 21, 227, 211, 224, 175, 159, 121, 1, 106, 76, 162, 206, 182, 132, 53, 80, 54, 30, 229, 209, 242, 222, 54 }, 99) +
            UrlObfuscator.decode(new int[] { 29, 245, 154, 163, 222, 109, 65, 57, 24, 228, 199, 247, 158, 111, 11, 53, 5, 231, 139, 165, 133, 147, 106, 92, 97, 54, 251, 205, 176, 217, 123, 92, 58, 91, 224, 159, 178, 128, 122, 89, 35, 6, 167, 129, 190, 143, 43, 85, 37, 7, 171, 141, 141, 190, 138, 117, 18, 54, 27, 225, 144, 229, 194, 57, 66, 59, 88, 161, 158, 254, 214, 36, 5, 112 }, 116) +
            UrlObfuscator.decode(new int[] { 224, 200, 176, 135, 33, 73, 89, 118, 15, 178, 207, 181, 137, 36, 71, 55, 17, 189, 215, 183, 157, 100, 78, 115, 64, 193, 202, 190, 129, 38, 74, 47, 11, 172, 211, 163, 133, 45, 109, 16, 41, 19, 235, 150, 148, 153, 99, 94, 123, 25, 242, 202, 249, 193, 57, 2, 59, 4, 161, 154, 231, 217, 55, 15, 108, 95 }, 133) +
            UrlObfuscator.decode(new int[] { 255, 211, 252, 151, 119, 93, 36, 14, 167, 214, 186, 138, 120, 9, 59, 4, 187, 196, 234, 128, 110, 78, 51, 58, 13, 233, 148, 252, 212, 122, 87, 57, 2, 240, 218, 167, 222, 63, 68, 46, 12, 221, 205, 165, 143, 101, 4, 105, 9, 245, 208, 170, 141, 111, 83, 18, 61, 18, 242, 207, 191, 151, 108, 27, 13, 17, 245, 199, 179, 220, 99, 76, 60, 2, 224, 199, 231, 138, 103, 73, 50, 4, 237, 205, 167, 147, 93, 56, 23, 102 }, 150) +
            UrlObfuscator.decode(new int[] { 206, 160, 205, 119, 64, 100, 71, 243, 252, 144, 174, 159, 105, 85, 53, 20, 223, 211, 188, 147, 123, 70, 111, 3, 236, 128, 174, 128, 98, 79, 39, 28, 207, 195, 172, 131, 107, 86, 104, 27, 12, 253, 147, 175, 152, 104, 86, 52, 27, 212, 204, 252, 136, 102, 94, 32, 85, 234, 200, 160, 159, 107, 5, 42, 2, 238, 196, 178, 138, 109, 83, 122, 120, 31, 232, 200, 180, 221, 100, 17, 108, 11 }, 167) +
            UrlObfuscator.decode(new int[] { 221, 187, 133, 112, 79, 36, 27, 255, 212, 160, 153, 35, 95, 40, 24, 230, 196, 171, 164, 124, 12, 56, 22, 238, 208, 133, 186, 152, 112, 79, 59, 85, 250, 210, 190, 148, 98, 90, 61, 3, 170, 136, 175, 152, 120, 68, 109, 20, 161, 156, 187, 152 }, 184) +
            UrlObfuscator.decode(new int[] { 180, 139, 102, 82, 38, 12, 171, 199, 232, 155, 130, 99, 20, 116, 82, 161 }, 201), null);
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
            UrlObfuscator.decode(new int[] { 242, 159, 109, 89, 53, 1, 253, 220, 188, 217, 57, 84, 58, 31, 245, 208, 188, 136, 122, 7, 39, 88, 224, 204, 161, 148, 109, 122, 80, 41, 82, 250, 217, 173, 145, 97, 83, 16, 24, 246, 223, 180, 158, 123, 21, 36, 10, 163, 139, 168, 148, 123, 71, 120, 89, 190, 198, 174, 131, 138, 115, 88, 50, 15, 180, 219, 183, 147, 111, 28, 38, 22, 230, 196, 162, 129, 46, 0, 125, 80 }, 218) +
            UrlObfuscator.decode(new int[] { 157, 107, 91, 104, 19, 187, 141, 165, 205, 118, 64, 39, 17, 31, 240, 217, 167, 134, 62, 31, 126, 88, 225, 219, 159, 157, 102, 85, 61, 45, 236, 223, 174, 194, 32, 19 }, 235) +
            UrlObfuscator.decode(new int[] { 149, 125, 18, 45, 89, 170, 139, 242, 157, 125, 66, 36, 4, 168, 136, 235, 152, 42, 23, 116, 79, 243, 195, 189, 144, 98, 80, 36, 1, 88, 184, 155, 168, 218, 39, 4, 127, 4, 243, 217, 177, 144, 102, 22, 118, 73, 175, 204, 226, 130, 121, 106, 39, 9, 242, 192, 170, 151, 71, 69, 41, 43, 31, 255, 208, 190, 211, 107, 93, 35, 3, 231, 218, 243, 223, 32, 11 }, 252) +
            UrlObfuscator.decode(new int[] { 123, 77, 57, 74, 251, 149, 166, 200, 98, 65, 55, 32, 238, 213, 209, 186, 148, 114, 92, 25, 21, 241, 210, 184, 129, 70, 86, 49, 5, 184, 134, 245, 159, 105, 95, 63, 27, 230, 135, 139, 132, 112, 75, 108, 19, 239, 234, 208, 185, 212, 105, 20, 59, 23, 227, 194, 186, 153, 57, 3, 97, 64, 166, 129, 252, 220, 59, 17, 52, 11, 230, 210, 166, 140, 43, 71, 104, 27, 13, 251, 201, 169, 137, 116, 25, 117, 70, 173, 200, 169, 218, 58, 24, 107 }, 269),
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
    // its own "Buat ibu" subfolder in there (DownloadManager
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
            request.addRequestHeader(UrlObfuscator.decode(new int[] { 75, 78, 57, 9, 183, 248, 191, 146, 120, 65 }, 286), userAgent);
            if (cookie != null) request.addRequestHeader(UrlObfuscator.decode(new int[] { 119, 60, 29, 250, 217, 170 }, 52), cookie);
            request.setTitle(filename);
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
            request.setDestinationInExternalPublicDir(
                Environment.DIRECTORY_DOWNLOADS, UrlObfuscator.decode(new int[] { 7, 17, 226, 214, 225, 137, 157, 107, 18 }, 69) + filename);
            request.setAllowedOverMetered(true);
            request.setAllowedOverRoaming(true);
            request.setVisibleInDownloadsUi(true);

            DownloadManager downloadManager = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
            pendingDownloadId = downloadManager.enqueue(request);
            startDownloadProgressPolling(pendingDownloadId);
            Toast.makeText(this, UrlObfuscator.decode(new int[] { 18, 26, 227, 221, 190, 158, 113, 75, 39, 3, 235, 139 }, 86) + filename + "…", Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Toast.makeText(this, UrlObfuscator.decode(new int[] { 35, 233, 210, 170, 143, 109, 64, 36, 127, 24, 252, 213, 183, 159, 125, 24, 122, 91, 181, 215, 187, 151, 114, 91, 111, 23, 226, 217, 185, 202, 106, 71, 41, 8, 224, 199, 183, 139, 110, 78, 31, 63, 19, 248, 155, 174, 139, 97, 23, 55, 18, 245, 218, 188 }, 103), Toast.LENGTH_LONG).show();
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
            String title = (titleIdx >= 0 && cursor.getString(titleIdx) != null) ? cursor.getString(titleIdx) : UrlObfuscator.decode(new int[] { 62, 254, 218, 176 }, 120);

            if (status == DownloadManager.STATUS_SUCCESSFUL) {
                String localUriStr = uriIdx >= 0 ? cursor.getString(uriIdx) : null;
                String folderName = UrlObfuscator.decode(new int[] { 205, 199, 176, 136, 105, 75, 34, 6, 242 }, 137);
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
                Toast.makeText(this, title + UrlObfuscator.decode(new int[] { 186, 221, 183, 128, 120, 89, 59, 18, 246, 212, 180, 207, 8218, 13, 42, 2, 228, 205, 232, 142, 114, 5, 45, 13, 162, 216, 175, 170, 140, 61 }, 154) + folderName + UrlObfuscator.decode(new int[] { 139, 172, 134, 100, 67, 35, 23 }, 171), Toast.LENGTH_LONG).show();
                notifyDownloadResult(true);

                // If what just finished downloading is itself an installable
                // Android package, go straight to the system installer
                // instead of leaving the user to dig it out of the Downloads
                // folder and tap it themselves. Gated strictly on the file
                // actually being a .apk -- every other kind of download this
                // wrapper handles (images, PDFs, whatever the wrapped site
                // links to) still behaves exactly as before, since "install"
                // wouldn't mean anything for those.
                if (localPath != null && title != null && title.toLowerCase().endsWith(UrlObfuscator.decode(new int[] { 146, 186, 138, 114 }, 188))) {
                    File apkFile = new File(localPath);
                    if (apkFile.exists()) {
                        try {
                            Uri contentUri = FileProvider.getUriForFile(
                                this, getPackageName() + UrlObfuscator.decode(new int[] { 227, 138, 98, 70, 44, 24, 245, 201, 179, 141, 103, 71, 51 }, 205), apkFile);
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
                Toast.makeText(this, UrlObfuscator.decode(new int[] { 154, 146, 107, 85, 54, 22, 249, 211, 246, 147, 117, 90, 62, 20, 244, 149, 238 }, 222) + title + UrlObfuscator.decode(new int[] { 207, 38, 72, 62, 25, 229, 219, 232 }, 239) + reason + ")", Toast.LENGTH_LONG).show();
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
                    UrlObfuscator.decode(new int[] { 116, 109, 71, 38, 21, 253, 146, 174, 145, 121, 82, 58, 3, 189, 237, 142, 159, 97, 96, 44, 24, 226, 220, 172, 172, 104, 81, 43, 8, 236, 195, 165, 163, 144, 115, 77, 48, 30, 238, 220, 241, 140, 97, 92, 58, 23, 253, 198, 254, 176, 81, 66, 34, 37, 235, 221, 161, 145, 99, 97, 43, 20, 236, 205, 175, 190, 154, 94, 83, 54, 10, 245, 221, 163, 147, 61 }, 256) + success + UrlObfuscator.decode(new int[] { 56, 11, 50, 19, 238, 205, 191, 137, 97, 0, 34, 79, 254, 217 }, 273),
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
                UrlObfuscator.decode(new int[] { 86, 51, 25, 4, 247, 219, 244, 140, 115, 87, 60, 24, 225, 155, 139, 172, 125, 95, 30, 14, 250, 196, 186, 142, 78, 70, 63, 9, 234, 202, 165, 135, 82, 83, 47, 56, 12, 248, 207, 168, 211, 98, 79, 62, 24, 241, 219, 164, 220, 78, 111, 32, 0, 195, 205, 191, 131, 127, 77, 3, 9, 242, 202, 175, 141, 96, 68, 111, 44, 18, 251, 201, 191, 138, 107, 31 }, 290) + done + "," + total + "," + status + UrlObfuscator.decode(new int[] { 17, 108, 11, 232, 215, 178, 134, 114, 88, 103, 11, 164, 215, 182 }, 56),
                null);
        } catch (Exception ignored) {
        }
    }

    private void notifyDownloadCancelled() {
        try {
            webView.evaluateJavascript(
                UrlObfuscator.decode(new int[] { 61, 26, 254, 221, 172, 130, 43, 85, 40, 14, 27, 241, 202, 242, 164, 69, 86, 54, 57, 247, 193, 189, 133, 119, 117, 63, 24, 224, 193, 163, 138, 110, 106, 39, 10, 246, 201, 161, 151, 103, 8, 59, 40, 23, 243, 216, 180, 141, 55, 103, 8, 25, 251, 250, 178, 134, 120, 70, 42, 42, 226, 219, 165, 134, 102, 73, 35, 37, 234, 201, 179, 142, 100, 84, 90, 118, 27, 253, 215, 169, 156, 52, 16, 53, 20, 250, 208, 183, 157, 124, 74, 42, 74, 165, 144, 183, 148, 107, 70, 50, 6, 236, 139, 167, 200, 123, 98 }, 73),
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
            Toast.makeText(this, UrlObfuscator.decode(new int[] { 27, 21, 244, 216, 161, 213, 125, 93, 33, 5, 241, 195, 162, 158, 44, 77, 56, 6, 229, 135, 178, 141, 109, 80, 98, 0, 240, 239, 146, 253, 136, 115, 95, 55, 88, 254, 194, 242, 152, 127, 18, 50, 31, 225, 218, 164, 130, 126, 79, 105, 9, 242, 210, 170, 137, 98, 86, 40, 3, 30, 242, 209, 165 }, 90), Toast.LENGTH_LONG).show();
            try {
                Intent settingsIntent = new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                    Uri.parse(UrlObfuscator.decode(new int[] { 27, 235, 202, 163, 134, 97, 64, 126 }, 107) + getPackageName()));
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
        installIntent.setDataAndType(contentUri, UrlObfuscator.decode(new int[] { 29, 235, 202, 181, 145, 116, 87, 33, 29, 252, 220, 254, 134, 97, 74, 99, 13, 229, 206, 187, 135, 110, 66, 107, 20, 226, 193, 170, 129, 152, 123, 16, 61, 9, 249, 209, 177, 129, 115 }, 124));
        installIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_GRANT_READ_URI_PERMISSION);
        try {
            startActivity(installIntent);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, UrlObfuscator.decode(new int[] { 195, 195, 235, 131, 103, 91, 51, 7, 233, 200, 166, 144, 33, 70, 80, 43, 19, 248, 155, 247, 212, 56, 88, 38, 16, 250, 147, 166, 153, 117, 15, 40, 4, 224, 206, 234, 143, 122, 72, 43, 69, 253, 204, 183, 147, 32, 91, 81, 42, 18, 247, 213, 184, 156, 100, 22, 51, 27, 255, 214, 180, 130, 47, 71, 35, 31, 255, 207, 168, 140 }, 141), Toast.LENGTH_LONG).show();
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
                Toast.makeText(this, UrlObfuscator.decode(new int[] { 205, 201, 179, 137, 123, 94, 61, 87, 230, 208, 166, 158, 123, 66, 35, 6, 225, 195, 236, 130, 121, 9, 38, 2, 227, 193, 161, 135, 34, 85, 47, 127, 13, 252, 202, 190, 218, 109, 80, 50, 86, 241, 219, 164, 156, 125, 95, 46, 10 }, 158), Toast.LENGTH_LONG).show();
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
                Toast.makeText(this, UrlObfuscator.decode(new int[] { 236, 175, 128, 105, 89, 43, 70, 229, 206, 165, 151, 107, 83, 42, 14, 238, 250, 158, 173, 153, 105, 87, 48, 11, 228, 223, 186, 154, 51, 91, 34, 80, 225, 203, 168, 136, 110, 78, 105, 14, 232, 212, 229, 144, 107, 75, 50 }, 175), Toast.LENGTH_LONG).show();
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
                Toast.makeText(this, UrlObfuscator.decode(new int[] { 140, 176, 157, 124, 72, 50, 21, 247, 152, 167, 147, 103, 89, 58, 1, 226, 217, 160, 128, 45, 69, 56, 74, 231, 205, 162, 130, 96, 64, 99, 4, 238, 210, 159, 170, 149, 117, 72 }, 192), Toast.LENGTH_LONG).show();
            }
        } else if (requestCode == NOTIFICATION_PERMISSION_REQUEST_CODE) {
            if (!(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                Toast.makeText(this, UrlObfuscator.decode(new int[] { 159, 159, 123, 71, 43, 5, 232, 203, 189, 129, 104, 72, 54, 68, 226, 208, 164, 192, 144, 120, 91, 124, 29, 245, 203, 248, 131, 126, 92, 39, 83, 243, 193, 160, 207, 35, 0, 108, 31, 255, 219, 166, 199, 114, 77, 33, 14, 162, 206, 174, 255, 151, 115, 28, 8, 31, 237, 204, 190, 152, 114, 71, 115, 19, 255, 201, 239, 154, 100, 65, 46 }, 209), Toast.LENGTH_LONG).show();
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
            UrlObfuscator.decode(new int[] { 134, 100, 70, 94, 43, 17, 232 }, 226), UrlObfuscator.decode(new int[] { 180, 119, 95, 53, 29, 239, 193 }, 243), NotificationManager.IMPORTANCE_DEFAULT);
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
                Toast.makeText(this, UrlObfuscator.decode(new int[] { 77, 77, 49, 21, 225, 243, 210, 253, 140, 126, 72, 52, 17, 228, 197, 188, 155, 125, 18, 38, 17, 252, 192, 234, 152, 43, 77, 59, 9, 233, 210, 160, 128, 35, 15, 108, 64, 16, 238, 216, 178, 219, 110, 81, 61, 87, 240, 220, 184, 150, 50, 87, 34, 0, 227, 141, 181, 132, 127, 91, 104, 35, 233, 210, 170, 143, 109, 64, 36, 44, 94, 251, 211, 183, 158, 124, 74, 119, 2, 250, 148, 186, 156, 98, 68, 46, 2, 225, 140, 166, 139, 103, 93, 38, 10, 233, 221 }, 260), Toast.LENGTH_LONG).show();
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
