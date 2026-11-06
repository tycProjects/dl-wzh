import 'dart:async';
import 'package:flutter/material.dart' hide Text;
import 'package:flutter/services.dart';
import 'package:flutter/foundation.dart' show ValueListenable;
import 'package:flutter/cupertino.dart' show CupertinoPageRoute;
import 'api.dart';
import 'config.dart';
import 'integrity.dart';
import 'quick/quick_builder.dart';
import 'notices.dart';
import 'notify.dart';
import 'push.dart';
import 'store.dart';
import 'theme.dart';
import 'screens/build_page.dart';
import 'screens/share_page.dart';
import 'screens/store_page.dart';
import 'widgets.dart';
import 'anim.dart';
import 'intro.dart';
import 'screens/home.dart';
import 'screens/host_page.dart';
import 'screens/code_home.dart';
import 'screens/create_sheet.dart';
import 'screens/menu_drawer.dart';
import 'screens/message_page.dart';
import 'screens/updates_page.dart';
import 'screens/tour.dart';
import 'tr.dart';
import 'appearance.dart';

void main() {
  // Draw something immediately so the app can never sit on the native splash forever.
  runZonedGuarded(() {
    WidgetsFlutterBinding.ensureInitialized();
    FlutterError.onError = (d) => FlutterError.presentError(d);
    SystemChrome.setSystemUIOverlayStyle(const SystemUiOverlayStyle(
      statusBarColor: Colors.transparent,
      statusBarIconBrightness: Brightness.light,
      systemNavigationBarColor: Color(0xFF111111),
      systemNavigationBarIconBrightness: Brightness.light,
    ));
    runApp(const WzhApp());
  }, (e, st) => debugPrint('Uncaught: $e\n$st'));
}

/// Runs Api.init() AFTER the first frame, with a timeout, and shows any error on screen.
class Boot extends StatefulWidget {
  const Boot({super.key});
  @override
  State<Boot> createState() => _BootState();
}

class _BootState extends State<Boot> with WidgetsBindingObserver {
  bool ready = false, held = false, introDone = false, tampered = false;
  String? error;

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addObserver(this);
    _start();
  }

  @override
  void dispose() {
    WidgetsBinding.instance.removeObserver(this);
    super.dispose();
  }

  @override
  void didChangeAppLifecycleState(AppLifecycleState state) {
    if (state == AppLifecycleState.resumed) {
      _verify();
      ChatNotifier.i.poke();
    }
  }

  /// Blocks the app only when the signature was read and is not one of the trusted ones.
  Future<void> _verify() async {
    final v = await AppIntegrity.verdict();
    if (mounted && tampered != (v == IntegrityVerdict.tampered)) setState(() => tampered = v == IntegrityVerdict.tampered);
  }

  Future<void> _start() async {
    setState(() => error = null);
    try {
      // Don't wait for the server check: open on the first server now and pick the best one in the
      // background. If another server is chosen, the session token is renewed from it.
      final firstBase = kApiBase;
      pickApiBase().then((_) {
        if (kApiBase != firstBase) Api.i.token(refresh: true).then((_) {}, onError: (_) {});
      }, onError: (_) {});
      await Api.i.init().timeout(const Duration(seconds: 10));
      Appearance.i.load();
      Lang.i.load();
      ChatNotifier.i.start();
      ChatPush.i.start();
      if (mounted) setState(() => ready = true);
      _verify();
    } catch (e, st) {
      if (mounted) setState(() => error = 'Couldn\u2019t start the app. Check your connection and try again.');
    }
  }

  @override
  Widget build(BuildContext context) {
    if (tampered) return TamperedScreen(onRetry: _verify);
    if (error != null) return _errorView();
    // The home screen is only built once the intro's animation is over and the app has loaded,
    // so building it never competes with the animation. The intro then fades away to reveal it.
    final shellUp = ready && held;
    return Stack(fit: StackFit.expand, children: [
      if (shellUp) const Shell(key: ValueKey('shell')),
      if (!introDone)
        IntroSplash(
          key: const ValueKey('intro'),
          reveal: shellUp,
          onHold: () { if (mounted) setState(() => held = true); },
          onDone: () {
            if (!mounted) return;
            setState(() => introDone = true);
            // Quiet background work waits until the intro is over so it can't cause a hitch.
            Future.delayed(const Duration(seconds: 2), () => QuickBuild.prefetch());
          },
        ),
    ]);
  }

  Widget _errorView() => Scaffold(
        backgroundColor: C.bg,
        body: Center(
          child: Padding(
            padding: const EdgeInsets.all(24),
            child: Column(mainAxisSize: MainAxisSize.min, children: [
              Text('Startup problem', style: inter(20, w: FontWeight.w700)),
              const SizedBox(height: 12),
              Flexible(child: SingleChildScrollView(child: Text(error!, style: inter(12, c: C.muted)))),
              const SizedBox(height: 16),
              Pressable(
                onTap: _start,
                child: Container(
                  height: 48, width: 160, alignment: Alignment.center,
                  decoration: BoxDecoration(color: C.tealBtn, borderRadius: BorderRadius.circular(24)),
                  child: Text('Try again', style: inter(17, w: FontWeight.w700, c: const Color(0xFF252A27))),
                ),
              ),
            ]),
          ),
        ),
      );
}

