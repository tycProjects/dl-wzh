#!/usr/bin/env python3
"""Offline structural/regression checks for the current Android Island source package."""
from pathlib import Path
import re
import sys
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[1]
ANDROID = "{http://schemas.android.com/apk/res/android}"
errors: list[str] = []
checks = 0

def require(condition: bool, message: str) -> None:
    global checks
    checks += 1
    if not condition:
        errors.append(message)

def read(rel: str) -> str:
    path = ROOT / rel
    if not path.is_file():
        errors.append(f"Missing required file: {rel}")
        return ""
    return path.read_text(encoding="utf-8")

# Parse every XML resource, not only the manifest.
for xml_path in ROOT.rglob("*.xml"):
    try:
        ET.parse(xml_path)
        checks += 1
    except ET.ParseError as exc:
        errors.append(f"Malformed XML: {xml_path.relative_to(ROOT)}: {exc}")

manifest_path = ROOT / "app/src/main/AndroidManifest.xml"
manifest_text = read("app/src/main/AndroidManifest.xml")
require(r"\\n" not in manifest_text, "Manifest contains a literal \\n sequence")
try:
    manifest_root = ET.parse(manifest_path).getroot()
    app = manifest_root.find("application")
    require(app is not None, "Manifest has no application element")
    if app is not None:
        component_names = {
            node.attrib.get(ANDROID + "name", "")
            for tag in ("activity", "activity-alias", "service", "receiver", "provider")
            for node in app.findall(tag)
        }
        app_name = app.attrib.get(ANDROID + "name", "")
        require(app_name == ".IslandApplication", "Application class declaration is missing or unexpected")
        for component in (
            ".ui.MainActivity", ".service.IslandAccessibilityService",
            ".service.WhatsAppNotificationListener", ".service.BootReceiver",
        ):
            require(component in component_names, f"Manifest component missing: {component}")
except ET.ParseError:
    pass

# Current project identity: version is sourced from the Gradle module and VERSION.txt.
build = read("app/build.gradle.kts")
version_name = re.search(r'versionName\s*=\s*"([^"]+)"', build)
version_code = re.search(r"versionCode\s*=\s*(\d+)", build)
version_file = read("VERSION.txt").strip()
require(bool(version_name and version_name.group(1) == version_file), "VERSION.txt and Gradle versionName do not match")
require(bool(version_code and int(version_code.group(1)) > 0), "Gradle versionCode is missing or invalid")
require('namespace = "com.android.island"' in build, "Unexpected Android namespace")
require('applicationId = "com.android.island"' in build, "Unexpected applicationId")
require("compileSdk = 35" in build and "targetSdk = 35" in build and "minSdk = 26" in build,
        "SDK levels differ from the current project baseline (min 26 / target+compile 35)")

# Accessibility is intentionally enabled for explicit screen-reading actions. Do not silently
# downgrade this capability: ScreenReader and the AI screen-analysis flow depend on it.
a11y = ET.parse(ROOT / "app/src/main/res/xml/accessibility_service_config.xml").getroot()
require(a11y.attrib.get(ANDROID + "canRetrieveWindowContent") == "true",
        "Accessibility window-content capability is required by the explicit screen-reading feature")
require(a11y.attrib.get(ANDROID + "canTakeScreenshot") == "true",
        "Accessibility screenshot capability is required by the explicit screen-analysis feature")

# Diagnostics uses a bounded in-memory tail (not the historical MAX_ENTRIES name).
diag = read("app/src/main/java/com/android/island/util/IslandDiagnostics.kt")
for token in ("RAM_TAIL", "exportText", "snapshot", "recordingEnabled", "setEnabled", "recordError"):
    require(token in diag, f"Diagnostic recorder missing {token}")
require("entries.size >= RAM_TAIL" in diag and "entries.removeFirst()" in diag,
        "Diagnostic in-memory tail is not bounded")

# The expanded WhatsApp surface must use the banner width selected by settings.
root_ui = read("app/src/main/java/com/android/island/island/IslandRoot.kt")
require("current is IslandActivity.WhatsApp -> waBannerW" in root_ui,
        "WhatsApp expanded panel is not tied to the WhatsApp banner width")

# Stage 9 observe-act regression: pending actions must run even if done=true.
policy = read("app/src/main/java/com/android/island/ai/AgentLoopPolicy.kt")
session = read("app/src/main/java/com/android/island/ai/AiSession.kt")
require("modelSaysDone && !hasActions" in policy, "done=true must not discard pending actions")
require("modelSaysDone = env.done" in session, "AiSession must pass the original done flag to loop policy")
require("shouldStopAfterActions(env.done, actions.isNotEmpty())" in session,
        "AI loop must observe action results before stopping")
