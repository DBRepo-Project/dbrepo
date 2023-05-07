###### FIRST STAGE ######
FROM dbrepo-metadata-db:latest as dependency
MAINTAINER Martin Weise <martin.weise@tuwien.ac.at>

###### SECOND STAGE ######
FROM maven:slim as build

COPY ./pom.xml ./

RUN mvn -fn -B dependency:go-offline > /dev/null

COPY --from=dependency /root/.m2/repository/at/tuwien /root/.m2/repository/at/tuwien

COPY ./api ./api
COPY ./rest-service ./rest-service
COPY ./services ./services
COPY ./report ./report

# Make sure it compiles
RUN mvn -q clean package -DskipTests

###### THIRD STAGE ######
FROM openjdk:11-jre-slim as runtime

ENV METADATA_DB=fda
ENV METADATA_USERNAME=root
ENV METADATA_PASSWORD=dbrepo
ENV GATEWAY_ENDPOINT=http://gateway-service
ENV WEBSITE=http://localhost
ENV LOG_LEVEL=debug
ENV DBREPO_CLIENT_SECRET=client-secret
ENV CLIENT_ID=dbrepo-client
ENV JWT_ISSUER=http://localhost:8080/realms/dbrepo
ENV JWT_PUBKEY=public-key

WORKDIR /app

COPY ./service_ready /usr/bin
RUN chmod +x /usr/bin/service_ready

HEALTHCHECK --interval=10s --timeout=5s --retries=12 CMD service_ready

COPY --from=build ./rest-service/target/rest-service-*.jar ./identifier-service.jar

EXPOSE 9096

ENTRYPOINT ["java", "-Dlog4j2.formatMsgNoLookups=true", "-jar", "./identifier-service.jar"]
