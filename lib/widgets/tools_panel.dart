import 'dart:math' as math;
import 'package:flutter/material.dart';
import '../editor_controller.dart';
import '../effects.dart';
import '../model.dart';
import '../theme.dart';
import 'common.dart';

const _tile = Color(0xFF1E212B);

/// Bottom panel for the selected layer. page == null shows the tool grid,
/// otherwise 'color' | 'blend' | 'shape' | 'move'.
class ToolsPanel extends StatelessWidget {
  final EditorController c;
  final ValueNotifier<String?> page;
  const ToolsPanel({super.key, required this.c, required this.page});

  void _soon(BuildContext ctx) => ScaffoldMessenger.of(ctx)
    ..hideCurrentSnackBar()
    ..showSnackBar(const SnackBar(content: Text('Coming in a later version'), duration: Duration(seconds: 1)));

  @override
  Widget build(BuildContext context) => Container(
        color: C.card,
        child: ValueListenableBuilder<String?>(
          valueListenable: page,
          builder: (context, pg, _) => ListenableBuilder(
            listenable: Listenable.merge([c, c.timeN]),
            builder: (context, _) {
              final l = c.selected;
              if (l == null) return const SizedBox();
              switch (pg) {
                case 'move':
                  return MoveTransformPanel(c: c, l: l, onBack: () => page.value = null);
                case 'color':
                  return _sub('Color & Fill', [
                    if (l.type == LayerType.image || l.type == LayerType.video)
                      const Text('Images and videos have no fill color.', style: TextStyle(color: C.muted))
                    else
                      Swatches(current: l.color, onPick: (v) => c.edit((x) => x.color = v)),
                  ]);
                case 'blend':
                  return _sub('Blending & Opacity', [
                    _SliderRow(c: c, l: l, k: 'opacity', label: 'Opacity', min: 0, max: 1),
                    const SizedBox(height: 6),
                    SizedBox(
                      height: 40,
                      child: ListView(scrollDirection: Axis.horizontal, children: [
                        for (final n in blendModes.keys)
                          Padding(
                            padding: const EdgeInsets.only(right: 8),
                            child: ChoiceChip(
                              label: Text(n),
                              selected: l.blend == n,
                              onSelected: (_) => c.edit((x) => x.blend = n),
                            ),
                          ),
                      ]),
                    ),
                  ]);
                case 'shape':
                  return _sub('Edit Shape', [
                    if (l.type == LayerType.rect)
                      _plain('Corner', l.radius, 0, 300, (v) => c.edit((x) => x.radius = v, cp: false)),
                    if (l.type == LayerType.text) ...[
                      TextFormField(
                        key: ValueKey('text_${l.id}'),
                        initialValue: l.text,
                        onTap: c.checkpoint,
                        onChanged: (s) => c.edit((x) {
                          x.text = s;
                          x.remeasure();
                        }, cp: false),
                        decoration: const InputDecoration(labelText: 'Text', border: OutlineInputBorder(), isDense: true),
                      ),
                      _plain('Size', l.fontSize, 20, 400, (v) => c.edit((x) {
                            x.fontSize = v;
                            x.remeasure();
                          }, cp: false)),
                    ],
                    if (l.type == LayerType.ellipse || l.type == LayerType.image || l.type == LayerType.video)
                      const Text('Nothing to edit for this layer type yet.', style: TextStyle(color: C.muted)),
                  ]);
                case 'adjust':
                  return _sub('Adjust & Filters', [
                    SizedBox(
                      height: 40,
                      child: ListView(scrollDirection: Axis.horizontal, children: [
                        for (final n in filterPresets.keys)
                          Padding(
                            padding: const EdgeInsets.only(right: 8),
                            child: ChoiceChip(label: Text(n), selected: l.filter == n, onSelected: (_) => c.edit((x) => x.filter = n)),
                          ),
                      ]),
                    ),
                    const SizedBox(height: 4),
                    if (l.filter != 'None') _SliderRow(c: c, l: l, k: 'fmix', label: 'Filter', min: 0, max: 1),
                    _SliderRow(c: c, l: l, k: 'bright', label: 'Bright', min: -1, max: 1),
                    _SliderRow(c: c, l: l, k: 'contrast', label: 'Contrast', min: 0, max: 2),
                    _SliderRow(c: c, l: l, k: 'sat', label: 'Color', min: 0, max: 2),
                  ]);
                case 'effects':
                  return _sub('Effects', [
                    _SliderRow(c: c, l: l, k: 'blur', label: 'Blur', min: 0, max: 60, fmt: (v) => v.toStringAsFixed(0)),
                    _SliderRow(c: c, l: l, k: 'glow', label: 'Glow', min: 0, max: 1),
                    _SliderRow(c: c, l: l, k: 'shadow', label: 'Shadow', min: 0, max: 1),
                    _SliderRow(c: c, l: l, k: 'split', label: 'Glitch', min: 0, max: 40, fmt: (v) => '${v.toStringAsFixed(0)}px'),
                  ]);
                case 'anim':
                  return _sub('Animations', [
                    _label('In'),
                    _chips(inOutNames, l.animIn, (i) {
                      c.edit((x) => x.animIn = i);
                      if (i != 0) c.previewAt(l.start);
                    }),
                    _label('Out'),
                    _chips(inOutNames, l.animOut, (i) {
                      c.edit((x) => x.animOut = i);
                      if (i != 0) c.previewAt(math.max(l.start, l.end - l.animDur - 0.3));
                    }),
                    _label('Loop'),
                    _chips(loopNames, l.animLoop, (i) {
                      c.edit((x) => x.animLoop = i);
                      if (i != 0) c.previewAt(l.start);
                    }),
                    const SizedBox(height: 4),
                    _plain('Length', l.animDur, 0.2, 1.5, (v) => c.edit((x) => x.animDur = v, cp: false), fmt: (v) => '${v.toStringAsFixed(1)}s'),
                  ]);
                default:
                  return _grid(context, l);
              }
            },
          ),
        ),
      );

