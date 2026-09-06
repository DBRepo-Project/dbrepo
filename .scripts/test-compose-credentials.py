#!/usr/bin/env python3
"""Run with python3 .scripts/test-compose-credentials.py; requires Docker Compose, not a daemon."""

import configparser
import json
import os
from pathlib import Path
import subprocess
import tempfile


ROOT = Path(__file__).resolve().parents[1]
# Do not inherit deployment secrets or Compose overrides from the calling shell.
ENV = {key: os.environ[key] for key in ("PATH", "HOME") if key in os.environ}


def check():
    with tempfile.TemporaryDirectory() as directory:
        work = Path(directory)
        env_file = work / ".env"
        for custom in (False, True):
            credentials = {
                "S3_ACCESS_KEY_ID": "test-user-key",
                "S3_SECRET_ACCESS_KEY": "test-user-secret",
                "S3_ADMIN_ACCESS_KEY_ID": "test-admin-key",
                "S3_ADMIN_SECRET_ACCESS_KEY": "test-admin-secret",
            } if custom else {}
            env_file.write_text("".join(f"{key}={value}\n" for key, value in credentials.items()))
            for compose in ("docker-compose.yml", ".docker/docker-compose.yml"):
                result = subprocess.run(
                    ["docker", "compose", "--env-file", str(env_file), "-f",
                     str(ROOT / compose), "config", "--format", "json"],
                    env=ENV, capture_output=True, text=True, check=True,
                )
                services = json.loads(result.stdout)["services"]
                for service, names, source_names, fallback in (
                    ("dbrepo-metadata-service", ("S3_ACCESS_KEY_ID", "S3_SECRET_ACCESS_KEY"),
                     ("S3_ACCESS_KEY_ID", "S3_SECRET_ACCESS_KEY"), "seaweedfsuser"),
                    ("dbrepo-data-service", ("S3_ACCESS_KEY", "S3_SECRET_KEY"),
                     ("S3_ACCESS_KEY_ID", "S3_SECRET_ACCESS_KEY"), "seaweedfsuser"),
                    ("dbrepo-storage-service-init", ("S3_ACCESS_KEY_ID", "S3_SECRET_ACCESS_KEY"),
                     ("S3_ADMIN_ACCESS_KEY_ID", "S3_ADMIN_SECRET_ACCESS_KEY"), "seaweedfsadmin"),
                ):
                    for name, source_name in zip(names, source_names):
                        assert services[service]["environment"].get(name) == credentials.get(source_name, fallback), (compose, service, name)

        # Execute only database config generation: no secret rotation, TLS creation or sudo.
        script = (ROOT / ".scripts/gen-secrets.sh").read_text()
        database_setup = script[script.index('SECRET_PATH="./dbrepo-metadata-db"'):]
        for install, target in (("0", "dbrepo-metadata-db"), ("1", "config")):
            (work / target).mkdir()
            subprocess.run(
                ["bash", "-eu", "-c", database_setup], cwd=work, check=True,
                env={**ENV, "INSTALL_SCRIPT": install, "READONLY_PASSWORD": "test-readonly-password",
                     "DATA_DB_PASSWORD": "test-root-password"},
            )
            metrics = configparser.ConfigParser()
            metrics.read(work / target / "metrics.cnf")
            assert metrics["client"]["user"] == "readonly"
            assert metrics["client"]["password"] == "test-readonly-password"
    print("PASS: both Compose files, default/custom S3 credentials, admin credentials, and both metrics output paths")


if __name__ == "__main__":
    check()
