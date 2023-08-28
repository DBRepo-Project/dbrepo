FROM bitnami/mariadb:10.5 as runtime

ENV METADATA_DB=fda
ENV METADATA_USERNAME=root
ENV METADATA_PASSWORD=dbrepo
ENV MARIADB_DATABASE="${METADATA_DB}"
ENV MARIADB_ROOT_PASSWORD="${METADATA_PASSWORD}"

# force lower-case table names
COPY ./51-dbrepo.cnf /opt/bitnami/mariadb/conf/my_custom.cnf

# Scripts are copied to /docker-entrypoint-initdb.d/ in docker-compose from analyze service
HEALTHCHECK --interval=10s --timeout=5s --retries=12 CMD mysqladmin ping --user="$METADATA_USERNAME" --password="$METADATA_PASSWORD" --silent

COPY ./setup-schema.sql /docker-entrypoint-initdb.d/setup-schema.sql