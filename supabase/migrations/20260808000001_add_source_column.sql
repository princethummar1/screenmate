-- Add source tracking column to daily_logs
-- Distinguishes website OCR uploads from Android automatic submissions
ALTER TABLE daily_logs ADD COLUMN IF NOT EXISTS source VARCHAR(20) DEFAULT 'screenshot';
COMMENT ON COLUMN daily_logs.source IS 'Origin of the log entry: screenshot (website OCR), android_auto (Android app automatic collection)';
