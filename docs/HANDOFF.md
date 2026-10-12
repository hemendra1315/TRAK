# TRAK — Engineering Handoff (read this first)

**Handoff date:** 2026-10-12
**Repository:** github.com/hemendra1315/TRAK (branch `main`)
**State at handoff:** Phase 1 code complete; every automated Phase 1 test green in CI (run 38285506999,
commit `6e8feb2`). Phase 1 still needs owner manual checks and approval before Phase 2 starts.

This file is the single source of truth for whoever continues the build. It condenses the product
blueprint, the approved Phase 1 plan, every decision taken, and the conventions the code already follows.
Decision details and version evidence: `docs/decisions/0001-toolchain-and-versions.md`.

---

## 1. Product

**TRAK — Track the work. See the progress.** An Android-first personal cricket performance journal for
competitive adult cricketers. It records training, bowling and batting workload, gym, sleep and recovery,
and turns them into trustworthy progress graphs.

**Central principle: record once, understand progress automatically.** One daily journal is the only
place a player enters a day's data. Progress graphs are derived from the same structured records, so
nothing is ever entered twice.

**Not:** a live scorer, team or academy manager, social network, AI coach, or medical tool. No injury
prediction, diagnosis, or "safe/unsafe workload" claims. No streaks, guilt-inducing notifications or
gamification. Reminders are opt-in only.

**Navigation (fixed in every phase):** Home · Log · Progress · Recovery · Profile.
- **Log** opens the daily journal for a date (`JournalRoute(date)`, today by default).
- **Recovery** shows today and the previous 7 days only (decision E6). Long-term graphs live only in **Progress**.

