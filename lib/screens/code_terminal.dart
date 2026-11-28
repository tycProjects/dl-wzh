import 'dart:async';
import '../anim.dart';
import 'dart:typed_data';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import '../termux/termux_session.dart';
import '../theme.dart';
import 'termux_setup_page.dart';

/// A terminal inside the code editor. Every command runs in Termux (see termux_session.dart) and shows up
/// here as a card: the command, what it printed, and whether it worked, in plain words.
class CodeTerminalPage extends StatefulWidget {
  /// The open project as a zip. When given, Termux unpacks a copy to ~/wzh_project and starts there.
  final Uint8List? projectZip;

  /// Runs this as soon as the terminal is ready (shown as [initialLabel] when given, e.g. for a long script).
  final String? initialCommand;
  final String? initialLabel;

  /// A session the caller already started (and the reason it failed, if it did), so the page opens ready.
  final TermuxSession? preSession;
  final String? preError;
  const CodeTerminalPage({super.key, this.projectZip, this.initialCommand, this.initialLabel, this.preSession, this.preError});

  static Route<void> route({Uint8List? projectZip, String? initialCommand, String? initialLabel, TermuxSession? preSession, String? preError}) =>
      MaterialPageRoute<void>(
          builder: (_) => CodeTerminalPage(
              projectZip: projectZip, initialCommand: initialCommand, initialLabel: initialLabel, preSession: preSession, preError: preError));

  @override
  State<CodeTerminalPage> createState() => _CodeTerminalPageState();
}

const _bg = Color(0xFF111111);
const _inset = Color(0xFF0B0B0C);
const _mono = TextStyle(fontFamily: 'monospace', fontSize: 12.5, height: 1.4, color: Color(0xFFD4D4D4));

/// Things a beginner can try. A command ending in a space waits for the person to finish it.
const _ideas = <(String, String)>[
  ('Where am I?', 'pwd'),
  ('Show my files', 'ls'),
  ('Show files with details', 'ls -la'),
  ('Go up one folder', 'cd ..'),
  ('Go to my home folder', 'cd ~'),
  ('Open a folder', 'cd '),
  ('Make a new folder', 'mkdir '),
  ('Read a file', 'cat '),
  ('Check the build tools', 'for t in java kotlinc aapt2 d8; do command -v \$t >/dev/null && echo "ok       \$t" || echo "missing  \$t"; done'),
  ('Install something', 'pkg install '),
];

class _Block {
  final String shown;
  final String where;
  final _Term out = _Term();
  bool running = true;
  int? rc;
  String secs = '';
  String? hint;
  List<InlineSpan>? cache;
  _Block(this.shown, this.where);
}

class _CodeTerminalPageState extends State<CodeTerminalPage> {
  late TermuxSession session;
  final blocks = <_Block>[];
  final input = TextEditingController();
  final focus = FocusNode();
  final scroll = ScrollController();
  final history = <String>[];
  int hIdx = 0;
  StreamSubscription<String>? sub;
  Timer? tick;
  String? error;
  String? intro;
  bool ready = false;
  bool busy = false;
  _Block? active;
  TermuxSession? _pre;

  @override
  void initState() {
    super.initState();
    _pre = widget.preSession;
    _start();
  }

  Future<void> _start() async {
    setState(() {
      error = null;
      ready = false;
    });
    final pre = _pre;
    _pre = null; // only the first start uses the session that was started for us
    session = pre ?? TermuxSession(projectZip: widget.projectZip);
    sub = session.output.listen((s) {
      active?.out.add(s);
      active?.cache = null;
      _later();
    });
    final err = pre != null ? widget.preError : await session.start();
    if (!mounted) return;
    if (err != null) {
      setState(() => error = err);
      return;
    }
    intro = (widget.projectZip != null && session.cwd.endsWith('wzh_project'))
        ? 'Your project is copied to the folder wzh_project. Changes you make here are not sent back to the editor.'
        : null;
    setState(() => ready = true);
    if (widget.initialCommand != null) {
      _submit(widget.initialCommand, widget.initialLabel);
    } else {
      focus.requestFocus();
    }
  }

