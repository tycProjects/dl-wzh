import 'package:flutter/material.dart' hide Text;
import 'package:flutter/cupertino.dart' show CupertinoPageRoute;
import '../theme.dart';
import '../widgets.dart';
import '../anim.dart';
import '../tr.dart';

class PermItem {
  final String key, label;
  final bool on;
  const PermItem(this.key, this.label, [this.on = false]);
}

/// Exactly the groups, labels and defaults of the real app (the keys go to the server as-is).
const permGroups = <String, List<PermItem>>{
  'Network & system': [
    PermItem('internet', 'Internet', true), PermItem('networkState', 'Network state', true),
    PermItem('notifications', 'Notifications'), PermItem('wakeLock', 'Keep screen awake'),
    PermItem('foregroundService', 'Background tasks'), PermItem('biometric', 'Biometric unlock'),
  ],
  'Media & camera': [
    PermItem('camera', 'Camera', true), PermItem('microphone', 'Microphone', true),
    PermItem('mediaImages', 'Photos'), PermItem('mediaVideo', 'Videos'), PermItem('mediaAudio', 'Audio files'),
  ],
  'Location': [
    PermItem('locationFine', 'Precise location', true), PermItem('locationCoarse', 'Approximate location', true),
    PermItem('locationBackground', 'Background location'),
  ],
  'Storage': [PermItem('storageLegacy', 'Files & media (legacy)', true)],
  'Contacts & calendar': [
    PermItem('contactsRead', 'Read contacts'), PermItem('contactsWrite', 'Edit contacts'),
    PermItem('calendarRead', 'Read calendar'), PermItem('calendarWrite', 'Edit calendar'),
  ],
  'Phone & SMS': [
    PermItem('callPhone', 'Make calls'), PermItem('phoneState', 'Phone state'), PermItem('callLog', 'Call log'),
    PermItem('smsRead', 'Read SMS'), PermItem('smsSend', 'Send SMS'),
  ],
  'Connectivity & sensors': [
    PermItem('bluetooth', 'Bluetooth'), PermItem('nearbyWifi', 'Nearby Wi-Fi devices'),
    PermItem('bodySensors', 'Body sensors'), PermItem('activityRecognition', 'Physical activity'),
    PermItem('vibrate', 'Vibrate', true),
  ],
  'Advanced': [
    PermItem('allFilesAccess', 'All files access'), PermItem('installUnknownApps', 'Install unknown apps'),
    PermItem('rootAccess', 'Root access'),
  ],
};

Map<String, bool> defaultPerms() => {for (final g in permGroups.values) for (final p in g) p.key: p.on};
List<String> selectedPerms(Map<String, bool> m) => [
      for (final g in permGroups.values) for (final p in g) if (m[p.key] == true) p.key
    ];

class PermissionsPage extends StatefulWidget {
  final Map<String, bool> perms;
  const PermissionsPage({super.key, required this.perms});
  static Route<void> route(Map<String, bool> p) => CupertinoPageRoute(builder: (_) => PermissionsPage(perms: p));
  @override
  State<PermissionsPage> createState() => _PermissionsPageState();
}

class _PermissionsPageState extends State<PermissionsPage> {
  @override
  Widget build(BuildContext context) {
    return Scaffold(
      body: SafeArea(
        child: Column(children: [
          const TopBar(title: 'App options', back: true),
          Expanded(
            child: ListView(
              physics: const BouncingScrollPhysics(parent: AlwaysScrollableScrollPhysics()),
              padding: const EdgeInsets.fromLTRB(18, 16, 18, 40),
              children: [
                Container(
                  decoration: BoxDecoration(color: C.card, borderRadius: BorderRadius.circular(26), border: Border.all(color: const Color(0xFF2B2B2D))),
                  padding: const EdgeInsets.fromLTRB(18, 8, 0, 14),
                  child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
                    for (final g in permGroups.entries) ...[
                      Padding(
                        padding: const EdgeInsets.only(top: 22, bottom: 8),
                        child: Text(g.key, style: inter(15, w: FontWeight.w600, c: const Color(0xFF9A9A9F))),
                      ),
                      for (var i = 0; i < g.value.length; i++) ...[
                        SizedBox(
                          height: 55,
                          child: Padding(
                            padding: const EdgeInsets.only(right: 18),
                            child: Row(children: [
                              Expanded(child: Text(g.value[i].label, style: inter(17))),
                              IosToggle(
                                value: widget.perms[g.value[i].key] ?? false,
                                onChanged: (v) => setState(() => widget.perms[g.value[i].key] = v),
                              ),
                            ]),
                          ),
                        ),
                        if (i != g.value.length - 1) const Divider(height: 1, thickness: 1, color: C.line),
                      ],
                    ],
                  ]),
                ),
              ],
            ),
          ),
        ]),
      ),
    );
  }
}
