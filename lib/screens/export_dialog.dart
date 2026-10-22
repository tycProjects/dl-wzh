import 'package:flutter/material.dart';
import '../export.dart';
import '../model.dart';
import '../theme.dart';

class ExportDialog extends StatefulWidget {
  final Project project;
  const ExportDialog({super.key, required this.project});
  @override
  State<ExportDialog> createState() => _ExportDialogState();
}

class _ExportDialogState extends State<ExportDialog> {
  double scale = 1;
  bool running = false, done = false;
  double prog = 0;
  String msg = '';
  String? error;

  Future<void> _start() async {
    setState(() {
      running = true;
      error = null;
    });
    Exporter.cancel = false;
    try {
      final out = await Exporter.run(widget.project, scale, (p, m) {
        if (mounted) {
          setState(() {
            prog = p;
            msg = m;
          });
        }
      });
      if (!mounted) return;
      setState(() {
        running = false;
        done = out != null;
        msg = out == null ? 'Cancelled' : 'Saved to your gallery';
      });
    } catch (e) {
      if (mounted) {
        setState(() {
          running = false;
          error = '$e';
        });
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    final p = widget.project;
    return AlertDialog(
      backgroundColor: C.sheet,
      title: const Text('Export MP4'),
      content: Column(mainAxisSize: MainAxisSize.min, crossAxisAlignment: CrossAxisAlignment.start, children: [
        if (running || done || msg.isNotEmpty) ...[
          LinearProgressIndicator(value: prog, color: C.teal),
          const SizedBox(height: 10),
          Text(msg, style: const TextStyle(color: C.muted)),
        ] else ...[
          Text('${p.fps} fps · ${p.duration.toStringAsFixed(0)}s', style: const TextStyle(color: C.muted)),
          const SizedBox(height: 12),
          Wrap(spacing: 8, children: [
            for (final s in [0.5, 0.75, 1.0])
              ChoiceChip(
                label: Text('${Exporter.evenSize(p.width, s)}x${Exporter.evenSize(p.height, s)}'),
                selected: scale == s,
                onSelected: (_) => setState(() => scale = s),
              ),
          ]),
        ],
        if (error != null) Padding(padding: const EdgeInsets.only(top: 10), child: Text(error!, style: const TextStyle(color: C.red, fontSize: 12))),
      ]),
      actions: [
        if (running)
          TextButton(onPressed: () => Exporter.cancel = true, child: const Text('Cancel'))
        else ...[
          TextButton(onPressed: () => Navigator.pop(context), child: Text(done ? 'Close' : 'Back')),
          if (!done) TextButton(onPressed: _start, child: const Text('Export', style: TextStyle(color: C.teal))),
        ],
      ],
    );
  }
}
