###### FIRST STAGE ######
FROM --platform=$BUILDPLATFORM maven:3-amazoncorretto-17 AS build
LABEL org.opencontainers.image.authors="martin.weise@tuwien.ac.at"

COPY ./pom.xml ./

RUN mvn -fn dependency:go-offline

COPY ./lib ./lib
COPY ./oai ./oai
COPY ./report ./report
COPY ./repositories ./repositories
COPY ./rest-service ./rest-service
COPY ./services ./services

# Make sure it compiles
RUN mvn -q clean package -DskipTests

###### SECOND STAGE ######
FROM --platform=$BUILDPLATFORM eclipse-temurin:17-jre AS runtime
LABEL org.opencontainers.image.authors="martin.weise@tuwien.ac.at"

RUN apt-get update && \
    apt-get install -y \
    bash \
    curl \
    jq && \
    apt-get clean

WORKDIR /app

RUN mkdir -p /var/log/app/service/metadata && \
    chown -R 1001:1001 /var/log/app

RUN useradd -m -u 1001 dbrepo && \
    chown -R 1001:1001 /app

USER 1001

COPY --from=build --chown=1001 ./rest-service/target/*service*.jar ./metadata-service.jar

# non-root port
EXPOSE 8080

ENV JAVA_OPTS=""

ENTRYPOINT exec java $JAVA_OPTS -jar ./metadata-service.jar