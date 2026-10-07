package com.generated.dem0;

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
            "PCFkb2N0eXBlIGh0bWw+PGh0bWwgbGFuZz0iZW4iPjxoZWFkPjxtZXRhIGNoYXJzZXQ9IlVURi04Ij48bWV0YSBuYW1lPSJ2aWV3cG9ydCIgY29udGVudD0id2lkdGg9ZGV2aWNlLXdpZHRoLGluaXRpYWwtc2NhbGU9MSI+PHRpdGxlPlB5U2NyaXB0IFRlc3Q8L3RpdGxlPjxsaW5rIHJlbD0ic3R5bGVzaGVldCIgaHJlZj0iaHR0cHM6Ly9weXNjcmlwdC5uZXQvcmVsZWFzZXMvMjAyNC4xLjEvY29yZS5jc3MiPjxzY3JpcHQgdHlwZT0ibW9kdWxlIiBzcmM9Imh0dHBzOi8vcHlzY3JpcHQubmV0L3JlbGVhc2VzLzIwMjQuMS4xL2NvcmUuanMiPjwvc2NyaXB0PjxzdHlsZT5ib2R5e2JhY2tncm91bmQtY29sb3I6I2YwZjBmMDtmb250LWZhbWlseTpBcmlhbCxzYW5zLXNlcmlmO21hcmdpbjo0MHB4fWgxe2NvbG9yOiMwMGZ9aDJ7Zm9udC1zaXplOjEuNWVtO2NvbG9yOmdyZWVufWgze2NvbG9yOmdyZWVufTwvc3R5bGU+PC9oZWFkPjxib2R5PjxoMT5QeVNjcmlwdCBUZXN0PC9oMT48aDIgaWQ9Im91dHB1dCI+TG9hZGluZyBQeXRob24uLi48L2gyPjxoMyBpZD0id2VsY29tZSI+Li4uPC9oMz48c2NyaXB0IHR5cGU9InB5Ij5mcm9tIHB5c2NyaXB0IGltcG9ydCBkb2N1bWVudA0KDQogICAgICAgICMgVXBkYXRlIHRoZSBoMiBlbGVtZW50IGRpcmVjdGx5IG9uIHBhZ2UgbG9hZA0KICAgICAgICBkb2N1bWVudC5xdWVyeVNlbGVjdG9yKCIjb3V0cHV0IikuaW5uZXJUZXh0ID0gIlB5dGhvbiBpcyBydW5uaW5nIGluc2lkZSBIVE1MISINCiAgICAgICAgZG9jdW1lbnQucXVlcnlTZWxlY3RvcigiI3dlbGNvbWUiKS5pbm5lclRleHQgPSAiV0VMQ09NRSBUTyBQWVNSSVBUISI8L3NjcmlwdD48L2JvZHk+PC9odG1sPg==");

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
