import 'package:flutter/material.dart' hide Text;
import 'package:flutter/services.dart';
import 'package:url_launcher/url_launcher.dart';
import '../anim.dart';
import '../theme.dart';
import '../widgets.dart';
import '../tr.dart';

const kTelegramHandle = 'forProjects01';

/// Opens the developer's Telegram chat (app first, then the web link). If neither
/// can open, the @handle is copied so the person can paste it into Telegram.
Future<void> openTelegram(BuildContext context) async {
  try {
    if (await launchUrl(Uri.parse('tg://resolve?domain=$kTelegramHandle'), mode: LaunchMode.externalApplication)) return;
  } catch (_) {}
  try {
    if (await launchUrl(Uri.parse('https://t.me/$kTelegramHandle'), mode: LaunchMode.externalApplication)) return;
  } catch (_) {}
  await Clipboard.setData(const ClipboardData(text: '@$kTelegramHandle'));
  if (context.mounted) {
    showInfoDialog(context,
        title: 'Couldn\u2019t open Telegram',
        message: 'I copied @$kTelegramHandle. Paste it into Telegram to message the admin.');
  }
}

/// One titled block of rows: (title, text).
class InfoSection {
  final String label;
  final List<(String, String)> rows;
  const InfoSection(this.label, this.rows);
}

class InfoPage extends StatelessWidget {
  final String title, sub;
  final IconData icon;
  final List<InfoSection> sections;

  /// Numbered cards (Privacy Policy / Terms of Service).
  final List<(String, String)> numbered;

  /// Shows a "Message on Telegram" card under this label.
  final String? telegramLabel;

  /// Plain paragraph card (Reminder).
  final String? paragraph;

  const InfoPage({
    super.key,
    required this.title,
    required this.sub,
    required this.icon,
    this.sections = const [],
    this.numbered = const [],
    this.telegramLabel,
    this.paragraph,
  });

  Widget _label(String t) => Padding(
        padding: const EdgeInsets.fromLTRB(6, 26, 6, 10),
        child: Text(t.toUpperCase(), style: inter(14, w: FontWeight.w500, c: C.muted).copyWith(letterSpacing: .6)),
      );

  Widget _card(List<Widget> rows) => Container(
        decoration: BoxDecoration(color: C.card, borderRadius: BorderRadius.circular(22)),
        child: Column(children: [
          for (var i = 0; i < rows.length; i++) ...[
            rows[i],
            if (i != rows.length - 1) Padding(padding: const EdgeInsets.only(left: 18), child: Container(height: 1, color: const Color(0xFF2A2A2C))),
          ],
        ]),
      );

  Widget _row(String t, String d) => Padding(
        padding: const EdgeInsets.fromLTRB(18, 15, 18, 16),
        child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
          Text(t, style: inter(17, w: FontWeight.w600)),
          const SizedBox(height: 5),
          Text(d, style: inter(15.5, c: C.muted, h: 1.4)),
        ]),
      );

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: C.bg,
      body: SafeArea(
        child: Column(children: [
          Container(
            height: 54,
            alignment: Alignment.centerLeft,
            padding: const EdgeInsets.symmetric(horizontal: 6),
            child: SizedBox(
              width: 44, height: 44,
              child: Pressable(
                onTap: () => Navigator.of(context).maybePop(),
                scale: .88,
                child: const Center(child: Icon(Icons.arrow_back_ios_new_rounded, size: 22, color: Color(0xFFE8E8E8))),
              ),
            ),
          ),
          Expanded(
            child: ListView(
              physics: const BouncingScrollPhysics(),
              padding: const EdgeInsets.fromLTRB(16, 4, 16, 40),
              children: [
                Padding(
                  padding: const EdgeInsets.fromLTRB(6, 0, 6, 4),
                  child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
                    Text(title, style: inter(32, w: FontWeight.w700, h: 1.1)),
                    const SizedBox(height: 8),
                    Text(sub, style: inter(16, c: C.muted, h: 1.4)),
                  ]),
                ),
                const SizedBox(height: 14),
                if (paragraph != null) _card([Padding(padding: const EdgeInsets.all(18), child: Text(paragraph!, style: inter(16.5, c: C.text, h: 1.45)))]),
                for (var i = 0; i < numbered.length; i++) ...[
                  Padding(
                    padding: const EdgeInsets.only(bottom: 10),
                    child: Container(
                      padding: const EdgeInsets.fromLTRB(16, 16, 16, 18),
                      decoration: BoxDecoration(color: C.card, borderRadius: BorderRadius.circular(22)),
                      child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
                        Row(children: [
                          Container(
                            width: 28, height: 28, alignment: Alignment.center,
                            decoration: BoxDecoration(color: C.tealDeep, shape: BoxShape.circle),
                            child: Text('${i + 1}', style: inter(14, w: FontWeight.w700, c: C.teal)),
                          ),
                          const SizedBox(width: 12),
                          Expanded(child: Text(numbered[i].$1, style: inter(17, w: FontWeight.w600))),
                        ]),
                        const SizedBox(height: 10),
                        Text(numbered[i].$2, style: inter(15.5, c: C.muted, h: 1.45)),
                      ]),
                    ),
                  ),
                ],
                for (final s in sections) ...[
                  _label(s.label),
                  _card([for (final r in s.rows) _row(r.$1, r.$2)]),
                ],
                if (telegramLabel != null) ...[
                  _label(telegramLabel!),
                  Pressable(
                    scale: .985,
                    onTap: () => openTelegram(context),
                    child: _card([_row('Message on Telegram', '@$kTelegramHandle')]),
                  ),
                ],
                const SizedBox(height: 30),
                Center(child: Text('Tycept \u00B7 WZH 2 APK', style: inter(14, c: C.muted))),
              ],
            ),
          ),
        ]),
      ),
    );
  }
}

