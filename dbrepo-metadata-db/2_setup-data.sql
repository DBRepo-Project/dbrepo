BEGIN;

INSERT INTO `mdb_containers` (id, name, internal_name, image_id, host, port, ui_host, ui_port, privileged_username,
                              privileged_password, readonly_username, readonly_password)
VALUES ('6cfb3b8e-1792-4e46-871a-f3d103527203', 'mariadb:11.3.2', 'mariadb_11_3_2',
        'd79cb089-363c-488b-9717-649e44d8fcc5', 'data-db', 3306, 'localhost', 3306, 'root', 'dbrepo', 'readonly',
        'readonly');

GRANT SLAVE MONITOR, PROCESS ON *.* TO `readonly`@`%`;

COMMIT;
