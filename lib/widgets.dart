import 'dart:ui' show PathMetric;
import 'package:flutter/material.dart' hide Text;
import 'package:flutter/services.dart' show HapticFeedback;
import 'theme.dart';
import 'store_auth.dart';
import 'store_cloud.dart';
import 'anim.dart';
import 'store.dart';
import 'notices.dart';
import 'banner.dart';
import 'tr.dart';
import 'appearance.dart';
import 'screens/prefs_sheets.dart';
import 'screens/updates_page.dart';
import 'screens/account_page.dart';
import 'package:flutter/cupertino.dart' show CupertinoPageRoute;

/// Dashed rounded rectangle border (the "Build your APK" card).
class DashedRRect extends StatelessWidget {
  final Widget child;
  final double radius;
  final Color color;
  final Color fill;
  const DashedRRect({super.key, required this.child, this.radius = 24, this.color = const Color(0xFFC9C9C9), this.fill = C.card});
  @override
  Widget build(BuildContext context) => CustomPaint(
        painter: _DashPainter(radius, color, fill),
        child: child,
      );
}

class _DashPainter extends CustomPainter {
  final double r;
  final Color color, fill;
  _DashPainter(this.r, this.color, this.fill);
  @override
  void paint(Canvas canvas, Size s) {
    final rr = RRect.fromRectAndRadius(Rect.fromLTWH(1, 1, s.width - 2, s.height - 2), Radius.circular(r));
    canvas.drawRRect(rr, Paint()..color = fill);
    final path = Path()..addRRect(rr);
    final p = Paint()..color = color..style = PaintingStyle.stroke..strokeWidth = 2;
    for (final PathMetric m in path.computeMetrics()) {
      double d = 0;
      while (d < m.length) {
        canvas.drawPath(m.extractPath(d, d + 6), p);
        d += 10;
      }
    }
  }
  @override
  bool shouldRepaint(_) => false;
}

/// Corner brackets around the HTML / ZIP tiles.
/// Picking a tile cross-fades its fill, text and brackets (about 220 ms) and gives the newly
/// chosen tile a small springy "settle" so the switch feels alive instead of snapping.
class BracketTile extends StatefulWidget {
  final String title, sub;
  final bool on;
  final VoidCallback onTap;
  const BracketTile({super.key, required this.title, required this.sub, required this.on, required this.onTap});
  @override
  State<BracketTile> createState() => _BracketTileState();
}

class _BracketTileState extends State<BracketTile> with SingleTickerProviderStateMixin {
  late final AnimationController _pop =
      AnimationController(vsync: this, duration: const Duration(milliseconds: 420), value: 1);

  @override
  void didUpdateWidget(BracketTile old) {
    super.didUpdateWidget(old);
    if (widget.on && !old.on) _pop.forward(from: 0);
  }

  @override
  void dispose() {
    _pop.dispose();
    super.dispose();
  }

  static const _fade = Duration(milliseconds: 220);

  @override
  Widget build(BuildContext context) {
    final on = widget.on;
    return Pressable(
      onTap: widget.onTap,
      scale: .95,
      child: AnimatedBuilder(
        animation: _pop,
        builder: (_, child) => Transform.scale(
          scale: .93 + .07 * Curves.easeOutBack.transform(_pop.value),
          child: child,
        ),
        child: TweenAnimationBuilder<Color?>(
          tween: ColorTween(end: on ? const Color(0xFFBDEFD9) : const Color(0xFFA2A2A6)),
          duration: _fade,
          curve: Curves.easeOut,
          builder: (_, bracket, child) => CustomPaint(foregroundPainter: _Brackets(bracket ?? const Color(0xFFA2A2A6)), child: child),
          child: AnimatedContainer(
            duration: _fade,
            curve: Curves.easeOut,
            height: 50,
            decoration: BoxDecoration(color: on ? C.teal : C.field, borderRadius: BorderRadius.circular(6)),
            child: Column(mainAxisAlignment: MainAxisAlignment.center, children: [
              AnimatedDefaultTextStyle(
                duration: _fade,
                curve: Curves.easeOut,
                style: inter(16, w: FontWeight.w700, c: on ? C.onTeal : Colors.white, h: 1.2),
                child: Text(widget.title, maxLines: 1, textAlign: TextAlign.center),
              ),
              AnimatedDefaultTextStyle(
                duration: _fade,
                curve: Curves.easeOut,
                style: inter(12, c: on ? const Color(0xFF1F6B55) : const Color(0xFFA2A2A6), h: 1.2),
                child: Text(widget.sub, maxLines: 1, textAlign: TextAlign.center),
              ),
            ]),
          ),
        ),
      ),
    );
  }
}

