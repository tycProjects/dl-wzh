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

const ok = () => new Response('ok');

Deno.serve(async (req) => {
  if (req.method !== 'POST') return ok();
  if (!SECRET || req.headers.get('x-telegram-bot-api-secret-token') !== SECRET) {
    return new Response('forbidden', { status: 403 });
  }

  let u: any;
  try { u = await req.json(); } catch { return ok(); }

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

  await tg('setMessageReaction', {
    chat_id: m.chat.id,
    message_id: m.message_id,
    reaction: [{ type: 'emoji', emoji: '\u{1F44D}' }],
  });
  return ok();
});
