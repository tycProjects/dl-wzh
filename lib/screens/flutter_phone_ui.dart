import 'package:flutter/material.dart' hide Text;
import 'package:flutter/cupertino.dart' show CupertinoPageRoute;
import '../anim.dart';
import '../termux/flutter_phone.dart';
import '../theme.dart';
import '../tr.dart';
import '../widgets.dart';
import 'code_terminal.dart';
import 'termux_setup_page.dart';

const _ink = Color(0xFF252A27);

/// "Where should it build?" for Flutter projects. Returns 'phone', 'setup' (phone chosen, tools not installed
/// yet), 'server' or null (closed).
Future<String?> showFlutterBuildSheet(BuildContext context) => showIosSheet<String>(context, (_) => const _FlutterBuildSheet());

class _FlutterBuildSheet extends StatefulWidget {
  const _FlutterBuildSheet();
  @override
  State<_FlutterBuildSheet> createState() => _FlutterBuildSheetState();
}

class _FlutterBuildSheetState extends State<_FlutterBuildSheet> {
  String selected = 'server';
  bool? ready;

  @override
  void initState() {
    super.initState();
    FlutterPhone.isReady().then((v) {
      if (mounted) {
        setState(() {
          ready = v;
          if (v) selected = 'phone';
        });
      }
    });
  }

  Widget _chip(IconData i, String t, bool on) => Container(
        padding: const EdgeInsets.fromLTRB(7, 3, 9, 3),
        decoration: BoxDecoration(color: const Color(0x14FFFFFF), borderRadius: BorderRadius.circular(8)),
        child: Row(mainAxisSize: MainAxisSize.min, children: [
          Icon(i, size: 12, color: on ? C.teal : C.muted),
          const SizedBox(width: 4),
          Text(t, style: inter(11, w: FontWeight.w600)),
        ]),
      );

  Widget _tile(String id, IconData icon, String title, String sub, List<(IconData, String)> chips, {String? badge}) {
    final on = selected == id;
    return Pressable(
      scale: .98,
      onTap: () => setState(() => selected = id),
      child: AnimatedContainer(
        duration: const Duration(milliseconds: 240),
        curve: Curves.easeOutCubic,
        padding: const EdgeInsets.fromLTRB(12, 11, 12, 11),
        decoration: BoxDecoration(
          color: on ? const Color(0xFF1F2522) : C.field,
          borderRadius: BorderRadius.circular(18),
          border: Border.all(color: on ? C.teal : const Color(0xFF2E2E30), width: on ? 1.4 : 1),
        ),
        child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
          Row(children: [
            Container(
              width: 36,
              height: 36,
              decoration: BoxDecoration(color: on ? C.tealBtn : C.tealDeep, borderRadius: BorderRadius.circular(11)),
              child: Icon(icon, size: 20, color: on ? _ink : C.teal),
            ),
            const SizedBox(width: 10),
            Expanded(
              child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
                Row(children: [
                  Flexible(child: Text(title, style: inter(15, w: FontWeight.w700))),
                  if (badge != null) ...[
                    const SizedBox(width: 8),
                    Container(
                      padding: const EdgeInsets.symmetric(horizontal: 7, vertical: 1.5),
                      decoration: BoxDecoration(color: C.tealDeep, borderRadius: BorderRadius.circular(7)),
                      child: Text(badge, style: inter(10, w: FontWeight.w700, c: C.teal)),
                    ),
                  ],
                ]),
                const SizedBox(height: 2),
                Text(sub, style: inter(12, c: C.muted, h: 1.25)),
              ]),
            ),
            const SizedBox(width: 8),
            Container(
              width: 20,
              height: 20,
              decoration: BoxDecoration(
                shape: BoxShape.circle,
                color: on ? C.tealBtn : Colors.transparent,
                border: Border.all(color: on ? C.tealBtn : const Color(0xFF5A5A5E), width: 1.8),
              ),
              child: on ? const Icon(Icons.check_rounded, size: 13, color: _ink) : null,
            ),
          ]),
          const SizedBox(height: 8),
          Wrap(spacing: 5, runSpacing: 5, children: [for (final c in chips) _chip(c.$1, c.$2, on)]),
        ]),
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    final phone = selected == 'phone';
    final isReady = ready == true;
    final label = phone ? (isReady ? 'Continue with Build on phone' : 'Set up Flutter on this phone') : 'Continue with Full build';
    return Container(
      width: double.infinity,
      padding: EdgeInsets.fromLTRB(14, 8, 14, 10 + MediaQuery.of(context).padding.bottom),
      decoration: const BoxDecoration(color: C.sheet, borderRadius: BorderRadius.vertical(top: Radius.circular(24))),
      child: Column(mainAxisSize: MainAxisSize.min, crossAxisAlignment: CrossAxisAlignment.stretch, children: [
        Center(child: Container(width: 34, height: 4, decoration: BoxDecoration(color: const Color(0xFF4A4A4C), borderRadius: BorderRadius.circular(2)))),
        const SizedBox(height: 12),
        Padding(
          padding: const EdgeInsets.symmetric(horizontal: 4),
          child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
            Text('Where should it build?', style: inter(17, w: FontWeight.w700)),
            const SizedBox(height: 1),
            Text('You can pick a different way next time.', style: inter(12, c: C.muted)),
          ]),
        ),
        const SizedBox(height: 12),
        _tile('phone', Icons.smartphone_rounded, 'Build on phone', 'Flutter compiled on your phone with Termux. No server, no limit',
            [
              (Icons.schedule_rounded, '10 to 30 minutes'),
              isReady ? (Icons.cloud_off_rounded, 'No server') : (Icons.build_circle_outlined, 'One-time setup, about 4 GB'),
              (Icons.wifi_rounded, 'Internet for libraries'),
            ],
            badge: 'Beta'),
        const SizedBox(height: 8),
        _tile('server', Icons.cloud_done_rounded, 'Full build', 'Built on the server with every feature',
            [(Icons.schedule_rounded, 'A few minutes'), (Icons.wifi_rounded, 'Needs internet')]),
        const SizedBox(height: 12),
        Pressable(
          onTap: () => Navigator.of(context, rootNavigator: true).pop(phone && !isReady ? 'setup' : selected),
          child: Container(
            height: 48,
            alignment: Alignment.center,
            decoration: BoxDecoration(color: C.teal, borderRadius: BorderRadius.circular(24)),
            child: Row(mainAxisAlignment: MainAxisAlignment.center, children: [
              Text(label, style: inter(15, w: FontWeight.w700, c: _ink)),
              const SizedBox(width: 6),
              const Icon(Icons.arrow_forward_rounded, size: 18, color: _ink),
            ]),
          ),
        ),
        Pressable(
          onTap: () => Navigator.of(context, rootNavigator: true).pop(),
          child: SizedBox(height: 38, child: Center(child: Text('Cancel', style: inter(13, w: FontWeight.w600, c: C.muted)))),
        ),
      ]),
    );
  }
}

