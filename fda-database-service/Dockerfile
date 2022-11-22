###### FIRST STAGE ######
FROM fda-metadata-db:latest as dependency
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
RUN mvn -q clean package -DskipTests > /dev/null

###### THIRD STAGE ######
FROM openjdk:11-jre-slim as runtime

ENV METADATA_DB=fda
ENV METADATA_USERNAME=dbrepo
ENV METADATA_PASSWORD=dbrepo
ENV BROKER_USERNAME=fda
ENV BROKER_PASSWORD=fda
ENV SEARCH_ENDPOINT=search-service
ENV SEARCH_USERNAME=elastic
ENV SEARCH_PASSWORD=elastic
ENV GATEWAY_ENDPOINT=http://gateway-service:9095
ENV LOG_LEVEL=debug

COPY ./service_ready /usr/bin
RUN chmod +x /usr/bin/service_ready

HEALTHCHECK --interval=10s --timeout=5s --retries=12 CMD service_ready

COPY --from=build ./rest-service/target/rest-service-*.jar ./database-service.jar

EXPOSE 9092

ENTRYPOINT ["java", "-Dlog4j2.formatMsgNoLookups=true", "-jar", "./database-service.jar"]
