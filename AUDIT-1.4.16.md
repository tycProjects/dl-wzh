# 1.4.16 implementation audit

## Shizuku
- `StatusBarIconController` uses `DISABLE_NOTIFICATION_ICONS = 0x00020000`.
- No `icon_blacklist` command, setting, backup, or restore remains in this controller.
- No `DISABLE_SYSTEM_INFO` or `DISABLE_CLOCK` flag is requested.
- The implementation follows the supplied Expressive Cutout 0.3.1-beta status-bar
  controller's service reflection and Binder-token strategy.
- The supplied reference uses the same notification-icons flag. It does not contain
  a separate Samsung Now Bar API. Whether One UI maps Now Bar to this flag is OEM
  behavior and must be verified on the target Samsung firmware; this source alone
  cannot guarantee that separate Now Bar surface is hidden.

## Overlay touch region
- `sceneWidthDp` is no longer forced to `expandedW` in compact/hidden states.
- Expanded panel and banner receive their wider host only while those states are active.
- Compact host width is limited to the compact scene, allowing swipes outside the
  capsule to reach the system shade/Quick Settings.

## Validation
- Static checks: passed (flag constant, blacklist removal, state-specific host width).
- Gradle compile: not run successfully; Gradle 8.9 download failed with
  `UnknownHostException: services.gradle.org` in this environment.
- No device test performed.
