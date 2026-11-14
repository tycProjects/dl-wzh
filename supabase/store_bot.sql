-- Run once in Supabase -> SQL Editor (needed by the Telegram approve buttons).
alter table public.store_apps add column if not exists notified boolean not null default false;
