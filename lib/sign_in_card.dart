import 'dart:convert';
import 'anim.dart';
import 'dart:math' as math;
import 'package:flutter/cupertino.dart' show CupertinoPageRoute;
import 'package:flutter/gestures.dart';
import 'package:flutter/material.dart' hide Text;
import 'api.dart';
import 'screens/info_pages.dart';
import 'theme.dart';
import 'widgets.dart';
import 'tr.dart';

/// Bump this whenever the Terms or Privacy Policy change in a way that needs fresh agreement.
const kLegalVersion = '2026-10-04';

/// White "Log in to unlock the best of <app>" card with a "Continue with Google" pill button.
/// Returns true when the person taps the button, false when they tap outside to close it.
Future<bool> showSignInCard(BuildContext context, {String appName = 'WZH 2 APK'}) async {
  final r = await showGeneralDialog<bool>(
    context: context,
    barrierDismissible: true,
    barrierLabel: 'Close',
    barrierColor: const Color(0x9E000000),
    transitionDuration: glassSpring.duration,
    pageBuilder: (_, __, ___) => _SignInCard(appName: appName),
    transitionBuilder: glassTransition(),
  );
  if (r == true) {
    // Keep proof of when the person agreed, and to which version of the legal pages.
    try {
      await Api.i.prefs.setString('legalConsent', jsonEncode({'v': kLegalVersion, 'at': DateTime.now().toUtc().toIso8601String()}));
    } catch (_) {}
  }
  return r == true;
}

/// Same dark card, for when sign-in fails. Returns true when the person taps "Try again".
Future<bool> showSignInError(BuildContext context, String message) async {
  final r = await showGeneralDialog<bool>(
    context: context,
    barrierDismissible: true,
    barrierLabel: 'Close',
    barrierColor: const Color(0x9E000000),
    transitionDuration: glassSpring.duration,
    pageBuilder: (_, __, ___) => _SignInError(message: message),
    transitionBuilder: glassTransition(),
  );
  return r == true;
}

class _SignInError extends StatelessWidget {
  final String message;
  const _SignInError({required this.message});

  @override
  Widget build(BuildContext context) {
    return Center(
      child: Material(
        color: Colors.transparent,
        child: DialogCard(title: 'Couldn’t sign in', message: message, actions: [
          DialogAction('Close', color: const Color(0xFFB4B4B8), onTap: () => Navigator.of(context).pop(false)),
          DialogAction('Try again', onTap: () => Navigator.of(context).pop(true)),
        ]),
      ),
    );
  }
}

class _SignInCard extends StatefulWidget {
  final String appName;
  const _SignInCard({required this.appName});

  @override
  State<_SignInCard> createState() => _SignInCardState();
}

class _SignInCardState extends State<_SignInCard> {
  bool _agreed = false;
  late final TapGestureRecognizer _tapTerms = TapGestureRecognizer()..onTap = () => _go(Pages.terms);
  late final TapGestureRecognizer _tapPrivacy = TapGestureRecognizer()..onTap = () => _go(Pages.privacy);

  void _go(Widget page) => Navigator.of(context).push(CupertinoPageRoute(builder: (_) => page));