class _Brackets extends CustomPainter {
  final Color color;
  const _Brackets(this.color);
  @override
  void paint(Canvas c, Size s) {
    final p = Paint()..color = color..style = PaintingStyle.stroke..strokeWidth = 2..strokeCap = StrokeCap.round;
    const l = 14.0, o = 2.0;
    final w = s.width, h = s.height;
    Path corner(double x, double y, double dx, double dy) =>
        Path()..moveTo(x + dx * l, y)..lineTo(x, y)..lineTo(x, y + dy * l);
    c.drawPath(corner(-o, -o, 1, 1), p);
    c.drawPath(corner(w + o, -o, -1, 1), p);
    c.drawPath(corner(-o, h + o, 1, -1), p);
    c.drawPath(corner(w + o, h + o, -1, -1), p);
  }
  @override
  bool shouldRepaint(_Brackets old) => old.color != color;
}

/// Pill segmented control with a thumb that slides like iOS.
class Seg extends StatelessWidget {
  final List<String> labels;
  final List<IconData>? icons;
  final int index;
  final ValueChanged<int> onChanged;
  final double height, radius;
  final bool bordered;
  final Color bg;
  final double? pad, thumbRadius;
  final double thumbInset, fontSize;
  const Seg({super.key, required this.labels, this.icons, required this.index, required this.onChanged,
      this.height = 54, this.radius = 22, this.bordered = false, this.bg = C.field,
      this.pad, this.thumbRadius, this.thumbInset = 0, this.fontSize = 16});
  @override
  Widget build(BuildContext context) => Container(
        height: height,
        padding: EdgeInsets.all(pad ?? (bordered ? 5 : 6)),
        decoration: BoxDecoration(
          color: bg, borderRadius: BorderRadius.circular(radius),
          border: bordered ? Border.all(color: const Color(0xFF333333)) : null),
        child: LayoutBuilder(builder: (c, box) {
          final w = box.maxWidth / labels.length;
          return Stack(children: [
            AnimatedPositioned(
              duration: const Duration(milliseconds: 300),
              curve: iosCurve,
              left: w * index + thumbInset, top: thumbInset, bottom: thumbInset, width: w - thumbInset * 2,
              child: Container(
                decoration: BoxDecoration(
                  color: bordered ? const Color(0xFF1F4A40) : const Color(0xFF333333),
                  borderRadius: BorderRadius.circular(thumbRadius ?? (bordered ? 15 : 17)),
                  border: bordered ? Border.all(color: C.teal, width: 1.5) : null),
              ),
            ),
            Row(children: [
              for (var i = 0; i < labels.length; i++)
                Expanded(
                  child: Pressable(
                    scale: .94,
                    haptic: false,
                    onTap: () => onChanged(i),
                    child: Center(
                      child: TweenAnimationBuilder<Color?>(
                        tween: ColorTween(end: i == index ? C.teal : (bordered ? const Color(0xFFD8D8DC) : Colors.white)),
                        duration: const Duration(milliseconds: 210),
                        builder: (c, col, _) => Padding(
                          padding: const EdgeInsets.symmetric(horizontal: 8),
                          child: FittedBox(
                            fit: BoxFit.scaleDown,
                            child: Row(mainAxisSize: MainAxisSize.min, children: [
                              if (icons != null) ...[Icon(icons![i], size: fontSize + 2, color: col), const SizedBox(width: 5)],
                              Text(labels[i], style: inter(fontSize, w: bordered ? FontWeight.w500 : FontWeight.w700, c: col ?? C.text)),
                            ]),
                          ),
                        ),
                      ),
                    ),
                  ),
                ),
            ]),
          ]);
        }),
      );
}

/// Bot floating button (bottom right).
class BotFab extends StatelessWidget {
  final VoidCallback? onTap;
  const BotFab({super.key, this.onTap});
  @override
  Widget build(BuildContext context) {
    final face = RepaintBoundary(
      child: Container(
        width: 54, height: 54,
        decoration: const BoxDecoration(color: Color(0xFF2C2C2E), shape: BoxShape.circle),
        child: CustomPaint(painter: _BotFace()),
      ),
    );
    return onTap == null ? face : Pressable(scale: .9, onTap: onTap, child: face);
  }
}

