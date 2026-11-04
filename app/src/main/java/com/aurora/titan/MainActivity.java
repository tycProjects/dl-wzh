package com.aurora.titan;

import android.app.Activity;
import android.os.Bundle;
import android.os.Build;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.content.Intent;
import android.provider.Settings;
import android.net.Uri;
import android.view.Gravity;
import android.view.View;
import android.widget.*;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

public class MainActivity extends Activity {
    private final int bg = Color.rgb(8, 12, 22);
    private final int panel = Color.rgb(19, 27, 44);
    private final int mint = Color.rgb(83, 226, 184);
    private final int white = Color.rgb(245, 248, 255);
    private final int muted = Color.rgb(155, 169, 194);
    private TextView status, result;
    private SeekBar windowBar, transitionBar, animatorBar;
    private TextView windowValue, transitionValue, animatorValue;
    private final float[] scales = new float[]{0f, .25f, .5f, .75f, 1f, 1.5f, 2f, 3f, 5f};
    private final String[] scaleLabels = new String[]{"0×", "0.25×", "0.5×", "0.75×", "1×", "1.5×", "2×", "3×", "5×"};

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(bg);
        getWindow().setNavigationBarColor(bg);
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(bg);
        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setPadding(dp(18), dp(20), dp(18), dp(28));
        page.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        scroll.addView(page);

        TextView brand = label("✦  A U R O R A   T I T A N", 17, mint, true);
        brand.setLetterSpacing(.07f);
        page.addView(brand);
        TextView tagline = label("لوحة تحكم متقدمة لجهازك • Root Toolkit", 12, muted, false);
        tagline.setPadding(0, dp(7), 0, dp(18));
        page.addView(tagline);

        LinearLayout hero = cardBox();
        hero.setPadding(dp(20), dp(22), dp(20), dp(22));
        hero.setBackground(gradient());
        TextView icon = label("⌁", 42, mint, true); icon.setGravity(Gravity.CENTER);
        hero.addView(icon, new LinearLayout.LayoutParams(-1, dp(50)));
        TextView title = label("قوّة تحت سيطرتك", 25, white, true); title.setGravity(Gravity.CENTER);
        title.setPadding(0, dp(5), 0, dp(5)); hero.addView(title);
        TextView sub = label("إعدادات سريعة، معلومات النظام، وأدوات روت بحذر.", 13, muted, false);
        sub.setGravity(Gravity.CENTER); hero.addView(sub);
        page.addView(hero);
        page.addView(gap(18));

        section(page, "حالة الروت");
        status = label("جارٍ فحص صلاحيات الروت…", 14, mint, true);
        status.setPadding(dp(14), dp(15), dp(14), dp(15));
        status.setBackground(round(panel, 16, Color.rgb(43, 57, 78), dp(1)));
        page.addView(status);
        page.addView(gap(10));
        addButton(page, "فحص الروت الآن", v -> new Thread(() -> {
            String out = runSu("id; echo ROOT_CHECK_DONE");
            boolean ok = out != null && out.contains("uid=0");
            runOnUiThread(() -> {
                status.setText(ok ? "✓ صلاحيات root فعّالة" : "⚠ لم يتم تأكيد الروت — افحص Magisk");
                status.setTextColor(ok ? mint : Color.rgb(255, 190, 100));
                showResult(out == null ? "لا يوجد ناتج من su." : out);
            });
        }).start());

        page.addView(gap(18));
        section(page, "سرعة الحركة والانتقالات");
        TextView note = label("غيّر السرعة مباشرة. 1× هي القيمة الافتراضية، و0× يوقف الحركة. التغيير يطبق عبر إعدادات النظام باستخدام الروت.", 12, muted, false);
        note.setLineSpacing(dp(3), 1f); page.addView(note); page.addView(gap(8));

        windowValue = label("حركة النوافذ: قراءة الإعداد…", 14, white, true); page.addView(windowValue);
        windowBar = seek(); page.addView(windowBar);
        transitionValue = label("حركة الانتقال: قراءة الإعداد…", 14, white, true); page.addView(transitionValue);
        transitionBar = seek(); page.addView(transitionBar);
        animatorValue = label("مدة حركة العناصر: قراءة الإعداد…", 14, white, true); page.addView(animatorValue);
        animatorBar = seek(); page.addView(animatorBar);
        addButton(page, "تطبيق سرعات الحركة", v -> applyScales());
        addButton(page, "إعادة السرعات الافتراضية (1×)", v -> {
            windowBar.setProgress(4); transitionBar.setProgress(4); animatorBar.setProgress(4); applyScales();
        });

        page.addView(gap(18));
        section(page, "اختصارات إعدادات أندرويد");
        addButton(page, "خيارات المطوّر", v -> openSettings(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS));
        addButton(page, "الإعدادات الرئيسية", v -> openSettings(Settings.ACTION_SETTINGS));
        addButton(page, "الشاشة والسطوع", v -> openSettings(Settings.ACTION_DISPLAY_SETTINGS));
        addButton(page, "البطارية", v -> openSettings(Settings.ACTION_BATTERY_SAVER_SETTINGS));
        addButton(page, "معلومات التطبيق", v -> openAppDetails());

