package com.chaty;

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
    // Removes the grey tap flash, the long-press callout and accidental text
    // selection so the page behaves like a native screen. Inputs, textareas
    // and contenteditable stay fully selectable/typeable; a site can opt any
    // element back in with data-allow-select. Zero-specificity (:where), so
    // a site's own user-select rules still win. Idempotent.
    private static final String NATIVE_FEEL_JS =
        UrlObfuscator.decode(new int[] { 228, 141, 127, 71, 43, 19, 239, 202, 170, 203, 43, 90 }, 204) +
        UrlObfuscator.decode(new int[] { 180, 154, 51, 77, 48, 22, 243, 217, 162, 218, 76, 109, 63, 17, 251, 199, 187, 137, 77, 79, 44, 4, 197, 201, 176, 138, 103, 11, 51, 5, 11, 235, 207, 178, 192, 109, 80, 54, 19, 249, 194, 250, 172, 77, 95, 49, 27, 231, 219, 169, 173, 111, 76, 36, 37, 233, 208, 170, 135, 63, 85, 50, 42, 27, 166 }, 221) +
        UrlObfuscator.decode(new int[] { 136, 120, 66, 40, 30, 224, 199, 169, 198, 98, 75, 107, 75, 250, 212, 205, 167, 134, 117, 93, 114, 29, 247, 212, 163, 152, 113, 93, 38, 95, 247, 202, 186, 168, 96, 78, 39, 12, 230, 211, 132, 156, 77, 71, 106, 70, 223, 192, 208, 188, 136, 114, 76, 60, 62, 242, 211, 185, 183, 96, 65, 118, 89, 166, 220, 168, 152, 126, 88, 39, 83 }, 238) +
        UrlObfuscator.decode(new int[] { 137, 127, 79, 124, 19, 167, 221, 183, 148, 99, 88, 49, 29, 230, 159, 184, 138, 111, 73, 48, 23, 238, 198, 171, 146, 107, 64, 42, 23, 172, 197, 175, 188, 139, 112, 89, 53, 14, 220, 212, 178, 155, 112, 90, 39, 73, 248, 214, 231, 207, 101, 5, 57, 15, 253, 221, 181, 136, 62 }, 255) +
        UrlObfuscator.decode(new int[] { 102, 78, 60, 77, 255, 223, 247, 141, 103, 68, 51, 8, 225, 205, 182, 207, 99, 109, 91, 60, 8, 254, 255, 181, 157, 122, 83, 59, 0, 187, 149, 162, 132, 118, 66, 40, 75, 162, 145, 186, 156, 41, 79, 33, 89, 164, 253, 158, 142, 158, 106, 84, 42, 30, 220, 220, 189, 155, 85, 70, 39, 84, 169 }, 272) +
        UrlObfuscator.decode(new int[] { 82, 52, 113, 10, 248, 196, 175, 185, 118, 86, 35, 19, 251, 192, 238, 213, 59, 75, 98, 25, 232, 206, 160, 131, 125, 5, 51, 7, 245, 137, 171, 139, 102, 72, 83, 55, 26, 244, 207, 247, 154, 119, 91, 57, 7, 174, 199, 160, 144, 126, 92, 62, 12, 254, 206, 164, 157, 117, 79, 50, 8, 232, 143, 160, 142, 100, 102, 69, 112, 11, 254, 216, 178, 145, 99, 27, 33, 27, 230, 209, 185, 221, 108, 79, 33, 0, 228, 223, 189, 210, 105, 73, 43, 1, 254, 152, 182, 136, 154, 108, 88, 116, 25, 245, 221, 161, 215, 60, 15, 58, 28, 230, 153, 185, 129, 126, 88, 56, 71, 254, 204, 176, 147, 103, 87, 33, 2, 174, 210, 165, 179, 155, 126, 72, 119, 33, 250, 215, 185, 130, 112, 90, 39, 23, 245, 217, 187, 143, 111, 64, 46, 55, 165, 243, 164, 137, 107, 80, 38, 12, 245, 197, 219, 183, 137, 125, 89, 54, 28, 197, 151, 252, 220, 61, 72, 127, 6, 245, 205, 165, 132, 120, 6, 63, 26, 237, 213, 235, 150, 97, 79, 39, 2, 244, 165, 208, 178, 146, 126, 1, 44, 11, 242, 196, 248, 135, 118, 94, 52, 19, 251, 148, 163, 131, 101, 79, 52, 51, 227, 199, 177, 133, 46, 67, 45, 12, 16, 233, 144, 175, 158, 118, 92, 59, 3, 203, 153, 143, 151, 115, 69, 49, 66, 239, 193, 160, 132, 125, 4, 59, 2, 234, 192, 167, 151, 95, 1, 106, 36, 83, 234, 217, 185, 145, 112, 76, 122, 3, 230, 209, 161, 223, 98, 85, 35, 11, 238, 216, 241, 158, 108, 80, 51, 93, 240, 215, 166, 144, 44, 83, 90, 50, 24, 255, 207, 224, 141, 125, 79, 34, 8, 179, 136 }, 289) +
        UrlObfuscator.decode(new int[] { 95, 120, 20, 228, 195, 183, 159, 116, 108, 38, 4, 224, 207, 226, 154, 124, 14, 125, 24, 231, 194, 182, 130, 104, 55, 91, 116, 7, 230, 199 }, 55) +
        UrlObfuscator.decode(new int[] { 47, 8, 174, 140, 255 }, 72) +
        UrlObfuscator.decode(new int[] { 45, 10, 238, 205, 188, 146, 59, 19, 53, 31, 236, 219, 160, 137, 101, 94, 103, 15, 226, 210, 128, 136, 102, 79, 36, 14, 11, 220, 196, 149, 159, 50, 30, 7, 40, 248, 212, 160, 154, 100, 84, 22, 10, 235, 193, 143, 152, 121, 14, 97, 78, 253, 193, 171, 128, 119, 76, 37, 49, 10, 179, 221, 191, 158, 92, 78, 50, 24, 225, 248, 186, 129, 101, 85, 33, 11, 255, 132, 236, 174, 70, 101, 4, 9, 235, 208, 166, 140, 117, 108, 80, 63, 25, 249, 223, 253, 213, 127, 88, 127, 78, 233, 206, 177, 144, 100, 76, 38, 69, 233, 130, 177, 148 }, 89) +
        UrlObfuscator.decode(new int[] { 23, 160, 128, 238, 221 }, 106);
    // Reads the live page (background behind the screen centre, theme-color,
    // the first real button's color and corner radius) so the exit dialog can
    // be dressed to match whatever site the app wraps. Read-only; returns '{}'
    // on any error and the dialog falls back to the app's bar color.
    private static final String EXIT_THEME_JS =
        UrlObfuscator.decode(new int[] { 83, 252, 204, 182, 148, 98, 92, 59, 29, 186, 152, 171, 155, 124, 84, 55 }, 123) +
        UrlObfuscator.decode(new int[] { 234, 222, 164, 138, 124, 78, 41, 11, 164, 204, 169, 201, 99, 54, 69, 47, 25, 239, 207, 171, 150, 55, 85, 115, 82, 240, 147, 236, 205, 40, 90, 63, 13, 229, 217, 185, 137, 117, 67, 43, 16, 164, 132, 231, 131, 209, 119, 83, 56, 30, 226, 246, 190, 223, 49, 71, 51, 17, 243, 153, 224, 195, 46, 29, 96, 75, 186, 133, 232, 215, 47, 2, 109, 66, 191, 156, 240, 228, 131 }, 140) +
        UrlObfuscator.decode(new int[] { 251, 201, 181, 153, 109, 81, 56, 24, 181, 199, 188, 158, 120, 84, 103, 13, 164, 215, 185, 143, 125, 93, 53, 8, 165, 199, 237, 139, 111, 68, 90, 38, 50, 250, 147, 253, 139, 127, 85, 55, 93, 179, 154, 239, 204, 45, 31, 113, 29, 237, 217, 185, 140, 78, 75, 41, 4, 240, 139, 161, 207, 115, 111, 82, 52, 8, 179, 157, 245, 223, 62, 109, 102, 41, 186, 140, 225, 222, 57, 20, 57, 30, 254, 207, 242, 149 }, 157) +
        UrlObfuscator.decode(new int[] { 216, 172, 158, 43, 72, 46, 85, 233, 211, 169, 136, 47, 71, 45, 93, 27, 241, 222, 169, 150, 127, 87, 44, 89, 243, 217, 177, 158, 119, 95, 36, 41, 252, 194, 161, 187, 101, 64, 38, 19, 174, 204, 170, 141, 103, 83, 23, 54, 26, 233, 212, 244, 200, 53, 81, 57, 24, 240, 198, 155, 151, 120, 87, 39, 26, 162, 158, 226, 209 }, 174) +
        UrlObfuscator.decode(new int[] { 200, 182, 148, 112, 94, 114, 28, 244, 145, 240, 212, 118, 84, 123, 10, 230, 206, 188, 205, 111, 22, 45, 12, 252, 228, 169, 136, 116, 86, 54, 4, 228, 204, 202, 164, 144, 126, 18, 60, 20, 190, 152, 183, 149, 112, 89, 54, 2, 224, 219, 163, 136, 72, 69, 37, 7, 245, 157, 172, 130, 43, 77, 42, 72, 28, 183, 155, 250, 136, 117, 85, 49, 19, 190, 214, 253, 218, 112, 86, 109, 12, 181, 200, 160, 214, 111, 69, 102, 23, 231, 215, 161, 141, 118, 100, 44, 58, 19, 248, 210, 175, 193, 100 }, 191) +
        UrlObfuscator.decode(new int[] { 185, 137, 38, 12, 46, 12, 163, 210, 190, 134, 116, 5, 38, 94, 229, 196, 180, 156, 145, 112, 76, 46, 14, 252, 220, 132, 130, 108, 88, 54, 90, 245, 223, 172, 155, 96, 73, 37, 30, 167, 204, 168, 133, 112, 73, 38, 12, 245, 229, 211, 187, 144, 121, 85, 46, 80, 182, 213, 183, 150, 127, 84, 32, 30, 229, 193, 170, 174, 99, 71, 37, 27, 179, 206, 160, 205, 107, 72, 106, 3, 169, 182, 220, 186, 193, 121, 1, 36 }, 208) +
        UrlObfuscator.decode(new int[] { 151, 97, 109, 30, 48, 65, 255, 213, 186, 141, 122, 83, 59, 0, 189, 195, 164, 149, 125, 87, 30, 9, 231, 207, 170, 156, 104, 84, 109, 67, 238, 199, 181, 129, 164, 112, 92, 49, 30, 167, 205, 176, 146, 123, 80, 121, 16, 253, 221, 191, 157, 83, 10, 101, 80, 252, 200, 186, 199, 114, 77, 121, 14, 189, 204, 238, 184, 155, 105, 125, 47, 14, 235, 209, 181, 131, 97, 81, 123, 85, 242, 223, 161, 154, 104, 66, 63, 77, 160, 146, 169, 147, 105, 72, 120 }, 225) +
        UrlObfuscator.decode(new int[] { 132, 112, 66, 111, 12, 249, 194, 246, 132, 124, 68, 43, 74, 247, 153, 238, 211, 45, 66, 76, 99, 25, 243, 216, 175, 148, 125, 89, 34, 91, 229, 198, 183, 131, 105, 124, 43, 1, 233, 200, 190, 134, 122, 102, 42, 9, 172, 132, 160, 148, 116, 107, 81, 51, 80, 250, 150, 130, 138, 120, 90, 48, 73, 241, 199, 165, 132, 96, 64, 16, 64, 226, 196, 185, 157, 115, 125, 49, 29, 243, 199, 252, 147, 138, 124, 80, 53, 15, 199, 158, 241, 204 }, 242) +
        UrlObfuscator.decode(new int[] { 101, 77, 51, 72, 9, 255, 207, 252, 146, 39, 9, 99, 30, 170, 215, 167, 221, 126, 84, 62, 8, 250, 197, 234, 205, 99, 21, 112, 87, 189, 204, 239, 200, 43, 90, 54, 62, 12, 189, 217, 230, 152, 106, 99, 62, 43, 185, 197, 238, 151, 63, 87, 42, 26, 207, 195, 190, 132, 109, 65, 41, 1, 198, 200, 170, 135, 111, 84, 109, 59, 30, 232, 147, 243, 194 }, 259) +
        UrlObfuscator.decode(new int[] { 125, 85, 122, 0, 190, 216, 167, 137, 120, 67, 118, 92, 190, 219, 186, 148, 42, 75, 39, 8, 231, 247, 202, 225, 206, 35, 70, 37, 9, 185, 212, 186, 128, 103, 93, 60, 76, 191, 210, 177, 157, 37, 94, 38, 24, 185, 207, 171, 138, 102, 80, 9, 5, 22, 249, 213, 168, 210, 121, 86, 54, 3, 255, 219, 161, 150, 41 }, 276) +
        UrlObfuscator.decode(new int[] { 83, 37, 17, 162, 210, 253, 184, 155, 105, 127, 52, 23, 233, 205, 163, 147, 113, 103, 39, 11, 253, 213, 231, 139, 36, 0, 40, 88, 180, 219, 233, 132, 100, 71, 40, 5, 243, 207, 202, 176, 153, 95, 84, 54, 22, 234, 140 }, 293) +
        UrlObfuscator.decode(new int[] { 82, 60, 81, 247, 220, 254, 150, 38, 26, 116, 87, 227, 192, 162, 132, 104, 3, 41, 91, 161, 129, 224, 134, 54, 2, 127, 92, 226, 248, 151, 166, 158, 111, 84, 100, 27, 165, 141, 167, 201, 99, 83, 35, 3, 234, 232, 161, 131, 106, 94, 97, 27, 169, 196, 170, 150, 103, 71, 51, 50, 30, 250, 212, 169, 136, 51, 69, 36, 71, 173, 215, 166, 150, 115, 90, 107, 18, 243 }, 59) +
        UrlObfuscator.decode(new int[] { 62, 14, 254, 220, 186, 137, 38, 111, 23, 44, 204, 143, 179, 171, 140, 116, 82, 60, 19, 255, 193, 255, 141, 119, 83, 105, 16, 246, 156, 187, 134, 104, 65, 46, 80, 253, 192, 235, 132, 113, 74, 121, 0, 245, 206, 147, 188, 137, 114, 105, 96, 11, 229, 158, 237 }, 76) +
        UrlObfuscator.decode(new int[] { 32, 31, 250, 206, 186, 144, 63, 83, 124, 15, 225, 215, 165, 133, 125, 64, 109, 75, 240, 215, 238, 211, 122, 91, 108, 76, 170, 153 }, 93);
    // Universal video loading indicator. Any <video> on any page that is
    // still fetching/buffering gets a spinner overlay (fades in, removed once
    // it can play), so the user never stares at a blank/grey box. Pure JS,
    // no per-site config; picks up videos added later too. Overlay is a
    // separate fixed element, so the page's own DOM/layout is untouched.
    // Idempotent (__vidLoaderBound).
    private static final String VIDEO_LOADER_JS =
        UrlObfuscator.decode(new int[] { 70, 235, 217, 165, 137, 125, 65, 40, 8, 173, 141, 184 }, 110) +
        UrlObfuscator.decode(new int[] { 22, 248, 149, 171, 146, 116, 93, 55, 0, 184, 234, 139, 133, 123, 85, 28, 0, 239, 201, 169, 153, 72, 70, 61, 9, 226, 140, 182, 134, 118, 84, 50, 49, 69, 234, 213, 181, 158, 118, 79, 121, 41, 202, 194, 186, 150, 93, 95, 46, 10, 232, 222, 137, 133, 124, 70, 35, 91, 241, 214, 182, 135, 58 }, 127) +
        UrlObfuscator.decode(new int[] { 230, 206, 188, 205, 126, 74, 44, 84, 184, 156 }, 144) +
        UrlObfuscator.decode(new int[] { 199, 181, 177, 157, 105, 85, 52, 20, 185, 219, 164, 133, 61, 29, 40, 6, 227, 201, 180, 135, 107, 4, 47, 5, 234, 221, 170, 131, 107, 80, 109, 5, 228, 212, 250, 178, 152, 113, 94, 52, 13, 218, 206, 159, 145, 60, 20, 13, 46, 230, 198, 170, 161, 99, 74, 46, 12, 250, 228, 181, 150, 35, 10, 107, 19, 229, 235, 203, 175, 146, 32, 76, 56, 10, 183, 222, 232, 144, 124, 81, 36, 29, 234, 192, 185, 194, 99, 79, 40, 12, 251, 218, 161, 139, 96, 87, 44, 5, 17, 234, 147, 184, 148, 121, 76, 53, 18, 248, 193, 145, 159, 119, 92, 53, 1, 250, 150, 165, 141, 34, 8, 32, 78, 244, 192, 176, 150, 112, 79, 123, 41, 31, 239, 156, 168, 142, 36, 92, 56, 21, 224, 217, 182, 156, 101, 30, 44, 28, 232, 205, 191, 143, 76, 68, 34, 11, 224, 202, 183, 202, 38, 83, 75, 39, 17, 249, 156, 243, 194, 107, 67, 120, 28, 240, 142, 245, 174, 79, 89, 39, 9, 192, 196, 171, 141, 109, 85, 5, 22, 247, 132, 249 }, 161) +
        "st.textContent='@keyframes __vlspin{to{transform:rotate(360deg)}}.__vl{position:fixed;z-index:2147483000;pointer-events:none;display:none;align-items:center;justify-content:center;background:rgba(8,12,28,.6);transition:opacity .25s ease;opacity:0;box-sizing:border-box}.__vl.on{opacity:1}.__vl i{width:38px;height:38px;border-radius:50%;border:3px solid rgba(255,255,255,.25);border-top-color:#fff;animation:__vlspin .8s linear infinite;box-sizing:border-box}';" +
        UrlObfuscator.decode(new int[] { 218, 255, 145, 127, 94, 40, 2, 239, 233, 161, 129, 107, 66, 109, 23, 247, 139, 250, 157, 156, 127, 73, 63, 19, 178, 220, 241, 140, 107, 72 }, 178) +
        UrlObfuscator.decode(new int[] { 165, 151, 111, 67, 75, 55, 18, 242, 155, 180, 156, 125, 83, 37, 93, 226, 154, 169, 133, 98, 86, 53 }, 195) +
        UrlObfuscator.decode(new int[] { 189, 149, 58, 71, 126, 10, 252, 223, 163, 153, 118, 85, 62, 73, 244, 192, 165, 135, 123, 114, 52, 62, 10, 248, 130, 230, 201, 101, 68, 33, 88, 251, 209, 167, 133, 126, 66, 36, 61, 249, 205, 191, 143, 52, 21, 122, 85, 172, 214, 166, 150, 116, 82, 81, 126, 27, 253, 215, 169, 156, 35 }, 212) +
        UrlObfuscator.decode(new int[] { 140, 98, 11, 99, 23, 174, 252, 203, 175, 142, 126, 84, 45, 43, 229, 213, 243, 210, 50, 68, 127, 23, 234, 218, 140, 152, 127, 88, 32, 10, 242, 210, 160, 204, 36, 81, 51, 3, 88, 183, 155, 250, 218, 108, 23, 41, 2, 243, 199, 173, 160, 119, 93, 53, 12, 250, 194, 190, 195, 45, 90, 39, 18, 244, 198, 161, 196, 43, 8, 50, 58, 10, 232, 206, 181, 218, 127, 89, 59, 5, 240, 143 }, 229) +
        UrlObfuscator.decode(new int[] { 132, 112, 64, 38, 0, 255, 144, 185, 192, 99, 73, 63, 29, 230, 218, 172, 181, 113, 69, 55, 7, 188, 157, 130, 236, 129, 96, 77, 116, 10, 253, 210, 189, 156, 122, 84, 46, 13, 177, 217, 224, 157, 109, 94, 57, 12, 236, 219, 186, 147, 42, 66, 55, 21, 239, 239, 210, 188, 133, 103, 70, 120, 89, 225, 152, 138, 171, 101, 94, 6, 17, 230, 218, 246 }, 246) +
        UrlObfuscator.decode(new int[] { 122, 69, 36, 16, 224, 202, 233, 133, 214, 101, 79, 57, 15, 239, 203, 182, 215, 112, 84, 56, 0, 247, 138, 173, 146 }, 263) +
        UrlObfuscator.decode(new int[] { 126, 66, 56, 22, 224, 218, 189, 159, 48, 71, 39, 9, 233, 131, 165, 192, 115, 72, 104, 6, 232, 194, 177, 146, 76, 118, 77, 41, 82, 233, 223, 180, 151, 97, 83, 125, 83, 252, 220, 246, 217, 52, 93, 40, 24, 223, 195, 164, 141, 104, 83, 49, 76, 229, 215, 175, 131, 139, 119, 82, 50, 83, 179, 194, 177, 145, 62, 20, 59, 93, 241, 221, 177, 156, 125, 97, 37, 24, 254, 135, 171, 136, 104, 81, 37, 10, 236, 210, 232, 248, 145, 115, 27, 114, 83, 246, 150, 164, 130, 108, 88, 54, 92, 245, 217, 188, 158, 97, 77, 50, 87, 174, 198, 168, 136, 96, 3, 120, 31, 173, 146, 137, 238, 212, 39, 70 }, 280) +
        UrlObfuscator.decode(new int[] { 79, 61, 9, 229, 209, 173, 140, 108, 1, 52, 54, 29, 246, 148, 242, 129, 122, 75, 36, 94, 188, 143, 165, 147, 99, 16, 57, 29, 176, 200, 164, 137, 124, 69, 34, 8, 241, 138, 164, 135, 117, 101, 83, 59, 16, 249, 213, 174, 138, 90, 78, 2, 20, 243, 253, 179, 156, 117, 7, 105, 27, 229, 207, 175, 134, 47, 14, 125, 19, 229, 209, 226, 128, 99, 107, 87, 43, 25, 166, 138, 226 }, 297) +
        UrlObfuscator.decode(new int[] { 89, 49, 15, 180, 205, 187, 139, 56, 94, 107, 69, 175, 218, 238, 135, 99, 1, 34, 8, 226, 204, 190, 129, 51, 78, 109, 78, 173, 216, 180, 128, 114, 63, 72, 96, 10, 232, 225, 176, 165, 44, 64, 52, 6, 179, 221, 236, 134, 33, 113, 18, 26, 231, 145, 191, 137, 117, 6, 55, 89, 245, 140, 166, 133, 139, 92, 82, 41, 21, 254, 208, 182, 144, 85, 89, 61, 22, 252, 197, 130, 138, 109, 89, 100, 66, 177, 223, 169, 149, 38, 70, 55, 94, 229, 196, 180, 156, 145, 112, 76, 46, 14, 252, 220, 132, 130, 108, 88, 54, 90, 231, 153, 244 }, 63) +
        UrlObfuscator.decode(new int[] { 38, 14, 252, 141, 191, 131, 101, 94, 117, 9, 227, 192, 160, 144, 42, 87, 105, 121, 88, 239, 146, 172, 147, 125, 76, 63, 72, 168, 130, 227, 212, 55, 66, 97, 6, 232, 197, 172, 130, 125, 22, 122, 80, 181, 130, 229, 144, 47, 66, 80, 42, 9, 243, 214, 228, 201, 62, 17, 36, 91, 224, 220, 162, 205, 121, 65, 32, 8, 254, 227, 175, 128, 111, 79, 50, 67, 162, 209, 236, 147, 105, 120, 86, 41, 66, 171, 156, 255, 138, 57, 90, 48, 18, 231, 142, 184, 158, 97, 75, 63, 59, 226, 206, 189, 128, 33, 0, 38, 23, 173, 212, 168, 147, 150, 124, 84, 48, 18, 238, 192, 249, 202, 43, 18, 60, 26, 246, 213, 181, 129, 41, 11, 106, 8, 249, 135, 172, 142, 117, 85, 40, 2, 251, 128, 253, 226, 217, 115, 83, 53, 31, 190, 131 }, 80) +
        UrlObfuscator.decode(new int[] { 8, 230, 183, 205, 181, 147, 108, 19, 34 }, 97) +
        UrlObfuscator.decode(new int[] { 27, 247, 152, 238, 129, 36, 87, 36, 87, 237, 199, 164, 147, 104, 65, 45, 22, 175, 195, 205, 187, 156, 104, 94, 31, 21, 253, 218, 179, 155, 96, 27, 117, 21, 249, 217, 233, 196, 55, 68, 100, 10, 228, 198, 181, 150, 74, 66, 47, 4, 189, 184, 225, 130, 138, 119, 29, 98, 23, 185, 215, 165, 132, 118, 92, 53, 51, 231, 199, 161, 136, 35, 78, 38, 11, 242, 203, 160, 138, 119, 12, 34, 18, 26, 255, 201, 185, 190, 118, 92, 53, 18, 248, 193, 252, 212, 123, 22, 121, 70, 181, 133, 168, 132, 105, 92, 37, 2, 232, 209, 234, 129, 109, 69, 57, 35, 2, 249, 211, 184, 143, 116, 93, 57, 2, 187, 208, 188, 145, 100, 93, 42, 0, 249, 233, 167, 143, 100, 77, 41, 18, 172, 138, 162, 146, 113, 69, 81, 58, 62, 244, 210, 182, 157, 48, 88, 127, 78, 226, 157, 141, 174, 102, 67, 115, 2, 183, 214 }, 114) +
        UrlObfuscator.decode(new int[] { 245, 195, 179, 192, 136, 127, 78, 97, 20, 180, 202, 172, 142, 122, 80, 122, 23, 251, 194, 160, 131, 111, 84, 113, 86, 183, 142, 174, 139, 99, 93, 99, 88 }, 131) +
        UrlObfuscator.decode(new int[] { 251, 157, 161, 133, 105, 67, 43, 67, 232, 194, 185, 153, 100, 70, 63, 88, 163, 197, 174, 132, 120, 56, 5, 50, 82, 232, 206, 160, 148, 114, 24, 57, 17, 245, 198, 236, 130, 33, 66, 40, 10, 255, 129, 238, 152, 127, 1, 126, 11, 173, 209, 181, 153, 147, 123, 19, 40, 20, 234, 132, 170, 217, 98, 90, 36, 88, 181, 193, 168, 200, 53, 66, 98, 24, 254, 208, 164, 130, 40, 82, 45, 7, 246, 201, 253, 173, 208, 106, 85, 63, 14, 241, 147, 240, 134, 109, 19, 104, 29, 191, 195, 187, 151, 97, 73, 101, 2, 236, 193, 160, 142, 113, 25, 49, 76, 233, 197, 214, 185, 149, 104, 16, 125, 9, 224, 144, 237, 154, 58, 64, 38, 8, 252, 202, 224, 143, 99, 89, 46, 12, 250, 245, 167, 129, 109, 86, 49, 92, 227, 236, 144, 191, 147, 105, 94, 60, 10, 197, 215, 177, 157, 102, 65, 106 }, 148) +
        UrlObfuscator.decode(new int[] { 204, 162, 203, 35, 86, 33, 44, 87, 230, 148, 189, 143, 119, 91, 35, 31, 250, 218, 251, 138, 56, 75, 61, 11, 252, 217, 174, 153, 125, 105, 41, 15, 232, 197, 183, 139, 110, 78, 121, 44, 28, 241, 222, 242, 159, 109, 89, 53, 1, 253, 220, 188, 217, 57, 84, 54, 67, 239, 199, 171, 154, 123, 107, 47, 22, 240, 141, 163, 133, 100, 55, 25, 50, 18, 188, 147, 226, 133, 62, 13, 40, 93, 187, 221, 248, 203, 114, 75, 33, 31, 238, 209, 166, 198, 100, 74, 36, 23, 240, 238, 168, 147, 139, 48, 92, 56, 31, 178, 158, 183, 153, 49, 28, 111, 14 }, 165) +
        UrlObfuscator.decode(new int[] { 215, 182, 128, 122, 68, 52, 91, 164, 149 }, 182) +
        UrlObfuscator.decode(new int[] { 186, 131, 105, 87, 38, 66, 232, 198, 151, 177, 219, 58, 84, 116, 10, 236, 206, 186, 144, 58, 87, 59, 2, 224, 195, 175, 148, 45, 22, 119, 78, 230, 200, 168, 128, 35, 5, 100, 14, 174, 252, 210, 188, 143, 104, 118, 48, 11, 227, 152, 182, 155, 125, 70, 48, 25, 225, 221, 229, 203, 100, 68, 110, 65, 174, 221, 173, 141, 103, 71, 105, 15, 86, 165, 192 }, 199) +
        "}" +
        UrlObfuscator.decode(new int[] { 170, 146, 98, 64, 38, 29, 178, 208, 179, 155, 103, 91, 41, 80, 247 }, 216) +
        UrlObfuscator.decode(new int[] { 143, 125, 73, 37, 17, 237, 204, 172, 193, 102, 109, 95, 48, 25, 179, 147, 162, 138, 118, 80, 104, 68, 168, 198, 163, 137, 116, 71, 43, 68, 255, 195, 170, 131, 47, 15, 123, 84, 170, 208, 160, 134, 194, 108, 88, 45, 14, 255, 202, 172, 182, 120, 92, 57, 18, 230, 216, 191, 129, 72, 95, 45, 6, 239, 129, 174, 149, 103, 72, 33, 74, 185, 220, 163, 190, 138, 126, 84, 115, 31, 176, 195, 170, 139 }, 233) +
        UrlObfuscator.decode(new int[] { 156, 108, 86, 52, 2, 252, 219, 189, 210, 102, 81, 38, 26, 165, 201, 226, 145, 96, 78, 111, 3, 171, 208, 162, 144, 102, 69, 75, 120, 91, 249, 149, 174, 152, 106, 80, 51, 1, 186, 199, 179, 150, 94, 78, 35, 8, 177, 150, 247, 206, 94, 110, 2, 32, 203, 132, 235, 132, 46, 107, 95, 47, 27, 254, 206, 247, 167, 72, 64, 57, 35, 242, 219, 165, 205, 123, 92, 56, 9, 176, 215 }, 250) +
        UrlObfuscator.decode(new int[] { 109, 95, 39, 11, 243, 207, 170, 138, 35, 77, 42, 72, 26, 183, 198, 181, 157, 50, 92, 118, 3, 247, 199, 179, 150, 102, 23, 118, 10, 160, 217, 173, 153, 109, 76, 60, 73, 242, 196, 163, 173, 99, 76, 37, 98, 67, 160, 155, 141, 179, 93, 125, 24, 81, 188, 209, 253, 134, 112, 66, 40, 11, 249, 130, 148, 181, 127, 68, 16, 7, 236, 208, 254, 132, 96, 76, 76, 59, 70, 225 }, 267) +
        UrlObfuscator.decode(new int[] { 71, 28, 45, 24, 241, 195, 191, 155, 115, 20, 126, 86, 227, 219, 175, 129, 96, 78, 46, 78, 164, 128, 181, 128, 97, 72, 43, 15, 231, 184, 227, 243, 154, 116, 72, 28, 25, 244, 222, 253, 146, 102, 92, 50, 4, 230, 193, 163, 196, 127, 3, 50, 12, 232, 197, 176, 137, 102, 76, 53, 78, 30, 250, 217, 153, 141, 127, 87, 44, 59, 255, 198, 160, 150, 124, 84, 34, 71, 250, 129, 187, 138, 99, 93, 100, 19, 244, 208, 161, 202, 57, 92, 105, 100 }, 284) +
        UrlObfuscator.decode(new int[] { 105, 118, 0, 227, 207, 180, 133, 101, 77, 110, 68, 160, 197, 164, 138, 115, 78, 32, 25, 88, 178, 154, 191, 154, 116, 73, 52, 22, 239, 193, 188, 129, 125, 68, 55, 7, 169, 129, 235, 135, 101, 72, 44, 2, 226, 193, 165, 151, 99, 6, 108, 120, 27, 239, 206, 180, 136, 62, 20, 112, 19, 248, 196, 167, 155, 116, 84, 104, 66, 170, 205, 169, 133, 123, 92, 96, 59, 171, 194, 172, 144, 68, 65, 92, 54, 85, 250, 206, 180, 154, 108, 94, 57, 27, 188, 199, 251, 138, 116, 64, 45, 24, 225, 206, 164, 157, 38, 70, 34, 1, 193, 213, 167, 143, 116, 83, 87, 46, 8, 254, 212, 188, 138, 63, 66, 121, 27, 248, 158, 165, 130, 122, 75, 100, 87, 246, 131, 242 }, 50) +
        UrlObfuscator.decode(new int[] { 48, 7, 245, 233, 209, 170, 152, 110, 77, 59, 21, 176, 209, 163, 155, 119, 71, 59, 30, 254, 135, 231, 150, 101, 77, 98, 72, 250, 198, 160, 204, 98, 81, 35, 12, 229, 183, 151, 230, 129, 55, 8, 108, 72, 190, 141 }, 67) +
        UrlObfuscator.decode(new int[] { 41, 90, 186, 152, 235 }, 84);
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
        UrlObfuscator.decode(new int[] { 77, 226, 214, 172, 130, 116, 118, 81, 51, 84, 178, 193 }, 101) +
        UrlObfuscator.decode(new int[] { 31, 243, 156, 164, 155, 127, 84, 32, 25, 163, 223, 163, 133, 126, 103, 55, 3, 235, 226, 170, 142, 100, 112, 86, 61, 22, 249, 201, 243, 139, 125, 67, 35, 7, 250, 136 }, 118) +
        UrlObfuscator.decode(new int[] { 240, 207, 171, 128, 108, 85, 111, 19, 23, 241, 202, 147, 139, 127, 87, 30, 30, 250, 208, 132, 154, 113, 90, 53, 29, 179, 203, 185, 133, 105, 93, 33, 8, 232, 141, 171, 147, 118, 82, 105, 36 }, 135) +
        UrlObfuscator.decode(new int[] { 247, 199, 162, 134, 41, 92, 34, 5, 227, 211, 178, 150, 113, 16 }, 152) +
        UrlObfuscator.decode(new int[] { 219, 173, 147, 115, 87, 42, 67, 236, 196, 183, 255, 174, 111, 83, 54, 19, 234, 221, 255, 144, 96, 90, 48, 6, 248, 223, 161, 198, 127, 73, 56, 5, 229, 222, 162, 202, 119, 65, 41, 7, 226, 212, 150, 165 }, 169) +
        UrlObfuscator.decode(new int[] { 204, 184, 138, 55, 95, 59, 4, 230, 198, 236, 148, 96, 77, 56, 1, 238, 196, 189, 198, 100, 84, 32, 5, 247, 199, 132, 140, 154, 115, 88, 50, 15, 178, 158, 177, 153, 102, 64, 32, 84, 187, 138 }, 186) +
        UrlObfuscator.decode(new int[] { 162, 132, 121, 93, 51, 72, 241, 221, 179, 135, 60, 7, 89, 55, 17, 249, 156, 225 }, 203) +
        UrlObfuscator.decode(new int[] { 181, 157, 50, 86, 40, 3, 229, 155, 185, 134, 126, 69, 57, 31, 226, 200, 229, 130, 100, 89, 61, 19, 168, 200, 177, 143, 118, 72, 48, 51, 27, 160, 200, 169, 143, 124, 3 }, 220) +
        UrlObfuscator.decode(new int[] { 153, 126, 82, 49, 31, 233, 213, 230, 128, 124, 87, 49, 92, 219, 194, 133, 245, 147, 107, 78, 42, 86, 227, 207, 165, 145, 96, 78, 45, 43, 210, 135, 227, 138, 100, 88, 12, 9, 228, 206, 237, 130, 118, 76, 34, 20, 22, 241, 211, 244, 143, 51, 66 }, 237) +
        UrlObfuscator.decode(new int[] { 136, 124, 78, 123, 27, 164, 204, 241, 208, 97, 26, 50, 17, 242, 213, 191, 154, 54, 69, 45, 66, 168, 201, 238, 148, 96, 80, 54, 16, 239, 155 }, 254) +
        UrlObfuscator.decode(new int[] { 64, 76, 39, 9, 232, 222, 231, 131, 98, 95, 54, 76, 226, 139, 239, 134, 144, 108, 120, 61, 24, 242, 145, 190, 130, 120, 86, 32, 26, 253, 223, 248, 130, 103, 64, 41, 66, 241, 204, 176, 147, 117, 11, 52, 22, 241, 201, 232, 178, 151, 112, 89, 114, 65 }, 271) +
        UrlObfuscator.decode(new int[] { 8, 94, 5, 16, 245, 214, 191, 164, 100, 75, 13, 40, 189, 157, 180, 158, 98, 106, 47, 14, 228, 131, 172, 156, 102, 68, 50, 12, 235, 205, 234, 132, 120, 107, 23, 38, 25, 227, 206, 170, 214, 103, 67, 38, 28, 187, 215, 169, 132, 38, 21, 48, 69, 176, 215, 224, 211, 122, 15, 126 }, 288) +
        UrlObfuscator.decode(new int[] { 95, 51, 92, 246, 202, 165, 131, 33, 66, 40, 2, 236, 222, 161, 193, 110, 72, 53, 17, 247, 140, 160, 131, 156, 123, 77, 40, 70, 255, 193, 172, 132, 56, 95, 59, 26, 252, 153, 247, 195, 41, 4, 119, 22, 233, 200, 188, 132, 110, 13, 33, 74, 249, 220 }, 54) +
        UrlObfuscator.decode(new int[] { 46, 8, 245, 209, 183, 204, 114, 84, 70, 50, 24, 178, 223, 179, 138, 104, 91, 55, 12, 169, 148, 188, 158, 126, 74, 105, 86 }, 71) +
        UrlObfuscator.decode(new int[] { 60, 24, 245, 192, 185, 150, 124, 69, 126, 13, 225, 201, 181, 197, 107, 89, 56, 2, 232, 193, 135, 139, 107, 77, 36, 119, 23, 243, 204, 174, 142, 48, 3 }, 88) +
        UrlObfuscator.decode(new int[] { 0, 230, 215, 179, 145, 42, 66, 38, 5, 197, 233, 219, 179, 136, 87, 83, 42, 12, 242, 216, 176, 134, 59, 21, 50, 24, 238, 192, 170, 137, 44, 6, 47, 29, 233, 197, 177, 141, 108, 76, 105, 73, 4 }, 105) +
        UrlObfuscator.decode(new int[] { 14, 235, 193, 172, 146, 122, 87, 38, 31, 244, 222, 187, 192, 111, 67, 47, 19, 167, 218, 162, 139, 106, 82, 38, 33, 233, 201, 211, 186, 213, 117, 85, 42, 12, 236, 158, 237, 136, 119, 82, 38, 18, 248, 135, 171, 196, 119, 86 }, 122) +
        UrlObfuscator.decode(new int[] { 226, 204, 225, 201, 110, 72, 53, 17, 247, 140, 167, 137, 147, 123, 78, 32, 7, 187, 208, 182, 135, 99, 65, 122, 21, 251, 221, 181, 156, 32, 65, 41, 5, 237, 221, 160, 206, 125 }, 139) +
        UrlObfuscator.decode(new int[] { 234, 218, 168, 217, 125, 69, 36, 78, 224, 193, 171, 138, 117, 93, 60, 80, 226, 206, 189, 201, 76, 104, 11, 32, 252, 192, 167, 145, 116, 118, 81, 51, 84, 188, 238, 177, 157, 55, 67, 38, 17, 225, 146, 176, 146, 96, 92, 57, 9, 239, 138, 168, 200, 117, 67, 52, 17, 230, 209, 181, 206, 216, 50, 26, 29, 25, 245, 203, 172, 178, 100, 71, 59, 1, 181, 152, 235, 146 }, 156) +
        UrlObfuscator.decode(new int[] { 206, 173, 159, 105, 65, 96, 2, 175, 222, 161, 145, 112, 28, 46, 58, 9, 189, 249, 169, 136, 118, 74, 127, 81, 212, 214, 188, 128, 101, 117, 61, 28, 226, 222, 236, 195, 50, 85 }, 173) +
        UrlObfuscator.decode(new int[] { 204, 184, 150, 126, 89, 45, 80, 242, 196, 167, 221, 40, 64, 52, 4, 250, 220, 163, 215, 118 }, 190) +
        UrlObfuscator.decode(new int[] { 185, 143, 127, 12, 35, 11, 231, 204, 171, 131, 118, 25, 2, 16, 243, 193, 198, 240, 141, 110, 84, 46, 22, 236, 206, 166, 144, 58, 94, 51, 1, 190, 204, 175, 129, 96, 3, 35, 7, 248, 210, 178, 203, 98, 74, 46, 4, 243, 179, 216, 168, 146, 120, 78, 48, 23, 249, 158, 179, 157, 127, 87, 120, 11 }, 207) +
        UrlObfuscator.decode(new int[] { 146, 154, 106, 72, 46, 21, 225, 210, 177, 153, 114, 15, 115, 21, 251, 221, 181, 200, 34, 67, 45, 6, 239, 147, 174, 142, 106, 64, 106, 13, 227, 204, 165, 243, 153, 120, 72, 29, 19, 245, 221, 237, 144, 96, 90, 48, 6, 248, 223, 161, 198, 36, 87, 57, 15, 253, 221, 181, 136, 37, 116, 49, 13, 236, 201, 204, 187, 211, 110, 94, 41, 22, 244, 193, 179, 221, 114, 90, 62, 20, 185, 148, 179, 144, 55, 86, 99, 82 }, 224) +
        UrlObfuscator.decode(new int[] { 131, 117, 92, 33, 1, 250, 206, 226, 134, 120, 83, 53, 75, 233, 214, 174, 149, 105, 111, 82, 56, 67, 243, 219, 183, 156, 123, 83, 38, 78, 200, 218, 176, 158, 107, 66, 40, 31, 208, 154, 148, 181, 46, 29, 56, 77, 184 }, 241) +
        UrlObfuscator.decode(new int[] { 107, 79, 48, 42, 10, 179, 223, 183, 147, 122, 83, 127, 95, 174, 201, 250, 201, 108, 11 }, 258) +
        UrlObfuscator.decode(new int[] { 110, 26, 120, 89 }, 275);
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
            line.setColor(Color.parseColor(UrlObfuscator.decode(new int[] { 7, 115, 32, 176, 151, 139, 238 }, 292)));
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
            int fur = Color.parseColor(UrlObfuscator.decode(new int[] { 25, 31, 76, 209, 131, 147, 182 }, 58));
            int pink = Color.parseColor(UrlObfuscator.decode(new int[] { 104, 44, 189, 233, 244, 165, 53 }, 75));
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
            line.setColor(Color.parseColor(UrlObfuscator.decode(new int[] { 127, 58, 223, 251, 224, 178, 38 }, 92)));
            c.drawLine(22, 47, 9, 44, line);
            c.drawLine(22, 52, 9, 53, line);
            c.drawLine(78, 47, 91, 44, line);
            c.drawLine(78, 52, 91, 53, line);
            line.setColor(Color.parseColor(UrlObfuscator.decode(new int[] { 78, 188, 233, 251, 222, 60, 23 }, 109)));

            fill.setColor(Color.parseColor(UrlObfuscator.decode(new int[] { 93, 172, 136, 233, 206, 44, 123 }, 126)));
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
            shape(c, tear, Color.parseColor(UrlObfuscator.decode(new int[] { 172, 150, 136, 168, 51, 108, 113 }, 143)));

            android.graphics.Path nose = new android.graphics.Path();
            nose.moveTo(47.5f, 48f); nose.lineTo(52.5f, 48f); nose.lineTo(50f, 51f); nose.close();
            fill.setColor(Color.parseColor(UrlObfuscator.decode(new int[] { 131, 250, 230, 202, 41, 2, 31 }, 160))); c.drawPath(nose, fill);

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
            paint.setColor(Color.parseColor(UrlObfuscator.decode(new int[] { 146, 150, 221, 72, 31, 9, 46 }, 177)));
            canvas.drawCircle(cx, cy, stroke * 1.05f, paint);

            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(stroke);
            paint.setColor(Color.parseColor(UrlObfuscator.decode(new int[] { 225, 212, 101, 122, 95, 57, 72 }, 194)));
            float[] radii = {w * 0.24f, w * 0.38f};
            int[] alphas = {235, 130};
            for (int i = 0; i < radii.length; i++) {
                paint.setAlpha(alphas[i]);
                RectF arc = new RectF(cx - radii[i], cy - radii[i], cx + radii[i], cy + radii[i]);
                canvas.drawArc(arc, 208, 124, false, paint);
            }

            paint.setAlpha(255);
            paint.setColor(Color.parseColor(UrlObfuscator.decode(new int[] { 240, 180, 87, 6, 13, 91, 186 }, 211)));
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
            String name = (appName == null || appName.trim().isEmpty()) ? UrlObfuscator.decode(new int[] { 165, 115, 82 }, 228) : appName.trim().toUpperCase();

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
            letter.setTypeface(Typeface.create(UrlObfuscator.decode(new int[] { 134, 117, 93, 33, 92, 227, 202, 188, 132, 106, 6, 39, 12, 236, 206, 179, 136 }, 245), Typeface.NORMAL));
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
                videoResId = context.getResources().getIdentifier(UrlObfuscator.decode(new int[] { 101, 80, 55, 23, 237, 204, 159, 172, 142, 113, 93, 40, 18 }, 262), UrlObfuscator.decode(new int[] { 101, 87, 34 }, 279), pkg);
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
                int resId = context.getResources().getIdentifier(UrlObfuscator.decode(new int[] { 75, 50, 21, 241, 203, 174, 189, 114, 80, 83, 63, 14, 244 }, 296), UrlObfuscator.decode(new int[] { 90, 47, 29, 236, 219, 187, 148, 114 }, 62), pkg);
                if (resId != 0) {
                    iv.setImageURI(Uri.parse(UrlObfuscator.decode(new int[] { 46, 0, 233, 222, 164, 131, 109, 6, 53, 3, 246, 203, 182, 144, 98, 69, 5, 113, 82 }, 79) + pkg + "/" + resId));
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
                mp.setDataSource(context, Uri.parse(UrlObfuscator.decode(new int[] { 1, 17, 250, 207, 179, 146, 126, 23, 42, 18, 229, 218, 161, 129, 113, 84, 106, 64, 161 }, 96) + pkg + "/" + videoResId));
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
            int iconResId = context.getResources().getIdentifier(UrlObfuscator.decode(new int[] { 24, 243, 240, 162, 140, 121, 69, 41, 1, 237, 213 }, 113), UrlObfuscator.decode(new int[] { 239, 200, 176, 178, 159, 109 }, 130), context.getPackageName());
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
        root.setBackgroundColor(Color.parseColor(UrlObfuscator.decode(new int[] { 176, 130, 228, 192, 58, 30, 120 }, 147)));

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
        swipeRefresh.setColorSchemeColors(Color.parseColor(UrlObfuscator.decode(new int[] { 135, 241, 166, 55, 99, 123, 24 }, 164)));
        swipeRefresh.addView(webView, new ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        root.addView(swipeRefresh, webParams);

        // Status-bar and navigation-bar strips. Added before the splash /
        // error / navigation overlays so those still cover the whole screen.
        topScrim = new View(this);
        topScrim.setBackgroundColor(Color.parseColor(UrlObfuscator.decode(new int[] { 150, 228, 198, 34, 4, 96, 90 }, 181)));
        root.addView(topScrim, new FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT, 0, Gravity.TOP));
        bottomScrim = new View(this);
        bottomScrim.setBackgroundColor(Color.parseColor(UrlObfuscator.decode(new int[] { 229, 213, 49, 19, 119, 81, 181 }, 198)));
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

        final SplashController loading = new StaticIconSplashView(this, Color.parseColor(UrlObfuscator.decode(new int[] { 244, 198, 37, 4, 99, 66, 161 }, 215)));
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
        final String offlineScrimColor = offlineIsNightMode ? UrlObfuscator.decode(new int[] { 203, 62, 31, 117, 84, 179, 146, 241, 208 }, 232) : UrlObfuscator.decode(new int[] { 218, 46, 1, 102, 69, 164, 131, 226, 193 }, 249);
        // Slightly translucent (not fully opaque) card/button fills, so a
        // hint of the dimmed scrim behind still shows through -- a cheap
        // stand-in for a true frosted-glass blur, which would need
        // RenderEffect (API 31+) and a snapshot of whatever's behind the
        // card to do for real.
        final String offlineCardBg = offlineIsNightMode ? UrlObfuscator.decode(new int[] { 41, 108, 112, 85, 197, 151, 129, 208, 50 }, 266) : UrlObfuscator.decode(new int[] { 56, 120, 97, 62, 165, 240, 231, 178, 33 }, 283);
        final String offlineTitleColor = offlineIsNightMode ? UrlObfuscator.decode(new int[] { 18, 22, 41, 200, 235, 138, 173 }, 49) : UrlObfuscator.decode(new int[] { 97, 81, 176, 175, 142, 237, 204 }, 66);
        final String offlineSubtitleColor = offlineIsNightMode ? UrlObfuscator.decode(new int[] { 112, 48, 169, 242, 247, 172, 73 }, 83) : UrlObfuscator.decode(new int[] { 71, 182, 224, 244, 162, 202, 91 }, 100);
        final String offlineButtonBg = offlineIsNightMode ? UrlObfuscator.decode(new int[] { 86, 172, 240, 230, 201, 36, 23, 122, 44 }, 117) : UrlObfuscator.decode(new int[] { 165, 157, 135, 167, 52, 101, 118, 27, 63 }, 134);
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
        errorTitle.setText(UrlObfuscator.decode(new int[] { 217, 217, 245, 154, 118, 70, 38, 31, 253, 197 }, 151));
        errorTitle.setTextColor(Color.parseColor(offlineTitleColor));
        errorTitle.setTextSize(16);
        errorTitle.setTypeface(errorTitle.getTypeface(), Typeface.BOLD);
        errorTitle.setGravity(Gravity.START);
        errorCard.addView(errorTitle);

        final TextView errorSubtitle = new TextView(this);
        errorSubtitle.setText(UrlObfuscator.decode(new int[] { 248, 171, 131, 100, 87, 38, 66, 243, 197, 203, 172, 132, 60, 76, 50, 28, 246, 151, 181, 154, 122, 93, 55, 18, 228, 202, 170, 205, 120, 68, 106, 0, 230, 211, 163, 151, 106, 70, 54 }, 168));
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
        retryText.setText(UrlObfuscator.decode(new int[] { 235, 189, 131, 100, 76 }, 185));
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
        navOverlay.setBackgroundColor(Color.parseColor(UrlObfuscator.decode(new int[] { 233, 217, 56, 23, 118, 85, 180 }, 202)));
        navOverlay.setVisibility(View.GONE);
        final BouncingDotsView navDots = new BouncingDotsView(this, Color.parseColor(UrlObfuscator.decode(new int[] { 248, 188, 43, 126, 101, 51, 208 }, 219)));
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
        String spoofedUA = defaultUA.replace(UrlObfuscator.decode(new int[] { 215, 43, 93, 63 }, 236), "").replaceAll("Version/[0-9.]+\s", "");
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
        webView.addJavascriptInterface(new AndroidBridge(), UrlObfuscator.decode(new int[] { 188, 114, 95, 40, 22, 241, 211, 148, 135, 125, 87, 53, 20 }, 253));

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
            WebViewCompat.addDocumentStartJavaScript(
                webView, VIDEO_LOADER_JS, java.util.Collections.singleton("*"));
            WebViewCompat.addDocumentStartJavaScript(
                webView, NATIVE_FEEL_JS, java.util.Collections.singleton("*"));
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
                view.evaluateJavascript(NATIVE_FEEL_JS, null);
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
                view.evaluateJavascript(ZOOM_LOCK_JS, null);
                view.evaluateJavascript(VIDEO_LOADER_JS, null);
                view.evaluateJavascript(NATIVE_FEEL_JS, null);
                // Picks up the page's <meta name="theme-color"> (if it has
                // one) to recolor the system bars, and wires up a light
                // haptic tap on buttons/links -- both no-ops wrapped in
                // try/catch so a page that doesn't have a theme-color tag,
                // or that runs somewhere AndroidBridge isn't defined (the
                // popup WebView below doesn't get it), just silently skips
                // rather than throwing a JS error.
                view.evaluateJavascript(
                    UrlObfuscator.decode(new int[] { 38, 75, 57, 5, 233, 221, 161, 136, 104, 13, 109, 24 }, 270) +
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
                    UrlObfuscator.decode(new int[] { 107, 76, 36, 7 }, 287) +
                    "var vp=document.querySelector('meta[name=\"viewport\"]');" +
                    UrlObfuscator.decode(new int[] { 92, 50, 91, 179, 199, 160, 198, 117, 91, 60, 86, 238, 198, 171, 146, 107, 64, 42, 23, 172, 194, 178, 186, 159, 105, 89, 30, 22, 252, 213, 178, 152, 97, 28, 116, 31, 244, 196, 174, 201, 36, 23, 61, 26, 167, 219, 162, 146, 68, 80, 55, 16, 232, 194, 202, 170, 152, 52, 28, 52, 24, 245, 210, 241, 217, 51, 69, 59, 20, 231, 223, 161, 159, 120, 12, 99, 82, 236, 200, 165, 144, 105, 70, 44, 21, 174, 247, 219, 188, 152, 53, 91, 41, 8, 242, 216, 177, 183, 123, 91, 61, 20, 167, 216, 189, 197, 48, 87 }, 53) +
                    UrlObfuscator.decode(new int[] { 48, 21, 170, 208, 167, 149, 65, 107, 74, 47, 21, 249, 207, 173, 157, 63, 17, 54, 27, 253, 198, 180, 158, 123, 9, 97, 75, 252, 195, 173, 156, 111, 27, 33, 1, 245, 203, 162, 133, 210, 105, 84, 56, 15, 242, 149, 248, 158, 120, 92, 32, 26, 243, 221, 253, 156, 109, 76, 32, 14, 183, 152, 230, 215, 42, 5, 41, 2, 250, 200, 173, 170, 147, 48, 79, 56, 27, 245, 221, 234, 199, 59, 4, 127, 82, 252, 217, 161, 135, 96, 89, 38, 71, 250, 203, 166, 138, 96, 25, 114, 76, 177, 140, 159, 171, 142, 121, 73, 119, 10, 251, 214, 186, 148, 118, 95, 55, 76, 254, 192, 233, 196, 55 }, 70) +
                    UrlObfuscator.decode(new int[] { 62, 16, 189, 149, 183, 157, 114, 69, 34, 11, 227, 216, 229, 141, 108, 92, 2, 10, 224, 201, 166, 140, 117, 98, 70, 23, 25, 180, 156, 133, 166, 121, 89, 50, 7, 251, 218, 182, 167, 121, 74, 57, 29, 227, 217, 190, 175, 97, 95, 97, 76, 173, 216 }, 87) +
                    UrlObfuscator.decode(new int[] { 30, 230, 212, 229, 151, 119, 31, 37, 15, 28, 235, 208, 185, 149, 110, 23, 59, 5, 243, 212, 160, 150, 87, 93, 53, 2, 235, 195, 184, 195, 45, 90, 60, 30, 234, 192, 227, 202, 57, 82, 52, 113, 23, 249, 129, 252, 165, 70, 89, 57, 18, 231, 219, 186, 150, 71, 89, 42, 25, 253, 195, 185, 158, 79, 65, 63, 65, 190 }, 104) +
                    UrlObfuscator.decode(new int[] { 10, 236, 153, 162, 144, 108, 71, 17, 30, 254, 219, 171, 131, 120, 22, 109, 1, 252, 202, 170, 201, 102, 76, 38, 24, 251, 242, 223, 165, 209, 108, 83, 61, 12, 255, 140, 228, 196, 35, 68, 38, 81, 230, 195, 189, 131, 121, 94, 40, 6, 243, 157, 170, 146, 102, 80, 39, 12, 16, 233, 144, 164, 193, 114, 80, 60, 19, 243, 219, 245, 154, 127, 65, 63, 29, 250, 204, 162, 159, 49, 70, 62, 2, 244, 195, 168, 140, 117, 12, 56, 101, 29, 241, 213, 171, 219, 112, 85, 39, 25, 231, 192, 178, 156, 101, 11, 98, 25, 232, 206, 160, 131, 125, 5, 51, 3, 253, 208, 238, 145, 104, 90, 90, 115, 28, 248, 209, 175, 138, 108, 13, 103, 69, 164, 150, 243, 152, 125, 95, 33, 31, 248, 202, 164, 157, 51, 83, 35, 29, 240, 142, 177, 136, 122, 122, 19, 60, 24, 241, 207, 170, 140, 45, 7, 101, 68, 182, 147, 184, 157, 127, 65, 63, 24, 234, 196, 189, 211, 122, 1, 126 }, 121) +
                    UrlObfuscator.decode(new int[] { 238, 198, 171, 146, 107, 64, 42, 23, 172, 201, 165, 190, 154, 51, 93, 43, 10, 252, 214, 179, 181, 125, 93, 63, 22, 185, 195, 187, 199, 54, 81 }, 138) +
                    UrlObfuscator.decode(new int[] { 230, 217, 184, 140, 116, 94, 125, 17, 186, 201, 172 }, 155) +
                    "try{var m=document.querySelector('meta[name=\"theme-color\"]');" +
                    UrlObfuscator.decode(new int[] { 197, 173, 194, 100, 14, 97, 17, 236, 202, 167, 141, 118, 14, 126, 48, 25, 238, 212, 179, 157, 90, 69, 63, 17, 243, 214, 244, 215, 81, 65, 42, 31, 227, 194, 174, 171, 122, 78, 34, 2, 225, 141, 163, 145, 112, 115, 71, 9, 20, 254, 215, 188, 187, 120, 90, 58, 6, 186, 201, 144, 158, 107, 92, 34, 5, 239, 232, 187, 129, 99, 65, 32, 74, 226, 210, 177, 140, 134, 74, 85, 57, 22, 255, 250, 183, 155, 121, 71, 124, 30, 188, 214, 181, 155, 79, 89, 56, 25, 227, 203, 189, 147, 99, 13, 99, 0, 237, 207, 180, 186, 144, 105, 27, 114, 6, 229, 159, 240, 223, 46, 73 }, 172) +
                    UrlObfuscator.decode(new int[] { 192, 191, 154, 110, 90, 48, 95, 243, 156, 175, 142 }, 189) +
                    UrlObfuscator.decode(new int[] { 186, 159, 117, 80, 35, 15, 160, 208, 175, 139, 96, 76, 53, 79, 193, 241, 218, 175, 147, 114, 94, 27, 10, 254, 210, 178, 145, 53, 20, 16, 30, 235, 220, 162, 133, 111, 104, 59, 1, 227, 193, 160, 202, 98, 82, 49, 12, 6, 206, 220, 187, 158, 88, 88, 59, 28, 241, 199, 187, 134, 124, 85, 121, 20 }, 206) +
                    UrlObfuscator.decode(new int[] { 169, 159, 111, 28, 43, 19, 250, 211, 149, 145, 40, 82, 38, 28, 242, 196, 166, 129, 99, 4, 98, 17 }, 223) +
                    UrlObfuscator.decode(new int[] { 134, 110, 92, 109, 15, 227, 203, 160, 134, 58, 125, 24, 95, 245, 195, 179, 192, 154, 35, 89, 51, 24, 239, 212, 189, 153, 98, 27, 49, 31, 247, 220, 181, 129, 122, 107, 62, 4, 231, 249, 167, 142, 104, 81, 108, 46, 227, 213, 168, 241, 152, 113, 83, 52, 8, 177, 207, 190, 152, 113, 91, 36, 92, 248, 222, 161, 139, 127, 123, 34, 14, 253, 192, 232, 212, 44, 8, 52, 11, 239, 196, 208, 169, 211, 117, 85, 52, 28, 234, 255, 179, 156, 115, 91, 38, 92, 162, 134, 245 }, 240) +
                    UrlObfuscator.decode(new int[] { 118, 72, 86, 50, 24, 180, 222, 243, 130, 123, 95, 55, 28, 250, 157, 162, 132, 99, 71, 102, 8, 165, 144, 175, 212, 109, 9, 54, 4, 246, 198, 172, 149, 69, 115, 91, 48, 25, 245, 206, 226, 133 }, 257) +
                    UrlObfuscator.decode(new int[] { 123, 87, 120, 11, 225, 206, 185, 134, 111, 71, 60, 73, 228, 202, 160, 154, 36, 7, 35, 55, 31, 244, 210, 245, 147, 119, 92, 50, 14, 218, 210, 251, 150, 126, 83, 58, 3, 232, 194, 191, 196, 107, 71, 35, 31, 172, 152, 243, 203, 98, 72, 94, 55, 19, 178, 203, 175, 138, 112, 31, 50, 26, 247, 198, 191, 148, 126, 91, 96, 15, 227, 207, 179, 192, 51 }, 274) +
                    UrlObfuscator.decode(new int[] { 74, 36, 73, 227, 247, 223, 180, 146, 53, 83, 55, 28, 242, 206, 154, 146, 59, 86, 62, 19, 250, 195, 168, 130, 127, 4, 45, 7, 228, 211, 168, 129, 109, 86, 4, 12, 26, 243, 216, 178, 143, 51, 5, 104, 94, 245, 221, 181, 154, 124, 31, 32, 26, 253, 197, 228, 143, 101, 74, 61, 10, 227, 203, 176, 205, 102, 78, 35, 42, 19, 248, 210, 175, 191, 117, 93, 58, 19, 251, 192, 250, 201 }, 291) +
                    UrlObfuscator.decode(new int[] { 79, 57, 5, 182, 197, 181, 129, 97, 84, 109, 9, 251, 195, 175, 159, 99, 70, 38, 79, 245, 140, 191, 138, 100, 9, 97, 44, 87, 239, 217, 175, 143, 107, 86, 119, 24, 224, 216, 191, 201, 103, 81, 61, 78, 236, 145, 184, 196, 96, 70, 35, 3, 253, 235, 165, 202, 38, 8, 24, 119, 70, 234, 218, 168, 217, 122, 10, 37, 91, 253, 221, 182, 148, 104, 96, 40, 69, 171, 130, 237, 192, 51, 78, 32, 77, 229, 159, 242, 157, 124, 125, 2, 109, 85, 233, 223, 173, 141, 101, 88, 117, 26, 230, 222, 189, 203 }, 57) +
                    UrlObfuscator.decode(new int[] { 60, 8, 250, 135, 176, 216, 119, 13, 49, 20, 226, 236, 202, 175, 149, 117, 93, 113, 25, 188, 135, 249, 150, 58, 28, 34, 0, 227, 199, 185, 196, 44, 6, 110, 65, 188, 207, 163, 204, 117, 12, 45, 5, 17, 249, 201, 180, 199, 41, 16, 42, 18, 226, 192, 166, 157, 50, 95, 37, 3, 226, 150 }, 74) +
                    UrlObfuscator.decode(new int[] { 45, 27, 235, 152, 182, 154, 40, 66, 125, 30, 244, 222, 168, 154, 101, 18, 120, 85, 249, 201, 181, 149, 96, 98, 47, 13, 224, 212, 151, 168, 166, 47, 102, 115, 67, 169, 140, 160, 148, 102, 19, 32, 76, 224, 206, 188, 158, 105, 98, 36, 29, 160, 209, 157, 213, 89, 15, 115, 81, 169, 179, 217, 224, 140, 122, 72, 42, 29, 222, 216, 161, 220, 101, 105, 96, 45, 163, 159, 253, 197, 39, 72, 37, 85, 247, 199, 183, 151, 102, 107, 47, 20, 87, 232, 230, 238, 166, 54, 8, 104, 94, 173 }, 91) +
                    UrlObfuscator.decode(new int[] { 5, 237, 130, 160, 155, 73, 71, 11, 76, 241, 139, 189, 156, 150, 109, 115, 61, 53, 178, 222, 241, 139, 106, 92, 39, 61, 243, 255, 248, 141, 98, 4, 101, 25, 239, 221, 189, 149, 104, 5, 42, 22, 238, 205, 251, 173, 155, 105, 73, 41, 20, 226, 202, 237, 132, 57, 83, 105, 21, 189, 210, 245, 140, 97, 0, 42, 80, 232, 196, 186, 221, 120, 31 }, 108) +
                    UrlObfuscator.decode(new int[] { 11, 253, 201, 250, 145, 125, 79, 107, 19, 225, 221, 177, 133, 121, 64, 32, 69, 239, 130, 177, 159, 105, 85, 102, 13, 185, 197, 183, 143, 99, 107, 87, 50, 18, 179, 212, 240, 131, 121, 11, 24, 21, 231, 218, 255, 157, 110, 86, 101, 92, 167, 231, 168, 156, 111, 8, 40, 13, 237, 138, 243, 213, 202, 50, 83, 117, 82, 161, 203, 189, 131, 99, 71, 58, 83, 186, 223, 236, 222, 56, 18, 107, 91, 173, 147, 239, 192, 47, 14, 42, 77, 246, 206, 147, 171, 140, 116, 82, 60, 82, 168, 142, 254, 205, 104, 15, 33, 23, 229, 197, 189, 128, 45, 11, 104, 77, 162, 192, 239, 133, 43, 86, 106, 73, 233, 136, 220, 240, 154, 53, 16, 50, 81, 251, 153, 180, 220, 47, 78, 105 }, 125) +
                    UrlObfuscator.decode(new int[] { 232, 194, 190, 195, 124, 72, 58, 71, 237, 152, 244, 216, 105, 29, 35, 55, 31, 244, 210, 245, 150, 124, 86, 48, 2, 253, 143, 184, 217, 58, 25, 52, 24, 236, 222, 235, 137, 52, 88, 38, 20, 246, 193, 235, 133, 100, 84, 124, 49, 16, 236, 206, 174, 156, 124, 100, 34, 12, 248, 214, 250, 146, 120, 78, 39, 3, 215, 192, 151, 192, 38, 69, 39, 6, 239, 196, 176, 142, 117, 113, 90, 30, 19, 247, 213, 171, 209, 44, 95, 51, 92, 240, 148, 247, 147, 33, 79, 115, 81, 187, 132, 240, 209, 46, 84, 32, 16, 246, 208, 175, 192, 151, 123, 69, 116, 24, 179, 130, 165 }, 142) +
                    UrlObfuscator.decode(new int[] { 233, 223, 175, 220, 111, 7, 41, 25, 229, 197, 176, 220, 116, 87, 37, 51, 224, 195, 189, 153, 127, 79, 45, 59, 243, 223, 169, 129, 43, 70, 46, 3, 10, 243, 216, 178, 143, 52, 91, 55, 19, 239, 201, 168, 151, 125, 82, 37, 2, 235, 195, 184, 197, 110, 70, 43, 18, 235, 192, 170, 151, 71, 77, 37, 50, 27, 243, 200, 242, 212, 122, 87, 59, 25, 231, 157, 232 }, 159) +
                    UrlObfuscator.decode(new int[] { 217, 169, 198, 121, 10, 109, 66, 185, 134, 245, 223, 60, 14, 55, 76, 243, 139, 143, 240, 200, 36, 12, 112, 13, 182, 208, 253, 197, 58, 2, 99, 69, 186, 219, 224, 143, 37, 4, 120, 92, 189, 153, 246, 203, 50, 10, 48, 4, 244, 234, 204, 179, 220, 60, 25, 105, 77, 167, 131, 229, 193, 52, 9 }, 176) +
                    UrlObfuscator.decode(new int[] { 179, 133, 139, 107, 79, 50, 91, 189, 154, 190, 145, 112, 83, 50, 21, 181, 138, 173, 212 }, 193) +
                    UrlObfuscator.decode(new int[] { 164, 144, 98, 15, 61, 8, 226, 207, 136, 142, 53, 65, 51, 11, 231, 215, 171, 142, 110, 55, 23, 38, 8, 233, 195, 162, 142, 118, 68, 117, 28, 174, 194, 184, 147, 100, 108, 42, 68, 162, 145, 160, 142, 47, 78, 100, 89, 190, 213, 168, 142, 155, 113, 74, 114, 36, 197, 216, 182, 147, 100, 90, 61, 23, 208, 214, 156, 142, 125, 89, 101, 16, 253, 192, 166, 131, 105, 82, 106, 60, 221, 192, 174, 187, 140, 114, 85, 63, 56, 254, 244, 182, 133, 97, 9, 59, 73, 208, 222, 171, 156, 98, 69, 47, 40, 251, 193, 163, 129, 96, 10, 34, 18, 241, 204, 198, 142, 156, 123, 94, 24, 24, 251, 220, 177, 135, 123, 70, 60, 21, 184, 199, 231, 214, 113, 86, 41, 8, 252, 196, 174, 205, 97, 10, 57, 28, 253, 164 }, 210) +
                    UrlObfuscator.decode(new int[] { 144, 103, 79, 36, 29, 25, 181, 149, 224 }, 227) +
                    UrlObfuscator.decode(new int[] { 157, 117, 26, 112, 7, 230, 192, 169, 131, 124, 4, 22, 55, 230, 200, 161, 150, 108, 75, 37, 34, 24, 220, 210, 169, 149, 126, 16, 35, 0, 255, 219, 176, 156, 101, 31, 15, 48, 239, 195, 168, 153, 101, 64, 44, 37, 225, 231, 171, 150, 108, 69, 125, 43, 12, 232, 217, 224, 158, 118, 91, 34, 27, 240, 218, 167, 220, 112, 84, 43, 43, 251, 201, 165, 158, 69, 65, 52, 18, 224, 202, 166, 144, 41, 7, 89, 49, 30, 233, 200, 179, 151, 63, 27, 37, 16, 250, 215, 144, 150, 60, 91, 60, 24, 233, 130, 241 }, 244) +
                    UrlObfuscator.decode(new int[] { 97, 75, 32, 23, 236, 197, 209, 170, 211, 125, 95, 62, 60, 238, 210, 184, 129, 88, 90, 33, 5, 245, 193, 171, 159, 36, 12, 62, 6, 253, 196, 174, 150, 112, 66, 48, 21, 167, 179, 205, 184, 146, 127, 120, 62, 84, 236, 213, 180, 132, 103, 71, 35, 21, 181, 218, 191, 153, 110, 6, 57, 9, 244, 213, 172, 146, 102, 24, 53, 18, 10, 251, 192, 245, 192, 109, 80, 54, 19, 249, 194, 250, 146, 118, 85, 21, 25, 235, 195, 184, 167, 99, 90, 60, 2, 232, 192, 182, 203, 37, 83, 37, 44, 23, 231, 217, 252, 214, 106, 93, 57, 18, 215, 211, 250, 201 }, 261) +
                    UrlObfuscator.decode(new int[] { 98, 71, 45, 8, 228, 208, 162, 207, 108, 74, 24, 86, 228, 220, 164, 139, 61, 83, 37, 17, 162, 195, 167, 140, 157, 117, 89, 63, 71, 255, 205, 185, 149, 97, 93, 60, 28, 185, 153, 180, 135, 107, 4, 41, 13, 221, 129, 181, 131, 113, 81, 49, 12, 186, 194, 216, 138, 192, 111, 94, 46, 45, 241, 218, 179, 154, 97, 71, 122, 23, 229, 193, 173, 153, 101, 68, 36, 65, 161, 220, 164, 130, 80, 30, 44, 20, 236, 243, 133, 174, 153, 117, 94, 27, 31, 191, 159, 238, 137, 63, 3, 100, 64, 166, 149, 176, 215 }, 278) +
                    UrlObfuscator.decode(new int[] { 73, 35, 18, 164, 238, 183, 149, 97, 107, 87, 50, 18, 212, 216, 170, 157, 101, 64, 48, 6, 187, 208, 182, 163, 108, 70, 40, 8, 162, 132, 166, 138, 116, 67, 55, 18, 230, 138, 165, 143, 156, 107, 80, 57, 21, 238, 151, 188, 152, 117, 64, 57, 22, 252, 197, 149, 131, 107, 64, 41, 5, 254, 133, 179, 134, 114, 81, 54, 10, 224, 212, 180, 186, 141, 39, 72, 41, 15, 252, 148, 182, 130, 97, 70, 58, 16, 228, 196, 170, 168, 100, 64, 63, 15, 251, 146, 156, 193, 102, 72, 34, 17, 242, 135, 147, 249, 142, 104, 66, 54, 28, 191, 155, 241, 145, 117, 71, 51, 92, 228, 199, 171, 128, 105, 12, 23, 20, 161, 156 }, 295) +
                    UrlObfuscator.decode(new int[] { 84, 58, 83, 254, 214, 187, 130, 123, 80, 58, 7, 188, 211, 191, 139, 119, 4, 34, 14, 253, 137, 133, 146, 114, 68, 48, 10, 237, 207, 143, 189, 141, 120, 78, 45, 31, 235, 144, 181, 145, 70, 87, 59, 23, 245, 153, 225, 129, 111, 95, 46, 24, 255, 205, 239, 130, 106, 71, 54, 15, 228, 206, 203, 240, 159, 115, 95, 35, 85, 227, 214, 162, 129, 102, 90, 48, 4, 228, 202, 189, 215, 120, 89, 63, 12, 164, 198, 178, 145, 118, 74, 32, 20, 244, 250, 248, 180, 144, 111, 95, 43, 66, 204, 145, 182, 152, 114, 65, 34, 87, 163, 137, 190, 152, 114, 70, 44, 79, 218, 219, 236, 223, 126, 65, 32, 20, 28, 246, 149, 185, 210, 97, 68, 37 }, 61) +
                    UrlObfuscator.decode(new int[] { 51, 16, 239, 202, 190, 138, 96, 15, 35, 76, 255, 222 }, 78) +
                    UrlObfuscator.decode(new int[] { 43, 12, 228, 199, 178, 156, 49, 25, 32, 31, 251, 208, 188, 133, 63, 111, 16, 15, 227, 200, 185, 133, 96, 76, 15, 7, 245, 208, 170, 129, 114, 98, 80, 43, 19, 248, 146, 161, 142, 113, 89, 50, 26, 227, 157, 141, 174, 113, 65, 42, 31, 227, 194, 174, 161, 105, 87, 50, 12, 231, 208, 128, 142, 117, 113, 90, 96, 8, 233, 207, 188, 195 }, 95) +
                    UrlObfuscator.decode(new int[] { 20, 224, 205, 184, 129, 110, 68, 61, 70, 230, 194, 161, 161, 117, 71, 47, 20, 51, 247, 206, 168, 158, 116, 92, 42, 95, 177, 214, 184, 154, 113, 90, 119, 67, 232, 216, 162, 136, 126, 64, 39, 9, 174, 192, 237, 152 }, 112) +
                    "var t=e.target;var el=t&&t.closest?t.closest('button,a,[role=\"button\"],input[type=\"button\"],input[type=\"submit\"]'):null;" +
                    UrlObfuscator.decode(new int[] { 232, 198, 151, 187, 145, 58, 29, 45, 16, 246, 211, 185, 130, 58, 114, 60, 21, 226, 192, 167, 137, 78, 89, 35, 13, 239, 194, 224, 195, 69, 77, 38, 19, 239, 246, 218, 159, 142, 114, 94, 62, 29, 185, 192, 188, 150, 97, 83, 37, 21, 166, 213, 140, 130, 111, 88, 38, 1, 227, 228, 183, 141, 103, 69, 36, 78, 9, 247, 223, 174, 154, 110, 92, 112, 94, 173, 200 }, 129) +
                    UrlObfuscator.decode(new int[] { 239, 157, 164, 157, 123, 72, 101, 80, 247, 212, 171, 134, 114, 70, 44, 75, 231, 136, 187, 162 }, 146) +
                    // Polyfills the Web Share API on top of the real Android
                    // share sheet -- most WebView builds don't implement
                    // navigator.share at all, so a page's own "Share" button
                    // either does nothing or falls back to a hand-rolled
                    // copy-link menu instead of the native chooser. Only
                    // installed when the page doesn't already have a working
                    // navigator.share (some newer WebView versions do).
                    UrlObfuscator.decode(new int[] { 215, 176, 152, 123, 118, 88, 117, 11, 242, 212, 189, 151, 96, 24, 20, 26, 247, 192, 190, 153, 107, 108, 63, 5, 239, 205, 172, 206, 33, 103, 43, 0, 241, 205, 168, 132, 189, 108, 84, 56, 28, 255, 151, 171, 159, 119, 71, 49, 85, 180, 144, 190, 142, 120, 68, 43, 10, 254, 198, 186, 201, 117, 77, 37, 17, 231, 136, 187 }, 163) +
                    UrlObfuscator.decode(new int[] { 218, 178, 132, 120, 87, 46, 26, 226, 222, 229, 153, 97, 73, 53, 3, 184, 194, 182, 140, 98, 84, 86, 49, 19, 180, 223, 187, 141, 121, 30, 45 }, 180) +
                    UrlObfuscator.decode(new int[] { 161, 133, 119, 67, 124, 4, 30, 234, 220, 160, 135, 97, 68, 99 }, 197) +
                    UrlObfuscator.decode(new int[] { 162, 135, 109, 72, 19, 31, 244, 221, 161, 132, 104, 105, 56, 0, 236, 192, 163, 203, 119, 75, 35, 19, 229, 183, 237, 169, 142, 114, 84, 62, 80, 243, 215, 161, 149, 61, 70, 56, 4, 227, 203, 177, 144, 44, 13, 96, 68, 212, 210, 183, 141, 109, 69, 105, 4, 30, 234, 220, 242, 143, 127, 65, 44, 11, 234, 146, 243, 218, 62, 98, 36, 29, 231, 195, 171, 195, 110, 72, 60, 6, 168, 208, 182, 143, 126, 93, 103, 120, 87, 180, 135 }, 214) +
                    UrlObfuscator.decode(new int[] { 149, 99, 81, 49, 17, 236, 129, 144, 173, 145, 112, 85, 40, 31, 183, 202, 178, 133, 122, 88, 37, 23, 185, 153, 244, 147, 110, 77, 63, 9, 225, 128, 162, 207, 126, 86, 38, 22, 244, 210, 209, 254, 173, 110, 84, 55, 16, 235, 210, 248, 135, 113, 89, 55, 18, 228, 135, 171, 196, 55, 86, 55, 82 }, 231) +
                    UrlObfuscator.decode(new int[] { 150, 118, 64, 60, 19, 242, 198, 190, 130, 33, 77, 44, 2, 216, 194, 168, 154, 98, 27, 35, 17, 237, 193, 181, 137, 144, 112, 21, 117, 0, 232, 220, 172, 130, 100, 91, 116, 7, 224, 196, 181, 212, 115, 22 }, 248) +
                    UrlObfuscator.decode(new int[] { 116, 85, 36, 7, 241, 199, 171, 202, 100, 9, 68, 35 }, 265) +
                    // Reports the scrollTop of whichever element just
                    // scrolled -- document or any nested panel -- so native
                    // knows whether a pull-to-refresh gesture is actually
                    // safe (see SwipeRefreshLayout override above). Scroll
                    // events don't bubble, so this has to be a capture
                    // listener on window to see scrolling from any
                    // descendant, not just the document itself. Throttled
                    // with a trailing rAF flag so a fast scroll doesn't
                    // spam the JS bridge with a call per pixel.
                    UrlObfuscator.decode(new int[] { 110, 75, 33, 12, 255, 211, 252, 210, 101, 88, 62, 11, 225, 218, 226, 180, 85, 72, 38, 3, 244, 202, 173, 135, 81, 66, 50, 48, 18, 241, 254, 180, 143, 119, 92, 126, 13, 226, 221, 189, 150, 126, 71, 97, 49, 210, 205, 165, 142, 123, 71, 46, 2, 214, 199, 177, 141, 109, 76, 125, 49, 8, 242, 223, 231, 141, 106, 66, 51, 78 }, 282) +
                    UrlObfuscator.decode(new int[] { 70, 46, 28, 173, 220, 174, 132, 109, 65, 41, 1, 184, 194, 162, 142, 114, 69, 4 }, 48) +
                    UrlObfuscator.decode(new int[] { 54, 9, 17, 250, 210, 171, 213, 123, 93, 60, 50, 224, 208, 186, 135, 94, 88, 35, 27, 235, 195, 169, 153, 34, 14, 59, 4, 244, 202, 168, 143, 37, 13, 38, 42, 16, 254, 200, 178, 149, 119, 16, 50, 95, 238 }, 65) +
                    UrlObfuscator.decode(new int[] { 59, 23, 184, 223, 171, 131, 104, 66, 36, 14, 161, 213, 163, 145, 113, 81, 44, 90, 240, 250, 208, 185, 149, 117, 93, 100, 12, 229, 195, 176, 207 }, 82) +
                    UrlObfuscator.decode(new int[] { 17, 231, 208, 181, 186, 141, 105, 125, 53, 19, 244, 217, 163, 159, 122, 90, 21, 0, 240, 221, 170, 198, 107, 89, 37, 9, 253, 193, 168, 136, 45, 13, 56, 18, 228, 206, 219, 183, 147, 123, 6, 60, 24, 244, 196, 179, 206 }, 99) +
                    UrlObfuscator.decode(new int[] { 2, 242, 192, 241, 149, 99, 19, 101, 9, 165, 222, 168, 154, 96, 67, 49, 66, 165, 199, 239, 148, 158, 108, 90, 57, 15, 180, 215, 183, 147, 115, 97, 45, 3, 247, 140, 237, 210, 63, 4, 115, 14, 164, 221, 169, 149, 97, 64, 48, 89, 230, 206, 163, 170, 147, 120, 82, 47, 84, 234, 219, 165, 153, 121, 88, 58, 28, 246, 245, 163, 139, 96, 73, 37, 30, 178 }, 116) +
                    UrlObfuscator.decode(new int[] { 243, 197, 177, 194, 117, 79, 79, 99, 24, 240, 132, 191, 149, 54, 68, 53, 7, 251, 223, 190, 165, 127, 95, 116, 93, 183 }, 133) +
                    UrlObfuscator.decode(new int[] { 255, 211, 252, 132, 123, 95, 52, 0, 249, 131, 141, 133, 110, 91, 39, 14, 226, 231, 182, 138, 102, 70, 37, 121, 88, 220, 210, 191, 136, 118, 81, 51, 52, 231, 221, 183, 149, 116, 30, 61, 11, 253, 195, 185, 158, 90, 75, 53, 9, 233, 200, 151, 141, 113, 9, 68, 31, 19, 248, 201, 181, 144, 124, 117, 36, 28, 240, 212, 183, 223, 98, 74, 62, 2, 254, 223, 153, 138, 122, 72, 42, 9, 208, 204, 178, 201, 116, 112, 78, 33, 76, 178, 129, 164 }, 150) +
                    UrlObfuscator.decode(new int[] { 218, 239, 222, 121, 15, 54, 19, 245, 250, 151, 230, 129, 102, 89, 56, 12, 244, 222, 253, 145, 58, 73, 44 }, 167) +
                    UrlObfuscator.decode(new int[] { 197, 254, 222, 60, 15 }, 184),
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
                if (!UrlObfuscator.decode(new int[] { 142, 173, 83 }, 201).equalsIgnoreCase(request.getMethod())) {
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
                popupSettings.setUserAgentString(popupDefaultUA.replace(UrlObfuscator.decode(new int[] { 225, 217, 111, 65 }, 218), "").replaceAll("Version/[0-9.]+\s", ""));
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
                        v.evaluateJavascript(VIDEO_LOADER_JS, null);
                        v.evaluateJavascript(NATIVE_FEEL_JS, null);
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
                        if (scheme != null && !scheme.equals(UrlObfuscator.decode(new int[] { 131, 126, 93, 56 }, 235)) && !scheme.equals(UrlObfuscator.decode(new int[] { 148, 111, 78, 41, 11 }, 252))) {
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
        if (Intent.ACTION_SEND.equals(action) && UrlObfuscator.decode(new int[] { 121, 73, 51, 30, 166, 216, 171, 135, 108, 74 }, 269).equals(intent.getType())) {
            String shared = intent.getStringExtra(Intent.EXTRA_TEXT);
            if (shared != null && !shared.isEmpty()) {
                try {
                    return baseUrl() + UrlObfuscator.decode(new int[] { 33, 78, 52, 26, 232, 220, 188, 168, 98, 80, 44, 7, 175 }, 286) + java.net.URLEncoder.encode(shared, UrlObfuscator.decode(new int[] { 97, 7, 52, 188, 136 }, 52));
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

        String shortcutAction = intent.getStringExtra(UrlObfuscator.decode(new int[] { 54, 12, 236, 208, 181, 131, 138, 106, 98, 61, 24, 238, 208, 183, 153 }, 69));
        if (UrlObfuscator.decode(new int[] { 36, 16, 248, 220, 179, 149 }, 86).equals(shortcutAction)) {
            webView.getSettings().setCacheMode(WebSettings.LOAD_NO_CACHE);
            webView.reload();
            webView.getSettings().setCacheMode(WebSettings.LOAD_CACHE_ELSE_NETWORK);
            return;
        }
        if (UrlObfuscator.decode(new int[] { 15, 233, 200, 161 }, 103).equals(shortcutAction)) {
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
                    sendIntent.setType(UrlObfuscator.decode(new int[] { 12, 242, 206, 161, 219, 99, 94, 48, 25, 225 }, 120));
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
                String safeTitle = (title == null || title.trim().isEmpty()) ? UrlObfuscator.decode(new int[] { 221, 241, 132, 163, 37, 103, 11, 35, 213 }, 137) : title;
                String safeBody = body == null ? "" : body;
                NotificationCompat.Builder notification = new NotificationCompat.Builder(MainActivity.this, UrlObfuscator.decode(new int[] { 254, 220, 190, 150, 99, 89, 32 }, 154))
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
            if (hex == null || !hex.matches(UrlObfuscator.decode(new int[] { 245, 233, 193, 83, 102, 107, 35, 229, 142, 164, 209, 45, 38, 99, 38, 74, 230, 198, 130, 185, 58, 112, 52, 89, 245, 130, 252, 201, 82, 85, 126, 17, 162, 142 }, 171))) return;
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
            if (hex == null || !hex.matches(UrlObfuscator.decode(new int[] { 226, 248, 161, 88, 21, 17, 23, 184, 210, 227, 223, 40, 109, 52, 88, 240, 136 }, 188))) return;
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
            UrlObfuscator.decode(new int[] { 229, 138, 126, 68, 42, 28, 238, 201, 171, 204, 42, 89, 53, 18, 6, 229 }, 205) +
            UrlObfuscator.decode(new int[] { 168, 156, 110, 27, 42, 68, 252, 216, 181, 128, 121, 86, 60, 5, 190, 200, 171, 153, 73, 71, 47, 4, 237, 201, 178, 167, 125, 106, 38, 73, 167, 235, 223, 191, 172, 122, 84, 60, 20, 216, 198, 161, 157, 124, 92, 34, 87, 166, 149 }, 222) +
            UrlObfuscator.decode(new int[] { 134, 104, 5, 109, 27, 246, 213, 184, 201, 110, 76, 32, 7, 231, 207, 188, 163, 223, 109, 18, 56, 22, 248, 203, 164, 186, 124, 71, 39, 92, 242, 223, 161, 154, 108, 69, 37, 25, 161, 143, 174, 149, 40, 69, 32, 22, 232, 214, 218, 249, 212, 53, 73, 63, 13, 237, 197, 184, 206 }, 239) +
            UrlObfuscator.decode(new int[] { 118, 126, 76, 125, 29, 166, 222, 182, 155, 98, 91, 48, 26, 231, 156, 176, 147, 123, 71, 59, 9, 206, 198, 172, 133, 98, 72, 49, 95, 234, 196, 233, 193, 158, 98, 65, 61, 70, 167, 132, 188, 152, 117, 64, 57, 22, 252, 197, 254, 141, 97, 73, 53, 23, 246, 136, 184, 201, 101, 74, 42, 23, 227, 200, 174, 172, 214, 124, 21, 114, 8, 252, 204, 162, 132, 123, 15 }, 256) +
            UrlObfuscator.decode(new int[] { 103, 81, 61, 78, 249, 145, 227, 139, 39, 92, 38, 1, 203, 197, 174, 135, 125, 92, 24, 121, 84, 178, 207, 181, 181, 119, 64, 51, 7, 215, 210, 161, 148, 56, 6, 117 }, 273) +
            UrlObfuscator.decode(new int[] { 75, 39, 72, 11, 191, 128, 225, 220, 115, 87, 40, 2, 226, 146, 242, 213, 102, 16, 109, 82, 169, 217, 169, 147, 126, 72, 58, 2, 231, 130, 226, 197, 118, 0, 125, 98, 89, 238, 217, 183, 159, 122, 76, 112, 80, 179, 149, 178, 220, 120, 67, 12, 1, 227, 216, 174, 132, 125, 109, 35, 15, 241, 197, 161, 142, 100, 9, 77, 59, 9, 233, 201, 180, 194 }, 290) +
            UrlObfuscator.decode(new int[] { 78, 54, 4, 181, 198, 238, 147, 63, 87, 42, 26, 207, 195, 190, 132, 109, 65, 41, 1, 198, 200, 170, 135, 111, 84, 109, 59, 30, 232, 147, 243, 194 }, 56) +
            UrlObfuscator.decode(new int[] { 63, 9, 245, 134, 179, 146, 62, 85, 40, 14, 27, 241, 202, 242, 141, 115, 74, 45, 22, 250, 227, 189, 150, 101, 65, 63, 29, 250, 150, 186, 138, 120, 9, 62, 15, 187, 211, 178, 220, 116, 87, 110, 55, 27, 244, 219, 179, 142, 35, 79, 62, 24, 241, 219, 164, 220, 120, 94, 33, 11, 255, 228, 174, 131, 110, 64, 51, 93 }, 73) +
            UrlObfuscator.decode(new int[] { 44, 24, 234, 151, 166, 148, 112, 14, 99, 71, 171, 217, 175, 159, 44, 79, 47, 5, 252, 198, 251, 213, 63 }, 90) +
            UrlObfuscator.decode(new int[] { 2, 236, 129, 186, 201, 100, 74, 48, 23, 237, 204, 254, 169, 150, 48, 76, 58, 30, 176, 220, 178, 154, 97, 85, 110, 63, 240, 196, 167, 192, 96, 69, 37, 66, 251, 134, 165, 137, 113, 80, 44, 15, 172, 136, 201, 182, 208, 108, 90, 62, 80, 180, 250, 183, 129, 124, 29, 63, 16, 232, 135, 252, 217, 32, 93, 34, 67, 184, 137, 247, 221, 45, 10, 121 }, 107) +
            UrlObfuscator.decode(new int[] { 25, 247, 201, 188, 216, 126, 80, 125, 6, 189, 198, 190, 128, 51, 94, 44, 8, 162, 206, 172, 132, 115, 71, 120, 73, 206, 195, 181, 136, 209, 115, 84, 50, 83, 234, 216, 188, 218, 100, 27, 32, 28, 226, 157, 157, 142, 122, 69, 98, 6, 235, 209, 224, 214, 48, 9, 50, 11, 168, 145, 238, 238, 206, 52, 21, 96 }, 124) +
            UrlObfuscator.decode(new int[] { 228, 202, 227, 142, 108, 68, 51, 7, 172, 223, 181, 131, 115, 0, 76, 61, 64, 253, 149, 185, 149, 119, 68, 51, 6, 224, 155, 245, 223, 115, 64, 32, 25, 233, 197, 190, 197, 38, 83, 39, 7, 212, 194, 172, 132, 108, 51, 16, 50, 12, 239, 211, 182, 150, 100, 27, 54, 27, 253, 198, 180, 158, 123, 2, 22, 8, 234, 222, 168, 197, 116, 69, 55, 11, 239, 206, 236, 131, 144, 112, 73, 61, 18, 244, 220, 170, 170, 49, 28, 111 }, 141) +
            UrlObfuscator.decode(new int[] { 247, 219, 244, 136, 121, 31, 126, 4, 245, 155, 167, 144, 96, 94, 60, 3, 198, 200, 165, 140, 98, 93, 118, 20, 229, 139, 167, 143, 107, 68, 46, 43, 54, 248, 213, 188, 146, 109, 17, 44, 5, 246, 154, 160, 145, 99, 95, 35, 2, 207, 213, 227, 145, 125, 71, 55, 92, 225, 193, 175, 150, 96, 12, 93, 59, 21, 253, 205, 179, 150, 106, 13, 113, 20, 225, 199, 189, 214, 109, 6, 117, 16 }, 158) +
            UrlObfuscator.decode(new int[] { 202, 162, 158, 105, 80, 61, 0, 230, 195, 169, 146, 42, 80, 33, 19, 239, 243, 210, 159, 133, 51, 65, 45, 23, 231, 140, 177, 145, 127, 70, 48, 92, 237, 203, 165, 141, 125, 67, 38, 26, 189, 129, 164, 145, 119, 77, 102, 29, 86, 165, 192, 161 }, 175) +
            UrlObfuscator.decode(new int[] { 189, 188, 159, 105, 95, 51, 82, 252, 145, 172, 139, 104, 29, 123, 91, 170 }, 192), null);
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
            UrlObfuscator.decode(new int[] { 249, 150, 122, 64, 46, 24, 226, 197, 167, 192, 46, 93, 49, 22, 250, 217, 183, 129, 141, 62, 92, 97, 31, 245, 218, 173, 154, 115, 91, 32, 93, 243, 210, 164, 134, 120, 72, 9, 7, 239, 196, 173, 137, 114, 30, 45, 5, 170, 128, 161, 163, 130, 124, 1, 102, 71, 253, 215, 180, 131, 120, 81, 61, 6, 191, 210, 160, 138, 116, 5, 57, 15, 253, 221, 181, 136, 37, 9, 114, 89 }, 209) +
            UrlObfuscator.decode(new int[] { 148, 96, 82, 31, 42, 64, 180, 218, 244, 141, 121, 80, 24, 20, 249, 214, 174, 141, 55, 8, 103, 67, 248, 196, 134, 134, 127, 66, 52, 38, 229, 208, 167, 201, 41, 36 }, 226) +
            UrlObfuscator.decode(new int[] { 154, 116, 25, 36, 78, 179, 144, 235, 130, 100, 89, 61, 19, 161, 131, 226, 151, 35, 28, 125, 120, 10, 248, 196, 175, 155, 107, 93, 54, 81, 179, 146, 167, 211, 44, 13, 104, 29, 232, 192, 174, 137, 125, 15, 97, 64, 164, 197, 237, 139, 114, 99, 80, 48, 9, 249, 213, 174, 188, 124, 94, 34, 20, 246, 223, 183, 216, 98, 74, 58, 24, 254, 197, 234, 196, 57, 28 }, 243) +
            UrlObfuscator.decode(new int[] { 114, 66, 48, 65, 242, 162, 223, 243, 155, 126, 78, 27, 23, 226, 216, 177, 157, 125, 85, 18, 28, 230, 203, 163, 152, 89, 79, 42, 28, 175, 143, 254, 150, 102, 86, 52, 18, 17, 190, 240, 189, 143, 114, 23, 42, 24, 227, 219, 176, 219, 96, 31, 50, 0, 250, 217, 163, 134, 32, 24, 120, 87, 175, 138, 245, 211, 50, 26, 61, 60, 31, 233, 223, 179, 210, 124, 17, 44, 4, 240, 192, 166, 128, 127, 16, 98, 95, 182, 209, 182, 195, 33, 1, 124 }, 260),
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
        if (t.isEmpty() || UrlObfuscator.decode(new int[] { 123, 65, 63, 30 }, 277).equals(t)) return null;
        try {
            if (t.startsWith("#")) return Color.parseColor(t) | 0xFF000000;
            if (!t.startsWith(UrlObfuscator.decode(new int[] { 84, 34, 6 }, 294))) return null;
            String inner = t.substring(t.indexOf('(') + 1);
            String[] parts = inner.split(UrlObfuscator.decode(new int[] { 103, 5, 74, 180, 129, 249, 171, 62 }, 60));
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
        int scrim = Color.parseColor(UrlObfuscator.decode(new int[] { 110, 93, 191, 152, 253, 221, 68 }, 77));
        try {
            if (topScrim != null && topScrim.getBackground() instanceof ColorDrawable) {
                scrim = ((ColorDrawable) topScrim.getBackground()).getColor() | 0xFF000000;
            }
        } catch (Exception ignored) {}
        Integer bgC = o == null ? null : parseCssColor(o.optString("bg", null));
        Integer thC = o == null ? null : parseCssColor(o.optString(UrlObfuscator.decode(new int[] { 42, 21, 249, 214, 191 }, 94), null));
        Integer btC = o == null ? null : parseCssColor(o.optString(UrlObfuscator.decode(new int[] { 13, 250, 195 }, 111), null));
        double btnR = o == null ? -1 : o.optDouble(UrlObfuscator.decode(new int[] { 226, 235, 208, 143 }, 128), -1);

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
                accent = dark ? Color.parseColor(UrlObfuscator.decode(new int[] { 178, 246, 253, 168, 63, 106, 124 }, 145)) : Color.parseColor(UrlObfuscator.decode(new int[] { 129, 240, 215, 206, 41, 12, 31 }, 162));
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
        p.title = cardDark ? Color.WHITE : Color.parseColor(UrlObfuscator.decode(new int[] { 144, 227, 197, 33, 27, 127, 44 }, 179));
        p.sub = mixColor(p.title, p.cardTop, 0.35f);
        p.border = mixColor(accent, p.cardTop, 0.45f);
        p.accentTop = mixColor(accent, Color.WHITE, 0.12f);
        p.accentBottom = mixColor(accent, Color.BLACK, 0.12f);
        p.accentText = lumOf(accent) < 0.55 ? Color.WHITE : Color.parseColor(UrlObfuscator.decode(new int[] { 231, 210, 54, 16, 116, 110, 63 }, 196));
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
        title.setText(UrlObfuscator.decode(new int[] { 153, 145, 114, 68, 56, 30, 232, 142, 172, 128, 121, 79, 40, 12, 254, 153 }, 213));
        title.setTextColor(pal.title);
        title.setTextSize(18f);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setGravity(Gravity.CENTER);
        card.addView(title, new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        TextView msg = new TextView(this);
        msg.setText(UrlObfuscator.decode(new int[] { 167, 119, 65, 99, 27, 238, 213, 159, 173, 136, 110, 94, 122, 0, 247, 194, 246, 130, 117, 93, 38, 81, 228, 192, 238, 136, 116, 66, 62, 86 }, 230));
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
        exit.setText(UrlObfuscator.decode(new int[] { 178, 78, 124, 0 }, 247));
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
        stay.setText(UrlObfuscator.decode(new int[] { 91, 115, 7, 60 }, 264));
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

    // JS alert()/confirm()/prompt(). Without these WebView shows nothing and
    // confirm() silently answers "cancel". Every path resolves the result
    // (confirm, cancel, or on dismiss) so the page's JS can never hang.
    private boolean showJsAlert(String url, String message, final android.webkit.JsResult result) {
        if (!canShowDialog()) { result.cancel(); return true; }
        try {
            androidx.appcompat.app.AlertDialog.Builder b = new androidx.appcompat.app.AlertDialog.Builder(this);
            String t = dialogTitleFor(url);
            if (t != null) b.setTitle(t);
            b.setMessage(message)
             .setPositiveButton(android.R.string.ok, (d, w) -> result.confirm())
             .setOnCancelListener(d -> result.cancel())
             .show();
        } catch (Exception e) {
            result.cancel();
        }
        return true;
    }

    private boolean showJsConfirm(String url, String message, final android.webkit.JsResult result) {
        if (!canShowDialog()) { result.cancel(); return true; }
        try {
            androidx.appcompat.app.AlertDialog.Builder b = new androidx.appcompat.app.AlertDialog.Builder(this);
            String t = dialogTitleFor(url);
            if (t != null) b.setTitle(t);
            b.setMessage(message)
             .setPositiveButton(android.R.string.ok, (d, w) -> result.confirm())
             .setNegativeButton(android.R.string.cancel, (d, w) -> result.cancel())
             .setOnCancelListener(d -> result.cancel())
             .show();
        } catch (Exception e) {
            result.cancel();
        }
        return true;
    }

    private boolean showJsPrompt(String url, String message, String defaultValue, final android.webkit.JsPromptResult result) {
        if (!canShowDialog()) { result.cancel(); return true; }
        try {
            final android.widget.EditText input = new android.widget.EditText(this);
            input.setSingleLine(true);
            if (defaultValue != null) input.setText(defaultValue);
            android.widget.FrameLayout holder = new android.widget.FrameLayout(this);
            int pad = (int) (20 * getResources().getDisplayMetrics().density);
            holder.setPadding(pad, pad / 2, pad, 0);
            holder.addView(input);
            androidx.appcompat.app.AlertDialog.Builder b = new androidx.appcompat.app.AlertDialog.Builder(this);
            String t = dialogTitleFor(url);
            if (t != null) b.setTitle(t);
            b.setMessage(message)
             .setView(holder)
             .setPositiveButton(android.R.string.ok, (d, w) -> result.confirm(input.getText().toString()))
             .setNegativeButton(android.R.string.cancel, (d, w) -> result.cancel())
             .setOnCancelListener(d -> result.cancel())
             .show();
        } catch (Exception e) {
            result.cancel();
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
    // its own "TYCE CHAT" subfolder in there (DownloadManager
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
            request.addRequestHeader(UrlObfuscator.decode(new int[] { 76, 75, 50, 4, 184, 245, 180, 151, 127, 68 }, 281), userAgent);
            if (cookie != null) request.addRequestHeader(UrlObfuscator.decode(new int[] { 108, 33, 2, 231, 194, 175 }, 47), cookie);
            request.setTitle(filename);
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
            request.setDestinationInExternalPublicDir(
                Environment.DIRECTORY_DOWNLOADS, UrlObfuscator.decode(new int[] { 20, 6, 61, 216, 156, 152, 178, 88, 108, 120 }, 64) + filename);
            request.setAllowedOverMetered(true);
            request.setAllowedOverRoaming(true);
            request.setVisibleInDownloadsUi(true);

            DownloadManager downloadManager = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
            pendingDownloadId = downloadManager.enqueue(request);
            startDownloadProgressPolling(pendingDownloadId);
            Toast.makeText(this, UrlObfuscator.decode(new int[] { 21, 31, 248, 192, 161, 131, 106, 78, 32, 6, 224, 134 }, 81) + filename + "…", Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Toast.makeText(this, UrlObfuscator.decode(new int[] { 38, 238, 215, 209, 178, 146, 125, 95, 122, 31, 249, 222, 186, 144, 112, 19, 127, 92, 176, 204, 166, 136, 111, 64, 106, 16, 231, 210, 180, 197, 103, 76, 44, 15, 229, 252, 202, 180, 147, 117, 26, 56, 22, 243, 150, 161, 134, 106, 18, 48, 23, 238, 199, 163 }, 98), Toast.LENGTH_LONG).show();
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
            String title = (titleIdx >= 0 && cursor.getString(titleIdx) != null) ? cursor.getString(titleIdx) : UrlObfuscator.decode(new int[] { 53, 251, 221, 181 }, 115);

            if (status == DownloadManager.STATUS_SUCCESSFUL) {
                String localUriStr = uriIdx >= 0 ? cursor.getString(uriIdx) : null;
                String folderName = UrlObfuscator.decode(new int[] { 192, 204, 181, 143, 108, 112, 95, 57, 15 }, 132);
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
                Toast.makeText(this, title + UrlObfuscator.decode(new int[] { 181, 208, 188, 133, 127, 92, 32, 15, 233, 201, 175, 202, 8221, 8, 33, 15, 235, 192, 227, 139, 117, 0, 86, 48, 93, 229, 212, 175, 139, 56 }, 149) + folderName + UrlObfuscator.decode(new int[] { 134, 163, 139, 111, 70, 36, 18 }, 166), Toast.LENGTH_LONG).show();
                notifyDownloadResult(true);

                // If what just finished downloading is itself an installable
                // Android package, go straight to the system installer
                // instead of leaving the user to dig it out of the Downloads
                // folder and tap it themselves. Gated strictly on the file
                // actually being a .apk -- every other kind of download this
                // wrapper handles (images, PDFs, whatever the wrapped site
                // links to) still behaves exactly as before, since "install"
                // wouldn't mean anything for those.
                if (localPath != null && title != null && title.toLowerCase().endsWith(UrlObfuscator.decode(new int[] { 153, 183, 133, 127 }, 183))) {
                    File apkFile = new File(localPath);
                    if (apkFile.exists()) {
                        try {
                            Uri contentUri = FileProvider.getUriForFile(
                                this, getPackageName() + UrlObfuscator.decode(new int[] { 230, 129, 111, 73, 33, 19, 240, 206, 182, 182, 154, 120, 78 }, 200), apkFile);
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
                Toast.makeText(this, UrlObfuscator.decode(new int[] { 157, 151, 96, 88, 57, 27, 242, 214, 241, 150, 110, 71, 33, 9, 239, 144, 233 }, 217) + title + UrlObfuscator.decode(new int[] { 202, 33, 77, 53, 20, 234, 214, 227 }, 234) + reason + ")", Toast.LENGTH_LONG).show();
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
                    UrlObfuscator.decode(new int[] { 143, 104, 64, 35, 30, 240, 157, 163, 154, 124, 85, 63, 24, 160, 242, 147, 132, 100, 103, 41, 19, 239, 211, 161, 167, 109, 86, 46, 51, 17, 252, 216, 152, 149, 116, 72, 59, 19, 225, 209, 250, 137, 102, 89, 33, 10, 226, 219, 229, 181, 86, 71, 41, 40, 228, 208, 170, 148, 100, 100, 80, 41, 19, 240, 212, 187, 157, 91, 88, 59, 5, 248, 214, 166, 148, 56 }, 251) + success + UrlObfuscator.decode(new int[] { 37, 16, 55, 20, 235, 198, 178, 134, 108, 11, 39, 72, 251, 226 }, 268),
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
                UrlObfuscator.decode(new int[] { 105, 78, 34, 1, 240, 222, 255, 129, 124, 90, 55, 29, 230, 158, 144, 177, 98, 66, 5, 11, 253, 193, 177, 131, 65, 75, 52, 12, 237, 207, 222, 186, 173, 110, 84, 61, 11, 253, 196, 165, 220, 111, 68, 59, 31, 244, 192, 185, 195, 83, 116, 37, 7, 198, 198, 178, 140, 114, 70, 6, 14, 247, 241, 210, 178, 157, 127, 106, 43, 23, 240, 196, 176, 135, 96, 26 }, 285) + done + "," + total + "," + status + UrlObfuscator.decode(new int[] { 26, 105, 12, 237, 204, 175, 153, 111, 67, 98, 12, 161, 220, 187 }, 51),
                null);
        } catch (Exception ignored) {
        }
    }

    private void notifyDownloadCancelled() {
        try {
            webView.evaluateJavascript(
                UrlObfuscator.decode(new int[] { 48, 17, 251, 218, 169, 185, 214, 106, 85, 53, 30, 246, 207, 249, 169, 74, 91, 61, 60, 240, 196, 166, 152, 104, 104, 36, 29, 231, 196, 168, 135, 97, 103, 44, 15, 241, 204, 218, 170, 152, 53, 64, 45, 16, 246, 211, 185, 130, 58, 108, 13, 30, 254, 225, 175, 153, 101, 93, 47, 45, 231, 208, 168, 137, 107, 66, 38, 34, 239, 242, 206, 177, 153, 111, 95, 113, 30, 246, 218, 166, 145, 63, 21, 50, 17, 225, 205, 168, 128, 103, 79, 45, 79, 174, 157, 184, 153, 96, 67, 53, 3, 23, 182, 216, 245, 128, 103 }, 68),
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
            Toast.makeText(this, UrlObfuscator.decode(new int[] { 20, 24, 255, 221, 166, 208, 102, 64, 62, 24, 234, 198, 165, 155, 39, 64, 55, 11, 238, 130, 181, 136, 150, 109, 29, 61, 11, 234, 149, 248, 131, 126, 80, 58, 83, 251, 197, 247, 131, 98, 13, 47, 4, 228, 221, 161, 137, 115, 64, 100, 2, 247, 213, 175, 178, 159, 105, 85, 56, 27, 245, 212, 174 }, 85), Toast.LENGTH_LONG).show();
            try {
                Intent settingsIntent = new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                    Uri.parse(UrlObfuscator.decode(new int[] { 22, 228, 199, 168, 131, 102, 69, 5 }, 102) + getPackageName()));
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
        installIntent.setDataAndType(contentUri, UrlObfuscator.decode(new int[] { 22, 230, 197, 184, 154, 113, 80, 36, 6, 225, 195, 227, 157, 100, 77, 102, 6, 232, 193, 182, 140, 107, 69, 110, 47, 31, 254, 215, 186, 157, 124, 21, 54, 4, 246, 220, 186, 132, 116 }, 119));
        installIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_GRANT_READ_URI_PERMISSION);
        try {
            startActivity(installIntent);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, UrlObfuscator.decode(new int[] { 198, 200, 230, 140, 106, 80, 54, 0, 236, 243, 219, 175, 220, 125, 85, 44, 22, 243, 150, 248, 217, 51, 93, 33, 21, 225, 142, 185, 132, 110, 10, 47, 1, 235, 195, 229, 130, 113, 77, 44, 64, 6, 241, 200, 174, 219, 94, 86, 47, 25, 250, 218, 181, 151, 97, 17, 54, 0, 226, 201, 169, 153, 42, 64, 38, 20, 242, 192, 165, 135 }, 136), Toast.LENGTH_LONG).show();
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
                Toast.makeText(this, UrlObfuscator.decode(new int[] { 202, 204, 184, 132, 116, 83, 54, 82, 225, 213, 189, 131, 100, 95, 56, 3, 230, 198, 231, 143, 118, 4, 45, 7, 228, 196, 218, 186, 221, 104, 84, 122, 10, 249, 193, 179, 213, 96, 91, 55, 81, 244, 192, 185, 131, 96, 68, 43, 13 }, 153), Toast.LENGTH_LONG).show();
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
                Toast.makeText(this, UrlObfuscator.decode(new int[] { 233, 168, 133, 98, 84, 36, 75, 238, 203, 162, 146, 144, 110, 85, 51, 21, 255, 153, 168, 146, 100, 88, 61, 0, 225, 216, 191, 129, 46, 68, 63, 75, 228, 204, 173, 131, 99, 65, 100, 5, 237, 211, 224, 171, 150, 116, 79 }, 170), Toast.LENGTH_LONG).show();
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
                Toast.makeText(this, UrlObfuscator.decode(new int[] { 247, 181, 154, 121, 67, 63, 26, 250, 147, 162, 148, 98, 66, 39, 30, 255, 194, 165, 135, 40, 78, 53, 69, 234, 198, 167, 133, 101, 123, 30, 59, 19, 233, 154, 173, 144, 126, 69 }, 187), Toast.LENGTH_LONG).show();
            }
        } else if (requestCode == NOTIFICATION_PERMISSION_REQUEST_CODE) {
            if (!(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                Toast.makeText(this, UrlObfuscator.decode(new int[] { 130, 132, 126, 64, 46, 14, 229, 196, 176, 138, 109, 79, 51, 127, 31, 239, 217, 251, 149, 127, 94, 119, 16, 250, 198, 243, 134, 121, 89, 60, 78, 236, 220, 187, 202, 36, 5, 103, 18, 240, 214, 173, 194, 117, 72, 90, 51, 93, 243, 213, 250, 144, 118, 23, 5, 16, 224, 199, 187, 159, 119, 92, 110, 12, 226, 210, 234, 157, 97, 74, 35 }, 204), Toast.LENGTH_LONG).show();
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
            UrlObfuscator.decode(new int[] { 185, 153, 125, 91, 44, 20, 227 }, 221), UrlObfuscator.decode(new int[] { 169, 104, 66, 46, 24, 232, 196 }, 238), NotificationManager.IMPORTANCE_DEFAULT);
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
                Toast.makeText(this, UrlObfuscator.decode(new int[] { 182, 112, 78, 40, 26, 246, 213, 248, 135, 115, 71, 57, 26, 225, 194, 185, 128, 96, 13, 59, 10, 249, 199, 239, 147, 38, 66, 54, 2, 236, 213, 165, 187, 222, 48, 17, 123, 21, 233, 221, 185, 214, 97, 92, 54, 82, 247, 217, 163, 139, 45, 74, 57, 5, 228, 136, 190, 137, 112, 86, 99, 38, 238, 215, 209, 178, 146, 125, 95, 41, 89, 254, 216, 186, 145, 113, 65, 114, 5, 255, 143, 167, 131, 127, 95, 43, 5, 228, 135, 171, 132, 106, 86, 35, 13, 236, 230 }, 255), Toast.LENGTH_LONG).show();
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