  Widget _grid(BuildContext context, Layer l) => Padding(
        padding: const EdgeInsets.fromLTRB(8, 4, 8, 6),
        child: Column(children: [
          SizedBox(
            height: 34,
            child: Row(mainAxisAlignment: MainAxisAlignment.spaceAround, children: [
              _mini(Icons.content_cut, 'Split', c.split),
              _mini(Icons.copy, 'Duplicate', c.duplicateSelected),
              _mini(Icons.flip_to_front, 'Forward', () => c.moveZ(1)),
              _mini(Icons.flip_to_back, 'Back', () => c.moveZ(-1)),
              _mini(Icons.delete_outline, 'Delete', c.deleteSelected),
            ]),
          ),
          Expanded(
            child: Row(children: [
              _big(Icons.format_color_fill, 'Color & Fill', () => page.value = 'color'),
              _big(Icons.tune, 'Adjust & Filters', () => page.value = 'adjust'),
              _big(Icons.layers_outlined, 'Blending & Opacity', () => page.value = 'blend'),
            ]),
          ),
          Expanded(
            child: Row(children: [
              _big(Icons.open_with, 'Move & Transform', () => page.value = 'move'),
              _big(Icons.edit_outlined, 'Edit Shape', () => page.value = 'shape'),
              _big(Icons.animation, 'Animations', () => page.value = 'anim'),
              _big(Icons.auto_awesome, 'Effects', () => page.value = 'effects'),
            ]),
          ),
        ]),
      );

  Widget _mini(IconData i, String tip, VoidCallback f) => IconButton(
        tooltip: tip,
        visualDensity: VisualDensity.compact,
        icon: Icon(i, size: 20, color: C.muted),
        onPressed: f,
      );

  Widget _big(IconData i, String label, VoidCallback f, {bool off = false}) => Expanded(
        child: GestureDetector(
          onTap: f,
          child: Opacity(
            opacity: off ? .4 : 1,
            child: Container(
              margin: const EdgeInsets.all(3),
              decoration: BoxDecoration(color: _tile, borderRadius: BorderRadius.circular(10)),
              child: Column(mainAxisAlignment: MainAxisAlignment.center, children: [
                Icon(i, size: 22),
                const SizedBox(height: 3),
                Text(label, textAlign: TextAlign.center, style: const TextStyle(fontSize: 10, color: C.muted)),
              ]),
            ),
          ),
        ),
      );

