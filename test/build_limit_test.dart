import 'package:flutter_test/flutter_test.dart';
import 'package:wzh_apk/api.dart';

// What the app does with the server's /api/build-limit and refund replies.
// Run with: flutter test
void main() {
  test('reads a full reply including the lifetime refund total', () {
    final l = BuildLimit.fromJson({'used': 2, 'limit': 5, 'remaining': 3, 'resetsAt': '2026-11-09T00:00:00.000Z', 'refundedTotal': 4});
    expect(l.used, 2);
    expect(l.limit, 5);
    expect(l.remaining, 3);
    expect(l.refunded, 4);
    expect(l.resetsAt, isNotNull);
  });

  test('refundedTotal missing means no popup (null), not zero', () {
    final l = BuildLimit.fromJson({'used': 0, 'limit': 5, 'remaining': 5});
    expect(l.refunded, isNull);
  });

  test('numbers sent as strings still parse', () {
    final l = BuildLimit.fromJson({'used': '1', 'limit': '5', 'remaining': '4', 'refundedTotal': '2'});
    expect(l.remaining, 4);
    expect(l.refunded, 2);
  });
}
