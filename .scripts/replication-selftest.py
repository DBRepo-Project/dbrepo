#!/usr/bin/env python3
"""Offline contract/safety tests; these are not three-site acceptance evidence."""
import contextlib
import copy
import importlib.util
import io
import json
import os
from pathlib import Path
import subprocess
import sys
import tempfile
import unittest
from types import SimpleNamespace
from unittest.mock import patch
import uuid

sys.dont_write_bytecode = True
ROOT = Path(__file__).resolve().parent


def load(name):
    spec = importlib.util.spec_from_file_location(name, ROOT / (name + ".py"))
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module


a = load("replication-acceptance")
r = load("replication-restore")


class FakeSite:
    """Tiny API double: fixture CRUD, mappings, persisted selections, archival."""
    peers = []
    corrupt = False

    def __init__(self, config, timeout):
        self.config, self.url, self.username = config, config["url"], "acceptance_user"
        self.db = None
        self.tables, self.subsets, self.history = {}, {}, {}
        self.peers.append(self)

    dbpath = a.Site.dbpath

    def get(self, path, **kwargs):
        return self.request(path, **kwargs)[0]

    def request(self, path, method="GET", body=None, role="user", expected=(200,)):
        status, data, headers = 200, {}, {}
        path = path.split("?")[0]
        primary = self.peers[0]
        if path == "/api/replication/status":
            pass
        elif path.startswith("/api/v1/user/"):
            data = {"username": self.username}
        elif path == "/api/v1/database":
            if method == "GET":
                data = []
            else:
                status = 201
                for site in self.peers:
                    site.db = {"id": str(uuid.uuid4()), "name": body["name"], "internal_name": body["name"],
                               "creation_location": primary.url, "tables": []}
                primary.db["replica_urls"] = {site.url: site.db["id"] for site in self.peers[1:]}
                data = primary.db
        elif path.endswith("/replication-access"):
            status = 202
        elif path == self.dbpath():
            data = dict(self.db, tables=[t for t in self.tables.values() if not t.get("archived_at")])
        elif path.endswith("/table"):
            if self is not primary:
                status = 403
            else:
                status, created = 201, []
                for site in self.peers:
                    table = dict(body, id=str(uuid.uuid4()), internal_name=body["name"], rows=[])
                    table["columns"] = [dict(c, id=str(uuid.uuid4()), internal_name=c["name"]) for c in body["columns"]]
                    site.tables[table["id"]] = table
                    site.history[table["id"]] = []
                    created.append(table)
                created[0]["replica_urls"] = {s.url: t["id"] for s, t in zip(self.peers[1:], created[1:])}
                data = created[0]
        elif "/subset" in path:
            if path.endswith("/subset"):
                status, key = 201, str(uuid.uuid4())
                table = self.tables[body["datasource_ids"][0]]
                data = a.rows(table["rows"])
                self.subsets[key] = {"id": key, "rows": data, "result_hash": "v2:" + a.fingerprint(data), "is_persisted": False}
            else:
                key = path.split("/subset/")[1].split("/")[0]
                if key not in self.subsets:
                    raise a.Failed("Missing canonical subset")
                if method == "PUT":
                    status = 202
                    self.subsets[key]["is_persisted"] = body["persist"]
                data = self.subsets[key]["rows"] if path.endswith("/data") else self.subsets[key]
            headers = {"x-id": key, "x-integrity": "verified", "x-result-hash": self.subsets[key]["result_hash"]}
        else:
            key = path.split("/table/")[1].split("/")[0]
            table = self.tables[key]
            if path.endswith("/history"):
                data = self.history[key]
            elif path.endswith("/data/replicate") and method == "GET":
                data = {"tuples": [{"data": table["rows"][0], "replicationKey": table["rows"][0]["replication_key"],
                                    "insertedAt": "2026-10-03T12:00:00Z"}]}
            elif path.endswith("/data/replicate"):
                status = 201 if method == "POST" else 200
                if self.corrupt:
                    table["rows"] = [body["tuple"]["data"]]
                    self.history[key].append({"event": "INSERT", "total": 1})
            elif path.endswith("/data"):
                if method == "GET":
                    data = table["rows"]
                elif self is not primary:
                    status = 403
                else:
                    status = 201 if method == "POST" else 202
                    for site in self.peers:
                        remote = next(t for t in site.tables.values() if t["name"] == table["name"])
                        if method == "POST":
                            remote["rows"] = [copy.deepcopy(body["data"])]
                        elif method == "PUT":
                            remote["rows"][0].update(body["data"])
                        else:
                            remote["rows"] = []
                        site.history[remote["id"]].append({"event": method, "total": 1})
            elif method == "DELETE":
                status = 202
                for site in self.peers:
                    next(t for t in site.tables.values() if t["name"] == table["name"])["archived_at"] = "2026-10-03T13:00:00Z"
            else:
                data = table
        a.require(status in expected, "Unexpected fake response status")
        return copy.deepcopy(data), headers


