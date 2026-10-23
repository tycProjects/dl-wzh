import 'package:flutter/foundation.dart';
import 'api.dart';

/// Shared, live data the screens listen to.
class Store {
  Store._() : botHidden = ValueNotifier(Api.i.prefs.getString('botHidden') == '1');
  static final Store i = Store._();

  /// The floating chat-bot button can be hidden from the "..." menu; the choice is remembered.
  final ValueNotifier<bool> botHidden;
  Future<void> setBotHidden(bool v) async {
    botHidden.value = v;
    if (v) {
      await Api.i.prefs.setString('botHidden', '1');
    } else {
      await Api.i.prefs.remove('botHidden');
    }
  }

  /// "Refresh app": wake the build server and reload everything that is shown.
  void refreshAll() {
    Api.i.wake();
    refreshLimit();
    refreshHistory();
  }

  final ValueNotifier<BuildLimit?> limit = ValueNotifier(null);
  final ValueNotifier<List<Job>> history = ValueNotifier(const []);
  final ValueNotifier<bool> historyLoaded = ValueNotifier(false);

  Future<void> refreshLimit({String ownerKey = ''}) async {
    final l = await Api.i.limit(ownerKey: ownerKey);
    if (l != null) limit.value = l;
  }

  Future<void> refreshHistory() async {
    try {
      final h = await Api.i.history();
      h.sort((a, b) => (b.createdAt ?? DateTime(2000)).compareTo(a.createdAt ?? DateTime(2000)));
      history.value = h;
    } catch (_) {
    } finally {
      historyLoaded.value = true;
    }
  }

  String? get activeJobId => Api.i.prefs.getString('activeJob');
  Future<void> setActiveJob(String? id) async {
    if (id == null) {
      await Api.i.prefs.remove('activeJob');
    } else {
      await Api.i.prefs.setString('activeJob', id);
    }
  }
}

/// Which tab of the home shell is showing. The code editor lives in the shell as a tab, so other
/// screens (menu, build errors) switch to it through here.
/// 0 Message, 1 Home (builds), 2 Host, 4 Code editor (3 Updates opens as its own page).
class ShellNav {
  static const int message = 0, home = 1, host = 2, code = 4;
  static final ValueNotifier<int> tab = ValueNotifier<int>(home);

  /// The little red "NEW" badge on the Code tab, until it has been opened once.
  static final ValueNotifier<bool> codeNew = ValueNotifier<bool>(Api.i.prefs.getString('wzhCodeTab') != '1');
  static void go(int i) {
    tab.value = i;
    if (i == code && codeNew.value) {
      codeNew.value = false;
      Api.i.prefs.setString('wzhCodeTab', '1');
    }
  }
}