  void _later({bool force = false}) {
    tick ??= Timer(const Duration(milliseconds: 80), () {
      tick = null;
      if (!mounted) return;
      final stick = force || !scroll.hasClients || scroll.position.maxScrollExtent - scroll.position.pixels < 160;
      setState(() {});
      if (stick) {
        WidgetsBinding.instance.addPostFrameCallback((_) {
          if (scroll.hasClients) scroll.jumpTo(scroll.position.maxScrollExtent);
        });
      }
    });
  }

  @override
  void dispose() {
    tick?.cancel();
    sub?.cancel();
    session.close();
    input.dispose();
    focus.dispose();
    scroll.dispose();
    super.dispose();
  }

  String get _home => '/data/data/com.termux/files/home';

  /// The folder in words a beginner understands.
  String get _folder {
    final c = session.cwd;
    if (c == _home || c == '~') return 'Home folder';
    final i = c.lastIndexOf('/');
    return i >= 0 && i < c.length - 1 ? c.substring(i + 1) : c;
  }

  Future<void> _submit([String? text, String? label]) async {
    final cmd = (text ?? input.text).trimRight();
    if (cmd.trim().isEmpty || !ready || busy) return;
    input.clear();
    final shown = label ?? cmd;
    if (history.isEmpty || history.last != shown) history.add(shown);
    hIdx = history.length;
    final t = cmd.trim();
    if (t == 'clear') {
      setState(blocks.clear);
      return;
    }
    if (t == 'exit') {
      if (mounted) Navigator.of(context).maybePop();
      return;
    }
    if (t == 'help') {
      _help();
      return;
    }
    final b = _Block(shown, _folder);
    setState(() {
      blocks.add(b);
      active = b;
      busy = true;
    });
    _later(force: true);
    final sw = Stopwatch()..start();
    final rc = await session.run(cmd);
    if (!mounted) return;
    b.running = false;
    b.rc = rc;
    b.secs = (sw.elapsedMilliseconds / 1000).toStringAsFixed(1);
    b.hint = rc == 0 || rc == -1 ? null : _hint(rc, b.out.plain);
    b.cache = null;
    active = null;
    setState(() => busy = false);
    _later(force: true);
    focus.requestFocus();
  }

  /// A plain-words explanation for the common ways a command fails.
  String _hint(int rc, String out) {
    final o = out.toLowerCase();
    if (rc == 143 || rc == 130) return 'You stopped it.';
    if (rc == 127 || o.contains('command not found')) {
      return 'That command is not installed, or it is spelled wrong. Check the spelling. To install a tool, type: pkg install and its name.';
    }
    if (o.contains('no such file or directory')) return 'That file or folder is not there. Tap Ideas, then Show my files, to see what is here.';
    if (o.contains('permission denied')) return 'Android does not allow that here. Try it inside your project folder.';
    if (o.contains('could not resolve') || o.contains('unable to resolve') || o.contains('network is unreachable')) {
      return 'There is no internet. Check your Wi-Fi and try again.';
    }
    if (o.contains('not a directory')) return 'That is a file, not a folder.';
    if (o.contains('is a directory')) return 'That is a folder, not a file.';
    return 'That did not work (code $rc). Read the lines above for the reason.';
  }

  void _hist(int d) {
    if (history.isEmpty) return;
    hIdx = (hIdx + d).clamp(0, history.length);
    final t = hIdx >= history.length ? '' : history[hIdx];
    input.value = TextEditingValue(text: t, selection: TextSelection.collapsed(offset: t.length));
  }

  void _insert(String s) {
    final v = input.value;
    final sel = v.selection.isValid ? v.selection : TextSelection.collapsed(offset: v.text.length);
    final t = v.text.replaceRange(sel.start, sel.end, s);
    input.value = TextEditingValue(text: t, selection: TextSelection.collapsed(offset: sel.start + s.length));
    focus.requestFocus();
  }

