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
