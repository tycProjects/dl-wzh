import 'package:flutter/cupertino.dart' show CupertinoPageRoute;
import 'package:flutter/material.dart' hide Text;
import 'package:flutter/services.dart' show Clipboard, ClipboardData, FilteringTextInputFormatter, TextInputType;
import '../anim.dart';
import '../banner.dart';
import '../config.dart' show kOwnerEmails;
import '../api.dart';
import '../notices.dart';
import '../signing_key.dart';
import '../sign_in_card.dart';
import '../store_auth.dart';
import '../store_cloud.dart';
import '../theme.dart';
import '../tr.dart';
import '../widgets.dart';
import 'gallery_picker.dart';
import 'updates_page.dart';

/// Opened by the profile button in the top bar: who is signed in, sign in / out,
/// and the app actions (Updates, Refresh app) that used to live in the "..." popup.
class AccountPage extends StatefulWidget {
  const AccountPage({super.key});
  @override
  State<AccountPage> createState() => _AccountPageState();
}

class _AccountPageState extends State<AccountPage> {
  bool _photoBusy = false;

  Future<void> _pickPhoto() async {
    final f = await pickIconImage(context);
    if (f == null || !mounted) return;
    setState(() => _photoBusy = true);
    try {
      await StoreCloud.setAvatar(f.path);
    } catch (e) {
      if (mounted) await showInfoDialog(context, title: 'Couldn\u2019t update photo', message: e is ApiError ? e.message : 'Something went wrong. Try again.', icon: Icons.error_outline_rounded, color: C.red);
    }
    if (mounted) setState(() => _photoBusy = false);
  }

  Future<void> _removePhoto() async {
    setState(() => _photoBusy = true);
    try {
      await StoreCloud.removeAvatar();
    } catch (e) {
      if (mounted) await showInfoDialog(context, title: 'Couldn\u2019t remove photo', message: e is ApiError ? e.message : 'Something went wrong. Try again.', icon: Icons.error_outline_rounded, color: C.red);
    }
    if (mounted) setState(() => _photoBusy = false);
  }

  Future<void> _signIn() async {
    final go = await showSignInCard(context);
    if (!go || !mounted) return;
    try {
      if (!await StoreAuth.signIn()) return;
      await StoreCloud.claimOld();
    } catch (e) {
      final msg = e is ApiError ? e.message : 'Something went wrong. Try again.';
      if (mounted && await showSignInError(context, msg) && mounted) _signIn();
    }
  }

  Future<void> _signOut() async {
    final ok = await confirmLeave(context,
        title: 'Sign out?', message: 'You can sign in again any time. Your published apps stay on your account.', stay: 'Cancel', leave: 'Sign out', destructive: true);
    if (ok) await StoreAuth.signOut();
  }

  Widget _label(String t) => Padding(
        padding: const EdgeInsets.fromLTRB(4, 26, 4, 10),
        child: Text(t, style: inter(14, w: FontWeight.w500, c: C.muted).copyWith(letterSpacing: .6)),
      );

  Widget _avatar() {
    if (!StoreAuth.signedIn) return const AccountAvatar(size: 92);
    return Pressable(
      onTap: _photoBusy ? null : _pickPhoto,
      child: SizedBox(
        width: 98,
        height: 98,
        child: Stack(children: [
          const Positioned(left: 0, top: 0, child: AccountAvatar(size: 92)),
          Positioned(
            right: 0,
            bottom: 0,
            child: Container(
              width: 32,
              height: 32,
              decoration: BoxDecoration(color: const Color(0xFF2A2A2C), shape: BoxShape.circle, border: Border.all(color: C.bg, width: 3)),
              child: const Icon(Icons.camera_alt_rounded, size: 16, color: C.text),
            ),
          ),
          if (_photoBusy)
            Positioned(left: 0, top: 0, child: Container(width: 92, height: 92, decoration: const BoxDecoration(color: Color(0x88000000), shape: BoxShape.circle), child: Center(child: IosSpinner(size: 26, color: C.teal)))),
        ]),
      ),
    );
  }

