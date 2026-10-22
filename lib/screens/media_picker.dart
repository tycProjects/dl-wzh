import 'dart:typed_data';
import 'package:flutter/material.dart';
import 'package:photo_manager/photo_manager.dart';
import '../theme.dart';

enum _Kind { all, video, photo }

/// The app's own gallery browser: albums, All / Video / Photo tabs, multi-select with order numbers.
/// Pops with the selected assets (in tap order).
class MediaPicker extends StatefulWidget {
  const MediaPicker({super.key});
  @override
  State<MediaPicker> createState() => _MediaPickerState();
}

class _MediaPickerState extends State<MediaPicker> {
  static const pageSize = 96, maxPick = 20;
  _Kind kind = _Kind.all;
  bool? access;
  List<AssetPathEntity> albums = [];
  int albumIdx = 0, page = 0, gen = 0;
  final List<AssetEntity> assets = [];
  final List<AssetEntity> picked = [];
  bool more = true, loading = false;
  final sc = ScrollController();

  RequestType get _type => kind == _Kind.video ? RequestType.video : (kind == _Kind.photo ? RequestType.image : RequestType.common);

  @override
  void initState() {
    super.initState();
    sc.addListener(() {
      if (sc.hasClients && sc.position.pixels > sc.position.maxScrollExtent - 700) _more();
    });
    _init();
  }

  @override
  void dispose() {
    sc.dispose();
    super.dispose();
  }

  Future<void> _init() async {
    final ps = await PhotoManager.requestPermissionExtend();
    if (!mounted) return;
    if (!ps.hasAccess) {
      setState(() => access = false);
      return;
    }
    setState(() => access = true);
    await _loadAlbums();
  }

  Future<void> _loadAlbums() async {
    final g = ++gen;
    setState(() {
      loading = true;
      assets.clear();
      page = 0;
      more = true;
    });
    final list = await PhotoManager.getAssetPathList(type: _type);
    if (!mounted || g != gen) return;
    albums = list;
    albumIdx = 0;
    if (list.isEmpty) {
      setState(() {
        loading = false;
        more = false;
      });
      return;
    }
    await _more(first: true);
  }

  Future<void> _more({bool first = false}) async {
    if (albums.isEmpty || !more || (loading && !first)) return;
    final g = gen;
    setState(() => loading = true);
    final items = await albums[albumIdx].getAssetListPaged(page: page, size: pageSize);
    if (!mounted || g != gen) return;
    setState(() {
      assets.addAll(items);
      page++;
      more = items.length == pageSize;
      loading = false;
    });
  }

  void _pickAlbum(int i) {
    gen++;
    setState(() {
      albumIdx = i;
      assets.clear();
      page = 0;
      more = true;
      loading = false;
    });
    _more();
  }

  void _setKind(_Kind k) {
    if (k == kind) return;
    kind = k;
    _loadAlbums();
  }

  void _albumSheet() {
    showModalBottomSheet(
      context: context,
      backgroundColor: C.sheet,
      builder: (ctx) => SafeArea(
        child: ListView.builder(
          shrinkWrap: true,
          itemCount: albums.length,
          itemBuilder: (_, i) => ListTile(
            title: Text(albums[i].name),
            selected: i == albumIdx,
            selectedColor: C.teal,
            trailing: FutureBuilder<int>(
              future: albums[i].assetCountAsync,
              builder: (_, s) => Text(s.hasData ? '${s.data}' : '', style: const TextStyle(color: C.muted)),
            ),
            onTap: () {
              Navigator.pop(ctx);
              _pickAlbum(i);
            },
          ),
        ),
      ),
    );
  }

  String _dur(Duration d) => '${d.inMinutes}:${(d.inSeconds % 60).toString().padLeft(2, '0')}';

  @override
  Widget build(BuildContext context) => Scaffold(
        backgroundColor: C.bg,
        body: SafeArea(
          child: Column(children: [
            _top(),
            _tabs(),
            Expanded(child: _body()),
            if (picked.isNotEmpty) _bar(),
          ]),
        ),
      );

  Widget _top() => SizedBox(
        height: 52,
        child: Row(children: [
          IconButton(icon: const Icon(Icons.close), onPressed: () => Navigator.pop(context)),
          Expanded(
            child: Center(
              child: TextButton.icon(
                onPressed: albums.isEmpty ? null : _albumSheet,
                icon: const Icon(Icons.keyboard_arrow_down, color: C.text),
                label: Text(
                  albums.isEmpty ? 'Gallery' : albums[albumIdx].name,
                  maxLines: 1,
                  overflow: TextOverflow.ellipsis,
                  style: const TextStyle(color: C.text, fontSize: 16, fontWeight: FontWeight.w600),
                ),
              ),
            ),
          ),
          const SizedBox(width: 48),
        ]),
      );

