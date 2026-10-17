# Phase 1 report: Foundation

Status: **automated work complete; not signed off.** The real-device checks A–E below are BLOCKED. They
need a phone and the project owner, so Phase 1 cannot be signed off yet.

- Baseline: `main` at `d15ab3d` (12 Oct 2026). The repository matched HANDOFF.md section 7, with one
  discrepancy, listed under "Brief vs repository".
- Close-out work: branch `phase1-closeout`, draft PR #1 (not merged). The CI evidence below is from
  this branch.

## 1. Scope completed

| Item | Result |
|---|---|
| Keyboard Next/Done (A12) | Fixed and regression-tested |
| Save/Continue visible with the keyboard open (A12) | Fixed for screens inside the tab shell; onboarding was already correct; regression-tested |
| Local-first profile sync audit | One defect fixed (no automatic retry on reconnect); one honesty improvement (Sync now) |
| Real-device checklist | Written (section 5); all checks BLOCKED pending a device and the owner |
| Phase 2 | Not started |

### Brief vs repository
- Onboarding has only **one** text field (name). The role is chips and the 18+ confirmation is a
  checkbox. So "Next moves focus to the next input" has no second text field to move to on this
  form. The name field's key is therefore **Done**. Done closes the keyboard and clears focus, which
  brings the role chips, the 18+ row and Continue into view. It does not submit, because role and
  18+ may still be missing.
- The Next behaviour itself was broken app-wide. `TrakTextField` defaulted to Next with an empty
  handler, so pressing Next did nothing. It now does the platform default: Next moves focus without
  closing the keyboard, and Done closes it. Every future multi-field form, such as the Phase 2
  journal, inherits this.

## 2. Files changed

| File | Why |
|---|---|
| `core/designsystem/.../TrakTextField.kt` | With no handler, the IME key now does the platform default instead of nothing |
| `app/.../ui/ProfileFields.kt` | Name field uses Done, which clears focus and closes the keyboard. Shared by onboarding and Edit profile |
| `app/.../main/MainShell.kt` | `consumeWindowInsets(padding)` on the NavHost. Before, a nested Save bar counted the navigation bar twice and floated a tab bar's height above the keyboard |
| `core/data/.../network/NetworkMonitor.kt` (new) | Validated-connectivity flow (`registerDefaultNetworkCallback`) |
| `core/data/src/main/AndroidManifest.xml` (new) | Declares `ACCESS_NETWORK_STATE` for the module. The app already had it; lint needs it at module level |
| `core/data/.../di/DataModule.kt` | Binds `NetworkMonitor` |
| `core/data/.../profile/ProfileRepository.kt` | `syncWhenOnline()` retries a PENDING profile on each offline-to-online transition |
| `app/.../profile/ProfileViewModel.kt` | Starts `syncWhenOnline` for the signed-in user. `syncNow()` reports an attempt that did not reach the server |
| `app/.../profile/ProfileScreens.kt`, `res/values/strings.xml` | Shows the "Couldn't reach the server…" notice under Sync now |
| `app/src/androidTest/.../KeyboardUiTest.kt` (new) | 5 keyboard and inset regression tests |
| `app/src/androidTest/.../OnboardingUiTest.kt` | Follows the real flow (type, Done, scroll to the chips). The old version tapped chips hidden behind the open keyboard on a small emulator |
| `core/data/src/test/.../ProfileRepositoryTest.kt` | A8b–A8d reconnect tests |
| `app/src/test/.../profile/SyncNoticeTest.kt` (new) | Sync now honesty mapping |
| `docs/reports/phase-1.md` | This report |

No migrations, no schema changes, no Supabase changes, no new dependencies.

## 3. Keyboard fixes and regression coverage (`KeyboardUiTest`)

1. Onboarding name: Done leaves the field and does not submit.
2. Two fields: Next moves focus to the second field, and the second's Done leaves it.
3. 320 × 480 dp screen at 200% font: Continue is laid out fully on screen.
4. Real soft keyboard, edge-to-edge and adjustResize like `MainActivity`, 200% font: Continue sits
   entirely above the keyboard.
5. Real soft keyboard inside the five-tab shell: Save sits on the keyboard (not under it, and no more
   than 24 dp above it). This is the regression test for the double-inset fix.

Tests 4 and 5 use the emulator's real keyboard. If the keyboard never appears, they report as
**skipped**, never as passed. In CI it appeared, and they ran.

## 4. Automated results

