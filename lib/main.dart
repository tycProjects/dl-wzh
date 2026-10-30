import 'dart:async';

import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

void main() {
  WidgetsFlutterBinding.ensureInitialized();
  SystemChrome.setPreferredOrientations([DeviceOrientation.portraitUp]);
  runApp(const RecorderApp());
}

class RecorderApp extends StatelessWidget {
  const RecorderApp({super.key});

  @override
  Widget build(BuildContext context) => MaterialApp(
        debugShowCheckedModeBanner: false,
        title: 'Screen Recorder',
        theme: ThemeData(
          brightness: Brightness.dark,
          scaffoldBackgroundColor: const Color(0xFF111318),
          colorScheme: const ColorScheme.dark(primary: Color(0xFFE53935)),
          useMaterial3: true,
        ),
        home: const HomePage(),
      );
}

class Quality {
  final String key, title, detail;
  const Quality(this.key, this.title, this.detail);
}

const _qualities = <Quality>[
  Quality('auto', 'Auto (recommended)', 'Picks the best your phone can hold without lag'),
  Quality('smooth', 'Smooth', '720p • 30 fps • lightest on the phone'),
  Quality('balanced', 'Balanced', '1080p • 30 fps'),
  Quality('high', 'High', '1080p • 60 fps'),
  Quality('max', 'Max', 'Full screen resolution • 60 fps'),
];

class HomePage extends StatefulWidget {
  const HomePage({super.key});

  @override
  State<HomePage> createState() => _HomePageState();
}

class _HomePageState extends State<HomePage> with WidgetsBindingObserver {
  static const _ctl = MethodChannel('screenrecorder/control');
  static const _evt = EventChannel('screenrecorder/status');

  StreamSubscription<dynamic>? _sub;
  bool _recording = false;
  bool _busy = false;
  String _quality = 'auto';
  String _plan = '';
  String _tier = '';
  String _resolved = '';
  int _startedAt = 0;
  String _liveSummary = '';

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addObserver(this);
    _init();
  }

  @override
  void dispose() {
    WidgetsBinding.instance.removeObserver(this);
    _sub?.cancel();
    super.dispose();
  }

  @override
  void didChangeAppLifecycleState(AppLifecycleState state) {
    if (state == AppLifecycleState.resumed) _refreshStatus();
  }

  Future<void> _init() async {
    _quality = await _ctl.invokeMethod<String>('getQuality') ?? 'auto';
    _sub = _evt.receiveBroadcastStream().listen(_onEvent);
    await _refreshStatus();
    await _refreshPlan();
  }

  Future<void> _refreshStatus() async {
    final s = await _ctl.invokeMapMethod<String, dynamic>('status');
    if (s != null && mounted) _apply(s);
  }

  Future<void> _refreshPlan() async {
    final p = await _ctl
        .invokeMapMethod<String, dynamic>('plan', {'quality': _quality});
    if (p == null || !mounted) return;
    setState(() {
      _plan = p['summary'] as String? ?? '';
      _tier = p['tier'] as String? ?? '';
      _resolved = p['resolved'] as String? ?? '';
    });
  }

  void _apply(Map<dynamic, dynamic> m) {
    setState(() {
      _recording = m['recording'] == true;
      _startedAt = (m['startedAt'] as num?)?.toInt() ?? 0;
      _liveSummary = m['summary'] as String? ?? '';
    });
  }

  void _onEvent(dynamic e) {
    if (e is! Map) return;
    _apply(e);
    final err = e['error'] as String?;
    final saved = e['saved'] as String?;
    if (err != null) _toast(err);
    if (saved != null) _toast('Saved to Movies/ScreenRecorder/$saved');
  }

  void _toast(String msg) {
    if (!mounted) return;
    ScaffoldMessenger.of(context)
      ..hideCurrentSnackBar()
      ..showSnackBar(SnackBar(content: Text(msg)));
  }

  Future<void> _toggle() async {
    if (_busy) return;
    setState(() => _busy = true);
    try {
      if (_recording) {
        await _ctl.invokeMethod('stop');
      } else {
        final ok = await _ctl
            .invokeMethod<bool>('start', {'quality': _quality}) ?? false;
        if (!ok) _toast('Screen capture permission denied');
      }
    } finally {
      if (mounted) setState(() => _busy = false);
    }
  }

  Future<void> _pick(String key) async {
    if (_recording) return;
    setState(() => _quality = key);
    await _ctl.invokeMethod('setQuality', {'quality': key});
    await _refreshPlan();
  }

  @override
  Widget build(BuildContext context) {
    final rec = _recording;
    return Scaffold(
      body: SafeArea(
        child: Padding(
          padding: const EdgeInsets.symmetric(horizontal: 20),
          child: Column(
            children: [
              const SizedBox(height: 28),
              const Text('Screen Recorder',
                  style: TextStyle(fontSize: 24, fontWeight: FontWeight.bold)),
              const SizedBox(height: 8),
              rec
                  ? _Elapsed(startedAt: _startedAt)
                  : const Text('Ready to record',
                      style: TextStyle(color: Color(0xFF9AA0A6), fontSize: 16)),
              const SizedBox(height: 4),
              Text(
                rec ? _liveSummary : _plan,
                style: const TextStyle(color: Color(0xFF9AA0A6), fontSize: 13),
              ),
              const Spacer(),
              GestureDetector(
                onTap: _toggle,
                child: Container(
                  width: 168,
                  height: 168,
                  decoration: BoxDecoration(
                    shape: BoxShape.circle,
                    color: rec ? const Color(0xFF3F51B5) : const Color(0xFFE53935),
                  ),
                  alignment: Alignment.center,
                  child: Text(rec ? 'STOP' : 'REC',
                      style: const TextStyle(
                          fontSize: 22,
                          fontWeight: FontWeight.bold,
                          color: Colors.white)),
                ),
              ),
              const Spacer(),
              for (final q in _qualities)
                _QualityTile(
                  q: q,
                  selected: q.key == _quality,
                  enabled: !rec,
                  extra: q.key == 'auto' && _tier.isNotEmpty
                      ? 'Device class: $_tier → $_resolved'
                      : null,
                  onTap: () => _pick(q.key),
                ),
              const SizedBox(height: 10),
              const Text(
                'Android shows its own recording indicator while capturing. '
                'It is drawn by the system and apps cannot hide it.',
                textAlign: TextAlign.center,
                style: TextStyle(color: Color(0xFF5F6368), fontSize: 11),
              ),
              const SizedBox(height: 14),
            ],
          ),
        ),
      ),
    );
  }
}

