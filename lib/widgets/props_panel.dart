import 'package:flutter/material.dart';
import '../editor_controller.dart';
import '../model.dart';
import '../theme.dart';
import 'common.dart';

class PropsPanel extends StatelessWidget {
  final EditorController c;
  const PropsPanel({super.key, required this.c});

  Widget _head(String s) => Padding(
        padding: const EdgeInsets.only(top: 16, bottom: 6),
        child: Text(s.toUpperCase(), style: const TextStyle(color: C.muted, fontSize: 11, letterSpacing: 1)),
      );

  @override
  Widget build(BuildContext context) => ListenableBuilder(
        listenable: Listenable.merge([c, c.timeN]),
        builder: (context, _) {
          final p = c.project;
          final l = c.selected;
          if (l == null) {
            return ListView(padding: const EdgeInsets.all(16), children: [
              const Text('Select a layer to edit it.', style: TextStyle(color: C.muted)),
              _head('Canvas background'),
              Swatches(current: p.bg, onPick: c.setBackground),
            ]);
          }
          return ListView(padding: const EdgeInsets.fromLTRB(16, 4, 16, 24), children: [
            Row(children: [
              Expanded(child: Text(l.name, style: const TextStyle(fontWeight: FontWeight.w700, fontSize: 16))),
              IconButton(icon: const Icon(Icons.flip_to_front, size: 20), tooltip: 'Bring forward', onPressed: () => c.moveZ(1)),
              IconButton(icon: const Icon(Icons.flip_to_back, size: 20), tooltip: 'Send back', onPressed: () => c.moveZ(-1)),
            ]),
            _head('Transform  (diamond = keyframe)'),
            _PropRow(c: c, l: l, k: 'x', label: 'X', min: -p.width.toDouble(), max: p.width * 2.0),
            _PropRow(c: c, l: l, k: 'y', label: 'Y', min: -p.height.toDouble(), max: p.height * 2.0),
            _PropRow(c: c, l: l, k: 'scale', label: 'Scale', min: 0.05, max: 5),
            _PropRow(c: c, l: l, k: 'rotation', label: 'Rotate', min: -360, max: 360),
            _PropRow(c: c, l: l, k: 'opacity', label: 'Opacity', min: 0, max: 1),
            _head('Appearance'),
            if (l.type != LayerType.image) Swatches(current: l.color, onPick: (v) => c.edit((l) => l.color = v)),
            if (l.type == LayerType.rect) ...[
              const SizedBox(height: 8),
              _plainSlider('Corner', l.radius, 0, 300, (v) => c.edit((l) => l.radius = v, cp: false)),
            ],
            if (l.type == LayerType.text) ...[
              const SizedBox(height: 12),
              TextFormField(
                key: ValueKey('text_${l.id}'),
                initialValue: l.text,
                onTap: c.checkpoint,
                onChanged: (s) => c.edit((l) {
                  l.text = s;
                  l.remeasure();
                }, cp: false),
                decoration: const InputDecoration(labelText: 'Text', border: OutlineInputBorder(), isDense: true),
              ),
              _plainSlider('Size', l.fontSize, 20, 400, (v) => c.edit((l) {
                    l.fontSize = v;
                    l.remeasure();
                  }, cp: false)),
            ],
            _head('Blend mode'),
            DropdownButton<String>(
              value: l.blend,
              isExpanded: true,
              dropdownColor: C.sheet,
              underline: Container(height: 1, color: C.line),
              items: [for (final n in blendModes.keys) DropdownMenuItem(value: n, child: Text(n))],
              onChanged: (v) => c.edit((l) => l.blend = v!),
            ),
            const SizedBox(height: 8),
            SwitchListTile(
              contentPadding: EdgeInsets.zero,
              title: const Text('Visible'),
              value: l.visible,
              activeColor: C.teal,
              onChanged: (v) => c.edit((l) => l.visible = v),
            ),
          ]);
        },
      );

  Widget _plainSlider(String label, double v, double min, double max, void Function(double) onChanged) => Row(children: [
        SizedBox(width: 58, child: Text(label, style: const TextStyle(fontSize: 13))),
        Expanded(
          child: Slider(
            value: v.clamp(min, max).toDouble(),
            min: min,
            max: max,
            onChangeStart: (_) => c.checkpoint(),
            onChanged: onChanged,
          ),
        ),
        SizedBox(width: 52, child: Text(v.toStringAsFixed(0), textAlign: TextAlign.right, style: const TextStyle(fontSize: 12, color: C.muted))),
      ]);
}

class _PropRow extends StatelessWidget {
  final EditorController c;
  final Layer l;
  final String k, label;
  final double min, max;
  const _PropRow({required this.c, required this.l, required this.k, required this.label, required this.min, required this.max});

  String _fmt(double v) {
    switch (k) {
      case 'scale':
        return v.toStringAsFixed(2);
      case 'rotation':
        return '${v.toStringAsFixed(0)}°';
      case 'opacity':
        return '${(v * 100).round()}%';
      default:
        return v.toStringAsFixed(0);
    }
  }

  @override
  Widget build(BuildContext context) {
    final prop = l.props[k]!;
    final v = prop.at(c.time);
    final key = prop.keyAt(c.time);
    return Column(children: [
      Row(children: [
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
        SizedBox(width: 52, child: Text(_fmt(v), textAlign: TextAlign.right, style: const TextStyle(fontSize: 12, color: C.muted))),
        InkResponse(
          onTap: () => c.toggleKey(k),
          child: Padding(
            padding: const EdgeInsets.all(10),
            child: Diamond(
              size: 11,
              filled: key != null,
              color: key != null ? Colors.amber : (prop.animated ? C.teal : C.muted),
            ),
          ),
        ),
      ]),
      if (key != null)
        Align(
          alignment: Alignment.centerRight,
          child: DropdownButton<KeyEasing>(
            value: key.easing,
            isDense: true,
            dropdownColor: C.sheet,
            underline: const SizedBox(),
            style: const TextStyle(fontSize: 12, color: Colors.amber),
            items: [for (final e in KeyEasing.values) DropdownMenuItem(value: e, child: Text(easingNames[e]!))],
            onChanged: (e) => c.setEasing(k, e!),
          ),
        ),
    ]);
  }
}
