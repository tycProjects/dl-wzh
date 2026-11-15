# Update 1.9.10 — Close flash fix

- Keep the expanded panel and its GL surface composed until the close Animatable is idle and has reached the compact endpoint. This removes the fixed 100 ms release guess that could release the panel before a Jelly rebound completed.
- Keep Jelly overshoot on opening, but make the close endpoint monotonic/critically damped so progress cannot dip below zero and re-trigger compact/panel composition at the end of the morph.
- Preserve the existing jelly stretch/squash layer, capsule anchor, corner morph, artwork/glass settings, and other features.
- Update package version to 1.9.10 (versionCode 107).

Build verification: source-package checks pass. Gradle compilation could not be run in this environment because the Gradle 8.9 distribution is not cached and services.gradle.org is unreachable.
