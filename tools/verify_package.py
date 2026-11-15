#!/usr/bin/env python3
"""Offline structural checks for the Android Island source package."""
from pathlib import Path
import re
import sys
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[1]
ANDROID = "{http://schemas.android.com/apk/res/android}"
errors = []

for xml_path in ROOT.rglob("*.xml"):
    try:
        ET.parse(xml_path)
    except ET.ParseError as exc:
        errors.append(f"Malformed XML: {xml_path.relative_to(ROOT)}: {exc}")

manifest_path = ROOT / "app/src/main/AndroidManifest.xml"
manifest = manifest_path.read_text(encoding="utf-8")
if r"\n" in manifest:
    errors.append("AndroidManifest.xml contains a literal \\n sequence")
try:
    tree = ET.parse(manifest_path)
    root = tree.getroot()
    app = root.find("application")
    if app is None:
        errors.append("Manifest has no application element")
    else:
        names = {node.attrib.get(ANDROID + "name", "") for node in app.findall("service")}
        required = {
            ".service.IslandAccessibilityService",
            ".service.WhatsAppNotificationListener",
        }
        missing = required - names
        if missing:
            errors.append("Missing service declarations: " + ", ".join(sorted(missing)))
except ET.ParseError:
    pass

build = (ROOT / "app/build.gradle.kts").read_text(encoding="utf-8")
version_name = re.search(r'versionName\s*=\s*"([^"]+)"', build)
version_code = re.search(r"versionCode\s*=\s*(\d+)", build)
if not version_name or version_name.group(1) != "1.9.20":
    errors.append("Expected app versionName 1.9.20")
if not version_code or version_code.group(1) != "117":
    errors.append("Expected app versionCode 117")

accessibility = ET.parse(ROOT / "app/src/main/res/xml/accessibility_service_config.xml").getroot()
if accessibility.attrib.get(ANDROID + "canRetrieveWindowContent") != "false":
    errors.append("Accessibility service must not retrieve other apps' window content")

diagnostics = ROOT / "app/src/main/java/com/android/island/util/IslandDiagnostics.kt"
if not diagnostics.exists():
    errors.append("Missing in-app diagnostic recorder")
else:
    diagnostic_text = diagnostics.read_text(encoding="utf-8")
    for required in ["MAX_ENTRIES", "exportText", "snapshot", "recordingEnabled", "setEnabled"]:
        if required not in diagnostic_text:
            errors.append(f"Diagnostic recorder missing {required}")

status_controller = ROOT / "app/src/main/java/com/android/island/system/StatusBarIconController.kt"
if not status_controller.exists():
    errors.append("Missing Shizuku status-bar controller")
else:
    status_text = status_controller.read_text(encoding="utf-8")
    for required in ["DISABLE_NOTIFICATION_ICONS", "0x00020000", "disableForUser", "lastApplied"]:
        if required not in status_text:
            errors.append(f"Shizuku controller missing {required}")
    if "icon_blacklist" in status_text:
        errors.append("Shizuku controller must not mutate icon_blacklist")

# Regression guards (kept in sync with 1.9.0) for the Liquid Glass / WhatsApp changes.
listener = (ROOT / "app/src/main/java/com/android/island/service/WhatsAppNotificationListener.kt").read_text(encoding="utf-8")
if "WA_SOURCE_REMOVED" not in listener or "IslandBus.clearWhatsApp(sbn.key)" in listener:
    errors.append("WhatsApp source removal must preserve the Island-owned banner")
if "lastProcessedText.remove(sbn.key)" in listener:
    errors.append("WhatsApp source removal must retain its deduplication fingerprint")
expanded = (ROOT / "app/src/main/java/com/android/island/island/ExpandedIsland.kt").read_text(encoding="utf-8")
if "rememberLazyListState()" not in expanded or "itemsIndexed(items = visible, key = { _, chat -> chat.id })" not in expanded:
    errors.append("Expanded WhatsApp stack must use stable keyed lazy scrolling")
settings = (ROOT / "app/src/main/java/com/android/island/data/SettingsRepository.kt").read_text(encoding="utf-8")
if "backgroundOpacity.coerceIn(0f, 1f)" not in settings:
    errors.append("Background opacity must support the complete 0–100% range")
lens = (ROOT / "app/src/main/java/com/android/island/island/LiquidLens.kt").read_text(encoding="utf-8")
if "uShift" not in lens or "float2 bend = -normal * (uShift * fold * fold)" not in lens:
    errors.append("Lens refraction shader implementation is missing")

