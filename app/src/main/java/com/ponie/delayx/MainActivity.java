package com.ponie.delayx;

import android.animation.ArgbEvaluator;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.graphics.drawable.ClipDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.graphics.drawable.RippleDrawable;
import android.net.Uri;
import android.net.VpnService;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.view.MotionEvent;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.RandomAccessFile;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends Activity implements View.OnClickListener {

    private static final int REQ_VPN = 0x3e9;
    private static final int REQ_OVERLAY = 0x3ea;
    private static final int REQ_NOTIFICATION_PERMISSION = 101;
    private static final String PREFS = "xevpn_prefs";
    private static final String KEY_TARGET_PACKAGE = "target_package";

    private static final String COLOR_BG_GRADIENT_START = "#07070F"; 
    private static final String COLOR_BG_GRADIENT_END = "#0F0A1A";   
    private static final String COLOR_CARD_BG = "#14121F";           
    private static final String COLOR_CARD_STROKE = "#2A2540";
    private static final String COLOR_ACCENT = "#FF2E63";            // Hồng neon mới
    private static final String COLOR_ACCENT2 = "#00F5D4";           // Cyan mint
    private static final String COLOR_GOLD = "#FFD166";              
    private static final String COLOR_TEXT = "#F8F8FF";              
    private static final String COLOR_TEXT_SEC = "#9BA3C7";          
    private static final String COLOR_SUCCESS = "#00E676";           
    private static final String COLOR_DANGER = "#FF1744";            

    private boolean engineOn = false;
    private String selPkg = null;
    private boolean waiting = false;
    private boolean isMonitoring = false;
    private long resumeTime = 0;

    private TextView btnDisplayState, btnVpnState, btnNotifState;

    private ScrollView viewHome, viewImpair;
    private LinearLayout bottomNav;
    private ImageView iconHome, iconImpair;
    private TextView tvHome, tvImpair;

    private Handler monitorHandler = new Handler(Looper.getMainLooper());
    private Thread networkThread;

    private TextView tvPingHeaderRight;
    private TextView tvPingMin, tvPingAvg, tvPingMax, tvPingJitter;
    private TextView tvPingStatusWord;
    private PingLineChartView pingChartView;

    private static int pingMin = Integer.MAX_VALUE;
    private static int pingMax = 0;
    private static long pingSum = 0;
    private static long pingCount = 0;
    private static int lastPing = 0;
    private static boolean isInitialPingLoaded = false;

    private TextView tvDeviceTempStatusTop;
    private TextView tvCpuTempBig;
    private LinearLayout layoutTempBars;
    private TextView tvCpuUsagePercent, tvRamUsageMb, tvBatteryTempBottom;

    private float batteryTemp = 0f;
    private float realCpuTemp = 0f;

    private int realCpuUsage = 0;
    private long lastCpuTotal = 0;
    private long lastCpuIdle = 0;
    private File cachedThermalFile = null;

    private Typeface defaultFont;
    private Typeface DelayXFont;

    private SeekBar sizeSeekBar1, opacitySeekBar1;
    private SeekBar sizeSeekBar2, opacitySeekBar2;
    private SeekBar sizeSeekBar3, opacitySeekBar3;

    private TextView sizeValue1, sizeValue2, sizeValue3;
    private TextView opacityValue1, opacityValue2, opacityValue3;

    private LinearLayout soundToggle1, visibilityToggle1;
    private LinearLayout soundToggle2, visibilityToggle2;
    private LinearLayout soundToggle3, visibilityToggle3;

    private View soundThumb1, visibilityThumb1;
    private View soundThumb2, visibilityThumb2;
    private View soundThumb3, visibilityThumb3;

    private boolean isSound1On = false, isVisible1On = true;
    private boolean isSound2On = false, isVisible2On = true;
    private boolean isSound3On = false, isVisible3On = true;

    private int currentSize1 = 90, currentOpacity1 = 100;
    private int currentSize2 = 90, currentOpacity2 = 100;
    private int currentSize3 = 90, currentOpacity3 = 100;

    private SharedPreferences prefs;
    private static final String PREFS_NAME = "DelayxPrefs";
    private static final String ACTION_APPLY_FLOAT_APPEARANCE = "com.ponie.delayx.APPLY_FLOAT_APPEARANCE";

    private static final ArrayList<Integer> staticPingPoints = new ArrayList<Integer>();

    private TextView tvTabAll, tvTabSafe, tvTabRisk;
    private LinearLayout tabAllBg, tabSafeBg, tabRiskBg;
    private View cardFreeze, cardGhost, cardTeleport;
    private View warningRowSafe, warningRowRisk;

    private BroadcastReceiver batteryReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            int temp = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0);
            if (temp > 0) {
                batteryTemp = temp / 10.0f;
                if (tvBatteryTempBottom != null && isInitialPingLoaded) {
                    stopBlink(tvBatteryTempBottom);
                    tvBatteryTempBottom.setText(Math.round(batteryTemp) + " °C");
                }
            }
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        long validTime = getIntent().getLongExtra("key_auth_check", -1);
        if (LoginActivity.key == 0 || validTime != LoginActivity.key) {
            finish();
            throw new RuntimeException("Hệ Thống Quét Chống Bug!");
        }

        requestWindowFeature(Window.FEATURE_NO_TITLE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            window.setStatusBarColor(Color.parseColor(COLOR_BG_GRADIENT_START));
            window.setNavigationBarColor(Color.parseColor(COLOR_BG_GRADIENT_END));
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            WindowManager.LayoutParams layoutParams = getWindow().getAttributes();
            layoutParams.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
            getWindow().setAttributes(layoutParams);
        }

        prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        selPkg = getSharedPreferences(PREFS, MODE_PRIVATE).getString(KEY_TARGET_PACKAGE, "com.dts.freefireth");

        loadImpairSettings();

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                DelayXFont = getResources().getFont(R.font.chudep);
                defaultFont = getResources().getFont(R.font.font);
            } else {
                DelayXFont = Typeface.createFromAsset(getAssets(), "fonts/chudep.ttf");
                defaultFont = Typeface.createFromAsset(getAssets(), "fonts/font.ttf");
            }
        } catch (Exception e) {
            DelayXFont = Typeface.create(Typeface.DEFAULT_BOLD, Typeface.ITALIC);
            defaultFont = Typeface.DEFAULT;
        }

        registerReceiver(batteryReceiver, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));

        RelativeLayout rootLayout = new RelativeLayout(this);
        rootLayout.setLayoutParams(new RelativeLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));

        GradientDrawable gradientBg = new GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                new int[]{Color.parseColor(COLOR_BG_GRADIENT_START), Color.parseColor(COLOR_BG_GRADIENT_END)}
        );
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            rootLayout.setBackground(gradientBg);
        } else {
            rootLayout.setBackgroundDrawable(gradientBg);
        }

        LinearLayout headerView = createHeaderView();
        RelativeLayout.LayoutParams headerParams = new RelativeLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        headerParams.addRule(RelativeLayout.ALIGN_PARENT_TOP);
        headerView.setLayoutParams(headerParams);
        rootLayout.addView(headerView);

        FrameLayout contentFrame = new FrameLayout(this);
        RelativeLayout.LayoutParams contentParams = new RelativeLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT);
        contentParams.bottomMargin = dpToPx(65f);
        contentParams.topMargin = dpToPx(80f);
        contentFrame.setLayoutParams(contentParams);

        viewHome = createHomeView();
        viewImpair = createImpairView();

        contentFrame.addView(viewHome);
        contentFrame.addView(viewImpair);
        rootLayout.addView(contentFrame);

        bottomNav = createBottomNav();
        RelativeLayout.LayoutParams navParams = new RelativeLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dpToPx(65f));
        navParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
        bottomNav.setLayoutParams(navParams);
        rootLayout.addView(bottomNav);

        setContentView(rootLayout);

        switchTab(0);
        showWelcomeDialog();

        loadHardwareStatsInstantly();
    }

    @Override
    protected void onResume() {
        super.onResume();
        waiting = false;
        resumeTime = System.currentTimeMillis();
        refreshPermissionStates();

        loadHardwareStatsInstantly();

        stopHardwareMonitor();
        startHardwareMonitor();
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopHardwareMonitor();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try { unregisterReceiver(batteryReceiver); } catch (Exception e) {}
        stopHardwareMonitor();
        if (engineOn) {
            stopEngine();
        }
    }

    private void startBlink(View view) {
        if (view == null) return;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            PropertyValuesHolder scaleX = PropertyValuesHolder.ofFloat("scaleX", 1f, 1.05f);
            PropertyValuesHolder scaleY = PropertyValuesHolder.ofFloat("scaleY", 1f, 1.05f);
            PropertyValuesHolder alpha = PropertyValuesHolder.ofFloat("alpha", 0.6f, 1.0f);
            ObjectAnimator anim = ObjectAnimator.ofPropertyValuesHolder(view, scaleX, scaleY, alpha);
            anim.setDuration(800);
            anim.setRepeatMode(ValueAnimator.REVERSE);
            anim.setRepeatCount(ValueAnimator.INFINITE);
            anim.setInterpolator(new AccelerateDecelerateInterpolator());
            anim.start();
            view.setTag(anim);
        }
    }

    private void stopBlink(View view) {
        if (view == null) return;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            Object anim = view.getTag();
            if (anim instanceof ObjectAnimator) {
                ((ObjectAnimator) anim).cancel();
            }
            view.setAlpha(1.0f);
            view.setScaleX(1.0f);
            view.setScaleY(1.0f);
        }
    }

    private void loadHardwareStatsInstantly() {
        long tempUsedRam = 0;
        try {
            ActivityManager actManager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
            ActivityManager.MemoryInfo memInfo = new ActivityManager.MemoryInfo();
            actManager.getMemoryInfo(memInfo);
            tempUsedRam = (memInfo.totalMem - memInfo.availMem) / 1048576L;
        } catch (Exception e) {}

        realCpuTemp = getRealCpuTemperature();
        realCpuUsage = getRealCpuUsage();

        updateHardwareStatsUI(realCpuUsage, realCpuTemp, tempUsedRam);
    }

    private void stopHardwareMonitor() {
        isMonitoring = false;
        monitorHandler.removeCallbacksAndMessages(null);
        if (networkThread != null) {
            networkThread.interrupt();
            networkThread = null;
        }
    }

    private void showWelcomeDialog() {
        final Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(dpToPx(28f), dpToPx(32f), dpToPx(28f), dpToPx(24f));
        layout.setGravity(Gravity.CENTER_HORIZONTAL);

        // Premium dark card with gold border
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Color.parseColor("#121212"));
        bg.setCornerRadius(dpToPx(28f));
        bg.setStroke(dpToPx(2f), Color.parseColor("#FFD700"));

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            layout.setBackground(bg);
        } else {
            layout.setBackgroundDrawable(bg);
        }

        // Crown / VIP title
        TextView tvTitle = new TextView(this);
        tvTitle.setText("👑Mướp MOD TEAM👑");
        tvTitle.setTextColor(Color.parseColor("#FFD700"));
        tvTitle.setTextSize(28);
        if (defaultFont != null) tvTitle.setTypeface(defaultFont, Typeface.BOLD);
        else tvTitle.setTypeface(null, Typeface.BOLD);
        tvTitle.setGravity(Gravity.CENTER);
        tvTitle.setPadding(0, 0, 0, dpToPx(6f));
        tvTitle.setShadowLayer(12, 0, 0, Color.parseColor("#FFD700"));

        TextView tvSub = new TextView(this);
        tvSub.setText("PREMIUM NETWORK SYSTEM");
        tvSub.setTextColor(Color.parseColor("#00E5FF"));
        tvSub.setTextSize(11);
        tvSub.setGravity(Gravity.CENTER);
        tvSub.setLetterSpacing(0.15f);
        tvSub.setPadding(0, 0, 0, dpToPx(20f));

        // Divider gold
        View divider = new View(this);
        divider.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dpToPx(1f)));
        divider.setBackgroundColor(Color.parseColor("#33FFD700"));
        LinearLayout.LayoutParams divLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dpToPx(1f));
        divLp.setMargins(dpToPx(20f), 0, dpToPx(20f), dpToPx(20f));
        divider.setLayoutParams(divLp);

        // Social links
        LinearLayout linksLayout = new LinearLayout(this);
        linksLayout.setOrientation(LinearLayout.VERTICAL);
        linksLayout.setGravity(Gravity.CENTER);
        linksLayout.setPadding(0, 0, 0, dpToPx(16f));

        TextView tvTele = new TextView(this);
        tvTele.setText("✈️  Telegram: t.me/muop_crack");
        tvTele.setTextColor(Color.parseColor("#00E5FF"));
        tvTele.setTextSize(15);
        tvTele.setPadding(0, dpToPx(10f), 0, dpToPx(10f));
        tvTele.setGravity(Gravity.CENTER);
        tvTele.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try { startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://t.me/muop_crack"))); } catch (Exception e){}
            }
        });

        TextView tvTiktok = new TextView(this);
        tvTiktok.setText("🎵  TikTok: @muop_depzai");
        tvTiktok.setTextColor(Color.parseColor("#00E5FF"));
        tvTiktok.setTextSize(15);
        tvTiktok.setPadding(0, dpToPx(6f), 0, dpToPx(16f));
        tvTiktok.setGravity(Gravity.CENTER);
        tvTiktok.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try { startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.tiktok.com/@muop_depzai"))); } catch (Exception e){}
            }
        });

        TextView tvDesc = new TextView(this);
        tvDesc.setText("Cảm ơn bạn đã sử dụng 👑Mướp MOD TEAM👑\nNhấn Theo Dõi để nhận cập nhật mới nhất.");
        tvDesc.setTextColor(Color.parseColor("#BBBBBB"));
        tvDesc.setTextSize(13);
        tvDesc.setGravity(Gravity.CENTER);
        tvDesc.setLineSpacing(4f, 1.15f);
        if (defaultFont != null) tvDesc.setTypeface(defaultFont);
        tvDesc.setPadding(0, dpToPx(8f), 0, dpToPx(24f));

        linksLayout.addView(tvTele);
        linksLayout.addView(tvTiktok);
        linksLayout.addView(tvDesc);

        // Buttons
        LinearLayout btnLayout = new LinearLayout(this);
        btnLayout.setOrientation(LinearLayout.HORIZONTAL);
        btnLayout.setGravity(Gravity.CENTER);

        // ===== Nút THEO DÕI - Neon Gold =====
        Button btnFollow = new Button(this);
        btnFollow.setText("THEO DÕI");
        btnFollow.setTextColor(Color.BLACK);
        btnFollow.setTextSize(15);
        if (defaultFont != null) btnFollow.setTypeface(defaultFont, Typeface.BOLD);
        btnFollow.setAllCaps(true);
        // Neon glow text
        btnFollow.setShadowLayer(18, 0, 0, Color.parseColor("#FFD700"));
        
        GradientDrawable bgFollow = new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,
                new int[]{Color.parseColor("#FFE566"), Color.parseColor("#FFD700"), Color.parseColor("#FFAA00")});
        bgFollow.setCornerRadius(dpToPx(16f));
        // Thêm viền sáng để tạo cảm giác neon
        bgFollow.setStroke(dpToPx(1.5f), Color.parseColor("#FFFFFF"));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) btnFollow.setBackground(bgFollow);
        else btnFollow.setBackgroundDrawable(bgFollow);
        
        // Hiệu ứng nhấn
        btnFollow.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    v.setAlpha(0.75f);
                    v.setScaleX(0.96f);
                    v.setScaleY(0.96f);
                } else if (event.getAction() == MotionEvent.ACTION_UP || event.getAction() == MotionEvent.ACTION_CANCEL) {
                    v.setAlpha(1f);
                    v.setScaleX(1f);
                    v.setScaleY(1f);
                }
                return false;
            }
        });
        btnFollow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try { startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.tiktok.com/@muop_depzai"))); } catch (Exception e){}
            }
        });

        // ===== Nút OK - Neon Cyan/Gold outline =====
        Button btnOk = new Button(this);
        btnOk.setText("OK");
        btnOk.setTextColor(Color.parseColor("#00E5FF"));
        btnOk.setTextSize(15);
        if (defaultFont != null) btnOk.setTypeface(defaultFont, Typeface.BOLD);
        btnOk.setAllCaps(true);
        // Neon glow text cyan
        btnOk.setShadowLayer(16, 0, 0, Color.parseColor("#00E5FF"));
        
        GradientDrawable bgOk = new GradientDrawable();
        bgOk.setColor(Color.parseColor("#0D0D0D"));
        bgOk.setStroke(dpToPx(2.5f), Color.parseColor("#00E5FF"));
        bgOk.setCornerRadius(dpToPx(16f));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) btnOk.setBackground(bgOk);
        else btnOk.setBackgroundDrawable(bgOk);
        
        btnOk.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    v.setAlpha(0.7f);
                    v.setScaleX(0.96f);
                    v.setScaleY(0.96f);
                } else if (event.getAction() == MotionEvent.ACTION_UP || event.getAction() == MotionEvent.ACTION_CANCEL) {
                    v.setAlpha(1f);
                    v.setScaleX(1f);
                    v.setScaleY(1f);
                }
                return false;
            }
        });
        btnOk.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });

        LinearLayout.LayoutParams btnParams = new LinearLayout.LayoutParams(0, dpToPx(50f), 1f);
        btnLayout.addView(btnFollow, btnParams);
        View gap = new View(this);
        gap.setLayoutParams(new LinearLayout.LayoutParams(dpToPx(14f), 1));
        btnLayout.addView(gap);
        btnLayout.addView(btnOk, btnParams);

        layout.addView(tvTitle);
        layout.addView(tvSub);
        layout.addView(divider);
        layout.addView(linksLayout);
        layout.addView(btnLayout);

        dialog.setContentView(layout);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            dialog.getWindow().setLayout((int)(getResources().getDisplayMetrics().widthPixels * 0.88), ViewGroup.LayoutParams.WRAP_CONTENT);
        }
        dialog.setCancelable(true);
        dialog.show();
    }


    private void loadImpairSettings() {
        currentSize1 = prefs.getInt("iconSize1", 90);
        currentSize2 = prefs.getInt("iconSize2", 90);
        currentSize3 = prefs.getInt("iconSize3", 90);
        currentOpacity1 = prefs.getInt("iconOpacity1", 100);
        currentOpacity2 = prefs.getInt("iconOpacity2", 100);
        currentOpacity3 = prefs.getInt("iconOpacity3", 100);
        isSound1On = prefs.getBoolean("iconSound1", false);
        isSound2On = prefs.getBoolean("iconSound2", false);
        isSound3On = prefs.getBoolean("iconSound3", false);
        isVisible1On = prefs.getBoolean("iconVisible1", true);
        isVisible2On = prefs.getBoolean("iconVisible2", true);
        isVisible3On = prefs.getBoolean("iconVisible3", true);
    }

    private void saveImpairSettings() {
        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt("iconSize1", currentSize1);
        editor.putInt("iconSize2", currentSize2);
        editor.putInt("iconSize3", currentSize3);
        editor.putInt("iconOpacity1", currentOpacity1);
        editor.putInt("iconOpacity2", currentOpacity2);
        editor.putInt("iconOpacity3", currentOpacity3);
        editor.putBoolean("iconSound1", isSound1On);
        editor.putBoolean("iconSound2", isSound2On);
        editor.putBoolean("iconSound3", isSound3On);
        editor.putBoolean("iconVisible1", isVisible1On);
        editor.putBoolean("iconVisible2", isVisible2On);
        editor.putBoolean("iconVisible3", isVisible3On);
        editor.apply();

        if (engineOn) {
            sendAppearanceUpdate();
        }
    }

    private void sendAppearanceUpdate() {
        Intent intent = new Intent(ACTION_APPLY_FLOAT_APPEARANCE);
        intent.putExtra("size1", currentSize1);
        intent.putExtra("size2", currentSize2);
        intent.putExtra("size3", currentSize3);
        intent.putExtra("alpha1", currentOpacity1);
        intent.putExtra("alpha2", currentOpacity2);
        intent.putExtra("alpha3", currentOpacity3);
        intent.putExtra("vis1", isVisible1On);
        intent.putExtra("vis2", isVisible2On);
        intent.putExtra("vis3", isVisible3On);
        intent.putExtra("snd1", isSound1On);
        intent.putExtra("snd2", isSound2On);
        intent.putExtra("snd3", isSound3On);
        sendBroadcast(intent);
    }

    private LinearLayout createBottomNav() {
        LinearLayout nav = new LinearLayout(this);
        nav.setOrientation(LinearLayout.HORIZONTAL);
        nav.setBackgroundColor(Color.parseColor(COLOR_BG_GRADIENT_END));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            nav.setElevation(dpToPx(16f));
        }

        View topBorder = new View(this);
        topBorder.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dpToPx(1f)));
        topBorder.setBackgroundColor(Color.parseColor(COLOR_CARD_STROKE));
        
        LinearLayout navInner = new LinearLayout(this);
        navInner.setOrientation(LinearLayout.HORIZONTAL);
        navInner.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        LinearLayout homeContainer = new LinearLayout(this);
        homeContainer.setOrientation(LinearLayout.VERTICAL);
        homeContainer.setGravity(Gravity.CENTER);
        homeContainer.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1));

        iconHome = new ImageView(this);
        LinearLayout.LayoutParams iconHomeParams = new LinearLayout.LayoutParams(dpToPx(26f), dpToPx(26f));
        iconHome.setLayoutParams(iconHomeParams);
        iconHome.setScaleType(ImageView.ScaleType.FIT_CENTER);
        try {
            int homeResId = getResources().getIdentifier("ic_home", "drawable", getPackageName());
            if (homeResId != 0) iconHome.setImageResource(homeResId);
        } catch (Exception e) {}

        tvHome = new TextView(this);
        tvHome.setText("Trang Chủ");
        tvHome.setTextSize(11);
        tvHome.setTypeface(null, Typeface.BOLD);
        tvHome.setGravity(Gravity.CENTER);
        tvHome.setPadding(0, dpToPx(2f), 0, 0);

        homeContainer.addView(iconHome);
        homeContainer.addView(tvHome);

        LinearLayout settingsContainer = new LinearLayout(this);
        settingsContainer.setOrientation(LinearLayout.VERTICAL);
        settingsContainer.setGravity(Gravity.CENTER);
        settingsContainer.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1));

        iconImpair = new ImageView(this);
        LinearLayout.LayoutParams iconImpairParams = new LinearLayout.LayoutParams(dpToPx(26f), dpToPx(26f));
        iconImpair.setLayoutParams(iconImpairParams);
        iconImpair.setScaleType(ImageView.ScaleType.FIT_CENTER);
        try {
            int settingsResId = getResources().getIdentifier("ic_settings", "drawable", getPackageName());
            if (settingsResId != 0) iconImpair.setImageResource(settingsResId);
        } catch (Exception e) {}

        tvImpair = new TextView(this);
        tvImpair.setText("Cài Đặt");
        tvImpair.setTextSize(11);
        tvImpair.setTypeface(null, Typeface.BOLD);
        tvImpair.setGravity(Gravity.CENTER);
        tvImpair.setPadding(0, dpToPx(2f), 0, 0);

        settingsContainer.addView(iconImpair);
        settingsContainer.addView(tvImpair);

        navInner.addView(homeContainer);
        navInner.addView(settingsContainer);
        
        LinearLayout navWrap = new LinearLayout(this);
        navWrap.setOrientation(LinearLayout.VERTICAL);
        navWrap.addView(topBorder);
        navWrap.addView(navInner);

        homeContainer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) { switchTab(0); }
        });

        settingsContainer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) { switchTab(1); }
        });

        return navWrap;
    }

    private void switchTab(int index) {
        if (iconHome != null) iconHome.setColorFilter(Color.parseColor(COLOR_TEXT_SEC));
        if (iconImpair != null) iconImpair.setColorFilter(Color.parseColor(COLOR_TEXT_SEC));
        if (tvHome != null) tvHome.setTextColor(Color.parseColor(COLOR_TEXT_SEC));
        if (tvImpair != null) tvImpair.setTextColor(Color.parseColor(COLOR_TEXT_SEC));

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR1) {
            if (index == 0) {
                iconHome.animate().scaleX(1.1f).scaleY(1.1f).setDuration(200).start();
                iconImpair.animate().scaleX(1.0f).scaleY(1.0f).setDuration(200).start();
            } else {
                iconImpair.animate().scaleX(1.1f).scaleY(1.1f).setDuration(200).start();
                iconHome.animate().scaleX(1.0f).scaleY(1.0f).setDuration(200).start();
            }
        }

        viewHome.setVisibility(View.GONE);
        viewImpair.setVisibility(View.GONE);

        if (index == 0) {
            viewHome.setVisibility(View.VISIBLE);
            if (iconHome != null) iconHome.setColorFilter(Color.parseColor(COLOR_ACCENT));
            if (tvHome != null) tvHome.setTextColor(Color.parseColor(COLOR_ACCENT));
        } else if (index == 1) {
            viewImpair.setVisibility(View.VISIBLE);
            if (iconImpair != null) iconImpair.setColorFilter(Color.parseColor(COLOR_ACCENT));
            if (tvImpair != null) tvImpair.setTextColor(Color.parseColor(COLOR_ACCENT));
        }
    }

    private ScrollView createHomeView() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setOverScrollMode(ScrollView.OVER_SCROLL_NEVER);
        scroll.setVerticalScrollBarEnabled(false);

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(dpToPx(16f), dpToPx(8f), dpToPx(16f), dpToPx(16f));

        layout.addView(createPermissionCard());
        layout.addView(createStartStopCard());
        layout.addView(createPingMonitorCard());
        layout.addView(createDeviceTempCard());

        scroll.addView(layout);
        return scroll;
    }

    private LinearLayout createHeaderView() {
        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setBackgroundColor(Color.parseColor(COLOR_BG_GRADIENT_START));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            header.setElevation(dpToPx(12f));
        }
        header.setPadding(dpToPx(16f), dpToPx(16f), dpToPx(16f), dpToPx(16f));

        ImageView logo = new ImageView(this) {
            Path path = new Path();
            RectF rect = new RectF();
            @Override
            protected void onDraw(Canvas canvas) {
                path.reset();
                float radius = dpToPx(12f);
                rect.set(0, 0, getWidth(), getHeight());
                path.addRoundRect(rect, radius, radius, Path.Direction.CW);
                canvas.clipPath(path);
                super.onDraw(canvas);
            }
        };
        LinearLayout.LayoutParams logoParams = new LinearLayout.LayoutParams(dpToPx(52f), dpToPx(52f));
        logo.setLayoutParams(logoParams);
        logo.setScaleType(ImageView.ScaleType.CENTER_CROP);

        int resId = getResources().getIdentifier("app_avatar", "drawable", getPackageName());
        if (resId != 0) logo.setImageResource(resId);
        else {
            GradientDrawable fallback = new GradientDrawable();
            fallback.setColor(Color.parseColor(COLOR_ACCENT2));
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) logo.setBackground(fallback);
            else logo.setBackgroundDrawable(fallback);
        }

        LinearLayout logoContainer = new LinearLayout(this);
        logoContainer.setPadding(dpToPx(3f), dpToPx(3f), dpToPx(3f), dpToPx(3f));
        GradientDrawable logoRing = new GradientDrawable();
        logoRing.setShape(GradientDrawable.RECTANGLE);
        logoRing.setCornerRadius(dpToPx(14f));
        logoRing.setStroke(dpToPx(2f), Color.parseColor(COLOR_ACCENT));
        logoRing.setColor(Color.TRANSPARENT);
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) logoContainer.setBackground(logoRing);
        else logoContainer.setBackgroundDrawable(logoRing);
        logoContainer.addView(logo);

        LinearLayout textLayout = new LinearLayout(this);
        textLayout.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams textParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1);
        textParams.leftMargin = dpToPx(16f);
        textLayout.setLayoutParams(textParams);

        final TextView tvTitle = new TextView(this);
        tvTitle.setText("👑Mướp MOD TEAM👑");
        tvTitle.setTextColor(Color.parseColor(COLOR_ACCENT));
        tvTitle.setTextSize(22);
        tvTitle.setShadowLayer(12, 0, 0, Color.parseColor(COLOR_ACCENT));
        if (DelayXFont != null) tvTitle.setTypeface(DelayXFont);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            ValueAnimator colorAnim = ValueAnimator.ofInt(
                    Color.parseColor(COLOR_ACCENT),
                    Color.parseColor(COLOR_ACCENT2),
                    Color.parseColor(COLOR_ACCENT)
            );
            colorAnim.setDuration(3000);
            colorAnim.setEvaluator(new ArgbEvaluator());
            colorAnim.setRepeatCount(ValueAnimator.INFINITE);
            colorAnim.setRepeatMode(ValueAnimator.RESTART);
            colorAnim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animator) {
                    int color = (Integer) animator.getAnimatedValue();
                    tvTitle.setTextColor(color);
                    tvTitle.setShadowLayer(10, 0, 0, color);
                }
            });
            colorAnim.start();
        }

        // Hiển thị key còn bao lâu – đẹp và rõ ràng
        final TextView tvKeyRemain = new TextView(this);
        tvKeyRemain.setTextColor(Color.parseColor(COLOR_ACCENT2));
        tvKeyRemain.setTextSize(11);
        tvKeyRemain.setTypeface(null, Typeface.BOLD);
        tvKeyRemain.setPadding(0, dpToPx(3f), 0, 0);
        tvKeyRemain.setShadowLayer(6f, 0, 0, Color.parseColor(COLOR_ACCENT2));

        long expires = getSharedPreferences("app_prefs", MODE_PRIVATE).getLong("key_expires", 0);
        updateKeyRemainText(tvKeyRemain, expires);

        // Tự cập nhật mỗi phút
        final Handler keyHandler = new Handler(Looper.getMainLooper());
        keyHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                long exp = getSharedPreferences("app_prefs", MODE_PRIVATE).getLong("key_expires", 0);
                updateKeyRemainText(tvKeyRemain, exp);
                keyHandler.postDelayed(this, 60000);
            }
        }, 60000);

        textLayout.addView(tvTitle);
        textLayout.addView(tvKeyRemain);

        // Pill trạng thái mới – đẹp hơn
        LinearLayout pill = new LinearLayout(this);
        pill.setOrientation(LinearLayout.HORIZONTAL);
        pill.setGravity(Gravity.CENTER_VERTICAL);
        pill.setPadding(dpToPx(12f), dpToPx(7f), dpToPx(14f), dpToPx(7f));

        GradientDrawable pillBg = new GradientDrawable();
        pillBg.setColor(Color.parseColor("#2200E676"));
        pillBg.setCornerRadius(dpToPx(20f));
        pillBg.setStroke(dpToPx(1f), Color.parseColor("#6600E676"));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) pill.setBackground(pillBg);
        else pill.setBackgroundDrawable(pillBg);

        View dot = new View(this);
        LinearLayout.LayoutParams dotParams = new LinearLayout.LayoutParams(dpToPx(7f), dpToPx(7f));
        dotParams.rightMargin = dpToPx(7f);
        dot.setLayoutParams(dotParams);

        GradientDrawable dotBg = new GradientDrawable();
        dotBg.setShape(GradientDrawable.OVAL);
        dotBg.setColor(Color.parseColor(COLOR_SUCCESS));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) dot.setBackground(dotBg);
        else dot.setBackgroundDrawable(dotBg);
        startBlink(dot);

        TextView tvOnline = new TextView(this);
        tvOnline.setText("ONLINE");
        tvOnline.setTextColor(Color.parseColor(COLOR_SUCCESS));
        tvOnline.setTextSize(10);
        tvOnline.setTypeface(null, Typeface.BOLD);
        tvOnline.setLetterSpacing(0.08f);

        pill.addView(dot);
        pill.addView(tvOnline);

        header.addView(logoContainer);
        header.addView(textLayout);
        header.addView(pill);

        return header;
    }

    private void updateKeyRemainText(TextView tv, long expiresEpochSec) {
        if (tv == null) return;
        if (expiresEpochSec <= 0) {
            tv.setText("Key: Vĩnh viễn / Không giới hạn");
            tv.setTextColor(Color.parseColor(COLOR_SUCCESS));
            return;
        }
        long now = System.currentTimeMillis() / 1000;
        long remain = expiresEpochSec - now;
        if (remain <= 0) {
            tv.setText("⚠ Key đã hết hạn");
            tv.setTextColor(Color.parseColor(COLOR_DANGER));
            return;
        }
        long days = remain / 86400;
        long hours = (remain % 86400) / 3600;
        long mins = (remain % 3600) / 60;
        String text;
        if (days > 0) {
            text = "⏱ Còn " + days + " ngày " + hours + " giờ";
        } else if (hours > 0) {
            text = "⏱ Còn " + hours + " giờ " + mins + " phút";
        } else {
            text = "⏱ Còn " + mins + " phút";
        }
        tv.setText(text);
        if (days < 1) tv.setTextColor(Color.parseColor("#FF9800"));
        else tv.setTextColor(Color.parseColor(COLOR_ACCENT2));
    }

    private LinearLayout createPermissionCard() {
        LinearLayout outer = new LinearLayout(this);
        outer.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams outerParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        outerParams.topMargin = dpToPx(8f);
        outer.setLayoutParams(outerParams);

        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(0, 0, 0, dpToPx(12f));

        LinearLayout leftTitleLayout = new LinearLayout(this);
        leftTitleLayout.setOrientation(LinearLayout.HORIZONTAL);
        leftTitleLayout.setGravity(Gravity.CENTER_VERTICAL);
        leftTitleLayout.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));

        View purpBar = new View(this);
        purpBar.setLayoutParams(new LinearLayout.LayoutParams(dpToPx(4f), dpToPx(16f)));
        purpBar.setBackgroundColor(Color.parseColor(COLOR_ACCENT));
        
        GradientDrawable barBg = new GradientDrawable();
        barBg.setColor(Color.parseColor(COLOR_ACCENT));
        barBg.setCornerRadius(dpToPx(2f));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) purpBar.setBackground(barBg);
        else purpBar.setBackgroundDrawable(barBg);

        TextView tvTitle = new TextView(this);
        tvTitle.setText(" QUYỀN TRUY CẬP HỆ THỐNG");
        tvTitle.setTextColor(Color.parseColor(COLOR_TEXT_SEC));
        tvTitle.setTextSize(12);
        tvTitle.setTypeface(null, Typeface.BOLD);

        leftTitleLayout.addView(purpBar);
        leftTitleLayout.addView(tvTitle);

        TextView tvBadge = new TextView(this);
        tvBadge.setText("BẮT BUỘC");
        tvBadge.setTextColor(Color.parseColor(COLOR_GOLD));
        tvBadge.setTextSize(9);
        tvBadge.setTypeface(null, Typeface.BOLD);
        tvBadge.setPadding(dpToPx(8f), dpToPx(4f), dpToPx(8f), dpToPx(4f));

        GradientDrawable badgeBg = new GradientDrawable();
        badgeBg.setColor(Color.parseColor("#1A" + COLOR_GOLD.replace("#", "")));
        badgeBg.setStroke(dpToPx(1f), Color.parseColor("#33" + COLOR_GOLD.replace("#", "")));
        badgeBg.setCornerRadius(dpToPx(6f));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) tvBadge.setBackground(badgeBg);
        else tvBadge.setBackgroundDrawable(badgeBg);

        header.addView(leftTitleLayout);
        header.addView(tvBadge);

        LinearLayout card = createDarkCard();
        card.setPadding(dpToPx(16f), dpToPx(8f), dpToPx(16f), dpToPx(8f));

        card.addView(createAppPermissionRow("Hiển thị nút nổi", "Cho phép hiển thị menu trên màn hình", "ic_monitor", 0));
        card.addView(createAppPermissionRow("Kết nối VPN", "Tạo luồng mạng riêng tư và an toàn", "ic_local", 1));
        card.addView(createAppPermissionRow("Nhận thông báo", "Theo dõi tình trạng hoạt động ngầm", "ic_notification", 2));

        outer.addView(header);
        outer.addView(card);
        return outer;
    }

    private LinearLayout createAppPermissionRow(String title, String subText, String iconResName, final int type) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, dpToPx(14f), 0, dpToPx(14f));

        ImageView icon = new ImageView(this);
        int resId = getResources().getIdentifier(iconResName, "drawable", getPackageName());
        if (resId != 0) icon.setImageResource(resId);
        icon.setScaleType(ImageView.ScaleType.FIT_CENTER);
        icon.setColorFilter(Color.parseColor(COLOR_ACCENT2));
        int p = dpToPx(10f);
        icon.setPadding(p, p, p, p);
        icon.setLayoutParams(new LinearLayout.LayoutParams(dpToPx(44f), dpToPx(44f)));

        GradientDrawable iconBg = new GradientDrawable();
        iconBg.setColor(Color.parseColor("#1A" + COLOR_ACCENT2.replace("#","")));
        iconBg.setCornerRadius(dpToPx(12f));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) icon.setBackground(iconBg);
        else icon.setBackgroundDrawable(iconBg);

        LinearLayout textLayout = new LinearLayout(this);
        textLayout.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams textParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1);
        textParams.leftMargin = dpToPx(14f);
        textParams.rightMargin = dpToPx(8f);
        textLayout.setLayoutParams(textParams);

        TextView tvTitle = new TextView(this);
        tvTitle.setText(title);
        tvTitle.setTextColor(Color.parseColor(COLOR_TEXT));
        tvTitle.setTextSize(14);
        tvTitle.setTypeface(null, Typeface.BOLD);

        TextView tvSub = new TextView(this);
        tvSub.setText(subText);
        tvSub.setTextColor(Color.parseColor(COLOR_TEXT_SEC));
        tvSub.setTextSize(11);
        tvSub.setLineSpacing(0f, 1.1f);

        textLayout.addView(tvTitle);
        textLayout.addView(tvSub);

        final TextView btnState = new TextView(this);
        btnState.setTextSize(11);
        btnState.setPadding(dpToPx(14f), dpToPx(6f), dpToPx(14f), dpToPx(6f));
        btnState.setTypeface(null, Typeface.BOLD);

        View.OnClickListener clickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (type == 0) {
                    if (Build.VERSION.SDK_INT >= 23 && !Settings.canDrawOverlays(MainActivity.this)) {
                        try { startActivityForResult(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION), REQ_OVERLAY); } catch (Exception e) {}
                    } else {
                        toast("Quyền hiển thị đã được cấp");
                    }
                } else if (type == 1) {
                    try {
                        Intent i = VpnService.prepare(MainActivity.this);
                        if (i != null) {
                            startActivityForResult(i, REQ_VPN);
                        } else {
                            toast("Quyền VPN đã được cấp");
                        }
                    } catch (Exception e) {}
                } else if (type == 2) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        if (checkSelfPermission("android.permission.POST_NOTIFICATIONS") != PackageManager.PERMISSION_GRANTED) {
                            openNotificationSettings();
                        } else {
                            toast("Quyền thông báo đã được cấp");
                        }
                    } else {
                        toast("Quyền thông báo đã được cấp (Android cũ)");
                    }
                }
                refreshPermissionStates();
            }
        };

        btnState.setOnClickListener(clickListener);
        row.setOnClickListener(clickListener);

        row.addView(icon);
        row.addView(textLayout);
        row.addView(btnState);

        if (type == 0) btnDisplayState = btnState;
        else if (type == 1) btnVpnState = btnState;
        else if (type == 2) btnNotifState = btnState;

        return row;
    }

    private void openNotificationSettings() {
        try {
            Intent intent = new Intent();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                intent.setAction(Settings.ACTION_APP_NOTIFICATION_SETTINGS);
                intent.putExtra(Settings.EXTRA_APP_PACKAGE, getPackageName());
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                intent.setData(Uri.parse("package:" + getPackageName()));
            } else {
                intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                intent.setData(Uri.parse("package:" + getPackageName()));
            }
            startActivity(intent);
        } catch (Exception e) {
            try {
                Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                intent.setData(Uri.parse("package:" + getPackageName()));
                startActivity(intent);
            } catch (Exception ex) {
                toast("Không thể mở cài đặt");
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQ_NOTIFICATION_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                toast("Đã cấp quyền thông báo");
                refreshPermissionStates();
            } else {
                toast("Cần cấp quyền thông báo để nhận thông báo");
            }
        }
    }

    private void updatePermissionButtonState(TextView btn, boolean isGranted) {
        if (btn == null) return;
        if (isGranted) {
            btn.setText("ĐÃ CẤP");
            btn.setTextColor(Color.parseColor(COLOR_SUCCESS));
            GradientDrawable bg = new GradientDrawable();
            bg.setColor(Color.parseColor("#1A" + COLOR_SUCCESS.replace("#","")));
            bg.setStroke(dpToPx(1f), Color.parseColor("#4D" + COLOR_SUCCESS.replace("#","")));
            bg.setCornerRadius(dpToPx(8f));
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) btn.setBackground(bg);
            else btn.setBackgroundDrawable(bg);
        } else {
            btn.setText("CẤP QUYỀN");
            btn.setTextColor(Color.parseColor(COLOR_DANGER));
            GradientDrawable bg = new GradientDrawable();
            bg.setColor(Color.parseColor("#1A" + COLOR_DANGER.replace("#","")));
            bg.setStroke(dpToPx(1f), Color.parseColor("#4D" + COLOR_DANGER.replace("#","")));
            bg.setCornerRadius(dpToPx(8f));
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) btn.setBackground(bg);
            else btn.setBackgroundDrawable(bg);
        }
    }

    private void refreshPermissionStates() {
        boolean vpnOk = false;
        try { vpnOk = (VpnService.prepare(this) == null); } catch (Exception e) { }
        updatePermissionButtonState(btnVpnState, vpnOk);

        boolean displayOk = (Build.VERSION.SDK_INT < 23) || Settings.canDrawOverlays(this);
        updatePermissionButtonState(btnDisplayState, displayOk);

        boolean notifOk = true;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            try {
                notifOk = (checkSelfPermission("android.permission.POST_NOTIFICATIONS") == PackageManager.PERMISSION_GRANTED);
            } catch (Exception e) { }
        }
        updatePermissionButtonState(btnNotifState, notifOk);
    }

    private LinearLayout createDarkCard() {
        LinearLayout card = new LinearLayout(this);
        card.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        card.setOrientation(LinearLayout.VERTICAL);
        GradientDrawable d = new GradientDrawable();
        d.setColor(Color.parseColor(COLOR_CARD_BG));
        d.setStroke(dpToPx(1f), Color.parseColor(COLOR_CARD_STROKE));
        d.setCornerRadius(dpToPx(20f));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) card.setBackground(d);
        else card.setBackgroundDrawable(d);
        card.setPadding(dpToPx(20f), dpToPx(20f), dpToPx(20f), dpToPx(20f));
        return card;
    }

    private LinearLayout createStartStopCard() {
        LinearLayout container = new LinearLayout(this);
        container.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        container.setOrientation(LinearLayout.HORIZONTAL);
        container.setPadding(0, dpToPx(12f), 0, dpToPx(8f));

        Button btnStart = new Button(this);
        btnStart.setText("▶ Khởi Chạy");
        btnStart.setTextColor(Color.WHITE);
        btnStart.setTextSize(15);
        btnStart.setTypeface(null, Typeface.BOLD);
        GradientDrawable bgStart = new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,
                new int[]{Color.parseColor(COLOR_ACCENT), Color.parseColor(COLOR_ACCENT2)});
        bgStart.setCornerRadius(dpToPx(16f));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) btnStart.setBackground(bgStart);
        else btnStart.setBackgroundDrawable(bgStart);

        Button btnStop = new Button(this);
        btnStop.setText("■ Dừng");
        btnStop.setTextColor(Color.parseColor(COLOR_TEXT_SEC));
        btnStop.setTextSize(15);
        btnStop.setTypeface(null, Typeface.BOLD);
        GradientDrawable bgStop = new GradientDrawable();
        bgStop.setColor(Color.parseColor("#1E1A30"));
        bgStop.setStroke(dpToPx(1f), Color.parseColor(COLOR_CARD_STROKE));
        bgStop.setCornerRadius(dpToPx(16f));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) btnStop.setBackground(bgStop);
        else btnStop.setBackgroundDrawable(bgStop);

        LinearLayout.LayoutParams p1 = new LinearLayout.LayoutParams(0, dpToPx(60f), 1);
        p1.rightMargin = dpToPx(8f);
        btnStart.setLayoutParams(p1);

        LinearLayout.LayoutParams p2 = new LinearLayout.LayoutParams(0, dpToPx(60f), 1);
        p2.leftMargin = dpToPx(8f);
        btnStop.setLayoutParams(p2);

        btnStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) { if (!waiting && !engineOn) checkOverlay(); }
        });

        btnStop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) { if (!waiting && engineOn) stopEngine(); }
        });

        container.addView(btnStart);
        container.addView(btnStop);
        return container;
    }

    private LinearLayout createPingMonitorCard() {
        LinearLayout outer = new LinearLayout(this);
        LinearLayout.LayoutParams outerParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        outerParams.topMargin = dpToPx(16f);
        outer.setLayoutParams(outerParams);
        outer.setOrientation(LinearLayout.VERTICAL);

        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setPadding(0, 0, 0, dpToPx(12f));

        TextView lblTitle = new TextView(this);
        lblTitle.setText("▐ ĐỘ TRỄ MẠNG (PING)");
        lblTitle.setTextColor(Color.parseColor(COLOR_TEXT_SEC));
        lblTitle.setTextSize(12);
        lblTitle.setTypeface(null, Typeface.BOLD);
        lblTitle.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));

        tvPingHeaderRight = new TextView(this);
        tvPingHeaderRight.setText("...");
        tvPingHeaderRight.setTextColor(Color.parseColor(COLOR_SUCCESS));
        tvPingHeaderRight.setTextSize(13);
        tvPingHeaderRight.setTypeface(null, Typeface.BOLD);
        startBlink(tvPingHeaderRight);

        header.addView(lblTitle);
        header.addView(tvPingHeaderRight);
        outer.addView(header);

        LinearLayout card = createDarkCard();

        LinearLayout rowStats = new LinearLayout(this);
        rowStats.setOrientation(LinearLayout.HORIZONTAL);
        rowStats.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        rowStats.setWeightSum(4.0f);
        rowStats.setGravity(Gravity.CENTER);
        rowStats.setPadding(0, dpToPx(10f), 0, dpToPx(24f));

        tvPingMin = addPingStatCol(rowStats, "MIN", COLOR_SUCCESS);
        tvPingAvg = addPingStatCol(rowStats, "AVG", COLOR_GOLD);
        tvPingMax = addPingStatCol(rowStats, "MAX", COLOR_DANGER);
        tvPingJitter = addPingStatCol(rowStats, "JITTER", COLOR_ACCENT2);
        card.addView(rowStats);

        pingChartView = new PingLineChartView(this);
        pingChartView.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dpToPx(80f)));
        card.addView(pingChartView);

        LinearLayout footer = new LinearLayout(this);
        footer.setOrientation(LinearLayout.HORIZONTAL);
        footer.setPadding(0, dpToPx(16f), 0, 0);

        TextView lblFoot = new TextView(this);
        lblFoot.setText("— Biểu đồ theo thời gian thực");
        lblFoot.setTextColor(Color.parseColor(COLOR_TEXT_SEC));
        lblFoot.setTextSize(11);
        lblFoot.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));

        tvPingStatusWord = new TextView(this);
        tvPingStatusWord.setText("• ĐANG TẢI...");
        tvPingStatusWord.setTextColor(Color.parseColor(COLOR_GOLD));
        tvPingStatusWord.setTextSize(11);
        tvPingStatusWord.setTypeface(null, Typeface.BOLD);

        footer.addView(lblFoot);
        footer.addView(tvPingStatusWord);
        card.addView(footer);

        outer.addView(card);

        if(isInitialPingLoaded) {
            updatePingStats(lastPing);
        }
        return outer;
    }

    private TextView addPingStatCol(LinearLayout parent, String title, String colorHex) {
        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setGravity(Gravity.CENTER);
        col.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f));

        TextView lbl = new TextView(this);
        lbl.setText(title);
        lbl.setTextColor(Color.parseColor(COLOR_TEXT_SEC));
        lbl.setTextSize(10);
        lbl.setTypeface(null, Typeface.BOLD);
        lbl.setGravity(Gravity.CENTER);

        TextView val = new TextView(this);
        val.setText("...");
        val.setTextColor(Color.parseColor(colorHex));
        val.setTextSize(18);
        val.setTypeface(null, Typeface.BOLD);
        val.setPadding(0, dpToPx(4f), 0, dpToPx(2f));
        val.setGravity(Gravity.CENTER);
        startBlink(val);

        TextView ms = new TextView(this);
        ms.setText("ms");
        ms.setTextColor(Color.parseColor(COLOR_TEXT_SEC));
        ms.setTextSize(9);
        ms.setGravity(Gravity.CENTER);

        col.addView(lbl);
        col.addView(val);
        col.addView(ms);
        parent.addView(col);
        return val;
    }

    private LinearLayout createDeviceTempCard() {
        LinearLayout outer = new LinearLayout(this);
        LinearLayout.LayoutParams outerParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        outerParams.topMargin = dpToPx(20f);
        outerParams.bottomMargin = dpToPx(20f);
        outer.setLayoutParams(outerParams);
        outer.setOrientation(LinearLayout.VERTICAL);

        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setPadding(0, 0, 0, dpToPx(12f));

        TextView lblTitle = new TextView(this);
        lblTitle.setText("▐ THÔNG TIN HỆ THỐNG");
        lblTitle.setTextColor(Color.parseColor(COLOR_TEXT_SEC));
        lblTitle.setTextSize(12);
        lblTitle.setTypeface(null, Typeface.BOLD);
        lblTitle.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));

        tvDeviceTempStatusTop = new TextView(this);
        tvDeviceTempStatusTop.setText("...");
        tvDeviceTempStatusTop.setTextColor(Color.parseColor(COLOR_SUCCESS));
        tvDeviceTempStatusTop.setTextSize(11);
        tvDeviceTempStatusTop.setTypeface(null, Typeface.BOLD);
        tvDeviceTempStatusTop.setPadding(dpToPx(10f), dpToPx(4f), dpToPx(10f), dpToPx(4f));
        GradientDrawable bgSt = new GradientDrawable();
        bgSt.setColor(Color.parseColor("#1A" + COLOR_SUCCESS.replace("#","")));
        bgSt.setStroke(dpToPx(1f), Color.parseColor("#4D" + COLOR_SUCCESS.replace("#","")));
        bgSt.setCornerRadius(dpToPx(10f));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) tvDeviceTempStatusTop.setBackground(bgSt);
        else tvDeviceTempStatusTop.setBackgroundDrawable(bgSt);
        startBlink(tvDeviceTempStatusTop);

        header.addView(lblTitle);
        header.addView(tvDeviceTempStatusTop);
        outer.addView(header);

        LinearLayout card = createDarkCard();

        LinearLayout midRow = new LinearLayout(this);
        midRow.setOrientation(LinearLayout.HORIZONTAL);
        midRow.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        midRow.setGravity(Gravity.CENTER_VERTICAL);

        ImageView iconFlash = new ImageView(this);
        int icExp = getResources().getIdentifier("ic_experiment", "drawable", getPackageName());
        if (icExp != 0) iconFlash.setImageResource(icExp);
        iconFlash.setScaleType(ImageView.ScaleType.FIT_CENTER);
        iconFlash.setColorFilter(Color.parseColor(COLOR_ACCENT));
        int pExp = dpToPx(14f);
        iconFlash.setPadding(pExp, pExp, pExp, pExp);
        LinearLayout.LayoutParams pIcon = new LinearLayout.LayoutParams(dpToPx(60f), dpToPx(60f));
        iconFlash.setLayoutParams(pIcon);

        GradientDrawable bgIcon = new GradientDrawable();
        bgIcon.setColor(Color.parseColor("#1A" + COLOR_ACCENT.replace("#","")));
        bgIcon.setCornerRadius(dpToPx(16f));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) iconFlash.setBackground(bgIcon);
        else iconFlash.setBackgroundDrawable(bgIcon);

        LinearLayout centerCpu = new LinearLayout(this);
        centerCpu.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams pCenter = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1);
        pCenter.leftMargin = dpToPx(16f);
        centerCpu.setLayoutParams(pCenter);

        TextView lblCpuTemp = new TextView(this);
        lblCpuTemp.setText("Nhiệt Độ CPU");
        lblCpuTemp.setTextColor(Color.parseColor(COLOR_TEXT_SEC));
        lblCpuTemp.setTextSize(10);
        lblCpuTemp.setTypeface(null, Typeface.BOLD);

        tvCpuTempBig = new TextView(this);
        tvCpuTempBig.setText("...");
        tvCpuTempBig.setTextColor(Color.parseColor(COLOR_SUCCESS));
        tvCpuTempBig.setTextSize(36);
        tvCpuTempBig.setTypeface(null, Typeface.BOLD);
        startBlink(tvCpuTempBig);

        centerCpu.addView(lblCpuTemp);
        centerCpu.addView(tvCpuTempBig);

        LinearLayout rightBars = new LinearLayout(this);
        rightBars.setOrientation(LinearLayout.VERTICAL);
        rightBars.setGravity(Gravity.RIGHT);

        TextView lblMucDo = new TextView(this);
        lblMucDo.setText("TẢI");
        lblMucDo.setTextColor(Color.parseColor(COLOR_TEXT_SEC));
        lblMucDo.setTextSize(9);
        lblMucDo.setPadding(0, 0, 0, dpToPx(6f));

        layoutTempBars = new LinearLayout(this);
        layoutTempBars.setOrientation(LinearLayout.HORIZONTAL);
        layoutTempBars.setGravity(Gravity.BOTTOM);
        layoutTempBars.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, dpToPx(20f)));

        int[] hBar = {8, 12, 16, 20};
        for (int i = 0; i < 4; i++) {
            View bar = new View(this);
            LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams(dpToPx(6f), dpToPx(hBar[i]));
            bp.leftMargin = dpToPx(4f);
            bar.setLayoutParams(bp);
            
            GradientDrawable bbg = new GradientDrawable();
            bbg.setColor(Color.parseColor(COLOR_CARD_STROKE));
            bbg.setCornerRadius(dpToPx(2f));
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) bar.setBackground(bbg);
            else bar.setBackgroundDrawable(bbg);
            
            layoutTempBars.addView(bar);
        }
        rightBars.addView(lblMucDo);
        rightBars.addView(layoutTempBars);

        midRow.addView(iconFlash);
        midRow.addView(centerCpu);
        midRow.addView(rightBars);
        card.addView(midRow);

        View divider = new View(this);
        LinearLayout.LayoutParams pDiv = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 1);
        pDiv.setMargins(0, dpToPx(30f), 0, dpToPx(30f));
        divider.setLayoutParams(pDiv);
        divider.setBackgroundColor(Color.parseColor(COLOR_CARD_STROKE));
        card.addView(divider);

        LinearLayout botRow = new LinearLayout(this);
        botRow.setOrientation(LinearLayout.HORIZONTAL);
        botRow.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        botRow.setWeightSum(3.0f);
        botRow.setGravity(Gravity.CENTER);

        tvCpuUsagePercent = addDeviceStatCol(botRow, "CPU", "...", "sử dụng", COLOR_ACCENT2);
        tvRamUsageMb = addDeviceStatCol(botRow, "RAM", "...", "sử dụng", COLOR_ACCENT);
        tvBatteryTempBottom = addDeviceStatCol(botRow, "PIN", "...", "nhiệt độ", COLOR_GOLD);

        card.addView(botRow);

        outer.addView(card);
        return outer;
    }

    private TextView addDeviceStatCol(LinearLayout parent, String title, String valTxt, String subTxt, String colorHex) {
        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setGravity(Gravity.CENTER);
        col.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f));

        TextView lbl = new TextView(this);
        lbl.setText(title);
        lbl.setTextColor(Color.parseColor(COLOR_TEXT_SEC));
        lbl.setTextSize(10);
        lbl.setGravity(Gravity.CENTER);
        lbl.setTypeface(null, Typeface.BOLD);

        TextView val = new TextView(this);
        val.setText(valTxt);
        if (valTxt.equals("...")) {
            startBlink(val);
        }
        val.setTextColor(Color.parseColor(colorHex));
        val.setTextSize(16);
        val.setTypeface(null, Typeface.BOLD);
        val.setPadding(0, dpToPx(4f), 0, dpToPx(2f));
        val.setGravity(Gravity.CENTER);

        TextView sub = new TextView(this);
        sub.setText(subTxt);
        sub.setTextColor(Color.parseColor("#4B5563"));
        sub.setTextSize(9);
        sub.setGravity(Gravity.CENTER);

        col.addView(lbl);
        col.addView(val);
        col.addView(sub);
        parent.addView(col);
        return val;
    }

    private class PingLineChartView extends View {
        private Paint linePaint;
        private Paint fillPaint;
        private Path path;
        private Path fillPath;
        private static final int MAX_POINTS = 50;

        public PingLineChartView(Context context) {
            super(context);
            linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            linePaint.setStyle(Paint.Style.STROKE);
            linePaint.setStrokeWidth(dpToPx(2f));
            linePaint.setStrokeJoin(Paint.Join.ROUND);

            fillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            fillPaint.setStyle(Paint.Style.FILL);

            path = new Path();
            fillPath = new Path();
        }

        public void addPoint(int val) {
            synchronized (staticPingPoints) {
                staticPingPoints.add(val);
                if (staticPingPoints.size() > MAX_POINTS) {
                    staticPingPoints.remove(0);
                }
            }
            invalidate();
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            
            List<Integer> drawPoints;
            synchronized (staticPingPoints) {
                if (staticPingPoints.size() < 2) return;
                drawPoints = new ArrayList<Integer>(staticPingPoints);
            }

            int width = getWidth();
            int height = getHeight();

            float sum = 0;
            float currentMax = -1f;
            float currentMin = Float.MAX_VALUE;
            
            for (int i = 0; i < drawPoints.size(); i++) {
                float v = drawPoints.get(i);
                if (v > currentMax) currentMax = v;
                if (v < currentMin) currentMin = v;
                sum += v;
            }

            float avg = sum / drawPoints.size();

            // 1. Chân biểu đồ luôn bắt đầu từ 0 theo đúng yêu cầu
            float renderMin = 0f;

            // 2. Trần biểu đồ dựa vào mức trung bình để các điểm ping thường nằm ở khoảng giữa (~50%)
            // Cắt bỏ các điểm spike (giật lag quá cao) để không làm bẹp biểu đồ thành một đường thẳng
            float renderMax = avg * 2f;
            if (renderMax < 30f) renderMax = 30f; // Đảm bảo có biên độ tối thiểu

            // Nếu đỉnh cao nhất thực tế không phải là spike quá trớn (chỉ cao hơn chút ít) 
            // thì mở rộng trần ra để thấy rõ dao động nhấp nhô
            if (currentMax > renderMax && currentMax < avg * 3f) {
                renderMax = currentMax + 5f;
            }

            float range = renderMax - renderMin;
            if (range < 5.0f) range = 5.0f;

            path.reset();
            fillPath.reset();

            float paddingBottom = dpToPx(4f);
            float paddingTop = dpToPx(4f);
            float availableHeight = height - paddingBottom - paddingTop;

            float stepX = (float) width / (MAX_POINTS - 1);
            float lastX = 0;

            for (int i = 0; i < drawPoints.size(); i++) {
                float val = drawPoints.get(i);
                float x = i * stepX;

                // Các điểm bị giật lag (vd 69ms, 120ms) sẽ bị cắt phẳng nóc tại điểm này (vượt quá 1.0f)
                // Các điểm bình thường (26ms) sẽ được trải thoải mái tại mức ~50% chiều cao
                float normalized = (val - renderMin) / range;
                if (normalized > 1.0f) normalized = 1.0f;
                if (normalized < 0.0f) normalized = 0.0f;

                float y = (height - paddingBottom) - (normalized * availableHeight);

                if (i == 0) {
                    path.moveTo(x, y);
                    fillPath.moveTo(x, y);
                } else {
                    path.lineTo(x, y);
                    fillPath.lineTo(x, y);
                }
                lastX = x;
            }

            fillPath.lineTo(lastX, height);
            fillPath.lineTo(0, height);
            fillPath.close();

            float y120 = (height - paddingBottom) - ((120f - renderMin) / range) * availableHeight;
            float y60 = (height - paddingBottom) - ((60f - renderMin) / range) * availableHeight;

            if (y120 >= y60) y120 = y60 - 1f;

            linePaint.setShader(new LinearGradient(0, y120, 0, y60,
                    new int[]{Color.parseColor(COLOR_DANGER), Color.parseColor(COLOR_GOLD), Color.parseColor(COLOR_SUCCESS)},
                    null, Shader.TileMode.CLAMP));

            if (!drawPoints.isEmpty()) {
                int currentP = drawPoints.get(drawPoints.size() - 1);
                int baseColor = getPingColor(currentP);
                int alphaColor = Color.argb(60, Color.red(baseColor), Color.green(baseColor), Color.blue(baseColor));
                fillPaint.setShader(new LinearGradient(0, 0, 0, height,
                        new int[]{alphaColor, Color.TRANSPARENT},
                        null, Shader.TileMode.CLAMP));
            }

            canvas.drawPath(fillPath, fillPaint);
            canvas.drawPath(path, linePaint);
        }
    }

    private void startHardwareMonitor() {
        isMonitoring = true;
        networkThread = new Thread(new Runnable() {
            @Override
            public void run() {
                long lastHardwareUpdate = 0;
                while (!Thread.interrupted() && isMonitoring) {
                    long currentLatency = 0;
                    long startTime = System.currentTimeMillis();
                    Socket socket = null;
                    try {
                        socket = new Socket();
                        socket.connect(new InetSocketAddress("8.8.8.8", 53), 1500);
                    } catch (Exception e) {
                        currentLatency = 999;
                    } finally {
                        if (socket != null) {
                            try { socket.close(); } catch (Exception e) {}
                        }
                    }
                    if (currentLatency != 999) {
                        currentLatency = System.currentTimeMillis() - startTime;
                    }

                    final int finalPing = (int) currentLatency;

                    long now = System.currentTimeMillis();
                    final boolean doHardwareUpdate = (now - lastHardwareUpdate >= 1000);

                    int tempCpuUsage = realCpuUsage;
                    float tempCpuTemp = realCpuTemp;
                    long tempUsedRam = 0;

                    if (doHardwareUpdate) {
                        tempCpuUsage = getRealCpuUsage();
                        tempCpuTemp = getRealCpuTemperature();
                        lastHardwareUpdate = now;

                        try {
                            ActivityManager actManager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
                            ActivityManager.MemoryInfo memInfo = new ActivityManager.MemoryInfo();
                            actManager.getMemoryInfo(memInfo);
                            long totalRam = memInfo.totalMem / 1048576L;
                            long availRam = memInfo.availMem / 1048576L;
                            tempUsedRam = totalRam - availRam;
                        } catch (Exception e) {}
                    }

                    final int cpuU = tempCpuUsage;
                    final float cpuT = tempCpuTemp;
                    final long ramU = tempUsedRam;

                    monitorHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            if (isMonitoring) {
                                updatePingStats(finalPing);
                                if (doHardwareUpdate) {
                                    realCpuUsage = cpuU;
                                    realCpuTemp = cpuT;
                                    updateHardwareStatsUI(cpuU, cpuT, ramU);
                                }
                            }
                        }
                    });

                    try { Thread.sleep(500); } catch (Exception e) { break; }
                }
            }
        });
        networkThread.start();
    }

    private void updatePingStats(int currentPing) {
        if (!isInitialPingLoaded) {
            isInitialPingLoaded = true;
            pingMin = currentPing;
            pingMax = currentPing;
            pingSum = 0;
            pingCount = 0;
            lastPing = currentPing;
        }

        if (currentPing <= 0) currentPing = 1;

        if (currentPing < 999) {
            if (currentPing < pingMin) pingMin = currentPing;
            if (currentPing > pingMax) pingMax = currentPing;
            pingCount++;
            pingSum += currentPing;
        }

        int avg = pingCount > 0 ? (int) (pingSum / pingCount) : currentPing;
        int jitter = Math.abs(currentPing - lastPing);
        lastPing = currentPing;

        pingChartView.addPoint(currentPing);

        int color = getPingColor(currentPing);

        stopBlink(tvPingHeaderRight);
        tvPingHeaderRight.setText(currentPing + " ms •");
        tvPingHeaderRight.setTextColor(color);

        stopBlink(tvPingMin);
        tvPingMin.setText(String.valueOf(pingMin));

        stopBlink(tvPingAvg);
        tvPingAvg.setText(String.valueOf(avg));

        stopBlink(tvPingMax);
        tvPingMax.setText(String.valueOf(pingMax));

        stopBlink(tvPingJitter);
        tvPingJitter.setText(String.valueOf(jitter));

        String statusTxt = "• TỐT";
        if (currentPing > 120) statusTxt = "• TỆ / MẤT KẾT NỐI";
        else if (currentPing > 60) statusTxt = "• BÌNH THƯỜNG";
        tvPingStatusWord.setText(statusTxt);
        tvPingStatusWord.setTextColor(color);
    }

    private int getPingColor(int ms) {
        if (ms <= 60) return Color.parseColor(COLOR_SUCCESS);
        if (ms <= 120) return Color.parseColor(COLOR_GOLD);
        return Color.parseColor(COLOR_DANGER);
    }

    private void updateHardwareStatsUI(int cpuU, float cpuT, long ramU) {
        int tempInt = Math.round(cpuT);
        String tempStatus = getTemperatureStatus(tempInt);
        int tempColor = getTemperatureColor(tempInt);

        stopBlink(tvCpuTempBig);
        tvCpuTempBig.setText(tempInt + "°C");
        tvCpuTempBig.setTextColor(tempColor);

        stopBlink(tvDeviceTempStatusTop);
        tvDeviceTempStatusTop.setText(tempStatus);
        tvDeviceTempStatusTop.setTextColor(tempColor);
        GradientDrawable bg = (GradientDrawable) tvDeviceTempStatusTop.getBackground();
        bg.setStroke(dpToPx(1f), Color.parseColor("#66" + String.format("%06X", (0xFFFFFF & tempColor))));
        bg.setColor(Color.parseColor("#1A" + String.format("%06X", (0xFFFFFF & tempColor))));

        int activeBars = getActiveBarsCount(tempInt);
        for (int i = 0; i < 4; i++) {
            View bar = layoutTempBars.getChildAt(i);
            GradientDrawable bbg = (GradientDrawable) bar.getBackground();
            if (i < activeBars) bbg.setColor(tempColor);
            else bbg.setColor(Color.parseColor(COLOR_CARD_STROKE));
        }

        if (ramU > 0) {
            stopBlink(tvRamUsageMb);
            tvRamUsageMb.setText(ramU + " MB");
        }

        int batInt = Math.round(batteryTemp);
        if (batInt > 0 || isInitialPingLoaded) {
            stopBlink(tvBatteryTempBottom);
            tvBatteryTempBottom.setText(batInt + " °C");
        }

        stopBlink(tvCpuUsagePercent);
        tvCpuUsagePercent.setText(cpuU + " %");
    }

    private String getTemperatureStatus(int temp) {
        if (temp <= 25) return "MÁT";
        if (temp <= 35) return "BÌNH THƯỜNG";
        if (temp <= 45) return "ẤM";
        return "NGUY HIỂM";
    }

    private int getTemperatureColor(int temp) {
        if (temp <= 25) return Color.parseColor(COLOR_ACCENT2);
        if (temp <= 35) return Color.parseColor(COLOR_SUCCESS);
        if (temp <= 45) return Color.parseColor(COLOR_GOLD);
        return Color.parseColor(COLOR_DANGER);
    }

    private int getActiveBarsCount(int temp) {
        if (temp <= 20) return 1;
        if (temp <= 30) return 2;
        if (temp <= 40) return 3;
        return 4;
    }

    private String readLineFromFile(File file) {
        if (file == null || !file.exists() || !file.canRead()) return null;
        try {
            BufferedReader br = new BufferedReader(new FileReader(file));
            String line = br.readLine();
            br.close();
            return line;
        } catch (Exception e) {
            return null;
        }
    }

    private float getRealCpuTemperature() {
        if (cachedThermalFile != null && cachedThermalFile.exists()) {
            String tempStr = readLineFromFile(cachedThermalFile);
            if (tempStr != null && !tempStr.trim().isEmpty()) {
                try {
                    float t = Float.parseFloat(tempStr.trim());
                    if (t > 10000) t = t / 1000.0f;
                    else if (t > 1000) t = t / 10.0f;
                    if (t > 20.0f && t < 110.0f) return t;
                } catch (Exception e) {}
            }
        }

        try {
            File dir = new File("/sys/class/thermal/");
            if (dir.exists()) {
                File[] files = dir.listFiles();
                if (files != null) {
                    for (int i = 0; i < files.length; i++) {
                        File f = files[i];
                        if (f.getName().startsWith("thermal_zone")) {
                            String type = readLineFromFile(new File(f, "type"));
                            if (type != null) {
                                type = type.toLowerCase();
                                if (type.contains("cpu") || type.contains("tsens") || type.contains("mtkt") || type.contains("bms") || type.contains("soc")) {
                                    File tempFile = new File(f, "temp");
                                    String tempStr = readLineFromFile(tempFile);
                                    if (tempStr != null && !tempStr.trim().isEmpty()) {
                                        float t = Float.parseFloat(tempStr.trim());
                                        if (t > 10000) t = t / 1000.0f;
                                        else if (t > 1000) t = t / 10.0f;

                                        if (t > 20.0f && t < 110.0f) {
                                            cachedThermalFile = tempFile;
                                            return t;
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            String[] paths = {
                    "/sys/class/thermal/thermal_zone0/temp",
                    "/sys/class/thermal/thermal_zone1/temp",
                    "/sys/devices/system/cpu/cpu0/cpufreq/cpu_temp",
                    "/sys/class/i2c-adapter/i2c-4/4-004c/temperature"
            };
            for (int i = 0; i < paths.length; i++) {
                File tFile = new File(paths[i]);
                String line = readLineFromFile(tFile);
                if (line != null && !line.trim().isEmpty()) {
                    float t = Float.parseFloat(line.trim());
                    if (t > 10000) t = t / 1000.0f;
                    else if (t > 1000) t = t / 10.0f;
                    if (t > 20.0f && t < 110.0f) {
                        cachedThermalFile = tFile;
                        return t;
                    }
                }
            }
        } catch (Exception e) {}

        if (batteryTemp > 20.0f) {
            return batteryTemp + 2.0f;
        }
        return 0f;
    }

    private int getRealCpuUsage() {
        int calculatedUsage = 0;
        boolean freqSuccess = false;

        try {
            int numCores = Runtime.getRuntime().availableProcessors();
            float totalUsage = 0;
            int activeCores = 0;

            for (int i = 0; i < numCores; i++) {
                try {
                    String maxStr = readLineFromFile(new File("/sys/devices/system/cpu/cpu" + i + "/cpufreq/cpuinfo_max_freq"));
                    String curStr = readLineFromFile(new File("/sys/devices/system/cpu/cpu" + i + "/cpufreq/scaling_cur_freq"));
                    if (maxStr != null && curStr != null) {
                        float max = Float.parseFloat(maxStr.trim());
                        float cur = Float.parseFloat(curStr.trim());
                        if (max > 0) {
                            totalUsage += (cur / max) * 100;
                            activeCores++;
                        }
                    }
                } catch (Exception exCore) {}
            }
            if (activeCores > 0) {
                calculatedUsage = (int) (totalUsage / activeCores);
                if(calculatedUsage > 100) calculatedUsage = 100;
                freqSuccess = true;
            }
        } catch (Exception e) {}

        if (!freqSuccess) {
            try {
                RandomAccessFile reader = new RandomAccessFile("/proc/stat", "r");
                String load = reader.readLine();
                reader.close();

                String[] toks = load.split(" +");
                long idle = Long.parseLong(toks[4]);
                long total = 0;
                for (int i = 1; i < toks.length; i++) {
                    total += Long.parseLong(toks[i]);
                }

                if (lastCpuTotal == 0) {
                    lastCpuTotal = total;
                    lastCpuIdle = idle;
                    return realCpuUsage > 0 ? realCpuUsage : 0;
                }

                long totald = total - lastCpuTotal;
                long idled = idle - lastCpuIdle;

                lastCpuTotal = total;
                lastCpuIdle = idle;

                if (totald > 0) {
                    calculatedUsage = (int) (100 * (totald - idled) / totald);
                    calculatedUsage = Math.max(0, Math.min(100, calculatedUsage));
                } else {
                    calculatedUsage = realCpuUsage;
                }
            } catch (Exception e) {
                calculatedUsage = realCpuUsage;
            }
        }

        if (System.currentTimeMillis() - resumeTime < 2500) {
            if (realCpuUsage > 0 && calculatedUsage > 85) {
                return realCpuUsage;
            }
        }

        if (realCpuUsage > 0) {
            calculatedUsage = (int) (realCpuUsage * 0.4f + calculatedUsage * 0.6f);
        }

        return calculatedUsage;
    }

    private ScrollView createImpairView() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setOverScrollMode(ScrollView.OVER_SCROLL_NEVER);
        scroll.setVerticalScrollBarEnabled(false);

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(dpToPx(16f), dpToPx(8f), dpToPx(16f), dpToPx(16f));

        LinearLayout tabContainer = new LinearLayout(this);
        tabContainer.setOrientation(LinearLayout.HORIZONTAL);

        LinearLayout.LayoutParams tabParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dpToPx(48f));
        tabParams.topMargin = dpToPx(8f);
        tabParams.bottomMargin = dpToPx(16f);
        tabContainer.setLayoutParams(tabParams);

        GradientDrawable tabContainerBg = new GradientDrawable();
        tabContainerBg.setColor(Color.parseColor(COLOR_CARD_BG));
        tabContainerBg.setStroke(dpToPx(1f), Color.parseColor(COLOR_CARD_STROKE));
        tabContainerBg.setCornerRadius(dpToPx(24f));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) tabContainer.setBackground(tabContainerBg);
        else tabContainer.setBackgroundDrawable(tabContainerBg);
        tabContainer.setPadding(dpToPx(6f), dpToPx(6f), dpToPx(6f), dpToPx(6f));

        tabAllBg = new LinearLayout(this);
        tabAllBg.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f));
        tabAllBg.setGravity(Gravity.CENTER);
        tvTabAll = new TextView(this);
        tvTabAll.setText("Tất Cả");
        tvTabAll.setTextSize(13);
        if (defaultFont != null) tvTabAll.setTypeface(defaultFont, Typeface.BOLD);
        tabAllBg.addView(tvTabAll);

        tabSafeBg = new LinearLayout(this);
        tabSafeBg.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f));
        tabSafeBg.setGravity(Gravity.CENTER);
        tvTabSafe = new TextView(this);
        tvTabSafe.setText("An Toàn");
        tvTabSafe.setTextSize(13);
        if (defaultFont != null) tvTabSafe.setTypeface(defaultFont, Typeface.BOLD);
        tabSafeBg.addView(tvTabSafe);

        tabRiskBg = new LinearLayout(this);
        tabRiskBg.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f));
        tabRiskBg.setGravity(Gravity.CENTER);
        tvTabRisk = new TextView(this);
        tvTabRisk.setText("Rủi Ro Cao");
        tvTabRisk.setTextSize(13);
        if (defaultFont != null) tvTabRisk.setTypeface(defaultFont, Typeface.BOLD);
        tabRiskBg.addView(tvTabRisk);

        tabContainer.addView(tabAllBg);
        tabContainer.addView(tabSafeBg);
        tabContainer.addView(tabRiskBg);

        layout.addView(tabContainer);

        cardFreeze = createFeatureCard("Freeze", "Đóng băng kẻ địch", COLOR_ACCENT, 1);
        cardGhost = createFeatureCard("Ghost", "Ẩn thân khỏi kẻ địch", COLOR_ACCENT2, 2);
        cardTeleport = createFeatureCard("Teleport", "Dịch chuyển tức thời", COLOR_GOLD, 3);

        layout.addView(cardFreeze);
        layout.addView(cardGhost);
        layout.addView(cardTeleport);

        layout.addView(createWarningBox());

        scroll.addView(layout);

        tabAllBg.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { switchImpairSubTab(0); }
        });
        tabSafeBg.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { switchImpairSubTab(1); }
        });
        tabRiskBg.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { switchImpairSubTab(2); }
        });

        switchImpairSubTab(0);

        return scroll;
    }

    private void switchImpairSubTab(int index) {
        tabAllBg.setBackgroundColor(Color.TRANSPARENT);
        tabSafeBg.setBackgroundColor(Color.TRANSPARENT);
        tabRiskBg.setBackgroundColor(Color.TRANSPARENT);
        tvTabAll.setTextColor(Color.parseColor(COLOR_TEXT_SEC));
        tvTabSafe.setTextColor(Color.parseColor(COLOR_TEXT_SEC));
        tvTabRisk.setTextColor(Color.parseColor(COLOR_TEXT_SEC));

        GradientDrawable activeBg = new GradientDrawable();
        activeBg.setColor(Color.parseColor(COLOR_CARD_STROKE));
        activeBg.setCornerRadius(dpToPx(18f));

        if (index == 0) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) tabAllBg.setBackground(activeBg);
            else tabAllBg.setBackgroundDrawable(activeBg);
            tvTabAll.setTextColor(Color.parseColor(COLOR_TEXT));

            if (cardFreeze != null) cardFreeze.setVisibility(View.VISIBLE);
            if (cardGhost != null) cardGhost.setVisibility(View.VISIBLE);
            if (cardTeleport != null) cardTeleport.setVisibility(View.VISIBLE);
            if (warningRowSafe != null) warningRowSafe.setVisibility(View.VISIBLE);
            if (warningRowRisk != null) warningRowRisk.setVisibility(View.VISIBLE);
        } else if (index == 1) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) tabSafeBg.setBackground(activeBg);
            else tabSafeBg.setBackgroundDrawable(activeBg);
            tvTabSafe.setTextColor(Color.parseColor(COLOR_TEXT));

            if (cardFreeze != null) cardFreeze.setVisibility(View.VISIBLE);
            if (cardGhost != null) cardGhost.setVisibility(View.VISIBLE);
            if (cardTeleport != null) cardTeleport.setVisibility(View.GONE);
            if (warningRowSafe != null) warningRowSafe.setVisibility(View.VISIBLE);
            if (warningRowRisk != null) warningRowRisk.setVisibility(View.GONE);
        } else if (index == 2) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) tabRiskBg.setBackground(activeBg);
            else tabRiskBg.setBackgroundDrawable(activeBg);
            tvTabRisk.setTextColor(Color.parseColor(COLOR_TEXT));

            if (cardFreeze != null) cardFreeze.setVisibility(View.GONE);
            if (cardGhost != null) cardGhost.setVisibility(View.GONE);
            if (cardTeleport != null) cardTeleport.setVisibility(View.VISIBLE);
            if (warningRowSafe != null) warningRowSafe.setVisibility(View.GONE);
            if (warningRowRisk != null) warningRowRisk.setVisibility(View.VISIBLE);
        }
    }

    private LinearLayout createWarningBox() {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.topMargin = dpToPx(24f);
        params.bottomMargin = dpToPx(24f);
        box.setLayoutParams(params);
        box.setPadding(dpToPx(20f), dpToPx(20f), dpToPx(20f), dpToPx(20f));

        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Color.parseColor("#1A" + COLOR_GOLD.replace("#","")));
        bg.setStroke(dpToPx(1f), Color.parseColor("#4D" + COLOR_GOLD.replace("#","")));
        bg.setCornerRadius(dpToPx(20f));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) box.setBackground(bg);
        else box.setBackgroundDrawable(bg);

        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(0, 0, 0, dpToPx(16f));

        ImageView iconLght = new ImageView(this);
        int resLght = getResources().getIdentifier("ic_lightning", "drawable", getPackageName());
        if(resLght != 0) {
            iconLght.setImageResource(resLght);
            iconLght.setColorFilter(Color.parseColor(COLOR_GOLD));
        } else {
            GradientDrawable def = new GradientDrawable(); def.setColor(Color.parseColor(COLOR_GOLD));
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) iconLght.setBackground(def);
            else iconLght.setBackgroundDrawable(def);
        }
        LinearLayout.LayoutParams lghtParams = new LinearLayout.LayoutParams(dpToPx(24f), dpToPx(24f));
        lghtParams.rightMargin = dpToPx(12f);
        iconLght.setLayoutParams(lghtParams);

        TextView title = new TextView(this);
        title.setText("LƯU Ý SỬ DỤNG");
        title.setTextColor(Color.parseColor(COLOR_GOLD));
        title.setTextSize(14);
        title.setTypeface(null, Typeface.BOLD);

        header.addView(iconLght);
        header.addView(title);

        View div = new View(this);
        div.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dpToPx(1f)));
        div.setBackgroundColor(Color.parseColor("#33" + COLOR_GOLD.replace("#","")));

        View spc = new View(this);
        spc.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dpToPx(16f)));

        warningRowSafe = createBulletRow(COLOR_SUCCESS, "Tính năng Freeze, Ghost an toàn nếu chơi không lộ.");
        warningRowRisk = createBulletRow(COLOR_DANGER, "Tính năng Teleport , Telekill có tỉ lệ ban off cao và ban ID cao, hãy sử dụng một cách tư duy nhất có thể để an toàn.");

        box.addView(header);
        box.addView(div);
        box.addView(spc);
        box.addView(warningRowSafe);
        box.addView(warningRowRisk);

        return box;
    }

    private LinearLayout createBulletRow(String colorHex, String text) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        row.setPadding(0, dpToPx(6f), 0, dpToPx(10f));

        View dot = new View(this);
        LinearLayout.LayoutParams dotParams = new LinearLayout.LayoutParams(dpToPx(8f), dpToPx(8f));
        dotParams.topMargin = dpToPx(5f);
        dotParams.rightMargin = dpToPx(12f);
        dot.setLayoutParams(dotParams);
        GradientDrawable bgDot = new GradientDrawable();
        bgDot.setShape(GradientDrawable.OVAL);
        bgDot.setColor(Color.parseColor(colorHex));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) dot.setBackground(bgDot);
        else dot.setBackgroundDrawable(bgDot);

        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextColor(Color.parseColor(COLOR_TEXT));
        tv.setTextSize(13);
        tv.setLineSpacing(0, 1.3f);
        if (defaultFont != null) tv.setTypeface(defaultFont);

        row.addView(dot);
        row.addView(tv);
        return row;
    }

    private LinearLayout createFeatureCard(String title, String description, final String accentColor, final int featureId) {
        LinearLayout card = createDarkCard();
        card.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));
        LinearLayout.LayoutParams cardParams = (LinearLayout.LayoutParams) card.getLayoutParams();
        cardParams.topMargin = dpToPx(12f);
        card.setLayoutParams(cardParams);

        LinearLayout inner = new LinearLayout(this);
        inner.setOrientation(LinearLayout.VERTICAL);
        inner.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));

        LinearLayout headerRow = new LinearLayout(this);
        headerRow.setOrientation(LinearLayout.HORIZONTAL);
        headerRow.setGravity(Gravity.CENTER_VERTICAL);
        headerRow.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));

        LinearLayout textContainer = new LinearLayout(this);
        textContainer.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        textContainer.setOrientation(LinearLayout.VERTICAL);

        TextView titleText = new TextView(this);
        titleText.setText(title);
        titleText.setTextSize(18);
        titleText.setTextColor(Color.parseColor(accentColor));
        if (defaultFont != null) titleText.setTypeface(defaultFont, Typeface.BOLD);
        else titleText.setTypeface(null, Typeface.BOLD);

        TextView descText = new TextView(this);
        descText.setText(description);
        descText.setTextSize(12);
        descText.setTextColor(Color.parseColor(COLOR_TEXT_SEC));
        if (defaultFont != null) descText.setTypeface(defaultFont);

        textContainer.addView(titleText);
        textContainer.addView(descText);

        final ImageView arrowIcon = new ImageView(this);
        LinearLayout.LayoutParams arrowParams = new LinearLayout.LayoutParams(dpToPx(24f), dpToPx(24f));
        arrowParams.leftMargin = dpToPx(8f);
        arrowIcon.setLayoutParams(arrowParams);
        arrowIcon.setColorFilter(Color.parseColor(COLOR_TEXT_SEC));
        final int iconDown = getResources().getIdentifier("ic_chevron_down", "drawable", getPackageName());
        final int iconUp = getResources().getIdentifier("ic_chevron_up", "drawable", getPackageName());
        if (iconDown != 0) arrowIcon.setImageResource(iconDown);

        headerRow.addView(textContainer);
        headerRow.addView(arrowIcon);

        final LinearLayout contentContainer = new LinearLayout(this);
        contentContainer.setOrientation(LinearLayout.VERTICAL);
        contentContainer.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));
        contentContainer.setVisibility(View.GONE);

        View divider = new View(this);
        LinearLayout.LayoutParams divParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dpToPx(1f)
        );
        divParams.topMargin = dpToPx(16f);
        divParams.bottomMargin = dpToPx(16f);
        divider.setLayoutParams(divParams);
        divider.setBackgroundColor(Color.parseColor(COLOR_CARD_STROKE));

        LinearLayout sizeSection = createModernSizeLayout("Kích cỡ", featureId, accentColor);
        LinearLayout opacitySection = createModernOpacityLayout("Độ mờ", featureId, accentColor);

        LinearLayout visToggleBox = new LinearLayout(this);
        visToggleBox.setOrientation(LinearLayout.HORIZONTAL);
        visToggleBox.setGravity(Gravity.CENTER_VERTICAL);
        visToggleBox.setPadding(dpToPx(16f), dpToPx(14f), dpToPx(16f), dpToPx(14f));
        LinearLayout.LayoutParams visBoxParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        visBoxParams.topMargin = dpToPx(16f);
        visToggleBox.setLayoutParams(visBoxParams);
        GradientDrawable visBg = new GradientDrawable();
        visBg.setColor(Color.parseColor("#1E1932")); 
        visBg.setCornerRadius(dpToPx(16f));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) visToggleBox.setBackground(visBg);
        else visToggleBox.setBackgroundDrawable(visBg);

        ImageView visLightIcon = new ImageView(this);
        int resVisLight = getResources().getIdentifier("ic_lightning", "drawable", getPackageName());
        if(resVisLight != 0) {
            visLightIcon.setImageResource(resVisLight);
            visLightIcon.setColorFilter(Color.parseColor(accentColor));
        }
        visLightIcon.setLayoutParams(new LinearLayout.LayoutParams(dpToPx(18f), dpToPx(18f)));

        TextView visLabelText = new TextView(this);
        visLabelText.setText("Hiển thị nút");
        visLabelText.setTextColor(Color.parseColor(COLOR_TEXT));
        visLabelText.setTextSize(14);
        visLabelText.setPadding(dpToPx(12f), 0, 0, 0);
        LinearLayout.LayoutParams visLblParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1);
        visLabelText.setLayoutParams(visLblParams);

        final LinearLayout vToggleBg = new LinearLayout(this);
        vToggleBg.setLayoutParams(new LinearLayout.LayoutParams(dpToPx(52f), dpToPx(28f)));
        vToggleBg.setGravity(Gravity.CENTER_VERTICAL);

        final View vThumb = new View(this);
        LinearLayout.LayoutParams vtParams = new LinearLayout.LayoutParams(dpToPx(24f), dpToPx(24f));
        vThumb.setLayoutParams(vtParams);
        GradientDrawable vtThumbBg = new GradientDrawable();
        vtThumbBg.setShape(GradientDrawable.OVAL);
        vtThumbBg.setColor(Color.WHITE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) vThumb.setBackground(vtThumbBg);
        else vThumb.setBackgroundDrawable(vtThumbBg);
        vToggleBg.addView(vThumb);

        visToggleBox.addView(visLightIcon);
        visToggleBox.addView(visLabelText);
        visToggleBox.addView(vToggleBg);

        LinearLayout soundToggleBox = new LinearLayout(this);
        soundToggleBox.setOrientation(LinearLayout.HORIZONTAL);
        soundToggleBox.setGravity(Gravity.CENTER_VERTICAL);
        soundToggleBox.setPadding(dpToPx(16f), dpToPx(14f), dpToPx(16f), dpToPx(14f));
        LinearLayout.LayoutParams sndBoxParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        sndBoxParams.topMargin = dpToPx(12f);
        soundToggleBox.setLayoutParams(sndBoxParams);

        GradientDrawable soundBg = new GradientDrawable();
        soundBg.setColor(Color.parseColor("#1E1932"));
        soundBg.setCornerRadius(dpToPx(16f));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) soundToggleBox.setBackground(soundBg);
        else soundToggleBox.setBackgroundDrawable(soundBg);

        final LinearLayout sToggleBg = new LinearLayout(this);
        sToggleBg.setLayoutParams(new LinearLayout.LayoutParams(dpToPx(52f), dpToPx(28f)));
        sToggleBg.setGravity(Gravity.CENTER_VERTICAL);

        final View sThumb = new View(this);
        LinearLayout.LayoutParams stParams = new LinearLayout.LayoutParams(dpToPx(24f), dpToPx(24f));
        sThumb.setLayoutParams(stParams);
        GradientDrawable stThumbBg = new GradientDrawable();
        stThumbBg.setShape(GradientDrawable.OVAL);
        stThumbBg.setColor(Color.WHITE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) sThumb.setBackground(stThumbBg);
        else sThumb.setBackgroundDrawable(stThumbBg);
        sToggleBg.addView(sThumb);

        View spacerSound = new View(this);
        LinearLayout.LayoutParams spacerSoundParams = new LinearLayout.LayoutParams(0, 1, 1);
        spacerSound.setLayoutParams(spacerSoundParams);

        ImageView speakerIcon = new ImageView(this);
        int resSound = getResources().getIdentifier("ic_sound", "drawable", getPackageName());
        if (resSound != 0) {
            speakerIcon.setImageResource(resSound);
            speakerIcon.setColorFilter(Color.parseColor(accentColor));
        }
        LinearLayout.LayoutParams speakerParams = new LinearLayout.LayoutParams(dpToPx(18f), dpToPx(18f));
        speakerParams.rightMargin = dpToPx(12f);
        speakerIcon.setLayoutParams(speakerParams);

        TextView soundLabelText = new TextView(this);
        soundLabelText.setText("Âm Thanh");
        soundLabelText.setTextColor(Color.parseColor(COLOR_TEXT));
        soundLabelText.setTextSize(14);

        soundToggleBox.addView(sToggleBg);
        soundToggleBox.addView(spacerSound);
        soundToggleBox.addView(speakerIcon);
        soundToggleBox.addView(soundLabelText);

        if (featureId == 1) {
            soundToggle1 = sToggleBg; soundThumb1 = sThumb;
            visibilityToggle1 = vToggleBg; visibilityThumb1 = vThumb;
        } else if (featureId == 2) {
            soundToggle2 = sToggleBg; soundThumb2 = sThumb;
            visibilityToggle2 = vToggleBg; visibilityThumb2 = vThumb;
        } else if (featureId == 3) {
            soundToggle3 = sToggleBg; soundThumb3 = sThumb;
            visibilityToggle3 = vToggleBg; visibilityThumb3 = vThumb;
        }

        updateToggleState(sToggleBg, sThumb, getSoundForFeature(featureId), false, false, accentColor);
        updateToggleState(vToggleBg, vThumb, getVisibilityForFeature(featureId), true, false, accentColor);

        soundToggleBox.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) { toggleSound(featureId, accentColor); }
        });

        visToggleBox.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) { toggleVisibility(featureId, accentColor); }
        });

        contentContainer.addView(divider);
        contentContainer.addView(sizeSection);
        contentContainer.addView(opacitySection);
        contentContainer.addView(visToggleBox);
        contentContainer.addView(soundToggleBox);

        headerRow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (contentContainer.getVisibility() == View.VISIBLE) {
                    contentContainer.setVisibility(View.GONE);
                    if (iconDown != 0) arrowIcon.setImageResource(iconDown);
                } else {
                    contentContainer.setVisibility(View.VISIBLE);
                    if (iconUp != 0) arrowIcon.setImageResource(iconUp);
                }
            }
        });

        inner.addView(headerRow);
        inner.addView(contentContainer);
        card.addView(inner);

        return card;
    }

    private LinearLayout createModernSizeLayout(String labelText, final int featureId, String accentColor) {
        LinearLayout layout = new LinearLayout(this);
        layout.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));
        layout.setOrientation(LinearLayout.VERTICAL);

        LinearLayout labelValueContainer = new LinearLayout(this);
        labelValueContainer.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));
        labelValueContainer.setOrientation(LinearLayout.HORIZONTAL);
        labelValueContainer.setGravity(Gravity.CENTER_VERTICAL);

        TextView label = new TextView(this);
        label.setText(labelText);
        label.setTextSize(13);
        label.setTextColor(Color.parseColor(COLOR_TEXT_SEC));
        if (defaultFont != null) label.setTypeface(defaultFont);
        label.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));

        int initVal = getSizeForFeature(featureId);
        if (initVal < 0) initVal = 0;
        if (initVal > 100) initVal = 100;

        final TextView value = new TextView(this);
        value.setText(initVal + " dp");
        value.setTextSize(13);
        value.setTextColor(Color.parseColor(accentColor));
        if (defaultFont != null) value.setTypeface(defaultFont, Typeface.BOLD);
        value.setGravity(Gravity.END);
        value.setPadding(dpToPx(12f), dpToPx(4f), dpToPx(12f), dpToPx(4f));
        GradientDrawable valueBadgeBg = new GradientDrawable();
        valueBadgeBg.setColor(Color.parseColor("#1A" + accentColor.replace("#","")));
        valueBadgeBg.setCornerRadius(dpToPx(10f));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) value.setBackground(valueBadgeBg);
        else value.setBackgroundDrawable(valueBadgeBg);

        labelValueContainer.addView(label);
        labelValueContainer.addView(value);

        LinearLayout seekbarContainer = new LinearLayout(this);
        seekbarContainer.setOrientation(LinearLayout.HORIZONTAL);
        seekbarContainer.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams scParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        );
        scParams.topMargin = dpToPx(12f);
        scParams.bottomMargin = dpToPx(4f);
        seekbarContainer.setLayoutParams(scParams);

        TextView btnMinus = new TextView(this);
        btnMinus.setText("-");
        btnMinus.setTextColor(Color.WHITE);
        btnMinus.setTextSize(18);
        btnMinus.setGravity(Gravity.CENTER);
        if (defaultFont != null) btnMinus.setTypeface(defaultFont, Typeface.BOLD);
        GradientDrawable btnBg = new GradientDrawable();
        btnBg.setColor(Color.parseColor("#1E1932"));
        btnBg.setCornerRadius(dpToPx(10f)); 
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) btnMinus.setBackground(btnBg);
        else btnMinus.setBackgroundDrawable(btnBg);
        LinearLayout.LayoutParams minParams = new LinearLayout.LayoutParams(dpToPx(36f), dpToPx(36f));
        btnMinus.setLayoutParams(minParams);

        final SeekBar newSeekBar = new SeekBar(this);
        LinearLayout.LayoutParams seekbarParams = new LinearLayout.LayoutParams(0, dpToPx(32f), 1f);
        seekbarParams.leftMargin = dpToPx(8f);
        seekbarParams.rightMargin = dpToPx(8f);
        newSeekBar.setLayoutParams(seekbarParams);

        TextView btnPlus = new TextView(this);
        btnPlus.setText("+");
        btnPlus.setTextColor(Color.WHITE);
        btnPlus.setTextSize(18);
        btnPlus.setGravity(Gravity.CENTER);
        if (defaultFont != null) btnPlus.setTypeface(defaultFont, Typeface.BOLD);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) btnPlus.setBackground(btnBg);
        else btnPlus.setBackgroundDrawable(btnBg);
        LinearLayout.LayoutParams plusParams = new LinearLayout.LayoutParams(dpToPx(36f), dpToPx(36f));
        btnPlus.setLayoutParams(plusParams);

        applyModernSeekBarStyle(newSeekBar, Color.parseColor(accentColor));
        newSeekBar.setMax(100);
        newSeekBar.setProgress(initVal);

        newSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                value.setText(progress + " dp");
                setSizeForFeature(featureId, progress);
                if (fromUser) saveImpairSettings();
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) { saveImpairSettings(); }
        });

        btnMinus.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int p = newSeekBar.getProgress();
                if (p > 0) newSeekBar.setProgress(p - 1);
                saveImpairSettings();
            }
        });

        btnPlus.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int p = newSeekBar.getProgress();
                if (p < newSeekBar.getMax()) newSeekBar.setProgress(p + 1);
                saveImpairSettings();
            }
        });

        if (featureId == 1) { sizeSeekBar1 = newSeekBar; sizeValue1 = value; }
        else if (featureId == 2) { sizeSeekBar2 = newSeekBar; sizeValue2 = value; }
        else if (featureId == 3) { sizeSeekBar3 = newSeekBar; sizeValue3 = value; }

        seekbarContainer.addView(btnMinus);
        seekbarContainer.addView(newSeekBar);
        seekbarContainer.addView(btnPlus);

        layout.addView(labelValueContainer);
        layout.addView(seekbarContainer);
        return layout;
    }

    private LinearLayout createModernOpacityLayout(String labelText, final int featureId, String accentColor) {
        LinearLayout layout = new LinearLayout(this);
        layout.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));
        layout.setOrientation(LinearLayout.VERTICAL);

        LinearLayout labelValueContainer = new LinearLayout(this);
        labelValueContainer.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));
        labelValueContainer.setOrientation(LinearLayout.HORIZONTAL);
        labelValueContainer.setGravity(Gravity.CENTER_VERTICAL);

        TextView label = new TextView(this);
        label.setText(labelText);
        label.setTextSize(13);
        label.setTextColor(Color.parseColor(COLOR_TEXT_SEC));
        if (defaultFont != null) label.setTypeface(defaultFont);
        label.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));

        int currentValue = getOpacityForFeature(featureId);
        if (currentValue < 0) currentValue = 0;
        if (currentValue > 100) currentValue = 100;

        final TextView value = new TextView(this);
        value.setText(currentValue + "%");
        value.setTextSize(13);
        value.setTextColor(Color.parseColor(accentColor));
        if (defaultFont != null) value.setTypeface(defaultFont, Typeface.BOLD);
        value.setGravity(Gravity.END);
        value.setPadding(dpToPx(12f), dpToPx(4f), dpToPx(12f), dpToPx(4f));
        GradientDrawable valueBadgeBg = new GradientDrawable();
        valueBadgeBg.setColor(Color.parseColor("#1A" + accentColor.replace("#","")));
        valueBadgeBg.setCornerRadius(dpToPx(10f));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) value.setBackground(valueBadgeBg);
        else value.setBackgroundDrawable(valueBadgeBg);

        labelValueContainer.addView(label);
        labelValueContainer.addView(value);

        LinearLayout seekbarContainer = new LinearLayout(this);
        seekbarContainer.setOrientation(LinearLayout.HORIZONTAL);
        seekbarContainer.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams scParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        );
        scParams.topMargin = dpToPx(12f);
        scParams.bottomMargin = dpToPx(4f);
        seekbarContainer.setLayoutParams(scParams);

        TextView btnMinus = new TextView(this);
        btnMinus.setText("-");
        btnMinus.setTextColor(Color.WHITE);
        btnMinus.setTextSize(18);
        btnMinus.setGravity(Gravity.CENTER);
        if (defaultFont != null) btnMinus.setTypeface(defaultFont, Typeface.BOLD);
        GradientDrawable btnBg = new GradientDrawable();
        btnBg.setColor(Color.parseColor("#1E1932"));
        btnBg.setCornerRadius(dpToPx(10f));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) btnMinus.setBackground(btnBg);
        else btnMinus.setBackgroundDrawable(btnBg);
        LinearLayout.LayoutParams minParams = new LinearLayout.LayoutParams(dpToPx(36f), dpToPx(36f));
        btnMinus.setLayoutParams(minParams);

        final SeekBar newSeekBar = new SeekBar(this);
        LinearLayout.LayoutParams seekbarParams = new LinearLayout.LayoutParams(0, dpToPx(32f), 1f);
        seekbarParams.leftMargin = dpToPx(8f);
        seekbarParams.rightMargin = dpToPx(8f);
        newSeekBar.setLayoutParams(seekbarParams);

        TextView btnPlus = new TextView(this);
        btnPlus.setText("+");
        btnPlus.setTextColor(Color.WHITE);
        btnPlus.setTextSize(18);
        btnPlus.setGravity(Gravity.CENTER);
        if (defaultFont != null) btnPlus.setTypeface(defaultFont, Typeface.BOLD);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) btnPlus.setBackground(btnBg);
        else btnPlus.setBackgroundDrawable(btnBg);
        LinearLayout.LayoutParams plusParams = new LinearLayout.LayoutParams(dpToPx(36f), dpToPx(36f));
        btnPlus.setLayoutParams(plusParams);

        applyModernSeekBarStyle(newSeekBar, Color.parseColor(accentColor));
        newSeekBar.setMax(100);
        newSeekBar.setProgress(currentValue);

        newSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                value.setText(progress + "%");
                setOpacityForFeature(featureId, progress);
                if (fromUser) saveImpairSettings();
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) { saveImpairSettings(); }
        });

        btnMinus.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int p = newSeekBar.getProgress();
                if (p > 0) newSeekBar.setProgress(p - 1);
                saveImpairSettings();
            }
        });

        btnPlus.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int p = newSeekBar.getProgress();
                if (p < newSeekBar.getMax()) newSeekBar.setProgress(p + 1);
                saveImpairSettings();
            }
        });

        if (featureId == 1) { opacitySeekBar1 = newSeekBar; opacityValue1 = value; }
        else if (featureId == 2) { opacitySeekBar2 = newSeekBar; opacityValue2 = value; }
        else if (featureId == 3) { opacitySeekBar3 = newSeekBar; opacityValue3 = value; }

        seekbarContainer.addView(btnMinus);
        seekbarContainer.addView(newSeekBar);
        seekbarContainer.addView(btnPlus);

        layout.addView(labelValueContainer);
        layout.addView(seekbarContainer);
        return layout;
    }

    private void updateToggleState(LinearLayout track, View thumb, boolean isOn, boolean isVisibilityToggle, boolean animate, String accentColor) {
        int trackWidth = track.getLayoutParams().width;
        int thumbSize = thumb.getLayoutParams().width;

        GradientDrawable bg = new GradientDrawable();
        if (isOn) {
            bg.setColor(Color.parseColor(accentColor));
        } else {
            bg.setColor(Color.parseColor(COLOR_CARD_STROKE));
        }
        bg.setCornerRadius(dpToPx(20f));

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) track.setBackground(bg);
        else track.setBackgroundDrawable(bg);

        float padding = dpToPx(2f);
        final float targetX = isOn ? (trackWidth - thumbSize - (padding*2)) : padding;

        if (animate && Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR1) {
            thumb.animate().translationX(targetX).setDuration(250).setInterpolator(new AccelerateDecelerateInterpolator()).start();
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            thumb.setTranslationX(targetX);
        } else {
            LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) thumb.getLayoutParams();
            params.setMargins((int)targetX, 0, 0, 0);
            thumb.setLayoutParams(params);
            thumb.requestLayout();
        }
    }

    private void applyModernSeekBarStyle(SeekBar seekBar, int progressColor) {
        seekBar.setPadding(0, 0, 0, 0);
        seekBar.setKeyProgressIncrement(1);

        GradientDrawable backgroundTrack = new GradientDrawable();
        backgroundTrack.setShape(GradientDrawable.RECTANGLE);
        backgroundTrack.setCornerRadius(dpToPx(8f));
        backgroundTrack.setColor(Color.parseColor(COLOR_CARD_STROKE));

        GradientDrawable progressTrack = new GradientDrawable();
        progressTrack.setShape(GradientDrawable.RECTANGLE);
        progressTrack.setCornerRadius(dpToPx(8f));
        progressTrack.setColor(progressColor);

        GradientDrawable thumb = new GradientDrawable();
        thumb.setShape(GradientDrawable.RECTANGLE);
        thumb.setCornerRadius(dpToPx(5f)); 
        thumb.setColor(progressColor);
        thumb.setSize(dpToPx(18f), dpToPx(18f)); 

        ClipDrawable clipProgress = new ClipDrawable(progressTrack, Gravity.START, ClipDrawable.HORIZONTAL);
        LayerDrawable layerDrawable = new LayerDrawable(new android.graphics.drawable.Drawable[]{backgroundTrack, clipProgress});

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            layerDrawable.setLayerHeight(0, dpToPx(6f));
            layerDrawable.setLayerHeight(1, dpToPx(6f));
            layerDrawable.setLayerGravity(0, Gravity.CENTER_VERTICAL);
            layerDrawable.setLayerGravity(1, Gravity.CENTER_VERTICAL);
        }

        seekBar.setProgressDrawable(layerDrawable);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            RippleDrawable rippleThumb = new RippleDrawable(android.content.res.ColorStateList.valueOf(Color.parseColor("#33FFFFFF")), thumb, null);
            seekBar.setThumb(rippleThumb);
            seekBar.setThumbOffset(dpToPx(9f)); 
        } else {
            seekBar.setThumb(thumb);
            seekBar.setThumbOffset(dpToPx(9f));
        }

        int thumbPadding = dpToPx(9f);
        seekBar.setPadding(thumbPadding, dpToPx(8f), thumbPadding, dpToPx(8f));
        seekBar.setMinimumHeight(dpToPx(36f));
        seekBar.setSplitTrack(false);
    }

    private int getSizeForFeature(int id) {
        if (id == 1) return currentSize1;
        if (id == 2) return currentSize2;
        return currentSize3;
    }

    private void setSizeForFeature(int id, int value) {
        if (id == 1) currentSize1 = value;
        else if (id == 2) currentSize2 = value;
        else currentSize3 = value;
    }

    private int getOpacityForFeature(int id) {
        if (id == 1) return currentOpacity1;
        if (id == 2) return currentOpacity2;
        return currentOpacity3;
    }

    private void setOpacityForFeature(int id, int value) {
        if (id == 1) currentOpacity1 = value;
        else if (id == 2) currentOpacity2 = value;
        else currentOpacity3 = value;
    }

    private boolean getVisibilityForFeature(int id) {
        if (id == 1) return isVisible1On;
        if (id == 2) return isVisible2On;
        return isVisible3On;
    }

    private void toggleVisibility(int id, String accentColor) {
        if (id == 1) {
            isVisible1On = !isVisible1On;
            if (visibilityToggle1 != null && visibilityThumb1 != null) updateToggleState(visibilityToggle1, visibilityThumb1, isVisible1On, true, true, accentColor);
        } else if (id == 2) {
            isVisible2On = !isVisible2On;
            if (visibilityToggle2 != null && visibilityThumb2 != null) updateToggleState(visibilityToggle2, visibilityThumb2, isVisible2On, true, true, accentColor);
        } else if (id == 3) {
            isVisible3On = !isVisible3On;
            if (visibilityToggle3 != null && visibilityThumb3 != null) updateToggleState(visibilityToggle3, visibilityThumb3, isVisible3On, true, true, accentColor);
        }
        saveImpairSettings();
    }

    private boolean getSoundForFeature(int id) {
        if (id == 1) return isSound1On;
        if (id == 2) return isSound2On;
        return isSound3On;
    }

    private void toggleSound(int id, String accentColor) {
        if (id == 1) {
            isSound1On = !isSound1On;
            if (soundToggle1 != null && soundThumb1 != null) updateToggleState(soundToggle1, soundThumb1, isSound1On, false, true, accentColor);
        } else if (id == 2) {
            isSound2On = !isSound2On;
            if (soundToggle2 != null && soundThumb2 != null) updateToggleState(soundToggle2, soundThumb2, isSound2On, false, true, accentColor);
        } else if (id == 3) {
            isSound3On = !isSound3On;
            if (soundToggle3 != null && soundThumb3 != null) updateToggleState(soundToggle3, soundThumb3, isSound3On, false, true, accentColor);
        }
        saveImpairSettings();
    }

    private void checkOverlay() {
        waiting = true;
        if (Build.VERSION.SDK_INT >= 23 && !Settings.canDrawOverlays(this)) {
            try { startActivityForResult(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION), REQ_OVERLAY); } catch (Exception e) { checkVpn(); }
        } else checkVpn();
    }

    private void checkVpn() {
        try {
            Intent v = VpnService.prepare(this);
            if (v != null) startActivityForResult(v, REQ_VPN);
            else startEngine();
        } catch (Exception e) {
            waiting = false;
            toast("Lỗi VPN: " + e.getMessage());
        }
    }

    private void startEngine() {
        waiting = false;
        engineOn = true;
        toast("Khởi chạy thành công!");

        try {
            Intent vpnIntent = new Intent(this, MyVpnService.class);
            vpnIntent.setAction(MyVpnService.ACTION_START);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(vpnIntent);
            } else {
                startService(vpnIntent);
            }
        } catch (Exception e) { }

        try {
            Intent floatIntent = new Intent(this, FloatingService.class);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(floatIntent);
            } else {
                startService(floatIntent);
            }
            sendAppearanceUpdate();
        } catch (Exception e) { }
    }

    private void stopEngine() {
        waiting = false;
        engineOn = false;
        toast("Đã đưng thành công!");

        try {
            Intent vpnIntent = new Intent(this, MyVpnService.class);
            vpnIntent.setAction(MyVpnService.ACTION_STOP);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(vpnIntent);
            } else {
                startService(vpnIntent);
            }
        } catch (Exception e) { }

        try {
            Intent floatIntent = new Intent(this, FloatingService.class);
            stopService(floatIntent);
        } catch (Exception e) { }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        waiting = false;
        if (requestCode == REQ_OVERLAY || requestCode == REQ_VPN) {
            refreshPermissionStates();
            if (requestCode == REQ_VPN && resultCode == RESULT_OK) startEngine();
        }
    }

    private void toast(final String msg) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() { Toast.makeText(MainActivity.this, msg, Toast.LENGTH_SHORT).show(); }
        });
    }

    private int dpToPx(float dp) {
        return (int) (dp * getResources().getDisplayMetrics().density);
    }

    @Override
    public void onClick(View v) {}
}
