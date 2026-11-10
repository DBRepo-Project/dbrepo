"""Post-fix monitoring acceptance with disposable native services and synthetic data.

Uses OpenSearch 2.18, Prometheus 2.54.1, Grafana 11.6.16 and the pinned plugin.
The test covers native query/proxy authorization and organization placement.
"""

import datetime
import json
import os
import signal
import uuid
import socket
import subprocess
import sys
import time
from pathlib import Path
import requests

ROOT = Path(os.environ["DBREPO_NATIVE_FIXTURE_DIR"])
REPO = Path(__file__).resolve().parents[2]
sys.path.insert(0, str(REPO / "dbrepo-dashboard-service"))
from provision import GrafanaSetup

NAME = "dbrepo-monitoring-test-" + uuid.uuid4().hex[:10]
STARTED = []

def stop_fixture(*_):
    raise SystemExit(1)

signal.signal(signal.SIGTERM, stop_fixture)
RESULTS = []
ADMIN = ("nativeadmin", "synthetic-native-admin-password")
VIEWER = ("ordinary-viewer", "synthetic-native-viewer-password")
MARKER = "NATIVE_MONITORING_REGRESSION"


def record(event, **values):
    result = {"event": event, **values}
    RESULTS.append(result)
    print(json.dumps(result), flush=True)


def port():
    with socket.socket() as s:
        s.bind(("127.0.0.1", 0))
        return s.getsockname()[1]


def session(auth=None):
    s = requests.Session()
    s.trust_env = False
    s.auth = auth
    return s


def run(name, image, environment=None, extra=()):
    command = ["docker", "run", "-d", "--name", name, "--network", NAME]
    for key, value in (environment or {}).items():
        command += ["-e", key + "=" + value]
    STARTED.append(name)
    subprocess.run(
        command + list(extra) + [image],
        check=True,
        stdout=subprocess.DEVNULL,
        timeout=120,
    )


def healthy(url, timeout=120):
    until = time.monotonic() + timeout
    while time.monotonic() < until:
        try:
            if session().get(url, timeout=2).status_code == 200:
                return
        except requests.RequestException:
            pass
        time.sleep(1)
    raise RuntimeError("Native fixture startup timeout: " + url)


