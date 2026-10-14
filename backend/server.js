'use strict';
const express = require('express');
const helmet = require('helmet');
const rateLimit = require('express-rate-limit');
const bcrypt = require('bcryptjs');
const jwt = require('jsonwebtoken');
const Database = require('better-sqlite3');

const app = express();
const PORT = Number(process.env.PORT || 8080);
const JWT_SECRET = process.env.JWT_SECRET;
if (!JWT_SECRET || JWT_SECRET.length < 32) {
  console.error('Set JWT_SECRET to a random secret of at least 32 characters.');
  process.exit(1);
}
const db = new Database(process.env.DB_PATH || './sensiscreen.sqlite');
db.pragma('journal_mode = WAL');
db.exec(`CREATE TABLE IF NOT EXISTS users (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  username TEXT NOT NULL UNIQUE COLLATE NOCASE,
  password_hash TEXT NOT NULL,
  created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP
)`);
// Safe migration for databases created by older SensiScreen versions.
const columns = new Set(db.prepare('PRAGMA table_info(users)').all().map(c => c.name));
if (!columns.has('role')) db.exec("ALTER TABLE users ADD COLUMN role TEXT NOT NULL DEFAULT 'user'");
if (!columns.has('locked')) db.exec('ALTER TABLE users ADD COLUMN locked INTEGER NOT NULL DEFAULT 0');

// Bootstrap the first administrator only from server-side environment variables.
// This never elevates an existing regular account and never resets an existing admin password.
async function bootstrapAdmin() {
  const username = process.env.ADMIN_BOOTSTRAP_USERNAME;
  const password = process.env.ADMIN_BOOTSTRAP_PASSWORD;
  if (!username && !password) return;
  if (!username || !password || !/^[a-zA-Z0-9_.-]{3,32}$/.test(username) || password.length < 12 || password.length > 128) {
    throw new Error('Set both ADMIN_BOOTSTRAP_USERNAME (3-32 valid characters) and ADMIN_BOOTSTRAP_PASSWORD (12-128 characters), or omit both.');
  }
  const existingAdmin = db.prepare("SELECT id FROM users WHERE role = 'admin' LIMIT 1").get();
  if (existingAdmin) return;
  const existing = db.prepare('SELECT id, role FROM users WHERE username = ?').get(username);
  if (existing) throw new Error('Bootstrap username already exists but is not an admin. Choose a new username; no account was promoted.');
  const hash = await bcrypt.hash(password, 12);
  db.prepare("INSERT INTO users (username, password_hash, role, locked) VALUES (?, ?, 'admin', 0)").run(username, hash);
  console.log(`Bootstrapped initial administrator: ${username}`);
}

