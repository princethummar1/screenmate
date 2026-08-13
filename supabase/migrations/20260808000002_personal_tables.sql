-- ============================================
-- PERSONAL DATA TABLES FOR ANDROID APP
-- ============================================
-- These tables store personal productivity data
-- separate from the existing ScreenMate challenge system.
-- All tables have RLS policies restricting access to the owning user.

-- Device Usage Daily
CREATE TABLE IF NOT EXISTS device_usage_daily (
    id BIGSERIAL PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES profiles(id) ON DELETE CASCADE,
    usage_date DATE NOT NULL,
    total_screen_time_seconds BIGINT NOT NULL DEFAULT 0,
    unlock_count INTEGER NOT NULL DEFAULT 0,
    app_open_count INTEGER NOT NULL DEFAULT 0,
    first_usage_at TIMESTAMPTZ,
    last_usage_at TIMESTAMPTZ,
    timezone TEXT,
    synced_at TIMESTAMPTZ DEFAULT NOW(),
    UNIQUE(user_id, usage_date)
);

CREATE TABLE IF NOT EXISTS device_usage_apps (
    id BIGSERIAL PRIMARY KEY,
    daily_id BIGINT NOT NULL REFERENCES device_usage_daily(id) ON DELETE CASCADE,
    package_name TEXT NOT NULL,
    app_label TEXT NOT NULL,
    usage_seconds BIGINT NOT NULL DEFAULT 0,
    open_count INTEGER NOT NULL DEFAULT 0
);