class WzhApp extends StatelessWidget {
  const WzhApp({super.key});
  @override
  Widget build(BuildContext context) => LangScope(
        notifier: Lang.i.tick,
        child: MaterialApp(
        debugShowCheckedModeBanner: false,
        title: 'WZH 2 APK',
        theme: buildTheme(),
        scrollBehavior: const _NoGlow(),
        builder: (c, w) => LightLayer(
          child: MediaQuery(
            data: MediaQuery.of(c).copyWith(textScaler: TextScaler.noScaling),
            child: _KeyboardGuard(child: w!),
          ),
        ),
        home: const Boot(),
      ),
      );
}

/// Fixes text fields that stay highlighted (teal ring, cursor, selection handles) after the
/// keyboard is closed: when the keyboard goes away the field is unfocused, and tapping empty
/// space unfocuses it too.
class _KeyboardGuard extends StatefulWidget {
  final Widget child;
  const _KeyboardGuard({required this.child});
  @override
  State<_KeyboardGuard> createState() => _KeyboardGuardState();
}

class _KeyboardGuardState extends State<_KeyboardGuard> with WidgetsBindingObserver {
  double _last = 0;

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addObserver(this);
  }

  @override
  void dispose() {
    WidgetsBinding.instance.removeObserver(this);
    super.dispose();
  }

  @override
  void didChangeMetrics() {
    final views = WidgetsBinding.instance.platformDispatcher.views;
    if (views.isEmpty) return;
    final v = views.first.viewInsets.bottom;
    if (_last > 0 && v == 0) FocusManager.instance.primaryFocus?.unfocus();
    _last = v;
  }

  @override
  Widget build(BuildContext context) => GestureDetector(
        behavior: HitTestBehavior.translucent,
        onTap: () => FocusManager.instance.primaryFocus?.unfocus(),
        child: widget.child,
      );
}

class _NoGlow extends MaterialScrollBehavior {
  const _NoGlow();
  @override
  Widget buildOverscrollIndicator(BuildContext context, Widget child, ScrollableDetails details) => child;
  @override
  ScrollPhysics getScrollPhysics(BuildContext context) => const IosScrollPhysics(parent: AlwaysScrollableScrollPhysics());
}

class Shell extends StatefulWidget {
  const Shell({super.key});
  @override
  State<Shell> createState() => _ShellState();
}

class _ShellState extends State<Shell> with WidgetsBindingObserver {
  bool _admin = false; // the floating Admin message card
  bool _holdResize = false; // keeps the screen behind the Admin chat from resizing with the keyboard
  int tab = ShellNav.tab.value; // (Message is now a floating card) 0 Message, 1 Home, 2 Host, 4 Code (3 Updates opens from the menu)

