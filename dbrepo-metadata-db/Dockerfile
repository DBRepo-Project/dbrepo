###### FIRST STAGE ######
FROM maven:3-openjdk-17 as build
MAINTAINER Martin Weise <martin.weise@tuwien.ac.at>

COPY ./pom.xml ./

RUN mvn -fn -B dependency:go-offline > /dev/null

COPY ./api ./api
COPY ./entities ./entities
COPY ./oai ./oai
COPY ./querystore ./querystore
COPY ./test ./test

# Make sure it compiles
RUN mvn -q clean package -DskipTests

# Install to local repository
RUN mvn -q clean install > /dev/null

###### SECOND STAGE ######
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

WORKDIR /app

COPY --from=build /root/.m2/repository/at/tuwien /root/.m2/repository/at/tuwien