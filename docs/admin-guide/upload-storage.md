# Upload and BLOB storage

The Data Service stages accepted uploads on disk, calculates SHA-256, SHA-1 and MD5 in one streaming pass, then uploads the file through the S3 SDK. The file body can be reopened if the SDK retries. HTTP 201, `X-S3-Key`, opaque upload keys, original filename metadata and owner checks keep their existing meaning.

Deploy the updated Data Service image. This change needs no database migration and no Python client update.

Keep `spring.servlet.multipart.file-size-threshold` at `0B` so the servlet spools multipart bodies to disk. `MAX_UPLOAD_SIZE` still controls the existing file and request limits, with the same 2 GB default. The application uses the JVM's temporary directory for staging; set `-Djava.io.tmpdir` to a writable temporary filesystem if the default is unsuitable for the deployment.

Provision temporary storage for concurrent accepted requests. Tomcat can move a spooled part into the staging file; other multipart implementations or filesystem layouts may copy it. Allow up to twice the accepted upload size per active request across servlet and application temporary storage. The staging file is removed after a successful upload or an ordinary transfer/storage failure. Process termination cannot run this cleanup, so use temporary storage with an appropriate container lifecycle.

BLOB insertion, updates and delete predicates now bind S3 response streams directly to server-prepared JDBC statements. Each operation keeps its streams open until execution finishes and releases them on success or failure. Unread S3 responses are aborted instead of drained after a binding failure. A transfer interrupted by an I/O error is reported as storage unavailable. Request maps retain their original object keys.

Query results use sequential column access. Binary columns are encoded directly to the JSON or CSV response as base64, including NULL and empty values, without creating a whole-object byte array or base64 string. The existing binary JSON/CSV representation is retained. Explicit whole-object storage helpers and nonbinary string values still have their documented memory requirements; they are not used by these BLOB mutation and serialization paths.

Validation uses the real endpoint and S3 SDK behind a disposable Tomcat server: a 256 MiB HTTP upload succeeds with a 96 MiB JVM heap, while a concurrent small upload returns HTTP 201. Stored bytes and ownership metadata are verified against native SeaweedFS. Separate checks cover SDK retry, cleanup on transfer/storage errors and the existing owner, CSV, gzip and binary workflows.

## JDBC upgrade and database limits

The Data Service uses MariaDB Connector/J 4.0.0. Java 21 is already required by the
service. The [connector's sequential-access mode](https://mariadb.com/resources/blog/whats-new-in-mariadb-connector-j-4-0/)
streams individual large binary columns from the network when a forward-only,
read-only statement uses a fetch size of `Integer.MIN_VALUE`. The old driver
buffered a whole BLOB before returning its input stream. Other Java services keep
their existing driver versions.

The Data Service keeps its previous plain-connection default explicitly. For a
TLS deployment, set `DBREPO_JDBC_SSL_MODE=verify-full` (the property is
`dbrepo.jdbc.ssl-mode`) and configure trusted certificates through the JVM trust
store where needed. Changing the driver does not silently change the deployment's
connection policy. Disposable non-TLS integration fixtures select `sslMode=disable`
explicitly.

Streaming query connections allow the protocol's 1-GiB packet ceiling rather than
the driver's default limit derived from the JVM heap. This does not increase the
server's `max_allowed_packet` or the capacity of the selected column type. The
upload limit and the database's BLOB limits are independent: accepting an upload
is not a promise that a particular BLOB column can store it. Keep appropriate
server and column limits for the deployment. The large-BLOB regression uses
LONGBLOB columns and a disposable server with a 512-MiB packet limit; it does not
change a live database setting.

Upgrading to server-prepared execution also requires actual quoted identifiers
for `ANALYZE TABLE`; parameter markers cannot represent database or table names.
The mapper quotes and escapes both metadata-derived names before executing that
statement. Value parameters remain bound. NULL predicates use `IS NULL` without
a parameter marker, and the binder skips those keys. Binary data values, including
empty values, still use stream parameters.

No research-data migration is needed for the BLOB patch itself. Rebuild the Data
Service image and use its matching bundled/runtime driver. The separate Query
Store row-lock patch still requires its documented migration for existing databases.

The native BLOB regression uses 128-MiB owned objects in a JVM limited to 96 MiB.
It calls the production endpoint methods with a synthetic authenticated principal
and a current Metadata fixture, while S3 and MariaDB are real disposable services.
It checks insertion, an update using a BLOB predicate, deletion, exact lengths and
hashes, JSON/CSV output, NULL/empty values, foreign-owner denial, partial binding
failure, SQL failure and interrupted-transfer cleanup. It does not run the full
HTTP security filter chain; the existing authenticated endpoint tests and the
post-deployment client checks provide that separate coverage.
