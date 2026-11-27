// WZH 2 APK -- "Share an APK" download Worker.  Paste this whole file into a Cloudflare Worker.
//
// Why: the download page used to hand the browser a github.com/<you>/<repo>/releases/... address, so anyone
// could see your GitHub account. Now the page only talks to this Worker. The Worker fetches the file from
// GitHub itself and streams it back, so GitHub's address never reaches the visitor (no redirect, no link).
//
//   GET /m/<tag>   details about the shared app (name, size, date, downloads, checksum), no GitHub links in it
//   GET /f/<tag>   the APK itself, streamed
//   GET /l/<tag>   the app's logo picture, if it has one
//
// Settings > Variables and Secrets:
//   SHARE_REPO     (text)    "owner/repo" of the APK repo, the same as kShareGithubRepo in the app
//   SHARE_TOKEN    (secret)  OPTIONAL but recommended: a fine-grained GitHub token, this repo only, Contents = Read-only.
//                            Without it GitHub allows only 60 lookups an hour per Cloudflare address, which is too few.
//                            With it, the repo can also be made PRIVATE so nobody can find it at all.
//   ALLOWED_ORIGINS (text)   OPTIONAL comma list of download-page addresses. Default: the two Firebase addresses below.

const DEFAULT_ORIGINS = ['https://wzhdownload.web.app', 'https://wzhdownload.firebaseapp.com'];
const TAG_RE = /^[A-Za-z0-9_.-]{3,80}$/;
const GH = 'https://api.github.com';

const origins = (env) => (env.ALLOWED_ORIGINS ? String(env.ALLOWED_ORIGINS).split(',').map((s) => s.trim()).filter(Boolean) : DEFAULT_ORIGINS);

function cors(req, env, extra = {}) {
  const h = new Headers({ 'cache-control': 'no-store', ...extra });
  const o = req.headers.get('Origin');
  if (o && origins(env).includes(o)) {
    h.set('Access-Control-Allow-Origin', o);
    h.set('Vary', 'Origin');
    h.set('Access-Control-Expose-Headers', 'Content-Length, Content-Range, Accept-Ranges');
  }
  return h;
}
const jsonRes = (req, env, obj, status = 200) => {
  const h = cors(req, env, { 'content-type': 'application/json' });
  return new Response(JSON.stringify(obj), { status, headers: h });
};

async function getRelease(env, tag) {
  const headers = { Accept: 'application/vnd.github+json', 'User-Agent': 'wzh-share-worker' };
  if (env.SHARE_TOKEN) headers.Authorization = 'Bearer ' + env.SHARE_TOKEN;
  const r = await fetch(GH + '/repos/' + env.SHARE_REPO + '/releases/tags/' + encodeURIComponent(tag), {
    headers,
    cf: { cacheTtl: 60, cacheEverything: true },
  });
  if (!r.ok) { const e = new Error('upstream ' + r.status); e.status = r.status; throw e; }
  return r.json();
}
const pickApk = (rel) => (rel.assets || []).find((x) => /\.apk$/i.test(x.name)) || (rel.assets || [])[0];
const pickLogo = (rel) => (rel.assets || []).find((x) => /\.(png|jpe?g|webp|gif)$/i.test(x.name) || /^image\//.test(x.content_type || ''));

// Streams one release file. Never forwards GitHub's headers or redirect: only a clean, small set.
async function streamAsset(req, env, asset, { download }) {
  const up = new Headers({ 'User-Agent': 'wzh-share-worker' });
  let url = asset.browser_download_url;
  if (env.SHARE_TOKEN) {            // token mode works for private repos too
    url = asset.url;
    up.set('Accept', 'application/octet-stream');
    up.set('Authorization', 'Bearer ' + env.SHARE_TOKEN);
  }
  const range = req.headers.get('Range');
  if (range) up.set('Range', range);
  const r = await fetch(url, { headers: up, redirect: 'follow' });
  if (!(r.status === 200 || r.status === 206)) { const e = new Error('upstream ' + r.status); e.status = r.status; throw e; }
  const h = cors(req, env, {
    'content-type': download ? 'application/vnd.android.package-archive' : (asset.content_type || 'image/png'),
    'accept-ranges': 'bytes',
    'cache-control': download ? 'no-store' : 'public, max-age=3600',
  });
  for (const k of ['content-length', 'content-range']) { const v = r.headers.get(k); if (v) h.set(k, v); }
  if (download) h.set('content-disposition', 'attachment; filename="' + asset.name.replace(/[^A-Za-z0-9._ -]/g, '_') + '"');
  h.set('X-Content-Type-Options', 'nosniff');
  return new Response(r.body, { status: r.status, headers: h });
}

export default {
  async fetch(req, env) {
    if (req.method === 'OPTIONS') {
      const h = cors(req, env, { 'Access-Control-Allow-Methods': 'GET, OPTIONS', 'Access-Control-Allow-Headers': 'Range' });
      return new Response(null, { status: 204, headers: h });
    }
    if (req.method !== 'GET' && req.method !== 'HEAD') return jsonRes(req, env, { error: 'Not allowed' }, 405);
    if (!env.SHARE_REPO) return jsonRes(req, env, { error: 'SHARE_REPO is not set on this Worker.' }, 500);

    const m = /^\/([mfl])\/([^/]+)$/.exec(new URL(req.url).pathname);
    if (!m) return jsonRes(req, env, { error: 'Not found' }, 404);
    const [, kind] = m;
    const tag = decodeURIComponent(m[2]);
    if (!TAG_RE.test(tag)) return jsonRes(req, env, { error: 'Not found' }, 404);

    try {
      const rel = await getRelease(env, tag);
      const apk = pickApk(rel);
      if (!apk) return jsonRes(req, env, { error: 'Not found' }, 404);
      if (kind === 'm') {
        const dg = /^sha256:([0-9a-f]{64})$/i.exec(apk.digest || '');
        return jsonRes(req, env, {
          name: String(rel.name || apk.name.replace(/\.apk$/i, '')),
          file: apk.name,
          size: apk.size,
          created: apk.created_at,
          downloads: apk.download_count,
          sha256: dg ? dg[1].toLowerCase() : '',
          logo: !!pickLogo(rel),
        });
      }
      if (kind === 'f') return await streamAsset(req, env, apk, { download: true });
      const logo = pickLogo(rel);
      if (!logo) return jsonRes(req, env, { error: 'Not found' }, 404);
      return await streamAsset(req, env, logo, { download: false });
    } catch (e) {
      const s = e && e.status;
      if (s === 404) return jsonRes(req, env, { error: 'Not found' }, 404);
      if (s === 403 || s === 429) return jsonRes(req, env, { error: 'Busy, try again soon.' }, 429);
      return jsonRes(req, env, { error: 'Download failed. Please try again.' }, 502);
    }
  },
};
