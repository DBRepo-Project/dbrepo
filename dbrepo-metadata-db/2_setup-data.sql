BEGIN;

CREATE USER readonly WITH PASSWORD 'readonly';

INSERT INTO mdb_containers (id, name, internal_name, image_id, host, port, ui_host, ui_port, privileged_username,
                              privileged_password, readonly_username, readonly_password)
VALUES ('ee960ff5-cca2-4222-bd6f-0f29ac2e1959', 'postgres:18-alpine', 'postgres_18_alpine',
        '32c13903-651a-404c-8fd3-f92708899a69', 'data-db', 5432, 'localhost', 5432, 'postgres', 'dbrepo', 'readonly',
        'readonly');

COMMIT;
