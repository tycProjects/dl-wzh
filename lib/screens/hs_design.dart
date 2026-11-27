import 'package:flutter/material.dart' hide Text;
import 'package:qr/qr.dart';
import '../anim.dart';
import '../theme.dart';
import '../tr.dart';
import '../widgets.dart';

/// Look of the Host and Share tabs: the app's own colors (dark grey, teal), grouped cards.
Color get kHsBlue => C.teal; // the app's own accent
Color get kHsGreen => C.teal;
const kHsOrange = Color(0xFFFF9F0A);
const kHsRed = Color(0xFFFF453A);
const kHsBg = C.bg;
const kHsCard = C.card;
const kHsTrack = Color(0xFF2C2C2E);
Color get kHsBarDim => Color.lerp(C.bg, C.teal, .33)!;
const kHsLine = Color(0x29FFFFFF);

/// One place for the corner shape of every Host / Share card, button and tile: Apple's smooth "continuous"
/// corner (a squircle). ContinuousRectangleBorder looks about half as round for the same number, so the
/// radius is doubled-ish to keep the size people are used to, and a little rounder than before.
ShapeBorder hsShape(double r, {BorderSide side = BorderSide.none}) =>
    ContinuousRectangleBorder(borderRadius: BorderRadius.circular(r * 1.9), side: side);

/// The iOS 27 layered-glass look for an icon tile: a soft light from the top, a thin bright rim, and a
/// little shade at the bottom, drawn over [child]. Only for tiles the app draws itself, never real logos.
class Gloss extends StatelessWidget {
  final Widget child;
  final double radius;
  const Gloss({super.key, required this.child, required this.radius});
  @override
  Widget build(BuildContext context) => Stack(fit: StackFit.passthrough, children: [
        child,
        Positioned.fill(
          child: IgnorePointer(
            child: DecoratedBox(
              decoration: ShapeDecoration(
                shape: hsShape(radius, side: const BorderSide(color: Color(0x2EFFFFFF), width: .8)),
                gradient: const LinearGradient(
                  begin: Alignment.topCenter,
                  end: Alignment.bottomCenter,
                  colors: [Color(0x38FFFFFF), Color(0x0AFFFFFF), Color(0x00000000), Color(0x24000000)],
                  stops: [0, .38, .62, 1],
                ),
              ),
            ),
          ),
        ),
      ]);
}

/// The "+" in the top bar of Host / Share tells the open tab to add something.
class HsPlus {
  static final host = ValueNotifier<int>(0);
  static final share = ValueNotifier<int>(0);
}

// ---------------------------------------------------------------- time helpers

int hsDaysSince(DateTime d) => DateTime.now().difference(d).inDays;

/// "today", "1 day ago", "3 days ago".
String hsAgo(DateTime d) {
  final n = hsDaysSince(d);
  if (n <= 0) return 'today';
  return n == 1 ? '1 day ago' : '$n days ago';
}

/// Compact "5h" / "2d" for the stats row.
String hsShort(DateTime d) {
  final diff = DateTime.now().difference(d);
  if (diff.inHours < 1) return '${diff.inMinutes.clamp(0, 59)}m';
  if (diff.inHours < 24) return '${diff.inHours}h';
  return '${diff.inDays}d';
}

const _months = ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec'];
String hsDate(DateTime d) => '${_months[d.month - 1]} ${d.day}, ${d.year}';

/// Days a hosted site has left before it is removed (7 days after the last update).
int hsDaysLeft(DateTime updated, {int total = 7}) => (total - hsDaysSince(updated)).clamp(0, total);

/// Monday-first values of the current week from a yyyy-mm-dd map, plus last week's sum.
({List<int> week, int prev}) hsWeek(Map<String, int> byDay) {
  final now = DateTime.now();
  final today = DateTime(now.year, now.month, now.day);
  final monday = today.subtract(Duration(days: today.weekday - 1));
  String k(DateTime d) => '${d.year}-${d.month.toString().padLeft(2, '0')}-${d.day.toString().padLeft(2, '0')}';
  final week = [for (var i = 0; i < 7; i++) byDay[k(monday.add(Duration(days: i)))] ?? 0];
  var prev = 0;
  for (var i = 1; i <= 7; i++) {
    prev += byDay[k(monday.subtract(Duration(days: i)))] ?? 0;
  }
  return (week: week, prev: prev);
}