os_port, gf_port, prom_port = port(), port(), port()
OS = "http://127.0.0.1:" + str(os_port)
BASE = "http://127.0.0.1:" + str(gf_port)
PROM = "http://127.0.0.1:" + str(prom_port)
(ROOT / "prometheus.yml").write_text(
    "global:\n  scrape_interval: 1s\nscrape_configs:\n  - job_name: native-regression\n    static_configs:\n      - targets: [localhost:9090]\n"
)
try:
    subprocess.run(
        ["docker", "network", "create", NAME],
        check=True,
        stdout=subprocess.DEVNULL,
        timeout=15,
    )
    run(
        NAME + "-os",
        "bitnamilegacy/opensearch:2.18.0",
        {
            "OPENSEARCH_ENABLE_SECURITY": "false",
            "OPENSEARCH_HEAP_SIZE": "512m",
            "OPENSEARCH_BIND_ADDRESS": "0.0.0.0",
            "OPENSEARCH_NODE_NAME": "native-regression",
        },
        ["--network-alias", "search-db", "-p", f"127.0.0.1:{os_port}:9200"],
    )
    run(
        NAME + "-prom",
        "docker.io/bitnamilegacy/prometheus:2.54.1",
        {},
        [
            "--network-alias",
            "metric-db",
            "-p",
            f"127.0.0.1:{prom_port}:9090",
            "-v",
            str(ROOT / "prometheus.yml")
            + ":/opt/bitnami/prometheus/conf/prometheus.yml:ro",
        ],
    )
    run(
        NAME + "-gf",
        "grafana/grafana:11.6.16",
        {
            "GF_SECURITY_ADMIN_USER": ADMIN[0],
            "GF_SECURITY_ADMIN_PASSWORD": ADMIN[1],
            "GF_AUTH_ANONYMOUS_ENABLED": "true",
            "GF_AUTH_ANONYMOUS_ORG_ROLE": "Viewer",
            "GF_RBAC_PERMISSION_CACHE": "false",
            "GF_LOG_LEVEL": "warn",
            "GF_PLUGINS_PREINSTALL_DISABLED": "true",
            "GF_INSTALL_PLUGINS": "yesoreyeram-infinity-datasource 4.0.0,grafana-opensearch-datasource 2.34.4",
        },
        ["-p", f"127.0.0.1:{gf_port}:3000"]
        + (
            ["-v", os.environ["DBREPO_NATIVE_PLUGINS"] + ":/var/lib/grafana/plugins:ro"]
            if os.getenv("DBREPO_NATIVE_PLUGINS")
            else []
        ),
    )
    healthy(OS)
    healthy(PROM + "/-/ready")
    healthy(BASE + "/api/health")
    version = session().get(OS, timeout=5).json()["version"]["number"]
    assert version == "2.18.0"
    response = session().put(
        OS + "/dbrepo-native-regression/_doc/1?refresh=true",
        json={
            "@timestamp": datetime.datetime.now(datetime.timezone.utc).isoformat(),
            "message": MARKER,
            "level": "INFO",
        },
        timeout=10,
    )
    response.raise_for_status()
    setup = GrafanaSetup(BASE, *ADMIN)
    setup.request("POST", "/orgs", {"name": "Unrelated existing organization"})
    monitor = setup.configure(
        "http://backend.invalid",
        "DBRepo Monitoring",
        metrics_url="http://metric-db:9090",
        search_url="http://search-db:9200",
        dashboards_dir=REPO / "dbrepo-dashboard-ui/dashboards/System",
    )
    assert monitor != 2
    setup.request(
        "POST",
        "/admin/users",
        {
            "login": VIEWER[0],
            "email": "viewer@synthetic.invalid",
            "password": VIEWER[1],
        },
    )
    record(
        "versions",
        grafana=setup.request("GET", "/health")["version"],
        opensearch=version,
        opensearch_plugin="2.34.4",
        prometheus="2.54.1",
        monitoring_org=monitor,
    )
    os_query = {
        "queries": [
            {
                "refId": "A",
                "datasource": {
                    "uid": "dbrepoopensearch0",
                    "type": "grafana-opensearch-datasource",
                },
                "query": MARKER,
                "queryType": "lucene",
                "luceneQueryType": "Logs",
                "metrics": [{"id": "1", "type": "logs"}],
                "bucketAggs": [],
                "timeField": "@timestamp",
                "format": "table",
                "intervalMs": 1000,
                "maxDataPoints": 100,
            }
        ],
        "from": str(int((time.time() - 300) * 1000)),
        "to": str(int((time.time() + 300) * 1000)),
    }
    prom_query = {
        "queries": [
            {
                "refId": "A",
                "datasource": {"uid": "dbrepometrics0", "type": "prometheus"},
                "expr": "vector(1)",
                "instant": True,
                "range": False,
                "intervalMs": 1000,
                "maxDataPoints": 100,
            }
        ],
        "from": str(int((time.time() - 60) * 1000)),
        "to": str(int(time.time() * 1000)),
    }
    for label, auth in [("admin", ADMIN), ("viewer", VIEWER), ("anonymous", None)]:
        client = session(auth)
        headers = {"X-Grafana-Org-Id": str(monitor)}
        for backend, payload, path, marker in [
            ("opensearch", os_query, "/dbrepo-native-regression/_search", MARKER),
            (
                "prometheus",
                prom_query,
                "/api/v1/query?query=vector(1)",
                '"status":"success"',
            ),
        ]:
            uid = "dbrepoopensearch0" if backend == "opensearch" else "dbrepometrics0"
            result = client.post(
                BASE + "/api/ds/query", json=payload, headers=headers, timeout=30
            )
            proxy = client.get(
                BASE + "/api/datasources/proxy/uid/" + uid + path,
                headers=headers,
                timeout=20,
            )
            lookup = client.get(
                BASE + "/api/datasources/uid/" + uid, headers=headers, timeout=10
            )
            record(
                "monitoring-access",
                role=label,
                backend=backend,
                query_status=result.status_code,
                proxy_status=proxy.status_code,
                lookup_status=lookup.status_code,
                marker_returned=marker in proxy.text,
                query_errors=[
                    r.get("error") for r in result.json().get("results", {}).values()
                ],
            )
            if label == "admin":
                assert result.status_code == 200, result.text
                assert all(
                    not r.get("error") for r in result.json()["results"].values()
                ), result.text
                assert proxy.status_code == 200 and marker in proxy.text, proxy.text
                assert lookup.status_code == 200
                if backend == "opensearch":
                    assert MARKER in result.text, result.text
            else:
                assert result.status_code in (401, 403, 404), result.text
                assert proxy.status_code in (401, 403, 404) and marker not in proxy.text
                assert lookup.status_code in (401, 403, 404)
    assert (
        setup.configure(
            "http://backend.invalid",
            "DBRepo Monitoring",
            metrics_url="http://metric-db:9090",
            search_url="http://search-db:9200",
            dashboards_dir=REPO / "dbrepo-dashboard-ui/dashboards/System",
        )
        == monitor
    )
    assert [s["uid"] for s in setup.request("GET", "/datasources")] == ["dbrepojson0"]
    record(
        "summary",
        passed=True,
        current_setup_used=True,
        native_query_and_proxy=True,
        repeated_setup=True,
        research_contains_only_caller_source=True,
    )
finally:
    (ROOT / "results.json").write_text(json.dumps(RESULTS, indent=2))
    cleanup_failed = []
    for name in reversed(STARTED):
        try:
            result = subprocess.run(
                ["docker", "logs", name], capture_output=True, text=True, timeout=5
            )
            (ROOT / (name + ".log")).write_text(result.stdout + result.stderr)
        except subprocess.TimeoutExpired:
            pass
        try:
            result = subprocess.run(
                ["docker", "rm", "-f", name], capture_output=True, text=True, timeout=10
            )
            if result.returncode and "No such container" not in result.stderr:
                cleanup_failed.append(name)
        except subprocess.TimeoutExpired:
            cleanup_failed.append(name)
    try:
        result = subprocess.run(
            ["docker", "network", "rm", NAME],
            capture_output=True,
            text=True,
            timeout=10,
        )
        if result.returncode and "not found" not in result.stderr:
            cleanup_failed.append(NAME)
    except subprocess.TimeoutExpired:
        cleanup_failed.append(NAME)
    if cleanup_failed:
        record("cleanup-pending", resources=cleanup_failed)
        raise RuntimeError(
            "Owned fixture cleanup failed; see the native fixture output"
        )
