import 'package:file_selector/file_selector.dart';
import 'package:flutter/material.dart' hide Text;
import 'package:photo_manager/photo_manager.dart';
import '../anim.dart';
import '../banner.dart';
import '../theme.dart';
import '../tr.dart';
import '../appearance.dart';

/// What the icon picker hands back.
class PickedImage {
  final String path, name;
  const PickedImage(this.path, this.name);
}

/// Opens the in-app gallery (our own design) to choose the app icon. If photo access is
/// refused it falls back to the system file picker so the person is never stuck.
Future<PickedImage?> pickIconImage(BuildContext context) async {
  PermissionState state;
  try {
    state = await PhotoManager.requestPermissionExtend();
  } catch (_) {
    state = PermissionState.denied;
  }
  if (!context.mounted) return null;
  final ok = state == PermissionState.authorized || state == PermissionState.limited;
  if (!ok) {
    IosBanner.show(context, kind: BannerKind.warn, title: 'Photos are off', msg: 'Turn on photo access to pick from your gallery. Opening your files for now.');
    return _fromFiles();
  }
  return Navigator.of(context, rootNavigator: true).push<PickedImage>(PageRouteBuilder<PickedImage>(
    transitionDuration: const Duration(milliseconds: 340),
    reverseTransitionDuration: const Duration(milliseconds: 260),
    pageBuilder: (_, __, ___) => const _GalleryPage(),
    transitionsBuilder: (_, a, __, child) => SlideTransition(
      position: Tween(begin: const Offset(0, 1), end: Offset.zero)
          .animate(CurvedAnimation(parent: a, curve: iosCurve, reverseCurve: iosCurve.flipped)),
      child: child,
    ),
  ));
}

Future<PickedImage?> _fromFiles() async {
  final f = await openFile(acceptedTypeGroups: [const XTypeGroup(label: 'Images', extensions: ['png', 'jpg', 'jpeg', 'webp'])]);
  return f == null ? null : PickedImage(f.path, f.name);
}

class _GalleryPage extends StatefulWidget {
  const _GalleryPage();
  @override
  State<_GalleryPage> createState() => _GalleryPageState();
}

class _GalleryPageState extends State<_GalleryPage> {
  static const _pageSize = 60;
  List<AssetPathEntity> albums = [];
  int album = 0;
  final List<AssetEntity> items = [];
  int page = 0;
  bool loading = true, more = true, opening = false, busy = false;
  final scroll = ScrollController();

  @override
  void initState() {
    super.initState();
    scroll.addListener(() {
      if (scroll.position.pixels > scroll.position.maxScrollExtent - 600) _loadMore();
    });
    _init();
  }

  @override
  void dispose() {
    scroll.dispose();
    super.dispose();
  }

  Future<void> _init() async {
    try {
      // Always newest first, and never reuse a stale thumbnail/file cache from an earlier visit.
      try { await PhotoManager.clearFileCache(); } catch (_) {}
      final list = await PhotoManager.getAssetPathList(
        type: RequestType.image,
        filterOption: FilterOptionGroup(
          orders: [const OrderOption(type: OrderOptionType.createDate, asc: false)],
        ),
      );
      if (!mounted) return;
      // "Recent" (all photos) goes first so the newest picture is the first thing you see.
      list.sort((a, b) => (b.isAll ? 1 : 0) - (a.isAll ? 1 : 0));
      albums = list;
      await _loadAlbum(0);
    } catch (_) {
      if (mounted) setState(() => loading = false);
    }
  }

  Future<void> _loadAlbum(int i) async {
    busy = false;
    setState(() { album = i; items.clear(); page = 0; more = true; loading = true; });
    await _loadMore();
  }

  Future<void> _loadMore() async {
    if (albums.isEmpty) {
      if (mounted) setState(() { loading = false; more = false; });
      return;
    }
    if (!more || busy) return;
    busy = true;
    final path = albums[album];
    final got = await path.getAssetListPaged(page: page, size: _pageSize);
    busy = false;
    if (!mounted || path != albums[album]) return;
    setState(() {
      items.addAll(got);
      page++;
      more = got.length == _pageSize;
      loading = false;
    });
  }

  Future<void> _choose(AssetEntity a) async {
    if (opening) return;
    opening = true;
    final file = await a.originFile ?? await a.file;
    opening = false;
    if (file == null || !mounted) return;
    Navigator.of(context).pop(PickedImage(file.path, file.path.split('/').last));
  }

