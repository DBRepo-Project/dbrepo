# Dashboard Service

The service manages Grafana dashboards in the root folder. Public or schema-public databases give the Viewer and Editor roles read access. Private dashboards remove those role grants. Database READ grants map to Grafana View; write grants map to Edit. The current database owner receives Admin when an active Grafana account has the exact same login. Trusted Grafana organization and server administrators retain their platform access.

Users sign in to Grafana through the DBRepo OAuth provider. Grafana creates an ordinary account for the verified provider identity; existing accounts are not automatically linked by username or email. Creating a database still works before its owner first signs in: a new private dashboard then remains accessible only to trusted administrators. Enabling a private dashboard requires an active owner account with the exact DBRepo login. The owner should sign in through OAuth before activation is retried. Explicit grants and revocations return an error when that login cannot be resolved. Existing manual accounts need the verified migration below.

Owner synchronization makes the current owner the only direct per-user Admin on a managed dashboard. A former Admin is downgraded to its retained DBRepo View/Edit access, or removed. Operator-added direct Admin exceptions are overwritten; unrelated View/Edit grants are preserved. Disabled users cannot receive new grants, but their existing grants can be revoked before they are re-enabled.

Access, owner, visibility and image changes are serialized per database in Metadata. An image request holds that row lock during S3 ownership checks and the object read, so slow storage can delay other mutations for the same database. The S3 client uses SDK I/O timeouts without an overall operation deadline; retries or a slowly streaming object can extend that wait. Ordinary reads do not take this lock. Content updates from table and view changes do not replay access grants. Repeating an identical grant request or revoking an already-removed metadata grant retries the Grafana operation. Metadata, SQL, Search and Grafana do not share a transaction: a failed request can have changed another backend. Resolve the reported backend error and retry the same operation. An ownership or visibility change that fails at Grafana keeps its previous Metadata state so the original owner can retry.

## Caller identity and organizations

The research datasource sends the signed-in user's OAuth access token to the Data API. Anonymous requests carry no service credentials, so public charts remain available and private data follows the caller's current DBRepo permissions. Grafana refreshes expiring tokens through Keycloak. If renewal fails, the user must sign in again. Logout or session revocation does not immediately invalidate an access token that has already been issued.

The Data Service reads current Metadata state for data access and grant operations. A request normally uses one current database snapshot for its permission and object checks. An unavailable Metadata service produces an error instead of using a cached permission list. This adds a Metadata request compared with a warm-cache read.

Research dashboards and their Infinity datasource stay in the configured research organization. Prometheus, OpenSearch and the four packaged System dashboards use the monitoring organization. Setup resolves that organization's configured name or ID and accepts existing membership only when every member is a trusted Grafana server administrator. It stops on known resource identity conflicts before changing those resources. Unrelated organizations and resources remain under operator control. Administrators can switch to the monitoring organization to view its dashboards.

## Configuration

The setup image runs `init.py`, which configures the OAuth client and datasources, then reconciles managed dashboard permissions. Keycloak administrator credentials are needed only by this one-shot setup process. The running Dashboard Service receives its existing Grafana credentials and research organization ID.

| Setting | Purpose |
| --- | --- |
| `BASE_URL` | Public DBRepo URL used for browser links and Keycloak login. |
| `DASHBOARD_PUBLIC_URL` | Public Grafana URL; defaults to `BASE_URL` plus `/dashboard/`. Its exact `/login/generic_oauth` callback is registered in Keycloak. |
| `DASHBOARD_DATA_ENDPOINT` | Backend URL reachable from Grafana. Compose defaults to `http://gateway-service:8080`; Helm uses `dashboardui.dataEndpoint`, defaulting to `http://gateway-service`. |
| `DASHBOARD_OAUTH_CLIENT_ID` | Dedicated confidential client, default `dbrepo-grafana`, with authorization-code flow and PKCE. |
| `DASHBOARD_OAUTH_SECRET` | Dedicated random client secret shared by Grafana and setup; at least 32 characters. |
| `DASHBOARD_ORG_ID`, `DASHBOARD_RESEARCH_ORG_NAME` | Matching numeric ID and name of the research organization. |
| `DASHBOARD_MONITORING_ORG_NAME`, `DASHBOARD_MONITORING_ORG_ID` | Monitoring organization name and optional existing ID. Setup does not assume ID 2 is available. |
| `DASHBOARD_MIGRATION_EXPORT` | Path to the verified System dashboard export for an existing installation. |
| `DASHBOARD_LEGACY_DATA_ENDPOINT` | Export-only validation of the old Infinity URL; defaults to `BASE_URL`. |
| `DASHBOARD_METRICS_URL`, `DASHBOARD_SEARCH_URL` | Actual monitoring backend URLs, also checked during export. Helm's metrics URL is `http://metric-db-server`. |

