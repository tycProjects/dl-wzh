package com.example.chatzipai;

import android.content.SharedPreferences;

/** Lưu cấu hình API theo từng nhà cung cấp (URL, key, model). */
final class Config {
    static final String CUSTOM = "Tùy chỉnh";
    static final String[] NAMES  = {"Groq", "DeepSeek", "OpenAI", "OpenRouter", CUSTOM};
    static final String[] URLS   = {
        "https://api.groq.com/openai/v1",
        "https://api.deepseek.com/v1",
        "https://api.openai.com/v1",
        "https://openrouter.ai/api/v1",
        ""
    };
    static final String[] MODELS = {
        "", "deepseek-chat", "gpt-4o-mini", "openai/gpt-4o-mini", ""
    };

    private final SharedPreferences p;

    Config(SharedPreferences p) { this.p = p; }

    static int indexOf(String name) {
        for (int i = 0; i < NAMES.length; i++) if (NAMES[i].equals(name)) return i;
        return 0;
    }

    String provider() { return p.getString("provider", NAMES[0]); }
    void setProvider(String v) { p.edit().putString("provider", v).apply(); }

    String urlOf(String prov) { return p.getString("url_" + prov, URLS[indexOf(prov)]); }
    String keyOf(String prov) {
        String k = p.getString("key_" + prov, null);
        if (k == null && prov.equals(NAMES[0])) k = p.getString("key", "");   // key cũ của bản trước
        return k == null ? "" : k;
    }
    String modelOf(String prov) { return p.getString("model_" + prov, MODELS[indexOf(prov)]); }

    String baseUrl() { return urlOf(provider()); }
    String apiKey()  { return keyOf(provider()); }
    String model()   { return modelOf(provider()); }

    void setModel(String m) { p.edit().putString("model_" + provider(), m).apply(); }

    void save(String prov, String url, String key, String model) {
        p.edit().putString("url_" + prov, url)
                .putString("key_" + prov, key)
                .putString("model_" + prov, model).apply();
    }


    String centralProvider() { return p.getString("central_provider", provider()); }
    void setCentralProvider(String v) { p.edit().putString("central_provider", v).apply(); }
    String centralUrl() { return p.getString("central_url", urlOf(centralProvider())); }
    String centralKey() { return p.getString("central_key", keyOf(centralProvider())); }
    String centralModel() { return p.getString("central_model", modelOf(centralProvider())); }
    void saveCentral(String prov, String url, String key, String model) {
        p.edit().putString("central_provider", prov).putString("central_url", url)
                .putString("central_key", key).putString("central_model", model).apply();
    }

    String system() { return p.getString("system", ""); }
    void setSystem(String s) { p.edit().putString("system", s).apply(); }

    int tpm() { return p.getInt("tpm", 0); }
    void setTpm(int v) { p.edit().putInt("tpm", v).apply(); }

    String chat() { return p.getString("chat", "[]"); }
    void setChat(String s) { p.edit().putString("chat", s).apply(); }
}
