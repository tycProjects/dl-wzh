# Android Island 1.4.47 — motion and compact sizing

- Increased the default compact capsule size slightly (138 × 38 dp).
- Expanded compact size controls to 96–220 dp width and 28–52 dp height.
- Fixed media resume routing: when playback becomes active while the island is hidden or has no foreground activity, the media activity is pushed into the visible compact state instead of being stored only as pending media.
- Retuned the shared spring motion toward a more responsive, controlled settle, inspired by Droidbubble's adjustable spring/bounce/snap approach. No Droidbubble source code was copied.
- Build and device verification are still required before calling this release stable.