  Widget _sub(String title, List<Widget> children) => Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
        Row(children: [
          IconButton(icon: const Icon(Icons.arrow_back), onPressed: () => page.value = null),
          Text(title, style: const TextStyle(fontWeight: FontWeight.w600)),
        ]),
        Expanded(child: ListView(padding: const EdgeInsets.fromLTRB(16, 0, 16, 12), children: children)),
      ]);

  Widget _label(String s) => Padding(
        padding: const EdgeInsets.only(top: 6, bottom: 2),
        child: Text(s, style: const TextStyle(color: C.muted, fontSize: 12)),
      );

  Widget _chips(List<String> names, int sel, void Function(int) f) => SizedBox(
        height: 40,
        child: ListView(scrollDirection: Axis.horizontal, children: [
          for (var i = 0; i < names.length; i++)
            Padding(
              padding: const EdgeInsets.only(right: 8),
              child: ChoiceChip(label: Text(names[i]), selected: sel == i, onSelected: (_) => f(i)),
            ),
        ]),
      );

  Widget _plain(String label, double v, double min, double max, void Function(double) f, {String Function(double)? fmt}) => Row(children: [
        SizedBox(width: 58, child: Text(label, style: const TextStyle(fontSize: 13))),
        Expanded(
          child: Slider(value: v.clamp(min, max).toDouble(), min: min, max: max, onChangeStart: (_) => c.checkpoint(), onChanged: f),
        ),
        SizedBox(width: 44, child: Text(fmt != null ? fmt(v) : v.toStringAsFixed(0), textAlign: TextAlign.right, style: const TextStyle(fontSize: 12, color: C.muted))),
      ]);
}

class _SliderRow extends StatelessWidget {
  final EditorController c;
  final Layer l;
  final String k, label;
  final double min, max;
  final String Function(double)? fmt;
  const _SliderRow({required this.c, required this.l, required this.k, required this.label, required this.min, required this.max, this.fmt});

  @override
  Widget build(BuildContext context) {
    final prop = l.props[k]!;
    final v = prop.at(c.time);
    final has = prop.keyAt(c.time) != null;
    return Row(children: [
      SizedBox(width: 58, child: Text(label, style: const TextStyle(fontSize: 13))),
      Expanded(
        child: Slider(
          value: v.clamp(min, max).toDouble(),
          min: min,
          max: max,
          onChangeStart: (_) => c.checkpoint(),
          onChanged: (x) => c.setProp(k, x),
        ),
      ),
      SizedBox(width: 44, child: Text(fmt != null ? fmt!(v) : '${(v * 100).round()}%', textAlign: TextAlign.right, style: const TextStyle(fontSize: 12, color: C.muted))),
      InkResponse(
        onTap: () => c.toggleKey(k),
        child: Padding(
          padding: const EdgeInsets.all(10),
          child: Diamond(size: 11, filled: has, color: has ? Colors.amber : (prop.animated ? C.teal : C.muted)),
        ),
      ),
    ]);
  }
}

// ---------------------------------------------------------------------------
// Move & Transform: position pad, rotation dial, width/height ruler
// ---------------------------------------------------------------------------

class MoveTransformPanel extends StatefulWidget {
  final EditorController c;
  final Layer l;
  final VoidCallback onBack;
  const MoveTransformPanel({super.key, required this.c, required this.l, required this.onBack});
  @override
  State<MoveTransformPanel> createState() => _MoveTransformPanelState();
}

class _MoveTransformPanelState extends State<MoveTransformPanel> {
  int mode = 0; // 0 position, 1 rotation, 2 size
  bool link = true;
  bool editW = true;
  double _rulerOff = 0, _lastAngle = 0;

  EditorController get c => widget.c;
  Layer get l => widget.l;

  List<String> get _keys => mode == 0 ? ['x', 'y'] : (mode == 1 ? ['rotation'] : ['sx', 'sy']);

  void _reset() {
    c.checkpoint();
    if (mode == 0) {
      c.setProp('x', c.project.width / 2);
      c.setProp('y', c.project.height / 2);
    } else if (mode == 1) {
      c.setProp('rotation', 0);
    } else {
      c.setProp('sx', 1);
      c.setProp('sy', 1);
    }
  }

  Future<void> _ask(String label, double cur, void Function(double) apply) async {
    final ctl = TextEditingController(text: cur.toStringAsFixed(2));
    final v = await showDialog<double>(
      context: context,
      builder: (ctx) => AlertDialog(
        backgroundColor: C.sheet,
        title: Text(label),
        content: TextField(
          controller: ctl,
          autofocus: true,
          keyboardType: const TextInputType.numberWithOptions(decimal: true, signed: true),
        ),
        actions: [
          TextButton(onPressed: () => Navigator.pop(ctx), child: const Text('Cancel')),
          TextButton(onPressed: () => Navigator.pop(ctx, double.tryParse(ctl.text)), child: const Text('Set')),
        ],
      ),
    );
    if (v != null) {
      c.checkpoint();
      apply(v);
    }
  }

