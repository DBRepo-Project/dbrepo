BEGIN;

INSERT INTO `mdb_containers` (name, internal_name, image_id, host, port, sidecar_host, sidecar_port,
                              privileged_username, privileged_password)
VALUES ('MariaDB 10.5', 'mariadb_10_5', 1, 'data-db', 3306, 'data-db-sidecar', 3305, 'root', 'dbrepo');

COMMIT;
