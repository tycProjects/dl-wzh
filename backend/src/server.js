'use strict';
// DroidStore backend. Без внешних зависимостей: только встроенные модули Node.js 18+.
// Хранилище: JSON-файл (для продакшена замените на PostgreSQL по database/schema.sql).

const http = require('http');
const fs = require('fs');
const path = require('path');
const crypto = require('crypto');

const PORT = Number(process.env.PORT || 8080);
const DATA_DIR = process.env.DATA_DIR || path.join(__dirname, '..', 'data');
const OWNER_EMAIL = (process.env.OWNER_EMAIL || '').trim().toLowerCase();
const BOT_TOKEN = process.env.TELEGRAM_BOT_TOKEN || '';          // секрет: только в .env на сервере
const BOT_USERNAME = (process.env.TELEGRAM_BOT_USERNAME || '').replace(/^@/, '');
const WEBHOOK_SECRET = process.env.TELEGRAM_WEBHOOK_SECRET || '';  // случайная строка, проверяет запросы Telegram
const PUBLIC_URL = process.env.PUBLIC_URL || '';                  // https-адрес сервера для webhook
const OWNER_TELEGRAM_ID = String(process.env.OWNER_TELEGRAM_ID || '');
const LOGIN_TTL_MS = 10 * 60 * 1000;
const VT_KEY = process.env.VIRUSTOTAL_API_KEY || '';
const DEV_AUTH = process.env.DEV_AUTH === '1'; // только для локальной проверки, в продакшене выключено
const ADMIN_DIR = path.join(__dirname, '..', '..', 'admin-panel');
const APK_DIR = path.join(DATA_DIR, 'apk');
const IMG_DIR = path.join(DATA_DIR, 'images');
const DB_FILE = path.join(DATA_DIR, 'db.json');
const MAX_APK = 200 * 1024 * 1024;
const MAX_IMG = 5 * 1024 * 1024;
const MAX_SCREENSHOTS = 10;

const ROLE_RANK = { user: 0, developer: 1, moderator: 2, admin: 3, owner: 4 };
const BADGES = ['verified_developer', 'verified_studio', 'official_app', 'security_checked', 'featured', 'new_developer'];
const CATEGORIES = ['tools', 'games', 'productivity', 'education', 'music', 'photo', 'social', 'finance', 'health', 'other'];
const EDITABLE_STATUSES = ['draft', 'changes_requested', 'rejected'];

fs.mkdirSync(APK_DIR, { recursive: true });
fs.mkdirSync(IMG_DIR, { recursive: true });

// ---------- Хранилище ----------
let db = fs.existsSync(DB_FILE)
  ? JSON.parse(fs.readFileSync(DB_FILE, 'utf8'))
  : { seq: 0, users: [], sessions: [], studios: [], apps: [], versions: [], images: [], ratings: [], reports: [], audit: [] };

function save() {
  fs.writeFileSync(DB_FILE + '.tmp', JSON.stringify(db, null, 2));
  fs.renameSync(DB_FILE + '.tmp', DB_FILE);
}
const nextId = () => ++db.seq;
db.loginSessions ||= [];
const now = () => new Date().toISOString();

function audit(actor, action, target, details = {}) {
  db.audit.push({ id: nextId(), at: now(), actorId: actor ? actor.id : null, actorEmail: actor ? actor.email : 'system', action, target, details });
  save();
}

// ---------- Утилиты HTTP ----------
class HttpError extends Error {
  constructor(status, message) { super(message); this.status = status; }
}
const fail = (status, message) => { throw new HttpError(status, message); };

function send(res, status, data) {
  const body = JSON.stringify(data);
  res.writeHead(status, {
    'Content-Type': 'application/json; charset=utf-8',
    'X-Content-Type-Options': 'nosniff',
    'Cache-Control': 'no-store',
  });
  res.end(body);
}

function readBody(req, limit) {
  return new Promise((resolve, reject) => {
    const chunks = [];
    let size = 0;
    req.on('data', (c) => {
      size += c.length;
      if (size > limit) { reject(new HttpError(413, 'payload_too_large')); req.destroy(); return; }
      chunks.push(c);
    });
    req.on('end', () => resolve(Buffer.concat(chunks)));
    req.on('error', reject);
  });
}

async function readJson(req) {
  const buf = await readBody(req, 1024 * 1024);
  if (!buf.length) return {};
  try { return JSON.parse(buf.toString('utf8')); } catch { fail(400, 'invalid_json'); }
}

function str(v, max, field, required = false) {
  if (v === undefined || v === null || v === '') {
    if (required) fail(400, `field_required:${field}`);
    return '';
  }
  if (typeof v !== 'string' || v.length > max) fail(400, `invalid_field:${field}`);
  return v.trim();
}

// ---------- Аутентификация и роли ----------
function currentUser(req) {
  const h = req.headers.authorization || '';
  const token = h.startsWith('Bearer ') ? h.slice(7) : '';
  if (!token) return null;
  const s = db.sessions.find((x) => x.token === token);
  if (!s) return null;
  const u = db.users.find((x) => x.id === s.userId);
  return u || null;
}

function createSession(user) {
  const token = crypto.randomBytes(32).toString('hex');
  db.sessions.push({ token, userId: user.id, createdAt: now() });
  save();
  return token;
}

