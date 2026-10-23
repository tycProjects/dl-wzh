import 'dart:io';
import 'package:flutter/foundation.dart';
import 'package:flutter/material.dart' hide Text;
import 'package:flutter/services.dart';
import 'package:package_info_plus/package_info_plus.dart';
import 'anim.dart';
import 'theme.dart';
import 'tr.dart';

enum IntegrityVerdict {
  /// Protection is not switched on yet (no trusted signature added) or this is not an Android release build.
  off,
  /// The signature matches one of the trusted ones.
  ok,
  /// The signature could not be read. Never treated as a problem.
  unknown,
  /// The signature was read twice and is not one of the trusted ones: a modified / re-signed copy.
  tampered,
}

/// Checks that this app is signed with YOUR key, so a copy that somebody unpacked, changed and
/// signed again with their own key shows "This app isn't available right now" instead of running.
///
/// It is built to avoid false alarms:
///  * it stays OFF until you add at least one trusted signature below;
///  * it only ever blocks when the signature was READ SUCCESSFULLY, twice, and is not in the list;
///  * if the signature can't be read for any reason, the app simply runs;
///  * debug builds and non-Android platforms are never checked;
///  * you can trust several signatures (for example your release key and a test key).
///
/// How to switch it on: build and install the app, open  Menu > Signing  and copy the value under
/// "This app's signature", then paste it into [trustedSignatures] below. Do it once per signing key.
class AppIntegrity {
  /// Signatures of the builds that are allowed to run, as shown on the Signing page.
  /// Letters, case, colons and spaces don't matter.
  static const trustedSignatures = <String>[
    '59:EF:B4:7C:F4:F5:63:E7:AD:77:B0:4B:B6:49:33:D9:76:8F:02:F0:F5:56:03:93:14:6F:A2:50:1A:F9:2D:17',
  ];

  static String normalize(String s) => s.toLowerCase().replaceAll(RegExp(r'[^0-9a-f]'), '');

  /// Compares without stopping at the first difference.
  static bool _same(String a, String b) {
    if (a.length != b.length) return false;
    var d = 0;
    for (var i = 0; i < a.length; i++) {
      d |= a.codeUnitAt(i) ^ b.codeUnitAt(i);
    }
    return d == 0;
  }

  static bool isTrusted(String signature) {
    final s = normalize(signature);
    if (s.isEmpty) return false;
    var ok = false;
    for (final t in trustedSignatures) {
      if (_same(s, normalize(t))) ok = true;
    }
    return ok;
  }

  /// True when at least one trusted value has the same format (length) as [signature]. A SHA-256
  /// can't be compared with a SHA-1, so a different format never counts as a mismatch.
  static bool comparable(String signature) {
    final n = normalize(signature).length;
    return n > 0 && trustedSignatures.any((t) => normalize(t).length == n);
  }

  static bool get enabled => trustedSignatures.any((t) => normalize(t).isNotEmpty);

  /// The signing certificate of the installed app as Android reports it ('' when unavailable).
  static Future<String> currentSignature() async {
    try {
      return (await PackageInfo.fromPlatform()).buildSignature;
    } catch (_) {
      return '';
    }
  }

  static Future<IntegrityVerdict> verdict() async {
    if (!Platform.isAndroid || kDebugMode || !enabled) return IntegrityVerdict.off;
    for (var attempt = 0; attempt < 2; attempt++) {
      final sig = normalize(await currentSignature());
      if (sig.isEmpty) {
        if (attempt == 0) {
          await Future.delayed(const Duration(milliseconds: 350));
          continue;
        }
        return IntegrityVerdict.unknown; // can't tell: never accuse
      }
      if (!comparable(sig)) return IntegrityVerdict.unknown; // different format: can't judge, never accuse
      if (isTrusted(sig)) return IntegrityVerdict.ok;
      if (attempt == 0) {
        await Future.delayed(const Duration(milliseconds: 450)); // read it a second time before blocking
        continue;
      }
      return IntegrityVerdict.tampered;
    }
    return IntegrityVerdict.unknown;
  }
}

/// Full-screen block shown for a modified copy. No way past it with Back.
class TamperedScreen extends StatefulWidget {
  final Future<void> Function() onRetry;
  const TamperedScreen({super.key, required this.onRetry});
  @override
  State<TamperedScreen> createState() => _TamperedScreenState();
}

class _TamperedScreenState extends State<TamperedScreen> {
  bool busy = false;

  Future<void> _retry() async {
    setState(() => busy = true);
    await widget.onRetry();
    if (mounted) setState(() => busy = false);
  }

