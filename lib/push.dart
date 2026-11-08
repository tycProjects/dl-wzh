import 'package:firebase_core/firebase_core.dart';
import 'package:firebase_messaging/firebase_messaging.dart';
import 'package:flutter/widgets.dart';
import 'api.dart';
import 'chat_cloud.dart';
import 'notify.dart';

/// Push notifications for admin replies, so they arrive even when the app is closed.
/// Sends this phone's Firebase token to Supabase; the telegram-hook function pushes to it.
/// Every call is wrapped: if Firebase isn't set up, the app simply keeps using ChatNotifier.
class ChatPush {
  ChatPush._();
  static final ChatPush i = ChatPush._();
  bool _started = false;

  Future<void> start() async {
    if (_started || !kChatCloudOn) return;
    final id = Api.i.clientId;
    if (id.isEmpty) return;
    _started = true;
    try {
      await Firebase.initializeApp();
      await ChatNotifier.i.prepare();
      final cloud = ChatCloud(ChatCloud.keyFor(id));
      Future<void> send(String? token) async {
        if (token == null || token.isEmpty) return;
        try {
          await cloud.registerDevice(token);
          ChatNotifier.pushOn = true;
        } catch (e) {
          debugPrint('Push token not saved: $e');
        }
      }

      await send(await FirebaseMessaging.instance.getToken());
      FirebaseMessaging.instance.onTokenRefresh.listen(send);
    } catch (e) {
      debugPrint('Push unavailable: $e');
    }
  }
}
