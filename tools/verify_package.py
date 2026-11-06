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
if not version_name or version_name.group(1) != "1.4.55":
    errors.append("Expected app versionName 1.4.55")
if not version_code or version_code.group(1) != "64":
    errors.append("Expected app versionCode 64")

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

if errors:
    print("FAIL")
    for error in errors:
        print(" -", error)
    sys.exit(1)

print("PASS: all XML resources parse")
print("PASS: required service declarations are present")
print("PASS: manifest has no malformed escaped newline")
print("PASS: version metadata is 1.4.55 (64)")
print("PASS: accessibility window-content retrieval is disabled")
print("PASS: Shizuku uses notification-icon disable flag without mutating system icon slots")
