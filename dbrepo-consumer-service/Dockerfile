###### FIRST STAGE ######
FROM --platform=$BUILDPLATFORM maven:3-amazoncorretto-21 AS build
LABEL org.opencontainers.image.authors="martin.weise@tuwien.ac.at"

COPY ./pom.xml ./

RUN mvn -fn dependency:go-offline

COPY ./amqp-service ./amqp-service
COPY ./lib ./lib
COPY ./report ./report
COPY ./repositories ./repositories
COPY ./services ./services

# Make sure it compiles
RUN mvn -q clean package -DskipTests

###### SECOND STAGE ######
FROM --platform=$BUILDPLATFORM docker.io/bitnamilegacy/java:21 AS runtime
LABEL org.opencontainers.image.authors="martin.weise@tuwien.ac.at"
LABEL org.opencontainers.image.source="https://github.com/DBRepo-Project/dbrepo"

RUN mkdir -p /var/log/app/service/consumer && \
    chown -R 1001:1001 /var/log/app/service/consumer

WORKDIR /app

RUN useradd dbrepo --uid 1001 && \
    chown -R 1001:1001 /app

USER 1001

COPY --from=build --chown=1001 ./amqp-service/target/*service-*.jar ./consumer-service.jar

ENV JAVA_OPTS=""

ENTRYPOINT exec java $JAVA_OPTS -jar ./consumer-service.jar
