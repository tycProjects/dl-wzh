import 'dart:io';
import 'dart:math' as math;
import 'dart:ui' as ui;
import 'package:ffmpeg_kit_flutter_new_min/ffmpeg_kit.dart';
import 'package:ffmpeg_kit_flutter_new_min/return_code.dart';
import 'package:flutter/material.dart';
import 'package:gal/gal.dart';
import 'package:path_provider/path_provider.dart';
import 'model.dart';
import 'renderer.dart';

class Exporter {
  static bool cancel = false;

  static int evenSize(int v, double scale) => math.max(2, (v * scale / 2).round() * 2);

  /// Renders each frame with [SceneRenderer] (same code as the preview), then encodes to MP4.
  static Future<String?> run(Project p, double scale, void Function(double, String) onProgress) async {
    for (final l in p.layers) {
      if (l.imagePath != null) await ImageStore.load(l.imagePath!);
    }
    final w = evenSize(p.width, scale), h = evenSize(p.height, scale);
    final tmp = await getTemporaryDirectory();
    final dir = Directory('${tmp.path}/export_${p.id}');
    if (await dir.exists()) await dir.delete(recursive: true);
    await dir.create(recursive: true);

    VideoStore.prefetch = false;
    try {
      final total = math.max(1, (p.duration * p.fps).round());
      for (var i = 0; i < total; i++) {
        if (cancel) return null;
        final tt = i / p.fps;
        for (final l in p.layers) {
          if (l.type == LayerType.video && l.visible && tt >= l.start && tt < l.end) await VideoStore.ensure(l, tt);
        }
        final rec = ui.PictureRecorder();
        final canvas = Canvas(rec);
        SceneRenderer.paint(canvas, Size(w.toDouble(), h.toDouble()), p, i / p.fps);
        final pic = rec.endRecording();
        final img = await pic.toImage(w, h);
        final bytes = await img.toByteData(format: ui.ImageByteFormat.png);
        img.dispose();
        pic.dispose();
        final name = 'f_${(i + 1).toString().padLeft(5, '0')}.png';
        await File('${dir.path}/$name').writeAsBytes(bytes!.buffer.asUint8List());
        onProgress(0.9 * (i + 1) / total, 'Rendering frame ${i + 1} / $total');
      }

      onProgress(0.92, 'Encoding MP4...');
      final safe = p.name.replaceAll(RegExp(r'[^A-Za-z0-9_-]'), '_');
      final out = '${tmp.path}/$safe.mp4';
      if (await File(out).exists()) await File(out).delete();

      final base = '-y -framerate ${p.fps} -i "${dir.path}/f_%05d.png"';
      // Hardware H.264 first, software MPEG-4 as a fallback so export never hard-fails.
      var s = await FFmpegKit.execute('$base -c:v h264_mediacodec -b:v 12M -pix_fmt yuv420p "$out"');
      if (!ReturnCode.isSuccess(await s.getReturnCode())) {
        s = await FFmpegKit.execute('$base -c:v mpeg4 -q:v 2 -pix_fmt yuv420p "$out"');
        if (!ReturnCode.isSuccess(await s.getReturnCode())) {
          throw Exception('Encoder failed: ${await s.getOutput()}');
        }
      }

      onProgress(0.97, 'Saving to gallery...');
      if (!await Gal.hasAccess()) await Gal.requestAccess();
      await Gal.putVideo(out);
      onProgress(1, 'Done');
      return out;
    } finally {
      VideoStore.prefetch = true;
      if (await dir.exists()) await dir.delete(recursive: true);
    }
  }
}
