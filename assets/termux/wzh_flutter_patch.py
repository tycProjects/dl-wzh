# WZH 2 APK: project fixes for an on-phone Flutter build. Run inside the Debian system, in the Flutter project folder.
# This is the phone's copy of the steps the server's Flutter workflow (flutterBuilder.js) does before
# "flutter build apk": INTERNET and media permissions, AGP 9 flags, compileSdk 36 for plugins, the v2 embedding
# fix, jniLibs packaging, and a Gradle / AGP upgrade to Flutter's minimum. Every step is best effort: a problem
# prints a warning and the build goes on. Signing and Google services are not done here (the app signs the APK
# itself, and Google services need the server's secrets).
import glob
import os
import re
import subprocess
import sys

NL = chr(10)
SDK_FLUTTER = '/root/flutter'
SDK_ANDROID = '/root/android-sdk'


def log(m):
    print('[patch] ' + m, flush=True)


def rd(p):
    with open(p, encoding='utf-8') as f:
        return f.read()


def wr(p, s):
    with open(p, 'w', encoding='utf-8') as f:
        f.write(s)


def first(*ps):
    for p in ps:
        if os.path.isfile(p):
            return p
    return None


def step(fn):
    try:
        fn()
    except Exception as e:  # never fail the build because a fix could not be applied
        log('warning: %s failed (%s) - going on' % (fn.__name__, e))


PUBSPEC = rd('pubspec.yaml') if os.path.isfile('pubspec.yaml') else ''
MANIFEST = 'android/app/src/main/AndroidManifest.xml'
APP_GRADLE = first('android/app/build.gradle.kts', 'android/app/build.gradle')


# 1. permissions the release manifest needs ---------------------------------------------------------------
def permissions():
    if not os.path.isfile(MANIFEST):
        return
    s = rd(MANIFEST)
    o = s

    def add(marker, lines):
        nonlocal s
        if marker in s:
            return
        s = s.replace('<application', NL.join(lines) + NL + '    <application', 1)
        log('added ' + marker)

    # flutter create only declares INTERNET in debug/profile, so a release app has no network at all.
    add('android.permission.INTERNET', ['<uses-permission android:name="android.permission.INTERNET"/>'])
    if 'photo_manager' in PUBSPEC:
        add('READ_MEDIA_IMAGES', [
            '<uses-permission android:name="android.permission.READ_MEDIA_IMAGES"/>',
            '    <uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" android:maxSdkVersion="32"/>'])
    if 'open_filex' in PUBSPEC:
        add('REQUEST_INSTALL_PACKAGES', [
            '<uses-permission android:name="android.permission.REQUEST_INSTALL_PACKAGES"/>',
            '    <uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" android:maxSdkVersion="29"/>'])
    # AGP refuses extractNativeLibs set in the manifest; useLegacyPackaging in the build file replaces it.
    s = re.sub(' ?android:extractNativeLibs="true"', '', s)
    if s != o:
        wr(MANIFEST, s)


# 2. bundled native tools (jniLibs) must be extracted on the phone ----------------------------------------
def legacy_packaging():
    if not os.path.isdir('android/app/src/main/jniLibs') or not APP_GRADLE:
        return
    s = rd(APP_GRADLE)
    if 'useLegacyPackaging' in s:
        return
    kts = APP_GRADLE.endswith('.kts')
    val = 'useLegacyPackaging = true' if kts else 'useLegacyPackaging true'
    block = NL.join(['    packagingOptions {', '        jniLibs {', '            ' + val, '        }', '    }'])
    for a in ('android {' + NL, 'android{' + NL, 'android {', 'android{'):
        i = s.find(a)
        if i != -1:
            i += len(a)
            wr(APP_GRADLE, s[:i] + block + NL + s[i:])
            log('set jniLibs useLegacyPackaging')
            return
    log('warning: no "android {" block in ' + APP_GRADLE + ' - jniLibs packaging not changed')


