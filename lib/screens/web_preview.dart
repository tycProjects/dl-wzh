import 'dart:async';
import 'dart:convert';
import 'dart:io';
import 'package:flutter/material.dart' hide Text;
import 'package:url_launcher/url_launcher.dart';
import 'package:webview_flutter/webview_flutter.dart';
import '../theme.dart';
import '../tr.dart';

const _host = 'preview.local';

const _mimes = {
  'png': 'image/png', 'jpg': 'image/jpeg', 'jpeg': 'image/jpeg', 'gif': 'image/gif', 'webp': 'image/webp', 'svg': 'image/svg+xml', 'ico': 'image/x-icon',
  'mp3': 'audio/mpeg', 'ogg': 'audio/ogg', 'wav': 'audio/wav', 'mp4': 'video/mp4', 'webm': 'video/webm',
  'html': 'text/html; charset=utf-8', 'htm': 'text/html; charset=utf-8', 'css': 'text/css; charset=utf-8',
  'js': 'text/javascript; charset=utf-8', 'mjs': 'text/javascript; charset=utf-8', 'json': 'application/json; charset=utf-8',
  'txt': 'text/plain; charset=utf-8', 'xml': 'application/xml; charset=utf-8', 'wasm': 'application/wasm', 'map': 'application/json',
  'woff': 'font/woff', 'woff2': 'font/woff2', 'ttf': 'font/ttf', 'otf': 'font/otf',
};

/// Catches console output and errors from the page and sends them to the Console panel.
const _hook = "<script>(function(){var s=function(k,a){try{Console.postMessage(JSON.stringify([k,[].slice.call(a).map(function(x){try{return typeof x==='object'?JSON.stringify(x):String(x)}catch(e){return String(x)}}).join(' ')]))}catch(e){}};"
    "['log','info','warn','error'].forEach(function(k){var o=console[k];console[k]=function(){s(k,arguments);if(o)o.apply(console,arguments)}});"
    "window.addEventListener('error',function(e){s('error',[e.message+(e.lineno?' (line '+e.lineno+')':'')])});"
    "window.addEventListener('unhandledrejection',function(e){s('error',['Promise: '+e.reason])})})();</script>";

String _ext(String p) => p.contains('.') ? p.split('.').last.toLowerCase() : '';

/// Resolves [ref] (as written in the file [from]) to a path in the project, or null for links that leave it.
String? _resolve(String from, String ref) {
  var r = ref.trim();
  if (r.isEmpty || r.startsWith('#') || RegExp(r'^[a-z][a-z0-9+.\-]*:', caseSensitive: false).hasMatch(r) || r.startsWith('//')) return null;
  r = r.split('#').first.split('?').first;
  try {
    r = Uri.decodeFull(r);
  } catch (_) {}
  final parts = r.startsWith('/') ? <String>[] : (from.split('/')..removeLast());
  for (final s in r.split('/')) {
    if (s == '..') {
      if (parts.isNotEmpty) parts.removeLast();
    } else if (s != '.' && s.isNotEmpty) {
      parts.add(s);
    }
  }
  return parts.join('/');
}

String _attr(String tag, String name) {
  final m = RegExp('\\b$name\\s*=\\s*(?:"([^"]*)"|\'([^\']*)\')', caseSensitive: false).firstMatch(tag);
  return m == null ? '' : (m.group(1) ?? m.group(2) ?? '');
}

String _text(Map<String, List<int>> f, String k) => utf8.decode(f[k]!, allowMalformed: true);

String? _dataUri(Map<String, List<int>> f, String path) {
  final b = f[path];
  final mime = _mimes[_ext(path)];
  if (b == null || mime == null || b.length > 4 * 1024 * 1024) return null;
  return 'data:$mime;base64,${base64Encode(b)}';
}

