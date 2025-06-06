---
author: Martin Weise
---

In case the deployment is unsuccessful, we have explanations on their origin and solutions to the most common errors:

**Are you trying to mount a directory onto a file (or vice-versa)?**

:   *Origin*:   Docker Compose does not find all files referenced in the `volumes` section of your `docker-compose.yml`
                file.
:   *Solution*: Ensure all mounted files in the `volumes` section of your `docker-compose.yml` exist and have correct
                file permissions (`0644`) to be found in the filesystem. Note that paths containing directories may not
                work when using Windows instead of the supported Linux.

**The Docker images have been updated but my deployment is not receiving the updates**

:   *Origin*:   Your local Docker image cache is not up-to-date and needs to fetch the remote changes.
:   *Solution*: Update your local Docker image cache by executing `docker compose pull`, it automatically downloads
                all Docker images that have updates. Then apply the new images with `docker compose up -d`.

**Error response from daemon: Error starting userland proxy: listen tcp4 0.0.0.0:xyz: bind: address already in use**

:   *Origin*:   Your deployment machine (e.g. laptop, virtual machine) has the port `xyz` already assigned. Some service
                or application is already listening to this port.
:   *Solution*: This service or application needs to be stopped. You can find out the service or application via
                `sudo netstat -tulpn` (sudo is necessary for the process id) and then stop the service or application
                gracefully or force a stop via `kill -15 PID` (not recommended).

**IllegalArgumentException values less than -1 bytes are not supported**

:   *Origin*:   Your deployment machine (e.g. laptop, virtual machine) appears to not have enough RAM assigned.
:   *Solution*: Assign more RAM to the deployment machine (e.g. add vRAM to the virtual machine).

**HTTP access denied: user 'admin' - invalid credentials**

:   *Origin*:   The broker service cannot bind to the identity service due to wrong configuration.
:   *Solution*: This is very likely due to a wrong `auth_ldap.dn_lookup_bind.password` in `rabbitmq.conf`. The error
                indicates that LDAP check is not even attempted.
