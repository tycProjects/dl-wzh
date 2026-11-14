-- Store: let the owner publish a NEW VERSION of an app they already have in the Store.
-- Run once in Supabase: SQL Editor -> New query -> paste ALL of it -> Run. (store.sql must have been run once before; safe to re-run.)
-- This one file is enough: it also upgrades store_publish so new apps save their versionCode.
--
-- * Only the signed-in owner can update their own app.
-- * The new APK must keep the same package name as the app in the Store.
-- * The new versionCode must be higher than the one in the Store (Android refuses to update an installed
--   app otherwise). Apps published before this change have no versionCode yet, so their first update only
--   needs a higher versionName.
-- * A live (approved) app stays live and serves the new APK right away; people who installed it
--   then see "Update" in the Store. A rejected app goes back to 'pending' so it is checked again.

alter table public.store_apps add column if not exists updated_at timestamptz;
alter table public.store_apps add column if not exists shots text[] not null default '{}';
alter table public.store_apps add column if not exists version_code bigint not null default 0;

-- drops EVERY old version of store_publish (an older run can leave a second copy, which makes the call fail with Store error 300)
do $$ declare r record; begin
  for r in select p.oid::regprocedure as sig from pg_proc p where p.proname = 'store_publish' and p.pronamespace = 'public'::regnamespace loop
    execute 'drop function ' || r.sig;
  end loop;
end $$;
create or replace function public.store_publish(p_owner text, p_name text, p_about text, p_cat text,
  p_pkg text, p_version text, p_size bigint, p_apk text, p_logo text, p_vcode bigint default 0, p_shots text[] default '{}')
returns uuid language plpgsql security definer set search_path = public as $$
declare v_id uuid; v_owner text;
begin
  if auth.uid() is null then raise exception 'Sign in to publish an app'; end if;
  v_owner := auth.uid()::text;
  if length(trim(p_name)) < 1 or length(p_name) > 60 then raise exception 'bad name'; end if;
  if not (p_apk like v_owner || '/%' or p_apk like 'n/' || v_owner || '/%') then raise exception 'bad file'; end if;
  if coalesce(p_logo, '') = '' then raise exception 'Add an app icon. It is required.'; end if;
  if coalesce(array_length(p_shots, 1), 0) < 1 then raise exception 'Add at least one screenshot. It is required.'; end if;
  if coalesce(array_length(p_shots, 1), 0) > 4 then raise exception 'Up to 4 screenshots'; end if;
  if exists (select 1 from unnest(coalesce(p_shots, '{}')) s where not (s like v_owner || '/%' or s like 'n/' || v_owner || '/%')) then raise exception 'bad file'; end if;
  if (select count(*) from store_apps where owner = v_owner) >= 2 then
    raise exception 'You can publish 2 apps. Delete one to add another.';
  end if;
  insert into store_apps (owner, name, about, cat, pkg, version, version_code, size_bytes, apk_path, logo_path, shots)
  values (v_owner, trim(p_name), left(coalesce(p_about, ''), 1000), left(coalesce(p_cat, 'Other'), 20),
          left(coalesce(p_pkg, ''), 150), left(coalesce(p_version, ''), 40), greatest(coalesce(p_vcode, 0), 0),
          coalesce(p_size, 0), p_apk, coalesce(p_logo, ''), coalesce(p_shots, '{}'))
  returning id into v_id;
  return v_id;
end $$;

-- drops EVERY old version of store_update (an older run can leave a second copy, which makes the call fail with Store error 300)
do $$ declare r record; begin
  for r in select p.oid::regprocedure as sig from pg_proc p where p.proname = 'store_update' and p.pronamespace = 'public'::regnamespace loop
    execute 'drop function ' || r.sig;
  end loop;
end $$;
create or replace function public.store_update(p_owner text, p_id uuid, p_name text, p_about text, p_cat text,
  p_pkg text, p_version text, p_size bigint, p_apk text, p_logo text, p_vcode bigint default 0, p_shots text[] default '{}')
returns void language plpgsql security definer set search_path = public as $$
declare r public.store_apps; v_owner text;
begin
  if auth.uid() is null then raise exception 'Sign in to update your app'; end if;
  v_owner := auth.uid()::text;
  select * into r from store_apps where id = p_id and owner = v_owner;
  if not found then raise exception 'That app is not yours'; end if;
  if length(trim(p_name)) < 1 or length(p_name) > 60 then raise exception 'bad name'; end if;
  if not (p_apk like v_owner || '/%' or p_apk like 'n/' || v_owner || '/%') then raise exception 'bad file'; end if;
  if coalesce(p_logo, '') = '' and r.logo_path = '' then raise exception 'Add an app icon. It is required.'; end if;
  if coalesce(array_length(p_shots, 1), 0) < 1 and coalesce(array_length(r.shots, 1), 0) < 1 then raise exception 'Add at least one screenshot. It is required.'; end if;
  if coalesce(array_length(p_shots, 1), 0) > 4 then raise exception 'Up to 4 screenshots'; end if;
  if exists (select 1 from unnest(coalesce(p_shots, '{}')) s where not (s like v_owner || '/%' or s like 'n/' || v_owner || '/%')) then raise exception 'bad file'; end if;
  if r.pkg <> '' and coalesce(p_pkg, '') <> '' and p_pkg <> r.pkg then
    raise exception 'This APK is a different app (%). An update must keep the package name %.', p_pkg, r.pkg;
  end if;
  if coalesce(p_vcode, 0) > 0 and r.version_code > 0 and p_vcode <= r.version_code then
    raise exception 'The new versionCode (%) must be higher than the current one (%).', p_vcode, r.version_code;
  end if;
  update store_apps set
    name = trim(p_name),
    about = left(coalesce(p_about, ''), 1000),
    cat = left(coalesce(p_cat, 'Other'), 20),
    pkg = case when coalesce(p_pkg, '') <> '' then left(p_pkg, 150) else pkg end,
    version = left(coalesce(p_version, ''), 40),
    version_code = case when coalesce(p_vcode, 0) > 0 then p_vcode else version_code end,
    size_bytes = coalesce(p_size, 0),
    apk_path = p_apk,
    logo_path = case when coalesce(p_logo, '') <> '' then p_logo else logo_path end,
    shots = case when coalesce(array_length(p_shots, 1), 0) > 0 then p_shots else shots end,
    status = case when status = 'rejected' then 'pending' else status end,
    updated_at = now()
  where id = p_id;
end $$;

revoke all on function public.store_update(text, uuid, text, text, text, text, text, bigint, text, text, bigint, text[]) from public;
grant execute on function public.store_update(text, uuid, text, text, text, text, text, bigint, text, text, bigint, text[]) to anon, authenticated;

revoke all on function public.store_publish(text, text, text, text, text, text, bigint, text, text, bigint, text[]) from public;
grant execute on function public.store_publish(text, text, text, text, text, text, bigint, text, text, bigint, text[]) to anon, authenticated;
