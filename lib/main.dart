import 'dart:io';
import 'dart:typed_data';
import 'package:flutter/material.dart';
import 'package:gal/gal.dart';
import 'package:image_picker/image_picker.dart';
import 'package:path_provider/path_provider.dart';
import 'package:share_plus/share_plus.dart';
import 'ai_engine.dart';
import 'enhancer.dart';

void main() => runApp(const UhdApp());

class UhdApp extends StatelessWidget {
  const UhdApp({super.key});
  @override
  Widget build(BuildContext context) {
    final scheme = ColorScheme.fromSeed(
        seedColor: const Color(0xFF7C5CFF), brightness: Brightness.dark);
    return MaterialApp(
      title: 'UHD Enhancer',
      debugShowCheckedModeBanner: false,
      theme: ThemeData(
          colorScheme: scheme,
          useMaterial3: true,
          scaffoldBackgroundColor: const Color(0xFF0B0B12)),
      home: const HomeScreen(),
    );
  }
}

const _tools = <(String, String, IconData, int)>[
  ('AI Enhance', 'Best for most photos', Icons.auto_awesome, 0),
  ('Portrait', 'Natural, brighter faces', Icons.face_retouching_natural, 1),
  ('Sharp', 'Crisp edges and detail', Icons.hd, 2),
  ('Vivid', 'Rich color and contrast', Icons.hdr_strong, 3),
  ('Clean Upscale', 'Bigger, colors unchanged', Icons.zoom_out_map, 4),
];

class HomeScreen extends StatelessWidget {
  const HomeScreen({super.key});

  Future<void> _open(BuildContext context, int preset) async {
    final f = await ImagePicker().pickImage(source: ImageSource.gallery);
    if (f == null || !context.mounted) return;
    final bytes = await f.readAsBytes();
    if (!context.mounted) return;
    Navigator.push(context,
        MaterialPageRoute(builder: (_) => EditorScreen(bytes: bytes, preset: preset)));
  }

  @override
  Widget build(BuildContext context) {
    final cs = Theme.of(context).colorScheme;
    return Scaffold(
      body: SafeArea(
        child: Padding(
          padding: const EdgeInsets.all(20),
          child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
            const SizedBox(height: 8),
            Text('UHD Enhancer',
                style: Theme.of(context)
                    .textTheme
                    .headlineMedium
                    ?.copyWith(fontWeight: FontWeight.w800)),
            const SizedBox(height: 4),
            Text('Pick a tool, then choose a photo.',
                style: TextStyle(color: cs.onSurfaceVariant)),
            const SizedBox(height: 20),
            Expanded(
              child: GridView.count(
                crossAxisCount: 2,
                mainAxisSpacing: 14,
                crossAxisSpacing: 14,
                childAspectRatio: 1.05,
                children: [
                  for (final t in _tools)
                    InkWell(
                      borderRadius: BorderRadius.circular(22),
                      onTap: () => _open(context, t.$4),
                      child: Ink(
                        decoration: BoxDecoration(
                          borderRadius: BorderRadius.circular(22),
                          gradient: LinearGradient(
                              begin: Alignment.topLeft,
                              end: Alignment.bottomRight,
                              colors: [cs.primaryContainer, cs.surfaceContainerHigh]),
                        ),
                        padding: const EdgeInsets.all(16),
                        child: Column(
                            mainAxisAlignment: MainAxisAlignment.spaceBetween,
                            crossAxisAlignment: CrossAxisAlignment.start,
                            children: [
                              Icon(t.$3, size: 34, color: cs.primary),
                              Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
                                Text(t.$1,
                                    style: const TextStyle(
                                        fontSize: 17, fontWeight: FontWeight.w700)),
                                const SizedBox(height: 2),
                                Text(t.$2,
                                    style: TextStyle(
                                        fontSize: 12, color: cs.onSurfaceVariant)),
                              ]),
                            ]),
                      ),
                    ),
                ],
              ),
            ),
          ]),
        ),
      ),
    );
  }
}

class EditorScreen extends StatefulWidget {
  final Uint8List bytes;
  final int preset;
  const EditorScreen({super.key, required this.bytes, required this.preset});
  @override
  State<EditorScreen> createState() => _EditorScreenState();
}

class _EditorScreenState extends State<EditorScreen> {
  late int _preset = widget.preset;
  int _quality = 0, _detail = 1; // detail: 0 soft, 1 normal, 2 strong
  int _w = 0, _h = 0;
  EnhanceResult? _result;
  bool _busy = false, _aiReady = false, _useAi = true, _usedAi = false;

  @override
  void initState() {
    super.initState();
    AiEngine.available().then((ok) {
      if (mounted) setState(() => _aiReady = ok);
    });
    decodeImageFromList(widget.bytes).then((im) {
      if (mounted) setState(() { _w = im.width; _h = im.height; });
    });
  }