  void _sync() {
    if (mounted && tab != ShellNav.tab.value) setState(() => tab = ShellNav.tab.value);
  }

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addObserver(this);
    ShellNav.tab.addListener(_sync);
    Store.i.refreshLimit();
    Store.i.refreshHistory();
    // A build that was still running when the app was closed: go straight back to it.
    WidgetsBinding.instance.addPostFrameCallback((_) {
      final id = Store.i.activeJobId;
      if (id != null && mounted) {
        Navigator.of(context).push(CupertinoPageRoute(builder: (_) => BuildPage(jobId: id, appName: 'Your app')));
      } else if (!tourSeen() && mounted) {
        showTour(context);
      }
      // The same announcement popups the HTML app shows (What's new + new APK version).
      Future.delayed(const Duration(milliseconds: 720), () {
        if (mounted) Notices.check(context, force: true);
      });
    });
  }

  @override
  void dispose() {
    ShellNav.tab.removeListener(_sync);
    WidgetsBinding.instance.removeObserver(this);
    super.dispose();
  }

  @override
  void didChangeAppLifecycleState(AppLifecycleState state) {
    if (state == AppLifecycleState.resumed) {
      Store.i.refreshHistory();
      Store.i.refreshLimit();
      if (mounted) Notices.check(context);
    }
  }

  @override
  Widget build(BuildContext context) {
    final kb = MediaQuery.of(context).viewInsets.bottom;
    // While the Admin chat is open the screen behind it stays still (it must not resize with the
    // keyboard); the chat itself rides on top of the keyboard. Resizing resumes once the keyboard is gone.
    if (_admin) {
      _holdResize = true;
    } else if (kb == 0) {
      _holdResize = false;
    }
    return PopScope(
      canPop: !_admin,
      onPopInvokedWithResult: (didPop, _) {
        if (!didPop && _admin) setState(() => _admin = false);
      },
      child: Stack(fit: StackFit.expand, children: [
      Scaffold(
      resizeToAvoidBottomInset: !_holdResize,
      body: SafeArea(
        bottom: false,
        child: Stack(children: [
          Column(children: [
            TopBar(onMenu: () => showIosDrawer(context, (_) => const MenuDrawer())),
            Expanded(
              // All tabs stay alive (state is kept); the shown one fades and eases in.
              child: Stack(fit: StackFit.expand, children: [
                _TabPane(visible: tab == 1, child: const HomeScreen()),
                _TabPane(visible: tab == 2, child: HostPage(embedded: true, visible: tab == 2)),
                _TabPane(visible: tab == ShellNav.share, child: SharePage(embedded: true, visible: tab == ShellNav.share)),
                _TabPane(visible: tab == ShellNav.store, child: StorePage(embedded: true, visible: tab == ShellNav.store)),
                _TabPane(visible: tab == ShellNav.code, child: const CodeHome()),
              ]),
            ),
          ]),
          Positioned(
            right: 17,
            bottom: 12,
            child: ValueListenableBuilder<bool>(
              valueListenable: Store.i.botHidden,
              builder: (_, hidden, __) {
                final show = !hidden && tab == 1 && !_admin;
                return IgnorePointer(
                  ignoring: !show,
                  child: AnimatedScale(
                    scale: show ? 1 : 0,
                    duration: const Duration(milliseconds: 300),
                    curve: iosCurve,
                    child: AnimatedOpacity(
                      opacity: show ? 1 : 0,
                      duration: const Duration(milliseconds: 180),
                      child: Stack(clipBehavior: Clip.none, children: [
                        ChatFab(
                          pulse: Api.i.prefs.getString('chatFabSeen') != '1',
                          onTap: () {
                            Api.i.prefs.setString('chatFabSeen', '1');
                            setState(() => _admin = true);
                          },
                        ),
                        Positioned(
                          right: -2,
                          top: -2,
                          child: ValueListenableBuilder<int>(
                            valueListenable: ChatNotifier.unread,
                            builder: (_, n, __) => n > 0
                                ? IgnorePointer(
                                    child: Container(
                                      constraints: const BoxConstraints(minWidth: 20, minHeight: 20),
                                      padding: const EdgeInsets.symmetric(horizontal: 5),
                                      alignment: Alignment.center,
                                      decoration: BoxDecoration(
                                        color: const Color(0xFFE8202E),
                                        borderRadius: BorderRadius.circular(10),
                                        border: Border.all(color: const Color(0xFF0A0A0A), width: 2),
                                      ),
                                      child: Text(n > 9 ? '9+' : '$n', style: inter(11, w: FontWeight.w800, c: Colors.white)),
                                    ),
                                  )
                                : const SizedBox.shrink(),
                          ),
                        ),
                      ]),
                    ),
                  ),
                );
              },
            ),
          ),
        ]),
      ),
      bottomNavigationBar: (kb > 0 && !_holdResize)
          ? const SizedBox.shrink()
          : _Nav(index: tab, onTap: ShellNav.go),
    ),
      Positioned.fill(child: _adminOverlay(context)),
    ]));
  }

  /// The Admin chat: a tall sheet that rides on top of the keyboard, like a messaging app.
  Widget _adminOverlay(BuildContext context) {
    final mq = MediaQuery.of(context);
    final kb = mq.viewInsets.bottom;
    final safeBottom = kb > 0 ? 0.0 : mq.padding.bottom;
    final avail = (mq.size.height - kb - mq.padding.top - 12).clamp(140.0, double.infinity).toDouble();
    return IgnorePointer(
      ignoring: !_admin,
      child: AnimatedOpacity(
        opacity: _admin ? 1 : 0,
        duration: Duration(milliseconds: _admin ? 220 : 160),
        curve: Curves.easeOut,
        child: Material(
          type: MaterialType.transparency,
          child: Stack(children: [
            Positioned.fill(
              child: GestureDetector(
                behavior: HitTestBehavior.opaque,
                onTap: () {
                  FocusManager.instance.primaryFocus?.unfocus();
                  setState(() => _admin = false);
                },
                child: const ColoredBox(color: Color(0x80000000)),
              ),
            ),
            Positioned(
              left: 0,
              right: 0,
              bottom: kb,
              height: avail,
              child: AnimatedSlide(
                offset: _admin ? Offset.zero : const Offset(0, .05),
                duration: const Duration(milliseconds: 300),
                curve: iosCurve,
                child: Container(
                  padding: EdgeInsets.only(bottom: safeBottom),
                  decoration: BoxDecoration(
                    color: const Color(0xFF151516),
                    borderRadius: const BorderRadius.vertical(top: Radius.circular(28)),
                    border: Border.all(color: C.line),
                    boxShadow: const [BoxShadow(color: Color(0x99000000), blurRadius: 30, offset: Offset(0, -6))],
                  ),
                  clipBehavior: Clip.antiAlias,
                  child: TickerMode(
                    enabled: _admin,
                    child: MessagePage(open: _admin, onClose: () {
                      FocusManager.instance.primaryFocus?.unfocus();
                      setState(() => _admin = false);
                    }),
                  ),
                ),
              ),
            ),
          ]),
        ),
      ),
    );
  }
}