# 3. the project's own Android scripts ---------------------------------------------------------------------
def project_scripts():
    if os.path.isfile('patch_android.py'):
        r = subprocess.run([sys.executable, 'patch_android.py'])
        log('patch_android.py ' + ('done' if r.returncode == 0 else 'failed - going on without it'))
    if os.path.isfile('tool/android_setup.py') and 'flutter_local_notifications' in PUBSPEC:
        r = subprocess.run([sys.executable, 'tool/android_setup.py', 'android'])
        log('tool/android_setup.py ' + ('done' if r.returncode == 0 else 'failed - going on without it'))


# 4. local.properties + AGP 9 compatibility flags ----------------------------------------------------------
def local_and_agp():
    os.makedirs('android', exist_ok=True)
    wr('android/local.properties', 'flutter.sdk=%s%ssdk.dir=%s%s' % (SDK_FLUTTER, NL, SDK_ANDROID, NL))
    gp = 'android/gradle.properties'
    s = rd(gp) if os.path.isfile(gp) else ''
    s = re.sub(r'(?m)^[ \t]*android\.(builtInKotlin|newDsl)[ \t]*=.*' + NL + '?', '', s)
    if s and not s.endswith(NL):
        s += NL
    s += NL + 'android.builtInKotlin=false' + NL + 'android.newDsl=false' + NL
    wr(gp, s)
    log('AGP 9 compatibility flags set')


# 5. compileSdk 36 for every plugin module -----------------------------------------------------------------
KTS_ROOT = '''// Added by apk-builder: force compileSdk 36 on all plugin modules.
subprojects {
    afterEvaluate {
        val ext = extensions.findByName("android")
        if (ext is com.android.build.gradle.BaseExtension) {
            ext.compileSdkVersion(36)
        }
    }
}

'''
GROOVY_ROOT = '''// Added by apk-builder: force compileSdk 36 on all plugin modules.
subprojects {
    afterEvaluate { project ->
        if (project.hasProperty('android')) {
            project.android.compileSdkVersion 36
        }
    }
}

'''
KTS_SETTINGS = '''
// Added by apk-builder: force compileSdk 36 on all plugin modules.
// No root android/build.gradle.kts in this project - hooked onto settings.gradle.kts instead.
gradle.subprojects {
    afterEvaluate {
        val ext = extensions.findByName("android")
        if (ext is com.android.build.gradle.BaseExtension) {
            ext.compileSdkVersion(36)
        }
    }
}
'''
GROOVY_SETTINGS = '''
// Added by apk-builder: force compileSdk 36 on all plugin modules.
// No root android/build.gradle in this project - hooked onto settings.gradle instead.
gradle.subprojects { project ->
    project.afterEvaluate {
        if (project.hasProperty("android")) {
            project.android.compileSdkVersion 36
        }
    }
}
'''


def compile_sdk():
    root = first('android/build.gradle.kts', 'android/build.gradle')
    if root:
        s = rd(root)
        if 'force compileSdk 36' not in s:
            wr(root, (KTS_ROOT if root.endswith('.kts') else GROOVY_ROOT) + s)
        log('compileSdk 36 forced in ' + root)
        return
    st = first('android/settings.gradle.kts', 'android/settings.gradle')
    if st:
        s = rd(st)
        if 'force compileSdk 36' not in s:
            wr(st, s + (KTS_SETTINGS if st.endswith('.kts') else GROOVY_SETTINGS))
        log('compileSdk 36 forced in ' + st)
        return
    log('warning: no android/build.gradle(.kts) or settings.gradle(.kts) - compileSdk 36 not forced')


