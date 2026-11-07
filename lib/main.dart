import 'dart:math';
import 'package:flutter/material.dart';

void main() => runApp(const SadikGamingApp());

const bg = Color(0xFF080812);
const panel = Color(0xFF151326);
const purple = Color(0xFF9B35FF);
const pink = Color(0xFFEA35FF);
const gold = Color(0xFFFFC857);

class SadikGamingApp extends StatelessWidget {
  const SadikGamingApp({super.key});
  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      debugShowCheckedModeBanner: false,
      title: 'SADIK GAMING',
      theme: ThemeData(
        useMaterial3: true,
        brightness: Brightness.dark,
        scaffoldBackgroundColor: bg,
        colorScheme: ColorScheme.fromSeed(seedColor: purple, brightness: Brightness.dark),
        fontFamily: 'Roboto',
      ),
      home: const GamingHome(),
    );
  }
}

class GameItem {
  final String name, subtitle;
  final IconData icon;
  final Color color;
  const GameItem(this.name, this.subtitle, this.icon, this.color);
}

const games = [
  GameItem('Free Fire', 'Battle in style', Icons.local_fire_department, Color(0xFFFF684A)),
  GameItem('PUBG Mobile', 'Winner winner', Icons.sports_esports, Color(0xFFFFB84D)),
  GameItem('BGMI', 'Squad up', Icons.shield, Color(0xFF58C8FF)),
  GameItem('Call of Duty', 'Ready for action', Icons.gps_fixed, Color(0xFF9B8CFF)),
  GameItem('Minecraft', 'Build & explore', Icons.grass, Color(0xFF64D88A)),
  GameItem('Roblox', 'Play your world', Icons.hexagon, Color(0xFFF5F5F5)),
];

class GamingHome extends StatefulWidget {
  const GamingHome({super.key});
  @override
  State<GamingHome> createState() => _GamingHomeState();
}

class _GamingHomeState extends State<GamingHome> {
  int tab = 0;
  int diamonds = 2450;
  bool dailyClaimed = false;
  final Set<String> openedGames = {};

