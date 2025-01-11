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