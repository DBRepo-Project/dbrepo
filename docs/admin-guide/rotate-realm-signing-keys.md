# Rotate imported realm keys

Installations created from the previous realm exports share signing and encryption keys. An update of the realm JSON files does not replace keys in an existing Keycloak database.

New installations generate their own keys during realm import. Search and Dashboard obtain verification keys from the configured `AUTH_SERVICE_ENDPOINT`. The old `JWT_PUBKEY` setting is no longer used. The Python clients reject the RSA keys from the retired exports even when Keycloak still advertises them.

For an existing installation:

1. Schedule a maintenance window and take a Keycloak database backup. Users will need to sign in again.
2. Stop external access to the API and UI while keeping Keycloak available to the administrator.
3. From the updated repository, run the rotation script against that installation's Keycloak URL. The script requires Python and Requests, and prompts for the administrator password. It also accepts `AUTH_SERVICE_ADMIN_PASSWORD` from the environment.

   ```sh
   python3 .scripts/rotate-realm-keys.py --endpoint https://your-keycloak.example --username admin
   ```

4. Deploy the updated services and restart Data, Metadata, Search, Dashboard and UI to discard cached keys and sessions. Check that newly issued tokens work and that a token issued before rotation is rejected before restoring external access.

The script replaces the providers whose IDs occur in the retired `dbrepo` and `master` realm exports. It creates replacements before deleting the old providers, refreshes its administrator token during master-realm rotation, and checks that the retired provider IDs are gone. Repeating a successful run makes no further changes.

If an administrator recreated or renamed the imported providers, their IDs may differ. Review those providers in Keycloak and rotate any keys copied from the old exports. A result of zero replacements only means that the known provider IDs are absent. It does not establish how other providers obtained their keys.

Search and Dashboard cache Keycloak verification keys for up to 60 seconds. Restarting them during the upgrade clears that cache. Future realm exports must pass through `.scripts/sanitize-realm-keys.py` before they are used as installation templates; the export helper invokes it automatically.
