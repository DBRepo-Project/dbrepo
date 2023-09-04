FROM bitnami/mariadb:10.5 as runtime

ENV METADATA_DB=fda
ENV METADATA_USERNAME=root
ENV METADATA_PASSWORD=dbrepo
ENV MARIADB_DATABASE="${METADATA_DB}"
ENV MARIADB_ROOT_PASSWORD="${METADATA_PASSWORD}"

COPY ./setup-schema.sql /docker-entrypoint-initdb.d/setup-schema.sql