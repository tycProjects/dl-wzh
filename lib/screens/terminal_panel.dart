import 'package:flutter/material.dart';
import 'package:xterm/xterm.dart';
import '../core/shell_session.dart';
import '../theme.dart';

/// A terminal view with a row of extra keys (Esc, Tab, Ctrl, arrows ...) that phone keyboards lack.
class TerminalPanel extends StatelessWidget {
  final ShellSession session;
  final String title;
  final VoidCallback? onClose;
  final VoidCallback? onToggleSize;
  final bool maximized;
  final bool showHeader;
  final bool autofocus;
  const TerminalPanel({
    super.key,
    required this.session,
    this.title = 'Terminal',
    this.onClose,
    this.onToggleSize,
    this.maximized = false,
    this.showHeader = true,
    this.autofocus = false,
  });

  @override
  Widget build(BuildContext context) {
    return Container(
      color: const Color(0xFF0C0C0C),
      child: Column(children: [
        if (showHeader)
          Container(
            height: 34,
            color: const Color(0xFF252526),
            padding: const EdgeInsets.only(left: 12),
            child: Row(children: [
              const Icon(Icons.terminal_rounded, size: 16, color: C.teal),
              const SizedBox(width: 8),
              Expanded(child: Text(title, style: inter(12.5, w: FontWeight.w600))),
              if (onToggleSize != null)
                IconButton(
                  visualDensity: VisualDensity.compact,
                  tooltip: maximized ? 'Smaller' : 'Bigger',
                  icon: Icon(maximized ? Icons.close_fullscreen_rounded : Icons.open_in_full_rounded, size: 17, color: C.muted),
                  onPressed: onToggleSize,
                ),
              if (onClose != null)
                IconButton(
                  visualDensity: VisualDensity.compact,
                  tooltip: 'Hide terminal',
                  icon: const Icon(Icons.keyboard_arrow_down_rounded, size: 22, color: C.muted),
                  onPressed: onClose,
                ),
            ]),
          ),
        Expanded(
          child: TerminalView(
            session.terminal,
            autofocus: autofocus,
            padding: const EdgeInsets.all(4),
            textStyle: TerminalStyle(fontSize: 12.5),
          ),
        ),
        _ExtraKeys(session),
      ]),
    );
  }
}

class _ExtraKeys extends StatelessWidget {
  final ShellSession s;
  const _ExtraKeys(this.s);

  Widget _k(String label, VoidCallback tap, {bool on = false}) => Padding(
        padding: const EdgeInsets.symmetric(horizontal: 2, vertical: 4),
        child: Material(
          color: on ? C.tealDeep : const Color(0xFF2A2A2C),
          borderRadius: BorderRadius.circular(6),
          child: InkWell(
            borderRadius: BorderRadius.circular(6),
            onTap: tap,
            child: Container(
              constraints: const BoxConstraints(minWidth: 40),
              height: 32,
              alignment: Alignment.center,
              padding: const EdgeInsets.symmetric(horizontal: 10),
              child: Text(label, style: inter(13, w: FontWeight.w600, c: on ? C.teal : C.text)),
            ),
          ),
        ),
      );

  @override
  Widget build(BuildContext context) {
    return Container(
      height: 40,
      color: const Color(0xFF1B1B1C),
      child: ListView(
        scrollDirection: Axis.horizontal,
        padding: const EdgeInsets.symmetric(horizontal: 4),
        children: [
          _k('Esc', () => s.write('\x1b')),
          _k('Tab', () => s.write('\t')),
          ValueListenableBuilder<bool>(valueListenable: s.ctrl, builder: (_, v, __) => _k('Ctrl', () => s.ctrl.value = !s.ctrl.value, on: v)),
          _k('↑', () => s.write('\x1b[A')),
          _k('↓', () => s.write('\x1b[B')),
          _k('←', () => s.write('\x1b[D')),
          _k('→', () => s.write('\x1b[C')),
          _k('^C', () => s.write('\x03')),
          _k('^D', () => s.write('\x04')),
          _k('|', () => s.write('|')),
          _k('/', () => s.write('/')),
          _k('-', () => s.write('-')),
          _k('~', () => s.write('~')),
          _k('\$', () => s.write('\$')),
          _k('Clear', () => s.write('\x0c')),
        ],
      ),
    );
  }
}

/// The terminal on its own screen.
class TerminalScreen extends StatelessWidget {
  const TerminalScreen({super.key});
  @override
  Widget build(BuildContext context) {
    final s = ShellSession.main..start();
    return Scaffold(
      backgroundColor: const Color(0xFF0C0C0C),
      appBar: AppBar(
        backgroundColor: const Color(0xFF252526),
        title: Text('Terminal', style: inter(16, w: FontWeight.w700)),
      ),
      body: SafeArea(child: TerminalPanel(session: s, showHeader: false, autofocus: true)),
    );
  }
}
