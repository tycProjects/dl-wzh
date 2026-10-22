package com.generated.ark;

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
            "PCFkb2N0eXBlIGh0bWw+PGh0bWwgbGFuZz0iZW4iPjxoZWFkPjxtZXRhIGNoYXJzZXQ9IlVURi04Ij48bWV0YSBuYW1lPSJ2aWV3cG9ydCIgY29udGVudD0id2lkdGg9ZGV2aWNlLXdpZHRoLGluaXRpYWwtc2NhbGU9MSI+PHRpdGxlPkNyeXB0b0Zsb3cgfCBNb2Rlcm4gQ3J5cHRvY3VycmVuY3kgVHJhZGluZyBQbGF0Zm9ybTwvdGl0bGU+PG1ldGEgbmFtZT0iZGVzY3JpcHRpb24iIGNvbnRlbnQ9IkV4cGVyaWVuY2Ugc2VhbWxlc3MgY3J5cHRvY3VycmVuY3kgdHJhZGluZyB3aXRoIHJlYWwtdGltZSBhbmFseXRpY3MsIEFJLXBvd2VyZWQgaW5zaWdodHMsIGFuZCB6ZXJvIGNvbW1pc3Npb24gZmVlcy4iPjxtZXRhIG5hbWU9ImF1dGhvciIgY29udGVudD0iQ3J5cHRvRmxvdyI+PG1ldGEgcHJvcGVydHk9Im9nOnRpdGxlIiBjb250ZW50PSJDcnlwdG9GbG93IHwgTW9kZXJuIENyeXB0b2N1cnJlbmN5IFRyYWRpbmcgUGxhdGZvcm0iPjxtZXRhIHByb3BlcnR5PSJvZzpkZXNjcmlwdGlvbiIgY29udGVudD0iRXhwZXJpZW5jZSBzZWFtbGVzcyBjcnlwdG9jdXJyZW5jeSB0cmFkaW5nIHdpdGggcmVhbC10aW1lIGFuYWx5dGljcywgQUktcG93ZXJlZCBpbnNpZ2h0cywgYW5kIHplcm8gY29tbWlzc2lvbiBmZWVzLiI+PG1ldGEgcHJvcGVydHk9Im9nOnR5cGUiIGNvbnRlbnQ9IndlYnNpdGUiPjxtZXRhIHByb3BlcnR5PSJvZzppbWFnZSIgY29udGVudD0iaHR0cHM6Ly9sb3ZhYmxlLmRldi9vcGVuZ3JhcGgtaW1hZ2UtcDk4cHFnLnBuZyI+PG1ldGEgbmFtZT0idHdpdHRlcjpjYXJkIiBjb250ZW50PSJzdW1tYXJ5X2xhcmdlX2ltYWdlIj48bWV0YSBuYW1lPSJ0d2l0dGVyOnNpdGUiIGNvbnRlbnQ9IkBjcnlwdG9mbG93Ij48bWV0YSBuYW1lPSJ0d2l0dGVyOmltYWdlIiBjb250ZW50PSJodHRwczovL2xvdmFibGUuZGV2L29wZW5ncmFwaC1pbWFnZS1wOThwcWcucG5nIj48bGluayByZWw9Imljb24iIHR5cGU9ImltYWdlL3N2Zyt4bWwiIGhyZWY9Ii4vZmF2aWNvbi9mYXZpY29uLmljbyI+PGxpbmsgcmVsPSJwcmVjb25uZWN0IiBocmVmPSJodHRwczovL2ZvbnRzLmdvb2dsZWFwaXMuY29tIj48bGluayByZWw9InByZWNvbm5lY3QiIGhyZWY9Imh0dHBzOi8vZm9udHMuZ3N0YXRpYy5jb20iIGNyb3Nzb3JpZ2luPjwvaGVhZD48Ym9keT48ZGl2IGlkPSJyb290Ij48L2Rpdj48c2NyaXB0IHR5cGU9Im1vZHVsZSIgc3JjPSIvc3JjL21haW4udHN4Ij48L3NjcmlwdD48L2JvZHk+PC9odG1sPg==");

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
