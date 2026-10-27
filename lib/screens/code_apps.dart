import 'dart:convert';
import 'dart:io';
import 'package:flutter/cupertino.dart' show CupertinoPageRoute;
import 'package:flutter/foundation.dart';
import 'package:flutter/material.dart' hide Text;
import '../anim.dart';
import '../api.dart';
import '../appearance.dart';
import '../quick/quick_builder.dart' show quickPackageName;
import '../theme.dart';
import '../tr.dart';
import '../widgets.dart';
import 'gallery_picker.dart';
import 'permissions_page.dart';

/// Apps that belong to the code editor only. They are kept apart from the options on the
/// Build APK screen, so changing one never touches the other.
class CodeApp {
  final String id;
  String name, suffix, orientation; // orientation: auto | portrait | landscape
  String? logoPath;
  Map<String, bool> perms;

  CodeApp({required this.id, required this.name, this.suffix = '', this.orientation = 'auto', this.logoPath, Map<String, bool>? perms})
      : perms = perms ?? defaultPerms();

  /// The app's real ID, worked out the same way the build does it.
  String get package => quickPackageName(name, suffix);
  int get permCount => perms.values.where((v) => v).length;

  Map<String, dynamic> toJson() => {
        'id': id,
        'name': name,
        'suffix': suffix,
        'orientation': orientation,
        'logo': logoPath,
        'perms': selectedPerms(perms),
      };

  factory CodeApp.fromJson(Map<String, dynamic> j) {
    final pm = defaultPerms();
    final saved = j['perms'];
    if (saved is List) {
      final on = saved.map((e) => e.toString()).toSet();
      for (final k in pm.keys.toList()) {
        pm[k] = on.contains(k);
      }
    }
    return CodeApp(
      id: (j['id'] ?? '').toString(),
      name: (j['name'] ?? 'My app').toString(),
      suffix: (j['suffix'] ?? '').toString(),
      orientation: (j['orientation'] ?? 'auto').toString(),
      logoPath: j['logo'] as String?,
      perms: pm,
    );
  }
}

class CodeApps {
  CodeApps._() {
    _load();
  }
  static final CodeApps i = CodeApps._();

  static const _kApps = 'codeEditorApps', _kCurrent = 'codeEditorAppCurrent';
  final ValueNotifier<List<CodeApp>> apps = ValueNotifier(const []);
  String? currentId;

  CodeApp? get current {
    for (final a in apps.value) {
      if (a.id == currentId) return a;
    }
    return apps.value.isEmpty ? null : apps.value.first;
  }

  void _load() {
    try {
      final l = jsonDecode(Api.i.prefs.getString(_kApps) ?? '[]') as List;
      apps.value = [for (final e in l) CodeApp.fromJson(Map<String, dynamic>.from(e as Map))];
    } catch (_) {
      apps.value = const [];
    }
    currentId = Api.i.prefs.getString(_kCurrent);
  }

  void save() {
    Api.i.prefs.setString(_kApps, jsonEncode([for (final a in apps.value) a.toJson()]));
    final c = current;
    if (c != null) Api.i.prefs.setString(_kCurrent, c.id);
    apps.value = List.of(apps.value); // tell listeners
  }

  /// The first time the code editor builds, an app named after the project is made for it.
  CodeApp ensure(String projectName) => current ?? create(projectName);

  CodeApp create(String name) {
    final clean = name.trim().isEmpty ? 'My app' : name.trim();
    final a = CodeApp(id: DateTime.now().microsecondsSinceEpoch.toString(), name: clean);
    apps.value = [...apps.value, a];
    currentId = a.id;
    save();
    return a;
  }

  void select(CodeApp a) {
    currentId = a.id;
    save();
  }

  void remove(CodeApp a) {
    apps.value = [for (final x in apps.value) if (x.id != a.id) x];
    if (currentId == a.id) currentId = apps.value.isEmpty ? null : apps.value.first.id;
    Api.i.prefs.setString(_kApps, jsonEncode([for (final x in apps.value) x.toJson()]));
    if (currentId == null) {
      Api.i.prefs.remove(_kCurrent);
    } else {
      Api.i.prefs.setString(_kCurrent, currentId!);
    }
    apps.value = List.of(apps.value);
  }
}

