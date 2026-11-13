import 'dart:io';
import 'dart:typed_data';
import 'package:archive/archive.dart';
import 'package:flutter/foundation.dart';
import 'package:flutter/services.dart';
import 'package:path_provider/path_provider.dart';
import '../quick/apk_signer.dart';
import '../quick/apk_zip.dart';
import '../quick/aapt_build.dart' show androidJarPath;
import '../quick/quick_builder.dart' show QuickBuildException;
import '../save_install.dart';
import '../signing_key.dart';
import 'termux_bridge.dart';

/// Builds a Kotlin / Java project on this phone with the tools in Termux (see termux_bridge.dart), then
/// signs it here with the same key the Quick build uses. No Gradle, no server, no GitHub.
///
/// Libraries: wzh_build.sh downloads the project's Gradle dependencies (Google Maven, Maven Central, maven{}
/// URLs, version catalog, BOMs), links their resources, merges their manifests and writes BuildConfig, so
/// androidx / Material projects build like on the server. Not done on the phone yet: Compose, kapt/ksp,
/// view/data binding and multi-module (project(":x")) projects.
class TermuxBuild {
  /// Which tools Termux has right now (null if Termux didn't answer).
  /// Returns e.g. {'java': true, 'kotlinc': false, ...}.
  static Future<Map<String, bool>?> probe() async {
    final script = await rootBundle.loadString('assets/termux/wzh_probe.sh');
    final o = await TermuxRunner(script: script, startTimeout: const Duration(seconds: 25)).run();
    if (!o.ok || o.probe == null) return null;
    final m = <String, bool>{};
    for (final part in o.probe!.split(RegExp(r'\s+'))) {
      final kv = part.split('=');
      if (kv.length == 2) m[kv[0]] = kv[1] == '1';
    }
    return m;
  }

  /// Which of the 4 stages on the progress page a step belongs to (Prepare, Compile, Package, Ready).
  static int stageOf(String step) {
    if (step.startsWith('Compiling') || step.startsWith('Converting')) return 1;
    if (step.startsWith('Packaging') || step.startsWith('Signing') || step.startsWith('Saving') || step.startsWith('Making')) return 2;
    return 0;
  }

  static const stageNames = ['Prepare', 'Compile', 'Package', 'Ready'];
  static const stageHints = ['Starting Termux and unpacking your project', 'Compiling your code', 'Packaging and signing your app', 'Ready to install'];
  // Seconds a stage takes to reach ~63% of its quarter of the bar. Compiling on a phone takes a while.
  static const stageTaus = [8.0, 70.0, 10.0];

  /// True when everything a build needs is installed.
  static bool toolsReady(Map<String, bool> t) {
    final base = ['java', 'javac', 'jar', 'aapt2', 'zip', 'unzip', 'curl'].every((k) => t[k] == true);
    return base && (t['d8'] == true || t['dx'] == true) && t['kotlinc'] == true;
  }

  /// Opens a Termux window and installs the build tools there (one time).
  static Future<String?> startInstall() async {
    final script = await rootBundle.loadString('assets/termux/wzh_setup.sh');
    return TermuxBridge.run(['-c', script, 'wzh'], background: false);
  }

  /// Runs the whole build. Returns the path of the signed APK. Throws [QuickBuildException] with a message
  /// (and the end of the tool log) when it fails.
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
      throw QuickBuildException('Termux is not installed. Open Set up on this phone to get it.');
    }
    if (!await TermuxBridge.hasPermission()) {
      throw QuickBuildException('This app is not allowed to run Termux commands yet. Open Set up on this phone and tap Allow.');
    }
    final script = await rootBundle.loadString('assets/termux/wzh_build.sh');
    final jar = await androidJarPath();
    Uint8List? doctor;
    try {
      doctor = (await rootBundle.load('assets/termux/wzh_doctor.js')).buffer.asUint8List();
    } catch (_) {
      doctor = null; // the build still works without the Build Doctor
    }
    final runner = TermuxRunner(
      script: script,
      extraArgs: [appId],
      projectZip: projectZip,
      androidJar: jar,
      doctorJs: doctor,
      onStep: onStep,
    );
    onStep('Starting Termux');
    final o = await runner.run();
    if (!o.ok || o.apk == null) {
      throw QuickBuildException(_failText(o));
    }

    var key = SigningKeys.i.current.value;
    if (key == null && await SigningKeys.i.syncWithAccount()) key = SigningKeys.i.current.value;
    if (key == null) {
      onStep('Making your signing key');
      key = await SigningKeys.i.create();
    }
    final pk8 = Uint8List.fromList(await File(key.keyPath).readAsBytes());
    final cert = Uint8List.fromList(await File(key.certPath).readAsBytes());

    onStep('Signing your app');
    final Uint8List signed;
    try {
      signed = await compute(_signApk, <String, Object>{'apk': o.apk!, 'pk8': pk8, 'cert': cert});
    } catch (e) {
      throw QuickBuildException('The app was built but signing it failed: $e');
    }
    onStep('Saving');
    final dir = await getTemporaryDirectory();
    final out = File('${dir.path}/${FileSaver.safe(appName)}_termux.apk');
    await out.writeAsBytes(signed, flush: true);
    return out.path;
  }

  static String _failText(TermuxOutcome o) {
    final b = StringBuffer(o.message.isEmpty ? 'The build did not finish.' : o.message);
    final tail = _tail(o.log);
    if (tail.isNotEmpty) {
      b.write('\n\n');
      b.write(tail);
    }
    return b.toString();
  }

  /// The last lines of the tool log, where the compile errors are.
  static String _tail(String log, {int lines = 14, int chars = 1400}) {
    final ls = log.split('\n').where((l) => l.trim().isNotEmpty).toList();
    if (ls.isEmpty) return '';
    // Prefer the lines that say "error" when there are some.
    final errs = ls.where((l) => l.toLowerCase().contains('error')).toList();
    final pick = (errs.isNotEmpty ? errs : ls);
    var t = pick.sublist(pick.length > lines ? pick.length - lines : 0).join('\n');
    if (t.length > chars) t = t.substring(t.length - chars);
    return t;
  }
}

/// Runs in a background isolate: re-packs the unsigned app (resources.arsc stored and aligned, as Android
/// 11+ needs) and signs it (v1 + v2) with the user's key.
Uint8List _signApk(Map<String, Object> m) {
  final arc = ZipDecoder().decodeBytes(m['apk'] as Uint8List);
  final files = <ZipFileData>[];
  ZipFileData? manifest;
  for (final f in arc.files) {
    if (!f.isFile) continue;
    final name = f.name;
    if (name.startsWith('META-INF/')) continue;
    final data = Uint8List.fromList(f.content as List<int>);
    final zf = ZipFileData(name, data, stored: name == 'resources.arsc');
    if (name == 'AndroidManifest.xml') {
      manifest = zf;
    } else {
      files.add(zf);
    }
  }
  if (manifest != null) files.insert(0, manifest);
  return signApk(files, m['pk8'] as Uint8List, m['cert'] as Uint8List);
}
