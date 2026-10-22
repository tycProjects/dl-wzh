import 'dart:ui' show FontFeature;
import 'package:flutter/material.dart';
import '../editor_controller.dart';
import '../model.dart';
import '../theme.dart';
import 'common.dart';

/// Alight Motion style: the playhead stays fixed in the middle, the timeline scrolls under it.
/// Scroll offset == time * pps, kept in sync both ways.
class TimelineView extends StatefulWidget {
  final EditorController c;
  final VoidCallback onAdd;
  const TimelineView({super.key, required this.c, required this.onAdd});
  @override
  State<TimelineView> createState() => _TimelineViewState();
}

class _TimelineViewState extends State<TimelineView> {
  double pps = 90;
  static const rowH = 44.0, rulerH = 26.0;
  static const clipBlue = Color(0xFF3B9EC8), clipDim = Color(0xFF2B6F8F);
  final sc = ScrollController();
  bool _prog = false, _user = false;
  double _orig = 0, _acc = 0;

  EditorController get c => widget.c;

  @override
  void initState() {
    super.initState();
    c.timeN.addListener(_follow);
  }

  @override
  void dispose() {
    c.timeN.removeListener(_follow);
    sc.dispose();
    super.dispose();
  }

  void _follow() {
    if (!_user && sc.hasClients) _jump();
  }

  void _jump() {
    final target = (c.time * pps).clamp(0.0, sc.position.maxScrollExtent).toDouble();
    if ((sc.offset - target).abs() > 0.5) {
      _prog = true;
      sc.jumpTo(target);
      _prog = false;
    }
  }

  bool _onScroll(ScrollNotification n) {
    if (n.metrics.axis != Axis.horizontal) return false;
    if (n is ScrollStartNotification && n.dragDetails != null) {
      _user = true;
      if (c.playing) c.pause();
    } else if (n is ScrollUpdateNotification && _user && !_prog) {
      c.seek(n.metrics.pixels / pps);
    } else if (n is ScrollEndNotification && _user) {
      _user = false;
      _jump();
    }
    return false;
  }

  void _zoom(double f) {
    setState(() => pps = (pps * f).clamp(30.0, 400.0));
    WidgetsBinding.instance.addPostFrameCallback((_) {
      if (sc.hasClients) _jump();
    });
  }

