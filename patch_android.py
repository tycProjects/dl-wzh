#!/usr/bin/env python3
"""Run once after `flutter create ...` (and in CI before building).
Adds the gallery/media permissions the in-app picker needs and sets minSdk 24 for ffmpeg_kit."""
import pathlib, re

root = pathlib.Path(__file__).resolve().parent
man = root / 'android/app/src/main/AndroidManifest.xml'
PERMS = [
    '<uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" android:maxSdkVersion="32"/>',
    '<uses-permission android:name="android.permission.READ_MEDIA_IMAGES"/>',
    '<uses-permission android:name="android.permission.READ_MEDIA_VIDEO"/>',
    '<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" android:maxSdkVersion="29"/>',
]
if man.exists():
    t = man.read_text()
    add = ''.join('\n    ' + p for p in PERMS if re.search(r'android:name="([^"]+)"', p).group(1) not in t)
    if add:
        t = re.sub(r'(<manifest[^>]*>)', lambda m: m.group(1) + add, t, count=1)
        man.write_text(t)
        print('manifest: permissions added')
else:
    print('no AndroidManifest.xml found - run flutter create first')

for name, repl in (('build.gradle.kts', 'minSdk = 24'), ('build.gradle', 'minSdkVersion 24')):
    f = root / 'android/app' / name
    if f.exists():
        t = f.read_text()
        n = re.sub(r'minSdk(Version)?\s*=?\s*flutter\.minSdkVersion', repl, t)
        if n != t:
            f.write_text(n)
            print(name + ': minSdk set to 24')

# --- app name + launcher icon -------------------------------------------------
LABEL = 'Kineflux'
if man.exists():
    t = man.read_text()
    n = re.sub(r'android:label="[^"]*"', 'android:label="%s"' % LABEL, t, count=1)
    if n != t:
        man.write_text(n)
        print('manifest: app name set to ' + LABEL)

import shutil
res_src = root / 'tool/android_res'
res_dst = root / 'android/app/src/main/res'
if res_src.exists() and res_dst.exists():
    shutil.copytree(res_src, res_dst, dirs_exist_ok=True)
    print('launcher icon installed')
