import 'package:flutter/material.dart' hide Text;
import 'package:flutter/cupertino.dart' show CupertinoPageRoute;
import 'package:flutter/services.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'package:url_launcher/url_launcher.dart';
import '../anim.dart';
import '../termux/termux_bridge.dart';
import '../termux/termux_download.dart';
import '../termux/termux_build.dart';
import '../theme.dart';
import '../widgets.dart';
import 'code_terminal.dart';
import '../tr.dart';

/// Remembers (on this phone) that the Termux build tools were checked and work.
class TermuxSetup {
  static const _key = 'termux_ready_v1';

  static Future<bool> flag() async => (await SharedPreferences.getInstance()).getBool(_key) ?? false;
  static Future<void> setFlag(bool v) async => (await SharedPreferences.getInstance()).setBool(_key, v);

  /// True when Termux is installed, this app may run commands there, and the tools were checked.
  static Future<bool> isReady() async => await flag() && await TermuxBridge.installed() && await TermuxBridge.hasPermission();
}

const _ink = Color(0xFF252A27);

/// "Build on this phone": one tap. It installs/allows/checks everything in order and carries on by itself
/// when you come back from F-Droid or Termux. Pops `true` when the phone is ready to build.
class TermuxSetupPage extends StatefulWidget {
  const TermuxSetupPage({super.key});
  static Route<bool> route() => CupertinoPageRoute<bool>(builder: (_) => const TermuxSetupPage());
  @override
  State<TermuxSetupPage> createState() => _TermuxSetupPageState();
}

