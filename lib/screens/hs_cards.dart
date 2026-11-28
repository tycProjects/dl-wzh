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

const _kChipBg = Color(0xFF262629);
const _kSeg = Color(0xFF2E2E31);
const _kBtnBg = Color(0xFF2A2A2D);
// Same flat dark surfaces and thin lines as the code editor.
const _kPanel = Color(0xFF171717);
// iOS 27 style: translucent glass surfaces with a thin light rim.
const _kRim = Color(0x1FFFFFFF);
const _kGlassBtn = Color(0x1AFFFFFF);
const _kEdge = Color(0xFF262626);
const _kEdge2 = Color(0xFF2E2E30);

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
  final Color color;
  const HsIconTile(this.icon, {super.key, this.size = 52, this.color = const Color(0xFF3A3A3F)});
  // A layered app icon: a solid squircle with a lighter inner layer and a white symbol.
  @override
  Widget build(BuildContext context) => SizedBox(
        width: size,
        height: size,
        child: Stack(alignment: Alignment.center, children: [
          Container(decoration: ShapeDecoration(color: color, shape: hsShape(size * .22, side: const BorderSide(color: Color(0x38FFFFFF), width: .8)))),
          Positioned.fill(
            child: Padding(
              padding: EdgeInsets.all(size * .09),
              child: DecoratedBox(decoration: ShapeDecoration(color: const Color(0x26FFFFFF), shape: hsShape(size * .15))),
            ),
          ),
          Icon(icon, size: size * .5, color: Colors.white),
        ]),
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
  // Plain meta text, no chip: a status dot (when there is one) and the words.
  @override
  Widget build(BuildContext context) => Row(mainAxisSize: MainAxisSize.min, children: [
        if (dot) ...[
          Container(width: 6, height: 6, decoration: BoxDecoration(color: color, shape: BoxShape.circle)),
          const SizedBox(width: 6),
        ],
        Text(text, verbatim: verbatim, style: inter(12.5, c: dot ? C.text : color)),
      ]);
}

