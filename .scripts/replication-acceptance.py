#!/usr/bin/env python3
"""Opt-in, bounded three-site checks. Creates fresh fixtures; never purges queues."""
import argparse
import base64
import copy
import datetime
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


def period(value):
    return datetime.datetime.fromisoformat(value.replace("Z", "+00:00")).astimezone(
        datetime.timezone.utc).strftime("%Y-%m-%d %H:%M:%S.%f")


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

    def request(self, path, method="GET", body=None, role="user", expected=(200,), parse_json=True):
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
        if not parse_json:
            return None, headers
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
        if (len(self.sites) != 3 or len({s.url for s in self.sites}) != 3
                or len({s.config["name"] for s in self.sites}) != 3):
            raise Blocked("Exactly three distinct site origins and labels are required; first is primary")
        if config.get("dedicated_test_users") is not True:
            raise Blocked("Config must attest dedicated_test_users=true")
        for site in self.sites:
            if not re.fullmatch(r"[a-zA-Z0-9_-]{1,24}", site.config["name"]):
                raise Blocked("Site names must be short safe labels")
        self.primary = self.sites[0]
        self.name = "replication_acceptance_" + uuid.uuid4().hex[:16]
        self.report = {"run": self.name, "status": "RUNNING", "resources": [], "checks": [],
                       "sites": {site.config["name"]: site.url for site in self.sites}}
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
        if self.config.get("strict_snapshot_activation") is not True or self.config.get("parent_coordinated") is not True:
            raise Blocked("Parent coordination and strict_snapshot_activation=true are required before fixture writes")
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
                                          "id": uid(item["id"]), "name": item.get("name"),
                                          "internal_name": item.get("internal_name")})
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
        meta = site.get(site.dbpath() + "/subset/" + subset)
        if not re.fullmatch(r"[a-f0-9]{64}", meta.get("snapshot_hash") or ""):
            raise Blocked("Immutable subset artifacts are not active; coordinate strict snapshot activation")
        return {"id": subset, "hash": headers["x-result-hash"], "snapshot_hash": meta["snapshot_hash"], "rows": rows(data)}

    def replay(self, index, subset):
        site = self.sites[index]
        path = site.dbpath() + "/subset/" + subset["id"]
        data, headers = site.request(path + "/data?page=0&size=100")
        require(rows(data) == subset["rows"], "Historical subset rows changed")
        require(headers.get("x-integrity") == "verified" and headers.get("x-result-hash") == subset["hash"],
                "Historical subset fixity changed or is unverified")
        require(headers.get("x-result-mode") == "immutable-snapshot", "Citation was not served from an immutable artifact")
        meta = site.get(path)
        require(meta["id"] == subset["id"] and meta["result_hash"] == subset["hash"]
                and meta["is_persisted"] is True and meta.get("snapshot_hash") == subset["snapshot_hash"],
                "Stored selection metadata or immutable artifact identity changed")

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

    def journal(self, tables):
        after, through, seen, events = 0, None, set(), []
        for _ in range(20):
            path = self.primary.dbpath() + "/replication/journal?after=%d&limit=100" % after
            if through is not None:
                path += "&through=%d" % through
            page = self.primary.get(path, role="system")
            require(type(page["through"]) is int and page["through"] >= after, "Invalid journal boundary")
            if through is None:
                through = page["through"]
            require(page["through"] == through and page["legacyThrough"] == 0,
                    "New fixture journal has a changed boundary or ambiguous legacy events")
            require(isinstance(page["events"], list) and len(page["events"]) <= 100, "Unbounded journal page")
            for event in page["events"]:
                payload = event["payload"]
                sequence, event_id = payload["eventSequence"], uid(payload["eventId"])
                require(type(sequence) is int and sequence == after + 1 and sequence <= through
                        and event_id not in seen, "Journal has missing, repeated, or reordered identities")
                require(event["method"] in ("POST", "PUT", "DELETE")
                        and uid(payload["database"]["id"]) == uid(self.primary.db["id"]), "Journal origin/method mismatch")
                seen.add(event_id)
                after = sequence
                if uid(payload["table"]["id"]) == uid(tables[0]["id"]):
                    # This is local provenance, not a new field added to the wire DTO.
                    events.append({"originalHttpMethod": event["method"], "payload": copy.deepcopy(payload)})
            require(page["nextAfter"] == after, "Journal cursor skipped retained events")
            if after == through:
                return events
            require(bool(page["events"]), "Incomplete journal prefix")
        raise Blocked("Fixture journal exceeded 20 bounded pages")

    def exported(self, tables, method="POST", value=None):
        events = [e for e in self.journal(tables) if e["originalHttpMethod"] == method
                  and (value is None or e["payload"]["tuple"]["data"]["value"] == value)]
        require(len(events) == 1, "Expected exactly one retained fixture event for this method/value")
        event = events[0]
        self.report.setdefault("journal_events", []).append({"event_id": uid(event["payload"]["eventId"]),
            "sequence": event["payload"]["eventSequence"], "originalHttpMethod": method,
            "source_payload_sha256": fingerprint(event["payload"])})
        self.save()
        return event

    def hydrated(self, tables, event):
        payload = copy.deepcopy(event["payload"])
        for kind, path in (("database", self.primary.dbpath()), ("table", self.path(0, tables))):
            metadata = self.primary.get(path, role="system")
            require(uid(metadata["id"]) == uid(payload[kind]["id"]), "Cannot hydrate another source identity")
            mapping = metadata.get("replica_urls") or {}
            for i in (1, 2):
                expected = self.sites[i].db["id"] if kind == "database" else tables[i]["id"]
                require(mapping.get(self.sites[i].url) == expected, "Target mapping differs from scoped fixture")
            payload[kind]["replica_urls"] = copy.deepcopy(mapping)
        return payload

    def observe(self, index, tables, key):
        site = self.sites[index]
        if not site.config.get("sql_observer"):
            raise Blocked("A scoped sql_observer is required for retained inbox and local visibility evidence")
        scope = {"action": "observe", "run": self.name, "database": site.db["internal_name"],
                 "database_id": uid(site.db["id"]),
                 "table": tables[index]["internal_name"], "table_id": uid(tables[index]["id"]),
                 "source_database_id": uid(self.primary.db["id"]), "source_table_id": uid(tables[0]["id"]),
                 "replication_key": uid(key), "replica": index != 0}
        observed = json.loads(command(site.config["sql_observer"], canonical(scope), self.args.http_timeout))
        require(all(isinstance(observed.get(k), list) for k in ("history", "inbox", "heads", "timestamps")),
                "Invalid scoped SQL observation")
        return observed

    @staticmethod
    def retained(observed, event, target):
        source = event["payload"]
        matches = [r for r in observed["inbox"] if r["event_id"] == source["eventId"]]
        require(len(matches) == 1, "Retained inbox event is missing or duplicated")
        row = matches[0]
        require(row["sequence"] == source["eventSequence"] and row["table_id"] == target
                and row["source_database_id"] == source["database"]["id"]
                and row["source_table_id"] == source["table"]["id"], "Inbox source identity changed")
        require(row["payload"] == {"method": event["originalHttpMethod"], "sequence": source["eventSequence"],
                "target": target, "database": source["database"]["id"], "table": source["table"]["id"],
                "tuple": source["tuple"]}, "Inbox did not preserve the original method/source tuple")
        require(type(row["receipt"].get("applied")) is bool, "Inbox receipt lacks explicit local visibility")
        return row["receipt"]

    def send_event(self, index, tables, event):
        return self.sites[index].get(self.path(index, tables) + "/data/replicate",
                method=event["originalHttpMethod"], body=self.hydrated(tables, event),
                role="system", expected=(200, 201))

    def delivery(self, scenario):
        if scenario != "duplicate":
            return self.reordered(scenario)
        tables = self.table(scenario)
        self.write(tables, "POST", "original")
        self.converged(tables, [{"id": 1, "value": "original"}])
        old = self.exported(tables)
        key = old["payload"]["tuple"]["replicationKey"]
        for i in (1, 2):
            before = self.observe(i, tables, key)
            receipt = self.retained(before, old, tables[i]["id"])
            require(self.send_event(i, tables, old) == receipt, "Duplicate did not return its original receipt")
            after = self.observe(i, tables, key)
            require(all(after[k] == before[k] for k in ("inbox", "history", "heads")),
                    "Duplicate changed inbox, history, or ordering head")
        self.converged(tables, [{"id": 1, "value": "original"}])

    def reordered(self, scenario):
        self.require_faults()
        tables = self.table(scenario)
        policy = "latest-only" if scenario == "reorder" else "delete-only"
        try:
            self.fault("block", tables, policy)
            self.write(tables, "POST", "original")
            self.converged(tables, [{"id": 1, "value": "original"}], (0, 1))
            old = self.exported(tables)
            if scenario == "reorder":
                self.write(tables, "PUT", "changed")
                self.converged(tables, [{"id": 1, "value": "changed"}], (0, 1))
                old = self.exported(tables, "PUT", "changed")
            require(rows(self.sites[2].get(self.path(2, tables) + "/data?page=0&size=100")) == [],
                    "Older versions leaked through the scoped fault")
            self.write(tables, "PUT" if scenario == "reorder" else "DELETE", "latest")
            latest = self.exported(tables, "PUT", "latest") if scenario == "reorder" else self.exported(tables, "DELETE")
            expected = [{"id": 1, "value": "latest"}] if scenario == "reorder" else []
            self.converged(tables, expected)
            key = old["payload"]["tuple"]["replicationKey"]
            before = self.observe(2, tables, key)
            require(len(before["heads"]) == 1 and before["heads"][0]["sequence"] == latest["payload"]["eventSequence"],
                    "Latest retained source sequence is not the target head")
            require(rows(before["history"]) == expected, "An unseen older version was made locally visible")
            receipt = self.send_event(2, tables, old)
            require(receipt.get("applied") is False and not receipt.get("insertedAt") and not receipt.get("deletedAt"),
                    "Stale unseen event fabricated a local version/period")
            after = self.observe(2, tables, key)
            require(self.retained(after, old, tables[2]["id"]) == receipt, "Stale event was not retained")
            require(after["history"] == before["history"] and after["heads"] == before["heads"],
                    "Stale event changed local visibility or ordering head")
            require(self.send_event(2, tables, old) == receipt, "Stale event duplicate changed its receipt")
            self.converged(tables, expected)
            self.report.setdefault("stale_events", []).append({"event_id": old["payload"]["eventId"],
                "retained": True, "locally_visible": False, "history_sha256": fingerprint(after["history"])})
        finally:
            self.fault("unblock", tables, policy)
        self.retry(tables, required=False)

    def require_faults(self):
        if not self.args.allow_faults or not self.config.get("fault_hook") or self.config.get("scoped_faults_coordinated") is not True:
            raise Blocked("Scoped faults require --allow-faults, scoped_faults_coordinated=true, and fault_hook")

    def fault(self, action, tables, policy="all"):
        scope = {"run": self.name, "database": self.sites[2].db["internal_name"],
                 "database_id": uid(self.sites[2].db["id"]),
                 "table": tables[2]["internal_name"], "table_id": uid(tables[2]["id"]),
                 "source_database_id": uid(self.primary.db["id"]), "source_table_id": uid(tables[0]["id"]),
                 "target_url": self.sites[2].url, "action": action, "policy": policy,
                 "coordinated": True, "ttl_seconds": self.args.deadline + 60}
        result = json.loads(command(self.config["fault_hook"], canonical(scope), self.args.http_timeout))
        require(result == scope, "Fault hook did not acknowledge exact fixture scope/action")

    def queue(self, tables):
        entries = self.primary.get("/api/replication/outbox", role="system")
        return [e for e in entries if e.get("localDatabaseId") == self.primary.db["id"]
                and e.get("localTableId") == tables[0]["id"] and e.get("targetSiteUrl", "").rstrip("/") == self.sites[2].url]

    def outage(self):
        self.require_faults()
        tables = self.table("outage")
        required_status = self.config.get("outage_wait_for", "PENDING")
        if required_status not in ("PENDING", "FAILED"):
            raise Blocked("outage_wait_for must be PENDING or FAILED")
        try:
            self.fault("block", tables)
            self.write(tables, "POST", "original")
            self.converged(tables, [{"id": 1, "value": "original"}], (0, 1))
            self.outage_key = uid(self.exported(tables)["payload"]["tuple"]["replicationKey"])
            require(rows(self.sites[2].get(self.path(2, tables) + "/data?page=0&size=100")) == [],
                    "Fault did not isolate the fixture")
            self.write(tables, "PUT", "changed")
            self.converged(tables, [{"id": 1, "value": "changed"}], (0, 1))
            self.write(tables, "DELETE")
            self.converged(tables, [], (0, 1))
            self.outage_events = self.journal(tables)
            require([e["originalHttpMethod"] for e in self.outage_events] == ["POST", "PUT", "DELETE"],
                    "Outage source journal is incomplete")
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

    def retry(self, tables, required=True):
        retried = 0
        for entry in sorted(self.queue(tables), key=lambda e: e["createdAt"]):
            if entry["status"] not in ("PENDING", "FAILED"):
                continue
            result = self.primary.get("/api/replication/outbox/" + uid(entry["id"]) + "/retry",
                                      method="POST", role="system")
            require(result.get("retried") is True, "Scoped retry returned retried=false")
            retried += 1
        if not retried and required:
            raise Blocked("Automatic recovery won the race; manual retry was not exercised")
        self.wait(lambda: bool(self.queue(tables)) and all(e["status"] == "SUCCEEDED" for e in self.queue(tables)),
                  "fixture-only delivery queue completion")

    def outage_history(self):
        if not hasattr(self, "outage_tables"):
            raise Blocked("Outage/retry did not complete")
        evidence = []
        for i, site in enumerate(self.sites):
            observed = self.observe(i, self.outage_tables, self.outage_key)
            if i == 0:
                require(rows(observed["history"]) == rows([{"id": 1, "value": "original"}, {"id": 1, "value": "changed"}]),
                        "Source history lost a committed version")
            else:
                receipts = [self.retained(observed, e, self.outage_tables[i]["id"]) for e in self.outage_events]
                visible = {r["insertedAt"] for r in receipts if r.get("applied") is not False and r.get("insertedAt")}
                require({h["start"] for h in observed["history"]} == {period(v) for v in visible},
                        "Local versions do not match applied receipts (unseen history must not be invented)")
                for receipt in receipts:
                    if receipt.get("applied") is False:
                        require(not receipt.get("insertedAt") and not receipt.get("deletedAt"), "Unseen event has a fabricated period")
                    else:
                        require(bool(receipt.get("insertedAt")), "Applied receipt omitted its local version start")
                        versions = [h for h in observed["history"] if h["start"] == period(receipt["insertedAt"])]
                        require(len(versions) == 1 and rows(versions) == rows([receipt["data"]]),
                                "Applied receipt values differ from native local history")
                        if receipt.get("deletedAt"):
                            require(versions[0]["end"] == period(receipt["deletedAt"]), "Delete receipt period differs from native history")
                require(len(observed["heads"]) == 1 and observed["heads"][0]["sequence"] == self.outage_events[-1]["payload"]["eventSequence"],
                        "Replica ordering head did not retain the delete")
            local = [t for t in observed["timestamps"] if t["site"] == site.url
                     and t["database"] == site.db["id"] and t["table"] == self.outage_tables[i]["id"]]
            require(sorted((t["start"], t["end"]) for t in local) ==
                    sorted((h["start"], h["end"]) for h in observed["history"]), "Local timestamp evidence differs from actual visibility")
            evidence.append(observed)
        require(evidence[0]["timestamps"] == evidence[1]["timestamps"] == evidence[2]["timestamps"],
                "Three-site timestamp evidence has not converged")
        self.report["outage_evidence_sha256"] = fingerprint(evidence)

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

    def offline_canonical_subset(self):
        hook = self.config.get("offline_hook")
        if not self.args.allow_faults or not hook or self.config.get("scoped_faults_coordinated") is not True:
            raise Blocked("Origin-offline citation requires a parent-coordinated, fixture-scoped offline_hook")
        if len(self.subsets) != 3:
            raise Blocked("Immutable origin artifact was not created")
        scope = {"run": self.name, "database_id": uid(self.primary.db["id"]), "subset_id": self.subsets[0]["id"],
                 "origin_url": self.primary.url, "target_urls": [s.url for s in self.sites[1:]],
                 "ttl_seconds": self.args.deadline + 60}
        try:
            block = dict(scope, action="block")
            require(json.loads(command(hook, canonical(block), self.args.http_timeout)) == block, "Offline hook scope mismatch")
            self.primary.request(self.primary.dbpath() + "/subset/" + self.subsets[0]["id"] + "/data?page=0&size=100",
                                 expected=(503,), parse_json=False)
            self.canonical_subset()
        finally:
            unblock = dict(scope, action="unblock")
            require(json.loads(command(hook, canonical(unblock), self.args.http_timeout)) == unblock, "Offline cleanup scope mismatch")

    def snapshot_receipt(self, entry):
        request = json.loads(entry["payloadJson"])
        snapshot = uid(request["snapshotId"])
        require(request.get("checkpointCaptured") is True, "History job omitted its durable pre-request checkpoint")
        target = next(site for site in self.sites[1:] if site.url == entry["targetSiteUrl"])
        source = next(r for r in self.report["resources"] if r["kind"] == "table"
                      and r["site"] == self.primary.config["name"] and r["id"] == entry["localTableId"])
        remote = [r for r in self.report["resources"] if r["kind"] == "table"
                  and r["site"] == target.config["name"] and r["name"] == source["name"]]
        require(len(remote) == 1 and entry.get("remoteDatabaseId") == target.db["id"]
                and entry.get("remoteTableId") == remote[0]["id"], "History job target differs from fixture manifest")
        suffix = "/replication/snapshots/" + snapshot
        envelope = self.primary.get(self.primary.dbpath() + suffix, role="system")
        manifest = envelope["manifest"]
        require(manifest.get("format") == 1 and manifest.get("snapshotId") == snapshot
                and manifest.get("origin") == self.primary.url
                and manifest.get("sourceDatabaseId") == self.primary.db["id"]
                and manifest.get("sourceTableId") == entry["localTableId"]
                and manifest.get("base") == request.get("checkpoint")
                and type(manifest.get("boundary")) is int and manifest["boundary"] >= 0
                and manifest.get("legacyThrough") == 0
                and re.fullmatch(r"[a-f0-9]{64}", envelope.get("sha256") or ""),
                "History source manifest identity/checkpoint/digest mismatch")
        if entry["localTableId"] == self.core[0]["id"]:
            require(manifest.get("rows") == 2 and manifest.get("currentKeys") == 0,
                    "Roundtrip snapshot did not retain both deleted historical versions")
        receipt = target.get(target.dbpath() + suffix + "/status", role="system")
        require(receipt.get("snapshotId") == snapshot and receipt.get("tableId") == remote[0]["id"]
                and receipt.get("status") == "RECONCILED"
                and receipt.get("historyVerified") is True and receipt.get("currentReconciled") is True
                and receipt.get("boundary") == manifest["boundary"]
                and receipt.get("legacyThrough") == manifest["legacyThrough"]
                and receipt.get("manifestDigest") == envelope["sha256"],
                "Succeeded history job lacks the matching verified/reconciled target receipt")
        return {"job": entry["id"], "target": target.config["name"], "receipt": receipt}

    def history_sync(self, database=False):
        if not hasattr(self, "core"):
            raise Blocked("History sync requires the newly created core fixture")
        source_tables = {r["id"] for r in self.report["resources"] if r["kind"] == "table"
                         and r["site"] == self.primary.config["name"]} if database else {self.core[0]["id"]}
        expected = {(table, site.url) for table in source_tables for site in self.sites[1:]}
        path = "/api/replication/data/synchronise/database/" + uid(self.primary.db["id"])
        if not database:
            path += "/table/" + uid(self.core[0]["id"])
        queued = self.primary.get(path, method="POST", role="system", expected=(202,))
        require(queued.get("status") == "queued" and isinstance(queued.get("jobs"), list)
                and 0 < len(queued["jobs"]) <= 100, "History sync did not return bounded durable job IDs")
        jobs = [uid(job) for job in queued["jobs"]]
        require(len(set(jobs)) == len(jobs), "History sync returned duplicate job IDs")
        require(len(jobs) == len(expected), "History sync did not queue every fixture table/peer pair")
        if database:
            require(type(queued.get("tables")) is int and queued["tables"] == len(source_tables),
                    "Database sync queued table count differs from the fixture manifest")
        record = {"scope": "database" if database else "table", "jobs": jobs, "status": "QUEUED"}
        self.report.setdefault("history_sync", []).append(record)
        self.save()
        deadline = time.monotonic() + self.args.sync_wait
        while True:
            require(time.monotonic() < deadline, "Timed out awaiting every returned HISTORY_SYNC job")
            entries = self.primary.get("/api/replication/outbox", role="system")
            require(time.monotonic() < deadline, "Timed out awaiting every returned HISTORY_SYNC job")
            selected = [entry for entry in entries if entry.get("id") in jobs]
            require(len(selected) == len(jobs) and {e["id"] for e in selected} == set(jobs),
                    "A returned durable history job is missing or duplicated in the outbox")
            for entry in selected:
                require(entry.get("operationType") == "HISTORY_SYNC" and entry.get("localDatabaseId") == self.primary.db["id"]
                        and (entry.get("localTableId"), entry.get("targetSiteUrl")) in expected,
                        "History job escaped the returned fixture scope")
            require({(e["localTableId"], e["targetSiteUrl"]) for e in selected} == expected,
                    "History jobs omit a fixture table/peer pair")
            record["states"] = {entry["id"]: entry["status"] for entry in selected}
            self.save()
            states = set(record["states"].values())
            require(states <= {"PENDING", "SUCCEEDED", "FAILED", "CANCELLED"}, "Unknown history job state")
            require(not states.intersection({"FAILED", "CANCELLED"}), "History job permanently failed or was cancelled")
            if states == {"SUCCEEDED"}:
                record["snapshots"] = [self.snapshot_receipt(entry) for entry in selected]
                require(time.monotonic() < deadline, "Timed out verifying completed HISTORY_SYNC snapshots")
                record["status"] = "SUCCEEDED"
                self.save()
                self.converged(self.core, [])
                return
            require(time.monotonic() < deadline, "Timed out awaiting every returned HISTORY_SYNC job")
            time.sleep(min(self.args.poll, max(0, deadline - time.monotonic())))

    def run(self):
        if self.check("fixture_setup", self.setup):
            self.check("primary_insert_update_delete_and_local_fixity", self.crud)
            self.check("replica_user_rejection", self.rejection)
            self.check("duplicate_delivery_no_extra_history", lambda: self.delivery("duplicate"))
            self.check("older_update_cannot_reset_newer", lambda: self.delivery("reorder"))
            self.check("older_insert_cannot_resurrect_delete", lambda: self.delivery("resurrection"))
            self.check("outage_and_scoped_manual_retry", self.outage)
            self.check("outage_retained_events_and_actual_local_visibility", self.outage_history)
            self.check("table_history_sync_jobs_complete", self.history_sync)
            self.check("database_history_sync_jobs_complete", lambda: self.history_sync(database=True))
            self.check("archive_preserves_local_subset_fixity", self.archive)
            self.check("canonical_origin_subset_replays_on_peers_after_archive", self.canonical_subset)
            self.check("offline_canonical_immutable_artifact", self.offline_canonical_subset)
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
    parser.add_argument("--sync-wait", type=int, default=900, help="Bounded wait for each set of HISTORY_SYNC jobs")
    parser.add_argument("--poll", type=float, default=2)
    parser.add_argument("--deadline", type=int, default=1800)
    args = parser.parse_args()
    runner = None
    owns_report = False
    try:
        if not args.allow_writes:
            raise Blocked("Explicit --allow-writes required; no network requests made")
        if not (1 <= args.http_timeout <= 120 and 1 <= args.wait <= 600 and 0.1 <= args.poll <= 30
                and 1 <= args.deadline <= 7200 and 1 <= args.sync_wait <= 3600):
            raise Blocked("Invalid bounds: HTTP 1..120s, wait 1..600s, sync 1..3600s, poll 0.1..30s, deadline 1..7200s")
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
