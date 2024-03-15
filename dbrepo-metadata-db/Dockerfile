FROM bitnami/mariadb:11.2.2-debian-11-r0 as runtime

ENV MARIADB_DATABASE=fda
ENV MARIADB_ROOT_PASSWORD=dbrepo

COPY 1_setup-schema.sql /docker-entrypoint-initdb.d/1_setup-schema.sql