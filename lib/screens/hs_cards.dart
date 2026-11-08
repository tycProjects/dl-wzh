import 'dart:async';
import 'package:flutter/material.dart' hide Text;
import '../anim.dart';
import '../illus.dart';
import '../theme.dart';
import '../tr.dart';
import 'hostshare_ui.dart' show HsPulse;
import 'hs_design.dart';

/// Components of the Host and Share tabs, iOS style: a header card with the slot meter and the
/// main button, item cards with three quick actions, swipe to delete, an action sheet, and
/// calm empty / loading states.

const _kHair = Color(0x14FFFFFF);
const _kChipBg = Color(0xFF262629);
const _kSeg = Color(0xFF2E2E31);
const _kBtnBg = Color(0xFF2A2A2D);

/// Soft green wash behind "Live" style labels.
Color get kHsLiveBg => C.teal.withAlpha(0x24);

/// Soft amber wash behind "running out" labels.
const kHsWarnBg = Color(0x26FF9F0A);

// ---------------------------------------------------------------- entrance

/// Fades and lifts its child in once, when it first appears. [index] staggers a list;
/// a negative [index] shows the child right away (it has been seen before).
class HsAppear extends StatefulWidget {
  final Widget child;
  final int index;
  const HsAppear({super.key, required this.child, this.index = 0});
  @override
  State<HsAppear> createState() => _HsAppearState();
}

class _HsAppearState extends State<HsAppear> with SingleTickerProviderStateMixin {
  late final AnimationController _c = AnimationController(vsync: this, duration: const Duration(milliseconds: 460));
  late final Animation<double> _a = CurvedAnimation(parent: _c, curve: iosCurve);
  Timer? _t;

  @override
  void initState() {
    super.initState();
    if (widget.index < 0) {
      _c.value = 1;
      return;
    }
    final i = widget.index > 6 ? 6 : widget.index;
    if (i == 0) {
      _c.forward();
    } else {
      _t = Timer(Duration(milliseconds: i * 55), () {
        if (mounted) _c.forward();
      });
    }
  }

  @override
  void dispose() {
    _t?.cancel();
    _c.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) => FadeTransition(
        opacity: _a,
        child: SlideTransition(
          position: Tween<Offset>(begin: const Offset(0, .035), end: Offset.zero).animate(_a),
          child: widget.child,
        ),
      );
}

// ---------------------------------------------------------------- small pieces

/// Flat dark-grey tile with a white icon (the header card, the sheets).
class HsIconTile extends StatelessWidget {
  final IconData icon;
  final double size;
  const HsIconTile(this.icon, {super.key, this.size = 52});
  @override
  Widget build(BuildContext context) => Container(
        width: size,
        height: size,
        decoration: ShapeDecoration(
          color: const Color(0xFF2C2C2F),
          shape: hsShape(size * .28),
        ),
        child: Icon(icon, size: size * .52, color: Colors.white),
      );
}

/// Small rounded label: a live dot or an icon, then the text.
class HsPill extends StatelessWidget {
  final String text;
  final IconData? icon;
  final bool dot;
  final bool verbatim;
  final Color color, bg;
  const HsPill(this.text, {super.key, this.icon, this.dot = false, this.verbatim = false, this.color = C.muted, this.bg = _kChipBg});
  @override
  Widget build(BuildContext context) => Container(
        padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 5),
        decoration: BoxDecoration(color: bg, borderRadius: BorderRadius.circular(20)),
        child: Row(mainAxisSize: MainAxisSize.min, children: [
          if (dot) ...[
            Container(
              width: 6,
              height: 6,
              decoration: BoxDecoration(color: color, shape: BoxShape.circle, boxShadow: [BoxShadow(color: color.withOpacity(.55), blurRadius: 5)]),
            ),
            const SizedBox(width: 6),
          ] else if (icon != null) ...[
            Icon(icon, size: 14, color: color),
            const SizedBox(width: 5),
          ],
          Text(text, verbatim: verbatim, style: inter(12.5, w: FontWeight.w500, c: color)),
        ]),
      );
}

