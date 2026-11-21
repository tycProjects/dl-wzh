# Update 1.9.8 — Capsule Morph Smoothing

- Keep the expanded overlay host reserved until the close morph reaches its final progress, avoiding a WindowManager width resize/re-centre during the closing frames.
- Make open/close morph timing more symmetrical and soften the close landing.
- Change corner-radius interpolation from an early square-root ramp to a smooth S-curve so the capsule's curved corners evolve continuously in both directions.
- Preserve existing animation options, gestures, notifications, music/lyrics, WhatsApp stack, and Liquid Glass rendering.

Build status: not verified in this environment; a real Android SDK / Gradle build is still required.