const _ink = Color(0xFF0F2A24);

Widget _appIcon(CodeApp a, {double size = 46}) {
  final p = a.logoPath;
  final has = p != null && File(p).existsSync();
  return Container(
    width: size, height: size,
    clipBehavior: Clip.antiAlias,
    decoration: BoxDecoration(color: C.tealDeep, borderRadius: BorderRadius.circular(size * .3)),
    child: has
        ? Unfilter(child: Image.file(File(p), fit: BoxFit.cover, width: size, height: size))
        : Center(child: Text(a.name.trim().isEmpty ? '?' : a.name.trim().substring(0, 1).toUpperCase(), style: inter(size * .45, w: FontWeight.w700, c: C.teal))),
  );
}

/// The app the next code build is for: its icon, name, package and a way to switch or add another.
class CodeAppCard extends StatelessWidget {
  final CodeApp app;
  final VoidCallback onChanged;
  const CodeAppCard({super.key, required this.app, required this.onChanged});

  static const _orient = {'auto': 'Auto-rotate', 'portrait': 'Portrait', 'landscape': 'Landscape'};

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.all(14),
      decoration: BoxDecoration(color: C.field, borderRadius: BorderRadius.circular(22), border: Border.all(color: const Color(0xFF2E2E30))),
      child: Column(crossAxisAlignment: CrossAxisAlignment.stretch, children: [
        Row(children: [
          _appIcon(app),
          const SizedBox(width: 12),
          Expanded(
            child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
              Text(app.name, maxLines: 1, overflow: TextOverflow.ellipsis, style: inter(17, w: FontWeight.w700)),
              const SizedBox(height: 2),
              Text(app.package, maxLines: 1, overflow: TextOverflow.ellipsis, style: inter(12.5, c: C.muted)),
              const SizedBox(height: 2),
              Text('${_orient[app.orientation] ?? 'Auto-rotate'} \u00B7 ${app.permCount} permissions', style: inter(12.5, c: C.muted)),
            ]),
          ),
        ]),
        const SizedBox(height: 12),
        Row(children: [
          Expanded(child: _pill(Icons.tune_rounded, 'Settings', () async {
            await Navigator.of(context, rootNavigator: true).push(CupertinoPageRoute<void>(builder: (_) => CodeAppPage(app: app)));
            onChanged();
          })),
          const SizedBox(width: 10),
          Expanded(child: _pill(Icons.swap_horiz_rounded, 'Switch / new', () async {
            await showCodeAppsSheet(context);
            onChanged();
          })),
        ]),
      ]),
    );
  }

  Widget _pill(IconData icon, String label, VoidCallback onTap) => Pressable(
        onTap: onTap,
        scale: .97,
        child: Container(
          height: 42,
          decoration: BoxDecoration(color: C.tealDeep, borderRadius: BorderRadius.circular(21)),
          child: Row(mainAxisAlignment: MainAxisAlignment.center, children: [
            Icon(icon, size: 18, color: C.teal),
            const SizedBox(width: 6),
            Text(label, style: inter(14, w: FontWeight.w600, c: C.teal)),
          ]),
        ),
      );
}

/// Lists the code editor's apps. Tap one to use it, or make a new one.
Future<void> showCodeAppsSheet(BuildContext context) => showIosSheet<void>(context, (_) => const _CodeAppsSheet());

class _CodeAppsSheet extends StatelessWidget {
  const _CodeAppsSheet();