/// Section title with a count badge: "Your sites  1".
class HsListHeader extends StatelessWidget {
  final String title;
  final int? count;
  const HsListHeader(this.title, {super.key, this.count});
  @override
  Widget build(BuildContext context) => Padding(
        padding: const EdgeInsets.fromLTRB(6, 30, 6, 12),
        child: Row(children: [
          Text(title, style: inter(13, w: FontWeight.w500, c: C.muted)),
          if (count != null) ...[
            const SizedBox(width: 6),
            Text('$count', verbatim: true, style: inter(13, c: const Color(0xFF5F5F64))),
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
        child: const SizedBox(width: 44, height: 44, child: Center(child: Icon(Icons.more_horiz_rounded, size: 22, color: C.muted))),
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
  // Glass capsules, one per action.
  @override
  Widget build(BuildContext context) => Row(children: [
        for (var i = 0; i < items.length; i++) ...[
          if (i > 0) const SizedBox(width: 8),
          Expanded(child: _QuickButton(items[i])),
        ],
      ]);
}

class _QuickButton extends StatelessWidget {
  final HsQuick q;
  const _QuickButton(this.q);
  @override
  Widget build(BuildContext context) => Opacity(
        opacity: q.onTap == null ? .4 : 1,
        child: Pressable(
          scale: .96,
          onTap: q.onTap,
          child: Container(
            height: 44,
            padding: const EdgeInsets.symmetric(horizontal: 6),
            decoration: ShapeDecoration(color: _kGlassBtn, shape: const StadiumBorder(side: BorderSide(color: _kRim, width: .8))),
            child: Row(mainAxisAlignment: MainAxisAlignment.center, children: [
              Icon(q.icon, size: 16, color: q.color ?? const Color(0xFFC7C7CC)),
              const SizedBox(width: 5),
              Flexible(child: FittedBox(fit: BoxFit.scaleDown, child: Text(q.label, maxLines: 1, softWrap: false, style: inter(13, c: C.text)))),
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
  final Color iconColor;
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
    this.iconColor = const Color(0xFF3A3A3F),
    this.busy = false,
    this.busyLabel = '',
    this.progress,
  });

  @override
  Widget build(BuildContext context) {
    final u = used ?? 0;
    final full = used != null && u >= limit;
    final free = limit - u < 0 ? 0 : limit - u;
    final Color fg = busy ? C.tealBtn : (full ? C.muted : C.onTeal);
    return Container(
      padding: const EdgeInsets.all(18),
      decoration: BoxDecoration(
        color: C.card,
        borderRadius: BorderRadius.circular(26),
        border: Border.all(color: const Color(0xFF2C2C2E)),
      ),
      child: Column(crossAxisAlignment: CrossAxisAlignment.stretch, children: [
        Row(children: [
          Container(
            width: 54, height: 54,
            decoration: BoxDecoration(color: C.tealBtn, borderRadius: BorderRadius.circular(15)),
            child: Icon(icon, size: 29, color: C.onTeal),
          ),
          const SizedBox(width: 14),
          Expanded(child: Text(title, style: inter(21, w: FontWeight.w600, h: 1.2))),
        ]),
        const SizedBox(height: 12),
        Text(subtitle, style: inter(16, c: C.muted, h: 1.4)),
        const SizedBox(height: 22),
        Row(children: [
          for (var i = 0; i < limit; i++) ...[
            if (i > 0) const SizedBox(width: 5),
            Expanded(
              child: AnimatedContainer(
                duration: const Duration(milliseconds: 320),
                curve: iosCurve,
                height: 6,
                decoration: BoxDecoration(color: used != null && i < u ? C.tealBtn : const Color(0x24FFFFFF), borderRadius: BorderRadius.circular(3)),
              ),
            ),
          ],
        ]),
        const SizedBox(height: 10),
        Row(children: [
          Text(used == null ? 'Checking\u2026' : '$u of $limit used', style: inter(15.5, c: C.muted)),
          const Spacer(),
          if (used != null) Text(full ? 'Full' : '$free free', style: inter(15.5, w: FontWeight.w500, c: full ? const Color(0xFFF27D85) : C.text)),
        ]),
        const SizedBox(height: 20),
        Pressable(
          scale: .97,
          onTap: busy ? null : onTap,
          child: Container(
            height: 56,
            decoration: BoxDecoration(
              color: busy ? const Color(0xFF25312C) : (full ? const Color(0x1AFFFFFF) : C.tealBtn),
              borderRadius: BorderRadius.circular(28),
            ),
            clipBehavior: Clip.antiAlias,
            child: Stack(alignment: Alignment.center, children: [
              if (busy && progress != null)
                Positioned.fill(
                  child: Align(
                    alignment: Alignment.centerLeft,
                    child: FractionallySizedBox(widthFactor: progress!.clamp(0.0, 1.0), child: Container(color: const Color(0xFF2E5A4B))),
                  ),
                ),
              Row(mainAxisSize: MainAxisSize.min, children: [
                if (busy) const IosSpinner(size: 18) else Icon(full ? Icons.lock_outline_rounded : buttonIcon, size: 23, color: fg),
                const SizedBox(width: 8),
                Flexible(child: Text(busy ? busyLabel : (full ? 'All slots used' : buttonLabel), maxLines: 1, overflow: TextOverflow.ellipsis, style: inter(17.5, w: FontWeight.w600, c: fg))),
              ]),
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
      decoration: BoxDecoration(
        color: C.card,
        borderRadius: BorderRadius.circular(26),
        border: Border.all(color: const Color(0xFF2C2C2E)),
      ),
      clipBehavior: Clip.antiAlias,
      child: Column(crossAxisAlignment: CrossAxisAlignment.stretch, children: [
        Pressable(
          scale: .99,
          onTap: onTap,
          child: Padding(
            padding: const EdgeInsets.fromLTRB(18, 18, 8, 14),
            child: Row(children: [
              leading,
              const SizedBox(width: 16),
              Expanded(
                child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
                  Text(title, verbatim: true, maxLines: 1, overflow: TextOverflow.ellipsis, style: inter(16, w: FontWeight.w600)),
                  const SizedBox(height: 4),
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
                  padding: const EdgeInsets.fromLTRB(18, 2, 18, 6),
                  child: Wrap(spacing: 18, runSpacing: 8, children: pills),
                ),
              if (notice != null)
                Padding(
                  padding: const EdgeInsets.fromLTRB(14, 10, 14, 0),
                  child: Container(
                    padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 9),
                    decoration: BoxDecoration(color: kHsWarnBg, borderRadius: BorderRadius.circular(8)),
                    child: Row(crossAxisAlignment: CrossAxisAlignment.start, children: [
                      const Padding(padding: EdgeInsets.only(top: 1), child: Icon(Icons.schedule_rounded, size: 16, color: kHsOrange)),
                      const SizedBox(width: 8),
                      Expanded(child: Text(notice!, verbatim: true, style: inter(12.5, c: const Color(0xFFFFC266), h: 1.3))),
                    ]),
                  ),
                ),
              Padding(
                padding: const EdgeInsets.fromLTRB(16, 14, 16, 18),
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
          decoration: BoxDecoration(color: kHsRed, borderRadius: BorderRadius.circular(26)),
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
          padding: const EdgeInsets.all(12),
          decoration: BoxDecoration(color: C.card, borderRadius: BorderRadius.circular(26), border: Border.all(color: const Color(0xFF2C2C2E))),
          child: Column(children: [
            Row(children: [
              Container(width: 40, height: 40, decoration: BoxDecoration(color: _kSeg, borderRadius: BorderRadius.circular(11))),
              const SizedBox(width: 12),
              Expanded(child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [_bar(130, 13, 5), const SizedBox(height: 8), _bar(190, 10, 5)])),
            ]),
            const SizedBox(height: 12),
            Row(children: [
              for (var i = 0; i < 3; i++) ...[
                if (i > 0) const SizedBox(width: 6),
                Expanded(child: Container(height: 38, decoration: BoxDecoration(color: _kSeg, borderRadius: BorderRadius.circular(10)))),
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
        padding: const EdgeInsets.fromLTRB(28, 26, 28, 8),
        child: Column(mainAxisSize: MainAxisSize.min, children: [
          Illus(
            illus,
            height: 110,
            fallback: Container(
              width: 56,
              height: 56,
              decoration: BoxDecoration(color: _kPanel, borderRadius: BorderRadius.circular(16), border: Border.all(color: _kEdge)),
              child: Icon(icon, size: 26, color: C.muted),
            ),
          ),
          const SizedBox(height: 14),
          Text(title, textAlign: TextAlign.center, style: inter(16, w: FontWeight.w600)),
          const SizedBox(height: 4),
          Text(message, textAlign: TextAlign.center, style: inter(13.5, c: C.muted, h: 1.4)),
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
