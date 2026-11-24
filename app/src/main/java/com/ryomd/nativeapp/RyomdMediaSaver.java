package com.ryomd.nativeapp;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.webkit.CookieManager;
import android.webkit.MimeTypeMap;
import android.webkit.URLUtil;
import android.widget.Toast;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Saves WebView download links into Android's media library.
 * Android 10+ uses scoped MediaStore; Android 8/9 use public media folders
 * and the narrowly-scoped WRITE_EXTERNAL_STORAGE permission.
 */
public final class RyomdMediaSaver {
    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "ryomd-media-saver");
        thread.setDaemon(true);
        return thread;
    });

    private static final Set<String> IN_FLIGHT = ConcurrentHashMap.newKeySet();

    private final Context context;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public RyomdMediaSaver(Context context) {
        this.context = context.getApplicationContext();
    }

    public boolean download(String rawUrl, String userAgent, String contentDisposition,
                            String suppliedMimeType, String referer) {
        final String safeUrl = rawUrl == null ? "" : rawUrl.trim();
        if (safeUrl.isEmpty() || !IN_FLIGHT.add(safeUrl)) return false;
        EXECUTOR.execute(() -> {
            try { performDownload(safeUrl, userAgent, contentDisposition, suppliedMimeType, referer); }
            finally { IN_FLIGHT.remove(safeUrl); }
        });
        return true;
    }

    private void performDownload(String rawUrl, String userAgent, String contentDisposition,
                                 String suppliedMimeType, String referer) {
        HttpURLConnection connection = null;
        Uri savedUri = null;
        try {
            URL initialUrl = new URL(rawUrl);
            validateDownloadUrl(initialUrl);
            String initialCookie = safeCookie(initialUrl.toString());
            connection = openConnection(initialUrl, userAgent, referer, initialCookie);

            int responseCode = connection.getResponseCode();
            if (responseCode < 200 || responseCode >= 300) {
                throw new IllegalStateException("Server unduhan merespons HTTP " + responseCode + ".");
            }

            String responseMime = cleanMime(connection.getContentType());
            if (responseMime.startsWith("text/") || "application/json".equals(responseMime)
                    || "application/problem+json".equals(responseMime)
                    || "application/xhtml+xml".equals(responseMime)) {
                throw new IllegalStateException("Server mengirim respons " + responseMime
                        + ", bukan file media. Periksa hasil unduhan backend Ryomd.");
            }

            String disposition = connection.getHeaderField("Content-Disposition");
            if (disposition == null || disposition.trim().isEmpty()) disposition = contentDisposition;
            String fileName = safeFileName(URLUtil.guessFileName(rawUrl, disposition,
                    chooseMimeType(suppliedMimeType, responseMime, rawUrl)), suppliedMimeType);
            String mimeType = chooseMimeTypeFromName(fileName, suppliedMimeType, responseMime, rawUrl);
            if (!mimeType.startsWith("video/") && !mimeType.startsWith("audio/") && !mimeType.startsWith("image/")) {
                throw new IllegalStateException("Jenis file bukan video, audio, atau gambar (" + mimeType + ").");
            }
            long expectedLength = connection.getContentLengthLong();
            showToast("Mengunduh " + fileName + "…", Toast.LENGTH_SHORT);

            InputStream input = new BufferedInputStream(connection.getInputStream());
            try {
                if (Build.VERSION.SDK_INT >= 29) {
                    savedUri = writeToMediaStore(input, fileName, mimeType, expectedLength);
                    MediaRecord record = verifyMediaStore(savedUri, fileName, mimeType);
                    showToast("Tersimpan: " + record.path + record.name + " · " + humanBytes(record.size), Toast.LENGTH_LONG);
                } else {
                    LegacyResult legacy = writeToLegacyMediaFolder(input, fileName, mimeType, expectedLength);
                    savedUri = legacy.uri;
                    if (!legacy.file.isFile() || legacy.file.length() <= 0) {
                        throw new IllegalStateException("File tujuan tidak ditemukan setelah penulisan.");
                    }
                    showToast("Tersimpan: " + legacy.file.getAbsolutePath() + " · " + humanBytes(legacy.file.length()), Toast.LENGTH_LONG);
                }
            } finally {
                input.close();
            }
        } catch (Exception error) {
            if (savedUri != null && Build.VERSION.SDK_INT >= 29) {
                try { context.getContentResolver().delete(savedUri, null, null); } catch (Exception ignored) { }
            }
            String detail = error.getMessage();
            if (detail == null || detail.trim().isEmpty()) detail = error.getClass().getSimpleName();
            showToast("Gagal menyimpan media: " + detail, Toast.LENGTH_LONG);
        } finally {
            if (connection != null) connection.disconnect();
        }
    }

    private HttpURLConnection openConnection(URL start, String userAgent,
                                             String referer, String initialCookie) throws Exception {
        URL current = start;
        for (int redirects = 0; redirects <= 8; redirects++) {
            validateDownloadUrl(current);
            HttpURLConnection connection = (HttpURLConnection) current.openConnection();
            connection.setRequestMethod("GET");
            connection.setInstanceFollowRedirects(false);
            connection.setConnectTimeout(20000);
            connection.setReadTimeout(60000);
            connection.setUseCaches(false);
            connection.setDoInput(true);
            connection.setRequestProperty("Accept", "*/*");
            if (userAgent != null && !userAgent.trim().isEmpty()) {
                connection.setRequestProperty("User-Agent", userAgent.trim());
            }
            if (referer != null && !referer.trim().isEmpty()) {
                connection.setRequestProperty("Referer", referer.trim());
            }
            String cookie = safeCookie(current.toString());
            if (cookie.isEmpty() && redirects == 0 && initialCookie != null) cookie = initialCookie;
            if (!cookie.isEmpty()) connection.setRequestProperty("Cookie", cookie);

            int code = connection.getResponseCode();
            if (code == HttpURLConnection.HTTP_MOVED_PERM
                    || code == HttpURLConnection.HTTP_MOVED_TEMP
                    || code == HttpURLConnection.HTTP_SEE_OTHER
                    || code == 307 || code == 308) {
                String location = connection.getHeaderField("Location");
                connection.disconnect();
                if (location == null || location.trim().isEmpty()) {
                    throw new IllegalStateException("Server memberi pengalihan tanpa alamat tujuan.");
                }
                current = new URL(current, location);
                continue;
            }
            return connection;
        }
        throw new IllegalStateException("Terlalu banyak pengalihan pada tautan unduhan.");
    }

    private void validateDownloadUrl(URL url) {
        String protocol = url.getProtocol() == null ? "" : url.getProtocol().toLowerCase(Locale.ROOT);
        String host = url.getHost() == null ? "" : url.getHost().toLowerCase(Locale.ROOT);
        boolean loopbackHttp = "http".equals(protocol)
                && ("127.0.0.1".equals(host) || "localhost".equals(host) || "::1".equals(host));
        if (!"https".equals(protocol) && !loopbackHttp) {
            throw new IllegalArgumentException("Unduhan hanya diizinkan dari HTTPS atau server lokal Ryomd.");
        }
        if (url.getUserInfo() != null) {
            throw new IllegalArgumentException("Alamat unduhan tidak boleh berisi kredensial URL.");
        }
    }

    private String safeCookie(String url) {
        try {
            String cookie = CookieManager.getInstance().getCookie(url);
            return cookie == null ? "" : cookie;
        } catch (Exception ignored) {
            return "";
        }
    }

    private Uri writeToMediaStore(InputStream input, String fileName, String mimeType, long expectedLength) throws Exception {
        ContentResolver resolver = context.getContentResolver();
        String directory;
        Uri collection;
        if (mimeType.startsWith("video/")) {
            directory = Environment.DIRECTORY_MOVIES + "/Ryomd";
            collection = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
        } else if (mimeType.startsWith("audio/")) {
            directory = Environment.DIRECTORY_MUSIC + "/Ryomd";
            collection = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        } else if (mimeType.startsWith("image/")) {
            directory = Environment.DIRECTORY_PICTURES + "/Ryomd";
            collection = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
        } else {
            directory = Environment.DIRECTORY_DOWNLOADS + "/Ryomd";
            collection = MediaStore.Downloads.EXTERNAL_CONTENT_URI;
        }

        ContentValues values = new ContentValues();
        values.put(MediaStore.MediaColumns.DISPLAY_NAME, fileName);
        values.put(MediaStore.MediaColumns.MIME_TYPE, mimeType);
        values.put(MediaStore.MediaColumns.RELATIVE_PATH, directory);
        values.put(MediaStore.MediaColumns.IS_PENDING, 1);

        Uri item = resolver.insert(collection, values);
        if (item == null) throw new IllegalStateException("Android tidak dapat membuat tujuan penyimpanan media.");

        boolean completed = false;
        try {
            OutputStream rawOutput = resolver.openOutputStream(item, "w");
            if (rawOutput == null) throw new IllegalStateException("Stream penyimpanan Android tidak tersedia.");
            long copied;
            try (OutputStream output = new BufferedOutputStream(rawOutput)) {
                copied = copyStream(input, output);
                output.flush();
            }
            if (expectedLength > 0 && copied != expectedLength) {
                throw new IllegalStateException("Unduhan tidak lengkap: " + copied + " dari " + expectedLength + " byte.");
            }
            ContentValues ready = new ContentValues();
            ready.put(MediaStore.MediaColumns.IS_PENDING, 0);
            int updated = resolver.update(item, ready, null, null);
            if (updated < 1) throw new IllegalStateException("Media tersimpan, tetapi status publikasinya gagal diperbarui.");
            completed = true;
            return item;
        } finally {
            if (!completed) {
                try { resolver.delete(item, null, null); } catch (Exception ignored) { }
            }
        }
    }

    private static final class LegacyResult {
        final Uri uri;
        final File file;
        LegacyResult(Uri uri, File file) { this.uri = uri; this.file = file; }
    }

    @SuppressWarnings("deprecation")
    private LegacyResult writeToLegacyMediaFolder(InputStream input, String fileName,
                                                   String mimeType, long expectedLength) throws Exception {
        String baseDirectory;
        if (mimeType.startsWith("video/")) baseDirectory = Environment.DIRECTORY_MOVIES;
        else if (mimeType.startsWith("audio/")) baseDirectory = Environment.DIRECTORY_MUSIC;
        else if (mimeType.startsWith("image/")) baseDirectory = Environment.DIRECTORY_PICTURES;
        else baseDirectory = Environment.DIRECTORY_DOWNLOADS;

        File folder = new File(Environment.getExternalStoragePublicDirectory(baseDirectory), "Ryomd");
        if (!folder.exists() && !folder.mkdirs()) {
            throw new IllegalStateException("Folder media Ryomd tidak dapat dibuat.");
        }
        File target = uniqueFile(folder, fileName);
        File temporary = new File(folder, target.getName() + ".part");
        try {
            long copied;
            try (OutputStream output = new BufferedOutputStream(new FileOutputStream(temporary))) {
                copied = copyStream(input, output);
                output.flush();
            }
            if (expectedLength > 0 && copied != expectedLength) {
                throw new IllegalStateException("Unduhan tidak lengkap: " + copied + " dari " + expectedLength + " byte.");
            }
            if (!temporary.renameTo(target)) {
                throw new IllegalStateException("File selesai diunduh, tetapi gagal dipindahkan ke folder media.");
            }
        } catch (Exception error) {
            if (temporary.exists()) temporary.delete();
            throw error;
        }
        android.media.MediaScannerConnection.scanFile(
                context, new String[]{target.getAbsolutePath()}, new String[]{mimeType}, null
        );
        return new LegacyResult(Uri.fromFile(target), target);
    }

    private static File uniqueFile(File folder, String fileName) {
        File candidate = new File(folder, fileName);
        if (!candidate.exists()) return candidate;
        String base = fileName;
        String ext = "";
        int dot = fileName.lastIndexOf('.');
        if (dot > 0) { base = fileName.substring(0, dot); ext = fileName.substring(dot); }
        for (int i = 1; i <= 999; i++) {
            candidate = new File(folder, base + " (" + i + ")" + ext);
            if (!candidate.exists()) return candidate;
        }
        return new File(folder, base + " " + System.currentTimeMillis() + ext);
    }

    private static long copyStream(InputStream input, OutputStream output) throws Exception {
        byte[] buffer = new byte[64 * 1024];
        int read;
        long total = 0;
        while ((read = input.read(buffer)) != -1) {
            if (read == 0) continue;
            output.write(buffer, 0, read);
            total += read;
        }
        if (total == 0) throw new IllegalStateException("File unduhan kosong (0 byte).");
        return total;
    }

    private static final class MediaRecord {
        final String name, path;
        final long size;
        MediaRecord(String name, String path, long size) { this.name = name; this.path = path == null ? "" : path; this.size = size; }
    }

    private MediaRecord verifyMediaStore(Uri uri, String expectedName, String expectedMime) throws Exception {
        String[] columns = { MediaStore.MediaColumns.DISPLAY_NAME, MediaStore.MediaColumns.MIME_TYPE,
                MediaStore.MediaColumns.RELATIVE_PATH, MediaStore.MediaColumns.SIZE, MediaStore.MediaColumns.IS_PENDING };
        try (Cursor cursor = context.getContentResolver().query(uri, columns, null, null, null)) {
            if (cursor == null || !cursor.moveToFirst()) throw new IllegalStateException("Android tidak menemukan file setelah disimpan.");
            String name = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME));
            String mime = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.MIME_TYPE));
            String path = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.RELATIVE_PATH));
            long size = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.SIZE));
            int pending = cursor.getInt(cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.IS_PENDING));
            if (size <= 0 || pending != 0 || !expectedName.equals(name) || !expectedMime.equalsIgnoreCase(mime)) {
                throw new IllegalStateException("Verifikasi MediaStore gagal (ukuran " + size + " byte; status pending " + pending + ").");
            }
            return new MediaRecord(name, path, size);
        }
    }

    private static String chooseMimeTypeFromName(String fileName, String supplied, String response, String url) {
        int dot = fileName == null ? -1 : fileName.lastIndexOf('.');
        String ext = dot < 0 ? "" : fileName.substring(dot + 1).toLowerCase(Locale.ROOT);
        String byExtension = null;
        switch (ext) {
            case "mp4": case "m4v": byExtension = "video/mp4"; break;
            case "webm": byExtension = "video/webm"; break;
            case "mkv": byExtension = "video/x-matroska"; break;
            case "mov": byExtension = "video/quicktime"; break;
            case "mp3": byExtension = "audio/mpeg"; break;
            case "m4a": byExtension = "audio/mp4"; break;
            case "aac": byExtension = "audio/aac"; break;
            case "wav": byExtension = "audio/wav"; break;
            case "ogg": case "opus": byExtension = "audio/ogg"; break;
            case "jpg": case "jpeg": byExtension = "image/jpeg"; break;
            case "png": byExtension = "image/png"; break;
            case "webp": byExtension = "image/webp"; break;
            case "gif": byExtension = "image/gif"; break;
            case "avif": byExtension = "image/avif"; break;
            default: break;
        }
        if (byExtension != null) return byExtension;
        return chooseMimeType(supplied, response, url);
    }

    private static String humanBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024L * 1024L) return String.format(Locale.US, "%.1f KB", bytes / 1024.0);
        if (bytes < 1024L * 1024L * 1024L) return String.format(Locale.US, "%.1f MB", bytes / (1024.0 * 1024.0));
        return String.format(Locale.US, "%.2f GB", bytes / (1024.0 * 1024.0 * 1024.0));
    }

    private static String chooseMimeType(String supplied, String response, String url) {
        String mime = cleanMime(response);
        if (mime.isEmpty() || "application/octet-stream".equals(mime)) mime = cleanMime(supplied);
        if (mime.isEmpty() || "application/octet-stream".equals(mime)) {
            String path = url == null ? "" : url;
            int query = path.indexOf('?');
            if (query >= 0) path = path.substring(0, query);
            String extension = MimeTypeMap.getFileExtensionFromUrl(path).toLowerCase(Locale.ROOT);
            String guessed = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
            if (guessed != null) mime = guessed;
        }
        return mime.isEmpty() ? "application/octet-stream" : mime;
    }

    private static String cleanMime(String value) {
        if (value == null) return "";
        int semicolon = value.indexOf(';');
        String mime = (semicolon >= 0 ? value.substring(0, semicolon) : value).trim();
        return mime.toLowerCase(Locale.ROOT);
    }

    private static String safeFileName(String name, String mimeType) {
        String clean = name == null ? "" : name.trim();
        clean = clean.replaceAll("[\\/:*?\"<>|\\p{Cntrl}]", "_");
        clean = clean.replaceAll("\\s+", " ").trim();
        while (clean.startsWith(".")) clean = clean.substring(1);
        if (clean.isEmpty()) clean = "Ryomd-download-" + System.currentTimeMillis();
        if (!clean.contains(".")) {
            String extension = MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType);
            if (extension != null && !extension.trim().isEmpty()) clean += "." + extension;
        }
        if (clean.length() > 180) {
            int dot = clean.lastIndexOf('.');
            String ext = dot > 0 ? clean.substring(dot) : "";
            clean = clean.substring(0, Math.min(180 - ext.length(), clean.length())) + ext;
        }
        return clean;
    }

    private void showToast(String message, int duration) {
        mainHandler.post(() -> Toast.makeText(context, message, duration).show());
    }
}
