#!/usr/bin/env python3
"""Exercise fresh and upgraded image constraints in a disposable MariaDB container."""
from concurrent.futures import ThreadPoolExecutor
from pathlib import Path
import subprocess
import time
from uuid import uuid4

ROOT = Path(__file__).resolve().parents[1]
NAME = 'dbrepo-image-default-test-' + uuid4().hex[:12]
SCHEMAS = ('dbrepo-metadata-db/1_setup-schema.sql', 'helm/dbrepo/files/01-setup-schema.sql',
           '.live/config/includes.chroot_after_packages/etc/skel/config/1_setup-schema.sql')


def sql(body, success=True):
    result = subprocess.run(['docker', 'exec', '-i', NAME, 'sh', '-c',
        'export MYSQL_PWD="$MARIADB_ROOT_PASSWORD"; exec mariadb -uroot --batch --skip-column-names'],
        input=body, text=True, capture_output=True)
    if success:
        assert result.returncode == 0, result.stderr
    return result


def table(source):
    text = (ROOT/source).read_text()
    return text[text.index('CREATE TABLE'):text.index(') WITH SYSTEM VERSIONING;')+len(') WITH SYSTEM VERSIONING;')]


def insert(database, name, default=False):
    return f"INSERT INTO {database}.mdb_images(id,name,version,default_port,dialect,driver_class,jdbc_method,is_default) VALUES('{name}','{name}','1',3306,'dialect','driver','jdbc',{int(default)});"


def rejected(body):
    result = sql(body, success=False)
    assert result.returncode != 0 and 'ERROR 1062' in result.stderr, result.stderr


def exercise(database):
    sql(insert(database, 'default_a', True) + insert(database, 'ordinary_a') + insert(database, 'ordinary_b'))
    rejected(insert(database, 'default_b', True))
    rejected(f"UPDATE {database}.mdb_images SET is_default=TRUE WHERE id='ordinary_a'")
    # The guard protects name/version uniqueness as well as default updates.
    rejected(insert(database, 'duplicate_name').replace("'duplicate_name','duplicate_name'", "'duplicate_name','ordinary_a'"))
    sql(f"UPDATE {database}.mdb_images SET is_default=FALSE WHERE id='default_a'")
    with ThreadPoolExecutor(max_workers=2) as pool:
        results = list(pool.map(lambda name: sql(insert(database, name, True), success=False), ['race_a', 'race_b']))
    assert sum(r.returncode == 0 for r in results) == 1, 'Concurrent default inserts must have exactly one winner'
    assert all(r.returncode == 0 or 'ERROR 1062' in r.stderr for r in results)
    assert sql(f'SELECT COUNT(*) FROM {database}.mdb_images WHERE is_default').stdout.strip() == '1'
    assert sql(f'SELECT COUNT(*) FROM {database}.mdb_images WHERE NOT is_default').stdout.strip() == '3'


def snapshot(database):
    return sql(f'SELECT id,name,version,is_default,ROW_START,ROW_END FROM {database}.mdb_images FOR SYSTEM_TIME ALL ORDER BY id,ROW_START').stdout


def main():
    create = table(SCHEMAS[0])
    assert all(table(source) == create for source in SCHEMAS), 'Fresh schema copies differ'
    subprocess.run(['docker', 'run', '-d', '--name', NAME, '--memory=512m', '-e',
                    'MARIADB_ROOT_PASSWORD=local-image-constraint-test', 'mariadb:10.11'],
                   check=True, stdout=subprocess.DEVNULL)
    try:
        for _ in range(120):
            if sql('SELECT 1', success=False).returncode == 0:
                break
            time.sleep(.5)
        else:
            raise AssertionError('Disposable MariaDB did not start')
        for index, source in enumerate(SCHEMAS):
            database = 'fresh_' + str(index)
            sql(f'CREATE DATABASE {database}; USE {database};\n' + table(source))
            exercise(database)
        sql('CREATE DATABASE upgraded; USE upgraded;\n' + create.replace(
            '    default_image TINYINT GENERATED ALWAYS AS (CASE WHEN is_default THEN 1 ELSE NULL END),\n', '').replace(
            'UNIQUE KEY `only_default_image` (`default_image`)', 'UNIQUE KEY `old_hibernate_name` (`is_default`)'))
        sql(insert('upgraded', 'old_default', True) + insert('upgraded', 'old_ordinary') +
            "UPDATE upgraded.mdb_images SET version='2' WHERE id='old_default';")
        before = snapshot('upgraded')
        history_setting = sql('SELECT @@SESSION.system_versioning_alter_history').stdout
        migration = (ROOT/'dbrepo-metadata-db/migration/image-default-constraint.sql').read_text()
        for _ in range(2):
            assert sql('USE upgraded;\n'+migration+'\nSELECT @@SESSION.system_versioning_alter_history').stdout == history_setting
            assert snapshot('upgraded') == before, 'Migration changed image rows or version history'
        sql("DELETE FROM upgraded.mdb_images;")
        exercise('upgraded')
        assert sql("SELECT COUNT(*) FROM information_schema.STATISTICS WHERE TABLE_SCHEMA='upgraded' AND TABLE_NAME='mdb_images' AND NON_UNIQUE=0 AND COLUMN_NAME='is_default'").stdout.strip() == '0'
        print('PASS: all fresh schemas, repeated migration, preserved version history, multiple ordinary images and concurrent single-default enforcement')
    finally:
        subprocess.run(['docker', 'rm', '-fv', NAME], stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL, check=True)


if __name__ == '__main__':
    main()
