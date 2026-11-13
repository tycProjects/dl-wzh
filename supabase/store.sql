-- Store tab. Run once in Supabase: SQL Editor -> New query -> paste -> Run.
-- Approve an app: Table Editor -> store_apps -> change status from 'pending' to 'approved'.

insert into storage.buckets (id, name, public, file_size_limit, allowed_mime_types)
values ('store', 'store', true, 26214400, array['application/vnd.android.package-archive', 'image/png', 'image/jpeg'])
on conflict (id) do update set public = true, file_size_limit = 26214400,
  allowed_mime_types = array['application/vnd.android.package-archive', 'image/png', 'image/jpeg'];

-- Only the exact paths the app uploads to (see security_hardening.sql for the details).
drop policy if exists "store anon upload" on storage.objects;
create policy "store anon upload" on storage.objects for insert to anon, authenticated
with check (
  bucket_id = 'store'
  and (
    name ~ '^[A-Za-z0-9-]{32,36}/[0-9]{10,16}/(app\.apk|logo\.png|shot[1-4]\.jpg)$'
    or name ~ '^profiles/[A-Za-z0-9-]{32,36}/[0-9]{10,16}\.png$'
  )
);

create table if not exists public.store_apps (
  id         uuid primary key default gen_random_uuid(),
  owner      text not null,
  name       text not null,
  about      text not null default '',
  cat        text not null default 'Other',
  pkg        text not null default '',
  version    text not null default '',
  size_bytes bigint not null default 0,
  apk_path   text not null,
  logo_path  text not null default '',
  status     text not null default 'pending' check (status in ('pending', 'approved', 'rejected')),
  downloads  int not null default 0,
  created_at timestamptz not null default now()
);
alter table public.store_apps add column if not exists shots text[] not null default '{}';  -- up to 4 screenshot paths (small JPEGs)
alter table public.store_apps add column if not exists version_code bigint not null default 0;  -- Android versionCode of the APK
create index if not exists store_apps_status_idx on public.store_apps (status, created_at desc);
alter table public.store_apps enable row level security;   -- no policies: the app only uses the functions below

create or replace function public.store_list(p_q text default '')
returns setof public.store_apps language sql security definer set search_path = public as $$
  select * from public.store_apps
  where status = 'approved' and (coalesce(p_q, '') = '' or name ilike '%' || p_q || '%')
  order by downloads desc, created_at desc limit 100;
$$;

-- Who is calling: the signed-in Google account if there is one, otherwise the old phone id.
create or replace function public.store_who(p_owner text)
returns text language sql stable set search_path = public as $$
  select coalesce(auth.uid()::text, case when length(p_owner) = 32 then p_owner end);
$$;

create or replace function public.store_mine(p_owner text)
returns setof public.store_apps language sql security definer set search_path = public as $$
  select * from public.store_apps where public.store_who(p_owner) is not null and owner = public.store_who(p_owner) order by created_at desc;
$$;

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

create or replace function public.store_delete(p_owner text, p_id uuid)
returns void language plpgsql security definer set search_path = public as $$
declare r public.store_apps; v_who text;
begin
  v_who := public.store_who(p_owner);
  if v_who is null then return; end if;
  delete from store_apps where id = p_id and owner = v_who returning * into r;
  -- Supabase blocks SQL deletes on storage.objects ("Direct deletion from storage tables is not allowed"),
  -- so only the row is removed here. The old APK/logo files can be cleared from Dashboard -> Storage -> store.
end $$;

-- After the first Google sign-in: moves the apps (and reviews) made before login from this phone's id to the account.
create or replace function public.store_claim(p_owner text)
returns void language plpgsql security definer set search_path = public as $$
begin
  if auth.uid() is null or length(p_owner) <> 32 then return; end if;
  update store_apps set owner = auth.uid()::text where owner = p_owner;
  if to_regclass('public.store_reviews') is not null then
    update store_reviews set owner = auth.uid()::text
      where owner = p_owner and app_id not in (select app_id from store_reviews where owner = auth.uid()::text);
  end if;
end $$;

create or replace function public.store_hit(p_id uuid)
returns void language sql security definer set search_path = public as $$
  update public.store_apps set downloads = downloads + 1 where id = p_id and status = 'approved';
$$;

revoke all on function public.store_list(text), public.store_mine(text),
  public.store_publish(text, text, text, text, text, text, bigint, text, text, bigint, text[]),
  public.store_delete(text, uuid), public.store_claim(text), public.store_hit(uuid) from public;
grant execute on function public.store_list(text), public.store_mine(text),
  public.store_publish(text, text, text, text, text, text, bigint, text, text, bigint, text[]),
  public.store_delete(text, uuid), public.store_claim(text), public.store_hit(uuid) to anon, authenticated;
