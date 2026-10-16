import os, sys, json, base64, re, requests, wasmtime, numpy as np
from collections import OrderedDict

WASM_PATH = os.path.join(os.path.dirname(__file__), 'sha3_wasm_bg.7b9ca65ddd.wasm')


# ============ محرك WASM لحل POW ============
class WasmEngine:
    def __init__(self):
        engine = wasmtime.Engine()
        with open(WASM_PATH, 'rb') as f:
            module = wasmtime.Module(engine, f.read())
        self.store = wasmtime.Store(engine)
        linker = wasmtime.Linker(engine)
        linker.define_wasi()
        self.instance = linker.instantiate(self.store, module)
        self.memory = self.instance.exports(self.store)["memory"]

    def _write(self, text):
        b = text.encode()
        ptr = self.instance.exports(self.store)["__wbindgen_export_0"](self.store, len(b), 1)
        mv = self.memory.data_ptr(self.store)
        for i, byte in enumerate(b):
            mv[ptr + i] = byte
        return ptr, len(b)

    def solve(self, cfg):
        retptr = self.instance.exports(self.store)["__wbindgen_add_to_stack_pointer"](self.store, -16)
        try:
            cp, cl = self._write(cfg['challenge'])
            pp, pl = self._write(f"{cfg['salt']}_{cfg['expire_at']}_")
            self.instance.exports(self.store)["wasm_solve"](
                self.store, retptr, cp, cl, pp, pl, float(cfg['difficulty']))
            mv = self.memory.data_ptr(self.store)
            if int.from_bytes(bytes(mv[retptr:retptr+4]), 'little', signed=True) == 0:
                return None
            return int(np.frombuffer(bytes(mv[retptr+8:retptr+16]), dtype=np.float64)[0])
        finally:
            self.instance.exports(self.store)["__wbindgen_add_to_stack_pointer"](self.store, 16)


_wasm = WasmEngine()


