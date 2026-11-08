import 'package:flutter/foundation.dart' show ValueListenable;
import 'package:flutter/material.dart' hide Text;
import '../anim.dart';
import '../api.dart';
import '../theme.dart';
import '../tr.dart';
import '../widgets.dart';
import 'hostshare_ui.dart';
import 'hs_cards.dart';
import 'hs_design.dart';

String _mb(int bytes) => bytes >= 1048576 ? '${(bytes / 1048576).toStringAsFixed(1)} MB' : '${(bytes / 1024).ceil()} KB';

/// Details of one shared APK: Copy link / Open / Replace, the numbers, the download link, the QR
/// code, version info, and Delete. It reads the live list from the Share tab, so it updates by
/// itself after a replace.
class ShareDetailPage extends StatelessWidget {
  final String slug;
  final ValueListenable<List<SharedApk>> items;
  final ValueListenable<String> busy; // slug being worked on ('' = none), 'new' while a new link uploads
  final ValueListenable<String> busyText;
  final VoidCallback onCopy, onOpen, onReplace, onDelete, onSaveQr;
  const ShareDetailPage({
    super.key,
    required this.slug,
    required this.items,
    required this.busy,
    required this.busyText,
    required this.onCopy,
    required this.onOpen,
    required this.onReplace,
    required this.onDelete,
    required this.onSaveQr,
  });

  Widget _stat(String value, String label) => Expanded(
        child: Column(mainAxisSize: MainAxisSize.min, children: [
          Text(value, verbatim: true, style: inter(20, w: FontWeight.w600)),
          const SizedBox(height: 1),
          Text(label, style: inter(12.5, c: C.muted)),
        ]),
      );

  Widget _action(String text, Color color, VoidCallback? onTap) => Pressable(
        scale: .985,
        onTap: onTap,
        child: Container(
          height: 50,
          alignment: Alignment.centerLeft,
          padding: const EdgeInsets.symmetric(horizontal: 16),
          color: Colors.transparent,
          child: Text(text, style: inter(16, c: onTap == null ? C.muted : color)),
        ),
      );

