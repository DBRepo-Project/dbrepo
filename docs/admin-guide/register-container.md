# Register an existing database container

Registration stores connection settings for a database server that the administrator
already operates. It does not start a server or create a Docker container.

Supply `name`, `host`, `image_id`, `quota`, `privileged_username`,
`privileged_password`, `readonly_username` and `readonly_password` to
`POST /api/v1/container`. `quota` is a positive integer no greater than 2147483647.
`port` defaults to the selected image's port. Optional `ui_host` and `ui_port`
control the displayed connection; their defaults are the effective host and port.

The privileged account must support DBRepo's database and account provisioning.
Use a separate, dedicated read-only username and its plaintext password. During
research database creation, Data Service creates or grants this read-only account
using the registered credentials. If that account already exists, supply its
current password. Do not point it at a privileged account or an unrelated user's
account. Registration itself neither creates nor verifies that SQL account.

The Python client accepts the same fields:

```python
container = client.create_container(
    name="Research database server",
    host="data-db",
    image_id=image_id,
    privileged_username=privileged_username,
    privileged_password=privileged_password,
    readonly_username=readonly_username,
    readonly_password=readonly_password,
    quota=50,
)
```

The SDK sends a quota of 50 unless another value is supplied. Keep credentials in
protected configuration, not source files. Responses contain no database
passwords. Missing read-only credentials now produce a validation error instead
of reaching a non-null database column and returning HTTP 500.

Existing registrations and SQL accounts are unchanged. No schema migration or
credential rotation is needed for this fix. New registrations must provide both
read-only credential fields. To verify a registration, read it back and create a
synthetic research database on it before making it available to users. Delete
only an unused test registration; deleting a registration does not delete the
external SQL server.
