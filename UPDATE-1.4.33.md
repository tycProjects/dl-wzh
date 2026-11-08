# Android Island 1.4.33

## Fixes
- Glass Sweep uses `drawWithCache`: the gradient brush is cached for the measured surface and only translated during animation frames. This removes per-frame Brush/gradient allocations while retaining a single uninterrupted linear sweep.
- Opening a WhatsApp notification banner or selecting the WhatsApp OPEN_APP action clears all WhatsApp cards from the Android Island queue after the launch action succeeds.
- Clearing the Island queue does not cancel Android's actual WhatsApp notifications; only the Island's mirrored banner/stack is cleared.
- `Controls.openApp` now returns a Boolean so the UI only clears the stack after a successful launch request.

## Validation
- Source-level checks and archive integrity checked.
- Kotlin compilation/device frame-time testing still requires an Android Gradle build environment and a physical device; do not interpret this patch as a measured FPS guarantee.
