/// Writes the Quick template's AndroidManifest.xml as plain text, for aapt2 to compile.
///
/// This mirrors the manifest inside assets/quick/template.apk one-to-one (same activity class,
/// same providers, same attributes) -- only the package, label, version, orientation,
/// permissions and deep-link scheme change. That matters: the template's classes.dex is
/// compiled against com.template.webapp.MainActivity and a few resources, so a manifest
/// that renames the activity or drops the providers makes the app crash on launch.

/// Class names inside the template's classes.dex never change, whatever package the app gets.
const templateActivity = 'com.template.webapp.MainActivity';

/// Resources of the template that its manifest points at. They keep their real ids (the
/// template's own resources.arsc stays in the APK), so aapt2 is told the exact ids with
/// --stable-ids. Names and ids were read out of assets/quick/template.apk's resources.arsc.
const templateStableIds = <String, int>{
  'style/Theme.GeneratedApp': 0x7f0e010a,
  'mipmap/ic_launcher': 0x7f0c0000,
  'xml/file_paths': 0x7f100000,
  'xml/shortcuts': 0x7f100001,
};

/// Permissions the template declares with a limit, kept as the template has them.
const _maxSdk = <String, int>{
  'android.permission.WRITE_EXTERNAL_STORAGE': 28,
  'android.permission.BLUETOOTH': 30,
  'android.permission.BLUETOOTH_ADMIN': 30,
};

String xmlEscape(String s) => s
    .replaceAll('&', '&amp;')
    .replaceAll('<', '&lt;')
    .replaceAll('>', '&gt;')
    .replaceAll('"', '&quot;')
    .replaceAll("'", '&apos;');

