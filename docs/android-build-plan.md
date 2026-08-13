# Android Build Plan — ScreenMate Companion + Personal Dashboard

Created: 2026-08-08

---

## 1. Existing Architecture Discovered

- **Website**: Next.js 16.2.11 (App Router) with React 19, TypeScript, Tailwind CSS 4
- **Backend**: Supabase (PostgreSQL + Auth + RLS)
- **Auth**: Email/password + Google OAuth via Supabase Auth
- **Screen-time verification**: Client-side OCR (Tesseract.js) → server parsing → `daily_logs` insert
- **Data model**: 4 tables — `profiles`, `rooms`, `room_members`, `daily_logs`
- **Scoring**: +20 base, +100 if under goal, +1/min under goal — computed in `/api/ocr` route
- **Streaks**: Incremented on each submission (no gap detection)
- **Daily uniqueness**: UNIQUE(room_id, user_id, log_date) constraint
- **Logical day**: Based on room's `reset_time`, client calculates the logical date
- **No existing mobile code**: No React Native, no Flutter, no mobile SDK integration exists

---

## 2. Integration Points

### Shared Infrastructure
- Same Supabase project (URL + anon key)
- Same `auth.users` → `profiles` identity
- Same `daily_logs` table for screen-time records
- Same `rooms` / `room_members` for room context

### Android → Website Data Flow
```
Android UsageStatsManager
  → Local aggregation (Room DB)
  → Supabase Edge Function `submit-screen-time`
  → Insert/upsert `daily_logs`
  → Calculate points & update `room_members`
```

### Website ← Data (read-only for Android)
```
Android queries:
  - room_members (to know which rooms user belongs to)
  - rooms (to get goal_minutes, reset_time)
  - daily_logs (to check if already submitted today)
```

---

## 3. Exact Database Changes

### Migration 1: Add source tracking to daily_logs
```sql
ALTER TABLE daily_logs ADD COLUMN source VARCHAR(20) DEFAULT 'screenshot';
COMMENT ON COLUMN daily_logs.source IS 'Origin: screenshot (website OCR), android_auto (Android app)';
```

### Migration 2: Personal data tables
All personal tables follow this pattern:
- `user_id UUID NOT NULL REFERENCES profiles(id) ON DELETE CASCADE`
- `created_at TIMESTAMPTZ DEFAULT NOW()`
- `updated_at TIMESTAMPTZ DEFAULT NOW()`
- RLS enabled with policy: `auth.uid() = user_id`

Tables to create:
- `personal_tasks` — title, description, due_at, priority, completed, category, completed_at
- `personal_habits` — name, icon, frequency, active
- `personal_habit_entries` — habit_id FK, entry_date, completed, note
- `personal_media` — tmdb_id, media_type, title, poster_path, release_year, overview, genres, status (want_to_watch/watching/completed/dropped), priority, rating, notes, started_at, finished_at, review, is_favorite, is_manual
- `personal_reading_items` — title, author, url, type, status, rating, notes, started_at, completed_at
- `personal_playlists` — title, description, image_url, category
- `personal_playlist_items` — playlist_id FK, title, creator, url, platform, notes, position
- `personal_wishlist_categories` — name, position
- `personal_wishlist_items` — name, image_url, expected_price, currency, product_url, store, category_id FK, priority, notes, purchased, purchased_at
- `personal_notes` — title, content, is_pinned
- `personal_journal_entries` — journal_date (UNIQUE per user), title, content, mood, is_favorite
- `personal_bookmark_categories` — name, position
- `personal_bookmarks` — title, url, description, category_id FK, tags, is_favorite
- `device_usage_daily` — usage_date, total_screen_time_seconds, unlock_count, app_open_count, first_usage_at, last_usage_at, timezone, synced_at
- `device_usage_apps` — daily_id FK, package_name, app_label, usage_seconds, open_count

### Migration 3: Edge Functions
- `submit-screen-time` — Authenticated endpoint for Android to submit daily screen time
- `generate-ai-commentary` — Proxy for OpenRouter AI reactions

---

## 4. Android Architecture

### Pattern: MVVM + Clean Architecture + Repository

