import 'dart:math' as math;
import 'package:flutter/material.dart' hide Text;
import '../anim.dart';
import '../theme.dart';
import '../widgets.dart';
import '../illus.dart';
import '../tr.dart';
import '../appearance.dart';
import 'hs_design.dart' show hsShape;

/// Shared look of the Host and Share tabs: a header card with the slot meter and the main
/// button, item cards with quick actions, skeleton cards while loading, empty and error states.
const _kTileBg = Color(0xFF2C2C2F);
const _kTileBorder = Color(0xFF2C2C2F);
const _kPill = Color(0xFF262629);
const _kOff = Color(0xFF2E2E31);

/// Rounded letter tile (the site / app initial).
class HsTile extends StatelessWidget {
  final String letter;
  final double size;
  final String logoUrl; // real picture; the letter tile shows until it loads, or if there is none
  const HsTile(this.letter, {super.key, this.size = 46, this.logoUrl = ''});
  @override
  Widget build(BuildContext context) {
    final t = letter.trim();
    final tile = _letter(t);
    if (logoUrl.isEmpty) return tile;
    return SizedBox(
      width: size, height: size,
      child: ClipPath(
        clipper: ShapeBorderClipper(shape: hsShape(size * .28)),
        child: Unfilter(
          child: Image.network(
            logoUrl,
            width: size, height: size,
            fit: BoxFit.cover,
            cacheWidth: 160,
            gaplessPlayback: true,
            errorBuilder: (_, __, ___) => tile,
            loadingBuilder: (_, child, p) => p == null ? child : tile,
          ),
        ),
      ),
    );
  }

  Widget _letter(String t) {
    return Container(
      width: size, height: size,
      alignment: Alignment.center,
      decoration: ShapeDecoration(
        color: Color(0xFF2C2C2F),
        shape: hsShape(size * .28),
      ),
      child: Text(t.isEmpty ? 'A' : t.substring(0, 1).toUpperCase(), verbatim: true, style: inter(size * .46, w: FontWeight.w700, c: Colors.white)),
    );
  }
}

/// Small rounded label, with an optional icon or a live dot.
class HsChip extends StatelessWidget {
  final IconData? icon;
  final bool dot;
  final String text;
  final Color color;
  const HsChip(this.text, {super.key, this.icon, this.dot = false, this.color = C.muted});
  @override
  Widget build(BuildContext context) => Container(
        padding: const EdgeInsets.symmetric(horizontal: 9, vertical: 4),
        decoration: BoxDecoration(color: _kPill, borderRadius: BorderRadius.circular(20)),
        child: Row(mainAxisSize: MainAxisSize.min, children: [
          if (dot) ...[
            Container(width: 6, height: 6, decoration: BoxDecoration(color: color, shape: BoxShape.circle)),
            const SizedBox(width: 6),
          ] else if (icon != null) ...[
            Icon(icon, size: 14, color: color),
            const SizedBox(width: 5),
          ],
          Text(text, style: inter(12.5, w: FontWeight.w500, c: color)),
        ]),
      );
}

class HsAct {
  final IconData icon;
  final String label;
  final VoidCallback onTap;
  const HsAct(this.icon, this.label, this.onTap);
}

/// Header card: icon + title, slot meter, and the main button. The button works right away,
/// even while the list is still loading.
class HsHero extends StatelessWidget {
  final IconData icon;
  final bool showIcon; // false = no icon tile, the title and text use the full width
  final String title, subtitle, buttonLabel;
  final IconData buttonIcon;
  final int? used; // null = still loading
  final int limit;
  final bool busy;
  final String busyLabel;
  final double? progress;
  final VoidCallback onTap;
  const HsHero({
    super.key,
    required this.icon,
    required this.title,
    required this.subtitle,
    required this.buttonLabel,
    required this.buttonIcon,
    required this.used,
    required this.limit,
    required this.onTap,
    this.busy = false,
    this.busyLabel = '',
    this.progress,
    this.showIcon = true,
  });