String buildManifest({
  required String packageName,
  required String appName,
  required String versionName,
  required int versionCode,
  required String orientation, // 'auto' | 'portrait' | 'landscape'
  required Set<String> permissionIds,
  required Map<String, List<String>> optionalPerms, // _optionalPerms from quick_builder.dart
  required bool allowInternet,
  String appLinkHost = '',
  String deepLinkScheme = '',
}) {
  final perms = StringBuffer();
  void perm(String name) {
    final m = _maxSdk[name];
    perms.writeln('    <uses-permission android:name="$name"${m == null ? '' : ' android:maxSdkVersion="$m"'} />');
  }

  if (allowInternet) perm('android.permission.INTERNET');
  perm('android.permission.ACCESS_NETWORK_STATE');
  for (final id in permissionIds) {
    for (final p in optionalPerms[id] ?? const <String>[]) {
      perm(p);
    }
  }
  // Always present in the template (notifications, installing downloaded APKs).
  perm('android.permission.POST_NOTIFICATIONS');
  perm('android.permission.REQUEST_INSTALL_PACKAGES');
  perms.writeln('    <uses-feature android:name="android.hardware.camera" android:required="false" />');
  perms.writeln('    <uses-feature android:name="android.hardware.camera.autofocus" android:required="false" />');
  perms.writeln('    <uses-feature android:name="android.hardware.microphone" android:required="false" />');
  perms.writeln('    <uses-feature android:name="android.hardware.location" android:required="false" />');
  // androidx.core registers receivers with this permission; it must exist under the NEW package name.
  perms.writeln('    <permission android:name="$packageName.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION" android:protectionLevel="signature" />');
  perms.writeln('    <uses-permission android:name="$packageName.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION" />');

  final orientationAttr = switch (orientation) {
    'portrait' => 'android:screenOrientation="portrait"',
    'landscape' => 'android:screenOrientation="landscape"',
    _ => '',
  };

  final links = StringBuffer();
  if (deepLinkScheme.isNotEmpty) {
    links.writeln('''
        <intent-filter>
          <action android:name="android.intent.action.VIEW" />
          <category android:name="android.intent.category.DEFAULT" />
          <category android:name="android.intent.category.BROWSABLE" />
          <data android:scheme="${xmlEscape(deepLinkScheme)}" />
        </intent-filter>''');
  }
  if (appLinkHost.isNotEmpty) {
    links.writeln('''
        <intent-filter android:autoVerify="true">
          <action android:name="android.intent.action.VIEW" />
          <category android:name="android.intent.category.DEFAULT" />
          <category android:name="android.intent.category.BROWSABLE" />
          <data android:scheme="https" android:host="${xmlEscape(appLinkHost)}" />
        </intent-filter>''');
  }

  return '''<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
    package="$packageName"
    android:versionCode="$versionCode"
    android:versionName="${xmlEscape(versionName)}">

$perms
    <application
        android:theme="@style/Theme.GeneratedApp"
        android:label="${xmlEscape(appName)}"
        android:icon="@mipmap/ic_launcher"
        android:roundIcon="@mipmap/ic_launcher"
        android:allowBackup="true"
        android:hardwareAccelerated="true"
        android:extractNativeLibs="true"
        android:usesCleartextTraffic="true"
        android:appComponentFactory="androidx.core.app.CoreComponentFactory"
        android:enableOnBackInvokedCallback="true">
        <activity
            android:name="$templateActivity"
            android:exported="true"
            android:launchMode="singleTask"
            $orientationAttr
            android:configChanges="keyboard|keyboardHidden|navigation|orientation|screenLayout|uiMode|screenSize|smallestScreenSize|density|layoutDirection|fontScale"
            android:windowSoftInputMode="adjustResize|stateHidden"
            android:resizeableActivity="true">
          <intent-filter>
            <action android:name="android.intent.action.MAIN" />
            <category android:name="android.intent.category.LAUNCHER" />
          </intent-filter>
$links
          <intent-filter>
            <action android:name="android.intent.action.SEND" />
            <category android:name="android.intent.category.DEFAULT" />
            <data android:mimeType="text/plain" />
          </intent-filter>
          <meta-data android:name="android.app.shortcuts" android:resource="@xml/shortcuts" />
        </activity>
        <provider
            android:name="androidx.core.content.FileProvider"
            android:authorities="$packageName.fileprovider"
            android:exported="false"
            android:grantUriPermissions="true">
          <meta-data android:name="android.support.FILE_PROVIDER_PATHS" android:resource="@xml/file_paths" />
        </provider>
        <provider
            android:name="androidx.startup.InitializationProvider"
            android:authorities="$packageName.androidx-startup"
            android:exported="false">
          <meta-data android:name="androidx.emoji2.text.EmojiCompatInitializer" android:value="androidx.startup" />
          <meta-data android:name="androidx.lifecycle.ProcessLifecycleInitializer" android:value="androidx.startup" />
        </provider>
    </application>
</manifest>
''';
}

/// Throw-away resources that only exist so aapt2 can resolve the manifest's @references.
/// Their ids are pinned to the template's real ones (templateStableIds); the resources.arsc
/// aapt2 writes from them is discarded -- only its binary AndroidManifest.xml is kept.
const placeholderFiles = <String, String>{
  'values/styles.xml':
      '<?xml version="1.0" encoding="utf-8"?>\n<resources>\n  <style name="Theme.GeneratedApp" parent="@android:style/Theme.Material.Light.NoActionBar" />\n</resources>\n',
  'mipmap-anydpi-v26/ic_launcher.xml':
      '<?xml version="1.0" encoding="utf-8"?>\n<shape xmlns:android="http://schemas.android.com/apk/res/android" />\n',
  'xml/file_paths.xml':
      '<?xml version="1.0" encoding="utf-8"?>\n<paths xmlns:android="http://schemas.android.com/apk/res/android" />\n',
  'xml/shortcuts.xml':
      '<?xml version="1.0" encoding="utf-8"?>\n<shortcuts xmlns:android="http://schemas.android.com/apk/res/android" />\n',
};

/// Contents of the --stable-ids file for [packageName].
String stableIdsFile(String packageName) => [
      for (final e in templateStableIds.entries) '$packageName:${e.key} = 0x${e.value.toRadixString(16)}',
    ].join('\n') + '\n';
