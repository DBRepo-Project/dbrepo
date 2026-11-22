SET @saved_oai_alter_history = @@SESSION.system_versioning_alter_history;
SET SESSION system_versioning_alter_history = KEEP;
ALTER TABLE mdb_identifiers ADD COLUMN IF NOT EXISTS datacite_repository VARCHAR(255);
SET SESSION system_versioning_alter_history = @saved_oai_alter_history;
