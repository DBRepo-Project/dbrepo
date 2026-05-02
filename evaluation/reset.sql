SELECT periods.drop_system_versioning('table4');
CREATE TABLE table4_tmp (LIKE table4 INCLUDING ALL);
DROP TABLE table4;
CREATE TABLE table4 (LIKE table4_tmp INCLUDING ALL);
SELECT periods.add_system_time_period('table4', 'row_start', 'row_end');
SELECT periods.add_system_versioning('table4');