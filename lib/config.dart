/// Build server (same one the HTML app uses).
const kApiBase = 'https://zip-to-apk-9qk0.onrender.com';

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

/// Fine-grained GitHub token limited to ONLY the repo above (Contents: Read and write).
/// Anyone who unpacks the app can read it, so never give it access to anything else.
const kShareGithubToken = '';

/// Biggest APK the Share tab accepts (GitHub allows up to 2 GB per file).
const kShareMaxMb = 200;

/// Where site/index.html is hosted (Firebase Hosting), like 'https://yourproject.web.app'.
/// Links then look like <this>/?d=<id> and open the download page. Leave it empty to hand out the
/// plain GitHub release page instead.
const kShareSiteBase = 'https://wzhdownload.web.app';

/// How many links one phone can keep at the same time (counted on the phone).
const kShareSlots = 3;
