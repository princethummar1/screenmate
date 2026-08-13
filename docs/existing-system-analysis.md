# Existing ScreenMate System Analysis

Analysis performed: 2026-08-08

---

## Current Architecture

### Framework & Stack
- **Framework**: Next.js 16.2.11 (App Router)
- **React**: 19.2.4
- **Language**: TypeScript 5.x
- **Styling**: Tailwind CSS 4.x with PostCSS
- **UI Icons**: Lucide React 1.26.0
- **Charts**: Recharts 3.10.0
- **Date Utils**: date-fns 4.4.0
- **OCR**: Tesseract.js 7.0.0 (client-side screenshot text extraction)
- **Image Processing**: Sharp 0.35.3 (server-side, external package)
- **Supabase Client**: @supabase/supabase-js 2.110.8 + @supabase/ssr 0.12.3
- **Deployment**: Docker (Node 20 Alpine), also supports Vercel
- **OCR Training Data**: `eng.traineddata` (Tesseract English, 5.2MB)

### Project Structure
```
screenmate/
├── src/
│   ├── app/
│   │   ├── layout.tsx          # Root layout (Geist fonts)
│   │   ├── page.tsx            # Dashboard — lists user's rooms
│   │   ├── globals.css         # Tailwind + CSS variables
│   │   ├── actions.ts          # Server actions (createRoom, joinRoom)
│   │   ├── login/page.tsx      # Login/signup page (client component)
│   │   ├── join/page.tsx       # Join room via invite code (server component)
│   │   ├── room/[id]/page.tsx  # Room detail page (server component)
│   │   ├── room/[id]/loading.tsx
│   │   ├── auth/callback/route.ts  # OAuth callback handler
│   │   └── api/ocr/route.ts    # OCR processing API route
│   ├── components/
│   │   ├── ActiveRoom.tsx      # Room detail with upload, leaderboard, chart
│   │   ├── Dashboard.tsx       # Static demo dashboard (unused in prod?)
│   │   ├── EmptyState.tsx      # Create/join room form
│   │   ├── HistoryChart.tsx    # 30-day multi-member line chart
│   │   └── ShareModal.tsx      # Invite code sharing modal
│   ├── lib/
│   │   └── supabase/
│   │       ├── client.ts       # Browser Supabase client
│   │       └── server.ts       # Server Supabase client (cookies)
│   └── middleware.ts           # Auth middleware (session refresh, redirects)
├── supabase/
│   └── migrations/
│       └── 20240101000000_init.sql  # Full schema + RLS
├── public/
├── package.json
├── next.config.ts              # serverExternalPackages: sharp, tesseract.js
├── tsconfig.json
├── Dockerfile
├── .env.example
└── .gitignore
```

---

## Existing Authentication

### Methods Supported
1. **Email/Password** — `supabase.auth.signUp()` and `supabase.auth.signInWithPassword()`
2. **Google OAuth** — `supabase.auth.signInWithOAuth({ provider: 'google' })`
3. **OAuth Callback** — `/auth/callback` route exchanges code for session

### Auth Flow
- Login page is a client component (`'use client'`)
- Sign-up passes `username` via `options.data.username` metadata
- OAuth redirects to `{origin}/auth/callback`
- Middleware (`middleware.ts`) checks auth on all routes except `/login`, `/auth`, `/api`, `/join`
- Unauthenticated users on protected routes → redirected to `/login`
- Session managed via cookies (SSR pattern using `@supabase/ssr`)

### Profile Creation
- Automatic via database trigger `on_auth_user_created`
- Creates `profiles` row with `id = auth.users.id`
- Username from `raw_user_meta_data->>'username'` or email prefix fallback

### Key Finding for Android
- **No magic link auth** — only email/password and Google OAuth
- Android app can use `supabase-kt` (Kotlin SDK) with the same auth methods
- Same Supabase project URL and anon key can be shared
- User identity is tied to `auth.users.id` → `profiles.id`

---

## Existing ScreenMate Data Flow

### Screenshot Verification Flow
1. User uploads a screenshot image in the room UI
2. **Client-side OCR**: Tesseract.js runs in-browser to extract text from the screenshot
3. Extracted text is sent to `/api/ocr` as JSON (`{ text, groupId, clientLogicalDate }`)
4. Server parses screen time from text using regex patterns (hours/minutes)
5. Server validates: non-zero time, ≤ 1440 minutes
6. Server compares against room's `goal_minutes`
7. Server inserts a `daily_logs` record
8. Server calculates points and updates `room_members` (streak + points)
9. Page reloads to show updated state

### Important: NO screenshot image is stored
- `screenshot_url` is set to `'placeholder_url'` — the actual image is NOT uploaded to Supabase Storage
- Only the OCR-extracted text is sent to the server
- The system trusts the client-side OCR result

### Logical Day Calculation
- Uses room's `reset_time` (default `00:00:00`)
- If current time < reset_time, the "logical today" is yesterday's date
- Client calculates `clientLogicalDate` and sends it to the server

