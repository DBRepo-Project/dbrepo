#!/usr/bin/env python3
"""Check the service URLs shipped by Helm, Compose and the Metadata Service."""
import subprocess
from pathlib import Path

import yaml

ROOT = Path(__file__).resolve().parents[1]
CROSSREF = "https://data.crossref.org"


def check_chart(data_endpoint, crossref_endpoint, *overrides):
    command = ["helm", "template", "endpoints", str(ROOT / "helm/dbrepo"), "--set", "ui.enabled=false"]
    for template in ("consumer-secret", "metadata-secret", "data-service"):
        command.extend(["--show-only", f"templates/{template}.yaml"])
    for key, value in overrides:
        command.extend(["--set-string", f"{key}={value}"])
    manifests = yaml.safe_load_all(subprocess.check_output(command, text=True))
    by_name = {doc["metadata"]["name"]: doc for doc in manifests if doc}
    assert by_name["consumer-service-secret"]["stringData"]["DATA_SERVICE_ENDPOINT"] == data_endpoint
    assert by_name["metadata-service-secret"]["stringData"]["CROSSREF_ENDPOINT"] == crossref_endpoint
    service_port = by_name["data-service"]["spec"]["ports"][0]
    assert (service_port["port"], service_port["targetPort"]) == (80, 8080)


def main():
    check_chart("http://data-service", CROSSREF)
    check_chart("https://data.example:8443", "https://crossref.example",
                ("dataservice.endpoint", "https://data.example:8443"),
                ("metadataservice.crossref.endpoint", "https://crossref.example"))
    compose_crossref = "${CROSSREF_ENDPOINT:-" + CROSSREF + "}"
    compose_data = "${DATA_SERVICE_ENDPOINT:-http://data-service:8080}"
    for path in ("docker-compose.yml", ".docker/docker-compose.yml",
                 ".live/config/includes.chroot_after_packages/etc/skel/docker-compose.yml"):
        services = yaml.safe_load((ROOT / path).read_text())["services"]
        assert services["dbrepo-metadata-service"]["environment"]["CROSSREF_ENDPOINT"] == compose_crossref
        assert services["dbrepo-consumer-service"]["environment"]["DATA_SERVICE_ENDPOINT"] == compose_data
    config = ROOT / "dbrepo-metadata-service/rest-service/src/main/resources/application.yml"
    endpoint = yaml.safe_load(config.read_text())["dbrepo"]["endpoints"]["crossRefService"]
    assert endpoint == "${CROSSREF_ENDPOINT:" + CROSSREF + "}"
    print("Verified service endpoint defaults and Helm overrides")


if __name__ == "__main__":
    main()
