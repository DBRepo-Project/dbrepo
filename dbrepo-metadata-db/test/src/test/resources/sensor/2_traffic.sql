CREATE SEQUENCE seq_sensor
    START 1;

CREATE TABLE sensor
(
    `timestamp` TIMESTAMP NULL,
    primary key (`timestamp`)
) with system versioning;