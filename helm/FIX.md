# Fix

Wrong datatypes and create `SYSTEM VERSIONING` column from `created_at`:

```sql
BEGIN;
SELECT periods.drop_system_versioning('dbrepo.wb_single_measurements');
SELECT periods.drop_system_time_period('dbrepo.wb_single_measurements');
DROP TABLE dbrepo.wb_single_measurements;
ALTER TABLE dbrepo.wb_single_measurements DROP COLUMN created_at;
ALTER TABLE dbrepo.wb_single_measurements DROP COLUMN row_start;
ALTER TABLE dbrepo.wb_single_measurements DROP COLUMN row_end;
ALTER TABLE dbrepo.wb_single_measurements ADD COLUMN created_at TIMESTAMP WITH TIME ZONE NULL;

SELECT periods.drop_system_versioning('dbrepo.wb_sample');
SELECT periods.drop_system_time_period('dbrepo.wb_sample');
DROP TABLE dbrepo.wb_sample_history;
ALTER TABLE dbrepo.wb_sample DROP COLUMN created_at;
ALTER TABLE dbrepo.wb_sample DROP COLUMN row_start;
ALTER TABLE dbrepo.wb_sample DROP COLUMN row_end;
ALTER TABLE dbrepo.wb_sample ADD COLUMN created_at TIMESTAMP WITH TIME ZONE NULL;

SELECT periods.drop_system_versioning('dbrepo.md_data_source');
SELECT periods.drop_system_time_period('dbrepo.md_data_source');
DROP TABLE dbrepo.md_data_source_history;
ALTER TABLE dbrepo.md_data_source DROP COLUMN created_at;
ALTER TABLE dbrepo.md_data_source DROP COLUMN row_start;
ALTER TABLE dbrepo.md_data_source DROP COLUMN row_end;
ALTER TABLE dbrepo.md_data_source ADD COLUMN created_at TIMESTAMP WITH TIME ZONE NULL;
END;

BEGIN;
SELECT periods.add_system_time_period('dbrepo.wb_single_measurements', 'created_at', 'row_end');
SELECT periods.add_system_versioning('dbrepo.wb_single_measurements');

SELECT periods.add_system_time_period('dbrepo.wb_sample', 'created_at', 'row_end');
SELECT periods.add_system_versioning('dbrepo.wb_sample');

SELECT periods.add_system_time_period('dbrepo.md_data_source', 'row_start', 'row_end');
SELECT periods.add_system_versioning('dbrepo.md_data_source');
UPDATE md_data_source SET row_start = created_at;
END;
```