**Design:** near-black, minimal, calm. Neutral surfaces (#0A0A0A, #111111, #161616), text #F5F5F5,
one teal accent #2DD4BF (Ground Mode accent #115E59). No green-tinted backgrounds, no blue/purple as
dominant colours, no gradients, glassmorphism, neon or decorative animation. **Ground Mode** is a
high-contrast light theme for direct sunlight. Touch targets ≥ 48 dp, primary buttons 56 dp; Save stays
reachable above the keyboard (`SaveBar`). Tokens live in `core/designsystem/.../theme/Color.kt`, and
`ContrastTest` enforces WCAG ratios (text ≥ 4.5:1, borders ≥ 3:1) and neutral surfaces.

---

## 2. Non-negotiable rules

1. **Missing is not zero.** Every measurement is nullable; null = "not recorded". No `DEFAULT 0` on
   measurements, and never coerce null to 0 in code, queries or charts.
2. **An empty day is not a rest day and not zero workload.** Rest is an explicit entry.
3. **Never infer** legal balls, overs, workload or anything not recorded. Overs come only from legal
   balls (27 legal → "4.3", 36 → "6.0"); never from deliveries and never by multiplying overs × 6.
4. **No double counting.** Session `elapsed_minutes` is the only source of training time. Activity minutes
   split it (remainder = "unallocated") and may not exceed it. Drill balls ≤ entry balls faced. Spell
   totals replace entry totals and are never added to them. Rules live in `core/model/.../NumericRules.kt`.
5. **Local-first.** Room is the source of truth; the UI observes Room through repositories. No network
   calls from Composables or ViewModels; only `:core:data` imports supabase-kt.
6. **No data loss.** Sync never deletes data. Unsynced data is never discarded without a typed
   confirmation ("DELETE"). Sign-out never silently discards unsynced data (decision S1).
7. **Security.** RLS on every user table; ownership comes from `auth.uid()`, never trusted from the client.
   **No service-role or secret key in the app or repo**; CI fails if one appears. Privileged operations
   (account deletion, export) run only server-side (Phase 5 Edge Functions).
8. **Migrations are explicit and never destructive.** No `fallbackToDestructiveMigration`. Never edit an
   applied Supabase migration; write a forward migration. Commit Room schema JSON; CI fails on drift.
9. **Analytics only in a pure Kotlin `:analytics` module (Phase 4), unit-tested.** Screens never compute
   metrics. Every metric returns its value plus coverage ("5 of 7 days"); incomplete periods are labelled.
10. **Adult-only pilot.** The "I am 18 or older" confirmation stays required. Juniors need a
    counsel-reviewed consent flow and explicit owner approval.
11. **External resources and cost:** do **not** create a hosted Supabase project, an SMTP provider account
    or any paid resource without the owner's explicit approval. All database testing uses the **local**
    Supabase stack.
12. **Honesty.** Never claim a build, test or deploy result you did not observe. Record evidence (CI run
    IDs, command output) in the ADR or reports.

---

## 3. Stack and verified versions

Verified on 2026-10-12 and proven together in CI (see ADR 0001):

| Item | Version / setting |
| --- | --- |
| AGP | 9.4.0 (**built-in Kotlin**: do NOT apply `org.jetbrains.kotlin.android`) |
| Gradle | 9.6.0 wrapper, JDK 17 |
| Kotlin | 2.4.21 (KGP version set by the root plugin declarations) |
| KSP / Hilt | 2.3.11 / 2.60.1 |
| Compose BOM | 2026.09.00 → compose-ui 1.12.1, material3 1.4.0 |
| SDK | compileSdk **37** (required by Compose 1.12.1), targetSdk 36, minSdk 26 |
| Room | **2.8.5** (not Room 3; decision D4). DAOs are suspend/Flow only |
| DataStore / Navigation / Lifecycle | 1.2.1 / 2.10.2 (type-safe routes) / 2.11.0 |
| supabase-kt | 3.8.0 BOM (auth-kt, postgrest-kt) + Ktor 3.5.1 OkHttp |
| Tests | JUnit 4.13.2, coroutines-test 1.11.0, androidx.test core/runner 1.7.0, ext-junit 1.3.0 |

All versions are in `gradle/libs.versions.toml`. Verify any new dependency against its official release
page and record it in the ADR.

**supabase-kt facts learned (do not regress):**
- Always pass our own `sessionManager` **and** `codeVerifierCache`. The library defaults (Settings-based)
  fail outside platform settings, and we must own storage for backup exclusion. See `SupabaseProvider.kt`.
- `SessionManager.loadSession()` must throw `NoSessionFoundException` when empty (`DataStoreSessionManager`).
- Use `lifecycleCallbacks = false` when constructing a client in plain JVM tests (no main looper).
- Email OTP: `auth.signInWith(OTP) { email = …; createUser = true }`; verify with
  `auth.verifyEmailOtp(type = OtpType.Email.EMAIL, email, token)`.

---

## 4. Architecture and modules

```
:app                 Navigation, screens, ViewModels (Hilt). No business logic, no network.
:core:designsystem   Theme tokens (Dark/Ground), type, spacing, components: TrakButton, TrakTextField,
                     ChoiceChips, LoadingView/EmptyView/ErrorView/OfflineBanner, SaveBar.
:core:model          Pure Kotlin: ids, Profile, enums (codes match DB constraints), Rating5, Rpe10,
                     LocalDay, validation rules (Profile/Numeric/TimeRange/Auth).
:core:data           Room (TrakDatabase v1: profile, app_meta), DAOs, DataStore (settings, session),
                     SupabaseClientHolder, AuthGateway, ProfileRemote, ProfileRepository,
                     SessionCoordinator (account guard), AppEntryResolver (routing), Hilt modules.
:analytics           (Phase 4) pure Kotlin metric functions + tests.
supabase/            config.toml, migrations/, tests/ (pgTAP), templates/ (OTP email shows {{ .Token }}).
scripts/e2e_auth_rls.py   API-level email-OTP + RLS end-to-end check against the local stack.
```

**Data flow:** Composable → ViewModel (`StateFlow`) → repository → Room. Writes: validate (`:core:model`)
→ Room transaction (row with `sync_state = PENDING`) → background sync.

**Routing** (`AppEntryResolver`): no session → sign-in; another account's unsynced data on the phone →
account guard; no local profile → restore from server, else onboarding (never onboarding while the server
can't be checked); otherwise the main tabs. A stored session works offline.

**Phase 1 profile sync** (`ProfileRepository`, replaced by the outbox in Phase 2): mutex-serialised
idempotent upsert keyed on id with return=representation. Mark SYNCED only if `client_modified_at` is
unchanged since send (fix B1). If the server ignored a stale write (empty return), fetch and adopt the
newer server row (fix B2). Network/401 → stays PENDING; 4xx refusal → FAILED with reason; never deletes.

**Account safety** (`SessionCoordinator`, fix B3): `app_meta.owner_user_id` marks the owner. A different
account is blocked while the owner has unsynced rows. Sign-out offers Sync now / Keep on this phone /
typed-DELETE. Session expiry keeps data.

**Backups** (fix B4): `allowBackup=false` and `data_extraction_rules.xml` / `backup_rules.xml` exclude
database, files (DataStore incl. session), sharedpref and root. `BackupRulesTest` enforces this.

---

## 5. Table conventions (every new table copies these)

- `id` UUID generated on the device (idempotent retries); `user_id` on **every** row, child rows
  included, so each RLS policy is a single equality check.
- Day-scoped records: `local_date` + `tz` (IANA region id). Instants as UTC `timestamptz`.
- `client_modified_at`, `created_at` / `updated_at` / `version` (server-set by trigger), `deleted_at`
  (tombstone); locally also `sync_state` (PENDING/SYNCED/FAILED) and `sync_error`.
- Measurements nullable, integer minutes, integer ml, `numeric` weight + unit. Enum codes are checked
  constraints mirrored by Kotlin enums with `code` and `fromCode`.
- RLS per table: select/insert/update own (`(select auth.uid()) = user_id`); child inserts also verify
  the parent belongs to the caller. No delete grant (tombstones via update). Revoke all from `anon`.
- Stale-write guard trigger, as in `20261013000100_profiles.sql`.

---

## 6. Journal and Progress data design (approved, decision E5)

The journal is **one scrolling screen that edits many tables**. Progress reads those tables and nothing
else. Each fact has exactly one home.

| Journal section | Stored in | Rules |
| --- | --- | --- |
| Day type + "what happened today" chips | `journal_days.day_type` (training, rest_planned, rest_unplanned, ill_or_injured, unspecified) + one `training_sessions` row per activity chip | A chip without detail creates a session with all-null measurements: it counts as a session, never as minutes or balls. Rest can't be chosen while sessions exist that day. |
| Duration, effort | `training_sessions.elapsed_minutes`, `rpe` (1–10) | Only source of training time |
| Activity split | `session_activities.minutes` | Never added to elapsed; remainder = unallocated |
| Bowling | `bowling_entries` (context nets/match/practice_match/solo/other, style, deliveries, legal_balls), `bowling_spells` | Legal balls never inferred; overs only from legal balls; spells replace entry totals |
| Batting | `batting_entries` (context, balls_faced, focus tags), `batting_drills` | Drill balls ≤ entry total |
| Gym | `workout_exercises`, `exercise_sets` (session kind gym); `exercise_definitions`, `workout_templates` | PRs computed, never stored |
| Sleep | `sleep_records` keyed by wake date | Time in bed (reported) across midnight/DST via `TimeRangeRules` |
| Energy, rested, fatigue, soreness, mood, confidence, motivation | `journal_days` rating columns (1–5, nullable) | Replaces a separate recovery_checkins table; labelled anchors on every scale |
| Sore areas | `soreness_records` (child of `journal_days`) | Area, side, severity, note; no diagnosis |
| Hydration amount | `hydration_entries.water_ml` (optional) | Decision E7 |
| Hydration/fuelling reflection | `journal_days.fuel_reflection` (chips) + `fuel_note` | Separate from ml |
| Session reflection, coach feedback | `session_reflections`, `coach_feedback` (`author_type`) | Per session |
| Tomorrow's focus, best moment, lesson, note | `journal_days.*` text | Never parsed into numbers |

**Saving:** one Save writes all touched tables in one Room transaction plus one outbox item per changed
row. An unsaved draft persists locally but is invisible to Progress. An empty day has no `journal_days`
row, so it is a gap, never zero or rest. Past dates use the same screen.

**Progress (Phase 4)** reads only these tables. Periods are week, month, 3 months and year/season (a season
is a player-defined date range). Every chart states what it measures, its period and its coverage. Nets
and matches stay separate. Journal consistency is a count, never a streak.

---

## 7. Phase status

### Phase 1 — Foundation: code complete, awaiting owner sign-off

**Done (in `main`):** build, catalog, CI, design system, domain rules, profiles migration + RLS, email OTP
template, Room v1, session storage, sync rules (B1/B2), account guard (B3), backup rules (B4), sign-in,
onboarding (name, role, 18+), five-tab shell with honest empty states, Log → journal placeholder with a
date picker, Profile / Edit / Settings (theme, week start, units, Sync now), sign-out dialogs.

**CI evidence, run 38285506999 on `6e8feb2` (all jobs green):**
- Unit tests: 69 run, 0 failures (1 skipped = the integration test, which runs in the database job)
- Emulator (API 35): 11 tests, 0 failures (Room DAO + schema, onboarding A12, tabs A13, dialogs A14)
- Kotlin integration test vs local Supabase: 1/1 (email code sign-in, upsert, stale guard, RLS, repo sync)
- pgTAP RLS: 12/12. API end-to-end (`scripts/e2e_auth_rls.py`): 13/13
- Lint and secret scan: pass

**Remaining to close Phase 1:**
1. Owner manual checks on a real device:
   (a) fresh install → email code → onboarding → Home in < 2 min;
   (b) airplane mode → edit profile → kill app → reopen → change kept with "will sync" → reconnect → synced;
   (c) second device restores the profile, no re-onboarding;
   (d) Ground Mode readable in direct sun;
   (e) TalkBack labels and font scale 200% without clipping.
   Manual checks (a)–(c) need a Supabase project. Use a local stack via `supabase start` with the app
   pointed at your machine, or a hosted project once the owner approves one.
2. Not yet verified automatically: "keyboard Next moves focus" and "Save visible with the keyboard open"
   (A12 parts). Add tests or record them as manual checks.
3. Write `docs/reports/phase-1.md` (files, commands, CI results above, known issues), then get owner approval.

### Phase 2 — Daily tracking and journal (next, after approval)
- Room v2 with an **explicit tested migration**: the tables in section 6 for sessions, activities,
  bowling, spells, batting, drills, gym, templates, reflections, coach feedback and journal_days core.
  The migration must convert any PENDING/FAILED profile row into an outbox item.
- **Outbox sync:** `outbox(id, table, row_id, op, created_at, attempts, last_error)` written in the same
  transaction as each change; `SyncWorker` (WorkManager, network-constrained, expedited + periodic,
  exponential backoff); push parents before children; idempotent upserts by client UUID; pull by
  `(updated_at, id)` cursor per table; tombstones; row-level last-writer-wins by `client_modified_at`; a
  server `row_history` trigger keeps overwritten rows (no silent loss). The UI shows pending/failed state.
- Supabase migrations for every new table with RLS, constraints matching `:core:model` rules, and pgTAP
  cross-user tests per table (including child-attached-to-foreign-parent).
- **The single-page journal UI:** chips reveal activity-specific sections; one Save; partial saves; edit
  past dates; quick entry in a few taps; "Add detail" for spells, drills and sets; `CountStepper` (−1/+1/+6/+12),
  `RatingRow` with labelled anchors. Target: a basic nets log in under 60 s.
- Training history list and edit/delete (delete = tombstone + undo).
- Tests: offline create/edit/delete round trips, duplicate prevention on retry, process death mid-sync,
  parent/child ordering, conflict loser preserved, migration v1 → v2.

### Phase 3 — Recovery
Sleep form, ratings, sore areas, hydration, explicit rest days, Recovery tab (today + previous 7 days),
opt-in reminders (POST_NOTIFICATIONS on 33+). All optional; no medical interpretation; severe or worsening
pain shows a calm suggestion to seek professional advice.

### Phase 4 — Progress and analytics
Create `:analytics` (pure Kotlin) with tested functions: bowling workload, nets vs match, overs from legal
balls, balls faced, training time + unallocated, session counts, explicit rest days, sleep and recovery
averages over recorded days only, gym PRs, period comparisons (% only when baseline > 0; "new this period";
"not enough data"; incomplete-period labels), goal progress, data completeness. Progress tab with week /
month / 3 months / season filters, line and bar charts with labelled axes and coverage. A neutral
workload-change notice (no risk score) only with ≥ 3 prior weeks of data.

### Phase 5 — Privacy and reliability
Edge Functions `export-data` (caller's rows as JSON + CSV) and `delete-account` (service role server-side
only), sync recovery screen, security review, Play data-safety preparation, custom SMTP before the pilot.

### Phase 6 — Pilot
15–20 adult players, 6 weeks. Measure logging time, retention, sync failures, data-loss incidents,
calculation errors and graph usefulness. Simplify based on evidence.

**Do not start a phase until the previous phase's acceptance criteria pass and the owner approves.**

---

## 8. Working in this repo

**Local commands (Windows: use `gradlew.bat`):**
```
./gradlew assembleDebug
./gradlew test                                  # JVM unit tests (all modules)
./gradlew lint
./gradlew connectedDebugAndroidTest             # needs an emulator/device
supabase start && supabase db reset && supabase test db     # needs Docker
python scripts/e2e_auth_rls.py                  # needs API_URL, ANON_KEY, MAIL_URL from `supabase status -o env`
```
The integration test runs when `TRAK_IT_API_URL`, `TRAK_IT_KEY` and `TRAK_IT_MAIL_URL` are set; otherwise
it is skipped.

**Config:** copy `local.properties.example` → `local.properties`; use only the publishable/anon key.
Without Supabase settings the app runs and sign-in reports "not configured".

**CI** (`.github/workflows/ci.yml`), four jobs:
- Build and unit tests: assemble, unit tests, lint, Room schema committed-check
- Secret scan: gitleaks plus a service-role reference check
- Database: local Supabase, db reset, pgTAP, API end-to-end, Kotlin integration test
- Instrumented: API 35 emulator

CI also emits results, errors and resolved versions as check-run annotations (`.github/scripts/*`).

**Room schema changes:** bump the version, add a `Migration` and a `MigrationTestHelper` test, build so
Room exports `core/data/schemas/.../<n>.json`, and commit that file (CI fails otherwise).

**Commits:** small and reviewable, one step per commit, CI green before the next step. Each phase ends
with a report: files changed, commands run, actual results, unresolved issues.

**Open decisions:**
- D3 pricing: no billing code until decided.
- D8 `app.trak.android` is provisional.
- A hosted Supabase project (region ap-south-1) and the SMTP provider both need owner approval.
