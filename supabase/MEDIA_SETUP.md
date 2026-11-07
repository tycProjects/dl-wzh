# Photos and videos in the Admin chat

The app shrinks every photo (max 1280 px JPEG, usually 200-500 KB) and video (about 540p, max 60 s, under 12 MB)
on the phone, then sends it to the `chat-media` function, which forwards it to Telegram.
**Nothing is saved in Supabase Storage**, so your Supabase can't fill up with media. Telegram keeps the file.
Only a short row ("Photo" / "Video") is added to the `messages` table.

## One-time setup
1. Supabase -> Edge Functions -> Deploy a new function -> name it `chat-media` -> paste `functions/chat-media/index.ts` -> Deploy.
   (Leave "Verify JWT" ON. This is different from `telegram-hook`.)
2. No new secrets: it uses `TELEGRAM_BOT_TOKEN` and `TELEGRAM_CHAT_ID` that `telegram-hook` already has.
3. Rebuild the app (new packages: `image`, `video_compress`). Run `python3 tool/android_setup.py android` as before; it now also adds the permission to list videos in the gallery.

## Limits
- Photo: 10 MB sent (the app makes them far smaller). Video: 60 seconds, 12 MB after shrinking.
- 6 files per 10 minutes per phone.
- The admin answers by replying to the file in Telegram, as with text messages.
- After the app is closed, the chat history shows "Photo" / "Video" text for old files (the picture itself is only shown on the phone that sent it, in the open chat).
