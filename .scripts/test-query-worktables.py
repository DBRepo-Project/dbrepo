#!/usr/bin/env python3
"""Exercise Query Store work-table isolation against a disposable MariaDB."""
from concurrent.futures import ThreadPoolExecutor
import hashlib
import importlib.util
import os
import re
from pathlib import Path
import secrets
import subprocess
import time

ROOT = Path(__file__).resolve().parents[1]


def check_result_hashes(sql):
    query = "SELECT a, b FROM samples"

    def digest(statement, expected_count, limit=1024, database="fixture"):
        literal = statement.replace("'", "''")
        result = sql(f"USE {database}; SET SESSION group_concat_max_len={limit}; "
                     f"CALL hash_query('{literal}', @digest); SELECT @digest,@count,@@session.group_concat_max_len")
        value, count, restored_limit = result.stdout.strip().split("\t")
        assert re.fullmatch(r"v2:[0-9a-f]{64}", value), value
        assert (int(count), int(restored_limit)) == (expected_count, limit)
        return value

    def store():
        return sql(f"USE fixture; CALL store_query('{query}','{query}',NOW(),@id); SELECT @id").stdout.strip()

    sql("USE fixture; CREATE TABLE samples(a VARCHAR(12), b VARCHAR(12)); INSERT INTO samples VALUES('ab','c')")
    first_hash = digest(query, 1)
    first_id = store()
    # The pre-upgrade citation keeps its stored hash and ID, even for the same query and rows.
    legacy = sql("SELECT result_hash FROM fixture.qs_queries WHERE id='00000000-0000-0000-0000-000000000137'")
    assert legacy.stdout.strip() == hashlib.sha256(b"abc").hexdigest()
    assert first_id != "00000000-0000-0000-0000-000000000137"
    assert sql(f"SELECT result_hash FROM fixture.qs_queries WHERE id='{first_id}'").stdout.strip() == first_hash
    assert store() == first_id
    sql("USE fixture; UPDATE samples SET a='a',b='bc'")
    assert digest(query, 1) != first_hash and store() != first_id

    sql("USE fixture; UPDATE samples SET a=NULL,b=''")
    null_first = digest(query, 1)
    sql("USE fixture; UPDATE samples SET a='',b=NULL")
    assert digest(query, 1) != null_first
    sql("USE fixture; UPDATE samples SET a='',b=''")
    assert digest(query, 1) != null_first
    assert digest("SELECT 1 AS a", 1) != digest("SELECT '1' AS a", 1)
    assert digest("SELECT b AS a,a AS b FROM samples WHERE FALSE", 0) != digest("SELECT a FROM samples WHERE FALSE", 0)
    assert digest("SELECT UNHEX('00FF') AS b", 1) != digest("SELECT UNHEX('0000') AS b", 1)

    sql("USE fixture; DELETE FROM samples; INSERT INTO samples VALUES('a',''),('z',NULL)")
    ascending = digest(query + " ORDER BY a ASC", 2)
    assert digest(query + " ORDER BY a DESC", 2) == ascending
    sql("USE fixture; INSERT INTO samples VALUES('a','')")
    assert digest(query, 3) != ascending
    # These two hashes also identify the same rows on integration/replication.
    sql("USE fixture; CREATE TABLE long_sample(payload VARBINARY(8192)); "
        "INSERT INTO long_sample VALUES(CONCAT(REPEAT('x',4096),'a'))")
    long_query = "SELECT payload FROM long_sample"
    first_long = "v2:075eec59e7a682b7830c96f43fd37354c5a0004eea8c5ddf77fed130750cace1"
    assert digest(long_query, 1) == first_long
    assert digest(long_query, 1, 16) == digest(long_query, 1, 100000) == first_long
    sql("USE fixture; UPDATE long_sample SET payload=CONCAT(REPEAT('x',4096),'b')")
    assert digest(long_query, 1) == "v2:5104b51a8064488e97a7b6d14a652b1bbc03be7ace51efa0edf414cb0debf90d"
    assert digest("SELECT 42 AS fixture", 1) == digest("SELECT 42 AS fixture", 1, database="fresh_fixture")

    # An oversized encoded row must fail instead of storing a digest of a partial row.
    packet_limit = int(sql("SELECT @@global.max_allowed_packet").stdout)
    sql("SET GLOBAL max_allowed_packet=32768")
    try:
        sql("USE fixture; CREATE TABLE wide(v BLOB); INSERT INTO wide VALUES(REPEAT('x',20000))")
        failed = sql("USE fixture; CALL store_query('SELECT v FROM wide','SELECT v FROM wide',NOW(),@id)", expect=False)
        assert failed.returncode != 0, "Oversized row was given a result hash"
        assert sql("SELECT COUNT(*) FROM fixture.qs_queries WHERE query='SELECT v FROM wide'").stdout.strip() == "0"
    finally:
        sql(f"SET GLOBAL max_allowed_packet={packet_limit}")

    preserved = sql("""USE fixture;
        CREATE TEMPORARY TABLE _dbrepo_hash_rows(marker INT);
        INSERT INTO _dbrepo_hash_rows VALUES(137);
        SET SESSION group_concat_max_len=512;
        DELIMITER $$
        BEGIN NOT ATOMIC
            DECLARE CONTINUE HANDLER FOR SQLEXCEPTION SET @failed=1;
            CALL hash_query('SELECT 13 AS sample', @h);
        END$$
        DELIMITER ;
        SELECT @failed,marker,@@session.group_concat_max_len FROM _dbrepo_hash_rows;
    """)
    assert preserved.stdout.strip() == "1\t137\t512"


