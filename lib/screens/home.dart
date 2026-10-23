import 'package:flutter/material.dart' hide Text;
import 'package:flutter/cupertino.dart' show CupertinoPageRoute;
import '../api.dart';
import '../store.dart';
import '../theme.dart';
import '../widgets.dart';
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

  Widget _chip(String text, Color c) => Container(
        padding: const EdgeInsets.symmetric(horizontal: 9, vertical: 3),
        decoration: BoxDecoration(color: Color.alphaBlend(c.withAlpha(38), C.card), borderRadius: BorderRadius.circular(20)),
        child: Text(text, style: inter(12, w: FontWeight.w500, c: c)),
      );

  Widget _row(BuildContext context, Job j) {
    final String status;
    final Color sc;
    if (j.isError) {
      status = 'Failed';
      sc = C.red;
    } else if (j.isDone) {
      status = j.expired ? 'Expired' : 'Ready';
      sc = j.expired ? C.muted : C.teal;
    } else if (j.isCancelled) {
      status = 'Cancelled';
      sc = C.muted;
    } else {
      status = j.status == 'queued-waiting' ? 'Waiting' : (j.status == 'awaiting-repair-confirmation' ? 'Auto-fix ready' : 'Building');
      sc = C.teal;
    }
    final canDelete = !j.isActive || j.isCancelled;
    final meta = [
      j.typeLabel,
      if (_time(j.createdAt).isNotEmpty) _time(j.createdAt),
      if (j.isDone && !j.expired && _size(j.sizeBytes).isNotEmpty) _size(j.sizeBytes),
    ].join(' · ');
    return Padding(
      padding: const EdgeInsets.only(bottom: 10),
      child: Dismissible(
        key: ValueKey(j.id),
        direction: canDelete ? DismissDirection.endToStart : DismissDirection.none,
        background: Container(
          alignment: Alignment.centerRight,
          padding: const EdgeInsets.only(right: 26),
          decoration: BoxDecoration(color: C.red, borderRadius: BorderRadius.circular(20)),
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
            decoration: BoxDecoration(color: C.card, borderRadius: BorderRadius.circular(20)),
            clipBehavior: Clip.antiAlias,
            child: Column(mainAxisSize: MainAxisSize.min, children: [
              Padding(
                padding: const EdgeInsets.fromLTRB(18, 16, 16, 16),
                child: Row(children: [
                  Expanded(
                    child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
                      Text(j.name, maxLines: 1, overflow: TextOverflow.ellipsis, style: inter(17, w: FontWeight.w500)),
                      const SizedBox(height: 6),
                      Row(children: [
                        _chip(status, sc),
                        const SizedBox(width: 8),
                        Flexible(child: Text(meta, maxLines: 1, overflow: TextOverflow.ellipsis, style: inter(13, c: C.muted))),
                      ]),
                    ]),
                  ),
                  const SizedBox(width: 6),
                  if (j.isActive && !j.isCancelled) const IosSpinner(size: 20) else const Icon(Icons.chevron_right_rounded, size: 24, color: Color(0xFF5A5A5E)),
                ]),
              ),
              if (j.isActive && !j.isCancelled)
                const LinearProgressIndicator(minHeight: 3, backgroundColor: Color(0xFF2A2A2C), valueColor: AlwaysStoppedAnimation<Color>(C.teal)),
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
        final groups = <String, List<Job>>{};
        for (final j in items) {
          groups.putIfAbsent(_group(j), () => []).add(j);
        }
        // The Build button and the "Recent builds" header stay put; only the history list scrolls.
        return Padding(
          padding: const EdgeInsets.fromLTRB(17, 85, 17, 0),
          child: Column(crossAxisAlignment: CrossAxisAlignment.stretch, children: [
            Pressable(
              scale: .98,
              onTap: () => showCreateSheet(context),
              child: DashedRRect(
                child: SizedBox(
                  height: 95,
                  child: Row(mainAxisAlignment: MainAxisAlignment.center, children: [
                    Container(
                      width: 43, height: 43,
                      decoration: const BoxDecoration(color: C.teal, shape: BoxShape.circle),
                      child: const Icon(Icons.add_rounded, size: 26, color: C.onTeal),
                    ),
                    const SizedBox(width: 17),
                    Text('Build your APK', style: inter(22, w: FontWeight.w500)),
                  ]),
                ),
              ),
            ),
            if (items.isNotEmpty) ...[
              const SizedBox(height: 44),
              Padding(
                padding: const EdgeInsets.symmetric(horizontal: 3),
                child: Row(crossAxisAlignment: CrossAxisAlignment.end, children: [
                  Expanded(
                    child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
                      Text('Recent builds', style: inter(24, w: FontWeight.w700)),
                      const SizedBox(height: 4),
                      Text('${items.length} ${items.length == 1 ? 'build' : 'builds'} · $readyCount ready', style: inter(14, c: C.muted)),
                    ]),
                  ),
                  Pressable(onTap: () => _clear(context, items), scale: .92, child: Padding(padding: const EdgeInsets.symmetric(horizontal: 4, vertical: 6), child: Text('Clear', style: inter(15, w: FontWeight.w500, c: C.teal)))),
                ]),
              ),
              const SizedBox(height: 18),
            ],
            Expanded(
              child: RefreshIndicator(
                color: C.teal,
                backgroundColor: C.card,
                onRefresh: () async {
                  await Store.i.refreshHistory();
                  await Store.i.refreshLimit();
                },
                child: ListView(
                  physics: const BouncingScrollPhysics(parent: AlwaysScrollableScrollPhysics()),
                  padding: EdgeInsets.only(top: items.isEmpty ? 0 : 2, bottom: 100),
                  children: [
                    if (items.isEmpty && loaded) ...[
                      const SizedBox(height: 64),
                      Center(
                        child: Column(children: [
                          Container(
                            width: 68, height: 68,
                            decoration: const BoxDecoration(color: C.card, shape: BoxShape.circle),
                            child: const Icon(Icons.history_rounded, size: 32, color: C.muted),
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
                        Padding(padding: const EdgeInsets.only(left: 4, bottom: 8), child: Text('$g · ${groups[g]!.length}', style: inter(14, c: C.muted))),
                        for (final j in groups[g]!) _row(context, j),
                        const SizedBox(height: 6),
                      ],
                  ],
                ),
              ),
            ),
          ]),
        );
      },
    );
  }
}