/// Section title with a count badge: "Your sites  1".
class HsListHeader extends StatelessWidget {
  final String title;
  final int? count;
  const HsListHeader(this.title, {super.key, this.count});
  @override
  Widget build(BuildContext context) => Padding(
        padding: const EdgeInsets.fromLTRB(6, 28, 6, 12),
        child: Row(children: [
          Text(title, style: inter(21, w: FontWeight.w700)),
          if (count != null) ...[
            const SizedBox(width: 10),
            Container(
              padding: const EdgeInsets.symmetric(horizontal: 9, vertical: 2),
              decoration: BoxDecoration(color: _kBtnBg, borderRadius: BorderRadius.circular(10)),
              child: Text('$count', verbatim: true, style: inter(13, w: FontWeight.w600, c: C.muted)),
            ),
          ],
        ]),
      );
}

/// The round "..." button at the right of a card.
class HsMoreButton extends StatelessWidget {
  final VoidCallback onTap;
  const HsMoreButton({super.key, required this.onTap});
  @override
  Widget build(BuildContext context) => Pressable(
        scale: .88,
        onTap: onTap,
        child: SizedBox(
          width: 44,
          height: 44,
          child: Center(
            child: Container(
              width: 30,
              height: 30,
              decoration: const BoxDecoration(color: _kBtnBg, shape: BoxShape.circle),
              child: const Icon(Icons.more_horiz_rounded, size: 19, color: C.text),
            ),
          ),
        ),
      );
}

class HsChevron extends StatelessWidget {
  const HsChevron({super.key});
  @override
  Widget build(BuildContext context) => const SizedBox(
        width: 36,
        height: 44,
        child: Center(child: Icon(Icons.chevron_right_rounded, size: 26, color: Color(0xFF5A5A5E))),
      );
}

// ---------------------------------------------------------------- quick actions

class HsQuick {
  final IconData icon;
  final String label;
  final VoidCallback? onTap; // null = greyed out
  final Color? color;
  const HsQuick(this.icon, this.label, this.onTap, {this.color});
}

/// Equal-width buttons with the icon above the label, like the action row of an iOS contact card.
class HsQuickRow extends StatelessWidget {
  final List<HsQuick> items;
  final Color bg;
  const HsQuickRow(this.items, {super.key, this.bg = _kBtnBg});
  @override
  Widget build(BuildContext context) => Row(children: [
        for (var i = 0; i < items.length; i++) ...[
          if (i > 0) const SizedBox(width: 8),
          Expanded(child: _QuickButton(items[i], bg)),
        ],
      ]);
}

class _QuickButton extends StatelessWidget {
  final HsQuick q;
  final Color bg;
  const _QuickButton(this.q, this.bg);
  @override
  Widget build(BuildContext context) => Opacity(
        opacity: q.onTap == null ? .4 : 1,
        child: Pressable(
          scale: .94,
          onTap: q.onTap,
          child: Container(
            height: 60,
            decoration: ShapeDecoration(color: bg, shape: hsShape(16)),
            child: Column(mainAxisAlignment: MainAxisAlignment.center, children: [
              Icon(q.icon, size: 21, color: q.color ?? C.teal),
              const SizedBox(height: 4),
              Padding(
                padding: const EdgeInsets.symmetric(horizontal: 4),
                child: FittedBox(
                  fit: BoxFit.scaleDown,
                  child: Text(q.label, maxLines: 1, softWrap: false, style: inter(12.5, w: FontWeight.w500)),
                ),
              ),
            ]),
          ),
        ),
      );
}

// ---------------------------------------------------------------- header card

/// Top card of a tab: icon, title, a slot meter (one capsule per slot), and the main button.
/// The button works right away, even while the list is still loading.
class HsHeroCard extends StatelessWidget {
  final IconData icon, buttonIcon;
  final String title, subtitle, buttonLabel;
  final int? used; // null = still loading
  final int limit;
  final bool busy;
  final String busyLabel;
  final double? progress;
  final VoidCallback onTap;
  const HsHeroCard({
    super.key,
    required this.icon,
    required this.title,
    required this.subtitle,
    required this.buttonLabel,
    required this.used,
    required this.limit,
    required this.onTap,
    this.buttonIcon = Icons.add_rounded,
    this.busy = false,
    this.busyLabel = '',
    this.progress,
  });

