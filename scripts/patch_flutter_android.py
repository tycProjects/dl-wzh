#!/usr/bin/env python3
"""Patch a freshly generated Flutter Android host to embed the Local AIDE PRoot engine."""
from pathlib import Path
import re
import shutil
import sys

if len(sys.argv) != 2:
    raise SystemExit("Usage: patch_flutter_android.py <flutter-host-project>")
root = Path(sys.argv[1]).resolve()
android = root / "android"
if not android.is_dir():
    raise SystemExit(f"Flutter Android host not found: {android}; run flutter create first")

# Include the no-Termux ARM64 PRoot library fetched by prepare_no_termux_engine.sh.
settings_candidates = [android / "settings.gradle.kts", android / "settings.gradle"]
settings = next((p for p in settings_candidates if p.exists()), None)
if settings is None:
    raise SystemExit("Neither android/settings.gradle.kts nor android/settings.gradle exists")
contents = settings.read_text()
if 'include(":proot-engine")' not in contents and "include ':proot-engine'" not in contents:
    if settings.suffix == ".kts":
        contents += '\ninclude(":proot-engine")\nproject(":proot-engine").projectDir = file("../vendor/pr/android/proot-engine")\n'
    else:
        contents += "\ninclude ':proot-engine'\nproject(':proot-engine').projectDir = file('../vendor/pr/android/proot-engine')\n"

# The upstream library declares com.android.library without a version. Flutter host
# templates normally pin only com.android.application, so also pin the matching
# library plugin version in the shared settings plugins block.
if settings.suffix == ".kts" and 'id("com.android.library")' not in contents:
    app_plugin = re.search(r'id\("com\.android\.application"\)\s+version\s+"([^"]+)"', contents)
    if app_plugin:
        match = app_plugin.group(1)
        contents = re.sub(
            r'(id\("com\.android\.application"\)\s+version\s+"' + re.escape(match) + r'"[^\n]*\n)',
            r'\1    id("com.android.library") version "' + match + '" apply false\n',
            contents,
            count=1,
        )
    else:
        raise SystemExit("Could not find com.android.application plugin version to pin com.android.library")
settings.write_text(contents)

app_gradle_candidates = [android / "app" / "build.gradle.kts", android / "app" / "build.gradle"]
app_gradle = next((p for p in app_gradle_candidates if p.exists()), None)
if app_gradle is None:
    raise SystemExit("Flutter app Gradle file was not found")
gradle = app_gradle.read_text()
if app_gradle.suffix == ".kts":
    # The engine requires API 36 and Android 9+ (minSdk 28).
    if re.search(r"compileSdk\s*=\s*flutter\.compileSdkVersion", gradle):
        gradle = re.sub(r"compileSdk\s*=\s*flutter\.compileSdkVersion", "compileSdk = maxOf(flutter.compileSdkVersion, 36)", gradle, count=1)
    elif re.search(r"compileSdk\s*=\s*\d+", gradle):
        gradle = re.sub(r"compileSdk\s*=\s*\d+", "compileSdk = 36", gradle, count=1)
    if re.search(r"minSdk\s*=\s*flutter\.minSdkVersion", gradle):
        gradle = re.sub(r"minSdk\s*=\s*flutter\.minSdkVersion", "minSdk = maxOf(flutter.minSdkVersion, 28)", gradle, count=1)
    elif re.search(r"minSdk\s*=\s*\d+", gradle):
        gradle = re.sub(r"minSdk\s*=\s*\d+", "minSdk = 28", gradle, count=1)
    if 'implementation(project(":proot-engine"))' not in gradle:
        match = re.search(r"dependencies\s*\{", gradle)
        if not match:
            raise SystemExit("Could not find dependencies block in app Gradle file")
        at = match.end()
        gradle = gradle[:at] + '\n    implementation(project(":proot-engine"))' + gradle[at:]
else:
    if re.search(r"compileSdkVersion\s+flutter\.compileSdkVersion", gradle):
        gradle = re.sub(r"compileSdkVersion\s+flutter\.compileSdkVersion", "compileSdkVersion 36", gradle, count=1)
    elif re.search(r"compileSdkVersion\s+\d+", gradle):
        gradle = re.sub(r"compileSdkVersion\s+\d+", "compileSdkVersion 36", gradle, count=1)
    if re.search(r"minSdkVersion\s+flutter\.minSdkVersion", gradle):
        gradle = re.sub(r"minSdkVersion\s+flutter\.minSdkVersion", "minSdkVersion 28", gradle, count=1)
    elif re.search(r"minSdkVersion\s+\d+", gradle):
        gradle = re.sub(r"minSdkVersion\s+\d+", "minSdkVersion 28", gradle, count=1)
    if "implementation project(':proot-engine')" not in gradle:
        match = re.search(r"dependencies\s*\{", gradle)
        if not match:
            raise SystemExit("Could not find dependencies block in app Gradle file")
        at = match.end()
        gradle = gradle[:at] + "\n    implementation project(':proot-engine')" + gradle[at:]
app_gradle.write_text(gradle)

manifest = android / "app" / "src" / "main" / "AndroidManifest.xml"
if not manifest.exists():
    raise SystemExit(f"Manifest not found: {manifest}")
xml = manifest.read_text()
xml = re.sub(r'android:name="\$\{applicationName\}"', 'android:name="com.localaide.local_aide.LocalAideApplication"', xml, count=1)
if 'android:name="com.localaide.local_aide.LocalAideApplication"' not in xml:
    # Add name to the application tag when a nonstandard template is encountered.
    xml, count = re.subn(r"<application\b", '<application android:name="com.localaide.local_aide.LocalAideApplication"', xml, count=1)
    if count != 1:
        raise SystemExit("Could not set LocalAideApplication in AndroidManifest.xml")
for permission in [
    '    <uses-permission android:name="android.permission.INTERNET" />',
    '    <uses-permission android:name="android.permission.MANAGE_EXTERNAL_STORAGE" />',
]:
    perm_name = re.search(r'android:name="([^"]+)"', permission).group(1)
    if f'android:name="{perm_name}"' not in xml:
        xml = re.sub(r"(<manifest\b[^>]*>)", r"\1\n" + permission, xml, count=1)
manifest.write_text(xml)

# Remove stale generated Kotlin entrypoints before copying the custom ones.
kotlin_root = android / "app" / "src" / "main" / "kotlin"
java_root = android / "app" / "src" / "main" / "java"
for base in [kotlin_root, java_root]:
    if base.exists():
        for old in base.rglob("MainActivity.kt"):
            if old.parent != (kotlin_root / "com" / "localaide" / "local_aide"):
                old.unlink()

source = Path(__file__).resolve().parents[1]
destination = kotlin_root / "com" / "localaide" / "local_aide"
destination.mkdir(parents=True, exist_ok=True)
for name in ["MainActivity.kt", "LocalAideApplication.kt", "LocalAideBootstrap.kt"]:
    shutil.copy2(source / "android_patch" / name, destination / name)

assets = android / "app" / "src" / "main" / "assets"
for subdir in ["bin", "scripts"]:
    (assets / subdir).mkdir(parents=True, exist_ok=True)
    srcdir = source / "android_assets" / subdir
    for item in srcdir.iterdir():
        shutil.copy2(item, assets / subdir / item.name)
print(f"Patched {root} to include the embedded no-Termux PRoot engine.")
