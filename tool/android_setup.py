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


def patch_media_permissions():
    # The chat gallery lists videos too (Android 13+ needs its own permission for that).
    s = read(manifest)
    m = re.search(r'<application\b', s)
    add = ''
    for perm in ('READ_MEDIA_VIDEO', 'READ_MEDIA_IMAGES'):
        if 'android.permission.' + perm not in s:
            add += '<uses-permission android:name="android.permission.%s"/>\n    ' % perm
    if add and m:
        write(manifest, s[:m.start()] + add + s[m.start():])
        print('manifest: added media read permissions')
    else:
        print('manifest: media permissions already there')


def patch_cleartext():
    s = read(manifest)
    if 'usesCleartextTraffic' in s:
        print('manifest: cleartext already set')
        return
    s = re.sub(r'<application\b', '<application android:usesCleartextTraffic="true"', s, count=1)
    write(manifest, s)
    print('manifest: allowed http://127.0.0.1 for the code preview')


def patch_large_heap():
    # The system file picker hands the app the chosen file; the picker plugin copies it inside the
    # app's Java heap, and a 50 MB APK ran the normal heap out of memory (the app just closed).
    s = read(manifest)
    if 'largeHeap' in s:
        print('manifest: largeHeap already set')
        return
    s = re.sub(r'<application\b', '<application android:largeHeap="true"', s, count=1)
    write(manifest, s)
    print('manifest: asked Android for a bigger memory heap')


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
                } else if (call.method == "installedVersion") {
                    // Store button: is this package installed? Returns "versionName|versionCode", or null.
                    val pkg = call.argument<String>("pkg") ?: ""
                    try {
                        val info = applicationContext.packageManager.getPackageInfo(pkg, 0)
                        val code = if (android.os.Build.VERSION.SDK_INT >= 28) info.longVersionCode else info.versionCode.toLong()
                        result.success((info.versionName ?: "") + "|" + code)
                    } catch (e: Exception) {
                        result.success(null)
                    }
                } else if (call.method == "launchApp") {
                    val pkg = call.argument<String>("pkg") ?: ""
                    val intent = applicationContext.packageManager.getLaunchIntentForPackage(pkg)
                    if (intent != null) {
                        intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                        startActivity(intent)
                        result.success(true)
                    } else {
                        result.success(false)
                    }
                } else {
                    result.notImplemented()
                }
            }
    }
"""


OLD_CHANNEL = '''
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
'''


EXTRA_BRANCHES = """else if (call.method == "installedVersion") {
                    val pkg = call.argument<String>("pkg") ?: ""
                    try {
                        val info = applicationContext.packageManager.getPackageInfo(pkg, 0)
                        val code = if (android.os.Build.VERSION.SDK_INT >= 28) info.longVersionCode else info.versionCode.toLong()
                        result.success((info.versionName ?: "") + "|" + code)
                    } catch (e: Exception) {
                        result.success(null)
                    }
                } else if (call.method == "launchApp") {
                    val pkg = call.argument<String>("pkg") ?: ""
                    val intent = applicationContext.packageManager.getLaunchIntentForPackage(pkg)
                    if (intent != null) {
                        intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                        startActivity(intent)
                        result.success(true)
                    } else {
                        result.success(false)
                    }
                }"""


def patch_queries():
    # Android 11+ hides other apps unless declared. Store apps all have a launcher icon, so this is enough to see them (no QUERY_ALL_PACKAGES).
    s = read(manifest)
    if 'android.intent.category.LAUNCHER' in s.split('<application')[0] and '<queries' in s:
        print('manifest: queries already there')
        return
    block = '<queries>\n        <intent>\n            <action android:name="android.intent.action.MAIN"/>\n            <category android:name="android.intent.category.LAUNCHER"/>\n        </intent>\n    </queries>\n    '
    m = re.search(r'<application\b', s)
    if not m:
        sys.exit('manifest: no <application> tag found')
    write(manifest, s[:m.start()] + block + s[m.start():])
    print('manifest: added queries so the Store can see which apps are installed')


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
        if 'installedVersion' in s:
            print('MainActivity: channel already there')
        elif OLD_CHANNEL in s:
            write(found, s.replace(OLD_CHANNEL, CHANNEL, 1))
            print('MainActivity: channel upgraded (installed / open)')
        else:
            # The channel was wired by the builder with only nativeLibraryDir: add the two Store methods before
            # its final "else { result.notImplemented() }" branch. Never fails the build: the Store button just stays "Install".
            m = re.search(r'\}\s*else\s*\{\s*result\.notImplemented\(\)', s)
            if m:
                write(found, s[:m.start()] + '} ' + EXTRA_BRANCHES + ' else {\n                    result.notImplemented()' + s[m.end():])
                print('MainActivity: channel upgraded (installed / open)')
            else:
                print('MainActivity: could not add installedVersion / launchApp; the Store button will stay on Install')
        return
    if found.endswith('.java'):
        sys.exit('MainActivity.java: delete it and let Flutter make MainActivity.kt, or add the channel by hand (ANDROID_SETUP_AAPT2.md)')
    m = re.search(r'class\s+MainActivity\s*:\s*FlutterActivity\(\)\s*(\{\s*\})?', s)
    if not m:
        sys.exit('MainActivity.kt: unexpected shape; add the channel by hand (ANDROID_SETUP_AAPT2.md)')
    write(found, s[:m.start()] + 'class MainActivity : FlutterActivity() {' + CHANNEL + '}' + s[m.end():])
    print('MainActivity: added the wzh/native_tools channel')


TERMUX_CHANNEL = """
        // On-phone builds: lets the app ask Termux (terminal app) to run a build script.
        io.flutter.plugin.common.MethodChannel(flutterEngine.dartExecutor.binaryMessenger, "wzh/termux")
            .setMethodCallHandler { call, result ->
                val termuxPerm = "com.termux.permission.RUN_COMMAND"
                if (call.method == "installed") {
                    try {
                        applicationContext.packageManager.getPackageInfo("com.termux", 0)
                        result.success(true)
                    } catch (e: Exception) {
                        result.success(false)
                    }
                } else if (call.method == "hasPermission") {
                    var ok = true
                    if (android.os.Build.VERSION.SDK_INT >= 23) {
                        ok = checkSelfPermission(termuxPerm) == android.content.pm.PackageManager.PERMISSION_GRANTED
                    }
                    result.success(ok)
                } else if (call.method == "requestPermission") {
                    if (android.os.Build.VERSION.SDK_INT >= 23) {
                        requestPermissions(arrayOf(termuxPerm), 7731)
                    }
                    result.success(true)
                } else if (call.method == "openTermux") {
                    val launch = applicationContext.packageManager.getLaunchIntentForPackage("com.termux")
                    if (launch != null) {
                        launch.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                        startActivity(launch)
                    }
                    result.success(launch != null)
                } else if (call.method == "run") {
                    val args = call.argument<List<String>>("args") ?: emptyList<String>()
                    val background = call.argument<Boolean>("background") ?: true
                    try {
                        val i = android.content.Intent("com.termux.RUN_COMMAND")
                        i.setClassName("com.termux", "com.termux.app.RunCommandService")
                        i.putExtra("com.termux.RUN_COMMAND_PATH", "/data/data/com.termux/files/usr/bin/bash")
                        i.putExtra("com.termux.RUN_COMMAND_ARGUMENTS", args.toTypedArray())
                        i.putExtra("com.termux.RUN_COMMAND_WORKDIR", "/data/data/com.termux/files/home")
                        i.putExtra("com.termux.RUN_COMMAND_BACKGROUND", background)
                        if (!background) i.putExtra("com.termux.RUN_COMMAND_SESSION_ACTION", "0")
                        if (android.os.Build.VERSION.SDK_INT >= 26) startForegroundService(i) else startService(i)
                        result.success(null)
                    } catch (e: Exception) {
                        result.success(e.javaClass.simpleName + ": " + (e.message ?: ""))
                    }
                } else {
                    result.notImplemented()
                }
            }
