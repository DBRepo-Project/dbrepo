#!/usr/bin/env python3
"""Manifest-scoped SQL observations and opt-in, self-expiring fixture write faults."""
import argparse
import json
from pathlib import Path
import re
import shlex
import subprocess
import sys
import time
import uuid


class Blocked(Exception):
    pass


def require(condition, message):
    if not condition:
        raise Blocked(message)


def validate(scope, report, site):
    common = {"action", "run", "database", "database_id", "table", "table_id", "source_database_id", "source_table_id"}
    fields = {"replication_key", "replica"} if scope["action"] == "observe" else {"policy", "coordinated", "target_url", "ttl_seconds"}
    require(set(scope) == common | fields, "Unexpected scope fields")
    run = scope["run"]
    require(re.fullmatch(r"replication_acceptance_[a-f0-9]{16}", run) is not None
            and report["run"] == run, "Not this run's unique fixture database")
    require(re.fullmatch(r"[a-z][a-z0-9_]{0,63}", scope["database"]) is not None, "Unsafe database name")
    require(re.fullmatch(r"[a-z][a-z0-9_]{0,63}", scope["table"]) is not None, "Unsafe table name")
    for field in ("database_id", "table_id", "source_database_id", "source_table_id"):
        require(str(uuid.UUID(scope[field])) == scope[field], "Noncanonical fixture UUID")
    resources = report["resources"]
    require(any(r["site"] == site and r["kind"] == "database" and r["name"] == run
                and (r.get("internal_name") or r["name"]) == scope["database"]
                and r["id"] == scope["database_id"] for r in resources),
            "Site/database absent from the harness manifest")
    require(any(r["site"] == site and r["kind"] == "table" and r["id"] == scope["table_id"]
                and (r.get("internal_name") or r["name"]) == scope["table"] for r in resources), "Table absent from the harness manifest")
    for field, kind in (("source_database_id", "database"), ("source_table_id", "table")):
        require(any(r["kind"] == kind and r["id"] == scope[field] for r in resources), "Source absent from manifest")
    require(scope["action"] in ("observe", "block", "unblock"), "Unsupported action")
    if scope["action"] == "observe":
        require(str(uuid.UUID(scope["replication_key"])) == scope["replication_key"], "Invalid fixture key")
        require(type(scope["replica"]) is bool, "Explicit replica observation flag required")
    else:
        require(scope["target_url"] == report["sites"][site], "Fault target origin differs from manifest")
        require(scope.get("coordinated") is True and scope["policy"] in ("all", "latest-only", "delete-only"),
                "Fault needs coordination and an explicit fixture policy")
        require(type(scope["ttl_seconds"]) is int and 1 <= scope["ttl_seconds"] <= 7260, "Invalid fault TTL")
        require(scope["action"] == "unblock" or report["status"] == "RUNNING", "Only an active run may enable faults")
        require(scope["table"] in ("outage", "reorder", "resurrection"), "Faults are restricted to dedicated scenario tables")


def observe_sql(scope):
    db, table, key = scope["database"], scope["table"], scope["replication_key"]
    where = ("table_id='%s' AND source_database_id='%s' AND source_table_id='%s'" %
             (scope["table_id"], scope["source_database_id"], scope["source_table_id"]))
    sql = f"""START TRANSACTION READ ONLY;
SELECT JSON_OBJECT('kind','history','id',h.id,'value',h.value,
 'start',DATE_FORMAT(h.row_start,'%Y-%m-%d %H:%i:%s.%f'),
 'end',IF(c.row_start IS NOT NULL,NULL,DATE_FORMAT(h.row_end,'%Y-%m-%d %H:%i:%s.%f')))
 FROM `{db}`.`{table}` FOR SYSTEM_TIME ALL AS h
 LEFT JOIN `{db}`.`{table}` AS c ON c.replication_key=h.replication_key AND c.row_start=h.row_start AND c.row_end=h.row_end
 WHERE h.replication_key='{key}' ORDER BY h.row_start LIMIT 101;
SELECT JSON_OBJECT('kind','timestamps','site',site_url,'key',replication_id,'database',database_id,'table',table_id,
 'start',DATE_FORMAT(row_start,'%Y-%m-%d %H:%i:%s.%f'),
 'end',DATE_FORMAT(row_end,'%Y-%m-%d %H:%i:%s.%f'))
 FROM `{db}`.tuple_replication_timestamps WHERE replication_id='{key}'
 ORDER BY site_url,database_id,table_id,row_start LIMIT 101;
"""
    if scope["replica"]:
        sql += f"""SELECT JSON_OBJECT('kind','inbox','event_id',event_id,'table_id',table_id,
 'source_database_id',source_database_id,'source_table_id',source_table_id,'sequence',event_sequence,
 'payload',JSON_QUERY(payload,'$'),'receipt',JSON_QUERY(receipt,'$'))
 FROM `{db}`.tuple_replication_inbox WHERE {where}
 AND JSON_VALUE(payload,'$.tuple.replicationKey')='{key}' ORDER BY event_sequence LIMIT 101;
SELECT JSON_OBJECT('kind','heads','sequence',event_sequence) FROM `{db}`.tuple_replication_heads
 WHERE {where} AND replication_key=BINARY '{key}' LIMIT 2;
"""
    return sql + "COMMIT;\n"


