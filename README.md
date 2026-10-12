# TRAK

**Track the work. See the progress.**

An Android-first personal cricket training, workload, gym and recovery journal. It is local-first:
Room is the source of truth, and Supabase (Auth and Postgres with Row Level Security) is a sync target.

> Status: Phase 1 build spike. See `docs/decisions/0001-toolchain-and-versions.md`.

## Requirements

- JDK 17
- Android SDK with platform 36 (Android Studio recent stable)
- For database tests: Docker and the Supabase CLI

## Setup

1. Copy `local.properties.example` to `local.properties` and fill in `sdk.dir`.
2. Supabase values are optional for building. When a project exists, add `SUPABASE_URL` and the
   **publishable** key. Never put a service-role or secret key in this repository or the app.

## Commands

```bash
./gradlew assembleDebug   # build the debug APK
./gradlew test            # JVM unit tests
./gradlew lint
```

CI (`.github/workflows/ci.yml`) runs the same commands on every push and pull request.

## Modules

| Module | Purpose |
| --- | --- |
| `:app` | Navigation, screens, ViewModels, Hilt entry points |
| `:core:designsystem` | Theme tokens (Dark and Ground Mode) and shared Compose components |
| `:core:model` | Pure Kotlin domain types and validation rules |
| `:core:data` | Room, DataStore, repositories, and the Supabase client (the only module that imports it) |
