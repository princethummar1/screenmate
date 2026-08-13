# ScreenMate

An accountability platform where groups of friends compete to reduce daily screen time via automated screenshot verification.

## Projects

### Web Application

To run the Next.js development server locally:

1. Copy `.env.example` to `.env.local` and add your Supabase credentials:
   ```bash
   cp .env.example .env.local
   ```
2. Install dependencies:
   ```bash
   npm install
   ```
3. Run the development server:
   ```bash
   npm run dev
   ```
4. Open [http://localhost:3000](http://localhost:3000) in your browser.

#### 🐳 Running with Docker

You can easily containerize and run ScreenMate using Docker. A `Dockerfile` and `.dockerignore` have been provided.

**1. Build the Docker Image**
From the root of the `screenmate` directory, run:

```bash
docker build -t screenmate-app .
```

**2. Run the Docker Container**
Once built, run the container and expose it on port 3000. Be sure to pass in your Supabase environment variables!

```bash
docker run -p 3000:3000 \
  -e NEXT_PUBLIC_SUPABASE_URL="YOUR_SUPABASE_URL" \
  -e NEXT_PUBLIC_SUPABASE_ANON_KEY="YOUR_ANON_KEY" \
  screenmate-app
```

Now you can open [http://localhost:3000](http://localhost:3000) to see your app running inside a Docker container!

#### 🗄️ Database Setup

The required Supabase database schema and Row Level Security (RLS) policies are located in `supabase/schema.sql`. Execute this SQL script in your Supabase project's SQL Editor to create the necessary tables and permissions.

### Android Companion App

Native Kotlin Android app that automatically tracks screen time and syncs to ScreenMate, plus personal productivity tools.

#### Architecture
- Kotlin + Jetpack Compose + Material 3
- MVVM + Repository pattern
- Room for local database
- WorkManager for background sync
- Supabase Kotlin SDK for backend
- Min SDK 34 (Android 14)

#### Directory Structure
- `android/app/src/main/java/com/screenmate/app/` - Kotlin source files
- `android/app/src/main/res/` - Android resources (layouts, values)

#### Requirements
- Android Studio Ladybug (2024.2.1) or newer
- JDK 17
- Android SDK 35
- Physical Android device (API 34+) for usage data testing

## Local Android Configuration

All API credentials are configured via `local.properties` and exposed through `BuildConfig`.

**1. Create your `local.properties`:**

Copy the example file and fill in your credentials:
```bash
cd android
cp local.properties.example local.properties
```

Edit `local.properties`:
```properties
sdk.dir=C:/Android/Sdk

SUPABASE_URL=https://your-project.supabase.co
SUPABASE_ANON_KEY=your-supabase-anon-key

TMDB_API_KEY=your-tmdb-v3-api-key
TMDB_READ_ACCESS_TOKEN=your-tmdb-read-access-token-jwt

OPENROUTER_API_KEY=sk-or-v1-your-openrouter-key
OPENROUTER_MODEL=google/gemini-2.0-flash-001
```

> **Note:** `local.properties` is automatically ignored by Git. Never commit real credentials.

**2. Supabase Setup**
Run migrations in order in your Supabase SQL editor:
- `supabase/migrations/20260808000001_add_source_column.sql`
- `supabase/migrations/20260808000002_personal_tables.sql`

**3. TMDb Setup**
- Register at [themoviedb.org](https://www.themoviedb.org/settings/api) and obtain both:
  - **API Key (v3 auth)**: 32-character hex string
  - **API Read Access Token (v4 auth)**: Long JWT starting with `eyJ...`
- Enter both in your `local.properties`

**4. OpenRouter Setup**
- Get an API key from [openrouter.ai](https://openrouter.ai)
- Model can be configured (default: `google/gemini-2.0-flash-001`)

**5. Build & Run**
1. Open `android/` directory in Android Studio
2. Sync Gradle
3. Run on device
4. Login with existing ScreenMate account
5. Grant Usage Access permission
6. Go to Settings > ScreenMate Sync > Refresh Rooms to load your rooms
7. Select which rooms to sync to

#### Building APKs
- Debug: `./gradlew assembleDebug`
- Release: `./gradlew assembleRelease` (requires signing config)

#### Known Android Limitations
- UsageStatsManager data retention varies by device (7-30 days)
- WorkManager execution timing is not exact
- Some OEMs restrict background work aggressively

#### Battery Optimization Notes
- Uses PeriodicWorkRequest (6 hour intervals)
- No permanent foreground service
- Network constraint prevents unnecessary wake

#### Troubleshooting
- **Usage Data Empty:** Ensure the 'Usage Access' permission was granted in Android Settings.
- **Background Sync Failing:** Check if the app is heavily battery-restricted by your OEM (Samsung, Xiaomi, etc.).
- **TMDb Connection Testing:** Go to Settings > Diagnostics > "Test TMDb Connection".
  - **HTTP 401 Unauthorized:** Your TMDb API Read Access Token is invalid, expired, or copied incorrectly.
  - **DNS/Host resolution failure:** The app cannot reach `api.themoviedb.org`. Ensure your device is connected to the internet.
  - **Important:** The app uses the official hostname `api.themoviedb.org`. Do NOT hardcode any IP address.
- **Missing Configuration:** If required fields are missing from `local.properties`, the app will log a warning on startup. Check Logcat for "ScreenMate" tag messages.
- **App crashes on launch:** Ensure `local.properties` has at least `SUPABASE_URL` and `SUPABASE_ANON_KEY` set.