  @override
  void dispose() {
    _tapTerms.dispose();
    _tapPrivacy.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final appName = widget.appName;
    const ink = C.text;
    const grey = C.muted;
    return Center(
      child: Material(
        color: Colors.transparent,
        child: Container(
          width: math.min(MediaQuery.of(context).size.width - 48, 360),
          padding: const EdgeInsets.fromLTRB(24, 24, 24, 20),
          decoration: BoxDecoration(
            color: C.sheet,
            borderRadius: BorderRadius.circular(24),
            boxShadow: const [BoxShadow(color: Color(0x66000000), blurRadius: 24, offset: Offset(0, 8))],
          ),
          child: Column(
            mainAxisSize: MainAxisSize.min,
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Container(
                width: 38,
                height: 38,
                decoration: BoxDecoration(color: C.tealBtn, shape: BoxShape.circle),
                child: Icon(Icons.storefront_rounded, color: C.onTeal, size: 22),
              ),
              const SizedBox(height: 14),
              Text('Log in to publish your app.', verbatim: true, style: inter(21, w: FontWeight.w700, c: ink, h: 1.25)),
              const SizedBox(height: 22),
              GestureDetector(
                onTap: _agreed ? () => Navigator.of(context).pop(true) : null,
                child: Opacity(
                  opacity: _agreed ? 1 : .4,
                  child: Container(
                  height: 48,
                  decoration: BoxDecoration(
                    color: C.field,
                    borderRadius: BorderRadius.circular(24),
                    border: Border.all(color: const Color(0xFF4A4A4E), width: 1),
                  ),
                  child: Row(
                    children: [
                      const SizedBox(width: 16),
                      const SizedBox(width: 22, height: 22, child: CustomPaint(painter: _GPainter())),
                      Expanded(child: Center(child: Text('Continue with Google', verbatim: true, style: inter(15, w: FontWeight.w600, c: C.text)))),
                      const SizedBox(width: 38),
                    ],
                  ),
                ),
                ),
              ),
              const SizedBox(height: 18),
              GestureDetector(
                behavior: HitTestBehavior.opaque,
                onTap: () => setState(() => _agreed = !_agreed),
                child: Row(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    AnimatedContainer(
                      duration: const Duration(milliseconds: 120),
                      width: 22,
                      height: 22,
                      margin: const EdgeInsets.only(top: 1),
                      decoration: BoxDecoration(
                        color: _agreed ? C.tealBtn : Colors.transparent,
                        borderRadius: BorderRadius.circular(6),
                        border: Border.all(color: _agreed ? C.tealBtn : const Color(0xFF6A6A70), width: 1.5),
                      ),
                      child: _agreed ? Icon(Icons.check_rounded, size: 16, color: C.onTeal) : null,
                    ),
                    const SizedBox(width: 10),
                    Expanded(
                      child: RichText(
                        text: TextSpan(
                          style: inter(12, c: grey, h: 1.4),
                          children: [
                            const TextSpan(text: 'I have read and agree to the '),
                            TextSpan(text: 'Terms of Service', recognizer: _tapTerms, style: inter(12, c: C.tealBtn, h: 1.4).copyWith(decoration: TextDecoration.underline)),
                            const TextSpan(text: ' and '),
                            TextSpan(text: 'Privacy Policy', recognizer: _tapPrivacy, style: inter(12, c: C.tealBtn, h: 1.4).copyWith(decoration: TextDecoration.underline)),
                            const TextSpan(text: '.'),
                          ],
                        ),
                      ),
                    ),
                  ],
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}

/// The multicolour Google "G".
class _GPainter extends CustomPainter {
  const _GPainter();
  @override
  void paint(Canvas canvas, Size s) {
    final c = Offset(s.width / 2, s.height / 2);
    final sw = s.width * .2;
    final rect = Rect.fromCircle(center: c, radius: s.width / 2 - sw / 2);
    double rad(double d) => d * math.pi / 180;
    Paint p(Color col) => Paint()..color = col..style = PaintingStyle.stroke..strokeWidth = sw;
    canvas.drawArc(rect, rad(225), rad(95), false, p(const Color(0xFFEA4335)));
    canvas.drawArc(rect, rad(135), rad(92), false, p(const Color(0xFFFBBC05)));
    canvas.drawArc(rect, rad(45), rad(92), false, p(const Color(0xFF34A853)));
    canvas.drawArc(rect, rad(0), rad(47), false, p(const Color(0xFF4285F4)));
    final bar = Paint()..color = const Color(0xFF4285F4)..style = PaintingStyle.fill;
    canvas.drawRect(Rect.fromLTRB(c.dx, c.dy - sw / 2, s.width - 0.5, c.dy + sw / 2), bar);
  }

  @override
  bool shouldRepaint(covariant CustomPainter old) => false;
}