  @override
  Widget build(BuildContext context) {
    final u = used ?? 0;
    final full = used != null && u >= limit;
    final free = limit - u < 0 ? 0 : limit - u;
    final Color fg = busy ? C.teal : (full ? C.muted : C.onTeal);
    return Container(
      padding: const EdgeInsets.all(18),
      decoration: ShapeDecoration(
        gradient: const LinearGradient(begin: Alignment.topLeft, end: Alignment.bottomRight, colors: [Color(0xFF252528), Color(0xFF1B1B1D)]),
        shape: hsShape(28, side: const BorderSide(color: _kHair, width: .7)),
      ),
      child: Column(crossAxisAlignment: CrossAxisAlignment.stretch, children: [
        Row(children: [
          HsIconTile(icon, size: 52),
          const SizedBox(width: 14),
          Expanded(
            child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
              Text(title, style: inter(21, w: FontWeight.w700)),
              const SizedBox(height: 2),
              Text(subtitle, style: inter(13.5, c: C.muted, h: 1.35)),
            ]),
          ),
        ]),
        const SizedBox(height: 20),
        Row(children: [
          for (var i = 0; i < limit; i++) ...[
            if (i > 0) const SizedBox(width: 6),
            Expanded(
              child: AnimatedContainer(
                duration: const Duration(milliseconds: 320),
                curve: iosCurve,
                height: 8,
                decoration: BoxDecoration(
                  color: used != null && i < u ? C.teal : _kSeg,
                  borderRadius: BorderRadius.circular(4),
                ),
              ),
            ),
          ],
        ]),
        const SizedBox(height: 9),
        Row(children: [
          Text(used == null ? 'Checking\u2026' : '$u of $limit used', style: inter(13, c: C.muted)),
          const Spacer(),
          if (used != null) Text(full ? 'Full' : '$free free', style: inter(13, w: FontWeight.w600, c: full ? const Color(0xFFF27D85) : C.teal)),
        ]),
        const SizedBox(height: 18),
        Pressable(
          scale: .97,
          onTap: busy ? null : onTap,
          child: Container(
            height: 54,
            decoration: ShapeDecoration(
              color: busy ? const Color(0xFF25312C) : (full ? const Color(0xFF2A2A2D) : C.teal),
              shape: hsShape(27),
            ),
            clipBehavior: Clip.antiAlias,
            child: Stack(children: [
              if (busy && progress != null)
                Positioned.fill(
                  child: Align(
                    alignment: Alignment.centerLeft,
                    child: FractionallySizedBox(widthFactor: progress!.clamp(0.0, 1.0), child: Container(color: const Color(0xFF2E5A4B))),
                  ),
                ),
              Center(
                child: Row(mainAxisSize: MainAxisSize.min, children: [
                  if (busy) const IosSpinner(size: 18) else Icon(full ? Icons.lock_outline_rounded : buttonIcon, size: 22, color: fg),
                  const SizedBox(width: 9),
                  Flexible(
                    child: Text(busy ? busyLabel : (full ? 'All slots used' : buttonLabel), maxLines: 1, overflow: TextOverflow.ellipsis, style: inter(17, w: FontWeight.w700, c: fg)),
                  ),
                ]),
              ),
            ]),
          ),
        ),
      ]),
    );
  }
}

// ---------------------------------------------------------------- item card

/// One site or link: picture, name, address, small labels, and three quick actions.
/// While something runs on it (replace, delete) the labels and buttons give way to the progress.
class HsCard extends StatelessWidget {
  final Widget leading;
  final String title, subtitle;
  final Widget? trailing;
  final VoidCallback? onTap; // tapping the top part of the card
  final List<Widget> pills;
  final String? notice; // amber line under the labels
  final List<HsQuick> quick;
  final bool busy;
  final String busyLabel;
  final double? progress;
  const HsCard({
    super.key,
    required this.leading,
    required this.title,
    required this.subtitle,
    required this.quick,
    this.trailing,
    this.onTap,
    this.pills = const [],
    this.notice,
    this.busy = false,
    this.busyLabel = '',
    this.progress,
  });

