BEGIN;

INSERT INTO `mdb_containers` (name, internal_name, image_id, host, port, ui_host, ui_port, sidecar_host, sidecar_port,
                              privileged_username, privileged_password)
VALUES ('MariaDB Galera 11.1.3', 'mariadb_11_1_3', 1, 'data-db', 3306, 'localhost', 3306, 'data-db-sidecar', 3305,
        'root', 'dbrepo');

COMMIT;
