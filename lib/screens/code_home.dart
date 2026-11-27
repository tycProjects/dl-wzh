import 'package:flutter/material.dart' hide Text;
import '../anim.dart';
import '../store.dart';
import '../theme.dart';
import '../tr.dart';
import 'code_editor.dart';
import 'code_terminal.dart';
import 'termux_setup_page.dart';

/// The Code tab, iOS style: large title, a "continue" card, quick actions and recent files.
/// The editor itself opens full screen from here and the back arrow returns to this page.
class CodeHome extends StatefulWidget {
  const CodeHome({super.key});
  @override
  State<CodeHome> createState() => _CodeHomeState();
}

class _CodeHomeState extends State<CodeHome> {
  @override
  void initState() {
    super.initState();
    ShellNav.tab.addListener(_refresh);
  }

  @override
  void dispose() {
    ShellNav.tab.removeListener(_refresh);
    super.dispose();
  }

  void _refresh() {
    if (mounted) setState(() {});
  }

  Future<void> _start() async {
    await Navigator.of(context).push(codeEditorRoute());
    _refresh();
  }

  Future<void> _terminal() async {
    await Navigator.of(context).push(CodeTerminalPage.route());
    _refresh();
  }

  Future<void> _setup() async {
    await Navigator.of(context).push(TermuxSetupPage.route());
    _refresh();
  }

  Future<void> _openFile(String path) async {
    openCodeFile(context, path, 1);
  }

  /// Files to list: the ones open in the editor last, then the first few of the project.
  List<String> _recent(CodeProject p) {
    final out = <String>[];
    for (final k in p.openPaths) {
      if (p.files.containsKey(k) && !out.contains(k)) out.add(k);
    }
    if (out.length < 3) {
      for (final k in p.files.keys) {
        if (!out.contains(k)) out.add(k);
        if (out.length >= 3) break;
      }
    }
    return out.take(3).toList();
  }

  (String, Color, Color) _badge(String path) {
    final ext = path.contains('.') ? path.split('.').last.toLowerCase() : '';
    switch (ext) {
      case 'dart': return ('DART', const Color(0xFF12303B), const Color(0xFF5AC8F5));
      case 'kt': return ('KT', const Color(0xFF2A2140), const Color(0xFFC792FF));
      case 'java': return ('JAVA', const Color(0xFF3A2414), const Color(0xFFFFA05A));
      case 'xml': return ('XML', const Color(0xFF14301F), const Color(0xFF5BD98A));
      case 'py': return ('PY', const Color(0xFF3A3014), const Color(0xFFF5D04A));
      case 'js': return ('JS', const Color(0xFF3A3414), const Color(0xFFF5D04A));
      case 'html': case 'htm': return ('HTML', const Color(0xFF3A1E14), const Color(0xFFFF8A5A));
      case 'css': return ('CSS', const Color(0xFF14233A), const Color(0xFF5A9BFF));
      case 'json': return ('JSON', const Color(0xFF2A2A2E), const Color(0xFFC7C7CC));
      default: return (ext.isEmpty ? 'FILE' : ext.toUpperCase().substring(0, ext.length > 4 ? 4 : ext.length), const Color(0xFF2A2A2E), const Color(0xFFC7C7CC));
    }
  }

  Widget _tile(IconData icon, Color color, String label, VoidCallback onTap) {
    return Expanded(
      child: Pressable(
        scale: .95,
        onTap: onTap,
        child: Container(
          padding: const EdgeInsets.fromLTRB(0, 14, 0, 10),
          decoration: BoxDecoration(color: C.card, borderRadius: BorderRadius.circular(18)),
          child: Column(children: [
            Icon(icon, size: 24, color: color),
            const SizedBox(height: 8),
            Text(label, style: inter(12, w: FontWeight.w500)),
          ]),
        ),
      ),
    );
  }

