#!/usr/bin/env python3
"""Android tweaks for UHD Enhancer. The apk builder runs this from the project root
right after `flutter create`. Safe to run twice; any problem only prints a warning."""
import os, re

MANIFEST = os.path.join('android', 'app', 'src', 'main', 'AndroidManifest.xml')

def main():
    if not os.path.isfile(MANIFEST):
        print('patch_android: no AndroidManifest.xml, skipping')
        return
    s = open(MANIFEST, encoding='utf-8').read()
    # Show a proper app name under the icon.
    s = re.sub(r'android:label="[^"]*"', 'android:label="UHD Enhancer"', s, count=1)
    # Saving to the gallery on Android 9 and older needs the legacy write permission.
    if 'WRITE_EXTERNAL_STORAGE' not in s:
        tag = '<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" android:maxSdkVersion="29"/>\n    '
        m = re.search(r'<application\b', s)
        if m:
            s = s[:m.start()] + tag + s[m.start():]
    open(MANIFEST, 'w', encoding='utf-8').write(s)
    print('patch_android: app label and storage permission set')

try:
    main()
except Exception as e:
    print('patch_android: warning -', e)