def main():
    name = "dbrepo-query-work-" + secrets.token_hex(6)
    password = secrets.token_urlsafe(24)
    env = {**os.environ, "MARIADB_ROOT_PASSWORD":password}
    def sql(text, expect=True, user="root"):
        # The temporary bootstrap server has no TCP listener.
        result = subprocess.run(["docker", "exec", "-i", "-e", "MYSQL_PWD", name,
            "mariadb", "--protocol=TCP", "--host=127.0.0.1", "-u"+user, "--batch", "--skip-column-names"],
            env={**os.environ,"MYSQL_PWD":password},input=text, text=True,capture_output=True,timeout=45)
        if expect and result.returncode:
            raise AssertionError("MariaDB fixture operation failed: " + result.stderr.replace(password,"[redacted]")[:500])
        return result
    subprocess.run(["docker","run","-d","--name",name,"--memory=512m","-e","MARIADB_ROOT_PASSWORD", "mariadb:10.11"],
                   env=env,check=True,stdout=subprocess.DEVNULL,stderr=subprocess.DEVNULL)
    try:
        for _ in range(60):
            if sql("SELECT 1",expect=False).returncode==0:break
            time.sleep(1)
        else:raise RuntimeError("MariaDB fixture did not start")
        sql("CREATE DATABASE fixture; USE fixture; CREATE TABLE _tmp(marker INT); INSERT INTO _tmp VALUES(94817);")
        fixture=(ROOT/"dbrepo-data-service/rest-service/src/test/resources/init/querystore.sql").read_text()
        sql("USE fixture;\nDELIMITER $$\n"+"\n".join(line.rstrip(";")+"$$" for line in fixture.splitlines() if line.strip())+"\nDELIMITER ;")
        sql("""USE fixture;
            CREATE PROCEDURE hash_table(IN name VARCHAR(255), OUT h VARCHAR(255), OUT n BIGINT)
                SELECT SHA2('abc',256),1 INTO h,n;
            INSERT INTO qs_queries (id,query,query_normalized,is_persisted,query_hash,result_hash,result_number)
                VALUES ('00000000-0000-0000-0000-000000000137','SELECT a, b FROM samples','SELECT a, b FROM samples',
                        TRUE,SHA2('SELECT a, b FROM samples',256),SHA2('abc',256),1)""")
        spec=importlib.util.spec_from_file_location("migration",ROOT/".scripts/query-worktables-migration.py")
        module=importlib.util.module_from_spec(spec);spec.loader.exec_module(module)
        sql(module.migration(["fixture"]))
        for call in ["CALL hash_query('SELECT 1 AS sample', @h); SELECT @h,@count;",
                     "CALL store_query('SELECT 2 AS sample','SELECT 2 AS sample',NOW(),@id); SELECT result_hash,result_number FROM qs_queries WHERE id=@id;",
                     "CALL _store_query('fixture','SELECT 3 AS sample','SELECT 3 AS sample',NOW(),@id); SELECT result_hash,result_number FROM qs_queries WHERE id=@id;"]:
            out=sql("USE fixture; "+call).stdout.strip().split("\t")
            assert out[-1]=="1"
            assert sql("SELECT marker FROM fixture._tmp").stdout.strip()=="94817"
        sql("CREATE DATABASE fresh_fixture; USE fresh_fixture; " + fixture)
        sql("USE fresh_fixture; " + (module.RESOURCES / "create_metadata_lock.sql").read_text())
        for routine in module.ROUTINES:
            ddl = (module.RESOURCES / (routine + ".sql")).read_text().strip().rstrip(";")
            sql("USE fresh_fixture;\nDELIMITER $$\n" + ddl + "$$\nDELIMITER ;")
        # Nested INVOKER hashing uses the execution context of each supported wrapper.
        sql("CREATE USER wrapper_fixture@'%' IDENTIFIED BY '" + password + "'")
        for schema in ("fixture", "fresh_fixture"):
            security = sql(f"SELECT SECURITY_TYPE FROM information_schema.ROUTINES WHERE ROUTINE_SCHEMA='{schema}' "
                           "AND ROUTINE_NAME='hash_table'")
            assert security.stdout.strip() == "INVOKER"
            for routine in ("hash_query", "store_query", "_store_query"):
                sql(f"GRANT EXECUTE ON PROCEDURE {schema}.{routine} TO wrapper_fixture@'%'")
            checks = sql(f"USE {schema}; "
                         "CALL hash_query('SELECT 317 AS sample',@digest); "
                         "CALL store_query('SELECT 317 AS sample','SELECT 317 AS sample',NOW(),@public); "
                         "CALL _store_query('fixture','SELECT 317 AS sample','SELECT 317 AS sample',NOW(),@internal); "
                         "SELECT @digest REGEXP '^v2:[0-9a-f]{64}$',@public=@internal",
                         user="wrapper_fixture")
            assert checks.stdout.strip() == "1\t1"
        check_result_hashes(sql)
        # Make metadata inserts overlap instead of relying on runner timing.
        sql("USE fixture; CREATE TRIGGER fixture_overlap BEFORE INSERT ON qs_queries FOR EACH ROW DO SLEEP(0.15)")
        expected = [sql(f"USE fixture; CALL hash_query('SELECT {n} AS sample',@h); SELECT @h,@count").stdout.strip()
                    for n in range(12)]
        assert len(set(expected)) == 12
        def hashed(value):
            query=f"SELECT {value} AS sample WHERE SLEEP(0.03)=0"
            if value % 3 == 0:
                call="CALL hash_query('"+query+"',@h); SELECT @h,@count;"
            elif value % 3 == 1:
                call="CALL store_query('"+query+"','"+query+"',NOW(),@id); SELECT result_hash,result_number FROM qs_queries WHERE id=@id;"
            else:
                call="CALL _store_query('fixture','"+query+"','"+query+"',NOW(),@id); SELECT result_hash,result_number FROM qs_queries WHERE id=@id;"
            out=sql("USE fixture; "+call).stdout.strip()
            assert out==expected[value], "Concurrent result differs"
        with ThreadPoolExecutor(max_workers=4) as pool:list(pool.map(hashed,range(12)))
        for empty in (False,True):
            query="SELECT 77 AS sample"+(" WHERE 1=0" if empty else "")
            def persisted(_):
                return sql("USE fixture; CALL store_query('"+query+"','"+query+"',NOW(),@id); SELECT @id").stdout.strip()
            with ThreadPoolExecutor(max_workers=4) as pool: ids=list(pool.map(persisted,range(8)))
            assert len(set(ids))==1 and ids[0] not in ("","NULL"), "Concurrent persistence duplicated query identity"
            assert sql("SELECT COUNT(*) FROM fixture.qs_queries WHERE id='"+ids[0]+"'").stdout.strip()=="1"
        sql("USE fixture; DROP TRIGGER fixture_overlap")
        sql("CREATE DATABASE other_fixture; CREATE USER outsider@'%' IDENTIFIED BY '"+password+"'; GRANT SELECT ON other_fixture.* TO outsider@'%'")
        denied=sql("START TRANSACTION; SELECT id FROM fixture.qs_metadata_lock FOR UPDATE",expect=False,user="outsider")
        assert denied.returncode != 0 and "1142" in denied.stderr, "Unrelated SQL user reached the metadata guard"
        # Holding the discarded server-global name cannot obstruct the row-locked implementation.
        lock="dbrepo:qs:"+hashlib.sha256(b"fixture").hexdigest()[:54]
        with ThreadPoolExecutor(max_workers=1) as pool:
            held=pool.submit(sql,"DO GET_LOCK('"+lock+"',0); DO SLEEP(5); DO RELEASE_LOCK('"+lock+"')",user="outsider")
            for _ in range(100):
                if sql("SELECT IS_USED_LOCK('"+lock+"') IS NOT NULL").stdout.strip()=="1":break
                time.sleep(0.02)
            else:raise AssertionError("Fixture advisory lock was not acquired")
            start=time.monotonic()
            assert sql("USE fixture; CALL store_query('SELECT 900 AS sample','SELECT 900 AS sample',NOW(),@id); SELECT @id IS NOT NULL").stdout.strip()=="1"
            assert time.monotonic()-start < 3, "Unrelated SQL account blocked Query Store via a named lock"
            held.result()
        sql("""USE fixture;
DELIMITER $$
CREATE TRIGGER fixture_failure BEFORE INSERT ON qs_queries FOR EACH ROW
 SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='fixture metadata failure'$$
CREATE PROCEDURE fixture_capture_failure() BEGIN
 DECLARE failed BOOLEAN DEFAULT FALSE;
 DECLARE CONTINUE HANDLER FOR SQLSTATE '45000' SET failed=TRUE;
 CALL store_query('SELECT 901 AS sample','SELECT 901+0*(@fixture_query_calls:=COALESCE(@fixture_query_calls,0)+1) AS sample',NOW(),@id);
 SELECT failed,@fixture_query_calls,@@in_transaction,@@SESSION.innodb_lock_wait_timeout;
END$$
DELIMITER ;""")
        assert sql("USE fixture; SET SESSION innodb_lock_wait_timeout=17; CALL fixture_capture_failure()").stdout.strip()=="1\t1\t0\t17"
        sql("USE fixture; DROP TRIGGER fixture_failure")
        assert sql("USE fixture; CALL _store_query('fixture','SELECT 902 AS sample','SELECT 902+0*(@fixture_query_calls:=COALESCE(@fixture_query_calls,0)+1) AS sample',NOW(),@id); SELECT @fixture_query_calls,@id IS NOT NULL,@@in_transaction,@@SESSION.innodb_lock_wait_timeout").stdout.strip()=="1\t1\t0\t50"
        before_count=sql("SELECT COUNT(*) FROM fixture.qs_queries").stdout.strip()
        with ThreadPoolExecutor(max_workers=1) as pool:
            held=pool.submit(sql,"START TRANSACTION; SELECT id FROM fixture.qs_metadata_lock FOR UPDATE; DO GET_LOCK('fixture-row-held',0); DO SLEEP(13); ROLLBACK")
            for _ in range(100):
                if sql("SELECT IS_USED_LOCK('fixture-row-held') IS NOT NULL").stdout.strip()=="1":break
                time.sleep(0.02)
            else:raise AssertionError("Fixture row lock was not acquired")
            failed=sql("USE fixture; CALL store_query('SELECT 903 AS sample','SELECT 903 AS sample',NOW(),@id)",expect=False)
            assert failed.returncode!=0 and "1205" in failed.stderr, "Contended metadata row must time out"
            held.result()
        assert sql("SELECT COUNT(*) FROM fixture.qs_queries").stdout.strip()==before_count
        # Missing guard data must fail closed, never persist without coordination.
        sql("DELETE FROM fixture.qs_metadata_lock")
        failed=sql("USE fixture; CALL store_query('SELECT 904 AS sample','SELECT 904 AS sample',NOW(),@id)",expect=False)
        assert failed.returncode != 0 and "lock row is missing" in failed.stderr
        assert sql("SELECT COUNT(*) FROM fixture.qs_queries").stdout.strip()==before_count
        sql("INSERT INTO fixture.qs_metadata_lock VALUES(1)")
        sql("USE fixture; CREATE OR REPLACE PROCEDURE hash_table(IN n VARCHAR(255), OUT h VARCHAR(255), OUT c BIGINT) SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='fixture hash failure';")
        for call in ["CALL hash_query('SELECT 1 AS sample',@h)",
                     "CALL store_query('SELECT 1 AS sample','SELECT 1 AS sample',NOW(),@id)",
                     "CALL _store_query('fixture','SELECT 1 AS sample','SELECT 1 AS sample',NOW(),@id)"]:
            assert sql("USE fixture; "+call,expect=False).returncode != 0
        assert sql("SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='fixture' AND table_name LIKE '_dbrepo_hash_%'").stdout.strip()=="0"
        assert sql("SELECT marker FROM fixture._tmp").stdout.strip()=="94817"
        # Routine replacement is repeatable and does not reopen ordinary SQL-user EXECUTE access.
        sql("CREATE USER reader@'%' IDENTIFIED BY '"+password+"'; GRANT SELECT ON fixture.* TO reader@'%';")
        sql("CREATE USER service_fixture@'%' IDENTIFIED BY '"+password+"'; GRANT EXECUTE ON PROCEDURE fixture.hash_query TO service_fixture@'%';")
        metadata="SELECT ROUTINE_NAME,DEFINER,SQL_MODE,SECURITY_TYPE,CHARACTER_SET_CLIENT,COLLATION_CONNECTION FROM information_schema.ROUTINES WHERE ROUTINE_SCHEMA='fixture' AND ROUTINE_NAME IN ('store_query','_store_query','hash_query','hash_table') ORDER BY ROUTINE_NAME"
        sql("USE fixture; ALTER PROCEDURE store_query SQL SECURITY INVOKER")
        before = [row.split("\t") for row in sql(metadata).stdout.splitlines()]
        for row in before:
            if row[0] == "hash_table":
                assert row[3] == "DEFINER"  # The injected failure stub models a legacy helper.
                row[3] = "INVOKER"
        sql(module.migration(["fixture"]))
        assert [row.split("\t") for row in sql(metadata).stdout.splitlines()] == before
        denied=sql("CALL fixture.hash_query('SELECT 1 AS sample',@h)",expect=False,user="reader")
        assert denied.returncode != 0 and "1370" in denied.stderr
        allowed=sql("CALL fixture.hash_query('SELECT 1 AS sample',@h)",expect=False,user="service_fixture")
        assert allowed.returncode == 0, "Existing service EXECUTE grant was lost"
        sql("CREATE DATABASE conflict_fixture; CREATE TABLE conflict_fixture.qs_metadata_lock(marker INT); INSERT INTO conflict_fixture.qs_metadata_lock VALUES(9817)")
        conflict=sql(module.migration(["conflict_fixture"]),expect=False)
        assert conflict.returncode != 0 and "conflicts" in conflict.stderr
        assert sql("SELECT marker FROM conflict_fixture.qs_metadata_lock").stdout.strip()=="9817"
        sql("USE fixture; SET SESSION system_versioning_alter_history=KEEP; ALTER TABLE qs_queries ADD COLUMN replication_revision BIGINT NOT NULL DEFAULT 0")
        before=sql("SHOW CREATE PROCEDURE fixture.store_query").stdout
        refused=sql(module.migration(["fixture"]),expect=False)
        assert refused.returncode != 0 and "Replication Query Store" in refused.stderr
        assert sql("SHOW CREATE PROCEDURE fixture.store_query").stdout == before
        print("PASS: complete v2 result hashes, legacy citations, fresh and upgraded stores, isolated work tables, overlapping metadata writes, nullable deduplication, database-scoped row locking, cleanup and migration/grant preservation")
    finally:
        subprocess.run(["docker","rm","-f","-v",name],capture_output=True,timeout=30)


if __name__=="__main__":main()
