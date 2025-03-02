CREATE
    DATABASE weather_at;
USE
    weather_at;

CREATE TABLE weather_location
(
    location VARCHAR(255) PRIMARY KEY,
    lat      DOUBLE PRECISION NULL,
    lng      DOUBLE PRECISION NULL
) WITH SYSTEM VERSIONING COMMENT 'Weather location';

INSERT INTO weather_location (location, lat, lng)
VALUES ('Sakhir', 26.0318979, 50.5084668),
       ('Fiorano', 44.534308, 10.8551698),
       ('Spielberg', 47.219672, 14.7625382),
       ('Fuji', 35.3726836, 138.927587);