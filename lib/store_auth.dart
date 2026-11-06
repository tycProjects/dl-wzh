import 'dart:convert';
import 'package:flutter/foundation.dart' show ValueNotifier;
import 'package:flutter/services.dart' show PlatformException;
import 'package:google_sign_in/google_sign_in.dart';
import 'package:http/http.dart' as http;
import 'api.dart';
import 'chat_cloud.dart' show kSupabaseUrl, kSupabaseAnonKey;
import 'config.dart';

/// Google sign-in for the Store. Only publishing needs it; everything else works signed out.
/// The Google ID token is exchanged for a Supabase session (access + refresh token), kept in prefs.
class StoreAuth {
  static const _key = 'storeAuth';
  static Map<String, dynamic>? _s;

  /// Bumps whenever the person signs in or out, so the Menu can show the account live.
  static final ValueNotifier<int> changes = ValueNotifier<int>(0);

  static Map<String, dynamic>? get _sess {
    if (_s != null) return _s;
    try {
      final raw = Api.i.prefs.getString(_key);
      if (raw != null && raw.isNotEmpty) _s = Map<String, dynamic>.from(jsonDecode(raw) as Map);
    } catch (_) {}
    return _s;
  }

  static bool get signedIn => '${_sess?['uid'] ?? ''}'.isNotEmpty;
  static String? get uid => signedIn ? '${_sess!['uid']}' : null;
  static String get email => '${_sess?['email'] ?? ''}';

  static Future<void> _store(Map<String, dynamic> j) async {
    final user = j['user'] is Map ? Map<String, dynamic>.from(j['user'] as Map) : <String, dynamic>{};
    final old = _sess ?? const {};
    final exp = (j['expires_at'] as num?)?.toInt() ??
        (DateTime.now().millisecondsSinceEpoch ~/ 1000 + ((j['expires_in'] as num?)?.toInt() ?? 3600));
    _s = {
      'access': '${j['access_token']}',
      'refresh': '${j['refresh_token'] ?? old['refresh'] ?? ''}',
      'exp': exp,
      'uid': '${user['id'] ?? old['uid'] ?? ''}',
      'email': '${user['email'] ?? old['email'] ?? ''}',
    };
    await Api.i.prefs.setString(_key, jsonEncode(_s));
    changes.value++;
  }

  static Map<String, String> get _anonH => {
        'apikey': kSupabaseAnonKey,
        'Content-Type': 'application/json',
        if (kSupabaseAnonKey.startsWith('eyJ')) 'Authorization': 'Bearer $kSupabaseAnonKey',
      };

  /// Opens the Google account picker. Returns false when the person backs out.
  static Future<bool> signIn() async {
    if (kGoogleWebClientId.startsWith('PASTE_')) {
      throw ApiError('Google sign-in isn\u2019t set up yet. Add the Web client ID in config.dart.');
    }
    final g = GoogleSignIn(serverClientId: kGoogleWebClientId, scopes: const ['email']);
    await g.signOut().catchError((_) => null); // always show the account picker
    final GoogleSignInAccount? acct;
    final GoogleSignInAuthentication auth;
    try {
      acct = await g.signIn();
      if (acct == null) return false;
      auth = await acct.authentication;
    } on PlatformException catch (e) {
      final m = '${e.message ?? ''}';
      var hint = '';
      if (m.contains(': 10') || m.contains('ApiException: 10') || '${e.code}' == 'sign_in_failed') {
        hint = ' (Code 10: the Android OAuth client package name or SHA-1 does not match this APK.)';
      } else if (m.contains(': 12500')) {
        hint = ' (Code 12500: set the OAuth consent screen / support email in Google Cloud.)';
      } else if (m.contains(': 7') || '${e.code}' == 'network_error') {
        hint = ' (Network problem.)';
      }
      throw ApiError('Google error ${e.code}: $m$hint');
    }
    final idToken = auth.idToken;
    if (idToken == null) throw ApiError('Google didn\u2019t return a sign-in token. Try again.');
    final res = await http
        .post(Uri.parse('$kSupabaseUrl/auth/v1/token?grant_type=id_token'),
            headers: _anonH, body: jsonEncode({'provider': 'google', 'id_token': idToken, if (auth.accessToken != null) 'access_token': auth.accessToken}))
        .timeout(const Duration(seconds: 30));
    if (res.statusCode != 200) {
      var msg = 'Couldn\u2019t sign in (${res.statusCode}).';
      try {
        final m = jsonDecode(res.body) as Map;
        final t = '${m['msg'] ?? m['error_description'] ?? m['message'] ?? ''}';
        if (t.isNotEmpty) msg = t;
      } catch (_) {}
      throw ApiError(msg);
    }
    await _store(Map<String, dynamic>.from(jsonDecode(res.body) as Map));
    return true;
  }

  /// A valid access token, renewed when it is about to expire. Null when signed out.
  static Future<String?> token() async {
    final s = _sess;
    if (s == null || '${s['access']}'.isEmpty) return null;
    final now = DateTime.now().millisecondsSinceEpoch ~/ 1000;
    if ((s['exp'] as num? ?? 0) - 60 > now) return '${s['access']}';
    final rt = '${s['refresh'] ?? ''}';
    if (rt.isEmpty) return null;
    try {
      final res = await http
          .post(Uri.parse('$kSupabaseUrl/auth/v1/token?grant_type=refresh_token'), headers: _anonH, body: jsonEncode({'refresh_token': rt}))
          .timeout(const Duration(seconds: 20));
      if (res.statusCode == 200) {
        await _store(Map<String, dynamic>.from(jsonDecode(res.body) as Map));
        return '${_s!['access']}';
      }
      if (res.statusCode == 400 || res.statusCode == 401) await signOut(); // session is gone for good
    } catch (_) {}
    return null;
  }

  static Future<void> signOut() async {
    _s = null;
    await Api.i.prefs.remove(_key);
    changes.value++;
    try {
      await GoogleSignIn().signOut();
    } catch (_) {}
  }
}