def observations(raw):
    result = {kind: [] for kind in ("history", "timestamps", "inbox", "heads")}
    for line in raw.splitlines():
        row = json.loads(line)
        kind = row.pop("kind")
        require(kind in result and len(result[kind]) < 100, "Unbounded/unknown fixture observation")
        result[kind].append(row)
    return result


def trigger_name(scope, operation):
    return "acc_" + uuid.UUID(scope["table_id"]).hex + "_" + operation.lower()


def trigger_body(scope, operation, expires):
    condition = "TRUE"
    if scope["policy"] == "latest-only" and operation != "DELETE":
        condition = "NOT (NEW.value <=> 'latest')"
    elif scope["policy"] != "all" and operation == "DELETE":
        condition = "FALSE"
    return ("BEGIN IF UNIX_TIMESTAMP() < %d AND (%s) THEN SIGNAL SQLSTATE '45000' "
            "SET MESSAGE_TEXT = 'acceptance:%s'; END IF; END" % (expires, condition, scope["run"]))


def fault_sql(scope, expires, existing):
    db, table = scope["database"], scope["table"]
    require(len(existing) <= 3, "Unexpected fault trigger inventory")
    known = {}
    for trigger in existing:
        operation = trigger["event"]
        match = re.search(r"UNIX_TIMESTAMP\(\) < ([0-9]+)", trigger["body"])
        require(operation in ("INSERT", "UPDATE", "DELETE") and match is not None, "Unknown trigger; refusing mutation")
        require(trigger["name"] == trigger_name(scope, operation) and trigger["table"] == table
                and trigger["timing"] == "BEFORE"
                and trigger["body"] == trigger_body(scope, operation, int(match.group(1))),
                "Existing trigger is not owned by this exact run/scope/policy")
        known[operation] = trigger
    sql = "DELIMITER //\n"
    for operation in ("INSERT", "UPDATE", "DELETE"):
        name = trigger_name(scope, operation)
        if scope["action"] == "unblock":
            if operation in known:
                sql += f"DROP TRIGGER `{db}`.`{name}`//\n"
        elif operation not in known:
            sql += (f"CREATE TRIGGER `{db}`.`{name}` BEFORE {operation} ON `{db}`.`{table}` "
                    f"FOR EACH ROW {trigger_body(scope, operation, expires)}//\n")
        else:
            require(int(re.search(r"UNIX_TIMESTAMP\(\) < ([0-9]+)", known[operation]["body"]).group(1)) > time.time(),
                    "Existing fault already expired; unblock before creating a new fault")
    return sql + "DELIMITER ;\n"


