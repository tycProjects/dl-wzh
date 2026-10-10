# Leon-Source web

Android Studio / Gradle project for an Android source inspector with a dark-neon interface.

## What's new in 2.0
- Redesigned UI: rounded cards, segmented tabs with resource counts, progress bar, HTML syntax highlighting, search with next/previous + match counter, ripple feedback, edge-to-edge insets (Android 15 safe).
- **Info (i) menu**: tells you whether the site is pure static HTML, a CMS site, a framework site, or a JavaScript-rendered SPA; also shows headers, structure counts, detected technologies (React/Vue/Angular/WordPress/jQuery/Bootstrap/...), SEO meta, and detects when the URL is actually a ZIP/PDF/binary instead of a web page. "Copy report" included.
- **Settings (gear) menu**: accent colour (8 presets + custom hue slider), background (Dark / AMOLED / Midnight / Light), interface font, code font, code size, syntax-highlight toggle, reset. Saved automatically.
- HTTP -> HTTPS redirects are followed; charset is honoured; Format now indents the HTML.
- 2.1: zero dependencies (no androidx), lint skipped on release, Gradle caching + parallel + configuration cache, async syntax highlighting. Much faster build, smaller APK.

## Included
- Fetches public HTTP/HTTPS pages and shows raw HTML.
- Lists linked CSS and JavaScript resource URLs found in static HTML.
- Extracts static image/video/audio/source links.
- Search in the currently displayed source.
- Basic whitespace formatting/minification.
- Exports HTML and up to 30 linked resources (per-resource 2 MB cap) into a ZIP and opens Android's share sheet.
- No API key or backend required for these basic features.

## Build
1. Open this folder in Android Studio.
2. Use JDK 17 and allow Gradle to sync (Android Gradle Plugin 8.7.3, compile SDK 35).
3. Build > Build APK(s).

This archive is a project source ZIP, not a prebuilt APK. The Gradle wrapper JAR is not included; Android Studio can import the project and use its configured Gradle installation, or generate wrapper files from the project using `gradle wrapper`.

## Known limitations
- Static HTTP fetch only. It does not execute page JavaScript or reproduce the browser DOM after scripts run.
- "CSS" and "JS" tabs list discovered resource URLs; this first version does not yet fetch and display the contents of each linked asset individually.
- Formatting/minifying is intentionally basic and not a full language-aware parser.
- Media extraction only detects common URLs in static HTML; CSS background images, lazy-loaded content, blob URLs, authenticated resources, and some modern web apps are not included.
- ZIP is a static snapshot. It doesn't rewrite every resource URL in HTML, so the downloaded bundle may not run offline unchanged.
- Some sites block automated clients, require login, use anti-bot checks, or have certificate/network policies that prevent fetching.
- Only inspect or download content you have permission to access.
- A backend would be useful for JavaScript-rendered pages, crawling multiple pages, robust HTML parsing, proxying restricted network setups, larger downloads, and advanced resource rewriting. Respect site terms, robots policies, and copyright.
