###### FIRST STAGE ######
FROM maven:3-openjdk-17 as build
MAINTAINER Martin Weise <martin.weise@tuwien.ac.at>

COPY ./pom.xml ./
COPY ./api/pom.xml ./api/
COPY ./entities/pom.xml ./entities/
COPY ./oai/pom.xml ./oai/
COPY ./querystore/pom.xml ./querystore/
COPY ./rest-service/pom.xml ./rest-service/
COPY ./services/pom.xml ./services/
COPY ./report/pom.xml ./report/

RUN mvn verify -B -fn

COPY ./api/ ./api
COPY ./entities ./entities
COPY ./oai ./oai
COPY ./querystore ./querystore
COPY ./rest-service ./rest-service
COPY ./services ./services
COPY ./report ./report

# Make sure it compiles
RUN mvn clean package -DskipTests

###### SECOND STAGE ######
FROM openjdk:17-alpine as runtime
MAINTAINER Martin Weise <martin.weise@tuwien.ac.at>

ENV ADMIN_MAIL="noreply@localhost"
ENV BASE_URL="http://localhost"
ENV BROKER_CONSUMERS=2
ENV BROKER_ENDPOINT=http://broker-service:15672
ENV BROKER_USERNAME=fda
ENV CLIENT_ID="dbrepo-client"
ENV DBREPO_CLIENT_SECRET="MUwRc7yfXSJwX8AdRMWaQC3Nep1VjwgG"
ENV DELETED_RECORD=persistent
ENV EARLIEST_DATESTAMP="2022-09-17T18:23:00Z"
ENV GRANULARITY="YYYY-MM-DDThh:mm:ssZ"
ENV JWT_ISSUER="http://localhost/realms/dbrepo"
ENV JWT_PUBKEY="MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAqqnHQ2BWWW9vDNLRCcxD++xZg/16oqMo/c1l+lcFEjjAIJjJp/HqrPYU/U9GvquGE6PbVFtTzW1KcKawOW+FJNOA3CGo8Q1TFEfz43B8rZpKsFbJKvQGVv1Z4HaKPvLUm7iMm8Hv91cLduuoWx6Q3DPe2vg13GKKEZe7UFghF+0T9u8EKzA/XqQ0OiICmsmYPbwvf9N3bCKsB/Y10EYmZRb8IhCoV9mmO5TxgWgiuNeCTtNCv2ePYqL/U0WvyGFW0reasIK8eg3KrAUj8DpyOgPOVBn3lBGf+3KFSYi+0bwZbJZWqbC/Xlk20Go1YfeJPRIt7ImxD27R/lNjgDO/MwIDAQAB"
ENV LOG_LEVEL=debug
ENV METADATA_DB=fda
ENV METADATA_PASSWORD=dbrepo
ENV METADATA_USERNAME=root
ENV NOT_SUPPORTED_KEYWORDS=\\*,AVG,BIT_AND,BIT_OR,BIT_XOR,COUNT,COUNTDISTINCT,GROUP_CONCAT,JSON_ARRAYAGG,JSON_OBJECTAGG,MAX,MIN,STD,STDDEV,STDDEV_POP,STDDEV_SAMP,SUM,VARIANCE,VAR_POP,VAR_SAMP,--
ENV PID_BASE="http://localhost/pid/"
ENV REPOSITORY_NAME="Example Repository"
ENV SEARCH_USERNAME=admin
ENV SHARED_FILESYSTEM=/tmp
ENV USER_NETWORK=userdb
ENV WEBSITE=http://localhost

WORKDIR /app

COPY --from=build ./rest-service/target/rest-service-*.jar ./metadata-service.jar

EXPOSE 9099

ENTRYPOINT ["java", "-Dlog4j2.formatMsgNoLookups=true",  "-jar", "./metadata-service.jar"]