def execute(args, sql):
    require(args.ssh and not args.ssh.startswith("-") and re.fullmatch(r"[a-zA-Z0-9_.@:-]+", args.ssh),
            "Explicit SSH destination required")
    require(args.container and re.fullmatch(r"[a-zA-Z0-9_-]+", args.container), "Explicit SQL container required")
    require(args.metadata_container and re.fullmatch(r"[a-zA-Z0-9_-]+", args.metadata_container),
            "Explicit metadata container required to verify the SSH site's BASE_URL")
    # Reads an existing container secret without exporting it to this process, argv, or evidence.
    remote = ["sh", "-c", 'origin=$(docker exec "$1" printenv BASE_URL) || exit 2; '
              '[ "$origin" = "$2" ] || exit 2; shift 2; exec "$@"', "sh", args.metadata_container, args.site_url,
              "docker", "exec", "-i", args.container, "sh", "-c",
              'test -n "$MARIADB_ROOT_PASSWORD" || exit 2; MYSQL_PWD="$MARIADB_ROOT_PASSWORD"; '
              'export MYSQL_PWD; exec mariadb --no-defaults -uroot --batch --raw --skip-column-names']
    argv = ["ssh", "-o", "BatchMode=yes", "-o", "ConnectTimeout=10", args.ssh, shlex.join(remote)]
    try:
        result = subprocess.run(argv, input="SET time_zone='+00:00'; SET SESSION max_statement_time=10; "
                    "SET SESSION lock_wait_timeout=5;\n" + sql, capture_output=True, text=True, timeout=args.timeout)
    except (OSError, subprocess.TimeoutExpired):
        raise Blocked("Scoped SQL command unavailable or timed out; output withheld") from None
    require(result.returncode == 0, "Scoped SQL command failed; output withheld")
    require(len(result.stdout) <= 4 * 1024 * 1024, "Observation exceeded fixture bound")
    return result.stdout


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--report", type=Path, required=True, help="Harness-created resource manifest")
    parser.add_argument("--site", required=True, help="Exact site label in that manifest")
    parser.add_argument("--ssh")
    parser.add_argument("--container")
    parser.add_argument("--metadata-container")
    parser.add_argument("--timeout", type=int, default=15)
    parser.add_argument("--execute", action="store_true", help="Without this, emit a plan and make no SSH/SQL calls")
    parser.add_argument("--allow-scoped-faults", action="store_true")
    args = parser.parse_args()
    try:
        raw = sys.stdin.read(65537)
        require(len(raw) <= 65536 and 1 <= args.timeout <= 120, "Invalid input or timeout bound")
        scope = json.loads(raw)
        report = json.loads(args.report.read_text())
        validate(scope, report, args.site)
        args.site_url = report["sites"][args.site]
        if scope["action"] != "observe":
            require(not args.execute or args.allow_scoped_faults, "Explicit --allow-scoped-faults required")
        if not args.execute:
            sql = observe_sql(scope) if scope["action"] == "observe" else fault_sql(scope, int(time.time()) + scope["ttl_seconds"], [])
            print(json.dumps({"status": "PLAN", "executed": False, "scope": scope, "sql": sql}))
            return 0
        if scope["action"] == "observe":
            print(json.dumps(observations(execute(args, observe_sql(scope)))))
            return 0
        names = ",".join("'%s'" % trigger_name(scope, op) for op in ("INSERT", "UPDATE", "DELETE"))
        inventory = execute(args, "SELECT UNIX_TIMESTAMP();\n"
                "SELECT JSON_OBJECT('name',TRIGGER_NAME,'event',EVENT_MANIPULATION,'table',EVENT_OBJECT_TABLE,"
                "'timing',ACTION_TIMING,'body',ACTION_STATEMENT) FROM information_schema.TRIGGERS "
                "WHERE TRIGGER_SCHEMA='%s' AND TRIGGER_NAME IN (%s);" % (scope["database"], names)).splitlines()
        now = int(inventory[0])
        require(scope["action"] == "unblock" or abs(now - time.time()) < 5,
                "SQL/controller clocks differ; refusing an unreliable fault TTL")
        sql = fault_sql(scope, now + scope["ttl_seconds"], [json.loads(line) for line in inventory[1:]])
        execute(args, sql)
        print(json.dumps(scope))
        return 0
    except Blocked as error:
        print(json.dumps({"status": "BLOCKED", "reason": str(error)}))
        return 2
    except (OSError, ValueError, KeyError, TypeError):
        # Neither a SQL error nor a malformed payload may expose container credentials.
        print(json.dumps({"status": "BLOCKED", "reason": "Scoped SQL precondition/command failed; inspect configuration without printing secrets"}))
        return 2


if __name__ == "__main__":
    raise SystemExit(main())
