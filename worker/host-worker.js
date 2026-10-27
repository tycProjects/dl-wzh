// WZH 2 APK -- "Host a site" Worker.  Paste this whole file into a Cloudflare Worker.
//
// The phone does the heavy work (unzip, hash, upload the files straight to Cloudflare). This Worker
// only holds your Cloudflare key and does the small, trusted steps: create / list / delete a site,
// hand the phone a short-lived upload pass, and publish the finished upload. It never sees the files.
//
// Settings > Variables and Secrets (same names as on Render):
//   CF_ACCOUNT_ID      (text)    your Cloudflare account id
//   CF_PAGES_TOKEN     (secret)  API token with  Account > Cloudflare Pages > Edit
//   HOST_SECRET        (secret)  the SAME value as HOST_SECRET on Render (so existing sites stay theirs)
// Optional:
//   HOST_MAX_PER_USER  (text, default 2)    live sites one install may have
//   HOST_MAX_PER_DAY   (text, default 5)    new sites per IP per day (needs the HOST_KV binding below)
//   HOST_MAX_SITES     (text, default 80)   max sites alive on the account
//   ALLOWED_ORIGINS    (text)               comma list; default: your two site origins
//   APP_UA_TOKEN       (text, default TyceptOfficial)  the app token the request User-Agent must contain
//   HOST_KV            (KV binding)         only needed for the per-IP daily limit

const CF = 'https://api.cloudflare.com/client/v4';
const BLOCKED_WORDS = ['paypal', 'google', 'facebook', 'instagram', 'whatsapp', 'telegram', 'apple', 'icloud', 'microsoft', 'amazon', 'netflix',
  'binance', 'coinbase', 'metamask', 'wallet', 'bank', 'login', 'signin', 'secure', 'verify', 'account', 'support', 'admin', 'official', 'tiktok', 'youtube', 'snapchat'];
const DEFAULT_ORIGINS = ['https://zip-to-apk.pages.dev', 'https://wzh2apk.rweb.site'];
const SPECIAL = ['_headers', '_redirects', '_routes.json'];

class HostError extends Error {
  constructor(status, message, code) { super(message); this.status = status; this.code = code; }
}
const json = (obj, status = 200) =>
  new Response(JSON.stringify(obj), { status, headers: { 'content-type': 'application/json', 'cache-control': 'no-store' } });

export default {
  async fetch(req, env) {
    try {
      return await route(req, env);
    } catch (e) {
      if (e instanceof HostError) return json({ error: e.message, code: e.code }, e.status);
      console.error('[host] failed:', e && e.message);
      const hint = e && (e.cfStatus === 403 || e.cfCode === 10000)
        ? 'The Cloudflare token is missing the "Cloudflare Pages: Edit" permission.'
        : 'Hosting failed. Please try again.';
      return json({ error: hint }, 500);
    }
  }
};

function config(env) {
  const account = env.CF_ACCOUNT_ID, token = env.CF_PAGES_TOKEN;
  if (!account || !token) throw new HostError(503, 'Hosting is not set up yet.');
  const num = (v, d) => (Number(v) > 0 ? Number(v) : d);
  return {
    account, token,
    secret: env.HOST_SECRET || token,
    maxPerUser: num(env.HOST_MAX_PER_USER, 2),
    maxPerDay: num(env.HOST_MAX_PER_DAY, 5),
    maxSites: num(env.HOST_MAX_SITES, 80)
  };
}

async function cf(cfg, method, url, body) {
  const res = await fetch(url.startsWith('http') ? url : CF + url, {
    method,
    headers: Object.assign({ Authorization: `Bearer ${cfg.token}` }, body && !(body instanceof FormData) ? { 'Content-Type': 'application/json' } : {}),
    body: body ? (body instanceof FormData ? body : JSON.stringify(body)) : undefined
  });
  let j = null;
  try { j = await res.json(); } catch { /* non-json */ }
  if (!res.ok || (j && j.success === false)) {
    const first = j && j.errors && j.errors[0];
    const err = new Error(`Cloudflare ${method} ${url.replace(CF, '').split('?')[0]} -> ${first ? `${first.code}: ${first.message}` : 'HTTP ' + res.status}`);
    err.cfStatus = res.status;
    err.cfCode = first && first.code;
    throw err;
  }
  return j;
}

