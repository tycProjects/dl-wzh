import 'dart:async';
import 'package:http/http.dart' as http;

/// Build server (same one the HTML app uses).
/// Servers, in order of preference: the new one first, the old one as a fallback.
const kApiBases = ['https://zip-to-apk-cajn.onrender.com', 'https://zip-to-apk-9qk0.onrender.com'];

/// The server currently in use. Set at startup by [pickApiBase]; starts as the first one.
String kApiBase = kApiBases[0];

/// Asks each server in order (/api/version) and keeps the first that answers.
/// Falls back to the first one if none answer in time (e.g. both are waking up).
Future<void> pickApiBase() async {
  for (final base in kApiBases) {
    try {
      final r = await http.get(Uri.parse('$base/api/version'), headers: {'Origin': kSiteOrigin}).timeout(const Duration(seconds: 12));
      if (r.statusCode < 500) { kApiBase = base; return; }
    } catch (_) {}
  }
  kApiBase = kApiBases[0];
}

/// The server only answers requests that say they come from your own site: every /api route
/// except downloads checks the Origin header against OUR_ORIGINS in server.js. A native app
/// sends no Origin on its own, so it is added here. This one is always on the server's list
/// (EXTRA_SITE_ORIGINS in server.js); if you ever change that list, keep this in step.
const kSiteOrigin = 'https://zip-to-apk.pages.dev';

/// Starting a build is app-only. The server wants the official app token in the User-Agent
/// (OFFICIAL_APP_UA_TOKEN on Render, 'TyceptOfficial' unless you set your own) followed by the
/// signing-key tag (TYCEPT_SIGNING_SHA256 on Render). If you changed either env var on Render,
/// put the same value here.
/// Release builds on Android no longer send the fixed [kSigningSha256] below: lib/app_proof.dart
/// reads the real signature (and a code fingerprint) from the installed app. The fixed value is
/// only used by debug builds and non-Android platforms.
const kAppToken = 'TyceptOfficial';
const kSigningSha256 = '59efb47cf4f563e7ad77b04bb64933d9768f02f0f5560393146fa2501af92d17';
const kAppUserAgent = '$kAppToken/1.0 TyceptSig/$kSigningSha256';

/// Where Quick build gets its offline template, once, by itself. This is your GitHub repo of the
/// html-to-apk project, written "owner/repo". The app downloads HtmlToApk.apk from its latest
/// release, takes assets/template.apk out of it and keeps it on the phone, so every Quick build
/// after that works with no internet. The repo has to be public.
const kQuickTemplateRepo = 'tycProjects/dl-wzh';


/// Share tab (upload an APK, get a download link). The phone uploads the APK straight to GitHub as a
/// Release file in your APK repo (public repo, so downloads are free and unlimited for you).
/// kShareGithubRepo is "owner/repo" of that repo. Create it with a README so it is not empty.
const kShareGithubRepo = 'tycweb/apk-files';

/// The GitHub token for the Share tab is NOT kept here. It lives on the server (SHARE_GITHUB_TOKEN
/// on Render) and the app fetches it at run time, so it never ends up in the APK or in a project
/// you push for building.

/// Biggest APK the Share tab accepts (GitHub allows up to 2 GB per file).
const kShareMaxMb = 200;

/// Where site/index.html is hosted (Firebase Hosting), like 'https://yourproject.web.app'.
/// Links then look like <this>/?d=<id> and open the download page. Leave it empty to hand out the
/// plain GitHub release page instead.
const kShareSiteBase = 'https://wzhdownload.web.app';

/// How many links one phone can keep at the same time (counted on the phone).
const kShareSlots = 3;

/// "Host a site" runs on your own Cloudflare Worker (worker/host-worker.js), so it keeps working when
/// the build server is down. Put the Worker's address here, like 'https://wzh-host.yourname.workers.dev'.
/// Leave it empty to keep hosting through the build server as before.
const kHostWorker = 'https://wzh-host.forworkingprojprojects.workers.dev';
