-- Store ratings & reviews. Run once in Supabase: SQL Editor -> New query -> paste -> Run.
-- (Run store.sql first. Safe to run again.)

alter table public.store_apps add column if not exists rating_avg numeric(3,2) not null default 0;
alter table public.store_apps add column if not exists rating_count int not null default 0;

create table if not exists public.store_reviews (
  id         uuid primary key default gen_random_uuid(),
  app_id     uuid not null references public.store_apps(id) on delete cascade,
  owner      text not null,
  name       text not null default 'WZH user',
  stars      smallint not null check (stars between 1 and 5),
  body       text not null default '',
  created_at timestamptz not null default now(),
  unique (app_id, owner)
);
-- the app owner's reply to a review (one reply per review, like Google Play's "Developer reply")
alter table public.store_reviews add column if not exists reply text;
alter table public.store_reviews add column if not exists reply_at timestamptz;
create index if not exists store_reviews_app_idx on public.store_reviews (app_id, created_at desc);
alter table public.store_reviews enable row level security;   -- no policies: the app only uses the functions below

-- keeps the average and the count on the app row, so lists can show stars without extra calls
create or replace function public.store_rating_refresh(p_app uuid)
returns void language sql security definer set search_path = public as $$
  update public.store_apps a set
    rating_avg = coalesce((select round(avg(stars)::numeric, 2) from public.store_reviews where app_id = p_app), 0),
    rating_count = (select count(*) from public.store_reviews where app_id = p_app)
  where a.id = p_app;
$$;

-- newest first, the caller's own review on top; "mine" tells the app which one can be edited
drop function if exists public.store_reviews_list(uuid, text);
create or replace function public.store_reviews_list(p_app uuid, p_owner text default '')
returns table (id uuid, name text, stars int, body text, created_at timestamptz, mine boolean, reply text, reply_at timestamptz)
language sql security definer set search_path = public as $$
  select r.id, r.name, r.stars::int, r.body, r.created_at, (public.store_who(p_owner) is not null and r.owner = public.store_who(p_owner)), r.reply, r.reply_at
  from public.store_reviews r
  where r.app_id = p_app
  order by (public.store_who(p_owner) is not null and r.owner = public.store_who(p_owner)) desc, r.created_at desc
  limit 100;
$$;

create or replace function public.store_review_save(p_owner text, p_app uuid, p_stars int, p_body text, p_name text)
returns void language plpgsql security definer set search_path = public as $$
declare v_who text;
begin
  v_who := public.store_who(p_owner);
  if v_who is null then raise exception 'bad owner'; end if;
  if p_stars is null or p_stars < 1 or p_stars > 5 then raise exception 'Pick 1 to 5 stars'; end if;
  if not exists (select 1 from store_apps where id = p_app and status = 'approved') then
    raise exception 'That app is not in the Store';
  end if;
  if exists (select 1 from store_apps where id = p_app and owner = v_who) then
    raise exception 'You can''t review your own app';
  end if;
  insert into store_reviews (app_id, owner, name, stars, body)
  values (p_app, v_who, left(coalesce(nullif(trim(p_name), ''), 'WZH user'), 40), p_stars::smallint, left(trim(coalesce(p_body, '')), 500))
  on conflict (app_id, owner) do update
    set stars = excluded.stars, body = excluded.body, name = excluded.name;
  perform store_rating_refresh(p_app);
end $$;

create or replace function public.store_review_delete(p_owner text, p_app uuid)
returns void language plpgsql security definer set search_path = public as $$
begin
  delete from store_reviews where app_id = p_app and public.store_who(p_owner) is not null and owner = public.store_who(p_owner);
  perform store_rating_refresh(p_app);
end $$;

-- the owner of an app answers a review of that app; an empty text removes the reply
create or replace function public.store_review_reply(p_owner text, p_review uuid, p_body text)
returns void language plpgsql security definer set search_path = public as $$
declare v_who text; v_body text;
begin
  v_who := public.store_who(p_owner);
  if v_who is null then raise exception 'bad owner'; end if;
  if not exists (select 1 from store_reviews r join store_apps a on a.id = r.app_id where r.id = p_review and a.owner = v_who) then
    raise exception 'You can only reply to reviews of your own apps';
  end if;
  v_body := left(trim(coalesce(p_body, '')), 500);
  update store_reviews
    set reply = nullif(v_body, ''), reply_at = case when v_body = '' then null else now() end
    where id = p_review;
end $$;

revoke all on function public.store_rating_refresh(uuid), public.store_reviews_list(uuid, text),
  public.store_review_save(text, uuid, int, text, text), public.store_review_delete(text, uuid), public.store_review_reply(text, uuid, text) from public;
grant execute on function public.store_reviews_list(uuid, text),
  public.store_review_save(text, uuid, int, text, text), public.store_review_delete(text, uuid), public.store_review_reply(text, uuid, text) to anon, authenticated;
