import 'dart:async';
import 'dart:convert';
import 'dart:io';
import 'dart:typed_data';
import 'package:archive/archive.dart';
import 'package:http/http.dart' as http;
import 'linux.dart';
import 'setup_script.dart';

typedef Report = void Function(String status, double? progress);

class BootstrapError implements Exception {
  final String message;
  BootstrapError(this.message);
  @override
  String toString() => message;
}

/// First-launch work that has to happen before any Linux exists: fetch proot (from the Termux
/// package repository), fetch and unpack the Ubuntu root, write its config. Everything after that
/// (Java, Flutter, Android SDK) is installed by setup_script.dart, visibly, in the terminal.
class Bootstrap {
  static const termux = 'https://packages.termux.dev/apt/termux-main';
  static const ubuntuDir = 'https://cdimage.ubuntu.com/ubuntu-base/releases/24.04/release/';

  final Report report;
  Bootstrap(this.report);

  Future<void> run() async {
    if (!Linux.prootInstalled) await _installProot();
    if (!Linux.rootfsInstalled) await _installRootfs();
    report('Preparing…', null);
    await writeConfig();
  }

  // ------------------------------------------------------------------ download helper

  Future<void> _download(String url, File to, String label) async {
    Object? last;
    for (var attempt = 1; attempt <= 3; attempt++) {
      final client = http.Client();
      try {
        final req = http.Request('GET', Uri.parse(url));
        final res = await client.send(req).timeout(const Duration(seconds: 30));
        if (res.statusCode != 200) throw BootstrapError('$label: server answered ${res.statusCode}');
        final total = res.contentLength ?? 0;
        var got = 0;
        var tick = DateTime.now();
        to.parent.createSync(recursive: true);
        final sink = to.openWrite();
        try {
          await for (final chunk in res.stream.timeout(const Duration(seconds: 40))) {
            sink.add(chunk);
            got += chunk.length;
            if (DateTime.now().difference(tick).inMilliseconds > 150) {
              tick = DateTime.now();
              final mb = (got / 1048576).toStringAsFixed(1);
              report(total > 0 ? '$label  $mb / ${(total / 1048576).toStringAsFixed(1)} MB' : '$label  $mb MB', total > 0 ? got / total : null);
            }
          }
        } finally {
          await sink.close();
        }
        return;
      } catch (e) {
        last = e;
        report('$label failed, trying again ($attempt/3)…', null);
        await Future<void>.delayed(const Duration(seconds: 2));
      } finally {
        client.close();
      }
    }
    throw BootstrapError('Couldn’t download $label. Check your internet connection and try again.\n($last)');
  }

  Future<String> _text(String url) async {
    final r = await http.get(Uri.parse(url)).timeout(const Duration(seconds: 40));
    if (r.statusCode != 200) throw BootstrapError('Server answered ${r.statusCode} for $url');
    return utf8.decode(r.bodyBytes, allowMalformed: true);
  }

  // ------------------------------------------------------------------ proot

  Future<void> _installProot() async {
    report('Finding proot…', null);
    String index;
    try {
      index = await _text('$termux/dists/stable/main/binary-aarch64/Packages');
    } catch (_) {
      final r = await http.get(Uri.parse('$termux/dists/stable/main/binary-aarch64/Packages.gz')).timeout(const Duration(seconds: 40));
      if (r.statusCode != 200) throw BootstrapError('Couldn’t reach the package server (${r.statusCode}).');
      index = utf8.decode(GZipDecoder().decodeBytes(r.bodyBytes), allowMalformed: true);
    }
    final pk = <String, Map<String, String>>{};
    for (final block in index.split('\n\n')) {
      final m = <String, String>{};
      for (final line in block.split('\n')) {
        final i = line.indexOf(': ');
        if (i > 0 && !line.startsWith(' ')) m[line.substring(0, i)] = line.substring(i + 2).trim();
      }
      final n = m['Package'];
      if (n != null) pk[n] = m;
    }
    if (!pk.containsKey('proot')) throw BootstrapError('proot was not found in the package list.');
    // proot and what it needs (libtalloc), nothing more.
    final want = <String>[];
    void add(String n) {
      if (want.contains(n) || !pk.containsKey(n) || want.length >= 8) return;
      want.add(n);
      for (final d in (pk[n]!['Depends'] ?? '').split(',')) {
        final name = d.split('|').first.replaceAll(RegExp(r'\(.*\)'), '').trim();
        if (name.isNotEmpty) add(name);
      }
    }

    add('proot');
    final dl = Directory('${Linux.base}/dl')..createSync(recursive: true);
    for (var i = 0; i < want.length; i++) {
      final n = want[i];
      final fn = pk[n]!['Filename'];
      if (fn == null) continue;
      final deb = File('${dl.path}/$n.deb');
      await _download('$termux/$fn', deb, 'Downloading $n (${i + 1}/${want.length})');
      report('Unpacking $n…', null);
      _extractDeb(deb.readAsBytesSync());
      deb.deleteSync();
    }
    _ensureSonames('${Linux.usr}/lib');
    await Process.run('/system/bin/chmod', ['-R', '755', Linux.usr]);
    if (!Linux.prootInstalled) throw BootstrapError('proot did not unpack correctly.');
    // A quick self test so a broken proot is caught here and not as a confusing terminal error.
    final t = await Process.run(Linux.proot, ['--version'], environment: Linux.hostEnv(), includeParentEnvironment: false);
    if (t.exitCode != 0) {
      throw BootstrapError('proot can’t run on this phone (exit ${t.exitCode}).\n${t.stderr}');
    }
  }

