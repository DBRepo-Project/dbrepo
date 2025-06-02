ALTER TABLE mdb_identifier_creators
    DROP SYSTEM VERSIONING;
ALTER TABLE mdb_identifier_creators
    ADD COLUMN ordinal_position INT NOT NULL;
ALTER TABLE mdb_identifier_creators
    ADD SYSTEM VERSIONING;
ALTER TABLE mdb_identifier_titles
    DROP SYSTEM VERSIONING;
ALTER TABLE mdb_identifier_titles
    ADD COLUMN ordinal_position INT NOT NULL;
ALTER TABLE mdb_identifier_titles
    ADD SYSTEM VERSIONING;
ALTER TABLE mdb_identifier_funders
    DROP SYSTEM VERSIONING;
ALTER TABLE mdb_identifier_funders
    ADD COLUMN ordinal_position INT NOT NULL;
ALTER TABLE mdb_identifier_funders
    ADD SYSTEM VERSIONING;
ALTER TABLE mdb_identifier_descriptions
    DROP SYSTEM VERSIONING;
ALTER TABLE mdb_identifier_descriptions
    ADD COLUMN ordinal_position INT NOT NULL;
ALTER TABLE mdb_identifier_descriptions
    ADD SYSTEM VERSIONING;
ALTER TABLE mdb_identifier_related
    DROP SYSTEM VERSIONING;
ALTER TABLE mdb_identifier_related
    ADD COLUMN ordinal_position INT NOT NULL;
ALTER TABLE mdb_identifier_related
    ADD SYSTEM VERSIONING;
