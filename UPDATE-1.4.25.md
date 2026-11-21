# Android Island 1.4.25

- Preserve WhatsApp stack while expanded: incoming media-session updates are kept as pending media and no longer replace the open WhatsApp stack.
- Lyric text is rendered white and the independently controlled lyric glow now uses a white glyph shadow rather than a green shadow. Shine remains a separate animated modifier.
- Removed the duplicate glow-intensity control so glow has one source of truth and shine controls remain independent.
- Increased the reply field's minimum touch/visual height to avoid a clipped input row.

Build verification: not completed in this environment. Gradle wrapper attempted to download Gradle 8.9 but network access to services.gradle.org was unavailable.