  Widget _field(String label, double v, VoidCallback onTap, {bool active = false, String suffix = ''}) => Expanded(
        child: GestureDetector(
          onTap: onTap,
          child: Container(
            height: 44,
            margin: const EdgeInsets.symmetric(horizontal: 4),
            decoration: BoxDecoration(
              color: const Color(0xFF22232B),
              borderRadius: BorderRadius.circular(8),
              border: Border.all(color: active ? C.teal : Colors.transparent),
            ),
            child: Column(mainAxisAlignment: MainAxisAlignment.center, children: [
              Text(label, style: const TextStyle(fontSize: 9, color: C.muted)),
              Text('${v.toStringAsFixed(2)}$suffix', style: const TextStyle(fontSize: 14, fontWeight: FontWeight.w600)),
            ]),
          ),
        ),
      );

  @override
  Widget build(BuildContext context) {
    final has = c.hasKeyAt(_keys), anim = c.isAnimated(_keys);
    return Row(children: [
      // left column: back / keyframe / reset
      SizedBox(
        width: 48,
        child: Column(mainAxisAlignment: MainAxisAlignment.spaceEvenly, children: [
          IconButton(icon: const Icon(Icons.arrow_back), onPressed: widget.onBack),
          InkResponse(
            onTap: () => c.toggleKeys(_keys),
            child: Padding(
              padding: const EdgeInsets.all(12),
              child: Diamond(size: 14, filled: has, color: has ? Colors.amber : (anim ? C.teal : C.muted)),
            ),
          ),
          IconButton(icon: const Icon(Icons.restart_alt, color: C.muted), tooltip: 'Reset', onPressed: _reset),
        ]),
      ),
      Expanded(child: Padding(padding: const EdgeInsets.symmetric(vertical: 8), child: _body())),
      // right column: mode switch
      SizedBox(
        width: 52,
        child: Column(mainAxisAlignment: MainAxisAlignment.spaceEvenly, children: [
          _modeBtn(0, Icons.open_with),
          _modeBtn(1, Icons.rotate_right),
          _modeBtn(2, Icons.aspect_ratio),
        ]),
      ),
    ]);
  }

  Widget _modeBtn(int m, IconData i) => GestureDetector(
        onTap: () => setState(() => mode = m),
        child: Container(
          width: 40,
          height: 40,
          decoration: BoxDecoration(color: mode == m ? C.tealDeep : Colors.transparent, borderRadius: BorderRadius.circular(8)),
          child: Icon(i, color: mode == m ? C.teal : C.muted),
        ),
      );

