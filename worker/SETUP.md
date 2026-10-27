# Host-a-site Worker (works without the Render server)

1. Cloudflare dashboard > Workers & Pages > Create > Worker. Name it e.g. `wzh-host`, Deploy,
   then Edit code, replace everything with `host-worker.js`, Deploy.
2. Worker > Settings > Variables and Secrets, add:
   - `CF_ACCOUNT_ID`  (text)    same as on Render
   - `CF_PAGES_TOKEN` (secret)  same as on Render
   - `HOST_SECRET`    (secret)  SAME value as on Render, or existing hosted sites stop being "yours"
     (if you never set HOST_SECRET on Render, set it here to the CF_PAGES_TOKEN value)
3. Copy the Worker address (`https://wzh-host.<you>.workers.dev`) into `lib/config.dart` as `kHostWorker`.
4. Build the app. Open the Host tab: sites you already have should be listed.

Optional: create a KV namespace, bind it to the Worker as `HOST_KV` to get the per-IP daily limit.
