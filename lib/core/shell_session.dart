import 'dart:async';
import 'dart:convert';
import 'dart:typed_data';
import 'package:flutter/foundation.dart';
import 'package:flutter_pty/flutter_pty.dart';
import 'package:xterm/xterm.dart';
import 'linux.dart';

/// One terminal: an xterm screen wired to a real pseudo-terminal that runs something inside Ubuntu.
class ShellSession {
  /// What the IDE uses: an interactive bash in Ubuntu that stays open.
  static final ShellSession main = ShellSession(const ['/bin/bash', '-l']);

  /// The first-run installer.
  static ShellSession? _setup;
  static ShellSession get setup => _setup ??= ShellSession(const ['/bin/bash', '/root/wzh-setup.sh']);
  static void resetSetup() => _setup = null;

  final List<String> command;
  ShellSession(this.command);

  final terminal = Terminal(maxLines: 20000);
  Pty? _pty;
  bool _started = false;
  int? exitCode; // set when the process ended
  final ctrl = ValueNotifier<bool>(false); // the on-screen Ctrl key is armed

  final _out = StreamController<String>.broadcast();
  /// Everything the process prints (already decoded), for the setup progress and exit markers.
  Stream<String> get output => _out.stream;
  final _exitCtl = StreamController<int>.broadcast();
  Stream<int> get onExit => _exitCtl.stream;

  bool get running => _pty != null && exitCode == null;

  void start() {
    if (_started) return;
    _started = true;
    exitCode = null;
    final pty = Pty.start(
      Linux.proot,
      arguments: Linux.prootArgs(command),
      environment: Linux.hostEnv(),
      columns: terminal.viewWidth > 0 ? terminal.viewWidth : 80,
      rows: terminal.viewHeight > 0 ? terminal.viewHeight : 24,
    );
    _pty = pty;
    pty.output.cast<List<int>>().transform(const Utf8Decoder(allowMalformed: true)).listen((s) {
      terminal.write(s);
      _scan(s);
      _out.add(s);
    });
    terminal.onOutput = (d) {
      var data = d;
      if (ctrl.value && data.length == 1) {
        final c = data.toLowerCase().codeUnitAt(0);
        if (c >= 97 && c <= 122) data = String.fromCharCode(c - 96);
        ctrl.value = false;
      }
      pty.write(Uint8List.fromList(utf8.encode(data)));
    };
    terminal.onResize = (w, h, pw, ph) => pty.resize(h, w);
    pty.exitCode.then((c) {
      exitCode = c;
      terminal.write('\r\n[process ended, code $c]\r\n');
      _exitCtl.add(c);
    });
  }

  /// Starts again after the process ended.
  void restart() {
    _pty?.kill();
    _pty = null;
    _started = false;
    terminal.write('\x1b[2J\x1b[H');
    start();
  }

  void write(String s) {
    final p = _pty;
    if (p == null || exitCode != null) return;
    p.write(Uint8List.fromList(utf8.encode(s)));
  }

  // ---------------------------------------------------------------- running commands for the IDE

  static final _marker = RegExp(r'\[wzh-exit:(\d+)\]');
  String _tail = '';
  final _waiting = <void Function(int)>[];

  void _scan(String chunk) {
    if (_waiting.isEmpty) {
      _tail = '';
      return;
    }
    _tail += chunk;
    final m = _marker.firstMatch(_tail);
    if (m != null) {
      _tail = _tail.substring(m.end);
      final code = int.parse(m.group(1)!);
      final cb = _waiting.removeAt(0);
      cb(code);
    } else if (_tail.length > 400) {
      _tail = _tail.substring(_tail.length - 200);
    }
  }

  /// Types [cmd] into the shell and calls [done] with its exit code when it finishes.
  void run(String cmd, {void Function(int code)? done}) {
    start();
    if (done != null) _waiting.add(done);
    // Ctrl-C first would kill a running job; instead just queue the line behind it.
    write('$cmd; echo "[wzh-exit:\$?]"\n');
  }
}
