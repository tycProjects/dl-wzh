import 'dart:math' as math;
import 'package:flutter/material.dart';
import '../editor_controller.dart';
import '../model.dart';
import '../renderer.dart';
import '../theme.dart';

class CanvasView extends StatelessWidget {
  final EditorController c;
  const CanvasView({super.key, required this.c});

  @override
  Widget build(BuildContext context) => ListenableBuilder(
        listenable: c,
        builder: (context, _) => LayoutBuilder(builder: (context, bc) {
          final p = c.project;
          final ar = p.width / p.height;
          var w = bc.maxWidth - 24;
          var h = w / ar;
          if (h > bc.maxHeight - 24) {
            h = bc.maxHeight - 24;
            w = h * ar;
          }
          return Center(child: _Interactive(c: c, size: Size(w, h)));
        }),
      );
}

class _Interactive extends StatefulWidget {
  final EditorController c;
  final Size size;
  const _Interactive({required this.c, required this.size});
  @override
  State<_Interactive> createState() => _InteractiveState();
}

class _InteractiveState extends State<_Interactive> {
  Layer? _l;
  Offset _f0 = Offset.zero;
  double _x0 = 0, _y0 = 0, _s0 = 1, _r0 = 0;
  bool _cp = false;

  EditorController get c => widget.c;

  Offset _toProject(Offset local) => Offset(
        local.dx * c.project.width / widget.size.width,
        local.dy * c.project.height / widget.size.height,
      );

  bool _live(Layer l, double t) => l.visible && t >= l.start && t < l.end;

  void _start(ScaleStartDetails d) {
    final p = c.project, t = c.time;
    final pt = _toProject(d.localFocalPoint);
    Layer? hit;
    final sel = c.selected;
    if (sel != null && _live(sel, t) && sel.hit(pt, t)) {
      hit = sel;
    } else {
      for (final l in p.layers.reversed) {
        if (_live(l, t) && l.hit(pt, t)) {
          hit = l;
          break;
        }
      }
    }
    c.select(hit?.id);
    _l = hit;
    _cp = false;
    if (hit == null) return;
    _f0 = d.localFocalPoint;
    _x0 = hit.x.at(t);
    _y0 = hit.y.at(t);
    _s0 = hit.scale.at(t);
    _r0 = hit.rotation.at(t);
  }

  void _update(ScaleUpdateDetails d) {
    if (_l == null) return;
    if (!_cp) {
      c.checkpoint();
      _cp = true;
    }
    final p = c.project;
    final delta = d.localFocalPoint - _f0;
    c.setProp('x', _x0 + delta.dx * p.width / widget.size.width);
    c.setProp('y', _y0 + delta.dy * p.height / widget.size.height);
    if (d.pointerCount >= 2) {
      c.setProp('scale', (_s0 * d.scale).clamp(0.05, 20.0).toDouble());
      c.setProp('rotation', _r0 + d.rotation * 180 / math.pi);
    }
  }

  @override
  Widget build(BuildContext context) => Container(
        width: widget.size.width,
        height: widget.size.height,
        decoration: BoxDecoration(border: Border.all(color: C.line)),
        child: GestureDetector(
          behavior: HitTestBehavior.opaque,
          onScaleStart: _start,
          onScaleUpdate: _update,
          child: RepaintBoundary(
            child: CustomPaint(size: widget.size, painter: _EditorPainter(c)),
          ),
        ),
      );
}

class _EditorPainter extends CustomPainter {
  final EditorController c;
  _EditorPainter(this.c) : super(repaint: Listenable.merge([c, c.timeN]));

  @override
  void paint(Canvas canvas, Size size) {
    final p = c.project, t = c.time;
    SceneRenderer.paint(canvas, size, p, t);
    final s = c.selected;
    if (s != null && s.visible && t >= s.start && t < s.end) {
      SceneRenderer.overlay(canvas, size, p, s, t);
    }
  }

  @override
  bool shouldRepaint(covariant _EditorPainter old) => true;
}
