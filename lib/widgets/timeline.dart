import 'package:flutter/material.dart';
import '../editor_controller.dart';
import '../model.dart';
import '../theme.dart';
import 'common.dart';

class TimelineView extends StatefulWidget {
  final EditorController c;
  const TimelineView({super.key, required this.c});
  @override
  State<TimelineView> createState() => _TimelineViewState();
}

class _TimelineViewState extends State<TimelineView> {
  double pps = 90; // pixels per second
  static const rowH = 44.0, rulerH = 30.0;
  double _acc = 0, _orig = 0;

  EditorController get c => widget.c;

  @override
  Widget build(BuildContext context) => ListenableBuilder(
        listenable: c,
        builder: (context, _) {
          final p = c.project;
          final totalW = p.duration * pps + 160;
          final layers = p.layers.reversed.toList();
          return Column(children: [
            _toolbar(p),
            Expanded(
              child: SingleChildScrollView(
                scrollDirection: Axis.horizontal,
                child: SizedBox(
                  width: totalW,
                  child: Stack(children: [
                    Column(children: [
                      _ruler(p),
                      Expanded(
                        child: layers.isEmpty
                            ? const Center(child: Text('No layers yet - tap + to add one', style: TextStyle(color: C.muted)))
                            : ListView(children: [for (final l in layers) _row(l)]),
                      ),
                    ]),
                    ValueListenableBuilder<double>(
                      valueListenable: c.timeN,
                      builder: (_, t, __) => Positioned(
                        left: t * pps - 1,
                        top: 0,
                        bottom: 0,
                        child: IgnorePointer(child: Container(width: 2, color: C.red)),
                      ),
                    ),
                  ]),
                ),
              ),
            ),
          ]);
        },
      );

  Widget _toolbar(Project p) => Padding(
        padding: const EdgeInsets.symmetric(horizontal: 8),
        child: Row(children: [
          const Text('Duration', style: TextStyle(color: C.muted, fontSize: 12)),
          IconButton(
            visualDensity: VisualDensity.compact,
            icon: const Icon(Icons.remove, size: 18),
            onPressed: () => c.setDuration(p.duration - 1),
          ),
          Text('${p.duration.toStringAsFixed(0)}s', style: const TextStyle(fontSize: 13)),
          IconButton(
            visualDensity: VisualDensity.compact,
            icon: const Icon(Icons.add, size: 18),
            onPressed: () => c.setDuration(p.duration + 1),
          ),
          const Spacer(),
          IconButton(
            visualDensity: VisualDensity.compact,
            icon: const Icon(Icons.zoom_out, size: 20),
            onPressed: () => setState(() => pps = (pps / 1.4).clamp(30.0, 400.0)),
          ),
          IconButton(
            visualDensity: VisualDensity.compact,
            icon: const Icon(Icons.zoom_in, size: 20),
            onPressed: () => setState(() => pps = (pps * 1.4).clamp(30.0, 400.0)),
          ),
        ]),
      );

  Widget _ruler(Project p) => GestureDetector(
        behavior: HitTestBehavior.opaque,
        onTapDown: (d) => c.seek(d.localPosition.dx / pps),
        onHorizontalDragUpdate: (d) => c.seek(d.localPosition.dx / pps),
        child: SizedBox(
          height: rulerH,
          width: double.infinity,
          child: CustomPaint(painter: _RulerPainter(pps, p.duration)),
        ),
      );

  List<double> _keyTimes(Layer l) => {
        for (final p in l.props.values)
          for (final k in p.keys) k.t
      }.toList();

  Widget _row(Layer l) {
    final sel = c.selectedId == l.id;
    final width = ((l.end - l.start) * pps).clamp(12.0, double.infinity).toDouble();
    return SizedBox(
      height: rowH,
      child: Stack(children: [
        Positioned(
          left: l.start * pps,
          width: width,
          top: 4,
          bottom: 4,
          child: _clip(l, sel),
        ),
        if (sel)
          for (final t in _keyTimes(l))
            Positioned(
              left: t * pps - 10,
              top: rowH / 2 - 10,
              child: GestureDetector(
                onTap: () => c.seek(t),
                child: const SizedBox(width: 20, height: 20, child: Center(child: Diamond(size: 9))),
              ),
            ),
      ]),
    );
  }

  Widget _clip(Layer l, bool sel) => Stack(children: [
        Positioned.fill(
          child: GestureDetector(
            onTap: () => c.select(l.id),
            onHorizontalDragStart: (_) {
              c.select(l.id);
              c.checkpoint();
              _acc = 0;
              _orig = l.start;
            },
            onHorizontalDragUpdate: (d) {
              _acc += d.delta.dx;
              c.moveClip(l, _orig + _acc / pps);
            },
            child: Container(
              padding: const EdgeInsets.symmetric(horizontal: 18),
              alignment: Alignment.centerLeft,
              decoration: BoxDecoration(
                color: sel ? C.tealDeep : const Color(0xFF2A2A2D),
                border: Border.all(color: sel ? C.teal : C.line, width: sel ? 1.5 : 1),
                borderRadius: BorderRadius.circular(8),
              ),
              child: Text(
                l.name,
                maxLines: 1,
                overflow: TextOverflow.clip,
                style: TextStyle(fontSize: 12, color: sel ? C.text : C.muted),
              ),
            ),
          ),
        ),
        Positioned(left: 0, top: 0, bottom: 0, width: 16, child: _handle(l, true)),
        Positioned(right: 0, top: 0, bottom: 0, width: 16, child: _handle(l, false)),
      ]);

  Widget _handle(Layer l, bool isStart) => GestureDetector(
        behavior: HitTestBehavior.opaque,
        onHorizontalDragStart: (_) {
          c.select(l.id);
          c.checkpoint();
          _acc = 0;
          _orig = isStart ? l.start : l.end;
        },
        onHorizontalDragUpdate: (d) {
          _acc += d.delta.dx;
          final t = _orig + _acc / pps;
          isStart ? c.trimStart(l, t) : c.trimEnd(l, t);
        },
        child: Center(
          child: Container(
            width: 4,
            height: 18,
            decoration: BoxDecoration(color: Colors.white54, borderRadius: BorderRadius.circular(2)),
          ),
        ),
      );
}

class _RulerPainter extends CustomPainter {
  final double pps, duration;
  _RulerPainter(this.pps, this.duration);

  @override
  void paint(Canvas canvas, Size size) {
    final line = Paint()
      ..color = C.muted
      ..strokeWidth = 1;
    final labelEvery = pps >= 60 ? 1 : 5;
    for (var s = 0; s <= duration.ceil(); s++) {
      final x = s * pps;
      canvas.drawLine(Offset(x, size.height - 10), Offset(x, size.height), line);
      if (pps >= 120 && s < duration) {
        canvas.drawLine(Offset(x + pps / 2, size.height - 5), Offset(x + pps / 2, size.height), line);
      }
      if (s % labelEvery == 0) {
        final tp = TextPainter(
          text: TextSpan(text: '${s}s', style: const TextStyle(color: C.muted, fontSize: 10)),
          textDirection: TextDirection.ltr,
        )..layout();
        tp.paint(canvas, Offset(x + 3, 4));
      }
    }
  }

  @override
  bool shouldRepaint(covariant _RulerPainter old) => old.pps != pps || old.duration != duration;
}
