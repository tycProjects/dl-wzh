// App -> Telegram: photos and videos from the Admin chat.
// The file is passed straight to Telegram and is NOT stored in Supabase Storage, so it never fills your project.
// Only a one-line row ("Photo" / "Video") is saved in public.messages so the chat history shows it.
// Deploy normally (leave "Verify JWT" ON: the app sends the anon key).
// Uses the same secrets as telegram-hook: TELEGRAM_BOT_TOKEN, TELEGRAM_CHAT_ID.
import { createClient } from 'npm:@supabase/supabase-js@2';

const sb = createClient(Deno.env.get('SUPABASE_URL')!, Deno.env.get('SUPABASE_SERVICE_ROLE_KEY')!);
const TOKEN = Deno.env.get('TELEGRAM_BOT_TOKEN') ?? '';
const ADMIN = String(Deno.env.get('TELEGRAM_CHAT_ID') ?? '');

const MB = 1024 * 1024;
const MAX: Record<string, number> = { photo: 10 * MB, video: 14 * MB }; // the app already shrinks to well below this
const LABEL: Record<string, string> = { photo: '\u{1F4F7} Photo', video: '\u{1F3AC} Video' };
const PER_10_MIN = 6; // files one chat may send in 10 minutes
const GLOBAL_PER_10_MIN = 60; // files ALL chats together may send in 10 minutes (stops key-rotation floods)

const json = (status: number, body: Record<string, unknown>) =>
  new Response(JSON.stringify(body), { status, headers: { 'Content-Type': 'application/json' } });

const newRef = () => {
  const b = crypto.getRandomValues(new Uint8Array(5));
  return [...b].map((x) => x.toString(16).padStart(2, '0')).join('');
};

Deno.serve(async (req) => {
  if (req.method !== 'POST') return json(405, { error: 'method' });
  if (!TOKEN || !ADMIN) return json(503, { error: 'not_configured' });

  let form: FormData;
  try { form = await req.formData(); } catch { return json(400, { error: 'bad_form' }); }

  const key = String(form.get('key') ?? '');
  const kind = String(form.get('kind') ?? '');
  const file = form.get('file');
  if (!/^[a-f0-9]{32}$/.test(key) || !(kind in MAX) || !(file instanceof File)) return json(400, { error: 'bad_request' });
  if (file.size < 1 || file.size > MAX[kind]) return json(413, { error: 'too_big' });

  // Simple flood guard.
  const since = new Date(Date.now() - 10 * 60 * 1000).toISOString();
  const { count } = await sb
    .from('messages')
    .select('id', { count: 'exact', head: true })
    .eq('chat_key', key)
    .eq('role', 'user')
    .in('body', Object.values(LABEL))
    .gte('created_at', since);
  if ((count ?? 0) >= PER_10_MIN) return json(429, { error: 'rate' });

  // Global guard: anyone with the public key can invent new chat keys, so also cap ALL chats together.
  const { count: all } = await sb
    .from('messages')
    .select('id', { count: 'exact', head: true })
    .eq('role', 'user')
    .in('body', Object.values(LABEL))
    .gte('created_at', since);
  if ((all ?? 0) >= GLOBAL_PER_10_MIN) return json(429, { error: 'busy' });

  const ref = newRef();
  const fd = new FormData();
  fd.append('chat_id', ADMIN);
  fd.append('caption', `${LABEL[kind]} from the app\n\nref:${ref}`);
  if (kind === 'video') fd.append('supports_streaming', 'true');
  fd.append(kind, file, kind === 'video' ? 'video.mp4' : 'photo.jpg');

  let ok = false;
  try {
    const r = await fetch(`https://api.telegram.org/bot${TOKEN}/${kind === 'video' ? 'sendVideo' : 'sendPhoto'}`, { method: 'POST', body: fd });
    const j = await r.json().catch(() => ({}));
    ok = r.ok && j?.ok === true;
    if (!ok) console.warn('telegram failed', r.status, JSON.stringify(j).slice(0, 300));
  } catch (e) {
    console.warn('telegram error', String(e));
  }
  if (!ok) return json(502, { error: 'telegram' });

  // The admin's reply (Telegram "reply" to this file) finds the chat through this ref.
  const { error } = await sb.from('messages').insert({ chat_key: key, ref, role: 'user', body: LABEL[kind] });
  if (error) console.error('insert failed', error.message);
  return json(200, { ok: true });
});