  @override
  Widget build(BuildContext context) {
    return Container(
      decoration: ShapeDecoration(color: C.card, shape: hsShape(24, side: const BorderSide(color: _kHair, width: .7))),
      clipBehavior: Clip.antiAlias,
      child: Column(crossAxisAlignment: CrossAxisAlignment.stretch, children: [
        Pressable(
          scale: .99,
          onTap: onTap,
          child: Padding(
            padding: const EdgeInsets.fromLTRB(14, 14, 6, 10),
            child: Row(children: [
              leading,
              const SizedBox(width: 13),
              Expanded(
                child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
                  Text(title, verbatim: true, maxLines: 1, overflow: TextOverflow.ellipsis, style: inter(17.5, w: FontWeight.w600)),
                  const SizedBox(height: 2),
                  Text(subtitle, verbatim: true, maxLines: 1, overflow: TextOverflow.ellipsis, style: inter(13, c: C.muted)),
                ]),
              ),
              trailing ?? const SizedBox(width: 8),
            ]),
          ),
        ),
        AnimatedSize(
          duration: const Duration(milliseconds: 260),
          curve: iosCurve,
          alignment: Alignment.topCenter,
          child: Column(mainAxisSize: MainAxisSize.min, crossAxisAlignment: CrossAxisAlignment.stretch, children: [
            if (busy)
              Padding(
                padding: const EdgeInsets.fromLTRB(14, 2, 14, 16),
                child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
                  Text(busyLabel, style: inter(13, w: FontWeight.w500, c: C.teal)),
                  if (progress != null) ...[
                    const SizedBox(height: 8),
                    ClipRRect(
                      borderRadius: BorderRadius.circular(3),
                      child: LinearProgressIndicator(value: progress! >= 1 ? null : progress, minHeight: 5, color: C.teal, backgroundColor: _kSeg),
                    ),
                  ],
                ]),
              )
            else ...[
              if (pills.isNotEmpty)
                Padding(
                  padding: const EdgeInsets.symmetric(horizontal: 14),
                  child: Wrap(spacing: 7, runSpacing: 7, children: pills),
                ),
              if (notice != null)
                Padding(
                  padding: const EdgeInsets.fromLTRB(14, 10, 14, 0),
                  child: Container(
                    padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 9),
                    decoration: ShapeDecoration(color: kHsWarnBg, shape: hsShape(12)),
                    child: Row(crossAxisAlignment: CrossAxisAlignment.start, children: [
                      const Padding(padding: EdgeInsets.only(top: 1), child: Icon(Icons.schedule_rounded, size: 16, color: kHsOrange)),
                      const SizedBox(width: 8),
                      Expanded(child: Text(notice!, verbatim: true, style: inter(12.5, c: const Color(0xFFFFC266), h: 1.3))),
                    ]),
                  ),
                ),
              Padding(
                padding: const EdgeInsets.fromLTRB(12, 12, 12, 12),
                child: HsQuickRow(quick),
              ),
            ],
          ]),
        ),
      ]),
    );
  }
}

// ---------------------------------------------------------------- swipe to delete

/// Swipe a card to the left to delete it, like a row in iOS Mail. The card always snaps back;
/// [onDelete] asks for confirmation and removes it from the list.
class HsSwipeDelete extends StatelessWidget {
  final String id;
  final Widget child;
  final bool enabled;
  final Future<void> Function() onDelete;
  const HsSwipeDelete({super.key, required this.id, required this.child, required this.onDelete, this.enabled = true});

  @override
  Widget build(BuildContext context) => Dismissible(
        key: ValueKey('swipe-$id'),
        direction: enabled ? DismissDirection.endToStart : DismissDirection.none,
        dismissThresholds: const {DismissDirection.endToStart: .3},
        confirmDismiss: (_) async {
          await onDelete();
          return false;
        },
        background: Container(
          alignment: Alignment.centerRight,
          padding: const EdgeInsets.only(right: 26),
          decoration: ShapeDecoration(color: kHsRed, shape: hsShape(24)),
          child: Column(mainAxisSize: MainAxisSize.min, children: [
            const Icon(Icons.delete_outline_rounded, size: 26, color: Colors.white),
            const SizedBox(height: 2),
            Text('Delete', style: inter(12, w: FontWeight.w600, c: Colors.white)),
          ]),
        ),
        child: child,
      );
}

// ---------------------------------------------------------------- loading and empty

/// Placeholder shaped like an item card; pulses softly. Also stands in for a card being created.
class HsCardSkeleton extends StatelessWidget {
  const HsCardSkeleton({super.key});
  Widget _bar(double w, double h, double r) => Container(width: w, height: h, decoration: BoxDecoration(color: _kSeg, borderRadius: BorderRadius.circular(r)));
  @override
  Widget build(BuildContext context) => HsPulse(
        child: Container(
          padding: const EdgeInsets.all(14),
          decoration: ShapeDecoration(color: C.card, shape: hsShape(24)),
          child: Column(children: [
            Row(children: [
              Container(width: 52, height: 52, decoration: ShapeDecoration(color: _kSeg, shape: hsShape(14))),
              const SizedBox(width: 13),
              Expanded(child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [_bar(130, 14, 6), const SizedBox(height: 9), _bar(190, 11, 5)])),
            ]),
            const SizedBox(height: 14),
            Row(children: [
              for (var i = 0; i < 3; i++) ...[
                if (i > 0) const SizedBox(width: 8),
                Expanded(child: Container(height: 60, decoration: ShapeDecoration(color: _kSeg, shape: hsShape(16)))),
              ],
            ]),
          ]),
        ),
      );
}

