CREATE DATABASE musicology;
USE musicology;

CREATE SEQUENCE seq_mfcc;

CREATE TABLE mfcc
(
    id    BIGINT PRIMARY KEY NOT NULL DEFAULT nextval(`seq_mfcc`),
    value DECIMAL            NOT NULL,
    raw   LONGBLOB           NULL
) WITH SYSTEM VERSIONING;

INSERT INTO `mfcc` (`value`)
VALUES (11.2),
       (11.3),
       (11.4),
       (11.9),
       (12.3),
       (23.1);