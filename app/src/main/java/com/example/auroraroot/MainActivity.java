package com.example.auroraroot;

import android.app.Activity;
import android.os.Bundle;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import java.io.BufferedReader;
import java.io.InputStreamReader;

public class MainActivity extends Activity {
    private final int bg = Color.rgb(9, 13, 24);
    private final int panel = Color.rgb(20, 27, 43);
    private final int mint = Color.rgb(83, 226, 184);
    private final int white = Color.rgb(244, 247, 255);
    private final int muted = Color.rgb(158, 170, 192);
    private TextView rootStatus, output;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(bg);
        getWindow().setNavigationBarColor(bg);

        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(bg);
        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setPadding(dp(20), dp(22), dp(20), dp(28));
        page.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        scroll.addView(page);

        TextView brand = label("✦  A U R O R A   ROOT", 16, mint, true);
        brand.setLetterSpacing(.08f);
        page.addView(brand);
        page.addView(space(20));

        LinearLayout hero = box();
        hero.setOrientation(LinearLayout.VERTICAL);
        hero.setPadding(dp(22), dp(24), dp(22), dp(24));
        hero.setBackground(gradient());
        TextView symbol = label("✧", 42, mint, true);
        symbol.setGravity(Gravity.CENTER);
        hero.addView(symbol, new LinearLayout.LayoutParams(-1, dp(56)));
        TextView title = label("تحكّم أذكى، بأمان", 27, white, true);
        title.setGravity(Gravity.CENTER);
        title.setPadding(0, dp(8), 0, dp(8));
        hero.addView(title);
        TextView subtitle = label("لوحة تجريبية لفحص صلاحيات الروت ومعلومات النظام على جهازك.", 14, muted, false);
        subtitle.setGravity(Gravity.CENTER);
        subtitle.setLineSpacing(dp(4), 1f);
        hero.addView(subtitle);
        page.addView(hero);
        page.addView(space(18));

        TextView rootHeading = label("حالة صلاحيات الروت", 20, white, true);
        page.addView(rootHeading);
        page.addView(space(8));

        rootStatus = label("جارٍ طلب صلاحيات الروت…", 15, mint, true);
        rootStatus.setPadding(dp(16), dp(17), dp(16), dp(17));
        rootStatus.setBackground(round(panel, 18, Color.rgb(41, 54, 75), dp(1)));
        page.addView(rootStatus);
        page.addView(space(12));

        Button check = button("إعادة فحص الروت");
        check.setOnClickListener(v -> new Thread(this::checkRoot).start());
        page.addView(check);
        page.addView(space(10));

        Button info = button("عرض معلومات النظام");
        info.setOnClickListener(v -> new Thread(this::showSystemInfo).start());
        page.addView(info);
        page.addView(space(18));

        TextView outputTitle = label("النتيجة", 19, white, true);
        page.addView(outputTitle);
        page.addView(space(8));
        output = label("سيظهر هنا ناتج الفحص. لا ينفّذ التطبيق أوامر تعديل أو حذف.", 13, muted, false);
        output.setTextIsSelectable(true);
        output.setPadding(dp(16), dp(16), dp(16), dp(16));
        output.setMinHeight(dp(100));
        output.setGravity(Gravity.TOP | Gravity.RIGHT);
        output.setBackground(round(panel, 18, Color.rgb(41, 54, 75), dp(1)));
        page.addView(output);

        page.addView(space(18));
        TextView safety = label("تنبيه: سيظهر طلب Magisk عند محاولة منح الروت. امنح الصلاحية لهذا التطبيق فقط إذا كنت تثق به. هذه النسخة تستخدم الروت للقراءة والفحص فقط.", 12, muted, false);
        safety.setLineSpacing(dp(3), 1f);
        page.addView(safety);

        setContentView(scroll);
        // Trigger the Android su request on first launch. Magisk/Superuser manages the prompt.
        new Thread(this::checkRoot).start();
    }

    private void checkRoot() {
        String result = runSu("id");
        boolean granted = result != null && result.contains("uid=0");
        runOnUiThread(() -> {
            if (granted) {
                rootStatus.setText("✓ الروت متاح — تم منح الصلاحية");
                rootStatus.setTextColor(mint);
                output.setText("نجح اختبار الروت:\n" + result.trim());
            } else {
                rootStatus.setText("⚠ لم يتم منح الروت أو لم يُعثر على su");
                rootStatus.setTextColor(Color.rgb(255, 190, 100));
                output.setText("تعذّر تأكيد صلاحيات الروت.\n\nقد يكون السبب:\n• رفض طلب Magisk.\n• عدم تثبيت/تفعيل Magisk.\n• عدم دعم بيئة البناء لاستدعاء su.\n\nالناتج:\n" + (result == null ? "لا يوجد ناتج." : result));
            }
        });
    }

    private void showSystemInfo() {
        String result = runSu("echo '--- ANDROID RELEASE ---'; getprop ro.build.version.release; echo '--- DEVICE ---'; getprop ro.product.manufacturer; getprop ro.product.model; echo '--- SDK ---'; getprop ro.build.version.sdk; echo '--- UID ---'; id");
        runOnUiThread(() -> output.setText(result == null ? "فشل تشغيل أمر su. افحص حالة الروت أولاً." : result.trim()));
    }

    private String runSu(String command) {
        Process process = null;
        try {
            process = new ProcessBuilder("su", "-c", command).redirectErrorStream(true).start();
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            StringBuilder out = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) out.append(line).append('\n');
            int exit = process.waitFor();
            out.append("\nExit code: ").append(exit);
            return out.toString();
        } catch (Exception e) {
            return "خطأ: " + e.getClass().getSimpleName() + ": " + e.getMessage();
        } finally {
            if (process != null) process.destroy();
        }
    }

    private LinearLayout box() {
        LinearLayout l = new LinearLayout(this);
        l.setOrientation(LinearLayout.VERTICAL);
        return l;
    }
    private TextView label(String s, float size, int color, boolean bold) {
        TextView t = new TextView(this);
        t.setText(s); t.setTextSize(size); t.setTextColor(color);
        t.setTypeface(Typeface.create("sans-serif", bold ? Typeface.BOLD : Typeface.NORMAL));
        t.setGravity(Gravity.CENTER_VERTICAL);
        return t;
    }
    private Button button(String s) {
        Button b = new Button(this);
        b.setText(s); b.setTextSize(15); b.setTextColor(Color.rgb(9, 28, 30));
        b.setAllCaps(false);
        b.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        b.setBackground(round(mint, 16, mint, 0));
        b.setPadding(dp(10), dp(8), dp(10), dp(8));
        return b;
    }
    private View space(int h) {
        View v = new View(this);
        v.setLayoutParams(new LinearLayout.LayoutParams(1, dp(h)));
        return v;
    }
    private GradientDrawable round(int color, int radius, int stroke, int width) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color); d.setCornerRadius(dp(radius));
        if (width > 0) d.setStroke(width, stroke);
        return d;
    }
    private GradientDrawable gradient() {
        GradientDrawable d = new GradientDrawable(GradientDrawable.Orientation.TL_BR,
            new int[]{Color.rgb(31, 75, 82), Color.rgb(28, 39, 67), Color.rgb(24, 29, 48)});
        d.setCornerRadius(dp(28));
        return d;
    }
    private int dp(int n) {
        return (int)(n * getResources().getDisplayMetrics().density + .5f);
    }
}
