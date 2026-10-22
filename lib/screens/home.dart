import 'package:flutter/material.dart';
import '../model.dart';
import '../renderer.dart';
import '../storage.dart';
import '../theme.dart';
import '../widgets/common.dart';
import 'editor.dart';

class HomeScreen extends StatefulWidget {
  const HomeScreen({super.key});
  @override
  State<HomeScreen> createState() => _HomeScreenState();
}

class _HomeScreenState extends State<HomeScreen> {
  List<Project> items = [];
  bool loading = true;

  @override
  void initState() {
    super.initState();
    _load();
  }

  Future<void> _load() async {
    final list = await Store.list();
    for (final p in list) {
      for (final l in p.layers) {
        if (l.imagePath != null) await ImageStore.load(l.imagePath!);
      }
    }
    if (!mounted) return;
    setState(() {
      items = list;
      loading = false;
    });
  }

  Future<void> _open(Project p) async {
    await Navigator.push(context, MaterialPageRoute(builder: (_) => EditorScreen(project: p)));
    _load();
  }

  void _new() {
    const presets = [
      ['Vertical 9:16', 1080, 1920],
      ['Landscape 16:9', 1920, 1080],
      ['Square 1:1', 1080, 1080],
      ['Portrait 4:5', 1080, 1350],
    ];
    showModalBottomSheet(
      context: context,
      backgroundColor: C.sheet,
      builder: (ctx) => SafeArea(
        child: Column(mainAxisSize: MainAxisSize.min, children: [
          const Padding(
            padding: EdgeInsets.all(16),
            child: Text('New project', style: TextStyle(fontSize: 18, fontWeight: FontWeight.w700)),
          ),
          for (final pr in presets)
            ListTile(
              title: Text(pr[0] as String),
              subtitle: Text('${pr[1]} x ${pr[2]} · 30 fps', style: const TextStyle(color: C.muted)),
              onTap: () async {
                Navigator.pop(ctx);
                final p = Project(
                  id: newId(),
                  name: 'Project ${items.length + 1}',
                  width: pr[1] as int,
                  height: pr[2] as int,
                );
                await Store.save(p);
                _open(p);
              },
            ),
        ]),
      ),
    );
  }

  @override
  Widget build(BuildContext context) => Scaffold(
        appBar: AppBar(title: const Text('Projects')),
        floatingActionButton: FloatingActionButton.extended(
          onPressed: _new,
          backgroundColor: C.teal,
          foregroundColor: C.onTeal,
          icon: const Icon(Icons.add),
          label: const Text('New project'),
        ),
        body: loading
            ? const Center(child: CircularProgressIndicator())
            : items.isEmpty
                ? const Center(child: Text('No projects yet', style: TextStyle(color: C.muted)))
                : GridView.builder(
                    padding: const EdgeInsets.fromLTRB(16, 8, 16, 96),
                    gridDelegate: const SliverGridDelegateWithFixedCrossAxisCount(
                      crossAxisCount: 2,
                      mainAxisSpacing: 14,
                      crossAxisSpacing: 14,
                      childAspectRatio: 0.8,
                    ),
                    itemCount: items.length,
                    itemBuilder: (_, i) => _card(items[i]),
                  ),
      );

  Widget _card(Project p) => GestureDetector(
        onTap: () => _open(p),
        onLongPress: () async {
          final ok = await showDialog<bool>(
            context: context,
            builder: (ctx) => AlertDialog(
              backgroundColor: C.sheet,
              title: Text('Delete "${p.name}"?'),
              actions: [
                TextButton(onPressed: () => Navigator.pop(ctx, false), child: const Text('Cancel')),
                TextButton(onPressed: () => Navigator.pop(ctx, true), child: const Text('Delete', style: TextStyle(color: C.red))),
              ],
            ),
          );
          if (ok == true) {
            await Store.delete(p.id);
            _load();
          }
        },
        child: Container(
          decoration: BoxDecoration(color: C.card, borderRadius: BorderRadius.circular(14)),
          padding: const EdgeInsets.all(10),
          child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
            Expanded(
              child: Center(
                child: AspectRatio(
                  aspectRatio: p.width / p.height,
                  child: CustomPaint(painter: _ThumbPainter(p)),
                ),
              ),
            ),
            const SizedBox(height: 8),
            Text(p.name, maxLines: 1, overflow: TextOverflow.ellipsis, style: const TextStyle(fontWeight: FontWeight.w600)),
            Text('${p.width}x${p.height} · ${fmtTime(p.duration)}', style: const TextStyle(color: C.muted, fontSize: 12)),
          ]),
        ),
      );
}

class _ThumbPainter extends CustomPainter {
  final Project p;
  _ThumbPainter(this.p);
  @override
  void paint(Canvas canvas, Size size) => SceneRenderer.paint(canvas, size, p, 0);
  @override
  bool shouldRepaint(covariant _ThumbPainter old) => true;
}
