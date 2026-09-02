###### FIRST STAGE ######
FROM --platform=$BUILDPLATFORM maven:3-amazoncorretto-21 AS build
LABEL org.opencontainers.image.authors="martin.weise@tuwien.ac.at"

COPY ./pom.xml ./

RUN mvn -fn dependency:go-offline

COPY ./lib ./lib
COPY ./rest-service ./rest-service
COPY ./services ./services

RUN mvn -q clean package -DskipTests

###### SECOND STAGE ######
FROM --platform=$BUILDPLATFORM docker.io/bitnamilegacy/java:21 AS runtime
LABEL org.opencontainers.image.authors="martin.weise@tuwien.ac.at"
LABEL org.opencontainers.image.source="https://github.com/DBRepo-Project/dbrepo"

WORKDIR /app

RUN mkdir -p /var/log/app/service/replication && \
    chown -R 1001:1001 /var/log/app/service/replication

RUN adduser dbrepo --uid 1001 && \
    chown -R 1001:1001 /app

USER 1001

COPY --from=build --chown=1001 ./rest-service/target/*service*.jar ./replication-service.jar

EXPOSE 8080

ENV JAVA_OPTS=""

ENTRYPOINT exec java $JAVA_OPTS -jar ./replication-service.jar
