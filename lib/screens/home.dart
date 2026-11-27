import 'package:flutter/material.dart' hide Text;
import 'package:flutter/cupertino.dart' show CupertinoPageRoute, CupertinoSliverRefreshControl;
import '../api.dart';
import '../store.dart';
import '../theme.dart';
import '../widgets.dart';
import '../illus.dart';
import '../anim.dart';
import 'build_page.dart';
import 'create_sheet.dart';
import '../tr.dart';

class HomeScreen extends StatelessWidget {
  const HomeScreen({super.key});

  String _group(Job j) {
    final d = j.createdAt;
    if (d == null) return 'Earlier';
    final t = DateTime.now();
    final days = DateTime(t.year, t.month, t.day).difference(DateTime(d.year, d.month, d.day)).inDays;
    return days <= 0 ? 'Today' : (days == 1 ? 'Yesterday' : 'Earlier');
  }

  String _time(DateTime? d) {
    if (d == null) return '';
    final h = d.hour % 12 == 0 ? 12 : d.hour % 12;
    return '$h:${d.minute.toString().padLeft(2, '0')} ${d.hour >= 12 ? 'PM' : 'AM'}';
  }

  Future<void> _clear(BuildContext context, List<Job> items) async {
    for (final j in items.where((j) => !j.isActive || j.isCancelled)) {
      Api.i.deleteHistory(j.id);
    }
    Store.i.history.value = items.where((j) => j.isActive && !j.isCancelled).toList();
  }

  String _size(int? bytes) {
    if (bytes == null) return '';
    final mb = bytes / (1024 * 1024);
    return mb >= 1 ? '${mb.toStringAsFixed(1)} MB' : '${(bytes / 1024).round()} KB';
  }

  Widget _chip(String text, Color fg, Color bg) => Container(
        padding: const EdgeInsets.symmetric(horizontal: 9, vertical: 3),
        decoration: BoxDecoration(color: bg, borderRadius: BorderRadius.circular(20)),
        child: Text(text, style: inter(12, w: FontWeight.w500, c: fg)),
      );

  /// "TyceRecorder_v2_32_SoundSync" -> name TyceRecorder, version v2.32, note SoundSync.
  /// Names that don't follow that pattern are shown whole.
  _NameParts _parts(String name) {
    final m = RegExp(r'^(.+?)[_ -]v(\d+)[_.](\d+)(?:[_ -](.+))?$', caseSensitive: false).firstMatch(name);
    if (m == null) return _NameParts(name, null, null);
    final note = (m.group(4) ?? '').replaceAll('_', ' ').trim();
    return _NameParts(m.group(1)!, 'v${m.group(2)}.${m.group(3)}', note.isEmpty ? null : note);
  }

