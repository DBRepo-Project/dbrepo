#!/usr/bin/env python3
"""Exercise Query Store work-table isolation against a disposable MariaDB."""
from concurrent.futures import ThreadPoolExecutor
import hashlib
import importlib.util
import os
from pathlib import Path
import secrets
import subprocess
import time

ROOT = Path(__file__).resolve().parents[1]


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
        fixture=fixture.replace("DECLARE _sql TEXT; SELECT", "DECLARE _sql TEXT; SET @fixture_hash_calls=COALESCE(@fixture_hash_calls,0)+1; SELECT")
        sql("USE fixture;\nDELIMITER $$\n"+"\n".join(line.rstrip(";")+"$$" for line in fixture.splitlines() if line.strip())+"\nDELIMITER ;")
        spec=importlib.util.spec_from_file_location("migration",ROOT/".scripts/query-worktables-migration.py")
        module=importlib.util.module_from_spec(spec);spec.loader.exec_module(module)
        sql(module.migration(["fixture"]))
        for call in ["CALL hash_query('SELECT 1 AS sample', @h); SELECT @h,@count;",
                     "CALL store_query('SELECT 2 AS sample','SELECT 2 AS sample',NOW(),@id); SELECT result_hash,result_number FROM qs_queries WHERE id=@id;",
                     "CALL _store_query('fixture','SELECT 3 AS sample','SELECT 3 AS sample',NOW(),@id); SELECT result_hash,result_number FROM qs_queries WHERE id=@id;"]:
            out=sql("USE fixture; "+call).stdout.strip().split("\t")
            assert out[-1]=="1"
            assert sql("SELECT marker FROM fixture._tmp").stdout.strip()=="94817"
        # Make metadata inserts overlap instead of relying on runner timing.
        sql("USE fixture; CREATE TRIGGER fixture_overlap BEFORE INSERT ON qs_queries FOR EACH ROW DO SLEEP(0.15)")
        def hashed(value):
            query=f"SELECT {value} AS sample WHERE SLEEP(0.03)=0"
            if value % 3 == 0:
                call="CALL hash_query('"+query+"',@h); SELECT @h,@count;"
            elif value % 3 == 1:
                call="CALL store_query('"+query+"','"+query+"',NOW(),@id); SELECT result_hash,result_number FROM qs_queries WHERE id=@id;"
            else:
                call="CALL _store_query('fixture','"+query+"','"+query+"',NOW(),@id); SELECT result_hash,result_number FROM qs_queries WHERE id=@id;"
            out=sql("USE fixture; "+call).stdout.strip()
            assert out==hashlib.sha256(str(value).encode()).hexdigest()+"\t1", "Concurrent result differs"
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
 CALL store_query('SELECT 901 AS sample','SELECT 901 AS sample',NOW(),@id);
 SELECT failed,@fixture_hash_calls,@@in_transaction,@@SESSION.innodb_lock_wait_timeout;
END$$
DELIMITER ;""")
        assert sql("USE fixture; SET SESSION innodb_lock_wait_timeout=17; CALL fixture_capture_failure()").stdout.strip()=="1\t1\t0\t17"
        sql("USE fixture; DROP TRIGGER fixture_failure")
        assert sql("USE fixture; CALL _store_query('fixture','SELECT 902 AS sample','SELECT 902 AS sample',NOW(),@id); SELECT @fixture_hash_calls,@id IS NOT NULL,@@in_transaction,@@SESSION.innodb_lock_wait_timeout").stdout.strip()=="1\t1\t0\t50"
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
        metadata="SELECT ROUTINE_NAME,DEFINER,SQL_MODE,SECURITY_TYPE,CHARACTER_SET_CLIENT,COLLATION_CONNECTION FROM information_schema.ROUTINES WHERE ROUTINE_SCHEMA='fixture' AND ROUTINE_NAME IN ('store_query','_store_query','hash_query') ORDER BY ROUTINE_NAME"
        sql("USE fixture; ALTER PROCEDURE store_query SQL SECURITY INVOKER")
        before=sql(metadata).stdout
        sql(module.migration(["fixture"]))
        assert sql(metadata).stdout==before, "Routine execution context changed during migration"
        denied=sql("CALL fixture.hash_query('SELECT 1 AS sample',@h)",expect=False,user="reader")
        assert denied.returncode != 0 and "1370" in denied.stderr
        allowed=sql("CALL fixture.hash_query('SELECT 1 AS sample',@h)",expect=False,user="service_fixture")
        assert "fixture hash failure" in allowed.stderr, "Existing service EXECUTE grant was lost"
        sql("CREATE DATABASE conflict_fixture; CREATE TABLE conflict_fixture.qs_metadata_lock(marker INT); INSERT INTO conflict_fixture.qs_metadata_lock VALUES(9817)")
        conflict=sql(module.migration(["conflict_fixture"]),expect=False)
        assert conflict.returncode != 0 and "conflicts" in conflict.stderr
        assert sql("SELECT marker FROM conflict_fixture.qs_metadata_lock").stdout.strip()=="9817"
        print("PASS: isolated work tables, overlapping metadata writes, nullable deduplication, database-scoped row locking, cleanup and migration/grant preservation")
    finally:
        subprocess.run(["docker","rm","-f","-v",name],capture_output=True,timeout=30)


if __name__=="__main__":main()
