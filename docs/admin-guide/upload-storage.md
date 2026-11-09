# Temporary storage for multipart uploads

The Data Service stages accepted uploads on disk, calculates SHA-256, SHA-1 and MD5 in one streaming pass, then uploads the file through the S3 SDK. The file body can be reopened if the SDK retries. HTTP 201, `X-S3-Key`, opaque upload keys, original filename metadata and owner checks keep their existing meaning.

Deploy the updated Data Service image. This change needs no database migration and no Python client update.

Keep `spring.servlet.multipart.file-size-threshold` at `0B` so the servlet spools multipart bodies to disk. `MAX_UPLOAD_SIZE` still controls the existing file and request limits, with the same 2 GB default. The application uses the JVM's temporary directory for staging; set `-Djava.io.tmpdir` to a writable temporary filesystem if the default is unsuitable for the deployment.

Provision temporary storage for concurrent accepted requests. Tomcat can move a spooled part into the staging file; other multipart implementations or filesystem layouts may copy it. Allow up to twice the accepted upload size per active request across servlet and application temporary storage. The staging file is removed after a successful upload or an ordinary transfer/storage failure. Process termination cannot run this cleanup, so use temporary storage with an appropriate container lifecycle.

The memory bound applies to HTTP upload processing, hashing and the S3 write. Helpers that deliberately return a complete object as `byte[]`, including downstream BLOB operations, retain that separate memory requirement. This upload change does not make large BLOB insertion stream through JDBC.

Validation uses the real endpoint and S3 SDK behind a disposable Tomcat server: a 256 MiB HTTP upload succeeds with a 96 MiB JVM heap, while a concurrent small upload returns HTTP 201. Stored bytes and ownership metadata are verified against native SeaweedFS. Separate checks cover SDK retry, cleanup on transfer/storage errors and the existing owner, CSV, gzip and binary workflows.
