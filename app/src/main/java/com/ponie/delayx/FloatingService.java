package com.ponie.delayx;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.text.InputFilter;
import android.text.InputType;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.text.DecimalFormat;

public class FloatingService extends Service {

    private static final String CH_ID = "gbf";
    private static final int NOTIF_ID = 2;
    // 7 màu neon hiện đại cho nút trong game
    private static final int[] NEON_COLORS = {
        0xFFFF0055, // Hồng neon
        0xFFFF6B00, // Cam
        0xFFFFD600, // Vàng
        0xFF00FF66, // Xanh lá
        0xFF00E5FF, // Cyan
        0xFF7B61FF, // Tím
        0xFFFF00E5  // Magenta
    };
    private static final int FLOAT_BUTTON_OFF_FILL = 0xCC0D0D1A;
    private static final int FLOAT_BUTTON_ON_FILL  = 0xCC001A12;
    private static final String MASK_BRAND_TEXT = "👑Mướp MOD TEAM👑";
    private static final String PREFS = "DelayxPrefs";
    private static final String ACTION_APPLY_FLOAT_APPEARANCE = "com.ponie.delayx.APPLY_FLOAT_APPEARANCE";
    
    private boolean onStandby = true;
    private boolean onFreeze = false;
    private boolean onGhost = false;
    private boolean onTeleSpeed = false;
    
    private boolean addedF = false;
    private boolean addedG = false;
    private boolean addedTS = false;
    private boolean addedBanner = false;
    
    private boolean isLocked1 = false;
    private boolean isLocked2 = false;
    private boolean isLocked3 = false;

    private int timerMode1 = 0;
    private int timerMode2 = 0;
    private int timerMode3 = 0;

    private long endTime1 = 0L;
    private long endTime2 = 0L;
    private long endTime3 = 0L;
    
    private long lastClickTime = 0L; 

    private Handler timerHandler = new Handler(Looper.getMainLooper());
    private Runnable timerRunnable1, timerRunnable2, timerRunnable3;
    
    private TextView btnFreeze;
    private TextView btnGhost;
    private TextView btnTeleSpeed;
    private TextView topBanner;
    private WindowManager wm;
    private WindowManager.LayoutParams freezeParams;
    private WindowManager.LayoutParams ghostParams;
    private WindowManager.LayoutParams teleSpeedParams;
    private BroadcastReceiver rcv;
    private Typeface floatingButtonFont;
    
    private SoundPool soundPool;
    private int soundOnId = 0;
    private int soundOffId = 0;
    
    private int size1 = 110, size2 = 110, size3 = 110; 
    private int labelFontSize = 18; 
    private int countdownFontSize = 22; 
    private int alpha1 = 100, alpha2 = 100, alpha3 = 100;
    private boolean vis1 = true, vis2 = true, vis3 = true;
    private boolean snd1 = false, snd2 = false, snd3 = false;
    private int posX1 = 18, posY1 = 110;
    private int posX2 = 156, posY2 = 110;
    private int posX3 = 156, posY3 = 248;

    private View configPopupView = null;
    private int popupIconIdForLock = -1; 
    private WindowManager.LayoutParams configPopupParams = null;
    
