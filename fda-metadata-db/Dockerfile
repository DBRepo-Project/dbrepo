###### FIRST STAGE ######
FROM maven:slim as build
MAINTAINER Cornelia Michlits <cornelia.michlits@tuwien.ac.at>

COPY ./pom.xml ./

RUN mvn -fn -B dependency:go-offline > /dev/null

COPY ./api ./api
COPY ./entities ./entities
COPY ./querystore ./querystore
COPY ./oai ./oai

# Make sure it compiles
RUN mvn -q clean package -DskipTests > /dev/null

# Install to local repository
RUN mvn -q clean install > /dev/null

###### SECOND STAGE ######
FROM mariadb:10.5 as runtime

ENV METADATA_DB=fda
ENV METADATA_USERNAME=root
ENV METADATA_PASSWORD=dbrepo
ENV MARIADB_DATABASE="${METADATA_DB}"
ENV MARIADB_ROOT_PASSWORD="${METADATA_PASSWORD}"

# Scripts are copied to /docker-entrypoint-initdb.d/ in docker-compose from analyze service
HEALTHCHECK --interval=10s --timeout=5s --retries=12 CMD mysqladmin ping --user="$METADATA_USERNAME" --password="$METADATA_PASSWORD" --silent

WORKDIR /docker-entrypoint-initdb.d

COPY ./setup-schema.sql ./setup-schema.sql

WORKDIR /app

COPY --from=build /root/.m2/repository/at/tuwien /root/.m2/repository/at/tuwien