async function tgApi(method, payload) {
  if (!BOT_TOKEN) fail(503, 'telegram_not_configured');
  const r = await fetch(`https://api.telegram.org/bot${BOT_TOKEN}/${method}`, {
    method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(payload),
  });
  return r.json();
}

function upsertTelegramUser(tg) {
  let u = db.users.find((x) => x.telegramId === tg.id);
  if (!u) {
    u = { id: nextId(), telegramId: tg.id, email: tg.username ? '@' + tg.username : 'tg:' + tg.id,
      name: tg.name || tg.username || 'Пользователь', role: 'user', blocked: false, createdAt: now() };
    db.users.push(u);
    audit(null, 'user.created', `user:${u.id}`, { telegramId: tg.id });
  }
  u.name = tg.name || u.name;
  if (tg.username) u.email = '@' + tg.username;
  // Владелец задаётся через OWNER_TELEGRAM_ID в .env, через API назначить нельзя.
  if (OWNER_TELEGRAM_ID && tg.id === OWNER_TELEGRAM_ID && u.role !== 'owner') {
    u.role = 'owner';
    audit(u, 'user.owner_assigned_from_config', `user:${u.id}`);
  }
  save();
  return u;
}

function upsertUser(identity) {
  let u = db.users.find((x) => x.email === identity.email);
  if (!u) {
    u = { id: nextId(), email: identity.email, name: identity.name, picture: identity.picture, role: 'user', blocked: false, createdAt: now() };
    db.users.push(u);
    audit(null, 'user.created', `user:${u.id}`, { email: u.email });
  }
  u.name = identity.name || u.name;
  // Владелец задаётся только переменной окружения OWNER_EMAIL, через API его назначить нельзя.
  if (OWNER_EMAIL && identity.email === OWNER_EMAIL && u.role !== 'owner') {
    u.role = 'owner';
    audit(u, 'user.owner_assigned_from_config', `user:${u.id}`);
  }
  save();
  return u;
}

function publicUser(u) {
  return { id: u.id, email: u.email, name: u.name, picture: u.picture, role: u.role, blocked: u.blocked };
}

// ---------- Вспомогательные функции для приложений ----------
const studioById = (id) => db.studios.find((s) => s.id === id);
const appById = (id) => db.apps.find((a) => a.id === id);
const imageById = (id) => db.images.find((i) => i.id === id);
const isStudioMember = (user, studio) => user && studio && studio.ownerId === user.id;
const canManageStudio = (user, studio) => isStudioMember(user, studio) || (user && ROLE_RANK[user.role] >= ROLE_RANK.admin);

function appRating(appId) {
  const rs = db.ratings.filter((r) => r.appId === appId);
  const avg = rs.length ? rs.reduce((s, r) => s + r.stars, 0) / rs.length : null;
  return { average: avg === null ? null : Math.round(avg * 10) / 10, count: rs.length };
}

function appPublicView(a) {
  const studio = studioById(a.studioId);
  return {
    id: a.id, studioId: a.studioId, studioName: studio ? studio.name : null,
    studioBadges: studio ? studio.badges : [],
    title: a.title, packageName: a.packageName, category: a.category,
    shortDescription: a.shortDescription, description: a.description,
    website: a.website, supportEmail: a.supportEmail, privacyPolicyUrl: a.privacyPolicyUrl,
    iconId: a.iconId, bannerId: a.bannerId, screenshotIds: a.screenshotIds,
    currentVersion: a.currentVersion, status: a.status, badges: a.badges,
    downloads: a.downloads, rating: appRating(a.id),
    security: a.security ? { status: a.security.status, score: a.security.score, checkedAt: a.security.checkedAt, methods: a.security.methods, limitations: a.security.limitations } : null,
    publishedAt: a.publishedAt || null,
  };
}

function checkApkLooksValid(buf) {
  // APK — это ZIP-архив: начинается с 'PK' и содержит AndroidManifest.xml.
  if (buf.length < 4 || buf[0] !== 0x50 || buf[1] !== 0x4b) return false;
  return buf.indexOf(Buffer.from('AndroidManifest.xml')) !== -1;
}

async function runSecurityScan(app, version) {
  const result = {
    status: 'unknown', score: null, checkedAt: now(), sha256: version.sha256,
    methods: ['sha256', 'zip_structure', 'manifest_presence'],
    limitations: [
      'Цифровая подпись APK не проверена (нужен apksigner, интеграция не подключена).',
      'Разрешения не извлечены из бинарного манифеста (нужен aapt2/bundletool).',
      'Поведенческий анализ не выполнялся.',
    ],
    vt: null,
  };
  if (!VT_KEY) {
    result.limitations.push('VirusTotal не подключён: VIRUSTOTAL_API_KEY не задан. Статус Unknown.');
    return result;
  }
  try {
    const r = await fetch('https://www.virustotal.com/api/v3/files/' + version.sha256, { headers: { 'x-apikey': VT_KEY } });
    if (r.status === 404) {
      result.vt = { found: false };
      result.limitations.push('Файл не найден в VirusTotal: это не доказывает безопасность.');
      return result;
    }
    if (!r.ok) { result.limitations.push(`VirusTotal вернул HTTP ${r.status}.`); return result; }
    const j = await r.json();
    const st = j.data.attributes.last_analysis_stats || {};
    const bad = (st.malicious || 0) + (st.suspicious || 0);
    result.vt = { found: true, malicious: st.malicious || 0, suspicious: st.suspicious || 0, harmless: st.harmless || 0 };
    if (bad > 0) {
      result.score = Math.max(0, 39 - 10 * bad);
      result.status = result.score < 40 ? 'high_risk' : 'warning';
    } else {
      // Отсутствие обнаружений не означает безопасность: максимум 'needs_review'.
      result.score = 75;
      result.status = 'needs_review';
      result.limitations.push('Отсутствие обнаружений не является подтверждением безопасности.');
    }
  } catch (e) {
    result.limitations.push('Ошибка обращения к VirusTotal: ' + e.message);
  }
  return result;
}