  @override
  Widget build(BuildContext context) {
    final u = used ?? 0;
    final full = used != null && u >= limit;
    final free = limit - u;
    final Color bg = busy ? const Color(0xFF25312C) : (full ? const Color(0xFF2A2A2D) : C.tealBtn);
    final Color fg = busy ? C.teal : (full ? C.muted : C.onTeal);
    return Container(
      padding: const EdgeInsets.all(18),
      decoration: BoxDecoration(color: C.card, borderRadius: BorderRadius.circular(26)),
      child: Column(crossAxisAlignment: CrossAxisAlignment.stretch, children: [
        Row(children: [
          if (showIcon) ...[
            Container(
              width: 48, height: 48,
              decoration: BoxDecoration(color: _kTileBg, borderRadius: BorderRadius.circular(16), border: Border.all(color: _kTileBorder)),
              child: Icon(icon, size: 25, color: Colors.white),
            ),
            const SizedBox(width: 14),
          ],
          Expanded(
            child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
              Text(title, style: inter(20, w: FontWeight.w700)),
              const SizedBox(height: 2),
              Text(subtitle, style: inter(13.5, c: C.muted, h: 1.35)),
            ]),
          ),
        ]),
        const SizedBox(height: 18),
        Row(children: [
          for (var i = 0; i < limit; i++) ...[
            if (i > 0) const SizedBox(width: 5),
            Expanded(
              child: AnimatedContainer(
                duration: const Duration(milliseconds: 250),
                height: 6,
                decoration: BoxDecoration(color: used != null && i < u ? C.teal : _kOff, borderRadius: BorderRadius.circular(3)),
              ),
            ),
          ],
        ]),
        const SizedBox(height: 8),
        Row(children: [
          Text(used == null ? 'Checking\u2026' : '$u of $limit used', style: inter(13, c: C.muted)),
          const Spacer(),
          if (used != null) Text(full ? 'Full' : '$free free', style: inter(13, w: FontWeight.w600, c: full ? const Color(0xFFF27D85) : C.teal)),
        ]),
        const SizedBox(height: 16),
        Pressable(
          scale: .98,
          onTap: busy ? null : onTap,
          child: ClipRRect(
            borderRadius: BorderRadius.circular(27),
            child: Container(
              height: 54,
              color: bg,
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
                    Text(busy ? busyLabel : (full ? 'All slots used' : buttonLabel), style: inter(17, w: FontWeight.w700, c: fg)),
                  ]),
                ),
              ]),
            ),
          ),
        ),
      ]),
    );
  }
}

class HsSection extends StatelessWidget {
  final String title;
  final int? count;
  const HsSection(this.title, {super.key, this.count});
  @override
  Widget build(BuildContext context) => Padding(
        padding: const EdgeInsets.fromLTRB(4, 0, 4, 0),
        child: Row(children: [
          Text(title, style: inter(18, w: FontWeight.w600)),
          if (count != null) ...[
            const SizedBox(width: 8),
            Text('$count', verbatim: true, style: inter(15, c: C.muted)),
          ],
        ]),
      );
}