  Future<void> _files() async {
    final r = await _fromFiles();
    if (r != null && mounted) Navigator.of(context).pop(r);
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: C.bg,
      body: SafeArea(
        child: Column(children: [
          Padding(
            padding: const EdgeInsets.fromLTRB(16, 10, 16, 6),
            child: Row(children: [
              Pressable(
                onTap: () => Navigator.of(context).pop(),
                child: Container(
                  width: 42, height: 42,
                  decoration: const BoxDecoration(color: Color(0xFF2A2A2C), shape: BoxShape.circle),
                  child: const Icon(Icons.close_rounded, size: 22, color: Colors.white),
                ),
              ),
              const SizedBox(width: 14),
              Expanded(
                child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
                  Text('Choose an icon', style: inter(20, w: FontWeight.w700)),
                  Text('Square image, 512×512 or larger works best.', style: inter(12.5, c: C.muted)),
                ]),
              ),
              Pressable(
                onTap: _files,
                child: Container(
                  height: 38, padding: const EdgeInsets.symmetric(horizontal: 14), alignment: Alignment.center,
                  decoration: BoxDecoration(color: const Color(0xFF1F4A40), borderRadius: BorderRadius.circular(19)),
                  child: Row(mainAxisSize: MainAxisSize.min, children: [
                    const Icon(Icons.folder_open_rounded, size: 17, color: C.teal),
                    const SizedBox(width: 6),
                    Text('Files', style: inter(15, w: FontWeight.w600, c: C.teal)),
                  ]),
                ),
              ),
            ]),
          ),
          if (albums.length > 1)
            SizedBox(
              height: 46,
              child: ListView.separated(
                padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 6),
                scrollDirection: Axis.horizontal,
                itemCount: albums.length,
                separatorBuilder: (_, __) => const SizedBox(width: 8),
                itemBuilder: (_, i) => Pressable(
                  scale: .94,
                  onTap: () => i == album ? null : _loadAlbum(i),
                  child: AnimatedContainer(
                    duration: const Duration(milliseconds: 200),
                    padding: const EdgeInsets.symmetric(horizontal: 14),
                    alignment: Alignment.center,
                    decoration: BoxDecoration(
                      color: i == album ? C.teal : const Color(0xFF2A2A2C),
                      borderRadius: BorderRadius.circular(17)),
                    child: Text(albums[i].isAll ? 'Recent' : albums[i].name,
                        style: inter(14, w: FontWeight.w600, c: i == album ? C.onTeal : Colors.white)),
                  ),
                ),
              ),
            ),
          Expanded(
            child: loading && items.isEmpty
                ? const Center(child: IosSpinner(size: 28))
                : items.isEmpty
                    ? Center(
                        child: Column(mainAxisSize: MainAxisSize.min, children: [
                          Text('No pictures here', style: inter(17, w: FontWeight.w600)),
                          const SizedBox(height: 6),
                          Text('Try another album or pick from Files.', style: inter(14, c: C.muted)),
                          const SizedBox(height: 16),
                          Pressable(
                            onTap: PhotoManager.openSetting,
                            child: Text('Photo access settings', style: inter(15, w: FontWeight.w600, c: C.teal)),
                          ),
                        ]),
                      )
                    : GridView.builder(
                        controller: scroll,
                        padding: const EdgeInsets.fromLTRB(3, 6, 3, 24),
                        gridDelegate: const SliverGridDelegateWithFixedCrossAxisCount(crossAxisCount: 3, mainAxisSpacing: 3, crossAxisSpacing: 3),
                        itemCount: items.length,
                        itemBuilder: (_, i) => _Thumb(key: ValueKey(items[i].id), asset: items[i], onTap: () => _choose(items[i])),
                      ),
          ),
        ]),
      ),
    );
  }
}

class _Thumb extends StatefulWidget {
  final AssetEntity asset;
  final VoidCallback onTap;
  const _Thumb({super.key, required this.asset, required this.onTap});
  @override
  State<_Thumb> createState() => _ThumbState();
}

class _ThumbState extends State<_Thumb> {
  late final Future<dynamic> data = widget.asset.thumbnailDataWithSize(const ThumbnailSize.square(260));
  @override
  Widget build(BuildContext context) => Pressable(
        scale: .94,
        onTap: widget.onTap,
        child: ClipRRect(
          borderRadius: BorderRadius.circular(8),
          child: Container(
            color: const Color(0xFF1F1F21),
            child: FutureBuilder<dynamic>(
              future: data,
              builder: (_, s) => AnimatedOpacity(
                opacity: s.data == null ? 0 : 1,
                duration: const Duration(milliseconds: 200),
                child: s.data == null ? const SizedBox.expand() : Unfilter(child: Image.memory(s.data, fit: BoxFit.cover, gaplessPlayback: true)),
              ),
            ),
          ),
        ),
      );
}
