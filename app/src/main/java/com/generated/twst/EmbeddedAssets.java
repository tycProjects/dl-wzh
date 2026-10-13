package com.generated.twst;

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
            "PCFkb2N0eXBlIGh0bWw+PGh0bWwgbGFuZz0iZW4iPjxoZWFkPjxtZXRhIGNoYXJzZXQ9InV0Zi04Ij48bWV0YSBuYW1lPSJ2aWV3cG9ydCIgY29udGVudD0id2lkdGg9ZGV2aWNlLXdpZHRoLGluaXRpYWwtc2NhbGU9MSx2aWV3cG9ydC1maXQ9Y292ZXIiPjx0aXRsZT5Cb3R0b20gRmxvYXRpbmcgQ2Fwc3VsZTwvdGl0bGU+PHN0eWxlPip7Ym94LXNpemluZzpib3JkZXItYm94fWJvZHksaHRtbHttYXJnaW46MDt3aWR0aDoxMDAlO2hlaWdodDoxMDAlO21pbi1oZWlnaHQ6MTAwJTtvdmVyZmxvdzpoaWRkZW47YmFja2dyb3VuZDojMDAwO292ZXJzY3JvbGwtYmVoYXZpb3I6bm9uZX1ib2R5e3Bvc2l0aW9uOnJlbGF0aXZlO21pbi1oZWlnaHQ6MTAwZHZoO21pbi1oZWlnaHQ6MTAwc3ZoO21pbi1oZWlnaHQ6MTAwdmh9LmZsb2F0aW5nLWJhcntwb3NpdGlvbjpmaXhlZDtsZWZ0OjUwJTtib3R0b206Y2FsYygxMnB4ICsgZW52KHNhZmUtYXJlYS1pbnNldC1ib3R0b20sMHB4KSk7dHJhbnNmb3JtOnRyYW5zbGF0ZVgoLTUwJSk7d2lkdGg6bWluKDgxLjF2dyw3MjBweCk7aGVpZ2h0OmNsYW1wKDQ0cHgsNi4ydmgsMTEwcHgpO21pbi1oZWlnaHQ6NDRweDttYXgtaGVpZ2h0OjExMHB4O2JvcmRlci1yYWRpdXM6OTk5OXB4O2JhY2tncm91bmQ6bGluZWFyLWdyYWRpZW50KDE4MGRlZywjMzMzIDAsIzMwMzAzMCA1NSUsIzJkMmQyZCAxMDAlKTtib3JkZXI6MXB4IHNvbGlkICM0OTQ5NDk7Ym94LXNoYWRvdzppbnNldCAwIDFweCAycHggcmdiYSgyNTUsMjU1LDI1NSwuMDU1KSxpbnNldCAwIC0ycHggM3B4IHJnYmEoMCwwLDAsLjIpLDAgNXB4IDE4cHggcmdiYSgwLDAsMCwuMyl9QG1lZGlhIChvcmllbnRhdGlvbjpsYW5kc2NhcGUpey5mbG9hdGluZy1iYXJ7d2lkdGg6bWluKDgxLjF2dyw3MjBweCk7aGVpZ2h0OmNsYW1wKDQ0cHgsNi4ydmgsMTEwcHgpO2JvdHRvbTpjYWxjKDEwcHggKyBlbnYoc2FmZS1hcmVhLWluc2V0LWJvdHRvbSwwcHgpKX19PC9zdHlsZT48L2hlYWQ+PGJvZHk+PGRpdiBjbGFzcz0iZmxvYXRpbmctYmFyIiBhcmlhLWhpZGRlbj0idHJ1ZSI+PC9kaXY+PC9ib2R5PjwvaHRtbD4=");

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