  /// A .deb is an "ar" archive holding control.tar.* and data.tar.*; Termux packages are laid out
  /// under /data/data/com.termux/files/usr, which we drop into our own usr folder.
  void _extractDeb(Uint8List b) {
    if (b.length < 8 || String.fromCharCodes(b.sublist(0, 8)) != '!<arch>\n') throw BootstrapError('A downloaded package is not valid.');
    var o = 8;
    Uint8List? data;
    String? dataName;
    while (o + 60 <= b.length) {
      final name = String.fromCharCodes(b.sublist(o, o + 16)).trim();
      final size = int.parse(String.fromCharCodes(b.sublist(o + 48, o + 58)).trim());
      final start = o + 60;
      if (name.startsWith('data.tar')) {
        data = Uint8List.sublistView(b, start, start + size);
        dataName = name;
        break;
      }
      o = start + size + (size.isOdd ? 1 : 0);
    }
    if (data == null) throw BootstrapError('A downloaded package has no files in it.');
    List<int> tarBytes;
    if (dataName!.endsWith('.xz')) {
      tarBytes = XZDecoder().decodeBytes(data);
    } else if (dataName.endsWith('.gz')) {
      tarBytes = GZipDecoder().decodeBytes(data);
    } else {
      tarBytes = data;
    }
    const prefix = 'data/data/com.termux/files/usr/';
    for (final f in TarDecoder().decodeBytes(tarBytes)) {
      var n = f.name;
      while (n.startsWith('./') || n.startsWith('/')) {
        n = n.startsWith('./') ? n.substring(2) : n.substring(1);
      }
      if (!n.startsWith(prefix)) continue;
      final rel = n.substring(prefix.length);
      if (rel.isEmpty) continue;
      final path = '${Linux.usr}/$rel';
      var isLink = false;
      String? target;
      try {
        isLink = (f as dynamic).isSymbolicLink == true;
        target = (f as dynamic).nameOfLinkedFile as String?;
      } catch (_) {}
      if (isLink && target != null && target.isNotEmpty) {
        try {
          final l = Link(path);
          if (l.existsSync()) l.deleteSync();
          Directory(File(path).parent.path).createSync(recursive: true);
          l.createSync(target);
        } catch (_) {}
      } else if (f.isFile) {
        Directory(File(path).parent.path).createSync(recursive: true);
        File(path).writeAsBytesSync(f.content as List<int>);
      }
    }
  }

  /// If the unpacker lost the lib symlinks, make real copies named like the library's soname.
  void _ensureSonames(String dir) {
    final d = Directory(dir);
    if (!d.existsSync()) return;
    final re = RegExp(r'^(lib.+\.so)\.(\d+)(\.[\d.]+)?$');
    for (final e in d.listSync().whereType<File>()) {
      final n = e.uri.pathSegments.last;
      final m = re.firstMatch(n);
      if (m == null) continue;
      for (final alias in ['${m.group(1)}.${m.group(2)}', m.group(1)!]) {
        final target = File('$dir/$alias');
        if (!target.existsSync() && !FileSystemEntity.isLinkSync(target.path)) e.copySync(target.path);
      }
    }
  }