Final CI run [39040956760](https://github.com/hemendra1315/TRAK/actions/runs/39040956760) on commit
`742ea5d`. All four jobs passed.

| Check | Baseline (`6e8feb2`) | This branch (`742ea5d`) |
|---|---|---|
| `assembleDebug` (debug APK) | pass | pass |
| Unit tests (`test`) | 69 run, 0 failed, 1 skipped | **74 run, 0 failed, 1 skipped** (+3 ProfileRepositoryTest, +2 SyncNoticeTest). The skip is the integration test, which runs in the database job |
| Android lint (`lint`) | pass | pass |
| Emulator tests (API 35, `connectedDebugAndroidTest`) | 11 run, 0 failed | **16 run, 0 failed, 0 skipped** (+5 KeyboardUiTest). 0 skipped means the real-keyboard tests 4 and 5 actually ran |
| Kotlin integration test vs local Supabase | 1/1 | **1/1** |
| pgTAP RLS tests | 12/12 | **12/12** |
| API end-to-end (`scripts/e2e_auth_rls.py`) | 13/13 | **13/13** |
| Secret scan | pass | pass |
| `git diff --check` | n/a | clean |

How it got there, for the record:
- The first run (`7ee8daf`) had three failures:
  - **lint:** `MissingPermission`, because `ACCESS_NETWORK_STATE` was declared only in the app
    module. Fixed by declaring it in `core:data`.
  - **Emulator:** `OnboardingUiTest` failed (1 of 16). The real keyboard covered the role chips on
    the 320 dp-wide emulator and the test tapped a hidden chip. The test now follows the real flow.
  - **Database job:** GitHub API rate limit while downloading the Supabase CLI. No test ran. It
    passed on the next run with no change.
- The second run is the one in the table above.

Not run in this workspace: Gradle cannot reach Maven or Google here, so every build and test ran in
GitHub Actions on the `phase1-closeout` branch.

## 5. Real-device checklist

No physical device, second device, test inbox or outdoor session was available to me, so every row
is **BLOCKED**. The owner runs them and fills in "Actual".

Backend for A–C: the **local** Supabase stack. Run `supabase start` (needs Docker), then put the API
URL and publishable key printed by the CLI in `local.properties`, using your PC's LAN IP rather than
127.0.0.1 so the phone can reach it. Read the code from Mailpit at http://localhost:54324. No hosted
project is needed.

| # | Steps | Expected | Actual | Status |
|---|---|---|---|---|
| A. First sign-up | Uninstall or clear data. Install the debug APK. Enter email, Send code, enter the code from Mailpit. Enter name, pick a role, tick 18+, Continue. Time it from launch, minus email wait | Home in under 2 min. No crash. Exactly one row in `public.profiles` for the user (`select count(*) … where id = <uid>` = 1) | | BLOCKED: needs a phone and the local stack |
| B. Offline edit | Signed in and Profile shows "Saved and synced". Airplane mode on. Edit profile, change name, Save. Swipe the app away, reopen | Profile shows the new name and "Saved on this phone. Will sync when online." | | BLOCKED |
| B (cont.) | Airplane mode off and wait about 10 s **without tapping anything** | Status becomes "Saved and synced". On the server, the name is updated and `version` went up by exactly 1. Still one row | | BLOCKED |
| C. Second device | On an emulator or second phone pointed at the same stack, sign in with the same email | Goes straight to Home with the same name and role. No onboarding. Nothing from any other account | | BLOCKED: needs a second device |
| D. Ground Mode | Settings > Theme > Ground. Outdoors in direct sun, check Home, Profile, Settings, chips (selected vs not), sync status line and buttons | Every text and selected state is readable. Nothing depends on colour alone | | BLOCKED: needs outdoor testing by a person. Automated contrast checks (A5) are not evidence of sunlight readability |
| E. Accessibility | TalkBack on: walk sign-in, onboarding, all tabs, Profile, Settings, sign-out dialog. Then Display size max and Font size 200% and repeat | Every control has a spoken label and role. Focus order follows reading order. No clipped or overlapping text. Continue and Save stay reachable with the keyboard open. Touch targets ≥ 48 dp | | BLOCKED: needs a phone. Font scale and keyboard layout are partly covered by `KeyboardUiTest` 3–5 (automated, not a substitute) |

## 6. Sync and security audit

| Requirement | Finding |
|---|---|
| Local write before network | Yes. `save()` writes Room as PENDING, then sync runs separately (A7) |
| Survives process death | Yes. Room is the source of truth. On relaunch, `AppEntryResolver` triggers a sync for an existing profile |
| Retries when connectivity returns | **Defect, fixed.** Before, retries happened only on the next save, app start or Sync now. Now a validated-network transition triggers sync (A8b, A8c). Refused (FAILED) rows are not auto-retried (A8d) |
| No duplicate server records | Yes. The row key is the auth user id, writes are upserts, and stale writes are skipped by `client_modified_at`. Flapping connectivity after a sync causes no extra push or version (A8b) |
| Auth expiry / rejected writes | 401 or network failure keeps the edit PENDING. A server refusal marks it FAILED with the reason shown. A failed token refresh while offline keeps the user signed in offline. If the session is actually ended, the app returns to sign-in and the data stays locked to its owner. **Not verified on device** |
| Sign-out never silently discards | Yes. A typed DELETE is required (A14, SessionCoordinatorTest) |
| No mixing of accounts | Yes. The account guard blocks another account while unsynced data exists (B3 tests) |
| Sync now honesty | Improved. It never showed false success, but an attempt that couldn't reach the server used to look like nothing happened. Now it says so (SyncNoticeTest) |
| Server RLS | Unchanged. pgTAP and API end-to-end results are in section 4 |

Known limitation, by design until Phase 2: a 401 while online stays PENDING until the next trigger
(save, app start, reconnect or Sync now). Phase 2's WorkManager outbox replaces this.

## 7. Known defects and blockers

- Real-device checks A–E: BLOCKED (section 5).
- `KeyboardUiTest` only proves keyboard layout on the CI emulator (API 35). Other OEM keyboards are
  covered by check E.
- The CI database job failed twice on a GitHub API rate limit while resolving the "latest" Supabase
  CLI, before running any test. Fixed by pinning the CLI to 2.121.0 in `ci.yml`.

## 8. Owner actions

1. Review draft PR #1.
2. Run checks A–E with the local stack and fill in "Actual" and "Status" in section 5.
3. If all pass, approve. I then merge PR #1 into `main` (only with your go-ahead) and mark Phase 1
   signed off in HANDOFF.md.

## 9. Recommendation

**Not ready for sign-off.** The automated scope is complete, but the five required real-device checks
are BLOCKED. Phase 1 is ready for sign-off once A–E pass on a phone.
