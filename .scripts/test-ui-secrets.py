#!/usr/bin/env python3
"""Check generated UI secrets, Compose wiring and rotation without touching a deployment."""
import base64
import importlib.util
import json
import os
from pathlib import Path
import subprocess
import tempfile
import yaml

ROOT = Path(__file__).resolve().parents[1]
ENV = {k: os.environ[k] for k in ("PATH", "HOME") if k in os.environ}
KEYS = ("NUXT_OIDC_TOKEN_KEY", "OIDC_AUTH_SESSION_SECRET", "OIDC_SESSION_SECRET")


def read_env(file):
    return dict(line.split("=", 1) for line in file.read_text().splitlines()
                if "=" in line and not line.startswith("#"))


def render(compose, env_file):
    return subprocess.run(["docker", "compose", "--env-file", str(env_file), "-f", str(ROOT/compose),
                           "config", "--format", "json"], env=ENV, capture_output=True, text=True)


def check_helm_oidc(docs, issuer, jwks):
    secrets = {v["metadata"]["name"]: v["stringData"] for v in docs
               if v and v.get("kind") == "Secret" and "stringData" in v}
    for name in ("metadata-service-secret", "data-service-secret", "consumer-service-secret"):
        assert secrets[name]["OAUTH_ISSUER_URI"] == issuer
        assert secrets[name]["OAUTH_JWK_SET_URI"] == jwks
    assert secrets["ui-secret"]["NUXT_OIDC_PROVIDERS_KEYCLOAK_OPEN_ID_CONFIGURATION_ISSUER"] == issuer
    assert secrets["ui-secret"]["NUXT_OIDC_PROVIDERS_KEYCLOAK_OPEN_ID_CONFIGURATION_JWKS_URI"] == jwks