async function ownerKey(cfg, id) {
  const key = await crypto.subtle.importKey('raw', new TextEncoder().encode(cfg.secret), { name: 'HMAC', hash: 'SHA-256' }, false, ['sign']);
  const sig = new Uint8Array(await crypto.subtle.sign('HMAC', key, new TextEncoder().encode('owner:' + id)));
  return [...sig].map((b) => b.toString(16).padStart(2, '0')).join('').slice(0, 6);
}
async function ownerOf(cfg, req) {
  const id = String(req.headers.get('x-host-owner') || '').trim();
  if (!/^[A-Za-z0-9_-]{8,64}$/.test(id)) throw new HostError(400, 'Open this from the app to host a site.');
  return ownerKey(cfg, id);
}
function cleanName(raw) {
  const n = String(raw || '').trim().toLowerCase().replace(/[\s_]+/g, '-').replace(/-+/g, '-').replace(/^-|-$/g, '');
  if (!/^[a-z0-9][a-z0-9-]{1,28}[a-z0-9]$/.test(n)) throw new HostError(400, 'Use 3-30 letters, numbers or dashes for the site name.');
  if (BLOCKED_WORDS.some((w) => n.includes(w))) throw new HostError(400, 'That name isn\u2019t allowed. Pick another one.');
  return n;
}
const isOwnName = (name, ok) => new RegExp(`^[a-z0-9][a-z0-9-]{1,38}-${ok}$`).test(name);

function guard(req, env) {
  const origins = env.ALLOWED_ORIGINS ? String(env.ALLOWED_ORIGINS).split(',').map((s) => s.trim()).filter(Boolean) : DEFAULT_ORIGINS;
  if (!origins.includes(req.headers.get('origin') || '')) throw new HostError(403, 'Open this from the app to host a site.');
  const tok = env.APP_UA_TOKEN || 'TyceptOfficial';
  if (!String(req.headers.get('user-agent') || '').includes(tok)) throw new HostError(403, 'Open this from the app to host a site.');
}

// Cloudflare only lists 10 projects per page.
async function allProjects(cfg) {
  const all = [];
  for (let page = 1; page <= 30; page++) {
    const j = await cf(cfg, 'GET', `/accounts/${cfg.account}/pages/projects?per_page=10&page=${page}`);
    const list = j.result || [];
    all.push(...list);
    const total = j.result_info && j.result_info.total_pages;
    if (list.length < 10 || (total && page >= total)) break;
  }
  return all;
}
const siteOf = (p) => ({ name: p.name, url: `https://${p.subdomain || p.name + '.pages.dev'}` });

async function body(req) {
  try { const b = await req.json(); return b && typeof b === 'object' ? b : {}; } catch { return {}; }
}