/// One site or link: tile + name + address, chips, and three quick actions.
class HsItemCard extends StatelessWidget {
  final String letter, title, subtitle;
  final String logoUrl;
  final List<Widget> chips;
  final List<HsAct> actions;
  final bool busy;
  final String busyLabel;
  final double? progress;
  final VoidCallback onMore;
  const HsItemCard({
    super.key,
    required this.letter,
    required this.title,
    required this.subtitle,
    required this.chips,
    required this.actions,
    required this.onMore,
    this.logoUrl = '',
    this.busy = false,
    this.busyLabel = '',
    this.progress,
  });

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.fromLTRB(14, 14, 8, 14),
      decoration: BoxDecoration(color: C.card, borderRadius: BorderRadius.circular(22)),
      child: Column(crossAxisAlignment: CrossAxisAlignment.stretch, children: [
        Row(children: [
          HsTile(letter, logoUrl: logoUrl),
          const SizedBox(width: 13),
          Expanded(
            child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
              Text(title, verbatim: true, maxLines: 1, overflow: TextOverflow.ellipsis, style: inter(17, w: FontWeight.w600)),
              const SizedBox(height: 2),
              Text(subtitle, verbatim: true, maxLines: 1, overflow: TextOverflow.ellipsis, style: inter(13, c: C.muted)),
            ]),
          ),
          if (!busy)
            Pressable(
              scale: .85,
              onTap: onMore,
              child: const SizedBox(width: 44, height: 44, child: Icon(Icons.more_horiz_rounded, size: 24, color: C.muted)),
            )
          else
            const SizedBox(width: 44, height: 44, child: Center(child: IosSpinner(size: 20))),
        ]),
        Padding(
          padding: const EdgeInsets.only(right: 6),
          child: Column(crossAxisAlignment: CrossAxisAlignment.stretch, children: [
            const SizedBox(height: 12),
            if (busy) ...[
              Text(busyLabel, style: inter(13, w: FontWeight.w500, c: C.teal)),
              if (progress != null) ...[
                const SizedBox(height: 8),
                ClipRRect(
                  borderRadius: BorderRadius.circular(3),
                  child: LinearProgressIndicator(value: progress! >= 1 ? null : progress, minHeight: 5, color: C.teal, backgroundColor: _kOff),
                ),
              ],
              const SizedBox(height: 2),
            ] else ...[
              if (chips.isNotEmpty) ...[
                Wrap(spacing: 7, runSpacing: 7, children: chips),
                const SizedBox(height: 12),
              ],
              Row(children: [
                for (var i = 0; i < actions.length; i++) ...[
                  if (i > 0) const SizedBox(width: 8),
                  Expanded(child: _ActBtn(actions[i])),
                ],
              ]),
            ],
          ]),
        ),
      ]),
    );
  }
}

class _ActBtn extends StatelessWidget {
  final HsAct a;
  const _ActBtn(this.a);
  @override
  Widget build(BuildContext context) => Pressable(
        scale: .95,
        onTap: a.onTap,
        child: Container(
          height: 42,
          alignment: Alignment.center,
          decoration: BoxDecoration(color: _kPill, borderRadius: BorderRadius.circular(14)),
          // Scales down a hair instead of wrapping or cutting the label, so all three buttons
          // stay on one line at any phone width.
          child: Padding(
            padding: const EdgeInsets.symmetric(horizontal: 6),
            child: FittedBox(
              fit: BoxFit.scaleDown,
              child: Row(mainAxisSize: MainAxisSize.min, children: [
                Icon(a.icon, size: 16, color: C.teal),
                const SizedBox(width: 5),
                Text(a.label, maxLines: 1, softWrap: false, style: inter(13, w: FontWeight.w500)),
              ]),
            ),
          ),
        ),
      );
}

/// Soft pulse for placeholders (cheap: one fade, no shaders).
class HsPulse extends StatefulWidget {
  final Widget child;
  const HsPulse({super.key, required this.child});
  @override
  State<HsPulse> createState() => _HsPulseState();
}

class _HsPulseState extends State<HsPulse> with SingleTickerProviderStateMixin {
  late final AnimationController _c = AnimationController(vsync: this, duration: const Duration(milliseconds: 1100))..repeat(reverse: true);
  late final Animation<double> _a = Tween<double>(begin: .45, end: 1).animate(CurvedAnimation(parent: _c, curve: Curves.easeInOut));
  @override
  void dispose() {
    _c.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) => FadeTransition(opacity: _a, child: widget.child);
}

class HsSkeletonCard extends StatelessWidget {
  const HsSkeletonCard({super.key});
  Widget _bar(double w, double h, [double r = 6]) => Container(width: w, height: h, decoration: BoxDecoration(color: _kOff, borderRadius: BorderRadius.circular(r)));
  @override
  Widget build(BuildContext context) => HsPulse(
        child: Container(
          padding: const EdgeInsets.all(14),
          decoration: BoxDecoration(color: C.card, borderRadius: BorderRadius.circular(22)),
          child: Column(children: [
            Row(children: [
              _bar(46, 46, 15),
              const SizedBox(width: 13),
              Expanded(child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [_bar(130, 14), const SizedBox(height: 9), _bar(190, 11)])),
            ]),
            const SizedBox(height: 14),
            Row(children: [
              Expanded(child: _bar(double.infinity, 42, 14)),
              const SizedBox(width: 8),
              Expanded(child: _bar(double.infinity, 42, 14)),
              const SizedBox(width: 8),
              Expanded(child: _bar(double.infinity, 42, 14)),
            ]),
          ]),
        ),
      );
}

