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

ENV METADATA_DB=fda
ENV METADATA_USERNAME=root
ENV METADATA_PASSWORD=dbrepo
ENV BROKER_USERNAME=fda
ENV BROKER_PASSWORD=fda
ENV SHARED_FILESYSTEM=/tmp
ENV USER_NETWORK=userdb
ENV LOG_LEVEL=debug
ENV DBREPO_CLIENT_SECRET=client-secret
ENV CLIENT_ID=dbrepo-client
ENV JWT_ISSUER=https://localhost/realms/dbrepo
ENV JWT_PUBKEY=public-key

WORKDIR /app

COPY ./service_ready /usr/bin
RUN chmod +x /usr/bin/service_ready

HEALTHCHECK --interval=10s --timeout=5s --retries=12 CMD service_ready

COPY ./server.keystore ./server.keystore
COPY --from=build ./rest-service/target/rest-service-*.jar ./container-service.jar

EXPOSE 9091

ENTRYPOINT ["java", "-Dlog4j2.formatMsgNoLookups=true", "-Djavax.net.ssl.trustStore=/app/server.keystore", "-Djavax.net.ssl.trustStorePassword=password", "-jar", "./container-service.jar"]
