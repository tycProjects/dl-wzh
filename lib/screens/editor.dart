import 'dart:math' as math;
import 'package:flutter/material.dart';
import 'package:flutter/scheduler.dart';
import 'package:photo_manager/photo_manager.dart';
import '../editor_controller.dart';
import '../media.dart';
import '../model.dart';
import '../renderer.dart';
import '../storage.dart';
import '../theme.dart';
import '../widgets/canvas_view.dart';
import '../widgets/common.dart';
import '../widgets/tools_panel.dart';
import '../widgets/timeline.dart';
import 'export_dialog.dart';
import 'media_picker.dart';

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
      if (l.type == LayerType.video && l.videoDir != null) {
        VideoStore.ensure(l, l.start).then((_) {
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

  Future<void> _pickMedia() async {
    final res = await Navigator.push<List<AssetEntity>>(
      context,
      MaterialPageRoute(fullscreenDialog: true, builder: (_) => const MediaPicker()),
    );
    if (res == null || res.isEmpty || !mounted) return;
    final msg = ValueNotifier<String>('Preparing...');
    showDialog(context: context, barrierDismissible: false, builder: (_) => _BusyDialog(msg: msg));
    final t0 = c.time;
    var failed = 0;
    for (var i = 0; i < res.length; i++) {
      final a = res[i];
      final isVideo = a.type == AssetType.video;
      msg.value = 'Importing ${i + 1} of ${res.length}${isVideo ? ' - video, this can take a bit' : ''}';
      try {
        final f = await a.file;
        if (f == null) throw Exception('no file');
        if (isVideo) {
          c.addVideo(await MediaImport.video(f.path, c.project.fps));
        } else {
          await c.addImage(await Store.importImage(f.path));
          if (res.length > 1) c.edit((l) => l.end = math.min(l.end, l.start + 3), cp: false);
        }
        // several picks: lay them out one after another
        if (res.length > 1 && c.selected != null) c.seek(c.selected!.end);
      } catch (_) {
        failed++;
      }
    }
    c.seek(t0);
    if (!mounted) return;
    Navigator.of(context, rootNavigator: true).pop();
    if (failed > 0) {
      ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text('$failed item(s) could not be imported')));
    }
  }

  void _addSheet() {
    showModalBottomSheet(
      context: context,
      backgroundColor: C.sheet,
      builder: (ctx) => SafeArea(
        child: Wrap(children: [
          ListTile(
            leading: const Icon(Icons.video_library_outlined),
            title: const Text('Media (photos & videos)'),
            onTap: () {
              Navigator.pop(ctx);
              _pickMedia();
            },
          ),
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
                  final panelH = pg == null ? 156.0 : (pg == 'move' || pg == 'adjust' ? 250.0 : (pg == 'effects' || pg == 'anim' ? 240.0 : 190.0));
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

class _BusyDialog extends StatelessWidget {
  final ValueNotifier<String> msg;
  const _BusyDialog({required this.msg});
  @override
  Widget build(BuildContext context) => PopScope(
        canPop: false,
        child: AlertDialog(
          backgroundColor: C.sheet,
          content: Row(children: [
            const SizedBox(width: 24, height: 24, child: CircularProgressIndicator(strokeWidth: 3)),
            const SizedBox(width: 16),
            Expanded(child: ValueListenableBuilder<String>(valueListenable: msg, builder: (_, m, __) => Text(m))),
          ]),
        ),
      );
}
