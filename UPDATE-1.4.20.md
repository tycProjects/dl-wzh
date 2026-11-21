# Android Island 1.4.20 — Motion & Glass controls

- Added an independent non-compact WhatsApp banner width preference; compact and standard widths no longer share one setting.
- Added a one-shot right-to-left Glass Sweep on expanded media surfaces, with enable/disable and opacity controls. It is a surface highlight, not a replacement background.
- Kept lyric shine and lyric glow intensity intact in Low Power and Ultra profiles.
- Kept core island morph, banner entrance, and banner swipe settle animations on the same spring/tween motion path in power-saving profiles. Expensive blur/rendering reductions remain separate from core motion.
- Kept capsule anchor geometry untouched.

Build verification: not completed. Gradle 8.9 could not be downloaded in this environment (`services.gradle.org` DNS/network unavailable), so compilation must be verified in an Android/Gradle environment.