---

## Existing Database Tables

### `profiles`
| Column | Type | Constraints |
|--------|------|-------------|
| id | UUID | PK, FK → auth.users(id) ON DELETE CASCADE |
| username | TEXT | NOT NULL |
| created_at | TIMESTAMPTZ | DEFAULT NOW() |

### `rooms`
| Column | Type | Default/Constraints |
|--------|------|--------------------|
| id | UUID | PK, gen_random_uuid() |
| name | TEXT | NOT NULL |
| invite_code | VARCHAR(6) | UNIQUE, NOT NULL |
| goal_minutes | INTEGER | DEFAULT 180 |
| duration_days | INTEGER | DEFAULT 7 |
| reward | TEXT | nullable |
| notification_time | TIME | nullable |
| reset_time | TIME | DEFAULT '00:00:00' |
| start_date | DATE | DEFAULT CURRENT_DATE |
| end_date | DATE | NOT NULL |
| owner_id | UUID | FK → profiles(id) |
| is_active | BOOLEAN | DEFAULT TRUE |
| created_at | TIMESTAMPTZ | DEFAULT NOW() |

### `room_members`
| Column | Type | Default/Constraints |
|--------|------|--------------------|
| room_id | UUID | PK (composite), FK → rooms(id) CASCADE |
| user_id | UUID | PK (composite), FK → profiles(id) CASCADE |
| total_points | INTEGER | DEFAULT 0 |
| current_streak | INTEGER | DEFAULT 0 |
| best_streak | INTEGER | DEFAULT 0 |
| joined_at | TIMESTAMPTZ | DEFAULT NOW() |

### `daily_logs`
| Column | Type | Default/Constraints |
|--------|------|--------------------|
| id | UUID | PK, gen_random_uuid() |
| room_id | UUID | FK → rooms(id) CASCADE |
| user_id | UUID | FK → profiles(id) CASCADE |
| screenshot_url | TEXT | NOT NULL (but stores 'placeholder_url') |
| screen_time_minutes | INTEGER | NOT NULL |
| log_date | DATE | DEFAULT CURRENT_DATE |
| status | VARCHAR(20) | DEFAULT 'verified' |
| created_at | TIMESTAMPTZ | DEFAULT NOW() |
| | | UNIQUE(room_id, user_id, log_date) |

---

## Existing Challenge System

### Room Concept
- Users create "challenge rooms" with a name, goal, and duration
- Rooms have 6-character invite codes (alphanumeric)
- Members join via invite code or direct link (`/join?code=XXXXXX`)
- Maximum 3 rooms per user
- Duration options: 7, 14, or 30 days
- Room has a start_date and calculated end_date
- Rooms can have optional reward/stakes text
- Optional notification_time and reset_time per room

### Daily Flow
- Each day, members upload a screenshot of their device's screen time
- OCR extracts total minutes
- One log per user per room per day (UNIQUE constraint)
- Status: `'verified'` (under goal) or `'over_goal'`

---

## Existing Scoring/Streak Logic

### Points (calculated in `/api/ocr`)
- **Base points**: Always +20 for submitting
- **Under goal bonus**: +100 if `screen_time_minutes ≤ goal_minutes`
- **Overachiever**: +1 per minute under goal
- **Formula**: `points = totalMinutes <= goalMinutes ? 100 + (goalMinutes - totalMinutes) : 20`

### Streaks
- `current_streak` incremented by 1 on every submission (regardless of over/under)
- `best_streak` = max of current best and new streak
- **Note**: Streak is NOT reset when over goal — it only tracks consecutive days of submission
- **Note**: No gap detection — streak always increments on submission without checking if yesterday was submitted

### Leaderboard
- Members sorted by `total_points` descending
- Displays: rank, username, points, current streak, today's status, total lifetime wasted minutes
- Visual indicators: 🥇🥈🥉 for top 3

---

## Existing API/Backend Interfaces

### Server Actions (`src/app/actions.ts`)
1. **`createRoom(formData)`** — Creates room + adds creator as member
2. **`joinRoom(inviteCode)`** — Finds room by code + adds user as member

### API Routes
1. **`POST /api/ocr`** — Processes OCR text, creates daily_log, updates points/streaks
   - Request body: `{ text: string, groupId: string, clientLogicalDate: string }`
   - Response: `{ success: boolean, data: { textExtracted, extractedDate, totalMinutes, hours, minutes } }`

### Auth Routes
1. **`GET /auth/callback`** — OAuth code exchange

### RLS Policies
- **rooms**: Anyone can SELECT; owners INSERT/UPDATE
- **room_members**: Anyone can SELECT; users INSERT/UPDATE own rows
- **daily_logs**: Members of the room can SELECT; users INSERT own rows
- **profiles**: Anyone can SELECT; users UPDATE own row
- **Grants**: ALL on all tables to anon, authenticated, service_role

---

## What Can Be Reused

