---
author: Martin Weise
hide:

- navigation

---

# Get Started

!!! info "Abstract"

    In this short getting started guide we show the dependencies to run the database repository and perform a small, 
    local, test deployment for quickly trying out the features that the repository offers.

## Requirements

### Hardware

For this small, local, test deployment any modern hardware would suffice, we recommend a dedicated virtual machine with
the following settings. Note that most of the CPU and RAM resources will be needed for starting the infrastructure,
this is because of Docker.

- 8 CPU cores
- 16GB RAM memory
- 100GB SSD memory available

### Software

Install Docker Engine for your operating system. There are excellent guides available for Linux, we highly recommend
to use a stable distribution such as [Debian](https://docs.docker.com/desktop/install/debian/). In the following guide
we only consider Debian.

## Deployment

### Docker Compose

We maintain a rapid prototype deployment option through Docker Compose (v2.17.0 and newer). This deployment creates the
core infrastructure and a single Docker container for all user-generated databases.

Download the
latest [`docker-compose.yml`](https://gitlab.phaidra.org/fair-data-austria-db-repository/fda-services/-/raw/master/docker-compose.prod.yml),
nginx reverse proxy
conf [`dbrepo.conf`](https://gitlab.phaidra.org/fair-data-austria-db-repository/fda-services/-/raw/master/dbrepo.conf)
and [`.env`](https://gitlab.phaidra.org/fair-data-austria-db-repository/fda-services/-/raw/master/.env.unix.example):

    curl -o docker-compose.yml https://gitlab.phaidra.org/fair-data-austria-db-repository/fda-services/-/raw/master/docker-compose.prod.yml
    curl -o dbrepo.conf https://gitlab.phaidra.org/fair-data-austria-db-repository/fda-services/-/raw/master/dbrepo.conf
    curl -o .env https://gitlab.phaidra.org/fair-data-austria-db-repository/fda-services/-/raw/master/.env.unix.example

Increase the virtual memory max swap limit for OpenSearch to at least 262144 on the *host machine* according
to [the official manual](https://opensearch.org/docs/1.0/opensearch/install/important-settings/), you need *sudo*
permissions for this (check first with `cat /proc/sys/vm/max_map_count`):

    echo "vm.max_map_count=262144" >> /etc/sysctl.conf
    sysctl -p

Start the services:

    docker compose pull
    docker compose up -d

View the logs:

    docker compose logs -f

The Metadata Database still needs to know that the Docker container that holds all user-generated databases exists, we
need to insert it:

    mariadb -h 127.0.0.1 -D fda -u root -pdbrepo -e "INSERT INTO `fda`.`mdb_containers` \
    (name, internal_name, image_id, host, port, privileged_username, privileged_password) \
    VALUES ('MariaDB 10.5', 'mariadb_10_5', 1, 'user-db', 3306, 'root', 'dbrepo')"

You should now be able to view the front end at <a href="http://127.0.0.1:80" target="_blank">http://127.0.0.1:80</a>

Please be warned that the default configuration is not intended for public deployments. It is only intended to have a
running system within minutes to play around within the system and explore features.

!!! warning "Known security issues with the default configuration"

    The system is auto-configured for a small, local, test deployment and is *not* secure! You need to make modifications
    in various places to make it secure:

    * **Authentication Service**:

        a. You need to use your own instance or configure a secure instance using a (self-signed) certificate.
           Additionally, when serving from a non-default Authentication Service, you need to put it into the 
           `JWT_ISSUER` environment variable (`.env`).

        b. You need to change the default admin user `fda` password in Realm
           master > Users > fda > Credentials > Reset password.

        c. You need to change the client secrets for the clients `dbrepo-client` and `broker-client`. Do this in Realm
           dbrepo > Clients > dbrepo-client > Credentials > Client secret > Regenerate. Do the same for the
           broker-client.

        d. You need to regenerate the public key of the `RS256` algorithm which is shared with all services to verify 
           the signature of JWT tokens. Add your securely generated private key in Realm 
           dbrepo > Realm settings > Keys > Providers > Add provider > rsa.

    * **Broker Service**: by default, this service is configured with an administrative user that has major privileges.
      You need to change the password of the user *fda* in Admin > Update this user > Password. We found this
      [simple guide](https://onlinehelp.coveo.com/en/ces/7.0/administrator/changing_the_rabbitmq_administrator_password.htm)
      to be very useful.

    * **Search Database**: by default, this service is configured to require authentication with an administrative user
      that is allowed to write into the indizes. Following
      this [simple guide](https://www.elastic.co/guide/en/elasticsearch/reference/8.7/reset-password.html), this can be
      achieved using the command line.

    * **Gateway Service**: by default, no HTTPS is used that protects the services behind. You need to provide a trusted
      SSL/TLS certificate in the configuration file or use your own proxy in front of the Gateway Service. See this
      [simple guide](http://nginx.org/en/docs/http/configuring_https_servers.html) on how to install a SSL/TLS
      certificate on NGINX.

##### Migration from 1.2 to 1.3

In case you have a previous deployment from version 1.2, shut down the containers and back them up manually. You can do
this by using the `busybox` image. Replace `deadbeef` with your container name or hash:

```console
export NAME=dbrepo-userdb-xyz
docker run --rm --volumes-from $NAME -v /home/$USER/backup:/backup busybox tar pcvfz /backup/$NAME.tar.gz /var/lib/mysql
```

!!! danger "Wipe all traces of DBRepo from your system"

    To erase all traces of DBRepo from your computer or virtual machine, the following commands delete all containers,
    volumes and networks that are present, execute the following **dangerous** command. It will **wipe** all information
    about DBRepo from your system (excluding the images).
    
    ```console
    docker container stop $(docker container ls -aq -f name=^/dbrepo-.*) || true
    docker container rm $(docker container ls -aq -f name=^/dbrepo-.*) || true
    docker volume rm $(docker volume ls -q -f name=^dbrepo-.*) || true
    docker network rm $(docker network ls -q -f name=^dbrepo-.*) || true
    ```

You can restore the volume *after* downloading the new 1.3 images and creating the infrastructure:

```console
export NAME=dbrepo-userdb-xyz
export PORT=12345
docker container create -h $NAME --name $NAME -p $PORT:3306 -e MARIADB_ROOT_PASSWORD=mariadb --network userdb -v /backup mariadb:10.5
docker run --rm --volumes-from $NAME -v /home/$USER/backup/.tar.gz:/backup/$NAME.tar.gz busybox sh -c 'cd /backup && tar xvfz /backup/$NAME.tar.gz && cp -r /backup/var/lib/mysql/* /var/lib/mysql'
```

Future releases will be backwards compatible and will come with migration scripts.

### Kubernetes

We maintain a RKE2 Kubernetes deployment from version 1.3 onwards. More on that when the release date is fixed.
