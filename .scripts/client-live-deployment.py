#!/usr/bin/env python3
"""Collect running application image IDs for a live-check report."""

import argparse
import json
from pathlib import Path
import re
import subprocess
from urllib.parse import urlsplit

from client_live_evidence import deployment


def output(*args):
    return subprocess.check_output(args, text=True, timeout=25).strip()


def selector(spec):
    terms = [f"{k}={v}" for k, v in spec.get("matchLabels", {}).items()]
    for expression in spec.get("matchExpressions", []):
        key = expression["key"]
        op = expression["operator"]
        if op in ("In", "NotIn"):
            terms.append(f"{key} {op.lower()} ({','.join(expression['values'])})")
        elif op in ("Exists", "DoesNotExist"):
            terms.append(key if op == "Exists" else "!" + key)
        else:
            raise ValueError(f"Unsupported pod selector operator {op}")
    if not terms:
        raise ValueError("Deployment has no pod selector")
    return ",".join(terms)


def check_revision(image, commit):
    tag = re.search(r":sha-([0-9a-f]{40})(?:@|$)", image)
    if tag and tag[1] != commit:
        raise ValueError(f"Image tag refers to {tag[1]} instead of the supplied commit")


def one_digest(records, component, commit):
    digests = set()
    for reference, identity in records:
        check_revision(reference, commit)
        match = re.search(r"(?:^|@|://)(sha256:[0-9a-f]{64})$", identity)
        if not match:
            raise ValueError(f"{component} has no immutable running image ID")
        digests.add(match[1])
    if len(digests) != 1:
        raise ValueError(f"{component} must have one running image across its replicas")
    return digests.pop()


def kubernetes_image(namespace, component, workload, commit):
    kubectl = ["kubectl", "--request-timeout=12s", "--namespace", namespace, "get"]
    name, _, container = workload.partition("/")
    status = output(*kubectl, "deployment", name, "-o", "jsonpath="
                    "{.metadata.generation}{' '}{.status.observedGeneration}{' '}{.spec.replicas}{' '}"
                    "{.status.updatedReplicas}{' '}{.status.readyReplicas}{' '}{.status.availableReplicas}{'\\n'}"
                    "{.spec.selector}{'\\n'}{.spec.template.spec.containers[*].name}").splitlines()
    counts = status[0].split()
    if (len(counts) != 6 or counts[0] != counts[1] or counts[2] == "0"
            or any(count != counts[2] for count in counts[3:])):
        raise ValueError(f"{name} has not completed its rollout")
    names = status[2].split()
    if not container:
        container = name if name in names else None
        if len(names) == 1:
            container = names[0]
    if container not in names:
        raise ValueError(f"Specify the application container as {component}={name}/CONTAINER")
    # Only project image identities and readiness. Pod configuration can contain credentials.
    template = ('go-template={{range .items}}{{if not .metadata.deletionTimestamp}}'
                '{{$phase := .status.phase}}{{range .status.containerStatuses}}'
                '{{printf "%s\\t%s\\t%t\\t%s\\t%s\\n" $phase .name .ready .image .imageID}}'
                '{{end}}{{end}}{{end}}')
    rows = [row.split("\t") for row in output(*kubectl, "pods", "-l", selector(json.loads(status[1])),
                                            "-o", template).splitlines()]
    selected = [row for row in rows if len(row) == 5 and row[1] == container]
    if len(selected) != int(counts[2]) or any(row[0] != "Running" or row[2] != "true" for row in selected):
        raise ValueError(f"{name} has missing or unready replicas")
    return one_digest([(row[3], row[4]) for row in selected], component, commit)


def compose_image(project, component, service, commit):
    ids = output("docker", "ps", "--all", "--filter", f"label=com.docker.compose.project={project}",
                 "--filter", f"label=com.docker.compose.service={service}", "--format", "{{.ID}}").split()
    if not ids:
        raise ValueError(f"No running containers for Compose service {service}")
    template = ('{{index .Config.Labels "com.docker.compose.oneoff"}}|{{.State.Status}}|'
                '{{if .State.Health}}{{.State.Health.Status}}{{end}}|{{.Config.Image}}|{{.Image}}')
    records = []
    for row in output("docker", "inspect", "--format", template, *ids).splitlines():
        oneoff, state, health, image, identity = row.split("|")
        if oneoff.lower() == "true":
            continue
        if state != "running" or health not in ("", "healthy"):
            raise ValueError(f"Compose service {service} has an unready container")
        records.append((image, identity))
    return one_digest(records, component, commit)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    target = parser.add_mutually_exclusive_group(required=True)
    target.add_argument("--kubernetes", metavar="NAMESPACE")
    target.add_argument("--compose", metavar="PROJECT")
    parser.add_argument("--endpoint", required=True)
    parser.add_argument("--commit", required=True, help="Full source SHA verified against the deployment record")
    parser.add_argument("--output", required=True, type=Path)
    parser.add_argument("components", nargs="+", metavar="COMPONENT[=WORKLOAD]")
    args = parser.parse_args()
    try:
        endpoint = urlsplit(args.endpoint)
        if (endpoint.scheme not in ("http", "https") or not endpoint.hostname or endpoint.username is not None
                or endpoint.password is not None or endpoint.query or endpoint.fragment):
            raise ValueError("Provide the public endpoint without credentials, a query or a fragment")
        if not re.fullmatch("[0-9a-f]{40}", args.commit):
            raise ValueError("Provide a full source commit")
        images = {}
        for entry in args.components:
            component, _, workload = entry.partition("=")
            if not re.fullmatch("[a-zA-Z0-9][a-zA-Z0-9_.-]*", component) or component in images:
                raise ValueError(f"Invalid or duplicate component {component}")
            if workload and not re.fullmatch(r"[a-zA-Z0-9][a-zA-Z0-9_.-]*(/[a-zA-Z0-9][a-zA-Z0-9_.-]*)?", workload):
                raise ValueError(f"Invalid workload for {component}")
            if args.kubernetes:
                images[component] = kubernetes_image(args.kubernetes, component, workload or component, args.commit)
            else:
                images[component] = compose_image(args.compose, component, workload or "dbrepo-" + component, args.commit)
        evidence = deployment({"endpoint": args.endpoint, "commit": args.commit, "images": images}, args.endpoint)
        with args.output.open("x") as stream:
            json.dump(evidence, stream, indent=2, sort_keys=True)
            stream.write("\n")
    except (ValueError, OSError, subprocess.SubprocessError) as error:
        parser.exit(1, f"{error}\n")


if __name__ == "__main__":
    main()
