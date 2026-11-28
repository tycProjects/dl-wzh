// Telegram -> Supabase. Deploy with "Verify JWT" turned OFF (Telegram can't send one).
// Secrets to add (Edge Functions -> Secrets): TELEGRAM_BOT_TOKEN, TELEGRAM_CHAT_ID, TELEGRAM_WEBHOOK_SECRET
// SUPABASE_URL and SUPABASE_SERVICE_ROLE_KEY are provided automatically.
import { createClient } from 'npm:@supabase/supabase-js@2';

const sb = createClient(Deno.env.get('SUPABASE_URL')!, Deno.env.get('SUPABASE_SERVICE_ROLE_KEY')!);
const TOKEN = Deno.env.get('TELEGRAM_BOT_TOKEN') ?? '';
const ADMIN = String(Deno.env.get('TELEGRAM_CHAT_ID') ?? '');
const SECRET = Deno.env.get('TELEGRAM_WEBHOOK_SECRET') ?? '';

async function tg(method: string, body: Record<string, unknown>) {
  try {
    await fetch(`https://api.telegram.org/bot${TOKEN}/${method}`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(body),
    });
  } catch (_) { /* courtesy calls only */ }
}

// ---- Firebase push (optional): needs the secret FIREBASE_SERVICE_ACCOUNT (the whole JSON file) ----
const SA: any = (() => { try { return JSON.parse(Deno.env.get('FIREBASE_SERVICE_ACCOUNT') ?? ''); } catch { return null; } })();