// ---------- Маршруты ----------
const routes = [];
function route(method, pattern, handler, opts = {}) {
  const re = new RegExp('^' + pattern.replace(/:(\w+)/g, '(?<$1>[^/]+)') + '$');
  routes.push({ method, re, handler, opts });
}

// Конфиг для клиентов (секретов здесь нет)
route('GET', '/api/config', async ({ res }) => send(res, 200, { telegramBot: BOT_USERNAME, devAuth: DEV_AUTH }));

// Вход через Telegram: 1) приложение получает ссылку, 2) пользователь нажимает Start в боте,
// 3) бот подтверждает сессию через webhook, 4) приложение забирает токен через status.
route('POST', '/api/auth/telegram/start', async ({ res }) => {
  if (!BOT_USERNAME || !BOT_TOKEN) fail(503, 'telegram_not_configured');
  const sid = crypto.randomBytes(16).toString('hex');
  db.loginSessions = db.loginSessions.filter((x) => Date.now() - x.createdAt < LOGIN_TTL_MS);
  db.loginSessions.push({ sid, createdAt: Date.now(), confirmed: false, consumed: false, tg: null });
  save();
  send(res, 200, { sid, url: `https://t.me/${BOT_USERNAME}?start=${sid}`, expiresInSec: LOGIN_TTL_MS / 1000 });
});

route('GET', '/api/auth/telegram/status', async ({ res, url }) => {
  const sid = url.searchParams.get('sid') || '';
  const s = db.loginSessions.find((x) => x.sid === sid);
  if (!s || Date.now() - s.createdAt > LOGIN_TTL_MS) fail(404, 'login_expired');
  if (!s.confirmed) return send(res, 200, { status: 'pending' });
  if (s.consumed) fail(410, 'login_already_used');
  s.consumed = true;
  const user = upsertTelegramUser(s.tg);
  if (user.blocked) fail(403, 'account_blocked');
  const token = createSession(user);
  send(res, 200, { status: 'ok', token, user: publicUser(user) });
});

// Webhook для бота. Telegram присылает секрет в заголовке; без него запрос отклоняется.
route('POST', '/api/telegram/webhook', async ({ req, res }) => {
  if (!WEBHOOK_SECRET || req.headers['x-telegram-bot-api-secret-token'] !== WEBHOOK_SECRET) fail(403, 'forbidden');
  const upd = await readJson(req);
  const msg = upd.message;
  if (!msg || !msg.from || !msg.chat || typeof msg.text !== 'string') return send(res, 200, { ok: true });
  const reply = (text) => tgApi('sendMessage', { chat_id: msg.chat.id, text }).catch(() => {});
  const m = msg.text.match(/^\/start\s+([a-f0-9]{32})$/);
  if (m) {
    const s = db.loginSessions.find((x) => x.sid === m[1] && !x.confirmed && Date.now() - x.createdAt < LOGIN_TTL_MS);
    if (!s) { await reply('Ссылка для входа устарела. Начните вход заново в приложении.'); return send(res, 200, { ok: true }); }
    s.confirmed = true;
    s.tg = {
      id: String(msg.from.id),
      name: [msg.from.first_name, msg.from.last_name].filter(Boolean).join(' '),
      username: msg.from.username || null,
    };
    save();
    await reply('Вход в DroidStore подтверждён. Вернитесь в приложение.');
  } else if (msg.text === '/start') {
    await reply('Привет! Откройте DroidStore и нажмите «Войти через Telegram», чтобы подтвердить вход.');
  }
  send(res, 200, { ok: true });
});

route('POST', '/api/auth/logout', async ({ req, res }) => {
  const h = req.headers.authorization || '';
  db.sessions = db.sessions.filter((s) => s.token !== h.slice(7));
  save();
  send(res, 200, { ok: true });
});

route('GET', '/api/me', async ({ res, user }) => send(res, 200, { user: publicUser(user) }), { auth: true });

// Только для локальной разработки (DEV_AUTH=1). Без проверки Telegram — не использовать в продакшене.
route('POST', '/api/auth/dev', async ({ req, res }) => {
  if (!DEV_AUTH) fail(404, 'not_found');
  const { email, name } = await readJson(req);
  const user = upsertUser({ email: str(email, 200, 'email', true).toLowerCase(), name: str(name, 100, 'name') || 'Dev user', picture: null });
  const token = createSession(user);
  send(res, 200, { token, user: publicUser(user) });
});

