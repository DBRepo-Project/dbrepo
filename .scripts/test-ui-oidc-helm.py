#!/usr/bin/env python3
"""Verify runtime OIDC endpoints and optional build labels in the Helm chart."""
import subprocess
import unittest
from pathlib import Path

import yaml

ROOT = Path(__file__).resolve().parents[1]
PREFIX = "NUXT_OIDC_PROVIDERS_KEYCLOAK_"


def environment(*overrides):
    command = ["helm", "template", "login-check", str(ROOT / "helm/dbrepo"),
               "--show-only", "templates/ui-secret.yaml", "--set", "hostname=login.example.test"]
    for value in overrides:
        command.extend(["--set-string", value])
    return yaml.safe_load(subprocess.check_output(command, text=True))["stringData"]


class UiOidcHelmTest(unittest.TestCase):
    def test_public_issuer_and_internal_keys_use_deployment_endpoints(self):
        for endpoint in ("http://auth-service", "http://auth-service:8080/"):
            with self.subTest(endpoint=endpoint):
                env = environment("authservice.endpoint=" + endpoint)
                self.assertEqual(env[PREFIX + "OPEN_ID_CONFIGURATION_ISSUER"],
                                 "https://login.example.test/realms/dbrepo")
                self.assertEqual(env[PREFIX + "OPEN_ID_CONFIGURATION_JWKS_URI"],
                                 endpoint.rstrip("/") + "/realms/dbrepo/protocol/openid-connect/certs")
                self.assertTrue(env[PREFIX + "AUTHORIZATION_URL"].startswith("https://login.example.test/"))

    def test_build_label_overrides_are_optional(self):
        defaults = environment()
        self.assertNotIn("NUXT_PUBLIC_VERSION", defaults)
        self.assertNotIn("NUXT_PUBLIC_COMMIT", defaults)
        env = environment("ui.public.version=integration/replication", "ui.public.commit=b9ceb377d")
        self.assertEqual(env["NUXT_PUBLIC_VERSION"], "integration/replication")
        self.assertEqual(env["NUXT_PUBLIC_COMMIT"], "b9ceb377d")


if __name__ == "__main__":
    unittest.main()
