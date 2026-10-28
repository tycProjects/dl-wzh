import 'dart:io';
import 'package:flutter/services.dart';
import 'package:path_provider/path_provider.dart';

/// Where everything lives on the phone, and how to start the little Linux the toolchain runs in.
///
/// The Linux is Ubuntu (arm64) started with proot, so no root is needed. proot itself comes from
/// the Termux package repository (see bootstrap.dart); the executables are allowed to run from the
/// app's own folder because the app targets Android 9 (targetSdk 28), same as Termux.
class Linux {
  static late final String base; // app support folder
  static late final String usr; // proot and its libraries (from Termux), laid out like a /usr
  static late final String rootfs; // the Ubuntu root
  static late final String tmp;
  static late final String fake; // fake /proc files (Android blocks the real ones)
  static String nativeLibDir = ''; // where Android put libaapt2.so

  static const _chan = MethodChannel('wzh/ide');

  static Future<void> init() async {
    base = (await getApplicationSupportDirectory()).path;
    usr = '$base/usr';
    rootfs = '$base/rootfs';
    tmp = '$base/tmp';
    fake = '$base/fake';
    try {
      nativeLibDir = (await _chan.invokeMethod<String>('nativeLibraryDir')) ?? '';
    } catch (_) {
      nativeLibDir = '';
    }
    for (final d in [usr, rootfs, tmp, fake]) {
      Directory(d).createSync(recursive: true);
    }
  }

  static String get proot => '$usr/bin/proot';
  static String get projectsDir => '$rootfs/root/projects';
  static File get readyFlag => File('$rootfs/root/.wzh/ready');
  static bool get prootInstalled => File(proot).existsSync() && File('$usr/libexec/proot/loader').existsSync();
  static bool get rootfsInstalled => File('$rootfs/usr/bin/bash').existsSync();

  /// True when the whole toolchain finished installing.
  static bool get ready => readyFlag.existsSync();

  /// Environment for the proot process itself (not for what runs inside).
  static Map<String, String> hostEnv() => {
        ...Platform.environment,
        'PROOT_TMP_DIR': tmp,
        'PROOT_LOADER': '$usr/libexec/proot/loader',
        'LD_LIBRARY_PATH': '$usr/lib',
        'TMPDIR': tmp,
        if (File('$base/no_seccomp').existsSync()) 'PROOT_NO_SECCOMP': '1',
      };

  /// proot arguments that start [cmd] inside the Ubuntu root.
  static List<String> prootArgs(List<String> cmd, {String cwd = '/root'}) {
    final a = <String>[
      '--kill-on-exit',
      '-0',
      '--link2symlink',
      '--sysvipc',
      '-L',
      '-r',
      rootfs,
    ];
    void bind(String host, [String? guest]) {
      if (FileSystemEntity.typeSync(host) == FileSystemEntityType.notFound) return;
      a..add('-b')..add(guest == null ? host : '$host:$guest');
    }

    bind('/dev');
    bind('/proc');
    bind('/sys');
    bind('/proc/self/fd', '/dev/fd');
    bind('/proc/self/fd/0', '/dev/stdin');
    bind('/proc/self/fd/1', '/dev/stdout');
    bind('/proc/self/fd/2', '/dev/stderr');
    bind('$tmp/shm', '/dev/shm');
    for (final f in ['stat', 'loadavg', 'uptime', 'version', 'vmstat']) {
      bind('$fake/$f', '/proc/$f');
    }
    // Lets the phone's own aapt2 run from inside Ubuntu if Ubuntu's does not work (see setup script).
    if (nativeLibDir.isNotEmpty) bind(nativeLibDir, '/opt/wzh-native');
    bind('/system');
    bind('/apex');
    bind('/linkerconfig');
    bind('/storage/emulated/0', '/sdcard');
    a
      ..add('-w')
      ..add(cwd)
      ..addAll([
        '/usr/bin/env',
        '-i',
        'HOME=/root',
        'USER=root',
        'LANG=C.UTF-8',
        'TERM=xterm-256color',
        'TMPDIR=/tmp',
        'PATH=/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin',
        ...cmd,
      ]);
    return a;
  }

  /// Runs a command inside Ubuntu without a terminal and returns its output (for small jobs).
  static Future<ProcessResult> run(List<String> cmd, {String cwd = '/root'}) =>
      Process.run(proot, prootArgs(cmd, cwd: cwd), environment: hostEnv(), includeParentEnvironment: false);
}
