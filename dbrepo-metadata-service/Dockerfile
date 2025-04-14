###### FIRST STAGE ######
FROM --platform=$BUILDPLATFORM dbrepo-core:build AS dependency
LABEL org.opencontainers.image.authors="martin.weise@tuwien.ac.at"

###### SECOND STAGE ######
FROM --platform=$BUILDPLATFORM maven:3-amazoncorretto-17 AS build
LABEL org.opencontainers.image.authors="martin.weise@tuwien.ac.at"

COPY ./pom.xml ./

RUN mvn -fn dependency:go-offline

COPY --from=dependency /root/.m2/repository/at/ac/tuwien/ifs/dbrepo /root/.m2/repository/at/ac/tuwien/ifs/dbrepo

COPY ./oai ./oai
COPY ./report ./report
COPY ./repositories ./repositories
COPY ./rest-service ./rest-service
COPY ./services ./services

# Make sure it compiles
RUN mvn -fn clean package -DskipTests

###### THIRD STAGE ######
FROM --platform=$BUILDPLATFORM amazoncorretto:17-alpine3.19 AS runtime
LABEL org.opencontainers.image.authors="martin.weise@tuwien.ac.at"

RUN apk add --no-cache curl bash jq

WORKDIR /app

RUN adduser -D dbrepo --uid 1001

USER 1001

COPY --from=build --chown=1001 ./rest-service/target/dbrepo-metadata-service-rest-service-*.jar ./metadata-service.jar

# non-root port
EXPOSE 8080

ENV JAVA_OPTS="-Dlog4j2.formatMsgNoLookups=true"

ENTRYPOINT exec java $JAVA_OPTS -jar ./metadata-service.jar