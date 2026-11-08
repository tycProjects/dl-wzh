import 'dart:async';
import 'dart:io';
import 'dart:typed_data';
import 'package:flutter/material.dart';
import '../anim.dart';
import 'package:flutter/services.dart';
import 'package:open_filex/open_filex.dart';
import 'package:video_player/video_player.dart';
import '../theme.dart';

String _mmss(Duration d) {
  final m = d.inMinutes.toString().padLeft(2, '0');
  final s = d.inSeconds.remainder(60).toString().padLeft(2, '0');
  return '$m:$s';
}

/// Top bar shared by the video and photo screens: back arrow, title, "today at 8:01 PM".
Widget _topBar(BuildContext context, String title, String subtitle, {VoidCallback? onOpenWith}) {
  return Container(
    padding: EdgeInsets.fromLTRB(4, MediaQuery.of(context).padding.top + 4, 4, 8),
    decoration: const BoxDecoration(
      gradient: LinearGradient(
        begin: Alignment.topCenter,
        end: Alignment.bottomCenter,
        colors: [Color(0xCC000000), Color(0x00000000)],
      ),
    ),
    child: Row(children: [
      IconButton(
        onPressed: () => Navigator.of(context).maybePop(),
        icon: const Icon(Icons.arrow_back_rounded, color: Colors.white, size: 26),
      ),
      const SizedBox(width: 4),
      Expanded(
        child: Column(crossAxisAlignment: CrossAxisAlignment.start, mainAxisSize: MainAxisSize.min, children: [
          Text(title, maxLines: 1, overflow: TextOverflow.ellipsis, style: inter(17, w: FontWeight.w600, c: Colors.white, h: 1.15)),
          const SizedBox(height: 2),
          Text(subtitle, maxLines: 1, overflow: TextOverflow.ellipsis, style: inter(12, c: const Color(0xCCFFFFFF), h: 1.1)),
        ]),
      ),
      if (onOpenWith != null)
        IconButton(
          onPressed: onOpenWith,
          icon: const Icon(Icons.open_in_new_rounded, color: Colors.white, size: 23),
        ),
    ]),
  );
}

/// Full-screen video player for the Admin chat: black screen, play / pause, a seek bar
/// and "00:02 / 00:26", with the caption under the picture.
class ChatVideoPage extends StatefulWidget {
  final String path;
  final String title;
  final String subtitle;
  final String caption;
  const ChatVideoPage({
    super.key,
    required this.path,
    this.title = 'Video',
    this.subtitle = '',
    this.caption = '',
  });
  @override
  State<ChatVideoPage> createState() => _ChatVideoPageState();
}

class _ChatVideoPageState extends State<ChatVideoPage> {
  late final VideoPlayerController _c;
  bool _show = true;
  bool _bad = false;
  bool _scrubbing = false;
  bool _resumeAfterScrub = false;
  Timer? _hide;

  @override
  void initState() {
    super.initState();
    _c = VideoPlayerController.file(File(widget.path));
    _c.initialize().then((_) {
      if (!mounted) return;
      setState(() {});
      _c.play();
      _armHide();
    }).catchError((Object _) {
      if (mounted) setState(() => _bad = true);
    });
  }

  @override
  void dispose() {
    _hide?.cancel();
    _c.dispose();
    super.dispose();
  }

  bool get _ended {
    final v = _c.value;
    return v.isInitialized && v.duration > Duration.zero && v.position >= v.duration;
  }

  void _armHide() {
    _hide?.cancel();
    _hide = Timer(const Duration(seconds: 3), () {
      if (mounted && _c.value.isPlaying && !_scrubbing) setState(() => _show = false);
    });
  }

  void _toggleControls() {
    setState(() => _show = !_show);
    if (_show) _armHide();
  }

  Future<void> _playPause() async {
    if (!_c.value.isInitialized) return;
    if (_ended) {
      await _c.seekTo(Duration.zero);
      await _c.play();
    } else if (_c.value.isPlaying) {
      await _c.pause();
    } else {
      await _c.play();
    }
    if (mounted) setState(() => _show = true);
    _armHide();
  }

  Future<void> _openWith() async {
    try {
      await OpenFilex.open(widget.path, type: 'video/mp4');
    } catch (_) {}
  }

  Widget _seekBar() {
    return ValueListenableBuilder<VideoPlayerValue>(
      valueListenable: _c,
      builder: (_, v, __) {
        final total = v.duration.inMilliseconds;
        final max = total > 0 ? total.toDouble() : 1.0;
        final pos = v.position.inMilliseconds.clamp(0, total > 0 ? total : 1).toDouble();
        return Row(children: [
          Expanded(
            child: SliderTheme(
              data: SliderTheme.of(context).copyWith(
                trackHeight: 2.5,
                activeTrackColor: Colors.white,
                inactiveTrackColor: const Color(0x44FFFFFF),
                thumbColor: Colors.white,
                overlayColor: const Color(0x22FFFFFF),
                thumbShape: const RoundSliderThumbShape(enabledThumbRadius: 5.5),
                overlayShape: const RoundSliderOverlayShape(overlayRadius: 16),
                trackShape: const RectangularSliderTrackShape(),
              ),
              child: Slider(
                value: pos,
                min: 0,
                max: max,
                onChangeStart: (_) {
                  _scrubbing = true;
                  _resumeAfterScrub = _c.value.isPlaying;
                  _hide?.cancel();
                  if (_resumeAfterScrub) _c.pause();
                },
                onChanged: (x) => _c.seekTo(Duration(milliseconds: x.round())),
                onChangeEnd: (_) {
                  _scrubbing = false;
                  if (_resumeAfterScrub) _c.play();
                  _armHide();
                },
              ),
            ),
          ),
          Padding(
            padding: const EdgeInsets.only(left: 6, right: 16),
            child: Text(
              '${_mmss(v.position)} / ${_mmss(v.duration)}',
              style: inter(13, c: Colors.white, h: 1.0),
            ),
          ),
        ]);
      },
    );
  }

