import 'dart:math' as math;
import 'package:flutter/material.dart';
import 'model.dart';

// ---------------------------------------------------------------------------
// Colour matrices (4x5, same layout as ColorFilter.matrix)
// ---------------------------------------------------------------------------

const List<double> kIdentity = [1, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 1, 0];

/// Applies [a] first, then [b].
List<double> thenM(List<double> a, List<double> b) {
  final r = List<double>.filled(20, 0);
  for (var i = 0; i < 4; i++) {
    for (var j = 0; j < 5; j++) {
      var s = 0.0;
      for (var k = 0; k < 4; k++) {
        s += b[i * 5 + k] * a[k * 5 + j];
      }
      if (j == 4) s += b[i * 5 + 4];
      r[i * 5 + j] = s;
    }
  }
  return r;
}

List<double> lerpM(List<double> a, List<double> b, double t) => [for (var i = 0; i < 20; i++) a[i] + (b[i] - a[i]) * t];

List<double> brightnessM(double b) {
  final o = b * 255;
  return [1, 0, 0, 0, o, 0, 1, 0, 0, o, 0, 0, 1, 0, o, 0, 0, 0, 1, 0];
}

List<double> contrastM(double c) {
  final o = 255 * 0.5 * (1 - c);
  return [c, 0, 0, 0, o, 0, c, 0, 0, o, 0, 0, c, 0, o, 0, 0, 0, 1, 0];
}

List<double> saturationM(double s) {
  const lr = 0.2126, lg = 0.7152, lb = 0.0722;
  return [
    lr * (1 - s) + s, lg * (1 - s), lb * (1 - s), 0, 0, //
    lr * (1 - s), lg * (1 - s) + s, lb * (1 - s), 0, 0,
    lr * (1 - s), lg * (1 - s), lb * (1 - s) + s, 0, 0,
    0, 0, 0, 1, 0,
  ];
}

/// CapCut-style one-tap looks. null = no filter.
final Map<String, List<double>?> filterPresets = {
  'None': null,
  'Vivid': thenM(saturationM(1.45), contrastM(1.1)),
  'Warm': const [1.1, 0, 0, 0, 8, 0, 1.0, 0, 0, 0, 0, 0, 0.88, 0, -6, 0, 0, 0, 1, 0],
  'Cool': const [0.9, 0, 0, 0, -4, 0, 1.0, 0, 0, 0, 0, 0, 1.12, 0, 10, 0, 0, 0, 1, 0],
  'Cinema': const [1.12, 0, -0.05, 0, 6, 0, 1.02, 0.04, 0, 0, -0.1, 0.08, 1.15, 0, 6, 0, 0, 0, 1, 0],
  'B&W': saturationM(0),
  'Noir': thenM(saturationM(0), thenM(contrastM(1.4), brightnessM(-0.05))),
  'Vintage': const [0.393, 0.769, 0.189, 0, 8, 0.349, 0.686, 0.168, 0, 0, 0.272, 0.534, 0.131, 0, 0, 0, 0, 0, 1, 0],
  'Fade': thenM(contrastM(0.8), thenM(saturationM(0.8), brightnessM(0.08))),
};

/// Combined filter + brightness/contrast/saturation for layer [l] at time [t]. null = nothing to apply.
List<double>? layerMatrix(Layer l, double t) {
  final f = filterPresets[l.filter];
  final br = l.bright.at(t), ct = l.contrast.at(t), sa = l.sat.at(t);
  final mix = l.fmix.at(t).clamp(0.0, 1.0).toDouble();
  final neutral = br.abs() < 0.002 && (ct - 1).abs() < 0.002 && (sa - 1).abs() < 0.002;
  if (f == null && neutral) return null;
  var m = f == null ? kIdentity : lerpM(kIdentity, f, mix);
  if (!neutral) {
    m = thenM(m, saturationM(sa));
    m = thenM(m, contrastM(ct));
    m = thenM(m, brightnessM(br));
  }
  return m;
}

// ---------------------------------------------------------------------------
// Animations (In / Out / Loop). Pure functions of time, so preview == export.
// ---------------------------------------------------------------------------

const inOutNames = ['None', 'Fade', 'Slide up', 'Slide side', 'Zoom', 'Pop', 'Spin'];
const loopNames = ['None', 'Pulse', 'Wiggle', 'Float', 'Shake', 'Rotate'];

class AnimState {
  double dx = 0, dy = 0, rot = 0, scale = 1, opacity = 1;
}

void _inOut(AnimState o, int kind, double p, int dir, double pw, double ph) {
  if (kind == 0 || p >= 1) return;
  final e = Curves.easeOutCubic.transform(p);
  final k = 1 - e;
  switch (kind) {
    case 1:
      o.opacity *= p;
      break;
    case 2:
      o.dy += dir * k * ph * 0.12;
      o.opacity *= e;
      break;
    case 3:
      o.dx -= dir * k * pw * 0.3;
      o.opacity *= e;
      break;
    case 4:
      o.scale *= 0.2 + 0.8 * e;
      o.opacity *= e;
      break;
    case 5:
      o.scale *= Curves.easeOutBack.transform(p);
      break;
    case 6:
      o.rot -= dir * 180 * k;
      o.scale *= e;
      o.opacity *= e;
      break;
  }
}

void _loop(AnimState o, int kind, double lt) {
  const tau = 2 * math.pi;
  switch (kind) {
    case 1:
      o.scale *= 1 + 0.07 * math.sin(lt * tau * 1.4);
      break;
    case 2:
      o.rot += 7 * math.sin(lt * tau * 1.6);
      break;
    case 3:
      o.dy += 18 * math.sin(lt * tau * 0.7);
      break;
    case 4:
      o.dx += 9 * math.sin(lt * 52) + 4 * math.sin(lt * 97);
      o.dy += 7 * math.sin(lt * 61 + 1);
      break;
    case 5:
      o.rot += lt * 90;
      break;
  }
}

AnimState layerAnim(Layer l, double t, double pw, double ph) {
  final o = AnimState();
  final d = math.max(0.05, l.animDur);
  final lt = t - l.start, rt = l.end - t;
  if (l.animIn != 0 && lt < d) _inOut(o, l.animIn, (lt / d).clamp(0.0, 1.0).toDouble(), 1, pw, ph);
  if (l.animOut != 0 && rt < d) _inOut(o, l.animOut, (rt / d).clamp(0.0, 1.0).toDouble(), -1, pw, ph);
  if (l.animLoop != 0) _loop(o, l.animLoop, lt);
  return o;
}
