-- Apply to the metadata database before deploying table archival support.
-- Existing rows and all system-versioned history remain intact.
SET SESSION system_versioning_alter_history = KEEP;
ALTER TABLE mdb_tables ADD COLUMN IF NOT EXISTS archived_at TIMESTAMP(6) NULL DEFAULT NULL;
