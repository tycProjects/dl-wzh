# Android Island 1.4.48 — Animation Pipeline Optimization

- Moved the capsule appearance animation read from composition into the graphics-layer phase. This prevents every animation frame from invalidating and recomposing the full island subtree.
- Kept the expand/collapse animation coroutine stable while motion preferences change. Updating settings no longer cancels and recreates an in-flight transition; changing the target still retargets the same Animatable.
- Preserved independent banner movement and the fixed capsule anchor.

Build status: source-level patch only; Gradle build must be verified in an environment with the required Gradle distribution and Android dependencies available.
