###### FIRST STAGE ######
FROM dbrepo-metadata-service:build as dependency
MAINTAINER Martin Weise <martin.weise@tuwien.ac.at>

###### SECOND STAGE ######
FROM maven:3-openjdk-17 as build
MAINTAINER Martin Weise <martin.weise@tuwien.ac.at>

COPY ./pom.xml ./

RUN mvn -fn -B dependency:go-offline

COPY --from=dependency /root/.m2/repository/at/tuwien /root/.m2/repository/at/tuwien

COPY ./api ./api
COPY ./querystore ./querystore
COPY ./report ./report
COPY ./rest-service ./rest-service
COPY ./services ./services

# Make sure it compiles
RUN mvn clean package -DskipTests

###### THIRD STAGE ######
FROM eclipse-temurin:17-jdk as runtime
MAINTAINER Martin Weise <martin.weise@tuwien.ac.at>

WORKDIR /app

COPY --from=build ./rest-service/target/rest-service-*.jar ./data-service.jar

EXPOSE 9093

ENTRYPOINT ["java", "-Dlog4j2.formatMsgNoLookups=true",  "-jar", "./data-service.jar"]