// ----- Студии -----
route('POST', '/api/studios', async ({ req, res, user }) => {
  const b = await readJson(req);
  const studio = {
    id: nextId(), ownerId: user.id,
    name: str(b.name, 60, 'name', true),
    description: str(b.description, 4000, 'description'),
    contactEmail: str(b.contactEmail, 200, 'contactEmail', true),
    website: str(b.website, 300, 'website'),
    logoId: null,
    status: 'newcomer',
    badges: ['new_developer'],
    createdAt: now(),
  };
  if (studio.name.length < 2) fail(400, 'invalid_field:name');
  if (!/^[^@\s]+@[^@\s]+\.[^@\s]+$/.test(studio.contactEmail)) fail(400, 'invalid_field:contactEmail');
  db.studios.push(studio);
  if (user.role === 'user') user.role = 'developer';
  audit(user, 'studio.created', `studio:${studio.id}`, { name: studio.name });
  save();
  send(res, 201, { studio });
}, { auth: true });

route('GET', '/api/studios/mine', async ({ res, user }) => {
  send(res, 200, { studios: db.studios.filter((s) => s.ownerId === user.id) });
}, { auth: true });

route('GET', '/api/studios/:id', async ({ res, params }) => {
  const s = studioById(Number(params.id));
  if (!s) fail(404, 'studio_not_found');
  const apps = db.apps.filter((a) => a.studioId === s.id && a.status === 'published');
  send(res, 200, { studio: s, appsCount: apps.length, downloads: apps.reduce((n, a) => n + a.downloads, 0) });
});

route('PATCH', '/api/studios/:id', async ({ req, res, user, params }) => {
  const s = studioById(Number(params.id));
  if (!s) fail(404, 'studio_not_found');
  if (!canManageStudio(user, s)) fail(403, 'forbidden');
  const b = await readJson(req);
  if (b.name !== undefined) s.name = str(b.name, 60, 'name', true);
  if (b.description !== undefined) s.description = str(b.description, 4000, 'description');
  if (b.contactEmail !== undefined) s.contactEmail = str(b.contactEmail, 200, 'contactEmail', true);
  if (b.website !== undefined) s.website = str(b.website, 300, 'website');
  if (b.logoId !== undefined) {
    const img = imageById(Number(b.logoId));
    if (!img || img.kind !== 'icon') fail(400, 'invalid_logo');
    s.logoId = img.id;
  }
  audit(user, 'studio.updated', `studio:${s.id}`);
  save();
  send(res, 200, { studio: s });
}, { auth: true });

// Галочки выдаются только администраторами
route('POST', '/api/admin/studios/:id/badges', async ({ req, res, user, params }) => {
  const s = studioById(Number(params.id));
  if (!s) fail(404, 'studio_not_found');
  const { badge, on } = await readJson(req);
  if (!BADGES.includes(badge)) fail(400, 'invalid_badge');
  s.badges = on ? [...new Set([...s.badges, badge])] : s.badges.filter((x) => x !== badge);
  audit(user, on ? 'studio.badge_granted' : 'studio.badge_revoked', `studio:${s.id}`, { badge });
  save();
  send(res, 200, { studio: s });
}, { auth: true, role: 'admin' });

// ----- Изображения -----
route('POST', '/api/images', async ({ req, res, user, url }) => {
  const kind = url.searchParams.get('kind');
  if (!['icon', 'banner', 'screenshot'].includes(kind)) fail(400, 'invalid_kind');
  const type = (req.headers['content-type'] || '').split(';')[0];
  const ext = { 'image/png': 'png', 'image/jpeg': 'jpg', 'image/webp': 'webp' }[type];
  if (!ext) fail(415, 'unsupported_image_type');
  const buf = await readBody(req, MAX_IMG);
  if (!buf.length) fail(400, 'empty_body');
  const id = nextId();
  const file = `${id}.${ext}`;
  fs.writeFileSync(path.join(IMG_DIR, file), buf);
  const img = { id, kind, mime: type, file, size: buf.length, uploadedBy: user.id, createdAt: now() };
  db.images.push(img);
  save();
  send(res, 201, { image: { id, kind, url: `/api/images/${id}` } });
}, { auth: true });

route('GET', '/api/images/:id', async ({ res, params }) => {
  const img = imageById(Number(params.id));
  if (!img) fail(404, 'image_not_found');
  res.writeHead(200, { 'Content-Type': img.mime, 'Cache-Control': 'public, max-age=86400', 'X-Content-Type-Options': 'nosniff' });
  fs.createReadStream(path.join(IMG_DIR, img.file)).pipe(res);
});

