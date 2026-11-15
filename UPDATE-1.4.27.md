# Android Island 1.4.27

- Keep WhatsApp stack entry separate from media expansion: tapping the side WhatsApp indicator opens the notification stack; tapping the capsule opens pending media when available.
- Apply the user's compact/non-compact banner preference to every WhatsApp card inside the saved stack.
- Clip the banner's animated graphics layer to its rounded glass shape to prevent rectangular spill during swipe/dismiss.
- Smooth the upward dismiss spring while preserving the existing full-exit travel distance.
- No capsule anchor or Shizuku behavior changed.

Build verification: not completed in this environment because Gradle 8.9 distribution download failed (`UnknownHostException: services.gradle.org`).