/// The tappable "Help" button on the home screen: a mint pill with a chat bubble, so it reads as
/// "message us" at a glance. A soft ring pulses until the chat has been opened once ([pulse]).
class ChatFab extends StatefulWidget {
  final VoidCallback onTap;
  final bool pulse;
  const ChatFab({super.key, required this.onTap, this.pulse = true});
  @override
  State<ChatFab> createState() => _ChatFabState();
}

class _ChatFabState extends State<ChatFab> with SingleTickerProviderStateMixin {
  late final AnimationController _c = AnimationController(vsync: this, duration: const Duration(milliseconds: 2400));

  @override
  void initState() {
    super.initState();
    if (widget.pulse) _c.repeat();
  }

  @override
  void didUpdateWidget(ChatFab old) {
    super.didUpdateWidget(old);
    if (widget.pulse && !_c.isAnimating) _c.repeat();
    if (!widget.pulse && _c.isAnimating) _c.stop();
  }

  @override
  void dispose() {
    _c.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final pill = Container(
      height: 44,
      padding: const EdgeInsets.fromLTRB(12, 0, 16, 0),
      decoration: BoxDecoration(
        color: const Color(0xFF262626),
        borderRadius: BorderRadius.circular(22),
        border: Border.all(color: const Color(0xFF333335), width: 1),
        boxShadow: const [BoxShadow(color: Color(0x66000000), blurRadius: 8, offset: Offset(0, 3))],
      ),
      child: Row(mainAxisSize: MainAxisSize.min, children: [
        SizedBox(width: 22, height: 22, child: CustomPaint(painter: _ChatBubbleIcon(C.teal))),
        const SizedBox(width: 8),
        Text('Help', style: inter(15.5, w: FontWeight.w600)),
      ]),
    );
    return Pressable(
      scale: .93,
      onTap: widget.onTap,
      child: RepaintBoundary(
        child: Stack(clipBehavior: Clip.none, alignment: Alignment.center, children: [
          if (widget.pulse)
            Positioned.fill(
              child: IgnorePointer(
                child: AnimatedBuilder(
                  animation: _c,
                  builder: (_, __) {
                    final t = Curves.easeOut.transform(_c.value);
                    return Opacity(
                      opacity: (1 - t) * .7,
                      child: Transform.scale(
                        scaleX: 1 + .3 * t,
                        scaleY: 1 + .55 * t,
                        child: Container(
                          decoration: BoxDecoration(
                            borderRadius: BorderRadius.circular(22),
                            border: Border.all(color: C.teal, width: 2),
                          ),
                        ),
                      ),
                    );
                  },
                ),
              ),
            ),
          pill,
        ]),
      ),
    );
  }
}

class _ChatBubbleIcon extends CustomPainter {
  final Color? color;
  const _ChatBubbleIcon([this.color]);
  @override
  void paint(Canvas c, Size s) {
    final k = s.width / 24;
    final body = Path()
      ..moveTo(5 * k, 4 * k)
      ..lineTo(19 * k, 4 * k)
      ..arcToPoint(Offset(21 * k, 6 * k), radius: Radius.circular(2 * k))
      ..lineTo(21 * k, 15 * k)
      ..arcToPoint(Offset(19 * k, 17 * k), radius: Radius.circular(2 * k))
      ..lineTo(12 * k, 17 * k)
      ..lineTo(7 * k, 21 * k)
      ..lineTo(7 * k, 17 * k)
      ..lineTo(5 * k, 17 * k)
      ..arcToPoint(Offset(3 * k, 15 * k), radius: Radius.circular(2 * k))
      ..lineTo(3 * k, 6 * k)
      ..arcToPoint(Offset(5 * k, 4 * k), radius: Radius.circular(2 * k))
      ..close();
    c.drawPath(body, Paint()..color = (color ?? C.teal).withOpacity(.14));
    c.drawPath(
      body,
      Paint()
        ..color = color ?? C.teal
        ..style = PaintingStyle.stroke
        ..strokeWidth = 2.1 * k
        ..strokeJoin = StrokeJoin.round,
    );
    final dot = Paint()..color = color ?? C.teal;
    for (final x in [8.5, 12.0, 15.5]) {
      c.drawCircle(Offset(x * k, 10.5 * k), 1.15 * k, dot);
    }
  }

