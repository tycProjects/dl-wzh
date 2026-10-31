import 'dart:isolate';
import 'dart:typed_data';
import 'package:image/image.dart' as img;

/// One look (like a Wink tool). Values are at 100% strength.
class Preset {
  final String name;
  final double sharp, smooth, contrast, sat, bright;
  const Preset(this.name,
      {this.sharp = 0, this.smooth = 0, this.contrast = 1, this.sat = 1, this.bright = 1});
}

const presets = <Preset>[
  Preset('Auto', sharp: 0.6, contrast: 1.06, sat: 1.08),
  Preset('Portrait', sharp: 0.4, smooth: 0.3, contrast: 1.03, sat: 1.04, bright: 1.05),
  Preset('Crisp', sharp: 1.1, contrast: 1.08),
  Preset('HDR', sharp: 0.7, contrast: 1.2, sat: 1.22),
  Preset('Upscale', sharp: 0.25),
];

class EnhanceParams {
  final Uint8List bytes;
  final double scale, sharp, smooth, contrast, sat, bright;
  const EnhanceParams(this.bytes,
      {required this.scale,
      required this.sharp,
      required this.smooth,
      required this.contrast,
      required this.sat,
      required this.bright});
}

/// Max output pixels, so phones do not run out of memory.
const maxPixels = 16000000;

/// Runs in a background isolate so the UI stays smooth.
Future<Uint8List> enhance(EnhanceParams p) => Isolate.run(() => _run(p));

int _c(num v) => v < 0 ? 0 : (v > 255 ? 255 : v.round());

Uint8List _run(EnhanceParams p) {
  var im = img.decodeImage(p.bytes);
  if (im == null) throw Exception('Could not read this image');
  im = img.bakeOrientation(im);

  var s = p.scale;
  while (im.width * im.height * s * s > maxPixels && s > 1) {
    s -= 0.25;
  }
  if (s > 1) {
    im = img.copyResize(im,
        width: (im.width * s).round(),
        height: (im.height * s).round(),
        interpolation: img.Interpolation.cubic);
  }

  // Soft smoothing (portrait): blend toward a blurred copy.
  if (p.smooth > 0.01) {
    final blur = img.gaussianBlur(im.clone(), radius: 3);
    for (var y = 0; y < im.height; y++) {
      for (var x = 0; x < im.width; x++) {
        final a = im.getPixel(x, y), b = blur.getPixel(x, y);
        a.r = _c(a.r + (b.r - a.r) * p.smooth);
        a.g = _c(a.g + (b.g - a.g) * p.smooth);
        a.b = _c(a.b + (b.b - a.b) * p.smooth);
      }
    }
  }

  // Unsharp mask.
  if (p.sharp > 0.01) {
    final blur = img.gaussianBlur(im.clone(), radius: 2);
    for (var y = 0; y < im.height; y++) {
      for (var x = 0; x < im.width; x++) {
        final a = im.getPixel(x, y), b = blur.getPixel(x, y);
        a.r = _c(a.r + p.sharp * (a.r - b.r));
        a.g = _c(a.g + p.sharp * (a.g - b.g));
        a.b = _c(a.b + p.sharp * (a.b - b.b));
      }
    }
  }

  if ((p.contrast - 1).abs() > 0.005 || (p.sat - 1).abs() > 0.005 || (p.bright - 1).abs() > 0.005) {
    im = img.adjustColor(im, contrast: p.contrast, saturation: p.sat, brightness: p.bright);
  }
  return Uint8List.fromList(img.encodeJpg(im, quality: 95));
}
