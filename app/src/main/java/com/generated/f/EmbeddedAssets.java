package com.generated.f;

import java.util.HashMap;
import java.util.Map;

// Generated at build time from the wrapped site's HTML/CSS/JS. Each
// file's bytes are stored base64-encoded as a compiled string constant
// (part of classes.dex), not as a loose file under assets/.
// MainActivity's shouldInterceptRequest override (see EMBED_HOST) decodes
// these on demand to feed the WebView, in place of the OS resolving
// file:///android_asset/ requests against a real assets/ folder.
public final class EmbeddedAssets {
    // Joins base64 chunks at class-init time. Deliberately NOT string
    // concatenation on the call site (see the comment above each F<n>
    // field) -- a method call is not a compile-time constant expression,
    // so javac can't fold these chunks back into one oversized constant.
    private static String join(String... parts) {
        StringBuilder sb = new StringBuilder();
        for (String p : parts) sb.append(p);
        return sb.toString();
    }

    private static final String F0 = join(
            "PD94bWwgdmVyc2lvbj0iMS4wIiBlbmNvZGluZz0idXRmLTgiPz4KPG1hbmlmZXN0IHhtbG5zOmFuZHJvaWQ9Imh0dHA6Ly9zY2hlbWFzLmFuZHJvaWQuY29tL2Fway9yZXMvYW5kcm9pZCIKICAgIHBhY2thZ2U9ImNvbS5leGFtcGxlLmYiPgoKICAgIDxhcHBsaWNhdGlvbiBhbmRyb2lkOmxhYmVsPSJAc3RyaW5nL2FwcF9uYW1lIiAvPgoKPC9tYW5pZmVzdD4K");

    private static final String F1 = join(
            );

    private static final String F2 = join(
            "PD94bWwgdmVyc2lvbj0iMS4wIiBlbmNvZGluZz0idXRmLTgiPz4KPHJlc291cmNlcz4KICAgIDxzdHJpbmcgbmFtZT0iYXBwX25hbWUiPmY8L3N0cmluZz4KPC9yZXNvdXJjZXM+Cg==");

    private static final Map<String, String> MAP = new HashMap<>();
    static {
        MAP.put("AndroidManifest.xml", F0);
        MAP.put("ha.html", F1);
        MAP.put("res/values/strings.xml", F2);
    }

    // rel: path as it would have appeared under assets/, e.g. "index.html"
    // or "css/style.css". Returns null if nothing was embedded at that path.
    public static byte[] get(String rel) {
        String b64 = MAP.get(rel);
        if (b64 == null) return null;
        byte[] raw = android.util.Base64.decode(b64, android.util.Base64.DEFAULT);
        return raw;
    }

    private EmbeddedAssets() {}
}