  void _openIdeas() {
    focus.unfocus();
    showGlassSheet<void>(
      context: context,
      backgroundColor: C.sheet,
      isScrollControlled: true,
      shape: const RoundedRectangleBorder(borderRadius: BorderRadius.vertical(top: Radius.circular(18))),
      builder: (ctx) => SafeArea(
        child: ConstrainedBox(
          constraints: BoxConstraints(maxHeight: MediaQuery.of(ctx).size.height * .75),
          child: Column(mainAxisSize: MainAxisSize.min, crossAxisAlignment: CrossAxisAlignment.start, children: [
            Padding(
              padding: const EdgeInsets.fromLTRB(20, 18, 20, 4),
              child: Text('Ideas', style: inter(18, w: FontWeight.w700)),
            ),
            Padding(
              padding: const EdgeInsets.fromLTRB(20, 0, 20, 10),
              child: Text('Tap one and it appears in the box. Press the arrow to run it.', style: inter(13.5, c: C.muted)),
            ),
            Flexible(
              child: ListView(shrinkWrap: true, padding: const EdgeInsets.only(bottom: 8), children: [
                for (final (title, cmd) in _ideas)
                  InkWell(
                    onTap: () {
                      Navigator.of(ctx).pop();
                      _setInput(cmd);
                    },
                    child: Padding(
                      padding: const EdgeInsets.symmetric(horizontal: 20, vertical: 11),
                      child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
                        Text(title, style: inter(15.5, w: FontWeight.w600)),
                        const SizedBox(height: 2),
                        Text(cmd.startsWith('for ') ? 'checks Java, Kotlin, aapt2 and d8' : cmd.trimRight(),
                            maxLines: 1, overflow: TextOverflow.ellipsis, style: const TextStyle(fontFamily: 'monospace', fontSize: 12.5, color: C.muted)),
                      ]),
                    ),
                  ),
              ]),
            ),
          ]),
        ),
      ),
    );
  }

  void _setInput(String cmd) {
    input.value = TextEditingValue(text: cmd, selection: TextSelection.collapsed(offset: cmd.length));
    focus.requestFocus();
  }

  void _help() {
    focus.unfocus();
    showGlassSheet<void>(
      context: context,
      backgroundColor: C.sheet,
      shape: const RoundedRectangleBorder(borderRadius: BorderRadius.vertical(top: Radius.circular(18))),
      builder: (ctx) => SafeArea(
        child: Padding(
          padding: const EdgeInsets.fromLTRB(20, 18, 20, 18),
          child: Column(mainAxisSize: MainAxisSize.min, crossAxisAlignment: CrossAxisAlignment.start, children: [
            Text('How this works', style: inter(18, w: FontWeight.w700)),
            const SizedBox(height: 12),
            for (final l in const [
              'A terminal lets you give your phone written instructions, called commands.',
              'Type a command in the box and press the arrow. The result shows in a card.',
              'The folder you are in stays the same from one command to the next.',
              'Not sure what to type? Tap Ideas.',
              'While something is running, the arrow becomes a stop button. Tap it to stop.',
              'Press and hold on any text to copy it.',
            ])
              Padding(
                padding: const EdgeInsets.only(bottom: 9),
                child: Text(l, style: inter(14.5, c: const Color(0xFFCFD2D8))),
              ),
          ]),
        ),
      ),
    );
  }

  Future<void> _restart() async {
    await sub?.cancel();
    await session.close();
    blocks.clear();
    active = null;
    busy = false;
    await _start();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: _bg,
      body: SafeArea(
        child: Column(children: [
          _top(),
          Expanded(child: error != null ? _errorView() : _body()),
          if (error == null) _composer(),
        ]),
      ),
    );
  }

  Widget _top() {
    final sub = error != null ? 'Not connected' : (ready ? 'In: $_folder' : 'Starting...');
    return Container(
      height: 56,
      decoration: const BoxDecoration(color: _bg, border: Border(bottom: BorderSide(color: C.line))),
      child: Row(children: [
        SizedBox(
          width: 48,
          height: 48,
          child: IconButton(
            padding: EdgeInsets.zero,
            tooltip: 'Back',
            onPressed: () => Navigator.of(context).maybePop(),
            icon: const Icon(Icons.arrow_back_ios_new_rounded, size: 20, color: C.text),
          ),
        ),
        Expanded(
          child: Column(mainAxisAlignment: MainAxisAlignment.center, crossAxisAlignment: CrossAxisAlignment.start, children: [
            Text('Terminal', style: inter(16.5, w: FontWeight.w700)),
            Text(sub, maxLines: 1, overflow: TextOverflow.ellipsis, style: inter(12, c: error != null ? C.red : C.muted)),
          ]),
        ),
        GlassMenuButton<String>(
          color: C.sheet,
          tooltip: 'More',
          icon: const Icon(Icons.more_horiz_rounded, size: 24, color: C.text),
          onSelected: (v) {
            if (v == 'help') _help();
            if (v == 'clear') setState(blocks.clear);
            if (v == 'new') _restart();
          },
          itemBuilder: (_) => [
            PopupMenuItem(value: 'help', height: 44, child: Text('How this works', style: inter(14.5))),
            PopupMenuItem(value: 'clear', height: 44, child: Text('Clear the screen', style: inter(14.5))),
            PopupMenuItem(value: 'new', height: 44, child: Text('Start over', style: inter(14.5))),
          ],
        ),
        const SizedBox(width: 4),
      ]),
    );
  }

  Widget _errorView() => Center(
        child: Padding(
          padding: const EdgeInsets.all(28),
          child: Column(mainAxisSize: MainAxisSize.min, children: [
            Text('Can\u2019t connect to Termux', style: inter(18, w: FontWeight.w700)),
            const SizedBox(height: 10),
            Text(error!, textAlign: TextAlign.center, style: inter(14.5, c: C.muted)),
            const SizedBox(height: 22),
            SizedBox(
              width: double.infinity,
              height: 48,
              child: FilledButton(
                style: FilledButton.styleFrom(backgroundColor: C.tealBtn, foregroundColor: C.onTeal, shape: const StadiumBorder()),
                onPressed: () => Navigator.of(context).push(TermuxSetupPage.route()),
                child: Text('Set up on this phone', style: inter(15, w: FontWeight.w700, c: C.onTeal)),
              ),
            ),
            const SizedBox(height: 6),
            TextButton(onPressed: _restart, child: Text('Try again', style: inter(14.5, w: FontWeight.w600, c: C.teal))),
          ]),
        ),
      );

  Widget _body() {
    if (!ready) {
      return Center(
        child: Column(mainAxisSize: MainAxisSize.min, children: [
          const SizedBox(width: 22, height: 22, child: CircularProgressIndicator(strokeWidth: 2.4)),
          const SizedBox(height: 14),
          Text('Starting Termux...', style: inter(14, c: C.muted)),
        ]),
      );
    }
    return SelectionArea(
      child: ListView.builder(
        controller: scroll,
        padding: const EdgeInsets.fromLTRB(14, 16, 14, 16),
        itemCount: blocks.length + 1,
        itemBuilder: (_, i) {
          if (i == 0) return _introView();
          return _card(blocks[i - 1]);
        },
      ),
    );
  }

  Widget _introView() => Padding(
        padding: const EdgeInsets.fromLTRB(4, 0, 4, 16),
        child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
          Text('Type a command below and press the arrow to run it.', style: inter(14.5, c: const Color(0xFFCFD2D8))),
          const SizedBox(height: 4),
          Text('Not sure what to type? Tap Ideas.', style: inter(14.5, c: C.muted)),
          if (intro != null) ...[
            const SizedBox(height: 10),
            Text(intro!, style: inter(13.5, c: C.muted)),
          ],
        ]),
      );

  Widget _card(_Block b) {
    final failed = !b.running && b.rc != null && b.rc != 0;
    final text = b.out.plain.trim();
    final Widget status;
    if (b.running) {
      status = const SizedBox(width: 16, height: 16, child: CircularProgressIndicator(strokeWidth: 2, color: Color(0xFF9A9AA0)));
    } else if (failed) {
      status = const Icon(Icons.close_rounded, size: 19, color: C.red);
    } else {
      status = Icon(Icons.check_rounded, size: 19, color: C.teal);
    }
    return Container(
      margin: const EdgeInsets.only(bottom: 12),
      decoration: BoxDecoration(color: C.card, borderRadius: BorderRadius.circular(14), border: Border.all(color: C.line)),
      child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
        Padding(
          padding: const EdgeInsets.fromLTRB(14, 12, 14, 10),
          child: Row(crossAxisAlignment: CrossAxisAlignment.start, children: [
            Padding(padding: const EdgeInsets.only(top: 1), child: SizedBox(width: 20, height: 20, child: Center(child: status))),
            const SizedBox(width: 10),
            Expanded(
              child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
                Text(b.shown, style: const TextStyle(fontFamily: 'monospace', fontSize: 13.5, height: 1.35, fontWeight: FontWeight.w600, color: C.text)),
                const SizedBox(height: 2),
                Text(
                  b.running ? 'Running in ${b.where}...' : 'In ${b.where}  \u00B7  ${b.secs}s',
                  style: inter(11.5, c: C.muted),
                ),
              ]),
            ),
          ]),
        ),
        if (text.isNotEmpty)
          Container(
            width: double.infinity,
            padding: const EdgeInsets.fromLTRB(14, 10, 14, 10),
            decoration: const BoxDecoration(color: _inset, border: Border(top: BorderSide(color: C.line))),
            child: Text.rich(TextSpan(children: b.cache ??= _ansi(b.out.shown)), style: _mono),
          ),
        if (!b.running && text.isEmpty && !failed)
          Container(
            width: double.infinity,
            padding: const EdgeInsets.fromLTRB(14, 0, 14, 12),
            child: Text('Done. There was nothing to show.', style: inter(13, c: C.muted)),
          ),
        if (failed && b.hint != null)
          Container(
            width: double.infinity,
            padding: const EdgeInsets.fromLTRB(14, 10, 14, 12),
            decoration: BoxDecoration(border: text.isEmpty ? null : const Border(top: BorderSide(color: C.line))),
            child: Text(b.hint!, style: inter(13.5, c: const Color(0xFFE9B2AE))),
          ),
        if (!b.running && text.isNotEmpty)
          Align(
            alignment: Alignment.centerRight,
            child: TextButton(
              style: TextButton.styleFrom(minimumSize: const Size(64, 40), padding: const EdgeInsets.symmetric(horizontal: 14)),
              onPressed: () {
                Clipboard.setData(ClipboardData(text: text));
                ScaffoldMessenger.of(context)
                  ..hideCurrentSnackBar()
                  ..showSnackBar(SnackBar(content: Text('Copied', style: inter(14)), duration: const Duration(seconds: 1)));
              },
              child: Text('Copy', style: inter(13, w: FontWeight.w600, c: C.teal)),
            ),
          ),
      ]),
    );
  }

  Widget _key(String label, VoidCallback f, {String? tip}) => Padding(
        padding: const EdgeInsets.only(right: 6),
        child: Semantics(
          label: tip,
          button: true,
          child: Material(
            color: C.card,
            borderRadius: BorderRadius.circular(9),
            child: InkWell(
              borderRadius: BorderRadius.circular(9),
              onTap: f,
              child: Container(
                constraints: const BoxConstraints(minWidth: 44, minHeight: 40),
                alignment: Alignment.center,
                padding: const EdgeInsets.symmetric(horizontal: 12),
                child: Text(label, style: const TextStyle(fontFamily: 'monospace', fontSize: 14.5, color: C.text)),
              ),
            ),
          ),
        ),
      );

  Widget _composer() {
    return Container(
      decoration: const BoxDecoration(color: _bg, border: Border(top: BorderSide(color: C.line))),
      padding: const EdgeInsets.fromLTRB(10, 8, 10, 10),
      child: Column(mainAxisSize: MainAxisSize.min, children: [
        SizedBox(
          height: 40,
          child: ListView(scrollDirection: Axis.horizontal, children: [
            _key('\u2191', () => _hist(-1), tip: 'Previous command'),
            _key('\u2193', () => _hist(1), tip: 'Next command'),
            for (final s in ['/', '-', '~', '.', '|', '>', '"', '*']) _key(s, () => _insert(s)),
          ]),
        ),
        const SizedBox(height: 8),
        Row(crossAxisAlignment: CrossAxisAlignment.end, children: [
          SizedBox(
            height: 46,
            child: TextButton.icon(
              style: TextButton.styleFrom(foregroundColor: C.text, padding: const EdgeInsets.symmetric(horizontal: 10)),
              onPressed: ready ? _openIdeas : null,
              icon: const Icon(Icons.lightbulb_outline_rounded, size: 20),
              label: Text('Ideas', style: inter(14.5, w: FontWeight.w600)),
            ),
          ),
          const SizedBox(width: 4),
          Expanded(
            child: Container(
              constraints: const BoxConstraints(minHeight: 46),
              padding: const EdgeInsets.symmetric(horizontal: 14),
              decoration: BoxDecoration(color: C.field, borderRadius: BorderRadius.circular(23), border: Border.all(color: C.line)),
              child: TextField(
                controller: input,
                focusNode: focus,
                enabled: ready,
                // A normal keyboard: suggestions bar, autocorrect and the keyboard's own learning stay on, so
                // Android does not treat this as a secret field. Capitalization stays off so commands keep their case.
                keyboardType: TextInputType.text,
                autocorrect: true,
                enableSuggestions: true,
                enableIMEPersonalizedLearning: true,
                textCapitalization: TextCapitalization.none,
                textInputAction: TextInputAction.send,
                onSubmitted: (_) => _submit(),
                style: const TextStyle(fontFamily: 'monospace', fontSize: 14.5, color: C.text),
                cursorColor: C.teal,
                decoration: InputDecoration(
                  isDense: true,
                  border: InputBorder.none,
                  contentPadding: const EdgeInsets.symmetric(vertical: 14),
                  hintText: 'Type a command',
                  hintStyle: inter(14.5, c: C.muted),
                ),
              ),
            ),
          ),
          const SizedBox(width: 8),
          ValueListenableBuilder<TextEditingValue>(
            valueListenable: input,
            builder: (_, v, __) {
              final can = ready && v.text.trim().isNotEmpty;
              if (busy) {
                return _round(Icons.stop_rounded, 'Stop', session.cancel, C.red, Colors.white);
              }
              return _round(Icons.arrow_upward_rounded, 'Run', can ? () => _submit() : null, can ? C.tealBtn : C.card, can ? C.onTeal : C.muted);
            },
          ),
        ]),
      ]),
    );
  }

  Widget _round(IconData i, String tip, VoidCallback? f, Color bg, Color fg) => SizedBox(
        width: 46,
        height: 46,
        child: Material(
          color: bg,
          shape: const CircleBorder(),
          child: IconButton(padding: EdgeInsets.zero, tooltip: tip, onPressed: f, icon: Icon(i, size: 23, color: fg)),
        ),
      );
}

