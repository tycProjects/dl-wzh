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
import '../widgets/tools_panel.dart';
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
  final ValueNotifier<String?> page = ValueNotifier(null);

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
    if (c.selected == null && page.value != null) page.value = null;
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
    page.dispose();
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

  void _settings() {
    showDialog(
      context: context,
      builder: (ctx) => ListenableBuilder(
        listenable: c,
        builder: (context, _) => AlertDialog(
          backgroundColor: C.sheet,
          title: const Text('Project settings'),
          content: Column(mainAxisSize: MainAxisSize.min, crossAxisAlignment: CrossAxisAlignment.start, children: [
            Row(children: [
              const Text('Duration'),
              const Spacer(),
              IconButton(icon: const Icon(Icons.remove), onPressed: () => c.setDuration(c.project.duration - 1)),
              Text('${c.project.duration.toStringAsFixed(0)}s'),
              IconButton(icon: const Icon(Icons.add), onPressed: () => c.setDuration(c.project.duration + 1)),
            ]),
            const SizedBox(height: 8),
            const Text('Background', style: TextStyle(color: C.muted, fontSize: 12)),
            const SizedBox(height: 8),
            Swatches(current: c.project.bg, onPick: c.setBackground),
          ]),
          actions: [TextButton(onPressed: () => Navigator.pop(ctx), child: const Text('Done'))],
        ),
      ),
    );
  }

  @override
  Widget build(BuildContext context) => Scaffold(
        body: SafeArea(
          child: Column(children: [
            _topBar(),
            Expanded(child: CanvasView(c: c)),
            _transport(),
            ValueListenableBuilder<String?>(
              valueListenable: page,
              builder: (context, pg, _) => ListenableBuilder(
                listenable: c,
                builder: (context, _) {
                  final sel = c.selected != null;
                  final panelH = pg == null ? 156.0 : (pg == 'move' ? 250.0 : 190.0);
                  return Column(children: [
                    SizedBox(height: sel ? 118 : 230, child: TimelineView(c: c, onAdd: _addSheet)),
                    if (sel) SizedBox(height: panelH, child: ToolsPanel(c: c, page: page)),
                  ]);
                },
              ),
            ),
          ]),
        ),
      );

  Widget _topBar() => ListenableBuilder(
        listenable: c,
        builder: (context, _) {
          final l = c.selected;
          return SizedBox(
            height: 52,
            child: Row(children: [
              IconButton(icon: const Icon(Icons.arrow_back_ios_new, size: 18), onPressed: () => Navigator.pop(context)),
              Expanded(
                child: Text(l?.name ?? c.project.name, maxLines: 1, overflow: TextOverflow.ellipsis, style: const TextStyle(fontWeight: FontWeight.w600)),
              ),
              if (l != null) ...[
                IconButton(icon: const Icon(Icons.copy), tooltip: 'Duplicate', onPressed: c.duplicateSelected),
                IconButton(icon: const Icon(Icons.delete_outline), tooltip: 'Delete', onPressed: c.deleteSelected),
              ] else ...[
                IconButton(icon: const Icon(Icons.settings_outlined), tooltip: 'Project settings', onPressed: _settings),
                IconButton(icon: const Icon(Icons.file_upload_outlined), tooltip: 'Export', onPressed: _export),
              ],
              const SizedBox(width: 4),
            ]),
          );
        },
      );

  Widget _transport() => ListenableBuilder(
        listenable: c,
        builder: (context, _) => SizedBox(
          height: 44,
          child: Row(mainAxisAlignment: MainAxisAlignment.spaceEvenly, children: [
            IconButton(icon: const Icon(Icons.undo), onPressed: c.canUndo ? c.undo : null),
            IconButton(icon: const Icon(Icons.redo), onPressed: c.canRedo ? c.redo : null),
            IconButton(icon: const Icon(Icons.skip_previous), onPressed: () => c.seek(0)),
            IconButton(
              icon: Icon(c.playing ? Icons.pause_circle_outline : Icons.play_circle_outline, size: 32, color: C.teal),
              onPressed: c.playing ? c.pause : c.play,
            ),
            IconButton(icon: const Icon(Icons.skip_next), onPressed: () => c.seek(c.project.duration)),
            IconButton(icon: const Icon(Icons.content_cut), tooltip: 'Split at playhead', onPressed: c.split),
          ]),
        ),
      );
}