    private DecimalFormat decimalFormat = new DecimalFormat("0");

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) { 
        return START_STICKY; 
    }

    @Override
    public void onCreate() {
        super.onCreate();
        
        if (LoginActivity.key == 0) {
            stopSelf();
            throw new RuntimeException("Hệ Thống Quét Chống Bug!");
        }

        doForeground();
        wm = (WindowManager) getSystemService(WINDOW_SERVICE);
        initSound(); 
        
        try { 
            floatingButtonFont = Typeface.createFromAsset(getAssets(), "fonts/chudep.ttf"); 
        } catch (Exception e) { 
            floatingButtonFont = Typeface.DEFAULT_BOLD; 
        }

        SharedPreferences prefs = getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        
        size1 = prefs.getInt("iconSize1", 110); 
        size2 = prefs.getInt("iconSize2", 110); 
        size3 = prefs.getInt("iconSize3", 110);
        
        alpha1 = prefs.getInt("iconOpacity1", 100); 
        alpha2 = prefs.getInt("iconOpacity2", 100); 
        alpha3 = prefs.getInt("iconOpacity3", 100);
        
        vis1 = prefs.getBoolean("iconVisible1", true); 
        vis2 = prefs.getBoolean("iconVisible2", true); 
        vis3 = prefs.getBoolean("iconVisible3", true);
        
        snd1 = prefs.getBoolean("iconSound1", false); 
        snd2 = prefs.getBoolean("iconSound2", false); 
        snd3 = prefs.getBoolean("iconSound3", false);
        
        posX1 = prefs.getInt("iconPosX1", dpToPx(18)); 
        posY1 = prefs.getInt("iconPosY1", dpToPx(110));
        posX2 = prefs.getInt("iconPosX2", dpToPx(156)); 
        posY2 = prefs.getInt("iconPosY2", dpToPx(110));
        posX3 = prefs.getInt("iconPosX3", dpToPx(156)); 
        posY3 = prefs.getInt("iconPosY3", dpToPx(248));

        onFreeze = prefs.getBoolean("stateFreeze", false); 
        onGhost = prefs.getBoolean("stateGhost", false); 
        onTeleSpeed = prefs.getBoolean("stateTeleSpeed", false);
        
        isLocked1 = prefs.getBoolean("isLocked1", false); 
        isLocked2 = prefs.getBoolean("isLocked2", false); 
        isLocked3 = prefs.getBoolean("isLocked3", false);

        timerMode1 = prefs.getInt("timerMode1", 0); 
        timerMode2 = prefs.getInt("timerMode2", 0); 
        timerMode3 = prefs.getInt("timerMode3", 0);

        topBanner = makeTopBanner();
        syncFloatingButtonVisibility();
        doRegister();
    }

    @Override
    public IBinder onBind(Intent intent) { 
        return null; 
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        saveAllStates();
        try { 
            if (rcv != null) unregisterReceiver(rcv); 
        } catch (Exception e) { }
        
        rm(btnFreeze, addedF); 
        rm(btnGhost, addedG); 
        rm(btnTeleSpeed, addedTS); 
        rm(topBanner, addedBanner);
        closeConfigPopup();
        
        if (soundPool != null) {
            soundPool.release();
            soundPool = null;
        }
        timerHandler.removeCallbacksAndMessages(null);
    }
    
    private void saveAllStates() {
        SharedPreferences prefs = getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        
        if (freezeParams != null) { 
            editor.putInt("iconPosX1", freezeParams.x); 
            editor.putInt("iconPosY1", freezeParams.y); 
        }
        if (ghostParams != null) { 
            editor.putInt("iconPosX2", ghostParams.x); 
            editor.putInt("iconPosY2", ghostParams.y); 
        }
        if (teleSpeedParams != null) { 
            editor.putInt("iconPosX3", teleSpeedParams.x); 
            editor.putInt("iconPosY3", teleSpeedParams.y); 
        }
        
        editor.putBoolean("stateFreeze", onFreeze); 
        editor.putBoolean("stateGhost", onGhost); 
        editor.putBoolean("stateTeleSpeed", onTeleSpeed);
        editor.putBoolean("isLocked1", isLocked1); 
        editor.putBoolean("isLocked2", isLocked2); 
        editor.putBoolean("isLocked3", isLocked3);
        editor.putInt("timerMode1", timerMode1); 
        editor.putInt("timerMode2", timerMode2); 
        editor.putInt("timerMode3", timerMode3);
        
        editor.apply();
    }
    
    private void doForeground() {
        try {
            if (Build.VERSION.SDK_INT >= 26) {
                NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
                if (nm != null) {
                    NotificationChannel ch = new NotificationChannel(CH_ID, "FloatingService", NotificationManager.IMPORTANCE_MIN);
                    ch.setSound(null, null); 
                    nm.createNotificationChannel(ch);
                }
                Notification n = new Notification.Builder(this, CH_ID)
                        .setContentTitle("👑Mướp MOD TEAM👑")
                        .setContentText("FloatingService Đang Chạy!")
                        .setSmallIcon(0x1080039).build();
                startForeground(NOTIF_ID, n);
            } else {
                Notification n = new Notification.Builder(this)
                        .setContentTitle("👑Mướp MOD TEAM👑")
                        .setContentText("FloatingService Đang Chạy!")
                        .setSmallIcon(0x1080039).build();
                startForeground(NOTIF_ID, n);
            }
        } catch (Exception e) { 
            try { 
                Notification n = new Notification(); 
                n.icon = 0x1080039; 
                startForeground(NOTIF_ID, n); 
            } catch (Exception e2) { } 
        }
    }

    private void doRegister() {
        rcv = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent == null) return;
                String action = intent.getAction();
                if (ACTION_APPLY_FLOAT_APPEARANCE.equals(action)) { 
                    applyAppearanceIntent(intent); 
                    return; 
                }
                if (!MyVpnService.BC_SYNC.equals(action)) return;

                boolean isRunning = intent.getBooleanExtra("_r", false);
                boolean teleActive = intent.getBooleanExtra("_to", false);
                boolean ghostState = intent.getBooleanExtra("ghost", false);
                boolean freezeState = intent.getBooleanExtra("freeze", false);
                
                if (!isRunning) {
                    if (onStandby || onFreeze || onGhost || onTeleSpeed) {
                        onStandby = false;
                        onFreeze = false;
                        onGhost = false;
                        onTeleSpeed = false;
                        saveAllStates();
                        
                        if (timerRunnable1 != null) timerHandler.removeCallbacks(timerRunnable1);
                        if (timerRunnable2 != null) timerHandler.removeCallbacks(timerRunnable2);
                        if (timerRunnable3 != null) timerHandler.removeCallbacks(timerRunnable3);
                        
                        if (btnFreeze != null) paintBtn(btnFreeze, "Freeze", false, alpha1, fontSizeForButton(size1), 2);
                        if (btnGhost != null) paintBtn(btnGhost, "Ghost", false, alpha2, fontSizeForButton(size2), 3);
                        if (btnTeleSpeed != null) paintBtn(btnTeleSpeed, "Teleport", false, alpha3, fontSizeForButton(size3), 7);
                    }
                } else {
                    onStandby = true;
                    if (System.currentTimeMillis() - lastClickTime > 1500) {
                        if (onFreeze != freezeState) {
                            onFreeze = freezeState;
                            if (!onFreeze && timerRunnable1 != null) timerHandler.removeCallbacks(timerRunnable1);
                            if (btnFreeze != null) paintBtn(btnFreeze, "Freeze", onFreeze, alpha1, fontSizeForButton(size1), 2);
                        }
                        if (onGhost != ghostState) {
                            onGhost = ghostState;
                            if (!onGhost && timerRunnable2 != null) timerHandler.removeCallbacks(timerRunnable2);
                            if (btnGhost != null) paintBtn(btnGhost, "Ghost", onGhost, alpha2, fontSizeForButton(size2), 3);
                        }
                        if (onTeleSpeed != teleActive) {
                            onTeleSpeed = teleActive;
                            if (!onTeleSpeed && timerRunnable3 != null) timerHandler.removeCallbacks(timerRunnable3);
                            if (btnTeleSpeed != null) paintBtn(btnTeleSpeed, "Teleport", onTeleSpeed, alpha3, fontSizeForButton(size3), 7);
                        }
                    }
                }
            }
        };
        try {
            IntentFilter filter = new IntentFilter(); 
            filter.addAction(MyVpnService.BC_SYNC); 
            filter.addAction(ACTION_APPLY_FLOAT_APPEARANCE);
            if (Build.VERSION.SDK_INT >= 33) {
                registerReceiver(rcv, filter, Context.RECEIVER_NOT_EXPORTED); 
            } else { 
                registerReceiver(rcv, filter); 
            }
        } catch (Exception e) { }
    }

    private void initSound() {
        try {
            if (Build.VERSION.SDK_INT >= 21) {
                AudioAttributes audioAttributes = new AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build();
                soundPool = new SoundPool.Builder()
                        .setMaxStreams(3)
                        .setAudioAttributes(audioAttributes)
                        .build();
            } else {
                soundPool = new SoundPool(3, AudioManager.STREAM_MUSIC, 0);
            }
            soundOnId = soundPool.load(this, R.raw.sound_on, 1);
            soundOffId = soundPool.load(this, R.raw.sound_off, 1);
        } catch (Exception e) {
            soundPool = null;
            soundOnId = 0;
            soundOffId = 0;
        }
    }
    
    private void playSound(boolean isOn) {
        if (soundPool == null) return;
        int soundId = isOn ? soundOnId : soundOffId;
        if (soundId == 0) return;
        try {
            soundPool.play(soundId, 1.0f, 1.0f, 1, 0, 1.0f);
        } catch (Exception e) { }
    }

    /** Font size theo kích thước nút – nhỏ nút vẫn còn chữ */
    private int fontSizeForButton(int sizeDp) {
        // size 110dp → ~18sp; size 60dp → ~11sp; tối thiểu 9sp
        int sp = Math.round(sizeDp * 18f / 110f);
        if (sp < 9) sp = 9;
        if (sp > 20) sp = 20;
        return sp;
    }

    private int paddingForButton(int sizeDp) {
        int p = Math.round(sizeDp * 8f / 110f);
        if (p < 2) p = 2;
        if (p > 10) p = 10;
        return dpToPx(p);
    }

    private TextView makeBtn(String label, int x, int y, final int id, int sizeDp, int alphaPrc) {
        TextView tv = new TextView(this);
        tv.setText(label); 
        tv.setTextColor(Color.WHITE); 
        tv.setAlpha(alphaPrc / 100f);

        int sizePx = dpToPx(sizeDp); 
        int dynamicFont = fontSizeForButton(sizeDp);
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, dynamicFont); 

        tv.setTypeface(floatingButtonFont != null ? floatingButtonFont : Typeface.DEFAULT_BOLD);
        tv.setGravity(Gravity.CENTER); 
        tv.setSingleLine(false); 
        tv.setMaxLines(2);
        tv.setClickable(true); 
        tv.setLongClickable(true); 
        tv.setAllCaps(false); 
        tv.setIncludeFontPadding(false);
        tv.setEllipsize(null);
        
        if (Build.VERSION.SDK_INT >= 21) {
            tv.setElevation(0f);
        }
        
        int innerPadding = paddingForButton(sizeDp); 
        tv.setPadding(innerPadding, innerPadding, innerPadding, innerPadding);
        
        boolean initOn = false;
        if (id == 2) initOn = onFreeze; 
        else if (id == 3) initOn = onGhost; 
        else if (id == 7) initOn = onTeleSpeed;
        
        tv.setBackground(getButtonStyle(initOn, id));
        tv.setShadowLayer(6f, 0f, 0f, Color.argb(180, 0, 255, 255));

        // Bỏ cờ FLAG_HARDWARE_ACCELERATED thừa thãi giúp giảm vắt kiệt GPU cho Floating window nhỏ
        int type = (Build.VERSION.SDK_INT >= 26) ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY : WindowManager.LayoutParams.TYPE_PHONE;
        final WindowManager.LayoutParams p = new WindowManager.LayoutParams(
                sizePx, sizePx, type,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL | WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH | WindowManager.LayoutParams.FLAG_SPLIT_TOUCH,
                PixelFormat.TRANSLUCENT
        );
        p.gravity = Gravity.TOP | Gravity.LEFT; 
        p.x = x; 
        p.y = y;

        try { wm.addView(tv, p); } catch (Exception e) { return tv; }

        if (id == 2) { addedF = true; freezeParams = p; btnFreeze = tv; }
        else if (id == 3) { addedG = true; ghostParams = p; btnGhost = tv; }
        else if (id == 7) { addedTS = true; teleSpeedParams = p; btnTeleSpeed = tv; }

        tv.setOnTouchListener(new SrcVipButtonTouchListener(p, tv, id));
        return tv;
    }

    private Drawable getButtonStyle(boolean isOn, int buttonId) {
        GradientDrawable bg = new GradientDrawable();
        bg.setShape(GradientDrawable.OVAL);
        bg.setColor(isOn ? FLOAT_BUTTON_ON_FILL : FLOAT_BUTTON_OFF_FILL);

        // Chọn màu neon theo nút (7 màu khác nhau)
        int colorIndex;
        if (buttonId == 2) colorIndex = 0;      // Freeze - Hồng
        else if (buttonId == 3) colorIndex = 3; // Ghost - Xanh lá
        else colorIndex = 4;                    // Teleport - Cyan

        int strokeColor = isOn ? NEON_COLORS[colorIndex] : NEON_COLORS[(colorIndex + 3) % 7];
        // Viền dày hơn + màu neon rõ để đẹp và nổi bật
        bg.setStroke(dpToPx(3), strokeColor);
        return bg;
    }

    private Drawable getButtonStyle(boolean isOn) {
        return getButtonStyle(isOn, 2);
    }

    private TextView makeTopBanner() {
        TextView tv = new TextView(this);
        tv.setText(MASK_BRAND_TEXT); 
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15); 
        tv.setTypeface(floatingButtonFont != null ? floatingButtonFont : Typeface.DEFAULT_BOLD); 
        tv.setSingleLine(true); 
        tv.setGravity(Gravity.CENTER); 
        tv.setAlpha(0.98f);
        tv.setPadding(dpToPx(18), dpToPx(9), dpToPx(18), dpToPx(9)); 

        // Nền kính mờ + viền neon đẹp hơn
        GradientDrawable bannerBg = new GradientDrawable();
        bannerBg.setColor(0xCC0A0A14);
        bannerBg.setCornerRadius(dpToPx(20));
        bannerBg.setStroke(dpToPx(1), 0xFF00E5FF);
        tv.setBackground(bannerBg);

        tv.setTextColor(0xFF00FFF0); 
        tv.setShadowLayer(12f, 0f, 0f, Color.argb(200, 0, 255, 240));

        int type = (Build.VERSION.SDK_INT >= 26) ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY : WindowManager.LayoutParams.TYPE_PHONE;
        int flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN;
        WindowManager.LayoutParams p = new WindowManager.LayoutParams(WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT, type, flags, PixelFormat.TRANSLUCENT);
        p.gravity = Gravity.TOP | Gravity.CENTER_HORIZONTAL; 
        p.x = 0; 
        p.y = dpToPx(10);
        try { 
            wm.addView(tv, p); 
            addedBanner = true; 
        } catch (Exception e) { }
        return tv;
    }

    private int dpToPx(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return Math.max(1, Math.round(dp * density));
    }

    private void applyAppearanceIntent(Intent intent) {
        if (intent == null) return;
        size1 = intent.getIntExtra("size1", size1); 
        size2 = intent.getIntExtra("size2", size2); 
        size3 = intent.getIntExtra("size3", size3);
        alpha1 = intent.getIntExtra("alpha1", alpha1); 
        alpha2 = intent.getIntExtra("alpha2", alpha2); 
        alpha3 = intent.getIntExtra("alpha3", alpha3);
        vis1 = intent.getBooleanExtra("vis1", vis1); 
        vis2 = intent.getBooleanExtra("vis2", vis2); 
        vis3 = intent.getBooleanExtra("vis3", vis3);
        snd1 = intent.getBooleanExtra("snd1", snd1); 
        snd2 = intent.getBooleanExtra("snd2", snd2); 
        snd3 = intent.getBooleanExtra("snd3", snd3);
        
        syncFloatingButtonVisibility();
        updateButtonLayout(btnFreeze, freezeParams, 2, onFreeze, size1, alpha1); 
        updateButtonLayout(btnGhost, ghostParams, 3, onGhost, size2, alpha2); 
        updateButtonLayout(btnTeleSpeed, teleSpeedParams, 7, onTeleSpeed, size3, alpha3);
    }

    private void syncFloatingButtonVisibility() {
        if (vis1) {
            if (btnFreeze == null) btnFreeze = makeBtn("Freeze", posX1, posY1, 2, size1, alpha1);
            else if (freezeParams != null) try { wm.updateViewLayout(btnFreeze, freezeParams); } catch (Exception e) { }
            paintBtn(btnFreeze, "Freeze", onFreeze, alpha1, fontSizeForButton(size1), 2); 
            updateButtonLayout(btnFreeze, freezeParams, 2, onFreeze, size1, alpha1);
        } else {
            if (freezeParams != null) { saveAllStates(); rm(btnFreeze, addedF); btnFreeze = null; freezeParams = null; addedF = false; }
        }

        if (vis2) {
            if (btnGhost == null) btnGhost = makeBtn("Ghost", posX2, posY2, 3, size2, alpha2);
            else if (ghostParams != null) try { wm.updateViewLayout(btnGhost, ghostParams); } catch (Exception e) { }
            paintBtn(btnGhost, "Ghost", onGhost, alpha2, fontSizeForButton(size2), 3); 
            updateButtonLayout(btnGhost, ghostParams, 3, onGhost, size2, alpha2);
        } else {
            if (ghostParams != null) { saveAllStates(); rm(btnGhost, addedG); btnGhost = null; ghostParams = null; addedG = false; }
        }

        if (vis3) {
            if (btnTeleSpeed == null) btnTeleSpeed = makeBtn("Teleport", posX3, posY3, 7, size3, alpha3);
            else if (teleSpeedParams != null) try { wm.updateViewLayout(btnTeleSpeed, teleSpeedParams); } catch (Exception e) { }
            paintBtn(btnTeleSpeed, "Teleport", onTeleSpeed, alpha3, fontSizeForButton(size3), 7); 
            updateButtonLayout(btnTeleSpeed, teleSpeedParams, 7, onTeleSpeed, size3, alpha3);
        } else {
            if (teleSpeedParams != null) { saveAllStates(); rm(btnTeleSpeed, addedTS); btnTeleSpeed = null; teleSpeedParams = null; addedTS = false; }
        }
    }

    private void updateButtonLayout(TextView tv, WindowManager.LayoutParams params, int id, boolean isOn, int sizeDp, int alphaPrc) {
        if (tv == null || params == null) return;
        int sizePx = dpToPx(sizeDp); 
        params.width = sizePx; 
        params.height = sizePx;
        int dynamicFont = fontSizeForButton(sizeDp);
        int pad = paddingForButton(sizeDp);
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, dynamicFont);
        tv.setPadding(pad, pad, pad, pad);
        tv.setAlpha(alphaPrc / 100f);
        try { wm.updateViewLayout(tv, params); } catch (Exception e) { }
        paintBtn(tv, getButtonLabel(id), isOn, alphaPrc, dynamicFont, id);
    }

    private String getButtonLabel(int id) {
        if (id == 2) return "Freeze"; 
        if (id == 3) return "Ghost"; 
        if (id == 7) return "Teleport"; 
        return "";
    }

    private void paintBtn(TextView tv, String label, boolean on, int alphaPrc, int fontSizeSp) {
        paintBtn(tv, label, on, alphaPrc, fontSizeSp, 2);
    }

    private void paintBtn(TextView tv, String label, boolean on, int alphaPrc, int fontSizeSp, int buttonId) {
        if (tv == null) return;
        tv.setAlpha(alphaPrc / 100f);
        // Chữ trắng + shadow neon nhẹ cho đẹp hơn
        tv.setTextColor(Color.WHITE);
        tv.setShadowLayer(6f, 0f, 0f, Color.argb(180, 0, 255, 255));
        tv.setBackground(getButtonStyle(on, buttonId));
        tv.setText(label);
        tv.setGravity(Gravity.CENTER);
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, fontSizeSp);
        tv.setTypeface(floatingButtonFont != null ? floatingButtonFont : Typeface.DEFAULT_BOLD);
    }

    private String formatCountdown(float seconds) {
        return decimalFormat.format(Math.ceil(seconds)) + "s";
    }

    private void startTimerAutoOff(final int id) {
        int timerVal = (id == 2) ? timerMode1 : ((id == 3) ? timerMode2 : timerMode3);
        if (timerVal <= 0) return;

        // Giảm tần số gọi đếm ngược từ 200ms lên 500ms để giảm tải CPU đáng kể
        if (id == 2) {
            endTime1 = System.currentTimeMillis() + (timerVal * 1000L);
            if (timerRunnable1 != null) timerHandler.removeCallbacks(timerRunnable1);
            final int cdFont1 = Math.min(22, fontSizeForButton(size1) + 2);
            timerRunnable1 = new Runnable() {
                @Override public void run() {
                    if (!onFreeze) return;
                    long remain = endTime1 - System.currentTimeMillis();
                    if (remain <= 50) { 
                        click(2); 
                    } else {
                        float secs = remain / 1000f;
                        if (btnFreeze != null) {
                            btnFreeze.setText(formatCountdown(secs));
                            btnFreeze.setTextSize(TypedValue.COMPLEX_UNIT_SP, cdFont1);
                        }
                        timerHandler.postDelayed(this, 500); 
                    }
                }
            };
            paintBtn(btnFreeze, formatCountdown(timerVal), true, alpha1, cdFont1, 2); 
            timerHandler.postDelayed(timerRunnable1, 500);

        } else if (id == 3) {
            endTime2 = System.currentTimeMillis() + (timerVal * 1000L);
            if (timerRunnable2 != null) timerHandler.removeCallbacks(timerRunnable2);
            final int cdFont2 = Math.min(22, fontSizeForButton(size2) + 2);
            timerRunnable2 = new Runnable() {
                @Override public void run() {
                    if (!onGhost) return;
                    long remain = endTime2 - System.currentTimeMillis();
                    if (remain <= 50) { 
                        click(3); 
                    } else { 
                        float secs = remain / 1000f;
                        if (btnGhost != null) {
                            btnGhost.setText(formatCountdown(secs));
                            btnGhost.setTextSize(TypedValue.COMPLEX_UNIT_SP, cdFont2);
                        }
                        timerHandler.postDelayed(this, 500); 
                    }
                }
            };
            paintBtn(btnGhost, formatCountdown(timerVal), true, alpha2, cdFont2, 3); 
            timerHandler.postDelayed(timerRunnable2, 500);

        } else if (id == 7) {
            endTime3 = System.currentTimeMillis() + (timerVal * 1000L);
            if (timerRunnable3 != null) timerHandler.removeCallbacks(timerRunnable3);
            final int cdFont3 = Math.min(22, fontSizeForButton(size3) + 2);
            timerRunnable3 = new Runnable() {
                @Override public void run() {
                    if (!onTeleSpeed) return;
                    long remain = endTime3 - System.currentTimeMillis();
                    if (remain <= 50) { 
                        click(7); 
                    } else { 
                        float secs = remain / 1000f;
                        if (btnTeleSpeed != null) {
                            btnTeleSpeed.setText(formatCountdown(secs));
                            btnTeleSpeed.setTextSize(TypedValue.COMPLEX_UNIT_SP, cdFont3);
                        }
                        timerHandler.postDelayed(this, 500); 
                    }
                }
            };
            paintBtn(btnTeleSpeed, formatCountdown(timerVal), true, alpha3, cdFont3, 7); 
            timerHandler.postDelayed(timerRunnable3, 500);
        }
    }

    private void sendToMyVpn(String action, boolean enabled) {
        try {
            Intent intent = new Intent(this, MyVpnService.class);
            intent.setAction(action);
            intent.putExtra("enabled", enabled);
            if (Build.VERSION.SDK_INT >= 26) {
                startForegroundService(intent);
            } else {
                startService(intent);
            }
        } catch (Exception e) { }
    }

    private void click(int id) {
        lastClickTime = System.currentTimeMillis(); 
        
        if (id == 2) { 
            onFreeze = !onFreeze;
            if (timerRunnable1 != null) timerHandler.removeCallbacks(timerRunnable1);
            
            sendToMyVpn(MyVpnService.ACTION_FREEZE, onFreeze);
            if (onFreeze && timerMode1 > 0) {
                startTimerAutoOff(2);
            } else {
                paintBtn(btnFreeze, "Freeze", onFreeze, alpha1, fontSizeForButton(size1), 2);
            }
            saveAllStates(); 
            if (snd1) playSound(onFreeze);
            
        } else if (id == 3) { 
            onGhost = !onGhost;
            if (timerRunnable2 != null) timerHandler.removeCallbacks(timerRunnable2);
            
            sendToMyVpn(MyVpnService.ACTION_GHOST, onGhost);
            if (onGhost && timerMode2 > 0) {
                startTimerAutoOff(3);
            } else {
                paintBtn(btnGhost, "Ghost", onGhost, alpha2, fontSizeForButton(size2), 3);
            }
            saveAllStates(); 
            if (snd2) playSound(onGhost);
            
        } else if (id == 7) { 
            onTeleSpeed = !onTeleSpeed;
            if (timerRunnable3 != null) timerHandler.removeCallbacks(timerRunnable3);
            
            sendToMyVpn(MyVpnService.A_TK, onTeleSpeed);
            if (onTeleSpeed && timerMode3 > 0) {
                startTimerAutoOff(7);
            } else {
                paintBtn(btnTeleSpeed, "Teleport", onTeleSpeed, alpha3, fontSizeForButton(size3), 7);
            }
            saveAllStates(); 
            if (snd3) playSound(onTeleSpeed);
        }
    }

    private void rm(View v, boolean added) {
        if (!added || v == null) return;
        try { wm.removeView(v); } catch (Exception e) { }
    }

    private class DraggableLayout extends LinearLayout {
        private WindowManager.LayoutParams params;
        private int initialX, initialY;
        private float initialTouchX, initialTouchY;
        private int lastUpdateX, lastUpdateY;
        private boolean isDragging = false;

        public DraggableLayout(Context context) {
            super(context);
        }

        public void setParams(WindowManager.LayoutParams params) {
            this.params = params;
        }

        @Override
        public boolean onInterceptTouchEvent(MotionEvent event) {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    if (params != null) {
                        initialX = params.x;
                        initialY = params.y;
                        lastUpdateX = params.x;
                        lastUpdateY = params.y;
                    }
                    initialTouchX = event.getRawX();
                    initialTouchY = event.getRawY();
                    isDragging = false;
                    break;
                case MotionEvent.ACTION_MOVE:
                    float dx = event.getRawX() - initialTouchX;
                    float dy = event.getRawY() - initialTouchY;
                    if (Math.abs(dx) > dpToPx(8) || Math.abs(dy) > dpToPx(8)) {
                        isDragging = true;
                        return true; 
                    }
                    break;
            }
            return super.onInterceptTouchEvent(event);
        }

        @Override
        public boolean onTouchEvent(MotionEvent event) {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    if (params != null) {
                        initialX = params.x;
                        initialY = params.y;
                    }
                    initialTouchX = event.getRawX();
                    initialTouchY = event.getRawY();
                    return true;
                case MotionEvent.ACTION_MOVE:
                    if (params != null) {
                        float dx = event.getRawX() - initialTouchX;
                        float dy = event.getRawY() - initialTouchY;
                        params.x = initialX + (int) dx;
                        params.y = initialY + (int) dy;
                        
                        // DEBOUNCE: Chỉ gọi updateViewLayout nếu thực sự thay đổi vị trí đáng kể (>3px)
                        if (Math.abs(params.x - lastUpdateX) > 3 || Math.abs(params.y - lastUpdateY) > 3) {
                            try { wm.updateViewLayout(this, params); } catch (Exception e) {}
                            lastUpdateX = params.x;
                            lastUpdateY = params.y;
                        }
                    }
                    return true;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    isDragging = false;
                    return true;
            }
            return super.onTouchEvent(event);
        }
    }

    private void closeConfigPopup() {
        if (configPopupView != null) {
            try {
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                if (imm != null) imm.hideSoftInputFromWindow(configPopupView.getWindowToken(), 0);
                wm.removeView(configPopupView);
            } catch (Exception e) { }
            configPopupView = null;
            configPopupParams = null;
            popupIconIdForLock = -1;
        }
    }

    private void showConfigPopup(final int id) {
        closeConfigPopup();
        popupIconIdForLock = id; 
        final int currentTimerVal = (id == 2) ? timerMode1 : ((id == 3) ? timerMode2 : timerMode3);
        final int initialTimerValue = currentTimerVal > 0 ? currentTimerVal : 5;

        int type = (Build.VERSION.SDK_INT >= 26) ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY : WindowManager.LayoutParams.TYPE_PHONE;
        configPopupParams = new WindowManager.LayoutParams(
                dpToPx(220), WindowManager.LayoutParams.WRAP_CONTENT, type,
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL | WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
                PixelFormat.TRANSLUCENT
        );
        configPopupParams.gravity = Gravity.CENTER;

        final DraggableLayout rootLayout = new DraggableLayout(this);
        rootLayout.setParams(configPopupParams);
        rootLayout.setOrientation(LinearLayout.VERTICAL);
        rootLayout.setFocusableInTouchMode(true);
        rootLayout.setDescendantFocusability(ViewGroup.FOCUS_BEFORE_DESCENDANTS);

        GradientDrawable rootBg = new GradientDrawable();
        rootBg.setColor(0xFF151828);
        rootBg.setCornerRadius(dpToPx(16));
        rootBg.setStroke(2, 0xFF2F3FBF);
        rootLayout.setBackground(rootBg);
        rootLayout.setPadding(dpToPx(12), dpToPx(12), dpToPx(12), dpToPx(12));
        
        RelativeLayout headerLayout = new RelativeLayout(this);
        TextView titleText = new TextView(this);
        titleText.setText("Chế độ chạy");
        titleText.setTextColor(0xFF9BA2CD);
        titleText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        
        TextView closeBtn = new TextView(this);
        closeBtn.setText("✕");
        closeBtn.setTextColor(0xFFE74C3C);
        closeBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        closeBtn.setPadding(dpToPx(10), 0, dpToPx(10), 0);
        
        RelativeLayout.LayoutParams titleParams = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
        titleParams.addRule(RelativeLayout.ALIGN_PARENT_LEFT); 
        titleParams.addRule(RelativeLayout.CENTER_VERTICAL);
        
        RelativeLayout.LayoutParams closeParams = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
        closeParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT); 
        closeParams.addRule(RelativeLayout.CENTER_VERTICAL);
        
        headerLayout.addView(titleText, titleParams); 
        headerLayout.addView(closeBtn, closeParams);
        
        LinearLayout modeLayout = new LinearLayout(this);
        modeLayout.setOrientation(LinearLayout.HORIZONTAL);
        modeLayout.setWeightSum(2f);
        LinearLayout.LayoutParams modeParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dpToPx(32));
        modeParams.setMargins(0, dpToPx(6), 0, dpToPx(6)); 
        modeLayout.setLayoutParams(modeParams);

        final TextView btnNormal = new TextView(this);
        btnNormal.setText("Thường"); 
        btnNormal.setAllCaps(false);
        btnNormal.setGravity(Gravity.CENTER);
        btnNormal.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        LinearLayout.LayoutParams btnParams1 = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f);
        btnParams1.setMargins(0, 0, dpToPx(6), 0); 
        btnNormal.setLayoutParams(btnParams1);

        final TextView btnTimer = new TextView(this);
        btnTimer.setText("Hẹn giờ"); 
        btnTimer.setAllCaps(false);
        btnTimer.setGravity(Gravity.CENTER);
        btnTimer.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        LinearLayout.LayoutParams btnParams2 = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f);
        btnTimer.setLayoutParams(btnParams2);

        modeLayout.addView(btnNormal); 
        modeLayout.addView(btnTimer);

        final GradientDrawable bgActive = new GradientDrawable(); 
        bgActive.setColor(0xFF3366FF); 
        bgActive.setCornerRadius(dpToPx(8));
        
        final GradientDrawable bgInactive = new GradientDrawable(); 
        bgInactive.setColor(0xFF262940); 
        bgInactive.setCornerRadius(dpToPx(8));

        final LinearLayout timerContainer = new LinearLayout(this);
        timerContainer.setOrientation(LinearLayout.VERTICAL);
        
        LinearLayout lockLayout = new LinearLayout(this);
        lockLayout.setOrientation(LinearLayout.HORIZONTAL);
        lockLayout.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams lockLayoutParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lockLayoutParams.setMargins(0, dpToPx(8), 0, dpToPx(2)); 
        lockLayout.setLayoutParams(lockLayoutParams);

        TextView lockTitle = new TextView(this);
        lockTitle.setText("Khóa Vị Trí"); 
        lockTitle.setTextColor(0xFF9BA2CD);
        lockTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        LinearLayout.LayoutParams lockTitleParams = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        lockTitle.setLayoutParams(lockTitleParams);

        final RelativeLayout lockToggle = new RelativeLayout(this);
        LinearLayout.LayoutParams toggleBgParams = new LinearLayout.LayoutParams(dpToPx(36), dpToPx(20));
        lockToggle.setLayoutParams(toggleBgParams);
        
        final GradientDrawable toggleBg = new GradientDrawable();
        toggleBg.setCornerRadius(dpToPx(10));
        boolean currentLockState = false;
        if (id == 2) currentLockState = isLocked1; 
        else if (id == 3) currentLockState = isLocked2; 
        else if (id == 7) currentLockState = isLocked3;
        
        toggleBg.setColor(currentLockState ? 0xFF3366FF : 0xFF262940);
        lockToggle.setBackground(toggleBg);
        
        final View toggleThumb = new View(this);
        RelativeLayout.LayoutParams thumbParams = new RelativeLayout.LayoutParams(dpToPx(16), dpToPx(16));
        thumbParams.addRule(RelativeLayout.CENTER_VERTICAL);
        thumbParams.leftMargin = dpToPx(2); 
        toggleThumb.setLayoutParams(thumbParams);
        
        GradientDrawable thumbBg = new GradientDrawable();
        thumbBg.setShape(GradientDrawable.OVAL);
        thumbBg.setColor(Color.WHITE);
        toggleThumb.setBackground(thumbBg);
        
        if (currentLockState) toggleThumb.setTranslationX(dpToPx(16)); 
        
        lockToggle.addView(toggleThumb);
        
        lockToggle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                boolean newState = false;
                if (id == 2) { isLocked1 = !isLocked1; newState = isLocked1; }
                else if (id == 3) { isLocked2 = !isLocked2; newState = isLocked2; }
                else if (id == 7) { isLocked3 = !isLocked3; newState = isLocked3; }
                
                toggleBg.setColor(newState ? 0xFF3366FF : 0xFF262940);
                toggleThumb.animate().translationX(newState ? dpToPx(16) : 0).setDuration(200).start();
                saveAllStates();
            }
        });

        lockLayout.addView(lockTitle); 
        lockLayout.addView(lockToggle);

        final Runnable rebuildTimerContainer = new Runnable() {
            @Override
            public void run() {
                timerContainer.removeAllViews();
                
                LinearLayout adjustLayout = new LinearLayout(FloatingService.this);
                adjustLayout.setOrientation(LinearLayout.HORIZONTAL);
                adjustLayout.setGravity(Gravity.CENTER);
                adjustLayout.setBaselineAligned(false);
                
                final EditText etCurrentTime = new EditText(FloatingService.this);
                etCurrentTime.setText(initialTimerValue + "s");
                etCurrentTime.setTextColor(Color.WHITE);
                etCurrentTime.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
                etCurrentTime.setPadding(0, 0, 0, 0);
                etCurrentTime.setBackgroundColor(Color.TRANSPARENT);
                etCurrentTime.setInputType(InputType.TYPE_CLASS_NUMBER);
                etCurrentTime.setImeOptions(EditorInfo.IME_ACTION_DONE);
                etCurrentTime.setFilters(new InputFilter[] { new InputFilter.LengthFilter(4) });
                etCurrentTime.setSingleLine(true);
                etCurrentTime.setGravity(Gravity.CENTER);
                etCurrentTime.setIncludeFontPadding(false);
                LinearLayout.LayoutParams etParams = new LinearLayout.LayoutParams(dpToPx(60), dpToPx(32));
                etCurrentTime.setLayoutParams(etParams);
                
                final GradientDrawable roundBtnBg = new GradientDrawable();
                roundBtnBg.setColor(0xFF262940);
                roundBtnBg.setCornerRadius(dpToPx(8));
                
                final TextView btnMinus = new TextView(FloatingService.this);
                btnMinus.setText("−");
                btnMinus.setTextColor(Color.WHITE);
                btnMinus.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
                btnMinus.setIncludeFontPadding(false);
                btnMinus.setGravity(Gravity.CENTER);
                btnMinus.setBackground(roundBtnBg);
                LinearLayout.LayoutParams pmMinus = new LinearLayout.LayoutParams(dpToPx(32), dpToPx(32));
                pmMinus.setMargins(0, 0, dpToPx(4), 0);
                btnMinus.setLayoutParams(pmMinus);
                
                final TextView btnPlus = new TextView(FloatingService.this);
                btnPlus.setText("+");
                btnPlus.setTextColor(Color.WHITE);
                btnPlus.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
                btnPlus.setIncludeFontPadding(false);
                btnPlus.setGravity(Gravity.CENTER);
                btnPlus.setBackground(roundBtnBg);
                LinearLayout.LayoutParams pmPlus = new LinearLayout.LayoutParams(dpToPx(32), dpToPx(32));
                pmPlus.setMargins(dpToPx(4), 0, 0, 0);
                btnPlus.setLayoutParams(pmPlus);
                
                adjustLayout.addView(btnMinus);
                adjustLayout.addView(etCurrentTime);
                adjustLayout.addView(btnPlus);
                
                LinearLayout quickTimeLayout = new LinearLayout(FloatingService.this);
                quickTimeLayout.setOrientation(LinearLayout.HORIZONTAL);
                quickTimeLayout.setWeightSum(4f);
                LinearLayout.LayoutParams quickParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dpToPx(28));
                quickParams.setMargins(0, dpToPx(8), 0, 0);
                quickTimeLayout.setLayoutParams(quickParams);
                
                String[] quickTimes = {"5s", "10s", "30s", "60s"};
                for (final String t : quickTimes) {
                    TextView qtBtn = new TextView(FloatingService.this);
                    qtBtn.setText(t);
                    qtBtn.setGravity(Gravity.CENTER);
                    qtBtn.setAllCaps(false);
                    qtBtn.setTextColor(0xFF9BA2CD);
                    qtBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10);
                    
                    GradientDrawable qtBg = new GradientDrawable();
                    qtBg.setColor(0xFF262940);
                    qtBg.setCornerRadius(dpToPx(6));
                    qtBtn.setBackground(qtBg);
                    
                    LinearLayout.LayoutParams qp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f);
                    qp.setMargins(dpToPx(2), 0, dpToPx(2), 0);
                    qtBtn.setLayoutParams(qp);
                    
                    qtBtn.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            etCurrentTime.clearFocus();
                            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                            if (imm != null) imm.hideSoftInputFromWindow(etCurrentTime.getWindowToken(), 0);
                            
                            int val = Integer.parseInt(t.replace("s", ""));
                            etCurrentTime.setText(val + "s");
                            updateTimerSetting(id, val);
                        }
                    });
                    quickTimeLayout.addView(qtBtn);
                }
                
                timerContainer.addView(adjustLayout);
                timerContainer.addView(quickTimeLayout);
                
                etCurrentTime.setOnFocusChangeListener(new View.OnFocusChangeListener() {
                    @Override
                    public void onFocusChange(View v, boolean hasFocus) {
                        if (hasFocus) {
                            String txt = etCurrentTime.getText().toString().replace("s", "").trim();
                            etCurrentTime.setText(txt);
                            etCurrentTime.setSelection(etCurrentTime.getText().length());
                        } else {
                            String s = etCurrentTime.getText().toString().replace("s", "").trim();
                            if (s.isEmpty()) s = "0";
                            etCurrentTime.setText(s + "s");
                            int val = 0;
                            try { val = Integer.parseInt(s); } catch (Exception e) {}
                            updateTimerSetting(id, val);
                            if (val == 0) {
                                btnNormal.performClick();
                            }
                        }
                    }
                });
                
                etCurrentTime.setOnEditorActionListener(new TextView.OnEditorActionListener() {
                    @Override
                    public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                        if (actionId == EditorInfo.IME_ACTION_DONE || (event != null && event.getAction() == KeyEvent.ACTION_DOWN && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
                            etCurrentTime.clearFocus();
                            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                            if (imm != null) imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
                            return true;
                        }
                        return false;
                    }
                });
                
                btnMinus.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        etCurrentTime.clearFocus();
                        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                        if (imm != null) imm.hideSoftInputFromWindow(etCurrentTime.getWindowToken(), 0);
                        
                        int val = 0;
                        try { val = Integer.parseInt(etCurrentTime.getText().toString().replace("s", "").trim()); } catch (Exception e) {}
                        val -= 1;
                        if (val < 0) val = 0;
                        
                        etCurrentTime.setText(val + "s");
                        updateTimerSetting(id, val);
                        if (val == 0) {
                            btnNormal.performClick();
                        }
                    }
                });
                
                btnPlus.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        etCurrentTime.clearFocus();
                        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                        if (imm != null) imm.hideSoftInputFromWindow(etCurrentTime.getWindowToken(), 0);
                        
                        int val = 0;
                        try { val = Integer.parseInt(etCurrentTime.getText().toString().replace("s", "").trim()); } catch (Exception e) {}
                        val += 1;
                        if (val > 9999) val = 9999;
                        etCurrentTime.setText(val + "s");
                        updateTimerSetting(id, val);
                    }
                });
            }
        };
        
        final Runnable updateUIMode = new Runnable() {
            @Override
            public void run() {
                int currentVal = (id == 2) ? timerMode1 : ((id == 3) ? timerMode2 : timerMode3);
                boolean isTimer = currentVal > 0;
                
                if (isTimer) {
                    btnNormal.setBackground(bgInactive);
                    btnNormal.setTextColor(0xFF9BA2CD);
                    btnTimer.setBackground(bgActive);
                    btnTimer.setTextColor(Color.WHITE);
                    rebuildTimerContainer.run();
                    timerContainer.setVisibility(View.VISIBLE);
                } else {
                    btnNormal.setBackground(bgActive);
                    btnNormal.setTextColor(Color.WHITE);
                    btnTimer.setBackground(bgInactive);
                    btnTimer.setTextColor(0xFF9BA2CD);
                    timerContainer.setVisibility(View.GONE);
                }
            }
        };
        
        btnNormal.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                updateTimerSetting(id, 0);
                updateUIMode.run();
            }
        });
        
        btnTimer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int currentTimerSetting = (id == 2) ? timerMode1 : ((id == 3) ? timerMode2 : timerMode3);
                if (currentTimerSetting <= 0) {
                    updateTimerSetting(id, 5);
                }
                updateUIMode.run();
            }
        });
        
        closeBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                closeConfigPopup();
            }
        });
        
        rootLayout.addView(headerLayout);
        rootLayout.addView(modeLayout);
        rootLayout.addView(timerContainer);
        rootLayout.addView(lockLayout);
        
        updateUIMode.run();
        
        configPopupView = rootLayout;
        wm.addView(configPopupView, configPopupParams);
    }

    private void updateTimerSetting(int id, int value) {
        if (id == 2) timerMode1 = value;
        else if (id == 3) timerMode2 = value;
        else if (id == 7) timerMode3 = value;
        saveAllStates();
    }

    private class SrcVipButtonTouchListener implements View.OnTouchListener {
        private final WindowManager.LayoutParams p;
        private final TextView tv;
        private final int id;

        private int startX, startY;
        private float startRawX, startRawY;
        private int lastUpdateX, lastUpdateY;
        private int activePointerId = -1;
        private boolean drag = false;
        private boolean longPressFired = false; 
        
        private static final int LONG_PRESS_TIMEOUT = 500; 
        private final Handler longPressHandler = new Handler(Looper.getMainLooper());
        private Runnable longPressRunnable;

        SrcVipButtonTouchListener(WindowManager.LayoutParams p, TextView tv, final int id) {
            this.p = p; 
            this.tv = tv; 
            this.id = id;
            this.longPressRunnable = new Runnable() {
                @Override
                public void run() { 
                    longPressFired = true; 
                    showConfigPopup(id); 
                }
            };
        }

        @Override
        public boolean onTouch(View v, MotionEvent e) {
            int action = e.getActionMasked();
            int actionIndex = e.getActionIndex();

            switch (action) {
                case MotionEvent.ACTION_DOWN:
                case MotionEvent.ACTION_POINTER_DOWN:
                    if (activePointerId != -1) return true;
                    activePointerId = e.getPointerId(actionIndex);
                    
                    startX = p.x;
                    startY = p.y;
                    lastUpdateX = p.x;
                    lastUpdateY = p.y;
                    
                    startRawX = e.getRawX();
                    startRawY = e.getRawY();
                    
                    drag = false; 
                    longPressFired = false; 
                    longPressHandler.postDelayed(longPressRunnable, LONG_PRESS_TIMEOUT);
                    tv.animate().cancel(); 
                    tv.animate().scaleX(0.94f).scaleY(0.94f).setDuration(40L).start();
                    return true;

                case MotionEvent.ACTION_MOVE:
                    if (activePointerId == -1) return false;
                    int idx = e.findPointerIndex(activePointerId); 
                    if (idx < 0) return false;
                    
                    float currentRawX = e.getRawX();
                    float currentRawY = e.getRawY();
                    float dx = currentRawX - startRawX;
                    float dy = currentRawY - startRawY;
                    
                    if (Math.abs(dx) > dpToPx(8) || Math.abs(dy) > dpToPx(8)) { 
                        if (!drag && !longPressFired) {
                            longPressHandler.removeCallbacks(longPressRunnable);
                        }
                       
                        boolean currentIconLocked = false;
                        if (id == 2) currentIconLocked = isLocked1; 
                        else if (id == 3) currentIconLocked = isLocked2; 
                        else if (id == 7) currentIconLocked = isLocked3;
                        
                        if (!currentIconLocked && !longPressFired) {
                            drag = true;
                            p.x = startX + (int) dx;
                            p.y = startY + (int) dy;
                            
                            // DEBOUNCE HẠ NHIỆT: Chỉ gọi render nếu icon đã lệch hơn 3px
                            if (Math.abs(p.x - lastUpdateX) > 3 || Math.abs(p.y - lastUpdateY) > 3) {
                                try { wm.updateViewLayout(tv, p); } catch (Exception ex) { }
                                lastUpdateX = p.x;
                                lastUpdateY = p.y;
                            }
                        }
                    }
                    return true;

                case MotionEvent.ACTION_POINTER_UP:
                case MotionEvent.ACTION_UP:
                    int pointerId = e.getPointerId(actionIndex); 
                    if (pointerId != activePointerId) return true;
                    longPressHandler.removeCallbacks(longPressRunnable);
                    tv.animate().cancel(); 
                    tv.animate().scaleX(1f).scaleY(1f).setDuration(40L).start();
                    
                    if (drag) saveAllStates(); 
                    else if (!longPressFired) click(id);
                    
                    activePointerId = -1; 
                    drag = false; 
                    longPressFired = false; 
                    return true;

                case MotionEvent.ACTION_CANCEL:
                    longPressHandler.removeCallbacks(longPressRunnable);
                    tv.animate().cancel(); 
                    tv.animate().scaleX(1f).scaleY(1f).setDuration(40L).start();
                    activePointerId = -1; 
                    drag = false; 
                    longPressFired = false; 
                    return true;
            }
            return false;
        }
    }
}
