import 'dart:async';
import 'dart:convert';
import 'dart:io';
import 'dart:math' as math;
import 'dart:typed_data';
import 'package:flutter/services.dart';

/// Talks to Termux (the terminal app) so builds can run on this phone, with no server and no GitHub.
///
/// How it works: this app starts a tiny web server that only listens on 127.0.0.1 (this phone, not the
/// network) and then asks Termux to run a script. The script downloads the project from that server,
/// compiles it, and posts the log and the finished app back. A random token in every address keeps other
/// apps from talking to it. Needs the one-time setup in termux_setup_page.dart.
class TermuxBridge {
  static const _ch = MethodChannel('wzh/termux');
  static const fdroidUrl = 'https://f-droid.org/packages/com.termux/';

  /// Paste this in Termux once so other apps are allowed to run commands there.
  static const enableCommand =
      'mkdir -p ~/.termux && (grep -qs "^allow-external-apps *= *true" ~/.termux/termux.properties || echo "allow-external-apps=true" >> ~/.termux/termux.properties); termux-reload-settings';

  /// True when this build of the app has the Termux channel at all (older server builds do not).
  static Future<bool> channelReady() async {
    try {
      await _ch.invokeMethod('installed');
      return true;
    } on MissingPluginException {
      return false;
    } catch (_) {
      return true;
    }
  }

  static Future<bool> installed() async {
    try {
      return await _ch.invokeMethod<bool>('installed') ?? false;
    } catch (_) {
      return false;
    }
  }

  static Future<bool> hasPermission() async {
    try {
      return await _ch.invokeMethod<bool>('hasPermission') ?? false;
    } catch (_) {
      return false;
    }
  }

  static Future<void> requestPermission() async {
    try {
      await _ch.invokeMethod('requestPermission');
    } catch (_) {}
  }

  static Future<void> openTermux() async {
    try {
      await _ch.invokeMethod('openTermux');
    } catch (_) {}
  }

  /// Sends one command to Termux. Returns null when it was sent, or the reason it was refused.
  /// [background] true = runs silently; false = opens a Termux window so you can watch it.
  static Future<String?> run(List<String> args, {bool background = true}) async {
    try {
      final r = await _ch.invokeMethod<String>('run', {'args': args, 'background': background});
      return r;
    } on MissingPluginException {
      return 'This version of the app was built without Termux support.';
    } catch (e) {
      return '$e';
    }
  }
}

class TermuxOutcome {
  final bool ok;
  final String message; // why it failed, or ''
  final String log; // everything the tools printed
  final Uint8List? apk; // the unsigned app, for a build
  final String? probe; // "java=1 kotlinc=0 ..." for a tool check
  const TermuxOutcome({required this.ok, this.message = '', this.log = '', this.apk, this.probe});
}

/// One script run: starts the local server, launches the script in Termux, waits for the result.
class TermuxRunner {
  final String script;
  final List<String> extraArgs;
  final Uint8List? projectZip;
  final String? androidJar;
  /// The Build Doctor (server repair rules as one node script); the build script downloads it when it wants it.
  final Uint8List? doctorJs;
  /// Other files the script may download from the app, by name (e.g. the Flutter build steps).
  final Map<String, Uint8List> extraFiles;
  final bool background;
  final void Function(String step)? onStep;
  final Duration startTimeout, idleTimeout, totalTimeout;
  TermuxRunner({
    required this.script,
    this.extraArgs = const [],
    this.projectZip,
    this.androidJar,
    this.doctorJs,
    this.extraFiles = const {},
    this.background = true,
    this.onStep,
    this.startTimeout = const Duration(seconds: 45),
    this.idleTimeout = const Duration(minutes: 12),
    this.totalTimeout = const Duration(minutes: 30),
  });

  bool _cancelled = false;
  void cancel() => _cancelled = true;

  static String _token() {
    final r = math.Random.secure();
    return List.generate(24, (_) => r.nextInt(36).toRadixString(36)).join();
  }