  Widget _photoActions() {
    if (!StoreAuth.signedIn) return const SizedBox.shrink();
    final has = StoreCloud.myAvatarPath.isNotEmpty;
    return Padding(
      padding: const EdgeInsets.only(top: 10),
      child: Row(mainAxisAlignment: MainAxisAlignment.center, children: [
        Pressable(
          haptic: false,
          onTap: _photoBusy ? null : _pickPhoto,
          child: Padding(padding: const EdgeInsets.symmetric(vertical: 6), child: Text(has ? 'Change photo' : 'Add photo', style: inter(15, w: FontWeight.w600, c: C.teal))),
        ),
        if (has) ...[
          const SizedBox(width: 22),
          Pressable(
            haptic: false,
            onTap: _photoBusy ? null : _removePhoto,
            child: Padding(padding: const EdgeInsets.symmetric(vertical: 6), child: Text('Remove', style: inter(15, w: FontWeight.w600, c: C.muted))),
          ),
        ],
      ]),
    );
  }

  Widget _row(String title, {String? value, VoidCallback? onTap, bool chevron = false, Color? color, bool verbatimValue = false}) => Pressable(
        scale: .985,
        onTap: onTap ?? () {},
        child: SizedBox(
          height: 55,
          child: Padding(
            padding: const EdgeInsets.symmetric(horizontal: 16),
            child: Row(children: [
              Text(title, style: inter(17, c: color ?? C.text)),
              if (value != null) ...[
                const SizedBox(width: 12),
                Expanded(child: Text(value, verbatim: verbatimValue, textAlign: TextAlign.right, maxLines: 1, overflow: TextOverflow.ellipsis, style: inter(16, c: C.muted))),
              ] else
                const Spacer(),
              if (chevron) ...[
                const SizedBox(width: 6),
                const Icon(Icons.chevron_right_rounded, size: 22, color: Color(0xFF6A6A6E)),
              ],
            ]),
          ),
        ),
      );

  Widget _group(List<Widget> rows) => Container(
        decoration: BoxDecoration(color: C.card, borderRadius: BorderRadius.circular(12)),
        child: Column(children: [
          for (var i = 0; i < rows.length; i++) ...[
            rows[i],
            if (i != rows.length - 1) Padding(padding: const EdgeInsets.only(left: 16), child: Container(height: 1, color: const Color(0xFF2A2A2C))),
          ],
        ]),
      );

  bool get _isOwner => StoreAuth.signedIn && kOwnerEmails.contains(StoreAuth.email.trim().toLowerCase());

  /// Owner only: gives a user's build back. Asks for the owner key each time (it is never saved).
  Future<void> _refundDialog() async {
    final keyC = TextEditingController(), idC = TextEditingController(), nC = TextEditingController(text: '1');
    var busy = false;
    await showGlassDialog<void>(
      context: context,
      builder: (dctx) => StatefulBuilder(
        builder: (dctx, setD) => AlertDialog(
          backgroundColor: const Color(0xFF1C1C1E),
          title: Text('Refund a build', style: inter(18, w: FontWeight.w700)),
          content: Column(mainAxisSize: MainAxisSize.min, children: [
            TextField(controller: keyC, obscureText: true, style: inter(15), decoration: const InputDecoration(hintText: 'Owner key')),
            const SizedBox(height: 10),
            TextField(controller: idC, style: inter(15), decoration: const InputDecoration(hintText: 'Support ID from the user')),
            const SizedBox(height: 10),
            TextField(
              controller: nC,
              keyboardType: TextInputType.number,
              inputFormatters: [FilteringTextInputFormatter.digitsOnly],
              style: inter(15),
              decoration: const InputDecoration(hintText: 'Builds to give back'),
            ),
          ]),
          actions: [
            TextButton(onPressed: busy ? null : () => Navigator.pop(dctx), child: Text('Cancel')),
            TextButton(
              onPressed: busy
                  ? null
                  : () async {
                      final key = keyC.text.trim();
                      final id = idC.text.trim();
                      final n = int.tryParse(nC.text.trim()) ?? 0;
                      if (key.isEmpty || id.isEmpty || n < 1) return;
                      setD(() => busy = true);
                      try {
                        final l = await Api.i.refundBuild(ownerKey: key, targetClientId: id, count: n);
                        if (dctx.mounted) Navigator.pop(dctx);
                        if (mounted) {
                          IosBanner.show(context, kind: BannerKind.ok, title: 'Build refunded', msg: l == null ? 'Gave back $n.' : 'They have ${l.remaining} left today.', duration: const Duration(seconds: 5));
                        }
                      } catch (e) {
                        setD(() => busy = false);
                        if (mounted) {
                          IosBanner.show(context, kind: BannerKind.err, title: 'Couldn\u2019t refund', msg: e is ApiError ? e.message : 'Check your connection and try again.', duration: const Duration(seconds: 6));
                        }
                      }
                    },
              child: Text('Refund'),
            ),
          ],
        ),
      ),
    );
    keyC.dispose();
    idC.dispose();
    nC.dispose();
  }

