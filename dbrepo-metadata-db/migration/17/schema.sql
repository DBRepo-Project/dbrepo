ALTER TABLE mdb_containers
    DROP SYSTEM VERSIONING;
ALTER TABLE mdb_containers
    ADD COLUMN readonly_username VARCHAR(255) NOT NULL;
ALTER TABLE mdb_containers
    ADD COLUMN readonly_password VARCHAR(255) NOT NULL;
ALTER TABLE mdb_containers
    ADD SYSTEM VERSIONING;

ALTER TABLE mdb_databases
    DROP SYSTEM VERSIONING;
ALTER TABLE mdb_databases
    ADD COLUMN grafana_dashboard_uid character varying(255);
ALTER TABLE mdb_databases
    ADD COLUMN is_dashboard_enabled BOOLEAN NOT NULL DEFAULT TRUE;
ALTER TABLE mdb_databases
    ADD SYSTEM VERSIONING;