  Future<TermuxOutcome> run() async {
    final token = _token();
    final srv = await HttpServer.bind(InternetAddress.loopbackIPv4, 0);
    final done = Completer<TermuxOutcome>();
    final log = StringBuffer();
    Uint8List? apk;
    String? probe;
    var gotSignal = false;
    var last = DateTime.now();
    final started = DateTime.now();

    void finish(TermuxOutcome o) {
      if (!done.isCompleted) done.complete(o);
    }

    srv.listen((HttpRequest req) async {
      try {
        final segs = req.uri.pathSegments;
        if (segs.length < 2 || segs[0] != token) {
          req.response.statusCode = 403;
          await req.response.close();
          return;
        }
        final route = segs[1];
        gotSignal = true;
        last = DateTime.now();
        if (req.method == 'GET' && route == 'project.zip' && projectZip != null) {
          req.response.headers.contentLength = projectZip!.length;
          req.response.add(projectZip!);
        } else if (req.method == 'GET' && route == 'wzh_doctor.js' && doctorJs != null) {
          req.response.headers.contentLength = doctorJs!.length;
          req.response.add(doctorJs!);
        } else if (req.method == 'GET' && extraFiles.containsKey(route)) {
          final b = extraFiles[route]!;
          req.response.headers.contentLength = b.length;
          req.response.add(b);
        } else if (req.method == 'GET' && route == 'android.jar' && androidJar != null) {
          final f = File(androidJar!);
          req.response.headers.contentLength = await f.length();
          await req.response.addStream(f.openRead());
        } else if (req.method == 'POST' || req.method == 'PUT') {
          final b = BytesBuilder(copy: false);
          await for (final c in req) {
            b.add(c);
          }
          final bytes = b.takeBytes();
          String text() => utf8.decode(bytes, allowMalformed: true);
          switch (route) {
            case 'step':
              onStep?.call(text().trim());
            case 'log':
              log.writeln(text());
            case 'out.apk':
              apk = bytes;
            case 'probe':
              probe = text().trim();
              finish(TermuxOutcome(ok: true, probe: probe, log: log.toString()));
            case 'status':
              final s = text().trim();
              if (s == 'ok') {
                finish(TermuxOutcome(ok: apk != null, apk: apk, log: log.toString(), message: apk == null ? 'The build finished but no app came back.' : ''));
              } else {
                finish(TermuxOutcome(ok: false, message: s.startsWith('fail:') ? s.substring(5).trim() : s, log: log.toString()));
              }
          }
        } else {
          req.response.statusCode = 404;
        }
        await req.response.close();
      } catch (_) {
        try {
          await req.response.close();
        } catch (_) {}
      }
    });

    final base = 'http://127.0.0.1:${srv.port}/$token';
    final watchdog = Timer.periodic(const Duration(seconds: 2), (_) {
      final now = DateTime.now();
      if (_cancelled) {
        finish(TermuxOutcome(ok: false, message: 'Cancelled.', log: log.toString()));
      } else if (!gotSignal && now.difference(started) > startTimeout) {
        finish(TermuxOutcome(
          ok: false,
          message: 'Termux did not answer. Check that Termux is installed (from F-Droid), that "Set up on this phone" is finished, and that Termux is not battery-restricted.',
          log: log.toString(),
        ));
      } else if (gotSignal && now.difference(last) > idleTimeout) {
        finish(TermuxOutcome(ok: false, message: 'The build stopped answering. Android may have closed Termux. Open Termux once, then try again.', log: log.toString()));
      } else if (now.difference(started) > totalTimeout) {
        finish(TermuxOutcome(ok: false, message: 'The build took too long and was stopped.', log: log.toString()));
      }
    });

    try {
      final err = await TermuxBridge.run(['-c', script, 'wzh', base, ...extraArgs], background: background);
      if (err != null) {
        finish(TermuxOutcome(ok: false, message: _explain(err), log: ''));
      }
      return await done.future;
    } finally {
      watchdog.cancel();
      await srv.close(force: true);
    }
  }

  static String _explain(String err) {
    if (err.contains('SecurityException') || err.contains('RUN_COMMAND')) {
      return 'Android has not allowed this app to run Termux commands yet. Open Set up on this phone and tap Allow.';
    }
    if (err.contains('ClassNotFound') || err.contains('not found') || err.contains('NameNotFound')) {
      return 'Termux is not installed. Get it from F-Droid (the Play Store version does not work).';
    }
    return 'Could not start Termux: $err';
  }
}