  @override
  bool shouldRepaint(_ChatBubbleIcon o) => o.color != color;
}

class _BotFace extends CustomPainter {
  const _BotFace();
  @override
  void paint(Canvas c, Size s) {
    final fill = Paint()..color = C.teal;
    final st = Paint()..color = C.teal..style = PaintingStyle.stroke..strokeWidth = 2.4..strokeCap = StrokeCap.round;
    final cx = s.width / 2, cy = s.height / 2;
    c.drawCircle(Offset(cx - 5, cy - 5), 1.8, fill);
    c.drawCircle(Offset(cx + 5, cy - 5), 1.8, fill);
    c.drawPath(Path()..moveTo(cx - 6, cy + 2)..quadraticBezierTo(cx, cy + 8, cx + 6, cy + 2), st);
    c.drawLine(Offset(cx - 9, cy + 7), Offset(cx - 10, cy + 10), st);
    c.drawLine(Offset(cx + 9, cy + 7), Offset(cx + 10, cy + 10), st);
  }
  @override
  bool shouldRepaint(_) => true;
}

/// Round account picture: first letter of the signed-in email on a colour picked from the email
/// (like Alight Motion's top-right profile circle). Signed out: grey circle with a person icon.
class AccountAvatar extends StatefulWidget {
  final double size;
  const AccountAvatar({super.key, this.size = 34});
  static const _palette = [
    Color(0xFF5136AC), Color(0xFF1F7A8C), Color(0xFFB5446E), Color(0xFF2E7D4F), Color(0xFFC25E2B), Color(0xFF3F5FBF),
  ];
  @override
  State<AccountAvatar> createState() => _AccountAvatarState();
}

class _AccountAvatarState extends State<AccountAvatar> {
  @override
  void initState() {
    super.initState();
    if (StoreAuth.signedIn) StoreCloud.refreshAvatar();
  }

  @override
  Widget build(BuildContext context) => ValueListenableBuilder<int>(
        valueListenable: StoreAuth.changes,
        builder: (_, __, ___) {
          final size = widget.size;
          final email = StoreAuth.email.trim();
          final on = StoreAuth.signedIn;
          final letter = email.isNotEmpty ? String.fromCharCode(email.runes.first).toUpperCase() : '';
          if (!on || letter.isEmpty) {
            return Container(
              width: size, height: size,
              alignment: Alignment.center,
              decoration: const BoxDecoration(color: Color(0xFF3A3A3F), shape: BoxShape.circle),
              child: Icon(Icons.person_rounded, size: size * .64, color: const Color(0xFF9A9AA0)),
            );
          }
          var h = 0;
          for (final r in email.toLowerCase().runes) { h = (h * 31 + r) & 0x7fffffff; }
          final letterCircle = Container(
            width: size, height: size,
            alignment: Alignment.center,
            decoration: BoxDecoration(color: AccountAvatar._palette[h % AccountAvatar._palette.length], shape: BoxShape.circle),
            child: Text(letter, verbatim: true, style: inter(size * .5, w: FontWeight.w600, c: Colors.white)),
          );
          final url = StoreCloud.avatarUrl(StoreCloud.myAvatarPath);
          if (url.isEmpty) return letterCircle;
          return Container(
            width: size, height: size,
            clipBehavior: Clip.antiAlias,
            decoration: const BoxDecoration(shape: BoxShape.circle),
            child: Unfilter(child: Image.network(url, width: size, height: size, fit: BoxFit.cover, cacheWidth: (size * 3).round(), gaplessPlayback: true, errorBuilder: (_, __, ___) => letterCircle)),
          );
        },
      );
}

