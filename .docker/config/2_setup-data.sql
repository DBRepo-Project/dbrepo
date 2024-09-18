BEGIN;

INSERT INTO `mdb_containers` (name, internal_name, image_id, host, port, ui_host, ui_port, sidecar_host, sidecar_port,
                              privileged_username, privileged_password)
VALUES ('mariadb:11.1.3-debian-11-r6', 'mariadb_11_1_3', 1, 'data-db', 3306, 'localhost', 3306, 'data-db-sidecar', 8080,
        'root', 'dbrepo');

COMMIT;
