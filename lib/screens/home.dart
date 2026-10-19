import 'package:flutter/material.dart';
import 'package:flutter/cupertino.dart' show CupertinoPageRoute;
import '../api.dart';
import '../store.dart';
import '../theme.dart';
import '../widgets.dart';
import '../anim.dart';
import 'build_page.dart';
import 'create_sheet.dart';

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

  Widget _row(BuildContext context, Job j) {
    final Widget icon;
    final String status;
    final Color sc;
    if (j.isError) {
      icon = const Icon(Icons.error_outline_rounded, size: 27, color: C.red);
      status = 'Failed';
      sc = C.red;
    } else if (j.isDone) {
      icon = Icon(j.expired ? Icons.schedule_rounded : Icons.check_circle_outline_rounded, size: 27, color: j.expired ? C.muted : C.teal);
      status = j.expired ? 'Expired' : 'Ready';
      sc = j.expired ? C.muted : C.teal;
    } else if (j.isCancelled) {
      icon = const Icon(Icons.cancel_outlined, size: 27, color: C.muted);
      status = 'Cancelled';
      sc = C.muted;
    } else {
      icon = const IosSpinner(size: 26);
      status = j.status == 'queued-waiting' ? 'Waiting…' : (j.status == 'awaiting-repair-confirmation' ? 'Auto-fix ready' : 'Building…');
      sc = C.muted;
    }
    return Padding(
      padding: const EdgeInsets.only(bottom: 10),
      child: Pressable(
        scale: .985,
        onTap: () async {
          await Navigator.of(context).push(CupertinoPageRoute(builder: (_) => BuildPage(jobId: j.id, appName: j.name)));
          Store.i.refreshHistory();
        },
        child: Container(
          height: 83,
          padding: const EdgeInsets.fromLTRB(25, 0, 22, 0),
          decoration: BoxDecoration(color: C.card, borderRadius: BorderRadius.circular(22)),
          child: Row(children: [
            SizedBox(width: 27, child: Center(child: icon)),
            const SizedBox(width: 17),
            Expanded(
              child: Column(mainAxisAlignment: MainAxisAlignment.center, crossAxisAlignment: CrossAxisAlignment.start, children: [
                Text(j.name, maxLines: 1, overflow: TextOverflow.ellipsis, style: inter(17)),
                const SizedBox(height: 5),
                Text.rich(TextSpan(children: [
                  TextSpan(text: status, style: inter(14, w: FontWeight.w500, c: sc)),
                  TextSpan(text: ' · ${j.typeLabel} · ${_time(j.createdAt)}', style: inter(14, c: C.muted)),
                ])),
              ]),
            ),
            const Icon(Icons.chevron_right_rounded, size: 26, color: Color(0xFF5A5A5E)),
          ]),
        ),
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    return ValueListenableBuilder<List<Job>>(
      valueListenable: Store.i.history,
      builder: (context, items, _) {
        final groups = <String, List<Job>>{};
        for (final j in items) {
          groups.putIfAbsent(_group(j), () => []).add(j);
        }
        return RefreshIndicator(
          color: C.teal,
          backgroundColor: C.card,
          onRefresh: () async {
            await Store.i.refreshHistory();
            await Store.i.refreshLimit();
          },
          child: ListView(
            physics: const BouncingScrollPhysics(parent: AlwaysScrollableScrollPhysics()),
            padding: const EdgeInsets.fromLTRB(17, 85, 17, 100),
            children: [
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
                const SizedBox(height: 68),
                Padding(
                  padding: const EdgeInsets.symmetric(horizontal: 3),
                  child: Row(crossAxisAlignment: CrossAxisAlignment.baseline, textBaseline: TextBaseline.alphabetic, children: [
                    Text('Recent builds', style: inter(24, w: FontWeight.w700)),
                    const Spacer(),
                    Pressable(onTap: () => _clear(context, items), scale: .92, child: Text('Clear', style: inter(15, w: FontWeight.w500, c: C.teal))),
                  ]),
                ),
                const SizedBox(height: 16),
                for (final g in ['Today', 'Yesterday', 'Earlier'])
                  if (groups[g] != null) ...[
                    Padding(padding: const EdgeInsets.only(left: 18, bottom: 6), child: Text(g, style: inter(14, c: C.muted))),
                    for (final j in groups[g]!) _row(context, j),
                    const SizedBox(height: 6),
                  ],
              ],
            ],
          ),
        );
      },
    );
  }
}
