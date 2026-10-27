# Admin chat on Supabase: setup

## 1. Supabase (browser)
1. supabase.com -> New project (free plan is fine). Wait until it is ready.
2. SQL Editor -> New query -> paste `schema.sql` -> Run.
3. Project Settings -> API. Copy: Project URL, the `anon` key (starts with eyJ), and the `service_role` key.
   Keep the service_role key private: it goes only into Supabase and Render, never into the app.
4. Edge Functions -> Deploy a new function -> Via Editor. Name it exactly `telegram-hook`,
   paste `functions/telegram-hook/index.ts`, Deploy.
   Then open the function's settings and turn OFF "Verify JWT" (Telegram can't send one).
5. Edge Functions -> Secrets. Add:
   - TELEGRAM_BOT_TOKEN  (same as on Render)
   - TELEGRAM_CHAT_ID    (same as on Render)
   - TELEGRAM_WEBHOOK_SECRET  (make up a long random string, letters and digits only)

## 2. Point Telegram at Supabase (open this once in a browser)
https://api.telegram.org/bot<BOT_TOKEN>/setWebhook?url=https://<PROJECT_ID>.supabase.co/functions/v1/telegram-hook&secret_token=<WEBHOOK_SECRET>&allowed_updates=["message"]
It should answer {"ok":true,"result":true,...}.

## 3. Render (zip-to-apk-cajn)
- Upload the new `contactReplies.js`.
- Environment: add SUPABASE_URL (the Project URL) and SUPABASE_SERVICE_KEY (the service_role key).
- Remove TELEGRAM_REPLY_WEBHOOK, or set it to off, so the server stops re-registering the old webhook.
- Redeploy.

## 4. App
- Copy `lib/chat_cloud.dart` in, replace `lib/screens/message_page.dart` and `pubspec.yaml`.
- Open `lib/chat_cloud.dart` and paste your Project URL and anon key at the top.
- Build and install over the current app (don't uninstall first).

## Test
Send "test" from the Admin sheet. In Telegram swipe-reply to it. The app shows your answer within a second
or two, and reopening the sheet later brings the whole conversation back.
