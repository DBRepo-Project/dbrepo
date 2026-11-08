# Image default constraint

New schemas allow multiple ordinary images and enforce at most one default image.
The database uses a generated key that is 1 for a default image and NULL otherwise.
The existing unique image name/version rule remains in place.

For an existing installation, back up the metadata database, pause image writes and
apply the migration using a dedicated administrative database connection:

```sh
mariadb --defaults-extra-file=/private/metadata-client.cnf dbrepo < dbrepo-metadata-db/migration/image-default-constraint.sql
```

The migration discovers the old unique index by its column, preserves system-versioned
history and can be run again. It installs the replacement guard before removing the old
index. It stops if existing data violates the one-default rule. Never resolve that error
by deleting user images or their history. Check the conflicting data first.

The migration does not select a new default or modify existing image rows. Keep the
backup until the image create/read/update/delete checks pass. Application rollback can
leave this compatible constraint in place; restoring the old FALSE uniqueness would
fail once several ordinary images exist.

Run `python3 .scripts/test-image-default-constraint.py` for the disposable native
MariaDB check. It does not connect to any configured deployment.
