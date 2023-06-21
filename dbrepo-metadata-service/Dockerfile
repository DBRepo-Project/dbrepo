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
ENV GATEWAY_ENDPOINT=http://gateway-service
ENV PID_BASE="http://localhost/pid/"
ENV REPOSITORY_NAME="Example Repository"
ENV BASE_URL="http://localhost"
ENV ADMIN_MAIL="noreply@localhost"
ENV EARLIEST_DATESTAMP="2022-09-17T18:23:00Z"
ENV DELETED_RECORD=persistent
ENV GRANULARITY="YYYY-MM-DDThh:mm:ssZ"
ENV LOG_LEVEL=debug

WORKDIR /app

COPY --from=build ./rest-service/target/rest-service-*.jar ./metadata-service.jar

EXPOSE 9090

ENTRYPOINT ["java", "-Dlog4j2.formatMsgNoLookups=true",  "-jar", "./metadata-service.jar"]