  Widget _row(BuildContext context, Job j) {
    const neutralBg = Color(0xFF2C2C2E);
    const neutralFg = Color(0xFFD8D8DC);
    final String status;
    final Color sc, sb;
    if (j.isError) {
      status = 'Failed';
      sc = C.red;
      sb = const Color(0xFF43201F);
    } else if (j.isDone) {
      status = j.expired ? 'Expired' : 'Ready';
      sc = j.expired ? C.muted : neutralFg;
      sb = neutralBg;
    } else if (j.isCancelled) {
      status = 'Cancelled';
      sc = C.muted;
      sb = neutralBg;
    } else {
      status = j.status == 'queued-waiting' ? 'Waiting' : (j.status == 'awaiting-repair-confirmation' ? 'Auto-fix ready' : 'Building');
      sc = neutralFg;
      sb = neutralBg;
    }
    final canDelete = !j.isActive || j.isCancelled;
    final p = _parts(j.name);
    // Time comes before size so a long note can never push the time off the line.
    final meta = [
      p.note ?? j.typeLabel,
      if (_time(j.createdAt).isNotEmpty) _time(j.createdAt),
      if (j.isDone && !j.expired && _size(j.sizeBytes).isNotEmpty) _size(j.sizeBytes),
    ].join(' · ');
    final failed = j.isError;
    return Padding(
      padding: const EdgeInsets.only(bottom: 10),
      child: Dismissible(
        key: ValueKey(j.id),
        direction: canDelete ? DismissDirection.endToStart : DismissDirection.none,
        background: Container(
          alignment: Alignment.centerRight,
          padding: const EdgeInsets.only(right: 26),
          decoration: BoxDecoration(color: C.red, borderRadius: BorderRadius.circular(18)),
          child: const Icon(Icons.delete_outline_rounded, size: 26, color: Colors.white),
        ),
        onDismissed: (_) {
          Api.i.deleteHistory(j.id);
          Store.i.history.value = Store.i.history.value.where((e) => e.id != j.id).toList();
        },
        child: Pressable(
          scale: .985,
          onTap: () async {
            await Navigator.of(context).push(CupertinoPageRoute(builder: (_) => BuildPage(jobId: j.id, appName: j.name)));
            Store.i.refreshHistory();
          },
          child: Container(
            decoration: BoxDecoration(
              color: failed ? const Color(0xFF2A1A19) : C.card,
              borderRadius: BorderRadius.circular(18),
              border: Border.all(color: failed ? C.red.withAlpha(90) : const Color(0x14FFFFFF), width: .7),
            ),
            clipBehavior: Clip.antiAlias,
            child: Column(mainAxisSize: MainAxisSize.min, children: [
              Padding(
                padding: const EdgeInsets.fromLTRB(18, 16, 16, 16),
                child: Row(children: [
                  Expanded(
                    child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
                      Row(children: [
                        Flexible(child: Text(p.name, verbatim: true, maxLines: 1, overflow: TextOverflow.ellipsis, style: inter(17, w: FontWeight.w500))),
                        if (p.version != null) ...[
                          const SizedBox(width: 8),
                          Container(
                            padding: const EdgeInsets.symmetric(horizontal: 6, vertical: 1),
                            decoration: BoxDecoration(color: const Color(0xFF2A2A2D), borderRadius: BorderRadius.circular(6)),
                            child: Text(p.version!, verbatim: true, style: inter(12, c: const Color(0xFFB5B5BA))),
                          ),
                        ],
                      ]),
                      const SizedBox(height: 6),
                      Row(children: [
                        _chip(status, sc, sb),
                        const SizedBox(width: 8),
                        Flexible(child: Text(meta, verbatim: true, maxLines: 1, overflow: TextOverflow.ellipsis, style: inter(13, c: failed ? const Color(0xFFB98F8C) : C.muted))),
                      ]),
                    ]),
                  ),
                  const SizedBox(width: 6),
                  if (j.isActive && !j.isCancelled)
                    const IosSpinner(size: 20)
                  else if (failed)
                    // Tapping anywhere on the row opens the build; this just says what it is for.
                    Container(
                      padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 6),
                      decoration: BoxDecoration(borderRadius: BorderRadius.circular(16), border: Border.all(color: const Color(0x80FFFFFF), width: .8)),
                      child: Text('Fix it', style: inter(13, w: FontWeight.w500)),
                    )
                  else
                    const Icon(Icons.chevron_right_rounded, size: 24, color: Color(0xFF5A5A5E)),
                ]),
              ),
              if (j.isActive && !j.isCancelled)
                LinearProgressIndicator(minHeight: 3, backgroundColor: Color(0xFF2A2A2C), valueColor: AlwaysStoppedAnimation<Color>(C.teal)),
            ]),
          ),
        ),
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    return AnimatedBuilder(
      animation: Listenable.merge([Store.i.history, Store.i.historyLoaded]),
      builder: (context, _) {
        final items = Store.i.history.value;
        final loaded = Store.i.historyLoaded.value;
        final readyCount = items.where((j) => j.isDone && !j.expired).length;
        final failedCount = items.where((j) => j.isError).length;
        final groups = <String, List<Job>>{};
        for (final j in items) {
          groups.putIfAbsent(_group(j), () => []).add(j);
        }
        // The Build button and the "Recent builds" header stay put; only the history list scrolls.
        return Padding(
          padding: const EdgeInsets.fromLTRB(17, 18, 17, 0),
          child: Column(crossAxisAlignment: CrossAxisAlignment.stretch, children: [
            Pressable(
              scale: .98,
              onTap: () => showCreateSheet(context),
              child: DashedRRect(
                child: SizedBox(
                  height: 80,
                  child: Row(mainAxisAlignment: MainAxisAlignment.center, children: [
                    Container(
                      width: 42, height: 42,
                      decoration: BoxDecoration(color: C.teal, borderRadius: BorderRadius.circular(13)),
                      child: Icon(Icons.add_rounded, size: 26, color: C.onTeal),
                    ),
                    const SizedBox(width: 15),
                    Text('Build your APK', style: heading(22, w: FontWeight.w600)),
                  ]),
                ),
              ),
            ),
            if (items.isNotEmpty) ...[
              const SizedBox(height: 28),
              Padding(
                padding: const EdgeInsets.symmetric(horizontal: 3),
                child: Row(crossAxisAlignment: CrossAxisAlignment.end, children: [
                  Expanded(
                    child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
                      Text('Recent builds', style: heading(24, w: FontWeight.w700)),
                      const SizedBox(height: 4),
                      Text('${items.length} ${items.length == 1 ? 'build' : 'builds'} · $readyCount ready${failedCount > 0 ? ' · $failedCount failed' : ''}', verbatim: true, style: inter(14, c: C.muted)),
                    ]),
                  ),
                  Pressable(onTap: () => _clear(context, items), scale: .92, child: Padding(padding: const EdgeInsets.symmetric(horizontal: 4, vertical: 6), child: Text('Clear', style: inter(15, w: FontWeight.w500, c: const Color(0xFFB5B5BA))))),
                ]),
              ),
              const SizedBox(height: 14),
            ],
            Expanded(
              child: Stack(children: [
              Positioned.fill(child: CustomScrollView(
                physics: const AlwaysScrollableScrollPhysics(parent: IosScrollPhysics()),
                slivers: [
                  CupertinoSliverRefreshControl(onRefresh: () async {
                    await Store.i.refreshHistory();
                    await Store.i.refreshLimit();
                  }),
                  SliverPadding(
                    padding: EdgeInsets.only(top: items.isEmpty ? 0 : 2, bottom: 100),
                    sliver: SliverList(delegate: SliverChildListDelegate([
                    if (items.isEmpty && loaded) ...[
                      const SizedBox(height: 64),
                      Center(
                        child: Column(children: [
                          Illus(
                            'empty_builds',
                            height: 170,
                            fallback: Container(
                              width: 68, height: 68,
                              decoration: const BoxDecoration(color: C.card, shape: BoxShape.circle),
                              child: const Icon(Icons.history_rounded, size: 32, color: C.muted),
                            ),
                          ),
                          const SizedBox(height: 18),
                          Text('No builds yet', style: inter(18, w: FontWeight.w500)),
                          const SizedBox(height: 6),
                          Text('Your running and finished builds show up here.', textAlign: TextAlign.center, style: inter(14, c: C.muted, h: 1.4)),
                        ]),
                      ),
                    ],
                    for (final g in ['Today', 'Yesterday', 'Earlier'])
                      if (groups[g] != null) ...[
                        Padding(
                          padding: const EdgeInsets.fromLTRB(4, 0, 6, 8),
                          child: Row(children: [
                            Text(g, style: inter(14, c: C.muted)),
                            const Spacer(),
                            Text('${groups[g]!.length}', verbatim: true, style: inter(14, c: C.muted)),
                          ]),
                        ),
                        for (final j in groups[g]!) _row(context, j),
                        const SizedBox(height: 6),
                      ],
                    ]))),
                ],
              )),
              Positioned(
                left: 0, right: 0, bottom: 0,
                child: IgnorePointer(
                  child: Container(
                    height: 72,
                    decoration: const BoxDecoration(
                      gradient: LinearGradient(begin: Alignment.topCenter, end: Alignment.bottomCenter, colors: [Color(0x00111111), C.bg]),
                    ),
                  ),
                ),
              ),
              ]),
            ),
          ]),
        );
      },
    );
  }
}

class _NameParts {
  final String name;
  final String? version, note;
  const _NameParts(this.name, this.version, this.note);
}
