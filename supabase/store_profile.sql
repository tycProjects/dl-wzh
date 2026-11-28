-- Profile photos for Store reviews. Run once in Supabase: SQL Editor -> New query -> paste -> Run.
-- (Run store.sql and store_reviews.sql first. Safe to run again.)

create table if not exists public.store_profiles (
  owner       text primary key,
  avatar_path text not null default '',
  updated_at  timestamptz not null default now()
);
alter table public.store_profiles enable row level security;   -- no policies: the app only uses the functions below

-- the caller's own photo ('' = none)
create or replace function public.store_profile_get(p_owner text)
returns text language sql security definer set search_path = public as $$
  select coalesce((select avatar_path from store_profiles where owner = public.store_who(p_owner)), '');
$$;

-- saves (or clears, with '') the caller's profile photo; the file itself is uploaded to the "store" bucket first
create or replace function public.store_profile_save(p_owner text, p_avatar text)
returns void language plpgsql security definer set search_path = public as $$
declare v_who text;
begin
  v_who := public.store_who(p_owner);
  if v_who is null then raise exception 'bad owner'; end if;
  if coalesce(p_avatar, '') <> '' and p_avatar not like 'profiles/' || v_who || '/%' then
    raise exception 'bad photo path';
  end if;
  insert into store_profiles (owner, avatar_path, updated_at) values (v_who, coalesce(p_avatar, ''), now())
  on conflict (owner) do update set avatar_path = excluded.avatar_path, updated_at = now();
end $$;

-- same list as before, now with each reviewer's photo
drop function if exists public.store_reviews_list(uuid, text);
create or replace function public.store_reviews_list(p_app uuid, p_owner text default '')
returns table (id uuid, name text, stars int, body text, created_at timestamptz, mine boolean, reply text, reply_at timestamptz, avatar text)
language sql security definer set search_path = public as $$
  select r.id, r.name, r.stars::int, r.body, r.created_at, (public.store_who(p_owner) is not null and r.owner = public.store_who(p_owner)), r.reply, r.reply_at, coalesce(p.avatar_path, '')
  from public.store_reviews r
  left join public.store_profiles p on p.owner = r.owner
  where r.app_id = p_app
  order by (public.store_who(p_owner) is not null and r.owner = public.store_who(p_owner)) desc, r.created_at desc
  limit 100;
$$;

revoke all on function public.store_profile_get(text), public.store_profile_save(text, text), public.store_reviews_list(uuid, text) from public;
grant execute on function public.store_profile_get(text), public.store_profile_save(text, text), public.store_reviews_list(uuid, text) to anon, authenticated;