class HsEmpty extends StatelessWidget {
  Widget _emptyIcon() => Container(
        width: 56, height: 56,
        decoration: const BoxDecoration(color: _kPill, shape: BoxShape.circle),
        child: Icon(icon, size: 27, color: C.muted),
      );
  final IconData icon;
  final String title, message;
  final String illus; // file name (without .png) in assets/illustrations/
  const HsEmpty({super.key, required this.icon, required this.title, required this.message, this.illus = ''});
  @override
  Widget build(BuildContext context) => DashedRRect(
        radius: 22,
        color: const Color(0xFF4A4A4E),
        child: Padding(
          padding: const EdgeInsets.fromLTRB(22, 26, 22, 26),
          child: Center(
            child: Column(mainAxisSize: MainAxisSize.min, children: [
              if (illus.isEmpty) _emptyIcon() else Illus(illus, height: 150, fallback: _emptyIcon()),
              const SizedBox(height: 14),
              Text(title, style: inter(17, w: FontWeight.w600)),
              const SizedBox(height: 5),
              Text(message, textAlign: TextAlign.center, style: inter(13.5, c: C.muted, h: 1.4)),
            ]),
          ),
        ),
      );
}

class HsError extends StatelessWidget {
  final String message;
  final VoidCallback onRetry;
  const HsError({super.key, required this.message, required this.onRetry});
  @override
  Widget build(BuildContext context) => Container(
        padding: const EdgeInsets.fromLTRB(20, 22, 20, 18),
        decoration: BoxDecoration(color: C.card, borderRadius: BorderRadius.circular(22)),
        child: Column(children: [
          Container(
            width: 52, height: 52,
            decoration: const BoxDecoration(color: _kPill, shape: BoxShape.circle),
            child: const Icon(Icons.cloud_off_rounded, size: 26, color: C.muted),
          ),
          const SizedBox(height: 12),
          Text(message, textAlign: TextAlign.center, style: inter(14, c: C.muted, h: 1.4)),
          const SizedBox(height: 14),
          Pressable(
            scale: .94,
            onTap: onRetry,
            child: Container(
              height: 42,
              padding: const EdgeInsets.symmetric(horizontal: 22),
              alignment: Alignment.center,
              decoration: BoxDecoration(color: _kPill, borderRadius: BorderRadius.circular(21)),
              child: Row(mainAxisSize: MainAxisSize.min, children: [
                const Icon(Icons.refresh_rounded, size: 18, color: const Color(0xFF0A84FF)),
                const SizedBox(width: 7),
                Text('Try again', style: inter(15, w: FontWeight.w600, c: const Color(0xFF0A84FF))),
              ]),
            ),
          ),
        ]),
      );
}

/// Shown under the skeleton when the server is slow to answer (it sleeps when idle).
class HsSlowHint extends StatelessWidget {
  const HsSlowHint({super.key});
  @override
  Widget build(BuildContext context) => Padding(
        padding: const EdgeInsets.fromLTRB(8, 14, 8, 0),
        child: Text('Waking up the server. This can take up to a minute.', textAlign: TextAlign.center, style: inter(13, c: C.muted, h: 1.4)),
      );
}

/// Amber notice under the hero card: unused hosted sites are removed automatically.
class HsWarning extends StatelessWidget {
  const HsWarning({super.key});
  @override
  Widget build(BuildContext context) => Container(
        padding: const EdgeInsets.fromLTRB(14, 13, 16, 13),
        decoration: BoxDecoration(
          color: const Color(0xFF2A2417),
          borderRadius: BorderRadius.circular(18),
          border: Border.all(color: const Color(0xFF4A3D1F)),
        ),
        child: Row(crossAxisAlignment: CrossAxisAlignment.start, children: [
          const Padding(
            padding: EdgeInsets.only(top: 1),
            child: Icon(Icons.warning_amber_rounded, size: 21, color: Color(0xFFE8B04A)),
          ),
          const SizedBox(width: 11),
          Expanded(
            child: Text(
              'Heads up: sites that aren\u{2019}t updated for 7 days are deleted automatically. Tap Update on a site to keep it.',
              style: inter(13.5, c: const Color(0xFFE8CFA0), h: 1.4),
            ),
          ),
        ]),
      );
}

