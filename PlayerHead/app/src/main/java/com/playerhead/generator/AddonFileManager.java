package com.playerhead.generator;

import android.content.Context;
import android.content.Intent;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.util.Base64;
import android.util.Log;

import androidx.core.content.FileProvider;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.Locale;

public class AddonFileManager {
    private static final String TAG = "AddonFileManager";
    public static final String FOLDER_NAME = "PlayerHead";
    private final Context context;

    public AddonFileManager(Context context) {
        this.context = context;
        ensureDirectoryExists();
    }

    public File getPlayerHeadDirectory() {
        File downloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        File dir = new File(downloadDir, FOLDER_NAME);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        return dir;
    }

    public String getFolderPath() {
        return getPlayerHeadDirectory().getAbsolutePath();
    }

    public boolean ensureDirectoryExists() {
        try {
            File dir = getPlayerHeadDirectory();
            if (!dir.exists()) {
                return dir.mkdirs();
            }
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Error creating PlayerHead directory: " + e.getMessage(), e);
            return false;
        }
    }

    public JSONObject saveBase64File(String base64Data, String fileName) {
        JSONObject result = new JSONObject();
        try {
            ensureDirectoryExists();
            File destFile = new File(getPlayerHeadDirectory(), fileName);

            // Strip prefix if any (e.g. data:application/zip;base64,)
            String cleanBase64 = base64Data;
            int commaIndex = cleanBase64.indexOf(',');
            if (commaIndex != -1) {
                cleanBase64 = cleanBase64.substring(commaIndex + 1);
            }

            byte[] decodedBytes = Base64.decode(cleanBase64, Base64.DEFAULT);
            try (FileOutputStream fos = new FileOutputStream(destFile)) {
                fos.write(decodedBytes);
                fos.flush();
            }

            // Trigger MediaScanner so file is indexed and visible to file managers & Minecraft
            MediaScannerConnection.scanFile(context,
                    new String[]{destFile.getAbsolutePath()},
                    null, null);

            result.put("success", true);
            result.put("path", destFile.getAbsolutePath());
            result.put("fileName", fileName);
            result.put("size", destFile.length());
            result.put("message", "Saved to " + destFile.getAbsolutePath());
        } catch (Exception e) {
            Log.e(TAG, "Error saving base64 file: " + e.getMessage(), e);
            try {
                result.put("success", false);
                result.put("message", e.getMessage());
            } catch (Exception ignored) {}
        }
        return result;
    }

    public JSONObject saveTextFile(String content, String fileName) {
        JSONObject result = new JSONObject();
        try {
            ensureDirectoryExists();
            File destFile = new File(getPlayerHeadDirectory(), fileName);

            try (FileOutputStream fos = new FileOutputStream(destFile)) {
                fos.write(content.getBytes("UTF-8"));
                fos.flush();
            }

            MediaScannerConnection.scanFile(context,
                    new String[]{destFile.getAbsolutePath()},
                    null, null);

            result.put("success", true);
            result.put("path", destFile.getAbsolutePath());
            result.put("fileName", fileName);
            result.put("size", destFile.length());
            result.put("message", "Saved to " + destFile.getAbsolutePath());
        } catch (Exception e) {
            Log.e(TAG, "Error saving text file: " + e.getMessage(), e);
            try {
                result.put("success", false);
                result.put("message", e.getMessage());
            } catch (Exception ignored) {}
        }
        return result;
    }

