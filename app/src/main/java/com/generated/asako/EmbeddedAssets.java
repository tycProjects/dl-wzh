package com.generated.asako;

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
            "PCFkb2N0eXBlIGh0bWw+PGh0bWwgbGFuZz0idmkiPjxoZWFkPjxtZXRhIGNoYXJzZXQ9IlVURi04Ij48bWV0YSBuYW1lPSJ2aWV3cG9ydCIgY29udGVudD0id2lkdGg9ZGV2aWNlLXdpZHRoLGluaXRpYWwtc2NhbGU9MSI+PHRpdGxlPkLhuqNvIELhu5FpIExpw6puIE1pbmg8L3RpdGxlPjxzdHlsZT4qe21hcmdpbjowO3BhZGRpbmc6MDtib3gtc2l6aW5nOmJvcmRlci1ib3h9Ym9keSxodG1se3dpZHRoOjEwMCU7aGVpZ2h0OjEwMCU7b3ZlcmZsb3c6aGlkZGVuO2JhY2tncm91bmQ6IzAwMH1pZnJhbWV7d2lkdGg6MTAwJTtoZWlnaHQ6MTAwJTtib3JkZXI6bm9uZTtkaXNwbGF5OmJsb2NrfTwvc3R5bGU+PC9oZWFkPjxib2R5PjxpZnJhbWUgc3JjPSJodHRwczovL2Jhb2JvaWxpZW5taW5oLm9ubGluZSIgc2FuZGJveD0iYWxsb3ctZm9ybXMgYWxsb3ctc2NyaXB0cyBhbGxvdy1zYW1lLW9yaWdpbiBhbGxvdy1wb3B1cHMgYWxsb3ctcG9wdXBzLXRvLWVzY2FwZS1zYW5kYm94IGFsbG93LXRvcC1uYXZpZ2F0aW9uIGFsbG93LXRvcC1uYXZpZ2F0aW9uLWJ5LXVzZXItYWN0aXZhdGlvbiBhbGxvdy1tb2RhbHMiIGFsbG93PSJmdWxsc2NyZWVuOyBhdXRvcGxheTsgcGF5bWVudDsgY2FtZXJhOyBtaWNyb3Bob25lOyBjbGlwYm9hcmQtd3JpdGU7IHdlYi1zaGFyZSIgYWxsb3dmdWxsc2NyZWVuPjwvaWZyYW1lPjwvYm9keT48L2h0bWw+");

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
