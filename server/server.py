#!/usr/bin/env python3
"""Máy chủ xác thực mẫu cho Map Phím (chỉ dùng thư viện chuẩn của Python 3.9+).

Chạy:   python3 server.py
Biến:   PORT=3000  DB=users.db  TRIAL_DAYS=30  BIND_DEVICE=1 (mỗi tài khoản chỉ dùng trên 1 máy)
Key:     python3 server.py genkey <số_giờ> [số_lượng]   (24 = 1 ngày, 336 = 14 ngày)
          python3 server.py keys | revoke <key>
Quản trị: python3 server.py extend <tài_khoản> <số_ngày>
          python3 server.py reset-device <tài_khoản>
Khi chạy thật, hãy đặt sau HTTPS (Caddy, nginx...) vì mật khẩu được gửi trong body.
"""
import hashlib, json, os, re, secrets, sqlite3, sys, time
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer

DB = os.environ.get("DB", "users.db")
PORT = int(os.environ.get("PORT", "3000"))
TRIAL_DAYS = int(os.environ.get("TRIAL_DAYS", "30"))
BIND_DEVICE = os.environ.get("BIND_DEVICE") == "1"
FAILS = {}

def db():
    c = sqlite3.connect(DB)
    c.execute("create table if not exists users(username text primary key, salt text, hash text, expires integer, device text)")
    c.execute("create table if not exists keys(key text primary key, hours integer, device text, activated integer, expires integer)")
    c.execute("create table if not exists tokens(token text primary key, username text, expires integer)")
    return c

def pw_hash(pw, salt):
    return hashlib.pbkdf2_hmac("sha256", pw.encode(), bytes.fromhex(salt), 120000).hex()

def too_many(u):
    now = time.time()
    FAILS[u] = [t for t in FAILS.get(u, []) if now - t < 600]
    return len(FAILS[u]) >= 5

