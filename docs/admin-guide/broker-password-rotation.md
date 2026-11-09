# Configure and rotate the Compose broker password

Compose uses `BROKER_SERVICE_USERNAME` and `BROKER_SERVICE_PASSWORD` for the broker, Consumer and Metadata Service. The Live image also uses these variables for its broker clients. `SYSTEM_PASSWORD` is a separate application credential.

Before each broker start, the entry point reads the mounted `definitions.json` and writes a runtime copy with a fresh password hash for the selected service user. It preserves the other definition entries. RabbitMQ imports the runtime copy during setup and the final server start. The healthcheck verifies that the configured internal account can authenticate.

The selected username must already exist in the definitions. The shipped account is `admin`. Selecting another account does not create it or grant additional permissions. Empty passwords and missing or duplicate selected users stop startup.

## New installation

Use the installer or secret generator on a new installation. Broker password generation uses OpenSSL and no longer depends on a host installation of `rabbitmqadmin`. The generated password is stored in the protected `.env` file. The shipped definitions contain no usable service password until the entry point prepares the runtime copy.

The development fallback remains `admin` if `BROKER_SERVICE_PASSWORD` is unset. Set an independent generated password for an installation that accepts external traffic.

## Existing installation

1. Schedule a maintenance window and pause ingestion and requests that use the broker. Back up its definitions, data volume and protected deployment configuration.
2. Install the matching Compose file and `sync-service-password.sh`. The packaged configuration includes the script under `config/`. Update `rabbitmq.conf` so `management.load_definitions` points to `/tmp/dbrepo-definitions.json`. Keep the original definitions mounted at `/app/definitions.json`.
3. Preserve custom users, permissions and queues in the original definitions. The entry point updates only the selected service user's password in its runtime copy; do not replace a customized file with the repository template.
4. Set the new `BROKER_SERVICE_PASSWORD` in the protected `.env` file, with the quoting required by Compose for special characters. Keep the selected username unchanged unless a separate account migration is intended. Recreate the broker:

   ```sh
   docker compose up -d --no-deps --force-recreate dbrepo-broker-service
   docker compose ps dbrepo-broker-service
   ```

5. Wait for the broker to be healthy. Then recreate its clients so they receive the same password:

   ```sh
   docker compose up -d --no-deps --force-recreate dbrepo-metadata-service dbrepo-consumer-service
   ```

   The Live image also runs a Data Service broker client; recreate that service there. If a gateway caches service addresses, reload its upstream configuration after replacing backends.
6. Verify an ingestion round trip, queued message counts and rejection of the previous password before resuming traffic. Existing AMQP connections are interrupted by the broker restart; clients must reconnect.

Do not rerun the full secret generator against an existing deployment, remove the broker volume or recreate its queues as part of rotation. For password rollback, keep the upgraded entry point and matching runtime-path configuration. Restore the previous protected password in `.env`, recreate the broker and its clients, then verify access and retained messages before reopening traffic.

Helm uses its own password and definition-hash settings. This Compose entry point does not change the Helm rotation process.
