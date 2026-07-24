-- Schema for ScreenMate

CREATE TABLE users (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  name TEXT NOT NULL,
  email TEXT UNIQUE NOT NULL,
  avatar_url TEXT,
  created_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE TABLE groups (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  name TEXT NOT NULL,
  invite_code VARCHAR(6) UNIQUE NOT NULL,
  goal_minutes INTEGER DEFAULT 180,
  owner_id UUID REFERENCES users(id),
  created_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE TABLE group_members (
  group_id UUID REFERENCES groups(id) ON DELETE CASCADE,
  user_id UUID REFERENCES users(id) ON DELETE CASCADE,
  joined_at TIMESTAMPTZ DEFAULT NOW(),
  PRIMARY KEY (group_id, user_id)
);

CREATE TABLE uploads (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id UUID REFERENCES users(id) ON DELETE CASCADE,
  group_id UUID REFERENCES groups(id) ON DELETE CASCADE,
  image_url TEXT NOT NULL,
  screen_time_minutes INTEGER NOT NULL,
  upload_date DATE DEFAULT CURRENT_DATE,
  verified BOOLEAN DEFAULT TRUE,
  created_at TIMESTAMPTZ DEFAULT NOW(),
  UNIQUE(user_id, group_id, upload_date)
);

CREATE TABLE points (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id UUID REFERENCES users(id) ON DELETE CASCADE,
  group_id UUID REFERENCES groups(id) ON DELETE CASCADE,
  points_earned INTEGER NOT NULL,
  upload_date DATE DEFAULT CURRENT_DATE,
  reason TEXT
);

CREATE TABLE streaks (
  user_id UUID REFERENCES users(id) ON DELETE CASCADE,
  group_id UUID REFERENCES groups(id) ON DELETE CASCADE,
  current_streak INTEGER DEFAULT 0,
  best_streak INTEGER DEFAULT 0,
  last_upload_date DATE,
  PRIMARY KEY (user_id, group_id)
);

-- ROW LEVEL SECURITY (RLS) POLICIES

-- Enable RLS on all tables
ALTER TABLE users ENABLE ROW LEVEL SECURITY;
ALTER TABLE groups ENABLE ROW LEVEL SECURITY;
ALTER TABLE group_members ENABLE ROW LEVEL SECURITY;
ALTER TABLE uploads ENABLE ROW LEVEL SECURITY;
ALTER TABLE points ENABLE ROW LEVEL SECURITY;
ALTER TABLE streaks ENABLE ROW LEVEL SECURITY;

-- users: Users can read their own data and update it.
CREATE POLICY "Users can view their own profile" ON users FOR SELECT USING (auth.uid() = id);
CREATE POLICY "Users can update their own profile" ON users FOR UPDATE USING (auth.uid() = id);

-- groups: Users can view groups they are a member of.
CREATE POLICY "Users can view groups they belong to" ON groups FOR SELECT
USING (
  EXISTS (
    SELECT 1 FROM group_members 
    WHERE group_members.group_id = groups.id 
    AND group_members.user_id = auth.uid()
  )
);
CREATE POLICY "Users can create groups" ON groups FOR INSERT WITH CHECK (auth.uid() = owner_id);
CREATE POLICY "Group owners can update their groups" ON groups FOR UPDATE USING (auth.uid() = owner_id);

-- group_members: Users can view members of groups they belong to.
CREATE POLICY "Users can view group members of their groups" ON group_members FOR SELECT
USING (
  EXISTS (
    SELECT 1 FROM group_members AS my_memberships
    WHERE my_memberships.group_id = group_members.group_id
    AND my_memberships.user_id = auth.uid()
  )
);
CREATE POLICY "Users can join groups" ON group_members FOR INSERT WITH CHECK (auth.uid() = user_id);

-- uploads: Users can view uploads for their groups.
CREATE POLICY "Users can view uploads from their groups" ON uploads FOR SELECT
USING (
  EXISTS (
    SELECT 1 FROM group_members 
    WHERE group_members.group_id = uploads.group_id 
    AND group_members.user_id = auth.uid()
  )
);
CREATE POLICY "Users can insert their own uploads" ON uploads FOR INSERT WITH CHECK (auth.uid() = user_id);

-- points: Users can view points for their groups.
CREATE POLICY "Users can view points from their groups" ON points FOR SELECT
USING (
  EXISTS (
    SELECT 1 FROM group_members 
    WHERE group_members.group_id = points.group_id 
    AND group_members.user_id = auth.uid()
  )
);

-- streaks: Users can view streaks for their groups.
CREATE POLICY "Users can view streaks from their groups" ON streaks FOR SELECT
USING (
  EXISTS (
    SELECT 1 FROM group_members 
    WHERE group_members.group_id = streaks.group_id 
    AND group_members.user_id = auth.uid()
  )
);