  @override
  Widget build(BuildContext context) {
    final ready = _c.value.isInitialized;
    return AnnotatedRegion<SystemUiOverlayStyle>(
      value: SystemUiOverlayStyle.light,
      child: Scaffold(
        backgroundColor: Colors.black,
        body: Stack(children: [
          Positioned.fill(
            child: GestureDetector(
              behavior: HitTestBehavior.opaque,
              onTap: _toggleControls,
              child: Center(
                child: _bad
                    ? Text('Can\u2019t play this video', style: inter(15, c: C.muted))
                    : ready
                        ? AspectRatio(aspectRatio: _c.value.aspectRatio, child: VideoPlayer(_c))
                        : IosSpinner(size: 30, color: Colors.white),
              ),
            ),
          ),
          if (ready)
            Center(
              child: ValueListenableBuilder<VideoPlayerValue>(
                valueListenable: _c,
                builder: (_, v, __) {
                  final showBtn = _show || !v.isPlaying;
                  final playing = v.isPlaying && !_ended;
                  return IgnorePointer(
                    ignoring: !showBtn,
                    child: AnimatedOpacity(
                      opacity: showBtn ? 1 : 0,
                      duration: const Duration(milliseconds: 180),
                      child: GestureDetector(
                        onTap: _playPause,
                        child: Container(
                          width: 64, height: 64,
                          decoration: const BoxDecoration(color: Color(0x99000000), shape: BoxShape.circle),
                          child: Icon(playing ? Icons.pause_rounded : Icons.play_arrow_rounded, size: 40, color: Colors.white),
                        ),
                      ),
                    ),
                  );
                },
              ),
            ),
          Positioned(
            left: 0, right: 0, top: 0,
            child: IgnorePointer(
              ignoring: !_show,
              child: AnimatedOpacity(
                opacity: _show ? 1 : 0,
                duration: const Duration(milliseconds: 180),
                child: _topBar(context, widget.title, widget.subtitle, onOpenWith: _openWith),
              ),
            ),
          ),
          Positioned(
            left: 0, right: 0, bottom: 0,
            child: IgnorePointer(
              ignoring: !_show,
              child: AnimatedOpacity(
                opacity: _show ? 1 : 0,
                duration: const Duration(milliseconds: 180),
                child: Container(
                  padding: EdgeInsets.only(bottom: MediaQuery.of(context).padding.bottom + 8, top: 24),
                  decoration: const BoxDecoration(
                    gradient: LinearGradient(
                      begin: Alignment.topCenter,
                      end: Alignment.bottomCenter,
                      colors: [Color(0x00000000), Color(0xCC000000)],
                    ),
                  ),
                  child: Column(mainAxisSize: MainAxisSize.min, crossAxisAlignment: CrossAxisAlignment.start, children: [
                    if (widget.caption.isNotEmpty)
                      Padding(
                        padding: const EdgeInsets.fromLTRB(16, 0, 16, 6),
                        child: Text(widget.caption, style: inter(16, c: Colors.white)),
                      ),
                    _seekBar(),
                  ]),
                ),
              ),
            ),
          ),
        ]),
      ),
    );
  }
}

/// Full-screen photo with pinch-to-zoom, same bars as the video screen.
class ChatPhotoPage extends StatefulWidget {
  final Uint8List bytes;
  final String title;
  final String subtitle;
  final String caption;
  const ChatPhotoPage({
    super.key,
    required this.bytes,
    this.title = 'Photo',
    this.subtitle = '',
    this.caption = '',
  });
  @override
  State<ChatPhotoPage> createState() => _ChatPhotoPageState();
}

class _ChatPhotoPageState extends State<ChatPhotoPage> {
  bool _show = true;

  @override
  Widget build(BuildContext context) {
    return AnnotatedRegion<SystemUiOverlayStyle>(
      value: SystemUiOverlayStyle.light,
      child: Scaffold(
        backgroundColor: Colors.black,
        body: Stack(children: [
          Positioned.fill(
            child: GestureDetector(
              behavior: HitTestBehavior.opaque,
              onTap: () => setState(() => _show = !_show),
              child: InteractiveViewer(
                minScale: 1,
                maxScale: 5,
                child: Center(child: Image.memory(widget.bytes, fit: BoxFit.contain, gaplessPlayback: true)),
              ),
            ),
          ),
          Positioned(
            left: 0, right: 0, top: 0,
            child: IgnorePointer(
              ignoring: !_show,
              child: AnimatedOpacity(
                opacity: _show ? 1 : 0,
                duration: const Duration(milliseconds: 180),
                child: _topBar(context, widget.title, widget.subtitle),
              ),
            ),
          ),
          if (widget.caption.isNotEmpty)
            Positioned(
              left: 16, right: 16,
              bottom: MediaQuery.of(context).padding.bottom + 14,
              child: IgnorePointer(
                child: AnimatedOpacity(
                  opacity: _show ? 1 : 0,
                  duration: const Duration(milliseconds: 180),
                  child: Text(widget.caption, style: inter(16, c: Colors.white)),
                ),
              ),
            ),
        ]),
      ),
    );
  }
}
