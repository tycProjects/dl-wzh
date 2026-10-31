import 'dart:io';
import 'dart:typed_data';
import 'package:flutter/material.dart';
import 'package:gal/gal.dart';
import 'package:image_picker/image_picker.dart';
import 'package:path_provider/path_provider.dart';
import 'package:share_plus/share_plus.dart';
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
  ('AI Enhance', 'One tap, better photo', Icons.auto_awesome, 0),
  ('Portrait', 'Soft skin, bright face', Icons.face_retouching_natural, 1),
  ('Crisp', 'Extra sharp details', Icons.hd, 2),
  ('HDR', 'Punchy color and contrast', Icons.hdr_strong, 3),
  ('Upscale', 'Bigger, clean pixels', Icons.zoom_out_map, 4),
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
  double _scale = 2, _strength = 1;
  Uint8List? _result;
  bool _busy = false, _showOriginal = false;

  void _toast(String m) =>
      ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(m)));

  Future<void> _run() async {
    setState(() => _busy = true);
    try {
      final p = presets[_preset], k = _strength;
      final out = await enhance(EnhanceParams(widget.bytes,
          scale: _scale,
          sharp: p.sharp * k,
          smooth: p.smooth * k,
          contrast: 1 + (p.contrast - 1) * k,
          sat: 1 + (p.sat - 1) * k,
          bright: 1 + (p.bright - 1) * k));
      if (!mounted) return;
      setState(() => _result = out);
    } catch (e) {
      if (mounted) _toast('Could not enhance: $e');
    }
    if (mounted) setState(() => _busy = false);
  }

  Future<void> _save() async {
    try {
      await Gal.putImageBytes(_result!, name: 'uhd_${DateTime.now().millisecondsSinceEpoch}.jpg');
      _toast('Saved to your gallery');
    } catch (e) {
      _toast('Could not save: $e');
    }
  }

  Future<void> _share() async {
    final dir = await getTemporaryDirectory();
    final f = File('${dir.path}/uhd_${DateTime.now().millisecondsSinceEpoch}.jpg');
    await f.writeAsBytes(_result!);
    await Share.shareXFiles([XFile(f.path)]);
  }

  @override
  Widget build(BuildContext context) {
    final shown = (_showOriginal || _result == null) ? widget.bytes : _result!;
    return Scaffold(
      appBar: AppBar(title: const Text('Enhance'), backgroundColor: Colors.transparent),
      body: SafeArea(
        child: Column(children: [
          Expanded(
            child: Listener(
              onPointerDown: (_) => setState(() => _showOriginal = true),
              onPointerUp: (_) => setState(() => _showOriginal = false),
              onPointerCancel: (_) => setState(() => _showOriginal = false),
              child: Stack(alignment: Alignment.bottomCenter, children: [
                Positioned.fill(
                    child: Padding(
                  padding: const EdgeInsets.symmetric(horizontal: 12),
                  child: ClipRRect(
                      borderRadius: BorderRadius.circular(16),
                      child: Image.memory(shown, fit: BoxFit.contain, gaplessPlayback: true)),
                )),
                if (_result != null)
                  Container(
                    margin: const EdgeInsets.only(bottom: 10),
                    padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 5),
                    decoration: BoxDecoration(
                        color: Colors.black54, borderRadius: BorderRadius.circular(20)),
                    child: Text(_showOriginal ? 'Original' : 'Hold to see original',
                        style: const TextStyle(fontSize: 12)),
                  ),
                if (_busy) const Center(child: CircularProgressIndicator()),
              ]),
            ),
          ),
          Padding(
            padding: const EdgeInsets.fromLTRB(16, 12, 16, 8),
            child: Column(children: [
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
              Row(children: [
                const Text('Size'),
                const SizedBox(width: 10),
                for (final s in [2.0, 3.0, 4.0])
                  Padding(
                    padding: const EdgeInsets.only(right: 8),
                    child: ChoiceChip(
                        label: Text('${s.toInt()}×'),
                        selected: _scale == s,
                        onSelected: _busy ? null : (_) => setState(() => _scale = s)),
                  ),
              ]),
              Row(children: [
                const Text('Strength'),
                Expanded(
                    child: Slider(
                        value: _strength,
                        min: 0,
                        max: 1.5,
                        onChanged: _busy ? null : (v) => setState(() => _strength = v))),
              ]),
              Row(children: [
                Expanded(
                    child: FilledButton.icon(
                        onPressed: _busy ? null : _run,
                        icon: const Icon(Icons.auto_awesome),
                        label: Text(_result == null ? 'Enhance' : 'Enhance again'))),
                if (_result != null) ...[
                  const SizedBox(width: 8),
                  IconButton.filledTonal(onPressed: _save, icon: const Icon(Icons.download)),
                  IconButton.filledTonal(onPressed: _share, icon: const Icon(Icons.share)),
                ],
              ]),
            ]),
          ),
        ]),
      ),
    );
  }
}
