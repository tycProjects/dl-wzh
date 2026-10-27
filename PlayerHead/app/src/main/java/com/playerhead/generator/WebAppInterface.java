package com.playerhead.generator;

import android.webkit.JavascriptInterface;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

public class WebAppInterface {
    private final MainActivity activity;
    private final AddonFileManager fileManager;

    public WebAppInterface(MainActivity activity, AddonFileManager fileManager) {
        this.activity = activity;
        this.fileManager = fileManager;
    }

    @JavascriptInterface
    public String saveAddon(String base64Data, String fileName) {
        JSONObject result = fileManager.saveBase64File(base64Data, fileName);
        return result.toString();
    }

    @JavascriptInterface
    public String saveJsonPack(String jsonString, String fileName) {
        JSONObject result = fileManager.saveTextFile(jsonString, fileName);
        return result.toString();
    }

    @JavascriptInterface
    public String getDownloadedAddons() {
        JSONArray files = fileManager.getDownloadedFiles();
        return files.toString();
    }

    @JavascriptInterface
    public boolean openAddon(String fileName) {
        return fileManager.openFile(fileName);
    }

    @JavascriptInterface
    public boolean shareAddon(String fileName) {
        return fileManager.shareFile(fileName);
    }

    @JavascriptInterface
    public boolean deleteAddon(String fileName) {
        return fileManager.deleteFile(fileName);
    }

    @JavascriptInterface
    public String getDownloadFolderPath() {
        return fileManager.getFolderPath();
    }

    @JavascriptInterface
    public boolean openDownloadFolder() {
        return fileManager.openDownloadFolder();
    }

    @JavascriptInterface
    public boolean isStoragePermissionGranted() {
        return activity.isStoragePermissionGranted();
    }

    @JavascriptInterface
    public void requestStoragePermission() {
        activity.runOnUiThread(activity::requestStoragePermissions);
    }

    @JavascriptInterface
    public void showToast(String message) {
        activity.runOnUiThread(() ->
                Toast.makeText(activity, message, Toast.LENGTH_SHORT).show()
        );
    }
}
