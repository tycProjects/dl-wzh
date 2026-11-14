import 'package:flutter/foundation.dart';
import 'package:flutter/services.dart';

/// Which Store apps are installed on this phone (so the button can say Install / Open / Update, like Google Play).
/// Backed by two small methods in MainActivity ("wzh/native_tools"); if they are missing the
/// answer is simply "not installed" and the button stays "Install".
class InstalledApps {
  static const _ch = MethodChannel('wzh/native_tools');
  static final Map<String, String> _versions = {}; // package -> installed versionName
  static final Map<String, int> _codes = {}; // package -> installed versionCode (0 = unknown, older native code)
  /// Changes whenever the installed state may have changed, so buttons rebuild.
  static final ValueNotifier<int> tick = ValueNotifier(0);

  /// Re-checks these packages. Call after the list loads, and whenever the app comes back to the front
  /// (after installing or uninstalling in the system screens).
  static Future<void> refresh(Iterable<String> pkgs) async {
    var changed = false;
    for (final p in pkgs.toSet()) {
      if (p.isEmpty) continue;
      String? v;
      try {
        v = await _ch.invokeMethod<String>('installedVersion', {'pkg': p});
      } catch (_) {
        v = null;
      }
      if (v == null) {
        _codes.remove(p);
        if (_versions.remove(p) != null) changed = true;
      } else {
        // New native code answers "versionName|versionCode"; older builds answer just the name.
        final i = v.lastIndexOf('|');
        final name = i < 0 ? v : v.substring(0, i);
        final code = i < 0 ? 0 : (int.tryParse(v.substring(i + 1)) ?? 0);
        if (_versions[p] != name || _codes[p] != code) {
          _versions[p] = name;
          _codes[p] = code;
          changed = true;
        }
      }
    }
    if (changed) tick.value++;
  }

  /// 0 = not installed (Install), 1 = installed (Open), 2 = installed but the Store has a newer version (Update).
  static int stateOf(String pkg, String storeVersion, [int storeCode = 0]) {
    final v = _versions[pkg];
    if (pkg.isEmpty || v == null) return 0;
    final have = _codes[pkg] ?? 0;
    // Android decides updates by versionCode, so use it whenever both sides know it.
    if (storeCode > 0 && have > 0) return storeCode > have ? 2 : 1;
    return storeVersion.isNotEmpty && _newer(storeVersion, v) ? 2 : 1;
  }

  static Future<bool> open(String pkg) async {
    try {
      return await _ch.invokeMethod<bool>('launchApp', {'pkg': pkg}) ?? false;
    } catch (_) {
      return false;
    }
  }

  static bool _newer(String a, String b) {
    List<int> parts(String s) => RegExp(r'\d+').allMatches(s).map((m) => int.parse(m.group(0)!)).toList();
    final x = parts(a), y = parts(b);
    for (var i = 0; i < (x.length > y.length ? x.length : y.length); i++) {
      final p = i < x.length ? x[i] : 0, q = i < y.length ? y[i] : 0;
      if (p != q) return p > q;
    }
    return false;
  }
}
