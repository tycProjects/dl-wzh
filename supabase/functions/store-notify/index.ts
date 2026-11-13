// Store -> Telegram. Deploy with "Verify JWT" left ON (the app sends its anon key).
// Uses the same secrets as telegram-hook: TELEGRAM_BOT_TOKEN, TELEGRAM_CHAT_ID.
import { createClient } from 'npm:@supabase/supabase-js@2';

const SUPA = Deno.env.get('SUPABASE_URL')!;
const sb = createClient(SUPA, Deno.env.get('SUPABASE_SERVICE_ROLE_KEY')!);
const TOKEN = Deno.env.get('TELEGRAM_BOT_TOKEN') ?? '';
const ADMIN = String(Deno.env.get('TELEGRAM_CHAT_ID') ?? '');
const mb = (n: number) => (n >= 1048576 ? `${(n / 1048576).toFixed(1)} MB` : `${Math.ceil(n / 1024)} KB`);

Deno.serve(async (req) => {
  let id = '';
  try { id = String((await req.json()).id ?? ''); } catch { /* ignore */ }
  if (!/^[0-9a-f-]{36}$/.test(id)) return new Response('bad id', { status: 400 });

  // Claim it: only the first call for an app sends a message.
  const { data: a } = await sb.from('store_apps').update({ notified: true })
    .eq('id', id).eq('status', 'pending').eq('notified', false).select('*').maybeSingle();
  if (!a || !TOKEN || !ADMIN) return new Response('ok');

  const url = `${SUPA}/storage/v1/object/public/store/${a.apk_path}`;
  const text = [
    '\u{1F4E6} New app to review',
    '',
    `${a.name} \u00B7 ${a.cat}`,
    `${mb(a.size_bytes)}${a.version ? ` \u00B7 v${a.version}` : ''}${a.pkg ? ` \u00B7 ${a.pkg}` : ''}`,
    a.about ? `\n${String(a.about).slice(0, 500)}` : '',
  ].join('\n');

  await fetch(`https://api.telegram.org/bot${TOKEN}/sendMessage`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({
      chat_id: ADMIN,
      text,
      reply_markup: {
        inline_keyboard: [
          [{ text: '\u2705 Approve', callback_data: `sa:${id}` }, { text: '\u274C Reject', callback_data: `sr:${id}` }],
          [{ text: '\u2B07 Download to test', url }],
        ],
      },
    }),
  });
  return new Response('ok');
});
