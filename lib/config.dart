/// Build server (same one the HTML app uses).
const kApiBase = 'https://zip-to-apk-9qk0.onrender.com';

/// The server only talks to the official app. Your HTML app gets through because the
/// native WebView shell sends your app token in its User-Agent. Put the SAME value as
/// the OFFICIAL_APP_UA_TOKEN env var on Render here, e.g. 'WZH2APK/1.0 <token>'.
/// Leave empty only if your server does not check it on /api routes.
const kAppUserAgent = '';
