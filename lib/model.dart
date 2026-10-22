import 'dart:math' as math;
import 'package:flutter/material.dart';

enum LayerType { rect, ellipse, text, image }

enum KeyEasing { linear, easeIn, easeOut, easeInOut, hold }

const easingNames = {
  KeyEasing.linear: 'Linear',
  KeyEasing.easeIn: 'Ease in',
  KeyEasing.easeOut: 'Ease out',
  KeyEasing.easeInOut: 'Ease in-out',
  KeyEasing.hold: 'Hold',
};

double applyEasing(KeyEasing e, double t) {
  switch (e) {
    case KeyEasing.linear:
      return t;
    case KeyEasing.easeIn:
      return Curves.easeInCubic.transform(t);
    case KeyEasing.easeOut:
      return Curves.easeOutCubic.transform(t);
    case KeyEasing.easeInOut:
      return Curves.easeInOutCubic.transform(t);
    case KeyEasing.hold:
      return 0;
  }
}

const Map<String, BlendMode> blendModes = {
  'Normal': BlendMode.srcOver,
  'Multiply': BlendMode.multiply,
  'Screen': BlendMode.screen,
  'Overlay': BlendMode.overlay,
  'Darken': BlendMode.darken,
  'Lighten': BlendMode.lighten,
  'Color dodge': BlendMode.colorDodge,
  'Color burn': BlendMode.colorBurn,
  'Difference': BlendMode.difference,
  'Exclusion': BlendMode.exclusion,
  'Add': BlendMode.plus,
};

class Keyframe {
  double t, v;
  KeyEasing easing;
  Keyframe(this.t, this.v, [this.easing = KeyEasing.easeInOut]);
  List<dynamic> toJson() => [t, v, easing.index];
  factory Keyframe.fromJson(List<dynamic> j) =>
      Keyframe((j[0] as num).toDouble(), (j[1] as num).toDouble(), KeyEasing.values[j[2] as int]);
}

/// An animatable number. No keyframes = constant [base].
class Prop {
  double base;
  final List<Keyframe> keys = [];
  Prop(this.base);

  bool get animated => keys.isNotEmpty;

  double at(double t) {
    if (keys.isEmpty) return base;
    if (t <= keys.first.t) return keys.first.v;
    if (t >= keys.last.t) return keys.last.v;
    for (var i = 0; i < keys.length - 1; i++) {
      final a = keys[i], b = keys[i + 1];
      if (t >= a.t && t < b.t) {
        final f = (t - a.t) / (b.t - a.t);
        return a.v + (b.v - a.v) * applyEasing(a.easing, f);
      }
    }
    return keys.last.v;
  }

  Keyframe? keyAt(double t, {double eps = 0.016}) {
    for (final k in keys) {
      if ((k.t - t).abs() <= eps) return k;
    }
    return null;
  }

  void _insert(Keyframe k) {
    keys.add(k);
    keys.sort((a, b) => a.t.compareTo(b.t));
  }

  /// Editing an animated prop writes a keyframe at [t]; a static prop just changes.
  void set(double t, double v) {
    if (!animated) {
      base = v;
      return;
    }
    final k = keyAt(t);
    if (k != null) {
      k.v = v;
    } else {
      _insert(Keyframe(t, v));
    }
  }

  void toggleKey(double t) {
    final k = keyAt(t);
    if (k != null) {
      if (keys.length == 1) base = k.v;
      keys.remove(k);
    } else {
      _insert(Keyframe(t, at(t)));
    }
  }

  Map<String, dynamic> toJson() => {'b': base, 'k': keys.map((e) => e.toJson()).toList()};
  void load(Map<String, dynamic> j) {
    base = (j['b'] as num).toDouble();
    keys
      ..clear()
      ..addAll((j['k'] as List).map((e) => Keyframe.fromJson(e as List)));
  }
}

class Layer {
  String id, name;
  LayerType type;
  double start, end; // seconds on the timeline
  double w, h; // unscaled size in project pixels
  final Prop x, y, scale, rotation, opacity;
  int color;
  double radius;
  String text;
  double fontSize;
  String? imagePath;
  String blend;
  bool visible;

