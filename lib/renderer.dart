import 'dart:io';
import 'dart:math' as math;
import 'dart:ui' as ui;
import 'package:flutter/material.dart';
import 'model.dart';

class ImageStore {
  static final Map<String, ui.Image> _m = {};
  static ui.Image? get(String path) => _m[path];

  static Future<void> load(String path) async {
    if (_m.containsKey(path)) return;
    try {
      final codec = await ui.instantiateImageCodec(await File(path).readAsBytes());
      _m[path] = (await codec.getNextFrame()).image;
    } catch (_) {}
  }
}

/// One painter for everything: the live preview, thumbnails and the exporter all call this,
/// so what you see is exactly what gets encoded.
class SceneRenderer {
  static void paint(Canvas c, Size size, Project p, double t) {
    c.save();
    c.scale(size.width / p.width, size.height / p.height);
    final full = Rect.fromLTWH(0, 0, p.width.toDouble(), p.height.toDouble());
    c.clipRect(full);
    c.drawRect(full, Paint()..color = Color(p.bg));
    for (final l in p.layers) {
      if (!l.visible || t < l.start || t >= l.end) continue;
      _layer(c, l, t);
    }
    c.restore();
  }

  static void _layer(Canvas c, Layer l, double t) {
    final op = l.opacity.at(t).clamp(0.0, 1.0).toDouble();
    if (op <= 0) return;
    final sx = l.sx.at(t), sy = l.sy.at(t);
    c.save();
    c.translate(l.x.at(t), l.y.at(t));
    c.rotate(l.rotation.at(t) * math.pi / 180);
    c.scale(sx, sy);
    final rect = Rect.fromCenter(center: Offset.zero, width: l.w, height: l.h);
    final bm = blendModes[l.blend] ?? BlendMode.srcOver;
    final needLayer = op < 1 || bm != BlendMode.srcOver;
    if (needLayer) {
      c.saveLayer(
        rect.inflate(4),
        Paint()
          ..color = Color.fromRGBO(255, 255, 255, op)
          ..blendMode = bm,
      );
    }
    final fill = Paint()
      ..color = Color(l.color)
      ..isAntiAlias = true;
    switch (l.type) {
      case LayerType.rect:
        c.drawRRect(RRect.fromRectAndRadius(rect, Radius.circular(l.radius)), fill);
        break;
      case LayerType.ellipse:
        c.drawOval(rect, fill);
        break;
      case LayerType.text:
        final tp = TextPainter(
          text: TextSpan(text: l.text, style: l.style),
          textDirection: TextDirection.ltr,
        )..layout();
        tp.paint(c, Offset(-tp.width / 2, -tp.height / 2));
        break;
      case LayerType.image:
        final img = l.imagePath == null ? null : ImageStore.get(l.imagePath!);
        if (img == null) {
          c.drawRect(rect, Paint()..color = const Color(0xFF3A3A3C));
        } else {
          c.drawImageRect(
            img,
            Rect.fromLTWH(0, 0, img.width.toDouble(), img.height.toDouble()),
            rect,
            Paint()..filterQuality = FilterQuality.medium,
          );
        }
        break;
    }
    if (needLayer) c.restore();
    c.restore();
  }

  /// Selection box + corner handles (editor only, never exported).
  static void overlay(Canvas c, Size size, Project p, Layer l, double t) {
    final k = size.width / p.width;
    c.save();
    c.scale(k, size.height / p.height);
    c.translate(l.x.at(t), l.y.at(t));
    c.rotate(l.rotation.at(t) * math.pi / 180);
    final rect = Rect.fromCenter(
      center: Offset.zero,
      width: l.w * l.sx.at(t).abs(),
      height: l.h * l.sy.at(t).abs(),
    );
    final stroke = Paint()
      ..style = PaintingStyle.stroke
      ..strokeWidth = 2 / k
      ..color = const Color(0xFF4FD5A6);
    c.drawRect(rect, stroke);
    final dot = Paint()..color = Colors.white;
    for (final o in [rect.topLeft, rect.topRight, rect.bottomLeft, rect.bottomRight]) {
      c.drawCircle(o, 6 / k, dot);
      c.drawCircle(o, 6 / k, stroke);
    }
    c.restore();
  }
}
