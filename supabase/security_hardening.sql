-- Security hardening for the Store bucket. Run once in Supabase: SQL Editor -> New query -> paste -> Run.
-- Safe to run again. (Also folded into store.sql for fresh installs.)
--
-- Before: any anonymous caller could upload ANY file name / type (up to 25 MB each, unlimited count) into the
-- public "store" bucket. Now the bucket only accepts the kinds of file the app really uploads,
-- at the exact paths the app uses:
--   <owner>/<timestamp>/app.apk        (APK)
--   <owner>/<timestamp>/logo.png       (app logo)
--   <owner>/<timestamp>/shot1..4.jpg   (screenshots, small JPEGs)
--   profiles/<owner>/<timestamp>.png   (review profile photo)
-- <owner> is the signed-in account id (36 chars) or the old 32-char phone id.

update storage.buckets
   set allowed_mime_types = array['application/vnd.android.package-archive', 'image/png', 'image/jpeg'],
       file_size_limit = 26214400
 where id = 'store';

drop policy if exists "store anon upload" on storage.objects;
create policy "store anon upload" on storage.objects for insert to anon, authenticated
with check (
  bucket_id = 'store'
  and (
    name ~ '^[A-Za-z0-9-]{32,36}/[0-9]{10,16}/(app\.apk|logo\.png|shot[1-4]\.jpg)$'
    or name ~ '^profiles/[A-Za-z0-9-]{32,36}/[0-9]{10,16}\.png$'
  )
);