"""


def patch_termux_manifest():
    # On-phone builds: permission to run commands in Termux, and visibility of the Termux package (Android 11+).
    s = read(manifest)
    changed = False
    if 'com.termux.permission.RUN_COMMAND' not in s:
        m = re.search(r'<application\b', s)
        if not m:
            sys.exit('manifest: no <application> tag found')
        s = s[:m.start()] + '<uses-permission android:name="com.termux.permission.RUN_COMMAND"/>\n    ' + s[m.start():]
        changed = True
    if 'android.permission.REQUEST_INSTALL_PACKAGES' not in s:
        m = re.search(r'<application\b', s)
        s = s[:m.start()] + '<uses-permission android:name="android.permission.REQUEST_INSTALL_PACKAGES"/>\n    ' + s[m.start():]
        changed = True
    if 'android:name="com.termux"' not in s:
        if '</queries>' in s:
            s = s.replace('</queries>', '    <package android:name="com.termux"/>\n    </queries>', 1)
        else:
            m = re.search(r'<application\b', s)
            s = s[:m.start()] + '<queries>\n        <package android:name="com.termux"/>\n    </queries>\n    ' + s[m.start():]
        changed = True
    if changed:
        write(manifest, s)
        print('manifest: added the Termux permission and package query')
    else:
        print('manifest: Termux permission already there')


def patch_termux_channel():
    found = None
    for d, _, files in os.walk(os.path.join(root, 'app', 'src', 'main')):
        for f in files:
            if f == 'MainActivity.kt':
                found = os.path.join(d, f)
    if not found:
        print('MainActivity: no MainActivity.kt, so no Termux channel (on-phone builds stay off)')
        return
    s = read(found)
    if 'wzh/termux' in s:
        print('MainActivity: Termux channel already there')
        return
    m = re.search(r'super\.configureFlutterEngine\(flutterEngine\)', s)
    if not m:
        print('MainActivity: could not add the Termux channel; on-phone builds stay off')
        return
    write(found, s[:m.end()] + '\n' + TERMUX_CHANNEL + s[m.end():])
    print('MainActivity: added the wzh/termux channel')


patch_manifest()
patch_media_permissions()
patch_cleartext()
patch_large_heap()
patch_queries()
patch_gradle()
patch_extract_libs()
patch_main_activity()
patch_termux_manifest()
patch_termux_channel()
print('android setup done')
