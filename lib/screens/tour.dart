import 'package:flutter/material.dart' hide Text;
import '../anim.dart';
import '../api.dart';
import '../theme.dart';
import '../tr.dart';

const _kTourKey = 'wzhTourSeen';

bool tourSeen() => Api.i.prefs.getString(_kTourKey) == '1';

/// The 3-slide intro. Shown once on first launch and from Menu > Take the tour.
Future<void> showTour(BuildContext context) {
  Api.i.prefs.setString(_kTourKey, '1');
  return Navigator.of(context, rootNavigator: true).push(PageRouteBuilder<void>(
    opaque: true,
    transitionDuration: const Duration(milliseconds: 340),
    reverseTransitionDuration: const Duration(milliseconds: 240),
    pageBuilder: (c, a, s) => const _Tour(),
    transitionsBuilder: (c, a, s, child) => FadeTransition(opacity: CurvedAnimation(parent: a, curve: Curves.easeOut), child: child),
  ));
}

class _Tour extends StatefulWidget {
  const _Tour();
  @override
  State<_Tour> createState() => _TourState();
}

class _TourState extends State<_Tour> {
  static const _slides = [
    (Icons.file_upload_outlined, 'Add your app', 'Choose an HTML file or a ZIP project, or paste a website link. Pick what you have.'),
    (Icons.verified_user_outlined, 'We check it first', 'Before you build, a quick check spots common problems, like a missing file or a wrong folder, so you don\u2019t waste a build.'),
    (Icons.download_rounded, 'Build and download', 'Tap Build, watch the progress, then download your APK. Finished builds stay in Recent builds.'),
  ];
  final _pc = PageController();
  int _cur = 0;

  @override
  void dispose() {
    _pc.dispose();
    super.dispose();
  }

  void _next() {
    if (_cur == _slides.length - 1) {
      Navigator.of(context).pop();
    } else {
      _pc.nextPage(duration: const Duration(milliseconds: 340), curve: iosCurve);
    }
  }

  @override
  Widget build(BuildContext context) {
    final last = _cur == _slides.length - 1;
    return Scaffold(
      backgroundColor: C.bg,
      body: SafeArea(
        child: Column(children: [
          Align(
            alignment: Alignment.centerRight,
            child: Padding(
              padding: const EdgeInsets.fromLTRB(8, 6, 14, 0),
              child: AnimatedOpacity(
                opacity: last ? 0 : 1,
                duration: const Duration(milliseconds: 160),
                child: Pressable(
                  onTap: last ? null : () => Navigator.of(context).pop(),
                  child: Padding(padding: const EdgeInsets.all(10), child: Text('Skip', style: inter(17, c: C.muted))),
                ),
              ),
            ),
          ),
          Expanded(
            child: PageView.builder(
              controller: _pc,
              itemCount: _slides.length,
              physics: const BouncingScrollPhysics(),
              onPageChanged: (i) => setState(() => _cur = i),
              itemBuilder: (c, i) => Padding(
                padding: const EdgeInsets.symmetric(horizontal: 34),
                child: Column(mainAxisAlignment: MainAxisAlignment.center, children: [
                  Container(
                    width: 104, height: 104,
                    decoration: BoxDecoration(color: C.tealDeep, borderRadius: BorderRadius.circular(34)),
                    child: Icon(_slides[i].$1, size: 50, color: C.teal),
                  ),
                  const SizedBox(height: 34),
                  Text(_slides[i].$2, textAlign: TextAlign.center, style: inter(28, w: FontWeight.w700, h: 1.15)),
                  const SizedBox(height: 14),
                  Text(_slides[i].$3, textAlign: TextAlign.center, style: inter(17, c: C.muted, h: 1.45)),
                ]),
              ),
            ),
          ),
          Row(mainAxisAlignment: MainAxisAlignment.center, children: [
            for (var i = 0; i < _slides.length; i++)
              Pressable(
                scale: .8,
                onTap: () => _pc.animateToPage(i, duration: const Duration(milliseconds: 300), curve: iosCurve),
                child: AnimatedContainer(
                  duration: const Duration(milliseconds: 210),
                  curve: iosCurve,
                  margin: const EdgeInsets.symmetric(horizontal: 4),
                  width: i == _cur ? 22 : 8, height: 8,
                  decoration: BoxDecoration(color: i == _cur ? C.teal : const Color(0xFF3A3A3C), borderRadius: BorderRadius.circular(4)),
                ),
              ),
          ]),
          Padding(
            padding: const EdgeInsets.fromLTRB(20, 26, 20, 24),
            child: Pressable(
              onTap: _next,
              child: Container(
                height: 56, alignment: Alignment.center,
                decoration: BoxDecoration(color: C.tealBtn, borderRadius: BorderRadius.circular(28)),
                child: Text(last ? 'Get started' : 'Continue', style: inter(18, w: FontWeight.w700, c: const Color(0xFF0F2A24))),
              ),
            ),
          ),
        ]),
      ),
    );
  }
}