  @override
  Widget build(BuildContext context) => ListenableBuilder(
        listenable: c,
        builder: (context, _) => LayoutBuilder(builder: (context, bc) {
          final vw = bc.maxWidth, pad = vw / 2;
          final p = c.project;
          final layers = p.layers.reversed.toList();
          return GestureDetector(
            onScaleUpdate: (d) {
              if (d.pointerCount >= 2 && d.scale != 1) _zoom(d.scale > 1 ? 1.03 : 0.97);
            },
            child: Stack(children: [
              NotificationListener<ScrollNotification>(
                onNotification: _onScroll,
                child: SingleChildScrollView(
                  controller: sc,
                  scrollDirection: Axis.horizontal,
                  child: SizedBox(
                    width: p.duration * pps + vw,
                    height: bc.maxHeight,
                    child: Column(children: [
                      SizedBox(
                        height: rulerH,
                        width: double.infinity,
                        child: CustomPaint(painter: _RulerPainter(pps, p.duration, pad)),
                      ),
                      Expanded(child: ListView(padding: EdgeInsets.zero, children: [for (final l in layers) _row(l, pad)])),
                    ]),
                  ),
                ),
              ),
              Positioned(
                left: pad - 1,
                top: 0,
                bottom: 0,
                child: IgnorePointer(child: Container(width: 2, color: Colors.white)),
              ),
              Positioned(
                left: 0,
                right: 0,
                top: 3,
                child: IgnorePointer(
                  child: Center(
                    child: ValueListenableBuilder<double>(
                      valueListenable: c.timeN,
                      builder: (_, t, __) => Container(
                        padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 1),
                        decoration: BoxDecoration(color: C.bg, borderRadius: BorderRadius.circular(6)),
                        child: Text(fmtFrames(t, p.fps), style: const TextStyle(fontSize: 11, fontFeatures: [FontFeature.tabularFigures()])),
                      ),
                    ),
                  ),
                ),
              ),
              Positioned(
                left: 12,
                bottom: 10,
                child: SizedBox(
                  width: 48,
                  height: 48,
                  child: FloatingActionButton(
                    heroTag: 'add_layer',
                    onPressed: widget.onAdd,
                    backgroundColor: C.teal,
                    foregroundColor: C.onTeal,
                    shape: const CircleBorder(),
                    child: const Icon(Icons.add, size: 28),
                  ),
                ),
              ),
            ]),
          );
        }),
      );

  List<double> _keyTimes(Layer l) => {
        for (final p in l.props.values)
          for (final k in p.keys) k.t
      }.toList();

  Widget _row(Layer l, double pad) {
    final sel = c.selectedId == l.id;
    final width = ((l.end - l.start) * pps).clamp(24.0, double.infinity).toDouble();
    return SizedBox(
      height: rowH,
      child: Stack(children: [
        Positioned(
          left: pad + l.start * pps,
          width: width,
          top: 4,
          bottom: 4,
          child: _clip(l, sel),
        ),
        if (sel)
          for (final t in _keyTimes(l))
            Positioned(
              left: pad + t * pps - 10,
              top: rowH / 2 - 10,
              child: GestureDetector(
                onTap: () => c.seek(t),
                child: const SizedBox(width: 20, height: 20, child: Center(child: Diamond(size: 9))),
              ),
            ),
        // eye toggle stays pinned to the left edge of the viewport
        ValueListenableBuilder<double>(
          valueListenable: c.timeN,
          builder: (_, t, __) => Positioned(
            left: (c.timeN.value * pps).clamp(0.0, double.infinity).toDouble() + 6,
            top: 7,
            child: GestureDetector(
              onTap: () {
                c.select(l.id);
                c.edit((x) => x.visible = !x.visible);
              },
              child: Container(
                width: 30,
                height: 30,
                decoration: BoxDecoration(color: C.bg.withOpacity(.7), shape: BoxShape.circle),
                child: Icon(l.visible ? Icons.visibility : Icons.visibility_off, size: 17, color: l.visible ? C.text : C.muted),
              ),
            ),
          ),
        ),
      ]),
    );
  }

  Color _clipColor(Layer l, bool sel) {
    if (l.type == LayerType.video) return sel ? const Color(0xFF8B6BE0) : const Color(0xFF5E48A0);
    return sel ? clipBlue : clipDim;
  }

  Widget _clip(Layer l, bool sel) => Stack(children: [
        Positioned.fill(
          child: GestureDetector(
            onTap: () => c.select(l.id),
            // long-press then drag moves a clip; plain drags scrub the timeline
            onLongPressStart: (_) {
              c.select(l.id);
              c.checkpoint();
              _orig = l.start;
            },
            onLongPressMoveUpdate: (d) => c.moveClip(l, _orig + d.offsetFromOrigin.dx / pps),
            child: Container(
              padding: const EdgeInsets.only(left: 42, right: 18),
              alignment: Alignment.centerLeft,
              decoration: BoxDecoration(
                color: _clipColor(l, sel),
                border: sel ? Border.all(color: Colors.white, width: 1.5) : null,
                borderRadius: BorderRadius.circular(6),
              ),
              child: Text(l.name, maxLines: 1, overflow: TextOverflow.clip, style: const TextStyle(fontSize: 12, color: Colors.white)),
            ),
          ),
        ),
        if (sel) Positioned(left: 0, top: 0, bottom: 0, width: 16, child: _handle(l, true)),
        if (sel) Positioned(right: 0, top: 0, bottom: 0, width: 16, child: _handle(l, false)),
      ]);

  Widget _handle(Layer l, bool isStart) => GestureDetector(
        behavior: HitTestBehavior.opaque,
        onHorizontalDragStart: (_) {
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
          child: Container(width: 4, height: 18, decoration: BoxDecoration(color: Colors.white, borderRadius: BorderRadius.circular(2))),
        ),
      );
}

class _RulerPainter extends CustomPainter {
  final double pps, duration, pad;
  _RulerPainter(this.pps, this.duration, this.pad);

  @override
  void paint(Canvas canvas, Size size) {
    final line = Paint()
      ..color = C.muted
      ..strokeWidth = 1;
    final every = pps >= 60 ? 1 : 5;
    for (var s = 0; s <= duration.ceil(); s++) {
      final x = pad + s * pps;
      canvas.drawLine(Offset(x, size.height - 8), Offset(x, size.height), line);
      if (pps >= 120 && s < duration) {
        canvas.drawLine(Offset(x + pps / 2, size.height - 4), Offset(x + pps / 2, size.height), line);
      }
      if (s % every == 0 && s > 0) {
        final tp = TextPainter(
          text: TextSpan(text: '${s}s', style: const TextStyle(color: C.muted, fontSize: 9)),
          textDirection: TextDirection.ltr,
        )..layout();
        tp.paint(canvas, Offset(x + 3, 12));
      }
    }
  }

  @override
  bool shouldRepaint(covariant _RulerPainter old) => old.pps != pps || old.duration != duration || old.pad != pad;
}
