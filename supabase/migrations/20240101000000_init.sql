-- Note: We assume auth.users is managed by Supabase Auth automatically.

CREATE TABLE profiles (
  id UUID PRIMARY KEY REFERENCES auth.users(id) ON DELETE CASCADE,
  username TEXT NOT NULL,
  created_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE TABLE rooms (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  name TEXT NOT NULL,
  invite_code VARCHAR(6) UNIQUE NOT NULL,
  goal_minutes INTEGER NOT NULL DEFAULT 180,
  duration_days INTEGER NOT NULL DEFAULT 7,
  reward TEXT,
  notification_time TIME,
  reset_time TIME DEFAULT '00:00:00',
  start_date DATE NOT NULL DEFAULT CURRENT_DATE,
  end_date DATE NOT NULL,
  owner_id UUID REFERENCES profiles(id),
  is_active BOOLEAN DEFAULT TRUE,
  created_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE TABLE room_members (
  room_id UUID REFERENCES rooms(id) ON DELETE CASCADE,
  user_id UUID REFERENCES profiles(id) ON DELETE CASCADE,
  total_points INTEGER DEFAULT 0,
  current_streak INTEGER DEFAULT 0,
  best_streak INTEGER DEFAULT 0,
  joined_at TIMESTAMPTZ DEFAULT NOW(),
  PRIMARY KEY (room_id, user_id)
);

CREATE TABLE daily_logs (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  room_id UUID REFERENCES rooms(id) ON DELETE CASCADE,
  user_id UUID REFERENCES profiles(id) ON DELETE CASCADE,
  screenshot_url TEXT NOT NULL,
  screen_time_minutes INTEGER NOT NULL,
  log_date DATE DEFAULT CURRENT_DATE,
  status VARCHAR(20) DEFAULT 'verified',
  created_at TIMESTAMPTZ DEFAULT NOW(),
  UNIQUE(room_id, user_id, log_date)
);

-- ROW LEVEL SECURITY (RLS) POLICIES

ALTER TABLE rooms ENABLE ROW LEVEL SECURITY;
ALTER TABLE room_members ENABLE ROW LEVEL SECURITY;
ALTER TABLE daily_logs ENABLE ROW LEVEL SECURITY;
ALTER TABLE profiles ENABLE ROW LEVEL SECURITY;

-- rooms: 
CREATE POLICY "Anyone can view rooms" ON rooms FOR SELECT USING (true);
CREATE POLICY "Users can create rooms" ON rooms FOR INSERT WITH CHECK (auth.uid() = owner_id);
CREATE POLICY "Room owners can update their rooms" ON rooms FOR UPDATE USING (auth.uid() = owner_id);

-- room_members: 
CREATE POLICY "Anyone can view room members" ON room_members FOR SELECT USING (true);
CREATE POLICY "Users can join rooms" ON room_members FOR INSERT WITH CHECK (auth.uid() = user_id);
CREATE POLICY "Users can update their own stats" ON room_members FOR UPDATE USING (auth.uid() = user_id);

-- daily_logs:
CREATE POLICY "Users can view logs from their rooms" ON daily_logs FOR SELECT
USING (
  EXISTS (
    SELECT 1 FROM room_members 
    WHERE room_members.room_id = daily_logs.room_id 
    AND room_members.user_id = auth.uid()
  )
);
CREATE POLICY "Users can insert their own logs" ON daily_logs FOR INSERT WITH CHECK (auth.uid() = user_id);

-- profiles:
CREATE POLICY "Anyone can view profiles" ON profiles FOR SELECT USING (true);
CREATE POLICY "Users can update own profile" ON profiles FOR UPDATE USING (auth.uid() = id);

-- TRIGGER FOR NEW USERS
CREATE OR REPLACE FUNCTION public.handle_new_user() 
RETURNS TRIGGER AS $$
BEGIN
  INSERT INTO public.profiles (id, username)
  VALUES (new.id, COALESCE(new.raw_user_meta_data->>'username', split_part(new.email, '@', 1)));
  RETURN new;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

DROP TRIGGER IF EXISTS on_auth_user_created ON auth.users;
CREATE TRIGGER on_auth_user_created
  AFTER INSERT ON auth.users
  FOR EACH ROW EXECUTE PROCEDURE public.handle_new_user();

-- EXPLICIT GRANTS TO INTERNAL ROLES
GRANT ALL ON TABLE public.rooms TO anon, authenticated, service_role;
GRANT ALL ON TABLE public.room_members TO anon, authenticated, service_role;
GRANT ALL ON TABLE public.daily_logs TO anon, authenticated, service_role;
GRANT ALL ON TABLE public.profiles TO anon, authenticated, service_role;