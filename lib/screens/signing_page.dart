import 'package:flutter/material.dart' hide Text;
import 'package:flutter/cupertino.dart' show CupertinoPageRoute;
import 'dart:typed_data';
import 'package:file_selector/file_selector.dart';
import '../anim.dart';
import '../banner.dart';
import '../api.dart';
import '../signing_key.dart';
import '../integrity.dart';
import '../theme.dart';
import '../widgets.dart';
import '../tr.dart';

/// "Your signing key": create, import, back up or remove the key used to sign release builds.
class SigningPage extends StatefulWidget {
  const SigningPage({super.key});
  static Route<void> route() => CupertinoPageRoute(builder: (_) => const SigningPage());
  @override
  State<SigningPage> createState() => _SigningPageState();
}

class _SigningPageState extends State<SigningPage> {
  final keys = SigningKeys.i;
  bool busy = false;

  Future<void> _run(Future<void> Function() job) async {
    if (busy) return;
    setState(() => busy = true);
    try {
      await job();
    } finally {
      if (mounted) setState(() => busy = false);
    }
  }

  void _problem(String title, String message) =>
      IosBanner.show(context, kind: BannerKind.err, title: title, msg: message, duration: const Duration(seconds: 6));

  Future<void> _create() => _run(() async {
        try {
          await keys.create();
          if (mounted) await _backupPrompt('Key created', 'It\u2019s ready for your next build.');
        } catch (_) {
          if (mounted) _problem('Couldn\u2019t create the key', 'Try again in a moment.');
        }
      });

  Future<void> _import() => _run(() async {
        final picked = await openFiles(acceptedTypeGroups: [
          const XTypeGroup(label: 'Signing key files'),
        ]);
        if (picked.isEmpty) return;
        try {
          final files = <({String name, Uint8List bytes})>[
            for (final f in picked) (name: f.name, bytes: await f.readAsBytes()),
          ];
          await keys.importFiles(files);
          if (mounted) await _backupPrompt('Key imported', 'It\u2019s ready for your next build.');
        } on FormatException catch (e) {
          if (mounted) _problem('Couldn\u2019t import that', e.message);
        } catch (_) {
          if (mounted) _problem('Couldn\u2019t import that', 'Pick the .pk8 key and its certificate together, or a backup .zip.');
        }
      });

  Future<void> _backup() => _run(() async {
        try {
          await keys.backup();
          if (mounted) {
            IosBanner.show(context, kind: BannerKind.ok, title: 'Backup saved', msg: 'Your key backup is in your Downloads. Keep it somewhere safe.', duration: const Duration(seconds: 6));
          }
        } catch (_) {
          if (mounted) _problem('Couldn\u2019t save the backup', 'Check that the phone has free space and try again.');
        }
      });

  Future<void> _remove() async {
    final ok = await showIosDialog<bool>(
      context,
      (ctx) => _Card(
        title: 'Remove this key?',
        message: 'If you have no backup, you can\u2019t update apps you signed with it. This can\u2019t be undone.',
        primary: 'Remove key',
        primaryColor: C.red,
        secondary: 'Cancel',
        onPrimary: () => Navigator.pop(ctx, true),
        onSecondary: () => Navigator.pop(ctx, false),
      ),
    );
    if (ok == true) await _run(keys.remove);
  }

  Future<void> _backupPrompt(String title, String message) async {
    final yes = await showIosDialog<bool>(
      context,
      (ctx) => _Card(
        title: title,
        message: '$message Download a backup now? If you clear the app\u2019s data without one, the key is gone for good.',
        primary: 'Download backup',
        secondary: 'Not now',
        onPrimary: () => Navigator.pop(ctx, true),
        onSecondary: () => Navigator.pop(ctx, false),
      ),
    );
    if (yes == true && mounted) await _backup();
  }

  Widget _row(IconData icon, String label, VoidCallback onTap, {Color color = C.teal, Color? text}) => Pressable(
        scale: .985,
        onTap: onTap,
        child: Padding(
          padding: const EdgeInsets.symmetric(horizontal: 20, vertical: 20),
          child: Row(children: [
            Icon(icon, size: 26, color: color),
            const SizedBox(width: 18),
            Expanded(child: Text(label, style: inter(20, c: text ?? C.text))),
            const Icon(Icons.arrow_forward_ios_rounded, size: 15, color: Color(0xFF6A6A6E)),
          ]),
        ),
      );