  void notice(String message) {
    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(content: Text(message), behavior: SnackBarBehavior.floating,
        backgroundColor: const Color(0xFF28213E)),
    );
  }

  void claimDaily() {
    if (dailyClaimed) {
      notice('Daily demo reward already claimed.');
      return;
    }
    setState(() { dailyClaimed = true; diamonds += 50; });
    notice('Demo reward claimed: +50 diamonds!');
  }

  void spin() {
    final rewards = [10, 25, 50, 75, 100];
    final prize = rewards[Random().nextInt(rewards.length)];
    setState(() => diamonds += prize);
    showDialog(context: context, builder: (_) => AlertDialog(
      backgroundColor: panel,
      title: const Text('Spin complete'),
      content: Text('You won $prize demo diamonds!'),
      actions: [TextButton(onPressed: () => Navigator.pop(context), child: const Text('NICE'))],
    ));
  }

  @override
  Widget build(BuildContext context) {
    final pages = [
      _homePage(),
      _gamesPage(),
      _rewardsPage(),
      _profilePage(),
    ];
    return Scaffold(
      body: SafeArea(child: pages[tab]),
      floatingActionButton: tab == 0 ? FloatingActionButton(
        backgroundColor: purple, foregroundColor: Colors.white,
        onPressed: () => setState(() => tab = 1),
        child: const Icon(Icons.play_arrow_rounded, size: 32),
      ) : null,
      floatingActionButtonLocation: FloatingActionButtonLocation.centerDocked,
      bottomNavigationBar: NavigationBar(
        backgroundColor: const Color(0xFF11101D),
        indicatorColor: purple.withOpacity(.22),
        selectedIndex: tab,
        onDestinationSelected: (v) => setState(() => tab = v),
        destinations: const [
          NavigationDestination(icon: Icon(Icons.home_outlined), selectedIcon: Icon(Icons.home), label: 'Home'),
          NavigationDestination(icon: Icon(Icons.sports_esports_outlined), selectedIcon: Icon(Icons.sports_esports), label: 'Games'),
          NavigationDestination(icon: Icon(Icons.card_giftcard_outlined), selectedIcon: Icon(Icons.card_giftcard), label: 'Rewards'),
          NavigationDestination(icon: Icon(Icons.person_outline), selectedIcon: Icon(Icons.person), label: 'Profile'),
        ],
      ),
    );
  }

  Widget _homePage() => ListView(
    padding: const EdgeInsets.fromLTRB(18, 16, 18, 90),
    children: [
      Row(children: [
        Container(width: 42, height: 42, decoration: BoxDecoration(
          borderRadius: BorderRadius.circular(14), gradient: const LinearGradient(colors: [purple, pink])),
          child: const Icon(Icons.bolt, color: Colors.white, size: 28)),
        const SizedBox(width: 10),
        const Expanded(child: Text('SADIK GAMING', style: TextStyle(fontSize: 20, fontWeight: FontWeight.w900, letterSpacing: .5))),
        IconButton(onPressed: () => notice('You are all caught up.'), icon: const Icon(Icons.notifications_none_rounded)),
      ]),
      const SizedBox(height: 22),
      Container(
        padding: const EdgeInsets.all(20), height: 185,
        decoration: BoxDecoration(
          borderRadius: BorderRadius.circular(24),
          gradient: const LinearGradient(begin: Alignment.topLeft, end: Alignment.bottomRight,
            colors: [Color(0xFF45117D), Color(0xFF191333), Color(0xFF25105A)]),
          border: Border.all(color: purple.withOpacity(.55)),
        ),
        child: Stack(children: [
          const Positioned(right: 0, top: 6, child: Icon(Icons.sports_esports, size: 88, color: Color(0x66FFFFFF))),
          Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
            const Text('LEVEL UP YOUR', style: TextStyle(color: Colors.white70, fontSize: 12, letterSpacing: 2)),
            const Text('GAME LIFE', style: TextStyle(fontSize: 28, fontWeight: FontWeight.w900)),
            const SizedBox(height: 8),
            const Text('Your gaming hub starts here', style: TextStyle(color: Colors.white70)),
            const Spacer(),
            FilledButton.icon(onPressed: () => setState(() => tab = 1),
              icon: const Icon(Icons.play_arrow), label: const Text('Explore Games'),
              style: FilledButton.styleFrom(backgroundColor: purple)),
          ]),
        ]),
      ),
      const SizedBox(height: 24),
      Row(mainAxisAlignment: MainAxisAlignment.spaceBetween, children: [
        const Text('Your Dashboard', style: TextStyle(fontSize: 18, fontWeight: FontWeight.bold)),
        Text('DEMO MODE', style: TextStyle(color: gold, fontSize: 10, fontWeight: FontWeight.bold)),
      ]),
      const SizedBox(height: 12),
      Row(children: [
        Expanded(child: _statCard(Icons.diamond, 'Diamonds', '$diamonds', const Color(0xFF63D8FF))),
        const SizedBox(width: 12),
        Expanded(child: _statCard(Icons.local_fire_department, 'Game streak', '3 days', gold)),
      ]),
      const SizedBox(height: 24),
      _sectionTitle('Quick Actions', 'View all', () => setState(() => tab = 2)),
      const SizedBox(height: 12),
      Row(children: [
        Expanded(child: _quickAction(Icons.casino, 'Spin Wheel', purple, spin)),
        const SizedBox(width: 10),
        Expanded(child: _quickAction(Icons.calendar_month, 'Daily Bonus', gold, claimDaily)),
        const SizedBox(width: 10),
        Expanded(child: _quickAction(Icons.card_giftcard, 'Rewards', pink, () => setState(() => tab = 2))),
      ]),
      const SizedBox(height: 24),
      _sectionTitle('Popular Games', 'See all', () => setState(() => tab = 1)),
      const SizedBox(height: 12),
      ...games.take(3).map(_gameTile),
      const SizedBox(height: 12),
      const Text('Rewards and balances in this starter app are demo-only.',
        style: TextStyle(color: Colors.white54, fontSize: 11)),
    ],
  );

  Widget _gamesPage() => ListView(
    padding: const EdgeInsets.all(18),
    children: [
      const Text('All Games', style: TextStyle(fontSize: 27, fontWeight: FontWeight.w900)),
      const SizedBox(height: 6),
      const Text('Choose a game to view its details.', style: TextStyle(color: Colors.white60)),
      const SizedBox(height: 18),
      ...games.map(_gameTile),
    ],
  );

  Widget _rewardsPage() => ListView(
    padding: const EdgeInsets.all(18),
    children: [
      const Text('Rewards', style: TextStyle(fontSize: 27, fontWeight: FontWeight.w900)),
      const SizedBox(height: 18),
      Container(padding: const EdgeInsets.all(20), decoration: BoxDecoration(
        borderRadius: BorderRadius.circular(22),
        gradient: const LinearGradient(colors: [Color(0xFF40116F), Color(0xFF17122C)]),
        border: Border.all(color: purple.withOpacity(.6))),
        child: Row(children: [
          const Icon(Icons.diamond, size: 46, color: Color(0xFF63D8FF)),
          const SizedBox(width: 16),
          Expanded(child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
            const Text('DEMO BALANCE', style: TextStyle(color: Colors.white60, fontSize: 11, letterSpacing: 1.5)),
            Text('$diamonds diamonds', style: const TextStyle(fontSize: 24, fontWeight: FontWeight.w900)),
          ])),
        ]),
      ),
      const SizedBox(height: 24),
      const Text('Daily Rewards', style: TextStyle(fontSize: 18, fontWeight: FontWeight.bold)),
      const SizedBox(height: 12),
      _rewardTile(Icons.card_giftcard, 'Daily check-in', 'Claim 50 demo diamonds', dailyClaimed ? 'Claimed' : 'Claim', dailyClaimed ? null : claimDaily),
      _rewardTile(Icons.casino, 'Lucky spin', 'Try your luck for a demo prize', 'Spin', spin),
      _rewardTile(Icons.task_alt, 'Starter mission', 'Explore the games list', 'Open', () => setState(() => tab = 1)),
      const SizedBox(height: 14),
      const Text('No real money, purchases, withdrawals, or redeemable rewards are enabled.',
        style: TextStyle(color: Colors.white54, fontSize: 12)),
    ],
  );

  Widget _profilePage() => ListView(
    padding: const EdgeInsets.all(18),
    children: [
      const Text('Profile', style: TextStyle(fontSize: 27, fontWeight: FontWeight.w900)),
      const SizedBox(height: 26),
      Center(child: Column(children: [
        Container(width: 92, height: 92, decoration: BoxDecoration(
          shape: BoxShape.circle, gradient: const LinearGradient(colors: [purple, pink]),
          boxShadow: [BoxShadow(color: purple.withOpacity(.35), blurRadius: 25)]),
          child: const Icon(Icons.person, size: 54, color: Colors.white)),
        const SizedBox(height: 12),
        const Text('Player One', style: TextStyle(fontSize: 22, fontWeight: FontWeight.bold)),
        const Text('SADIK GAMING MEMBER', style: TextStyle(color: Colors.white54, fontSize: 11, letterSpacing: 1.5)),
      ])),
      const SizedBox(height: 28),
      _menuTile(Icons.history, 'Activity History', () => _showInfo('Activity History', openedGames.isEmpty ? 'No games opened yet.' : openedGames.join('\\n'))),
      _menuTile(Icons.settings_outlined, 'App Settings', () => _showSettings()),
      _menuTile(Icons.help_outline, 'Help & Support', () => _showInfo('Help & Support', 'This is a starter demo. Connect your own support service for production.')),
      _menuTile(Icons.info_outline, 'About App', () => _showInfo('About SADIK GAMING', 'Premium gaming hub Flutter starter project.')),
    ],
  );

  Widget _statCard(IconData icon, String label, String value, Color color) => Container(
    padding: const EdgeInsets.all(15), decoration: BoxDecoration(color: panel, borderRadius: BorderRadius.circular(18),
      border: Border.all(color: Colors.white.withOpacity(.06))),
    child: Row(children: [
      Icon(icon, color: color, size: 27), const SizedBox(width: 10),
      Expanded(child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
        Text(label, style: const TextStyle(color: Colors.white60, fontSize: 11)),
        Text(value, style: const TextStyle(fontWeight: FontWeight.w800, fontSize: 18)),
      ])),
    ]),
  );

  Widget _sectionTitle(String title, String action, VoidCallback onTap) => Row(
    mainAxisAlignment: MainAxisAlignment.spaceBetween,
    children: [Text(title, style: const TextStyle(fontSize: 18, fontWeight: FontWeight.bold)),
      TextButton(onPressed: onTap, child: Text(action))],
  );

  Widget _quickAction(IconData icon, String label, Color color, VoidCallback onTap) => InkWell(
    borderRadius: BorderRadius.circular(16), onTap: onTap,
    child: Container(padding: const EdgeInsets.symmetric(vertical: 16, horizontal: 6),
      decoration: BoxDecoration(color: panel, borderRadius: BorderRadius.circular(16),
        border: Border.all(color: color.withOpacity(.3))),
      child: Column(children: [
        Icon(icon, color: color, size: 28), const SizedBox(height: 9),
        Text(label, textAlign: TextAlign.center, style: const TextStyle(fontSize: 11, fontWeight: FontWeight.w600)),
      ]),
    ),
  );

  Widget _gameTile(GameItem game) => Container(
    margin: const EdgeInsets.only(bottom: 11),
    decoration: BoxDecoration(color: panel, borderRadius: BorderRadius.circular(18),
      border: Border.all(color: Colors.white.withOpacity(.06))),
    child: ListTile(
      contentPadding: const EdgeInsets.symmetric(horizontal: 14, vertical: 5),
      leading: Container(width: 48, height: 48, decoration: BoxDecoration(
        color: game.color.withOpacity(.16), borderRadius: BorderRadius.circular(14)),
        child: Icon(game.icon, color: game.color, size: 27)),
      title: Text(game.name, style: const TextStyle(fontWeight: FontWeight.bold)),
      subtitle: Text(game.subtitle, style: const TextStyle(color: Colors.white60, fontSize: 12)),
      trailing: const Icon(Icons.arrow_forward_ios, size: 15, color: purple),
      onTap: () {
        setState(() => openedGames.add(game.name));
        _showInfo(game.name, 'Game details page placeholder. This button works; connect the official game install page or your own content to launch the real game.');
      },
    ),
  );

  Widget _rewardTile(IconData icon, String title, String subtitle, String button, VoidCallback? onTap) => Container(
    margin: const EdgeInsets.only(bottom: 11), padding: const EdgeInsets.symmetric(vertical: 6),
    decoration: BoxDecoration(color: panel, borderRadius: BorderRadius.circular(17)),
    child: ListTile(
      leading: Icon(icon, color: gold, size: 30),
      title: Text(title, style: const TextStyle(fontWeight: FontWeight.bold)),
      subtitle: Text(subtitle, style: const TextStyle(color: Colors.white60, fontSize: 12)),
      trailing: FilledButton(onPressed: onTap, style: FilledButton.styleFrom(backgroundColor: onTap == null ? Colors.grey : purple),
        child: Text(button)),
    ),
  );

  Widget _menuTile(IconData icon, String title, VoidCallback onTap) => Container(
    margin: const EdgeInsets.only(bottom: 8), decoration: BoxDecoration(color: panel, borderRadius: BorderRadius.circular(15)),
    child: ListTile(leading: Icon(icon, color: purple), title: Text(title),
      trailing: const Icon(Icons.chevron_right), onTap: onTap),
  );

  void _showInfo(String title, String message) {
    showDialog(context: context, builder: (_) => AlertDialog(
      backgroundColor: panel, title: Text(title), content: Text(message),
      actions: [TextButton(onPressed: () => Navigator.pop(context), child: const Text('CLOSE'))],
    ));
  }

  void _showSettings() {
    bool notifications = true;
    showDialog(context: context, builder: (dialogContext) => StatefulBuilder(builder: (context, setLocal) => AlertDialog(
      backgroundColor: panel, title: const Text('App Settings'),
      content: SwitchListTile(
        contentPadding: EdgeInsets.zero, value: notifications, activeColor: purple,
        title: const Text('Notifications'), subtitle: const Text('Demo setting'),
        onChanged: (v) => setLocal(() => notifications = v)),
      actions: [TextButton(onPressed: () { Navigator.pop(dialogContext); notice('Settings saved for this session.'); },
        child: const Text('SAVE'))],
    )));
  }
}