```
app/
├── core/
│   ├── database/          # Room DB, entities, DAOs, migrations
│   ├── network/           # Supabase client, API services
│   ├── auth/              # AuthRepository, session management
│   ├── sync/              # SyncManager, WorkManager workers
│   ├── ui/                # Theme, shared composables, design system
│   └── utils/             # Date/time utilities, extensions
│
├── features/
│   ├── onboarding/        # Welcome, usage access permission
│   ├── dashboard/         # Home dashboard screen
│   ├── screentime/        # Daily, weekly, monthly reports
│   ├── tasks/             # Task management
│   ├── habits/            # Habit tracker
│   ├── watchlist/         # TMDb watchlist + watch log
│   ├── reading/           # Reading list
│   ├── playlists/         # Playlist manager
│   ├── wishlist/          # Wishlist with categories
│   ├── scratchpad/        # Quick notes
│   ├── journal/           # Daily journal with calendar
│   ├── bookmarks/         # Website bookmark manager
│   └── settings/          # App settings, sync status, about
│
├── usage/
│   ├── collector/         # UsageStatsManager wrapper
│   ├── aggregator/        # Daily aggregation logic
│   ├── sessionizer/       # App session counting algorithm
│   └── sync/              # ScreenMate sync worker
│
├── navigation/            # Navigation graph, drawer
├── di/                    # Manual DI or Hilt modules
└── MainActivity.kt
```

### Key Libraries
- Kotlin 2.0+
- Jetpack Compose (BOM latest stable)
- Material 3
- Room (local DB)
- WorkManager (background sync)
- Supabase Kotlin SDK (gotrue-kt, postgrest-kt, functions-kt)
- Ktor client (HTTP for TMDb, used by Supabase SDK)
- Coil (image loading for posters)
- kotlinx-serialization
- kotlinx-datetime

### Min SDK: 34 (Android 14)
### Target SDK: 35 (Android 15)

---

## 5. Authentication Strategy

1. Use Supabase Kotlin SDK (`GoTrue`)
2. **Email/Password**: `supabase.auth.signInWith(Email) { email = ...; password = ... }`
3. **Google One Tap**: `supabase.auth.signInWith(Google)` via Android Credential Manager
4. **Sign Up**: Include `username` in user metadata to trigger the existing `handle_new_user` trigger
5. **Session Persistence**: Supabase Kotlin SDK handles session refresh automatically; store tokens in EncryptedSharedPreferences
6. **Session Expiry**: SDK auto-refreshes; handle `AuthException` for expired sessions
7. **Logout**: Clear local data, cancel sync workers, sign out from Supabase

---

## 6. Usage Collection Strategy

### Data Sources
- `UsageStatsManager` → `queryUsageStats()` for daily totals
- `UsageStatsManager` → `queryEvents()` for detailed events (sessions, unlocks, first/last usage)
- `UsageEvents.Event.ACTIVITY_RESUMED` / `ACTIVITY_PAUSED` for foreground sessions
- `UsageEvents.Event.KEYGUARD_HIDDEN` for unlock events

### Sessionization Algorithm
1. Query `UsageEvents` for the calendar day (local timezone, midnight-to-midnight)
2. Track per-app foreground state: ACTIVITY_RESUMED starts a session, ACTIVITY_PAUSED ends it
3. If the same app resumes within a **configurable gap threshold** (default: 2 seconds) of the last pause, it's the SAME session (not a new app open)
4. If a DIFFERENT app resumes, the previous app's session ends
5. Count distinct sessions per app = app open count
6. Sum foreground durations per app = per-app screen time
7. Total screen time = sum of all app foreground durations

### Unlock Counting
- Count `KEYGUARD_HIDDEN` events in the day's event range
- Alternative: `KEYGUARD_DONE` if available on device

### First/Last Usage
- First usage: Timestamp of first `KEYGUARD_HIDDEN` or first `ACTIVITY_RESUMED` (whichever is earlier and represents intentional interaction)
- Last usage: Timestamp of last `ACTIVITY_PAUSED` event in the day

### Calendar Day Boundaries
- Use `LocalDate` with device timezone → convert to epoch milliseconds for UsageStatsManager queries
- Never use "24 hours ago" — always use calendar day start/end

---

## 7. ScreenMate Sync Strategy

### Sync Triggers
1. **Periodic WorkManager** — `PeriodicWorkRequest` every 6 hours (battery-conscious)
2. **Daily finalization** — `OneTimeWorkRequest` scheduled via `WorkManager` for after midnight
3. **App launch reconciliation** — On app open, check for unsynced days and backfill
4. **Manual sync** — User taps "Sync Now" in Settings

### Sync Process
1. Check user's room memberships via `room_members` query
2. For each room, check `daily_logs` for existing entries (avoid duplicates)
3. For unsynced days (up to 7 days back), aggregate local usage data
4. Call Edge Function `submit-screen-time` with: `{ screen_time_minutes, log_date, timezone, source: 'android_auto' }`
5. Edge Function handles: upsert daily_log + points/streak calculation
6. Mark local records as synced

