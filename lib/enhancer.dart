import 'dart:isolate';
import 'dart:math' as math;
import 'dart:typed_data';
import 'package:image/image.dart' as img;

/// A look. [sharp] is edge sharpening; the rest is color. All values are at "Normal" detail.
class Preset {
  final String name, hint;
  final double sharp, contrast, sat, bright;
  const Preset(this.name, this.hint,
      {this.sharp = 0, this.contrast = 1, this.sat = 1, this.bright = 1});
}

const presets = <Preset>[
  Preset('Auto', 'Best for most photos', sharp: 1.0, contrast: 1.05, sat: 1.06),
  Preset('Portrait', 'Natural, brighter faces', sharp: 0.6, contrast: 1.03, sat: 1.03, bright: 1.04),
  Preset('Sharp', 'Crisp edges and detail', sharp: 1.6, contrast: 1.06),
  Preset('Vivid', 'Rich color and contrast', sharp: 1.0, contrast: 1.16, sat: 1.2),
  Preset('Clean', 'Bigger, colors unchanged', sharp: 0.8),
];

/// Output size choices in plain words. [longEdge] 0 = just double the size.
class Quality {
  final String label, sub;
  final int longEdge;
  const Quality(this.label, this.sub, this.longEdge);
}

const qualities = <Quality>[
  Quality('Auto', '2× bigger', 0),
  Quality('1080p', 'Full HD', 1920),
  Quality('2K', 'Quad HD', 2560),
  Quality('4K', 'Ultra HD', 3840),
];

/// Max output pixels, so phones do not run out of memory.
const maxPixels = 16000000;

/// Scale factor for a quality choice. Never shrinks a photo (result is >= 1).
double scaleFor(Quality q, int w, int h) {
  var s = q.longEdge == 0 ? 2.0 : q.longEdge / math.max(w, h);
  if (s < 1.02) return 1.0;
  while (w * h * s * s > maxPixels && s > 1.0) {
    s -= 0.05;
  }
  return s < 1.02 ? 1.0 : s;
}

class EnhanceParams {
  final Uint8List bytes;
  final double scale, sharp, contrast, sat, bright;
  final bool fromAi; // true: bytes already went through the AI model
  const EnhanceParams(this.bytes,
      {this.fromAi = false,
      required this.scale,
      required this.sharp,
      required this.contrast,
      required this.sat,
      required this.bright});
}

class EnhanceResult {
  final Uint8List bytes;
  final int width, height;
  const EnhanceResult(this.bytes, this.width, this.height);
}

/// Runs in a background isolate so the screen stays smooth.
Future<EnhanceResult> enhance(EnhanceParams p) => Isolate.run(() => _run(p));

int _c(num v) => v < 0 ? 0 : (v > 255 ? 255 : v.round());

/// Fixes the phone-camera rotation and returns a PNG, ready for the AI engine.
Future<Uint8List> prepareForAi(Uint8List bytes) => Isolate.run(() {
      var im = img.decodeImage(bytes);
      if (im == null) throw Exception('Could not read this image');
      im = img.bakeOrientation(im);
      return img.encodePng(im, level: 1);
    });

