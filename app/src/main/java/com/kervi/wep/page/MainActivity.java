package com.kervi.wep.page;

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
    // Records the name a page gives an <a download="..."> link (also for
    // anchors clicked from script) so blob:/data: downloads keep it.
    private static final String DOWNLOAD_NAME_JS =
        UrlObfuscator.decode(new int[] { 70, 235, 217, 165, 137, 125, 65, 40, 8, 173, 141, 184, 150, 115, 89, 68, 55, 27, 180, 204, 179, 151, 124, 88, 33, 91, 203, 236, 182, 157, 94, 78, 35, 8, 206, 196, 191, 135, 108, 14, 52, 0, 240, 214, 176, 143, 59, 104, 87, 51, 24, 244, 205, 247, 167, 72, 82, 57, 58, 242, 223, 180, 178, 96, 91, 35, 8, 182, 222, 187, 157, 98, 29 }, 110) +
        UrlObfuscator.decode(new int[] { 25, 235, 211, 191, 143, 115, 86, 54, 87, 228, 208, 183, 219, 115, 24, 43, 27, 252, 212, 183, 130, 108, 1, 41, 65, 160, 196, 234, 139, 99, 82, 1, 43, 10, 239, 213, 185, 143, 109, 93, 113, 80, 244, 154, 187, 147, 98, 113, 59, 26, 255, 197, 169, 159, 125, 77, 111, 65, 225, 203, 180, 140, 109, 79, 94, 58, 90, 181, 157, 252, 152, 54, 95, 36, 16, 242, 149, 244, 134, 121, 65, 42, 2, 251, 133, 139, 135, 108, 85, 41, 12, 224, 225, 176, 136, 100, 120, 91, 123, 90, 218, 212, 189, 138, 120, 95, 49, 54, 225, 219, 181, 151, 106, 0, 35, 3, 255, 207, 141, 135, 112, 72, 41, 11, 226, 198, 143, 129, 146, 123, 20, 39, 58, 244, 221, 170, 152, 127, 81, 22, 1, 251, 213, 183, 138, 32, 67, 35, 31, 239, 237, 167, 144, 104, 73, 43, 2, 230, 239, 161, 178, 155, 53, 93, 117, 18, 235, 221, 177, 218, 116, 26, 52, 23, 229, 241, 187, 154, 127, 69, 41, 31, 253, 205, 239, 193, 97, 75, 52, 12, 237, 207, 222, 186, 218, 53, 71, 38, 94, 191, 158, 237, 136, 105, 80, 51, 5, 243, 199, 230, 136, 37, 80, 55, 20 }, 127) +
        UrlObfuscator.decode(new int[] { 244, 192, 173, 152, 97, 78, 36, 29, 166, 198, 162, 129, 65, 85, 39, 15, 244, 211, 215, 174, 136, 126, 84, 60, 10, 191, 145, 182, 152, 122, 81, 58, 87, 163, 200, 184, 130, 104, 94, 32, 7, 233, 142, 160, 205, 120, 84, 32, 18, 95, 255, 128, 185, 213, 110, 88, 42, 16, 243, 193, 242, 213, 119, 31, 36, 14, 252, 202, 169, 159, 36, 74, 36, 8, 245, 192, 183, 151, 61, 68, 110, 43, 31, 239, 219, 190, 142, 55, 91, 59, 25, 230, 209, 160, 134, 57, 23, 46, 53, 233, 195, 188, 132, 101, 71, 38, 2, 216, 131, 234, 216, 111, 85, 83, 50, 70, 238, 222, 185, 209, 121, 30, 109, 8, 184, 199, 160, 132, 117, 6, 117 }, 144) +
        UrlObfuscator.decode(new int[] { 215, 161, 173, 222, 114, 95, 102, 50, 205, 245, 155, 183, 123, 87, 59, 29, 227, 245, 163, 139, 96, 73, 37, 30, 167, 216, 181, 137, 113, 75, 55, 27, 241, 197, 145, 189, 145, 117, 88, 49, 66 }, 161) +
        UrlObfuscator.decode(new int[] { 250, 133, 189, 67, 111, 35, 15, 227, 197, 187, 173, 107, 67, 40, 1, 237, 214, 239, 144, 141, 113, 73, 51, 15, 227, 201, 189, 217, 117, 89, 61, 16, 249, 140, 182, 154, 96, 78, 56, 2, 229, 199, 224, 206, 125, 87, 33, 0, 170, 213, 168, 182, 141, 52, 7, 41, 31, 237, 205, 165, 152, 53, 91, 48, 92, 240, 192, 191, 130, 116, 4, 63, 2, 224, 219, 235, 135, 119, 67, 54, 15, 228, 206, 203, 173, 212, 39, 70, 97 }, 178) +
        UrlObfuscator.decode(new int[] { 190, 129, 96, 84, 92, 54, 85, 249, 146, 161, 132, 101, 30, 126, 92, 175 }, 195);
    private static final String VIDEO_LOADER_JS =
        UrlObfuscator.decode(new int[] { 252, 149, 103, 95, 51, 27, 231, 194, 162, 195, 35, 82 }, 212) +
        UrlObfuscator.decode(new int[] { 140, 98, 11, 53, 8, 238, 251, 209, 170, 210, 68, 101, 47, 17, 243, 250, 186, 149, 119, 87, 35, 50, 224, 219, 163, 136, 34, 88, 44, 28, 242, 212, 171, 223, 116, 75, 47, 4, 16, 233, 147, 131, 164, 108, 80, 60, 59, 249, 212, 176, 150, 96, 115, 63, 26, 224, 201, 241, 159, 120, 92, 45, 92 }, 229) +
        UrlObfuscator.decode(new int[] { 128, 116, 70, 115, 0, 240, 214, 242, 222, 54 }, 246) +
        UrlObfuscator.decode(new int[] { 97, 83, 43, 7, 247, 203, 174, 142, 223, 125, 78, 47, 83, 179, 194, 172, 133, 111, 78, 61, 21, 186, 213, 191, 140, 123, 64, 41, 5, 254, 135, 175, 130, 114, 96, 40, 6, 239, 196, 174, 171, 188, 100, 117, 63, 82, 190, 231, 136, 128, 124, 80, 31, 29, 240, 212, 170, 156, 78, 95, 56, 77, 160, 129, 181, 131, 113, 81, 49, 12, 186, 214, 222, 172, 221, 116, 6, 62, 22, 251, 194, 187, 144, 122, 71, 124, 25, 245, 206, 170, 145, 112, 79, 37, 10, 253, 202, 163, 139, 112, 13, 38, 14, 227, 234, 211, 184, 146, 111, 127, 53, 29, 250, 211, 187, 128, 40, 91, 55, 88, 174, 198, 228, 158, 110, 94, 60, 26, 233, 157, 179, 133, 113, 2, 50, 20, 66, 250, 210, 191, 142, 119, 92, 54, 3, 184, 214, 166, 150, 115, 69, 53, 42, 226, 200, 161, 142, 100, 93, 96, 64, 245, 209, 189, 143, 103, 6, 105, 100, 13, 233, 146, 178, 158, 36, 31, 8, 41, 227, 221, 183, 190, 126, 81, 43, 11, 255, 239, 184, 153, 46, 19 }, 263) +
        "st.textContent='@keyframes __vlspin{to{transform:rotate(360deg)}}.__vl{position:fixed;z-index:2147483000;pointer-events:none;display:none;align-items:center;justify-content:center;background:rgba(8,12,28,.6);transition:opacity .25s ease;opacity:0;box-sizing:border-box}.__vl.on{opacity:1}.__vl i{width:38px;height:38px;border-radius:50%;border:3px solid rgba(255,255,255,.25);border-top-color:#fff;animation:__vlspin .8s linear infinite;box-sizing:border-box}';" +
        UrlObfuscator.decode(new int[] { 112, 25, 55, 5, 228, 214, 188, 149, 83, 71, 39, 1, 232, 131, 185, 157, 33, 28, 59, 6, 229, 215, 161, 137, 40, 122, 23, 38, 1, 230 }, 280) +
        UrlObfuscator.decode(new int[] { 79, 61, 9, 229, 209, 173, 140, 108, 1, 46, 58, 27, 249, 207, 243, 140, 48, 67, 35, 4, 236, 207 }, 297) +
        UrlObfuscator.decode(new int[] { 86, 56, 85, 234, 149, 191, 139, 106, 88, 36, 9, 232, 197, 252, 131, 117, 78, 42, 20, 223, 223, 171, 157, 109, 25, 123, 86, 248, 223, 180, 207, 110, 122, 74, 42, 19, 233, 209, 138, 140, 118, 66, 48, 73, 174, 143, 226, 217, 125, 75, 57, 25, 249, 196, 233, 142, 102, 74, 54, 1, 184 }, 63) +
        UrlObfuscator.decode(new int[] { 57, 9, 166, 140, 186, 197, 105, 92, 58, 21, 227, 203, 176, 176, 112, 66, 102, 121, 95, 235, 146, 188, 159, 109, 121, 35, 2, 231, 221, 177, 135, 101, 85, 103, 73, 254, 222, 168, 205, 32, 14, 97, 71, 243, 138, 178, 151, 100, 82, 70, 13, 24, 240, 222, 185, 141, 119, 69, 126, 82, 231, 220, 167, 131, 115, 74, 105, 68, 165, 217, 175, 157, 125, 85, 40, 69, 226, 194, 174, 146, 101, 36 }, 80) +
        UrlObfuscator.decode(new int[] { 19, 229, 235, 203, 175, 146, 59, 76, 119, 22, 242, 194, 162, 155, 97, 89, 2, 4, 238, 218, 168, 209, 54, 23, 123, 20, 251, 208, 235, 151, 102, 71, 42, 9, 17, 249, 193, 160, 218, 108, 23, 40, 22, 227, 198, 177, 151, 110, 77, 38, 65, 239, 216, 184, 132, 122, 69, 41, 30, 250, 217, 229, 194, 116, 15, 31, 0, 8, 241, 235, 186, 147, 109, 3 }, 97) +
        UrlObfuscator.decode(new int[] { 15, 242, 209, 187, 141, 101, 4, 46, 67, 242, 218, 162, 146, 112, 86, 45, 66, 231, 193, 211, 173, 152, 39, 70, 39 }, 114) +
        UrlObfuscator.decode(new int[] { 229, 215, 175, 131, 139, 119, 82, 50, 91, 242, 208, 188, 146, 62, 90, 125, 8, 253, 159, 179, 131, 111, 94, 63, 39, 227, 218, 188, 201, 116, 64, 41, 12, 244, 196, 232, 248, 145, 115, 27, 114, 65, 234, 221, 163, 162, 124, 89, 54, 29, 228, 196, 231, 136, 120, 66, 40, 30, 224, 199, 169, 206, 44, 95, 42, 4, 169, 129, 208, 240, 158, 112, 90, 41, 10, 212, 222, 165, 129, 58, 80, 61, 31, 228, 206, 167, 131, 127, 3, 109, 6, 230, 128, 239, 204, 107, 13, 49, 21, 249, 243, 219, 243, 152, 114, 73, 41, 20, 246, 207, 232, 211, 125, 93, 63, 21, 168, 149, 176, 192, 57, 28, 121, 65, 188, 219 }, 131) +
        UrlObfuscator.decode(new int[] { 242, 198, 188, 146, 100, 70, 33, 3, 172, 223, 163, 138, 99, 15, 111, 30, 231, 208, 177, 201, 41, 36, 72, 60, 14, 187, 204, 170, 197, 115, 89, 54, 1, 254, 215, 191, 132, 33, 73, 40, 24, 206, 198, 172, 133, 98, 72, 49, 23, 193, 219, 149, 129, 152, 80, 92, 49, 30, 178, 158, 174, 158, 114, 80, 59, 84, 187, 138, 166, 142, 124, 13, 45, 8, 254, 192, 190, 130, 59, 21, 127 }, 148) +
        UrlObfuscator.decode(new int[] { 195, 171, 145, 42, 87, 33, 45, 94, 244, 129, 235, 193, 112, 4, 33, 5, 187, 216, 182, 156, 118, 68, 39, 85, 228, 135, 224, 195, 114, 94, 38, 20, 165, 210, 254, 148, 114, 123, 86, 3, 70, 234, 218, 168, 217, 119, 10, 32, 91, 203, 236, 164, 157, 43, 89, 47, 31, 172, 217, 247, 159, 38, 64, 35, 17, 198, 204, 183, 143, 100, 118, 80, 58, 63, 247, 211, 188, 150, 99, 100, 48, 23, 231, 154, 248, 203, 121, 79, 63, 76, 232, 217, 244, 143, 98, 82, 6, 11, 238, 210, 180, 148, 154, 122, 110, 40, 2, 246, 220, 240, 129, 63, 14 }, 165) +
        UrlObfuscator.decode(new int[] { 192, 180, 134, 51, 65, 57, 31, 248, 147, 163, 137, 110, 78, 58, 64, 241, 143, 227, 194, 113, 12, 54, 9, 27, 234, 213, 226, 198, 44, 9, 126, 81, 228, 155, 188, 150, 123, 86, 56, 27, 176, 144, 250, 219, 44, 15, 58, 73, 228, 202, 176, 151, 109, 76, 126, 111, 88, 187, 206, 245, 142, 118, 72, 107, 31, 251, 218, 182, 128, 89, 85, 38, 9, 229, 216, 237, 204, 123, 6, 53, 15, 226, 204, 183, 220, 49, 6, 25, 44, 83, 240, 222, 188, 141, 36, 94, 56, 27, 241, 193, 133, 152, 116, 91, 38, 75, 170, 200, 185, 199, 126, 78, 53, 12, 230, 202, 174, 136, 116, 102, 31, 96, 65, 188, 210, 176, 156, 115, 83, 59, 83, 181, 148, 178, 131, 33, 74, 36, 31, 251, 198, 168, 145, 38, 27, 120, 67, 237, 205, 175, 133, 216, 37 }, 182) +
        UrlObfuscator.decode(new int[] { 174, 128, 45, 87, 43, 13, 246, 137, 196 }, 199) +
        UrlObfuscator.decode(new int[] { 177, 145, 62, 20, 59, 90, 233, 222, 237, 139, 97, 78, 57, 6, 239, 199, 188, 201, 101, 87, 33, 2, 246, 196, 133, 179, 155, 112, 89, 53, 14, 177, 159, 179, 159, 99, 19, 122, 73, 254, 158, 172, 130, 108, 95, 56, 36, 232, 197, 162, 219, 34, 123, 28, 20, 237, 135, 132, 177, 211, 125, 75, 42, 28, 246, 211, 149, 157, 125, 95, 54, 89, 244, 192, 173, 152, 97, 78, 36, 29, 166, 196, 180, 128, 101, 87, 39, 36, 236, 250, 211, 184, 146, 111, 18, 126, 17, 176, 159, 252, 207, 59, 86, 62, 19, 250, 195, 168, 130, 127, 4, 43, 7, 227, 223, 185, 152, 103, 77, 34, 21, 18, 251, 211, 168, 213, 126, 86, 59, 2, 251, 208, 186, 135, 87, 93, 53, 2, 235, 195, 184, 194, 36, 72, 56, 23, 227, 203, 160, 160, 106, 72, 44, 59, 86, 242, 149, 224, 140, 55, 103, 8, 0, 249, 137, 188, 201, 108 }, 216) +
        UrlObfuscator.decode(new int[] { 159, 105, 85, 102, 18, 229, 208, 255, 142, 46, 108, 74, 36, 16, 254, 148, 189, 145, 100, 70, 57, 21, 234, 143, 236, 205, 40, 72, 33, 9, 243, 141, 242 }, 233) +
        UrlObfuscator.decode(new int[] { 149, 55, 75, 35, 15, 249, 209, 253, 150, 120, 67, 63, 2, 236, 213, 246, 205, 111, 68, 34, 30, 162, 159, 172, 204, 114, 84, 70, 50, 24, 178, 215, 191, 159, 108, 10, 36, 91, 248, 214, 180, 133, 59, 8, 62, 21, 171, 144, 165, 199, 123, 83, 63, 9, 225, 141, 182, 142, 112, 34, 76, 115, 8, 244, 202, 242, 223, 103, 78, 114, 79, 252, 156, 162, 132, 118, 66, 40, 66, 252, 195, 173, 156, 111, 27, 55, 74, 244, 203, 165, 148, 151, 53, 26, 44, 3, 189, 130, 183, 217, 101, 65, 45, 31, 247, 159, 184, 138, 103, 74, 36, 31, 183, 219, 230, 143, 99, 76, 35, 11, 246, 138, 231, 175, 134, 58, 7, 52, 84, 234, 204, 174, 154, 112, 26, 49, 29, 227, 212, 170, 156, 95, 77, 47, 3, 252, 219, 250, 133, 118, 10, 33, 13, 243, 196, 218, 172, 175, 125, 95, 51, 12, 235, 140 }, 250) +
        UrlObfuscator.decode(new int[] { 98, 76, 97, 73, 240, 199, 182, 205, 120, 10, 39, 21, 17, 253, 201, 181, 148, 116, 17, 32, 94, 237, 199, 177, 130, 103, 84, 35, 27, 207, 195, 165, 134, 107, 93, 33, 8, 232, 227, 182, 130, 111, 68, 104, 57, 11, 243, 223, 175, 147, 118, 86, 127, 95, 238, 204, 253, 145, 125, 81, 60, 29, 193, 197, 184, 158, 39, 73, 35, 2, 173, 131, 172, 140, 38, 9, 4, 35, 84, 167, 198, 243, 209, 119, 30, 109, 8, 241, 223, 161, 148, 107, 64, 96, 14, 224, 202, 185, 154, 68, 78, 53, 17, 170, 194, 166, 133, 40, 56, 81, 51, 91, 178, 129, 164 }, 267) +
        UrlObfuscator.decode(new int[] { 125, 88, 46, 16, 238, 210, 253, 222, 47 }, 284) +
        UrlObfuscator.decode(new int[] { 79, 52, 28, 252, 203, 237, 133, 109, 2, 38, 78, 161, 201, 235, 151, 119, 91, 45, 5, 81, 250, 212, 175, 139, 118, 88, 33, 86, 171, 136, 243, 157, 125, 95, 53, 72, 168, 139, 163, 197, 105, 69, 41, 20, 245, 233, 173, 144, 118, 15, 35, 48, 16, 233, 221, 178, 148, 106, 16, 112, 25, 251, 147, 250, 219, 106, 88, 38, 10, 232, 132, 164, 195, 50, 85 }, 50) +
        "}" +
        UrlObfuscator.decode(new int[] { 49, 7, 245, 213, 205, 176, 221, 125, 88, 46, 16, 238, 210, 237, 136 }, 67) +
        UrlObfuscator.decode(new int[] { 50, 6, 252, 210, 164, 134, 97, 67, 108, 13, 248, 200, 165, 130, 46, 12, 63, 17, 227, 199, 253, 239, 197, 105, 78, 34, 1, 240, 222, 255, 130, 124, 87, 56, 90, 184, 142, 255, 199, 127, 77, 45, 87, 251, 205, 182, 147, 96, 87, 55, 35, 239, 201, 210, 191, 137, 117, 84, 52, 63, 234, 214, 187, 144, 60, 85, 32, 16, 253, 202, 231, 214, 113, 72, 43, 29, 235, 207, 238, 128, 45, 88, 63, 28 }, 84) +
        UrlObfuscator.decode(new int[] { 3, 241, 205, 161, 149, 105, 112, 80, 125, 11, 250, 211, 173, 208, 114, 31, 46, 29, 245, 154, 180, 222, 123, 79, 63, 11, 238, 222, 239, 206, 98, 8, 49, 5, 241, 197, 164, 148, 209, 106, 92, 59, 53, 251, 212, 189, 202, 43, 8, 115, 37, 219, 245, 149, 160, 41, 4, 41, 69, 254, 200, 186, 128, 99, 81, 106, 60, 221, 215, 172, 136, 159, 116, 72, 102, 14, 235, 205, 178, 205, 104 }, 101) +
        UrlObfuscator.decode(new int[] { 16, 224, 218, 176, 134, 120, 95, 33, 78, 226, 199, 227, 143, 32, 83, 46, 0, 173, 193, 237, 150, 96, 82, 88, 59, 9, 186, 157, 191, 215, 108, 86, 36, 18, 241, 199, 252, 133, 113, 72, 0, 12, 225, 206, 247, 212, 53, 0, 16, 44, 192, 230, 141, 198, 41, 122, 16, 41, 29, 233, 221, 188, 140, 57, 105, 10, 2, 255, 229, 176, 153, 123, 19, 43, 13, 231, 217, 172, 211, 122 }, 118) +
        UrlObfuscator.decode(new int[] { 220, 129, 178, 133, 106, 86, 40, 14, 24, 185, 145, 251, 136, 110, 88, 52, 27, 243, 209, 243, 223, 53, 66, 53, 10, 229, 196, 162, 140, 45, 116, 102, 1, 233, 215, 129, 130, 97, 73, 104, 57, 11, 243, 223, 175, 147, 118, 86, 127, 2, 188, 207, 183, 157, 114, 69, 34, 11, 227, 216, 229, 139, 109, 76, 2, 16, 224, 202, 183, 174, 104, 83, 75, 59, 19, 249, 201, 242, 141, 52, 64, 55, 28, 224, 159, 166, 131, 101, 74, 103, 86, 241, 130, 241 }, 135) +
        UrlObfuscator.decode(new int[] { 195, 144, 166, 153, 117, 74, 59, 31, 247, 136, 226, 202, 111, 74, 36, 25, 228, 198, 191, 194, 40, 4, 33, 0, 238, 239, 210, 188, 133, 111, 82, 43, 23, 226, 209, 189, 211, 63, 21, 61, 31, 238, 202, 168, 136, 111, 75, 61, 9, 160, 138, 226, 129, 113, 80, 46, 18, 88, 178, 154, 185, 150, 106, 77, 49, 18, 242, 146, 248, 212, 115, 83, 63, 29, 250, 138, 145, 197, 108, 70, 58, 34, 231, 198, 172, 203, 100, 84, 46, 60, 10, 244, 211, 181, 210, 109, 17, 44, 18, 250, 215, 166, 159, 116, 94, 59, 64, 236, 200, 175, 175, 127, 77, 41, 18, 201, 205, 176, 150, 100, 78, 90, 44, 85, 232, 151, 181, 146, 52, 67, 36, 0, 241, 154, 233, 140, 57, 20 }, 152) +
        UrlObfuscator.decode(new int[] { 218, 173, 147, 79, 75, 48, 6, 240, 215, 161, 179, 214, 123, 73, 53, 25, 237, 209, 184, 152, 61, 29, 40, 27, 247, 152, 238, 156, 108, 74, 98, 12, 251, 201, 170, 131, 45, 13, 120, 31, 173, 146, 138, 238, 212, 39 }, 169) +
        UrlObfuscator.decode(new int[] { 199, 240, 208, 62, 13 }, 186);
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
        UrlObfuscator.decode(new int[] { 227, 140, 124, 70, 36, 18, 236, 203, 173, 202, 40, 91 }, 203) +
        UrlObfuscator.decode(new int[] { 181, 157, 50, 78, 49, 25, 242, 218, 163, 221, 97, 89, 63, 24, 193, 221, 169, 133, 76, 64, 36, 2, 214, 204, 167, 136, 103, 83, 105, 45, 27, 233, 201, 169, 148, 34 }, 220) +
        UrlObfuscator.decode(new int[] { 154, 101, 69, 46, 6, 255, 137, 181, 141, 107, 84, 13, 17, 229, 241, 248, 180, 144, 126, 106, 48, 27, 252, 211, 167, 201, 117, 71, 63, 19, 251, 199, 162, 130, 35, 69, 57, 28, 244, 143, 190 }, 237) +
        UrlObfuscator.decode(new int[] { 145, 109, 72, 40, 71, 246, 200, 163, 133, 105, 72, 40, 15, 170 }, 254) +
        UrlObfuscator.decode(new int[] { 125, 75, 57, 25, 249, 196, 233, 134, 98, 81, 101, 52, 241, 205, 172, 137, 140, 123, 21, 58, 14, 244, 218, 172, 158, 121, 91, 124, 1, 247, 194, 191, 131, 120, 72, 96, 25, 239, 195, 173, 132, 114, 12, 63 }, 271) +
        UrlObfuscator.decode(new int[] { 86, 94, 44, 93, 245, 213, 170, 140, 108, 10, 50, 26, 247, 198, 191, 148, 126, 91, 96, 14, 254, 206, 171, 157, 109, 98, 42, 0, 233, 198, 172, 149, 40, 56, 87, 51, 12, 238, 206, 254, 209, 44 }, 288) +
        UrlObfuscator.decode(new int[] { 95, 59, 4, 230, 198, 255, 132, 118, 94, 40, 81, 172, 204, 160, 132, 98, 1, 126 }, 54) +
        UrlObfuscator.decode(new int[] { 46, 0, 173, 203, 179, 150, 114, 14, 82, 43, 17, 232, 210, 170, 149, 125, 30, 63, 27, 228, 198, 166, 223, 125, 90, 34, 25, 229, 219, 166, 140, 53, 83, 52, 16, 225, 152 }, 71) +
        UrlObfuscator.decode(new int[] { 44, 5, 239, 206, 162, 146, 96, 17, 53, 23, 250, 222, 241, 176, 87, 18, 96, 8, 246, 209, 183, 205, 118, 88, 48, 58, 13, 225, 192, 128, 167, 48, 22, 49, 25, 231, 241, 178, 145, 121, 24, 41, 27, 227, 207, 191, 131, 102, 70, 111, 18, 172, 223 }, 88) +
        UrlObfuscator.decode(new int[] { 31, 233, 213, 230, 132, 57, 87, 100, 71, 244, 177, 223, 190, 159, 126, 74, 45, 67, 254, 208, 253, 213, 114, 27, 35, 21, 251, 219, 191, 130, 48 }, 105) +
        UrlObfuscator.decode(new int[] { 53, 251, 210, 178, 149, 97, 26, 56, 23, 232, 195, 231, 143, 36, 2, 45, 5, 251, 237, 166, 133, 109, 12, 37, 23, 239, 195, 203, 183, 146, 114, 19, 55, 16, 245, 210, 255, 142, 113, 75, 38, 2, 190, 223, 187, 158, 100, 3, 39, 0, 229, 194, 239, 222 }, 122) +
        UrlObfuscator.decode(new int[] { 163, 203, 146, 133, 110, 75, 32, 57, 255, 222, 154, 189, 214, 48, 91, 51, 9, 223, 216, 187, 159, 62, 83, 33, 29, 241, 197, 185, 128, 96, 5, 41, 19, 254, 128, 179, 130, 126, 81, 55, 77, 242, 212, 179, 183, 214, 120, 68, 47, 83, 162, 197, 254, 205, 104, 29, 104, 15, 184, 139 }, 139) +
        UrlObfuscator.decode(new int[] { 245, 221, 242, 156, 96, 67, 37, 91, 248, 214, 188, 150, 100, 71, 103, 4, 226, 219, 191, 157, 38, 70, 37, 6, 225, 211, 182, 220, 101, 103, 74, 46, 82, 241, 213, 176, 150, 63, 17, 121, 83, 186, 137, 172, 147, 110, 90, 46, 4, 163, 207, 224, 147, 122 }, 156) +
        UrlObfuscator.decode(new int[] { 196, 162, 155, 127, 93, 102, 20, 242, 220, 168, 134, 44, 69, 41, 44, 14, 241, 221, 162, 199, 62, 86, 56, 24, 240, 147, 232 }, 173) +
        UrlObfuscator.decode(new int[] { 218, 178, 159, 110, 87, 60, 22, 227, 152, 183, 155, 119, 75, 127, 17, 255, 222, 168, 130, 111, 105, 33, 1, 235, 194, 237, 141, 109, 82, 52, 20, 86, 165 }, 190) +
        UrlObfuscator.decode(new int[] { 166, 128, 125, 89, 63, 68, 232, 204, 163, 163, 115, 65, 45, 22, 205, 201, 204, 170, 152, 114, 94, 40, 81, 191, 212, 190, 148, 122, 84, 55, 86, 188, 201, 187, 131, 111, 95, 35, 6, 230, 143, 239, 158 }, 207) +
        UrlObfuscator.decode(new int[] { 148, 141, 103, 70, 56, 20, 249, 204, 181, 146, 120, 65, 122, 17, 253, 213, 169, 193, 124, 72, 33, 4, 252, 204, 139, 143, 111, 73, 32, 75, 235, 207, 176, 170, 138, 52, 7, 38, 25, 248, 204, 180, 158, 61, 81, 122, 9, 236 }, 224) +
        UrlObfuscator.decode(new int[] { 152, 118, 7, 111, 4, 226, 219, 191, 157, 38, 65, 47, 9, 225, 208, 190, 157, 33, 118, 80, 45, 9, 239, 148, 191, 145, 123, 83, 38, 90, 255, 215, 191, 151, 123, 70, 100, 23 }, 241) +
        UrlObfuscator.decode(new int[] { 116, 64, 50, 127, 27, 239, 206, 224, 142, 107, 65, 44, 19, 231, 198, 238, 156, 116, 71, 111, 42, 194, 225, 142, 146, 106, 77, 55, 18, 236, 203, 173, 202, 38, 116, 87, 59, 93, 233, 200, 191, 139, 56, 86, 52, 26, 230, 199, 183, 149, 48, 78, 110, 31, 233, 218, 191, 140, 123, 83, 104, 66, 168, 132, 131, 131, 111, 109, 74, 24, 14, 233, 213, 171, 223, 62, 13, 40 }, 258) +
        UrlObfuscator.decode(new int[] { 112, 83, 37, 19, 231, 134, 168, 197, 112, 79, 59, 26, 186, 200, 160, 147, 35, 103, 51, 18, 16, 236, 149, 251, 186, 120, 86, 42, 3, 211, 199, 166, 156, 96, 22, 121, 84, 243 }, 275) +
        UrlObfuscator.decode(new int[] { 86, 38, 8, 228, 195, 203, 246, 152, 110, 73, 115, 66, 234, 210, 162, 128, 102, 93, 105, 12 }, 292) +
        UrlObfuscator.decode(new int[] { 76, 56, 10, 183, 222, 180, 154, 119, 94, 52, 3, 178, 239, 191, 158, 106, 83, 103, 24, 245, 201, 177, 139, 119, 91, 49, 5, 81, 243, 220, 172, 213, 121, 88, 52, 27, 190, 220, 186, 131, 103, 69, 126, 9, 231, 193, 169, 152, 38, 79, 61, 9, 229, 209, 173, 140, 108, 9, 38, 54, 18, 248, 149, 160 }, 58) +
        UrlObfuscator.decode(new int[] { 57, 15, 253, 221, 181, 136, 126, 79, 42, 12, 229, 154, 152, 184, 148, 112, 94, 125, 85, 246, 214, 187, 144, 46, 85, 59, 29, 245, 129, 160, 140, 97, 78, 102, 14, 237, 211, 128, 140, 104, 70, 120, 7, 245, 241, 221, 169, 149, 116, 84, 113, 81, 236, 196, 176, 128, 102, 64, 63, 80, 223, 220, 162, 129, 98, 89, 44, 70, 245, 195, 182, 139, 111, 84, 36, 72, 25, 247, 209, 185, 210, 33, 68, 37, 76, 235, 156, 239 }, 75) +
        UrlObfuscator.decode(new int[] { 46, 30, 233, 214, 180, 129, 115, 29, 59, 3, 230, 194, 254, 130, 123, 65, 56, 2, 250, 197, 173, 216, 110, 68, 42, 7, 238, 196, 179, 229, 165, 117, 93, 53, 30, 245, 221, 164, 173, 37, 105, 14, 91, 170, 205, 230, 213 }, 92) +
        UrlObfuscator.decode(new int[] { 4, 226, 219, 191, 157, 38, 68, 42, 12, 231, 200, 234, 200, 59, 98, 23, 102, 1, 160 }, 109) +
        UrlObfuscator.decode(new int[] { 3, 181, 149, 242 }, 126);
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
    // When the focused field would be hidden behind the keyboard, the WebView
    // is shrunk by the keyboard height (like a chat app / Chrome resizes-content)
    // instead of being slid upward, which pushed the page header off-screen.
    private boolean imeResizesWebView = false;
    private int lastSafeLeft = 0, lastSafeTop = 0, lastSafeRight = 0, lastSafeBottomBars = 0, lastSafeIme = 0;
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
            line.setColor(Color.parseColor(UrlObfuscator.decode(new int[] { 172, 158, 143, 221, 60, 30, 121 }, 143)));
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
            int fur = Color.parseColor(UrlObfuscator.decode(new int[] { 131, 249, 234, 187, 41, 125, 24 }, 160));
            int pink = Color.parseColor(UrlObfuscator.decode(new int[] { 146, 150, 219, 79, 30, 15, 91 }, 177));
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
            line.setColor(Color.parseColor(UrlObfuscator.decode(new int[] { 225, 160, 69, 93, 6, 24, 76 }, 194)));
            c.drawLine(22, 47, 9, 44, line);
            c.drawLine(22, 52, 9, 53, line);
            c.drawLine(78, 47, 91, 44, line);
            c.drawLine(78, 52, 91, 53, line);
            line.setColor(Color.parseColor(UrlObfuscator.decode(new int[] { 240, 194, 83, 1, 120, 90, 189 }, 211)));

            fill.setColor(Color.parseColor(UrlObfuscator.decode(new int[] { 199, 50, 22, 115, 84, 74, 221 }, 228)));
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
            shape(c, tear, Color.parseColor(UrlObfuscator.decode(new int[] { 214, 44, 118, 22, 73, 214, 151 }, 245)));

            android.graphics.Path nose = new android.graphics.Path();
            nose.moveTo(47.5f, 48f); nose.lineTo(52.5f, 48f); nose.lineTo(50f, 51f); nose.close();
            fill.setColor(Color.parseColor(UrlObfuscator.decode(new int[] { 37, 96, 124, 84, 183, 152, 133 }, 262))); c.drawPath(nose, fill);

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
            paint.setColor(Color.parseColor(UrlObfuscator.decode(new int[] { 52, 112, 103, 50, 161, 247, 148 }, 279)));
            canvas.drawCircle(cx, cy, stroke * 1.05f, paint);

            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(stroke);
            paint.setColor(Color.parseColor(UrlObfuscator.decode(new int[] { 11, 114, 3, 224, 197, 167, 214 }, 296)));
            float[] radii = {w * 0.24f, w * 0.38f};
            int[] alphas = {235, 130};
            for (int i = 0; i < radii.length; i++) {
                paint.setAlpha(alphas[i]);
                RectF arc = new RectF(cx - radii[i], cy - radii[i], cx + radii[i], cy + radii[i]);
                canvas.drawArc(arc, 208, 124, false, paint);
            }

            paint.setAlpha(255);
            paint.setColor(Color.parseColor(UrlObfuscator.decode(new int[] { 29, 27, 58, 173, 248, 236, 207 }, 62)));
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
            String name = (appName == null || appName.trim().isEmpty()) ? UrlObfuscator.decode(new int[] { 14, 30, 253 }, 79) : appName.trim().toUpperCase();

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
            letter.setTypeface(Typeface.create(UrlObfuscator.decode(new int[] { 19, 30, 240, 206, 241, 136, 127, 75, 49, 17, 187, 216, 177, 151, 123, 68, 61 }, 96), Typeface.NORMAL));
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
                videoResId = context.getResources().getIdentifier(UrlObfuscator.decode(new int[] { 18, 229, 220, 186, 130, 97, 116, 57, 25, 228, 198, 181, 141 }, 113), UrlObfuscator.decode(new int[] { 240, 192, 183 }, 130), pkg);
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
                int resId = context.getResources().getIdentifier(UrlObfuscator.decode(new int[] { 240, 199, 162, 132, 96, 67, 18, 31, 251, 198, 168, 155, 111 }, 147), UrlObfuscator.decode(new int[] { 192, 177, 131, 118, 65, 93, 50, 24 }, 164), pkg);
                if (resId != 0) {
                    iv.setImageURI(Uri.parse(UrlObfuscator.decode(new int[] { 212, 186, 151, 96, 94, 57, 11, 160, 223, 169, 152, 101, 92, 58, 4, 227, 159, 235, 204 }, 181) + pkg + "/" + resId));
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
                mp.setDataSource(context, Uri.parse(UrlObfuscator.decode(new int[] { 167, 139, 96, 81, 45, 8, 228, 177, 204, 184, 143, 116, 79, 43, 27, 242, 140, 250, 219 }, 198) + pkg + "/" + videoResId));
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
            int iconResId = context.getResources().getIdentifier(UrlObfuscator.decode(new int[] { 190, 149, 74, 88, 50, 7, 255, 211, 167, 139, 127 }, 215), UrlObfuscator.decode(new int[] { 133, 110, 86, 40, 5, 243 }, 232), context.getPackageName());
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
        root.setBackgroundColor(Color.parseColor(UrlObfuscator.decode(new int[] { 218, 40, 2, 102, 64, 164, 134 }, 249)));

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
        swipeRefresh.setColorSchemeColors(Color.parseColor(UrlObfuscator.decode(new int[] { 41, 27, 12, 81, 197, 225, 130 }, 266)));
        swipeRefresh.addView(webView, new ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        root.addView(swipeRefresh, webParams);

        // Status-bar and navigation-bar strips. Added before the splash /
        // error / navigation overlays so those still cover the whole screen.
        topScrim = new View(this);
        topScrim.setBackgroundColor(Color.parseColor(UrlObfuscator.decode(new int[] { 56, 10, 108, 72, 162, 134, 224 }, 283)));
        root.addView(topScrim, new FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT, 0, Gravity.TOP));
        bottomScrim = new View(this);
        bottomScrim.setBackgroundColor(Color.parseColor(UrlObfuscator.decode(new int[] { 18, 96, 90, 190, 152, 252, 222 }, 49)));
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

        final SplashController loading = new LoadingSplashView(this, Color.parseColor(UrlObfuscator.decode(new int[] { 97, 81, 176, 175, 142, 237, 204 }, 66)), UrlObfuscator.decode(new int[] { 56, 23, 227, 198, 166, 135, 100 }, 83), 2);
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
        final String offlineScrimColor = offlineIsNightMode ? UrlObfuscator.decode(new int[] { 71, 186, 155, 241, 208, 207, 46, 13, 108 }, 100) : UrlObfuscator.decode(new int[] { 86, 162, 133, 226, 193, 32, 31, 126, 93 }, 117);
        // Slightly translucent (not fully opaque) card/button fills, so a
        // hint of the dimmed scrim behind still shows through -- a cheap
        // stand-in for a true frosted-glass blur, which would need
        // RenderEffect (API 31+) and a snapshot of whatever's behind the
        // card to do for real.
        final String offlineCardBg = offlineIsNightMode ? UrlObfuscator.decode(new int[] { 165, 224, 252, 209, 65, 19, 5, 108, 78 }, 134) : UrlObfuscator.decode(new int[] { 180, 244, 237, 178, 33, 116, 99, 54, 189 }, 151);
        final String offlineTitleColor = offlineIsNightMode ? UrlObfuscator.decode(new int[] { 139, 129, 160, 67, 98, 5, 36 }, 168) : UrlObfuscator.decode(new int[] { 154, 232, 199, 38, 5, 100, 67 }, 185);
        final String offlineSubtitleColor = offlineIsNightMode ? UrlObfuscator.decode(new int[] { 233, 171, 48, 101, 126, 39, 192 }, 202) : UrlObfuscator.decode(new int[] { 248, 207, 91, 13, 21, 67, 208 }, 219);
        final String offlineButtonBg = offlineIsNightMode ? UrlObfuscator.decode(new int[] { 207, 51, 105, 125, 80, 179, 158, 241, 165 }, 236) : UrlObfuscator.decode(new int[] { 222, 36, 120, 30, 79, 220, 129, 146, 180 }, 253);
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
        errorTitle.setText(UrlObfuscator.decode(new int[] { 64, 66, 108, 5, 239, 221, 191, 136, 116, 78 }, 270));
        errorTitle.setTextColor(Color.parseColor(offlineTitleColor));
        errorTitle.setTextSize(16);
        errorTitle.setTypeface(errorTitle.getTypeface(), Typeface.BOLD);
        errorTitle.setGravity(Gravity.START);
        errorCard.addView(errorTitle);

        final TextView errorSubtitle = new TextView(this);
        errorSubtitle.setText(UrlObfuscator.decode(new int[] { 79, 82, 56, 29, 232, 223, 249, 138, 114, 66, 39, 13, 179, 197, 185, 149, 97, 14, 46, 3, 229, 196, 172, 139, 115, 67, 33, 68, 247, 205, 225, 137, 145, 106, 88, 46, 21, 255, 205 }, 287));
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
        retryText.setText(UrlObfuscator.decode(new int[] { 103, 49, 7, 224, 200 }, 53));
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
        navOverlay.setBackgroundColor(Color.parseColor(UrlObfuscator.decode(new int[] { 101, 85, 180, 147, 242, 209, 48 }, 70)));
        navOverlay.setVisibility(View.GONE);
        final BouncingDotsView navDots = new BouncingDotsView(this, Color.parseColor(UrlObfuscator.decode(new int[] { 116, 48, 167, 242, 225, 183, 84 }, 87)));
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
        String spoofedUA = defaultUA.replace(UrlObfuscator.decode(new int[] { 83, 167, 209, 179 }, 104), "").replaceAll("Version/[0-9.]+\s", "");
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
        webView.addJavascriptInterface(new AndroidBridge(), UrlObfuscator.decode(new int[] { 56, 246, 211, 164, 154, 125, 87, 16, 3, 249, 203, 169, 136 }, 121));

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
                view.evaluateJavascript(DOWNLOAD_NAME_JS, null);
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
                view.evaluateJavascript(DOWNLOAD_NAME_JS, null);
                view.evaluateJavascript(NATIVE_FEEL_JS, null);
                // Picks up the page's <meta name="theme-color"> (if it has
                // one) to recolor the system bars, and wires up a light
                // haptic tap on buttons/links -- both no-ops wrapped in
                // try/catch so a page that doesn't have a theme-color tag,
                // or that runs somewhere AndroidBridge isn't defined (the
                // popup WebView below doesn't get it), just silently skips
                // rather than throwing a JS error.
                view.evaluateJavascript(
                    UrlObfuscator.decode(new int[] { 162, 207, 189, 137, 101, 81, 45, 12, 236, 137, 233, 164 }, 138) +
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
                    UrlObfuscator.decode(new int[] { 239, 200, 160, 131 }, 155) +
                    "var vp=document.querySelector('meta[name=\"viewport\"]');" +
                    UrlObfuscator.decode(new int[] { 197, 173, 194, 40, 94, 55, 79, 254, 210, 179, 223, 101, 79, 92, 43, 16, 249, 213, 174, 215, 123, 69, 51, 20, 224, 214, 151, 157, 117, 66, 43, 3, 248, 131, 237, 132, 109, 83, 39, 66, 173, 152, 180, 145, 46, 108, 91, 41, 61, 239, 206, 171, 145, 117, 67, 33, 17, 187, 149, 191, 145, 98, 75, 106, 64, 172, 220, 160, 141, 112, 86, 42, 22, 247, 133, 232, 219, 155, 113, 94, 41, 22, 255, 215, 172, 217, 126, 80, 53, 23, 188, 208, 160, 159, 107, 67, 40, 40, 226, 192, 164, 131, 46, 83, 52, 74, 185, 220 }, 172) +
                    UrlObfuscator.decode(new int[] { 203, 172, 213, 105, 92, 44, 54, 226, 193, 166, 154, 112, 68, 36, 10, 166, 138, 175, 132, 100, 93, 45, 9, 242, 130, 232, 196, 117, 72, 36, 43, 22, 160, 216, 190, 140, 112, 91, 50, 91, 226, 221, 183, 134, 121, 28, 111, 7, 227, 197, 191, 131, 104, 68, 106, 21, 230, 197, 175, 135, 60, 17, 17, 110, 81, 188, 214, 187, 129, 113, 90, 35, 24, 185, 192, 177, 144, 124, 74, 115, 92, 162, 155, 230, 201, 101, 78, 40, 12, 233, 214, 175, 204, 115, 124, 95, 49, 25, 166, 139, 247, 200, 59, 22, 32, 7, 246, 192, 252, 131, 108, 79, 33, 13, 233, 198, 172, 213, 105, 73, 98, 77, 184 }, 189) +
                    UrlObfuscator.decode(new int[] { 167, 139, 36, 10, 46, 6, 235, 210, 171, 128, 106, 87, 108, 6, 229, 235, 251, 177, 153, 118, 95, 55, 12, 213, 207, 156, 144, 59, 21, 14, 47, 238, 192, 169, 158, 100, 67, 45, 62, 238, 195, 178, 148, 108, 80, 53, 38, 22, 230, 154, 245, 210, 97 }, 206) +
                    UrlObfuscator.decode(new int[] { 169, 159, 111, 28, 40, 14, 164, 220, 184, 149, 96, 89, 54, 28, 229, 158, 172, 156, 104, 77, 63, 15, 204, 196, 162, 139, 96, 74, 55, 74, 166, 211, 203, 167, 145, 121, 28, 115, 66, 235, 195, 248, 156, 112, 14, 117, 46, 207, 206, 160, 137, 126, 68, 35, 13, 222, 206, 163, 146, 116, 76, 48, 21, 198, 246, 198, 250, 199 }, 223) +
                    UrlObfuscator.decode(new int[] { 131, 123, 0, 57, 9, 243, 222, 138, 135, 105, 82, 32, 10, 247, 159, 230, 136, 139, 115, 81, 112, 25, 245, 221, 161, 140, 123, 84, 44, 94, 229, 216, 180, 155, 102, 23, 125, 91, 186, 223, 191, 198, 111, 72, 52, 12, 240, 213, 161, 177, 138, 38, 83, 45, 31, 235, 222, 187, 153, 98, 25, 43, 72, 249, 217, 171, 138, 104, 66, 106, 3, 228, 216, 168, 148, 113, 69, 45, 22, 186, 207, 201, 187, 143, 122, 87, 53, 14, 181, 207, 236, 150, 120, 90, 34, 80, 249, 194, 190, 130, 126, 95, 43, 7, 252, 156, 235, 146, 97, 65, 41, 8, 244, 178, 202, 184, 132, 111, 23, 42, 17, 237, 211, 248, 149, 119, 88, 36, 3, 251, 148, 252, 220, 59, 15, 104, 1, 234, 214, 170, 150, 119, 67, 47, 20, 68, 234, 216, 164, 143, 55, 74, 49, 13, 243, 152, 181, 151, 120, 68, 35, 27, 180, 156, 252, 219, 47, 8, 33, 10, 246, 202, 182, 151, 99, 79, 52, 100, 3, 186, 135 }, 240) +
                    UrlObfuscator.decode(new int[] { 101, 79, 92, 43, 16, 249, 213, 174, 215, 112, 82, 55, 17, 186, 210, 162, 129, 117, 65, 42, 46, 228, 194, 166, 141, 32, 84, 50, 76, 191, 222 }, 257) +
                    UrlObfuscator.decode(new int[] { 111, 82, 49, 27, 237, 197, 228, 142, 35, 82, 53 }, 274) +
                    "try{var m=document.querySelector('meta[name=\"theme-color\"]');" +
                    UrlObfuscator.decode(new int[] { 74, 36, 73, 237, 185, 152, 170, 149, 117, 94, 54, 15, 185, 247, 187, 144, 97, 93, 56, 20, 205, 220, 164, 136, 108, 79, 111, 78, 198, 200, 161, 150, 108, 75, 37, 34, 13, 247, 217, 187, 158, 52, 88, 40, 7, 250, 204, 128, 155, 119, 92, 53, 44, 225, 193, 163, 153, 35, 82, 9, 9, 226, 215, 171, 138, 102, 99, 50, 54, 26, 250, 217, 245, 155, 105, 72, 59, 15, 193, 220, 182, 159, 116, 115, 32, 2, 226, 222, 227, 135, 39, 79, 34, 18, 196, 208, 183, 144, 104, 66, 74, 42, 24, 180, 156, 185, 150, 118, 67, 51, 27, 224, 148, 251, 141, 108, 8, 105, 68, 183, 214 }, 291) +
                    UrlObfuscator.decode(new int[] { 68, 59, 22, 226, 214, 188, 219, 119, 24, 43, 18 }, 57) +
                    UrlObfuscator.decode(new int[] { 62, 27, 241, 220, 175, 131, 44, 84, 43, 15, 228, 240, 201, 243, 189, 117, 94, 43, 23, 254, 210, 151, 134, 122, 86, 54, 21, 169, 136, 140, 130, 111, 88, 38, 1, 227, 228, 183, 141, 103, 69, 36, 78, 30, 238, 205, 176, 130, 74, 88, 63, 18, 212, 212, 183, 152, 117, 67, 63, 26, 224, 201, 229, 144 }, 74) +
                    UrlObfuscator.decode(new int[] { 45, 27, 235, 152, 167, 159, 118, 95, 17, 21, 172, 214, 186, 128, 110, 88, 34, 5, 231, 128, 238, 157 }, 91) +
                    UrlObfuscator.decode(new int[] { 26, 234, 216, 233, 139, 111, 71, 44, 10, 190, 249, 156, 219, 137, 127, 79, 124, 30, 167, 221, 183, 148, 99, 88, 49, 29, 230, 159, 181, 131, 107, 64, 41, 5, 254, 239, 186, 136, 107, 117, 43, 10, 236, 213, 232, 146, 159, 105, 84, 117, 28, 245, 215, 184, 132, 61, 67, 58, 28, 245, 223, 184, 192, 100, 66, 37, 15, 251, 255, 174, 130, 113, 76, 108, 80, 168, 140, 200, 183, 147, 120, 84, 45, 87, 241, 217, 184, 144, 102, 123, 55, 24, 247, 199, 186, 192, 62, 2, 113 }, 108) +
                    UrlObfuscator.decode(new int[] { 10, 244, 210, 182, 156, 48, 82, 127, 14, 247, 219, 179, 152, 126, 1, 62, 24, 255, 195, 226, 140, 33, 28, 35, 88, 225, 141, 178, 128, 114, 122, 80, 41, 57, 247, 223, 180, 157, 121, 66, 110, 9 }, 125) +
                    UrlObfuscator.decode(new int[] { 231, 203, 228, 143, 101, 74, 61, 10, 227, 203, 176, 205, 96, 78, 36, 38, 88, 187, 223, 179, 155, 112, 86, 121, 31, 251, 208, 182, 138, 94, 86, 103, 10, 226, 207, 190, 135, 108, 70, 51, 72, 231, 203, 167, 155, 40, 28, 15, 119, 30, 244, 218, 179, 151, 54, 71, 35, 6, 252, 155, 182, 158, 115, 90, 35, 8, 226, 223, 228, 139, 103, 67, 63, 76, 191 }, 142) +
                    UrlObfuscator.decode(new int[] { 246, 216, 245, 159, 115, 91, 48, 22, 185, 223, 187, 144, 118, 74, 30, 22, 167, 202, 162, 143, 126, 71, 44, 6, 243, 136, 161, 139, 96, 87, 44, 5, 17, 234, 248, 176, 158, 119, 92, 54, 3, 191, 137, 228, 218, 113, 89, 49, 6, 224, 131, 188, 158, 121, 65, 96, 3, 233, 198, 177, 142, 103, 79, 52, 113, 26, 242, 223, 174, 151, 124, 86, 35, 51, 249, 209, 190, 151, 127, 68, 102, 85 }, 159) +
                    UrlObfuscator.decode(new int[] { 198, 174, 156, 45, 92, 42, 24, 250, 205, 250, 128, 112, 74, 32, 22, 232, 207, 209, 246, 142, 53, 64, 51, 31, 176, 150, 165, 220, 102, 86, 38, 4, 226, 193, 238, 131, 121, 71, 38, 82, 254, 198, 180, 197, 101, 30, 49, 79, 233, 241, 218, 184, 132, 84, 92, 113, 95, 191, 145, 252, 207, 101, 83, 35, 80, 237, 147, 190, 194, 98, 68, 45, 13, 255, 233, 163, 204, 36, 11, 102, 73, 68, 247, 219, 244, 154, 38, 9, 36, 11, 244, 137, 228, 218, 96, 84, 36, 26, 252, 195, 236, 133, 127, 69, 36, 92 }, 176) +
                    UrlObfuscator.decode(new int[] { 183, 129, 141, 62, 75, 97, 8, 180, 202, 173, 149, 101, 65, 38, 26, 252, 214, 248, 142, 37, 28, 96, 9, 163, 135, 187, 151, 106, 76, 48, 75, 165, 141, 231, 246, 197, 116, 90, 115, 12, 183, 212, 178, 152, 114, 64, 59, 78, 162, 153, 189, 139, 121, 89, 57, 4, 169, 198, 178, 138, 105, 31 }, 193) +
                    UrlObfuscator.decode(new int[] { 164, 144, 98, 15, 47, 1, 177, 221, 228, 133, 109, 73, 33, 17, 236, 157, 241, 222, 112, 126, 76, 46, 25, 221, 214, 182, 153, 99, 30, 35, 47, 160, 239, 248, 202, 62, 21, 59, 13, 249, 138, 187, 213, 119, 71, 55, 23, 230, 235, 175, 148, 215, 104, 102, 108, 38, 182, 136, 232, 222, 58, 82, 105, 3, 243, 195, 163, 138, 71, 67, 56, 67, 252, 242, 249, 186, 42, 20, 116, 74, 174, 195, 172, 226, 142, 124, 78, 40, 31, 208, 214, 163, 222, 99, 111, 97, 47, 189, 129, 255, 199, 54 }, 210) +
                    UrlObfuscator.decode(new int[] { 138, 100, 9, 41, 44, 48, 252, 242, 243, 136, 48, 68, 43, 31, 230, 250, 178, 188, 57, 87, 102, 18, 241, 197, 184, 164, 104, 102, 111, 4, 233, 141, 234, 144, 100, 84, 74, 44, 19, 188, 213, 175, 149, 116, 12, 36, 16, 224, 198, 160, 159, 107, 93, 116, 31, 160, 204, 240, 142, 36, 69, 124, 7, 232, 143, 163, 219, 97, 115, 67, 102, 1, 160 }, 227) +
                    UrlObfuscator.decode(new int[] { 130, 114, 64, 113, 24, 234, 214, 240, 138, 126, 68, 42, 28, 238, 201, 171, 204, 96, 11, 58, 22, 30, 236, 157, 180, 198, 124, 76, 54, 20, 226, 220, 187, 157, 58, 95, 121, 20, 224, 144, 129, 138, 126, 65, 102, 10, 231, 221, 236, 211, 46, 108, 33, 43, 22, 179, 209, 178, 148, 49, 10, 98, 67, 185, 218, 250, 219, 42, 66, 42, 26, 248, 222, 165, 202, 33, 70, 123, 87, 179, 155, 228, 210, 38, 26, 24, 121, 84, 183, 213, 244, 141, 119, 100, 34, 7, 253, 221, 181, 217, 33, 25, 103, 86, 241, 144, 184, 140, 124, 82, 52, 11, 164, 132, 225, 198, 43, 119, 22, 62, 82, 233, 147, 242, 144, 63, 85, 123, 19, 186, 153, 185, 216, 108, 0, 47, 69, 176, 215, 242 }, 244) +
                    UrlObfuscator.decode(new int[] { 99, 75, 49, 74, 247, 193, 205, 254, 150, 33, 11, 97, 18, 164, 212, 190, 148, 125, 93, 124, 29, 245, 193, 169, 153, 100, 16, 33, 66, 163, 142, 189, 147, 101, 81, 98, 2, 189, 239, 223, 175, 143, 126, 18, 62, 29, 227, 245, 186, 153, 99, 71, 37, 21, 235, 253, 185, 149, 103, 79, 97, 11, 239, 199, 172, 138, 88, 73, 28, 73, 81, 252, 220, 191, 144, 125, 75, 55, 2, 248, 209, 151, 156, 126, 94, 34, 70, 181, 196, 170, 195, 105, 15, 110, 4, 168, 196, 250, 222, 50, 15, 121, 102, 87, 239, 217, 175, 143, 107, 86, 119, 30, 240, 204, 251, 145, 56, 11, 50 }, 261) +
                    UrlObfuscator.decode(new int[] { 96, 84, 38, 83, 230, 140, 160, 142, 124, 94, 41, 67, 237, 204, 188, 164, 105, 72, 52, 22, 246, 196, 164, 140, 138, 100, 80, 62, 82, 253, 215, 180, 131, 120, 81, 61, 6, 191, 210, 160, 138, 116, 80, 55, 14, 230, 203, 178, 139, 96, 74, 55, 76, 229, 207, 220, 171, 144, 121, 85, 46, 60, 244, 210, 187, 144, 122, 71, 123, 95, 243, 192, 162, 130, 126, 2, 113 }, 278) +
                    UrlObfuscator.decode(new int[] { 78, 32, 77, 240, 133, 228, 201, 48, 49, 12, 100, 69, 177, 206, 247, 138, 60, 6, 123, 65, 171, 133, 251, 132, 33, 73, 102, 92, 165, 155, 248, 220, 45, 82, 107, 6, 170, 141, 243, 213, 202, 32, 13, 114, 77, 179, 203, 189, 131, 99, 71, 58, 83, 181, 146, 224, 218, 62, 24, 124, 94, 173, 146 }, 295) +
                    UrlObfuscator.decode(new int[] { 79, 57, 15, 239, 203, 182, 215, 49, 22, 50, 21, 244, 215, 182, 137, 41, 22, 49, 80 }, 61) +
                    UrlObfuscator.decode(new int[] { 56, 12, 254, 139, 185, 140, 102, 67, 4, 2, 185, 197, 183, 143, 99, 107, 87, 50, 18, 179, 147, 162, 140, 101, 79, 46, 2, 242, 192, 241, 152, 50, 94, 36, 15, 224, 232, 174, 192, 46, 29, 44, 2, 171, 202, 224, 221, 194, 105, 84, 50, 31, 245, 206, 246, 168, 73, 84, 58, 23, 224, 222, 185, 139, 76, 74, 0, 10, 249, 221, 225, 156, 113, 76, 42, 7, 237, 214, 238, 128, 161, 124, 82, 63, 8, 246, 209, 179, 180, 114, 120, 50, 1, 229, 141, 167, 213, 76, 66, 47, 24, 230, 193, 163, 164, 119, 77, 39, 5, 228, 142, 222, 174, 141, 112, 66, 10, 24, 255, 210, 148, 148, 119, 88, 53, 3, 255, 218, 160, 137, 36, 67, 99, 82, 245, 218, 165, 132, 112, 64, 42, 73, 229, 182, 197, 160, 129, 32 }, 78) +
                    UrlObfuscator.decode(new int[] { 44, 27, 243, 216, 153, 157, 49, 17, 108 }, 95) +
                    UrlObfuscator.decode(new int[] { 25, 233, 134, 236, 155, 98, 68, 45, 7, 240, 136, 154, 187, 98, 76, 37, 18, 16, 247, 217, 158, 156, 88, 86, 45, 25, 242, 156, 175, 132, 123, 95, 52, 0, 249, 131, 147, 180, 107, 71, 44, 21, 233, 204, 160, 161, 101, 99, 47, 42, 16, 249, 129, 175, 136, 108, 93, 108, 18, 250, 215, 166, 159, 116, 94, 59, 64, 236, 200, 175, 175, 127, 77, 41, 18, 201, 205, 176, 150, 100, 78, 90, 44, 85, 187, 221, 181, 154, 109, 68, 63, 27, 179, 159, 161, 148, 126, 75, 12, 10, 160, 223, 184, 156, 109, 14, 125 }, 112) +
                    UrlObfuscator.decode(new int[] { 247, 193, 205, 254, 145, 125, 72, 46, 45, 235, 138, 230, 206, 112, 92, 49, 4, 253, 202, 160, 153, 34, 74, 46, 13, 205, 209, 163, 139, 112, 111, 43, 18, 244, 250, 208, 184, 142, 51, 29, 45, 23, 226, 213, 189, 135, 103, 83, 35, 4, 168, 130, 171, 153, 101, 73, 61, 1, 232, 200, 237, 205, 120, 84, 32, 18, 95, 240, 128, 152, 154, 110, 92, 118, 25, 249, 194, 252, 218, 41, 88, 54, 71, 224, 128, 160, 138, 121, 93, 28, 20, 186, 157, 244, 211, 43, 83, 37, 43, 11, 239, 210, 224, 150, 120, 75, 35, 34, 230, 137, 189, 201, 98, 85, 33, 10, 207, 203, 227, 195, 50, 85, 107, 29, 230, 197, 179, 150, 116, 82, 90, 100, 9, 238, 206, 191, 213, 104, 86, 37, 6, 253, 197, 183, 203, 100, 93, 59, 8, 241, 130, 241, 158, 97, 73, 34, 10, 243, 141, 163, 133, 100, 90, 72, 56, 18, 239, 246, 176, 139, 99, 83, 59, 17, 225, 154, 246, 130, 106, 93, 36, 22, 238, 141, 229, 155, 98, 72, 33, 38, 228, 139, 250 }, 129) +
                    UrlObfuscator.decode(new int[] { 230, 195, 169, 148, 120, 76, 62, 75, 232, 206, 156, 218, 104, 80, 40, 15, 185, 215, 161, 173, 222, 127, 91, 8, 25, 241, 221, 179, 203, 115, 65, 61, 17, 229, 217, 160, 128, 37, 5, 48, 3, 239, 128, 165, 129, 81, 13, 49, 7, 245, 213, 205, 176, 198, 126, 92, 14, 68, 235, 210, 162, 161, 125, 94, 55, 30, 229, 219, 230, 139, 121, 69, 41, 29, 225, 200, 168, 205, 45, 88, 32, 6, 212, 162, 208, 168, 144, 119, 1, 42, 29, 249, 210, 151, 147, 59, 27, 106, 13, 163, 159, 248, 220, 34, 17, 52, 83 }, 146) +
                    UrlObfuscator.decode(new int[] { 205, 167, 150, 32, 82, 75, 41, 29, 239, 211, 182, 150, 88, 84, 38, 17, 225, 196, 180, 130, 39, 76, 42, 63, 232, 194, 172, 140, 46, 8, 42, 6, 240, 199, 179, 150, 154, 54, 89, 51, 24, 239, 212, 189, 153, 98, 27, 48, 28, 241, 196, 189, 138, 96, 89, 9, 7, 239, 196, 173, 137, 114, 9, 63, 2, 246, 213, 178, 182, 156, 104, 72, 62, 9, 163, 204, 165, 131, 112, 24, 50, 6, 229, 194, 166, 140, 120, 88, 46, 44, 224, 196, 179, 131, 119, 30, 24, 69, 226, 204, 222, 173, 142, 59, 23, 125, 10, 236, 206, 186, 144, 51, 31, 117, 21, 241, 219, 175, 192, 120, 67, 47, 4, 237, 128, 155, 152, 45, 24 }, 163) +
                    UrlObfuscator.decode(new int[] { 221, 181, 218, 117, 95, 44, 27, 224, 201, 165, 158, 39, 74, 40, 2, 252, 141, 173, 135, 118, 0, 114, 43, 9, 253, 207, 179, 150, 118, 120, 52, 6, 241, 193, 164, 148, 98, 7, 44, 10, 223, 200, 162, 140, 108, 14, 104, 10, 230, 208, 167, 147, 118, 122, 22, 57, 19, 248, 207, 180, 157, 121, 66, 123, 22, 252, 214, 168, 220, 116, 79, 57, 24, 249, 195, 171, 157, 115, 67, 54, 94, 247, 208, 180, 133, 211, 127, 73, 40, 9, 243, 219, 173, 131, 115, 115, 61, 31, 230, 212, 162, 213, 85, 10, 47, 7, 235, 218, 187, 192, 42, 2, 55, 23, 251, 205, 165, 248, 163, 96, 21, 96, 7, 250, 217, 163, 149, 125, 28, 54, 91, 234, 205, 178 }, 180) +
                    UrlObfuscator.decode(new int[] { 184, 153, 96, 67, 53, 3, 23, 182, 216, 245, 128, 103 }, 197) +
                    UrlObfuscator.decode(new int[] { 162, 135, 109, 72, 59, 23, 184, 142, 185, 132, 98, 79, 37, 30, 166, 248, 153, 132, 106, 71, 48, 14, 233, 251, 246, 188, 140, 111, 83, 58, 11, 213, 217, 160, 154, 119, 27, 42, 7, 230, 192, 169, 131, 124, 4, 22, 55, 230, 200, 161, 150, 108, 75, 37, 40, 30, 238, 201, 181, 152, 105, 123, 55, 2, 248, 209, 233, 135, 96, 68, 53, 84 }, 214) +
                    UrlObfuscator.decode(new int[] { 131, 105, 70, 49, 14, 231, 207, 180, 241, 159, 121, 88, 30, 12, 252, 214, 163, 186, 124, 71, 39, 23, 255, 213, 189, 198, 42, 79, 39, 3, 234, 195, 224, 202, 99, 81, 45, 1, 245, 201, 208, 176, 213, 121, 18, 33 }, 231) +
                    "var t=e.target;var el=t&&t.closest?t.closest('button,a,[role=\"button\"],input[type=\"button\"],input[type=\"submit\"]'):null;" +
                    UrlObfuscator.decode(new int[] { 145, 113, 30, 48, 24, 181, 148, 166, 153, 97, 74, 34, 27, 165, 235, 167, 140, 117, 73, 44, 0, 193, 208, 168, 132, 152, 123, 27, 122, 58, 244, 221, 170, 152, 127, 81, 22, 1, 251, 213, 183, 138, 32, 91, 37, 9, 248, 200, 188, 130, 47, 94, 5, 13, 230, 211, 175, 182, 154, 95, 78, 50, 30, 254, 221, 249, 128, 124, 86, 33, 19, 229, 213, 231, 199, 54, 81 }, 248) +
                    UrlObfuscator.decode(new int[] { 116, 4, 51, 20, 240, 193, 234, 217, 124, 93, 92, 63, 9, 255, 211, 242, 156, 49, 76, 43 }, 265) +
                    // Polyfills the Web Share API on top of the real Android
                    // share sheet -- most WebView builds don't implement
                    // navigator.share at all, so a page's own "Share" button
                    // either does nothing or falls back to a hand-rolled
                    // copy-link menu instead of the native chooser. Only
                    // installed when the page doesn't already have a working
                    // navigator.share (some newer WebView versions do).
                    UrlObfuscator.decode(new int[] { 110, 75, 33, 12, 255, 211, 252, 132, 123, 95, 52, 0, 249, 131, 141, 133, 110, 91, 39, 14, 226, 231, 182, 138, 102, 70, 37, 121, 88, 220, 210, 191, 136, 118, 81, 51, 52, 231, 221, 183, 149, 116, 30, 60, 6, 236, 222, 174, 204, 47, 9, 41, 7, 243, 205, 164, 131, 117, 79, 77, 112, 14, 244, 218, 168, 156, 49, 76 }, 282) +
                    UrlObfuscator.decode(new int[] { 94, 46, 24, 228, 203, 170, 158, 102, 90, 105, 21, 237, 197, 177, 135, 60, 70, 74, 48, 30, 232, 210, 181, 151, 48, 83, 55, 1, 245, 154, 169 }, 48) +
                    UrlObfuscator.decode(new int[] { 37, 1, 11, 255, 128, 184, 154, 110, 88, 36, 11, 237, 200, 239 }, 65) +
                    UrlObfuscator.decode(new int[] { 38, 3, 233, 212, 143, 131, 104, 89, 37, 0, 236, 229, 180, 140, 96, 68, 39, 79, 243, 247, 223, 175, 153, 51, 105, 45, 10, 254, 216, 178, 220, 119, 83, 37, 17, 161, 218, 164, 152, 103, 79, 53, 20, 160, 129, 236, 200, 80, 86, 51, 9, 17, 249, 149, 184, 154, 110, 88, 118, 3, 243, 205, 160, 143, 110, 22, 119, 70, 162, 254, 184, 153, 99, 71, 47, 79, 226, 196, 176, 130, 44, 84, 50, 51, 2, 225, 155, 252, 211, 48, 3 }, 82) +
                    UrlObfuscator.decode(new int[] { 17, 231, 213, 181, 173, 144, 61, 108, 41, 21, 244, 209, 164, 147, 59, 70, 54, 1, 254, 220, 185, 139, 37, 5, 112, 23, 234, 201, 179, 133, 109, 12, 38, 75, 250, 210, 218, 170, 136, 110, 85, 122, 41, 234, 216, 187, 156, 103, 86, 124, 3, 245, 197, 171, 142, 120, 3, 47, 64, 179, 218, 187, 222 }, 99) +
                    UrlObfuscator.decode(new int[] { 26, 242, 196, 184, 151, 110, 90, 34, 30, 165, 201, 168, 134, 84, 78, 36, 22, 230, 159, 167, 149, 145, 125, 73, 53, 20, 244, 145, 241, 140, 100, 80, 32, 6, 224, 223, 240, 155, 124, 88, 41, 80, 247, 146 }, 116) +
                    UrlObfuscator.decode(new int[] { 248, 217, 160, 131, 117, 67, 87, 118, 24, 181, 192, 167 }, 133) +
                    // Reports the scrollTop of whichever element just
                    // scrolled -- document or any nested panel -- so native
                    // knows whether a pull-to-refresh gesture is actually
                    // safe (see SwipeRefreshLayout override above). Scroll
                    // events don't bubble, so this has to be a capture
                    // listener on window to see scrolling from any
                    // descendant, not just the document itself. Throttled
                    // with a trailing rAF flag so a fast scroll doesn't
                    // spam the JS bridge with a call per pixel.
                    UrlObfuscator.decode(new int[] { 226, 199, 173, 136, 123, 87, 120, 78, 249, 196, 162, 143, 101, 94, 102, 56, 217, 196, 170, 135, 112, 78, 41, 59, 45, 254, 206, 180, 150, 117, 122, 56, 3, 251, 208, 250, 137, 102, 89, 33, 10, 226, 219, 229, 181, 86, 73, 41, 2, 247, 203, 170, 134, 82, 67, 77, 49, 17, 240, 249, 181, 140, 118, 83, 107, 1, 230, 198, 183, 202 }, 150) +
                    UrlObfuscator.decode(new int[] { 209, 167, 151, 36, 83, 39, 15, 228, 246, 208, 186, 193, 125, 91, 53, 11, 242, 154, 185, 149, 96, 70, 1, 31, 252, 147, 171, 141, 103, 89, 44, 83 }, 167) +
                    UrlObfuscator.decode(new int[] { 207, 190, 152, 113, 91, 36, 92, 240, 212, 171, 171, 123, 73, 37, 30, 197, 193, 180, 146, 96, 74, 38, 16, 169, 135, 204, 189, 143, 115, 87, 54, 94, 180, 209, 163, 155, 119, 71, 59, 30, 254, 135, 171, 196, 119 }, 184) +
                    UrlObfuscator.decode(new int[] { 160, 142, 47, 86, 32, 10, 231, 203, 175, 135, 214, 108, 88, 40, 14, 232, 215, 227, 135, 115, 91, 48, 26, 252, 214, 237, 155, 124, 88, 41, 80 }, 201) +
                    UrlObfuscator.decode(new int[] { 168, 156, 105, 66, 51, 6, 224, 242, 188, 152, 125, 78, 58, 4, 227, 197, 140, 155, 105, 74, 35, 77, 226, 214, 172, 130, 116, 118, 81, 51, 84, 178, 193, 169, 157, 121, 82, 60, 26, 244, 143, 183, 145, 99, 93, 40, 87 }, 218) +
                    UrlObfuscator.decode(new int[] { 157, 107, 91, 104, 2, 234, 152, 236, 134, 44, 85, 33, 45, 25, 248, 200, 253, 220, 124, 22, 35, 23, 231, 211, 182, 134, 63, 94, 32, 10, 232, 248, 178, 154, 108, 21, 122, 91, 180, 141, 252, 135, 47, 84, 94, 44, 26, 249, 207, 224, 157, 119, 84, 35, 24, 241, 221, 166, 223, 99, 76, 60, 2, 224, 199, 163, 135, 111, 98, 42, 0, 233, 198, 172, 149, 59 }, 235) +
                    UrlObfuscator.decode(new int[] { 138, 122, 72, 121, 12, 248, 198, 232, 145, 127, 13, 52, 28, 161, 221, 174, 158, 100, 70, 37, 60, 232, 214, 255, 212, 56 }, 252) +
                    UrlObfuscator.decode(new int[] { 123, 77, 57, 74, 249, 199, 180, 219, 113, 75, 51, 92, 177, 155, 214, 184, 213, 108, 84, 41, 88, 165, 138, 186, 148, 103, 71, 2, 30, 227, 134, 181, 129, 109, 88, 62, 57, 231, 212, 251, 149, 107, 80, 121, 8, 230, 183, 201, 180, 146, 127, 85, 46, 86, 214, 216, 177, 134, 124, 91, 53, 50, 253, 199, 169, 139, 110, 12, 111, 41, 233, 194, 183, 139, 106, 70, 3, 18, 22, 250, 218, 185, 213, 104, 92, 40, 24, 228, 193, 135, 144, 96, 94, 60, 3, 218, 194, 188, 194, 113, 104, 38, 3, 244, 202, 173, 135, 64, 83, 41, 59, 25, 248, 146, 169, 159, 105, 87, 37, 2, 198, 215, 161, 157, 125, 92, 27, 1, 253, 132, 191, 133, 121, 84, 119, 79, 190, 217, 190 }, 269) +
                    UrlObfuscator.decode(new int[] { 99, 20, 103, 6, 182, 205, 170, 130, 115, 28, 111, 14, 239, 210, 177, 155, 109, 69, 100, 14, 163, 210, 181 }, 286) +
                    UrlObfuscator.decode(new int[] { 73, 122, 90, 184, 139 }, 52),
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
                if (rel.isEmpty()) rel = UrlObfuscator.decode(new int[] { 44, 10, 231, 199, 185, 206, 151, 106, 80, 48 }, 69);

                byte[] data = embedCache.get(rel);
                if (data == null) {
                    data = EmbeddedAssets.get(rel);
                    if (data != null) embedCache.put(rel, data);
                }
                if (data == null) return super.shouldInterceptRequest(view, request);

                return new WebResourceResponse(guessEmbeddedMime(rel), UrlObfuscator.decode(new int[] { 3, 33, 210, 158, 234 }, 86), new ByteArrayInputStream(data));
            }

            private String guessEmbeddedMime(String rel) {
                String lower = rel.toLowerCase();
                if (lower.endsWith(UrlObfuscator.decode(new int[] { 73, 238, 209, 169, 143 }, 103)) || lower.endsWith(UrlObfuscator.decode(new int[] { 86, 255, 194, 184 }, 120))) return UrlObfuscator.decode(new int[] { 253, 205, 191, 146, 42, 76, 55, 15, 237 }, 137);
                if (lower.endsWith(UrlObfuscator.decode(new int[] { 180, 218, 171, 132 }, 154))) return UrlObfuscator.decode(new int[] { 223, 175, 145, 124, 8, 37, 22, 247 }, 171);
                if (lower.endsWith(UrlObfuscator.decode(new int[] { 146, 177, 137 }, 188)) || lower.endsWith(UrlObfuscator.decode(new int[] { 227, 129, 97, 89 }, 205))) return UrlObfuscator.decode(new int[] { 191, 141, 108, 87, 51, 26, 249, 195, 191, 154, 122, 28, 56, 16, 230, 206, 189, 142, 126, 66, 58, 29 }, 222);
                if (lower.endsWith(UrlObfuscator.decode(new int[] { 193, 100, 94, 35, 5 }, 239))) return UrlObfuscator.decode(new int[] { 97, 111, 78, 49, 21, 248, 219, 173, 145, 120, 88, 122, 30, 224, 221, 191 }, 256);
                if (lower.endsWith(UrlObfuscator.decode(new int[] { 63, 67, 57, 9 }, 273))) return UrlObfuscator.decode(new int[] { 75, 44, 1, 24, 251, 146, 175, 141, 125, 18, 32, 26, 250 }, 290);
                if (lower.endsWith(UrlObfuscator.decode(new int[] { 22, 39, 24, 242 }, 56))) return UrlObfuscator.decode(new int[] { 32, 5, 230, 193, 160, 203, 115, 76, 38 }, 73);
                if (lower.endsWith(UrlObfuscator.decode(new int[] { 116, 19, 232, 208 }, 90)) || lower.endsWith(UrlObfuscator.decode(new int[] { 69, 224, 217, 173, 128 }, 107))) return UrlObfuscator.decode(new int[] { 21, 246, 219, 190, 157, 56, 92, 37, 17, 244 }, 124);
                if (lower.endsWith(UrlObfuscator.decode(new int[] { 163, 203, 162, 140 }, 141))) return UrlObfuscator.decode(new int[] { 247, 208, 189, 156, 127, 22, 63, 30, 240 }, 158);
                if (lower.endsWith(UrlObfuscator.decode(new int[] { 129, 185, 136, 110, 91 }, 175))) return UrlObfuscator.decode(new int[] { 169, 178, 159, 122, 89, 116, 13, 252, 218, 167 }, 192);
                if (lower.endsWith(UrlObfuscator.decode(new int[] { 255, 135, 96, 72, 43, 94 }, 209))) return UrlObfuscator.decode(new int[] { 132, 110, 78, 75, 113, 10, 243, 221, 188, 203 }, 226);
                if (lower.endsWith(UrlObfuscator.decode(new int[] { 221, 101, 94, 54, 9 }, 243))) return UrlObfuscator.decode(new int[] { 98, 76, 44, 21, 175, 232, 209, 187, 154 }, 260);
                if (lower.endsWith(UrlObfuscator.decode(new int[] { 59, 64, 39, 20 }, 277))) return UrlObfuscator.decode(new int[] { 64, 42, 10, 247, 141, 181, 148, 153 }, 294);
                if (lower.endsWith(UrlObfuscator.decode(new int[] { 18, 50, 25, 246 }, 60))) return UrlObfuscator.decode(new int[] { 36, 1, 234, 205, 172, 199, 127, 11, 44, 7, 236, 204 }, 77);
                return UrlObfuscator.decode(new int[] { 63, 13, 236, 215, 179, 154, 121, 67, 63, 26, 250, 156, 189, 146, 100, 74, 58, 64, 255, 223, 184, 140, 105, 74 }, 94);
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
                popupSettings.setUserAgentString(popupDefaultUA.replace(UrlObfuscator.decode(new int[] { 84, 174, 218, 186 }, 111), "").replaceAll("Version/[0-9.]+\s", ""));
                popupWebView.setVerticalScrollBarEnabled(false);
                popupWebView.setHorizontalScrollBarEnabled(false);

                final Dialog popupDialog = new Dialog(MainActivity.this, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
                // Loading overlay: same bouncing dots + colors as the main
                // navigation overlay, shown over the popup until its page has
                // painted, so there is never an empty black/white screen while
                // a link (sign-in, Telegram, any website) is still loading.
                final FrameLayout popupRoot = new FrameLayout(MainActivity.this);
                popupRoot.setBackgroundColor(Color.parseColor(UrlObfuscator.decode(new int[] { 163, 175, 142, 237, 204, 43, 10 }, 128)));
                popupRoot.addView(popupWebView, new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
                final FrameLayout popupLoader = new FrameLayout(MainActivity.this);
                popupLoader.setBackgroundColor(Color.parseColor(UrlObfuscator.decode(new int[] { 178, 128, 255, 222, 61, 28, 123 }, 145)));
                final BouncingDotsView popupDots = new BouncingDotsView(MainActivity.this, Color.parseColor(UrlObfuscator.decode(new int[] { 129, 135, 210, 185, 44, 120, 25 }, 162)));
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
                        if (request.isForMainFrame()) hidePopupLoader.run();
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
                        if (scheme != null && !scheme.equals(UrlObfuscator.decode(new int[] { 219, 166, 133, 96 }, 179)) && !scheme.equals(UrlObfuscator.decode(new int[] { 172, 151, 118, 81, 51 }, 196))) {
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
        return EMBED_HOST + UrlObfuscator.decode(new int[] { 188, 154, 119, 87, 41, 94, 231, 218, 160, 128 }, 213);
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
        if (Intent.ACTION_SEND.equals(action) && UrlObfuscator.decode(new int[] { 146, 96, 92, 55, 77, 241, 204, 222, 183, 147 }, 230).equals(intent.getType())) {
            String shared = intent.getStringExtra(Intent.EXTRA_TEXT);
            if (shared != null && !shared.isEmpty()) {
                try {
                    return baseUrl() + UrlObfuscator.decode(new int[] { 200, 101, 93, 53, 1, 247, 213, 143, 155, 107, 85, 56, 86 }, 247) + java.net.URLEncoder.encode(shared, UrlObfuscator.decode(new int[] { 93, 115, 0, 72, 188 }, 264));
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

        String shortcutAction = intent.getStringExtra(UrlObfuscator.decode(new int[] { 106, 80, 56, 4, 225, 215, 166, 134, 78, 81, 44, 26, 228, 195, 165 }, 281));
        if (UrlObfuscator.decode(new int[] { 93, 43, 1, 227, 202, 174 }, 47).equals(shortcutAction)) {
            webView.getSettings().setCacheMode(WebSettings.LOAD_NO_CACHE);
            webView.reload();
            webView.getSettings().setCacheMode(WebSettings.LOAD_CACHE_ELSE_NETWORK);
            return;
        }
        if (UrlObfuscator.decode(new int[] { 40, 48, 19, 248 }, 64).equals(shortcutAction)) {
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
                blobTmp = File.createTempFile(UrlObfuscator.decode(new int[] { 51, 28, 224, 204 }, 81), UrlObfuscator.decode(new int[] { 76, 245, 205, 207 }, 98), getCacheDir());
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
                Toast.makeText(MainActivity.this, UrlObfuscator.decode(new int[] { 55, 253, 198, 190, 131, 97, 76, 40, 75, 236, 200, 161, 139, 99, 65, 126, 67, 225, 206, 181, 179, 154, 61, 82, 52, 14, 185, 202, 178, 151, 113, 20, 39, 26, 248, 195, 239, 136, 100, 64, 46 }, 115), Toast.LENGTH_LONG).show();
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
                    sendIntent.setType(UrlObfuscator.decode(new int[] { 240, 198, 186, 149, 47, 111, 82, 60, 21, 245 }, 132));
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
                String safeTitle = (title == null || title.trim().isEmpty()) ? UrlObfuscator.decode(new int[] { 254, 209, 161, 132, 120, 89, 38 }, 149) : title;
                String safeBody = body == null ? "" : body;
                NotificationCompat.Builder notification = new NotificationCompat.Builder(MainActivity.this, UrlObfuscator.decode(new int[] { 194, 160, 130, 98, 87, 45, 20 }, 166))
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
            if (hex == null || !hex.matches(UrlObfuscator.decode(new int[] { 233, 245, 221, 79, 114, 127, 55, 241, 130, 168, 221, 33, 18, 23, 18, 190, 218, 186, 190, 69, 14, 4, 0, 173, 249, 142, 240, 197, 70, 65, 106, 5, 190, 146 }, 183))) return;
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
            if (hex == null || !hex.matches(UrlObfuscator.decode(new int[] { 150, 196, 93, 100, 105, 37, 227, 140, 166, 239, 211, 36, 97, 32, 76, 228, 156 }, 200))) return;
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
            UrlObfuscator.decode(new int[] { 241, 158, 98, 88, 54, 0, 250, 221, 191, 216, 38, 85, 57, 30, 242, 209 }, 217) +
            UrlObfuscator.decode(new int[] { 156, 104, 90, 103, 22, 184, 192, 172, 129, 116, 77, 90, 48, 9, 178, 220, 191, 141, 93, 91, 51, 24, 241, 221, 166, 179, 105, 102, 42, 69, 171, 223, 171, 139, 88, 70, 40, 0, 232, 236, 178, 149, 105, 112, 80, 46, 91, 178, 129 }, 234) +
            UrlObfuscator.decode(new int[] { 146, 124, 17, 121, 7, 234, 201, 164, 221, 122, 88, 52, 11, 235, 195, 176, 151, 43, 89, 102, 4, 234, 196, 183, 144, 78, 72, 51, 43, 80, 254, 211, 181, 142, 120, 81, 57, 5, 189, 147, 186, 129, 60, 81, 44, 26, 228, 218, 174, 205, 32, 1, 53, 3, 241, 209, 177, 140, 58 }, 251) +
            UrlObfuscator.decode(new int[] { 122, 74, 56, 73, 233, 154, 162, 138, 103, 86, 47, 4, 238, 235, 144, 188, 159, 111, 83, 47, 29, 210, 218, 176, 153, 118, 92, 37, 75, 230, 200, 229, 205, 106, 86, 53, 9, 186, 155, 248, 128, 108, 65, 52, 13, 26, 240, 201, 242, 153, 117, 93, 33, 11, 234, 148, 164, 221, 113, 94, 62, 27, 239, 196, 162, 152, 34, 72, 97, 78, 244, 192, 176, 150, 112, 79, 123 }, 268) +
            UrlObfuscator.decode(new int[] { 107, 93, 41, 90, 237, 133, 255, 151, 59, 64, 50, 21, 223, 209, 162, 139, 113, 80, 108, 77, 160, 134, 179, 137, 73, 75, 52, 7, 243, 227, 222, 173, 152, 52, 18, 97 }, 285) +
            UrlObfuscator.decode(new int[] { 90, 52, 89, 228, 142, 243, 208, 43, 66, 36, 25, 253, 211, 225, 195, 34, 87, 99, 92, 189, 184, 202, 184, 132, 111, 91, 43, 29, 246, 145, 243, 210, 103, 19, 108, 77, 168, 221, 168, 128, 110, 73, 61, 79, 161, 128, 228, 133, 45, 75, 50, 35, 16, 240, 201, 185, 149, 110, 124, 60, 30, 226, 212, 182, 159, 119, 24, 34, 10, 250, 216, 190, 133, 49 }, 51) +
            UrlObfuscator.decode(new int[] { 50, 2, 240, 129, 178, 226, 159, 51, 91, 62, 14, 219, 215, 162, 152, 113, 93, 61, 21, 210, 220, 166, 139, 99, 88, 25, 15, 234, 220, 239, 207, 62 }, 68) +
            UrlObfuscator.decode(new int[] { 35, 21, 225, 146, 167, 134, 50, 89, 36, 2, 239, 197, 190, 198, 113, 79, 54, 17, 226, 206, 151, 137, 154, 105, 77, 51, 9, 238, 130, 174, 150, 100, 21, 34, 27, 175, 199, 166, 208, 120, 91, 98, 3, 239, 192, 175, 143, 114, 31, 51, 10, 236, 197, 175, 168, 208, 116, 82, 53, 31, 235, 240, 178, 159, 114, 92, 39, 73 }, 85) +
            UrlObfuscator.decode(new int[] { 16, 228, 214, 227, 146, 96, 68, 2, 111, 75, 167, 205, 187, 139, 56, 83, 51, 25, 224, 210, 239, 193, 43 }, 102) +
            UrlObfuscator.decode(new int[] { 30, 240, 157, 166, 221, 112, 94, 36, 27, 225, 192, 242, 157, 98, 4, 56, 6, 226, 140, 160, 134, 110, 85, 33, 98, 51, 252, 200, 179, 212, 116, 81, 57, 94, 231, 154, 177, 157, 101, 68, 32, 3, 160, 132, 189, 130, 36, 88, 38, 2, 172, 136, 142, 131, 117, 72, 17, 51, 28, 228, 147, 232, 205, 52, 65, 62, 95, 164, 157, 227, 201, 57, 6, 117 }, 119) +
            UrlObfuscator.decode(new int[] { 237, 203, 181, 128, 36, 74, 36, 73, 242, 177, 202, 178, 140, 39, 74, 56, 28, 190, 210, 176, 152, 103, 83, 108, 93, 194, 207, 185, 132, 37, 71, 32, 6, 175, 214, 164, 128, 46, 80, 111, 20, 16, 238, 145, 145, 154, 110, 81, 118, 26, 247, 205, 252, 194, 36, 29, 38, 7, 164, 157, 226, 218, 58, 0, 97, 92 }, 136) +
            UrlObfuscator.decode(new int[] { 240, 222, 255, 146, 112, 88, 39, 19, 184, 203, 185, 143, 127, 12, 56, 9, 180, 201, 233, 133, 105, 75, 48, 7, 242, 212, 151, 249, 211, 127, 84, 52, 13, 253, 217, 162, 217, 58, 71, 51, 19, 192, 206, 160, 136, 96, 7, 100, 6, 248, 211, 175, 138, 106, 80, 111, 2, 239, 241, 202, 184, 146, 111, 22, 2, 28, 246, 194, 180, 217, 96, 81, 35, 31, 227, 194, 224, 143, 100, 68, 61, 9, 238, 200, 160, 150, 94, 5, 104, 91 }, 153) +
            UrlObfuscator.decode(new int[] { 195, 175, 192, 116, 69, 99, 66, 240, 193, 239, 147, 156, 108, 82, 48, 23, 210, 220, 177, 144, 126, 65, 106, 0, 241, 159, 179, 131, 103, 72, 34, 31, 194, 204, 161, 128, 110, 81, 109, 24, 241, 194, 238, 172, 157, 111, 83, 55, 22, 219, 193, 255, 141, 97, 91, 35, 72, 245, 213, 163, 154, 108, 0, 41, 15, 225, 201, 177, 143, 106, 86, 121, 69, 224, 213, 203, 177, 218, 97, 18, 97, 4 }, 170) +
            UrlObfuscator.decode(new int[] { 222, 182, 138, 125, 76, 33, 28, 250, 215, 189, 134, 62, 92, 45, 31, 227, 199, 166, 171, 113, 15, 61, 17, 235, 211, 248, 133, 101, 115, 74, 60, 80, 249, 223, 177, 153, 97, 95, 58, 6, 169, 149, 176, 133, 123, 65, 106, 17, 162, 145, 180, 149 }, 187) +
            UrlObfuscator.decode(new int[] { 177, 136, 107, 93, 43, 15, 174, 192, 237, 152, 127, 92, 105, 119, 87, 166 }, 204), null);
    }

    // Lifts the WebView just enough that the focused text field sits above
    // the keyboard (like a browser scrolling the field into view), and drops
    // it back when the keyboard closes. Does nothing if the field is already
    // visible, so a sheet that fits above the keyboard is not moved at all.
    private void panFocusedFieldAboveKeyboard(final int imeBottom, final int bottomBars) {
        if (webView == null || swipeRefresh == null) return;
        if (imeBottom <= 0) {
            swipeRefresh.animate().translationY(0f).setDuration(120).start();
            if (imeResizesWebView) {
                imeResizesWebView = false;
                applySafeArea(lastSafeLeft, lastSafeTop, lastSafeRight, lastSafeBottomBars, 0);
            }
            return;
        }
        final int kb = Math.max(0, imeBottom - bottomBars);
        webView.evaluateJavascript(
            UrlObfuscator.decode(new int[] { 245, 154, 110, 84, 58, 12, 254, 217, 187, 220, 58, 73, 37, 2, 246, 213, 187, 141, 121, 10, 40, 85, 227, 201, 166, 145, 110, 71, 47, 20, 81, 255, 222, 168, 146, 108, 92, 29, 27, 243, 216, 177, 157, 102, 10, 57, 9, 166, 140, 173, 151, 118, 72, 117, 90, 187, 193, 171, 128, 119, 76, 37, 49, 10, 179, 222, 180, 158, 96, 17, 37, 19, 225, 193, 161, 156, 49, 29, 126, 85 }, 221) +
            UrlObfuscator.decode(new int[] { 152, 108, 94, 107, 30, 180, 128, 166, 200, 113, 69, 36, 44, 224, 205, 218, 162, 129, 59, 28, 115, 87, 236, 216, 154, 154, 99, 86, 32, 50, 241, 220, 171, 197, 37, 16 }, 238) +
            UrlObfuscator.decode(new int[] { 150, 120, 21, 40, 90, 167, 132, 255, 158, 120, 69, 33, 7, 181, 151, 246, 155, 47, 16, 113, 76, 254, 204, 176, 147, 103, 87, 33, 2, 165, 135, 230, 171, 223, 32, 1, 124, 9, 252, 212, 178, 149, 97, 19, 117, 84, 176, 209, 225, 135, 126, 111, 36, 4, 253, 205, 169, 146, 64, 64, 42, 22, 224, 194, 211, 187, 212, 110, 94, 46, 12, 234, 217, 246, 216, 37, 8 }, 255) +
            UrlObfuscator.decode(new int[] { 102, 78, 60, 77, 254, 150, 171, 199, 111, 66, 50, 39, 235, 214, 172, 133, 105, 113, 89, 30, 16, 242, 223, 183, 140, 69, 83, 54, 0, 187, 155, 234, 130, 106, 90, 56, 30, 229, 138, 132, 137, 115, 78, 107, 22, 236, 215, 175, 132, 215, 108, 19, 62, 20, 238, 205, 183, 154, 60, 4, 100, 67, 187, 158, 225, 223, 62, 22, 49, 8, 235, 221, 171, 143, 46, 64, 109, 24, 240, 196, 180, 170, 140, 115, 28, 118, 75, 162, 197, 170, 223, 61, 29, 104 }, 272),
            value -> {
                try {
                    double bottomCss = Double.parseDouble(value == null ? "-1" : value.trim());
                    if (bottomCss < 0) return;
                    float density = getResources().getDisplayMetrics().density;
                    int visibleBottomPx = swipeRefresh.getHeight() - kb;
                    int padPx = (int) (24 * density);
                    int overlapPx = (int) (bottomCss * density) + padPx - visibleBottomPx;
                    if (overlapPx > 0) {
                        // Field is under the keyboard: shrink the WebView so the page
                        // re-lays out above the keyboard (header stays put, input rides
                        // on top of the keyboard) rather than sliding everything up.
                        swipeRefresh.animate().translationY(0f).setDuration(0).start();
                        if (!imeResizesWebView) {
                            imeResizesWebView = true;
                            applySafeArea(lastSafeLeft, lastSafeTop, lastSafeRight, lastSafeBottomBars, lastSafeIme);
                        }
                    } else if (!imeResizesWebView) {
                        swipeRefresh.animate().translationY(0f).setDuration(120).start();
                    }
                } catch (Exception ignored) {}
            });
    }

    // Insets the WebView by the system bars / display cutout / keyboard so
    // page content always sits inside the safe area, and sizes the
    // status-bar and navigation-bar strips. Bottom is whichever is taller of
    // the navigation bar and the keyboard.
    private void applySafeArea(int left, int top, int right, int bottomBars, int imeBottom) {
        if (swipeRefresh == null) return;
        lastSafeLeft = left; lastSafeTop = top; lastSafeRight = right; lastSafeBottomBars = bottomBars; lastSafeIme = imeBottom;
        // The keyboard no longer shrinks the WebView. Resizing it made the
        // page's layout viewport (100vh/100dvh) collapse to the strip above
        // the keyboard, so bottom sheets/dialogs stretched to fill the whole
        // screen -- much taller than in a mobile browser, where the keyboard
        // simply overlays the page. Instead the keyboard overlays the page
        // and panFocusedFieldAboveKeyboard() lifts the view only as far as
        // needed to keep the focused field visible.
        int bottom = (imeResizesWebView && imeBottom > 0) ? Math.max(bottomBars, imeBottom) : bottomBars;
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
        if (t.isEmpty() || UrlObfuscator.decode(new int[] { 79, 53, 51, 18 }, 289).equals(t)) return null;
        try {
            if (t.startsWith("#")) return Color.parseColor(t) | 0xFF000000;
            if (!t.startsWith(UrlObfuscator.decode(new int[] { 69, 49, 23 }, 55))) return null;
            String inner = t.substring(t.indexOf('(') + 1);
            String[] parts = inner.split(UrlObfuscator.decode(new int[] { 19, 57, 182, 136, 253, 205, 95, 10 }, 72));
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
        int scrim = Color.parseColor(UrlObfuscator.decode(new int[] { 122, 73, 163, 132, 225, 193, 80 }, 89));
        try {
            if (topScrim != null && topScrim.getBackground() instanceof ColorDrawable) {
                scrim = ((ColorDrawable) topScrim.getBackground()).getColor() | 0xFF000000;
            }
        } catch (Exception ignored) {}
        Integer bgC = o == null ? null : parseCssColor(o.optString("bg", null));
        Integer thC = o == null ? null : parseCssColor(o.optString(UrlObfuscator.decode(new int[] { 30, 225, 205, 170, 131 }, 106), null));
        Integer btC = o == null ? null : parseCssColor(o.optString(UrlObfuscator.decode(new int[] { 25, 238, 215 }, 123), null));
        double btnR = o == null ? -1 : o.optDouble(UrlObfuscator.decode(new int[] { 238, 223, 164, 187 }, 140), -1);

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
                accent = dark ? Color.parseColor(UrlObfuscator.decode(new int[] { 190, 250, 233, 188, 43, 126, 96 }, 157)) : Color.parseColor(UrlObfuscator.decode(new int[] { 141, 252, 219, 58, 29, 120, 43 }, 174));
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
        p.title = cardDark ? Color.WHITE : Color.parseColor(UrlObfuscator.decode(new int[] { 156, 239, 201, 45, 15, 107, 56 }, 191));
        p.sub = mixColor(p.title, p.cardTop, 0.35f);
        p.border = mixColor(accent, p.cardTop, 0.45f);
        p.accentTop = mixColor(accent, Color.WHITE, 0.12f);
        p.accentBottom = mixColor(accent, Color.BLACK, 0.12f);
        p.accentText = lumOf(accent) < 0.55 ? Color.WHITE : Color.parseColor(UrlObfuscator.decode(new int[] { 243, 222, 58, 28, 120, 90, 203 }, 208));
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
        title.setText(UrlObfuscator.decode(new int[] { 173, 101, 126, 72, 52, 18, 252, 154, 184, 148, 101, 83, 52, 16, 234, 141 }, 225));
        title.setTextColor(pal.title);
        title.setTextSize(18f);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setGravity(Gravity.CENTER);
        card.addView(title, new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        TextView msg = new TextView(this);
        msg.setText(UrlObfuscator.decode(new int[] { 179, 99, 85, 111, 23, 226, 217, 235, 153, 124, 90, 34, 70, 252, 203, 182, 194, 118, 65, 81, 42, 93, 232, 212, 250, 156, 96, 94, 34, 74 }, 242));
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
        exit.setText(UrlObfuscator.decode(new int[] { 70, 122, 8, 52 }, 259));
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
        stay.setText(UrlObfuscator.decode(new int[] { 71, 103, 19, 40 }, 276));
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
            boolean chat = h.equals(UrlObfuscator.decode(new int[] { 81, 106, 14, 231 }, 293)) || h.endsWith(UrlObfuscator.decode(new int[] { 21, 46, 87, 245, 210 }, 59))
                || h.equals(UrlObfuscator.decode(new int[] { 56, 14, 230, 204, 175, 149, 103, 72, 106, 14, 231 }, 76)) || h.equals(UrlObfuscator.decode(new int[] { 41, 25, 247, 223, 190, 138, 118, 91, 123, 16, 252, 213 }, 93))
                || h.equals(UrlObfuscator.decode(new int[] { 25, 236, 130, 166, 143 }, 110)) || h.equals(UrlObfuscator.decode(new int[] { 30, 238, 212, 242, 140, 114, 88, 44, 4, 247, 197, 164, 221, 113, 94, 61 }, 127));
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
        if (scheme.equals(UrlObfuscator.decode(new int[] { 248, 219, 186, 157 }, 144)) || scheme.equals(UrlObfuscator.decode(new int[] { 201, 180, 171, 142, 110 }, 161)) || scheme.equals(UrlObfuscator.decode(new int[] { 212, 184, 156, 106 }, 178))
                || scheme.equals(UrlObfuscator.decode(new int[] { 162, 128, 110, 85, 75 }, 195)) || scheme.equals(UrlObfuscator.decode(new int[] { 176, 146, 102, 80 }, 212)) || scheme.equals(UrlObfuscator.decode(new int[] { 135, 104, 76, 32 }, 229))
                || scheme.equals(UrlObfuscator.decode(new int[] { 156, 116, 66, 50, 1, 242, 194, 166, 158, 121 }, 246)) || scheme.equals(UrlObfuscator.decode(new int[] { 100, 73, 43, 16, 230, 204, 181 }, 263))) {
            return false;
        }
        try {
            Intent intent;
            if (scheme.equals(UrlObfuscator.decode(new int[] { 113, 89, 34, 16, 250, 199 }, 280))) {
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
                if (scheme.equals(UrlObfuscator.decode(new int[] { 64, 38, 19, 227, 203, 176 }, 297))) {
                    Intent parsed = Intent.parseUri(uri.toString(), Intent.URI_INTENT_SCHEME);
                    String fallback = parsed.getStringExtra(UrlObfuscator.decode(new int[] { 93, 44, 18, 235, 200, 191, 139, 71, 81, 55, 25, 248, 209, 179, 146, 123, 112, 59, 31, 224 }, 63));
                    if (fallback != null && (fallback.startsWith(UrlObfuscator.decode(new int[] { 56, 27, 250, 221, 191, 209, 37, 6 }, 80)) || fallback.startsWith(UrlObfuscator.decode(new int[] { 9, 244, 235, 206, 231, 211, 52 }, 97)))) {
                        view.loadUrl(fallback);
                        return true;
                    }
                }
            } catch (Exception ignored) {}
            Toast.makeText(this, UrlObfuscator.decode(new int[] { 60, 254, 144, 174, 158, 125, 12, 45, 5, 252, 198, 163, 198, 113, 75, 99, 13, 241, 197, 209, 254, 137, 116, 82, 41, 89, 244, 222, 184, 158 }, 114), Toast.LENGTH_SHORT).show();
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
        String name = raw == null ? "" : raw.replaceAll("[\\\/:*?\"<>|]", "_").trim();
        if (name.length() > 120) name = name.substring(name.length() - 120);
        if (name.isEmpty() || name.equals(".") || name.equals("..")) name = UrlObfuscator.decode(new int[] { 231, 205, 182, 142, 147, 113, 92, 56 }, 131);
        if (name.lastIndexOf('.') <= 0 && mime != null && !mime.isEmpty()) {
            String ext = android.webkit.MimeTypeMap.getSingleton().getExtensionFromMimeType(mime);
            if (ext != null) name = name + "." + ext;
        }
        return name;
    }

    private void handleInlineDownload(String url, String contentDisposition, String mimeType) {
        try {
            String named = downloadNames.get(downloadKey(url));
            if (url.startsWith(UrlObfuscator.decode(new int[] { 240, 210, 166, 144, 42 }, 148))) {
                int comma = url.indexOf(',');
                if (comma < 0) throw new IllegalArgumentException(UrlObfuscator.decode(new int[] { 199, 165, 135, 34, 69, 33, 43, 31, 189, 233, 137, 182 }, 165));
                String meta = url.substring(5, comma);
                String payload = url.substring(comma + 1);
                String mime = meta.split(";")[0];
                if (mime.isEmpty()) mime = (mimeType != null && !mimeType.isEmpty()) ? mimeType : UrlObfuscator.decode(new int[] { 194, 176, 140, 103, 29, 33, 28, 238, 199, 163 }, 182);
                byte[] bytes;
                if (meta.contains(UrlObfuscator.decode(new int[] { 252, 132, 100, 87, 38, 84, 181 }, 199))) {
                    if (payload.indexOf('%') >= 0) payload = Uri.decode(payload);
                    bytes = android.util.Base64.decode(payload, android.util.Base64.DEFAULT);
                } else {
                    bytes = Uri.decode(payload).getBytes(UrlObfuscator.decode(new int[] { 141, 163, 80, 24, 108 }, 216));
                }
                String name = cleanDownloadName(named != null ? named : URLUtil.guessFileName(url, contentDisposition, mime), mime);
                File tmp = File.createTempFile("dl", UrlObfuscator.decode(new int[] { 199, 124, 74, 54 }, 233), getCacheDir());
                java.io.FileOutputStream fos = new java.io.FileOutputStream(tmp);
                try { fos.write(bytes); } finally { fos.close(); }
                finishInlineSave(tmp, name, mime);
                return;
            }
            // blob: -- read it inside the page and stream it back in chunks.
            String mime = (mimeType != null && !mimeType.isEmpty()) ? mimeType : "";
            String name = cleanDownloadName(named != null ? named : URLUtil.guessFileName(url, contentDisposition, mime), mime);
            Toast.makeText(this, UrlObfuscator.decode(new int[] { 190, 118, 79, 57, 26, 250, 213, 183, 155, 127, 87, 111 }, 250) + name + "\u2026", Toast.LENGTH_SHORT).show();
            final String js = UrlObfuscator.decode(new int[] { 35, 76, 60, 6, 228, 210, 172, 139, 109, 10, 52, 76, 17, 178, 208, 245, 128, 110, 75, 33, 12 }, 267)
                + UrlObfuscator.decode(new int[] { 106, 90, 40, 89, 224, 138, 184, 144, 99, 19, 10, 60, 220, 231, 186, 153, 124, 121, 47, 24, 253, 194, 181, 145, 44, 10, 121, 25, 174, 240, 206, 184, 146, 51, 29, 30, 61, 195, 145, 249, 129, 63, 70, 35, 5, 234, 135, 246, 148, 37, 88, 44, 27, 247, 201, 171, 151, 102, 118, 56, 16, 26, 163, 154, 190, 151, 117, 91, 127, 76 }, 284)
                + UrlObfuscator.decode(new int[] { 74, 127, 31, 225, 203, 191, 158, 100, 88, 116, 14, 242, 200, 166, 144, 106, 77, 47, 72, 86, 229, 252, 178, 159, 104, 86, 49, 19, 212, 199, 189, 151, 117, 84, 126, 13, 226, 194, 174, 184, 107, 95, 45, 33, 231, 204, 168, 134, 102, 9, 105, 100, 3, 166 }, 50)
                + UrlObfuscator.decode(new int[] { 59, 76, 238, 206, 211, 177, 156, 120, 6, 60, 12, 246, 212, 162, 156, 123, 93, 122, 88, 235, 219, 188, 148, 119, 93, 43, 27, 168, 197, 251, 157, 42, 81, 39, 18, 240, 240, 208, 174, 153, 55, 121, 17, 69, 160, 142, 227, 192, 32, 0, 125, 31, 233, 200, 240, 220, 48 }, 67)
                + UrlObfuscator.decode(new int[] { 21, 29, 246, 195, 191, 134, 106, 111, 62, 2, 238, 206, 173, 201, 100, 73, 43, 1, 209, 192, 182, 186, 188, 120, 91, 50, 20, 177, 214, 251, 155, 105, 72, 49, 92, 229, 201, 191, 139, 113, 80, 108, 77, 160, 147 }, 84)
                + UrlObfuscator.decode(new int[] { 77, 226, 214, 172, 130, 116, 118, 81, 51, 92, 245, 223, 161, 140, 63, 31, 46, 29, 245, 154, 190, 150, 105, 16, 112, 14, 165, 217, 160, 146, 98, 15, 62, 37, 237, 198, 179, 143, 150, 122, 127, 46, 18, 254, 222, 189, 217, 116, 89, 59, 17, 193, 208, 166, 138, 75, 67, 40, 67, 163, 146, 186, 130, 114, 80, 54, 13, 185, 220 }, 101)
                + UrlObfuscator.decode(new int[] { 0, 244, 198, 243, 128, 44, 94, 42, 25, 173, 234, 162, 134, 108, 122, 34, 7, 225, 193, 177, 202, 40, 27 }, 118)
                + UrlObfuscator.decode(new int[] { 245, 136, 170, 138, 111, 77, 32, 4, 66, 248, 200, 178, 152, 110, 80, 55, 25, 190, 156, 175, 133, 115, 67, 112, 28, 179, 254, 184, 153, 99, 71, 47, 79, 244, 139, 182, 134, 113, 84, 44, 43, 87, 166, 253, 181, 158, 107, 87, 62, 18, 215, 198, 186, 150, 118, 85, 97, 12, 225, 195, 169, 185, 104, 94, 34, 37, 237, 209, 173, 137, 41, 83, 17, 45, 8, 254, 200, 174, 139, 113, 89, 49, 93, 231, 157, 187, 159, 116, 74, 54, 34, 234, 131, 237, 197, 47, 14, 109, 84, 173, 138, 249, 142, 102, 121, 21, 96, 63, 211, 129, 183, 157, 111, 66, 125, 93, 168, 207, 234 }, 135)
                + UrlObfuscator.decode(new int[] { 234, 153, 185, 155, 113, 65, 32, 30, 226, 146, 168, 152, 98, 72, 62, 0, 231, 201, 238, 204, 127, 98, 44, 5, 242, 240, 215, 185, 190, 105, 83, 61, 31, 242, 152, 183, 152, 124, 80, 2, 17, 249, 203, 139, 141, 98, 70, 44, 12, 175, 143, 254, 153, 56 }, 152)
                + UrlObfuscator.decode(new int[] { 219, 230, 149, 99, 68, 32, 34, 241, 229, 161, 171, 159, 72, 110, 23, 82, 251, 150, 164, 154, 124, 87, 54, 90, 254, 214, 169, 194, 98, 74, 45, 65, 202, 224, 238, 207, 62, 89, 106, 74, 168, 155 }, 169)
                + UrlObfuscator.decode(new int[] { 199, 186, 153, 99, 85, 61, 92, 246, 155, 170, 177, 97, 74, 63, 3, 226, 206, 139, 154, 110, 66, 34, 1, 173, 192, 173, 143, 157, 77, 92, 42, 30, 220, 216, 177, 155, 115, 81, 124, 90, 169, 204, 173, 212 }, 186)
                + UrlObfuscator.decode(new int[] { 179, 196, 122, 77, 41, 2, 173, 141, 248 }, 203)
                + UrlObfuscator.decode(new int[] { 161, 152, 123, 77, 59, 31, 190, 208, 253, 136, 102, 67, 41, 20, 207, 195, 168, 153, 101, 64, 44, 37, 244, 204, 160, 132, 103, 15, 34, 51, 17, 255, 239, 186, 140, 124, 126, 54, 31, 249, 209, 183, 218, 56, 11, 50, 13, 236, 216, 168, 130, 33, 119, 110, 29, 248, 217, 190, 203, 41 }, 220)
                + org.json.JSONObject.quote(url) + "," + org.json.JSONObject.quote(name) + "," + org.json.JSONObject.quote(mime) + ");";
            webView.evaluateJavascript(js, null);
        } catch (Exception e) {
            Toast.makeText(this, UrlObfuscator.decode(new int[] { 169, 99, 92, 36, 5, 231, 198, 162, 197, 98, 66, 43, 13, 229, 251, 132, 253, 159, 116, 79, 53, 28, 183, 216, 186, 128, 51, 64, 52, 17, 235, 142, 185, 132, 98, 89, 105, 14, 238, 202, 160 }, 237), Toast.LENGTH_LONG).show();
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
                cv.put(android.provider.MediaStore.MediaColumns.MIME_TYPE, (mime == null || mime.isEmpty()) ? UrlObfuscator.decode(new int[] { 159, 109, 76, 55, 19, 250, 217, 163, 159, 122, 90, 124, 29, 242, 196, 170, 154, 32, 95, 63, 24, 236, 201, 170 }, 254) : mime);
                cv.put(android.provider.MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + UrlObfuscator.decode(new int[] { 32, 69, 40, 30, 253, 195, 160, 129 }, 271));
                cv.put(android.provider.MediaStore.MediaColumns.IS_PENDING, 1);
                android.content.ContentResolver cr = getContentResolver();
                Uri dest = cr.insert(android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI, cv);
                if (dest == null) throw new java.io.IOException(UrlObfuscator.decode(new int[] { 109, 90, 58, 20, 253, 232, 174, 150, 106, 82, 118, 28, 250, 192, 183, 131, 100, 15, 40, 12, 229, 199, 175, 141 }, 288));
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
                File dir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), UrlObfuscator.decode(new int[] { 93, 48, 6, 229, 219, 184, 153 }, 54));
                if (!dir.exists() && !dir.mkdirs()) throw new java.io.IOException(UrlObfuscator.decode(new int[] { 42, 13, 225, 205, 177, 145, 33, 70, 94, 55, 17, 249, 223 }, 71));
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
                ? shown + " downloaded \u2014 find it in your kerviii folder"
                : UrlObfuscator.decode(new int[] { 28, 24, 225, 219, 184, 156, 115, 85, 112, 9, 239, 196, 160, 142, 110, 19, 104, 4, 233, 208, 168, 135, 34, 79, 47, 43, 94, 238, 221, 173, 159, 57 }, 88) + shown, Toast.LENGTH_LONG).show();
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
    // its own "kerviii" subfolder in there (DownloadManager
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
        if (url != null && (url.startsWith(UrlObfuscator.decode(new int[] { 11, 228, 200, 164, 223 }, 105)) || url.startsWith(UrlObfuscator.decode(new int[] { 30, 248, 204, 182, 204 }, 122)))) {
            handleInlineDownload(url, contentDisposition, mimeType);
            return;
        }

        final String filename = URLUtil.guessFileName(url, contentDisposition, mimeType);
        final String cookie = CookieManager.getInstance().getCookie(url);

        try {
            DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
            request.addRequestHeader(UrlObfuscator.decode(new int[] { 222, 217, 172, 154, 42, 103, 34, 1, 237, 214 }, 139), userAgent);
            if (cookie != null) request.addRequestHeader(UrlObfuscator.decode(new int[] { 223, 212, 181, 146, 113, 82 }, 156), cookie);
            request.setTitle(filename);
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
            request.setDestinationInExternalPublicDir(
                Environment.DIRECTORY_DOWNLOADS, UrlObfuscator.decode(new int[] { 198, 169, 153, 124, 64, 33, 14, 169 }, 173) + filename);
            request.setAllowedOverMetered(true);
            request.setAllowedOverRoaming(true);
            request.setVisibleInDownloadsUi(true);

            DownloadManager downloadManager = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
            pendingDownloadId = downloadManager.enqueue(request);
            startDownloadProgressPolling(pendingDownloadId);
            Toast.makeText(this, UrlObfuscator.decode(new int[] { 250, 178, 139, 117, 86, 54, 25, 243, 223, 187, 147, 51 }, 190) + filename + "…", Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Toast.makeText(this, UrlObfuscator.decode(new int[] { 139, 129, 122, 66, 39, 5, 232, 204, 231, 128, 100, 77, 47, 7, 229, 128, 146, 243, 221, 127, 83, 63, 26, 243, 151, 175, 154, 97, 65, 114, 18, 255, 193, 160, 136, 111, 95, 35, 6, 230, 135, 167, 139, 96, 3, 54, 19, 249, 191, 223, 186, 157, 114, 84 }, 207), Toast.LENGTH_LONG).show();
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
            String title = (titleIdx >= 0 && cursor.getString(titleIdx) != null) ? cursor.getString(titleIdx) : UrlObfuscator.decode(new int[] { 166, 150, 114, 88 }, 224);

            if (status == DownloadManager.STATUS_SUCCESSFUL) {
                String localUriStr = uriIdx >= 0 ? cursor.getString(uriIdx) : null;
                String folderName = UrlObfuscator.decode(new int[] { 181, 127, 88, 32, 1, 227, 202, 174, 154 }, 241);
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
                Toast.makeText(this, title + UrlObfuscator.decode(new int[] { 34, 69, 47, 40, 16, 241, 211, 186, 158, 124, 92, 119, 8290, 181, 210, 186, 156, 117, 16, 38, 26, 173, 197, 165, 202, 112, 71, 50, 20, 165 }, 258) + folderName + UrlObfuscator.decode(new int[] { 51, 84, 62, 28, 235, 203, 191 }, 275), Toast.LENGTH_LONG).show();
                notifyDownloadResult(true);

                // If what just finished downloading is itself an installable
                // Android package, go straight to the system installer
                // instead of leaving the user to dig it out of the Downloads
                // folder and tap it themselves. Gated strictly on the file
                // actually being a .apk -- every other kind of download this
                // wrapper handles (images, PDFs, whatever the wrapped site
                // links to) still behaves exactly as before, since "install"
                // wouldn't mean anything for those.
                if (localPath != null && title != null && title.toLowerCase().endsWith(UrlObfuscator.decode(new int[] { 10, 34, 18, 234 }, 292))) {
                    File apkFile = new File(localPath);
                    if (apkFile.exists()) {
                        try {
                            Uri contentUri = FileProvider.getUriForFile(
                                this, getPackageName() + UrlObfuscator.decode(new int[] { 20, 63, 17, 251, 211, 165, 134, 124, 68, 56, 20, 234, 220 }, 58), apkFile);
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
                Toast.makeText(this, UrlObfuscator.decode(new int[] { 15, 5, 254, 198, 171, 137, 100, 64, 99, 4, 224, 201, 211, 187, 153, 38, 27 }, 75) + title + UrlObfuscator.decode(new int[] { 124, 83, 255, 203, 170, 152, 100, 21 }, 92) + reason + ")", Toast.LENGTH_LONG).show();
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
                    UrlObfuscator.decode(new int[] { 25, 254, 210, 177, 128, 110, 15, 49, 12, 234, 199, 173, 150, 46, 64, 97, 50, 18, 213, 219, 173, 145, 97, 83, 17, 27, 228, 220, 189, 159, 110, 74, 14, 3, 230, 218, 165, 141, 115, 67, 108, 31, 244, 203, 175, 132, 144, 105, 19, 3, 36, 245, 215, 150, 150, 98, 92, 34, 22, 214, 222, 167, 129, 98, 66, 45, 15, 201, 198, 165, 151, 106, 64, 48, 6, 170 }, 109) + success + UrlObfuscator.decode(new int[] { 87, 166, 193, 166, 153, 120, 76, 52, 30, 189, 209, 250, 137, 108 }, 126),
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
                UrlObfuscator.decode(new int[] { 251, 220, 180, 151, 98, 76, 97, 31, 238, 200, 161, 139, 116, 12, 30, 63, 16, 240, 243, 189, 143, 115, 79, 61, 51, 249, 194, 186, 159, 125, 80, 52, 63, 252, 194, 171, 153, 111, 90, 59, 78, 253, 210, 173, 141, 102, 78, 55, 113, 33, 194, 211, 181, 180, 120, 76, 62, 0, 240, 240, 188, 133, 127, 92, 32, 15, 233, 252, 185, 133, 110, 90, 34, 21, 246, 140 }, 143) + done + "," + total + "," + status + UrlObfuscator.decode(new int[] { 137, 132, 163, 128, 127, 90, 46, 26, 240, 159, 179, 220, 111, 78 }, 160),
                null);
        } catch (Exception ignored) {
        }
    }

    private void notifyDownloadCancelled() {
        try {
            webView.evaluateJavascript(
                UrlObfuscator.decode(new int[] { 197, 162, 150, 117, 68, 42, 67, 253, 192, 166, 131, 105, 82, 106, 60, 221, 206, 174, 145, 159, 105, 85, 45, 31, 221, 215, 160, 152, 121, 91, 50, 22, 210, 223, 162, 158, 97, 73, 63, 15, 160, 211, 176, 143, 107, 64, 44, 21, 175, 255, 224, 177, 147, 82, 90, 46, 16, 238, 210, 146, 154, 99, 93, 62, 30, 241, 203, 141, 130, 97, 91, 38, 12, 252, 194, 238, 131, 101, 79, 49, 4, 172, 184, 221, 188, 146, 120, 95, 53, 20, 242, 210, 242, 221, 40, 79, 44, 19, 238, 218, 174, 132, 35, 79, 96, 19, 250 }, 177),
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
            Toast.makeText(this, UrlObfuscator.decode(new int[] { 131, 141, 108, 112, 73, 125, 21, 245, 201, 173, 153, 123, 90, 38, 84, 245, 192, 190, 157, 47, 90, 37, 5, 248, 138, 168, 152, 119, 10, 101, 16, 235, 199, 175, 192, 150, 106, 26, 48, 23, 186, 218, 183, 153, 98, 92, 58, 6, 247, 145, 177, 154, 122, 66, 33, 10, 254, 192, 171, 134, 106, 73, 61 }, 194), Toast.LENGTH_LONG).show();
            try {
                Intent settingsIntent = new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                    Uri.parse(UrlObfuscator.decode(new int[] { 163, 147, 114, 91, 46, 9, 232, 150 }, 211) + getPackageName()));
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
        installIntent.setDataAndType(contentUri, UrlObfuscator.decode(new int[] { 133, 115, 82, 45, 9, 28, 255, 201, 181, 148, 116, 22, 46, 25, 242, 155, 181, 157, 118, 67, 63, 6, 234, 131, 188, 138, 105, 66, 41, 0, 227, 136, 165, 145, 97, 73, 41, 41, 27 }, 228));
        installIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_GRANT_READ_URI_PERMISSION);
        try {
            startActivity(installIntent);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, UrlObfuscator.decode(new int[] { 187, 123, 19, 59, 31, 227, 219, 175, 129, 96, 78, 56, 73, 238, 200, 179, 139, 96, 3, 111, 76, 160, 240, 206, 184, 146, 59, 78, 49, 29, 183, 208, 188, 152, 118, 18, 55, 2, 224, 195, 237, 149, 100, 95, 59, 72, 195, 201, 178, 138, 111, 77, 32, 4, 12, 190, 219, 179, 151, 126, 92, 42, 87, 255, 219, 167, 135, 119, 80, 52 }, 245), Toast.LENGTH_LONG).show();
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
                Toast.makeText(this, UrlObfuscator.decode(new int[] { 85, 81, 43, 17, 227, 198, 165, 255, 142, 120, 78, 54, 19, 234, 203, 190, 153, 123, 20, 58, 1, 177, 222, 170, 139, 105, 73, 47, 74, 253, 199, 231, 149, 100, 82, 38, 66, 245, 200, 218, 254, 153, 115, 76, 52, 21, 247, 214, 178 }, 262), Toast.LENGTH_LONG).show();
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
                Toast.makeText(this, UrlObfuscator.decode(new int[] { 84, 87, 56, 17, 225, 211, 254, 157, 102, 77, 63, 3, 251, 194, 166, 134, 98, 6, 53, 1, 241, 207, 168, 147, 140, 119, 82, 50, 91, 243, 202, 248, 153, 115, 80, 48, 22, 246, 145, 182, 128, 124, 13, 56, 3, 227, 218 }, 279), Toast.LENGTH_LONG).show();
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
                Toast.makeText(this, UrlObfuscator.decode(new int[] { 100, 40, 5, 228, 208, 170, 141, 111, 0, 79, 59, 15, 241, 210, 169, 138, 113, 88, 56, 85, 253, 192, 242, 159, 117, 74, 42, 8, 232, 139, 172, 134, 122, 7, 50, 13, 237, 208 }, 296), Toast.LENGTH_LONG).show();
            }
        } else if (requestCode == NOTIFICATION_PERMISSION_REQUEST_CODE) {
            if (!(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                Toast.makeText(this, UrlObfuscator.decode(new int[] { 112, 50, 8, 242, 220, 176, 155, 118, 66, 60, 27, 253, 193, 241, 145, 125, 75, 109, 3, 237, 204, 233, 142, 104, 84, 101, 16, 235, 203, 178, 192, 158, 110, 77, 124, 86, 183, 153, 172, 130, 100, 91, 116, 7, 250, 212, 189, 207, 97, 67, 108, 2, 228, 137, 155, 130, 114, 81, 45, 13, 229, 210, 224, 190, 144, 100, 28, 47, 19, 244, 221 }, 62), Toast.LENGTH_LONG).show();
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
            UrlObfuscator.decode(new int[] { 43, 11, 235, 205, 190, 134, 125 }, 79), UrlObfuscator.decode(new int[] { 39, 26, 240, 216, 174, 154, 118 }, 96), NotificationManager.IMPORTANCE_DEFAULT);
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
                Toast.makeText(this, UrlObfuscator.decode(new int[] { 56, 254, 220, 186, 140, 96, 71, 106, 25, 237, 213, 171, 140, 119, 80, 43, 14, 238, 191, 201, 188, 143, 117, 29, 45, 88, 240, 196, 180, 154, 103, 87, 53, 80, 162, 131, 237, 131, 123, 79, 39, 72, 243, 206, 160, 196, 101, 75, 45, 5, 95, 248, 207, 179, 150, 58, 64, 55, 2, 228, 149, 144, 156, 101, 95, 60, 0, 239, 201, 191, 203, 108, 70, 36, 3, 227, 215, 228, 151, 109, 1, 41, 49, 13, 233, 221, 183, 150, 57, 85, 54, 24, 224, 213, 191, 158, 104 }, 113), Toast.LENGTH_LONG).show();
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