// ---------------------------------------------------------------- top bar

class HsTopBar extends StatelessWidget {
  final VoidCallback onMenu, onPlus;
  const HsTopBar({super.key, required this.onMenu, required this.onPlus});
  @override
  Widget build(BuildContext context) => SizedBox(
        height: 54,
        child: Padding(
          padding: const EdgeInsets.symmetric(horizontal: 12),
          child: Row(children: [
            Pressable(scale: .88, onTap: onMenu, child: const SizedBox(width: 44, height: 44, child: Icon(Icons.settings_outlined, size: 27, color: Colors.white))),
            const Spacer(),
            Pressable(scale: .88, onTap: onPlus, child: SizedBox(width: 44, height: 44, child: Icon(Icons.add_rounded, size: 33, color: kHsBlue))),
          ]),
        ),
      );
}

// ---------------------------------------------------------------- cards and rows

class HsCardList extends StatelessWidget {
  final List<Widget> rows;
  final double dividerInset;
  const HsCardList({super.key, required this.rows, this.dividerInset = 64});
  @override
  Widget build(BuildContext context) => Container(
        decoration: BoxDecoration(color: kHsCard, borderRadius: BorderRadius.circular(18)),
        clipBehavior: Clip.antiAlias,
        child: Column(children: [
          for (var i = 0; i < rows.length; i++) ...[
            if (i > 0) Padding(padding: EdgeInsets.only(left: dividerInset), child: const Divider(height: .5, thickness: .5, color: kHsLine)),
            rows[i],
          ],
        ]),
      );
}

/// A tappable row of a [HsCardList]: icon, title, a few lines, chevron. Dims while pressed.
class HsListRow extends StatefulWidget {
  final Widget leading;
  final String title, subtitle;
  final List<Widget> lines; // extra widgets under the subtitle
  final VoidCallback? onTap;
  final Widget? trailing;
  const HsListRow({super.key, required this.leading, required this.title, required this.subtitle, this.lines = const [], this.onTap, this.trailing});
  @override
  State<HsListRow> createState() => _HsListRowState();
}

class _HsListRowState extends State<HsListRow> {
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
          padding: const EdgeInsets.fromLTRB(14, 11, 10, 11),
          child: Row(crossAxisAlignment: CrossAxisAlignment.start, children: [
            widget.leading,
            const SizedBox(width: 12),
            Expanded(
              child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
                Text(widget.title, verbatim: true, maxLines: 1, overflow: TextOverflow.ellipsis, style: inter(16, w: FontWeight.w500)),
                const SizedBox(height: 1),
                Text(widget.subtitle, verbatim: true, maxLines: 1, overflow: TextOverflow.ellipsis, style: inter(13, c: C.muted)),
                for (final l in widget.lines) ...[const SizedBox(height: 2), l],
              ]),
            ),
            const SizedBox(width: 6),
            Padding(
              padding: const EdgeInsets.only(top: 8),
              child: widget.trailing ?? const Icon(Icons.chevron_right_rounded, size: 22, color: Color(0xFF5A5A5E)),
            ),
          ]),
        ),
      );
}

/// "+ Host a site" / "+ Share an APK" row. While busy it shows a spinner, the label and the progress.
class HsAddRow extends StatelessWidget {
  final String label;
  final bool busy;
  final String busyLabel;
  final double? progress;
  final VoidCallback onTap;
  const HsAddRow({super.key, required this.label, required this.onTap, this.busy = false, this.busyLabel = '', this.progress});
  @override
  Widget build(BuildContext context) => Pressable(
        scale: .985,
        onTap: busy ? null : onTap,
        child: Container(
          color: Colors.transparent,
          padding: const EdgeInsets.fromLTRB(14, 10, 14, 10),
          child: Column(crossAxisAlignment: CrossAxisAlignment.stretch, children: [
            Row(children: [
              SizedBox(
                width: 36, height: 36,
                child: busy
                    ? const Center(child: IosSpinner(size: 20))
                    : DashedRRect(radius: 11, color: Color(0xFF48484A), fill: Colors.transparent, child: Center(child: Icon(Icons.add_rounded, size: 22, color: C.teal))),
              ),
              const SizedBox(width: 12),
              Expanded(child: Text(busy ? busyLabel : label, verbatim: busy, maxLines: 1, overflow: TextOverflow.ellipsis, style: inter(16, c: kHsBlue))),
            ]),
            if (busy && progress != null) ...[
              const SizedBox(height: 8),
              ClipRRect(
                borderRadius: BorderRadius.circular(3),
                child: LinearProgressIndicator(value: progress! >= 1 ? null : progress, minHeight: 4, color: kHsBlue, backgroundColor: kHsTrack),
              ),
            ],
          ]),
        ),
      );
}

