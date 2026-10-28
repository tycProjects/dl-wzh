import 'dart:io';
import 'package:archive/archive.dart';
import 'package:file_selector/file_selector.dart';
import 'package:flutter/cupertino.dart' show CupertinoPageRoute;
import 'package:flutter/material.dart';
import '../banner.dart';
import '../core/linux.dart';
import '../theme.dart';
import 'ide_screen.dart';
import 'terminal_panel.dart';

class ProjectsScreen extends StatefulWidget {
  const ProjectsScreen({super.key});
  @override
  State<ProjectsScreen> createState() => _ProjectsScreenState();
}

class _ProjectsScreenState extends State<ProjectsScreen> {
  List<Directory> items = [];

  @override
  void initState() {
    super.initState();
    _refresh();
  }

  void _refresh() {
    final d = Directory(Linux.projectsDir);
    d.createSync(recursive: true);
    final l = d.listSync().whereType<Directory>().where((x) => File('${x.path}/pubspec.yaml').existsSync()).toList()
      ..sort((a, b) => b.statSync().modified.compareTo(a.statSync().modified));
    setState(() => items = l);
  }

  String _name(Directory d) => d.uri.pathSegments.where((s) => s.isNotEmpty).last;

  Future<void> _open(String name, {bool create = false}) async {
    await Navigator.of(context).push(CupertinoPageRoute<void>(builder: (_) => IdeScreen(name: name, create: create)));
    if (mounted) _refresh();
  }

  /// Dart package names: lower case letters, digits and underscores, starting with a letter.
  static String? _bad(String n) {
    if (!RegExp(r'^[a-z][a-z0-9_]*$').hasMatch(n)) return 'Use lower case letters, digits and _ (start with a letter).';
    if (const {'test', 'flutter', 'dart', 'async', 'math', 'io', 'ui'}.contains(n)) return 'That name is reserved by Dart.';
    return null;
  }

  Future<String?> _ask(String title, String hint, String action) {
    final c = TextEditingController();
    String? err;
    return showDialog<String>(
      context: context,
      builder: (ctx) => StatefulBuilder(
        builder: (ctx, set) => AlertDialog(
          backgroundColor: C.sheet,
          title: Text(title, style: inter(18, w: FontWeight.w700)),
          content: TextField(
            controller: c,
            autofocus: true,
            style: inter(16),
            decoration: InputDecoration(hintText: hint, errorText: err, errorMaxLines: 3),
          ),
          actions: [
            TextButton(onPressed: () => Navigator.pop(ctx), child: Text('Cancel', style: inter(15, c: C.muted))),
            TextButton(
              onPressed: () {
                final v = c.text.trim();
                final e = _bad(v);
                if (e != null) return set(() => err = e);
                if (Directory('${Linux.projectsDir}/$v').existsSync()) return set(() => err = 'A project with that name already exists.');
                Navigator.pop(ctx, v);
              },
              child: Text(action, style: inter(15, w: FontWeight.w700, c: C.teal)),
            ),
          ],
        ),
      ),
    );
  }

  Future<void> _new() async {
    final n = await _ask('New Flutter project', 'my_app', 'Create');
    if (n == null || !mounted) return;
    await _open(n, create: true);
  }

  Future<void> _import() async {
    final f = await openFile(acceptedTypeGroups: [const XTypeGroup(label: 'Project zip', extensions: ['zip'])]);
    if (f == null || !mounted) return;
    try {
      var stem = f.name.replaceAll(RegExp(r'\.zip$', caseSensitive: false), '').toLowerCase().replaceAll(RegExp(r'[^a-z0-9_]'), '_');
      if (stem.isEmpty || !RegExp(r'^[a-z]').hasMatch(stem)) stem = 'p_$stem';
      var name = stem, i = 2;
      while (Directory('${Linux.projectsDir}/$name').existsSync()) {
        name = '${stem}_${i++}';
      }
      final z = ZipDecoder().decodeBytes(await f.readAsBytes());
      // A zip made of one top folder (GitHub style) is unwrapped.
      final tops = z.files.map((e) => e.name.split('/').first).toSet();
      final strip = tops.length == 1 && z.files.any((e) => e.name.contains('/')) ? '${tops.first}/' : '';
      for (final e in z.files) {
        if (!e.isFile) continue;
        var n = e.name;
        if (strip.isNotEmpty && n.startsWith(strip)) n = n.substring(strip.length);
        if (n.isEmpty || n.contains('..')) continue;
        final out = File('${Linux.projectsDir}/$name/$n');
        out.parent.createSync(recursive: true);
        out.writeAsBytesSync(e.content as List<int>);
      }
      if (!File('${Linux.projectsDir}/$name/pubspec.yaml').existsSync()) {
        Directory('${Linux.projectsDir}/$name').deleteSync(recursive: true);
        if (mounted) IosBanner.show(context, kind: BannerKind.err, title: 'Not a Flutter project', msg: 'No pubspec.yaml in that zip.');
        return;
      }
      _refresh();
      if (mounted) IosBanner.show(context, kind: BannerKind.ok, title: 'Imported', msg: name);
    } catch (e) {
      if (mounted) IosBanner.show(context, kind: BannerKind.err, title: 'Couldn’t import that zip', msg: '$e');
    }
  }

