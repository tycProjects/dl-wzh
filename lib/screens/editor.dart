import 'package:flutter/material.dart';
import 'package:flutter/scheduler.dart';
import 'package:image_picker/image_picker.dart';
import '../editor_controller.dart';
import '../model.dart';
import '../renderer.dart';
import '../storage.dart';
import '../theme.dart';
import '../widgets/canvas_view.dart';
import '../widgets/common.dart';
import '../widgets/props_panel.dart';
import '../widgets/timeline.dart';
import 'export_dialog.dart';

class EditorScreen extends StatefulWidget {
  final Project project;
  const EditorScreen({super.key, required this.project});
  @override
  State<EditorScreen> createState() => _EditorScreenState();
}

class _EditorScreenState extends State<EditorScreen> with SingleTickerProviderStateMixin {
  late final EditorController c = EditorController(widget.project);
  late final Ticker _ticker = createTicker(_tick);
  Duration _last = Duration.zero;

  @override
  void initState() {
    super.initState();
    c.addListener(_sync);
    for (final l in widget.project.layers) {
      if (l.imagePath != null) {
        ImageStore.load(l.imagePath!).then((_) {
          if (mounted) c.refresh();
        });
      }
    }
  }

  void _sync() {
    if (c.playing && !_ticker.isActive) {
      _last = Duration.zero;
      _ticker.start();
    } else if (!c.playing && _ticker.isActive) {
      _ticker.stop();
    }
  }

  void _tick(Duration d) {
    final dt = (d - _last).inMicroseconds / 1e6;
    _last = d;
    c.advance(dt);
  }

  @override
  void dispose() {
    _ticker.dispose();
    c.saveNow();
    c.dispose();
    super.dispose();
  }

  void _addSheet() {
    showModalBottomSheet(
      context: context,
      backgroundColor: C.sheet,
      builder: (ctx) => SafeArea(
        child: Wrap(children: [
          ListTile(
            leading: const Icon(Icons.crop_square),
            title: const Text('Rectangle'),
            onTap: () {
              Navigator.pop(ctx);
              c.addShape(LayerType.rect);
            },
          ),
          ListTile(
            leading: const Icon(Icons.circle_outlined),
            title: const Text('Ellipse'),
            onTap: () {
              Navigator.pop(ctx);
              c.addShape(LayerType.ellipse);
            },
          ),
          ListTile(
            leading: const Icon(Icons.title),
            title: const Text('Text'),
            onTap: () {
              Navigator.pop(ctx);
              c.addText();
            },
          ),
          ListTile(
            leading: const Icon(Icons.image_outlined),
            title: const Text('Image'),
            onTap: () async {
              Navigator.pop(ctx);
              final x = await ImagePicker().pickImage(source: ImageSource.gallery);
              if (x != null) await c.addImage(await Store.importImage(x.path));
            },
          ),
        ]),
      ),
    );
  }

  void _export() {
    c.pause();
    showDialog(
      context: context,
      barrierDismissible: false,
      builder: (_) => ExportDialog(project: c.project),
    );
  }

  @override
  Widget build(BuildContext context) => Scaffold(
        body: SafeArea(
          child: Column(children: [
            _topBar(),
            Expanded(flex: 5, child: CanvasView(c: c)),
            _transport(),
            Expanded(
              flex: 5,
              child: DefaultTabController(
                length: 2,
                child: Column(children: [
                  const TabBar(
                    indicatorColor: C.teal,
                    labelColor: C.teal,
                    unselectedLabelColor: C.muted,
                    dividerColor: C.line,
                    tabs: [Tab(text: 'Timeline', height: 38), Tab(text: 'Properties', height: 38)],
                  ),
                  Expanded(
                    child: TabBarView(
                      physics: const NeverScrollableScrollPhysics(),
                      children: [TimelineView(c: c), PropsPanel(c: c)],
                    ),
                  ),
                ]),
              ),
            ),
          ]),
        ),
      );

  Widget _topBar() => ListenableBuilder(
        listenable: c,
        builder: (context, _) => Padding(
          padding: const EdgeInsets.symmetric(horizontal: 4),
          child: Row(children: [
            IconButton(icon: const Icon(Icons.arrow_back_ios_new, size: 18), onPressed: () => Navigator.pop(context)),
            Expanded(child: Text(c.project.name, maxLines: 1, overflow: TextOverflow.ellipsis, style: const TextStyle(fontWeight: FontWeight.w600))),
            IconButton(icon: const Icon(Icons.undo), onPressed: c.canUndo ? c.undo : null),
            IconButton(icon: const Icon(Icons.redo), onPressed: c.canRedo ? c.redo : null),
            const SizedBox(width: 4),
            FilledButton(
              style: FilledButton.styleFrom(backgroundColor: C.teal, foregroundColor: C.onTeal, visualDensity: VisualDensity.compact),
              onPressed: _export,
              child: const Text('Export'),
            ),
            const SizedBox(width: 8),
          ]),
        ),
      );

  Widget _transport() => ListenableBuilder(
        listenable: Listenable.merge([c, c.timeN]),
        builder: (context, _) => SizedBox(
          height: 48,
          child: ListView(scrollDirection: Axis.horizontal, padding: const EdgeInsets.symmetric(horizontal: 8), children: [
            IconButton(icon: const Icon(Icons.skip_previous), onPressed: () => c.seek(0)),
            IconButton(icon: const Icon(Icons.chevron_left), onPressed: () => c.step(-1)),
            IconButton(
              icon: Icon(c.playing ? Icons.pause_circle_filled : Icons.play_circle_fill, size: 34, color: C.teal),
              onPressed: c.playing ? c.pause : c.play,
            ),
            IconButton(icon: const Icon(Icons.chevron_right), onPressed: () => c.step(1)),
            Center(
              child: Padding(
                padding: const EdgeInsets.symmetric(horizontal: 6),
                child: Text('${fmtTime(c.time)} / ${fmtTime(c.project.duration)}', style: const TextStyle(fontSize: 12, color: C.muted)),
              ),
            ),
            IconButton(icon: const Icon(Icons.add_circle, color: C.teal), tooltip: 'Add layer', onPressed: _addSheet),
            IconButton(icon: const Icon(Icons.content_cut), tooltip: 'Split at playhead', onPressed: c.split),
            IconButton(icon: const Icon(Icons.copy), tooltip: 'Duplicate', onPressed: c.duplicateSelected),
            IconButton(icon: const Icon(Icons.delete_outline), tooltip: 'Delete', onPressed: c.deleteSelected),
          ]),
        ),
      );
}
