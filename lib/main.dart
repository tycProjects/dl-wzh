import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_inappwebview/flutter_inappwebview.dart';
import 'package:permission_handler/permission_handler.dart';

Future<void> main() async {
  WidgetsFlutterBinding.ensureInitialized();
  await SystemChrome.setPreferredOrientations(const [
    DeviceOrientation.portraitUp,
    DeviceOrientation.portraitDown,
  ]);
  runApp(const ShopManagerApp());
}

class ShopManagerApp extends StatelessWidget {
  const ShopManagerApp({super.key});

  @override
  Widget build(BuildContext context) => MaterialApp(
        debugShowCheckedModeBanner: false,
        title: 'দোকান ম্যানেজার',
        theme: ThemeData(useMaterial3: true, colorSchemeSeed: const Color(0xFF4F46E5)),
        home: const ShopManagerHome(),
      );
}

class ShopManagerHome extends StatefulWidget {
  const ShopManagerHome({super.key});
  @override
  State<ShopManagerHome> createState() => _ShopManagerHomeState();
}

class _ShopManagerHomeState extends State<ShopManagerHome> {
  InAppWebViewController? _controller;
  bool _loading = true;
  double _progress = 0;

  Future<PermissionResponse> _requestWebPermissions(List<String> resources) async {
    final requested = <Permission>[];
    if (resources.any((r) => r.toLowerCase().contains('video') || r.toLowerCase().contains('camera'))) {
      requested.add(Permission.camera);
    }
    if (resources.any((r) => r.toLowerCase().contains('audio') || r.toLowerCase().contains('microphone'))) {
      requested.add(Permission.microphone);
    }
    if (requested.isNotEmpty) {
      final result = await requested.request();
      final granted = result.values.every((s) => s.isGranted);
      return PermissionResponse(
        resources: resources,
        action: granted ? PermissionResponseAction.GRANT : PermissionResponseAction.DENY,
      );
    }
    return PermissionResponse(resources: resources, action: PermissionResponseAction.GRANT);
  }

  Future<bool> _onBack() async {
    final c = _controller;
    if (c != null && await c.canGoBack()) {
      await c.goBack();
      return false;
    }
    return true;
  }

  @override
  Widget build(BuildContext context) => PopScope(
        canPop: false,
        onPopInvokedWithResult: (didPop, result) async {
          if (!didPop && await _onBack() && mounted) SystemNavigator.pop();
        },
        child: Scaffold(
          backgroundColor: const Color(0xFFE8EEFC),
          body: SafeArea(
            child: Stack(children: [
              InAppWebView(
                initialFile: 'assets/index.html',
                initialSettings: InAppWebViewSettings(
                  javaScriptEnabled: true,
                  domStorageEnabled: true,
                  databaseEnabled: true,
                  supportZoom: false,
                  builtInZoomControls: false,
                  displayZoomControls: false,
                  mediaPlaybackRequiresUserGesture: false,
                  allowsInlineMediaPlayback: true,
                  useOnDownloadStart: true,
                  allowFileAccess: true,
                  allowFileAccessFromFileURLs: true,
                  allowUniversalAccessFromFileURLs: true,
                  transparentBackground: true,
                  isInspectable: false,
                  preferredContentMode: UserPreferredContentMode.MOBILE,
                ),
                onWebViewCreated: (controller) => _controller = controller,
                onProgressChanged: (controller, progress) {
                  if (mounted) setState(() {
                    _progress = progress / 100;
                    _loading = progress < 100;
                  });
                },
                onPermissionRequest: (controller, request) async => _requestWebPermissions(request.resources),
                onGeolocationPermissionsShowPrompt: (controller, origin) async => GeolocationPermissionShowPromptResponse(
                  origin: origin,
                  allow: false,
                  retain: false,
                ),
                onConsoleMessage: (controller, message) {
                  debugPrint('Shop Manager WebView: ${message.message}');
                },
                onReceivedError: (controller, request, error) {
                  if (request.isForMainFrame == true && mounted) {
                    ScaffoldMessenger.of(context).showSnackBar(
                      const SnackBar(content: Text('পেজ লোডে সমস্যা হয়েছে। ইন্টারনেট সংযোগ পরীক্ষা করুন।')),
                    );
                  }
                },
              ),
              if (_loading)
                Align(
                  alignment: Alignment.topCenter,
                  child: LinearProgressIndicator(value: _progress == 0 ? null : _progress),
                ),
            ]),
          ),
        ),
      );
}
