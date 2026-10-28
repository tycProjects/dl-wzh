import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'core/linux.dart';
import 'intro.dart';
import 'screens/projects_screen.dart';
import 'screens/setup_screen.dart';
import 'theme.dart';

void main() {
  WidgetsFlutterBinding.ensureInitialized();
  SystemChrome.setSystemUIOverlayStyle(const SystemUiOverlayStyle(statusBarColor: Colors.transparent, statusBarIconBrightness: Brightness.light));
  runApp(const IdeApp());
}

class IdeApp extends StatelessWidget {
  const IdeApp({super.key});
  @override
  Widget build(BuildContext context) => MaterialApp(
        debugShowCheckedModeBanner: false,
        title: 'WZH Flutter IDE',
        theme: buildTheme(),
        home: const Boot(),
      );
}

/// Shows the startup animation while the app finds out whether the toolchain is already installed.
class Boot extends StatefulWidget {
  const Boot({super.key});
  @override
  State<Boot> createState() => _BootState();
}

class _BootState extends State<Boot> {
  bool ready = false, held = false, introDone = false;
  String? error;

  @override
  void initState() {
    super.initState();
    Linux.init().then((_) {
      if (mounted) setState(() => ready = true);
    }).catchError((Object e) {
      if (mounted) setState(() => error = '$e');
    });
  }

  @override
  Widget build(BuildContext context) {
    if (error != null) {
      return Scaffold(body: Center(child: Padding(padding: const EdgeInsets.all(24), child: Text('Couldn’t start: $error', style: inter(14)))));
    }
    final up = ready && held;
    return Stack(fit: StackFit.expand, children: [
      if (up) const Gate(key: ValueKey('gate')),
      if (!introDone)
        IntroSplash(
          key: const ValueKey('intro'),
          reveal: up,
          onHold: () {
            if (mounted) setState(() => held = true);
          },
          onDone: () {
            if (mounted) setState(() => introDone = true);
          },
        ),
    ]);
  }
}

/// Setup on the first launch, the project list after that.
class Gate extends StatefulWidget {
  const Gate({super.key});
  @override
  State<Gate> createState() => _GateState();
}

class _GateState extends State<Gate> {
  late bool ready = Linux.ready;
  @override
  Widget build(BuildContext context) => ready ? const ProjectsScreen() : SetupScreen(onDone: () => setState(() => ready = true));
}
