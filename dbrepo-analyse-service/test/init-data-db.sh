#!/bin/bash
#
# Script to initialize a test data db according to a csv file
# Intented to be run from pytest inside a mysql/mariadb container

mysql -u"$MYSQL_USER" -p"$MYSQL_ROOT_PASSWORD" <<EOSQL
CREATE DATABASE internal1;

USE internal1;

CREATE TABLE table1 (
    col1 DATETIME,
    col2 VARCHAR(255),
    col3 FLOAT,
    col4 FLOAT,
    col5 INT
);

LOAD DATA INFILE '/test_stats_01.csv'
INTO TABLE table1
FIELDS TERMINATED BY ',' ENCLOSED BY '"'
LINES TERMINATED BY '\\n'
IGNORE 1 LINES
(col1, col2, col3, col4, col5);
EOSQL
