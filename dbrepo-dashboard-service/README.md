# Dashboard Service

The service manages Grafana dashboards in the root folder. Public or schema-public databases give the Viewer and Editor roles read access. Private dashboards remove those role grants. Database READ grants map to Grafana View; write grants map to Edit. The current database owner receives Admin when an active Grafana account has the exact same login. Trusted Grafana organization and server administrators retain their platform access.

Grafana accounts are managed separately. DBRepo does not create accounts or link them by email. Creating a database still works when its owner has no Grafana account: a new private dashboard then remains accessible only to trusted administrators. Enabling a private dashboard requires an active owner account before Metadata saves the enabled state. Create the account with the matching login and retry. Explicit grants and revocations return an error when that login cannot be resolved. After a Grafana account rename, an operator must remove any old numeric-user grant and repair the login mapping before retrying.

Owner synchronization makes the current owner the only direct per-user Admin on a managed dashboard. A former Admin is downgraded to its retained DBRepo View/Edit access, or removed. Operator-added direct Admin exceptions are overwritten; unrelated View/Edit grants are preserved. Disabled users cannot receive new grants, but their existing grants can be revoked before they are re-enabled.

Access, owner, visibility and image changes are serialized per database in Metadata. An image request holds that row lock during S3 ownership checks and the object read, so slow storage can delay other mutations for the same database. The S3 client uses SDK I/O timeouts without an overall operation deadline; retries or a slowly streaming object can extend that wait. Ordinary reads do not take this lock. Content updates from table and view changes do not replay access grants. Repeating an identical grant request or revoking an already-removed metadata grant retries the Grafana operation. Metadata, SQL, Search and Grafana do not share a transaction: a failed request can have changed another backend. Resolve the reported backend error and retry the same operation. An ownership or visibility change that fails at Grafana keeps its previous Metadata state so the original owner can retry.

## Deployment and existing dashboards

Upgrade the Dashboard Service and shared Python client before Metadata, which uses the new content-only update endpoint and owner-account check. Apply `permission_cache = false` to every Grafana replica and restart it; both packaged configurations include this setting. Grafana's permission cache can otherwise keep previously authorized Viewer or Editor requests working until expiry.

For existing dashboards, run `init.py` during a maintenance window with database access, ownership and visibility mutations stopped. It reconciles role access, current owners and existing member grants. It attempts all databases and exits with an error if any synchronization fails. Repair missing accounts or backend errors, then rerun before reopening access. It does not remove unrelated direct View/Edit exceptions added by operators.

Managed dashboards must remain in the root folder. Moving one into a folder can introduce inherited permissions that a local dashboard revoke cannot remove. Updates to such dashboards fail with HTTP 409; an operator must inspect the folder permissions and move the dashboard back. Creation uses a neutral title until restrictive access is established. Failed creation attempts try to delete the new dashboard, but a backend outage can prevent cleanup and require operator attention.

## Internal API

All dashboard mutations and account checks require the system role.

- `POST /api/v1/dashboard` creates a dashboard from `database_name`, `owner_username`, `is_public` and `is_schema_public`.
- `PUT /api/v1/dashboard/{uid}` synchronizes owner and role access, then managed content when enabled.
- `PUT /api/v1/dashboard/{uid}/content` updates enabled managed content without changing access.
- `PUT /api/v1/dashboard/{uid}/access/{username}` accepts `permission`: `View`, `Edit`, `Editor` (legacy alias), `Admin`, or an empty string to revoke.
- `HEAD /api/v1/dashboard/user/{username}` returns 204 for an active exact-login account, or 404 otherwise.

Malformed payloads return 400, missing accounts or dashboards 404, operator-managed folder placement 409, and backend errors 502. Native Grafana calls have finite timeouts; an operation involving several calls is not an atomic permission transaction.
