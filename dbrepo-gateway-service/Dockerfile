###### FIRST STAGE ######
FROM dbrepo-metadata-db:latest as dependency
MAINTAINER Martin Weise <martin.weise@tuwien.ac.at>

###### FIRST STAGE ######
FROM maven:slim as build

COPY ./pom.xml ./

RUN mvn -fn -B dependency:go-offline > /dev/null

COPY --from=dependency /root/.m2/repository/at/tuwien /root/.m2/repository/at/tuwien

COPY ./report ./report
COPY ./rest-service ./rest-service

# Make sure it compiles
RUN mvn -q clean package -DskipTests > /dev/null

###### SECOND STAGE ######
FROM openjdk:11-jre-slim as runtime

ENV METADATA_DB=fda
ENV METADATA_USERNAME=postgres
ENV METADATA_PASSWORD=postgres
ENV GATEWAY_ENDPOINT=http://gateway-service:9095
ENV LOG_LEVEL=debug

WORKDIR /app

COPY ./server.keystore ./server.keystore
COPY ./root.crt /etc/ssl/certs/DBRepo_Root_CA.crt

RUN cat /etc/ssl/certs/DBRepo_Root_CA.crt >> /etc/ssl/certs/ca-certificates.crt

COPY ./service_ready /usr/bin
RUN chmod +x /usr/bin/service_ready

HEALTHCHECK --interval=10s --timeout=5s --retries=12 CMD service_ready

COPY --from=build ./rest-service/target/rest-service-*.jar ./rest-service.jar

EXPOSE 9095

ENTRYPOINT ["java", "-Dlog4j2.formatMsgNoLookups=true", "-Djavax.net.ssl.trustStore=/app/server.keystore", "-Djavax.net.ssl.trustStorePassword=password", "-jar", "./rest-service.jar"]