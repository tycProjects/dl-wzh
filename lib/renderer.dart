import 'dart:io';
import 'dart:math' as math;
import 'dart:ui' as ui;
import 'package:flutter/material.dart';
import 'effects.dart';
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

/// Small LRU cache of decoded video frames. The painter asks synchronously; misses are loaded in the
/// background (with a little look-ahead) and [tick] fires so the canvas repaints. The exporter awaits
/// [ensure] for every frame instead, so exports never show a stale frame.
class VideoStore {
  static const _max = 30;
  static bool prefetch = true;
  static final ValueNotifier<int> tick = ValueNotifier(0);
  static final Map<String, ui.Image> _m = {};
  static final Map<String, ui.Image> _last = {};
  static final Set<String> _busy = {};

  static ui.Image? get(Layer l, double t) {
    final dir = l.videoDir;
    if (dir == null) return null;
    final i = l.frameAt(t);
    final p = l.framePath(i);
    final img = _m.remove(p);
    if (prefetch) {
      for (var k = 0; k < 6 && i + k < l.vFrames; k++) {
        _request(l.framePath(i + k));
      }
    }
    if (img != null) {
      _m[p] = img; // most recently used
      _last[dir] = img;
      return img;
    }
    return _last[dir];
  }

  static Future<void> ensure(Layer l, double t) async {
    final p = l.framePath(l.frameAt(t));
    final img = _m.remove(p);
    if (img != null) {
      _m[p] = img;
      _last[l.videoDir!] = img;
      return;
    }
    await _load(p);
    final n = _m[p];
    if (n != null) _last[l.videoDir!] = n;
  }

  static void _request(String p) {
    if (_m.containsKey(p) || _busy.contains(p)) return;
    _load(p).then((_) => tick.value++);
  }

  static Future<void> _load(String p) async {
    if (_m.containsKey(p) || !_busy.add(p)) return;
    try {
      final codec = await ui.instantiateImageCodec(await File(p).readAsBytes());
      final img = (await codec.getNextFrame()).image;
      codec.dispose();
      _m[p] = img;
      while (_m.length > _max) {
        final k = _m.keys.first;
        final old = _m.remove(k)!;
        _last.removeWhere((_, v) => identical(v, old));
        old.dispose();
      }
    } catch (_) {
    } finally {
      _busy.remove(p);
    }
  }
}

/// One painter for everything: the live preview, thumbnails and the exporter all call this,
/// so what you see is exactly what gets encoded.
class SceneRenderer {
  static const _red = ColorFilter.matrix(<double>[1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0]);
  static const _cyan = ColorFilter.matrix(<double>[0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 1, 0]);

  static void paint(Canvas c, Size size, Project p, double t) {
    c.save();
    c.scale(size.width / p.width, size.height / p.height);
    final full = Rect.fromLTWH(0, 0, p.width.toDouble(), p.height.toDouble());
    c.clipRect(full);
    c.drawRect(full, Paint()..color = Color(p.bg));
    for (final l in p.layers) {
      if (!l.visible || t < l.start || t >= l.end) continue;
      _layer(c, p, l, t);
    }
    c.restore();
  }

  static void _layer(Canvas c, Project p, Layer l, double t) {
    final a = layerAnim(l, t, p.width.toDouble(), p.height.toDouble());
    final op = (l.opacity.at(t) * a.opacity).clamp(0.0, 1.0).toDouble();
    if (op <= 0) return;
    c.save();
    c.translate(l.x.at(t) + a.dx, l.y.at(t) + a.dy);
    c.rotate((l.rotation.at(t) + a.rot) * math.pi / 180);
    c.scale(l.sx.at(t) * a.scale, l.sy.at(t) * a.scale);
    final rect = Rect.fromCenter(center: Offset.zero, width: l.w, height: l.h);
    final bm = blendModes[l.blend] ?? BlendMode.srcOver;

    final blurS = l.blur.at(t).clamp(0.0, 80.0).toDouble();
    final glowV = l.glow.at(t).clamp(0.0, 1.0).toDouble();
    final shV = l.shadow.at(t).clamp(0.0, 1.0).toDouble();
    final splitV = l.split.at(t).clamp(0.0, 80.0).toDouble();
    final cm = layerMatrix(l, t);
    final glowSigma = 6 + glowV * 30;
    final pad = 4 + math.max(math.max(blurS * 3, glowV > 0.01 ? glowSigma * 3 : 0.0), math.max(shV > 0.01 ? 70.0 : 0.0, splitV));
    final bounds = rect.inflate(pad.toDouble());

    final needLayer = op < 1 || bm != BlendMode.srcOver;
    if (needLayer) {
      c.saveLayer(
        bounds,
        Paint()
          ..color = Color.fromRGBO(255, 255, 255, op)
          ..blendMode = bm,
      );
    }

    void base() => _content(c, l, rect, t);
    void filtered() {
      if (cm == null) {
        base();
        return;
      }
      c.saveLayer(bounds, Paint()..colorFilter = ColorFilter.matrix(cm));
      base();
      c.restore();
    }

    // drop shadow
    if (shV > 0.01) {
      c.save();
      c.translate(0, 14);
      c.saveLayer(
        bounds,
        Paint()
          ..imageFilter = ui.ImageFilter.blur(sigmaX: 14, sigmaY: 14, tileMode: TileMode.decal)
          ..colorFilter = ColorFilter.mode(Color.fromRGBO(0, 0, 0, shV * 0.85), BlendMode.srcIn),
      );
      base();
      c.restore();
      c.restore();
    }
    // glow: two blurred copies under the sharp one
    if (glowV > 0.01) {
      for (final m in const [1.0, 0.45]) {
        c.saveLayer(
          bounds,
          Paint()
            ..imageFilter = ui.ImageFilter.blur(sigmaX: glowSigma * m, sigmaY: glowSigma * m, tileMode: TileMode.decal)
            ..color = Color.fromRGBO(255, 255, 255, math.min(1.0, glowV * 1.2)),
        );
        filtered();
        c.restore();
      }
    }
    // RGB split / glitch: red and cyan ghosts pushed apart
    if (splitV > 0.5) {
      for (final g in [(-1.0, _red), (1.0, _cyan)]) {
        c.save();
        c.translate(g.$1 * splitV, 0);
        c.saveLayer(bounds, Paint()..colorFilter = g.$2..blendMode = BlendMode.screen);
        filtered();
        c.restore();
        c.restore();
      }
    }
    // the layer itself (optionally blurred)
    if (blurS < 0.1) {
      filtered();
    } else {
      c.saveLayer(bounds, Paint()..imageFilter = ui.ImageFilter.blur(sigmaX: blurS, sigmaY: blurS, tileMode: TileMode.decal));
      filtered();
      c.restore();
    }

    if (needLayer) c.restore();
    c.restore();
  }

  static void _content(Canvas c, Layer l, Rect rect, double t) {
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
        _img(c, l.imagePath == null ? null : ImageStore.get(l.imagePath!), rect);
        break;
      case LayerType.video:
        _img(c, VideoStore.get(l, t), rect);
        break;
    }
  }

  static void _img(Canvas c, ui.Image? img, Rect rect) {
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