/// Bottom bar: a floating rounded bar with a raised round Store button in the middle.
/// Order: Build, Code, [Store], Host, Share. The active tab is tinted teal with a short underline.
class _Nav extends StatelessWidget {
  final int index;
  final ValueChanged<int> onTap;
  const _Nav({required this.index, required this.onTap});

  static const _idle = Color(0xFF8E8E93);
  static const double _pad = 24, _bar = 66, _fab = 52;

  Widget _item(int i, IconData icon, String label, {ValueListenable<bool>? badge}) {
    final sel = i == index;
    return Expanded(
      child: Semantics(
        button: true,
        selected: sel,
        label: label,
        child: Pressable(
          scale: .9,
          onTap: () => onTap(i),
          child: Container(
            color: Colors.transparent,
            alignment: Alignment.center,
            child: Column(mainAxisSize: MainAxisSize.min, children: [
              SizedBox(
                width: 44,
                height: 30,
                child: Stack(clipBehavior: Clip.none, alignment: Alignment.center, children: [
                  AnimatedContainer(
                    duration: const Duration(milliseconds: 240),
                    curve: iosCurve,
                    width: sel ? 38 : 0,
                    height: sel ? 38 : 0,
                    decoration: const BoxDecoration(shape: BoxShape.circle),
                  ),
                  Icon(icon, size: 26, color: sel ? Colors.white : _idle),
                  if (badge != null)
                    Positioned(
                      top: -2,
                      right: -4,
                      child: ValueListenableBuilder<bool>(
                        valueListenable: badge,
                        builder: (_, show, __) => show
                            ? Container(
                                padding: const EdgeInsets.symmetric(horizontal: 4.5, vertical: 1.5),
                                decoration: BoxDecoration(color: const Color(0xFFE8202E), borderRadius: BorderRadius.circular(6)),
                                child: Text('NEW', style: inter(9, w: FontWeight.w800, c: Colors.white).copyWith(height: 1.2, letterSpacing: .2)),
                              )
                            : const SizedBox.shrink(),
                      ),
                    ),
                ]),
              ),
              Text(label, maxLines: 1, style: inter(11, w: sel ? FontWeight.w600 : FontWeight.w500, c: sel ? Colors.white : _idle)),
              const SizedBox(height: 2),
              AnimatedContainer(
                duration: const Duration(milliseconds: 240),
                curve: iosCurve,
                width: sel ? 18 : 0,
                height: 3,
                decoration: BoxDecoration(color: Colors.white, borderRadius: BorderRadius.circular(3)),
              ),
            ]),
          ),
        ),
      ),
    );
  }

