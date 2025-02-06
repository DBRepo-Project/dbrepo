ALTER TABLE mdb_users
    DROP SYSTEM VERSIONING;
BEGIN;
ALTER TABLE mdb_users
    ADD COLUMN keycloak_id character varying(36) NOT NULL;
UPDATE mdb_users
SET keycloak_id = id;
ALTER TABLE mdb_users
    ADD CONSTRAINT UNIQUE (keycloak_id);
COMMIT;
ALTER TABLE mdb_users
    ADD SYSTEM VERSIONING;