SET SESSION system_versioning_alter_history = KEEP;
ALTER TABLE mdb_view ADD COLUMN IF NOT EXISTS archived_at TIMESTAMP(6) NULL DEFAULT NULL;
