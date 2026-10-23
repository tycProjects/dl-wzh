import 'package:flutter/widgets.dart';
import 'package:flutter_local_notifications/flutter_local_notifications.dart';
import 'api.dart';
import 'tr.dart';

/// Tells the person when a build is finished, even if they left the app.
/// It keeps watching the build on its own, so it works after leaving the build screen.
/// Every call is wrapped so a notification problem can never get in the way of a build.
class BuildNotifier {
  BuildNotifier._();
  static final BuildNotifier i = BuildNotifier._();

  final FlutterLocalNotificationsPlugin _plugin = FlutterLocalNotificationsPlugin();
  bool _ready = false, _asked = false;
  final Set<String> _watching = {};
  final Set<String> _sent = {};

  Future<void> _init() async {
    if (_ready) return;
    try {
      await _plugin.initialize(const InitializationSettings(android: AndroidInitializationSettings('@mipmap/ic_launcher')));
      _ready = true;
    } catch (e) {
      debugPrint('Notifications unavailable: $e');
    }
  }

  /// Asks for permission once (Android 13 and newer show a system question).
  Future<void> _askOnce() async {
    await _init();
    if (!_ready || _asked) return;
    _asked = true;
    try {
      await _plugin.resolvePlatformSpecificImplementation<AndroidFlutterLocalNotificationsPlugin>()?.requestNotificationsPermission();
    } catch (_) {}
  }

  /// Starts watching a build that was just started. Safe to call twice for the same build.
  void watch(String jobId, String appName) {
    _askOnce();
    if (!_watching.add(jobId)) return;
    _loop(jobId, appName);
  }

  Future<void> _loop(String id, String name) async {
    for (var n = 0; n < 600; n++) {
      await Future.delayed(const Duration(seconds: 6));
      try {
        final j = await Api.i.status(id);
        if (j.isDone || j.isError || j.isCancelled) {
          await finished(j, name);
          break;
        }
      } on ApiError catch (e) {
        if (e.status == 404) break;
      } catch (_) {}
    }
    _watching.remove(id);
  }

  /// Shows the "finished" notification, once per build, and only when the app is not on screen.
  Future<void> finished(Job j, String appName) async {
    if (j.isCancelled || !_sent.add(j.id)) return;
    if (WidgetsBinding.instance.lifecycleState == AppLifecycleState.resumed) return;
    await _init();
    if (!_ready) return;
    final name = appName.trim().isEmpty ? j.name : appName.trim();
    final ok = j.isDone;
    try {
      await _plugin.show(
        j.id.hashCode & 0x7fffffff,
        tr(ok ? 'Your app is ready' : 'Build failed'),
        tr(ok ? '$name finished building. Tap to open the app.' : '$name didn\u2019t finish building. Open the app to see why.'),
        const NotificationDetails(
          android: AndroidNotificationDetails(
            'builds',
            'Build updates',
            channelDescription: 'Lets you know when a build is finished',
            importance: Importance.high,
            priority: Priority.high,
          ),
        ),
      );
    } catch (e) {
      debugPrint('Notification failed: $e');
    }
  }
}
