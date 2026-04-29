BEGIN;

CREATE
USER readonly WITH PASSWORD 'readonly';

INSERT INTO dbrepo.mdb_containers (id, name, internal_name, image_id, host, port, privileged_username,
                                   privileged_password, readonly_username, readonly_password)
VALUES ('ee960ff5-cca2-4222-bd6f-0f29ac2e1959', 'postgres:17-alpine', 'postgres_17_alpine',
        '32c13903-651a-404c-8fd3-f92708899a69', 'data-db', 5432, 'dbrepo', 'dbrepo', 'dbrepo',
        'dbrepo');

COMMIT;
