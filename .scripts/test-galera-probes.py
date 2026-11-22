#!/usr/bin/env python3
"""Render and exercise the Galera probes without a running Kubernetes cluster."""

import os
from pathlib import Path
import subprocess
import tempfile

import yaml

CHART = Path(__file__).resolve().parents[1] / "helm/dbrepo"
SYNCED = "wsrep_cluster_status\tPrimary\nwsrep_local_state_comment\tSynced\nwsrep_ready\tON\n"


def check():
    with tempfile.TemporaryDirectory() as tmp:
        directory = Path(tmp)
        client = directory / "mariadb"
        client.write_text('#!/bin/sh\n[ "$MYSQL_PWD" = fixture ] || exit 1\nprintf "%s" "$STATUS"\n')
        client.chmod(0o700)
        password_file = directory / "password"
        password_file.write_text("fixture")
        rendered = subprocess.check_output([
            "helm", "template", "test", str(CHART), "--set-string",
            "ui.oidc.tokenKey=fixture,ui.oidc.sessionSecret=fixture,ui.oidc.authSessionSecret=fixture",
            "--set-string", f"datadb.persistence.mountPath={tmp},metadatadb.persistence.mountPath={tmp}",
        ], text=True)
        sets = {obj["metadata"]["name"]: obj for obj in yaml.safe_load_all(rendered)
                if obj and obj["kind"] == "StatefulSet"}
        for name in ("data-db", "metadata-db"):
            db = sets[name]["spec"]["template"]["spec"]["containers"][0]
            assert db["livenessProbe"]["tcpSocket"]["port"] == "mysql"
            startup = db["startupProbe"]
            assert startup["periodSeconds"] * startup["failureThreshold"] >= 1800
            command = db["readinessProbe"]["exec"]["command"]
            env = dict(os.environ, PATH=f"{tmp}:{os.environ['PATH']}", MARIADB_ROOT_USER="root",
                       MARIADB_ROOT_PASSWORD="fixture", STATUS=SYNCED)
            marker = directory / ".user_scripts_initialized"
            assert subprocess.run(command, env=env).returncode != 0
            marker.touch()
            subprocess.run(command, env=env, check=True)
            env.update(MARIADB_ROOT_PASSWORD_FILE=str(password_file), MARIADB_ROOT_PASSWORD="")
            subprocess.run(command, env=env, check=True)
            for status in (SYNCED.replace("Primary", "non-Primary"), SYNCED.replace("Synced", "Joining"),
                           SYNCED.replace("ON", "OFF"), ""):
                assert subprocess.run(command, env={**env, "STATUS": status}).returncode != 0
            marker.unlink()


if __name__ == "__main__":
    check()
