BEGIN;

INSERT INTO `mdb_containers` (id, name, internal_name, image_id, host, port, ui_host, ui_port, privileged_username,
                              privileged_password)
VALUES ('6cfb3b8e-1792-4e46-871a-f3d103527203', 'mariadb:11.1.3-debian-11-r6', 'mariadb_11_1_3',
        'd79cb089-363c-488b-9717-649e44d8fcc5', 'data-db', 3306, 'localhost', 3306, 'root', 'dbrepo');

COMMIT;