// ---------------------------------------------------------------------------
// Host tab, iOS style: a widget-like card with a slot ring, then an inset grouped list.
// ---------------------------------------------------------------------------

/// Card at the top of the Host tab: a ring with one segment per slot (white = used), the count,
/// and the main button. The button works right away, even while the list is still loading.
class HsSlotCard extends StatelessWidget {
  final int? used; // null = still loading
  final int limit;
  final bool busy;
  final String busyLabel;
  final VoidCallback onTap;
  final String title, unit, buttonLabel;
  final IconData buttonIcon;
  final bool titleFirst; // true = the title is the big line and the count sits under it (Share tab)
  const HsSlotCard({
    super.key,
    required this.used,
    required this.limit,
    required this.onTap,
    this.busy = false,
    this.busyLabel = '',
    this.title = 'Free hosting',
    this.unit = 'sites',
    this.buttonLabel = 'Host a site',
    this.buttonIcon = Icons.add_circle_outline_rounded,
    this.titleFirst = false,
  });

  @override
  Widget build(BuildContext context) {
    final u = used ?? 0;
    final full = used != null && u >= limit;
    final free = (limit - u).clamp(0, limit);
    final Color bg = busy ? const Color(0xFF25312C) : (full ? const Color(0xFF2A2A2D) : C.tealBtn);
    final Color fg = busy ? C.teal : (full ? C.muted : C.onTeal);
    final count = used == null ? 'Checking\u2026' : '$u of $limit $unit';
    return Container(
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        borderRadius: BorderRadius.circular(24),
        gradient: const LinearGradient(begin: Alignment(-.35, -1), end: Alignment(.35, 1), colors: [Color(0xFF2A2A2D), Color(0xFF1A1A1C)]),
        border: Border.all(color: const Color(0x1AFFFFFF), width: .6),
        boxShadow: const [BoxShadow(color: Color(0x8C000000), blurRadius: 28, offset: Offset(0, 10))],
      ),
      child: Row(children: [
        SizedBox(
          width: 76, height: 76,
          child: TweenAnimationBuilder<double>(
            tween: Tween(begin: 0, end: u.toDouble()),
            duration: const Duration(milliseconds: 520),
            curve: iosCurve,
            builder: (_, fill, __) => CustomPaint(
              painter: _SlotRingPainter(limit, fill),
              child: Center(
                child: Column(mainAxisSize: MainAxisSize.min, children: [
                  Text(used == null ? '\u2013' : '$free', verbatim: true, style: inter(24, w: FontWeight.w500).copyWith(height: 1)),
                  const SizedBox(height: 2),
                  Text('free', style: inter(11, c: C.muted)),
                ]),
              ),
            ),
          ),
        ),
        const SizedBox(width: 16),
        Expanded(
          child: Column(crossAxisAlignment: CrossAxisAlignment.start, mainAxisSize: MainAxisSize.min, children: [
            if (titleFirst) ...[
              Text(title, maxLines: 1, overflow: TextOverflow.ellipsis, style: inter(22, w: FontWeight.w600)),
              const SizedBox(height: 2),
              Text(count, verbatim: true, style: inter(12.5, c: C.muted)),
            ] else ...[
              Text(title, style: inter(12.5, c: C.muted)),
              const SizedBox(height: 2),
              Text(count, verbatim: true, style: inter(17, w: FontWeight.w500)),
            ],
            const SizedBox(height: 12),
            Pressable(
              scale: .97,
              onTap: busy || full ? null : onTap,
              child: Container(
                height: 40,
                alignment: Alignment.center,
                decoration: BoxDecoration(color: bg, borderRadius: BorderRadius.circular(12)),
                child: Row(mainAxisSize: MainAxisSize.min, children: [
                  if (busy) const IosSpinner(size: 16) else Icon(full ? Icons.lock_outline_rounded : buttonIcon, size: 19, color: fg),
                  const SizedBox(width: 7),
                  Flexible(child: Text(busy ? busyLabel : (full ? 'All slots used' : buttonLabel), maxLines: 1, overflow: TextOverflow.ellipsis, style: inter(15, w: FontWeight.w500, c: fg))),
                ]),
              ),
            ),
          ]),
        ),
      ]),
    );
  }
}

