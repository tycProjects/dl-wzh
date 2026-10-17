import 'package:flutter/material.dart';
import '../theme.dart';
import '../anim.dart';

const splashInfo = <String, String>{
  'Off': 'No loading screen at all, opens straight into your site',
  'Custom': 'Your own video or image plays instead',
  'Native': "Your app icon on your site's color, then a soft fade into the app",
  'iOS zoom': 'Launch screen zooms toward you as the app opens',
  'Slide away': 'Launch screen lifts up to reveal the app',
  'Pop': 'Icon swells, then vanishes into the app',
};

class SplashSheet extends StatefulWidget {
  final String selected;
  const SplashSheet({super.key, required this.selected});
  @override
  State<SplashSheet> createState() => _SplashSheetState();
}

class _SplashSheetState extends State<SplashSheet> {
  late String sel = widget.selected;

  Widget _card(String name, {bool preview = false}) {
    final on = sel == name;
    final black = name == 'Off' || name == 'Custom';
    return Pressable(
      scale: .975,
      onTap: () => setState(() => sel = name),
      child: AnimatedContainer(
        duration: const Duration(milliseconds: 300),
        curve: iosCurve,
        decoration: BoxDecoration(
          color: C.card, borderRadius: BorderRadius.circular(24),
          border: Border.all(color: on ? C.teal : const Color(0xFF2B2B2D), width: on ? 2 : 1)),
        clipBehavior: Clip.antiAlias,
        child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
          SizedBox(
            height: 70,
            child: Stack(children: [
              Positioned.fill(child: Container(color: black ? Colors.black : const Color(0xFF16171A))),
              if (black)
                Center(child: Text(name == 'Off' ? 'Straight to site' : 'Upload below', style: inter(14, c: Colors.white)))
              else
                Align(
                  alignment: Alignment.bottomCenter,
                  child: Container(
                    width: 41, height: 66,
                    decoration: BoxDecoration(color: const Color(0xFF0F2428), borderRadius: const BorderRadius.vertical(top: Radius.circular(8)), border: Border.all(color: const Color(0xFF2E3A3D))),
                    alignment: Alignment.center,
                    child: Text('A', style: inter(11, w: FontWeight.w800)),
                  ),
                ),
              if (!black)
                Positioned(
                  right: 11, top: 10,
                  child: AnimatedContainer(
                    duration: const Duration(milliseconds: 250),
                    width: 18, height: 18,
                    decoration: BoxDecoration(
                      shape: BoxShape.circle,
                      color: on ? C.teal : Colors.transparent,
                      border: Border.all(color: on ? C.teal : const Color(0xFF8A8A8E), width: 1.5)),
                  ),
                ),
            ]),
          ),
          Padding(
            padding: const EdgeInsets.fromLTRB(14, 12, 12, 14),
            child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
              Text(name, style: inter(18, w: FontWeight.w700)),
              const SizedBox(height: 4),
              Text(splashInfo[name]!, style: inter(14.5, c: C.muted, h: 1.3)),
            ]),
          ),
        ]),
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    final h = MediaQuery.of(context).size.height;
    return Container(
      height: h * .69,
      decoration: const BoxDecoration(color: Color(0xFF181818), borderRadius: BorderRadius.vertical(top: Radius.circular(28))),
      padding: const EdgeInsets.fromLTRB(21, 22, 21, 18),
      child: Column(children: [
        Expanded(
          child: ListView(
            physics: const BouncingScrollPhysics(),
            padding: EdgeInsets.zero,
            children: [
              Text('Startup loading', style: inter(22, w: FontWeight.w700)),
              const SizedBox(height: 14),
              Row(crossAxisAlignment: CrossAxisAlignment.start, children: [
                Expanded(child: _card('Off')),
                const SizedBox(width: 10),
                Expanded(child: _card('Custom')),
              ]),
              const SizedBox(height: 22),
              Text('Launch screen', style: inter(17, w: FontWeight.w600, c: const Color(0xFFD8D8DC))),
              const SizedBox(height: 10),
              Row(crossAxisAlignment: CrossAxisAlignment.start, children: [
                Expanded(child: _card('Native')),
                const SizedBox(width: 10),
                Expanded(child: _card('iOS zoom')),
              ]),
              const SizedBox(height: 10),
              Row(crossAxisAlignment: CrossAxisAlignment.start, children: [
                Expanded(child: _card('Slide away')),
                const SizedBox(width: 10),
                Expanded(child: _card('Pop')),
              ]),
            ],
          ),
        ),
        const SizedBox(height: 12),
        Pressable(
          onTap: () => Navigator.pop(context, sel),
          child: Container(
            height: 50,
            alignment: Alignment.center,
            decoration: BoxDecoration(color: C.tealBtn, borderRadius: BorderRadius.circular(25)),
            child: Text('Done', style: inter(18, w: FontWeight.w700, c: const Color(0xFF0F2A24))),
          ),
        ),
      ]),
    );
  }
}
