FROM mariadb:10.5 as runtime
MAINTAINER Martin Weise <martin.weise@tuwien.ac.at>

COPY ./querystore.sql /docker-entrypoint-initdb.d/querystore.sql

HEALTHCHECK --interval=10s --timeout=5s --retries=12 CMD mysqladmin ping --host=127.0.0.1