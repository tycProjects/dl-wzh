import 'dart:async';
import 'dart:convert';
import 'dart:io';
import 'dart:math' as math;
import 'dart:typed_data';
import 'package:flutter/services.dart';
import 'termux_bridge.dart';

/// A terminal session: a loop running in Termux (assets/termux/wzh_term.sh) fetches the commands this class
/// queues over a local, token-protected server and sends the output back. The folder and exported variables
/// are kept between commands. Typing input into a running command (stdin) is not supported.
class TermuxSession {
  final Uint8List? projectZip;
  TermuxSession({this.projectZip});

  final _out = StreamController<String>.broadcast();
  Stream<String> get output => _out.stream;

  HttpServer? _srv;
  final String _token = _makeToken();
  final _queue = <_Cmd>[];
  final _running = <String, Completer<int>>{};
  Completer<void>? _wake;
  Completer<String>? _hello;
  bool _closed = false;
  bool _cancel = false;
  int _n = 0;

  /// The folder the next command starts in (as the shell reports it).
  String cwd = '~';
  bool get busy => _running.isNotEmpty;
  bool get closed => _closed;

  late final ByteConversionSink _dec = const Utf8Decoder(allowMalformed: true).startChunkedConversion(StringConversionSink.fromStringSink(_Emit((s) {
    if (!_out.isClosed) _out.add(s);
  })));

  static String _makeToken() {
    final r = math.Random.secure();
    return List.generate(24, (_) => r.nextInt(36).toRadixString(36)).join();
  }

  /// Starts Termux and waits until it answers. Returns null when ready, or the reason it did not work.
  Future<String?> start() async {
    if (!await TermuxBridge.channelReady()) return 'This version of the app was built without Termux support.';
    if (!await TermuxBridge.installed()) return 'Termux is not installed. Open Set up on this phone to get it.';
    if (!await TermuxBridge.hasPermission()) return 'This app is not allowed to run Termux commands yet. Open Set up on this phone and tap Allow.';
    final script = await rootBundle.loadString('assets/termux/wzh_term.sh');
    final srv = await HttpServer.bind(InternetAddress.loopbackIPv4, 0);
    _srv = srv;
    _hello = Completer<String>();
    srv.listen(_handle);
    final base = 'http://127.0.0.1:${srv.port}/$_token';
    final err = await TermuxBridge.run(['-c', script, 'wzh', base, projectZip != null ? '1' : '0']);
    if (err != null) {
      await close();
      return 'Could not start Termux: $err';
    }
    try {
      cwd = await _hello!.future.timeout(const Duration(seconds: 60));
      return null;
    } on TimeoutException {
      await close();
      return 'Termux did not answer. Check that Termux is installed (from F-Droid), that Set up on this phone is finished, and that Termux is not battery-restricted.';
    }
  }

  /// Runs one command. Completes with its exit code (-1 when the session is closed first).
  Future<int> run(String command) {
    if (_closed) return Future.value(-1);
    final id = '${++_n}';
    final c = Completer<int>();
    _running[id] = c;
    _queue.add(_Cmd(id, command));
    _poke();
    return c.future;
  }

  void _poke() {
    final w = _wake;
    if (w != null && !w.isCompleted) w.complete();
  }

  /// Stops the command that is running now (the same as Ctrl+C).
  void cancel() {
    if (_running.isNotEmpty) _cancel = true;
  }

  Future<void> close() async {
    if (_closed) return;
    _closed = true;
    _poke();
    for (final c in _running.values) {
      if (!c.isCompleted) c.complete(-1);
    }
    _running.clear();
    final s = _srv;
    _srv = null;
    // Give the Termux loop a moment to be told to stop (it gets 410), then shut the server.
    Future.delayed(const Duration(seconds: 3), () async {
      try {
        await s?.close(force: true);
      } catch (_) {}
      if (!_out.isClosed) await _out.close();
    });
  }

  Future<void> _handle(HttpRequest req) async {
    try {
      final segs = req.uri.pathSegments;
      if (segs.length < 2 || segs[0] != _token) {
        req.response.statusCode = 403;
        await req.response.close();
        return;
      }
      final route = segs[1];
      if (req.method == 'GET' && route == 'project.zip' && projectZip != null) {
        req.response.headers.contentLength = projectZip!.length;
        req.response.add(projectZip!);
      } else if (req.method == 'GET' && route == 'cmd') {
        await _serveCmd(req);
        return;
      } else if (req.method == 'GET' && route == 'cancel') {
        req.response.write(_cancel ? '1' : '0');
      } else if (req.method == 'POST') {
        final b = BytesBuilder(copy: false);
        await for (final c in req) {
          b.add(c);
        }
        final bytes = b.takeBytes();
        switch (route) {
          case 'hello':
            if (!(_hello?.isCompleted ?? true)) _hello!.complete(utf8.decode(bytes, allowMalformed: true).trim());
          case 'out':
            _dec.add(bytes);
          case 'done':
            final parts = utf8.decode(bytes, allowMalformed: true).split('\n');
            if (parts.length >= 3) {
              cwd = parts.sublist(2).join('\n').trim();
              final c = _running.remove(parts[0].trim());
              if (_running.isEmpty) _cancel = false;
              if (c != null && !c.isCompleted) c.complete(int.tryParse(parts[1].trim()) ?? -1);
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
  }

  /// Long poll: hands over the next command, or answers 204 after 25 s so the loop asks again.
  Future<void> _serveCmd(HttpRequest req) async {
    final res = req.response;
    try {
      if (_closed) {
        res.statusCode = 410;
      } else {
        if (_queue.isEmpty) {
          _wake = Completer<void>();
          await _wake!.future.timeout(const Duration(seconds: 25), onTimeout: () {});
        }
        if (_closed) {
          res.statusCode = 410;
        } else if (_queue.isEmpty) {
          res.statusCode = 204;
        } else {
          final c = _queue.removeAt(0);
          res.write('${c.id}\n${c.text}\n');
        }
      }
    } catch (_) {}
    try {
      await res.close();
    } catch (_) {}
  }
}

class _Cmd {
  final String id, text;
  _Cmd(this.id, this.text);
}

class _Emit implements StringSink {
  final void Function(String) f;
  _Emit(this.f);
  @override
  void write(Object? o) => f('$o');
  @override
  void writeAll(Iterable objects, [String separator = '']) => f(objects.join(separator));
  @override
  void writeCharCode(int c) => f(String.fromCharCode(c));
  @override
  void writeln([Object? o = '']) => f('$o\n');
}
