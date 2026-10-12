-- pgTAP: owner-only access to public.profiles (acceptance test A11).
-- Run with `supabase test db` against the local stack. Everything rolls back.
begin;
create extension if not exists pgtap with schema extensions;
select plan(12);

insert into auth.users (id, email) values
  ('11111111-1111-1111-1111-111111111111', 'a@test.local'),
  ('22222222-2222-2222-2222-222222222222', 'b@test.local');

-- RLS is on and the table has exactly the expected policies.
select ok((select relrowsecurity from pg_class where oid = 'public.profiles'::regclass),
  'RLS is enabled on public.profiles');
select policies_are('public', 'profiles',
  array['profiles_select_own','profiles_insert_own','profiles_update_own'],
  'only owner select/insert/update policies exist');

-- ---------- Act as user A ----------
set local role authenticated;
select set_config('request.jwt.claims',
  '{"sub":"11111111-1111-1111-1111-111111111111","role":"authenticated"}', true);

-- Invalid zone is tested before A's row exists, so it cannot fail on a duplicate key instead.
select throws_ok($$ insert into public.profiles (id, display_name, primary_role, time_zone,
  adult_confirmed_at, client_modified_at) values ('11111111-1111-1111-1111-111111111111',
  'A', 'bowler', 'Not/AZone', now(), now()) $$, '22023', null, 'invalid time zone rejected');

select lives_ok($$ insert into public.profiles (id, display_name, primary_role, time_zone,
  adult_confirmed_at, client_modified_at) values ('11111111-1111-1111-1111-111111111111',
  'A', 'bowler', 'Asia/Kolkata', now(), now()) $$, 'A can create own profile');

select throws_ok($$ insert into public.profiles (id, display_name, primary_role, time_zone,
  adult_confirmed_at, client_modified_at) values ('22222222-2222-2222-2222-222222222222',
  'X', 'batter', 'Asia/Kolkata', now(), now()) $$, '42501', null, 'A cannot create B''s profile');

-- ---------- Act as user B ----------
select set_config('request.jwt.claims',
  '{"sub":"22222222-2222-2222-2222-222222222222","role":"authenticated"}', true);
insert into public.profiles (id, display_name, primary_role, time_zone, adult_confirmed_at, client_modified_at)
  values ('22222222-2222-2222-2222-222222222222', 'B', 'batter', 'Asia/Kolkata', now(), now());

select is((select count(*) from public.profiles)::int, 1, 'B sees only own row');
select is((select count(*) from public.profiles
  where id = '11111111-1111-1111-1111-111111111111')::int, 0, 'B cannot select A');

update public.profiles set display_name = 'hacked', client_modified_at = now()
  where id = '11111111-1111-1111-1111-111111111111';

select throws_ok($$ delete from public.profiles where id = '22222222-2222-2222-2222-222222222222' $$,
  '42501', null, 'B cannot delete, even own row (no delete grant)');

-- Stale write (older client_modified_at) is ignored by the trigger.
update public.profiles set display_name = 'old', client_modified_at = now() - interval '1 day'
  where id = '22222222-2222-2222-2222-222222222222';
select is((select display_name from public.profiles), 'B', 'stale client write ignored');

-- ---------- Back to A: B's update must not have touched A's row ----------
select set_config('request.jwt.claims',
  '{"sub":"11111111-1111-1111-1111-111111111111","role":"authenticated"}', true);
select is((select display_name from public.profiles), 'A', 'B''s update did not change A''s row');
select is((select version from public.profiles), 1::bigint, 'A''s version unchanged');

-- ---------- Anonymous role ----------
reset role;
set local role anon;
select throws_ok($$ select * from public.profiles $$, '42501', null, 'anon has no access');

select * from finish();
rollback;