  Widget _body() {
    final on = StoreAuth.signedIn;
    final email = StoreAuth.email;
    return ListView(
      physics: const IosScrollPhysics(),
      padding: const EdgeInsets.fromLTRB(15, 26, 15, 40),
      children: [
        Center(child: _avatar()),
        const SizedBox(height: 16),
        Center(
          child: Text(on ? (email.isEmpty ? 'Google account' : email) : 'Not signed in',
              verbatim: on && email.isNotEmpty, maxLines: 1, overflow: TextOverflow.ellipsis, textAlign: TextAlign.center, style: inter(20, w: FontWeight.w700)),
        ),
        const SizedBox(height: 4),
        Center(child: Text(on ? 'Signed in with Google' : 'Sign in to publish apps', textAlign: TextAlign.center, style: inter(15, c: C.muted))),
        _photoActions(),
        _label('ACCOUNT INFORMATION'),
        _group([
          if (on) ...[
            _row('Email', value: email.isEmpty ? '\u2014' : email, verbatimValue: email.isNotEmpty),
            _row('Sign-in', value: 'Google'),
          ] else
            _row('Status', value: 'Signed out'),
          _row('Your apps', value: on ? 'Saved to this account' : 'Sign in to keep them'),
          if (on)
            AnimatedBuilder(
              animation: Listenable.merge([SigningKeys.i.current, SigningKeys.i.busy]),
              builder: (_, __) {
                final k = SigningKeys.i.current.value;
                if (k != null) return _row('Signing key', value: k.shortFingerprint, verbatimValue: true);
                return _row('Signing key', value: SigningKeys.i.busy.value ? 'Creating\u2026' : 'Not ready yet');
              },
            ),
          if (Api.i.clientId.isNotEmpty)
            _row('Support ID',
                value: Api.i.clientId.length > 10 ? '${Api.i.clientId.substring(0, 8)}\u2026' : Api.i.clientId,
                verbatimValue: true,
                onTap: () {
                  Clipboard.setData(ClipboardData(text: Api.i.clientId));
                  IosBanner.show(context, kind: BannerKind.ok, title: 'Support ID copied', msg: 'Send it to support if you need a build refunded.', duration: const Duration(seconds: 4));
                }),
        ]),
        const SizedBox(height: 14),
        Pressable(
          scale: .97,
          onTap: on ? _signOut : _signIn,
          child: Container(
            height: 50,
            alignment: Alignment.center,
            decoration: BoxDecoration(color: on ? const Color(0xFF2A2A2C) : C.tealBtn, borderRadius: BorderRadius.circular(14)),
            child: Row(mainAxisSize: MainAxisSize.min, children: [
              Icon(on ? Icons.logout_rounded : Icons.login_rounded, size: 20, color: on ? C.red : C.onTeal),
              const SizedBox(width: 8),
              Text(on ? 'Sign out' : 'Sign in', style: inter(16.5, w: FontWeight.w600, c: on ? C.red : C.onTeal)),
            ]),
          ),
        ),
        if (_isOwner) ...[
          _label('OWNER'),
          _group([
            _row('Refund a build', chevron: true, onTap: _refundDialog),
          ]),
        ],
        _label('APP'),
        _group([
          _row('Updates',
              chevron: true, onTap: () => Navigator.of(context).push(CupertinoPageRoute(builder: (_) => const UpdatesPage()))),
          _row('Refresh app', onTap: () => Notices.refreshApp(context)),
        ]),
      ],
    );
  }

  @override
  Widget build(BuildContext context) => Scaffold(
        body: SafeArea(
          bottom: false,
          child: Column(children: [
            const TopBar(title: 'Account', back: true, account: false),
            Expanded(
              child: ValueListenableBuilder<int>(
                valueListenable: StoreAuth.changes,
                builder: (_, __, ___) => _body(),
              ),
            ),
          ]),
        ),
      );
}
