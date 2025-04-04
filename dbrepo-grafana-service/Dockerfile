###### FIRST STAGE ######
FROM dbrepo-metadata-service:build AS dependency
MAINTAINER Martin Weise <martin.weise@tuwien.ac.at>

FROM maven:3-openjdk-17 AS build
MAINTAINER Martin Weise <martin.weise@tuwien.ac.at>

COPY ./pom.xml ./

RUN mvn -fn -B dependency:go-offline

COPY ./rest-service ./rest-service
COPY --from=dependency /root/.m2/repository/at/tuwien /root/.m2/repository/at/tuwien

# Make sure it compiles
RUN mvn clean package -DskipTests

###### SECOND STAGE ######
FROM eclipse-temurin:17-jdk AS runtime
MAINTAINER Martin Weise <martin.weise@tuwien.ac.at>

WORKDIR /app

USER 65534

COPY --from=build --chown=65534 ./rest-service/target/dbrepo-grafana-service-rest-service-*.jar ./grafana-service.jar

# non-root port
EXPOSE 8080

ENTRYPOINT ["java", "-Dlog4j2.formatMsgNoLookups=true",  "-jar", "./grafana-service.jar"]