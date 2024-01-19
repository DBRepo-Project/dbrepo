#!/bin/bash
#
# Script to initialize db schema and insert test values
# Intented to be run from pytest inside a mysql/mariadb docker container

mysql -u"$MYSQL_USER" -p"$MYSQL_ROOT_PASSWORD" "$MYSQL_DATABASE" <<EOSQL
source /schema.sql

INSERT INTO mdb_users (id, username, firstname, lastname, email, orcid, affiliation, mariadb_password, theme_dark)
VALUES
    ('1', 'user1', 'John', 'Doe', 'user1@example.com', '0000-0001-1234-5678', 'University A', 'password1', TRUE);

INSERT INTO mdb_images (name, version, default_port, dialect, driver_class, jdbc_method, created, last_modified)
VALUES
    ('Image1', '1.0', 5432, 'postgresql', 'org.postgresql.Driver', 'jdbc:postgresql', NOW(), NOW()),
    ('Image2', '2.0', 3306, 'mysql', 'com.mysql.jdbc.Driver', 'jdbc:mysql', NOW(), NOW());

INSERT INTO mdb_containers (internal_name, name, host, port, ui_host, ui_port, ui_additional_flags, sidecar_host, sidecar_port, image_id, created, last_modified, privileged_username, privileged_password)
VALUES
    ('container1', 'Container1', 'localhost', 3306, 'localhost', 3306, NULL, 'localhost', 3305, 1, NOW(), NOW(), 'admin', 'admin'),
    ('container2', 'Container2', 'localhost', 3307, 'localhost', 3307, NULL, 'localhost', 3306, 2, NOW(), NOW(), 'admin', 'admin');

INSERT INTO mdb_databases (cid, name, internal_name, exchange_name, description, engine, is_public, created_by, owned_by, contact_person, created, last_modified)
VALUES
    (1, 'Database1', 'internal1', 'Exchange1', 'Description1', 'InnoDB', TRUE, NULL, NULL, NULL, NOW(), NOW()),
    (2, 'Database2', 'internal2', 'Exchange2', 'Description2', 'MyISAM', FALSE, NULL, NULL, NULL, NOW(), NOW());

INSERT INTO mdb_tables (tDBID, internal_name, queue_name, routing_key, tName, tDescription, num_rows, data_length, max_data_length, avg_row_length, \`separator\`, quote, element_null, skip_lines, element_true, element_false, Version, created, versioned, created_by, owned_by, last_modified)
VALUES
    (1, 'table1', 'Queue1', 'RoutingKey1', 'TableName1', 'TableDescription1', 5, 9, 15, 3, ',', '"', NULL, 2, TRUE, FALSE, '1.0', NOW(), TRUE, '1', '1', NOW()),
    (1, 'table2', 'Queue2', 'RoutingKey2', 'TableName2', 'TableDescription2', 0, 0, 0, 0, ',', "'", 'NA', 0, '1', '0', '2.0', NOW(), TRUE, '1', '1', NOW()),
    (2, 'table3', 'Queue3', 'RoutingKey3', 'TableName3', 'TableDescription3', 0, 0, 0, 0, ',', '"', 'EMPTY', 5, 'yes', 'no', '1.0', NOW(), TRUE, '1', '1', NOW());

INSERT INTO mdb_columns (tID, dfID, cName, internal_name, alias, Datatype, length, ordinal_position, is_primary_key, index_length, size, d, auto_generated, is_null_allowed, val_min, val_max, mean, median, std_dev, created, last_modified)
VALUES
    (1, NULL, 'Column1', 'col1', 'Alias1', 'DATETIME', 255, 1, TRUE, NULL, NULL, NULL, FALSE, TRUE, NULL, NULL, NULL, NULL, NULL, NOW(), NOW()),
    (1, NULL, 'Column2', 'col2', 'Alias2', 'VARCHAR', NULL, 2, FALSE, NULL, NULL, NULL, FALSE, TRUE, NULL, NULL, NULL, NULL, NULL, NOW(), NOW()),
    (1, NULL, 'Column3', 'col3', 'Alias3', 'FLOAT', 10, 3, FALSE, NULL, NULL, 2, FALSE, TRUE, 0, 100, NULL, NULL, NULL, NOW(), NOW()),
    (1, NULL, 'Column4', 'col4', 'Alias4', 'FLOAT', 10, 3, FALSE, NULL, NULL, 2, FALSE, TRUE, 0, 100, NULL, NULL, NULL, NOW(), NOW()),
    (1, NULL, 'Column5', 'col5', 'Alias5', 'INT', 10, 3, FALSE, NULL, NULL, 2, FALSE, TRUE, 0, 100, NULL, NULL, NULL, NOW(), NOW());
EOSQL