// ----- Приложения -----
route('POST', '/api/studios/:id/apps', async ({ req, res, user, params }) => {
  const s = studioById(Number(params.id));
  if (!s) fail(404, 'studio_not_found');
  if (!canManageStudio(user, s)) fail(403, 'forbidden');
  const b = await readJson(req);
  const packageName = str(b.packageName, 150, 'packageName', true);
  if (!/^[a-z][a-z0-9_]*(\.[a-z][a-z0-9_]*)+$/i.test(packageName)) fail(400, 'invalid_field:packageName');
  if (db.apps.some((a) => a.packageName === packageName)) fail(409, 'package_name_taken');
  const category = str(b.category, 30, 'category', true);
  if (!CATEGORIES.includes(category)) fail(400, 'invalid_category');
  const app = {
    id: nextId(), studioId: s.id, packageName,
    title: str(b.title, 60, 'title', true),
    shortDescription: str(b.shortDescription, 120, 'shortDescription', true),
    description: str(b.description, 4000, 'description', true),
    category,
    website: str(b.website, 300, 'website'),
    supportEmail: str(b.supportEmail, 200, 'supportEmail', true),
    privacyPolicyUrl: str(b.privacyPolicyUrl, 300, 'privacyPolicyUrl', true),
    minAndroid: Math.max(10, Math.min(16, Number(b.minAndroid) || 10)),
    iconId: null, bannerId: null, screenshotIds: [],
    currentVersion: null, status: 'draft', badges: [], downloads: 0,
    security: null, createdBy: user.id, createdAt: now(),
  };
  db.apps.push(app);
  audit(user, 'app.created', `app:${app.id}`, { packageName });
  save();
  send(res, 201, { app: appPublicView(app) });
}, { auth: true });

route('PATCH', '/api/apps/:id', async ({ req, res, user, params }) => {
  const a = appById(Number(params.id));
  if (!a) fail(404, 'app_not_found');
  if (!canManageStudio(user, studioById(a.studioId))) fail(403, 'forbidden');
  if (!EDITABLE_STATUSES.includes(a.status)) fail(409, 'app_not_editable_in_status:' + a.status);
  const b = await readJson(req);
  if (b.title !== undefined) a.title = str(b.title, 60, 'title', true);
  if (b.shortDescription !== undefined) a.shortDescription = str(b.shortDescription, 120, 'shortDescription', true);
  if (b.description !== undefined) a.description = str(b.description, 4000, 'description', true);
  if (b.website !== undefined) a.website = str(b.website, 300, 'website');
  if (b.supportEmail !== undefined) a.supportEmail = str(b.supportEmail, 200, 'supportEmail', true);
  if (b.privacyPolicyUrl !== undefined) a.privacyPolicyUrl = str(b.privacyPolicyUrl, 300, 'privacyPolicyUrl', true);
  if (b.category !== undefined) {
    if (!CATEGORIES.includes(b.category)) fail(400, 'invalid_category');
    a.category = b.category;
  }
  if (b.iconId !== undefined) { const i = imageById(Number(b.iconId)); if (!i || i.kind !== 'icon') fail(400, 'invalid_icon'); a.iconId = i.id; }
  if (b.bannerId !== undefined) { const i = imageById(Number(b.bannerId)); if (!i || i.kind !== 'banner') fail(400, 'invalid_banner'); a.bannerId = i.id; }
  if (b.screenshotIds !== undefined) {
    if (!Array.isArray(b.screenshotIds) || b.screenshotIds.length > MAX_SCREENSHOTS) fail(400, 'invalid_screenshots');
    const ids = b.screenshotIds.map(Number);
    if (ids.some((id) => { const i = imageById(id); return !i || i.kind !== 'screenshot'; })) fail(400, 'invalid_screenshots');
    a.screenshotIds = ids;
  }
  audit(user, 'app.updated', `app:${a.id}`);
  save();
  send(res, 200, { app: appPublicView(a) });
}, { auth: true });

// Загрузка APK: тело запроса — сам файл. Создаёт новую версию.
route('POST', '/api/apps/:id/versions', async ({ req, res, user, params, url }) => {
  const a = appById(Number(params.id));
  if (!a) fail(404, 'app_not_found');
  if (!canManageStudio(user, studioById(a.studioId))) fail(403, 'forbidden');
  if (!EDITABLE_STATUSES.includes(a.status)) fail(409, 'app_not_editable_in_status:' + a.status);
  const versionCode = Number(url.searchParams.get('versionCode'));
  const versionName = str(url.searchParams.get('versionName'), 40, 'versionName', true);
  const releaseNotes = str(url.searchParams.get('releaseNotes'), 2000, 'releaseNotes');
  if (!Number.isInteger(versionCode) || versionCode < 1) fail(400, 'invalid_versionCode');
  const prev = db.versions.filter((v) => v.appId === a.id);
  if (prev.some((v) => v.versionCode >= versionCode)) fail(409, 'versionCode_must_increase');
  const buf = await readBody(req, MAX_APK);
  if (!checkApkLooksValid(buf)) fail(400, 'invalid_apk');
  const sha256 = crypto.createHash('sha256').update(buf).digest('hex');
  const id = nextId();
  const file = `${a.id}-${versionCode}-${id}.apk`;
  fs.writeFileSync(path.join(APK_DIR, file), buf);
  const ver = { id, appId: a.id, versionCode, versionName, releaseNotes, file, size: buf.length, sha256, uploadedBy: user.id, createdAt: now() };
  db.versions.push(ver);
  a.currentVersion = { id, versionCode, versionName };
  audit(user, 'app.version_uploaded', `app:${a.id}`, { versionCode, versionName, sha256 });
  save();
  send(res, 201, { version: { id, versionCode, versionName, size: ver.size, sha256 } });
}, { auth: true });

