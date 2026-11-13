import 'dart:async';
import 'dart:io';
import 'dart:typed_data';
import 'package:flutter/foundation.dart';
import 'package:http/http.dart' as http;
import 'package:image/image.dart' as img;
import 'package:video_compress/video_compress.dart';
import 'chat_cloud.dart';

/// Thrown with a message that is safe to show to the person.
class MediaError implements Exception {
  final String message;
  const MediaError(this.message);
  @override
  String toString() => message;
}

/// What is ready to upload.
class PreparedMedia {
  final String kind; // 'photo' | 'video'
  final Uint8List? bytes; // photos are small enough to hold in memory
  final String? path; // videos are streamed from a file
  final Uint8List? thumb;
  final int? durationMs; // videos only
  const PreparedMedia(this.kind, {this.bytes, this.path, this.thumb, this.durationMs});
}

/// Photos and videos for the Admin chat.
///
/// Everything is shrunk on the phone first and sent to the `chat-media` Supabase function, which passes
/// it straight on to Telegram. Nothing is kept in Supabase Storage, so it can never fill up.
class ChatMedia {
  static const photoMaxSide = 1280;
  static const photoQuality = 75;
  static const maxVideoSeconds = 60;
  static const maxVideoBytes = 12 * 1024 * 1024; // after shrinking
  static const maxPickedVideoBytes = 400 * 1024 * 1024; // before shrinking

  // ---- photos ----

  /// Reads the picture, turns it upright, scales it down to [photoMaxSide] and saves it as a JPEG.
  /// Done in a background isolate so the chat never freezes.
  static Future<PreparedMedia> preparePhoto(String path) async {
    try {
      final out = await compute(_shrinkPhoto, path);
      if (out == null) throw const MediaError('That picture can’t be read. Try another one.');
      return PreparedMedia('photo', bytes: out, thumb: out);
    } on MediaError {
      rethrow;
    } catch (_) {
      throw const MediaError('That picture can’t be read. Try another one.');
    }
  }

  // ---- videos ----

  static Future<PreparedMedia> prepareVideo(String path) async {
    final f = File(path);
    if (!await f.exists()) throw const MediaError('That video can’t be opened.');
    if (await f.length() > maxPickedVideoBytes) throw const MediaError('That video is too big. Pick one under 400 MB.');
    try {
      final info = await VideoCompress.getMediaInfo(path);
      final secs = (info.duration ?? 0) / 1000;
      if (secs > maxVideoSeconds) throw const MediaError('That video is too long. The limit is 60 seconds.');

      MediaInfo? out = await VideoCompress.compressVideo(
        path,
        quality: VideoQuality.Res960x540Quality,
        deleteOrigin: false,
        includeAudio: true,
        frameRate: 24,
      );
      var size = out?.file == null ? 0 : await out!.file!.length();
      // Still big (a long or very busy clip): one more try at a smaller picture.
      if (out?.file == null || size > maxVideoBytes) {
        out = await VideoCompress.compressVideo(
          path,
          quality: VideoQuality.Res640x480Quality,
          deleteOrigin: false,
          includeAudio: true,
          frameRate: 20,
        );
        size = out?.file == null ? 0 : await out!.file!.length();
      }
      if (out?.file == null) throw const MediaError('Couldn’t shrink that video. Try another one.');
      if (size > maxVideoBytes) throw const MediaError('That video is still too big after shrinking. Try a shorter one.');

      Uint8List? thumb;
      try {
        thumb = await VideoCompress.getByteThumbnail(path, quality: 50, position: -1);
      } catch (_) {}
      final ms = (out!.duration ?? info.duration ?? 0).round();
      return PreparedMedia('video', path: out.file!.path, thumb: thumb, durationMs: ms > 0 ? ms : null);
    } on MediaError {
      rethrow;
    } catch (_) {
      throw const MediaError('Couldn’t shrink that video. Try another one.');
    }
  }

  /// Frees the temporary shrunk copies the video tool leaves in the phone's cache.
  static Future<void> clearCache() async {
    try {
      await VideoCompress.deleteAllCache();
    } catch (_) {}
  }

  // ---- upload ----

  static Future<void> send(PreparedMedia m, String clientId) async {
    final req = http.MultipartRequest('POST', Uri.parse('$kSupabaseUrl/functions/v1/chat-media'));
    req.headers.addAll({
      'apikey': kSupabaseAnonKey,
      if (kSupabaseAnonKey.startsWith('eyJ')) 'Authorization': 'Bearer $kSupabaseAnonKey',
    });
    req.fields['key'] = ChatCloud.keyFor(clientId);
    req.fields['kind'] = m.kind;
    if (m.kind == 'photo') {
      req.files.add(http.MultipartFile.fromBytes('file', m.bytes!, filename: 'photo.jpg'));
    } else {
      req.files.add(await http.MultipartFile.fromPath('file', m.path!, filename: 'video.mp4'));
    }
    try {
      final streamed = await req.send().timeout(const Duration(seconds: 150));
      final res = await http.Response.fromStream(streamed);
      if (res.statusCode >= 200 && res.statusCode < 300) return;
      if (res.statusCode == 413) throw const MediaError('That file is too big to send.');
      if (res.statusCode == 429) throw const MediaError('You’ve sent a lot of files. Try again in a few minutes.');
      if (res.statusCode == 502) throw const MediaError('Couldn’t reach Telegram. Try again soon.');
      if (res.statusCode == 404) throw const MediaError('Sending files isn’t set up yet.');
      throw const MediaError('Couldn’t send that. Try again in a bit.');
    } on MediaError {
      rethrow;
    } on TimeoutException {
      throw const MediaError('Sending took too long. Check your connection and retry.');
    } catch (_) {
      throw const MediaError('Can’t reach the server. Check your connection.');
    }
  }
}

/// Runs in a background isolate.
Uint8List? _shrinkPhoto(String path) {
  final raw = File(path).readAsBytesSync();
  var im = img.decodeImage(raw);
  if (im == null) return null;
  im = img.bakeOrientation(im);
  const side = ChatMedia.photoMaxSide;
  if (im.width > side || im.height > side) {
    im = im.width >= im.height
        ? img.copyResize(im, width: side, interpolation: img.Interpolation.average)
        : img.copyResize(im, height: side, interpolation: img.Interpolation.average);
  }
  return Uint8List.fromList(img.encodeJpg(im, quality: ChatMedia.photoQuality));
}