/// Turns the project's page into one self-contained html string: styles, scripts and images are put inline.
String buildPreviewHtml(Map<String, List<int>> files, String entry) {
  var html = _text(files, entry);

  // <link rel="stylesheet" href="x.css">  ->  <style>…</style>
  html = html.replaceAllMapped(RegExp(r'<link\b[^>]*>', caseSensitive: false), (m) {
    final tag = m.group(0)!;
    if (!_attr(tag, 'rel').toLowerCase().contains('stylesheet')) return tag;
    final to = _resolve(entry, _attr(tag, 'href'));
    if (to == null || !files.containsKey(to)) return tag;
    var css = _text(files, to);
    css = css.replaceAllMapped(RegExp(r'''url\(\s*["']?([^"')]+)["']?\s*\)'''), (u) {
      final ap = _resolve(to, u.group(1)!);
      final d = ap == null ? null : _dataUri(files, ap);
      return d == null ? u.group(0)! : 'url("$d")';
    });
    return '<style>\n$css\n</style>';
  });

  // <script src="x.js"></script>  ->  <script>…</script>
  html = html.replaceAllMapped(RegExp(r'<script\b([^>]*)>\s*</script>', caseSensitive: false), (m) {
    final attrs = m.group(1)!;
    final src = _attr(attrs, 'src');
    if (src.isEmpty) return m.group(0)!;
    final to = _resolve(entry, src);
    if (to == null || !files.containsKey(to)) return m.group(0)!;
    final keep = attrs.replaceAll(RegExp('\\bsrc\\s*=\\s*(?:"[^"]*"|\'[^\']*\')', caseSensitive: false), '');
    return '<script$keep>\n${_text(files, to).replaceAll('</script', r'<\/script')}\n</script>';
  });

  // images, audio, video, fonts: src="…" / poster="…"
  html = html.replaceAllMapped(RegExp(r'''\b(src|poster)\s*=\s*(["'])(.*?)\2''', caseSensitive: false), (m) {
    final to = _resolve(entry, m.group(3)!);
    final d = to == null ? null : _dataUri(files, to);
    return d == null ? m.group(0)! : '${m.group(1)}=${m.group(2)}$d${m.group(2)}';
  });

  // the console hook goes first, so it sees everything the page's own scripts do
  final head = RegExp(r'<head[^>]*>', caseSensitive: false).firstMatch(html);
  if (head != null) {
    html = html.replaceRange(head.end, head.end, _hook);
  } else {
    html = '$_hook$html';
  }
  return html;
}


/// A tiny web server on this phone (127.0.0.1 only) that serves the project's files, so the preview runs on a real
/// http:// address like it would in Chrome: localStorage, fetch, ES modules and relative links all behave.
class PreviewServer {
  final Map<String, List<int>> files;
  final void Function(String path) onMissing;
  HttpServer? _s;
  PreviewServer(this.files, this.onMissing);

  int get port => _s!.port;
  String url(String path) => 'http://127.0.0.1:$port/${path.split('/').map(Uri.encodeComponent).join('/')}';

  Future<void> start() async {
    _s = await HttpServer.bind(InternetAddress.loopbackIPv4, 0);
    _s!.listen((req) async {
      try {
        var to = Uri.decodeFull(req.uri.path).replaceFirst(RegExp(r'^/+'), '');
        if (to.isEmpty || to.endsWith('/')) to = '${to}index.html';
        final res = req.response;
        var bytes = files[to];
        if (bytes == null && files.containsKey('$to/index.html')) bytes = files['$to/index.html'];
        if (bytes == null) {
          if (to != 'favicon.ico') onMissing(to);
          res.statusCode = 404;
          res.headers.contentType = ContentType.text;
          res.write('Not found: $to');
        } else {
          final ext = _ext(to);
          final code = const {'html', 'htm', 'css', 'js', 'mjs', 'json'}.contains(ext);
          res.headers.set('Cache-Control', code ? 'no-store' : 'max-age=3600'); // the project is a fixed snapshot while Preview is open
          res.headers.set('Content-Type', _mimes[ext] ?? 'application/octet-stream');
          if (ext == 'html' || ext == 'htm') {
            var html = utf8.decode(bytes, allowMalformed: true);
            final head = RegExp(r'<head[^>]*>', caseSensitive: false).firstMatch(html);
            html = head != null ? html.replaceRange(head.end, head.end, _hook) : '$_hook$html';
            bytes = utf8.encode(html);
          }
          res.headers.contentLength = bytes.length;
          res.add(bytes);
        }
        await res.close();
      } catch (_) {
        try {
          await req.response.close();
        } catch (_) {}
      }
    });
  }

  Future<void> stop() async => _s?.close(force: true);
}

/// Shows the project's page the way the built app would, live from what is typed in the editor.
class WebPreviewPage extends StatefulWidget {
  final Map<String, List<int>> files;
  final String entry;
  const WebPreviewPage({super.key, required this.files, required this.entry});
  @override
  State<WebPreviewPage> createState() => _WebPreviewPageState();
}

class _WebPreviewPageState extends State<WebPreviewPage> {
  late final WebViewController web;
  late String path = widget.entry;
  final logs = <(String, String)>[];
  bool consoleOpen = false, loading = true;
  PreviewServer? server;
  bool covered = true; // dark sheet over the page while it loads, fades away when ready
  final _pending = <(String, String)>[];
  Timer? _flush;
  late final Widget _view = RepaintBoundary(child: WebViewWidget(controller: web)); // built once, never rebuilt by console updates