touch_service = (ROOT / "app/src/main/java/com/android/island/service/IslandAccessibilityService.kt").read_text(encoding="utf-8")
for required in ["closeMorphHoldUntilMs", "TOUCH_BOUNDS_MORPH_HOLD", "hold-expanded-until-close-settles"]:
    if required not in touch_service:
        errors.append(f"Touch morph guard missing {required}")

# Low-end Liquid Glass must use the static, shader-free rendering path.
profile = (ROOT / "app/src/main/java/com/android/island/core/DeviceProfile.kt").read_text(encoding="utf-8")
root_ui = (ROOT / "app/src/main/java/com/android/island/island/IslandRoot.kt").read_text(encoding="utf-8")
liquid = (ROOT / "app/src/main/java/com/android/island/island/LiquidGlass.kt").read_text(encoding="utf-8")
backdrop = (ROOT / "app/src/main/java/com/android/island/island/FluidGlass.kt").read_text(encoding="utf-8")
if "staticGlass" not in profile or "applyStaticGlass(settings.lowEndMode)" not in root_ui:
    errors.append("Low-end setting is not wired to static glass mode")
if "setLowEnd(settings.lowEndMode)" not in root_ui or "GlassTuning.lowEnd = if (settings.lowEndMode) 1 else 0" not in root_ui:
    errors.append("Only the explicit Low-End toggle may select the low-allocation glass path")
if "setLowEnd(memoryEfficient)" in root_ui:
    errors.append("Low Power/Battery Saver must not silently activate Low-End glass rendering")
if "if (DeviceProfile.staticGlass) com.android.island.island.trimDeepIdleCaches()" not in (ROOT / "app/src/main/java/com/android/island/core/ScreenState.kt").read_text(encoding="utf-8"):
    errors.append("Deep idle lens cache release must be scoped to Low-End mode")
if "DeviceProfile.staticGlass" not in liquid or "StaticGlassBackdrop" not in backdrop:
    errors.append("Plain Low-End Liquid Glass renderer is missing")
if "bakeStaticOverlay(" in backdrop or "drawStaticOverlay(" in backdrop or "drawStaticMirrorReflection" in backdrop:
    errors.append("Low-End backdrop must not bake overlay bitmaps or draw reflection layers")
if "filterQuality = FilterQuality.Low" not in backdrop or "Color.Black.copy(alpha = scrimAlpha)" not in backdrop:
    errors.append("Low-End backdrop must use a direct low-quality bitmap draw and one flat scrim")
expanded_music = (ROOT / "app/src/main/java/com/android/island/island/ExpandedIsland.kt").read_text(encoding="utf-8")
whatsapp_banner = (ROOT / "app/src/main/java/com/android/island/island/WhatsAppBanner.kt").read_text(encoding="utf-8")
if "background(if (com.android.island.core.DeviceProfile.staticGlass) Color.Black" not in expanded_music:
    errors.append("Low-End expanded music black surface is missing")
if "source.bitmap.asImageBitmap()" not in whatsapp_banner or "Low-End WhatsApp" not in whatsapp_banner:
    errors.append("Low-End WhatsApp plain avatar background is missing")

root_source = (ROOT / "app/src/main/java/com/android/island/island/IslandRoot.kt").read_text(encoding="utf-8")
if "smooth(reveal, 0.045f, 0.18f)" not in root_source:
    errors.append("Missing close morph anti-flash guard")

# Regression guard for the 1.9.17 build break: Compose DrawScope lives under graphics.drawscope.
for kt in (ROOT / "app/src/main/java").rglob("*.kt"):
    if "import androidx.compose.ui.drawscope." in kt.read_text(encoding="utf-8"):
        errors.append(f"Wrong Compose drawscope package (must be androidx.compose.ui.graphics.drawscope): {kt.relative_to(ROOT)}")

if "var staticGlass: Boolean by mutableStateOf(false)" not in profile or "var tier: Int by mutableIntStateOf(1)" not in profile:
    errors.append("DeviceProfile low-end flags must be Compose state (like GlassTuning), not @Volatile")

motion_src = (ROOT / "app/src/main/java/com/android/island/island/IslandMotion.kt").read_text(encoding="utf-8")
if "Elasticity.HIGH -> 0.50f" in motion_src or "fun jellyDamping(" not in motion_src:
    errors.append("Jelly damping must come from the single jellyDamping() table in IslandMotion.kt")