class _TermuxSetupPageState extends State<TermuxSetupPage> with WidgetsBindingObserver {
  bool channel = true;
  bool? installed, perm, linked, toolsOk;
  bool busy = false;
  bool waiting = false; // true while the person is away (F-Droid / Termux); we continue when they return
  bool done = false;
  String status = 'Tap once. I will do the rest and tell you if I need you for a moment.';

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addObserver(this);
    _quickLook();
  }

  @override
  void dispose() {
    WidgetsBinding.instance.removeObserver(this);
    super.dispose();
  }

  @override
  void didChangeAppLifecycleState(AppLifecycleState state) {
    if (state != AppLifecycleState.resumed) return;
    if (waiting && !busy) {
      _run();
    } else {
      _quickLook();
    }
  }

  Future<void> _quickLook() async {
    final c = await TermuxBridge.channelReady();
    final i = await TermuxBridge.installed();
    final p = await TermuxBridge.hasPermission();
    final ready = await TermuxSetup.flag() && i && p;
    if (!mounted) return;
    setState(() {
      channel = c;
      installed = i;
      perm = p;
      if (ready) {
        linked = true;
        toolsOk = true;
        done = true;
        status = 'Everything is ready. You can build on this phone.';
      }
    });
  }

  void _say(String s) {
    if (mounted) setState(() => status = s);
  }

  /// Walks through every step, skipping the ones already done. Stops (and waits) only where Android
  /// itself needs the person: installing Termux, and pasting one line in Termux.
  Future<void> _run() async {
    if (busy) return;
    if (done) {
      if (mounted) Navigator.of(context).pop(true);
      return;
    }
    setState(() {
      busy = true;
      waiting = false;
    });
    try {
      // 1. Termux itself
      final i = await TermuxBridge.installed();
      if (!mounted) return;
      setState(() => installed = i);
      if (!i) {
        waiting = true;
        _say('Downloading Termux...');
        final err = await TermuxDownload.downloadAndInstall(onProgress: (p) {
          if (p != null) _say('Downloading Termux... ${(p * 100).round()}%');
        });
        if (err == null) {
          _say('Tap Install on the Android screen, then come back here. I will carry on by myself.');
        } else {
          _say('$err Opening the download page instead.');
          await launchUrl(Uri.parse(TermuxBridge.fdroidUrl), mode: LaunchMode.externalApplication);
        }
        return;
      }

      // 2. Permission to run commands in Termux
      if (!await TermuxBridge.hasPermission()) {
        _say('Allow this app to use Termux when Android asks.');
        await TermuxBridge.requestPermission();
        await Future.delayed(const Duration(milliseconds: 1500));
      }
      final p = await TermuxBridge.hasPermission();
      if (!mounted) return;
      setState(() => perm = p);
      if (!p) {
        waiting = true;
        _say('Turn on \u201cRun commands in Termux environment\u201d in this app\u2019s permissions, then come back. I will carry on.');
        return;
      }

      // 3. Termux answers us (needs "external apps" turned on once)
      _say('Checking that Termux answers...');
      var t = await TermuxBuild.probe();
      if (!mounted) return;
      if (t == null) {
        setState(() => linked = false);
        await Clipboard.setData(const ClipboardData(text: TermuxBridge.enableCommand));
        waiting = true;
        _say('I copied one line. In Termux: paste it, press Enter, close Termux, then come back. I will carry on.');
        await Future.delayed(const Duration(milliseconds: 600));
        await TermuxBridge.openTermux();
        return;
      }
      setState(() => linked = true);

      // 4. Build tools
      if (!TermuxBuild.toolsReady(t)) {
        _say('Installing the build tools (use Wi-Fi, a few hundred MB). Stay until it says Done.');
        final script = await rootBundle.loadString('assets/termux/wzh_setup.sh');
        if (!mounted) return;
        await Navigator.of(context).push(CodeTerminalPage.route(
          initialCommand: script,
          initialLabel: 'Installing the build tools (Java, Kotlin, Android tools)...',
        ));
        if (!mounted) return;
        _say('Checking the tools...');
        t = await TermuxBuild.probe();
      }
      final ok = t != null && TermuxBuild.toolsReady(t);
      await TermuxSetup.setFlag(ok);
      if (!mounted) return;
      setState(() {
        toolsOk = ok;
        done = ok;
      });
      if (ok) {
        _say('All set. Taking you back to build...');
        await Future.delayed(const Duration(milliseconds: 900));
        if (mounted) Navigator.of(context).pop(true);
      } else {
        _say('Some tools are still missing. Tap again to retry the install.');
      }
    } finally {
      if (mounted) setState(() => busy = false);
    }
  }

  Widget _row(String title, bool? ok, {bool active = false}) => Padding(
        padding: const EdgeInsets.symmetric(vertical: 9),
        child: Row(children: [
          Container(
            width: 26,
            height: 26,
            decoration: BoxDecoration(color: ok == true ? C.teal : const Color(0xFF2A2A2C), shape: BoxShape.circle),
            child: ok == true
                ? const Icon(Icons.check_rounded, size: 16, color: Color(0xFF062019))
                : (active ? Padding(padding: const EdgeInsets.all(6), child: CircularProgressIndicator(strokeWidth: 2.2, color: C.teal)) : null),
          ),
          const SizedBox(width: 12),
          Expanded(child: Text(title, style: inter(16, w: FontWeight.w600, c: ok == true ? C.text : C.muted))),
        ]),
      );

  @override
  Widget build(BuildContext context) {
    final label = done ? 'Done' : (busy ? 'Working...' : (waiting ? 'Continue' : 'Set up'));
    return Scaffold(
      body: SafeArea(
        child: Column(children: [
          const TopBar(back: true, title: 'Build on this phone'),
          Expanded(
            child: ListView(
              physics: const IosScrollPhysics(parent: AlwaysScrollableScrollPhysics()),
              padding: const EdgeInsets.fromLTRB(20, 18, 20, 24),
              children: [
                Text('Build Kotlin and Java apps right here, with no server. One tap sets everything up.', style: inter(15, c: C.muted, h: 1.45)),
                const SizedBox(height: 16),
                if (!channel)
                  Container(
                    margin: const EdgeInsets.only(bottom: 12),
                    padding: const EdgeInsets.all(14),
                    decoration: BoxDecoration(color: C.card, borderRadius: BorderRadius.circular(16), border: Border.all(color: const Color(0xFF3A2A2A))),
                    child: Text('This copy of the app was built without Termux support, so this can\u2019t work yet. Build a new version of the app, then come back.', style: inter(14.5, h: 1.4)),
                  ),
                Container(
                  padding: const EdgeInsets.fromLTRB(16, 8, 16, 8),
                  decoration: BoxDecoration(color: C.card, borderRadius: BorderRadius.circular(20), border: Border.all(color: const Color(0xFF2E2E30))),
                  child: Column(children: [
                    _row('Termux installed', installed, active: busy && installed != true),
                    _row('This app allowed to use it', perm, active: busy && installed == true && perm != true),
                    _row('Termux connected', linked, active: busy && perm == true && linked != true),
                    _row('Build tools installed', toolsOk, active: busy && linked == true && toolsOk != true),
                  ]),
                ),
                const SizedBox(height: 16),
                Text(status, style: inter(14.5, c: C.muted, h: 1.4)),
              ],
            ),
          ),
          Padding(
            padding: const EdgeInsets.fromLTRB(20, 0, 20, 14),
            child: Pressable(
              onTap: busy ? () {} : _run,
              child: Container(
                height: 56,
                alignment: Alignment.center,
                decoration: BoxDecoration(color: busy ? const Color(0xFF2A2A2C) : C.teal, borderRadius: BorderRadius.circular(28)),
                child: Text(label, style: inter(17, w: FontWeight.w700, c: busy ? C.muted : _ink)),
              ),
            ),
          ),
        ]),
      ),
    );
  }
}

