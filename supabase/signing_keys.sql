-- One signing key per account, so a reinstall (or a new phone) gets the same key back after login.
-- Run once in Supabase: SQL Editor -> New query -> paste -> Run. Safe to run again.
-- NOTE: the key is stored as-is (not encrypted). Row-level security makes each row readable
-- only by its own account, but anyone with admin access to this Supabase project can read them.

create table if not exists public.signing_keys (
  uid        uuid primary key references auth.users(id) on delete cascade,
  key_b64    text not null,
  cert_b64   text not null,
  updated_at timestamptz not null default now()
);
alter table public.signing_keys enable row level security;

drop policy if exists signing_keys_select on public.signing_keys;
drop policy if exists signing_keys_insert on public.signing_keys;
drop policy if exists signing_keys_update on public.signing_keys;
drop policy if exists signing_keys_delete on public.signing_keys;
create policy signing_keys_select on public.signing_keys for select to authenticated using (uid = auth.uid());
create policy signing_keys_insert on public.signing_keys for insert to authenticated with check (uid = auth.uid());
create policy signing_keys_update on public.signing_keys for update to authenticated using (uid = auth.uid()) with check (uid = auth.uid());
create policy signing_keys_delete on public.signing_keys for delete to authenticated using (uid = auth.uid());

revoke all on public.signing_keys from anon, public;
grant select, insert, update, delete on public.signing_keys to authenticated;
