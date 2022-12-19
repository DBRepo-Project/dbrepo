CREATE SEQUENCE IF NOT EXISTS `qs_queries_seq`;
CREATE SEQUENCE IF NOT EXISTS `qs_tables_seq`;
CREATE SEQUENCE IF NOT EXISTS `qs_columns_seq`;
CREATE SEQUENCE IF NOT EXISTS `qs_views_seq`;
CREATE TABLE `qs_queries`
(
    `id`               bigint       not null primary key default nextval(`qs_queries_seq`),
    `cid`              bigint       not null,
    `created`          datetime     not null             default now(),
    `created_by`       bigint       not null,
    `dbid`             bigint       not null,
    `execution`        datetime     not null,
    `last_modified`    datetime,
    `query`            text         not null,
    `query_normalized` text         not null,
    `is_persisted`     boolean      not null,
    `query_hash`       varchar(255) not null,
    `result_hash`      varchar(255),
    `result_number`    bigint,
    `type`             VARCHAR(50)  not null
);
CREATE TABLE `qs_tables`
(
    `id`            bigint   not null primary key default nextval(`qs_tables_seq`),
    `created`       datetime not null,
    `dbid`          bigint   not null,
    `last_modified` datetime
);
CREATE TABLE `qs_columns`
(
    `id`            bigint   not null primary key default nextval(`qs_columns_seq`),
    `created`       datetime not null,
    `dbid`          bigint   not null,
    `tid`           bigint   not null,
    `last_modified` datetime
);
CREATE TABLE `qs_views`
(
    `id`              bigint       not null primary key default nextval(`qs_views_seq`),
    `vcid`            bigint       not null,
    `vdbid`           bigint       not null,
    `created_by`      bigint       not null,
    `name`            varchar(255) not null,
    `internal_name`   varchar(255) not null,
    `is_public`       boolean      not null,
    `is_initial_view` boolean      not null,
    `query`           text         not null,
    `created`         datetime     not null
);