Run the UI regressions from `dbrepo-ui`:

```sh
bun install
bunx playwright install chromium
bunx playwright test
```

The tests render the production Vue components with Vuetify, the real client composables and the persisted Pinia store. Playwright supplies HTTP responses. They cover schema serialization and inline errors, numeric add/edit/save/reload, grant selection, revocation, role restrictions and recovery after a committed grant with failed dashboard synchronization.

These tests do not start the DBRepo services. Metadata persistence tests cover database changes after dashboard failures; the live client and replication scripts still need to run against the approved deployment. A mock HTTP response is not evidence that a live SQL grant, Grafana ACL or historical subset is correct.
