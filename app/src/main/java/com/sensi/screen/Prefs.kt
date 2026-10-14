package com.sensi.screen

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/** w/h/d = 0 nghĩa là "dùng giá trị mặc định chung". */
data class Profile(val w: Int, val h: Int, val d: Int)

class Prefs(ctx: Context) {
    private val sp = ctx.applicationContext.getSharedPreferences("sensi", Context.MODE_PRIVATE)

    /** Bật/tắt tự động áp dụng theo ứng dụng */
    var enabled: Boolean
        get() = sp.getBoolean("enabled", true)
        set(v) = sp.edit().putBoolean("enabled", v).apply()

    /** "auto" | "shizuku" | "root" */
    var mode: String
        get() = sp.getString("mode", "auto") ?: "auto"
        set(v) = sp.edit().putString("mode", v).apply()

    var width: Int
        get() = sp.getInt("w", 1000)
        set(v) = sp.edit().putInt("w", v).apply()

    var height: Int
        get() = sp.getInt("h", 1000)
        set(v) = sp.edit().putInt("h", v).apply()

    /** 0 = không đổi density */
    var density: Int
        get() = sp.getInt("d", 0)
        set(v) = sp.edit().putInt("d", v).apply()

    /** true = màn hình đang bị app này chỉnh (cần khôi phục) */
    var dirty: Boolean
        get() = sp.getBoolean("dirty", false)
        set(v) = sp.edit().putBoolean("dirty", v).apply()

    fun apps(): Map<String, Profile> {
        val o = JSONObject(sp.getString("apps", "{}") ?: "{}")
        val m = LinkedHashMap<String, Profile>()
        for (k in o.keys()) {
            val a = o.getJSONArray(k)
            m[k] = Profile(a.optInt(0), a.optInt(1), a.optInt(2))
        }
        return m
    }

    fun setApp(pkg: String, p: Profile?) {
        val o = JSONObject(sp.getString("apps", "{}") ?: "{}")
        if (p == null) o.remove(pkg) else o.put(pkg, JSONArray(listOf(p.w, p.h, p.d)))
        sp.edit().putString("apps", o.toString()).apply()
    }

    /** Hồ sơ cuối cùng áp dụng cho [pkg], hoặc null nếu app này không được chọn. */
    fun resolve(pkg: String): Profile? {
        val o = apps()[pkg] ?: return null
        val own = o.w > 0 && o.h > 0
        return Profile(
            if (own) o.w else width,
            if (own) o.h else height,
            if (o.d > 0) o.d else density
        )
    }
}