route('GET', '/api/apps/:id/versions', async ({ res, user, params }) => {
  const a = appById(Number(params.id));
  if (!a) fail(404, 'app_not_found');
  const owner = canManageStudio(user, studioById(a.studioId));
  if (a.status !== 'published' && !owner) fail(404, 'app_not_found');
  const list = db.versions.filter((v) => v.appId === a.id)
    .map(({ id, versionCode, versionName, releaseNotes, size, sha256, createdAt }) => ({ id, versionCode, versionName, releaseNotes, size, sha256, createdAt }));
  send(res, 200, { versions: list });
}, { auth: true });

route('POST', '/api/apps/:id/submit', async ({ res, user, params }) => {
  const a = appById(Number(params.id));
  if (!a) fail(404, 'app_not_found');
  if (!canManageStudio(user, studioById(a.studioId))) fail(403, 'forbidden');
  if (!EDITABLE_STATUSES.includes(a.status)) fail(409, 'app_not_editable_in_status:' + a.status);
  if (!a.iconId) fail(400, 'icon_required');
  if (!a.screenshotIds.length) fail(400, 'screenshot_required');
  if (!a.currentVersion) fail(400, 'apk_required');
  const ver = db.versions.find((v) => v.id === a.currentVersion.id);
  a.status = 'security_scan';
  save();
  a.security = await runSecurityScan(a, ver);
  a.status = 'pending_review';
  audit(user, 'app.submitted', `app:${a.id}`, { securityStatus: a.security.status });
  save();
  send(res, 200, { app: appPublicView(a) });
}, { auth: true });

// Приложения студии (включая черновики) — только владелец студии или admin
route('GET', '/api/studios/:id/apps', async ({ res, user, params }) => {
  const s = studioById(Number(params.id));
  if (!s) fail(404, 'studio_not_found');
  if (!canManageStudio(user, s)) fail(403, 'forbidden');
  const list = db.apps.filter((a) => a.studioId === s.id).map(appPublicView);
  send(res, 200, { apps: list });
}, { auth: true });

// Публичный каталог: только опубликованные приложения
route('GET', '/api/apps', async ({ res, url }) => {
  const q = (url.searchParams.get('q') || '').toLowerCase();
  const category = url.searchParams.get('category');
  const sort = url.searchParams.get('sort') || 'downloads';
  let list = db.apps.filter((a) => a.status === 'published');
  if (q) list = list.filter((a) => a.title.toLowerCase().includes(q) || a.packageName.includes(q));
  if (category) list = list.filter((a) => a.category === category);
  const views = list.map(appPublicView);
  if (sort === 'rating') views.sort((x, y) => (y.rating.average || 0) - (x.rating.average || 0));
  else if (sort === 'new') views.sort((x, y) => String(y.publishedAt).localeCompare(String(x.publishedAt)));
  else views.sort((x, y) => y.downloads - x.downloads);
  send(res, 200, { apps: views });
});

route('GET', '/api/apps/:id', async ({ res, user, params }) => {
  const a = appById(Number(params.id));
  if (!a) fail(404, 'app_not_found');
  const canSee = a.status === 'published' || canManageStudio(user, studioById(a.studioId)) || (user && ROLE_RANK[user.role] >= ROLE_RANK.moderator);
  if (!canSee) fail(404, 'app_not_found');
  send(res, 200, { app: appPublicView(a) });
});

// Скачивание: только опубликованное приложение, счётчик увеличивается
route('GET', '/api/apps/:id/download', async ({ res, params, url }) => {
  const a = appById(Number(params.id));
  if (!a || a.status !== 'published') fail(404, 'app_not_found');
  const versionId = Number(url.searchParams.get('versionId')) || (a.currentVersion && a.currentVersion.id);
  const ver = db.versions.find((v) => v.id === versionId && v.appId === a.id);
  if (!ver) fail(404, 'version_not_found');
  a.downloads += 1;
  save();
  res.writeHead(200, {
    'Content-Type': 'application/vnd.android.package-archive',
    'Content-Length': ver.size,
    'Content-Disposition': `attachment; filename="${a.packageName}-${ver.versionName}.apk"`,
    'X-Content-Type-Options': 'nosniff',
    'X-SHA256': ver.sha256,
  });
  fs.createReadStream(path.join(APK_DIR, ver.file)).pipe(res);
});

// Оценки: одна оценка от пользователя, можно изменить
route('POST', '/api/apps/:id/ratings', async ({ req, res, user, params }) => {
  const a = appById(Number(params.id));
  if (!a || a.status !== 'published') fail(404, 'app_not_found');
  const b = await readJson(req);
  const stars = Number(b.stars);
  if (!Number.isInteger(stars) || stars < 1 || stars > 5) fail(400, 'invalid_stars');
  const comment = str(b.comment, 1000, 'comment');
  let r = db.ratings.find((x) => x.appId === a.id && x.userId === user.id);
  if (r) { r.stars = stars; r.comment = comment; r.updatedAt = now(); }
  else { r = { id: nextId(), appId: a.id, userId: user.id, userName: user.name, stars, comment, createdAt: now() }; db.ratings.push(r); }
  save();
  send(res, 200, { rating: r, summary: appRating(a.id) });
}, { auth: true });