/// Every drawer page, using the same text as the HTML app.
class Pages {
  static const features = InfoPage(
    title: 'Features',
    sub: 'Built into every app you make.',
    icon: Icons.auto_awesome_outlined,
    sections: [
      InfoSection('Feels like a real app', [
        ('Native feel', 'No grey tap flash, long-press menu or stray text selection. Typing still works.'),
        ('Pull to refresh', 'Swipe down at the top of the page to reload. It will not trigger while you scroll.'),
        ('Page slide', 'Pages slide in from the side when you go forward or back.'),
        ('Smooth first open', 'Your page fades in when it is ready, with no white flash.'),
        ('Keyboard aware', 'The field you tap moves up above the keyboard.'),
        ('Smart bar icons', 'Status and navigation bar icons turn light or dark so they stay readable.'),
        ('Back gesture', 'Goes back through your pages first, then exits. Works with Android swipe back.'),
        ('Fits your screen', 'Content stays clear of the notch, status bar and keyboard.'),
        ('Matching colors', 'The status and navigation bars match your site.'),
        ('No zoom or glow', 'Pinch zoom, font scaling and the edge glow are turned off.'),
        ('Startup screen', 'Pick a loading screen, or use your own image or video.'),
      ]),
      InfoSection('Works like one too', [
        ('Offline mode', 'Can open without internet after the first launch, depending on how your site caches its pages.'),
        ('Retry screen', 'No browser error page. It reconnects on its own.'),
        ('File uploads', 'Pick files and photos from inside your site.'),
        ('Downloads', 'Files save with their real names, even ones made inside the page.'),
        ('Notifications', 'Your site can send phone notifications.'),
        ('Share and vibrate', 'Your site can open the share sheet and buzz the phone.'),
        ('Outside links', 'Your site can open links in the browser or another app.'),
        ('Video', 'Videos autoplay, go fullscreen and show a loading spinner.'),
        ('Stays signed in', 'Logins and cookies are kept between launches, unless your site or Android clears them.'),
        ('Camera, mic and location', 'Your site can ask for them, like any app.'),
        ('Google sign-in', 'Login popups are supported. Add google-services.json for Firebase, or paste your Firebase App ID, API key and Project ID. Some sign-in setups may need extra work.'),
        ('App Links', 'Links to your domain can open inside the app. Needs the App Link Host set and assetlinks.json hosted on your site.'),
      ]),
      InfoSection('Customize and protect', [
        ('Permissions', 'Choose exactly what your app can access.'),
        ('Custom icon', 'Upload your own. 512x512 or larger works best.'),
        ('Name and package', 'Set the app name and an optional package suffix.'),
        ('Release signing', 'Your key is made once and reused. You can download it.'),
        ('Your own key', 'Sign with your own key and certificate instead.'),
        ('Special access', 'Optional all-files access. Root access only works on phones that are already rooted.'),
        ('Dex2C protection', 'Optional. Moves parts of your code into native C++ to make it harder to reverse-engineer. It does not make copying impossible.'),
        ('String protection', 'Makes text inside your app harder to read. It is obfuscation, not encryption.'),
      ]),
    ],
  );

