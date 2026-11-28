-- Run once in Supabase: SQL Editor -> New query -> paste -> Run.
create table if not exists public.devices (
  chat_key   text primary key,   -- same 32-char key as in public.messages
  token      text not null,      -- Firebase push token of that phone
  updated_at timestamptz not null default now()
);
alter table public.devices enable row level security;   -- no policies: only the function below can write

create or replace function public.register_device(p_key text, p_token text)
returns void
language sql
security definer
set search_path = public
as $$
  insert into public.devices (chat_key, token)
  select p_key, p_token
  where length(p_key) = 32 and length(p_token) between 20 and 4096
  on conflict (chat_key) do update set token = excluded.token, updated_at = now();
$$;

revoke all on function public.register_device(text, text) from public;
grant execute on function public.register_device(text, text) to anon;
