package com.ryomd.nativeapp.youtube;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;

import com.google.android.gms.auth.api.identity.AuthorizationRequest;
import com.google.android.gms.auth.api.identity.AuthorizationResult;
import com.google.android.gms.auth.api.identity.Identity;
import com.google.android.gms.common.api.Scope;

import org.json.JSONObject;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Native Google OAuth authorization and YouTube Data API bridge.
 * Access tokens remain in memory only; the app never asks for or stores Google passwords.
 */
public final class RyomdYouTubeAccountBridge {
    public static final int REQUEST_CODE = 4826;
    private static final String YOUTUBE_READONLY = "https://www.googleapis.com/auth/youtube.readonly";
    private static final String USERINFO_URL = "https://www.googleapis.com/oauth2/v3/userinfo";
    private static final String YOUTUBE_API = "https://www.googleapis.com/youtube/v3";

    private final Activity activity;
    private final WebView webView;
    private final Handler main = new Handler(Looper.getMainLooper());
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final OkHttpClient http = new OkHttpClient.Builder().build();
    private volatile String accessToken = "";

    public RyomdYouTubeAccountBridge(Activity activity, WebView webView) {
        this.activity = activity;
        this.webView = webView;
    }

    @JavascriptInterface
    public boolean isAvailable() { return true; }

    /** Starts Google's native account/consent screen. */
    @JavascriptInterface
    public void connect() {
        main.post(() -> {
            try {
                List<Scope> scopes = Arrays.asList(
                        new Scope("openid"),
                        new Scope("email"),
                        new Scope("profile"),
                        new Scope(YOUTUBE_READONLY)
                );
                AuthorizationRequest request = AuthorizationRequest.builder()
                        .setRequestedScopes(scopes)
                        .build();

                Identity.getAuthorizationClient(activity).authorize(request)
                        .addOnSuccessListener(result -> handleAuthorizationResult(result))
                        .addOnFailureListener(error -> emitError(describeAuthFailure(error)));
            } catch (Exception error) {
                emitError("Layanan otorisasi Google tidak tersedia: " + safeMessage(error));
            }
        });
    }

    private void handleAuthorizationResult(AuthorizationResult result) {
        if (result == null) {
            emitError("Google tidak mengembalikan hasil otorisasi.");
            return;
        }
        String token = result.getAccessToken();
        if (token != null && !token.trim().isEmpty()) {
            acceptAccessToken(token);
            return;
        }
        PendingIntent pending = result.getPendingIntent();
        if (pending == null) {
            emitError("Google belum memberikan izin akses YouTube. Coba sambungkan akun lagi.");
            return;
        }
        try {
            activity.startIntentSenderForResult(
                    pending.getIntentSender(), REQUEST_CODE, null, 0, 0, 0
            );
        } catch (Exception error) {
            emitError("Tidak dapat membuka layar persetujuan Google: " + safeMessage(error));
        }
    }

