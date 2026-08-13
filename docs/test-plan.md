# ScreenMate Test Plan

This document outlines the testing strategy for the ScreenMate Android application, covering both automatic screen time tracking and personal productivity features.

## 1. Installation & Auth
- **Test 01: Initial Setup & Configuration**
  - **Category:** Manual Device
  - **Prerequisites:** Clean install of app.
  - **Steps:** Launch app, enter Supabase URL and Anon Key.
  - **Expected Results:** Configuration saves successfully and user is presented with login screen.
  - **Verification:** UI confirms configuration.
- **Test 02: User Authentication**
  - **Category:** Manual Device
  - **Prerequisites:** Valid Supabase credentials.
  - **Steps:** Enter email and password, tap Login.
  - **Expected Results:** Login succeeds, session is saved securely, user routed to main dashboard.
  - **Verification:** Dashboard loads user profile data.

## 2. Usage Access
- **Test 03: Permission Prompt**
  - **Category:** Manual Device
  - **Prerequisites:** Logged in, first launch.
  - **Steps:** Observe permission requirement screen.
  - **Expected Results:** App prompts for "Usage Access" permission.
  - **Verification:** User is taken to Android settings.
- **Test 04: Permission Grant Detection**
  - **Category:** Manual Device
  - **Prerequisites:** App waiting for permission.
  - **Steps:** Grant permission in settings and return to app.
  - **Expected Results:** App detects permission grant and proceeds to main functionality.
  - **Verification:** Dashboard unlocks.

## 3. Screen Time Data
- **Test 05: Data Collection (Today)**
  - **Category:** Integration
  - **Prerequisites:** Usage access granted.
  - **Steps:** Trigger local usage data fetch for current day.
  - **Expected Results:** Total screen time and app specific times are calculated correctly.
  - **Verification:** Match with system Digital Wellbeing stats.
- **Test 06: App Label Resolution**
  - **Category:** Unit
  - **Prerequisites:** Mock usage data with package names.
  - **Steps:** Run label resolution process.
  - **Expected Results:** `com.android.chrome` resolves to "Chrome", etc.
  - **Verification:** App list shows human-readable names.
- **Test 07: Unlock Count Tracking**
  - **Category:** Manual Device
  - **Prerequisites:** Background service active.
  - **Steps:** Lock and unlock the device 5 times.
  - **Expected Results:** Unlock count increments by 5.
  - **Verification:** UI updates to show correct count.
- **Test 08: App Open Count Tracking**
  - **Category:** Manual Device
  - **Prerequisites:** Background service active.
  - **Steps:** Open YouTube 3 separate times.
  - **Expected Results:** YouTube open count is 3.
  - **Verification:** App details show correct open count.
- **Test 09: Timezone Handling**
  - **Category:** Unit
  - **Prerequisites:** Usage data spanning midnight.
  - **Steps:** Parse data with specific timezone offset.
  - **Expected Results:** Usage is correctly split across two calendar days based on local timezone.
  - **Verification:** Daily totals are correct.
- **Test 10: Missing Data Handling**
  - **Category:** Integration
  - **Prerequisites:** Query usage for a day with no device activity.
  - **Steps:** Fetch usage data.
  - **Expected Results:** Returns 0 seconds, no crash.
  - **Verification:** UI shows 0h 0m.

## 4. Sync & Background
- **Test 11: Manual Sync to Room**
  - **Category:** Integration
  - **Prerequisites:** Logged in, joined a room.
  - **Steps:** Press "Sync Now" button.
  - **Expected Results:** Usage data is pushed to Supabase `daily_logs` with source='android_auto'.
  - **Verification:** Check Supabase database.
- **Test 12: Background Periodic Sync**
  - **Category:** Integration
  - **Prerequisites:** WorkManager scheduled (6 hour interval).
  - **Steps:** Simulate WorkManager execution.
  - **Expected Results:** Sync runs silently in background.
  - **Verification:** Supabase shows updated timestamp.
- **Test 13: Offline Sync Queuing**
  - **Category:** Manual Device
  - **Prerequisites:** Disable internet.
  - **Steps:** Trigger manual sync.
  - **Expected Results:** Sync fails gracefully, schedules retry.
  - **Verification:** WorkManager shows pending job.
- **Test 14: Room Selection Configuration**
  - **Category:** Integration
  - **Prerequisites:** Multiple active rooms.
  - **Steps:** Select specific rooms to sync to in Settings.
  - **Expected Results:** Sync only targets selected rooms.
  - **Verification:** Unselected rooms do not receive logs.
