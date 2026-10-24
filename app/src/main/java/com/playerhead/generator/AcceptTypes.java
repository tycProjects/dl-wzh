package com.playerhead.generator;

import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;
import java.util.function.Function;

/**
 * Turns the value of an HTML file input's accept attribute into real MIME types.
 *
 * WebView hands the raw tokens to the app, for example ".png" or "image/png,.png".
 * Passing a token such as ".png" to Intent.setType() is invalid, so no app can handle the
 * intent and Android shows "No apps can perform this action" (no skin could be chosen).
 * This class converts every token into a valid MIME type instead.
 *
 * It uses no Android classes on purpose, so it can be unit tested on a normal computer.
 */
final class AcceptTypes {

    /** Wildcard type: no filtering. */
    static final String ANY = "*/*";

    private AcceptTypes() {
    }

    /**
     * @param acceptTypes     tokens from FileChooserParams.getAcceptTypes(); one entry may itself
     *                        contain several comma separated tokens
     * @param extensionToMime fallback lookup for extensions handled nowhere else (Android's
     *                        MimeTypeMap); may be null
     * @return valid MIME types without duplicates, or just {@link #ANY} when nothing should be
     * filtered
     */
    static String[] resolve(String[] acceptTypes, Function<String, String> extensionToMime) {
        Set<String> mimes = new LinkedHashSet<>();

        if (acceptTypes != null) {
            for (String entry : acceptTypes) {
                if (entry == null) continue;

                for (String raw : entry.split(",")) {
                    String token = raw.trim().toLowerCase(Locale.ROOT);
                    if (token.isEmpty()) continue;

                    if (token.startsWith(".")) {
                        addForExtension(mimes, token.substring(1), extensionToMime);
                    } else if (token.indexOf('/') > 0) {
                        mimes.add(token);
                    }
                    // Anything else is not a type we understand, so it is ignored.
                }
            }
        }

        if (mimes.isEmpty() || mimes.contains(ANY)) {
            return new String[]{ANY};
        }
        return mimes.toArray(new String[0]);
    }

    private static void addForExtension(Set<String> out, String extension,
                                        Function<String, String> extensionToMime) {
        switch (extension) {
            case "png":
                out.add("image/png");
                return;
            case "json":
                // File managers label .json files inconsistently, so accept the usual variants.
                out.add("application/json");
                out.add("text/plain");
                out.add("application/octet-stream");
                return;
            default:
                break;
        }

        String mime = extensionToMime == null ? null : extensionToMime.apply(extension);
        out.add(mime == null || mime.isEmpty() ? ANY : mime);
    }
}