  Widget _info(String k, String v) => Padding(
        padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 13),
        child: Row(children: [
          Text(k, style: inter(15)),
          const SizedBox(width: 16),
          Expanded(child: Text(v, verbatim: true, textAlign: TextAlign.right, maxLines: 1, overflow: TextOverflow.ellipsis, style: inter(15, c: C.muted))),
        ]),
      );

  Widget _divider() => const Divider(height: .5, thickness: .5, color: kHsLine);

  Widget _card({required Widget child, EdgeInsetsGeometry? padding}) => Container(
        padding: padding,
        decoration: ShapeDecoration(color: kHsCard, shape: hsShape(20)),
        clipBehavior: Clip.antiAlias,
        child: child,
      );

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: kHsBg,
      body: SafeArea(
        child: ValueListenableBuilder<List<SharedApk>>(
          valueListenable: items,
          builder: (context, list, _) {
            final i = list.indexWhere((e) => e.slug == slug);
            if (i < 0) return const SizedBox.shrink();
            final a = list[i];
            return ValueListenableBuilder<String>(
              valueListenable: busy,
              builder: (context, b, _) {
                final working = b == slug;
                final locked = b.isNotEmpty;
                final since = a.replaced ?? a.created;
                final infos = <List<String>>[
                  if (a.replaced != null) ['Last replaced', hsDate(a.replaced!)],
                  if (a.versionName.isNotEmpty || a.versionCode > 0) ['Version', a.versionName.isEmpty ? '${a.versionCode}' : a.versionName],
                ];
                return ListView(
                  physics: const IosScrollPhysics(parent: AlwaysScrollableScrollPhysics()),
                  padding: const EdgeInsets.fromLTRB(17, 0, 17, 40),
                  children: [
                    SizedBox(
                      height: 54,
                      child: Align(
                        alignment: Alignment.centerLeft,
                        child: Pressable(
                          scale: .92,
                          onTap: () => Navigator.of(context).maybePop(),
                          child: Row(mainAxisSize: MainAxisSize.min, children: [
                            Icon(Icons.arrow_back_ios_new_rounded, size: 20, color: kHsBlue),
                            const SizedBox(width: 6),
                            Text('Share', style: inter(17, c: kHsBlue)),
                          ]),
                        ),
                      ),
                    ),
                    const SizedBox(height: 6),
                    Center(child: HsAppIcon(a.name, size: 80, logoUrl: a.logoUrl)),
                    const SizedBox(height: 14),
                    Text(a.name, verbatim: true, textAlign: TextAlign.center, maxLines: 2, overflow: TextOverflow.ellipsis, style: inter(23, w: FontWeight.w700)),
                    const SizedBox(height: 3),
                    Text('Android app, APK', textAlign: TextAlign.center, style: inter(13.5, c: C.muted)),
                    const SizedBox(height: 20),
                    HsQuickRow(
                      [
                        HsQuick(Icons.content_copy_rounded, 'Copy link', onCopy),
                        HsQuick(Icons.open_in_new_rounded, 'Open', onOpen),
                        HsQuick(Icons.sync_rounded, 'Replace', locked ? null : onReplace),
                      ],
                      bg: kHsCard,
                    ),
                    const SizedBox(height: 14),
                    _card(
                      padding: const EdgeInsets.symmetric(vertical: 15),
                      child: IntrinsicHeight(
                        child: Row(children: [
                          _stat(a.downloads >= 0 ? '${a.downloads}' : '\u2013', 'downloads'),
                          const VerticalDivider(width: .5, thickness: .5, color: kHsLine),
                          _stat(_mb(a.sizeBytes), 'size'),
                          const VerticalDivider(width: .5, thickness: .5, color: kHsLine),
                          _stat(since == null ? '\u2013' : hsShort(since), 'since update'),
                        ]),
                      ),
                    ),
                    const SizedBox(height: 14),
                    _card(
                      padding: const EdgeInsets.fromLTRB(16, 12, 16, 12),
                      child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
                        Text('Download link', style: inter(12.5, c: C.muted)),
                        const SizedBox(height: 3),
                        Text(a.url, verbatim: true, maxLines: 1, overflow: TextOverflow.ellipsis, style: inter(15)),
                      ]),
                    ),
                    const SizedBox(height: 14),
                    _card(
                      padding: const EdgeInsets.fromLTRB(14, 18, 14, 4),
                      child: Column(children: [
                        HsQr(a.url, size: 140),
                        const SizedBox(height: 10),
                        Text('Scan to install', style: inter(12.5, c: C.muted)),
                        Pressable(scale: .95, onTap: onSaveQr, child: SizedBox(height: 46, child: Center(child: Text('Save QR Code', style: inter(16, c: kHsBlue))))),
                      ]),
                    ),
                    if (infos.isNotEmpty) ...[
                      const SizedBox(height: 14),
                      _card(
                        child: Column(children: [
                          for (var k = 0; k < infos.length; k++) ...[
                            if (k > 0) _divider(),
                            _info(infos[k][0], infos[k][1]),
                          ],
                        ]),
                      ),
                    ],
                    const SizedBox(height: 14),
                    _card(
                      child: Column(crossAxisAlignment: CrossAxisAlignment.stretch, children: [
                        if (working) ...[
                          ValueListenableBuilder<String>(
                            valueListenable: busyText,
                            builder: (_, t, __) => Padding(
                              padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 14),
                              child: Row(children: [
                                IosSpinner(size: 18, color: kHsBlue),
                                const SizedBox(width: 10),
                                Text(t, verbatim: true, style: inter(15, c: C.muted)),
                              ]),
                            ),
                          ),
                          _divider(),
                        ],
                        _action('Delete Link', kHsRed, locked ? null : onDelete),
                      ]),
                    ),
                    const HsFootnote('Replacing keeps the same link, and the download count stays.'),
                  ],
                );
              },
            );
          },
        ),
      ),
    );
  }
}
