FROM bitnami/mariadb:11.2.2-debian-11-r0 as runtime

ENV MARIADB_DATABASE=fda
ENV MARIADB_ROOT_PASSWORD=dbrepo

COPY ./setup-schema.sql /docker-entrypoint-initdb.d/setup-schema.sql