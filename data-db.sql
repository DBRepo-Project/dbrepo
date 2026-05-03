create table dbrepo.asdf
(
    column00 bigint       not null
        primary key,
    column01 varchar(255) null,
    column02 text         null,
    column03 bigint       null,
    column04 varchar(255) null,
    column05 varchar(255) null,
    column06 varchar(255) null,
    column07 varchar(255) null,
    column08 varchar(255) null,
    column09 varchar(255) null,
    column10 varchar(255) null,
    column11 varchar(255) null,
    column12 varchar(255) null,
    column13 varchar(255) null,
    column14 varchar(255) null,
    column15 varchar(255) null,
    column16 varchar(255) null,
    column17 varchar(255) null,
    column18 varchar(255) null,
    column19 varchar(255) null,
    column20 varchar(255) null
);
SELECT periods.add_system_time_period('dbrepo.asdf', 'row_start', 'row_end');
SELECT periods.add_system_versioning('dbrepo.asdf');