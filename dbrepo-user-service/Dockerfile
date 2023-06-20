###### FIRST STAGE ######
FROM dbrepo-metadata-db:latest as dependency
MAINTAINER Martin Weise <martin.weise@tuwien.ac.at>

###### SECOND STAGE ######
FROM maven:3-openjdk-17 as build
MAINTAINER Martin Weise <martin.weise@tuwien.ac.at>

COPY ./pom.xml ./

RUN mvn -fn -B dependency:go-offline > /dev/null

COPY --from=dependency /root/.m2/repository/at/tuwien /root/.m2/repository/at/tuwien

COPY ./rest-service ./rest-service
COPY ./services ./services
COPY ./report ./report

# Make sure it compiles
RUN mvn -q clean package -DskipTests

###### THIRD STAGE ######
FROM openjdk:17-alpine as runtime
MAINTAINER Martin Weise <martin.weise@tuwien.ac.at>

RUN apk --no-cache add bash

ENV METADATA_DB=fda
ENV METADATA_USERNAME=root
ENV METADATA_PASSWORD=dbrepo
ENV GATEWAY_ENDPOINT=http://gateway-service
ENV JWT_ISSUER="http://localhost/realms/dbrepo"
ENV JWT_PUBKEY="MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAqqnHQ2BWWW9vDNLRCcxD++xZg/16oqMo/c1l+lcFEjjAIJjJp/HqrPYU/U9GvquGE6PbVFtTzW1KcKawOW+FJNOA3CGo8Q1TFEfz43B8rZpKsFbJKvQGVv1Z4HaKPvLUm7iMm8Hv91cLduuoWx6Q3DPe2vg13GKKEZe7UFghF+0T9u8EKzA/XqQ0OiICmsmYPbwvf9N3bCKsB/Y10EYmZRb8IhCoV9mmO5TxgWgiuNeCTtNCv2ePYqL/U0WvyGFW0reasIK8eg3KrAUj8DpyOgPOVBn3lBGf+3KFSYi+0bwZbJZWqbC/Xlk20Go1YfeJPRIt7ImxD27R/lNjgDO/MwIDAQAB"
ENV LOG_LEVEL=debug
ENV SEARCH_PASSWORD=admin

WORKDIR /app

COPY ./service_ready /usr/bin
RUN chmod +x /usr/bin/service_ready

HEALTHCHECK --interval=10s --timeout=5s --retries=12 CMD service_ready

COPY --from=build ./rest-service/target/rest-service-*.jar ./user-service.jar

EXPOSE 9098

ENTRYPOINT ["java", "-Dlog4j2.formatMsgNoLookups=true", "-jar", "./user-service.jar"]