loop_tests = read("app/src/test/java/com/android/island/ai/AgentLoopPolicyTest.kt")
for test_name in ("continuesUntilThirtySixStepLimit", "stopsAtThirtySixStepLimit",
                  "continuesUntilFiveMinuteTimeLimit", "stopsAtFiveMinuteTimeLimit"):
    require(test_name in loop_tests, f"Observe-act boundary test missing: {test_name}")

# Stage 9 news de-duplication and canonical URL regression guards.
web_search = read("app/src/main/java/com/android/island/ai/WebSearch.kt")
require("internal fun canonicalUrl" in web_search, "Canonical URL helper is missing")
require("seenTitles.add(titleKey)" in web_search and "seenUrls.add(urlKey)" in web_search,
        "News title/URL deduplication guards are missing")
require('startsWith("utm_")' in web_search and "fbclid" in web_search,
        "Common tracking query parameters are not stripped")

# Shell safety boundaries remain explicit and testable.
shell_policy = read("app/src/main/java/com/android/island/ai/ShellPolicy.kt")
for token in ("FATAL_COMMANDS", "CONFIRM_COMMANDS", "PROTECTED_ROOTS", "Risk.FATAL"):
    require(token in shell_policy, f"Shell policy guard missing {token}")

# Public API catalog and validation must be present; no assumption that catalog entries are free.
public_apis = read("app/src/main/java/com/android/island/ai/PublicApis.kt")
require("MAX_REDIRECTS" in public_apis and "MAX_BODY" in public_apis,
        "Public API response/redirect bounds are missing")
require("Hanya https yang diizinkan" in public_apis and "tidak tercantum dalam katalog" in public_apis,
        "Public API host/scheme allow-list validation is missing")
require((ROOT / "app/src/main/assets/public_apis.txt").is_file(), "Public API catalog asset is missing")

# Gradle wrapper metadata should point to the project's pinned distribution.
wrapper = read("gradle/wrapper/gradle-wrapper.properties")
require("gradle-8.9-bin.zip" in wrapper, "Gradle wrapper is not pinned to Gradle 8.9")

# Stage 68: the jelly overshoot reserve must stay a SINGLE shared formula so the window owner
# (service) and the renderer (IslandRoot) can never disagree and cut the jelly crest flat.
window_tuning = read("app/src/main/java/com/android/island/core/WindowTuning.kt")
for token in ("jellyOvershootFraction", "jellyRoomReserveDp", "drawWindowRequiredHeightDp"):
    require(token in window_tuning, f"Shared jelly-reserve helper missing: {token}")
root_ui = read("app/src/main/java/com/android/island/island/IslandRoot.kt")
require("drawWindowRequiredHeightDp(" in root_ui and "jellyPrMax" in root_ui,
        "IslandRoot must derive the jelly crest amplitude (jellyPrMax) from the shared window budget")
service_src = read("app/src/main/java/com/android/island/service/IslandAccessibilityService.kt")
require("drawWindowRequiredHeightDp(" in service_src,
        "IslandAccessibilityService must size the draw window through the shared budget")
require("coerceAtMost(120" not in root_ui,
        "The old hard-capped 120dp jelly slack must not return (it cut tall panels)")

# Stage 68: cheap "alive" details — shared helpers must exist and be gated on the static renderer.
micro = read("app/src/main/java/com/android/island/island/MicroMotion.kt")
for token in ("fun Modifier.breathingAura", "fun Modifier.enterOnce"):
    require(token in micro, f"Alive-detail helper missing in MicroMotion: {token}")
ai_panel = read("app/src/main/java/com/android/island/island/AiPanel.kt")
require("rememberInfiniteTransition" not in ai_panel,
        "AiPanel must not run recomposition-based infinite transitions (draw-phase clocks only)")
require("aiBreathRim" in ai_panel, "AiPanel breathing rim light is missing")
call_banner = read("app/src/main/java/com/android/island/island/CallBanner.kt")
require("staggerIn(enter" in call_banner and "breatheGlow" in call_banner,
        "Call banner entrance cascade / breathing answer glow are missing")
wa_banner = read("app/src/main/java/com/android/island/island/WhatsAppBanner.kt")
require("breathingAura" in wa_banner, "WhatsApp banner breathing aura is missing")
compact = read("app/src/main/java/com/android/island/island/CompactIsland.kt")
require("BreathDot" in compact, "Compact capsule breathing status dot is missing")

if errors:
    print("FAIL")
    for error in errors:
        print(" - " + error)
    print(f"{checks} checks evaluated; {len(errors)} failed")
    sys.exit(1)
print(f"PASS: {checks} structural and regression checks")