  @override
  Widget build(BuildContext context) {
    final store = CodeApps.i;
    return Container(
      width: double.infinity,
      constraints: BoxConstraints(maxHeight: MediaQuery.of(context).size.height * .8),
      padding: EdgeInsets.fromLTRB(16, 10, 16, 14 + MediaQuery.of(context).padding.bottom),
      decoration: const BoxDecoration(color: C.sheet, borderRadius: BorderRadius.vertical(top: Radius.circular(30))),
      child: ValueListenableBuilder<List<CodeApp>>(
        valueListenable: store.apps,
        builder: (ctx, list, _) => Column(mainAxisSize: MainAxisSize.min, crossAxisAlignment: CrossAxisAlignment.stretch, children: [
          Center(child: Container(width: 38, height: 5, decoration: BoxDecoration(color: const Color(0xFF4A4A4C), borderRadius: BorderRadius.circular(3)))),
          const SizedBox(height: 16),
          Padding(
            padding: const EdgeInsets.symmetric(horizontal: 4),
            child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
              Text('Your code apps', style: inter(20, w: FontWeight.w700)),
              const SizedBox(height: 1),
              Text('Only used by the code editor.', style: inter(13, c: C.muted)),
            ]),
          ),
          const SizedBox(height: 14),
          Flexible(
            child: ListView(
              shrinkWrap: true,
              physics: const BouncingScrollPhysics(),
              children: [
                for (final a in list)
                  Padding(
                    padding: const EdgeInsets.only(bottom: 10),
                    child: Pressable(
                      scale: .98,
                      onTap: () {
                        store.select(a);
                        Navigator.of(context, rootNavigator: true).pop();
                      },
                      child: Container(
                        padding: const EdgeInsets.all(12),
                        decoration: BoxDecoration(
                          color: C.field,
                          borderRadius: BorderRadius.circular(20),
                          border: Border.all(color: store.current?.id == a.id ? C.teal : const Color(0xFF2E2E30), width: store.current?.id == a.id ? 1.6 : 1),
                        ),
                        child: Row(children: [
                          _appIcon(a, size: 42),
                          const SizedBox(width: 12),
                          Expanded(
                            child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
                              Text(a.name, maxLines: 1, overflow: TextOverflow.ellipsis, style: inter(16, w: FontWeight.w700)),
                              Text(a.package, maxLines: 1, overflow: TextOverflow.ellipsis, style: inter(12.5, c: C.muted)),
                            ]),
                          ),
                          if (store.current?.id == a.id) const Icon(Icons.check_circle_rounded, size: 22, color: C.teal),
                        ]),
                      ),
                    ),
                  ),
              ],
            ),
          ),
          const SizedBox(height: 6),
          Pressable(
            onTap: () {
              final nav = Navigator.of(context, rootNavigator: true);
              final a = CodeApps.i.create('New app ${list.length + 1}');
              nav.pop();
              nav.push(CupertinoPageRoute<void>(builder: (_) => CodeAppPage(app: a, isNew: true)));
            },
            child: Container(
              height: 54,
              alignment: Alignment.center,
              decoration: BoxDecoration(color: C.tealBtn, borderRadius: BorderRadius.circular(27)),
              child: Row(mainAxisAlignment: MainAxisAlignment.center, children: [
                const Icon(Icons.add_rounded, size: 22, color: _ink),
                const SizedBox(width: 6),
                Text('New app', style: inter(17, w: FontWeight.w700, c: _ink)),
              ]),
            ),
          ),
          Pressable(
            onTap: () => Navigator.of(context, rootNavigator: true).pop(),
            child: SizedBox(height: 44, child: Center(child: Text('Close', style: inter(15, w: FontWeight.w600, c: C.muted)))),
          ),
        ]),
      ),
    );
  }
}

/// One code app's own settings: name, package, icon, orientation and permissions.
class CodeAppPage extends StatefulWidget {
  final CodeApp app;
  final bool isNew;
  const CodeAppPage({super.key, required this.app, this.isNew = false});
  @override
  State<CodeAppPage> createState() => _CodeAppPageState();
}

class _CodeAppPageState extends State<CodeAppPage> {
  late final TextEditingController name = TextEditingController(text: widget.app.name);
  late final TextEditingController suffix = TextEditingController(text: widget.app.suffix);
  CodeApp get app => widget.app;

  @override
  void dispose() {
    name.dispose();
    suffix.dispose();
    super.dispose();
  }

  void _changed() {
    app.name = name.text;
    app.suffix = suffix.text;
    CodeApps.i.save();
    setState(() {});
  }

  Future<void> _pickLogo() async {
    final f = await pickIconImage(context);
    if (f == null || !mounted) return;
    PaintingBinding.instance.imageCache.clear();
    PaintingBinding.instance.imageCache.clearLiveImages();
    app.logoPath = f.path;
    CodeApps.i.save();
    setState(() {});
  }

  Future<void> _delete() async {
    CodeApps.i.remove(app);
    if (mounted) Navigator.of(context).maybePop();
  }

