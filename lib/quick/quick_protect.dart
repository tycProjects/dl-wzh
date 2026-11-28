import 'dart:convert';
import 'dart:math' as math;
import 'dart:typed_data';
import 'package:crypto/crypto.dart';
import 'package:pointycastle/export.dart';
import 'der.dart';

/// Matches the server's SealedSite.java byte for byte (see generateWebViewWrapperProject's
/// `runtimeConfig` branch, writeJavaFileProtected(..., 'SealedSite.java', ...)) -- that is the
/// ONLY content-hiding path the real template actually has wired into shouldInterceptRequest
/// and the start-URL picker (`SealedSite.present(...) ? EMBED_HOST + path : file:///android_asset/...`).
///
/// There is no config file involved and nothing is generated per app on the Dart side: every
/// local site file is AES-256-GCM encrypted and stored as a plain top-level zip entry
/// "m/<hash of its path>" (outside assets/, so AssetManager never lists it and a casual unzip
/// finds no index.html, no file names, no extensions). The key is derived purely from this
/// app's own signing certificate -- SealedSite.init() re-derives the same key at runtime from
/// whatever certificate the running APK is actually signed with, so re-signing or editing the
/// APK silently breaks decryption instead of needing an online check.
///
/// blockCapture / blockRoot (FLAG_SECURE, root/debugger detection) have no counterpart anywhere
/// in the current template or server -- those two Options toggles are not backed by anything yet.
class QuickProtect {
  final bool encrypt;
  final Uint8List _key;
  QuickProtect._(this.encrypt, this._key);

  bool get active => encrypt;

  /// SealedSite.java's SALT_HEX constant, verbatim.
  static final Uint8List _salt =
      _hexDecode('260b98dfbf7272f53ec1bbc8af9a3e3db94be7d0a4155ffeae50c6f44372e570');

  static Uint8List _hexDecode(String hex) =>
      Uint8List.fromList([for (var i = 0; i < hex.length; i += 2) int.parse(hex.substring(i, i + 2), radix: 16)]);

  /// SealedSite.init()'s key: SHA-256(SALT_HEX_bytes ++ 0x01 ++ SHA-256(signing cert DER)).
  factory QuickProtect.create({required bool encrypt, required Uint8List cert}) {
    final certHash = sha256.convert(certToDer(cert)).bytes;
    final key = sha256.convert([..._salt, 1, ...certHash]).bytes;
    return QuickProtect._(encrypt, Uint8List.fromList(key));
  }

  /// SealedSite.entryName(rel): "m/" + first 16 bytes of SHA-256(SALT_HEX_bytes ++ utf8(rel)), as hex.
  /// No path->name table is kept anywhere -- the app recomputes this same name from the request path.
  String assetName(String path) {
    final h = sha256.convert([..._salt, ...utf8.encode(path)]).bytes;
    return 'm/${h.take(16).map((b) => b.toRadixString(16).padLeft(2, '0')).join()}';
  }

  /// SealedSite.get(): AES/GCM/NoPadding, 12-byte random nonce, NO associated data (unlike a
  /// typical per-file-bound scheme -- match it exactly or the app's Cipher.doFinal throws).
  Uint8List encryptFile(String path, Uint8List data) {
    final r = math.Random.secure();
    final nonce = Uint8List.fromList(List.generate(12, (_) => r.nextInt(256)));
    final gcm = GCMBlockCipher(AESEngine())
      ..init(true, AEADParameters(KeyParameter(_key), 128, nonce, Uint8List(0)));
    final out = gcm.process(data);
    return Uint8List.fromList([...nonce, ...out]);
  }
}