EnhanceResult _run(EnhanceParams p) {
  var im = img.decodeImage(p.bytes);
  if (im == null) throw Exception('Could not read this image');
  im = img.bakeOrientation(im);

  var s = p.scale;
  if (s < 0.98) {
    // AI output is 4x; shrink it to the exact size asked for (looks extra clean).
    im = img.copyResize(im,
        width: math.max(1, (im.width * s).round()),
        height: math.max(1, (im.height * s).round()),
        interpolation: img.Interpolation.average);
    s = 1.0;
  } else {
    if (s < 1.02) s = 1.0;
    while (im.width * im.height * s * s > maxPixels && s > 1.0) {
      s -= 0.05;
    }
    if (s > 1.02) {
      im = img.copyResize(im,
          width: (im.width * s).round(),
          height: (im.height * s).round(),
          interpolation: img.Interpolation.cubic);
    } else {
      s = 1.0;
    }
  }

  // Edge-safe sharpening: crisp edges, no halos, no boosted noise.
  // AI output is already sharp, so it only gets a light touch.
  final sharp = p.fromAi ? p.sharp * 0.35 : p.sharp;
  if (sharp > 0.01) {
    final r = p.fromAi ? 1 : (s >= 2.6 ? 3 : (s >= 1.6 ? 2 : 1));
    _sharpen(im, sharp, r);
    if (r > 1) _sharpen(im, sharp * 0.6, 1);
  }

  if ((p.contrast - 1).abs() > 0.005 ||
      (p.sat - 1).abs() > 0.005 ||
      (p.bright - 1).abs() > 0.005) {
    im = img.adjustColor(im, contrast: p.contrast, saturation: p.sat, brightness: p.bright);
  }
  return EnhanceResult(img.encodePng(im, level: 3), im.width, im.height);
}

int _cl(int v, int n) => v < 0 ? 0 : (v >= n ? n - 1 : v);

Uint8List _box(Uint8List s, int w, int h, int r, bool horiz) {
  final o = Uint8List(w * h);
  final len = horiz ? w : h, lines = horiz ? h : w;
  final step = horiz ? 1 : w, lstep = horiz ? w : 1, d = 2 * r + 1;
  for (var l = 0; l < lines; l++) {
    final base = l * lstep;
    var sum = 0;
    for (var k = -r; k <= r; k++) {
      sum += s[base + _cl(k, len) * step];
    }
    for (var k = 0; k < len; k++) {
      o[base + k * step] = (sum + (d >> 1)) ~/ d;
      sum += s[base + _cl(k + r + 1, len) * step] - s[base + _cl(k - r, len) * step];
    }
  }
  return o;
}

Uint8List _blur(Uint8List y, int w, int h, int r) {
  var a = _box(y, w, h, r, true);
  a = _box(a, w, h, r, false);
  a = _box(a, w, h, r, true);
  return _box(a, w, h, r, false);
}

/// Sharpens brightness only. A change is skipped when it is just noise, and every pixel is
/// kept between its neighbours' darkest and brightest value, which is what stops halos.
void _sharpen(img.Image im, double amount, int radius) {
  final w = im.width, h = im.height;
  final y = Uint8List(w * h);
  var i = 0;
  for (var j = 0; j < h; j++) {
    for (var x = 0; x < w; x++, i++) {
      final p = im.getPixel(x, j);
      y[i] = (p.r.toInt() * 77 + p.g.toInt() * 150 + p.b.toInt() * 29) >> 8;
    }
  }
  final b = _blur(y, w, h, radius);
  final win = radius >= 2 ? 2 : 1;
  const low = 2.0, high = 12.0;
  for (var j = 0; j < h; j++) {
    for (var x = 0; x < w; x++) {
      final idx = j * w + x;
      final d = (y[idx] - b[idx]).toDouble();
      final ad = d.abs();
      if (ad <= low) continue;
      var g = ad >= high ? 1.0 : (ad - low) / (high - low);
      g = g * g;
      var ny = y[idx] + amount * d * g;
      var mn = 255, mx = 0;
      for (var dy = -win; dy <= win; dy++) {
        final row = _cl(j + dy, h) * w;
        for (var dx = -win; dx <= win; dx++) {
          final v = y[row + _cl(x + dx, w)];
          if (v < mn) mn = v;
          if (v > mx) mx = v;
        }
      }
      if (ny < mn) ny = mn.toDouble();
      if (ny > mx) ny = mx.toDouble();
      final dl = (ny - y[idx]).round();
      if (dl == 0) continue;
      final p = im.getPixel(x, j);
      p.r = _c(p.r + dl);
      p.g = _c(p.g + dl);
      p.b = _c(p.b + dl);
    }
  }
}