  Widget _center() {
    final sel = index == ShellNav.store;
    return Semantics(
      button: true,
      selected: sel,
      label: 'Store',
      child: Pressable(
        scale: .92,
        onTap: () => onTap(ShellNav.store),
        child: AnimatedContainer(
          duration: const Duration(milliseconds: 240),
          curve: iosCurve,
          width: _fab,
          height: _fab,
          alignment: Alignment.center,
          decoration: BoxDecoration(
            shape: BoxShape.circle,
            color: C.teal,
            border: Border.all(color: Colors.white.withAlpha(sel ? 110 : 0), width: 2),
          ),
          child: const Icon(Icons.storefront_outlined, size: 26, color: C.onTeal),
        ),
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    final safe = MediaQuery.of(context).padding.bottom;
    return Container(
      color: C.bg,
      padding: EdgeInsets.fromLTRB(14, 0, 14, safe + 8),
      child: SizedBox(
        height: _pad + _bar,
        child: Stack(children: [
          Positioned.fill(child: CustomPaint(painter: _BarPainter())),
          Positioned(
            left: 0,
            right: 0,
            top: _pad,
            height: _bar,
            child: Row(children: [
              _item(ShellNav.home, Icons.hardware_outlined, 'Build'),
              _item(ShellNav.code, Icons.code_rounded, 'Code', badge: ShellNav.codeNew),
              const SizedBox(width: 84),
              _item(ShellNav.host, Icons.cloud_upload_outlined, 'Host'),
              _item(ShellNav.share, Icons.share_outlined, 'Share'),
            ]),
          ),
          Positioned(top: 2, left: 0, right: 0, child: Center(child: _center())),
          Positioned(
            top: _pad + _bar - 20,
            left: 0,
            right: 0,
            child: IgnorePointer(
              child: Center(child: Text('Store', style: inter(11, w: index == ShellNav.store ? FontWeight.w600 : FontWeight.w500, c: index == ShellNav.store ? Colors.white : _idle))),
            ),
          ),
        ]),
      ),
    );
  }
}

/// The bar's outline: rounded rectangle with a smooth hump above the middle button.
class _BarPainter extends CustomPainter {
  @override
  void paint(Canvas canvas, Size s) {
    const r = 26.0, hump = 14.0, hw = 46.0, t = _Nav._pad;
    final w = s.width, b = s.height, cx = w / 2;
    final p = Path()
      ..moveTo(r, t)
      ..lineTo(cx - hw, t)
      ..cubicTo(cx - hw * .5, t, cx - hw * .62, t - hump, cx, t - hump)
      ..cubicTo(cx + hw * .62, t - hump, cx + hw * .5, t, cx + hw, t)
      ..lineTo(w - r, t)
      ..arcToPoint(Offset(w, t + r), radius: const Radius.circular(r))
      ..lineTo(w, b - r)
      ..arcToPoint(Offset(w - r, b), radius: const Radius.circular(r))
      ..lineTo(r, b)
      ..arcToPoint(Offset(0, b - r), radius: const Radius.circular(r))
      ..lineTo(0, t + r)
      ..arcToPoint(Offset(r, t), radius: const Radius.circular(r))
      ..close();
    canvas.drawShadow(p, Colors.black, 10, true);
    canvas.drawPath(p, Paint()..color = C.card);
    canvas.drawPath(p, Paint()..color = const Color(0x1AFFFFFF)..style = PaintingStyle.stroke..strokeWidth = 1);
  }

  @override
  bool shouldRepaint(covariant CustomPainter old) => false;
}

/// One tab of the shell: cross-fades and slides a hair with the iOS curve when it becomes
/// the visible tab. Hidden tabs ignore touches and have their tickers paused.
class _TabPane extends StatelessWidget {
  final bool visible;
  final Widget child;
  const _TabPane({required this.visible, required this.child});
  @override
  Widget build(BuildContext context) => IgnorePointer(
        ignoring: !visible,
        child: AnimatedOpacity(
          opacity: visible ? 1 : 0,
          duration: Duration(milliseconds: visible ? 300 : 160),
          curve: Curves.easeOut,
          child: AnimatedSlide(
            offset: visible ? Offset.zero : const Offset(0, .02),
            duration: const Duration(milliseconds: 290),
            curve: iosCurve,
            child: TickerMode(enabled: visible, child: RepaintBoundary(child: child)),
          ),
        ),
      );
}