/// One-time setup of the Flutter tools. Pops `true` when they are installed and checked.
class FlutterSetupPage extends StatefulWidget {
  const FlutterSetupPage({super.key});
  static Route<bool> route() => CupertinoPageRoute<bool>(builder: (_) => const FlutterSetupPage());
  @override
  State<FlutterSetupPage> createState() => _FlutterSetupPageState();
}

class _FlutterSetupPageState extends State<FlutterSetupPage> {
  bool busy = false, done = false;
  bool? termuxOk, toolsOk;
  String status = 'Tap once. This downloads Flutter, Java and the Android tools (about 4 GB), so use Wi-Fi and keep the phone charged.';

  void _say(String s) {
    if (mounted) setState(() => status = s);
  }

  Future<void> _run() async {
    if (busy) return;
    if (done) {
      Navigator.of(context).pop(true);
      return;
    }
    setState(() => busy = true);
    try {
      // 1. Termux itself (install, allow, connect): the same one-tap setup the Kotlin builds use
      if (!await TermuxSetup.isReady()) {
        final ok = await Navigator.of(context).push<bool>(TermuxSetupPage.route());
        if (!mounted) return;
        if (ok != true) {
          _say('Termux is not ready yet. Tap Set up to finish it first.');
          return;
        }
      }
      setState(() => termuxOk = true);

      // 2. Already installed?
      _say('Checking the Flutter tools...');
      var t = await FlutterPhone.probe();
      if (!mounted) return;
      if (t == null) {
        _say('Termux did not answer. Open Termux once, then tap Set up again.');
        return;
      }

      // 3. Install (visible, long)
      if (t['flutter'] != true) {
        _say('Installing the Flutter tools. Stay until it says Done.');
        final script = await FlutterPhone.setupScript();
        if (!mounted) return;
        await Navigator.of(context).push(CodeTerminalPage.route(initialCommand: script, initialLabel: 'Installing Flutter, Java and the Android tools...'));
        if (!mounted) return;
        _say('Checking the Flutter tools...');
        t = await FlutterPhone.probe();
      }
      final ok = t != null && t['flutter'] == true;
      await FlutterPhone.setFlag(ok);
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
        final names = {'has_flutter': 'Flutter', 'has_java': 'Java', 'has_sdk': 'Android SDK', 'has_aapt2': 'aapt2', 'has_gensnapshot': 'gen_snapshot'};
        final miss = t == null ? <String>[] : [for (final e in names.entries) if (t[e.key] == false) e.value];
        if (miss.isEmpty) {
          _say('The Flutter tools did not finish. Tap Set up to try again.');
        } else if (miss.contains('aapt2') || miss.contains('gen_snapshot')) {
          _say('Still missing: ${miss.join(', ')}. aapt2 and gen_snapshot are the two parts this phone may not be able to run, so if they stay missing after another try, use Full build.');
        } else {
          _say('Still missing: ${miss.join(', ')}. Tap Set up to try again (it keeps what is already installed).');
        }
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
    final label = done ? 'Done' : (busy ? 'Working...' : 'Set up');
    return Scaffold(
      body: SafeArea(
        child: Column(children: [
          const TopBar(back: true, title: 'Flutter on this phone'),
          Expanded(
            child: ListView(
              physics: const IosScrollPhysics(parent: AlwaysScrollableScrollPhysics()),
              padding: const EdgeInsets.fromLTRB(20, 18, 20, 24),
              children: [
                Text('Build Flutter apps right here, with no server. This is a beta: it installs a small Linux system inside Termux and the first build can take a while.', style: inter(15, c: C.muted, h: 1.45)),
                const SizedBox(height: 16),
                Container(
                  padding: const EdgeInsets.fromLTRB(16, 8, 16, 8),
                  decoration: BoxDecoration(color: C.card, borderRadius: BorderRadius.circular(20), border: Border.all(color: const Color(0xFF2E2E30))),
                  child: Column(children: [
                    _row('Termux ready', termuxOk, active: busy && termuxOk != true),
                    _row('Flutter tools installed', toolsOk, active: busy && termuxOk == true && toolsOk != true),
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
