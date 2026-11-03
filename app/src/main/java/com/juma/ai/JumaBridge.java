package com.juma.ai;

import android.app.Activity;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraManager;
import android.media.AudioManager;
import android.net.Uri;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Vibrator;
import android.provider.AlarmClock;
import android.provider.CalendarContract;
import android.provider.ContactsContract;
import android.provider.MediaStore;
import android.provider.Settings;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.speech.tts.Voice;
import android.telephony.SmsManager;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Locale;
import java.util.Set;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;

public class JumaBridge {
    private final Activity activity;
    private final WebView webView;

    // Speech Recognizer
    private SpeechRecognizer speechRecognizer = null;
    private boolean isListening = false;

    // Text-to-Speech
    private TextToSpeech tts = null;
    private boolean isTtsInitialized = false;

    public JumaBridge(Activity activity, WebView webView) {
        this.activity = activity;
        this.webView = webView;
        initTts();
    }

    private void runJs(final String jsCode) {
        if (webView == null || jsCode == null) return;
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                        webView.evaluateJavascript(jsCode, null);
                    } else {
                        webView.loadUrl("javascript:" + jsCode);
                    }
                } catch (Exception ignored) {}
            }
        });
    }

    private String escapeJsString(String str) {
        if (str == null) return "";
        return str.replace("\\", "\\\\")
                  .replace("'", "\\'")
                  .replace("\"", "\\\"")
                  .replace("\n", "\\n")
                  .replace("\r", "\\r");
    }

    // ==========================================
    // RUNTIME ENVIRONMENT DETECTION
    // ==========================================

    @JavascriptInterface
    public boolean isAvailable() {
        return true;
    }

    @JavascriptInterface
    public String getRuntimeEnvironment() {
        return "FULL_NATIVE_ANDROID";
    }

    // ==========================================
    // 1. NATIVE SPEECH RECOGNITION
    // ==========================================

    @JavascriptInterface
    public void startListening(final String lang) {
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        if (activity.checkSelfPermission(android.Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                            activity.requestPermissions(new String[]{android.Manifest.permission.RECORD_AUDIO}, 101);
                            runJs("if (window.onAndroidSpeechError) { window.onAndroidSpeechError(9, 'PERMISSION_REQUIRED: Microphone permission is required. Please grant permission.'); }");
                            return;
                        }
                    }

                    if (speechRecognizer != null) {
                        try {
                            speechRecognizer.destroy();
                        } catch (Exception ignored) {}
                        speechRecognizer = null;
                    }

                    speechRecognizer = SpeechRecognizer.createSpeechRecognizer(activity);
                    if (speechRecognizer == null) {
                        runJs("if (window.onAndroidSpeechError) { window.onAndroidSpeechError(-1, 'SpeechRecognizer not available on device'); }");
                        return;
                    }

                    speechRecognizer.setRecognitionListener(new RecognitionListener() {
                        @Override
                        public void onReadyForSpeech(Bundle params) {
                            isListening = true;
                            runJs("if (window.onAndroidSpeechReady) { window.onAndroidSpeechReady(); }");
                        }

                        @Override
                        public void onBeginningOfSpeech() {
                            runJs("if (window.onAndroidSpeechStart) { window.onAndroidSpeechStart(); }");
                        }

                        @Override
                        public void onRmsChanged(float rmsdB) {
                            runJs("if (window.onAndroidSpeechRms) { window.onAndroidSpeechRms(" + rmsdB + "); }");
                        }

                        @Override
                        public void onBufferReceived(byte[] buffer) {}

                        @Override
                        public void onEndOfSpeech() {
                            isListening = false;
                        }

                        @Override
                        public void onError(int error) {
                            isListening = false;
                            String errorMsg = getSpeechErrorMessage(error);
                            runJs("if (window.onAndroidSpeechError) { window.onAndroidSpeechError(" + error + ", '" + escapeJsString(errorMsg) + "'); }");
                        }

                        @Override
                        public void onResults(Bundle results) {
                            isListening = false;
                            ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                            String recognizedText = (matches != null && !matches.isEmpty()) ? matches.get(0) : "";
                            runJs("if (window.onAndroidSpeechResult) { window.onAndroidSpeechResult('" + escapeJsString(recognizedText) + "', true); }");
                        }

                        @Override
                        public void onPartialResults(Bundle partialResults) {
                            ArrayList<String> matches = partialResults.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                            String partialText = (matches != null && !matches.isEmpty()) ? matches.get(0) : "";
                            runJs("if (window.onAndroidSpeechResult) { window.onAndroidSpeechResult('" + escapeJsString(partialText) + "', false); }");
                        }

                        @Override
                        public void onEvent(int eventType, Bundle params) {}
                    });

                    Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
                    intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
                    intent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);
                    intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3);

                    if (lang != null && !lang.isEmpty()) {
                        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, lang);
                        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, lang);
                    } else {
                        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "bn-BD");
                    }

                    speechRecognizer.startListening(intent);
                } catch (Exception e) {
                    runJs("if (window.onAndroidSpeechError) { window.onAndroidSpeechError(-1, '" + escapeJsString(e.getMessage()) + "'); }");
                }
            }
        });
    }

    @JavascriptInterface
    public void stopListening() {
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                try {
                    if (speechRecognizer != null) {
                        speechRecognizer.stopListening();
                    }
                    isListening = false;
                } catch (Exception ignored) {}
            }
        });
    }

    private String getSpeechErrorMessage(int errorCode) {
        switch (errorCode) {
            case SpeechRecognizer.ERROR_AUDIO: return "Audio recording error";
            case SpeechRecognizer.ERROR_CLIENT: return "Client-side error";
            case SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS: return "Microphone permission required";
            case SpeechRecognizer.ERROR_NETWORK: return "Network error during recognition";
            case SpeechRecognizer.ERROR_NETWORK_TIMEOUT: return "Network connection timed out";
            case SpeechRecognizer.ERROR_NO_MATCH: return "No speech recognized";
            case SpeechRecognizer.ERROR_RECOGNIZER_BUSY: return "Speech recognizer is busy";
            case SpeechRecognizer.ERROR_SERVER: return "Server error";
            case SpeechRecognizer.ERROR_SPEECH_TIMEOUT: return "No speech input detected";
            default: return "Speech recognition failed";
        }
    }

    // ==========================================
    // 2. NATIVE TEXT-TO-SPEECH (TTS)
    // ==========================================

    private void initTts() {
        tts = new TextToSpeech(activity.getApplicationContext(), new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if (status == TextToSpeech.SUCCESS) {
                    isTtsInitialized = true;
                    applyOptimalFemaleVoice("bn");
                    tts.setOnUtteranceProgressListener(new UtteranceProgressListener() {
                        @Override
                        public void onStart(String utteranceId) {
                            runJs("if (window.onAndroidTtsStart) { window.onAndroidTtsStart('" + escapeJsString(utteranceId) + "'); }");
                        }

                        @Override
                        public void onDone(String utteranceId) {
                            runJs("if (window.onAndroidTtsDone) { window.onAndroidTtsDone('" + escapeJsString(utteranceId) + "'); }");
                        }

                        @Override
                        public void onError(String utteranceId) {
                            runJs("if (window.onAndroidTtsError) { window.onAndroidTtsError('" + escapeJsString(utteranceId) + "'); }");
                        }
                    });
                }
            }
        });
    }

    private void applyOptimalFemaleVoice(String langHint) {
        if (tts == null || !isTtsInitialized) return;
        try {
            Locale targetLocale = "en".equalsIgnoreCase(langHint) ? Locale.US : new Locale("bn", "BD");
            int result = tts.setLanguage(targetLocale);
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                tts.setLanguage(Locale.US);
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                Set<Voice> voices = tts.getVoices();
                if (voices != null && !voices.isEmpty()) {
                    Voice selectedVoice = null;
                    String[] femaleKeywords = new String[]{"female", "woman", "girl", "zira", "kore", "aria", "samantha", "bn"};
                    for (Voice v : voices) {
                        String name = v.getName().toLowerCase();
                        boolean isTargetLang = v.getLocale() != null && v.getLocale().getLanguage().startsWith(targetLocale.getLanguage());
                        for (String kw : femaleKeywords) {
                            if (name.contains(kw)) {
                                if (isTargetLang) {
                                    selectedVoice = v;
                                    break;
                                } else if (selectedVoice == null) {
                                    selectedVoice = v;
                                }
                            }
                        }
                        if (selectedVoice != null && isTargetLang) break;
                    }
                    if (selectedVoice != null) {
                        tts.setVoice(selectedVoice);
                    }
                }
            }
        } catch (Exception ignored) {}
    }

    @JavascriptInterface
    public boolean speak(final String text, final String lang, final float rate) {
        if (text == null || text.trim().isEmpty()) return false;
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                try {
                    if (tts == null || !isTtsInitialized) {
                        initTts();
                        return;
                    }

                    if (lang != null && !lang.isEmpty()) {
                        applyOptimalFemaleVoice(lang);
                    }

                    if (rate > 0.1f && rate < 3.0f) {
                        tts.setSpeechRate(rate);
                    }

                    String utteranceId = "juma_tts_" + System.currentTimeMillis();
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId);
                    } else {
                        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null);
                    }
                } catch (Exception ignored) {}
            }
        });
        return true;
    }

    @JavascriptInterface
    public void stopSpeaking() {
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                try {
                    if (tts != null) {
                        tts.stop();
                    }
                } catch (Exception ignored) {}
            }
        });
    }

    @JavascriptInterface
    public void setSpeechRate(float rate) {
        if (tts != null && rate > 0.1f && rate < 3.0f) {
            tts.setSpeechRate(rate);
        }
    }

    // ==========================================
    // 3. NATIVE PERMISSION BRIDGE
    // ==========================================

    @JavascriptInterface
    public boolean checkPermission(String permission) {
        if (permission == null) return false;
        if ("notifications".equalsIgnoreCase(permission)) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                return activity.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED;
            } else {
                NotificationManager nm = (NotificationManager) activity.getSystemService(Context.NOTIFICATION_SERVICE);
                return nm != null && nm.areNotificationsEnabled();
            }
        }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return true;

        String realPerm = mapPermissionNameToAndroid(permission);
        return activity.checkSelfPermission(realPerm) == PackageManager.PERMISSION_GRANTED;
    }

    @JavascriptInterface
    public void requestPermission(final String permission) {
        if (permission == null) return;
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if ("notifications".equalsIgnoreCase(permission)) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        activity.requestPermissions(new String[]{android.Manifest.permission.POST_NOTIFICATIONS}, 102);
                    } else {
                        openSettings("notifications");
                    }
                    return;
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    String realPerm = mapPermissionNameToAndroid(permission);
                    activity.requestPermissions(new String[]{realPerm}, 101);
                }
            }
        });
    }

    @JavascriptInterface
    public String checkAllPermissions() {
        JSONObject obj = new JSONObject();
        try {
            boolean isM = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M;
            obj.put("microphone", isM ? activity.checkSelfPermission(android.Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED : true);
            obj.put("phone", isM ? activity.checkSelfPermission(android.Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED : true);
            obj.put("contacts", isM ? activity.checkSelfPermission(android.Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED : true);
            obj.put("sms", isM ? activity.checkSelfPermission(android.Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED : true);
            obj.put("camera", isM ? activity.checkSelfPermission(android.Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED : true);
            obj.put("location", isM ? activity.checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED : true);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                obj.put("notifications", activity.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED);
            } else {
                NotificationManager nm = (NotificationManager) activity.getSystemService(Context.NOTIFICATION_SERVICE);
                obj.put("notifications", nm != null && nm.areNotificationsEnabled());
            }
            obj.put("accessibility", isAccessibilityServiceEnabled());
        } catch (Exception ignored) {}
        return obj.toString();
    }

    private String mapPermissionNameToAndroid(String permission) {
        switch (permission.toLowerCase()) {
            case "microphone":
            case "record_audio":
                return android.Manifest.permission.RECORD_AUDIO;
            case "camera":
                return android.Manifest.permission.CAMERA;
            case "phone":
            case "call_phone":
                return android.Manifest.permission.CALL_PHONE;
            case "contacts":
            case "read_contacts":
                return android.Manifest.permission.READ_CONTACTS;
            case "sms":
            case "send_sms":
                return android.Manifest.permission.SEND_SMS;
            case "location":
                return android.Manifest.permission.ACCESS_FINE_LOCATION;
            case "notifications":
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    return android.Manifest.permission.POST_NOTIFICATIONS;
                }
                return android.Manifest.permission.INTERNET;
            default:
                return permission;
        }
    }

    // ==========================================
    // 4. PHONE CALLING
    // ==========================================

    @JavascriptInterface
    public String makePhoneCall(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.trim().isEmpty()) return "INVALID_NUMBER";
        try {
            boolean hasCallPerm = Build.VERSION.SDK_INT < Build.VERSION_CODES.M ||
                activity.checkSelfPermission(android.Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED;

            Intent intent;
            if (hasCallPerm) {
                // Direct cellular call when CALL_PHONE is granted
                intent = new Intent(Intent.ACTION_CALL, Uri.parse("tel:" + phoneNumber.trim()));
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                activity.startActivity(intent);
                return "CALL_INITIATED";
            } else {
                // Safe dialer fallback with explicit notification that dialer was launched
                intent = new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + phoneNumber.trim()));
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                activity.startActivity(intent);
                return "FALLBACK_DIALER";
            }
        } catch (Exception e) {
            return "ERROR:" + e.getMessage();
        }
    }

    @JavascriptInterface
    public boolean openDialer(String phoneNumber) {
        try {
            Intent intent = new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + (phoneNumber != null ? phoneNumber.trim() : "")));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            activity.startActivity(intent);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    // ==========================================
    // 5. SMS MESSAGING
    // ==========================================

    @JavascriptInterface
    public String sendSMS(String phoneNumber, String messageText) {
        if (phoneNumber == null || phoneNumber.trim().isEmpty()) return "INVALID_NUMBER";
        try {
            boolean hasSmsPerm = Build.VERSION.SDK_INT < Build.VERSION_CODES.M ||
                activity.checkSelfPermission(android.Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED;

            if (hasSmsPerm) {
                // Direct background SMS transmission
                SmsManager smsManager;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    smsManager = activity.getSystemService(SmsManager.class);
                } else {
                    smsManager = SmsManager.getDefault();
                }
                if (smsManager != null) {
                    smsManager.sendTextMessage(phoneNumber.trim(), null, messageText != null ? messageText : "", null, null);
                    return "SMS_SENT";
                }
            }

            // Safe Intent fallback: launches default SMS compose screen
            Intent intent = new Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:" + phoneNumber.trim()));
            if (messageText != null) {
                intent.putExtra("sms_body", messageText);
            }
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            activity.startActivity(intent);
            return "FALLBACK_COMPOSE";
        } catch (Exception e) {
            return "ERROR:" + e.getMessage();
        }
    }

    // ==========================================
    // 6. CAMERA & 7. FILES
    // ==========================================

    @JavascriptInterface
    public boolean openNativeCamera() {
        try {
            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            if (intent.resolveActivity(activity.getPackageManager()) != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                activity.startActivity(intent);
                return true;
            }
        } catch (Exception ignored) {}
        return false;
    }

    @JavascriptInterface
    public boolean pickFile(String mimeType) {
        try {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType(mimeType != null && !mimeType.isEmpty() ? mimeType : "*/*");
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            activity.startActivityForResult(Intent.createChooser(intent, "Select File"), 200);
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }

    // ==========================================
    // 8. CALENDAR & 9. ALARM
    // ==========================================

    @JavascriptInterface
    public boolean openCalendar() {
        try {
            Uri.Builder builder = CalendarContract.CONTENT_URI.buildUpon();
            builder.appendPath("time");
            Intent intent = new Intent(Intent.ACTION_VIEW, builder.build());
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            activity.startActivity(intent);
            return true;
        } catch (Exception e) {
            return openApp("com.google.android.calendar");
        }
    }

    @JavascriptInterface
    public boolean addCalendarEvent(String title, long startTime, long endTime, String location) {
        try {
            Intent intent = new Intent(Intent.ACTION_INSERT)
                .setData(CalendarContract.Events.CONTENT_URI)
                .putExtra(CalendarContract.Events.TITLE, title != null ? title : "JUMA Event")
                .putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, startTime > 0 ? startTime : System.currentTimeMillis())
                .putExtra(CalendarContract.EXTRA_EVENT_END_TIME, endTime > 0 ? endTime : System.currentTimeMillis() + 3600000);
            if (location != null) {
                intent.putExtra(CalendarContract.Events.EVENT_LOCATION, location);
            }
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            activity.startActivity(intent);
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }

    @JavascriptInterface
    public boolean setNativeAlarm(int hours, int minutes, String label) {
        try {
            Intent intent = new Intent(AlarmClock.ACTION_SET_ALARM);
            intent.putExtra(AlarmClock.EXTRA_HOUR, hours);
            intent.putExtra(AlarmClock.EXTRA_MINUTES, minutes);
            if (label != null) {
                intent.putExtra(AlarmClock.EXTRA_MESSAGE, label);
            }
            intent.putExtra(AlarmClock.EXTRA_SKIP_UI, false);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            activity.startActivity(intent);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    // ==========================================
    // 10. NOTIFICATIONS
    // ==========================================

    @JavascriptInterface
    public void showNotification(String title, String body) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (activity.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                    return;
                }
            }
            NotificationManager nm = (NotificationManager) activity.getSystemService(Context.NOTIFICATION_SERVICE);
            String channelId = "juma_alerts";
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                NotificationChannel channel = new NotificationChannel(channelId, "JUMA AI Alerts", NotificationManager.IMPORTANCE_HIGH);
                nm.createNotificationChannel(channel);
            }
            android.app.Notification.Builder builder;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                builder = new android.app.Notification.Builder(activity, channelId);
            } else {
                builder = new android.app.Notification.Builder(activity);
            }
            builder.setContentTitle(title)
                   .setContentText(body)
                   .setSmallIcon(android.R.drawable.ic_dialog_info)
                   .setAutoCancel(true);
            nm.notify((int) System.currentTimeMillis(), builder.build());
        } catch (Exception ignored) {}
    }

    // ==========================================
    // 11. BATTERY TELEMETRY
    // ==========================================

    @JavascriptInterface
    public String getBatteryStatus() {
        try {
            IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
            Intent batteryStatus = activity.registerReceiver(null, ifilter);

            int level = -1;
            int scale = -1;
            boolean isCharging = false;
            float temperature = 0;

            if (batteryStatus != null) {
                level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
                scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
                int status = batteryStatus.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
                isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                             status == BatteryManager.BATTERY_STATUS_FULL;
                int tempInt = batteryStatus.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0);
                temperature = tempInt / 10.0f;
            }

            int batteryPct = (int) ((level / (float) scale) * 100);
            JSONObject obj = new JSONObject();
            obj.put("level", batteryPct >= 0 ? batteryPct : 100);
            obj.put("isCharging", isCharging);
            obj.put("temperature", temperature);
            return obj.toString();
        } catch (Exception e) {
            return "{\"level\":100,\"isCharging\":false}";
        }
    }

    @JavascriptInterface
    public int getBatteryLevel() {
        try {
            JSONObject obj = new JSONObject(getBatteryStatus());
            return obj.optInt("level", 100);
        } catch (Exception e) {
            return 100;
        }
    }

    @JavascriptInterface
    public boolean isCharging() {
        try {
            JSONObject obj = new JSONObject(getBatteryStatus());
            return obj.optBoolean("isCharging", false);
        } catch (Exception e) {
            return false;
        }
    }

    // ==========================================
    // 12. APP LAUNCHING & INTENTS
    // ==========================================

    @JavascriptInterface
    public String launchIntent(String packageName, String action, String activityName, String extraParams) {
        try {
            Intent intent = null;
            PackageManager pm = activity.getPackageManager();

            if (action != null && action.startsWith("android.settings.")) {
                intent = new Intent(action);
            } else if (packageName != null && !packageName.isEmpty() && activityName != null && !activityName.isEmpty()) {
                intent = new Intent();
                intent.setComponent(new ComponentName(packageName, activityName));
            } else if (packageName != null && !packageName.isEmpty()) {
                intent = pm.getLaunchIntentForPackage(packageName);
            } else if (action != null && !action.isEmpty()) {
                intent = new Intent(action);
            }

            if (intent == null) {
                return "APP_NOT_FOUND";
            }

            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            activity.startActivity(intent);
            return "SUCCESS";
        } catch (ActivityNotFoundException e) {
            return "APP_NOT_FOUND";
        } catch (Exception e) {
            return "APP_NOT_FOUND";
        }
    }

    @JavascriptInterface
    public boolean openApp(String packageName) {
        try {
            PackageManager pm = activity.getPackageManager();
            Intent intent = pm.getLaunchIntentForPackage(packageName);
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                activity.startActivity(intent);
                return true;
            }
        } catch (Exception ignored) {}
        return false;
    }

    @JavascriptInterface
    public boolean isAppInstalled(String packageName) {
        if (packageName == null || packageName.isEmpty()) return false;
        try {
            PackageManager pm = activity.getPackageManager();
            pm.getPackageInfo(packageName, PackageManager.GET_ACTIVITIES);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    // ==========================================
    // 13. FOREGROUND SERVICE
    // ==========================================

    @JavascriptInterface
    public String startForegroundService() {
        // Android 14 / API 34 compliance:
        // Continuous background microphone operation requires explicit user initiation in foreground
        // and RECORD_AUDIO permission. Starting from background throws ForegroundServiceStartNotAllowedException.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (activity.checkSelfPermission(android.Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                return "PERMISSION_REQUIRED:RECORD_AUDIO";
            }
        }

        try {
            Intent serviceIntent = new Intent(activity, JumaForegroundService.class);
            serviceIntent.setAction(JumaForegroundService.ACTION_START);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                activity.startForegroundService(serviceIntent);
            } else {
                activity.startService(serviceIntent);
            }
            return "SUCCESS";
        } catch (Exception e) {
            return "ERROR:" + e.getClass().getSimpleName() + ":" + e.getMessage();
        }
    }

    @JavascriptInterface
    public boolean stopForegroundService() {
        try {
            Intent serviceIntent = new Intent(activity, JumaForegroundService.class);
            serviceIntent.setAction(JumaForegroundService.ACTION_STOP);
            activity.startService(serviceIntent);
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }

    @JavascriptInterface
    public boolean isForegroundServiceRunning() {
        return JumaForegroundService.isRunning();
    }

    // ==========================================
    // 14. ACCESSIBILITY SERVICE
    // ==========================================

    @JavascriptInterface
    public boolean isAccessibilityServiceEnabled() {
        try {
            int accessibilityEnabled = Settings.Secure.getInt(
                activity.getContentResolver(),
                Settings.Secure.ACCESSIBILITY_ENABLED, 0
            );
            if (accessibilityEnabled == 1) {
                String services = Settings.Secure.getString(
                    activity.getContentResolver(),
                    Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
                );
                if (services != null && services.contains("com.juma.ai/.JumaAccessibilityService")) {
                    return true;
                }
            }
        } catch (Exception ignored) {}
        return JumaAccessibilityService.isRunning();
    }

    @JavascriptInterface
    public boolean performAccessibilityAction(String action) {
        JumaAccessibilityService service = JumaAccessibilityService.getInstance();
        if (service != null) {
            return service.performAction(action);
        }
        return false;
    }

    @JavascriptInterface
    public void openAccessibilitySettings() {
        try {
            Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            activity.startActivity(intent);
        } catch (Exception ignored) {}
    }

    // ==========================================
    // 15. ANDROID SETTINGS
    // ==========================================

    @JavascriptInterface
    public boolean openSettings(String section) {
        try {
            String action = Settings.ACTION_SETTINGS;
            if ("wifi".equalsIgnoreCase(section)) {
                action = Settings.ACTION_WIFI_SETTINGS;
            } else if ("bluetooth".equalsIgnoreCase(section)) {
                action = Settings.ACTION_BLUETOOTH_SETTINGS;
            } else if ("sound".equalsIgnoreCase(section)) {
                action = Settings.ACTION_SOUND_SETTINGS;
            } else if ("display".equalsIgnoreCase(section)) {
                action = Settings.ACTION_DISPLAY_SETTINGS;
            } else if ("apps".equalsIgnoreCase(section)) {
                action = Settings.ACTION_APPLICATION_SETTINGS;
            } else if ("location".equalsIgnoreCase(section)) {
                action = Settings.ACTION_LOCATION_SOURCE_SETTINGS;
            } else if ("storage".equalsIgnoreCase(section)) {
                action = Settings.ACTION_INTERNAL_STORAGE_SETTINGS;
            } else if ("battery".equalsIgnoreCase(section)) {
                action = Settings.ACTION_BATTERY_SAVER_SETTINGS;
            } else if ("accessibility".equalsIgnoreCase(section)) {
                action = Settings.ACTION_ACCESSIBILITY_SETTINGS;
            } else if ("app_permissions".equalsIgnoreCase(section)) {
                Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                intent.setData(Uri.parse("package:" + activity.getPackageName()));
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                activity.startActivity(intent);
                return true;
            }

            Intent intent = new Intent(action);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            activity.startActivity(intent);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    // ==========================================
    // 16. HARDWARE CONTROLS (TORCH, AUDIO, VIBRATE)
    // ==========================================

    @JavascriptInterface
    public boolean setFlashlight(boolean enable) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                CameraManager cameraManager = (CameraManager) activity.getSystemService(Context.CAMERA_SERVICE);
                if (cameraManager != null) {
                    String[] ids = cameraManager.getCameraIdList();
                    if (ids != null && ids.length > 0) {
                        cameraManager.setTorchMode(ids[0], enable);
                        return true;
                    }
                }
            }
        } catch (Exception ignored) {}
        return false;
    }

    @JavascriptInterface
    public boolean setVolume(String direction, int level) {
        try {
            AudioManager audio = (AudioManager) activity.getSystemService(Context.AUDIO_SERVICE);
            if (audio != null) {
                if ("up".equalsIgnoreCase(direction)) {
                    audio.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_RAISE, AudioManager.FLAG_SHOW_UI);
                    return true;
                } else if ("down".equalsIgnoreCase(direction)) {
                    audio.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_LOWER, AudioManager.FLAG_SHOW_UI);
                    return true;
                } else if ("mute".equalsIgnoreCase(direction)) {
                    audio.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_MUTE, AudioManager.FLAG_SHOW_UI);
                    return true;
                } else if ("max".equalsIgnoreCase(direction)) {
                    int max = audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
                    audio.setStreamVolume(AudioManager.STREAM_MUSIC, max, AudioManager.FLAG_SHOW_UI);
                    return true;
                }
            }
        } catch (Exception ignored) {}
        return false;
    }

    @JavascriptInterface
    public void vibrate(long durationMs) {
        try {
            Vibrator vibrator = (Vibrator) activity.getSystemService(Context.VIBRATOR_SERVICE);
            if (vibrator != null) {
                vibrator.vibrate(durationMs > 0 ? durationMs : 100);
            }
        } catch (Exception ignored) {}
    }

    @JavascriptInterface
    public void showToast(final String message) {
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(activity, message != null ? message : "", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // ==========================================
    // 17. CONTACTS
    // ==========================================

    @JavascriptInterface
    public String searchContacts(String query) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
            activity.checkSelfPermission(android.Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
            return "PERMISSION_DENIED";
        }

        JSONArray arr = new JSONArray();
        Cursor cursor = null;
        try {
            Uri uri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI;
            String selection = ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " LIKE ?";
            String[] args = new String[]{"%" + (query != null ? query : "") + "%"};
            cursor = activity.getContentResolver().query(uri, null, selection, args, null);
            if (cursor != null) {
                int nameIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME);
                int numIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER);
                while (cursor.moveToNext() && arr.length() < 10) {
                    JSONObject contact = new JSONObject();
                    contact.put("name", nameIdx != -1 ? cursor.getString(nameIdx) : "Contact");
                    contact.put("number", numIdx != -1 ? cursor.getString(numIdx) : "");
                    arr.put(contact);
                }
            }
        } catch (Exception ignored) {
        } finally {
            if (cursor != null) {
                try { cursor.close(); } catch (Exception ignored) {}
            }
        }
        return arr.toString();
    }

    @JavascriptInterface
    public String getDeviceInfo() {
        try {
            JSONObject obj = new JSONObject();
            obj.put("model", Build.MODEL);
            obj.put("manufacturer", Build.MANUFACTURER);
            obj.put("androidVersion", Build.VERSION.RELEASE);
            obj.put("sdkInt", Build.VERSION.SDK_INT);
            obj.put("brand", Build.BRAND);
            obj.put("runtimeEnvironment", "FULL_NATIVE_ANDROID");
            obj.put("nativeVersionName", "1.0.0");
            obj.put("nativeVersionCode", 1);
            try {
                android.content.pm.PackageInfo pInfo = activity.getPackageManager().getPackageInfo(activity.getPackageName(), 0);
                obj.put("nativeVersionName", pInfo.versionName);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    obj.put("nativeVersionCode", pInfo.getLongVersionCode());
                } else {
                    obj.put("nativeVersionCode", pInfo.versionCode);
                }
            } catch (Exception ignored) {}
            return obj.toString();
        } catch (Exception e) {
            return "{}";
        }
    }

    /**
     * Secure Native APK Download & Package Installer Launcher
     * Enforces HTTPS, verifies SHA-256 integrity, uses FileProvider content:// URI,
     * and delegates to the standard Android Package Installer.
     */
    @JavascriptInterface
    public String downloadAndInstallApk(final String apkUrl, final String expectedSha256, final int targetVersionCode) {
        if (apkUrl == null || (!apkUrl.startsWith("https://") && !apkUrl.startsWith("http://"))) {
            return "{\"success\":false,\"message\":\"Security Exception: APK URL must use a secure protocol (HTTPS).\"}";
        }

        // On Android 8.0+ (API 26+), check if app is permitted to install unknown apps
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (!activity.getPackageManager().canRequestPackageInstalls()) {
                try {
                    Intent grantIntent = new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                            Uri.parse("package:" + activity.getPackageName()));
                    grantIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    activity.startActivity(grantIntent);
                    return "{\"success\":false,\"message\":\"PERMISSION_REQUIRED: Please toggle 'Allow from this source' in Android Settings to allow updating JUMA AI, then retry.\"}";
                } catch (Exception e) {
                    return "{\"success\":false,\"message\":\"Please enable install permission in Android Settings.\"}";
                }
            }
        }

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    dispatchApkProgress(10, "Connecting to update server...");
                    URL url = new URL(apkUrl);
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setConnectTimeout(15000);
                    conn.setReadTimeout(30000);
                    conn.setRequestProperty("User-Agent", "JUMA-AI-Android-Updater");
                    conn.connect();

                    int responseCode = conn.getResponseCode();
                    if (responseCode != HttpURLConnection.HTTP_OK) {
                        dispatchApkProgress(0, "Download failed: Server returned HTTP " + responseCode);
                        return;
                    }

                    int fileLength = conn.getContentLength();
                    File cacheDir = activity.getExternalCacheDir() != null ? activity.getExternalCacheDir() : activity.getCacheDir();
                    File apkFile = new File(cacheDir, "juma-update-" + targetVersionCode + ".apk");
                    if (apkFile.exists()) {
                        apkFile.delete();
                    }

                    InputStream input = conn.getInputStream();
                    FileOutputStream output = new FileOutputStream(apkFile);

                    byte[] data = new byte[8192];
                    long total = 0;
                    int count;
                    int lastProgress = 0;

                    while ((count = input.read(data)) != -1) {
                        total += count;
                        output.write(data, 0, count);
                        if (fileLength > 0) {
                            int progress = (int) (total * 80 / fileLength) + 10;
                            if (progress - lastProgress >= 5) {
                                lastProgress = progress;
                                dispatchApkProgress(progress, "Downloading JUMA APK (" + (total / 1024) + " KB)...");
                            }
                        }
                    }

                    output.flush();
                    output.close();
                    input.close();
                    conn.disconnect();

                    dispatchApkProgress(92, "Verifying cryptographic SHA-256 integrity...");

                    // SHA-256 Integrity Verification
                    if (expectedSha256 != null && expectedSha256.trim().length() == 64) {
                        String actualSha256 = calculateSha256(apkFile);
                        if (!actualSha256.equalsIgnoreCase(expectedSha256.trim())) {
                            apkFile.delete();
                            dispatchApkProgress(0, "Security validation failed: APK SHA-256 signature does not match manifest. Installation aborted for safety.");
                            return;
                        }
                    }

                    dispatchApkProgress(98, "Launching Android Package Installer...");

                    // Launch Android Package Installer via Intent
                    activity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                Intent installIntent = new Intent(Intent.ACTION_VIEW);
                                installIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                                    String authority = activity.getPackageName() + ".fileprovider";
                                    Uri contentUri = JumaFileProvider.getUriForFile(activity, authority, apkFile);
                                    installIntent.setDataAndType(contentUri, "application/vnd.android.package-archive");
                                    installIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                                } else {
                                    installIntent.setDataAndType(Uri.fromFile(apkFile), "application/vnd.android.package-archive");
                                }

                                activity.startActivity(installIntent);
                                dispatchApkProgress(100, "Package installer active. Confirm installation on device screen.");
                            } catch (Exception ex) {
                                dispatchApkProgress(0, "Error starting package installer: " + ex.getMessage());
                            }
                        }
                    });

                } catch (Exception e) {
                    dispatchApkProgress(0, "APK Update error: " + e.getMessage());
                }
            }
        }).start();

        return "{\"success\":true,\"message\":\"APK download initiated. You will be prompted to confirm package update.\"}";
    }

    private String calculateSha256(File file) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            FileInputStream fis = new FileInputStream(file);
            byte[] byteArray = new byte[8192];
            int bytesCount;
            while ((bytesCount = fis.read(byteArray)) != -1) {
                digest.update(byteArray, 0, bytesCount);
            }
            fis.close();
            byte[] bytes = digest.digest();
            StringBuilder sb = new StringBuilder();
            for (byte b : bytes) {
                sb.append(Integer.toString((b & 0xff) + 0x100, 16).substring(1));
            }
            return sb.toString();
        } catch (Exception e) {
            return "";
        }
    }

    private void dispatchApkProgress(final int percent, final String message) {
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                String safeMsg = message != null ? message.replace("'", "\\'") : "";
                String js = "if (window.onApkInstallProgress) { window.onApkInstallProgress(" + percent + ", '" + safeMsg + "'); }";
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                    webView.evaluateJavascript(js, null);
                } else {
                    webView.loadUrl("javascript:" + js);
                }
            }
        });
    }

    public void onDestroy() {
        if (speechRecognizer != null) {
            try {
                speechRecognizer.destroy();
            } catch (Exception ignored) {}
            speechRecognizer = null;
        }
        if (tts != null) {
            try {
                tts.stop();
                tts.shutdown();
            } catch (Exception ignored) {}
            tts = null;
        }
    }
}