  Widget _box(String label, TextEditingController c, String hint, {String? desc}) => Container(
        padding: const EdgeInsets.fromLTRB(16, 12, 16, 14),
        decoration: BoxDecoration(color: C.card, borderRadius: BorderRadius.circular(20)),
        child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
          Text(label, style: inter(13, w: FontWeight.w600, c: C.muted)),
          const SizedBox(height: 6),
          TextField(
            controller: c,
            scrollPadding: const EdgeInsets.fromLTRB(20, 20, 20, 120),
            onChanged: (_) => _changed(),
            style: inter(17),
            cursorColor: C.teal,
            decoration: InputDecoration(border: InputBorder.none, isCollapsed: true, hintText: hint, hintStyle: inter(17, c: const Color(0xFF6E6E73))),
          ),
          if (desc != null) ...[const SizedBox(height: 6), Text(desc, style: inter(13, c: C.muted, h: 1.3))],
        ]),
      );

  Widget _row(IconData icon, String title, String sub, VoidCallback onTap, {Widget? lead}) => Pressable(
        onTap: onTap,
        scale: .98,
        child: Container(
          padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 12),
          decoration: BoxDecoration(color: C.card, borderRadius: BorderRadius.circular(20)),
          child: Row(children: [
            lead ?? Container(
              width: 40, height: 40,
              decoration: BoxDecoration(color: C.tealDeep, borderRadius: BorderRadius.circular(12)),
              child: Icon(icon, size: 22, color: C.teal),
            ),
            const SizedBox(width: 14),
            Expanded(
              child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
                Text(title, style: inter(17)),
                Text(sub, style: inter(13, c: C.muted)),
              ]),
            ),
            const Icon(Icons.arrow_forward_ios_rounded, size: 14, color: Color(0xFF6A6A6E)),
          ]),
        ),
      );

  Widget _orientChip(String code, String label) {
    final on = app.orientation == code;
    return Expanded(
      child: Pressable(
        scale: .96,
        onTap: () {
          app.orientation = code;
          CodeApps.i.save();
          setState(() {});
        },
        child: Container(
          height: 44,
          alignment: Alignment.center,
          decoration: BoxDecoration(
            color: on ? C.tealBtn : C.card,
            borderRadius: BorderRadius.circular(14),
            border: Border.all(color: on ? C.tealBtn : const Color(0xFF2E2E30)),
          ),
          child: Text(label, style: inter(14, w: FontWeight.w600, c: on ? _ink : C.text)),
        ),
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      body: SafeArea(
        child: Column(children: [
          TopBar(title: widget.isNew ? 'New app' : 'App settings', back: true),
          Expanded(
            child: ListView(
              physics: const BouncingScrollPhysics(parent: AlwaysScrollableScrollPhysics()),
              padding: const EdgeInsets.fromLTRB(18, 16, 18, 40),
              children: [
                _box('App name', name, 'App name'),
                const SizedBox(height: 12),
                _box('Package ID suffix', suffix, 'e.g. myshop.app', desc: 'Final ID: ${app.package}'),
                const SizedBox(height: 12),
                _row(Icons.image_rounded, 'App icon', app.logoPath == null ? 'Using the project\u2019s own icon' : 'Custom icon chosen', _pickLogo, lead: app.logoPath == null ? null : _appIcon(app, size: 40)),
                const SizedBox(height: 12),
                Row(children: [
                  _orientChip('auto', 'Auto'),
                  const SizedBox(width: 8),
                  _orientChip('portrait', 'Portrait'),
                  const SizedBox(width: 8),
                  _orientChip('landscape', 'Landscape'),
                ]),
                const SizedBox(height: 12),
                _row(Icons.verified_user_rounded, 'Permissions', '${app.permCount} on', () async {
                  await Navigator.of(context).push(PermissionsPage.route(app.perms));
                  CodeApps.i.save();
                  if (mounted) setState(() {});
                }),
                const SizedBox(height: 20),
                Pressable(
                  onTap: _delete,
                  child: SizedBox(height: 48, child: Center(child: Text('Delete this app', style: inter(16, w: FontWeight.w600, c: C.red)))),
                ),
              ],
            ),
          ),
        ]),
      ),
    );
  }
}