class HsFootnote extends StatelessWidget {
  final String text;
  const HsFootnote(this.text, {super.key});
  @override
  Widget build(BuildContext context) => Padding(
        padding: const EdgeInsets.fromLTRB(6, 8, 6, 0),
        child: Text(text, style: inter(12.5, c: C.muted, h: 1.4)),
      );
}

class HsLabel extends StatelessWidget {
  final String text;
  const HsLabel(this.text, {super.key});
  @override
  Widget build(BuildContext context) => Padding(
        padding: const EdgeInsets.fromLTRB(6, 22, 6, 8),
        child: Text(text, style: inter(13, c: C.muted)),
      );
}

/// Blue rounded square with a globe: a hosted site.
class HsGlobeIcon extends StatelessWidget {
  final double size;
  const HsGlobeIcon({super.key, this.size = 36});
  @override
  Widget build(BuildContext context) => Container(
        width: size, height: size,
        decoration: BoxDecoration(color: const Color(0xFF2C2C2F), borderRadius: BorderRadius.circular(size * .32)),
        child: Icon(Icons.language_rounded, size: size * .56, color: Colors.white),
      );
}

// ---------------------------------------------------------------- weekly chart

class HsWeekChart extends StatelessWidget {
  final List<int> values; // Monday first, 7 entries
  final int? prevTotal; // last week's total; null = not known (no comparison shown)
  final String unit; // "visits", "downloads"
  const HsWeekChart({super.key, required this.values, required this.unit, this.prevTotal});

  static const _days = ['M', 'T', 'W', 'T', 'F', 'S', 'S'];

  @override
  Widget build(BuildContext context) {
    final total = values.fold<int>(0, (a, b) => a + b);
    final peak = values.fold<int>(0, (a, b) => a > b ? a : b);
    final peakAt = peak > 0 ? values.indexOf(peak) : -1;
    final today = DateTime.now().weekday - 1;
    final diff = prevTotal == null ? null : total - prevTotal!;
    const barArea = 92.0;
    return Container(
      padding: const EdgeInsets.fromLTRB(14, 12, 14, 10),
      decoration: BoxDecoration(color: kHsCard, borderRadius: BorderRadius.circular(18)),
      child: Column(crossAxisAlignment: CrossAxisAlignment.stretch, children: [
        Text('This week', style: inter(13, c: C.muted)),
        const SizedBox(height: 2),
        Row(crossAxisAlignment: CrossAxisAlignment.baseline, textBaseline: TextBaseline.alphabetic, children: [
          Text('$total', verbatim: true, style: inter(26, w: FontWeight.w600)),
          const SizedBox(width: 6),
          Text(unit, style: inter(14, c: C.muted)),
          const Spacer(),
          if (diff != null)
            Text('${diff >= 0 ? '+' : '\u2212'}${diff.abs()} vs last week', verbatim: true, style: inter(13, w: FontWeight.w500, c: diff > 0 ? kHsGreen : (diff < 0 ? kHsRed : C.muted))),
        ]),
        const SizedBox(height: 8),
        SizedBox(
          height: barArea + 20,
          child: Row(crossAxisAlignment: CrossAxisAlignment.end, children: [
            for (var i = 0; i < 7; i++) ...[
              if (i > 0) const SizedBox(width: 8),
              Expanded(
                child: Column(mainAxisAlignment: MainAxisAlignment.end, children: [
                  if (i == peakAt) Padding(padding: const EdgeInsets.only(bottom: 3), child: Text('$peak', verbatim: true, style: inter(12, w: FontWeight.w600))),
                  TweenAnimationBuilder<double>(
                    tween: Tween(begin: 0, end: peak == 0 ? 0 : values[i] / peak),
                    duration: const Duration(milliseconds: 520),
                    curve: iosCurve,
                    builder: (_, f, __) => Container(
                      height: 14 + (barArea - 34) * f,
                      decoration: BoxDecoration(color: i == today ? kHsBlue : kHsBarDim, borderRadius: BorderRadius.circular(6)),
                    ),
                  ),
                  const SizedBox(height: 5),
                  Text(_days[i], verbatim: true, style: inter(12, w: i == today ? FontWeight.w700 : FontWeight.w400, c: i == today ? Colors.white : C.muted)),
                ]),
              ),
            ],
          ]),
        ),
      ]),
    );
  }
}

