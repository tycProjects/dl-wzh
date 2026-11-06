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

## Auto-delete unused sites (7 days)

1. Worker > Settings > Triggers > Cron Triggers > Add: `0 3 * * *` (every day 03:00 UTC).
2. Optional variables: `HOST_EXPIRE_DAYS` (default 7, `0` turns cleanup off), `HOST_KEEP` (comma list of project names to never delete).
3. A site counts as "unused" when its last upload (deploy) is older than that. Visits are not tracked, so a site that
   is only viewed and never re-uploaded is removed after the same number of days.