class _SlotRingPainter extends CustomPainter {
  final int limit;
  final double fill; // how many segments are filled (animated, can be fractional)
  _SlotRingPainter(this.limit, this.fill);

  @override
  void paint(Canvas canvas, Size s) {
    const stroke = 7.0;
    final rect = Rect.fromCircle(center: s.center(Offset.zero), radius: s.width / 2 - stroke / 2 - 1);
    final n = limit < 1 ? 1 : limit;
    // Each segment is a round-capped arc; the gap leaves room for the caps.
    final gap = n == 1 ? 0.0 : .34;
    final sweep = (2 * math.pi - gap * n) / n;
    for (var i = 0; i < n; i++) {
      final t = (fill - i).clamp(0.0, 1.0);
      final paint = Paint()
        ..style = PaintingStyle.stroke
        ..strokeWidth = stroke
        ..strokeCap = n == 1 ? StrokeCap.butt : StrokeCap.round
        ..color = Color.lerp(const Color(0xFF38383A), const Color(0xFFF2F2F7), t)!;
      final start = -math.pi / 2 + gap / 2 + i * (sweep + gap);
      canvas.drawArc(rect, start, sweep, false, paint);
    }
  }

  @override
  bool shouldRepaint(_SlotRingPainter o) => o.fill != fill || o.limit != limit;
}

/// Small grey label above a grouped list, like an iOS section header.
class HsGroupLabel extends StatelessWidget {
  final String text;
  const HsGroupLabel(this.text, {super.key});
  @override
  Widget build(BuildContext context) => Padding(
        padding: const EdgeInsets.fromLTRB(16, 26, 16, 7),
        child: Text(tr(text).toUpperCase(), verbatim: true, style: inter(12.5, c: C.muted).copyWith(letterSpacing: .2)),
      );
}

/// Rounded inset group of rows with hairline dividers, inset past the leading avatar.
class HsGroup extends StatelessWidget {
  final List<Widget> cells;
  final double dividerInset;
  const HsGroup({super.key, required this.cells, this.dividerInset = 62});
  @override
  Widget build(BuildContext context) => Container(
        decoration: BoxDecoration(color: C.card, borderRadius: BorderRadius.circular(14)),
        clipBehavior: Clip.antiAlias,
        child: Column(children: [
          for (var i = 0; i < cells.length; i++) ...[
            if (i > 0) Padding(padding: EdgeInsets.only(left: dividerInset), child: const Divider(height: .5, thickness: .5, color: Color(0x29FFFFFF))),
            cells[i],
          ],
        ]),
      );
}

/// One row of an [HsGroup]. Highlights while pressed, like a table row on iOS.
class HsCell extends StatefulWidget {
  final Widget leading;
  final String title, subtitle;
  final Color titleColor;
  final Widget? trailing;
  final VoidCallback? onTap;
  final String? meta; // optional third, smaller line
  final double minHeight;
  const HsCell({super.key, required this.leading, required this.title, required this.subtitle, this.titleColor = C.text, this.trailing, this.onTap, this.meta, this.minHeight = 60});
  @override
  State<HsCell> createState() => _HsCellState();
}

