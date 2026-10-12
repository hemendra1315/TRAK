# ADR 0001 — Toolchain, versions and Phase 1 decisions

- **Status:** Accepted (versions provisional until the build spike passes in CI)
- **Date:** 2026-10-12
- **Plan:** "TRAK — Implementation Plan (Phase 1)", sections 2, 3 and 10

## Context

TRAK is a local-first Android app (Kotlin, Compose, Room, Hilt, WorkManager, Supabase).
Versions must be verified and must be shown to build together, not just listed.

The authoring workspace cannot reach Google Maven, Maven Central or the Gradle distribution
server (egress proxy returns 403), and has no running Docker. **All builds and tests run on
GitHub Actions** (decision E1), and this ADR records results only from CI logs.

## Versions (read from official release pages on 2026-10-12)

| Component | Version | Source |
| --- | --- | --- |
| Android Gradle Plugin | 9.4.0 (requires Gradle ≥ 9.6.0, JDK 17, max API 37; built-in Kotlin) | developer.android.com/build/releases/agp-9-4-0-release-notes |
| Gradle | 9.6.0 | AGP 9.4 compatibility table |
| Kotlin (KGP, Compose and serialization plugins) | 2.4.21 (2026-10-08) | kotlinlang.org/docs/releases.html |
| KSP | 2.3.11 (2026-08-03) | github.com/google/ksp/releases |
| Dagger/Hilt | 2.60.1 (2026-07-06) | github.com/google/dagger/releases |
| Compose BOM | 2026.09.00 | developer.android.com/develop/ui/compose/bom/bom-mapping |
| Room | 2.8.5 (2026-09-09) | developer.android.com/jetpack/androidx/versions |
| DataStore | 1.2.1 | same |
| Activity / Lifecycle / Core KTX | 1.13.0 / 2.11.0 / 1.19.1 | same |
| supabase-kt (BOM) | 3.8.0 (2026-08-26) | github.com/supabase-community/supabase-kt/releases |
| Ktor | 3.5.1 | supabase-kt 3.8.0 `gradle/libs.versions.toml` |
| kotlinx-serialization / coroutines | 1.11.0 / 1.11.0 | same |
| SDK levels | minSdk 26, targetSdk 36, compileSdk 36 | Play target-API requirement (since 2026-08-31); supabase-kt documented minSdk 26 |

supabase-kt 3.8.0 itself is built with Kotlin 2.4.0 (its catalog), so consuming it from 2.4.21 is
forward-compatible.

## Decisions

| ID | Decision |
| --- | --- |
| D1 | Email OTP (6-digit code) only in Phase 1. |
| D2 | Near-black neutral surfaces, single teal accent #2DD4BF; Ground Mode light theme. |
| D3 | Pricing open; no billing code. |
| D4 | Room 2.8.5, not Room 3.0.x (Room 3 reached stable 2026-07-01 and its 3.0.2/3.0.3 patches fixed transaction deadlock/regression bugs). DAOs stay suspend/Flow-only. |
| D5 | Adult-only pilot ("I am 18 or older"). |
| D6 | Supabase region ap-south-1 when a hosted project is approved. |
| D7 | Week start user-selectable, default Monday. |
| D8 | `app.trak.android` — provisional application ID. |
| E1 | GitHub Actions is the build and test environment. |
| E2 | New repository `hemendra1315/TRAK`; existing `cricket-progress` is an unrelated Expo app. |
| E3 | No hosted Supabase project or paid resource without explicit owner approval; DB tests run on the local Supabase stack in CI. |
| E4 | Built-in Supabase email for development; custom SMTP before pilot. |
| E5 | `journal_days` consolidation; journal is the single daily editor over structured tables. |
| E6 | Recovery tab: today + previous 7 days; long-term graphs only in Progress. |
| E7 | Optional hydration ml plus a separate reflection. |
| E8 | Try Kotlin 2.4.21 first; fall back to 2.3.21 only on a recorded failure. |
| S1 | Sign-out never silently discards unsynced data. |

## Build spike evidence (GitHub Actions, 2026-10-12)

Only results read from CI check-run annotations are recorded here. Run IDs refer to
`hemendra1315/TRAK` Actions runs.

| Check | Result | Evidence |
| --- | --- | --- |
| `assembleDebug` with AGP 9.4.0, Gradle 9.6.0, JDK 17.0.20.1 | **Pass** (after compileSdk 37, below) | runs 38280291948 and later |
| Unit tests (`:core:model`, `:core:data`) | **Pass: 6 tests, 0 failures** | run 38280291948 |
| KGP used by AGP built-in Kotlin | **2.4.21** (AGP requests 2.2.10, resolved up to 2.4.21 from the root plugin declarations) | `buildEnvironment` annotation |
| KSP | 2.3.11 (AGP's 2.2.10-2.0.2 request resolved to 2.3.11) | same |
| Hilt + KSP code generation | **Pass** (`@HiltAndroidApp`, `@AndroidEntryPoint` compile) | `assembleDebug` |
| Compose BOM 2026.09.00 resolution | compose-ui **1.12.1**, material3 **1.4.0** | runtime classpath annotation |
| Room 2.8.5 + KSP, schema export v1 | **Pass**; `core/data/schemas/app.trak.core.data.db.TrakDatabase/1.json`, identityHash `04b023f195aa03c92d7bac0e97653fc7` | schema annotation, CI committed-schema check |
| supabase-kt 3.8.0 + Ktor 3.5.1 client construction (JVM) | **Pass** with TRAK-supplied `SessionManager` and `CodeVerifierCache` | `SupabaseProviderTest` |
| Other resolved runtime versions | kotlin-stdlib 2.4.21, coroutines 1.11.0, serialization-json 1.11.0, room-runtime 2.8.5, hilt-android 2.60.1, auth-kt 3.8.0, ktor-client-core 3.5.1 | runtime classpath annotation |
| Kotlin 2.3.21 fallback (E8) | **Not used.** No Kotlin compatibility failure occurred. | |

### Failures found and how they were resolved

1. **compileSdk 36 rejected** (`:core:designsystem:checkDebugAarMetadata`): Compose 1.12.1 artifacts
   (`androidx.compose.ui:ui-android:1.12.1` and 10 others) require compileSdk >= 37.
   **Resolution:** compileSdk 37 (supported by AGP 9.4, max API 37); targetSdk stays 36; minSdk 26.
2. **supabase-kt default storage fails off-device:** "Failed to create default settings for
   SettingsSessionManager". In 3.8.0, `AuthConfig.sessionManager` and `codeVerifierCache` are
   nullable and fall back to Settings-based defaults.
   **Resolution:** TRAK always supplies both. This confirms review fix B4: session storage is TRAK-owned
   (Phase 1 step 7 adds the app-private, backup-excluded implementation).
3. **SessionManager contract:** `loadSession(): UserSession` is non-null and must throw
   `NoSessionFoundException` when empty (`loadSessionOrNull()` is provided by default).
   TRAK's implementation must follow this.

### Notes

- `gradle --version` reports "Kotlin: 2.3.21". That is Gradle's embedded Kotlin for build scripts,
  not the project compiler (KGP 2.4.21 above).
- Actions annotations flagged deprecated Node 20 actions and that `ubuntu-latest` moves to Ubuntu 26
  from 2026-10-19; `checkout` and `setup-java` are already on v5.