  static const how = InfoPage(
    title: 'How it works',
    sub: 'From your project to an APK in a few steps.',
    icon: Icons.help_outline_rounded,
    sections: [
      InfoSection('Steps', [
        ('1. Add your project', 'On the Build tab, choose your source: a zip, a link, or pasted code.'),
        ('2. Set your options', 'On the Options tab, set the app name, icon, permissions, startup screen and signing.'),
        ('3. Build', 'Tap Build and follow the live progress. Build times vary.'),
        ('4. Download', 'When it finishes, download your APK. Past builds are in History.'),
      ]),
      InfoSection('Keep in mind', [
        ('Not every project builds', 'Some project setups fail. The build log shows what went wrong.'),
        ('Daily limit', 'The menu shows how many builds you have left today.'),
        ('Back up your key', 'Download your signing key and keep it safe. You need it to update the same app later.'),
      ]),
    ],
  );

  static const faq = InfoPage(
    title: 'FAQ',
    sub: 'Quick answers to common questions.',
    icon: Icons.quiz_outlined,
    sections: [
      InfoSection('Questions', [
        ('Is it free?', 'You get a limited number of builds each day. The menu shows how many are left.'),
        ('How long does a build take?', 'It varies with your project and how busy the server is. Follow the live progress on the Progress tab.'),
        ('Where is my APK?', 'When a build finishes, tap download. Past builds stay in the History tab.'),
        ('Can I update my app later?', 'Yes, but sign it with the same key. Download your signing key and keep it safe.'),
        ('Does it work offline?', 'The app can open offline after the first launch, depending on how your site caches its pages.'),
        ('Can I use my own icon?', 'Yes. Upload one in the Options tab. 512x512 or larger works best.'),
      ]),
    ],
  );

  static const tips = InfoPage(
    title: 'Tips & troubleshooting',
    sub: 'If something goes wrong, start here.',
    icon: Icons.build_outlined,
    sections: [
      InfoSection('Before you build', [
        ('Check your link', 'Make sure your website URL opens in a normal browser and starts with https.'),
        ('Pick a unique name', 'Use a package suffix if you build several apps, so they do not replace each other.'),
        ('Choose only needed permissions', 'Fewer permissions make a cleaner app.'),
      ]),
      InfoSection('If a build fails', [
        ('Read the build log', 'It shows what went wrong. Fix it and build again.'),
        ('Try again', 'Some failures are temporary. A second try can work.'),
        ('Turn off extras', 'Try the build without Dex2C or String protection.'),
        ('Still stuck?', 'Use Report a problem and include what you were building.'),
      ]),
      InfoSection('If the APK will not install', [
        ('Allow unknown apps', 'Android may ask you to allow installs from your browser or file manager.'),
        ('Remove the old version', 'If an older build was signed with a different key, uninstall it first.'),
      ]),
    ],
  );

  static const privacy = InfoPage(
    title: 'Privacy Policy',
    sub: 'Last updated October 4, 2026. What Tycept collects when you use WZH 2 APK, and why.',
    icon: Icons.verified_user_outlined,
    numbered: [
      ('What gets sent to our servers.', 'The zip, link, or code you submit is uploaded automatically to build your APK, along with basic technical data (IP address, device/browser type, timestamps) needed to run and secure the build service.'),
      ('Why we use it.', 'To run your build, show progress, generate your APK and signing key, and keep your build history.'),
      ('Third parties.', 'We use third-party hosting and build services to run the service, so your project files and build logs pass through their systems and are also subject to their own policies.'),
      ('What we don\u2019t do.', 'We don\u2019t sell your data and we don\u2019t browse your source code. If a build fails, automated repair tools read your project files and build log, and may send excerpts to third-party AI services to suggest a fix, and may edit your project files to retry. The developer can also see your builds and may install them to check the service works.'),
      ('Storage & retention.', 'Build artifacts and logs are kept for a limited time so you can download them, then removed. We don\u2019t guarantee an exact deletion time. Signing keys are generated and stored on our side so they can be reused. Download and back yours up yourself.'),
      ('Security.', 'We take reasonable steps to protect your data: uploads travel over encrypted connections and access is limited. No system is 100% secure, and we can\u2019t guarantee your data will never be accessed without permission. If we discover unauthorized access, we\u2019ll notify affected users as required by law.'),
      ('Local storage.', 'Your device keeps small bits of data locally (like your build history and a device ID). The device ID may also be sent to our server to apply daily build limits and match builds to your device. You can clear local data anytime from the History tab or your device settings.'),
      ('Messages to the admin.', 'If you use Message admin, your message, any contact detail you add, and your device ID are sent to our server and forwarded to the admin through Telegram. Don\u2019t include passwords or private information.'),
      ('Your choices.', 'You can clear your local build history anytime from the History tab. To ask about data held server-side, use Message admin in the menu.'),
    ],
  );

