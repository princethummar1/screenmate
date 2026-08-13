-- ============================================
-- UPDATE RLS POLICIES (Idempotent)
-- ============================================

-- 1. Add UPDATE policy for daily_logs (required for Android upsert)
DO $$ BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_policies
        WHERE tablename = 'daily_logs'
        AND policyname = 'Users can update their own logs'
    ) THEN
        CREATE POLICY "Users can update their own logs"
            ON daily_logs FOR UPDATE
            USING (auth.uid() = user_id);
    END IF;
END $$;

-- 2. Recreate device_usage_daily policies (idempotent)
DROP POLICY IF EXISTS "Users manage own usage daily" ON device_usage_daily;
CREATE POLICY "Users manage own usage daily"
    ON device_usage_daily FOR ALL
    USING (auth.uid() = user_id)
    WITH CHECK (auth.uid() = user_id);

-- 3. Recreate device_usage_apps policies (idempotent)
DROP POLICY IF EXISTS "Users manage own usage apps" ON device_usage_apps;
CREATE POLICY "Users manage own usage apps"
    ON device_usage_apps FOR ALL
    USING (EXISTS (SELECT 1 FROM device_usage_daily d WHERE d.id = device_usage_apps.daily_id AND d.user_id = auth.uid()))
    WITH CHECK (EXISTS (SELECT 1 FROM device_usage_daily d WHERE d.id = device_usage_apps.daily_id AND d.user_id = auth.uid()));

-- 4. Ensure grants are in place
GRANT ALL ON TABLE device_usage_daily TO authenticated;
GRANT ALL ON TABLE device_usage_apps TO authenticated;
GRANT USAGE, SELECT ON SEQUENCE device_usage_daily_id_seq TO authenticated;
GRANT USAGE, SELECT ON SEQUENCE device_usage_apps_id_seq TO authenticated;
