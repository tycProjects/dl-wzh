import 'dart:convert';
import 'dart:math' as math;
import 'dart:typed_data';
import 'package:crypto/crypto.dart';
import 'package:pointycastle/export.dart';
import 'der.dart';

/// Per-app protection for Quick builds. Everything here is made fresh for each app,
/// so no two APKs share a key. The template (see TEMPLATE_PROTECT.md) reads it from assets/dexopt/.wc.
///
///  - encrypt: every site file becomes assets/<path>.wzp = nonce(12) + AES-256-GCM(ciphertext + tag),
///    with the file path as associated data, so a file can't be swapped for another one.
///  - certSha: SHA-256 of this app's signing certificate. The template compares it with the
///    certificate it is really signed with, and closes if they differ (re-signed / cloned APK).
///  - blockCapture / blockRoot: switches the template reads.
class QuickProtect {
  final bool encrypt, blockCapture, blockRoot;
  final Uint8List _key;
  final String certSha;
  final String _mask;
  QuickProtect._(this.encrypt, this.blockCapture, this.blockRoot, this._key, this.certSha, this._mask);

  bool get active => encrypt || blockCapture || blockRoot;

  factory QuickProtect.create({
    required bool encrypt,
    required bool blockCapture,
    required bool blockRoot,
    required Uint8List cert,
    required String packageName,
  }) {
    final r = math.Random.secure();
    final key = Uint8List.fromList(List.generate(32, (_) => r.nextInt(256)));
    final sha = sha256.convert(certToDer(cert)).toString();
    return QuickProtect._(encrypt, blockCapture, blockRoot, key, sha, packageName);
  }

  /// The key is stored XOR-ed with a hash of the package name, so it isn't a plain string in the APK.
  /// This slows down casual unzipping only; the template needs the key to run, so it is never unbreakable.
  Map<String, dynamic> toJson() {
    final m = sha256.convert(utf8.encode('wzh:$_mask')).bytes;
    final masked = Uint8List.fromList([for (var i = 0; i < 32; i++) _key[i] ^ m[i]]);
    return {
      if (encrypt) 'enc': 1,
      if (encrypt) 'k': base64.encode(masked),
      'certSha': certSha,
      if (blockCapture) 'secure': 1,
      if (blockRoot) 'noRoot': 1,
    };
  }

  /// Obfuscates an arbitrary string (the remote site URL for website-link builds) with the
  /// same per-app mask used for the key above, repeated to cover the whole string. Stored
  /// under "u" instead of a plain "remoteUrl" so the destination isn't sitting in the open
  /// in wzh_quick.json. Same caveat as the key mask: this slows down casual unzipping only --
  /// the template derives the same mask from its own package name, so it's never unbreakable.
  String maskUrl(String url) {
    final m = sha256.convert(utf8.encode('wzh:$_mask')).bytes;
    final bytes = utf8.encode(url);
    final masked = Uint8List.fromList([for (var i = 0; i < bytes.length; i++) bytes[i] ^ m[i % m.length]]);
    return base64.encode(masked);
  }

  /// Deterministic, opaque on-disk name for a site file -- same per-app mask as everywhere
  /// else, hashed together with the file's own path. No ".html"/".css"/".js" survives into
  /// the stored name, and no path->name table needs to be stored or shipped: the template
  /// recomputes the same hash from the request path and its own package name.
  String assetName(String path) {
    final h = sha256.convert(utf8.encode('wzh:map:$_mask:$path')).bytes;
    return h.take(16).map((b) => b.toRadixString(16).padLeft(2, '0')).join();
  }

  Uint8List encryptFile(String path, Uint8List data) {
    final r = math.Random.secure();
    final nonce = Uint8List.fromList(List.generate(12, (_) => r.nextInt(256)));
    final gcm = GCMBlockCipher(AESEngine())
      ..init(true, AEADParameters(KeyParameter(_key), 128, nonce, Uint8List.fromList(utf8.encode(path))));
    final out = gcm.process(data);
    return Uint8List.fromList([...nonce, ...out]);
  }
}