def main():
    script = (ROOT/".scripts/gen-secrets.sh").read_text()
    script = script[:script.index('echo "[🙉]')]
    spec = importlib.util.spec_from_file_location("rotate_ui", ROOT/".scripts/rotate-ui-secrets.py")
    module = importlib.util.module_from_spec(spec); spec.loader.exec_module(module)
    with tempfile.TemporaryDirectory() as directory:
        all_values = []
        for index in range(2):
            work = Path(directory)/str(index); (work/"config").mkdir(parents=True)
            result = subprocess.run(["bash", "-c", script], cwd=work,
                env={**ENV, "INSTALL_SCRIPT":"1"}, capture_output=True, text=True)
            assert result.returncode == 0, "Secret generation failed"
            file = work/".env"
            with file.open("a") as stream:
                stream.write("AUTH_SERVICE_CLIENT=synthetic-client-id\nAUTH_SERVICE_CLIENT_SECRET=synthetic-confidential-secret\n")
                if index:
                    stream.write("BASE_URL=https://review.example\nAUTH_SERVICE_ENDPOINT=http://internal-keycloak:8080\n")
            values = read_env(file)
            assert file.stat().st_mode & 0o777 == 0o600
            assert len(base64.b64decode(values[KEYS[0]].split(",",1)[1], validate=True)) == 32
            assert all(values[k] not in result.stdout + result.stderr for k in KEYS)
            all_values.append(values)
            for compose in ("docker-compose.yml", ".docker/docker-compose.yml", ".live/config/includes.chroot_after_packages/etc/skel/docker-compose.yml"):
                rendered = render(compose, file); assert rendered.returncode == 0, "Compose rendering failed"
                ui = json.loads(rendered.stdout)["services"]["dbrepo-ui"]["environment"]
                assert ui["NUXT_OIDC_PROVIDERS_KEYCLOAK_CLIENT_SECRET"] == values["AUTH_SERVICE_CLIENT_SECRET"]
                assert ui["NUXT_OIDC_PROVIDERS_KEYCLOAK_CLIENT_SECRET"] != values["AUTH_SERVICE_CLIENT"]
                assert ui["NUXT_OIDC_TOKEN_KEY"] == values[KEYS[0]]
                assert ui["NUXT_OIDC_AUTH_SESSION_SECRET"] == values[KEYS[1]]
                assert ui["NUXT_OIDC_SESSION_SECRET"] == values[KEYS[2]]
                if compose in ("docker-compose.yml", ".docker/docker-compose.yml"):
                    assert ui["NUXT_OIDC_PROVIDERS_KEYCLOAK_OPEN_ID_CONFIGURATION_ISSUER"] == \
                        values.get("BASE_URL", "http://localhost") + "/realms/dbrepo"
                    assert ui["NUXT_OIDC_PROVIDERS_KEYCLOAK_OPEN_ID_CONFIGURATION_JWKS_URI"] == \
                        values.get("AUTH_SERVICE_ENDPOINT", "http://auth-service:8080") + "/realms/dbrepo/protocol/openid-connect/certs"
                for missing in KEYS:
                    file.write_text("\n".join(f"{k}={v}" for k,v in values.items() if k != missing)+"\n")
                    assert render(compose, file).returncode != 0, "Missing UI secret accepted"
                file.write_text("\n".join(f"{k}={v}" for k,v in values.items())+"\n")
            helm_values = {"ui": {"oidc": dict(zip(("tokenKey", "authSessionSecret", "sessionSecret"), (values[k] for k in KEYS)))}}
            if index:
                helm_values.update({"hostname": "review.example", "authservice": {"endpoint": "http://internal-keycloak:8080/"}})
            helm_file = work/"ui-values.yaml"
            helm_file.write_text(yaml.safe_dump(helm_values))
            args = ["helm", "template", "audit", str(ROOT/"helm/dbrepo"), "-f", str(helm_file)]
            rendered = subprocess.run(args, env=ENV, capture_output=True, text=True)
            assert rendered.returncode == 0, "Helm rendering failed"
            docs = [v for v in yaml.safe_load_all(rendered.stdout) if v]
            check_helm_oidc(docs, "https://" + ("review.example" if index else "example.com") + "/realms/dbrepo",
                            ("http://internal-keycloak:8080" if index else "http://auth-service") + "/realms/dbrepo/protocol/openid-connect/certs")
            if index:
                overridden = subprocess.run(args + ["--set-string", "authservice.issuerUri=https://login.example/custom-realm",
                    "--set-string", "authservice.jwkUri=https://keys.example/jwks"], env=ENV, capture_output=True, text=True)
                assert overridden.returncode == 0, "Helm OIDC override rendering failed"
                check_helm_oidc(list(yaml.safe_load_all(overridden.stdout)),
                                "https://login.example/custom-realm", "https://keys.example/jwks")
            ui = next(v["stringData"] for v in docs if v.get("kind") == "Secret" and v["metadata"]["name"] == "ui-secret")
            assert [ui[k] for k in ("NUXT_OIDC_TOKEN_KEY", "NUXT_OIDC_AUTH_SESSION_SECRET", "NUXT_OIDC_SESSION_SECRET")] == [values[k] for k in KEYS]
            for missing in ("tokenKey", "authSessionSecret", "sessionSecret"):
                rejected = subprocess.run(args+["--set-string", "ui.oidc."+missing+"="], env=ENV, capture_output=True, text=True)
                assert rejected.returncode != 0 and "Set ui.oidc."+missing in rejected.stderr
            module.rotate(file)
            rotated = read_env(file)
            assert all(rotated[k] != values[k] for k in KEYS)
            assert {k:v for k,v in values.items() if k not in KEYS} == {k:v for k,v in rotated.items() if k not in KEYS}
            assert file.stat().st_mode & 0o777 == 0o600
        assert all(all_values[0][k] != all_values[1][k] for k in KEYS)
    print("PASS: random UI secrets, Compose/Helm OIDC defaults and overrides, missing-secret rejection and isolated rotation")


if __name__ == "__main__": main()