        page.addView(gap(18));
        section(page, "معلومات الجهاز");
        addButton(page, "جمع معلومات النظام", v -> new Thread(() -> {
            String cmd = "echo '=== DEVICE ==='; getprop ro.product.manufacturer; getprop ro.product.model; " +
                "echo '=== ANDROID ==='; getprop ro.build.version.release; getprop ro.build.version.sdk; " +
                "echo '=== KERNEL ==='; uname -a; echo '=== UPTIME ==='; cat /proc/uptime; echo '=== ROOT ==='; id";
            String out = runSu(cmd);
            runOnUiThread(() -> showResult(out == null ? "تعذّر قراءة معلومات النظام." : out));
        }).start());

        page.addView(gap(18));
        section(page, "أدوات ملفات آمنة");
        TextView fileNote = label("إنشاء تقرير نصي داخل مجلد التطبيق الخاص. لا يعدّل ملفات النظام ولا يحذف بياناتك.", 12, muted, false);
        page.addView(fileNote);
        addButton(page, "إنشاء تقرير النظام", v -> createReport());
        addButton(page, "عرض نتيجة الفحص", v -> showResult("آخر نتيجة تظهر في الصندوق أدناه. استخدم أزرار الفحص لجمع بيانات جديدة."));

        page.addView(gap(14));
        result = label("نتائج الأوامر ستظهر هنا…", 12, muted, false);
        result.setTextIsSelectable(true);
        result.setGravity(Gravity.TOP | Gravity.RIGHT);
        result.setPadding(dp(14), dp(14), dp(14), dp(14));
        result.setMinHeight(dp(130));
        result.setBackground(round(panel, 16, Color.rgb(43, 57, 78), dp(1)));
        page.addView(result);
        page.addView(gap(14));
        TextView warning = label("سلامة الجهاز: تغييرات الرسوم المتحركة قابلة للعكس. اختصارات الإعدادات تفتح صفحات النظام فقط. لا توجد أوامر تنظيف قسري أو تعديل ملفات النظام تلقائيًا. صلاحيات الروت تمنح وصولًا واسعًا؛ لا تضف أوامر لا تفهمها.", 12, muted, false);
        warning.setLineSpacing(dp(3), 1f); page.addView(warning);
        TextView footer = label("AURORA TITAN • صنع للهاتف", 11, muted, false);
        footer.setGravity(Gravity.CENTER); footer.setPadding(0, dp(20), 0, 0); page.addView(footer);

