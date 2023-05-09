###### FIRST STAGE ######
FROM dbrepo-metadata-db:latest as dependency
MAINTAINER Martin Weise <martin.weise@tuwien.ac.at>

###### SECOND STAGE ######
FROM maven:slim as build

COPY ./pom.xml ./

RUN mvn -fn -B dependency:go-offline > /dev/null

COPY --from=dependency /root/.m2/repository/at/tuwien /root/.m2/repository/at/tuwien

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
ENV PID_BASE="http://example.com/pid/"
ENV REPOSITORY_NAME="Example Repository"
ENV BASE_URL="http://example.com"
ENV ADMIN_MAIL="noreply@example.com"
ENV EARLIEST_DATESTAMP="2022-09-17T18:23:00Z"
ENV DELETED_RECORD=persistent
ENV GRANULARITY="YYYY-MM-DDThh:mm:ssZ"
ENV LOG_LEVEL=debug

WORKDIR /app

COPY ./service_ready /usr/bin
RUN chmod +x /usr/bin/service_ready

HEALTHCHECK --interval=10s --timeout=5s --retries=12 CMD service_ready

COPY ./server.keystore ./server.keystore
COPY --from=build ./rest-service/target/rest-service-*.jar ./metadata-service.jar

EXPOSE 9090

ENTRYPOINT ["java", "-Dlog4j2.formatMsgNoLookups=true", "-Djavax.net.ssl.trustStore=/app/server.keystore", "-Djavax.net.ssl.trustStorePassword=password", "-jar", "./metadata-service.jar"]
