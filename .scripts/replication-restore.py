#!/usr/bin/env python3
"""Prove MariaDB history dump/restore using new disposable databases only."""
import argparse
import hashlib
import json
import os
from pathlib import Path
import re
import shlex
import subprocess
import tempfile
import uuid


class Blocked(Exception):
    pass


def run(args, stdin, timeout):
    try:
        result = subprocess.run(args, input=stdin, stdout=subprocess.PIPE,
                                stderr=subprocess.PIPE, timeout=timeout, check=False)
    except (OSError, subprocess.TimeoutExpired):
        raise Blocked("SQL client unavailable or timed out; check isolated endpoint") from None
    if result.returncode:
        # Client stderr may contain connection credentials or SQL data.
        raise Blocked("SQL client failed (exit %d); stderr withheld" % result.returncode)
    return result.stdout


class MariaDB:
    def __init__(self, args, password, directory):
        self.args, self.password = args, password
        self.defaults = Path(directory) / "client.cnf"
        if not args.ssh:
            if not 1024 <= args.port <= 65535 or args.port == 3306:
                raise Blocked("Use a dedicated loopback test port, never port 3306")
            quoted = password.replace("\\", "\\\\").replace('"', '\\"')
            self.defaults.write_text('[client]\npassword="' + quoted + '"\n')
            self.defaults.chmod(0o600)

    def command(self, dump=False, extra=(), sql=b""):
        program = self.args.dump if dump else self.args.client
        if self.args.ssh:
            # The password travels on encrypted stdin, never in argv or a log.
            remote = ["docker", "exec", "-i", self.args.container, "sh", "-c",
                      'IFS= read -r MYSQL_PWD; export MYSQL_PWD; exec "$@"', "sh",
                      program, "--no-defaults", "--user=root", *extra]
            argv = ["ssh", "-o", "BatchMode=yes", "-o", "ConnectTimeout=10",
                    self.args.ssh, shlex.join(remote)]
            stdin = self.password.encode() + b"\n" + sql
        else:
            argv = [program, "--defaults-extra-file=" + str(self.defaults),
                    "--protocol=tcp", "--host=127.0.0.1", "--port=" + str(self.args.port),
                    "--user=root", *extra]
            stdin = sql
        return run(argv, stdin, self.args.timeout)

    def sql(self, statement, database=None):
        extra = ["--batch", "--skip-column-names", "--raw"]
        if database:
            if not re.fullmatch(r"acceptance_restore_test_[a-f0-9]{12}_(source|restored)", database):
                raise Blocked("Refusing database outside this probe's namespace")
            extra.append(database)
        return self.command(extra=extra, sql=("SET time_zone='+00:00';\n" + statement).encode())


def digest(data):
    return hashlib.sha256(data).hexdigest()


