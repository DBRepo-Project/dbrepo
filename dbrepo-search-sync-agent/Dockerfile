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
ENV SEARCH_USERNAME=fda
ENV SEARCH_PASSWORD=fda
ENV LOG_LEVEL=debug

WORKDIR /app

COPY --from=build ./rest-service/target/rest-service-*.jar ./search-sync-agent.jar

EXPOSE 9050

ENTRYPOINT ["java", "-Dlog4j2.formatMsgNoLookups=true",  "-jar", "./search-sync-agent.jar"]