    /** Called by MainActivity before its file-picker result handling. */
    public boolean handleActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode != REQUEST_CODE) return false;

        // Google can return an ApiException/status inside the result Intent even when the
        // Activity result code is not RESULT_OK. Parse that payload first so configuration
        // errors are not incorrectly reported as a user cancellation.
        if (data == null) {
            if (resultCode == Activity.RESULT_CANCELED) {
                emitError("Layar persetujuan Google ditutup sebelum hasil izin diterima. "
                        + "Jika Anda tidak menekan Batal/Kembali, periksa Google Play Services, "
                        + "OAuth Android client, package com.ryomd.nativeapp, dan SHA-1 signing WZH.");
            } else {
                emitError("Google tidak mengirim data hasil otorisasi (resultCode=" + resultCode + "). "
                        + "Coba lagi dan pastikan Google Play Services aktif.");
            }
            return true;
        }

        try {
            AuthorizationResult result = Identity.getAuthorizationClient(activity)
                    .getAuthorizationResultFromIntent(data);
            String token = result == null ? null : result.getAccessToken();
            if (token == null || token.trim().isEmpty()) {
                List<String> granted = result == null ? null : result.getGrantedScopes();
                if (granted != null && !granted.contains(YOUTUBE_READONLY)) {
                    emitError("Izin YouTube belum disetujui. Pilih akun Google lalu izinkan akses baca YouTube.");
                } else if (resultCode == Activity.RESULT_CANCELED) {
                    emitError("Google menyelesaikan alur tanpa token akses. Periksa izin YouTube dan konfigurasi OAuth Android.");
                } else {
                    emitError("Google tidak memberikan token akses (resultCode=" + resultCode + "). "
                            + "Pastikan YouTube Data API v3 aktif dan akun pengujian sudah terdaftar.");
                }
            } else {
                List<String> granted = result.getGrantedScopes();
                if (granted != null && !granted.isEmpty() && !granted.contains(YOUTUBE_READONLY)) {
                    emitError("Akun berhasil dikenali, tetapi izin YouTube read-only tidak diberikan. Sambungkan ulang dan setujui izin tersebut.");
                } else {
                    acceptAccessToken(token);
                }
            }
        } catch (com.google.android.gms.common.api.ApiException error) {
            emitError(describeAuthFailure(error));
        } catch (Exception error) {
            emitError("Otorisasi Google gagal (resultCode=" + resultCode + "): " + safeMessage(error));
        }
        return true;
    }

    private void acceptAccessToken(String token) {
        accessToken = token;
        executor.execute(() -> {
            try {
                JSONObject profile = getJson(USERINFO_URL);
                if (profile == null) profile = new JSONObject();
                profile.put("connected", true);
                profile.put("provider", "Google / YouTube");
                emit("onConnected", profile);
            } catch (Exception error) {
                // Keep the token only in memory, but tell the UI the account identity lookup failed.
                JSONObject profile = new JSONObject();
                try {
                    profile.put("connected", true);
                    profile.put("name", "Akun Google terhubung");
                    profile.put("email", "");
                    profile.put("picture", "");
                    profile.put("profileWarning", safeMessage(error));
                } catch (Exception ignored) { }
                emit("onConnected", profile);
            }
        });
    }

    @JavascriptInterface
    public void loadSubscriptions() {
        loadYouTubeResource("subscriptions?part=snippet,contentDetails&mine=true&maxResults=50", "onSubscriptions");
    }

    @JavascriptInterface
    public void loadPlaylists() {
        loadYouTubeResource("playlists?part=snippet,contentDetails&mine=true&maxResults=50", "onPlaylists");
    }

    private void loadYouTubeResource(String path, String callback) {
        final String token = accessToken;
        if (token == null || token.trim().isEmpty()) {
            emitError("Hubungkan akun Google terlebih dahulu untuk membaca data YouTube pribadi.");
            return;
        }
        try {
            executor.execute(() -> {
                try {
                    // Ambil sampai 6 halaman (maks ~300 item) agar daftar langganan tidak terpotong di 50.
                    org.json.JSONArray all = new org.json.JSONArray();
                    String pageToken = "";
                    for (int page = 0; page < 6; page++) {
                        String url = YOUTUBE_API + "/" + path
                                + (pageToken.isEmpty() ? "" : "&pageToken=" + java.net.URLEncoder.encode(pageToken, "UTF-8"));
                        JSONObject result = getJson(url);
                        if (result == null) break;
                        org.json.JSONArray items = result.optJSONArray("items");
                        if (items != null) for (int i = 0; i < items.length(); i++) all.put(items.get(i));
                        pageToken = result.optString("nextPageToken", "");
                        if (pageToken.isEmpty()) break;
                    }
                    JSONObject merged = new JSONObject();
                    merged.put("items", all);
                    emit(callback, merged);
                } catch (Exception error) {
                    if (String.valueOf(error.getMessage()).contains("HTTP 401")) accessToken = "";
                    String message = safeMessage(error);
                    if (message.contains("HTTP 403")) {
                        message += " — pastikan YouTube Data API v3 sudah diaktifkan di project Google Cloud dan akun terdaftar sebagai test user.";
                    }
                    emitError("Tidak dapat memuat data akun YouTube: " + message);
                }
            });
        } catch (RuntimeException rejected) { /* executor dimatikan */ }
    }

    /**
     * Mencabut izin lewat endpoint OAuth standar Google (https://oauth2.googleapis.com/revoke) dan
     * menghapus token di memori. Tidak bergantung pada RevokeAccessRequest (yang membutuhkan akun).
     */
    @JavascriptInterface
    public void disconnect() {
        final String token = accessToken;
        accessToken = "";
        if (token == null || token.trim().isEmpty()) {
            JSONObject result = new JSONObject();
            try {
                result.put("revoked", false);
                result.put("message", "Sesi lokal dihapus. Cabut izin lewat https://myaccount.google.com/connections bila perlu.");
            } catch (Exception ignoredJson) { }
            emit("onDisconnected", result);
            return;
        }
        try {
            executor.execute(() -> {
                JSONObject result = new JSONObject();
                try {
                    Request revoke = new Request.Builder()
                            .url("https://oauth2.googleapis.com/revoke")
                            .post(new FormBody.Builder().add("token", token).build())
                            .build();
                    try (Response response = http.newCall(revoke).execute()) {
                        result.put("revoked", response.isSuccessful());
                        if (!response.isSuccessful()) {
                            result.put("message", "Sesi lokal dihapus, tetapi Google menolak pencabutan (HTTP " + response.code() + ").");
                        }
                    }
                } catch (Exception error) {
                    try {
                        result.put("revoked", false);
                        result.put("message", "Sesi lokal dihapus. Cabut izin dari pengaturan akun Google: " + safeMessage(error));
                    } catch (Exception ignoredJson) { }
                }
                emit("onDisconnected", result);
            });
        } catch (RuntimeException rejected) { /* executor dimatikan */ }
    }

    private JSONObject getJson(String url) throws IOException {
        String token = accessToken;
        Request.Builder builder = new Request.Builder().url(url).get();
        if (token != null && !token.trim().isEmpty()) {
            builder.header("Authorization", "Bearer " + token);
        }
        try (Response response = http.newCall(builder.build()).execute()) {
            String body = response.body() == null ? "{}" : response.body().string();
            if (!response.isSuccessful()) {
                throw new IOException("HTTP " + response.code() + (body.isEmpty() ? "" : ": " + body));
            }
            try {
                return new JSONObject(body);
            } catch (Exception error) {
                throw new IOException("Respons Google bukan JSON yang valid.", error);
            }
        }
    }

    private void emitError(String message) {
        JSONObject payload = new JSONObject();
        try { payload.put("message", message == null ? "Terjadi kesalahan Google." : message); }
        catch (Exception ignored) { }
        emit("onError", payload);
    }

    private void emit(String callback, JSONObject payload) {
        String json = payload == null ? "{}" : payload.toString();
        String script = "(function(){var api=window.ryomdYouTubeAccount;if(api&&typeof api." + callback
                + "==='function')api." + callback + "(" + json + ");})();";
        main.post(() -> {
            if (webView != null && !activity.isFinishing() && !activity.isDestroyed()) {
                webView.evaluateJavascript(script, null);
            }
        });
    }

    /** Releases token material and background work when the hosting Activity is destroyed. */
    public void shutdown() {
        accessToken = "";
        executor.shutdownNow();
    }

    /** Terjemahkan galat umum konfigurasi OAuth menjadi langkah perbaikan yang jelas. */
    private static String describeAuthFailure(com.google.android.gms.common.api.ApiException error) {
        int code = error == null ? -1 : error.getStatusCode();
        if (code == 10) {
            return "Konfigurasi OAuth Android tidak cocok (kode 10 / DEVELOPER_ERROR). Periksa package "
                    + "com.ryomd.nativeapp dan SHA-1 dari signing key APK release WZH pada OAuth client Android Google Cloud.";
        }
        if (code == 7 || code == 8) {
            return "Koneksi/layanan Google gagal (kode " + code + "). Periksa internet, tanggal/waktu perangkat, lalu coba lagi.";
        }
        if (code == 16 || code == 12501) {
            return "Alur izin Google dibatalkan (status " + code + "). Jika bukan Anda yang menekan Batal/Kembali, coba lagi dan pastikan aplikasi Google serta Google Play Services aktif.";
        }
        if (code == 17) {
            return "Layanan otorisasi Google tidak tersedia pada perangkat ini (status 17). Perbarui Google Play Services dan coba lagi.";
        }
        return "Google gagal menyelesaikan otorisasi (status " + code + "): " + safeMessage(error)
                + ". Periksa OAuth Android client, status consent screen/test user, dan YouTube Data API v3.";
    }

    private static String safeMessage(Throwable error) {
        String message = error == null ? "" : error.getMessage();
        if (message == null || message.trim().isEmpty()) message = error == null ? "" : error.getClass().getSimpleName();
        message = message.replace('\n', ' ').replace('\r', ' ');
        return message.length() > 220 ? message.substring(0, 220) : message;
    }
}
