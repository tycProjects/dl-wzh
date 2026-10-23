import 'package:flutter/material.dart' hide Text;
import '../anim.dart';
import '../store.dart';
import '../theme.dart';
import '../tr.dart';
import 'code_editor.dart';

/// The Code tab: a calm landing page. The editor itself opens full screen from here
/// (no bottom bar in the way) and the back arrow returns to this page.
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

  @override
  Widget build(BuildContext context) {
    final p = CodeProject.i;
    return Center(
      child: Padding(
        padding: const EdgeInsets.fromLTRB(28, 0, 28, 40),
        child: Column(mainAxisSize: MainAxisSize.min, children: [
          Container(
            width: 84, height: 84,
            decoration: const BoxDecoration(color: C.card, shape: BoxShape.circle),
            child: const Icon(Icons.code_rounded, size: 40, color: C.teal),
          ),
          const SizedBox(height: 20),
          Text('Code editor', style: inter(24, w: FontWeight.w700)),
          const SizedBox(height: 8),
          Text(
            p.isOpen
                ? 'Your project is still open. Pick up where you left off.'
                : 'Open a zip with Kotlin, Java, Flutter or web code (HTML, CSS, JS), change the files, then build it.',
            textAlign: TextAlign.center,
            style: inter(15, c: C.muted, h: 1.4),
          ),
          if (p.isOpen) ...[
            const SizedBox(height: 18),
            Container(
              padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 12),
              decoration: BoxDecoration(color: C.card, borderRadius: BorderRadius.circular(18)),
              child: Row(mainAxisSize: MainAxisSize.min, children: [
                const Icon(Icons.folder_zip_outlined, size: 22, color: C.teal),
                const SizedBox(width: 10),
                Flexible(child: Text('${p.name}.zip \u00B7 ${p.files.length} files', maxLines: 1, overflow: TextOverflow.ellipsis, style: inter(15, w: FontWeight.w500))),
              ]),
            ),
          ],
          const SizedBox(height: 26),
          Pressable(
            scale: .95,
            onTap: _start,
            child: Container(
              height: 54, width: double.infinity, alignment: Alignment.center,
              decoration: BoxDecoration(color: C.tealBtn, borderRadius: BorderRadius.circular(27)),
              child: Text(p.isOpen ? 'Continue editing' : 'Start editing', style: inter(18, w: FontWeight.w700, c: const Color(0xFF0F2A24))),
            ),
          ),
        ]),
      ),
    );
  }
}