        setContentView(scroll);
        setupScaleBar(windowBar, windowValue, "حركة النوافذ");
        setupScaleBar(transitionBar, transitionValue, "حركة الانتقال");
        setupScaleBar(animatorBar, animatorValue, "مدة حركة العناصر");
        new Thread(this::loadScales).start();
        new Thread(this::checkRootOnStart).start();
    }

    private void checkRootOnStart() {
        String out = runSu("id");
        boolean ok = out != null && out.contains("uid=0");
        runOnUiThread(() -> {
            status.setText(ok ? "✓ الروت فعّال — Magisk منح التطبيق الصلاحية" : "⚠ لم يتم تأكيد الروت");
            status.setTextColor(ok ? mint : Color.rgb(255, 190, 100));
        });
    }

    private void setupScaleBar(SeekBar bar, TextView label, String name) {
        bar.setMax(scales.length - 1);
        bar.setProgress(4);
        bar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            public void onProgressChanged(SeekBar b, int p, boolean user) {
                label.setText(name + ": " + scaleLabels[p]);
            }
            public void onStartTrackingTouch(SeekBar b) {}
            public void onStopTrackingTouch(SeekBar b) {}
        });
    }

    private void loadScales() {
        String out = runSu("settings get global window_animation_scale; settings get global transition_animation_scale; settings get global animator_duration_scale");
        if (out == null) return;
        String[] lines = out.trim().split("\\n");
        if (lines.length < 3) return;
        int a = nearest(parse(lines[0])), b = nearest(parse(lines[1])), c = nearest(parse(lines[2]));
        runOnUiThread(() -> {
            windowBar.setProgress(a); transitionBar.setProgress(b); animatorBar.setProgress(c);
        });
    }

    private float parse(String s) {
        try { return Float.parseFloat(s.trim()); } catch (Exception e) { return 1f; }
    }
    private int nearest(float value) {
        int best = 4; float distance = Float.MAX_VALUE;
        for (int i = 0; i < scales.length; i++) {
            float d = Math.abs(scales[i] - value);
            if (d < distance) { distance = d; best = i; }
        }
        return best;
    }

    private void applyScales() {
        float a = scales[windowBar.getProgress()];
        float b = scales[transitionBar.getProgress()];
        float c = scales[animatorBar.getProgress()];
        String cmd = String.format(Locale.US,
            "settings put global window_animation_scale %s; settings put global transition_animation_scale %s; settings put global animator_duration_scale %s; " +
            "echo 'Applied animation scales'; settings get global window_animation_scale; settings get global transition_animation_scale; settings get global animator_duration_scale; id",
            Float.toString(a), Float.toString(b), Float.toString(c));
        new Thread(() -> {
            String out = runSu(cmd);
            runOnUiThread(() -> showResult(out == null ? "فشل تنفيذ الأمر. تحقق من Magisk." : out));
        }).start();
    }

    private void createReport() {
        new Thread(() -> {
            String out = runSu("echo 'AURORA TITAN SYSTEM REPORT'; date; getprop ro.product.manufacturer; getprop ro.product.model; getprop ro.build.version.release; getprop ro.build.version.sdk; uname -a; id; settings get global window_animation_scale; settings get global transition_animation_scale; settings get global animator_duration_scale");
            if (out == null) out = "تعذّر إنشاء التقرير.";
            final String report = out;
            runOnUiThread(() -> {
                try {
                    File dir = getExternalFilesDir(null);
                    if (dir == null) dir = getFilesDir();
                    File file = new File(dir, "aurora_system_report.txt");
                    FileOutputStream fos = new FileOutputStream(file);
                    fos.write(report.getBytes(StandardCharsets.UTF_8));
                    fos.close();
                    showResult("تم إنشاء التقرير:\n" + file.getAbsolutePath() + "\n\n" + report);
                    Toast.makeText(this, "تم إنشاء تقرير النظام", Toast.LENGTH_SHORT).show();
                } catch (Exception e) { showResult("تعذّر حفظ التقرير: " + e.getMessage()); }
            });
        }).start();
    }

    private String runSu(String command) {
        Process p = null;
        try {
            p = new ProcessBuilder("su", "-c", command).redirectErrorStream(true).start();
            BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream()));
            StringBuilder s = new StringBuilder(); String line;
            while ((line = r.readLine()) != null) s.append(line).append('\n');
            int code = p.waitFor(); s.append("\nExit code: ").append(code);
            return s.toString();
        } catch (Exception e) { return "خطأ: " + e.getClass().getSimpleName() + ": " + e.getMessage(); }
        finally { if (p != null) p.destroy(); }
    }

    private void openSettings(String action) {
        try { startActivity(new Intent(action)); }
        catch (Exception e) { Toast.makeText(this, "هذه الصفحة غير متاحة على هذا الإصدار.", Toast.LENGTH_LONG).show(); }
    }
    private void openAppDetails() {
        try {
            Intent i = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            i.setData(Uri.parse("package:" + getPackageName())); startActivity(i);
        } catch (Exception e) { openSettings(Settings.ACTION_SETTINGS); }
    }
    private void showResult(String s) { result.setText(s); }
    private void section(LinearLayout page, String title) {
        TextView t = label(title, 19, white, true);
        t.setPadding(0, 0, 0, dp(9)); page.addView(t);
    }
    private LinearLayout cardBox() { LinearLayout l = new LinearLayout(this); l.setOrientation(LinearLayout.VERTICAL); return l; }
    private SeekBar seek() {
        SeekBar s = new SeekBar(this);
        s.setMax(8); s.setProgress(4);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, dp(42));
        lp.bottomMargin = dp(7); s.setLayoutParams(lp);
        return s;
    }
    private void addButton(LinearLayout page, String text, View.OnClickListener click) {
        Button b = new Button(this);
        b.setText(text); b.setTextSize(14); b.setTextColor(Color.rgb(9, 28, 30));
        b.setAllCaps(false); b.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        b.setBackground(round(mint, 15, mint, 0));
        b.setPadding(dp(8), dp(7), dp(8), dp(7));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.topMargin = dp(6); lp.bottomMargin = dp(4);
        page.addView(b, lp); b.setOnClickListener(click);
    }
    private TextView label(String text, float size, int color, boolean bold) {
        TextView t = new TextView(this); t.setText(text); t.setTextSize(size); t.setTextColor(color);
        t.setTypeface(Typeface.create("sans-serif", bold ? Typeface.BOLD : Typeface.NORMAL));
        t.setGravity(Gravity.CENTER_VERTICAL); return t;
    }
    private View gap(int height) {
        View v = new View(this); v.setLayoutParams(new LinearLayout.LayoutParams(1, dp(height))); return v;
    }
    private GradientDrawable round(int color, int radius, int stroke, int width) {
        GradientDrawable d = new GradientDrawable(); d.setColor(color); d.setCornerRadius(dp(radius));
        if (width > 0) d.setStroke(width, stroke); return d;
    }
    private GradientDrawable gradient() {
        GradientDrawable d = new GradientDrawable(GradientDrawable.Orientation.TL_BR,
            new int[]{Color.rgb(28, 78, 80), Color.rgb(27, 41, 70), Color.rgb(24, 29, 48)});
        d.setCornerRadius(dp(26)); return d;
    }
    private int dp(int n) { return (int)(n * getResources().getDisplayMetrics().density + .5f); }
}