class TopBar extends StatelessWidget {
  final String title;
  final bool back;
  final VoidCallback? onMenu;
  /// Shows the profile button (opens the Account page). The Account page itself turns it off.
  final bool account;
  /// No divider line: used when the bar floats over scrolling content.
  final bool transparent;
  const TopBar({super.key, this.title = 'WZH 2 APK', this.back = false, this.onMenu, this.account = true, this.transparent = false});
  Widget _b(IconData i, VoidCallback f) => SizedBox(
      width: 44, height: 44,
      child: Pressable(
        onTap: f, scale: .88,
        child: Center(child: Icon(i, size: 24, color: const Color(0xFFE8E8E8))),
      ));
  @override
  Widget build(BuildContext context) => Container(
        height: 48,
        padding: const EdgeInsets.symmetric(horizontal: 6),
        decoration: transparent ? null : const BoxDecoration(border: Border(bottom: BorderSide(color: C.line))),
        child: Row(children: [
          SizedBox(
            width: 100,
            child: Row(children: [
              back
                  ? _b(Icons.arrow_back_ios_new_rounded, () => Navigator.of(context).maybePop())
                  : _b(Icons.settings_outlined, onMenu ?? () {}),
            ]),
          ),
          Expanded(child: Text(title, textAlign: TextAlign.center, style: heading(19, w: FontWeight.w700))),
          SizedBox(
            width: 100,
            child: Row(mainAxisAlignment: MainAxisAlignment.end,
                children: [
                  if (account)
                    SizedBox(
                      width: 44, height: 44,
                      child: Pressable(
                        onTap: () => Navigator.of(context).push(CupertinoPageRoute(builder: (_) => const AccountPage())),
                        scale: .88,
                        child: const Center(child: AccountAvatar(size: 34)),
                      ),
                    ),
                ]),
          ),
        ]),
      );
}


/// Card used by every popup: title and text on top, then the buttons across the full width,
/// side by side with thin dividers (one button fills the row).
class DialogCard extends StatelessWidget {
  final String title;
  final String? message;
  final Widget? body;
  final List<Widget> actions;
  final double maxWidth;
  const DialogCard({super.key, required this.title, this.message, this.body, required this.actions, this.maxWidth = 340});
  @override
  Widget build(BuildContext context) {
    final avail = MediaQuery.of(context).size.width - 56;
    const line = Color(0x14FFFFFF);
    final side = actions.length <= 2;
    return Container(
      width: avail < maxWidth ? avail : maxWidth,
      clipBehavior: Clip.antiAlias,
      decoration: BoxDecoration(
        color: const Color(0xFF222225),
        borderRadius: BorderRadius.circular(28),
        border: Border.all(color: line),
        boxShadow: const [BoxShadow(color: Color(0x66000000), blurRadius: 28, offset: Offset(0, 10))],
      ),
      child: Column(mainAxisSize: MainAxisSize.min, crossAxisAlignment: CrossAxisAlignment.stretch, children: [
        Padding(
          padding: const EdgeInsets.fromLTRB(24, 24, 24, 20),
          child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
            Text(title, style: inter(21, w: FontWeight.w600, h: 1.25)),
            if (message != null) ...[
              const SizedBox(height: 10),
              Text(message!, style: inter(15.5, c: const Color(0xFFB4B4B8), h: 1.45)),
            ],
            if (body != null) ...[const SizedBox(height: 14), body!],
          ]),
        ),
        if (actions.isNotEmpty) Container(height: 1, color: line),
        if (actions.isEmpty)
          const SizedBox.shrink()
        else if (side)
          IntrinsicHeight(
            child: Row(crossAxisAlignment: CrossAxisAlignment.stretch, children: [
              for (var i = 0; i < actions.length; i++) ...[
                if (i > 0) Container(width: 1, color: line),
                Expanded(child: actions[i]),
              ],
            ]),
          )
        else
          for (var i = 0; i < actions.length; i++) ...[
            if (i > 0) Container(height: 1, color: line),
            actions[i],
          ],
      ]),
    );
  }
}

/// One full-width text button inside a [DialogCard]. Teal unless a [color] is given.
class DialogAction extends StatelessWidget {
  final String label;
  final Color? color;
  final VoidCallback? onTap;
  const DialogAction(this.label, {super.key, this.color, this.onTap});
  @override
  Widget build(BuildContext context) => Pressable(
        onTap: onTap,
        scale: .97,
        child: Container(
          height: 52,
          alignment: Alignment.center,
          color: Colors.transparent,
          child: Text(label, style: inter(16.5, w: FontWeight.w600, c: color ?? C.teal)),
        ),
      );
}

/// Simple popup with a title, a message and one OK button. No icon badge (icon/color are accepted
/// so older call sites keep compiling, but they are not drawn).
Future<void> showInfoDialog(BuildContext context, {required String title, String? message, IconData? icon, Color? color, String button = 'OK'}) =>
    showIosDialog<void>(
      context,
      (ctx) => DialogCard(title: title, message: message, actions: [
        DialogAction(button, onTap: () => Navigator.pop(ctx)),
      ]),
      dismissible: true,
    );

