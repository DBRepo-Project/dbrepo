###### FIRST STAGE ######
FROM --platform=$BUILDPLATFORM maven:3-amazoncorretto-21 AS build
LABEL org.opencontainers.image.authors="martin.weise@tuwien.ac.at"

COPY ./pom.xml ./

RUN mvn -fn dependency:go-offline

COPY ./api ./api
COPY ./lib ./lib
COPY ./querystore ./querystore
COPY ./report ./report
COPY ./repositories ./repositories
COPY ./rest-service ./rest-service
COPY ./services ./services

# Make sure it compiles
RUN mvn -q clean package -DskipTests

###### SECOND STAGE ######
FROM --platform=$BUILDPLATFORM docker.io/bitnamilegacy/java:21 AS runtime
LABEL org.opencontainers.image.authors="martin.weise@tuwien.ac.at"

ENV DUCKDB_VERSION="1.4.4"
ENV DUCKDB_PLATFORM="linux_amd64"

WORKDIR /home/dbrepo/.duckdb/extensions/v${DUCKDB_VERSION}/${DUCKDB_PLATFORM}

COPY ./lib/httpfs.duckdb_extension.gz ./httpfs.duckdb_extension.gz
COPY ./lib/mysql_scanner.duckdb_extension.gz ./mysql_scanner.duckdb_extension.gz

RUN gunzip -r . && \
    mkdir -p /var/log/app/service/data && \
    chown -R 1001:1001 /var/log/app/service/data /home/dbrepo/.duckdb

WORKDIR /app

RUN useradd dbrepo --uid 1001 && \
    chown -R 1001:1001 /app

USER 1001

COPY --from=build --chown=1001 ./rest-service/target/*service-*.jar ./data-service.jar

# non-root port
EXPOSE 8080

ENV JAVA_OPTS="-XX:+IgnoreUnrecognizedVMOptions --add-opens=java.base/java.lang=ALL-UNNAMED --add-opens=java.base/java.lang.invoke=ALL-UNNAMED --add-opens=java.base/java.lang.reflect=ALL-UNNAMED --add-opens=java.base/java.io=ALL-UNNAMED --add-opens=java.base/java.net=ALL-UNNAMED --add-opens=java.base/java.nio=ALL-UNNAMED --add-opens=java.base/java.util=ALL-UNNAMED --add-opens=java.base/java.util.concurrent=ALL-UNNAMED --add-opens=java.base/java.util.concurrent.atomic=ALL-UNNAMED --add-opens=java.base/jdk.internal.ref=ALL-UNNAMED --add-opens=java.base/sun.nio.ch=ALL-UNNAMED --add-opens=java.base/sun.nio.cs=ALL-UNNAMED --add-opens=java.base/sun.security.action=ALL-UNNAMED --add-opens=java.base/sun.util.calendar=ALL-UNNAMED --add-opens=java.security.jgss/sun.security.krb5=ALL-UNNAMED -Djdk.reflect.useDirectMethodHandle=false -Dio.netty.tryReflectionSetAccessible=true"

ENTRYPOINT exec java $JAVA_OPTS -jar ./data-service.jar