/* https://www.kaggle.com/jsphyg/weather-dataset-rattle-package */
CREATE
DATABASE weather;
USE
weather;

CREATE TABLE weather_location
(
    location VARCHAR(255) PRIMARY KEY,
    lat      DOUBLE PRECISION NULL,
    lng      DOUBLE PRECISION NULL
) WITH SYSTEM VERSIONING;

CREATE VIEW `hs_weather_location` AS
SELECT *
FROM (SELECT ROW_START AS inserted_at, IF(ROW_END > NOW(), NULL, ROW_END) AS deleted_at, COUNT(*) as total
      FROM `weather_location` FOR SYSTEM_TIME ALL
      GROUP BY inserted_at, deleted_at
      ORDER BY deleted_at DESC LIMIT 50) AS v
ORDER BY v.inserted_at, v.deleted_at ASC;

CREATE TABLE weather_aus
(
    id       BIGINT NOT NULL PRIMARY KEY,
    `date`   DATE   NOT NULL,
    location VARCHAR(255) NULL,
    mintemp  DOUBLE PRECISION NULL,
    rainfall DOUBLE PRECISION NULL,
    FOREIGN KEY (location) REFERENCES weather_location (location),
    UNIQUE (`date`),
    CHECK (`mintemp` > 0)
) WITH SYSTEM VERSIONING;

CREATE VIEW `hs_weather_aus` AS
SELECT *
FROM (SELECT ROW_START AS inserted_at, IF(ROW_END > NOW(), NULL, ROW_END) AS deleted_at, COUNT(*) as total
      FROM `weather_aus` FOR SYSTEM_TIME ALL
      GROUP BY inserted_at, deleted_at
      ORDER BY deleted_at DESC LIMIT 50) AS v
ORDER BY v.inserted_at, v.deleted_at ASC;

CREATE TABLE sensor
(
    `timestamp` TIMESTAMP NOT NULL PRIMARY KEY,
    `value`     DECIMAL
) WITH SYSTEM VERSIONING;

CREATE VIEW `hs_sensor` AS
SELECT *
FROM (SELECT ROW_START AS inserted_at, IF(ROW_END > NOW(), NULL, ROW_END) AS deleted_at, COUNT(*) as total
      FROM `sensor` FOR SYSTEM_TIME ALL
      GROUP BY inserted_at, deleted_at
      ORDER BY deleted_at DESC LIMIT 50) AS v
ORDER BY v.inserted_at, v.deleted_at ASC;

-- sequence not in metadata on purpose
CREATE SEQUENCE weather_aut_seq NOCACHE;

-- table not in metadata on purpose
CREATE TABLE weather_aut
(
    id       BIGINT NOT NULL PRIMARY KEY default nextval(`weather_aut_seq`),
    `date`   DATE   NOT NULL,
    location VARCHAR(255) NULL,
    mintemp  DOUBLE PRECISION NULL,
    rainfall DOUBLE PRECISION NULL,
    FOREIGN KEY (location) REFERENCES weather_location (location),
    UNIQUE (`date`),
    CHECK (`mintemp` > 0)
) WITH SYSTEM VERSIONING;

-- no history view in data database on purpose

-- table not in metadata on purpose
CREATE TABLE weather_aut_without_versioning
(
    id       BIGINT NOT NULL PRIMARY KEY,
    `date`   DATE   NOT NULL,
    location VARCHAR(255) NULL,
    mintemp  DOUBLE PRECISION NULL,
    rainfall DOUBLE PRECISION NULL,
    FOREIGN KEY (location) REFERENCES weather_location (location),
    UNIQUE (`date`),
    CHECK (`mintemp` > 0)
);

-- no history view in data database on purpose

-- view not in metadata on purpose
CREATE VIEW weather_aut_merge AS
(
SELECT id, `date`
FROM `weather_aut` v
WHERE NOT EXISTS (SELECT `id` FROM weather_aut_without_versioning vv WHERE vv.id = v.id)
    );

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
select `date`, `location`, `mintemp`, `rainfall`
from `weather_aus`
where `location` = 'Albury');

CREATE VIEW junit3 AS
(
select w.`mintemp`, w.`rainfall`, w.`location`, m.`date`
from `weather_aus` w
         join `junit2` m on m.`location` = w.`location` and m.`date` = w.`date`
    )