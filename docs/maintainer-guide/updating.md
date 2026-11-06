---
author: Martin Weise
---

The update procedure is:

1. Make a [full backup](/infrastructures/dbrepo/1.13/maintainer-guide/backup-data/) of the Metadata Database.
2. If there are schema changes, you need to apply them in the Metadata Database according to the release notes. We
   supply a migration script in this case with the release notes.
3. For Docker, update the `docker-compose.yml` with 
   the [updated file](/infrastructures/dbrepo/1.13/maintainer-guide/install-docker/). For Kubernetes, install the
   new [Helm Chart](/infrastructures/dbrepo/1.13/maintainer-guide/install-kubernetes/).
4. Restart the application.


Dashboard permission reconciliation requires a maintenance window. Stop database access, owner and visibility changes before upgrading: Helm runs the initializer as a release Job, and Compose includes it in the initialization service. Upgrade the Dashboard Service and its shared Python client before Metadata. Apply the packaged Grafana permission-cache setting to every replica and restart Grafana. Check initialization logs and repair any missing exact-login accounts or backend failures before reopening mutations. Private dashboard activation requires an active Grafana account with the same login as its database owner; creating a database does not.

Keep managed dashboards in the root folder. Folder inheritance and operator-managed permissions need review before reconciliation. Direct per-user Admin exceptions are replaced by the current database owner's permission; existing unrelated View/Edit grants remain. See the Dashboard Service README for retry and account-rename handling.