/// "Are you sure you want to leave?" card with two text buttons. Returns true when the person leaves.
Future<bool> confirmLeave(BuildContext context, {required String title, required String message, String stay = 'Stay', String leave = 'Leave', bool destructive = false}) async {
  final r = await showIosDialog<bool>(
    context,
    (ctx) => DialogCard(title: title, message: message, actions: [
      DialogAction(stay, color: const Color(0xFFB4B4B8), onTap: () => Navigator.pop(ctx, false)),
      DialogAction(leave, color: destructive ? C.red : C.teal, onTap: () => Navigator.pop(ctx, true)),
    ]),
    dismissible: true,
  );
  return r == true;
}

/// Text-field container that draws the teal focus ring when anything inside it has focus.
class FocusRing extends StatefulWidget {
  final Widget child;
  final double height, radius;
  final Color color, idleBorder;
  final EdgeInsetsGeometry padding;
  const FocusRing({
    super.key,
    required this.child,
    required this.height,
    required this.radius,
    this.color = C.field,
    this.idleBorder = Colors.transparent,
    this.padding = const EdgeInsets.symmetric(horizontal: 16),
  });
  @override
  State<FocusRing> createState() => _FocusRingState();
}

class _FocusRingState extends State<FocusRing> {
  bool on = false;
  @override
  Widget build(BuildContext context) => Focus(
        onFocusChange: (v) => setState(() => on = v),
        child: AnimatedContainer(
          duration: const Duration(milliseconds: 130),
          curve: Curves.easeOut,
          height: widget.height,
          padding: widget.padding,
          decoration: BoxDecoration(
            color: widget.color,
            borderRadius: BorderRadius.circular(widget.radius),
            border: Border.all(color: on ? C.teal : widget.idleBorder, width: on ? 1.8 : 1),
          ),
          child: widget.child,
        ),
      );
}


/// The small iOS-style popup under the "..." button: Refresh app / Updates.
/// It grows out of the button's corner with the iOS curve and closes on any tap outside.
Future<void> showTopMenu(BuildContext context, Rect anchor) {
  final screen = MediaQuery.of(context).size;
  return Navigator.of(context, rootNavigator: true).push(PageRouteBuilder<void>(
    opaque: false,
    barrierDismissible: true,
    barrierLabel: 'close',
    barrierColor: Colors.transparent,
    transitionDuration: glassSpring.duration,
    reverseTransitionDuration: const Duration(milliseconds: 260),
    transitionsBuilder: glassTransition(origin0: anchor.center),
    pageBuilder: (c, a, s) {
      void close(VoidCallback action) {
        Navigator.of(c).pop();
        action();
      }

      Widget row(IconData icon, String label, VoidCallback action) => Pressable(
            scale: .97,
            onTap: () => close(action),
            child: Padding(
              padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 10),
              child: Row(children: [
                Icon(icon, size: 19, color: C.teal),
                const SizedBox(width: 11),
                Expanded(child: Text(label, maxLines: 1, overflow: TextOverflow.ellipsis, style: inter(15, w: FontWeight.w500))),
              ]),
            ),
          );

      return Material(
        type: MaterialType.transparency,
        child: Stack(children: [
          Positioned(
            top: anchor.bottom + 4,
            right: (screen.width - anchor.right).clamp(12.0, 400.0).toDouble(),
            child: Container(
                  width: 172,
                  padding: const EdgeInsets.symmetric(vertical: 2),
                  decoration: BoxDecoration(
                    color: const Color(0xFF262628),
                    borderRadius: BorderRadius.circular(16),
                    boxShadow: const [BoxShadow(color: Color(0x66000000), blurRadius: 12, offset: Offset(0, 6))],
                  ),
                  child: Column(mainAxisSize: MainAxisSize.min, children: [
                    row(Icons.refresh_rounded, 'Refresh app', () => Notices.refreshApp(context)),
                    Container(height: 1, margin: const EdgeInsets.symmetric(horizontal: 12), color: const Color(0xFF3A3A3C)),
                    row(Icons.notifications_none_rounded, 'Updates',
                        () => Navigator.of(context).push(CupertinoPageRoute(builder: (_) => const UpdatesPage()))),
                  ]),
                ),
          ),
        ]),
      );
    },
  ));
}

