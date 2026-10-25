#!/usr/bin/env python3
"""Prepares the generated Android project for build notifications.

Run it once after the android/ folder exists and before `flutter build apk`:

    python3 tool/android_setup.py android

It (1) adds the notification permission to AndroidManifest.xml, (2) turns on core library
desugaring in android/app/build.gradle(.kts), which the notification plugin needs, and (3) sets up the
bundled aapt2: Android must EXTRACT libaapt2.so to disk (useLegacyPackaging) so it can be run, and
MainActivity needs the "wzh/native_tools" channel that tells Dart where that folder is.
It is safe to run more than once.
"""
import os
import re
import sys

root = sys.argv[1] if len(sys.argv) > 1 else 'android'
manifest = os.path.join(root, 'app', 'src', 'main', 'AndroidManifest.xml')
DESUGAR = 'com.android.tools:desugar_jdk_libs:2.1.4'


def read(p):
    with open(p, encoding='utf-8') as f:
        return f.read()


def write(p, s):
    with open(p, 'w', encoding='utf-8') as f:
        f.write(s)


def patch_manifest():
    s = read(manifest)
    if 'android.permission.POST_NOTIFICATIONS' in s:
        print('manifest: permission already there')
        return
    tag = '<uses-permission android:name="android.permission.POST_NOTIFICATIONS"/>\n    '
    m = re.search(r'<application\b', s)
    if not m:
        sys.exit('manifest: no <application> tag found')
    write(manifest, s[:m.start()] + tag + s[m.start():])
    print('manifest: added POST_NOTIFICATIONS')


def patch_cleartext():
    s = read(manifest)
    if 'usesCleartextTraffic' in s:
        print('manifest: cleartext already set')
        return
    s = re.sub(r'<application\b', '<application android:usesCleartextTraffic="true"', s, count=1)
    write(manifest, s)
    print('manifest: allowed http://127.0.0.1 for the code preview')


def patch_gradle():
    kts = os.path.join(root, 'app', 'build.gradle.kts')
    groovy = os.path.join(root, 'app', 'build.gradle')
    if os.path.exists(kts):
        path, is_kts = kts, True
    elif os.path.exists(groovy):
        path, is_kts = groovy, False
    else:
        sys.exit('gradle: no app/build.gradle(.kts) found')
    s = read(path)
    flag = 'isCoreLibraryDesugaringEnabled = true' if is_kts else 'coreLibraryDesugaringEnabled true'
    dep = ('    coreLibraryDesugaring("%s")' if is_kts else "    coreLibraryDesugaring '%s'") % DESUGAR
    if re.search(r'oreLibraryDesugaringEnabled', s):
        print('gradle: desugaring flag already on')
    else:
        m = re.search(r'compileOptions\s*\{', s)
        if m:
            s = s[:m.end()] + '\n        ' + flag + s[m.end():]
        else:
            m = re.search(r'^android\s*\{', s, re.M)
            if not m:
                sys.exit('gradle: no android { } block found in ' + path)
            s = s[:m.end()] + '\n    compileOptions {\n        ' + flag + '\n    }' + s[m.end():]
        print('gradle: turned on desugaring in ' + os.path.basename(path))
    if 'desugar_jdk_libs' in s:
        print('gradle: desugar library already there')
    else:
        m = re.search(r'^dependencies\s*\{', s, re.M)
        if m:
            s = s[:m.end()] + '\n' + dep + s[m.end():]
        else:
            s = s.rstrip('\n') + '\n\ndependencies {\n' + dep + '\n}\n'
        print('gradle: added the desugar library')
    write(path, s)


def patch_extract_libs():
    kts = os.path.join(root, 'app', 'build.gradle.kts')
    groovy = os.path.join(root, 'app', 'build.gradle')
    path, is_kts = (kts, True) if os.path.exists(kts) else (groovy, False)
    s = read(path)
    if 'useLegacyPackaging' in s:
        print('gradle: native libs already extracted')
        return
    block = ('\n    packaging {\n        jniLibs {\n            useLegacyPackaging = true\n        }\n    }'
             if is_kts else
             '\n    packaging {\n        jniLibs {\n            useLegacyPackaging true\n        }\n    }')
    m = re.search(r'^android\s*\{', s, re.M)
    if not m:
        sys.exit('gradle: no android { } block found in ' + path)
    write(path, s[:m.end()] + block + s[m.end():])
    print('gradle: native libs are now extracted (aapt2 can run)')


CHANNEL = """
    override fun configureFlutterEngine(flutterEngine: io.flutter.embedding.engine.FlutterEngine) {
        super.configureFlutterEngine(flutterEngine)
        io.flutter.plugin.common.MethodChannel(flutterEngine.dartExecutor.binaryMessenger, "wzh/native_tools")
            .setMethodCallHandler { call, result ->
                if (call.method == "nativeLibraryDir") {
                    result.success(applicationContext.applicationInfo.nativeLibraryDir)
                } else {
                    result.notImplemented()
                }
            }
    }
"""


def patch_main_activity():
    found = None
    for d, _, files in os.walk(os.path.join(root, 'app', 'src', 'main')):
        for f in files:
            if f in ('MainActivity.kt', 'MainActivity.java'):
                found = os.path.join(d, f)
    if not found:
        sys.exit('MainActivity: not found under app/src/main')
    s = read(found)
    if 'wzh/native_tools' in s:
        print('MainActivity: channel already there')
        return
    if found.endswith('.java'):
        sys.exit('MainActivity.java: delete it and let Flutter make MainActivity.kt, or add the channel by hand (ANDROID_SETUP_AAPT2.md)')
    m = re.search(r'class\s+MainActivity\s*:\s*FlutterActivity\(\)\s*(\{\s*\})?', s)
    if not m:
        sys.exit('MainActivity.kt: unexpected shape; add the channel by hand (ANDROID_SETUP_AAPT2.md)')
    write(found, s[:m.start()] + 'class MainActivity : FlutterActivity() {' + CHANNEL + '}' + s[m.end():])
    print('MainActivity: added the wzh/native_tools channel')


patch_manifest()
patch_cleartext()
patch_gradle()
patch_extract_libs()
patch_main_activity()
print('android setup done')