  // ------------------------------------------------------------------ Ubuntu root

  Future<void> _installRootfs() async {
    report('Finding Ubuntu…', null);
    final html = await _text(ubuntuDir);
    final re = RegExp(r'ubuntu-base-(\d+)\.(\d+)\.?(\d*)-base-arm64\.tar\.gz');
    String? best;
    var bestKey = -1;
    for (final m in re.allMatches(html)) {
      final key = int.parse(m.group(1)!) * 1000000 + int.parse(m.group(2)!) * 1000 + (int.tryParse(m.group(3) ?? '') ?? 0);
      if (key > bestKey) {
        bestKey = key;
        best = m.group(0);
      }
    }
    if (best == null) throw BootstrapError('Couldn’t find the Ubuntu download.');
    final f = File('${Linux.base}/dl/rootfs.tar.gz');
    await _download('$ubuntuDir$best', f, 'Downloading Ubuntu');
    report('Unpacking Ubuntu (this takes a minute)…', null);
    final dir = Directory(Linux.rootfs);
    if (dir.existsSync()) dir.deleteSync(recursive: true);
    dir.createSync(recursive: true);
    ProcessResult r;
    try {
      r = await Process.run('/system/bin/tar', ['-xzf', f.path, '-C', Linux.rootfs]);
    } on ProcessException {
      r = await Process.run('tar', ['-xzf', f.path, '-C', Linux.rootfs]);
    }
    // tar may complain about a few special files; what matters is that the system is there.
    if (!Linux.rootfsInstalled) throw BootstrapError('Unpacking Ubuntu failed.\n${r.stderr}');
    f.deleteSync();
  }

  // ------------------------------------------------------------------ config

  /// Writes the small files the Linux needs. Safe to run again (the setup script is refreshed each time).
  static Future<void> writeConfig() async {
    final r = Linux.rootfs;
    for (final d in ['proc', 'sys', 'dev', 'dev/shm', 'tmp', 'root', 'root/projects', 'root/.wzh', 'system', 'apex', 'linkerconfig', 'sdcard', 'opt/wzh-native', 'run', 'etc/apt/apt.conf.d', 'root/.gradle', 'usr/local/bin']) {
      Directory('$r/$d').createSync(recursive: true);
    }
    Directory('${Linux.tmp}/shm').createSync(recursive: true);
    void put(String path, String text) {
      final f = File('$r/$path');
      if (FileSystemEntity.isLinkSync(f.path)) Link(f.path).deleteSync();
      f.parent.createSync(recursive: true);
      f.writeAsStringSync(text);
    }

    put('etc/resolv.conf', 'nameserver 8.8.8.8\nnameserver 1.1.1.1\n');
    put('etc/hosts', '127.0.0.1 localhost\n::1 localhost\n');
    put('etc/apt/apt.conf.d/99wzh', 'APT::Sandbox::User "root";\nAcquire::Retries "3";\nDpkg::Options { "--force-unsafe-io"; };\n');
    put('root/.wzhenv', wzhEnv);
    put('root/.profile', 'source /root/.wzhenv\n[ -f ~/.bashrc ] && source ~/.bashrc\n');
    final rc = File('$r/root/.bashrc');
    final old = rc.existsSync() ? rc.readAsStringSync() : '';
    if (!old.contains('.wzhenv')) rc.writeAsStringSync('$old\nsource /root/.wzhenv\n');
    put('root/wzh-setup.sh', setupScript);
    // /proc files Android hides from apps; the JVM and Gradle read some of them.
    void fake(String n, String t) => File('${Linux.fake}/$n').writeAsStringSync(t);
    fake('stat', 'cpu  1000 0 1000 100000 0 0 0 0 0 0\ncpu0 1000 0 1000 100000 0 0 0 0 0 0\nintr 0\nctxt 0\nbtime 1700000000\nprocesses 1\nprocs_running 1\nprocs_blocked 0\n');
    fake('loadavg', '0.10 0.10 0.10 1/100 1\n');
    fake('uptime', '10000.00 10000.00\n');
    fake('version', 'Linux version 6.1.0 (wzh@ide) (gcc 12) #1 SMP PREEMPT\n');
    fake('vmstat', 'nr_free_pages 100000\n');
  }
}
