#!/usr/bin/env python3
"""Run once, from the project folder, after `flutter create .` has made the android/ folder:

    flutter create --org com.wzh --project-name wzh_flutter_ide .
    python3 tool/patch_android.py

What it changes (all needed for the on-phone Linux to work):
  * targetSdk 28 .......... lets the app run proot and the other tools it unpacks into its own folder
                            (Android 10+ blocks that for apps targeting 29 or higher; Termux does the same)
  * arm64 only ............ the toolchain is built for 64-bit ARM phones
  * extractNativeLibs ..... libaapt2.so must exist as a real file so it can be run
  * permissions ........... internet, install APKs, keep the screen on while installing
  * MainActivity .......... one method that tells Dart where Android put libaapt2.so
  * libaapt2.so ........... copied from android_overlay/ (the phone's own aapt2, same file WZH 2 APK uses)
"""
import os, re, shutil, sys

root = os.getcwd()
android = os.path.join(root, 'android')
if not os.path.isdir(android):
    sys.exit('No android/ folder here. Run `flutter create --org com.wzh --project-name wzh_flutter_ide .` first.')

def read(p):
    with open(p, encoding='utf-8') as f:
        return f.read()

def write(p, s):
    with open(p, 'w', encoding='utf-8') as f:
        f.write(s)

# ---- MainActivity (Kotlin or Java template: replace with Kotlin, keep the package)
main_dir = os.path.join(android, 'app', 'src', 'main')
act = None
for dp, _, fs in os.walk(main_dir):
    for f in fs:
        if f in ('MainActivity.kt', 'MainActivity.java'):
            act = os.path.join(dp, f)
if not act:
    sys.exit('MainActivity not found.')
pkg = re.search(r'^\s*package\s+([\w.]+)', read(act), re.M).group(1)
if act.endswith('.java'):
    os.remove(act)
    act = act[:-5] + '.kt'
write(act, f'''package {pkg}

import io.flutter.embedding.android.FlutterActivity
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.MethodChannel

class MainActivity : FlutterActivity() {{
    override fun configureFlutterEngine(flutterEngine: FlutterEngine) {{
        super.configureFlutterEngine(flutterEngine)
        MethodChannel(flutterEngine.dartExecutor.binaryMessenger, "wzh/ide").setMethodCallHandler {{ call, result ->
            if (call.method == "nativeLibraryDir") {{
                result.success(applicationContext.applicationInfo.nativeLibraryDir)
            }} else {{
                result.notImplemented()
            }}
        }}
    }}
}}
''')
print('MainActivity ->', act)

# ---- manifest
mf = os.path.join(main_dir, 'AndroidManifest.xml')
s = read(mf)
perms = ['android.permission.INTERNET', 'android.permission.REQUEST_INSTALL_PACKAGES', 'android.permission.WAKE_LOCK']
add = ''.join(f'    <uses-permission android:name="{p}"/>\n' for p in perms if p not in s)
if add:
    s = s.replace('<application', add + '    <application', 1)
def attr(s, name, val):
    if f'android:{name}=' in s:
        return re.sub(rf'android:{name}="[^"]*"', f'android:{name}="{val}"', s, count=1)
    return s.replace('<application', f'<application\n        android:{name}="{val}"', 1)
s = attr(s, 'extractNativeLibs', 'true')
s = attr(s, 'usesCleartextTraffic', 'true')
s = attr(s, 'label', 'WZH Flutter IDE')
write(mf, s)
print('AndroidManifest.xml patched')

# ---- gradle
app = os.path.join(android, 'app')
kts = os.path.join(app, 'build.gradle.kts')
grv = os.path.join(app, 'build.gradle')
if os.path.exists(kts):
    g = read(kts)
    g = re.sub(r'targetSdk\s*=\s*[^\n]+', 'targetSdk = 28', g)
    if 'abiFilters' not in g:
        g = re.sub(r'(defaultConfig\s*\{)', r'\1\n        ndk { abiFilters += listOf("arm64-v8a") }', g, count=1)
    if 'useLegacyPackaging' not in g:
        g = re.sub(r'(^android\s*\{)', r'\1\n    packaging { jniLibs { useLegacyPackaging = true } }', g, count=1, flags=re.M)
    write(kts, g)
    print('build.gradle.kts patched')
elif os.path.exists(grv):
    g = read(grv)
    g = re.sub(r'targetSdkVersion\s+[^\n]+', 'targetSdkVersion 28', g)
    if 'abiFilters' not in g:
        g = re.sub(r'(defaultConfig\s*\{)', r"\1\n        ndk { abiFilters 'arm64-v8a' }", g, count=1)
    if 'useLegacyPackaging' not in g:
        g = re.sub(r'(^android\s*\{)', r'\1\n    packagingOptions { jniLibs { useLegacyPackaging = true } }', g, count=1, flags=re.M)
    write(grv, g)
    print('build.gradle patched')
else:
    sys.exit('No android/app/build.gradle(.kts) found.')

# ---- aapt2
src = os.path.join(root, 'android_overlay', 'app', 'src', 'main', 'jniLibs', 'arm64-v8a', 'libaapt2.so')
dst_dir = os.path.join(main_dir, 'jniLibs', 'arm64-v8a')
os.makedirs(dst_dir, exist_ok=True)
shutil.copy(src, os.path.join(dst_dir, 'libaapt2.so'))
print('libaapt2.so copied')
print('Done. Now: flutter build apk --release')