route('GET', '/api/apps/:id/ratings', async ({ res, params }) => {
  const a = appById(Number(params.id));
  if (!a || a.status !== 'published') fail(404, 'app_not_found');
  const list = db.ratings.filter((r) => r.appId === a.id).map(({ id, stars, comment, userName, createdAt }) => ({ id, stars, comment, userName, createdAt }));
  send(res, 200, { summary: appRating(a.id), ratings: list });
});

// Жалобы (Scam report)
route('POST', '/api/apps/:id/reports', async ({ req, res, user, params }) => {
  const a = appById(Number(params.id));
  if (!a) fail(404, 'app_not_found');
  const b = await readJson(req);
  const reasons = ['fake_app', 'brand_impersonation', 'misleading_description', 'suspicious_payments', 'suspicious_behavior', 'malicious_file', 'fake_developer', 'copyright', 'other'];
  if (!reasons.includes(b.reason)) fail(400, 'invalid_reason');
  const rep = { id: nextId(), appId: a.id, reporterId: user.id, reason: b.reason, description: str(b.description, 3000, 'description', true), evidence: str(b.evidence, 1000, 'evidence'), status: 'new_report', createdAt: now() };
  db.reports.push(rep);
  save();
  send(res, 201, { report: rep });
}, { auth: true });

// ----- Администрирование (модераторы и выше) -----
route('GET', '/api/admin/dashboard', async ({ res }) => {
  const count = (arr, f) => arr.filter(f).length;
  send(res, 200, {
    users: db.users.length,
    developers: count(db.users, (u) => u.role === 'developer'),
    studios: db.studios.length,
    apps: db.apps.length,
    published: count(db.apps, (a) => a.status === 'published'),
    pendingReview: count(db.apps, (a) => a.status === 'pending_review'),
    rejected: count(db.apps, (a) => a.status === 'rejected'),
    suspended: count(db.apps, (a) => a.status === 'suspended'),
    reportsNew: count(db.reports, (r) => r.status === 'new_report'),
    recentActions: db.audit.slice(-10).reverse(),
  });
}, { auth: true, role: 'moderator' });

route('GET', '/api/admin/apps', async ({ res, url }) => {
  const status = url.searchParams.get('status');
  const list = db.apps.filter((a) => !status || a.status === status).map((a) => ({ ...appPublicView(a), versions: db.versions.filter((v) => v.appId === a.id).length }));
  send(res, 200, { apps: list });
}, { auth: true, role: 'moderator' });

route('POST', '/api/admin/apps/:id/decision', async ({ req, res, user, params }) => {
  const a = appById(Number(params.id));
  if (!a) fail(404, 'app_not_found');
  const { action, reason } = await readJson(req);
  const needsReason = ['reject', 'request_changes', 'suspend'];
  if (needsReason.includes(action) && !str(reason, 1000, 'reason')) fail(400, 'reason_required');
  const transitions = {
    approve: { from: ['pending_review'], to: 'approved' },
    request_changes: { from: ['pending_review'], to: 'changes_requested' },
    reject: { from: ['pending_review'], to: 'rejected' },
    publish: { from: ['approved'], to: 'published' },
    suspend: { from: ['published', 'approved'], to: 'suspended' },
    unpublish: { from: ['published'], to: 'approved' },
  };
  const t = transitions[action];
  if (!t) fail(400, 'invalid_action');
  if (!t.from.includes(a.status)) fail(409, `invalid_transition:${a.status}->${t.to}`);
  a.status = t.to;
  if (t.to === 'published') { a.publishedAt = now(); }
  a.lastDecision = { action, reason: reason || null, by: user.id, at: now() };
  audit(user, 'app.' + action, `app:${a.id}`, { reason: reason || null, to: t.to });
  save();
  send(res, 200, { app: appPublicView(a) });
}, { auth: true, role: 'moderator' });

// Отметки приложения (Official, Featured, Security Checked): только админ
route('POST', '/api/admin/apps/:id/badges', async ({ req, res, user, params }) => {
  const a = appById(Number(params.id));
  if (!a) fail(404, 'app_not_found');
  const { badge, on } = await readJson(req);
  if (!['official_app', 'security_checked', 'featured'].includes(badge)) fail(400, 'invalid_badge');
  if (badge === 'security_checked' && (!a.security || a.security.status === 'unknown')) fail(409, 'security_check_not_completed');
  a.badges = on ? [...new Set([...a.badges, badge])] : a.badges.filter((x) => x !== badge);
  audit(user, on ? 'app.badge_granted' : 'app.badge_revoked', `app:${a.id}`, { badge });
  save();
  send(res, 200, { app: appPublicView(a) });
}, { auth: true, role: 'admin' });

route('GET', '/api/admin/reports', async ({ res }) => {
  send(res, 200, { reports: db.reports });
}, { auth: true, role: 'moderator' });

route('POST', '/api/admin/reports/:id', async ({ req, res, user, params }) => {
  const r = db.reports.find((x) => x.id === Number(params.id));
  if (!r) fail(404, 'report_not_found');
  const { status } = await readJson(req);
  const allowed = ['new_report', 'under_investigation', 'confirmed', 'rejected', 'resolved'];
  if (!allowed.includes(status)) fail(400, 'invalid_status');
  r.status = status;
  audit(user, 'report.status_changed', `report:${r.id}`, { status });
  save();
  send(res, 200, { report: r });
}, { auth: true, role: 'moderator' });

