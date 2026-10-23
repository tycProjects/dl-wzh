import 'dart:async';
import 'package:flutter/material.dart' hide Text;
import 'package:flutter/services.dart';
import 'package:flutter/foundation.dart' show ValueListenable;
import 'package:flutter/cupertino.dart' show CupertinoPageRoute;
import 'api.dart';
import 'integrity.dart';
import 'quick/quick_builder.dart';
import 'notices.dart';
import 'store.dart';
import 'theme.dart';
import 'screens/build_page.dart';
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
    if (state == AppLifecycleState.resumed) _verify();
  }

  /// Blocks the app only when the signature was read and is not one of the trusted ones.
  Future<void> _verify() async {
    final v = await AppIntegrity.verdict();
    if (mounted && tampered != (v == IntegrityVerdict.tampered)) setState(() => tampered = v == IntegrityVerdict.tampered);
  }

  Future<void> _start() async {
    setState(() => error = null);
    try {
      await Api.i.init().timeout(const Duration(seconds: 10));
      Appearance.i.load();
      Lang.i.load();
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
                  child: Text('Try again', style: inter(17, w: FontWeight.w700, c: const Color(0xFF0F2A24))),
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
  ScrollPhysics getScrollPhysics(BuildContext context) => const BouncingScrollPhysics(parent: AlwaysScrollableScrollPhysics());
}

class Shell extends StatefulWidget {
  const Shell({super.key});
  @override
  State<Shell> createState() => _ShellState();
}

class _ShellState extends State<Shell> with WidgetsBindingObserver {
  bool _admin = false; // the floating Admin message card
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
    return PopScope(
      canPop: !_admin,
      onPopInvokedWithResult: (didPop, _) {
        if (!didPop && _admin) setState(() => _admin = false);
      },
      child: Scaffold(
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
                _TabPane(visible: tab == ShellNav.code, child: const CodeHome()),
              ]),
            ),
          ]),
          // Floating Admin message: a rounded card over the current screen (state stays alive).
          Positioned.fill(
            child: IgnorePointer(
              ignoring: !_admin,
              child: AnimatedOpacity(
                opacity: _admin ? 1 : 0,
                duration: Duration(milliseconds: _admin ? 220 : 160),
                curve: Curves.easeOut,
                child: Stack(children: [
                  Positioned.fill(
                    child: GestureDetector(
                      behavior: HitTestBehavior.opaque,
                      onTap: () {
                        FocusManager.instance.primaryFocus?.unfocus();
                        setState(() => _admin = false);
                      },
                      child: const ColoredBox(color: Color(0x73000000)),
                    ),
                  ),
                  Align(
                    alignment: Alignment.bottomCenter,
                    child: LayoutBuilder(builder: (_, box) {
                      final h = (box.maxHeight - 20).clamp(200.0, 540.0).toDouble();
                      return AnimatedSlide(
                        offset: _admin ? Offset.zero : const Offset(0, .06),
                        duration: const Duration(milliseconds: 300),
                        curve: iosCurve,
                        child: Padding(
                          padding: const EdgeInsets.fromLTRB(12, 0, 12, 10),
                          child: Container(
                            height: h,
                            decoration: BoxDecoration(
                              color: const Color(0xFF151516),
                              borderRadius: BorderRadius.circular(30),
                              border: Border.all(color: C.line),
                              boxShadow: const [BoxShadow(color: Color(0x99000000), blurRadius: 30, offset: Offset(0, 10))],
                            ),
                            clipBehavior: Clip.antiAlias,
                            child: TickerMode(
                              enabled: _admin,
                              child: MessagePage(onClose: () {
                                FocusManager.instance.primaryFocus?.unfocus();
                                setState(() => _admin = false);
                              }),
                            ),
                          ),
                        ),
                      );
                    }),
                  ),
                ]),
              ),
            ),
          ),
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
                      child: BotFab(onTap: () => setState(() => _admin = true)),
                    ),
                  ),
                );
              },
            ),
          ),
        ]),
      ),
      bottomNavigationBar: MediaQuery.of(context).viewInsets.bottom > 0
          ? const SizedBox.shrink()
          : _Nav(index: tab, onTap: ShellNav.go),
    ));
  }
}

/// Bottom bar: flat bar with a thin top divider and three equal tabs - Host, Build, Code.
/// The active tab gets a dark-green pill behind its icon and a teal bold label.
/// (Message opens from the bot button.)
class _Nav extends StatelessWidget {
  final int index;
  final ValueChanged<int> onTap;
  const _Nav({required this.index, required this.onTap});

  static const _pill = Color(0xFF1F3B32);
  static const _idle = Color(0xFF8E8E93);

  Widget _item(int i, IconData icon, String label, {ValueListenable<bool>? badge}) {
    final sel = i == index;
    final col = sel ? C.teal : _idle;
    return Expanded(
      child: Pressable(
        scale: .92,
        onTap: () => onTap(i),
        child: Container(
          color: Colors.transparent,
          alignment: Alignment.center,
          child: Column(mainAxisSize: MainAxisSize.min, children: [
            AnimatedContainer(
              duration: const Duration(milliseconds: 240),
              curve: iosCurve,
              width: sel ? 66 : 40,
              height: 36,
              decoration: BoxDecoration(
                color: sel ? _pill : Colors.transparent,
                borderRadius: BorderRadius.circular(18),
              ),
              child: Stack(clipBehavior: Clip.none, alignment: Alignment.center, children: [
                Icon(icon, size: 26, color: col),
                if (badge != null)
                  Positioned(
                    top: -4,
                    right: sel ? 2 : -10,
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
            const SizedBox(height: 5),
            Text(label, maxLines: 1, overflow: TextOverflow.ellipsis, style: inter(14, w: sel ? FontWeight.w700 : FontWeight.w500, c: col)),
          ]),
        ),
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    final safe = MediaQuery.of(context).padding.bottom;
    return Container(
      decoration: const BoxDecoration(
        color: C.bg,
        border: Border(top: BorderSide(color: C.line, width: 1)),
      ),
      padding: EdgeInsets.only(bottom: safe > 0 ? safe : 8),
      child: SizedBox(
        height: 76,
        child: Row(children: [
          _item(ShellNav.host, Icons.cloud_upload_outlined, 'Host'),
          _item(ShellNav.home, Icons.add_rounded, 'Build'),
          _item(ShellNav.code, Icons.code_rounded, 'Code', badge: ShellNav.codeNew),
        ]),
      ),
    );
  }
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