  void _toast(String m) =>
      ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(m)));

  String _sizeText(int qi) {
    if (_w == 0) return '';
    final s = scaleFor(qualities[qi], _w, _h);
    if (s <= 1.0) return 'Same size';
    return '${(_w * s).round()}×${(_h * s).round()}';
  }

  Future<void> _run() async {
    if (_w == 0) return;
    setState(() => _busy = true);
    try {
      final p = presets[_preset];
      const detailMul = [0.6, 1.0, 1.5];
      final scale = scaleFor(qualities[_quality], _w, _h);
      var src = widget.bytes;
      var rel = scale;
      var ai = false;
      // The AI model is for photos that need to get bigger (up to about 1 megapixel).
      if (_useAi && _aiReady && scale > 1.02 && _w * _h <= 1000000) {
        final clean = await prepareForAi(widget.bytes);
        final up = await AiEngine.upscale4x(clean);
        if (up != null) {
          src = up;
          rel = scale / 4;
          ai = true;
        }
      }
      final out = await enhance(EnhanceParams(src,
          fromAi: ai,
          scale: rel,
          sharp: p.sharp * detailMul[_detail],
          contrast: p.contrast,
          sat: p.sat,
          bright: p.bright));
      _usedAi = ai;
      if (!mounted) return;
      setState(() => _result = out);
    } catch (e) {
      if (mounted) _toast('Could not enhance: $e');
    }
    if (mounted) setState(() => _busy = false);
  }

  Future<void> _save() async {
    try {
      await Gal.putImageBytes(_result!.bytes,
          name: 'uhd_${DateTime.now().millisecondsSinceEpoch}.png');
      _toast('Saved to your gallery');
    } catch (e) {
      _toast('Could not save: $e');
    }
  }

  Future<void> _share() async {
    final dir = await getTemporaryDirectory();
    final f = File('${dir.path}/uhd_${DateTime.now().millisecondsSinceEpoch}.png');
    await f.writeAsBytes(_result!.bytes);
    await Share.shareXFiles([XFile(f.path)]);
  }

  Widget _label(String t, String hint) => Padding(
        padding: const EdgeInsets.only(top: 12, bottom: 6),
        child: Row(children: [
          Text(t, style: const TextStyle(fontWeight: FontWeight.w700)),
          const SizedBox(width: 8),
          Expanded(
              child: Text(hint,
                  style: TextStyle(
                      fontSize: 12, color: Theme.of(context).colorScheme.onSurfaceVariant))),
        ]),
      );

  @override
  Widget build(BuildContext context) {
    final cs = Theme.of(context).colorScheme;
    return Scaffold(
      appBar: AppBar(title: const Text('Enhance'), backgroundColor: Colors.transparent),
      body: SafeArea(
        child: Column(children: [
          Expanded(
            child: Padding(
              padding: const EdgeInsets.symmetric(horizontal: 12),
              child: ClipRRect(
                borderRadius: BorderRadius.circular(16),
                child: Stack(children: [
                  Positioned.fill(
                      child: _Compare(before: widget.bytes, after: _result?.bytes)),
                  if (_busy)
                    Positioned.fill(
                        child: Container(
                            color: Colors.black54,
                            child: const Center(
                                child: Column(mainAxisSize: MainAxisSize.min, children: [
                              CircularProgressIndicator(),
                              SizedBox(height: 12),
                              Text('Enhancing… this can take a while'),
                            ])))),
                ]),
              ),
            ),
          ),
          Padding(
            padding: const EdgeInsets.fromLTRB(16, 4, 16, 8),
            child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
              if (_result != null)
                Padding(
                  padding: const EdgeInsets.only(top: 8),
                  child: Text(
                      'Done · ${_result!.width}×${_result!.height} · ${_usedAi ? 'AI enhanced' : 'Smart enhance'} · drag the line to compare',
                      style: TextStyle(fontSize: 12, color: cs.primary)),
                ),
              _label('Output size', 'How big the new photo will be'),
              Row(children: [
                for (var i = 0; i < qualities.length; i++)
                  Expanded(
                    child: Padding(
                      padding: EdgeInsets.only(right: i == qualities.length - 1 ? 0 : 8),
                      child: InkWell(
                        borderRadius: BorderRadius.circular(14),
                        onTap: _busy ? null : () => setState(() => _quality = i),
                        child: Container(
                          padding: const EdgeInsets.symmetric(vertical: 10, horizontal: 4),
                          decoration: BoxDecoration(
                            borderRadius: BorderRadius.circular(14),
                            color: _quality == i ? cs.primaryContainer : cs.surfaceContainerHigh,
                            border: Border.all(
                                color: _quality == i ? cs.primary : Colors.transparent, width: 1.5),
                          ),
                          child: Column(children: [
                            Text(qualities[i].label,
                                style: const TextStyle(fontWeight: FontWeight.w800, fontSize: 15)),
                            Text(qualities[i].sub,
                                style: TextStyle(fontSize: 10, color: cs.onSurfaceVariant)),
                            const SizedBox(height: 2),
                            Text(_sizeText(i),
                                style: TextStyle(fontSize: 10, color: cs.primary)),
                          ]),
                        ),
                      ),
                    ),
                  ),
              ]),
              _label('Look', presets[_preset].hint),
              SizedBox(
                height: 40,
                child: ListView(scrollDirection: Axis.horizontal, children: [
                  for (var i = 0; i < presets.length; i++)
                    Padding(
                      padding: const EdgeInsets.only(right: 8),
                      child: ChoiceChip(
                          label: Text(presets[i].name),
                          selected: _preset == i,
                          onSelected: _busy ? null : (_) => setState(() => _preset = i)),
                    ),
                ]),
              ),
              SwitchListTile(
                contentPadding: EdgeInsets.zero,
                dense: true,
                title: const Text('AI upscale', style: TextStyle(fontWeight: FontWeight.w700)),
                subtitle: Text(
                    _aiReady
                        ? 'Adds real detail to small photos (up to ~1 megapixel)'
                        : 'Not in this build, using Smart enhance',
                    style: const TextStyle(fontSize: 12)),
                value: _useAi && _aiReady,
                onChanged: (_busy || !_aiReady) ? null : (v) => setState(() => _useAi = v),
              ),
              _label('Detail', 'More detail = sharper edges'),
              SizedBox(
                width: double.infinity,
                child: SegmentedButton<int>(
                  showSelectedIcon: false,
                  segments: const [
                    ButtonSegment(value: 0, label: Text('Soft')),
                    ButtonSegment(value: 1, label: Text('Normal')),
                    ButtonSegment(value: 2, label: Text('Strong')),
                  ],
                  selected: {_detail},
                  onSelectionChanged: _busy ? null : (v) => setState(() => _detail = v.first),
                ),
              ),
              const SizedBox(height: 14),
              Row(children: [
                Expanded(
                    child: FilledButton.icon(
                        onPressed: (_busy || _w == 0) ? null : _run,
                        icon: const Icon(Icons.auto_awesome),
                        label: Text(_result == null ? 'Enhance' : 'Enhance again'))),
                if (_result != null) ...[
                  const SizedBox(width: 8),
                  IconButton.filledTonal(
                      tooltip: 'Save', onPressed: _save, icon: const Icon(Icons.download)),
                  IconButton.filledTonal(
                      tooltip: 'Share', onPressed: _share, icon: const Icon(Icons.share)),
                ],
              ]),
            ]),
          ),
        ]),
      ),
    );
  }
}