  Widget _tabs() => SizedBox(
        height: 40,
        child: Row(children: [
          const SizedBox(width: 12),
          for (final e in const {_Kind.all: 'All', _Kind.video: 'Video', _Kind.photo: 'Photo'}.entries)
            Padding(
              padding: const EdgeInsets.only(right: 8),
              child: ChoiceChip(
                label: Text(e.value),
                selected: kind == e.key,
                onSelected: (_) => _setKind(e.key),
              ),
            ),
        ]),
      );

  Widget _body() {
    if (access == null) return const Center(child: CircularProgressIndicator());
    if (access == false) {
      return Center(
        child: Padding(
          padding: const EdgeInsets.all(28),
          child: Column(mainAxisSize: MainAxisSize.min, children: [
            const Icon(Icons.lock_outline, size: 40, color: C.muted),
            const SizedBox(height: 12),
            const Text('Allow access to your photos and videos to pick media.', textAlign: TextAlign.center, style: TextStyle(color: C.muted)),
            const SizedBox(height: 16),
            ElevatedButton(onPressed: PhotoManager.openSetting, child: const Text('Open settings')),
            TextButton(onPressed: _init, child: const Text('Try again')),
          ]),
        ),
      );
    }
    if (assets.isEmpty) {
      return Center(child: loading ? const CircularProgressIndicator() : const Text('Nothing here', style: TextStyle(color: C.muted)));
    }
    return GridView.builder(
      controller: sc,
      padding: const EdgeInsets.fromLTRB(2, 2, 2, 12),
      gridDelegate: const SliverGridDelegateWithFixedCrossAxisCount(crossAxisCount: 4, mainAxisSpacing: 2, crossAxisSpacing: 2),
      itemCount: assets.length,
      itemBuilder: (_, i) => _tile(assets[i]),
    );
  }

  Widget _tile(AssetEntity a) {
    final idx = picked.indexWhere((p) => p.id == a.id);
    final sel = idx >= 0;
    return GestureDetector(
      onTap: () => setState(() {
        if (sel) {
          picked.removeAt(idx);
        } else if (picked.length < maxPick) {
          picked.add(a);
        }
      }),
      child: Stack(fit: StackFit.expand, children: [
        _Thumb(key: ValueKey(a.id), asset: a),
        if (sel) Container(color: C.teal.withOpacity(.28)),
        if (a.type == AssetType.video)
          Positioned(
            left: 5,
            bottom: 3,
            child: Text(_dur(a.videoDuration), style: const TextStyle(fontSize: 11, fontWeight: FontWeight.w600, shadows: [Shadow(blurRadius: 4)])),
          ),
        Positioned(
          right: 5,
          top: 5,
          child: Container(
            width: 22,
            height: 22,
            alignment: Alignment.center,
            decoration: BoxDecoration(
              shape: BoxShape.circle,
              color: sel ? C.teal : Colors.black38,
              border: Border.all(color: Colors.white, width: 1.5),
            ),
            child: sel ? Text('${idx + 1}', style: const TextStyle(fontSize: 12, fontWeight: FontWeight.w700, color: C.onTeal)) : null,
          ),
        ),
      ]),
    );
  }

  Widget _bar() => Container(
        color: C.card,
        padding: const EdgeInsets.fromLTRB(16, 8, 16, 8),
        child: Row(children: [
          Text('${picked.length} selected', style: const TextStyle(color: C.muted)),
          const Spacer(),
          ElevatedButton(
            style: ElevatedButton.styleFrom(backgroundColor: C.teal, foregroundColor: C.onTeal),
            onPressed: () => Navigator.pop(context, List<AssetEntity>.of(picked)),
            child: Text('Add ${picked.length}'),
          ),
        ]),
      );
}

class _Thumb extends StatefulWidget {
  final AssetEntity asset;
  const _Thumb({super.key, required this.asset});
  @override
  State<_Thumb> createState() => _ThumbState();
}

class _ThumbState extends State<_Thumb> {
  late final Future<Uint8List?> f = widget.asset.thumbnailDataWithSize(const ThumbnailSize.square(240), quality: 80);
  @override
  Widget build(BuildContext context) => FutureBuilder<Uint8List?>(
        future: f,
        builder: (_, s) => s.data == null
            ? Container(color: C.card)
            : Image.memory(s.data!, fit: BoxFit.cover, gaplessPlayback: true),
      );
}
