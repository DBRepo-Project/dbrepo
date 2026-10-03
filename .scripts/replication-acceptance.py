#!/usr/bin/env python3
"""Opt-in, bounded three-site checks. Creates fresh fixtures; never purges queues."""
import argparse
import base64
import hashlib
import json
import os
from pathlib import Path
import re
import signal
import subprocess
import time
import urllib.error
import urllib.parse
import urllib.request
import uuid


class Blocked(Exception):
    pass


class Failed(Exception):
    pass


class Deadline(Exception):
    pass


def require(condition, message):
    if not condition:
        raise Failed(message)


def uid(value):
    return str(uuid.UUID(str(value)))


def canonical(value):
    return json.dumps(value, sort_keys=True, separators=(",", ":"), ensure_ascii=True)


def rows(value):
    require(isinstance(value, list) and len(value) < 100, "Expected a complete fixture page (<100 rows)")
    return sorted(({"id": r["id"], "value": r["value"]} for r in value), key=canonical)


def fingerprint(value):
    return hashlib.sha256(canonical(value).encode()).hexdigest()


class NoRedirect(urllib.request.HTTPRedirectHandler):
    def redirect_request(self, req, fp, code, msg, headers, newurl):
        return None


class Site:
    def __init__(self, config, timeout):
        self.config, self.timeout = config, timeout
        self.url = config["url"].rstrip("/")
        parsed = urllib.parse.urlsplit(self.url)
        if (parsed.scheme != "https" or not parsed.hostname or parsed.username or parsed.password
                or parsed.path or parsed.query or parsed.fragment):
            raise Blocked("Each site must be an explicit HTTPS origin without credentials, path, or query")
        self.auth = {}
        for role in ("user", "system"):
            username = os.environ.get(config[role + "_env"], "")
            password = os.environ.get(config[role + "_password_env"], "")
            if not username or not password or ":" in username:
                raise Blocked("Missing environment credentials for site " + config["name"])
            self.auth[role] = "Basic " + base64.b64encode((username + ":" + password).encode()).decode()
        if self.auth["user"] == self.auth["system"]:
            raise Blocked("User rejection checks require separate ordinary-user credentials")
        self.username = os.environ[config["user_env"]]
        self.http = urllib.request.build_opener(urllib.request.ProxyHandler({}), NoRedirect())
        self.db = None

    def request(self, path, method="GET", body=None, role="user", expected=(200,)):
        if not path.startswith("/api/") or path.startswith("//"):
            raise Blocked("Refusing request outside API origin")
        request = urllib.request.Request(self.url + path, method=method,
                  data=None if body is None else canonical(body).encode(),
                  headers={"Authorization": self.auth[role], "Content-Type": "application/json",
                           "Accept": "application/json"})
        try:
            response = self.http.open(request, timeout=self.timeout)
        except urllib.error.HTTPError as error:
            response = error
        except (OSError, urllib.error.URLError):
            raise Blocked("HTTP transport unavailable at " + self.config["name"]) from None
        with response:
            raw = response.read(4 * 1024 * 1024 + 1)
            status, headers = response.status, dict((k.lower(), v) for k, v in response.headers.items())
        require(status in expected, "%s %s returned HTTP %d" % (self.config["name"], method, status))
        require(len(raw) <= 4 * 1024 * 1024, "Response exceeded bounded fixture size")
        try:
            return json.loads(raw) if raw else None, headers
        except ValueError:
            raise Failed("Expected JSON response; body withheld") from None

    def get(self, path, **kwargs):
        return self.request(path, **kwargs)[0]

    def dbpath(self):
        return "/api/v1/database/" + uid(self.db["id"])


def command(argv, payload, timeout):
    if not isinstance(argv, list) or not argv or not all(isinstance(x, str) for x in argv):
        raise Blocked("Hook/SQL commands must be explicit argv arrays")
    try:
        result = subprocess.run(argv, input=payload, capture_output=True, text=True, timeout=timeout)
    except (OSError, subprocess.TimeoutExpired):
        raise Blocked("Configured command unavailable or timed out; output withheld") from None
    if result.returncode:
        raise Blocked("Configured command failed; output withheld")
    return result.stdout


