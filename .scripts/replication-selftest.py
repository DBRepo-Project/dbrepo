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
s = load("replication-sql")


class FakeSite:
    """Tiny API double: fixture CRUD, mappings, persisted selections, archival."""
    peers = []
    corrupt = False

    def __init__(self, config, timeout):
        self.config, self.url, self.username = config, config["url"], "acceptance_user"
        self.db = None
        self.tables, self.subsets, self.history = {}, {}, {}
        self.events, self.inbox, self.heads, self.versions = [], {}, {}, {}
        self.fault_policy = None
        self.peers.append(self)

    dbpath = a.Site.dbpath

    def get(self, path, **kwargs):
        return self.request(path, **kwargs)[0]

    def request(self, path, method="GET", body=None, role="user", expected=(200,)):
        status, data, headers = 200, {}, {}
        query = a.urllib.parse.parse_qs(a.urllib.parse.urlsplit(path).query)
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
        elif path.endswith("/replication/journal"):
            after = int(query.get("after", [0])[0])
            through = int(query.get("through", [len(self.events)])[0])
            events = self.events[after:through][:int(query.get("limit", [100])[0])]
            data = {"through": through, "legacyThrough": 0,
                    "nextAfter": after + len(events), "events": events}
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
                    site.versions[table["id"]] = []
                    created.append(table)
                created[0]["replica_urls"] = {s.url: t["id"] for s, t in zip(self.peers[1:], created[1:])}
                data = created[0]
        elif "/subset" in path:
            if path.endswith("/subset"):
                status, key = 201, str(uuid.uuid4())
                table = self.tables[body["datasource_ids"][0]]
                data = a.rows(table["rows"])
                self.subsets[key] = {"id": key, "rows": data, "result_hash": "v2:" + a.fingerprint(data),
                                     "snapshot_hash": a.fingerprint(data), "is_persisted": False}
            else:
                key = path.split("/subset/")[1].split("/")[0]
                if key not in self.subsets:
                    raise a.Failed("Missing canonical subset")
                if method == "PUT":
                    status = 202
                    self.subsets[key]["is_persisted"] = body["persist"]
                data = self.subsets[key]["rows"] if path.endswith("/data") else self.subsets[key]
            headers = {"x-id": key, "x-integrity": "verified", "x-result-hash": self.subsets[key]["result_hash"],
                       "x-result-mode": "immutable-snapshot"}
        else:
            key = path.split("/table/")[1].split("/")[0]
            table = self.tables[key]
            if path.endswith("/history"):
                data = self.history[key]
            elif path.endswith("/data/replicate") and method == "GET":
                raise AssertionError("Current rows must never be used to invent a retained event")
            elif path.endswith("/data/replicate"):
                status = 201 if method == "POST" else 200
                data = self.apply(table, {"method": method, "payload": body})
            elif path.endswith("/data"):
                if method == "GET":
                    data = table["rows"]
                elif self is not primary:
                    status = 403
                else:
                    status = 201 if method == "POST" else 202
                    value = copy.deepcopy(body["data"] if method == "POST" else table["rows"][0])
                    if method == "PUT":
                        value.update(body["data"])
                    sequence = len(self.events) + 1
                    instant = "2026-10-03T12:00:%02dZ" % sequence
                    payload = {"eventId": str(uuid.uuid4()), "eventSequence": sequence,
                        "database": {k: copy.deepcopy(v) for k, v in primary.db.items() if k != "tables"},
                        "table": {k: copy.deepcopy(v) for k, v in table.items() if k != "rows"},
                        "tuple": {"data": value, "replicationKey": value["replication_key"],
                                  "insertedAt": instant, "deletedAt": instant if method == "DELETE" else None,
                                  "applied": None}}
                    event = {"method": method, "payload": payload}
                    self.events.append(copy.deepcopy(event))
                    for site in self.peers:
                        remote = next(t for t in site.tables.values() if t["name"] == table["name"])
                        site.apply(remote, event)
            elif method == "DELETE":
                status = 202
                for site in self.peers:
                    next(t for t in site.tables.values() if t["name"] == table["name"])["archived_at"] = "2026-10-03T13:00:00Z"
            else:
                data = table
        a.require(status in expected, "Unexpected fake response status")
        return copy.deepcopy(data), headers

    def apply(self, table, event):
        body, method = event["payload"], event["method"]
        event_id, sequence, key = body["eventId"], body["eventSequence"], table["id"]
        if event_id in self.inbox and not self.corrupt:
            return copy.deepcopy(self.inbox[event_id]["receipt"])
        stale = sequence < self.heads.get(key, 0) and not self.corrupt
        if not stale and self.fault_policy and not (
                self.fault_policy == "latest-only" and body["tuple"]["data"]["value"] == "latest"
                or self.fault_policy != "all" and method == "DELETE"):
            return None
        receipt = {"data": {}, "replicationKey": body["tuple"]["replicationKey"],
                   "insertedAt": None, "deletedAt": None, "applied": False}
        if not stale:
            instant = body["tuple"]["insertedAt"]
            if method != "DELETE" or table["rows"]:
                if self.versions[key] and self.versions[key][-1]["end"] is None:
                    self.versions[key][-1]["end"] = a.period(instant)
                if method != "DELETE":
                    self.versions[key].append(dict(body["tuple"]["data"], start=a.period(instant), end=None))
                receipt.update(data=copy.deepcopy(body["tuple"]["data"]), applied=True,
                               insertedAt=instant, deletedAt=instant if method == "DELETE" else None)
                if method == "DELETE":
                    receipt["insertedAt"] = self.versions[key][-1]["start"].replace(" ", "T") + "Z"
                table["rows"] = [] if method == "DELETE" else [copy.deepcopy(body["tuple"]["data"])]
                self.history[key].append({"event": method, "total": 1})
            self.heads[key] = sequence
        self.inbox[event_id] = {"event_id": event_id, "table_id": key, "sequence": sequence,
            "source_database_id": body["database"]["id"], "source_table_id": body["table"]["id"],
            "payload": {"method": method, "sequence": sequence, "target": key, "database": body["database"]["id"],
                        "table": body["table"]["id"], "tuple": copy.deepcopy(body["tuple"])}, "receipt": receipt}
        return copy.deepcopy(receipt)


