-- Run once in Supabase: SQL Editor -> New query -> paste -> Run.

create table if not exists public.messages (
  id         bigint generated always as identity primary key,
  chat_key   text not null,                         -- 32-char hash of the app's device id (the app computes the same one)
  ref        text not null,                         -- 10-char tag shown in Telegram under each message
  role       text not null check (role in ('user', 'admin')),
  body       text not null,
  created_at timestamptz not null default now()
);
create index if not exists messages_chat_idx on public.messages (chat_key, id);
create index if not exists messages_ref_idx  on public.messages (ref);

-- Row level security ON with no policies: the public (anon) key cannot read or write the table directly.
alter table public.messages enable row level security;

-- The only door for the app: it must know the whole 32-character chat_key to get that chat.
create or replace function public.get_messages(p_key text, p_since bigint default 0)
returns table (id bigint, role text, body text, created_at timestamptz)
language sql
security definer
set search_path = public
as $$
  select t.id, t.role, t.body, t.created_at
  from (
    select m.id, m.role, m.body, m.created_at
    from public.messages m
    where length(p_key) = 32 and m.chat_key = p_key and m.id > p_since
    order by m.id desc
    limit 100
  ) t
  order by t.id;
$$;

revoke all on function public.get_messages(text, bigint) from public;
grant execute on function public.get_messages(text, bigint) to anon;

-- Optional tidy-up (run now and then): delete from public.messages where created_at < now() - interval '90 days';
