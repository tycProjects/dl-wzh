import 'dart:async';
import 'dart:convert';
import 'package:crypto/crypto.dart';
import 'package:http/http.dart' as http;
import 'package:supabase/supabase.dart';

// Paste your own values from Supabase -> Project Settings -> API.
// Use the "anon" (legacy, starts with eyJ) key; it is meant to be public.
const kSupabaseUrl = 'https://dmyvnwfpxxndwrqbeqoo.supabase.co';
const kSupabaseAnonKey = 'eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImRteXZud2ZweHhuZHdycWJlcW9vIiwicm9sZSI6ImFub24iLCJpYXQiOjE3OTMwOTM3MzksImV4cCI6MjEwODY2OTczOX0.WtW-4eJMUafuKV4Bu9OOenqT3JtmF6AwlqUzp5tUZro';

/// Off until both values above are filled in; the Admin chat then uses the older server check.
bool get kChatCloudOn => kSupabaseUrl.startsWith('https://') && kSupabaseAnonKey.length > 20;

class CloudMsg {
  final int id;
  final bool admin;
  final String body;
  const CloudMsg(this.id, this.admin, this.body);
}

/// Admin chat history and live replies, kept in Supabase.
/// The table can't be read directly: the app calls one function with its private chat key.
class ChatCloud {
  final String key;
  SupabaseClient? _client;
  RealtimeChannel? _ch;
  ChatCloud(this.key);

  /// Same hash the server uses, so the app never sends its raw device id anywhere new.
  static String keyFor(String clientId) =>
      sha256.convert(utf8.encode('wzh-chat:$clientId')).toString().substring(0, 32);

  Map<String, String> get _h => {
        'Content-Type': 'application/json',
        'apikey': kSupabaseAnonKey,
        if (kSupabaseAnonKey.startsWith('eyJ')) 'Authorization': 'Bearer $kSupabaseAnonKey',
      };

  Future<List<CloudMsg>> fetch(int since) async {
    final res = await http
        .post(
          Uri.parse('$kSupabaseUrl/rest/v1/rpc/get_messages'),
          headers: _h,
          body: jsonEncode({'p_key': key, 'p_since': since}),
        )
        .timeout(const Duration(seconds: 12));
    if (res.statusCode != 200) throw Exception('chat ${res.statusCode}');
    final list = jsonDecode(res.body) as List;
    return [
      for (final r in list)
        CloudMsg((r['id'] as num).toInt(), r['role'] == 'admin', '${r['body'] ?? ''}'),
    ];
  }

  /// Calls [onPing] whenever the admin sends a reply. Safe to call again; it replaces the old listener.
  void listen(void Function() onPing) {
    stop();
    try {
      final c = _client ??= SupabaseClient(kSupabaseUrl, kSupabaseAnonKey);
      _ch = c.channel('chat:$key').onBroadcast(event: 'reply', callback: (_) => onPing()).subscribe();
    } catch (_) {
      // The 30 second check in the chat page still picks replies up.
    }
  }

  void stop() {
    final c = _client, ch = _ch;
    _ch = null;
    if (c == null || ch == null) return;
    try {
      c.removeChannel(ch).catchError((_) => '');
    } catch (_) {}
  }

  void dispose() {
    stop();
    try {
      _client?.dispose();
    } catch (_) {}
    _client = null;
  }
}