if "if (com.android.island.core.DeviceProfile.staticGlass) return this" not in (ROOT / "app/src/main/java/com/android/island/island/LiquidLens.kt").read_text(encoding="utf-8"):
    errors.append("Low-End edge sheen must be bypassed without allocating gradient brushes")
controls = (ROOT / "app/src/main/java/com/android/island/island/UnifiedGlassEngine.kt").read_text(encoding="utf-8")
if "if (com.android.island.core.DeviceProfile.staticGlass)" not in controls:
    errors.append("Low-End playback controls must use solid fills, not gradients")
if "!com.android.island.core.DeviceProfile.staticGlass" not in expanded:
    errors.append("Low-End lyric glow/shine and glass sweep must be bypassed")
if "glassSweepEnabled && !com.android.island.core.DeviceProfile.staticGlass" not in expanded:
    errors.append("Low-End must not start the decorative glass sweep clock")
if "glowAlpha > 0.02f && !com.android.island.core.DeviceProfile.staticGlass" not in expanded:
    errors.append("Low-End must not draw the lyric glow/offscreen layer")
if "shineOn && !com.android.island.core.DeviceProfile.staticGlass" not in expanded:
    errors.append("Low-End must not draw the lyric shine gradient")
if "if (com.android.island.core.DeviceProfile.staticGlass)" not in controls or "return@drawBehind" not in controls:
    errors.append("Low-End playback controls must short-circuit before gradient creation")

svc = (ROOT / "app/src/main/java/com/android/island/service/IslandAccessibilityService.kt").read_text(encoding="utf-8")
if "MATCH_PARENT,\n            drawWindowFixedHeightPx()" in svc or "drawLane(" not in svc:
    errors.append("Drawing window must use the centred lane width, not MATCH_PARENT")
if "(drawLp?.x ?: 0)" not in svc:
    errors.append("Touch forwarding must subtract the drawing window origin")
if "fun laneWidthDp(" not in (ROOT / "app/src/main/java/com/android/island/core/WindowTuning.kt").read_text(encoding="utf-8"):
    errors.append("Lane width rule missing from WindowTuning")
if "ScreenState.refreshPowerSave" not in (ROOT / "app/src/main/java/com/android/island/IslandApplication.kt").read_text(encoding="utf-8"):
    errors.append("Battery saver state is not tracked in IslandApplication")

if "overlayView?.isAttachedToWindow == false" not in svc:
    errors.append("Overlay must be rebuilt when the system has detached it (long idle, screen off)")
if "ScreenState.interactive.collect" not in svc:
    errors.append("Overlay must be checked when the screen comes back on")
if "val panelW = if (current is IslandActivity.WhatsApp) waBannerW else expandedW" not in (ROOT / "app/src/main/java/com/android/island/island/IslandRoot.kt").read_text(encoding="utf-8"):
    errors.append("WhatsApp expanded panel must use the banner width")

wa_listener = (ROOT / "app/src/main/java/com/android/island/service/WhatsAppNotificationListener.kt").read_text(encoding="utf-8")
if "if (live === this) live = null" not in wa_listener:
    errors.append("Listener must forget its instance on disconnect, or screen-on never requests a rebind")
if "requestListenerRebind(app)" not in wa_listener:
    errors.append("Screen-on must rebind the notification listener when it is not connected")
if "missed.sortedBy { it.postTime }" not in wa_listener:
    errors.append("Recent WhatsApp notifications from while the listener was unbound must be shown")

if "active = media.isPlaying," not in (ROOT / "app/src/main/java/com/android/island/island/ExpandedIsland.kt").read_text(encoding="utf-8"):
    errors.append("Lyric shine must stop while the media is paused")

fluid_src = (ROOT / "app/src/main/java/com/android/island/island/FluidGlass.kt").read_text(encoding="utf-8")
if "source.copy(Bitmap.Config.ARGB_8888, false) ?: return@runCatching null\n        } else source" in fluid_src:
    errors.append("Hardware artwork must not be copied at full size before scaling")

if 'val islandThemeName = if (settings.lowEndMode) "WHITE" else settings.islandTheme.name' not in (ROOT / "app/src/main/java/com/android/island/island/IslandRoot.kt").read_text(encoding="utf-8"):
    errors.append("Low-end mode must force the white island theme")