  Widget get _line => Container(height: .5, margin: const EdgeInsets.only(left: 64), color: const Color(0xFF3A3A3C));

  @override
  Widget build(BuildContext context) => Scaffold(
        body: SafeArea(
          child: Column(children: [
            const TopBar(title: 'Signing', back: true),
            Expanded(
              child: ValueListenableBuilder<SigningKeyInfo?>(
                valueListenable: keys.current,
                builder: (_, key, __) => ListView(
                  physics: const BouncingScrollPhysics(parent: AlwaysScrollableScrollPhysics()),
                  padding: const EdgeInsets.fromLTRB(32, 28, 32, 40),
                  children: [
                    if (busy) const Padding(padding: EdgeInsets.only(bottom: 22), child: Center(child: IosSpinner(size: 30))),
                    Text('Your signing key', textAlign: TextAlign.center, style: inter(30, w: FontWeight.w800)),
                    const SizedBox(height: 10),
                    Text(
                      key == null
                          ? 'No key of your own yet. Builds use a key we make for you.'
                          : 'Using your key (${key.shortFingerprint}). Every signed build uses it. It is stored on this device only, so back it up.',
                      textAlign: TextAlign.center,
                      style: inter(17, c: C.muted, h: 1.4),
                    ),
                    const SizedBox(height: 26),
                    Opacity(
                      opacity: busy ? .5 : 1,
                      child: Container(
                        decoration: BoxDecoration(color: C.card, borderRadius: BorderRadius.circular(22)),
                        clipBehavior: Clip.antiAlias,
                        child: Column(children: [
                          _row(Icons.add_rounded, key == null ? 'Create key' : 'New key', _create),
                          _line,
                          _row(Icons.file_download_outlined, 'Import', _import),
                          if (key != null) ...[
                            _line,
                            _row(Icons.file_upload_outlined, 'Back up', _backup),
                            _line,
                            _row(Icons.delete_outline_rounded, 'Remove', _remove, color: C.red, text: C.red),
                          ],
                        ]),
                      ),
                    ),
                    const SizedBox(height: 18),
                    Text('Your signing key is made once and reused. Keep it safe.',
                        textAlign: TextAlign.center, style: inter(16, c: C.muted, h: 1.4)),
                    const AppSignatureCard(),
                    if (key != null && !Api.i.customSigningOk) ...[
                      const SizedBox(height: 14),
                      Text('The build server can\u2019t use your own key right now, so builds use the default key until it can.',
                          textAlign: TextAlign.center, style: inter(14, c: C.red, h: 1.35)),
                    ],
                  ],
                ),
              ),
            ),
          ]),
        ),
      );
}

/// The rounded confirmation card (Key created / Remove this key?).
class _Card extends StatelessWidget {
  final String title, message, primary, secondary;
  final Color primaryColor;
  final VoidCallback onPrimary, onSecondary;
  const _Card({
    required this.title, required this.message, required this.primary, required this.secondary,
    required this.onPrimary, required this.onSecondary, this.primaryColor = C.tealBtn,
  });
  @override
  Widget build(BuildContext context) => Container(
        width: MediaQuery.of(context).size.width - 48,
        padding: const EdgeInsets.fromLTRB(24, 24, 24, 12),
        decoration: BoxDecoration(color: const Color(0xFF1E1E20), borderRadius: BorderRadius.circular(30)),
        child: Column(mainAxisSize: MainAxisSize.min, crossAxisAlignment: CrossAxisAlignment.start, children: [
          Text(title, style: inter(24, w: FontWeight.w800)),
          const SizedBox(height: 12),
          Text(message, style: inter(18, c: const Color(0xFFD8D8DC), h: 1.35)),
          const SizedBox(height: 22),
          Pressable(
            onTap: onPrimary,
            child: Container(
              height: 56, alignment: Alignment.center,
              decoration: BoxDecoration(color: primaryColor, borderRadius: BorderRadius.circular(28)),
              child: Text(primary, style: inter(19, w: FontWeight.w700, c: primaryColor == C.red ? Colors.white : C.onTeal)),
            ),
          ),
          Pressable(
            onTap: onSecondary,
            child: Container(
              height: 56, alignment: Alignment.center,
              child: Text(secondary, style: inter(19, w: FontWeight.w500)),
            ),
          ),
        ]),
      );
}
