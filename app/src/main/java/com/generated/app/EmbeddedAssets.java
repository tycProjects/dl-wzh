package com.generated.app;

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
            "PCFkb2N0eXBlIGh0bWw+PGh0bWwgbGFuZz0iYXIiPjxoZWFkPjxtZXRhIGNoYXJzZXQ9IlVURi04Ij48bWV0YSBuYW1lPSJ2aWV3cG9ydCIgY29udGVudD0id2lkdGg9ZGV2aWNlLXdpZHRoLGluaXRpYWwtc2NhbGU9MSI+PHRpdGxlPllvdUdsaXNoIC0g2KrYudmE2YUg2KfZhNmG2LfZgiDYp9mE2KXZhtis2YTZitiy2Yo8L3RpdGxlPjxzdHlsZT5ib2R5e21hcmdpbjowO3BhZGRpbmc6MTVweDtmb250LWZhbWlseTpBcmlhbCxzYW5zLXNlcmlmO2JhY2tncm91bmQtY29sb3I6I2ZmZjt0ZXh0LWFsaWduOmNlbnRlcn1oMntjb2xvcjojMjIyfXB7Y29sb3I6IzU1NX0uY29udGFpbmVye3dpZHRoOjEwMCU7bWFyZ2luOjIwcHggYXV0b31pZnJhbWV7d2lkdGg6MTAwJTtoZWlnaHQ6ODB2aDtib3JkZXI6bm9uZX08L3N0eWxlPjwvaGVhZD48Ym9keT48aDI+WW91R2xpc2g8L2gyPjxwPtiq2LnZhNmFINmG2LfZgiDYp9mE2YPZhNmF2KfYqiDYp9mE2KXZhtis2YTZitiy2YrYqSDZhdmGINin2YTZgdmK2K/ZitmI2YfYp9iqPC9wPjxkaXYgY2xhc3M9ImNvbnRhaW5lciI+PGEgaWQ9InlnLXdpZGdldC0wIiBjbGFzcz0ieW91Z2xpc2gtd2lkZ2V0IiBkYXRhLXF1ZXJ5PSJncmVhdCBwb3dlciIgZGF0YS1sYW5nPSJlbmdsaXNoIiBkYXRhLWNvbXBvbmVudHM9Ijg0MTUiIGRhdGEtYmtnLWNvbG9yPSJ0aGVtZV9saWdodCIgcmVsPSJub2ZvbGxvdyIgaHJlZj0iaHR0cHM6Ly95b3VnbGlzaC5jb20iPlZpc2l0IFlvdUdsaXNoLmNvbTwvYT48L2Rpdj48c2NyaXB0IGFzeW5jIHNyYz0iaHR0cHM6Ly95b3VnbGlzaC5jb20vcHVibGljL2VtYi93aWRnZXQuanMiIGNoYXJzZXQ9InV0Zi04Ij48L3NjcmlwdD48L2JvZHk+PC9odG1sPg==");

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
