# Refresh generated dashboard content

Public and schema-public dashboards include generated panels only for views whose data or schema is public. Private dashboards retain their private view panels for authorized users. Disabling generation removes the generated section and keeps the owner's manual section above it.

Existing Grafana dashboards need a content refresh after this upgrade. Installing a new service image does not remove metadata from dashboard JSON that Grafana already stores.

1. Schedule a maintenance window. Block external dashboard access and pause DBRepo metadata mutations, including API clients and background jobs. Keep Grafana and the Metadata Service available internally for synchronization.
2. Back up the Grafana database and Metadata database. Save the deployment configuration with restricted file permissions.
3. Deploy matching Metadata Service, Dashboard Service and dashboard initializer images built from the corrected source. All Python images must contain the updated DBRepo client library.
4. Run the existing synchronization entry point with the deployment's system credentials and Grafana organization settings. With Compose:

   ```sh
   docker compose run --rm --no-deps --entrypoint python dbrepo-dashboard-service-init \
     -c 'from init import synchronize_dashboards; synchronize_dashboards()'
   ```

   This refreshes database dashboards and their managed permissions. Calling `synchronize_dashboards()` directly avoids running unrelated OAuth and datasource provisioning. It exits with an error if any dashboard fails; resolve the error and repeat while access remains blocked.
5. Check the effective dashboard JSON as an anonymous user: public views should be present and private view names and column metadata should be absent. Check a private dashboard as its owner and anonymously; the owner should retain access and the anonymous request should return HTTP 403. For disabled generation, confirm that the generated section is gone and manual panels remain.
6. Resume metadata mutations and reopen access after all refreshes and checks succeed.

Keep metadata mutations paused throughout synchronization. Ordinary API mutations use a shared database row lock, while the maintenance initializer requires this external maintenance window. Concurrent Grafana edits can cause a version conflict; retry from the current dashboard version after resolving the competing edit.

Manual panels belong to the owner and are preserved. Review them before publishing a database: the generated-content filter cannot remove private information that an owner put in a manual panel. A refresh also cannot remove metadata already copied from an older dashboard response.
