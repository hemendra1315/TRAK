-- TRAK profiles: one row per auth user. Owner-only access via Row Level Security.
-- Plan: "TRAK — Implementation Plan (Phase 1)", section 7. Never edit after it is applied;
-- write a new forward migration instead.

create table public.profiles (
  id                 uuid primary key references auth.users (id) on delete cascade,
  display_name       text not null
                       check (char_length(btrim(display_name)) between 1 and 60),
  primary_role       text not null
                       check (primary_role in ('batter','bowler','all_rounder','wicketkeeper',
                                               'wicketkeeper_batter','other')),
  batting_hand       text check (batting_hand in ('right','left')),
  bowling_styles     text[] not null default '{}'
                       check (bowling_styles <@ array['fast','medium','off_spin','leg_spin',
                              'left_arm_orthodox','wrist_spin_other','other']::text[]),
  week_start         smallint not null default 1 check (week_start between 1 and 7), -- ISO: 1 = Monday
  weight_unit        text not null default 'kg' check (weight_unit in ('kg','lb')),
  time_zone          text not null,
  adult_confirmed_at timestamptz not null,
  client_modified_at timestamptz not null,
  created_at         timestamptz not null default now(),
  updated_at         timestamptz not null default now(),
  version            bigint not null default 1
);

comment on table public.profiles is
  'One profile per player. Owner-only RLS. Server sets created_at, updated_at, version.';

-- Server owns timestamps and version, validates the time zone, and ignores stale client writes
-- (last writer by client_modified_at wins; the skipped write is detected by the client because
-- the upsert returns no row, after which it fetches and adopts the newer server row).
create function public.profiles_before_write() returns trigger
language plpgsql
set search_path = ''
as $$
begin
  perform now() at time zone new.time_zone;   -- raises 22023 on an invalid IANA zone
  if new.client_modified_at > now() + interval '24 hours' then
    raise exception 'client_modified_at is too far in the future'
      using errcode = '22023';
  end if;

  if tg_op = 'INSERT' then
    new.created_at := now();
    new.updated_at := now();
    new.version := 1;
  else
    if new.client_modified_at < old.client_modified_at then
      return null;                               -- stale write: keep the newer row
    end if;
    new.id := old.id;
    new.created_at := old.created_at;
    new.updated_at := now();
    new.version := old.version + 1;
  end if;
  return new;
end
$$;

create trigger profiles_before_write
  before insert or update on public.profiles
  for each row execute function public.profiles_before_write();

alter table public.profiles enable row level security;

create policy profiles_select_own on public.profiles
  for select to authenticated
  using ((select auth.uid()) = id);

create policy profiles_insert_own on public.profiles
  for insert to authenticated
  with check ((select auth.uid()) = id);

create policy profiles_update_own on public.profiles
  for update to authenticated
  using ((select auth.uid()) = id)
  with check ((select auth.uid()) = id);

-- No delete policy or grant: rows are removed only by account deletion (auth.users cascade, Phase 5).

revoke all on public.profiles from anon;
revoke all on public.profiles from authenticated;
grant select, insert, update on public.profiles to authenticated;