  int get errors => logs.where((l) => l.$1 == 'error').length;

  @override
  void initState() {
    super.initState();
    web = WebViewController()
      ..setJavaScriptMode(JavaScriptMode.unrestricted)
      ..setBackgroundColor(Colors.white)
      ..addJavaScriptChannel('Console', onMessageReceived: (m) {
        try {
          final a = jsonDecode(m.message) as List;
          _pending.add(('${a[0]}', '${a[1]}'));
          _flush ??= Timer(const Duration(milliseconds: 150), () {
            _flush = null;
            if (!mounted) return;
            setState(() {
              logs.addAll(_pending);
              _pending.clear();
              if (logs.length > 300) logs.removeRange(0, logs.length - 300);
            });
          });
        } catch (_) {}
      })
      ..setNavigationDelegate(NavigationDelegate(
        onPageFinished: (_) {
          if (mounted) setState(() { loading = false; covered = false; });
        },
        onUrlChange: (c) {
          final u = Uri.tryParse(c.url ?? '');
          final srv = server;
          if (u != null && srv != null && u.host == '127.0.0.1' && u.port == srv.port && mounted) {
            var to = Uri.decodeFull(u.path).replaceFirst(RegExp(r'^/+'), '');
            if (to.isEmpty || to.endsWith('/')) to = '${to}index.html';
            if (to != path) setState(() { path = to; logs.clear(); });
          }
        },
        onNavigationRequest: (r) {
          final u = Uri.tryParse(r.url);
          if (u == null) return NavigationDecision.prevent;
          if (u.scheme == 'about' || u.scheme == 'data') return NavigationDecision.navigate;
          final srv = server;
          if (srv != null && u.host == '127.0.0.1' && u.port == srv.port) return NavigationDecision.navigate; // stays in the project
          if (u.host == _host) {
            var to = Uri.decodeFull(u.path).replaceFirst(RegExp(r'^/'), '');
            if (to.isEmpty || to.endsWith('/')) to = '${to}index.html';
            // a link to a spot on the same page just scrolls
            if (to == path && u.fragment.isNotEmpty) return NavigationDecision.navigate;
            if (widget.files.containsKey(to) && const {'html', 'htm'}.contains(_ext(to))) {
              _open(to);
            } else {
              _note('warn', 'That link goes to "$to", which isn\u2019t in the project.');
            }
          }
          return NavigationDecision.prevent; // the preview never leaves the project
        },
      ));
    _boot();
  }

  Future<void> _boot() async {
    try {
      final srv = PreviewServer(widget.files, (p) => _note('warn', 'Not found: $p'));
      await srv.start();
      if (!mounted) {
        await srv.stop();
        return;
      }
      server = srv;
    } catch (_) {
      server = null; // falls back to the inline preview below
    }
    await _afterTransition();
    if (mounted) _open(path);
  }

  /// Loading a page while the screen is still sliding in makes the slide stutter, so wait for it to land.
  Future<void> _afterTransition() async {
    if (!mounted) return;
    final a = ModalRoute.of(context)?.animation;
    if (a == null || a.isCompleted) return;
    final c = Completer<void>();
    void l(AnimationStatus st) {
      if (st == AnimationStatus.completed && !c.isCompleted) c.complete();
    }
    a.addStatusListener(l);
    await c.future.timeout(const Duration(milliseconds: 900), onTimeout: () {});
    a.removeStatusListener(l);
  }

  @override
  void dispose() {
    _flush?.cancel();
    server?.stop();
    super.dispose();
  }

  Future<void> _inChrome() async {
    final srv = server;
    if (srv == null) return _note('warn', 'The preview server is not running.');
    final ok = await launchUrl(Uri.parse(srv.url(path)), mode: LaunchMode.externalApplication);
    if (!ok) _note('warn', 'Couldn\u2019t open Chrome.');
  }

  void _note(String k, String t) {
    if (mounted) setState(() => logs.add((k, t)));
  }

  void _open(String p) {
    if (!widget.files.containsKey(p)) {
      _note('error', 'No file called $p.');
      return;
    }
    setState(() {
      path = p;
      loading = true;
      covered = true;
      logs.clear();
    });
    Timer(const Duration(seconds: 6), () {
      if (mounted && covered) setState(() => covered = false); // never leave the page hidden if a load stalls
    });
    final srv = server;
    if (srv != null) {
      web.loadRequest(Uri.parse(srv.url(p)));
      return;
    }
    final dir = p.contains('/') ? '${p.substring(0, p.lastIndexOf('/'))}/' : '';
    web.loadHtmlString(buildPreviewHtml(widget.files, p), baseUrl: 'https://$_host/$dir');
  }

