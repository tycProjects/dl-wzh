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

