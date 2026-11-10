# UI session secrets

All three Compose variants, including the live distribution, require the three names produced by the installer:
`NUXT_OIDC_TOKEN_KEY`, `OIDC_AUTH_SESSION_SECRET` and `OIDC_SESSION_SECRET`.
Missing values stop configuration rendering. The committed fallback credentials
and the legacy aliases `OIDC_TOKEN_KEY` and `NUXT_OIDC_AUTH_SESSION_SECRET` are no
longer used. New installations generate an independent random 32-byte token key
and random session secrets.

For an existing installation, run the rotation as the owner of its environment
file, from the updated checkout:

```sh
python3 .scripts/rotate-ui-secrets.py /private/deployment/.env
docker compose --env-file /private/deployment/.env up -d --no-deps --force-recreate dbrepo-ui
```

The helper replaces only the UI secrets, removes the old aliases and preserves
the other settings. It writes the replacement atomically with permissions 0600
and prints no values. Keep the environment file and its backups private. Do not
rerun the full secret generator against an existing installation: it also changes
database and service credentials.

Recreate every UI instance with the same new values. Existing UI sessions become
invalid and users must sign in again. This rotation does not change Keycloak's
realm signing keys or database passwords. Helm also requires explicit `ui.oidc.tokenKey`, `ui.oidc.authSessionSecret` and
`ui.oidc.sessionSecret` in a protected values file; the chart has no shared defaults.
Use the generated values for the corresponding three Compose settings above.
Supply the same private values file on every upgrade so sessions remain stable.
To rotate Helm, replace those values and roll out every UI pod. The Compose helper
does not update Kubernetes Secrets. The OIDC client secret is a separate credential:
Compose uses `AUTH_SERVICE_CLIENT_SECRET`; Helm uses `authservice.client.secret`.
Keep it equal to the confidential client's secret in the identity provider.
