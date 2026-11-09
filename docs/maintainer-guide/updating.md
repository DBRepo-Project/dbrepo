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

Existing database dashboards also need a [generated-content refresh](../admin-guide/dashboard-content-upgrade.md)
before dashboard access resumes. Deploying new images alone leaves the previous dashboard JSON in Grafana.


## Dashboard identity and datasource upgrade

Existing Grafana installations need the following maintenance sequence before normal traffic resumes. Keep public Grafana access and DBRepo permission, owner and visibility mutations paused. Back up the Grafana database as well as Metadata. Keep the automatic initializer stopped during preparation; on Helm, use `dashboardservice.setupJob.enabled=false` until the migration is ready.

Preserve a working Grafana server administrator for setup and reconciliation. Before retiring the only server administrator or the account configured as `DASHBOARD_UI_USERNAME`, create and verify a separate trusted service administrator and update the backend credentials. The default Grafana administrator may share the DBRepo administrator's login, but a newly created OAuth identity is an ordinary user and cannot replace those service privileges.

1. **Export before removing provisioning.** Use the new setup image against the existing Grafana instance. Set its Grafana administrator credentials and research organization, and supply the exact existing backend URLs. `DASHBOARD_LEGACY_DATA_ENDPOINT` identifies the old Infinity URL and defaults to `BASE_URL`; Helm installations use `DASHBOARD_METRICS_URL=http://metric-db-server`. With a protected writable export directory mounted into the setup container, run:

   ```sh
   python /app/provision.py export /migration/system-dashboards.json
   ```

   Override the image's normal initializer entry point when running this command. The export directory must be writable by its execution user (the packaged image uses UID 1001). The command creates `system-dashboards.json` and `system-dashboards.json.datasources.yaml` with mode 0600, and refuses to overwrite an existing backup. The latter file contains only the verified one-shot datasource deletions. Keep the JSON backup outside the replaced configuration directory.

2. **Resolve preflight conflicts.** Unexpected datasource identities or additional System dashboards stop export before cleanup files are created. Move UI-created System dashboards to a separate folder. Export and migrate extra provider-owned dashboards explicitly before removing their provider. Keep unrelated resources intact.

3. **Perform the native provisioning transition.** Install the updated Grafana configuration with the old System provider and datasource definitions removed. Temporarily mount the generated `.datasources.yaml` file under `/etc/grafana/provisioning/datasources/` and restart the protected Grafana instance. Verify that the three legacy managed datasources are gone and the backup is readable. For Helm, a temporary Secret mounted through `dashboardui.extraSecretMounts` can supply this cleanup file; keep the setup Job disabled during this stage. Provider removal may delete the four old System dashboards, which is why the export is required first.

4. **Remove the one-shot cleanup file.** Restore the packaged empty datasource provisioning before running setup or allowing another normal restart. The cleanup file must not remain mounted after recreation, or it can delete the new research datasource on a later restart.

5. **Configure identity and organizations.** Add the dedicated OAuth secret to the existing deployment configuration. Use the public DBRepo/Grafana URLs for login and the internal `DASHBOARD_DATA_ENDPOINT` for backend requests. Upgrade Data, Dashboard Service and the shared Python client before Metadata uses their current behavior. Set `DASHBOARD_MIGRATION_EXPORT` to the mounted JSON backup and run the setup image. Its normal `init.py` entry point configures the dedicated Keycloak client, research datasource and monitoring organization, imports the preserved System dashboards, then reconciles permissions. Setup may reuse an existing monitoring organization only when its members are trusted Grafana server administrators and resource identities match.

   For Helm, store the JSON as key `system-dashboards.json` in a protected Secret and set `dashboardui.migrationExportSecret` to its name. Re-enable the setup Job only after the provisioning cleanup is removed. For external Keycloak or Grafana, keep the automatic Job disabled and run the same setup image explicitly with the external endpoints and required setup credentials.

6. **Resolve account prerequisites and reconcile.** Existing manual Grafana accounts need the individually verified migration described in the [Dashboard Service README](https://github.com/DBRepo-Project/dbrepo/blob/main/dbrepo-dashboard-service/README.md#existing-manual-grafana-accounts). Automatic username or email linking is disabled. An owner must sign in through OAuth before private dashboard activation can succeed. If initialization reports a missing account or backend failure, keep traffic paused, resolve it and rerun. If identity setup is needed before account migration, run `python -c 'from provision import configure_environment; configure_environment()'` in the setup image first, then run the normal initializer after the accounts are ready.

7. **Review preserved content and restart.** Operator-added panels may still reference datasources in the old organization. Review and rebind those references explicitly; unrelated datasources are not automatically moved. The native query checks do not execute every historical or custom panel. Restart every Grafana replica with the packaged permission-cache setting after setup, then verify ordinary authorized private charts, anonymous public charts and administrator-only monitoring before reopening traffic.

The research organization keeps managed database dashboards in the root folder. Folder inheritance needs operator review. Owner reconciliation replaces direct per-user Admin exceptions with the current owner's permission; unrelated View/Edit grants remain. Metadata, SQL and Grafana do not share a transaction, so a failed stage is not a rollback of the complete upgrade. Use the saved export and the reported error to complete recovery while traffic stays paused.