class Checks(unittest.TestCase):
    def setUp(self):
        self.tmp = tempfile.TemporaryDirectory()
        self.addCleanup(self.tmp.cleanup)
        self.args = SimpleNamespace(report=Path(self.tmp.name) / "report.json", http_timeout=1, wait=0.01,
                                    poll=0.001, allow_faults=False, deadline=10, sync_wait=0.01)
        roles = ["create-table", "insert-table-data", "delete-table-data", "persist-query"]
        self.config = {"dedicated_test_users": True, "strict_snapshot_activation": True, "parent_coordinated": True,
                       "primary_container_id": str(uuid.uuid4()),
                       "sites": [{"name": str(i), "url": "https://site%d.invalid" % i,
                                  "attested_user_roles": roles} for i in range(3)]}
        FakeSite.peers, FakeSite.corrupt = [], False

    def runner(self):
        with patch.object(a, "Site", FakeSite):
            return a.Acceptance(self.config, self.args)

    @staticmethod
    def observed(runner, index, tables, key):
        site, table = runner.sites[index], tables[index]
        timestamps = [{"site": peer.url, "database": peer.db["id"], "table": remote["id"], "key": key,
                       "start": version["start"], "end": version["end"]}
                      for peer, remote in zip(runner.sites, tables) for version in peer.versions[remote["id"]]]
        return copy.deepcopy({"history": site.versions[table["id"]], "timestamps": sorted(timestamps, key=a.canonical),
            "inbox": [r for r in site.inbox.values() if r["table_id"] == table["id"]],
            "heads": [{"sequence": site.heads[table["id"]]}] if table["id"] in site.heads else []})

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
        self.args.allow_faults = True
        self.config.update(scoped_faults_coordinated=True, fault_hook=["unused"])
        def fault(action, tables, policy="all"):
            runner.sites[2].fault_policy = policy if action == "block" else None
        with patch.object(runner, "observe", side_effect=lambda *args: self.observed(runner, *args)), \
                patch.object(runner, "fault", side_effect=fault), patch.object(runner, "retry"):
            for scenario in ("duplicate", "reorder", "resurrection"):
                with self.subTest(scenario=scenario):
                    runner.delivery(scenario)
            FakeSite.corrupt = True
            with self.assertRaises(a.Failed):
                runner.delivery("duplicate")

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

    def test_journal_replay_preserves_identity_method_and_source_payload(self):
        runner = self.runner()
        runner.setup()
        tables = runner.table("duplicate")
        runner.write(tables, "POST", "original")
        runner.write(tables, "PUT", "changed")
        old = runner.exported(tables, "POST")
        update = runner.exported(tables, "PUT", "changed")
        self.assertEqual("POST", old["originalHttpMethod"])
        self.assertEqual("PUT", update["originalHttpMethod"])
        self.assertLess(old["payload"]["eventSequence"], update["payload"]["eventSequence"])
        self.assertNotEqual(old["payload"]["eventId"], update["payload"]["eventId"])
        old["payload"]["database"]["replica_urls"] = {}
        old["payload"]["table"]["replica_urls"] = {}
        original = copy.deepcopy(old)
        hydrated = runner.hydrated(tables, old)
        for field in ("database", "table"):
            self.assertEqual({k: v for k, v in original["payload"][field].items() if k != "replica_urls"},
                             {k: v for k, v in hydrated[field].items() if k != "replica_urls"})
            self.assertEqual(2, len(hydrated[field]["replica_urls"]))
        self.assertEqual(original, old)
        self.assertEqual(old["payload"]["tuple"], hydrated["tuple"])
        with patch.object(runner.sites[1], "get", return_value={}) as send:
            runner.send_event(1, tables, old)
            self.assertEqual("POST", send.call_args.kwargs["method"])
            self.assertEqual(hydrated, send.call_args.kwargs["body"])
            self.assertNotIn(409, send.call_args.kwargs["expected"])

    def test_journal_pins_boundary_and_rejects_gaps_legacy_or_changed_boundary(self):
        runner = self.runner()
        runner.setup()
        tables = runner.table("duplicate")
        runner.write(tables, "POST", "original")
        runner.write(tables, "PUT", "changed")
        events = copy.deepcopy(runner.primary.events)
        pages = [{"through": 2, "legacyThrough": 0, "nextAfter": i + 1, "events": [event]}
                 for i, event in enumerate(events)]
        with patch.object(runner.primary, "get", side_effect=pages) as get:
            self.assertEqual(2, len(runner.journal(tables)))
            self.assertIn("after=1&limit=100&through=2", get.call_args.args[0])
        for corrupt in (dict(pages[0], legacyThrough=1), dict(pages[0], nextAfter=2),
                        dict(pages[0], events=[events[1]]), dict(pages[0], events=[])):
            with self.subTest(page=corrupt), patch.object(runner.primary, "get", return_value=corrupt), self.assertRaises(a.Failed):
                runner.journal(tables)
        with patch.object(runner.primary, "get", side_effect=[pages[0], dict(pages[1], through=3)]), self.assertRaises(a.Failed):
            runner.journal(tables)

    def test_strict_activation_blocks_before_any_fixture_write(self):
        runner = self.runner()
        self.config["strict_snapshot_activation"] = False
        with patch.object(runner.primary, "get") as get, self.assertRaises(a.Blocked):
            runner.setup()
        get.assert_not_called()

    def test_async_history_sync_waits_for_every_exact_returned_job(self):
        runner = self.runner()
        runner.setup()
        runner.core = runner.table("roundtrip")
        for database in (False, True):
            jobs = [str(uuid.uuid4()), str(uuid.uuid4())]
            entries = [{"id": job, "status": "SUCCEEDED", "operationType": "HISTORY_SYNC",
                        "localDatabaseId": runner.primary.db["id"], "localTableId": runner.core[0]["id"],
                        "targetSiteUrl": site.url} for job, site in zip(jobs, runner.sites[1:])]
            pending = copy.deepcopy(entries)
            pending[1]["status"] = "PENDING"
            with patch.object(runner.primary, "get", side_effect=[{"status": "queued", "tables": 1, "jobs": jobs},
                    pending + [{"id": str(uuid.uuid4()), "status": "FAILED"}], entries]) as get, \
                    patch.object(runner, "converged") as converged, \
                    patch.object(runner, "snapshot_receipt", side_effect=lambda entry: {"job": entry["id"]}) as receipts:
                runner.history_sync(database)
                self.assertEqual(3, get.call_count)
                self.assertEqual(jobs, [call.args[0]["id"] for call in receipts.call_args_list])
                self.assertEqual((202,), get.call_args_list[0].kwargs["expected"])
                self.assertEqual("POST", get.call_args_list[0].kwargs["method"])
                converged.assert_called_once_with(runner.core, [])
            self.assertEqual("SUCCEEDED", runner.report["history_sync"][-1]["status"])

    def test_async_history_sync_fails_on_terminal_missing_unscoped_or_timed_out_job(self):
        runner = self.runner()
        runner.setup()
        runner.core = runner.table("roundtrip")
        job = str(uuid.uuid4())
        entry = {"id": job, "status": "SUCCEEDED", "operationType": "HISTORY_SYNC",
                 "localDatabaseId": runner.primary.db["id"], "localTableId": runner.core[0]["id"],
                 "targetSiteUrl": runner.sites[1].url}
        other = dict(entry, id=str(uuid.uuid4()), targetSiteUrl=runner.sites[2].url)
        queued = {"status": "queued", "jobs": [job, other["id"]]}
        for entries in ([], [dict(entry, status="FAILED")], [dict(entry, status="CANCELLED")],
                        [dict(entry, localDatabaseId=str(uuid.uuid4()))], [dict(entry, operationType="DATA_CREATE")],
                        [entry, entry], [dict(entry, status="UNKNOWN")],
                        [dict(entry, targetSiteUrl=other["targetSiteUrl"])]):
            with self.subTest(entries=entries), patch.object(runner.primary, "get", side_effect=[queued, entries + [other]]), \
                    patch.object(a.time, "sleep", side_effect=AssertionError("Terminal failure must not poll")), \
                    self.assertRaises(a.Failed):
                runner.history_sync()
        with patch.object(runner.primary, "get", side_effect=lambda path, **_: queued if "synchronise" in path
                          else [dict(entry, status="PENDING"), other]), self.assertRaisesRegex(a.Failed, "Timed out"):
            runner.history_sync()
        for wrong in ({"tuples": 10, "pages": 1}, {"status": "queued", "jobs": []},
                      {"status": "queued", "jobs": [job, job]}, {"status": "queued", "jobs": [job]}):
            with patch.object(runner.primary, "get", return_value=wrong), self.assertRaises(a.Failed):
                runner.history_sync()
        with patch.object(runner.primary, "get", return_value=dict(queued, tables=2)), self.assertRaises(a.Failed):
            runner.history_sync(database=True)

    def test_succeeded_history_job_requires_matching_verified_reconciled_snapshot(self):
        runner = self.runner()
        runner.setup()
        runner.core = runner.table("roundtrip")
        snapshot, job = str(uuid.uuid4()), str(uuid.uuid4())
        request = {"snapshotId": snapshot, "checkpointCaptured": True, "checkpoint": None}
        entry = {"id": job, "localTableId": runner.core[0]["id"], "targetSiteUrl": runner.sites[1].url,
                 "remoteDatabaseId": runner.sites[1].db["id"], "remoteTableId": runner.core[1]["id"],
                 "payloadJson": json.dumps(request)}
        manifest = {"format": 1, "snapshotId": snapshot, "origin": runner.primary.url,
                    "sourceDatabaseId": runner.primary.db["id"], "sourceTableId": runner.core[0]["id"],
                    "base": None, "boundary": 3, "legacyThrough": 0, "rows": 2, "currentKeys": 0}
        envelope = {"manifest": manifest, "sha256": "a" * 64}
        receipt = {"snapshotId": snapshot, "tableId": runner.core[1]["id"], "status": "RECONCILED",
                   "historyVerified": True, "currentReconciled": True, "boundary": 3, "legacyThrough": 0,
                   "manifestDigest": envelope["sha256"]}
        with patch.object(runner.primary, "get", return_value=envelope), \
                patch.object(runner.sites[1], "get", return_value=receipt) as get:
            self.assertEqual(receipt, runner.snapshot_receipt(entry)["receipt"])
            self.assertTrue(get.call_args.args[0].endswith(snapshot + "/status"))
        for wrong in (dict(receipt, historyVerified=False), dict(receipt, currentReconciled=False),
                      dict(receipt, status="VERIFIED"),
                      dict(receipt, manifestDigest="b" * 64), dict(receipt, boundary=2),
                      dict(receipt, tableId=str(uuid.uuid4())), dict(receipt, snapshotId=str(uuid.uuid4()))):
            with self.subTest(receipt=wrong), patch.object(runner.primary, "get", return_value=envelope), \
                    patch.object(runner.sites[1], "get", return_value=wrong), self.assertRaises(a.Failed):
                runner.snapshot_receipt(entry)
        for wrong in (dict(manifest, rows=0), dict(manifest, currentKeys=1), dict(manifest, legacyThrough=1),
                      dict(manifest, base={"boundary": 1}), dict(manifest, sourceTableId=str(uuid.uuid4()))):
            with self.subTest(manifest=wrong), patch.object(runner.primary, "get", return_value=dict(envelope, manifest=wrong)), \
                    self.assertRaises(a.Failed):
                runner.snapshot_receipt(entry)
        with self.assertRaises(a.Failed):
            runner.snapshot_receipt(dict(entry, remoteDatabaseId=str(uuid.uuid4())))
        with self.assertRaises(a.Failed):
            runner.snapshot_receipt(dict(entry, payloadJson=json.dumps(dict(request, checkpointCaptured=False))))

    def test_history_job_success_after_deadline_is_not_accepted(self):
        runner = self.runner()
        runner.setup()
        runner.core = runner.table("roundtrip")
        queued = {"status": "queued", "jobs": [str(uuid.uuid4()), str(uuid.uuid4())]}
        with patch.object(runner.primary, "get", side_effect=[queued, []]), \
                patch.object(a.time, "monotonic", side_effect=[0, 0, 1]), \
                self.assertRaisesRegex(a.Failed, "Timed out"):
            runner.history_sync()

    def test_duplicate_site_labels_cannot_alias_sql_manifest_scopes(self):
        self.config["sites"][2]["name"] = self.config["sites"][1]["name"]
        with self.assertRaises(a.Blocked):
            self.runner()

    def sql_scope(self, action="observe"):
        runner = self.runner()
        runner.setup()
        tables = runner.table("outage")
        scope = {"action": action, "run": runner.name, "database": runner.name,
                 "database_id": runner.sites[2].db["id"], "table": tables[2]["internal_name"], "table_id": tables[2]["id"],
                 "source_database_id": runner.primary.db["id"], "source_table_id": tables[0]["id"]}
        if action == "observe":
            scope.update(replication_key=str(uuid.uuid4()), replica=True)
        else:
            scope.update(target_url=runner.sites[2].url, policy="all", coordinated=True, ttl_seconds=60)
        return runner, scope

    def test_sql_observer_is_manifest_scoped_read_only_and_bounded(self):
        runner, scope = self.sql_scope()
        s.validate(scope, runner.report, "2")
        sql = s.observe_sql(scope)
        self.assertIn("START TRANSACTION READ ONLY", sql)
        self.assertIn("FOR SYSTEM_TIME ALL", sql)
        self.assertIn("LIMIT 101", sql)
        self.assertIn("LEFT JOIN", sql)
        self.assertIn("IF(c.row_start IS NOT NULL,NULL", sql)
        self.assertNotIn("2038", sql)  # Current membership, not a server-version-specific period sentinel.
        self.assertNotRegex(sql, r"(?i)\b(INSERT|UPDATE|DELETE|DROP|CREATE|ALTER)\b")
        for bad in (dict(scope, database="research"), dict(scope, table="outage`; DROP DATABASE research;--"),
                    dict(scope, table_id=str(uuid.uuid4())), dict(scope, source_table_id=str(uuid.uuid4())),
                    dict(scope, password="do-not-print")):
            with self.subTest(scope=bad), self.assertRaises(s.Blocked):
                s.validate(bad, runner.report, "2")
        self.assertEqual([], s.observations("")["inbox"])
        with self.assertRaises(s.Blocked):
            s.observations('\n'.join([json.dumps({"kind": "history"})] * 101))

    def test_sql_observer_uses_manifest_internal_database_name(self):
        runner, scope = self.sql_scope()
        database = next(r for r in runner.report["resources"] if r["site"] == "2" and r["kind"] == "database")
        database["internal_name"] = runner.name + "_abcd"
        scope["database"] = database["internal_name"]
        s.validate(scope, runner.report, "2")
        with self.assertRaises(s.Blocked):
            s.validate(dict(scope, database=runner.name + "_other"), runner.report, "2")

    def test_scoped_fault_plans_expire_and_never_drop_foreign_triggers(self):
        runner, scope = self.sql_scope("block")
        s.validate(scope, runner.report, "2")
        expiry = int(a.time.time()) + 60
        sql = s.fault_sql(scope, expiry, [])
        self.assertEqual(3, sql.count("CREATE TRIGGER"))
        self.assertIn("UNIX_TIMESTAMP() < %d" % expiry, sql)
        self.assertNotIn("DROP", sql)
        owned = [{"event": op, "name": s.trigger_name(scope, op), "table": scope["table"], "timing": "BEFORE",
                  "body": s.trigger_body(scope, op, expiry)} for op in ("INSERT", "UPDATE", "DELETE")]
        self.assertNotIn("CREATE", s.fault_sql(scope, expiry, owned))
        self.assertEqual(3, s.fault_sql(dict(scope, action="unblock"), expiry, owned).count("DROP TRIGGER"))
        owned[0]["body"] += " /* changed externally */"
        with self.assertRaises(s.Blocked):
            s.fault_sql(dict(scope, action="unblock"), expiry, owned)
        latest = dict(scope, policy="latest-only")
        self.assertIn("NEW.value <=> 'latest'", s.trigger_body(latest, "UPDATE", expiry))
        self.assertIn("AND (FALSE)", s.trigger_body(latest, "DELETE", expiry))

    def test_sql_controller_defaults_to_plan_and_requires_fault_opt_in(self):
        runner, scope = self.sql_scope("block")
        runner.save()
        argv = [sys.executable, str(ROOT / "replication-sql.py"), "--report", str(self.args.report), "--site", "2"]
        planned = subprocess.run(argv, input=json.dumps(scope), capture_output=True, text=True, timeout=5)
        self.assertEqual(0, planned.returncode)
        self.assertEqual({"status": "PLAN", "executed": False},
                         {k: json.loads(planned.stdout)[k] for k in ("status", "executed")})
        blocked = subprocess.run(argv + ["--execute"], input=json.dumps(scope), capture_output=True, text=True, timeout=5)
        self.assertEqual(2, blocked.returncode)
        self.assertIn("BLOCKED", blocked.stdout)

    def test_sql_command_checks_remote_origin_without_returning_secrets(self):
        args = SimpleNamespace(ssh="lab", container="dbrepo-data-db", metadata_container="dbrepo-metadata-service",
                               site_url="https://site.invalid", timeout=1)
        with patch.object(s.subprocess, "run", return_value=SimpleNamespace(returncode=0, stdout="{}", stderr="")) as run:
            self.assertEqual("{}", s.execute(args, "SELECT 1;"))
            remote = run.call_args.args[0][-1]
            self.assertIn("printenv BASE_URL", remote)
            self.assertIn("MARIADB_ROOT_PASSWORD", remote)
            self.assertIn("https://site.invalid", remote)
            self.assertIn("max_statement_time=10", run.call_args.kwargs["input"])
        with patch.object(s.subprocess, "run", return_value=SimpleNamespace(returncode=1, stdout="", stderr="secret")), \
                self.assertRaises(s.Blocked) as raised:
            s.execute(args, "SELECT 1;")
        self.assertNotIn("secret", str(raised.exception))

    def test_retry_false_is_not_success_and_unblock_runs_on_failure(self):
        runner = self.runner()
        runner.setup()
        self.args.allow_faults = True
        self.config.update(scoped_faults_coordinated=True, fault_hook=["unused"])
        with patch.object(runner, "fault") as fault, patch.object(runner, "write", side_effect=a.Failed("write failed")):
            with self.assertRaises(a.Failed):
                runner.outage()
            self.assertEqual(["block", "unblock"], [c.args[0] for c in fault.call_args_list])
        pending = {"id": str(uuid.uuid4()), "status": "PENDING", "createdAt": "2026-10-03T12:00:00Z"}
        with patch.object(runner, "queue", return_value=[pending]), patch.object(runner.primary, "get", return_value={"retried": False}):
            with self.assertRaisesRegex(a.Failed, "retried=false"):
                runner.retry([])
        completed = dict(pending, status="SUCCEEDED")
        with patch.object(runner, "queue", side_effect=[[pending], [completed], [completed], [completed]]), \
                patch.object(runner.primary, "get", return_value={"retried": False}):
            runner.retry([], required=False)
        with patch.object(runner, "queue", side_effect=[[pending], [completed]]), \
                patch.object(runner.primary, "get", return_value={"retried": False}):
            with self.assertRaisesRegex(a.Blocked, "manual retry was not exercised"):
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

    def test_citation_requires_immutable_mode_and_offline_gate_probes_origin(self):
        runner = self.runner()
        runner.setup()
        runner.crud()
        source = runner.subsets[0]
        for site in runner.sites[1:]:
            site.subsets[source["id"]] = copy.deepcopy(runner.primary.subsets[source["id"]])
        runner.canonical_subset()
        original = runner.sites[1].request
        def mutable(path, **kwargs):
            data, headers = original(path, **kwargs)
            headers.pop("x-result-mode", None)
            return data, headers
        with patch.object(runner.sites[1], "request", side_effect=mutable), self.assertRaises(a.Failed):
            runner.replay(1, source)
        self.args.allow_faults = True
        self.config.update(scoped_faults_coordinated=True, offline_hook=["unused"])
        with patch.object(a, "command", side_effect=lambda argv, payload, timeout: payload) as hook, \
                patch.object(runner.primary, "request", return_value=(None, {})) as probe:
            runner.offline_canonical_subset()
            self.assertEqual((503,), probe.call_args.kwargs["expected"])
            self.assertFalse(probe.call_args.kwargs["parse_json"])
            self.assertEqual(["block", "unblock"], [json.loads(c.args[1])["action"] for c in hook.call_args_list])
        with patch.object(a, "command", side_effect=lambda argv, payload, timeout: payload) as hook, \
                patch.object(runner.primary, "request", side_effect=a.Failed("Origin was not blocked")), self.assertRaises(a.Failed):
            runner.offline_canonical_subset()
        self.assertEqual("unblock", json.loads(hook.call_args.args[1])["action"])

    def test_history_gate_accepts_unseen_events_but_rejects_invented_visibility(self):
        runner = self.runner()
        runner.setup()
        runner.outage_tables = runner.table("outage")
        runner.write(runner.outage_tables, "POST", "original")
        runner.write(runner.outage_tables, "PUT", "changed")
        runner.write(runner.outage_tables, "DELETE")
        runner.outage_events = runner.journal(runner.outage_tables)
        runner.outage_key = runner.outage_events[0]["payload"]["tuple"]["replicationKey"]
        peer, table = runner.sites[2], runner.outage_tables[2]
        peer.versions[table["id"]] = []
        for row in peer.inbox.values():
            row["receipt"] = {"applied": False, "data": {}, "replicationKey": runner.outage_key}
        with patch.object(runner, "observe", side_effect=lambda *args: self.observed(runner, *args)):
            runner.outage_history()
            self.assertIn("outage_evidence_sha256", runner.report)
            peer.versions[table["id"]] = copy.deepcopy(runner.primary.versions[runner.outage_tables[0]["id"]])
            with self.assertRaises(a.Failed):
                runner.outage_history()

    def test_full_report_never_turns_blocked_gates_into_pass(self):
        runner = self.runner()
        with patch.object(runner, "archive"), patch.object(runner, "history_sync", side_effect=a.Blocked("not active")), \
                contextlib.redirect_stdout(io.StringIO()):
            self.assertEqual(1, runner.run())
        report = json.loads(self.args.report.read_text())
        self.assertEqual("FAIL", report["status"])
        self.assertEqual(13, len(report["checks"]))
        self.assertEqual(8, sum(c["status"] == "BLOCKED" for c in report["checks"]))
        self.assertEqual("FAIL", report["checks"][-2]["status"])
        self.assertEqual("BLOCKED", report["checks"][-1]["status"])

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