1. **Supabase project** — Same project URL, anon key, auth system
2. **User identity** — Same `auth.users` / `profiles` table
3. **Authentication methods** — Email/password + Google OAuth work in Android SDK
4. **`daily_logs` table** — Android can INSERT screen time data directly
5. **Points/streak logic** — Exists in `/api/ocr` route; Android could either:
   - Call the same API endpoint (but it's designed for OCR text, not raw minutes), OR
   - Insert `daily_logs` directly and let a Supabase Edge Function handle scoring, OR
   - Create a new lightweight API endpoint that accepts raw minutes
6. **RLS policies** — Already allow authenticated users to insert their own logs

---

## What Must Be Added

### For ScreenMate Integration
1. **New API endpoint or Edge Function** that accepts raw screen-time minutes from Android (the existing `/api/ocr` expects OCR text, not structured data)
2. **Alternatively**: Android can directly insert into `daily_logs` via Supabase client and call a separate function to update points/streaks
3. **Source tracking**: Add a `source` column to `daily_logs` to distinguish `'screenshot'` vs `'android_auto'`
4. **Potential migration**: Add `source VARCHAR(20) DEFAULT 'screenshot'` to `daily_logs`

### For Android Personal Features (New Tables)
- `personal_tasks`
- `personal_habits` + `personal_habit_entries`
- `personal_media` (watchlist + watch log unified)
- `personal_reading_items`
- `personal_playlists` + `personal_playlist_items`
- `personal_wishlist_items`
- `personal_categories` (shared category system)
- `personal_notes` (scratchpad)
- `personal_journal_entries`
- `personal_bookmarks` + `personal_bookmark_categories`
- `device_usage_daily` (local aggregated daily stats for Android)
- `device_usage_apps` (per-app daily breakdown)

### For AI Commentary
- Supabase Edge Function to proxy OpenRouter calls securely
- Environment variable: `OPENROUTER_API_KEY`, `OPENROUTER_MODEL`

---

## Risks Discovered

### Critical
1. **`.env` committed to repo** — Contains real Supabase URL, anon key, AND service role key on line 3. The `.gitignore` has `.env*` but the file exists in the working directory. The service role key should be rotated.
2. **Auth check commented out** — In `/api/ocr`, the unauthorized return is commented out (`// return NextResponse.json(...)`) allowing unauthenticated OCR submissions.
3. **Overly broad grants** — `GRANT ALL ON TABLE ... TO anon` allows anonymous users to modify all tables.
4. **No screenshot storage** — `screenshot_url` is always `'placeholder_url'`, so screenshot verification is essentially trust-based.

### Moderate
5. **Streak logic has no gap detection** — Submitting after missing 3 days doesn't reset the streak.
6. **`daily_logs` UNIQUE constraint is per room** — UNIQUE(room_id, user_id, log_date). Android needs to know which room(s) to submit for.
7. **No storage buckets configured** — Despite `screenshot_url` field existing, no Supabase Storage is set up.
8. **`Dashboard.tsx` appears unused** — Static demo component with hardcoded data, not imported anywhere in the active flow.

### Low
9. **No rate limiting** on OCR endpoint.
10. **No input validation** on room name length, invite code format etc.
11. **README contains what appears to be a password/key** on line 51: `lYkrEGBuTSGP54pa`.

---

## Proposed Android Integration

### Authentication Strategy
- Use `io.github.jan-tennert.supabase:gotrue-kt` (Supabase Kotlin SDK)
- Support email/password sign-in
- Support Google One Tap sign-in (same Google OAuth provider)
- Share the same `SUPABASE_URL` and `SUPABASE_ANON_KEY`
- Store session tokens securely in Android EncryptedSharedPreferences

### Data Sync Strategy
- Android collects usage data via `UsageStatsManager`
- Aggregates daily totals locally in Room database
- Syncs to `daily_logs` for each room the user is a member of
- Requires knowing the user's room memberships → query `room_members` table
- Uses upsert with UNIQUE(room_id, user_id, log_date) for idempotency
- Points/streaks need server-side calculation → **create a Supabase Edge Function** `submit-screen-time` that:
  1. Accepts `{ screen_time_minutes, log_date, source }` from Android
  2. Looks up user's rooms
  3. For each room: inserts/upserts daily_log, calculates points, updates streaks
  4. Returns success/failure

### Minimal Database Changes
1. **Add `source` column to `daily_logs`**: `ALTER TABLE daily_logs ADD COLUMN source VARCHAR(20) DEFAULT 'screenshot';`
2. **Create new personal data tables** (all with `user_id UUID REFERENCES profiles(id)` + RLS)
3. **Create Edge Function** for Android screen-time submission
4. **Create Edge Function** for AI commentary proxy (OpenRouter)

### What the Website Does NOT Need to Change
- No UI changes
- No styling changes
- No component changes
- The existing screenshot-upload flow continues working as-is
- The existing `/api/ocr` route stays untouched
- Existing scoring logic in `/api/ocr` continues for website users
- The Edge Function handles scoring separately for Android submissions