  Layer({
    required this.id,
    required this.name,
    required this.type,
    required this.start,
    required this.end,
    required this.w,
    required this.h,
    double cx = 0,
    double cy = 0,
    this.color = 0xFF4FD5A6,
    this.radius = 0,
    this.text = '',
    this.fontSize = 100,
    this.imagePath,
    this.blend = 'Normal',
    this.visible = true,
  })  : x = Prop(cx),
        y = Prop(cy),
        scale = Prop(1),
        rotation = Prop(0),
        opacity = Prop(1);

  Map<String, Prop> get props => {'x': x, 'y': y, 'scale': scale, 'rotation': rotation, 'opacity': opacity};

  TextStyle get style => TextStyle(color: Color(color), fontSize: fontSize, fontWeight: FontWeight.w700, height: 1.0);

  void remeasure() {
    if (type != LayerType.text) return;
    final tp = TextPainter(
      text: TextSpan(text: text.isEmpty ? ' ' : text, style: style),
      textDirection: TextDirection.ltr,
    )..layout();
    w = math.max(10, tp.width);
    h = math.max(10, tp.height);
  }

  bool hit(Offset p, double t) {
    final a = -rotation.at(t) * math.pi / 180;
    final s = math.max(0.01, scale.at(t));
    final dx = p.dx - x.at(t), dy = p.dy - y.at(t);
    final lx = (dx * math.cos(a) - dy * math.sin(a)) / s;
    final ly = (dx * math.sin(a) + dy * math.cos(a)) / s;
    return lx.abs() <= w / 2 && ly.abs() <= h / 2;
  }

  Map<String, dynamic> toJson() => {
        'id': id,
        'name': name,
        'type': type.index,
        'start': start,
        'end': end,
        'w': w,
        'h': h,
        'color': color,
        'radius': radius,
        'text': text,
        'fontSize': fontSize,
        'image': imagePath,
        'blend': blend,
        'visible': visible,
        'props': props.map((k, v) => MapEntry(k, v.toJson())),
      };

  factory Layer.fromJson(Map<String, dynamic> j) {
    final l = Layer(
      id: j['id'] as String,
      name: j['name'] as String,
      type: LayerType.values[j['type'] as int],
      start: (j['start'] as num).toDouble(),
      end: (j['end'] as num).toDouble(),
      w: (j['w'] as num).toDouble(),
      h: (j['h'] as num).toDouble(),
      color: j['color'] as int,
      radius: (j['radius'] as num).toDouble(),
      text: (j['text'] as String?) ?? '',
      fontSize: (j['fontSize'] as num).toDouble(),
      imagePath: j['image'] as String?,
      blend: (j['blend'] as String?) ?? 'Normal',
      visible: (j['visible'] as bool?) ?? true,
    );
    final pr = j['props'] as Map<String, dynamic>;
    for (final e in l.props.entries) {
      if (pr[e.key] != null) e.value.load(pr[e.key] as Map<String, dynamic>);
    }
    return l;
  }
}

class Project {
  String id, name;
  int width, height, fps;
  double duration;
  int bg;
  int modified;
  final List<Layer> layers; // index 0 = bottom

  Project({
    required this.id,
    required this.name,
    required this.width,
    required this.height,
    this.fps = 30,
    this.duration = 5,
    this.bg = 0xFF000000,
    this.modified = 0,
    List<Layer>? layers,
  }) : layers = layers ?? [];

  Map<String, dynamic> toJson() => {
        'id': id,
        'name': name,
        'width': width,
        'height': height,
        'fps': fps,
        'duration': duration,
        'bg': bg,
        'modified': modified,
        'layers': layers.map((l) => l.toJson()).toList(),
      };

  factory Project.fromJson(Map<String, dynamic> j) => Project(
        id: j['id'] as String,
        name: j['name'] as String,
        width: j['width'] as int,
        height: j['height'] as int,
        fps: j['fps'] as int,
        duration: (j['duration'] as num).toDouble(),
        bg: j['bg'] as int,
        modified: (j['modified'] as int?) ?? 0,
        layers: (j['layers'] as List).map((e) => Layer.fromJson(e as Map<String, dynamic>)).toList(),
      );
}

String newId() => DateTime.now().microsecondsSinceEpoch.toRadixString(36);
