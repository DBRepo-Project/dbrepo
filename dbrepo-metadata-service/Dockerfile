####### FIRST STAGE ######
#FROM --platform=$BUILDPLATFORM maven:3-amazoncorretto-17 AS build
#LABEL org.opencontainers.image.authors="martin.weise@tuwien.ac.at"
#
#COPY ./pom.xml ./
#
#RUN mvn -fn dependency:go-offline
#
#COPY ./lib ./lib
#COPY ./oai ./oai
#COPY ./report ./report
#COPY ./repositories ./repositories
#COPY ./rest-service ./rest-service
#COPY ./services ./services
#
## Make sure it compiles
#RUN mvn -q clean package -DskipTests

###### SECOND STAGE ######
FROM --platform=$BUILDPLATFORM amazoncorretto:17-alpine3.21 AS runtime
LABEL org.opencontainers.image.authors="martin.weise@tuwien.ac.at"

RUN apk --no-cache add \
    bash \
    curl \
    jq

WORKDIR /app

RUN mkdir -p /var/log/app/service/metadata && \
    chown -R 1001:1001 /var/log/app

RUN adduser -D dbrepo --uid 1001 && \
    chown -R 1001:1001 /app

USER 1001

#COPY --from=build --chown=1001 ./rest-service/target/*service*.jar ./metadata-service.jar
COPY --chown=1001 ./rest-service/target/*service-*.jar ./metadata-service.jar

# non-root port
EXPOSE 8080

ENV JAVA_OPTS=""

ENTRYPOINT exec java $JAVA_OPTS -jar ./metadata-service.jar