class Acceptance:
    def __init__(self, config, args):
        self.config, self.args = config, args
        self.sites = [Site(site, args.http_timeout) for site in config["sites"]]
        if len(self.sites) != 3 or len({s.url for s in self.sites}) != 3:
            raise Blocked("Exactly three distinct sites are required; first is primary")
        if config.get("dedicated_test_users") is not True:
            raise Blocked("Config must attest dedicated_test_users=true")
        for site in self.sites:
            if not re.fullmatch(r"[a-zA-Z0-9_-]{1,24}", site.config["name"]):
                raise Blocked("Site names must be short safe labels")
        self.primary = self.sites[0]
        self.name = "replication_acceptance_" + uuid.uuid4().hex[:16]
        self.report = {"run": self.name, "status": "RUNNING", "resources": [], "checks": []}
        self.subsets = []

    def save(self):
        self.args.report.write_text(json.dumps(self.report, indent=2) + "\n")

    def check(self, name, action):
        start = time.monotonic()
        try:
            action()
            result = {"name": name, "status": "PASS"}
        except (Blocked, Failed) as error:
            result = {"name": name, "status": "BLOCKED" if isinstance(error, Blocked) else "FAIL",
                      "reason": str(error)}
        except (KeyError, TypeError, ValueError):
            result = {"name": name, "status": "FAIL", "reason": "Unexpected API contract; response withheld"}
        result["seconds"] = round(time.monotonic() - start, 3)
        self.report["checks"].append(result)
        self.save()
        print(json.dumps(result), flush=True)
        return result["status"] == "PASS"

    def wait(self, action, label):
        deadline = time.monotonic() + self.args.wait
        while True:
            try:
                result = action()
                if result:
                    return result
            except (Blocked, Failed):
                pass
            if time.monotonic() >= deadline:
                raise Failed("Timed out waiting for " + label)
            time.sleep(min(self.args.poll, max(0, deadline - time.monotonic())))

    def setup(self):
        required_roles = {"create-table", "insert-table-data", "delete-table-data", "persist-query"}
        for site in self.sites:
            if not required_roles.issubset(site.config.get("attested_user_roles", [])):
                raise Blocked("Attest ordinary-user realm roles before rejection checks")
        for site in self.sites:
            site.get("/api/replication/status", role="system")
            existing = site.get("/api/v1/database", role="system")
            require(not any(d.get("name") == self.name for d in existing), "Fixture name collision")
            user = site.get("/api/v1/user/" + urllib.parse.quote(site.username, safe=""))
            require(user["username"] == site.username, "Ordinary user not authenticated")
        body = {"name": self.name, "container_id": uid(self.config["primary_container_id"]),
                "is_public": False, "is_schema_public": False,
                "replica_urls": [s.url for s in self.sites[1:]]}
        # Persist intended name before any write, even if the creation response is lost.
        self.save()
        self.primary.db = self.primary.get("/api/v1/database", method="POST", body=body, expected=(201,))
        self.record(self.primary, self.primary.db, "database")
        self.primary.db = self.primary.get(self.primary.dbpath(), role="system")
        require(self.primary.db["name"] == self.name, "Created database name mismatch")
        for site in self.sites[1:]:
            def mapped():
                origin = self.primary.get(self.primary.dbpath(), role="system")
                remote = (origin.get("replica_urls") or {}).get(site.url)
                if not remote:
                    return False
                site.db = site.get("/api/v1/database/" + uid(remote), role="system")
                require(site.db["name"] == self.name, "Replica database name mismatch")
                require(site.db.get("creation_location", "").rstrip("/") == self.primary.url,
                        "Replica origin mismatch")
                return True
            self.wait(mapped, "database mapping on " + site.config["name"])
            self.record(site, site.db, "database")
            # These must be dedicated users: mapping also records their origin identity.
            site.get(site.dbpath() + "/replication-access", method="PUT", role="system",
                     body={"local_username": site.username}, expected=(200, 202))

    def record(self, site, item, kind):
        self.report["resources"].append({"site": site.config["name"], "kind": kind,
                                          "id": uid(item["id"]), "name": item.get("name")})
        self.save()

    @staticmethod
    def table_body(name):
        return {"name": name, "is_public": False, "is_schema_public": False,
                "columns": [{"name": "id", "type": "int", "null_allowed": False},
                            {"name": "value", "type": "varchar", "size": 64, "null_allowed": False},
                            {"name": "replication_key", "type": "char", "size": 36, "null_allowed": False}],
                "constraints": {"primary_key": ["id"], "uniques": [["replication_key"]],
                                "foreign_keys": [], "checks": []}}

    def table(self, name):
        primary = self.primary.get(self.primary.dbpath() + "/table", method="POST",
                                   body=self.table_body(name), expected=(201,))
        tables = [primary]
        self.record(self.primary, primary, "table")
        for site in self.sites[1:]:
            def mapped():
                origin = self.primary.get(self.path(0, tables), role="system")
                remote = (origin.get("replica_urls") or {}).get(site.url)
                return site.get(site.dbpath() + "/table/" + uid(remote), role="system") if remote else None
            tables.append(self.wait(mapped, "table mapping on " + site.config["name"]))
            require(tables[-1]["name"] == name, "Replica table name mismatch")
            self.record(site, tables[-1], "table")
        return tables

    def path(self, index, tables):
        return self.sites[index].dbpath() + "/table/" + uid(tables[index]["id"])

    def write(self, tables, method, value=None):
        body = {"keys": {"id": 1}}
        if method == "POST":
            body = {"data": {"id": 1, "value": value, "replication_key": str(uuid.uuid4())}}
        elif method == "PUT":
            body["data"] = {"value": value}
        self.primary.get(self.path(0, tables) + "/data", method=method, body=body,
                         expected=(201,) if method == "POST" else (202,))

    def converged(self, tables, expected, indices=(0, 1, 2)):
        for index in indices:
            self.wait(lambda: rows(self.sites[index].get(self.path(index, tables) + "/data?page=0&size=100"))
                      == expected, "current rows on " + self.sites[index].config["name"])

    def subset(self, index, tables):
        site = self.sites[index]
        table = site.get(self.path(index, tables), role="system")
        columns = [{"id": uid(c["id"]), "alias": c["internal_name"]} for c in table["columns"]
                   if c["internal_name"] in ("id", "value")]
        require(len(columns) == 2, "Missing fixture columns")
        data, headers = site.request(site.dbpath() + "/subset?page=0&size=100", method="POST",
                                     body={"datasource_ids": [uid(table["id"])], "columns": columns}, expected=(201,))
        subset = uid(headers["x-id"])
        self.record(site, {"id": subset}, "subset")
        site.get(site.dbpath() + "/subset/" + subset, method="PUT", body={"persist": True}, expected=(202,))
        require(headers.get("x-integrity") == "verified" and headers.get("x-result-hash", "").startswith("v2:"),
                "Subset fixity is not verified v2")
        require(rows(data) == [{"id": 1, "value": "original"}], "Initial subset rows differ")
        return {"id": subset, "hash": headers["x-result-hash"], "rows": rows(data)}

    def replay(self, index, subset):
        site = self.sites[index]
        path = site.dbpath() + "/subset/" + subset["id"]
        data, headers = site.request(path + "/data?page=0&size=100")
        require(rows(data) == subset["rows"], "Historical subset rows changed")
        require(headers.get("x-integrity") == "verified" and headers.get("x-result-hash") == subset["hash"],
                "Historical subset fixity changed or is unverified")
        meta = site.get(path)
        require(meta["id"] == subset["id"] and meta["result_hash"] == subset["hash"]
                and meta["is_persisted"] is True, "Stored selection metadata changed")

    def crud(self):
        self.core = self.table("roundtrip")
        self.write(self.core, "POST", "original")
        self.converged(self.core, [{"id": 1, "value": "original"}])
        self.subsets = [self.subset(i, self.core) for i in range(3)]
        self.write(self.core, "PUT", "changed")
        self.converged(self.core, [{"id": 1, "value": "changed"}])
        self.write(self.core, "DELETE")
        self.converged(self.core, [])
        for i, subset in enumerate(self.subsets):
            self.replay(i, subset)

    def rejection(self):
        tables = self.table("readonly")
        self.write(tables, "POST", "original")
        self.converged(tables, [{"id": 1, "value": "original"}])
        for i in (1, 2):
            site, path = self.sites[i], self.path(i, tables)
            before = site.get(path + "/history?size=100")
            for method, body in [("POST", {"data": {"id": 2, "value": "forbidden", "replication_key": str(uuid.uuid4())}}),
                                 ("PUT", {"keys": {"id": 1}, "data": {"value": "forbidden"}}),
                                 ("DELETE", {"keys": {"id": 1}})]:
                site.get(path + "/data", method=method, body=body, expected=(403,))
            site.get(site.dbpath() + "/table", method="POST", body=self.table_body("forbidden"), expected=(403,))
            require(site.get(path + "/history?size=100") == before, "Rejected writes changed history")
        self.converged(tables, [{"id": 1, "value": "original"}])

    def exported(self, tables):
        data = self.primary.get(self.path(0, tables) + "/data/replicate?page=0&size=100", role="system")
        require(len(data["tuples"]) == 1, "Expected one exported fixture tuple")
        return {"tuple": data["tuples"][0]}

    def delivery(self, scenario):
        tables = self.table(scenario)
        self.write(tables, "POST", "original")
        self.converged(tables, [{"id": 1, "value": "original"}])
        old = self.exported(tables)
        if scenario != "duplicate":
            self.write(tables, "PUT", "changed")
            self.converged(tables, [{"id": 1, "value": "changed"}])
        if scenario == "resurrection":
            self.write(tables, "DELETE")
            self.converged(tables, [])
        expected = [] if scenario == "resurrection" else [{"id": 1, "value": "original" if scenario == "duplicate" else "changed"}]
        for i in (1, 2):
            site, path = self.sites[i], self.path(i, tables)
            before = site.get(path + "/history?size=100")
            require(bool(before), "History observation unavailable")
            time.sleep(0.05)
            # Replay the exact committed payload, simulating a lost response / late event.
            site.get(path + "/data/replicate", method="PUT" if scenario == "reorder" else "POST",
                     body=old, role="system", expected=(200, 201, 204, 409))
            require(rows(site.get(path + "/data?page=0&size=100")) == expected, "Replay reset or resurrected tuple")
            require(site.get(path + "/history?size=100") == before, "Replay appended history")

    def fault(self, action, tables):
        scope = {"run": self.name, "database_id": self.primary.db["id"], "table_id": tables[0]["id"],
                 "remote_database_id": self.sites[2].db["id"], "remote_table_id": tables[2]["id"],
                 "target_url": self.sites[2].url, "action": action, "ttl_seconds": self.args.deadline + 60}
        result = json.loads(command(self.config["fault_hook"], canonical(scope), self.args.http_timeout))
        require(result == scope, "Fault hook did not acknowledge exact fixture scope/action")

    def queue(self, tables):
        entries = self.primary.get("/api/replication/outbox", role="system")
        return [e for e in entries if e.get("localDatabaseId") == self.primary.db["id"]
                and e.get("localTableId") == tables[0]["id"] and e.get("targetSiteUrl", "").rstrip("/") == self.sites[2].url]

    def outage(self):
        if not self.args.allow_faults or not self.config.get("fault_hook") or self.config.get("isolated_fault_environment") is not True:
            raise Blocked("Outage requires --allow-faults, isolated_fault_environment=true, and a scoped fault_hook")
        tables = self.table("outage")
        required_status = self.config.get("outage_wait_for", "PENDING")
        if required_status not in ("PENDING", "FAILED"):
            raise Blocked("outage_wait_for must be PENDING or FAILED")
        try:
            self.fault("block", tables)
            self.write(tables, "POST", "original")
            self.converged(tables, [{"id": 1, "value": "original"}], (0, 1))
            self.outage_key = uid(self.exported(tables)["tuple"]["replicationKey"])
            require(rows(self.sites[2].get(self.path(2, tables) + "/data?page=0&size=100")) == [],
                    "Fault did not isolate the fixture")
            self.write(tables, "PUT", "changed")
            self.converged(tables, [{"id": 1, "value": "changed"}], (0, 1))
            self.write(tables, "DELETE")
            self.converged(tables, [], (0, 1))
            def durable_events():
                entries = self.queue(tables)
                operations = {e.get("operationType") for e in entries if e.get("status") == required_status}
                return entries if operations >= {"DATA_CREATE", "DATA_UPDATE", "DATA_DELETE"} else None
            queued = self.wait(durable_events, "three durable, fixture-scoped " + required_status + " delivery events")
            self.report["outage_entries"] = [{k: e.get(k) for k in ("id", "httpMethod", "status", "attempts")} for e in queued]
            self.save()
        finally:
            # Also attempt unblocking after a failed/partial block acknowledgement.
            self.fault("unblock", tables)
        self.retry(tables)
        self.converged(tables, [])
        self.outage_tables = tables

    def retry(self, tables):
        retried = 0
        for entry in sorted(self.queue(tables), key=lambda e: e["createdAt"]):
            if entry["status"] not in ("PENDING", "FAILED"):
                continue
            result = self.primary.get("/api/replication/outbox/" + uid(entry["id"]) + "/retry",
                                      method="POST", role="system")
            require(result.get("retried") is True, "Scoped retry returned retried=false")
            retried += 1
        if not retried:
            raise Blocked("Automatic recovery won the race; manual retry was not exercised")
        self.wait(lambda: bool(self.queue(tables)) and all(e["status"] == "SUCCEEDED" for e in self.queue(tables)),
                  "fixture-only delivery queue completion")

    def outage_history(self):
        if not hasattr(self, "outage_tables"):
            raise Blocked("Outage/retry did not complete")
        histories, timestamps = [], []
        for i, site in enumerate(self.sites):
            if not site.config.get("sql_observer"):
                raise Blocked("All sites need read-only sql_observer argv for exact history/timestamp evidence")
            db, table = site.db["internal_name"], self.outage_tables[i]["internal_name"]
            require(db == self.name and re.fullmatch(r"[a-z0-9_]+", table), "Unsafe SQL fixture name")
            sql = f"""SET time_zone='+00:00'; START TRANSACTION READ ONLY;
SELECT JSON_OBJECT('id',id,'value',value) FROM `{db}`.`{table}` FOR SYSTEM_TIME ALL ORDER BY id,row_start;
SELECT JSON_OBJECT('site',site_url,'key',replication_id,'database',database_id,'table',table_id,
 'start',row_start,'end',row_end) FROM `{db}`.tuple_replication_timestamps
 WHERE replication_id='{self.outage_key}' ORDER BY site_url,row_start;
COMMIT;
"""
            observed = [json.loads(line) for line in command(site.config["sql_observer"], sql, self.args.http_timeout).splitlines()]
            histories.append([r for r in observed if "value" in r])
            timestamps.append(sorted([r for r in observed if "site" in r], key=canonical))
        expected = [{"id": 1, "value": "original"}, {"id": 1, "value": "changed"}]
        require(all(h == expected for h in histories), "Catch-up lost, reordered, or duplicated historical versions")
        require(timestamps[0] == timestamps[1] == timestamps[2], "Three-site timestamp maps diverged")
        for site in self.sites:
            versions = [r for r in timestamps[0] if r["site"].rstrip("/") == site.url]
            require(len(versions) == 2 and all(r["end"] for r in versions), "Missing closed version mappings for a site")
        self.report["outage_history_sha256"] = fingerprint(histories[0])
        self.report["outage_timestamps_sha256"] = fingerprint(timestamps[0])

    def archive(self):
        if len(self.subsets) != 3:
            raise Blocked("Three persisted local subsets were not created")
        self.primary.get(self.path(0, self.core), method="DELETE", expected=(202,))
        for i, subset in enumerate(self.subsets):
            site = self.sites[i]
            def archived():
                db = site.get(site.dbpath() + "?include_archived=true", role="system")
                return any(t["id"] == self.core[i]["id"] and t.get("archived_at") for t in db["tables"])
            self.wait(archived, "archive propagation")
            active = site.get(site.dbpath(), role="system")
            require(all(t["id"] != self.core[i]["id"] for t in active["tables"]), "Archived table remains active")
            self.replay(i, subset)

    def canonical_subset(self):
        if len(self.subsets) != 3:
            raise Blocked("Origin subset was not created")
        for i in (1, 2):
            def replay():
                self.replay(i, self.subsets[0])
                return True
            self.wait(replay, "canonical origin subset replay after archive")

    def run(self):
        if self.check("fixture_setup", self.setup):
            self.check("primary_insert_update_delete_and_local_fixity", self.crud)
            self.check("replica_user_rejection", self.rejection)
            self.check("duplicate_delivery_no_extra_history", lambda: self.delivery("duplicate"))
            self.check("older_update_cannot_reset_newer", lambda: self.delivery("reorder"))
            self.check("older_insert_cannot_resurrect_delete", lambda: self.delivery("resurrection"))
            self.check("outage_and_scoped_manual_retry", self.outage)
            self.check("outage_history_and_three_site_timestamps", self.outage_history)
            self.check("archive_preserves_local_subset_fixity", self.archive)
            self.check("canonical_origin_subset_replays_on_peers_after_archive", self.canonical_subset)
        else:
            self.report["checks"].append({"name": "remaining_scenarios", "status": "BLOCKED", "reason": "Fixture setup failed"})
        statuses = {c["status"] for c in self.report["checks"]}
        self.report["status"] = "FAIL" if "FAIL" in statuses else "BLOCKED" if "BLOCKED" in statuses else "PASS"
        self.save()
        return {"PASS": 0, "FAIL": 1, "BLOCKED": 2}[self.report["status"]]


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--config", type=Path, required=True)
    parser.add_argument("--report", type=Path, required=True, help="New report file, never overwritten")
    parser.add_argument("--allow-writes", action="store_true")
    parser.add_argument("--allow-faults", action="store_true")
    parser.add_argument("--http-timeout", type=int, default=20)
    parser.add_argument("--wait", type=int, default=120)
    parser.add_argument("--poll", type=float, default=2)
    parser.add_argument("--deadline", type=int, default=1800)
    args = parser.parse_args()
    runner = None
    owns_report = False
    try:
        if not args.allow_writes:
            raise Blocked("Explicit --allow-writes required; no network requests made")
        if not (1 <= args.http_timeout <= 120 and 1 <= args.wait <= 600 and 0.1 <= args.poll <= 30
                and 1 <= args.deadline <= 7200):
            raise Blocked("Invalid bounds: HTTP 1..120s, wait 1..600s, poll 0.1..30s, deadline 1..7200s")
        config = json.loads(args.config.read_text())
        os.umask(0o077)
        # Exclusive creation prevents overwriting old evidence, including symlinks.
        with args.report.open("x"):
            pass
        owns_report = True
        runner = Acceptance(config, args)
        def deadline(*_):
            raise Deadline("Overall acceptance deadline exceeded")
        signal.signal(signal.SIGALRM, deadline)
        signal.alarm(args.deadline)
        return runner.run()
    except (Blocked, Failed, Deadline) as error:
        status = "FAIL" if isinstance(error, Failed) else "BLOCKED"
        reason = str(error)
    except (KeyError, TypeError, ValueError, OSError, KeyboardInterrupt):
        status, reason = "BLOCKED", "Invalid config/API contract, interrupted, or evidence unavailable; details withheld"
    finally:
        signal.alarm(0)
    if runner:
        runner.report.update(status=status, reason=reason)
        runner.save()
    elif owns_report:
        args.report.write_text(json.dumps({"status": status, "reason": reason}) + "\n")
    print(json.dumps({"status": status, "reason": reason}), flush=True)
    return 2 if status == "BLOCKED" else 1


if __name__ == "__main__":
    raise SystemExit(main())
