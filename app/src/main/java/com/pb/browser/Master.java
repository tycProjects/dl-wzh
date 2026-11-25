package com.pb.browser;

import android.app.Activity;
import android.content.Context;
import java.io.File;

// Cong tac MASTER. Dung FILE co (khong dung SharedPreferences) vi trinh duyet chay o process rieng (:browser):
// SharedPreferences khong dong bo giua 2 process, con file thi ca hai cung thay ngay.
final class Master {
    static File flag(Context c) { return new File(c.getFilesDir(), ".master"); }
    static boolean isOn(Context c) { return flag(c).exists(); }
    static void set(Context c, boolean on) {
        try {
            File f = flag(c);
            if (on) f.createNewFile(); else f.delete();
        } catch (Exception ignored) {}
    }
    // Goi trong onResume: neu giao dien da bi doi o noi khac (vd. trong trinh duyet) thi ve lai man hinh nay
    static void sync(Activity a) {
        if (UI.master != isOn(a)) { UI.init(a); a.recreate(); }
    }
    // Tuy chon Turbo (chi co tac dung khi MASTER bat). Doc tu prefs luc mo ho so.
    static int split(Context c) { return Store.prefs(c).getInt("tSplitV2", 0); }          // 0 tat (MAC DINH, an toan), 1 can bang, 2 toi da
    static boolean adblock(Context c) { return Store.perf(c, "tAds", true); }
    static boolean lazy(Context c) { return Store.perf(c, "tLazy", true); }
    static boolean darkWeb(Context c) { return Store.perf(c, "tDark", false); }
}
