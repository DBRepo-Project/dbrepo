FROM bitnami/mariadb:10.5 as runtime

ENV MARIADB_DATABASE=fda
ENV MARIADB_ROOT_PASSWORD=dbrepo

COPY ./setup-schema.sql /docker-entrypoint-initdb.d/setup-schema.sql