route('GET', '/api/admin/users', async ({ res, url }) => {
  const q = (url.searchParams.get('q') || '').toLowerCase();
  const list = db.users.filter((u) => !q || u.email.includes(q) || u.name.toLowerCase().includes(q)).map(publicUser);
  send(res, 200, { users: list });
}, { auth: true, role: 'admin' });

route('POST', '/api/admin/users/:id/block', async ({ req, res, user, params }) => {
  const target = db.users.find((u) => u.id === Number(params.id));
  if (!target) fail(404, 'user_not_found');
  if (target.role === 'owner') fail(403, 'cannot_block_owner');
  if (ROLE_RANK[target.role] >= ROLE_RANK[user.role]) fail(403, 'cannot_act_on_equal_or_higher_role');
  const { blocked } = await readJson(req);
  target.blocked = Boolean(blocked);
  if (target.blocked) db.sessions = db.sessions.filter((s) => s.userId !== target.id);
  audit(user, target.blocked ? 'user.blocked' : 'user.unblocked', `user:${target.id}`);
  save();
  send(res, 200, { user: publicUser(target) });
}, { auth: true, role: 'admin' });

// Роли: назначать и снимать может только Owner. Владелец не назначается через API.
route('POST', '/api/admin/users/:id/role', async ({ req, res, user, params }) => {
  const target = db.users.find((u) => u.id === Number(params.id));
  if (!target) fail(404, 'user_not_found');
  if (target.id === user.id) fail(403, 'cannot_change_own_role');
  if (target.role === 'owner') fail(403, 'cannot_change_owner');
  const { role } = await readJson(req);
  if (!['user', 'developer', 'moderator', 'admin'].includes(role)) fail(400, 'invalid_role');
  const before = target.role;
  target.role = role;
  audit(user, 'user.role_changed', `user:${target.id}`, { from: before, to: role });
  save();
  send(res, 200, { user: publicUser(target) });
}, { auth: true, role: 'owner' });

route('GET', '/api/admin/audit', async ({ res, url }) => {
  const limit = Math.min(500, Number(url.searchParams.get('limit')) || 100);
  send(res, 200, { entries: db.audit.slice(-limit).reverse() });
}, { auth: true, role: 'admin' });

// ---------- Статика админ-панели ----------
const MIME = { '.html': 'text/html; charset=utf-8', '.js': 'application/javascript; charset=utf-8', '.css': 'text/css; charset=utf-8' };
function serveStatic(pathname, res) {
  const rel = pathname === '/admin' || pathname === '/admin/' ? 'index.html' : pathname.replace(/^\/admin\//, '');
  const file = path.normalize(path.join(ADMIN_DIR, rel));
  if (!file.startsWith(ADMIN_DIR)) return send(res, 403, { error: 'forbidden' });
  if (!fs.existsSync(file) || fs.statSync(file).isDirectory()) return send(res, 404, { error: 'not_found' });
  res.writeHead(200, { 'Content-Type': MIME[path.extname(file)] || 'application/octet-stream', 'X-Content-Type-Options': 'nosniff' });
  fs.createReadStream(file).pipe(res);
}

// ---------- Сервер ----------
const server = http.createServer(async (req, res) => {
  try {
    const url = new URL(req.url, 'http://localhost');
    if (!url.pathname.startsWith('/api/')) {
      if (url.pathname === '/admin' || url.pathname.startsWith('/admin/')) return serveStatic(url.pathname, res);
      return send(res, 404, { error: 'not_found' });
    }
    for (const r of routes) {
      if (r.method !== req.method) continue;
      const m = url.pathname.match(r.re);
      if (!m) continue;
      const user = currentUser(req);
      if (r.opts.auth && !user) return send(res, 401, { error: 'auth_required' });
      if (user && user.blocked) return send(res, 403, { error: 'account_blocked' });
      if (r.opts.role && ROLE_RANK[user.role] < ROLE_RANK[r.opts.role]) return send(res, 403, { error: 'forbidden' });
      return await r.handler({ req, res, url, params: m.groups || {}, user });
    }
    send(res, 404, { error: 'not_found' });
  } catch (e) {
    if (e instanceof HttpError) return send(res, e.status, { error: e.message });
    console.error(e);
    send(res, 500, { error: 'internal_error' });
  }
});

async function registerWebhook() {
  if (!BOT_TOKEN || !PUBLIC_URL || !WEBHOOK_SECRET) {
    console.log('Telegram webhook не настроен (нужны TELEGRAM_BOT_TOKEN, PUBLIC_URL, TELEGRAM_WEBHOOK_SECRET).');
    return;
  }
  try {
    const r = await tgApi('setWebhook', {
      url: PUBLIC_URL.replace(/\/$/, '') + '/api/telegram/webhook',
      secret_token: WEBHOOK_SECRET,
      allowed_updates: ['message'],
    });
    console.log('Telegram webhook:', r.ok ? 'установлен' : r.description);
  } catch (e) {
    console.error('Telegram webhook error:', e.message);
  }
}

if (require.main === module) {
  server.listen(PORT, () => {
    console.log(`DroidStore backend on :${PORT}`);
    registerWebhook();
  });
}
module.exports = { server };