// ---------------------------------------------------------------- time left before removal

class HsTimeLeftRow {
  final String name;
  final int left; // days left, 0..total
  const HsTimeLeftRow(this.name, this.left);
}

class HsTimeLeft extends StatelessWidget {
  final List<HsTimeLeftRow> rows;
  final int total;
  const HsTimeLeft({super.key, required this.rows, this.total = 7});
  @override
  Widget build(BuildContext context) => Container(
        decoration: BoxDecoration(color: kHsCard, borderRadius: BorderRadius.circular(18)),
        padding: const EdgeInsets.symmetric(horizontal: 14),
        child: Column(children: [
          for (var i = 0; i < rows.length; i++) ...[
            if (i > 0) const Divider(height: .5, thickness: .5, color: kHsLine),
            Padding(
              padding: const EdgeInsets.symmetric(vertical: 11),
              child: Column(crossAxisAlignment: CrossAxisAlignment.stretch, children: [
                Row(children: [
                  Expanded(child: Text(rows[i].name, verbatim: true, maxLines: 1, overflow: TextOverflow.ellipsis, style: inter(15, w: FontWeight.w500))),
                  Text('${rows[i].left} of $total days', verbatim: true, style: inter(13, w: rows[i].left <= 1 ? FontWeight.w600 : FontWeight.w400, c: rows[i].left <= 1 ? kHsOrange : C.muted)),
                ]),
                const SizedBox(height: 8),
                ClipRRect(
                  borderRadius: BorderRadius.circular(3),
                  child: TweenAnimationBuilder<double>(
                    tween: Tween(begin: 0, end: rows[i].left / total),
                    duration: const Duration(milliseconds: 520),
                    curve: iosCurve,
                    builder: (_, f, __) => LinearProgressIndicator(value: f, minHeight: 5, color: rows[i].left <= 1 ? kHsOrange : kHsBlue, backgroundColor: kHsTrack),
                  ),
                ),
              ]),
            ),
          ],
        ]),
      );
}

// ---------------------------------------------------------------- QR code

/// Draws a QR code for [data] on a white rounded tile.
class HsQr extends StatelessWidget {
  final String data;
  final double size;
  const HsQr(this.data, {super.key, this.size = 128});
  @override
  Widget build(BuildContext context) => Container(
        width: size, height: size,
        padding: EdgeInsets.all(size * .07),
        decoration: BoxDecoration(color: Colors.white, borderRadius: BorderRadius.circular(size * .1)),
        child: CustomPaint(painter: HsQrPainter(data)),
      );
}

class HsQrPainter extends CustomPainter {
  final String data;
  HsQrPainter(this.data);
  @override
  void paint(Canvas canvas, Size s) {
    final img = QrImage(QrCode.fromData(data: data, errorCorrectLevel: QrErrorCorrectLevel.M));
    final n = img.moduleCount;
    final cell = s.width / n;
    final p = Paint()..color = Colors.black;
    for (var r = 0; r < n; r++) {
      for (var c = 0; c < n; c++) {
        if (img.isDark(r, c)) canvas.drawRect(Rect.fromLTWH(c * cell, r * cell, cell + .4, cell + .4), p);
      }
    }
  }

  @override
  bool shouldRepaint(HsQrPainter o) => o.data != data;
}
