###### FIRST STAGE ######
FROM maven:slim as build
MAINTAINER Cornelia Michlits <cornelia.michlits@tuwien.ac.at>
LABEL db=metadata

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

RUN apt-get update && apt-get install -y curl
RUN cd /tmp && curl -LO https://github.com/prometheus/node_exporter/releases/download/v0.18.1/node_exporter-0.18.1.linux-amd64.tar.gz && tar -xvf node_exporter-0.18.1.linux-amd64.tar.gz && mv node_exporter-0.18.1.linux-amd64/node_exporter /usr/local/bin/

ENV METADATA_DB=fda
ENV METADATA_USERNAME=root
ENV METADATA_PASSWORD=dbrepo
ENV MARIADB_DATABASE="${METADATA_DB}"
ENV MARIADB_ROOT_PASSWORD="${METADATA_PASSWORD}"

# Scripts are copied to /docker-entrypoint-initdb.d/ in docker-compose from analyze service
HEALTHCHECK --interval=10s --timeout=5s --retries=12 CMD mysqladmin ping --user="$METADATA_USERNAME" --password="$METADATA_PASSWORD" --silent

COPY ./setup-schema.sql /docker-entrypoint-initdb.d/setup-schema.sql
COPY ./setup-node-exporter.sh /docker-entrypoint-initdb.d/setup-node-exporter.sh
RUN chmod +x /docker-entrypoint-initdb.d/setup-node-exporter.sh

EXPOSE 9100

WORKDIR /app

COPY --from=build /root/.m2/repository/at/tuwien /root/.m2/repository/at/tuwien