/// The text of one command's output: finished lines and the line being written. '\r' rewrites the line
/// (progress bars). Only the last lines are kept, so a huge output can't freeze the screen.
class _Term {
  final done = <String>[];
  String cur = '';
  bool _cr = false;
  bool cut = false;

  void add(String s) {
    var i = 0;
    while (i < s.length) {
      final c = s.codeUnitAt(i);
      if (c == 10) {
        _cr = false;
        done.add(cur);
        cur = '';
        i++;
      } else if (c == 13) {
        _cr = true;
        i++;
      } else {
        var j = i;
        while (j < s.length && s.codeUnitAt(j) != 10 && s.codeUnitAt(j) != 13) {
          j++;
        }
        if (_cr) {
          cur = '';
          _cr = false;
        }
        cur += s.substring(i, j);
        i = j;
      }
    }
    if (done.length > 600) {
      done.removeRange(0, done.length - 600);
      cut = true;
    }
  }

  /// All lines, with colors.
  String get shown => '${cut ? '(earlier lines hidden)\n' : ''}${[...done, cur].join('\n')}';

  /// All lines as plain text.
  String get plain => shown.replaceAll(_osc, '').replaceAll(_csi, '');
}

final _osc = RegExp('\x1B\\][^\x07\x1B]*(\x07|\x1B\\\\)');
final _csi = RegExp('\x1B\\[([0-9;?]*)([A-Za-z])');
const _pal = <Color>[
  Color(0xFF000000), Color(0xFFCD3131), Color(0xFF0DBC79), Color(0xFFE5E510),
  Color(0xFF2472C8), Color(0xFFBC3FBC), Color(0xFF11A8CD), Color(0xFFE5E5E5),
  Color(0xFF666666), Color(0xFFF14C4C), Color(0xFF23D18B), Color(0xFFF5F543),
  Color(0xFF3B8EEA), Color(0xFFD670D6), Color(0xFF29B8DB), Color(0xFFFFFFFF),
];

