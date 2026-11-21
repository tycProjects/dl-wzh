# Update 1.9.15 — Close morph anti-flash

- Fixes the faint sharp-corner black flash at the end of the expanded-to-compact transition.
- Keeps the opening reveal unchanged.
- Adds a close-only fade window to the entire clipped panel surface, so tiny spring rebound samples near zero cannot briefly reveal the black backing as a rectangular sliver.
- Preserves the jelly spring, overshoot, content absorption, capsule geometry, and four-side static low-end glass.

Build status: source/package checks only; Release APK compilation must be confirmed in the target build environment.
