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
FROM openjdk:17-alpine as runtime
MAINTAINER Martin Weise <martin.weise@tuwien.ac.at>

ENV METADATA_DB=fda
ENV METADATA_HOST=metadata-db
ENV METADATA_JDBC_EXTRA_ARGS=""
ENV METADATA_PASSWORD=dbrepo
ENV METADATA_USERNAME=root
ENV SEARCH_USERNAME=admin
ENV SEARCH_PASSWORD=admin
ENV LOG_LEVEL=debug

WORKDIR /app

COPY --from=build ./rest-service/target/rest-service-*.jar ./search-sync-agent.jar

EXPOSE 9050

ENTRYPOINT ["java", "-Dlog4j2.formatMsgNoLookups=true",  "-jar", "./search-sync-agent.jar"]
