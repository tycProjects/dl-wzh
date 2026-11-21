# Low-End mode isolation update

- The app now starts with the Normal renderer budget; no RAM-based or automatic profile selection is used.
- The explicit Low-End switch selects the minimum-allocation renderer. Turning it off restores the full Normal renderer.
- Low-End expanded music surfaces remain OLED black and skip the album-art backdrop, blur, shader sweep, and premium glass layers.
- Low-End WhatsApp banners now use a pure #000000 background instead of drawing the contact avatar as a blurred/full-card backdrop. The contact avatar remains as the small foreground avatar.
- Normal mode keeps its existing enlarged/blurred album-art backdrop and Liquid Glass rendering paths.
- Jelly and core interaction/open-close animations are not removed by this change.
- The Low-End settings description now reflects the actual behavior.

Build verification could not be completed in this environment: Gradle 8.9 was not cached, network access to services.gradle.org was unavailable, and ANDROID_HOME was not configured.
