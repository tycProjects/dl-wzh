import 'dart:async';
import 'dart:convert';
import 'dart:math' as math;
import 'package:flutter/foundation.dart';
import 'media.dart';
import 'model.dart';
import 'renderer.dart';
import 'storage.dart';

double _clamp(double v, double a, double b) => v < a ? a : (v > b ? b : v);

/// Structure changes notify the controller; time changes only touch [timeN] so playback is cheap.
class EditorController extends ChangeNotifier {
  EditorController(this.project);

  Project project;
  String? selectedId;
  final ValueNotifier<double> timeN = ValueNotifier(0);
  bool playing = false;
  final List<String> _undo = [], _redo = [];
  Timer? _saveTimer;

  double get time => timeN.value;
  bool get canUndo => _undo.isNotEmpty;
  bool get canRedo => _redo.isNotEmpty;

  Layer? get selected {
    for (final l in project.layers) {
      if (l.id == selectedId) return l;
    }
    return null;
  }

  void refresh() => notifyListeners();

  // ---------- persistence / history ----------
  void _changed() {
    _saveTimer?.cancel();
    _saveTimer = Timer(const Duration(seconds: 2), () => Store.save(project));
    notifyListeners();
  }

  Future<void> saveNow() {
    _saveTimer?.cancel();
    return Store.save(project);
  }

  void checkpoint() {
    _undo.add(jsonEncode(project.toJson()));
    if (_undo.length > 100) _undo.removeAt(0);
    _redo.clear();
    notifyListeners();
  }

  void _restore(String json) {
    project = Project.fromJson(jsonDecode(json) as Map<String, dynamic>);
    if (selected == null) selectedId = null;
    timeN.value = _clamp(timeN.value, 0, project.duration);
    _changed();
  }

  void undo() {
    if (_undo.isEmpty) return;
    _redo.add(jsonEncode(project.toJson()));
    _restore(_undo.removeLast());
  }

  void redo() {
    if (_redo.isEmpty) return;
    _undo.add(jsonEncode(project.toJson()));
    _restore(_redo.removeLast());
  }

  // ---------- time / playback ----------
  double snap(double t) {
    final f = project.fps.toDouble();
    return _clamp((t * f).round() / f, 0, project.duration - 1 / f);
  }

  void seek(double t) => timeN.value = snap(t);
  void step(int frames) => seek(time + frames / project.fps);

  void play() {
    if (time >= project.duration - 2 / project.fps) timeN.value = 0;
    playing = true;
    notifyListeners();
  }

  void pause() {
    playing = false;
    notifyListeners();
  }

  void advance(double dt) {
    final n = time + dt;
    if (n >= project.duration) {
      timeN.value = project.duration - 1 / project.fps;
      pause();
    } else {
      timeN.value = n;
    }
  }

  // ---------- selection ----------
  void select(String? id) {
    if (selectedId == id) return;
    selectedId = id;
    notifyListeners();
  }

  // ---------- layers ----------
  double get _newStart => _clamp(time, 0, project.duration - 0.5);

  void _add(Layer l) {
    project.layers.add(l);
    selectedId = l.id;
    _changed();
  }

  void addShape(LayerType type) {
    checkpoint();
    _add(Layer(
      id: newId(),
      name: type == LayerType.rect ? 'Rectangle' : 'Ellipse',
      type: type,
      start: _newStart,
      end: project.duration,
      w: 300,
      h: 300,
      cx: project.width / 2,
      cy: project.height / 2,
      radius: type == LayerType.rect ? 24 : 0,
    ));
  }

  void addText() {
    checkpoint();
    final l = Layer(
      id: newId(),
      name: 'Text',
      type: LayerType.text,
      start: _newStart,
      end: project.duration,
      w: 100,
      h: 100,
      cx: project.width / 2,
      cy: project.height / 2,
      color: 0xFFFFFFFF,
      text: 'Text',
      fontSize: project.width * 0.1,
    )..remeasure();
    _add(l);
  }

  Future<void> addImage(String storedPath) async {
    await ImageStore.load(storedPath);
    final img = ImageStore.get(storedPath);
    if (img == null) return;
    checkpoint();
    final w = project.width * 0.6;
    _add(Layer(
      id: newId(),
      name: 'Image',
      type: LayerType.image,
      start: _newStart,
      end: project.duration,
      w: w,
      h: w * img.height / img.width,
      cx: project.width / 2,
      cy: project.height / 2,
      imagePath: storedPath,
    ));
  }

  void addVideo(ImportedVideo v) {
    checkpoint();
    final start = _newStart;
    final need = (start + v.duration).ceilToDouble();
    if (need > project.duration) project.duration = math.min(120.0, need);
    final fit = math.min(project.width / v.w, project.height / v.h);
    _add(Layer(
      id: newId(),
      name: 'Video',
      type: LayerType.video,
      start: start,
      end: math.min(project.duration, start + v.duration),
      w: v.w * fit,
      h: v.h * fit,
      cx: project.width / 2,
      cy: project.height / 2,
      videoDir: v.dir,
      vFrames: v.frames,
      vFps: v.fps,
    ));
  }