  Widget _btn(IconData i, String tip, VoidCallback f, {Color c = C.text}) => SizedBox(
        width: 40, height: 40,
        child: IconButton(padding: EdgeInsets.zero, tooltip: tip, onPressed: f, icon: Icon(i, size: 22, color: c)),
      );

  Color _kc(String k) => k == 'error' ? const Color(0xFFFF8A80) : (k == 'warn' ? const Color(0xFFFFD54F) : C.text);

  Widget _consoleList() => logs.isEmpty
      ? Center(child: Text('Nothing printed yet. console.log shows up here.', style: inter(13, c: C.muted)))
      : ListView.builder(
          padding: const EdgeInsets.all(12),
          itemCount: logs.length,
          itemBuilder: (_, i) => Padding(
            padding: const EdgeInsets.only(bottom: 6),
            child: SelectableText(logs[i].$2, style: TextStyle(fontFamily: 'monospace', fontSize: 12.5, height: 1.35, color: _kc(logs[i].$1))),
          ),
        );

  @override
  Widget build(BuildContext context) {
    final bad = errors;
    return Scaffold(
      backgroundColor: C.bg,
      body: SafeArea(
        child: Column(children: [
          SizedBox(
            height: 52,
            child: Row(children: [
              _btn(Icons.arrow_back_ios_new_rounded, 'Back', () => Navigator.of(context).maybePop()),
              Expanded(
                child: Column(mainAxisAlignment: MainAxisAlignment.center, crossAxisAlignment: CrossAxisAlignment.start, children: [
                  Text('Preview', style: inter(16, w: FontWeight.w700)),
                  Text(path, maxLines: 1, overflow: TextOverflow.ellipsis, style: inter(11.5, c: C.muted)),
                ]),
              ),
              if (loading) const Padding(padding: EdgeInsets.only(right: 8), child: SizedBox(width: 16, height: 16, child: CircularProgressIndicator(strokeWidth: 2, color: C.teal))),
              _btn(Icons.open_in_new_rounded, 'Open in Chrome', _inChrome),
              _btn(Icons.refresh_rounded, 'Reload', () => _open(path), c: C.teal),
            ]),
          ),
          Expanded(
            child: ClipRect(
              child: Stack(children: [
                Positioned.fill(child: _view),
                Positioned.fill(
                  child: IgnorePointer(
                    ignoring: !covered,
                    child: AnimatedOpacity(
                      opacity: covered ? 1 : 0,
                      duration: Duration(milliseconds: covered ? 0 : 240),
                      curve: Curves.easeOut,
                      child: const ColoredBox(color: C.bg),
                    ),
                  ),
                ),
                Positioned(
                  left: 0, right: 0, bottom: 0,
                  child: AnimatedContainer(
                    duration: const Duration(milliseconds: 240),
                    curve: Curves.easeOutCubic,
                    height: consoleOpen ? 190 : 0,
                    width: double.infinity,
                    color: const Color(0xFF0E0E0F),
                    child: consoleOpen ? _consoleList() : null,
                  ),
                ),
              ]),
            ),
          ),
          GestureDetector(
            onTap: () => setState(() => consoleOpen = !consoleOpen),
            child: Container(
              height: 44,
              color: const Color(0xFF161618),
              padding: const EdgeInsets.symmetric(horizontal: 16),
              child: Row(children: [
                Icon(bad > 0 ? Icons.error_outline_rounded : Icons.terminal_rounded, size: 19, color: bad > 0 ? const Color(0xFFFF8A80) : C.muted),
                const SizedBox(width: 10),
                Expanded(child: Text(bad > 0 ? 'Console \u00B7 $bad ${bad == 1 ? 'error' : 'errors'}' : 'Console \u00B7 ${logs.length}', style: inter(14, w: FontWeight.w600, c: bad > 0 ? const Color(0xFFFF8A80) : C.text))),
                if (logs.isNotEmpty && consoleOpen)
                  GestureDetector(onTap: () => setState(logs.clear), child: Padding(padding: const EdgeInsets.only(right: 12), child: Text('Clear', style: inter(13, c: C.teal)))),
                Icon(consoleOpen ? Icons.keyboard_arrow_down_rounded : Icons.keyboard_arrow_up_rounded, color: C.muted),
              ]),
            ),
          ),
        ]),
      ),
    );
  }
}