- **Test 15: Conflict Resolution (Same Day)**
  - **Category:** Integration
  - **Prerequisites:** Existing log for today in database.
  - **Steps:** Sync updated data for today.
  - **Expected Results:** Existing log is updated, not duplicated.
  - **Verification:** Only one log per user per day per room.
- **Test 16: Battery Optimization Constraints**
  - **Category:** Manual Device
  - **Prerequisites:** App in standby bucket.
  - **Steps:** Wait for sync window.
  - **Expected Results:** Sync respects Doze mode and executes in maintenance window.
  - **Verification:** System logs.

## 5. Account Management
- **Test 17: Logout**
  - **Category:** Integration
  - **Prerequisites:** Logged in.
  - **Steps:** Press Logout.
  - **Expected Results:** Local data cleared, WorkManager jobs cancelled, returned to login.
  - **Verification:** Cannot access dashboard.
- **Test 18: Profile Data Fetching**
  - **Category:** Integration
  - **Prerequisites:** Logged in.
  - **Steps:** Navigate to Profile.
  - **Expected Results:** Shows correct username and avatar.
  - **Verification:** Matches web platform data.

## 6. Watchlist & Media
- **Test 19: TMDb Search**
  - **Category:** Integration
  - **Prerequisites:** TMDb API key configured.
  - **Steps:** Search for "Inception".
  - **Expected Results:** Returns movie list with posters.
  - **Verification:** UI displays results.
- **Test 20: Add to Watchlist**
  - **Category:** Integration
  - **Prerequisites:** Search results loaded.
  - **Steps:** Add item to watchlist.
  - **Expected Results:** Saved to `personal_media` table.
  - **Verification:** Appears in personal watchlist.
- **Test 21: Update Watch Status**
  - **Category:** Integration
  - **Prerequisites:** Item in watchlist.
  - **Steps:** Mark as "Watched" and add rating.
  - **Expected Results:** Status and rating update in database.
  - **Verification:** UI reflects changes.

## 7. Journal
- **Test 22: Create Journal Entry**
  - **Category:** Integration
  - **Prerequisites:** None.
  - **Steps:** Write new entry for today, set mood.
  - **Expected Results:** Saved to `personal_journal_entries`.
  - **Verification:** Entry appears in list.
- **Test 23: Edit Journal Entry**
  - **Category:** Integration
  - **Prerequisites:** Existing entry.
  - **Steps:** Modify text and save.
  - **Expected Results:** Database updates, no duplicate created.
  - **Verification:** Updated text shown.
- **Test 24: View Past Entries**
  - **Category:** Integration
  - **Prerequisites:** Multiple entries exist.
  - **Steps:** Scroll through journal history.
  - **Expected Results:** Entries load correctly ordered by date.
  - **Verification:** UI displays correct dates.

## 8. Bookmarks & Wishlist
- **Test 25: Save Bookmark**
  - **Category:** Integration
  - **Prerequisites:** None.
  - **Steps:** Add URL and title.
  - **Expected Results:** Saved to `personal_bookmarks`.
  - **Verification:** Link is clickable in UI.
- **Test 26: Add Wishlist Item**
  - **Category:** Integration
  - **Prerequisites:** None.
  - **Steps:** Add product name, price, URL.
  - **Expected Results:** Saved to `personal_wishlist_items`.
  - **Verification:** UI displays item with price.

## 9. AI Commentary
- **Test 27: Generate Summary**
  - **Category:** Integration
  - **Prerequisites:** OpenRouter API key configured, usage data available.
  - **Steps:** Request AI summary of daily usage.
  - **Expected Results:** Calls LLM and returns insightful text.
  - **Verification:** UI shows text response.
- **Test 28: API Error Handling**
  - **Category:** Integration
  - **Prerequisites:** Invalid OpenRouter key.
  - **Steps:** Request summary.
  - **Expected Results:** Graceful error message displayed.
  - **Verification:** App does not crash.

## 10. Reliability
- **Test 29: Database Migration (Room)**
  - **Category:** Unit
  - **Prerequisites:** Old version of local database.
  - **Steps:** Upgrade to new schema version.
  - **Expected Results:** Data preserved, schema updated.
  - **Verification:** App starts normally.
- **Test 30: State Restoration**
  - **Category:** Manual Device
  - **Prerequisites:** Deep within app navigation.
  - **Steps:** Trigger process death, restore app.
  - **Expected Results:** Returns to same screen with data intact.
  - **Verification:** UI state preserved.