# ============ عميل DeepSeek ============
class DeepSeek:
    CHAT = "https://chat.deepseek.com/api/v0/chat/completion"
    CREATE = "https://chat.deepseek.com/api/v0/chat_session/create"
    LIST = "https://chat.deepseek.com/api/v0/chat_session/list"
    DELETE = "https://chat.deepseek.com/api/v0/chat_session/delete"
    POW = "https://chat.deepseek.com/api/v0/chat/create_pow_challenge"

    def __init__(self, token):
        self.token = token
        self.h = {
            "User-Agent": "DeepSeek/2.1.0 Android/36",
            "Accept": "application/json",
            "Content-Type": "application/json",
            "x-client-platform": "android",
            "x-client-version": "2.1.0",
            "x-client-locale": "ar",
            "x-client-bundle-id": "com.deepseek.chat",
            "authorization": f"Bearer {token}",
        }

    def get_pow(self, target="/api/v0/chat/completion"):
        """جلب تحدي PoW وحلّه محلياً."""
        r = requests.post(self.POW, headers=self.h, json={"target_path": target}, timeout=10)
        if r.status_code != 200:
            return None
        cfg = r.json()['data']['biz_data']['challenge']
        ans = _wasm.solve(cfg)
        if ans is None:
            return None
        payload = OrderedDict([
            ('algorithm', cfg['algorithm']),
            ('challenge', cfg['challenge']),
            ('salt', cfg['salt']),
            ('answer', ans),
            ('signature', cfg['signature']),
            ('target_path', cfg['target_path']),
        ])
        return base64.b64encode(json.dumps(payload, separators=(',', ':')).encode()).decode()

    def new_session(self):
        """إنشاء جلسة جديدة."""
        r = requests.post(self.CREATE, headers={**self.h, "content-length": "0"}, data=b'', timeout=10)
        return r.json().get("data", {}).get("biz_data", {}).get("chat_session", {}).get("id")

    def sessions(self):
        """عرض الجلسات."""
        r = requests.post(self.LIST, headers={**self.h, "content-length": "0"}, data=b'', timeout=10)
        return r.json().get("data", {}).get("biz_data", {}).get("chat_sessions", [])

    def delete(self, sid):
        """حذف جلسة."""
        r = requests.post(self.DELETE, headers=self.h,
                          json={"chat_session_id": sid}, timeout=10)
        return r.status_code == 200

    def chat(self, prompt, session_id=None, parent_id=None, on_chunk=None,
             think=False, search=False, temperature=0.7):
        """
        إرسال رسالة والحصول على الرد.
        on_chunk: callback(full_text, new_chunk) للـ streaming
        """
        if not session_id:
            session_id = self.new_session()
            if not session_id:
                return {"success": False, "error": "فشل إنشاء الجلسة"}

        pow_token = self.get_pow()
        if not pow_token:
            return {"success": False, "error": "فشل POW", "session_id": session_id}

        payload = {
            "chat_session_id": session_id,
            "prompt": prompt,
            "ref_file_ids": [],
            "thinking_enabled": think,
            "search_enabled": search,
            "temperature": temperature,
            "max_tokens": 2048,
            "top_p": 0.9,
            "stream": True,
        }
        if parent_id:
            payload["parent_message_id"] = parent_id

        full_text, new_pid = "", parent_id
        try:
            with requests.post(self.CHAT, headers={**self.h, "x-ds-pow-response": pow_token},
                               json=payload, stream=True, timeout=90) as resp:
                if resp.status_code != 200:
                    return {"success": False, "error": f"HTTP {resp.status_code}",
                            "session_id": session_id}

                buf = b""
                for raw in resp.iter_content(256):
                    buf += raw
                    while b"\n" in buf:
                        line, buf = buf.split(b"\n", 1)
                        line = line.decode("utf-8", "replace").strip()
                        if not line.startswith("data: "):
                            continue
                        try:
                            evt = json.loads(line[6:])
                        except:
                            continue

                        if "id" in evt:
                            new_pid = evt["id"]
                        if "error" in evt:
                            return {"success": False, "error": str(evt["error"]),
                                    "session_id": session_id}

                        v = evt.get("v", "")
                        if isinstance(v, str) and v and "thinking" not in evt.get("p", ""):
                            full_text += v
                            if on_chunk:
                                on_chunk(full_text, v)

            return {"success": True, "text": full_text,
                    "parent_id": new_pid, "session_id": session_id}

        except Exception as e:
            return {"success": False, "error": str(e), "session_id": session_id}


# ============ أدوات مساعدة ============
def extract_code(text):
    """استخراج أكواد markdown."""
    return [(m[0], m[1].strip()) for m in
            re.findall(r"```(\w*)\n?([\s\S]*?)```", text) if m[1].strip()]


def build_prompt(history):
    """تحويل السجل إلى prompt واحد."""
    return "\n".join(
        f"{'User' if m['role']=='user' else 'Assistant'}: {m['content']}"
        for m in history
    ) + "\n"


# ============ CLI ============
if __name__ == '__main__':
    token = sys.argv[1] if len(sys.argv) > 1 else os.environ.get("DEEPSEEK_TOKEN")
    if not token:
        print("Usage: python deepseek.py <AUTH_TOKEN>")
        sys.exit(1)

    ds = DeepSeek(token)
    sid = ds.new_session()
    print(f"✅ Session: {sid}\n")

    history, parent_id = [], None

    while True:
        try:
            q = input("أنت: ").strip()
        except (EOFError, KeyboardInterrupt):
            break
        if not q or q.lower() in ("exit", "quit", "خروج"):
            break

        history.append({"role": "user", "content": q})
        print("🤖 ", end="", flush=True)

        r = ds.chat(
            prompt=build_prompt(history),
            session_id=sid,
            parent_id=parent_id,
            on_chunk=lambda full, chunk: print(chunk, end="", flush=True),
        )
        print()

        if r["success"]:
            history.append({"role": "assistant", "content": r["text"]})
            parent_id = r["parent_id"]
        else:
            print(f"❌ {r.get('error')}")