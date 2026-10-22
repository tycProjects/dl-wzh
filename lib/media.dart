import 'dart:io';
import 'dart:ui' as ui;
import 'package:ffmpeg_kit_flutter_new_min/ffmpeg_kit.dart';
import 'package:ffmpeg_kit_flutter_new_min/return_code.dart';
import 'storage.dart';

class ImportedVideo {
  final String dir;
  final int frames, w, h;
  final double fps, duration;
  const ImportedVideo(this.dir, this.frames, this.fps, this.duration, this.w, this.h);
}

class MediaImport {
  /// Videos are turned into a folder of JPEG frames at the project fps. The preview and the
  /// exporter then treat each frame like an image, so effects work the same on video.
  static const maxSeconds = 60;

  static Future<ImportedVideo> video(String src, int fps) async {
    final d = await Store.newVideoDir();
    final s = await FFmpegKit.execute(
      '-y -t $maxSeconds -i "$src" -an '
      '-vf "fps=$fps,scale=1280:1280:force_original_aspect_ratio=decrease:force_divisible_by=2" '
      '-q:v 3 "${d.path}/f_%05d.jpg"',
    );
    if (!ReturnCode.isSuccess(await s.getReturnCode())) {
      if (await d.exists()) await d.delete(recursive: true);
      throw Exception('Could not read this video');
    }
    final frames = d.listSync().whereType<File>().where((f) => f.path.endsWith('.jpg')).length;
    if (frames == 0) {
      if (await d.exists()) await d.delete(recursive: true);
      throw Exception('This video has no readable frames');
    }
    final codec = await ui.instantiateImageCodec(await File('${d.path}/f_00001.jpg').readAsBytes());
    final fr = await codec.getNextFrame();
    final w = fr.image.width, h = fr.image.height;
    fr.image.dispose();
    codec.dispose();
    return ImportedVideo(d.path, frames, fps.toDouble(), frames / fps, w, h);
  }
}