app.disable('x-powered-by');
app.use(helmet());
app.use(express.json({ limit: '10kb' }));
app.use('/api/auth', rateLimit({ windowMs: 15 * 60 * 1000, limit: 30, standardHeaders: true, legacyHeaders: false }));
app.use('/api/admin', rateLimit({ windowMs: 15 * 60 * 1000, limit: 120, standardHeaders: true, legacyHeaders: false }));
const usernameOk = s => typeof s === 'string' && /^[a-zA-Z0-9_.-]{3,32}$/.test(s);
const passwordOk = s => typeof s === 'string' && s.length >= 8 && s.length <= 128;
function publicUser(user) {
  return { id: user.id, username: user.username, role: user.role, locked: Boolean(user.locked), createdAt: user.created_at };
}
function issueToken(user) {
  // Role is intentionally read from the database for each protected request, not trusted from this token.
  return jwt.sign({ sub: String(user.id), username: user.username }, JWT_SECRET, { expiresIn: '7d', issuer: 'sensiscreen-auth' });
}
function authenticate(req, res, next) {
  const header = req.get('authorization') || '';
  const token = header.startsWith('Bearer ') ? header.slice(7) : '';
  try {
    const payload = jwt.verify(token, JWT_SECRET, { issuer: 'sensiscreen-auth' });
    const user = db.prepare('SELECT id, username, role, locked, created_at FROM users WHERE id = ?').get(Number(payload.sub));
    if (!user || user.locked) return res.status(401).json({ error: 'Tài khoản không tồn tại hoặc đã bị khóa. Vui lòng đăng nhập lại.' });
    req.authUser = user;
    next();
  } catch (_) {
    return res.status(401).json({ error: 'Phiên đăng nhập không hợp lệ hoặc đã hết hạn.' });
  }
}
function requireAdmin(req, res, next) {
  if (!req.authUser || req.authUser.role !== 'admin') return res.status(403).json({ error: 'Bạn không có quyền quản trị.' });
  next();
}
app.get('/health', (_req, res) => res.json({ ok: true }));
app.post('/api/auth/register', async (req, res) => {
  const { username, password } = req.body || {};
  if (!usernameOk(username)) return res.status(400).json({ error: 'Tên tài khoản phải dài 3–32 ký tự, chỉ gồm chữ, số, dấu chấm, gạch dưới hoặc gạch ngang.' });
  if (!passwordOk(password)) return res.status(400).json({ error: 'Mật khẩu phải dài từ 8 đến 128 ký tự.' });
  try {
    const hash = await bcrypt.hash(password, 12);
    const result = db.prepare("INSERT INTO users (username, password_hash, role, locked) VALUES (?, ?, 'user', 0)").run(username, hash);
    const user = { id: Number(result.lastInsertRowid), username, role: 'user', locked: 0, created_at: new Date().toISOString() };
    return res.status(201).json({ token: issueToken(user), user: publicUser(user) });
  } catch (e) {
    if (String(e.code).includes('SQLITE_CONSTRAINT')) return res.status(409).json({ error: 'Tên tài khoản đã tồn tại.' });
    console.error(e); return res.status(500).json({ error: 'Lỗi máy chủ.' });
  }
});
app.post('/api/auth/login', async (req, res) => {
  const { username, password } = req.body || {};
  if (typeof username !== 'string' || typeof password !== 'string') return res.status(400).json({ error: 'Vui lòng nhập tên tài khoản và mật khẩu.' });
  const user = db.prepare('SELECT id, username, password_hash, role, locked, created_at FROM users WHERE username = ?').get(username);
  const valid = user ? await bcrypt.compare(password, user.password_hash) : false;
  if (!valid || user.locked) return res.status(401).json({ error: 'Tên tài khoản/mật khẩu không đúng hoặc tài khoản đã bị khóa.' });
  return res.json({ token: issueToken(user), user: publicUser(user) });
});
app.get('/api/auth/me', authenticate, (req, res) => res.json({ user: publicUser(req.authUser) }));

// Admin-only user management. The server checks the current DB role on every request.
app.get('/api/admin/users', authenticate, requireAdmin, (_req, res) => {
  const users = db.prepare('SELECT id, username, role, locked, created_at FROM users ORDER BY created_at DESC, id DESC').all();
  res.json({ users: users.map(publicUser) });
});
app.patch('/api/admin/users/:id/status', authenticate, requireAdmin, (req, res) => {
  const id = Number(req.params.id);
  const locked = req.body && req.body.locked;
  if (!Number.isSafeInteger(id) || id < 1 || typeof locked !== 'boolean') return res.status(400).json({ error: 'ID hoặc trạng thái khóa không hợp lệ.' });
  const target = db.prepare('SELECT id, username, role, locked, created_at FROM users WHERE id = ?').get(id);
  if (!target) return res.status(404).json({ error: 'Không tìm thấy tài khoản.' });
  if (target.id === req.authUser.id && locked) return res.status(400).json({ error: 'Không thể tự khóa tài khoản Admin đang đăng nhập.' });
  if (target.role === 'admin' && locked) return res.status(400).json({ error: 'Không thể khóa tài khoản Admin bằng màn hình này.' });
  db.prepare('UPDATE users SET locked = ? WHERE id = ?').run(locked ? 1 : 0, id);
  const updated = db.prepare('SELECT id, username, role, locked, created_at FROM users WHERE id = ?').get(id);
  res.json({ user: publicUser(updated) });
});

app.use((_req, res) => res.status(404).json({ error: 'Không tìm thấy endpoint.' }));
bootstrapAdmin().then(() => {
  app.listen(PORT, '0.0.0.0', () => console.log(`SensiScreen API listening on port ${PORT}`));
}).catch(err => {
  console.error(`Admin bootstrap failed: ${err.message}`);
  process.exit(1);
});