  static const terms = InfoPage(
    title: 'Terms of Service',
    sub: 'Last updated October 25, 2026. The short version of what you\u2019re agreeing to by using WZH 2 APK.',
    icon: Icons.description_outlined,
    numbered: [
      ('Accepting these terms.', 'By using WZH 2 APK you agree to these terms. If you don\u2019t agree, please don\u2019t use the app. You must be old enough to agree to them where you live.'),
      ('Your responsibility.', 'You confirm you have the right to build, host, share and distribute whatever you upload. Don\u2019t submit malware, stolen code, phishing or impersonation pages, or anything you don\u2019t own or have permission to use.'),
      ('Your content.', 'You keep ownership of your projects. You give us permission to process, build, store and deliver them so the service can work, and to host or share them if you use those features.'),
      ('Service as-is.', 'Builds are provided "as is," with no guarantee of uptime, build speed, or that every project will succeed. Features described in the app may not work with every site or device.'),
      ('Use at your own risk.', 'You use WZH 2 APK, and any app, site or link you make with it, at your own risk. We don\u2019t promise it will be error-free, always available, or right for your needs, and you\u2019re responsible for testing your app before you release it or rely on it.'),
      ('Code and data security.', 'We take reasonable steps to keep your projects private, and we don\u2019t share your code on purpose. No online service can promise perfect security, though, so in rare cases uploaded files could be exposed or copied. Keep passwords, private keys and other secrets out of your project, and keep your own backup.'),
      ('Automatic fixes.', 'If a build fails, automated tools may read your project files and build log and edit your files to try again. The result can differ from what you uploaded, so check your app before you release it.'),
      ('Protection is not a guarantee.', 'Code and string protection only make an app harder to reverse-engineer. They don\u2019t guarantee your code can\u2019t be copied or read.'),
      ('Your keys, your responsibility.', 'Signing keys are generated for you and may not be recoverable if lost, so back them up. Losing a key means you can\u2019t publish updates to that same app listing.'),
      ('Hosting and sharing.', 'Host and Share give you a limited number of slots, and file size limits apply. Anyone with a link can open what you put behind it, so don\u2019t upload anything private. Links and files may expire or be removed, so keep your own copy. Don\u2019t host or share anything unlawful, harmful, deceptive, or that you don\u2019t have the right to share.'),
      ('Fair use.', 'Daily build limits reset at midnight UTC and keep the service usable for everyone. Automated abuse or attempts to bypass limits may get access restricted.'),
      ('Official app only.', 'Use only the official app. Modified, re-signed or copied versions aren\u2019t supported and may stop working.'),
      ('Removal and suspension.', 'We may remove content, links or builds, or restrict access, if these terms are broken, if the law requires it, or to protect the service and other people.'),
      ('Limitation of liability.', 'To the maximum extent the law allows, Tycept isn\u2019t liable for damages arising from your use of a built app, content you uploaded, hosted or shared, or service interruptions. You\u2019re responsible for your own project\u2019s compliance with applicable law, including any app store rules.'),
      ('Changes and questions.', 'These terms may be updated occasionally; continued use after a change means you accept the update. Questions are welcome, so use Message admin in the menu.'),
    ],
  );

  static const reminder = InfoPage(
    title: 'Reminder',
    sub: 'A quick note from the developer.',
    icon: Icons.notifications_none_rounded,
    paragraph:
        'Hey, thanks for using my app! Heads up: I can see your builds and may install them to check that everything works. I don\u2019t publish your app. If a build fails, automated tools may adjust your project files to try to fix it. Questions are welcome, just use Message admin in the menu.',
  );

  static const about = InfoPage(
    title: 'About developer',
    sub: 'The person behind WZH 2 APK.',
    icon: Icons.person_outline_rounded,
    telegramLabel: 'Contact',
    sections: [
      InfoSection('Developer', [
        ('Tycept', 'Built and maintained WZH 2 APK, a tool that turns your website or project into an Android app.'),
        ('Why it exists', 'Made to make building an APK quick and simple, right from your phone.'),
      ]),
      InfoSection('App', [
        ('WZH 2 APK', 'Build from a zip, a link or pasted code, then download your APK.'),
        ('Feedback', 'Found a bug or have an idea? Send a message and it will be looked at.'),
      ]),
    ],
  );

  static const report = InfoPage(
    title: 'Report a problem',
    sub: 'Tell the developer what happened.',
    icon: Icons.flag_outlined,
    telegramLabel: 'Send',
    sections: [
      InfoSection('What to include', [
        ('What you did', 'The steps before the problem, like the source type and options you chose.'),
        ('What happened', 'The error message or a screenshot of the build log.'),
        ('Your phone', 'Phone model and Android version help a lot.'),
      ]),
    ],
  );
}