    public JSONArray getDownloadedFiles() {
        JSONArray array = new JSONArray();
        try {
            File dir = getPlayerHeadDirectory();
            if (dir.exists() && dir.isDirectory()) {
                File[] files = dir.listFiles();
                if (files != null) {
                    // Sort descending by last modified
                    Arrays.sort(files, new Comparator<File>() {
                        @Override
                        public int compare(File f1, File f2) {
                            return Long.compare(f2.lastModified(), f1.lastModified());
                        }
                    });

                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());

                    for (File file : files) {
                        if (file.isFile()) {
                            JSONObject obj = new JSONObject();
                            obj.put("name", file.getName());
                            obj.put("path", file.getAbsolutePath());
                            obj.put("size", file.length());
                            obj.put("formattedSize", formatFileSize(file.length()));
                            obj.put("lastModified", file.lastModified());
                            obj.put("date", sdf.format(new Date(file.lastModified())));
                            obj.put("isAddon", file.getName().toLowerCase().endsWith(".mcaddon") || file.getName().toLowerCase().endsWith(".zip"));
                            obj.put("isJson", file.getName().toLowerCase().endsWith(".json"));
                            array.put(obj);
                        }
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error listing downloaded files: " + e.getMessage(), e);
        }
        return array;
    }

    public boolean deleteFile(String fileName) {
        try {
            File file = new File(getPlayerHeadDirectory(), fileName);
            if (file.exists()) {
                boolean deleted = file.delete();
                if (deleted) {
                    MediaScannerConnection.scanFile(context,
                            new String[]{file.getAbsolutePath()},
                            null, null);
                }
                return deleted;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error deleting file: " + e.getMessage(), e);
        }
        return false;
    }

    public boolean openFile(String fileName) {
        try {
            File file = new File(getPlayerHeadDirectory(), fileName);
            if (!file.exists()) return false;

            Uri uri = FileProvider.getUriForFile(context,
                    context.getPackageName() + ".fileprovider", file);

            Intent intent = new Intent(Intent.ACTION_VIEW);
            String mimeType = "application/octet-stream";
            if (fileName.toLowerCase().endsWith(".json")) {
                mimeType = "application/json";
            } else if (fileName.toLowerCase().endsWith(".mcaddon") || fileName.toLowerCase().endsWith(".zip")) {
                mimeType = "application/octet-stream";
            }

            intent.setDataAndType(uri, mimeType);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

            context.startActivity(Intent.createChooser(intent, "Open with").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Error opening file: " + e.getMessage(), e);
            return false;
        }
    }

    public boolean shareFile(String fileName) {
        try {
            File file = new File(getPlayerHeadDirectory(), fileName);
            if (!file.exists()) return false;

            Uri uri = FileProvider.getUriForFile(context,
                    context.getPackageName() + ".fileprovider", file);

            Intent intent = new Intent(Intent.ACTION_SEND);
            String mimeType = "application/octet-stream";
            if (fileName.toLowerCase().endsWith(".json")) {
                mimeType = "application/json";
            }
            intent.setType(mimeType);
            intent.putExtra(Intent.EXTRA_STREAM, uri);
            intent.putExtra(Intent.EXTRA_SUBJECT, fileName);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

            Intent chooser = Intent.createChooser(intent, "Share " + fileName);
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(chooser);
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Error sharing file: " + e.getMessage(), e);
            return false;
        }
    }

    public boolean openDownloadFolder() {
        try {
            Uri selectedUri = Uri.parse(getFolderPath());
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(selectedUri, "resource/folder");
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            if (intent.resolveActivity(context.getPackageManager()) != null) {
                context.startActivity(intent);
                return true;
            } else {
                // Fallback to general Downloads
                Intent fallback = new Intent(Intent.ACTION_GET_CONTENT);
                fallback.setDataAndType(Uri.parse(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getAbsolutePath()), "*/*");
                fallback.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(Intent.createChooser(fallback, "Open Folder"));
                return true;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error opening download folder: " + e.getMessage(), e);
            return false;
        }
    }

    private String formatFileSize(long bytes) {
        if (bytes <= 0) return "0 B";
        final String[] units = new String[]{"B", "KB", "MB", "GB"};
        int digitGroups = (int) (Math.log10(bytes) / Math.log10(1024));
        if (digitGroups >= units.length) digitGroups = units.length - 1;
        return String.format(Locale.US, "%.1f %s", bytes / Math.pow(1024, digitGroups), units[digitGroups]);
    }
}
