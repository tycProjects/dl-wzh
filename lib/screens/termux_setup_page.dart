import 'package:flutter/material.dart' hide Text;
import 'package:flutter/cupertino.dart' show CupertinoPageRoute;
import 'package:flutter/services.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'package:url_launcher/url_launcher.dart';
import '../anim.dart';
import '../banner.dart';
import '../termux/termux_bridge.dart';
import '../termux/termux_build.dart';
import '../theme.dart';
import '../widgets.dart';
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

/// "Set up on this phone": the one-time steps so Kotlin / Java apps can be built right here.
class TermuxSetupPage extends StatefulWidget {
  const TermuxSetupPage({super.key});
  static Route<void> route() => CupertinoPageRoute(builder: (_) => const TermuxSetupPage());
  @override
  State<TermuxSetupPage> createState() => _TermuxSetupPageState();
}

class _TermuxSetupPageState extends State<TermuxSetupPage> with WidgetsBindingObserver {
  bool channel = true;
  bool? installed, perm;
  Map<String, bool>? tools;
  bool checking = false;
  String? note;

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addObserver(this);
    _refresh();
  }

  @override
  void dispose() {
    WidgetsBinding.instance.removeObserver(this);
    super.dispose();
  }

  @override
  void didChangeAppLifecycleState(AppLifecycleState state) {
    if (state == AppLifecycleState.resumed) _refresh();
  }

  Future<void> _refresh() async {
    final c = await TermuxBridge.channelReady();
    final i = await TermuxBridge.installed();
    final p = await TermuxBridge.hasPermission();
    if (!mounted) return;
    setState(() {
      channel = c;
      installed = i;
      perm = p;
    });
  }

  void _toast(BannerKind k, String title, String msg) => IosBanner.show(context, kind: k, title: title, msg: msg, duration: const Duration(seconds: 5));

  Future<void> _allow() async {
    await TermuxBridge.requestPermission();
    await Future.delayed(const Duration(milliseconds: 1500));
    await _refresh();
    if (mounted && perm != true) {
      _toast(BannerKind.warn, 'Not allowed yet', 'If no pop-up showed, open Android Settings > Apps > this app > Permissions and allow \u201cRun commands in Termux environment\u201d.');
    }
  }

  Future<void> _install() async {
    if (installed != true || perm != true) {
      _toast(BannerKind.warn, 'Finish steps 1 to 3 first', 'Termux must be installed, allowed, and have external apps turned on.');
      return;
    }
    final err = await TermuxBuild.startInstall();
    if (err != null && mounted) _toast(BannerKind.err, 'Couldn\u2019t start Termux', err);
  }

  Future<void> _check() async {
    if (checking) return;
    setState(() {
      checking = true;
      note = null;
    });
    try {
      await _refresh();
      if (installed != true || perm != true) {
        note = 'Finish steps 1 and 2 first.';
        return;
      }
      final t = await TermuxBuild.probe();
      if (t == null) {
        note = 'Termux did not answer. Did you finish step 3 (external apps) and restart Termux?';
        await TermuxSetup.setFlag(false);
        return;
      }
      tools = t;
      final ok = TermuxBuild.toolsReady(t);
      await TermuxSetup.setFlag(ok);
      note = ok ? 'Everything is ready. You can build Kotlin and Java apps on this phone.' : 'Some tools are missing. Run step 4 and wait until Termux says Done, then check again.';
    } finally {
      if (mounted) setState(() => checking = false);
    }
  }

  Widget _dot(bool? ok) => Container(
        width: 28,
        height: 28,
        decoration: BoxDecoration(color: ok == true ? C.teal : const Color(0xFF2A2A2C), shape: BoxShape.circle),
        child: ok == true ? const Icon(Icons.check_rounded, size: 17, color: Color(0xFF062019)) : null,
      );

  Widget _btn(String t, VoidCallback f, {bool solid = true}) => Pressable(
        onTap: f,
        child: Container(
          height: 44,
          padding: const EdgeInsets.symmetric(horizontal: 18),
          alignment: Alignment.center,
          decoration: BoxDecoration(color: solid ? C.tealBtn : const Color(0xFF2A2A2C), borderRadius: BorderRadius.circular(22)),
          child: Text(t, style: inter(15.5, w: FontWeight.w700, c: solid ? _ink : C.text)),
        ),
      );

  Widget _card(int n, String title, String desc, bool? ok, List<Widget> actions) => Container(
        margin: const EdgeInsets.only(bottom: 12),
        padding: const EdgeInsets.all(16),
        decoration: BoxDecoration(color: C.card, borderRadius: BorderRadius.circular(20), border: Border.all(color: const Color(0xFF2E2E30))),
        child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
          Row(children: [
            _dot(ok),
            const SizedBox(width: 12),
            Expanded(child: Text('$n. $title', style: inter(17, w: FontWeight.w700, h: 1.25))),
          ]),
          const SizedBox(height: 8),
          Text(desc, style: inter(14.5, c: C.muted, h: 1.4)),
          if (actions.isNotEmpty) ...[
            const SizedBox(height: 12),
            Wrap(spacing: 10, runSpacing: 10, children: actions),
          ],
        ]),
      );

  @override
  Widget build(BuildContext context) {
    final ready = tools != null && TermuxBuild.toolsReady(tools!);
    return Scaffold(
      body: SafeArea(
        child: Column(children: [
          const TopBar(back: true, title: 'Build on this phone'),
          Expanded(
            child: ListView(
              physics: const IosScrollPhysics(parent: AlwaysScrollableScrollPhysics()),
              padding: const EdgeInsets.fromLTRB(20, 18, 20, 40),
              children: [
                Text('Build Kotlin and Java apps right here, with no server and no GitHub. This is a one-time setup that uses the Termux app.', style: inter(15, c: C.muted, h: 1.45)),
                const SizedBox(height: 16),
                if (!channel)
                  Container(
                    margin: const EdgeInsets.only(bottom: 12),
                    padding: const EdgeInsets.all(14),
                    decoration: BoxDecoration(color: C.card, borderRadius: BorderRadius.circular(16), border: Border.all(color: const Color(0xFF3A2A2A))),
                    child: Text('This copy of the app was built without Termux support, so this can\u2019t work yet. Build a new version of the app, then come back.', style: inter(14.5, h: 1.4)),
                  ),
                _card(1, 'Install Termux', 'Get it from F-Droid. The Play Store version is old and does not work.', installed, [
                  _btn('Open F-Droid page', () => launchUrl(Uri.parse(TermuxBridge.fdroidUrl), mode: LaunchMode.externalApplication)),
                ]),
                _card(2, 'Allow this app to use Termux', 'Android asks once. If no pop-up shows, turn on \u201cRun commands in Termux environment\u201d in this app\u2019s permissions.', perm, [
                  _btn('Allow', _allow),
                ]),
                _card(3, 'Turn on external apps in Termux', 'Open Termux, paste this one line, press Enter, then close and reopen Termux.', null, [
                  _btn('Copy the line', () async {
                    await Clipboard.setData(const ClipboardData(text: TermuxBridge.enableCommand));
                    if (mounted) _toast(BannerKind.ok, 'Copied', 'Now paste it in Termux.');
                  }),
                  _btn('Open Termux', TermuxBridge.openTermux, solid: false),
                ]),
                _card(4, 'Install the build tools', 'Opens Termux and installs Java, Kotlin and the Android tools. It downloads a few hundred MB, so use Wi-Fi. Wait until it says Done.', null, [
                  _btn('Install tools', _install),
                ]),
                _card(5, 'Check that it works', note ?? 'Tests the whole chain: this app to Termux and back.', ready ? true : null, [
                  _btn(checking ? 'Checking\u2026' : 'Check', _check),
                ]),
                if (tools != null) ...[
                  const SizedBox(height: 4),
                  Wrap(spacing: 8, runSpacing: 8, children: [
                    for (final e in tools!.entries)
                      Container(
                        padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 5),
                        decoration: BoxDecoration(color: e.value ? C.tealDeep : const Color(0xFF3A2020), borderRadius: BorderRadius.circular(12)),
                        child: Text('${e.value ? '\u2713' : '\u2717'} ${e.key}', verbatim: true, style: inter(13, c: e.value ? C.teal : C.red)),
                      ),
                  ]),
                ],
                const SizedBox(height: 18),
                Text('Good to know: only the Android platform and .jar files in your project\u2019s libs/ folder are supported so far. Projects that need androidx or Material from Maven still need the server build. Keep this app open while it builds.', style: inter(13.5, c: C.muted, h: 1.45)),
              ],
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