-- Personal Tasks
CREATE TABLE IF NOT EXISTS personal_tasks (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES profiles(id) ON DELETE CASCADE,
    title TEXT NOT NULL,
    description TEXT,
    due_at TIMESTAMPTZ,
    priority INTEGER NOT NULL DEFAULT 0,
    completed BOOLEAN NOT NULL DEFAULT FALSE,
    category TEXT,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    completed_at TIMESTAMPTZ,
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

-- Personal Habits
CREATE TABLE IF NOT EXISTS personal_habits (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES profiles(id) ON DELETE CASCADE,
    name TEXT NOT NULL,
    icon TEXT,
    frequency TEXT NOT NULL DEFAULT 'daily',
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS personal_habit_entries (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    habit_id UUID NOT NULL REFERENCES personal_habits(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES profiles(id) ON DELETE CASCADE,
    entry_date DATE NOT NULL,
    completed BOOLEAN NOT NULL DEFAULT FALSE,
    note TEXT,
    UNIQUE(habit_id, entry_date)
);

-- Personal Media (Watchlist + Watch Log unified)
CREATE TABLE IF NOT EXISTS personal_media (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES profiles(id) ON DELETE CASCADE,
    tmdb_id INTEGER,
    media_type TEXT NOT NULL DEFAULT 'movie',
    title TEXT NOT NULL,
    poster_path TEXT,
    release_year INTEGER,
    overview TEXT,
    genres TEXT,
    status TEXT NOT NULL DEFAULT 'want_to_watch',
    priority INTEGER NOT NULL DEFAULT 0,
    rating REAL,
    notes TEXT,
    review TEXT,
    started_at TIMESTAMPTZ,
    finished_at TIMESTAMPTZ,
    is_favorite BOOLEAN NOT NULL DEFAULT FALSE,
    is_manual BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

-- Personal Reading Items
CREATE TABLE IF NOT EXISTS personal_reading_items (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES profiles(id) ON DELETE CASCADE,
    title TEXT NOT NULL,
    author TEXT,
    url TEXT,
    type TEXT NOT NULL DEFAULT 'book',
    status TEXT NOT NULL DEFAULT 'to_read',
    rating REAL,
    notes TEXT,
    started_at TIMESTAMPTZ,
    completed_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

-- Personal Playlists
CREATE TABLE IF NOT EXISTS personal_playlists (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES profiles(id) ON DELETE CASCADE,
    title TEXT NOT NULL,
    description TEXT,
    image_url TEXT,
    category TEXT,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS personal_playlist_items (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    playlist_id UUID NOT NULL REFERENCES personal_playlists(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES profiles(id) ON DELETE CASCADE,
    title TEXT NOT NULL,
    creator TEXT,
    url TEXT,
    platform TEXT NOT NULL DEFAULT 'other',
    notes TEXT,
    position INTEGER NOT NULL DEFAULT 0
);

-- Wishlist Categories
CREATE TABLE IF NOT EXISTS personal_wishlist_categories (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES profiles(id) ON DELETE CASCADE,
    name TEXT NOT NULL,
    position INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- Wishlist Items
CREATE TABLE IF NOT EXISTS personal_wishlist_items (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES profiles(id) ON DELETE CASCADE,
    name TEXT NOT NULL,
    image_url TEXT,
    expected_price NUMERIC,
    currency TEXT DEFAULT 'INR',
    product_url TEXT,
    store TEXT,
    category_id UUID REFERENCES personal_wishlist_categories(id) ON DELETE SET NULL,
    priority INTEGER NOT NULL DEFAULT 1,
    notes TEXT,
    purchased BOOLEAN NOT NULL DEFAULT FALSE,
    purchased_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

-- Personal Notes (Scratchpad)
CREATE TABLE IF NOT EXISTS personal_notes (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES profiles(id) ON DELETE CASCADE,
    title TEXT,
    content TEXT NOT NULL DEFAULT '',
    is_pinned BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

-- Personal Journal Entries
CREATE TABLE IF NOT EXISTS personal_journal_entries (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES profiles(id) ON DELETE CASCADE,
    journal_date DATE NOT NULL,
    title TEXT,
    content TEXT NOT NULL DEFAULT '',
    mood TEXT,
    is_favorite BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW(),
    UNIQUE(user_id, journal_date)
);

-- Bookmark Categories
CREATE TABLE IF NOT EXISTS personal_bookmark_categories (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES profiles(id) ON DELETE CASCADE,
    name TEXT NOT NULL,
    position INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- Bookmarks
CREATE TABLE IF NOT EXISTS personal_bookmarks (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES profiles(id) ON DELETE CASCADE,
    title TEXT NOT NULL,
    url TEXT NOT NULL,
    description TEXT,
    category_id UUID REFERENCES personal_bookmark_categories(id) ON DELETE SET NULL,
    tags TEXT,
    is_favorite BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

-- ============================================
-- ROW LEVEL SECURITY POLICIES
-- ============================================

ALTER TABLE device_usage_daily ENABLE ROW LEVEL SECURITY;
ALTER TABLE device_usage_apps ENABLE ROW LEVEL SECURITY;
ALTER TABLE personal_tasks ENABLE ROW LEVEL SECURITY;
ALTER TABLE personal_habits ENABLE ROW LEVEL SECURITY;
ALTER TABLE personal_habit_entries ENABLE ROW LEVEL SECURITY;
ALTER TABLE personal_media ENABLE ROW LEVEL SECURITY;
ALTER TABLE personal_reading_items ENABLE ROW LEVEL SECURITY;
ALTER TABLE personal_playlists ENABLE ROW LEVEL SECURITY;
ALTER TABLE personal_playlist_items ENABLE ROW LEVEL SECURITY;
ALTER TABLE personal_wishlist_categories ENABLE ROW LEVEL SECURITY;
ALTER TABLE personal_wishlist_items ENABLE ROW LEVEL SECURITY;
ALTER TABLE personal_notes ENABLE ROW LEVEL SECURITY;
ALTER TABLE personal_journal_entries ENABLE ROW LEVEL SECURITY;
ALTER TABLE personal_bookmark_categories ENABLE ROW LEVEL SECURITY;
ALTER TABLE personal_bookmarks ENABLE ROW LEVEL SECURITY;

-- Helper function for common policy pattern
-- Each user can only access their own rows

-- device_usage_daily
CREATE POLICY "Users manage own usage daily" ON device_usage_daily FOR ALL USING (auth.uid() = user_id) WITH CHECK (auth.uid() = user_id);

-- device_usage_apps (access via daily_id join)
CREATE POLICY "Users manage own usage apps" ON device_usage_apps FOR ALL
    USING (EXISTS (SELECT 1 FROM device_usage_daily d WHERE d.id = device_usage_apps.daily_id AND d.user_id = auth.uid()))
    WITH CHECK (EXISTS (SELECT 1 FROM device_usage_daily d WHERE d.id = device_usage_apps.daily_id AND d.user_id = auth.uid()));

-- All personal tables: same pattern
CREATE POLICY "Users manage own tasks" ON personal_tasks FOR ALL USING (auth.uid() = user_id) WITH CHECK (auth.uid() = user_id);
CREATE POLICY "Users manage own habits" ON personal_habits FOR ALL USING (auth.uid() = user_id) WITH CHECK (auth.uid() = user_id);
CREATE POLICY "Users manage own habit entries" ON personal_habit_entries FOR ALL USING (auth.uid() = user_id) WITH CHECK (auth.uid() = user_id);
CREATE POLICY "Users manage own media" ON personal_media FOR ALL USING (auth.uid() = user_id) WITH CHECK (auth.uid() = user_id);
CREATE POLICY "Users manage own reading" ON personal_reading_items FOR ALL USING (auth.uid() = user_id) WITH CHECK (auth.uid() = user_id);
CREATE POLICY "Users manage own playlists" ON personal_playlists FOR ALL USING (auth.uid() = user_id) WITH CHECK (auth.uid() = user_id);
CREATE POLICY "Users manage own playlist items" ON personal_playlist_items FOR ALL USING (auth.uid() = user_id) WITH CHECK (auth.uid() = user_id);
CREATE POLICY "Users manage own wishlist categories" ON personal_wishlist_categories FOR ALL USING (auth.uid() = user_id) WITH CHECK (auth.uid() = user_id);
CREATE POLICY "Users manage own wishlist items" ON personal_wishlist_items FOR ALL USING (auth.uid() = user_id) WITH CHECK (auth.uid() = user_id);
CREATE POLICY "Users manage own notes" ON personal_notes FOR ALL USING (auth.uid() = user_id) WITH CHECK (auth.uid() = user_id);
CREATE POLICY "Users manage own journal" ON personal_journal_entries FOR ALL USING (auth.uid() = user_id) WITH CHECK (auth.uid() = user_id);
CREATE POLICY "Users manage own bookmark categories" ON personal_bookmark_categories FOR ALL USING (auth.uid() = user_id) WITH CHECK (auth.uid() = user_id);
CREATE POLICY "Users manage own bookmarks" ON personal_bookmarks FOR ALL USING (auth.uid() = user_id) WITH CHECK (auth.uid() = user_id);

-- Grants
GRANT ALL ON TABLE device_usage_daily TO authenticated;
GRANT ALL ON TABLE device_usage_apps TO authenticated;
GRANT ALL ON TABLE personal_tasks TO authenticated;
GRANT ALL ON TABLE personal_habits TO authenticated;
GRANT ALL ON TABLE personal_habit_entries TO authenticated;
GRANT ALL ON TABLE personal_media TO authenticated;
GRANT ALL ON TABLE personal_reading_items TO authenticated;
GRANT ALL ON TABLE personal_playlists TO authenticated;
GRANT ALL ON TABLE personal_playlist_items TO authenticated;
GRANT ALL ON TABLE personal_wishlist_categories TO authenticated;
GRANT ALL ON TABLE personal_wishlist_items TO authenticated;
GRANT ALL ON TABLE personal_notes TO authenticated;
GRANT ALL ON TABLE personal_journal_entries TO authenticated;
GRANT ALL ON TABLE personal_bookmark_categories TO authenticated;
GRANT ALL ON TABLE personal_bookmarks TO authenticated;

GRANT USAGE, SELECT ON SEQUENCE device_usage_daily_id_seq TO authenticated;
GRANT USAGE, SELECT ON SEQUENCE device_usage_apps_id_seq TO authenticated;