# 6. old v1 embedding -> v2 ---------------------------------------------------------------------------------
def embedding():
    if not os.path.isfile(MANIFEST):
        log('warning: ' + MANIFEST + ' is missing')
        return
    WS = '[ \t\n\r]'
    SP = '[ \t]'
    orig = rd(MANIFEST)
    with open(MANIFEST, encoding='utf-8-sig') as f:
        m = f.read()
    m = re.sub('^' + WS + '+(?=<[?]xml)', '', m)
    live = re.sub('<!--.*?-->', '', m, flags=re.S)
    tags = re.findall('<meta-data[^>]*>', live, flags=re.S)
    emb = [t for t in tags if re.search('android:name *= *.flutterEmbedding.', t)]
    has_v2 = len(emb) > 0 and all(re.search('android:value *= *.2.', t) for t in emb)
    if not has_v2:
        m = re.sub(SP + '*<meta-data[^>]*flutterEmbedding[^>]*>' + SP + '*' + NL + '?', '', m, flags=re.S)
        m = re.sub('<application([^>]*?)' + WS + '*/>', lambda x: '<application' + x.group(1) + '>' + NL + '    </application>', m, count=1)
        block = '    <meta-data' + NL + '        android:name="flutterEmbedding"' + NL + '        android:value="2" />' + NL + '    </application>'
        m = re.sub(SP + '*</application' + WS + '*>', lambda x: block, m, count=1)
    if 'xmlns:android' not in m:
        m = re.sub('<manifest', lambda x: '<manifest xmlns:android="http://schemas.android.com/apk/res/android"', m, count=1)
    m = re.sub('(android:name *= *.)io[.]flutter[.]app[.]FlutterApplication', lambda x: x.group(1) + 'android.app.Application', m)
    if m != orig:
        wr(MANIFEST, m)
        log('declared flutterEmbedding=2')
    files = glob.glob('android/app/src/main/**/*.kt', recursive=True) + glob.glob('android/app/src/main/**/*.java', recursive=True)
    for f in files:
        c = rd(f)
        if 'io.flutter.app.' not in c and 'registerWith(this)' not in c.replace(' ', ''):
            continue
        o = c
        c = c.replace('io.flutter.app.FlutterFragmentActivity', 'io.flutter.embedding.android.FlutterFragmentActivity')
        c = c.replace('io.flutter.app.FlutterActivity', 'io.flutter.embedding.android.FlutterActivity')
        c = c.replace('io.flutter.app.FlutterApplication', 'android.app.Application')
        c = c.replace('FlutterApplication', 'Application')
        c = ''.join(l for l in c.splitlines(True) if 'registerWith(this)' not in l.replace(' ', ''))
        if c != o:
            wr(f, c)
            log('moved ' + f + ' to the v2 embedding')


# 7. Gradle wrapper and AGP up to Flutter's minimum ---------------------------------------------------------
def vt(v):
    n = [int(x) for x in re.findall('[0-9]+', v)[:3]]
    return tuple(n + [0] * (3 - len(n)))


def gradle_upgrade():
    GRADLE_TARGET = '8.14.3'
    AGP_TARGET = '8.11.1'
    wp = 'android/gradle/wrapper/gradle-wrapper.properties'
    if not os.path.isfile(wp):
        log('warning: ' + wp + ' not found')
    else:
        s = rd(wp)
        m = re.search('gradle-([0-9][0-9.]*[0-9])-(bin|all)[.]zip', s)
        if not m:
            log('warning: could not read the Gradle version')
        elif vt(m.group(1)) >= vt(GRADLE_TARGET):
            log('Gradle wrapper is already ' + m.group(1))
        else:
            s = s.replace(m.group(0), 'gradle-' + GRADLE_TARGET + '-' + m.group(2) + '.zip')
            s = re.sub('(?m)^distributionSha256Sum=.*' + NL + '?', '', s)
            wr(wp, s)
            log('Gradle wrapper upgraded: ' + m.group(1) + ' -> ' + GRADLE_TARGET)

    def bump(m):
        cur = m.group(2)
        if vt(cur)[0] == 8 and vt(cur) < vt(AGP_TARGET):
            log('AGP ' + cur + ' -> ' + AGP_TARGET)
            return m.group(1) + AGP_TARGET
        return m.group(0)

    for f in ('android/settings.gradle.kts', 'android/settings.gradle', 'android/build.gradle.kts', 'android/build.gradle'):
        if not os.path.isfile(f):
            continue
        t = rd(f)
        o = t
        t = re.sub('(com[.]android[.](?:application|library).{0,60}?version.{0,6}?)([0-9]+[.][0-9]+[.][0-9]+)', bump, t)
        t = re.sub('(com[.]android[.]tools[.]build:gradle:)([0-9]+[.][0-9]+[.][0-9]+)', bump, t)
        if t != o:
            wr(f, t)
            log('updated AGP version in ' + f)


for fn in (permissions, legacy_packaging, project_scripts, local_and_agp, compile_sdk, embedding, gradle_upgrade):
    step(fn)
log('done')