const b64u = (b: ArrayBuffer | string) => {
  const bytes = typeof b === 'string' ? new TextEncoder().encode(b) : new Uint8Array(b);
  let s = '';
  for (const c of bytes) s += String.fromCharCode(c);
  return btoa(s).replace(/\+/g, '-').replace(/\//g, '_').replace(/=+$/, '');
};

async function fcmAccessToken(): Promise<string | null> {
  if (!SA?.client_email || !SA?.private_key) return null;
  const now = Math.floor(Date.now() / 1000);
  const head = b64u(JSON.stringify({ alg: 'RS256', typ: 'JWT' }));
  const claim = b64u(JSON.stringify({
    iss: SA.client_email,
    scope: 'https://www.googleapis.com/auth/firebase.messaging',
    aud: 'https://oauth2.googleapis.com/token',
    iat: now,
    exp: now + 3600,
  }));
  const pem = String(SA.private_key).replace(/-----[^-]+-----/g, '').replace(/\s+/g, '');
  const der = Uint8Array.from(atob(pem), (c) => c.charCodeAt(0));
  const key = await crypto.subtle.importKey('pkcs8', der, { name: 'RSASSA-PKCS1-v1_5', hash: 'SHA-256' }, false, ['sign']);
  const sig = await crypto.subtle.sign('RSASSA-PKCS1-v1_5', key, new TextEncoder().encode(`${head}.${claim}`));
  const r = await fetch('https://oauth2.googleapis.com/token', {
    method: 'POST',
    headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
    body: new URLSearchParams({ grant_type: 'urn:ietf:params:oauth:grant-type:jwt-bearer', assertion: `${head}.${claim}.${b64u(sig)}` }),
  });
  const j = await r.json();
  return j.access_token ?? null;
}

// Pushes the admin's answer to the phone. Never throws: a push problem must not lose the reply.
async function pushToPhone(chatKey: string, body: string) {
  try {
    if (!SA) return;
    const { data: dev } = await sb.from('devices').select('token').eq('chat_key', chatKey).maybeSingle();
    if (!dev?.token) return;
    const at = await fcmAccessToken();
    if (!at) { console.warn('fcm: could not get access token'); return; }
    const r = await fetch(`https://fcm.googleapis.com/v1/projects/${SA.project_id}/messages:send`, {
      method: 'POST',
      headers: { Authorization: `Bearer ${at}`, 'Content-Type': 'application/json' },
      body: JSON.stringify({
        message: {
          token: dev.token,
          notification: { title: 'Admin', body: body.slice(0, 140) },
          android: { priority: 'HIGH', notification: { channel_id: 'admin_chat' } },
        },
      }),
    });
    if (!r.ok) {
      const t = await r.text();
      console.warn('fcm failed', r.status, t.slice(0, 300));
      if (r.status === 404 || t.includes('UNREGISTERED')) await sb.from('devices').delete().eq('chat_key', chatKey);
    }
  } catch (e) {
    console.warn('push failed', String(e));
  }
}

const ok = () => new Response('ok');

Deno.serve(async (req) => {
  if (req.method !== 'POST') return ok();
  if (!SECRET || req.headers.get('x-telegram-bot-api-secret-token') !== SECRET) {
    return new Response('forbidden', { status: 403 });
  }

  let u: any;
  try { u = await req.json(); } catch { return ok(); }

  // Approve / Reject buttons under a new Store app (sent by the store-notify function).
  const cq = u?.callback_query;
  if (cq) {
    const mm = String(cq.data ?? '').match(/^s([ar]):([0-9a-f-]{36})$/);
    if (!ADMIN || String(cq.message?.chat?.id) !== ADMIN || !mm) {
      await tg('answerCallbackQuery', { callback_query_id: cq.id });
      return ok();
    }
    const status = mm[1] === 'a' ? 'approved' : 'rejected';
    const { data: app, error } = await sb.from('store_apps').update({ status }).eq('id', mm[2]).select('id').maybeSingle();
    await tg('answerCallbackQuery', {
      callback_query_id: cq.id,
      text: error || !app ? 'Couldn\u2019t update that app' : status === 'approved' ? 'Approved' : 'Rejected',
    });
    if (app) {
      await tg('editMessageReplyMarkup', {
        chat_id: ADMIN,
        message_id: cq.message.message_id,
        reply_markup: {
          inline_keyboard: [[
            status === 'approved'
              ? { text: '\u2705 Approved \u2013 tap to reject', callback_data: `sr:${app.id}` }
              : { text: '\u274C Rejected \u2013 tap to approve', callback_data: `sa:${app.id}` },
          ]],
        },
      });
    }
    return ok();
  }

  const m = u?.message;
  if (!m?.reply_to_message) return ok();
  if (!ADMIN || String(m.chat?.id) !== ADMIN) return ok(); // only the admin chat counts

  const src = String(m.reply_to_message.text ?? m.reply_to_message.caption ?? '');
  const refs = [...src.matchAll(/ref:([a-f0-9]{10})\b/g)];
  const text = String(m.text ?? m.caption ?? '').trim().slice(0, 2000);
  if (!refs.length || !text) return ok();
  const ref = refs[refs.length - 1][1]; // the tag the server appended is always last

  const { data: row } = await sb.from('messages').select('chat_key').eq('ref', ref).limit(1).maybeSingle();
  if (!row) {
    await tg('sendMessage', {
      chat_id: ADMIN,
      reply_to_message_id: m.message_id,
      text: 'Couldn\u2019t find that chat, so your answer wasn\u2019t delivered.',
    });
    return ok();
  }

  const { data: ins, error } = await sb
    .from('messages')
    .insert({ chat_key: row.chat_key, ref, role: 'admin', body: text })
    .select('id')
    .single();
  if (error || !ins) {
    console.error('insert failed', error?.message);
    await tg('sendMessage', { chat_id: ADMIN, reply_to_message_id: m.message_id, text: 'Couldn\u2019t save your answer. Try again.' });
    return ok();
  }

  // Wake the open chat instantly (the app also re-checks every 30 s, so a miss here is harmless).
  try {
    const ch = sb.channel(`chat:${row.chat_key}`);
    await ch.send({ type: 'broadcast', event: 'reply', payload: { id: ins.id } });
    await sb.removeChannel(ch);
  } catch (e) { console.warn('broadcast failed', String(e)); }

  await pushToPhone(row.chat_key, text);

  await tg('setMessageReaction', {
    chat_id: m.chat.id,
    message_id: m.message_id,
    reaction: [{ type: 'emoji', emoji: '\u{1F44D}' }],
  });
  return ok();
});