Public login URLs and the internal data endpoint have separate purposes. `localhost` inside Grafana identifies the Grafana container, so a browser-facing localhost URL is not an internal backend address.

Fresh Compose installations obtain the dedicated secret from the packaged secret generator. For an upgrade, add the new variable to the existing protected environment file without regenerating unrelated credentials. Helm generates a secret on installation and reuses the existing Kubernetes Secret on connected upgrades. For render-only or GitOps workflows, supply a stable `dashboardui.oauthClientSecret` through the deployment's secret-management process.

The automatic Helm setup Job requires the bundled Keycloak and Grafana services. For externally managed services, set `dashboardservice.setupJob.enabled=false` and run the setup image explicitly with the corresponding endpoints, administrator credentials and matching Grafana OAuth settings. Setup failures require operator action; an unsuccessful Job does not complete a migration.

## Existing installations

Follow the ordered [upgrade procedure](../docs/maintainer-guide/updating.md). Keep public Grafana traffic and DBRepo access, owner and visibility mutations paused throughout the transition. Upgrade the Data Service to the current-permission implementation and the Dashboard Service/shared Python client before Metadata uses their new endpoints.

Export System dashboard JSON **before removing the old file provider**. The provider can delete its dashboards when it is removed. The export preserves edits to the four known dashboards and generates a one-shot `deleteDatasources` file for verified legacy resources. Read-only file-provisioned datasources need that native provisioning transition before setup can recreate them through the API. Remove the cleanup file before setup and every normal restart; leaving it installed can delete the recreated research datasource.

An extra dashboard in the System folder stops export without creating cleanup files. Grafana's metadata does not reliably distinguish an edited file-provisioned dashboard from a UI-created one in this case. Move a UI-created dashboard to a separate folder, or export and migrate an additional provider-owned dashboard explicitly before removing its provider.

Preserving dashboard JSON does not move its custom datasource dependencies. Review operator-added panels and historical datasource aliases, then rebind references as needed in the monitoring organization. Setup does not move unrelated datasources automatically. Native datasource acceptance does not establish that every historical or customized panel works.

Apply `permission_cache = false` and restart every Grafana replica after setup before reopening traffic. This also clears old datasource routes retained in Grafana's cache. Confirm that setup and permission reconciliation finish successfully, that an ordinary user can query authorized private data, and that monitoring queries require administrator access.

`init.py` attempts every database during permission reconciliation and exits with an error if any synchronization fails. Repair account or backend errors and rerun while mutations remain paused. Reconciliation makes the current owner the only direct per-user Admin; unrelated direct View/Edit exceptions remain for operator review.

Managed research dashboards must remain in the root folder. Moving one into a folder can introduce inherited permissions that a local revoke cannot remove. Updates to such dashboards fail with HTTP 409. Creation uses a neutral title until restrictive access is established. Failed creation tries to remove the new dashboard, but a backend outage can leave cleanup for an operator.

## Existing manual Grafana accounts

Account migration requires an operator to verify the Keycloak issuer and subject against the old Grafana numeric user ID, login and email. Matching names or addresses alone are insufficient. Pause permission changes and record the current authoritative DBRepo grants before proceeding.

1. Disable the verified old Grafana account and log out its active sessions using the native administrator APIs.
2. Remove that numeric user's direct grants from the managed DBRepo dashboards. Preserve unrelated dashboards, team memberships and permission entries for separate review.
3. Give the old account unique retired login and email values, keeping it disabled.
4. Have the verified user sign in through DBRepo OAuth. Grafana creates a new ordinary numeric identity for that provider subject.
5. Reconcile the current DBRepo grants onto the new account, then verify its authorized dashboard and data access. Confirm that old credentials and sessions remain unusable and that the old numeric ID has no managed dashboard grants.

This process retains the disabled old account. It does not merge authentication records or preserve its numeric ID, personal preferences or unrelated access. No account migration is automatic. The native procedure was checked with disposable identities; real account ownership still requires independent operator verification.

## Internal API

All dashboard mutations and account checks require the system role.

- `POST /api/v1/dashboard` creates a dashboard from `database_name`, `owner_username`, `is_public` and `is_schema_public`.
- `PUT /api/v1/dashboard/{uid}` synchronizes owner and role access, then managed content when enabled.
- `PUT /api/v1/dashboard/{uid}/content` updates enabled managed content without changing access.
- `PUT /api/v1/dashboard/{uid}/access/{username}` accepts `permission`: `View`, `Edit`, `Editor` (legacy alias), `Admin`, or an empty string to revoke.
- `HEAD /api/v1/dashboard/user/{username}` returns 204 for an active exact-login account, or 404 otherwise.

Malformed payloads return 400, missing accounts or dashboards 404, operator-managed folder placement 409, and backend errors 502. Native Grafana calls have finite timeouts; an operation involving several calls is not an atomic permission transaction.
