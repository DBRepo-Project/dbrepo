###### FIRST STAGE ######
FROM dbrepo-metadata-service:build as dependency
MAINTAINER Martin Weise <martin.weise@tuwien.ac.at>

###### SECOND STAGE ######
FROM maven:3-openjdk-17 as build
MAINTAINER Martin Weise <martin.weise@tuwien.ac.at>

COPY ./pom.xml ./

RUN mvn -fn -B dependency:go-offline

COPY --from=dependency /root/.m2/repository/at/tuwien /root/.m2/repository/at/tuwien

COPY ./rest-service ./rest-service
COPY ./services ./services
COPY ./report ./report

# Make sure it compiles
RUN mvn clean package -DskipTests

###### THIRD STAGE ######
FROM eclipse-temurin:17-jdk as runtime
MAINTAINER Martin Weise <martin.weise@tuwien.ac.at>

ENV METADATA_DB=fda
ENV METADATA_HOST=metadata-db
ENV METADATA_JDBC_EXTRA_ARGS=""
ENV METADATA_PASSWORD=dbrepo
ENV METADATA_USERNAME=root
ENV SEARCH_USERNAME=admin
ENV SEARCH_PASSWORD=admin
ENV LOG_LEVEL=debug
ENV JWT_ISSUER="http://localhost/realms/dbrepo"
ENV JWT_PUBKEY="MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAqqnHQ2BWWW9vDNLRCcxD++xZg/16oqMo/c1l+lcFEjjAIJjJp/HqrPYU/U9GvquGE6PbVFtTzW1KcKawOW+FJNOA3CGo8Q1TFEfz43B8rZpKsFbJKvQGVv1Z4HaKPvLUm7iMm8Hv91cLduuoWx6Q3DPe2vg13GKKEZe7UFghF+0T9u8EKzA/XqQ0OiICmsmYPbwvf9N3bCKsB/Y10EYmZRb8IhCoV9mmO5TxgWgiuNeCTtNCv2ePYqL/U0WvyGFW0reasIK8eg3KrAUj8DpyOgPOVBn3lBGf+3KFSYi+0bwZbJZWqbC/Xlk20Go1YfeJPRIt7ImxD27R/lNjgDO/MwIDAQAB"
ENV BROKER_USERNAME=fda
ENV BROKER_PASSWORD=fda
ENV MIN_CONCURRENT_CONSUMERS=1
ENV MAX_CONCURRENT_CONSUMERS=5
ENV REQUEUE_REJECTED=true
ENV BROKER_HOST="broker-service"
ENV BROKER_PORT=5672
ENV BROKER_VIRTUALHOST=dbrepo
ENV QUEUE_NAME="dbrepo"
ENV EXCHANGE_NAME="dbrepo"
ENV ROUTING_KEY="dbrepo.#"
ENV CONNECTION_TIMEOUT=60000

WORKDIR /app

COPY --from=build ./rest-service/target/rest-service-*.jar ./data-service.jar

EXPOSE 9093

ENTRYPOINT ["java", "-Dlog4j2.formatMsgNoLookups=true",  "-jar", "./data-service.jar"]