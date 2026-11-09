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
        result = subprocess.run(["docker", "exec", "-i", "-e", "MYSQL_PWD", name,
            "mariadb", "-u"+user, "--batch", "--skip-column-names"],
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
        spec=importlib.util.spec_from_file_location("migration",ROOT/".scripts/query-worktables-migration.py")
        module=importlib.util.module_from_spec(spec);spec.loader.exec_module(module)
        sql(module.migration(["fixture"]))
        for call in ["CALL hash_query('SELECT 1 AS sample', @h); SELECT @h,@count;",
                     "CALL store_query('SELECT 2 AS sample','SELECT 2 AS sample',NOW(),@id); SELECT result_hash,result_number FROM qs_queries WHERE id=@id;",
                     "CALL _store_query('fixture','SELECT 3 AS sample','SELECT 3 AS sample',NOW(),@id); SELECT result_hash,result_number FROM qs_queries WHERE id=@id;"]:
            out=sql("USE fixture; "+call).stdout.strip().split("\t")
            assert out[-1]=="1"
            assert sql("SELECT marker FROM fixture._tmp").stdout.strip()=="94817"
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
        print("PASS: all three callers preserve _tmp, concurrent results stay isolated, failures clean up, migration repeats and reader EXECUTE stays denied")
    finally:
        subprocess.run(["docker","rm","-f","-v",name],capture_output=True,timeout=30)


if __name__=="__main__":main()
