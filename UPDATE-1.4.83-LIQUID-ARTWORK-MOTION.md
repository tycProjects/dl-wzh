# Update 1.4.83 — Artwork opacity, clear Liquid Glass, and slower motion

- Renamed the artwork background control to **Opasitas artwork**; the value now multiplies the album artwork alpha directly and no longer changes the glass fill.
- Reduced the default Gaussian artwork blur to 0%, preserving sharp artwork unless the user deliberately raises the blur slider.
- Removed the opacity slider's black-background side effect and reduced the broad white/accent overlays to restrained optical glints.
- Reduced the white substrate fill so the artwork does not look milky or frosted; the glass substrate is independent of artwork opacity.
- Increased the three-dome lens displacement, bevel response, and directional dome reflections while retaining the connected three-dome field.
- Slowed the capsule-to-expanded morph and close transition; increased the stagger interval and entrance duration for WhatsApp stack cards.
- Preserved the existing WhatsApp per-message identity/dedupe and reply behavior files without modification.

Build status: source-level edits only; Gradle/Kotlin compilation and device animation tests still need to be run.
- Added a one-time settings migration that clears the legacy full-strength frost and reduces the old white veil, while keeping both controls available for future manual adjustment.
