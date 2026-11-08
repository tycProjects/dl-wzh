package com.generated.prantoxo;

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
            "PCFkb2N0eXBlIGh0bWw+PGh0bWwgbGFuZz0iZW4iPjxoZWFkPjxtZXRhIGNoYXJzZXQ9IlVURi04Ij48bWV0YSBuYW1lPSJ2aWV3cG9ydCIgY29udGVudD0id2lkdGg9ZGV2aWNlLXdpZHRoLGluaXRpYWwtc2NhbGU9MSI+PHRpdGxlPlByYW50b1hPPC90aXRsZT48c3R5bGU+Ym9keXttYXJnaW46MDtoZWlnaHQ6MTAwdmg7ZGlzcGxheTpmbGV4O2p1c3RpZnktY29udGVudDpjZW50ZXI7YWxpZ24taXRlbXM6Y2VudGVyO2JhY2tncm91bmQ6IzAwMDtjb2xvcjojZmZmO2ZvbnQtZmFtaWx5OkFyaWFsLHNhbnMtc2VyaWZ9aDF7Zm9udC1zaXplOjQycHg7Zm9udC13ZWlnaHQ6NzAwO2xldHRlci1zcGFjaW5nOjNweH08L3N0eWxlPjwvaGVhZD48Ym9keT48aDE+UHJhbnRvWE88L2gxPjwvYm9keT48L2h0bWw+");

    private static final Map<String, String> MAP = new HashMap<>();
    static {
        MAP.put("index.html", F0);
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