  /// Jump to [t] and play, used to preview an animation preset.
  void previewAt(double t) {
    seek(t);
    play();
  }

  void deleteSelected() {
    final l = selected;
    if (l == null) return;
    checkpoint();
    project.layers.remove(l);
    selectedId = null;
    _changed();
  }

  void duplicateSelected() {
    final l = selected;
    if (l == null) return;
    checkpoint();
    final copy = Layer.fromJson(l.toJson())
      ..id = newId()
      ..name = '${l.name} copy';
    for (final p in [copy.x, copy.y]) {
      p.base += 40;
      for (final k in p.keys) {
        k.v += 40;
      }
    }
    project.layers.insert(project.layers.indexOf(l) + 1, copy);
    selectedId = copy.id;
    _changed();
  }

  /// dir +1 = bring forward, -1 = send back.
  void moveZ(int dir) {
    final l = selected;
    if (l == null) return;
    final i = project.layers.indexOf(l), j = i + dir;
    if (j < 0 || j >= project.layers.length) return;
    checkpoint();
    project.layers
      ..removeAt(i)
      ..insert(j, l);
    _changed();
  }

  void split() {
    final l = selected;
    if (l == null || time <= l.start + 0.05 || time >= l.end - 0.05) return;
    checkpoint();
    final copy = Layer.fromJson(l.toJson())..id = newId();
    if (l.type == LayerType.video) copy.srcOffset = l.srcOffset + (time - l.start);
    copy.start = time;
    l.end = time;
    project.layers.insert(project.layers.indexOf(l) + 1, copy);
    selectedId = copy.id;
    _changed();
  }

  // ---------- properties ----------
  void setProp(String key, double v) {
    final l = selected;
    if (l == null) return;
    l.props[key]!.set(time, v);
    _changed();
  }

  void toggleKey(String key) {
    final l = selected;
    if (l == null) return;
    checkpoint();
    l.props[key]!.toggleKey(time);
    _changed();
  }

  void setEasing(String key, KeyEasing e) {
    final l = selected;
    if (l == null) return;
    final k = l.props[key]!.keyAt(time);
    if (k == null) return;
    checkpoint();
    k.easing = e;
    _changed();
  }

  void edit(void Function(Layer l) f, {bool cp = true}) {
    final l = selected;
    if (l == null) return;
    if (cp) checkpoint();
    f(l);
    _changed();
  }

  void setBackground(int color) {
    checkpoint();
    project.bg = color;
    _changed();
  }

  bool hasKeyAt(List<String> keys) {
    final l = selected;
    if (l == null) return false;
    return keys.any((k) => l.props[k]!.keyAt(time) != null);
  }

  bool isAnimated(List<String> keys) {
    final l = selected;
    if (l == null) return false;
    return keys.any((k) => l.props[k]!.animated);
  }

  /// Adds a keyframe on every prop in [keys] at the playhead, or removes them if any exists.
  void toggleKeys(List<String> keys) {
    final l = selected;
    if (l == null) return;
    checkpoint();
    final remove = hasKeyAt(keys);
    for (final k in keys) {
      final p = l.props[k]!;
      final exists = p.keyAt(time) != null;
      if (remove == exists) p.toggleKey(time);
    }
    _changed();
  }

  // ---------- timeline editing ----------
  void moveClip(Layer l, double newStart) {
    final len = l.end - l.start;
    final s = _clamp(newStart, 0, project.duration - len);
    final d = s - l.start;
    l.start = s;
    l.end = s + len;
    for (final p in l.props.values) {
      for (final k in p.keys) {
        k.t += d; // keyframes travel with the clip
      }
    }
    _changed();
  }

  void trimStart(Layer l, double t) {
    final lo = l.type == LayerType.video ? math.max(0.0, l.start - l.srcOffset) : 0.0;
    final ns = _clamp(t, lo, l.end - 0.1);
    if (l.type == LayerType.video) l.srcOffset += ns - l.start;
    l.start = ns;
    _changed();
  }

  void trimEnd(Layer l, double t) {
    var hi = project.duration;
    if (l.type == LayerType.video) hi = math.min(hi, l.start + l.vFrames / l.vFps - l.srcOffset);
    l.end = _clamp(t, l.start + 0.1, math.max(l.start + 0.1, hi));
    _changed();
  }

  void setDuration(double d) {
    d = _clamp(d, 1, 120);
    checkpoint();
    project.duration = d;
    for (final l in project.layers) {
      if (l.end > d) l.end = d;
      if (l.start > d - 0.1) l.start = d - 0.1 < 0 ? 0 : d - 0.1;
    }
    timeN.value = _clamp(timeN.value, 0, d - 1 / project.fps);
    _changed();
  }

  @override
  void dispose() {
    _saveTimer?.cancel();
    timeN.dispose();
    super.dispose();
  }
}
