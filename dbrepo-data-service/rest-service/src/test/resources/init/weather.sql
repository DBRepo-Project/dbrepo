/* https://www.kaggle.com/jsphyg/weather-dataset-rattle-package */
CREATE DATABASE weather;
USE weather;

CREATE TABLE weather_location
(
    location VARCHAR(255) PRIMARY KEY,
    lat      DOUBLE PRECISION NULL,
    lng      DOUBLE PRECISION NULL
) WITH SYSTEM VERSIONING;

CREATE TABLE weather_aus
(
    id       SERIAL PRIMARY KEY,
    `date`   DATE             NOT NULL,
    location VARCHAR(255)     NULL COMMENT 'Closest city',
    mintemp  DOUBLE PRECISION NULL,
    rainfall DOUBLE PRECISION NULL,
    FOREIGN KEY (location) REFERENCES weather_location (location) ON DELETE SET NULL,
    CONSTRAINT some_constraint UNIQUE (`date`),
    CHECK (`mintemp` > 0)
) WITH SYSTEM VERSIONING COMMENT 'Weather in Australia';

CREATE TABLE complex_primary_key
(
    id       BIGINT NOT NULL,
    other_id BIGINT NOT NULL,
    PRIMARY KEY (id, other_id)
) WITH SYSTEM VERSIONING;

CREATE TABLE complex_foreign_keys
(
    id         BIGINT NOT NULL PRIMARY KEY,
    weather_id BIGINT NOT NULL,
    other_id   BIGINT NOT NULL,
    FOREIGN KEY (weather_id, other_id) REFERENCES complex_primary_key (id, `other_id`)
) WITH SYSTEM VERSIONING;

CREATE TABLE sensor
(
    `timestamp` TIMESTAMP NOT NULL PRIMARY KEY,
    `value`     DECIMAL
) WITH SYSTEM VERSIONING;

CREATE TABLE exotic_boolean
(
    `bool_default`          BOOLEAN             NOT NULL PRIMARY KEY,
    `bool_tinyint`          TINYINT(1)          NOT NULL,
    `bool_tinyint_unsigned` TINYINT(1) UNSIGNED NOT NULL
) WITH SYSTEM VERSIONING;

INSERT INTO weather_location (location, lat, lng)
VALUES ('Albury', -36.0653583, 146.9112214),
       ('Sydney', -33.847927, 150.6517942),
       ('Vienna', null, null);

INSERT INTO weather_aus (id, `date`, location, mintemp, rainfall)
VALUES (1, '2008-12-01', 'Albury', 13.4, 0.6),
       (2, '2008-12-02', 'Albury', 7.4, 0),
       (3, '2008-12-03', 'Albury', 12.9, 0);

INSERT INTO sensor (`timestamp`, value)
VALUES ('2022-12-24 17:00:00', 10.0),
       ('2022-12-24 18:00:00', 10.2),
       ('2022-12-24 19:00:00', null),
       ('2022-12-24 20:00:00', 10.3),
       ('2022-12-24 21:00:00', 10.0),
       ('2022-12-24 22:00:00', null);

-- #####################################################################################################################
-- ## TEST CASE PRE-REQUISITE                                                                                         ##
-- #####################################################################################################################

CREATE VIEW junit2 AS
(
select `date`, `location` as loc, `mintemp`, `rainfall`
from `weather_aus`
where `location` = 'Albury');

CREATE TABLE not_in_metadata_db
(
    id          BIGINT       NOT NULL PRIMARY KEY,
    given_name  VARCHAR(255) NOT NULL,
    middle_name VARCHAR(255) NULL,
    family_name VARCHAR(255) NOT NULL,
    age         INT          NOT NULL CHECK ( age > 0 AND age < 120 ),
    UNIQUE (given_name, family_name)
) WITH SYSTEM VERSIONING;

CREATE VIEW not_in_metadata_db2 AS
(
select `date`, `location`, `mintemp` as `MinTemp`, `rainfall` as `Rainfall`
from `weather_aus`
where `location` = 'Vienna');