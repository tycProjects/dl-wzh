'use strict';
const express = require('express');
const helmet = require('helmet');
const rateLimit = require('express-rate-limit');
const bcrypt = require('bcryptjs');
const jwt = require('jsonwebtoken');
const Database = require('better-sqlite3');
const crypto = require('crypto');

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
app.disable('x-powered-by');
app.use(helmet());
app.use(express.json({ limit: '10kb' }));
app.use('/api/auth', rateLimit({ windowMs: 15 * 60 * 1000, limit: 30, standardHeaders: true, legacyHeaders: false }));
const usernameOk = s => typeof s === 'string' && /^[a-zA-Z0-9_.-]{3,32}$/.test(s);
const passwordOk = s => typeof s === 'string' && s.length >= 8 && s.length <= 128;
function issueToken(user) {
  return jwt.sign({ sub: String(user.id), username: user.username }, JWT_SECRET, { expiresIn: '7d', issuer: 'sensiscreen-auth' });
}
app.get('/health', (_req, res) => res.json({ ok: true }));
app.post('/api/auth/register', async (req, res) => {
  const { username, password } = req.body || {};
  if (!usernameOk(username)) return res.status(400).json({ error: 'Tên tài khoản phải dài 3–32 ký tự, chỉ gồm chữ, số, dấu chấm, gạch dưới hoặc gạch ngang.' });
  if (!passwordOk(password)) return res.status(400).json({ error: 'Mật khẩu phải dài từ 8 đến 128 ký tự.' });
  try {
    const hash = await bcrypt.hash(password, 12);
    const result = db.prepare('INSERT INTO users (username, password_hash) VALUES (?, ?)').run(username, hash);
    const user = { id: result.lastInsertRowid, username };
    return res.status(201).json({ token: issueToken(user), user: { username } });
  } catch (e) {
    if (String(e.code).includes('SQLITE_CONSTRAINT')) return res.status(409).json({ error: 'Tên tài khoản đã tồn tại.' });
    console.error(e); return res.status(500).json({ error: 'Lỗi máy chủ.' });
  }
});
app.post('/api/auth/login', async (req, res) => {
  const { username, password } = req.body || {};
  if (typeof username !== 'string' || typeof password !== 'string') return res.status(400).json({ error: 'Vui lòng nhập tên tài khoản và mật khẩu.' });
  const user = db.prepare('SELECT id, username, password_hash FROM users WHERE username = ?').get(username);
  const valid = user ? await bcrypt.compare(password, user.password_hash) : false;
  if (!valid) return res.status(401).json({ error: 'Tên tài khoản hoặc mật khẩu không đúng.' });
  return res.json({ token: issueToken(user), user: { username: user.username } });
});
app.get('/api/auth/me', (req, res) => {
  const header = req.get('authorization') || '';
  const token = header.startsWith('Bearer ') ? header.slice(7) : '';
  try {
    const payload = jwt.verify(token, JWT_SECRET, { issuer: 'sensiscreen-auth' });
    return res.json({ user: { username: payload.username } });
  } catch (_) { return res.status(401).json({ error: 'Phiên đăng nhập không hợp lệ hoặc đã hết hạn.' }); }
});
app.use((_req, res) => res.status(404).json({ error: 'Không tìm thấy endpoint.' }));
app.listen(PORT, '0.0.0.0', () => console.log(`SensiScreen auth API listening on port ${PORT}`));