### Idempotency
- Edge Function uses `ON CONFLICT (room_id, user_id, log_date) DO UPDATE`
- Local sync state tracks which dates have been successfully synced
- Repeated sync attempts are safe

### Backfill
- Default: 7 days
- On app launch, detect gaps in local `device_usage_daily` table
- Query `UsageStatsManager` for missing dates (Android retains ~7-30 days of history depending on device)
- Aggregate and sync missing days

---

## 8. Personal Data Sync Strategy

### Hybrid Model: Room (local) ↔ Supabase (cloud)

- All personal data stored locally in Room DB first (offline-first)
- Background sync to Supabase for cloud backup
- Each entity has: `sync_status` (SYNCED, PENDING_CREATE, PENDING_UPDATE, PENDING_DELETE)
- On network availability, push pending changes to Supabase
- On fresh install/login, pull user's data from Supabase
- Conflict resolution: Last-write-wins using `updated_at` timestamp

### Sync Worker
- `PeriodicWorkRequest` every 30 minutes with network constraint
- Also triggers on app launch
- Processes pending changes in order
- Handles failures with exponential backoff

---

## 9. Security Strategy

1. **No service role key in Android** — Only anon key (public/client-safe)
2. **RLS enforces data isolation** — All personal tables have `auth.uid() = user_id` policies
3. **OpenRouter key NEVER in APK** — Proxied via Supabase Edge Function
4. **TMDb API key** — Stored in `local.properties` (gitignored), injected via BuildConfig
5. **Supabase credentials** — Stored in `local.properties`, injected via BuildConfig
6. **Session tokens** — EncryptedSharedPreferences
7. **No sensitive data in logs** — ProGuard/R8 strips in release builds

---

## 10. Testing Strategy

### Unit Tests
- Usage aggregation logic (sessionization, unlock counting, day boundaries)
- Date/timezone boundary calculations
- Sync idempotency
- Repository layer tests with fake DAOs
- ViewModel state tests

### Instrumented Tests
- Room DAO operations (insert, query, update, delete)
- Database migrations
- Supabase auth flow (with test credentials)

### Manual Device Tests
- Usage Access permission grant/deny flow
- Background worker execution after reboot
- Offline → online sync recovery
- Day rollover behavior
- TMDb search and media lifecycle
- Journal persistence across app kills

### Backend Tests
- Edge Function authorization
- RLS policy verification
- Upsert/duplicate prevention
- Points calculation accuracy

---

## 11. Files/Modules That Will Be Added

### Website Side (minimal)
- `supabase/migrations/20260808000001_add_source_column.sql`
- `supabase/migrations/20260808000002_personal_tables.sql`
- `supabase/functions/submit-screen-time/index.ts` (Edge Function)
- `supabase/functions/generate-ai-commentary/index.ts` (Edge Function)

### Android Side (new project)
- Full native Android project under `android/` directory
- ~50-70 Kotlin files across core, features, usage, navigation modules
- Room database with ~15 entity classes, ~15 DAOs
- ~12 ViewModel classes
- ~15 Screen composables
- 2-3 WorkManager workers
- Unit test files
- `local.properties.example` for credential configuration

---

## 12. Website Files That Need Modification

### Files NOT modified
- No UI components changed
- No styling changed
- No existing routes changed
- No existing API routes changed
- No package.json changes

### Files potentially modified
- `.gitignore` — Add `android/` build artifacts, `local.properties`
- `README.md` — Add Android section reference

### New files in website project
- `supabase/migrations/` — 2 new migration files
- `supabase/functions/` — 2 Edge Function directories
- `docs/` — Analysis, build plan, test plan, verification report
- `android/` — Entire Android project

---

## Implementation Order

| Phase | Description | Estimated Effort |
|-------|-------------|------------------|
| 0 | ✅ Analyze existing repo | Complete |
| 1 | ✅ Document integration contract | Complete |
| 2 | Create Android project skeleton + auth | High |
| 3 | Usage Access + screen-time collection | High |
| 4 | Room DB + local persistence | Medium |
| 5 | ScreenMate sync + Edge Functions | High |
| 6 | Digital Life UI (daily/weekly/monthly) | Medium |
| 7 | Tasks + Habits | Medium |
| 8 | TMDb Watchlist + Watch Log | High |
| 9 | Reading, Playlists, Wishlist | Medium |
| 10 | Scratchpad, Journal, Bookmarks | Medium |
| 11 | AI commentary via Edge Function | Medium |
| 12 | Polish dark UI | Medium |
| 13 | Testing | High |
| 14 | Documentation | Medium |
| 15 | Build verification | Low |
