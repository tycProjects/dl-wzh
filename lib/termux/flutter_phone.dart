import 'dart:typed_data';
import 'package:flutter/services.dart';
import 'package:shared_preferences/shared_preferences.dart';
import '../quick/quick_builder.dart' show QuickBuildException;
import 'termux_bridge.dart';
import 'termux_build.dart';

/// Builds a Flutter project on this phone: Flutter, the Android SDK and Java live in a Debian system inside
/// Termux (proot-distro), because Flutter needs a normal Linux. The built APK comes back here and is signed
/// with the same key as the other on-phone builds. Needs the one-time setup (assets/termux/wzh_flutter_setup.sh).
/// Beta: only the arm64 release build is made, and it needs about 4 GB free plus internet for the libraries.
class FlutterPhone {
  static const _key = 'flutter_phone_ready_v1';

  static Future<bool> flag() async => (await SharedPreferences.getInstance()).getBool(_key) ?? false;
  static Future<void> setFlag(bool v) async => (await SharedPreferences.getInstance()).setBool(_key, v);

  /// True when the Flutter tools were installed and checked, and Termux can still be reached.
  static Future<bool> isReady() async => await flag() && await TermuxBridge.installed() && await TermuxBridge.hasPermission();

  static Future<String> setupScript() => rootBundle.loadString('assets/termux/wzh_flutter_setup.sh');

  /// e.g. {'proot': true, 'flutter': false}, or null when Termux didn't answer.
  static Future<Map<String, bool>?> probe() async {
    final script = await rootBundle.loadString('assets/termux/wzh_flutter_probe.sh');
    final o = await TermuxRunner(script: script, startTimeout: const Duration(seconds: 40)).run();
    if (!o.ok || o.probe == null) return null;
    final m = <String, bool>{};
    for (final part in o.probe!.split(RegExp(r'\s+'))) {
      final kv = part.split('=');
      if (kv.length == 2) m[kv[0]] = kv[1] == '1';
    }
    return m;
  }

  static const stageNames = ['Prepare', 'Compile', 'Package', 'Ready'];
  static const stageHints = ['Starting Termux and unpacking your project', 'Compiling your Flutter app (the first build is the slowest)', 'Packaging and signing your app', 'Ready to install'];
  // Seconds a stage takes to reach ~63% of its quarter of the bar. A Flutter build on a phone is long.
  static const stageTaus = [10.0, 420.0, 10.0];
  static int stageOf(String step) => TermuxBuild.stageOf(step);

  /// Runs the whole build. Returns the path of the signed APK.
  static Future<String> run({
    required Uint8List projectZip,
    required String appName,
    required String appId,
    required void Function(String step) onStep,
  }) async {
    if (!await TermuxBridge.channelReady()) {
      throw QuickBuildException('This version of the app was built without Termux support. Build it on the server instead.');
    }
    if (!await TermuxBridge.installed()) {
      throw QuickBuildException('Termux is not installed. Open Set up Flutter on this phone to get it.');
    }
    if (!await TermuxBridge.hasPermission()) {
      throw QuickBuildException('This app is not allowed to run Termux commands yet. Open Set up Flutter on this phone and tap Allow.');
    }
    final script = await rootBundle.loadString('assets/termux/wzh_flutter_build.sh');
    Uint8List asset(ByteData d) => d.buffer.asUint8List(d.offsetInBytes, d.lengthInBytes);
    final runner = TermuxRunner(
      script: script,
      extraArgs: [appId, appName],
      projectZip: projectZip,
      extraFiles: {
        'wzh_flutter_inner.sh': asset(await rootBundle.load('assets/termux/wzh_flutter_inner.sh')),
        'wzh_flutter_patch.py': asset(await rootBundle.load('assets/termux/wzh_flutter_patch.py')),
      },
      onStep: onStep,
      startTimeout: const Duration(seconds: 60),
      idleTimeout: const Duration(minutes: 10),
      totalTimeout: const Duration(minutes: 75),
    );
    onStep('Starting Termux');
    final o = await runner.run();
    if (!o.ok || o.apk == null) {
      final b = StringBuffer(o.message.isEmpty ? 'The build did not finish.' : o.message);
      final tail = _tail(o.log);
      if (tail.isNotEmpty) b..write('\n\n')..write(tail);
      throw QuickBuildException(b.toString());
    }
    return TermuxBuild.signAndSave(o.apk!, appName, onStep);
  }

  /// The lines that say what went wrong (Flutter / Gradle print "Error", "FAILURE", "What went wrong").
  static String _tail(String log, {int lines = 14, int chars = 1400}) {
    final ls = log.split('\n').where((l) => l.trim().isNotEmpty).toList();
    if (ls.isEmpty) return '';
    // Gradle prints the real reason on the lines right after "What went wrong:" (up to "* Try:").
    final w = ls.lastIndexWhere((l) => l.toLowerCase().contains('what went wrong'));
    if (w >= 0) {
      final block = <String>[];
      for (var i = w + 1; i < ls.length && block.length < 10; i++) {
        if (ls[i].trimLeft().startsWith('* Try')) break;
        block.add(ls[i]);
      }
      if (block.isNotEmpty) {
        var t = block.join('\n');
        if (t.length > chars) t = t.substring(0, chars);
        return t;
      }
    }
    final errs = ls.where((l) {
      final t = l.toLowerCase();
      return t.contains('error') || t.contains('failure') || t.contains('went wrong') || t.contains('exception');
    }).toList();
    final pick = errs.isNotEmpty ? errs : ls;
    var t = pick.sublist(pick.length > lines ? pick.length - lines : 0).join('\n');
    if (t.length > chars) t = t.substring(t.length - chars);
    return t;
  }
}