class Checks(unittest.TestCase):
    def setUp(self):
        self.tmp = tempfile.TemporaryDirectory()
        self.addCleanup(self.tmp.cleanup)
        self.args = SimpleNamespace(report=Path(self.tmp.name) / "report.json", http_timeout=1, wait=0.01,
                                    poll=0.001, allow_faults=False, deadline=10)
        roles = ["create-table", "insert-table-data", "delete-table-data", "persist-query"]
        self.config = {"dedicated_test_users": True, "primary_container_id": str(uuid.uuid4()),
                       "sites": [{"name": str(i), "url": "https://site%d.invalid" % i,
                                  "attested_user_roles": roles} for i in range(3)]}
        FakeSite.peers, FakeSite.corrupt = [], False

    def runner(self):
        with patch.object(a, "Site", FakeSite):
            return a.Acceptance(self.config, self.args)

    def test_crud_persisted_local_selection_and_archive_contract(self):
        runner = self.runner()
        runner.setup()
        runner.crud()
        for site in runner.sites:
            original = site.request
            def include_archived(path, _site=site, _original=original, **kwargs):
                if path.endswith("?include_archived=true"):
                    return dict(_site.db, tables=list(_site.tables.values())), {}
                return _original(path, **kwargs)
            site.request = include_archived
        runner.archive()
        self.assertEqual(3, len(runner.subsets))
        with self.assertRaises(a.Failed):
            runner.canonical_subset()  # Locally equal selections are NOT canonical replication.

    def test_duplicate_reorder_resurrection_checks_detect_mutation(self):
        runner = self.runner()
        runner.setup()
        for scenario in ("duplicate", "reorder", "resurrection"):
            with self.subTest(scenario=scenario), patch.object(a.time, "sleep"):
                runner.delivery(scenario)
        FakeSite.corrupt = True
        with self.assertRaises(a.Failed), patch.object(a.time, "sleep"):
            runner.delivery("reorder_bad")

    def test_rejection_contract_and_blocked_outage(self):
        runner = self.runner()
        runner.setup()
        runner.rejection()
        with self.assertRaises(a.Blocked):
            runner.outage()
        with contextlib.redirect_stdout(io.StringIO()):
            self.assertFalse(runner.check("outage", runner.outage))
        self.assertEqual("BLOCKED", runner.report["checks"][-1]["status"])

    def test_only_exact_fixture_queue_entries_can_be_retried(self):
        runner = self.runner()
        runner.setup()
        tables = runner.table("outage")
        scoped = {"localDatabaseId": runner.primary.db["id"], "localTableId": tables[0]["id"],
                  "targetSiteUrl": runner.sites[2].url}
        entries = [scoped, dict(scoped, localDatabaseId=str(uuid.uuid4())),
                   dict(scoped, localTableId=str(uuid.uuid4())), dict(scoped, targetSiteUrl=runner.sites[1].url)]
        with patch.object(runner.primary, "get", return_value=entries):
            self.assertEqual([scoped], runner.queue(tables))

    def test_retry_false_is_not_success_and_unblock_runs_on_failure(self):
        runner = self.runner()
        runner.setup()
        self.args.allow_faults = True
        self.config.update(isolated_fault_environment=True, fault_hook=["unused"])
        with patch.object(runner, "fault") as fault, patch.object(runner, "write", side_effect=a.Failed("write failed")):
            with self.assertRaises(a.Failed):
                runner.outage()
            self.assertEqual(["block", "unblock"], [c.args[0] for c in fault.call_args_list])
        pending = {"id": str(uuid.uuid4()), "status": "PENDING", "createdAt": "2026-10-03T12:00:00Z"}
        with patch.object(runner, "queue", return_value=[pending]), patch.object(runner.primary, "get", return_value={"retried": False}):
            with self.assertRaisesRegex(a.Failed, "retried=false"):
                runner.retry([])

    def test_bounded_wait_and_unverified_fixity_fail(self):
        runner = self.runner()
        with self.assertRaises(a.Failed):
            runner.wait(lambda: False, "nonconvergence")
        runner.setup()
        runner.crud()
        subset = dict(runner.subsets[0], hash="v2:wrong")
        with self.assertRaises(a.Failed):
            runner.replay(0, subset)

    def test_history_gate_rejects_missing_site_versions(self):
        runner = self.runner()
        runner.setup()
        runner.outage_tables = runner.table("outage")
        runner.outage_key = str(uuid.uuid4())
        historical = [{"id": 1, "value": "original"}, {"id": 1, "value": "changed"}]
        maps = [{"site": s.url, "start": start, "end": "closed"} for s in runner.sites for start in ("one", "two")]
        for site in runner.sites:
            site.config["sql_observer"] = ["unused"]
        with patch.object(a, "command", return_value="\n".join(map(json.dumps, historical + maps))) as command:
            runner.outage_history()
            self.assertIn("START TRANSACTION READ ONLY", command.call_args.args[1])
            self.assertIn(runner.name, command.call_args.args[1])
        with patch.object(a, "command", return_value="\n".join(map(json.dumps, historical + maps[:-1]))):
            with self.assertRaises(a.Failed):
                runner.outage_history()

    def test_full_report_never_turns_blocked_gates_into_pass(self):
        runner = self.runner()
        with patch.object(runner, "archive"), contextlib.redirect_stdout(io.StringIO()):
            self.assertEqual(1, runner.run())
        report = json.loads(self.args.report.read_text())
        self.assertEqual("FAIL", report["status"])
        self.assertEqual(10, len(report["checks"]))
        self.assertEqual(2, sum(c["status"] == "BLOCKED" for c in report["checks"]))
        self.assertEqual("FAIL", report["checks"][-1]["status"])

    def test_deadline_is_not_swallowed_by_check_or_poll(self):
        runner = self.runner()
        def timed_out():
            raise a.Deadline("deadline")
        with self.assertRaises(a.Deadline):
            runner.check("timeout", timed_out)
        with self.assertRaises(a.Deadline):
            runner.wait(timed_out, "timeout")

    def test_http_secrets_and_redirects(self):
        config = {"name": "test", "url": "https://site.invalid", "user_env": "TEST_USER",
                  "user_password_env": "TEST_PASSWORD", "system_env": "TEST_ADMIN",
                  "system_password_env": "TEST_ADMIN_PASSWORD"}
        env = {"TEST_USER": "acceptance_user", "TEST_PASSWORD": "never-print-me",
               "TEST_ADMIN": "technical", "TEST_ADMIN_PASSWORD": "never-print-admin"}
        with patch.dict(os.environ, env):
            site = a.Site(config, 1)
            self.assertIsNone(a.NoRedirect().redirect_request(None, None, 302, "", {}, "https://other.invalid"))
            with patch.object(site.http, "open", side_effect=OSError("never-print-me")):
                with self.assertRaises(a.Blocked) as raised:
                    site.request("/api/replication/status")
                self.assertNotIn("never-print", str(raised.exception))
            for bad in ("http://site.invalid", "https://user:secret@site.invalid", "https://site.invalid/path"):
                with self.subTest(url=bad), self.assertRaises(a.Blocked):
                    a.Site(dict(config, url=bad), 1)

    def test_no_opt_in_never_reads_config_or_writes_evidence(self):
        for script, argv in [("replication-acceptance.py", ["--config", "/missing", "--report", str(self.args.report)]),
                             ("replication-restore.py", ["--output", str(self.args.report)])]:
            result = subprocess.run([sys.executable, str(ROOT / script), *argv], capture_output=True, text=True, timeout=5)
            self.assertEqual(2, result.returncode)
            self.assertIn("BLOCKED", result.stdout)
            self.assertFalse(self.args.report.exists())

    def test_restore_loopback_namespace_and_password_not_in_argv(self):
        args = SimpleNamespace(ssh=None, port=13366, client="mariadb", dump="mariadb-dump", timeout=1)
        db = r.MariaDB(args, 'secret"\\value', self.tmp.name)
        self.assertEqual(0o600, db.defaults.stat().st_mode & 0o777)
        with self.assertRaises(r.Blocked):
            db.sql("SELECT 1", "research")
        with patch.object(r, "run", return_value=b"") as run:
            db.sql("SELECT 1", "acceptance_restore_test_012345abcdef_source")
            argv = run.call_args.args[0]
            self.assertIn("--host=127.0.0.1", argv)
            self.assertNotIn("secret", " ".join(argv))
        args.port = 3306
        with self.assertRaises(r.Blocked):
            r.MariaDB(args, "secret", self.tmp.name)

    def test_restore_ssh_sends_secret_only_over_stdin_and_redacts_errors(self):
        args = SimpleNamespace(ssh="lab", container="acceptance-test", client="mariadb", dump="mariadb-dump", timeout=1)
        db = r.MariaDB(args, "hidden-password", self.tmp.name)
        with patch.object(r, "run", return_value=b"") as run:
            db.sql("SELECT 1;")
            argv, stdin, timeout = run.call_args.args
            self.assertNotIn("hidden-password", " ".join(argv))
            self.assertTrue(stdin.startswith(b"hidden-password\n"))
            self.assertIn("acceptance-test", argv[-1])
        with patch.object(r.subprocess, "run", return_value=SimpleNamespace(returncode=1, stderr=b"hidden-password")):
            with self.assertRaises(r.Blocked) as raised:
                r.run(["unused"], b"", 1)
            self.assertNotIn("hidden-password", str(raised.exception))

    def test_restore_verifier_requires_history_dump_and_exact_periods(self):
        class Database:
            corrupt = False

            def sql(self, statement, database=None):
                if statement == "SELECT VERSION();":
                    return b"11.3.2-MariaDB\n"
                if statement == "SELECT selected_at FROM saved_selection;":
                    return b"2026-10-03 12:00:00.000001\n"
                if statement.startswith("SELECT id,value,row_start,row_end"):
                    return b"wrong periods" if self.corrupt and database.endswith("_restored") else b"version\n" * 7
                if statement.startswith("SELECT o.id"):
                    return b"1\toriginal\toriginal-label\n2\tretained\tretained-label\n"
                if statement.startswith("SELECT id,value FROM"):
                    return b"2\tretained\n"
                return b""

            def command(self, dump=False, extra=()):
                if extra == ["--help"]:
                    return b"--dump-history"
                assert dump and "--dump-history" in extra and "--databases" not in extra
                return b"-- fixture dump\n"

        database = Database()
        report = {}
        r.verify(database, Path(self.tmp.name), report)
        self.assertEqual("PASS", report["status"])
        self.assertEqual(2, len(report["created_databases"]))
        database.corrupt = True
        with self.assertRaises(AssertionError):
            r.verify(database, Path(self.tmp.name), {})


if __name__ == "__main__":
    unittest.main(verbosity=2)