/// Nothing here yet: the picture, a title and one line. No box around it, like iOS.
class HsEmptyState extends StatelessWidget {
  final String illus, title, message;
  final IconData icon;
  const HsEmptyState({super.key, required this.illus, required this.icon, required this.title, required this.message});
  @override
  Widget build(BuildContext context) => Padding(
        padding: const EdgeInsets.fromLTRB(28, 34, 28, 8),
        child: Column(mainAxisSize: MainAxisSize.min, children: [
          Illus(
            illus,
            height: 140,
            fallback: Container(
              width: 64,
              height: 64,
              decoration: const BoxDecoration(color: C.card, shape: BoxShape.circle),
              child: Icon(icon, size: 30, color: C.muted),
            ),
          ),
          const SizedBox(height: 18),
          Text(title, textAlign: TextAlign.center, style: inter(19, w: FontWeight.w600)),
          const SizedBox(height: 6),
          Text(message, textAlign: TextAlign.center, style: inter(14, c: C.muted, h: 1.4)),
        ]),
      );
}

// ---------------------------------------------------------------- action sheet

class HsSheetItem {
  final IconData icon;
  final String text, value;
  final bool danger;
  const HsSheetItem(this.icon, this.text, this.value, {this.danger = false});
}

/// iOS action sheet: a card with the item on top and the actions under it, then a separate
/// Cancel card. Returns the chosen item's value, or null.
Future<String?> showHsActionSheet(
  BuildContext context, {
  required Widget leading,
  required String title,
  required String subtitle,
  required List<HsSheetItem> items,
}) {
  return showIosSheet<String>(context, (ctx) {
    final bottom = MediaQuery.of(ctx).padding.bottom;
    Widget row(HsSheetItem it, bool last) => Pressable(
          scale: .985,
          onTap: () => Navigator.pop(ctx, it.value),
          child: Container(
            height: 54,
            padding: const EdgeInsets.symmetric(horizontal: 16),
            decoration: BoxDecoration(border: last ? null : const Border(bottom: BorderSide(color: Color(0x1FFFFFFF), width: .5))),
            child: Row(children: [
              Icon(it.icon, size: 22, color: it.danger ? kHsRed : C.teal),
              const SizedBox(width: 14),
              Expanded(child: Text(it.text, style: inter(17, c: it.danger ? kHsRed : C.text))),
            ]),
          ),
        );
    return Container(
      width: double.infinity,
      padding: EdgeInsets.fromLTRB(10, 0, 10, 8 + bottom),
      child: Column(mainAxisSize: MainAxisSize.min, crossAxisAlignment: CrossAxisAlignment.stretch, children: [
        Container(
          decoration: ShapeDecoration(color: const Color(0xFF262628), shape: hsShape(22)),
          clipBehavior: Clip.antiAlias,
          child: Column(children: [
            Padding(
              padding: const EdgeInsets.fromLTRB(16, 16, 16, 14),
              child: Row(children: [
                leading,
                const SizedBox(width: 12),
                Expanded(
                  child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
                    Text(title, verbatim: true, maxLines: 1, overflow: TextOverflow.ellipsis, style: inter(17, w: FontWeight.w600)),
                    const SizedBox(height: 2),
                    Text(subtitle, verbatim: true, maxLines: 1, overflow: TextOverflow.ellipsis, style: inter(13, c: C.muted)),
                  ]),
                ),
              ]),
            ),
            Container(height: .5, color: const Color(0x29FFFFFF)),
            for (var i = 0; i < items.length; i++) row(items[i], i == items.length - 1),
          ]),
        ),
        const SizedBox(height: 8),
        Pressable(
          scale: .98,
          onTap: () => Navigator.pop(ctx),
          child: Container(
            height: 56,
            alignment: Alignment.center,
            decoration: ShapeDecoration(color: const Color(0xFF262628), shape: hsShape(22)),
            child: Text('Cancel', style: inter(17, w: FontWeight.w600, c: C.teal)),
          ),
        ),
      ]),
    );
  });
}
