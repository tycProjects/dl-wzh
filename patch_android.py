#!/usr/bin/env python3
"""Android setup for UHD Enhancer. The apk builder runs this from the project root right
after `flutter create` and before `flutter pub get`, on a machine WITH internet.

It (1) sets the app name and a storage permission, and (2) downloads the real AI engine
(Real-ESRGAN via ncnn) and its model, then bundles them. Every step is best-effort: if
anything fails it prints a warning and the app still builds, using its built-in engine."""
import os, re, shutil, tempfile, urllib.request, zipfile

MANIFEST = os.path.join('android', 'app', 'src', 'main', 'AndroidManifest.xml')
ASSETS_URLS = [
    'https://github.com/tumuyan/RealSR-NCNN-Android/releases/download/1.11.1/assets.zip',
    'https://github.com/tumuyan/RealSR-NCNN-Android/releases/download/1.7.13-1/assets.zip',
]


def patch_manifest():
    if not os.path.isfile(MANIFEST):
        print('patch_android: no AndroidManifest.xml, skipping manifest')
        return
    s = open(MANIFEST, encoding='utf-8').read()
    s = re.sub(r'android:label="[^"]*"', 'android:label="UHD Enhancer"', s, count=1)
    if 'WRITE_EXTERNAL_STORAGE' not in s:
        tag = '<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" android:maxSdkVersion="29"/>\n    '
        m = re.search(r'<application\b', s)
        if m:
            s = s[:m.start()] + tag + s[m.start():]
    open(MANIFEST, 'w', encoding='utf-8').write(s)
    print('patch_android: app label and storage permission set')


def legacy_packaging():
    # Android must extract the AI program to disk so the app can run it.
    for f in ('android/app/build.gradle.kts', 'android/app/build.gradle'):
        if not os.path.isfile(f):
            continue
        s = open(f, encoding='utf-8').read()
        if 'useLegacyPackaging' in s:
            print('patch_android: useLegacyPackaging already set')
            return
        name = 'packaging' if f.endswith('.kts') else 'packagingOptions'
        ins = '\n    %s {\n        jniLibs {\n            useLegacyPackaging = true\n        }\n    }\n' % name
        s2 = re.sub(r'(\bandroid\s*\{)', lambda m: m.group(1) + ins, s, count=1)
        if s2 != s:
            open(f, 'w', encoding='utf-8').write(s2)
            print('patch_android: enabled useLegacyPackaging in ' + f)
        return


def download(dest):
    for url in ASSETS_URLS:
        try:
            print('patch_android: downloading ' + url)
            req = urllib.request.Request(url, headers={'User-Agent': 'Mozilla/5.0'})
            with urllib.request.urlopen(req, timeout=180) as r, open(dest, 'wb') as f:
                shutil.copyfileobj(r, f)
            if zipfile.is_zipfile(dest):
                return True
        except Exception as e:
            print('patch_android: download failed (%s)' % e)
    return False


def elf_abi(path):
    with open(path, 'rb') as f:
        h = f.read(20)
    if h[:4] != b'\x7fELF':
        return None
    machine = h[18] | (h[19] << 8)
    if machine == 183:
        return 'arm64-v8a'
    if machine == 40:
        return 'armeabi-v7a'
    return None


def setup_ai():
    tmp = tempfile.mkdtemp()
    z = os.path.join(tmp, 'assets.zip')
    if not download(z):
        print('::warning::Could not download the AI engine - building without it')
        return
    out = os.path.join(tmp, 'x')
    zipfile.ZipFile(z).extractall(out)

    placed = 0
    for dp, _, files in os.walk(out):
        if 'realsr-ncnn' not in files:
            continue
        exe = os.path.join(dp, 'realsr-ncnn')
        abi = elf_abi(exe)
        if not abi:
            continue
        dest = os.path.join('android', 'app', 'src', 'main', 'jniLibs', abi)
        os.makedirs(dest, exist_ok=True)
        shutil.copyfile(exe, os.path.join(dest, 'librealsr_exec.so'))
        for lib in ('libncnn.so', 'libomp.so'):
            src = os.path.join(dp, lib)
            if os.path.isfile(src):
                shutil.copyfile(src, os.path.join(dest, lib))
        for fn in os.listdir(dest):
            os.chmod(os.path.join(dest, fn), 0o755)
        print('patch_android: AI engine placed for ' + abi + ': ' + ', '.join(sorted(os.listdir(dest))))
        placed += 1
    if not placed:
        print('::warning::realsr-ncnn program not found in assets.zip - building without AI')
        return

    # A 4x model: a folder holding x4.param + x4.bin (prefer the JPEG-clean one).
    cands = [dp for dp, _, fs in os.walk(out) if 'x4.param' in fs and 'x4.bin' in fs]
    if not cands:
        print('::warning::No x4 model found in assets.zip - building without AI')
        return
    cands.sort(key=lambda d: (0 if 'jpeg' in os.path.basename(d).lower() else 1, d))
    pick = cands[0]
    os.makedirs(os.path.join('assets', 'ai'), exist_ok=True)
    shutil.copyfile(os.path.join(pick, 'x4.param'), os.path.join('assets', 'ai', 'model.param'))
    shutil.copyfile(os.path.join(pick, 'x4.bin'), os.path.join('assets', 'ai', 'model.bin'))
    print('patch_android: model copied from ' + os.path.basename(pick))
    shutil.rmtree(tmp, ignore_errors=True)


for step in (patch_manifest, legacy_packaging, setup_ai):
    try:
        step()
    except Exception as e:
        print('::warning::patch_android step %s failed: %s' % (step.__name__, e))