Color _c256(int n) {
  if (n < 16) return _pal[n];
  if (n >= 232) {
    final g = 8 + (n - 232) * 10;
    return Color.fromARGB(255, g, g, g);
  }
  final k = n - 16;
  int lv(int v) => v == 0 ? 0 : 55 + v * 40;
  return Color.fromARGB(255, lv(k ~/ 36), lv((k ~/ 6) % 6), lv(k % 6));
}

/// Colors (SGR) are drawn; every other escape code is dropped. Dark text colors are lifted so they stay readable.
List<InlineSpan> _ansi(String line) {
  line = line.replaceAll(_osc, '');
  final out = <InlineSpan>[];
  Color? fg;
  var bold = false;
  void put(String t) {
    if (t.isEmpty) return;
    out.add(TextSpan(text: t, style: TextStyle(color: fg, fontWeight: bold ? FontWeight.w700 : null)));
  }

  var at = 0;
  for (final m in _csi.allMatches(line)) {
    put(line.substring(at, m.start));
    at = m.end;
    if (m.group(2) != 'm') continue;
    final p = (m.group(1) ?? '').split(';').map((e) => int.tryParse(e) ?? 0).toList();
    if (p.isEmpty) p.add(0);
    for (var i = 0; i < p.length; i++) {
      final v = p[i];
      if (v == 0) {
        fg = null;
        bold = false;
      } else if (v == 1) {
        bold = true;
      } else if (v == 22) {
        bold = false;
      } else if (v >= 30 && v <= 37) {
        fg = v == 30 ? _pal[8] : _pal[v - 30];
      } else if (v >= 90 && v <= 97) {
        fg = _pal[v - 90 + 8];
      } else if (v == 39) {
        fg = null;
      } else if (v == 38 && i + 2 < p.length && p[i + 1] == 5) {
        fg = _c256(p[i + 2]);
        i += 2;
      } else if (v == 38 && i + 4 < p.length && p[i + 1] == 2) {
        fg = Color.fromARGB(255, p[i + 2] & 255, p[i + 3] & 255, p[i + 4] & 255);
        i += 4;
      }
    }
  }
  put(line.substring(at));
  return out;
}