if "trimDeepIdleCaches()" not in (ROOT / "app/src/main/java/com/android/island/core/ScreenState.kt").read_text(encoding="utf-8"):
    errors.append("Long screen-off must release the lens caches")

if "if (!com.android.island.core.ScreenState.interactive.value) return false" not in (ROOT / "app/src/main/java/com/android/island/service/IslandAccessibilityService.kt").read_text(encoding="utf-8"):
    errors.append("Overlay must be released while the screen is off")

if "opaqueToRgb565(shrinkArt(rawArt))" not in (ROOT / "app/src/main/java/com/android/island/service/WhatsAppNotificationListener.kt").read_text(encoding="utf-8"):
    errors.append("Opaque media artwork must be stored as RGB_565")

if "synchronized(lock) { entries.clear() }" not in (ROOT / "app/src/main/java/com/android/island/util/IslandDiagnostics.kt").read_text(encoding="utf-8"):
    errors.append("Disabling diagnostics must release the log buffer")

svc2 = (ROOT / "app/src/main/java/com/android/island/service/IslandAccessibilityService.kt").read_text(encoding="utf-8")
if "cancelWindowShrink(\"touch\")" not in svc2:
    errors.append("Window must grow back on touch-down before a gesture reaches the renderer")
if "compactWindowShrink: Boolean = false" not in (ROOT / "app/src/main/java/com/android/island/data/SettingsRepository.kt").read_text(encoding="utf-8"):
    errors.append("Window shrink must default to off (experimental)")

bus_src = (ROOT / "app/src/main/java/com/android/island/core/IslandBus.kt").read_text(encoding="utf-8")
if "while (items.size > MAX_WHATSAPP_QUEUE) items.removeAt(0)" not in bus_src:
    errors.append("WhatsApp queue must be bounded")
if "while (mem.size > 1000)" in (ROOT / "app/src/main/java/com/android/island/island/ExpandedIsland.kt").read_text(encoding="utf-8"):
    errors.append("Liked-track set must not carry an arbitrary cap")

if "windowManager.removeViewImmediate(view)" not in (ROOT / "app/src/main/java/com/android/island/service/IslandAccessibilityService.kt").read_text(encoding="utf-8"):
    errors.append("Overlay window must always be removed on dispose (leak guard)")

svc3 = (ROOT / "app/src/main/java/com/android/island/service/IslandAccessibilityService.kt").read_text(encoding="utf-8")
if "if (sig == lastDisplaySig) return" not in svc3:
    errors.append("Display changes must be ignored unless size, density or rotation changed")
if "maxOf(drawH, touchH)" not in svc3:
    errors.append("Shrunk window must cover the touch bounds, not only the drawn capsule")

wa3 = (ROOT / "app/src/main/java/com/android/island/service/WhatsAppNotificationListener.kt").read_text(encoding="utf-8")
if "if (currentTrackKey != trackKey || activeController == null) return@withContext" not in wa3:
    errors.append("Background artwork must be published only for the track still on screen")
if "opaqueToRgb565(shrinkArt(rawArt))" not in wa3 or "opaqueToRgb565(shrinkArt(it))" in wa3:
    errors.append("Cover-art processing must run off the main thread")

if "private val windowShrinkAllowed = false" not in (ROOT / "app/src/main/java/com/android/island/service/IslandAccessibilityService.kt").read_text(encoding="utf-8"):
    errors.append("Window shrink must stay disabled: its grow-back causes a visible stutter")

if "com.hamondev.shevery" in (ROOT / "app/src/main/AndroidManifest.xml").read_text(encoding="utf-8"):
    errors.append("Unused package com.hamondev.shevery must not be queried")
if "700f / span" not in (ROOT / "app/src/main/java/com/android/island/island/LyricGlow.kt").read_text(encoding="utf-8"):
    errors.append("Reading-head snap threshold must be time-based")

if errors:
    print("FAIL")
    for error in errors:
        print(" -", error)
    sys.exit(1)

print("PASS: all XML resources parse")
print("PASS: required service declarations are present")
print("PASS: manifest has no malformed escaped newline")
print("PASS: version metadata is 1.9.20 (117)")

print("PASS: close morph anti-flash guard")
print("PASS: expanded touch bounds are held until close morph settles")
print("PASS: accessibility window-content retrieval is disabled")
print("PASS: Shizuku uses notification-icon disable flag without mutating system icon slots")
