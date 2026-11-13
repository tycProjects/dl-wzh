import 'dart:async';
import 'package:flutter/widgets.dart';
import 'package:flutter_local_notifications/flutter_local_notifications.dart';
import 'api.dart';
import 'chat_cloud.dart';
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

/// Tells the person when the admin answers in the chat while the chat sheet is closed.
/// It listens on its own for as long as the app is running (also in the background).
/// Every call is wrapped so a notification problem can never get in the way of the app.
class ChatNotifier {
  ChatNotifier._();
  static final ChatNotifier i = ChatNotifier._();

  /// Set by the chat sheet: true while it is on screen (it shows replies itself).
  static bool chatOpen = false;

  /// Admin replies that came in while the chat was closed (shown as a badge on the chat button).
  static final ValueNotifier<int> unread = ValueNotifier<int>(0);

  /// True once this phone is registered for push: then the system shows replies while the app is in the background.
  static bool pushOn = false;

  final FlutterLocalNotificationsPlugin _plugin = FlutterLocalNotificationsPlugin();
  ChatCloud? _cloud;
  Timer? _timer;
  bool _ready = false, _asked = false, _busy = false, _started = false;
  int _since = -1; // -1 = first run: remember what is already there, don't notify for it

  Future<void> _init() async {
    if (_ready) return;
    try {
      await _plugin.initialize(const InitializationSettings(android: AndroidInitializationSettings('@mipmap/ic_launcher')));
      _ready = true;
    } catch (e) {
      debugPrint('Chat notifications unavailable: $e');
    }
  }

  /// Makes sure the notification channel exists (push messages from the server use it too).
  Future<void> prepare() async {
    await _init();
    if (!_ready) return;
    try {
      await _plugin.resolvePlatformSpecificImplementation<AndroidFlutterLocalNotificationsPlugin>()?.createNotificationChannel(
            const AndroidNotificationChannel(
              'admin_chat',
              'Admin replies',
              description: 'Lets you know when the admin answers your message',
              importance: Importance.high,
            ),
          );
    } catch (_) {}
  }

  /// Asks for permission once (Android 13 and newer show a system question).
  Future<void> askPermission() async {
    await _init();
    if (!_ready || _asked) return;
    _asked = true;
    try {
      await _plugin.resolvePlatformSpecificImplementation<AndroidFlutterLocalNotificationsPlugin>()?.requestNotificationsPermission();
    } catch (_) {}
  }

  /// Starts watching for admin replies. Safe to call again.
  void start() {
    if (_started || !kChatCloudOn) return;
    final id = Api.i.clientId;
    if (id.isEmpty) return;
    _started = true;
    final cloud = _cloud = ChatCloud(ChatCloud.keyFor(id));
    _since = int.tryParse(Api.i.prefs.getString('dwNotifSince') ?? '') ?? -1;
    cloud.listen(_check);
    _timer = Timer.periodic(const Duration(seconds: 30), (_) => _check());
    _check();
  }

  /// Checks right away (used when the app comes back to the front).
  void poke() => _check();

  Future<void> _check() async {
    final cloud = _cloud;
    if (cloud == null || _busy) return;
    _busy = true;
    try {
      final first = _since < 0;
      final rows = await cloud.fetch(first ? 0 : _since);
      var newest = first ? 0 : _since;
      String? last;
      var fresh = 0;
      for (final r in rows) {
        if (r.id > newest) newest = r.id;
        if (!first && r.admin && r.id > _since) {
          last = r.body;
          fresh++;
        }
      }
      if (first || newest != _since) {
        _since = newest;
        await Api.i.prefs.setString('dwNotifSince', '$newest');
      }
      if (fresh > 0 && !chatOpen) unread.value += fresh;
      final onScreen = WidgetsBinding.instance.lifecycleState == AppLifecycleState.resumed;
      if (last != null && !chatOpen && (!pushOn || onScreen)) await _show(last);
    } catch (_) {
      // Offline: the next check tries again.
    } finally {
      _busy = false;
    }
  }

  Future<void> _show(String text) async {
    await _init();
    if (!_ready) return;
    final body = text.length > 140 ? '${text.substring(0, 137)}...' : text;
    try {
      await _plugin.show(
        0x41444d, // same id every time, so a new reply replaces the old notification
        'Admin',
        body,
        const NotificationDetails(
          android: AndroidNotificationDetails(
            'admin_chat',
            'Admin replies',
            channelDescription: 'Lets you know when the admin answers your message',
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