  Widget _body() {
    final t = c.time, p = c.project;
    if (mode == 0) {
      final x = l.x.at(t), y = l.y.at(t);
      return Column(children: [
        Row(children: [
          _field('X', x, () => _ask('X position', x, (v) => c.setProp('x', v))),
          _field('Y', y, () => _ask('Y position', y, (v) => c.setProp('y', v))),
        ]),
        const SizedBox(height: 8),
        Expanded(
          child: GestureDetector(
            onPanStart: (_) => c.checkpoint(),
            onPanUpdate: (d) {
              c.setProp('x', l.x.at(c.time) + d.delta.dx);
              c.setProp('y', l.y.at(c.time) + d.delta.dy);
            },
            child: Container(
              margin: const EdgeInsets.symmetric(horizontal: 4),
              decoration: BoxDecoration(color: const Color(0xFF181A21), borderRadius: BorderRadius.circular(8), border: Border.all(color: C.line)),
              child: const Center(child: Text('Swipe here to move layer', style: TextStyle(color: C.muted, fontSize: 12))),
            ),
          ),
        ),
      ]);
    }
    if (mode == 1) {
      final r = l.rotation.at(t);
      return LayoutBuilder(builder: (context, bc) {
        final d = math.min(bc.maxWidth, bc.maxHeight);
        return Center(
          child: SizedBox(
            width: d,
            height: d,
            child: GestureDetector(
              onPanStart: (s) {
                c.checkpoint();
                _lastAngle = math.atan2(s.localPosition.dy - d / 2, s.localPosition.dx - d / 2);
              },
              onPanUpdate: (u) {
                final a = math.atan2(u.localPosition.dy - d / 2, u.localPosition.dx - d / 2);
                var delta = a - _lastAngle;
                while (delta > math.pi) {
                  delta -= 2 * math.pi;
                }
                while (delta < -math.pi) {
                  delta += 2 * math.pi;
                }
                _lastAngle = a;
                c.setProp('rotation', l.rotation.at(c.time) + delta * 180 / math.pi);
              },
              child: CustomPaint(
                painter: _DialPainter(r),
                child: Center(
                  child: GestureDetector(
                    onTap: () => _ask('Rotation (degrees)', r, (v) => c.setProp('rotation', v)),
                    child: Container(
                      padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 8),
                      decoration: BoxDecoration(color: const Color(0xFF22232B), borderRadius: BorderRadius.circular(8)),
                      child: Text('${r.round()}°', style: const TextStyle(fontSize: 22, fontWeight: FontWeight.w600, color: C.teal)),
                    ),
                  ),
                ),
              ),
            ),
          ),
        );
      });
    }
    // size
    final w = l.w * l.sx.at(t).abs(), h = l.h * l.sy.at(t).abs();
    return Column(children: [
      Row(children: [
        _field('Width', w, () => setState(() => editW = true), active: editW),
        GestureDetector(
          onTap: () => setState(() => link = !link),
          child: Container(
            width: 40,
            height: 44,
            decoration: BoxDecoration(color: link ? C.tealDeep : const Color(0xFF22232B), borderRadius: BorderRadius.circular(8)),
            child: Icon(link ? Icons.link : Icons.link_off, size: 18, color: link ? C.teal : C.muted),
          ),
        ),
        _field('Height', h, () => setState(() => editW = false), active: !editW),
      ]),
      const SizedBox(height: 8),
      Expanded(
        child: GestureDetector(
          onPanStart: (_) => c.checkpoint(),
          onPanUpdate: (d) {
            final tt = c.time;
            final cw = l.w * l.sx.at(tt).abs(), ch = l.h * l.sy.at(tt).abs();
            final cur = editW ? cw : ch;
            final next = math.max(2.0, cur + d.delta.dx);
            final f = next / cur;
            setState(() => _rulerOff += d.delta.dx);
            if (link) {
              c.setProp('sx', l.sx.at(tt) * f);
              c.setProp('sy', l.sy.at(tt) * f);
            } else if (editW) {
              c.setProp('sx', l.sx.at(tt) * f);
            } else {
              c.setProp('sy', l.sy.at(tt) * f);
            }
          },
          child: Container(
            margin: const EdgeInsets.symmetric(horizontal: 4),
            decoration: BoxDecoration(color: const Color(0xFF181A21), borderRadius: BorderRadius.circular(8)),
            child: CustomPaint(painter: _RulerPainter(_rulerOff), size: Size.infinite),
          ),
        ),
      ),
    ]);
  }
}

class _DialPainter extends CustomPainter {
  final double deg;
  _DialPainter(this.deg);
  @override
  void paint(Canvas canvas, Size size) {
    final c = size.center(Offset.zero);
    final r = size.width / 2 - 14;
    canvas.drawCircle(
      c,
      r,
      Paint()
        ..style = PaintingStyle.stroke
        ..strokeWidth = 4
        ..color = C.teal.withOpacity(.55),
    );
    final a = deg * math.pi / 180;
    canvas.drawCircle(c + Offset(math.cos(a), math.sin(a)) * r, 13, Paint()..color = Colors.white);
  }

  @override
  bool shouldRepaint(covariant _DialPainter old) => old.deg != deg;
}

class _RulerPainter extends CustomPainter {
  final double off;
  _RulerPainter(this.off);
  @override
  void paint(Canvas canvas, Size size) {
    final p = Paint()
      ..color = C.muted.withOpacity(.6)
      ..strokeWidth = 1;
    const step = 9.0;
    final start = off % step;
    for (var x = start; x < size.width; x += step) {
      final tall = ((x - start) / step).round() % 5 == 0;
      canvas.drawLine(Offset(x, size.height * (tall ? .3 : .38)), Offset(x, size.height * (tall ? .7 : .62)), p);
    }
    canvas.drawLine(
      Offset(size.width / 2, size.height * .2),
      Offset(size.width / 2, size.height * .8),
      Paint()
        ..color = C.teal
        ..strokeWidth = 2,
    );
  }

  @override
  bool shouldRepaint(covariant _RulerPainter old) => old.off != off;
}