  @override
  Widget build(BuildContext context) => PopScope(
        canPop: false,
        child: Scaffold(
          backgroundColor: C.bg,
          body: SafeArea(
            child: Center(
              child: Padding(
                padding: const EdgeInsets.symmetric(horizontal: 34),
                child: Column(mainAxisSize: MainAxisSize.min, children: [
                  Container(
                    width: 76, height: 76,
                    decoration: BoxDecoration(color: const Color(0x33F0483E), borderRadius: BorderRadius.circular(26)),
                    child: const Icon(Icons.lock_outline_rounded, size: 34, color: C.red),
                  ),
                  const SizedBox(height: 22),
                  Text('This app isn\u2019t available right now',
                      textAlign: TextAlign.center, style: inter(26, w: FontWeight.w800, h: 1.2)),
                  const SizedBox(height: 12),
                  Text(
                    'This copy doesn\u2019t match the original WZH 2 APK, so it has been switched off. '
                    'It may have been changed or re-signed by someone else. Please install the original app.',
                    textAlign: TextAlign.center,
                    style: inter(16, c: C.muted, h: 1.4),
                  ),
                  const SizedBox(height: 26),
                  Pressable(
                    onTap: busy ? null : _retry,
                    child: Container(
                      height: 50, width: 190, alignment: Alignment.center,
                      decoration: BoxDecoration(color: const Color(0xFF2A2A2C), borderRadius: BorderRadius.circular(25)),
                      child: busy
                          ? const IosSpinner(size: 22)
                          : Text('Check again', style: inter(17, w: FontWeight.w700)),
                    ),
                  ),
                ]),
              ),
            ),
          ),
        ),
      );
}

/// On the Signing page, setup only: shows this app's own signature so it can be added to
/// [AppIntegrity.trustedSignatures]. It disappears completely once the protection is on and matches.
class AppSignatureCard extends StatefulWidget {
  const AppSignatureCard({super.key});
  @override
  State<AppSignatureCard> createState() => _AppSignatureCardState();
}

class _AppSignatureCardState extends State<AppSignatureCard> {
  late final Future<String> _sig = AppIntegrity.currentSignature();

  @override
  Widget build(BuildContext context) => FutureBuilder<String>(
        future: _sig,
        builder: (context, snap) {
          final sig = snap.data ?? '';
          // Once the protection is set up and this app matches, nothing about it is shown at all.
          if (snap.connectionState == ConnectionState.done && sig.isNotEmpty && AppIntegrity.enabled && AppIntegrity.comparable(sig) && AppIntegrity.isTrusted(sig)) {
            return const SizedBox.shrink();
          }
          final String status;
          Color col = C.muted;
          if (snap.connectionState != ConnectionState.done) {
            status = 'Reading\u2026';
          } else if (sig.isEmpty) {
            status = 'Couldn\u2019t read it on this device. The protection stays off here, so it can\u2019t block you by mistake.';
          } else if (!AppIntegrity.enabled) {
            status = 'Protection is off. Copy this value into trustedSignatures in lib/integrity.dart, then build again to switch it on.';
          } else if (!AppIntegrity.comparable(sig)) {
            status = 'The value above is in a different format from the trusted one, so the check can\u2019t compare them and stays off. Copy the value above into trustedSignatures in lib/integrity.dart.';
            col = const Color(0xFFF5A524);
          } else if (AppIntegrity.isTrusted(sig)) {
            status = 'Protection is on and this app matches.';
            col = C.teal;
          } else {
            status = 'Protection is on, but this app\u2019s signature is not in the trusted list.';
            col = const Color(0xFFF5A524);
          }
          return Padding(
            padding: const EdgeInsets.only(top: 26),
            child: Container(
            padding: const EdgeInsets.all(16),
            decoration: BoxDecoration(color: C.card, borderRadius: BorderRadius.circular(22)),
            child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
              Text('This app\u2019s signature', style: inter(17, w: FontWeight.w700)),
              const SizedBox(height: 8),
              if (sig.isNotEmpty)
                Row(children: [
                  Expanded(child: SelectableText(sig, style: inter(13, c: C.text, h: 1.35))),
                  const SizedBox(width: 8),
                  Pressable(
                    onTap: () => Clipboard.setData(ClipboardData(text: sig)),
                    child: const Padding(padding: EdgeInsets.all(6), child: Icon(Icons.copy_rounded, size: 20, color: C.teal)),
                  ),
                ]),
              if (sig.isNotEmpty) const SizedBox(height: 8),
              Text(status, style: inter(13.5, c: col, h: 1.35)),
            ]),
            ),
          );
        },
      );
}