def verify(db, output, report):
    version = db.sql("SELECT VERSION();").decode().strip()
    if not re.fullmatch(r"[0-9][A-Za-z0-9.+:~_-]*MariaDB[A-Za-z0-9.+:~_-]*", version):
        raise Blocked("The test endpoint must be MariaDB")
    if b"--dump-history" not in db.command(dump=True, extra=["--help"]):
        raise Blocked("mariadb-dump must support --dump-history (10.11+)")
    report["server_version"] = version
    name = "acceptance_restore_test_" + uuid.uuid4().hex[:12]
    source, restored = name + "_source", name + "_restored"
    # No IF NOT EXISTS and no DROP: collision or partial failure cannot overwrite data.
    for database in (source, restored):
        db.sql("CREATE DATABASE `" + database + "`;")
        report.setdefault("created_databases", []).append(database)
        (output / "report.json").write_text(json.dumps(report, indent=2) + "\n")
    db.sql("""
CREATE TABLE observation (id INT PRIMARY KEY, value VARCHAR(64),
 row_start TIMESTAMP(6) GENERATED ALWAYS AS ROW START,
 row_end TIMESTAMP(6) GENERATED ALWAYS AS ROW END,
 PERIOD FOR SYSTEM_TIME(row_start,row_end)) WITH SYSTEM VERSIONING;
CREATE TABLE label (id INT PRIMARY KEY, value VARCHAR(64)) WITH SYSTEM VERSIONING;
CREATE TABLE saved_selection (selected_at DATETIME(6) NOT NULL);
INSERT INTO observation(id,value) VALUES (1,'original'),(2,'retained');
INSERT INTO label VALUES (1,'original-label'),(2,'retained-label');
DO SLEEP(0.02);
INSERT INTO saved_selection VALUES (NOW(6));
DO SLEEP(0.02);
UPDATE observation SET value='changed' WHERE id=1;
UPDATE label SET value='changed-label' WHERE id=1;
DO SLEEP(0.02);
DELETE FROM observation WHERE id=1;
DELETE FROM label WHERE id=1;
""", source)
    selected = db.sql("SELECT selected_at FROM saved_selection;", source).decode().strip()
    if not re.fullmatch(r"\d{4}-\d\d-\d\d \d\d:\d\d:\d\d\.\d{6}", selected):
        raise Blocked("Unexpected selection timestamp")
    history = """SELECT id,value,row_start,row_end FROM observation FOR SYSTEM_TIME ALL
ORDER BY id,row_start,row_end;
SELECT id,value,row_start,row_end FROM label FOR SYSTEM_TIME ALL ORDER BY id,row_start,row_end;
SELECT * FROM saved_selection;"""
    historical = ("SELECT o.id,o.value,l.value FROM observation FOR SYSTEM_TIME AS OF TIMESTAMP '"
                  + selected + "' o JOIN label FOR SYSTEM_TIME AS OF TIMESTAMP '" + selected
                  + "' l USING(id) ORDER BY o.id;")
    current = "SELECT id,value FROM observation ORDER BY id; SELECT id,value FROM label ORDER BY id;"
    expected = b"1\toriginal\toriginal-label\n2\tretained\tretained-label\n"
    before = db.sql(history, source)
    if len(before.splitlines()) != 7 or db.sql(historical, source) != expected:
        raise Blocked("Fixture did not retain the expected three versions per table")
    dumped = db.command(dump=True, extra=["--dump-history", "--single-transaction", "--skip-lock-tables",
                                         "--skip-add-locks", "--skip-comments", "--hex-blob",
                                         source, "observation", "label", "saved_selection"])
    (output / "history.sql").write_bytes(dumped)
    db.sql(dumped.decode(), restored)
    after = db.sql(history, restored)
    if before != after or db.sql(historical, restored) != expected or db.sql(current, source) != db.sql(current, restored):
        raise AssertionError("Restored current rows, history timestamps, or historical JOIN differ")
    report.update(status="PASS", history_sha256=digest(before), dump_sha256=digest(dumped),
                  historical_join_sha256=digest(expected), version_rows_per_table=3,
                  selected_at=selected, checks=["all versions and microsecond periods identical",
                                               "deleted row recovered at saved selection time",
                                               "historical JOIN and current rows identical"])


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--allow-isolated-writes", action="store_true")
    parser.add_argument("--password-env", default="ACCEPTANCE_DB_PASSWORD")
    parser.add_argument("--port", type=int, default=13366)
    parser.add_argument("--client", default="mariadb")
    parser.add_argument("--dump", default="mariadb-dump")
    parser.add_argument("--ssh", help="Optional SSH destination of isolated test container")
    parser.add_argument("--container", help="Only acceptance-* or replication-access-test-* containers")
    parser.add_argument("--timeout", type=int, default=60)
    parser.add_argument("--output", type=Path, required=True, help="New, private evidence directory")
    args = parser.parse_args()
    os.umask(0o077)
    report = {"status": "BLOCKED", "created_databases": []}
    owns_output = False
    try:
        if not args.allow_isolated_writes:
            raise Blocked("Explicit --allow-isolated-writes required")
        password = os.environ.get(args.password_env)
        if not password or "\n" in password or "\r" in password:
            raise Blocked("Provide a nonempty single-line password in the selected environment variable")
        if not 1 <= args.timeout <= 300:
            raise Blocked("Timeout must be 1..300 seconds")
        if args.ssh and (args.ssh.startswith("-") or not args.container or not re.fullmatch(
                r"(?:acceptance|replication-access-test)-[a-zA-Z0-9_-]+", args.container)):
            raise Blocked("SSH requires an explicitly named isolated test container")
        args.output.mkdir(mode=0o700, parents=False, exist_ok=False)
        owns_output = True
        with tempfile.TemporaryDirectory(prefix="replication-restore-") as directory:
            verify(MariaDB(args, password, directory), args.output, report)
    except Blocked as error:
        report.update(status="BLOCKED", reason=str(error))
    except AssertionError as error:
        report.update(status="FAIL", reason=str(error))
    except (OSError, ValueError, KeyboardInterrupt):
        report.update(status="BLOCKED", reason="Invalid input, interrupted, or cannot create evidence; details withheld")
    # Do not overwrite an existing evidence directory on preflight failure.
    if owns_output:
        (args.output / "report.json").write_text(json.dumps(report, indent=2) + "\n")
    print(json.dumps(report, indent=2))
    return {"PASS": 0, "FAIL": 1, "BLOCKED": 2}[report["status"]]


if __name__ == "__main__":
    raise SystemExit(main())