  Future<void> _delete(Directory d) async {
    final ok = await showDialog<bool>(
      context: context,
      builder: (ctx) => AlertDialog(
        backgroundColor: C.sheet,
        title: Text('Delete ${_name(d)}?', style: inter(18, w: FontWeight.w700)),
        content: Text('The project and its files are removed from the phone.', style: inter(14.5, c: C.muted)),
        actions: [
          TextButton(onPressed: () => Navigator.pop(ctx, false), child: Text('Cancel', style: inter(15, c: C.muted))),
          TextButton(onPressed: () => Navigator.pop(ctx, true), child: Text('Delete', style: inter(15, w: FontWeight.w700, c: C.red))),
        ],
      ),
    );
    if (ok != true) return;
    try {
      d.deleteSync(recursive: true);
    } catch (_) {}
    _refresh();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: C.bg,
      appBar: AppBar(
        backgroundColor: C.bg,
        title: Text('Projects', style: inter(22, w: FontWeight.w800)),
        actions: [
          IconButton(tooltip: 'Import zip', icon: const Icon(Icons.folder_zip_outlined), onPressed: _import),
          IconButton(
            tooltip: 'Terminal',
            icon: const Icon(Icons.terminal_rounded),
            onPressed: () => Navigator.of(context).push(CupertinoPageRoute<void>(builder: (_) => const TerminalScreen())),
          ),
        ],
      ),
      floatingActionButton: FloatingActionButton.extended(
        onPressed: _new,
        backgroundColor: C.tealBtn,
        foregroundColor: C.onTeal,
        icon: const Icon(Icons.add_rounded),
        label: Text('New project', style: inter(15, w: FontWeight.w700, c: C.onTeal)),
      ),
      body: items.isEmpty
          ? Center(
              child: Padding(
                padding: const EdgeInsets.symmetric(horizontal: 32),
                child: Column(mainAxisSize: MainAxisSize.min, children: [
                  const Icon(Icons.flutter_dash, size: 56, color: C.teal),
                  const SizedBox(height: 14),
                  Text('No projects yet', style: inter(19, w: FontWeight.w700)),
                  const SizedBox(height: 6),
                  Text('Tap New project to start a Flutter app, or import a zip.', textAlign: TextAlign.center, style: inter(14, c: C.muted, h: 1.4)),
                ]),
              ),
            )
          : ListView.builder(
              padding: const EdgeInsets.fromLTRB(12, 4, 12, 96),
              itemCount: items.length,
              itemBuilder: (_, i) {
                final d = items[i];
                final n = _name(d);
                return Card(
                  color: C.card,
                  margin: const EdgeInsets.symmetric(vertical: 4),
                  shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(14)),
                  child: ListTile(
                    leading: const Icon(Icons.flutter_dash, color: C.teal),
                    title: Text(n, style: inter(16, w: FontWeight.w700)),
                    subtitle: Text('Edited ${_ago(d.statSync().modified)}', style: inter(12.5, c: C.muted)),
                    trailing: IconButton(icon: const Icon(Icons.delete_outline_rounded, color: C.muted), onPressed: () => _delete(d)),
                    onTap: () => _open(n),
                  ),
                );
              },
            ),
    );
  }

  static String _ago(DateTime t) {
    final d = DateTime.now().difference(t);
    if (d.inMinutes < 1) return 'just now';
    if (d.inHours < 1) return '${d.inMinutes} min ago';
    if (d.inDays < 1) return '${d.inHours} h ago';
    return '${d.inDays} d ago';
  }
}
