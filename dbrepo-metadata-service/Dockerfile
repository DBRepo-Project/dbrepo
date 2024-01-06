###### FIRST STAGE ######
FROM maven:3-openjdk-17 as build
MAINTAINER Martin Weise <martin.weise@tuwien.ac.at>

COPY ./pom.xml ./
COPY ./api/pom.xml ./api/
COPY ./entities/pom.xml ./entities/
COPY ./oai/pom.xml ./oai/
COPY ./querystore/pom.xml ./querystore/
COPY ./report/pom.xml ./report/
COPY ./repositories/pom.xml ./repositories/
COPY ./rest-service/pom.xml ./rest-service/
COPY ./services/pom.xml ./services/
COPY ./test/pom.xml ./test/

RUN mvn verify -B -fn

COPY ./api ./api
COPY ./entities ./entities
COPY ./oai ./oai
COPY ./querystore ./querystore
COPY ./report ./report
COPY ./repositories ./repositories
COPY ./rest-service ./rest-service
COPY ./services ./services
COPY ./test ./test

# Make sure it compiles
RUN mvn clean install -DskipTests

###### SECOND STAGE ######
FROM openjdk:17-alpine as runtime
MAINTAINER Martin Weise <martin.weise@tuwien.ac.at>

RUN apk --no-cache add curl bash

ENV ADMIN_MAIL="noreply@localhost"
ENV BASE_URL="http://localhost"
ENV GRANT_PRIVILEGES="SELECT, CREATE, CREATE VIEW, CREATE ROUTINE, CREATE TEMPORARY TABLES, LOCK TABLES, INDEX, TRIGGER, INSERT, UPDATE, DELETE"
ENV BROKER_ENDPOINT="http://broker-service:15672/admin/broker"
ENV BROKER_HOST="broker-service"
ENV BROKER_PORT=5672
ENV BROKER_USERNAME=fda
ENV BROKER_PASSWORD=fda
ENV DELETED_RECORD=persistent
ENV EARLIEST_DATESTAMP="2022-09-17T18:23:00Z"
ENV GRANULARITY="YYYY-MM-DDThh:mm:ssZ"
ENV JWT_ISSUER="http://localhost/realms/dbrepo"
ENV JWT_PUBKEY="MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAqqnHQ2BWWW9vDNLRCcxD++xZg/16oqMo/c1l+lcFEjjAIJjJp/HqrPYU/U9GvquGE6PbVFtTzW1KcKawOW+FJNOA3CGo8Q1TFEfz43B8rZpKsFbJKvQGVv1Z4HaKPvLUm7iMm8Hv91cLduuoWx6Q3DPe2vg13GKKEZe7UFghF+0T9u8EKzA/XqQ0OiICmsmYPbwvf9N3bCKsB/Y10EYmZRb8IhCoV9mmO5TxgWgiuNeCTtNCv2ePYqL/U0WvyGFW0reasIK8eg3KrAUj8DpyOgPOVBn3lBGf+3KFSYi+0bwZbJZWqbC/Xlk20Go1YfeJPRIt7ImxD27R/lNjgDO/MwIDAQAB"
ENV LOG_LEVEL=debug
ENV METADATA_DB=fda
ENV METADATA_HOST=metadata-db
ENV METADATA_JDBC_EXTRA_ARGS=""
ENV METADATA_USERNAME=root
ENV METADATA_PASSWORD=dbrepo
ENV NOT_SUPPORTED_KEYWORDS=\\*,AVG,BIT_AND,BIT_OR,BIT_XOR,COUNT,COUNTDISTINCT,GROUP_CONCAT,JSON_ARRAYAGG,JSON_OBJECTAGG,MAX,MIN,STD,STDDEV,STDDEV_POP,STDDEV_SAMP,SUM,VARIANCE,VAR_POP,VAR_SAMP,--
ENV PID_BASE="http://localhost/pid/"
ENV REPOSITORY_NAME="Example Repository"
ENV SEARCH_USERNAME=admin
ENV SEARCH_PASSWORD=admin
ENV USER_NETWORK=userdb
ENV WEBSITE="http://localhost"
ENV KEYCLOAK_HOST="http://authentication-service:8080"
ENV KEYCLOAK_ADMIN=fda
ENV KEYCLOAK_ADMIN_PASSWORD=fda
ENV MIN_CONCURRENT_CONSUMERS=1
ENV MAX_CONCURRENT_CONSUMERS=5
ENV BROKER_VIRTUALHOST=dbrepo
ENV QUEUE_NAME="dbrepo"
ENV EXCHANGE_NAME="dbrepo"
ENV ROUTING_KEY="dbrepo.#"
ENV CONNECTION_TIMEOUT=60000
ENV DATACITE_URL="https://api.test.datacite.org"
ENV DATACITE_PREFIX=""
ENV DATACITE_USERNAME=""
ENV DATACITE_PASSWORD=""
ENV S3_STORAGE_ENDPOINT="http://storage-service:9000"
ENV S3_ACCESS_KEY_ID="seaweedfsadmin"
ENV S3_SECRET_ACCESS_KEY="seaweedfsadmin"
ENV MIRROR_RATE=60
ENV OBTAIN_METADATA_RATE=60
ENV DELETE_STALE_QUERIES_RATE=60

WORKDIR /app

COPY --from=build ./rest-service/target/dbrepo-metadata-service-rest-service-*.jar ./metadata-service.jar

EXPOSE 9099

ENTRYPOINT ["java", "-Dlog4j2.formatMsgNoLookups=true",  "-jar", "./metadata-service.jar"]