async function route(req, env) {
  const url = new URL(req.url);
  const p = url.pathname.replace(/^\/api(?=\/host)/, '').replace(/\/+$/, '') || '/';
  if (req.method === 'GET' && (p === '/' || p === '/health')) return json({ ok: true });
  if (!p.startsWith('/host')) throw new HostError(404, 'Not found.');
  guard(req, env);
  const cfg = config(env);
  const ok = await ownerOf(cfg, req);

  // GET /host/mine -> { limit, suffix, sites }
  if (req.method === 'GET' && p === '/host/mine') {
    const sites = (await allProjects(cfg)).filter((x) => isOwnName(String(x.name), ok)).map(siteOf);
    return json({ limit: cfg.maxPerUser, suffix: ok, sites });
  }

  // POST /host/new { siteName? } -> { name, url, jwt }   creates the project + an upload pass
  if (req.method === 'POST' && p === '/host/new') {
    const b = await body(req);
    const wanted = b.siteName ? cleanName(b.siteName) : 'site-' + [...crypto.getRandomValues(new Uint8Array(3))].map((x) => x.toString(16).padStart(2, '0')).join('');
    const name = `${wanted}-${ok}`;
    const all = await allProjects(cfg);
    const own = all.filter((x) => isOwnName(String(x.name), ok));
    if (own.some((x) => x.name === name)) throw new HostError(409, 'You already have a site with that name. Pick another one.');
    if (own.length >= cfg.maxPerUser) throw new HostError(403, `You already have ${cfg.maxPerUser} hosted sites. Delete one to host another.`, 'site_limit');
    if (all.filter((x) => /-[a-f0-9]{6}$/.test(String(x.name))).length >= cfg.maxSites) throw new HostError(503, 'Hosting is full right now. Please try again later.');
    let day = null;
    if (env.HOST_KV) {
      const ip = req.headers.get('cf-connecting-ip') || 'x';
      const key = `d:${new Date().toISOString().slice(0, 10)}:${ip}`;
      const n = parseInt(await env.HOST_KV.get(key), 10) || 0;
      if (n >= cfg.maxPerDay) throw new HostError(429, `That\u2019s ${cfg.maxPerDay} hosted sites for today. Come back after midnight UTC.`);
      day = { key, n };
    }
    let created;
    try {
      created = await cf(cfg, 'POST', `/accounts/${cfg.account}/pages/projects`, { name, production_branch: 'main' });
    } catch (e) {
      if (e.cfCode === 8000002) throw new HostError(409, 'That name is taken. Pick another one.');
      throw e;
    }
    if (day) await env.HOST_KV.put(day.key, String(day.n + 1), { expirationTtl: 172800 });
    const jwt = (await cf(cfg, 'GET', `/accounts/${cfg.account}/pages/projects/${name}/upload-token`)).result.jwt;
    return json({ name, url: siteOf(created.result || { name }).url, jwt });
  }

  // POST /host/token { name } -> { jwt }   upload pass for updating one of your own sites
  if (req.method === 'POST' && p === '/host/token') {
    const name = String((await body(req)).name || '').toLowerCase();
    if (!isOwnName(name, ok)) throw new HostError(403, 'That is not your site.');
    const jwt = (await cf(cfg, 'GET', `/accounts/${cfg.account}/pages/projects/${name}/upload-token`)).result.jwt;
    return json({ name, jwt });
  }

  // POST /host/deploy { name, manifest, special } -> { name, url, deploymentUrl }
  if (req.method === 'POST' && p === '/host/deploy') {
    const b = await body(req);
    const name = String(b.name || '').toLowerCase();
    if (!isOwnName(name, ok)) throw new HostError(403, 'That is not your site.');
    const manifest = b.manifest && typeof b.manifest === 'object' ? b.manifest : {};
    const paths = Object.keys(manifest);
    if (!paths.length || paths.length > 2000 || !paths.every((k) => k.startsWith('/') && /^[a-f0-9]{32}$/.test(String(manifest[k])))) {
      throw new HostError(400, 'Nothing to publish in that upload.');
    }
    const form = new FormData();
    form.append('manifest', JSON.stringify(manifest));
    form.append('branch', 'main');
    const special = b.special && typeof b.special === 'object' ? b.special : {};
    for (const k of Object.keys(special)) {
      if (!SPECIAL.includes(k) || typeof special[k] !== 'string' || special[k].length > 1400000) continue;
      form.append(k, new Blob([Uint8Array.from(atob(special[k]), (c) => c.charCodeAt(0))]), k);
    }
    const dep = await cf(cfg, 'POST', `/accounts/${cfg.account}/pages/projects/${name}/deployments`, form);
    const proj = await cf(cfg, 'GET', `/accounts/${cfg.account}/pages/projects/${name}`);
    return json({ name, url: siteOf(proj.result || { name }).url, deploymentUrl: dep.result && dep.result.url });
  }

  // DELETE /host/:name
  if (req.method === 'DELETE' && p.startsWith('/host/')) {
    const name = decodeURIComponent(p.slice('/host/'.length)).toLowerCase();
    if (!isOwnName(name, ok)) throw new HostError(403, 'That is not your site.');
    await cf(cfg, 'DELETE', `/accounts/${cfg.account}/pages/projects/${name}`);
    return json({ deleted: name });
  }

  throw new HostError(404, 'Not found.');
}
