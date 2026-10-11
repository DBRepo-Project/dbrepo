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
# Plugin versions must be compatible with the Grafana image; update the image and both plugins together.
IMAGE = "grafana/grafana:11.6.16"
PLUGINS = ["yesoreyeram-infinity-datasource 4.0.0", "grafana-opensearch-datasource 2.34.4"]


def check():
    for compose in COMPOSE_FILES:
        result = subprocess.run(
            ["docker", "compose", "-f", str(ROOT / compose), "config", "--format", "json"],
            env=ENV, capture_output=True, text=True, check=True,
        )
        service = json.loads(result.stdout)["services"]["dbrepo-dashboard-ui"]
        environment = service["environment"]
        # An unpinned plugin installs the newest release, which can require a newer Grafana.
        assert environment["GF_INSTALL_PLUGINS"].split(",") == PLUGINS, (compose, environment["GF_INSTALL_PLUGINS"])
        # Grafana otherwise installs unpinned default apps in the background.
        assert environment["GF_PLUGINS_PREINSTALL_DISABLED"] == "true", compose
        assert service["image"] == f"docker.io/{IMAGE}", (compose, service["image"])
        for volume in service["volumes"]:
            if volume["source"].endswith((".ini", ".yaml")):
                assert volume["target"].startswith("/etc/grafana/"), (compose, volume["target"])
    # The chart's default values do not render on their own, so read the dashboardui block directly.
    values = (ROOT / "helm/dbrepo/values.yaml").read_text()
    dashboardui = re.search(r"^dashboardui:\n(.*?)^\S", values, re.M | re.S).group(1)
    plugins = re.search(r"^  plugins:\n((?:    - .*\n)+)", dashboardui, re.M).group(1)
    assert [line.strip().removeprefix("- ") for line in plugins.splitlines()] == PLUGINS, ("helm", plugins)
    assert re.search(r'^    GF_PLUGINS_PREINSTALL_DISABLED: "true"$', dashboardui, re.M), "helm"
    image = [re.search(rf"^    {key}: (\S+)$", dashboardui, re.M)[1] for key in ("repository", "tag")]
    assert ":".join(image) == IMAGE, ("helm", image)
    print("PASS: all Compose files and the Helm chart pin Grafana plugins for the official image")


if __name__ == "__main__":
    check()
