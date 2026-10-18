import 'package:flutter/foundation.dart';
import 'api.dart';

/// Shared, live data the screens listen to.
class Store {
  Store._();
  static final Store i = Store._();

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
