###### FIRST STAGE ######
FROM dbrepo-metadata-service:build AS dependency
LABEL org.opencontainers.image.authors="martin.weise@tuwien.ac.at"

###### SECOND STAGE ######
FROM maven:3-amazoncorretto-17 AS build
LABEL org.opencontainers.image.authors="martin.weise@tuwien.ac.at"

COPY ./pom.xml ./

RUN mvn -fn -B -q dependency:go-offline

COPY --from=dependency /root/.m2/repository/at/tuwien /root/.m2/repository/at/tuwien

COPY ./querystore ./querystore
COPY ./report ./report
COPY ./rest-service ./rest-service
COPY ./services ./services

# Make sure it compiles
RUN mvn -fn -B -q clean package -DskipTests

###### THIRD STAGE ######
FROM amazoncorretto:17-alpine3.19 AS runtime
LABEL org.opencontainers.image.authors="martin.weise@tuwien.ac.at"

RUN apk add --no-cache curl bash jq

WORKDIR /app

RUN adduser -S -u 1001 data-service

USER 1001

COPY --from=build --chown=1001 ./rest-service/target/rest-service-*.jar ./data-service.jar

# non-root port
EXPOSE 8080

ENV JAVA_OPTS="-XX:+IgnoreUnrecognizedVMOptions --add-opens=java.base/java.lang=ALL-UNNAMED --add-opens=java.base/java.lang.invoke=ALL-UNNAMED --add-opens=java.base/java.lang.reflect=ALL-UNNAMED --add-opens=java.base/java.io=ALL-UNNAMED --add-opens=java.base/java.net=ALL-UNNAMED --add-opens=java.base/java.nio=ALL-UNNAMED --add-opens=java.base/java.util=ALL-UNNAMED --add-opens=java.base/java.util.concurrent=ALL-UNNAMED --add-opens=java.base/java.util.concurrent.atomic=ALL-UNNAMED --add-opens=java.base/jdk.internal.ref=ALL-UNNAMED --add-opens=java.base/sun.nio.ch=ALL-UNNAMED --add-opens=java.base/sun.nio.cs=ALL-UNNAMED --add-opens=java.base/sun.security.action=ALL-UNNAMED --add-opens=java.base/sun.util.calendar=ALL-UNNAMED --add-opens=java.security.jgss/sun.security.krb5=ALL-UNNAMED -Djdk.reflect.useDirectMethodHandle=false -Dio.netty.tryReflectionSetAccessible=true"

ENTRYPOINT exec java $JAVA_OPTS -jar ./data-service.jar