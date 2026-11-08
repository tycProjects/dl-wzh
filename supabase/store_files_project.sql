-- Run this ONCE in the SECOND Supabase project (SQL Editor). It only needs the Store's file bucket.
-- No tables, no auth setup: the app uploads here with this project's anon key, limited to the exact
-- folder names below (same rules as the main project).
--
-- Setup
--   1. supabase.com -> create a NEW ORGANIZATION (free) -> new project (any name, e.g. "wzh2-files").
--      Free egress is counted per organization, so it must be a separate organization.
--   2. Run this file in its SQL Editor.
--   3. Project Settings -> API: copy the Project URL and the "anon" (legacy, starts with eyJ) key into
--      kStoreFilesUrl and kStoreFilesAnonKey in lib/chat_cloud.dart, then rebuild the app.
--   New uploads go to the new project; apps already in the Store keep working from the main project.
--   To move an OLD app's download off the main project, re-upload it with Update in the Store tab.

insert into storage.buckets (id, name, public, file_size_limit, allowed_mime_types)
values ('store', 'store', true, 26214400, array['application/vnd.android.package-archive', 'image/png', 'image/jpeg'])
on conflict (id) do update set public = true, file_size_limit = 26214400,
  allowed_mime_types = array['application/vnd.android.package-archive', 'image/png', 'image/jpeg'];

drop policy if exists "store anon upload" on storage.objects;
create policy "store anon upload" on storage.objects for insert to anon, authenticated
with check (
  bucket_id = 'store'
  and (
    name ~ '^[A-Za-z0-9-]{32,36}/[0-9]{10,16}/(app\.apk|logo\.png|shot[1-4]\.jpg)$'
    or name ~ '^profiles/[A-Za-z0-9-]{32,36}/[0-9]{10,16}\.png$'
  )
);