/// Before/after viewer: the original shows left of the line, the enhanced photo right of it.
class _Compare extends StatefulWidget {
  final Uint8List before;
  final Uint8List? after;
  const _Compare({required this.before, required this.after});
  @override
  State<_Compare> createState() => _CompareState();
}

class _LeftClip extends CustomClipper<Rect> {
  final double t;
  _LeftClip(this.t);
  @override
  Rect getClip(Size size) => Rect.fromLTWH(0, 0, size.width * t, size.height);
  @override
  bool shouldReclip(_LeftClip old) => old.t != t;
}

class _CompareState extends State<_Compare> {
  double _t = 0.5;

  @override
  void didUpdateWidget(_Compare old) {
    super.didUpdateWidget(old);
    if (old.after != widget.after) _t = 0.5;
  }

  Widget _tag(String s, {bool left = true}) => Positioned(
        top: 10,
        left: left ? 10 : null,
        right: left ? null : 10,
        child: Container(
          padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 4),
          decoration:
              BoxDecoration(color: Colors.black54, borderRadius: BorderRadius.circular(8)),
          child: Text(s, style: const TextStyle(fontSize: 12, fontWeight: FontWeight.w600)),
        ),
      );

  @override
  Widget build(BuildContext context) {
    final after = widget.after;
    return LayoutBuilder(builder: (context, box) {
      final w = box.maxWidth, h = box.maxHeight;
      Widget pic(Uint8List b) =>
          Image.memory(b, fit: BoxFit.contain, gaplessPlayback: true, cacheWidth: 2000);
      if (after == null) {
        return Stack(children: [Positioned.fill(child: pic(widget.before)), _tag('Original')]);
      }
      return GestureDetector(
        behavior: HitTestBehavior.opaque,
        onTapDown: (d) => setState(() => _t = (d.localPosition.dx / w).clamp(0.0, 1.0)),
        onHorizontalDragUpdate: (d) =>
            setState(() => _t = (_t + d.delta.dx / w).clamp(0.0, 1.0)),
        child: Stack(children: [
          Positioned.fill(child: pic(after)),
          Positioned.fill(
              child: ClipRect(clipper: _LeftClip(_t), child: pic(widget.before))),
          Positioned(
              left: w * _t - 1.5,
              top: 0,
              bottom: 0,
              child: Container(width: 3, color: Colors.white)),
          Positioned(
            left: w * _t - 20,
            top: h / 2 - 20,
            child: Container(
              width: 40,
              height: 40,
              decoration: const BoxDecoration(color: Colors.white, shape: BoxShape.circle),
              child: const Icon(Icons.compare_arrows, color: Colors.black87),
            ),
          ),
          _tag('Before'),
          _tag('After', left: false),
        ]),
      );
    });
  }
}
