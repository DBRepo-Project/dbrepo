#!/usr/bin/env python3
"""Run with python3 .scripts/test-grafana-plugins.py; requires Docker Compose, not a daemon."""

import json
import os
from pathlib import Path
import re
import subprocess


ROOT = Path(__file__).resolve().parents[1]
# Do not inherit deployment settings or Compose overrides from the calling shell.
ENV = {key: os.environ[key] for key in ("PATH", "HOME") if key in os.environ}
COMPOSE_FILES = (
    "docker-compose.yml",
    ".docker/docker-compose.yml",
    ".live/config/includes.chroot_after_packages/etc/skel/docker-compose.yml",
)


def check():
    for compose in COMPOSE_FILES:
        result = subprocess.run(
            ["docker", "compose", "-f", str(ROOT / compose), "config", "--format", "json"],
            env=ENV, capture_output=True, text=True, check=True,
        )
        service = json.loads(result.stdout)["services"]["dbrepo-dashboard-ui"]
        environment = service["environment"]
        # An unpinned plugin installs the newest release, which can require a newer Grafana.
        for plugin in environment["GF_INSTALL_PLUGINS"].split(","):
            assert len(plugin.split()) == 2, (compose, plugin)
        # Grafana otherwise installs unpinned default apps in the background.
        assert environment["GF_PLUGINS_PREINSTALL_DISABLED"] == "true", compose
        assert service["image"].startswith("docker.io/grafana/grafana:"), (compose, service["image"])
        for volume in service["volumes"]:
            if volume["source"].endswith((".ini", ".yaml")):
                assert volume["target"].startswith("/etc/grafana/"), (compose, volume["target"])
    # The chart's default values do not render on their own, so read the dashboardui block directly.
    values = (ROOT / "helm/dbrepo/values.yaml").read_text()
    dashboardui = re.search(r"^dashboardui:\n(.*?)^\S", values, re.M | re.S).group(1)
    plugins = re.search(r"^  plugins:\n((?:    - .*\n)+)", dashboardui, re.M).group(1)
    for plugin in plugins.splitlines():
        assert len(plugin.split()) == 3, ("helm", plugin)
    assert re.search(r'^    GF_PLUGINS_PREINSTALL_DISABLED: "true"$', dashboardui, re.M), "helm"
    assert re.search(r"^    repository: grafana/grafana$", dashboardui, re.M), "helm"
    print("PASS: all Compose files and the Helm chart pin Grafana plugins for the official image")


if __name__ == "__main__":
    check()
