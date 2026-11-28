# Store tab: setup

1. Supabase -> SQL Editor -> New query -> paste `store.sql` -> Run (once). It creates the public `store` bucket,
   the `store_apps` table and the functions the app calls.
2. Build and install the app. The Store tab uses the same Supabase URL and anon key as the admin chat
   (`lib/chat_cloud.dart`), so nothing else to paste.
3. Approve apps: Supabase -> Table Editor -> `store_apps` -> change `status` from `pending` to `approved`
   (or `rejected`). Approved apps show in the Store for everyone.

4. Ratings & reviews: SQL Editor -> New query -> paste `store_reviews.sql` -> Run (once, after `store.sql`).
   Everyone can rate an approved app once (they can edit or delete it); nobody can rate their own app.

5. Updating an app: SQL Editor -> New query -> paste `store_update.sql` -> Run (once, after `store.sql`; safe to re-run).
   The owner opens their app in the Store and taps **Update app**, picks the new APK (same package name, higher
   versionCode) and it goes live right away. Phones that have the app see **Update** instead of Open.
   Order if you re-run everything: `store.sql`, then `store_update.sql`, then `store_reviews.sql`.

6. Screenshots (Play-style pictures on the app page): SQL Editor -> New query -> paste `store_shots.sql` -> Run (once, after `store.sql`; safe to re-run).
   Owners add up to 4 pictures when they publish or update an app. The app shrinks each one to a small JPEG
   (longest side 960 px, about 60-120 KB) before uploading, the Store only loads the ones on screen, and nothing is
   saved on the phone (pictures are decoded at the size shown and kept in memory only). Apps without screenshots work as before.
   Updating an app with new screenshots replaces the old ones; the old files can be cleared by hand in Storage -> store.

Limits: 25 MB per APK and 2 apps in total per phone (delete one to add another).

Fix for "Couldn't delete / Direct deletion from storage tables is not allowed": paste the updated `store.sql`
into the SQL Editor and Run it again (safe to re-run). Deleting an app now removes its listing; the leftover
files can be removed by hand in Storage -> store.

## Google sign-in (needed to publish)

Only publishing asks for a login; browsing, installing and reviewing work signed out.

1. Google Cloud Console -> APIs and Services -> Credentials -> Create OAuth client ID:
   - one of type **Web application** (copy its Client ID and Client secret),
   - one of type **Android** (package name of this app + the SHA-1 of your signing key).
2. Supabase -> Authentication -> Sign In / Providers -> **Google**: turn it on, paste the Web client ID and
   secret, and turn on **Skip nonce checks** (needed for the native Google picker). Save.
3. Paste the Web client ID into `kGoogleWebClientId` in `lib/config.dart`, then build.
4. Supabase -> SQL Editor: run `store.sql`, then `store_reviews.sql` again (both are safe to re-run).

The first time someone signs in, the apps they published before login on that phone are moved to their account.
