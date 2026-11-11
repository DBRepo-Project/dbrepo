-- Apply before deploying the metadata service. Safe to run again.
SET SESSION system_versioning_alter_history = KEEP;
ALTER TABLE mdb_have_access ADD COLUMN IF NOT EXISTS sql_grant_pending BOOLEAN NOT NULL DEFAULT FALSE;