class H(BaseHTTPRequestHandler):
    def log_message(self, *a): pass

    def reply(self, code, obj):
        b = json.dumps(obj, ensure_ascii=False).encode()
        self.send_response(code)
        self.send_header("Content-Type", "application/json; charset=utf-8")
        self.send_header("Content-Length", str(len(b)))
        self.end_headers()
        self.wfile.write(b)

    def issue(self, c, u, exp):
        t = secrets.token_hex(32)
        c.execute("insert into tokens values(?,?,?)", (t, u, min(exp, int(time.time()) + 30 * 86400)))
        c.commit()
        return {"token": t, "expires_at": exp, "user": u}

    def do_POST(self):
        n = int(self.headers.get("Content-Length") or 0)
        try:
            d = json.loads(self.rfile.read(n) or b"{}")
        except ValueError:
            return self.reply(400, {"error": "JSON không hợp lệ."})
        p = self.path.split("?")[0].rstrip("/")
        c = db()
        try:
            if p == "/api/auth/register": self.register(c, d)
            elif p == "/api/auth/login": self.login(c, d)
            elif p == "/api/auth/verify": self.verify(c)
            elif p == "/api/license/activate": self.activate(c, d)
            else: self.reply(404, {"error": "Không tìm thấy."})
        finally:
            c.close()

    def register(self, c, d):
        u = str(d.get("username", "")).strip().lower()
        pw = str(d.get("password", ""))
        if not re.fullmatch(r"[a-z0-9_]{3,32}", u):
            return self.reply(400, {"error": "Tên tài khoản 3-32 ký tự: chữ, số, dấu gạch dưới."})
        if len(pw) < 6:
            return self.reply(400, {"error": "Mật khẩu cần ít nhất 6 ký tự."})
        salt = secrets.token_hex(16)
        exp = int(time.time()) + TRIAL_DAYS * 86400
        dev = (d.get("device_id") or None) if BIND_DEVICE else None
        try:
            c.execute("insert into users values(?,?,?,?,?)", (u, salt, pw_hash(pw, salt), exp, dev))
        except sqlite3.IntegrityError:
            return self.reply(409, {"error": "Tài khoản đã tồn tại."})
        self.reply(200, self.issue(c, u, exp))

    def login(self, c, d):
        u = str(d.get("username", "")).strip().lower()
        pw = str(d.get("password", ""))
        if too_many(u):
            return self.reply(429, {"error": "Thử sai quá nhiều lần, hãy đợi 10 phút."})
        r = c.execute("select salt,hash,expires,device from users where username=?", (u,)).fetchone()
        if not r or not secrets.compare_digest(pw_hash(pw, r[0]), r[1]):
            FAILS.setdefault(u, []).append(time.time())
            return self.reply(401, {"error": "Sai tên tài khoản hoặc mật khẩu."})
        if r[2] < time.time():
            return self.reply(403, {"error": "Tài khoản đã hết hạn."})
        dev = d.get("device_id") or ""
        if BIND_DEVICE:
            if r[3] and r[3] != dev:
                return self.reply(403, {"error": "Tài khoản đã gắn với thiết bị khác."})
            if not r[3]:
                c.execute("update users set device=? where username=?", (dev, u))
        self.reply(200, self.issue(c, u, r[2]))

    def activate(self, c, d):
        k = str(d.get("key", "")).strip().upper()
        dev = str(d.get("device_id") or "")
        if too_many("k:" + dev):
            return self.reply(429, {"error": "Thử sai quá nhiều lần, hãy đợi 10 phút."})
        r = c.execute("select hours,device,expires from keys where key=?", (k,)).fetchone()
        if not r:
            FAILS.setdefault("k:" + dev, []).append(time.time())
            return self.reply(404, {"error": "Key không hợp lệ."})
        hours, kdev, exp = r
        now = int(time.time())
        if kdev:
            if kdev != dev:
                return self.reply(403, {"error": "Key đã được sử dụng trên thiết bị khác."})
            if exp < now:
                return self.reply(403, {"error": "Key đã hết hạn."})
        else:
            exp = now + hours * 3600
            c.execute("update keys set device=?, activated=?, expires=? where key=?", (dev, now, exp, k))
        self.reply(200, self.issue(c, "license:" + k, exp))

    def verify(self, c):
        t = (self.headers.get("Authorization") or "").removeprefix("Bearer ").strip()
        now = time.time()
        r = c.execute("select expires,username from tokens where token=?", (t,)).fetchone()
        bad = {"error": "Phiên đã hết hạn, hãy đăng nhập lại."}
        if not r or r[0] < now:
            return self.reply(401, bad)
        if r[1].startswith("license:"):
            k = c.execute("select expires from keys where key=?", (r[1][8:],)).fetchone()
            exp = k[0] if k else 0
        else:
            u = c.execute("select expires from users where username=?", (r[1],)).fetchone()
            exp = u[0] if u else 0
        if exp < now:
            return self.reply(401, bad)
        self.reply(200, {"valid": True, "expires_at": exp})

if __name__ == "__main__":
    a = sys.argv[1:]
    if a[:1] == ["extend"] and len(a) == 3:
        c = db()
        now = int(time.time())
        c.execute("update users set expires=max(?,expires)+? where username=?", (now, int(a[2]) * 86400, a[1].lower()))
        c.commit(); print("Đã gia hạn", a[1])
    elif a[:1] == ["reset-device"] and len(a) == 2:
        c = db(); c.execute("update users set device=null where username=?", (a[1].lower(),)); c.commit(); print("Đã mở khóa thiết bị", a[1])
    elif a[:1] == ["genkey"] and len(a) >= 2:
        c = db()
        alpha = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
        for _ in range(int(a[2]) if len(a) > 2 else 1):
            k = "MP-" + "-".join("".join(secrets.choice(alpha) for _ in range(4)) for _ in range(3))
            c.execute("insert into keys(key,hours) values(?,?)", (k, int(a[1])))
            print(k)
        c.commit()
    elif a[:1] == ["keys"]:
        for r in db().execute("select key,hours,device,expires from keys"):
            print(r[0], f"{r[1]}h", "đã dùng" if r[2] else "chưa dùng", time.strftime("%d/%m/%Y %H:%M", time.localtime(r[3])) if r[3] else "-")
    elif a[:1] == ["revoke"] and len(a) == 2:
        c = db(); c.execute("delete from keys where key=?", (a[1].upper(),)); c.commit(); print("Đã thu hồi", a[1])
    else:
        print(f"Máy chủ đang chạy ở cổng {PORT}")
        ThreadingHTTPServer(("0.0.0.0", PORT), H).serve_forever()