  Widget _continueCard(CodeProject p) {
    final changed = p.modified.length;
    final meta = '${p.files.length} files' + (changed > 0 ? ' · $changed changed' : '');
    return Container(
      padding: const EdgeInsets.all(18),
      decoration: BoxDecoration(
        color: C.card,
        borderRadius: BorderRadius.circular(26),
        border: Border.all(color: const Color(0xFF2C2C2E)),
      ),
      child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
        Row(children: [
          Container(
            width: 44, height: 44,
            decoration: BoxDecoration(color: C.tealBtn, borderRadius: BorderRadius.circular(12)),
            child: Icon(Icons.code_rounded, size: 24, color: C.onTeal),
          ),
          const SizedBox(width: 12),
          Expanded(
            child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
              Text(p.isOpen ? 'CONTINUE EDITING' : 'CODE EDITOR', style: inter(12, w: FontWeight.w600, c: C.teal)),
              const SizedBox(height: 2),
              Text(p.isOpen ? p.name : 'No project open', maxLines: 1, overflow: TextOverflow.ellipsis, style: inter(16, w: FontWeight.w600)),
            ]),
          ),
        ]),
        const SizedBox(height: 12),
        Text(
          p.isOpen ? meta : 'Open a zip with Kotlin, Java, Flutter or web code (HTML, CSS, JS), change the files, then build it.',
          style: inter(13, c: C.muted, h: 1.4),
        ),
        const SizedBox(height: 16),
        Pressable(
          scale: .96,
          onTap: _start,
          child: Container(
            height: 50, width: double.infinity, alignment: Alignment.center,
            decoration: BoxDecoration(color: C.tealBtn, borderRadius: BorderRadius.circular(25)),
            child: Text(p.isOpen ? 'Continue editing' : 'Start editing', style: inter(16, w: FontWeight.w700, c: C.onTeal)),
          ),
        ),
      ]),
    );
  }

  Widget _fileRow(String path, bool last) {
    final b = _badge(path);
    final parts = path.split('/');
    final name = parts.last;
    final dir = parts.length > 1 ? parts.sublist(0, parts.length - 1).join('/') : 'project root';
    return Pressable(
      scale: .985,
      onTap: () => _openFile(path),
      child: Container(
        padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 10),
        decoration: BoxDecoration(border: last ? null : const Border(bottom: BorderSide(color: Color(0xFF2C2C2E)))),
        child: Row(children: [
          Container(
            width: 36, height: 36, alignment: Alignment.center,
            decoration: BoxDecoration(color: b.$2, borderRadius: BorderRadius.circular(10)),
            child: Text(b.$1, style: inter(11, w: FontWeight.w700, c: b.$3)),
          ),
          const SizedBox(width: 12),
          Expanded(
            child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
              Text(name, maxLines: 1, overflow: TextOverflow.ellipsis, style: inter(15, w: FontWeight.w500)),
              Text(dir, maxLines: 1, overflow: TextOverflow.ellipsis, style: inter(12, c: C.muted)),
            ]),
          ),
          const Icon(Icons.chevron_right_rounded, size: 20, color: Color(0xFF6C6C72)),
        ]),
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    final p = CodeProject.i;
    final recent = p.isOpen ? _recent(p) : const <String>[];
    return SingleChildScrollView(
      padding: const EdgeInsets.fromLTRB(16, 12, 16, 150),
      child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
        Padding(padding: const EdgeInsets.symmetric(horizontal: 4), child: Text('Code', style: heading(34))),
        const SizedBox(height: 14),
        _continueCard(p),
        const SizedBox(height: 14),
        Row(children: [
          _tile(Icons.edit_note_rounded, C.teal, 'Editor', _start),
          const SizedBox(width: 10),
          _tile(Icons.terminal_rounded, const Color(0xFFFFB454), 'Terminal', _terminal),
          const SizedBox(width: 10),
          _tile(Icons.download_for_offline_outlined, const Color(0xFF5AA9FF), 'Set up', _setup),
        ]),
        if (recent.isNotEmpty) ...[
          const SizedBox(height: 22),
          Padding(padding: const EdgeInsets.symmetric(horizontal: 4), child: Text('Recent files', style: heading(20))),
          const SizedBox(height: 10),
          ClipRRect(
            borderRadius: BorderRadius.circular(20),
            child: Container(
              color: C.card,
              child: Column(children: [
                for (var i = 0; i < recent.length; i++) _fileRow(recent[i], i == recent.length - 1),
              ]),
            ),
          ),
        ],
      ]),
    );
  }
}
