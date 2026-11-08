# Approve Store apps from your Telegram bot (one-time setup)

1. SQL Editor: run `store_bot.sql`.
2. Edge Functions -> `telegram-hook` -> edit: add the callback block from this zip's `functions/telegram-hook/index.ts`
   (the part starting at "Approve / Reject buttons", just above `const m = u?.message;`) -> Deploy.
3. Edge Functions -> Deploy a new function -> Via Editor -> name it exactly `store-notify`, paste
   `functions/store-notify/index.ts`, Deploy. Leave "Verify JWT" ON.
4. Open this once in a browser so Telegram also sends button taps (replace the three values):
   https://api.telegram.org/bot<BOT_TOKEN>/setWebhook?url=https://<PROJECT_ID>.supabase.co/functions/v1/telegram-hook&secret_token=<WEBHOOK_SECRET>&allowed_updates=["message","callback_query"]
5. Build the app. When someone taps Publish, your bot sends the app with Approve / Reject / Download buttons.