class _QualityTile extends StatelessWidget {
  const _QualityTile({
    required this.q,
    required this.selected,
    required this.enabled,
    required this.onTap,
    this.extra,
  });

  final Quality q;
  final bool selected, enabled;
  final VoidCallback onTap;
  final String? extra;

  @override
  Widget build(BuildContext context) {
    return Opacity(
      opacity: enabled ? 1 : 0.5,
      child: InkWell(
        onTap: enabled ? onTap : null,
        borderRadius: BorderRadius.circular(10),
        child: Padding(
          padding: const EdgeInsets.symmetric(vertical: 7, horizontal: 6),
          child: Row(
            children: [
              Icon(
                selected ? Icons.radio_button_checked : Icons.radio_button_off,
                size: 20,
                color: selected ? const Color(0xFFE53935) : const Color(0xFF9AA0A6),
              ),
              const SizedBox(width: 12),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(q.title, style: const TextStyle(fontSize: 14.5)),
                    Text(extra ?? q.detail,
                        style: const TextStyle(
                            color: Color(0xFF9AA0A6), fontSize: 11.5)),
                  ],
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}

/// Own ticker so only this tiny widget rebuilds each second.
class _Elapsed extends StatefulWidget {
  const _Elapsed({required this.startedAt});
  final int startedAt;

  @override
  State<_Elapsed> createState() => _ElapsedState();
}

class _ElapsedState extends State<_Elapsed> {
  Timer? _t;

  @override
  void initState() {
    super.initState();
    _t = Timer.periodic(const Duration(seconds: 1), (_) => setState(() {}));
  }

  @override
  void dispose() {
    _t?.cancel();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final ms = widget.startedAt == 0
        ? 0
        : DateTime.now().millisecondsSinceEpoch - widget.startedAt;
    final int s = (ms ~/ 1000).clamp(0, 359999).toInt();
    String two(int v) => v.toString().padLeft(2, '0');
    return Text('● Recording  ${two(s ~/ 3600)}:${two(s % 3600 ~/ 60)}:${two(s % 60)}',
        style: const TextStyle(color: Color(0xFFE53935), fontSize: 16));
  }
}
