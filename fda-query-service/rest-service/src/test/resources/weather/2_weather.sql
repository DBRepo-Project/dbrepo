CREATE USER `junit`@`%`;
GRANT CREATE, CREATE VIEW, SELECT, INSERT, UPDATE, DELETE ON *.* TO `junit`@`%`;

/* https://www.kaggle.com/jsphyg/weather-dataset-rattle-package */
CREATE TABLE weather_location
(
    location VARCHAR(255) PRIMARY KEY,
    lat      DOUBLE PRECISION NULL,
    lng      DOUBLE PRECISION NULL
) WITH SYSTEM VERSIONING;

CREATE TABLE weather_aus
(
    id       BIGINT           NOT NULL PRIMARY KEY,
    `date`   DATE             NOT NULL,
    location VARCHAR(255)     NULL,
    mintemp  DOUBLE PRECISION NULL,
    rainfall DOUBLE PRECISION NULL,
    FOREIGN KEY (location) REFERENCES weather_location (location)
) WITH SYSTEM VERSIONING;

INSERT INTO weather_location (location, lat, lng)
VALUES ('Albury', -36.0653583, 146.9112214),
       ('Sydney', -33.847927, 150.6517942);

INSERT INTO weather_aus (id, `date`, location, mintemp, rainfall)
VALUES (1, '2008-12-01', 'Albury', 13.4, 0.6),
       (2, '2008-12-02', 'Albury', 7.4, 0),
       (3, '2008-12-03', 'Albury', 12.9, 0);

########################################################################################################################
## TEST CASE PRE-REQUISITE                                                                                            ##
########################################################################################################################

CREATE VIEW mock_view AS
(
SELECT `location`, `lat`, `lng`
FROM `weather_location`
WHERE `location` = 'Albury');

