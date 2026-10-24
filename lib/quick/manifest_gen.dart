/// Writes a real AndroidManifest.xml from scratch -- no byte-patching, no same-length
/// string limits. aapt2 compiles this text file directly, so any app name, any package
/// name, any permission list and any deep-link scheme is just normal text here.
String buildManifest({
  required String packageName,
  required String versionName,
  required int versionCode,
  required String orientation, // 'auto' | 'portrait' | 'landscape'
  required Set<String> permissionIds,
  required Map<String, List<String>> optionalPerms, // same table as _optionalPerms in quick_builder.dart
  String appLinkHost = '',
  String deepLinkScheme = '',
  bool keepScreenOn = false,
}) {
  final perms = StringBuffer();
  perms.writeln('    <uses-permission android:name="android.permission.INTERNET" />');
  perms.writeln('    <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />');
  for (final id in permissionIds) {
    for (final p in optionalPerms[id] ?? const <String>[]) {
      perms.writeln('    <uses-permission android:name="$p" />');
    }
  }

  final orientationAttr = switch (orientation) {
    'portrait' => 'android:screenOrientation="portrait"',
    'landscape' => 'android:screenOrientation="landscape"',
    _ => 'android:screenOrientation="unspecified"',
  };

  final intentFilters = StringBuffer();
  intentFilters.writeln('''
        <intent-filter>
          <action android:name="android.intent.action.MAIN" />
          <category android:name="android.intent.category.LAUNCHER" />
        </intent-filter>''');
  if (deepLinkScheme.isNotEmpty) {
    intentFilters.writeln('''
        <intent-filter>
          <action android:name="android.intent.action.VIEW" />
          <category android:name="android.intent.category.DEFAULT" />
          <category android:name="android.intent.category.BROWSABLE" />
          <data android:scheme="$deepLinkScheme" />
        </intent-filter>''');
  }
  if (appLinkHost.isNotEmpty) {
    intentFilters.writeln('''
        <intent-filter android:autoVerify="true">
          <action android:name="android.intent.action.VIEW" />
          <category android:name="android.intent.category.DEFAULT" />
          <category android:name="android.intent.category.BROWSABLE" />
          <data android:scheme="https" android:host="$appLinkHost" />
        </intent-filter>''');
  }

  return '''<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
    package="$packageName"
    android:versionCode="$versionCode"
    android:versionName="$versionName">

$perms
    <application
        android:label="@string/app_name"
        android:icon="@mipmap/ic_launcher"
        android:roundIcon="@mipmap/ic_launcher"
        android:allowBackup="false"
        android:usesCleartextTraffic="false">
        <activity
            android:name=".MainActivity"
            android:exported="true"
            android:launchMode="singleTask"
            $orientationAttr
            ${keepScreenOn ? 'android:keepScreenOn="true"' : ''}
            android:configChanges="orientation|screenSize|keyboardHidden">
$intentFilters
        </activity>
    </application>
</manifest>
''';
}

/// res/values/strings.xml -- just the app name, XML-escaped (no length limit, unlike the
/// old arscRenameSameLength patch).
String buildStringsXml(String appName) {
  final esc = appName
      .replaceAll('&', '&amp;')
      .replaceAll('<', '&lt;')
      .replaceAll('>', '&gt;')
      .replaceAll('"', '&quot;')
      .replaceAll("'", '&apos;');
  return '<?xml version="1.0" encoding="utf-8"?>\n<resources>\n  <string name="app_name">$esc</string>\n</resources>\n';
}
