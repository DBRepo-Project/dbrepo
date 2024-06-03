###### FIRST STAGE ######
FROM maven:3-openjdk-17 as build
MAINTAINER Martin Weise <martin.weise@tuwien.ac.at>

COPY ./pom.xml ./
COPY ./api/pom.xml ./api/
COPY ./entities/pom.xml ./entities/
COPY ./oai/pom.xml ./oai/
COPY ./report/pom.xml ./report/
COPY ./repositories/pom.xml ./repositories/
COPY ./rest-service/pom.xml ./rest-service/
COPY ./services/pom.xml ./services/
COPY ./test/pom.xml ./test/

RUN mvn verify -B -fn

COPY ./api ./api
COPY ./entities ./entities
COPY ./oai ./oai
COPY ./report ./report
COPY ./repositories ./repositories
COPY ./rest-service ./rest-service
COPY ./services ./services
COPY ./test ./test

# Make sure it compiles
RUN mvn clean install -DskipTests

###### SECOND STAGE ######
FROM amazoncorretto:17-alpine3.19 as runtime
MAINTAINER Martin Weise <martin.weise@tuwien.ac.at>

WORKDIR /app

USER 65534

COPY --from=build --chown=65534 ./rest-service/target/dbrepo-metadata-service-rest-service-*.jar ./metadata-service.jar

# non-root port
EXPOSE 8080

ENTRYPOINT ["java", "-Dlog4j2.formatMsgNoLookups=true",  "-jar", "./metadata-service.jar"]