class _HsCellState extends State<HsCell> {
  bool _down = false;
  @override
  Widget build(BuildContext context) => GestureDetector(
        behavior: HitTestBehavior.opaque,
        onTapDown: widget.onTap == null ? null : (_) => setState(() => _down = true),
        onTapUp: (_) => setState(() => _down = false),
        onTapCancel: () => setState(() => _down = false),
        onTap: widget.onTap,
        child: AnimatedContainer(
          duration: const Duration(milliseconds: 120),
          color: _down ? const Color(0xFF2C2C2E) : Colors.transparent,
          constraints: BoxConstraints(minHeight: widget.minHeight),
          padding: const EdgeInsets.fromLTRB(14, 11, 14, 11),
          child: Row(children: [
            widget.leading,
            const SizedBox(width: 12),
            Expanded(
              child: Column(crossAxisAlignment: CrossAxisAlignment.start, mainAxisSize: MainAxisSize.min, children: [
                Text(widget.title, verbatim: true, maxLines: 1, overflow: TextOverflow.ellipsis, style: inter(16, c: widget.titleColor)),
                const SizedBox(height: 1),
                Text(widget.subtitle, verbatim: true, maxLines: 1, overflow: TextOverflow.ellipsis, style: inter(13, c: C.muted)),
                if (widget.meta != null) ...[
                  const SizedBox(height: 2),
                  Text(widget.meta!, verbatim: true, maxLines: 1, overflow: TextOverflow.ellipsis, style: inter(12.5, c: C.muted)),
                ],
              ]),
            ),
            if (widget.trailing != null) ...[const SizedBox(width: 8), widget.trailing!],
          ]),
        ),
      );
}

/// Round grey initial, like iOS Contacts. [empty] draws a dashed circle with a plus instead.
class HsAvatar extends StatelessWidget {
  final String letter;
  final bool empty;
  const HsAvatar(this.letter, {super.key, this.empty = false});
  @override
  Widget build(BuildContext context) {
    if (empty) {
      return const SizedBox(
        width: 36, height: 36,
        child: DashedRRect(radius: 18, color: Color(0xFF48484A), child: Center(child: Icon(Icons.add_rounded, size: 18, color: Color(0xFF636366)))),
      );
    }
    final t = letter.trim();
    return Container(
      width: 36, height: 36,
      alignment: Alignment.center,
      decoration: const BoxDecoration(shape: BoxShape.circle, gradient: LinearGradient(begin: Alignment.topLeft, end: Alignment.bottomRight, colors: [Color(0xFF4A4A4F), Color(0xFF2F2F33)])),
      child: Text(t.isEmpty ? 'A' : t.substring(0, 1).toUpperCase(), verbatim: true, style: inter(16, w: FontWeight.w500, c: const Color(0xFFE5E5EA))),
    );
  }
}

/// An app icon as it looks on a phone: a flat rounded square. Shows the real logo when there is
/// one; otherwise a solid color (picked from the name, so it stays the same) with the first letter.
class HsAppIcon extends StatelessWidget {
  final String name;
  final String logoUrl;
  final double size;
  const HsAppIcon(this.name, {super.key, this.logoUrl = '', this.size = 48});

  static const _palette = [
    Color(0xFF6C5CE7), Color(0xFFE8A33D), Color(0xFF2FA4A9),
    Color(0xFFD9534F), Color(0xFF4C8DF6), Color(0xFF8E6BD9),
  ];

  Widget _letter() {
    final t = name.trim();
    final c = _palette[t.runes.fold<int>(0, (a, b) => a + b) % _palette.length];
    return Container(
      width: size, height: size,
      alignment: Alignment.center,
      decoration: ShapeDecoration(color: c, shape: hsShape(size * .26)),
      child: Text(t.isEmpty ? 'A' : t.substring(0, 1).toUpperCase(), verbatim: true, style: inter(size * .46, w: FontWeight.w600, c: Colors.white)),
    );
  }

  @override
  Widget build(BuildContext context) {
    final tile = _letter();
    if (logoUrl.isEmpty) return tile;
    return SizedBox(
      width: size, height: size,
      child: ClipPath(
        clipper: ShapeBorderClipper(shape: hsShape(size * .26)),
        child: Unfilter(
          child: Image.network(
            logoUrl,
            width: size, height: size,
            fit: BoxFit.cover,
            cacheWidth: 160,
            gaplessPlayback: true,
            errorBuilder: (_, __, ___) => tile,
            loadingBuilder: (_, child, p) => p == null ? child : tile,
          ),
        ),
      ),
    );
  }
}