/// "Where should it build?" for Kotlin / Java projects. Returns 'phone', 'server' or null (closed).
Future<String?> showKotlinBuildSheet(BuildContext context) => showIosSheet<String>(context, (_) => const _KotlinBuildSheet());

class _KotlinBuildSheet extends StatefulWidget {
  const _KotlinBuildSheet();
  @override
  State<_KotlinBuildSheet> createState() => _KotlinBuildSheetState();
}

class _KotlinBuildSheetState extends State<_KotlinBuildSheet> {
  String selected = 'server';
  bool? ready;

  @override
  void initState() {
    super.initState();
    TermuxSetup.isReady().then((v) {
      if (mounted) {
        setState(() {
          ready = v;
          if (v) selected = 'phone';
        });
      }
    });
  }

  Widget _tile(String id, IconData icon, String title, String sub, {String? badge}) {
    final on = selected == id;
    return Pressable(
      scale: .98,
      onTap: () => setState(() => selected = id),
      child: AnimatedContainer(
        duration: const Duration(milliseconds: 240),
        curve: Curves.easeOutCubic,
        padding: const EdgeInsets.all(14),
        decoration: BoxDecoration(
          color: on ? const Color(0xFF222926) : C.field,
          borderRadius: BorderRadius.circular(22),
          border: Border.all(color: on ? C.teal : const Color(0xFF2E2E30), width: on ? 1.6 : 1),
        ),
        child: Row(children: [
          Container(
            width: 44,
            height: 44,
            decoration: BoxDecoration(color: on ? C.tealBtn : C.tealDeep, borderRadius: BorderRadius.circular(14)),
            child: Icon(icon, size: 24, color: on ? _ink : C.teal),
          ),
          const SizedBox(width: 12),
          Expanded(
            child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
              Row(children: [
                Flexible(child: Text(title, style: inter(17, w: FontWeight.w700))),
                if (badge != null) ...[
                  const SizedBox(width: 8),
                  Container(
                    padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 2),
                    decoration: BoxDecoration(color: C.tealDeep, borderRadius: BorderRadius.circular(10)),
                    child: Text(badge, style: inter(11.5, w: FontWeight.w700, c: C.teal)),
                  ),
                ],
              ]),
              const SizedBox(height: 2),
              Text(sub, style: inter(13.5, c: C.muted, h: 1.3)),
            ]),
          ),
        ]),
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    final phone = selected == 'phone';
    return Container(
      width: double.infinity,
      padding: EdgeInsets.fromLTRB(16, 10, 16, 14 + MediaQuery.of(context).padding.bottom),
      decoration: const BoxDecoration(color: C.sheet, borderRadius: BorderRadius.vertical(top: Radius.circular(30))),
      child: Column(mainAxisSize: MainAxisSize.min, crossAxisAlignment: CrossAxisAlignment.stretch, children: [
        Center(child: Container(width: 38, height: 5, decoration: BoxDecoration(color: const Color(0xFF4A4A4C), borderRadius: BorderRadius.circular(3)))),
        const SizedBox(height: 16),
        Padding(
          padding: const EdgeInsets.symmetric(horizontal: 4),
          child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
            Text('Where should it build?', style: inter(20, w: FontWeight.w700)),
            const SizedBox(height: 1),
            Text('You can pick a different way next time.', style: inter(13, c: C.muted)),
          ]),
        ),
        const SizedBox(height: 14),
        _tile('phone', Icons.smartphone_rounded, 'On this phone', ready == true ? 'Uses Termux. No server, no limit.' : 'Uses Termux. Needs a one-time setup.', badge: 'Free'),
        const SizedBox(height: 10),
        _tile('server', Icons.cloud_done_rounded, 'Build server', 'Uses a build. Handles androidx and other libraries.'),
        const SizedBox(height: 16),
        Pressable(
          onTap: () async {
            if (phone && ready != true) {
              final nav = Navigator.of(context, rootNavigator: true);
              nav.pop('setup');
              return;
            }
            Navigator.of(context, rootNavigator: true).pop(selected);
          },
          child: Container(
            height: 56,
            alignment: Alignment.center,
            decoration: BoxDecoration(color: C.teal, borderRadius: BorderRadius.circular(28)),
            child: Text(phone ? (ready == true ? 'Build on this phone' : 'Set up on this phone') : 'Build on the server', style: inter(17, w: FontWeight.w700, c: _ink)),
          ),
        ),
        const SizedBox(height: 2),
        Pressable(
          onTap: () => Navigator.of(context, rootNavigator: true).pop(),
          child: SizedBox(height: 44, child: Center(child: Text('Cancel', style: inter(15, w: FontWeight.w600, c: C.muted)))),
        ),
      ]),
    );
  }
}
