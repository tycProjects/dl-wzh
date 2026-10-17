import 'dart:async';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter/cupertino.dart' show CupertinoPageRoute;
import 'api.dart';
import 'store.dart';
import 'theme.dart';
import 'screens/build_page.dart';
import 'widgets.dart';
import 'anim.dart';
import 'screens/home.dart';
import 'screens/menu_drawer.dart';

void main() {
  // Draw something immediately so the app can never sit on the native splash forever.
  runZonedGuarded(() {
    WidgetsFlutterBinding.ensureInitialized();
    FlutterError.onError = (d) => FlutterError.presentError(d);
    SystemChrome.setSystemUIOverlayStyle(const SystemUiOverlayStyle(
      statusBarColor: Colors.transparent,
      statusBarIconBrightness: Brightness.light,
      systemNavigationBarColor: Color(0xFF161616),
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

class _BootState extends State<Boot> {
  bool ready = false;
  String? error;

  @override
  void initState() {
    super.initState();
    _start();
  }

  Future<void> _start() async {
    setState(() => error = null);
    try {
      await Api.i.init().timeout(const Duration(seconds: 10));
      if (mounted) setState(() => ready = true);
    } catch (e, st) {
      if (mounted) setState(() => error = '$e\n\n$st');
    }
  }

  @override
  Widget build(BuildContext context) {
    if (ready) return const Shell();
    return Scaffold(
      backgroundColor: C.bg,
      body: Center(
        child: error == null
            ? const CircularProgressIndicator(color: C.teal)
            : Padding(
                padding: const EdgeInsets.all(24),
                child: Column(mainAxisSize: MainAxisSize.min, children: [
                  Text('Startup problem', style: inter(20, w: FontWeight.w700)),
                  const SizedBox(height: 12),
                  Flexible(child: SingleChildScrollView(child: Text(error!, style: inter(12, c: C.muted)))),
                  const SizedBox(height: 16),
                  ElevatedButton(onPressed: _start, child: const Text('Retry')),
                ]),
              ),
      ),
    );
  }
}

class WzhApp extends StatelessWidget {
  const WzhApp({super.key});
  @override
  Widget build(BuildContext context) => MaterialApp(
        debugShowCheckedModeBanner: false,
        title: 'WZH 2 APK',
        theme: buildTheme(),
        scrollBehavior: const _NoGlow(),
        builder: (c, w) => MediaQuery(
          data: MediaQuery.of(c).copyWith(textScaler: TextScaler.noScaling),
          child: w!,
        ),
        home: const Boot(),
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
  int tab = 1; // 0 Message, 1 Build, 2 Updates

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addObserver(this);
    Store.i.refreshLimit();
    Store.i.refreshHistory();
    // A build that was still running when the app was closed: go straight back to it.
    WidgetsBinding.instance.addPostFrameCallback((_) {
      final id = Store.i.activeJobId;
      if (id != null && mounted) {
        Navigator.of(context).push(CupertinoPageRoute(builder: (_) => BuildPage(jobId: id, appName: 'Your app')));
      }
    });
  }

  @override
  void dispose() {
    WidgetsBinding.instance.removeObserver(this);
    super.dispose();
  }

  @override
  void didChangeAppLifecycleState(AppLifecycleState state) {
    if (state == AppLifecycleState.resumed) {
      Store.i.refreshHistory();
      Store.i.refreshLimit();
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      body: SafeArea(
        bottom: false,
        child: Stack(children: [
          Column(children: [
            TopBar(onMenu: () => showIosDrawer(context, (_) => const MenuDrawer())),
            Expanded(
              child: IndexedStack(index: tab, children: const [
                _Soon('Message'), HomeScreen(), _Soon('Updates'),
              ]),
            ),
          ]),
          const Positioned(right: 17, bottom: 17, child: BotFab()),
        ]),
      ),
      bottomNavigationBar: _Nav(index: tab, onTap: (i) => setState(() => tab = i)),
    );
  }
}

class _Soon extends StatelessWidget {
  final String name;
  const _Soon(this.name);
  @override
  Widget build(BuildContext context) => Center(child: Text(name, style: inter(15, c: C.muted)));
}

class _Nav extends StatelessWidget {
  final int index;
  final ValueChanged<int> onTap;
  const _Nav({required this.index, required this.onTap});

  @override
  Widget build(BuildContext context) {
    const items = [
      (Icons.chat_bubble_outline_rounded, 'Message'),
      (Icons.add_rounded, 'Build'),
      (Icons.notifications_none_rounded, 'Updates'),
    ];
    return Container(
      height: 80 + MediaQuery.of(context).padding.bottom,
      padding: const EdgeInsets.only(top: 9),
      decoration: const BoxDecoration(color: Color(0xFF161616), border: Border(top: BorderSide(color: C.line))),
      child: Row(children: [
        for (var i = 0; i < items.length; i++)
          Expanded(
            child: GestureDetector(
              behavior: HitTestBehavior.opaque,
              onTap: () => onTap(i),
              child: Column(children: [
                AnimatedContainer(
                  duration: const Duration(milliseconds: 220),
                  width: 67, height: 38,
                  decoration: BoxDecoration(
                    color: i == index ? C.tealDeep : Colors.transparent,
                    borderRadius: BorderRadius.circular(19)),
                  child: Icon(items[i].$1, size: 26, color: i == index ? C.teal : C.muted),
                ),
                const SizedBox(height: 4),
                Text(items[i].$2,
                    style: inter(13, w: i == index ? FontWeight.w600 : FontWeight.w400,
                        c: i == index ? C.teal : C.muted)),
              ]),
            ),
          ),
      ]),
    );
  }
}
